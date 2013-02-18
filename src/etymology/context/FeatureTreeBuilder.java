/**
    Copyright (C) 2010-2012 University of Helsinki.    

    This file is part of Etymon.
    Etymon is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Etymon is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Etymon.  If not, see <http://www.gnu.org/licenses/>.
**/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package etymology.context;

import etymology.config.Configuration;
import etymology.context.FeatureTreeContainer.BabyTreeType;
import etymology.context.FeatureTreeContainer.Context;
import etymology.context.FeatureTreeContainer.Features;
import etymology.context.FeatureTreeContainer.Level;
import etymology.context.FeatureTreeContainer.TreeType;
import etymology.cost.FeatureTreeCostCalculator;
import etymology.cost.PrequentialCodeLengthCostFunction;
import etymology.input.FeatureVocabulary;
import etymology.util.EtyMath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;



/**
 *
 * @author sxhiltun
 */
public class FeatureTreeBuilder {

    private ContextCellContainer[] sourceCellContainer;
    private ContextCellContainer[] targetCellContainer;

    private FeatureVocabulary fvSource;
    private FeatureVocabulary fvTarget;

    private List<Map<Character, List<Integer>>> combinedMapSource;
    private List<Map<Character, List<Integer>>> combinedMapTarget;

    
    public FeatureTreeBuilder(
            FeatureVocabulary fvSource,
            FeatureVocabulary fvTarget,
            List<Map<Character, List<Integer>>> combinedMapSource,
            List<Map<Character, List<Integer>>> combinedMapTarget,
            ContextCellContainer[] sourceCellContainer,
            ContextCellContainer[] targetCellContainer) {

        this.fvSource = fvSource;
        this.fvTarget = fvTarget;
        this.combinedMapSource = combinedMapSource;
        this.combinedMapTarget = combinedMapTarget;
        this.sourceCellContainer = sourceCellContainer;
        this.targetCellContainer = targetCellContainer;

        boolean doBinary = Configuration.getInstance().isUseBinaryValueTrees();
        boolean doMultiple = Configuration.getInstance().areMultipleValueTreesOn();


        CandidateFinder.setBinaryCandidates(doBinary);
        CandidateFinder.setNormalCandidates(doMultiple);

    }
    
    //constructor for one level trees only
    public FeatureTreeBuilder(
            FeatureVocabulary fvSource,            
            List<Map<Character, List<Integer>>> combinedMapSource,            
            ContextCellContainer[] sourceCellContainer) {

        this.fvSource = fvSource;
        this.fvTarget = null;
        this.combinedMapSource = combinedMapSource;
        this.combinedMapTarget = null;
        this.sourceCellContainer = sourceCellContainer;
        this.targetCellContainer = null;

        boolean doBinary = Configuration.getInstance().isUseBinaryValueTrees();
        boolean doMultiple = Configuration.getInstance().areMultipleValueTreesOn();


        CandidateFinder.setBinaryCandidates(doBinary);
        CandidateFinder.setNormalCandidates(doMultiple);
        CandidateFinder.setOneLevelCandidatesOnly(true);
    }
    
    

    public FeatureTree buildNewJointCodeFeatureTree(Features featureName, BabyTreeType btt,
            List<Character> sourceLabels,  List<Character> targetLabels, 
            Map<Character, List<Integer>> sourceLabelsToIndexesMap, Map<Character, List<Integer>> targetLabelsToIndexesMap, TreeType treeType) throws Exception {

        List<List<ContextCell>> contextCells = 
                initializeJointCodeContextCellDistributionInRootNode(btt, sourceLabels, targetLabels, sourceLabelsToIndexesMap, targetLabelsToIndexesMap);
        TreeNode rootNode = new TreeNode(0, "*", contextCells);
        FeatureTree tree = 
                new JointlyCodingFeatureTree(rootNode, sourceLabels, targetLabels, btt, featureName, treeType);
        int[][] matrix = buildJointCodeRootNodeMatrix(tree);                
        rootNode.setCountMatrix(matrix);
        
        //double cost = getNodeMatrixCost(matrix);
        double cost = new FeatureTreeCostCalculator().getDataCostForTreeNode(rootNode);        
        rootNode.setCost(cost);
        
        
        
        
        List<Candidate> candidates = CandidateFinder.getRestrictedListOfCandidatesOfRootNode(tree);
        rootNode.setCandidates(candidates);
        if (candidates.size() > 0) {
            buildTree(tree, rootNode);
        }

        tree.computeTotalCostOfTree();
        
        return tree;

    }


