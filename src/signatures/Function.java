package signatures;

import basic.Grid;
import basic.Trajectory;

import java.util.*;


public class Function {

    public static Map<String, Map<Integer, Double>> constructSignature(final Vector<Trajectory> trajectories,
                                                                       final Grid grid, final String sigType,
                                                                       final int gramLen, final int binSize, final int gridNum) {
        // Step-1: get "dim2count" for different types of signature accordingly
        Map<String, Map<Integer, Double>> signatures = new HashMap<>();
        boolean cosineSimilarity = true;
        switch (sigType) {
            case "spatial" -> signatures = Spatial.constructSpatial(trajectories, grid);
            case "sequential", "seq" -> signatures = Sequential.constructSequential(trajectories, grid, gramLen);
            case "temporal", "time" -> {
                cosineSimilarity = false;   // we use EMD for temporal signature based linking
                signatures = Temporal.constructTemporal(trajectories, binSize);
            }
            case "spatiotemporal", "spatio-temporal", "st" -> signatures = Spatiotemporal.constructSignature(trajectories, grid, gridNum);
        }

        // Step-2: finalize, suitable for cosine similarity measurement
        if(cosineSimilarity)
            fromCount2TFIDF(signatures);

        return signatures;
    }

    public static void fromCount2TFIDF(Map<String, Map<Integer, Double>> user2point2count){
        // from count to frequency, meanwhile counting for IDF
        Map<Integer, Double> point2IDF = new HashMap<>();
        user2point2count.forEach((id, point2cnt) -> {
            double total = point2cnt.values().stream().mapToDouble(c -> c).sum();
            point2cnt.replaceAll((p, c) -> c /= total);
            point2cnt.forEach((vid, cnt) -> point2IDF.compute(vid, (k, v) -> v == null ? 1 : ++v));
        });

        finalizeSignature(point2IDF, user2point2count);
    }

    private static void finalizeSignature(Map<Integer, Double> point2IDF, Map<String, Map<Integer, Double>> signatures) {
        // to obtain the IDF vector
        int totalObjects = signatures.size();
        point2IDF.replaceAll((vid, cnt) -> cnt = Math.log((totalObjects + 1) * 1.0 / cnt));

        // finalize the TF-IDF-based signature
        signatures.forEach((u, sig) -> {
            // compute the complete TF-IDF signatures
            sig.replaceAll((vid, tf) -> tf *= point2IDF.get(vid));

            // normalization
            double denominator = Math.sqrt(sig.values().stream().mapToDouble(v -> v * v).sum());
            sig.replaceAll((vid, tfidf) -> tfidf /= denominator);
            sortMapByValue(sig);    // hereafter, it's sorted and normalized
        });
    }

    public static void sortMapByValue(Map<Integer, Double> map) {
        List<Map.Entry<Integer, Double>> sortedEntryList = new ArrayList<>(map.entrySet());
        sortedEntryList.sort((me1, me2) -> {
            return me2.getValue().compareTo(me1.getValue());    // if m2 > m1, return 1,
            // so they should be swapped, it means m2 is in front
        });

        map.clear();
        sortedEntryList.forEach(entry -> map.put(entry.getKey(), entry.getValue()));
    }

    // oriVector is sorted
    public static Map<Integer, Double> cutSignature(Map<Integer, Double> oriVector, int cutoff) {
        if(oriVector == null) {
            return null;
        }

        if (cutoff == 0 || cutoff >= oriVector.size()) {
            return oriVector;
        }

        Map<Integer, Double> newVector = new LinkedHashMap<>();
        double denominator = 0;
        int i = 0;
        for (int pid : oriVector.keySet()) {
            double value = oriVector.get(pid);
            denominator += value * value;
            newVector.put(pid, value);
            i++;
            if (i >= cutoff)
                break;
        }
        // normalization again
        final double finalDenominator = Math.sqrt(denominator);
        newVector.replaceAll((p, v) -> v /= finalDenominator);
        return newVector;
    }
}
