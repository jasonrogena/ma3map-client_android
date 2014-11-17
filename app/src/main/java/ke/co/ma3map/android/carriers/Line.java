package ke.co.ma3map.android.carriers;

import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;
import ke.co.ma3map.android.helpers.JSONArray;

/**
 * Created by jason on 21/09/14.
 */
public class Line implements Parcelable {

    public static final String PARCELABLE_KEY = "Line";
    public static final String[] ALL_COLUMNS = new String[]{"line_id", "route_id", "direction_id"};

    private String id;
    private int directionID;
    private List<Stop> stops;
    private List<Point> points;


    public Line() {
        id = null;
        directionID = -1;
        stops = null;
        points = null;
    }

    public Line(Parcel source){
        this();
        readFromParcel(source);
    }

    public Line(JSONObject lineData) throws JSONException{
        id = lineData.getString("line_id");
        directionID = lineData.getInt("direction_id");

        stops = new ArrayList<Stop>();
        JSONArray stopData = lineData.getJSONArray("stops");
        for(int sIndex = 0; sIndex < stopData.length(); sIndex++){
            stops.add(new Stop(stopData.getJSONObject(sIndex)));
        }

        points = new ArrayList<Point>();
        JSONArray pointData = lineData.getJSONArray("points");
        for(int pIndex = 0; pIndex < pointData.length(); pIndex++){
            points.add(new Point(pointData.getJSONObject(pIndex)));
        }
    }

    public Line(Database database, SQLiteDatabase readableDB, String[] columnValues, boolean light){
        //{"line_id", "route_id", "direction_id"}
        id = columnValues[0];
        directionID = Integer.parseInt(columnValues[2]);

        String query = "line_id='"+id+"'";
        String[][] stopLineRows = database.runSelectQuery(readableDB, Database.TABLE_STOP_LINE, new String[]{"stop_id"}, query, null, null, null, null, null);

        stops = new ArrayList<Stop>();
        for(int stopLineIndex = 0; stopLineIndex < stopLineRows.length; stopLineIndex++){
            Stop currStop = new Stop(database, readableDB, stopLineRows[stopLineIndex][0]);
            stops.add(currStop);
        }

        if(light == false){
            String[][] pointRows = database.runSelectQuery(readableDB, Database.TABLE_POINT, Point.ALL_COLUMNS, query, null, null, null, null, null);
            points = new ArrayList<Point>();
            for(int pointIndex = 0; pointIndex < pointRows.length; pointIndex++){
                Point currPoint = new Point(pointRows[pointIndex]);
                points.add(currPoint);
            }
        }
    }

    public void insertIntoDB(Database database, SQLiteDatabase writableDB, String routeID){
        String[] values = {id, routeID, String.valueOf(directionID)};

        database.runInsertQuery(Database.TABLE_LINE, ALL_COLUMNS, values, -1, writableDB);

        for(int sIndex = 0; sIndex < stops.size(); sIndex++){
            stops.get(sIndex).insertIntoDB(database, writableDB, id);
        }

        for(int pIndex = 0; pIndex < points.size(); pIndex++){
            points.get(pIndex).insertIntoDB(database, writableDB, id);
        }
    }

    /**
     * This method unloads (from memory) all the points in this line. As a way of reducing the object's size.
     * Stops are not unloaded
     */
    public void unloadPoints() {
        if(this.points!= null){
            this.points.clear();
            this.points = null;
        }
    }

    public List<Stop> getStops(){
        return this.stops;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeInt(directionID);
        parcel.writeTypedList(stops);
        parcel.writeTypedList(points);
    }

    public void readFromParcel(Parcel in){
        id = in.readString();
        directionID = in.readInt();
        in.readTypedList(stops, Stop.CREATOR);
        in.readTypedList(points, Point.CREATOR);
    }

    /**
     * This static object is to facilitate for other parcelable objects to carry a Line object
     */
    public static final Creator<Line> CREATOR=new Creator<Line>() {
        @Override
        public Line createFromParcel(Parcel source)
        {
            return new Line(source);
        }

        @Override
        public Line[] newArray(int size)
        {
            return new Line[size];
        }
    };
}
