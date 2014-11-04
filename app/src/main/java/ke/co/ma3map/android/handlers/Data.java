package ke.co.ma3map.android.handlers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ke.co.ma3map.android.R;
import ke.co.ma3map.android.carriers.Route;
import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;
import ke.co.ma3map.android.helpers.JSONArray;
import ke.co.ma3map.android.listeners.ProgressListener;

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

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String URI_API_GET_ROUTES = "/get/routes";
    private static final String URI_API_SEARCH = "/search";

    private List<ProgressListener> progressListeners;
    private final Context context;

    public Data(Context context) {
        this.context = context;
        progressListeners = new ArrayList<ProgressListener>();
    }

    public void addProgressListener(ProgressListener progressListener){
        progressListeners.add(progressListener);
    }

    private void updateProgressListeners(int progress, int end, String message, int flag){
        for(int i = 0; i < progressListeners.size(); i++){
            progressListeners.get(i).onProgress(progress, end, message, flag);
        }
    }

    private void finalizeProgressListeners(Bundle output, String message, int flag){
        for(int i = 0; i < progressListeners.size(); i++){
            progressListeners.get(i).onDone(output, message, flag);
        }
    }

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
     * @param uri                   From where in the server you want to get the data from use the URI_* constants in this Class
     * @param data                  The data as a json object
     * @param willProgressContinue  If set to true, this method will send a signal to onProgress on all the registered ProgressListeners instead of onDone
     * @param bundleKey             The key to be used to bundle the data in the progress listeners
     *
     * @return Returns a jsonObject with that looks like this {data, error, message}.
     * Error stores a boolean (True if an error occured). Message being present doesn't mean an error occurred
     */
    public JSONArray getDataFromServer(String uri, JSONObject data, boolean willProgressContinue, String bundleKey) {
        JSONArray serverData = new JSONArray();

        int finalFlag = ProgressListener.FLAG_ERROR;//assuming something bad will happen until final exit code changes this to something else

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
                    updateProgressListeners(0, 0, "Error occurred while trying to send data", ProgressListener.FLAG_ERROR);
                }
            }

            String dataString = URLEncodedUtils.format(nameValuePairs, "utf-8");

            Log.d(TAG, "Sending data to this url " + SERVER_URL + uri + dataString);

            updateProgressListeners(0, 0, "Getting data from the server", ProgressListener.FLAG_WORKING);//progress and end set to 0 because we currently dont have a way of measuring network transfers


            HttpGet httpGet = new HttpGet(SERVER_URL + uri + dataString);
            try {
                HttpResponse httpResponse = httpClient.execute(httpGet);
                updateProgressListeners(0, 0, "Getting data from the server", ProgressListener.FLAG_WORKING);

                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    Header[] headers = httpResponse.getAllHeaders();
                    for(int i =0; i < headers.length; i++){
                        Log.i(TAG, headers[i].getName() + " " + headers[i].getValue());
                    }

                    HttpEntity httpEntity = httpResponse.getEntity();
                    if (httpEntity != null) {
                        updateProgressListeners(0, 0, "Decoding the data", ProgressListener.FLAG_WORKING);
                        InputStream inputStream = httpEntity.getContent();
                        String responseString = convertStreamToString(inputStream);

                        serverData = new JSONArray(responseString.trim());
                        updateProgressListeners(0, 0, "Decoding the data", ProgressListener.FLAG_WORKING);
                        finalFlag = ProgressListener.FLAG_DONE;
                    }
                    else{
                        updateProgressListeners(0, 0, "The server gave us nothing", ProgressListener.FLAG_DONE);
                    }
                } else {
                    Log.e(TAG, "Status Code " + String.valueOf(httpResponse.getStatusLine().getStatusCode()) + " passed");
                    updateProgressListeners(0, 0, "The server farted. Smells like "+String.valueOf(httpResponse.getStatusLine().getStatusCode()), ProgressListener.FLAG_ERROR);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                updateProgressListeners(0, 0, "Could not decode the data received from the server", ProgressListener.FLAG_ERROR);
            } catch (IOException e){
                e.printStackTrace();
                updateProgressListeners(0, 0, "Could not connect to the server", ProgressListener.FLAG_ERROR);
            }
        }

        if(willProgressContinue == false){
            Bundle bundle = new Bundle();
            bundle.putSerializable(bundleKey, serverData);
            finalizeProgressListeners(bundle, "Done getting data from server", finalFlag);
        }

        return serverData;
    }

    /**
     * This method caches the map data in the SQLite database
     *
     * @param data                  Response gotten from the server
     * @param willProgressContinue  If set to true, this method will send a signal to onProgress on all the registered ProgressListeners instead of onDone
     * @param bundleKey             The key to be used to bundle the data in the progress listeners
     * @return
     */
    private ArrayList<Route> cacheMapData(JSONArray data, boolean willProgressContinue, String bundleKey){
        int finalFlag = ProgressListener.FLAG_ERROR;

        ArrayList<Route> routes = new ArrayList<Route>();
        try {
            Database database = new Database(context);
            SQLiteDatabase writableDB = database.getWritableDatabase();

            updateProgressListeners(0, 0, "Clearing existing cache", ProgressListener.FLAG_WORKING);

            database.runTruncateQuery(writableDB, Database.TABLE_POINT);
            database.runTruncateQuery(writableDB, Database.TABLE_STOP);
            database.runTruncateQuery(writableDB, Database.TABLE_LINE);
            database.runTruncateQuery(writableDB, Database.TABLE_ROUTE);

            updateProgressListeners(0, 0, "Clearing existing cache", ProgressListener.FLAG_WORKING);

            updateProgressListeners(0, 100, "Caching the new route data", ProgressListener.FLAG_WORKING);
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
                updateProgressListeners(Math.round(progress), 100, "Caching the new route data", ProgressListener.FLAG_WORKING);
            }
            finalFlag = ProgressListener.FLAG_DONE;
        }
        catch (JSONException e){
            e.printStackTrace();
            updateProgressListeners(0, 0, "Could not decode the data received", ProgressListener.FLAG_ERROR);
        }

        if(willProgressContinue == false){
            Bundle output = new Bundle();
            output.putParcelableArrayList(bundleKey, routes);

            finalizeProgressListeners(output, "Done caching the data", finalFlag);
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
     * @param willProgressContinue If set to true, this method will send a signal to onProgress on all the registered ProgressListeners instead of onDone
     * @param bundleKey The key to be used to bundle the data in the progress listeners
     *
     * @return
     */
    public ArrayList<Route> getAllRouteData(boolean willProgressContinue, String bundleKey){

        JSONArray serverResponse = getDataFromServer(URI_API_GET_ROUTES, new JSONObject(), true, bundleKey);
        ArrayList<Route> routes = cacheMapData(serverResponse, true, bundleKey);

        if(willProgressContinue == false){
            Bundle output = new Bundle();
            output.putParcelableArrayList(Route.PARCELABLE_KEY, routes);
            finalizeProgressListeners(output, "Done getting the data", ProgressListener.FLAG_DONE);
        }
        else {
            updateProgressListeners(0, 0, "Done getting the data", ProgressListener.FLAG_DONE);
        }

        return routes;
    }

    /**
     * This method get's suggestions of places from Google's Places API using an initial string
     * TODO:make this method not static and implement using ProgressListeners
     *
     * @param context
     * @param input
     * @return
     */
    public static ArrayList<String[]> getPlaceSuggestions(Context context, String input) {
        ArrayList<String[]> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + "/autocomplete/json");
            sb.append("?input=" + URLEncoder.encode(input, "utf8"));
            sb.append("&components=country:ke");
            sb.append("&location=-1.2927254,36.8204436");
            sb.append("&radius=50000");//50KM
            //sb.append("&types=sublocality");
            sb.append("&key=" + context.getResources().getString(R.string.places_api_key));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            Log.d(TAG, "suggestions = "+jsonResults.toString());
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<String[]>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(new String[]{predsJsonArray.getJSONObject(i).getString("place_id"), predsJsonArray.getJSONObject(i).getJSONArray("terms").getJSONObject(0).getString("value")});
            }
        } catch (JSONException e) {
            Log.e(TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }

    /**
     * This method gets a LatLng corresponding to a placeID using Google's Places API
     * TODO:make this method not static and implement ProgressListeners
     * @param context
     * @param placeID
     * @return
     */
    public static LatLng getPlaceLatLng(Context context, String placeID) {
        ArrayList<String[]> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + "/details/json");
            sb.append("?placeid=" + URLEncoder.encode(placeID, "utf8"));
            sb.append("&key=" + context.getResources().getString(R.string.places_api_key));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error processing Places API URL", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Places API", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            Log.d(TAG, "place details = "+jsonResults.toString());
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            org.json.JSONObject location = jsonObj.getJSONObject("result").getJSONObject("geometry").getJSONObject("location");

            return new LatLng(Double.parseDouble(location.getString("lat")), Double.parseDouble(location.getString("lng")));
        } catch (JSONException e) {
            Log.e(TAG, "Cannot process JSON results", e);
        }

        return null;
    }
}