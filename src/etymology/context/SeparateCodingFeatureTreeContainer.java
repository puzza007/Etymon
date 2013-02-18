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

import etymology.input.Input;
import etymology.input.FeatureVocabulary;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import etymology.util.EtyMath;
import java.util.Arrays;
import java.util.TreeMap;


/**
 *
 * @author sxhiltun
 */
public class SeparateCodingFeatureTreeContainer implements FeatureTreeContainer {

    private boolean codeWordByWord;

    private List<List<FeatureTree>> vowelTreeContainer;
    private List<List<FeatureTree>> consonantTreeContainer;
    private List<FeatureTree> typeFeatureTreeContainer;


    private List<FeatureTree> sourceTrees;
    private List<FeatureTree> targetTrees;
    
    

    ContextCellContainer[] sourceCellContainer;
    ContextCellContainer[] targetCellContainer;

    private Input input;
    private FeatureVocabulary fvSource;
    private FeatureVocabulary fvTarget;

    private List<Map<Character, List<Integer>>> combinedMapSource;
    private List<Map<Character, List<Integer>>> combinedMapTarget;

    private FeatureTreeBuilder featureTreeBuilder;

    
   

    public SeparateCodingFeatureTreeContainer(
            ContextCellContainer[] sourceCellContainer,
            ContextCellContainer[] targetCellContainer,
            Input input, int sourceLanguageIndex, int targetLanguageIndex) throws Exception {

        this.sourceCellContainer = sourceCellContainer;
        this.targetCellContainer = targetCellContainer;
        this.input = input;
        this.fvSource = (FeatureVocabulary)input.getVocabulary(sourceLanguageIndex);
        this.fvTarget = (FeatureVocabulary)input.getVocabulary(targetLanguageIndex);
        
        init();
    }

    

    private void init() throws Exception {
        combinedMapSource = fvSource.getCombinedFeatureValueFilter();
        combinedMapTarget = fvTarget.getCombinedFeatureValueFilter();
        this.featureTreeBuilder = new FeatureTreeBuilder(
                fvSource, fvTarget,
                combinedMapSource, combinedMapTarget,
                sourceCellContainer, targetCellContainer);

        sourceTrees = new ArrayList<FeatureTree>();
        targetTrees = new ArrayList<FeatureTree>();
        rebuildAllFeatureTrees();

    }

   



    @Override
    public void rebuildAllFeatureTrees() throws Exception {

        //<Added by Javad>: Memory Leak
        sourceTrees.clear();
        targetTrees.clear();
        //</Added by Javad>
        
         //build the trees
        //System.out.println("typeTrees");
        buildTypeFeatureTrees();
        //System.out.println("vowelTrees");
        buildAllVowelTrees();
        //System.out.println("consonantTrees");
        buildAllConsonantTrees();

    }

    public List<List<FeatureTree>> buildAllVowelTrees() throws Exception {
        vowelTreeContainer = new ArrayList<List<FeatureTree>>();
        int indexOfTreesInContainer = 0;
        TreeType treeType = TreeType.VOWEL;

        for (Features featureName: Features.getVowelFeatures()) {

            vowelTreeContainer.add(new ArrayList<FeatureTree>());
            int featureIndex = Features.featureNameToIndexInMap.get(featureName);


            //labels of the matrix
            List<Character> matrixLabels = new ArrayList<Character>(combinedMapSource.get(featureIndex).keySet());
            
            //maps char values of feature to indices of features that have this char value
            Map<Character, List<Integer>> sourceMap = combinedMapSource.get(featureIndex);
            Map<Character, List<Integer>> targetMap = combinedMapTarget.get(featureIndex);

            FeatureTree tree;

            //source tree
            tree = featureTreeBuilder.buildNewSeparateCodeFeatureTree(featureName, BabyTreeType.SOURCE,
                    matrixLabels, sourceMap, treeType);
            vowelTreeContainer.get(indexOfTreesInContainer).add(tree);
            sourceTrees.add(tree);

            //target tree
            tree = featureTreeBuilder.buildNewSeparateCodeFeatureTree(featureName, BabyTreeType.TARGET,
                    matrixLabels, targetMap, treeType);
            vowelTreeContainer.get(indexOfTreesInContainer).add(tree);
            targetTrees.add(tree);

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
            List<Character> matrixLabels = new ArrayList<Character>(combinedMapSource.get(featureIndex).keySet());
            

            //maps char values of feature to indices of features that have this char value
            Map<Character, List<Integer>> sourceMap = combinedMapSource.get(featureIndex);
            Map<Character, List<Integer>> targetMap = combinedMapTarget.get(featureIndex);

            FeatureTree tree;

            //source tree
            tree = featureTreeBuilder.buildNewSeparateCodeFeatureTree(featureName, BabyTreeType.SOURCE,
                    matrixLabels, sourceMap, treeType);
            consonantTreeContainer.get(indexOfTreesInContainer).add(tree);
            sourceTrees.add(tree);

            //target tree
            tree = featureTreeBuilder.buildNewSeparateCodeFeatureTree(featureName, BabyTreeType.TARGET,
                    matrixLabels, targetMap, treeType);
            consonantTreeContainer.get(indexOfTreesInContainer).add(tree);
            targetTrees.add(tree);


            indexOfTreesInContainer++;
        }

        return consonantTreeContainer;

    }

