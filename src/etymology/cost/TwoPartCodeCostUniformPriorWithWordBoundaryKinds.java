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
 * @author sxhiltun
 */
public class TwoPartCodeCostUniformPriorWithWordBoundaryKinds extends TwoPartCodeCostFunction {

    @Override
    public String getName() {
        return "2P CODE COST WITH KINDS (including word boundary kinds) - EQ16";
    }

    @Override
    public double getCodebookCost(AlignmentMatrix matrix) {
        double codebookCost = 0; //  #-# is one of the kinds now
        for (Kind k : matrix.getAllKinds()) {
            //region cost is the codebook cost of kind k
            codebookCost += k.getRegionCost();
        }

        return codebookCost;
    }

    @Override
    public double getConditionalCost(AlignmentMatrix matrix) {
        //return super.getConditionalCost(matrix.getAlignmentCountMatrix(), matrix.getNumberOfWords());
        return getConditionalCost(matrix.getAlignmentCountMatrix(), matrix.getNumberOfWords(), matrix);
    }

    public double getConditionalCost(int[][] alignmentCountMatrix, int numberOfWords, AlignmentMatrix matrix) {
        double prior = 1;


        // IF TAKING ENDS INTO ACCOUNT, init to zero
        double negativeSumLogGammaCounts = 0;
        double positiveSumLogGammaPriors = 0;
        double positiveLogGammaSumCounts = 0;
        double negativeLogGammaSumPriors = 0;

        double negativeSumLogGammaCountsBoundary = 0;
        double positiveSumLogGammaPriorsBoundary = 0;
        double positiveLogGammaSumCountsBoundary = 0;
        double negativeLogGammaSumPriorsBoundary = 0;


        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for (int j = 0; j < alignmentCountMatrix[0].length; j++) {
                int count = alignmentCountMatrix[i][j];
                if (count <= 0 ) {
                    continue;
                }
                if (matrix.getKind(i, j).isBoundaryKind()) {
                    negativeSumLogGammaCountsBoundary -= EtyMath.base2LogGamma(count + prior);
                    positiveSumLogGammaPriorsBoundary += EtyMath.base2LogGamma(prior);
                    positiveLogGammaSumCountsBoundary += count + prior;
                    negativeLogGammaSumPriorsBoundary += prior;
                }

                else {
                    negativeSumLogGammaCounts -= EtyMath.base2LogGamma(count + prior);
                    positiveSumLogGammaPriors += EtyMath.base2LogGamma(prior);
                    positiveLogGammaSumCounts += count + prior;
                    negativeLogGammaSumPriors += prior;
                }
            }
        }

        double conditionalCost =
                  negativeSumLogGammaCounts
                + positiveSumLogGammaPriors
                + EtyMath.base2LogGamma(positiveLogGammaSumCounts)
                - EtyMath.base2LogGamma(negativeLogGammaSumPriors);


        double conditionalCostBoundary =
                  negativeSumLogGammaCountsBoundary
                + positiveSumLogGammaPriorsBoundary
                + EtyMath.base2LogGamma(positiveLogGammaSumCountsBoundary)
                - EtyMath.base2LogGamma(negativeLogGammaSumPriorsBoundary);
        

        return conditionalCost + conditionalCostBoundary;
    }



}
