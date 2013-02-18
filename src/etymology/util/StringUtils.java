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

import etymology.input.FeatureVocabulary;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author arto
 */
public class StringUtils {

    private static final List<Character.UnicodeBlock> COMBINING_CHAR_BLOCKS;
    private static final Map<String, List<String>> STRING_TO_GLYPHS;

    static {
        STRING_TO_GLYPHS = new HashMap();
        COMBINING_CHAR_BLOCKS = new ArrayList();
        COMBINING_CHAR_BLOCKS.add(Character.UnicodeBlock.COMBINING_DIACRITICAL_MARKS);
        COMBINING_CHAR_BLOCKS.add(Character.UnicodeBlock.SPACING_MODIFIER_LETTERS);
    }

    public static List<String> splitToGlyphs(String s) {
        if (STRING_TO_GLYPHS.containsKey(s)) {
            return STRING_TO_GLYPHS.get(s);
        }


        List<String> glyphs = new ArrayList();

        int lastSplitIdx = s.length();
        for (int i = s.length() - 1; i >= 0; i--) {
            if (COMBINING_CHAR_BLOCKS.contains(Character.UnicodeBlock.of(s.charAt(i)))) {
                continue;
            }

            glyphs.add(s.substring(i, lastSplitIdx));
            lastSplitIdx = i;
        }

        Collections.reverse(glyphs);
        STRING_TO_GLYPHS.put(s, glyphs);
        return glyphs;
    }

    public static List<String> getUniqueGlyphs(List<String> words) {
        Set<String> glyphSet = new HashSet();
        for (String word : words) {
            if (!StringUtils.isOkWord(word)) {
                continue;
            }

            glyphSet.addAll(splitToGlyphs(word));
        }

        List<String> glyphs = new ArrayList(glyphSet);
        Collections.sort(glyphs, String.CASE_INSENSITIVE_ORDER);

        return glyphs;
    }

    public static List<String> getUniqueFeatures(List<String> featureBlocks) {
        Set<String> featureSet = new HashSet();
        
        for (String feature : featureBlocks) {
            
            if (!StringUtils.isOkFeature(feature)) {
                continue;
            }
            
            

            featureSet.addAll(splitToFeatures(feature));
        }

        List<String> features = new ArrayList(featureSet);
        Collections.sort(features);

        return features;

    }

    public static List<String> splitToFeatures(String feature) {
        return Arrays.asList(feature.trim().split(","));
    }

    public static int getGlyphComboLength(String glyphCombo) {
        int length = 0;
        for (char c : glyphCombo.toCharArray()) {
            //if the unicode representation of c can be found in the unicode block
            if (COMBINING_CHAR_BLOCKS.contains(Character.UnicodeBlock.of(c))) {
                
                continue;
            }
            //System.out.println(c);
            length++;
        }

        return length;
    }

    public static double getLevenshteinDistance(String s, String t) throws Exception {
        return getLevenshteinDistance(s, t, null, null);
    }

    /* from http://www.merriampark.com/ldjava.htm */
    public static double getLevenshteinDistance(String s, String t, FeatureVocabulary fv1, FeatureVocabulary fv2) throws Exception {
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }


        List<String> ss = splitToGlyphs(s);
        List<String> tt = splitToGlyphs(t);
        /*
        The difference between this impl. and the previous is that, rather
        than creating and retaining a matrix of size s.length()+1 by t.length()+1,
        we maintain two single-dimensional arrays of length s.length()+1.  The first, d,
        is the 'current working' distance array that maintains the newest distance cost
        counts as we iterate through the characters of String s.  Each time we increment
        the index of String t we are comparing, d is copied to p, the second int[].  Doing so
        allows us to retain the previous cost counts as required by the algorithm (taking
        the minimum of the cost count to the left, up one, and diagonally up and to the left
        of the current cost count being calculated).  (Note that the arrays aren't really
        copied anymore, just switched...this is clearly much better than cloning an array
        or doing a System.arraycopy() each time  through the outer loop.)

        Effectively, the difference between the two implementations is this one does not
        cause an out of memory condition when calculating the LD over two very large strings.
         */

//        int n = s.length(); // length of s
//        int m = t.length(); // length of t

