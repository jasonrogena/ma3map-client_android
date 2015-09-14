package ke.co.ma3map.android.carriers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;
import ke.co.ma3map.android.helpers.JSONArray;

/**
 * Created by jason on 21/09/14.
 */
public class Route implements Parcelable {
    public static final String TAG = "ma3map.Route";
    public static final String PARCELABLE_KEY = "Route";
    private static final int PARCELABLE_DESC = 7522;
    public static final String[] ALL_COLUMNS = new String[]{"route_id", "route_short_name", "route_long_name", "route_desc", "route_type", "route_url", "route_color", "route_text_color"};

    private String shortName;
    private String longName;
    private String id;
    private String desc;
    private int type;
    private String url;
    private String color;
    private String textColor;
    private List<Line> lines;

    public Route() {
        shortName = null;
        longName = null;
        id = null;
        desc = null;
        type = -1;
        url = null;
        color = null;
        textColor = null;
        lines = new ArrayList<Line>();
    }

    public Route(Parcel source){
        this();
        readFromParcel(source);
        //Log.d(TAG, "Initialized route from parcel");
    }

    public Route(JSONObject routeData) throws JSONException{
        shortName = routeData.getString("short_name");
        longName = routeData.getString("long_name");
        id = routeData.getString("id");
        desc = routeData.getString("description");
        type = routeData.getInt("type");
        url = routeData.getString("url");
        color = routeData.getString("color");
        textColor = routeData.getString("text_color");

        lines = new ArrayList<Line>();
        JSONArray lineData = routeData.getJSONArray("lines");
        for(int lIndex  = 0; lIndex < lineData.length(); lIndex++){
            lines.add(new Line(lineData.getJSONObject(lIndex)));
        }
    }

    public Route(Database database, SQLiteDatabase readableDB, String[] columnValues, boolean light){
        //"route_id", "route_short_name", "route_long_name", "route_desc", "route_type", "route_url", "route_color", "route_text_color"
        Log.d(TAG, "routeID = "+columnValues[0]);
        id = columnValues[0];
        Log.d(TAG, "shortName = "+columnValues[1]);
        shortName = columnValues[1];
        Log.d(TAG, "longName = "+columnValues[2]);
        longName = columnValues[2];
        Log.d(TAG, "desc = "+columnValues[3]);
        desc = columnValues[3];
        Log.d(TAG, "type = "+columnValues[4]);
        type = Integer.parseInt(columnValues[4]);
        Log.d(TAG, "url = "+columnValues[5]);
        url = columnValues[5];
        Log.d(TAG, "color = "+columnValues[6]);
        color = columnValues[6];
        Log.d(TAG, "textColor = "+columnValues[7]);
        textColor = columnValues[7];

        String query = "route_id='"+id+"'";

        String[][] lineRows = database.runSelectQuery(readableDB, Database.TABLE_LINE, Line.ALL_COLUMNS, query, null, null, null, null, null);
        lines = new ArrayList<Line>();

        for(int lineIndex = 0; lineIndex < lineRows.length; lineIndex++){
            Line currLine = new Line(database, readableDB, lineRows[lineIndex], light);
            lines.add(currLine);
        }
    }

    public void insertIntoDB(Database database, SQLiteDatabase writableDB){
        String[] values = {id, shortName, longName, desc, String.valueOf(type), url, color, textColor};
        database.runInsertQuery(Database.TABLE_ROUTE, ALL_COLUMNS, values, -1, writableDB);

        for(int lIndex = 0; lIndex < lines.size(); lIndex++){
            lines.get(lIndex).insertIntoDB(database, writableDB, id);
        }
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public String getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public int getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getColor() {
        return color;
    }

    public String getTextColor() {
        return textColor;
    }

    public List<Line> getLines() {
        return lines;
    }

    public List<Stop> getStops(int lineIndex){
        return lines.get(lineIndex).getStops();
    }

    public boolean isStopInRoute(Stop stop){
        for(int index = 0; index < lines.size(); index++){
            if(lines.get(index).isStopInLine(stop)){
                return true;
            }
        }
        return false;
    }

    public double getDistanceToStop(Stop stop){
        double closest = -1;
        for(int index = 0; index < lines.size(); index++){
            double currDistance = lines.get(index).getDistanceToStop(stop);
            if(closest == -1 || currDistance < closest){
                closest = currDistance;
            }
        }
        return closest;
    }

    public Stop getClosestStop(Stop stop){
        Stop closestStop = null;
        double closestDistance = -1;
        for(int index = 0; index < lines.size(); index++){
            Stop currCloseStop = lines.get(index).getClosestStop(stop);

            if(closestDistance == -1 || (currCloseStop != null && currCloseStop.getDistance(stop.getLatLng()) < closestDistance)){
                closestStop = currCloseStop;
                if(currCloseStop != null){
                    closestDistance = currCloseStop.getDistance(stop.getLatLng());
                }
            }
        }

        return closestStop;
    }

    @Override
    public int describeContents() {
        return PARCELABLE_DESC;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(shortName);//1
        parcel.writeString(longName);//2
        parcel.writeString(id);//3
        parcel.writeString(desc);//4
        parcel.writeInt(type);//5
        parcel.writeString(url);//6
        parcel.writeString(color);//7
        parcel.writeString(textColor);//8
        parcel.writeTypedList(lines);//9
    }

    public void readFromParcel(Parcel in){
        shortName = in.readString();//1
        longName = in.readString();//2
        id = in.readString();//3
        desc = in.readString();//4
        type = in.readInt();//5
        url = in.readString();//6
        color = in.readString();//7
        textColor = in.readString();//8
        in.readTypedList(lines, Line.CREATOR);//9
    }

    /**
     * This static object is to facilitate for other parcelable objects to carry a Route object
     */
    public static final Creator<Route> CREATOR=new Creator<Route>() {
        @Override
        public Route createFromParcel(Parcel source)
        {
            return new Route(source);
        }

        @Override
        public Route[] newArray(int size)
        {
            return new Route[size];
        }
    };

    /**
     * This method unloads all points in all the lines in this route. Stops are not unloaded
     */
    public void unloadPoints(){
        if(this.lines != null){
            for(int i =0; i < this.lines.size(); i++){
                this.lines.get(i).unloadPoints();
            }
        }
    }

    /**
     * This method loads the points for each of the route's lines from the SQLite database
     *
     * @param context   The Context from where this method is called
     */
    public void loadPoints(Context context){
        if(this.lines != null){
            for(int i = 0; i < this.lines.size(); i++){
                this.lines.get(i).loadPoints(context);
            }
        }
    }

    /**
     * This method gets the GIS polyline corresponding to this route. Start and destination will be
     * the endpoints in the polyline
     *
     * @param endPointA     The first endpoint in the polyline
     * @param endPointB     The second enpoint in the polyline
     * @return
     */
    public ArrayList<LatLng> getPolyline(Stop endPointA, Stop endPointB){
        ArrayList<LatLng> polyline = new ArrayList<LatLng>();
        for(int lineIndex = 0; lineIndex < lines.size(); lineIndex++){
            polyline.addAll(lines.get(lineIndex).getPolyline(endPointA, endPointB));
        }

        return polyline;
    }
}
