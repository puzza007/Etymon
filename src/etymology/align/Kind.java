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

import etymology.config.Configuration;
import etymology.util.EtyMath;

/**
 *
 * @author arto
 */
public class Kind {
    public String name;
    int[] region;
    
    private int regionSize;
    private int numOfNonZeroEvents;
    public double codebookCost = 0.0;
    private boolean boundaryKind;
    private Double prior;

    public Kind(String name, int[] region) {
        this.numOfNonZeroEvents = 0;
        this.name = name;
        this.region = region;
        this.boundaryKind = false;
//        System.out.println("KIND INITIATED: " + name);
//        System.out.println("REGION: X: " + region[0] + "-" + region[1] + ", Y: " + region[2] + "-" + region[3]);

        // 2d
        regionSize = (region[1] - region[0]) * (region[3] - region[2]);
        if(name.contains("#")) {
            regionSize = 1;
        }
        else if(region.length == 6) {
//            System.out.println("Class Kind: ");
//            System.out.println("Region length 6");
//            System.out.println("Name: " + name);
            regionSize *= (region[5] - region[4]);
        }
        if (name.contains("^")) {
            boundaryKind = true;
        }

    }

    private String regionString;
    public String getRegion() {
        if (regionString == null) {
            regionString = "";
//            regionString = "(" + region[0] + ", " + region[1] + "), (" + region[2] + ", " + region[3] + ")";
//
//            if(region.length == 6) {
//                regionString += ", (" + region[4] + ", " + region[5] + ")";
//            }

            regionString += "\t size: " + getRegionSize();
        }

        return regionString;
    }

    public boolean isBoundaryKind() {
        return boundaryKind;
    }

    public Double getPrior() {
        return prior;
    }

    public void setPrior(Double prior) {
        this.prior = prior;
    }

    public void setNumOfNonZeroEvents(int numOfNonZeroEvents) {
        this.numOfNonZeroEvents = numOfNonZeroEvents;
    }

    public int getRegionSize() {
        return regionSize;
    }

    public void increaseNumOfNonZeroEvents() {
        numOfNonZeroEvents++;
    }
    
    public void decreaseNumOfNonZeroEvents() {
        numOfNonZeroEvents--;
    }

    public int getNumOfNonZeroEvents() {
        return numOfNonZeroEvents;
    }

    public double getRegionCost() {
        // regionSize = N_k, size of the kind k
        // getNumOfNonZeroEvents = M_k; non-zero-count events
        return EtyMath.base2Log(regionSize + 1) +
                EtyMath.base2LogBinomial(regionSize, getNumOfNonZeroEvents());
    }
    
    //for 2D model
    public boolean inRegion(int x, int y) {
        if (region[1] > x && region[0] <= x) {
            if(region[3] > y && region[2] <= y) {
                return true;
            }
        }

        return false;
    }

    public boolean allInRegion(int x, int y, int z) {
        if(!inRegion(x, y)) {
            return false;
        }
        
        return (region[5] > z && region[4] <= z);
    }

    //for 3D model
    public boolean inRegion(Integer x, Integer y, Integer z) {
        if(x != null && (region[1] <= x || region[0] > x)) {
            return false;
        }

        if(y != null && (region[3] <= y || region[2] > y)) {
            return false;
        }

        if(z != null && (region[5] <= z || region[4] > z)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder area = new StringBuilder();
        for(int regionVar: region) {
            area.append(" ").append(regionVar);
        }

        return name +":"+ area.toString();
    }
}
