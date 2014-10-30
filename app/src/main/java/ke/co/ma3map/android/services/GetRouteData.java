package ke.co.ma3map.android.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ke.co.ma3map.android.R;
import ke.co.ma3map.android.carriers.Route;
import ke.co.ma3map.android.handlers.Data;
import ke.co.ma3map.android.listeners.Progress;

/**
 * Created by jason on 26/09/14.
 */
public class GetRouteData extends IntentService
                            implements Progress.ProgressListener{

    private static final String TAG = "GetRouteData";
    private final int NOTIFICATION_ID = 1;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private Progress progress;

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

        progress = (Progress)intent.getSerializableExtra(Progress.PARCELABLE_KEY);

        Data data = new Data(this);
        data.addProgressListener(this);
        data.addProgressListener(progress.getProgressListener());
        data.getAllRouteData(false, Route.PARCELABLE_KEY);

        Log.i(TAG, "********* Service finished *********");
    }

    @Override
    public void onProgress(int progress, int done, String message, int flag) {
        Log.i(TAG, message + " " + String.valueOf(progress));
        builder.setContentText(message)
                .setProgress(done, progress, false);

        //TODO: change icon based on flag (working, done and error)

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDone(Bundle output, String message, int flag) {
        Log.i(TAG, message + " " + String.valueOf(progress));
        builder.setContentText(message)
                .setProgress(0, 0, false);
    }
}
