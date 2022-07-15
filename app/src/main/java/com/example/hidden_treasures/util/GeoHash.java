package com.example.hidden_treasures.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class GeoHash {

    private final String base32 = "0123456789bcdefghjkmnpqrstuvwxyz";


    /* Gets the geohash of a point with it's latitude and longitude and curent zoom level  */
    public String encode(double latitude, double longitude, float zoomLevel) {

        int index = 0; // index into base32 map
        int bit = 0; // each char holds 5 bits
        boolean evenBit = true;
        StringBuilder geohash = new StringBuilder();

        double latMin =  -90, latMax =  90;
        double longMin = -180, longMax = 180;

        // the length of the geohash is the precision (zoom level)
        while (geohash.length() < zoomLevel) {
            // alternate between dividing latitude and longitude
            if (evenBit) {
                // divide E-W longitude
                double longMid = (longMin + longMax) / 2;
                if (longitude >= longMid) {
                    index = index*2 + 1;
                    longMin = longMid;
                } else {
                    index = index*2;
                    longMax = longMid;
                }
            } else {
                // divide N-S latitude
                double latMid = (latMin + latMax) / 2;
                if (latitude >= latMid) {
                    index = index*2 + 1;
                    latMin = latMid;
                } else {
                    index = index*2;
                    latMax = latMid;
                }
            }
            evenBit = !evenBit;

            if (++bit == 5) {
                // 5 bits makes a character: append it and start over
                geohash.append(base32.charAt(index));
                bit = 0;
                index = 0;
            }
        }
        return geohash.toString();
    }


    public LatLngBounds bounds(String geohash) {
        boolean evenBit = true;
        double latMin =  -90, latMax =  90;
        double longMin = -180, longMax = 180;

        // for each character in the geohash
        for (int i = 0; i < geohash.length(); i++) {
            char chr = geohash.charAt(i);
            int index = base32.indexOf(chr);

            // decoding the index, reverse process from the encode function
            for (int n = 4; n >= 0; n--) {
                // I understand what this is doing but I'm confused how it works
                int bitN = index >> n & 1;
                if (evenBit) {
                    // longitude
                    double longMid = (longMin + longMax) / 2;
                    if (bitN == 1) {
                        longMin = longMid;
                    } else {
                        longMax = longMid;
                    }
                } else {
                    // latitude
                    double latMid = (latMin + latMax) / 2;
                    if (bitN == 1) {
                        latMin = latMid;
                    } else {
                        latMax = latMid;
                    }
                }
                evenBit = !evenBit;
            }
        }
        LatLngBounds bounds = new LatLngBounds(new LatLng(latMin, longMin), new LatLng(latMax, longMax));
        return bounds;
    }

}
