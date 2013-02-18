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
package etymology.align.matrices;

import etymology.align.*;
import etymology.config.Configuration;
import etymology.config.Constants;
import etymology.cost.CostFunction;
import etymology.cost.MultiGlyphCostFunction;
import etymology.cost.SuffixCostCalculator;
import etymology.cost.TwoPartCodeCostFunction;
import etymology.cost.TwoPartCodeCostNoKindsUniformPrior;
import etymology.cost.TwoPartCodeCostNoKindsWithSuffixes;
import etymology.input.Input;
import etymology.input.Tuple;
import etymology.util.EtyMath;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import etymology.align.WordAlignment;
import etymology.cost.*;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author arto
 */
public class TwoLangAlignmentMatrix implements ITwoLangAlignmentMatrix, AlignmentMatrix, PriorHolder {

    protected KindHolder kindHolder;
    protected int[][] alignmentCountMatrix;
    protected int totalAlignmentCounts;
    protected int totalAlignmentAlphas; // number of non-zero cells in whole matrix
    protected int l1SymbolCount;
    protected int l2SymbolCount;
    protected int l1LanguageIdx;
    protected int l2LanguageIdx;
    private String langOne;
    private String langTwo;
    private final int numOfWordsForThisMatrix;

    //suffix-stuff
    private SuffixCostCalculator suffixCostCalculator;
    private SuffixAlignmentMatrix sourceSuffixMatrix;
    private SuffixAlignmentMatrix targetSuffixMatrix;
    
    //debugging method
    public void setAlignmentCountMatrix(int[][] matrix) {
        alignmentCountMatrix = matrix;
    }

    public TwoLangAlignmentMatrix(Input input, Tuple<Integer, Integer> languageIdTuple) {
        this(input, languageIdTuple.getFirst(), languageIdTuple.getSecond());
    }

    public TwoLangAlignmentMatrix(Input input) {
        this(input, 0, 1);
    }

    public TwoLangAlignmentMatrix(Input input, int sourcelanguageIdx, int targetLanguageIdx) {
        this.l1LanguageIdx = sourcelanguageIdx;
        this.l2LanguageIdx = targetLanguageIdx;
        
        this.langOne = input.getVocabulary(l1LanguageIdx).getLanguage();
        this.langTwo = input.getVocabulary(l2LanguageIdx).getLanguage();
        
        //System.out.println(l1LanguageIdx + " :" + this.langOne + " " + l2LanguageIdx + ": " + this.langTwo);
        //count the number of word pairs between the two given languages
        int numOfWords = 0;
        for (int i = 0; i < input.getNumOfWords(); i++) {
            String l1Word = input.getVocabulary(l1LanguageIdx).getWord(i);
            String l2Word = input.getVocabulary(l2LanguageIdx).getWord(i);

            if (l1Word == null || l2Word == null) {
                continue;
            }

            numOfWords++;
        }

        this.numOfWordsForThisMatrix = numOfWords;

        init(input);

        
    }

    private void init(Input input) {
        int extraGlyphs = 0;
        if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
            extraGlyphs = 2;
        }
        if (Configuration.getInstance().isRemoveSuffixes()) {
            extraGlyphs = 2;
        }


        this.l1SymbolCount = extraGlyphs + 1 + input.getLengthOneGlyphCount(l1LanguageIdx);
        this.l2SymbolCount = extraGlyphs + 1 + input.getLengthOneGlyphCount(l2LanguageIdx);

        if (Configuration.getInstance().getMaxGlyphsToAlign() > 1) {            
            l1SymbolCount += ((extraGlyphs + input.getLengthOneGlyphCount(l1LanguageIdx)) * (extraGlyphs + input.getLengthOneGlyphCount(l1LanguageIdx)));
            l2SymbolCount += ((extraGlyphs + input.getLengthOneGlyphCount(l2LanguageIdx)) * (extraGlyphs + input.getLengthOneGlyphCount(l2LanguageIdx)));
        }

        alignmentCountMatrix = new int[l1SymbolCount][l2SymbolCount];
        totalAlignmentCounts = numOfWordsForThisMatrix;
        totalAlignmentAlphas = 1;

        kindHolder = new TwoLangKindHolder(input.getLengthOneGlyphCount(l1LanguageIdx), input.getLengthOneGlyphCount(l2LanguageIdx), l1LanguageIdx, l2LanguageIdx);