    public FeatureTree buildNewSeparateCodeFeatureTree(Features featureName, BabyTreeType btt, List<Character> labels,
            Map<Character, List<Integer>> labelsToIndexesMap, TreeType treeType) throws Exception {

        List<List<ContextCell>> contextCells = initializeSeparateCodeContextCellDistributionInRootNode(btt, labels, labelsToIndexesMap);
        TreeNode rootNode = new TreeNode(0, "*", contextCells);
        FeatureTree tree = new SeparateCodingFeatureTree(rootNode, labels, btt, featureName, treeType);
        int[][] matrix = buildSeparateCodeRootNodeMatrix(tree);
        rootNode.setCountMatrix(matrix);
        
        double cost = new FeatureTreeCostCalculator().getDataCostForTreeNode(rootNode);        
        rootNode.setCost(cost);
        
        List<Candidate> candidates = CandidateFinder.getRestrictedListOfCandidatesOfRootNode(tree);
        rootNode.setCandidates(candidates);
        if (candidates.size() > 0) {
            buildTree(tree, rootNode);
        }
        
        tree.computeTotalCostOfTree();
        return tree;
    }






    private List<List<ContextCell>> initializeJointCodeContextCellDistributionInRootNode(
            BabyTreeType babyTreeType,
            List<Character> sourceLabels, List<Character> targetLabels,
            Map<Character, List<Integer>> labelsToSourceIndexesMap,
            Map<Character, List<Integer>> labelsToTargetIndexesMap) throws Exception {

        List<List<ContextCell>> contextCells = new ArrayList<List<ContextCell>>();
        Collection<Integer> matchingSourceIndices;
        Collection<Integer> matchingTargetIndices;

        // both source- and target cell containers contain the same information in different order
        // TODO: a good way to index "combined container"

        for (char rowLabel : sourceLabels) {
            
            //these feature vectors have value "sourcelabel"
            matchingSourceIndices = labelsToSourceIndexesMap.get(rowLabel);

            for (char colLabel : targetLabels) {
                
                //collect the context cells, which have (sourceLabel, targetLabel)
                List<ContextCell> cells = new ArrayList<ContextCell>();

                //these feature vectors have value "targetLabel"
                matchingTargetIndices = new TreeSet(labelsToTargetIndexesMap.get(colLabel));

                //
                for (int matchingIndex : matchingSourceIndices) {
                    for (ContextCell sourceCell : sourceCellContainer[matchingIndex].getCells()) {
                        if (matchingTargetIndices.contains(sourceCell.getCandidateIndexInContext(Level.TARGET, Context.ITSELF))) {
                            cells.add(sourceCell);
                        }
                    }
                }
                contextCells.add(cells);
            }
        }

        return contextCells;
    }



    /**
     * Compute list of context cells for all possible values the feature of this feature tree.
     *
     * @param babyTreeType - source or target level tree
     * @param labels - the names of the matrix indices = possible values of feature that the current tree codes
     * @param labelsToIndexesMap - maps possible values of feature to indices of feature vectors that have this value of feature.
     * @return list that contains list of context cells for every possible value of this feature
     */
    private List<List<ContextCell>> initializeSeparateCodeContextCellDistributionInRootNode(
            BabyTreeType babyTreeType,
            List<Character> labels,
            Map<Character, List<Integer>> labelsToIndexesMap) {

        List<Integer> matchingIndices;

        //contains one list of contextCells for each label => one list for each matrix cell
        List<List<ContextCell>> contextCells = new ArrayList<List<ContextCell>>();

        ContextCellContainer[] container = null;

        //choose the context cell container
        if (babyTreeType == BabyTreeType.SOURCE) {
            container = sourceCellContainer;
           
            
        }else if (babyTreeType == BabyTreeType.TARGET){
            container = targetCellContainer;
        }

        // loop possible values of this feature
        // e.g feature = rounded --> labels = {u (yes), n (no)}
        for(char label: labels) {
            

            //in these feature vectors the current feature has value "label"
            matchingIndices = labelsToIndexesMap.get(label);

            //collect the corresponding context cells
            List<ContextCell> cells = new ArrayList();
            for (int matchingIndex : matchingIndices) {
                //all distinct feature vectors have their own row in container
                //the feature index and the glyph index are the same!!? matchingIndex is the feature index, here is used actually as glyph index!!!? ---Lv
                cells.addAll(container[matchingIndex].getCells());
            }

            contextCells.add(cells);
        }
        
        return contextCells;
    }


