package com.virjar.geolib;

import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.virjar.geolib.bean.Admin;
import com.virjar.geolib.bean.BoundaryLine;
import com.virjar.geolib.bean.GeoAdmin;
import com.virjar.geolib.bean.Location;
import com.virjar.geolib.core.AdminNormalizer;
import com.virjar.geolib.core.GeoDb;
import com.virjar.geolib.core.GeoUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GeoTrans {
    private static final String DB_RES_KEY = "geo-vj00.bin";
    private static final String DB_RES_ZIP_KEY = "geo-vj00.bin.zip";
    private static final String ADMIN_ADDRESS_CONFIG = "admin_address.properties";
    private static final GeoDb geoDb = openGeoDb();
    private static final AdminNormalizer adminNormalizer = new AdminNormalizer(geoDb);

    /**
     * 中文名称-行政区域对应表
     */
    private static final Map<String, Integer> normalizeNames = loadAddressData();


    /**
     * 给定任意一个行政编码，返回格式化的行政区域定义
     *
     * @param code 行政编码
     * @return 行政区域定义
     */
    public static Admin getAdmin(int code) {
        return geoDb.getAdmin(code);
    }

    public static List<Admin> allAdminList() {
        return geoDb.allAdminList();
    }

    /**
     * 给定任意一个地址描述，返回最匹配这个地址的行政区域定义，请注意对于某些重名地址，转换过程可能存在歧义，
     * 但你可以给定更加详细的地址描述，从而达到更加精确的匹配
     *
     * @param address 地址
     * @return 行政区域定义
     */
    public static Admin normalizeName(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        String key = GeoUtils.toHexString(address.getBytes(StandardCharsets.UTF_8));
        if (normalizeNames.containsKey(key)) {
            Integer adminCode = normalizeNames.get(key);
            if (adminCode == null) {
                return null;
            }
            return getAdmin(adminCode);
        }

        // 没有缓存记录，那么扫描整个admin库，再寻找最优匹配
        int adminCode = adminNormalizer.doNormalize(address);

        normalizeNames.put(key, adminCode);
        storeAddressData(normalizeNames);
        return getAdmin(adminCode);
    }

    /**
     * 给定某个行政节点，查询他的下一级节点列表
     *
     * @param code 行政编码
     * @return 下级新政列表
     */
    public static Collection<Admin> getChildren(int code) {
        return geoDb.getChildren(code);
    }

    /**
     * 给定经纬度，计算行政详情（同时包含省市区）
     *
     * @param lng 经度
     * @param lat 纬度
     */
    public static GeoAdmin resolveGeoAdmin(double lng, double lat) {
        int adminCode = resolveAdminCode(lng, lat);
        if (adminCode <= 0) {
            return GeoAdmin.defaultOverSea;
        }

        Admin admin = getAdmin(adminCode);
        if (admin == null) {
            return GeoAdmin.defaultOverSea;
        }

        Admin province = null, city = null, district = null;
        do {
            switch (admin.getLevel()) {
                case District:
                    district = admin;
                    break;
                case City:
                    city = admin;
                    break;
                case Province:
                    province = admin;
                    break;
            }
            admin = getAdmin(admin.getParentId());
        } while (admin != null);
        if (province == null && city == null && district == null) {
            return GeoAdmin.defaultOverSea;
        }
        Admin maxDeep = district != null ? district : (city != null ? city : province);
        return new GeoAdmin(province == null ? "未知" : province.getName(),
                city == null ? "未知" : city.getName(),
                district == null ? "未知" : district.getName(),
                province == null ? -1 : province.getId(),
                city == null ? -1 : city.getId(),
                district == null ? -1 : district.getId(),
                maxDeep.getCenterLng(), maxDeep.getCenterLat());
    }


    /**
     * 给定某个特定的经纬度，计算他的行政区域节点（区县级别）
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 行政编码，如在海外则返回-1
     */
    public static int resolveAdminCode(double lng, double lat) {
        if (GeoUtils.outOfChina(lng, lat)) {
            return -1;
        }
        S2LatLng s2LatLng = S2LatLng.fromDegrees(lat, lng);
        long s2Id = S2CellId.fromLatLng(s2LatLng).parent(GeoDb.S2_LEVEL).id();
        if (geoDb.quickHint(s2Id) != null) {
            return geoDb.quickHint(s2Id);
        }
        for (int level = GeoDb.S2_QUICK_CELL_START_LEVEL; level <= GeoDb.S2_QUICK_CELL_END_LEVEL; level++) {
            long testS2Id = S2CellId.fromLatLng(s2LatLng).parent(level).id();
            Integer code = geoDb.quickHint(testS2Id);
            if (code != null) {
                return code;
            }
        }

        //step1 使圆环覆盖线段库，圆环按2倍扩容，直到超过200000
        Set<BoundaryLine> lines = new HashSet<>();
        //必须大于2000m，否则会出现格子半径过小选择错误问题
        int maxRadius = 2000;
        while (lines.isEmpty() && maxRadius < 200000) {
            lines = GeoUtils.cellIdsWithCapLevel(s2LatLng, maxRadius, GeoDb.S2_LEVEL)
                    .stream()
                    .flatMap((Function<Long, Stream<BoundaryLine>>) start ->
                            geoDb.queryLines(start).stream())
                    .collect(Collectors.toSet());
            maxRadius = maxRadius * 2;
        }
        if (lines.isEmpty()) {
            return -1;
        }

        // step2，这些线段，求距离目标点位最近的一批线段
        double minDistance = Double.MAX_VALUE;
        List<BoundaryLine> minDistanceLines = new ArrayList<>();
        for (BoundaryLine boundaryLine : lines) {
            S2LatLng start = new S2CellId(boundaryLine.start).toLatLng();
            S2LatLng end = new S2CellId(boundaryLine.end).toLatLng();
            double dis = GeoUtils.pointToLineDis(start.lngDegrees(), start.latDegrees(), end.lngDegrees(), end.latDegrees(), lng, lat);
            if (dis > minDistance) {
                continue;
            }
            if (dis < minDistance) {
                minDistance = dis;
                minDistanceLines.clear();
            }
            minDistanceLines.add(boundaryLine);
        }

        // step3,射线顶点问题，目标点在在射线的顶点侧，此时目标点到两条线段的最短距离都是射线顶点，然而实际上依然可以区分两条线段和目标点谁近谁远
        //
        // case point(lng:135.11,lat: 43.86)
        //       line1(6796877348982489088 -> 6796874462764466176) : 正确值 -> 多边形外部，正确答案海外
        //       line2(6796874462764466176 -> 6796880372639465472) ： 需过滤 -> 如果不过滤，将会被判定为在多边形内部，解析结果到 黑龙江省鸡西市密山市
        minDistance = Double.MAX_VALUE;
        List<BoundaryLine> minDistanceLinesLevel1 = minDistanceLines;
        minDistanceLines = new ArrayList<>();
        for (BoundaryLine boundaryLine : minDistanceLinesLevel1) {
            S2LatLng start = new S2CellId(boundaryLine.start).toLatLng();
            S2LatLng end = new S2CellId(boundaryLine.end).toLatLng();
            // 根据点到线段中心点在做一次过滤，确定真实的最近点
            double dis = GeoUtils.lineDis((start.lngDegrees() + end.lngDegrees()) / 2,
                    (start.latDegrees() + end.latDegrees()) / 2,
                    lng, lat);
            if (dis > minDistance) {
                continue;
            }
            if (dis < minDistance) {
                minDistance = dis;
                minDistanceLines.clear();
            }
            minDistanceLines.add(boundaryLine);
        }


        // step3，根据最近线段确认行政区域
        List<BoundaryLine> inPylons = minDistanceLines.stream().filter(boundaryLine -> {
            // 三点行列式计算面积大于0，则证明在多边形内部，
            // 我们的行政区域多边形数据使用逆时针方向存储，此时如果我们的目标点（p3）在多边形内部，则(p1,p2,p3)组成三角形位逆向方向，行列式面积位为正数
            return GeoUtils.area(Arrays.asList(boundaryLine.start, boundaryLine.end, s2Id, boundaryLine.start)) >= 0;
            // 面积为0，则证明在边界线上，那么当作有效判定
        }).collect(Collectors.toList());

        if (!inPylons.isEmpty()) {
            // todo 多个是否可能有多个结果命中，并且adminCode还不一致？
            return inPylons.get(0).adminCode;
        }

        if (minDistanceLines.size() == 1) {
            // 国内海外边界 & 点位消重精度模糊导致的顶点回溯在线段上的问题
            // 关于回溯到线段上场景： a->b b->c d->d 其中c位于ab线段上，则会产生一个重合线段，此时最小距离线段搜寻可能命中bc，则bc将会让目标点命中多边形外部，则是错误结果
            // 请注意这个问题是多边形精度模糊处理导致的，如果是来自测绘局正常的多边形数据，不会出现这种奇怪多边形变种
            BoundaryLine boundaryLine = minDistanceLines.get(0);
            S2LatLng start = new S2CellId(boundaryLine.start).toLatLng();
            S2LatLng end = new S2CellId(boundaryLine.end).toLatLng();

            // 实际地球球面以米为单位的距离
            Double earthDistance = GeoUtils.distanceMeter(
                    new Location((start.lngDegrees() + end.lngDegrees()) / 2, (start.latDegrees() + end.latDegrees()) / 2),
                    s2Id
            );
            if (earthDistance < 15000D) {
                // 由于我们对于海外边界精准度要求并不高，所以至少距离边界少于15公里，那么当作国内看待
                // 因为实际看来在海边小岛，如果按中国大陆边界来判定的话，会存在解析到海外
                return boundaryLine.adminCode;
            }
        }
        return -1;
    }


    private static GeoDb openGeoDb() {
        File dbFile = new File(geoBaseDir(), DB_RES_KEY);
        if (!dbFile.exists()) {
            try (ZipInputStream zipInputStream = new ZipInputStream(openResource(DB_RES_ZIP_KEY))) {
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry == null || !DB_RES_KEY.equals(nextEntry.getName())) {
                    throw new IllegalStateException("error geo db zip resource file: " + DB_RES_ZIP_KEY);
                }
                FileUtils.forceMkdirParent(dbFile);
                try (OutputStream outputStream = Files.newOutputStream(dbFile.toPath())) {
                    IOUtils.copy(zipInputStream, outputStream);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return new GeoDb(dbFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream openResource(String name) {
        InputStream resource = GeoTrans.class.getClassLoader()
                .getResourceAsStream(name);
        if (resource == null) {
            throw new IllegalStateException("can not find resource: " + name);
        }
        return resource;
    }

    @SneakyThrows
    private static Map<String, Integer> loadAddressData() {
        ConcurrentHashMap<String, Integer> ret = new ConcurrentHashMap<>();
        File file = new File(geoBaseDir(), ADMIN_ADDRESS_CONFIG);
        if (!file.exists()) {
            ret.putAll(GeoTrans.adminNormalizer.getFullNameMap());
            storeAddressData(ret);
            return ret;
        }
        Properties properties = new Properties();
        properties.load(Files.newInputStream(file.toPath()));
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key, "-1");
            ret.put(key, Integer.parseInt(value));
        }
        return ret;
    }

    @SneakyThrows
    public static void storeAddressData(Map<String, Integer> normalizeNames) {
        Properties properties = new Properties();
        normalizeNames.forEach((s, integer) -> properties.put(s, String.valueOf(integer)));
        properties.store(Files.newOutputStream(new File(geoBaseDir(), ADMIN_ADDRESS_CONFIG).toPath()), "geo address config");
    }

    private static File geoBaseDir() {
        String userHome = System.getProperty("user.home");
        File base;
        if (userHome != null && !userHome.trim().isEmpty()) {
            base = new File(userHome);
        } else {
            base = new File(".");
        }
        return new File(base, ".geo");
    }

    public static Long cellId(Double lng, Double lat) {
        S2LatLng s2LatLng = S2LatLng.fromDegrees(lat, lng);
        return S2CellId.fromLatLng(s2LatLng).parent(GeoDb.S2_LEVEL).id();
    }
}
