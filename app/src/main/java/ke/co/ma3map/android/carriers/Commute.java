package ke.co.ma3map.android.carriers;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by jason on 28/10/14.
 */
public class Commute implements Parcelable {

    public static final String PARCELABLE_KEY = "Commute";
    private static final int PARCELABLE_DESC = 2112;
    private static final String TAG = "ma3map.Commute";

    private final double SCORE_STEP = 10;//score given for each step in commute
    private final double SCORE_WALKING = 0.1;//score given for each meter walked
    private final double SCORE_STOP = 2;//score given for each stop in commute
    private final double SPEED_WALKING = 2.77778;//average walking speed in m/s
    private final double SPEED_MATATU = 5.55556;//average value in m/s that can be used to estimate how long it would take a matatu to cover some distance

    private LatLng from;//actual point on map use wants to go from
    private LatLng to;//actual point on map user want to go to
    private List<Step> steps;
    private double time;

    public Commute(LatLng from, LatLng to){
        this.from = from;
        this.to = to;
        this.steps = new ArrayList<Step>();
        time = -1;
    }

    /*public Commute(){
        this.from = null;
        this.to = null;
        this.steps = new ArrayList<Step>();
    }*/

    public Commute(Parcel source){
        this(null, null);
        readFromParcel(source);
    }

    public ArrayList<Route> getMatatuRoutes(){
        ArrayList <Route> matatuRoutes = new ArrayList<Route>();

        for(int index = 0; index < steps.size(); index++){
            if(steps.get(index).getStepType() == Step.TYPE_MATATU){
                matatuRoutes.add(steps.get(index).getRoute());
            }
        }

        return matatuRoutes;
    }

    public Step getStep(int index){
        return steps.get(index);
    }

    public List<Step> getSteps(){
        return steps;
    }

    public void setSteps(List<Step> steps){
        this.steps = new ArrayList<Step>();
        for(int index = 0; index < steps.size(); index++){
            this.steps.add(new Step(steps.get(index).getStepType(), steps.get(index).getRoute(), steps.get(index).getStart(), steps.get(index).getDestination()));
        }
    }

    public double getTime(){
        return time;
    }

    public void setStep(int index, Step step){
        if(index < steps.size() && index >= 0){
            steps.set(index, step);
        }
        else {
            Log.e(TAG, "Unable to set step to index "+index+" because index is out of bounds");
        }
    }

    public void addStep(Step step){
        this.steps.add(step);
    }

    public double getScore(){
        /*
        1. number of steps (five points per step)
        2. total number of stops in between (two points per stop)
        3. total distance walked (one point per 10m)
         */

        double stepScore = SCORE_STEP * steps.size();
        int noStops = 0;
        double totalDistanceWalked = 0;

        //get distances from actual from and to points
        if(steps.get(0).getStepType() == Step.TYPE_MATATU){
            if(steps.get(0).getStart() != null){
                totalDistanceWalked = totalDistanceWalked + steps.get(0).getStart().getDistance(from);
            }
        }

        if(steps.get(steps.size() - 1).getStepType() == Step.TYPE_MATATU){
            if(steps.get(steps.size() - 1).getDestination() != null){
                totalDistanceWalked = totalDistanceWalked + steps.get(steps.size() - 1).getDestination().getDistance(to);
            }
        }

        for(int index = 0; index < steps.size(); index++){
            if(steps.get(index).getStepType() == Step.TYPE_WALKING){
                totalDistanceWalked = totalDistanceWalked + steps.get(index).getStart().getDistance(steps.get(index).getDestination().getLatLng());
            }
            else if(steps.get(index).getStepType() == Step.TYPE_MATATU){
                noStops = noStops + steps.get(index).getRoute().getStops(0).size();
            }
        }
        //double stopScore = noStops * SCORE_STOP;
        double stopScore = 0;
        //TODO: get the actual route stops in the commute routes and not just all the stops
        double walkingScore = SCORE_WALKING * totalDistanceWalked;

        return stepScore + stopScore + walkingScore;
    }

