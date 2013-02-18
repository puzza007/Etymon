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
package etymology;





import etymology.util.LogRegretMatrixPrinter;
import etymology.align.Alignator;
import etymology.align.AlignmentMatrixType;
import etymology.align.WordAlignment;
import etymology.config.CLOptions;
import etymology.config.CommandLineReader;
import etymology.config.Constants;
import etymology.config.Configuration;
import java.io.IOException;
import java.util.Map;
import etymology.data.convert.ConversionRules;

import etymology.cost.CostFunctionIdentifier;
import etymology.context.FeatureTree;
import etymology.cost.SuffixCostType;
import etymology.impute.BaselineImputator;
import etymology.impute.ContextImputator;
import etymology.impute.NxNViterbiImputator;
import etymology.impute.Imputator;
import etymology.impute.ThreeDimBaselineImputator;
import etymology.input.Input;

import etymology.logging.LoggerInitiator;
import etymology.logging.StaticLogger;
import etymology.output.GnuPlotPrinter;
import etymology.output.TreeGraphPrinter;
import etymology.util.CostsToTableReader;

import java.io.File;

import etymology.util.EtyMath;
import etymology.viterbi.ViterbiMatrix;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 *   
 * @author arto
 */
public class Main extends Constants implements CLOptions {

    static String[] okLangs = new String[]{"est", "fin", "khn", "kom", "man", "mar", "mrd", "proto", "saa", "udm", "ugr", "sm", "vi"};
    static String[] ssaLangs = new String[]{"ink", "ka", "li", "lp", "ly", "md", "ostj", "sm", "syrj", "tser", "unk", "va", "ve", "vi", "vog", "votj"};
    //sm ink ka li ly va ve vi 

    private static boolean SKIP_RUN_IF_LOG_EXISTS = true;
    private static List<List<String>> suspiciousWordPairs = new ArrayList<List<String>>();;


    /**
     * Starts from here!!
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        
        init(args);
    }
    
    /**
     * Set configurations
     * @param args
     * @throws Exception 
     */
    private static void init(String[] args) throws Exception {
        //convert conmandline into config
        Configuration config = CommandLineReader.readConfiguration(args);
        System.out.println("Alignemnt type : " + config.getAlignmentType());
        //first choose to useFeatures or not, then set config dependingly 
        if (args.length == 0) {
            
            
            SKIP_RUN_IF_LOG_EXISTS = false;

            //change only this
            config.setUseFeatures(false);
            //config.setDoFirstBaselineThenContext(false);
            
            if (config.isUseFeatures()) {
                setContextConfiguration(config);
            } else {
                setNonContextConfiguration(config);
            }
        }

        //Override possible extra params if pipeline approach
        //todo: hybrid model has problems with log naming, fails, fix
        if (config.isFirstBaselineThenContext()) {
            setFirstBaselineThenContextConfig(config);
        }
        
        Constants.INITIAL_TEMPERATURE = config.getInitialAnnealingTemp();
        Constants.TEMPERATURE_MULTIPLIER = config.getAnnealingMultiplier();
        Constants.REBUILD_TREES_EVERY_NTH_ITERATION = config.getTreeRebuildingFrequency();
        
        // Init conversion rules from file (for context model??)
        
        //ConversionRules cr = new ConversionRules(new File("/home/group/langtech/Etymology-Project/StarLing/uralet/languageSpecificRules"));
        //config.setConversionRules(cr);

        //read in the regret table if NML model
        if (config.getCostFunctionIdentifier().equals(CostFunctionIdentifier.NML) || config.getCostFunctionIdentifier().equals(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE_NML) || config.getCostFunctionIdentifier().equals(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML)) {
            String logRegretTableName = config.getLogRegretMatrixFileName();// "/home/group/langtech/Etymology-Project/logRegretMatrix-10kx1k";
            EtyMath.setLogRegretTable(LogRegretMatrixPrinter.getLogRegretMatrix(logRegretTableName));
        }
        
        
        //cost function is optional, if context model, the default is  prequential
        if (config.isUseFeatures() && !config.getCostFunctionIdentifier().equals(CostFunctionIdentifier.NML)) {
            config.setCostFunctionIdentifier(CostFunctionIdentifier.PREQUENTIAL);
        }
        //baseline cost function can not be used in multiglyph alignment
        if (config.getCostFunctionIdentifier().equals(CostFunctionIdentifier.BASELINE)) {
            if (config.getMaxGlyphsToAlign() > 1) {
               System.out.println("baseline cost function can not be used in multiglyph alignment");
                return;
            }
        }

        //-boundaries option: starts and ends can not be taken into account if aligning only single glyphs
        if (config.isTakeStartsAndEndsIntoAccount()) {
            if (config.getMaxGlyphsToAlign() < 2) {
                System.out.println("starts and ends can not be taken into account if aligning only single glyphs");
                return;
            }
        }

        //starts and ends can not be taken in account with codebook no kinds model
        if (config.isTakeStartsAndEndsIntoAccount()) {
            if (config.getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_NO_KINDS) {
                 System.out.println("starts and ends can not be taken in account with codebook no kinds model");
                return;
            }
        }


        init();

    }
    
    
    /**
     * Init loggers and alignator
     * @throws Exception 
     */
     private static void init() throws Exception {
         
        //do not run if SKIP is true 
        if (SKIP_RUN_IF_LOG_EXISTS) {
            String filename = Configuration.getInstance().getLogFilePath() + ".costs";            
            File f = new File(filename);
            System.out.println("f: " + f);
            if (f.exists() && f.length() > 0) {                
                System.out.println("Log already exists, not running..");
                return;
            }
        }
        
        
        //if there are some loggers hanging around, get the job done and close them.
        StaticLogger.closeIfOpen();
      
        // initiate loggers
        LoggerInitiator.initLoggers(Configuration.getInstance());
   
        // this needs to be initiated after the loggers have been created -- forces loggers to be loaded after they are known
        StaticLogger.init();
    
        GnuPlotPrinter.logPath = Configuration.getInstance().getLogPath();
   
        //init alignator
        
        initAlignator(Configuration.getInstance());
    }

     
     
