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
import etymology.config.Configuration;
import etymology.input.Input;
import java.util.List;

/**
 *
 * @author arto
 */
public class JointAlignmentMatrix extends BaseThreeLangAlignmentMatrix implements AlignmentMatrix {
    private int[][] l1l2Alignments;
    private int[][] l1l3Alignments;
    private int[][] l2l3Alignments;
    
    private int[][][] l3Alignments;

    private double[][][] l3Star;


    public JointAlignmentMatrix(Input input) {
        super(input);
        initV2(input);
    }

    private void initV2(Input input) {
        if (input.getNumOfLanguages() != 3) {
            return;
        }

        if (Configuration.getInstance().getMaxGlyphsToAlign() != 1) {
            return;
        }

        l1l2Alignments = new int[l1SymbolCount][l2SymbolCount];
        l1l3Alignments = new int[l1SymbolCount][l3SymbolCount];
        l2l3Alignments = new int[l2SymbolCount][l3SymbolCount];

        l3Alignments = new int[l1SymbolCount][l2SymbolCount][l3SymbolCount];

        l3Star = new double[l1SymbolCount + 1][l2SymbolCount + 1][l3SymbolCount + 1];
        resetCache();
    }

    @Override
    public double[] incrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if (l1GlyphIdx == null) {
            l2l3Alignments[l2GlyphIdx][l3GlyphIdx]++;
        } else if (l2GlyphIdx == null) {
            l1l3Alignments[l1GlyphIdx][l3GlyphIdx]++;
        } else if (l3GlyphIdx == null) {
            l1l2Alignments[l1GlyphIdx][l2GlyphIdx]++;
        } else {
            l3Alignments[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx]++;
            
            if(l3Alignments[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] == 1) {
                Kind k = getKind(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
                k.increaseNumOfNonZeroEvents();
                totalAlignmentAlphas++;
            }
        }

        totalAlignmentCounts++;
        return null;
    }

    @Override
    public double getAlignmentCountAtIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if(l1GlyphIdx == null || l2GlyphIdx == null || l3GlyphIdx == null) {
            return getTwoLangAlignmentCount(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
        }

        if(l3Star[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] >= 0) {
            return l3Star[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx];
        }

        double cellWeight = l3Alignments[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx];
        double count = cellWeight;
        
        if(count == 0.0) {
            l3Star[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] = 0.0;
            return 0;
        }

        // l1-l2
        double weight = 0.0; // we know that there's at least one, count > 0
        for(int i = 0; i < l3SymbolCount; i++) {
            weight += l3Alignments[l1GlyphIdx][l2GlyphIdx][i];
        }
        count += ((cellWeight * l1l2Alignments[l1GlyphIdx][l2GlyphIdx]) / weight);

        // l1-l3
        weight = 0.0; // we know that there's at least one, count > 0
        for(int i = 0; i < l2SymbolCount; i++) {
            weight += l3Alignments[l1GlyphIdx][i][l3GlyphIdx];
        }
        count += ((cellWeight * l1l3Alignments[l1GlyphIdx][l3GlyphIdx]) / weight);

        // l2-l3
        weight = 0.0; // we know that there's at least one, count > 0
        for(int i = 0; i < l1SymbolCount; i++) {
            weight += l3Alignments[i][l2GlyphIdx][l3GlyphIdx];
        }

        count += ((cellWeight * l2l3Alignments[l2GlyphIdx][l3GlyphIdx]) / weight);

        l3Star[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] = count;
        return count;
    }

    private double getTwoLangAlignmentCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        double sum = 0.0;
        if (l1GlyphIdx == null) {
            if(l3Star[l1SymbolCount][l2GlyphIdx][l3GlyphIdx] >= 0) {
                return l3Star[l1SymbolCount][l2GlyphIdx][l3GlyphIdx];
            }

            for (int i = 0; i < l1SymbolCount; i++) {
                sum += l3Alignments[i][l2GlyphIdx][l3GlyphIdx];
            }

            // hide sum's at the extra indexes

            l3Star[l1SymbolCount][l2GlyphIdx][l3GlyphIdx] = sum + l2l3Alignments[l2GlyphIdx][l3GlyphIdx];
            return l3Star[l1SymbolCount][l2GlyphIdx][l3GlyphIdx];
        } else if (l2GlyphIdx == null) {
            if(l3Star[l1GlyphIdx][l2SymbolCount][l3GlyphIdx] >= 0) {
                return l3Star[l1GlyphIdx][l2SymbolCount][l3GlyphIdx];
            }

            for (int i = 0; i < l2SymbolCount; i++) {
                sum += l3Alignments[l1GlyphIdx][i][l3GlyphIdx];
            }

            l3Star[l1GlyphIdx][l2SymbolCount][l3GlyphIdx] = sum + l1l3Alignments[l1GlyphIdx][l3GlyphIdx];
            return l3Star[l1GlyphIdx][l2SymbolCount][l3GlyphIdx];
        } else if (l3GlyphIdx == null) {
            if(l3Star[l1GlyphIdx][l2GlyphIdx][l3SymbolCount] >= 0) {
                return l3Star[l1GlyphIdx][l2GlyphIdx][l3SymbolCount];
            }

            for (int i = 0; i < l3SymbolCount; i++) {
                sum += l3Alignments[l1GlyphIdx][l2GlyphIdx][i];
            }

            l3Star[l1GlyphIdx][l2GlyphIdx][l3SymbolCount] = sum + l1l2Alignments[l1GlyphIdx][l2GlyphIdx];
            return l3Star[l1GlyphIdx][l2GlyphIdx][l3SymbolCount];
        }

        throw new UnsupportedOperationException("Invalid input, one of the glyph idx:ses need to be null.");
    }

    @Override
    public void decrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if (l1GlyphIdx == null) {
            l2l3Alignments[l2GlyphIdx][l3GlyphIdx]--;
        } else if (l2GlyphIdx == null) {
            l1l3Alignments[l1GlyphIdx][l3GlyphIdx]--;
        } else if (l3GlyphIdx == null) {
            l1l2Alignments[l1GlyphIdx][l2GlyphIdx]--;
        } else {
            l3Alignments[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx]--;

            if(l3Alignments[l1GlyphIdx][l2GlyphIdx][l3GlyphIdx] == 0) {
                Kind k = getKind(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
                k.decreaseNumOfNonZeroEvents();
                totalAlignmentAlphas--;
            }
        }

        totalAlignmentCounts--;
    }

    public void decrementAlignByDeterminedCosts(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, double[] costs) {
        decrementAlignCount(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
    }

    @Override
    public void resetCache() {
        super.resetCache();

        if(l3Star == null) {
            return;
        }

        for (int i = 0; i < l3Star.length; i++) {
            for (int j = 0; j < l3Star[0].length; j++) {
                for (int k = 0; k < l3Star[0][0].length; k++) {
                    l3Star[i][j][k] = -1;
                }
            }
        }
    }

    @Override
    public void incrementSuffixes(List<Integer> sourceSuffix, List<Integer> targetSuffix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void decrementSuffixes(List<Integer> sourceSuffix, List<Integer> targetSuffix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMostProbableGlyphAlignmentByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, int languageToImputeIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
