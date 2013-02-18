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
package etymology.align;

import etymology.align.matrices.SuffixAlignmentMatrix;
import etymology.cost.AlignmentCostFunction;
import java.util.List;

/**
 *
 * @author arto
 */
public interface AlignmentMatrix extends ITwoLangAlignmentMatrix, PriorHolder, AlignmentCostFunction {
    public int getNumberOfWords();

    public int getL1LangId();
    public int getL2LangId();
    public int getL3LangId();


    public void incrementAlignCount(WordAlignment wa);

    public double[] incrementAlignCount(Integer... glyphIdx);

    public void incrementSuffixes(List<Integer> sourceSuffix, List<Integer> targetSuffix);
    public void decrementSuffixes(List<Integer> sourceSuffix, List<Integer> targetSuffix);
    public SuffixAlignmentMatrix getSuffixAlignmentMatrix(int languageId);
    public double getSuffixCost(List<Integer> suffix, int language);

    public double getAlignmentCountAtIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx);

    public void decrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx);

    public double getAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx);

    public double getDotToDotAllowedAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx);
    
    public int getMostProbableGlyphAlignmentByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx);

    public abstract int getMostProbableGlyphAlignmentByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, int languageToImputeIndex);

    public Kind getKind(int l1SymbolIdx, int l2SymbolIdx, int l3SymbolIdx);

    public void decrementAlignByDeterminedCosts(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, double[] costs);

    public int getL3SymbolCount();
    
    public int getNumberOfNonZeroAlignments();

    public void resetCache();
}
