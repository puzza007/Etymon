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
import etymology.align.PriorHolder;
import etymology.input.Input;
import etymology.util.EtyMath;

/**
 *
 * TODO: IF THIS IS GENERALIZED TO MULTIPLE LANGUAGES, HAVE ANOTHER WAY FOR RETRIEVING SOURCE AND TARGET SIZE
 *
 *
 * @author avihavai
 */
public class MultiGlyphCostFunction implements CostFunction {

    private int sourceSize;
    private int targetSize;

    public MultiGlyphCostFunction(AlignmentMatrix matrix, Input input) {
        this.sourceSize = input.getLengthOneGlyphCount(0);
        if (matrix.getAlignmentCountMatrix().length < this.sourceSize + 2) {
            throw new RuntimeException("Invalid data for " + getName());
        }

        this.targetSize = input.getLengthOneGlyphCount(1);
        if (matrix.getAlignmentCountMatrix().length < this.targetSize + 2) {
            throw new RuntimeException("Invalid data for " + getName());
        }
    }

    public double getCost(AlignmentMatrix matrix) {
        double cost = 0.0;
        cost -= getNegativeSumLogGammaCounts(matrix.getAlignmentCountMatrix(), matrix.getNumberOfWords(), matrix);
        cost += getPositiveGammaSumCountsWithPriors(matrix.getAlignmentCountMatrix(), matrix.getNumberOfWords());

        cost -= EtyMath.base2LogGamma((4 * sourceSize * targetSize) + 1);

        cost += ((sourceSize + 1) * (targetSize + 1) + 1) * EtyMath.base2LogGamma(1); // will be 0 -- optimizer handles
        cost += (sourceSize * sourceSize * (targetSize + 1)) * EtyMath.base2LogGamma(1.0 / sourceSize);
        cost += ((sourceSize + 1) * targetSize * targetSize) * EtyMath.base2LogGamma(1.0 / targetSize);
        cost += (sourceSize * sourceSize * targetSize * targetSize) * EtyMath.base2LogGamma(1.0 / (sourceSize * targetSize));
        return cost;
    }

    public final String getName() {
        return "WRITTEN OUT COST (EQ 22)";
    }

    public int getPriorSum() {
        return ((sourceSize + 1) * (targetSize + 1))
                + (sourceSize * (targetSize + 1))
                + ((sourceSize + 1) * targetSize)
                + (sourceSize * targetSize);
    }

    public double getPrior(int sourceIndex, int targetIndex) {
        if (sourceIndex <= sourceSize + 1 && targetIndex <= targetSize + 1) {
            return 1;
        }

        if (sourceIndex <= sourceSize + 1 && targetIndex > targetSize + 1) {
            return 1.0 / targetSize;
        }

        if (sourceIndex > sourceSize + 1 && targetIndex <= targetSize + 1) {
            return 1.0 / sourceSize;
        }

        // sourceIndex > sourceSize + 1 && targetIndex > targetSize + 1
        return 1.0 / (sourceSize * targetSize);
    }

    private double getPositiveGammaSumCountsWithPriors(int[][] alignmentCountMatrix, int numOfWords) {
        // #-# alignments at the beginning
        double alignmentCounts = numOfWords;

        for (int[] row : alignmentCountMatrix) {
            for (int count : row) {
                if (count <= 0) {
                    continue;
                }

                alignmentCounts += count;
            }
        }

        alignmentCounts += (4 * sourceSize * targetSize) + 1;
        return EtyMath.base2LogGamma(alignmentCounts);
    }

    private double getNegativeSumLogGammaCounts(int[][] alignmentCountMatrix, int numOfWords, PriorHolder priorHolder) {
        double negSumLogGammaCounts = EtyMath.base2LogGamma(numOfWords + 1);

        for (int i = 0; i < alignmentCountMatrix.length; i++) {
            for (int j = 0; j < alignmentCountMatrix[0].length; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }


                int count = alignmentCountMatrix[i][j];
                double prior = priorHolder.getPrior(i, j);
                negSumLogGammaCounts += EtyMath.base2LogGamma(count + prior);
            }
        }

        return negSumLogGammaCounts;
    }
}
