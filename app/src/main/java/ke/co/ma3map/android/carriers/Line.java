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

    public void insertIntoDB(Database database, SQLiteDatabase writableDB, String routeID){
        String[] columns = {"line_id", "route_id", "direction_id"};
        String[] values = {id, routeID, String.valueOf(directionID)};

        database.runInsertQuery(Database.TABLE_LINE, columns, values, -1, writableDB);

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
