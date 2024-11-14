package com.ming.geolib;

import com.ming.geolib.utils.GeoUtils;
import com.ming.geolib.utils.Point;
import com.virjar.geolib.GeoTrans;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class BuildGeohashAdminCode {
    public static void main(String[] args) {
        Queue<String> queue = new LinkedList<>();
        HashMap<String, Integer> map = new HashMap<>();

        String seed = "wtw1vy";
        queue.add(seed);

        while (!queue.isEmpty()) {
            String gh = queue.poll();

            if (map.containsKey(gh)) {
                continue;
            }

            Point pt = GeoUtils.decode(gh);
            if (GeoUtils.outOfChina(pt)) {
                continue;
            }

            int adminCode = GeoTrans.resolveAdminCode(pt.getLongitude(), pt.getLatitude());
            if (adminCode < 0) {
                continue;
            }

            // add to hash map
            map.put(gh, adminCode);

            for (CharSequence neighbor : GeoUtils.neighbors(gh)) {
                queue.add(neighbor.toString());
            }

            // show progress
            if (map.size() % 10000 == 0) {
                System.out.println("Map size: " + map.size());
            }
        }

        // write file
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Paths.get("geohash.ser")))) {
            oos.writeObject(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
