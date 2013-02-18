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

import etymology.util.StringUtils;
import java.io.*;
import java.util.*;

/**
 *
 * @author lv
 */
public class ReprintLogs2 {
    static final String logDir = "/home/lv/Documents/";
    //static final String[] allLangs = new String[]{"fin", "est", "khn", "kom", "man", "mar", "mrd", "saa", "udm", "ugr"};
    
    
    public static List<String> getUnionAlphabet(String logDir) throws IOException {
        File file = new File(logDir + "fin.first");
        Scanner s = new Scanner(StringUtils.readFileAsString(file));
        s.nextLine();
        String line = s.nextLine().trim();
        String[] lineArray = line.split("\\s+");
        List<String> lineList = Arrays.asList(lineArray);
        Collections.sort(lineList);
        //System.out.println(line);
        return lineList;
    }
    
    public static ArrayList<String> getLangAlphabet(String lang, List<String> UnionAlphabet) throws IOException {
        /*
         * Languages to choose(top dialect): FIN EST KHN_DN KOM_S MAN_P MAR_KB MRD_E SAA_N UDM_S UGR
         */
        
        File file = new File ("/home/fs/lv/etymology/logs/bio_17/" + "2D-CB+kinds-Cond+kinds-starling-top2-dialects.utf8-" + lang + "-" + lang + "-1x1-simann-0.99-50.0-data.log.best");
        Scanner scan = new Scanner(StringUtils.readFileAsString(file));
        scan.nextLine();
        scan.nextLine();
        String line = scan.nextLine().trim();

        String[] lineArray = line.split("\\s+");
        ArrayList<String> lineList = new ArrayList<String>(Arrays.asList(lineArray));
        Collections.sort(lineList);
        return lineList;
        
        

        
    }
    
    public static void readMatrix(Scanner s, StringBuilder sb, List<String> langAlphabet, int lenOfUnionAlph) {
       
            HashMap<String, String> map = new HashMap<String, String>();
            String line = s.nextLine();
           // String line2 = s2.nextLine();
            //System.out.println(line);
            sb.append(line + "\n");
            sb.append(s.nextLine() + "\n");
            //System.out.println(sb);
            //collect the alphabet
            while( (s.hasNext()) && !((line = s.nextLine()).equals(""))) {
                String[] lineArray = line.split("\\s+");
                //System.out.println(lineArray[0]);
                map.put(lineArray[0], line);
            }
            //generate sb
            for(String langAlph : langAlphabet) {
                //sb.append(langAlph + "    ");
                if (map.containsKey(langAlph)) {
                    sb.append(map.get(langAlph) + "\n");
                } else {
                    sb.append(langAlph + "   ");
                    for(int i = 0; i < lenOfUnionAlph; i++) {
                        sb.append("0    "); //00
                    }
                    sb.append("\n");
                    
                }
            }
            sb.append("\n");
            
        }
    
    
    public static void main(String[] args) throws IOException {
        //String lang = "kom_s";
        /*
         * Languages to choose(top dialect): FIN EST KHN_DN KOM_S MAN_P MAR_KB MRD_E SAA_N UDM_S UGR
         */
       String[] langArr = {"est", "fin", "khn_dn", "khn_v","kom_s", "kom_p", "man_p", "man_so", "mar_kb", "mar_b", "mrd_e", "mrd_m","saa_n", "saa_l", "udm_s", "udm_g",  "ugr"};
        List<String> langList = Arrays.asList(langArr);
        for(String lang : langList) {
            
        
        File file = new File(logDir + lang + ".first");
        List<String> unionAlphabet = getUnionAlphabet(logDir);
        //for(String s : unionAlphabet) {
        //    System.out.println(s);
        //}
        int lenOfUnionAlph = unionAlphabet.size();
        ArrayList<String> langAlphabet = getLangAlphabet(lang, unionAlphabet);
        langAlphabet.remove(".");

        
        for(String s : langAlphabet) {
            //System.out.println(s);
        }
        Scanner s = new Scanner(StringUtils.readFileAsString(file));
    //    Scanner s2 = new Scanner(StringUtils.readFileAsString(file));
        StringBuilder sb = new StringBuilder();
        //sb.append(s.nextLine());
        
        for (int i = 0; i < langArr.length - 1; i++) {
            //System.out.println("!!!!!!!!!!!!!!!!!!!!");
            
            if(s.hasNextLine()) {
                readMatrix(s, sb, langAlphabet, lenOfUnionAlph);
            }
            
        }
        
        BufferedWriter out = new BufferedWriter(new FileWriter("/home/lv/Documents/" + lang + ".second"));
        out.write(sb.toString() + "\n\n");
        out.close();
        System.out.println(sb + "\n\n");
        
        
    }
    }
}
