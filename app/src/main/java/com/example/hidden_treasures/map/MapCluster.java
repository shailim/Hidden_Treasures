package com.example.hidden_treasures.map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.hidden_treasures.R;
import com.example.hidden_treasures.util.BitmapFormat;
import com.example.hidden_treasures.util.GeoHash;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapCluster {

    // clusters markers according to their geohash
    public static void clusterMarkers(MapFragment mapFrag) {
        Log.i("MapFragment", "now onto clustering");
        String curHash = GeoHash.encode(mapFrag.map.getCameraPosition().target.latitude, mapFrag.map.getCameraPosition().target.longitude, 2);
        if (mapFrag.markerTable.get(curHash) != null) {
            for (Map.Entry<String, List<Marker>> set : mapFrag.markerTable.get(curHash).entrySet()) {
                if (set.getValue() != null && set.getValue().size() > 2) {
                    List<Marker> removed = new ArrayList<>();
                    for (Marker marker : set.getValue()) {
                        marker.setVisible(false);
                        removed.add(marker);
                        Log.i("MapFragment", "removed a marker");
                    }
                    Marker cluster = mapFrag.map.addMarker(new MarkerOptions().title("Cluster").position(set.getValue().get(0).getPosition()));
                    setCLusterIcon(mapFrag, cluster, removed.size());
                    cluster.setTag(removed);
                    mapFrag.clusters.add(cluster);
                }
            }
        }
    }

    // sets the icon for the cluster markers
    public static void setCLusterIcon(MapFragment mapFrag, Marker cluster, int num) {
        int icon;
        int size;
        if (num < 5) {
            icon = R.drawable.lightpurplecircle;
            size = 80;
        } else if (num < 7) {
            icon = R.drawable.purplecircle;
            size = 95;
        } else {
            icon = R.drawable.indigocircle;
            size = 105;
        }
        Bitmap image = BitmapFactory.decodeResource(mapFrag.getResources(), icon);
        image = Bitmap.createScaledBitmap(image, size, size, false);
        image = BitmapFormat.getCircularBitmap(image);
        cluster.setIcon(BitmapDescriptorFactory.fromBitmap(image));
        // setting opacity
        cluster.setAlpha(0.5f);
    }

    // removes clusters and re-shows markers
    public static void deCluster(MapFragment mapFrag) {
        for (Marker cluster : mapFrag.clusters) {
            // re-show all the markers
            for (Marker marker : (List<Marker>) cluster.getTag()) {
                marker.setVisible(true);
            }
            // remove the cluster
            cluster.remove();
        }
        // empty the clusters list
        mapFrag.clusters.clear();
    }

}