        int n = ss.size();
        int m = tt.size();

        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }

        double p[] = new double[n + 1]; //'previous' cost array, horizontally
        double d[] = new double[n + 1]; // cost array, horizontally
        double _d[]; //placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        //char t_j; // jth character of t
        String t_j;

        double cost; // cost

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }

        for (j = 1; j <= m; j++) {
            //t_j = t.charAt(j - 1);
            t_j = tt.get(j-1);
            d[0] = j;

            for (i = 1; i <= n; i++) {
                //cost = s.charAt(i - 1) == t_j ? 0 : 1;
                if (fv1 == null) {
                    cost = ss.get(i-1).equals(t_j) ? 0 : 1;
                } else {
                    cost = getFeatureDistance(ss.get(i-1), t_j, fv1, fv2);
                }
                
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }


    public static double getFeatureDistance(String glyph1, String glyph2, FeatureVocabulary fv1, FeatureVocabulary fv2) throws Exception {

        if (    glyph1.equals(".") || glyph2.equals(".") ||
                glyph1.equals("^") || glyph2.equals("^") ||
                glyph1.equals("$") || glyph2.equals("$") ||
                glyph1.equals("#") || glyph2.equals("#") ) {
            throw new Exception("String containing special characters ");
        }
        //System.out.println(glyph1 + " " + glyph2);
        String f1 = fv1.getFeature(fv1.getGlyphIndex(glyph1));
        String f2 = fv2.getFeature(fv2.getGlyphIndex(glyph2));
         //System.out.println("f1 : " + f1 + " f2 : " + f2);
       // Thread.currentThread().sleep(10);

        if (f1.startsWith("V") ^ f2.startsWith("V")) {
            return 1;
        }


        if (f1.length() != f2.length()) {
            System.out.println("glyph 1: " + f1);
            System.out.println("glyph 2: " + f2);
            throw new Exception("FeatureVectors not the same type, different length");
        }

        int notEqual = 0;
        int numOfFeatures = 0;

        //ignore the first feature == type
        for (int i=1; i< f1.length(); i++) {
            if (f1.charAt(i) != f2.charAt(i)) {
                notEqual++;
            }
            numOfFeatures++;
        }

        double cost = (1.0 * notEqual/(numOfFeatures));
        return cost;

    }


    public static int getUniqueGlyphCount(List<String> words) {
        return getUniqueGlyphs(words).size();
    }

    public static boolean isOkWord(String word) {
        //I don't know why this code was removing all words which contain V ' ?
        //There are tons of 's in saami, which this coe removes the words, so a better idea is to remove these symbols from banned symbols, and add a conversion
        //rule which removes saami '.
        //PS: maybe these symbols are because of some other applications that the current function may have, --> TODO: check, and change if needed.
        //--javad 25/06/2012
        if (word == null || word.equals("-")){// || word.contains("?") || word.contains("'") || word.contains("V") ) {
            return false;
        }

        return true;
    }

    public static boolean isOkWordForFeatureData(String word) {
        if (word == null || "-".equals(word) || "?".equals(word)) {
            return false;
        }

        return true;

    }

    public static boolean isOkFeature(String feature) {
        if (feature == null) {
            return false;
        }
        return true;
    }


    public static String tryReadFileAsString(File file) {
        try {
            return readFileAsString(file);
        } catch (IOException ex) {
            Logger.getLogger(StringUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static String readFileAsString(File file) throws IOException {
        final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        final byte[] bytes = new byte[(int) file.length()];
        bis.read(bytes);
        bis.close();

        return new String(bytes, Charset.forName("UTF8").displayName());
    }

    public static String rightAlign(int totalWidth, String s) {
        return String.format("%1$#" + totalWidth + "s", s);
    }

    public static String leftAlign(int totalWidth, String s) {
        return String.format("%1$-" + totalWidth + "s", s);
    }

    public static String reverseString(String s) {

        List<String> glyphs = splitToGlyphs(s);
        StringBuilder sb = new StringBuilder();
        for (int i=glyphs.size(); i>0; i--) {
            sb.append(glyphs.get(i-1));
        }
        return sb.toString();
    }
}
