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

package etymology.output;

import etymology.config.Configuration;
import etymology.context.FeatureTree;
import etymology.context.TreeNode;
import etymology.config.Constants;
import etymology.context.FeatureTreeContainer.BabyTreeType;
import etymology.context.JointlyCodingFeatureTree;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sxhiltun
 */
public class TreeGraphPrinter {
    
    public enum EdgeLabels {
        
        FEATURE_VALUE,
        CANDIDATE_USED;

        private String attributeValue;
        
        public void setAttributeValue(String attributeValue) {
            this.attributeValue = attributeValue;
        }
                       
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (attributeValue != null) {
                sb.append(this.name().toLowerCase());
                sb.append("=");
                removeSpaces();
                sb.append(attributeValue);
                sb.append(" ");
                return sb.toString();
            }
            return "";
        }

        public String getDefaultValueString() {
            return attributeValue + " ";
        }

        //in bmvis-format, all spaces in attribute values must be replaced with '+'
        private void removeSpaces() {
            this.attributeValue = this.attributeValue.trim();
            this.attributeValue = this.attributeValue.replace(" ", "+");
        }

                
    }

    /**
     * Contains the names of all attributes of the nodes
     */
    public enum NodeLabels {
        LABEL,          //default label?
        TREE_LEVEL,      //baby-tree-type, source- or target level tree
        FEATURE_NAME,    //name of the feature
        ALIGNMENT_TYPE,  //STRONG;WEAK; SEMIWEAK
        TREE_TYPE,
        TOTAL_COST_OF_TREE,
        MODEL_COST_OF_TREE,
        DATA_COST_OF_TREE,
        MATRIX,         //count matrix
        MATRIX_COST, 
        COLUMN_LABELS,
        ROW_LABELS,
        NUMBER_OF_CANDIDATES,
        BEST_CANDIDATE, //only if there is one
        BEST_CANDIDATE_COST, //cost of adding this candidate, data + more complex model, maybe should be removed / cost not for complete tree!!!
        FEATURE_VALUE_IN_PREVIOUS_SPLIT, //the value of the feature used to split the parent node of this node
        FILL, //the color of the node
        POS,
        ENTROPY;

        private String attributeValue;

        /**
         * set attribute value before calling toString()-method
         * @param attributeValue the value of called attribute as String
         */
        public void setAttributeValue(String attributeValue) {
            this.attributeValue = attributeValue;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (attributeValue != null) {
                removeSpaces();
                sb.append(this.name().toLowerCase());
                sb.append("=");
                sb.append(attributeValue);
                sb.append(" ");
                return sb.toString();
            }
            return "";
        }

        //in bmvis-format, all spaces in attribute values must be replaced with '+'
        private void removeSpaces() {
            //this.attributeValue = this.attributeValue.trim();
            this.attributeValue = this.attributeValue.replace(" ", "+");
        }

    }

    FileWriter fw;
    List<FeatureTree> allTrees;
    private int nodeCounter;
    double minEntropy;
    double maxEntropy;

    int prevSourceXCoordinate;
    int prevTargetXCoordinate;


    public TreeGraphPrinter(FeatureTree tree) throws IOException {
        this(new ArrayList<FeatureTree>(Arrays.asList(tree)), 0);

    }

    public TreeGraphPrinter(List<FeatureTree> allTrees, int runNumber) throws IOException {

        fw = createGraphFile(runNumber);
        this.allTrees = allTrees;
        this.nodeCounter = 0;

        for (FeatureTree tree : allTrees) {
            printTree(tree);
        }

        fw.close();
    }

    private FileWriter createGraphFile(int runNumber) {
        String iternum = String.valueOf(runNumber) + "-";
        String fileName = Configuration.getInstance().getCommonPrefixOfContextBasedOutputFileNames() + iternum + Constants.GRAPH_NAME;
        System.out.println("file name: " + fileName);
        FileWriter file = null;
        try {
            file = new FileWriter(new File(fileName));
        } catch (IOException ex) {
            Logger.getLogger(TreeGraphPrinter.class.getName()).log(Level.SEVERE, null, ex);
        }

        return file;
    }

    private void printTree(FeatureTree tree) {
        minEntropy = Double.POSITIVE_INFINITY;
        maxEntropy = Double.NEGATIVE_INFINITY;

        for (TreeNode node : tree.getNodes()) {
            
            //if node matrix contains only zeros, don't print
            if (node.getCost() != 0 ) {
                node.setNodeIdentityNumber(nodeCounter);
                nodeCounter += 1;

                double nodeEntropy = computeEntropy(node.getCountMatrix());
                if (nodeEntropy > maxEntropy) {
                    maxEntropy = nodeEntropy;
                }
                if (nodeEntropy < minEntropy) {
                    minEntropy = nodeEntropy;
                }
            }
        }

        for (TreeNode node : tree.getNodes()) {
            try {
                if (node.getCost() != 0 ) {
                    fw.write(getEdgeString(tree, node));
                    fw.write(getNodeString(tree, node));
                }
            } catch (IOException ex) {
                Logger.getLogger(TreeGraphPrinter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private double computeEntropy(int[][] matrix) {
        int totalSum = 0;
        double entropy = 0;
        for (int[] row : matrix) {
            for (int count: row) {
                totalSum += count;
            }
        }

        for (int[] row : matrix) {
            for (int x: row) {
               entropy -= p(x, totalSum)*log2(p(x, totalSum));
            }
        }

        return entropy;
    }

    private double log2(double val) {
        if (val == 0) {
            return 0;
        }
        return Math.log(val)/Math.log(2);
    }

    private double p(double x, double total) {
        if (total == 0) {
            return 0;
        }
        return x/total;
    }

    private String getEdgeString(FeatureTree tree, TreeNode parentNode) {
        StringBuilder sb = new StringBuilder();

        
        for (TreeNode child : parentNode.getChildren()) {
            if (child.getCost() != 0 ) {
                sb.append(getNodeTypeAndDatabaseIdString(tree, parentNode));
                sb.append(getNodeTypeAndDatabaseIdString(tree, child));

                String value = String.valueOf(child.getValueOfCandidateFeature());
                value = fixPlusAndMinusSigns(value);
                EdgeLabels.FEATURE_VALUE.setAttributeValue(value);
                sb.append(EdgeLabels.FEATURE_VALUE.getDefaultValueString());
                sb.append(EdgeLabels.FEATURE_VALUE.toString());

                value = String.valueOf(parentNode.getAppliedCandidate());
                value = fixPlusAndMinusSigns(value);
                EdgeLabels.CANDIDATE_USED.setAttributeValue(value);
                sb.append(EdgeLabels.CANDIDATE_USED.toString());

                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String getNodeTypeAndDatabaseIdString(FeatureTree tree, TreeNode node) {

        StringBuilder sb = new StringBuilder();

        //type-attribute
        //level of the tree        
        sb.append(tree.getBabyTreeType());
        sb.append("/");

         if (tree.getClass() == JointlyCodingFeatureTree.class) {
             sb.append(tree.getAlignmentType());
             sb.append("/");
         }

        //name of the feature        
        sb.append(tree.getFeatureName());
        sb.append("_");
        
        //unique node number -  database_id -attribute
        int nodeIdentity = node.getNodeIdentityNumber();
        sb.append(nodeIdentity);

        sb.append(" ");

        return sb.toString();
    }



    private String getNodeString(FeatureTree tree, TreeNode parentNode) {
        StringBuilder sb = new StringBuilder();
     

        //each node line begins with this
        sb.append("# _attributes ");
        
        String language = tree.getLanguage();
        
        

        //the default name of the node; here the name of the feature        
        sb.append(getNodeTypeAndDatabaseIdString(tree, parentNode));

        if (parentNode.isRootNode()) {
            //add some extra attributes for root node
            sb.append(getAttributeString(NodeLabels.TOTAL_COST_OF_TREE, tree.getTotalTreeCost()));
            sb.append(getAttributeString(NodeLabels.MODEL_COST_OF_TREE, tree.getModelCost()));
            sb.append(getAttributeString(NodeLabels.DATA_COST_OF_TREE, tree.getDataCost()));            
            sb.append(getAttributeString(NodeLabels.FEATURE_NAME,   tree.getFeatureName()));
            sb.append(getAttributeString(NodeLabels.TREE_LEVEL,     tree.getBabyTreeType().toString() + ":" + language));
            sb.append(getAttributeString(NodeLabels.TREE_TYPE,  tree.getTreeType()));
            sb.append(getAttributeString(NodeLabels.COLUMN_LABELS,  tree.getColumnLabels()));
            if (tree.getClass() == JointlyCodingFeatureTree.class) {
                sb.append(getAttributeString(NodeLabels.ROW_LABELS,  tree.getRowLabels()));
                sb.append(getAttributeString(NodeLabels.ALIGNMENT_TYPE, tree.getAlignmentType()));
            }
            if (tree.getBabyTreeType() != BabyTreeType.JOINT) {
                sb.append(getAttributeString(NodeLabels.POS, getRootNodePositionString(tree, parentNode)));
            }
            sb.append("queryset=root ");

        } else {
            //not root node
            sb.append(getAttributeString(NodeLabels.FILL, getColor(minEntropy, maxEntropy, computeEntropy(parentNode.getCountMatrix()))));
        }
        
        //default label, if not defined, the getNodeDatabaseIdString is shown in nodes instead
        sb.append(getAttributeString(NodeLabels.LABEL,          getMatrixString(tree, parentNode)));

        //attributes that all nodes share
        sb.append(getAttributeString(NodeLabels.MATRIX_COST,    parentNode.getCost()));
        sb.append(getAttributeString(NodeLabels.MATRIX,         getMatrixString(tree, parentNode)));
        
        
        sb.append(getAttributeString(NodeLabels.FEATURE_VALUE_IN_PREVIOUS_SPLIT, parentNode.getValueOfCandidateFeature()));
        sb.append(getAttributeString(NodeLabels.NUMBER_OF_CANDIDATES, parentNode.getCandidates().size()));
        sb.append(getAttributeString(NodeLabels.ENTROPY, computeEntropy(parentNode.getCountMatrix())));
        

        //inner nodes have been split
        if (parentNode.getAppliedCandidate() != null) {
            sb.append(getAttributeString(NodeLabels.BEST_CANDIDATE, parentNode.getAppliedCandidate()));
            sb.append(getAttributeString(NodeLabels.BEST_CANDIDATE_COST, parentNode.getAppliedCandidate().getCost()));
        }

        sb.append("\n");

        return sb.toString();
    }

    private String getMatrixString(FeatureTree tree, TreeNode node) {
        StringBuilder sb = new StringBuilder();
        int[][] matrix = node.getCountMatrix();
        if (tree.getClass() != JointlyCodingFeatureTree.class) {
            return Arrays.toString(matrix[0]);
        }else {
            int counter = 0;
            sb.append("    ");
            for (char c: tree.getColumnLabels()) {

                sb.append(c).append("  ");
            }
            sb.append("\\n");
            for (int[] row : matrix) {
                sb.append(tree.getRowLabels().get(counter)).append(" ");
                sb.append(Arrays.toString(row)).append("\\n");
                counter++;
            }
            //sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }

    private String getAttributeString(NodeLabels nodeLabel, char valueOfAttribute) {
        String  value = String.valueOf(valueOfAttribute);
        return getAttributeString(nodeLabel, value);
    }

    private String getAttributeString(NodeLabels nodeLabel, double valueOfAttribute) {
        String value = Constants.COST_FORMAT.format(valueOfAttribute);
        return getAttributeString(nodeLabel, value);
    }

    private String getAttributeString(NodeLabels nodeLabel, Object valueOfAttribute) {
        String value = String.valueOf(valueOfAttribute);
        value = fixPlusAndMinusSigns(value);
        nodeLabel.setAttributeValue(value);
        return nodeLabel.toString();
    }

    private String fixPlusAndMinusSigns(String value) {
        // normal plus and minus signs cause strange things;
        // also '+' -signs mean whitespace-character in graph, cannot be seen...

        value = value.replace("+", "\u2295"); //circled plus
        value = value.replace("-", "\u2296"); //circled minus
        return value;
    }
    

    private String getColor(double min, double max, double val) {
        double colorPercentage = (val - min) / (max - min);
//         148/130/237

        int b = (int) (130 + (125 * (1- colorPercentage)));
        int g = 130;
        int r = (int) (130 + (125 * (colorPercentage)));

        return r + "/" + g + "/" + b;
    }

    private String getRootNodePositionString(FeatureTree tree, TreeNode node) {
        StringBuilder sb = new StringBuilder();

        int x = 0;
        int y = 0;

        
        if (tree.getBabyTreeType().equals(BabyTreeType.SOURCE)) {
            prevSourceXCoordinate += 1000;
            x = prevSourceXCoordinate;
            y = 0;
            
        }else {
            prevTargetXCoordinate += 1000;
            x = prevTargetXCoordinate;
            y = 2000;
        }

        sb.append(x);
        sb.append(",");
        sb.append(y);


        return sb.toString();
    }


}
