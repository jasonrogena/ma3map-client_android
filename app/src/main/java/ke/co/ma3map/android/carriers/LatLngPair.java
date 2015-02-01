package ke.co.ma3map.android.carriers;

import com.google.android.gms.maps.model.LatLng;

/**
* Created by jrogena on 01/02/2015.
*/
public class LatLngPair {
    private final LatLng pointA;
    private final LatLng pointB;
    private final double distance;

    public LatLngPair(LatLng pointA, LatLng pointB, double distance){
        this.pointA = pointA;
        this.pointB = pointB;
        this.distance = distance;
    }

    public LatLng getPointA(){
        return pointA;
    }

    public LatLng getPointB(){
        return pointB;
    }

    public double getDistance(){
        return distance;
    }

    public boolean equals(LatLngPair latLngPair){
        if(latLngPair.getPointA().equals(pointA) && latLngPair.getPointB().equals(pointB)){
            return true;
        }
        return false;
    }
}
