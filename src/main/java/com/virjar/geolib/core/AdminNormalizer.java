package com.virjar.geolib.core;

import com.virjar.geolib.bean.Admin;

import java.util.*;
import java.util.stream.Collectors;

public class AdminNormalizer {
    private final List<Admin> adminList;
    private final GeoDb geoDb;

    private final Map<String, Integer> fullNameMap = new HashMap<>();
    private final Map<String, Integer> fullLevelMap = new HashMap<>();

    private final Map<String, String> renameDistrictMap = new HashMap<String, String>() {
        {
            put("云州区", "大同县");
            put("云冈区", "南郊区");
            put("光明区", "宝安区");
            put("上党区", "长治县");
            put("大柴旦行政委员会", "海西蒙古族藏族自治州直辖");
            put("冷湖行政委员会", "海西蒙古族藏族自治州直辖");
            put("茫崖行政委员会", "海西蒙古族藏族自治州直辖");
            put("上饶县", "广信区");
            put("色尼区", "那曲县");
            put("襄樊", "襄阳");
            new ArrayList<>(entrySet()).forEach(entry -> put(entry.getValue(), entry.getKey()));
        }
    };

    public AdminNormalizer(GeoDb geoDb) {
        this.geoDb = geoDb;
        adminList = geoDb.allAdminList();
        // 使用行政区域名称构建姓名缓存
        buildNameCache();
    }

    public Map<String, Integer> getFullNameMap() {
        return Collections.unmodifiableMap(fullNameMap);
    }

    private void buildNameCache() {
        Map<String, List<Admin>> fullNames = new HashMap<>();
        adminList.forEach(admin -> fullNames.computeIfAbsent(admin.getName(), s -> new ArrayList<>()).add(admin));
        fullNames.forEach((s, adminList) -> {
            if (adminList.size() == 1) {
                // 如果有多条记录，则证明可能有歧义，暂时忽略
                fullNameMap.put(s, adminList.get(0).getId());
            }
        });

        fullNames.clear();
        adminList.forEach(admin -> fullNames.computeIfAbsent(admin.getShortName(), s -> new ArrayList<>()).add(admin));
        fullNames.forEach((s, adminList) -> {
            if (adminList.size() == 1 && !fullNameMap.containsKey(s)) {
                fullNameMap.put(s, adminList.get(0).getId());
            }
        });

        adminList.forEach(admin -> {
            Admin tmpAdmin = admin;
            int id = admin.getId();
            LinkedList<Admin> fullLevel = new LinkedList<>();
            while (tmpAdmin != null) {
                fullLevel.addFirst(tmpAdmin);
                tmpAdmin = geoDb.getAdmin(tmpAdmin.getParentId());
            }
            fullLevelMap.put(fullLevel.stream().map(Admin::getName).collect(Collectors.joining()), id);
            fullLevelMap.put(fullLevel.stream().map(Admin::getShortName).collect(Collectors.joining()), id);
        });
    }

    public int doNormalize(String address) {
        return doNormalize(address, true);
    }

    public int doNormalize(String address, boolean useRename) {
        address = address.trim();
        if (fullNameMap.containsKey(address)) {
            return fullNameMap.get(address);
        }
        String hintKey = null;
        double maxScore = Double.MIN_VALUE;
        for (String test : fullLevelMap.keySet()) {
            double score = levenshtein(address, test);
            if (score > maxScore) {
                hintKey = test;
                maxScore = score;
            }
        }

        if (hintKey != null && maxScore > 0.2) {
            return fullLevelMap.get(hintKey);
        }
        if (useRename) {
            // 处理改名地区
            for (Map.Entry<String, String> entry : renameDistrictMap.entrySet()) {
                address = address.replace(entry.getKey(), entry.getValue());
            }
            return doNormalize(address, false);
        }
        return -1;
    }


    /**
     * 计算两个字符串的编辑距离相似度，具体原理请网上查询编辑距离的概念
     */
    public static double levenshtein(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        int[][] dif = new int[len1 + 1][len2 + 1];
        for (int a = 0; a <= len1; a++) {
            dif[a][0] = a;
        }
        for (int a = 0; a <= len2; a++) {
            dif[0][a] = a;
        }
        int tmp;
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    tmp = 0;
                } else {
                    tmp = 1;
                }
                dif[i][j] = min(dif[i - 1][j - 1] + tmp, dif[i][j - 1] + 1, dif[i - 1][j] + 1);
            }
        }
        return 1 - dif[len1][len2] / (double) Math.max(str1.length(), str2.length());

    }

    private static int min(int... is) {
        int min = Integer.MAX_VALUE;
        for (int i : is) {
            if (min > i) {
                min = i;
            }
        }
        return min;
    }
}
