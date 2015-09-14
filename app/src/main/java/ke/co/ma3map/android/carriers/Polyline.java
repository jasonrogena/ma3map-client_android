package ke.co.ma3map.android.carriers;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by jrogena on 14/09/2015.
 */
public class Polyline {
    private final ArrayList<LatLng> points;

    /**
     * This method decodes polylines encoded using the algorithm described in
     * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
     * Code obtained from https://github.com/googlemaps/android-maps-utils
     *
     * @param encodedPath   String representing an encoded polyline
     */
    public Polyline(final String encodedPath) {
        int len = encodedPath.length();

        // For speed we preallocate to an upper bound on the final length, then
        // truncate the array before returning.
        final ArrayList<LatLng> path = new ArrayList<LatLng>();
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            path.add(new LatLng(lat * 1e-5, lng * 1e-5));
        }

        this.points = path;
    }

    public ArrayList<LatLng> getPoints(){
        return points;
    }

}
