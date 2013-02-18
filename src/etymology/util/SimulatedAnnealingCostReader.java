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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author sxhiltun
 */
public class SimulatedAnnealingCostReader {
    
    private static final String directory = "/home/sxhiltun/NetBeansProjects/EtyMalign/log/";
    //private static final String directory = "/fs-2/a/sxhiltun/sanity-checks/context-separate-zero-best/";
    //private static final String inputFileName = directory + "two_lang-codebook_with_kinds-starling-top-dialects.utf8-man_p-est-1x1-simann-0.99-init_temp-100.0-data.log";
    private static final String inputFileName = directory +"context_2d-starling-top-dialects.utf8-man_p-est-simann-0.995-init_temp-50.0-zero-multi-binary-data.log";
    private static final String outputFileName =directory +  "";
    private static Map<String, List<Double>> dataMap = new HashMap<String, List<Double>>(); 
    
    
    public static void main(String[] args) throws FileNotFoundException {
        initMap();
        readData();
        writeData();
    }
    
    private static void initMap() {
        dataMap.put("ITERATION:", new ArrayList<Double>());
        dataMap.put("TEMPERATURE:", new ArrayList<Double>());
        dataMap.put("COST OF TREES", new ArrayList<Double>());
        //dataMap.put("COST", new ArrayList<Double>());
    }

    private static void readData() throws FileNotFoundException {
        
        File f = new File(inputFileName);
        Scanner sc = new Scanner(f);
        
        String re = "([0-9])[^.,0123456789]([0-9]{3})";
        
        Pattern pa = Pattern.compile(re);
        
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.contains("Retrieving final cost for best alignments")) {
                break;
            }

            Matcher  m = pa.matcher(line);
            if (m.find()) {
               //System.out.println(m.group(0) + " " + m.group(1) + m.group(2));
               //System.out.println(Arrays.toString(m.group(0).toCharArray()));
               line = line.replaceAll(m.group(0), m.group(1) + m.group(2));
               
            }
            
            for (String key : dataMap.keySet()) {
                
                
                if (line.contains(key)) {
                    String[] values = line.trim().split(" ");
                    String value = values[values.length-1].replace(",", ".");
                    try {
                        dataMap.get(key).add(Double.valueOf(value));                            
                    }catch(Exception e) {
                        System.out.println("value: " + Arrays.toString(values) + " " + value);
                    }
                    break;
                }
            }                        
        }
        sc.close();
    }

    private static void writeData() {
        int iterations=0;
        int smallest=0;
        for (String key : dataMap.keySet()) {
            iterations = dataMap.get(key).size();
            if (smallest > dataMap.get(key).size() || smallest == 0) {
                smallest = iterations;
            }
            System.out.println(key + " " + iterations);
        }
        
        for (int i=0; i<smallest; i++) {
            for (String key : dataMap.keySet()) {
                System.out.print(dataMap.get(key).get(i));
                System.out.print("\t\t");                                
            }
            System.out.println("");
        }
    }

    
    
}
