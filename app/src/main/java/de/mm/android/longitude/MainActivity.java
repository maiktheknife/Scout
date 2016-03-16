package de.mm.android.longitude;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.Games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.mm.android.longitude.base.GameActivity;
import de.mm.android.longitude.database.MyDBDelegate;
import de.mm.android.longitude.fragment.BackwardAble;
import de.mm.android.longitude.fragment.ChangeLogDialogFragment;
import de.mm.android.longitude.fragment.ContactListFragment;
import de.mm.android.longitude.fragment.GMapFragment;
import de.mm.android.longitude.fragment.ProcessFragment;
import de.mm.android.longitude.fragment.Slideable;
import de.mm.android.longitude.fragment.UpdateAble;
import de.mm.android.longitude.intro.IntroActivity;
import de.mm.android.longitude.location.UpdateLocationService;
import de.mm.android.longitude.model.ContactData;
import de.mm.android.longitude.model.SignInFailureReason;
import de.mm.android.longitude.network.GCMListenerService;
import de.mm.android.longitude.network.GCMRegUtil;
import de.mm.android.longitude.network.RestService;
import de.mm.android.longitude.util.AccountUtil;
import de.mm.android.longitude.util.GameUtil;
import de.mm.android.longitude.util.PreferenceUtil;
import retrofit.RetrofitError;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends GameActivity implements GMapFragment.IMapFragment, ContactListFragment.IContactFragment {
    private static final String ARG_CONTACTS = "contacts";
    private static final String ARG_FRAGMENT = "fragment";
    private static final String ARG_LOCATION = "location";
    private static final String TAG = MainActivity.class.getSimpleName();

    @Bind(R.id.ac_main_nav_view) NavigationView navigationView;
    @Bind(R.id.ac_main_drawer_layout) DrawerLayout drawerLayout;

    private LocalReceiver localReceiver;
    private RestService webService;
    private Action1<Throwable> errorAction = t -> {
        RetrofitError error = (RetrofitError) t;
        showMessage(error.getMessage());
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.ac_container);
        if (f != null && f instanceof UpdateAble) {
            ((UpdateAble) f).onDataReceived(null);
        }
    };
    // current state
    private Location curLocation;
    private int curFragment; // itemId from current shown Fragment
    private ArrayList<ContactData> curContactDataList;

    private class LocalReceiver extends BroadcastReceiver {
        private final String TAG = LocalReceiver.class.getSimpleName();
    	@Override
    	public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: " + action);
            switch (action) {
                case UpdateLocationService.LOCATION_ACTION:
                    Log.d(TAG, "onReceive Location Update via LocalBroadcast");
                    curLocation = intent.getParcelableExtra("location");
                    break;

                case GCMListenerService.GCM_ACTION:
                    String gcm = intent.getStringExtra(GCMListenerService.GCM_VALUE);
                    Log.d(TAG, "onReceive GCM Message via LocalBroadcast: " + gcm + " --> onDataReceived");
                    loadFriends();
                    break;

                default:
                    Log.w(TAG, "Unknown Action " + action);
                    break;
            }
    	}
    }

	/* LifeCycle */

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.ac_main);
        ButterKnife.bind(this);

		PreferenceManager.setDefaultValues(this, PREFS_NAME_SETTINGS, MODE_PRIVATE, R.xml.prefs_settings, false);

        if (!PreferenceUtil.isUsingApp(this) || !AccountUtil.isAccountValid(this, PreferenceUtil.getAccountEMail(this))) {
            finish();
            startActivity(new Intent(this, IntroActivity.class));
            return;
        }

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                publishSlide(slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }

            private void publishSlide(float offset) {
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.ac_container);
                if (f != null && f instanceof Slideable) {
                    ((Slideable) f).onSilde(offset);
                }
            }
        });

        navigationView.getMenu().getItem(0).setChecked(true);
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            menuItem.setChecked(true);
            showFragment(menuItem.getItemId());
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        localReceiver = new LocalReceiver();
        webService = RestService.Creator.create(this);

        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate Restore");
			curFragment = savedInstanceState.getInt(ARG_FRAGMENT);
            navigationView.getMenu().getItem(curFragment).setChecked(true);
            curLocation = savedInstanceState.getParcelable(ARG_LOCATION);
            curContactDataList = savedInstanceState.getParcelableArrayList(ARG_CONTACTS);
        } else {
            Log.d(TAG, "onCreate new instance");
            curFragment = -1;
            curLocation = PreferenceUtil.getLatestLocation(this);
            curContactDataList = new ArrayList<>();
            updateContacts(MyDBDelegate.Companion.selectFriends(this));
            loadFriends();
            pokeFriends();
            showFragment(R.id.drawer_map);
        }

        showChangeLog(true);
    }

    @Override
	protected void onStart() {
        super.onStart();
		Log.d(TAG, "onStart");
        if (!PreferenceUtil.isUsingApp(this) || !AccountUtil.isAccountValid(this, PreferenceUtil.getAccountEMail(this))) {
            finish();
            startActivity(new Intent(this, IntroActivity.class));
            return;
        }
        setLocalReceiverEnabled(true);
        startService(new Intent(this, UpdateLocationService.class));
    }

    @Override
	protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        outState.putInt(ARG_FRAGMENT, curFragment);
        outState.putParcelable(ARG_LOCATION, curLocation);
        outState.putParcelableArrayList(ARG_CONTACTS, curContactDataList);
        super.onSaveInstanceState(outState);
    }

    @Override
	protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        setLocalReceiverEnabled(false);
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult");

		switch (requestCode) {
        case REQUEST_PICK_CONTACT:
            if (resultCode == RESULT_OK) {
                CursorLoader loader = new CursorLoader(this, data.getData(), null, null, null, null);
                    loader.startLoading();
                    loader.registerListener(2291, (loader1, c) -> {
                        Log.d(TAG, "onLoadComplete " + c.toString());
                        if (c.moveToFirst()) {
                            String mail = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                            if (mail != null) {
                                addFriend(mail);
                            }
                        } else {
                            showMessage(getString(R.string.error_import));
                        }
                    });
            }
            break;
		default:
            break;
		}
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        // support old menu key and toggle the drawer
		if (KeyEvent.KEYCODE_MENU == keyCode) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed");
		// handle drawer
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
			return;
		}

        // handle MapFragment back pressed
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.ac_container);
        if (f != null && f instanceof BackwardAble) {
            if (((BackwardAble) f).onStepBack()){
                return;
            }
        }

		super.onBackPressed();
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Helper */

	private void showFragment(final int which) {
		Log.d(TAG, "showFragment " + which);
		if (curFragment == which) {
			return;
		}
		switch (which) {
		case R.id.drawer_map:
            curContactDataList = MyDBDelegate.Companion.selectFriends(this);
            curFragment = which;
            navigationView.getMenu().getItem(0).setChecked(true);
            getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).replace(R.id.ac_container, GMapFragment.newInstance(curContactDataList)).commit();
            break;

        case R.id.drawer_process:
            curFragment = which;
            navigationView.getMenu().getItem(1).setChecked(true);
            getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).replace(R.id.ac_container, ProcessFragment.newInstance()).commit();
            break;

        case R.id.drawer_contacts:
            curFragment = which;
            navigationView.getMenu().getItem(2).setChecked(true);

            Fragment f = ContactListFragment.newInstance(curContactDataList, curLocation);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                f.setSharedElementEnterTransition(new FabTransition());
