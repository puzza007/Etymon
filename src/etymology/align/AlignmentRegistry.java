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
package etymology.align;

import etymology.config.Configuration;
import etymology.context.FeatureAlignmentMatrix;
import etymology.context.FeatureTreeContainer;
import etymology.viterbi.IViterbiMatrix;
import etymology.viterbi.ViterbiMatrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author arto
 */
public class AlignmentRegistry {
    // each array index represents word index
    private WordAlignment[] lastAlignments;
    private double[] lastAlignmentCosts;

    public AlignmentRegistry(int wordCount) {
        init(wordCount);
    }    

    private void init(int wordCount) {
        this.lastAlignments = new WordAlignment[wordCount];
        this.lastAlignmentCosts = new double[wordCount];
        
    }

    public void setAllWordAlignments(WordAlignment[] firstAlignments) {        
        this.lastAlignments = firstAlignments;
        //System.out.println(Arrays.toString(lastAlignments));
    }

    public WordAlignment[] getAlignments() {
        return lastAlignments;
    }
    
   

    public WordAlignment getAlignment(int wordIndex) {
        return lastAlignments[wordIndex];
    }


    public double getAlignmentCost(int wordIndex) {
        return lastAlignmentCosts[wordIndex];
    }
    
    

    public void setAlignmentCost(int wordIndex, double cost) {
        
        //System.out.println("WordIdex: " + wordIndex + " Cost: " + cost);
        lastAlignmentCosts[wordIndex] = cost;
    }
    
   
    
   
    
    public WordAlignment deregisterAlignment(AlignmentMatrix alignmentMatrix, int wordIndex) {
        WordAlignment alignment = getAlignment(wordIndex);
        if (alignment == null || alignment.isEmpty()) {
            return alignment;
        }

        // deregister alignment with a missing word
        if(alignment.hasMissingLanguage()) {
            deregisterNonFullAlignment(alignmentMatrix, alignment);
            return alignment;
        }

        // deregister full alignment
        for (int i = 0; i < alignment.get(0).size(); i++) {
            if(alignment.getNumOfLanguages() == 2) {
                alignmentMatrix.decrementAlignCount(alignment.get(0).get(i), alignment.get(1).get(i));
            } else if (alignment.getNumOfLanguages() == 3) {
                alignmentMatrix.decrementAlignCount(alignment.get(0).get(i), alignment.get(1).get(i), alignment.get(2).get(i));
            }
        }

        if (Configuration.getInstance().isRemoveSuffixes()) {
            alignmentMatrix.decrementSuffixes(alignment.getSuffix(0), alignment.getSuffix(1));
        }

        // reset caches after the full decrements
        alignmentMatrix.resetCache();
        return alignment;
    }



    public void deregisterNonFullAlignment(AlignmentMatrix alignmentMatrix, WordAlignment alignment) {
        // a language is missing in this alignment, deregister using the known alignment values
        List<double[]> costs = alignment.getAlignmentCosts();
        for (int i = 0; i < alignment.get(0).size(); i++) {
            switch (alignment.getMissingLanguageId()) {
                case 0:
                    alignmentMatrix.decrementAlignByDeterminedCosts(null, alignment.get(1).get(i), alignment.get(2).get(i), costs.get(i));
                    break;
                case 1:
                    alignmentMatrix.decrementAlignByDeterminedCosts(alignment.get(0).get(i), null, alignment.get(2).get(i), costs.get(i));
                    break;
                case 2:
                    alignmentMatrix.decrementAlignByDeterminedCosts(alignment.get(0).get(i), alignment.get(1).get(i), null, costs.get(i));
                    break;
            }
        }
    }


    public void registerAlignment(AlignmentMatrix alignmentMatrix, int wordIndex, IViterbiMatrix viterbiMatrix) {
        WordAlignment alignment = new WordAlignment(viterbiMatrix);
        registerAlignment(alignmentMatrix, alignment, wordIndex);
        
        if (Configuration.getInstance().isRemoveSuffixes()) {
            alignmentMatrix.incrementSuffixes(alignment.getSuffix(0), alignment.getSuffix(1));
        }

    }


    public void registerAlignment(AlignmentMatrix alignmentMatrix, WordAlignment wordAlignment, int wordIndex) {

        lastAlignments[wordIndex] = wordAlignment;


        //3D-alignment
        if(wordAlignment.hasMissingLanguage()) {
            registerNonFullAlignment(alignmentMatrix, wordAlignment);
            return;
        }

        // no langs missing, peace of cake.
        for (int i = 0; i < wordAlignment.get(0).size(); i++) {
            if(wordAlignment.getNumOfLanguages() == 2) {
                alignmentMatrix.incrementAlignCount(wordAlignment.get(0).get(i), wordAlignment.get(1).get(i));
            } else { // assume 3
                alignmentMatrix.incrementAlignCount(wordAlignment.get(0).get(i), wordAlignment.get(1).get(i), wordAlignment.get(2).get(i));
            }
        }

    }


