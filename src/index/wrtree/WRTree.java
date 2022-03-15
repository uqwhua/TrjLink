package index.wrtree;

import index.AscendNeighbor;
import index.Calculate;
import index.DescendNode;
import index.rtree.RTLeafElement;
import index.rtree.RTNode;
import index.rtree.RTree;
import index.rtree.Rectangle;

import java.util.*;

public class WRTree extends RTree {

    // empty tree, only initialize parameters
    public WRTree(int _branchFactor, int _leafCapacity, int _fanoutRatio) {
        super(_branchFactor, _leafCapacity, _fanoutRatio);
    }

    /**
     * --------------------------- 1) construct tree by inserting one by one ---------------------------
     */

    @Override
    public void insertElement(RTLeafElement toInsertElement) {
        if (toInsertElement == null) {
            throw new IllegalArgumentException("Error in WRTree.insertElement: Null to be inserted.");
        }

        this.entityNum++;

        // this is the first element to be inserted into this tree
        // just create a leaf node, and it naturally becomes the root of this tree
        if (this.root == null) {
            WRTLeafNode rootNode = new WRTLeafNode(null);      // level of leaf node = 0
            rootNode.addElement(toInsertElement);
            this.root = rootNode;
            return;
        }

        // else, we need to find the best leaf node to insert this element
        RTNode curNode = this.root;
        int minSize = (int) Math.max(leafCapacity * 1.0 / fanoutRatio, 2);  // at least 2
        while (curNode.level >= 0) {
            if (curNode.isLeaf()) {
                WRTLeafNode curLeafNode = (WRTLeafNode) curNode;

                // if this element were to inserted, this leaf node would overflow
                if (curLeafNode.reachMaxCapacity(this.leafCapacity)) {
                    // need a sibling node
                    WRTLeafNode brotherNode = curLeafNode.splitAndShare(toInsertElement, minSize);
                    overflowHandler(curLeafNode, brotherNode);
                } else {  // still have space to add this element
                    curLeafNode.addElement(toInsertElement);
                }
                return; // the end of insertion for this element
            }
            else {
                WRTDirNode curDirNode = (WRTDirNode) curNode;

                // different strategies to choose best child for this element
                int bestChildIdx = curDirNode.chooseBestChild_MostCommonPoint(toInsertElement);
                if (bestChildIdx == -1) {
                    bestChildIdx = curDirNode.chooseBestChild_LeastEnlargement(toInsertElement.mbr);
                }

                // current node need to be updated
                curDirNode.mbr.enlargeRectangle(toInsertElement.mbr);    // enlarge the mbr of this internal node
                curDirNode.updateAggregator(toInsertElement.signature);  // updateAggregator the aggregator-signature

                // go down to next lower level
                curNode = curDirNode.getChildByID(bestChildIdx);
            }
        }
    }

    // these two nodes are siblings, they are at the same level with the same parent node
    @Override
    public void overflowHandler(RTNode overflowNode, RTNode newBrotherNode) {

        // the overflowNode is the root of this tree, then create a new root
        if (overflowNode.isRoot()) {
            WRTDirNode newRoot = new WRTDirNode(null, overflowNode.level + 1);
            newRoot.addChildNode(overflowNode);
            newRoot.addChildNode(newBrotherNode);
            this.root = newRoot;
        } else {  // it isn't a root
            // so their parent node may also overflow if directly insert this new brother node
            WRTDirNode parentNode = (WRTDirNode) overflowNode.parent;
            if (parentNode.reachMaxCapacity(this.branchFactor)) {
//                overflow(parentNode, newBrotherNode);
                WRTDirNode brotherNode = parentNode.splitAndShare(newBrotherNode, (int) Math.max(branchFactor * 1.0 / fanoutRatio, 2));
                overflowHandler(parentNode, brotherNode);
            }
            else {
                parentNode.addChildNode(newBrotherNode);
            }
        }
    }


