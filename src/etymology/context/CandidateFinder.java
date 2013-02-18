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

package etymology.context;

import etymology.config.Configuration;
import etymology.context.FeatureTreeContainer.BabyTreeType;
import etymology.context.FeatureTreeContainer.Context;
import etymology.context.FeatureTreeContainer.Features;
import etymology.context.FeatureTreeContainer.Level;
import etymology.context.FeatureTreeContainer.TreeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sxhiltun
 */
public class CandidateFinder {

    private static boolean binaryCandidates = false;
    private static boolean normalCandidates = true;
    private static boolean oneLevelCandidatesOnly = false;


    public static void setBinaryCandidates(boolean useCandidates) {
        binaryCandidates = useCandidates;
    }

    public static void setNormalCandidates(boolean use) {
        normalCandidates = use;
    }

    public static void setOneLevelCandidatesOnly(boolean oneLevelOnly) {
        oneLevelCandidatesOnly = oneLevelOnly;
    }

    public static List<Candidate> getRestrictedListOfCandidatesOfRootNode(FeatureTree tree) {

        List<Candidate> candidates = new ArrayList();
        List<Level> sourceLevel = new ArrayList<Level>(Arrays.asList(Level.SOURCE));        

        switch(tree.getBabyTreeType()) {

            case SOURCE:
                if (Configuration.getInstance().isCodeCompleteWordFirst()) {
                    return new ArrayList<Candidate>();
                }

                //zero-depth-converge: while sim-ann, no candidates on source side!
                if (Configuration.getInstance().isZeroDepthTricks() && Configuration.getInstance().isUseSimulatedAnnealing()) {
                   return new ArrayList<Candidate>();
                }

                //root-restrictions when normal sim-ann (= infinite depth model):
                if (Configuration.getInstance().getInfiniteDepth()) {
                    candidates = getCandidatesOfItselfContextOnMyLevel(tree, candidates, Level.SOURCE);
                    candidates = getCommonCandidatesOfAllTrees(candidates, sourceLevel);
                    return candidates;
                }

                //no restrictions!!!
                candidates = getListOfCandidates(tree);

                break;

            case TARGET:

                if (Configuration.getInstance().isCodeCompleteWordFirst()) {
                    return getListOfCandidates(tree);
                }

                //zero-depth-converge
                if (Configuration.getInstance().isZeroDepthTricks() && Configuration.getInstance().isUseSimulatedAnnealing()) {
                   candidates = getCandidatesOfItselfContextOnOppositeLevel(tree, candidates);
                   return candidates;
                }
                
                //root-restrictions when  sim-ann on: 
                if (Configuration.getInstance().getInfiniteDepth()) {
                    
                    //old version of inf-depth-restricted
                   if (Configuration.getInstance().isUsePreviousVersion()) {
                       candidates = getCandidatesOfItselfContextOnOppositeLevel(tree, candidates);
                       candidates = getCommonCandidatesOfAllTrees(candidates, sourceLevel);
                       return candidates;
                   }

                   candidates = getCandidatesOfItselfContextOnOppositeLevel(tree, candidates);
                   return candidates;
                }

                //no restrictions!!!
                candidates = getListOfCandidates(tree);

                break;

            case JOINT:
                //return nothing
                if (Configuration.getInstance().isZeroDepthTricks() && Configuration.getInstance().isUseSimulatedAnnealing()) {
                    return candidates;
                }

                candidates = getListOfCandidates(tree);
                //closest context missing. --> find out when ok?
//                candidates = getCandidatesOfItselfContextOnMyLevel(tree, candidates, Level.SOURCE);
//                candidates = getCandidatesOfItselfContextOnMyLevel(tree, candidates, Level.TARGET);
//                candidates = getCommonCandidatesOfAllTrees(candidates, sourceLevel);
//                candidates = getCommonCandidatesOfAllTrees(candidates, targetLevel);
                break;

            default:
                throw new RuntimeException("Unknown alignment type");
        }


        return candidates;
    }



