package signatures;

import signatures.temporalEMD.EMDmetric;
import basic.Trajectory;

import java.sql.Timestamp;
import java.util.*;

public class Temporal {
    public static Map<String, Map<Integer, Double>> constructTemporal(final Vector<Trajectory> trajectories, final int timeWindowSize) {

        EMDmetric.binSize = timeWindowSize;
//        System.out.println("[INFO] Construct temporal signatures");
//        System.out.printf( "\t time-window size = %dh, vector dimension = %d\n", timeWindowSize, 24 / timeWindowSize);

        Map<String, Map<Integer, Double>> id2hour2pointCnt = prepareForTemporal(trajectories);

        Map<String, Map<Integer, Double>> id2bin2count = new HashMap<>();
        id2hour2pointCnt.forEach((id, counts) -> {
            Map<Integer, Double> vec = transformToBin(counts, timeWindowSize);
            if(!vec.isEmpty())
                id2bin2count.put(id, vec);
        });
        return id2bin2count;
    }

    private static Map<String, Map<Integer, Double>> prepareForTemporal(final Vector<Trajectory> trajectories) {
        Map<String, Map<Integer, Double>> user2hour2pointCnt = new HashMap<>();
        trajectories.forEach(traj -> {
            String id = traj.get_trajectoryId() + "";
            Map<Integer, Double> hour2pointCnt = user2hour2pointCnt.compute(id, (k, v) -> v == null ? new LinkedHashMap<>() : v);
            for(int i = 0, len = traj.get_length(); i < len; i++) {
                Timestamp timestamp = new Timestamp(traj.get_point_by_idx(i).get_exactTime() * 1000);
                int hour = timestamp.toLocalDateTime().getHour();
                hour2pointCnt.compute(hour, (k, v) -> v == null ? 1 : ++v);
            }
        });
        return user2hour2pointCnt;
    }

    private static Map<Integer, Double> transformToBin(final Map<Integer, Double> hour2pointCnt, final int binSize) {
        final double sum = hour2pointCnt.values().stream().mapToDouble(c -> c).sum();

        Map<Integer, Double> bin2tf = new LinkedHashMap<>();
        int startHour = 0;
        while(startHour < 24){
            double pointCnt = 0;
            // within the current time window
            for(int i = 0; i < binSize; i++){
                int hour = startHour + i;
                pointCnt += hour2pointCnt.getOrDefault(hour, 0.0);  // accumulate
            }
            if(pointCnt > 0) {
                bin2tf.put(startHour, pointCnt/sum);
//                bin2tf.put(startHour, pointCnt);
            }
            startHour += binSize;
        }

        Function.sortMapByValue(bin2tf);
        return bin2tf;
    }
}
