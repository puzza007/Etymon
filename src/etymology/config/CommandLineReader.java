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
package etymology.config;

import com.sun.org.apache.xerces.internal.xs.StringList;
import etymology.align.AlignmentMatrixType;
import etymology.cost.CostFunctionIdentifier;
import etymology.cost.SuffixCostType;
import etymology.data.convert.ConversionRules;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 *
 * @author avihavai
 */
public class CommandLineReader extends Constants implements CLOptions {
    
    public static Configuration readConfiguration(String[] args) throws Exception {        
        //parse and get the commandLine object
        CommandLine commandLine = getCommandLine(args);
        
        //First parse the config file
        Configuration config = Configuration.getInstance();
        parseConfigFile(commandLine, config);
        
        //get Configuration object
                
        //set config according to commandLine 
        config.setLanguages(getLanguages(commandLine));
        if(config.getMaxGlyphsToAlign() > getMaxGlyphsToAlign(commandLine)){
            config.setMaxGlyphsToAlign(config.getMaxGlyphsToAlign());
        }else{
            config.setMaxGlyphsToAlign(getMaxGlyphsToAlign(commandLine));
        }
        config.setInputFile(getInputFile(commandLine));
        config.setUseSimulatedAnnealing(config.isUseSimulatedAnnealing() || commandLine.hasOption(ANNEALING_OPTION));
        config.setLogPath(getLogPath(commandLine));
        config.setAlignmentType(getAlignmentType(commandLine));
        config.setPrintOnlyFinalLogs(config.isPrintOnlyFinalLogs() || commandLine.hasOption(PRINT_ONLY_FINAL_LOGS));       
        if(!config.areWordsFlippedAround()){
            config.setWordsFlippedAround(commandLine.hasOption(REVERSED_WORDS));        
        }
        if(getWordsToMonitor(commandLine) != null){
            config.setWordsToMonitor(getWordsToMonitor(commandLine));        
        }
        config.setInitialAnnealingTemp(getInitialAnnealingTemp(commandLine));
        config.setAnnealingMultiplier(getAnnealingMultiplier(commandLine));
        config.setCostFunctionIdentifier(getCostFunctionIdentifier(commandLine));        
        config.setTreeRebuildingFrequency(getTreeRebuildingFrequency(commandLine));
        config.setConversionRules(getConversionRules(commandLine));
        config.setGoldStandardFilePath(getGoldStandardFilePath(commandLine));
        config.setRepetitionCount(getRepetitionCount(commandLine));
        config.setDoFirstBaselineThenContext(config.isFirstBaselineThenContext() || commandLine.hasOption(BASELINE_CONTEXT_COMBINATION));
        config.setDoZeroDepthTricks(config.isZeroDepthTricks() || commandLine.hasOption(ZERO_LEVEL_ON));
        config.setInfiniteDepth(config.getInfiniteDepth() || commandLine.hasOption(INFINITE_DEPTH_RESTRICTED));
        config.setBinaryValueTrees(config.isUseBinaryValueTrees() || commandLine.hasOption(BINARY_VALUE_TREES));
        config.setMultipleValueTreesOff(!config.areMultipleValueTreesOn() || commandLine.hasOption(NO_MULTIPLE_VALUE_TREES));
        config.setJointCoding(config.isJointCoding() || commandLine.hasOption(JOINT_CODING));
        config.setUsePreviousVersion(config.isUsePreviousVersion() || commandLine.hasOption(USE_PREVIOUS_CONTEXT_MODEL));
        config.setTakeStartsAndEndsIntoAccount(config.isTakeStartsAndEndsIntoAccount() || commandLine.hasOption(UTILIZE_WORD_BOUNDARIES));
        config.setImpute(config.isUseImputation() || commandLine.hasOption(IMPUTE));
        config.setRemoveSuffixes(config.isRemoveSuffixes() || commandLine.hasOption(REMOVE_SUFFIXES));
        config.setIterationNumber(getIterationNumber(commandLine));
        config.setRandomSeed(getSeed(commandLine));
        config.setLogRegretMatrixFileName(getLogregretFileName(commandLine));
        config.setDictionaryFileName(getDictionaryFileName(commandLine));        
        config.setConfigFileName(getConfigFile(commandLine));
        return config;
    }
    public static void parseConfigFile(CommandLine cl, Configuration config) {
        String configFileName = cl.getOptionValue(CLOptions.CONFIG_FILE_OPTION);        
        try {
            if(null == configFileName){
                return;
            }
            File configFile = new File(configFileName);
            if(!configFile.exists()){
                //Does not exist
                System.err.println("Config file \"" + configFileName + "\" does not exist!");
                System.exit(2);                            
            }
            //Parsing
            Scanner scanner = new Scanner(configFile);
            while(scanner.hasNextLine()){
                String line = scanner.nextLine();
                if(line.contains("#")){
                    line = line.substring(0, line.indexOf("#"));
                }
                line = line.trim();
                if(0 == line.length()){
                    continue;
                }
                parseLine(line, config, cl);
            }
        } catch (IOException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void parseLine(String line, Configuration config, CommandLine cl) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(line);
        String identifier = tokenizer.nextToken();
        //Now see what it is
        //Options with values
        String value = line.substring(line.indexOf(identifier)+identifier.length()).trim();
        
        if(identifier.equals(ConfigFileOptions.INPUT_FILE_OPTION)){
            if(cl.hasOption(CLOptions.INPUT_FILE_OPTION)){
                return;
            }
            config.setInputFile(value);            
        }else if(identifier.equals(ConfigFileOptions.LOG_REGRET_MATRIX_FILE_OPTION)){
            if(cl.hasOption(CLOptions.LOG_REGRET_MATRIX_FILE_OPTION)){
                return;
            }
            config.setLogRegretMatrixFileName(value);           
        }else if(identifier.equals(ConfigFileOptions.CONVERSION_RULES)){            
            if(cl.hasOption(CLOptions.CONVERSION_RULES)){
                return;
            }
            File f = new File(value);
            if(!f.exists()) {
                System.err.println("Conversion rule file " + value + " does not exist.");
            }
            config.setConversionRules(new ConversionRules(f));            
        }else if(identifier.equals(ConfigFileOptions.DICTIONARY_FILE)){
            if(cl.hasOption(CLOptions.DICTIONARY_FILE_OPTION)){
                return;
            }
            config.setDictionaryFileName(value);            
        }else if(identifier.equals(ConfigFileOptions.COST_FUNCTION)){            
            if(cl.hasOption(CLOptions.COST_FUNCTION)){
                return;
            }
            CostFunctionIdentifier costIdentifier = getCostFunctionIdentifierByString(value);
            if(null == costIdentifier){
                System.err.println("Invalid Cost function identifer: \"" + value + "\" in config file.");
                System.exit(-1);
            }
            config.setCostFunctionIdentifier(costIdentifier);
        }else if(ConfigFileOptions.LOG_FILE_PATH.equals(identifier)){
            if(cl.hasOption(LOG_FILE_PATH)){
                return;
            }
            config.setLogPath(value);
        }else if(ConfigFileOptions.INITIAL_ANNEALING_TEMP.equals(identifier)){
            try{
                double initTemp = Double.parseDouble(value);
                config.setInitialAnnealingTemp(initTemp);
            }catch(Exception e){
                System.err.println("Simulated annealing temperature is not in correct format in config file.");
                System.exit(-1);
            }
        }else if(ConfigFileOptions.ANNEALING_MULTIPLIER.equals(identifier))
        {
            //Not implemented yet!
            try{
                double saMultiplier = Double.parseDouble(value);
                System.out.println("SA cooling factor: " + saMultiplier);
                config.setAnnealingMultiplier(saMultiplier);
            }catch(Exception e){
                System.err.println("Simulated annealing cooling schedule is not in correct format in config file.");
                System.exit(-1);
            }
        }else if(ConfigFileOptions.TREE_REBUILDING_FREQUENCY.equals(identifier)){
            try{
                int treeFreq = Integer.parseInt(value);
                config.setTreeRebuildingFrequency(treeFreq);
            }catch(Exception e){
                System.err.println("Tree rebuilding frequency is not in correct format in config file: " + value);
                System.exit(-1);
            }
        }
        else if(ConfigFileOptions.REPETITION_COUNT.equals(identifier)){
            try{
                int repCount = Integer.parseInt(value);
                config.setRepetitionCount(repCount);
            }catch(Exception e){
                System.err.println("Config file parse error, repetition count is not in correct format: " + value);
                System.exit(-1);
            }
        }else if(ConfigFileOptions.ITERATION.equals(identifier)){
            try{
                int iterationNumber = Integer.parseInt(value);
                config.setIterationNumber(iterationNumber);
            }catch(Exception e){
                System.err.println("Config file parsing error: iteration number is  not in correct formar: " + value);
                System.exit(-1);
            }
        }else if(ConfigFileOptions.SEED.equals(identifier)){
            try{
                long seed = Long.parseLong(value);
                config.setRandomSeed(seed);
            }catch(Exception e){
                System.err.println("Config file parsing error: random seed is not in correct format: " + value);
                System.exit(-1);
            }        
        }else if(ConfigFileOptions.GOLD_STANDARD.equals(identifier)){            
            config.setGoldStandardFilePath(value);
        }else if(ConfigFileOptions.WORDS_TO_MONITOR.equals(identifier)){
            String[] words = value.split("\\s");
            config.setWordsToMonitor(Arrays.asList(words));
        }else if(ConfigFileOptions.LANGUAGES_OPTION.equals(identifier)){
            String[] langs = value.split("\\s");
            config.setLanguages(Arrays.asList(langs));
        }else if(ConfigFileOptions.VERSION_OPTION.equals(identifier))
        {
            if(value.length() > 0){
                config.setVersion(value);
            }else{
                System.err.println("Error parsing version");
                System.exit(-1);
            }
        }else{
            handleTrueFalseOption(identifier, value, line, config);
            //System.err.println("Not implemented yet! : " + identifier);
        }
    }
    
    private static CostFunctionIdentifier getCostFunctionIdentifierByString(String version) {        
        if ("eq1".equals(version) || "baseline".equals(version)) {
            return CostFunctionIdentifier.BASELINE;
        } else if ("eq2".equals(version) || "codebook-no-kinds".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_NO_KINDS;
        } else if ("eq16".equals(version) || "codebook-and-kinds".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE;
        } else if ("eq22".equals(version) || "written-out-nxn".equals(version)) {
            return CostFunctionIdentifier.WRITTEN_OUT_NXN;
        } else if ("prequential".equalsIgnoreCase(version)) {
            return CostFunctionIdentifier.PREQUENTIAL;            
        } else if ("nml".equalsIgnoreCase(version)) {
            return CostFunctionIdentifier.NML;
        } else if ("codebook-and-kinds-separate".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE;
        } else if ("codebook-and-kinds-separate-nml".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE_NML;
        } else if ("codebook-and-kinds-nml".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML;
        }

        return null;
    }
    

    private static String getConfigFile(CommandLine cl) {
        String configFileName = cl.getOptionValue(CONFIG_FILE_OPTION);
        System.out.println("Config File: " + configFileName);
        return configFileName;
    }
    private static String getLogregretFileName(CommandLine cl) {
        String fileName = cl.getOptionValue(LOG_REGRET_MATRIX_FILE_OPTION);
        if(null != fileName){            
            return fileName;
        }
        if(null != Configuration.getInstance().getLogRegretMatrixFileName()){
            return Configuration.getInstance().getLogRegretMatrixFileName();
        }        
        return Constants.LOG_REGRET_MATRIX_FILE;
    }
    private static List<String> getLanguages(CommandLine cl) {
        String[] langs = cl.getOptionValues(LANGUAGES_OPTION);
        // set languages to align
        if (langs == null || langs.length == 0 || langs.length > 3) {
            if(Configuration.getInstance().getLanguages() != null){
                return Configuration.getInstance().getLanguages();//.toArray();
            }else{
                langs = new String[]{"FIN", "EST"};            
            }
        }

        return Arrays.asList(langs);
    }

    private static List<String> getWordsToMonitor(CommandLine cl) {
        String[] words = cl.getOptionValues(WORDS_TO_MONITOR);
        // set languages to align
        if (words == null || words.length == 0) {
            //if(Configuration.getInstance().getWordsToMonitor() != null){
            //    return Configuration.getInstance().getWordsToMonitor();
            //}
            return null;
        }

        return Arrays.asList(words);
    }

    private static int getMaxGlyphsToAlign(CommandLine cl) {
        // set max glyphs to align
        if (cl.hasOption(ALIGN_TWO_GLYPHS)) {
            return 2;
        }

        return 1;
    }

    private static String getLogPath(CommandLine cl) {
        String logPath = cl.getOptionValue(LOG_FILE_PATH);
        if (logPath == null) {
            if(null != Configuration.getInstance().getLogPath()){
                logPath = Configuration.getInstance().getLogPath();
            }else{
                logPath = "log/";    
            }            
        }
        return logPath;
    }

    private static String getInputFile(CommandLine cl) {
        String inputFile = cl.getOptionValue(INPUT_FILE_OPTION);
        if (inputFile == null) { 
            if(null == Configuration.getInstance().getInputFile()){
                inputFile = Constants.DATA_PATH;
            }else{
                return Configuration.getInstance().getInputFile();
            }
        }

        return inputFile;
    }


    private static int getIterationNumber(CommandLine cl) {
        String reps = cl.getOptionValue(ITERATION);
        if(null != reps){
            try {
                return Integer.parseInt(reps);
            } catch (Exception e) {
                System.err.println("Command line parsing error, iteration number is not in correct format: " + reps);
                System.exit(-1);
            }
        }
        if(Configuration.getInstance().getIterationNumber() > 0){
            return Configuration.getInstance().getIterationNumber();
        }
        return 0;
    }

    private static int getRepetitionCount(CommandLine cl) {
        String reps = cl.getOptionValue(REPETITION_COUNT);
        if(null != reps){
            try {
                return Integer.parseInt(reps);
            } catch (Exception e) {
                System.err.println("Command line parsing error, repetition count is not in correct format: " + reps);
                System.exit(-1);
            }
        }
        if(Configuration.getInstance().getRepetitionCount() > 0)
            return Configuration.getInstance().getRepetitionCount();
        return 1;
    }

    public static CommandLine getCommandLine(String[] args) throws Exception {
        Options opt = getOptions();
        if (args.length == 0) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("java -jar <this>.jar", opt);
        }

        CommandLineParser parser = new GnuParser();
        return parser.parse(opt, args);
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(ANNEALING_OPTION, false, "use simulated annealing");
        options.addOption(LANGUAGES_OPTION, "languages", true, "languages to utilize");
        options.addOption(WORDS_TO_MONITOR, "monitor", true, "words to monitor, these will have their viterbi matrices printed out in each iteration");
        options.addOption(INPUT_FILE_OPTION, "file", true, "input file");
        options.addOption(LOG_FILE_PATH, true, "path to put logs into");
        options.addOption(PRINT_ONLY_FINAL_LOGS, false, "print only final logs");        
        options.addOption(VERSION_OPTION, true, "choose:\n"
                + " <two-lang> -- 2D symbol alignment \n "
                + "<joint> or <marginal> -- 3D symbol alignment\n  "
                + "<context_2d> -- 2D context alignment \n "
                + "<context_marginal_3d> -- 3D context alignment:");
        options.addOption(LOG_TO_CONSOLE, false, "log to console, default false");
        options.addOption(ALIGN_TWO_GLYPHS, false, "use also glyph-pairs for alignment (default false)");
        options.addOption(INITIAL_ANNEALING_TEMP, true, "initial annealing temperature, 50 by default");
        options.addOption(COST_FUNCTION, true, "cost function to use -- \n"
                + "<baseline> -- use only with 2D 1x1 non-context model, \n"
                + "<codebook-no-kinds> -- 2D and 3D 1x1 non-context models, \n"
                + "<codebook-and-kinds> -- 2D and 3D 1x1 non-context models, 2D 2x2 model, code kinds only in codebook\n"
                + "<codebook-and-kinds-separate> -- 2D and 3D 1x1 non-context models, code kinds in both codebook and conditional part \n"
                + "<codebook-and-kinds-separate-nml> -- use nml instead of prequential (separate kinds)\n"
                + "<codebook-and-kinds-nml> -- use nml (not separate kinds) \n"
                + "(written-out-nxn -- not used, possibly out-of-date code), \n"
                + "<prequential> -- prequential coding for context models, \n"
                + "<nml> -- nml coding for context separate model \n"
                + "default codebook-no-kinds");
        options.addOption(CONVERSION_RULES, true, "file containing conversion rules");
        options.addOption(REVERSED_WORDS, false, "flip the words around");
        options.addOption(TREE_REBUILDING_FREQUENCY, true, "tree rebuilding frequency, 0 by default");
        options.addOption(ANNEALING_MULTIPLIER, true, "the temperature multiplier in simulated annealing, 0.99 by default");
        options.addOption(REPETITION_COUNT, true, "repetition count");
        options.addOption(GOLD_STANDARD, true, "path to gold standard file");
        options.addOption(BASELINE_CONTEXT_COMBINATION, false, "do first baseline alignment, then context alignment");
        options.addOption(ZERO_LEVEL_ON, false, "depth 0 trees have some different restrictions, defined in wiki, default false");
        options.addOption(INFINITE_DEPTH_RESTRICTED, false, "restrictions in normal simann, default false");
        options.addOption(JOINT_CODING, false, "use joint coding instead of separate");
        options.addOption(BINARY_VALUE_TREES, false, "compute also binary candidates of trees, default false");
        options.addOption(NO_MULTIPLE_VALUE_TREES, false, "Use to switch off the computation of multi-value candidates for trees, default on");
        options.addOption(USE_PREVIOUS_CONTEXT_MODEL, false, "Use the old version of the zero- or infinite-depth model, "
                + "check functionality before using");
        options.addOption(UTILIZE_WORD_BOUNDARIES, false, "Use word initial and end symbols. Use only with option -g2. Default false");
        options.addOption(IMPUTE, false, "Compute the imputation distance after building the model. default false");
        options.addOption(REMOVE_SUFFIXES, false, "try to remove the suffixes of word, default false");
        options.addOption(ITERATION, true, "add the number of iteration, this terminates logging, default zero");
        options.addOption(SEED, true, "insert the random seed to be used");
        options.addOption(LOG_REGRET_MATRIX_FILE_OPTION, true, "insert the path to logregret matrix file.");
        options.addOption(CONFIG_FILE_OPTION, true, "insert the path to config file. Commandline options will override the settings in config file");
        options.addOption(DICTIONARY_FILE_OPTION, true, "insert the path to feature dictionary file.");
        
        return options;
    }




