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
package etymology.viterbi;

import etymology.config.Configuration;
import etymology.config.Constants;
import etymology.align.Alignator;
import etymology.align.AlignmentMatrixType;
import etymology.cost.SuffixCostCalculator;
import etymology.input.FeatureVocabulary;
import etymology.input.Input;
import etymology.logging.StaticLogger;
import etymology.util.Arrows;
import etymology.util.EtyMath;
import etymology.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author arto
 */
public class ViterbiMatrix implements IViterbiMatrix {

    private Alignator alignator;

    private ViterbiCell[][] viterbiPath;
    private ViterbiCell[][][] l3ViterbiPath;

    private int sourceLangId = 0;
    private int targetLangId = 1;

    private int totalLanguages;
    private List<Integer> missingLanguageIds = new ArrayList();

    private boolean fixAlignment = false;
    private boolean completelyRandom = false;
    private boolean impute = false;

    private List<Integer> sourceWord;
    private List<Integer> targetWord;

    private boolean useFeatures = Configuration.getInstance().isUseFeatures();
    private ViterbiCell finalCell;
    private double totalCost;

    public double getTotalCost() {
        return totalCost;
    }

    
    public ViterbiMatrix(Alignator alignator) {
        this.alignator = alignator;
    }

    public void setCompletelyRandom(boolean completelyRandom) {
        this.completelyRandom = completelyRandom;
    }

    public int getTotalLanguages() {
        return totalLanguages;
    }

    public Collection<Integer> getMissingLanguageIds() {
        return missingLanguageIds;
    }
    

