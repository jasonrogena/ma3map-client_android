package ke.co.ma3map.android.handlers;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ke.co.ma3map.android.carriers.Commute;
import ke.co.ma3map.android.carriers.Route;
import ke.co.ma3map.android.carriers.Stop;

/**
 * Created by jason on 17/11/14.
 * This Class is responsible for determining the best path between source and destination
 */
public class BestPath {
    private static final String TAG = "ma3map.BestPath";
    private static final int MAX_NODES = 2;//the maximum number of routes (nodes) that should be in a commute
    private static final double MAX_WALKING_DISTANCE = 50;//the maximum distance allowed for waliking when connecting nodes (routes)
    public static final int MAX_FROM_POINTS = 5;
    public static final int MAX_TO_POINTS = 10;
    private final List<Stop> from;
    private final LatLng actualFrom;
    private final List<Stop> to;
    private final LatLng actualTo;
    private final ArrayList<Route> routes;

    public BestPath(List<Stop> from, LatLng actualFrom, List<Stop> to, LatLng actualTo, ArrayList<Route> routes){
        this.from = from;
        this.actualFrom = actualFrom;
        this.to = to;
        this.actualTo = actualTo;
        this.routes = routes;
    }

    /**
     * This method returns a list of commutes starting with the best
     * @return
     */
    public ArrayList<Commute> getCommutes(){
        ArrayList<Commute> commutes = new ArrayList<Commute>();

        for(int fromIndex = 0; fromIndex < from.size(); fromIndex++){
            Log.d(TAG, "########################################");
            Stop currFrom = from.get(fromIndex);
            ArrayList<Route> fromRoutes = getRoutesWithStop(currFrom);
            Log.d(TAG, "From point in "+fromRoutes.size()+" routes");

            //check if to point in any of the
            for(int index = 0; index < fromRoutes.size(); index++){
                Log.d(TAG, "*********************************");
                List<String> noGoRouteIDs = new ArrayList<String>();
                for(int j = 0; j < fromRoutes.size(); j++){
                    noGoRouteIDs.add(fromRoutes.get(j).getId());
                }

                //ArrayList<Route> currNodes = new ArrayList<Route>();
                //currNodes.add(fromRoutes.get(index));
                Commute currCommute = new Commute(actualFrom, actualTo);
                Commute.Step firstStep = new Commute.Step(Commute.Step.TYPE_MATATU);
                firstStep.setStart(currFrom);
                firstStep.setRoute(fromRoutes.get(index));
                currCommute.addStep(firstStep);
                Commute resultantBestCommute = getBestCommute(currCommute, noGoRouteIDs);
                if(resultantBestCommute != null) {
                    commutes.add(resultantBestCommute);
                }
                //Log.d(TAG, "Current path has "+currBestPath.size() + " routes");
                Log.d(TAG, "*********************************");
            }
            Log.d(TAG, "########################################");
        }

        if(commutes != null){
            Collections.sort(commutes, new Commute.ScoreComparator());
        }
        return commutes;
    }

