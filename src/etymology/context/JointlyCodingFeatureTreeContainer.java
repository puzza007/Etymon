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

import etymology.input.FeatureVocabulary;
import etymology.input.Input;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author sxhiltun
 */
public class JointlyCodingFeatureTreeContainer implements FeatureTreeContainer {

    private List<List<FeatureTree>> vowelTreeContainer;
    private List<List<FeatureTree>> consonantTreeContainer;
    private List<FeatureTree> typeFeatureTreeContainer;
    private List<FeatureTree> separateTypeTreesContainer;

    ContextCellContainer[] sourceCellContainer;
    ContextCellContainer[] targetCellContainer;

    private Input input;
    private FeatureVocabulary fvSource;
    private FeatureVocabulary fvTarget;

    private List<Map<Character, List<Integer>>> combinedMapSource;
    private List<Map<Character, List<Integer>>> combinedMapTarget;

    private FeatureTreeBuilder featureTreeBuilder;
    SeparateCodingFeatureTreeContainer scftc;



    public JointlyCodingFeatureTreeContainer(
            ContextCellContainer[] sourceCellContainer,
            ContextCellContainer[] targetCellContainer,
            Input input, int sourceLanguageId, int targetLanguageId) throws Exception {

        this.sourceCellContainer = sourceCellContainer;
        this.targetCellContainer = targetCellContainer;
        this.input = input;
        this.fvSource = (FeatureVocabulary)input.getVocabulary(sourceLanguageId);
        this.fvTarget = (FeatureVocabulary)input.getVocabulary(targetLanguageId);

        init();
    }

    private void init() throws Exception {
        combinedMapSource = fvSource.getCombinedFeatureValueFilter();
        combinedMapTarget = fvTarget.getCombinedFeatureValueFilter();
        this.featureTreeBuilder = new FeatureTreeBuilder(
                fvSource, fvTarget,
                combinedMapSource, combinedMapTarget,
                sourceCellContainer, targetCellContainer);

        //scftc = new SeparateCodingFeatureTreeContainer(sourceCellContainer, targetCellContainer, input);
        rebuildAllFeatureTrees();

    }


    @Override
    public void rebuildAllFeatureTrees() throws Exception {
        //initialize the containers
        vowelTreeContainer = new ArrayList<List<FeatureTree>>();
        consonantTreeContainer = new ArrayList<List<FeatureTree>>();
        typeFeatureTreeContainer = new ArrayList<FeatureTree>();
        separateTypeTreesContainer = new ArrayList<FeatureTree>();


        
        //build the trees
        buildJointTypeFeatureTree();
        buildAllVowelTrees();
        buildAllConsonantTrees();
        //separateTypeTreesContainer = scftc.buildTypeFeatureTrees();
        //vowelTreeContainer = scftc.buildAllVowelTrees();
        //consonantTreeContainer = scftc.buildAllConsonantTrees();
        
    }

