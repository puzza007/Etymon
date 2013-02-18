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

import etymology.align.AlignmentMatrixType;
import etymology.cost.CostFunctionIdentifier;
import etymology.cost.SuffixCostType;
import etymology.data.convert.ConversionRules;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;

/**
 *
 * @author avihavai
 */
public class Configuration {

    private static Configuration configuration;

    private Configuration() {
    }

    public static Configuration getInstance() {
        if (configuration == null) {
            configuration = new Configuration();
        }

        return configuration;
    }
    
    public static void clearConfigutation() {
        configuration = null;
    }
    
    private String inputFile;
    private String logRegretMatrixFileName;
    private String configFileName;
    private List<String> languages;
    private List<String> wordsToMonitor;
    private int maxGlyphsToAlign = 0;    
    private String logPath;
    private String dictionaryFileName;
    private boolean printOnlyFinalLogs;
    private boolean logToConsole;    
    private double initialAnnealingTemp = -1;
    private double annealingMultiplier = -0.5;
    private AlignmentMatrixType alignmentType;
    private CostFunctionIdentifier costFunctionIdentifier = CostFunctionIdentifier.CODEBOOK_NO_KINDS;
    private ConversionRules conversionRules;
    private SuffixCostType suffixCostType;
    private boolean randomSeedIsSet = false;


    private int rebuildTreesEveryNthIteration = -1;       
    private boolean useFeatures;
    private boolean useSimulatedAnnealing;
    private boolean doFirstBaselineThenContext = false;
    private boolean doZeroDepthTricks = false;
    private boolean restrictedRoot = false;
    private boolean flipWordsAround = false;
    private String version = null;
    // count kinds separately (also prequentially)
    private boolean isSeparateKinds = false;
    public boolean isSeparateKinds () { return isSeparateKinds;}
    
    private static String fileNameCommonPrefix = null;

    private String goldStandardFilePath;

    private int repetitionCount = -1;
    private Integer maxWordsToUse;

    private long randomSeed;
    private Random random;


    private boolean jointCoding = false;
    private boolean codeOneLevelOnly = false;
    private boolean binaryValueTrees = false;
    private boolean multipleValueTrees = true;

    private boolean usePreviousVersion = false;

    private Integer imputeWordAtIndex;
    private boolean impute;


    private boolean takeStartsAndEndsIntoAccount = false;
    private boolean removeSuffixes = false;

    private boolean codeCompleteWord = false;
    private int iterationNumber = -1;

    public String getDictionaryFileName() {
        return dictionaryFileName;
    }

    public void setDictionaryFileName(String dictionaryFileName) {        
        this.dictionaryFileName = dictionaryFileName;
    }

    
    public String getConfigFileName() {
        return configFileName;
    }

    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    
    public String getLogRegretMatrixFileName() {
        return logRegretMatrixFileName;
    }

    public void setLogRegretMatrixFileName(String logRegretMatrixFileName) {
        this.logRegretMatrixFileName = logRegretMatrixFileName;
    }
    
    public void setIsSeparateKinds(boolean isSeparateKinds) {
        this.isSeparateKinds = isSeparateKinds;
    }
    public void setCodeOneLevelOnly(boolean oneLevelOnly) {
        this.codeOneLevelOnly = oneLevelOnly;
    }
    public boolean isCodeOneLevelOnly() {
        return codeOneLevelOnly;
    }
    
    public void setCodeCompleteWordFirst(boolean codeCompleteWord) {
        this.codeCompleteWord = codeCompleteWord;
    }

    public boolean isCodeCompleteWordFirst() {
        return codeCompleteWord;
    }

    public void setIterationNumber(int iteration) {
        this.iterationNumber = iteration;
    }

    public int getIterationNumber() {
        return this.iterationNumber;
    }

    public SuffixCostType getSuffixCostType() {
        return this.suffixCostType;
    }

    public void setSuffixCostType(SuffixCostType sct) {
        this.suffixCostType = sct;
    }

    public void setRemoveSuffixes(boolean remove) {
        this.removeSuffixes = remove;
    }

    public boolean isRemoveSuffixes() {
        return removeSuffixes;
    }

    public void setImpute(boolean impute) {
        this.impute = impute;
    }

    public boolean isUseImputation() {
        return impute;
    }


