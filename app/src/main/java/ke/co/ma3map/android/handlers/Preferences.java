package ke.co.ma3map.android.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import ke.co.ma3map.android.R;

/**
 * Created by jason on 27/09/14.
 */
public class Preferences {

    private static final String TAG = "ma3map.Preferences";

    public static final String SP_GET_DATA_FLAG = "getDataFlag";
    public static final String SP_GET_DATA_MESSAGE = "getDataMessage";
    public static final String SP_GET_DATA_PROGRESS = "getDataProgress";
    public static final String SP_GET_DATA_DONE = "getDataDone";


    /**
     * This method sets a shared preference to the specified value. Note that shared preferences can only handle strings
     *
     * @param context The context from where you want to set the value
     * @param sharedPreferenceKey The key corresponding to the shared preference. All shared preferences accessible by this app are defined in
     *                            DataHandler e.g DataHandler.SP_KEY_LOCALE
     * @param value The value the sharedPreference is to be set to
     */
    public static void setSharedPreference(Context context, String sharedPreferenceKey, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(context.getString(R.string.app_name),Context.MODE_PRIVATE).edit();
        editor.putString(sharedPreferenceKey,value);
        editor.commit();
        Log.d(TAG, sharedPreferenceKey + " shared preference saved as " + value);
    }

    /**
     * Gets the vaule of a shared preference accessible by the context
     *
     * @param context Context e.g activity that is requesting for the shared preference
     * @param sharedPreferenceKey The key corresponding to the shared preference. All shared preferences accessible by this app are defined in
     *                            DataHandler e.g DataHandler.SP_KEY_LOCALE
     * @param defaultValue What will be returned by this method if the sharedPreference is empty or unavailable
     *
     * @return The value of the sharedPreference or the default value specified if the sharedPreference is empty
     */
    public static String getSharedPreference(Context context, String sharedPreferenceKey, String defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        //Log.d(TAG, "value of " + sharedPreferenceKey + " is " + sharedPreferences.getString(sharedPreferenceKey, defaultValue));
        return sharedPreferences.getString(sharedPreferenceKey, defaultValue);
    }
}
