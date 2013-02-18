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
import etymology.align.matrices.SuffixAlignmentMatrix;
import etymology.config.Configuration;
import etymology.util.EtyMath;

/**
 *
 * @author sxhiltun
 */
public class TwoPartCodeCostNoKindsWithSuffixes extends TwoPartCodeCostFunction {
    private double[] codebook;
    private double[] conditional;

    public TwoPartCodeCostNoKindsWithSuffixes() {
        this.codebook = new double[3];
        this.conditional = new double[3];
    }


    @Override
    public String getName() {
        return "2P CODECOST - NO KINDS - UNIFORM PRIOR - SUFFIX COST TYPE: " + Configuration.getInstance().getSuffixCostType();
    }

    @Override
    public double getCodebookCost(AlignmentMatrix matrix) {
        double codebookCost = super.getCodebookCostNoKinds(matrix.getAlignmentCountMatrix());
        double suffixSourceCodebookCost = getSuffixCodebookCostNoKinds(matrix.getSuffixAlignmentMatrix(0));
        double suffixTargetCodebookCost = getSuffixCodebookCostNoKinds(matrix.getSuffixAlignmentMatrix(1));
        codebook[0] = codebookCost;
        codebook[1] = suffixSourceCodebookCost;
        codebook[2] = suffixTargetCodebookCost;

        return codebookCost + suffixSourceCodebookCost + suffixTargetCodebookCost;
    }
    

    @Override
    public double getConditionalCost(AlignmentMatrix matrix) {
        double conditionalCost = super.getConditionalCostKindsNotSeparate(matrix.getAlignmentCountMatrix(), matrix.getNumberOfWords());
        double sourceSuffixConditional = getConditionalSuffixCost(matrix.getSuffixAlignmentMatrix(0).getSuffixMatrix());
        double targetSuffixConditional = getConditionalSuffixCost(matrix.getSuffixAlignmentMatrix(1).getSuffixMatrix());
        
        conditional[0] = conditionalCost;
        conditional[1] = sourceSuffixConditional;
        conditional[2] = targetSuffixConditional;

        return conditionalCost + sourceSuffixConditional + targetSuffixConditional;

    }

    public double[] getAllCodebookCosts() {
        return codebook;
    }

    public double[] getAllConditionalCosts()  {
        return conditional;
    }

   private double getSuffixCodebookCostNoKinds(SuffixAlignmentMatrix matrix) {
        int numOfNonZeroEvents = matrix.getTotalAlignmentAlphas();
        int numOfPossibleEvents = matrix.getNumberOfPossibleEvents();

         // cost of sending the permutation that contains our events + encoding potential situation with 0 events
        double costToSendNumOfNonZeroEvents = EtyMath.base2Log(numOfPossibleEvents + 1);

        // cost to send the events
        double costToSendNonZeroEvents = EtyMath.base2LogBinomial(numOfPossibleEvents, numOfNonZeroEvents);

        // + 1 for taking separate #-# into account (log (areasize + 1) = log2 = 1)
        return costToSendNonZeroEvents + costToSendNumOfNonZeroEvents;
    }


    private double getConditionalSuffixCost(int[] suffixCountMatrix) {
        double prior = 1;

        double negativeSumLogGammaCounts = 0;
        double positiveSumLogGammaPriors = 0;
        double positiveLogGammaSumCounts = 0;
        double negativeLogGammaSumPriors = 0;



        for (int i = 0; i < suffixCountMatrix.length; i++) {
            int count = suffixCountMatrix[i];
            if (count <= 0) {
                continue;
            }

            prior = 1; // prior = 1;

            negativeSumLogGammaCounts -= EtyMath.base2LogGamma(count + prior);
            positiveSumLogGammaPriors += EtyMath.base2LogGamma(prior);
            positiveLogGammaSumCounts += (count + prior);
            negativeLogGammaSumPriors += prior;

        }

        double conditionalCost = negativeSumLogGammaCounts
                + positiveSumLogGammaPriors
                + EtyMath.base2LogGamma(positiveLogGammaSumCounts)
                - EtyMath.base2LogGamma(negativeLogGammaSumPriors);

        return conditionalCost;
    }

}
