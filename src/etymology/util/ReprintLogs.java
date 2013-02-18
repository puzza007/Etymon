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

import java.io.*;
import java.util.*;

/**
 *
 * @author lv
 */
public class ReprintLogs {
    
    static final String logDir = "/home/fs/lv/etymology/logs/bio_17";
    private List<String> filenameMustContain;
    private List<File> files; //= getFiles(logDir, filenameMustContain);
    static final String[] allLangs = new String[]{"fin", "est", "khn_dn", "khn_v", "kom_s", "kom_p", "man_p", "man_so", "mar_kb", "mar_b", "mrd_e", "mrd_m",  "saa_n", "saa_l",  "udm_s", "udm_g", "ugr"};
    //static final String[] allLangs = new String[]{"fin", "est", "khn_dn", "khn_v", "kom_s", "kom_p", "man_p","man_tj", "mar_kb","mar_b", "mrd_e","mrd_m", "saa_n","saa_l", "udm_s","udm_g", "ugr"};
    
    public ReprintLogs(List<String> filenameMustContain) {
        this.filenameMustContain = filenameMustContain;
        files = getFiles(logDir, filenameMustContain);
    }
    
    
    public static List<File> getFiles(String logDir, List<String> filenameMustContain) {
        
        List<File> files = new ArrayList();
        
        if (!new File(logDir).exists()) {
            System.out.println("logDir not exist.");
            return files;
        }
        
        IterateFiles:
        for (File file : new File(logDir).listFiles()) {
            String filename = file.getName();
            for (String tag : filenameMustContain) {
                if (!filename.contains(tag)) {
                     continue IterateFiles;
                }
            }
            files.add(file);
        }
        return files;
    }
    
    public static List<String> getUnionAlph() throws IOException {
        //implement
        List<String> unionAlph = new ArrayList<String>();
        List<File> bestFiles;
        //find all .best files like (suffice)
        bestFiles = getFiles(logDir, Arrays.asList(new String[] {"best"}));
        //for (File f : bestFiles) {
        //    System.out.println(f.getName());
        //}
        //looping through all the files in bestFiles to collect the Union alphabet
        for (File file : bestFiles) {
            String line;
            Scanner s = new Scanner(StringUtils.readFileAsString(file));
            line = s.nextLine();
            line = s.nextLine();
            line = s.nextLine();
            line = line.trim();
            //System.out.println(line);
            for(String glyph : Arrays.asList(line.split("\\s+"))) {
                if(glyph != "" && glyph != "\n") {
                    unionAlph.add(glyph);
                }
            }
        }
        
        Set alphabetSet = new HashSet(unionAlph);
        List<String> alphabetList = new ArrayList(alphabetSet);
        Collections.sort(alphabetList);
                
        return alphabetList;
    }
    
    public static String formatNumber(String number) {
        if (number.equals(".")) {
            return ".    ";
        } else {
            int num = Integer.parseInt(number);
            if (num < 10 ) {
                return (num + "    ");
            } else if (num < 100) {
                return (num + "   ");
            } else {
                return (num + "  ");
            }
        }
        
    }
    
     public static List<String> getLangAlphabet(String lang) throws IOException {
        File file = new File ("/home/fs/lv/etymology/logs/bio_17/" + "2D-CB+kinds-Cond+kinds-starling-top2-dialects.utf8-" + lang + "-" + lang + "-1x1-simann-0.99-50.0-data.log.best");
        Scanner scan = new Scanner(StringUtils.readFileAsString(file));
        scan.nextLine();
        scan.nextLine();
        String line = scan.nextLine();

        String[] lineArray = line.split("\\s+");
        List<String> lineList = Arrays.asList(lineArray);
        return lineList;
        
        

        
    }
    
    
    
    
    
