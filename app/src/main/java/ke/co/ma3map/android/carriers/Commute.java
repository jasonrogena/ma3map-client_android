package ke.co.ma3map.android.carriers;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jason on 28/10/14.
 */
public class Commute implements Parcelable {

    public static final String PARCELABLE_KEY = "Commute";

    private final double SCORE_STEP = 5;//score given for each step in commute
    private final double SCORE_WALKING = 0.1;//score given for each meter walked
    private final double SCORE_STOP = 2;//score given for each stop in commute

    private List<Step> steps;

    public Commute(){
        this.steps = new ArrayList<Step>();
    }

    public Commute(Parcel source){
        this();
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
        double stopScore = 0;
        //TODO: get the actual route stops in the commute routes and not just all the stops
        double totalDistanceWalked = 0;
        for(int index = 0; index < steps.size(); index++){
            if(steps.get(index).getStepType() == Step.TYPE_WALKING){
                totalDistanceWalked = totalDistanceWalked + steps.get(index).getStart().getDistance(steps.get(index).getDestination().getLatLng());
            }
        }

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
    }

    public void readFromParcel(Parcel in){
        in.readTypedList(steps, Step.CREATOR);
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
}
