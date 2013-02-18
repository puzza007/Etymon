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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author avihavai
 */
public class Clusterer {
     private static File distFile = new File("/cs/group/home/langtech/Etymology-Project/etymon-logs/language-distances/distance-codebook-no-kinds.txt.test");
    // private static File distFile = new File("/cs/group/home/langtech/Etymology-Project/etymon-logs/language-distances/distance-num-of-words.txt");
    // private static File distFile = new File("/cs/group/home/langtech/Etymology-Project/etymon-logs/language-distances/distance-gzip.txt");
    // private static File distFile = new File("/cs/group/home/langtech/Etymology-Project/etymon-logs/language-distances/distance-bzip2.txt");

    //private static File distFile = new File("/cs/group/home/langtech/Etymology-Project/etymon-logs/language-distances/distance-simann-no-kinds-192-random-mean-over-100-runs-correct-ncd.txt"); //
    private static File outputFile = new File(distFile.getAbsolutePath() + ".bmg");
    private static final String COLOR_DODGER_BLUE = "30/144/255";
    private static final String COLOR_LEMON_CHIFFON = "255/250/205";
    private static final String COLOR_FIREBRICK = "178/34/34";
    private static final String LEAF_NODE_COLOR = COLOR_DODGER_BLUE;
    private static final String INNER_NODE_COLOR = COLOR_LEMON_CHIFFON;
    private static final String ROOT_NODE_COLOR = COLOR_FIREBRICK;
    private static boolean PRINT_NAMES_TO_INNER_AND_ROOT_NODES = false;
    private static Set<String> usedLangs = new HashSet();

    public static void main(String[] args) {
        File folder = new File("/cs/group/home/langtech/Etymology-Project/etymon-logs/language-distances/");
        //File folder = new File("/cs/group/home/langtech/Etymology-Project/etymon-logs/language-distances/cleaned-dialects/");
//        for (File file : folder.listFiles()) {
//            handleFile(file);
//        }
//
        handleFile(new File(folder, "context-separate-zero-11-11-25.txt"));
        //handleFile(new File(folder, "codebook-no-kinds-simann.txt"));
    }

    private static void handleFile(File file) {
        if (!file.getName().endsWith("txt")) {
            return;
        }

        usedLangs = new HashSet();
        distFile = file;
        outputFile = new File(distFile.getAbsolutePath() + ".bmg");
        getAndPrintDistances();

        // getAndPrintUPGMADistances();
    }

    private static void getAndPrintUPGMADistances() {
        Map<Tuple, Double> distances = getDistances();

        int iters = 5;
        while (true) {
            Tuple<String, String> smallestDistanceTuple = findSmallestDistanceTuple(distances);
            if (smallestDistanceTuple == null) {
                break;
            }

            System.out.println("Smallest distance tuple: " + smallestDistanceTuple);
            distances.remove(smallestDistanceTuple);

            double distance = distances.get(smallestDistanceTuple);


            iters--;
            if (iters <= 0) {
                break;
            }
        }
    }

