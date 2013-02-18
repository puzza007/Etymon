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
import etymology.util.EtyMath;

/**
 *
 * @author avihavai
 */
public class BaselineCostFunction implements CostFunction {

    public String getName() {
        return "BASELINE MODEL - EQ1";
    }

    public double getCost(AlignmentMatrix matrix) {
        return getCost(matrix.getAlignmentCountMatrix(), matrix.getNumberOfWords());
    }

    public double getCost(int[][] alignmentMatrix, int numOfWords) {
        double prior = 1.0;

        double negativeSumLogGammaCounts = -1.0 * EtyMath.base2LogGamma(numOfWords + prior);
        double positiveLogGammaSumCounts = numOfWords + prior;

        double positiveSumLogGammas = EtyMath.base2LogGamma(prior);

        double negativeLogGammaSumAlphas = prior;

        for (int[] row : alignmentMatrix) {
            for (int count : row) {
                negativeSumLogGammaCounts -= EtyMath.base2LogGamma(count + prior);
                positiveLogGammaSumCounts += count + prior;
                positiveSumLogGammas += EtyMath.base2LogGamma(count + prior);
                negativeLogGammaSumAlphas += prior;
            }
        }

        return negativeSumLogGammaCounts
                + EtyMath.base2LogGamma(positiveLogGammaSumCounts)
                + positiveSumLogGammas
                - EtyMath.base2LogGamma(negativeLogGammaSumAlphas);
    }
}
