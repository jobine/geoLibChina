package com.virjar.geolib.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeoAdmin {
    private String province;
    private String city;
    private String district;

    private int provinceCode;
    private int cityCode;
    private int districtCode;

    private float centerLng;
    private float centerLat;


    public static GeoAdmin defaultOverSea = new GeoAdmin(
            "海外", "海外", "海外",
            -1, -1, -1,
            0F, 0F);
}
