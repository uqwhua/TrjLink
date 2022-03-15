package signatures;

import basic.Grid;
import basic.SimplePoint;
import basic.Trajectory;

import java.util.*;

public class Sequential {

    public static Map<PointSeq, Integer> qgram2Id = null;
    public static Map<Integer, PointSeq> id2qgram = null;

    public static Map<String, Map<Integer, Double>> constructSequential(final Vector<Trajectory> trajectories,
                                                                        final Grid grid, final int gramLength) {
        // initialize two indexing containers
        if(qgram2Id == null) {
            qgram2Id = new HashMap<>();
        }
        if(id2qgram == null) {
            id2qgram = new HashMap<>();
        }

        Map<String, Map<Integer, Double>> user2ngram2freq = new HashMap<>();
        trajectories.forEach(traj -> {
            Map<Integer, Double> ngram2freq = new LinkedHashMap<>();
            for(int i = 0, len = traj.get_length(); i <= len - gramLength; i++){
                // compose n-gram
                PointSeq ngram = new PointSeq();
                int j = 0;
                int vid_prev = -1;
                while (ngram.size() < gramLength && i + j < len){
                    SimplePoint p = traj.get_point_by_idx(i + j);
                    int vid = grid.getNearestID(new SimplePoint(p.getLongitude(), p.getLatitude())); // filter duplicate
                    if(vid >= 0) {   // valid point

                        // avoid duplicate
                        if(vid_prev < 0 || vid_prev != vid) {
                            ngram.addPoint(vid);
                        }
                        vid_prev = vid;
                        ngram.addPoint(vid);
                    }
                    j++;
                }

                if(!ngram.isEmpty()) {
                    ngram.sort();   // no specific ordering among a ngram

                    final int nextId = qgram2Id.size();
                    int gramId = qgram2Id.compute(ngram, (k, v) -> v == null ? nextId : v);
                    id2qgram.put(gramId, ngram);

                    // count its frequency
                    ngram2freq.compute(gramId, (k, v) -> v == null ? 1 : ++v);
                }
            }

            if(!ngram2freq.isEmpty()) {

                // whether group by userID
                String id = traj.get_trajectoryId() + "";
                user2ngram2freq.put(id, ngram2freq);
            }
        });

        return user2ngram2freq;
    }

    public static void reset() {
        id2qgram = null;
        qgram2Id = null;
    }
}
