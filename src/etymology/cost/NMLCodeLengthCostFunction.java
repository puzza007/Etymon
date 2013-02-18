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

import etymology.util.EtyMath;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sxhiltun
 */
public class NMLCodeLengthCostFunction implements FeatureTreeDataCostFunction{
    
    //get NML codel length    
    public double getCodeLength(int[][] alignmentCountMatrix) {
        
        //number of possible event types in this matrix
        int K = alignmentCountMatrix.length * alignmentCountMatrix[0].length; //m*n matrix
        
        //number of events occurred
        //compute sum of all events in matrix
        int N = 0; 
        
        
        
        for (int[] matrixRow : alignmentCountMatrix) {
            for (int count: matrixRow) {
                N += count;
            }
        }
        

        // compute -logNML
        // -logNML(f) = -sum_i(f_i * log(f_i / sum_i(log(f_i))) + logRegret(N,K)  
        double logNML = 0;        
        for (int[] matrixRow : alignmentCountMatrix) {
            for (int count: matrixRow) {
                logNML -= EtyMath.xbase2logx(1.0*count);
                
            }
        }
        
        logNML += EtyMath.xbase2logx(N);                
        try {
            logNML += EtyMath.logRegret(K, N); //logregret in base2 form in file; should be
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NMLCodeLengthCostFunction.class.getName()).log(Level.SEVERE, null, ex);
        }
        
//        System.out.println("Matrix: " + Arrays.toString(alignmentCountMatrix[0]));
//        System.out.println("N: " + N);
//        System.out.println("K: " + K);
//        System.out.println("regret: " + EtyMath.logRegret(K, N));
//        System.out.println("logNML: " + logNML);
//        System.out.println("");
        
        return logNML;
        
        
        
//        try {
//            EtyMath.logRegret(K, N); //logregret in base2 form in file; should be
//        } catch(Exception e) {
//            System.out.println(e.toString());
//            System.out.println("K " + K);
//            System.out.println("N " + N);
//            System.out.println(Arrays.toString(alignmentCountMatrix[0]));
//        }

        
    }

    
}
