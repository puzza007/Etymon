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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author sxhiltun
 */
public class FeatureConverter {
    public static final String DO_NOT_USE_TAG = "DONOTUSE";

    //sound -> feature vector presentation
    private static Map<String, String> soundsToFeaturesMap = new TreeMap();

    //map of rules for sound modifiers
    private static Map<Integer, AbstractMap.SimpleEntry<Integer, Character>> vowelFeatures = new TreeMap();
    private static Map<Integer, AbstractMap.SimpleEntry<Integer, Character>> consonantFeatures = new TreeMap();

    //mapping of feature values to new encoding
    private static List<Map<Character, Character>> newVowelFeatures;
    private static List<Map<Character, Character>> newConsonantFeatures;

    //debugging
    private static String language;
    private static String wrd;

    static{
       soundsToFeaturesMap = createSoundsToFeaturesMap();

       vowelFeatures = createVowelFeaturesMap();
       consonantFeatures = createConsonantFeaturesMap();

       newVowelFeatures = buildVowelMapForNewEncoding(new ArrayList<Map<Character, Character>>());
       newConsonantFeatures = buildConsonantMapForNewEncoding(new ArrayList<Map<Character, Character>>());
    }


    /**
     * Returns a string containing list of features it consists of.
     * Features are separated by comma.
     * If a glyph in word does not have a feature vector presentation,
     * it is replaced with "null"
     *
     * Todo: replace comma separated string with a list of strings e.g.
     *
     * @param word
     * @param lang
     * @return
     */
    public static String getFeatures(String word, String lang) {

        //TODO: SSA data, different language names
        //      SSA: extend all rules for saami (and hungarian)

        wrd = word;
        language = lang;


        //preprocess the word
        word = EncodingConverter.decompose(word);
       // word = doCleanUp(word);
        String featureString = getFeatureList(word);

        return featureString;
    }


    private static String doCleanUp(String word) {        

        word = word.replace(String.valueOf((char)39), ""); //removal ok???
        word = word.replace((char)96, (char)768);

        return word;
    }

    /*
     * e.g. ānd --> VlBn5,CNd+n1,CPd+'1
     */
    private static String getFeatureList(String word) {
        List<String> glyphList = StringUtils.splitToGlyphs(word);
        StringBuilder sb = new StringBuilder();

        for (String glyph : glyphList) {
            String oldfeature = convertGlyph(glyph);
            String newfeature;
            if (oldfeature != null) {
                //convert to the new encoding
                newfeature = getNewEncodingOfFeature(oldfeature);
            }else {
                newfeature = DO_NOT_USE_TAG;
                System.out.println("NULL-feature: " + word + " " + newfeature);
            }
            sb.append(newfeature);
            sb.append(",");
        }
        //remove the last comma
        sb.delete(sb.length()-1, sb.length());

        return sb.toString();
    }

   private static String getNewEncodingOfFeature(String oldFeature) {

        List<Map<Character, Character>>  featureMap = new ArrayList();

        if (oldFeature.length() == 5) {
            featureMap = FeatureConverter.newVowelFeatures;
        }else if (oldFeature.length() == 6) {
            featureMap = FeatureConverter.newConsonantFeatures;
        }

        StringBuilder newFeature = new StringBuilder();

        for (int i=0; i<oldFeature.length(); i++) {
            char c = oldFeature.charAt(i);
            newFeature.append(featureMap.get(i).get(c));
        }

        return newFeature.toString();

    }


