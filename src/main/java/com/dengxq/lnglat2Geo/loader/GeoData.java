package com.dengxq.lnglat2Geo.loader;

import com.dengxq.lnglat2Geo.entity.AdminBoundary;
import com.dengxq.lnglat2Geo.entity.AdminNode;
import com.dengxq.lnglat2Geo.entity.BusinessAreaGroup;
import com.dengxq.lnglat2Geo.entity.CellAdmin;
import com.dengxq.lnglat2Geo.utils.Utils;
import com.speedment.common.tuple.Tuple3;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 离线数据包，包括行政区折线图、行政等级划分、商圈、城市编码、城市级别等
 * <p>
 * 他可以由二进制或者json两种格式转换加载，并且可以序列化到二进制和json两种格式，
 * 其中二进制格式将会伴随发布包发布，作为默认的离线数据资源，
 * json格式则主要用于调试和升级二进制格式使用
 */
@Data
public class GeoData {

    /**
     * 行政区域数据
     */
    private List<AdminNode> adminData;

    /**
     * 街道数据
     */
    private List<AdminNode> streetData;

    /**
     * 多边形边界线，规定每个区域的范围
     */
    private List<AdminBoundary> adminBoundaries;


    /**
     * 商圈
     */
    private List<BusinessAreaGroup> areaGroups;

    /**
     * s2到行政编码的缓存，他不是必须的，但是可以提高计算速度
     */
    private List<CellAdmin> cellAdmins;

    /**
     * 城市级别
     */
    private Map<String, String> cityLevel;

    /**
     * 国家编码
     */
    private Map<String, String> countryCode;

    // 以下为extension数据，他是根据上面的字段计算而来，因为他们的计算比较耗时，我们把计算好的数据缓存到这里，
    // json 格式不序列化这些字段，bin需要序列化这些字段
    private Map<Long, List<Tuple3<Long, Integer, Integer>>> runtimeBoundaryData;

    private Map<Long, List<Long>> runtimeBoundaryIndex;

    private Map<Long, Integer> runtimeBoundaryAdminCell;

    public void clearRuntime() {
        runtimeBoundaryData = null;
        runtimeBoundaryIndex = null;
        runtimeBoundaryAdminCell = null;
    }

    public Map<Long, List<Tuple3<Long, Integer, Integer>>> getOrCreateRuntimeBoundaryData() {
        if (runtimeBoundaryData == null) {
            runtimeBoundaryData = Utils.parseBoundaryData(adminBoundaries);
        }
        return runtimeBoundaryData;
    }

    public Map<Long, List<Long>> getOrCreateRuntimeBoundaryIndex() {
        if (runtimeBoundaryIndex == null) {
            runtimeBoundaryIndex = Utils.buildBoundaryIndex(getOrCreateRuntimeBoundaryData());
        }
        return runtimeBoundaryIndex;
    }

    public Map<Long, Integer> getOrCreateRuntimeBoundaryAdminCell() {
        if (runtimeBoundaryAdminCell == null) {
            runtimeBoundaryAdminCell = Utils.buildBoundaryAdminCell(cellAdmins);
        }
        return runtimeBoundaryAdminCell;
    }
}
