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
import ke.co.ma3map.android.carriers.Commute;
import ke.co.ma3map.android.carriers.Point;
import ke.co.ma3map.android.carriers.Route;
import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;
import ke.co.ma3map.android.helpers.JSONArray;
import ke.co.ma3map.android.listeners.ProgressListener;

/**
 * @author Jason Rogena <jasonrogena@gmail.com>
 * @since 2014-09-21
 *
 * This class handles movement and storage of data in the app.
 * <p>
 * Most of the public methods in this class should be called in a thread running asynchronously from
 * the UI thread.
 * <p>
 * It's best practice not to share an instance of this class between multiple {@link android.content.Context}
 */
public class Data {
    private static final String TAG = "ma3map.Data";

    private static final String SERVER_URL = "http://api.ma3map.org";
    private static final int HTTP_POST_TIMEOUT = 20000;
    private static final int HTTP_RESPONSE_TIMEOUT = 200000;

    private static final String API_GOOGLE_PLACES_URL = "https://maps.googleapis.com/maps/api/place";
    private static final String API_GOOGLE_DISTANCE_MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix";
    private static final String API_GOOGLE_DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions";
    private static final String API_MA3MAP_URI_GET_ROUTES = "/get/routes";
    public static final String API_MA3MAP_URI_GET_PATHS = "/get/paths";

    public static final String DIRECTIONS_WALKING = "walking";
    public static final String DIRECTIONS_DRIVING = "driving";

    private List<ProgressListener> progressListeners;
    private final Context context;

    /**
     * Class constructor.
     * <p>
     * @param context   <code>Context</code> from where this class has been initialised
     */
    public Data(Context context) {
        this.context = context;
        progressListeners = new ArrayList<ProgressListener>();
    }

    /**
     * Adds a <code>ProgressListener</code> to the list of progress listeners that will be updated
     * when an action performed by this class is updated.
     * <p>
     * Note that not all actions performed by this class update registered progress listeners
     * <p>
     * @param progressListener  The new <code>ProgressListener</code> to be added to the list of updated
     *                          progress listeners
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     */
    public void addProgressListener(ProgressListener progressListener){
        progressListeners.add(progressListener);
    }

    /**
     * Updates all registered <code>ProgressListeners</code>
     * <p>
     * @param progress  Number not greater than <code>end</code> that indicates progress
     *                  <code>progress/end</code> should indicate progress as a fraction
     * @param end       Indicates the potential maximum value of <code>progress</code>
     * @param message   String indicating the progress. Can be displayed to the user
     * @param flag      Indicates the status of the task in question. Possible values are:
     *                      - {@link ProgressListener#FLAG_DONE}
     *                      - {@link ProgressListener#FLAG_WORKING}
     *                      - {@link ProgressListener#FLAG_ERROR}
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     */
    protected void updateProgressListeners(int progress, int end, String message, int flag){
        for(int i = 0; i < progressListeners.size(); i++){
            progressListeners.get(i).onProgress(progress, end, message, flag);
        }
    }

    /**
     * Calls {@link ProgressListener#onDone(android.os.Bundle, String, int)} on all the registered
     * progress listeners.
     * <p>
     * @param output    <code>Bundle</code> containing all packaged data
     * @param message   String indicating finalisation of action. Can be displayed to the user
     * @param flag      Indicates the status of the task in question. Possible values are:
     *                      - {@link ProgressListener#FLAG_DONE}
     *                      - {@link ProgressListener#FLAG_WORKING}
     *                      - {@link ProgressListener#FLAG_ERROR}
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     */
    protected void finalizeProgressListeners(Bundle output, String message, int flag){
        for(int i = 0; i < progressListeners.size(); i++){
            progressListeners.get(i).onDone(output, message, flag);
        }
    }

