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

import java.text.Normalizer;
import java.util.List;
import java.util.Map;

/**
 *
 * @author avihavai
 */
public class EncodingConverter {

    public static List<Map<String, String>> convertData(List<Map<String, String>> data) {
        for (Map<String, String> wordSet : data) {
            for (String lang : wordSet.keySet()) {
                wordSet.put(lang, decompose(wordSet.get(lang)));
            }
        }

        return data;

    }

    public static String decompose(String word) {
        //decompose the unicode form
        word = Normalizer.normalize(word, Normalizer.Form.NFD);
        return word;
    }

    public static String compose(String word) {
        //compose the unicode form
        word = Normalizer.normalize(word, Normalizer.Form.NFC);
        return word;
    }
}
