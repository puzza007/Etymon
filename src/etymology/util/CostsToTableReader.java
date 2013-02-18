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

import etymology.config.CommandLineReader;
import etymology.config.Configuration;
import etymology.cost.CostFunctionIdentifier;
import etymology.input.Input;
import etymology.input.LanguageAgainstItselfCostComputer;
import etymology.util.EtyMath;
import etymology.util.StringUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;


/**
 *
 * @author sxhiltun
 */
public class CostsToTableReader {

    private static void ComputeNCDforExistingMinCosts(ModelType modelType, CostFunctionIdentifier costFunctionIdentifier, String minCostDir, String outputDir) throws FileNotFoundException, Exception {
        //MinCostDir:
        
        //String minCostDir = "/home/nouri/Desktop/utils/new/";
        //String[] langs = new String[]{"fin", "est", "khn_dn", "kom_s", "man_p", "mar_kb", "mrd_e", "saa_n", "udm_s", "ugr"};
        String[] langs = new String[]{"fin", "est", "khn_dn", "khn_v", "kom_s", "kom_p", "man_p", "man_so", "mar_kb", "mar_b", "mrd_e", "mrd_m", "saa_n", "saa_l", "udm_s", "udm_g","ugr"};
        String fileName = "";
        switch(modelType){
            case JOINT_NORMAL:
                fileName = "joint-normal-prequential";
                break;
            case JOINT_ZERO:
                fileName = "joint-zero-prequential";
                break;                
            case SEPARATE_INF_NML:
                fileName = "separate-inf-nml";
                break;
            case SEPARATE_INF_PREQUENTIAL:
                fileName = "separate-inf-prequential";
                break;
            case SEPARATE_ZERO_NML:
                fileName = "separate-zero-nml";
                break;
            case SEPARATE_ZERO_PREQUENTIAL:
                fileName = "separate-zero-prequential";
                break;
            case SEPARATE_NORMAL_NML:
                fileName = "separate-normal-nml";
                break;
            case SEPARATE_NORMAL_PREQUENTIAL:
                fileName = "separate-normal-prequential";
                break;
        }
        System.out.println(fileName);
        String filePath = minCostDir + fileName;
        Scanner minCostScanner = new Scanner(new File(filePath));
        PrintStream out = new PrintStream(new File(outputDir + fileName));
        for(String lang1 : langs){            
            for(String lang2 : langs){
                System.out.println("\t" + lang1 + "\t" + lang2);
                if(lang1.equals(lang2)){
                    out.print("0\t");
                    continue;
                }                
                double costBetween = Double.parseDouble(minCostScanner.next());
                //Now get C(l1|l2):
                LanguageAgainstItselfCostComputer lai1 = new LanguageAgainstItselfCostComputer(modelType, costFunctionIdentifier, lang1, lang2);
                double l1l2 = lai1.getLanguageAgainstItselfCost();
                lai1 = new LanguageAgainstItselfCostComputer(modelType, costFunctionIdentifier, lang2, lang1);
                double l2l1 = lai1.getLanguageAgainstItselfCost();
                double normalizedCost = EtyMath.getNormalizedCompressionDistance(costBetween, l1l2, l2l1);
                out.print(normalizedCost+"\t");                
            }            
            out.println("");
        }
        minCostScanner.close();
        out.close();
    }

    public enum ModelType {
        BASELINE("baseline"),
        CODEBOOK_NO_KINDS_1x1("codebook-no-kinds"),
        
        CODEBOOK_WITH_KINDS_1x1("codebook-and-kinds"),
        CODEBOOK_WITH_KINDS_1x1_BACKWARDS("codebook-and-kinds"),
        CODEBOOK_WITH_KINDS_1X1_SEPARATE("codebook-and-kinds-separate"),
        CODEBOOK_WITH_KINDS_1X1_NML("codebook-and-kinds-nml"),
        CODEBOOK_WITH_KINDS_1X1_SEPARATE_NML("codebook-and-kinds-separate-nml"),
        
        CODEBOOK_WITH_KINDS_2x2("codebook-and-kinds"),
        CODEBOOK_2x2_BOUNDARIES("codebook-and-kinds-boundaries"),
        CODEBOOK_2x2_BOUNDARIES_BACKWARDS("codebook-and-kinds-boundaries"),

        SEPARATE_ZERO("context-separate"),
        SEPARATE_ZERO_NML("context-separate-zero-nml-best"),
        SEPARATE_ZERO_PREQUENTIAL("context-separate-zero-prequential"),
        SEPARATE_NORMAL("context-separate-normal"),
        SEPARATE_NORMAL_PREQUENTIAL("context-separate-normal-prequential"),
        SEPARATE_NORMAL_NML("context-separate-normal-nml"),
        SEPARATE_INF_PREQUENTIAL("context-separate-inf-prequential-best"),
        SEPARATE_INF_NML("context-separate-inf-nml"),

        JOINT_NORMAL("context-joint-normal-prequential"),
        JOINT_ZERO("context-joint-zero-prequential");

        
        private String name; //directory name
        private String params;
        private Map<String, Map<CostType, Number>> costInfo;

        private ModelType(String name) {
            this.name = name;
            costInfo = new TreeMap<String, Map<CostType, Number>>();
        }

        public String getDirName() {
            return name;
        }

        public void addCostToMap(String[] languages, CostType costType, Number cost) {
            String languagePair = getLanguageList(languages);
            if (costInfo.containsKey(languagePair)) {
                costInfo.get(languagePair).put(costType, cost);
            } else {
                EnumMap<CostType, Number> costInfoMap = new EnumMap<CostType, Number>(CostType.class);
                costInfoMap.put(costType, cost);
                costInfo.put(languagePair, costInfoMap);
            }

        }

        public Number getCost(String languages[], CostType costType) {
            String languagePair = getLanguageList(languages);
                        
            try {
                return costInfo.get(languagePair).get(costType);
            } catch (Exception e) {
                System.out.println("costInfo: " + costInfo == null);
                return null;
            }
            
        }
        
        public static String getLanguageList(String[] languages) {
            String languageGroup = "";
            for (String lang : languages) {
                languageGroup += "-";
                languageGroup += lang;
            }
            
            //remove the first "-"
            languageGroup = languageGroup.substring(1);
            
            return languageGroup;
        }

        public void setModelParameters(String params) {
            this.params = params;
        }

        public String getModelParameters() {
            return params;
        }



