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

import etymology.config.Configuration;
import etymology.config.Constants;
import etymology.config.CLOptions;
import java.io.File;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author avihavai
 */
public class LoggerInitiator extends Constants implements CLOptions {

    private static final Formatter SIMPLE_FORMATTER = new SimpleLogFormatter();

    public static void initLoggers(Configuration config) throws Exception {
        String logFile = config.getLogFilePath();
        if (!config.isPrintOnlyFinalLogs()) {
            initDefaultLogger(logFile, config);
        }

        if (config.getIterationNumber() == 0) {
            initFinalLogger(logFile + ".final");
        }
        
        initCostLogger(logFile + ".costs");

        if (!config.isUseFeatures() && !config.isFirstBaselineThenContext()) {
            if (config.getIterationNumber() == 0) {
                initBestAlignmentLogger(logFile + ".best");
            }
        }
    }
    
    public static void initImputationLogger(String logFileName) throws Exception {
        initFinalLogger(logFileName);
    }

    private static void initDefaultLogger(String logFile, Configuration config) throws Exception {
        initSimpleLogger(Logger.GLOBAL_LOGGER_NAME, logFile);

        if (config.isLogToConsole() && Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).getHandlers().length == 1) {
            // log to console
            ConsoleHandler ch = new ConsoleHandler();
            ch.setLevel(Level.ALL);
            ch.setFormatter(SIMPLE_FORMATTER);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).addHandler(ch);
        }
    }

    private static void initBestAlignmentLogger(String logFile) throws Exception {
        initSimpleLogger(Constants.BEST_COST_LOGGER_NAME, logFile);
    }

    private static void initFinalLogger(String logFile) throws Exception {
        initSimpleLogger(Constants.FINAL_LOGGER_NAME, logFile);
    }

    private static void initCostLogger(String logFile) throws Exception {
        initSimpleLogger(Constants.COSTS_LOGGER_NAME, logFile);
    }

    private static void initSimpleLogger(String loggerIdentifier, String logFile) throws Exception {
        Handler fh = new FileHandler(logFile);
        fh.setFormatter(SIMPLE_FORMATTER);
        fh.setLevel(Level.ALL);

        Handler[] oldHandlers = Logger.getLogger(loggerIdentifier).getHandlers();
        for(Handler h: oldHandlers) {
            Logger.getLogger(loggerIdentifier).removeHandler(h);
        }

        Logger.getLogger(loggerIdentifier).addHandler(fh);
        Logger.getLogger(loggerIdentifier).setLevel(Level.ALL);
    }
}