    public List<FeatureTree> buildTypeFeatureTrees() throws Exception {

        typeFeatureTreeContainer = new ArrayList<FeatureTree>();

        TreeType treeType = TreeType.TYPE_TREE;
        Features featureName = Features.TYPE;
        int featureIndex = Features.featureNameToIndexInMap.get(featureName);


       //labels of the matrix
        List<Character> sourceTreeMatrixLabels = new ArrayList<Character>(combinedMapSource.get(featureIndex).keySet());
        sourceTreeMatrixLabels.add('#');
        

       //labels of the matrix
        List<Character> targetTreeMatrixLabels = new ArrayList<Character>(combinedMapTarget.get(featureIndex).keySet());
        //targetTreeMatrixLabels.add('#');


        List<Integer> swbind = new ArrayList<Integer>(Arrays.asList(fvSource.getFeatureVocabularySize()-1));
        //System.out.println(fvSource.getFeature(swbind.get(0)));
        //maps char values of feature to indices of features that have this char value
        Map<Character, List<Integer>> sourceMap = new TreeMap(combinedMapSource.get(featureIndex));
        sourceMap.put('#', swbind);

        //List<Integer> twbind = new ArrayList<Integer>(Arrays.asList(fvTarget.getFeatureVocabularySize()-1));
        Map<Character, List<Integer>> targetMap = new TreeMap(combinedMapTarget.get(featureIndex));
        //targetMap.put('#', twbind);


        FeatureTree tree;

        //source tree
        tree = featureTreeBuilder.buildNewSeparateCodeFeatureTree(featureName, BabyTreeType.SOURCE,
                sourceTreeMatrixLabels, sourceMap, treeType);
        typeFeatureTreeContainer.add(tree);
        sourceTrees.add(tree);

        //target tree
        tree = featureTreeBuilder.buildNewSeparateCodeFeatureTree(featureName, BabyTreeType.TARGET,
                targetTreeMatrixLabels, targetMap, treeType);
        typeFeatureTreeContainer.add(tree);
        targetTrees.add(tree);


        return typeFeatureTreeContainer;

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

        if (aki.equals(AlignmentKindIdentifier.DOTDOT)) {
            return typeFeatureTreeContainer;
        }

        //add type
        // if other than WW and DOTDOT, the typeFeatureTrees should always be modified
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
                return Arrays.asList(0,1);
            case DOTK:
            case DOTV:
                return Arrays.asList(1);
            case KDOT:
            case VDOT:
                return Arrays.asList(0);
            case VK:
                if (treeType.equals(TreeType.VOWEL)) {
                    return Arrays.asList(0);
                }else {
                    return Arrays.asList(1);
                }
            case KV:
                if (treeType.equals(TreeType.VOWEL)) {
                    return Arrays.asList(1);
                }else {
                    return Arrays.asList(0);
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
        return sourceTrees;
    }

    @Override
    public List<FeatureTree> getTargetLevelTrees() {
        return targetTrees;
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