    public void setTakeStartsAndEndsIntoAccount(boolean takeStartsAndEndsIntoAccount) {
        this.takeStartsAndEndsIntoAccount = takeStartsAndEndsIntoAccount;
    }

    public boolean isTakeStartsAndEndsIntoAccount() {
        return takeStartsAndEndsIntoAccount;
    }

    public void setUsePreviousVersion(boolean usePrevVersion) {
        usePreviousVersion = usePrevVersion;
    }

    public boolean isUsePreviousVersion() {
        return usePreviousVersion;
    }

    public void setJointCoding(boolean yesno) {
        jointCoding = yesno;
    }

    public boolean isJointCoding() {
        return jointCoding;
    }


    public void setBinaryValueTrees(boolean yesno) {
        binaryValueTrees = yesno;
    }

    public boolean isUseBinaryValueTrees() {
        return binaryValueTrees;
    }

    public void setMultipleValueTreesOff(boolean switchOff) {
        multipleValueTrees = !switchOff;
    }

    public boolean areMultipleValueTreesOn() {
        return multipleValueTrees;
    }

    public void setInfiniteDepth(boolean restrict) {
        this.restrictedRoot = restrict;
    }

    public boolean getInfiniteDepth() {
        return restrictedRoot;
    }

    public void setDoZeroDepthTricks(boolean doIt) {
        this.doZeroDepthTricks = doIt;
    }

    public boolean isZeroDepthTricks() {
        return doZeroDepthTricks;
    }


    public void setDoFirstBaselineThenContext(boolean doBoth) {
        this.doFirstBaselineThenContext = doBoth;
    }

    public boolean isFirstBaselineThenContext() {
        return doFirstBaselineThenContext;
    }

    public void setImputeWordAtIndex(Integer imputeWordAtIndex) {
        this.imputeWordAtIndex = imputeWordAtIndex;
    }

    public Integer getImputeWordAtIndex() {
        return imputeWordAtIndex;
    }

    private boolean fuzzUpFinnish = false;
    private double fuzzificationProb = 0.3;

    public boolean isFuzzUpFinnish() {
        return fuzzUpFinnish;
    }

    public void setFuzzUpFinnish(boolean fuzzUpFinnish) {
        this.fuzzUpFinnish = fuzzUpFinnish;
    }

    public double getFuzzificationProb() {
        return fuzzificationProb;
    }

    public void setFuzzificationProb(double fuzzificationProb) {
        this.fuzzificationProb = fuzzificationProb;
    }

    public boolean hasWordLimit() {
        return maxWordsToUse != null;
    }

    public int getMaxWordsToUse() {
        return maxWordsToUse;
    }

    public void setMaxWordsToUse(Integer maxWordsToUse) {
        this.maxWordsToUse = maxWordsToUse;
    }


    public int getRepetitionCount() {
        return repetitionCount;
    }

    public void setRepetitionCount(int repetitionCount) {
        this.repetitionCount = repetitionCount;
    }

    public void setGoldStandardFilePath(String goldStandardFilePath) {
        this.goldStandardFilePath = goldStandardFilePath;
    }

    public String getGoldStandardFilePath() {
        return goldStandardFilePath;
    }

    public void setTreeRebuildingFrequency(int freq) {
        this.rebuildTreesEveryNthIteration = freq;
    }
    
    public int getTreeRebuildingFrequency() {
        return this.rebuildTreesEveryNthIteration;
    }


    public void setConversionRules(ConversionRules conversionRules) {
        this.conversionRules = conversionRules;
    }

