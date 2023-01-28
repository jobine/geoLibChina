package com.virjar.geolib.bean;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 行政区域边界定义，由多个多边形组成一个区域范围，如描述四川省的经纬度范围
 */
@Data
@RequiredArgsConstructor
public class Boundary {
    /**
     * 行政编码，如四川省
     */
    @NonNull
    private Integer code;

    /**
     * 多个多边形，请注意从标准上来说，一个行政区域可以是多个多边形，比如美国在太平洋有多个海岛
     * 当然在中国大陆不存在这个问题
     */
    private List<List<Long>> pylons = new ArrayList<>();
}
