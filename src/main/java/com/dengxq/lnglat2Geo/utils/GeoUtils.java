package com.dengxq.lnglat2Geo.utils;

import com.dengxq.lnglat2Geo.entity.enums.Azimuth;
import com.dengxq.lnglat2Geo.entity.Bound;
import com.dengxq.lnglat2Geo.entity.enums.CoordinateSystem;
import com.dengxq.lnglat2Geo.entity.Location;
import com.google.common.geometry.S2;
import com.google.common.geometry.S2Cap;
import com.google.common.geometry.S2LatLng;
import com.speedment.common.tuple.Tuple2;
import com.speedment.common.tuple.Tuples;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GeoUtils {
    private static final double x_PI = Math.PI * 3000.0 / 180.0;
    private static final double EE = 0.00669342162296594323;
    private static final double A = 6378245.0; // BJZ54坐标系地球长半轴, m
    public static double EQUATOR_C = 20037508.3427892; // 赤道周长, m
    public static double EARTH_RADIUS = 6378137.0;//WGS84, CGCS2000坐标系地球长半轴, m
    public static double EARTH_POLAR_RADIUS = 6356725.0; //极半径, m

    public static double SQRT2 = 1.414213562;

    public static double rad(Double d) {
        return d * Math.PI / 180.0;
    }

    public static Double distance(Location locA, Location locB) {
        Double lngA = locA.lng;
        Double latA = locA.lat;
        Double lngB = locB.lng;
        Double latB = locB.lat;
        double f = rad((latA + latB) / 2);
        double g = rad((latA - latB) / 2);
        double l = rad((lngA - lngB) / 2);
        if (g == 0 && l == 0)
            return 0D;
        double sg = Math.sin(g), sl = Math.sin(l), sf = Math.sin(f);

        double s, c, w, r, d, h1, h2, dis, a = EARTH_RADIUS, fl = 1 / 298.257;
        sg = sg * sg;
        sl = sl * sl;
        sf = sf * sf;
        s = sg * (1 - sl) + (1 - sf) * sl;
        c = (1 - sg) * (1 - sl) + sf * sl;
        w = Math.atan(Math.sqrt(s / c));
        r = Math.sqrt(s * c) / w;
        d = 2 * w * a;
        h1 = (3 * r - 1) / 2 / c;
        h2 = (3 * r + 1) / 2 / s;
        dis = d * (1 + fl * (h1 * sf * (1 - sg) - h2 * (1 - sf) * sg));

        //return dis.formatted("%.2f").toDouble
        return new BigDecimal(dis).setScale(2, RoundingMode.HALF_UP).doubleValue();

    }


    public static Tuple2<Double, Double> gcj02ToWgs84(Double lng, Double lat) {
        if (outOfChina(lng, lat))
            return Tuples.of(lng, lat);
        double dlat = transformLat(lng - 105.0, lat - 35.0);
        double dlng = transformLng(lng - 105.0, lat - 35.0);
        double radlat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radlat);
        magic = 1 - EE * magic * magic;
        double sqrtmagic = Math.sqrt(magic);
        dlat = (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * Math.PI);
        dlng = (dlng * 180.0) / (A / sqrtmagic * Math.cos(radlat) * Math.PI);
        double mglat = lat + dlat;
        double mglng = lng + dlng;
        return Tuples.of(lng * 2 - mglng, lat * 2 - mglat);
    }


    public static Tuple2<Double, Double> gcj02ToBD09(Double lng, Double lat) {
        if (outOfChina(lng, lat)) return Tuples.of(lng, lat);
        double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * x_PI);
        double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * x_PI);
        double bd_lng = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;
        return Tuples.of(bd_lng, bd_lat);
    }

    public static Tuple2<Double, Double> bd09ToGCJ02(Double lng, Double lat) {
        if (outOfChina(lng, lat)) return Tuples.of(lng, lat);
        double x = lng - 0.0065;
        double y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_PI);
        double gg_lng = z * Math.cos(theta);
        double gg_lat = z * Math.sin(theta);
        return Tuples.of(gg_lng, gg_lat);
    }

    public static Tuple2<Double, Double> wgs84ToGCj02(Double lng, Double lat) {
        double mglat, mglng;
        if (outOfChina(lng, lat)) {
            mglat = lat;
            mglng = lng;
        } else {
            double dLat = transformLat(lng - 105.0, lat - 35.0);
            double dLon = transformLng(lng - 105.0, lat - 35.0);
            double radLat = lat / 180.0 * Math.PI;
            double magic = Math.sin(radLat);
            magic = 1 - EE * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * Math.PI);
            dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * Math.PI);
            mglat = lat + dLat;
            mglng = lng + dLon;
        }
        return Tuples.of(mglng, mglat);
    }


    private static Double transformLng(Double lng, Double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    private static Double transformLat(Double lng, Double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }


    public static boolean isInChina(Double lng, Double lat) {
        return !outOfChina(lng, lat);
    }

    public static boolean outOfChina(Double lng, Double lat) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    private static final double kEarthCircumferenceMeters = 1000 * 40075.017;

    private static double earthMeters2Radians(Double meters) {
        return (2 * S2.M_PI) * (meters / kEarthCircumferenceMeters);
    }

    public static S2Cap genS2Cap(Location loc, Double radius) {
        S2LatLng s2LatLng = S2LatLng.fromDegrees(loc.lat, loc.lng);
        double radiusRadians = earthMeters2Radians(radius);
        return S2Cap.fromAxisHeight(s2LatLng.normalized().toPoint(), (radiusRadians * radiusRadians) / 2);
    }

    public static Tuple2<Double, Double> toGCJ02(Double lng, Double lat, CoordinateSystem coordType) {
        switch (coordType) {
            case WGS84:
                return GeoUtils.wgs84ToGCj02(lng, lat);
            case BD09:
                return GeoUtils.bd09ToGCJ02(lng, lat);
            default:
                return Tuples.of(lng, lat);
        }
    }

    public static Tuple2<Double, Double> toWGS84(Double lng, Double lat, CoordinateSystem coordType) {
        switch (coordType) {
            case GCJ02:
                return GeoUtils.gcj02ToWgs84(lng, lat);
            case BD09:
                Tuple2<Double, Double> d02 = GeoUtils.bd09ToGCJ02(lng, lat);
                return GeoUtils.gcj02ToWgs84(d02.get0(), d02.get1());
            default:
                return Tuples.of(lng, lat);
        }
    }

    public static Location move(Location loc, Double distance, Azimuth azimuth) {
        double radLat = GeoUtils.rad(loc.lat);
        double radLng = GeoUtils.rad(loc.lng);

        double ec = GeoUtils.EARTH_POLAR_RADIUS + (GeoUtils.EARTH_RADIUS - GeoUtils.EARTH_POLAR_RADIUS) * (90 - loc.lng) / 90;
        double ed = ec * Math.cos(radLat);

        double dx = distance * Math.sin(azimuth.getCode() * Math.PI / 180);
        double dy = distance * Math.cos(azimuth.getCode() * Math.PI / 180);

        double lng = (dx / ed + radLng) * 180 / Math.PI;
        double lat = (dy / ec + radLat) * 180 / Math.PI;

        return new Location(lng, lat);
    }

    public static Bound genCapBound(Location loc, Double radius) {
        double swDistance = GeoUtils.SQRT2 * radius;
        Location sw = move(loc, swDistance, Azimuth.SouthWest);
        Location ne = move(loc, swDistance, Azimuth.NorthEast);
        return new Bound(sw, ne);
    }

    public static Bound genCapInnerBound(Location loc, Double radius) {
        // val swDistance = SQRT2 / 2d * radius
        Location sw = move(loc, radius, Azimuth.SouthWest);
        Location ne = move(loc, radius, Azimuth.NorthEast);
        return new Bound(sw, ne);
    }
}
