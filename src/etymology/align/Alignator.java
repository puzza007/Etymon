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
import etymology.config.Constants;
import etymology.align.matrices.MarginalAlignmentMatrix;
import etymology.align.matrices.JointAlignmentMatrix;
import etymology.align.matrices.MultiLangMarginalAlignmentMatrix;
import etymology.align.matrices.TwoLangAlignmentMatrix;
import etymology.align.matrices.TwoLangAlignmentMatrixWithBoundaryMatrix;
import etymology.context.FeatureAlignmentMatrix;
import etymology.context.FeatureTree;
import etymology.context.MarginalFeatureAlignmentMatrix;
import etymology.cost.*;
import etymology.data.convert.FeatureConverter;
import etymology.input.GlyphVocabulary;
import etymology.viterbi.ViterbiMatrix;
import etymology.input.Input;
import etymology.logging.StaticLogger;
import etymology.output.GnuPlotPrinter;
import etymology.output.AlignmentPrinter;
import etymology.util.ChangeStatus;
import etymology.util.CollectionUtil;
import etymology.util.EtyMath;
import etymology.viterbi.IViterbiMatrix;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author arto
 */
public class Alignator {

    private static Alignator instance;
    private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final Logger FINAL_LOGGER = Logger.getLogger(Constants.FINAL_LOGGER_NAME);
    private Input input;
    private AlignmentMatrix alignmentMatrix;
    private FeatureAlignmentMatrix featureAlignmentMatrix;
    private AlignmentRegistry alignmentRegistry;
    private double currentTemperature;
    private double cost;
    private Double previousCost;
    private double bestCost = Double.MAX_VALUE;
    private CostHandler costHandler;
    private int iteration = 0;
    private int noAlignmentChangesInNIterations = 0;
    private int noCostChangesInNIterations = 0;
    
    
    private boolean printCostInfoOnly = false;    
    private boolean useSimulatedAnnealing;
    private boolean disableBasicLogging = false;

    private Configuration configuration;

    private double finalCost;

    private boolean executeSanityChecks = true;
    private boolean executingSanityChecks = false;

    private WordAlignment[] bestAlignments;
    

    //Constructor with no input
     public Alignator(Configuration config) throws Exception {
        this(config, new Input(config));
    }
    //Constructor with input
    public Alignator(Configuration config, Input input) throws Exception {
        this.configuration = config;
        this.input = input;
        
        init();
    }

    public Logger getFinalLogger() {
        return FINAL_LOGGER;
    }

    public void setExecuteSanityChecks(boolean executeSanityChecks) {
        this.executeSanityChecks = executeSanityChecks;
    }

    public static Alignator getInstance() {
        return instance;
    }

    public double getFinalCost() {
        return finalCost;
    }

    public static CostHandler getCostHandler() {
        return getInstance().costHandler;
    }

   

    public AlignmentMatrix getAlignmentMatrix() {
        if (configuration.isUseFeatures()) {
            return featureAlignmentMatrix;
        }

        return alignmentMatrix;
    }

    public FeatureAlignmentMatrix getFeatureAlignmentMatrix() {
        return featureAlignmentMatrix;
    }

    public Input getInput() {
        return input;
    }

    public boolean isUseSimulatedAnnealing() {
        return useSimulatedAnnealing;
    }

    public double getCurrentTemperature() {
        return currentTemperature;
    }

    public AlignmentRegistry getAlignmentStorage() {
        return alignmentRegistry;
    }

    public void align() throws Exception {        
        if (configuration.getIterationNumber() == 0 && !printCostInfoOnly) {
            StaticLogger.printStatistics(LOG, input);
        }
        
        executeAlignmentIterations();        
        doFinalStuff();        

        // determine whether to switch to feature alignment
        // if we don't want to switch to features -- stop here
        if (!configuration.isFirstBaselineThenContext()) {
            return;
        }

        // if we already were using features -- stop here
        if (configuration.isUseFeatures()) {
            return;
        }

        // feature alignment is only ok for 2 lang alignment
        if (!configuration.getAlignmentType().equals(AlignmentMatrixType.TWO_LANG)) {
            return;
        }

        doItAgainUsingFeatures();


    }

