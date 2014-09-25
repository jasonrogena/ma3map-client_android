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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
     * @param context The activity/service from where you want to check for the connection
     * @param uri     From where in the server you want to get the data from use the URI_* constants in this Class
     * @param data    The data as a json object
     * @return Returns a jsonObject with that looks like this {data, error, message}.
     * Error stores a boolean (True if an error occured). Message being present doesn't mean an error occurred
     */
    public static JSONObject getDataFromServer(Context context, String uri, JSONObject data) {
        JSONObject response = new JSONObject();
        try {
            response.put("message", "");
            response.put("error", new Boolean(false));
            response.put("data", new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }

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
                    try {
                        response.put("message", "Something when wrong while trying to encode the data before sending to the server");
                        response.put("error", new Boolean(true));
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            String dataString = URLEncodedUtils.format(nameValuePairs, "utf-8");

            Log.d(TAG, "Sending data to this url " + SERVER_URL + uri + dataString);
            HttpGet httpGet = new HttpGet(SERVER_URL + uri + dataString);
            try {
                HttpResponse httpResponse = httpClient.execute(httpGet);
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    Header[] headers = httpResponse.getAllHeaders();
                    for(int i =0; i < headers.length; i++){
                        Log.i(TAG, headers[i].getName() + " " + headers[i].getValue());
                    }

                    HttpEntity httpEntity = httpResponse.getEntity();
                    if (httpEntity != null) {
                        InputStream inputStream = httpEntity.getContent();
                        String responseString = convertStreamToString(inputStream);

                        JSONArray jsonArray = new JSONArray(responseString.trim());
                        response.put("message", "Data gotten from the server");
                        response.put("data", jsonArray);
                    }
                    else{
                        response.put("message", "No data received from the server");
                    }
                } else {
                    Log.e(TAG, "Status Code " + String.valueOf(httpResponse.getStatusLine().getStatusCode()) + " passed");
                    response.put("message", "Server responded with the status code " + String.valueOf(httpResponse.getStatusLine().getStatusCode()));
                    response.put("error", new Boolean(true));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                try {
                    response.put("message", "An error occurred while trying to convert the data received from the server");
                    response.put("error", new Boolean(true));
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e){
                e.printStackTrace();
                try {
                    response.put("message", "An error occurred while trying to connect to the server");
                    response.put("error", new Boolean(true));
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return response;
    }

    private static List<Route> cacheMapData(Context context, JSONObject serverResponse){
        List<Route> routes = new ArrayList<Route>();
        try {
            if(serverResponse.getBoolean("error") == false){
                JSONArray data = serverResponse.getJSONArray("data");
                Database database = new Database(context);
                SQLiteDatabase writeableDB = database.getWritableDatabase();

                database.runTruncateQuery(writeableDB, Database.TABLE_POINT);
                database.runTruncateQuery(writeableDB, Database.TABLE_STOP);
                database.runTruncateQuery(writeableDB, Database.TABLE_LINE);
                database.runTruncateQuery(writeableDB, Database.TABLE_ROUTE);

                for(int routeIndex = 0; routeIndex < data.length(); routeIndex++){
                    routes.add(new Route(data.getJSONObject(routeIndex)));
                }
            }
        }
        catch (JSONException e){
            e.printStackTrace();
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
    public static List<Route> downloadAllRouteData(Context context){

        JSONObject serverResponse = getDataFromServer(context, URI_API_GET_ROUTES, new JSONObject());
        return cacheMapData(context, serverResponse);
    }
}