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

package etymology.impute;

import etymology.config.Configuration;
import etymology.cost.AlignmentCostFunction;
import etymology.input.GlyphVocabulary;
import etymology.input.Input;
import etymology.util.EtyMath;
import etymology.util.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ForwardViterbi {
    private AlignmentCostFunction alignmentCostFunction;
    private Input input;
    private Collection<String> states;
    private int maxGlyphLength;

    private int sourceLanguageId;
    private int targetLanguageId;

    public ForwardViterbi(AlignmentCostFunction alignmentCostFunction, Input input, Collection<String> states, int sourceLanguageId, int targetLanguageId) {
        this.alignmentCostFunction = alignmentCostFunction;
        this.states = states;
        this.input = input;
        
        this.maxGlyphLength = Configuration.getInstance().getMaxGlyphsToAlign();

        this.sourceLanguageId = sourceLanguageId;
        this.targetLanguageId = targetLanguageId;
    }

    public String imputeWord(String observedWord) {


        ViterbiNode imputedPath = runViterbi(observedWord);
//        try {
            return getImputedWord(imputedPath);//.replaceAll("\\.", "");
//        }catch(Exception e) {
//            System.out.println("observed: " + observedWord);
//
//        }
//        return null;
        
    }

    private ViterbiNode runViterbi(String observedWord) {

        // indexBasedTransitionMap for observed word 'sieni'
        // {0=[s, si], 1=[i, ie], 2=[e, en], 3=[n, ni], 4=[i]}
        Map<Integer, List<String>> indexBasedTransitionMap = getIndexBasedTransitionMap(observedWord);

        Map<Integer, ViterbiNode> pathWaveFront = new HashMap();
        int maxState = StringUtils.getGlyphComboLength(observedWord);

        for (int ind = 0; ind < maxState; ind++) {
            if (pathWaveFront.containsKey(ind - 1)) {
                pathWaveFront.remove(ind - 1); // past fronts can be removed
            }

            
            for (String state : indexBasedTransitionMap.get(ind)) {
              
                ViterbiNode bestPathNode = getBestCostNode(states, state, pathWaveFront.get(ind));
                if (bestPathNode == null ) {
                    continue;
                }
                
                if(pathWaveFront.containsKey(bestPathNode.getLength())) {
                     if(pathWaveFront.get(bestPathNode.getLength()).getCost() > bestPathNode.getCost()) {
                         pathWaveFront.put(bestPathNode.getLength(), bestPathNode);
                     }
                } else {
                    pathWaveFront.put(bestPathNode.getLength(), bestPathNode);
                }
            }

//            System.out.println("ind: " + ind);
//            System.out.println("best: " + pathWaveFront);
        }
        //System.out.println("pathWaveFront: " + pathWaveFront.toString());
        //System.out.println("observed: " + observedWord);

        if (pathWaveFront.containsKey(maxState)) {
            //System.out.println("thisfar1: " + getImputedWord(pathWaveFront.get(maxState)));
            return pathWaveFront.get(maxState);
        }
        //System.out.println("thisfar2: " + getImputedWord(pathWaveFront.get(maxState-1)));
        return pathWaveFront.get(maxState-1);
    }


    private static String combineStrings(List<String> stringList) {
        StringBuilder sb = new StringBuilder();
        for (String s : stringList) {
            sb.append(s);
        }

        return sb.toString();
    }

    private static String getImputedWord(ViterbiNode node) {
        if (node == null) {
            System.out.println("null node");
        }
        String word = node.getObservation();
        while(node.getParent() != null) {
            node = node.getParent();
            word = node.getObservation() + word; // " -> " + word;
        }

        //return word.replaceAll("\\.", "");
        return word;
    }

    private Map<Integer, List<String>> getIndexBasedTransitionMap(String sourceWord) {
        //System.out.println("sourceWord: " + sourceWord);
        List<String> glyphs = StringUtils.splitToGlyphs(sourceWord);
        Map<Integer, List<String>> glyphIndexToPossibleStates = new TreeMap<Integer, List<String>>();

        for (int glyphStartIdx = 0; glyphStartIdx < glyphs.size(); glyphStartIdx++) {
            glyphIndexToPossibleStates.put(glyphStartIdx, getPossibleStates(glyphs, glyphStartIdx));
        }

        //System.out.println("glyphIndex: " + glyphIndexToPossibleStates.toString());
        return glyphIndexToPossibleStates;
    }

    private List<String> getPossibleStates(List<String> glyphs, int glyphStartIdx) {
        List<String> possibleStates = new ArrayList();

        for (int glyphLen = 1; glyphLen <= maxGlyphLength; glyphLen++) {
            if(glyphStartIdx + glyphLen > glyphs.size()) {
                break;
            }
            
            String state = combineStrings(glyphs.subList(glyphStartIdx, glyphStartIdx + glyphLen));


            possibleStates.add(state);
        }

        return possibleStates;
    }
    
    private ViterbiNode getBestCostNode(Collection<String> possibleObservations, String state, ViterbiNode parent) {
        double bestCost = Double.POSITIVE_INFINITY;
        String bestCostObs = null;

        Map<Integer, Integer> languageIdToGlyphIdMap = new HashMap();
        languageIdToGlyphIdMap.put(sourceLanguageId, input.getVocabulary(sourceLanguageId).getGlyphIndex(state));
        GlyphVocabulary targetVoc = input.getVocabulary(targetLanguageId);

        for (String observation : possibleObservations) {
            

            if (isThisImpossibleCombination(state, observation, parent)) {
                continue;
            }
         


            // get observation probability when compared to state       
            int targetGlyphIdx = targetVoc.getGlyphIndex(observation);
            languageIdToGlyphIdMap.put(targetLanguageId, targetGlyphIdx);

//            System.out.println("observation: " + observation);
//            System.out.println("languageIdToGlyphIdMap" + languageIdToGlyphIdMap.toString());

            double cost;
            cost = alignmentCostFunction.getAlignmentCost(languageIdToGlyphIdMap);


            if (bestCostObs == null || bestCost > cost) {
                bestCost = cost;
                bestCostObs = observation;
            }
        }

        double parentCost = 0;
        if(parent != null) {
            parentCost = parent.getCost();
        }

        if (bestCostObs != null) {
            return new ViterbiNode(bestCostObs, parentCost + bestCost, parent);
        }


//        System.out.println("parent: " + parent);
//        System.out.println("state: " + state);
        return null;
    }

    private boolean isThisImpossibleCombination(String sourceGlyph, String targetGlyph, ViterbiNode parent) {

        if (!Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
            return false;
        }

        if (sourceGlyph.contains("^") ^ targetGlyph.contains("^")) {
            return true;
        }

        if (sourceGlyph.contains("$") ^ targetGlyph.contains("$")) {
            return true;
        }



        if (parent != null && parent.getObservation().contains("$") && targetGlyph.contains("$")) {
            return true;
        }


        return false;
    }
}

class ViterbiNode {
    private int length;
    private double cost;
    private String observation;
    
    private ViterbiNode parent;

    public ViterbiNode(String observation, double cost, ViterbiNode parent) {
        this.cost = cost;
        this.observation = observation;
        this.parent = parent;

        
        this.length = StringUtils.getGlyphComboLength(observation);
        if (parent != null) {
            this.length += parent.getLength();
        }
    }

    public int getLengthIndex() {
        return length-1;
    }

    public int getLength() {
        return length;
    }

    public double getCost() {
        return cost;
    }

    public String getObservation() {
        return observation;
    }

    public ViterbiNode getParent() {
        return parent;
    }

    public String toString() {
        return this.observation;
    }
}
