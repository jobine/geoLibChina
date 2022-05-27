package com.dengxq.lnglat2Geo.loader;

import com.dengxq.lnglat2Geo.entity.*;
import com.dengxq.lnglat2Geo.entity.enums.DistrictLevel;
import com.dengxq.lnglat2Geo.utils.Leb128;
import com.dengxq.lnglat2Geo.utils.Md5Util;
import com.speedment.common.tuple.Tuple3;
import com.speedment.common.tuple.Tuples;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 二进制格式加载，相比json更快，更节省空间，
 * 但是文件内容无法查看
 */
public class BinLoader implements ILoader {
    private static final int M = (1 << 20);

    private String sourcePath = "data.bin";

    public BinLoader(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public BinLoader() {
    }

    @Override
    public GeoData load() {
        try (InputStream stream = BinLoader.class.getClassLoader().getResourceAsStream(sourcePath)) {
            if (stream == null) {
                throw new RuntimeException("can not find data resource: " + sourcePath);
            }
            byte[] bytes = IOUtils.toByteArray(stream);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            GeoData geoData = new GeoData();
            geoData.setAdminData(readAdminNodes(buffer));
            geoData.setStreetData(readAdminNodes(buffer));
            geoData.setAdminBoundaries(readAdminBoundaries(buffer));
            geoData.setAreaGroups(readAreaGroups(buffer));
            geoData.setCellAdmins(readCellAdmins(buffer));
            geoData.setCityLevel(readMap(buffer));
            geoData.setCountryCode(readMap(buffer));

            // 对于扩展数据，我们默认是不会打入到二进制中，因为他会导致我们文件太大
            // 但是我们会把他缓存到缓存文件中，这样多次运行就会比较快
            String dataMd5 = Md5Util.getHashWithInputStream(new ByteArrayInputStream(bytes));
            handleExtension(geoData, dataMd5);
            return geoData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleExtension(GeoData geoData, String md5) throws IOException {
        String userHome = System.getProperty("user.home");
        File base;
        if (userHome != null && !userHome.trim().isEmpty()) {
            base = new File(userHome);
        } else {
            base = new File(".");
        }
        File extensionPart = new File(base, md5 + ".xcgeo");
        if (extensionPart.exists()) {
            // 如果存在缓存，那么把缓存加载到内存
            loadExtension(geoData, extensionPart);
        } else {
            new Thread("createExtension") {
                @Override
                public void run() {
                    // 如果没有缓存文件，那么使用新的线程建立缓存文件
                    storeExtension(geoData, extensionPart);
                }
            }.start();

        }
    }

    private void loadExtension(GeoData geoData, File file) throws IOException {
        try (FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel()) {
            MappedByteBuffer buffer = fileChannel
                    .map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            geoData.setRuntimeBoundaryData(readRuntimeBoundaryData(buffer));
            geoData.setRuntimeBoundaryIndex(readRuntimeBoundaryIndex(buffer));
            geoData.setRuntimeBoundaryAdminCell(readRuntimeBoundaryAdminCell(buffer));
        }
    }

    private void storeExtension(GeoData geoData, File file) {
        ByteBuffer buffer = ByteBuffer.allocate(300 * M);
        writeRuntimeBoundaryData(geoData.getOrCreateRuntimeBoundaryData(), buffer);
        writeRuntimeBoundaryIndex(geoData.getOrCreateRuntimeBoundaryIndex(), buffer);
        writeRuntimeBoundaryAdminCell(geoData.getOrCreateRuntimeBoundaryAdminCell(), buffer);
        saveByteBuffer(buffer, file);
    }

    @Override
    public void dump(File path, GeoData geoData) {
        // dump不需要考虑性能,分配50M的内存空间
        ByteBuffer buffer = ByteBuffer.allocate(70 * M);
        writeAdminNodes(geoData.getAdminData(), buffer);
        writeAdminNodes(geoData.getStreetData(), buffer);
        writeAdminBoundaries(geoData.getAdminBoundaries(), buffer);
        writeAreaGroups(geoData.getAreaGroups(), buffer);
        writeCellAdmins(geoData.getCellAdmins(), buffer);
        writeMap(geoData.getCityLevel(), buffer);
        writeMap(geoData.getCountryCode(), buffer);

        saveByteBuffer(buffer, path);
    }

    private static void saveByteBuffer(ByteBuffer buffer, File file) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            buffer.flip();
            byte[] bytes = localBuff(4096);

            while (buffer.hasRemaining()) {
                int count = Math.min(bytes.length, buffer.remaining());
                buffer.get(bytes, 0, count);
                fileOutputStream.write(bytes, 0, count);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ThreadLocal<byte[]> buffLocal = new ThreadLocal<>();

    private static byte[] localBuff(int len) {
        if (len < 1024) {
            len = 1024;
        }
        byte[] bytes = buffLocal.get();
        if (bytes != null && bytes.length >= len) {
            return bytes;
        }
        bytes = new byte[len];
        buffLocal.set(bytes);
        return bytes;
    }


    private static String readString(ByteBuffer buffer) {
        int len = Leb128.readUnsignedLeb128(buffer);
        if (len == 0) {
            return "";
        }
        // 读需要使用缓存的内存变量，减少gc消耗，写不需要关注，
        // 这里提供了一个4k的缓存
        byte[] buf = localBuff(len);
        buffer.get(buf, 0, len);
        return new String(buf, 0, len, StandardCharsets.UTF_8);
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

    private void writeAdminNodes(List<AdminNode> adminNodes, ByteBuffer buffer) {
        Leb128.writeUnsignedLeb128(buffer, adminNodes.size());
        for (AdminNode adminNode : adminNodes) {
            Leb128.writeUnsignedLeb128(buffer, adminNode.id);
            writeString(buffer, adminNode.name);
            writeString(buffer, adminNode.shortName);

            // 经纬度，我们使用float编码，因为实际上看起来，目前的数据应该在float上
            buffer.putFloat(adminNode.center.lng.floatValue());
            buffer.putFloat(adminNode.center.lat.floatValue());

            buffer.put((byte) adminNode.level.ordinal());
            Leb128.writeUnsignedLeb128(buffer, adminNode.parentId);

            List<Integer> children = adminNode.children;
            Leb128.writeUnsignedLeb128(buffer, children.size());
            for (Integer integer : children) {
                Leb128.writeSignedLeb128(buffer, integer);
            }
        }
    }

    private List<AdminNode> readAdminNodes(ByteBuffer buffer) {
        int size = Leb128.readUnsignedLeb128(buffer);
        DistrictLevel[] districtLevels = DistrictLevel.values();
        ArrayList<AdminNode> adminNodes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            AdminNode adminNode = new AdminNode();
            adminNodes.add(adminNode);
            adminNode.setId(Leb128.readUnsignedLeb128(buffer));
            adminNode.setName(readString(buffer));
            adminNode.setShortName(readString(buffer));
            adminNode.setCenter(new Location((double) buffer.getFloat(), (double) buffer.getFloat()));
            adminNode.setLevel(districtLevels[buffer.get()]);
            adminNode.setParentId(Leb128.readUnsignedLeb128(buffer));

            int childrenSize = Leb128.readUnsignedLeb128(buffer);
            ArrayList<Integer> children = new ArrayList<>(childrenSize);
            adminNode.setChildren(children);
            for (int j = 0; j < childrenSize; j++) {
                children.add(Leb128.readSignedLeb128(buffer));
            }
        }
        return adminNodes;
    }


    private void writeAdminBoundaries(List<AdminBoundary> adminBoundaries, ByteBuffer buffer) {
        Leb128.writeUnsignedLeb128(buffer, adminBoundaries.size());
        for (AdminBoundary adminBoundary : adminBoundaries) {
            //code
            Leb128.writeSignedLeb128(buffer, adminBoundary.code);
            List<List<Long>> boundary = adminBoundary.boundary;
            Leb128.writeUnsignedLeb128(buffer, boundary.size());
            for (List<Long> line : boundary) {
                Leb128.writeUnsignedLeb128(buffer, line.size());
                for (Long point : line) {
                    buffer.putLong(point);
                }
            }
        }
    }

    public List<AdminBoundary> readAdminBoundaries(ByteBuffer buffer) {
        int size = Leb128.readUnsignedLeb128(buffer);
        ArrayList<AdminBoundary> adminBoundaries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            AdminBoundary adminBoundary = new AdminBoundary();
            adminBoundaries.add(adminBoundary);
            adminBoundary.setCode(Leb128.readSignedLeb128(buffer));

            int boundarySize = Leb128.readUnsignedLeb128(buffer);
            ArrayList<List<Long>> boundary = new ArrayList<>(boundarySize);
            adminBoundary.setBoundary(boundary);
            for (int j = 0; j < boundarySize; j++) {
                int lineSize = Leb128.readUnsignedLeb128(buffer);
                ArrayList<Long> line = new ArrayList<>(lineSize);
                boundary.add(line);
                for (int z = 0; z < lineSize; z++) {
                    line.add(buffer.getLong());
                }
            }

        }

        return adminBoundaries;
    }


    private void writeAreaGroups(List<BusinessAreaGroup> areaGroups, ByteBuffer buffer) {
        Leb128.writeUnsignedLeb128(buffer, areaGroups.size());
        for (BusinessAreaGroup businessAreaGroup : areaGroups) {
            Leb128.writeSignedLeb128(buffer, businessAreaGroup.cityAdCode);
            List<BusinessAreaData> areas = businessAreaGroup.areas;
            Leb128.writeUnsignedLeb128(buffer, areas.size());
            for (BusinessAreaData businessAreaData : areas) {
                writeString(buffer, businessAreaData.name);
                buffer.putFloat(businessAreaData.center.lng.floatValue());
                buffer.putFloat(businessAreaData.center.lat.floatValue());
                Leb128.writeSignedLeb128(buffer, businessAreaData.areaCode);
            }
        }
    }

    public List<BusinessAreaGroup> readAreaGroups(ByteBuffer buffer) {
        int size = Leb128.readUnsignedLeb128(buffer);
        List<BusinessAreaGroup> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            BusinessAreaGroup areaGroup = new BusinessAreaGroup();
            ret.add(areaGroup);
            areaGroup.setCityAdCode(Leb128.readSignedLeb128(buffer));
            int businessAreaDataSize = Leb128.readUnsignedLeb128(buffer);
            List<BusinessAreaData> areas = new ArrayList<>(businessAreaDataSize);
            areaGroup.setAreas(areas);
            for (int j = 0; j < businessAreaDataSize; j++) {
                BusinessAreaData businessAreaData = new BusinessAreaData();
                areas.add(businessAreaData);
                businessAreaData.setName(readString(buffer));
                businessAreaData.setCenter(new Location((double) buffer.getFloat(), (double) buffer.getFloat()));
                businessAreaData.setAreaCode(Leb128.readSignedLeb128(buffer));
            }
        }
        return ret;
    }

    private void writeCellAdmins(List<CellAdmin> cellAdmins, ByteBuffer buffer) {
        Leb128.writeUnsignedLeb128(buffer, cellAdmins.size());
        for (CellAdmin cellAdmin : cellAdmins) {
            Leb128.writeSignedLeb128(buffer, cellAdmin.adCode);
            buffer.putLong(cellAdmin.cellId);
        }
    }

    private List<CellAdmin> readCellAdmins(ByteBuffer buffer) {
        int size = Leb128.readUnsignedLeb128(buffer);
        List<CellAdmin> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            CellAdmin cellAdmin = new CellAdmin(Leb128.readSignedLeb128(buffer),
                    buffer.getLong());
            ret.add(cellAdmin);
        }
        return ret;
    }


    private void writeMap(Map<String, String> map, ByteBuffer buffer) {
        Leb128.writeUnsignedLeb128(buffer, map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            writeString(buffer, entry.getKey());
            writeString(buffer, entry.getValue());
        }
    }

    private Map<String, String> readMap(ByteBuffer buffer) {
        Map<String, String> ret = new HashMap<>();
        int size = Leb128.readUnsignedLeb128(buffer);
        for (int i = 0; i < size; i++) {
            ret.put(readString(buffer), readString(buffer));
        }
        return ret;
    }

    // 请注意，runtime文件不进行leb128，因为runtime大约会在200M左右，对他进行压缩没有意义
    // 反而leb128会有一些计算出现
    private void writeRuntimeBoundaryData(Map<Long, List<Tuple3<Long, Integer, Integer>>> data, ByteBuffer buffer) {
        buffer.putInt(data.size());
        for (Map.Entry<Long, List<Tuple3<Long, Integer, Integer>>> entry : data.entrySet()) {
            Long key = entry.getKey();
            buffer.putLong(key);
            List<Tuple3<Long, Integer, Integer>> value = entry.getValue();
            buffer.putInt(value.size());
            for (Tuple3<Long, Integer, Integer> tuple3 : value) {
                buffer.putLong(tuple3.get0());
                buffer.putInt(tuple3.get1());
                buffer.putInt(tuple3.get2());
            }
        }
    }

    private Map<Long, List<Tuple3<Long, Integer, Integer>>> readRuntimeBoundaryData(ByteBuffer buffer) {
        int mapSize = buffer.getInt();
        Map<Long, List<Tuple3<Long, Integer, Integer>>> ret = new HashMap<>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            long key = buffer.getLong();
            int listSize = buffer.getInt();
            List<Tuple3<Long, Integer, Integer>> line = new ArrayList<>(listSize);
            ret.put(key, line);

            for (int j = 0; j < listSize; j++) {
                line.add(Tuples.of(buffer.getLong(), buffer.getInt(), buffer.getInt()));
            }
        }
        return ret;
    }

