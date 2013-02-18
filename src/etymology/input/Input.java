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
import etymology.data.convert.ConversionRules;
import etymology.util.StringUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.omg.CORBA.MARSHAL;

/**
 *
 * @author arto
 */
public class Input {

    private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private Map<Integer, GlyphVocabulary> languageIdToSymbolVocabularies = new HashMap();
    private int numOfWords = 0;
    private int totalGlyphs;
    private int totalWordLength;
    private Map<String, Integer> languageToLanguageIdMap;
    private Map<Integer, String> languageIdToLanguageMap;
    private static Input instance;

    public static Input getInstance() {
        return instance;
    }

    public static void setInstance(Input input) {
        instance = input;
    }

    public Input(Configuration config) throws Exception {
        this(new File(config.getInputFile()), config.getLanguages(), config.getConversionRules());
    }

    public Input(File inputFile, List<String> languages) throws Exception {
        this(inputFile, languages, null);
    }

    public Input(File inputFile, List<String> languages, ConversionRules conversionRules) throws Exception {
        initData(inputFile, languages, conversionRules, null);
    }
    
    public Input(Configuration config, List<Integer> wordsToRemove) throws Exception {
        initData(new File(config.getInputFile()), config.getLanguages(), config.getConversionRules(), wordsToRemove);
    }
    
    public Input(Configuration config, Map<String, List<String>> wordsFromExistingModel) {
        initExistingData(wordsFromExistingModel, config.getLanguages(), config.getConversionRules());
    }

    public void reset() {
        totalGlyphs = -1;
        totalWordLength = -1;
    }

    private void initExistingData(Map<String, List<String>> wordsFromExistingModel, 
        List<String> languages, ConversionRules conversionRules) {
        instance = this;
        numOfWords = 0;

        

        // setup alphabets        
        Map<String, List<String>> wordColumns = wordsFromExistingModel;                
        applyConversionRules(conversionRules, wordColumns);

        if (Configuration.getInstance().hasWordLimit()) {
            removeNotOkWords(wordColumns);
        }
                
        this.languageIdToLanguageMap = new TreeMap();
        this.languageToLanguageIdMap = new TreeMap();

        Set<String> usedLanguages = new HashSet();
        for (int langId = 0; langId < languages.size(); langId++) {
            String langNoId = languages.get(langId);

            String lang = languages.get(langId);

            int count = 1;
            while (usedLanguages.contains(lang)) {
                lang = languages.get(langId) + "-" + count;
                count++;
            }

            this.languageIdToLanguageMap.put(langId, lang);
            this.languageToLanguageIdMap.put(lang, langId);

            // although there might be same languages, we still generate separate vocabularies for each
            List<String> column = wordColumns.get(lang);
        
            if(langNoId.equals(langNoId)) {
                applyConversionRules(conversionRules, "fin", column);
            }

        
            usedLanguages.add(lang);

            languageIdToSymbolVocabularies.put(langId, new FeatureVocabulary(lang, column));
            numOfWords = Math.max(wordColumns.get(lang).size(), numOfWords);
        }

        //System.out.println(languageIdToSymbolVocabularies.keySet());
        reset();
        
        
    }
    
