package index;

import java.util.Map;

public class Calculate {

    // if two vectors are normalized, then their dot product = cosine similarity
    public static float cosineSimilarity(Map<Integer, Double> vector1, Map<Integer, Double> vector2) {
        if(vector1.size() < vector2.size()){
            return dotProduct(vector1, vector2);
        }
        else {
            return dotProduct(vector2, vector1);
        }
    }

    /** signatureVec :    a TF-IDf normalized vector for a specific taxi
     *  aggregatedVec:    for nodes, recording each inside point's max TF-IDF which haven't been normalized
     *  @return their upper bound
     */
    public static float upperBound(Map<Integer, Double> signatureVec, Map<Integer, Double> aggregatedVec){
        return dotProduct(signatureVec, aggregatedVec);
    }

    public static float dotProduct(final Map<Integer, Double> shortVector, final Map<Integer, Double> longVector) {
        float dotProduct = 0;
        for(int pid: shortVector.keySet()){
            Double value = longVector.get(pid);
            if(value != null){
                dotProduct += shortVector.get(pid) * value;
            }
        }
        return dotProduct;
    }
}
