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
import etymology.config.Configuration;
import etymology.cost.CostFunctionIdentifier;
import etymology.data.convert.ConversionRules;
import etymology.input.Input;
import etymology.logging.LoggerInitiator;
import etymology.logging.StaticLogger;
import etymology.output.GnuPlotPrinter;
import etymology.viterbi.ViterbiMatrix;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author lv
 */
public class LOOLogScoreGenerator {
    
    /**
     * Final variables
     */
    private static final String INPUT_FILE = "/home/group/langtech/Etymology-Project/StarLing/uralet/starling-input-data/starling-10-top2-dialects.utf8";
    private static final String[] langs= {"FIN", "EST"};
    
    /**
     * private variables
     */
    private static Configuration config;
    private static Input input;
    
    
    
    public static void main(String[] args) throws IOException, Exception {
        trainModel();
    }
    
    public static void trainModel() throws IOException, Exception {
        initConfiguration(Arrays.asList(langs));
        input = new Input(config);
        
        StaticLogger.closeIfOpen();

        // initiate loggers
        LoggerInitiator.initLoggers(Configuration.getInstance());

        // this needs to be initiated after the loggers have been created -- forces loggers to be loaded after they are known
        StaticLogger.init();
        GnuPlotPrinter.logPath = Configuration.getInstance().getLogPath();
        
        align();
        
    }
    
    private static void initConfiguration(List<String> languages) throws IOException {
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
        
    }
    
    public static void align() throws Exception {
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
            
            //calculate LOO score
            
            
            System.out.println(vm.toString());
        }
        
    }
    
}