    private static String convertGlyph(String glyph) {
        String feature = null;
        String sound = null;

        Map<Integer, AbstractMap.SimpleEntry<Integer, Character>> modifierFeatureMap = null;
        int pos = 0;


        if (glyph.length() == 1) {
            sound = glyph;
        }
        else{
            pos = 0;
            //ǫ
            String ohook =  EncodingConverter.decompose(String.valueOf((char)491));
            //õ
            String otilde =  EncodingConverter.decompose(String.valueOf((char)245));

            //find the sound
            for (char c: glyph.toCharArray()) {
                if (Character.isLetter(c)) {

                    sound = String.valueOf(c);                    
                    
                    //two special cases; 
                    // (char)491 = 111 + 808  -> length 2 in featuremap
                    // (char)245 = 111 + 771 -length 2 in featuremap

                    if (glyph.contains(otilde)) {                        
                        sound = otilde;
                    }
                    if (glyph.contains(ohook)) {
                        sound = ohook;
                    }

                    if (pos > 0) {
                        glyph = glyph.replace(sound, "");
                        glyph = sound + glyph;
                    }

                    break;
                }
                pos++;
            }
        }


        feature = soundsToFeaturesMap.get(sound);
        if (feature == null) {
            System.err.print("Sound was not found: ");
            System.err.print(glyph);
            System.err.print(" ");
            System.err.print((int)sound.charAt(0));
            System.err.print(" ");
            System.err.print(wrd);
            System.err.print(" ");
            System.err.println(language);

            return null;

        } else {

            if (feature.length() == 5) {
                modifierFeatureMap = vowelFeatures;
            }else {
                modifierFeatureMap = consonantFeatures;
            }
        }


        pos++;
        if (glyph.length() > pos) {
            for (int i=pos; i<glyph.length(); i++) {
                int charValue = (int)(glyph.charAt(i));
                if (modifierFeatureMap.containsKey(charValue)) {
                    AbstractMap.SimpleEntry<Integer, Character> rule
                            = modifierFeatureMap.get(charValue);
                    feature = applyMofidiersToFeatureVector(feature, rule);
                }
            }
        }

        return feature;
    }



    private static String applyMofidiersToFeatureVector(String feature, AbstractMap.SimpleEntry<Integer, Character> rule) {
        StringBuilder sb = new StringBuilder(feature);
        int index = rule.getKey().intValue();
        if (rule.getValue().equals('-')) {
            int oldValue = Character.digit(sb.charAt(index), 9);
            int newValue = oldValue - 1;
            if (newValue < 1) {
                newValue = 1;
            }
            sb.setCharAt(index, Character.forDigit(newValue, 9));
        }else {
            sb.setCharAt(index, rule.getValue());
        }

        return sb.toString();

    }



    private static Map<Integer, AbstractMap.SimpleEntry<Integer, Character>> createVowelFeaturesMap() {
        vowelFeatures.put(813, mapping(1, '-'));
        vowelFeatures.put(776, mapping(2, '1'));
        vowelFeatures.put(785, mapping(2, '2'));
        vowelFeatures.put(814, mapping(2, '2'));
        vowelFeatures.put(775, mapping(2, '-'));
        vowelFeatures.put(803, mapping(2, '3'));
	vowelFeatures.put(778, mapping(3, '2'));
        vowelFeatures.put(774, mapping(4, '2'));
	vowelFeatures.put(768, mapping(4, '4'));
        vowelFeatures.put(772, mapping(4, '5'));
        vowelFeatures.put(770, mapping(4, '5'));

        return vowelFeatures;
    }


    private static Map<Integer, AbstractMap.SimpleEntry<Integer, Character>> createConsonantFeaturesMap() {
        consonantFeatures.put(803, mapping(2, '4'));
        consonantFeatures.put(769, mapping(4, '2'));
        consonantFeatures.put(755, mapping(4, '3'));
        consonantFeatures.put(703, mapping(4, '4'));
        consonantFeatures.put(772, mapping(5, '2'));
        consonantFeatures.put(780, mapping(2, '4'));

        return consonantFeatures;
    }

    private static AbstractMap.SimpleEntry<Integer, Character> mapping(int index, char value) {
        AbstractMap.SimpleEntry<Integer, Character> mapping =
                new AbstractMap.SimpleEntry(index, value);
        return mapping;
    }



