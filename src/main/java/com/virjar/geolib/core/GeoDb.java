package com.virjar.geolib.core;

import com.virjar.geolib.bean.Admin;
import com.virjar.geolib.bean.BoundaryLine;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 存储所有的Geo数据
 */
public class GeoDb implements Closeable {
    public static final int S2_LEVEL = 12;
    public static final int S2_QUICK_CELL_START_LEVEL = 8;
    public static final int S2_QUICK_CELL_END_LEVEL = 10;


    // it equals "geo-vj00"
    public static final long magic = 0x67656f2d766a3030L;
    public static final int itemSize = 8 + 8 + 4;

    /**
     * enable mmap when running on 64-bit os system,
     * mmap is a memory save way because of it`s zero copy(DMA) implementation and virtual memory mapping by os system.
     * we will not load our GEO-db file (200M size) info memory if we not use GeoDb module
     */
    public static final boolean USE_MMAP = is64bit();
    private final ByteBuffer dbBuffer;
    int boundaryOffset;
    int hashItemSize;
    private final RandomAccessFile randomAccessFile;

    private final Map<Integer, AdminEx> adminMap = new HashMap<>();

    private final Map<Long, Integer> quickCache = new HashMap<>();

    public GeoDb(File dataFile) throws IOException {
        randomAccessFile = new RandomAccessFile(dataFile, "r");
        FileChannel fileChannel = randomAccessFile.getChannel();
        if (USE_MMAP) {
            dbBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, dataFile.length());
        } else {
            dbBuffer = ByteBuffer.allocate((int) dataFile.length());
            fileChannel.read(dbBuffer);
        }

        long magic = dbBuffer.getLong();
        if (magic != GeoDb.magic) {
            throw new IOException("error format: " + dataFile.getAbsolutePath() + " not geoDb binary database file,"
                    + "expected: " + GeoDb.magic + " actual: " + magic);
        }

        boundaryOffset = dbBuffer.getInt();
        hashItemSize = dbBuffer.getInt();
        // 行政节点，只有四千个左右，数据量级可控，故读取到内存再散开
        readAdminData(dbBuffer);
        // 快速缓存，对于一些特定区块，可以直接map快速查询，本map 4k数据量
        readQuickCache(dbBuffer);
        // 行政区域边界线数据量巨大（大约150M），故不能做内存读取解析，而是直接使用线性hashTable来执行文件索引
    }


    public Admin getAdmin(int code) {
        return adminMap.get(code);
    }

    public Collection<Admin> getChildren(int code) {
        AdminEx adminEx = adminMap.get(code);
        if (adminEx == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableSet(adminEx.children);
    }

    public List<Admin> allAdminList() {
        return new ArrayList<>(adminMap.values());
    }

    public Integer quickHint(Long s2Id) {
        return quickCache.get(s2Id);
    }

    public List<BoundaryLine> queryLines(long point) {
        List<BoundaryLine> ret = new ArrayList<>();

        int startIndex = (int) (GeoDb.linePointHash(point) % hashItemSize);
        for (int i = 0; i < hashItemSize; i++) {
            int index = (startIndex + i) % hashItemSize;
            long start = dbBuffer.getLong(boundaryOffset + index * itemSize);
            if (start == 0) {
                return ret;
            }
            if (start != point) {
                // 哈希冲突
                continue;
            }
            long end = dbBuffer.getLong(boundaryOffset + index * itemSize + 8);
            int adminCode = dbBuffer.getInt(boundaryOffset + index * itemSize + 16);
            ret.add(new BoundaryLine(adminCode, start, end));
        }
        return ret;
    }


    private void readAdminData(ByteBuffer buffer) {
        List<AdminEx> adminExes = readAdminList(buffer);
        adminExes.forEach(adminEx -> adminMap.put(adminEx.getId(), adminEx));
        adminExes.forEach(adminEx -> {
            int parentId = adminEx.getParentId();
            AdminEx parent = adminMap.get(parentId);
            if (parent != null) {
                parent.children.add(adminEx);
            }
        });
    }

    private void readQuickCache(ByteBuffer buffer) {
        int size = Leb128.readUnsignedLeb128(buffer);
        for (int i = 0; i < size; i++) {
            long cellId = buffer.getLong();
            int adminCode = buffer.getInt();
            quickCache.put(cellId, adminCode);
        }
    }

    public static long linePointHash(long startPointS2) {
        return Math.abs(startPointS2 ^ magic);
    }

    private static boolean is64bit() {
        // 如果需要支持android，请解开下面注释
//        try{
//            Class.forName("android.content.Context");
//            return Process.is64Bit();
//        }catch (Throwable ignore){
//            // not android env
//        }
        if (System.getProperty("os.name").contains("Windows")) {
            return System.getenv("ProgramFiles(x86)") != null;
        } else {
            return System.getProperty("os.arch").contains("64");
        }
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }

    private List<AdminEx> readAdminList(ByteBuffer buffer) {
        int size = Leb128.readUnsignedLeb128(buffer);
        Admin.DistrictLevel[] districtLevels = Admin.DistrictLevel.values();
        ArrayList<AdminEx> adminNodes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            AdminEx adminNode = new AdminEx();
            adminNodes.add(adminNode);
            adminNode.setId(Leb128.readUnsignedLeb128(buffer));
            adminNode.setParentId(Leb128.readUnsignedLeb128(buffer));
            adminNode.setName(readString(buffer));
            adminNode.setShortName(readString(buffer));
            adminNode.setCenterLng(buffer.getFloat());
            adminNode.setCenterLat(buffer.getFloat());
            adminNode.setLevel(districtLevels[buffer.get()]);
        }
        return adminNodes;
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

    private static final ThreadLocal<WeakReference<byte[]>> buffLocal = new ThreadLocal<>();

    private static byte[] localBuff(int len) {
        if (len < 1024) {
            len = 1024;
        }
        WeakReference<byte[]> weakReference = buffLocal.get();
        byte[] bytes;
        if (weakReference != null) {
            bytes = weakReference.get();
            if (bytes != null && bytes.length >= len) {
                return bytes;
            }
        }

        bytes = new byte[len];
        buffLocal.set(new WeakReference<>(bytes));
        return bytes;
    }


    private static class AdminEx extends Admin {
        private final Set<Admin> children = new TreeSet<>();
    }
}
