package ke.co.ma3map.android.carriers;

import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;

import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;

/**
 * Created by jason on 25/09/14.
 */
public class Point implements Parcelable{
    //point_lat text, point_lon text, point_sequence int, dist_traveled int
    public static final String PARCELABLE_KEY = "Point";
    private static final int PARCELABLE_DESC = 6302;
    public static final String[] ALL_COLUMNS = new String[]{"line_id", "point_lat", "point_lon", "point_sequence", "dist_traveled"};

    private String lat;
    private String lon;
    private int sequence;
    private int distTraveled;

    public Point(Parcel source) {
        this();
        readFromParcel(source);
    }

    public Point() {
        lat = null;
        lon = null;
        sequence = -1;
        distTraveled = -1;
    }

    public Point(JSONObject pointData) throws JSONException{
        lat = pointData.getString("point_lat");
        lon = pointData.getString("point_lon");
        sequence = pointData.getInt("point_sequence");
        distTraveled = pointData.getInt("dist_traveled");
    }

    public Point(String[] columnValues){
        //{"line_id", "point_lat", "point_lon", "point_sequence", "dist_traveled"};
        lat = columnValues[1];
        lon = columnValues[2];
        sequence = Integer.parseInt(columnValues[3]);
        distTraveled = Integer.parseInt(columnValues[4]);
    }

    public void insertIntoDB(Database database, SQLiteDatabase writableDB, String lineID){
        String[] values = {lineID, lat, lon, String.valueOf(sequence), String.valueOf(distTraveled)};

        database.runInsertQuery(Database.TABLE_POINT, ALL_COLUMNS, values, -1, writableDB);
    }

    @Override
    public int describeContents() {
        return PARCELABLE_DESC;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(lat);//1
        parcel.writeString(lon);//2
        parcel.writeInt(distTraveled);//3
        parcel.writeInt(sequence);//4
    }

    public void readFromParcel(Parcel in) {
        lat = in.readString();//1
        lon = in.readString();//2
        distTraveled = in.readInt();//3
        sequence = in.readInt();//4
    }

    /**
     * This static object is to facilitate for other parcelable objects to carry a point object
     */
    public static final Creator<Point> CREATOR=new Creator<Point>() {
        @Override
        public Point createFromParcel(Parcel source)
        {
            return new Point(source);
        }

        @Override
        public Point[] newArray(int size)
        {
            return new Point[size];
        }
    };
}
