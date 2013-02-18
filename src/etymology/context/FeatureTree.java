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
import etymology.context.FeatureTreeContainer.AlignmentType;
import etymology.context.FeatureTreeContainer.BabyTreeType;
import etymology.context.FeatureTreeContainer.Context;
import etymology.context.FeatureTreeContainer.Features;
import etymology.context.FeatureTreeContainer.Level;
import etymology.context.FeatureTreeContainer.TreeType;
import etymology.cost.FeatureTreeCostCalculator;
import etymology.input.FeatureVocabulary;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sxhiltun
 */
public abstract class FeatureTree {


    private TreeNode rootNode;

    private double modelCost;
    private double dataCost;
    private double totalCost;

    private TreeType treeType;
    private BabyTreeType babyTreeType;
    private AlignmentType alignmentType;


    private Features featureName;

    private List<TreeNode> allNodes;

    private List<Character> rowLabels;
    private List<Character> columnLabels;
    


    //-------------------constructors---------------------------//

    public FeatureTree(TreeNode root,
            List<Character> rowLabels, List<Character> columnLabels,
            BabyTreeType btt, Features featureName, TreeType treeType) {

        this(root, columnLabels, btt, featureName, treeType);
        this.rowLabels = rowLabels;
        
        
    }

    public FeatureTree(TreeNode root,
            List<Character> columnLabels,
            BabyTreeType btt, Features featureName, TreeType treeType) {

        allNodes = new ArrayList();
        allNodes.add(root);

        this.columnLabels = columnLabels;

        this.rootNode = root;
        this.modelCost = Double.POSITIVE_INFINITY;

        this.babyTreeType = btt; //strong, weak, ...  // source, target
        this.featureName = featureName; //name of the feature, voiced, rounded, ...
        this.treeType = treeType; //vowel, consonant

    }


    //------------------abstract stuff----------------------------//
    public abstract double getEventCost(TreeNode node, String sourceVector, String targetVector);
   
    // public abstract void decrementMatrixValue(TreeNode node, String sourceVector, String targetVector);
    public abstract void decrementMatrixValue(TreeNode node, String sourceVector, String targetVector);
    public abstract void incrementMatrixValue(TreeNode node, String sourceVector, String targetVector);

    //public abstract void changeWordBoundaryValue(TreeNode node, int value); //korjaa tää

    //-------------Nodes------------------------------//

    public TreeNode getRootNode() {
        return rootNode;
    }

    public List<TreeNode> getNodes() {
        return allNodes;
    }

    public void addNode(TreeNode node) {
        allNodes.add(node);
    }

    
    public String getLanguage() {
        if (this.babyTreeType == BabyTreeType.SOURCE) {
            return Configuration.getInstance().getLanguages().get(0);
        } else if (this.babyTreeType == BabyTreeType.TARGET) {
            return Configuration.getInstance().getLanguages().get(1);
        } else if (this.babyTreeType == BabyTreeType.JOINT){
            return Configuration.getInstance().getLanguages().get(0) + "-" + Configuration.getInstance().getLanguages().get(1);
        } else {
            return null;
        }
    }


   /**
     * Finds the appropriate leaf node of this tree for the given alignment.
     *
     * @param contextCell  knows indexes of vectors in all possible contexts for this aligment (sourceIdx:targetIdx)
     * @param fvSource
     * @param fvTarget feature vocabulary of target
     * @return a leaf node where aligment is counted
     * @throws Exception if there is a problem fetching the index of vector of that context
     */
    public TreeNode getLeafNodeOfAlignment(ContextCell contextCell, FeatureVocabulary fvSource, FeatureVocabulary fvTarget) throws Exception {
        TreeNode node = getRootNode();
        List<TreeNode> children;

        while (!node.getChildren().isEmpty()) {
            //identify the candidate used to split this node
            Candidate candidate = node.getAppliedCandidate();
            Level level = candidate.getLevel();
            Context context = candidate.getContext();
            Features feature = candidate.getFeature();

            //pick the feature vector in chosen context  for this alignment
            int contextVectorIndex = contextCell.getCandidateIndexInContext(level, context);


            //find the position and value of the candidate feature from vector
            int positionInContextVector= Features.featureVectorPosition.get(feature);
            String contextVectorChar;

            FeatureVocabulary fv;
            if (level == Level.SOURCE) {
                fv = fvSource;
            }else {
                fv = fvTarget;
            }

            //ok type
            if ((Features.getConsonantFeatures().contains(feature) && fv.isConsonant(contextVectorIndex)) ||
                    (Features.getVowelFeatures().contains(feature) && fv.isVowel(contextVectorIndex))) {

                contextVectorChar = String.valueOf(fv.getFeature(contextVectorIndex).charAt(positionInContextVector));

            //type always available, possibly word boundary
            }else if ((feature == Features.TYPE) || (contextVectorIndex == fv.getFeatureVocabularySize()-1)) {
                contextVectorChar = String.valueOf(fv.getFeature(contextVectorIndex).charAt(0));
            }
            else {
                //WRONG TYPE
                contextVectorChar = String.valueOf((char)8800);
            }

            //find the correct branch in tree
            children = node.getChildren();
            boolean childFound = false;
            boolean firstChildDoesNotMatch = false;

            for (TreeNode child : children) {
                String childChar = child.getValueOfCandidateFeature();

                if (childChar.equals(contextVectorChar)) {
                    node = child;
                    childFound = true;
                    break;
                } 
                // special case, binary tree!!! TODO: something more robust...
                else if(childChar.charAt(0) == (char)172 && firstChildDoesNotMatch) {
                    node = child;
                    childFound = true;
                    break;
                }
                // binary trees: must be the another node
                else {
                    firstChildDoesNotMatch = true;
                }
            }

            if (!childFound) {
                System.out.println("ChildWasNotFound!");
                System.out.println(contextVectorChar);
                System.exit(-100);
            }
        }

        return node;
    }


    //------------------Labels------------------------------//

    public List<Character> getRowLabels() {
        return rowLabels;
    }

    public List<Character> getColumnLabels() {
        return columnLabels;
    }

    //-----------------the cost of the tree, pick one.------------------------//
    
    public double getTotalTreeCost() {
        return totalCost;
    }

    public double getModelCost() {
        return modelCost;
    }

    public double getDataCost() {
        return dataCost;
    }

    public void computeTotalCostOfTree() throws Exception {
        FeatureTreeCostCalculator calc = new FeatureTreeCostCalculator();
        modelCost = calc.computeModelCostOfTree(rootNode);
        dataCost = calc.computeDataCostOfTree(rootNode);
        totalCost = modelCost + dataCost;
    }

    //---------------tree properties ------------------------------//

    public TreeType getTreeType() {
        return treeType;
    }
    
    public BabyTreeType getBabyTreeType() {
        return babyTreeType;
    }

    public Features getFeatureName() {
        return featureName;
    }

    public AlignmentType getAlignmentType() {
        return alignmentType;
    }

    public void setAlignmentType(AlignmentType alignmentType) {
        this.alignmentType = alignmentType;
    }


    //--------------------to String()-----------------------//
    @Override
    public abstract String toString();


}
