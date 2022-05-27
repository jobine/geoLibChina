package com.dengxq.lnglat2Geo;

import com.dengxq.lnglat2Geo.entity.*;
import com.dengxq.lnglat2Geo.entity.enums.CoordinateSystem;
import com.dengxq.lnglat2Geo.entity.enums.DistrictLevel;
import com.dengxq.lnglat2Geo.loader.ILoader;
import com.dengxq.lnglat2Geo.utils.GeoUtils;
import com.dengxq.lnglat2Geo.utils.LineUtils;
import com.dengxq.lnglat2Geo.utils.S2Utils;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.speedment.common.tuple.Tuple2;
import com.speedment.common.tuple.Tuple3;
import com.speedment.common.tuple.Tuple4;
import com.speedment.common.tuple.Tuples;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeoTransImpl {
    public static final int min_level = 12;
    public static Map<Integer, List<BusinessAreaData>> cityBusinessArea = ILoader.Storage.getOrLoad().getAreaGroups()
            .stream().collect(Collectors.toMap(s -> s.cityAdCode, s -> s.areas));

    public static Map<Long, Integer> boundaryAdminCell = ILoader.Storage.getOrLoad().getOrCreateRuntimeBoundaryAdminCell();

    public static Map<Long, List<Tuple3<Long, Integer, Integer>>> boundaryData =
            ILoader.Storage.getOrLoad().getOrCreateRuntimeBoundaryData();

    public static Map<Long, List<Long>> boundaryIndex = ILoader.Storage.getOrLoad().getOrCreateRuntimeBoundaryIndex();
    public static Map<String, String> cityLevelData = ILoader.Storage.getOrLoad().getCityLevel();

    public static Map<Integer, AdminNode> adminData = ILoader.Storage.getOrLoad().getAdminData()
            .stream().collect(
                    Collectors.toMap(adminNode -> adminNode.id, adminNode -> adminNode)
            );

    public static Map<Integer, AdminNode> streetData = ILoader.Storage.getOrLoad().getStreetData().stream().collect(
            Collectors.toMap(adminNode -> adminNode.id, adminNode -> adminNode)
    );

    public static Map<String, String> countryCode = ILoader.Storage.getOrLoad().getCountryCode();


    public static Map<String, String> cityNameMap = new HashMap<String, String>() {{
        put("重庆市", "");
        put("上海市", "上海城区");
        put("北京市", "北京城区");
        put("天津市", "天津城区");
        put("那曲市", "那曲地区");
    }};

    public static Map<String, String> districtNameMap = new HashMap<String, String>() {
        {
            put("云州区", "大同县");
            put("平城区", "城区");
            put("云冈区", "南郊区");
            put("余江区", "余江县");
            put("马龙区", "马龙县");
            put("光明区", "宝安区");
            put("怀仁区", "怀仁县");
            put("彬州市", "彬县");
            put("海安市", "海安县");
            put("漠河市", "漠河县");
            put("京山市", "京山县");
            put("济阳区", "济阳县");
            put("潞州区", "城区");
            put("上党区", "长治县");
            put("屯留区", "屯留县");
            put("潞城区", "潞城市");
            put("滦州市", "滦县");
            put("潜山市", "潜山县");
            put("邹平市", "邹平县");
            put("荔浦市", "荔浦县");
            put("兴仁市", "兴仁县");
            put("水富市", "水富县");
            put("华亭市", "华亭县");
            put("积石山县", "积石山保安族东乡族撒拉族自治县");
            put("元江县", "元江哈尼族彝族傣族自治县");
            put("双江县", "双江拉祜族佤族布朗族傣族自治县");
            put("孟连县", "孟连傣族拉祜族佤族自治县");
            put("镇沅县", "镇沅彝族哈尼族拉祜族自治县");
            put("大柴旦行政委员会", "海西蒙古族藏族自治州直辖");
            put("冷湖行政委员会", "海西蒙古族藏族自治州直辖");
            put("茫崖行政委员会", "海西蒙古族藏族自治州直辖");
            put("上饶县", "广信区");
            put("达孜区", "达孜县");
            put("色尼区", "那曲县");
        }
    };


    public static List<BusinessArea> determineAreaByCityId(Double lon, Double lat, int cityId, int radius, CoordinateSystem coordSys) {
        if (cityId == -1) {
            return Collections.emptyList();
        }
        Tuple2<Double, Double> tuple2 = GeoUtils.toGCJ02(lon, lat, coordSys);
        Location location = new Location(tuple2.get0(), tuple2.get1());
        List<BusinessAreaData> businessAreaDataList = cityBusinessArea.getOrDefault(cityId, Collections.emptyList());

        // todo 可以被优化，整个城市过滤太粗糙了
        return businessAreaDataList.stream()
                .map(
                        s -> new BusinessArea(s.name, s.areaCode,
                                GeoUtils.distance(s.center, location).intValue()
                        ))
                .filter(businessArea -> businessArea.distance <= radius)
                .collect(Collectors.toList());

    }

    public static int determineAdminCode(double lonIn, double latIn, CoordinateSystem coordSys) {
        if (coordSys == null) {
            coordSys = CoordinateSystem.GCJ02;
        }
        Tuple2<Double, Double> gcj02LonLat = GeoUtils.toGCJ02(lonIn, latIn, coordSys);
        Double lon = gcj02LonLat.get0();
        Double lat = gcj02LonLat.get1();

        S2LatLng s2LatLng = S2LatLng.fromDegrees(lat, lon);
        long id = S2CellId.fromLatLng(s2LatLng).parent(GeoTransImpl.min_level).id();
        long id2 = S2CellId.fromLatLng(s2LatLng).parent(GeoTransImpl.min_level - 2).id();

        if (GeoUtils.outOfChina(lon, lat)) {
            return -1;
        }
        if (boundaryAdminCell.containsKey(id)) {
            return boundaryAdminCell.getOrDefault(id, -1);
        }
        if (boundaryAdminCell.containsKey(id2)) {
            return boundaryAdminCell.getOrDefault(id2, -1);
        }

        Set<Long> keys = new HashSet<>();
        //必须大于2000m，否则会出现格子半径过小选择错误问题
        int maxLevel = 2000;
        while (keys.isEmpty() && maxLevel < 200000) {
            keys = S2Utils.getCellId(s2LatLng, maxLevel, GeoTransImpl.min_level)
                    .stream()
                    .flatMap((Function<Long, Stream<Long>>) aLong ->
                            boundaryIndex.getOrDefault(aLong, Collections.emptyList()).stream())
                    .collect(Collectors.toSet());
            maxLevel = maxLevel * 2;
        }

        if (keys.isEmpty()) {
            return -1;
        }


        List<Tuple2<Tuple4<Tuple2<Double, Double>, Tuple2<Double, Double>, Integer, Boolean>, Double>> lines1 =
                keys.stream()
                        .map(aLong -> Tuples.of(aLong, new S2CellId(aLong).toLatLng().getEarthDistance(s2LatLng)))
                        .sorted(Comparator.comparing(Tuple2::get1))
                        .limit(5)
                        .flatMap((Function<Tuple2<Long, Double>, Stream<Tuple4<Long, Long, Integer, Boolean>>>)
                                startPoint -> boundaryData.getOrDefault(startPoint.get0(), Collections.emptyList())
                                        .stream()
                                        .flatMap((Function<Tuple3<Long, Integer, Integer>, Stream<Tuple4<Long, Long, Integer, Boolean>>>)
                                                value -> {
                                                    List<Tuple4<Long, Long, Integer, Boolean>> s = Arrays.asList(
                                                            Tuples.of(startPoint.get0(), value.get0(), value.get1(), true),
                                                            Tuples.of(startPoint.get0(), value.get0(), value.get2(), false)
                                                    );
                                                    return s.stream();
                                                }))
                        .map(line -> {
                            S2LatLng start = new S2CellId(line.get0()).toLatLng();
                            S2LatLng end = new S2CellId(line.get1()).toLatLng();
                            Double dis = LineUtils.pointToLineDis(start.lngDegrees(), start.latDegrees(), end.lngDegrees(), end.latDegrees(), lon, lat);

                            return Tuples.of(Tuples.of(Tuples.of(start.lngDegrees(), start.latDegrees()),
                                    Tuples.of(end.lngDegrees(), end.latDegrees()),
                                    line.get2(), line.get3()
                            ), dis);
                        })
                        .collect(Collectors.toList());

        // 取出所有距离最短的线段
        Double minDis = lines1.stream().map(tuple4DoubleTuple2 -> tuple4DoubleTuple2.get1()).min(Double::compareTo).get();

        List<Tuple4<Tuple2<Double, Double>, Tuple2<Double, Double>, Integer, Boolean>> lines =
                lines1.stream()
                        .filter(s -> Objects.equals(s.get1(), minDis))
                        .map(tuple2 -> tuple2.get0())
                        .collect(Collectors.groupingBy(Tuple4::get0))
                        .values()
                        .stream()
                        .max(Comparator.comparingInt(List::size))
                        .get();


        if (lines.size() == 1) {
            // 国内海外边界
            Tuple4<Tuple2<Double, Double>, Tuple2<Double, Double>, Integer, Boolean> line1 = lines.get(0);
            Tuple2<Double, Double> start = line1.get0();
            Tuple2<Double, Double> end = line1.get1();
            // 三点用行列式判断旋转方向
            double angle = (start.get0() - lon) * (end.get1() - lat) - (end.get0() - lon) * (start.get1() - lat);
            if ((angle < 0) == line1.get3())
                return line1.get2();
            return -1;
        }
        if (lines.size() == 2) {
            // 两条线段，如果终点不同，则一定是国内和海外，并且点到线段距离最短点为起点，终点相同，则为国内两个区域边界
            Tuple4<Tuple2<Double, Double>, Tuple2<Double, Double>, Integer, Boolean> line1 = lines.get(0);
            Tuple4<Tuple2<Double, Double>, Tuple2<Double, Double>, Integer, Boolean> line2 = lines.get(1);

            // 终点相同，为国内两个相邻区域，终点不同，为国界线
            Tuple2<Double, Double> start;
            if (line1.get1().equals(line2.get1())) {
                start = line1.get0();
            } else {
                start = line2.get1();
            }

            Tuple2<Double, Double> end = line1.get1();

            // 三点用行列式判断旋转方向
            double angle = (start.get0() - lon) * (end.get1() - lat) - (end.get0() - lon) * (start.get1() - lat);
            if ((angle < 0) == line1.get3())
                return line1.get2();
            else if (line1.get1().equals(line2.get1()) && !line1.get3().equals(line2.get3()))
                return line2.get2();
            else return -1;
        }
        //多区域顶点 判断


        return lines.stream().collect(Collectors.groupingBy(Tuple4::get2)).entrySet().stream().map(s -> {

            Tuple4<Tuple2<Double, Double>, Tuple2<Double, Double>, Integer, Boolean> line1 =
                    s.getValue().stream().filter(Tuple4::get3).findFirst().get();
            Tuple4<Tuple2<Double, Double>, Tuple2<Double, Double>, Integer, Boolean> line2 =
                    s.getValue().stream().filter(it -> !it.get3()).findFirst().get();

            Tuple2<Double, Double> start = line2.get1();
            Tuple2<Double, Double> end = line1.get1();
            Tuple2<Double, Double> point = line1.get0();

            Double dis1 = LineUtils.lineDis(start.get0(), start.get1(), point.get0(), point.get1());
            Double dis2 = LineUtils.lineDis(end.get0(), end.get1(), point.get0(), point.get1());
            if (dis1 > dis2)
                start = Tuples.of(point.get0() + dis2 / dis1 * (start.get0() - point.get0()), point.get1() + dis2 / dis1 * (start.get1() - point.get1()));
            else
                end = Tuples.of(point.get0() + dis1 / dis2 * (end.get0() - point.get0()), point.get1() + dis1 / dis2 * (end.get1() - point.get1()));
            double angle = (start.get0() - lon) * (end.get1() - lat) - (end.get0() - lon) * (start.get1() - lat);
            return Tuples.of(s.getKey(), angle);
        }).min((o1, o2) -> o1.get1().compareTo(o2.get1())).get().get0();
    }

    public static String getCityLevel(String adcode_or_name) {
        return cityLevelData.getOrDefault(adcode_or_name, "未知");
    }

    public static String getCityLevel(Admin admin) {
        return getCityLevel(String.valueOf(admin.cityCode));
    }

    public static AdminNode normalizeName(int adcode) {
        AdminNode adminNode = adminData.get(adcode);
        if (adminNode != null) {
            return adminNode;
        }
        return streetData.get(adcode);
    }

    public static List<AdminNode> normalizeName(String name, DistrictLevel level, boolean isFullMatch) {
        return adminData.values().stream()
                .filter(it -> it.level.equals(level))
                .filter(s -> isFullMatch ? s.name.equals(name) : s.shortName.contains(name) || s.name.contains(name))
                .collect(Collectors.toList());
    }


    public static List<Admin> normalizeName(String provinceIn, String cityIn, String districtIn, String streetIn, boolean isFullMatch) {
        String province = provinceIn == null || provinceIn.equals("未知") ? "" : provinceIn;
        String city = cityIn == null || cityIn.equals("未知") ? "" : cityNameMap.getOrDefault(cityIn, cityIn);
        String district = districtIn == null || districtIn.equals("未知") ? "" : districtNameMap.getOrDefault(districtIn, districtIn);
        String street = streetIn == null || streetIn.equals("未知") ? "" : streetIn;


        List<AdminNode> adminNodes
                = buildAdminNode(city, district, street, province, isFullMatch);


        return adminNodes.stream().map(admin -> {
            if (admin.level == DistrictLevel.Province) {
                return Admin.createProvince(admin.name, admin.id, admin.center);
            }
            if (admin.level == DistrictLevel.City) {
                AdminNode province1 = adminData.get(admin.parentId);
                return Admin.createCity(province1.name, admin.name, province1.id, admin.id, admin.center);
            }
            if (admin.level == DistrictLevel.District) {
                AdminNode city1 = adminData.get(admin.parentId);
                AdminNode province1 = (city1.level == DistrictLevel.City) ? adminData.get(city1.parentId) : city1;
                return Admin.createDistrict(province1.name, city1.name, admin.name, province1.id, city1.id, admin.id, admin.center);
            }
            if (admin.level == DistrictLevel.Street) {
                AdminNode district1 = adminData.get(admin.parentId);
                AdminNode city1 = (district1.level == DistrictLevel.District) ? adminData.get(district1.parentId) : district1;
                AdminNode province1 = (city1.level == DistrictLevel.City) ? adminData.get(city1.parentId) : city1;
                return Admin.createStreet(province1.name, city1.name, district1.name, admin.name, province1.id, city1.id, district1.id, admin.id, admin.center);
            }
            return Admin.createOversea();
        }).collect(Collectors.toList());
    }


    private static List<AdminNode> buildAdminNode(String city, String district, String street, String province, boolean isFullMatch) {
        List<AdminNode> provinceAd = adminData.values().stream().filter(s -> s.level == (DistrictLevel.Province)).filter(s -> StringUtils.isEmpty(province) || s.name.equals(province) || (!isFullMatch && s.shortName.equals(province))).collect(Collectors.toList());

        if (StringUtils.isEmpty(city) && StringUtils.isEmpty(district) && StringUtils.isEmpty(street)) {
            return provinceAd;
        }
        List<AdminNode> cityAd = provinceAd.stream()
                .flatMap(s -> s.children.stream().map(adCode -> adminData.getOrDefault(adCode, streetData.get(adCode))))
                .filter(s ->
                        // todo 原文这里的逻辑组合太复杂了，先保持原样看下情况
                        (!(s.level == (DistrictLevel.City)))
                                || (s.level == (DistrictLevel.City))
                                && (
                                StringUtils.isEmpty(city)
                                        || s.name.equals(city)
                                        || (!isFullMatch && s.shortName.equals(city))
                        )
                )
                .collect(Collectors.toList());
        if (cityAd.isEmpty()) {
            return provinceAd;
        }

        if (StringUtils.isEmpty(district) && StringUtils.isEmpty(street)) {
            return cityAd.stream()
                    .filter(s -> s.level == (DistrictLevel.Province) || s.level == (DistrictLevel.City))
                    .collect(Collectors.toList());
        }

        List<AdminNode> districtAd = cityAd.stream().flatMap(s -> {
                    if (s.level == (DistrictLevel.City) && StringUtils.isEmpty(street)) {
                        return s.children.stream().map(adCode -> adminData.get(adCode));
                    } else if (s.level == (DistrictLevel.City)) {
                        return s.children.stream().map(adCode -> adminData.getOrDefault(adCode, streetData.get(adCode)));
                    } else {
                        return Stream.of(s);
                    }
                })
                .filter(s -> s != null)
                .filter(s -> (s.level != (DistrictLevel.District)) ||
                        s.level == (DistrictLevel.District) &&
                                (StringUtils.isEmpty(district) || s.name.equals(district) || (!isFullMatch && s.shortName.equals(district)))
                ).collect(Collectors.toList());

        if (districtAd.isEmpty()) {
            return cityAd;
        }

        if (StringUtils.isEmpty(street)) {
            return
                    districtAd.stream().filter(
                            s -> s.level == (DistrictLevel.Province) || s.level == (DistrictLevel.City) || s.level == (DistrictLevel.District)
                    ).collect(Collectors.toList());
        }

        List<AdminNode> streetAd = districtAd.stream().flatMap(s -> {
                    if (s.level == (DistrictLevel.District)) {
                        return s.children.stream().map(adCode -> streetData.get(adCode));
                    } else {
                        return Stream.of(s);
                    }
                }).filter(s -> s.name.equals(street) || (!isFullMatch && s.shortName.equals(street)))
                .collect(Collectors.toList());

        if (streetAd.isEmpty()) {
            return districtAd;
        }
        return streetAd;
    }


    public static Admin determineAdmin(double lon, double lat, boolean needStreet, CoordinateSystem coordSys) {
        Tuple2<Double, Double> wgs84LonLat = GeoUtils.toWGS84(lon, lat, coordSys);
        int code = GeoTransImpl.determineAdminCode(wgs84LonLat.get0(), wgs84LonLat.get1(), null);
        if (code == -1) {
            return Admin.createOversea();
        }
        AdminNode district = adminData.get(code);
        AdminNode city = (district.level == DistrictLevel.District) ? adminData.get(district.parentId) : district;
        AdminNode province = (city.level == DistrictLevel.City) ? adminData.get(city.parentId) : city;

        int streetCode = 0;
        String streetName = "";

        if (needStreet) {
            List<Integer> children = district.children;
            if (children != null && !children.isEmpty()) {
                AdminNode minAdminNode = null;
                double minDistance = Double.MAX_VALUE;
                for (Integer integer : children) {
                    AdminNode adminNode = streetData.get(integer);
                    if (minAdminNode == null) {
                        minAdminNode = adminNode;
                        minDistance = GeoUtils.distance(adminNode.center, new Location(wgs84LonLat.get0(), wgs84LonLat.get1()));
                        continue;
                    }
                    Double distance = GeoUtils.distance(adminNode.center, new Location(wgs84LonLat.get0(), wgs84LonLat.get1()));
                    if (distance < minDistance) {
                        minAdminNode = adminNode;
                        minDistance = distance;
                    }
                }
                streetCode = minAdminNode.id;
                streetName = minAdminNode.name;
            }


        }

        if (streetCode > 0) {
            return Admin.createStreet(province.name, city.name, district.name, streetName, province.id, city.id, district.id, streetCode, district.center);
        }
        return Admin.createDistrict(province.name, city.name, district.name, province.id, city.id, district.id, district.center);
    }
}
