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

import etymology.config.Constants;
import etymology.output.LxLMatrixBuilder;
import etymology.util.StringUtils;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author avihavai
 */
public class LxLMatrixPrinter {
    private static final int MIN_WORDS = 50;
    private static final int MIN_CODEBOOK_COST = Integer.MIN_VALUE;
    private static final int MIN_CONDITIONAL_COST = Integer.MIN_VALUE;

    private static final boolean CHOOSE_MINIMUM_OVER_DIAGONAL = true;

    enum VALUE_TO_PRINT {
        LANG_DISTANCE, NUM_OF_WORDS;
    }

    private static final VALUE_TO_PRINT CHOSEN_VALUE = VALUE_TO_PRINT.LANG_DISTANCE; // .NUM_OF_WORDS;


    public static void main(String[] args) throws Exception {
        // "/fs-0/b/avihavai/EtyMalign/baseline/";
        String logFolder = "/fs-0/b/avihavai/EtyMalign/codebook-no-kinds/";

        logFolder = "/home/group/langtech/Etymology-Project/etymon-logs/codebook-no-kinds";
        logFolder = "/cs/group/home/langtech/Etymology-Project/etymon-logs/nxn-boundaries";
        // logFolder = "/home/avihavai/NetBeansProjects/EtyMalign/log/";

        // String twoLangLogFolder = "/group/home/langtech/Etymology-Project/etymon-logs/2-lang-twopartcode-eq2";
        printTwoLangAlignmentsFromLogFolder(logFolder);

        // String threeLangLogFolder = "/fs-0/b/avihavai/EtyMalign/logs";
        // printThreeLangAlignmentsFromLogFolder(threeLangLogFolder);
    }

    private static void printTwoLangAlignmentsFromLogFolder(String logFolder) throws Exception {
        LxLMatrixBuilder m = new LxLMatrixBuilder(logFolder, LxLMatrixBuilder.CostIdentifier.MEAN);

//        System.out.println("1 X 1 -- GREEDY");
//        System.out.println(getTwoLangPrintableTable("1x1-no-simann", m));
//        System.out.println("");
//        System.out.println("");
//        System.out.println("1 X 1 -- SIMULATED ANNEALING");
//
//        System.out.println(getTwoLangPrintableTable("1x1-simann", m));

//        System.out.println("");
//        System.out.println("");
//        System.out.println("NxN");
//        System.out.println("");
////
        System.out.println("N X N -- BOUNDARIES");
        System.out.println(getTwoLangPrintableTable("nxn-boundaries", m));
////        System.out.println("");
////        System.out.println("");
//         System.out.println("N X N -- SIMULATED ANNEALING");
//         System.out.println(getTwoLangPrintableTable("nxn-simann", m));
    }


    private static void printThreeLangAlignmentsFromLogFolder(String logFolder) throws Exception {
        LxLMatrixBuilder m = new LxLMatrixBuilder(logFolder, LxLMatrixBuilder.CostIdentifier.MIN);
        System.out.println("1 X 1 -- GREEDY");
        System.out.println(getThreeLangPrintableTables("1x1-no-simann", m));
        System.out.println("");
        System.out.println("");
        System.out.println("1 X 1 -- SIMULATED ANNEALING");
        System.out.println(getThreeLangPrintableTables("1x1-simann", m));
    }
    
    public static String getMapAsTable(Collection<String> languages, Map<String, Map<String, Double>> map, DecimalFormat format) {
        StringBuilder sb = new StringBuilder();

        int singleCellLength = 8;

        // print header
        sb.append(StringUtils.leftAlign(singleCellLength, ""));
        for (String language : languages) {
            sb.append(StringUtils.leftAlign(singleCellLength, language));
        }
        sb.append("\n");

        for (String rowLang : map.keySet()) {
            sb.append(StringUtils.leftAlign(singleCellLength, rowLang));
            for (String headerLang : languages) {

                if (headerLang.equals(rowLang)) {
                    sb.append(StringUtils.leftAlign(singleCellLength, "0.0001"));
                } else {
                    double val = map.get(rowLang).get(headerLang);
                    if(CHOOSE_MINIMUM_OVER_DIAGONAL) {
                        if (val > map.get(headerLang).get(rowLang)) {
                            val = map.get(headerLang).get(rowLang);
                        }
                    }

                    sb.append(StringUtils.leftAlign(singleCellLength, format.format(val)));
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private static String getTwoLangPrintableTable(String category, LxLMatrixBuilder m) {
        Map<String, Map<String, Double>> data = new TreeMap();

        for (String langOne : m.getLanguages()) {
            for (String langTwo : m.getLanguages()) {
                if (!data.containsKey(langOne)) {
                    data.put(langOne, new TreeMap());
                }

                String langKey = m.getKey(langOne, langTwo);

                double value = getWantedValue(m, category, langKey);
                if (m.getCodebookCost(category, langKey) < MIN_CODEBOOK_COST) {
                    value = -1;
                }

                if (m.getConditionalCost(category, langKey) < MIN_CONDITIONAL_COST) {
                    value = -1;
                }

                if (m.getTotalWordPairs(category, langKey) < MIN_WORDS) {
                    value = -1;
                }
                
                data.get(langOne).put(langTwo, value);
            }
        }

        return getMapAsTable(m.getLanguages(), data, getWantedFormatter());
    }

    private static double getWantedValue(LxLMatrixBuilder m, String cat, String langKey) {
        switch(CHOSEN_VALUE) {
            case NUM_OF_WORDS:
                return (1.0 / m.getTotalWordPairs(cat, langKey));
            case LANG_DISTANCE:
            default:
                return m.getLangDistance(cat, langKey);
        }
    }

    private static DecimalFormat getWantedFormatter() {
         return Constants.DIST_FORMAT;
    }

    private static String getThreeLangPrintableTables(String cat, LxLMatrixBuilder m) {
        

        StringBuilder tables = new StringBuilder();

        for (String langOne : m.getLanguages()) {
            Map<String, Map<String, Double>> langsToAvg = new TreeMap();
            for (String langTwo : m.getLanguages()) {
                String key = m.getKey(langOne, langTwo);
                if (!langsToAvg.containsKey(key)) {
                    langsToAvg.put(key, new TreeMap());
                }

                for (String langThree : m.getLanguages()) {
                    String langKey = m.getKey(langOne, langTwo, langThree);
                    Double avg = m.getAverageCost(cat, langKey);
                    
                    if (m.getCodebookCost(cat, langKey) < MIN_CODEBOOK_COST) {
                        avg = -1.0;
                    }

                    if (m.getConditionalCost(cat, langKey) < MIN_CONDITIONAL_COST) {
                        avg = -1.0;
                    }

                    if (m.getTotalWordPairs(cat, langKey) < MIN_WORDS * 2) {
                        avg = -1.0;
                    }

                    langsToAvg.get(key).put(langThree, avg);
                }
            }

            tables.append(getMapAsTable(m.getLanguages(), langsToAvg, Constants.AVG_FORMAT));
            tables.append("\n\n\n\n");
        }

        return tables.toString();
    }
}
