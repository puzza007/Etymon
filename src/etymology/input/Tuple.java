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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author avihavai
 */
public class Tuple<E1, E2> {

    private final E1 first;
    private final E2 second;

    public Tuple(E1 first, E2 second) {
        if(first == null || second == null) {
            throw new IllegalArgumentException("Cannot build tuple from null values");
        }

        this.first = first;
        this.second = second;
    }

    public E1 getFirst() {
        return first;
    }

    public E2 getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "(" + getFirst() + ", " + getSecond() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if(super.equals(obj)) {
            return true; // same reference
        }

        if(!(obj instanceof Tuple)) {
            return false;
        }

        Tuple other = (Tuple) obj;
        if(!getFirst().equals(other.getFirst())) {
            return false;
        }

        if(!getSecond().equals(other.getSecond())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.first != null ? this.first.hashCode() : 0);
        hash = 97 * hash + (this.second != null ? this.second.hashCode() : 0);
        return hash;
    }

    public static List<Tuple> getAsPairs(Collection<Integer> dataCollection) {
        List<Integer> dataList = new ArrayList(dataCollection);

        List<Tuple> pairs = new ArrayList();
        for(int first = 0; first < dataList.size() - 1; first++) {
            for(int second = first + 1; second < dataList.size(); second++) {
                pairs.add(new Tuple(dataList.get(first), dataList.get(second)));
            }
        }

        return pairs;
    }
}
