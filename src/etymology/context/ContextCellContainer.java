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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sxhiltun
 */
public class ContextCellContainer {

    private List<ContextCell> cells;

    public ContextCellContainer() {
        cells = new ArrayList();
    }

    public ContextCell addContextCell(int symbol, int wordIndex, int positionInWord) throws Exception {
        ContextCell c = new ContextCell(symbol, wordIndex, positionInWord);
        cells.add(c);
        return c;
    }

    public ContextCell getContextCell(int wordIndex, int positionInWord) {
        for (ContextCell c : cells) {
            if (c.getWordIndex() == wordIndex && c.getPositionInWord() == positionInWord) {
                return c;
            }
        }
        return null;
    }

    public void removeContextCell(int wordIndex, int positionInWord) {
        List<ContextCell> removeTheseCells = new ArrayList();
        for (ContextCell c : cells) {
            if (c.getWordIndex() == wordIndex && c.getPositionInWord() == positionInWord) {
                removeTheseCells.add(c);
            }
        }

        cells.removeAll(removeTheseCells);
    }

    public int getSize() {
        return this.cells.size();
    }

    public List<ContextCell> getCells() {
        return cells;
    }
}
