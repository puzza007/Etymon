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

import java.util.List;

/**
 *
 * @author avihavai
 */
public class ArrayUtil {

    public static int getNumberOfDimensions(Object object) {
        int dimensions = 0;

        Class objClass = object.getClass();
        while (objClass.isArray()) {
            dimensions++;
            objClass = objClass.getComponentType();
        }

        return dimensions;
    }

    public static int[] toIntArray(List<Integer> integerList) {
        return CollectionUtil.toIntArray(integerList);
    }
}
