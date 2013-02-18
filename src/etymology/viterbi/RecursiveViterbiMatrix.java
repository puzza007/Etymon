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

import etymology.config.Constants;
import etymology.config.Configuration;
import etymology.cost.AlignmentCostFunction;
import etymology.input.Input;
import etymology.util.Matrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

/**
 *
 * @author arto
 */
public class RecursiveViterbiMatrix {

    private Matrix<ViterbiCell> viterbiMatrix;
    private Map<Integer, List<Integer>> languageIdToWordIndexes;
    
    private boolean randomAlignments;
    private boolean useSimulatedAnnealing;
    private double currentTemperature;
    
    private AlignmentCostFunction alignmentCostFunction;

    public RecursiveViterbiMatrix(Map<Integer, List<Integer>> languageIdToWordIndexes, AlignmentCostFunction alignmentCostFunction) {
        this(languageIdToWordIndexes, alignmentCostFunction, false);
    }

    public RecursiveViterbiMatrix(Map<Integer, List<Integer>> languageIdToWordIndexes, AlignmentCostFunction alignmentCostFunction, double simulatedAnnealingTemperature) {
        this(languageIdToWordIndexes, alignmentCostFunction, simulatedAnnealingTemperature, false);
    }

    public RecursiveViterbiMatrix(Map<Integer, List<Integer>> languageIdToWordIndexes, AlignmentCostFunction alignmentCostFunction, boolean randomAlignments) {
        this(languageIdToWordIndexes, alignmentCostFunction, -1, randomAlignments);
    }

    private RecursiveViterbiMatrix(Map<Integer, List<Integer>> languageIdToWordIndexes, AlignmentCostFunction alignmentCostFunction, double simulatedAnnealingTemperature, boolean randomAlignments) {
        if(simulatedAnnealingTemperature > 0) {
            this.useSimulatedAnnealing = true;
            this.currentTemperature = simulatedAnnealingTemperature;
        }

        this.languageIdToWordIndexes = languageIdToWordIndexes;
        this.alignmentCostFunction = alignmentCostFunction;
        this.randomAlignments = randomAlignments;
        generateMatrix();
        initRoot();

        // now we have a root at 0, 0, ..., 0 -> we can fill the matrix from end
        fillMatrix();
    }


    public ViterbiCell getViterbiCell(List<Integer> indexes) {
        return getCell(indexes);
    }

    private void generateMatrix() {
        List<Integer> dimensionSizes = new ArrayList();

        for (Integer languageId: languageIdToWordIndexes.keySet()) {
            int dimensionSize = 1;
            if (languageIdToWordIndexes.get(languageId) != null) {
                dimensionSize += languageIdToWordIndexes.get(languageId).size();
            }

            dimensionSizes.add(dimensionSize);
        }

        viterbiMatrix = new Matrix<ViterbiCell>(ViterbiCell.class, dimensionSizes);
    }

    private void initRoot() {
        // create root location by adding indexes for it, root at 0, 0, ..., 0
        List<Integer> indexes = new ArrayList();
        for (int i = 0; i < viterbiMatrix.getDimensionCount(); i++) {
            indexes.add(0);
        }

        // create a root, and put it to the root location
        viterbiMatrix.setCell(ViterbiCell.createRoot(), indexes);
    }

    /*
     * Matrix is filled by collecting viterbi candidates for the last index. Collecting candidates
     * uses getCell-function that creates a cell if one is missing. 
     */
    private void fillMatrix() {
        List<Integer> lastCellIndex = new ArrayList();
        for (int dimId = 0; dimId < viterbiMatrix.getDimensionCount(); dimId++) {
            lastCellIndex.add(viterbiMatrix.getDimensionLength(dimId) - 1);
        }

        collectViterbiCandidates(lastCellIndex);
    }

    private ViterbiCell getCell(List<Integer> indexes) {
        ViterbiCell cell = viterbiMatrix.getCell(indexes);
        
        if(cell == null) {
            initCell(indexes);
            cell = viterbiMatrix.getCell(indexes);
        }

        return cell;
    }

    private void initCell(List<Integer> indexes) {
        List<ViterbiCell> candidates = collectViterbiCandidates(indexes);

        Collections.sort(candidates);
        ViterbiCell bestCandidate = candidates.get(0);

        if (!randomAlignments && useSimulatedAnnealing) {
            bestCandidate = getBestCandidateBySimulatedAnnealing(candidates, bestCandidate.getCost());
        }

        if (randomAlignments) {
            Collections.shuffle(candidates, Configuration.getRnd());
            bestCandidate = candidates.get(0);
        }

        viterbiMatrix.setCell(bestCandidate, indexes);
    }

    private List<ViterbiCell> collectViterbiCandidates(List<Integer> wordIndexes) {
        List<ViterbiCell> candidates = new ArrayList();
        collectViterbiCandidates(candidates, wordIndexes, new ArrayList());
        return candidates;
    }

