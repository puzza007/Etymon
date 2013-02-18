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

package etymology.context;

import etymology.config.Constants;
import etymology.input.FeatureVocabulary;
import etymology.input.Input;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 * @author sxhiltun
 */
public interface FeatureTreeContainer {

    public enum Context {
        ITSELF,
        PREVIOUS_CONSONANT, PREVIOUS_VOWEL, PREVIOUS_POSITION, PREVIOUS_SYMBOL,
        //PREVIOUS_VOWEL_GROUP,
        CLOSEST_CONSONANT, CLOSEST_VOWEL, CLOSEST_SYMBOL,
        NEXT_CONSONANT, NEXT_VOWEL, NEXT_SYMBOL;

        private int[] symbolIndex = new int[2];
        

        public void setContextIndex(Level level, int symbolIndex) {
            this.symbolIndex[level.getLevelIdx()] = symbolIndex;
        }

        public int getIndexOfGlyph(Level level) {
            return symbolIndex[level.getLevelIdx()];
        }



        public static void setAllHistoryContexts(int indexOfGlyph, Level level,
                List<Integer> glyphs, FeatureVocabulary fv) throws Exception {

            int glyphInContext;
                                     
            glyphInContext = glyphs.get(indexOfGlyph);
            ITSELF.setContextIndex(level, glyphInContext);
            
            //the closest consonant up until the one before current
            glyphInContext = getPreviousGlyphOfGivenType(indexOfGlyph-1, glyphs, fv, "consonant");
            PREVIOUS_CONSONANT.setContextIndex(level, glyphInContext);
            
            //similar to PREVIOUS_CONSONANT but include current position
            glyphInContext = getPreviousGlyphOfGivenType(indexOfGlyph, glyphs, fv, "consonant");
            CLOSEST_CONSONANT.setContextIndex(level, glyphInContext);

            glyphInContext = getPreviousGlyphOfGivenType(indexOfGlyph-1, glyphs, fv, "vowel");
            PREVIOUS_VOWEL.setContextIndex(level, glyphInContext);

            glyphInContext = getPreviousGlyphOfGivenType(indexOfGlyph, glyphs, fv, "vowel");
            CLOSEST_VOWEL.setContextIndex(level, glyphInContext);

            glyphInContext = getPreviousGlyphOfGivenType(indexOfGlyph-1, glyphs, fv, "notDot");
            PREVIOUS_SYMBOL.setContextIndex(level, glyphInContext);

            glyphInContext = getPreviousGlyphOfGivenType(indexOfGlyph, glyphs, fv, "notDot");
            CLOSEST_SYMBOL.setContextIndex(level, glyphInContext);


            if (indexOfGlyph > 0) {
                glyphInContext = glyphs.get(indexOfGlyph-1);
            } else {
                //if the first symbol of the word, the prev. symbol was word boundary
                glyphInContext = fv.getGlyphIndex("#");
            }
            PREVIOUS_POSITION.setContextIndex(level, glyphInContext);

            //previous vowel group needs to be verified
            //glyphInContext = getPreviousVowelGroupGlyph(indexOfGlyph, glyphs, fv);
            //PREVIOUS_VOWEL_GROUP.setContextIndex(level, glyphInContext);
        }

        public static void setAllFutureContext(int glyphIndex, Level level,
                List<Integer> glyphs, FeatureVocabulary fv) throws Exception {

            int glyphInContext;

            glyphInContext = getNextGlyphOfGivenType(glyphIndex+1, glyphs, fv, "vowel");
            NEXT_VOWEL.setContextIndex(level, glyphInContext);

            glyphInContext = getNextGlyphOfGivenType(glyphIndex+1, glyphs, fv, "consonant");
            NEXT_CONSONANT.setContextIndex(level, glyphInContext);

            glyphInContext = getNextGlyphOfGivenType(glyphIndex+1, glyphs, fv, "notDot");
            NEXT_SYMBOL.setContextIndex(level, glyphInContext);

        }




        private static int getPreviousVowelGroupGlyph(int startSearchFromIndex, List<Integer> glyphs, FeatureVocabulary fv) {

            //if the current glyph is not vowel, the previous vowel group == previous vowel
            if (!fv.isVowel(glyphs.get(startSearchFromIndex))) {
                return getPreviousGlyphOfGivenType(startSearchFromIndex, glyphs, fv, "vowel");
            }

            //if the current glyph is vowel, previous vowel group is behind a consonant or a dot
            int glyphInd = startSearchFromIndex-1;
            while(glyphInd >= 0 && fv.isVowel(glyphs.get(glyphInd))) {
                glyphInd--;
            }

            return getPreviousGlyphOfGivenType(glyphInd, glyphs, fv, "vowel");
        }