    private static void setContextConfiguration(Configuration config) {
        config.setUseSimulatedAnnealing(true);
        
        if (config.isUseSimulatedAnnealing()) {
            config.setInitialAnnealingTemp(50);
            config.setAnnealingMultiplier(0.9);
        }

        //config.setCodeCompleteWordFirst(true);
        config.setDoZeroDepthTricks(true);
        config.setInfiniteDepth(false);
        config.setTreeRebuildingFrequency(0);
        config.setWordsFlippedAround(false);
        config.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);
        config.setCostFunctionIdentifier(CostFunctionIdentifier.PREQUENTIAL);
        //config.setCostFunctionIdentifier(CostFunctionIdentifier.PREQUENTIAL);
        config.setBinaryValueTrees(true);
        config.setJointCoding(false);
        // sm ink ka li ly va ve vi 

        config.setLanguages(Arrays.asList(new String[]{"KOM_P", "MAN_SO"}));
        //config.setInputFile("/group/home/langtech/Etymology-Project/ssa-all.utf8");
        //config.setInputFile("/home/lv/Etymology/starling-top-dialects.utf8");
        config.setInputFile("/home/group/langtech/Etymology-Project/StarLing/uralet/starling-input-data/starling-10-top2-dialects.utf8");
        //config.setInputFile("/group/home/langtech/Etymology-Project/StarLing/starling-top2-dialects.utf8");
        
        //config.setInputFile("/group/home/langtech/Etymology-Project/StarLing/starling-10.utf8");
        config.setIterationNumber(0);
        config.setPrintOnlyFinalLogs(false);
        config.setImpute(true);
        config.setRepetitionCount(1);
        
