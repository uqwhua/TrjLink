package index;

public class AscendNeighbor implements Comparable{

    public String entityID;
    public float similarity;                // exact similarity between two signatures


    public AscendNeighbor(String _entityID, float _similarity){
        this.entityID = _entityID;
        this.similarity = _similarity;
    }

    @Override
    public int compareTo(Object obj) {
        if(obj instanceof AscendNeighbor a){
            return Float.compare(this.similarity, a.similarity);      // if this.sim < a.sim, return -1
                                                                      // indicate an ascending ordering
        }
        throw new IllegalArgumentException("Error in AscendNeighbor.compareTo: Not an AscendNeighbor.");
    }
}
