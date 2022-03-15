package index.rtree;

import index.AscendNeighbor;
import index.Calculate;
import basic.SimplePoint;

import java.util.*;

import static index.GeoHash.getGeoHash;


public abstract class RTree {
    // This value indicates # of child nodes of an internal node
    // is in the range of [capacity/FanoutRatio, capacity].
    public int fanoutRatio;

    public int leafCapacity;   // min num of elements in one leaf node = leafCapacity / FanoutRatio
    public int branchFactor;

    public static int dimension = 2;   // data dimension, currently fixed

    public RTNode root;
    public int rootLev;
    public int entityNum;   // # of taxis

    // empty tree, only initialize parameters
    public RTree(int _branchFactor, int _leafCapacity, int _fanoutRatio) {
        branchFactor = _branchFactor;
        leafCapacity = _leafCapacity;
        fanoutRatio = _fanoutRatio;
        root = null;
    }

    public abstract void insertElement(RTLeafElement toInsertElement);
    public abstract void overflowHandler(RTNode overflowNode, RTNode newBrotherNode);
    public abstract Queue<AscendNeighbor> findKNN(Map<Integer, Double> querySignature, int numOfK);

    // insertion-based construction method
    public void constructRTree(List<RTLeafElement> elementList) {
        if (this.root != null || elementList == null) {
            throw new IllegalArgumentException("Error in RTree.constructWRTree!");
        }

        this.entityNum = elementList.size();      // = # of taxi for WRtree and 2d-Rtree
        if (entityNum == 0) {
            System.out.println("Warning in constructRTree: The given element list is empty!");
            return;
        }

        // ========= these elements only need one node as the root
        if (entityNum <= this.leafCapacity) {
            RTLeafNode _root = new RTLeafNode(null);
            elementList.forEach(_root::addElement);
            this.root = _root;
            return;
        }

        // ========= need more than one parent nodes
        elementList.sort((o1, o2) -> {
            // two elements have the same MBR, then compare # of points
            if (o1.mbr.equals(o2.mbr)) {
                return Integer.compare(o1.getPointSetSize(), o2.getPointSetSize());
            }
            else { // compare the GeoHash of center points
                SimplePoint p1 = o1.mbr.getCenterPoint();
                SimplePoint p2 = o2.mbr.getCenterPoint();
                return getGeoHash(p1).compareTo(getGeoHash(p2));
            }
        });

        elementList.forEach(this::insertElement);        // then insert them one by one
    }

    public void scanLeafNode(final Rectangle queryMBR, final Map<Integer, Double> querySignature,
                             final List<RTLeafElement> candidates, final int numOfK, Queue<AscendNeighbor> NNqueue) {
        candidates.forEach(elem -> {
            if (queryMBR.isIntersection(elem.mbr)) {
                // calculate exact similarity between two signatures
                float similarity = Calculate.cosineSimilarity(querySignature, elem.signature);
                if (similarity > 0) {
                    updateNNqueue(NNqueue, numOfK, elem.entityID, similarity);
                }
            }
        });
    }


    // it is useful if the top-k Nearest Neighbors are required
    public static void updateNNqueue(Queue<AscendNeighbor> NNqueue, final int numOfK, final String taxiID, final float similarity) {

        // case-1: NNqueue isn't full, just add it
        if (NNqueue.size() < numOfK) {
            NNqueue.add(new AscendNeighbor(taxiID, similarity));
            return;
        }

        // case-2
        // has already found enough Nearest Neighbors so far
        // but this similarity >= current min similarity
        assert NNqueue.peek() != null;
        float minSimilarity = NNqueue.peek().similarity;
        if (similarity >= minSimilarity) {

            if (similarity > minSimilarity) {     // if they are equal, reserve both
                NNqueue.poll();                 // else remove top-K NN with min similarity
            }
            NNqueue.add(new AscendNeighbor(taxiID, similarity));
        }
    }

    public void sortRTLeafElements(List<RTLeafElement> elements, final int fromIdx, final int elementNum, int targetDim) {
        List<RTLeafElement> tmpList = new ArrayList<>(elements.subList(fromIdx, fromIdx + elementNum));
        int dimIndex = targetDim - 1;

        tmpList.sort((elem1, elem2) -> {
            double coord1 = elem1.mbr.getCoord_BottomLeft(dimIndex) + elem1.mbr.getCoord_TopRight(dimIndex);
            double coord2 = elem2.mbr.getCoord_BottomLeft(dimIndex) + elem2.mbr.getCoord_TopRight(dimIndex);
            return Double.compare(coord1, coord2);
        });

        for(int i = 0; i < elementNum; i++){
            elements.set(fromIdx + i, tmpList.get(i));
        }
    }

    public void sortRTNodes(Vector<RTNode> nodeList, int fromIdx, int nodeNum, int targetDim) {

        List<RTNode> tmpList = new ArrayList<>(nodeList.subList(fromIdx, fromIdx + nodeNum));
        int dimIndex = targetDim - 1;

        tmpList.sort((node1, node2) -> {
            double coord1 = node1.mbr.getCoord_BottomLeft(dimIndex) + node1.mbr.getCoord_TopRight(dimIndex);
            double coord2 = node2.mbr.getCoord_BottomLeft(dimIndex) + node2.mbr.getCoord_TopRight(dimIndex);
            return Double.compare(coord1, coord2);
        });

        for (int i = 0; i < nodeNum; i++) {
            nodeList.set(fromIdx + i, tmpList.get(i));
        }
    }

}
