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
package etymology.input;

import etymology.util.CostsToTableReader;
import etymology.util.CostsToTableReader.ModelType;
import etymology.align.Alignator;
import etymology.align.AlignmentMatrixType;
import etymology.align.WordAlignment;
import etymology.config.Configuration;
import etymology.context.FeatureTree;
import etymology.cost.CostFunctionIdentifier;
import etymology.data.convert.ConversionRules;
import etymology.output.AlignmentPrinter;
import etymology.util.LogRegretMatrixPrinter;
import etymology.util.EtyMath;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author sxhiltun
 */
public class LanguageAgainstItselfCostComputer {
    
    private Configuration config;
    private Alignator alignator;
    private WordAlignment[] alignments;
    private Input input;
    private static final String INPUT_FILE ="/home/group/langtech/Etymology-Project/StarLing/uralet/starling-input-data/starling-10-top2-dialects.utf8";
    //private static final String INPUT_FILE ="/group/home/langtech/Etymology-Project/StarLing/starling-top-dialects.utf8";
    //private static final String CONVERSION_RULES = "/fs-2/a/sxhiltun/ety/languageSpecificRules";  
    private static final String CONVERSION_RULES = "/home/group/langtech/Etymology-Project/StarLing/uralet/languageSpecificRules";
    private static final String logRegretTableName = "/home/group/langtech/Etymology-Project/logRegretMatrix-10kx1k";
    private CostsToTableReader.ModelType model;
    private String languageAgainstItself;
    private String restrictiveLanguage;
    private CostFunctionIdentifier cf;
    
    private List<Integer> notGoodWords;
    
    public LanguageAgainstItselfCostComputer(CostsToTableReader.ModelType model, CostFunctionIdentifier cf, 
            String languageAgainstItself, String restrictiveLanguage) throws Exception {
        this.model = model;
        this.languageAgainstItself = languageAgainstItself.toUpperCase();
        this.restrictiveLanguage = restrictiveLanguage.toUpperCase();
        this.cf = cf;
               
    }
    
    public  double getLanguageAgainstItselfCost() throws Exception {
        //read in 3d data to find only the intersection of words
        String[] languages = new String[]{languageAgainstItself, languageAgainstItself, restrictiveLanguage};
        initConfiguration(languages, CostsToTableReader.ModelType.BASELINE);
        input = new Input(config);
        
        //compute the indexes of words to be removed
        notGoodWords = findNotRelevantWords(languages);
        
        //create the actual model we are after select the cost function here
        initConfiguration(new String[]{languageAgainstItself, languageAgainstItself}, model);        
        input = new Input(config, notGoodWords);
        
        //because all the alignments are words to themselves
        createExistingAlignments();
        //System.out.println("model: " + model);
        
        double cost = initializeModelBuildingAndGetCost();
//        System.out.println("cost: " + cost);
//        List<FeatureTree> trees =  alignator.getFeatureAlignmentMatrix().getTrees();
//
//        for (FeatureTree tree : trees) {
//            System.out.println(tree);
//        }
//        
        return cost;
        
    }
           
            
    private double initializeModelBuildingAndGetCost() throws Exception {

       alignator = new Alignator(config, input);
       
       alignator.setExecuteSanityChecks(false);
       alignator.initAlignmentsOfExistingModel(alignments);
//
//       AlignmentPrinter ap = new AlignmentPrinter(alignator.getAlignmentMatrix(), input);
//        System.out.println(ap.getPrintableAlignmentMatrix(alignator.getAlignmentMatrix()));
        
       double cost = Alignator.getCostHandler().getGlobalCost();
       return cost;
        
    }
    
    private List<Integer> findNotRelevantWords(String[] languages) throws Exception {
        
                        
        //print stats
        //System.out.println("langs: " + config.getLanguages());
        //System.out.println("Number of words: " + input.getNumOfWords());
                
        notGoodWords = new ArrayList<Integer>();
        List<List<Integer>> wordAlignment = new ArrayList<List<Integer>>();
        
        int mainLangCounter = 0;
        for (int wordIndex=0; wordIndex<input.getNumOfWords(); wordIndex++) {
            
            //remove from word index list if third lang is missing
            if (input.getWord(2, wordIndex) == null && input.getWord(0, wordIndex) != null) {
                notGoodWords.add(wordIndex);     
            } 
                                 
        }                    
               
        //System.out.println("Number of words removed " + notGoodWords.size());        
        
        return notGoodWords;
        
    }
    
