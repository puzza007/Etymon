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

package etymology.context;

import etymology.context.FeatureTreeContainer.Context;
import etymology.context.FeatureTreeContainer.Level;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author sxhiltun
 */
public class ContextCell {
    
    private int positionInWord;
    private int wordIndex;
    private int symbol;

    private Map<Context, int[]> contextMap;

    public ContextCell(int symbol, int wordIndex, int positionInWordIndex) {
        this.symbol = symbol;
        this.wordIndex = wordIndex;
        this.positionInWord = positionInWordIndex;

        contextMap = new EnumMap<Context, int[]>(Context.class);
        for (Context context : Context.values()) {
            contextMap.put(context, new int[2]);
        }
    }

    public int getSymbol() {
        return this.symbol;
    }

    public int getWordIndex() {
        return this.wordIndex;
    }

    public int getPositionInWord() {
        return this.positionInWord;
    }

    public int getCandidateIndexInContext(Level level, Context context) throws Exception {
        return contextMap.get(context)[level.getLevelIdx()];
    }

    public void setGlyphIndexInContext(int symbol, Level level, Context context) throws Exception {
        contextMap.get(context)[level.getLevelIdx()] = symbol;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Context context : Context.values()) {
            sb.append(context.name()).append(Arrays.toString(contextMap.get(context))).append(" ");
        }

        return sb.toString();
    }
}
