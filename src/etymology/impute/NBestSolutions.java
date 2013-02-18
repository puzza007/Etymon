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
import etymology.context.FeatureAlignmentMatrix;
import etymology.context.FeatureTreeContainer;
import etymology.input.FeatureVocabulary;
import etymology.input.Input;
import etymology.util.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author sxhiltun
 */
public class NBestSolutions {


    private Alignator alignator;
    private Input input;
    private FeatureAlignmentMatrix fam;

    private int sourceLanguageId;
    private int targetLanguageId;
    private int nbest;

    private Imputator imputator;

    public NBestSolutions(Alignator alignator, Input input, int sourceLanguageId, int targetLanguageId, int nbest, Imputator imputator) {
        
        this.input = input;
        this.alignator = alignator;
        this.fam = alignator.getFeatureAlignmentMatrix();

        this.nbest = nbest;
        this.sourceLanguageId = sourceLanguageId;
        this.targetLanguageId = targetLanguageId;

        this.imputator = imputator;
    }

    public void compareCosts(int wordIndex, List<Integer> sourceSuggestion, List<Integer> targetSuggestion) throws Exception {
        //save the alignment used
        WordAlignment alignmentToReRegister = alignator.getAlignmentStorage().getAlignment(wordIndex);

        //remove the aligment
        alignator.getAlignmentStorage().deregisterFeatureAlignment(fam, wordIndex);
        fam.setPrintCosts(true);


        // compute costs:
        //imputation
//        AlignmentNode bestImputationAlignment = getBestImputedAlignment(wordIndex);
//        computeAndPrintAlignmentCost(bestImputationAlignment, "Imputation Cost");

        //the lowest cost alignment
        System.out.println("Best Viterbi Cost");
        AlignmentNode realAlignment = getRealAlignment(alignmentToReRegister);
        computeAndPrintAlignmentCost(realAlignment, "");

        
        //the cost of given alignment
        System.out.println("Cost of given alignment");
        AlignmentNode givenAlignment = getGivenAlignment(sourceSuggestion, targetSuggestion);
        computeAndPrintAlignmentCost(givenAlignment, "Cost of given alignment");


        //reregister the alignment
        alignator.getAlignmentStorage().registerFeatureAlignment(fam, alignmentToReRegister, wordIndex);


    }

    private void computeAndPrintAlignmentCost(AlignmentNode bestAlignment, String message) {

        List<Integer> bestSource = bestAlignment.getSourceIndexes();
        List<Integer> bestTarget = bestAlignment.getTargetIndexes();
        String sourceword = getStringPresentationOfSourceWord(bestSource);
        String targetword = getStringPresentationOfImputedWord(bestTarget);
        
        System.out.println("source: " + sourceword);
        System.out.println("target: " + targetword);
        System.out.println("total cost: " + bestAlignment.getCost());
        System.out.println("");
    }

    public String imputeWord(int wordIndex) throws Exception {

        //save the alignment used
        WordAlignment alignmentToReRegister = alignator.getAlignmentStorage().getAlignment(wordIndex);
        //save the trees 
        FeatureTreeContainer origContainer = alignator.getFeatureAlignmentMatrix().getFeatureTreeContainer();
        //remove the aligment and rebuild the trees
        alignator.getAlignmentStorage().deregisterFeatureAlignment(fam, wordIndex);

        // compute costs:
        //imputation
        AlignmentNode bestImputationAlignment = getBestImputedAlignment(wordIndex);
        //the lowest cost alignment
        AlignmentNode realAlignment = getRealAlignment(alignmentToReRegister);


        //the alignment obtained by imputation
        List<Integer> bestSource = bestImputationAlignment.getSourceIndexes();
        List<Integer> bestTarget = bestImputationAlignment.getTargetIndexes();

        //the alignment obtained by the model
        List<Integer> bestRealSource = realAlignment.getSourceIndexes();
        List<Integer> bestRealTarget = realAlignment.getTargetIndexes();

        String sourceword = getStringPresentationOfSourceWord(bestSource);
        String imputed = getStringPresentationOfImputedWord(bestTarget);

        String realSource = getStringPresentationOfSourceWord(bestRealSource);
        String realTarget = getStringPresentationOfImputedWord(bestRealTarget);
        

        double realCost = realAlignment.getCost();
        double imputCost = bestImputationAlignment.getCost();
        String message = "";

        if (realCost < imputCost) {
            message = "Imputation did not work!!!";
        }
        //imputator.logImputationResult(sourceword, input.getVocabulary(targetLanguageId).getWord(wordIndex), imputed);
        imputator.logCompleteImputationResult(realSource, realTarget, sourceword, imputed, realCost, imputCost, message);


        //reregister the alignment and replace the trees back
       // alignator.getAlignmentStorage().registerAlignment(fam, alignmentToReRegister, wordIndex);
        alignator.getAlignmentStorage().registerFeatureAlignmentAndRestoreTrees(fam, alignmentToReRegister, wordIndex, origContainer);


        //return imputed;
        String word = imputed;
        return word;
    }


