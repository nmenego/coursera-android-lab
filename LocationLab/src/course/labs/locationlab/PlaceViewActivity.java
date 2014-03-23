package course.labs.locationlab;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class PlaceViewActivity extends ListActivity implements LocationListener {
	private static final long FIVE_MINS = 5 * 60 * 1000;

	private static String TAG = "Lab-Location";

	private Location mLastLocationReading;
	private PlaceViewAdapter mAdapter;

	// default minimum time between new readings
	private long mMinTime = 5000;

	// default minimum distance between old and new readings.
	private float mMinDistance = 1000.0f;

	private LocationManager mLocationManager;

	// A fake location provider used for testing
	private MockLocationProvider mMockLocationProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			Log.e("D", "ONLINE !");
		} else {
			Log.e("D", "OFFLINE !");
		}

        // Set up the app's user interface
        // This class is a ListActivity, so it has its own ListView
        // ListView's adapter should be a PlaceViewAdapter
		ListView listView = getListView();
		registerForContextMenu(listView);
		mAdapter = new PlaceViewAdapter(getApplicationContext());

        // add a footerView to the ListView
        // You can use footer_view.xml to define the footer
		View footerView = ((LayoutInflater) getApplicationContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
				.inflate(R.layout.footer_view, listView, false);
		listView.setFooterDividersEnabled(true);
		getListView().addFooterView(footerView);
		setListAdapter(mAdapter);

        // When the footerView's onClick() method is called, it must issue the
        // following log call
		footerView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				log("Entered footerView.OnClickListener.onClick()");
		        
		        // footerView must respond to user clicks.
		        // Must handle 3 cases:
		        // 1) The current location is new - download new Place Badge. Issue the
		        // following log call:
		        // log("Starting Place Download");

		        // 2) The current location has been seen before - issue Toast message.
		        // Issue the following log call:
		        // log("You already have this location badge");
		        
		        // 3) There is no current location - response is up to you. The best
		        // solution is to disable the footerView until you have a location.
		        // Issue the following log call:
		        // log("Location data is not available");
				
				if(mLastLocationReading != null && age(mLastLocationReading) < FIVE_MINS) {
					boolean isSeenBefore = mAdapter.intersects(mLastLocationReading);
					if(isSeenBefore) {
						log("You already have this location badge");
						Toast.makeText(getApplicationContext(), "You already have this location badge.", Toast.LENGTH_LONG).show();
					} else {
						log("Starting Place Download");
						PlaceDownloaderTask task = new PlaceDownloaderTask(PlaceViewActivity.this);
						task.execute(mLastLocationReading);
					}
				} else {
					log("Location data is not available");
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		mMockLocationProvider = new MockLocationProvider(
				LocationManager.NETWORK_PROVIDER, this);

        // Check NETWORK_PROVIDER for an existing location reading.
        // Only keep this last reading if it is fresh - less than 5 minutes old.
		Location lastKnown = mLocationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (lastKnown != null && mLastLocationReading != null) {
			if ((age(lastKnown) - age(mLastLocationReading)) < FIVE_MINS) {
				// keep the data
				mLastLocationReading = lastKnown;
			} else {
				mLastLocationReading = null;
			}
		}

        // register to receive location updates from NETWORK_PROVIDER
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, this);

	}

	@Override
	protected void onPause() {

		mMockLocationProvider.shutdown();
		// unregister for location updates
        mLocationManager.removeUpdates(this);
		super.onPause();
	}

	// Callback method used by PlaceDownloaderTask
	public void addNewPlace(PlaceRecord place) {

		log("Entered addNewPlace()");
		mAdapter.add(place);

	}

	@Override
	public void onLocationChanged(Location currentLocation) {

        // Handle location updates
        // Cases to consider
        // 1) If there is no last location, keep the current location.
        // 2) If the current location is older than the last location, ignore
        // the current location
        // 3) If the current location is newer than the last locations, keep the
        // current location.
		if (mLastLocationReading == null) {
			// keep current location
			mLastLocationReading = currentLocation;
		} else {
			if ((age(mLastLocationReading) - age(currentLocation)) > 0) {
				mLastLocationReading = currentLocation;
			}
		}

	}

	@Override
	public void onProviderDisabled(String provider) {
		// not implemented
	}

	@Override
	public void onProviderEnabled(String provider) {
		// not implemented
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// not implemented
	}

	private long age(Location location) {
		return System.currentTimeMillis() - location.getTime();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.print_badges:
			ArrayList<PlaceRecord> currData = mAdapter.getList();
			for (int i = 0; i < currData.size(); i++) {
				log(currData.get(i).toString());
			}
			return true;
		case R.id.delete_badges:
			mAdapter.removeAllViews();
			return true;
		case R.id.place_one:
			mMockLocationProvider.pushLocation(37.422, -122.084);
			return true;
		case R.id.place_invalid:
			mMockLocationProvider.pushLocation(0, 0);
			return true;
		case R.id.place_two:
			mMockLocationProvider.pushLocation(38.996667, -76.9275);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private static void log(String msg) {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.i(TAG, msg);
	}

}
