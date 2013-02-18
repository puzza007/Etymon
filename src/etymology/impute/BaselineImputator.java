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
import etymology.align.AlignmentMatrix;
import etymology.align.WordAlignment;
import etymology.config.Configuration;
import etymology.config.Constants;
import etymology.input.FeatureVocabulary;
import etymology.input.GlyphVocabulary;
import etymology.input.Input;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author sxhiltun
 */
public class BaselineImputator extends Imputator {


    private Input input;
    private int sourceLangId;
    private int targetLangId;

    private int maxWordsToAlign;
    private boolean useSingleAlignmentIterationForImputation;
    private Alignator cachedAlignator;

    public BaselineImputator(Input input, int sourceLanguageId, int targetLanguageId) {
        this(input, sourceLanguageId, targetLanguageId, input.getNumOfWords());
    }

    public BaselineImputator(Alignator alignator, Input input, int sourceLanguageId, int targetLanguageId)  {
        this(input, sourceLanguageId, targetLanguageId, input.getNumOfWords());
        this.cachedAlignator = alignator;
        useSingleAlignmentIterationForImputation = true;
    }

    public BaselineImputator(Input input, int sourceLanguageId, int targetLanguageId, int maxWordsToAlign) {
        super(input);

        this.input = input;
        this.sourceLangId = sourceLanguageId;
        this.targetLangId = targetLanguageId;
        
        this.maxWordsToAlign = maxWordsToAlign;
        if(this.maxWordsToAlign > input.getNumOfWords()) {
            this.maxWordsToAlign = input.getNumOfWords();
        }

        setMaxWordsToAlign(this.maxWordsToAlign);
    }


    @Override
    public void imputeWords() throws Exception {

        FeatureVocabulary targetFeatureVocabulary = (FeatureVocabulary)input.getVocabulary(targetLangId);

        Logger finalLogger = cachedAlignator.getFinalLogger();        
        logLoggerTitle(finalLogger);
        

        for (Integer wordIndex : getWordIndexes()) {
            System.out.println("Imputing word at index " + wordIndex);

            String wordToImputeFrom = input.getWord(sourceLangId, wordIndex);
            Alignator alignator = getAlignator(wordIndex);

            String imputed = runNaiveImputation(alignator, wordIndex);
            String correctWord = input.getWord(targetLangId, wordIndex);

            logImputationResult(wordToImputeFrom, correctWord, imputed);
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

    public Alignator getCachedAlignator() {
        return this.cachedAlignator;
    }

    private Alignator getAlignator(int imputingWordAtIndex) throws Exception {
        
        if(useSingleAlignmentIterationForImputation) {
            if(cachedAlignator != null) {
                
                return cachedAlignator;
            }
            
            Configuration.getInstance().setImputeWordAtIndex(null);
            Alignator alignator = new Alignator(Configuration.getInstance(), input);
            alignator.setExecuteSanityChecks(false);
            alignator.align();

            cachedAlignator = alignator;
            return cachedAlignator;
        }

        Configuration.getInstance().setImputeWordAtIndex(imputingWordAtIndex);
        Alignator alignator = new Alignator(Configuration.getInstance(), input);
        alignator.setExecuteSanityChecks(false);
        alignator.align();

        return alignator;
    }

    private String runNaiveImputation(Alignator alignator, Integer wordIndex) throws RuntimeException {
        String imputed;
        WordAlignment wordAlignment = null;

        //deregister the word that is imputed if using the "cheat" - model
        if (useSingleAlignmentIterationForImputation) {
            wordAlignment = alignator.getAlignmentStorage().deregisterAlignment(alignator.getAlignmentMatrix(), wordIndex);
        }

        imputed = naiveImputeSingleWord(alignator.getAlignmentMatrix(), wordIndex);

        //reregister the word
        if (useSingleAlignmentIterationForImputation) {            
            alignator.getAlignmentStorage().registerAlignment(alignator.getAlignmentMatrix(), wordAlignment, wordIndex);
        }

        //remove the dots
        return imputed;
        //return imputed.replaceAll("\\.", "");
        
    }

    private String naiveImputeSingleWord(AlignmentMatrix matrix, int wordIndex) throws RuntimeException {

        List<Integer> imputedAlignment = new ArrayList();

        GlyphVocabulary sourceVoc = Input.getInstance().getVocabulary(sourceLangId);

        for(int sourceGlyphIdx: sourceVoc.getWordIndexes(wordIndex)) {

            //source glyph can not be dot
            if(sourceGlyphIdx == Constants.DOT_INDEX) {
                continue;
            }

            int targetGlyphIdx;
            if(sourceLangId == 0) {
                targetGlyphIdx = matrix.getMostProbableGlyphAlignmentByIndex(sourceGlyphIdx, null);
            } else { // assuming 1
                targetGlyphIdx = matrix.getMostProbableGlyphAlignmentByIndex(null, sourceGlyphIdx);
            }

            imputedAlignment.add(targetGlyphIdx);
        }


        return input.getWordFromIndexes(targetLangId, imputedAlignment);
    }



    @Override
    public void setUseSingleAlignmentIterationForImputation(boolean useSingleAlignmentIterationForImputation) {
        this.useSingleAlignmentIterationForImputation = useSingleAlignmentIterationForImputation;
    }

    

    // return just a list [0, 1, 2, ... , numOfWords]
    private List<Integer> getWordIndexes() {
        List<Integer> wordIndexes = new ArrayList();
        for(int idx = 0; idx < input.getNumOfWords(); idx++) {
            wordIndexes.add(idx);
        }

        if(maxWordsToAlign >= input.getNumOfWords()) {
            return wordIndexes;
        }

        Collections.shuffle(wordIndexes);
        wordIndexes = wordIndexes.subList(0, maxWordsToAlign);
        return wordIndexes;
    }




}
