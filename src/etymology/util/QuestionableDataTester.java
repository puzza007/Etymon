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
package etymology.util;

import etymology.align.Alignator;
import etymology.align.AlignmentMatrixType;
import etymology.align.WordAlignment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import etymology.config.Configuration;
import etymology.cost.CostFunctionIdentifier;
import etymology.data.convert.ConversionRules;
import etymology.input.Input;
import etymology.input.LanguageAgainstItselfCostComputer;
import etymology.logging.LoggerInitiator;
import etymology.logging.StaticLogger;
import etymology.output.GnuPlotPrinter;
import etymology.util.EtyMath;
import etymology.util.StringUtils;
import etymology.viterbi.ViterbiMatrix;
import java.io.File;
import java.io.IOException;
import java.util.*;

    
/**
 * Class to test the quality of the questionable words
 * @author lv
 */
public class QuestionableDataTester {
    
    private static Map<String, Double> wordIndexToCostAB = new HashMap<String, Double>();
    private static Map<String, Double> wordIndexToCostAA = new HashMap<String, Double>();
    private static Map<String, Double> wordIndexToCostBB = new HashMap<String, Double>();
    private static Map<String, Double> wordToNCDCost = new HashMap<String, Double>();
    private static final String INPUT_FILE = "/home/group/langtech/Etymology-Project/StarLing/uralet/starling-input-data/starling-10-top2-dialects.utf8";
    
    private static  Input input_final ;
    private static Configuration config;

    private static Input input;
    
    private static final int AB = 1;
    private static final int AA = 2;
    private static final int BB = 3;
    private static final int[] models = new int[]{AB, AA, BB};
    private static final String[] langs= {"FIN", "EST"};
    
    public static void main(String[] args) throws IOException, Exception {
        printWordpairsAndCosts();
    }
    
    public static void printWordpairsAndCosts() throws IOException, Exception {
        
        // train 3 models
        for(int i = 0; i < models.length; i++) {
            trainModel(models[i]);
            
        } 
        
        //calculate NCD costs for each word pair
        input_final = new Input (initConfiguration(Arrays.asList(langs)));
        //System.out.println(wordIndexToCostAA);
        for(int word = 0; word < input_final.getNumOfWords(); word++) {
            //System.out.println(input_final.getVocabulary(0).getWord(word)+ "_" + input_final.getVocabulary(1).getWord(word));
            double aa = wordIndexToCostAA.get(input_final.getVocabulary(0).getWord(word));
            double ab = wordIndexToCostAB.get(input_final.getVocabulary(0).getWord(word) + "_" + input_final.getVocabulary(1).getWord(word));
            double bb = wordIndexToCostBB.get(input_final.getVocabulary(1).getWord(word));
            double NCDCost = computeNCDCost(aa, ab, bb);
            wordToNCDCost.put(input_final.getVocabulary(0).getWord(word) + "_" + input_final.getVocabulary(1).getWord(word), NCDCost);
        }
        
        //sort the map
        ValueComparator bvc =  new ValueComparator(wordToNCDCost);
        TreeMap<String,Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(wordToNCDCost);
         for (String key : sorted_map.keySet()) {
            System.out.println("key/value: " + key + "/"+sorted_map.get(key));
        }
        
    }
    
    private static double computeNCDCost(double aa, double ab, double bb) {
        if(aa <= bb) {
            return (ab - aa) / bb;
        } else {
            return (ab - bb) / aa;
        }
    }
    private static void trainModel(int model) throws IOException, Exception {
        switch (model) {
            case AB:
                initConfiguration(Arrays.asList(langs));
                break;
            case AA:
                initConfiguration(Arrays.asList(new String[]{langs[0], langs[0]}));
                break;
            case BB:
                initConfiguration(Arrays.asList(new String[]{langs[1], langs[1]}));
                break;
            default:
                System.out.println("@@@");
        }
        
        input = new Input(config);
        
        
        StaticLogger.closeIfOpen();

        // initiate loggers
        LoggerInitiator.initLoggers(Configuration.getInstance());

        // this needs to be initiated after the loggers have been created -- forces loggers to be loaded after they are known
        StaticLogger.init();
        GnuPlotPrinter.logPath = Configuration.getInstance().getLogPath();
        
        align(model);
    }
    
    
    private static Configuration initConfiguration(List<String> languages) throws IOException {
        Configuration.clearConfigutation();
        
        config = Configuration.getInstance();
        config.setUseFeatures(false);
        config.setLanguages(languages);
        
        System.out.println("langs: " + config.getLanguages());
        
        config.setInputFile(INPUT_FILE);
        ConversionRules cr = new ConversionRules(new File("/home/group/langtech/Etymology-Project/StarLing/uralet/languageSpecificRules"));
        config.setConversionRules(cr);
        
        //set cost function
        config.setCostFunctionIdentifier(CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE);
        config.setAlignmentType(AlignmentMatrixType.TWO_LANG);
        config.setMaxGlyphsToAlign(1);
                
        //init these just in case
        config.setUseSimulatedAnnealing(true);
        config.setRepetitionCount(1);                
        config.setPrintOnlyFinalLogs(true);
        config.setImpute(true);
        config.setRandomSeed(null);
        
        return config;
    }
    
    public static void align(int model) throws Exception {
        Alignator alignator = new Alignator(config, input);
        alignator.setExecuteSanityChecks(false);
        
        alignator.align();
        
        WordAlignment wordAlignment = null;
        
        for (int wordIndex = 0; wordIndex < input.getNumOfWords(); wordIndex++) {
            //deregister this word
            wordAlignment = alignator.getAlignmentStorage().deregisterAlignment(alignator.getAlignmentMatrix(), wordIndex);
            
            //realign this word
            ViterbiMatrix vm = new ViterbiMatrix(alignator);
            vm.init(input, wordIndex);
            System.out.println(vm.toString());
            double cost = vm.getCost();
            
            switch (model) {
                    case AB:
                        wordIndexToCostAB.put(input.getVocabulary(0).getWord(wordIndex) + "_" + input.getVocabulary(1).getWord(wordIndex) , cost);
                        break;
                    case AA:
                        wordIndexToCostAA.put(input.getVocabulary(0).getWord(wordIndex), cost);
                        break;
                    case BB :
                        wordIndexToCostBB.put(input.getVocabulary(0).getWord(wordIndex), cost);
                        break;
                    default:
                        System.out.println("@@");
            }
            
            //reregister this word back
            alignator.getAlignmentStorage().registerAlignment(alignator.getAlignmentMatrix(), wordAlignment, wordIndex);
        }
        
        System.out.println(alignator.getFinalCost());
    }
    
    
    
    
  static class ValueComparator implements Comparator {

    Map base;
    public ValueComparator(Map base) {
          this.base = base;
     }

     public int compare(Object a, Object b) {

        if((Double)base.get(a) < (Double)base.get(b)) {
              return 1;
        } else if((Double)base.get(a) == (Double)base.get(b)) {
                return 0;
          } else {
        return -1;
     }
      }
        }
    
}


