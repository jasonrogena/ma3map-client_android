package ke.co.ma3map.android.handlers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ke.co.ma3map.android.carriers.Commute;
import ke.co.ma3map.android.carriers.Route;
import ke.co.ma3map.android.carriers.Stop;
import ke.co.ma3map.android.listeners.ProgressListener;

/**
 * @author  Jason Rogena <jasonrogena@gmail.com>
 * @since   2014-11-17
 *
 * This Class is responsible for determining the best path between source and destination.
 *
 * <strong>ALGORITHM</strong>
 * <p></p>
 * <strong>Problem</strong>
 * 1. Given a set with source route stops S and a set with destinations route stops D, find paths
 *      between members of set S and set D in the least time possible.
 * 2. Order determined paths using predetermined path weights (not to be confused with edge weights
 *      explained in the next section).
 * <p>
 * <strong>Solution</strong>
 * The class Implements Batched Shortest Path Computation using Dijkstra's Algorithm.
 * Key elements in Dijkstra's Algorithm are nodes, edges/arcs and weights. In our case:
 *  - nodes: A section of a route, terminated on both ends by stops
 *  - edges/arcs: two nodes are connected to each other if the corresponding routes share the
 *                  terminating stop or the terminating stops for each of the two routes are
 *                  MAX_WALKING_DISTANCE away from each other
 *  - weight: the weight of the edge joining stop A and B is dependent on the walking distance from
 *              terminating stop at A to terminating stop at B
 *  - path: A plot from one of the potential starting points in the graph to a potential finishing
 *              point in the graph
 * <p>
 * <strong>Considerations</strong>
 * 1. The possibility of getting more than one edge with the least weight from a node is very high.
 *  In this algorithm, all the edges with the least weights from a node are considered. It is therefore
 *  possible to create more than one paths from a node.
 * <p>
 * <strong>Optimizations</strong>
 * 1. Path should have a maximum of MAX_NODES nodes
 * 2. Once the algorithm obtains MAX_COMMUTES paths, it terminates and does not continue traversing
 *      the graph
 * <p>
 * <strong>Future work</strong>
 * Since the classic Djkstra's algorithm is considered a naive algorithm [2], we consider solving the
 * same problem using faster algorithms presented in [1, 2]
 * <p>
 * <strong>References</strong>
 * @see <a href="https://dl.google.com/eclipse/plugin/4.4">1. D. Delling, A. Goldberg, and R. Werneck. Faster Batched Shortest Paths in Road Networks. (Accessed 18th Jan 2015)</a>
 * @see <a href="http://algo2.iti.kit.edu/download/diss_geisberger.pdf">2. R. Geisberger, et al. Advanced Route Planning in Transportation Networks. (Accessed 16th Jan 2015)</a>
 */
public class BestPath extends Data {
    private static final String TAG = "ma3map.BestPath";
    private static final int MAX_COMMUTES = 10;//the maximum number of commutes to be generated
    private static final int MAX_NODES = 2;//the maximum number of routes (nodes) that should be in a commute
    private static final double MAX_WALKING_DISTANCE = 1000;//the maximum distance allowed for waliking when connecting nodes (routes)
    public static final int MAX_FROM_POINTS = 5;
    public static final int MAX_TO_POINTS = 10;
    private final List<Stop> from;
    private final LatLng actualFrom;
    private final List<Stop> to;
    private final LatLng actualTo;
    private final ArrayList<Route> routes;
    private final ArrayList<Commute> allCommutes;
    private int bestPathThreadIndex;
    private String bundleKey;

    public BestPath(Context context, List<Stop> from, LatLng actualFrom, List<Stop> to, LatLng actualTo, ArrayList<Route> routes, String bundleKey){
        super(context);
        this.from = from;
        this.actualFrom = actualFrom;
        this.to = to;
        this.actualTo = actualTo;
        this.routes = routes;
        this.allCommutes = new ArrayList<Commute>();
        bestPathThreadIndex = 0;
        this.bundleKey = bundleKey;
    }

