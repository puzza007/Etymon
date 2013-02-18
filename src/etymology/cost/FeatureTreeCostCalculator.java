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

package etymology.cost;

import etymology.config.Configuration;
import etymology.context.TreeNode;
import etymology.util.EtyMath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author avihavai
 */
public class FeatureTreeCostCalculator {
    
    FeatureTreeDataCostFunction dataCostFunction;
    
    public FeatureTreeCostCalculator() throws Exception {
        
        if (Configuration.getInstance().getCostFunctionIdentifier().equals(CostFunctionIdentifier.PREQUENTIAL)) {
             dataCostFunction = new PrequentialCodeLengthCostFunction();
        } else if (Configuration.getInstance().getCostFunctionIdentifier().equals(CostFunctionIdentifier.NML)) {
            dataCostFunction = new NMLCodeLengthCostFunction();
        } else {
            throw new Exception("UNKNOWN COST FUNCTION! Use prequential or NML cost function.");
        }
    }

    public double computeDataCostOfTree(TreeNode rootNode) {
        double dataCost = 0.0;
        for (TreeNode t : getAllNodesInSubTree(rootNode)) {
            if(!t.isLeafNode()) {
                continue;
            }

            dataCost += getDataCostForTreeNode(t);
        }

        return dataCost;
    }

    public double getDataCostForTreeNode(TreeNode node) {
                
        return dataCostFunction.getCodeLength(node.getCountMatrix());
    }



    public double computeModelCostOfTree(TreeNode node) {
        int rootNodeCandidates = node.getCandidates().size();
        double model = 0.0;

        //compute the total cost of the tree
        for (TreeNode t : getAllNodesInSubTree(node)) {
            model += 1;

            if(!t.isLeafNode()) {
                //int numOfCandidatesMinusDepth = rootNodeCandidates - t.getDepth();
                int numOfCandidatesMinusDepth = t.getCandidates().size();
                model += EtyMath.base2Log(numOfCandidatesMinusDepth);
            }
        }

        return model;
    }

    public Collection<TreeNode> getAllNodesInSubTree(TreeNode node) {
        List<TreeNode> nodes = new ArrayList();
        nodes.add(node);
        
        for (TreeNode child : node.getChildren()) {
            nodes.addAll(getAllNodesInSubTree(child));
        }

        return nodes;
    }
}