    private void writeRuntimeBoundaryIndex(Map<Long, List<Long>> data, ByteBuffer buffer) {
        buffer.putInt(data.size());
        for (Map.Entry<Long, List<Long>> entry : data.entrySet()) {
            Long key = entry.getKey();
            buffer.putLong(key);
            List<Long> value = entry.getValue();
            buffer.putInt(value.size());
            for (Long aLong : value) {
                buffer.putLong(aLong);
            }
        }
    }

    private Map<Long, List<Long>> readRuntimeBoundaryIndex(ByteBuffer buffer) {
        int mapSize = buffer.getInt();
        Map<Long, List<Long>> ret = new HashMap<>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            long key = buffer.getLong();
            int listSize = buffer.getInt();
            List<Long> line = new ArrayList<>(listSize);
            ret.put(key, line);
            for (int j = 0; j < listSize; j++) {
                line.add(buffer.getLong());
            }
        }
        return ret;
    }

    private void writeRuntimeBoundaryAdminCell(Map<Long, Integer> data, ByteBuffer buffer) {
        buffer.putInt(data.size());
        for (Map.Entry<Long, Integer> entry : data.entrySet()) {
            buffer.putLong(entry.getKey());
            buffer.putInt(entry.getValue());
        }
    }

    private Map<Long, Integer> readRuntimeBoundaryAdminCell(ByteBuffer buffer) {
        int mapSize = buffer.getInt();
        Map<Long, Integer> ret = new HashMap<>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            ret.put(buffer.getLong(), buffer.getInt());
        }
        return ret;
    }
}
