package ke.co.ma3map.android.carriers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import ke.co.ma3map.android.handlers.Data;
import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;
import ke.co.ma3map.android.helpers.JSONArray;

/**
 * Created by jason on 21/09/14.
 */
public class Line implements Parcelable {

    private static final String TAG = "ma3map.Line";

    public static final String PARCELABLE_KEY = "Line";
    private static final int PARCELABLE_DESC = 9324;
    public static final String[] ALL_COLUMNS = new String[]{"line_id", "route_id", "direction_id"};

    private String id;
    private int directionID;
    private List<Stop> stops;
    private List<Point> points;


    public Line() {
        id = null;
        directionID = -1;
        stops = new ArrayList<Stop>();
        points = new ArrayList<Point>();
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

    public boolean isStopInLine(Stop stop){
        for(int index = 0; index < stops.size(); index++){
            //if(stops.get(index).getLat().equals(stop.getLat()) && stops.get(index).getLon().equals(stop.getLon())) {
            if(stops.get(index).equals(stop)) {
                return true;
            }
        }
        return false;
    }

    public double getDistanceToStop(Stop stop){
        //first check if stop is in line
        if(isStopInLine(stop)){
            return 0;
        }
        else {
            double closest = -1;
            for(int index = 0; index < stops.size(); index++){
                double currDistance = stops.get(index).getDistance(stop.getLatLng());
                if(closest == -1 || currDistance < closest){
                    closest = currDistance;
                }
            }
            return closest;
        }
    }

    public Stop getClosestStop(Stop stop){
        Stop closestStop = null;
        double closestDistance = -1;
        for(int index = 0; index < stops.size(); index++){
            double currDistance = stops.get(index).getDistance(stop.getLatLng());
            if(closestDistance == -1 || currDistance < closestDistance){
                closestStop = stops.get(index);
                closestDistance = currDistance;
            }
        }

        return closestStop;
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

    /**
     * This method loads all points corresponding to this line from the SQLite database if they are not
     * already loaded.
     *
     * @param context   The context from where this method is called
     */
    public void loadPoints(Context context){
        if(this.points == null || this.points.size() == 0){
            Data dataHandler = new Data(context);
            points = dataHandler.getLinePoints(id);
        }
        else {
            Log.i(TAG, "Points for current line already loaded. not loading again");
        }
    }

    /**
     * This method returns the GIS polyline corresponding to this line with endPointA and endPointB
     * being the endpoints in the polyline
     *
     * @param endPointA     The first endpoint in the polyline
     * @param endPointB     The second endpoint in hte polyline
     *
     * @return  The GIS line corresponding to this line (ordered by the points' sequence ids)
     */
    public ArrayList<LatLng> getPolyline(Stop endPointA, Stop endPointB){
        ArrayList<LatLng> polyline = new ArrayList<LatLng>();

        if(points != null){
            //get the sequence number for the point closest to endPointA
            int endPointASN = points.get(0).getSequence();
            double endPointACD = endPointA.getDistance(points.get(0).getLatLng());
            for(int pointIndex = 1; pointIndex < points.size(); pointIndex++){
                double currDistance = endPointA.getDistance(points.get(pointIndex).getLatLng());
                if(currDistance < endPointACD){
                    endPointACD = currDistance;
                    endPointASN = points.get(pointIndex).getSequence();
                }
            }

            //get the sequence number for the point closest to endPointB
            int endPointBSN = points.get(0).getSequence();
            double endPointBCD = endPointB.getDistance(points.get(0).getLatLng());
            for(int pointIndex = 1; pointIndex < points.size(); pointIndex++){
                double currDistance = endPointB.getDistance(points.get(pointIndex).getLatLng());
                if(currDistance < endPointBCD){
                    endPointBCD = currDistance;
                    endPointBSN = points.get(pointIndex).getSequence();
                }
            }

            int seq1 = -1;
            int seq2 = -1;

            if(endPointASN < endPointBSN){
                seq1 = endPointASN;
                seq2 = endPointBSN;
            }
            else {
                seq2 = endPointASN;
                seq1 = endPointBSN;
            }

            for(int pointIndex = 0; pointIndex < points.size(); pointIndex++){
                if(points.get(pointIndex).getSequence() >= seq1 && points.get(pointIndex).getSequence() <= seq2){
                    polyline.add(points.get(pointIndex).getLatLng());
                }
            }
        }
        return polyline;
    }

    public List<Stop> getStops(){
        return this.stops;
    }

    @Override
    public int describeContents() {
        return PARCELABLE_DESC;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);//1
        parcel.writeInt(directionID);//2
        parcel.writeTypedList(stops);//3
        parcel.writeTypedList(points);//4
    }

    public void readFromParcel(Parcel in){
        id = in.readString();//1
        directionID = in.readInt();//2
        in.readTypedList(stops, Stop.CREATOR);//3
        in.readTypedList(points, Point.CREATOR);//4
    }

    /**
     * This static object is to facilitate for other parcelable objects to carry a Line object
     */
    public static final Creator<Line> CREATOR=new Creator<Line>() {
        @Override
        public Line createFromParcel(Parcel source) {
            return new Line(source);
        }

        @Override
        public Line[] newArray(int size) {
            return new Line[size];
        }
    };
}
