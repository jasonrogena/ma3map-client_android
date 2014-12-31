package ke.co.ma3map.android.views;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import ke.co.ma3map.android.carriers.Commute;
import ke.co.ma3map.android.R;

/**
 * Created by jason on 31/12/14.
 */
public class CommuteRecyclerView extends LinearLayout {

    Context context;
    TextView mainRoute;
    TextView connections;
    TextView time;
    View bound;

    public CommuteRecyclerView(Context context) {
        super(context);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setLayoutParams(layoutParams);
        this.setBackgroundColor(context.getResources().getColor(R.color.accent));
        this.setOrientation(LinearLayout.VERTICAL);
        final float scale = getContext().getResources().getDisplayMetrics().density;
        int horizontalPadding = (int) (16 * scale + 0.5f);
        int verticalPadding = (int) (8 * scale + 0.5f);
        this.setPadding(horizontalPadding, verticalPadding, horizontalPadding, 0);

        this.context = context;
        this.mainRoute = new TextView(context);
        LinearLayout.LayoutParams mainRouteLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        mainRouteLP.setMargins(0, 0, 0, (int) (5 * scale + 0.5f));
        this.mainRoute.setLayoutParams(mainRouteLP);
        this.mainRoute.setTextColor(context.getResources().getColor(R.color.default_text_color));
        this.addView(this.mainRoute);

        this.connections = new TextView(context);
        LinearLayout.LayoutParams connectionsLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        connectionsLP.setMargins(horizontalPadding, 0, 0, (int) (5 * scale + 0.5f));
        this.connections.setLayoutParams(connectionsLP);
        this.connections.setTextColor(context.getResources().getColor(R.color.secondary_text_color));
        this.addView(this.connections);

        this.time = new TextView(context);
        LinearLayout.LayoutParams timeLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        timeLP.setMargins(horizontalPadding, 0, 0, (int) (5 * scale + 0.5f));
        this.time.setLayoutParams(timeLP);
        this.time.setTextColor(context.getResources().getColor(R.color.primary));
        this.addView(this.time);

        this.bound = new View(context);
        LinearLayout.LayoutParams boundLP = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
        this.bound.setLayoutParams(boundLP);
        bound.setBackgroundColor(R.color.bound_color);
        this.addView(bound);
    }

    public void setCommute(Commute commute){
        final float scale = getContext().getResources().getDisplayMetrics().density;
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

        this.mainRoute.setText(mainRoute);

        this.connections.setText(connections);

        if(commute.getTime() < (3600)){//time equal to or greater than 1 hour
            this.time.setText(String.valueOf((int)(commute.getTime()/60))+" Mins");
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

            this.time.setText(String.valueOf(hour)+" "+hourText+", "+String.valueOf(minutes)+" "+minText);
        }
    }
}
