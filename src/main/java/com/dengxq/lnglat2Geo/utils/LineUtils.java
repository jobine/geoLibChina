package com.dengxq.lnglat2Geo.utils;

public class LineUtils {
    public static Double lineDis(Double x1, Double y1, Double x2, Double y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public static Double pointToLineDis(Double x1, Double y1, Double x2, Double y2, Double x0, Double y0) {
        double
                a = lineDis(x1, y1, x2, y2),// 线段的长度
                b = lineDis(x1, y1, x0, y0),// 点到起点的距离
                c = lineDis(x2, y2, x0, y0);// 点到终点的距离
        //点在端点上
        if (c <= 0.000001 || b <= 0.000001) {
            return 0D;
        }
        //直线距离过短
        if (a <= 0.000001) {
            return b;
        }
        // 点在起点左侧，距离等于点到起点距离
        if (c * c >= a * a + b * b) {
            return b;
        }
        //点在终点右侧，距离等于点到终点距离
        if (b * b >= a * a + c * c) {
            return c;
        }
        //点在起点和终点中间，为垂线距离
        double k = (y2 - y1) / (x2 - x1);
        double z = y1 - k * x1;
        double p = (a + b + c) / 2;
        // 半周长
        double s = Math.sqrt(p * (p - a) * (p - b) * (p - c));//海伦公式求面积
        return 2 * s / a;// 返回点到线的距离（利用三角形面积公式求高）

    }
}