        public List<String> getMustWords(String[] langs) {
            
            //public List<String> getMustWords(String comparisonLang, String anotherLang) {
            List<String> mustWords = new ArrayList<String>();
            //String must = comparisonLang + "-" + anotherLang;
            String must = ModelType.getLanguageList(langs);
            mustWords.add(must);
            mustWords.add(".costs");
            
            switch(this) {
                case BASELINE:
                    mustWords.add("baseline");
                    break;
                case CODEBOOK_NO_KINDS_1x1:
                    if (langs.length == 3) {
                       mustWords.add("marginal");
                    }
                    mustWords.add("1x1");
                    mustWords.add("CB-Nokinds");                    
                    break;
                case CODEBOOK_WITH_KINDS_1x1:
                    mustWords.add("1x1");
                    mustWords.add("CB+kinds");
                    //mustWords.add("init_temp-100.0");
                    break;
                case CODEBOOK_WITH_KINDS_1X1_SEPARATE:
                    mustWords.add("1x1");
                    mustWords.add("CB+kinds-Cond+kinds");
                    break;
                case CODEBOOK_WITH_KINDS_1X1_NML:
                    mustWords.add("1x1");
                    mustWords.add("CB+kinds");
                    mustWords.add("NML");
                    break;
                case CODEBOOK_WITH_KINDS_1X1_SEPARATE_NML:
                    mustWords.add("1x1");
                    mustWords.add("CB+kinds-Cond+kinds");
                    mustWords.add("NML");
                    break;
                case CODEBOOK_WITH_KINDS_1x1_BACKWARDS:
                    mustWords.add("1x1");
                    mustWords.add("CB+kinds");
                    mustWords.add("backwards");
                    break;
                case CODEBOOK_WITH_KINDS_2x2:
                    mustWords.add("nxn");
                    mustWords.add("CB+kinds");
                    break;
                case CODEBOOK_2x2_BOUNDARIES:
                    mustWords.add("nxn");
                    mustWords.add("CB+kinds");
                    mustWords.add("boundaries");
                    //mustWords.add("init_temp-100.0");
                    break;
                case CODEBOOK_2x2_BOUNDARIES_BACKWARDS:
                    mustWords.add("nxn");
                    mustWords.add("CB+kinds");
                    mustWords.add("boundaries");
                    mustWords.add("backwards");
                    break;
                case SEPARATE_ZERO_PREQUENTIAL:
                    mustWords.add("context");
                    mustWords.add("zero");
                    mustWords.add("multi-binary");
                    mustWords.add("prequential");
                    break;
                case SEPARATE_ZERO_NML:
                    mustWords.add("context");
                    mustWords.add("zero");
                    mustWords.add("multi-binary");
                    mustWords.add("nml");
                    break;
                case SEPARATE_NORMAL:
                    mustWords.add("context");
                    mustWords.add("multi-binary");
                    break;
                case SEPARATE_NORMAL_NML:
                    mustWords.add("context");
                    mustWords.add("multi-binary");
                    mustWords.add("nml");
                    break;
                case SEPARATE_NORMAL_PREQUENTIAL:
                    mustWords.add("context");
                    mustWords.add("multi-binary");
                    mustWords.add("prequential");
                    break;
                case SEPARATE_INF_NML:
                    mustWords.add("context");
                    mustWords.add("inf");
                    mustWords.add("multi-binary");
                    mustWords.add("nml");
                    break;
                 case SEPARATE_INF_PREQUENTIAL:
                    mustWords.add("context");
                    mustWords.add("inf");
                    mustWords.add("multi-binary");
                    mustWords.add("prequential");
                    break;
                case JOINT_NORMAL:
                    mustWords.add("joint");
                    mustWords.add("multi-binary");
                    break;
                case JOINT_ZERO:
                    mustWords.add("joint");
                    mustWords.add("zero");
                    mustWords.add("multi-binary");
                    break;

            }
            

            return mustWords;
        }

        public List<String> getNotWords() {

            List<String> notWords = new ArrayList<String>();
            switch(this) {
                case BASELINE:
                    notWords.add("nxn");
                    
                    break;
                case CODEBOOK_NO_KINDS_1x1:
                    notWords.add("CB+kinds");
                    notWords.add("baseline");
                    notWords.add("nxn");
                    break;
                case CODEBOOK_WITH_KINDS_1x1:
                    notWords.add("codebook_no_kinds");
                    notWords.add("baseline");
                    notWords.add("nxn");
                    notWords.add("backwards");
                    break;
                case CODEBOOK_WITH_KINDS_1X1_NML:
                    notWords.add("codebook_no_kinds");
                    notWords.add("baseline");
                    notWords.add("nxn");
                    notWords.add("backwards");
                    break;
                case CODEBOOK_WITH_KINDS_1X1_SEPARATE_NML:
                    notWords.add("codebook_no_kinds");
                    notWords.add("baseline");
                    notWords.add("nxn");
                    notWords.add("backwards");
                    break;
                case CODEBOOK_WITH_KINDS_1X1_SEPARATE:   
                    notWords.add("codebook_no_kinds");
                    notWords.add("baseline");
                    notWords.add("nxn");
                    notWords.add("backwards");
                    break;
                case CODEBOOK_WITH_KINDS_1x1_BACKWARDS:
                    notWords.add("codebook_no_kinds");
                    notWords.add("baseline");
                    notWords.add("nxn");
                    break;
                case CODEBOOK_WITH_KINDS_2x2:
                    notWords.add("codebook_no_kinds");
                    notWords.add("baseline");
                    notWords.add("boundaries");
                    notWords.add("1x1");
                    break;                
                case CODEBOOK_2x2_BOUNDARIES:
                   notWords.add("codebook_no_kinds");
                   notWords.add("baseline");
                   notWords.add("1x1");
                   notWords.add("backwards");
                   break;
                case CODEBOOK_2x2_BOUNDARIES_BACKWARDS:
                    notWords.add("1x1");
                    break;
                case SEPARATE_ZERO_PREQUENTIAL:
                    notWords.add("joint");
                    notWords.add("inf");
                    notWords.add("baseline");
                    notWords.add("nml");
                    break;   
                case SEPARATE_ZERO_NML:
                    notWords.add("joint");
                    notWords.add("inf");
                    notWords.add("baseline");
                    notWords.add("prequential");
                    break;   
                case SEPARATE_NORMAL:
                    notWords.add("joint");
                    notWords.add("inf");
                    notWords.add("zero");
                    notWords.add("baseline");
                    break;
                case SEPARATE_NORMAL_NML:
                    notWords.add("joint");
                    notWords.add("inf");
                    notWords.add("zero");
                    notWords.add("baseline");
                    notWords.add("prequential");
                    break;
                case SEPARATE_NORMAL_PREQUENTIAL:
                    notWords.add("joint");
                    notWords.add("inf");
                    notWords.add("zero");
                    notWords.add("baseline");
                    notWords.add("nml");
                    break;    
                case SEPARATE_INF_PREQUENTIAL:
                    notWords.add("joint");
                    notWords.add("zero");
                    notWords.add("baseline");
                    notWords.add("nml");
                    break;
                case SEPARATE_INF_NML:
                    notWords.add("joint");
                    notWords.add("zero");
                    notWords.add("baseline");
                    notWords.add("prequential");
                    break;

                case JOINT_NORMAL:
                    notWords.add("zero");
                    break;
                case JOINT_ZERO:
                    notWords.add("inf");
                    break;

            }

            return notWords;
        }

