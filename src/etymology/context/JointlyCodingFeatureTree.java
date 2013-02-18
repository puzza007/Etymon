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
import etymology.context.FeatureTreeContainer.Features;
import etymology.context.FeatureTreeContainer.TreeType;
import etymology.cost.CostFunctionIdentifier;
import etymology.cost.PrequentialCodeLengthCostFunction;
import etymology.util.EtyMath;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;


/**
 *
 * @author sxhiltun
 */
public class JointlyCodingFeatureTree extends FeatureTree {

    private boolean doNML = false;
    
    public JointlyCodingFeatureTree(TreeNode root,
            List<Character> sourceLabels, List<Character> targetLabels,
            BabyTreeType btt, Features featureName, TreeType treeType) {

        super(root, sourceLabels, targetLabels, btt, featureName, treeType);
        
        if (Configuration.getInstance().getCostFunctionIdentifier().equals(CostFunctionIdentifier.PREQUENTIAL)) {
            doNML = false;
        }else if(Configuration.getInstance().getCostFunctionIdentifier().equals(CostFunctionIdentifier.NML)) {
            doNML = true;
            throw new RuntimeException("NML NOT implemented for joint context model yet!: " + Configuration.getInstance().getCostFunctionIdentifier());
        }else {
            throw new RuntimeException("UNKNOWN COST TYPE: " + Configuration.getInstance().getCostFunctionIdentifier());
        }
    }


    @Override
    public double getEventCost(TreeNode node, String sourceVector, String targetVector) {


        //determine the position of the event in matrix
        int positionInVector = Features.featureVectorPosition.get(getFeatureName());
        int rowIndexOfMatrix = -1;
        int colIndexOfMatrix = -1;

        if (getBabyTreeType() == BabyTreeType.JOINT) {
            rowIndexOfMatrix = getRowLabels().indexOf(sourceVector.charAt(positionInVector));
            colIndexOfMatrix = getColumnLabels().indexOf(targetVector.charAt(positionInVector));

        } else if (getBabyTreeType() == BabyTreeType.SOURCE ){
            rowIndexOfMatrix = getRowLabels().indexOf(sourceVector.charAt(positionInVector));
            colIndexOfMatrix = 0;

        } else if (getBabyTreeType() == BabyTreeType.TARGET) {
            rowIndexOfMatrix = 0;
            colIndexOfMatrix = getColumnLabels().indexOf(targetVector.charAt(positionInVector));
        } else {
            throwErrorMessage(node);
        }

        int[][] matrix = node.getCountMatrix();

        int eventCount = matrix[rowIndexOfMatrix][colIndexOfMatrix];

        //use formula (20)
        int sumOfEventTypes = matrix.length * matrix[0].length; //number of cells
        int sumOfEvents = 0;

        for(int[] row: matrix) {
            for (int count : row) {
                sumOfEvents += count;
            }
        }
        
        double costOfEvent = (eventCount + 1.0) / (sumOfEvents + sumOfEventTypes);

        return -EtyMath.base2Log(costOfEvent);
    }


    @Override
    public void incrementMatrixValue(TreeNode node, String sourceVector, String targetVector) {

        changeMatrixValue(node, sourceVector, targetVector, 1);
    }

    @Override
    public void decrementMatrixValue(TreeNode node, String sourceVector, String targetVector) {

        changeMatrixValue(node, sourceVector, targetVector, -1);
    }



    private void changeMatrixValue(TreeNode node, String sourceVector, String targetVector, int value) {
        //determine the position of the event in matrix
        int positionInVector = Features.featureVectorPosition.get(getFeatureName());
        int rowIndexOfMatrix = -1;
        int colIndexOfMatrix = -1;

        if (getBabyTreeType() == BabyTreeType.JOINT) {
            rowIndexOfMatrix = getRowLabels().indexOf(sourceVector.charAt(positionInVector));
            colIndexOfMatrix = getColumnLabels().indexOf(targetVector.charAt(positionInVector));

        } else if (getBabyTreeType() == BabyTreeType.SOURCE ){
            rowIndexOfMatrix = getRowLabels().indexOf(sourceVector.charAt(positionInVector));
            colIndexOfMatrix = 0;

        } else if (getBabyTreeType() == BabyTreeType.TARGET) {
            rowIndexOfMatrix = 0;
            colIndexOfMatrix = getColumnLabels().indexOf(targetVector.charAt(positionInVector));
        } else {
            throwErrorMessage(node);
        }


        int[][] matrix = node.getCountMatrix();  
        
        StringBuilder matBuilder;        
         PrequentialCodeLengthCostFunction calcu = new PrequentialCodeLengthCostFunction();
        double costBeforeIncrement = calcu.getCodeLength(matrix);
        matrix[rowIndexOfMatrix][colIndexOfMatrix] += value;
        double costAfterIncrement = calcu.getCodeLength(matrix);        
        node.setCountMatrix(matrix);
        if(matrix[rowIndexOfMatrix][colIndexOfMatrix] < 0) {
            throwErrorMessage(node);
        }
        

    }

    private void throwErrorMessage(TreeNode node) {

            System.err.println(getFeatureName());
            System.err.println(getColumnLabels());
            System.err.println(node);
            throw new RuntimeException("Something is wrong, matrix cell got negative value!");

    }

    @Override
    public String toString() {
        DecimalFormat twoPlaces = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

        StringBuilder sb = new StringBuilder();
        sb.append("Tree: ").append(getTreeType()).append("\n");
        sb.append("Baby tree: ").append(getBabyTreeType()).append("\n");
        sb.append("Alignment type: ").append(getAlignmentType()).append("\n");
        sb.append("Feature: ").append(getFeatureName()).append("\n");
        sb.append("Source labels: ").append(getRowLabels()).append("\n");
        sb.append("Column labels: ").append(getColumnLabels()).append("\n");
        sb.append("Number of candidates: ").append(getRootNode().getCandidates().size()).append("\n");
        sb.append("Total cost: ").append(twoPlaces.format(getTotalTreeCost())).append(" Model Cost: ").append(twoPlaces.format(getModelCost())).append(" Data Cost: ").append(twoPlaces.format(getDataCost())).append("\n");
        sb.append("------------------------\n");

        for (TreeNode t : getNodes()) {
            t.setMyTree(this);
            sb.append(t);
        }
        sb.append("\n");

        return sb.toString();
    }


  
}