    public ArrayList<LatLngPair> getStepLatLngPairs(ArrayList<LatLngPair> currLatLngPairs){
        if(currLatLngPairs == null){
            currLatLngPairs = new ArrayList<LatLngPair>();
        }

        for(int stepIndex = 0; stepIndex < steps.size(); stepIndex++){
            LatLngPair currLatLngPair = new LatLngPair(steps.get(stepIndex).getStart().getLatLng(), steps.get(stepIndex).getDestination().getLatLng(), -1);

            LatLngPair startLatLngPair = null;
            LatLngPair destinationLatLngPair = null;
            boolean add = true;
            boolean addStart = false;
            boolean addDestination = false;
            if(stepIndex == 0){
                startLatLngPair = new LatLngPair(from, steps.get(stepIndex).getStart().getLatLng(), -1);
                addStart = true;
            }
            if(stepIndex == steps.size() - 1){
                destinationLatLngPair = new LatLngPair(steps.get(stepIndex).getDestination().getLatLng(), to, -1);
                addDestination = true;
            }
            for(int lIndex = 0; lIndex < currLatLngPairs.size(); lIndex++){
                if(startLatLngPair != null && currLatLngPairs.get(lIndex).equals(startLatLngPair)){
                    addStart = false;
                }
                if(destinationLatLngPair != null && currLatLngPairs.get(lIndex).equals(destinationLatLngPair)){
                    addDestination = false;
                }
                if(currLatLngPairs.get(lIndex).equals(currLatLngPair)){
                    add = false;
                    break;
                }
            }

            if(add == true){
                currLatLngPairs.add(currLatLngPair);
            }
            if(addStart == true && startLatLngPair != null){
                currLatLngPairs.add(startLatLngPair);
            }
            if(addDestination == true && destinationLatLngPair != null){
                currLatLngPairs.add(destinationLatLngPair);
            }
        }

        return currLatLngPairs;
    }

    /**
     * Please run this method in an thread running asynchronously from the main thread
     * This method calculates the time it would take for the commute.
     *
     * @param latLngPairs   Dictionary of LatLngPairs where you can get distances for all the steps
     */
    public void setCommuteTime(ArrayList<LatLngPair> latLngPairs){
        time = -1;
        double totalTime = 0;

        //get distance between start and first step
        LatLngPair startLatLngPair = new LatLngPair(from, steps.get(0).getStart().getLatLng(), -1);
        double startDistance = -1;
        for(int lIndex = 0; lIndex < latLngPairs.size(); lIndex++){
            if(latLngPairs.get(lIndex).equals(startLatLngPair)){
                startDistance = latLngPairs.get(lIndex).getDistance();
                break;
            }
        }
        if(startDistance == -1){
            return;
        }
        else {
            Log.d(TAG, "Start distance is "+startDistance);
            totalTime = totalTime + (startDistance/SPEED_WALKING);
        }

        //get distance between last step and destination
        LatLngPair destinationLatLngPair = new LatLngPair(steps.get(steps.size() - 1).getDestination().getLatLng(), to, -1);
        double destinationDistance = -1;
        for(int lIndex = 0; lIndex < latLngPairs.size(); lIndex++){
            if(latLngPairs.get(lIndex).equals(destinationLatLngPair)){
                destinationDistance = latLngPairs.get(lIndex).getDistance();
                break;
            }
        }
        if(destinationDistance == -1){
            return;
        }
        else {
            Log.d(TAG, "Destination distance is "+destinationDistance);
            totalTime = totalTime + (destinationDistance/SPEED_WALKING);
        }

        //get distances in between steps
        for(int stepIndex = 0; stepIndex < steps.size(); stepIndex++){
            LatLngPair stepLatLngPair = new LatLngPair(steps.get(stepIndex).getStart().getLatLng(), steps.get(stepIndex).getDestination().getLatLng(), -1);
            double distance = -1;
            for(int lIndex = 0; lIndex < latLngPairs.size(); lIndex++){
                if(latLngPairs.get(lIndex).equals(stepLatLngPair)){
                    Log.d(TAG, latLngPairs.get(lIndex).getPointA()+" : "+latLngPairs.get(lIndex).getPointB()+" matches with "+stepLatLngPair.getPointA()+" : "+stepLatLngPair.getPointB());
                    distance = latLngPairs.get(lIndex).getDistance();
                    break;
                }
            }

            //return -1. Total distance should be atomic
            if(distance == -1){
                return;
            }

            if(steps.get(stepIndex).getStepType() == Step.TYPE_MATATU){
                totalTime = totalTime + (distance/SPEED_MATATU);
            }
            else if(steps.get(stepIndex).getStepType() == Step.TYPE_WALKING){
                totalTime = totalTime + (distance/SPEED_WALKING);
            }
        }
        Log.d(TAG, "Estimated time for commute is "+String.valueOf(totalTime));
        time = totalTime;
    }

