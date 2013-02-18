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

package etymology.output;

import etymology.util.EtyMath;
import etymology.util.StringUtils;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author avihavai
 */
public class LxLMatrixBuilder implements MatrixConsts {
    public enum CostIdentifier {
        MEAN("Mean: "), MIN("Min: ");

        private CostIdentifier(String id) {
            this.identifier = id;
        }

        String identifier;
    }

    private CostIdentifier costIdentifier;
    private String[] alignTypes = {"1x1", "nxn"};
    private boolean debug = false;
    private Integer maxWords;

    private String[] typeCategories = {"1x1-simann",
        "1x1-no-simann",
        "nxn-simann",
        "nxn-no-simann",
        "nxn-boundaries"};

    // eh.
    private Map<String, Map<String, CostHolder>> categoryToLangComboToCostHolder = new TreeMap();
    private Set<String> languages = new TreeSet();

    public LxLMatrixBuilder(final String fromFolder, final CostIdentifier costIdentifier) throws Exception {
        this(fromFolder, costIdentifier, null);
    }

    public LxLMatrixBuilder(final String fromFolder, final CostIdentifier costIdentifier, Integer maxWords) throws Exception {
        this.costIdentifier = costIdentifier;
        this.maxWords = maxWords;
        
        init(fromFolder);
    }

    public List<String> getCategories() {
        Set<String> categories = new TreeSet();
        for(String category: categoryToLangComboToCostHolder.keySet()) {
            categories.add(category);
        }
        
        return new ArrayList(categories);
    }

    public String getKey(String langOne, String langTwo) {
        return langOne + "-" + langTwo;
    }


    public String getKey(String langOne, String langTwo, String langThree) {
        return langOne + "-" + langTwo + "-" + langThree;
    }


    public String getKey(List<String> langs) {
        String key = langs.get(0);
        for(int i = 1; i < langs.size(); i++) {
            key += "-" + langs.get(i);
        }
        
        return key;
    }

    public double getLangDistance(String category, String langKey) {
        try {
            return categoryToLangComboToCostHolder.get(category).get(langKey).getLanguageDistance();
        } catch (Exception e) {
        }
        System.out.println(category + ": " + langKey + " not found.");
        return -1;
    }

    public double getAverageCost(String category, String langKey) {
        try {
            return categoryToLangComboToCostHolder.get(category).get(langKey).getAverageCost();
        } catch (Exception e) {
        }
        System.out.println(category + ": " + langKey + " not found.");
        return -1;
    }

    public double getConditionalCost(String category, String langKey) {
        try {
            return categoryToLangComboToCostHolder.get(category).get(langKey).getConditionalCost();
        } catch (Exception e) {
        }
        System.out.println(category + ": " + langKey + " not found.");
        return -1;
    }

    public double getCodebookCost(String category, String langKey) {
        try {
            return categoryToLangComboToCostHolder.get(category).get(langKey).getCodebookCost();
        } catch (Exception e) {
        }
        System.out.println(category + ": " + langKey + " not found.");
        return -1;
    }

    public int getTotalWordPairs(String category, String langKey) {
        try {
            return categoryToLangComboToCostHolder.get(category).get(langKey).getTotalWordPairs();
        } catch (Exception e) {
        }
        System.out.println(category + ": " + langKey + " not found.");
        return -1;
    }

    public int getTotalSymbolCount(String category, String langKey) {
        try {
            return categoryToLangComboToCostHolder.get(category).get(langKey).getTotalSymbolCount();
        } catch (Exception e) {
        }
        System.out.println(category + ": " + langKey + " not found.");
        return -1;
    }

    public Collection<String> getLanguages() {
        return languages;
    }

    private void init(String fromFolder) throws Exception {
        Collection<File> costFiles = getCostFiles(fromFolder);

        Map<String, Collection<File>> categorizedFiles = categorizeCostFilesBasedOnType(costFiles);
        for(String category: categorizedFiles.keySet()) {
            readDataFromCategory(category, categorizedFiles.get(category));
        }
    }

