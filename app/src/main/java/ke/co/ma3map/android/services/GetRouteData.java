package ke.co.ma3map.android.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;

import ke.co.ma3map.android.R;
import ke.co.ma3map.android.activities.Map;
import ke.co.ma3map.android.carriers.Route;
import ke.co.ma3map.android.handlers.Data;
import ke.co.ma3map.android.listeners.ProgressListener;

/**
 * Created by jason on 26/09/14.
 */
public class GetRouteData extends IntentService
                            implements ProgressListener{

    private static final String TAG = "ma3map.GetRouteData";
    private final int NOTIFICATION_ID = 1;
    public static final String ACTION_GET_ROUTE_DATA = "ke.co.ma3map.android.action.getRouteData";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private ArrayList<Route> allRouteData;

    /**
     * Default constructor. Make sure this is there or Android will not be able
     * to call the service
     */
    public GetRouteData(){
        super(TAG);
        allRouteData = null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "********* Starting Service *********");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(this)
                .setContentTitle("Matatu route data")
                .setSmallIcon(R.drawable.ic_launcher);

        Data data = new Data(this);
        data.addProgressListener(GetRouteData.this);
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
        Log.i(TAG, message);
        builder.setContentText(message)
                .setProgress(0, 0, false);

        allRouteData = output.getParcelableArrayList(Route.PARCELABLE_KEY);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent();
        intent.setAction(ACTION_GET_ROUTE_DATA);

        //reduce this sizes of the route objects by unloading stops
        for(int i =0; i < allRouteData.size(); i++){
            allRouteData.get(i).unloadPoints();
        }

        intent.putParcelableArrayListExtra(Route.PARCELABLE_KEY, allRouteData);
        localBroadcastManager.sendBroadcast(intent);
    }
}
