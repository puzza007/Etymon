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
import etymology.align.Kind;
import etymology.align.WordAlignment;
import etymology.align.matrices.SuffixAlignmentMatrix;
import etymology.config.Configuration;
import etymology.context.FeatureTreeContainer.AlignmentKindIdentifier;
import etymology.context.FeatureTreeContainer.BabyTreeType;
import etymology.context.FeatureTreeContainer.Context;
import etymology.context.FeatureTreeContainer.Level;
import etymology.input.FeatureVocabulary;
import etymology.input.Input;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 
 * @author sxhiltun
 */
public class FeatureAlignmentMatrix  implements AlignmentMatrix {

    private int l1SymbolCount;
    private int l2SymbolCount;

    private int l1LanguageIdx;
    private int l2LanguageIdx;

    private boolean printCostsDuringImputation;
    private List<Double> sourceCosts;
    private List<Double> targetCosts;

    private ContextCellContainer[] sourceContextCellContainer;
    private ContextCellContainer[] targetContextCellContainer;
    private int[][] alignmentCountMatrix;
 

    private Input input;
    private FeatureVocabulary fvSource;
    private FeatureVocabulary fvTarget;

    //number of words in common for both languages
    private final int numOfWordsForThisMatrix;

    FeatureTreeContainer featureTreeContainer;


    public FeatureAlignmentMatrix(Input input) throws Exception {
        
        this(input, 0, 1);
    }
    