    /**
     * When the context cells are known, let's extract the counts of the events
     * and plug them in to the matrix.
     *
     * @param tree - the feature tree we are currently building
     * @return count matrix of root node
     */
    private int[][] buildSeparateCodeRootNodeMatrix(FeatureTree tree) {

        List<Character> columnLabels = tree.getColumnLabels();

        List<List<ContextCell>> contextCells = tree.getRootNode().getContextCellsForEachFeatureValue();

        int[][] rootNodeMatrix = new int[1][columnLabels.size()];

        if(columnLabels.size() != contextCells.size()) {
            throw new RuntimeException("Label count and alignment cell count don't match");
        }

        for (int i = 0; i < rootNodeMatrix[0].length; i++) {
            
            rootNodeMatrix[0][i] = contextCells.get(i).size();
        }

        
        return rootNodeMatrix;
    }

    private int[][] buildJointCodeRootNodeMatrix(FeatureTree tree) {
        List<Character> columnLabels = tree.getColumnLabels();
        List<Character> rowLabels = tree.getRowLabels();

        List<List<ContextCell>> contextCells = tree.getRootNode().getContextCellsForEachFeatureValue();

        int[][] rootNodeMatrix = new int[rowLabels.size()][columnLabels.size()];

        int cell = 0;
        for (int row=0; row< rowLabels.size(); row++) {
            for (int col=0; col< columnLabels.size(); col++) {
                rootNodeMatrix[row][col] = contextCells.get(cell).size();
                cell++;
            }
        }


        return rootNodeMatrix;

    }


    /**
     * Recursively build the full tree!!!
     *
     * @param tree - this tree we build now
     * @param parentNode - initially this is the rootnode
     * @throws Exception - then there is a bug
     */
    private void buildTree(FeatureTree tree, TreeNode parentNode) throws Exception {

        Candidate bestCandidate = null;
        Candidate equalCandidate = null;

        // + 1 indicates that we can say also "we did not split"
        // current best cost is from option "do not split"
        double bestCost = parentNode.getCost() + 1;

        //list of candidate triplets
        List<Candidate> candidatesOfParentNode = parentNode.getCandidates();
        List<Double> costsOfCandidates = new ArrayList(); //for debugging
        
        List<Candidate> bestOnes = new ArrayList<Candidate>();
        

        for (Candidate candidate: candidatesOfParentNode) {

            List<TreeNode> childNodesOfCandidateSplit;

            //the heavy stuff, which child nodes this candidate creates
            //childNodesOfCandidateSplit.size() ==  how many possible values the candidate feature has + word boundary + wrong type
            if (candidate.isBinary()) {
                childNodesOfCandidateSplit = getBinaryCandidateChildren(tree, parentNode, candidate);
            }else {
                childNodesOfCandidateSplit = getListOfCandidateChildren(tree, parentNode, candidate);
            }

            //check matrix sums
            verifyParentAndChildCoherence(tree, parentNode, candidate, childNodesOfCandidateSplit);

            //compute cost
            double costOfAddingChildrenForThisParentNode = computeCostOfAddingChildNodes(childNodesOfCandidateSplit, parentNode);
            costsOfCandidates.add(costOfAddingChildrenForThisParentNode);

            
            

            if (costOfAddingChildrenForThisParentNode < bestCost) {                
                bestCost = costOfAddingChildrenForThisParentNode;                
                bestCandidate = candidate;
                
                bestCandidate.setCost(bestCost);
                bestCandidate.setListOfChildNodesByThisCandidate(childNodesOfCandidateSplit);
                
                parentNode.setAppliedCandidate(bestCandidate);
                parentNode.setCostOfAppliedCandidate(bestCost);
                parentNode.setChildren(childNodesOfCandidateSplit);
                
                //randomize candidate selection
                bestOnes.clear();
                bestOnes.add(bestCandidate);
            } 
            else if (costOfAddingChildrenForThisParentNode == bestCost) {
                
                //randomize candidate selection
                equalCandidate = candidate;
                
                equalCandidate.setCost(bestCost);
                equalCandidate.setListOfChildNodesByThisCandidate(childNodesOfCandidateSplit);
                
                bestOnes.add(equalCandidate);
            }
        }
        
        if (bestOnes.size() > 1) {
            
            int candidateToPick = Configuration.getRnd().nextInt(bestOnes.size());            
            Candidate randomCandidate = bestOnes.get(candidateToPick);
            
            
            parentNode.setAppliedCandidate(randomCandidate);
            parentNode.setCostOfAppliedCandidate(randomCandidate.getCost());
            parentNode.setChildren(randomCandidate.getListOfChildNodesByThisCandidate());
            
            //System.out.println("bestOnes: " + bestOnes.toString());
            //System.out.println("candidateToPick: " + candidateToPick);
            //System.out.println("random Candidate: " + randomCandidate);
            //System.out.println("children: " + randomCandidate.getListOfChildNodesByThisCandidate());
            
        }
        
        bestOnes.clear();

        parentNode.setCostsOfCandidates(costsOfCandidates);

        List<Candidate> candidatesOfChildren;

        //root node might have different set of candidates
        if (parentNode.isRootNode()) {
            candidatesOfChildren = new ArrayList(CandidateFinder.getListOfCandidates(tree));
        }else {
            candidatesOfChildren = new ArrayList(candidatesOfParentNode);
        }

        candidatesOfChildren.remove(parentNode.getAppliedCandidate());

        //ja rekursio
        for (TreeNode t: parentNode.getChildren()) {
            t.setCandidates(candidatesOfChildren);
            tree.addNode(t); //depth-first-order
            // System.out.println("Kustannus: " + t.getCost());
            buildTree(tree, t);
        }
    }

