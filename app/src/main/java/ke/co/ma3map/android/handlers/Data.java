package ke.co.ma3map.android.handlers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ke.co.ma3map.android.carriers.Route;
import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;
import ke.co.ma3map.android.helpers.JSONArray;

/**
 * Created by jason on 21/09/14.
 * This class handles movement and storage of data in the app.
 * Most of the public methods in this class should be run from inside
 * an AsyncTask as they are might block the UI thread if not
 */
public class Data {
    private static final String TAG = "Data";

    private static final String SERVER_URL = "http://ma3map.herokuapp.com";
    private static final int HTTP_POST_TIMEOUT = 20000;
    private static final int HTTP_RESPONSE_TIMEOUT = 200000;

    public static final int FLAG_WORKING = 0;
    public static final int FLAG_DONE = 1;
    public static final int FLAG_ERROR = -1;

    private static final String URI_API_GET_ROUTES = "/get/routes";
    private static final String URI_API_SEARCH = "/search";

    /**
     * This method checks whether the application can access the internet
     *
     * @param context The activity/service from where you want to check for the connection
     * @return True if the application can connect to the internet and False if not
     */
    public static boolean checkNetworkConnection(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) { //no connection
            return false;
        } else {
            return true;
        }
    }

    /**
     * This method gets data from the server using a get request.
     * Make sure you call this method from a thread running asynchronously from the UI thread
     *
     * @param context           The activity/service from where you want to check for the connection
     * @param uri               From where in the server you want to get the data from use the URI_* constants in this Class
     * @param data              The data as a json object
     * @param progressListener  The progressListener to be used to show progress
     *
     * @return Returns a jsonObject with that looks like this {data, error, message}.
     * Error stores a boolean (True if an error occured). Message being present doesn't mean an error occurred
     */
    public static JSONArray getDataFromServer(Context context, String uri, JSONObject data, ProgressListener progressListener) {
        JSONArray serverData = new JSONArray();

        if (checkNetworkConnection(context)) {
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, HTTP_POST_TIMEOUT);
            //HttpConnectionParams.setSoTimeout(httpParameters, HTTP_RESPONSE_TIMEOUT);
            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);

            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    nameValuePairs.add(new BasicNameValuePair(key, data.getString(key)));
                } catch (Exception e) {
                    e.printStackTrace();
                    progressListener.onProgress(0, 0, "Error occurred while trying to send data", FLAG_ERROR);
                }
            }

            String dataString = URLEncodedUtils.format(nameValuePairs, "utf-8");

            Log.d(TAG, "Sending data to this url " + SERVER_URL + uri + dataString);

            progressListener.onProgress(0, 0, "Getting route data from the server", FLAG_WORKING);//progress and end set to 0 because we currently dont have a way of measuring network transfers


            HttpGet httpGet = new HttpGet(SERVER_URL + uri + dataString);
            try {
                HttpResponse httpResponse = httpClient.execute(httpGet);
                progressListener.onProgress(0, 0, "Getting route data from the server", FLAG_WORKING);

                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    Header[] headers = httpResponse.getAllHeaders();
                    for(int i =0; i < headers.length; i++){
                        Log.i(TAG, headers[i].getName() + " " + headers[i].getValue());
                    }

                    HttpEntity httpEntity = httpResponse.getEntity();
                    if (httpEntity != null) {
                        progressListener.onProgress(0, 0, "Decoding the route data", FLAG_WORKING);
                        InputStream inputStream = httpEntity.getContent();
                        String responseString = convertStreamToString(inputStream);

                        serverData = new JSONArray(responseString.trim());
                        progressListener.onProgress(0, 0, "Decoding the route data", FLAG_WORKING);
                    }
                    else{
                        progressListener.onProgress(0, 0, "The server gave us nothing", FLAG_DONE);
                    }
                } else {
                    Log.e(TAG, "Status Code " + String.valueOf(httpResponse.getStatusLine().getStatusCode()) + " passed");
                    progressListener.onProgress(0, 0, "The server farted. Smells like "+String.valueOf(httpResponse.getStatusLine().getStatusCode()), FLAG_ERROR);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                progressListener.onProgress(0, 0, "Could not decode the data received from the server", FLAG_ERROR);
            } catch (IOException e){
                e.printStackTrace();
                progressListener.onProgress(0, 0, "Could not connect to the server", FLAG_ERROR);
            }
        }
        return serverData;
    }

    /**
     * This method caches the map data in the SQLite database
     *
     * @param context           The activity/service from where you want to check for the connection
     * @param data              Response gotten from the server
     * @param progressListener  The progressListener to be used to show progress
     * @return
     */
    private static List<Route> cacheMapData(Context context, JSONArray data, ProgressListener progressListener){
        List<Route> routes = new ArrayList<Route>();
        try {
            Database database = new Database(context);
            SQLiteDatabase writableDB = database.getWritableDatabase();

            progressListener.onProgress(0, 0, "Clearing existing cache", FLAG_WORKING);

            database.runTruncateQuery(writableDB, Database.TABLE_POINT);
            database.runTruncateQuery(writableDB, Database.TABLE_STOP);
            database.runTruncateQuery(writableDB, Database.TABLE_LINE);
            database.runTruncateQuery(writableDB, Database.TABLE_ROUTE);

            progressListener.onProgress(0, 0, "Clearing existing cache", FLAG_WORKING);

            progressListener.onProgress(0, 100, "Caching the new route data", FLAG_WORKING);
            for(int routeIndex = 0; routeIndex < data.length(); routeIndex++){
                Route currRoute = new Route(data.getJSONObject(routeIndex));
                currRoute.insertIntoDB(database, writableDB);
                routes.add(currRoute);
                float incIndex = (float)(routeIndex+1);
                float dLength = (float)data.length();
                float progress = ((incIndex/dLength) * 100f);
                Log.d(TAG, "route index = "+String.valueOf(routeIndex+1));
                Log.d(TAG, "length = "+String.valueOf(data.length()));
                Log.d(TAG, "progress = "+String.valueOf(progress));
                progressListener.onProgress(Math.round(progress), 100, "Caching the new route data", FLAG_WORKING);
            }
        }
        catch (JSONException e){
            e.printStackTrace();
            progressListener.onProgress(0, 0, "Could not decode the data received", FLAG_ERROR);
        }

        return routes;
    }

    /**
     * This method pings the server and checks if it responds before the timeout
     *
     * @param timeout
     * @return
     */
    private static boolean isConnectedToServer(int timeout) {
        try {
            URL myUrl = new URL(SERVER_URL);
            URLConnection connection = myUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.connect();
            return true;
        } catch (Exception e) {
            // Handle your exceptions
            return false;
        }
    }

    /**
     * This method coverts an inputStream into a string
     *
     * @param inputStream
     * @return
     */
    private static String convertStreamToString(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();

            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }

    /**
     * This method downloads all route data from the server and caches them in the SQLite database
     * Several methods called by this method will block the thread for a long time. This method should
     * thus be called in a thread running asynchronously from the UI Thread
     *
     * @param context The activity/service from where you want to check for the connection
     *
     * @return
     */
    public static List<Route> getAllRouteData(Context context, ProgressListener progressListener){

        JSONArray serverResponse = getDataFromServer(context, URI_API_GET_ROUTES, new JSONObject(), progressListener);
        List<Route> routes = cacheMapData(context, serverResponse, progressListener);
        progressListener.onProgress(0, 0, "Done getting the data", FLAG_DONE);
        return routes;
    }

    public interface ProgressListener {

        /**
         * This method is called when progress is made in whatever is being done
         *
         * @param progress  Number showing progress. Where one is complete
         * @param end       Maximum number progress can get to
         * @param message    String explaining what is currently happening
         * @param flag      Flag showing the status of the action e.g working, done, error
         */
        public void onProgress(int progress, int end, String message, int flag);
    }
}