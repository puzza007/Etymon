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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author sxhiltun
 */
public class TreeNode {

    private int[][] countMatrix;
    private List<TreeNode> children;
    private double cost;
    private int depth;
    private List<Candidate> candidates;
    private List<Double> costsOfCandidates;
    private List<List<ContextCell>> alignmentCells;
    private Candidate appliedCandidate;
    private Candidate  secondLevelCandidate;
    private double costOfAppliedCandidate;
    private double costOfSecondLevelCandidate;
    private String valueOfCandidateFeature;
    private int nodeIdentityNumber;
    private FeatureTree myTree;

    public TreeNode(int depth, String valueOfCandidateFeature, List<List<ContextCell>> alignmentCells) {
        this.depth = depth;
        this.alignmentCells = alignmentCells;
        this.children = new ArrayList();
        this.valueOfCandidateFeature = valueOfCandidateFeature;
    }

    public void setNodeIdentityNumber(int nodeNumber) {
        this.nodeIdentityNumber = nodeNumber;
    }

    public int getNodeIdentityNumber() {
        return this.nodeIdentityNumber;
    }

    public void setAppliedCandidate(Candidate appliedCandidate) {
        this.appliedCandidate = appliedCandidate;
    }

    public Candidate getAppliedCandidate() {
        return appliedCandidate;
    }

    public void set2ndLevelCandidate(Candidate appliedCandidate) {
        this.secondLevelCandidate = appliedCandidate;
    }

    public Candidate get2ndLevelCandidate() {
        return secondLevelCandidate;
    }

    public int[][] getCountMatrix() {
        return countMatrix;
    }

    public void setCountMatrix(int[][] matrix) {
        this.countMatrix = matrix;
    }

    public boolean isLeafNode() {
        return children.isEmpty();
    }

    public boolean isRootNode() {
        return this.depth == 0;
    }

    /**
     * @return the children
     */
    public List<TreeNode> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }

    /**
     * @return the cost
     */
    public double getCost() {
        return cost;
    }

    /**
     * @param cost the cost to set
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * @return the candidates
     */
    public List<Candidate> getCandidates() {
        return candidates;
    }

    /**
     * @param candidates the candidates to set
     */
    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public void setCostsOfCandidates(List<Double> costsOfCandidates) {
        this.costsOfCandidates = costsOfCandidates;
    }

    public void setCostOfAppliedCandidate(double cost) {
        this.costOfAppliedCandidate = cost;
    }

    public void setCostOf2ndLevelCandidate(double cost) {
        this.costOfSecondLevelCandidate = cost;
    }

    /**
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @param depth the depth to set
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setMyTree(FeatureTree tree) {
        this.myTree = tree;
    }

    public List<List<ContextCell>> getContextCellsForEachFeatureValue() {
        return alignmentCells;
    }

    /**
     * @return the valueOfCandidateFeature
     */
    public String getValueOfCandidateFeature() {
        return valueOfCandidateFeature;
    }

    public String toComplexString() {

        DecimalFormat twoPlaces = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

        StringBuilder sb = new StringBuilder();
        char[] spaces = new char[this.depth * 5];
        Arrays.fill(spaces, ' ');

        sb.append(spaces);
        if (valueOfCandidateFeature.equals("*")) {
            sb.append("Root ").append(" ");
        } else {
            sb.append("--> ");
            sb.append(this.valueOfCandidateFeature).append(" ");
        }

        sb.append(getCountMatrixString());
        sb.append(" ");
        sb.append(twoPlaces.format(cost));
        sb.append("\n");

        if (costsOfCandidates != null && !costsOfCandidates.isEmpty()) {
            for (int i = 0; i < candidates.size(); i++) {
                sb.append(spaces).append("  ").append(candidates.get(i)).append(" ").append(twoPlaces.format(costsOfCandidates.get(i)));
                sb.append("\n");
            }


        }

        if (!children.isEmpty()) {

            sb.append(spaces).append("  ").append("The best candidate: ");
            sb.append("\n");

            sb.append(spaces).append("  ").append(appliedCandidate).append(" ").append(twoPlaces.format(costOfAppliedCandidate));
            //sb.append(" second level: ").append(secondLevelCandidate).append(" ").append(twoPlaces.format(costOfSecondLevelCandidate));
            sb.append(" (--> level ").append(this.depth + 1).append(")\n");

        }


        return sb.toString();
    }


    @Override
    public String toString() {

        DecimalFormat twoPlaces = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

        StringBuilder sb = new StringBuilder();
        char[] spaces = new char[this.depth * 5];
        int lengthOfSpaces2 = 0;

        Arrays.fill(spaces, ' ');

        sb.append(spaces);

        if (valueOfCandidateFeature.equals("*")) {
            sb.append("Root ").append(" ");
            lengthOfSpaces2 = sb.length();
        } else {
            sb.append(this.valueOfCandidateFeature).append(" ");
            sb.append("--> ");
            lengthOfSpaces2 = sb.length();
        }

        sb.append(getComplexCountMatrixString(lengthOfSpaces2));
        //sb.append(getCountMatrixString());
        if (cost != 0) {
            sb.append(" ( DEPTH ").append(getDepth()).append(" ) ");
            String cands = (getCandidates() == null) ? "?" : "" + getCandidates().size();
            sb.append(" ( CANDIDATES ").append(cands).append(" ) ");
            sb.append(" ( COST ").append(twoPlaces.format(cost)).append(" ) ");
        }
        sb.append("\n\n");


        if (!children.isEmpty()) {
            sb.append(spaces);
            sb.append("  ").append(appliedCandidate).append(" ").append(twoPlaces.format(costOfAppliedCandidate));
            sb.append("\n");
        }

        return sb.toString();
    }


    public String getCountMatrixString() {
        StringBuilder sb = new StringBuilder();
        if (cost == 0) {
            sb.append(" EMPTY");
            return sb.toString();
        }
        for (int[] row : countMatrix) {
            sb.append(Arrays.toString(row));            
        }
        return sb.toString();
    }

    public String getComplexCountMatrixString(int spacelength) {

        char[] spaces = new char[spacelength];
        Arrays.fill(spaces, ' ');

        List<Character> rowLabels = myTree.getRowLabels();
        List<Character> colLabels = myTree.getColumnLabels();
        char[] dashes = new char[colLabels.size()*3];
        Arrays.fill(dashes, '-');
        
        StringBuilder sb = new StringBuilder();
        if (cost == 0) {
            sb.append(" EMPTY");
            return sb.toString();
        }


        int counter = 0;

        sb.append("  ");
        sb.append(colLabels.toString());
        sb.append("\n");

        sb.append(spaces);
        sb.append("  ");
        sb.append(dashes);
        sb.append("\n");



        for (int[] row : countMatrix) {
        
            sb.append(spaces);
        
            sb.append(rowLabels.get(counter));
            sb.append(" ");
            sb.append(Arrays.toString(row));

            if (counter < countMatrix.length-1) {
                sb.append("\n");
            }

            counter++;
        }

        return sb.toString();
    }



}
