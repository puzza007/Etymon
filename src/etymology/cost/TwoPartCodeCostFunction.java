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
import etymology.align.matrices.TwoLangKindHolder;
import etymology.config.Configuration;
import etymology.util.EtyMath;
import java.io.FileNotFoundException;

/**
 *
 * @author avihavai
 */
public abstract class TwoPartCodeCostFunction implements CostFunction {

    private static final double DEFAULT_PRIOR = 1.0; // PRIOR IS ALWAYS 1 WHEN USING CODEBOOK

    public abstract double getCodebookCost(AlignmentMatrix matrix);
    public abstract double getConditionalCost(AlignmentMatrix matrix);
    private double defaultPrior;

    public TwoPartCodeCostFunction() {
        this(DEFAULT_PRIOR);
    }

    public TwoPartCodeCostFunction(double defaultPrior) {
        this.defaultPrior = defaultPrior;
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public double getCost(AlignmentMatrix matrix) {
        
        return getCodebookCost(matrix) + getConditionalCost(matrix);
        
    }

    public int getNumberOfPossibleEvents(int[][] matrix) {

        if (Configuration.getInstance().isRemoveSuffixes()) {
            //the boundarysymbols # and - on both sides.
            return (matrix.length-2) * (matrix[0].length-2) + 3;
        }
        // includes the #-#, but excludes .-.
        return matrix.length * matrix[0].length;
    }

    protected double getPrior(int sourceSymbolIdx, int targetSymbolIdx) {
        return defaultPrior;
    }


    public double getCodebookCostNoKinds(int[][] alignmentCountMatrix) {
        int numOfNonZeroEvents = getNumberOfNonZeroEvents(alignmentCountMatrix);
        int numOfPossibleEvents = getNumberOfPossibleEvents(alignmentCountMatrix);

         // cost of sending the permutation that contains our events + encoding potential situation with 0 events
        double costToSendNumOfNonZeroEvents = EtyMath.base2Log(numOfPossibleEvents + 1);

        // cost to send the events
        double costToSendNonZeroEvents = EtyMath.base2LogBinomial(numOfPossibleEvents, numOfNonZeroEvents);

        // + 1 for taking separate #-# into account (log (areasize + 1) = log2 = 1)
        double extraForWordBoundaries = 1;
        if (Configuration.getInstance().isRemoveSuffixes()) {
            extraForWordBoundaries = 0;
        }
        return costToSendNonZeroEvents + costToSendNumOfNonZeroEvents + extraForWordBoundaries;
    }
    
    public double getConditionalCostTwoPartCodeNoKinds(int[][] alignmentCountMatrix, int numberOfWords) {
        double prior = DEFAULT_PRIOR;


        // IF TAKING ENDS INTO ACCOUNT, init to zero
        double negativeSumLogGammaCounts = 0;
        double positiveSumLogGammaPriors = 0;
        double positiveLogGammaSumCounts = 0;
        double negativeLogGammaSumPriors = 0;


        // encode word ends at the beginning, for each word there's one alignment end event
        // take #-# event into account, there are total "num of words" such events
        if (!Configuration.getInstance().isTakeStartsAndEndsIntoAccount() 
                && !(Configuration.getInstance().isRemoveSuffixes())) {
            negativeSumLogGammaCounts = -1.0 * EtyMath.base2LogGamma(numberOfWords + prior);
            positiveSumLogGammaPriors = EtyMath.base2LogGamma(prior);
            positiveLogGammaSumCounts = (numberOfWords + prior);
            negativeLogGammaSumPriors = prior;
        }
        
        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for (int j = 0; j < alignmentCountMatrix[0].length; j++) {
                int count = alignmentCountMatrix[i][j];
                if (count <= 0) {
                    continue;
                }

                prior = getPrior(i, j); // prior = 1;

                negativeSumLogGammaCounts -= EtyMath.base2LogGamma(count + prior);
                
                //This is 0 if prior is 1
                positiveSumLogGammaPriors += EtyMath.base2LogGamma(prior);
                positiveLogGammaSumCounts += (count + prior);
                negativeLogGammaSumPriors += prior;
            }
        }

        double conditionalCost = negativeSumLogGammaCounts
                + positiveSumLogGammaPriors
                + EtyMath.base2LogGamma(positiveLogGammaSumCounts)
                - EtyMath.base2LogGamma(negativeLogGammaSumPriors);

        return conditionalCost;
    }
    
