package de.mm.android.longitude.fragment;

import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TabHost;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.mm.android.longitude.R;
import de.mm.android.longitude.common.Constants;
import de.mm.android.longitude.database.MyDBDelegate;
import de.mm.android.longitude.model.ProcessEntry;
import de.mm.android.longitude.util.PreferenceUtil;

public class ProcessFragment extends Fragment implements Slideable {
    private static final String TAG = ProcessFragment.class.getSimpleName();

    public static ProcessFragment newInstance() {
        return new ProcessFragment();
    }

    private GoogleMap googleMap;
    private TileOverlay tileOverlay;
    @Bind(R.id.f_process_floating_recent)
    FloatingActionButton fab;

	/* LifeCycle */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.f_process, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_f_process_mapMode:
                showMapModeDialog();
                return true;

            case R.id.menu_f_process_heatmap:
                toggleHeatMap();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_process, container, false);
        ButterKnife.bind(this, view);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.mipmap.ic_drawer);
        toolbar.setSubtitle(R.string.title_progress);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        SupportMapFragment smf = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.f_process_googlemap);
        smf.getMapAsync(googleMap -> {
            ProcessFragment.this.googleMap = googleMap;
            setUpMap();
            showDate(Calendar.getInstance(), null);
        });

        fab.setOnClickListener(v -> showPickerDialog());
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        PreferenceUtil.setMapZoom(getActivity(), googleMap.getCameraPosition().zoom);
    }

	/* Stuff */

    private void showMapModeDialog() {
        Log.d(TAG, "showMapModeDialog");
        int mode = PreferenceUtil.getMapMode(getActivity());
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.menu_mapmode)
            .setCancelable(true)
            .setIcon(android.R.drawable.ic_menu_mapmode)
            .setSingleChoiceItems(R.array.mapmode, mode - 1, (dialogInterface, which) -> {
                // mapType starts with 1, diaglogItems with 0
                googleMap.setMapType(which + 1);
                PreferenceUtil.setMapMode(getActivity(), which + 1);
                dialogInterface.dismiss();
            })
            .create();

        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().setWindowAnimations(R.style.dialogAnimation);
        dialog.show();
    }

    private void toggleHeatMap() {
        if (tileOverlay != null) {
            tileOverlay.remove();
            tileOverlay = null;
        } else {
            List<ProcessEntry> l = MyDBDelegate.selectProcess(getActivity(), null, null);
            List<LatLng> heatList = new ArrayList<>(l.size());
            for (ProcessEntry p : l) {
                heatList.add(new LatLng(p.getLat(), p.getLon()));
            }

            HeatmapTileProvider heatmapTileProvider = new HeatmapTileProvider.Builder()
                    .data(heatList)
                    .build();
            tileOverlay = googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapTileProvider));
        }
    }

    private static void setNewTab(TabHost tabHost, String tag, String title, int contentID) {
        TabHost.TabSpec tabSpec = tabHost.newTabSpec(tag);
        tabSpec.setIndicator(title);
        tabSpec.setContent(contentID);
        tabHost.addTab(tabSpec);
    }

    private void showPickerDialog() {
        Log.d(TAG, "showPickerDialog");

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.double_picker, null, false);
        final TabHost tabHost = (TabHost) view.findViewById(R.id.tabHost);
        tabHost.setup();
        setNewTab(tabHost, "tab1", "Zeitpunkt", R.id.tab1);
        setNewTab(tabHost, "tab2", "Zeitraum", R.id.tab2);

        final DatePicker day = (DatePicker) view.findViewById(R.id.dialog_ddp_day);
        final DatePicker min = (DatePicker) view.findViewById(R.id.dialog_ddp_period_min);
        final DatePicker max = (DatePicker) view.findViewById(R.id.dialog_ddp_period_max);

        Calendar today = Calendar.getInstance();

        day.setMaxDate(today.getTimeInMillis());
        min.setMaxDate(today.getTimeInMillis());
        max.setMaxDate(today.getTimeInMillis());

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.menu_date)
            .setCancelable(true)
            .setIcon(android.R.drawable.ic_menu_recent_history)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, (dialogInterface, which) -> dialogInterface.dismiss())
            .setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
                Calendar minDate = Calendar.getInstance();
                Calendar maxDate = Calendar.getInstance();
                minDate.clear();
                maxDate.clear();
                if (tabHost.getCurrentTabTag().equals("tab1")) {
                    minDate.set(day.getYear(), day.getMonth(), day.getDayOfMonth());
                    maxDate.set(day.getYear(), day.getMonth(), day.getDayOfMonth());
                    maxDate.add(Calendar.DAY_OF_MONTH, 1);
                } else {
                    minDate.set(min.getYear(), min.getMonth(), min.getDayOfMonth());
                    maxDate.set(max.getYear(), max.getMonth(), max.getDayOfMonth());
                }
                showDate(minDate, maxDate);
                Snackbar.make(fab, Constants.DATEFORMAT_LOCAL.format(minDate.getTime()) + " - " + Constants.DATEFORMAT_LOCAL.format(maxDate.getTime()), Snackbar.LENGTH_LONG).show();
            })
            .create();

        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().setWindowAnimations(R.style.dialogAnimation);
        dialog.show();
    }

    private void setUpMap() {
        Log.d(TAG, "setUpMap");
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);

        googleMap.setMyLocationEnabled(true);
        googleMap.setMapType(PreferenceUtil.getMapMode(getActivity()));

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

    private void showDate(Calendar min, Calendar max) {
        googleMap.clear();

        List<ProcessEntry> process = MyDBDelegate.selectProcess(getActivity(), min, max);

        PolylineOptions options = new PolylineOptions();
        options.color(getResources().getColor(R.color.black_transparent));
        for (ProcessEntry p : process) {
            options.add(new LatLng(p.getLat(), p.getLon()));
        }

        googleMap.addPolyline(options);
    }

    /** {@link Slideable} */

    @Override
    public void onSilde(float offset) {
        if (fab != null) {
            fab.setTranslationX(offset * 200);
        }
    }

}