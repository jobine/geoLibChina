package com.virjar.geolib.core;

import com.virjar.geolib.bean.Location;
import com.google.common.geometry.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeoUtils {
    public static double EARTH_RADIUS = 6378137.0;//WGS84, CGCS2000坐标系地球长半轴, m
    private static final char[] hexChar = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);

        for (byte b1 : b) {
            sb.append(hexChar[(b1 & 240) >>> 4]);
            sb.append(hexChar[b1 & 15]);
        }

        return sb.toString();
    }

    public static Map<Integer, Double> capHeightMap = new HashMap<Integer, Double>() {{
        for (int kRadius : Arrays.asList(2, 4, 8, 16, 32, 64, 128, 256)) {
            double meterRadius = kRadius * 1000D;
            put((int) meterRadius, capHeight(meterRadius));
        }
    }};


    private static double capHeight(double radius) {
        double rad = earthMeters2Radians(radius);
        return rad * rad * 2;
    }

    public static double earthMeters2Radians(Double meters) {
        return (2 * S2.M_PI) * (meters / 40075017);
    }

    public static double getCapHeight(int radius) {
        double capHeight = capHeightMap.getOrDefault(radius, 0d);
        if (capHeight == 0d) {
            capHeight = capHeight(radius);
        }
        return capHeight;
    }

    public static List<Long> cellIdsWithCapLevel(S2LatLng s2LatLng, int radius, int desLevel) {
        double capHeight = getCapHeight(radius);

        S2Cap cap = S2Cap.fromAxisHeight(s2LatLng.toPoint(), capHeight);
        S2RegionCoverer coverer = new S2RegionCoverer();
        coverer.setMaxLevel(desLevel);
        coverer.setMinLevel(desLevel);
        //圆形内的cell会自动做聚合，手动拆分
        return coverer.getCovering(cap)
                .cellIds()
                .stream()
                .flatMap((Function<S2CellId, Stream<Long>>) s2CellId -> {
                    int cellLevel = getLevel(s2CellId.id());
                    if (cellLevel == desLevel) {
                        return Stream.of(s2CellId.id());
                    } else {
                        return childrenCellId(s2CellId, cellLevel, desLevel).stream().map(S2CellId::id);
                    }
                }).collect(Collectors.toList());
    }

    public static int getLevel(Long inputs) {
        int n = 0;
        long input = inputs;
        while (input % 2 == 0) {
            input = input / 2;
            n += 1;
        }
        return 30 - n / 2;
    }

    public static List<S2CellId> childrenCellId(S2CellId s2CellId, int curLevel, int desLevel) {
        List<S2CellId> list = new ArrayList<>();
        if (curLevel < desLevel) {
            long interval = (s2CellId.childEnd().id() - s2CellId.childBegin().id()) / 4;
            for (int i = 0; i < 4; i++) {
                long id = s2CellId.childBegin().id() + interval * i;
                S2CellId cellId = new S2CellId(id);
                list.addAll(childrenCellId(cellId, curLevel + 1, desLevel));
            }
        } else {
            list.add(s2CellId);
        }
        return list;
    }

    public static Double lineDis(Double x1, Double y1, Double x2, Double y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }


    public static boolean outOfChina(Double lng, Double lat) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }


    public static Double pointToLineDis(Double x1, Double y1, Double x2, Double y2, Double x0, Double y0) {
        double
                a = lineDis(x1, y1, x2, y2),// 线段的长度
                b = lineDis(x1, y1, x0, y0),// 点到起点的距离
                c = lineDis(x2, y2, x0, y0);// 点到终点的距离
        //点在端点上
        if (c <= 0.000001 || b <= 0.000001) {
            return 0D;
        }
        //直线距离过短
        if (a <= 0.000001) {
            return b;
        }
        // 点在起点左侧，距离等于点到起点距离
        if (c * c >= a * a + b * b) {
            return b;
        }
        //点在终点右侧，距离等于点到终点距离
        if (b * b >= a * a + c * c) {
            return c;
        }
        //点在起点和终点中间，为垂线距离
        double k = (y2 - y1) / (x2 - x1);
        double z = y1 - k * x1;
        double p = (a + b + c) / 2;
        // 半周长
        double s = Math.sqrt(p * (p - a) * (p - b) * (p - c));//海伦公式求面积
        return 2 * s / a;// 返回点到线的距离（利用三角形面积公式求高）
    }


    public static Double distanceMeter(Location location, long s2CellId) {
        S2LatLng s2LatLng = new S2CellId(s2CellId).toLatLng();
        return distanceMeter(location, new Location(s2LatLng.lngDegrees(), s2LatLng.latDegrees()));
    }

    public static double rad(Double d) {
        return d * Math.PI / 180.0;
    }

    public static Double distanceMeter(Location locA, Location locB) {
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

    public static double area(List<Long> pylon) {
        double area = 0D;
        for (int i = 0; i < pylon.size() - 1; i++) {
            S2LatLng p1 = new S2CellId(pylon.get(i)).toLatLng();
            S2LatLng p2 = new S2CellId(pylon.get(i + 1)).toLatLng();
            area += p1.lngDegrees() * p2.latDegrees();
            area -= p2.lngDegrees() * p1.latDegrees();
        }
        return area / 2;
    }
}
