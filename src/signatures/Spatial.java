package signatures;

import basic.Grid;
import basic.SimplePoint;
import basic.Trajectory;

import java.util.*;

public class Spatial {

    /**
     * @param data         the raw trajectories of moving objects
     * @param grid         stores the vertices of the road network; could be added some outlier points
     * @return the complete full-length signatures without reduction
     */
    public static Map<String, Map<Integer, Double>> constructSpatial(final Vector<Trajectory> data, final Grid grid) {
        Map<String, Map<Integer, Double>> id2point2count = new HashMap<>();

        // transform from raw coordinates to the node-Id on the road network
        data.forEach(traj -> {
            Map<Integer, Double> sig = new LinkedHashMap<>();   // !!! should support sorting (when reduction)
            for (int i = 0, len = traj.get_length(); i < len; i++) {
                SimplePoint p = traj.get_point_by_idx(i);
                int vid = grid.getNearestID(new SimplePoint(p.getLongitude(), p.getLatitude()));    // align each point to its nearest road intersection
                if(vid >= 0)
                    sig.compute(vid, (k, v) -> v == null ? 1 : ++v);    // point frequency increases
                // else the point is out of range
            }
            if(!sig.isEmpty()) {
                id2point2count.put(traj.get_trajectoryId() + "", sig);
            }
        });

        return id2point2count;
    }
}