    /**
     * --------------------------- 2) STR bulk-loading algorithm ---------------------------------------
     */

    public void constructRTree_STR(List<RTLeafElement> elementList) {

        this.entityNum = elementList.size();
        Vector<RTNode> curNodeList = new Vector<>();
        int rtn = constructLeafLevel_STR_SubRoutine(elementList, 0, entityNum, 1, curNodeList);

        if (rtn == 1) {
            System.out.println("Error in RTree.constructRtree_STR: Leaf level construction failed!\n");
            curNodeList.clear();
            return;
        }
//        System.out.println("[PROGRESS] successfully construct leaf level.");

        int level = 0;
        Vector<RTNode> nextNodeList = new Vector<>();

        // start from the leaf nodes, construct the internal nodes in a bottom-up manner
        while (curNodeList.size() > 1) {
            nextNodeList.clear();
            int curNodeNum = curNodeList.size();
            rtn = constructNextLevel_STR_SubRoutine(curNodeList, 0, curNodeNum, level, 1, nextNodeList);

            if (rtn == 1) {
                System.out.println("Error in RTree.constructRtree_STR: Next level construction failed!\n");
                curNodeList.clear();
                nextNodeList.clear();
                return;
            }

            curNodeList = new Vector<>(nextNodeList);
            level++;
        }

        root = curNodeList.firstElement();
        rootLev = level;
    }

