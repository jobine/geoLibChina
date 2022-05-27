package com.dengxq.lnglat2Geo.entity;

import lombok.Data;

import java.util.List;

@Data
public class AdminBoundary {
    public int code;
    public List<List<Long>> boundary;

}
