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

/**
 *
 * @author avihavai
 */
public class Arrows {
    public static final String NORTH_WEST_DOUBLE_ARROW = "\u21d6";
    public static final String WEST_DOUBLE_ARROW = "\u21d0";
    public static final String NORTH_DOUBLE_ARROW = "\u21d1";
    public static final String NORTH_WEST_ARROW = "\u2196";
    public static final String WEST_ARROW = "\u2190";
    public static final String NORTH_ARROW = "\u2191";


    public static String getDirection(int xDifference, int yDifference) {
        if(xDifference == yDifference) {
            if(xDifference == 0) {
                return " . ";
            } else if (xDifference == 1) {
                return " " + NORTH_WEST_ARROW + " ";
            } else if (xDifference == 2) {
                return " " + NORTH_WEST_DOUBLE_ARROW + " ";
            }
        }

        return getNorthWiseCorrectArrow(xDifference) + " " + getWestWiseCorrectArrow(yDifference);
    }


    private static String getNorthWiseCorrectArrow(int xDifference) {
        if(xDifference == 0) {
            return "";//.";
        }

        if(xDifference == 1) {
            return Arrows.NORTH_ARROW;
        }

        if(xDifference == 2) {
            return Arrows.NORTH_DOUBLE_ARROW;
        }

        return null;
    }


    private static String getWestWiseCorrectArrow(int yDifference) {
        if(yDifference == 0) {
            //return ".";
            return "";
        }

        if(yDifference == 1) {
            return Arrows.WEST_ARROW;
        }

        if(yDifference == 2) {
            return Arrows.WEST_DOUBLE_ARROW;
        }

        return null;
    }
}
