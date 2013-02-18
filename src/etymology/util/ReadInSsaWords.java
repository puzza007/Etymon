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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 *
 * @author sxhiltun
 */
public class ReadInSsaWords {

    private static String outputPath = "/home/sxhiltun/sxhiltun/ssa-fin-est-compression-data/";
    private static String inputPath = "/home/group/langtech/Etymology-Project/etymon-logs/zip-comparison/fin-est-comparison-data/";
    private static String logName = "/two_lang-codebook_no_kinds-ssa.utf8-sm-vi-1x1-simann-0.99-max-words-xxx-data.log.final";

    private static String oldComparisonFile = "/home/group/langtech/Etymology-Project/etymon-logs/zip-comparison/fin-est-ssa-comparison.txt";
    private static String[] costFiles = new String[]{        
        "/fs-2/a/sxhiltun/ssa-fin-est-compression-data/log/two_lang-codebook_with_kinds-ssa-fin-est-xxx-fin-est-1x1-simann-0.99-init_temp-50.0-data.log.costs",
        "/fs-2/a/sxhiltun/ssa-fin-est-compression-data/log/two_lang-codebook_with_kinds-ssa-fin-est-xxx-fin-est-nxn-boundaries-simann-0.99-init_temp-50.0-data.log.costs",
        "/fs-2/a/sxhiltun/ssa-fin-est-compression-data/log/context_based-ssa-fin-est-xxx-fin-est-simann-0.99-init_temp-50.0-zero-multi-binary-data.log.costs"
    };

    private static final String COST_LINE_INDICATOR = "Min: ";

    //"/fs-2/a/sxhiltun/ssa-fin-est-compression-data/log/two_lang-codebook_no_kinds-ssa-fin-est-xxx-fin-est-1x1-simann-0.99-init_temp-50.0-data.log.costs",


    private static File getFiles(String logDir, String logName, String iter) {

        IterateFiles:
        for (File file : new File(logDir).listFiles()) {
            String filename = file.getName();
            String todoFileName = logName.replace("xxx", iter);
            if (todoFileName.equals(filename)) {
                return file;
            }

        }

        return null;
    }

    private static List<String[]> getWordPairs(File f, String[] catchTheseFirst) throws FileNotFoundException {

        List<String[]> wordPairs = new ArrayList<String[]>();

        Scanner sc = new Scanner(f);
        boolean found = false;

        NextLine:
        while (sc.hasNextLine()) {
            String line = sc.nextLine();

            if (found && line.isEmpty()) {
                break;
            }

            if (found) {
                wordPairs.add(line.split("\t"));
                continue;
            }


            if (line.split("\t").length != 2) {
                continue NextLine;
            }
            if (!line.split("\t")[0].trim().equals(catchTheseFirst[0]) &&
                    !line.split("\t")[1].trim().equals(catchTheseFirst[1])) {
                continue NextLine;
            }
            
            
            found = true;
        }
        return wordPairs;
    }

    public static void writeToFile(String filesize, List<String[]> wordPairs) throws IOException {
        String filename = outputPath + "ssa-fin-est-" + filesize;
        File file = new File(filename);
        FileWriter fw = new FileWriter(file);

        fw.write("FIN\tEST\n\n");
        for (String[] pair: wordPairs) {
            fw.write(pair[0].trim());
            fw.write("\t");
            fw.write(pair[1].trim());
            fw.write("\n");
        }

        fw.close();
    }

    public static void writeSsaFiles() throws FileNotFoundException, IOException {
        for (int maxWords = 100; maxWords <= 3500; maxWords += 100) {
            String max = String.valueOf(maxWords);
            List<String> mustContain  = new ArrayList<String>();
            mustContain.add(max);
            mustContain.add(".final");
            File file = getFiles(inputPath, logName, max);
            List<String[]> wordPairs = getWordPairs(file, new String[]{"sm", "vi"});
            writeToFile(max, wordPairs);


        }

    }


    private static void readSsaCostFiles() throws FileNotFoundException, IOException {
        
        List<String> oldLines = new ArrayList<String>();
        Map<String, Double> codebookCosts = new TreeMap<String, Double>();
        Map<String, Double> boundariesCosts = new TreeMap<String, Double>();
        Map<String, Double> contextCosts = new TreeMap<String, Double>();



        //read in the existing file
        Scanner sc = new Scanner(new File(oldComparisonFile));
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            oldLines.add(line);
        }


        for (int numOfWords = 100; numOfWords <= 3200; numOfWords += 100) {
            String wordNum = String.valueOf(numOfWords);
            String costLine;
            double cost;

            String codebookwithkindsCostFile = costFiles[0].replace("xxx", wordNum);
            costLine = readLine(new File(codebookwithkindsCostFile));
            cost = getCostFromString(costLine, COST_LINE_INDICATOR);
            codebookCosts.put(wordNum, cost);
            
            String boundariesCostFile = costFiles[1].replace("xxx", wordNum);
            costLine = readLine(new File(boundariesCostFile));
            cost = getCostFromString(costLine, COST_LINE_INDICATOR);
            boundariesCosts.put(wordNum, cost);


            String contextCostFile = costFiles[2].replace("xxx", wordNum);
            costLine = readLine(new File(contextCostFile));
            cost = getCostFromString(costLine, COST_LINE_INDICATOR);
            contextCosts.put(wordNum, cost);


        }
        DecimalFormat twoPlaces = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        for (int linenum = 0; linenum < oldLines.size(); linenum++) {
            String oldline = oldLines.get(linenum);
            
            if (oldline.startsWith("#")) {
                //print topic
                System.out.print(oldline.trim());
                System.out.print("\t");
                if (linenum == 0) {
                    System.out.print("codebook-with-kinds\t\t");
                    System.out.print("2x2-boundaries\t\t");
                    System.out.println("context-0");
                } else {
                    System.out.print("BYTES\t");
                    System.out.print("BITS\t");
                    System.out.print("BYTES\t");
                    System.out.print("BITS\t");
                    System.out.print("BYTES\t");
                    System.out.print("BITS\t");
                    System.out.println("");
                }
            } else {
                String datasize = oldline.split("\\s+")[0].trim();

                System.out.print(oldline.trim());
                System.out.print("\t");
                System.out.print(twoPlaces.format(codebookCosts.get(datasize)/8));
                System.out.print("\t");
                System.out.print(twoPlaces.format(codebookCosts.get(datasize)));

                System.out.print("\t");
                System.out.print(twoPlaces.format(boundariesCosts.get(datasize)/8));
                System.out.print("\t");
                System.out.print(twoPlaces.format(boundariesCosts.get(datasize)));

                System.out.print("\t");
                System.out.print(twoPlaces.format(contextCosts.get(datasize)/8));
                System.out.print("\t");
                System.out.print(twoPlaces.format(contextCosts.get(datasize)));

                System.out.println("");
                if (datasize.equals("3200")) {
                    break;
                }
            }

        }

    }
    
    private static double getCostFromString(String line, String costLineIndicator) {

        try {            
            double cost = Double.parseDouble(line.substring(costLineIndicator.length()).trim());
            return cost;

        } catch(Exception e) {
            System.out.println("line: " + line);
            System.out.println("costName: " + costLineIndicator);
            System.exit(-1);
        }
        return -1;

    }

    public static String readLine(File file) throws FileNotFoundException {

        String costLine = null;
        
        Scanner sc = new Scanner(file);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (!line.contains(COST_LINE_INDICATOR)) {
                continue;
            }
            return line;
        }
        return costLine;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        //writeSsaFiles();
        readSsaCostFiles();

    }

}
