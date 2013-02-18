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

package etymology.align.matrices;

import etymology.util.EtyMath;
import java.util.List;

/**
 *
 * @author sxhiltun
 */
public class SuffixAlignmentMatrix {

    private int[] matrix;
    private int languageId;
    private int symbolCount;

    private int eventCounts;
    private int alphaCounts; // number of non-zero cells


    public SuffixAlignmentMatrix(int symbolCount, int language) {
        this.languageId = language;
        this.symbolCount = symbolCount;
        this.matrix = new int[symbolCount];

        this.eventCounts = 0;
        this.alphaCounts = 0;
    }

    public void incrementSymbolCount(int symbolIndex) {

        this.matrix[symbolIndex]++;
        eventCounts++;

        if (matrix[symbolIndex] == 1) {
            alphaCounts++;
        }

    }

    public void decrementSymbolCount(int symbolIndex) {
        this.matrix[symbolIndex]--;

        eventCounts--;

        if (matrix[symbolIndex] == 0) {
            alphaCounts--;
        }
    }

    public double getPrequentialSuffixCost(List<Integer> suffix) {
        
        double cost = 0;

        for (int s : suffix) {
            cost += getSuffixCostByIndex(s);
        }
        //System.out.println("suffix: " + suffix + " " + cost);
        return cost;
    }

    public int[] getSuffixMatrix() {
        return matrix;
    }

    private double getSuffixCostByIndex(int suffixIndex) {

        int indexAlignmentCount = getAlignmentCountAtIndex(suffixIndex);

        int sumAligns = getTotalAlignmentCounts();
        int sumAlignAlphas = getTotalAlignmentAlphas();

        int countSums = sumAligns;
        int numOfNonZeroEvents = sumAlignAlphas;

        if (indexAlignmentCount > 0) {
            double cost = (1.0 + indexAlignmentCount) / (sumAligns + sumAlignAlphas);
            return -1.0 * EtyMath.base2Log(cost);
        }
               
        int kindSize = getNumberOfPossibleEvents(); //matrix.length-2; // N_k -- if no kinds, use whole area (. and - never in suffix)
        int kindNumOfNonZeroEvents = numOfNonZeroEvents; // M_k -- if no kinds use sum of all
        int sumOfNonZeroCountEvents = numOfNonZeroEvents; // |E| = sum(M_k) for all k


        double value = 1.0 / (1.0 * countSums + sumOfNonZeroCountEvents);
        value *= ((1.0 * sumOfNonZeroCountEvents) / (countSums + sumOfNonZeroCountEvents + 1));
        value *= ((kindNumOfNonZeroEvents + 1.0) / (kindSize - kindNumOfNonZeroEvents));

        return -1.0 * EtyMath.base2Log(value);

    }

    public int getAlignmentCountAtIndex(int index) {
        return matrix[index];
    }

    public int getTotalAlignmentCounts() {
        return eventCounts;
    }

    public int getTotalAlignmentAlphas() {
        return alphaCounts;
    }

    public int getNumberOfPossibleEvents() {
        return symbolCount-2; //assumes ., -, # in symbols, # only used
    }


}
