package com.virjar.geolib;

import com.virjar.geolib.bean.Admin;
import com.virjar.geolib.bean.Boundary;
import com.virjar.geolib.bean.BoundaryLine;
import com.virjar.geolib.core.GeoDb;
import com.virjar.geolib.core.GeoUtils;
import com.virjar.geolib.core.Leb128;
import com.google.common.geometry.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 构建geo数据库文件，考虑geo数据库文件大小，正常情况下将geo数据加载到内存会有很大的冷启动时间，所以这里我会将geo数据编译成二进制格式，
 * 并且在文件中构建内存映射格式，这样可以通过直接打开文件的方式加载格式化数据，并且绕过数据加载的解析过程，
 * 当然这种方案的坏处就是我们对于数据的操作就会比较有难度了
 */
public class GeoBinBuilder {
    private static final int M = (1 << 20);

    private volatile boolean commit = false;
    private final Map<Integer, Admin> adminData = new HashMap<>();
    private final Map<Integer, Boundary> boundaryData = new HashMap<>();

    /**
     * <a href="https://baike.baidu.com/item/%E8%83%A1%E7%84%95%E5%BA%B8%E7%BA%BF">黑河-腾冲线</a>
     */
    public static final long HEI_HE = S2CellId.fromLatLng(S2LatLng.fromDegrees(50.250535, 127.838467)).parent(GeoDb.S2_LEVEL).id();
    public static final long TEN_CONG = S2CellId.fromLatLng(S2LatLng.fromDegrees(24.871457, 98.310169)).parent(GeoDb.S2_LEVEL).id();


    public GeoBinBuilder addAdmin(Admin admin) {
        if (commit) {
            throw new IllegalStateException("can not add data after commit");
        }
        adminData.put(admin.getId(), admin);
        return this;
    }

    public void addAdminBoundary(int adminCode, List<Long> pylon) {
        if (commit) {
            throw new IllegalStateException("can not add data after commit");
        }

        Boundary boundary = boundaryData.computeIfAbsent(adminCode, Boundary::new);
        // transform cell level as 12
        List<Long> pylonWith12 = new ArrayList<>();
        Set<Long> duplicateRemove = new HashSet<>();
        for (Long point : pylon) {
            Long newId = new S2CellId(point).parent(GeoDb.S2_LEVEL).id();
            if (duplicateRemove.contains(newId)) {
                continue;
            }
            duplicateRemove.add(newId);
            pylonWith12.add(newId);
        }
        if (!Objects.equals(pylonWith12.get(0), pylonWith12.get(pylonWith12.size() - 1))) {
            // make sure the pylon close
            pylonWith12.add(pylonWith12.get(0));
        }
        boundary.getPylons().add(pylonWith12);
    }

    public byte[] build() {
        commit = true;
        // 总的多边形顶点数量：141w，则线段数量越为141w
        // 数据大小预估为： 141w * 20(bit) / 0.75(factor) = 180M
        // 线段有两个顶点，本质上顶点数据存在两倍冗余，故数据包压缩可以带来大约50%的压缩率,压缩后大小为86M
        ByteBuffer buffer = ByteBuffer.allocate(180 * M);
        buffer.putLong(GeoDb.magic);// magic
        buffer.putInt(0);// boundary offset
        buffer.putInt(0);// hashItemSize
        writeAdminNodes(adminData.values(), buffer);
        log("build pylon data");
        PylonBundle pylonBundle = buildBoundaryLineList();

        // 对于一些很小或者很大的地方，创建快速查询缓存
        // 1. 对于很小的地方(约2k个)，多边形平均精度小于2km，导致无法在level 12构建合法多边形，此时直接使用s2点集合索引
        // 2. 对于很大的地方(约2.2k个)，如新疆、西藏，由于多边形太大，则探测一个较大范围的level 12集合范围,此时将会比较影响查询效率，但是我们可以完全使用更高级别的s2（level 9）表达的区块，
        //    直接使用s2点集合索引（高级别level区块可以表达更大范围空间，故其总量是较少的、可靠的）
        writeCacheS2Table(pylonBundle.quickCachePoints, buffer);
        int boundaryOffset = buffer.position();


        log("write pylon into hashtable start");
        int hashItemSize = writeBoundaryHashTable(buffer, pylonBundle.boundaryLines);
        log("write pylon into hashtable finish");
        int endOffset = buffer.position();

        buffer.position(8);
        buffer.putInt(boundaryOffset);
        buffer.putInt(hashItemSize);
        buffer.position(endOffset);


        buffer.flip();
        byte[] bytes = new byte[buffer.limit() - buffer.position()];
        buffer.get(bytes);
        return bytes;
    }


    private void writeCacheS2Table(Map<Long, Integer> cache, ByteBuffer buffer) {
        Leb128.writeUnsignedLeb128(buffer, cache.size());
        cache.forEach((s2Id, adminCode) -> {
            buffer.putLong(s2Id);
            buffer.putInt(adminCode);
        });
    }

