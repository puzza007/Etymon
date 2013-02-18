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

import etymology.align.Kind;
import etymology.align.WordAlignment;
import etymology.input.Input;
import etymology.input.Tuple;
import etymology.util.EtyMath;
import java.util.Map;

/**
 *
 * @author sxhiltun
 */
public class TwoLangAlignmentMatrixWithBoundaryMatrix extends TwoLangAlignmentMatrix {



    private int boundaryAlignmentCounts;
    private int boundaryAlignmentAlphas;


    public TwoLangAlignmentMatrixWithBoundaryMatrix(Input input) {
        super(input, 0, 1);
        this.boundaryAlignmentAlphas = 0;
        this.boundaryAlignmentCounts = 0;

    }

    public TwoLangAlignmentMatrixWithBoundaryMatrix(Input input, Tuple<Integer, Integer> languageIdTuple) {
        super(input, languageIdTuple.getFirst(), languageIdTuple.getSecond());
        this.boundaryAlignmentAlphas = 0;
        this.boundaryAlignmentCounts = 0;
    }


    public TwoLangAlignmentMatrixWithBoundaryMatrix(Input input, int sourcelanguageIdx, int targetLanguageIdx) {
        super(input, sourcelanguageIdx, targetLanguageIdx);
        this.boundaryAlignmentAlphas = 0;
        this.boundaryAlignmentCounts = 0;
    }


    @Override
    public void incrementAlignCount(int sourceSymbolIndex, int targetSymbolIndex) {

        super.incrementAlignCount(sourceSymbolIndex, targetSymbolIndex);

        //check if boundary type
        boolean boundaryKind = super.getKind(sourceSymbolIndex, targetSymbolIndex).isBoundaryKind();

        if (boundaryKind) {
            boundaryAlignmentCounts++;
            if (getAlignmentCountAtIndex(sourceSymbolIndex, targetSymbolIndex) == 1) {
                boundaryAlignmentAlphas++;
            }
        }
    }


    @Override
    public void incrementAlignCount(WordAlignment wa) {
        for(int wordAlignmentPos = 0; wordAlignmentPos < wa.getAlignmentLength(); wordAlignmentPos++) {
            incrementAlignCount(wa.get(0).get(wordAlignmentPos), wa.get(1).get(wordAlignmentPos));
        }
    }

    @Override
    public double[] incrementAlignCount(Integer... glyphIdx) {

        if(glyphIdx.length == 2) {
            incrementAlignCount(glyphIdx[0], glyphIdx[1]);
        } else {
            incrementAlignCount(glyphIdx[0], glyphIdx[1], glyphIdx[2]);
        }

        return null;
    }



    @Override
    public void decrementAlignCount(int sourceSymbolIndex, int targetSymbolIndex) {

        super.decrementAlignCount(sourceSymbolIndex, targetSymbolIndex);

        boolean boundaryKind = super.getKind(sourceSymbolIndex, targetSymbolIndex).isBoundaryKind();
        if (boundaryKind) {
            boundaryAlignmentCounts--;
            if (getAlignmentCountAtIndex(sourceSymbolIndex, targetSymbolIndex) == 0) {
                boundaryAlignmentAlphas--;
            }
        }
    }


    @Override
    public double getAlignmentCostByIndex(int sourceGlyphIndex, int targetGlyphIndex) {
        double p = getAlignmentProbabilityByIndex(sourceGlyphIndex, targetGlyphIndex);
        return -1.0 * EtyMath.base2Log(p);
    }



    @Override
    public double getAlignmentProbabilityByIndex(int sourceGlyphIndex, int targetGlyphIndex) {

        int sumAligns;
        int sumAlignAlphas;

        boolean boundaryKind = super.getKind(sourceGlyphIndex, targetGlyphIndex).isBoundaryKind();

        if (boundaryKind) {
            sumAligns = boundaryAlignmentCounts;
            sumAlignAlphas = boundaryAlignmentAlphas;
        } else {
            //the boundary alignment counts are now removed from the full matrix
            sumAligns = getTotalAlignmentCounts() - boundaryAlignmentCounts;
            sumAlignAlphas = getTotalAlignmentAlphas() - boundaryAlignmentAlphas;
        }

        int indexAlignmentCount = getAlignmentCountAtIndex(sourceGlyphIndex, targetGlyphIndex);


        //formula 20: alignment exists
        int countSums = sumAligns;
        int numOfNonZeroEvents = sumAlignAlphas;

        if (indexAlignmentCount > 0) {
            return (1.0 + indexAlignmentCount) / (sumAligns + sumAlignAlphas);
        }

        //formula 21 : zero occurrence of alignment
        int kindSize;
        int kindNumOfNonZeroEvents;
        int sumOfNonZeroCountEventsOfAnyKind = numOfNonZeroEvents;

        Kind k = super.getKind(sourceGlyphIndex, targetGlyphIndex);
        kindSize = k.getRegionSize();
        kindNumOfNonZeroEvents = k.getNumOfNonZeroEvents();
        

        double value = 1.0 / (1.0 * countSums + sumOfNonZeroCountEventsOfAnyKind);
        value *= ((1.0 * sumOfNonZeroCountEventsOfAnyKind) / (countSums + sumOfNonZeroCountEventsOfAnyKind + 1));
        value *= ((kindNumOfNonZeroEvents + 1.0) / (kindSize - kindNumOfNonZeroEvents));

        return value;

    }


    @Override
    public double getAlignmentCost(Map<Integer, Integer> languageIdToGlyphIndexes) {
        return getAlignmentCostByIndex(languageIdToGlyphIndexes.get(0), languageIdToGlyphIndexes.get(1));
    }


    
}
