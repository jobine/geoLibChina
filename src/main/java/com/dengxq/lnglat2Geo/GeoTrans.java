package com.dengxq.lnglat2Geo;

import com.dengxq.lnglat2Geo.entity.*;
import com.dengxq.lnglat2Geo.entity.enums.CoordinateSystem;
import com.dengxq.lnglat2Geo.entity.enums.DistrictLevel;
import com.dengxq.lnglat2Geo.utils.GeoUtils;
import com.speedment.common.tuple.Tuple2;

import java.util.List;
import java.util.Map;

public class GeoTrans {


    /**
     * 判断经纬度的行政区划
     *
     * @param lon        经度
     * @param lat        纬度
     * @param needStreet 是否需要街道信息
     * @param coordSys   输入经纬度的坐标系
     * @return 行政区划
     */
    public static Admin determineAdmin(Double lon, Double lat, CoordinateSystem coordSys, Boolean needStreet) {
        if (needStreet == null) {
            needStreet = true;
        }
        return GeoTransImpl.determineAdmin(lon, lat, needStreet, coordSys);
    }

    /**
     * 给出附近的所有商圈信息
     *
     * @param lon        经度
     * @param lat        纬度
     * @param radius     需要商圈的半径
     * @param coordSys   输入经纬度的坐标系 CoordinateSystem
     * @param needStreet 是否需要返回行政区划的街道信息
     * @return
     */
    public static BusinessAreaInfo aroundBusinessAreas(Double lon, Double lat, Integer radius, CoordinateSystem coordSys, Boolean needStreet) {
        if (radius == null) {
            radius = 4000;
        }
        if (needStreet == null) {
            needStreet = true;
        }
        Tuple2<Double, Double> gcj02LonLat = GeoUtils.toGCJ02(lon, lat, coordSys);
        Admin admin = determineAdmin(gcj02LonLat.get0(), gcj02LonLat.get1(), CoordinateSystem.GCJ02, needStreet);
        //return GeoTransImpl.determineAreaByAdmin(gcj02LonLat._1, gcj02LonLat._2, admin, radius);
        return new BusinessAreaInfo(admin, GeoTransImpl.determineAreaByCityId(lon, lat, admin.cityCode, radius, CoordinateSystem.GCJ02));
    }

    /**
     * 给出附近的所有商圈信息
     *
     * @param lon      经度
     * @param lat      纬度
     * @param radius   需要商圈的半径
     * @param coordSys 输入经纬度的坐标系 CoordinateSystem
     * @param cityID   输入城市adcode
     * @return
     */
    public static List<BusinessArea> aroundBusinessAreasByCityID(
            Double lon, Double lat, Integer radius, CoordinateSystem coordSys, Integer cityID) {
        if (radius == null) {
            radius = 4000;
        }
        return GeoTransImpl.determineAreaByCityId(lon, lat, cityID, radius, coordSys);
    }

    /**
     * 获取城市级别
     *
     * @param adcodeOrName 城市adcode或者城市名
     * @return 城市级别
     */
    public static String getCityLevel(String adcodeOrName) {
        return GeoTransImpl.getCityLevel(adcodeOrName);
    }

    /**
     * 根据地区code返回规范数据
     *
     * @param adcode 地区code
     * @return
     */
    public static AdminNode normalizeName(int adcode) {
        return GeoTransImpl.normalizeName(adcode);
    }

    /**
     * 根据地区name返回规范化的地区信息
     *
     * @return 规范化的地区信息
     */
    public static java.util.List<AdminNode> normalizeName(String name, DistrictLevel level, Boolean isFullMatch) {
        return GeoTransImpl.normalizeName(name, level, isFullMatch);
    }

    /**
     * 根据所有信息返回规范化的地区信息
     *
     * @param province    省名 可为空
     * @param city        城市名 可为空
     * @param district    区县名 可为空
     * @param street      街道名 可为空
     * @param isFullMatch 所有输入区域是简称还是全名
     * @return 规范化的地区信息，可能有多个或不存在
     */
    public static java.util.List<Admin> normalizeName(String province, String city, String district,
                                                      String street, Boolean isFullMatch) {
        province = trimToEmpty(province);
        city = trimToEmpty(city);
        district = trimToEmpty(district);
        street = trimToEmpty(street);
        if (isFullMatch == null) {
            isFullMatch = false;
        }
        return GeoTransImpl.normalizeName(province, city, district, street, isFullMatch);
    }

    private static String trimToEmpty(String input) {
        if (input == null) {
            return "";
        }
        return input;
    }

    /**
     * 获取所有行政区划数据
     *
     * @return 所有行政区划数据，不包含街道
     */
    public static Map<Integer, AdminNode> adminData() {
        return GeoTransImpl.adminData;
    }

    public static Map<String, String> countryCode() {
        return GeoTransImpl.countryCode;
    }

//    public static districtBoundary():Map[Int,List[List[Long]]]=
//
//    {
//        AdminDataProvider.AdminLoader.loadBoundarySrc.map(s = > (s.code, s.boundary))
//      .toMap
//    }
}
