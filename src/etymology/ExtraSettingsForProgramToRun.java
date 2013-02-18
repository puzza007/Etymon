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
package etymology;

import etymology.align.Alignator;
import etymology.config.Configuration;
import etymology.context.FeatureTree;
import etymology.output.TreeGraphPrinter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author sxhiltun
 */
public class ExtraSettingsForProgramToRun {
    
     public static List<List<String>> setSuspiciousWordPairs() {
        List<List<String>> suspiciousWordPairs = new ArrayList<List<String>>();
        suspiciousWordPairs.add(new ArrayList<String>());
        suspiciousWordPairs.get(0).add("kuiva");
        suspiciousWordPairs.get(0).add(".kuiv");

        suspiciousWordPairs.add(new ArrayList<String>());
        suspiciousWordPairs.get(1).add("kuul.e");
        suspiciousWordPairs.get(1).add(".kuule");

        suspiciousWordPairs.add(new ArrayList<String>());
        suspiciousWordPairs.get(2).add(".kumo");
        suspiciousWordPairs.get(2).add("gǫm.o");
        System.out.println(suspiciousWordPairs);
        return suspiciousWordPairs;
    }
     
    public static void printTreeGraphs(Alignator alignator, int iter) throws IOException {
        System.out.println("\nPrinting tree graphs...");
        List<FeatureTree> trees = alignator.getFeatureAlignmentMatrix().getTrees();
        TreeGraphPrinter tgp = new TreeGraphPrinter(trees, (int) alignator.getFinalCost());
    }
    
    
    
    private static void multiRun() {
        // initiate loggers
        for (String l1 : Main.okLangs) {
            Configuration.getInstance().setLanguages(Arrays.asList(new String[]{l1.toUpperCase(), "UGR"}));
            try {                
                Main.main(new String[0]);
            } catch (Exception e) {
            }
        }

        for (String l1 : Main.okLangs) {
            Configuration.getInstance().setLanguages(Arrays.asList(new String[]{"UGR", l1.toUpperCase()}));
            try {
                Main.main(new String[0]);
            } catch (Exception e) {
            }
        }
    }

    public static void fuzzedMultiRun() {
        // initiate loggers
        Configuration config = Configuration.getInstance();

        config.setFuzzUpFinnish(true);
        config.setLanguages(Arrays.asList(new String[]{"EST", "MAN"}));


        for(double d = 0.0; d < 0.95; d+= 0.05) {
            config.setFuzzificationProb(d);
            try {
                Main.main(new String[0]);
            } catch (Exception e) {
            }
        }
    }
    
}