    private static AlignmentMatrixType getAlignmentType(CommandLine cl) {
        String version = cl.getOptionValue(VERSION_OPTION);    
        if(version==null){
            //Try to get it from config file
            version = Configuration.getInstance().getVersion();
        }else{
            Configuration.getInstance().setVersion(version);
        }
        if (getLanguages(cl).size() == 2) {
            if ("context_2d".equalsIgnoreCase(version)) {
                return AlignmentMatrixType.CONTEXT_2D;
            }
            
            return AlignmentMatrixType.TWO_LANG;
        }


        if (version == null) {
            return AlignmentMatrixType.MARGINAL;
        }

        if ("joint".equalsIgnoreCase(version)) {
            return AlignmentMatrixType.JOINT;
        }

        if ("marginal".equalsIgnoreCase(version)) {
            return AlignmentMatrixType.MARGINAL;
        }

        if ("context_marginal_3d".equalsIgnoreCase(version)) {
            return AlignmentMatrixType.CONTEXT_MARGINAL_3D;
        }
        

        // default to MARGINAL
        return AlignmentMatrixType.MARGINAL;
    }


    private static Long getSeed(CommandLine commandLine) {
        String seed = commandLine.getOptionValue(SEED);
        if (seed != null) {
            try {
                return Long.parseLong(seed);
            } catch (Exception e) {
                System.err.println("Command line parsing error, random seed is not in correct format: " + seed);
                System.exit(-1);
            }
        }

        if(Configuration.getInstance().isRandomSeedNull()){
            return null;
        }else{
            return Configuration.getInstance().getRandomSeed();
        }
    }



