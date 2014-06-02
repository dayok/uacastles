package com.ukrcastles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

public class MapActivity extends FragmentActivity {

	GoogleMap map;
	AutoCompleteTextView autoComplete;
	private Handler mHandler = new Handler();
	private Handler spHandler = new Handler();
	Handler сHandler = new Handler();
	DataBaseHelper myDbHelper;
	LatLng myCoord = null;
	LatLng myNearCoord = null;
	SQLiteDatabase db;
	ArrayList<String> it;
	GPSTracker gpsTracker;
	int i = 0;
	Marker marker;
	String prefix;
	private static final int NEW_MENU_ID = Menu.FIRST + 1;

	public int setMyLocation() {
		gpsTracker = new GPSTracker(MapActivity.this);
		if (gpsTracker.canGetLocation()) {
			String stringLatitude = String.valueOf(gpsTracker.latitude);
			String stringLongitude = String.valueOf(gpsTracker.longitude);
			if (stringLatitude.equals("0.0")) {
				stringLatitude = "49.853192";
				stringLongitude = "24.024499";
			}
			String country = gpsTracker.getCountryName(MapActivity.this);
			String city = gpsTracker.getLocality(MapActivity.this);
			String addressLine = gpsTracker.getAddressLine(MapActivity.this);
			// map.clear();
			myCoord = new LatLng(Double.parseDouble(stringLatitude),
					Double.parseDouble(stringLongitude));
			map.addMarker(new MarkerOptions()
					.title(getString(getResources().getIdentifier(
							"you_here_string" + prefix, "string",
							getPackageName())))
					.snippet(
							country + ", " + city + "\n" + addressLine + "\n"
									+ stringLatitude + ", " + stringLongitude)
					.position(myCoord));

			map.moveCamera(CameraUpdateFactory.newLatLngZoom(myCoord, 8));

		}
		return 1;
	}

	private class AsyncMaps extends AsyncTask<String, Void, ArrayList<String>> {

		@SuppressLint("NewApi")
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			myDbHelper = new DataBaseHelper(MapActivity.this);
			try {
				myDbHelper.createDataBase();
			} catch (IOException ioe) {
				throw new Error("Unable to create database");
			}
			try {
				myDbHelper.openDataBase();
			} catch (SQLException sqle) {
				throw sqle;
			}

			mHandler.post(new Runnable() {
				@SuppressLint("NewApi")
				@SuppressWarnings({ "unchecked", "rawtypes" })
				public void run() {
					// check if GPS enabled
					// Get a handle to the Map Fragment
					if (map == null) {
						map = ((MapFragment) getFragmentManager()
								.findFragmentById(R.id.map)).getMap();
						map.setMyLocationEnabled(true);
						gpsTracker = new GPSTracker(MapActivity.this);
						setMyLocation();
					}
					db = myDbHelper.getWritableDatabase();
					Cursor c = db.query("info_data", new String[] { "*" },
							null, null, null, null, null);

					float distance;
					Map<String, Float> mapDist = new HashMap<>();
					for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {

						String coordinates = c.getString(c
								.getColumnIndex("coordinates"));
						String name = c.getString(
								c.getColumnIndex("name" + prefix)).replace(";",
								",");
						String description = c.getString(
								c.getColumnIndex("description" + prefix))
								.replace(";", ",");
						String image = c.getString(c.getColumnIndex("image"));

						LatLng coord = new LatLng(Double
								.parseDouble(coordinates.split(",")[0]), Double
								.parseDouble(coordinates.split(",")[1]));
						Location locMy = new Location("");
						locMy.setLatitude(myCoord.latitude);
						locMy.setLongitude(myCoord.longitude);

						Location locTo = new Location("");
						locTo.setLatitude(coord.latitude);
						locTo.setLongitude(coord.longitude);
						distance = locMy.distanceTo(locTo);

						// mapDist.put(new LatLng(coord.latitude,
						// coord.longitude), distance);

						mapDist.put(coordinates + ";" + name + ";"
								+ description + ";" + image + ";" + distance,
								distance);
					}

					List list = new LinkedList(mapDist.entrySet());
					Collections.sort(list, new Comparator() {
						public int compare(Object o1, Object o2) {
							return ((Comparable) ((Map.Entry) (o1)).getValue())
									.compareTo(((Map.Entry) (o2)).getValue());
						}
					});

					Map<String, Float> result = new LinkedHashMap<String, Float>();
					for (Iterator it = list.iterator(); it.hasNext();) {
						Map.Entry<String, Float> entry = (Map.Entry) it.next();
						result.put(entry.getKey(), entry.getValue());
					}
					myNearCoord = new LatLng(Double.parseDouble(result.keySet()
							.iterator().next().toString().split(";")[0]
							.split(",")[0]), Double.parseDouble(result.keySet()
							.iterator().next().toString().split(";")[0]
							.split(",")[1]));

					it = new ArrayList<String>();
					for (Entry<String, Float> ent : result.entrySet()) {
						it.add(ent.getKey());
					}
				}
			});

			return it;
		}

