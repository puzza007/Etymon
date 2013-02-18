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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 *
 * @author arto
 */
public class Constants {

    // temperatures for simulated annealing
    public static double INITIAL_TEMPERATURE = 50.0;
    public static double TEMPERATURE_MULTIPLIER = 0.99;
    public static final double STOP_AT_TEMPERATURE = 0.00001;

    public static final int DISABLE_SIMANN_AFTER_NUM_OF_NO_CHANGES = 10;
    public static final int DISABLE_SIMANN_AFTER_NUM_OF_NO_COST_CHANGES = 25;


    public static final int LOG_EVERY_NTH_WORD_ALIGNMENT = 10;
    public static int REBUILD_TREES_EVERY_NTH_ITERATION = 0;

    public static final String UNKNOWN = "*";
    
    public static final int DOT_INDEX = 0;
    public static final int WORD_BOUNDARY_INDEX  = 1;
    public static final int SUFFIX_BOUNDARY_INDEX = 2;

    public static final int BEST_PATHS_OF_CONTEXT_IMPUTATION = 100;

    //public static final String DATA_PATH = "/home/sxhiltun/Desktop/oneWord.utf8"; 
    public static final String DATA_PATH = "/home/lv/starling-top-dialects.utf8"; 
    public static final String LOG_REGRET_MATRIX_FILE = "**Log regret matrix file**";    


    public static final String FINAL_LOGGER_NAME = "final";
    public static final String COSTS_LOGGER_NAME = "costs";
    public static final String BEST_COST_LOGGER_NAME = "best";
    public static final String GRAPH_NAME = "graph.bmg";

    public static final DecimalFormat DIST_FORMAT = new DecimalFormat("#.#####", new DecimalFormatSymbols(Locale.US));
    public static final DecimalFormat AVG_FORMAT = new DecimalFormat("#0.######", new DecimalFormatSymbols(Locale.US));
    public static final DecimalFormat COST_FORMAT = new DecimalFormat("#####0.##", new DecimalFormatSymbols(Locale.US));
    public static final DecimalFormat TWO_DECIMALS = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
    static String DICTIONARY_FILE = "";
}