    private void readDataFromCategory(String category, Collection<File> files) throws Exception {
        System.out.println("Total " + files.size() + " files in category \"" + category + "\"");
        if(files.isEmpty()) {
            return;
        }

        int fileCount = 0;

        for(File file: files) {
            if(!file.getName().endsWith(FINAL_FILE_SUFFIX)) {
                continue;
            }

            String costFileName = file.getAbsolutePath();
            costFileName = costFileName.replace(FINAL_FILE_SUFFIX, COST_FILE_SUFFIX);


            Map<String, String> data;

            if(new File(costFileName).exists()) {
                data = getData(file, new File(costFileName));
            } else {
                data = getData(file, null);
            }

            List<String> langs = getLangs(file);
            languages.addAll(langs);
            
            String langKey = getKey(langs);
            addData(category, langKey, data);
            
            fileCount++;
            if(debug && fileCount >= 100) {
                break;
            }
        }
    }


    private void addData(String category, String langKey, Map<String, String> data) {
        if(!categoryToLangComboToCostHolder.containsKey(category)) {
            categoryToLangComboToCostHolder.put(category, new HashMap());
        }

        if(langKey.contains("utf8")) {
            langKey = langKey.substring(langKey.indexOf("utf8") + 5);
        }

        if(langKey.length() > 7) {
            return;
        }

        CostHolder ch = new CostHolder(langKey.split("-"), data);
        categoryToLangComboToCostHolder.get(category).put(langKey, ch);
    }

    private Map<String, String> getData(File finalFile, File costsFile) throws Exception {
        Scanner reader = new Scanner(finalFile);
        Map<String, String> data = new HashMap();
        String languageKey = null;

        while(reader.hasNextLine()) {
            String line = reader.nextLine();

            if(line.contains(DISTANCE_COMPARISON_DATA)) {
                line = line.substring(line.indexOf(DISTANCE_COMPARISON_DATA) + DISTANCE_COMPARISON_DATA.length()).trim();
                String langs = line.split(" ")[0];
                String cost = line.split(" ")[2];
                // System.out.println(langs + " -> " + cost);

                String[] langArr = langs.split("-");
                if(!langArr[0].equals(langArr[1])) {
                    languageKey = langs;
                }

                data.put(langs, cost);
            }

            if(line.contains(AVERAGE_COST) && !data.containsKey(AVERAGE_COST)) {
                line = line.substring(line.indexOf(AVERAGE_COST) + AVERAGE_COST.length()).trim();
                data.put(AVERAGE_COST, line.split("\\s+")[0]);
            }

            if(line.contains(CODEBOOK_COST) && !data.containsKey(CODEBOOK_COST)) {
                line = line.substring(line.indexOf(CODEBOOK_COST) + CODEBOOK_COST.length()).trim();
                data.put(CODEBOOK_COST, line.split("\\s+")[0]);
            }

            if(line.contains(CONDITIONAL_COST) && !data.containsKey(CONDITIONAL_COST)) {
                line = line.substring(line.indexOf(CONDITIONAL_COST) + CONDITIONAL_COST.length()).trim();
                data.put(CONDITIONAL_COST, line.split("\\s+")[0]);
            }

            if (line.contains(TOTAL_WORD_PAIRS) && !data.containsKey(TOTAL_WORD_PAIRS)) {
                line = line.substring(line.indexOf(TOTAL_WORD_PAIRS) + TOTAL_WORD_PAIRS.length()).trim();
                data.put(TOTAL_WORD_PAIRS, line.split("\\s+")[0]);
            }

            if (line.contains(TOTAL_SYMBOL_COUNT) && !data.containsKey(TOTAL_SYMBOL_COUNT)) {
                line = line.substring(line.indexOf(TOTAL_SYMBOL_COUNT) + TOTAL_SYMBOL_COUNT.length()).trim();
                data.put(TOTAL_SYMBOL_COUNT, line.split("\\s+")[0]);
            }
        }

        reader.close();

        if(costsFile == null || languageKey == null) {
            return data;
        }


        // System.out.println("Retrieving " + costIdentifier.identifier);

        reader = new Scanner(costsFile);
        while(reader.hasNextLine()) {
            String line = reader.nextLine();
            if(line.startsWith(costIdentifier.identifier)) {
                String retrievedData = line.substring(costIdentifier.identifier.length()).trim();
                // System.out.println("" + languageKey + " mean " + mean);
                data.put(languageKey, retrievedData);
            }
        }
        return data;
    }

    private List<String> getLangs(File file) {
        String filename = file.getName();
        for(String tag: alignTypes) {
            if (filename.contains(tag)) {
                filename = filename.substring(0, filename.indexOf(tag)-1);
            }
        }

        if(filename.contains(".utf8")) {
            filename = filename.substring(filename.indexOf(".utf8")+6);
        }

        return Arrays.asList(filename.split("-"));
    }