    /**
     * Initiates the process of getting a list of <code>Commutes</code> using the provided list
     * of from and to <code>Stops</code>
     * <p>
     * @see ke.co.ma3map.android.carriers.Commute
     * @see ke.co.ma3map.android.carriers.Stop
     */
    public void calculateCommutes(){
        final ArrayList<Commute> commutes = new ArrayList<Commute>();

        //spawn asynchronous threads for each of the from points for calculating the best path
        for(int fromIndex = 0; fromIndex < from.size(); fromIndex++){
            final Stop currFrom = from.get(fromIndex);
            BestPathTasker currBestPathTasker = new BestPathTasker(from.size(), currFrom);
            currBestPathTasker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
        }
    }

    /**
     * Gets the next possible <code>routes</code> to be added to the provided <code>Commute</code>.
     * <p>
     * This method is recursive and will terminate when:
     *  -   All possible <code>routes</code> have been considered
     *  -   Number of <code>routes</code> in provided <code>commute</code> have surpassed {@link #MAX_NODES}
     * <p>
     * @param commute       Commute object carrying the current route path being constructed
     * @param noGoRouteIDs  A list of IDs corresponding to routes that should not be considered as the
     *                      next possible route. Use <code>route.getId()</code> to get a route's ID
     *
     * @return  A <code>Commute</code> object containing a complete path from a potential from <code>Stop</code>
     *          to a potential to <code>Stop</code> or <code>null</code> if no path is found
     *
     * @see ke.co.ma3map.android.carriers.Commute
     * @see ke.co.ma3map.android.carriers.Route
     * @see ke.co.ma3map.android.carriers.Stop
     */
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
                Commute.Step tmpStep = commute.getStep(commute.getSteps().size() - 1);
                if(tmpStep.getStepType() == Commute.Step.TYPE_MATATU){
                    commute.setStep(commute.getSteps().size() - 1, new Commute.Step(tmpStep.getStepType(), tmpStep.getRoute(), tmpStep.getStart(), currTo));
                }
                toIsInRoute = true;
                break;
            }
        }

        if(!toIsInRoute){
            //last node does not have the destination

            //check if enough commute alternatives have been gotten
            if(allCommutes.size() > MAX_COMMUTES){
                return null;
            }

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
                final Route currReferenceRoute = routes.get(rIndex);
                for(int sIndex = 0; sIndex < nodeStops.size(); sIndex++) {
                    final Stop currReferenceStop = nodeStops.get(sIndex);
                    double distanceToStop = currReferenceRoute.getDistanceToStop(currReferenceStop);//if stop is in route, distance is going to be 0
                    /*double distanceToStop = -1;
                    if(currReferenceRoute.isStopInRoute(currReferenceStop)){
                        distanceToStop = 0;
                    }*/
                    //search for routeID in noGoRouteIDs
                    boolean checkRoute = true;
                    for(String searchID : noGoRouteIDs){
                        if(searchID.equals(currReferenceRoute.getId())){
                            checkRoute = false;
                            break;
                        }
                    }

                    //check if nodeStops(sIndex) is also starting point for last node
                    Stop startForLastNode = commute.getSteps().get(commute.getSteps().size() - 1).getStart();
                    if(startForLastNode.equals(currReferenceStop)){
                        checkRoute = false;
                    }
                    if (checkRoute == true
                            && distanceToStop != -1 && distanceToStop < MAX_WALKING_DISTANCE) {
                        Log.d(TAG, "** Checking route with index = "+rIndex+" of "+routes.size());
                        List<String> newNoGoRouteIDs = new ArrayList<String>();
                        newNoGoRouteIDs.add(currReferenceRoute.getId());
                        newNoGoRouteIDs.addAll(noGoRouteIDs);

                        Commute newCommute = new Commute(actualFrom, actualTo);
                        newCommute.setSteps(commute.getSteps());
                        Commute.Step tmpStep = commute.getStep(newCommute.getSteps().size() - 1);
                        if(tmpStep.getStepType() == Commute.Step.TYPE_MATATU){
                            newCommute.setStep(newCommute.getSteps().size() - 1, new Commute.Step(tmpStep.getStepType(), tmpStep.getRoute(), tmpStep.getStart(), currReferenceStop));
                        }

                        //add walking step if necessary
                        if(distanceToStop > 0){
                            Commute.Step walkingStep = new Commute.Step(Commute.Step.TYPE_WALKING, null, currReferenceStop, currReferenceRoute.getClosestStop(currReferenceStop));
                            newCommute.addStep(walkingStep);
                        }
                        Commute.Step matatuStep = new Commute.Step(Commute.Step.TYPE_MATATU, currReferenceRoute, currReferenceRoute.getClosestStop(currReferenceStop), null);
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
     * Gets all <code>routes</code> in {@link #allCommutes} that contain the provided <code>stop</code>.
     * <p>
     * @param stop  The <code>stop</code> to be checked against all available <code>routes</code>
     *              to this class
     *
     * @return  An ArrayList with <code>routes</code> that contain the provided <code>stop</code>
     * @see ke.co.ma3map.android.carriers.Route
     * @see ke.co.ma3map.android.carriers.Stop
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

    /**
     * @author Jason Rogena <jasonrogena@gmail.com>
     *
     * This class implements an asynchronous thread that tries to obtain a path from one <code>stop</code>
     * considered as the 'from' point to one of the <code>stops</code> in {@link #to}
     */
    private class BestPathTasker extends AsyncTask<Integer, Integer, ArrayList<Commute>>{
        private final int threadCount;
        private final Stop from;

        /**
         * Class constructor.
         * <p>
         * @param threadCount   The number of threads (in the thread pool) initialised to calculate
         *                      route paths
         * @param from          The <code>stop</code> from where to start determining the path
         * @see ke.co.ma3map.android.carriers.Stop
         */
        public BestPathTasker(int threadCount, Stop from){
            this.threadCount = threadCount;
            this.from = from;
        }

        /**
         * Code to be run asynchronously from parent thread. Among other things, potential routes from
         * the provided {@link #from} <code>stop</code> are determined using {@link BestPath#getBestCommute(ke.co.ma3map.android.carriers.Commute, java.util.List)}
         * <p>
         * @param params    List of integers not really used anywhere. Set as anything
         *
         * @return  A list of <code>commutes</code> that all start from the provided {@link #from}
         * @see ke.co.ma3map.android.carriers.Stop
         * @see ke.co.ma3map.android.carriers.Commute
         */
        @Override
        protected ArrayList<Commute> doInBackground(Integer... params) {
            final ArrayList<Commute> commutes = new ArrayList<Commute>();
            Log.d(TAG, "########################################");
            ArrayList<Route> fromRoutes = getRoutesWithStop(from);
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
                Commute.Step firstStep = new Commute.Step(Commute.Step.TYPE_MATATU, fromRoutes.get(index), from, null);
                currCommute.addStep(firstStep);
                Commute resultantBestCommute = getBestCommute(currCommute, noGoRouteIDs);
                if(resultantBestCommute != null) {
                    commutes.add(resultantBestCommute);
                }
                //Log.d(TAG, "Current path has "+currBestPath.size() + " routes");
                Log.d(TAG, "*********************************");
            }
            Log.d(TAG, "########################################");
            return commutes;
        }

        /**
         * Code to be executed after thread is done executing background tasks.
         * <p>
         * @param commutes  A list of potential routes from the initialised {@link #from} to any of the
         *                  stops in {@link BestPath#to}
         */
        @Override
        protected void onPostExecute(ArrayList<Commute> commutes) {
            super.onPostExecute(commutes);
            if(commutes != null){
                allCommutes.addAll(commutes);
            }

            bestPathThreadIndex++;
            if(bestPathThreadIndex == threadCount){//this is the last BestPathTasker to finish executing
                //finalize the progressListeners
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(bundleKey, allCommutes);
                finalizeProgressListeners(bundle, "Done calculating commute paths", ProgressListener.FLAG_DONE);
            }
            else {
                //update the progress listeners
                updateProgressListeners(bestPathThreadIndex, threadCount, "Calculating commute paths", ProgressListener.FLAG_WORKING);
            }
        }
    }
}
