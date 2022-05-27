package com.dengxq.lnglat2Geo.test;

import com.dengxq.lnglat2Geo.GeoTrans;
import com.dengxq.lnglat2Geo.GeoTransImpl;
import com.dengxq.lnglat2Geo.entity.Admin;
import com.dengxq.lnglat2Geo.entity.enums.CoordinateSystem;
import com.dengxq.lnglat2Geo.entity.Location;
import com.dengxq.lnglat2Geo.loader.BinLoader;
import com.dengxq.lnglat2Geo.loader.GeoData;
import com.dengxq.lnglat2Geo.loader.ILoader;
import com.dengxq.lnglat2Geo.utils.AdminUtils;
import com.speedment.common.tuple.Tuple3;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Test {
    public static void main(String[] args) {
        //loaderTest();
        testCase();
    }

    private static void loaderTest() {
        GeoData geoData = ILoader.Storage.getOrLoad();
        //new JSONLoader().dump(new File("src/main/resources/data.json"), geoData);
        new BinLoader().dump(new File("src/main/resources/data_bak.bin"), geoData);
//        //数据dump到json文件中
//        new JSONLoader().dump(new File("src/main/resources/json/"), geoData);
//        ILoader.Storage.setLoader(new JSONLoader());
//        new BinLoader().dump(new File("src/main/resources/data.bin"), geoData);
    }

    private static void testCase() {
        long start = System.currentTimeMillis();
        ILoader.Storage.getOrLoad();
        System.out.println("data load cost = " + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        Map<Long, List<Tuple3<Long, Integer, Integer>>> boundaryData = GeoTransImpl.boundaryData;
        System.out.println("data init cost = " + (System.currentTimeMillis() - start));

        shortCityTest();
        System.out.println("\n\n\n");
        normalizeNameTest();
        System.out.println("\n\n\n");
        determineAdminTest();
    }


    private static void determineAdminTest() {


        long start1 = System.currentTimeMillis();
        System.out.println(GeoTrans.determineAdmin(119.718017578125, 36.25756282630298, CoordinateSystem.GCJ02, true));
        System.out.println("determineAdmin cost  = " + (System.currentTimeMillis() - start1));


        String str = "112.989382,28.147062;109.046404,35.042294;106.559531,29.607832;119.481842,33.652686;116.525612,39.824004;109.090599,35.080281;113.508112,37.892087;123.417829,41.791227;120.517459,30.459049;113.865295,35.290525;110.290043,20.015764;108.934191,34.294362;117.183897,34.264914;126.587992,45.757869;115.859063,28.695778;106.771075,26.584885; 108.92224,34.233088;113.809742,23.067213;118.778811,32.089465;113.715261,35.2587";
        for (String s : str.split(";")) {
            String[] parts = s.split(",");
            Admin ss = GeoTrans.determineAdmin(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])
                    , CoordinateSystem.WGS84, true);
            System.out.println(ss);
        }


        Location[] testLocation = new Location[]{
                new Location(121.1572265625, 23.9260130330), // 中国台湾省南投县仁爱乡
                new Location(112.567757, 35.096176), // 济源
                new Location(116.9565868378, 39.6513677208), // 天津市武清区河西务镇
                new Location(100.4315185547, 21.7594997307), // 中国云南省西双版纳傣族自治州勐海县勐混镇
                new Location(85.5670166016, 41.5548386631), // 中国新疆维吾尔自治区巴音郭楞蒙古自治州库尔勒市 普惠乡
                new Location(117.9969406128, 27.7447712551), // 中国福建省南平市武夷山市 崇安街道
                new Location(110.8520507813, 34.0526594214), // 河南省三门峡市卢氏县 瓦窑沟乡下河村
                new Location(116.4811706543, 39.9255352817), // 北京市朝阳区 六里屯街道甜水园
                new Location(116.3362348080, 40.0622912084), // 北京市昌平区 回龙观地区吉晟别墅社区
                new Location(116.3362830877, 40.0594500522), // 北京市北京市昌平区 建材城西路65号
                new Location(116.3325601816, 40.0397393499), // 北京市海淀区 清河街道
                new Location(117.0977783203, 36.5085323575), // 山东省济南市历城区
                new Location(118.6358642578, 35.8356283889), // 山东省临沂市沂水县
                new Location(119.7853088379, 36.3029520437), // 山东省潍坊市高密市柏城镇
                new Location(119.8567199707, 36.2808142593), // 山东省青岛市胶州市胶西镇
                new Location(120.3892135620, 36.2777698228), // 山东省青岛市城阳区流亭街道于家社区
                new Location(120.152983, 36.119759), // 海外
                new Location(98.774694, 23.706633) // 海外
        };

        Admin[] testResult = new Admin[]{
                Admin.createProvincialCity("台湾省", "南投县", 710008, 710008, new Location(0.0, 0.0)),
                Admin.createProvincialCity("河南省", "济源市", 419001, 419001, new Location(112.602256, 35.067199)),
                Admin.createDistrict("天津市", "天津城区", "武清区", 120100, 120100, 120114, new Location(117.044387, 39.384119)),
                Admin.createDistrict("云南省", "西双版纳傣族自治州", "勐海县", 532800, 532800, 532822, new Location(100.452547, 21.957353)),
                Admin.createDistrict("新疆维吾尔自治区", "巴音郭楞蒙古自治州", "库尔勒市", 652800, 652800, 652801, new Location(86.174633, 41.725891)),
                Admin.createDistrict("福建省", "南平市", "武夷山市", 350700, 350700, 350782, new Location(118.035309, 27.756647)),
                Admin.createDistrict("河南省", "三门峡市", "卢氏县", 411200, 411200, 411224, new Location(111.047858, 34.054324)),
                Admin.createDistrict("北京市", "北京城区", "朝阳区", 110100, 110100, 110105, new Location(116.443205, 39.921506)),
                Admin.createDistrict("北京市", "北京城区", "昌平区", 110100, 110100, 110114, new Location(116.231254, 40.220804)),
                Admin.createDistrict("北京市", "北京城区", "昌平区", 110100, 110100, 110114, new Location(116.231254, 40.220804)),
                Admin.createDistrict("北京市", "北京城区", "海淀区", 110100, 110100, 110108, new Location(116.298262, 39.95993)),
                Admin.createDistrict("山东省", "济南市", "历城区", 370100, 370100, 370112, new Location(117.06523, 36.680259)),
                Admin.createDistrict("山东省", "临沂市", "沂水县", 371300, 371300, 371323, new Location(118.627917, 35.79045)),
                Admin.createDistrict("山东省", "潍坊市", "高密市", 370700, 370700, 370785, new Location(119.755597, 36.382594)),
                Admin.createDistrict("山东省", "青岛市", "胶州市", 370200, 370200, 370281, new Location(120.033382, 36.26468)),
                Admin.createDistrict("山东省", "青岛市", "城阳区", 370200, 370200, 370214, new Location(120.396256, 36.307559)),
                Admin.createDistrict("海外", "海外", "海外", -1, -1, -1, new Location(120.396256, 36.307559)),
                Admin.createDistrict("海外", "海外", "海外", -1, -1, -1, new Location(120.396256, 36.307559))
        };
        System.out.println("\n\n\n");

        for (int i = 0; i < testLocation.length; i++) {
            Location location = testLocation[i];
            Admin result = testResult[i];

            Admin admin = GeoTrans.determineAdmin(location.lng, location.lat, CoordinateSystem.GCJ02, true);
            System.out.println(admin);
            System.out.println(result);

            if (!(Objects.equals(admin.province, result.province))) {
                throw new AssertionError();
            }
            if (!(Objects.equals(admin.city, result.city))) {
                throw new AssertionError();
            }
            if (!(admin.cityCode == result.cityCode)) {
                throw new AssertionError();
            }
            if (!(Objects.equals(admin.district, result.district))) {
                throw new AssertionError();
            }
            if (!(admin.districtCode == result.districtCode)) {
                throw new AssertionError();
            }
        }

    }

    private static void normalizeNameTest() {
        for (Admin admin : GeoTrans.normalizeName("", "攀枝花", "", "", false)) {
            System.out.println(admin);
        }
        for (Admin admin : GeoTrans.normalizeName("", "北京", "海淀", "", false)) {
            System.out.println(admin);
        }

    }


    private static void shortCityTest() {
        String[] testSource1 = new String[]{"襄樊市", "恩施州", "昌吉回族自治州", "海北藏族自治州", "克孜勒苏柯尔克孜自治州", "文山壮族苗族自治州", "海西蒙古族藏族自治州", "海南藏族自治州", "博尔塔拉蒙古自治州", "西双版纳傣族自治州", "玉树藏族自治州", "果洛藏族自治州", "怒江傈僳族自治州", "迪庆藏族自治州", "楚雄彝族自治州", "大理白族自治州", "德宏傣族景颇族自治州", "黄南藏族自治州", "湘西土家族苗族自治州", "伊犁哈萨克自治州", "延边朝鲜族自治州", "红河哈尼族彝族自治州", "黔西南布依族苗族自治州", "恩施土家族苗族自治州", "黔南布依族苗族自治州", "临夏回族自治州", "甘孜藏族自治州", "黔东南苗族侗族自治州", "凉山彝族自治州", "甘南藏族自治州", "阿坝藏族羌族自治州", "巴音郭楞蒙古自治州"};
        String[] testResult1 = new String[]{"襄阳", "恩施", "昌吉", "海北", "克孜勒苏", "文山", "海西", "海南", "博尔塔拉", "西双版纳", "玉树", "果洛", "怒江", "迪庆", "楚雄", "大理", "德宏", "黄南", "湘西", "伊犁", "延边", "红河", "黔西南", "恩施", "黔南", "临夏", "甘孜", "黔东南", "凉山", "甘南", "阿坝", "巴音郭楞"};

        String[] testSource2 = new String[]{"嘉模堂区", "新竹县", "嘉义市", "风顺堂区", "花王堂区", "云林县", "台北市", "望德堂区", "圣方济各堂区", "屏东县", "南投县", "桃园县", "花地玛堂区", "台中市", "大堂区", "新竹市", "基隆市", "彰化县", "苗栗县", "台南市", "台东县", "宜兰县", "澎湖县", "嘉义县", "路凼填海区", "花莲县", "高雄市", "新北市", "三沙市", "昆玉市", "铁门关市", "湾仔区", "油尖旺区", "中西区", "双河市", "东区", "观塘区", "九龙城区", "屯门区", "深水埗区", "黄大仙区", "葵青区", "南区", "荃湾区", "沙田区", "大埔区", "西贡区", "海口市", "东沙群岛", "可克达拉市", "舟山市", "珠海市", "拉萨市", "阳泉市", "长治市", "三亚市", "运城市", "山南市", "盘锦市", "临汾市", "汕头市", "忻州市", "银川市", "克拉玛依市", "哈密市", "石嘴山市", "晋中市", "乌鲁木齐市", "朔州市", "林芝市", "吕梁市", "上海城区", "昌吉回族自治州", "鞍山市", "潮州市", "阿克苏地区", "苏州市", "吐鲁番市", "深圳市", "连云港市", "晋城市", "邯郸市", "海北藏族自治州", "本溪市", "昌都市", "大同市", "揭阳市", "日喀则市", "石家庄市", "大兴安岭地区", "临沧市", "韶关市", "那曲地区", "和田地区", "辽阳市", "克孜勒苏柯尔克孜自治州", "文山壮族苗族自治州", "阿勒泰地区", "邢台市", "太原市", "营口市", "南通市", "衡水市", "鄂州市", "鹤岗市", "海西蒙古族藏族自治州", "海南藏族自治州", "大庆市", "盐城市", "博尔塔拉蒙古自治州", "七台河市", "莱芜市", "葫芦岛市", "泰州市", "松原市", "东营市", "白城市", "西双版纳傣族自治州", "昆明市", "抚顺市", "玉树藏族自治州", "阿里地区", "枣庄市", "锦州市", "保山市", "黄石市", "宿迁市", "西宁市", "果洛藏族自治州", "无锡市", "扬州市", "徐州市", "海东市", "随州市", "大连市", "肇庆市", "沧州市", "淮安市", "丹东市", "广州市", "河源市", "汕尾市", "朝阳市", "普洱市", "厦门市", "秦皇岛市", "白山市", "宁波市", "阜新市", "丽江市", "伊春市", "怒江傈僳族自治州", "菏泽市", "濮阳市", "四平市", "威海市", "江门市", "孝感市", "聊城市", "迪庆藏族自治州", "鸡西市", "楚雄彝族自治州", "廊坊市", "云浮市", "鹤壁市", "温州市", "张家界市", "大理白族自治州", "镇江市", "景德镇市", "铁岭市", "株洲市", "泰安市", "吴忠市", "德宏傣族景颇族自治州", "黄南藏族自治州", "萍乡市", "漯河市", "惠州市", "齐齐哈尔市", "梅州市", "唐山市", "中卫市", "塔城地区", "清远市", "台州市", "黑河市", "淮北市", "铜陵市", "淮南市", "佛山市", "莆田市", "阳江市", "烟台市", "湖州市", "周口市", "玉溪市", "娄底市", "漳州市", "淄博市", "滨州市", "永州市", "常州市", "嘉兴市", "新余市", "潍坊市", "安庆市", "承德市", "济南市", "南昌市", "喀什地区", "咸宁市", "九江市", "通化市", "鹰潭市", "临沂市", "双鸭山市", "湘西土家族苗族自治州", "济宁市", "保定市", "德州市", "北海市", "固原市", "荆门市", "商丘市", "衡阳市", "伊犁哈萨克自治州", "湘潭市", "焦作市", "泉州市", "十堰市", "新乡市", "哈尔滨市", "日照市", "延边朝鲜族自治州", "曲靖市", "宜昌市", "辽源市", "天津城区", "亳州市", "合肥市", "抚州市", "安阳市", "西安市", "衢州市", "郴州市", "蚌埠市", "宿州市", "芜湖市", "牡丹江市", "岳阳市", "襄阳市", "黄山市", "昭通市", "荆州市", "宜春市", "黄冈市", "红河哈尼族彝族自治州", "南京市", "咸阳市", "绍兴市", "阜阳市", "安顺市", "开封市", "吉安市", "池州市", "绥化市", "许昌市", "宝鸡市", "六安市", "益阳市", "张家口市", "平顶山市", "黔西南布依族苗族自治州", "常德市", "龙岩市", "郑州市", "驻马店市", "铜川市", "马鞍山市", "防城港市", "湛江市", "赣州市", "沈阳市", "上饶市", "南平市", "恩施土家族苗族自治州", "渭南市", "佳木斯市", "金华市", "邵阳市", "长春市", "南阳市", "三门峡市", "桂林市", "黔南布依族苗族自治州", "长沙市", "安康市", "延安市", "武汉市", "六盘水市", "吉林市", "商洛市", "福州市", "贺州市", "北京城区", "青岛市", "信阳市", "洛阳市", "三明市", "柳州市", "丽水市", "雅安市", "崇左市", "宣城市", "金昌市", "滁州市", "汉中市", "怀化市", "贵港市", "贵阳市", "梧州市", "钦州市", "遵义市", "攀枝花市", "茂名市", "南宁市", "杭州市", "玉林市", "眉山市", "河池市", "张掖市", "乐山市", "临夏回族自治州", "白银市", "武威市", "天水市", "百色市", "成都市", "来宾市", "乌海市", "甘孜藏族自治州", "巴中市", "毕节市", "黔东南苗族侗族自治州", "酒泉市", "榆林市", "凉山彝族自治州", "内江市", "遂宁市", "铜仁市", "德阳市", "陇南市", "广元市", "宜宾市", "资阳市", "绵阳市", "甘南藏族自治州", "巴彦淖尔市", "广安市", "定西市", "阿坝藏族羌族自治州", "庆阳市", "泸州市", "平凉市", "巴音郭楞蒙古自治州", "自贡市", "重庆城区", "呼和浩特市", "包头市", "兰州市", "宁德市", "南充市", "达州市", "通辽市", "阿拉善盟", "兴安盟", "锡林郭勒盟", "重庆郊县", "赤峰市", "乌兰察布市", "鄂尔多斯市", "呼伦贝尔市"};

        for (int i = 0; i < testSource1.length; i++) {
            String s = AdminUtils.shortCityImp(testSource1[i]).get0();
            System.out.println(testSource1[i] + "->" + s);
            if ((!s.equals(testResult1[i])))
                throw new AssertionError();
        }

        System.out.println("\n\n\n");

        for (String test : testSource2) {
            String shortCity = AdminUtils.shortCityImp(test).get0();
            System.out.println(test + "->" + shortCity);
//            if ((!shortCity.equals(test)))
//                throw new AssertionError();
        }
    }


}