    private static void getAndPrintDistances() {
        Map<Tuple, Double> distances = getDistances();

        List<Tuple> tuplesToRemove = new ArrayList();
        
        //remove tuple if cost = 0.0
        for (Tuple t : distances.keySet()) {
            if (distances.get(t).equals(0.0)) {
                tuplesToRemove.add(t);
            }
        }

        for (Tuple t : tuplesToRemove) {
            distances.remove(t);
        }

        Collection<String> languages = getLangs(distances.keySet());
        List<Tuple> connectingNodes = new ArrayList();


        while (true) {
            Tuple<String, String> smallestDistanceTuple = findSmallestDistanceTuple(distances);
            if (smallestDistanceTuple == null) {
                break;
            }

            connectingNodes.add(smallestDistanceTuple);
            System.out.println("New connecting node: " + smallestDistanceTuple + ": " + distances.get(smallestDistanceTuple));

            distances.remove(smallestDistanceTuple);
            combineData(distances, smallestDistanceTuple);

            usedLangs.add(smallestDistanceTuple.getFirst());
            usedLangs.add(smallestDistanceTuple.getSecond());

            System.out.println("");
        }


        try {
            printBmgData(languages, connectingNodes);
        } catch (IOException ex) {
            Logger.getLogger(Clusterer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void printBmgData(Collection<String> languages, Collection<Tuple> internalNodes) throws IOException {
        FileWriter fw = new FileWriter(outputFile);
        System.out.println("Printing to " + outputFile.getAbsolutePath());

        Map<String, Integer> nodeNameToNodeId = new TreeMap();

        int nodeId = 0;
        for (String language : new HashSet<String>(languages)) {
            language = language.toUpperCase();
            nodeNameToNodeId.put(language, nodeId);
            fw.write("# _attributes " + nodeId + "_N PrimaryName=" + language + " label=" + language + " fill=" + LEAF_NODE_COLOR + "\n");
            nodeId++;
        }

        List<Tuple> internalNodeList = new ArrayList(internalNodes);

        for (Tuple<String, String> internalNode : internalNodeList.subList(0, internalNodeList.size() - 1)) {
            printNode(internalNode, fw, nodeId, nodeNameToNodeId, INNER_NODE_COLOR);
            nodeId++;
        }

        printNode(internalNodeList.get(internalNodeList.size() - 1), fw, nodeId, nodeNameToNodeId, ROOT_NODE_COLOR);

        fw.flush();
        fw.close();
    }

    private static void printNode(Tuple<String, String> internalNode, FileWriter fw, int nodeId, Map<String, Integer> nodeNameToNodeId, String nodeColor) throws IOException {
        String internalNodeString = internalNode.toString().trim().replaceAll("\\s+", "").toUpperCase();
        String[] connectedNodes = internalNodeString.split(",");
        String leftNode = connectedNodes[0].substring(1);
        String rightNode = connectedNodes[1].substring(0, connectedNodes[1].length() - 1);
        String nodeLabel = "I";
        if (PRINT_NAMES_TO_INNER_AND_ROOT_NODES) {
            nodeLabel = internalNodeString;
        }

        fw.write("# _attributes " + nodeId + "_N PrimaryName=" + nodeLabel + " label=" + nodeLabel + " fill=" + nodeColor + "\n");
        fw.write(nodeNameToNodeId.get(leftNode) + "_N " + nodeId + "_N\n");
        fw.write(nodeNameToNodeId.get(rightNode) + "_N " + nodeId + "_N\n");

        nodeNameToNodeId.put(internalNodeString.replace(",", "-"), nodeId);
    }

    private static void combineData(Map<Tuple, Double> distances, Tuple<String, String> tupleToCombine) {
        if (tupleToCombine == null) {
            return;
        }

        System.out.println("Combining " + tupleToCombine);

        String l1 = tupleToCombine.getFirst();
        String l2 = tupleToCombine.getSecond();

        Tuple t1Rem = getTuple(distances.keySet(), l1, l2);
        distances.remove(t1Rem);
        Tuple t2Rem = getTuple(distances.keySet(), l2, l1);
        distances.remove(t2Rem);

        if (usedLangs.contains(l1) && usedLangs.contains(l2)) {
            return;
        }

        Collection<String> langs = getLangs(distances.keySet());

        for (String lang : langs) {
            if (l1.equals(lang) || l2.equals(lang)) {
                continue;
            }

            addAveragesToCreatedTuple(distances,
                    getTuple(distances.keySet(), lang, l1),
                    getTuple(distances.keySet(), lang, l2),
                    new Tuple(lang, "(" + l1 + "-" + l2 + ")"));

            addAveragesToCreatedTuple(distances,
                    getTuple(distances.keySet(), l1, lang),
                    getTuple(distances.keySet(), l2, lang),
                    new Tuple("(" + l1 + "-" + l2 + ")", lang));
        }
    }

    private static void addAveragesToCreatedTuple(Map<Tuple, Double> distances, Tuple t1, Tuple t2, Tuple createdTuple) {
        if (t1 == null || t2 == null) {
            return;
        }

        double d1 = distances.get(t1);
        double d2 = distances.get(t2);
        double dAvg = (d1 + d2) / 2;
        
        distances.put(createdTuple, dAvg);
        distances.remove(t1);
        distances.remove(t2);
    }

    private static Tuple getTuple(Collection<Tuple> tuples, String l1, String l2) {
        for (Tuple<String, String> t : tuples) {
            if (t.getFirst().equals(l1) && t.getSecond().equals(l2)) {
                return t;
            }
        }

        // System.out.println("Not found:" + l1 + ", " + l2);
        return null;
    }

    private static Collection<String> getLangs(Collection<Tuple> tuples) {
        Set<String> langs = new TreeSet();
        for (Tuple<String, String> t : tuples) {
            langs.add(t.getFirst());
            langs.add(t.getSecond());
        }

        return langs;
    }

    private static Tuple findSmallestDistanceTuple(Map<Tuple, Double> distances) {
        Tuple smallest = null;
        double smallestDistance = Double.MAX_VALUE;

        for (Tuple<String, String> t : distances.keySet()) {
            System.out.println(t + " " + distances.get(t));
            if (distances.get(t) < smallestDistance) {
                smallest = t;
                smallestDistance = distances.get(smallest);
            }
        }

        return smallest;
    }

    private static Map<Tuple, Double> getDistances() {
        Scanner sc;
        try {
            sc = new Scanner(distFile);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Clusterer.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        sc.nextLine(); // skip header

        String langString = sc.nextLine().trim();
        List<String> langs = Arrays.asList(langString.split("\\s+"));

        Map<Tuple, Double> data = new HashMap();

        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            String[] elements = line.split("\\s+");

            String rowLang = elements[0];
            for (int elementIdx = 1; elementIdx < elements.length; elementIdx++) {
                String colLang = langs.get(elementIdx - 1);

                if (rowLang.equals(colLang)) {
                    continue;
                }

                Tuple<String, String> t = new Tuple(rowLang, colLang);

                double value = Double.parseDouble(elements[elementIdx]);
                if(value == -1.0) {
                    value = 1;
                }

                data.put(t, value);
            }
        }

        return data;
    }
}
