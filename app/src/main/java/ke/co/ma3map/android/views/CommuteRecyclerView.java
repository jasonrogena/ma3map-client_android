package ke.co.ma3map.android.views;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ke.co.ma3map.android.carriers.Commute;
import ke.co.ma3map.android.R;
import ke.co.ma3map.android.services.Navigate;

/**
 * Created by jason on 31/12/14.
 */
public class CommuteRecyclerView extends LinearLayout
                                 implements View.OnClickListener{

    public static final int MODE_RETRACTED = 0;
    public static final int MODE_EXPANDED = 1;

    private static final String TAG = "ma3map.CommuteRecyclerView";
    private static final long ANIMATION_EXPAND_TIME = 200;

    private int mode;

    private final Context context;
    private TextView mainRouteTV;
    private TextView connectionsTV;
    private LinearLayout stepsLL;
    private ArrayList<TextView> steps;
    private RelativeLayout navigateRL;
    private View navigationBlueV;
    private TextView startB;
    private TextView timeTV;
    //private View bound;

    private Commute commute;
    private OnItemClickedListener onItemClickedListener;
    private int position;

    public CommuteRecyclerView(Context context, OnItemClickedListener onItemClickedListener, int position) {
        super(context);

        this.position = position;
        this.onItemClickedListener = onItemClickedListener;
        final float scale = getContext().getResources().getDisplayMetrics().density;
        int horizontalPadding = (int) (16 * scale + 0.5f);
        int verticalPadding = (int) (8 * scale + 0.5f);
        MarginLayoutParams layoutParams = new MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(horizontalPadding, verticalPadding, horizontalPadding, 0);
        this.setLayoutParams(layoutParams);
        this.setBackgroundColor(context.getResources().getColor(R.color.accent));
        this.setOrientation(LinearLayout.VERTICAL);

        this.context = context;
        this.mainRouteTV = new TextView(context);
        LinearLayout.LayoutParams mainRouteLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        mainRouteLP.setMargins(horizontalPadding, verticalPadding, 0, (int) (5 * scale + 0.5f));
        this.mainRouteTV.setLayoutParams(mainRouteLP);
        this.mainRouteTV.setTextColor(context.getResources().getColor(R.color.default_text_color));
        this.addView(this.mainRouteTV);

        this.connectionsTV = new TextView(context);
        LinearLayout.LayoutParams connectionsLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        connectionsLP.setMargins(horizontalPadding*2, 0, 0, (int) (5 * scale + 0.5f));
        this.connectionsTV.setLayoutParams(connectionsLP);
        this.connectionsTV.setTextColor(context.getResources().getColor(R.color.secondary_text_color));
        this.addView(this.connectionsTV);

        this.stepsLL = new LinearLayout(context);
        stepsLL.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams stepsLLLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        stepsLLLP.setMargins(horizontalPadding*2, 0, 0, (int) (5 * scale + 0.5f));
        this.stepsLL.setLayoutParams(stepsLLLP);
        this.addView(stepsLL);
        this.steps = new ArrayList<TextView>();

        this.navigateRL = new RelativeLayout(context);
        LinearLayout.LayoutParams navigateLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        navigateLP.setMargins(0, 0, 0, 0);
        navigateRL.setLayoutParams(navigateLP);
        navigateRL.setOnClickListener(this);

        this.navigationBlueV = new View(context);
        RelativeLayout.LayoutParams viewLP = new RelativeLayout.LayoutParams(0, (int)(50 * scale + 0.5f));
        viewLP.addRule(RelativeLayout.CENTER_VERTICAL);
        viewLP.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        navigationBlueV.setLayoutParams(viewLP);
        navigationBlueV.setBackgroundColor(context.getResources().getColor(R.color.primary));
        navigateRL.addView(navigationBlueV);

        this.timeTV = new TextView(context);
        RelativeLayout.LayoutParams timeRL = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        timeRL.addRule(RelativeLayout.CENTER_VERTICAL);
        timeRL.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        timeRL.setMargins(horizontalPadding*2, 0, 0, 0);
        timeTV.setLayoutParams(timeRL);
        this.timeTV.setTextColor(context.getResources().getColor(R.color.primary));
        navigateRL.addView(timeTV);

        this.startB = new TextView(context);
        RelativeLayout.LayoutParams startLP = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        startLP.addRule(RelativeLayout.CENTER_VERTICAL);
        startLP.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        startLP.setMargins(horizontalPadding*2, 0, 0, 0);
        startB.setLayoutParams(startLP);
        startB.setTextColor(context.getResources().getColor(android.R.color.white));
        navigateRL.addView(startB);

        this.addView(this.navigateRL);

        /*this.bound = new View(context);
        LinearLayout.LayoutParams boundLP = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
        this.bound.setLayoutParams(boundLP);
        bound.setBackgroundColor(R.color.bound_color);
        this.addView(bound);*/

        this.setOnClickListener(this);
    }

    public void setCommute(Commute commute){
        this.commute = commute;
    }

    /**
     * This method shows the RecyclerView in as retracted mode
     */
    public void retract(){
        //add the data
        setMode(MODE_RETRACTED);
        //this.connectionsTV.setVisibility(TextView.VISIBLE);
        updateMainRoute();

        //this.stepsLL.setVisibility(LinearLayout.GONE);

        updateTime();

        //perform the animations
        final float scale = getContext().getResources().getDisplayMetrics().density;
        //1. Padding for container
        /*ValueAnimator containerPaddingAnimator = ValueAnimator.ofInt(this.getPaddingLeft(), 16);
        containerPaddingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int padding = (int)animation.getAnimatedValue();
                CommuteRecyclerView.this.setPadding(padding, padding, padding, padding);
            }
        });*/

        //2. Text Size
        /*ValueAnimator textSizeAnimator = ValueAnimator.ofFloat(mainRouteTV.getTextSize(), 14);
        textSizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float textSize = (float)animation.getAnimatedValue();
                mainRouteTV.setTextSize(textSize);
            }
        });*/

        //3. Elevation
        ValueAnimator elevationAnimator = null;
        if(Build.VERSION.SDK_INT < 21){
            elevationAnimator = ValueAnimator.ofFloat(ViewCompat.getElevation(this), 0);
        }
        else {
            elevationAnimator = ValueAnimator.ofFloat(this.getElevation(), 0);
        }
        elevationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float elevation = (float)animation.getAnimatedValue();
                if(Build.VERSION.SDK_INT < 21){
                    ViewCompat.setElevation(CommuteRecyclerView.this, elevation);
                }
                else {
                    CommuteRecyclerView.this.setElevation(elevation);
                }
            }
        });

        //4. Navigation blue background width
        ValueAnimator navigationBlueWidthAnimator = ValueAnimator.ofInt(navigationBlueV.getLayoutParams().width, 0);
        navigationBlueWidthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int width = (int)animation.getAnimatedValue();
                ViewGroup.LayoutParams viewLayoutParams = navigationBlueV.getLayoutParams();
                viewLayoutParams.width = width;
                navigationBlueV.setLayoutParams(viewLayoutParams);
                if(width <= (timeTV.getWidth() + ((RelativeLayout.LayoutParams)timeTV.getLayoutParams()).leftMargin)){
                    timeTV.setTextColor(context.getResources().getColor(R.color.primary));
                }
            }
        });

        //5 Move time
        ValueAnimator timeAnimator = ValueAnimator.ofInt(((RelativeLayout.LayoutParams)timeTV.getLayoutParams()).leftMargin, ((int) (16 * scale + 0.5f))*2);
        startB.setVisibility(Button.GONE);
        timeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int margin = (int)animation.getAnimatedValue();
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)timeTV.getLayoutParams();
                layoutParams.leftMargin = margin;
                timeTV.setLayoutParams(layoutParams);
            }
        });

        //6. Hide steps
        final int initialStepsLLHeight = stepsLL.getHeight();
        final int connectionsHeight = (connectionsTV.getMeasuredHeight());
        ValueAnimator stepsAnimator = ValueAnimator.ofInt(stepsLL.getHeight(), connectionsHeight);
        stepsAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int height = (int)animation.getAnimatedValue();
                if(height == connectionsHeight){
                    connectionsTV.setVisibility(TextView.VISIBLE);
                    stepsLL.setVisibility(LinearLayout.GONE);
                    ViewGroup.LayoutParams layoutParams = stepsLL.getLayoutParams();
                    layoutParams.height = initialStepsLLHeight;
                    stepsLL.setLayoutParams(layoutParams);
                }
                else {
                    ViewGroup.LayoutParams layoutParams = stepsLL.getLayoutParams();
                    layoutParams.height = height;
                    stepsLL.setLayoutParams(layoutParams);
                }
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(ANIMATION_EXPAND_TIME);
        animatorSet.playTogether(elevationAnimator, navigationBlueWidthAnimator, timeAnimator, stepsAnimator);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.start();
    }

    public void expand(){
        //add the data
        setMode(MODE_EXPANDED);
        //this.connectionsTV.setVisibility(TextView.GONE);
        updateMainRoute();

        //this.stepsLL.setVisibility(LinearLayout.VISIBLE);
        updateSteps();

        updateTime();

        //perform the animations
        final float scale = getContext().getResources().getDisplayMetrics().density;
        //1. Padding for container
        /*ValueAnimator containerPaddingAnimator = ValueAnimator.ofInt(this.getPaddingLeft(), 16);
        containerPaddingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int padding = (int)animation.getAnimatedValue();
                CommuteRecyclerView.this.setPadding(padding, padding, padding, padding);
            }
        });*/

        //2. Text Size
        /*ValueAnimator textSizeAnimator = ValueAnimator.ofFloat(mainRouteTV.getTextSize(), 14);
        textSizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float textSize = (float)animation.getAnimatedValue();
                mainRouteTV.setTextSize(textSize);
            }
        });*/

        //3. Elevation
        ValueAnimator elevationAnimator = ValueAnimator.ofFloat(0, 20);
        elevationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float elevation = (float)animation.getAnimatedValue();
                if(Build.VERSION.SDK_INT < 21){
                    ViewCompat.setElevation(CommuteRecyclerView.this, elevation);
                }
                else {
                    CommuteRecyclerView.this.setElevation(elevation);
                }
            }
        });

        //4. Navigation blue background width
        ValueAnimator navigationBlueWidthAnimator = ValueAnimator.ofInt(0, this.getWidth());
        navigationBlueWidthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int width = (int)animation.getAnimatedValue();
                ViewGroup.LayoutParams viewLayoutParams = navigationBlueV.getLayoutParams();
                viewLayoutParams.width = width;
                navigationBlueV.setLayoutParams(viewLayoutParams);
                if(width >= (timeTV.getWidth() + ((RelativeLayout.LayoutParams)timeTV.getLayoutParams()).leftMargin)){
                    startB.setVisibility(Button.VISIBLE);
                    timeTV.setTextColor(context.getResources().getColor(android.R.color.white));
                }
            }
        });

        //5 Move time
        ValueAnimator timeAnimator = ValueAnimator.ofInt(((RelativeLayout.LayoutParams)timeTV.getLayoutParams()).leftMargin, (this.getWidth() - ((RelativeLayout.LayoutParams)timeTV.getLayoutParams()).leftMargin - timeTV.getWidth()));//TODO: change
        //timeTV.setTextColor(context.getResources().getColor(android.R.color.white));
        timeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int margin = (int)animation.getAnimatedValue();
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)timeTV.getLayoutParams();
                layoutParams.leftMargin = margin;
                timeTV.setLayoutParams(layoutParams);
            }
        });

        //6. show steps
        int initialStepsLLHeight = ((int) (31 * scale + 0.5f)) * steps.size();
        stepsLL.setVisibility(LinearLayout.GONE);
        connectionsTV.setVisibility(TextView.GONE);
        ValueAnimator stepsAnimator = ValueAnimator.ofInt(connectionsTV.getMeasuredHeight(), initialStepsLLHeight);
        stepsAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int height = (int)animation.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = stepsLL.getLayoutParams();
                layoutParams.height = height;
                stepsLL.setLayoutParams(layoutParams);
                if(stepsLL.getVisibility() == LinearLayout.GONE){
                    stepsLL.setVisibility(LinearLayout.VISIBLE);
                }
                if(connectionsTV.getVisibility() == TextView.VISIBLE){
                    connectionsTV.setVisibility(TextView.GONE);
                }
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(ANIMATION_EXPAND_TIME);
        animatorSet.playTogether(elevationAnimator, navigationBlueWidthAnimator, timeAnimator, stepsAnimator);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.start();
    }

    /**
     * This method updates all views related to the major route name
     */
    private void updateMainRoute(){
        List<Commute.Step> steps = commute.getSteps();
        String mainRoute = null;
        String connections = null;
        for(int stepIndex = 0; stepIndex < steps.size(); stepIndex++){
            if(steps.get(stepIndex).getStepType() == Commute.Step.TYPE_MATATU && mainRoute == null){
                mainRoute = steps.get(stepIndex).getRoute().getLongName() + " - No. " + steps.get(stepIndex).getRoute().getShortName();
            }
            else if(steps.get(stepIndex).getStepType() == Commute.Step.TYPE_MATATU){
                boolean first = false;
                if(connections == null){
                    connections = "Connect with";
                    first = true;
                }

                if(first == true){
                    connections = connections + " No. " + steps.get(stepIndex).getRoute().getShortName();
                }
                else {
                    connections = connections + " and No. " + steps.get(stepIndex).getRoute().getShortName();
                }
            }
        }
        if(connections == null){
            connections = "Direct";
        }

        this.mainRouteTV.setText(mainRoute);

        this.connectionsTV.setText(connections);
    }

    /**
     * This method updates views related to time in this RecyclerView
     */
    private void updateTime(){
        startB.setText(context.getResources().getString(R.string.start_commute));

        if(commute.getTime() < (3600)){//time equal to or greater than 1 hour
            this.timeTV.setText(String.valueOf((int) (commute.getTime() / 60)) + " Mins");
        }
        else {
            double rawTime = commute.getTime()/3600;
            int hour = (int) Math.floor(rawTime);
            int minutes = (int)((rawTime - hour) * 60);

            String hourText = "Hrs";
            if(hour == 1){
                hourText = "Hr";
            }
            String minText = "Mins";
            if(minutes == 1){
                minText = "Min";
            }

            this.timeTV.setText(String.valueOf(hour) + " " + hourText + ", " + String.valueOf(minutes) + " " + minText);
        }
    }

    /**
     * This method updates views related to the actual steps (Whether it be matatu or walking steps)
     */
    private void updateSteps(){
        emptyStepLL();
        List<Commute.Step> stepData = this.commute.getSteps();

        final float scale = getContext().getResources().getDisplayMetrics().density;
        int horizontalMargin = 0;
        int verticalMargin = (int) (4 * scale + 0.5f);

        TextView firstStepTV = new TextView(context);
        LinearLayout.LayoutParams firstStepLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        firstStepLP.setMargins(horizontalMargin, 0, horizontalMargin, verticalMargin);
        firstStepTV.setLayoutParams(firstStepLP);
        firstStepTV.setText("1. " + context.getResources().getString(R.string.walk_to) + " " + stepData.get(0).getStart().getName());
        steps.add(firstStepTV);
        stepsLL.addView(firstStepTV);

        for(int stepIndex = 0; stepIndex < stepData.size(); stepIndex++){
            Commute.Step currStep = stepData.get(stepIndex);

            TextView currStepTV = new TextView(context);
            LinearLayout.LayoutParams stepLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            stepLP.setMargins(horizontalMargin, 0, horizontalMargin, verticalMargin);
            currStepTV.setLayoutParams(stepLP);

            if(currStep.getStepType() == Commute.Step.TYPE_WALKING){
                currStepTV.setText(String.valueOf(stepIndex + 2) + ". " + context.getResources().getString(R.string.walk_to) + " " + currStep.getDestination().getName());
            }
            else if(currStep.getStepType() == Commute.Step.TYPE_MATATU){
                currStepTV.setText(String.valueOf(stepIndex + 2) + ". " + context.getResources().getString(R.string.take_a) + " " + currStep.getRoute().getLongName() + " (No. " + currStep.getRoute().getShortName() + ") " + context.getResources().getString(R.string.to) + " " + currStep.getDestination().getName());
            }
            else {
                Log.e(TAG, "Current commute type has no type");
            }

            steps.add(currStepTV);
            stepsLL.addView(currStepTV);
        }

        Log.d(TAG, "Commute in total has "+steps.size()+" steps");
    }

    private void emptyStepLL(){
        for(int stepIndex = 0; stepIndex < steps.size(); stepIndex++){
            stepsLL.removeView(steps.get(stepIndex));
        }

        steps = new ArrayList<TextView>();
    }

    private void setMode(int mode){
        this.mode = mode;
    }

    private int toggleMode(){
        if(getMode() == MODE_RETRACTED){
            expand();
            return MODE_EXPANDED;
        }
        else{//default. Assumes CommuteRecyclerView is expanded
            retract();
            return MODE_RETRACTED;
        }
    }

    @Override
    public void onClick(View view) {
        if(view == CommuteRecyclerView.this){
            if(onItemClickedListener != null){
                onItemClickedListener.onClick(getMode());
            }
            toggleMode();
        }
        else if(view == navigateRL){
            Log.i(TAG, "********* Navigate button clicked *********");
            List<Commute.Step> steps = commute.getSteps();
            for(int stepCount = 0; stepCount < steps.size(); stepCount++){
                Commute.Step currStep = steps.get(stepCount);
                if(currStep.getStepType() == Commute.Step.TYPE_MATATU){
                    Log.d(TAG, "Current step is route: "+currStep.getRoute().getLongName());
                }
                else {
                    Log.d(TAG, "Current step is for walking");
                }
                if(currStep.getStart() != null) Log.d(TAG, "Start stop: " + currStep.getStart().getName());
                else Log.w(TAG, "Current step has a null start");
                if(currStep.getDestination() != null) Log.d(TAG, "End stop: "+currStep.getDestination().getName());
                else Log.w(TAG, "Current step has a null destination");
            }
            Log.i(TAG, "********* End button clicked event *********");

            Intent intent = new Intent(context, Navigate.class);
            intent.putExtra(Commute.PARCELABLE_KEY, commute);
            context.startService(intent);
        }
    }

    public int getMode(){
        if(mode == MODE_EXPANDED){
            return MODE_EXPANDED;
        }
        else {
            return MODE_RETRACTED;//default mode is retracted
        }
    }

    /**
     * This interface handles RecycleView item clicks that are specifically of the CommuteRecyclerView
     * variety
     */
    public interface OnItemClickedListener {
        /**
         * This method is called when a CommuteRecyclerView is clicked before any other code is called
         *
         * @param currentMode The mode the CommuteRecyclerView was in before being clicked.
         *             Can be MODE_RETRACTED or MODE_EXPANDED
         */
        public void onClick(int currentMode);
    }
}
