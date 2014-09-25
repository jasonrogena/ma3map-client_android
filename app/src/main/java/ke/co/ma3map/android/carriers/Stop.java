package ke.co.ma3map.android.carriers;

import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import ke.co.ma3map.android.helpers.Database;

/**
 * Created by jason on 21/09/14.
 */
public class Stop {
    //stop_id text, stop_name text, stop_code text, stop_desc text, stop_lat text, stop_lon text, location_type int, parent_station text
    private String id;
    private String name;
    private String code;
    private String desc;
    private String lat;
    private String lon;
    private int locationType;
    private String parentStation;

    public Stop(JSONObject stopData){
        try{
            id = stopData.getString("stop_id");
            name = stopData.getString("stop_name");
            code = stopData.getString("stop_code");
            desc = stopData.getString("stop_desc");
            lat = stopData.getString("stop_lat");
            lon = stopData.getString("stop_lon");
            locationType = stopData.getInt("location_type");
            parentStation = stopData.getString("parent_station");
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void insertIntoDB(Database database, SQLiteDatabase writableDB, String lineID){
        //check if stop already in database
        String[][] results = database.runSelectQuery(writableDB, Database.TABLE_STOP, new String[]{"stop_id"}, "stop_id="+id, null, null, null, null, null);
        if(results.length == 0){
            String[] columns = {"stop_id", "stop_name", "stop_code", "stop_desc", "stop_lat", "stop_lon", "location_type", "parent_station"};
            String[] values = {id, name, code, desc, lat, lon, String.valueOf(locationType), parentStation};

            database.runInsertQuery(Database.TABLE_STOP, columns, values, 0, writableDB);
        }

        database.runInsertQuery(Database.TABLE_STOP_LINE, new String[]{"stop_id", "line_id"}, new String[]{id, lineID}, -1, writableDB);
    }
}
