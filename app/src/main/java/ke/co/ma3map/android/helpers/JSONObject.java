package ke.co.ma3map.android.helpers;

import org.json.JSONException;

import ke.co.ma3map.android.helpers.JSONArray;

/**
 * Created by jason on 26/09/14.
 */
public class JSONObject extends org.json.JSONObject {
    public JSONObject(String jsonString) throws JSONException {
        super(jsonString);
    }

    public JSONObject(org.json.JSONObject jsonObject) throws JSONException {
        this(jsonObject.toString());
    }

    public JSONObject(){
        super();
    }

    @Override
    public int getInt(String name) throws JSONException {
        if(isNull(name)){
            return -1;
        }
        else {
            String intString = super.getString(name).trim();
            if(intString.length() == 0){
                return -1;
            }
            else {
                return Integer.parseInt(intString);
            }
        }
    }

    @Override
    public String getString(String name) throws JSONException {
        if(isNull(name)){
            return "";
        }
        else {
            return super.getString(name).trim();
        }
    }

    @Override
    public JSONArray getJSONArray(String name) throws JSONException {
        return new JSONArray(super.getJSONArray(name).toString());
    }
}
