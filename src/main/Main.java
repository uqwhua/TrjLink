package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import io.FileIO;
import signatures.Function;
import basic.Grid;
import basic.SimplePoint;
import basic.Trajectory;

public class Main {

    public static final boolean DEBUG = false;
    public static Map<Integer, SimplePoint> NNid2lnglat = new HashMap<>();   // read from file

    public static void main(String[] args) throws IOException {

        /* get the parameters from file */
        final String homepath = System.getProperty("user.dir") + "/";
        final String paramFile = homepath + "src/resources/config.properties";
        Properties props = springUtil(paramFile);

        final String dataInputFolder = homepath + props.getProperty("dataInput");
        final String nodeFileName = homepath + props.getProperty("roadNetworkFile");    // the road intersections on road network (for map-matching)

        final int cutoff = Integer.parseInt(props.getProperty("reduction"));        // reduce the full-length signature to top-m sorted by the weight
        final int topK = Integer.parseInt(props.getProperty("topK"));               // to get the accuracy@K of the linking result
        final int objectTotal = Integer.parseInt(props.getProperty("objectNum"));   // how many object will be tested
        final String method = props.getProperty("linkingMethod");                   // the linking method used in this program, WR-tree is more efficient than linear scan
        final String sigType = props.getProperty("signatureType");                  // which signature will be used for linking

        final int gramLen = Integer.parseInt(props.getProperty("seq_gramLen"));    // for sequential signatures, we model it via n-gram, which should be specified
        final int timeWindowSize = Integer.parseInt(props.getProperty("time_windowSize"));      // for temporal signatures, we divide one day into several time windows and vectorize it
        final int gridNum = Integer.parseInt(props.getProperty("st_gridNum"));           // for spatiotemporoal signatures, we model the time dimension on an hourly basis,
                                        // while for the spatial dimension, we may approximate a point as a grid cell (should specify the grid partition, like 100*100, 200*200)


        /* Step-1: build a simple grid index, in order to align each raw point with its nearest road intersection */
        final float step = 0.001f;    // determine the size of each grid cell
        final int maxVertexId = 296709;   // the max ID of the road node
        final Grid grid = FileIO.buildGridIndex(nodeFileName, maxVertexId, step, step);    // the intersections on the road network

        /* Step-2: read raw data from file and meanwhile split data into two parts based on the date (odd or even) */
        System.out.println("[INFO] Reading raw data for map-matching and split meanwhile ...");

        final Vector<Trajectory> oddData = new Vector<>(), evenData = new Vector<>();
        final double avgLength = FileIO.readRawDataset(dataInputFolder, objectTotal, oddData, evenData);
        System.out.printf("\t Avg Length of the %d objects' trips = %.3f\n", objectTotal, avgLength);


        /* Step-3: construct different signatures and perform linking
        *           Four signatures are provided: spatial, temporal, spatiotemporal, sequential */
        System.out.println("\n---------------------------------");
        System.out.printf("[INFO] Constructing %s signatures for two sets ...\n", sigType);
        final Map<String, Map<Integer, Double>> oddSignatures = Function.constructSignature(oddData, grid, sigType, gramLen, timeWindowSize, gridNum);
        final Map<String, Map<Integer, Double>> evenSignatures = Function.constructSignature(evenData, grid, sigType, gramLen, timeWindowSize, gridNum);


        /* start linking */
        final int total = oddSignatures.size();
        int[] success = null;

        // temporal signatures don't have any spatial information, so only linear scan is possible
        if(method.equalsIgnoreCase("linear") || sigType.equalsIgnoreCase("time") || sigType.equalsIgnoreCase("temporal")) {
            success = Linking.linearExecute(oddSignatures, evenSignatures, cutoff, topK, sigType);
        }
        else if(method.equalsIgnoreCase("wr-tree") || method.equalsIgnoreCase("wrtree")){
            final int capacity = 28, fanout = 4;  // the fixed parameters of R-tree, okay with changes
            success = Linking.rtreeBased(oddSignatures, evenSignatures, cutoff, topK, capacity, fanout);
        }

        showResult(success, total, topK);
        System.out.println("\nThe program is done!");
    }

    // print out result (from Acc@1 to Acc@K)
    private static void showResult(final int[] success, final int total, final int topK){
        if(success != null) {
            for (int k = 1; k <= topK; k++) {
                double acc = success[k] / (total * 1.0);
                System.out.printf("\t[Accuracy] # of positive = %d, Acc@%d = %.5f\n", success[k], k, acc);
            }
            System.out.println("---------------------------------");
        }
    }

    // read config.properties file for parameters
    private static Properties springUtil(String path) {
        Properties props = new Properties();
        System.out.println("---------------------------------");
        try (InputStream input = new FileInputStream(path)) {
            System.out.println("[INFO] Reading parameters from " + path);
            props.load(input);
            for (Object key : props.keySet()) {
                System.out.print("\t" + key + ":\t");
                System.out.println(props.get(key));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("---------------------------------");
        return props;
    }
}