    // reprint the log file
    public static String reprint(File file, List<String> unionAlph, String lang1, String lang2) throws IOException {
        
        String[] unionAlphArray = new String[unionAlph.size()];
        unionAlph.toArray(unionAlphArray);
        Scanner scan = new Scanner(StringUtils.readFileAsString(file));
        String line;
        StringBuilder sb = new StringBuilder();
        
        //the first line in the matrix is fixed
        sb.append("    ");
        for(int i = 0; i < unionAlphArray.length; i++) {
            if(unionAlphArray[i].length() > 1){
                sb.append(unionAlphArray[i] + "    ");
            } else {
                sb.append(unionAlphArray[i] + "    ");
            }
            
        }
        sb.append("\n");
        //System.out.println(sb);
        
        
        line = scan.nextLine();     
        line = scan.nextLine();
        line = scan.nextLine();
        line = line.trim();
        
        String[] glyphArray = line.split("\\s+");
        //for(int i = 0; i < glyphArray.length; i++){
        //    System.out.println(glyphArray[i]);
   // }
        line = scan.nextLine();
       
        while (! (line = scan.nextLine()).startsWith("Total")) {
            //System.out.println(line);
            String[] splitLine = line.trim().split("\\s+");
            sb.append(splitLine[0] + "   ");
            //System.out.println(sb);
            //for(int i = 0; i < splitLine.length; i++) {
            //    System.out.println(splitLine[i]);
            //}
            //build the map with the union alphabet and the current line
            
            HashMap<String, String> hm = new HashMap();
            for(int i = 0; i < glyphArray.length; i++) {
                //System.out.println(glyphArray[i] + ": " + splitLine[i + 1]);
                if (splitLine[i + 1].equals(".")) {
                    hm.put(glyphArray[i], "0");
                }   else {
                    hm.put(glyphArray[i], splitLine[i + 1]);
                }
                    
                }
            
            for(int i = 0; i < unionAlphArray.length; i++) {
                if( Arrays.asList(glyphArray).contains(unionAlphArray[i])) {
                    hm.get(unionAlphArray[i]);
                    
                        sb.append(formatNumber(hm.get(unionAlphArray[i])));
                } else {
                    sb.append("0    ");
                }
            }
            //System.out.println(sb);
            sb.append("\n");
        }
        //BufferedWriter out = new BufferedWriter(new FileWriter("/home/lv/Documents/" + lang1 + "-" + lang2 + ".first"));
        //out.write(sb.toString());
        //out.close();
        System.out.println(sb);
        return sb.toString();
    }
    
    
    public static void main(String[] args) throws IOException {
        
        List<String> UnionAlph;
        List<String> mustWords = Arrays.asList(new String[]{"must1", "must2"});
        //FIN EST KHN_DN KOM_S MAN_P MAR_KB MRD_E SAA_N UDM_S UGR
        
        //ReprintLogs reprintLogs = new ReprintLogs(mustWords);
        
        //Get union alphbet
        UnionAlph =ReprintLogs.getUnionAlph();
        //System.out.println(UnionAlph.toString());
        
        for (int i = 0; i < allLangs.length; i++) {
            String string = "";
            for (int j = 0; j < allLangs.length; j++) {
                if (i == j) {continue;}
                
                String lang1 = allLangs[i];
                String lang2 = allLangs[j];
                List<File> bestFile = getFiles(logDir, Arrays.asList(new String[]{lang1 + "-" + lang2, "best"}));
                for (File file : bestFile) {
                    String fileName = file.getName();
                    String[] fileNameArray = fileName.split("-");
                    String newFilename = fileNameArray[6] + " - " + fileNameArray[7];
                    System.out.println("2D-" + newFilename);
                    string += "2D-" + newFilename + "\n";
                    string += reprint(file, UnionAlph, allLangs[i], allLangs[j]) + "\n";
                    
                }
                
               
                
            }
            BufferedWriter out = new BufferedWriter(new FileWriter("/home/lv/Documents/" + allLangs[i] + ".first"));
            out.write(string);
            out.close();
        }
        
        
    }
    
 
}
