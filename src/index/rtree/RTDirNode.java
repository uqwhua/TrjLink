package index.rtree;

import java.util.ArrayList;
import java.util.List;

public class RTDirNode extends RTNode{

    public List<RTNode> childNodes;    // aka children pointers

    public RTDirNode(RTNode parent, int level) {
        super(parent, level);
        childNodes = new ArrayList<>();
    }

    public int getChildNum(){
        if(childNodes == null){
            throw new IllegalStateException("Error in WRTDirNode.getChildNum: childNodes is null.");
        }
        return childNodes.size();
    }

    public List<RTNode> getChildren(){
        if(childNodes == null){
            throw new IllegalStateException("Error in WRTDirNode.getChildren: childNodes is null.");
        }
        return childNodes;
    }

    public RTNode getChildByID(int index) {
        return childNodes.get(index);
    }

    // during construction, consider the spatial enlargement
    public int chooseBestChild_LeastEnlargement(Rectangle mbr){

        int bestChildIdx = -1;
        double minAreaIncrement = Double.POSITIVE_INFINITY;

        for(int i = 0; i < childNodes.size(); i++){
            RTNode curNode = childNodes.get(i);
            Rectangle cur = curNode.mbr;
            double curArea = cur.computeArea();
            Rectangle union = cur.getUnionRectangle(mbr);

            double enlargement = union.computeArea() - curArea;

            if(enlargement < minAreaIncrement){
                minAreaIncrement = enlargement;
                bestChildIdx = i;
            }
            else if(enlargement == minAreaIncrement){
                // the same enlargement, then choose the one with smaller self-area
                RTNode curBest = childNodes.get(bestChildIdx);
                bestChildIdx = (curBest.mbr.computeArea() < curArea) ? bestChildIdx : i;
            }
        }

        return bestChildIdx;
    }
}