    private void createExistingAlignments() {
        
        alignments = new WordAlignment[input.getNumOfWords()];
        
        for (int wordIndex=0; wordIndex<alignments.length; wordIndex++) {
            List<List<Integer>> wordAlignment = new ArrayList<List<Integer>>();
            wordAlignment.add(input.getWordIndexes(0, wordIndex));
            wordAlignment.add(input.getWordIndexes(1, wordIndex));
            //System.out.println(input.getWordIndexes(0, wordIndex));
            //System.out.println(input.getWordIndexes(1, wordIndex));
            
            alignments[wordIndex] = new WordAlignment(wordAlignment);
            
        }
        //System.out.println("Number of good alignments: " + alignments.length);
    }
    
    public Configuration initConfiguration(String[] languages, CostsToTableReader.ModelType model) throws IOException, Exception {
        //remove all old stuff
        Configuration.clearConfigutation();
        
        config = Configuration.getInstance();
        config.setLanguages(Arrays.asList(languages));
        config.setInputFile(INPUT_FILE);
        config.setConversionRules(new ConversionRules(new File(CONVERSION_RULES)));
                
        //init these just in case
        config.setUseSimulatedAnnealing(false);
        config.setRepetitionCount(1);                
        config.setPrintOnlyFinalLogs(true);
        config.setImpute(true);
        config.setRandomSeed(null);

        //determine model dependant parameters
        switch(model) {            
            case BASELINE:
                config.setCostFunctionIdentifier(CostFunctionIdentifier.BASELINE);
                config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
                if (languages.length == 3) {
                    config.setAlignmentType(AlignmentMatrixType.MARGINAL);
                }
                break;
            case CODEBOOK_NO_KINDS_1x1:
                config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_NO_KINDS);
                config.setAlignmentType(AlignmentMatrixType.TWO_LANG);                
                break;
            case CODEBOOK_WITH_KINDS_1x1:
                config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE);
                config.setAlignmentType(AlignmentMatrixType.TWO_LANG);                
                break;
            case CODEBOOK_WITH_KINDS_1X1_SEPARATE:
                config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE);
                config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
            case CODEBOOK_WITH_KINDS_1X1_SEPARATE_NML:
                config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE_NML);
                config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
                break;
            case CODEBOOK_WITH_KINDS_1X1_NML:
                config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML);
                config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
                break;    
            case CODEBOOK_WITH_KINDS_1x1_BACKWARDS:
                config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE);
                config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
                config.setWordsFlippedAround(true);
                break;
            case CODEBOOK_2x2_BOUNDARIES:
                config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE);
                config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
                config.setMaxGlyphsToAlign(2);
                config.setTakeStartsAndEndsIntoAccount(true);
                break;
            case CODEBOOK_2x2_BOUNDARIES_BACKWARDS:
                config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE);
                config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
                config.setMaxGlyphsToAlign(2);
                config.setTakeStartsAndEndsIntoAccount(true);
                config.setWordsFlippedAround(true);
                break;                
            case SEPARATE_ZERO_PREQUENTIAL:                
                config.setCodeOneLevelOnly(true);                
                if (cf != null) {
                    config.setCostFunctionIdentifier(cf);
                } else {
                    config.setCostFunctionIdentifier(CostFunctionIdentifier.PREQUENTIAL);
                }
                config.setTreeRebuildingFrequency(0);
                config.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);
                config.setUseFeatures(true);
                config.setBinaryValueTrees(true);
                config.setDoZeroDepthTricks(true);
                break;
            case SEPARATE_ZERO_NML:                
                config.setCodeOneLevelOnly(true);                
                if (cf != null) {
                    config.setCostFunctionIdentifier(cf);
                } else {
                    config.setCostFunctionIdentifier(CostFunctionIdentifier.NML);
                }
                config.setTreeRebuildingFrequency(0);
                config.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);
                config.setUseFeatures(true);
                config.setBinaryValueTrees(true);
                config.setDoZeroDepthTricks(true);
                break;
            case SEPARATE_NORMAL_PREQUENTIAL:
                config.setCodeOneLevelOnly(true);
                if (cf != null) {
                    config.setCostFunctionIdentifier(cf);
                } else {
                    config.setCostFunctionIdentifier(CostFunctionIdentifier.PREQUENTIAL);
                }                
                config.setTreeRebuildingFrequency(0);
                config.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);
                config.setUseFeatures(true);
                config.setBinaryValueTrees(true);
                break;
            case SEPARATE_NORMAL_NML:
                config.setCodeOneLevelOnly(true);
                if (cf != null) {
                    config.setCostFunctionIdentifier(cf);
                } else {
                    config.setCostFunctionIdentifier(CostFunctionIdentifier.NML);
                }                
                config.setTreeRebuildingFrequency(0);
                config.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);
                config.setUseFeatures(true);
                config.setBinaryValueTrees(true);
                break;
            case SEPARATE_INF_PREQUENTIAL:
                config.setCodeOneLevelOnly(true);
                if (cf != null) {
                    config.setCostFunctionIdentifier(cf);
                } else {
                    config.setCostFunctionIdentifier(CostFunctionIdentifier.PREQUENTIAL);
                }                
                config.setTreeRebuildingFrequency(0);
                config.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);
                config.setUseFeatures(true);
                config.setBinaryValueTrees(true);
                config.setInfiniteDepth(true);
                break;
            case SEPARATE_INF_NML:
                config.setCodeOneLevelOnly(true);
                if (cf != null) {
                    config.setCostFunctionIdentifier(cf);
                } else {
                    config.setCostFunctionIdentifier(CostFunctionIdentifier.NML);
                }                
                config.setTreeRebuildingFrequency(0);
                config.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);
                config.setUseFeatures(true);
                config.setBinaryValueTrees(true);
                config.setInfiniteDepth(true);
                break;
            case JOINT_ZERO:
                config.setCodeOneLevelOnly(true);
                if (cf != null) {
                    config.setCostFunctionIdentifier(cf);
                } else {
                    config.setCostFunctionIdentifier(CostFunctionIdentifier.PREQUENTIAL);
                }                
                config.setTreeRebuildingFrequency(0);
                config.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);
                config.setUseFeatures(true);
                config.setBinaryValueTrees(true);
                config.setJointCoding(true);
                config.setDoZeroDepthTricks(true);
                break;
            case JOINT_NORMAL:
                config.setCodeOneLevelOnly(true);
                if (cf != null) {
                    config.setCostFunctionIdentifier(cf);
                } else {
                    config.setCostFunctionIdentifier(CostFunctionIdentifier.PREQUENTIAL);
                }                
                config.setTreeRebuildingFrequency(0);
                config.setAlignmentType(AlignmentMatrixType.CONTEXT_2D);
                config.setUseFeatures(true);
                config.setBinaryValueTrees(true);
                config.setJointCoding(true);
                break;
            default:
                throw new RuntimeException("Unknown model: " + model);
        }
             

        
        //read in the regret table if NML model
        if (config.getCostFunctionIdentifier().equals(CostFunctionIdentifier.NML)) {            
            EtyMath.setLogRegretTable(LogRegretMatrixPrinter.getLogRegretMatrix(logRegretTableName));
        }
        return config;
                            
    }
    
    public  void oneLevelTreeSanityChecks() throws IOException, Exception {
        String[] langsToCheck = new String[]{"UGR", "MAR_KB", "FIN"};       
        int repeatNtimes = 10;
        int dataSetSize;

        Map<String, Map<Integer, Double>> sampleSizeToCostMap = 
                new TreeMap<String, Map<Integer, Double>>();
        
        
        for (String language : langsToCheck) {
            sampleSizeToCostMap.put(language, new TreeMap<Integer, Double>());
            initConfiguration(new String[]{language, language}, CostsToTableReader.ModelType.BASELINE);
            input = new Input(config);
            dataSetSize = input.getNumOfWords();
            
            //repeat for each subset
            for (int maxWords=50; maxWords<dataSetSize+10; maxWords+=10) {
                if (maxWords > dataSetSize) {
                    maxWords = dataSetSize;
                }
                
                List<Double> modelCost = new ArrayList<Double>();
                //repeat N times to get the average
                for (int repetition=0; repetition<repeatNtimes; repetition++) {
                    
                    initConfiguration(new String[]{language, language}, model);        
                    config.setMaxWordsToUse(maxWords);
                    input = new Input(config);       
                    createExistingAlignments();              
                    double cost = initializeModelBuildingAndGetCost();
                    modelCost.add(cost);                    
                }
//                System.out.println(modelCost.toString());
//                System.out.println(EtyMath.getMean(modelCost));
                sampleSizeToCostMap.get(language).put(maxWords, EtyMath.getMean(modelCost));
                //System.out.println(sampleSizeToCostMap.get(language).toString());
            }
            
            StringBuilder sb = new StringBuilder();
            System.out.println("# Language: " + language);
            for (int size : sampleSizeToCostMap.get(language).keySet()) {
                sb.append(language).append("\t");
                sb.append(size).append("\t");
                sb.append(sampleSizeToCostMap.get(language).get(size));  
                sb.append("\n");
            }
            System.out.println(sb.toString());
            System.out.println("");
            System.out.println("");
            
        }
        
                        
        
    }
    
    
    
    public static void main(String[] args) throws Exception  {
        LanguageAgainstItselfCostComputer l = 
                new LanguageAgainstItselfCostComputer(
                        CostsToTableReader.ModelType.SEPARATE_ZERO, CostFunctionIdentifier.NML,
                         "FIN","FIN");
        //l.getLanguageAgainstItselfCost();
        l.oneLevelTreeSanityChecks();
    }
    
    

    
    
}
