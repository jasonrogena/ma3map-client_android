package ke.co.ma3map.android.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.melnykov.fab.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ke.co.ma3map.android.R;
import ke.co.ma3map.android.services.GetRouteData;

public class Map extends Activity
                 implements GooglePlayServicesClient.ConnectionCallbacks,
                            GoogleApiClient.OnConnectionFailedListener,
                            View.OnClickListener,
                            GoogleMap.OnMapClickListener,
                            View.OnFocusChangeListener{

    private final String TAG = "Map";

    private final int DEFAULT_ZOOM = 10;
    private final long I_LAYOUT_TRANS_TIME = 600;//the time in milliseconds it will take to hide/show the interaction layout
    private final double I_LAYOUT_HIGHT_RATIO = 0.9;//the ratio of height of the interactionLL to the activityHeight
    private final int MODE_MAP = 0;
    private final int MODE_DROP_PIN = 1;
    private final int MODE_NAVIGATE = 2;
    private final int PIN_FROM = 0;
    private final int PIN_TO = 1;


    private int mode;
    private int pin;
    private GoogleMap googleMap;
    private LocationClient locationClient;

    private LinearLayout interactionLL;
    private RelativeLayout routeSelectionRL;
    private AutoCompleteTextView fromACTV;
    private AutoCompleteTextView toACTV;
    private FloatingActionButton navigateFAB;
    private EditText phantomET;
    private Button fromDropPinB;
    private Button fromLocationB;
    private Button toDropPinB;
    private Button toLocationB;
    private Marker fromMarker;
    private Marker toMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //check if google services is enabled
        final int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS){
            Toast.makeText(this, "This app requires Google Play Services in order to work", Toast.LENGTH_LONG).show();
            finish();
        }

        mode = MODE_MAP;

        //init views
        interactionLL = (LinearLayout)this.findViewById(R.id.interaction_ll);
        interactionLL.setOnClickListener(this);
        double iLLHeight = getWindowHeight()*I_LAYOUT_HIGHT_RATIO;
        ViewGroup.LayoutParams iLLLayoutParams = interactionLL.getLayoutParams();
        iLLLayoutParams.height = (int)iLLHeight;
        interactionLL.setLayoutParams(iLLLayoutParams);

        routeSelectionRL = (RelativeLayout)this.findViewById(R.id.route_selection_rl);

        fromACTV = (AutoCompleteTextView)this.findViewById(R.id.from_actv);
        fromACTV.setOnFocusChangeListener(this);
        //fromACTV.setOnClickListener(this);
        fromDropPinB = (Button)this.findViewById(R.id.from_drop_pin_b);
        fromDropPinB.setOnClickListener(this);
        fromLocationB = (Button)this.findViewById(R.id.from_location_b);
        fromLocationB.setOnClickListener(this);

        toACTV = (AutoCompleteTextView)this.findViewById(R.id.to_actv);
        toACTV.setOnFocusChangeListener(this);
        //toACTV.setOnClickListener(this);
        toDropPinB = (Button)this.findViewById(R.id.to_drop_pin_b);
        toDropPinB.setOnClickListener(this);
        toLocationB = (Button)this.findViewById(R.id.to_location_b);
        toLocationB.setOnClickListener(this);

        navigateFAB = (FloatingActionButton)this.findViewById(R.id.navigate_fab);
        navigateFAB.setShadow(true);
        navigateFAB.setColorNormalResId(R.color.accent);
        navigateFAB.setColorPressedResId(R.color.accent_light);
        navigateFAB.setImageResource(R.drawable.ic_navigate);
        navigateFAB.setOnClickListener(this);

        googleMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.setOnMapClickListener(this);

        locationClient = new LocationClient(this, this, this);

        interactionLL.setY(getWindowHeight());

        phantomET = new EditText(this);
        phantomET.setVisibility(EditText.INVISIBLE);
        interactionLL.addView(phantomET);

        //ask user for permission to get route data
        getRouteData();
    }

    /**
     * Make sure you initialize all hardware resources here because they will be
     * released in onPause
     */
    @Override
    protected void onResume() {
        super.onResume();

        if(!locationClient.isConnected()){
            locationClient.connect();
        }
    }

    /**
     * Release all hardware resources here and not onDestroy to avoid blocking the resources
     * when not using them
     */
    @Override
    protected void onPause(){
        super.onPause();

        locationClient.disconnect();
    }

    /**
     * This method zooms in on the devices location.
     * Should be run after resources initialized i.e. in onResume()
     */
    private void zoomInOnLocation(){
        Location myLocation = locationClient.getLastLocation();

        if(myLocation != null){
            LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, DEFAULT_ZOOM));
            Log.d(TAG, "Zoomed in on user's position");
        }
        else {
            Log.w(TAG, "Unable to get user's location");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        zoomInOnLocation();
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * This method first asks for permission from the user before spawning a service
     * that gets the route data
     */
    private void getRouteData(){

        /*final Dialog dialog = new Dialog(Map.this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog);

        TextView dialogTitle = (TextView)dialog.findViewById(R.id.title);
        dialogTitle.setText("Matatu Route Data");
        TextView dialogBody = (TextView)dialog.findViewById(R.id.body);
        dialogBody.setText(R.string.warning_download_route_data);

        Button positive = (Button)dialog.findViewById(R.id.positive_button);
        positive.setText(R.string.okay);
        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Map.this, GetRouteData.class);
                Map.this.startService(intent);
                dialog.dismiss();
            }
        });

        Button negative = (Button)dialog.findViewById(R.id.negative_button);
        negative.setText(R.string.later);
        negative.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();*/
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Matatu Route Data");
        dialogBuilder.setMessage(R.string.warning_download_route_data);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent=new Intent(Map.this, GetRouteData.class);
                Map.this.startService(intent);
                dialogInterface.dismiss();
            }
        });
        dialogBuilder.setNegativeButton(R.string.later, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onclick called");
        if(view == navigateFAB){
            Log.d(TAG, "routeActivationB clicked");
            //toggleInteractionLayout();
            navigateFAB.hide(true);
            expandInteractionLayout();
        }
        else if(view == fromDropPinB){
            pin = PIN_FROM;
            setDropPinMode();
        }
        else if(view == fromLocationB){
            Location myLocation = locationClient.getLastLocation();
            if(myLocation != null){
                fromACTV.setText(getLocationName(new LatLng(myLocation.getLatitude(), myLocation.getLongitude())));
            }
        }
        else if(view == toDropPinB){
            pin = PIN_TO;
            setDropPinMode();
        }
        else if(view == toLocationB){
            Location myLocation = locationClient.getLastLocation();
            if(myLocation != null){
                toACTV.setText(getLocationName(new LatLng(myLocation.getLatitude(), myLocation.getLongitude())));
            }
        }
        else if(view == interactionLL || view == fromACTV || view == toACTV){
            if(mode == MODE_DROP_PIN) expandInteractionLayout();
        }
    }

    private void expandInteractionLayout() {
        mode = MODE_NAVIGATE;
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        double newY = getWindowHeight()*(1-I_LAYOUT_HIGHT_RATIO);
        ObjectAnimator yAnimator = ObjectAnimator.ofFloat(interactionLL,"y",(float)newY);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(I_LAYOUT_TRANS_TIME);
        animatorSet.play(yAnimator);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.start();
    }

    private void collapseInteractionLayout(){
        mode = MODE_MAP;
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(phantomET.getWindowToken(), 0);

        ObjectAnimator yAnimator = ObjectAnimator.ofFloat(interactionLL,"y", (float)getWindowHeight());
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(yAnimator);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setStartDelay(300);
        animatorSet.start();
    }

    private void setDropPinMode(){
        mode = MODE_DROP_PIN;
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(phantomET.getWindowToken(), 0);

        ViewGroup.LayoutParams layoutParams = routeSelectionRL.getLayoutParams();
        float newY = getWindowHeight() - routeSelectionRL.getHeight() - routeSelectionRL.getPaddingBottom()*2 - routeSelectionRL.getPaddingTop()*2;//TODO: this is a hack

        ObjectAnimator yAnimator = ObjectAnimator.ofFloat(interactionLL,"y", newY);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(yAnimator);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setStartDelay(300);
        animatorSet.start();
    }

    private int getWindowHeight(){
        Log.i(TAG, "Window height = " + String.valueOf(this.getResources().getDisplayMetrics().heightPixels));
        return this.getResources().getDisplayMetrics().heightPixels;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if(mode == MODE_NAVIGATE){
            collapseInteractionLayout();
            navigateFAB.show(true);
        }
        else if(mode == MODE_DROP_PIN){
            String location = dropPin(latLng, pin);
            if(pin == PIN_FROM){
                fromACTV.setText(location);
            }
            else if(pin == PIN_TO){
                toACTV.setText(location);
            }
        }
    }

    private String dropPin(LatLng latLng, long pin){
        String locationName = getLocationName(latLng);
        float hue = 0f;
        if(pin == PIN_FROM) {
            hue = BitmapDescriptorFactory.HUE_RED;
            if(fromMarker != null) fromMarker.remove();
            fromMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getResources().getString(R.string.from) + " " + locationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        }
        else if(pin == PIN_TO) {
            hue = BitmapDescriptorFactory.HUE_GREEN;
            if(toMarker != null)toMarker.remove();
            toMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getResources().getString(R.string.to) + " " + locationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        }

        return locationName;
    }

    @Override
    public void onFocusChange(View view, boolean isFocused) {
        if(view == fromACTV){
            if(isFocused){
                if(mode == MODE_DROP_PIN) expandInteractionLayout();
                fromLocationB.setVisibility(Button.VISIBLE);
                fromDropPinB.setVisibility(Button.VISIBLE);
            }
            else{
                fromLocationB.setVisibility(Button.GONE);
                fromDropPinB.setVisibility(Button.GONE);
            }
        }
        else if(view == toACTV){
            if(isFocused){
                if(mode == MODE_DROP_PIN) expandInteractionLayout();
                toLocationB.setVisibility(Button.VISIBLE);
                toDropPinB.setVisibility(Button.VISIBLE);
            }
            else {
                toLocationB.setVisibility(Button.GONE);
                toDropPinB.setVisibility(Button.GONE);
            }
        }
    }

    private String getLocationName(LatLng location){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
            if(addresses.size() > 0){
                return addresses.get(0).getFeatureName();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class SearchSuggestionTasker extends AsyncTask<String, Integer, List<String>>{

        @Override
        protected List<String> doInBackground(String... strings) {
            List<String> names = new ArrayList<String>();
            String typed = strings[0];
            try {
                List<Address> addresses = new Geocoder(Map.this).getFromLocationName(typed, 10, -1.501543, 36.196317, -0.620050, 37.451505);//kajiado to kirinyaga

                for(int index = 0; index < addresses.size(); index++){
                    if(addresses.get(index).getFeatureName() != null){
                        names.add(addresses.get(index).getFeatureName());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return names;
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);
        }
    }
}
