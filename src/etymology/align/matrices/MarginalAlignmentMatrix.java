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

import etymology.align.*;
import etymology.input.Input;
import etymology.util.EtyMath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author arto
 */
public class MarginalAlignmentMatrix extends BaseThreeLangAlignmentMatrix implements AlignmentMatrix {
    private final Collection<TwoLangAlignmentMatrix> matrices = new ArrayList();
    
    private final TwoLangAlignmentMatrix l1l2Alignments;
    private final TwoLangAlignmentMatrix l1l3Alignments;
    private final TwoLangAlignmentMatrix l2l3Alignments;

    private List<Kind> allKinds;

    public MarginalAlignmentMatrix(Input input) {
        super(input);

        if (input.getNumOfLanguages() != 3) {
            throw new RuntimeException(MarginalAlignmentMatrix.class.getName() + " can be run only using three languages.");
        }

        l1l2Alignments = new TwoLangAlignmentMatrix(input, 0, 1);
        l1l3Alignments = new TwoLangAlignmentMatrix(input, 0, 2);
        l2l3Alignments = new TwoLangAlignmentMatrix(input, 1, 2);
        matrices.add(l1l2Alignments);
        matrices.add(l1l3Alignments);
        matrices.add(l2l3Alignments);
    }

    public Collection<TwoLangAlignmentMatrix> getMatrices() {
        return matrices;
    }

    @Override
    public void incrementAlignCount(WordAlignment wa) {
        for(int wordAlignmentPos = 0; wordAlignmentPos < wa.getAlignmentLength(); wordAlignmentPos++) {
            incrementAlignCount(wa.get(0).get(wordAlignmentPos), wa.get(1).get(wordAlignmentPos), wa.get(2).get(wordAlignmentPos));
        }
    }

    @Override
    public double[] incrementAlignCount(Integer... glyphIdx) {
        return incrementAlignCount(glyphIdx[0], glyphIdx[1], glyphIdx[2]);
    }

    @Override
    public double[] incrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if (l1GlyphIdx != null && l2GlyphIdx != null) {
            l1l2Alignments.incrementAlignCount(l1GlyphIdx, l2GlyphIdx);
        }

        if (l1GlyphIdx != null && l3GlyphIdx != null) {
            l1l3Alignments.incrementAlignCount(l1GlyphIdx, l3GlyphIdx);
        }

        if (l2GlyphIdx != null && l3GlyphIdx != null) {
            l2l3Alignments.incrementAlignCount(l2GlyphIdx, l3GlyphIdx);
        }

        return null;
    }

    @Override
    public double getAlignmentCountAtIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if (l1GlyphIdx == null) {
            return l2l3Alignments.getAlignmentCountAtIndex(l2GlyphIdx, l3GlyphIdx);
        }

        if (l2GlyphIdx == null) {
            return l1l3Alignments.getAlignmentCountAtIndex(l1GlyphIdx, l3GlyphIdx);
        }

        if (l3GlyphIdx == null) {
            return l1l2Alignments.getAlignmentCountAtIndex(l1GlyphIdx, l2GlyphIdx);
        }

        // sum otherwise
        return l1l2Alignments.getAlignmentCountAtIndex(l1GlyphIdx, l2GlyphIdx)
                + l1l3Alignments.getAlignmentCountAtIndex(l1GlyphIdx, l3GlyphIdx)
                + l2l3Alignments.getAlignmentCountAtIndex(l2GlyphIdx, l3GlyphIdx);
    }

    @Override
    public int getMostProbableGlyphAlignmentByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, int imputingLangIndex) {
        int highestIdx = -1;
        int highestCount = -1;

        if (imputingLangIndex == 0) {
            for (int l1Idx = 0; l1Idx < getL1SymbolCount(); l1Idx++) {
                int countAtIdx = (int) getAlignmentCountAtIndex(l1Idx, l2GlyphIdx, l3GlyphIdx);
                if (countAtIdx > highestCount) {
                    highestIdx = l1Idx;
                    highestCount = countAtIdx;
                }
            }

            return highestIdx;
        }


        if (imputingLangIndex == 1) {
            for (int l2Idx = 0; l2Idx < getL2SymbolCount(); l2Idx++) {
                int countAtIdx = (int) getAlignmentCountAtIndex(l1GlyphIdx, l2Idx, l3GlyphIdx);
                if (countAtIdx > highestCount) {
                    highestIdx = l2Idx;
                    highestCount = countAtIdx;
                }
            }

            return highestIdx;

        }


        if (imputingLangIndex == 2) {
            for (int l3Idx = 0; l3Idx < getL3SymbolCount(); l3Idx++) {
                int countAtIdx = (int) getAlignmentCountAtIndex(l1GlyphIdx, l2GlyphIdx, l3Idx);
                if (countAtIdx > highestCount) {
                    highestIdx = l3Idx;
                    highestCount = countAtIdx;
                }
            }           
        }
        return highestIdx;

    }



    public double getAlignmentCostByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        return -1.0 * EtyMath.base2Log(getAlignmentProbabilityByIndex(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx));
    }

    @Override
    public double getAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        return getDotToDotAllowedAlignmentProbabilityByIndex(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
    }

    @Override
    public double getDotToDotAllowedAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if (l1GlyphIdx == null) {
            return l2l3Alignments.getAlignmentProbabilityByIndex(l2GlyphIdx, l3GlyphIdx);
        }

        if (l2GlyphIdx == null) {
            return l1l3Alignments.getAlignmentProbabilityByIndex(l1GlyphIdx, l3GlyphIdx);
        }

        if (l3GlyphIdx == null) {
            return l1l2Alignments.getAlignmentProbabilityByIndex(l1GlyphIdx, l2GlyphIdx);
        }

        double l1l2Prob, l1l3Prob, l2l3Prob;
        l1l2Prob = l1l2Alignments.getAlignmentProbabilityByIndex(l1GlyphIdx, l2GlyphIdx);
        l1l3Prob = l1l3Alignments.getAlignmentProbabilityByIndex(l1GlyphIdx, l3GlyphIdx);
        l2l3Prob = l2l3Alignments.getAlignmentProbabilityByIndex(l2GlyphIdx, l3GlyphIdx);

        return l1l2Prob * l1l3Prob * l2l3Prob;
    }

    @Override
    public void decrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if (l1GlyphIdx != null && l2GlyphIdx != null) {
            l1l2Alignments.decrementAlignCount(l1GlyphIdx, l2GlyphIdx);
        }

        if (l1GlyphIdx != null && l3GlyphIdx != null) {
            l1l3Alignments.decrementAlignCount(l1GlyphIdx, l3GlyphIdx);
        }

        if (l2GlyphIdx != null && l3GlyphIdx != null) {
            l2l3Alignments.decrementAlignCount(l2GlyphIdx, l3GlyphIdx);
        }
    }



    public void decrementAlignByDeterminedCosts(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, double[] costs) {
        decrementAlignCount(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
    }

    @Override
    public void resetCache() {
        super.resetCache();
    }

    @Override
    public Collection<Kind> getAllKinds() {
        if(allKinds != null) {
            return allKinds;
        }

        allKinds = new ArrayList();
        allKinds.addAll(l1l2Alignments.getAllKinds());
        allKinds.addAll(l1l3Alignments.getAllKinds());
        allKinds.addAll(l2l3Alignments.getAllKinds());
        return allKinds;
    }

    
}