        public static Collection<ModelType> getCorrelationModelTypes() {
            return EnumSet.of(
                    BASELINE,
                    CODEBOOK_NO_KINDS_1x1,
                    CODEBOOK_WITH_KINDS_1x1,
                    CODEBOOK_WITH_KINDS_1X1_SEPARATE,
                    CODEBOOK_WITH_KINDS_1X1_NML,
                    CODEBOOK_WITH_KINDS_1X1_SEPARATE_NML,
                    CODEBOOK_WITH_KINDS_1x1_BACKWARDS,
                    CODEBOOK_WITH_KINDS_2x2,
                    CODEBOOK_2x2_BOUNDARIES,
                    CODEBOOK_2x2_BOUNDARIES_BACKWARDS,
                    SEPARATE_NORMAL,
                    SEPARATE_ZERO,
                    SEPARATE_INF_NML
                    //JOINT_NORMAL
                    //JOINT_ZERO
                    );
        }

        public static Collection<ModelType> getContextModels() {
            return EnumSet.of(SEPARATE_ZERO_NML, SEPARATE_ZERO_PREQUENTIAL,  SEPARATE_NORMAL_PREQUENTIAL, SEPARATE_NORMAL_NML, SEPARATE_INF_NML,SEPARATE_INF_PREQUENTIAL, JOINT_NORMAL, JOINT_ZERO);
        }

    }

    public enum CostType {
        COST_MIN,
        COST_STD,
        COST_MEAN,

        NED_MIN,
        NED_STD,

        NFED_MIN,
        NFED_STD,
        NFED_MEAN,

        COST_NED_CORR,
        COST_NFED_CORR,

        MIN_COST_NED,
        MIN_COST_NFED,

        MIN_COST_NCD,

        ITERATIONS,
        BEST_ITER,
        BEST_SEED;

