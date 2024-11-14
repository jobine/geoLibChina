package com.ming.geolib.utils;

import org.elasticsearch.geometry.Rectangle;
import org.elasticsearch.geometry.utils.Geohash;

import java.util.Collection;

public class GeoUtils {
    public static String encode(Double longitude, Double latitude) {
        return Geohash.stringEncode(longitude, latitude, 6);
    }

    public static Point decode(String geohash) {
        Rectangle rec = Geohash.toBoundingBox(geohash);
        return new Point((rec.getMaxLon() + rec.getMinLon()) / 2, (rec.getMaxLat() + rec.getMinLat()) / 2);
    }

    public static boolean outOfChina(Point pt) {
        return pt.getLongitude() < 72.004 || pt.getLongitude() > 137.8347 || pt.getLatitude() < 0.8293 || pt.getLatitude() > 55.8271;
    }

    public static Collection<? extends CharSequence> neighbors(String geohash) {
        return Geohash.getNeighbors(geohash);
    }
}
