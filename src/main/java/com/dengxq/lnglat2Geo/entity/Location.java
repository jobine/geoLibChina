package com.dengxq.lnglat2Geo.entity;

import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Location {
    public Double lng;
    public Double lat;
    private long id;

    public Location(Double lng, Double lat) {
        this.lng = lng;
        this.lat = lat;
        id = S2CellId.fromLatLng(S2LatLng.fromDegrees(lat, lng)).id();
    }

}
