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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 *
 * @author avihavai
 */
public class CostReader {

    static int iteration = 0;

    public static void main(String[] args) throws FileNotFoundException {
        //String file = "/group/home/langtech/Etymology-Project/etymon-logs/2-lang-baseline-eq1/two_lang-starling.utf8-fin-est-1x1-simann-data.log";
        String file = "/home/sxhiltun/sxhiltun/ety2/log/context_based-starling.utf8-fin-fin-1x1-simann-0.995-rebuildingfreq-fulliteration-data.log";
        Scanner sc = new Scanner(new File(file));
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            //handleLine(line);
            handleContextLogLine(line);
        }
    }

    private static void handleLine(String line) {
        if (line.trim().isEmpty()) {
            return;
        }

        if(line.contains("ITERATION: ")) {
            line = line.replaceAll(",", ".");
            int iter = Integer.parseInt(line.substring("ITERATION: ".length()));
            if(iter < iteration) {
                System.exit(0);
            }

            iteration = iter;
        }

        if(line.contains("COST")) {
            String cost = line.split("\\s+")[3];
            System.out.println(iteration + "\t" + cost);
        }

    }

    private static void handleContextLogLine(String line) {
        if (line.trim().isEmpty()) {
            return;
        }

        if(line.contains("ITERATION: ")) {
            line = line.replaceAll(",", ".");
            String iterNum = line.substring("ITERATION: ".length());
            iterNum = iterNum.replaceAll("\\W", "");
            int iter = Integer.parseInt(iterNum);
//            if(iter < iteration) {
//                System.exit(0);
//            }

            iteration = iter;
        }

        if(line.contains("COST OF TREES ")) {
            String cost = line.split("\\s+")[3];
            System.out.println(iteration + "\t" + cost);
        }

    }
}
