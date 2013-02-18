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

import etymology.align.AlignmentMatrix;
import etymology.align.Kind;
import etymology.util.EtyMath;

/**
 *
 * @author lv
 */
public class TwoPartCodeCostFunctionWithKindsSeparate extends TwoPartCodeCostFunction{
    @Override
    public String getName() {
        return "2P CODE COST  CODEBOOK WITH KINDS CONDITIONAL WITH KINDS";
    }

    @Override
    public double getCodebookCost(AlignmentMatrix matrix) {
        //System.out.println("TwoPartCodeCostFunctionWithKindsSeparate CodeBookCost");
        double codebookCost = 1.0; // take #-# into account
        for (Kind k : matrix.getAllKinds()) {
            codebookCost += k.getRegionCost();
        }
        //System.out.println("CODEBOOK: " + codebookCost);
        return codebookCost;
    }

    @Override
    public double getConditionalCost(AlignmentMatrix matrix) {
        
        double conditionalCostWithSeparateKinds = 0.0;
        /*
         *  L_K here prequentially
         */
        double logGammaSum = 0;
        double countPlusOneSum = 0;
        double L_D_K = 0;
        double L_K =0;
        // there are only 3 kinds in getAllKinds(), so we need to code #-# separately 

        
        
        for (Kind k : matrix.getAllKinds()) {
            L_D_K += super.getConditionalCostOfKindSeparate(matrix.getAlignmentCountMatrix(), matrix.getNumberOfWords(), k);
            
            //calculate L_K
            
            logGammaSum -= EtyMath.base2LogGamma(super.countOfKind(matrix.getAlignmentCountMatrix(), k) + 1);
            countPlusOneSum += super.countOfKind(matrix.getAlignmentCountMatrix(), k) + 1;
        }
        
        // add cost of #-#
        double negativeSumLogGammaCounts = -1.0 * EtyMath.base2LogGamma(matrix.getNumberOfWords() + 1);
        double positiveSumLogGammaPriors = EtyMath.base2LogGamma(1);//0
        double positiveLogGammaSumCounts = (matrix.getNumberOfWords() + 1);

        double L_hash = negativeSumLogGammaCounts
                + positiveSumLogGammaPriors
                + EtyMath.base2LogGamma(positiveLogGammaSumCounts);
        
        L_K = logGammaSum + EtyMath.base2LogGamma(countPlusOneSum) - EtyMath.base2LogGamma(4);
        //System.out.println("L (D|CB) : " + L_D_K);
        //System.out.println("L_K: " + L_K);
        conditionalCostWithSeparateKinds = L_D_K + L_K + L_hash;
        //System.out.println("Conditional cost with kinds separate: " + conditionalCostWithSeparateKinds);
        return conditionalCostWithSeparateKinds;
        
        
    }
}