    /**
     * Checks whether the application can access the internet.
     * <p>
     * @param context   The activity/service from where you are doing the check
     *
     * @return  <code>true</code> if the application can connect to the internet and <code>false</code> if not
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
     * Gets data from any of the ma3map APIs using a GET request.
     * <p>
     * This method updates all registered <code>ProgressListeners</code>
     * Make sure you call this method from a thread running asynchronously from the UI thread.
     * <p>
     * @param uri                   From where in the server you want to get the data from use the URI_* constants in this Class
     * @param data                  The data as a json object
     * @param willProgressContinue  If set to true, this method will send a signal to onProgress on all the registered ProgressListeners instead of onDone
     * @param bundleKey             The key to be used to bundle the data in the progress listeners
     *
     * @return  JSONArray with the data from the accessed API.
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     * @see ke.co.ma3map.android.helpers.JSONObject
     * @see ke.co.ma3map.android.helpers.JSONArray
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
     * Caches the map data in the SQLite database
     * <p>
     * This method updates all registered <code>ProgressListeners</code>
     * Make sure you call this method from a thread running asynchronously from the UI thread.
     * <p>
     * @param data                  Response gotten from the server
     * @param willProgressContinue  If set to true, this method will send a signal to onProgress on all the registered ProgressListeners instead of onDone
     * @param bundleKey             The key to be used to bundle the data in the progress listeners
     *
     * @return  A list of all caches <code>routes</code>
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     * @see ke.co.ma3map.android.helpers.JSONArray
     * @see ke.co.ma3map.android.carriers.Route
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
     * Gets cached map data from the local SQLite database.
     * <p>
     * This method updates all registered <code>ProgressListeners</code>
     * Make sure you call this method from a thread running asynchronously from the UI thread.
     * <p>
     * @param willProgressContinue  If set to true, this method will send a signal to onProgress on all the registered ProgressListeners instead of onDone
     * @param bundleKey             The key to be used to bundle the data in the progress listeners
     *
     * @return                      An array list of all the route data
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     * @see ke.co.ma3map.android.carriers.Route
     */
    public ArrayList<Route> getCachedRouteData(boolean willProgressContinue, String bundleKey, boolean light){
        Database database = new Database(context);
        SQLiteDatabase readableDB = database.getReadableDatabase();

        String[][] routeRows = database.runSelectQuery(readableDB, Database.TABLE_ROUTE, Route.ALL_COLUMNS, null, null, null, null, null, null);
        ArrayList<Route> routes = new ArrayList<Route>();

        updateProgressListeners(0, 100, "Getting cached route data", ProgressListener.FLAG_WORKING);
        for(int routeIndex = 0; routeIndex < routeRows.length; routeIndex++){
            Route currRoute = new Route(database, readableDB, routeRows[routeIndex], light);
            routes.add(currRoute);
            Log.d(TAG, "processed "+(routeIndex+1) + " of "+routeRows.length);
            updateProgressListeners(Math.round(((routeIndex+1) * 100)/routeRows.length), 100, "Getting cached route data", ProgressListener.FLAG_WORKING);
        }

        if(willProgressContinue == false){
            Bundle output = new Bundle();
            output.putParcelableArrayList(bundleKey, routes);

            finalizeProgressListeners(output, "Done getting cached route data", ProgressListener.FLAG_DONE);
        }
        return routes;
    }

    /**
     * Gets cached points that a linked to a <code>line</code>.
     * <p>
     * This method updates all registered <code>ProgressListeners</code>
     * Make sure you call this method from a thread running asynchronously from the UI thread.
     * <p>
     * @return  An ArrayList of all Points linked to a line ordered by their sequence numbers
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     * @see ke.co.ma3map.android.carriers.Line
     * @see ke.co.ma3map.android.carriers.Point
     */
    public ArrayList<Point> getLinePoints(String pointID){
        ArrayList<Point> points = new ArrayList<Point>();
        Database database = new Database(context);
        SQLiteDatabase readableDB = database.getReadableDatabase();

        String[][] pointRows = database.runSelectQuery(readableDB, Database.TABLE_POINT, Point.ALL_COLUMNS, "line_id=?", new String[]{pointID}, null, null, "point_sequence", null);
        for(int rowIndex = 0; rowIndex < pointRows.length; rowIndex++){
            Point currPoint = new Point(pointRows[rowIndex]);
            points.add(currPoint);
            updateProgressListeners(rowIndex+1, pointRows.length, "Getting cached route points", ProgressListener.FLAG_WORKING);
        }

        Bundle output = new Bundle();
        output.putParcelableArrayList(Point.PARCELABLE_KEY, points);

        finalizeProgressListeners(output, "Done getting cached route points", ProgressListener.FLAG_DONE);

        return points;
    }

