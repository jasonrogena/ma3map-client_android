package ke.co.ma3map.android.carriers;

import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;

import ke.co.ma3map.android.helpers.Database;
import ke.co.ma3map.android.helpers.JSONObject;

/**
 * Created by jason on 21/09/14.
 */
public class Stop implements Parcelable{
    //stop_id text, stop_name text, stop_code text, stop_desc text, stop_lat text, stop_lon text, location_type int, parent_station text
    public static final String PARCELABLE_KEY = "Stop";

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
    }

    public Stop(JSONObject stopData) throws JSONException{
        id = stopData.getString("stop_id");
        name = stopData.getString("stop_name");
        code = stopData.getString("stop_code");
        desc = stopData.getString("stop_desc");
        lat = stopData.getString("stop_lat");
        lon = stopData.getString("stop_lon");
        locationType = stopData.getInt("location_type");
        parentStation = stopData.getString("parent_station");
    }

    public void insertIntoDB(Database database, SQLiteDatabase writableDB, String lineID){
        String[] columns = {"stop_id", "stop_name", "stop_code", "stop_desc", "stop_lat", "stop_lon", "location_type", "parent_station"};
        String[] values = {id, name, code, desc, lat, lon, String.valueOf(locationType), parentStation};

        database.runInsertQuery(Database.TABLE_STOP, columns, values, 0, writableDB);

        database.runInsertQuery(Database.TABLE_STOP_LINE, new String[]{"stop_id", "line_id"}, new String[]{id, lineID}, -1, writableDB);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(code);
        parcel.writeString(desc);
        parcel.writeString(lat);
        parcel.writeInt(locationType);
        parcel.writeString(parentStation);
    }

    public void readFromParcel(Parcel in){
        id = in.readString();
        name = in.readString();
        code = in.readString();
        desc = in.readString();
        lat = in.readString();
        lon = in.readString();
        locationType = in.readInt();
        parentStation = in.readString();
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
}