    private void collectViterbiCandidates(List<ViterbiCell> cells, List<Integer> wordIndexes, List<Integer> indexStack) {
        if (indexStack.size() == wordIndexes.size()) {
            if(indexStack.equals(wordIndexes)) {
                // ignore the cell that we start the search from
                return;
            }

            ViterbiCell candidate = createCandidateCell(wordIndexes, indexStack);
            cells.add(candidate);
            return;
        }


        // if the stack that we use for retrieving the candidates is smaller than
        // the total index count, we have not yet reached the data -- go deeper
        int currentIndexId = indexStack.size();
        int currWordIdx = wordIndexes.get(currentIndexId);

        for (int wordIdx = currWordIdx; wordIdx >= currWordIdx - Configuration.getInstance().getMaxGlyphsToAlign() && wordIdx >= 0; wordIdx--) {
            indexStack.add(wordIdx);
            collectViterbiCandidates(cells, wordIndexes, indexStack);
            indexStack.remove(indexStack.size() - 1);
        }
    }

    private ViterbiCell getBestCandidateBySimulatedAnnealing(List<ViterbiCell> candidates, double bestCandidateCost) {
        double bestCost = bestCandidateCost;
        
        for (ViterbiCell candidate : candidates.subList(1, candidates.size())) {
            double costDifference = (candidate.getCost() - bestCost) / currentTemperature;
            candidate.setCostDifference(costDifference);
        }

        double rnd = Configuration.getRnd().nextDouble();

        int maxCandidateIndex = 1;
        while (maxCandidateIndex < candidates.size()
                && Math.exp(-1.0 * candidates.get(maxCandidateIndex).getCostDifference()) > rnd) {
            maxCandidateIndex++;
        }

        int candidateIndexToPick = Configuration.getRnd().nextInt(maxCandidateIndex);
        return candidates.get(candidateIndexToPick);
    }

    private ViterbiCell createCandidateCell(List<Integer> wordIndexes, List<Integer> indexStack) {
        ViterbiCell parent = getCell(indexStack);

        Map<Integer, Integer> languageIdSpecificGlyphIndexes = new TreeMap();
        for(int languageId: languageIdToWordIndexes.keySet()) {
            Integer glyphIndex = getGlyphIndex(languageId, wordIndexes.get(languageId), indexStack.get(languageId), this.languageIdToWordIndexes.get(languageId));
            languageIdSpecificGlyphIndexes.put(languageId, glyphIndex);
        }

        double cost = getCellCost(parent, languageIdSpecificGlyphIndexes);
        ViterbiCell candidate = new ViterbiCell(parent, cost, languageIdSpecificGlyphIndexes);

        return candidate;
    }

    private int getGlyphIndex(int languageIdx, int x, int xi, List<Integer> wordIndexes) throws RuntimeException {
        int glyphIdx;
        switch (x - xi) {
            case 0:
                glyphIdx = Constants.DOT_INDEX;
                break;
            case 1:
                glyphIdx = wordIndexes.get(xi);
                break;
            case 2:
                Input input = Input.getInstance();
                if(Configuration.getInstance().isUseFeatures()) {
                    throw new RuntimeException("Cannot do nxn alignment using features (yet!).");
                }

                // TODO: too complex logic for a simple task, fix
                List<Integer> indices = Arrays.asList(wordIndexes.get(xi), wordIndexes.get(xi + 1));
                String s = input.getWordFromIndexes(languageIdx, indices);
                glyphIdx = input.getVocabulary(languageIdx).getGlyphIndex(s);
                break;
            default:
                throw new RuntimeException("Aligning more than 2 glyphs not supported (Not yet implemented), aligning more than 2 glyphs.");
        }

        return glyphIdx;
    }

    private double getCellCost(ViterbiCell parent, Map<Integer, Integer> languageSpecificGlyphIndexes) {
        double costFromParent = parent.getCost();

        if (!Configuration.getInstance().isUseFeatures()) {
            return costFromParent + alignmentCostFunction.getAlignmentCost(languageSpecificGlyphIndexes);
        }

        if (randomAlignments) {
            return costFromParent + Math.random();
        }

        // using features

        // build alignment path from origin to this node -- THIS CONSTRUCTS PATH TO PARENT
        Map<Integer, List<Integer>> pathTilNow = reconstructAlignmentPath(parent.getPathToStart());

        // add self to path
        for (int languageId : pathTilNow.keySet()) {
            Integer glyphIndex = languageSpecificGlyphIndexes.get(languageId);
            pathTilNow.get(languageId).add(glyphIndex);
        }
       
        return costFromParent + alignmentCostFunction.getFeatureAlignmentCostByGlyphIndexes(pathTilNow, languageSpecificGlyphIndexes);
    }

    public Map<Integer, List<Integer>> reconstructAlignmentPath(Stack<ViterbiCell> path) {
        Map<Integer, List<Integer>> languageAlignments = new TreeMap();
        for(int languageId: languageIdToWordIndexes.keySet()) {
            languageAlignments.put(languageId, new ArrayList());
        }

        while (!path.isEmpty()) {
            ViterbiCell cell = path.pop();

            for (int languageId : languageIdToWordIndexes.keySet()) {
                List<Integer> alignmentPath = languageAlignments.get(languageId);
                alignmentPath.add(cell.getGlyphIdx(languageId));
            }
        }

        return languageAlignments;
    }
}
