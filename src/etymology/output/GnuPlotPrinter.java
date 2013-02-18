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

import etymology.align.Alignator;
import etymology.config.Configuration;
import etymology.align.AlignmentMatrix;
import etymology.align.matrices.MarginalAlignmentMatrix;
import etymology.align.matrices.TwoLangAlignmentMatrix;
import etymology.input.GlyphVocabulary;
import etymology.input.Input;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author avihavai
 */
public class GnuPlotPrinter {
    private static final String[] XYZ = new String[]{"x", "y", "z"};
    
    private PrintWriter writer;

    private String filenamePrefix;

    private boolean showZeroCounts = true;

    public static String logPath;

    public static void printGnuPlotData() {
        if (Configuration.getInstance().getIterationNumber() != 0) {
            return;
        }
        if(Configuration.getInstance().isUseFeatures() || Configuration.getInstance().isFirstBaselineThenContext()) {
            return;
        }

        try {
            GnuPlotPrinter printer = new GnuPlotPrinter();
            printer.print();
        } catch (IOException ex) {
            Logger.getLogger(Alignator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private GnuPlotPrinter() throws IOException {
        this("");
    }

    private GnuPlotPrinter(String filenamePrefix) throws IOException {
        this.filenamePrefix = filenamePrefix;
    }

    private void print() throws FileNotFoundException, IOException {
        this.writer = new PrintWriter(getPlotFilename(true));
        printGnuPlotCommand();
        writer.flush();
        writer.close();

        writer = new PrintWriter(getDataFilename(true));
        printData();
        writer.flush();
        writer.close();

        if(showZeroCounts) {
           showZeroCounts = false;


           this.filenamePrefix += "no-zero-counts-";
           print();
        }
    }

    private String determineFilename(boolean usePath) {
        Configuration config = Configuration.getInstance();

        String res = config.getAlignmentType().toString().toLowerCase() + "-" + config.getCostFunctionIdentifier().toString().toLowerCase() + "-" + new File(config.getInputFile()).getName() + "-" + config.getLogFileNamePrefix();
        res = res.toLowerCase();
        if(filenamePrefix != null) {
            res = filenamePrefix + res;
        }

        if(usePath && logPath != null) {
            res = logPath + res;
        }        

        return res;
    }

    private String getDataFilename(boolean usePath) {
        return determineFilename(usePath) + "gp.dat";
    }

    private String getPlotFilename(boolean usePath) {
        return determineFilename(usePath) + "plot.gp";
    }

    private void printGnuPlotCommand() {
        Input input = Alignator.getInstance().getInput();
        printHeader();
        printAxes();

        if(input.getNumOfLanguages() == 3) {
            //w("splot \"" + getDataFilename(false) + "\" using 1:2:3:(sqrt($4/20)) with points pt 7 ps variable");
            w("splot \"" + getDataFilename(false) + "\" using 1:2:3:(($4/20)**0.3333) with points pt 7 ps variable");
            
        } else {
            w("plot \"" + getDataFilename(false) + "\" using 1:2:(sqrt($3/20)) with points pt 7 ps variable");
        }        
    }

    private void printHeader() {
        Input input = Alignator.getInstance().getInput();

        String tics = "";
        for (int i = 0; i < input.getNumOfLanguages(); i++) {
            tics += XYZ[i] + "tics ";
        }
        w("set grid " + tics + " linewidth 0.2");
        w("set border");

        for (int i = 0; i < input.getNumOfLanguages(); i++) {
            w("set " + XYZ[i] + "label \"" + input.getVocabulary(i).getLanguage() + "\"");
        }

        w("set ticslevel 0.04");
        w("unset key");
        w("");
    }

    private void printAxes() {
        Input input = Alignator.getInstance().getInput();

        // write axis tics
        for (int langId = 0; langId < input.getNumOfLanguages(); langId++) {
            GlyphVocabulary voc = input.getVocabulary(langId);

            w("# " + voc.getLanguage());
            w("set " + XYZ[langId] + "tics ( \\");

            for (int glyphId = 0; glyphId < voc.getSize() - 1; glyphId++) {
                w("\"" + voc.getGlyph(glyphId) + "\" " + glyphId + ", \\");
            }
            w("\"" + voc.getGlyph(voc.getSize() - 1) + "\" " + (voc.getSize() - 1) + ")");
        }
    }

    private void printData() throws IOException {
        if(Configuration.getInstance().getLanguages().size() > 2) {
            printThreeLangData();
        } else {
            printTwoLangData();
        }
    }

    private void printTwoLangData() {
        AlignmentMatrix alignmentMatrix = Alignator.getInstance().getAlignmentMatrix();
        Input input = Alignator.getInstance().getInput();

        double probSum = 0.0;
        //for (int g1 = 0; g1 < alignmentMatrix.getL1SymbolCount(); g1++) {
          //  for (int g2 = 0; g2 < alignmentMatrix.getL2SymbolCount(); g2++) {
        for (int g1 = 0; g1 < input.getVocabulary(0).getSize(); g1++) {
            String s1 = input.getVocabulary(0).getGlyph(g1);

            for (int g2 = 0; g2 < input.getVocabulary(1).getSize(); g2++) {
                String s2 = input.getVocabulary(1).getGlyph(g2);

                if(g1 == 0 && g2 == 0) {
                    if(Configuration.getInstance().getLanguages().size() == 2) {
                        continue;
                    }
                }

                if (Configuration.getInstance().isTakeStartsAndEndsIntoAccount()) {
                    if (s1.contains("^") ^ s2.contains("^")) {
                        continue;
                    }
                    if (s1.contains("$") ^ s2.contains("$")) {
                        continue;
                    }

                }

                double count = alignmentMatrix.getAlignmentCountAtIndex(g1, g2);
                
                probSum += alignmentMatrix.getAlignmentProbabilityByIndex(g1, g2);
                if (count <= 0) {
                    continue;
                }

                count = Math.round(count * 1000) / 1000.0;
                w(g1 + "\t" + g2 + "\t" + count);

            }
        }

        System.out.println("Probability sum for 2 langs: " + probSum);
    }

    private void printThreeLangData() {
        AlignmentMatrix alignmentMatrix = Alignator.getInstance().getAlignmentMatrix();
        int alignmentCounts = 0;

        MarginalAlignmentMatrix mam = (MarginalAlignmentMatrix) alignmentMatrix;
        for (TwoLangAlignmentMatrix tlam : mam.getMatrices()) {
            alignmentCounts += tlam.getNumberOfWords(); // #-# alignments

            for (int source = 0; source < tlam.getL1SymbolCount(); source++) {
                for (int target = 0; target < tlam.getL2SymbolCount(); target++) {
                    alignmentCounts += tlam.getAlignmentCountAtIndex(source, target);
                }
            }
        }

        System.out.println("Total " + alignmentCounts + " alignments.");
        double probSum = 0;

        for (int g1 = 0; g1 < alignmentMatrix.getL1SymbolCount(); g1++) {
            for (int g2 = 0; g2 < alignmentMatrix.getL2SymbolCount(); g2++) {
                for (int g3 = 0; g3 < alignmentMatrix.getL3SymbolCount(); g3++) {
                    if(!showZeroCounts && (g1 == 0 || g2 == 0 || g3 == 0)) {
                        continue;
                    }

                    if(g1 == 0 && g2 == 0 && g3 == 0) {
                        continue;
                    }

                    double prob = alignmentMatrix.getDotToDotAllowedAlignmentProbabilityByIndex(g1, g2, g3);
                    if (prob <= 0) {
                        continue;
                    }

                    probSum += prob;
                }
            }
        }
        
        System.out.println("Probability sum: " + probSum);


        for (int g1 = 0; g1 < alignmentMatrix.getL1SymbolCount(); g1++) {
            for (int g2 = 0; g2 < alignmentMatrix.getL2SymbolCount(); g2++) {
                for (int g3 = 0; g3 < alignmentMatrix.getL3SymbolCount(); g3++) {
                    if(!showZeroCounts && (g1 == 0 || g2 == 0 || g3 == 0)) {
                        continue;
                    }

                    if(g1 == 0 && g2 == 0 && g3 == 0) {
                        continue;
                    }

                    double prob = alignmentMatrix.getDotToDotAllowedAlignmentProbabilityByIndex(g1, g2, g3);
                    if (prob <= 0) {
                        continue;
                    }

                    double weight = alignmentCounts * (prob / probSum);
//                    if (g2 == alignmentMatrix.getL2SymbolCount()-1) {
//                        System.out.println("g2: " + weight);
//                    }

                    // weight *= 100;
                    //try: weight < [0.9, 1, ..] !!!
                    if(weight < 0.5) {
                        continue;
                    }


                    w(g1 + "\t" + g2 + "\t" + g3 + "\t" + weight);
                }
            }
        }
    }

    private void w(String line) {
        writer.write(line + "\n");
    }
}
