package index.wrtree;

import index.rtree.RTLeafElement;
import index.rtree.RTLeafNode;
import index.rtree.RTNode;
import index.rtree.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WRTLeafNode extends RTLeafNode {

    protected WRTLeafNode(RTNode parent){
        super(parent);
        this.aggregator = new HashMap<>();
    }

    void addElement(RTLeafElement elem) {
        if(elements == null){
            elements = new ArrayList<>();
        }
        if(elements.isEmpty()){        // it is the first element of this leaf node
            this.mbr = new Rectangle(elem.mbr);
            this.aggregator = new HashMap<>(elem.signature);
        }
        else {      // update its mbr and aggregator
            this.mbr.enlargeRectangle(elem.mbr);
            updateAggregator(elem.signature);
        }
        elements.add(elem);
    }

    WRTLeafNode splitAndShare(RTLeafElement toInsertElement, int minSize) {
        int curElemNum = elements.size();

        // copy all original child elements
        List<RTLeafElement> children = new ArrayList<>();
        List<Rectangle> rectangles = new ArrayList<>();
        for (RTLeafElement child : elements) {
            children.add(child);
            rectangles.add(child.mbr);
        }
        // don't forget to add new one
        children.add(toInsertElement);
        rectangles.add(toInsertElement.mbr);

        int[] seedIdx = pickSeeds(rectangles);
        int[] groupIdx = quadraticSplit(rectangles, seedIdx, minSize);

        // reset this node and create a new brother node
        resetLeafNode();
        WRTLeafNode brotherNode = new WRTLeafNode(this.parent);

        for(int i = 0; i < curElemNum + 1; i++){
            if(groupIdx[i] == 1){
                this.addElement(children.get(i));
            } else {
                brotherNode.addElement(children.get(i));
            }
        }

        return brotherNode;
    }

    private void resetLeafNode() {
        this.elements = new ArrayList<>();
        this.aggregator = new HashMap<>();
        this.mbr = new Rectangle();
    }
}