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

import etymology.input.Tuple;
import etymology.util.DataCompressor;
import etymology.util.EtyMath;
import etymology.util.StringUtils;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author avihavai
 */
public class Compressor {
    // private static String logFolder = "/home/group/langtech/Etymology-Project/etymon-logs/codebook-no-kinds/";

    private static String logFolder = "/home/avihavai/NetBeansProjects/EtyMalign/log/";
    

    private static DataCompressor.CompressionMethod SELECTED_COMPRESSION_METHOD = DataCompressor.CompressionMethod.BZIP2;

    public static void main(String[] args) throws Exception {
        for (DataCompressor.CompressionMethod c : DataCompressor.CompressionMethod.values()) {
            SELECTED_COMPRESSION_METHOD = c;
            System.out.println("");
            System.out.println("Selected compression method: " + c);
            // compressAndPrintDataTable();

            Map<Integer, Integer> sizes = getCompressedSizesInBytes();
            for(int key: sizes.keySet()) {
                System.out.println(key + "\t" + sizes.get(key) + "\t" + (sizes.get(key)*8));
            }
        }
    }



    private static Map<Integer, Integer> getCompressedSizesInBytes() throws Exception {
        Collection<File> files = getFiles();
        Map<Integer, Integer> sizes = new TreeMap();

        DataCompressor dc = new DataCompressor(SELECTED_COMPRESSION_METHOD);
        for (File f : files) {
            String data = getLanguageDataWithNoHeaders(f);
            data = getLanguageDataSplitToRows(data, 0);
            sizes.put(getMaxWords(f.getName()), dc.compress(data).length);
        }
        
        return sizes;
    }

    private static void compressAndPrintDataTable() throws Exception {
        System.out.println("COMPRESSING USING " + SELECTED_COMPRESSION_METHOD);
        Collection<File> files = getFiles();
        Map<Tuple, Double> distances = new HashMap();

        for (File f : files) {
            // System.out.println(f.getName());
            String data = getLanguageDataWithNoHeaders(f);
            distances.put(getLanguageTuple(f), compressAndGetDistance(data));
        }

        Map<String, Map<String, Double>> simpleMap = convertToSimpleMap(distances);
        String table = LxLMatrixPrinter.getMapAsTable(simpleMap.keySet(), simpleMap, new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US)));

        System.out.println(table);
    }

    private static Map<String, Map<String, Double>> convertToSimpleMap(Map<Tuple, Double> distances) {
        Map<String, Map<String, Double>> data = new TreeMap();

        for (Tuple<String, String> t : distances.keySet()) {
            if (!data.containsKey(t.getFirst())) {
                data.put(t.getFirst(), new TreeMap());
            }

            if (!data.containsKey(t.getSecond())) {
                data.put(t.getSecond(), new TreeMap());
            }

            data.get(t.getFirst()).put(t.getSecond(), distances.get(t));
            data.get(t.getSecond()).put(t.getFirst(), distances.get(t));
        }

        return data;
    }

    private static Tuple<String, String> getLanguageTuple(File file) {
        String name = file.getName();
        name = name.substring(name.indexOf(".utf8-") + 6, name.indexOf("-1x1"));
        String[] langs = name.split("-");

        return new Tuple<String, String>(langs[0], langs[1]);
    }

    private static double compressAndGetDistance(String data) throws Exception {
        String l1Tol1Data = getLanguageDataSplitToRows(data, 0);
        String l2Tol2Data = getLanguageDataSplitToRows(data, 1);
        String l1Tol2Data = getLanguageDataSplitToRows(data, -1);

        DataCompressor compressor = new DataCompressor(SELECTED_COMPRESSION_METHOD);

        int l1l1InBytes = compressor.compress(l1Tol1Data).length;
        int l2l2InBytes = compressor.compress(l2Tol2Data).length;
        int l1l2InBytes = compressor.compress(l1Tol2Data).length;

        double distance = EtyMath.getNormalizedCompressionDistance(l1l2InBytes, l1l1InBytes, l2l2InBytes);
        return distance;
    }


    private static List<File> getFiles() {
        return getFiles(-1);
    }

    private static List<File> getFiles(int maxNumOfWords) {
        List<File> files = new ArrayList();

        for (File f : new File(logFolder).listFiles()) {
            String filename = f.getName();

            if (!filename.endsWith(".final")) {
                continue;
            }

            if (!filename.contains("1x1")) {
                continue;
            }

            if (!filename.contains("simann")) {
                continue;
            }

            if (filename.contains("marginal")) {
                continue;
            }

            if (f.length() == 0) {
                continue;
            }

            if (maxNumOfWords >= 0 && !filename.contains("max-words-" + maxNumOfWords)) {
                continue;
            }

            files.add(f);
        }

        Collections.sort(files, new MaxNumOfWordsComparator());

        return files;
    }

    private static String getLanguageDataSplitToRows(String data) {
        return getLanguageDataSplitToRows(data, -1);
    }

    private static String getLanguageDataSplitToRows(String data, int useOnlyColumn) {
        StringBuilder sb = new StringBuilder();
        for (String line : data.split("\n")) {
            String[] cols = line.split("\\s+");
            for (String col : cols) {
                if (useOnlyColumn >= 0) {
                    col = cols[useOnlyColumn];
                }

                sb.append(col).append("\n");
                if (useOnlyColumn >= 0) {
                    break;
                }
            }
        }

        return sb.toString().trim() + "\n";
    }

    private static String getLanguageDataWithNoHeaders(File file) {
        String data = getLanguageData(file);
        data = data.substring(data.indexOf("\n") + 1, data.length());
        return data;
    }

    private static String getLanguageData(File file) {
        if (!file.getName().endsWith(".final")) {
            throw new IllegalArgumentException("Sorry, I'm only reading final files!");
        }

        StringBuilder sb = new StringBuilder();
        boolean start = false;
        for (String line : StringUtils.tryReadFileAsString(file).split("\n")) {
            if (line.trim().isEmpty()) {
                if (start == false) {
                    start = true;
                } else {
                    break;
                }

                continue;
            }

            if (!start) {
                continue;
            }

            sb.append(line).append("\n");
        }

        return sb.toString();
    }


    protected static int getMaxWordsStartIndex(String name) {
        return name.indexOf("max-words-") + 10;
    }

    protected static int getMaxWordsEndIndex(String name) {
        return name.indexOf("-", getMaxWordsStartIndex(name) + 1);
    }

    public static int getMaxWords(String name) {
        if(!name.contains("max-words")) {
            return -1;
        }

        return Integer.parseInt(name.substring(getMaxWordsStartIndex(name), getMaxWordsEndIndex(name)));
    }
}

class MaxNumOfWordsComparator implements Comparator<File> {

    public int compare(File o1, File o2) {
        String f1Name = o1.getName();
        String f2Name = o2.getName();

        // if there is no notion of max num of words, just compare the filenames
        if (!f1Name.contains("max-words") || !f2Name.contains("max-words")) {
            return f1Name.compareTo(f2Name);
        }

        int f1Words = Compressor.getMaxWords(f1Name);
        int f2Words = Compressor.getMaxWords(f2Name);

        return new Integer(f1Words).compareTo(f2Words);
    }
}