    private void registerNonFullAlignment(AlignmentMatrix alignmentMatrix, WordAlignment alignment) {
        // a language is missing in this alignment, register it and store the increments for future decrement
        List<double[]> costs = new ArrayList();
        for (int i = 0; i < alignment.get(0).size(); i++) {
            double[] cost = null;

            switch (alignment.getMissingLanguageId()) {
                case 0:
                    cost = alignmentMatrix.incrementAlignCount(null, alignment.get(1).get(i), alignment.get(2).get(i));
                    break;
                case 1:
                    cost = alignmentMatrix.incrementAlignCount(alignment.get(0).get(i), null, alignment.get(2).get(i));
                    break;
                case 2:
                    cost = alignmentMatrix.incrementAlignCount(alignment.get(0).get(i), alignment.get(1).get(i), null);
                    break;
            }

            costs.add(cost);
        }

        alignment.setAlignmentCosts(costs); // for future use
    }

    //-------------------Feature alignments-------------------------------------//

    public WordAlignment deregisterFeatureAlignment(FeatureAlignmentMatrix featureAlignmentMatrix, int wordIndex) throws Exception {
        WordAlignment alignment = getAlignment(wordIndex);
        if (alignment == null || alignment.isEmpty()) {
            return alignment;
        }

        // deregister full alignment
        
        //featureAlignmentMatrix.decrementAlignCountsFromStructureAndTrees(alignment, wordIndex); 
        featureAlignmentMatrix.decrementAlignCountsFromStructureAndRebuildTrees(alignment, wordIndex); //---Lv
        return alignment;
    }
    
    public WordAlignment deregisterFeatureAlignmentNotRebuildTrees(FeatureAlignmentMatrix featureAlignmentMatrix, int wordIndex) throws Exception {
        WordAlignment alignment = getAlignment(wordIndex);
        if (alignment == null || alignment.isEmpty()) {
            return alignment;
        }

        // deregister full alignment
        
        //Javad: commented for debugging only //Next line only
        featureAlignmentMatrix.decrementAlignCountsFromStructureAndTrees(alignment, wordIndex);
        //featureAlignmentMatrix.decrementAlignCountsFromStructureAndRebuildTrees(alignment, wordIndex);
        return alignment;
    }



    public void registerFirstRoundFeatureAlignment(FeatureAlignmentMatrix featureAlignmentMatrix, int wordIndex, ViterbiMatrix viterbiMatrix) throws Exception {
        WordAlignment alignment = new WordAlignment(viterbiMatrix);
        lastAlignments[wordIndex] = alignment;
        featureAlignmentMatrix.incrementAlignCounts(alignment, wordIndex);

    }

    public void registerFeatureAlignment(FeatureAlignmentMatrix featureAlignmentMatrix, int wordIndex, ViterbiMatrix viterbiMatrix) throws Exception {
        WordAlignment alignment = new WordAlignment(viterbiMatrix);
        registerFeatureAlignment(featureAlignmentMatrix, alignment, wordIndex);

    }
    
    public void registerFeatureAlignmentAndRestoreTrees(FeatureAlignmentMatrix featureAlignmentMatrix, WordAlignment wordAlignment, int wordIndex, FeatureTreeContainer ftc) throws Exception {
        lastAlignments[wordIndex] = wordAlignment;
        featureAlignmentMatrix.incrementAlignCounts(wordAlignment, wordIndex);
        featureAlignmentMatrix.SetFeatureTreeContainer(ftc);
        
    }
    
    public void registerFeatureAlignment(FeatureAlignmentMatrix featureAlignmentMatrix, WordAlignment wordAlignment, int wordIndex) throws Exception {
        lastAlignments[wordIndex] = wordAlignment;
/* Javad: commented for debugging only */        
        featureAlignmentMatrix.incrementAlignCounts(wordAlignment, wordIndex);
        featureAlignmentMatrix.addIncrementsToLeafNodes(wordAlignment, wordIndex);
        
       
    }



    //to be used with pipeline approach..
    public void registerExistingAlignmentsToFeatureMatrix(FeatureAlignmentMatrix featureAlignmentMatrix) throws Exception {
        for(int wordIndex = 0; wordIndex < lastAlignments.length; wordIndex++) {
            if(lastAlignments[wordIndex] == null) {
                continue;
            }

            featureAlignmentMatrix.incrementAlignCounts(lastAlignments[wordIndex], wordIndex);
        }

        featureAlignmentMatrix.buildTrees();
        //System.out.println("Existing alignments now in alignment register");
    }
    
    public void registerExistingAlignmentsToAlignmentMatrix(AlignmentMatrix matrix) {
        
        for(int wordIndex = 0; wordIndex < lastAlignments.length; wordIndex++) {
            if(lastAlignments[wordIndex] == null) {
                continue;
            }

            matrix.incrementAlignCount(lastAlignments[wordIndex]);
        }

        
    }

}
