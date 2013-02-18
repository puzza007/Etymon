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
import etymology.cost.CostFunctionIdentifier;
import etymology.input.FeatureVocabulary;
import etymology.util.EtyMath;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 *
 * @author sxhiltun
 */
public class SeparateCodingFeatureTree extends FeatureTree {

     private boolean doNML = false;
     
     public SeparateCodingFeatureTree(TreeNode root,
            List<Character> labels,
            BabyTreeType btt, Features featureName, TreeType treeType) {

        super(root, labels, btt, featureName, treeType);
        
        if (Configuration.getInstance().getCostFunctionIdentifier().equals(CostFunctionIdentifier.PREQUENTIAL)) {
            doNML = false;
        }else if(Configuration.getInstance().getCostFunctionIdentifier().equals(CostFunctionIdentifier.NML)) {
            doNML = true;
        }else {
            throw new RuntimeException("UNKNOWN COST TYPE: " + Configuration.getInstance().getCostFunctionIdentifier());
        }
        
    }


    // TODO: VERIFY
    @Override
    public double getEventCost(TreeNode node, String sourceVector, String targetVector) {
        String symbolVector;

        if (getBabyTreeType().equals(BabyTreeType.SOURCE)) {
            symbolVector = sourceVector;
        }else if (getBabyTreeType().equals(BabyTreeType.TARGET)) {
            symbolVector = targetVector;
        }else {
            symbolVector = null;
        }

        //determine the position of the event in matrix
        int positionInVector = Features.featureVectorPosition.get(this.getFeatureName());
        int colIndexOfMatrix = getColumnLabels().indexOf(symbolVector.charAt(positionInVector));



        int[][] matrix = node.getCountMatrix();
        int eventCount = matrix[0][colIndexOfMatrix];

        //use formula (20)
        int sumOfAllEventTypes = matrix[0].length;
        int sumOfEvents = 0;

        for(int count: matrix[0]) {
            sumOfEvents += count;
        }
        
        if (!doNML) {
            double costOfEvent = (eventCount + 1.0) / (sumOfEvents + sumOfAllEventTypes);
            return -EtyMath.base2Log(costOfEvent);
        }
        
        
        //else, do nml
        double costOfEvent = 0;
        try {
            costOfEvent = - EtyMath.xbase2logx(eventCount+1)  
                         + EtyMath.xbase2logx(sumOfEvents+1)                 
                         + EtyMath.xbase2logx(eventCount)
                         - EtyMath.xbase2logx(sumOfEvents)
                         + EtyMath.logRegret(sumOfAllEventTypes, sumOfEvents+1)
                         - EtyMath.logRegret(sumOfAllEventTypes, sumOfEvents);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SeparateCodingFeatureTree.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
                        
        return costOfEvent;
    }





    @Override
    public void incrementMatrixValue(TreeNode node, String sourceVector, String targetVector) {
        
        String symbolVector;
        if (getBabyTreeType().equals(BabyTreeType.SOURCE)) {
            symbolVector = sourceVector;
        }else if (getBabyTreeType().equals(BabyTreeType.TARGET)) {
            symbolVector = targetVector;
        }else {
            symbolVector = null;
        }


        changeMatrixValue(node, symbolVector, 1);

    }

    @Override
    public void decrementMatrixValue(TreeNode node, String sourceVector, String targetVector) {

        String symbolVector;
        if (getBabyTreeType().equals(BabyTreeType.SOURCE)) {
            symbolVector = sourceVector;
        }else if (getBabyTreeType().equals(BabyTreeType.TARGET)) {
            symbolVector = targetVector;
        }else {
            symbolVector = null;
        }
        
        changeMatrixValue(node, symbolVector, -1);

    }


    private void changeMatrixValue(TreeNode node, String symbolVector, int value) {
        //determine the position of the event in matrix
        int positionInVector = Features.featureVectorPosition.get(getFeatureName());       
        int colIndexOfMatrix = getColumnLabels().indexOf(symbolVector.charAt(positionInVector));
        

        int[][] matrix = node.getCountMatrix();

        matrix[0][colIndexOfMatrix] += value;
        node.setCountMatrix(matrix);

        if(matrix[0][colIndexOfMatrix] < 0) {
            System.err.println(getFeatureName());
            System.err.println(getColumnLabels());
            System.err.println(node);
            throw new RuntimeException("Something is wrong, matrix cell got negative value!");

        }


    }




    @Override
    public String toString() {

        DecimalFormat twoPlaces = new DecimalFormat("0.00",new DecimalFormatSymbols(Locale.US));

        StringBuilder sb = new StringBuilder();
        sb.append("Tree: ").append(getTreeType()).append("\n");
        sb.append("Baby tree: ").append(getBabyTreeType()).append("\n");
        sb.append("Feature: ").append(getFeatureName()).append("\n");
        //sb.append("Source labels: ").append(getRowLabels()).append("\n");
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

    @Override
   public List<Character> getRowLabels() {
        List<Character> labels = new ArrayList();
        labels.add(' ');
        return labels;
    }






}
