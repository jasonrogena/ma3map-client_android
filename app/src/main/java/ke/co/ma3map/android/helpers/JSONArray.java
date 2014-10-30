package ke.co.ma3map.android.helpers;

import org.json.JSONException;

import java.io.Serializable;

import ke.co.ma3map.android.helpers.JSONObject;

/**
 * Created by jason on 26/09/14.
 */
public class JSONArray extends org.json.JSONArray implements Serializable {
    public JSONArray(String arrayString) throws JSONException {
        super(arrayString);
    }

    public JSONArray(){
        super();
    }

    @Override
    public JSONObject getJSONObject(int index) throws JSONException {
        return new JSONObject(super.getJSONObject(index).toString());
    }
}