    private double computeCostOfAddingChildNodes(List<TreeNode> childNodesOfCandidateSplit, TreeNode parentNode) throws Exception {

        FeatureTreeCostCalculator calc = new FeatureTreeCostCalculator();
        double totalDataCostOfChildren = 0.0;
        for (TreeNode childNode : childNodesOfCandidateSplit) {
            totalDataCostOfChildren += calc.computeDataCostOfTree(childNode);
        }
        
        //  1 for each node (split/no split), and the cost of choosing this unique candidate
        double totalModelCostOfParentAndChildren = 1 + EtyMath.base2Log(parentNode.getCandidates().size());
        totalModelCostOfParentAndChildren += childNodesOfCandidateSplit.size();
        
        double costOfAddingChildrenForThisParentNode = totalDataCostOfChildren + totalModelCostOfParentAndChildren;
        
        return costOfAddingChildrenForThisParentNode;
    }

    private List<TreeNode> getBinaryCandidateChildren(FeatureTree tree, TreeNode parentNode, Candidate candidate) throws Exception {

        Level level = candidate.getLevel();
        Context context = candidate.getContext();
        Features candidateFeature = candidate.getFeature();
        char candidateCharacter = candidate.getValue();

        String candidateValueString = String.valueOf(candidateCharacter); //a
        //String complementValueString = "\u00AC" + candidateValueString;  //not a
        String complementValueString = String.valueOf((char)172) + candidateValueString;  //not a

        List<TreeNode> currentCandidateNodes = new ArrayList<TreeNode>();
        int indexOfCandidateFeature = Features.featureNameToIndexInMap.get(candidateFeature);

        Map<Character, List<Integer>> candidateFeatureMap = null;
        Collection<Integer>  candidateFeatureIndices = null;

        //init tree info
        List<List<ContextCell>> allParentNodeContextCells = parentNode.getContextCellsForEachFeatureValue();
        int indexOfMissingValue = -1;

        FeatureVocabulary fv;
        //choose level
        if (level.equals(Level.SOURCE)) {
            candidateFeatureMap = combinedMapSource.get(indexOfCandidateFeature);
            indexOfMissingValue = fvSource.getFeatureVocabularySize()-1;
            fv = fvSource;
        } else {
            candidateFeatureMap = combinedMapTarget.get(indexOfCandidateFeature);
            indexOfMissingValue = fvTarget.getFeatureVocabularySize()-1;
            fv = fvTarget;
        }

        if (candidateCharacter == '#') {
            candidateFeatureIndices = new HashSet<Integer>();
            candidateFeatureIndices.add(indexOfMissingValue);
        }else {
            candidateFeatureIndices = new HashSet<Integer>(candidateFeatureMap.get(candidateCharacter));
        }


        int[][] candidateMatrix = new int[tree.getRootNode().getCountMatrix().length][tree.getRootNode().getCountMatrix()[0].length];
        int[][] complementMatrix = new int[tree.getRootNode().getCountMatrix().length][tree.getRootNode().getCountMatrix()[0].length];

        int numberOfRowsInContextCellList = candidateMatrix.length * candidateMatrix[0].length;
        List<List<ContextCell>> candidateNodeContextCells = new ArrayList<List<ContextCell>>(numberOfRowsInContextCellList);
        List<List<ContextCell>> complementNodeContextCells = new ArrayList<List<ContextCell>>(numberOfRowsInContextCellList);


        int kthRowInContextCellList = 0;
        for (int matrixRow = 0; matrixRow < tree.getRootNode().getCountMatrix().length; matrixRow++) {
            for (int matrixCol=0; matrixCol<tree.getRootNode().getCountMatrix()[0].length; matrixCol++) {
                candidateNodeContextCells.add(new ArrayList<ContextCell>(allParentNodeContextCells.get(kthRowInContextCellList).size()));
                complementNodeContextCells.add(new ArrayList<ContextCell>(allParentNodeContextCells.get(kthRowInContextCellList).size()));
                buildBinaryNodeMatrixCells(allParentNodeContextCells, kthRowInContextCellList,
                        level, context,
                        candidateFeatureIndices,
                        candidateNodeContextCells, complementNodeContextCells,
                        candidateMatrix, complementMatrix, matrixRow, matrixCol);
                kthRowInContextCellList++;
            }
        }

        TreeNode candidateNode = createNewNode(parentNode, candidateValueString, candidateNodeContextCells, candidateMatrix);
        currentCandidateNodes.add(candidateNode);

        TreeNode complementNode = createNewNode(parentNode, complementValueString, complementNodeContextCells, complementMatrix);
        currentCandidateNodes.add(complementNode);

        return currentCandidateNodes;
    }
    
