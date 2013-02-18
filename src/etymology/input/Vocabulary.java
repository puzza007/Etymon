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

package etymology.input;

import java.util.List;

/**
 *
 * @author sxhiltun
 */
public interface Vocabulary {

    void add(String glyph);

    String getLanguage();

    List<String> getSingleLengthSymbols();

    int getSize();

    String getSymbol(int index);

    int getSymbolIndex(String glyph);

    List<String> getSymbols();

    List<Integer> getSymbolsAsIndexes(List<String> glyphs);

    int getTotalGlyphs();

    int getTotalWords();

    String getWord(int wordIndex);
    
    List<Integer> getWordIndexes(int wordIndex);

    public boolean isConsonant(int symbolIndex);

    public boolean isVowel(int symbolIndex);

}