    private static double getInitialAnnealingTemp(CommandLine commandLine) {
        String initialAnnealingTemp = commandLine.getOptionValue(INITIAL_ANNEALING_TEMP);
        if (initialAnnealingTemp != null) {
            try {
                return Double.parseDouble(initialAnnealingTemp);
            } catch (Exception e) {
            }
        }
        if(Configuration.getInstance().getInitialAnnealingTemp() > 0)
            return Configuration.getInstance().getInitialAnnealingTemp();
        else
            return Constants.INITIAL_TEMPERATURE;
        
        //return Constants.INITIAL_TEMPERATURE;
    }

    private static double getAnnealingMultiplier(CommandLine commandLine) {
        String annealingMultiplier = commandLine.getOptionValue(ANNEALING_MULTIPLIER);
        if (annealingMultiplier != null) {
            try {
                return Double.parseDouble(annealingMultiplier);
            } catch (Exception e) {

            }
        }
        if(Configuration.getInstance().getAnnealingMultiplier() > 0){
            return Configuration.getInstance().getAnnealingMultiplier();
        }
        return Constants.TEMPERATURE_MULTIPLIER;

    }

    private static int getTreeRebuildingFrequency(CommandLine commandLine) {
        String treeRebuildingFrequency = commandLine.getOptionValue(TREE_REBUILDING_FREQUENCY);
        if (treeRebuildingFrequency != null) {
            try {
                return Integer.parseInt(treeRebuildingFrequency);
            } catch (Exception e) {
                System.err.println("Tree building frequency is not in correct format in command line arguments: " + treeRebuildingFrequency);
                System.exit(-1);
            }
        }
        
        if(Configuration.getInstance().getTreeRebuildingFrequency() > 0){
            return Configuration.getInstance().getTreeRebuildingFrequency();
        }
        return Constants.REBUILD_TREES_EVERY_NTH_ITERATION;
    }