    public void setRandomSeed(Long randomSeed) {
        if (randomSeed == null) {
            this.randomSeed = new Random().nextInt();
        } else {
            this.randomSeed = randomSeed.longValue();
        }
        this.random = new Random(this.randomSeed);
        randomSeedIsSet = true;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public ConversionRules getConversionRules() {
        return conversionRules;
    }

    public void setWordsToMonitor(List<String> wordsToMonitor) {
        this.wordsToMonitor = wordsToMonitor;
        if(wordsToMonitor == null){
            return;
        }
        System.out.println("Flipped around: " + areWordsFlippedAround());
        if (areWordsFlippedAround()) {            
            this.wordsToMonitor = new ArrayList();
            for (int i=0; i<wordsToMonitor.size(); i++) {
                StringBuilder sb = new StringBuilder();
                sb.append(wordsToMonitor.get(i));
                sb.reverse();
                this.wordsToMonitor.add(sb.toString());
            }
        }
        
    }

    public List<String> getWordsToMonitor() {
        return wordsToMonitor;
    }

    public void setInitialAnnealingTemp(double initialAnnealingTemp) {
        this.initialAnnealingTemp = initialAnnealingTemp;
    }

    public double getInitialAnnealingTemp() {
        return initialAnnealingTemp;
    }

    public void setAnnealingMultiplier(double annealingMultiplier) {
        this.annealingMultiplier = annealingMultiplier;
    }

    public double getAnnealingMultiplier() {
        return this.annealingMultiplier;
    }

    public void setCostFunctionIdentifier(CostFunctionIdentifier costFunctionIdentifier) {
        this.costFunctionIdentifier = costFunctionIdentifier;
    }

    public CostFunctionIdentifier getCostFunctionIdentifier() {
        return costFunctionIdentifier;
    }

    public AlignmentMatrixType getAlignmentType() {
        if (alignmentType != null) {
            return alignmentType;
        }

        if (getLanguages() != null && getLanguages().size() == 2) {
            return AlignmentMatrixType.TWO_LANG;
        }

        return AlignmentMatrixType.MARGINAL;
    }

    public void setAlignmentType(AlignmentMatrixType alignmentType) {
        this.alignmentType = alignmentType;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setMaxGlyphsToAlign(int maxGlyphsToAlign) {
        this.maxGlyphsToAlign = maxGlyphsToAlign;
    }

    public void setUseSimulatedAnnealing(boolean useSimulatedAnnealing) {
        this.useSimulatedAnnealing = useSimulatedAnnealing;
    }

    public String getLogPath() {
        if (logPath == null) {
            logPath = "log";
        }

        if (!logPath.endsWith("/")) {
            logPath += "/";
        }

        return logPath;
    }

    public int getMaxGlyphsToAlign() {
        return maxGlyphsToAlign;
    }

    public String getLogFilePath() {
        if (isUseFeatures() || isFirstBaselineThenContext()) {
            return getCommonPrefixOfContextBasedOutputFileNames() + getLogFilename();
        }
        
        String alignmentType = "";
        String functionIdentifier = "";
        switch (getAlignmentType()){
                case TWO_LANG:
                    alignmentType = "2D";
                    break;
                case MARGINAL:
                    alignmentType = "Marginal";
                    break;
                case JOINT:
                    alignmentType = "joint";
                case CONTEXT_MARGINAL_3D:
                    alignmentType = "context-marginal-3D";
                    break;
                case CONTEXT_2D:
                    alignmentType = "context-2D";
                    break;

        }   
        
        switch (getCostFunctionIdentifier()){
                case CODEBOOK_WITH_KINDS_SEPARATE:
                    functionIdentifier = "CB+kinds-Cond+kinds";
                    break;
                case CODEBOOK_WITH_KINDS_NOT_SEPARATE:
                    functionIdentifier = "CB+kinds";
                    break;
                case CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML:
                    functionIdentifier = "CB+kinds-NML";
                    break;
                case CODEBOOK_WITH_KINDS_SEPARATE_NML:
                    functionIdentifier = "CB+kinds-Cond+kinds-NML";
                    break;
                case BASELINE:
                    functionIdentifier = "baseline";
                    break;
                case CODEBOOK_NO_KINDS:
                    functionIdentifier = "CB-Nokinds";
                    break;
                case NML:
                    functionIdentifier = "NML";
                    break;
                case PREQUENTIAL:
                    functionIdentifier = "prequential";
                    break;
                case WRITTEN_OUT_NXN:
                    functionIdentifier = "written-out-nxn";
                    break;
        }            
        
        String lp = getLogPath() + alignmentType + "-" + functionIdentifier + "-" + new File(getInputFile()).getName() + "-" +getLogFileNamePrefix() + getLogFilename();
        //System.out.println("Just asked for logpath, it is: " + lp);
        return lp;
    }

    public String getCommonPrefixOfContextBasedOutputFileNames() {
        //!!! This does not work for pipeline model anymore
//        if (fileNameCommonPrefix != null) {
//            return fileNameCommonPrefix;
//        }

        fileNameCommonPrefix = getLogPath();
        if (isFirstBaselineThenContext()) {
            fileNameCommonPrefix += "baseline-first-";
        }
        fileNameCommonPrefix += getAlignmentType().toString().toLowerCase() + 
                "-" + getCostFunctionIdentifier().toString().toLowerCase() + 
                "-" + new File(getInputFile()).getName() + "-" + getLogFileNamePrefix();
        
        return fileNameCommonPrefix;
    }

    public String getLogFileNamePrefix() {
        StringBuilder sb = new StringBuilder();
        for (String lang : languages) {
            sb.append(lang).append("-");
        }

        if (getMaxGlyphsToAlign() > 1) {
            sb.append("nxn-");
        } else if (!isFirstBaselineThenContext() && !isUseFeatures()) {
            sb.append("1x1-");
        }
        if (isTakeStartsAndEndsIntoAccount()) {
            sb.append("boundaries-");
        }

        if (isJointCoding()) {
            sb.append("joint-");
        }

        if (isUseSimulatedAnnealing()) {
            sb.append("simann-");
            sb.append(annealingMultiplier);
            sb.append("-");

            sb.append(initialAnnealingTemp);
            sb.append("-");
        }

        if (areWordsFlippedAround()) {
            sb.append("backwards-");
        }

        if (getInfiniteDepth()) {
            sb.append("inf-");
        }
        
        if (isZeroDepthTricks()) {
            sb.append("zero-");
        }
        
        if (isUseBinaryValueTrees() && areMultipleValueTreesOn()) {
            sb.append("multi-binary-");
        }
        
        if (isUseBinaryValueTrees() && !areMultipleValueTreesOn()) {
            sb.append("binary-");
        }


        if(hasWordLimit()) {
            sb.append("max-words-").append(getMaxWordsToUse()).append("-");
        }

        if(isFuzzUpFinnish()) {
            sb.append("fuzz-").append(Constants.TWO_DECIMALS.format(getFuzzificationProb())).append("-");
        }

        if (getIterationNumber() != 0) {
            sb.append(this.iterationNumber);
            sb.append("-");

        }


        return sb.toString().toLowerCase();
    }

    public String getLogFilename() {
        StringBuilder sb = new StringBuilder();
        sb.append("data.log");
        return sb.toString().toLowerCase();
    }

    public boolean isPrintOnlyFinalLogs() {
        return printOnlyFinalLogs;
    }

    public void setPrintOnlyFinalLogs(boolean printOnlyFinalLogs) {
        this.printOnlyFinalLogs = printOnlyFinalLogs;
    }

    public boolean isUseSimulatedAnnealing() {
        return useSimulatedAnnealing;
    }

    public boolean isLogToConsole() {
        return logToConsole;
    }

    public List<String> getLanguages() {
        if(languages == null) {
            languages = new ArrayList();
        }

        return languages;
    }

    public boolean isUseFeatures() {
//        if (getLanguages() != null && getLanguages().size() > 2) {
//            return false; // override feature usage in the case where we have more than 2 langs
//        }

        return useFeatures || getAlignmentType().equals(AlignmentMatrixType.CONTEXT_2D) || getAlignmentType().equals(AlignmentMatrixType.CONTEXT_MARGINAL_3D);
    }

    public void setUseFeatures(boolean useFeatures) {
        this.useFeatures = useFeatures;
    }

    public static Random getRnd() {
        return getInstance().getRandom();
    }
    
    public Random getRandom() {
        if (random == null) {
            throw new RuntimeException("Random seed has not been given, unable to initiate random.");
        }

        return random;
    }

    public void setWordsFlippedAround(boolean flip) {        
        this.flipWordsAround = flip;
        if (wordsToMonitor != null && flip) {            
            List currentWordsToMonitor = wordsToMonitor;
            this.wordsToMonitor = new ArrayList();            
            for (int i=0; i<currentWordsToMonitor.size(); i++) {
                StringBuilder sb = new StringBuilder();                
                sb.append(currentWordsToMonitor.get(i));
                sb.reverse();                
                this.wordsToMonitor.add(sb.toString());
            }
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    

    public boolean areWordsFlippedAround() {
        return this.flipWordsAround;
    }

    boolean isRandomSeedNull() {
        return !randomSeedIsSet;
    }

    

}
