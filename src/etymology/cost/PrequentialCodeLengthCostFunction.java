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

import etymology.util.EtyMath;

/**
 *
 * @author avihavai
 */
public class PrequentialCodeLengthCostFunction implements FeatureTreeDataCostFunction {

    // eq 12 -- prequential code length
    public double getCodeLength(int[][] alignmentCountMatrix) {
        double negativeSumLogFactorialCounts = 0;
        int numberOfEvents = alignmentCountMatrix.length * alignmentCountMatrix[0].length; //m*n matrix
        int eventCount = 0; // num of #-# events

        for (int[] matrixRow : alignmentCountMatrix) {
            for (int count: matrixRow) {
                if (count <= 0) {
                    continue;
                }

                negativeSumLogFactorialCounts -= EtyMath.base2LogFactorial(count);
                eventCount += count;
            }
        }

        double conditionalCost = 
                negativeSumLogFactorialCounts
                + EtyMath.base2LogFactorial(numberOfEvents - 1 + eventCount)
                - EtyMath.base2LogFactorial(numberOfEvents - 1);

        return conditionalCost;
    }
}
