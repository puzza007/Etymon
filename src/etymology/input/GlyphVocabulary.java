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

import etymology.config.Configuration;
import etymology.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author arto
 */
public class GlyphVocabulary {
    private String language;

    private String[] words;

    private List<String> indexToGlyph;
    private Map<String, Integer> wordToWordIndexMap;

    private Map<String, Integer> glyphToIndex;

    private List<String> singleLengthGlyphs;
    private int totalGlyphs;
    private List<Integer>[] wordGlyphIndexes;

    private int totalWords;

    public GlyphVocabulary(String language, List<String> words) {
        if (Configuration.getInstance().areWordsFlippedAround()) {
            for (int i=0; i<words.size(); i++) {
                String word = StringUtils.reverseString(words.get(i));
                words.set(i, word);
            }
        }
        this.language = language;
        init(words);

    }

    public Integer getWordIndex(String word) {
        return wordToWordIndexMap.get(word);
    }

    public String getLanguage() {
        return language;
    }

    public int getTotalWords() {
        //System.out.println(Arrays.toString(wordGlyphIndexes));
        //return wordGlyphIndexes.length;
        return totalWords;
    }

    public String[] getWords() {
        return words;
    }

    private void init(List<String> words) {
        this.wordToWordIndexMap = new HashMap();
        this.indexToGlyph = new ArrayList();
        this.glyphToIndex = new HashMap();
        this.totalWords = 0;
        
        //add symbols to indexToGlyph and glyphToIndex
        add(".");
        if (Configuration.getInstance().isRemoveSuffixes()) {
            add("#");
            add("-");
        }

        if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
            add("^"); // indeksi 1
            add("$"); // indeksi 2
        }

        initSingleLengthGlyphs(words);

        //add something like "^a ^b ^c ... a$ b$ c$"
        if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
            for (String g : singleLengthGlyphs) {

                addGlyphCombinations("^" + g, true);
            }


            for (String g : singleLengthGlyphs) {
                addGlyphCombinations(g + "$", false);
            }            
        }
        
        //add something like "aa ab ac ...ba bb bc ... ..."
        for (String glyph : singleLengthGlyphs) {
            addGlyphCombinations(glyph, true);
        }
        
        totalGlyphs = 0;
        for (String word : words) {
            if (!StringUtils.isOkWord(word)) {
                //System.out.println("Not ok word!: " + word);
                continue;
            }

            totalWords++;
            totalGlyphs += StringUtils.getGlyphComboLength(word);
        }

        initWordGlyphIndexes(words);
    }

    private void initWordGlyphIndexes(List<String> words) {
        wordGlyphIndexes = new ArrayList[words.size()];
        this.words = new String[words.size()];
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);

            if (!StringUtils.isOkWord(word)) {
                //System.out.println("Not ok word!: " + word);
                continue;
            }


            if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
                word = "^" + word + "$";
            }

            this.wordToWordIndexMap.put(word, i);
            this.words[i] = word;
            wordGlyphIndexes[i] = getWordAsGlyphIndexes(word);
        }
    }
    
    /**
     * 
     * @param wordIndex
     * @return a list of indexes of all glyphs in the word of wordIndex
     */
    public List<Integer> getWordIndexes(int wordIndex) {
        return wordGlyphIndexes[wordIndex];
    }
    
    
    private void initSingleLengthGlyphs(List<String> words) {
        singleLengthGlyphs = StringUtils.getUniqueGlyphs(words);
    }

    //public List<Integer> getGlyphsAsIndexes(List<String> glyphs) {
    public List<Integer> getGlyphsAsIndexes(List<String> glyphs) {
        List<Integer> indexes = new ArrayList();
        for (String glyph : glyphs) {
            indexes.add(glyphToIndex.get(glyph));
        }

        return indexes;
    }

    private List<Integer> getWordAsGlyphIndexes(String word) {
        List<Integer> glyphIndexes = new ArrayList();
        for (String glyph : StringUtils.splitToGlyphs(word)) {
            glyphIndexes.add(getGlyphIndex(glyph));
        }

        return glyphIndexes;
    }

    public int getTotalGlyphs() {
        return totalGlyphs;
    }

    public int getGlyphIndex(String glyph) {
        return glyphToIndex.get(glyph);
    }

    public String getGlyph(int index) {
        return indexToGlyph.get(index);
    }

    public void add(String glyph) {
        if (glyphToIndex.containsKey(glyph)) {
            return;
        }

        int size = glyphToIndex.size();
        glyphToIndex.put(glyph, size);        
        indexToGlyph.add(glyph);
    }


    public List<String> getSingleLengthGlyphs() {
        return singleLengthGlyphs;
    }

    public List<String> getGlyphs() {
        return indexToGlyph;
    }

    private void addGlyphCombinations(String currentGlyphCombo, boolean appendToEnd) {
        if (!"".equals(currentGlyphCombo)) {
            add(currentGlyphCombo);
            //System.out.println(currentGlyphCombo);
        }
        
        //only add glyphcombos according to getMaxGlyphsToAlign
        if (StringUtils.getGlyphComboLength(currentGlyphCombo) >= Configuration.getInstance().getMaxGlyphsToAlign()) {
            
            return;
        }

        for (String glyph : singleLengthGlyphs) {
            if(appendToEnd) {
                addGlyphCombinations(currentGlyphCombo + glyph, appendToEnd);
                
            } else {
                addGlyphCombinations(glyph + currentGlyphCombo, appendToEnd);
                
            }            
        }
    }

    public int getSize() {
        return indexToGlyph.size();
    }

    public String getWord(int wordIndex) {
        return words[wordIndex];
    }
}

class CaseInsensitiveComparator implements Comparator<String> {

    public int compare(String o1, String o2) {
        return o1.compareToIgnoreCase(o2);
    }
}
