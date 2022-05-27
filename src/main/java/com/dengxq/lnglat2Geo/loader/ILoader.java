package com.dengxq.lnglat2Geo.loader;

import lombok.Setter;

import java.io.File;

/**
 * 数据加载和dump的顶层，支持从json文件和bin文件中加载，以及dump到json和bin文件中
 * 实现可视化格式和二进制格式的相互转换。
 * <p>
 * 我们导入和更新数据来自于可视化json，他易于编辑，但是文本文件提及太大。
 * 线上使用二进制格式，能快速加载和解析，但是他没办法编辑和直接查看
 */
public interface ILoader {
    /**
     * 从一个class path路径加载离线数据
     *
     * @return 数据
     */
    GeoData load();

    /**
     * 将离线数据dump到文件中，并且这个文件可以被load再次识别
     *
     * @param path    文件
     * @param geoData 数据结构对象
     */
    void dump(File path, GeoData geoData);

    class Storage {
        private static GeoData geoData = null;
        @Setter
        private static ILoader loader = new BinLoader();

        public static void clear() {
            geoData = null;
        }

        public static GeoData getOrLoad() {
            if (geoData != null) {
                return geoData;
            }
            synchronized (Storage.class) {
                if (geoData != null) {
                    return geoData;
                }
                geoData = loader.load();
            }
            return geoData;
        }
    }
}