		@Override
		protected void onPreExecute() {
			spHandler.post(new Runnable() {
				@SuppressLint("NewApi")
				public void run() {
					setProgressBarIndeterminateVisibility(true);
				    setProgressBarVisibility(true);
				}
			});
		}

		@Override
		protected void onPostExecute(ArrayList<String> aList) {
			int count = it.size();

			for (int i = 0; i < count; i++) {
				String coordinates = it.get(i).toString().split(";")[0];
				String name = it.get(i).toString().split(";")[1];
				String description = it.get(i).toString().split(";")[2];
				String image = it.get(i).toString().split(";")[3];
				String distance = it.get(i).toString().split(";")[4];
				int distanceMeter = Math
						.round(Float.parseFloat(distance) * 100) / 100;
				if (distanceMeter < 1000)
					distance = String.valueOf(distanceMeter) + " m";
				else {
					float distanceKilometer = Float.valueOf(distanceMeter);
					distanceKilometer = distanceKilometer / 1000;
					distance = String.valueOf(distanceKilometer);
					distance = distance.replace(".", ",");
					if (distance.split(",")[1].length() >= 3)
						distance = distance.split(",")[0] + ","
								+ distance.split(",")[1].substring(0, 2)
								+ " km";
					else {
						distance = distance + " km";
					}

				}
				LatLng coord = new LatLng(Double.parseDouble(coordinates
						.split(",")[0]), Double.parseDouble(coordinates
						.split(",")[1]));

				// Drawable d= getResources().getDrawable(getResources()
				// .getIdentifier("drawable/" + image, null,
				// getPackageName()));
				// d.setLevel(1234);
				// BitmapDrawable bd=(BitmapDrawable) d.getCurrent();
				// BitmapFactory.Options o = new BitmapFactory.Options();
				// o.inJustDecodeBounds = true;
				// Bitmap b=bd.getBitmap();
				//
				// Bitmap bhalfsize=Bitmap.createScaledBitmap(b,
				// b.getWidth()/15,b.getHeight()/15, false);

				map.addMarker(new MarkerOptions()
						.title(name + " - " + distance)
						.snippet(
								description.substring(0, 20)
										+ "...\n("
										+ getString(getResources()
												.getIdentifier(
														"touch_here" + prefix,
														"string",
														getPackageName()))
										+ ")")
						// .icon(BitmapDescriptorFactory.fromBitmap(bhalfsize)).anchor(0.0f,
						// 1.0f)
						.icon(BitmapDescriptorFactory
								.fromResource(getResources().getIdentifier(
										"drawable/ico_" + image, null,
										getPackageName()))).anchor(0.0f, 1.0f)
						.position(coord));

			}

			db.close();
	        setProgressBarIndeterminateVisibility(false);
	        setProgressBarVisibility(false);
			map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
				@Override
				public void onInfoWindowClick(Marker marker) {
					String title = marker.getTitle();
					if (title.equals(getString(getResources().getIdentifier(
							"add_place_string" + prefix, "string",
							getPackageName())))) {
						Intent intent = new Intent(MapActivity.this,
								AddActivity.class);
						intent.putExtra(
								"position",
								marker.getPosition().latitude + ","
										+ marker.getPosition().longitude);
						startActivity(intent);
					} else {
						if (title.equals(getString(getResources()
								.getIdentifier("you_here_string" + prefix,
										"string", getPackageName())))) {
						} else {
							Intent intent = new Intent(MapActivity.this,
									InfoActivity.class);
							intent.putExtra("title", title.split("-")[0].trim());
							startActivity(intent);
						}
					}
				}
			});

			/*
			 * map.setOnMapClickListener(new OnMapClickListener() {
			 * 
			 * @Override public void onMapClick(LatLng point) { if (marker !=
			 * null) marker.remove(); marker = map.addMarker(new MarkerOptions()
			 * .title(getString(getResources().getIdentifier("add_place_string"
			 * + prefix, "string", getPackageName()))).position(point)
			 * .draggable(true)); } });
			 */
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefix = prefs.getString("prefix", "");
		if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
			setContentView(R.layout.activity_map);
			AsyncMaps maps = new AsyncMaps();
			maps.execute();

			if (Build.VERSION.SDK_INT >= 15) {
				ActionBar bar = getActionBar();
				bar.setDisplayHomeAsUpEnabled(true);
			}

		} else {
			Toast toast = Toast.makeText(
					getApplicationContext(),
					getString(getResources().getIdentifier(
							"no_google_play_services" + prefix, "string",
							getPackageName())), Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_map, menu);
		menu.add(
				0,
				NEW_MENU_ID,
				0,
				getString(getResources().getIdentifier("change_view" + prefix,
						"string", getPackageName())));
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, StartActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.action_list_map:
			Intent in = new Intent(MapActivity.this, PlacesActivity.class);
			startActivity(in);
			return true;
		case NEW_MENU_ID: {
			if (map.getMapType() == GoogleMap.MAP_TYPE_NORMAL)
				map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
			else if (map.getMapType() == GoogleMap.MAP_TYPE_HYBRID)
				map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			return true;
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_map, container,
					false);
			return rootView;
		}
	}
}
