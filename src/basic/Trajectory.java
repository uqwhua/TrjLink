package basic;

import java.util.ArrayList;
import java.util.List;

public class Trajectory {

    // basic thing, every model needs it
    private final List<ComplexPoint> pointSeq;

    private final int userID;
    private final int tripID;     // one user may have multiple trips

    // default constructor
    public Trajectory(){
        pointSeq = new ArrayList<>();
        userID = -1;
        tripID = -1;
    }

    public Trajectory(final int uid, final int tid, List<ComplexPoint> seq) {
        pointSeq = seq;
        userID = uid;
        tripID = tid;
    }

    public int get_userId() {
        return userID;
    }

    public int get_trajectoryId() {
        return tripID;
    }

    public int get_length() {
        return pointSeq.size();
    }

    public ComplexPoint get_point_by_idx(int idx) {
        if (idx < pointSeq.size()) {
            return pointSeq.get(idx);
        }
        return null;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result += result * 31 + tripID;
        result += result * 31 + pointSeq.size();
        for (ComplexPoint geoPoint : pointSeq) {
            result += result * 31 + geoPoint.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Trajectory trj))
            return false;

        if (trj.tripID != this.tripID || trj.get_length() != pointSeq.size())
            return false;

        for (int i = 0; i < pointSeq.size(); i++) {
            if (!pointSeq.get(i).equals(trj.get_point_by_idx(i))) {
                return false;
            }
        }
        return true;
    }
}