//                f.setEnterTransition(new Fade());
//                f.setSharedElementReturnTransition(new FabTransition());
//            }

            getSupportFragmentManager()
                    .beginTransaction()
//                    .addSharedElement(findViewById(R.id.f_map_sheetfab), getString(R.string.transitionFab))
                    .replace(R.id.ac_container, f)
                    .commit();
            break;

        case R.id.drawer_achievements:
            if (isInetAvailable() && getGoogleApiClient().isConnected()) {
                startActivityForResult(Games.Achievements.getAchievementsIntent(getGoogleApiClient()), 12345);
            } else {
                showMessage(getString(R.string.error_noNetworkConnectionFound));
            }
            break;

        case R.id.drawer_leaderboard:
            if (isInetAvailable() && getGoogleApiClient().isConnected()) {
                startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getGoogleApiClient(), getString(R.string.leaderboard_traveled_distance)), 12346);
            } else {
                showMessage(getString(R.string.error_noNetworkConnectionFound));
            }
            break;

        case R.id.drawer_preferences:
            startActivity(new Intent(this, SettingsActivity.class));
            break;

        case R.id.drawer_website:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.website))));
            break;

        case R.id.drawer_changelog:
            showChangeLog(false);
		    break;
		}
	}

    private void setLocalReceiverEnabled(final boolean isEnabled) {
        Log.d(TAG, "setLocalReceiverEnabled: " + isEnabled);
        if (isEnabled) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(UpdateLocationService.LOCATION_ACTION);
            filter.addAction(GCMListenerService.GCM_ACTION);
            LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, filter);
        } else {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
        }
    }

	private void showChangeLog(final boolean checkVersion) {
        Log.d(TAG, "showChangeLog: " + checkVersion);
        if (!checkVersion) {
            showChangeLogDialog();
        } else {
            try {
                String thisVersion = ""+getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                String latestVersion = PreferenceUtil.getVersion(this);
                if (!latestVersion.equals(thisVersion)) {
                    PreferenceUtil.setVersion(this, thisVersion);
                    showChangeLogDialog();
                    GCMRegUtil
                        .getNewGCMRegID(this)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(s -> showMessage("GCM erneuert"), t -> showMessage("GCM erneuert fehlgeschlagen: " + t.getLocalizedMessage()));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "version", e);
            }
        }
    }

    private void showChangeLogDialog() {
        Log.d(TAG, "showChangeLogDialog");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ChangeLogDialogFragment f = new ChangeLogDialogFragment();
        f.show(ft, "ChangeLogDialogFragment");
    }

    private void showMailInputDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_inputmail, null, false);
        final TextInputLayout textInputLayout = (TextInputLayout) v.findViewById(R.id.dialog_email_layout);
        final EditText emailText = (EditText) v.findViewById(R.id.dialog_email_email);

        emailText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String email = emailText.getText().toString();
                if (email.isEmpty()) {
                    textInputLayout.setErrorEnabled(true);
                    textInputLayout.setError(getString(R.string.error_field_is_empty));
                } else {
                    textInputLayout.setErrorEnabled(false);
                    textInputLayout.setError(null);
                }
            }
        });

        final AlertDialog b = new AlertDialog.Builder(this)
            .setView(v)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
            .create();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(b.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        b.show();
        b.getWindow().setAttributes(lp);

        b.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
            String email = emailText.getText().toString();
            if (!email.isEmpty()) {
                addFriend(email);
                b.dismiss();
            } else {
                textInputLayout.setErrorEnabled(true);
                textInputLayout.setError(getString(R.string.error_field_is_empty));
            }
        });

    }

    private void setUpFriendsInDrawer(final List<ContactData> data) {
        List<ContactData> validContacts = new ArrayList<>(data.size());

        Log.d(TAG, "setUpFriendsInDrawer: " + data.size());
        navigationView.getMenu().removeGroup(2291);
        if (data.isEmpty()) {
            return;
        }

        for (ContactData c : data) {
            if (c.is_confirmed()) {
                validContacts.add(c);
            }
        }

        Menu subMenu = navigationView.getMenu().addSubMenu(2291, 22, 10, R.string.friends);
        subMenu.setGroupCheckable(2291, false, false);
        for (final ContactData c: validContacts) {
            final MenuItem xx = subMenu.add(c.getName());

//            PendingResult<People.LoadPeopleResult> result = Plus.PeopleApi.load(googleApiClient, c.getPlusID());
//            result.setResultCallback(loadPeopleResult -> {
//                Log.d(TAG, "setUpFriendsInDrawer.loadPeopleResult");
//                if (loadPeopleResult == null || loadPeopleResult.getPersonBuffer() == null) {
//                    return;
//                }
//                for (Person p : loadPeopleResult.getPersonBuffer()) {
//                    String photoUrl = p.getImage().getUrl();
//                    photoUrl = photoUrl.substring(0, photoUrl.length() - 2) + "100";
//                    Log.d(TAG, "setUpFriendsInDrawer.loadPeopleResult.url: " + photoUrl);
//                    Picasso
//                        .with(this)
//                        .load(photoUrl)
//                        .placeholder(android.R.drawable.ic_menu_help)
//                        .into(new Target() {
//                            @Override
//                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                                Log.d(TAG, "setUpFriendsInDrawer.loadPeopleResult.onBitmapLoaded: " + bitmap);
//                                BitmapDrawable bitmapDrawable = new BitmapDrawable(navigationView.getResources(), bitmap);
////                                BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
////                                int width = bitmap.getWidth();
////                                int height = bitmap.getHeight();
////                                Set<Integer> colors = new HashSet<>(0);
////                                for(int x = 0; x < width; x++){
////                                    for(int y = 0; y < height; y++){
////                                        colors.add(bitmap.getPixel(x,y));
////                                    }
////                                }
////                                Log.d(TAG, "setUpFriendsInDrawer.loadPeopleResult.onBitmapLoaded: " + colors.size() + " " + bitmapDrawable);
//
//                                xx.setIcon(bitmapDrawable);
//                                navigationView.getMenu().setGroupVisible(2291, false);
//                                navigationView.getMenu().setGroupVisible(2291, true);
//
////                                ViewGroup v = (ViewGroup) findViewById(android.R.id.content);
////                                ImageView iv = new ImageView(MainActivity.this);
////                                iv.setImageDrawable(bitmapDrawable);
////                                v.addView(iv);
//                            }
//                            @Override
//                            public void onBitmapFailed(Drawable errorDrawable) {
//                                Log.d(TAG, "setUpFriendsInDrawer.loadPeopleResult.onBitmapFailed");
//                            }
//                            @Override
//                            public void onPrepareLoad(Drawable placeHolderDrawable) {
//                                Log.d(TAG, "setUpFriendsInDrawer.loadPeopleResult.onPrepareLoad");
//                                xx.setIcon(placeHolderDrawable);
//                            }
//                        });

//                    Bitmap pic = StorageUtil.loadBitmap(MainActivity.this, c.getEmail());
//                    if (pic != null) {
//                        Log.d(TAG, "setUpFriendsInDrawer.loadPeopleResult.setIcon");
//                        xx.setIcon(new BitmapDrawable(getResources(), pic));
//                        navigationView.getMenu().setGroupVisible(2291, false);
//                        navigationView.getMenu().setGroupVisible(2291, true);
//                    } else {
//                        new ImageLoaderTask(bitmap -> {
//                            Log.d(TAG, "setUpFriendsInDrawer.loadPeopleResult.onFinished");
//                            StorageUtil.storeBitmap(MainActivity.this, c.getEmail(), bitmap);
//                            xx.setIcon(new BitmapDrawable(getResources(), bitmap));
//                            navigationView.getMenu().setGroupVisible(2291, false);
//                            navigationView.getMenu().setGroupVisible(2291, true);
//                        }).execute(photoUrl);
//                    }

//                    navigationView.getMenu().setGroupVisible(2291, false);
//                    navigationView.getMenu().setGroupVisible(2291, true);
//                }
//                loadPeopleResult.release();
//            });

//            xx.setIcon(R.mipmap.profil_placeholder);
            xx.setIcon(android.R.drawable.ic_menu_help);
            xx.setOnMenuItemClickListener(item -> {
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.ac_container);
                if (f != null && f instanceof GMapFragment) {
                    GMapFragment mapFragment = (GMapFragment) f;
                    mapFragment.update(c);
                } else {
                    showFragment(R.id.drawer_map);
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        // Workaround --> https://code.google.com/p/android/issues/detail?id=176300
        navigationView.getMenu().setGroupVisible(2291, false);
        navigationView.getMenu().setGroupVisible(2291, true);
    }

    private void loadProfileInformation(GoogleSignInAccount signInAccount) {
        Log.d(TAG, "loadProfileInformation: " + signInAccount);

//        TextView drawerNameText = (TextView) findViewById(R.id.f_drawer_profile_name);
//        TextView drawerMailText = (TextView) findViewById(R.id.f_drawer_profile_mail);
//
//        drawerNameText.setText(signInAccount.getDisplayName());
//        drawerMailText.setText(signInAccount.getEmail());

//        if (signInAccount.getPhotoUrl() != null) { // g+ image set
//            Log.d(TAG, "loadProfileInformation.loadPhoto");
//            Picasso
//                .with(this)
//                .load(signInAccount.getPhotoUrl())
//                .placeholder(android.R.mipmap.sym_def_app_icon)
//                .into((ImageView) findViewById(R.id.f_drawer_profile_image));
//        }

        // TODO load cover, no 'new' way atm.
//        if (mee.getCover() != null && mee.getCover().getCoverPhoto() != null && mee.getCover().getCoverPhoto().hasUrl()) { // g+ cover image set
//            Log.d(TAG, "loadProfileInformation.loadCover");
//            Picasso
//                .with(this)
//                .load(Uri.parse(mee.getCover().getCoverPhoto().getUrl()))
//                .placeholder(R.mipmap.background_poly)
//                .into(new Target() {
//                    @Override
//                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                        Log.d(TAG, "loadProfileInformation.Picasso.onBitmapLoaded");
//                        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.f_drawer_relativelayout);
//                        relativeLayout.setBackground(new BitmapDrawable(relativeLayout.getResources(), bitmap));
//                    }
//
//                    @Override
//                    public void onBitmapFailed(Drawable errorDrawable) {
//                    }
//
//                    @Override
//                    public void onPrepareLoad(Drawable placeHolderDrawable) {
//                    }
//                });
//        }
    }

    private void updateContacts(final List<ContactData> data){
        Log.d(TAG, "updateContacts: " + data.size() + " " + curContactDataList);

        curContactDataList.clear();
        curContactDataList.addAll(data);
        Collections.sort(curContactDataList);

        setUpFriendsInDrawer(curContactDataList);
        MyDBDelegate.Companion.mergeFriends(this, curContactDataList);

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.ac_container);
        if (f != null && f instanceof UpdateAble) {
            ((UpdateAble) f).onDataReceived(curContactDataList);
        }
    }

	/* Web Tasks */

	private void loadFriends() {
		Log.d(TAG, "loadFriends");
        Log.d(TAG, "loadFriends\n" + PreferenceUtil.getAccountToken(this));

        if (!isInetAvailable()) {
            showMessage(R.string.error_noNetworkConnectionFound);
        } else {
            Observable
                .zip(webService.getFriends(), webService.getNewFriends(), (networkResponse, networkResponse2) -> {
                    if (!networkResponse.isSuccess() || !networkResponse2.isSuccess()) {
                        showMessage(networkResponse.getError().getMessage());
                        throw new RuntimeException("" + networkResponse);
                    }
                    List<ContactData> data = new ArrayList<>();
                    data.addAll(networkResponse.getData().getFriends());
                    data.addAll(networkResponse2.getData().getNewFriends());
                    return data;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateContacts, errorAction);
		}
	}

	private void pokeFriend(final int which) {
        Log.d(TAG, "pokeFriend " + which  + " " + BuildConfig.GCM_ENABLED);
        if (!BuildConfig.GCM_ENABLED) {
            return;
        }

		if (!isInetAvailable()) {
            showMessage(R.string.error_noNetworkConnectionFound);
        } else {
            GameUtil.incrementPokeCount(this, getGoogleApiClient());
            webService
                .pokeFriend(which)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkResponse -> showMessage(networkResponse.isSuccess() ? getString(R.string.poked) : networkResponse.getError().getMessage()), errorAction);
        }
    }

    private void pokeFriends() {
		Log.d(TAG, "pokeFriends " + BuildConfig.GCM_ENABLED);
        if (!BuildConfig.GCM_ENABLED) {
            return;
        }
		if (!isInetAvailable()) {
            showMessage(R.string.error_noNetworkConnectionFound);
        } else {
            GameUtil.incrementPokeCount(this, getGoogleApiClient());
            webService
                .pokeFriends()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkResponse -> showMessage(networkResponse.isSuccess() ? getString(R.string.poked) : networkResponse.getError().getMessage()), errorAction);
		}
	}

    private void addFriend(final String email) {
        Log.d(TAG, "addFriend " + email);
        if (!isInetAvailable()) {
            showMessage(R.string.error_noNetworkConnectionFound);
        } else {
            webService
                .addFriend(email)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkResponse -> showMessage(networkResponse.isSuccess() ? networkResponse.getData().getMessage() : networkResponse.getError().getMessage()), errorAction);
        }
    }

	private void confirmFriend(final int which, final boolean isAccepted) {
		Log.d(TAG, "confirmFriend " + which + " " + isAccepted);
        if (!isInetAvailable()) {
            showMessage(R.string.error_noNetworkConnectionFound);
        } else {
            webService
                .confirmFriend(which, isAccepted)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkResponse -> {
                    if (networkResponse.isSuccess()) {
                        if (isAccepted){
                            GameUtil.incrementFriendCount(MainActivity.this, getGoogleApiClient());
                        }
                        Fragment f = getSupportFragmentManager().findFragmentById(R.id.ac_container);
                        if (f != null && f instanceof ContactListFragment) {
                            ContactListFragment ff = (ContactListFragment) f;
                            ff.onPersonConfirmed(which, isAccepted);
                        }
                        showMessage(networkResponse.getData().getMessage());
                    } else {
                        showMessage(networkResponse.getError().getMessage());
                    }
                }, errorAction);
        }
	}

    private void deleteFriend(final int which) {
        Log.d(TAG, "deleteFriend " + which);
        if (!isInetAvailable()) {
            showMessage(R.string.error_noNetworkConnectionFound);
        } else {
            webService
                .deleteFriend(which)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(networkResponse -> {
                    if (networkResponse.isSuccess()) {
                        List<ContactData> tmp = new ArrayList<>();
                        for (ContactData c : curContactDataList) {
                            if (c.getPerson_id() != which) {
                                tmp.add(c);
                            }
                        }
                        updateContacts(tmp);
                        Fragment f = getSupportFragmentManager().findFragmentById(R.id.ac_container);
                        if (f != null && f instanceof ContactListFragment) {
                            ContactListFragment mapFragment = (ContactListFragment) f;
                            mapFragment.onPersonDeleted(which);
                        }
                        showMessage(networkResponse.getData().getMessage());
                    } else {
                        showMessage(networkResponse.getError().getMessage());
                    }
                }, errorAction);
        }
    }

	/* GMapFragment */
	
	public void onMapRefreshClicked() {
        Log.d(TAG, "onMapRefreshClicked");
        startService(new Intent(this, UpdateLocationService.class));
        loadFriends();
        pokeFriends();
	}
	
    @Override
    public void onMapContactClicked(@NonNull final ContactData contact) {
        Log.d(TAG, "onMapContactClicked: " + contact);
        pokeFriend(contact.getPerson_id());
    }

    @Override
    public void onInviteContactClicked() {
        Log.d(TAG, "onInviteContactClicked");
        GameUtil.inviteFriends(this);
        GameUtil.incrementPromotionCount(this, getGoogleApiClient());
    }

    /* IContactListFragment */

    @Override
    public void onContactAddFriendClicked(@AddingStrategy final int addingStrategy) {
        Log.d(TAG, "onMapAddFriendClicked " + addingStrategy);

        if (!isInetAvailable()){
            showMessage(R.string.error_noNetworkConnectionFound);
            return;
        }

        switch (addingStrategy){
            case STRATEGY_ADDRESS_BOOK:
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                intent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE); // show contacts with an email only
                startActivityForResult(intent, REQUEST_PICK_CONTACT);
                break;

            case STRATEGY_EMAIL:
                showMailInputDialog();
                break;

            case STRATEGY_PLUS:
                showMessage("Soon...");
//                final PendingResult<People.LoadPeopleResult> result = Plus.PeopleApi.loadConnected(googleApiClient);
//                result.setResultCallback(new ResultCallback<People.LoadPeopleResult>() {
//                    @Override
//                    public void onResult(People.LoadPeopleResult loadPeopleResult) {
//                        Log.d(TAG, "loadConnected onResult");
//                        Iterator<Person> i = loadPeopleResult.getPersonBuffer().iterator();
//                        while (i.hasNext()) {
//                            Person p = i.next();
//                            Log.d(TAG, p.getDisplayName());
//                        }
//                        loadPeopleResult.release();
//                    }
//                });
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        People.LoadPeopleResult r = result.await();
//                        Log.d(TAG, "loadConnected onResult");
//                        Iterator<Person> i = r.getPersonBuffer().iterator();
//                        while (i.hasNext()) {
//                            Person p = i.next();
//                            Log.d(TAG, p.toString());
//                        }
//                        r.release();
//                    }
//                }).start();
                break;
            default:
                break;
        }
    }

    @Override
    public void onContactRefreshClicked() {
        Log.d(TAG, "onContactRefreshClicked");
        loadFriends();
    }

    @Override
    public void onContactClicked(final int which) {
        Log.d(TAG, "onContactClicked: " + which);
        showFragment(R.id.drawer_map);
    }

    @Override
    public void onContactPoked(final int which) {
        Log.d(TAG, "onContactPoked: " + which);
        pokeFriend(which);
    }

    @Override
    public void onContactDeleted(final int which) {
        Log.d(TAG, "onContactDeleted: " + which);
        deleteFriend(which);
    }

    @Override
    public void onContactConfirmed(final int which, final boolean isAccepted) {
        Log.d(TAG, "onContactDeleted: " + which);
        confirmFriend(which, isAccepted);
    }

    @Override
    public void onPokeAllClicked() {
        Log.d(TAG, "onPokeAllClicked");
        pokeFriends();
    }

    /* GameActivity */

    @Override
    protected void onSuccess(@NonNull GoogleSignInAccount signInAccount) {
        Log.d(TAG, "onSuccess");
        loadProfileInformation(signInAccount);
//        GameUtil.incrementAppUsage(this, getGoogleApiClient()); // TODO
    }

    @Override
    protected void onFailure(@NonNull SignInFailureReason reason) {
        Log.d(TAG, "onFailure: " + reason);
        showMessage(reason.toString());
    }

}