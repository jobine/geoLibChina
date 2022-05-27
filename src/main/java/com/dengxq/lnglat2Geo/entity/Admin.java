package com.dengxq.lnglat2Geo.entity;

import com.dengxq.lnglat2Geo.utils.AdminUtils;

import java.util.Objects;

public class Admin {

    private static final String CHINA_NAME = "中国";
    private static final String CHINA_ID = "CN";
    private static final String OVERSEA_NAME_VAL = "海外";
    private static final String UNKNOWN_NAME_VAL = "未知";
    private static final int UNKNOWN_ID_VAL = -1;
    private static final Location UNKNOWN_LOCATION_VAL = null;


    public String country;
    public String province;
    public String city;
    public String district;
    public String town;
    public String level;
    public String countryCode;
    public int provinceCode;
    public int cityCode;
    public int districtCode;
    public int townCode;
    public Location center;

    public Admin(String country, String province, String city, String district, String town, String level, String countryCode, int provinceCode, int cityCode, int districtCode, int townCode, Location center) {
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.town = town;
        this.level = level;
        this.countryCode = countryCode;
        this.provinceCode = provinceCode;
        this.cityCode = cityCode;
        this.districtCode = districtCode;
        this.townCode = townCode;
        this.center = center;
    }


    public Admin() {
    }

    public boolean hasCenter() {
        return center != Admin.UNKNOWN_LOCATION_VAL;
    }

    public boolean hasProvince() {
        return !Objects.equals(province, Admin.UNKNOWN_NAME_VAL);
    }


    public boolean hasCity() {
        return !Objects.equals(city, Admin.UNKNOWN_NAME_VAL);
    }


    public boolean hasDistrict() {
        return !Objects.equals(district, Admin.UNKNOWN_NAME_VAL);
    }


    public boolean hasCityId() {
        return cityCode != Admin.UNKNOWN_ID_VAL;
    }


    public boolean hasDistrictId() {
        return districtCode != Admin.UNKNOWN_ID_VAL;
    }


    public boolean hasTown() {
        return !Objects.equals(town, Admin.UNKNOWN_NAME_VAL);
    }


    public String shortProvince() {
        return AdminUtils.shortProvince(province);
    }

    public String shortCity() {
        return AdminUtils.shortCity(city);
    }

    public Admin toShort() {
        return new Admin(country,
                AdminUtils.shortProvince(province),
                AdminUtils.shortCity(city),
                AdminUtils.shortDistrict(district),
                AdminUtils.shortStreet(town),
                level, countryCode, provinceCode, cityCode, districtCode, townCode, center
        );
    }
    // def toNameString: String = s"$country${if (hasProvince) province else ""}
    // ${if (hasCity) city else ""}
    // ${if (hasDistrict) district else ""}
    // ${if (hasTown) town else ""}"

    public String toNameString() {
        StringBuilder sb = new StringBuilder(country);
        if (hasProvince()) {
            sb.append(province);
        }
        if (hasCity()) {
            sb.append(city);
        }
        if (hasDistrict()) {
            sb.append(district);
        }
        if (hasTown()) {
            sb.append(town);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toNameString();
    }

    public static Admin createOversea() {
        return new Admin(OVERSEA_NAME_VAL,
                OVERSEA_NAME_VAL,
                OVERSEA_NAME_VAL,
                OVERSEA_NAME_VAL,
                OVERSEA_NAME_VAL,
                AdminLevel.Oversea,
                "",
                UNKNOWN_ID_VAL,
                UNKNOWN_ID_VAL,
                UNKNOWN_ID_VAL,
                UNKNOWN_ID_VAL,
                UNKNOWN_LOCATION_VAL);
    }

    public static Admin createCountry(String country, String countryID, Location center) {
        return new Admin(country,
                UNKNOWN_NAME_VAL,
                UNKNOWN_NAME_VAL,
                UNKNOWN_NAME_VAL,
                UNKNOWN_NAME_VAL,
                AdminLevel.Country,
                countryID,
                UNKNOWN_ID_VAL,
                UNKNOWN_ID_VAL,
                UNKNOWN_ID_VAL,
                UNKNOWN_ID_VAL,
                center);
    }

    public static Admin createProvince(String province, int provinceId, Location center) {
        return new Admin(
                CHINA_NAME,
                province,
                UNKNOWN_NAME_VAL,
                UNKNOWN_NAME_VAL,
                UNKNOWN_NAME_VAL,
                AdminLevel.Province,
                CHINA_ID,
                provinceId,
                UNKNOWN_ID_VAL,
                UNKNOWN_ID_VAL,
                UNKNOWN_ID_VAL,
                center
        );
    }

    public static Admin createCity(String province, String city, int provinceId, int cityId, Location center) {
        return new Admin(
                CHINA_NAME,
                province,
                city,
                UNKNOWN_NAME_VAL,
                UNKNOWN_NAME_VAL,
                AdminLevel.City,
                CHINA_ID,
                provinceId,
                cityId,
                UNKNOWN_ID_VAL,
                UNKNOWN_ID_VAL,
                center
        );
    }

    public static Admin createProvincialCity(String province, String city, int provinceId, int cityId, Location center) {
        return new Admin(
                CHINA_NAME,
                province,
                city,
                city,
                UNKNOWN_NAME_VAL,
                AdminLevel.ProvincialCity,
                CHINA_ID,
                provinceId,
                cityId,
                cityId,
                UNKNOWN_ID_VAL,
                center
        );
    }

    public static Admin createDistrict(String province, String city, String district,
                                       int provinceId, int cityId, int districtId, Location center) {
        return new Admin(
                CHINA_NAME,
                province,
                city,
                district,
                UNKNOWN_NAME_VAL,
                AdminLevel.District,
                CHINA_ID,
                provinceId,
                cityId,
                districtId,
                UNKNOWN_ID_VAL,
                center
        );
    }

    public static Admin createStreet(String province, String city, String district, String town,
                                     int provinceId, int cityId, int districtId, int streetId, Location center) {
        return new Admin(
                CHINA_NAME,
                province,
                city,
                district,
                town,
                AdminLevel.Street,
                CHINA_ID,
                provinceId,
                cityId,
                districtId,
                streetId,
                center
        );
    }

}