    private TreeNode createNewNode(TreeNode parentNode, String newNodeValueString, 
            List<List<ContextCell>> newNodeContextCells, 
            int[][] matrix) throws Exception {
        
        TreeNode newNode = new TreeNode(parentNode.getDepth() + 1, newNodeValueString, newNodeContextCells);
        newNode.setCountMatrix(matrix);
        //double cost = getNodeMatrixCost(matrix);
        double cost = new FeatureTreeCostCalculator().getDataCostForTreeNode(newNode);
        
        newNode.setCost(cost);        
        
        return newNode;
    }

    private void buildBinaryNodeMatrixCells(List<List<ContextCell>> allParentNodeContextCells,
            int kthRowInContextCellList, Level level, Context context,
            Collection<Integer> candidateFeatureIndices,
            List<List<ContextCell>> candidateNodeContextCells,
            List<List<ContextCell>> complementNodeContextCells,
            int[][] candidateMatrix, int[][] complementMatrix,
            int matrixRow, int matrixCol) throws Exception {

        for (ContextCell contextCell : allParentNodeContextCells.get(kthRowInContextCellList)) {
            if (!candidateFeatureIndices.contains(contextCell.getCandidateIndexInContext(level, context))) {
                complementNodeContextCells.get(kthRowInContextCellList).add(contextCell);
            } else {
                candidateNodeContextCells.get(kthRowInContextCellList).add(contextCell);
            }
        }

        //System.out.println(newNodeAlignmentIndices.get(k).size());
        candidateMatrix[matrixRow][matrixCol] += candidateNodeContextCells.get(kthRowInContextCellList).size();
        complementMatrix[matrixRow][matrixCol] += complementNodeContextCells.get(kthRowInContextCellList).size();

    }


