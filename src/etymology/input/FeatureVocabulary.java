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
package etymology.input;

import etymology.align.AlignmentMatrixType;
import etymology.config.Configuration;
import etymology.data.convert.EncodingConverter;
import etymology.data.convert.FeatureConverter;
import etymology.util.StringUtils;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sxhiltun
 */
public class FeatureVocabulary extends GlyphVocabulary {

    private String[] indexToFeature;
    private Map<String, Integer> featureToIndex;
    private List<String> uniqueFeatures;
    private List<Integer>[] wordFeatureIndexes;
    private int totalFeatures;
    private static final int VOWEL_LENGTH = 5;
    private static final int CONSONANT_LENGTH = 6;
    private List<Map<Character, List<Integer>>> vowelFeatureValueFilter;
    private List<Map<Character, List<Integer>>> consonantFeatureValueFilter;
    private List<Map<Character, List<Integer>>> combinedFeatureValueFilter;
    private static List<List<Character>> featureValueLabels;

    public FeatureVocabulary(String language, List<String> words) {        
        super(language, words);
        //System.out.println("lang :" + language);

        if(Configuration.getInstance().isUseFeatures()) {
            init(language, words);
        }
        
        else if(Configuration.getInstance().isFirstBaselineThenContext()) {
            init(language, words);
        }

        else if(Configuration.getInstance().isUseImputation()) {
            init(language, words);
        }
    }

    /**
     *
     * @param features
     * @return list of indices of given features
     */
    public List<Integer> getFeaturesAsIndexes(List<String> features) {

        List<Integer> indexes = new ArrayList();
        for (String feature : features) {
            indexes.add(featureToIndex.get(feature));
        }

        return indexes;
    }

    /**
     *
     * @param feature
     * @return the index of the given feature
     */
    @Override
    public int getGlyphIndex(String feature) {
        if(!Configuration.getInstance().isUseFeatures()) {
            return super.getGlyphIndex(feature);
        }

        try {
            if (".".equals(feature)) {
                return super.getGlyphIndex(feature);
            }

            if ("#".equals(feature) || feature.length() > 4) {
                return featureToIndex.get(feature);
            }

            return super.getGlyphIndex(feature);

        } catch (Exception e) {
            System.out.println("feature " + feature);
            e.printStackTrace();            
            System.exit(1);

        }
        return -1;
    }

    public int getFeatureIndex(String feature) {        
        return featureToIndex.get(feature);
    }

    /**
     *
     * @param index
     * @return the symbol (feature) that the index presents
     *
     */
    public String getFeature(int index) {
        return indexToFeature[index];
    }


    /**
     *
     * @return the list of unique features of language
     * (only the real vectors, not (., #))
     */
    public List<String> getSingleLengthFeatures() {
        return uniqueFeatures;
    }

    /**
     *
     * @return mapping from list indices to features
     * including "." and "#" (for word boundary)
     */
    public List<String> getFeatures() {
        return Arrays.asList(indexToFeature);
    }

    /**
     *
     * @return size of the vocabulary -- # of distinct features (includes . and #)
     */
    public int getFeatureVocabularySize() {
        return indexToFeature.length;
    }

    public boolean isVowel(int symbolIndex) {

        String feature = indexToFeature[symbolIndex];
        if (feature.startsWith("V")) {
            return true;
        }
        return false;
    }

    public boolean isConsonant(int symbolIndex) {

        String feature = indexToFeature[symbolIndex];
        if(feature==null){
            System.out.println("Feature is null.");
        }
        if (feature.startsWith("C")) {            
            return true;
        }
        return false;


    }


    public List<Integer> getVowelIndices() {
        return this.vowelFeatureValueFilter.get(0).get('V');
    }

    public List<Integer> getConsonantIndices() {
        return this.consonantFeatureValueFilter.get(0).get('C');
    }

