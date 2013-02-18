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
import java.util.Arrays;
import java.util.TreeMap;


/**
 *
 * @author sxhiltun
 */
public class OneLevelOnlyFeatureTreeContainer implements FeatureTreeContainer{
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

    private List<List<FeatureTree>> vowelTreeContainer;
    private List<List<FeatureTree>> consonantTreeContainer;
    private List<FeatureTree> typeFeatureTreeContainer;


    private List<FeatureTree> sourceTrees;           
    private ContextCellContainer[] sourceCellContainer;
    
    private Input input;
    private FeatureVocabulary fvSource;
    private FeatureVocabulary fvTarget;

    private List<Map<Character, List<Integer>>> combinedMapSource;    

    private FeatureTreeBuilder featureTreeBuilder;

    
   

    public OneLevelOnlyFeatureTreeContainer(
            ContextCellContainer[] sourceCellContainer,            
            Input input, int sourceLanguageIndex) throws Exception {

        this.sourceCellContainer = sourceCellContainer;        
        this.input = input;
        this.fvSource = (FeatureVocabulary)input.getVocabulary(sourceLanguageIndex);        
        
        init();
    }

    

    private void init() throws Exception {
        combinedMapSource = fvSource.getCombinedFeatureValueFilter();
        
        this.featureTreeBuilder = new FeatureTreeBuilder(
                fvSource,
                combinedMapSource,
                sourceCellContainer);

        sourceTrees = new ArrayList<FeatureTree>();        
        rebuildAllFeatureTrees();
    }

   



    @Override
    public void rebuildAllFeatureTrees() throws Exception {

                
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
            
            FeatureTree tree;

            //source tree
            tree = featureTreeBuilder.buildNewSeparateCodeFeatureTree(featureName, BabyTreeType.SOURCE,
                    matrixLabels, sourceMap, treeType);
            vowelTreeContainer.get(indexOfTreesInContainer).add(tree);
            sourceTrees.add(tree);

            
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

            FeatureTree tree;

            //source tree
            tree = featureTreeBuilder.buildNewSeparateCodeFeatureTree(featureName, BabyTreeType.SOURCE,
                    matrixLabels, sourceMap, treeType);
            consonantTreeContainer.get(indexOfTreesInContainer).add(tree);
            sourceTrees.add(tree);

            
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
             
        List<Integer> swbind = new ArrayList<Integer>(Arrays.asList(fvSource.getFeatureVocabularySize()-1));
        
        //maps char values of feature to indices of features that have this char value
        Map<Character, List<Integer>> sourceMap = new TreeMap(combinedMapSource.get(featureIndex));
        sourceMap.put('#', swbind);

        
        FeatureTree tree;

        //source tree
        tree = featureTreeBuilder.buildNewSeparateCodeFeatureTree(featureName, BabyTreeType.SOURCE,
                sourceTreeMatrixLabels, sourceMap, treeType);
        typeFeatureTreeContainer.add(tree);
        sourceTrees.add(tree);

        
        return typeFeatureTreeContainer;

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
    public List<FeatureTree> getTreesForModification(AlignmentKindIdentifier aki) {
        throw new UnsupportedOperationException("Not supported yet.");
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
