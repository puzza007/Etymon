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
import etymology.viterbi.ViterbiMatrix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author sxhiltun
 */
public class ThreeDimBaselineImputator extends Imputator {

    public ThreeDimBaselineImputator(Input input) {
        super(input);
    }


    private Input input;
    private int firstLangId;
    private int secondLangId;
    private int thirdLangId;

    private int maxWordsToAlign;
    private boolean useSingleAlignmentIterationForImputation;
    private Alignator cachedAlignator;

    public ThreeDimBaselineImputator(Input input, int firstLangId, int secondLangId, int thirdLangId) {
        this(input, firstLangId, secondLangId, thirdLangId, input.getNumOfWords());
    }

    public ThreeDimBaselineImputator(Alignator alignator, Input input, int firstLangId, int secondLangId, int thirdLangId) {
        this(input, firstLangId, secondLangId, thirdLangId, input.getNumOfWords());
        this.cachedAlignator = alignator;
        useSingleAlignmentIterationForImputation = true;
    }

    public ThreeDimBaselineImputator(Input input, int firstLangId, int secondLangId, int thirdLangId, int maxWordsToAlign) {
        super(input);

        this.input = input;
        this.firstLangId = firstLangId;
        this.secondLangId = secondLangId;
        this.thirdLangId = thirdLangId;

        this.maxWordsToAlign = maxWordsToAlign;
        if(this.maxWordsToAlign > input.getNumOfWords()) {
            this.maxWordsToAlign = input.getNumOfWords();
        }

        setMaxWordsToAlign(this.maxWordsToAlign);
    }


    @Override
    public void imputeWords() throws Exception {

        FeatureVocabulary thirdFeatureVocabulary = (FeatureVocabulary)input.getVocabulary(thirdLangId);

        Logger finalLogger = cachedAlignator.getFinalLogger();
        logLoggerTitle(finalLogger);


        for (Integer wordIndex : getWordIndexes()) {


            String word1ToImputeFrom = input.getWord(firstLangId, wordIndex);
            String word2ToImputeFrom = input.getWord(secondLangId, wordIndex);
            String correctWordNumberThree = input.getWord(thirdLangId, wordIndex);

            //do not try to impute if third lang is missing (can't compute the difference)
            if (correctWordNumberThree == null) {
                continue;
            }

            Alignator alignator = getAlignator(wordIndex);

            String imputed = runNaive3DImputation(alignator, wordIndex, word1ToImputeFrom, word2ToImputeFrom);            

            logImputationResult(new String[]{word1ToImputeFrom, word2ToImputeFrom}, correctWordNumberThree, imputed);
            imputed = imputed.replaceAll("\\.", "");

            addLevenshteinDicreteDist(imputed, correctWordNumberThree);
            addLevenshteinFeatureWiseDist(imputed, correctWordNumberThree, thirdFeatureVocabulary);
            addCorrectWordsCharacterLength(correctWordNumberThree);


            if(correctWordNumberThree.equals(imputed)) {
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

    private String runNaive3DImputation(Alignator alignator, Integer wordIndex, String word1, String word2) throws RuntimeException, Exception {
        String imputed;
        WordAlignment wordAlignment = null;

        //deregister the word that is imputed if using the "cheat" - model
        if (useSingleAlignmentIterationForImputation) {
            wordAlignment = alignator.getAlignmentStorage().deregisterAlignment(alignator.getAlignmentMatrix(), wordIndex);
        }
        
        
        imputed = imputeSingleWord(alignator.getAlignmentMatrix(), wordIndex, word1, word2, thirdLangId);
        //System.out.println("Imputed: " + imputed);

        //reregister the word
        if (useSingleAlignmentIterationForImputation) {
            alignator.getAlignmentStorage().registerAlignment(alignator.getAlignmentMatrix(), wordAlignment, wordIndex);
        }

        return imputed;

        //remove the dots
        //return imputed.replaceAll("\\.", "");

    }
    
    private String imputeSingleWord(AlignmentMatrix matrix, int wordIndex, String word1, String word2, int thirdLangId) throws RuntimeException, Exception {
        //if third lang is missing - no chance - handled earlier already
        
        //System.out.println("w1: " + word1);
        //System.out.println("w2: " + word2);
        
        //if first or second lang is missing, do naive imputetation in 2D-way
        if (word1 == null) {
            return naiveImputeSingleWordIn2D(matrix, wordIndex, secondLangId);
        }
        if (word2 == null) {
            return naiveImputeSingleWordIn2D(matrix, wordIndex, firstLangId);
        }
        
        //if lang 1 and lang2 are present, use dynamic programming to align first two langs concurrently        
        return naiveImputeSingleWordIn3D(matrix, wordIndex);
                       
    }

    private String naiveImputeSingleWordIn3D(AlignmentMatrix matrix, int wordIndex) throws RuntimeException, Exception {
        
        ViterbiMatrix vm = new ViterbiMatrix(cachedAlignator);
        vm.init3DImputation(input, wordIndex);        
        WordAlignment alignment = new WordAlignment(vm);
        
        //String firstWord = input.getWordFromIndexes(firstLangId, alignment.get(firstLangId));
        //String secondWord = input.getWordFromIndexes(secondLangId, alignment.get(secondLangId));
        String imputedWord = input.getWordFromIndexes(thirdLangId, alignment.get(thirdLangId));
        
        //System.out.println(alignment.getStringPresentation(input));        
        log3DImputation(alignment);
        //imputedWord = imputedWord.replaceAll(" ", "");
        
        
        return imputedWord;
    }
    

    private String naiveImputeSingleWordIn2D(AlignmentMatrix matrix, int wordIndex, int imputeFromLangId) throws RuntimeException {

        List<Integer> imputedAlignment = new ArrayList();

        GlyphVocabulary imputeFromVoc = Input.getInstance().getVocabulary(imputeFromLangId);

        for(int imputeFromGlyphIdx: imputeFromVoc.getWordIndexes(wordIndex)) {

            //source glyph can not be dot
            if(imputeFromGlyphIdx == Constants.DOT_INDEX) {
                continue;
            }

            int imputeToGlyphIdx;                        
            if(imputeFromLangId == firstLangId) {
                imputeToGlyphIdx = matrix.getMostProbableGlyphAlignmentByIndex(imputeFromGlyphIdx, null, null, thirdLangId);
            } else { // assuming 1
                imputeToGlyphIdx = matrix.getMostProbableGlyphAlignmentByIndex(null, imputeFromGlyphIdx, null, thirdLangId);
            }

            imputedAlignment.add(imputeToGlyphIdx);
        }


        return input.getWordFromIndexes(thirdLangId, imputedAlignment);
    }



    @Override
    public void setUseSingleAlignmentIterationForImputation(boolean useSingleAlignmentIterationForImputation) {
        this.useSingleAlignmentIterationForImputation = useSingleAlignmentIterationForImputation;
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