    private void init(String language, List<String> words) {
        List<String> featureBlocks = mapWordsToFeatures(language, words);
       
        featureToIndex = new HashMap();
        indexToFeature = new String[super.getGlyphs().size() + 1];


        if (featureBlocks.size() != words.size()) {
            System.err.println("All words do not have feature vectors!");
            System.exit(1);
        }

        getUniqueFeatures(featureBlocks);


        add(".", 0); //dot
        List<Integer> wordIndexes;
        String featuresAsBlock;
        List<String> separatedFeatures;
        
        for (int idx = 0; idx < super.getWords().length; idx++) {
          
            
            if (!StringUtils.isOkWord(super.getWord(idx))) {
                continue;
            }
            
            // e.g. (5,4,6,7)
            wordIndexes = super.getWordIndexes(idx);  
            //e.g. "VhFn3,CPb-n1,VlBn3,VlBn3"
            featuresAsBlock = featureBlocks.get(idx);
            
            // split by "," into a list
            separatedFeatures = StringUtils.splitToFeatures(featuresAsBlock);
            
//            System.out.println("word index: " + idx);            
//            System.out.println("features: " + separatedFeatures);
//            System.out.println("word: " + wordIndexes);
            
            for (int glyphIdx = 0; glyphIdx < wordIndexes.size(); glyphIdx++) {
                String feature = null;

                if (Configuration.getInstance().isUseImputation() &&
                        Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {

                    if ((wordIndexes.get(glyphIdx) == 1) || (wordIndexes.get(glyphIdx) == 2)) {
                        continue;
                    } else {
                        feature = separatedFeatures.get(glyphIdx-1);
                        add(feature, wordIndexes.get(glyphIdx));
                    }

                    
                } else {

                    try {
                        feature = separatedFeatures.get(glyphIdx);

                    }catch(Exception e) {
                        System.err.println(glyphIdx);
                        System.err.println(separatedFeatures);
                        System.err.println(wordIndexes);

                    }
                    add(feature, wordIndexes.get(glyphIdx));
                }
            }

        }

        add("#", indexToFeature.length - 1); //word boundary

        totalFeatures = 0;
        for (String featureBlock : featureBlocks) {
            if (!StringUtils.isOkFeature(featureBlock)) {
                continue;
            }

            totalFeatures += featureBlock.split(",").length;
        }

        initWordFeatureIndexes(featureBlocks);
        initFeatureValueFilteringMaps();
        if (featureValueLabels == null) {
            initFeatureValueLabelList();
        }


        int i=0;
        for (String s : indexToFeature) {
            if (s == null && !Configuration.getInstance().isUseImputation()) {
                System.out.print("glyph " + i + " " + getGlyph(i));
                for (char c : EncodingConverter.decompose(String.valueOf(getGlyph(i))).toCharArray()) {
                    System.out.print("  " + c + " " + (int)c);
                    
                }
                System.out.println("");
                System.out.println(Arrays.toString(indexToFeature));
                
            }
            i++;
        }
        
        //System.out.println(super.getGlyph(super.getGlyphIndex("VcMn3")));
        //for(int j = 0; j < indexToFeature.length; j++) {
        //    System.out.println(j + "  " + indexToFeature[j]);
        //}
        //System.out.println("glyph size: " + super.getGlyphs().size());
        //System.out.println("***************************");
    }

    public final void add(String feature, int idx) {
        if (featureToIndex.containsKey(feature)) {
            return;
        }

        featureToIndex.put(feature, idx);
        indexToFeature[idx] = feature;
    }



    public List<Map<Character, List<Integer>>> getCombinedFeatureValueFilter() {
        return combinedFeatureValueFilter;
    }

    public List<Map<Character, List<Integer>>> getVowelFeatureValueFilter() {
        return vowelFeatureValueFilter;
    }

    public List<Map<Character, List<Integer>>> getConsonantFeatureValueFilter() {
        return consonantFeatureValueFilter;
    }

    private void initWordFeatureIndexes(List<String> featureBlocks) {
        wordFeatureIndexes = new ArrayList[featureBlocks.size()];
        for (int i = 0; i < featureBlocks.size(); i++) {

            String feature = featureBlocks.get(i);
            if (!StringUtils.isOkFeature(feature)) {
                continue;
            }

            wordFeatureIndexes[i] = getWordAsFeatureIndexes(feature);
        }
    }

    public String getWordAsStringFeatures(int wordIndex) {
//        System.out.println("Word: " + getWord(wordIndex));
//        System.out.println(wordFeatureIndexes[wordIndex]);
        if (wordFeatureIndexes == null) {
            return null;
        }

        List<Integer> wordFeatureIndexList = wordFeatureIndexes[wordIndex];

        StringBuilder sb = new StringBuilder();
        for (int wordFeatureIndex : wordFeatureIndexList.subList(0, wordFeatureIndexList.size() - 1)) {
            sb.append(getFeature(wordFeatureIndex)).append(",");
        }

        sb.append(getFeature(wordFeatureIndexList.get(wordFeatureIndexList.size() - 1)));
        return sb.toString();
    }

    public String getWordAsStringFeatures(String word) {
        Integer wordIndex = getWordIndex(word);
        if (wordIndex == null) {
            System.out.println("Illegal word index.");
            return null;
        }

        return getWordAsStringFeatures(wordIndex);
    }

    private List<Integer> getWordAsFeatureIndexes(String featureBlock) {
        List<Integer> featureIndexes = new ArrayList();
        for (String feature : StringUtils.splitToFeatures(featureBlock)) {
            featureIndexes.add(featureToIndex.get(feature));
        }

        return featureIndexes;
    }

    private void getUniqueFeatures(List<String> features) {
        uniqueFeatures = StringUtils.getUniqueFeatures(features);
    }

    private void initFeatureValueLabelList() {
        featureValueLabels = new ArrayList();
        for (Map<Character, List<Integer>> combinedFilter : combinedFeatureValueFilter) {
            featureValueLabels.add(new ArrayList(combinedFilter.keySet()));
        }
        
        //add word boundary symbol here
        featureValueLabels.get(0).add('#');
    }

    public static List<List<Character>> getFeatureValueLabelList() {
        return featureValueLabels;
    }

    private void initFeatureValueFilteringMaps() {
        vowelFeatureValueFilter = new ArrayList();
        vowelFeatureValueFilter = buildVowelFeatureFilter(vowelFeatureValueFilter);
        consonantFeatureValueFilter = new ArrayList();
        consonantFeatureValueFilter = buildConsonantFeatureFilter(consonantFeatureValueFilter);

        fillFeatureFilteringMaps();

        combinedFeatureValueFilter = new ArrayList();
        combinedFeatureValueFilter.addAll(vowelFeatureValueFilter);
        combinedFeatureValueFilter.addAll(consonantFeatureValueFilter.subList(1, consonantFeatureValueFilter.size()));
        combinedFeatureValueFilter = fixTypeFeatureIssue(combinedFeatureValueFilter);
    }
    
    //merge the TYPE feature in the two lists , more organized 
    private List<Map<Character, List<Integer>>> fixTypeFeatureIssue(
            List<Map<Character, List<Integer>>> combinedFeatureValueFilter) {

        // # -> index = last
        List<Integer> wordBoundaryIndex = new ArrayList<Integer>();
        int indexOfWordBoundary = uniqueFeatures.size() + 1;
        wordBoundaryIndex.add(indexOfWordBoundary);


        // . -> index =  0
        List<Integer> dotIndex = new ArrayList<Integer>();
        dotIndex.add(0);

        List<Integer> consonants = consonantFeatureValueFilter.get(0).get('C');
        combinedFeatureValueFilter.get(0).put('C', consonants);
        combinedFeatureValueFilter.get(0).put('.', dotIndex);
        //combinedFeatureValueFilter.get(0).put('#', wordBoundaryIndex);

        return combinedFeatureValueFilter;

    }

    private void fillFeatureFilteringMaps() {
        List<Map<Character, List<Integer>>> filter = null;

        for (int i = 0; i < uniqueFeatures.size(); i++) {

            String feature = uniqueFeatures.get(i);

//            System.out.println("unique: " + uniqueFeatures);
//            System.out.println("all: " + Arrays.toString(indexToFeature));
//            System.out.println("i " + i);
//            System.out.println("feature: " + feature);

            int index = getFeatureIndex(feature);
            filter = (feature.length() == VOWEL_LENGTH)
                    ? vowelFeatureValueFilter : consonantFeatureValueFilter;

            for (int j = 0; j < feature.length(); j++) {
                filter.get(j).get(feature.charAt(j)).add(index);
            }
        }
    }

    private List<Map<Character, List<Integer>>> buildVowelFeatureFilter(List<Map<Character, List<Integer>>> vowels) {

        for (int i = 0; i < VOWEL_LENGTH; i++) {
            //vowels.add(new TreeMap());
            //LinkedHashMap remain the inserted order
            vowels.add(new LinkedHashMap());
            switch (i) {
                case 0: // Group: G
                    vowels.get(i).put('V', new ArrayList()); //vowel V
                    break;
                case 1: // Vertical articulation: V
                    vowels.get(i).put('h', new ArrayList()); //close/high
                    vowels.get(i).put('c', new ArrayList()); //mid-close
                    vowels.get(i).put('o', new ArrayList()); //mid-open
                    vowels.get(i).put('l', new ArrayList()); //open/low
                    break;
                case 2: // Horizontal articulation: H
                    vowels.get(i).put('F', new ArrayList()); //front
                    vowels.get(i).put('M', new ArrayList()); //central/medium
                    vowels.get(i).put('B', new ArrayList()); //front
                    break;
                case 3: // Rounded: R
                    vowels.get(i).put('n', new ArrayList()); //no
                    vowels.get(i).put('u', new ArrayList()); //yes
                    break;
                case 4: // Length: L
                    vowels.get(i).put('1', new ArrayList());
                    vowels.get(i).put('2', new ArrayList());
                    vowels.get(i).put('3', new ArrayList());
                    vowels.get(i).put('4', new ArrayList());
                    vowels.get(i).put('5', new ArrayList());
                    break;
            }
        }
        return vowels;
    }

    private List<Map<Character, List<Integer>>> buildConsonantFeatureFilter(List<Map<Character, List<Integer>>> consonants) {
        for (int i = 0; i < CONSONANT_LENGTH; i++) {
            //consonants.add(new TreeMap());
            consonants.add(new LinkedHashMap());
            switch (i) {
                case 0: // Group: G
                    consonants.get(i).put('C', new ArrayList());
                    break;
                case 1: // Manner: M
                    consonants.get(i).put('P', new ArrayList()); //Plosive-stop
                    consonants.get(i).put('N', new ArrayList()); //nasal
                    consonants.get(i).put('L', new ArrayList()); //lateral
                    consonants.get(i).put('T', new ArrayList()); //trill
                    consonants.get(i).put('F', new ArrayList()); //spirant-Frikative
                    consonants.get(i).put('S', new ArrayList()); //sibilant
                    consonants.get(i).put('W', new ArrayList()); //semi-vowel
                    consonants.get(i).put('A', new ArrayList()); //affricate
                    break;
                case 2: // Place of articulation: P
                    consonants.get(i).put('b', new ArrayList()); //bilabial
                    consonants.get(i).put('l', new ArrayList()); //labiodental
                    consonants.get(i).put('d', new ArrayList()); //dental
                    consonants.get(i).put('r', new ArrayList()); //retroflex
                    consonants.get(i).put('v', new ArrayList()); //velar
                    consonants.get(i).put('u', new ArrayList()); //uvular
                    break;
                case 3: // Voiced: V
                    consonants.get(i).put('-', new ArrayList()); //no
                    consonants.get(i).put('+', new ArrayList()); //yes
                    break;
                case 4: // Secondary articulation: S
                    consonants.get(i).put('n', new ArrayList()); //none
                    consonants.get(i).put('\'', new ArrayList()); //palatalized
                    consonants.get(i).put('w', new ArrayList()); //labialized
                    consonants.get(i).put('h', new ArrayList()); //aspirated
                    break;
                case 5: // Length: L
                    consonants.get(i).put('1', new ArrayList()); //normal/short
                    consonants.get(i).put('2', new ArrayList()); //long
                    break;
            }
        }
        return consonants;
    }
    /**
     * 
     * @param language
     * @param words
     * @return features of each word as a list
     */
    private List<String> mapWordsToFeatures(String language, List<String> words) {
        List<String> featureList = new ArrayList();

        for (String word : words) {
            if (!StringUtils.isOkWord(word)) {
            //if ("-".equals(word)) {                
                featureList.add(null);
                continue;
            }
            
            String features = FeatureConverter.getFeatures(word, language);
            featureList.add(features);
            //System.out.println(word + ' ' + language);
            //System.out.println(features);

        }

        //System.out.println(featureList);
        return featureList;
    }
}
