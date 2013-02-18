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
package etymology.logging;

import etymology.align.Alignator;
import etymology.align.AlignmentMatrix;
import etymology.align.AlignmentRegistry;
import etymology.align.WordAlignment;
import etymology.config.Configuration;
import etymology.config.Constants;
import etymology.context.FeatureAlignmentMatrix;
import etymology.context.FeatureTree;
import etymology.cost.CostHandler;
import etymology.input.GlyphVocabulary;
import etymology.input.Input;
import etymology.output.AlignmentPrinter;
import etymology.viterbi.ViterbiMatrix;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 *
 * @author avihavai
 */
public class StaticLogger {
    
    //class variables
    private static Logger LOG;
    private static Logger FINAL_LOGGER;
    private static Logger COST_LOGGER;
    private static Logger BEST_LOGGER;

    public static void init() {

        if (Configuration.getInstance().getIterationNumber() == 0) {
            LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            FINAL_LOGGER = Logger.getLogger(Constants.FINAL_LOGGER_NAME);
            BEST_LOGGER = Logger.getLogger(Constants.BEST_COST_LOGGER_NAME);
        }
        
        COST_LOGGER = Logger.getLogger(Constants.COSTS_LOGGER_NAME);

    }

    public static void closeIfOpen() {
        List<Logger> loggers = Arrays.asList(LOG, FINAL_LOGGER, COST_LOGGER, BEST_LOGGER);
        
        //check if there exists any logger, if there is, flush the cache and close
        for(Logger logger: loggers) {
            if(logger == null) {
                continue;
            }
            
            for(Handler h: logger.getHandlers()) {
                h.flush();
                h.close();
            }
        }
    }

    public static void printStatistics(Logger log, Input input) {
        StringBuilder sb = new StringBuilder();
        for (int langId = 0; langId < input.getNumOfLanguages(); langId++) {
            GlyphVocabulary gv = input.getVocabulary(langId);
            sb.append("Lang: ").append(gv.getLanguage()).append("\n");
            sb.append("\tWords: ").append(gv.getTotalWords()).append("\n");
            sb.append("\tUnique Glyphs: ").append(input.getLengthOneGlyphCount(langId));
            sb.append("\n");
        }

        sb.append("Total word \"pairs\": ").append(input.getNumOfWords()).append("\n");
        sb.append("Total symbol count (total word length): ").append(input.getTotalWordLength()).append("\n");
        sb.append("\n");

        sb.append(getWordsToAlign(input));

        log.fine(sb.toString());
    }

    public static String getWordsToAlign(Input input) {
        if(input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for (int langId = 0; langId < input.getNumOfLanguages(); langId++) {
            sb.append(input.getVocabulary(langId).getLanguage()).append("\t");
        }

        sb.append("\n");

        for (int wordId = 0; wordId < input.getNumOfWords(); wordId++) {
            for (int langId = 0; langId < input.getNumOfLanguages(); langId++) {
                String word = input.getWord(langId, wordId);

                if(word == null) {
                    sb.append("-");
                } else {
                    sb.append(word);
                }

                sb.append("\t");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    public static void logAlignment(Input input, AlignmentRegistry alignmentRegistry, int wordIndex, ViterbiMatrix vm, boolean printViterbiMatrix, boolean vmIsNotNull) {

        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }
        
        if (vmIsNotNull && (printViterbiMatrix || wordIndex % 1 == 0 || input.getNumOfWords() < 20)) {
            LOG.fine(vm.toString());
        }
        
        LOG.fine(alignmentRegistry.getAlignment(wordIndex).getStringPresentation(input, wordIndex));
    }

    public static void logFinalOutput() {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }
        FINAL_LOGGER.fine(getFinalOutput());
    }

    public static void logLatestOutputToBestAlignmentLog() {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }
        BEST_LOGGER.fine(getFinalOutput());
    }

    public static void logWordAlignmentsToBestAlignmentLog(WordAlignment[] wordAlignments, Input input) {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }
        BEST_LOGGER.fine("");
        for(WordAlignment al: wordAlignments) {
            BEST_LOGGER.fine(al.getStringPresentation(input));
        }
    }

    public static void logToCostsLog(String content) {
        COST_LOGGER.fine("");        
        COST_LOGGER.fine(content);
    }

    private static String getFinalOutput() {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return null;
        }

        Input input = Alignator.getInstance().getInput();
        CostHandler costHandler = Alignator.getCostHandler();
        double cost = costHandler.getGlobalCost(); // Alignator.getInstance().getCost();
        boolean disableBasicLogging = Alignator.getInstance().isDisableBasicLogging();
        AlignmentRegistry alignmentRegistry = Alignator.getInstance().getAlignmentStorage();
        FeatureAlignmentMatrix featureAlignmentMatrix = Alignator.getInstance().getFeatureAlignmentMatrix();
        AlignmentMatrix alignmentMatrix = Alignator.getInstance().getAlignmentMatrix();


        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.getNumOfLanguages(); i++) {
            sb.append(input.getVocabulary(i).getLanguage());
            sb.append(" ");
        }
        sb.append("- ");
        sb.append(costHandler.getCostString()).append("\n");


        double averagedCost = cost / input.getTotalWordLength();

        sb.append("AVG: ").append(Constants.AVG_FORMAT.format(averagedCost)).append(" :: TOTAL GLYPHS ").append(input.getTotalGlyphs()).append("\n");

        if (disableBasicLogging) {
            sb.append("  -----\n");
            return sb.toString();
        }

        AlignmentPrinter ap = new AlignmentPrinter(alignmentMatrix, input);

        sb.append(ap.getPrintableAlignmentMatrix(alignmentMatrix)).append("\n");
        sb.append("***MODELSTART***\n");
        sb.append(ap.getPrintableAlignments(alignmentRegistry)).append("\n");
        sb.append("***MODELEND***\n");
        sb.append("  -----\n");

        if (Configuration.getInstance().isUseFeatures()) {

            for (FeatureTree tree : featureAlignmentMatrix.getTrees()) {
                sb.append(tree);
            }
        }

        return sb.toString();
    }
}