   private static Map<String, String> createSoundsToFeaturesMap() {
        //vowels
        //close/high
        soundsToFeaturesMap.put(sound('i'),              "11113");
        soundsToFeaturesMap.put(sound((char)7433),       "11111");  //ᴉ
        soundsToFeaturesMap.put(sound('y'),              "11123");
                
        soundsToFeaturesMap.put(sound((char)623),        "11223");  //ɯ
        soundsToFeaturesMap.put(sound((char)1096),       "11223");  //ɯ
        soundsToFeaturesMap.put(sound('u'),              "11323");

        //mid-close
        soundsToFeaturesMap.put(sound('e'),              "12113");
        //soundsToFeaturesMap.put(sound('E'),              "12113");
        soundsToFeaturesMap.put(sound((char)601),        "12111");  //ə
        soundsToFeaturesMap.put(sound((char)477),        "12111");  //ə
        soundsToFeaturesMap.put(sound((char)491),        "12213");
        soundsToFeaturesMap.put(sound((char)245),        "12213");
        soundsToFeaturesMap.put(sound((char)629),        "12223");
        soundsToFeaturesMap.put(sound('o'),              "12323");

        //mid-open
        soundsToFeaturesMap.put(sound((char)603),        "13113");
        soundsToFeaturesMap.put(sound((char)1101),       "13111");
        soundsToFeaturesMap.put(sound((char)596),        "13323");

        //open/low
        soundsToFeaturesMap.put(sound('a'),              "14313");

        //what ever wowel, ignored here
        //soundsToFeaturesMap.put(sound((char)7432),       "10003"); //obs! TODO: new encoding

        //consonants
        //stop (1)
        soundsToFeaturesMap.put(sound('p'),              "211111");
        //soundsToFeaturesMap.put(sound('B'),              "211111");
        soundsToFeaturesMap.put(sound('b'),              "211211");
        soundsToFeaturesMap.put(sound('t'),              "213111");
        soundsToFeaturesMap.put(sound((char)977),        "253111"); // theta
        //soundsToFeaturesMap.put(sound('D'),              "213111");
        soundsToFeaturesMap.put(sound('d'),              "213211");
        soundsToFeaturesMap.put(sound('k'),              "215111");
        soundsToFeaturesMap.put(sound((char)610),        "215111");
        //soundsToFeaturesMap.put(sound('G'),              "215111");
        soundsToFeaturesMap.put(sound('g'),              "215211");
        soundsToFeaturesMap.put(sound((char)702),        "216111");
        soundsToFeaturesMap.put(sound((char)660),        "216111");

        //nasal  (2)
        soundsToFeaturesMap.put(sound('m'),              "221211");
        soundsToFeaturesMap.put(sound('n'),              "223211");
        soundsToFeaturesMap.put(sound((char)331),        "225211");
        

        //lateral (3),
        soundsToFeaturesMap.put(sound((char)671),        "233111");
        soundsToFeaturesMap.put(sound((char)7463),       "233111");
        soundsToFeaturesMap.put(sound((char)1083),       "233111");
        soundsToFeaturesMap.put(sound('l'),              "233211");

        //trill (4),
        soundsToFeaturesMap.put(sound((char)640),        "243111");
        soundsToFeaturesMap.put(sound('R'),              "243111");
        soundsToFeaturesMap.put(sound('r'),              "243211");

        //spirant (5),
        soundsToFeaturesMap.put(sound('f'),              "252111");
        soundsToFeaturesMap.put(sound('v'),              "252211");
        soundsToFeaturesMap.put(sound((char)359),        "253111"); // t with stroke = theta
        soundsToFeaturesMap.put(sound((char)948),        "253211");
        soundsToFeaturesMap.put(sound((char)273),        "253211");
        soundsToFeaturesMap.put(sound((char)240),        "253211");
        soundsToFeaturesMap.put(sound((char)967),        "255111");
        soundsToFeaturesMap.put(sound((char)947),        "255211");
        soundsToFeaturesMap.put(sound((char)611),        "255211");
        soundsToFeaturesMap.put(sound((char)485),        "255211");
        soundsToFeaturesMap.put(sound('h'),              "256111");

        //sibilant (6),
        soundsToFeaturesMap.put(sound('s'),              "263111");
        soundsToFeaturesMap.put(sound('z'),              "263211");

        //semi-vowel (7),
        soundsToFeaturesMap.put(sound('w'),              "271211");
        soundsToFeaturesMap.put(sound('j'),              "275211");

        //affricate (8)
        soundsToFeaturesMap.put(sound('c'),              "283111");
        soundsToFeaturesMap.put(sound((char)658),        "283211");

        return soundsToFeaturesMap;
    }

