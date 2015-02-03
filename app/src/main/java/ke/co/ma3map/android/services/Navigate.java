package ke.co.ma3map.android.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ke.co.ma3map.android.carriers.Commute;
import ke.co.ma3map.android.carriers.Stop;
import ke.co.ma3map.android.handlers.Data;

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

    public static final String ACTION_GET_COMMUTE_SEGMENTS = "ke.co.ma3map.android.action.getCommuteSegments";
    public static final String ACTION_GET_NAVIGATION_STATUS = "ke.co.ma3map.android.action.getNavigationStatus";

    public static final String STATUS_PARCELABLE_KEY = "status";
    public static final int STATUS_ERROR = 0;
    public static final int STATUS_STARTING = 1;
    public static final int STATUS_UPDATED = 2;
    public static final int STATUS_FINISHED = 3;

    private static final String TAG = "ma3map.Navigate";
    private Commute commute;
    private GoogleApiClient googleApiClient;

    /*
    This variable stores the index of the current commute step.
    Default value is -1 (this should infer to walking to the first stop)
    When currCommuteStep = commute.steps.size(), infer to walking from destination step to actual destination
     */
    private int currCommuteStep;

    /*
     This variable shows whether we are clear to start the navigation.
     Default value for this variable is false.
     Events that can change this variable include:
        - whether the commute broadcasts has been sent successfully. Refer to broadcastCommuteSegments()
     */
    private boolean allGood;

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
     * @param intent Intent used to call the service
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "********* Starting Navigate Service *********");
        if(connectToLocationAPI()){
            //get data from the intent
            Bundle bundle = intent.getExtras();
            commute = bundle.getParcelable(Commute.PARCELABLE_KEY);

            //reset currCommuteStep to -1
            currCommuteStep = -1;

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



            if(commute.getSteps().size() > 0){

                //do all other preparations before starting navigation
                if(broadcastCommuteSegments()){
                    allGood = true;
                    broadcastNavigationStatus(STATUS_STARTING);
                }
                else {
                    broadcastNavigationStatus(STATUS_ERROR);
                }
            }
            else {
                broadcastNavigationStatus(STATUS_ERROR);
            }

        }
        Log.i(TAG, "********* Navigate Service finished *********");
    }

    private void broadcastNavigationStatus(int status){
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_GET_NAVIGATION_STATUS);
        broadcastIntent.putExtra(STATUS_PARCELABLE_KEY, status);
        localBroadcastManager.sendBroadcast(broadcastIntent);
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     * The service should clean up any resources it holds (threads, registered receivers, etc) at this point.
     * Upon return, there will be no more calls in to this Service object and it is effectively dead.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Navigate service about to be destroyed. Releasing all resources bound to the service");
        if(googleApiClient != null){
            googleApiClient.disconnect();
        }
        else {
            Log.i(TAG, "GoogleApiClient is already null. No way to release this service");
        }

        broadcastNavigationStatus(STATUS_FINISHED);
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
        Log.d(TAG, "*%*%*%*%*% User's location updated");
        if(allGood){

            //broadcast navigation update
            broadcastNavigationStatus(STATUS_UPDATED);
        }
        else {
            Log.d(TAG, "Service still not ready to start navigation");
        }
    }

    /**
     * This method broadcasts a list of Commute.Segments corresponding to the current commute to all
     * broadcast receivers registered to ACTION_GET_COMMUTE_SEGMENTS
     *
     * @return  True if data successfully broadcasted and false otherwise
     */
    private boolean broadcastCommuteSegments(){
        Data dataHandler = new Data(Navigate.this);

        ArrayList<Commute.Segment> segments = new ArrayList<Commute.Segment>();

        //get walking directions (and in extension the commute segment) from origin to first stop
        JSONObject originSegmentJsonObject = dataHandler.getDirections(Data.DIRECTIONS_WALKING, commute.getFrom(), commute.getStep(0).getStart().getLatLng());
        Commute.Segment startSegment = new Commute.Segment(originSegmentJsonObject, Commute.Segment.TYPE_WALKING);
        if(startSegment.getPolyline().size() == 0){
            Log.e(TAG, "Commute segment representing walking from start point has an empty polyline");
        }
        segments.add(startSegment);

        //get polylines corresponding to all the steps
        for(int stepIndex = 0; stepIndex < commute.getSteps().size(); stepIndex++){
            Commute.Step currStep = commute.getStep(stepIndex);
            if(currStep.getStepType() == Commute.Step.TYPE_WALKING){
                JSONObject stepSegmentJsonObject = dataHandler.getDirections(Data.DIRECTIONS_WALKING, currStep.getStart().getLatLng(), currStep.getDestination().getLatLng());
                Commute.Segment currSegment = new Commute.Segment(stepSegmentJsonObject, Commute.Segment.TYPE_WALKING);
                if(currSegment.getPolyline().size() == 0){
                    Log.e(TAG, "Commute segment representing walking in current commute step has an empty polyline");
                    return false;
                }
                segments.add(currSegment);
            }
            else if(currStep.getStepType() == Commute.Step.TYPE_MATATU){
                currStep.getRoute().loadPoints(Navigate.this);
                Commute.Segment currSegment = new Commute.Segment(currStep.getRoute().getPolyline(), Commute.Segment.TYPE_MATATU);
                if(currSegment.getPolyline().size() == 0){
                    Log.e(TAG, "Commute segment representing walking in current commute step has an empty polyline");
                    return false;
                }
                segments.add(currSegment);
            }
        }
        //get walking directions (and in extension the commute segment) from last stop to the destination
        JSONObject destinationSegmentJsonObject = dataHandler.getDirections(Data.DIRECTIONS_WALKING, commute.getStep(commute.getSteps().size() - 1).getDestination().getLatLng(), commute.getTo());
        Commute.Segment destinationSegment = new Commute.Segment(destinationSegmentJsonObject, Commute.Segment.TYPE_WALKING);
        if(destinationSegment.getPolyline().size() == 0){
            Log.e(TAG, "Commute segment representing walking from last stop to destination has an empty polyline");
            return false;
        }
        segments.add(destinationSegment);

        //broadcast the segments
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent();
        intent.setAction(ACTION_GET_COMMUTE_SEGMENTS);

        intent.putParcelableArrayListExtra(Commute.Segment.PARCELABLE_KEY, segments);
        localBroadcastManager.sendBroadcast(intent);
        return true;
    }

    private void updateWalkingStatus(LatLng currLocation, Stop destination){

    }

    private void updateWalkingStatus(LatLng currLocation, LatLng destination){

    }

    private void updateInMatatuStatus(){

    }

    /**
     * This method is called whenever the connection to the GoogleApiClient fails
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection failed to GoogleApiClient");
        broadcastNavigationStatus(STATUS_ERROR);
    }
}