    private static CostFunctionIdentifier getCostFunctionIdentifier(CommandLine cl) {
        String version = cl.getOptionValue(COST_FUNCTION);
        
        if ("eq1".equals(version) || "baseline".equals(version)) {
            return CostFunctionIdentifier.BASELINE;
        } else if ("eq2".equals(version) || "codebook-no-kinds".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_NO_KINDS;
        } else if ("eq16".equals(version) || "codebook-and-kinds".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE;
        } else if ("eq22".equals(version) || "written-out-nxn".equals(version)) {
            return CostFunctionIdentifier.WRITTEN_OUT_NXN;
        } else if ("prequential".equalsIgnoreCase(version)) {
            return CostFunctionIdentifier.PREQUENTIAL;            
        } else if ("nml".equalsIgnoreCase(version)) {
            return CostFunctionIdentifier.NML;
        } else if ("codebook-and-kinds-separate".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE;
        } else if ("codebook-and-kinds-separate-nml".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE_NML;
        } else if ("codebook-and-kinds-nml".equals(version)) {
            return CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML;
        }       
        if(null != Configuration.getInstance().getCostFunctionIdentifier()){
            return Configuration.getInstance().getCostFunctionIdentifier();
        }        
        return CostFunctionIdentifier.CODEBOOK_NO_KINDS;
    }