        //config.setRandomSeed(new Long(1864917862));
        //config.setIterationNumber(44);
        //suspiciousWordPairs = ExtraSettingsForProgramToRun.setSuspiciousWordPairs();               
    }

    private static void setFirstBaselineThenContextConfig(Configuration config) {
        config.setUseFeatures(false);
        config.setUseSimulatedAnnealing(true);
        config.setMaxGlyphsToAlign(1);
        config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
        config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_NO_KINDS);
    }

    

   



    private static void setNonContextConfiguration(Configuration config) {
        config.setUseSimulatedAnnealing(true);
//        if (config.isUseSimulatedAnnealing()) {
//            config.setInitialAnnealingTemp(50);
//            config.setAnnealingMultiplier(0.9);
//        }
        //config.setIsSeparateKinds(true);
        config.setInitialAnnealingTemp(50);
        config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
        config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE_NML);
        //config.setRemoveSuffixes(true);
        //config.setSuffixCostType(SuffixCostType.PREQUENTIAL);
        config.setPrintOnlyFinalLogs(false);
        config.setMaxGlyphsToAlign(1);
        //config.setIterationNumber(1);
        config.setTakeStartsAndEndsIntoAccount(false);
        //static String[] ssaLangs = new String[]{"ink", "ka", "li", "lp", "ly", "md", "ostj", "sm", "syrj", "tser", "unk", "va", "ve", "vi", "vog", "votj"};
        //ok: sm
        
        /*
         * Languages to choose(top dialect): FIN EST KHN_DN KOM_S MAN_P MAR_KB MRD_E SAA_N UDM_S UGR
         */
        
        config.setLanguages(Arrays.asList(new String[]{"FIN", "EST"}));
        //config.setLanguages(Arrays.asList(new String[]{"KHN_DN", "KHN_DN"}));
        System.out.println("langs: " + config.getLanguages());
        //config.setInputFile("");
        //config.setInputFile("/group/home/langtech/Etymology-Project/ssa-all.utf8");
        //config.setInputFile("/group/home/langtech/Etymology-Project/StarLing/starling-top-dialects.utf8");
        //config.setInputFile("/group/home/langtech/Etymology-Project/StarLing/starling-top2-dialects.utf8");
        config.setInputFile("/home/group/langtech/Etymology-Project/StarLing/uralet/starling-input-data/starling-10-top2-dialects.utf8");
        //config.setInputFile("/group/home/langtech/Etymology-Project/StarLing/starling-10.utf8");
        //config.setRepetitionCount(1);
        config.setWordsFlippedAround(false);
        //config.setPrintOnlyFinalLogs(false);
        //config.setRandomSeed(new Long(1297388814));
        //config.setMaxWordsToUse(50);
        config.setImpute(true);
        System.out.println("Set configuration, max glyphs: " + config.getMaxGlyphsToAlign());
        

        //config.setWordsToMonitor(Arrays.asList(new String[]{"jum", "duobmâ"}));
    }


    private static void initAlignator(Configuration config) throws Exception {
        System.out.println(Configuration.getInstance().getCostFunctionIdentifier());
        
        int iteration = 0;
        Map<Long, Double> seedToCostMap = new HashMap();
        StringBuilder costImpCost = new StringBuilder();
        List<Double> imputationDistance = new ArrayList<Double>();
        List<Double> imputationFeatureDistance = new ArrayList<Double>();
        List<Double> imputationAccuracy = new ArrayList<Double>();
        double LOOScore = 0;
        

        //sim-ann turned off if converged
        boolean repeatUsingSimAnnealing = config.isUseSimulatedAnnealing();
        Alignator alignator;
        
        //loop "repetitionCount" number of times 
        do {
            if (config.isFirstBaselineThenContext()) {
                setFirstBaselineThenContextConfig(config);
            }

            if (repeatUsingSimAnnealing) {
                config.setUseSimulatedAnnealing(true);
            }

            //long randomSeed = new Random().nextInt();
            //randomSeed = -1634159881;
            //config.setRandomSeed(randomSeed);
            if (iteration > 0) {
                config.setRandomSeed(null);
            }

            long randomSeed = config.getRandomSeed();
            System.out.println("context imputation test: randomeseed " + randomSeed);
            alignator = new Alignator(config);
            alignator.setExecuteSanityChecks(false);

            alignator.align();

            seedToCostMap.put(randomSeed, alignator.getFinalCost());

            costImpCost.append(Configuration.getInstance().getIterationNumber()).append("\t");
            costImpCost.append(randomSeed).append("\t");
            costImpCost.append(alignator.getFinalCost()).append("\n");

            
            System.out.println("Now impute...");
            
            
            if (config.isUseImputation()) {

                Imputator imputator;

                if (config.isUseFeatures()) {
                    imputator = new ContextImputator(alignator, 0, 1);
                } else if (config.getCostFunctionIdentifier() == CostFunctionIdentifier.BASELINE) {
                    imputator = new BaselineImputator(alignator, alignator.getInput(), 0, 1);
                } else if (config.getLanguages().size() == 3) {
                    imputator = new ThreeDimBaselineImputator(alignator, alignator.getInput(), 0, 1, 2);
                }
                else {
                    
                    imputator = new NxNViterbiImputator(alignator, alignator.getInput(), 0, 1);
                }

                //impute
                imputator.imputeWords();
                
                /**
                 * get LOO-score ---Lv
                 **/
                 
                for(int wordIndex = 0; wordIndex < alignator.getInput().getNumOfWords(); wordIndex++) {
                   LOOScore += alignator.getAlignmentStorage().getAlignmentCost(wordIndex);
                }
                System.out.println("LOO-Score: " + LOOScore);
                
                /**
                 * Use the deregister-align-reregister method to test the short cut above (non context model)
                 */
                /*
                WordAlignment wordAlignment = null;
                double totalCost = 0;
                for (int wordIndex = 0; wordIndex < alignator.getInput().getNumOfWords(); wordIndex++) {
                    //deregister this word
                    wordAlignment = alignator.getAlignmentStorage().deregisterAlignment(alignator.getAlignmentMatrix(), wordIndex);

                    //realign this word
                    ViterbiMatrix vm = new ViterbiMatrix(alignator);
                    vm.init(alignator.getInput(), wordIndex);
                    
                    totalCost += vm.getCost();
                    //reregister this word back
                    alignator.getAlignmentStorage().registerAlignment(alignator.getAlignmentMatrix(), wordAlignment, wordIndex);
                }
                
                System.out.println("LOO-Score (to verify): " + totalCost);
                 
                */
                imputationDistance.add(imputator.getLevenshteinDistanceOfImputation());
                imputationAccuracy.add(imputator.getAccuracyOnWordLevel());
                imputationFeatureDistance.add(imputator.getFeatureWiseLevenshteinDistanceOfImputation());

                costImpCost.append(imputator.getLevenshteinDistanceOfImputation()).append("\t");
                costImpCost.append(imputator.getFeatureWiseLevenshteinDistanceOfImputation()).append("\n");                
            }

            //sanity checks
            if (!suspiciousWordPairs.isEmpty()) {
                ContextImputator imputator = new ContextImputator(alignator, 0, 1);
                for (List<String> wordpair : suspiciousWordPairs) {
                    imputator.computeCosts(wordpair.get(0), wordpair.get(1));
                }
            }

            if (config.isUseFeatures() ) {
                ExtraSettingsForProgramToRun.printTreeGraphs(alignator, iteration);
            }

            iteration++;

        } while (iteration < Configuration.getInstance().getRepetitionCount());

        if (Configuration.getInstance().getIterationNumber() == 0) {
            printCostData(seedToCostMap);
            printImputationData(imputationDistance, "Levenshtein distance of imputation");
            printImputationData(imputationFeatureDistance, "Featurewise Levenshtein distance of imputation");
            printImputationData(imputationAccuracy, "Accuracy of imputation on word level");
            StaticLogger.logToCostsLog("LOO-Score: " + LOOScore);
        }
        printIterationSpecificCostInformation(costImpCost);

        if (Configuration.getInstance().getIterationNumber() == 0) {
            if (!config.isUseFeatures()) { //something does not work here with context model        
                
                if (repeatUsingSimAnnealing) {
                    config.setUseSimulatedAnnealing(true);
                }

                long randomSeed = getSmallestCostKey(seedToCostMap);
                config.setRandomSeed(randomSeed);

                alignator = new Alignator(config);
                alignator.setExecuteSanityChecks(false);
                alignator.align();

                StaticLogger.logLatestOutputToBestAlignmentLog();
            }
        }

    }

    
    private static void verifyLanguages(List<String> languages) {
        VerifyLanguages:
        for (String lang : languages) {
            lang = lang.toUpperCase();

            for (String okLang : okLangs) {
                okLang = okLang.toUpperCase();

                if (lang.contains(okLang)) {
                    continue VerifyLanguages;
                }
            }

            System.out.println("Sorry, not ok language.");
            System.exit(1);
        }
    }

    private static void printCostData(Map<Long, Double> costMap) {
        Collection<Double> costs = costMap.values();

        StringBuilder sb = new StringBuilder();
        sb.append("Total repetitions done: ").append(costs.size()).append("\n\n");
        sb.append("Mean: ").append(EtyMath.getMean(costs)).append("\n");
        sb.append("Min: ").append(EtyMath.getMin(costs)).append("\n");
        sb.append("Max: ").append(EtyMath.getMax(costs)).append("\n");
        sb.append("Variance: ").append(EtyMath.getVariance(costs)).append("\n");
        sb.append("Standard Deviation: ").append(EtyMath.getStandardDeviation(costs)).append("\n\n");
        sb.append("Min cost with random seed: ").append(getSmallestCostKey(costMap)).append("\n");

        System.out.println(sb);
        StaticLogger.logToCostsLog(sb.toString());
    }

    private static void printImputationData(List<Double> imputationDistances, String message) {
        if (imputationDistances.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(message);
        sb.append("\n");
        sb.append("Mean: ").append(EtyMath.getMean(imputationDistances)).append("\n");
        sb.append("Min: ").append(EtyMath.getMin(imputationDistances)).append("\n");
        sb.append("Max: ").append(EtyMath.getMax(imputationDistances)).append("\n");
        sb.append("Standard Deviation: ").append(EtyMath.getStandardDeviation(imputationDistances)).append("\n\n");

        System.out.println(sb);
        StaticLogger.logToCostsLog(sb.toString());
        
    }

    private static void printIterationSpecificCostInformation(StringBuilder sb) {
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Iter:\tSeed:\t\tCost:\t\t\tEdit Distance:\t\tFeaturewise Edit Distance\n");
        sb2.append(sb);

        StaticLogger.logToCostsLog(sb2.toString());
    }

    private static long getSmallestCostKey(Map<Long, Double> keyToCostMap) {
        double min = EtyMath.getMin(keyToCostMap.values());

        for (Long randomSeed : keyToCostMap.keySet()) {
            if (!keyToCostMap.get(randomSeed).equals(min)) {
                continue;
            }

            return randomSeed;
        }

        throw new IllegalArgumentException("No smallest key available.");
    }

    


}
