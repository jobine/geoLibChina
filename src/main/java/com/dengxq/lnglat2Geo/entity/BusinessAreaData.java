package com.dengxq.lnglat2Geo.entity;

import lombok.Data;

@Data
public class BusinessAreaData {
    //  商圈名称
    public String name;
    //商圈中心点
    public Location center;
    //areaCode 商圈ID
    public int areaCode;

}