   private static String sound(char c) {
        String s = Character.toString(c);
        s = EncodingConverter.decompose(s);

        return s;
    }



    private static List<Map<Character, Character>> buildVowelMapForNewEncoding(List<Map<Character, Character>>  vowels) {
        for (int i=0; i<5; i++) {
            vowels.add(new TreeMap());
            switch(i) {
                case 0: // Group: G
                    vowels.get(i).put('1', 'V'); //vowel V
                    break;
                case 1: // Vertical articulation: V
                    vowels.get(i).put('1', 'h'); //close/high
                    vowels.get(i).put('2', 'c'); //mid-close
                    vowels.get(i).put('3', 'o'); //mid-open
                    vowels.get(i).put('4', 'l'); //open/low
                    break;
                case 2: // Horizontal articulation: H
                    vowels.get(i).put('1', 'F'); //front
                    vowels.get(i).put('2', 'M'); //central/medium
                    vowels.get(i).put('3', 'B'); //front
                    break;
                case 3: // Rounded: R
                    vowels.get(i).put('1', 'n'); //no
                    vowels.get(i).put('2', 'u'); //yes
                    break;
                case 4: // Length: L
                    vowels.get(i).put('1', '1');
                    vowels.get(i).put('2', '2');
                    vowels.get(i).put('3', '3');
                    vowels.get(i).put('4', '4');
                    vowels.get(i).put('5', '5');
                    break;
            }
        }
        return vowels;
    }

    private static List<Map<Character, Character>> buildConsonantMapForNewEncoding(List<Map<Character, Character>>  consonants) {
       for (int i=0; i<6; i++) {
           consonants.add(new TreeMap());
           switch(i) {
               case 0: // Group: G
                   consonants.get(i).put('2', 'C');
                   break;
               case 1: // Manner: M
                   consonants.get(i).put('1', 'P'); //Plosive-stop
                   consonants.get(i).put('2', 'N'); //nasal
                   consonants.get(i).put('3', 'L'); //lateral
                   consonants.get(i).put('4', 'T'); //trill
                   consonants.get(i).put('5', 'F'); //spirant-Frikative
                   consonants.get(i).put('6', 'S'); //sibilant
                   consonants.get(i).put('7', 'W'); //semi-vowel
                   consonants.get(i).put('8', 'A'); //affricate
                   break;
               case 2: // Place of articulation: P
                   consonants.get(i).put('1', 'b'); //bilabial
                   consonants.get(i).put('2', 'l'); //labiodental
                   consonants.get(i).put('3', 'd'); //dental
                   consonants.get(i).put('4', 'r'); //retroflex
                   consonants.get(i).put('5', 'v'); //velar
                   consonants.get(i).put('6', 'u'); //uvular
                   break;
               case 3: // Voiced: V
                   consonants.get(i).put('1', '-'); //no
                   consonants.get(i).put('2', '+'); //yes
                   break;
               case 4: // Secondary articulation: S
                   consonants.get(i).put('1', 'n'); //none
                   consonants.get(i).put('2', '\''); //palatalized
                   consonants.get(i).put('3', 'w'); //labialized
                   consonants.get(i).put('4', 'h'); //aspirated
                   break;
               case 5: // Length: L
                   consonants.get(i).put('1', '1'); //normal/short
                   consonants.get(i).put('2', '2'); //long
                   break;
           }
       }
       return consonants;
    }

    //blaablaablaa
    public static void main(String[] args) {

        char c1 = (char)39;
        String s = String.format ("\\u%04x", (int)c1);
        System.out.println("39: " + s);

        c1 = (char)96;
        s = String.format ("\\u%04x", (int)c1);
        System.out.println("96: " + s);

        c1 = (char)768;
        s = String.format ("\\u%04x", (int)c1);
        System.out.println("768: " + s);

        s = "\u0060";
        System.out.println("s: " + s);
        System.out.println("39: "+String.valueOf((char)39));
        System.out.println("96" + String.valueOf((char)96));
        System.out.println("768" + String.valueOf((char)768));
     }







}


