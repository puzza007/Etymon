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
package etymology.cost;

import etymology.align.AlignmentMatrix;

/**
 *
 * @author avihavai
 */
public class TwoPartCodeCostNoKindsUniformPrior extends TwoPartCodeCostFunction {

    @Override
    public String getName() {
        return "2P CODECOST - NO-KINDS - EQ2";
    }

    @Override
    public double getCodebookCost(AlignmentMatrix matrix) {
        return super.getCodebookCostNoKinds(matrix.getAlignmentCountMatrix());
    }

    @Override
    public double getConditionalCost(AlignmentMatrix matrix) {
         return super.getConditionalCostTwoPartCodeNoKinds(matrix.getAlignmentCountMatrix(), matrix.getNumberOfWords());
    }
}
