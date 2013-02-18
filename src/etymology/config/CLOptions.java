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
public interface CLOptions {

    // option keys
    public static final String VERSION_OPTION = "v";
    public static final String ANNEALING_OPTION = "a";
    public static final String LANGUAGES_OPTION = "l";
    public static final String INPUT_FILE_OPTION = "f";
    public static final String LOG_FILE_PATH = "lp";
    public static final String LOG_TO_CONSOLE = "lc";
    public static final String PRINT_ONLY_FINAL_LOGS = "of";
    public static final String ALIGN_TWO_GLYPHS = "g2";
    public static final String WORDS_TO_MONITOR = "monitor";
    public static final String INITIAL_ANNEALING_TEMP = "t";
    public static final String ANNEALING_MULTIPLIER = "alpha";
    public static final String COST_FUNCTION = "costfunction";
    public static final String TREE_REBUILDING_FREQUENCY = "tf";
    public static final String REVERSED_WORDS = "rev";
    public static final String REPETITION_COUNT = "rep";
    public static final String GOLD_STANDARD = "g";
    public static final String CONVERSION_RULES = "cr";
    public static final String BASELINE_CONTEXT_COMBINATION = "bc";
    public static final String ZERO_LEVEL_ON = "zero";
    public static final String INFINITE_DEPTH_RESTRICTED = "inf";
    public static final String JOINT_CODING = "joint";
    public static final String BINARY_VALUE_TREES = "binary";
    public static final String NO_MULTIPLE_VALUE_TREES = "nomulti";
    public static final String USE_PREVIOUS_CONTEXT_MODEL = "old";
    public static final String UTILIZE_WORD_BOUNDARIES = "boundaries";
    public static final String IMPUTE = "impute";
    public static final String REMOVE_SUFFIXES = "suffixes";
    public static final String ITERATION = "iteration";
    public static final String SEED = "seed";
    public static final String LOG_REGRET_MATRIX_FILE_OPTION = "logregret";
    public static final String CONFIG_FILE_OPTION = "config";
    public static final String DICTIONARY_FILE_OPTION = "dict";

}