    /**
     * construct next level based on the STL algorithm
     *
     * @param _fromIdx     :   the start index (inclusive)
     * @param _nodeNum     :   # of nodes to be processed, hence the index of last node should be (_fromIdx + _nodeNum - 1)
     * @param _targetDim   :   which dimension we used for sorting in the current recursion.
     * @param nextNodeList :   the target place to store the next levels nodes
     */
    public int constructNextLevel_STR_SubRoutine(Vector<RTNode> _curNodeList, int _fromIdx, int _nodeNum, int _curLevel, int _targetDim,
                                                 Vector<RTNode> nextNodeList) {

        if (_curNodeList == null) {
            System.out.println("Error in WRTree.constructNextLevel_STR_SubRoutine: _curNodeList == null!");
            return 1;
        }
        int totalNodeNum = _curNodeList.size();
        if (_fromIdx < 0 || _fromIdx >= totalNodeNum || _nodeNum > totalNodeNum || (_fromIdx + _nodeNum) > totalNodeNum) {
            System.out.println("Error in WRTree.constructNextLevel_STR_SubRoutine: Illegal _fromIdx!");
            System.out.println("//debug: _fromIdx = " + _fromIdx + ", _nodeNum = " + _nodeNum + ", totalNodeNum = " + totalNodeNum);
            return 1;
        }

        double exp = 1.0 / (dimension - _targetDim + 1);
        int slabNum = (int) Math.ceil(Math.pow((_nodeNum * 1.0 / branchFactor), exp));
        int slabSize = (int) Math.ceil(_nodeNum * 1.0 / slabNum);    // # of nodes in each slab

        int nextLevel = _curLevel + 1;

        if (_nodeNum <= branchFactor) {
            // There is only one node
            WRTDirNode curNode = new WRTDirNode(null, nextLevel);
            for (int i = 0; i < _nodeNum; i++) {
                curNode.addChildNode(_curNodeList.get(_fromIdx + i));
            }
            nextNodeList.add(curNode);
            return 0;
        }

        /* case-1, tag for debug */
        if (slabSize <= branchFactor / fanoutRatio || dimension == _targetDim) {
            // There are too few points to make slabs. Or this is the last level in the recursion.
            // Note that: There must be at least two slabs because branchFactor < nodeNum.

            // Sort these nodes by the _targetDim coordinate
            sortRTNodes(_curNodeList, _fromIdx, _nodeNum, _targetDim);

            int nextNodeNum = (int) Math.ceil(_nodeNum * 1.0 / branchFactor);
            WRTDirNode curNode;
            int curIndex = _fromIdx;
            int tempNum = branchFactor;
            // We construct the first (nextNodeNum - 2) nodes, because we may need to deal with the last two nodes specially.
            for (int i = 0; i < nextNodeNum - 2; ++i) {
                curNode = new WRTDirNode(null, nextLevel);
                for (int j = 0; j < tempNum; ++j) {
                    curNode.addChildNode(_curNodeList.get(curIndex));
                    curIndex++;
                }
                nextNodeList.add(curNode);
            }

            // We deal with the last two nodes specially and equally
            tempNum = (_fromIdx + _nodeNum - curIndex) / 2;
            curNode = new WRTDirNode(null, nextLevel);

            for (int j = 0; j < tempNum; ++j) {
                curNode.addChildNode(_curNodeList.get(curIndex));
                curIndex++;
            }
            nextNodeList.add(curNode);

            // the last one
            tempNum = _fromIdx + _nodeNum - curIndex;   // the remaining nodes
            curNode = new WRTDirNode(null, nextLevel);
            for (int j = 0; j < tempNum; ++j) {
                curNode.addChildNode(_curNodeList.get(curIndex));
                curIndex++;
            }
            nextNodeList.add(curNode);

            // Code for debug
            if (curIndex != (_fromIdx + _nodeNum)) {
                System.out.println("//debug in WRTree.constructNextLevel_STR_SubRoutine: in case-1, the index of last node is wrong...");
                return 1;
            }

            return 0;
        } else {
            // This is not the last level in the recursion and there are enough points in each slab.
            // Make slabs.
            // Note that: There must be at least two slabs because _branchFactor < _nodeNum.
            sortRTNodes(_curNodeList, _fromIdx, _nodeNum, _targetDim);

            int rtn = 0;
            int curFromIdx = _fromIdx;
            for (int i = 0; i < slabNum - 2; ++i) {
                rtn = constructNextLevel_STR_SubRoutine(_curNodeList, curFromIdx, slabSize, _curLevel, _targetDim + 1, nextNodeList);
                if (rtn == 1) {
                    System.out.println("Error in WRtree.constructNextLevel_STR_SubRoutine: Something must be wrong in recursion!");
                    return 1;
                }
                curFromIdx += slabSize;
            }


            /** Process the last two slabs specially. */
            slabSize = (_nodeNum - curFromIdx) / 2;            // equal slabSize for the last two slabs, may different with above

            rtn = constructNextLevel_STR_SubRoutine(_curNodeList, curFromIdx, slabSize, _curLevel, _targetDim + 1, nextNodeList);
            if (rtn == 1) {
                System.out.println("Error in WRtree.constructNextLevel_STR_SubRoutine: Something must be wrong in recursion!");
                return 1;
            }

            /** The last one slab */
            curFromIdx += slabSize;
            slabSize = _nodeNum - curFromIdx;       // Put all the rest points to the last slab.
            rtn = constructNextLevel_STR_SubRoutine(_curNodeList, curFromIdx, slabSize, _curLevel, _targetDim + 1, nextNodeList);
            if (rtn == 1) {
                System.out.println("Error in WRtree.constructNextLevel_STR_SubRoutine: Something must be wrong in recursion!");
                return 1;
            }

            // Code for debug
            if ((curFromIdx + slabSize) != (_fromIdx + _nodeNum)) {
                System.out.println("//debug in WRTree.constructNextLevel_STR_SubRoutine: when slabbing, the index of last node is wrong...");
                return 1;
            }

            return 0;
        }
    }