        private static int getPreviousGlyphOfGivenType(int startSearchFromIndex, List<Integer> glyphs, FeatureVocabulary fv, String type) {
            int wordBoundaryIndex = fv.getGlyphIndex("#");
            if (startSearchFromIndex < 0) {
                return wordBoundaryIndex;
            }

            for (int i=startSearchFromIndex; i>=0; i--) {
                if (type.equals("vowel") && fv.isVowel(glyphs.get(i))) {
                    return glyphs.get(i);
                } else if (type.equals("consonant") && fv.isConsonant(glyphs.get(i))) {
                    return glyphs.get(i);
                } else if (type.equals("notDot") && (glyphs.get(i) != Constants.DOT_INDEX)) {
                    return glyphs.get(i);
                }
            }

            //no match in the beginning of word
            return wordBoundaryIndex;
        }

        private static int getNextGlyphOfGivenType(int startSearchFromIndex, List<Integer> glyphs, FeatureVocabulary fv, String type) {
            int wordBoundaryIndex = fv.getGlyphIndex("#");
            if (startSearchFromIndex == glyphs.size()) {
                return wordBoundaryIndex;
            }
            for (int i=startSearchFromIndex; i<glyphs.size(); i++) {
                if (type.equals("vowel") && (fv.isVowel(glyphs.get(i)))) {
                    return glyphs.get(i);
                } else if (type.equals("consonant") && (fv.isConsonant(glyphs.get(i)))  ) {
                    return glyphs.get(i);
                } else if (type.equals("notDot")) {
                    return glyphs.get(i);
                }
            }

            return wordBoundaryIndex;
        }
        

        public static Set<Context> getAllButNotItselfContext() {
            return EnumSet.complementOf(EnumSet.of(ITSELF));
        }

        public static Set<Context> getPreviousContexts() {
            return EnumSet.of(PREVIOUS_CONSONANT, PREVIOUS_VOWEL, PREVIOUS_POSITION, PREVIOUS_SYMBOL); //PREVIOUS VOWEL GROUP
        }

        public static Set<Context> getClosestContexts() {
            return EnumSet.of(CLOSEST_CONSONANT, CLOSEST_VOWEL, CLOSEST_SYMBOL);
        }

        public static Set<Context> getHistoryFutureFullFeatureSetContexts() {
            return EnumSet.of(PREVIOUS_POSITION, PREVIOUS_SYMBOL, CLOSEST_SYMBOL, ITSELF, NEXT_SYMBOL);
        }

        public static Set<Context> getPastFullFeatureSetContexts() {
            return EnumSet.of(PREVIOUS_POSITION, PREVIOUS_SYMBOL, CLOSEST_SYMBOL, ITSELF);
        }

        public static Set<Context> getFutureContexts() {
            return EnumSet.of(NEXT_CONSONANT, NEXT_VOWEL, NEXT_SYMBOL);
        }

    }

    public enum Level {
        SOURCE(0),
        TARGET(1);

        private int levelIdx;
        private Level(int levelIdx) {
            this.levelIdx = levelIdx;
        }

        public int getLevelIdx() {
            return this.levelIdx;
        }
    }

    public enum TreeType{
        CONSONANT, VOWEL, TYPE_TREE
    }

    public enum BabyTreeType {
        SOURCE, TARGET, JOINT
    }

    public enum AlignmentType {
        STRONG, WEAK, SEMIWEAK
    }

    public enum Features{
        TYPE,
        VERTICAL, HORIZONTAL, ROUNDED, VLENGTH,
        MANNER, PLACE, VOICED, SECONDARY; // , CLENGTH;

        public static Map<Features, Integer> featureVectorPosition;
        public static Map<Features, Integer> featureNameToIndexInMap;
        public static List<List<Character>> featureValueNames;


        static {
            initFeatureNameToIndexMap();
            initVectorPositionMap();
            initFeatureValueNames();

        }

