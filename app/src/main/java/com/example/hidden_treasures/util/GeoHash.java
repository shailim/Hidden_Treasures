package com.example.hidden_treasures.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class GeoHash {

    private static final String base32 = "0123456789bcdefghjkmnpqrstuvwxyz";


    /* Gets the geohash of a point with it's latitude and longitude and curent zoom level  */
    public static String encode(double latitude, double longitude, float precision) {

        int index = 0; // index into base32 map
        int bit = 0; // each char holds 5 bits
        boolean evenBit = true;
        StringBuilder geohash = new StringBuilder();

        double latMin =  -90, latMax =  90;
        double longMin = -180, longMax = 180;

        // the length of the geohash is the precision (zoom level)
        while (geohash.length() < precision) {
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


    public static LatLngBounds bounds(String geohash) {
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


    // returns the geohash for a neighboring cell
    // directions are : 0 - n, 1 - e, 2 - s, 3 - w
    public static String adjacent(String geohash, int direction) {
        geohash = geohash.toLowerCase();

        final String[][] neighbour = new String[4][2];
        neighbour[0][0] = "p0r21436x8zb9dcf5h7kjnmqesgutwvy";
        neighbour[0][1] = "bc01fg45238967deuvhjyznpkmstqrwx";
        neighbour[1][0] = "14365h7k9dcfesgujnmqp0r2twvyx8zb";
        neighbour[1][1] = "238967debc01fg45kmstqrwxuvhjyznp";
        neighbour[2][0] = "bc01fg45238967deuvhjyznpkmstqrwx";
        neighbour[2][1] = "p0r21436x8zb9dcf5h7kjnmqesgutwvy";
        neighbour[3][0] = "238967debc01fg45kmstqrwxuvhjyznp";
        neighbour[3][1] = "14365h7k9dcfesgujnmqp0r2twvyx8zb";

        final String[][] border = new String[4][2];
        border[0][0] = "prxz";
        border[0][1] = "bcfguvyz";
        border[1][0] = "028b";
        border[1][1] = "0145hjnp";
        border[2][0] = "bcfguvyz";
        border[2][1] = "prxz";
        border[3][0] = "0145hjnp";
        border[3][1] = "028b";


        char lastCh = geohash.charAt(geohash.length()-1);    // last character of hash
        String parent = geohash.substring(0, geohash.length()-1); // hash without last character

        int type = geohash.length() % 2;

        // check for edge-cases which don't share common prefix
        if (border[direction][type].indexOf(lastCh) != -1 && parent.length() > 0) {
            parent = adjacent(parent, direction);
        }

        // append letter for direction to parent
        return parent + base32.charAt(neighbour[direction][type].indexOf(lastCh));
    }

    public static String[] neighbours(String geohash) {
        // neighbors array is [n, ne, e, se, s, sw, w, nw]
        String[] neighbours = new String[8];
        neighbours[0] = adjacent(geohash, 0);
        neighbours[1] = adjacent(neighbours[0], 1);
        neighbours[2] = adjacent(geohash, 1);
        neighbours[3] = adjacent(neighbours[2], 2);
        neighbours[4] = adjacent(geohash, 2);
        neighbours[5] = adjacent(neighbours[4], 3);
        neighbours[6] = adjacent(geohash, 3);
        neighbours[7] = adjacent(neighbours[6], 0);
        return neighbours;
    }

}