        //ACCURACY;

       
        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public enum Language {
        EST, FIN, KHN, KOM, MAN, MAR, MRD, SAA, UDM, UGR;

        public static Collection<String> getStarling10Languages() {
            List<String> langs = new ArrayList<String>();
//            langs.add("EST");
//            langs.add("FIN");
//            langs.add("KHN");
//            langs.add("KOM");
//            langs.add("MAN");
//            langs.add("MAR");
//            langs.add("MRD");
//            langs.add("SAA");
//            langs.add("UDM");
//            langs.add("UGR");

//            List<String> langs = new ArrayList<String>();
            //Languages to choose(top dialect): FIN EST KHN_DN KOM_S MAN_P MAR_KB MRD_E SAA_N UDM_S UGR
            langs.add("EST");
            langs.add("FIN");
            langs.add("KHN_DN");
            langs.add("KOM_S");
            langs.add("MAN_P");            
            langs.add("MAR_KB");
            langs.add("MRD_E");
            langs.add("SAA_N");
            langs.add("UDM_S");
            langs.add("UGR");
            
            //return EnumSet.allOf(Language.class);
            //return EnumSet.of(EST, FIN);
//            String[] dials = {"khn_dn", "khn_v", "kom_s", "kom_p", "man_p", "man_so", "mar_kb", "mar_b", "mrd_e", "mrd_m",  "udm_s", "udm_g"};
//            List<String> langs = Arrays.asList(dials);

            /*langs.add("ALT");
            //langs.add("ATU");
            langs.add("AZB");
            langs.add("BAS");
            langs.add("BLKX");
            //langs.add("CHG");
            langs.add("CHV");
            langs.add("TRK");
            langs.add("DOLG");
            langs.add("GAGX");
            langs.add("HAK");
            langs.add("JAK");
            langs.add("KAZ");
            langs.add("KHAL" );
            langs.add("KLPX");
            langs.add("KRG");
            //langs.add("KRH");
            langs.add("KRMX");
            langs.add("NOGX");
            langs.add("QUM");
            langs.add("SAL");
            langs.add("SHR");
            langs.add("SJG");
            langs.add("TAT");
            langs.add("TOF");
            langs.add("TRM");
            langs.add("TUV");
            langs.add("UIG");
            langs.add("UZB");*/

            return langs;
        }
        
        

        public static Collection<String> getSSALanguages() {
            return Arrays.asList(new String[]{"EST", "FIN"});
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    
    private static String logPath = "/home/group/langtech/Etymology-Project/etymon-logs/Uralic-Starling/2012-05-25/";
    //private static String logPath = "/home/group/langtech/Etymology-Project/etymon-logs/Uralic-Starling/2012-03-16/";
    private static String contextLogPath = logPath;//"/home/sxhiltun/sxhiltun/ety-nml/"; //logPath;
    //private static String contextLogPath = "/home/sxhiltun/sxhiltun/dialect-comparison/";
    //private static String ssaLogPath = "/home/sxhiltun/sxhiltun/ssa-fin-est-compression-data/fin-est-log";
    private static String ssaLogPath = "/home/sxhiltun/sxhiltun/ssa-fin-est-compression-data/ssa-log";

    //private static ModelType[] models = ModelType.values();
    //private static ModelType[] models = new ModelType[]{ModelType.CODEBOOK_NO_KINDS_1x1, ModelType.CODEBOOK_WITH_KINDS_1x1, ModelType.CODEBOOK_WITH_KINDS_1X1_NML, ModelType.CODEBOOK_WITH_KINDS_1X1_SEPARATE, ModelType.CODEBOOK_WITH_KINDS_1X1_SEPARATE_NML};
    //private static ModelType[] models = new ModelType[]{ModelType.SEPARATE_NORMAL_PREQUENTIAL, ModelType.SEPARATE_NORMAL_NML, ModelType.SEPARATE_ZERO_PREQUENTIAL, ModelType.SEPARATE_ZERO_NML, ModelType.SEPARATE_INF_PREQUENTIAL, ModelType.SEPARATE_INF_NML, ModelType.JOINT_NORMAL, ModelType.JOINT_ZERO};
    private static ModelType[] models = new ModelType[]{ModelType.CODEBOOK_WITH_KINDS_1x1, ModelType.SEPARATE_NORMAL_PREQUENTIAL};

    //private static ModelType[] models = new ModelType[]{ModelType.SEPARATE_NORMAL_NML};
    //private static String[] starling10langs = new String[]{"est", "fin", "khn", "kom", "man", "mar", "mrd", "saa", "udm", "ugr"};
    //private static String[] ssaLangs = new String[]{"est", "fin"};

    private static final int numberOfIterations = 50;

    private static final String INDICATOR_MEAN = "Mean: ";
    private static final String INDICATOR_MIN = "Min: ";
    private static final String INDICATOR_MAX = "Max: ";
    private static final String INDICATOR_STD = "Standard Deviation: ";


    private static final boolean collectContextModelResultsSeparately = false;
    private static final boolean collect3DModelResultsSeparately = false;
    private static final boolean doSSAData = false;
    
    
    public static void main(String[] args) throws FileNotFoundException, IOException, Exception {        
        readInAllData();
        
        //choose manually what you want to print: 
        printDataTablesGroupedByLanguage();
        //printSanityCheckComparisonFourWays();
        //printDataTablesGroupedByModel();
        //computeAndPrintCorrelationOfModels();
        
        //writeTaskFileByRandomSeed(null);
        //computeNCDAndPrintLxLTable(ModelType.CODEBOOK_WITH_KINDS_1X1_SEPARATE, CostType.COST_MIN, false, SelectionType.DO_NOTHING, CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE);
        //ComputeNCDforExistingMinCosts(ModelType.SEPARATE_ZERO_NML, CostFunctionIdentifier.NML,"/home/nouri/Desktop/utils/uralic17/", "/home/nouri/Desktop/utils/uralic17/NCD/");
     }


    private static Collection<String> listOfLanguages() {
        if (doSSAData) {
            return Language.getSSALanguages();
        } else {
            return Language.getStarling10Languages();
        }
    }

    private static void readAllCostsFromFiles(ModelType model, String[] languages) throws FileNotFoundException {
                   
        String logDir = logPath + model.name;
        if (collectContextModelResultsSeparately && ModelType.getContextModels().contains(model)) {
            logDir = contextLogPath + model.name;
        }

        if (doSSAData) {
            logDir = ssaLogPath;
        }

/*        if (!(new File(logDir).exists())) {
            System.out.println("Path doesn't exist: " + logDir);
            return;
        }*/
        
       
        List<File> files = getFiles(logDir, model.getMustWords(languages), model.getNotWords());
        
        if (collectContextModelResultsSeparately && ModelType.getContextModels().contains(model)) {
            collectAllIterations(files, model, languages);
        } else if (collect3DModelResultsSeparately) {

            collectAllIterations(files, model, languages);
        }
        else if (doSSAData) {
            collectAllIterations(files, model, languages);
        }
        else {
            for (File file : files) {               
                readCostLinesAndAddToMap(file, languages, model);
                readIterationDataAndAddToMap(file, model, languages);
            }
        }
               
        

    }

    private static void collectAllIterations(List<File> files, ModelType model, String[] languages) throws FileNotFoundException {

        List<Double> costs = new ArrayList<Double>();
        List<Double> editDistances = new ArrayList<Double>();
        List<Double> featureEditDistances = new ArrayList<Double>();
        List<Integer> iterations = new ArrayList<Integer>();
        List<Long> seeds = new ArrayList<Long>();




        for (File file : files) {
            List<String> costlines = getCostLinesForCorrelationComputation(file, "Iter:");
            if (costlines.isEmpty()) {
                continue;
            }

            String costLine = costlines.get(0);
            //System.out.println("costLIne: " + costLine);
            String[] cols = costLine.split("\\s+");

            costs.add(Double.parseDouble(cols[2]));
            iterations.add(Integer.parseInt(cols[0]));
            seeds.add(Long.parseLong(cols[1]));
            if (cols.length > 3){
                editDistances.add(Double.parseDouble(cols[3]));
                featureEditDistances.add(Double.parseDouble(cols[4]));                
            }
        }

        if (costs.isEmpty()) {
            return;
        }
        
        
        
        model.addCostToMap(languages, CostType.COST_MIN, EtyMath.getMin(costs));
        model.addCostToMap(languages, CostType.COST_MEAN, EtyMath.getMean(costs));
        
        if (!editDistances.isEmpty()) {
        model.addCostToMap(languages, CostType.NED_MIN, EtyMath.getMin(editDistances));
        model.addCostToMap(languages, CostType.NFED_MIN, EtyMath.getMin(featureEditDistances));
        model.addCostToMap(languages, CostType.NFED_MEAN, EtyMath.getMean(featureEditDistances));

        model.addCostToMap(languages, CostType.COST_STD, EtyMath.getStandardDeviation(costs));
        model.addCostToMap(languages, CostType.NED_STD, EtyMath.getStandardDeviation(editDistances));
        model.addCostToMap(languages, CostType.NFED_STD, EtyMath.getStandardDeviation(featureEditDistances));
        }
        
        model.addCostToMap(languages, CostType.ITERATIONS, costs.size());



        double[] costT = new double[costs.size()];
        double[] editT = new double[costs.size()];
        double[] featureT = new double[costs.size()];
        int[] iterT = new int[costs.size()];
        long[] seedT = new long[costs.size()];

        for (int i=0; i<costs.size(); i++) {
            costT[i] = costs.get(i);
            if (!editDistances.isEmpty()) {
                editT[i] = editDistances.get(i);
                featureT[i] = featureEditDistances.get(i);
            }
            iterT[i] = iterations.get(i);
            seedT[i] = seeds.get(i);
        }

        mapIterationData(costT, editT, featureT, iterT, seedT, model, languages);
    }
    
    private static boolean readIterationDataAndAddToMap(File file, ModelType model, String[] languages) throws FileNotFoundException, NumberFormatException {
        
        double[] costs = new double[numberOfIterations];
        double[] edits = new double[numberOfIterations];
        double[] featureEdits = new double[numberOfIterations];
        int[] iterations = new int[numberOfIterations];
        long[] seeds = new long[numberOfIterations];
        
        List<String> costlines = getCostLinesForCorrelationComputation(file, "Iter:");
        if (costlines.isEmpty()) {
            return false;
        }
        
        String costLine;
        for (int i = 0; i < numberOfIterations; i++) {
            costLine = costlines.get(i);            
            String[] cols = costLine.split("\\s+");
            if(cols.length <3){
                System.out.println("Error:");
            }
            costs[i] = Double.parseDouble(cols[2]);
            if (cols.length > 3) {
                edits[i] = Double.parseDouble(cols[3]);
                featureEdits[i] = Double.parseDouble(cols[4]);
            }
            iterations[i] = Integer.parseInt(cols[0]);
            seeds[i] = Long.parseLong(cols[1]);
        }
        mapIterationData(costs, edits, featureEdits, iterations, seeds, model, languages);
        
        return true;
    }

     private static void mapIterationData(double[] costs, double[] edits, double[] featureEdits,
             int[] iterations, long[] seeds, ModelType model, String[] languages) {
        
         //compute correlation
        double corr = Correlation.getPearsonCorrelation(costs, edits);
        model.addCostToMap(languages, CostType.COST_NED_CORR, corr);
        corr = Correlation.getPearsonCorrelation(costs, featureEdits);
        model.addCostToMap(languages, CostType.COST_NFED_CORR, corr);

        //find edist distance corresponding to minimum cost.
        double minCost = model.getCost(languages, CostType.COST_MIN).doubleValue();
        for (int i = 0; i < costs.length; i++) {
            if (costs[i] == minCost) {
                if (edits.length > 0) {
                    model.addCostToMap(languages, CostType.MIN_COST_NED, edits[i]);
                    model.addCostToMap(languages, CostType.MIN_COST_NFED, featureEdits[i]);
                }
                model.addCostToMap(languages, CostType.BEST_ITER, iterations[i]);
                model.addCostToMap(languages, CostType.BEST_SEED, seeds[i]);
                break;
            }
        }
    }

    private static void readCostLinesAndAddToMap(File file, String[] languages, ModelType model) throws FileNotFoundException {
        String costLine;
        
        //codelength min
        costLine =  getCostLine(file, "Total", INDICATOR_MIN);
        addCostToMap(languages, model, costLine, CostType.COST_MIN, INDICATOR_MIN);

        //codelength mean
        costLine =  getCostLine(file, "Total", INDICATOR_MEAN);
        addCostToMap(languages, model, costLine, CostType.COST_MEAN, INDICATOR_MEAN);
        
        //codelength std
        costLine = getCostLine(file, "Total", INDICATOR_STD);
        addCostToMap(languages, model, costLine, CostType.COST_STD, INDICATOR_STD);

        //edit dist min
        costLine = getCostLine(file, "Levenshtein", INDICATOR_MIN);
        addCostToMap(languages, model, costLine, CostType.NED_MIN, INDICATOR_MIN);

        //edit dist std
        costLine = getCostLine(file, "Levenshtein", INDICATOR_STD);
        addCostToMap(languages, model, costLine, CostType.NED_STD, INDICATOR_STD);

        //edit dist featurewise min
        costLine = getCostLine(file, "Featurewise", INDICATOR_MIN);
        addCostToMap(languages, model, costLine, CostType.NFED_MIN, INDICATOR_MIN);

        //edit dist featurewise std
        costLine = getCostLine(file, "Featurewise", INDICATOR_STD);
        addCostToMap(languages, model, costLine, CostType.NFED_STD, INDICATOR_STD);
        
        //edit dist featurewise std
        costLine = getCostLine(file, "Featurewise", INDICATOR_MEAN);
        addCostToMap(languages, model, costLine, CostType.NFED_MEAN, INDICATOR_MEAN);
        
        model.addCostToMap(languages, CostType.ITERATIONS, numberOfIterations);
        
    }

    private static void addCostToMap(String[] languages, ModelType model, String costLine, CostType costType, String costLineIndicator) {
        if (costLine == null) {
            System.out.println("costLine is null!!!");
            return;
        }
        
        double cost = getCostFromString(costLine, costLineIndicator);     
        model.addCostToMap(languages, costType,  cost);
    }



    private static List<File> getFiles(String logDir, List<String> filenameMustContain, List<String> filenameMustNotContain) {
        List<File> files = new ArrayList();

        if (!new File(logDir).exists()) {
            System.out.println("File does not exist: " + logDir);
            return files;
        }
        IterateFiles:
        for (File file : new File(logDir).listFiles()) {
            String filename = file.getName();
            
            if (doSSAData) {
                filename = filename.replace("ssa-fin-est", "ssa");
            }
            for (String tag : filenameMustContain) {
                if (!filename.contains(tag)) {
                     continue IterateFiles;
                }
            }

            for (String tag : filenameMustNotContain) {
                if (filename.contains(tag)) {
                    continue IterateFiles;
                }
            }

            files.add(file);
        }
        return files;
    }

    private static String getCostLine(File f, String catchThisFirst, String findThis) throws FileNotFoundException {

        Scanner sc = new Scanner(f);
        boolean found = false;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (found && line.contains(findThis)) {
                return line;
            }
            if (!line.contains(catchThisFirst)) {
                continue;
            }
            found = true;
        }
        sc.close();
        return null;
    }

    private static List<String> getCostLinesForCorrelationComputation(File f, String catchThisFirst) throws FileNotFoundException {

        Scanner sc = new Scanner(f);
        List<String> costlines = new ArrayList<String>();
        boolean found = false;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (found ) {
                costlines.add(line);
            }
            if (!line.contains(catchThisFirst)) {
                continue;
            }
            found = true;
        }
        sc.close();
        return costlines;
    }

