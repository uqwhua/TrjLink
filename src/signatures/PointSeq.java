package signatures;

import index.rtree.Rectangle;
import main.Main;
import basic.SimplePoint;

import java.util.LinkedList;
import java.util.List;

public class PointSeq {
    private final List<Integer> pointIdSeq;
    public boolean SeqSort = true;

    public PointSeq() {
        pointIdSeq = new LinkedList<>();
    }

    public void addPoint(int vertexId) {
        pointIdSeq.add(vertexId);
    }

    public boolean isEmpty(){
        return pointIdSeq.isEmpty();
    }

    public int size() {
        return pointIdSeq.size();
    }

    public int getPointByIdx(int i) {
        if (i < pointIdSeq.size()) {
            return pointIdSeq.get(i);
        }
        throw new IndexOutOfBoundsException("PointSeq.getPointByIdx()");
    }

    public void sort() {
        if (SeqSort && pointIdSeq.size() > 1) {
            pointIdSeq.sort(Integer::compareTo);
        }
    }

    public Rectangle getRectangle() {
        boolean isFirstPoint = true;
        Rectangle rectangle = new Rectangle();
        for(int pid: pointIdSeq){
            SimplePoint point = Main.NNid2lnglat.get(pid);
            if (isFirstPoint) {
                rectangle.bottomLeft = new SimplePoint(point);
                rectangle.topRight = new SimplePoint(point);
                isFirstPoint = false;
            }
            else {
                Rectangle rec = new Rectangle(point, point);
                rectangle.enlargeRectangle(rec);
            }
        }
        return rectangle;
    }

    @Override
    public int hashCode() {
        this.sort();
        int result = 0;
        for (int id : pointIdSeq) {
            result = 31 * result + id;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PointSeq ps)) {
            return false;
        }
        if (ps.size() != this.size()) {
            return false;
        }

        this.sort();
        ps.sort();
        for (int i = 0; i < pointIdSeq.size(); i++) {
            if (!pointIdSeq.get(i).equals(ps.getPointByIdx(i))) {
                return false;
            }
        }
        return true;
    }
}
