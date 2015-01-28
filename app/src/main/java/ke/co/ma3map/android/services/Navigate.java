package ke.co.ma3map.android.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import ke.co.ma3map.android.carriers.Commute;

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
        commute = (Commute) intent.getParcelableExtra(Commute.PARCELABLE_KEY);

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
