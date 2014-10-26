package ke.co.ma3map.android.carriers;

import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;
import ke.co.ma3map.android.helpers.JSONArray;

/**
 * Created by jason on 21/09/14.
 */
public class Line {

    private String id;
    private int directionID;
    private List<Stop> stops;
    private List<Point> points;

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
}