    public static List<Candidate> getListOfCandidates(FeatureTree tree) {

        List<Candidate> candidates = new ArrayList<Candidate>();
        List<Level> sourceLevel = new ArrayList<Level>(Arrays.asList(Level.SOURCE));
        List<Level> targetLevel = new ArrayList<Level>(Arrays.asList(Level.TARGET));


        switch(tree.getBabyTreeType()) {
            case SOURCE:
                //first code the complete sourceword, check if ok
                if (Configuration.getInstance().isCodeCompleteWordFirst()) {
                    return new ArrayList<Candidate>();
                }

                //zero-depth-converge
                if (Configuration.getInstance().isZeroDepthTricks() && Configuration.getInstance().isUseSimulatedAnnealing()) {
                   return new ArrayList<Candidate>();
                }

                //inf-depth has restrictions only on root level
                
                if (oneLevelCandidatesOnly) {
                    candidates = getCandidatesOfItselfContextOnMyLevel(tree, candidates, Level.SOURCE);
                    candidates = getCommonCandidatesOfAllTrees(candidates, sourceLevel);                    
                    return candidates;
                }
                
                //no restrictions - normal tree building
                candidates = getCandidatesOfItselfContextOnMyLevel(tree, candidates, Level.SOURCE);
                candidates = getCommonCandidatesOfAllTrees(candidates, sourceLevel);
                candidates = getCommonCandidatesOfAllTrees(candidates, targetLevel);                
                break;

            case TARGET:
                if (!Configuration.getInstance().isCodeCompleteWordFirst() && 
                        Configuration.getInstance().isZeroDepthTricks() &&
                        Configuration.getInstance().isUseSimulatedAnnealing()) {
                   
                    //old version, do nothing on deeper levels
                   if (Configuration.getInstance().isUsePreviousVersion()) {
                       return new ArrayList<Candidate>();
                   }

                   return getCandidatesOfItselfContextOnOppositeLevel(tree, candidates);
                   
                }

                //no restrictions - normal tree building
                candidates = getCandidatesOfItselfContextOnOppositeLevel(tree, candidates);
                candidates = getCandidatesOfItselfContextOnMyLevel(tree, candidates, Level.TARGET);

                candidates = getCommonCandidatesOfAllTrees(candidates, sourceLevel);
                candidates = getCommonCandidatesOfAllTrees(candidates, targetLevel);
               

                if (Configuration.getInstance().isCodeCompleteWordFirst()) {
                    candidates = getFutureCandidates(candidates, sourceLevel);
                }
                candidates = getCandidatesOfClosestContext(tree, candidates);
                
                break;

            case JOINT:

                //zero-depth-converge
                if (Configuration.getInstance().isZeroDepthTricks() && Configuration.getInstance().isUseSimulatedAnnealing()) {
                   return new ArrayList<Candidate>();
                }

                //prev context always ok
                candidates = getCandidatesOfItselfContextOnMyLevel(tree, candidates, Level.SOURCE);
                candidates = getCandidatesOfItselfContextOnMyLevel(tree, candidates, Level.TARGET);

                //itself context  already coded  ok
                candidates = getCommonCandidatesOfAllTrees(candidates, sourceLevel);
                candidates = getCommonCandidatesOfAllTrees(candidates, targetLevel);
                break;

            default:
                throw new RuntimeException("Unknown alignment type");
        }


        return candidates;

    }


   private static List<Candidate> getCandidatesOfClosestContext(FeatureTree tree, List<Candidate> candidates) {
       //context = closest
       List<Level> level; 
       List<Features> features;
       List<Context> context;

       switch(tree.getBabyTreeType()) {
            case SOURCE:
                //i know nothing about itself context on target level yet,
                //i don't know everything about myself yet
                return candidates;
            case TARGET:
                //we can only ask what's on source level
                level = Arrays.asList(Level.SOURCE);
                break;
            default:
                throw new RuntimeException("Unknown alignment type");
        }


        context = Arrays.asList(Context.CLOSEST_SYMBOL);
        features = Features.getFullFeatureSet();
        candidates = enumerateCandidates(candidates, level, context, features);

        context = Arrays.asList(Context.CLOSEST_VOWEL);
        features = Features.getVowelFeatures();        
        candidates = enumerateCandidates(candidates, level, context, features);

        context = Arrays.asList(Context.CLOSEST_CONSONANT);
        features = Features.getConsonantFeatures();
        candidates = enumerateCandidates(candidates, level, context, features);


        return candidates;
   }