    private AlignmentNode getBestImputedAlignment(int wordIndex) throws Exception {
             
        List<AlignmentNode> nBestParentAlignments = new ArrayList<AlignmentNode>();
        nBestParentAlignments.add(null);

        List<Integer> sourceWord = new ArrayList(input.getWordIndexes(sourceLanguageId, wordIndex));
        //add word boundary to the end of suorce word
        sourceWord.add(input.getVocabulary(sourceLanguageId).getGlyphIndex("#"));


        int dot = 0;
        //System.out.println("sourceWord: " + sourceWord);
        for (int position=0; position<sourceWord.size(); position++) {
            int i = sourceWord.get(position);

            //for (int i : sourceWord) {

            List<Integer> endOfWordIfDot = sourceWord.subList(position, sourceWord.size());
            List<Integer> endOfWord = sourceWord.subList(position+1, sourceWord.size());

            
            // getNBestAligments(List<AlignmentNode> parentAlignments, int newGlyphIndex, int glyphPosition, List<Integer> endOfWord) throws Exception {

            //add dot, same set of parents
            List<AlignmentNode> dot1Added = getNBestAligments(nBestParentAlignments, dot,  endOfWordIfDot);
            List<AlignmentNode> dot2Added = getNBestAligments(dot1Added, dot,  endOfWordIfDot);
            List<AlignmentNode> dot3Added = getNBestAligments(dot2Added, dot,  endOfWordIfDot);

            //add glyph i directly
            List<AlignmentNode> glyphAdded = getNBestAligments(nBestParentAlignments, i,  endOfWord);
            List<AlignmentNode> dot1GlyphAdded = getNBestAligments(dot1Added, i,  endOfWord);
            List<AlignmentNode> dot2GlyphAdded = getNBestAligments(dot2Added, i,  endOfWord);
            List<AlignmentNode> dot3GlyphAdded = getNBestAligments(dot3Added, i,  endOfWord);

            nBestParentAlignments = new ArrayList<AlignmentNode>();
            nBestParentAlignments.addAll(glyphAdded);
            nBestParentAlignments.addAll(dot1GlyphAdded);
            nBestParentAlignments.addAll(dot2GlyphAdded);
            nBestParentAlignments.addAll(dot3GlyphAdded);

            Collections.sort(nBestParentAlignments);

            if (nBestParentAlignments.size() >= nbest) {
                nBestParentAlignments = nBestParentAlignments.subList(0, nbest);
            }
        }

        return nBestParentAlignments.get(0);       
    }





    private String getStringPresentationOfImputedWord(List<Integer> bestAlignment) {
        
        StringBuilder sb = new StringBuilder();
        for (int i : bestAlignment.subList(0, bestAlignment.size()-1)) {
            //System.out.println("i: " + i);
            sb.append(input.getVocabulary(targetLanguageId).getGlyph(i));
        }
        //String word = sb.toString().replaceAll("\\.", "");
        //return word;
        return sb.toString();
    }

    private String getStringPresentationOfSourceWord(List<Integer> bestAlignment) {

        StringBuilder sb = new StringBuilder();
        for (int i : bestAlignment.subList(0, bestAlignment.size()-1)) {
            sb.append(input.getVocabulary(sourceLanguageId).getGlyph(i));
        }

        return sb.toString();
    }

    private AlignmentNode getRealAlignment(WordAlignment alignment) throws Exception {

        return getGivenAlignment(alignment.get(sourceLanguageId), alignment.get(targetLanguageId));

    }

    private AlignmentNode getGivenAlignment(List<Integer> givenSourceWord, List<Integer> givenTargetWord) throws Exception {
        List<List<Double>> sourceCosts = new ArrayList<List<Double>>();
        List<List<Double>> targetCosts = new ArrayList<List<Double>>();

        List<Integer> sourceWord = new ArrayList(givenSourceWord);
        sourceWord.add(input.getVocabulary(sourceLanguageId).getGlyphIndex("#"));

        List<Integer> targetWord = new ArrayList(givenTargetWord);
        targetWord.add(input.getVocabulary(targetLanguageId).getGlyphIndex("#"));

        AlignmentNode parentNode = null;
        for (int i=0; i<sourceWord.size(); i++) {
            AlignmentNode newAlignmentNode = new AlignmentNode(parentNode, sourceWord.get(i), targetWord.get(i));

            double cost = fam.getAlignmentCostByIndex(newAlignmentNode.getAlignment(), i, i);
            sourceCosts.add(fam.getSourceCosts());
            targetCosts.add(fam.getTargetCosts());

            newAlignmentNode.setCost(cost);
            parentNode = newAlignmentNode;
        }
//
//        System.out.println("source: " + sourceCosts);
//        System.out.println("target: " + targetCosts);

        return parentNode;

    }

