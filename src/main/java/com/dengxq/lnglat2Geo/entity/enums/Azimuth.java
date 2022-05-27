package com.dengxq.lnglat2Geo.entity.enums;

import lombok.Getter;

public enum Azimuth {
    North(0),
    NorthEast(45),
    East(90),
    SouthEast(135),
    South(180),
    SouthWest(225),
    West(270),
    NorthWest(315);

    @Getter
    private int code;

    Azimuth(int value) {
        this.code = value;
    }
}
