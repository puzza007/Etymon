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

/**
 *
 * @author arto
 */
public interface ConfigFileOptions {

    // option keys
    public static final String VERSION_OPTION = "version\"\"";
    public static final String ANNEALING_OPTION = "UseSimulatedAnnealing?";
    public static final String LANGUAGES_OPTION = "languages[]";
    public static final String INPUT_FILE_OPTION = "UTF8InputFile/";
    public static final String LOG_FILE_PATH = "LogDirectory/";
    //public static final String LOG_TO_CONSOLE = "LogToConsole?";
    public static final String PRINT_ONLY_FINAL_LOGS = "PrintOnlyFinalLogs?";
    public static final String ALIGN_TWO_GLYPHS = "AlignTwoGlyphs?";
    public static final String WORDS_TO_MONITOR = "WordsToBeMonitored";
    public static final String INITIAL_ANNEALING_TEMP = "SimulatedAnnealingInitialTemp";
    public static final String ANNEALING_MULTIPLIER = "SimulatedAnnealingCoolingSchedule";
    public static final String COST_FUNCTION = "CostFunction\"\"";
    public static final String TREE_REBUILDING_FREQUENCY = "TreeBuildingFrequency";
    public static final String REVERSED_WORDS = "ReversedWords?";
    public static final String REPETITION_COUNT = "RepetitionCount";
    public static final String GOLD_STANDARD = "GoldStandardFilePath/";
    public static final String CONVERSION_RULES = "ConversionRules/";
    public static final String BASELINE_CONTEXT_COMBINATION = "BaselineContextCombination?";
    public static final String ZERO_LEVEL_ON = "UseZeroLevel?";
    public static final String INFINITE_DEPTH_RESTRICTED = "InfDepthRestricted?";
    public static final String JOINT_CODING = "UseJointCoding?";
    public static final String BINARY_VALUE_TREES = "UseBinaryCandidates?";
    public static final String NO_MULTIPLE_VALUE_TREES = "NoMultipleValueTrees?";
    public static final String USE_PREVIOUS_CONTEXT_MODEL = "UsePreviousContextModel?";
    public static final String UTILIZE_WORD_BOUNDARIES = "UseWordBoundaries?";
    public static final String IMPUTE = "ImputeWords?";
    public static final String REMOVE_SUFFIXES = "RemoveSuffixes?";
    public static final String ITERATION = "Iteration";
    public static final String SEED = "RandomSeed";
    public static final String LOG_REGRET_MATRIX_FILE_OPTION = "LogRegretMatrixFile/";   
    public static final String DICTIONARY_FILE = "FeatureDictionaryFile/";   

}
