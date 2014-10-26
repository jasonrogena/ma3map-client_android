package ke.co.ma3map.android.carriers;

import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;

import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;

/**
 * Created by jason on 25/09/14.
 */
public class Point {
    //point_lat text, point_lon text, point_sequence int, dist_traveled int

    private String lat;
    private String lon;
    private int sequence;
    private int distTraveled;

    public Point(JSONObject pointData) throws JSONException{
        lat = pointData.getString("point_lat");
        lon = pointData.getString("point_lon");
        sequence = pointData.getInt("point_sequence");
        distTraveled = pointData.getInt("dist_traveled");
    }

    public void insertIntoDB(Database database, SQLiteDatabase writableDB, String lineID){
        String[] columns = {"line_id", "point_lat", "point_lon", "point_sequence", "dist_traveled"};
        String[] values = {lineID, lat, lon, String.valueOf(sequence), String.valueOf(distTraveled)};

        database.runInsertQuery(Database.TABLE_POINT, columns, values, -1, writableDB);
    }
}
