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

import etymology.context.FeatureTreeContainer.Context;
import etymology.context.FeatureTreeContainer.Features;
import etymology.context.FeatureTreeContainer.Level;
import java.util.List;

/**
 *
 * @author avihavai
 */
public class Candidate {

    private Level level;
    private Features features;
    private Context context;
    private char value;

    private boolean binary;

    private double cost;
    private List<TreeNode> listOfChildNodesByThisCandidate;

    public Candidate(Level level, Context context, Features features) {
        this.level = level;
        this.context = context;
        this.features = features;
        this.cost = -1;
        this.value = 0;
        this.binary = false;
    }

    public Candidate(Level level, Context context, Features features, char c) {
        this.level = level;
        this.context = context;
        this.features = features;
        this.cost = -1;
        this.binary = true;
        this.value = c;
    }

    public void setBinaryCandidate(boolean isBinary) {
        binary = isBinary;
    }

    public boolean isBinary() {
        return binary;
    }

    public double getCost() {
        return this.cost;
    }
    
    public void setListOfChildNodesByThisCandidate(List<TreeNode> listOfChildNodes) {
        this.listOfChildNodesByThisCandidate = listOfChildNodes;
    }

    public List<TreeNode> getListOfChildNodesByThisCandidate()  {
        return this.listOfChildNodesByThisCandidate;
    }
    
    public void setCost(double cost) {
        this.cost = cost;
    }

    public Context getContext() {
        return context;
    }

    public Features getFeature() {
        return features;
    }

    public Level getLevel() {
        return level;
    }

    public char getValue() {
        return value;
    }

    @Override
    public String toString() {
        String charValue = "";
        if (this.binary) {
            charValue = " " + String.valueOf(value);
        }
        return level.toString() + " " + context.toString() + " " + features.toString() + charValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Candidate)) {
            return false;
        }

        Candidate cand = (Candidate) obj;
        if (this.context != cand.getContext()) {
            return false;
        }

        if (this.features != cand.getFeature()) {
            return false;
        }

        if (this.level != cand.getLevel()) {
            return false;
        }

        if (this.value != cand.getValue()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.level != null ? this.level.hashCode() : 0);
        hash = 67 * hash + (this.features != null ? this.features.hashCode() : 0);
        hash = 67 * hash + (this.context != null ? this.context.hashCode() : 0);
        return hash;
    }
}