        private static void initVectorPositionMap() {
            featureVectorPosition = new EnumMap(Features.class);
            featureVectorPosition.put(TYPE, 0);
            int i=1;
            for (Features f: getVowelFeatures()) {
                featureVectorPosition.put(f, i++);
            }
            i=1;
            for (Features f: getConsonantFeatures()) {
                featureVectorPosition.put(f, i++);
            }
        }

        private static void initFeatureNameToIndexMap() {
            featureNameToIndexInMap = new EnumMap<Features, Integer>(Features.class);

            int i = 0;
            for (Features f: Features.values()) {
                featureNameToIndexInMap.put(f, i++);
            }
        }

        private static void initFeatureValueNames() {
            featureValueNames = FeatureVocabulary.getFeatureValueLabelList();
        }

        public  List<Character> getFeatureValueNames() {
            int index = featureNameToIndexInMap.get(this);
            return featureValueNames.get(index);
        }


        public static List<Features> getVowelFeatures() {
            return new ArrayList<Features>(EnumSet.of(VERTICAL, HORIZONTAL, ROUNDED, VLENGTH));
        }

        public static List<Features> getConsonantFeatures() {
            return new ArrayList<Features>(EnumSet.of(MANNER, PLACE, VOICED, SECONDARY)); // CLENGTH
        }

        public static List<Features> getTypeFeature() {
            return new ArrayList<Features>(EnumSet.of(TYPE));
        }

        public static List<Features> getFullFeatureSet() {
            return new ArrayList<Features>(EnumSet.allOf(Features.class));
        }
    }

    public enum AlignmentKindIdentifier {
        KK, KDOT, DOTK, VV, VDOT, DOTV, KV, VK, WW, DOTDOT;

        public static Set<AlignmentKindIdentifier> getVowelTreeIdentifiers() {
            return EnumSet.of(VV, VDOT, DOTV);
        }

        public static Set<AlignmentKindIdentifier> getConsonantTreeIdentifiers() {
            return EnumSet.of(KK, KDOT, DOTK);
        }

        public static AlignmentKindIdentifier getAlignmentKind(Input input, int sourceIdx, int targetIdx, int sourceLangId, int targetLangId) {
            FeatureVocabulary fvS = (FeatureVocabulary) input.getVocabulary(sourceLangId);
            FeatureVocabulary fvT = (FeatureVocabulary) input.getVocabulary(targetLangId);

            // get source type
            boolean isSourceVowel = fvS.isVowel(sourceIdx);
            boolean isSourceConsonant = fvS.isConsonant(sourceIdx);
            boolean isTargetVowel = fvT.isVowel(targetIdx);
            boolean isTargetConsonant = fvT.isConsonant(targetIdx);

            // source weak = dot to something
            if (sourceIdx == 0) {
                if (isTargetVowel) {return DOTV;}
                if (isTargetConsonant) {return DOTK;}

            }

            // target weak = something to dot
            if (targetIdx == 0) {
                if (isSourceVowel) {return VDOT;}
                if (isSourceConsonant) {return KDOT;}
            }

            // strong = same type, e.g. K-K, V-V
            // semi-weak = K-V or V-K
            if (isSourceVowel) {
                if(isTargetVowel) {return VV;}
                if(isTargetConsonant) {return VK;}
            }
            
            if (isSourceConsonant) {
                if(isTargetConsonant) {return KK;}
                if(isTargetVowel) {return KV;}
            }
            
            //#-# maybe?
            if (sourceIdx == fvS.getFeatureVocabularySize()-1 &&
                    targetIdx == fvT.getFeatureVocabularySize()-1) {
                return WW;

            }
            if (sourceIdx == 0 && targetIdx == 0) {
                return DOTDOT;
            }

           throw new RuntimeException("No type.");
        }
    }

    List<FeatureTree> getTreesForModification(AlignmentKindIdentifier aki);
    
    List<List<FeatureTree>> getConsonantTrees();
    List<List<FeatureTree>> getVowelTrees();
    List<FeatureTree> getTypeTrees();

    List<FeatureTree> getSourceLevelTrees();
    List<FeatureTree> getTargetLevelTrees();


    void rebuildAllFeatureTrees() throws Exception;
    
    //<Added by Javad for debugging and experiments>     
    public void setConsonantTrees(List<List<FeatureTree>> trees);
    public void setVowelTrees(List<List<FeatureTree>> trees);   
    public void setTypeTrees(List<FeatureTree> trees);
    //</Javad>
}