    private static double getCostFromString(String line, String costLineIndicator) {

        try {            
            double cost = Double.parseDouble(line.substring(costLineIndicator.length()).trim());
            return cost;

        } catch(Exception e) {
            System.out.println("line: " + line);
            System.out.println("costName: " + costLineIndicator);
            System.exit(-1);
        }
        return -1;

    }



    private static void printCostTable(StringBuilder topic, StringBuilder data, CostType[] costTypes) {

        int columnLength  = 15;
        String columnwidth = "%-" +String.valueOf(columnLength) + "s";
        String topicwidth = "%-" + String.valueOf(costTypes.length * columnLength) + "s";

        System.out.printf(columnwidth, "#model: ");
        for (String s : topic.toString().split(" ")) {
            if (s.contains("\n")) {
                System.out.print(s);
            }else {
                System.out.printf(topicwidth, s);
            }
        }

        String output = data.toString();
        for (String s : output.split(" ")) {
            System.out.printf(columnwidth, s);
        }

    }


    private static void preprocessDataToBePrinted(StringBuilder data, String[] languages , ModelType[] modelTypes, CostType[] costTypes, boolean printItself) throws Exception {
        
        List langsAsList = new ArrayList(Arrays.asList(languages));
        if (!printItself && (new HashSet(langsAsList)).size() > 1) {
            return; 
        }

        
        DecimalFormat fourPlaces = new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.US));
        data.append(ModelType.getLanguageList(languages)).append(" ");
        //data.append(comparisonLang).append("-").append(language).append(" ");

