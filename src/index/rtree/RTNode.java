package index.rtree;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static signatures.Function.sortMapByValue;

public abstract class RTNode {

    public RTNode parent;       // root.parent = null
    public int level;           // level of LeafNode = 0
                                // level of root = the height of tree
    public Rectangle mbr;

    /**
     * Aggregated signature
     * recording each point's max TF-IDF value around its children-nodes
     * if it is null, then it is not a WR-tree node
     */
    public Map<Integer, Double> aggregator;

    public RTNode(RTNode _parent, int _level) {
        this.parent = _parent;
        this.level = _level;
        this.mbr = new Rectangle();
        this.aggregator = null;
    }

    public boolean isLeaf() {
        return this.level == 0;
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    public Set<Integer> getPointSet(){
        if(this.aggregator == null){
            throw new IllegalArgumentException("Error in RTNode.getPointSet: it is not a WR-tree node.");
        }
        return this.aggregator.keySet();
    }

    public void updateAggregator(Map<Integer, Double> _pid2TFIDF) {
        _pid2TFIDF.forEach((pid, value) ->{
            if(value > aggregator.getOrDefault(pid, 0.0)){
                aggregator.put(pid, value);
            }
        });
        sortMapByValue(aggregator);
    }

    // check if this node has extra space to add a new child
    public boolean reachMaxCapacity(int capacity){
        return this.getChildNum() + 1 > capacity;
    }

    public abstract int getChildNum();
    public abstract List getChildren();
    public abstract Object getChildByID(int idx);

    /**
     * for each rectangle pair (r1,r2)
     *      compute the union area that can cover them
     *      increment = Area(union) - Area(r1) - Area(r2) + Area(intersection)
     *
     * after check all pairs,
     * choose the pair with the largest increment as seeds, indicating minimum overlap
     */
    public int[] pickSeeds(List<Rectangle> rectangles) {

        double maxIncrement = Double.NEGATIVE_INFINITY;
        int idx1 = 0, idx2 = 0;
        int num = rectangles.size();

        double[] allArea = new double[num];

        for (int i = 0; i < num - 1; i++) {
            Rectangle mbr1 = rectangles.get(i);
            double area1 = getArea(allArea, i, mbr1);

            for (int j = i + 1; j < num; j++) {
                Rectangle mbr2 = rectangles.get(j);

                Rectangle union = mbr1.getUnionRectangle(mbr2);
                double intersectArea = mbr1.getIntersectArea(mbr2);

                double  d = union.computeArea() - area1 - getArea(allArea, j, mbr2) + intersectArea;

                if (d > maxIncrement) {
                    maxIncrement = d;
                    idx1 = i;
                    idx2 = j;
                }
            }
        }

        return new int[] { idx1, idx2 };
    }

    /**
     * quadratic algorithm to split an overflow node's children,
     * according to the pair with min overlap
     */
    public int[] quadraticSplit(List<Rectangle> children, int[] seedIdx, int minSize) {

        int childNum = children.size();
        int maxSize = childNum / 2 + 1;

        int[] groupIdx = new int[childNum];
        int GROUP_1 = 1, GROUP_2 = 2;
        groupIdx[seedIdx[0]] = GROUP_1;
        groupIdx[seedIdx[1]] = GROUP_2;
        int unAssigned = childNum - 2;
        int g1 = 1, g2 = 1;

        Rectangle mbr1 = new Rectangle(children.get(seedIdx[0]));
        Rectangle mbr2 = new Rectangle(children.get(seedIdx[1]));
        double area1 = mbr1.computeArea();
        double area2 = mbr2.computeArea();
        double a1, a2;
        while (unAssigned > 0){
            if (g1 + unAssigned == minSize || g2 == maxSize){
                for(int i = 0; i < childNum; i++){
                    if(groupIdx[i] == 0) {
                        groupIdx[i] = GROUP_1;
                    }
                }
                break;
            }
            else if(g2 + unAssigned == minSize || g1 == maxSize){
                for(int i = 0; i < childNum; i++){
                    if(groupIdx[i] == 0) {
                        groupIdx[i] = GROUP_2;
                    }
                }
                break;
            }
            else {
                double maxDiff = -1;
                int selectIdx = -1;
                double diff1, diff2, diff;
                Rectangle r1, r2, rec;
                for(int i = 0; i < childNum; i++){
                    if(groupIdx[i] == 0) {
                        rec = children.get(i);
                        r1 = mbr1.getUnionRectangle(rec);
                        diff1 = r1.computeArea() - area1;

                        r2 = mbr2.getUnionRectangle(rec);
                        diff2 = r2.computeArea() - area2;

                        // keep the maximum increment
                        diff = Math.abs(diff1 - diff2);
                        if (diff > maxDiff) {
                            maxDiff = diff;
                            selectIdx = i;
                        }
                    }
                }

                // after scanning, the next node is determined
                Rectangle selectRec = children.get(selectIdx);
                r1 = mbr1.getUnionRectangle(selectRec);
                a1 = r1.computeArea();
                diff1 = a1 - area1;

                r2 = mbr2.getUnionRectangle(selectRec);
                a2 = r2.computeArea();
                diff2 = a2 - area2;

                boolean assignToGroup1 = true;
                if (diff1 > diff2) {
                    assignToGroup1 = false;
                }
                else if (diff1 == diff2){
                    if (area1 > area2) {
                        assignToGroup1 = false;
                    }
                    else if (area1 == area2) {
                        if (g1 > g2) {
                            assignToGroup1 = false;
                        }
                    }
                }

                if(assignToGroup1){
                    groupIdx[selectIdx] = GROUP_1;
                    mbr1.enlargeRectangle(selectRec);
                    area1 = a1;
                    g1++;
                }
                else {
                    groupIdx[selectIdx] = GROUP_2;
                    mbr2.enlargeRectangle(selectRec);
                    area2 = a2;
                    g2++;
                }
                unAssigned--;
            }
        }
        return groupIdx;
    }


    // avoid repeat computation of area
    private double getArea(double[] area, int idx, Rectangle mbr){

        if(area[idx] == 0) {
            area[idx] =  mbr.computeArea();
        }
        return area[idx];
    }
}