    private void initData(File inputFile, List<String> languages, ConversionRules conversionRules, 
            List<Integer> wordsToRemove) throws Exception {
        instance = this;
        numOfWords = 0;
        
        //in n dimension model, we can missed at most n-2 words when doing alignment
        int maxWordsToMiss = Math.abs(languages.size() - 2); // 0

        // setup alphabets
        DataTableReader dataReader = new DataTableReader(inputFile);
        Map<String, List<String>> wordColumns = dataReader.getWordColumns(languages, maxWordsToMiss);        
        
        
        applyConversionRules(conversionRules, wordColumns);

        if (Configuration.getInstance().hasWordLimit()) {
            removeNotOkWords(wordColumns);
        }
        
        if (wordsToRemove != null) {
            
            removeWordsOnTheList(wordColumns, wordsToRemove);
        }

        this.languageIdToLanguageMap = new TreeMap();
        this.languageToLanguageIdMap = new TreeMap();

        Set<String> usedLanguages = new HashSet();
        for (int langId = 0; langId < languages.size(); langId++) {
            String langNoId = languages.get(langId);

            String lang = languages.get(langId);

            int count = 1;
            while (usedLanguages.contains(lang)) {
                lang = languages.get(langId) + "-" + count;
                count++;
            }

            this.languageIdToLanguageMap.put(langId, lang);
            this.languageToLanguageIdMap.put(lang, langId);

            // although there might be same languages, we still generate separate vocabularies for each
            List<String> column = wordColumns.get(lang);
            if (Configuration.getInstance().isFuzzUpFinnish() && "fin".equalsIgnoreCase(langNoId)) {
                System.out.println("Fuzzing up fin!");
                // applyConversionRules(conversionRules, "fin-cyr", column);
                applyConversionRules(conversionRules, "fin", column);
            }

            /*if(langNoId.equals(langNoId)) {
                applyConversionRules(conversionRules, "fin", column);
            }*/

            if(Configuration.getInstance().isFuzzUpFinnish() && Configuration.getInstance().getFuzzificationProb() > 0.0) {
                column = fuzzUpColumn(column);
            }

            usedLanguages.add(lang);

            languageIdToSymbolVocabularies.put(langId, new FeatureVocabulary(lang, column));
            numOfWords = Math.max(wordColumns.get(lang).size(), numOfWords);
        }

        //System.out.println(languageIdToSymbolVocabularies.keySet());
        reset();
    }
    
    private List<String> fuzzUpColumn(List<String> column) {
        List<String> novelCol = new ArrayList(column);
        Random rnd = new Random();

        double fuzzProb = Configuration.getInstance().getFuzzificationProb();

        for (int i = 0; i < novelCol.size(); i++) {
            String word = novelCol.get(i);
            if (!StringUtils.isOkWord(word)) {
                continue;
            }

            String novelWord = "";
            char[] wordChars = word.toCharArray();
            int idx = 0;
            while (idx < wordChars.length) {
                if (rnd.nextDouble() < fuzzProb) {
                    novelWord += rnd.nextInt(10);
                } else {
                    novelWord += wordChars[idx];
                    idx++;
                }
            }

            novelCol.set(i, novelWord);
        }

        return novelCol;
    }
    
    private void removeWordsOnTheList(Map<String, List<String>> wordColumns, List<Integer> wordColumnIndicesToRemove) {
        Collections.sort(wordColumnIndicesToRemove);
        Collections.reverse(wordColumnIndicesToRemove);
        
        for (List<String> wordColumn : wordColumns.values()) {
            for (int indexToRemove : wordColumnIndicesToRemove) {
                wordColumn.remove(indexToRemove);
            }
        }
                
    }

    private void removeNotOkWords(Map<String, List<String>> wordColumns) {
        
        List<Integer> wordColumnIndicesToRemove = new ArrayList();
        for (List<String> wordColumn : wordColumns.values()) {
            for (int wordIndex = 0; wordIndex < wordColumn.size(); wordIndex++) {
                String word = wordColumn.get(wordIndex);

                if (!StringUtils.isOkWord(word)) {
                    wordColumnIndicesToRemove.add(wordIndex);
                }
            }
        }

        Collections.sort(wordColumnIndicesToRemove);
        Collections.reverse(wordColumnIndicesToRemove);
        for (List<String> wordColumn : wordColumns.values()) {
            for (int indexToRemove : wordColumnIndicesToRemove) {
                wordColumn.remove(indexToRemove);
            }
        }

        for (String wordColumnKey : wordColumns.keySet()) {
            List<String> wordColumn = wordColumns.get(wordColumnKey);
            if (Configuration.getInstance().getMaxWordsToUse() < wordColumn.size()) {
                wordColumn = wordColumn.subList(0, Configuration.getInstance().getMaxWordsToUse());
                wordColumns.put(wordColumnKey, wordColumn);
            }
        }
    }

    public String getLanguage(int languageId) {
        return languageIdToLanguageMap.get(languageId);
    }

    public Integer getLanguageId(String language) {
        return languageToLanguageIdMap.get(language);
    }

    public Set<String> getLanguages() {
        return languageToLanguageIdMap.keySet();
    }

