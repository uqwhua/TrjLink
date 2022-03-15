package index.wrtree;

import index.rtree.RTDirNode;
import index.rtree.RTLeafElement;
import index.rtree.RTNode;
import index.rtree.Rectangle;

import java.util.*;

public class WRTDirNode extends RTDirNode {

    WRTDirNode(RTNode parent, int level) {
        super(parent, level);
        this.aggregator = new HashMap<>();
    }

    /**
     * Add a child node into this internal node
     * no worries about overflow. it should be always safe.
     *
     * @param newChild, either WRTDirNode or WRTLeafNode
     */
    void addChildNode(RTNode newChild){
        if(this.childNodes == null){
            this.childNodes = new ArrayList<>();
        }

        // updateAggregator the MBR and aggregator
        if (this.childNodes.isEmpty()) {      // this is an empty node
            this.mbr = new Rectangle(newChild.mbr);

            this.aggregator = new HashMap<>(newChild.aggregator);
        }
	    else {
            this.mbr.enlargeRectangle(newChild.mbr);
            updateAggregator(newChild.aggregator);
        }

        this.childNodes.add(newChild);
        newChild.parent = this;
    }

    /**
     * this internal node would overflow if this child node were to be inserted
     * so need a new brother node to share its child nodes and toInsertNode together
     */
    WRTDirNode splitAndShare(RTNode toInsertNode, int minSize) {

        int curNodeNum = childNodes.size();

        List<RTNode> children = new ArrayList<>();
        List<Rectangle> rectangles = new ArrayList<>();
        for(int i = 0 ; i < curNodeNum; i++){
            RTNode child = this.getChildByID(i);
            children.add(child);
            rectangles.add(child.mbr);
        }
        children.add(toInsertNode);
        rectangles.add(toInsertNode.mbr);

        // currently just consider spatial overlap
        // further consider common point number
        int[] seedIdx = pickSeeds(rectangles);
        int[] groupIdx = quadraticSplit(rectangles, seedIdx, minSize);

        // reset this node and create a new brother node
        resetDirNode();
        WRTDirNode brotherNode = new WRTDirNode(this.parent, this.level);
        for(int i = 0; i < curNodeNum + 1; i++){
            if(groupIdx[i] == 1){
                this.addChildNode(children.get(i));
            } else {
                brotherNode.addChildNode(children.get(i));
            }
        }
        return brotherNode;
    }

    private void resetDirNode() {
        this.childNodes.clear();
        this.aggregator = new HashMap<>();
        this.mbr = new Rectangle();
    }

    // we expect to insert this element to a leaf node, their common points should be as many as possible
    // only for WR-tree
    int chooseBestChild_MostCommonPoint(final RTLeafElement toInsertElement) {
        int bestChildIdx = -1;
        int maxCommonPointNum = Integer.MIN_VALUE;
        Rectangle mbr = toInsertElement.mbr;
        Set<Integer> intersection;
        for(int i = 0, childNum = getChildNum(); i < childNum; i++){
            RTNode curChild = this.childNodes.get(i);
            if(curChild.mbr.isIntersection(mbr)){            // two MBRs have spatial overlapping

                // here maybe time-consuming, but it is acceptable, cz it's building state
                intersection = new HashSet<>(toInsertElement.getPointSet());
                intersection.retainAll(curChild.getPointSet());
                int commonPointNum = intersection.size();

                if(commonPointNum > 0){
                    if(maxCommonPointNum < commonPointNum){
                        maxCommonPointNum = commonPointNum;
                        bestChildIdx = i;
                    }
                    else if(maxCommonPointNum == commonPointNum){

                        // if the same common point number, then pick the one whose self-area is smaller
                        double a1 = curChild.mbr.computeArea();
                        double a2 = getChildByID(bestChildIdx).mbr.computeArea();

                        if(a1 < a2){
                            bestChildIdx = i;
                        }
                    }
                }
            }
        }

        return bestChildIdx;
    }

}