        if (Configuration.getInstance().isRemoveSuffixes()) {

            this.suffixCostCalculator = new SuffixCostCalculator(input);
            this.sourceSuffixMatrix = new SuffixAlignmentMatrix(l1SymbolCount, l1LanguageIdx);
            this.targetSuffixMatrix = new SuffixAlignmentMatrix(l2SymbolCount, l2LanguageIdx);
        }


    }

    public int getL1SymbolCount() {
        return l1SymbolCount;
    }

    public int getL2SymbolCount() {
        return l2SymbolCount;
    }

    public int getL1LangId() {
        return l1LanguageIdx;
    }

    public int getL2LangId() {
        return l2LanguageIdx;
    }

    public int getL3LangId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getPrior(int sourceSymbolIdx, int targetSymbolIdx) {
        return kindHolder.getKind(sourceSymbolIdx, targetSymbolIdx).getPrior();
    }

    public String getAlignedLanguages() {
        return langOne + "-" + langTwo;
    }

    public int[][] getAlignmentCountMatrix() {
        return alignmentCountMatrix;
    }

    public int getTotalAlignmentCounts() {
        return totalAlignmentCounts;
    }

    public int getTotalAlignmentAlphas() {
        return totalAlignmentAlphas;
    }

    @Override
    public void incrementSuffixes(List<Integer> sourceSuffix, List<Integer> targetSuffix) {
        int sourceSymbolIndex = Constants.SUFFIX_BOUNDARY_INDEX;
        int targetSymbolIndex = Constants.SUFFIX_BOUNDARY_INDEX;
        if (sourceSuffix == null) {
            sourceSymbolIndex = Constants.WORD_BOUNDARY_INDEX;
        }
        if (targetSuffix == null) {
            targetSymbolIndex = Constants.WORD_BOUNDARY_INDEX;
        }

        incrementAlignCount(sourceSymbolIndex, targetSymbolIndex);

        if (sourceSuffix != null) {
            for (int s : sourceSuffix) {
                sourceSuffixMatrix.incrementSymbolCount(s);
            }
            sourceSuffixMatrix.incrementSymbolCount(Constants.WORD_BOUNDARY_INDEX);
        }

        if (targetSuffix != null) {
            for (int t : targetSuffix) {
                targetSuffixMatrix.incrementSymbolCount(t);
            }
            targetSuffixMatrix.incrementSymbolCount(Constants.WORD_BOUNDARY_INDEX);
        }
        
    }


    public SuffixAlignmentMatrix getSuffixAlignmentMatrix(int languageId) {
        if (languageId == this.l1LanguageIdx) {
            return sourceSuffixMatrix;
        } else {
            return targetSuffixMatrix;
        }

    }

    public void incrementAlignCount(int sourceSymbolIndex, int targetSymbolIndex) {
        alignmentCountMatrix[sourceSymbolIndex][targetSymbolIndex]++;
        totalAlignmentCounts++;

        if (alignmentCountMatrix[sourceSymbolIndex][targetSymbolIndex] == 1) {
            Kind k = kindHolder.getKind(sourceSymbolIndex, targetSymbolIndex);

            totalAlignmentAlphas++;
            k.increaseNumOfNonZeroEvents();
        }
    }

    public void incrementAlignCount(WordAlignment wa) {
        for(int wordAlignmentPos = 0; wordAlignmentPos < wa.getAlignmentLength(); wordAlignmentPos++) {
            incrementAlignCount(wa.get(0).get(wordAlignmentPos), wa.get(1).get(wordAlignmentPos));
        }
    }

    public double[] incrementAlignCount(Integer... glyphIdx) {

        if(glyphIdx.length == 2) {            
            incrementAlignCount(glyphIdx[0], glyphIdx[1]);
        } else {
            incrementAlignCount(glyphIdx[0], glyphIdx[1], glyphIdx[2]);
        }
        
        return null;
    }

    public int getAlignmentCountAtIndex(int sourceGlyphIndex, int targetGlyphIndex) {        
        return alignmentCountMatrix[sourceGlyphIndex][targetGlyphIndex];
    }


    @Override
    public void decrementSuffixes(List<Integer> sourceSuffix, List<Integer> targetSuffix) {

        int sourceSymbolIndex = Constants.SUFFIX_BOUNDARY_INDEX;
        int targetSymbolIndex = Constants.SUFFIX_BOUNDARY_INDEX;

        if (sourceSuffix == null) {
            sourceSymbolIndex = Constants.WORD_BOUNDARY_INDEX;
        }
        if (targetSuffix == null) {
            targetSymbolIndex = Constants.WORD_BOUNDARY_INDEX;
        }

        //decrement -:#, #:- or #:# count from alignment matrix
        decrementAlignCount(sourceSymbolIndex, targetSymbolIndex);

        //decrement counts from suffix matrices.     
        //suffixes do not include word boundary symbols, decrement only if suffix exists
        if (sourceSuffix != null) {
            for (int s : sourceSuffix) {
                sourceSuffixMatrix.decrementSymbolCount(s);
            }
            sourceSuffixMatrix.decrementSymbolCount(Constants.WORD_BOUNDARY_INDEX);
        }

        if (targetSuffix != null) {
            for (int t : targetSuffix) {
               targetSuffixMatrix.decrementSymbolCount(t);
            }
            targetSuffixMatrix.decrementSymbolCount(Constants.WORD_BOUNDARY_INDEX);
        }
    }

    public void decrementAlignCount(int sourceSymbolIndex, int targetSymbolIndex) {
        alignmentCountMatrix[sourceSymbolIndex][targetSymbolIndex]--;
        totalAlignmentCounts--;

        if (alignmentCountMatrix[sourceSymbolIndex][targetSymbolIndex] == 0) {
            Kind k = kindHolder.getKind(sourceSymbolIndex, targetSymbolIndex);
            totalAlignmentAlphas--;
            k.decreaseNumOfNonZeroEvents();
        }
    }

    public int getNumberOfNonZeroAlignments() {
        return totalAlignmentAlphas;
    }

    public double getAlignmentCostByIndex(int sourceGlyphIndex, int targetGlyphIndex) {
        double p = getAlignmentProbabilityByIndex(sourceGlyphIndex, targetGlyphIndex);
        return -1.0 * EtyMath.base2Log(p);
    }

    public double getSuffixCost(List<Integer> suffix, int language) {

        if (suffix == null || suffix.isEmpty()) {
            return 0;
        }

        //suffix should include the word boundary symbol (index)

        switch (Configuration.getInstance().getSuffixCostType()) {
            case UNIFORM:
                return SuffixCostCalculator.getUniformSuffixCost(language, suffix);
            case UNIGRAM:
                return SuffixCostCalculator.getUnigramSuffixCost(language, suffix);
            case PREQUENTIAL:
                if (language == l1LanguageIdx) {
                    return sourceSuffixMatrix.getPrequentialSuffixCost(suffix);
                }else {
                    return targetSuffixMatrix.getPrequentialSuffixCost(suffix);
                }
            default:
                throw new RuntimeException("Unknown suffix cost type: " + Configuration.getInstance().getSuffixCostType());
        }
        
    }




    @Override
    public double getAlignmentProbabilityByIndex(int sourceGlyphIndex, int targetGlyphIndex) {
        double p = 0.0;
       // System.out.println(Configuration.getInstance().getCostFunctionIdentifier());
        if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE) {
            
              p =  getAlignmentProbabilityByIndexSeparateKinds( sourceGlyphIndex,  targetGlyphIndex); 
        } else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.BASELINE) {
            p = getAlignmentProbabilityByIndexBaseline(sourceGlyphIndex, targetGlyphIndex);
        }else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_NO_KINDS) {
            p = getAlignmentProbabilityByIndexCodebookNoKindsOrNotSeparateKinds(sourceGlyphIndex, targetGlyphIndex);
        }
        else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE) {
              p = getAlignmentProbabilityByIndexCodebookNoKindsOrNotSeparateKinds( sourceGlyphIndex,  targetGlyphIndex); 
        } else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE_NML) {
            try {
                //System.out.println("using CODEBOOK_WITH_KINDS_SEPARATE_NML");
                  p = getAlignmentProbabilityByIndexSeparateKindsNML(sourceGlyphIndex, targetGlyphIndex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TwoLangAlignmentMatrix.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML) {
            try {
                p = getAlignmentProbabilityByIndexCodebookNoKindsOrNotSeparateKindsNML( sourceGlyphIndex,  targetGlyphIndex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TwoLangAlignmentMatrix.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return p;
    }
    private double getAlignmentProbabilityByIndexBaseline(int sourceGlyphIndex, int targetGlyphIndex) {
        double p = 0;
        int indexAlignmentCount = getAlignmentCountAtIndex(sourceGlyphIndex, targetGlyphIndex);
        int totalSize = getAreaSize();
        int sumAligns = getTotalAlignmentCounts();
        p = (indexAlignmentCount +1.0) / (sumAligns + totalSize);
        
        return p;
    }
    private double getAlignmentProbabilityByIndexCodebookNoKindsOrNotSeparateKinds(int sourceGlyphIndex, int targetGlyphIndex) {
        //System.out.println("Alignment probability kinds not separate");
        //System.out.println("sg: " + sourceGlyphIndex + " tg: " + targetGlyphIndex);
        int indexAlignmentCount = getAlignmentCountAtIndex(sourceGlyphIndex, targetGlyphIndex);

        int sumAligns = getTotalAlignmentCounts();
        int sumAlignAlphas = getTotalAlignmentAlphas();

        int countSums = sumAligns;
        int numOfNonZeroEvents = sumAlignAlphas;

        if (indexAlignmentCount > 0) {
            if (Alignator.getCostHandler().getCostFunction() instanceof MultiGlyphCostFunction) {
                // if we use eq22 the cost for alignment changes
                MultiGlyphCostFunction mgcf = (MultiGlyphCostFunction) Alignator.getCostHandler().getCostFunction();
                return (1.0 * indexAlignmentCount + mgcf.getPrior(sourceGlyphIndex, targetGlyphIndex)) / (sumAligns + mgcf.getPriorSum());
            }
            //System.out.println("(1.0 + " + indexAlignmentCount + ") / ( " + sumAligns + " " + sumAlignAlphas + " )" + "number of words for this matrix: " + numOfWordsForThisMatrix);
            return (1.0 + indexAlignmentCount) / (sumAligns + sumAlignAlphas);
        }

        // if the cost function does not use kinds, do not utilize them
        CostFunction cf = Alignator.getCostHandler().getCostFunction();

        int kindSize = alignmentCountMatrix.length * alignmentCountMatrix[0].length; // N_k -- if no kinds, use whole area
        
        int kindNumOfNonZeroEvents = numOfNonZeroEvents; // M_k -- if no kinds use sum of all
        int sumOfNonZeroCountEvents = numOfNonZeroEvents; // |E| = sum(M_k) for all k

        if (cf instanceof TwoPartCodeCostFunction 
                && !(cf instanceof TwoPartCodeCostNoKindsUniformPrior)
                && !(cf instanceof TwoPartCodeCostNoKindsWithSuffixes)) {
            Kind k = kindHolder.getKind(sourceGlyphIndex, targetGlyphIndex);
            kindSize = k.getRegionSize();
            kindNumOfNonZeroEvents = k.getNumOfNonZeroEvents();
        }
        //if(cf instanceof TwoPartCodeCostNoKindsUniformPrior) {
        //    System.out.println("TwoPartCodeCostNoKindsUniformPrior");
        //}
        double value = 1.0 / (1.0 * countSums + sumOfNonZeroCountEvents);
        value *= ((1.0 * sumOfNonZeroCountEvents) / (countSums + sumOfNonZeroCountEvents + 1));
        value *= ((kindNumOfNonZeroEvents + 1.0) / (kindSize - kindNumOfNonZeroEvents));

        return value;
    }
    
    
    
    private double getAlignmentProbabilityByIndexSeparateKinds(int sourceGlyphIndex, int targetGlyphIndex) {
               // System.out.println("Alignment probability separate kinds");
        //System.out.println("sg: " + sourceGlyphIndex + " tg: " + targetGlyphIndex);
        int indexAlignmentCount = getAlignmentCountAtIndex(sourceGlyphIndex, targetGlyphIndex);

        int sumAligns = getTotalAlignmentCounts();
        int sumAlignAlphas = getTotalAlignmentAlphas();

        int countSums = sumAligns;
        int numOfNonZeroEvents = sumAlignAlphas;
        Kind k = kindHolder.getKind(sourceGlyphIndex, targetGlyphIndex);
        double p_k;
        int sumAlignsOfK;
        int numOfNonZeroEventsOfK = k.getNumOfNonZeroEvents();
            sumAlignsOfK = sumAlignsOfKind(k);
        p_k = (1.0 + sumAlignsOfK)/(sumAligns + 3);
        
        //e has been observed before, use L_K = 2..
        if (indexAlignmentCount > 0) {
            //need to change here later
            if (Alignator.getCostHandler().getCostFunction() instanceof MultiGlyphCostFunction) {
                // if we use eq22 the cost for alignment changes
                MultiGlyphCostFunction mgcf = (MultiGlyphCostFunction) Alignator.getCostHandler().getCostFunction();
                return (1.0 * indexAlignmentCount + mgcf.getPrior(sourceGlyphIndex, targetGlyphIndex)) / (sumAligns + mgcf.getPriorSum());
            }
            
            //calculate total number of counts in kind k 
            
            // debugging:
            //System.out.println("(1.0 +" + sumAlignsOfK+")/(" + sumAligns +" + 3)");
            //System.out.println( "(" + WordAlignment.getStringByGlyphIndex(sourceGlyphIndex,Input.getInstance().getVocabulary(0)) 
              //      +           ":" + WordAlignment.getStringByGlyphIndex(targetGlyphIndex,Input.getInstance().getVocabulary(1)) + ")"
                //    + p_k + " * "
                  //  + "(1.0 +" + indexAlignmentCount + " ) / (" + sumAlignsOfK + " + " + numOfNonZeroEventsOfK + ")" );
            return p_k*(1.0 + indexAlignmentCount) / (sumAlignsOfK + numOfNonZeroEventsOfK);
        }

        // if the cost function does not use kinds, do not utilize them
        CostFunction cf = Alignator.getCostHandler().getCostFunction();

        //int kindSize = alignmentCountMatrix.length * alignmentCountMatrix[0].length; // N_k -- if no kinds, use whole area
        int kindSize = getAreaSize();
        int kindNumOfNonZeroEvents = numOfNonZeroEvents; // M_k -- if no kinds use sum of all


        if (cf instanceof TwoPartCodeCostFunctionWithKindsSeparate
                && !(cf instanceof TwoPartCodeCostNoKindsUniformPrior)
                && !(cf instanceof TwoPartCodeCostNoKindsWithSuffixes)) {
          
            kindSize = k.getRegionSize();
            kindNumOfNonZeroEvents = k.getNumOfNonZeroEvents();
        }
        
        // cost of data given the codebook and kind:
        double value = kindNumOfNonZeroEvents;
        //System.out.println(value);
        value /= ( sumAlignsOfKind(k) + kindNumOfNonZeroEvents + 1.0)*(sumAlignsOfKind(k) + kindNumOfNonZeroEvents);
        
        // codebook:
        value *= ((kindNumOfNonZeroEvents + 1.0) / (kindSize - kindNumOfNonZeroEvents));

        // cost of kinds:
        value *= p_k;
 
        
        //System.out.println( "(" + WordAlignment.getStringByGlyphIndex(sourceGlyphIndex,Input.getInstance().getVocabulary(0)) 
        //    +           ":" + WordAlignment.getStringByGlyphIndex(targetGlyphIndex,Input.getInstance().getVocabulary(1)) + ")"
        //      + p_k +" * " + "( " + kindNumOfNonZeroEvents + "/ ([" + sumAlignsOfKind(k) + "+ " + kindNumOfNonZeroEvents + " + 1.0 ]" + "[ " + sumAlignsOfKind(k) 
        //   + " + " + kindNumOfNonZeroEvents+"]) * (" + kindNumOfNonZeroEvents + " + 1) / (" +  kindSize + " - " + kindNumOfNonZeroEvents + ")" 
        //      + "    --->>> value = " + value);
        return value;

    }
    
    private double getAlignmentProbabilityByIndexCodebookNoKindsOrNotSeparateKindsNML(int sourceGlyphIndex, int targetGlyphIndex) throws FileNotFoundException {
        //System.out.println("Alignment probability No separate kinds NML");
        int indexAlignmentCount = getAlignmentCountAtIndex(sourceGlyphIndex, targetGlyphIndex);
        int sumAligns = getTotalAlignmentCounts();
        Kind k = kindHolder.getKind(sourceGlyphIndex, targetGlyphIndex);    
        //e has been observed before
        if (indexAlignmentCount > 0) {
            //need to change here later
            if (Alignator.getCostHandler().getCostFunction() instanceof MultiGlyphCostFunction) {
                // if we use eq22 the cost for alignment changes
                MultiGlyphCostFunction mgcf = (MultiGlyphCostFunction) Alignator.getCostHandler().getCostFunction();
                return (1.0 * indexAlignmentCount + mgcf.getPrior(sourceGlyphIndex, targetGlyphIndex)) / (sumAligns + mgcf.getPriorSum());
            }
            

            double Delta_L = - (indexAlignmentCount + 1) * EtyMath.base2Log(indexAlignmentCount + 1) + indexAlignmentCount * EtyMath.base2Log(indexAlignmentCount)
                             + (sumAligns + 1) * EtyMath.base2Log(sumAligns + 1) - sumAligns * EtyMath.base2Log(sumAligns)
                             + EtyMath.logRegret(totalAlignmentAlphas, sumAligns + 1) - EtyMath.logRegret(totalAlignmentAlphas, sumAligns);
            
            
            //return the probability
            return Math.pow(2, -Delta_L);
        }
        //e has not been observed before
        
        
        // if the cost function does not use kinds, do not utilize them
        //CostFunction cf = Alignator.getCostHandler().getCostFunction();

        //int kindSize = alignmentCountMatrix.length * alignmentCountMatrix[0].length; // N_k -- if no kinds, use whole area
        //int kindSize = getAreaSize();
        //int kindNumOfNonZeroEvents = numOfNonZeroEvents; // M_k -- if no kinds use sum of all


        //if (cf instanceof TwoPartCodeCostFunctionWithKindsSeparate
        //        && !(cf instanceof TwoPartCodeCostNoKindsUniformPrior)
        //        && !(cf instanceof TwoPartCodeCostNoKindsWithSuffixes)) {
          
            int kindSize = k.getRegionSize();
            int kindNumOfNonZeroEvents = k.getNumOfNonZeroEvents();
        //}
        
        //calculate \Delta L(CB)
            double Delta_L_CB = - EtyMath.base2Log((kindNumOfNonZeroEvents + 1.0)/(kindSize - kindNumOfNonZeroEvents));
        
        //calculate \Delta L
            double Delta_L = Delta_L_CB
                    + (sumAligns + 1) * EtyMath.base2Log(sumAligns + 1) - sumAligns * EtyMath.base2Log(sumAligns)
                    + EtyMath.logRegret(totalAlignmentAlphas + 1, sumAligns + 1) - EtyMath.logRegret(totalAlignmentAlphas, sumAligns);
        
                    
            return Math.pow(2, -Delta_L);
    }
    
    private double getAlignmentProbabilityByIndexSeparateKindsNML(int sourceGlyphIndex, int targetGlyphIndex) throws FileNotFoundException {
        //System.out.println("Alignment probability separate kinds NML");
        int indexAlignmentCount = getAlignmentCountAtIndex(sourceGlyphIndex, targetGlyphIndex);
        int sumAligns = getTotalAlignmentCounts();
        Kind k = kindHolder.getKind(sourceGlyphIndex, targetGlyphIndex);
        double p_k;
        int sumAlignsOfK;

        sumAlignsOfK = sumAlignsOfKind(k);
        p_k = (1.0 + sumAlignsOfK)/(sumAligns + 3);
        
        //e has been observed before
        if (indexAlignmentCount > 0) {
            //need to change here later
            if (Alignator.getCostHandler().getCostFunction() instanceof MultiGlyphCostFunction) {
                // if we use eq22 the cost for alignment changes
                MultiGlyphCostFunction mgcf = (MultiGlyphCostFunction) Alignator.getCostHandler().getCostFunction();
                return (1.0 * indexAlignmentCount + mgcf.getPrior(sourceGlyphIndex, targetGlyphIndex)) / (sumAligns + mgcf.getPriorSum());
            }
            
            // calculate \Delta L(K)
            double Delta_L_K = - EtyMath.base2Log(p_k);
            
            double Delta_L = Delta_L_K - (indexAlignmentCount + 1) * EtyMath.base2Log(indexAlignmentCount + 1) + indexAlignmentCount * EtyMath.base2Log(indexAlignmentCount)
                             + (sumAlignsOfK + 1) * EtyMath.base2Log(sumAlignsOfK + 1) - sumAlignsOfK * EtyMath.base2Log(sumAlignsOfK)
                             + EtyMath.logRegret(k.getNumOfNonZeroEvents(), sumAlignsOfK + 1) - EtyMath.logRegret(k.getNumOfNonZeroEvents(), sumAlignsOfK);
            
            //debugging
            
           // System.out.println(Delta_L_K +"- ("+indexAlignmentCount+ "+ 1) *" + "EtyMath.base2Log(" + indexAlignmentCount + "+ 1+) +" +indexAlignmentCount +"*" + "EtyMath.base2Log(" + indexAlignmentCount+")"
             //                + "+ ("+sumAlignsOfK + "+ 1) *" + "EtyMath.base2Log(" + sumAlignsOfK +"+ 1) -" + sumAlignsOfK + "*" + "EtyMath.base2Log(" + sumAlignsOfK+")"
               //              + "+ EtyMath.logRegret(" + k.getNumOfNonZeroEvents() + "," + sumAlignsOfK  + "+ 1) -EtyMath.logRegret(" + k.getNumOfNonZeroEvents() + "," + sumAlignsOfK + ")");
            //System.out.println("Delta_L: " + Delta_L);
            //return the probability
            return Math.pow(2, -Delta_L);
        }
        
        
        //e has not been observed before
        
        
        // if the cost function does not use kinds, do not utilize them
        //CostFunction cf = Alignator.getCostHandler().getCostFunction();

        //int kindSize = alignmentCountMatrix.length * alignmentCountMatrix[0].length; // N_k -- if no kinds, use whole area
        //int kindSize = getAreaSize();
        //int kindNumOfNonZeroEvents = numOfNonZeroEvents; // M_k -- if no kinds use sum of all


        //if (cf instanceof TwoPartCodeCostFunctionWithKindsSeparate
        //        && !(cf instanceof TwoPartCodeCostNoKindsUniformPrior)
        //        && !(cf instanceof TwoPartCodeCostNoKindsWithSuffixes)) {
          
            int kindSize = k.getRegionSize();
            int kindNumOfNonZeroEvents = k.getNumOfNonZeroEvents();
        //}
        
        //calculate \Delta L(CB)
            double Delta_L_CB = - EtyMath.base2Log((kindNumOfNonZeroEvents + 1.0)/(kindSize - kindNumOfNonZeroEvents));
        //calculate \Delta L(K)
            double Delta_L_K = - EtyMath.base2Log(p_k);
        //calculate \Delta L
        //Two cases to be considered here: (1) Nk+ is not 0 (2) Nk+ is 0;
            double Delta_L = 0;
            if (k.getNumOfNonZeroEvents() > 0) {
            Delta_L = Delta_L_CB + Delta_L_K 
                    + (sumAlignsOfK + 1) * EtyMath.base2Log(sumAlignsOfK + 1) - sumAlignsOfK * EtyMath.base2Log(sumAlignsOfK)
                    + EtyMath.logRegret(k.getNumOfNonZeroEvents() + 1, sumAlignsOfK + 1) - EtyMath.logRegret(k.getNumOfNonZeroEvents(), sumAlignsOfK);
            } else {
                Delta_L = Delta_L_CB + Delta_L_K 
                    + (sumAlignsOfK + 1) * EtyMath.base2Log(sumAlignsOfK + 1) 
                    + EtyMath.logRegret(k.getNumOfNonZeroEvents() + 1, sumAlignsOfK + 1);
            } 
            
            
            //debugging
            //System.out.println(Delta_L_CB + "+" + Delta_L_K + 
                 //   "+ (" + sumAlignsOfK + " + 1) * EtyMath.base2Log(" + sumAlignsOfK + "+ 1) - " + sumAlignsOfK + " * EtyMath.base2Log(" + sumAlignsOfK + ")"
               //    + "+ EtyMath.logRegret(" + k.getNumOfNonZeroEvents() + " + 1, " + sumAlignsOfK + " + 1) - EtyMath.logRegret(" + k.getNumOfNonZeroEvents() + ", " + sumAlignsOfK + ")");
           // System.out.println("Delta_L: " + Delta_L);
                    
            return Math.pow(2, -Delta_L);
    }
     
    public int sumAlignsOfKind(Kind k) {
        int sumOfKind = 0;
        for(int i=0; i<alignmentCountMatrix.length; i++) {
                for (int j=0; j<alignmentCountMatrix[0].length; j++) {
                    if(k.inRegion(i, j)) {
                        sumOfKind += alignmentCountMatrix[i][j];
                    }
                }
            }
        return sumOfKind;
    }
    
    

    private int getAreaSize() {
        int size = 0;
        if (Configuration.getInstance().isRemoveSuffixes()) {
            size = (alignmentCountMatrix.length-2)* (alignmentCountMatrix[0].length-2); //(remove # and .)
            size += 3; // #:#, #:-, -:#, -:- (4 extra) ( .:. (minus 1 cell used for this) )
        } else {
            size = alignmentCountMatrix.length * alignmentCountMatrix[0].length; // N_k -- if no kinds, use whole area
        }
        return size;
    }

    public double[] incrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        incrementAlignCount(l1GlyphIdx, l2GlyphIdx);
        return null;
    }

    public double getAlignmentCountAtIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        return getAlignmentCountAtIndex(l1GlyphIdx, l2GlyphIdx);
    }

    public void decrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        decrementAlignCount(l1GlyphIdx, l2GlyphIdx);
    }

    public double getAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        return getAlignmentProbabilityByIndex(l1GlyphIdx, l2GlyphIdx);
    }

    public Kind getKind(int l1SymbolIdx, int l2SymbolIdx, int l3SymbolIdx) {
        return kindHolder.getKind(l1SymbolIdx, l2SymbolIdx, l3SymbolIdx);
    }

    public void decrementAlignByDeterminedCosts(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, double[] costs) {
        decrementAlignCount(l1GlyphIdx, l2GlyphIdx);
    }

    public int getL3SymbolCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resetCache() {
        // no cache to reset
    }

    @Override
    public int getMostProbableGlyphAlignmentByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx) {
        int highestIdx = -1;
        int highestCount = -1;

        if (l1GlyphIdx == null) {
            for (int l1Idx = 0; l1Idx < getL1SymbolCount(); l1Idx++) {
                int countAtIdx = getAlignmentCountAtIndex(l1Idx, l2GlyphIdx);
                if (countAtIdx > highestCount) {
                    highestIdx = l1Idx;
                    highestCount = countAtIdx;
                }
            }

            return highestIdx;
        }

        // else, l2GlyphIdx == null
        for (int l2Idx = 0; l2Idx < getL2SymbolCount(); l2Idx++) {
            int countAtIdx = getAlignmentCountAtIndex(l1GlyphIdx, l2Idx);
            if (countAtIdx > highestCount) {
                highestIdx = l2Idx;
                highestCount = countAtIdx;
            }
        }

        return highestIdx;
    }

    @Override
    public Kind getKind(int sourceSymbolIndex, int targetSymbolIndex) {
        return kindHolder.getKind(sourceSymbolIndex, targetSymbolIndex);
    }

    @Override
    public Collection<Kind> getAllKinds() {
        return kindHolder.getKinds();
    }

    /**
     *
     * @param l1GlyphIdx
     * @param l2GlyphIdx
     * @param l3GlyphIdx
     * @return
     */
    @Override
    public double getDotToDotAllowedAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *
     * @return
     */
    @Override
    public int getNumberOfWords() {
        return numOfWordsForThisMatrix;
    }

    public double getAlignmentCost(Map<Integer, Integer> languageIdToGlyphIndexes) {
        return getAlignmentCostByIndex(languageIdToGlyphIndexes.get(0), languageIdToGlyphIndexes.get(1));
    }

    public double getFeatureAlignmentCostByGlyphIndexes(Map<Integer, List<Integer>> languageIdToAlignmentPathUntilNow, Map<Integer, Integer> languageIdToGlyphIndexes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMostProbableGlyphAlignmentByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, int languageToImputeIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
