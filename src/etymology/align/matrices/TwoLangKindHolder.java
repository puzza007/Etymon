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
import etymology.config.Configuration;
import etymology.input.Input;
import etymology.util.StringUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author avihavai
 */
public class TwoLangKindHolder implements KindHolder {
    protected Kind[][] kindCache; // cache for kinds, should be a tree of some sort -- do not reset
    protected Map<String, Kind> kindMap;

    private boolean utilizeDotToDot;
    private boolean utilizeHashToHash;

    private int lang1Index = 0;
    private int lang2Index = 1;
    

    public TwoLangKindHolder(int l1GlyphCount, int l2GlyphCount, int lang1Index, int lang2Index) {
        this(l1GlyphCount, l2GlyphCount, Configuration.getInstance().getMaxGlyphsToAlign() > 1);
        this.lang1Index = lang1Index;
        this.lang2Index = lang2Index;

    }

    public void setUtilizeDotToDot(boolean utilizeDotToDot) {
        this.utilizeDotToDot = utilizeDotToDot;
    }

    public void setUtilizeHashToHash(boolean utilizeHashToHash) {
        this.utilizeHashToHash = utilizeHashToHash;
    }

    
    
    private TwoLangKindHolder(int l1GlyphCount, int l2GlyphCount, boolean multiGlyph) {
        kindMap = new HashMap();

        int extra = 0;
        if(Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
            extra = 2;
        }
        if (Configuration.getInstance().isRemoveSuffixes()) {
            extra = 2;
        }

        if (multiGlyph) {
            kindCache = new Kind[(l1GlyphCount + 1 + extra)
                    * (l1GlyphCount + 1 + extra) + 5][(l2GlyphCount + 1 + extra) *
                    (l2GlyphCount + 1 + extra) + 5];
        } else {
            kindCache = new Kind[l1GlyphCount + 2 + extra][l2GlyphCount + 2 + extra];
        }

        
        initSingleGlyphKinds(l1GlyphCount, l2GlyphCount);

        if(!multiGlyph) {
            return;
        }        

        if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
            initWordBoundaryGlyphKinds(l1GlyphCount, l2GlyphCount);
        }
        initMultiGlyphKinds(l1GlyphCount, l2GlyphCount);
    }


    public static TwoLangKindHolder getSingleGlyphKindHolder(int[][] oneToOneAlignmentMatrix) {
        TwoLangKindHolder holder = new TwoLangKindHolder(oneToOneAlignmentMatrix.length-1, oneToOneAlignmentMatrix[0].length-1, 0, 1);
        
        for(int row = 0; row < oneToOneAlignmentMatrix.length; row++) {
            for(int col = 0; col < oneToOneAlignmentMatrix[0].length; col++) {
                if(oneToOneAlignmentMatrix[row][col] <= 0) {
                    continue;
                }

                holder.getKind(row, col).increaseNumOfNonZeroEvents();
            }
        }

        return holder;
    }

    public Collection<Kind> getKinds() {
       // System.out.println(kindMap.values());
        return kindMap.values();
    }



    private void initSingleGlyphKinds(int l1GlyphCount, int l2GlyphCount) {
        Kind k;
        int l1Symbols = 1 + l1GlyphCount;
        int l2Symbols = 1 + l2GlyphCount;

        if (utilizeHashToHash) {
            // start to start -- usually taken into account already at cost computation
            k = new Kind("#-#", new int[]{0, 0, 1, 1});
            k.setNumOfNonZeroEvents(1);
            k.setPrior(1.0);
            kindMap.put("#-#", k);
        }

        if(Configuration.getInstance().getLanguages().size() > 2) {
            // dot to dot, only ok for more than two langs
            // (viterbi matrix construction takes care that there's no dot to dot in 2d)
            k = new Kind(".-.", new int[]{0, 1, 0, 1});
            k.setPrior(1.0);
            kindMap.put(".-.", k);
        }
        

        // single glyph alignments
        k = new Kind(".-G", new int[]{0, 1, 1, l2Symbols});
        k.setPrior(1.0);
        kindMap.put(".-G", k);

        k = new Kind("G-.", new int[]{1, l1Symbols, 0, 1});
        k.setPrior(1.0);
        kindMap.put("G-.", k);

        k = new Kind("G-G", new int[]{1, l1Symbols, 1, l2Symbols});
        k.setPrior(1.0);
        kindMap.put("G-G", k);
    }




    private void initWordBoundaryGlyphKinds(int l1GlyphCount, int l2GlyphCount) {

        //TODO: fix priors

        //TODO: why +1 here??
        int l1Symbols = 1 + l1GlyphCount;
        int l2Symbols = 1 + l2GlyphCount;

        Kind k;

        // (^:^) and  ($:$)
        k = new Kind("^-^", new int[]{0, 1, 0, 1});
        k.setPrior(1.0);
        kindMap.put("^-^", k);

        // equals (.:tau)
        k = new Kind("^-^G", new int[]{0, 1, 1, l2Symbols});
        k.setPrior(1.0);
        kindMap.put("^-^G", k);

        // equals (sigma:.)
        k = new Kind("^G-^", new int[]{1, l1Symbols, 0, 1});
        k.setPrior(1.0);
        kindMap.put("^G-^", k);

        //equals (sigma : tau)
        k = new Kind("^G-^G", new int[]{1, l1Symbols, 1, l2Symbols});
        k.setPrior(1.0);
        kindMap.put("^G-^G", k);

        k = new Kind("$-$", new int[]{0, 1, 0, 1});
        k.setPrior(1.0);
        kindMap.put("$-$", k);

        k = new Kind("$-G$", new int[]{0, 1, 1, l2Symbols});
        k.setPrior(1.0);
        kindMap.put("$-G$", k);

        k = new Kind("G$-$", new int[]{1, l1Symbols, 0, 1});
        k.setPrior(1.0);
        kindMap.put("G$-$", k);

        k = new Kind("G$-G$", new int[]{1, l1Symbols, 1, l2Symbols});
        k.setPrior(1.0);
        kindMap.put("G$-G$", k);


    }

    // works only for 2 languages at the moment
    private void initMultiGlyphKinds(int l1GlyphCount, int l2GlyphCount) {
        int l1Symbols = 1 + l1GlyphCount;
        int l2Symbols = 1 + l2GlyphCount;

        // multiglyph alignments
        int[] ss = new int[]{(l1Symbols), (l1Symbols + (l1GlyphCount * l1GlyphCount))};
        int[] tt = new int[]{(l2Symbols), (l2Symbols + (l2GlyphCount * l2GlyphCount))};

        Kind k = new Kind(".-GG", new int[]{0, 1, tt[0], tt[1]});
        k.setPrior(1.0 / l2Symbols);
        kindMap.put(".-GG", k);

        k = new Kind("GG-.", new int[]{ss[0], ss[1], 0, 1});
        k.setPrior(1.0 / l1Symbols);
        kindMap.put("GG-.", k);

        k = new Kind("G-GG", new int[]{1, l1Symbols, tt[0], tt[1]});
        k.setPrior(1.0 / l2Symbols);
        kindMap.put("G-GG", k);

        k = new Kind("GG-G", new int[]{ss[0], ss[1], 1, l2Symbols});
        k.setPrior(1.0 / l1Symbols);
        kindMap.put("GG-G", k);

        k = new Kind("GG-GG", new int[]{ss[0], ss[1], tt[0], tt[1]});
        k.setPrior(1.0 / (l1Symbols * l2Symbols));
        kindMap.put("GG-GG", k);
    }

    public final Kind getKind(int sourceSymbolIndex, int targetSymbolIndex) {

        if(kindCache != null && kindCache[sourceSymbolIndex][targetSymbolIndex] != null) {
            return kindCache[sourceSymbolIndex][targetSymbolIndex];
        }

        Input input = Input.getInstance();
        String s = input.getVocabulary(this.lang1Index).getGlyph(sourceSymbolIndex);
        String t = input.getVocabulary(this.lang2Index).getGlyph(targetSymbolIndex);
//        System.out.println("s-t: " + s + " " + t);

        //System.out.println("s " + s + " t " + t);
        if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
            s = getWordBoundaryIncludingKindString(s);
            t = getWordBoundaryIncludingKindString(t);
            
        }else {
            s = getKindString(s);
            t = getKindString(t);
        }

        Kind k = kindMap.get(s+"-"+t);
//        System.out.println("kind: " + k);
//        System.out.println("");
        if (kindCache != null) {
            kindCache[sourceSymbolIndex][targetSymbolIndex] = k;
        }
        return k;
    }

    private String getKindString(String s) {
        if(StringUtils.getGlyphComboLength(s) == 1) {
            if(!".".equals(s)) {
                s = "G";
            }
        } else {
            s = "GG";
        }

        return s;
    }

    private String getWordBoundaryIncludingKindString(String s) {
        if(StringUtils.getGlyphComboLength(s) == 1) {
            if(!(".".equals(s) || "^".equals(s) || "$".equals(s))) {
                s = "G";
            }
        } else {
            if (s.contains("^")) {
                s = "^G";
            }else if (s.contains("$")) {
                s = "G$";
            }else {
                s = "GG";
            }
        }

        return s;
        
    }

    public Kind getKind(int... symbolIdxs) {
        if(symbolIdxs.length != 2) {
            throw new IllegalArgumentException("Illegal symbol idx amount.");
        }

        return getKind(symbolIdxs[0], symbolIdxs[1]);
    }
}
