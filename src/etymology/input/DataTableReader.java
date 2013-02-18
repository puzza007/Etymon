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

import etymology.config.Configuration;
import etymology.util.StringUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

/**
 *
 * @author arto
 */
public class DataTableReader {
    private Map<String, Integer> headerToColumnIndex = new HashMap();
    private List<List<String>> dataTable = new ArrayList();

    private int words = 0;

    public DataTableReader(String inputFilePath) throws FileNotFoundException, IOException {
        this(new File(inputFilePath));
    }

    public DataTableReader(File inputFile) throws FileNotFoundException, IOException {
        readData(inputFile);

        if(Configuration.getInstance().hasWordLimit()) {
            shuffleWords();
        }
    }

    private List<Integer> getNewOrder() {
        List<Integer> order = new ArrayList();
        for(int wordId = 0; wordId < words; wordId++) {
            order.add(wordId);
        }
        Collections.shuffle(order, new Random(Configuration.getInstance().getRandomSeed()));// use the same random key for each shuffle
        return order;
    }

    private List<String> switchToOrder(List<String> column, List<Integer> newOrder) {
        String[] data = new String[newOrder.size()];
        for(int i = 0; i < newOrder.size(); i++) {
            data[i] = column.get(newOrder.get(i));
        }

        return Arrays.asList(data);
    }

    private void shuffleWords() {
        List<Integer> order = getNewOrder();

        for (String header : getHeaders()) {
            List<String> wordColumn = getColumn(header);
            wordColumn = switchToOrder(wordColumn, order);
            setColumn(header, wordColumn);
        }
    }

    public final List<String> getColumn(String header) {
        return getColumn(headerToColumnIndex.get(header));
    }

    private void setColumn(String header, List<String> column) {
        int index = headerToColumnIndex.get(header);
         for(int i = 0; i < words; i++) {
            dataTable.get(i).set(index, column.get(i));
        }
    }

    public final Collection<String> getHeaders() {
        return headerToColumnIndex.keySet();
    }

    private List<String> getColumn(int index) {
        List<String> column = new ArrayList();
        for(int i = 0; i < words; i++) {
            column.add(dataTable.get(i).get(index));
        }

        return column;
    }

    public Map<String, List<String>> getWordColumns(List<String> langs, int maxLangsToMiss) {
        List<List<String>> cols = new ArrayList();
        for (String lang: langs) {
            cols.add(getColumn(lang));
        }

        cleanColumns(cols, maxLangsToMiss);

        Map<String, List<String>> languageToWordColumnMap = new TreeMap();

        List<String> usedLangs = new ArrayList();
        for(int i = 0; i < langs.size(); i++) {
            String lang = langs.get(i);
            int count = 1;

            // if we align same languages, use fin, fin-1, fin-2 etc
            while(usedLangs.contains(lang)) {
                lang = langs.get(i) + "-" + count;
                count++;
            }

            languageToWordColumnMap.put(lang, cols.get(i));
            usedLangs.add(lang);
        }

        return languageToWordColumnMap;
    }


    private void cleanColumns(List<List<String>> cols, int maxLangsToMiss) {
        for (int wordIndex = words - 1; wordIndex >= 0; wordIndex--) {
            int missedLangs = 0;
            boolean remove = false;

            for (List<String> col : cols) {
                String word = col.get(wordIndex);

                if(StringUtils.isOkWord(word)) {
                    continue;
                }

                missedLangs++;
                if (missedLangs > maxLangsToMiss) {
                    remove = true;
                    break;
                }
            }

            if (remove) {
                for (List<String> col : cols) {
                    col.remove(wordIndex);
                }
            }
        }
    }

    private void readData(File file) throws FileNotFoundException, IOException {
        Scanner s = new Scanner(StringUtils.readFileAsString(file)); // , Charset.forName("UTF8").displayName());
        String line;

        boolean headersRead = false;

        while (s.hasNextLine()) {
            line = s.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            if (!headersRead) {
                readHeaders(line);
                headersRead = true;
            } else {
                readRow(line);
            }
        }
    }

    private void readRow(String line) {
        dataTable.add(Arrays.asList(line.split("\\s+")));
        words++;
    }

    private void readHeaders(String line) {
        String[] cols = line.split("\\s+");
        for (int i = 0; i < cols.length; i++) {
            headerToColumnIndex.put(cols[i], i);
        }
    }
}