    private Map<String, Collection<File>> categorizeCostFilesBasedOnType(Collection<File> costFiles) {
        Map<String, Collection<File>> categorizedFiles = new HashMap();
        for(String fileCategory: typeCategories) {
            categorizedFiles.put(fileCategory, new ArrayList());
        }

        for(File costFile: costFiles) {
            String fileName = costFile.getName();

            String category;
            if(fileName.contains("1x1")) {
                category = "1x1";
            } else {
                category = "nxn";
            }

            if (fileName.contains("simann")) {
                category += "-simann";
            } else {
                category += "-no-simann";
            }

            if(fileName.contains("nxn-boundaries")) {
                category = "nxn-boundaries";
            }

            categorizedFiles.get(category).add(costFile);
        }


        return categorizedFiles;
    }



    private List<File> getCostFiles(String folder) throws Exception {
        List<File> files = new ArrayList();
        for (File f : new File(folder).listFiles()) {
            if(!f.getName().endsWith(FINAL_FILE_SUFFIX)) {
                continue;
            }

            if(maxWords != null) {
                if(!f.getName().contains("max-words-" + maxWords)) {
                    continue;
                }
            }

            if(f.length() <= 0) {
                continue;
            }

            if(getLangs(f).size() > 2) {
                continue;
            }

            files.add(f);
        }

        return files;
    }

    private String getWithBlanks(DecimalFormat df, double value, int totalLength) {
        return getWithBlanks(df.format(value), totalLength);
    }

    private String getWithBlanks(String s, int totalLength) {
        for (int i = s.length(); i < totalLength; i++) {
            s = " " + s;
        }

        return s;
    }
}


class CostHolder implements MatrixConsts {
    List<String> languages;
    Map<String, String> data;

    public CostHolder(String[] langs, Map<String, String> data) {
        this.languages = Arrays.asList(langs);
        this.data = data;
    }

    public double getLanguageDistance() {
        try {
            String l1 = (languages.get(0) + "-" + languages.get(0)).toUpperCase();
            String l2 = (languages.get(1) + "-" + languages.get(1)).toUpperCase();
            String mean = (languages.get(0) + "-" + languages.get(1)).toUpperCase();


//            System.out.println("l1: " + l1);
//            System.out.println("l2: " + l2);
//            System.out.println("mean: " + mean);

            String l1Cost = data.get(l1);
            String l2Cost = data.get(l2);
            String meanCost = data.get(mean);

//            System.out.println("l1Cost: " + l1Cost);
//            System.out.println("l2Cost: " + l2Cost);
//            System.out.println("meanCost: " + meanCost);

            return EtyMath.getNormalizedCompressionDistance(Double.parseDouble(meanCost), Double.parseDouble(l1Cost), Double.parseDouble(l2Cost));
        } catch (Exception e) {
            // e.printStackTrace();
        }

        return -1;
    }

    public double getAverageCost() {
        try {
            return Double.parseDouble(data.get(AVERAGE_COST));
        } catch (Exception e) {
        }

        return -1;
        
    }

    public double getConditionalCost() {
        try {
            return Double.parseDouble(data.get(CONDITIONAL_COST));
        } catch (Exception e) {
        }

        return -1;
    }

    public double getCodebookCost() {
        try {
            return Double.parseDouble(data.get(CODEBOOK_COST));
        } catch (Exception e) {
        }

        return -1;
    }

    public int getTotalWordPairs() {
        try {
            return Integer.parseInt(data.get(TOTAL_WORD_PAIRS));
        } catch (Exception e) {
        }

        return -1;
    }

    public int getTotalSymbolCount() {
        try {
            return Integer.parseInt(data.get(TOTAL_SYMBOL_COUNT));
        } catch (Exception e) {
        }

        return -1;
    }
}

interface MatrixConsts {
    public static final String FINAL_FILE_SUFFIX = ".final";
    public static final String COST_FILE_SUFFIX = ".costs";
    public static final String AVERAGE_COST = "AVG: ";
    public static final String CONDITIONAL_COST = "(COND)";
    public static final String CODEBOOK_COST = "(BOOK)";
    public static final String TOTAL_WORD_PAIRS = "Total word \"pairs\": ";
    public static final String TOTAL_SYMBOL_COUNT = "Total symbol count (total word length): ";
    public static final String DISTANCE_COMPARISON_DATA = "DISTANCE COMPARISON DATA -- ";
}