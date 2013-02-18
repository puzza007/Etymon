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
import etymology.align.WordAlignment;
import etymology.config.Configuration;
import etymology.input.FeatureVocabulary;
import etymology.input.Input;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author avihavai
 */
public class NxNViterbiImputator extends Imputator {


    private Input input;
        
    private int sourceLangId;
    private int targetLangId;

    private int maxWordsToAlign;

    private boolean useSingleAlignmentIterationForImputation;
    private Alignator cachedAlignator;

    public NxNViterbiImputator(Input input, int sourceLanguageId, int targetLanguageId) {
        this(input, sourceLanguageId, targetLanguageId, input.getNumOfWords());
    }

    public NxNViterbiImputator(Alignator alignator, Input input, int sourceLanguageId, int targetLanguageId)  {
        this(input, sourceLanguageId, targetLanguageId, input.getNumOfWords());
        this.cachedAlignator = alignator;
        useSingleAlignmentIterationForImputation = true;
    }

    public NxNViterbiImputator(Input input, int sourceLanguageId, int targetLanguageId, int maxWordsToAlign) {

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
    public void setUseSingleAlignmentIterationForImputation(boolean useSingleAlignmentIterationForImputation) {
        this.useSingleAlignmentIterationForImputation = useSingleAlignmentIterationForImputation;
    }

    public Alignator getCachedAlignator() {
        return this.cachedAlignator;
    }

    @Override
    public void imputeWords() throws Exception {

        FeatureVocabulary targetFeatureVocabulary = (FeatureVocabulary)input.getVocabulary(targetLangId);
        Logger finalLogger = cachedAlignator.getFinalLogger();
        logLoggerTitle(finalLogger);
        

        for (Integer wordIndex : getWordIndexes()) {

            Alignator alignator = getAlignator(wordIndex);
            
            String wordToImputeFrom = input.getWord(sourceLangId, wordIndex);
            String correctWord = input.getWord(targetLangId, wordIndex);
            String imputed = runNxNimputation(alignator, wordIndex, wordToImputeFrom);
            

            if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
                wordToImputeFrom = wordToImputeFrom.replace("^", "");
                wordToImputeFrom = wordToImputeFrom.replace("$", "");
                correctWord = correctWord.replace("^", "");
                correctWord = correctWord.replace("$", "");
                imputed = imputed.replace("^", "");
                imputed = imputed.replace("$", "");
            }

            
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

    private String runNxNimputation(Alignator alignator, Integer wordIndex, String wordToImputeFrom) {
        String imputed;
        WordAlignment wordAlignment = null;

        //deregister the word that is imputed if using the cheating model
        if (useSingleAlignmentIterationForImputation) {
            wordAlignment = alignator.getAlignmentStorage().deregisterAlignment(alignator.getAlignmentMatrix(), wordIndex);
        }

        List<String> targetGlyphs = input.getVocabulary(targetLangId).getGlyphs();

        ForwardViterbi fv = new ForwardViterbi(alignator.getAlignmentMatrix(), input, targetGlyphs, sourceLangId, targetLangId);
        imputed = fv.imputeWord(wordToImputeFrom);

        //reregister the word
        if (useSingleAlignmentIterationForImputation) {
            alignator.getAlignmentStorage().registerAlignment(alignator.getAlignmentMatrix(), wordAlignment, wordIndex);
        }

        return imputed;
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
