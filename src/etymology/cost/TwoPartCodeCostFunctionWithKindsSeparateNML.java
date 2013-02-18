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

/**
 *
 * @author lv
 */

import etymology.align.AlignmentMatrix;
import etymology.align.Kind;
import etymology.config.Configuration;
import etymology.util.EtyMath;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TwoPartCodeCostFunctionWithKindsSeparateNML extends TwoPartCodeCostFunction{
     @Override
    public String getName() {
        return "2P CODE COST  CODEBOOK WITH KINDS CONDITIONAL WITH KINDS NML";
    }

    @Override
    public double getCodebookCost(AlignmentMatrix matrix) {
        //System.out.println("TwoPartCodeCostFunctionWithKindsSeparateNML CodeBookCost");
        double codebookCost = 1.0; // take #-# into account
        for (Kind k : matrix.getAllKinds()) {
            codebookCost += k.getRegionCost();
        }
        //System.out.println("CODEBOOK: " + codebookCost);
        return codebookCost;
    }

    @Override
    public double getConditionalCost(AlignmentMatrix matrix) {
        //System.out.println("TwoPartCodeCostFunctionWithKindsSeparateNML ConditionalCost");

        
        double conditionalCostWithSeparateKindsNML = 0.0;
        /*
         *  L_K here prequentially
         */
        double logGammaSum = 0;
        double countPlusOneSum = 0;
        double L_D_K = 0;
        double L_K =0;
        // there are only 3 kinds in getAllKinds(), so we need to code #-# separately 

        for (Kind k : matrix.getAllKinds()) {
            try {
                L_D_K += super.getConditionalCostOfKindSeparateNML(matrix.getAlignmentCountMatrix(), matrix.getNumberOfWords(), k);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TwoPartCodeCostFunctionWithKindsSeparateNML.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //calculate L_K
            logGammaSum -= EtyMath.base2LogGamma(super.countOfKind(matrix.getAlignmentCountMatrix(), k) + 1);
            countPlusOneSum += super.countOfKind(matrix.getAlignmentCountMatrix(), k) + 1;

        }
             L_K = logGammaSum + EtyMath.base2LogGamma(countPlusOneSum) - EtyMath.base2LogGamma(4);
        
        //System.out.println("L (D|CB) : " + L_D_K);
        //System.out.println("L_K: " + L_K);
             
        //add cost for #-#
        double negativeSumCountLogCount = - matrix.getNumberOfWords()*EtyMath.base2Log(matrix.getNumberOfWords());
        double totalCountOfKLogTotalCountOfK = matrix.getNumberOfWords() * EtyMath.base2Log(matrix.getNumberOfWords());
        double logC = 0;
        try {
            logC = EtyMath.logRegret(1, matrix.getNumberOfWords());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TwoPartCodeCostFunctionWithKindsSeparateNML.class.getName()).log(Level.SEVERE, null, ex);
        }
        double L_hash = negativeSumCountLogCount + totalCountOfKLogTotalCountOfK + logC;
        conditionalCostWithSeparateKindsNML = L_D_K + L_K + L_hash;
        //System.out.println("Conditional cost with kinds separate: " + conditionalCostWithSeparateKinds);
        return conditionalCostWithSeparateKindsNML;
        
        
    }
}
