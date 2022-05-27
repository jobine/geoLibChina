package com.dengxq.lnglat2Geo.entity;

public class BusinessArea {
    public String name;
    public Integer areaCode;
    public Integer distance;

    public BusinessArea(String name, Integer areaCode, Integer distance) {
        this.name = name;
        this.areaCode = areaCode;
        this.distance = distance;
    }
}
