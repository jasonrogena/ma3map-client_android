package ke.co.ma3map.android.carriers;

import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.util.Comparator;

import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;

/**
 * Created by jason on 21/09/14.
 */
public class Stop implements Parcelable{
    private static final String TAG = "ma3map.Stop";
    //stop_id text, stop_name text, stop_code text, stop_desc text, stop_lat text, stop_lon text, location_type int, parent_station text
    public static final String PARCELABLE_KEY = "Stop";
    private static final int PARCELABLE_DESC = 1834;
    public static final String[] ALL_COLUMNS = new String[]{"stop_id", "stop_name", "stop_code", "stop_desc", "stop_lat", "stop_lon", "location_type", "parent_station"};

    private String id;
    private String name;
    private String code;
    private String desc;
    private String lat;
    private String lon;
    private int locationType;
    private String parentStation;

    public Stop(){
        id = null;
        name = null;
        code = null;
        desc = null;
        lat = null;
        lon = null;
        locationType = -1;
        parentStation = null;
    }

    public Stop(Parcel source){
        this();
        readFromParcel(source);
        //Log.d(TAG, "Initialized stop from parcel");
    }

    public Stop(JSONObject stopData) throws JSONException{
        id = stopData.getString("id");
        name = stopData.getString("name");
        code = stopData.getString("code");
        desc = stopData.getString("desc");
        lat = stopData.getString("lat");
        lon = stopData.getString("lng");
        locationType = stopData.getInt("location_type");
        parentStation = stopData.getString("parent_station");
    }

    public Stop(Database database, SQLiteDatabase readableDB, String id){
        String query = "stop_id='"+id+"'";
        String[][] rows = database.runSelectQuery(readableDB, Database.TABLE_STOP, ALL_COLUMNS, query, null, null, null, null, null);
        if(rows != null && rows.length == 1){
            //{"stop_id", "stop_name", "stop_code", "stop_desc", "stop_lat", "stop_lon", "location_type", "parent_station"}
            name = rows[0][1];
            code = rows[0][2];
            desc = rows[0][3];
            lat = rows[0][4];
            lon = rows[0][5];
            locationType = Integer.parseInt(rows[0][6]);
            parentStation = rows[0][7];
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public void setLocationType(int locationType) {
        this.locationType = locationType;
    }

    public void setParentStation(String parentStation) {
        this.parentStation = parentStation;
    }

    public void insertIntoDB(Database database, SQLiteDatabase writableDB, String lineID){
        String[] values = {id, name, code, desc, lat, lon, String.valueOf(locationType), parentStation};

        database.runInsertQuery(Database.TABLE_STOP, ALL_COLUMNS, values, 0, writableDB);

        database.runInsertQuery(Database.TABLE_STOP_LINE, new String[]{"stop_id", "line_id"}, new String[]{id, lineID}, -1, writableDB);
    }

    /**
     * This method returns the distance in metres between this stop and the provided point
     *
     * @param point The point you want to get the distance to
     *
     * @return The distance in metres between this point and the provided point. Will return -1 if
     *          unable to calculate the distance
     */
    public double getDistance(LatLng point){
        final int earthRadius = 6371;
        LatLng stopLocation = getLatLng();

        double latDiff = Math.toRadians(stopLocation.latitude - point.latitude);
        double lonDiff = Math.toRadians(stopLocation.longitude - point.longitude);

        double a = (Math.sin(latDiff/2) * Math.sin(latDiff/2))
                    + Math.sin(lonDiff/2)
                    * Math.sin(lonDiff/2)
                    * Math.cos(Math.toRadians(stopLocation.latitude))
                    * Math.cos(Math.toRadians(point.latitude));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double d = earthRadius * c;

        return d * 1609;//convert to metres
    }

    public String getId(){
        return this.id;
    }

    public String getLat(){
        return this.lat;
    }

    public String getLon(){
        return this.lon;
    }

    public String getName(){
        return this.name;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public int getLocationType() {
        return locationType;
    }

    public String getParentStation() {
        return parentStation;
    }

    public LatLng getLatLng(){
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
    }

    public boolean equals(Stop comparison){
        if(comparison.getLat().equals(lat) && comparison.getLon().equals(lon) && comparison.getName().equals(name)){
            return true;
        }
        return false;
    }

    @Override
    public int describeContents() {
        return PARCELABLE_DESC;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);//1
        parcel.writeString(name);//2
        parcel.writeString(code);//3
        parcel.writeString(desc);//4
        parcel.writeString(lat);//5
        parcel.writeString(lon);//6
        parcel.writeInt(locationType);//7
        parcel.writeString(parentStation);//8
    }

    public void readFromParcel(Parcel in){
        id = in.readString();//1
        name = in.readString();//2
        code = in.readString();//3
        desc = in.readString();//4
        lat = in.readString();//5
        lon = in.readString();//6
        locationType = in.readInt();//7
        parentStation = in.readString();//8
    }

    /**
     * This static object is to facilitate for other parcelable objects to carry a Stop object
     */
    public static final Creator<Stop> CREATOR=new Creator<Stop>() {
        @Override
        public Stop createFromParcel(Parcel source)
        {
            return new Stop(source);
        }

        @Override
        public Stop[] newArray(int size)
        {
            return new Stop[size];
        }
    };

    public static class DistanceComparator implements Comparator<Stop> {

        LatLng reference;

        public DistanceComparator(LatLng reference){
            this.reference = reference;
        }

        @Override
        public int compare(Stop s0, Stop s1) {
            double d0 = s0.getDistance(reference);
            double d1 = s1.getDistance(reference);

            if(d0 < d1){
                return -1;
            }
            else if(d0 == d1){
                return 0;
            }
            else {
                return 1;
            }
        }
    }
}
