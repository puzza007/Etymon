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
package etymology.data.convert;

import etymology.util.StringUtils;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author avihavai
 */
public class ConversionRules {

    private Map<String, Map<String, String>> languageToRuleMap;
    private Map<String, Map<String, String>> languageToConsonantRuleMap;
    private Map<String, Map<String, String>> languageToVowelRuleMap;

    public ConversionRules(String rules) {
        languageToRuleMap = new TreeMap<String, Map<String, String>>();
        languageToConsonantRuleMap = new TreeMap<String, Map<String, String>>();
        languageToVowelRuleMap = new TreeMap<String, Map<String, String>>();
        init(rules);
        //System.out.println("Conversion rules: \n"+ languageToRuleMap);
    }

    public ConversionRules(File file) throws IOException {
        this(StringUtils.readFileAsString(file));
    }

    private void init(String rules) {
        Scanner sc = new Scanner(rules);

        while (sc.hasNextLine()) {

            String line = sc.nextLine();


            if (line.contains("#")) {
                line = line.substring(0, line.indexOf("#"));
            }

            if (line.trim().isEmpty()) {
                continue;
            }

            String[] elements = line.split("\\s+");
            
            if (elements.length != 3 && elements.length != 2) {
                
                System.out.println(elements.length);
                System.out.println("Illegal conversion rule: " + line);
                continue;
            }
            if (elements.length == 2) {

                String[] elements2 = new String[3];
                elements2[0] = elements[0];
                elements2[1] = elements[1];
                elements2[2] = "";
                elements = elements2;

            }

            add(elements[0].toLowerCase().trim(), elements[1], elements[2]);
        }
    }

    private void add(String language, String convertFrom, String convertTo) {
        Map<String, Map<String, String>> mapToAddTo = languageToRuleMap;
        
        //Right now, we do not have any C or V rules which try to tell st about consonants or vowels, and in proto language, we have lots of "V"s which have
        //to be changed to something else, and it also seems that te following languageToConsonantMap and languageToVowelMap are
        //never used, thats why I removed it, because when this portion of code is present, V is not changed to naything.
        //--Javad 26/06/2012
/*        if(convertFrom.contains("C")) {
            System.out.println("Consonant rule!: " + language + ", " + convertFrom + ", " + convertTo);
            mapToAddTo = languageToConsonantRuleMap;
        } else if (convertFrom.contains("V")) {
            System.out.println("Vowel rule!: " + language + ", " + convertFrom + ", " + convertTo);
            mapToAddTo = languageToVowelRuleMap;
        }
*/

        language = language.toLowerCase().trim();

        if (!mapToAddTo.containsKey(language)) {
            //rules must be used in the given order!!!
            mapToAddTo.put(language, new LinkedHashMap<String, String>());
        }

        mapToAddTo.get(language).put(convertFrom, convertTo);
        
        convertFrom = EncodingConverter.decompose(convertFrom);
        convertTo = EncodingConverter.decompose(convertTo);
        
        mapToAddTo.get(language).put(convertFrom, convertTo);
    }

    public String applyConversionRule(String language, String fromWord) {
        language = language.toLowerCase().trim();

        return applyBasicConversionRules(language, fromWord);
    }



    private String applyBasicConversionRules(String language, String fromWord) {
        if(!languageToRuleMap.containsKey(language)) {
            return fromWord;
        }

        // first pass without decomposing
        Map<String, String> conversionRules = languageToRuleMap.get(language);


        // second pass with decomposing
        fromWord = EncodingConverter.decompose(fromWord);
        for(String rule: conversionRules.keySet()) {            
            if(fromWord.contains(rule)) {
                fromWord = fromWord.replaceAll(rule, conversionRules.get(rule));
            }
        }

        return EncodingConverter.compose(fromWord);
    }

}
