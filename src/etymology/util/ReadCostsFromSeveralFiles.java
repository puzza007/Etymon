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

import etymology.util.Compressor;
import etymology.util.StringUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import etymology.config.Constants;

/**
 *
 * @author avihavai
 */
public class ReadCostsFromSeveralFiles {
    // private static final String COST_LINE_INDICATOR = "DISTANCE COMPARISON DATA -- sm-sm COST ";
    // private static final String COST_LINE_INDICATOR = "Min: ";
    private static final String COST_LINE_INDICATOR = "Mean: ";


    private static final String LOG_PATH = "/home/avihavai/192-random-splits/"; // /home/avihavai/NetBeansProjects/EtyMalign/log/";

    // private static final String LOG_PATH = "/cs/group/home/langtech/Etymology-Project/etymon-logs/fin-est-subset-comparison/";

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Map<Integer, Double> costMap;

        costMap  = getCostMap(new String[]{"1x1", ".costs", "-simann-"}, new String[]{".lck"});

        for(int key: costMap.keySet()) {
            double bits = costMap.get(key);
            double bytes = bits / 8;
            System.out.println(key + "\t" +
                    Constants.COST_FORMAT.format(bytes) + "\t" +
                    Constants.COST_FORMAT.format(bits));
        }

//        costMap  = getCostMap(new String[]{"1x1", "simann", ".final"}, new String[]{".lck"});
//        System.out.println("1x1 SIMULATED ANNEALING");
//        for(String key: costMap.keySet()) {
//            System.out.println(key + ": " + costMap.get(key));
//        }
//
//        costMap  = getCostMap(new String[]{"nxn", ".final"}, new String[]{".lck", "simann"});
//        System.out.println("NxN NO ANNEALING");
//        for(String key: costMap.keySet()) {
//            System.out.println(key + ": " + costMap.get(key));
//        }
//
//        costMap  = getCostMap(new String[]{"nxn", "simann", ".final"}, new String[]{".lck"});
//        System.out.println("NxN SIMULATED ANNEALING");
//        for(String key: costMap.keySet()) {
//            System.out.println(key + ": " + costMap.get(key));
//        }
//

        System.exit(0);

    }

    private static Map<Integer, Double> getCostMap(String[] filenameMustContain, String[] filenameMustNotContain) throws FileNotFoundException, IOException {
        Map<Integer, Double> sizeToCost = new TreeMap();
        
        for(File file: getFiles(filenameMustContain, filenameMustNotContain)) {
            String data = StringUtils.readFileAsString(file);

            Scanner sc = new Scanner(data);
            while(sc.hasNextLine()) {
                String line = sc.nextLine();
                if(!line.contains(COST_LINE_INDICATOR)) {
                    continue;
                }

                sizeToCost.put(Compressor.getMaxWords(file.getName()), getFinalCost(line));
            }
        }

        return sizeToCost;
    }

    private static Map<String, Double> handleCostLines(String langOne, String langTwo, List<String> costLines) {
        Map<String, Double> costs = new HashMap();
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double real = -1;

        String langPair = langOne + "-" + langTwo;

        for(String line: costLines) {
            String firstLang = line.split(" ")[0];
            String secondLang = line.split(" ")[1];

            if(firstLang.equals(secondLang)) {
                double cost = getFinalCost(line);

                if(min > cost) {
                    min = cost;
                }

                if(max < cost) {
                    max = cost;
                }
            } else {
                real = getFinalCost(line);
            }
        }

        double cost = ((real - max) / min);
        costs.put(langPair, cost);
        return costs;
    }

    private static List<String> getCostLines(File f) throws FileNotFoundException {
        List<String> costLines = new ArrayList();
        Scanner sc = new Scanner(f);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (!line.contains(COST_LINE_INDICATOR)) {
                continue;
            }


            costLines.add(line);
        }

        return costLines;
    }

    private static double getFinalCost(String line) {
        return Double.parseDouble(line.substring(COST_LINE_INDICATOR.length()).trim());
    }

    private static List<File> getFiles(String[] filenameMustContain, String[] filenameMustNotContain) {
        List<File> files = new ArrayList();

        IterateFiles:
        for (File file : new File(LOG_PATH).listFiles()) {
            String filename = file.getName();
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


            System.out.println("Adding " + file.getName());
            files.add(file);
        }

        return files;
    }
}