    private Commute getBestCommute(Commute commute, List<String> noGoRouteIDs){
        ArrayList<Route> nodes = commute.getMatatuRoutes();

        Log.d(TAG, "Getting best path for node list with "+nodes.get(0).getShortName()+" as first node");
        Log.d(TAG, " ** node list has "+nodes.size()+" before processing");
        Log.d(TAG, " ** noGoRouteIDs has "+noGoRouteIDs.size()+" nodes");
        //check if last node has stop
        Route lastNode = nodes.get(nodes.size() - 1);
        Log.d(TAG, "Last node is "+lastNode.getShortName());

        boolean toIsInRoute = false;

        for(int toIndex = 0; toIndex < to.size(); toIndex++){//still assumes that to stops are ordered in terms of closeness to actual destination
            Stop currTo = to.get(toIndex);
            if(lastNode.isStopInRoute(currTo)){
                List<Commute.Step> finalSteps = commute.getSteps();
                if(finalSteps.get(finalSteps.size() - 1).getStepType() == Commute.Step.TYPE_MATATU){
                    finalSteps.get(finalSteps.size() - 1).setDestination(currTo);
                }
                commute.setSteps(finalSteps);
                toIsInRoute = true;
                break;
            }
        }

        if(!toIsInRoute){
            //last node does not have the destination

            //check if we have reached the maxNode limit
            if(nodes.size() > MAX_NODES){
                Log.d(TAG, "Node list already has a maximum number of nodes. Returning null");
                //max nodes reached and last node does not have destination, return null
                return null;
            }

            //check if all routes have been traversed
            if(noGoRouteIDs.size() >= routes.size()){
                Log.d(TAG, "Node list already has all possible routes. Returning null");
                //all routes have been traversed, no route to destination found
                return null;
            }

            //get all routes that are linked to this route
            //and determine which is the best path
            Log.d(TAG, "Getting all routes linked to last node = "+lastNode.getShortName());
            Commute bestCommute = null;
            List<Stop> nodeStops = lastNode.getStops(0);
            Log.d(TAG, "Last node has "+nodeStops.size()+" stops");
            for (int rIndex = 0; rIndex < routes.size(); rIndex++) {
                for(int sIndex = 0; sIndex < nodeStops.size(); sIndex++) {
                    //double distanceToStop = routes.get(rIndex).getDistanceToStop(nodeStops.get(sIndex));//if stop is in route, distance is going to be 0
                    double distanceToStop = -1;
                    if(routes.get(rIndex).isStopInRoute(nodeStops.get(sIndex))){
                        distanceToStop = 0;
                    }
                    //search for routeID in noGoRouteIDs
                    boolean checkRoute = true;
                    for(String searchID : noGoRouteIDs){
                        if(searchID.equals(routes.get(rIndex).getId())){
                            checkRoute = false;
                            break;
                        }
                    }

                    //check if nodeStops(sIndex) is also starting point for last node
                    Stop startForLastNode = commute.getSteps().get(commute.getSteps().size() - 1).getStart();
                    if(startForLastNode.equals(nodeStops.get(sIndex))){
                        checkRoute = false;
                    }
                    if (checkRoute == true
                            && distanceToStop != -1 && distanceToStop < MAX_WALKING_DISTANCE){
                        Log.d(TAG, "** Checking route with index = "+rIndex+" of "+routes.size());
                        List<String> newNoGoRouteIDs = new ArrayList<String>();
                        newNoGoRouteIDs.add(routes.get(rIndex).getId());
                        newNoGoRouteIDs.addAll(noGoRouteIDs);

                        Commute newCommute = new Commute(actualFrom, actualTo);
                        List<Commute.Step> oldSteps = commute.getSteps();
                        if(oldSteps.get(oldSteps.size() - 1).getStepType() == Commute.Step.TYPE_MATATU){
                            oldSteps.get(oldSteps.size() - 1).setDestination(nodeStops.get(sIndex));
                        }
                        newCommute.setSteps(oldSteps);
                        if(distanceToStop > 0){
                            Commute.Step walkingStep = new Commute.Step(Commute.Step.TYPE_WALKING);
                            walkingStep.setStart(nodeStops.get(sIndex));
                            walkingStep.setDestination(routes.get(rIndex).getClosestStop(nodeStops.get(sIndex)));
                            newCommute.addStep(walkingStep);
                        }
                        Commute.Step matatuStep = new Commute.Step(Commute.Step.TYPE_MATATU);
                        matatuStep.setStart(routes.get(rIndex).getClosestStop(nodeStops.get(sIndex)));
                        matatuStep.setRoute(routes.get(rIndex));
                        newCommute.addStep(matatuStep);

                        Commute currBestCommute = getBestCommute(newCommute, newNoGoRouteIDs);
                        if(currBestCommute != null){
                            Log.d(TAG, "Current commute score = "+currBestCommute.getScore());
                        }
                        if(bestCommute != null){
                            Log.d(TAG, "Current best commute score = "+bestCommute.getScore());
                        }
                        else {
                            Log.d(TAG, "Best commute not set yet. Current commute will be best");
                        }

                        if(bestCommute == null || (currBestCommute != null && currBestCommute.getScore() < bestCommute.getScore())){
                            bestCommute = currBestCommute;
                        }
                        if(currBestCommute!= null) break;//don't iterate through any other stops in the last node to be compared with the current route
                    }
                }

            }

            //Log.d(TAG, "Best path has "+bestPath.size()+" nodes");
            return bestCommute;
        }
        Log.d(TAG, "To stop is in the last node. Node list complete. Returning list");
        return commute;
    }

    /**
     * TODO: debug
     * @param stop
     * @return
     */
    private ArrayList<Route> getRoutesWithStop(Stop stop){
        ArrayList<Route> stopRoutes = new ArrayList<Route>();

        for(int index = 0; index < routes.size(); index++){
            if(routes.get(index).isStopInRoute(stop)){
                stopRoutes.add(routes.get(index));
            }
        }

        return stopRoutes;
    }
}