    public double getConditionalCostOfKindSeparateNML(int[][] alignmentCountMatrix, int numberOfWords, Kind k) throws FileNotFoundException {
        
        double prior = DEFAULT_PRIOR;

        double L_D_K = 0.0;

        // IF TAKING ENDS INTO ACCOUNT, init to zero
        double negativeSumCountLogCount = 0;
        int totalCountOfK = 0;
        double totalCountOfKLogTotalCountOfK = 0;
        double logC = 0;


        
        //If kind k has none zero event counts:
        if(k.getNumOfNonZeroEvents() > 0) {
        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for (int j = 0; j < alignmentCountMatrix[0].length; j++) {
                int count = alignmentCountMatrix[i][j];
                //only count the event e which is in this kind and has non zero counts
                if (!k.inRegion(i, j) || count <= 0) {
                    continue;
                }
                prior = getPrior(i, j); // prior = 1;

                negativeSumCountLogCount -= count * EtyMath.base2Log(count);
                totalCountOfK += count;
            }
        }
                totalCountOfKLogTotalCountOfK = totalCountOfK * EtyMath.base2Log(totalCountOfK);
                L_D_K = negativeSumCountLogCount  + totalCountOfKLogTotalCountOfK + EtyMath.logRegret(k.getNumOfNonZeroEvents(), totalCountOfK);
        }
        
        
        return L_D_K;
    }
    public double getConditionalCostKindsNotSeparate(int[][] alignmentCountMatrix, int numberOfWords) {
        double prior = DEFAULT_PRIOR;


        // IF TAKING ENDS INTO ACCOUNT, init to zero
        double negativeSumLogGammaCounts = 0;
        double positiveSumLogGammaPriors = 0;
        double positiveLogGammaSumCounts = 0;
        double negativeLogGammaSumPriors = 0;


        // encode word ends at the beginning, for each word there's one alignment end event
        // take #-# event into account, there are total "num of words" such events
        if (!Configuration.getInstance().isTakeStartsAndEndsIntoAccount() 
                && !(Configuration.getInstance().isRemoveSuffixes())) {
            negativeSumLogGammaCounts = -1.0 * EtyMath.base2LogGamma(numberOfWords + prior);
            positiveSumLogGammaPriors = EtyMath.base2LogGamma(prior);
            positiveLogGammaSumCounts = (numberOfWords + prior);
            negativeLogGammaSumPriors = prior;
        }
        
        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for (int j = 0; j < alignmentCountMatrix[0].length; j++) {
                int count = alignmentCountMatrix[i][j];
                if (count <= 0) {
                    continue;
                }

                prior = getPrior(i, j); // prior = 1;

                negativeSumLogGammaCounts -= EtyMath.base2LogGamma(count + prior);
                
                //This is 0 if prior is 1
                positiveSumLogGammaPriors += EtyMath.base2LogGamma(prior);
                positiveLogGammaSumCounts += (count + prior);
                negativeLogGammaSumPriors += prior;
            }
        }

        double conditionalCost = negativeSumLogGammaCounts
                + positiveSumLogGammaPriors
                + EtyMath.base2LogGamma(positiveLogGammaSumCounts)
                - EtyMath.base2LogGamma(negativeLogGammaSumPriors);

        return conditionalCost;
    }

    public double getNegativeSumLogGammaCounts(int[][] alignmentCountMatrix, int numberOfWords) {
        double prior = defaultPrior;
        
        // take #-# event into account, there are total "num of words" such events
        double negativeSumLogGammaCounts = -1.0 * EtyMath.base2LogGamma(numberOfWords + prior);

        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for (int j = 0; j < alignmentCountMatrix[0].length; j++) {
                int count = alignmentCountMatrix[i][j];
                if (count <= 0) {
                    continue;
                }

                prior = getPrior(i, j);

                negativeSumLogGammaCounts -= EtyMath.base2LogGamma(count + prior);
            }
        }

        return negativeSumLogGammaCounts;
    }
    
    /*
     * For two part code with kinds and prequential
     */
    public double getConditionalCostOfKindSeparate(int[][] alignmentCountMatrix, int numberOfWords, Kind k) {
        /*
        System.out.println("Number of words: " + numberOfWords + " " + "Kind: " + k.toString());
        
        System.out.println("#####################################");
        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for ( int j = 0; j < alignmentCountMatrix[0].length; j++) {
                System.out.print(alignmentCountMatrix[i][j] + " ");
            }
            System.out.println();
        }
                System.out.println("#####################################");
        */
                
        double prior = DEFAULT_PRIOR;

        double L_D_K = 0.0;

        // IF TAKING ENDS INTO ACCOUNT, init to zero
        double negativeSumLogGammaCounts = 0;
        double positiveSumLogGammaPriors = 0;
        double positiveLogGammaSumCounts = 0;
        double negativeLogGammaSumPriors = 0;


        // encode word ends at the beginning, for each word there's one alignment end event
        // take #-# event into account, there are total "num of words" such events
        //THIS PART MIGHT BE NOT CHANGING FOR CONDITIONAL COST WITH KINDS SEPARATE
        /*
        if (flag && (!Configuration.getInstance().isTakeStartsAndEndsIntoAccount() 
                && !(Configuration.getInstance().isRemoveSuffixes()))) {
            negativeSumLogGammaCounts = -1.0 * EtyMath.base2LogGamma(numberOfWords + prior);
            positiveSumLogGammaPriors = EtyMath.base2LogGamma(prior);
            positiveLogGammaSumCounts = (numberOfWords + prior);
            negativeLogGammaSumPriors = prior;
        }
        */
        
        
        
        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for (int j = 0; j < alignmentCountMatrix[0].length; j++) {
                int count = alignmentCountMatrix[i][j];
                //only count the event e which is in this kind and has non zero counts
                if (!k.inRegion(i, j) || count <= 0) {
                    continue;
                }

                prior = getPrior(i, j); // prior = 1;
                negativeSumLogGammaCounts -= EtyMath.base2LogGamma(count + prior);
                
                //This is 0 if prior is 1
                positiveSumLogGammaPriors += EtyMath.base2LogGamma(prior);
                positiveLogGammaSumCounts += (count + prior);
                negativeLogGammaSumPriors += prior;
            }
        }

                L_D_K = negativeSumLogGammaCounts
                + positiveSumLogGammaPriors
                + EtyMath.base2LogGamma(positiveLogGammaSumCounts)
                - EtyMath.base2LogGamma(negativeLogGammaSumPriors);
        
        
        
        double conditionalCostOfKind = L_D_K;
        //System.out.println("L_D_K: " + L_D_K);
        return conditionalCostOfKind;
    }
    
    public double getConditionalCostKindsNotSeparateNML(int[][] alignmentCountMatrix, int numberOfWords) throws FileNotFoundException {
         double prior = DEFAULT_PRIOR;

        double L_D_K = 0.0;

        // IF TAKING ENDS INTO ACCOUNT, init to zero
        double negativeSumCountLogCount = 0;
        int totalCount = 0;
        double totalCountLogTotalCount = 0;
        double logC = 0;


        // encode word ends at the beginning, for each word there's one alignment end event
        // take #-# event into account, there are total "num of words" such events
        
        if (!Configuration.getInstance().isTakeStartsAndEndsIntoAccount() 
                && !(Configuration.getInstance().isRemoveSuffixes())) {
            negativeSumCountLogCount = - numberOfWords*EtyMath.base2Log(numberOfWords);
            totalCountLogTotalCount = numberOfWords * EtyMath.base2Log(numberOfWords);
            logC = EtyMath.logRegret(1, numberOfWords); 
        }
        
        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for (int j = 0; j < alignmentCountMatrix[0].length; j++) {
                int count = alignmentCountMatrix[i][j];
                
                if (count <= 0) {
                    continue;
                }

                prior = getPrior(i, j); // prior = 1;
                
                negativeSumCountLogCount -= count * EtyMath.base2Log(count);
                
                totalCount += count;
                
            }
        }
                totalCountLogTotalCount += totalCount * EtyMath.base2Log(totalCount); 
                logC = logC + EtyMath.logRegret(getNumberOfNonZeroEvents(alignmentCountMatrix), totalCount);
                L_D_K = negativeSumCountLogCount  + totalCountLogTotalCount + logC;
        
        
        
        
        return L_D_K;
    }
    
    
    public int getNumberOfNonZeroEvents(int[][] alignmentCountMatrix) {
        int numOfNonZeroEvents = 0;
        if (!Configuration.getInstance().isRemoveSuffixes()) {
            numOfNonZeroEvents = (alignmentCountMatrix[0][0] <= 0) ? 1 : 0; // include #-# if .-. has not been included
        }


        for (int[] row : alignmentCountMatrix) {
            for (int count : row) {
                if (count <= 0) {
                    continue;
                }

                numOfNonZeroEvents++;
            }
        }

        return numOfNonZeroEvents;
    }
    
    public int countOfKind(int[][] alignmentCountMatrix, Kind k) {
        
        int count_kind = 0;
        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for (int j = 0; j < alignmentCountMatrix[0].length; j++) {
                int count = alignmentCountMatrix[i][j];
                //only count the event e which is in this kind and has non zero counts
                if (!k.inRegion(i, j) || count <= 0) {
                    continue;
                }
                count_kind += count;
            }
        }
        return count_kind;
        
    }
}