    private void buildJointTypeFeatureTree() throws Exception {
        TreeType treeType = TreeType.TYPE_TREE;
        Features featureName = Features.TYPE;
        int featureIndex = Features.featureNameToIndexInMap.get(featureName);


        //labels of the matrix
        List<Character> sourceTreeMatrixLabels = new ArrayList<Character>(combinedMapSource.get(featureIndex).keySet());
        sourceTreeMatrixLabels.add('#');

        List<Character> targetTreeMatrixLabels = new ArrayList<Character>(combinedMapTarget.get(featureIndex).keySet());
        targetTreeMatrixLabels.add('#');

        List<Integer> sourceWordBoundaryIndex = new ArrayList<Integer>(Arrays.asList(fvSource.getFeatureVocabularySize()-1));
        List<Integer> targetWordBoundaryIndex = new ArrayList<Integer>(Arrays.asList(fvTarget.getFeatureVocabularySize()-1));

        //maps char values of feature to indices of features that have this char value
        Map<Character, List<Integer>> sourceMap = new TreeMap(combinedMapSource.get(featureIndex));
        sourceMap.put('#', sourceWordBoundaryIndex);

        Map<Character, List<Integer>> targetMap = new TreeMap(combinedMapTarget.get(featureIndex));
        targetMap.put('#', targetWordBoundaryIndex);

        FeatureTree tree;

        //joint tree
        tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.JOINT, sourceTreeMatrixLabels, targetTreeMatrixLabels, sourceMap, targetMap, treeType);
        tree.setAlignmentType(AlignmentType.STRONG);
        typeFeatureTreeContainer.add(tree);


    }


     public List<List<FeatureTree>> buildAllVowelTrees() throws Exception {
        vowelTreeContainer = new ArrayList<List<FeatureTree>>();
        int indexOfTreesInContainer = 0;
        TreeType treeType = TreeType.VOWEL;

        for (Features featureName: Features.getVowelFeatures()) {

            vowelTreeContainer.add(new ArrayList<FeatureTree>());
            int featureIndex = Features.featureNameToIndexInMap.get(featureName);

            //labels of the matrix
            List<Character> matrixLabels =  new ArrayList<Character>(combinedMapSource.get(featureIndex).keySet());
            
            List<Character> dotLabel = new ArrayList<Character>(Collections.singleton('.'));
            List<Character> wrongTypeLabel = new ArrayList<Character>(Collections.singleton('C'));

            //maps char values of feature to indices of features that have this char value
            Map<Character, List<Integer>> sourceMap = combinedMapSource.get(featureIndex);
            Map<Character, List<Integer>> targetMap = combinedMapTarget.get(featureIndex);
            Map<Character, List<Integer>> sourceDotMap  = Collections.singletonMap('.', combinedMapSource.get(0).get('.'));
            Map<Character, List<Integer>> targetDotMap  = Collections.singletonMap('.', combinedMapTarget.get(0).get('.'));
            Map<Character, List<Integer>> sourceWrongTypeMap  = Collections.singletonMap('C', combinedMapSource.get(0).get('C'));
            Map<Character, List<Integer>> targetWrongTypeMap  = Collections.singletonMap('C', combinedMapTarget.get(0).get('C'));

            FeatureTree tree;

            //STRONG TREE
            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.JOINT, matrixLabels, matrixLabels, sourceMap, targetMap, treeType);
            tree.setAlignmentType(AlignmentType.STRONG);
            vowelTreeContainer.get(indexOfTreesInContainer).add(tree);

            //WEAK TREES
            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.SOURCE, matrixLabels, dotLabel, sourceMap, targetDotMap, treeType);
            tree.setAlignmentType(AlignmentType.WEAK);
            vowelTreeContainer.get(indexOfTreesInContainer).add(tree);
            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.TARGET, dotLabel, matrixLabels, sourceDotMap, targetMap, treeType);
            tree.setAlignmentType(AlignmentType.WEAK);
            vowelTreeContainer.get(indexOfTreesInContainer).add(tree);

            //SEMI-WEAK-TREES
            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.SOURCE, matrixLabels, wrongTypeLabel, sourceMap, targetWrongTypeMap, treeType);
            tree.setAlignmentType(AlignmentType.SEMIWEAK);
            vowelTreeContainer.get(indexOfTreesInContainer).add(tree);
            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.TARGET, wrongTypeLabel, matrixLabels, sourceWrongTypeMap, targetMap, treeType);
            tree.setAlignmentType(AlignmentType.SEMIWEAK);
            vowelTreeContainer.get(indexOfTreesInContainer).add(tree);

            indexOfTreesInContainer++;
        }

        return vowelTreeContainer;

    }


    public List<List<FeatureTree>>  buildAllConsonantTrees() throws Exception {

        consonantTreeContainer = new ArrayList<List<FeatureTree>>();

        int indexOfTreesInContainer = 0;
        TreeType treeType = TreeType.CONSONANT;

        for (Features featureName: Features.getConsonantFeatures()) {

            consonantTreeContainer.add(new ArrayList<FeatureTree>());
            int featureIndex = Features.featureNameToIndexInMap.get(featureName);

            //labels of the matrix
            List<Character> matrixLabels =  new ArrayList<Character>(combinedMapSource.get(featureIndex).keySet());
            List<Character> dotLabel = new ArrayList<Character>(Collections.singleton('.'));
            List<Character> wrongTypeLabel = new ArrayList<Character>(Collections.singleton('V'));

            //maps char values of feature to indices of features that have this char value
            Map<Character, List<Integer>> sourceMap = combinedMapSource.get(featureIndex);
            Map<Character, List<Integer>> targetMap = combinedMapTarget.get(featureIndex);
            Map<Character, List<Integer>> sourceDotMap  = Collections.singletonMap('.', combinedMapSource.get(0).get('.'));
            Map<Character, List<Integer>> targetDotMap  = Collections.singletonMap('.', combinedMapTarget.get(0).get('.'));
            Map<Character, List<Integer>> sourceWrongTypeMap  = Collections.singletonMap('V', combinedMapSource.get(0).get('V'));
            Map<Character, List<Integer>> targetWrongTypeMap  = Collections.singletonMap('V', combinedMapTarget.get(0).get('V'));

            FeatureTree tree;

            //STRONG TREE
            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.JOINT, matrixLabels, matrixLabels, sourceMap, targetMap, treeType);
            tree.setAlignmentType(AlignmentType.STRONG);
            consonantTreeContainer.get(indexOfTreesInContainer).add(tree);

            //WEAK TREES
            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.SOURCE, matrixLabels, dotLabel, sourceMap, targetDotMap, treeType);
            tree.setAlignmentType(AlignmentType.WEAK);
            consonantTreeContainer.get(indexOfTreesInContainer).add(tree);

            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.TARGET, dotLabel, matrixLabels, sourceDotMap, targetMap, treeType);
            tree.setAlignmentType(AlignmentType.WEAK);
            consonantTreeContainer.get(indexOfTreesInContainer).add(tree);


            //SEMI-WEAK-TREES
            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.SOURCE, matrixLabels, wrongTypeLabel, sourceMap, targetWrongTypeMap, treeType);
            tree.setAlignmentType(AlignmentType.SEMIWEAK);
            consonantTreeContainer.get(indexOfTreesInContainer).add(tree);

            tree = featureTreeBuilder.buildNewJointCodeFeatureTree(featureName, BabyTreeType.TARGET, wrongTypeLabel, matrixLabels, sourceWrongTypeMap, targetMap, treeType);
            tree.setAlignmentType(AlignmentType.SEMIWEAK);
            consonantTreeContainer.get(indexOfTreesInContainer).add(tree);


            indexOfTreesInContainer++;
        }

        return consonantTreeContainer;

    }


    @Override
    public List<FeatureTree> getTreesForModification(AlignmentKindIdentifier aki) {
        TreeType treeType = null;
        List<FeatureTree> selectedTrees = new ArrayList<FeatureTree>();
        List<List<FeatureTree>> treeContainer = null;

        if (aki.equals(AlignmentKindIdentifier.WW)) {
            selectedTrees.add(typeFeatureTreeContainer.get(0));
            return selectedTrees;
        }

        //add type
        selectedTrees.addAll(typeFeatureTreeContainer);

        if (AlignmentKindIdentifier.getVowelTreeIdentifiers().contains(aki)) {
            treeType = TreeType.VOWEL;
            treeContainer = vowelTreeContainer;

            addTreesFromList(aki, treeType, treeContainer, selectedTrees);

        }else if (AlignmentKindIdentifier.getConsonantTreeIdentifiers().contains(aki)) {
            treeType = TreeType.CONSONANT;
            treeContainer = consonantTreeContainer;
            addTreesFromList(aki, treeType, treeContainer, selectedTrees);

        }else { //semi-weak alignment
            treeType = TreeType.VOWEL;
            treeContainer = vowelTreeContainer;
            addTreesFromList(aki, treeType, treeContainer, selectedTrees);

            treeType = TreeType.CONSONANT;
            treeContainer = consonantTreeContainer;
            addTreesFromList(aki, treeType, treeContainer, selectedTrees);

        }

        return selectedTrees;
    }


    private void  addTreesFromList(
            AlignmentKindIdentifier aki, TreeType treeType,
            List<List<FeatureTree>> treeContainer,
            List<FeatureTree> selectedTrees) {

        for (List<FeatureTree> treeList : treeContainer) {
            for (int i : getPositionInTreeList(aki, treeType)) {
                selectedTrees.add(treeList.get(i));
            }
        }
    }

    private List<Integer> getPositionInTreeList(AlignmentKindIdentifier aki, TreeType treeType) throws RuntimeException {

        switch (aki) {
            case VV:
            case KK:
                //return strong tree
                return Arrays.asList(0);
            case DOTK:
            case DOTV:
                //return weak-source-tree
                return Arrays.asList(2);
            case KDOT:
            case VDOT:
                //return weak-target-tree
                return Arrays.asList(1);
            case VK:
                if (treeType.equals(TreeType.VOWEL)) {
                    return Arrays.asList(3);
                }else {
                    return Arrays.asList(4);
                }
            case KV:
                if (treeType.equals(TreeType.VOWEL)) {
                    return Arrays.asList(4);
                }else {
                    return Arrays.asList(3);
                }

            default:
                throw new RuntimeException("Trying to retrieve tree with identifier " + aki);
        }


    }

    @Override
    public List<List<FeatureTree>> getConsonantTrees() {
        return consonantTreeContainer;
    }

    @Override
    public List<List<FeatureTree>> getVowelTrees() {
        return vowelTreeContainer;
    }

    @Override
    public List<FeatureTree> getTypeTrees() {
        return typeFeatureTreeContainer;
    }

    @Override
    public List<FeatureTree> getSourceLevelTrees() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<FeatureTree> getSeperatelyCodedTypeTrees() {
        return separateTypeTreesContainer;
    }

    @Override
    public List<FeatureTree> getTargetLevelTrees() {
        throw new UnsupportedOperationException("Not supported yet.");
    }





    //<Added by Javad for debugging and experiments> 
    @Override
    public void setConsonantTrees(List<List<FeatureTree>> trees) {
        this.consonantTreeContainer = trees;
    }

    @Override
    public void setVowelTrees(List<List<FeatureTree>> trees) {
        this.vowelTreeContainer = trees;
    }

    @Override
    public void setTypeTrees(List<FeatureTree> trees) {
        this.typeFeatureTreeContainer = trees;
    }
    //</Javad>

}