    private List<AlignmentNode> getNBestAligments(List<AlignmentNode> parentAlignments, int newGlyphIndex, List<Integer> endOfWord) throws Exception {

        FeatureVocabulary targetVocabulary = (FeatureVocabulary) input.getVocabulary(targetLanguageId);
        int sourceWordBoundaryIndex = input.getVocabulary(sourceLanguageId).getGlyphIndex("#");
        int targetWordBoundaryIndex = input.getVocabulary(targetLanguageId).getGlyphIndex("#");
        
        List<AlignmentNode> allCandidateAligments = new ArrayList<AlignmentNode>();

        int[] sourceGlyphPossibilities = {newGlyphIndex};

        List<List<Integer>> completeAlignment;

        for (int sourceGlyphIndex : sourceGlyphPossibilities) {

            for (int targetGlyphIndex = 0; targetGlyphIndex < targetVocabulary.getFeatures().size(); targetGlyphIndex++) {

                

                //dot to dot not allowed
                if ((sourceGlyphIndex == 0) && (targetGlyphIndex == 0)) {
                    continue;
                }

                //word boundary only against word boundary
                if ((sourceGlyphIndex == sourceWordBoundaryIndex) ^ (targetGlyphIndex == targetWordBoundaryIndex)) {
                    continue;
                }

                //go through all possible paths this far
                for (AlignmentNode parentNode : parentAlignments) {
                    completeAlignment = new ArrayList<List<Integer>>();
                    completeAlignment.add(new ArrayList<Integer>());
                    completeAlignment.add(new ArrayList<Integer>());

                    //what if you add the s, t combination to this parent
                    AlignmentNode newAlignmentNode = new AlignmentNode(parentNode, sourceGlyphIndex, targetGlyphIndex);
                    int glyphPosition = newAlignmentNode.getAlignment().get(0).size()-1;

                    completeAlignment.get(0).addAll(newAlignmentNode.getAlignment().get(0));
                    completeAlignment.get(1).addAll(newAlignmentNode.getAlignment().get(1));

                    //adding the end of word saves the opportunity to look forward if the model allows that
                    completeAlignment.get(0).addAll(endOfWord);
                    

                    double cost = fam.getAlignmentCostByIndex(completeAlignment, glyphPosition, glyphPosition);
                    newAlignmentNode.setCost(cost);
                    allCandidateAligments.add(newAlignmentNode);
                }                
            }
        }


        Collections.sort(allCandidateAligments);


        if (nbest <= allCandidateAligments.size()) {
            return allCandidateAligments.subList(0, nbest);
        }
        else {
            return allCandidateAligments;
        }
                
    }
}

class AlignmentNode implements Comparable {

    private AlignmentNode parent;
    private List<List<Integer>> alignment;
    private double cost;


    public AlignmentNode(AlignmentNode parent, int sourceIndex, int targetIndex) {


       this.parent = parent;
       this.cost = 0;
       this.alignment = new ArrayList<List<Integer>>();
       if (parent == null) {
           this.alignment.add(new ArrayList());
           this.alignment.add(new ArrayList());
       }
       else {
           this.alignment.add(new ArrayList(parent.getAlignment().get(0)));
           this.alignment.add(new ArrayList(parent.getAlignment().get(1)));
       }
              
       this.alignment.get(0).add(sourceIndex);
       this.alignment.get(1).add(targetIndex);

    }


    public List<List<Integer>> getAlignment() {
        return alignment;
    }

    public void setCost(double cost) {
        if (parent != null) {
            this.cost = cost + parent.getCost();
        } else {
            this.cost = cost;
        }
    }

    public double getCost() {
        return cost;
    }

    public List<Integer> getTargetIndexes() {
        return this.alignment.get(1).subList(0, alignment.get(1).size());
    }

    public List<Integer> getSourceIndexes() {
        return this.alignment.get(0).subList(0, alignment.get(0).size());
    }

    public AlignmentNode getParentNode() {
        return parent;
    }

    @Override
    public int compareTo(Object another) {

        if (!(another instanceof AlignmentNode)) {
            throw new ClassCastException("An AlignmentNode object expected.");
        }
    double anotherCost = ((AlignmentNode) another).getCost();

    if (this.cost > anotherCost) {
        return 1;
    }
    if (this.cost < anotherCost) {
        return -1;
    }
    else {
        return 0;
    }
   

    }


    public String toString() {
        return cost + " " + alignment.get(1).toString();
    }




    
}

