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

import etymology.align.AlignmentMatrix;
import etymology.align.Kind;
import etymology.config.Configuration;
import etymology.input.Input;

/**
 *
 * @author avihavai
 */
public abstract class BaseThreeLangAlignmentMatrix extends TwoLangAlignmentMatrix implements AlignmentMatrix {

    protected double[][][] l3AlignmentCountMatrix;
    protected double[][][] probabilityCache; // cache for probabilities, reset after use

    protected int l3SymbolCount;
    protected int l3LanguadeIdx;

    public int getL3SymbolCount() {
        return l3SymbolCount;
    }
    
    public BaseThreeLangAlignmentMatrix(Input input) {
        super(input);
        init(input);
    }

    protected final void init(Input input) {
        l1SymbolCount = 1 + input.getLengthOneGlyphCount(0);
        l2SymbolCount = 1 + input.getLengthOneGlyphCount(1);
        if (Configuration.getInstance().getMaxGlyphsToAlign() > 1) {
            l1SymbolCount += (input.getLengthOneGlyphCount(0) * input.getLengthOneGlyphCount(0));
            l2SymbolCount += (input.getLengthOneGlyphCount(1) * input.getLengthOneGlyphCount(1));
        }

        alignmentCountMatrix = new int[l1SymbolCount][l2SymbolCount];

        if (input.getNumOfLanguages() == 3 && Configuration.getInstance().getMaxGlyphsToAlign() == 1) {
            l3SymbolCount = 1 + input.getLengthOneGlyphCount(2);
            l3AlignmentCountMatrix = new double[l1SymbolCount][l2SymbolCount][l3SymbolCount];
        }

        l3LanguadeIdx = 2;

        totalAlignmentCounts = input.getNumOfWords();
        totalAlignmentAlphas = 1;

        if(Configuration.getInstance().getMaxGlyphsToAlign() > 1) {
            kindHolder = new ThreeLangKindHolder(input, l1SymbolCount, l2SymbolCount, l3SymbolCount);
        }
        initCaches();
    }

    @Override
    public int getL3LangId() {
        return l3LanguadeIdx;
    }

    private void initCaches() {
        probabilityCache = new double[l1SymbolCount][l2SymbolCount][l3SymbolCount];
        resetProbabilityCache();
    }

    public double[][][] getL3AlignmentCountMatrix() {
        return l3AlignmentCountMatrix;
    }

    @Override
    public double getAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if (l1GlyphIdx == null || l2GlyphIdx == null || l3GlyphIdx == null) {
            return getSummedAlignmentProbabilityByIndex(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
        }

        if(probabilityCache[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] >= 0) {
            return probabilityCache[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx];
        }

        double alignmentCount = getAlignmentCountAtIndex(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);

        int sumAligns = getTotalAlignmentCounts();
        int sumAlignAlphas = getTotalAlignmentAlphas();

        if (alignmentCount > 0) {
            probabilityCache[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] = (1.0 + alignmentCount) / (sumAligns + sumAlignAlphas);
            return (1.0 + alignmentCount) / (sumAligns + sumAlignAlphas);
        }

        // count = 0, special case --
        Kind k = getKind(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
        int numOfPossibleEvents = k.getRegionSize();
        int numOfNonZeroEvents = k.getNumOfNonZeroEvents(); // .getNumOfNonZeroEntries();

        double head = (1.0 + numOfNonZeroEvents) * sumAligns;
        double sumAlignCountsAndAlphas = sumAligns + sumAlignAlphas;
        double divider = sumAlignCountsAndAlphas
                * (sumAlignCountsAndAlphas + 1.0)
                * (numOfPossibleEvents - numOfNonZeroEvents);

        probabilityCache[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] = head / divider;
        return head / divider;
    }

    private double getSummedAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        double sum = 0.0;
        if(l1GlyphIdx == null) {
            for(int i = 0; i < l1SymbolCount; i++) {
                sum += getAlignmentProbabilityByIndex(i, l2GlyphIdx, l3GlyphIdx);
            }
            return sum;
        } else if (l2GlyphIdx == null) {
            for(int i = 0; i < l2SymbolCount; i++) {
                sum += getAlignmentProbabilityByIndex(l1GlyphIdx, i, l3GlyphIdx);
            }
            return sum;
        } else if (l3GlyphIdx == null) {
            for(int i = 0; i < l3SymbolCount; i++) {
                sum += getAlignmentProbabilityByIndex(l1GlyphIdx, l2GlyphIdx, i);
            }
            return sum;
        }

        return sum;
    }

    public double[] incrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if(l1GlyphIdx == null || l2GlyphIdx == null || l3GlyphIdx == null) {
            System.err.println("Incrementing align count with missing glyph value.");
            System.err.println("Need to override method to add functionality handling this case.");
            System.exit(0);
        }

        l3AlignmentCountMatrix[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx]++;
        totalAlignmentCounts++;

        if (l3AlignmentCountMatrix[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] == 1) {
            Kind k = getKind(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
            totalAlignmentAlphas++;
            k.increaseNumOfNonZeroEvents();
        }

        return null;
    }

    @Override
    public void decrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if (l1GlyphIdx == null || l2GlyphIdx == null || l3GlyphIdx == null) {
            System.err.println("Decrementing align count with missing glyph value.");
            System.err.println("Should utilize existing counts for decrement.");
            System.exit(0);
            return;
        }

        l3AlignmentCountMatrix[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx]--;
        totalAlignmentCounts--;

        if (l3AlignmentCountMatrix[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] <= 0) {
            Kind k = getKind(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
            totalAlignmentAlphas--;
            k.decreaseNumOfNonZeroEvents();
        }
    }

    // basic versions have no caching
    public void resetCache() {
        resetProbabilityCache();
    }

    private void resetProbabilityCache() {
        for (int i = 0; i < probabilityCache.length; i++) {
            for (int j = 0; j < probabilityCache[0].length; j++) {
                for (int k = 0; k < probabilityCache[0][0].length; k++) {
                    probabilityCache[i][j][k] = -1;
                }
            }
        }
    }
}
