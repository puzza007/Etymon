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

package etymology.impute;

import etymology.align.Alignator;
import etymology.config.Constants;
import etymology.input.FeatureVocabulary;
import etymology.input.Input;
import etymology.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author sxhiltun
 */
public class ContextImputator extends Imputator {

    private Alignator alignator;
    private Input input;

    private int sourceLangId;
    private int targetLangId;

    private int maxWordsToAlign;

    public ContextImputator(Alignator alignator, int sourceLanguageId, int targetLanguageId) {

        super(alignator.getInput());

        this.alignator = alignator;
        this.input = alignator.getInput();

        this.sourceLangId = sourceLanguageId;
        this.targetLangId = targetLanguageId;
        setMaxWordsToAlign(input.getNumOfWords());

    }

    public Alignator getCachedAlignator() {
        return this.alignator;
    }

    @Override
    public void imputeWords() throws Exception {

        FeatureVocabulary targetFeatureVocabulary = (FeatureVocabulary)input.getVocabulary(targetLangId);

        Logger finalLogger = alignator.getFinalLogger();
        //logLoggerTitle(finalLogger);
        logCompleteLoggerTitle(finalLogger);
                
        for (int wordIndex = 0; wordIndex < input.getNumOfWords(); wordIndex++) {
          
            String imputed;
            
            String wordToImputeFrom = input.getWord(sourceLangId, wordIndex);
            //System.out.println("Now imputing : " + wordToImputeFrom);
            
            NBestSolutions nb = new NBestSolutions(alignator, input, sourceLangId, targetLangId, Constants.BEST_PATHS_OF_CONTEXT_IMPUTATION, this);
            imputed = nb.imputeWord(wordIndex);

            
            String correctWord = input.getWord(targetLangId, wordIndex);


            //logImputationResult(wordToImputeFrom, correctWord, imputed);

            imputed = imputed.replaceAll("\\.", "");

            addLevenshteinDicreteDist(imputed, correctWord);
            addLevenshteinFeatureWiseDist(imputed, correctWord, targetFeatureVocabulary);
            addCorrectWordsCharacterLength(correctWord);


            if(correctWord.equals(imputed)) {
                addExactMatch();
            }
        }

        logImputationSummary();
        
    }
    
    public void sanityCheckOfImputationCost(List<List<String>> wordPairs) throws Exception {
        FeatureVocabulary targetFeatureVocabulary = (FeatureVocabulary)input.getVocabulary(targetLangId);

        Logger finalLogger = alignator.getFinalLogger();        
        logCompleteLoggerTitle(finalLogger);
 
        for (List<String> wordPair :  wordPairs) {
            String original = wordPair.get(0);
            String imputed = wordPair.get(1);
            
            addLevenshteinDicreteDist(imputed, original);
            addLevenshteinFeatureWiseDist(imputed, original, targetFeatureVocabulary);
            addCorrectWordsCharacterLength(original);


            if(original.equals(imputed)) {
                addExactMatch();
            }
        }

    logImputationSummary();
                
        
    }

    public void computeCosts(String sourceWord, String targetWord) throws Exception {
        String wordOfInterest = sourceWord.replaceAll("\\.", "");
        int wordIndex = input.getWordIndex(sourceLangId, wordOfInterest);
        if (wordIndex < 0) {
            System.out.println("Error: word index was: " + wordIndex);
            System.exit(wordIndex);
        }

        List<Integer> sourceIndexes = new ArrayList<Integer>();
        for (String s : StringUtils.splitToGlyphs(sourceWord)) {
            sourceIndexes.add(input.getVocabulary(sourceLangId).getGlyphIndex(s));
        }

        List<Integer> targetIndexes = new ArrayList<Integer>();
        for (String t : StringUtils.splitToGlyphs(targetWord)) {
            targetIndexes.add(input.getVocabulary(targetLangId).getGlyphIndex(t));
        }

        NBestSolutions nb = new NBestSolutions(alignator, input, sourceLangId, targetLangId, Constants.BEST_PATHS_OF_CONTEXT_IMPUTATION, this);
        nb.compareCosts(wordIndex, sourceIndexes, targetIndexes);

    }


    @Override
    public void setUseSingleAlignmentIterationForImputation(boolean useSingleAlignmentIterationForImputation) {
        throw new UnsupportedOperationException("Not supported yet.");
    }



}