   private static List<Candidate> getCandidatesOfItselfContextOnMyLevel(FeatureTree tree, List<Candidate> candidates, Level level) {

        List<Level> levelList = new ArrayList<Level>(Arrays.asList(level));
        List<Context> contextList = new ArrayList<Context>(Arrays.asList(Context.ITSELF));
        List<Features> filteredFeatures;

        //Type feature is always the first in order
        List<Features> allFeatureNames = new ArrayList<Features>(Features.getTypeFeature());
        if (tree.getTreeType().equals(TreeType.VOWEL)) {
            //i am a vowel
            allFeatureNames.addAll(Features.getVowelFeatures());
        } else if (tree.getTreeType().equals(TreeType.CONSONANT)) {
            //i'm a consonant
            allFeatureNames.addAll(Features.getConsonantFeatures());
        } //else i'm a type tree, there is nothing to ask about myself on my level

        //pick the features before this feature in coding order
        int firstIndex = 0;
        int lastIndex = allFeatureNames.indexOf(tree.getFeatureName());
        filteredFeatures = new ArrayList<Features>();
        filteredFeatures.addAll(allFeatureNames.subList(firstIndex, lastIndex));

        candidates = enumerateCandidates(candidates, levelList, contextList, filteredFeatures);

        return candidates;
   }

   private static List<Candidate> getCandidatesOfItselfContextOnOppositeLevel(FeatureTree tree, List<Candidate> candidates) {

        List<Level> oppositeLevel;
        List<Context> context;

        //pick alignment type
        switch(tree.getBabyTreeType()) {
            case SOURCE:
                //i know nothing about itself context on target level yet
                break;
            case TARGET:
                context = new ArrayList<Context>(Arrays.asList(Context.ITSELF));
                oppositeLevel = new ArrayList<Level>(Arrays.asList(Level.SOURCE));
                //it's ok to ask everything
                candidates = enumerateCandidates(candidates, oppositeLevel, context, Features.getFullFeatureSet());
                break;
            default:
                throw new RuntimeException("Unknown alignment type");
        }

        return candidates;

   }

   private static List<Candidate> getFutureCandidates(List<Candidate> candidates, List<Level> levels) {

        List<Context> contexts;
        List<Features> features;


        //context = next C
        contexts = Arrays.asList(Context.NEXT_CONSONANT);
        features = Features.getConsonantFeatures();
        features.add(Features.TYPE);
        candidates = enumerateCandidates(candidates, levels, contexts, features);


        //context = next V
        contexts = Arrays.asList(Context.NEXT_VOWEL);
        features = Features.getVowelFeatures();
        features.add(Features.TYPE);
        candidates = enumerateCandidates(candidates, levels, contexts, features);


        //context = prev S
        contexts = Arrays.asList(Context.NEXT_SYMBOL);
        features = Features.getFullFeatureSet();
        candidates = enumerateCandidates(candidates, levels, contexts, features);

       return candidates;

   }

   

   private static List<Candidate> getCommonCandidatesOfAllTrees(List<Candidate> candidates, List<Level> levels) {

        List<Context> contexts;
        List<Features> features;


        //context = prev C
        contexts = Arrays.asList(Context.PREVIOUS_CONSONANT);
        features = Features.getConsonantFeatures();
        features.add(Features.TYPE);
        //No type feature: is there point to ask what is the type of the previous consonant?
        candidates = enumerateCandidates(candidates, levels, contexts, features);

        //context = prev V
        contexts = Arrays.asList(Context.PREVIOUS_VOWEL);
        features = Features.getVowelFeatures();
        features.add(Features.TYPE);
        candidates = enumerateCandidates(candidates, levels, contexts, features);

        //context = prev S
        contexts = Arrays.asList(Context.PREVIOUS_SYMBOL);
        features = Features.getFullFeatureSet();        
        candidates = enumerateCandidates(candidates, levels, contexts, features);

        //context = prev P
        contexts = Arrays.asList(Context.PREVIOUS_POSITION);
        features = Features.getFullFeatureSet();        
        candidates = enumerateCandidates(candidates, levels, contexts, features);

        //context = prev VG
//        contexts = Arrays.asList(Context.PREVIOUS_VOWEL_GROUP);
//        features = Features.getVowelFeatures();
//        features.add(Features.TYPE);
//        candidates = enumerateCandidates(candidates, levels, contexts, features);


        return candidates;
    }





    private static List<Candidate> enumerateCandidates(List<Candidate> candidates,
            List<Level> levels, List<Context> contexts, List<Features> features) {

        Candidate candidate;

        for (Level l: levels) {
            for (Context c: contexts) {
                for (Features f: features) {

                    if (binaryCandidates) {
                        for (char m : f.getFeatureValueNames()) {
                            candidate = new Candidate(l, c, f, m);
                            candidates.add(candidate);
                        }
                    }
                    if (normalCandidates) {
                        candidate = new Candidate(l, c, f);
                        candidates.add(candidate);
                    }
                }
            }
        }

        return candidates;

    }

}
