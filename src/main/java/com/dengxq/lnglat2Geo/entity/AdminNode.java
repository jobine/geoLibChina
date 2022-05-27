package com.dengxq.lnglat2Geo.entity;

import com.dengxq.lnglat2Geo.entity.enums.DistrictLevel;

import java.util.ArrayList;
import java.util.List;

public class AdminNode {
    public int id;
    public String name;
    public String shortName;
    public Location center;
    public DistrictLevel level;
    public int parentId;
    public List<Integer> children = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public Location getCenter() {
        return center;
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    public DistrictLevel getLevel() {
        return level;
    }

    public void setLevel(DistrictLevel level) {
        this.level = level;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public List<Integer> getChildren() {
        return children;
    }

    public void setChildren(List<Integer> children) {
        this.children = children;
    }
}
