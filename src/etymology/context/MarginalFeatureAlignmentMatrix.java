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

import etymology.align.AlignmentMatrix;
import etymology.align.WordAlignment;
import etymology.input.Input;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author sxhiltun
 */
public class MarginalFeatureAlignmentMatrix extends FeatureAlignmentMatrix implements AlignmentMatrix {


    private final Collection<FeatureAlignmentMatrix> matrices = new ArrayList();

    private final FeatureAlignmentMatrix l1l2Alignments;
    private final FeatureAlignmentMatrix l1l3Alignments;
    private final FeatureAlignmentMatrix l2l3Alignments;

    private int l3SymbolCount;
    private int l3LanguadeIdx;

    public MarginalFeatureAlignmentMatrix(Input input) throws Exception {
        super(input);

        if (input.getNumOfLanguages() != 3) {
            throw new RuntimeException(MarginalFeatureAlignmentMatrix.class.getName() + " can be run only using three languages.");
        }

        l1l2Alignments = new FeatureAlignmentMatrix(input, 0, 1);
        l1l3Alignments = new FeatureAlignmentMatrix(input, 0, 2);
        l2l3Alignments = new FeatureAlignmentMatrix(input, 1, 2);
        matrices.add(l1l2Alignments);
        matrices.add(l1l3Alignments);
        matrices.add(l2l3Alignments);

        init(input);
    }

    private void init(Input input) {
        l3SymbolCount = 1 + input.getLengthOneGlyphCount(2);
        l3LanguadeIdx = 2;
    }

    public Collection<FeatureAlignmentMatrix> getMatrices() {
        return matrices;
    }

    @Override
    public int getL3LangId() {
        return l3LanguadeIdx;
    }

    @Override
    public int getL3SymbolCount() {
        return l3SymbolCount;
    }

    @Override
    public void buildTrees() throws Exception {

        for (FeatureAlignmentMatrix fam : matrices) {
            fam.buildTrees();
        }

    }

    @Override
    public void rebuildTrees() throws Exception {
       for (FeatureAlignmentMatrix fam : matrices) {
            fam.rebuildTrees();
        }

    }

    @Override
    public List<FeatureTree> getTrees() {
        List<FeatureTree> allTrees = new ArrayList();
        for (FeatureAlignmentMatrix fam : matrices) {
            allTrees.addAll(fam.getTrees());
        }

        
        return allTrees;
    }


    
    @Override
    public void incrementAlignCounts(WordAlignment wa, int wordIndex) throws Exception {
        for (FeatureAlignmentMatrix fam : matrices) {

           if (wa.get(fam.getL1LangId()).isEmpty() ||
                   wa.get(fam.getL2LangId()).isEmpty()) {
                continue;
            }
            fam.incrementAlignCounts(wa, wordIndex);

        }
    }

    @Override
    public void addIncrementsToLeafNodes(WordAlignment wa, int wordIndex) throws Exception {
        for (FeatureAlignmentMatrix fam : matrices) {
           if (wa.get(fam.getL1LangId()).isEmpty() ||
                   wa.get(fam.getL2LangId()).isEmpty()) {
                continue;
           }
           
            fam.addIncrementsToLeafNodes(wa, wordIndex);            
        }        
    }

    
   
    @Override
    public void decrementAlignCountsFromStructureAndTrees(WordAlignment wa, int wordIndex) throws Exception {
        for (FeatureAlignmentMatrix fam : matrices) {
           if (wa.get(fam.getL1LangId()).isEmpty() ||
                   wa.get(fam.getL2LangId()).isEmpty()) {               
                continue;
            }
           
           fam.decrementAlignCountsFromStructureAndTrees(wa, wordIndex);          
        }      
    }



    @Override
    public double getAlignmentCountAtIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        if (l1GlyphIdx == null) {
            return l2l3Alignments.getAlignmentCountAtIndex(l2GlyphIdx, l3GlyphIdx);
        }

        if (l2GlyphIdx == null) {
            return l1l3Alignments.getAlignmentCountAtIndex(l1GlyphIdx, l3GlyphIdx);
        }

        if (l3GlyphIdx == null) {
            return l1l2Alignments.getAlignmentCountAtIndex(l1GlyphIdx, l2GlyphIdx);
        }

        // sum otherwise
        return    l1l2Alignments.getAlignmentCountAtIndex(l1GlyphIdx, l2GlyphIdx)
                + l1l3Alignments.getAlignmentCountAtIndex(l1GlyphIdx, l3GlyphIdx)
                + l2l3Alignments.getAlignmentCountAtIndex(l2GlyphIdx, l3GlyphIdx);
    }

    @Override
    public double getAlignmentCostByIndex(List<List<Integer>> alignmentPathsTillNow) throws Exception {
        double totalCost = 0;


        for (FeatureAlignmentMatrix fam : matrices) {
            int l1 = fam.getL1LangId();
            int l2 = fam.getL2LangId();
            if (!alignmentPathsTillNow.get(l1).contains(null) &&
                    !alignmentPathsTillNow.get(l2).contains(null)) {
                int length = alignmentPathsTillNow.get(l1).size()-1;
                totalCost += getAlignmentCostByIndex(fam, alignmentPathsTillNow, length, length);
            }

        }      
        
        return totalCost;
        
    }
   
    
    private double getAlignmentCostByIndex(FeatureAlignmentMatrix fam,
            List<List<Integer>> alignmentPathsTillNow, 
            int sourcePositionInWordIdx, int targetPositionInWordIdx) throws Exception {
               
        return fam.getAlignmentCostByIndex(alignmentPathsTillNow, sourcePositionInWordIdx, targetPositionInWordIdx);
    }

}
