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
package etymology.output;

import etymology.config.Configuration;
import etymology.config.Constants;
import etymology.align.Alignator;
import etymology.align.WordAlignment;
import etymology.align.AlignmentMatrix;
import etymology.align.AlignmentRegistry;
import etymology.align.Kind;
import etymology.align.matrices.MarginalAlignmentMatrix;
import etymology.align.matrices.TwoLangAlignmentMatrix;
import etymology.input.FeatureVocabulary;
import etymology.input.GlyphVocabulary;
import etymology.cost.CostFunction;
import etymology.cost.TwoPartCodeCostFunction;
import etymology.input.Input;
import etymology.util.StringUtils;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author arto
 */
public class AlignmentPrinter {

    private Input input;

    public AlignmentPrinter(AlignmentMatrix matrix, Input input) {
        this.input = input;
    }

    public String getPrintableAlignmentMatrix(AlignmentMatrix matrix) {

        if (Configuration.getInstance().isUseFeatures()) {
            return getTwoLangFeatureAlignmentMatrix(matrix);
        }
        
        StringBuilder nnzEvents = new StringBuilder("Total non-zero events: " + matrix.getNumberOfNonZeroAlignments() + "\n");
        nnzEvents.append("Kind specific:\n");
        for (Kind k : matrix.getAllKinds()) {
            nnzEvents.append("\t").append(k.name).append(" ").append(k.getRegion());
            nnzEvents.append(" : ").append(k.getNumOfNonZeroEvents()).append("\n");

        }


        if (input.getNumOfLanguages() == 2) {
            if (Configuration.getInstance().isRemoveSuffixes()) {
                return getTwoLangAlignmentMatrix(matrix) + getSuffixMatrices(matrix);
            }

            return getTwoLangAlignmentMatrix(matrix)  + nnzEvents.toString();
        }

        if(matrix instanceof MarginalAlignmentMatrix) {
            MarginalAlignmentMatrix mam = (MarginalAlignmentMatrix) matrix;
            StringBuilder sb = new StringBuilder();

            for(TwoLangAlignmentMatrix tlam: mam.getMatrices()) {
                sb.append("Languages: ").append(tlam.getAlignedLanguages()).append("\n");

                CostFunction cf = Alignator.getCostHandler().getCostFunction();
                double cost = cf.getCost(tlam);
                sb.append("COST: ").append(Constants.COST_FORMAT.format(cost)).append("\n");

                if(cf instanceof TwoPartCodeCostFunction) {
                    TwoPartCodeCostFunction tpcf = (TwoPartCodeCostFunction) cf;
                    sb.append(" (BOOK) ").append(Constants.COST_FORMAT.format(tpcf.getCodebookCost(tlam))).append("\n");
                    sb.append(" (COND) ").append(Constants.COST_FORMAT.format(tpcf.getConditionalCost(tlam))).append("\n");
                }

                //Print the actural matrice
                sb.append(getTwoLangAlignmentMatrix(tlam));
                //Print other stuff
                sb.append("Total non-zero events: ");
                sb.append(tlam.getNumberOfNonZeroAlignments());
                sb.append("\n");
            }

            return sb.toString();
        }

        return getMultiLangAlignments(matrix) + nnzEvents.toString();
    }

    public String getPrintableAlignments(AlignmentRegistry alignmentRegistry) {

        StringBuilder sb = new StringBuilder();
        WordAlignment[] alignments = alignmentRegistry.getAlignments();
        for (int i = 0; i < alignments.length; i++) {
            WordAlignment alignment = alignments[i];
            if(alignment == null) {
                continue;
            }

            sb.append(alignment.getStringPresentation(input, i)).append("\n");
        }     

        return sb.toString();
    }

    private String getSuffixMatrices(AlignmentMatrix am) {
        StringBuilder ret = new StringBuilder();


        ret.append(getSuffixHeader(input.getVocabulary(am.getL1LangId()).getGlyphs(), am.getL1LangId()));
        ret.append("\n");
        ret.append(getSuffixData(am, input.getVocabulary(am.getL1LangId()), am.getL1LangId()));
        ret.append("\n");

        ret.append(getSuffixHeader(input.getVocabulary(am.getL2LangId()).getGlyphs(), am.getL2LangId()));
        ret.append("\n");
        ret.append(getSuffixData(am, input.getVocabulary(am.getL2LangId()), am.getL2LangId()));
        ret.append("\n");
        return ret.toString();
    }

