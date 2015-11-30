package de.mm.android.longitude.common;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

import de.mm.android.longitude.R;

public class MyInfoWindowAdapter implements InfoWindowAdapter {
	private View myContentView;

	public MyInfoWindowAdapter(LayoutInflater inflater) {
		myContentView = inflater.inflate(R.layout.map_infowindow, null, false);
	}
	
	@Override
	public View getInfoWindow(Marker marker) {
		return null;
	}
	
	@Override
	public View getInfoContents(Marker marker) {
		TextView title = (TextView) myContentView.findViewById(R.id.mapmarkerinfo_title);
		TextView text = (TextView) myContentView.findViewById(R.id.mapmarkerinfo_snippet);
		title.setText(marker.getTitle());
		text.setText(marker.getSnippet());
		return myContentView;
	}

}