    private List<TreeNode> getListOfCandidateChildren(FeatureTree tree, TreeNode parentNode, Candidate candidate) throws Exception {

        Level level = candidate.getLevel();
        Context context = candidate.getContext();
        Features candidateFeature = candidate.getFeature();

        List<TreeNode> currentCandidateNodes = new ArrayList<TreeNode>();

        //both source and target side have same set of possible values of candidate feature (eg: feature= consonant length --> values/labels = {1,2}
        int indexOfCandidateFeature = Features.featureNameToIndexInMap.get(candidateFeature);
        List<Character> candidateFeatureLabels = new ArrayList<Character>(combinedMapSource.get(indexOfCandidateFeature).keySet());


        Collection<Integer>  candidateFeatureIndices = null;
        Map<Character, List<Integer>> candidateFeatureMap = null;

        //init tree info
        List<List<ContextCell>> allParentNodeContextCells = parentNode.getContextCellsForEachFeatureValue();        
        int indexOfMissingValue = -1;

        FeatureVocabulary fv;
        //choose level
        if (level.equals(Level.SOURCE)) {
            candidateFeatureMap = combinedMapSource.get(indexOfCandidateFeature);
            indexOfMissingValue = fvSource.getFeatureVocabularySize()-1;
            fv = fvSource;

        } else {
            candidateFeatureMap = combinedMapTarget.get(indexOfCandidateFeature);
            indexOfMissingValue = fvTarget.getFeatureVocabularySize()-1;
            fv = fvTarget;
        }

        //for (char c: candidateFeatureLabels) {
        for (int characterIndex=0; characterIndex<candidateFeatureLabels.size()+2; characterIndex++) {

            TreeNode currentNode = initializeNewNode(
                    tree,
                    candidateFeatureLabels, allParentNodeContextCells,
                    candidateFeatureIndices,
                    candidateFeatureMap,
                    candidateFeature, characterIndex, indexOfMissingValue,
                    context, level, parentNode, fv);

//
//            System.out.println("Has node with " + cInd + ": " + currentNode);
//
            if(currentNode != null) {
                currentCandidateNodes.add(currentNode);
            }


        }

        return currentCandidateNodes;

    }

    private TreeNode initializeNewNode(
            FeatureTree tree,
            List<Character> candidateFeatureLabels,
            List<List<ContextCell>> allParentNodeContextCells,
            Collection<Integer> candidateFeatureIndices,
            Map<Character, List<Integer>> candidateFeatureMap,
            Features candidateFeature, int cInd, int indexOfMissingValue,
            Context context, Level level, TreeNode parentNode, FeatureVocabulary fv) throws Exception {

        char character;
        int[][] matrix = new int[tree.getRootNode().getCountMatrix().length][tree.getRootNode().getCountMatrix()[0].length];
        int numberOfRowsInContextCellList = matrix.length * matrix[0].length;
                

        //the word boundary candidate
        if (cInd == candidateFeatureLabels.size()) {
            //if (context == Context.ITSELF && tree.getTreeType() != TreeType.TYPE_TREE) {

            // itself is never word boundary
            if (context == Context.ITSELF) {
                return null;
            }
            //the missing value == word boundary
            character = '#';
            candidateFeatureIndices = new HashSet<Integer>();
            candidateFeatureIndices.add(indexOfMissingValue);
        }
        //wrong type
        else if(cInd == (candidateFeatureLabels.size() + 1)) {

            if (!Context.getHistoryFutureFullFeatureSetContexts().contains(context)) {
                return null;
            }
            if (context == Context.ITSELF && level.toString().equals(tree.getBabyTreeType().toString())) {
                //cant be wrongType
                return null;
            }

            //wrong type
            character = (char)8800;
            candidateFeatureIndices = new HashSet<Integer>();

           
            if (Features.getConsonantFeatures().contains(candidateFeature) ) {
                candidateFeatureIndices.add(0); //dot
                candidateFeatureIndices.addAll(fv.getVowelIndices()); //vowels
            }else if(Features.getVowelFeatures().contains(candidateFeature) ) {
                candidateFeatureIndices.add(0); //dot
                candidateFeatureIndices.addAll(fv.getConsonantIndices()); //consonants
            }else {
                return null; //never here
            }

        } else {
            character = candidateFeatureLabels.get(cInd);
            candidateFeatureIndices = new HashSet<Integer>(candidateFeatureMap.get(character));

        }

        
        List<List<ContextCell>> newNodeContextCells = new ArrayList<List<ContextCell>>(numberOfRowsInContextCellList);


        int kthRowInContextCellList = 0;
        for (int matrixRow = 0; matrixRow < tree.getRootNode().getCountMatrix().length; matrixRow++) {
            for (int matrixCol=0; matrixCol<tree.getRootNode().getCountMatrix()[0].length; matrixCol++) {
                newNodeContextCells.add(new ArrayList<ContextCell>(allParentNodeContextCells.get(kthRowInContextCellList).size()));
                buildNodeMatrixCell(allParentNodeContextCells, kthRowInContextCellList, level, context, candidateFeatureIndices, newNodeContextCells, matrix, matrixRow, matrixCol);
                kthRowInContextCellList++;
            }
        }

        TreeNode currentNode = createNewNode(parentNode, String.valueOf(character), newNodeContextCells, matrix);

//        TreeNode currentNode = new TreeNode(parentNode.getDepth() + 1, String.valueOf(character), newNodeContextCells);
//        double cost = getNodeMatrixCost(matrix);
//        currentNode.setCountMatrix(matrix);
//        currentNode.setCost(cost);

        return currentNode;
    }