    private void doFinalStuff() throws Exception {
        if (configuration.getIterationNumber() != 0) {
            return;
        }
        System.out.println("Printing final output..");

        StaticLogger.printStatistics(FINAL_LOGGER, input);
        StaticLogger.logFinalOutput();
        if(!executingSanityChecks) {
            GnuPlotPrinter.printGnuPlotData();
        }

        printLanguageAlignmentCost();

        executeSanityChecks();
    }

    public void doItAgainUsingFeatures() throws Exception {
        
        System.out.println("COST OF BASELINE: " + bestCost);
        
        configuration.setUseFeatures(true);
        configuration.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);

        configuration.setTreeRebuildingFrequency(0);
        this.executeSanityChecks = false;
        configuration.setUseSimulatedAnnealing(false);
        useSimulatedAnnealing = false;
        initSpecialCase();
        align();

    }

    private void executeAlignmentIterations() throws Exception {
        
        // generate random alignments
        if (!configuration.isUseFeatures()) {
            generateRandomAlignments();
        } else if (configuration.isUseFeatures() == true && 
                !configuration.isFirstBaselineThenContext()) {
            generateRandomFeatureAlignments(); //ok
        }

        int greedyPrinter = 0;
        while (continueIterating()) {
            if (!isUseSimulatedAnnealing()) {                
                greedyPrinter++;
            }

            handleIterationChange();
            alignWordsIncremental();
            
            getCost();            
            printCost();

            if (configuration.getIterationNumber() == 0) {
                LOG.fine("");
            }
            storeBestCostAlignment();
        }

        if (configuration.isUseFeatures() || configuration.isFirstBaselineThenContext()) {
            System.out.println("Number of iterations total: " + iteration);
            System.out.println("Number of greedy Iterations: " + greedyPrinter);
        }
        //System.out.println("Number of iterations without change in cost: " + this.noCostChangesInNIterations);

        if (configuration.getIterationNumber() == 0) {
            LOG.fine("Retrieving final cost for best alignments.");
        }
        getCost();
        printCost();

        if (!executingSanityChecks) {
            finalCost = cost;
        }
    }


    private void storeBestCostAlignment() {
        if (executingSanityChecks) {
            return;
        }

        if(bestCost <= cost) {
            return;
        }

        bestCost = cost;
        WordAlignment[] currentAlignment = alignmentRegistry.getAlignments();
        bestAlignments = Arrays.copyOf(currentAlignment, currentAlignment.length);
    }



    private void generateRandomAlignments() {
        for (int wordIndex = 0; wordIndex < input.getNumOfWords(); wordIndex++) {
            // reregister just in case
            alignmentRegistry.deregisterAlignment(alignmentMatrix, wordIndex); //ok
            IViterbiMatrix vm = new ViterbiMatrix(this);
            if(Configuration.getInstance().getLanguages().size() > 3) {
                // vm = new RecursiveViterbiMatrix(languageIdToWordIndexes, alignmentCostFunction);
            } else {
                vm = new ViterbiMatrix(this); //ok
            }
           
            vm.setCompletelyRandom(true);
            
            vm.init(input, wordIndex);
            alignmentRegistry.registerAlignment(alignmentMatrix, wordIndex, vm);
        }
    }

    private void generateRandomFeatureAlignments() throws Exception {

        if (configuration.getIterationNumber() == 0) {
            LOG.fine("The first random alignments:\n");
        }
        
        for (int wordIndex = 0; wordIndex < input.getNumOfWords(); wordIndex++) {
            ViterbiMatrix vm = new ViterbiMatrix(this); //ok
            vm.setCompletelyRandom(true);
            
            // run through the viterbi algorithm and get the full alignments 
            vm.init(input, wordIndex);
            
            //put statistic information in featureAlignmentMatrix also ---Lv
            alignmentRegistry.registerFirstRoundFeatureAlignment(featureAlignmentMatrix, wordIndex, vm);

            if (disableBasicLogging) {
                continue;
            }

            if (configuration.getIterationNumber() == 0) {
                StaticLogger.logAlignment(input, alignmentRegistry, wordIndex, vm, false, true);
            }
        }

        featureAlignmentMatrix.buildTrees();

        LOG.fine("\nThe trees that are built according to the random alignments: \n");
        for (FeatureTree ft : featureAlignmentMatrix.getTrees()) {
            LOG.fine(ft.toString());
        }
    }

    public boolean isDisableBasicLogging() {
        return disableBasicLogging;
    }

    private void handleIterationChange() {
        
        AlignmentPrinter ap = new AlignmentPrinter(getAlignmentMatrix(), input);
                
        iteration++;
        LOG.fine(ap.getPrintableAlignmentMatrix(getAlignmentMatrix()));
        if (configuration.getIterationNumber() == 0) {
            if (!printCostInfoOnly) {
                LOG.fine(ap.getPrintableAlignmentMatrix(getAlignmentMatrix()));
            }
            
            LOG.log(Level.FINE, "ITERATION: {0}", iteration);
            
            if (useSimulatedAnnealing) {
                LOG.log(Level.FINE, "TEMPERATURE: {0}", currentTemperature);
            }
        }
        
        if (useSimulatedAnnealing) {            
            currentTemperature *= Constants.TEMPERATURE_MULTIPLIER;
        }

        disableSimulatedAnnealingIfConverged();
    }

    private void getCost() {
        previousCost = cost;

        // get current alignment cost
        cost = costHandler.getGlobalCost();
        //if (cost == previousCost) {
        if (Math.abs(cost-previousCost) < 1 && getCurrentTemperature() < 1) {
            noCostChangesInNIterations++;
        } else {
            noCostChangesInNIterations = 0;
        }
    }

    private boolean continueIterating() {
        if (useSimulatedAnnealing && currentTemperature > Constants.STOP_AT_TEMPERATURE) {
            return true;
        }

        if (previousCost == null) {
            return true;
        }

        if (cost < previousCost) {
            return true;
        }



        return false;
    }

    private void disableSimulatedAnnealingIfConverged() {
        if (noAlignmentChangesInNIterations <= 0 && noCostChangesInNIterations <= 0) {
            return;
        }

        boolean cond1 = disableSimulatedAnnealingIfNoAlignmentChanges();
        boolean cond2 = disableSimulatedAnnealingIfNoCostChanges();

        //if (configuration.isUseFeatures()) {
            if (cond1 || cond2) {
                configuration.setUseSimulatedAnnealing(false);
                System.out.println("Simann disabled");
            }
        //}
    }

    private boolean disableSimulatedAnnealingIfNoAlignmentChanges() {

        if (noAlignmentChangesInNIterations <= 0) {
            return false;
        }

        if (configuration.getIterationNumber() == 0) {
            LOG.log(Level.FINE, "No viterbi alignment changes in {0} iterations", noAlignmentChangesInNIterations);
        }

        if (noAlignmentChangesInNIterations >= Constants.DISABLE_SIMANN_AFTER_NUM_OF_NO_CHANGES) {
            //LOG.info("Disabling simulated annealing due to no alignment changes in N iterations.");
            if (configuration.getIterationNumber() == 0) {
                LOG.fine("Disabling simulated annealing due to no alignment changes.");
            }
            
            useSimulatedAnnealing = false;
            return true;
        }

        return false;
    }

    private boolean disableSimulatedAnnealingIfNoCostChanges() {
        if (noCostChangesInNIterations <= 0) {
            return false;
        }

        if (configuration.getIterationNumber() == 0) {
            LOG.log(Level.FINE, "No cost changes in {0} iterations", noCostChangesInNIterations);
        }
        if (noCostChangesInNIterations >= Constants.DISABLE_SIMANN_AFTER_NUM_OF_NO_COST_CHANGES) {
            //LOG.info("Disabling simulated annealing due to no cost changes.");
            if (configuration.getIterationNumber() == 0) {
                LOG.fine("Disabling simulated annealing due to no cost changes.");
            }
            useSimulatedAnnealing = false;
            return true;
        }
        return false;
    }

    private boolean alignWordsIncremental() throws Exception {
        boolean alignmentsChanged = false;
        for (Integer wordIndex = 0; wordIndex < input.getNumOfWords(); wordIndex++) {
            ChangeStatus status;

            // if we are imputing a specific word, do not align it
            if(Configuration.getInstance().getImputeWordAtIndex() == wordIndex) {
                continue;
            }
            
            if (configuration.isUseFeatures()) {
                status = alignFeatureVectors(wordIndex); 
            } else {
                // Perform Alignment here: no features, symbol-level alignment
                status = alignCognates(wordIndex);
            }

            if (ChangeStatus.CHANGED == status) {
                alignmentsChanged = true;
            }
        }

        //rebuild the trees always after aligning all words
        if (configuration.isUseFeatures()) {
            if (configuration.getIterationNumber() == 0) {
                LOG.fine("All words aligned");
            }
            featureAlignmentMatrix.rebuildTrees();
        }

        if (alignmentsChanged) {
            noAlignmentChangesInNIterations = 0;
        } else {
            noAlignmentChangesInNIterations++;
        }

        return alignmentsChanged;
    }



    public double getFeatureAlignmentCostByIndex(List<List<Integer>> alignmentPathTillThisCell, Collection<Integer> glyphIndexes) throws Exception {
        return getFeatureAlignmentCostByIndex(alignmentPathTillThisCell, CollectionUtil.toIntArray(glyphIndexes));
    }

    public double getFeatureAlignmentCostByIndex(List<List<Integer>> alignmentPathTillThisCell, int... glyphIndexes) throws Exception {
        if (glyphIndexes.length == 0) {
            return featureAlignmentMatrix.getAlignmentCostByIndex(alignmentPathTillThisCell);
        }
        return getFeatureAlignmentCostByIndex(alignmentPathTillThisCell, glyphIndexes[0], glyphIndexes[1]);
    }

    public double getFeatureAlignmentCostByIndex(List<List<Integer>> alignmentPathTillThisCell, int langOneGlyphPositionInWord, int langTwoGlyphPositionInWord) throws Exception {
        return featureAlignmentMatrix.getAlignmentCostByIndex(alignmentPathTillThisCell, langOneGlyphPositionInWord, langTwoGlyphPositionInWord);
        //return -1.0 * EtyMath.base2Log(getFeatureAlignmentProbabilityByIndex(alignmentPathTillThisCell, langOneGlyphIndex, langTwoGlyphIndex));
    }

    public double getFeatureAlignmentProbabilityByIndex(List<List<Integer>> alignmentPathTillThisCell, int langOneGlyphPositionInWord, int langTwoGlyphPositionInWord) throws Exception {
        return featureAlignmentMatrix.getAlignmentProbabilityByIndex(alignmentPathTillThisCell, langOneGlyphPositionInWord, langTwoGlyphPositionInWord);
    }

    public double getRandomFeatureAlignmentCost() {
        return -1.0 * EtyMath.base2Log(featureAlignmentMatrix.getRandomAlignmentProbability());
    }

    public double getSuffixCost(List<Integer> suffix, int language) {
        return getAlignmentMatrix().getSuffixCost(suffix, language);
    }

    public double getAlignmentCostByIndex(int... glyphIndexes) {
        return getAlignmentCostByIndex(glyphIndexes[0], glyphIndexes[1]);
    }

    public double getAlignmentCostByIndex(int langOneGlyphIndex, int langTwoGlyphIndex) {
        return -1.0 * EtyMath.base2Log(getAlignmentProbabilityByIndex(langOneGlyphIndex, langTwoGlyphIndex));
    }

    public double getAlignmentProbabilityByIndex(int langOneGlyphIndex, int langTwoGlyphIndex) {
        return getAlignmentMatrix().getAlignmentProbabilityByIndex(langOneGlyphIndex, langTwoGlyphIndex);
    }

    public double getAlignmentCostByIndex(Integer l1Idx, Integer l2Idx, Integer l3Idx) {
        return -1.0 * EtyMath.base2Log(getAlignmentProbabilityByIndex(l1Idx, l2Idx, l3Idx));
    }

    public double getAlignmentProbabilityByIndex(Integer l1Idx, Integer l2Idx, Integer l3Idx) {
        return getAlignmentMatrix().getAlignmentProbabilityByIndex(l1Idx, l2Idx, l3Idx);
    }

    private ChangeStatus alignCognates(int wordIndex) {
        
        double previousAlignmentCost = alignmentRegistry.getAlignmentCost(wordIndex);
        // print every tenth iteration, OR every iteration if we don't use simulated annealing
        boolean hasMonitoredWord = hasMonitoredWord(wordIndex);
        ViterbiMatrix vm = new ViterbiMatrix(this);
        
        if (isVerboseIteration() || hasMonitoredWord ) { // verbose iteration
            if (!printCostInfoOnly) {
                StaticLogger.logAlignment(input, alignmentRegistry, wordIndex, vm, false, false);
            }
        }        
        
        // Main Steps:
        // Deregister, Re-align, re-register
        alignmentRegistry.deregisterAlignment(alignmentMatrix, wordIndex);
        
        vm.init(input, wordIndex);

        alignmentRegistry.registerAlignment(alignmentMatrix, wordIndex, vm);
        alignmentRegistry.setAlignmentCost(wordIndex, vm.getCost());
        

        if (isVerboseIteration() || hasMonitoredWord ) { // verbose iteration
            if (!printCostInfoOnly) {
                StaticLogger.logAlignment(input, alignmentRegistry, wordIndex, vm, hasMonitoredWord, true);
            }
        }

        if (vm.getCost() == previousAlignmentCost) {
            return ChangeStatus.NOT_CHANGED;
        }

        return ChangeStatus.CHANGED;
    }

    private ChangeStatus alignFeatureVectors(int wordIndex) throws Exception {
           double previousAlignmentCost = alignmentRegistry.getAlignmentCost(wordIndex);
        
        boolean hasMonitoredWord = hasMonitoredWord(wordIndex);

        
        //deregister
        double costsBeforeDereg = featureAlignmentMatrix.getTotalTreeCosts();
        
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine("\nCost of trees before de-registering word " + wordIndex + ": " + costsBeforeDereg);
        alignmentRegistry.deregisterFeatureAlignmentNotRebuildTrees(featureAlignmentMatrix, wordIndex);
        double costsAfterDereg = featureAlignmentMatrix.getTotalTreeCosts();
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).fine("Cost of trees after de-registering word " + wordIndex + ", before rebuilding: " + costsAfterDereg);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.FINE, "Gain by de-registering: {0}", (costsBeforeDereg - costsAfterDereg));
        
        ViterbiMatrix vm = new ViterbiMatrix(this);
        vm.init(input, wordIndex);

        //register

        alignmentRegistry.registerFeatureAlignment(featureAlignmentMatrix, wordIndex, vm);

        alignmentRegistry.setAlignmentCost(wordIndex, vm.getCost());        


        

        //rebuild the trees!!!
        //costs = 0;
        int rebuildingDivider = configuration.getTreeRebuildingFrequency();
        if (rebuildingDivider != 0 && ((wordIndex % rebuildingDivider) == 0)) {            
            featureAlignmentMatrix.rebuildTrees();            

        }
        
        // print every tenth iteration, OR every iteration if we don't use simulated annealing
        if (isVerboseIteration() || hasMonitoredWord) { // verbose iteration
            if (!disableBasicLogging) {
                StaticLogger.logAlignment(input, alignmentRegistry, wordIndex, vm, hasMonitoredWord, true);
            }
        }        

        if (vm.getCost() == previousAlignmentCost) {
            return ChangeStatus.NOT_CHANGED;
        }

        return ChangeStatus.CHANGED;



    }

    private boolean hasMonitoredWord(int wordIndex) {
        List<String> wordsToMonitor = Configuration.getInstance().getWordsToMonitor();

        if (wordsToMonitor == null || wordsToMonitor.isEmpty()) {
            return false;
        }

        for (GlyphVocabulary vocabulary : input.getVocabularies()) {
            if (wordsToMonitor.contains(vocabulary.getWord(wordIndex))) {
                return true;
            }
        }

        return false;
    }

    private boolean isVerboseIteration() {
        if (!useSimulatedAnnealing) {
            return true;
        }

        return iteration % 10 == 0;
    }

    private void init() throws Exception {
        initAlignmentMatrix();
        CostFunction costFunction = getCostFunction();

        this.alignmentRegistry = new AlignmentRegistry(input.getNumOfWords());
        this.currentTemperature = Constants.INITIAL_TEMPERATURE;
        this.costHandler = new CostHandler(alignmentMatrix, input, costFunction);

        this.cost = Double.MAX_VALUE;

        //sanity check empties the alignemt registry...
        if (configuration.isFirstBaselineThenContext()) {
            this.setExecuteSanityChecks(false);
        }

        if(!executingSanityChecks) {
            this.bestCost = Double.MAX_VALUE;
        }

        this.useSimulatedAnnealing = configuration.isUseSimulatedAnnealing();
        
        this.previousCost = null;
        this.iteration = 0;
        this.noAlignmentChangesInNIterations = 0;
        this.noCostChangesInNIterations = 0;

        instance = this;
    }

    private void initAlignmentMatrix() throws Exception {
        switch (configuration.getAlignmentType()) {
            case TWO_LANG:
                if (configuration.isTakeStartsAndEndsIntoAccount()) {
                    this.alignmentMatrix = new TwoLangAlignmentMatrixWithBoundaryMatrix(input);
                }else{
                    this.alignmentMatrix = new TwoLangAlignmentMatrix(input);
                }
                break;
            case JOINT:
                this.alignmentMatrix = new JointAlignmentMatrix(input);
                break;
            case MARGINAL:
                this.alignmentMatrix = new MarginalAlignmentMatrix(input);
                break;
            case CONTEXT_2D:
                this.featureAlignmentMatrix = new FeatureAlignmentMatrix(input);
                break;
            case CONTEXT_MARGINAL_3D:
                this.featureAlignmentMatrix = new MarginalFeatureAlignmentMatrix(input);
                break;
            default:
                throw new Exception("Unknown alignment type, cannot continue!!");
        }

        if(configuration.getLanguages().size() > 3) {
            System.out.println("Aligning more than 3 languages: " + configuration.getLanguages());
            this.alignmentMatrix = new MultiLangMarginalAlignmentMatrix(input);
        }

    }

    private CostFunction getCostFunction() {
        CostFunction costFunction;
        switch (configuration.getCostFunctionIdentifier()) {
            case BASELINE:
                costFunction = new BaselineCostFunction();
                break;
            case CODEBOOK_NO_KINDS:
                costFunction = new TwoPartCodeCostNoKindsUniformPrior();
                if (Configuration.getInstance().isRemoveSuffixes()) {
                    costFunction = new TwoPartCodeCostNoKindsWithSuffixes();
                }
                break;
            case CODEBOOK_WITH_KINDS_NOT_SEPARATE:
                costFunction = new TwoPartCodeCostFunctionWithKindsNotSeparate();

                if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
                    costFunction = new TwoPartCodeCostUniformPriorWithWordBoundaryKinds();
                }                
                break;
            case CODEBOOK_WITH_KINDS_SEPARATE:
                costFunction = new TwoPartCodeCostFunctionWithKindsSeparate();

                if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
                    costFunction = new TwoPartCodeCostUniformPriorWithWordBoundaryKinds();
                }                
                break;
            case CODEBOOK_WITH_KINDS_SEPARATE_NML:
                costFunction = new TwoPartCodeCostFunctionWithKindsSeparateNML();
                break;
            case WRITTEN_OUT_NXN:
                costFunction = new MultiGlyphCostFunction(alignmentMatrix, input);
                break;
            case CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML:
                costFunction = new TwoPartCodeCostFunctionWithKindsNotSeparateNML();
                break;
            default:
                
                costFunction = new TwoPartCodeCostNoKindsUniformPrior();
        }
        return costFunction;
    }

    public void initAlignmentsOfExistingModel(WordAlignment[] alignments) throws Exception {
        alignmentRegistry.setAllWordAlignments(alignments);
        if (configuration.isUseFeatures()) {
            alignmentRegistry.registerExistingAlignmentsToFeatureMatrix(featureAlignmentMatrix);
        } else {
            alignmentRegistry.registerExistingAlignmentsToAlignmentMatrix(alignmentMatrix);
            
        }
    }

    private void initSpecialCase() throws Exception {

        this.featureAlignmentMatrix = new FeatureAlignmentMatrix(input);
        this.alignmentRegistry.registerExistingAlignmentsToFeatureMatrix(featureAlignmentMatrix);

        this.currentTemperature = Constants.INITIAL_TEMPERATURE;

        this.cost = Double.MAX_VALUE;
        this.bestCost = Double.MAX_VALUE;

        this.previousCost = null;
        this.iteration = 0;
        this.noAlignmentChangesInNIterations = 0;
        this.noCostChangesInNIterations = 0;
    }

    private void printCost() {
        if (configuration.getIterationNumber() != 0) {
            return;
        }
        LOG.log(Level.FINE, costHandler.getCostString());
        if (!printCostInfoOnly) {
            LOG.log(Level.FINE, "{0} :: COST {1} :: AVG {2} :: TOTAL GLYPHS {3}", new Object[]{costHandler.getCostFunction().getName(), Constants.COST_FORMAT.format(cost), Constants.AVG_FORMAT.format(getAveragedCost(cost)), input.getTotalGlyphs()});
        }
        
    }

    private double getAveragedCost(double cost) {
        return cost / input.getTotalWordLength();
    }

    private void executeSanityChecks() throws Exception {
        if (!executeSanityChecks) {
            return;
        }
        
        executingSanityChecks = true;
        disableBasicLogging = true;


        if (configuration.getLanguages().size() != 2) {
            System.out.println("Not a 2-lang-alignment matrix -- no verification");
            return;
        }
        System.out.println("Verification runs..");

        // check costs based on these exact words -- first language first
        GlyphVocabulary voc0 = input.getVocabulary(0);
        GlyphVocabulary voc1 = input.getVocabulary(1);
        input.setVocabulary(0, voc0);
        input.setVocabulary(1, voc0);
        input.reset();

        try {
            init();
        } catch (Exception ex) {
            Logger.getLogger(Alignator.class.getName()).log(Level.SEVERE, null, ex);
        }

        for(int i = 0; i < input.getNumOfWords(); i++) {
            List<Integer> l1WordAsIndexes = input.getVocabulary(0).getWordIndexes(i);
            List<Integer> l2WordAsIndexes = input.getVocabulary(1).getWordIndexes(i);
        

            for(int gi = 0; gi < l1WordAsIndexes.size(); gi++) {
                alignmentMatrix.incrementAlignCount(l1WordAsIndexes.get(gi), l2WordAsIndexes.get(gi));
            }
        }

        if (configuration.getIterationNumber() == 0) {
            StaticLogger.printStatistics(FINAL_LOGGER, input);
            StaticLogger.logFinalOutput();
            printLanguageAlignmentCost();
        }


        input.setVocabulary(0, voc1);
        input.setVocabulary(1, voc1);
        input.reset();


        try {
            init();
        } catch (Exception ex) {
            Logger.getLogger(Alignator.class.getName()).log(Level.SEVERE, null, ex);
        }

                try {
            init();
        } catch (Exception ex) {
            Logger.getLogger(Alignator.class.getName()).log(Level.SEVERE, null, ex);
        }

        for(int i = 0; i < input.getNumOfWords(); i++) {
            List<Integer> l1WordAsIndexes = input.getVocabulary(0).getWordIndexes(i);
            List<Integer> l2WordAsIndexes = input.getVocabulary(1).getWordIndexes(i);

            for(int gi = 0; gi < l1WordAsIndexes.size(); gi++) {
                alignmentMatrix.incrementAlignCount(l1WordAsIndexes.get(gi), l2WordAsIndexes.get(gi));
            }
        }

        if (configuration.getIterationNumber() == 0) {
            StaticLogger.printStatistics(FINAL_LOGGER, input);
            StaticLogger.logFinalOutput();
            printLanguageAlignmentCost();
        }
    }

    private void printLanguageAlignmentCost() {
        getCost();

        if (configuration.getIterationNumber() != 0) {
            return;
        }

        StringBuilder sb = new StringBuilder("DISTANCE COMPARISON DATA -- ");
        sb.append(input.getVocabularies().get(0).getLanguage());
        for (GlyphVocabulary gv : input.getVocabularies().subList(1, input.getVocabularies().size())) {
            sb.append("-");
            sb.append(gv.getLanguage());
        }
        sb.append(" COST ").append(cost);
        sb.append("\n\n");
        
        FINAL_LOGGER.info(sb.toString());
    }

}
