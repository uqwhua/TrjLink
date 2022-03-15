package signatures.temporalEMD;

import java.util.*;

public class EMDmetric {

    public static int binSize = 0;
    public static int dimension;

    public static double computeSimilarity(Map<Integer, Double> signature_odd, Map<Integer, Double> signature_even){
        if(binSize == 0){
            throw new IllegalArgumentException("[ERROR] No bin size assigned!");
        }
        dimension = 24 / binSize;
        return 1 - distance(signature_odd, signature_even, 0);
    }

    private static double distance(Map<Integer, Double> signature1, Map<Integer, Double> signature2, double extraMassPenalty){

        double[][] costMatrix = new double[dimension][];
        for(int i = 0; i < dimension; i++){
            costMatrix[i] = new double[dimension];
        }

        for(int i = 0; i < dimension; i++){
            for(int j = 0; j < dimension; j++){
                double value = Math.abs(i - j) * binSize;
                if(value <= 12){
                    costMatrix[i][j] = value / 12;
                }
                else {
                    costMatrix[i][j] = (24 - value) / 12;
                }
            }
        }

        return emdHat(toDoubleArray(signature1), toDoubleArray(signature2), costMatrix, extraMassPenalty);
    }

    public static double[] toDoubleArray(Map<Integer, Double> sig) {
        double[] array = new double[dimension];
        for(int h = 0; h < dimension; h++){
            int startHour = h * binSize;
            array[h] = sig.getOrDefault(startHour, 0.0);
        }
        return array;
    }


    /**
     * @param P,Q two signatures
     * @param extraMassPenalty 0
     * @return pairwise emd-distance
     *
     * Note: it should be (0,1), then similarity = 1 - dist
     *       otherwise sth wrong
     */

    static private double emdHat(double[] P, double[] Q, double[][] distMatrix,
                                 double extraMassPenalty) {

        // This condition should hold: ( 2^(sizeof(CONVERT_TO_T*8)) >= ( MULT_FACTOR^2 )
        // Note that it can be problematic to check it because of overflow problems.
        // I simply checked it with Linux calc which has arbitrary precision.
        double MULT_FACTOR = 1000000;

        // initialize the input
        long[] iP = new long[dimension];
        long[] iQ = new long[dimension];
        long[][] iC = new long[dimension][];
        for(int i = 0 ; i < dimension; i++){
            iC[i] = new long[dimension];
        }

        // Converting to CONVERT_TO_T
        double sumP = 0.0;
        double sumQ = 0.0;
        double maxC = distMatrix[0][0];
        for (int i = 0; i < dimension; i++) {
            sumP += P[i];
            sumQ += Q[i];
            for (int j = 0; j < dimension; j++) {
                if (distMatrix[i][j] > maxC)
                    maxC = distMatrix[i][j];
            }
        }

        double minSum = Math.min(sumP, sumQ);
        double maxSum = Math.max(sumP, sumQ);
        double normFactor_PQ = MULT_FACTOR / maxSum;
        double normFactor_C = MULT_FACTOR / maxC;
        for (int i = 0; i < dimension; i++) {
            iP[i] = (long) (Math.floor(P[i] * normFactor_PQ + 0.5));
            iQ[i] = (long) (Math.floor(Q[i] * normFactor_PQ + 0.5));

            for (int j = 0; j < dimension; j++) {
                iC[i][j] = (long) (Math.floor(distMatrix[i][j] * normFactor_C + 0.5));
            }
        }

        // computing distance without extra mass penalty
        double dist = emdHatImplLongLongInt(iP, iQ, iC, 0);

        // unnormalize
        dist = dist / normFactor_PQ;
        dist = dist / normFactor_C;

        // adding extra mass penalty
        if (extraMassPenalty == -1)
            extraMassPenalty = maxC;

        dist += (maxSum - minSum) * extraMassPenalty;

        return dist;
    }

