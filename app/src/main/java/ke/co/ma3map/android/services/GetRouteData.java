package ke.co.ma3map.android.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ke.co.ma3map.android.R;
import ke.co.ma3map.android.handlers.Data;
import ke.co.ma3map.android.handlers.Preferences;

/**
 * Created by jason on 26/09/14.
 */
public class GetRouteData extends IntentService
                            implements Data.ProgressListener{

    private static final String TAG = "GetRouteData";
    private final int NOTIFICATION_ID = 1;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;

    /**
     * Default constructor. Make sure this is there or Android will not be able
     * to call the service
     */
    public GetRouteData(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "********* Starting Service *********");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(this)
                .setContentTitle("Matatu route data")
                .setSmallIcon(R.drawable.ic_launcher);

        Data.getAllRouteData(this, this);
        Log.i(TAG, "********* Service finished *********");
    }

    @Override
    public void onProgress(int progress, int done, String message, int flag) {
        Log.i(TAG, message + " " + String.valueOf(progress));
        builder.setContentText(message)
                .setProgress(done, progress, false);

        //TODO: change icon based on flag (working, done and error)

        Preferences.setSharedPreference(this, Preferences.SP_GET_DATA_DONE, String.valueOf(done));
        Preferences.setSharedPreference(this, Preferences.SP_GET_DATA_PROGRESS, String.valueOf(progress));
        Preferences.setSharedPreference(this, Preferences.SP_GET_DATA_MESSAGE, message);
        Preferences.setSharedPreference(this, Preferences.SP_GET_DATA_FLAG, String.valueOf(flag));

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
