package main;

import index.AscendNeighbor;
import index.rtree.RTLeafElement;
import index.wrtree.WRTree;
import signatures.temporalEMD.EMDmetric;

import java.util.*;

import static signatures.Function.cutSignature;

public class Linking {

    public static int[] linearExecute(final Map<String, Map<Integer, Double>> oddSignatures,
                                      final Map<String, Map<Integer, Double>> evenSignatures,
                                      final int cutoff, final int topK, final String sigType) {
        System.out.println("[INFO] Start Linear Linking ...");
        int[] success = new int[topK + 1];
        Arrays.fill(success, 0);

        final boolean cosine = !sigType.equalsIgnoreCase("time"); // for temporal signature, we use EMD as the metric
        Map<String, Map<Integer, Double>> reducedSig_even = new HashMap<>();    // to avoid repeated computation
        for (Map.Entry<String, Map<Integer, Double>> oddEntry : oddSignatures.entrySet()) {
            String oddObject = oddEntry.getKey();
            Map<Integer, Double> oddSig = cutSignature(oddEntry.getValue(), cutoff);  // the full-length signatures have already been sorted

            if (oddSig != null && !oddSig.isEmpty()) {
                // if top-K NNs are required
                Queue<AscendNeighbor> NNqueue = new PriorityQueue<>();

                // or top-1
                double maxSim = Double.NEGATIVE_INFINITY;
                String matchObj = "";

                for (Map.Entry<String, Map<Integer, Double>> evenEntry : evenSignatures.entrySet()) {
                    String evenObject = evenEntry.getKey();
                    Map<Integer, Double> evenSig = reducedSig_even.compute(evenObject, (k, v) -> v == null ?
                            cutSignature(evenEntry.getValue(), cutoff) : v);
                    if (evenSig != null && !evenSig.isEmpty()) {
                        float similarity = (float) (cosine ? computeCosineSimilarity(oddSig, evenSig) : EMDmetric.computeSimilarity(oddSig, evenSig));
                        if (similarity > 0) {
                            if (topK == 1) {
                                if (similarity > maxSim) {
                                    maxSim = similarity;
                                    matchObj = evenObject;
                                } else if (similarity == maxSim && oddObject.equals(evenObject)) {
                                    matchObj = evenObject;
                                }
                            } else {
                                updateNNqueue(NNqueue, topK, evenObject, similarity);
                            }
                        }
                    }
                }

                if (topK == 1) {
                    if (matchObj.equals(oddObject)) {
                        success[topK]++;
                    }
                } else {
                    checkNNqueue(NNqueue, oddObject, topK, success);
                }
            }
        }

        return success;
    }

    public static int[] rtreeBased(final Map<String, Map<Integer, Double>> oddSignatures,
                                   final Map<String, Map<Integer, Double>> evenSignatures,
                                   final int cutoff, final int topK, final int capacity, final int fanoutRatio) {
        // prepare the list for tree construction
        // I always build the tree based on the even set and use the odd set as query
        // it can be reversed as what you expected
        // just make sure it is consistent when comparing with other methods
        List<RTLeafElement> elementList = new ArrayList<>();
        evenSignatures.forEach((taxi, sig) -> elementList.add(new RTLeafElement(taxi, cutSignature(sig, cutoff))));

        WRTree tree = new WRTree(capacity, capacity, fanoutRatio);
        tree.constructRTree_STR(elementList);

        System.out.println("[INFO] Start WR-tree based Linking ...");
        int[] success = new int[topK + 1];
        Arrays.fill(success, 0);

        for (String taxi : oddSignatures.keySet()) {        // query one by one
            Map<Integer, Double> sig = oddSignatures.get(taxi);
            Queue<AscendNeighbor> neighbors = tree.findKNN(cutSignature(sig, cutoff), topK);
            if (neighbors != null) {
                checkNNqueue(neighbors, taxi, topK, success);
            }
        }

        elementList.clear();
        return success;
    }

    /* if the query taxi can be found in final NNqueue, our linking is accurate */
    private static void checkNNqueue(final Queue<AscendNeighbor> NNqueue, final String queryTaxi,
                                     final int topK, int[] success) {

        Stack<AscendNeighbor> stack = new Stack<>();    // to reverse the NN queue
        Queue<AscendNeighbor> kNN_copy = new PriorityQueue<>(NNqueue);
        while (!kNN_copy.isEmpty()) {
            AscendNeighbor neighbor = kNN_copy.poll();   // poll the one with minimal similarity
            stack.push(neighbor);
        }

        AscendNeighbor[] candidates = new AscendNeighbor[topK + 1];
        Arrays.fill(candidates, null);
        int k = 1;
        while (!stack.isEmpty() && k <= topK) {
            if (k == 1) {
                candidates[k++] = stack.pop();
            } else {
                AscendNeighbor nn = stack.pop();
                if (candidates[k - 1].similarity == nn.similarity) {
                    if (nn.entityID.equals(queryTaxi)) {
                        candidates[k - 1] = nn;
                    }
                } else {
                    candidates[k++] = nn;
                }
            }
        }
        // check candidates
        for (k = topK; k >= 1; k--) {
            if (candidates[k] != null && candidates[k].entityID.equals(queryTaxi)) {
                for (int j = topK; j >= k; j--) {
                    success[j]++;
                }
                break;
            }
        }
    }


    // it is useful if the top-k Nearest Neighbors are required
    private static void updateNNqueue(Queue<AscendNeighbor> NNqueue, final int numOfK, final String taxiID, final float similarity) {

        // case-1: NNqueue isn't full, just add it
        if (NNqueue.size() < numOfK) {
            NNqueue.add(new AscendNeighbor(taxiID, similarity));
            return;
        }

        // case-2
        // has already found enough Nearest Neighbors so far
        // but this similarity >= current min similarity
        assert NNqueue.peek() != null;
        double minSimilarity = NNqueue.peek().similarity;
        if (similarity >= minSimilarity) {
            if (similarity > minSimilarity) {     // if they are equal, reserve both
                NNqueue.poll();                 // else remove top-K NN with min similarity
            }
            NNqueue.add(new AscendNeighbor(taxiID, similarity));
        }
    }

    // cosine similarity is equal to dot product (as the signatures are already normalized and sorted)
    // NOTE: in the paper of ICDE'19, we used Jaccard similarity for sequential signature
    //   while in TKDE'20, we found that it is more effective to construct TFIDF-based signatures along with cosine similarity for sequential signature
    private static double computeCosineSimilarity(final Map<Integer, Double> v1, final Map<Integer, Double> v2) {
        final List<Double> similarity = new ArrayList<>();
        v1.forEach((p, v) -> similarity.add(v2.getOrDefault(p, 0.0) * v));
        return similarity.stream().mapToDouble(v -> v).sum();
    }
}
