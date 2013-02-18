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

import java.lang.reflect.Array;
import java.util.Collection;

/**
 *
 * @author avihavai
 */
public class Matrix<T> {

    private T[] content;    
    private int[] dimensions;

    public Matrix(Class<T> classType, Collection<Integer> indexes) {
        this(classType, CollectionUtil.toIntArray(indexes));
    }

    public Matrix(Class<T> classType, int... dimensions) {
        this.content = (T[]) Array.newInstance(classType, dimensions);
        this.dimensions = dimensions;
    }

    public T getCell(Collection<Integer> indexes) {
        return getCell(CollectionUtil.toIntArray(indexes));
    }

    public T getCell(int... indexes) {
        if(indexes.length != dimensions.length) {
            throw new IllegalArgumentException("Trying to get cell with wrong arguments -- Matrix dimension differs from arguments.");
        }

        T[] arr = content;

        for (int indexId = 0; indexId < indexes.length - 1; indexId++) {
            int indexValue = indexes[indexId];
            if(indexValue < 0 || indexValue >= getDimensionLength(indexId)) {
                throw new IllegalArgumentException("Bad indexes given");
            }

            arr = (T[]) arr[indexValue];
        }

        int indexValue = indexes[indexes.length - 1];
        return arr[indexValue];
    }

    public int getDimensionCount() {
        return dimensions.length;
    }

    public int getDimensionLength(int dimensionId) {
        return dimensions[dimensionId];
    }

    public void setCell(T cellValue, Collection<Integer> indexes) {
        setCell(cellValue, CollectionUtil.toIntArray(indexes));
    }

    public void setCell(T cellValue, int... indexes) {
        if (indexes.length != dimensions.length) {
            throw new IllegalArgumentException("Trying to set cell with wrong arguments -- Matrix dimension differs from arguments.");
        }

        T[] arr = content;

        for (int indexId = 0; indexId < indexes.length - 1; indexId++) {
            int indexValue = indexes[indexId];

            if(indexValue < 0 || indexValue >= getDimensionLength(indexId)) {
                throw new IllegalArgumentException("Bad indexes given");
            }

            arr = (T[]) arr[indexValue];
        }

        int indexValue = indexes[indexes.length - 1];
        if (indexValue < 0 || indexValue >= getDimensionLength(indexes.length - 1)) {
            throw new IllegalArgumentException("Bad indexes given");
        }

        arr[indexValue] = cellValue;
    }
}
