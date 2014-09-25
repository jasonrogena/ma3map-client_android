package ke.co.ma3map.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import ke.co.ma3map.android.handlers.Data;

public class Map extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private final String TAG = "Map";

    private final int DEFAULT_ZOOM = 10;

    private GoogleMap googleMap;
    private LocationClient locationClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //check if google services is enabled
        final int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS){
            Toast.makeText(this, "This app requires Google Play Services inorder to work", Toast.LENGTH_LONG).show();
            finish();
        }

        googleMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        googleMap.setMyLocationEnabled(true);

        locationClient = new LocationClient(this, this, this);

        //initialize a thread to fetch the route data
        RouteDataTask routeDataTask = new RouteDataTask();
        routeDataTask.execute(0);
    }

    /**
     * Make sure you initialize all hardware resources here because they will be
     * released in onPause
     */
    @Override
    protected void onResume() {
        super.onResume();

        if(!locationClient.isConnected()){
            locationClient.connect();
        }
    }

    /**
     * Release all hardware resources here and not onDestroy to avoid blocking the resources
     * when not using them
     */
    @Override
    protected void onPause(){
        super.onPause();

        locationClient.disconnect();
    }

    /**
     * This method zooms in on the devices location.
     * Should be run after resources initialized i.e. in onResume()
     */
    private void zoomInOnLocation(){
        Location myLocation = locationClient.getLastLocation();

        if(myLocation != null){
            LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, DEFAULT_ZOOM));
            Log.d(TAG, "Zoomed in on user's position");
        }
        else {
            Log.w(TAG, "Unable to get user's location");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        zoomInOnLocation();
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private class RouteDataTask extends AsyncTask<Integer, Integer, String>{

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(Map.this, "", "Loading, please wait...", true);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values[0]);
            Log.i(TAG, "Progress people");
        }

        @Override
        protected String doInBackground(Integer... integers) {
            Log.i(TAG, "About to download route data from the server");
            return Data.downloadAllRouteData(Map.this);
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);

            progressDialog.dismiss();
        }
    }
}
