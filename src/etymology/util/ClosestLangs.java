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
package etymology.util;

import etymology.output.LxLMatrixBuilder;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 *
 * @author avihavai
 */
public class ClosestLangs {

    public static void main(String[] args) throws Exception {
        String logFolder = "/home/group/langtech/Etymology-Project/etymon-logs/codebook-no-kinds";
        LxLMatrixBuilder m = new LxLMatrixBuilder(logFolder, LxLMatrixBuilder.CostIdentifier.MIN);
        Map<String, PriorityQueue<LanguageDistance>> closestLanguages = new TreeMap();


        String category = "1x1-simann";

        for (String langOne : m.getLanguages()) {
            for (String langTwo : m.getLanguages()) {
                if (langOne.equals(langTwo)) {
                    continue;
                }

                String langKey = m.getKey(langOne, langTwo);

                if (m.getTotalWordPairs(category, langKey) < 100) {
                    continue;
                }


                double cost = m.getAverageCost(category, langKey);

                if(!closestLanguages.containsKey(langOne)) {
                    closestLanguages.put(langOne, new PriorityQueue());
                }

                closestLanguages.get(langOne).add(new LanguageDistance(langTwo, cost));
            }
        }

        System.out.println("Closest languages sorted by their cost (1x1 with simulated annealing) :");
        for (String lang : closestLanguages.keySet()) {
            System.out.println(lang);
            while(!closestLanguages.get(lang).isEmpty()) {
                System.out.println("\t" + closestLanguages.get(lang).poll());
            }
        }
    }
}

class LanguageDistance implements Comparable<LanguageDistance> {

    private final String lang;
    private final Double avgCost;

    public LanguageDistance(String lang, Double avgCost) {
        this.lang = lang;
        this.avgCost = avgCost;
    }

    public String getLang() {
        return lang;
    }

    public Double getAvgCost() {
        return avgCost;
    }

    @Override
    public String toString() {
        return lang + " (" + avgCost + ")";
    }

    public int compareTo(LanguageDistance t) {
        return avgCost.compareTo(t.getAvgCost());
    }
}