    public FeatureAlignmentMatrix(Input input, int l1LanguageIdx, int l2LanguageIdx) throws Exception {
        
        this.l1LanguageIdx = l1LanguageIdx;
        this.l2LanguageIdx = l2LanguageIdx;
        this.input = input;
        printCostsDuringImputation = false;

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

    private void init(Input input) throws Exception {
         
        fvSource = (FeatureVocabulary) input.getVocabulary(l1LanguageIdx);        
        fvTarget = (FeatureVocabulary) input.getVocabulary(l2LanguageIdx);
               
        //dot + symbols + # (word boundary symbol)
        this.l1SymbolCount = fvSource.getFeatureVocabularySize();
        this.l2SymbolCount = fvTarget.getFeatureVocabularySize();

        
        //both context cell containers contain the same things, but make search faster
        this.sourceContextCellContainer = new ContextCellContainer[l1SymbolCount];
        for (int i=0; i<l1SymbolCount; i++) {
            this.sourceContextCellContainer[i] = new ContextCellContainer();
        }

        this.targetContextCellContainer = new ContextCellContainer[l2SymbolCount];
        for (int i=0; i<l2SymbolCount; i++) {
            this.targetContextCellContainer[i] = new ContextCellContainer();
        }

        //this only for printing logs...
        alignmentCountMatrix = new int[l1SymbolCount][l2SymbolCount];
        for (int i=0; i<l1SymbolCount; i++) {
            for (int j=0; j<l2SymbolCount; j++) {                
                alignmentCountMatrix[i][j] = 0;
            }
        }       
    }


    //*************************************************************//
    //****** manipulate the alignment count matrix & update trees**//
    //*************************************************************//


    //creates the first instances of trees
    public void buildTrees() throws Exception {
        if (Configuration.getInstance().isJointCoding()) {
            featureTreeContainer = new JointlyCodingFeatureTreeContainer(sourceContextCellContainer, targetContextCellContainer, input, l1LanguageIdx, l2LanguageIdx);
        } 
        else if (Configuration.getInstance().isCodeOneLevelOnly()) {
            featureTreeContainer = new OneLevelOnlyFeatureTreeContainer(sourceContextCellContainer, input, l1LanguageIdx);
        }
        else {
            featureTreeContainer = new SeparateCodingFeatureTreeContainer(sourceContextCellContainer, targetContextCellContainer, input, l1LanguageIdx, l2LanguageIdx);
        }
    }

    public void rebuildTrees() throws Exception {
        featureTreeContainer.rebuildAllFeatureTrees();
    }
    
    public FeatureTreeContainer getFeatureTreeContainer () {
        return featureTreeContainer;
    }
    
    public void SetFeatureTreeContainer(FeatureTreeContainer ftc) {
        this.featureTreeContainer = ftc;
    }
    
    public List<FeatureTree> getTrees() {
        List<FeatureTree> allTrees = new ArrayList();
        if(featureTreeContainer == null || featureTreeContainer.getTypeTrees() == null) {
            try {
                buildTrees();
            } catch (Exception ex) {
                System.out.println("Unable to build trees at startup..");
            }
        }


        //add type feature trees to the list
        allTrees.addAll(featureTreeContainer.getTypeTrees());

        //add vowel feature trees
        for (List<FeatureTree> ftrees : featureTreeContainer.getVowelTrees()) {
            allTrees.addAll(ftrees);
        }
        
        //add consonant feature trees
        for (List<FeatureTree> ftrees : featureTreeContainer.getConsonantTrees()) {
            allTrees.addAll(ftrees);
        }

        return allTrees;
    }


    public void incrementAlignCounts(WordAlignment wa, int wordIndex) throws Exception {
        addAlignmentToContextCellStructure(wa, wordIndex);
    }


    public void addIncrementsToLeafNodes(WordAlignment wa, int wordIndex) throws Exception {

        //add word boundaries
        List<Integer> sourceIndices = new ArrayList<Integer>(wa.get(l1LanguageIdx));
        
        sourceIndices.add(l1SymbolCount-1);
        
        List<Integer> targetIndices = new ArrayList<Integer>(wa.get(l2LanguageIdx));
        targetIndices.add(l2SymbolCount-1);
        

        int sourceIndex;
        int targetIndex;
        for (int idx=0; idx<sourceIndices.size(); idx++) {

            sourceIndex = sourceIndices.get(idx);
            targetIndex = targetIndices.get(idx);

            //determine the type of the alignment
            AlignmentKindIdentifier aki = AlignmentKindIdentifier.getAlignmentKind(input, sourceIndex, targetIndex, l1LanguageIdx, l2LanguageIdx);
            ContextCell cc;

            //get trees
            List<FeatureTree> featureTrees = featureTreeContainer.getTreesForModification(aki);
            TreeNode node;

            String sourceVector = fvSource.getFeature(sourceIndex);
            String targetVector = fvTarget.getFeature(targetIndex);

            for(FeatureTree ft: featureTrees) {
                try {
                    if (ft.getBabyTreeType() == FeatureTreeContainer.BabyTreeType.SOURCE) {
                        cc = sourceContextCellContainer[sourceIndex].getContextCell(wordIndex, idx);
                    }else {
                        cc = targetContextCellContainer[targetIndex].getContextCell(wordIndex, idx);
                    }

                    node = ft.getLeafNodeOfAlignment(cc, fvSource, fvTarget);
                    ft.incrementMatrixValue(node, sourceVector, targetVector);


                } catch (Exception e) {
                    Thread.sleep(10000);
                    e.printStackTrace();

                    System.err.println("sourceINdex: "+ sourceIndex +" "+sourceVector);
                    System.err.println("targetIndex: "+ targetIndex + " "+ targetVector);
                    System.err.println(ft);
                    System.exit(1);
                }
            }          
        }
    }




    /**
     * Do not rebuild the trees, instead decrement one from the corresponding leaf nodes
     * Note! Costs are currently not updated after decrement.
     *
     * @param wa
     * @param wordIndex
     * @throws Exception
     */
    public void decrementAlignCountsFromStructureAndTrees(WordAlignment wa, int wordIndex) throws Exception {

        //add word boundaries
        List<Integer> sourceIndices = new ArrayList<Integer>(wa.get(l1LanguageIdx));
        sourceIndices.add(l1SymbolCount-1);
        List<Integer> targetIndices = new ArrayList<Integer>(wa.get(l2LanguageIdx));
        targetIndices.add(l2SymbolCount-1);


        int sourceIndex;
        int targetIndex;
        for (int idx=0; idx<sourceIndices.size(); idx++) {

            sourceIndex = sourceIndices.get(idx);
            targetIndex = targetIndices.get(idx);

          
            //determine the type of the alignment
            AlignmentKindIdentifier aki = AlignmentKindIdentifier.getAlignmentKind(input, sourceIndex, targetIndex, l1LanguageIdx, l2LanguageIdx);
            ContextCell cc;

            //get trees
            List<FeatureTree> featureTrees = featureTreeContainer.getTreesForModification(aki);
            TreeNode node;

            String sourceVector = fvSource.getFeature(sourceIndex);
            String targetVector = fvTarget.getFeature(targetIndex);

            for(FeatureTree ft: featureTrees) {

                try {
                    if (ft.getBabyTreeType() == FeatureTreeContainer.BabyTreeType.SOURCE) {
                        cc = sourceContextCellContainer[sourceIndex].getContextCell(wordIndex, idx);

                    }else {
                        cc = targetContextCellContainer[targetIndex].getContextCell(wordIndex, idx);
                    }

                    node = ft.getLeafNodeOfAlignment(cc, fvSource, fvTarget);
                    ft.decrementMatrixValue(node, sourceVector, targetVector);

                } catch (Exception e) {
                    Thread.sleep(10000);
                    e.printStackTrace();

//                        System.err.println(subAlignment.get(0));
//                        System.err.println(subAlignment.get(1));
                    System.err.println("sourceIndex: " + sourceIndex +" "+sourceVector);
                    System.err.println("targetIndex: " + targetIndex + " "+ targetVector);
                    System.err.println(ft);
                    System.exit(1);
                }
            }

        }
        decrementContextCellValues(wa, wordIndex);
        
    }
    /**
     * first subtract the alignment from featureAlignmentMatrix and then rebuild the trees
     * @param wa
     * @param wordIndex
     * @throws Exception 
     */
    public void decrementAlignCountsFromStructureAndRebuildTrees (WordAlignment wa, int wordIndex) throws Exception {
        //add word boundaries
        List<Integer> sourceIndices = new ArrayList<Integer>(wa.get(l1LanguageIdx));
        sourceIndices.add(l1SymbolCount-1);
        List<Integer> targetIndices = new ArrayList<Integer>(wa.get(l2LanguageIdx));
        targetIndices.add(l2SymbolCount-1);
        
        decrementContextCellValues(wa, wordIndex);
        this.rebuildTrees();
        
        
        
    }

    private void decrementContextCellValues(WordAlignment wa, int wordIndex) {

        //add word boundaries
        List<Integer> sourceIndices = new ArrayList<Integer>(wa.get(l1LanguageIdx));
        sourceIndices.add(l1SymbolCount -1);
        List<Integer> targetIndices = new ArrayList<Integer>(wa.get(l2LanguageIdx));
        targetIndices.add(l2SymbolCount -1);


        for (int alignmentIdx = 0; alignmentIdx < sourceIndices.size(); alignmentIdx++) {
            int currentS = sourceIndices.get(alignmentIdx); //source symbol to align
            int currentT = targetIndices.get(alignmentIdx); //target symbol to align

            sourceContextCellContainer[currentS].removeContextCell(wordIndex, alignmentIdx);
            targetContextCellContainer[currentT].removeContextCell(wordIndex, alignmentIdx);
            alignmentCountMatrix[currentS][currentT] -= 1;
        }
    }


    private void addAlignmentToContextCellStructure(WordAlignment wa, int wordIndex) throws Exception {

        //add word boundaries
        List<Integer> sourceIndices = new ArrayList<Integer>(wa.get(l1LanguageIdx));
        sourceIndices.add(l1SymbolCount -1);
        
        List<Integer> targetIndices = new ArrayList<Integer>(wa.get(l2LanguageIdx));
        targetIndices.add(l2SymbolCount -1);

        int alignmentLength = sourceIndices.size();
//        System.out.println(wa.getStringPresentation(input));
//        System.out.println(sourceIndices);
//        System.out.println(targetIndices);


        //iterate through the glyphs in alignment, attach the context
        for (int glyphIndex=0; glyphIndex<alignmentLength; glyphIndex++) {
            int currentS = sourceIndices.get(glyphIndex);   //source symbol to align
            int currentT = targetIndices.get(glyphIndex);   //target symbol to align

            //add glyph pair to alignment matrix
            alignmentCountMatrix[currentS][currentT] += 1;

            //create context cell for source and target glyph -- a structure that contains all information about the sorroundings of the glyph
            ContextCell sourceCell = sourceContextCellContainer[currentS].addContextCell(currentS, wordIndex, glyphIndex);
            fillContextCell(sourceCell, glyphIndex, Level.SOURCE, sourceIndices, fvSource);
            fillContextCell(sourceCell, glyphIndex, Level.TARGET, targetIndices, fvTarget);

            //target level
            ContextCell targetCell = targetContextCellContainer[currentT].addContextCell(currentT, wordIndex, glyphIndex);
            fillContextCell(targetCell, glyphIndex, Level.SOURCE, sourceIndices, fvSource);
            fillContextCell(targetCell, glyphIndex, Level.TARGET, targetIndices, fvTarget);

            //System.out.println("targetcell " + targetCell);

        }

    }

    private void fillContextCell(ContextCell contextCell, int glyphIndex, Level level,  List<Integer> glyphIndexes, FeatureVocabulary fv) throws Exception {
        // precompute = find values in all possible contexts
        //specialize all the Contexts according to certain glyph and level --Lv
        Context.setAllHistoryContexts(glyphIndex, level, glyphIndexes, fv);

        if (Configuration.getInstance().isCodeCompleteWordFirst()) {
            Context.setAllFutureContext(glyphIndex, level, glyphIndexes, fv);
        }

        // attach the values in all contexts to contextCell
        for (Context context : Context.values()) {
           contextCell.setGlyphIndexInContext(context.getIndexOfGlyph(level), level, context);
        }

    }


    //*************************************************************//
    //******     Methods for Viterbi                ***************//
    //*************************************************************//

    public double getRandomAlignmentProbability() {
        return Configuration.getRnd().nextDouble();
    }




    public void setPrintCosts(boolean print) {
        this.printCostsDuringImputation = print;
    }

    public List<Double> getSourceCosts() {
        return this.sourceCosts;
    }

    public List<Double> getTargetCosts() {
        return this.targetCosts;
    }

    public double getAlignmentProbabilityByIndex(List<List<Integer>> alignmentPathsTillNow, int sourceIdx, int targetIdx) throws Exception {

        double cost = getAlignmentCostByIndex(alignmentPathsTillNow, sourceIdx, targetIdx);
        return Math.pow(2, -cost);

    }

    public double getAlignmentCostByIndex(List<List<Integer>> alignmentPathsTillNow) throws Exception {
        int pathSize = alignmentPathsTillNow.get(l1LanguageIdx).size();
        return getAlignmentCostByIndex(alignmentPathsTillNow, pathSize, pathSize);
    }

    
    public double getAlignmentCostByIndex(List<List<Integer>> alignmentPathsTillNow,
            int sourcePositionInWordIdx, int targetPositionInWordIdx) throws Exception {

        //collect context information        
        ContextCell cc = putAlignmentToContextCell(alignmentPathsTillNow, sourcePositionInWordIdx, targetPositionInWordIdx);

        int sourceGlyphIndex = alignmentPathsTillNow.get(l1LanguageIdx).get(sourcePositionInWordIdx);
        int targetGlyphIndex = alignmentPathsTillNow.get(l2LanguageIdx).get(targetPositionInWordIdx);
        
        //determine the type of the alignment
        AlignmentKindIdentifier aki = AlignmentKindIdentifier.getAlignmentKind(input, sourceGlyphIndex, targetGlyphIndex, l1LanguageIdx, l2LanguageIdx);

        
        
        List<FeatureTree> featureTrees = featureTreeContainer.getTreesForModification(aki);


        //compute the cost of each feature
        double cost = 0.0;
        TreeNode node;

        if (this.printCostsDuringImputation) {
            sourceCosts = new ArrayList<Double>();
            targetCosts = new ArrayList<Double>();
        }

        String sourceVector = fvSource.getFeature(sourceGlyphIndex);
        String targetVector = fvTarget.getFeature(targetGlyphIndex);


        for(FeatureTree ft: featureTrees) {

            double eventCost;
            node = ft.getLeafNodeOfAlignment(cc, fvSource, fvTarget);

            eventCost = ft.getEventCost(node, sourceVector, targetVector);
            cost += eventCost;

            if (this.printCostsDuringImputation) {
                if (ft.getBabyTreeType().equals(BabyTreeType.SOURCE)) {
                    sourceCosts.add(eventCost);
                }else if (ft.getBabyTreeType().equals(BabyTreeType.TARGET)) {
                    targetCosts.add(eventCost);
                }
            }

        }

        return cost;
       
    }

    private ContextCell putAlignmentToContextCell(List<List<Integer>> alignmentPathsTillNow, int sourcePositionInWordIndex, int targetPositionInWordIndex) throws Exception {

        //alignmentPaths till now MUST include the current symbol pair
        List<Integer> sourceIndices = new ArrayList<Integer>(alignmentPathsTillNow.get(l1LanguageIdx));
        List<Integer> targetIndices = new ArrayList<Integer>(alignmentPathsTillNow.get(l2LanguageIdx));



        //public ContextCell(int symbol, int wordIndex, int positionInWordIndex) {
        ContextCell cc = new ContextCell(-1, -1, -1);
        fillContextCell(cc, sourcePositionInWordIndex, Level.SOURCE, sourceIndices, fvSource);
        fillContextCell(cc, targetPositionInWordIndex, Level.TARGET, targetIndices, fvTarget);

        return cc;
    }



    //************************************************************************//
    //** Implements interface, mostly undone, needed in printing????----------//
    //************************************************************************//



    public double[] incrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getAlignmentCountAtIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void decrementAlignCount(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMostProbableGlyphAlignmentByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Kind getKind(int l1SymbolIdx, int l2SymbolIdx, int l3SymbolIdx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void decrementAlignByDeterminedCosts(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, double[] costs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getL3SymbolCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void resetCache() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int[][] getAlignmentCountMatrix() {
        return alignmentCountMatrix;
    }

    public int getTotalAlignmentCounts() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getTotalAlignmentAlphas() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void incrementAlignCount(int sourceSymbolIndex, int targetSymbolIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void incrementAlignCount(WordAlignment wa) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double[] incrementAlignCount(Integer... glyphIdx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getAlignmentCountAtIndex(int sourceGlyphIndex, int targetGlyphIndex) {
        return alignmentCountMatrix[sourceGlyphIndex][targetGlyphIndex];
    }

    public void decrementAlignCount(int sourceSymbolIndex, int targetSymbolIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getAlignmentProbabilityByIndex(int sourceGlyphIndex, int targetGlyphIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Kind getKind(int sourceSymbolIndex, int targetSymbolIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Kind> getAllKinds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getL1SymbolCount() {
        return l1SymbolCount;
    }
    
    public int getL2SymbolCount() {
        return l2SymbolCount;
    }

    public double getPrior(int sourceSymbolIdx, int targetSymbolidx) {
        throw new UnsupportedOperationException("Not supported yet.");
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

    public int getNumberOfNonZeroAlignments() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
 

    public double getDotToDotAllowedAlignmentProbabilityByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    public int getNumberOfWords() {
        return this.numOfWordsForThisMatrix;
    }

    public double getAlignmentCost(Map<Integer, Integer> languageIdToGlyphIndexes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public double getFeatureAlignmentCostByGlyphIndexes(Map<Integer, List<Integer>> languageIdToAlignmentPathUntilNow, Map<Integer, Integer> languageIdToGlyphIndexes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void incrementSuffixes(List<Integer> sourceSuffix, List<Integer> targetSuffix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void decrementSuffixes(List<Integer> sourceSuffix, List<Integer> targetSuffix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SuffixAlignmentMatrix getSuffixAlignmentMatrix(int languageId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getSuffixCost(List<Integer> suffix, int language) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMostProbableGlyphAlignmentByIndex(Integer l1GlyphIdx, Integer l2GlyphIdx, Integer l3GlyphIdx, int languageToImputeIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public double getTotalTreeCosts() throws Exception {        
        double costs = 0;
        for(FeatureTree tree : getTrees()){
            tree.computeTotalCostOfTree();
            costs += tree.getTotalTreeCost();
        }
        return costs;
    }
}