    public Set<Integer> getLanguageIds() {
        return languageIdToLanguageMap.keySet();
    }

    private void applyConversionRules(ConversionRules conversionRules, Map<String, List<String>> wordColumns) {
        if (conversionRules == null) {
            return;
        }        
        // if language against itself, apply rules only once, otherwise goes wrong (hungarian chain rules)
        
        for (String language : wordColumns.keySet()) {
            String ruleLang = language;
            ruleLang = ruleLang.split("[-_]")[0];
            //if(ruleLang : etymology.Main.Ss)
            
//            if(ruleLang.contains("-") ) {
//                ruleLang = ruleLang.substring(0, ruleLang.indexOf("-"));
//            } else if (ruleLang.contains("_")) {
//                ruleLang = ruleLang.substring(0, ruleLang.indexOf("_"));
//            } 
//                        
            applyConversionRules(conversionRules, ruleLang, wordColumns.get(language));                  
            applyConversionRules(conversionRules, "*", wordColumns.get(language));
        }
    }

    private void applyConversionRules(ConversionRules rules, String language, List<String> words) {
        for (int wordIndex = 0; wordIndex < words.size(); wordIndex++) {
            words.set(wordIndex, rules.applyConversionRule(language, words.get(wordIndex)));
        }
    }

    public String getWord(int languageId, int wordIndex) {
        return getVocabulary(languageId).getWord(wordIndex);
    }

    public int getWordIndex(int languageId, String word) {
        String[] words  = getVocabulary(languageId).getWords();
        for (int i=0; i<words.length; i++) {
            if (words[i].equals(word)) {
                return i;
            }
        }
        return -1;
    }

    public int getNumOfLanguages() {
        return languageIdToSymbolVocabularies.keySet().size();
    }

    public int getNumOfWords() {
        return numOfWords;
    }

    public GlyphVocabulary getVocabulary(int languageId) {
        return languageIdToSymbolVocabularies.get(languageId);
    }

    public void setVocabulary(int languageId, GlyphVocabulary vocabulary) {
        languageIdToSymbolVocabularies.put(languageId, vocabulary);
    }

    public List<GlyphVocabulary> getVocabularies() {
        return new ArrayList(languageIdToSymbolVocabularies.values());
    }

    public int getLengthOneGlyphCount(int languageId) {
        return getVocabulary(languageId).getSingleLengthGlyphs().size();
    }

    public Collection<String> getSingleLengthSymbols(int languageId) {
        return languageIdToSymbolVocabularies.get(languageId).getSingleLengthGlyphs();
    }

    public List<Integer> getWordIndexes(int languageId, int wordIndex) {
        return getVocabulary(languageId).getWordIndexes(wordIndex);
    }

    public boolean hasMissingWord(int wordIndex) {
        for (GlyphVocabulary v : getVocabularies()) {
            if (v.getWordIndexes(wordIndex) == null) {
                return true;
            }
        }

        return false;
    }

    public String getWordFromIndexes(int languageId, List<Integer> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return "-";
        }

        StringBuilder sb = new StringBuilder();
        for (int i : indexes) {
            sb.append(getVocabulary(languageId).getGlyph(i));
        }
        return sb.toString();
    }

    public String getPrintableWordFromIndexes(int languageId, List<Integer> indexes) {
        StringBuilder sb = new StringBuilder();
        for (int i : indexes) {
            sb.append("   ").append(getVocabulary(languageId).getGlyph(i));
        }
        return sb.toString();
    }

    public int getTotalWordLength() {
        if (totalWordLength >= 0) {
            return totalWordLength;
        }

        totalWordLength = 0;
        for (int languageId : languageIdToSymbolVocabularies.keySet()) {
            totalWordLength += getVocabulary(languageId).getTotalGlyphs();
        }

        totalWordLength += (getNumOfLanguages() * getNumOfWords());
        return totalWordLength;
    }

    public int getTotalGlyphs() {
        if (totalGlyphs >= 0) {
            return totalGlyphs;
        }

        totalGlyphs = 0;
        for (int languageId : languageIdToSymbolVocabularies.keySet()) {
            totalGlyphs += getLengthOneGlyphCount(languageId);
        }

        return totalGlyphs;
    }



}
