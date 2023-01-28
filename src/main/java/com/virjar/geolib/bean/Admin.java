package com.virjar.geolib.bean;

import lombok.Data;

/**
 * 行政区域定义
 */
@Data
public class Admin implements Comparable<Admin> {
    private int id = -1;
    private int parentId = -1;
    private String name;
    private String shortName;
    private float centerLng;
    private float centerLat;
    private DistrictLevel level;

    @Override
    public int compareTo(Admin o) {
        return Integer.compare(id, o.id);
    }

    public enum DistrictLevel {
        Country("国家"),
        Province("省,自治区"),
        City("地级市"),
        District("区,县,县级市"),
        ;
        public final String desc;

        DistrictLevel(String desc) {
            this.desc = desc;
        }
    }
}
