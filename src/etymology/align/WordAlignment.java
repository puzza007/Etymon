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

import etymology.config.Configuration;
import etymology.input.FeatureVocabulary;
import etymology.input.GlyphVocabulary;
import etymology.input.Input;
import etymology.util.StringUtils;
import etymology.viterbi.IViterbiMatrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author avihavai
 */
public class WordAlignment extends ArrayList<List<Integer>> implements Cloneable {

    private int numOfLanguages;
    private Set<Integer> missingLangIds;
    private List<double[]> alignmentCosts;
    private int alignmentLength;
    private List<Integer> sourceSuffix;
    private List<Integer> targetSuffix;


    public WordAlignment(IViterbiMatrix viterbiMatrix) {
        this(viterbiMatrix.reconstructAlignmentPath(), viterbiMatrix.getTotalLanguages(), viterbiMatrix.getMissingLanguageIds());


        if (Configuration.getInstance().isRemoveSuffixes()) {
            sourceSuffix = viterbiMatrix.getFinalCell().getSourceSuffix();
            targetSuffix = viterbiMatrix.getFinalCell().getTargetSuffix();

        }

    }

    public WordAlignment(List<List<Integer>> alignment) {
        this(alignment, 2, Arrays.asList(-1));

    }

    private WordAlignment(List<List<Integer>> alignmentData, int numOfLanguages, Collection<Integer> missingLangIds) {
        super(alignmentData);
        this.numOfLanguages = numOfLanguages;
        this.missingLangIds = new HashSet(missingLangIds);
        this.alignmentLength = alignmentData.get(0).size();
        this.missingLangIds.remove(-1); // viterbi matrix gives -1 in case of no missing langs
    }


    public List<Integer> getSuffix(int languageId) {
        if (languageId == 0) {
            return sourceSuffix;
        }else {
            return targetSuffix;
        }
    }
    public int getAlignmentLength() {
        return alignmentLength;
    }

    public List<double[]> getAlignmentCosts() {
        return alignmentCosts;
    }

    public void setAlignmentCosts(List<double[]> alignmentCosts) {
        this.alignmentCosts = alignmentCosts;
    }

    public boolean hasMissingLanguage() {
        return missingLangIds != null && missingLangIds.size() > 0;
    }

    public int getMissingLanguageId() {
        return missingLangIds.iterator().next();
    }

    public int getNumOfLanguages() {
        return numOfLanguages;
    }

    public String getStringPresentation(Input input) {
         return getStringPresentation(input, this, -1);
    }
    public String getStringPresentation(Input input, List<List<Integer>> alignment) {
        return getStringPresentation(input, this, -1);
    }

    public String getStringPresentation(Input input, int wordIndex) {
        return getStringPresentation(input, this, wordIndex);
    }

    public String getStringPresentation(Input input, List<List<Integer>> currentAlignment, int wordIndex) {

        List<List<Integer>> alignment = currentAlignment;
       
        StringBuilder sb = new StringBuilder();

        for (int langId = 0; langId < getNumOfLanguages(); langId++) {
            if(missingLangIds.contains(langId)) {
                sb.append("  ---\n");
                continue;
            }

            sb.append(getString(alignment.get(langId), input.getVocabulary(langId)));

            if (Configuration.getInstance().isRemoveSuffixes()) {
                List<Integer> suffixIndexes;
                if (langId == 0) {
                    suffixIndexes = sourceSuffix;
                } else {
                    suffixIndexes = targetSuffix;
                }
                
                sb.append(" - ");
                
                if (suffixIndexes != null) {
                    sb.append(getSuffixString(suffixIndexes, input.getVocabulary(langId)));
                }
            }

            if (Configuration.getInstance().isUseFeatures()) {
                addFeatureInformation(alignment, sb, langId, input);
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private void addFeatureInformation(List<List<Integer>> alignment, StringBuilder sb, int langId, Input input) {
        if (alignment.get(0).size() < 5) {
            sb.append("\t");
        }
        
        sb.append("\t");
        sb.append(getFeatureString(alignment.get(langId), (FeatureVocabulary) input.getVocabulary(langId)));
    }

    private String getString(List<Integer> glyphIndexes, GlyphVocabulary glyphVocabulary) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for(int glyphIndex: glyphIndexes) {
            String glyph = glyphVocabulary.getGlyph(glyphIndex);
            sb.append(glyph);

            if(StringUtils.getGlyphComboLength(glyph) <= 1) {
                sb.append("  ");
            } else {
                sb.append(" ");
            }
        }
        if (Configuration.getInstance().areWordsFlippedAround()) {
            return StringUtils.reverseString(sb.toString()).trim();
        }

        return sb.toString();
    }

        public static String getStringByGlyphIndex(int glyphIndex, GlyphVocabulary glyphVocabulary) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        
        String glyph = glyphVocabulary.getGlyph(glyphIndex);
        sb.append(glyph);

        sb.append(" ");
           
        return sb.toString();
    }

    private String getSuffixString(List<Integer> suffixIndexes, GlyphVocabulary glyphVocabulary) {
        StringBuilder sb = new StringBuilder();
        sb.append("\t");
        for(int glyphIndex: suffixIndexes) {
            String glyph = glyphVocabulary.getGlyph(glyphIndex);
            sb.append(glyph);
        }

        return sb.toString();

    }

    private String getFeatureString(List<Integer> glyphIndexes, FeatureVocabulary glyphVocabulary) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        for(int featureIndex: glyphIndexes) {
            String glyph = glyphVocabulary.getFeature(featureIndex);
            if (glyph.length() == 5 ) {
                sb.append(" ");
            }else if (glyph.length() == 1 ) {
                sb.append("   ");
            }
            sb.append(glyph);
            if (glyph.length() == 1 ) {
                sb.append("  ");
            }
            sb.append(" ");

        }
        return sb.toString();
    }


}
