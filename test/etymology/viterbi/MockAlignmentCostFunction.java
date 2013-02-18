/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package etymology.viterbi;

import etymology.cost.AlignmentCostFunction;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 * @author arto
 */
public class MockAlignmentCostFunction implements AlignmentCostFunction {

    private boolean isUseFeatures;

    public MockAlignmentCostFunction(boolean isUseFeatures) {
        this.isUseFeatures = isUseFeatures;
    }

    public double getRandomFeatureAlignmentCost() {
        return 1;
    }

    public boolean isUseFeatures() {
        return isUseFeatures;
    }

    public double getFeatureAlignmentCostByGlyphIndexes(Map<Integer, List<Integer>> alignmentPathUntilNow, Map<Integer, Integer> languageIdToGlyphIndexes) {
        return 1;
    }

    public double getAlignmentCost(Map<Integer, Integer> languageIdToGlyphIndexes) {
        return 1;
    }
}
