package ke.co.ma3map.android.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ke.co.ma3map.android.carriers.Commute;
import ke.co.ma3map.android.carriers.Route;

/**
 * This class implements the IntentService that handles navigation in ma3map.
 * Created by jrogena on 28/01/2015.
 */
public class Navigate extends IntentService {

    private static final String TAG = "ma3map.Navigate";
    private Commute commute;

    /**
     * Creates an IntentService. Default constructor, invoked by the superclass constructor.
     * Make sure this constructor exists.
     */
    public Navigate() {
        super(TAG);
        Log.i(TAG, "Navigate Service initialized");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "********* Starting Navigate Service *********");
        Bundle bundle = intent.getExtras();
        commute = bundle.getParcelable(Commute.PARCELABLE_KEY);
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
}