    /**
     * 使用数组hash存储所有多边形线段，数组hash具有线性映射特性，所以我们可以直接打开文件把hash表释放到内存中，
     * 这样可以节省项目启动大量多边形数据加载时间
     */
    private int writeBoundaryHashTable(ByteBuffer buffer, List<BoundaryLine> boundaryLines) {
        // 按照点位排序一下，当然实际上也可以不用排序的，排序之后让hash表范围更加稳定，可以方便二进制观察数据
        boundaryLines.sort(Comparator.comparingLong(o -> o.start));

        // 每个条目组成，两个点位16字节，code 4个字节

        // java的哈希惯例，使用0.75作为荷载因子，然而0.75已经会触发hash resize了，此时实际空间大小将会变成实际条目大小的 1.33倍（1/0.75 = 1.33）
        // 0.75 最高冲突检测250次， 0.70 最高冲突检测次数：159 0.65 最高冲突检测：142 0.60 最高冲突检测次数：84
        // 荷载因子越大，则越节省空间；但是会导致哈西冲突变高；
        int cab = (int) (boundaryLines.size() / 0.60);
        int hashTableSize = cab * GeoDb.itemSize;
        byte[] bytes = new byte[hashTableSize];
        ByteBuffer boundaryTable = ByteBuffer.wrap(bytes);

        //  AtomicInteger maxHashCount = new AtomicInteger(0);
        boundaryLines.forEach(boundaryLine -> {
            int startIndex = (int) (GeoDb.linePointHash(boundaryLine.start) % cab);
            for (int i = 0; i < cab; i++) {
                int index = (startIndex + i) % cab;
                if (boundaryTable.getLong(index * GeoDb.itemSize) != 0) {
                    // 已经有值了
                    continue;
                }
                //if (i > maxHashCount.get()) {
                //    maxHashCount.set(i);
                //}
                //System.out.println("write hashTable for point: " + JSONObject.toJSONString(boundaryLine) + " hashCount:" + i);
                boundaryTable.position(index * GeoDb.itemSize);
                boundaryTable.putLong(boundaryLine.start);
                boundaryTable.putLong(boundaryLine.end);
                boundaryTable.putInt(boundaryLine.adminCode);
                break;
            }
        });

        //System.out.println("max hashCount: " + maxHashCount.get());
        buffer.put(bytes);
        return cab;
    }


    private PylonBundle buildBoundaryLineList() {
        PylonBundle pylonBundle = new PylonBundle();
        boundaryData.values().forEach(boundary -> {
            int adminCode = boundary.getCode();

            boundary.getPylons().forEach(pylon -> {
                // 每个list就是一个多边形pylon，对于单个行政区域可能由多个多边形组成
                // 检查多边形的线段组成方向，设定逆时针位合法方向，如此使用行列式计算多边形面积则面积为正数
                // 请注意多边形方向是非常重要的，因为我们最终是依靠目标点位到多边形任意线段组成的三角形的面积来判定点位是否处于多边形内部，进而判定点位的行政区域
                double area = GeoUtils.area(pylon);
                if (area == 0) {
                    // 如果面积为0，那说明多边形不构成图形，就是单纯的点/线段，出现这个问题的原因是2km的精度下多边形顶点会被重合
                    // 此时说明这是一个很小的图形，此时直接存储到索引中就可以
                    pylon.forEach(aLong -> pylonBundle.quickCachePoints.put(aLong, adminCode));
                    return;
                }
                if (area < 0) {
                    // 如若判定面积为负数，则需要对点位集合进行逆序
                    Collections.reverse(pylon);
                }

                for (int i = 0; i < pylon.size() - 1; i++) {
                    pylonBundle.boundaryLines.add(new BoundaryLine(adminCode, pylon.get(i), pylon.get(i + 1)));
                }


                // 当多边形在黑河-腾冲线左边，则可能是地广人稀的区域，此时生成快速索引表,这个过程非常耗时（约2分钟），但是还好我们是提前预计算完成的
                List<Long> heartMapLineTriangle = new ArrayList<>();
                heartMapLineTriangle.add(HEI_HE);
                heartMapLineTriangle.add(TEN_CONG);
                heartMapLineTriangle.add(pylon.get(0));
                heartMapLineTriangle.add(HEI_HE);// 注意，这是三角形，但是需要4个点才能描述闭合动作（即首尾相同），同时数据结构方便了三角形行列式计算（避免执行环形数组回看）

                if (GeoUtils.area(heartMapLineTriangle) < 0) {
                    S2Polygon s2Polygon = new S2Polygon(new S2Loop(pylon.stream().map(aLong -> new S2CellId(aLong).toPoint()).collect(Collectors.toList())));

                    S2RegionCoverer s2RegionCoverer = new S2RegionCoverer();
                    s2RegionCoverer.setMinLevel(GeoDb.S2_QUICK_CELL_START_LEVEL);
                    s2RegionCoverer.setMaxLevel(GeoDb.S2_QUICK_CELL_END_LEVEL);
                    s2RegionCoverer.getInteriorCovering(s2Polygon).forEach(s2CellId ->
                            pylonBundle.quickCachePoints.put(s2CellId.id(), adminCode)
                    );
                }
            });
        });
        return pylonBundle;
    }


    private void writeAdminNodes(Collection<Admin> adminNodes, ByteBuffer buffer) {
        Leb128.writeUnsignedLeb128(buffer, adminNodes.size());
        for (Admin adminNode : adminNodes) {
            Leb128.writeUnsignedLeb128(buffer, adminNode.getId());
            Leb128.writeUnsignedLeb128(buffer, adminNode.getParentId());
            writeString(buffer, adminNode.getName());
            writeString(buffer, adminNode.getShortName());
            buffer.putFloat(adminNode.getCenterLng());
            buffer.putFloat(adminNode.getCenterLat());
            buffer.put((byte) adminNode.getLevel().ordinal());
        }
    }


    private static void writeString(ByteBuffer buffer, String str) {
        if (str == null) {
            str = "";
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        Leb128.writeUnsignedLeb128(buffer, bytes.length);
        if (bytes.length > 0) {
            buffer.put(bytes);
        }
    }

    private static class PylonBundle {
        private final List<BoundaryLine> boundaryLines = new ArrayList<>();
        private final Map<Long, Integer> quickCachePoints = new HashMap<>();
    }

    public static void log(String msg) {
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + ":" + msg);
    }

}
