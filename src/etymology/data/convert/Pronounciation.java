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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author avihavai
 */
public class Pronounciation {

    private static final Map<String, String> pronounciations = new HashMap();


    static {

        pronounciations.put("ts&#780;", "č");
        pronounciations.put(EncodingConverter.compose("tš"), "č");
        pronounciations.put(EncodingConverter.decompose("tš"), "č");

        pronounciations.put(EncodingConverter.compose("dž̌"), "ǯ");
        pronounciations.put(EncodingConverter.decompose("dž̌"), "ǯ");

        pronounciations.put(EncodingConverter.compose("ĳ"), "ij");
        pronounciations.put(EncodingConverter.decompose("ĳ"), "ij");



       
    }

    public static String changeText(String text) {

        for (String pron : pronounciations.keySet()) {
            if (text.contains(pron)) {
                text = text.replaceAll(pron, pronounciations.get(pron));
            }

        }
        return text;
    }

    
    public static void main(String[] s) {


    }
}