    /**
     *
     * @param allParentNodeContextCells
     * @param kthRowInContextCellList
     * @param level
     * @param context
     * @param candidateFeatureIndices
     * @param newNodeContextCell
     * @param matrix
     * @param matrixRow
     * @param matrixCol
     * @throws Exception
     */
    private void buildNodeMatrixCell(List<List<ContextCell>> allParentNodeContextCells,
            int kthRowInContextCellList, Level level, Context context, Collection<Integer> candidateFeatureIndices,
            List<List<ContextCell>> newNodeContextCell,
            int[][] matrix, int matrixRow, int matrixCol) throws Exception {
        //System.out.println(allParentNodeAlignmentIndices.get(k));
        for (ContextCell contextCell : allParentNodeContextCells.get(kthRowInContextCellList)) {
            if (!candidateFeatureIndices.contains(contextCell.getCandidateIndexInContext(level, context))) {
                continue;
            }

            //System.out.println(cc);
            newNodeContextCell.get(kthRowInContextCellList).add(contextCell);
            //System.out.println("new alignmentIndices" + newNodeAlignmentIndices);
        }

        //System.out.println(newNodeAlignmentIndices.get(k).size());
        matrix[matrixRow][matrixCol] += newNodeContextCell.get(kthRowInContextCellList).size();
        //System.out.println("matrix" + Arrays.toString(matrix));
    }


    /**
     * Checks that the sum of the values of parent node matrix equals
     * the sums of the values of all child node matrices.
     *
     * @param tree
     * @param parentNode
     * @param candidate
     * @param currentCandidateNodes
     * @throws Exception
     */
    private void verifyParentAndChildCoherence(FeatureTree tree, TreeNode parentNode, Candidate candidate, List<TreeNode> currentCandidateNodes) throws Exception {
        int parentNodeMatrixSum = 0;
        for (int[] matrixRow : parentNode.getCountMatrix()) {
            for (int numberInRowCell : matrixRow) {
                parentNodeMatrixSum += numberInRowCell;
            }
        }

        int childNodeMatrixSum = 0;
        for (TreeNode node: currentCandidateNodes) {
            for (int[] matrixRow : node.getCountMatrix()) {
                for (int numberInRowCell : matrixRow) {
                    childNodeMatrixSum += numberInRowCell;
                }
            }
        }

        if (parentNodeMatrixSum != childNodeMatrixSum) {
            System.err.println("ei mätsännyt. ");
            System.err.println(tree.getTreeType());
            System.err.println(candidate);
            System.err.println(Arrays.toString(parentNode.getCountMatrix()[0]));
            for (TreeNode node : currentCandidateNodes) {
                System.err.println(Arrays.toString(node.getCountMatrix()[0]));
            }
            System.exit(1);
        }
    }


}
