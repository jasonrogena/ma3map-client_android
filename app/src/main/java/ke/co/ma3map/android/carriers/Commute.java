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

    private List<Step> steps;
    private double score;

    public Commute(){
        this.steps = new ArrayList<Step>();
        this.score = 0;
    }

    public Commute(Parcel source){
        this();
        readFromParcel(source);
    }

    public double getScore(){
        return score;
    }

    public Step getStep(int index){
        return steps.get(index);
    }

    public List<Step> getSteps(){
        return steps;
    }

    public void addStep(Step step){
        this.steps.add(step);
        calculateScore();
    }

    public long getTotalDuration(){
        long duration = 0;
        for(int i = 0; i < steps.size(); i++){
            duration = duration + steps.get(i).getDuration();
        }
        return duration;
    }

    public void calculateScore(){
        //TODO: do calculation
        /*
        1. number of steps (five points per step)
        2. total number of stops in between (two points per stop)
        3. total distance walked (one point per 100m)
         */
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(steps);
        parcel.writeDouble(score);
    }

    public void readFromParcel(Parcel in){
        in.readTypedList(steps, Step.CREATOR);
        score = in.readDouble();
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
        private long duration;

        public Step(){
            route = null;
            start = null;
            destination = null;
            stepType = -1;
            duration = -1;
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
            this.duration = -1l;
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

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
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
            parcel.writeLong(duration);
        }

        public void readFromParcel(Parcel in){
            route = in.readParcelable(Route.class.getClassLoader());
            start = in.readParcelable(Stop.class.getClassLoader());
            destination = in.readParcelable(Stop.class.getClassLoader());
            stepType = in.readInt();
            duration = in.readLong();
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
