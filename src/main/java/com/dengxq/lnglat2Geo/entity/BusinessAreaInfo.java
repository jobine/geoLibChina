package com.dengxq.lnglat2Geo.entity;

import java.util.List;

public class BusinessAreaInfo {
    public Admin admin;
    public List<BusinessArea> areas;

    public BusinessAreaInfo(Admin admin, List<BusinessArea> areas) {
        this.admin = admin;
        this.areas = areas;
    }
}