    @Override
    public int describeContents() {
        return Commute.PARCELABLE_DESC;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(steps);//1
        parcel.writeParcelable(from, 0);//2
        parcel.writeParcelable(to, 0);//3
        parcel.writeDouble(time);//4
    }

    public void readFromParcel(Parcel in){
        in.readTypedList(steps, Step.CREATOR);//1
        from = in.readParcelable(LatLng.class.getClassLoader());//2
        to = in.readParcelable(LatLng.class.getClassLoader());//3
        time = in.readDouble();//4
    }

    /**
     * This static object is to facilitate for other parcelable objects to carry a Step object
     */
    public static final Creator<Commute> CREATOR=new Creator<Commute>() {
        @Override
        public Commute createFromParcel(Parcel source)
        {
            return new Commute(source);
        }

        @Override
        public Commute[] newArray(int size)
        {
            return new Commute[size];
        }
    };

    /**
     * This data carrier class stores an instance of a step in navigating
     */
    public static class Step implements Parcelable{
        public static  final String PARCELABLE_KEY = "Commute.Step";
        private static final int PARCELABLE_DESC = 4322;

        public static final int TYPE_MATATU = 0;
        public static final int TYPE_WALKING = 1;

        private Route route;
        private Stop start;
        private Stop destination;//destination stop regardless of whether current step is walking or in a matatu
        private int stepType;

        public Step(){
            route = null;
            start = null;
            destination = null;
            stepType = -1;
        }

        public Step(Parcel source){
            this();
            readFromParcel(source);
        }

        public Step(int stepType){
            this.stepType = stepType;
            this.route = null;
            this.start = null;
            this.destination = null;
        }

        public Step(int stepType, Route route, Stop start, Stop destination){
            this.stepType = stepType;
            this.route = route;
            this.start = start;
            this.destination = destination;
        }

        public int getStepType(){
            return stepType;
        }

        public Route getRoute() {
            return route;
        }

        public Stop getStart() {
            return start;
        }

        public Stop getDestination() {
            return destination;
        }

        @Override
        public int describeContents() {
            return Step.PARCELABLE_DESC;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flag) {
            parcel.writeInt(stepType);//1
            parcel.writeParcelable(destination, flag);//2
            parcel.writeParcelable(start, flag);//3
            parcel.writeParcelable(route, flag);//4
        }

        public void readFromParcel(Parcel in){
            stepType = in.readInt();//2
            destination = in.readParcelable(Stop.class.getClassLoader());//2
            start = in.readParcelable(Stop.class.getClassLoader());//3
            route = in.readParcelable(Route.class.getClassLoader());//4
        }

        /**
         * This static object is to facilitate for other parcelable objects to carry a Step object
         */
        public static final Creator<Step> CREATOR=new Creator<Step>() {
            @Override
            public Step createFromParcel(Parcel source)
            {
                return new Step(source);
            }

            @Override
            public Step[] newArray(int size)
            {
                return new Step[size];
            }
        };
    }

    /**
     * This class is used to compare two commutes' scores.
     * Can be used with List.sort
     */
    public static class ScoreComparator implements Comparator<Commute> {

        @Override
        public int compare(Commute c0, Commute c1) {
            double s0 = c0.getScore();
            double s1 = c1.getScore();

            if(s0 < s1){
                return -1;
            }
            else if(s0 == s1){
                return 0;
            }
            else {
                return 1;
            }
        }
    }

}