    static private long emdHatImplLongLongInt(long[] Pc, long[] Qc, long[][] C, long extraMassPenalty) {

        // Ensuring that the supplier - P, have more mass.
        // Note that we assume here that C is symmetric
        Vector<Long> P;
        Vector<Long> Q;
        long absDiffSumPSumQ;
        long sumP = 0;
        long sumQ = 0;
        for (int i = 0; i < dimension; i++)
            sumP += Pc[i];
        for (int i = 0; i < dimension; i++)
            sumQ += Qc[i];

        if (sumQ > sumP) {
            P = transfer(dimension, Qc);
            Q = transfer(dimension, Pc);
            absDiffSumPSumQ = sumQ - sumP;
        }
        else {
            P = transfer(dimension, Pc);
            Q = transfer(dimension, Qc);
            absDiffSumPSumQ = sumP - sumQ;
        }

        // creating the b vector that contains all vertexes
        Vector<Long> b = new Vector<>();
        for (int i = 0; i < 2 * dimension + 2; i++) {
            b.add(0L);
        }
        int THRESHOLD_NODE = 2 * dimension;
        int ARTIFICIAL_NODE = 2 * dimension + 1; // need to be last !
        for (int i = 0; i < dimension; i++) {
            b.set(i, P.get(i));
        }
        for (int i = dimension; i < 2 * dimension; i++) {
            b.set(i, Q.get(i - dimension));
        }

        // remark*)
        // I put here a deficit of the extra mass,
        // as mass that flows to the threshold node can be absorbed from all sources with cost zero
        // (this is in reverse order from the paper,
        // where incoming edges to the threshold node had the cost of the threshold and outgoing
        // edges had the cost of zero)
        // This also makes sum of b zero.
        b.set(THRESHOLD_NODE, -absDiffSumPSumQ);
        b.set(ARTIFICIAL_NODE, 0L);

        long maxC = 0;
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                assert (C[i][j] >= 0);
                if (C[i][j]> maxC)
                    maxC = C[i][j];
            }
        }
        if (extraMassPenalty == -1)
            extraMassPenalty = maxC;

        Set<Integer> sourcesThatFlowNotOnlyToThresh = new HashSet<>();
        Set<Integer> sinksThatGetFlowNotOnlyFromThresh = new HashSet<>();
        long preFlowCost = 0;

        // regular edges between sinks and sources without threshold edges
        Vector<List<Edge>> c = new Vector<>();
        for (int i = 0; i < b.size(); i++) {
            c.add(new LinkedList<>());
        }
        for (int i = 0; i < dimension; i++) {
            if (b.get(i) == 0)
                continue;
            for (int j = 0; j < dimension; j++) {
                if (b.get(j + dimension) == 0)
                    continue;
                if (C[i][j] == maxC)
                    continue;
                c.get(i).add(new Edge(j + dimension, C[i][j]));
            }
        }

        // checking which are not isolated
        for (int i = 0; i < dimension; i++) {
            if (b.get(i) == 0)
                continue;
            for (int j = 0; j < dimension; j++) {
                if (b.get(j + dimension) == 0)
                    continue;
                if (C[i][j] == maxC)
                    continue;
                sourcesThatFlowNotOnlyToThresh.add(i);
                sinksThatGetFlowNotOnlyFromThresh.add(j + dimension);
            }
        }

        // converting all sinks to negative
        for (int i = dimension; i < 2 * dimension; i++) {
            b.set(i, -b.get(i));
        }

        // add edges from/to threshold node,
        // note that costs are reversed to the paper (see also remark* above)
        // It is important that it will be this way because of remark* above.
        for (int i = 0; i < dimension; ++i) {
            c.get(i).add(new Edge(THRESHOLD_NODE, 0));
        }
        for (int j = 0; j < dimension; ++j) {
            c.get(THRESHOLD_NODE).add(new Edge(j + dimension, maxC));
        }

        // artificial arcs - Note the restriction that only one edge i,j is
        // artificial so I ignore it...
        for (int i = 0; i < ARTIFICIAL_NODE; i++) {
            c.get(i).add(new Edge(ARTIFICIAL_NODE, maxC + 1));
            c.get(ARTIFICIAL_NODE).add(new Edge(i, maxC + 1));
        }

        // remove nodes with supply demand of 0
        // and vertexes that are connected only to the
        // threshold vertex
        int currentNodeName = 0;
        // Note here it should be vector<int> and not vector<int>
        // as I'm using -1 as a special flag !!!
        int REMOVE_NODE_FLAG = -1;
        Vector<Integer> nodesNewNames = new Vector<>();
        Vector<Integer> nodesOldNames = new Vector<>();
        for (int i = 0; i < b.size(); i++) {
            nodesNewNames.add(REMOVE_NODE_FLAG);
            nodesOldNames.add(0);
        }
        for (int i = 0; i < dimension * 2; i++) {
            if (b.get(i) != 0) {
                if (sourcesThatFlowNotOnlyToThresh.contains(i)
                        || sinksThatGetFlowNotOnlyFromThresh.contains(i)) {
                    nodesNewNames.set(i, currentNodeName);
                    nodesOldNames.add(i);
                    currentNodeName++;
                } else {
                    if (i >= dimension) {
                        preFlowCost -= (b.get(i) * maxC);
                    }
                    b.set(THRESHOLD_NODE, b.get(THRESHOLD_NODE) + b.get(i)); // add mass(i<N) or deficit (i>=N)
                }
            }
        }
        nodesNewNames.set(THRESHOLD_NODE, currentNodeName);
        nodesOldNames.add(THRESHOLD_NODE);
        currentNodeName++;
        nodesNewNames.set(ARTIFICIAL_NODE, currentNodeName);
        nodesOldNames.add(ARTIFICIAL_NODE);
        currentNodeName++;

        Vector<Long> bb = new Vector<>();
        for (int i = 0; i < currentNodeName; i++) {
            bb.add(0L);
        }
        int j = 0;
        for (int i = 0; i < b.size(); i++) {
            if (nodesNewNames.get(i) != REMOVE_NODE_FLAG) {
                bb.set(j, b.get(i));
                j++;
            }
        }

        Vector<List<Edge>> cc = new Vector<>();
        for (int i = 0; i < bb.size(); i++) {
            cc.add(new LinkedList<>());
        }
        for (int i = 0; i < c.size(); i++) {
            if (nodesNewNames.get(i) != REMOVE_NODE_FLAG) {
                for (Edge it : c.get(i)) {
                    if (nodesNewNames.get(it._to) != REMOVE_NODE_FLAG) {
                        cc.get(nodesNewNames.get(i)).add(
                                new Edge(nodesNewNames.get(it._to), it._cost));
                    }
                }
            }
        }

        MinCostFlow mcf = new MinCostFlow();

        Vector<List<Edge0>> flows = new Vector<>(bb.size());
        for (int i = 0; i < bb.size(); i++) {
            flows.add(new LinkedList<>());
        }

        long mcfDist = mcf.compute(bb, cc, flows);

        long myDist = preFlowCost + // pre-flowing on cases where it was possible
                mcfDist + // solution of the transportation problem
                (absDiffSumPSumQ * extraMassPenalty); // emd-hat extra mass penalty

        return myDist;
    }

    private static Vector<Long> transfer(int n, long[] pc) {
        Vector<Long> vec = new Vector<>();
        for(int i = 0; i < n; i++){
            vec.add(pc[i]);
        }
        return vec;
    }
}