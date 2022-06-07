package com.dengxq.lnglat2Geo.utils;

import com.google.common.geometry.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class S2Utils {

    //  // 预算，提升速度
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
    public static List<Long> getCellId(S2LatLng s2LatLng, int radius, int desLevel) {
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
}
