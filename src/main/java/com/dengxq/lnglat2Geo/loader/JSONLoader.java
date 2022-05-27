package com.dengxq.lnglat2Geo.loader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dengxq.lnglat2Geo.entity.AdminBoundary;
import com.dengxq.lnglat2Geo.entity.AdminNode;
import com.dengxq.lnglat2Geo.entity.BusinessAreaGroup;
import com.dengxq.lnglat2Geo.entity.CellAdmin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 从json资源中加载数据，生产不会使用它
 */
public class JSONLoader implements ILoader {

    private static String loadResource(String key) {
        key = "json/" + key;
        try (InputStream inputStream = JSONLoader.class.getClassLoader().getResourceAsStream(key)) {
            if (inputStream == null) {
                throw new RuntimeException("resource not exist: " + key);
            }
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String FILE_ADMIN_DATA = "adminData.json";
    private static final String FILE_STREET_DATA = "streetData.json";
    private static final String FILE_CELL_ADMINS = "cellAdmins.json";
    private static final String FILE_ADMIN_BOUNDARIES = "adminBoundaries.json";
    private static final String FILE_AREA_GROUPS = "areaGroups.json";
    private static final String FILE_CITY_LEVEL_DATA = "cityLevelData.json";
    private static final String FILE_COUNTRY_CODE_DATA = "countryCode.json";

    @SuppressWarnings("unchecked")
    @Override
    public GeoData load() {
        GeoData geoData = new GeoData();
        geoData.setAdminData(JSON.parseArray(loadResource(FILE_ADMIN_DATA), AdminNode.class));
        geoData.setStreetData(JSON.parseArray(loadResource(FILE_STREET_DATA), AdminNode.class));

        geoData.setAdminBoundaries(JSON.parseArray(loadResource(FILE_ADMIN_BOUNDARIES), AdminBoundary.class));
        geoData.setAreaGroups(JSON.parseArray(loadResource(FILE_AREA_GROUPS), BusinessAreaGroup.class));
        geoData.setCellAdmins(JSON.parseArray(loadResource(FILE_CELL_ADMINS), CellAdmin.class));

        geoData.setCityLevel(JSON.parseObject(loadResource(FILE_CITY_LEVEL_DATA), Map.class));
        geoData.setCountryCode(JSON.parseObject(loadResource(FILE_COUNTRY_CODE_DATA), Map.class));
        return geoData;
    }

    @Override
    public void dump(File path, GeoData geoData) {
        try {
            // 在测试环境，我们大概率不希望所有数据都存储在一个大json中，所以这里我们分割多个文件存储
            // 二进制不具备可读性，故合并所有数据在同一个文件
            dumpObject(new File(path, FILE_ADMIN_DATA), geoData.getAdminData());
            dumpObject(new File(path, FILE_STREET_DATA), geoData.getStreetData());
            dumpObject(new File(path, FILE_ADMIN_BOUNDARIES), geoData.getAdminBoundaries());
            dumpObject(new File(path, FILE_AREA_GROUPS), geoData.getAreaGroups());
            dumpObject(new File(path, FILE_CELL_ADMINS), geoData.getCellAdmins());
            dumpObject(new File(path, FILE_CITY_LEVEL_DATA), geoData.getCityLevel());
            dumpObject(new File(path, FILE_COUNTRY_CODE_DATA), geoData.getCountryCode());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void dumpObject(File file, Object obj) throws IOException {
        FileUtils.writeStringToFile(file, JSON.toJSONString(obj, SerializerFeature.PrettyFormat));
    }
}