    public void init(Input input, int wordIndex) {
        
        //get the list of glyph indexes for both target word and source word : wordIndexes
        List<List<Integer>> wordIndexes = new ArrayList();
        for(int langNo = 0; langNo < input.getNumOfLanguages(); langNo++) {
            wordIndexes.add(input.getWordIndexes(langNo, wordIndex));
            
        }
        
        List<Integer>[] indexes = wordIndexes.toArray(new ArrayList[wordIndexes.size()]);
        
        try {
            
            init(indexes);
        } catch (Exception ex) {
            Logger.getLogger(ViterbiMatrix.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    //... means zero or more List<Integer> (as an array) may be passed to the function
    public void init(List<Integer>... wordIndexes) throws Exception {
        if(wordIndexes.length == 2) {
            init(wordIndexes[0], wordIndexes[1]);
        } else {
            // System.out.println("Three langs..");
            
            init(wordIndexes[0], wordIndexes[1], wordIndexes[2]);
        }
    }

    public void init(List<Integer> l1Word, List<Integer> l2Word, List<Integer> l3Word) throws Exception {

        totalLanguages = 3;


        // IF ONE WORD MISSING; HANDLE AS 2D CASE
        if(l1Word == null || l2Word == null || l3Word == null) {
            // System.out.println("One lang null, special case");
            initSpecialCase(l1Word, l2Word, l3Word);
            finalCell = viterbiPath[viterbiPath.length - 1][viterbiPath[0].length - 1];
            return;
        }

        // otherwise, proceed as if we were doing 3d alignment

        l3ViterbiPath = new ViterbiCell[l1Word.size() + 1][l2Word.size() + 1][l3Word.size() + 1];

        initRoot();
        // System.out.println("Root initiated");

        for (int x = 0; x < l3ViterbiPath.length; x++) {
            for (int y = 0; y < l3ViterbiPath[0].length; y++) {
                for (int z = 0; z < l3ViterbiPath[0][0].length; z++) {
                    // System.out.println("Trying to init cell " + x + ", " + y + ", " + z);
                    if(l3ViterbiPath[x][y][z] != null) {
                        // we know this cell already, so ignore it
                        continue;
                    }

                    // System.out.println("Initiating cell " + x + ", " + y + ", " + z);
                    initCell(x, y, z, l1Word, l2Word, l3Word);
                }
            }
        }
        // System.out.println("Cells initiated");
        
    }
    

    public void init(List<Integer> sourceWord, List<Integer> targetWord) throws Exception {
        this.sourceWord = sourceWord;
        this.targetWord = targetWord;

//        if (completelyRandom && !Configuration.getInstance().areWordsFlippedAround()) {
//            Collections.reverse(this.sourceWord);
//            Collections.reverse(this.targetWord);
//        }

        totalLanguages = 2;
        viterbiPath = new ViterbiCell[sourceWord.size() + 1][targetWord.size() + 1];

        // root
        initRoot();

        for (int x = 0; x < viterbiPath.length; x++) {
            for (int y = 0; y < viterbiPath[0].length; y++) {

                if (viterbiPath[x][y] != null) {
                    // we know this cell already, so ignore it
                    continue;
                }

                //2x2-model: 
                if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
                    //the impossible cells
                    if  ((x == 0) ^ (y == 0)) {
                        continue;
                    }
                    // this is a XOR - operator !!!!! :)
                    if ((x == viterbiPath.length-1) ^ (y == viterbiPath[0].length-1)) {
                        continue;
                    }
                }
                
                
                viterbiPath[x][y] = initCell(x, y, sourceWord, targetWord);

            }
        }

        if (Configuration.getInstance().isRemoveSuffixes()) {
            computeBestHyperJump(sourceWord, targetWord);
        } else {
            finalCell = viterbiPath[viterbiPath.length - 1][viterbiPath[0].length - 1];
            //<Added by Javad for debugging purposes>
            //The cost printed here is not the actual cost. We must also pay for word boundary coding.
            double costAlongWithWordBoundary = computeWordBoundaryCostToo(finalCell);
            this.totalCost = costAlongWithWordBoundary;
            
            //Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine("COST: " + costAlongWithWordBoundary);
            //</Added by Javad>
        }
        //System.out.println("Final cell: " + finalCell.getCost());
//        
//        if (completelyRandom && !Configuration.getInstance().areWordsFlippedAround()) {
//            Collections.reverse(this.sourceWord);
//            Collections.reverse(this.targetWord);
//        }

    }
    
    public void init3DImputation(Input input, int wordIndex) throws Exception {
        
        this.impute = true;
        
        List<Integer> l1Word = input.getWordIndexes(sourceLangId, wordIndex);
        List<Integer> l2Word = input.getWordIndexes(targetLangId, wordIndex);
        
        init(l1Word, l2Word);
        
        totalLanguages = 3;
        
    }
    
    

    private void computeBestHyperJump(List<Integer> sourceWord, List<Integer> targetWord) {

        double[] sourceSuffixCostTable = computeCostOfSuffixes(sourceLangId, sourceWord);
        double[] targetSuffixCostTable = computeCostOfSuffixes(targetLangId, targetWord);

        List<ViterbiCell> allCandidates = new ArrayList<ViterbiCell>(sourceWord.size()*targetWord.size());
        
        //normally this is the final cell
        ViterbiCell minimumCell = viterbiPath[viterbiPath.length - 1][viterbiPath[0].length - 1];

        //add cost of (#:#)
        double minimumCost = minimumCell.getCost() +
                alignator.getAlignmentCostByIndex(Constants.WORD_BOUNDARY_INDEX, Constants.WORD_BOUNDARY_INDEX);

        //minimumCell.setSuffixAlignmentCost(minimumCost);


        double totalSuffixCost;
        double sourceSuffixCost;
        double targetSuffixCost;
        int sourceBoundary;
        int targetBoundary;

        for (int x = 0; x < viterbiPath.length; x++) {
            sourceSuffixCost = sourceSuffixCostTable[x];
            if (sourceSuffixCost > 0) {
                sourceBoundary = Constants.SUFFIX_BOUNDARY_INDEX;
            }else {
                sourceBoundary = Constants.WORD_BOUNDARY_INDEX;
            }

            for (int y = 0; y < viterbiPath[0].length; y++) {
                targetSuffixCost = targetSuffixCostTable[y];
                if (targetSuffixCost > 0) {
                    targetBoundary = Constants.SUFFIX_BOUNDARY_INDEX;
                }else {
                    targetBoundary = Constants.WORD_BOUNDARY_INDEX;
                }

                totalSuffixCost = 
                        viterbiPath[x][y].getCost() +
                        alignator.getAlignmentCostByIndex(sourceBoundary, targetBoundary) +
                        sourceSuffixCost + targetSuffixCost;
                        

                //viterbiPath[x][y].setSuffixAlignmentCost(totalSuffixCost);
                viterbiPath[x][y].setCost(totalSuffixCost);
                if (x < sourceWord.size()) {
                    viterbiPath[x][y].setSourceSuffix(sourceWord.subList(x, sourceWord.size()));
                }
                if (y < targetWord.size()) {
                    viterbiPath[x][y].setTargetSuffix(targetWord.subList(y, targetWord.size()));
                }

                if (totalSuffixCost < minimumCost) {
                    minimumCost = totalSuffixCost;
                    minimumCell = viterbiPath[x][y];
                }

                if (Configuration.getInstance().isUseSimulatedAnnealing()) {
                    allCandidates.add(viterbiPath[x][y]);
                }

            }
        }

        //pick any cell
        if(completelyRandom) {
            Collections.shuffle(allCandidates, Configuration.getRnd());
            finalCell = allCandidates.get(0);
        }
        // simann
        else if(Configuration.getInstance().isUseSimulatedAnnealing()) {
            finalCell = findBestCandidateUsingSimAnn(allCandidates, minimumCell);
        } 
        //best = lowest cost
        else {
            finalCell = minimumCell;
        }



    }

    private ViterbiCell findBestCandidateUsingSimAnn(List<ViterbiCell> allCandidates, ViterbiCell minimumCell) throws RuntimeException {
        ViterbiCell bestCandidate = null;

        //Collections.sort(allCandidates, ViterbiCell.SUFFIX_COST_COMPARATOR);
        Collections.sort(allCandidates);

        bestCandidate = allCandidates.get(0);
        if (!bestCandidate.equals(minimumCell)) {
            throw new RuntimeException("Minimum Cell not found correctly!!!");
        }
        bestCandidate.setCostDifference(0);
        
        
        //double bestCost = bestCandidate.getSuffixAlignmentCost();
        double bestCost = bestCandidate.getCost();
        for (ViterbiCell candidate : allCandidates.subList(1, allCandidates.size())) {
            double costDiff = (candidate.getCost() - bestCost) / alignator.getCurrentTemperature();
            candidate.setCostDifference(costDiff);
        }

        double rnd = Configuration.getRnd().nextDouble();
        int maxCandidateIndex = 1;
        while (maxCandidateIndex < allCandidates.size() && Math.exp(-1.0 * allCandidates.get(maxCandidateIndex).getCostDifference()) > rnd) {
            maxCandidateIndex++;
        }
        int candidateIndexToPick = Configuration.getRnd().nextInt(maxCandidateIndex);
        bestCandidate = allCandidates.get(candidateIndexToPick);

        return bestCandidate;


    }

    private double[] computeCostOfSuffixes(int language, List<Integer> word) {

        //Add word boundary to the end of suffix
        //if length of suffix > 0, wb must be coded separately
        List<Integer> suffix = new ArrayList<Integer>(word);
        suffix.add(Constants.WORD_BOUNDARY_INDEX);
        
        double[] suffixCost = new double[suffix.size()];

        for (int i=0; i<suffix.size()-1; i++) {

            //change this to alignator.getCost(...)
            suffixCost[i] = alignator.getSuffixCost(suffix.subList(i, suffix.size()), language);
            //suffixCost[i] = SuffixCostCalculator.getSuffixCost(language, suffix.subList(i, suffix.size()));
        }
        suffixCost[suffix.size()-1] = 0;

        return suffixCost;
    }


    /**
     * create the first cell and set it to viterbiPath[0][0]
     */    
    private void initRoot() {
        //root.parent=null, root.cost=0.0
        ViterbiCell root = ViterbiCell.createRoot();

        if(viterbiPath != null) {
            viterbiPath[0][0] = root;
        } else {
            l3ViterbiPath[0][0][0] = root;
        }
    }

    @Override
    public ViterbiCell getFinalCell() {
        return finalCell;
    }

    @Override
    public double getCost() {
        if (Configuration.getInstance().isRemoveSuffixes()) {
            //return finalCell.getSuffixAlignmentCost();
            return finalCell.getCost();
        }
        if(viterbiPath == null) {
            return l3ViterbiPath[l3ViterbiPath.length - 1][l3ViterbiPath[0].length - 1][l3ViterbiPath[0][0].length - 1].getCost();
        }

        return viterbiPath[viterbiPath.length - 1][viterbiPath[0].length - 1].getCost();
    }

    private ViterbiCell initCell(int sourceIdx, int targetIdx, List<Integer> sourceWord, List<Integer> targetWord) throws Exception  {
        
        List<ViterbiCell> candidates = collectViterbiCandidates(sourceIdx, targetIdx, sourceWord, targetWord);

        assert (candidates.size() == 3);
        Collections.sort(candidates);
        //System.out.println(candidates.size());
        ViterbiCell bestCandidate = null;
        try {
             bestCandidate = candidates.get(0);
             
             //PICK RANDOMLY FROM THE "EQUALLY COST CANDIDATES"
             int lastEqualCandidate = 1;
             for (ViterbiCell candidate : candidates.subList(1, candidates.size())) {
                 double costDiff = candidate.getCost() - bestCandidate.getCost();
                 if (costDiff < 0.00001 & !alignator.isUseSimulatedAnnealing()) {
                     lastEqualCandidate++;
                 }
             }
            int candidateIndexToPick = Configuration.getRnd().nextInt(lastEqualCandidate);
            bestCandidate = candidates.get(candidateIndexToPick);
//             
//             //find all candidates that have equal cost to the first candidate in order. 
//             int lastEqualCandidate = 1;
//             for (int i=1; i<candidates.size(); i++) {
//
//                 
//                 //tarkista
//                 if (candidates.get(i).compareTo(bestCandidate) == 0) {
//                     lastEqualCandidate = i+1;
//                 }else{
//                     break;
//                 }
//             }
//             
//             int candidateIndexToPick = Configuration.getRnd().nextInt(lastEqualCandidate);
//             bestCandidate = candidates.get(candidateIndexToPick);
             
                          
        } catch(Exception e) {
            System.out.println("Null, sourceIdx: " + sourceIdx +  " targetIdx: " + targetIdx);
        }

        if (alignator.isUseSimulatedAnnealing()) {
            double bestCost = bestCandidate.getCost();
            for (ViterbiCell candidate : candidates.subList(1, candidates.size())) {
                double costDiff = (candidate.getCost() - bestCost) / alignator.getCurrentTemperature();
                candidate.setCostDifference(costDiff);
            }

            double rnd = Configuration.getRnd().nextDouble();
            int maxCandidateIndex = 1;
            while (maxCandidateIndex < candidates.size()
                    && Math.exp(-1.0 * candidates.get(maxCandidateIndex).getCostDifference()) > rnd) {
                maxCandidateIndex++;
            }

            int candidateIndexToPick = Configuration.getRnd().nextInt(maxCandidateIndex);
            bestCandidate = candidates.get(candidateIndexToPick);
        }

        if(completelyRandom) {
            Collections.shuffle(candidates, Configuration.getRnd());
            bestCandidate = candidates.get(0);
        }

        return bestCandidate;
        // best candidate!
        
//        viterbiPath[sourceIdx][targetIdx] = bestCandidate;
    }


    private List<ViterbiCell> collectViterbiCandidates(int sourceIdx, int targetIdx, List<Integer> sourceGlyphIndices, List<Integer> targetGlyphIndices) throws Exception  {
        List<ViterbiCell> candidates = new ArrayList();

        for (int si = sourceIdx; si >= 0 && si >= sourceIdx - Configuration.getInstance().getMaxGlyphsToAlign(); si--) {
            for (int ti = targetIdx; ti >= 0 && ti >= targetIdx - Configuration.getInstance().getMaxGlyphsToAlign(); ti--) {
                if (si == sourceIdx && ti == targetIdx) {
                    continue; // ignore the current loc
                }

                //sIdx & tIdx Glyph indexes --> 0=.   1=^   2=#
                int sIdx = getGlyphIndex(0, sourceIdx, si, sourceGlyphIndices);
                int tIdx = getGlyphIndex(1, targetIdx, ti, targetGlyphIndices);
                int thirdIndex = -1;
                
                if (impute) {
                    thirdIndex = alignator.getAlignmentMatrix().getMostProbableGlyphAlignmentByIndex(sIdx, tIdx, null, 2);                    
                }
                ViterbiCell parent = viterbiPath[si][ti];
                double cellCost;

                if (Configuration.getInstance().isUseFeatures()) {
                    //the cellCost is the cost of going from parent cell to the current cell
                    cellCost = getCellCostInContextModel(parent, sourceGlyphIndices, targetGlyphIndices,
                            sIdx, tIdx, si, ti);
                }

                else if(Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {

                    // border cells are not allowed
                    if ((sourceIdx > 0)  && (targetIdx > 0) && (si == 0 ^ ti == 0)) {
                        continue;
                    } else if (si == sourceGlyphIndices.size() ^ ti == targetGlyphIndices.size()) {
                        continue;
                    }

                    cellCost = getCellCost(parent, sIdx, tIdx);

                } else if (impute) {
                    cellCost = alignator.getAlignmentCostByIndex(sIdx, tIdx, thirdIndex);
                }
                else {
                    //si and ti are matrix indexes.                    
                    cellCost = getCellCost(parent, sIdx, tIdx);
                }

                if(completelyRandom && fixAlignment && si == ti) {
                    cellCost = 0.001;
                }

                double cost = parent.getCost() + cellCost;

                ViterbiCell vc = new ViterbiCell(parent, cost);

                vc.setParentDirection(Arrows.getDirection(sourceIdx - si, targetIdx - ti));

                vc.setGlyphIdx(0, sIdx);
                vc.setGlyphIdx(1, tIdx);
                if (impute) {                    
                    vc.setGlyphIdx(2, thirdIndex);
                }

                candidates.add(vc);
            }
        }
        
        return candidates;
    }

    private double getCellCostIn3DContextModel(ViterbiCell parent,
            Integer l0Idx, Integer l1Idx, Integer l2Idx) throws Exception {

        if (completelyRandom) {
            return alignator.getRandomFeatureAlignmentCost();
        }

        // using features
        // build alignment path from origin to this node -- THIS CONSTRUCTS PATH TO PARENT
        List<List<Integer>> pathTilNow = reconstructAlignmentPath(parent.getPathToStart());

        //  -- ADD SELF TO PATH
        pathTilNow.get(0).add(l0Idx);
        pathTilNow.get(1).add(l1Idx);
        pathTilNow.get(2).add(l2Idx);

        
               
        return alignator.getFeatureAlignmentCostByIndex(pathTilNow);

    }
    
    private double getCellCostInContextModel(ViterbiCell parent, List<Integer> sourceWord, List<Integer> targetWord,
            int sourceGlyphIdx, int targetGlyphIdx, int sourcePositionIndex, int targetPositionIndex) throws Exception {

        if (completelyRandom) {
            return alignator.getRandomFeatureAlignmentCost();
        }

        // using features

        // build alignment path from origin to this node -- THIS CONSTRUCTS PATH TO PARENT
        //In order to know the cell cost in a certain cell, we need the alignments up until this cell to get the context information
        List<List<Integer>> pathTilNow = reconstructAlignmentPath(parent.getPathToStart());

        //  -- ADD SELF TO PATH
        pathTilNow.get(0).add(sourceGlyphIdx);
        pathTilNow.get(1).add(targetGlyphIdx);


        //the index of pair of interest is now the last element in the list
        int positionOfCurrentGlyphInPath = pathTilNow.get(0).size()-1;



        if (!Configuration.getInstance().isCodeCompleteWordFirst()) {
            // this is the vertical (=normal) model
            return alignator.getFeatureAlignmentCostByIndex(pathTilNow, positionOfCurrentGlyphInPath, positionOfCurrentGlyphInPath);
        }

        //System.out.println("s: "+sourceGlyphIdx);

        //only this part is horizontal!!!
        int sp = sourcePositionIndex;
        int tp = targetPositionIndex;

        if (sourceGlyphIdx != Constants.DOT_INDEX) {
            sp++;
        }
        if (targetGlyphIdx != Constants.DOT_INDEX) {
            tp++;
        }

        pathTilNow.get(sourceLangId).addAll(sourceWord.subList(sp, sourceWord.size()));
        pathTilNow.get(targetLangId).addAll(targetWord.subList(tp, targetWord.size()));


        return alignator.getFeatureAlignmentCostByIndex(pathTilNow, positionOfCurrentGlyphInPath, positionOfCurrentGlyphInPath);

    }

    private double getCellCost(ViterbiCell parent, int sourceGlyphIdx, int targetGlyphIdx) throws Exception {
        if (completelyRandom) {
            double randProb = Configuration.getRnd().nextDouble();
            return -1.0 * EtyMath.base2Log(randProb);            
        }
        // alignment event \epsilon = (\sigma_i:\tau_j)
        // in Baseline model: this computes the cost of the event: L(e)
        return alignator.getAlignmentCostByIndex(sourceGlyphIdx, targetGlyphIdx);               
    }

    @Override
    public List<List<Integer>> reconstructAlignmentPath() {
        return reconstructAlignmentPath(getPathToStart());
    }
    
    /**
     * Find all the glyph-to-glyph alignments along the path
     * @param path
     * @return 
     */
    public List<List<Integer>> reconstructAlignmentPath(Stack<ViterbiCell> path) {
        // has to return a map

        List<List<Integer>> languageAlignments = new ArrayList();
        Input input = alignator.getInput();
        for(int languageId: input.getLanguageIds()) {
            languageAlignments.add(new ArrayList());
        }

        while (!path.isEmpty()) {
            ViterbiCell cell = path.pop();

            for(int languageId: input.getLanguageIds()) {
                List<Integer> alignmentPath = languageAlignments.get(languageId);
                String glyph = cell.getGlyph(languageId);
                if(glyph == null) {
                    continue;
                }

                alignmentPath.add(input.getVocabulary(languageId).getGlyphIndex(glyph));
            }
        }
        //System.out.println(languageAlignments);
        return languageAlignments;
    }

    private Stack<ViterbiCell> getPathToStart() {
        ViterbiCell lastCell;

        if(viterbiPath == null) {
            lastCell = l3ViterbiPath[l3ViterbiPath.length - 1][l3ViterbiPath[0].length - 1][l3ViterbiPath[0][0].length - 1];
        } else {
            //lastCell = viterbiPath[viterbiPath.length - 1][viterbiPath[0].length - 1];
            lastCell = finalCell;
        }
        //System.out.println("lastCell: " + lastCell);
        return lastCell.getPathToStart();
    }

    private void initCell(int x, int y, int z, List<Integer> l1Word, List<Integer> l2Word, List<Integer> l3Word) throws Exception {
        List<ViterbiCell> candidates =
                collectViterbiCandidates(x, y, z, l1Word, l2Word, l3Word);
        
        Collections.sort(candidates);
        ViterbiCell bestCandidate = candidates.get(0);

        if (alignator.isUseSimulatedAnnealing()) {
            double bestCost = bestCandidate.getCost();
            for (ViterbiCell candidate : candidates.subList(1, candidates.size())) {
                double costDiff = (candidate.getCost() - bestCost) / alignator.getCurrentTemperature();
                candidate.setCostDifference(costDiff);
            }

            double rnd = Configuration.getRnd().nextDouble();
            int maxCandidateIndex = 1;
            while (maxCandidateIndex < candidates.size()
                    && Math.exp(-1.0 * candidates.get(maxCandidateIndex).getCostDifference()) > rnd) {
                maxCandidateIndex++;
            }

            int candidateIndexToPick = Configuration.getRnd().nextInt(maxCandidateIndex);
            bestCandidate = candidates.get(candidateIndexToPick);
        }


        if(completelyRandom) {
            Collections.shuffle(candidates, Configuration.getRnd());
            bestCandidate = candidates.get(0);
        }

        // System.out.println("cand created..");
        // best candidate!
        l3ViterbiPath[x][y][z] = bestCandidate;
    }

    private List<ViterbiCell> collectViterbiCandidates(int x, int y, int z, 
            List<Integer> l1Word, List<Integer> l2Word, List<Integer> l3Word) throws Exception {
        List<ViterbiCell> candidates = new ArrayList();
        // System.out.println("Collecting candidates for (" + x + " " + y + " " + z + ")");

        for (int xi = x; xi >= 0 && xi >= x - Configuration.getInstance().getMaxGlyphsToAlign(); xi--) {
            for (int yi = y; yi >= 0 && yi >= y - Configuration.getInstance().getMaxGlyphsToAlign(); yi--) {
                for (int zi = z; zi >= 0 && zi >= z - Configuration.getInstance().getMaxGlyphsToAlign(); zi--) {
                    if (xi == x && yi == y && zi == z) {
                        continue; // ignore the current loc -- it is the parent
                    }
                    
                    ViterbiCell parent = l3ViterbiPath[xi][yi][zi];
                    // System.out.println("Parent (" + xi + " " + yi + " " + zi + "): " + parent);

                    int l1Idx = getGlyphIndex(0, x, xi, l1Word);
                    int l2Idx = getGlyphIndex(1, y, yi, l2Word);
                    int l3Idx = getGlyphIndex(2, z, zi, l3Word);

                    double cost;
                    if (Configuration.getInstance().isUseFeatures()) {
                        cost = parent.getCost() + getCellCostIn3DContextModel(parent, l1Idx, l2Idx, l3Idx);
                    } else {
                        cost = parent.getCost() + alignator.getAlignmentCostByIndex(l1Idx, l2Idx, l3Idx);
                    }

                    ViterbiCell vc = new ViterbiCell(parent, cost);

                    vc.setGlyphIdx(0, l1Idx);
                    vc.setGlyphIdx(1, l2Idx);
                    vc.setGlyphIdx(2, l3Idx);

                    candidates.add(vc);
                }
            }
        }

        // System.out.println("Total candidates " + candidates.size());
        return candidates;
    }

    /**
     * Get the previous (one or two) glyph(s) idex horizontally or vertically 
     * @param languageIdx
     * @param x: current cell where we are now
     * @param xi: the one preceding current cell , can be in either of the 3 directions
     * @param wordIndexes
     * @return
     * @throws RuntimeException 
     */
    private int getGlyphIndex(int languageIdx, int x, int xi, List<Integer> wordIndexes) throws RuntimeException {
        // TODO: IF WE ALLOW MORE THAN ONE STEP, VERIFY THAT PROBABILITIES ARE RETRIEVED USING MULTIGLYPH INDEXES!!
        //int sIdx = getGlyphIndex(0, sourceIdx, si, sourceGlyphIndices);
        int glyphIdx;
        switch (x - xi) {
            case 0:
                glyphIdx = Constants.DOT_INDEX;
                break;
            case 1:
                glyphIdx = wordIndexes.get(xi);
                break;
            case 2:
                // need to make this easier!!
                List<Integer> indices = new ArrayList();
                indices.add(wordIndexes.get(xi));
                indices.add(wordIndexes.get(xi + 1));
                String s = alignator.getInput().getWordFromIndexes(languageIdx, indices);
                //System.out.println("Retrieving indexes for glyph: " + s);
                glyphIdx = alignator.getInput().getVocabulary(languageIdx).getGlyphIndex(s);
                
                break;
            default:
                throw new RuntimeException("Not yet implemented, aligning more than 2 glyphs.");
        }
        
        return glyphIdx;
    }

    // TODO: CLEAN THIS UP!!

    private void initSpecialCase(List<Integer> l1Word, List<Integer> l2Word, List<Integer> l3Word) throws Exception {
        if (l1Word == null) {
            this.missingLanguageIds = Arrays.asList(0);
            sourceWord = l2Word;
            sourceLangId = 1;
            targetWord = l3Word;
            targetLangId = 2;
        } else if (l2Word == null) {
            this.missingLanguageIds = Arrays.asList(1);
            sourceWord = l1Word;
            sourceLangId = 0;
            targetWord = l3Word;
            targetLangId = 2;
        } else if (l3Word == null) {
            this.missingLanguageIds = Arrays.asList(2);
            sourceWord = l1Word;
            sourceLangId = 0;
            targetWord = l2Word;
            targetLangId = 1;
        }

        viterbiPath = new ViterbiCell[sourceWord.size() + 1][targetWord.size() + 1];

        // init root
        initRoot();

        // init rest
        for (int x = 0; x < viterbiPath.length; x++) {
            for (int y = 0; y < viterbiPath[0].length; y++) {
                if(viterbiPath[x][y] != null) {
                    continue;
                }

                initCell(x, y, sourceWord, targetWord, missingLanguageIds);
            }
        }
    }

    private void initCell(int sourceIdx, int targetIdx, List<Integer> sourceWord, List<Integer> targetWord, List<Integer> missingLangIds) throws Exception {
        List<ViterbiCell> candidates =
                collectViterbiCandidates(sourceIdx, targetIdx, sourceWord, targetWord, missingLangIds);
        Collections.sort(candidates);
        ViterbiCell bestCandidate = candidates.get(0);

        if (alignator.isUseSimulatedAnnealing()) {
            double bestCost = bestCandidate.getCost();
            for (ViterbiCell candidate : candidates.subList(1, candidates.size())) {
                double costDiff = (candidate.getCost() - bestCost) / alignator.getCurrentTemperature();
                candidate.setCostDifference(costDiff);
            }

            double rnd = Configuration.getRnd().nextDouble();
            int maxCandidateIndex = 1;
            while (maxCandidateIndex < candidates.size()
                    && Math.exp(-1.0 * candidates.get(maxCandidateIndex).getCostDifference()) > rnd) {
                maxCandidateIndex++;
            }

            int candidateIndexToPick = Configuration.getRnd().nextInt(maxCandidateIndex);
            bestCandidate = candidates.get(candidateIndexToPick);
        }

        // best candidate!
        viterbiPath[sourceIdx][targetIdx] = bestCandidate;
    }

    private List<ViterbiCell> collectViterbiCandidates(int sourceIdx, int targetIdx, List<Integer> sourceWord, List<Integer> targetWord, List<Integer> missingLangIds) throws Exception {
        List<ViterbiCell> candidates = new ArrayList();

        int sourceLanguageId = -1;
        int targetLanguageId = -1;

        if(missingLangIds.size() != 1) {
            // should be exactly one language missing
            throw new IllegalArgumentException("Handling a special case with wrong parameters.");

        }

        if(this.missingLanguageIds.contains(0)) {
            sourceLanguageId = 1;
            targetLanguageId = 2;
        } else if (this.missingLanguageIds.contains(1)) {
            sourceLanguageId = 0;
            targetLanguageId = 2;
        } else if (this.missingLanguageIds.contains(2)) {
            sourceLanguageId = 0;
            targetLanguageId = 1;
        }

        for (int si = sourceIdx; si >= 0 && si >= sourceIdx - Configuration.getInstance().getMaxGlyphsToAlign(); si--) {
            for (int ti = targetIdx; ti >= 0 && ti >= targetIdx - Configuration.getInstance().getMaxGlyphsToAlign(); ti--) {
                if (si == sourceIdx && ti == targetIdx) {
                    continue; // ignore the current loc
                }

                int sIdx = getGlyphIndex(sourceLanguageId, sourceIdx, si, sourceWord);
                int tIdx = getGlyphIndex(targetLanguageId, targetIdx, ti, targetWord);

                ViterbiCell parent = viterbiPath[si][ti];

                double cost = parent.getCost();
                if (useFeatures) {
                    if (this.missingLanguageIds.contains(0)) {
                        cost += getCellCostIn3DContextModel(parent, null, sIdx, tIdx);
                    } else if (this.missingLanguageIds.contains(1)) {
                        cost += getCellCostIn3DContextModel(parent, sIdx, null, tIdx);
                    } else if (this.missingLanguageIds.contains(2)) {
                        cost += getCellCostIn3DContextModel(parent, sIdx, tIdx, null);
                    }
                } else {
                    if (this.missingLanguageIds.contains(0)) {
                        cost += alignator.getAlignmentCostByIndex(null, sIdx, tIdx);
                    } else if (this.missingLanguageIds.contains(1)) {
                        cost += alignator.getAlignmentCostByIndex(sIdx, null, tIdx);
                    } else if (this.missingLanguageIds.contains(2)) {
                        cost += alignator.getAlignmentCostByIndex(sIdx, tIdx, null);
                    }
                }
                
                ViterbiCell vc = new ViterbiCell(parent, cost);
                vc.setParentDirection(Arrows.getDirection(sourceIdx - si, targetIdx - ti));

                vc.setGlyph(sourceLanguageId, alignator.getInput().getVocabulary(sourceLanguageId).getGlyph(sIdx));
                vc.setGlyph(targetLanguageId, alignator.getInput().getVocabulary(targetLanguageId).getGlyph(tIdx));

//                vc.setX(alignator.getInput().getVocabulary(sourceLanguageId).getGlyph(sIdx));
//                vc.setY(alignator.getInput().getVocabulary(targetLanguageId).getGlyph(tIdx));
                candidates.add(vc);
            }
        }

        return candidates;
    }


    @Override
    public String toString() {
//        if (useFeatures) {
//            return toContextViterbiMatrixString();
//        }
        if (l3ViterbiPath != null) {
            return to3DViterbiMatrixString() + getPathToParent();
        } else {
            return to2DViterbiMatrixString();
        }
    }

    private String to3DViterbiMatrixString() {
        StringBuilder sb = new StringBuilder();
        int level = 1;
        for(ViterbiCell[][] layer: l3ViterbiPath) {
            sb.append(" layer ").append(level).append("\n");
            level++;

            for(ViterbiCell[] row: layer) {
                for(ViterbiCell cell: row) {
                    sb.append(StringUtils.rightAlign(16, cell.toViterbiMatrixCellString()));
                }
                sb.append("\n");
            }

            sb.append("\n\n");
        }

        return sb.toString();
    }

    private String to2DViterbiMatrixString() {
        StringBuilder sb = new StringBuilder();


        sb.append("  ");
        sb.append(StringUtils.rightAlign(16, "."));
        for(int targetGlyphIdx: targetWord) {
            sb.append(StringUtils.rightAlign(16, alignator.getInput().getVocabulary(targetLangId).getGlyph(targetGlyphIdx)));
        }
        sb.append("\n");


        int rowId = 0;

        for(ViterbiCell[] row: viterbiPath) {
            sb.append(" ");
            if(rowId == 0) {
                sb.append(".");
            } else {
                sb.append(alignator.getInput().getVocabulary(sourceLangId).getGlyph(sourceWord.get(rowId-1)));
            }

            rowId++;

            for(ViterbiCell cell: row) {

                if (cell == null) {
                    sb.append(StringUtils.rightAlign(16, "-"));
                } else {
                    sb.append(StringUtils.rightAlign(16, cell.toViterbiMatrixCellString()));
                }
                if(!completelyRandom){
                    if(row == viterbiPath[viterbiPath.length - 1] && cell == row[row.length - 1]){
                        //Last iteration
                        sb.append("  --> " + getTotalCost());
                    }
                }
            }

            sb.append("\n");
        }
        return sb.toString();
    }





    private String toContextViterbiMatrixString() {

        StringBuilder sb = new StringBuilder();


        sb.append("       ");
        sb.append(StringUtils.rightAlign(16, "."));
        for(int targetGlyphIdx: targetWord) {
            sb.append(StringUtils.rightAlign(16, alignator.getInput().getVocabulary(targetLangId).getGlyph(targetGlyphIdx)));
        }
        sb.append("\n");


        int rowId = 0;

        for(ViterbiCell[] row: viterbiPath) {
            sb.append(" ");
            if(rowId == 0) {
                sb.append(StringUtils.rightAlign(7,"."));
            } else {
                sb.append(StringUtils.rightAlign(7,alignator.getInput().getVocabulary(sourceLangId).getGlyph(sourceWord.get(rowId-1))));
            }

            rowId++;

            for(ViterbiCell cell: row) {
                //sb.append(rightAlign(16, cell.getDebugString()));
                sb.append(StringUtils.rightAlign(16, cell.toViterbiMatrixCellString()));
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private String getPathToParent() {
        if(l3ViterbiPath == null) {
            return getPathToParent(viterbiPath);
        }

        StringBuilder sb = new StringBuilder();
        int level = 1;
        for(ViterbiCell[][] layer: l3ViterbiPath) {
            sb.append(" layer ").append(level).append("\n");
            level++;

            sb.append(getPathToParent(layer)).append("\n");
        }
        return sb.toString();
    }

    private String getPathToParent(ViterbiCell[][] layer) {
        StringBuilder sb = new StringBuilder();
        for (ViterbiCell[] row : layer) {
            for (ViterbiCell cell : row) {
                sb.append(StringUtils.rightAlign(10, cell.getPathString()));
            }

            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Javad Nouri
     * nouri [a t] cs dot helsinki dot fi
     * This computes total cost of cell + word boundary coding cost
     * @param finalCell
     * @return 
     */
    private double computeWordBoundaryCostToo(ViterbiCell finalCell) throws Exception {
        if(completelyRandom){
            return -1;
        }

        // using features

        // build alignment path from origin to this node -- THIS CONSTRUCTS PATH TO PARENT
        //In order to know the cell cost in a certain cell, we need the alignments up until this cell to get the context information
        List<List<Integer>> pathTilNow = reconstructAlignmentPath(finalCell.getPathToStart());

        //  -- ADD SELF TO PATH
        int sourceEnd = ((FeatureVocabulary)Input.getInstance().getVocabulary(sourceLangId)).getFeatureVocabularySize() - 1;
        int targetEnd = ((FeatureVocabulary)Input.getInstance().getVocabulary(targetLangId)).getFeatureVocabularySize() - 1;
        //System.out.println("SourceEnd: " + sourceEnd);
        //System.out.println("TargetEnd: " + targetEnd);
        pathTilNow.get(0).add(sourceEnd); //End of word
        pathTilNow.get(1).add(targetEnd);
        
        /*for(int i = 0; i < pathTilNow.get(0).size() ; i++){
            System.out.println(pathTilNow.get(0).get(i) + "\t" + pathTilNow.get(1).get(i));            
        }*/

        //Let's print it
        /*StringBuilder out = new StringBuilder();
        for(int i = 0 ; i < pathTilNow.get(0).size() ; i++){
            out.append(Input.getInstance().getVocabulary(sourceLangId).getGlyph(pathTilNow.get(0).get(i)) + " ");
        }
        out.append("\n");
        for(int i = 0 ; i < pathTilNow.get(0).size() ; i++){
            out.append(Input.getInstance().getVocabulary(targetLangId).getGlyph(pathTilNow.get(1).get(i)) + " ");
        }
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine(out.toString());
        //the index of pair of interest is now the last element in the list
        //int positionOfCurrentGlyphInPath = 0;//pathTilNow.get(0).size()-1;
        * */
        
        double cost = 0;
        for(int positionOfCurrentGlyphInPath = 0 ; positionOfCurrentGlyphInPath < pathTilNow.get(0).size() ; positionOfCurrentGlyphInPath++){
            cost += alignator.getFeatureAlignmentCostByIndex(pathTilNow, positionOfCurrentGlyphInPath, positionOfCurrentGlyphInPath);
        }
        return cost;

    }

  
}
