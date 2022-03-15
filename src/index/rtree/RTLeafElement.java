package index.rtree;

import java.util.Map;
import java.util.Set;

// the entity stored in the leaf node
public class RTLeafElement {

    public String entityID;
    public Rectangle mbr;
    public Map<Integer, Double> signature;     // TF-IDF vector for this taxi

    public RTLeafElement(String _taxiID, Map<Integer, Double> _pointID2TFIDF){
        this.entityID = _taxiID;
        this.mbr = new Rectangle(_pointID2TFIDF.keySet());
        this.signature = _pointID2TFIDF;
    }

    public Set<Integer> getPointSet() {
        return this.signature.keySet();
    }

    public int getPointSetSize(){
        return getPointSet().size();    // # of unique points in the signature
    }
}
