package signatures;

import main.Main;
import org.apache.commons.lang3.tuple.Pair;
import basic.ComplexPoint;
import basic.Grid;
import basic.SimplePoint;
import basic.Trajectory;

import java.sql.Timestamp;
import java.util.*;

public class Spatiotemporal {
    public static Grid gridST = null;
    public static Map<Pair<Integer, Integer>, Integer> dimension2Id = null;     // each spatiotemporal dimension is denoted as a pair of (pointID/gridID, hour)
    public static Map<Integer, Pair<Integer, Integer>> id2dimension = null;
    private static final int temporalDimension = 24;

    public static Map<String, Map<Integer, Double>> constructSignature(final Vector<Trajectory> trajectories,
                                                                       final Grid grid, final int gridNum) {
        Map<String, Map<Integer, Double>> signatures = new HashMap<>();

        Map<String, Map<Integer, Integer[]>> id2point2hour2cnt = prepareForST(trajectories, grid);

        Map<Integer, Integer> pointID2gridID = null;
        if (gridNum > 0) {    //e.g., 100, 200, 300
            pointID2gridID = new HashMap<>();
            gridST = gridPartition(gridNum, grid, id2point2hour2cnt, pointID2gridID);
        }

        if(dimension2Id == null)
            dimension2Id = new HashMap<>(); // first time

//        System.out.println("[PROGRESS] Transform to point-based count vectors");
        transformDimension(id2point2hour2cnt, pointID2gridID, signatures);
//        System.out.println("\t # of distinct dimension = " + dimension2Id.size());

        if(id2dimension == null) {
            id2dimension = new HashMap<>();
        }
        dimension2Id.forEach((dim, id) -> id2dimension.put(id, dim));

        return signatures;
    }

    // to get TF-based vectors
    private static void transformDimension(final Map<String, Map<Integer, Integer[]>> taxi2pid2hour2cnt,
                                           final Map<Integer, Integer> pointID2gridID,
                                           Map<String, Map<Integer, Double>> taxi2signatures) {
        taxi2pid2hour2cnt.forEach((taxi, pidList) -> {
            Map<Integer, Double> dim2count = new LinkedHashMap<>();   // to be the base of signature later
            // scan the points belonging to current tax
            pidList.forEach((pid, hours) -> {
                for (int h = 0; h < temporalDimension; h++) {
                    if (hours[h] > 0) {   // the occurrence time > 0
                        int gid = -1;
                        if(pointID2gridID != null){
                            gid = pointID2gridID.get(pid);
                        }

                        Pair<Integer, Integer> dim = Pair.of((gid == -1 ? pid : gid), h);
                        final int nextId = dimension2Id.size();
                        Integer dimId = dimension2Id.compute(dim, (k, v) -> v == null ? nextId : v);
                        dim2count.put(dimId, hours[h] * 1.0);
                    }
                }
            });
            if(!dim2count.isEmpty())
                taxi2signatures.put(taxi, dim2count);
        });
    }

    private static Map<String, Map<Integer, Integer[]>> prepareForST(final Vector<Trajectory> trajectories,
                                                                     final Grid grid) {
        Map<String, Map<Integer, Integer[]>> id2point2hour2cnt = new HashMap<>();
        trajectories.forEach(traj -> {
            String id = traj.get_trajectoryId() + "";

            Map<Integer, Integer[]> pid2hour2cnt = id2point2hour2cnt.compute(id, (k, v) -> v == null ? new HashMap<>() : v);
            for (int i = 0, len = traj.get_length(); i < len; i++) {
                ComplexPoint p = traj.get_point_by_idx(i);
                final int vid = grid.getNearestID(new SimplePoint(p.getLongitude(), p.getLatitude()));
                if(vid > 0) {
                    Integer[] hour2cnt = pid2hour2cnt.get(vid);
                    if (hour2cnt == null) {
                        hour2cnt = new Integer[temporalDimension];
                        Arrays.fill(hour2cnt, 0);
                    }
                    Timestamp timestamp = new Timestamp(p.get_exactTime() * 1000);
                    int hour = timestamp.toLocalDateTime().getHour();
                    hour2cnt[hour]++;
                    pid2hour2cnt.put(vid, hour2cnt);
                }
            }
        });
        return id2point2hour2cnt;
    }

    private static Grid gridPartition(final int gridNum, final Grid grid, final Map<String, Map<Integer, Integer[]>> taxi2pid2hourlyPointCnt,
                                      Map<Integer, Integer> pointID2gridID) {
        Set<Integer> pointSet = new HashSet<>();
        taxi2pid2hourlyPointCnt.values().stream().map(Map::keySet).forEach(pointSet::addAll);
//        System.out.println("[REPORT] # of occurrence points = " + pointSet.size());

//        double lng_max = Double.NEGATIVE_INFINITY, lng_min = Double.POSITIVE_INFINITY;
//        double lat_max = Double.NEGATIVE_INFINITY, lat_min = Double.POSITIVE_INFINITY;
//
//        for (int pid : pointSet) {
//            SimplePoint p = LinkingAttack.NNid2lnglat.get(pid);
//            lng_max = Math.max(lng_max, p.getLongitude());
//            lng_min = Math.min(lng_min, p.getLongitude());
//            lat_max = Math.max(lat_max, p.getLatitude());
//            lat_min = Math.min(lat_min, p.getLatitude());
//        }
//        Grid newGrid = new Grid(lng_min, lng_max, lat_min, lat_max);

        Grid newGrid = new Grid(grid);  // the spatial range is the same, but with different partition
        newGrid.setNum(gridNum);

        pointSet.forEach(pid -> {
            SimplePoint p = Main.NNid2lnglat.get(pid);
            int gid = (int) newGrid.get_gridId_by_lnglat(p.getLongitude(), p.getLatitude());
            pointID2gridID.put(pid, gid);
        });

        return newGrid;
    }

    public static void reset() {
        id2dimension = null;
        dimension2Id = null;
    }
}
