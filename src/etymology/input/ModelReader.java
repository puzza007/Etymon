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

import etymology.util.CostsToTableReader.ModelType;
import etymology.align.Alignator;
import etymology.align.AlignmentMatrixType;
import etymology.align.WordAlignment;
import etymology.config.Configuration;
import etymology.context.FeatureTree;
import etymology.cost.CostFunctionIdentifier;
import etymology.data.convert.ConversionRules;
import etymology.impute.ContextImputator;
import etymology.impute.Imputator;
import etymology.logging.LoggerInitiator;
import etymology.output.TreeGraphPrinter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author sxhiltun
 */
public class ModelReader {
    private Configuration config;
    private Alignator alignator;
    private WordAlignment[] alignments;
    private Input input;


    public ModelReader() throws FileNotFoundException, Exception {
        initExampleCase();
    }

    private void initExampleCase() throws FileNotFoundException, Exception {

        String lang1 = "EST";
        String lang2 = "FIN";
        ModelType model = ModelType.SEPARATE_ZERO;
        
        //String fileName = "/home/group/langtech/Etymology-Project/etymon-logs/all-logs-04-21/ety50-0406/context-separate/fin-est-test";        
        //String fileName = "/home/sxhiltun/sxhiltun/sanity-checks/codebook-and-kinds/two_lang-codebook_with_kinds-starling-top-dialects.utf8"+
        //        "-khn_dn-saa_n-1x1-simann-0.99-init_temp-50.0-data.log.best";
        String logdir = "/home/group/langtech/Etymology-Project/etymon-logs/all-logs-04-21/ety50-0406/context-separate/";
        String fileName = logdir + "est-fin-test.log.final";
        //String fileName = "./log/two_lang-codebook_with_kinds-starling-10.utf8-fin-saa-1x1-simann-0.995-init_temp-50.0-data.log.best";
        //String fileName = "/home/sxhiltun/NetBeansProjects/EtyMalign/log/two_lang-codebook_with_kinds-starling-10.utf8--fin-1x1-simann-0.995-init_temp-50.0-data.log.best";


        File file = setFileToRead(fileName);
        List<String> rawAlignments = getAlignmentLines(file, "MODELSTART", "MODELEND");
        Map<String, List<String>> data = preprocessAlignmentLines(rawAlignments, lang1, lang2);
        
        
        LanguageAgainstItselfCostComputer laic = new LanguageAgainstItselfCostComputer(model, CostFunctionIdentifier.PREQUENTIAL, lang1, lang2);
        config = laic.initConfiguration(new String[]{lang1, lang2}, model);
        
        
        input = new Input(config, data);
        System.out.println(input.getVocabulary(0).getGlyphs());     
        
        createExistingAlignments(rawAlignments);
        initializeModelBuilding();
        
        
        List<FeatureTree> trees =  alignator.getFeatureAlignmentMatrix().getTrees();
//        for (WordAlignment al : alignator.getAlignmentStorage().getAlignments()) {
//            System.out.println(al.getStringPresentation(input));
//        }
        for (FeatureTree tree : trees) {
            System.out.println(tree);
        }
        //TreeGraphPrinter tgp = new TreeGraphPrinter(trees, (int) Alignator.getCostHandler().getCost());
        
        LoggerInitiator.initImputationLogger(fileName + ".old-imputation");
        Imputator imputator = new ContextImputator(alignator, 0, 1);
        imputator.imputeWords();
        System.out.println("fned: " + imputator.getFeatureWiseLevenshteinDistanceOfImputation());
        System.out.println("ned: " + imputator.getLevenshteinDistanceOfImputation());
        //List<List<String>> wordPairs = oldImputationResults(logdir + "est-fin-test-old-imputation-result");
        //((ContextImputator)imputator).sanityCheckOfImputationCost(wordPairs);

    }

