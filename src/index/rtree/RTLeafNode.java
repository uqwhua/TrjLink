package index.rtree;

import java.util.ArrayList;
import java.util.List;

public class RTLeafNode extends RTNode {

    public List<RTLeafElement> elements;

    public RTLeafNode(RTNode parent){
        super(parent, 0);
        this.elements = new ArrayList<>();
    }

    public int getChildNum(){
        if(this.elements == null){
            throw new IllegalArgumentException("Error in RTLeafNode.getChildNum: element list is null.");
        }
        return this.elements.size();
    }

    public List<RTLeafElement> getChildren(){
        if(this.elements == null){
            throw new IllegalArgumentException("Error in RTLeafNode.getChildren: element list is null.");
        }
        return this.elements;
    }

    public RTLeafElement getChildByID(int idx){
        if(elements == null || idx >= elements.size()) {
            throw new IllegalArgumentException("Error in RTLeafNode.getChildByID.");
        }
        return elements.get(idx);
    }

    // different with WRTLeafNode
    void addElement(RTLeafElement elem) {
        if(elements == null){
            elements = new ArrayList<>();
        }
        if(elements.isEmpty()){        // the first element of this leaf node
            this.mbr = new Rectangle(elem.mbr);
        }
        else {      // updateAggregator its mbr
            this.mbr.enlargeRectangle(elem.mbr);
        }
        elements.add(elem);
    }

}