    /**
     * Pings the server and checks if it responds before the timeout
     * <p>
     * @param timeout   Time in milliseconds before timing out
     *
     * @return  <code>true</code> if able to connect to the server and <code>false</code> if not
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
     * Coverts an inputStream into a string
     * <p>
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
     * Downloads all route data from the server and caches them in the SQLite database
     * <p>
     * This method updates all registered <code>ProgressListeners</code>
     * Make sure you call this method from a thread running asynchronously from the UI thread.
     * <p>
     *
     * @param willProgressContinue  If set to true, this method will send a signal to onProgress on
     *                              all the registered ProgressListeners instead of onDone
     * @param bundleKey             The key to be used to bundle the data in the progress listeners
     *
     * @return  A list of all <code>routes</code> gotten from the server
     *
     * @see ke.co.ma3map.android.listeners.ProgressListener
     * @see ke.co.ma3map.android.carriers.Route
     */
    public ArrayList<Route> getAllRouteData(boolean willProgressContinue, String bundleKey){

        JSONArray serverResponse = getDataFromServer(API_MA3MAP_URI_GET_ROUTES, new JSONObject(), true, bundleKey);
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
     * Checks whether the route data is currently cached in the local SQLite database.
     * <p>
     * @return  <code>true</code> if route data is cached and <code>false</code> if not
     */
    public boolean isRouteDataPresent(){
        Database database = new Database(context);
        SQLiteDatabase readableDB = database.getReadableDatabase();
        String[][] points = database.runSelectQuery(readableDB, Database.TABLE_POINT, new String[]{"line_id"}, null, null, null, null, null, "1");
        if(points != null && points.length > 0) return true;

        return false;
    }

    /**
     * Gets suggestions of places from Google's Places API.
     * TODO: make this method not static and implement using ProgressListeners
     * <p>
     * Make sure you call this method from a thread running asynchronously from the UI thread
     * <p>
     * @param context   <<code>Context</code> from where you are calling this method</code>
     * @param input     String to be used as an input in the API
     *
     * @return  A list of two element arrays containing the place ID and name of places returned from
     *          the API
     *
     * @see <a href="https://developers.google.com/places/documentation/">Google Places API Documentation</a>
     */
    public static ArrayList<String[]> getPlaceSuggestions(Context context, String input) {
        ArrayList<String[]> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(API_GOOGLE_PLACES_URL + "/autocomplete/json");
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
     * Gets the driving distance in metres between two points using Google's Distance
     * Matrix API.
     * <p>
     * Make sure you call this method from a thread running asynchronously from the UI thread
     * <p>
     * @param pointA    The first point
     * @param pointB    The second point
     *
     * @return  The distance in metres between pointA and pointB
     *
     * @see <a href="https://developers.google.com/maps/documentation/distancematrix/">Google Distance Matrix API Documentation</a>
     */
    public double getDrivingDistance(LatLng pointA, LatLng pointB) {
        ArrayList<String[]> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(API_GOOGLE_DISTANCE_MATRIX_URL + "/json");
            sb.append("?origins="+String.valueOf(pointA.latitude)+","+String.valueOf(pointA.longitude));
            sb.append("&destinations="+String.valueOf(pointB.latitude)+","+String.valueOf(pointB.longitude));
            sb.append("&units=metric");
            sb.append("&mode=driving");
            sb.append("&key=" + context.getResources().getString(R.string.distance_matrix_api_key));

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
            Log.e(TAG, "Error processing Distance Matrix API URL", e);
            return -1;
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Distance Matrix API", e);
            return -1;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            if(jsonObj.getString("status").equals("OK")){
                JSONArray apiRows = jsonObj.getJSONArray("rows");
                if(apiRows.length() > 0){
                    JSONObject bestRow = apiRows.getJSONObject(0);
                    JSONArray elements = bestRow.getJSONArray("elements");
                    if(elements.length() > 0){
                        JSONObject bestElement = elements.getJSONObject(0);
                        Log.d(TAG, "Distance text from Google Distance Matrix API is "+bestElement.getJSONObject("distance").getString("text"));
                        Log.d(TAG, "Distance value from Google Distance Matrix API is "+bestElement.getJSONObject("distance").getString("value"));
                        //note that the distance matrix API returns text in KM but value in metres. No need to convert value from KM to metres
                        return bestElement.getJSONObject("distance").getDouble("value");
                    }
                    else {
                        Log.e(TAG, "No results from Distance Matrix API");
                        return -1;
                    }
                }
                else {
                    Log.e(TAG, "No results from Distance Matrix API");
                    return -1;
                }
            }
            else {
                Log.e(TAG, "Result from Distance Matrix API is "+jsonObj.getString("status"));
                return -1;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Cannot process JSON results", e);
        }

        return -1;
    }


    /**
     * Retrieves map directions between the provided origin and destination according to
     * Google Directions API.
     * <p>
     * Make sure you call this method from a thread running asynchronously from the UI thread
     * <p>
     * @param mode          The mode of transportation. Use DIRECTIONS_WALKING or DIRECTIONS_DRIVING
     * @param origin        The origin point
     * @param destination   The destination
     *
     * @return  Object containing raw JSON output from API. Refer to API page. Returns null if something
     *          goes wrong.
     * @see <a href="https://developers.google.com/maps/documentation/directions/">Google Directions API Documentation</a>
     */
    public org.json.JSONObject getDirections(String mode, LatLng origin, LatLng destination){
        Log.d(TAG, "About to get directions from " + String.valueOf(origin.latitude)+","+String.valueOf(origin.longitude) + " to " + String.valueOf(destination.latitude)+","+String.valueOf(destination.longitude));
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(API_GOOGLE_DIRECTIONS_URL + "/json");
            sb.append("?mode="+mode);
            sb.append("&origin="+String.valueOf(origin.latitude)+","+String.valueOf(origin.longitude));
            sb.append("&destination="+String.valueOf(destination.latitude)+","+String.valueOf(destination.longitude));
            sb.append("&units=metric");
            sb.append("&key=" + context.getResources().getString(R.string.distance_matrix_api_key));

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
            Log.e(TAG, "Error processing Distance Matrix API URL", e);
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Distance Matrix API", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            org.json.JSONObject jsonObj = new org.json.JSONObject(jsonResults.toString());
            return jsonObj;

        } catch (org.json.JSONException e) {
            Log.e(TAG, "Cannot process JSON results", e);
        }

        return null;
    }

    /**
     * Gets the <code>LatLng</code> corresponding to a Google's Places API place ID
     *
     * TODO:make this method not static and implement ProgressListeners
     * <p>
     * Make sure you call this method from a thread running asynchronously from the UI thread
     * <p>
     * @param context   Context from where you are calling this method
     * @param placeID   Google Places API place ID
     *
     * @return  <code>LatLng</code> corresponding to the provided place ID or <code>null</code> if
     *          nothing returned from the API
     *
     * @see <a href="https://developers.google.com/places/documentation/">Google Places API Documentation</a>
     */
    public static LatLng getPlaceLatLng(Context context, String placeID) {
        ArrayList<String[]> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(API_GOOGLE_PLACES_URL + "/details/json");
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

            Log.d(TAG, "Latitude = "+location.getString("lat")+" longitude = "+location.getString("lng")+" for "+placeID);
            return new LatLng(Double.parseDouble(location.getString("lat")), Double.parseDouble(location.getString("lng")));
        } catch (JSONException e) {
            Log.e(TAG, "Cannot process JSON results", e);
        }

        return null;
    }
}