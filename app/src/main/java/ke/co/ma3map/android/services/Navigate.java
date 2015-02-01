package ke.co.ma3map.android.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import ke.co.ma3map.android.carriers.Commute;

/**
 * This class implements the IntentService that handles navigation in ma3map.
 * Navigation in this service should be considered event based and updated whenever
 * the onLocationChanged method is called
 *
 * Created by jrogena on 28/01/2015.
 */
public class Navigate extends IntentService
                        implements GoogleApiClient.ConnectionCallbacks,
                                    GoogleApiClient.OnConnectionFailedListener,
                                    LocationListener{

    private static final String TAG = "ma3map.Navigate";
    private Commute commute;
    private GoogleApiClient googleApiClient;

    /**
     * Creates an IntentService. Default constructor, invoked by the superclass constructor.
     * Make sure this constructor exists.
     */
    public Navigate() {
        super(TAG);
        Log.i(TAG, "Navigate Service initialized");

        initLocationAPI();
    }

    /**
     * This method initialises the GoogleApiClient if it not already initialised
     */
    private void initLocationAPI(){
        //check if the googleApiClient is already initialized
        if(googleApiClient == null){
            Log.i(TAG, "GoogleApiClient is null. Trying to initialise it");
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        else {
            Log.i(TAG, "GoogleApiClient already initialised. Not initialising it again");
        }
    }

    /**
     * This method connects the GoogleApiClient.
     *
     * @return  True if GoogleApiClient is connected or connecting
     */
    private boolean connectToLocationAPI(){
        if(googleApiClient != null){
            if(!googleApiClient.isConnected() || !googleApiClient.isConnecting()){
                Log.d(TAG, "GoogleApiClient initialised. Trying to connect");
                googleApiClient.connect();
            }
            else {
                Log.i(TAG, "GoogleApiClient is either connected or trying to connect. Not calling the connect() method again");
            }
            return true;
        }
        else {
            Log.e(TAG, "Unable to connect to the googleApiClient. Cannot get user's location");
        }
        return false;
    }

    /**
     * This method is called whenever this service is accessed using an intent
     *
     * @param intent    Intent used to call the service
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "********* Starting Navigate Service *********");
        if(connectToLocationAPI()){
            //get data from the intent
            Bundle bundle = intent.getExtras();
            commute = bundle.getParcelable(Commute.PARCELABLE_KEY);

            //log the data gotten from the intent
            List<Commute.Step> steps = commute.getSteps();
            for(int stepCount = 0; stepCount < steps.size(); stepCount++){
                Commute.Step currStep = steps.get(stepCount);
                if(currStep.getStepType() == Commute.Step.TYPE_MATATU){
                    if(currStep.getRoute() != null) Log.d(TAG, "Current step is route: "+currStep.getRoute().getLongName());
                    else Log.e(TAG, "Route object in current matatu step is null");
                }
                else {
                    Log.d(TAG, "Current step is for walking");
                }
                if(currStep.getStart() != null) Log.d(TAG, "Start stop: " + currStep.getStart().getName());
                else Log.w(TAG, "Current step has a null start");
                if(currStep.getDestination() != null) Log.d(TAG, "End stop: "+currStep.getDestination().getName());
                else Log.w(TAG, "Current step has a null destination");
            }
        }
        Log.i(TAG, "********* Navigate Service finished *********");
    }

    /**
     * This method checks whether an instance of the Navigate service is running
     *
     * @return Returns true if an instance is running
     */
    public static boolean isServiceRunning(Context context){
        final ActivityManager activityManager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals("Navigate")){
                Log.i(TAG, "An instance of the Navigate service is already running");
                return true;
            }
        }
        return false;
    }

    /**
     * This method is called whenever the GoogleApiClient object is connected.
     * The GoogleApiClient object is what is being used to obtain the user's location.
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "GoogleApiClient successfully connected");
    }

    /**
     * This method is called whenever the connection in the GoogleApiClient object is suspended.
     * The GoogleApiClient object is what is being used to obtain the user's location.
     *
     * @param i
     */
    @Override
    public void onConnectionSuspended(int cause) {
        if(cause == CAUSE_NETWORK_LOST){
            Log.i(TAG, "GoogleApiClient suspended due to network loss");
        }
        else if(cause == CAUSE_SERVICE_DISCONNECTED){
            Log.i(TAG, "GoogleApiClient suspended due to service disconnection");
        }
    }

    /**
     * This method is called whenever the user's location is updated on the GoogleApiClient object.
     * The GoogleApiClient object is what is being used to obtain the user's location.
     * Navigation in this service is updated whenever this method is called.
     *
     * @param location  The current user's location
     */
    @Override
    public void onLocationChanged(Location location) {
    }

    /**
     * This
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection failed to GoogleApiClient");
    }
}
