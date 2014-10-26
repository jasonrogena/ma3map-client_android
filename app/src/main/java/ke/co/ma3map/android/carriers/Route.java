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
public class Route {

    private String shortName;
    private String longName;
    private String id;
    private String desc;
    private int type;
    private String url;
    private String color;
    private String textColor;
    private List<Line> lines;

    public Route(JSONObject routeData) throws JSONException{
        shortName = routeData.getString("route_short_name");
        longName = routeData.getString("route_long_name");
        id = routeData.getString("route_id");
        desc = routeData.getString("route_desc");
        type = routeData.getInt("route_type");
        url = routeData.getString("route_url");
        color = routeData.getString("route_color");
        textColor = routeData.getString("route_text_color");

        lines = new ArrayList<Line>();
        JSONArray lineData = routeData.getJSONArray("lines");
        for(int lIndex  = 0; lIndex < lineData.length(); lIndex++){
            lines.add(new Line(lineData.getJSONObject(lIndex)));
        }
    }

    public void insertIntoDB(Database database, SQLiteDatabase writableDB){
        String[] columns = {"route_id", "route_short_name", "route_long_name", "route_desc", "route_type", "route_url", "route_color", "route_text_color"};
        String[] values = {id, shortName, longName, desc, String.valueOf(type), url, color, textColor};
        database.runInsertQuery(Database.TABLE_ROUTE, columns, values, -1, writableDB);

        for(int lIndex = 0; lIndex < lines.size(); lIndex++){
            lines.get(lIndex).insertIntoDB(database, writableDB, id);
        }
    }

}
