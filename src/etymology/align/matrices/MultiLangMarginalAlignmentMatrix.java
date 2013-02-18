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
import etymology.cost.AlignmentCostFunction;
import etymology.input.Input;
import etymology.input.Tuple;
import etymology.util.EtyMath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author arto
 */
public class MultiLangMarginalAlignmentMatrix extends BaseThreeLangAlignmentMatrix implements AlignmentMatrix, AlignmentCostFunction {
    private final Map<Tuple, TwoLangAlignmentMatrix> languageTupleToMatrix = new HashMap();
    private final Map<Integer, Map<Integer, Tuple>> languageIdsToTupleMap = new HashMap();

    private List<Kind> allKinds;

    public MultiLangMarginalAlignmentMatrix(Input input) {
        super(input);

        List<Tuple> languagePairs = Tuple.getAsPairs(input.getLanguageIds());
        for(Tuple<Integer, Integer> languagePair: languagePairs) {
            putPairToLanguageIdsToTupleMap(languagePair);
            languageTupleToMatrix.put(languagePair, new TwoLangAlignmentMatrix(input, languagePair));
        }
    }

    private void putPairToLanguageIdsToTupleMap(Tuple<Integer, Integer> languagePair) {
        int firstLang = languagePair.getFirst();
        int secondLang = languagePair.getSecond();

        if (!languageIdsToTupleMap.containsKey(firstLang)) {
            languageIdsToTupleMap.put(firstLang, new HashMap());
        }

        if (!languageIdsToTupleMap.containsKey(secondLang)) {
            languageIdsToTupleMap.put(secondLang, new HashMap());
        }
        
        languageIdsToTupleMap.get(firstLang).put(secondLang, languagePair);
        languageIdsToTupleMap.get(secondLang).put(firstLang, languagePair);
    }

    public Collection<TwoLangAlignmentMatrix> getMatrices() {
        return languageTupleToMatrix.values();
    }

    @Override
    public double[] incrementAlignCount(Integer... glyphIdArray) {
        for(Tuple<Integer, Integer> t: languageTupleToMatrix.keySet()) {
            if (glyphIdArray[t.getFirst()] == null) {
                continue;
            }

            if (glyphIdArray[t.getSecond()] == null) {
                continue;
            }

            TwoLangAlignmentMatrix am = languageTupleToMatrix.get(t);
            am.incrementAlignCount(glyphIdArray[t.getFirst()], glyphIdArray[t.getSecond()]);
        }

        return null;
    }

    public double getAlignmentCountAtIndex(Integer... glyphIdArray) {
        double count = 0;

        for (Tuple<Integer, Integer> t : languageTupleToMatrix.keySet()) {
            if (glyphIdArray[t.getFirst()] == null) {
                continue;
            }

            if (glyphIdArray[t.getSecond()] == null) {
                continue;
            }

            TwoLangAlignmentMatrix am = languageTupleToMatrix.get(t);
            count += am.getAlignmentCountAtIndex(glyphIdArray[t.getFirst()], glyphIdArray[t.getSecond()]);
        }

        return count;
    }

    public double getAlignmentCostByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        return -1.0 * EtyMath.base2Log(getAlignmentProbabilityByIndex(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx));
    }

    @Override
    public double getAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        return getDotToDotAllowedAlignmentProbabilityByIndex(l1GlyphIdx, l2GlyphIdx, l3GlyphIdx);
    }

    public double getDotToDotAllowedAlignmentProbabilityByIndex(Integer... glyphIdArray) {
        double probability = 1;

        for (Tuple<Integer, Integer> t : languageTupleToMatrix.keySet()) {
            if (glyphIdArray[t.getFirst()] == null) {
                continue;
            }

            if (glyphIdArray[t.getSecond()] == null) {
                continue;
            }

            TwoLangAlignmentMatrix am = languageTupleToMatrix.get(t);
            probability *= am.getAlignmentProbabilityByIndex(glyphIdArray[t.getFirst()], glyphIdArray[t.getSecond()]);
        }

        return probability;
    }

    public void decrementAlignCount(Integer... glyphIdArray) {
        for (Tuple<Integer, Integer> t : languageTupleToMatrix.keySet()) {
            if (glyphIdArray[t.getFirst()] == null) {
                continue;
            }

            if (glyphIdArray[t.getSecond()] == null) {
                continue;
            }

            TwoLangAlignmentMatrix am = languageTupleToMatrix.get(t);
            am.decrementAlignCount(glyphIdArray[t.getFirst()], glyphIdArray[t.getSecond()]);
        }
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
        for(TwoLangAlignmentMatrix tlam: languageTupleToMatrix.values()) {
            allKinds.addAll(tlam.getAllKinds());
        }
        return allKinds;
    }

    public double getAlignmentCostByGlyphIndexes(Map<Integer, Integer> languageIdToGlyphIndexes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getRandomFeatureAlignmentCost() {
        return new Random().nextDouble();
    }

    public double getFeatureAlignmentCostByGlyphIndexes(Map<Integer, List<Integer>> languageIdToAlignmentPathUntilNow, Map<Integer, Integer> languageIdToGlyphIndexes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isUseFeatures() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMostProbableGlyphAlignmentByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, int languageToImputeIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
