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
package etymology.viterbi;

import etymology.align.Alignator;
import etymology.util.StringUtils;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

/**
 *
 * @author arto
 */
public class ViterbiCell implements Comparable<ViterbiCell> {
    private static final DecimalFormat DF = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.US));


    private static int CELL_COUNT = 0;
    private int id;
    private ViterbiCell parent;
    private double costDifference;
    private double cost;
    private String parentDirection;
    private double suffixAlignmentCost;
    private List<Integer> sourceSuffix;
    private List<Integer> targetSuffix;

//    public static final Comparator<ViterbiCell> SUFFIX_COST_COMPARATOR
//     = new Comparator<ViterbiCell>() {
//
//      @Override
//      public int compare(ViterbiCell v1, ViterbiCell v2) {
//         return Double.compare(v1.getSuffixAlignmentCost(), v2.getSuffixAlignmentCost());
//      }
//
//    };

    Map<Integer, Integer> languageSpecificGlyphIndexes;


    public static ViterbiCell createRoot() {
        CELL_COUNT = 0;
        return new ViterbiCell();
    }

    private ViterbiCell() {
        this(null, 0.0);
    }

    public ViterbiCell(ViterbiCell parent, double cost) {
        this(parent, cost, null);
    }


    public ViterbiCell(ViterbiCell parent, double cost, Map<Integer, Integer> languageToGlyphIndexes) {
        this.parent = parent;
        this.id = CELL_COUNT++;

        this.sourceSuffix = null;
        this.targetSuffix = null;

        if (cost < 0) {
            System.out.println("NEGATIVE COST");
            System.exit(-1);
        }

        this.cost = cost;

        if(languageToGlyphIndexes != null) {
            this.languageSpecificGlyphIndexes = languageToGlyphIndexes;
        } else {
            this.languageSpecificGlyphIndexes = new HashMap();
        }
    }


    public Stack<ViterbiCell> getPathToStart() {
        ViterbiCell current = this;

        Stack<ViterbiCell> path = new Stack();
        while(current.getParent() != null) {
            path.add(current);
            current = current.getParent();
        }

        return path;
    }

    public ViterbiCell getParent() {
        return parent;
    }

    public void setSourceSuffix(List<Integer> suffix) {
        this.sourceSuffix = suffix;
    }

    public void setTargetSuffix(List<Integer> suffix) {
        this.targetSuffix = suffix;
    }

    public List<Integer> getSourceSuffix() {
        return this.sourceSuffix;
    }

    public List<Integer> getTargetSuffix() {
        return this.targetSuffix;
    }
//    public void setSuffixAlignmentCost(double suffixAlignmentCost) {
//        this.suffixAlignmentCost = suffixAlignmentCost;
//    }
//
//    public double getSuffixAlignmentCost() {
//        return this.suffixAlignmentCost;
//    }

    public double getCostDifference() {
        return costDifference;
    }

    public void setCostDifference(double costDifference) {
        this.costDifference = costDifference;
    }

    public int getId() {
        return id;
    }

    @Override
    public int compareTo(ViterbiCell o) {
        //originally: return Double.compare(cost, o.cost);
        int costComparison = Double.compare(cost, o.cost);
        return costComparison;
  //      if (costComparison != 0) {
    //        return costComparison;
      //  }
        
        //if cost is equal, compare source glyphs
//        int glyphComparison = getGlyph(0).compareTo(o.getGlyph(0));
  //      if (glyphComparison != 0) {
    //        return glyphComparison;
      //  }
        
        //if source glyphs equal, compare target glyphs
      //  return getGlyph(1).compareTo(o.getGlyph(1));
        
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    @Override
    public String toString() {
        return toAlignCharString() + " cost:" + cost;
    }

    public String toAlignCharString() {
        if (parentDirection != null) {
            return parentDirection;
        }
        return "";

        /*StringBuilder sb = new StringBuilder("( ");
        for(int languageId: Alignator.getInstance().getInput().getLanguageIds()) {
            String glyph = getGlyph(languageId);
            if(glyph == null) {
                glyph = "-";
            }

            if(this.parent == null) {
                glyph = "#";
            }

            sb.append(StringUtils.rightAlign(2, glyph));
            sb.append(" ");
        }
        
        sb.append(")");
        return sb.toString();*/
    }

    public String toViterbiMatrixCellString() {
        return getCostString() + " " + toAlignCharString();
    }

    public String getCostString() {
        return DF.format(getCost());
    }

    public String getPathString() {
        return ((this.parent == null) ? "r" : parent.getId()) + ":" + getId();
    }

    public void setParentDirection(String parentDirection) {
        this.parentDirection = parentDirection;
    }

    public void setGlyph(int languageId, String glyph) {
        int glyphIdx = Alignator.getInstance().getInput().getVocabulary(languageId).getGlyphIndex(glyph);
        setGlyphIdx(languageId, glyphIdx);
    }

    public final void setGlyphIdx(int languageId, int glyphIdx) {
        languageSpecificGlyphIndexes.put(languageId, glyphIdx);
    }

    public int getGlyphIdx(int languageId) {
        return languageSpecificGlyphIndexes.get(languageId);
    }
    
    public String getGlyph(int languageId) {
        Integer glyphIdx = languageSpecificGlyphIndexes.get(languageId);
        if(glyphIdx == null) {
            return null;
        }

        return Alignator.getInstance().getInput().getVocabulary(languageId).getGlyph(glyphIdx);
    }
}