    /**
     * Construct leaf level by STR algorithm.
     * the range of elementList to be used is [_fromIdx, _fromIdx + _elementNum)
     *
     * @param _elemNum     :    # of elements to be processed in this round
     * @param _targetDim   :    which dimension we used for sorting in the current recursion.
     * @param leafNodeList :    the target place to store the leaf nodes.
     */
    public int constructLeafLevel_STR_SubRoutine(List<RTLeafElement> _elementList, int _fromIdx, int _elemNum, int _targetDim,
                                                 Vector<RTNode> leafNodeList) {

        if (_elementList == null || _elemNum <= 0) {
            System.out.println("Error in WRTree.constructLeafLevel_STR_SubRoutine: elementList == null!");
            return 1;
        }
        int totalElemNum = _elementList.size();
        if (_fromIdx < 0 || _fromIdx >= totalElemNum || _elemNum > totalElemNum || (_fromIdx + _elemNum) > totalElemNum) {
            System.out.println("Error in WRTree.constructLeafLevel_STR_SubRoutine: Illegal _fromIdx!");
            System.out.println("//debug: _fromIdx = " + _fromIdx + ", _elementNum = " + _elementList + ", totalElemNum = " + totalElemNum);
            return 1;
        }

        int halfCapacity = (int) Math.ceil(leafCapacity / 2.0);

        double exp = 1.0 / (dimension - _targetDim + 1);
        int slabNum = (int) Math.ceil(Math.pow((_elemNum * 1.0 / halfCapacity), exp));
        int slabSize = (int) Math.ceil(_elemNum * 1.0 / slabNum);     // # of elements in each slab

        if (_elemNum <= leafCapacity) {
            // there is only one leaf node
            WRTLeafNode curNode = new WRTLeafNode(null);      // level of leaf node = 0
            for (int i = 0; i < _elemNum; i++) {
                curNode.addElement(_elementList.get(i + _fromIdx));
            }
            leafNodeList.add(curNode);
            return 0;
        }

        // the elements grouped in one slab could be stored in multiple leaf nodes
        if (slabSize <= leafCapacity / fanoutRatio || _targetDim == dimension) {
            // There are too few elements to make slabs
            sortRTLeafElements(_elementList, _fromIdx, _elemNum, _targetDim);

            // Note that leafNodeNum > 1
            int leafNodeNum = (int) Math.ceil(_elemNum * 1.0 / halfCapacity);
            WRTLeafNode curNode;
            int curIndex = _fromIdx;
            int tempNum = halfCapacity;

            // We construct the first (leafNodeNum - 2) nodes because we may need to deal with the last two nodes specially.
            for (int i = 0; i < leafNodeNum - 2; ++i) {
                curNode = new WRTLeafNode(null);
                for (int j = 0; j < tempNum; ++j) {
                    curNode.addElement(_elementList.get(curIndex));
                    curIndex++;
                }
                leafNodeList.add(curNode);
            }

            // We deal with the last two nodes specially.
            tempNum = (_fromIdx + _elemNum - curIndex) / 2;
            curNode = new WRTLeafNode(null);
            for (int j = 0; j < tempNum; ++j) {
                curNode.addElement(_elementList.get(curIndex));
                curIndex++;
            }
            leafNodeList.add(curNode);

            tempNum = _fromIdx + _elemNum - curIndex;
            curNode = new WRTLeafNode(null);
            for (int j = 0; j < tempNum; ++j) {
                curNode.addElement(_elementList.get(curIndex));
                curIndex++;
            }
            leafNodeList.add(curNode);

            // Code for debug
            if (curIndex != (_fromIdx + _elemNum)) {
                System.out.println("//debug in WRTree.constructLeafLevel_STR_SubRoutine: in case-1, the index of last node is wrong...");
                return 1;
            }
            return 0;
        }
        else {
            // This is not the last level in the recursion and there are enough points in each slab.
            // Make slabs.
            // Note that: There must be at least two slabs because _leafCapacity < _elemNum.
            // Sort all the points by the _dimLev-dim coordinates.
            sortRTLeafElements(_elementList, _fromIdx, _elemNum, _targetDim);

            int rtn = 0;
            int curFromIdx = _fromIdx;
            for (int i = 0; i < slabNum - 2; ++i) {
                rtn = constructLeafLevel_STR_SubRoutine(_elementList, curFromIdx, slabSize, _targetDim + 1, leafNodeList);
                if (rtn == 1) {
                    System.out.println("Error in WRtree.constructLeafLevel_STR_SubRoutine: Something must be wrong in recursion!");
                    return 1;
                }
                curFromIdx += slabSize;
            }


            /* Process the last two slabs specially. */
            slabSize = (_elemNum - curFromIdx) / 2;            // equal slabSize for the last two slabs, may different with above

            rtn = constructLeafLevel_STR_SubRoutine(_elementList, curFromIdx, slabSize, _targetDim + 1, leafNodeList);
            if (rtn == 1) {
                System.out.println("Error in WRtree.constructLeafLevel_STR_SubRoutine: Something must be wrong in recursion!");
                return 1;
            }

            /* The last one slab */
            curFromIdx += slabSize;
            slabSize = _elemNum - curFromIdx;       // Put all the rest points to the last slab.
            rtn = constructLeafLevel_STR_SubRoutine(_elementList, curFromIdx, slabSize, _targetDim + 1, leafNodeList);
            if (rtn == 1) {
                System.out.println("Error in WRtree.constructLeafLevel_STR_SubRoutine: Something must be wrong in recursion!\n");
                return 1;
            }

            // Code for debug
            if ((curFromIdx + slabSize) != (_fromIdx + _elemNum)) {
                System.out.println("//debug in WRTree.constructLeafLevel_STR_SubRoutine: when slabbing, the index of last node is wrong...");
                return 1;
            }
            return 0;
        }
    }



