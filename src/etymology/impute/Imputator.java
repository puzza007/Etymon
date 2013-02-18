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

import etymology.align.WordAlignment;
import etymology.config.Configuration;
import etymology.input.FeatureVocabulary;
import etymology.input.Input;
import etymology.util.StringUtils;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sxhiltun
 */
public abstract class Imputator {

    private double levenshteinDiscreteSummedUp;
    private double levenshteinFeatureWiseSummedUp;
    private double correctWordsCharacterLength;
    private int exactMatches;
    private int maxWordsToAlign;
    private Input input;

    private static Logger finalLogger;

    private FeatureVocabulary sourceFv, targetFv;

    public Imputator(Input input) {
        levenshteinDiscreteSummedUp = 0;
        levenshteinFeatureWiseSummedUp = 0;
        correctWordsCharacterLength = 0;

        exactMatches = 0;
        maxWordsToAlign = 0;
        this.input = input;

        this.sourceFv = (FeatureVocabulary)input.getVocabulary(0);
        this.targetFv = (FeatureVocabulary)input.getVocabulary(1);
    }

    protected double addLevenshteinDicreteDist(String imputed, String correctWord) throws Exception {
        double dist = StringUtils.getLevenshteinDistance(imputed, correctWord);
        levenshteinDiscreteSummedUp += dist;

        return dist;
    }

    protected double addLevenshteinFeatureWiseDist(String imputed, String correctWord, FeatureVocabulary targetFeatureVocabulary) throws Exception {
        double dist = StringUtils.getLevenshteinDistance(imputed, correctWord, targetFeatureVocabulary, targetFeatureVocabulary);
        levenshteinFeatureWiseSummedUp += dist;

        return dist;
    }

    protected double addCorrectWordsCharacterLength(String correctWord) {
        double length = StringUtils.getGlyphComboLength(correctWord);
        correctWordsCharacterLength += length;

        return length;
    }

    protected void addExactMatch() {
        exactMatches += 1;
    }

    protected void setMaxWordsToAlign(int maxWordsToAlign) {
        this.maxWordsToAlign = maxWordsToAlign;
    }

    protected void logLoggerTitle(Logger logger) {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }

        finalLogger = logger;
        finalLogger.fine("IMPUTATION: \n");
        //finalLogger.log(Level.FINE, "{0}\t{1}\t{2}", new Object[]{"source word", "target word", "imputed word"});
    }

    protected void logCompleteLoggerTitle(Logger logger) {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }

        finalLogger = logger;
        finalLogger.fine("IMPUTATION: \n");
        finalLogger.log(Level.FINE, "{0}\t{1}\t{2}\t\t{3}\t{4}\t{5}", new Object[]{"source word", "target word", "cost", "source word", "imputed target", "cost"});
    }

    protected void logImputationResult(String[] wordsToImputeFrom, String correctWord, String imputed) throws Exception {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }

        //double sourceEdit = StringUtils.getLevenshteinDistance(wordToImputeFrom.replaceAll("\\.", ""), correctWord.replaceAll("\\.", ""), sourceFv, targetFv);
        //double targetEdit = StringUtils.getLevenshteinDistance(imputed.replaceAll("\\.", ""), correctWord.replaceAll("\\.", ""), targetFv, targetFv);
        //sourceToTargetFeatureWiseEditDistance += sourceEdit;

        finalLogger.log(Level.FINE, "{0}\t\t{1}\t\t{2}\t\t{3}", new Object[]{wordsToImputeFrom[0], wordsToImputeFrom[1], correctWord, imputed});
        finalLogger.log(Level.FINE, "\n");
    }

    protected void log3DImputation(WordAlignment alignment) {        
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }
        finalLogger.log(Level.FINE, alignment.getStringPresentation(input));
    }
    
    protected void logImputationResult(String wordToImputeFrom, String correctWord, String imputed) throws Exception {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }

        //double sourceEdit = StringUtils.getLevenshteinDistance(wordToImputeFrom.replaceAll("\\.", ""), correctWord.replaceAll("\\.", ""), sourceFv, targetFv);
        //double targetEdit = StringUtils.getLevenshteinDistance(imputed.replaceAll("\\.", ""), correctWord.replaceAll("\\.", ""), targetFv, targetFv);
        //sourceToTargetFeatureWiseEditDistance += sourceEdit;

        finalLogger.log(Level.FINE, "{0}\t\t{2}\t\t{1}", new Object[]{wordToImputeFrom, correctWord, imputed});
    }

    protected void logCompleteImputationResult(String realSource, String realTarget, String imputationSource, String imputationTarget, double realCost, double imputedCost, String message) throws Exception {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }

        DecimalFormat twoPlaces = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        String[] words = new String[]{realSource, realTarget, twoPlaces.format(realCost), imputationSource, imputationTarget, twoPlaces.format(imputedCost), message};
        for (int i=0; i<words.length-1; i++) {
            String formattedString = String.format("%-15s", words[i]);
            words[i] = formattedString;
        }


        finalLogger.log(Level.FINE, "{0}{1}{2}\t{3}{4}{5}{6}", words);
    }

    protected void logImputationSummary() {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }
        finalLogger.log(Level.FINE, "Normalized edit distance: {0}", getLevenshteinDistanceOfImputation());
        finalLogger.log(Level.FINE, "Normalized featurewise edit distance: {0}", getFeatureWiseLevenshteinDistanceOfImputation());
        //finalLogger.log(Level.FINE, "Normalized featurewise edit distance from source language: {0}", getFeatureWiseLevenshteinDistanceFromSourceLangToTargetLang());
        finalLogger.log(Level.FINE, "Accuracy on word level: {0}", getAccuracyOnWordLevel());

    }



    public abstract void imputeWords() throws Exception;
    public abstract void setUseSingleAlignmentIterationForImputation(boolean useSingleAlignmentIterationForImputation);


    public double getLevenshteinDistanceOfImputation() {
        double dist = 1.0 * levenshteinDiscreteSummedUp / correctWordsCharacterLength;
        return dist;
    }


    public double getAccuracyOnWordLevel() {
        return (1.0 * exactMatches / maxWordsToAlign);
    }



    public double getFeatureWiseLevenshteinDistanceOfImputation() {
        double dist = 1.0 * levenshteinFeatureWiseSummedUp / correctWordsCharacterLength;
        return dist;
    }




}
