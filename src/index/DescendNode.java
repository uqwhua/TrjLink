package index;

import index.rtree.RTNode;

public class DescendNode implements Comparable{

    public RTNode node;
    public float upperBound; // the upper bound between a signature and this node's aggregator

    public DescendNode(RTNode _node, float _upperBound){
        node = _node;
        upperBound = _upperBound;
    }

    @Override
    public int compareTo(Object obj) {
        if(obj instanceof DescendNode a){
            return Float.compare(a.upperBound, this.upperBound);      //descending order
        }

        throw new IllegalArgumentException("Error in DescendNode.compareTo: Not a DescendNode.");
    }
}