    private List<List<String>> oldImputationResults(String filename) throws FileNotFoundException {
        List<List<String>> imputationWordPairs = new ArrayList<List<String>>();
        
        File f = new File(filename);
        Scanner sc = new Scanner(f);

        
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            
                        
            String[] words = line.split("\\s+");
            List<String> wordPair = new ArrayList<String>();
            wordPair.add(words[0].trim());
            if (words.length>1) {
                wordPair.add(words[1].trim());                                        
            }else {
                wordPair.add("");
            }
            imputationWordPairs.add(wordPair);
        }
        sc.close();
        
        return imputationWordPairs;
                
    }

       


    private void initializeModelBuilding() throws Exception {

       
       alignator = new Alignator(config, input);
       alignator.setExecuteSanityChecks(false);
       alignator.initAlignmentsOfExistingModel(alignments);

       double cost = Alignator.getCostHandler().getGlobalCost();
       System.out.println("Total cost: " + cost);        
    }

                
    private void createExistingAlignments(List<String> alignmentLinesFromFile) {
        
        
        alignments = new WordAlignment[alignmentLinesFromFile.size() / 2];

        int wordPairCount = 0;
        for (int sourceLine = 0; sourceLine < alignmentLinesFromFile.size()-1; sourceLine += 2) {
            int targetLine = sourceLine + 1;

            List<List<Integer>> wordAlignment = new ArrayList<List<Integer>>();

            String sourceWord = alignmentLinesFromFile.get(sourceLine);
            String targetWord = alignmentLinesFromFile.get(targetLine);

            List<Integer> sourceList = new ArrayList<Integer>();
            List<Integer> targetList = new ArrayList<Integer>();

            for (String glyph : sourceWord.split("\\s+")) {
                int glyphIndex = input.getVocabulary(0).getGlyphIndex(glyph);
                sourceList.add(glyphIndex);
            }

            for (String glyph : targetWord.split("\\s+")) {
                int glyphIndex = input.getVocabulary(1).getGlyphIndex(glyph);
                targetList.add(glyphIndex);
            }

            wordAlignment.add(sourceList);
            wordAlignment.add(targetList);
            alignments[wordPairCount++] = new WordAlignment(wordAlignment);
        }

    }
    


    private Map<String, List<String>> preprocessAlignmentLines(List<String> alignmentLinesFromFile, String lang1, String lang2) {

        List<String> sourceList = new ArrayList<String>();
        List<String> targetList = new ArrayList<String>();
        
        for (int sourceLine = 0; sourceLine < alignmentLinesFromFile.size()-1; sourceLine += 2) {
            int targetLine = sourceLine + 1;

            String sourceWord = alignmentLinesFromFile.get(sourceLine).replaceAll("[\\s+\\.]", "");
            String targetWord = alignmentLinesFromFile.get(targetLine).replaceAll("[\\s+\\.]", "");
            
            sourceList.add(sourceWord);
            targetList.add(targetWord);

        }
        Map<String, List<String>> dataMap = new HashMap<String, List<String>>();
        dataMap.put(lang1, sourceList);
        dataMap.put(lang2, targetList);
        
        return dataMap;
        
    }



    private File setFileToRead(String logDir) {

        //add more sophisticated file finding using config ...
        File f = new File(logDir);
        return f;
    }
    
    private List<String> getAlignmentLines(File f, String catchThisFirst, String stopHere) throws FileNotFoundException {

        List<String> alignmentLines = new ArrayList<String>();
        Scanner sc = new Scanner(f);

        boolean found = false;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (line.contains(stopHere)) {
                break;
            }

            if (found) {
                String[] word = line.split("\t");
                if (word != null && !word[0].isEmpty()) {
                    alignmentLines.add(word[0].trim());
                }
            }
            if (!line.contains(catchThisFirst)) {
                continue;
            }
            found = true;
        }
        sc.close();
        return alignmentLines;
    }



    public static void main(String[] args) throws FileNotFoundException, Exception {
        ModelReader m  = new ModelReader();
    }

}
