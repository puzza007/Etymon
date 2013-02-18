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

package etymology.align.matrices;

import etymology.align.Kind;
import etymology.input.Input;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author avihavai
 */
public class ThreeLangKindHolder implements KindHolder {
    private Kind[][][] kindCache;
    protected Map<String, Kind> kindMap;


    public ThreeLangKindHolder(Input input, int l1SymbolCount, int l2SymbolCount, int l3SymbolCount) {
         kindCache = new Kind[l1SymbolCount][l2SymbolCount][l3SymbolCount];
         kindMap = new HashMap<String, Kind>();

         initKinds(input, l1SymbolCount, l2SymbolCount, l3SymbolCount);
    }

    public Collection<Kind> getKinds() {
        return kindMap.values();
    }

    
    public Kind getKind(int l1SymbolIdx, int l2SymbolIdx, int l3SymbolIdx) {
        if(kindCache[l1SymbolIdx][l2SymbolIdx][l3SymbolIdx] != null) {
            return kindCache[l1SymbolIdx][l2SymbolIdx][l3SymbolIdx];
        }

        // System.out.println("Attempting to find kind: (" + l1SymbolIdx + ", " + l2SymbolIdx + ", " + l3SymbolIdx + ")" );
        for (Kind k : kindMap.values()) {
            if (k.inRegion(l1SymbolIdx, l2SymbolIdx, l3SymbolIdx)) {
                kindCache[l1SymbolIdx][l2SymbolIdx][l3SymbolIdx] = k;
                return k;
            }
        }



        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void initKinds(Input input, int l1SymbolCount, int l2SymbolCount, int l3SymbolCount) {
        Kind k;

        // start to start
        k = new Kind("$-$-$", new int[]{0, 0, 1, 1, 2, 2});
        k.setNumOfNonZeroEvents(1);
        kindMap.put("$-$-$", k);

        k = new Kind("G-G-G", new int[]{1, l1SymbolCount, 1, l2SymbolCount, 1, l3SymbolCount});
        kindMap.put("G-G-G", k);

        k = new Kind("G-G-.", new int[]{1, l1SymbolCount, 1, l2SymbolCount, 0, 1});
        kindMap.put("G-G-.", k);

        k = new Kind("G-.-G", new int[]{1, l1SymbolCount, 0, 1, 1, l3SymbolCount});
        kindMap.put("G-.-G", k);

        k = new Kind(".-G-G", new int[]{0, 1, 1, l2SymbolCount, 1, l3SymbolCount});
        kindMap.put(".-G-G", k);

        k = new Kind(".-.-G", new int[]{0, 1, 0, 1, 1, l3SymbolCount});
        kindMap.put(".-.-G", k);

        k = new Kind(".-G-.", new int[]{0, 1, 1, l2SymbolCount, 0, 1});
        kindMap.put(".-G-.", k);

        k = new Kind("G-.-.", new int[]{1, l1SymbolCount, 0, 1, 0, 1});
        kindMap.put("G-.-.", k);
    }

    public Kind getKind(int... symbolIdxs) {
        if(symbolIdxs.length != 3) {
            throw new IllegalArgumentException("Illegal symbol idx amount.");
        }

        return getKind(symbolIdxs[0], symbolIdxs[1], symbolIdxs[2]);
    }
}
