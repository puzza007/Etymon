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

package etymology.align;

import java.util.Collection;

/**
 *
 * @author avihavai
 */
public interface IAlignmentMatrix {
    public double[] incrementAlignCount(Integer... glyphIdxs);
    public void decrementAlignCount(Integer... glyphIdxs);
    public void decrementAlignCountByDeterminedCosts(double[] costs, Integer... glyphIdxs);
    public double getAlignmentCountAtIndex(Integer... glyphIdxs);

    public double getAlignmentProbabilityByIndex(Integer... glyphIdxs);
    public int getTotalAlignmentCounts();
    public int getTotalAlignmentAlphas();
    public int getLanguageCount();
    public int getSymbolCount(int languageId);

    public Kind getKind(Integer... glyphIdxs);
    public Collection<Kind> getAllKinds();

    public void resetCache();
}