    private String getSuffixHeader(Collection<String> headerData, int lang) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nSuffix events: ").append(input.getLanguage(lang)).append("\n");
        for (String s : headerData) {
            sb.append(s).append("    ");
            if (s.length() == 5) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    private String getSuffixData(AlignmentMatrix am, GlyphVocabulary vocabulary, int languageId) {
        StringBuilder ret = new StringBuilder();

        for (String glyph : vocabulary.getGlyphs()) {
            int num = am.getSuffixAlignmentMatrix(languageId).getAlignmentCountAtIndex(vocabulary.getGlyphIndex(glyph));
            int numLength = String.valueOf(num).length();
            char[] spaces = new char[5-numLength];
            Arrays.fill(spaces, ' ');
            ret.append(num);
            ret.append(spaces);
            
        }

        return ret.toString();
    }
    
    /*
    private String getTwoLangAlignmentMatrix(AlignmentMatrix am) {
        StringBuilder ret = new StringBuilder();

        if (Configuration.getInstance().isRemoveSuffixes()) {

            ret.append(getHeader(input.getVocabulary(am.getL2LangId()).getGlyphs())).append("\n");
            for (String sourceGlyph : input.getVocabulary(am.getL1LangId()).getGlyphs()) {
                ret.append(getRow(input, am, sourceGlyph)).append("\n");
            }

        } else {
            //First line of the matrix
            ret.append(getHeader(input.getVocabulary(am.getL2LangId()).getSingleLengthGlyphs())).append("\n");
            //System.out.println(ret);

            //second line of the matrix
            ret.append(getRow(input, am, ".")).append("\n");
            //System.out.println(ret);
            
            //rest of the matix
            //iterate over the source language
            for (String sourceGlyph : input.getVocabulary(am.getL1LangId()).getSingleLengthGlyphs()) {
                ret.append(getRow(input, am, sourceGlyph)).append("\n");
            }
        }

        Map<Integer, Set<String>> multiGlyphAlignments = getMultiGlyphAlignments(am);
        
        for (int key : multiGlyphAlignments.keySet()) {
            StringBuilder sb = new StringBuilder();
            for (String value : multiGlyphAlignments.get(key)) {
                sb.append("\t: [").append(value).append("]\n");
            }

            ret.append("   ").append(key).append(sb.toString());
        }

        //System.out.println(ret.toString());
        return ret.toString();
    }
    */
    
    private String getTwoLangAlignmentMatrix(AlignmentMatrix am) {
        StringBuilder ret = new StringBuilder();

        if (Configuration.getInstance().isRemoveSuffixes()) {

            ret.append(getHeader(input.getVocabulary(am.getL2LangId()).getGlyphs())).append("\n");
            for (String sourceGlyph : input.getVocabulary(am.getL1LangId()).getGlyphs()) {
                ret.append(getRow(input, am, sourceGlyph)).append("\n");
            }

        } else {
            //First line of the matrix
            
            //target lang glyph list
            input.getVocabulary(am.getL2LangId()).getSingleLengthGlyphs();
                    
            ret.append(getHeader(input.getVocabulary(am.getL2LangId()).getSingleLengthGlyphs())).append("\n");
            //System.out.println(ret);

            //second line of the matrix
            ret.append(getRow(input, am, ".")).append("\n");
            //System.out.println(ret);
            
            //rest of the matix
            //iterate over the source language
            for (String sourceGlyph : input.getVocabulary(am.getL1LangId()).getSingleLengthGlyphs()) {
                ret.append(getRow(input, am, sourceGlyph)).append("\n");
            }
        }

        Map<Integer, Set<String>> multiGlyphAlignments = getMultiGlyphAlignments(am);
        
        for (int key : multiGlyphAlignments.keySet()) {
            StringBuilder sb = new StringBuilder();
            for (String value : multiGlyphAlignments.get(key)) {
                sb.append("\t: [").append(value).append("]\n");
            }

            ret.append("   ").append(key).append(sb.toString());
        }

        //System.out.println(ret.toString());
        return ret.toString();
    }
    
    private String getTwoLangFeatureAlignmentMatrix(AlignmentMatrix matrix) {
        StringBuilder ret = new StringBuilder();
        ret.append(getTwoLangAlignmentMatrix(matrix));


        FeatureVocabulary fS = (FeatureVocabulary) input.getVocabulary(0);
        FeatureVocabulary fT = (FeatureVocabulary) input.getVocabulary(1);


        //tee tähän mäppi index -> feature
        int firstlang = input.getLengthOneGlyphCount(0);
        int secondlang = input.getLengthOneGlyphCount(1);

        ret.append("\n").append(fS.getLanguage()).append("\t\t");
        ret.append(fT.getLanguage()).append("\n");
        if (firstlang <= secondlang) {
            for (int i=1; i<=firstlang; i++) {
                ret.append(fS.getGlyph(i)).append(": ").append(fS.getFeature(i)).append("\t");
                ret.append(fT.getGlyph(i)).append(": ").append(fT.getFeature(i)).append("\n");
            }
            for (int i=firstlang+1; i<=secondlang; i++) {
                ret.append("\t\t");
                ret.append(fT.getGlyph(i)).append(": ").append(fT.getFeature(i)).append("\n");
            }
        }
        else{
            for (int i=1; i<=secondlang; i++) {
                ret.append(fS.getGlyph(i)).append(": ").append(fS.getFeature(i)).append("\t");
                ret.append(fT.getGlyph(i)).append(": ").append(fT.getFeature(i)).append("\n");
            }
            for (int i=secondlang+1; i<=firstlang; i++) {
                //ret.append(i).append(": ").append(input.getVocabulary(0).getGlyph(i)).append("\n");
                ret.append(fS.getGlyph(i)).append(": ").append(fS.getFeature(i)).append("\n");
            }
        }

        return ret.toString();




    }



    private Map<Integer, Set<String>> getMultiGlyphAlignments(AlignmentMatrix matrix) {
        Map<Integer, Set<String>> multiGlyphAlignments = new TreeMap();

        for (String s : input.getVocabulary(0).getGlyphs()) {
            for (String t : input.getVocabulary(1).getGlyphs()) {
                if (StringUtils.getGlyphComboLength(s) <= 1 && StringUtils.getGlyphComboLength(t) <= 1) {
                    continue;
                }
                int si = input.getVocabulary(0).getGlyphIndex(s);
                int ti = input.getVocabulary(1).getGlyphIndex(t);
                int count = matrix.getAlignmentCountAtIndex(si, ti);
                if (count == 0) {
                    continue;
                }

                if (!multiGlyphAlignments.containsKey(count)) {
                    multiGlyphAlignments.put(count, new TreeSet());
                }

                String gg = input.getVocabulary(0).getGlyph(si) + " " + input.getVocabulary(1).getGlyph(ti);
                multiGlyphAlignments.get(count).add(gg);
            }
        }

        return multiGlyphAlignments;
    }

    private String getMultiLangAlignments(AlignmentMatrix matrix) {
        Map<Double, Set<String>> multiglyphScores = getMultiglyphScores(matrix);
        return getAlignmentString(multiglyphScores);
    }

    private String getAlignmentString(Map<Double, Set<String>> alignments) {
        StringBuilder ret = new StringBuilder();
        int count = 0;
        
        for (Double key : alignments.keySet()) {
            StringBuilder sb = new StringBuilder();
            count += alignments.get(key).size();

            for (String value : alignments.get(key)) {
                sb.append("\t: [").append(value).append("]\n");
            }

            ret.append(key).append(sb.toString());
        }

        if (count > 0) {
            ret.append("Total ").append(count).append(" non-zero entries.\n");
        }

        return ret.toString();
    }

    private Map<Double, Set<String>> getMultiglyphScores(AlignmentMatrix matrix) {
        Map<Double, Set<String>> multiglyphScores = new TreeMap();

        for (String g1 : input.getVocabulary(0).getGlyphs()) {
            for (String g2 : input.getVocabulary(1).getGlyphs()) {
                for (String g3 : input.getVocabulary(2).getGlyphs()) {
                    int g1i = input.getVocabulary(0).getGlyphIndex(g1);
                    int g2i = input.getVocabulary(1).getGlyphIndex(g2);
                    int g3i = input.getVocabulary(2).getGlyphIndex(g3);

                    double count = matrix.getAlignmentCountAtIndex(g1i, g2i, g3i);
                    if (count == 0) {
                        continue;
                    }

                    count = Math.round(count * 1000) / 1000.0;

                    if (!multiglyphScores.containsKey(count)) {
                        multiglyphScores.put(count, new TreeSet());
                    }

                    String gg = input.getVocabulary(0).getGlyph(g1i) + " " + input.getVocabulary(1).getGlyph(g2i) + " " + input.getVocabulary(2).getGlyph(g3i);
                    multiglyphScores.get(count).add(gg);
                }
            }
        }

        return multiglyphScores;
    }

    private String getHeader(Collection<String> headerData) {
        StringBuilder sb;
        if (!Configuration.getInstance().isRemoveSuffixes()) {
            sb = new StringBuilder("     .   ");
        }else {
            sb = new StringBuilder("    ");
        }
        
        for (String s : headerData) {
            sb.append(s).append("   ");
            if (s.length() == 5) {
                sb.append("  ");
            }
        }

        return sb.toString();
    }

    private String getFeatureMatrixHeader(Collection<String> headerData) {
        StringBuilder sb = new StringBuilder("    .  ");
        for (String s : headerData) {
            int index = input.getVocabulary(1).getGlyphIndex(s);
            sb.append(index).append(" ");
            if (index < 9) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }


    

    private String getRow(Input input, AlignmentMatrix am, String sourceGlyph) {
        StringBuilder sb = new StringBuilder();
        
        //sb_temp is for my own test
        

        sb.append(sourceGlyph).append(" ");
        

        int sourceLangId = am.getL1LangId();
        int targetLangId = am.getL2LangId();

        GlyphVocabulary sourceVocabulary = input.getVocabulary(sourceLangId);
        GlyphVocabulary targetVocabulary = input.getVocabulary(targetLangId);

        int num;

        

        //first get the number correspond to "."
        if (!Configuration.getInstance().isRemoveSuffixes()) {
            num = am.getAlignmentCountAtIndex(sourceVocabulary.getGlyphIndex(sourceGlyph), targetVocabulary.getGlyphIndex("."));
            sb.append(stringWithSpaces(num));
            
            //add the count correspond to "."
            
        }

        if (Configuration.getInstance().isRemoveSuffixes()) {
            for (String targetGlyph : targetVocabulary.getGlyphs()) {
                num = am.getAlignmentCountAtIndex(sourceVocabulary.getGlyphIndex(sourceGlyph), targetVocabulary.getGlyphIndex(targetGlyph));
                sb.append(stringWithSpaces(num));
            }
            // get the number correspond to glyph pairs
        }else {
            for (String targetGlyph : targetVocabulary.getSingleLengthGlyphs()) {
                num = am.getAlignmentCountAtIndex(sourceVocabulary.getGlyphIndex(sourceGlyph), targetVocabulary.getGlyphIndex(targetGlyph));
                sb.append(stringWithSpaces(num));
                
                //add the count correspond to other glyphs
            
            }
        }

        

       
        
        return sb.toString();
    }

    private String getFeatureMatrixRow(Input input, AlignmentMatrix am, String sourceGlyph) {
        StringBuilder sb = new StringBuilder();
        int index = input.getVocabulary(0).getGlyphIndex(sourceGlyph);
        if (index == 0) {
            sb.append(". ");
        }else {
            sb.append(index).append(" ");
        }
        if (index < 10) {
            sb.append(" ");
        }
        int num = am.getAlignmentCountAtIndex(input.getVocabulary(0).getGlyphIndex(sourceGlyph), input.getVocabulary(1).getGlyphIndex("."));
        sb.append(stringWithSpaces(num));

        for (String targetGlyph : input.getVocabulary(1).getSingleLengthGlyphs()) {
            num = am.getAlignmentCountAtIndex(input.getVocabulary(0).getGlyphIndex(sourceGlyph), input.getVocabulary(1).getGlyphIndex(targetGlyph));
            sb.append(stringWithSpaces(num));
        }

        return sb.toString();
    }



    // ugly <tm>
    private static String stringWithSpaces(int forNum) {


        if (forNum == 0) { // special case
            return "   .";
        }

        if (forNum < 10) {
            return "   " + forNum;
        }

        if (forNum < 100) {
            return "  " + forNum;
        }

        return " " + forNum;
    }

    
        


}
