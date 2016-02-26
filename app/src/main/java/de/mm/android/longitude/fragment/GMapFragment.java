package de.mm.android.longitude.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.pkmmte.view.CircularImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.mm.android.longitude.R;
import de.mm.android.longitude.base.BaseFragment;
import de.mm.android.longitude.common.TransparentTileProvider;
import de.mm.android.longitude.model.ContactData;
import de.mm.android.longitude.util.PreferenceUtil;
import de.mm.android.longitude.util.StorageUtil;
import de.mm.android.longitude.view.MultiDrawable;

public class GMapFragment
        extends BaseFragment
        implements UpdateAble, BackwardAble {

    private static final String TAG = GMapFragment.class.getSimpleName();
    private static final String ARG_CONTACTS = "contacts";

    public interface IMapFragment {
        void onMapRefreshClicked();
        void onMapContactClicked(@NonNull final ContactData contact);
    }

    public static GMapFragment newInstance(ArrayList<ContactData> data) {
        GMapFragment f = new GMapFragment();
        Bundle b = new Bundle();
        b.putParcelableArrayList(ARG_CONTACTS, data);
        f.setArguments(b);
        return f;
    }

    private IMapFragment listener;
    private GoogleMap googleMap;
    private MenuItem refreshMenuItem;
    private TileOverlay weatherOverlay;
    private ClusterManager<ContactData> clusterManager;
    private String overlayValue = "none";
    private BottomSheetBehavior bottomSheetBehavior;
    @Bind(R.id.item_sheet_image) ImageSwitcher imageView;
    @Bind(R.id.item_sheet_name) TextSwitcher nameView;
    @Bind(R.id.item_sheet_latestupdate) TextSwitcher updateView;
    @Bind(R.id.item_sheet_address) TextSwitcher addressView;
    // current State
    private ContactData currentContact;
    private ArrayList<ContactData> contactList;

	/* LifeCycle */

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof IMapFragment) {
            listener = (IMapFragment) getActivity();
        } else {
            throw new IllegalStateException(getActivity().getClass().getName() + " must implement " + IMapFragment.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        contactList = new ArrayList<>();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.f_map, menu);
        refreshMenuItem = menu.findItem(R.id.menu_f_map_update);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_f_map_update:
                if (isInetAvailable()) {
                    listener.onMapRefreshClicked();
                    if (refreshMenuItem != null) {
                        refreshMenuItem.setActionView(R.layout.indeterminate_progress);
                        refreshMenuItem.expandActionView();
                    }
                } else {
                    showMessage(R.string.error_noNetworkConnectionFound);
                }
                return true;

            case R.id.menu_f_map_mapMode:
                showMapModeDialog();
                return true;

            case R.id.menu_f_map_weather:
                showWeatherDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.f_gmap, container, false);
        ButterKnife.bind(this, v);

        Toolbar toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_drawer);
        toolbar.setSubtitle(R.string.title_map);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        prepareMap(savedInstanceState);
        prepareBottomSheet();

        bottomSheetBehavior = BottomSheetBehavior.from(v.findViewById(R.id.item_sheet_content));
        return  v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        outState.putParcelableArrayList(ARG_CONTACTS, contactList);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (googleMap != null) {
            Location l = googleMap.getMyLocation();
            if (l != null) {
                PreferenceUtil.setLatestLocation(getActivity(), l);
            }
            CameraPosition cp = googleMap.getCameraPosition();
            if (cp != null) {
                PreferenceUtil.setMapZoom(getActivity(), cp.zoom);
            }
        }
    }

	/* Map */

    private void prepareMap(@Nullable Bundle savedInstanceState) {
        SupportMapFragment smf = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.f_map_googlemap);
        smf.getMapAsync(googleMap -> {
            this.googleMap = googleMap;
            setupMap();
            setupClusterManager();
            if (getArguments().containsKey(ARG_CONTACTS)) {
                onDataReceived(getArguments().getParcelableArrayList(ARG_CONTACTS));
            } else if (savedInstanceState != null) {
                onDataReceived(savedInstanceState.getParcelableArrayList(ARG_CONTACTS));
            } else {
                throw new IllegalStateException("Keine Argumente und Kein SavedState..., dann ist halt Feierabend");
            }
        });
    }

    private void setupMap() {
        Log.d(TAG, "setUpMap");

        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);

        googleMap.setMyLocationEnabled(true);
        googleMap.setMapType(PreferenceUtil.getMapMode(getActivity()));
        googleMap.setOnMapClickListener(latLng -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        TypedValue value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true);
        int actionBarSize = getResources().getDimensionPixelSize(value.resourceId);
        googleMap.setPadding(0, actionBarSize, 0, 0);

        Location l = PreferenceUtil.getLatestLocation(getActivity());
        if (l != null) {
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition(new LatLng(l.getLatitude(), l.getLongitude()), PreferenceUtil.getMapZoom(getActivity()), 0, 0)));
        }
    }

    private void setupClusterManager() {
        clusterManager = new ClusterManager<>(getActivity(), googleMap);
        googleMap.setOnCameraChangeListener(clusterManager);
        googleMap.setOnMarkerClickListener(clusterManager);
        googleMap.setOnInfoWindowClickListener(clusterManager);

        clusterManager.setRenderer(new PersonRenderer());
        clusterManager.setOnClusterItemClickListener(contactData -> {
            currentContact = contactData;
            zoomInContact(contactData);
            fillSheetInfo(contactData);
            return false;
        });
        clusterManager.setOnClusterClickListener(cluster -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return false;
        });
    }

    /* Sheet */

    @OnClick(R.id.item_sheet_fab)
    void onFABClick(View v){
        listener.onMapContactClicked(currentContact);
    }

    private void prepareBottomSheet() {
        Animation in = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left);
        Animation out = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right);

        nameView.setInAnimation(in);
        nameView.setOutAnimation(out);
        addressView.setInAnimation(in);
        addressView.setOutAnimation(out);
        updateView.setInAnimation(in);
        updateView.setOutAnimation(out);
        imageView.setInAnimation(in);
        imageView.setOutAnimation(out);

        imageView.setFactory(() -> {
            Log.w(TAG, "makeView imageView");
            CircularImageView v = new CircularImageView(getActivity());
            v.setBorderWidth(-1);
            return v;
        });

        nameView.setFactory(() -> {
            Log.w(TAG, "makeView nameView");
            TextView v = new TextView(getActivity());
            v.setTextAppearance(getActivity(), android.R.style.TextAppearance_Large);
            v.setTextColor(getResources().getColor(R.color.accentColor));
            return v;
        });
        addressView.setFactory(() -> {
            TextView v = new TextView(getActivity());
            v.setTextAppearance(getActivity(), android.R.style.TextAppearance_Small);
            v.setTextColor(getResources().getColor(android.R.color.white));
            v.setSingleLine(true);
            v.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            v.setMarqueeRepeatLimit(-1);
            v.setMaxLines(1);
            v.setSelected(true);
            v.setFocusable(true);
            v.setFocusableInTouchMode(true);
            return v;
        });
        updateView.setFactory(() -> {
            TextView v = new TextView(getActivity());
            v.setTextAppearance(getActivity(), android.R.style.TextAppearance_Small);
            v.setTextColor(getResources().getColor(android.R.color.white));
            return v;
        });
    }

    private void fillSheetInfo(ContactData data){
        Log.d(TAG, "fillSheetInfo");
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        nameView.setText(data.getName());
        updateView.setText(data.getUpdatedOnFormatted());
        addressView.setText(data.getAddress());
        imageView.setImageDrawable(new BitmapDrawable(getResources(), StorageUtil.loadBitmap(getActivity(), data.getEmail())));
    }

    /* Stuff */

    private void showMapModeDialog() {
        Log.d(TAG, "showMapModeDialog");
        int mode = PreferenceUtil.getMapMode(getActivity());
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.menu_mapmode)
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_menu_mapmode)
                .setSingleChoiceItems(R.array.mapmode, mode - 1, (dialog, which) -> {
                    // mapType starts with 1, diaglogItems with 0
                    googleMap.setMapType(which + 1);
                    PreferenceUtil.setMapMode(getActivity(), which + 1);
                    dialog.dismiss();
                })
                .create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.getWindow().setWindowAnimations(R.style.dialogAnimation);
        alertDialog.show();
    }

    private void showWeatherDialog() {
        Log.d(TAG, "showWeatherDialog");
        final String[] weatherOptions = getResources().getStringArray(R.array.weather_overlay);
        final String[] weatherOptionsValues = getResources().getStringArray(R.array.weather_overlay_values);
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.menu_weather)
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_menu_view)
                .setSingleChoiceItems(weatherOptionsValues, Arrays.asList(weatherOptions).indexOf(overlayValue), (dialog, which) -> {
                    overlayValue = weatherOptions[which];
                    showWeatherOverLay(overlayValue);
                    dialog.dismiss();
                })
                .create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.getWindow().setWindowAnimations(R.style.dialogAnimation);
        alertDialog.show();
    }

    private void showWeatherOverLay(@NonNull String typ) {
        Log.d(TAG, "showWeatherOverLay " + typ);
        if (weatherOverlay != null) {
            weatherOverlay.remove();
            weatherOverlay = null;
        }

        if (!"none".equals(typ)) {
            weatherOverlay = googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(new TransparentTileProvider(typ)));
            if (!isInetAvailable()) {
                showMessage(R.string.weather_dialog_offline_msg);
            }
        }

    }

    private void zoomInContact(@NonNull ContactData c) {
        Log.d(TAG, "zoomInContact: " + c);
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(c.getLatitude(), c.getLongitude()), 15, 0, 0)));
    }

    private void showMarkers(@NonNull final List<ContactData> contactsList) {
        Log.d(TAG, "showMarker");
        if (googleMap == null) {
            return;
        }

        googleMap.clear();
        contactList.clear();
        contactList.addAll(contactsList);

        clusterManager.clearItems();
        clusterManager.setRenderer(new PersonRenderer()); // WorkAround --> http://stackoverflow.com/questions/22287207/clustermanager-repaint-markers-of-google-maps-v2-utils
        clusterManager.addItems(contactsList);
        clusterManager.cluster();
    }

    private Circle drawCircle(LatLng location, double accuracy) {
        CircleOptions options = new CircleOptions();
        options.center(location);
        options.radius(accuracy); //Radius in meters
        options.fillColor(getResources().getColor(R.color.accentColorTransparent));
        options.strokeColor(getResources().getColor(R.color.primaryColorTransparent));
        options.strokeWidth(5);
        return googleMap.addCircle(options);
    }

    public void update(@NonNull ContactData contact) {
        Log.d(TAG, "onDataReceived: " + contact);
        currentContact = contact;
        zoomInContact(contact);
        fillSheetInfo(contact);
    }

    /** {@link UpdateAble} */

    @Override
    public void onConnectivityUpdate(boolean isConnected) {
        setInetAvailable(isConnected);
    }

    public void onDataReceived(@Nullable final List<ContactData> contacts) {
        if (contacts == null) {
            Log.d(TAG, "onDataReceived: with 0 Values");
        } else {
            Log.d(TAG, "onDataReceived: " + contacts.size());
            List<ContactData> confirmedContacts = new ArrayList<>();
            for (ContactData c : contacts) {
                if (c.is_confirmed()) {
                    confirmedContacts.add(c);
                }
            }
            showMarkers(confirmedContacts);
        }
        if (refreshMenuItem != null) {
            refreshMenuItem.collapseActionView();
            refreshMenuItem.setActionView(null);
        }
    }

    /** {@link BackwardAble} */

    @Override
    public boolean onStepBack() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return true;
        }
        return false;
    }

    /* Clazz */

    private class PersonRenderer extends DefaultClusterRenderer<ContactData> {
        private final IconGenerator clusterIconGenerator;
        private final ImageView clusterImageView;
        private final IconGenerator singleIconGenerator;
        private final ImageView singleImageView;
        private final int mDimension;
        private final SparseArray<Circle> drawnCircles;

        public PersonRenderer() {
            super(getActivity(), googleMap, clusterManager);
            View multiProfile = getLayoutInflater(null).inflate(R.layout.multi_profile, null);
            clusterIconGenerator = new IconGenerator(getActivity());
            clusterIconGenerator.setContentView(multiProfile);
            clusterImageView = (ImageView) multiProfile.findViewById(R.id.image);

            View singleProfile = getLayoutInflater(null).inflate(R.layout.single_profile, null);
            singleIconGenerator = new IconGenerator(getActivity());
            singleIconGenerator.setContentView(singleProfile);
            singleImageView = (ImageView) singleProfile.findViewById(R.id.image);

            mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            drawnCircles  = new SparseArray<>();
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<ContactData> cluster, MarkerOptions markerOptions) {
            super.onBeforeClusterRendered(cluster, markerOptions);

            int clusterSize = cluster.getSize();
            Log.d(TAG, "onBeforeClusterRendered: " + clusterSize);
            List<Drawable> profilePhotos = new ArrayList<>(Math.min(4, clusterSize));
            List<String> names = new ArrayList<>(Math.min(4, clusterSize));

//            double lat = 0;
//            double lon = 0;
            for (ContactData c : cluster.getItems()) {
                if (profilePhotos.size() == 4) break;
                Bitmap bitmap = StorageUtil.loadBitmap(getActivity(), c.getEmail());
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                drawable.setBounds(0, 0, mDimension, mDimension);
                profilePhotos.add(drawable);
                names.add(c.getName());
//                lat += c.getLatitude();
//                lon += c.getLongitude();
            }
//            lat /= clusterSize;
//            lon /= clusterSize;

            MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
            multiDrawable.setBounds(0, 0, mDimension, mDimension);
            clusterImageView.setImageDrawable(multiDrawable);

            for (ContactData c : cluster.getItems()) {
                Circle circle = drawnCircles.get(c.getPerson_id());
                if (circle != null) {
                    circle.remove();
                }
            }

            Bitmap icon = clusterIconGenerator.makeIcon(String.valueOf(clusterSize));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
            markerOptions.snippet(names.toString());
//            markerOptions.position(new LatLng(lat, lon));
            markerOptions.alpha(.7f);
        }

        @Override
        protected void onBeforeClusterItemRendered(ContactData c, MarkerOptions markerOptions) {
            Log.d(TAG, "onBeforeClusterItemRendered: " + c.getName());

            Bitmap bitmap = StorageUtil.loadBitmap(getActivity(), c.getEmail());
            if (bitmap != null) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                drawable.setBounds(0, 0, mDimension, mDimension);
                singleImageView.setImageDrawable(drawable);

                Bitmap icon = singleIconGenerator.makeIcon();
                markerOptions.snippet(c.getName() + "\n" + c.getUpdatedOnFormatted());
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
                markerOptions.alpha(.7f);
            }

            Circle circle = drawnCircles.get(c.getPerson_id());
            if (circle != null) {
                circle.remove();
            }
            circle = drawCircle(c.getPosition(), c.getAccuracy());
            drawnCircles.put(c.getPerson_id(), circle);
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster<ContactData> cluster) {
            return cluster.getSize() > 2;
        }

    }

}