    /* ---------------------------------------- query functions ----------------------------------------- */

    @Override
    public Queue<AscendNeighbor> findKNN(Map<Integer, Double> querySignature, int numOfK) {

        if (this.root == null) {
            throw new IllegalArgumentException("Error in findKNN by WRtree: The root is NULL.");
        }
        Rectangle queryMBR = new Rectangle(querySignature.keySet());

        // any candidate node added to this queue must have spatial overlapping with this query
        Queue<DescendNode> candidates = new PriorityQueue<>();
        candidates.add(new DescendNode(this.root, -1));   // no need to compute upper bound for root node, just add it

        Queue<AscendNeighbor> NNqueue = new PriorityQueue<>();          // ascending order of similarity

        float minSimilarityOfNN;

        while (!candidates.isEmpty()) {

            minSimilarityOfNN = NNqueue.isEmpty() ? Float.NEGATIVE_INFINITY : NNqueue.peek().similarity;

            DescendNode curCandidate = candidates.poll();       // take out the top node with current max upperBound

            if (curCandidate.upperBound < minSimilarityOfNN && NNqueue.size() >= numOfK)
                break;

            // start to check current candidate
            RTNode curNode = curCandidate.node;

            if (curNode.isLeaf()) {

                // scan all children of this leaf node (might need to calculate exact similarity between signatures)
                scanLeafNode(queryMBR, querySignature, curNode.getChildren(), numOfK, NNqueue);
            } else {
                // it's an internal node
                // may need upperbound computation between query signature and node's aggregator-signature
                WRTDirNode curDirNode = (WRTDirNode) curNode;

                // scan its all child nodes
                for (int i = 0; i < curDirNode.getChildNum(); i++) {

                    RTNode child = curDirNode.getChildByID(i);      // could be either internal or leaf node

                    // only if they have spatial overlap and common points, the upperbound computation may happen
                    if (queryMBR.isIntersection(child.mbr)) {

                        float upperBound = Calculate.upperBound(querySignature, child.aggregator);

                        // haven't found enough NN, or the upperbound is greater than current min similarity of NNs
                        // then add this node into candidate queue
                        if (NNqueue.size() < numOfK || upperBound >= minSimilarityOfNN) {
                            candidates.add(new DescendNode(child, upperBound));
                        }
                    }
                }
            }
        }

        return NNqueue;
    }


}