        for (ModelType model : modelTypes) {
            for (CostType costType : costTypes) {
                Number cost;
                 
                cost = model.getCost(languages, costType);
                
                if (cost == null) {
                    data.append("-");
                }else if(cost.getClass().equals(Integer.class))  {
                    data.append(cost.intValue());
                }
                else if (cost.getClass().equals(Double.class)){
                    data.append(fourPlaces.format(cost.doubleValue()));
                }else {
                    data.append(cost.longValue());
                }
                data.append(" ");
            }
        }
        data.append("\n");

        
    }



    
    private static void readInAllData() throws FileNotFoundException {
        for (ModelType model : models) {
            
            for(String language: listOfLanguages()) {
                String lang1 = language.toLowerCase();
                for(String language2: listOfLanguages()) {
                    String lang2 = language2.toLowerCase();
                    String[] languages;
                    
                    if (collect3DModelResultsSeparately) {
                        for (String language3 : listOfLanguages()) {
                            
                    
                            String lang3 = language3.toLowerCase();
                            languages = new String[]{lang1, lang2, lang3};
                            
                            
                             List langsAsList = new ArrayList(Arrays.asList(languages));
                             if ((new HashSet(langsAsList)).size() != 3) {
                                    continue; 
                             }
                            
                            readAllCostsFromFiles(model, languages);
                            //writeTaskFileByRandomSeed(languages);
                            
                        }
                        
                    } else {
                        
                        languages = new String[]{lang1, lang2};
                        readAllCostsFromFiles(model, languages);                
                    }
                    
                    
                }
                                
            }
        }        
    }
    

    private static void printSanityCheckComparisonFourWays() {
        StringBuilder sb = new StringBuilder();    
        CostType costType = CostType.NFED_MIN;
        sb.append("         ").append("\t\t").append("Left To Right").append("\t\t\t").append("Right to Left").append("\n");
        sb.append("langs    ").append("\t\t").append("lang1-lang2").append("\t").append("lang2-lang1");
        sb.append("\t").append("lang1-lang2").append("\t").append("lang2-lang1").append("\n");
        for(String language1: listOfLanguages()) {
            String lang1 = language1.toLowerCase();
            for(String language2: listOfLanguages()) {
                String lang2 = language2.toLowerCase();
                sb.append(lang1).append("-").append(lang2).append("         ");
                sb.append("\t");
                for (ModelType model : models) {
                    sb.append(getCellFill(model, costType, new String[]{lang1, lang2}));
                    sb.append("\t");
                    sb.append(getCellFill(model, costType, new String[]{lang2, lang1}));
                    sb.append("\t");
                }
                sb.append("\n");
            }
        }
        
        System.out.println(sb.toString());
        
    }
    
    private static String getCellFill(ModelType model, CostType costType, String[] languages) {
        
        String fill;
        DecimalFormat fourPlaces = new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.US));
        Number cost = model.getCost(languages, costType);

        if (cost == null) {
            fill = "-";
        }else if(cost.getClass().equals(Integer.class))  {
            fill = String.valueOf(cost.intValue());
        }
        else if (cost.getClass().equals(Double.class)){
            fill = String.valueOf(fourPlaces.format(cost.doubleValue()));
        }else {
            fill = String.valueOf(cost.longValue());
        }
        
        
        return fill;
    }

    private static void printDataTablesGroupedByLanguage() throws Exception {

  
        //CostType[] costTypes = new CostType[]{CostType.COST_MIN, CostType.MIN_COST_NFED, CostType.MIN_COST_NED};
        /*
         * Choose the columns to be printed (the cost types)
         */
        CostType[] costTypes = new CostType[]{CostType.COST_MIN, CostType.MIN_COST_NFED};
        //CostType[] costTypes = new CostType[]{CostType.COST_MIN};

        //CostFunctionIdentifier cf = CostFunctionIdentifier.PREQUENTIAL;
         CostFunctionIdentifier cf = null;
        boolean computeNCD = false;

        String[] languages;
       
        if (computeNCD) {
            for (ModelType model : models) {
                System.out.println("Model: " + model);
                System.out.println("        " + "\t" + "c(lang1, lang2)" + "\t" + "c(lang1, lang1)" + "\t" + "c(lang2, lang2)" + "\t" + "NCD" );
               // System.out.println("lang:\t\tl1-l2\t\tl1-l1\t\tl2-l2\t\tncd");
                for(String language1: listOfLanguages()) {
                    String lang1 = language1.toLowerCase();
                    for(String language2: listOfLanguages()) {
                        String lang2 = language2.toLowerCase();
                        if ( !lang1.equals(lang2) ) {
                        
                        languages = new String[]{lang1, lang2};
                        
                        
                        
                        double cost = computeNCDCost(model,  lang1, lang2, model.getCost(languages, CostType.COST_MIN).doubleValue(), cf);
                        model.addCostToMap(languages, CostType.MIN_COST_NCD, cost);
                        }
                    }
                }
            }
            //return;
        }
        
        for(String language1: listOfLanguages()) {
            String lang1 = language1.toLowerCase();
            StringBuilder data = new StringBuilder();
            StringBuilder topic = new StringBuilder();            
            data.append("#Languages ");

            for (ModelType model : models) {
                topic.append(model);
                topic.append(" ");

                for (CostType costType : costTypes) {
                    data.append(costType);
                    data.append(" ");
                }
            }
            data.append("\n");
            topic.append("\n");
            
            for(String language2: listOfLanguages()) {
                String lang2 = language2.toLowerCase();
                
//                if (!lang1.split("_")[0].equals(lang2.split("_")[0])) {
//                    continue;
//                }
                
                if (lang1.equals(lang2)) {
                    continue;
                }
                

                if (collect3DModelResultsSeparately) {
                    for (String language3 : listOfLanguages()) {

                        String lang3 = language3.toLowerCase();
                        languages = new String[]{lang1, lang2, lang3};
                        
                        preprocessDataToBePrinted(data, languages, models, costTypes, true);                        
                    }                                            

                } else {
                    languages = new String[]{lang1, lang2};
                    preprocessDataToBePrinted(data, languages, models, costTypes, true);
                    
                }
                    
            }

            printCostTable(topic, data, costTypes);
            
            System.out.println("");
            System.out.println("");
        }
        printSegmentationInfo(models, costTypes);
    }
    
    private static void printSegmentationInfo(ModelType[] models, CostType[] costTypes) {
        int languageNumber = 1;
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n\n");
        DecimalFormat fourPlaces = 
                new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.US));
        
        List<String> languageList = (List<String>) listOfLanguages();
        for(String language1: languageList) {
            String lang1 = language1.toLowerCase();                        
                        
            for(String language2: languageList.subList(languageNumber, languageList.size())) {
                String lang2 = language2.toLowerCase();
                                                
                String[] languages = new String[]{lang1, lang2};                                
                sb.append(lang1).append("-").append(lang2).append("\t");
                StringBuilder align = new StringBuilder();
                align.append(lang1).append("-").append(lang2);
                if (align.length()<8) {                    
                    sb.append("\t");
                }
                
                for (ModelType model : models) {
                    for (CostType cost : costTypes) {
                        sb.append(fourPlaces.format(model.getCost(languages, cost)));
                        sb.append("\t");
                    }
                    sb.append("\t");
                }
                
                sb.append("\n");
                
                String[] languages2 = new String[]{lang2, lang1};                
                sb.append(lang2).append("-").append(lang1).append("\t");
                align = new StringBuilder();
                align.append(lang2).append("-").append(lang1);
                if (align.length()<8) {                    
                    sb.append("\t");
                }
                
                for (ModelType model : models) {
                    for (CostType cost : costTypes) {
                        sb.append(fourPlaces.format(model.getCost(languages2, cost)));
                        sb.append("\t");
                    }
                    sb.append("\t");
                }
                
                sb.append("\n\n");
                                    
            }
            
            languageNumber++;


        }
        
        System.out.println(sb.toString());
    }
    
    


    private static void printDataTablesGroupedByModel() throws FileNotFoundException, Exception {
        //String comparisonLang = "fin";
        for (ModelType model : models) {

            //add topic line
            StringBuilder topic = new StringBuilder();
            topic.append(model);
            topic.append(" ");
            topic.append("\n");

            //add matrix headers
            StringBuilder data = new StringBuilder();
            data.append("#LANGUAGES:").append(" ");
            for (CostType costType : CostType.values()) {
                data.append(costType.toString()).append(" ");
            }
            data.append("\n");

            String[] languages;
            //print data
            for(String language: listOfLanguages()) {
                String lang1 = language.toLowerCase();
                
                for(String language2: listOfLanguages()) {
                    String lang2 = language2.toLowerCase();

                    if (collect3DModelResultsSeparately) {
                        for (String language3 : listOfLanguages()) {
                            String lang3 = language3.toLowerCase();
                            languages = new String[]{lang1, lang2, lang3};                             
                            List langsAsList = new ArrayList(Arrays.asList(languages));
                             if ((new HashSet(langsAsList)).size() != 3) {
                                    continue; 
                             }

                            preprocessDataToBePrinted(data, languages, new ModelType[]{model}, CostType.values(), true);                        
                        }                                            

                    } else {
                        languages = new String[]{lang1, lang2};
                        preprocessDataToBePrinted(data, languages, new ModelType[]{model}, CostType.values(), true);                        

                    }

                }                                                                                                
            }
            
            printCostTable(topic, data, CostType.values());
            System.out.println("");
            System.out.println("");
        }
    }

    private static void computeAndPrintCorrelationOfModels() {

        System.out.println("Correlations: " + ModelType.getCorrelationModelTypes());
        List<Double> correlations = new ArrayList<Double>();

        
        for(String lang1: listOfLanguages()) {
            String language1  = lang1.toLowerCase();
            for(String lang2: listOfLanguages()) {
                String language2  = lang2.toLowerCase();
                
                if (language1.equals(language2)) {
                    continue;
                }
                
                String[] languages = new String[]{language1, language2};
                double[] costList = new double[ModelType.getCorrelationModelTypes().size()];
                double[] nfedList = new double[ModelType.getCorrelationModelTypes().size()];
                
                int counter = 0;
                for (ModelType model : ModelType.getCorrelationModelTypes()) {
                    Double cost = model.getCost(languages, CostType.COST_MIN).doubleValue();
                    Double minCostNfed = model.getCost(languages, CostType.MIN_COST_NFED).doubleValue();

                    if (cost != null && minCostNfed !=null) {
                        costList[counter] = cost;
                        nfedList[counter] = minCostNfed;
                        counter++;
                    }

                }
                double corr = Correlation.getPearsonCorrelation(Arrays.copyOfRange(costList, 0, counter),
                        Arrays.copyOfRange(nfedList, 0, counter));
                correlations.add(corr);

                System.out.print(language1 + "-" + language2);
                System.out.print("\t");
                System.out.println(corr);

            }
        }

        double sum = 0;
        double avg = 0;
        for (double d : correlations) {
            sum += d;
        }
        avg = sum/correlations.size();
        System.out.println("");
        System.out.println ("Correlation avg. " + avg);
    }


    public static void writeTaskFileByRandomSeed(String[] optionalLanguages) {

        ModelType.SEPARATE_INF_PREQUENTIAL.setModelParameters("-inf -binary -costfunction prequential");
        ModelType.SEPARATE_ZERO_PREQUENTIAL.setModelParameters("-zero -binary -costfunction prequential");
        ModelType.SEPARATE_INF_NML.setModelParameters("-inf -costfunction nml");
        ModelType.SEPARATE_ZERO_NML.setModelParameters("-zero -binary -costfunction nml ");
        ModelType.SEPARATE_NORMAL_PREQUENTIAL.setModelParameters( "-binary -costfunction prequential ");
        ModelType.SEPARATE_NORMAL_NML.setModelParameters( "-binary -costfunction nml ");
        ModelType.JOINT_NORMAL.setModelParameters("-joint -binary ");
        ModelType.JOINT_ZERO.setModelParameters("-joint -zero -binary ");
        
        String toReturn = "";

        String logDir = "-lp /home/group/langtech/Etymology-Project/etymon-logs/Uralic-Starling/2012-04-24-limited-candidates/best/context-separate-normal-nml-best ";
        String inputFile ="-f /home/group/langtech/Etymology-Project/StarLing/uralet/starling-input-data/starling-10-top2-dialects.utf8 ";

        //String logDir = "-lp /fs-2/a/sxhiltun/etyBestIterationsSSA/";
        //String inputFile = "-f /fs-2/a/sxhiltun/ssa-fin-est-compression-data/ssa-fin-est-3500 ";


        String executeJar = "nice java -jar ";
        String jarPath = "/home/group/langtech/Etymology-Project/Javad/EtyMalign.jar  ";
        

        
        String fixedParameters = inputFile  + " -v context_2d -a -alpha 0.995 -tf 0 -impute ";
        //String fixedParameters = inputFile + "-costfunction codebook-and-kinds -a -impute "; 
        //String fixedParameters = inputFile  + "-of -v marginal -a -impute -costfunction eq2 ";
        String command = executeJar + jarPath + fixedParameters;
        ModelType model = ModelType.JOINT_NORMAL;
        
        if (optionalLanguages != null) {
            String lang1 = optionalLanguages[0];
            String lang2 = optionalLanguages[1];
            String lang3 = optionalLanguages[2];
            
            String runSpecificParameters = 
                            " -l " + lang1.toUpperCase() + " -l " + lang2.toUpperCase() + " -l " + lang3.toUpperCase() + 
                            " " + logDir  + " -seed " + String.valueOf(model.getCost(optionalLanguages, CostType.BEST_SEED));
            
             System.out.println(command + runSpecificParameters);            
             return;
        }

        for (String l1 : Language.getStarling10Languages()) {
        //for (Language l1 : Language.getSSALanguages()) {
            String lang1 = l1.toLowerCase();
            
            for (String l2 : Language.getStarling10Languages()) {
                if(l1.equals(l2)){
                    continue;
                }
            //for (Language l2 : Language.getSSALanguages()) {
                String lang2 = l2.toLowerCase();
              
//                for (String l3 : Language.getStarling10Languages()) {
//                    String lang3 = l3.toLowerCase();
                                
                    //String[] languages = new String[]{lang1, lang2, lang3};
                    String[] languages = new String[]{lang1, lang2};
                    
                    //for (ModelType model : ModelType.getContextModels()) {
                     String runSpecificParameters = 
                            "-l " + lang1.toUpperCase() + " -l " + lang2.toUpperCase() + " " + model.getModelParameters()
                             + logDir  + " -seed " + String.valueOf(model.getCost(languages, CostType.BEST_SEED));

                        //+ model.name + " " + model.getModelParameters()
                        
                        
                        System.out.println(command +runSpecificParameters );
                    //}
                //}

            }
        }
        
    }

    private static void computeAveragedEditDistance(String l1, String l2) throws Exception {
        Configuration config = CommandLineReader.readConfiguration(null);
        String inputfile;
        if (doSSAData) {
            inputfile = "/fs-2/a/sxhiltun/ssa-fin-est-compression-data/ssa-fin-est-3500";
        }else {
            inputfile = "/group/home/langtech/Etymology-Project/StarLing/starling-top-10";
        }

        config.setLanguages(Arrays.asList(new String[]{l1, l2}));
        config.setInputFile("/group/home/langtech/Etymology-Project/StarLing/starling-10");
        Input input = new Input(config);
        input.getTotalGlyphs();

    }

    




    
    private enum SelectionType {
        MIN, AVG, DO_NOTHING, MAX
    }
    
    private static void computeNCDAndPrintLxLTable(ModelType model, CostType ct, boolean plainCost, SelectionType selectionType,  CostFunctionIdentifier cf) throws Exception {
        Collection<String> languages = listOfLanguages();
        Map<String, Map<String, Double>> data = new TreeMap<String, Map<String, Double>>();
        DecimalFormat fourPlaces = new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.US));
        
        for(String language1: languages) {
            String lang1 = language1.toLowerCase();
           
            for(String language2: languages) {
                String lang2 = language2.toLowerCase();
                if (!data.containsKey(lang1)) {
                    data.put(lang1, new TreeMap());
                }

                
                double costBetween; 
                Number cb =  model.getCost(new String[]{lang1, lang2}, ct);
                
                if (cb != null) {
                    costBetween = cb.doubleValue();
                } else {
                    costBetween = -1;//model.getCost(new String[]{lang2, lang1}, ct).doubleValue();
                }
                
                double matrixCost;
                if (ct == CostType.COST_MIN && !plainCost) {
                    double ncdCost = computeNCDCost(model, language1, language2, costBetween, cf);
                    matrixCost = ncdCost;
                }
                else { //ned, nfed
                    
                    matrixCost = costBetween;
                    
                }
                                                             
                data.get(lang1).put(lang2, matrixCost);                
                                
            }

        }                               
        //form the table
        //getMapAsTable(languages, data, fourPlaces);

        //print the table
        System.out.println("MODEL: " + model.toString() + 
                ", COST TYPE:  " + ct.toString() + ", SELECTED FROM MATRIX: " + selectionType.toString());
        System.out.println(getMapAsTable(languages, data, fourPlaces, selectionType));
        
    }
    
    private static double computeNCDCost(ModelType model, String language1, String language2, double costBetween, CostFunctionIdentifier cf) throws Exception {
        
        DecimalFormat fourPlaces = new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.US));
        
        StringBuilder sb = new StringBuilder();
        
        
        
        sb.append(language1).append("-").append(language2);
        
        
        if (sb.length() < 8) {
            sb.append("\t");
        }
        
        sb.append("\t");
        
        
        sb.append(fourPlaces.format(costBetween));
        sb.append("\t");
        
        
        //compute the language against itself cost for the words that also in the other language
        LanguageAgainstItselfCostComputer laicc;
        laicc = new LanguageAgainstItselfCostComputer(model, cf, language1, language2);
        double costL1 = laicc.getLanguageAgainstItselfCost();
        sb.append(fourPlaces.format(costL1));
        sb.append("\t");
        
        laicc = new LanguageAgainstItselfCostComputer(model, cf, language2, language1);
        double costL2 = laicc.getLanguageAgainstItselfCost();                
        sb.append(fourPlaces.format(costL2));
        sb.append("\t");
        //System.out.println(lang1 + " " + lang2);
        //System.out.println("b: " + costBetween + " costl1: " + costL1  + " cost2: " + costL2);
        double ncdCost = EtyMath.getNormalizedCompressionDistance(costBetween, costL1, costL2);
        sb.append(fourPlaces.format(ncdCost));
        sb.append("\t");
        
        
        
        System.out.println(sb.toString());
        return ncdCost;
        
    }
    
    
 
    private static String getMapAsTable(Collection<String> languages, Map<String, Map<String, Double>> map, 
            DecimalFormat format,  SelectionType s) {
        StringBuilder sb = new StringBuilder();

        int singleCellLength = 8;

        // print header
        sb.append(StringUtils.leftAlign(singleCellLength, ""));
        for (String language : map.keySet()) {
            sb.append(StringUtils.leftAlign(singleCellLength, language.toLowerCase()));
        }
        sb.append("\n");

        for (String rowLang : map.keySet()) {
            sb.append(StringUtils.leftAlign(singleCellLength, rowLang));
            for (String headerLang : map.keySet()) {

                if (headerLang.equals(rowLang)) {
                    sb.append(StringUtils.leftAlign(singleCellLength, "0.0000"));
                } else {
                    
                    double val = map.get(rowLang).get(headerLang);                    
                    double val2 = map.get(headerLang).get(rowLang);
                    
                    switch (s) {
                        case AVG: 
                            if (Math.abs(val - val2) > 0.000001) {
                                val = (val + val2)/2;
                            }
                            break;
                        case MIN: 
                            if (val > val2) {
                                val = val2;
                            }
                            break;
                        case MAX:
                            if (val < val2) {
                                val = val2;
                            }
                            break;
                        case DO_NOTHING: 
                            break;
                    }
                    

                    sb.append(StringUtils.leftAlign(singleCellLength, format.format(val)));
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }


}
 class Correlation {
     
    public static double getPearsonCorrelation(double[] scores1, double[] scores2){
        double result = 0;
        double sum_sq_x = 0;
        double sum_sq_y = 0;
        double sum_coproduct = 0;
        double mean_x = scores1[0];
        double mean_y = scores2[0];
        for(int i=2;i<scores1.length+1;i+=1){
            double sweep =Double.valueOf(i-1)/i;
            double delta_x = scores1[i-1]-mean_x;
            double delta_y = scores2[i-1]-mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        double pop_sd_x = (double) Math.sqrt(sum_sq_x/scores1.length);
        double pop_sd_y = (double) Math.sqrt(sum_sq_y/scores1.length);
        double cov_x_y = sum_coproduct / scores1.length;
        result = cov_x_y / (pop_sd_x*pop_sd_y);
        return result;
    }
    

}



