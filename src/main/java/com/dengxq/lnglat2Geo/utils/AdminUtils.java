package com.dengxq.lnglat2Geo.utils;

import com.dengxq.lnglat2Geo.entity.enums.DistrictLevel;
import com.speedment.common.tuple.Tuple2;
import com.speedment.common.tuple.Tuples;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminUtils {
    public static final String[] NATIONS = "阿昌族,鄂温克族,傈僳族,水族,白族,高山族,珞巴族,塔吉克族,保安族,仡佬族,满族,塔塔尔族,布朗族,哈尼族,毛南族,土家族,布依族,哈萨克族,门巴族,土族,朝鲜族,汉族,蒙古族,佤族,达斡尔族,赫哲族,苗族,维吾尔族,傣族,回族,仫佬族,乌孜别克族,德昂族,基诺族,纳西族,锡伯族,东乡族,京族,怒族,瑶族,侗族,景颇族,普米族,彝族,独龙族,柯尔克孜族,羌族,裕固族,俄罗斯族,拉祜族,撒拉族,藏族,鄂伦春族,黎族,畲族,壮族".split(",");

    public static final Pattern p1 = Pattern.compile("(.+)(?:省|市)$");
    public static final Pattern p2 = Pattern.compile("(.+)自治区");
    public static final Pattern p3 = Pattern.compile("(.+)特别行政区");

    public static final Pattern c0 = Pattern.compile("^(.{2})$"); // 2 长度为2的 "东区" "南区"
    public static final Pattern c1 = Pattern.compile("(.+)(?:自治州|自治县)$"); // 30 自治州  琼中黎族苗族自治县
    public static final Pattern c2 = Pattern.compile("(.+)[市|盟|州]$"); // 304 地级市, 盟; + 1恩施州
    public static final Pattern c3 = Pattern.compile("(.+)地区$"); // 8 地区
    public static final Pattern c4 = Pattern.compile("(.+)(?:群岛|填海区)$"); // 2 东沙群岛
    public static final Pattern c5 = Pattern.compile("(.+[^地郊城堂])区$"); // 20 港澳 不含"东区" "南区"2个字的
    public static final Pattern c6 = Pattern.compile("(.+)(?:城区|郊县)$"); // 6 九龙城区,上海城区,天津城区,北京城区,重庆城区,重庆郊县
    public static final Pattern c7 = Pattern.compile("(.+[^郊])县$"); // 12 台湾的xx县

    public static final Pattern d0 = Pattern.compile("^(.{2})$"); // 2 长度为2的 "随县"
    public static final Pattern d1 = Pattern.compile("(.+)[市]$"); // 304 城区 “赤水市”
    public static final Pattern d2 = Pattern.compile("(.+)自治县$"); // 30 自治县
    public static final Pattern d3 = Pattern.compile("(.+)自治州直辖$"); // 30 自治州直辖 "海西蒙古族藏族自治州直辖"
    public static final Pattern d4 = Pattern.compile("(.+)[区|县]$"); // 8 区县
    public static final Pattern d5 = Pattern.compile("(.+)(?:乡|镇|街道)$"); // 8 乡镇|街道

    public static final Pattern s0 = Pattern.compile("^(.{2})$");
    public static final Pattern s1 = Pattern.compile("(.+)(?:特别行政管理区|街道办事处|旅游经济特区|民族乡|地区街道)$");
    public static final Pattern s2 = Pattern.compile("(.+)(?:镇|乡|村|街道|苏木|老街|管理区|区公所|苏木|办事处|社区|经济特区|行政管理区)$");


    public static String shortProvince(String province) {
        Matcher matcher = p1.matcher(province);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        matcher = p2.matcher(province);
        if (matcher.matches()) {
            String x = matcher.group(1);
            if (x.equals("内蒙古")) {
                return x;
            }
            return replaceNations(x);
        }

        matcher = p3.matcher(province);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        return province;
    }


    public static Tuple2<String, Integer> shortCityImp(String city) {
        // 总数 383
        Matcher matcher = c0.matcher(city);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 0);
        }
        matcher = c1.matcher(city);
        if (matcher.matches()) {
            return Tuples.of(replaceNations(matcher.group(1)), 2);
        }
        matcher = c2.matcher(city);
        if (matcher.matches()) {
            String x = matcher.group(1);
            if (x.equals("襄樊")) {
                x = "襄阳";
            }
            return Tuples.of(x, 1);
        }
        matcher = c3.matcher(city);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 3);
        }
        matcher = c4.matcher(city);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 4);
        }
        matcher = c5.matcher(city);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 5);
        }
        matcher = c6.matcher(city);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 6);
        }
        matcher = c7.matcher(city);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 7);
        }
        return Tuples.of(city, -1);
    }


    public static Tuple2<String, Integer> shortDistrictImp(String district) {
        // 总数 2963 56个内蒙八旗和新疆兵团没有处理
        Matcher matcher = d0.matcher(district);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 0);
        }
        matcher = d1.matcher(district);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 1);
        }
        matcher = d2.matcher(district);
        if (matcher.matches()) {
            return Tuples.of(replaceNations(matcher.group(1)), 2);
        }
        matcher = d3.matcher(district);
        if (matcher.matches()) {
            return Tuples.of(replaceNations(matcher.group(1)), 3);
        }
        matcher = d4.matcher(district);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 4);
        }
        matcher = d5.matcher(district);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 5);
        }
        return Tuples.of(district, -1);
    }


    public static Tuple2<String, Integer> shortStreetImp(String street) {
        // 总数 42387
        // 柘城县邵园乡人民政府, 保安镇, 鹅湖镇人民政府, 东风地区
        Matcher matcher = s0.matcher(street);
        if (matcher.matches()) {
            return Tuples.of(matcher.group(1), 0);
        }

        matcher = s1.matcher(street);
        if (matcher.matches()) {
            return Tuples.of(replaceNationsNotEmpty(matcher.group(1)), 1);
        }


        matcher = s2.matcher(street);
        if (matcher.matches()) {
            return Tuples.of(replaceNationsNotEmpty(matcher.group(1)), 1);
        }

        return Tuples.of(street, -1);
    }


    public static String replaceNations(String ncity) {
        for (String y : NATIONS) {
            ncity = ncity.replace(y, "");
            if (y.length() > 2) {
                String replace = y.replaceAll("族", "");
                ncity = ncity.replace(replace, "");
            }
        }
        return ncity;
    }


    public static String replaceNationsNotEmpty(String name) {
        String x = name;
        for (String y : NATIONS) {
            String x2 = x.replace(y, "");
            if (y.length() > 2) {
                String replace = y.replaceAll("族", "");
                x2 = x2.replace(replace, "");
            }
            x = x2.isEmpty() ? x : x2;
        }
        return x;
    }

    public static String shortCity(String city) {
        return shortCityImp(city).get0();
    }

    public static String shortDistrict(String district) {
        return shortDistrictImp(district).get0();
    }

    public static String shortStreet(String street) {
        return shortStreetImp(street).get0();
    }

    //  def shortAdmin(name: String, level: DistrictLevel): String = {
    //    level match {
    //      case DistrictLevel.Province => AdminUtils1.shortProvince(name)
    //      case DistrictLevel.City => AdminUtils1.shortCity(name)
    //      case DistrictLevel.District => AdminUtils1.shortDistrict(name)
    //      case DistrictLevel.Street => AdminUtils1.shortStreet(name)
    //      case _ => name
    //    }
    //  }

    //def shortAdmin(name: String, level: DistrictLevel)
    public static String shortAdmin(String name, DistrictLevel level) {
//        if (DistrictLevel.Province().equals(level)) {
//            return shortProvince(name);
//        } else if (DistrictLevel.City().equals(level)) {
//            return shortCity(name);
//        } else if (DistrictLevel.District().equals(level)) {
//            return shortDistrict(name);
//        } else if (DistrictLevel.Street().equals(level)) {
//            return shortStreet(name);
//        }
        return name;
    }
}
