package com.virjar.geolib.bean;

/**
 * 行政区域的一条边界线段
 */
public class BoundaryLine {
    public final int adminCode;
    // start - end 标记一个线段，他们都使用Google s2描述的一个经纬度点位
    public final long start;
    public final long end;

    public BoundaryLine(int adminCode, long start, long end) {
        this.adminCode = adminCode;
        this.start = start;
        this.end = end;
    }
}