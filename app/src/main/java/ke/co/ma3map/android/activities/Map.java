package ke.co.ma3map.android.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
//import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/*from the new PlayServices API*/
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.melnykov.fab.FloatingActionButton;

import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ke.co.ma3map.android.R;
import ke.co.ma3map.android.carriers.Commute;
import ke.co.ma3map.android.carriers.LatLngPair;
import ke.co.ma3map.android.carriers.Route;
import ke.co.ma3map.android.carriers.Stop;
import ke.co.ma3map.android.handlers.BestPath;
import ke.co.ma3map.android.handlers.Data;
import ke.co.ma3map.android.helpers.CommuteRecyclerAdapter;
import ke.co.ma3map.android.listeners.ProgressListener;
import ke.co.ma3map.android.services.GetRouteData;
import ke.co.ma3map.android.services.Navigate;

public class Map extends Activity
                 implements GooglePlayServicesClient.ConnectionCallbacks,
                            GoogleApiClient.OnConnectionFailedListener,
                            GoogleApiClient.ConnectionCallbacks,
                            LocationListener,
                            View.OnClickListener,
                            GoogleMap.OnMapClickListener,
                            View.OnFocusChangeListener,
                            Serializable{

    private final String TAG = "ma3map.Map";

    private final int DEFAULT_ZOOM = 10;
    private final long I_LAYOUT_TRANS_TIME = 600;//the time in milliseconds it will take to hide/show the interaction layout
    private final double I_LAYOUT_HIGHT_RATIO = 1;//the ratio of height of the interactionLL to the activityHeight
    private final int MODE_MAP = 0;
    private final int MODE_DROP_PIN = 1;
    private final int MODE_NAVIGATE = 2;
    private final int PIN_FROM = 0;
    private final int PIN_TO = 1;


    private int mode;
    private int pin;
    private GoogleMap googleMap;
    //private LocationClient locationClient;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location lastLocation;

    private LinearLayout interactionLL;
    private RelativeLayout routeSelectionRL;
    private RecyclerView commutesRV;
    private AutoCompleteTextView fromACTV;
    private AutoCompleteTextView toACTV;
    private FloatingActionButton navigateFAB;
    private FloatingActionButton searchFAB;
    private EditText phantomET;
    private Button fromDropPinB;
    //private Button fromLocationB;
    private Button toDropPinB;
    //private Button toLocationB;
    private Marker fromMarker;
    private Marker toMarker;
    //private Button searchB;
    private RoutePoint fromPoint;
    private RoutePoint toPoint;
    private SearchRoutesTasker searchRoutesTasker;
    private PlacesSearchSuggestionTasker fromPlacesSuggestionTasker;
    private PlacesSearchSuggestionTasker toPlacesSearchSuggestionTasker;

    private boolean listenForFromText;//flag determining whether text changes should be listened at that moment
    private boolean listenForToText;//flag determining whether text changes should be listened at that moment

    private ArrayList<Polyline> mapPolylines;
    private ArrayList<Route> routes;
    /*
    This broadcast receiver receivers broadcasts from the GetRouteData service.
    Broadcast consists of route data from the service
     */
    private BroadcastReceiver routeDataBroadcastReceiver;

    /*
    This broadcast receiver receives broadcasts from the Navigation service.
    Broadcasts contain navigation statuses and can either be STATUS_ERROR, STATUS_STARTING, STATUS_UPDATED or STATUS_FINISHED.
     */
    private BroadcastReceiver navigationStatusBroadcastReceiver;

    /*
     This broadcast receiver receives commute segments containing GIS polylines for the current route being handled by the
     Navigation service
     */
    private BroadcastReceiver commuteSegmentBroadcastReceiver;

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

        listenForFromText = true;
        listenForToText = true;

        //init views
        interactionLL = (LinearLayout)this.findViewById(R.id.interaction_ll);
        interactionLL.setOnClickListener(this);
        double iLLHeight = getWindowHeight()*I_LAYOUT_HIGHT_RATIO;
        ViewGroup.LayoutParams iLLLayoutParams = interactionLL.getLayoutParams();
        iLLLayoutParams.height = (int)iLLHeight;
        interactionLL.setLayoutParams(iLLLayoutParams);

        routeSelectionRL = (RelativeLayout)this.findViewById(R.id.route_selection_rl);

        commutesRV = (RecyclerView)this.findViewById(R.id.commutes_rv);
        LinearLayoutManager commutesRVLLM = new LinearLayoutManager(Map.this);
        commutesRVLLM.setOrientation(LinearLayoutManager.VERTICAL);
        commutesRV.setLayoutManager(commutesRVLLM);

        fromACTV = (AutoCompleteTextView)this.findViewById(R.id.from_actv);
        fromACTV.setOnClickListener(this);
        fromACTV.setOnFocusChangeListener(this);
        fromPlacesSuggestionTasker = new PlacesSearchSuggestionTasker(fromACTV);
        fromACTV.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if(listenForFromText && fromACTV.getText().toString().length() >= 2){
                    fromPoint = new RoutePoint();
                    fromPoint.setSelectionMode(RoutePoint.MODE_TYPED);
                    fromPoint.setName(fromACTV.getText().toString());

                    //if(fromPlacesSuggestionTasker.isRunning() == false){
                    fromPlacesSuggestionTasker = new PlacesSearchSuggestionTasker(fromACTV);//reinitialize the asynctaks. You can only run an asynctask once
                    fromPlacesSuggestionTasker.execute(fromPoint);
                    //}
                }
            }
        });
        //fromACTV.setOnClickListener(this);
        fromDropPinB = (Button)this.findViewById(R.id.from_drop_pin_b);
        fromDropPinB.setOnClickListener(this);
        /*fromLocationB = (Button)this.findViewById(R.id.from_location_b);
        fromLocationB.setOnClickListener(this);*/

        toACTV = (AutoCompleteTextView)this.findViewById(R.id.to_actv);
        toACTV.setOnClickListener(this);
        toACTV.setOnFocusChangeListener(this);
        toPlacesSearchSuggestionTasker = new PlacesSearchSuggestionTasker(toACTV);
        toACTV.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if(listenForToText && toACTV.getText().toString().length() >= 2){
                    toPoint = new RoutePoint();
                    toPoint.setSelectionMode(RoutePoint.MODE_TYPED);
                    toPoint.setName(toACTV.getText().toString());

                    //if(toPlacesSearchSuggestionTasker.isRunning() == false) {
                    toPlacesSearchSuggestionTasker = new PlacesSearchSuggestionTasker(toACTV);
                    toPlacesSearchSuggestionTasker.execute(toPoint);
                    //}
                }
            }
        });
        //toACTV.setOnClickListener(this);
        toDropPinB = (Button)this.findViewById(R.id.to_drop_pin_b);
        toDropPinB.setOnClickListener(this);
        /*toLocationB = (Button)this.findViewById(R.id.to_location_b);
        toLocationB.setOnClickListener(this);*/

        /*searchB = (Button)findViewById(R.id.search_b);
        searchB.setOnClickListener(this);*/

        navigateFAB = (FloatingActionButton)this.findViewById(R.id.navigate_fab);
        navigateFAB.setShadow(true);
        navigateFAB.setColorNormalResId(R.color.secondary);
        navigateFAB.setColorPressedResId(R.color.secondary_light);
        navigateFAB.setImageResource(R.drawable.ic_navigate);
        navigateFAB.setOnClickListener(this);

        searchFAB = (FloatingActionButton)this.findViewById(R.id.search_fab);
        searchFAB.setShadow(true);
        searchFAB.setColorNormalResId(R.color.secondary);
        searchFAB.setColorPressedResId(R.color.secondary_light);
        searchFAB.setOnClickListener(this);

        /**
         * googleMap will be initialized as null if:
         *  - version of Play Services on devices is outdated
         *  - user is not signed in to the PlayStore
         */
        googleMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        if(googleMap != null){
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setZoomControlsEnabled(false);
            googleMap.setOnMapClickListener(this);

            //locationClient = new LocationClient(this, this, this);
            lastLocation = null;
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            interactionLL.setY(getWindowHeight());

            phantomET = new EditText(this);
            phantomET.setVisibility(EditText.INVISIBLE);
            interactionLL.addView(phantomET);

            //ask user for permission to get route data
            DataStatusTasker dataStatusTasker = new DataStatusTasker();
            dataStatusTasker.execute(0);

        /*PlacesSearchSuggestionTasker fromPlacesSuggestions = new PlacesSearchSuggestionTasker(fromACTV);
        fromPlacesSuggestions.execute(0);
        PlacesSearchSuggestionTasker toPlacesSuggestions = new PlacesSearchSuggestionTasker(toACTV);
        toPlacesSuggestions.execute(0);*/

            routes = null;
            mapPolylines = new ArrayList<Polyline>();

            //initialize the relevant broadcast receivers
            routeDataBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Toast.makeText(Map.this, "Route data gotten", Toast.LENGTH_LONG).show();
                    routes = intent.getParcelableArrayListExtra(Route.PARCELABLE_KEY);
                    Log.d(TAG, "************************* ALL ROUTES ****************************");
                    for(int i = 0; i < routes.size(); i++){
                        Log.d(TAG, routes.get(i).getShortName());
                    }
                    Log.d(TAG, "************************* END ROUTES ****************************");
                }
            };

            navigationStatusBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Bundle bundle = intent.getExtras();
                    if(bundle != null){
                        int status = bundle.getInt(Navigate.STATUS_PARCELABLE_KEY);
                        if(status == Navigate.STATUS_ERROR){//service has encountered an error an cannot continue

                        }
                        else if(status == Navigate.STATUS_STARTING){//service has initialised and is waiting for the first GPS update

                        }
                        else if(status == Navigate.STATUS_UPDATED){//service has just received a GPS update that it's about to work on

                        }
                        else if(status == Navigate.STATUS_FINISHED){//service has completed its tasks and is about to be destroyed

                        }
                    }
                }
            };

            commuteSegmentBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Bundle bundle = intent.getExtras();
                    if(bundle != null){
                        setDropPinMode();
                        ArrayList<Commute.Segment> commuteSegments = bundle.getParcelableArrayList(Commute.Segment.PARCELABLE_KEY);
                        redrawMapPolylines(commuteSegments);
                    }
                }
            };
        }
        else {
            Toast.makeText(Map.this, "Unable to initialize correctly. Make sure you are running the most recent version of Google Play Services", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Make sure you initialize all hardware resources here because they will be
     * released in onPause
     */
    @Override
    protected void onResume() {
        super.onResume();

        //do nothing of Google Maps API not initialized
        if(googleApiClient != null){
            googleApiClient.connect();
            /*if(!locationClient.isConnected()){
                locationClient.connect();
            }*/

            //register broadcast receivers
            //broadcast receiver connecting this activity to the GetRouteData service
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.registerReceiver(routeDataBroadcastReceiver, new IntentFilter(GetRouteData.ACTION_GET_ROUTE_DATA));
            localBroadcastManager.registerReceiver(navigationStatusBroadcastReceiver, new IntentFilter(Navigate.ACTION_GET_NAVIGATION_STATUS));
            localBroadcastManager.registerReceiver(commuteSegmentBroadcastReceiver, new IntentFilter(Navigate.ACTION_GET_COMMUTE_SEGMENTS));
        }

    }

    /**
     * Release all hardware resources here and not onDestroy to avoid blocking the resources
     * when not using them
     */
    @Override
    protected void onPause(){
        super.onPause();

        //if Google Maps API was not initialized, nothing was done in onResume. Do nothing here also
        if(googleApiClient != null){
            //locationClient.disconnect();
            googleApiClient.disconnect();

            //unregister broadcast receivers
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.unregisterReceiver(routeDataBroadcastReceiver);
            localBroadcastManager.unregisterReceiver(navigationStatusBroadcastReceiver);
            localBroadcastManager.unregisterReceiver(commuteSegmentBroadcastReceiver);
        }
    }

    /**
     * This method zooms in on the devices location.
     * Should be run after resources initialized i.e. in onResume()
     */
    private void zoomInOnLocation(){
        //Location myLocation = locationClient.getLastLocation();

        if(lastLocation != null){
            LatLng myLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, DEFAULT_ZOOM));
            Log.d(TAG, "Zoomed in on user's position");
        }
        else {
            Log.w(TAG, "Unable to get user's location");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); //Update location every 10 seconds

        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);

        zoomInOnLocation();
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onLocationChanged(Location location) {
        boolean zoomIn = false;
        if(this.lastLocation== null){
            zoomIn = true;
        }
        this.lastLocation = location;

        if(zoomIn){
            zoomInOnLocation();
        }
    }

    /**
     * This method redraws the polylines on the map. Polylines will have different colors depending
     * on the type of commute segment they come from.
     *
     * @param commuteSegments   The Commute Segments holding the polylines.
     */
    private void redrawMapPolylines(ArrayList<Commute.Segment> commuteSegments){
        if(googleMap != null) {
            //first remove all existing polylines
            for (int index = 0; index < mapPolylines.size(); index++) {
                mapPolylines.get(index).remove();
            }

            //now add the new polylines to the map
            for (int index = 0; index < commuteSegments.size(); index++) {
                Commute.Segment currSegment = commuteSegments.get(index);

                int color = Color.BLUE;//default color

                if(currSegment.getType() == Commute.Segment.TYPE_WALKING){
                    color = Color.GRAY;
                }

                Polyline currLine = googleMap.addPolyline(new PolylineOptions()
                        .addAll(currSegment.getPolyline())
                        .width(10)
                        .color(color)
                );

                mapPolylines.add(currLine);
            }
        }
    }

    /**
     * This method first asks for permission from the user before spawning a service
     * that gets the route data
     */
    private void getRouteData(){
        if(!GetRouteData.isServiceRunning(Map.this)) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Matatu Route Data");
            dialogBuilder.setMessage(R.string.warning_download_route_data);
            dialogBuilder.setCancelable(false);
            dialogBuilder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    Intent intent = new Intent(Map.this, GetRouteData.class);
                    Map.this.startService(intent);
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
        else {
            Toast.makeText(Map.this, getResources().getString(R.string.app_fetching_route_data), Toast.LENGTH_LONG).show();
        }
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
        /*else if(view == fromLocationB){
            Location myLocation = locationClient.getLastLocation();
            if(myLocation != null){
                String name = getLocationName(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()));
                fromPoint = new RoutePoint(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), name, RoutePoint.MODE_CURR_LOC);
                fromACTV.setText(fromPoint.getName());
            }
        }*/
        else if(view == toDropPinB){
            pin = PIN_TO;
            setDropPinMode();
        }
        /*else if(view == toLocationB){
            Location myLocation = locationClient.getLastLocation();
            if(myLocation != null){
                String name = getLocationName(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()));
                toPoint = new RoutePoint(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), name, RoutePoint.MODE_CURR_LOC);
                toACTV.setText(toPoint.getName());
            }
        }*/
        else if(view == interactionLL || view == fromACTV || view == toACTV){
            if(mode == MODE_DROP_PIN) expandInteractionLayout();
        }
        else if(view == searchFAB){
            Log.d(TAG, "Search FAB clicked");
            if(searchRoutesTasker != null){
                searchRoutesTasker.cancel(true);
                searchRoutesTasker = null;
            }
            searchRoutesTasker = new SearchRoutesTasker(fromPoint, toPoint);
            searchRoutesTasker.execute(0);
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
            String location = dropPin(latLng, pin, null);
            if(pin == PIN_FROM){
                listenForFromText = false;
                fromACTV.setText(location);
                listenForFromText = true;
            }
            else if(pin == PIN_TO){
                listenForToText = false;
                toACTV.setText(location);
                listenForToText = true;
            }

            //log the RoutePoints
            if(fromPoint != null) {
                Log.d(TAG, "fromPoint selectionMode = " + fromPoint.getSelectionMode());
                if (fromPoint.getLatLng() != null)
                    Log.d(TAG, "fromPoint latLng = " + fromPoint.getLatLng());
                else Log.d(TAG, "fromPoint latLng = null");
                if (fromPoint.getPlaceID() != null)
                    Log.d(TAG, "fromPoint placeID = " + fromPoint.getPlaceID());
                else Log.d(TAG, "fromPoint placeID = null");
            }
            if(toPoint != null) {
                Log.d(TAG, "toPoint selectionMode = " + toPoint.getSelectionMode());
                if (toPoint.getLatLng() != null)
                    Log.d(TAG, "toPoint latLng = " + toPoint.getLatLng());
                else Log.d(TAG, "toPoint latLng = null");
                if (toPoint.getPlaceID() != null)
                    Log.d(TAG, "toPoint placeID = " + toPoint.getPlaceID());
                else Log.d(TAG, "toPoint placeID = null");
            }

        }
    }

    private String dropPin(LatLng latLng, long pin, String name){
        String locationName = null;
        if(name == null) locationName = getLocationName(latLng);

        if(pin == PIN_FROM) {
            if(name == null) {
                fromPoint = new RoutePoint(latLng, locationName, RoutePoint.MODE_DROP_PIN);
            }

            dropFromPin(latLng);
        }
        else if(pin == PIN_TO) {
            if(name == null) {
                toPoint = new RoutePoint(latLng, locationName, RoutePoint.MODE_DROP_PIN);
            }

            dropToPin(latLng);
        }

        return locationName;
    }

    private void dropFromPin(LatLng latLng){
        float hue = BitmapDescriptorFactory.HUE_RED;
        if(fromMarker != null) fromMarker.remove();
        fromMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(getResources().getString(R.string.from) + " " + fromPoint.getName())
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
    }

    private void dropToPin(LatLng latLng){
        float hue = BitmapDescriptorFactory.HUE_GREEN;
        if(toMarker != null)toMarker.remove();
        toMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(getResources().getString(R.string.to) + " " + toPoint)
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
    }

    @Override
    public void onFocusChange(View view, boolean isFocused) {
        if(view == fromACTV){
            if(isFocused){
                if(mode == MODE_DROP_PIN) expandInteractionLayout();
                //fromLocationB.setVisibility(Button.VISIBLE);
                fromDropPinB.setVisibility(Button.VISIBLE);
            }
            else{
                //fromLocationB.setVisibility(Button.GONE);
                fromDropPinB.setVisibility(Button.GONE);
            }
        }
        else if(view == toACTV){
            if(isFocused){
                if(mode == MODE_DROP_PIN) expandInteractionLayout();
                //toLocationB.setVisibility(Button.VISIBLE);
                toDropPinB.setVisibility(Button.VISIBLE);
            }
            else {
                //toLocationB.setVisibility(Button.GONE);
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

    private class PlacesSearchSuggestionTasker extends AsyncTask<RoutePoint, Integer, List<String[]>>{

        private RoutePoint routePoint;
        private AutoCompleteTextView actv;
        private boolean isRunning;

        public PlacesSearchSuggestionTasker(AutoCompleteTextView actv){
            this.routePoint = null;
            this.actv = actv;
            isRunning = false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isRunning = true;
        }

        @Override
        protected List<String[]> doInBackground(RoutePoint... routePoints) {
            routePoint = routePoints[0];

            List<String> names = new ArrayList<String>();
            String typed = routePoint.getName();

            List<String[]> suggestions = Data.getPlaceSuggestions(Map.this, typed);

            //set the first suggestion as the current location
            if(suggestions != null && suggestions.size() > 0){
                Log.d(TAG, "most relevant placeID is "+suggestions.get(0)[0]);
                routePoint.setPlaceID(suggestions.get(0)[0]);//assuming here the first item in the array is the most relevant
                routePoint.setLatLng(null);

                if(fromPoint != null){
                    Log.d(TAG, "fromPoint selectionMode = "+fromPoint.getSelectionMode());
                    if(fromPoint.getLatLng() != null) Log.d(TAG, "fromPoint latLng = "+fromPoint.getLatLng());
                    else Log.d(TAG, "fromPoint latLng = null");
                    if(fromPoint.getPlaceID() != null) Log.d(TAG, "fromPoint placeID = "+fromPoint.getPlaceID());
                    else Log.d(TAG, "fromPoint placeID = null");
                }
                if(toPoint != null){
                    Log.d(TAG, "toPoint selectionMode = "+toPoint.getSelectionMode());
                    if(toPoint.getLatLng() != null) Log.d(TAG, "toPoint latLng = "+toPoint.getLatLng());
                    else Log.d(TAG, "toPoint latLng = null");
                    if(toPoint.getPlaceID() != null) Log.d(TAG, "toPoint placeID = "+toPoint.getPlaceID());
                    else Log.d(TAG, "toPoint placeID = null");
                }
            }

            return suggestions;
        }

        public boolean isRunning(){
            return isRunning;
        }

        @Override
        protected void onPostExecute(final List<String[]> places) {
            super.onPostExecute(places);
            if(places != null){
                List<String> suggestionNames = new ArrayList<String>();
                for(int i = 0; i < places.size(); i++){
                    suggestionNames.add(places.get(i)[1]);
                }
                isRunning = false;
                ArrayAdapter<String> placesAA = new ArrayAdapter<String>(Map.this, android.R.layout.simple_list_item_1, suggestionNames);
                actv.setAdapter(placesAA);

                actv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        //assuming that the routePoint is a pointer to the main route point (either fromPoint or toPoint)
                        routePoint.setPlaceID(places.get(position)[0]);
                        routePoint.setLatLng(null);
                        if(fromPoint != null){
                            Log.d(TAG, "fromPoint selectionMode = "+fromPoint.getSelectionMode());
                            if(fromPoint.getLatLng() != null) Log.d(TAG, "fromPoint latLng = "+fromPoint.getLatLng());
                            else Log.d(TAG, "fromPoint latLng = null");
                            if(fromPoint.getPlaceID() != null) Log.d(TAG, "fromPoint placeID = "+fromPoint.getPlaceID());
                            else Log.d(TAG, "fromPoint placeID = null");
                        }
                        if(toPoint != null){
                            Log.d(TAG, "toPoint selectionMode = "+toPoint.getSelectionMode());
                            if(toPoint.getLatLng() != null) Log.d(TAG, "toPoint latLng = "+toPoint.getLatLng());
                            else Log.d(TAG, "toPoint latLng = null");
                            if(toPoint.getPlaceID() != null) Log.d(TAG, "toPoint placeID = "+toPoint.getPlaceID());
                            else Log.d(TAG, "toPoint placeID = null");
                        }
                    }
                });
            }
            else {
                Log.e(TAG, "Could not get place suggestions from Google Places API");
            }
        }
    }

    public class SearchRoutesTasker extends AsyncTask<Integer, Integer, Boolean>
                                    implements ProgressListener{

        private ProgressDialog progressDialog;
        private RoutePoint from;
        private RoutePoint to;
        private String message;
        private ArrayList<LatLngPair> latLngPairs;
        private int distanceIndex;
        private ArrayList<Commute> commutes;

        public SearchRoutesTasker(RoutePoint from, RoutePoint to){
            this.from = new RoutePoint(from.getLatLng(), from.getName(), from.getSelectionMode(), from.getPlaceID());
            this.to = new RoutePoint(to.getLatLng(), to.getName(), to.getSelectionMode(), to.getPlaceID());

            message = null;
            latLngPairs = null;
            distanceIndex = -1;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(Map.this);
            progressDialog.setMessage("Calculating commute paths");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgressNumberFormat("");
            progressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Integer... integers) {
            if(to != null && from != null){
                if(to.getName().length() > 0 && from.getName().length() > 0){
                    //1. check if LatLng for to and from set and set if not
                    if(to.getSelectionMode() == RoutePoint.MODE_TYPED && to.getLatLng() == null && to.getPlaceID()!=null){
                        to.setLatLng(Data.getPlaceLatLng(Map.this, to.getPlaceID()));
                    }
                    else if(to.getSelectionMode() == RoutePoint.MODE_TYPED){
                        Log.e(TAG, "No locationID for to point found");
                        return false;
                    }

                    if(from.getSelectionMode() == RoutePoint.MODE_TYPED && from.getLatLng() == null && from.getPlaceID()!=null){
                        from.setLatLng(Data.getPlaceLatLng(Map.this, from.getPlaceID()));
                    }
                    else if(from.getSelectionMode() == RoutePoint.MODE_TYPED) {
                        Log.e(TAG, "No locationID for from point found");
                        return false;
                    }

                    //2. get cached route data
                    if(routes != null){
                        //3. get closest stops
                        Log.d(TAG, "Getting all stops");
                        List<Stop> fromStops = new ArrayList<Stop>();
                        List<Stop> toStops = new ArrayList<Stop>();

                        for(int routeIndex = 0; routeIndex < routes.size(); routeIndex++){
                            List<Stop> routeStops = routes.get(routeIndex).getStops(0);
                            //Log.d(TAG, "Route has "+routeStops.size()+" stops");

                            for(int rStopIndex = 0 ; rStopIndex < routeStops.size(); rStopIndex++){
                                boolean isThere = false;
                                //Log.d(TAG, "Comparing current stop in route with "+fromStops.size()+" other stops");
                                for(int aStopIndex = 0; aStopIndex < fromStops.size(); aStopIndex++){
                                    if(routeStops.get(rStopIndex).getLat().equals(fromStops.get(aStopIndex).getLat())
                                            && routeStops.get(rStopIndex).getLon().equals(fromStops.get(aStopIndex).getLon())){
                                        isThere = true;
                                        break;
                                    }
                                }

                                if(isThere == false){
                                    fromStops.add(routeStops.get(rStopIndex));
                                    toStops.add(routeStops.get(rStopIndex));
                                }
                            }
                        }

                        Log.d(TAG, "Number of stops = " + fromStops.size());

                        Collections.sort(fromStops, new Stop.DistanceComparator(from.getLatLng()));//stop closest to from becomes first
                        Collections.sort(toStops, new Stop.DistanceComparator(to.getLatLng()));//stop closest to destination becomes first

                        //4. determine commutes using closest stops
                        BestPath bestPath = new BestPath(Map.this, fromStops.subList(0, BestPath.MAX_FROM_POINTS), from.getLatLng(), toStops.subList(0, BestPath.MAX_TO_POINTS), to.getLatLng(), routes, Commute.PARCELABLE_KEY);
                        bestPath.addProgressListener(SearchRoutesTasker.this);
                        bestPath.calculateCommutes();
                        return true;
                    }
                    else {
                        message = "Route data not available at the moment";
                    }
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(result == null || result.booleanValue() == false){
                Toast.makeText(Map.this, "Something went wrong. Try searching again", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        private void finish(){
            progressDialog.dismiss();
            from = null;
            to = null;
        }

        private void setCommuteTimes(){
            /*for(int index = 0; index < latLngPairs.size(); index++){
                Log.d(TAG, "---------------------------------------");
                Log.d(TAG, " Point A "+latLngPairs.get(index).getPointA());
                Log.d(TAG, " Point B "+latLngPairs.get(index).getPointB());
                Log.d(TAG, " Distance "+latLngPairs.get(index).getDistance());
                Log.d(TAG, "---------------------------------------");
            }*/
            for(int cIndex = 0; cIndex < commutes.size(); cIndex++){
                commutes.get(cIndex).setCommuteTime(latLngPairs);
            }
        }

        /**
         * This method adds commutes in the Commute Recycler View
         */
        public void showCommutes(){
            CommuteRecyclerAdapter recyclerAdapter = new CommuteRecyclerAdapter(Map.this, commutes);
            commutesRV.setAdapter(recyclerAdapter);
        }

        @Override
        public void onProgress(int progress, int end, String message, int flag) {
            progressDialog.setProgress(progress);
            progressDialog.setMax(end);
            progressDialog.setMessage(message);
        }

        @Override
        public void onDone(Bundle output, String message, int flag) {
            //update the from and to points on the map
            dropFromPin(from.getLatLng());
            dropToPin(to.getLatLng());

            commutes = output.getParcelableArrayList(Commute.PARCELABLE_KEY);
            progressDialog.setProgress(1);
            progressDialog.setMax(1);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Sorting calculated commute paths");

            Collections.sort(commutes, new Commute.ScoreComparator());

            if(commutes != null && flag == ProgressListener.FLAG_DONE){

                Log.d(TAG, "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
                for(int commuteIndex = 0; commuteIndex < commutes.size(); commuteIndex++){
                    Log.d(TAG, "Commute with index as "+commuteIndex+" has score of "+commutes.get(commuteIndex).getScore());
                    for(int stepIndex = 0; stepIndex < commutes.get(commuteIndex).getSteps().size(); stepIndex++){
                        Commute currCommute = commutes.get(commuteIndex);
                        if(currCommute.getSteps().get(stepIndex).getStepType() == Commute.Step.TYPE_WALKING){
                            Log.d(TAG, "  step "+stepIndex+" is walking from "+currCommute.getSteps().get(stepIndex).getStart().getName()+" to "+currCommute.getSteps().get(stepIndex).getDestination().getName());
                        }
                        else if(currCommute.getSteps().get(stepIndex).getStepType() == Commute.Step.TYPE_MATATU){
                            Log.d(TAG, "  step "+stepIndex+" is using route '"+currCommute.getSteps().get(stepIndex).getRoute().getLongName()+"("+currCommute.getSteps().get(stepIndex).getRoute().getShortName()+")'");
                            if(currCommute.getSteps().get(stepIndex).getStart() != null)
                                Log.d(TAG, "    from "+currCommute.getSteps().get(stepIndex).getStart().getName()+" "+currCommute.getSteps().get(stepIndex).getStart().getLat()+","+currCommute.getSteps().get(stepIndex).getStart().getLon());
                            if(currCommute.getSteps().get(stepIndex).getDestination() != null)
                                Log.d(TAG, "    to "+currCommute.getSteps().get(stepIndex).getDestination().getName()+" "+currCommute.getSteps().get(stepIndex).getDestination().getLat()+","+currCommute.getSteps().get(stepIndex).getDestination().getLon());
                        }
                    }

                    latLngPairs = commutes.get(commuteIndex).getStepLatLngPairs(latLngPairs);
                    Log.d(TAG, "------------------------------------------------------");
                }

                Log.d(TAG, "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");

                //use Google Distance Matrix API to calculate distances between the latLngPairs
                if(latLngPairs != null){
                    distanceIndex = 0;

                    Log.d(TAG, "About to spawn "+latLngPairs.size()+" HTTP AsyncTasks to get driving distances from Google Distance Matrix API");
                    Data dataHandler = new Data(Map.this);
                    for(int index = 0; index < latLngPairs.size(); index++){
                        DrivingDistanceTasker dDTasker = new DrivingDistanceTasker(dataHandler, latLngPairs.get(index));
                        //dDTasker.execute(0);
                        dDTasker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);//execute the thread in parallel
                    }
                }

                if(message != null){
                    Toast.makeText(Map.this, message, Toast.LENGTH_LONG).show();
                }
            }
        }

        /**
         * This AsyncTask gets the driving distance between two LatLngs using Google's Distance Matrix API
         */
        private class DrivingDistanceTasker extends AsyncTask<Integer, Integer, Double>{
            private final LatLngPair latLngPair;
            private final Data dataHandler;

            public DrivingDistanceTasker(Data dataHandler, LatLngPair latLngPair){
                this.latLngPair = latLngPair;
                this.dataHandler = dataHandler;
            }

            @Override
            protected Double doInBackground(Integer... params) {
                return dataHandler.getDrivingDistance(latLngPair.getPointA(), latLngPair.getPointB());
            }

            @Override
            protected void onPostExecute(Double distance) {
                super.onPostExecute(distance);

                if(distance != null) {
                    for (int index = 0; index < latLngPairs.size(); index++) {
                        if (latLngPair.equals(latLngPairs.get(index))) {
                            latLngPairs.set(index, new LatLngPair(latLngPair.getPointA(), latLngPair.getPointB(), distance.doubleValue()));
                            break;
                        }
                    }
                }

                distanceIndex++;
                if(distanceIndex == latLngPairs.size()){
                    setCommuteTimes();
                    showCommutes();
                    finish();
                }
            }
        }
    }

    private class DataStatusTasker extends AsyncTask<Integer, Integer, Boolean>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Integer... integers) {
            Data data = new Data(Map.this);
            return new Boolean(data.isRouteDataPresent());
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if(result == true){
                FetchDataTasker fetchDataTasker = new FetchDataTasker();
                fetchDataTasker.execute(0);
            }
            else {
                getRouteData();//ask user for permission to get data from the server
            }
        }
    }

    /**
     * This class spawns an asynctask to fetch route data cached in the local SQLite Database
     */
    private class FetchDataTasker extends AsyncTask<Integer, Double, ArrayList<Route>>
                                    implements ProgressListener{

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(Map.this);
            progressDialog.setMessage("Getting cached route data");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgressNumberFormat("");
            progressDialog.show();
        }

        @Override
        protected ArrayList<Route> doInBackground(Integer... integers) {
            Data data = new Data(Map.this);
            data.addProgressListener(FetchDataTasker.this);
            return data.getCachedRouteData(false, Route.PARCELABLE_KEY, true);
        }

        @Override
        protected void onPostExecute(ArrayList<Route> routes) {
            super.onPostExecute(routes);
            progressDialog.dismiss();
            Toast.makeText(Map.this, "Done getting route data", Toast.LENGTH_LONG).show();

            Map.this.routes = routes;
        }

        @Override
        public void onProgress(int progress, int end, String message, int flag) {
            Log.d(TAG, "getCachedRouteData progress = "+progress);
            progressDialog.setProgress(progress);
        }

        @Override
        public void onDone(Bundle output, String message, int flag) {
        }
    }

    /**
     * This data carrier class is meant to store data on the end points for a transit i.e origin and destination
     */
    private class RoutePoint {

        public static final int MODE_CURR_LOC = 0;
        public static final int MODE_DROP_PIN = 1;
        public static final int MODE_TYPED = 2;

        private LatLng latLng;
        private String name;
        private int selectionMode;
        private String placeID;

        public RoutePoint(){
            latLng = null;
            name = "";
            selectionMode = -1;
            placeID = null;
        }

        public RoutePoint(LatLng latLng, String name, int selectionMode){
            this.latLng = latLng;
            this.name = name;
            this.selectionMode = selectionMode;
        }

        public RoutePoint(LatLng latLng, String name, int selectionMode, String placeID){
            this(latLng, name, selectionMode);
            this.placeID = placeID;
        }

        public LatLng getLatLng() {
            return latLng;
        }

        public void setLatLng(LatLng latLng) {
            this.latLng = latLng;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            Log.d(TAG, "Name changed to "+name);
            this.name = name;
        }

        public int getSelectionMode() {
            return selectionMode;
        }

        public void setSelectionMode(int selectionMode) {
            Log.d(TAG, "Selection mode changed to "+selectionMode);
            this.selectionMode = selectionMode;
        }

        public String getPlaceID(){
            return placeID;
        }

        public void setPlaceID(String placeID){
            Log.d(TAG, "PlaceID changed to "+placeID);
            this.placeID = placeID;
        }

    }
}
