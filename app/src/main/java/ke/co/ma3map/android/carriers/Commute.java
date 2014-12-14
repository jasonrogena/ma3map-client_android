package ke.co.ma3map.android.carriers;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by jason on 28/10/14.
 */
public class Commute implements Parcelable {

    public static final String PARCELABLE_KEY = "Commute";

    private final double SCORE_STEP = 5;//score given for each step in commute
    private final double SCORE_WALKING = 0.1;//score given for each meter walked
    private final double SCORE_STOP = 2;//score given for each stop in commute

    private LatLng from;//actual point on map use wants to go from
    private LatLng to;//actual point on map user want to go to
    private List<Step> steps;

    public Commute(LatLng from, LatLng to){
        this.from = from;
        this.to = to;
        this.steps = new ArrayList<Step>();
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
            this.steps.add(steps.get(index));
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(steps);
        parcel.writeParcelable(from, 0);
        parcel.writeParcelable(to, 0);
    }

    public void readFromParcel(Parcel in){
        in.readTypedList(steps, Step.CREATOR);
        from = in.readParcelable(LatLng.class.getClassLoader());
        to = in.readParcelable(LatLng.class.getClassLoader());
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

        public int getStepType(){
            return stepType;
        }

        public Route getRoute() {
            return route;
        }

        public void setRoute(Route route) {
            this.route = route;
        }

        public Stop getStart() {
            return start;
        }

        public void setStart(Stop start) {
            this.start = start;
        }

        public Stop getDestination() {
            return destination;
        }

        public void setDestination(Stop destination) {
            this.destination = destination;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(route, i);
            parcel.writeParcelable(start, i);
            parcel.writeParcelable(destination, i);
            parcel.writeInt(stepType);
        }

        public void readFromParcel(Parcel in){
            route = in.readParcelable(Route.class.getClassLoader());
            start = in.readParcelable(Stop.class.getClassLoader());
            destination = in.readParcelable(Stop.class.getClassLoader());
            stepType = in.readInt();
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
