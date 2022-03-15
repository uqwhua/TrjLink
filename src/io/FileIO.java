package io;

import main.Main;
import basic.*;

import java.io.*;
import java.util.*;
import java.sql.Timestamp;

public class FileIO {

    // read the data from file, meanwhile, split the data by odd/even dates
    public static double readRawDataset(final String folderName, final int totalObject,
                                        Vector<Trajectory> oddData, Vector<Trajectory> evenData) throws IOException {
        File folder = new File(folderName);
        if (!folder.exists()) {
            throw new FileNotFoundException();
        }
        final int idPos = 0, timePos = 1, lngPos = 2, latPos = 3;   // revised if using a different dataset
        final int minTrajLen = 500;
        int n = 0;
        double avgLength = 0;
        for (int i = 1; i < 10357; i++) {
            String filename = folderName + i + ".txt";
            final BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = "", id = "0";
            int totalPoint = 0;
            List<ComplexPoint> oddPoints = new ArrayList<>(), evenPoints = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                id = tokens[idPos];
                Timestamp t = Timestamp.valueOf(tokens[timePos]);
                int day = t.toLocalDateTime().getDayOfMonth();
                float lng = Float.parseFloat(tokens[lngPos]), lat = Float.parseFloat(tokens[latPos]);
                long second = t.getTime() / 1000;
                ComplexPoint point = new ComplexPoint(lng, lat, second);
                if (day % 2 == 0) {  // split dataset by the date of odd or even
                    evenPoints.add(point);
                }
                else {
                    oddPoints.add(point);
                }
                totalPoint++;
            }
            if(totalPoint >= minTrajLen ) {
                avgLength += totalPoint;
                if (!oddPoints.isEmpty() && !evenPoints.isEmpty()) {
                    int idd = Integer.parseInt(id);
                    oddData.add(new Trajectory(idd, idd, oddPoints));
                    evenData.add(new Trajectory(idd, idd, evenPoints));
                    if (++n == totalObject) {
                        break;
                    }
                }
            }
        }
        return avgLength / totalObject;
    }


    // a simple mapmatching:
    // to align each point to its nearest neighbour vertex on the road network
    public static Grid buildGridIndex(final String inputFile, final int maxNodeId,
                                      final float lng_step, final float lat_step) {
        Grid grid = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String s;
            float lng_max = Float.NEGATIVE_INFINITY, lng_min = Float.POSITIVE_INFINITY;
            float lat_max = Float.NEGATIVE_INFINITY, lat_min = Float.POSITIVE_INFINITY;
            Set<SimplePoint> pointSet = new HashSet<>();
            while ((s = br.readLine()) != null) {
                String[] tokens = s.split(",");
                int vid = Integer.parseInt(tokens[0]);
                String[] lnglat = tokens[1].split(" ");
                float lng = Long.parseLong(lnglat[0]) / 10000000f;
                float lat = Long.parseLong(lnglat[1]) / 10000000f;
                SimplePoint p = new SimplePoint(vid, lng, lat);
                Main.NNid2lnglat.put(vid, p);

                // for grid construction
                pointSet.add(p);
                lng_max = Math.max(lng_max, lng);
                lng_min = Math.min(lng_min, lng);
                lat_max = Math.max(lat_max, lat);
                lat_min = Math.min(lat_min, lat);
            }
            br.close();

            grid = new Grid(lng_min, lng_max, lat_min, lat_max, lng_step, lat_step);
            grid.addVertices(pointSet); // the vertices of road network
            grid.maxNodeId = maxNodeId;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return grid;
    }
}
