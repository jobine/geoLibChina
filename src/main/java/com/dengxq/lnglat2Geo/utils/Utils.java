package com.dengxq.lnglat2Geo.utils;

import com.dengxq.lnglat2Geo.GeoTransImpl;
import com.dengxq.lnglat2Geo.entity.AdminBoundary;
import com.dengxq.lnglat2Geo.entity.CellAdmin;
import com.google.common.geometry.S2CellId;
import com.speedment.common.tuple.Tuple2;
import com.speedment.common.tuple.Tuple3;
import com.speedment.common.tuple.Tuple4;
import com.speedment.common.tuple.Tuples;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

    public static Map<Long, Integer> buildBoundaryAdminCell(List<CellAdmin> data) {
        return data.stream().collect(Collectors.toMap(s -> s.cellId, s -> s.adCode));
    }

    public static Map<Long, List<Long>> buildBoundaryIndex(Map<Long, List<Tuple3<Long, Integer, Integer>>> boundaryData) {
        Map<Long, List<Tuple2<Long, Long>>> map = boundaryData.keySet().stream().map(s -> Tuples.of(
                new S2CellId(s).parent(GeoTransImpl.min_level).id(), s
        )).collect(Collectors.groupingBy(Tuple2::get0));
        Map<Long, List<Long>> result = new HashMap<>();
        for (Map.Entry<Long, List<Tuple2<Long, Long>>> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream().map(Tuple2::get1).collect(Collectors.toList()));
        }
        return result;
    }

    public static Map<Long, List<Tuple3<Long, Integer, Integer>>> parseBoundaryData(List<AdminBoundary> adminBoundaryInJavas) {
        Map<Long, List<Tuple4<Long, Long, Integer, Boolean>>> map = adminBoundaryInJavas
                .stream()
                .flatMap((Function<AdminBoundary, Stream<Tuple4<Long, Long, Integer, Boolean>>>)
                        adminBoundary -> adminBoundary.boundary.stream().flatMap(
                                (Function<List<Long>, Stream<Tuple4<Long, Long, Integer, Boolean>>>)
                                        input -> {
                                            List<Tuple4<Long, Long, Integer, Boolean>> ret = new ArrayList<>();
                                            for (int i = 0; i < input.size() - 2; i++) {
                                                ret.add(Tuples.of(input.get(i), input.get(i + 1), adminBoundary.code, true));
                                                ret.add(Tuples.of(input.get(i + 1), input.get(i), adminBoundary.code, false));
                                            }
                                            return ret.stream();
                                        }
                        ))
                .collect(Collectors.groupingBy(Tuple4::get0));

        Map<Long, List<Tuple3<Long, Integer, Integer>>> ret = new HashMap<>();

        for (Map.Entry<Long, List<Tuple4<Long, Long, Integer, Boolean>>> entry : map.entrySet()) {
            List<Tuple4<Long, Long, Integer, Boolean>> value = entry.getValue();

            Map<Long, List<Tuple3<Long, Integer, Boolean>>> sssMap = value.stream().map(longLongIntegerBooleanTuple4 -> Tuples.of(longLongIntegerBooleanTuple4.get1(), longLongIntegerBooleanTuple4.get2(), longLongIntegerBooleanTuple4.get3())).collect(Collectors.groupingBy(new Function<Tuple3<Long, Integer, Boolean>, Long>() {
                @Override
                public Long apply(Tuple3<Long, Integer, Boolean> longIntegerBooleanTuple3) {
                    return longIntegerBooleanTuple3.get0();
                }
            }));

            List<Tuple3<Long, Integer, Integer>> ret1 = new ArrayList<>();
            ret.put(entry.getKey(), ret1);
            for (Map.Entry<Long, List<Tuple3<Long, Integer, Boolean>>> sss : sssMap.entrySet()) {
                List<Tuple2<Integer, Boolean>> list = sss.getValue().stream().map(longIntegerBooleanTuple3 ->
                        Tuples.of(longIntegerBooleanTuple3.get1(), longIntegerBooleanTuple3.get2())).sorted((o1, o2) -> o2.get1().compareTo(o1.get1())).collect(Collectors.toList());

                if (list.size() > 2) {
                    throw new RuntimeException();
                }
                if (list.size() == 2) {
                    if (!list.get(0).get1() || list.get(1).get1())
                        throw new RuntimeException();
                    Integer first = list.get(0).get0();
                    Integer second = list.get(1).get0();
                    ret1.add(Tuples.of(sss.getKey(), first, second));
                } else {
                    if (list.get(0).get1()) {
                        ret1.add(Tuples.of(sss.getKey(), list.get(0).get0(), -1));
                    } else {
                        ret1.add(Tuples.of(sss.getKey(), -1, -1));
                    }
                }
            }
        }
        return ret;

    }

}
