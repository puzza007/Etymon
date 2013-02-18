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

import etymology.input.Input;
import etymology.util.EtyMath;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author sxhiltun
 */
public class SuffixCostCalculator {
       
    private Input input;

    private static Map<Integer, Double> uniformGlyphCosts;
    private static Map<Integer, Map<Integer, Double>> unigramGlyphCosts;

    public SuffixCostCalculator(Input input) {

        this.input = input;
        init();

    }

    private void init() {
        setUniformGlyphCosts();
        setUnigramGlyphCosts();
        //setBigramGlyphCosts();
        //setNgramGlyphCosts();
    }

    public static double getUniformSuffixCost(int language, List<Integer> word) {

        double cost = word.size() * getUniformGlyphCost(language);
        
        return cost;
    }

    public static double getUnigramSuffixCost(int language, List<Integer> word) {
        double cost = 0;
        for (int i: word) {
            cost += getUnigramGlyphCost(language, i);
        }
        return cost;
    }
    

    private void setUniformGlyphCosts() {

        uniformGlyphCosts = new TreeMap<Integer, Double>();
        for (int langId : input.getLanguageIds()) {
            double cost = -EtyMath.base2Log(1.0/ input.getLengthOneGlyphCount(langId));
            uniformGlyphCosts.put(langId, cost);
        }
    }

    private static double getUniformGlyphCost(int languageId) {
        return uniformGlyphCosts.get(languageId).doubleValue();
    }

    private void setUnigramGlyphCosts() {

        unigramGlyphCosts = new TreeMap<Integer, Map<Integer, Double>>();

        for (int languageIdx : input.getLanguageIds()) {
            unigramGlyphCosts.put(languageIdx, new TreeMap<Integer, Double>());
            for (int glyphId = 0; glyphId<input.getLengthOneGlyphCount(languageIdx); glyphId++) {
                double prob = computeGlyphFrequency(languageIdx, glyphId);
                double cost = -EtyMath.base2Log(prob);
                unigramGlyphCosts.get(languageIdx).put(glyphId, cost);
            }

        }

    }

    private static double getUnigramGlyphCost(int languageId, int glyphId) {
        return unigramGlyphCosts.get(languageId).get(glyphId);
    }

    private double computeGlyphFrequency(int language, int glyphId) {
        
        int totalGlyphLength = 0;
        int frequency = 0;
        List<Integer> wordIndexes;
        for (int wordIndex=0; wordIndex<input.getNumOfWords(); wordIndex++) {
            wordIndexes = input.getWordIndexes(language, wordIndex);
            for (int i : wordIndexes) {
                totalGlyphLength++;
                if (i == glyphId) {
                    frequency++;
                }
            }
        }


        return 1.0 * frequency / totalGlyphLength;

    }

}