    private static ConversionRules getConversionRules(CommandLine commandLine) throws IOException {
        String conversionRuleFile = commandLine.getOptionValue(CONVERSION_RULES);
        if (conversionRuleFile == null) {
            if(null != Configuration.getInstance().getConversionRules()){
                return Configuration.getInstance().getConversionRules();
            }
            return null;
        }
        
        File f = new File(conversionRuleFile);
        if(!f.exists()) {
            System.err.println("Conversion rule file " + conversionRuleFile + " does not exist.");
            return null;
        }
        return new ConversionRules(f);
    }
    
    private static boolean getTrueFalseValue(String value) {       
        value = value.toLowerCase();
        if(value.equals("true") || value.equals("yes") || value.equals("on") || value.equals("+")){
            return true;
        }
        if(value.equals("false") || value.equals("no") || value.equals("off") || value.equals("-")){
            return false;
        }
        throw new InputMismatchException(value);
    }

    private static void handleTrueFalseOption(String identifier, String value, String line, Configuration config) {
        try{
            if(ConfigFileOptions.ANNEALING_OPTION.equals(identifier)){                
                config.setUseSimulatedAnnealing(getTrueFalseValue(value));
            }else if(ConfigFileOptions.PRINT_ONLY_FINAL_LOGS.equals(identifier)){
                config.setPrintOnlyFinalLogs(getTrueFalseValue(value));
            }else if(ConfigFileOptions.REVERSED_WORDS.equals(identifier)){
                config.setWordsFlippedAround(getTrueFalseValue(value));
            }else if(ConfigFileOptions.BASELINE_CONTEXT_COMBINATION.equals(identifier)){
                config.setDoFirstBaselineThenContext(getTrueFalseValue(value));
            }else if(ConfigFileOptions.ZERO_LEVEL_ON.equals(identifier)){
                config.setDoZeroDepthTricks(getTrueFalseValue(value));
            }else if(ConfigFileOptions.INFINITE_DEPTH_RESTRICTED.equals(identifier)){
                config.setInfiniteDepth(getTrueFalseValue(value));
            }else if(ConfigFileOptions.JOINT_CODING.equals(identifier)){
                config.setJointCoding(getTrueFalseValue(value));
            }else if(ConfigFileOptions.BINARY_VALUE_TREES.equals(identifier)){
                config.setBinaryValueTrees(getTrueFalseValue(value));
            }else if(ConfigFileOptions.NO_MULTIPLE_VALUE_TREES.equals(identifier)){
                config.setMultipleValueTreesOff(getTrueFalseValue(value));
            }else if(ConfigFileOptions.USE_PREVIOUS_CONTEXT_MODEL.equals(identifier)){
                config.setUsePreviousVersion(getTrueFalseValue(value));
            }else if(ConfigFileOptions.UTILIZE_WORD_BOUNDARIES.equals(identifier)){
                config.setTakeStartsAndEndsIntoAccount(getTrueFalseValue(value));
            }else if(ConfigFileOptions.IMPUTE.equals(identifier)){
                config.setImpute(getTrueFalseValue(value));
            }else if(ConfigFileOptions.REMOVE_SUFFIXES.equals(identifier)){
                config.setRemoveSuffixes(getTrueFalseValue(value));
            }else if(ConfigFileOptions.ALIGN_TWO_GLYPHS.equals(identifier)){
                if(getTrueFalseValue(value)){
                    config.setMaxGlyphsToAlign(2);
                }
            }else{
                System.err.println("Unknown: " + identifier + "\t" + value);
            }
        }catch(InputMismatchException e){
            System.err.println("Config file parse error, value for " + identifier + " must be either true/yes/on or false/no/off. found \"" + value + "\"");
            System.exit(-1);
        }
    }

    private static String getGoldStandardFilePath(CommandLine commandLine) {
        String filePath = commandLine.getOptionValue(GOLD_STANDARD);
        if(filePath != null){
            return filePath;            
        }
        return Configuration.getInstance().getGoldStandardFilePath();
    }

    private static String getDictionaryFileName(CommandLine commandLine) {
        String fileName = commandLine.getOptionValue(DICTIONARY_FILE_OPTION);
        if(null != fileName){            
            return fileName;
        }
        if(null != Configuration.getInstance().getDictionaryFileName()){
            return Configuration.getInstance().getDictionaryFileName();
        }        
        return Constants.DICTIONARY_FILE;
    }
}
