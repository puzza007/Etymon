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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math.special.Gamma;
import etymology.util.LogRegretMatrixPrinter;
import java.io.FileNotFoundException;

/**
 * The approximations are from articles like
 * http://citeseer.ist.psu.edu/schraudolph98fast.html
 *
 * @author arto
 */
public class EtyMath {
    private static final double GAMMA_CACHE_DIVIDER = 100.0;
    private static final double LOG2 = Math.log(2);
    
    private static List<Double> logValues = new ArrayList();
    private static List<Double> logGammaValues = new ArrayList();
    private static double[][] logRegret;

    static {
        base2LogFactorial(1000000);
        //initLogRegret(8, 8000);
    }
    
    
    public static void setLogRegretTable(double[][] logRegret) {
        EtyMath.logRegret = logRegret;
    }
    
    public static double[][] initLogRegret(int K, int N) {
        logRegret = new double[K][N];
        int row;
        int col;
        
        //init: L(1,n) = 0 for all n in N
        for (int n=1; n<=N; n++) {
            System.out.println("n: " + n);
            col = n-1;
            
            //init k=1 (row 0)
            logRegret[0][col] = 0;
            logRegret[1][col] = 0;                       
            
            
            //init k==2 (row 1)
            for (int i=0; i<=n; i++) {
                double a =  logRegret[1][col];
                double b = lnFactorial(n) - lnFactorial(i) - lnFactorial(n-i) 
                         + xlogx(i) + xlogx(n-i) - xlogx(n);
                
                logRegret[1][col] = logPlus(a, b); 
            }
            
            //init k>=3 (row 2 -->)
            for (int k=3; k<=K; k++) {
                row = k-1;
                double a = logRegret[row-1][col];
                double b = Math.log(n) - Math.log(k) + logRegret[row-2][col];
                
                logRegret[row][col] = logPlus(a, b);
            }
        }
        
//        for (double[] roww : logRegret) {
//            System.out.println(Arrays.toString(roww));
//        }
        
       return logRegret;
        
        //
    }
    
    public static double  logPlus(double a, double b) {
        double logPlus = 0;
        double max = Math.max(a, b);
        double min = Math.min(a, b);
        
        logPlus = max + Math.log(1.0 + Math.exp(min-max));
        
        return logPlus;
    }
    
    public static double xbase2logx(double x) {
        return xlogx(x) / LOG2;
    }
    public static double xlogx(double x) {
        if (x==0) {
            return 0;
        }
        return x * Math.log(x);
    }
    /**
     * 
     * @param K Event type
     * @param N Event instances
     * @return 
     */
    public static double logRegret(int K, int N) throws FileNotFoundException{

        if (K==0) {
            throw new RuntimeException("K = 0!!!");
        }
        if (N==0) {
            return 0;
        }
        if (logRegret == null) {
            String logRegretTableName = "/home/group/langtech/Etymology-Project/logRegretMatrix-10kx1k";
            setLogRegretTable(LogRegretMatrixPrinter.getLogRegretMatrix(logRegretTableName));
        } 
        return logRegret[K-1][N-1];

    }

    public static double base2LogGamma(double value) {
        return lnGamma(value) / LOG2;
    }

    // gamma K = (K - 1)!, returns lnGamma
    public static double lnGamma(double value) {
        if (value < 0) {
            throw new RuntimeException("Value for lnGamma must be > 0.");
        }

        if (value == 0) {
            return 0;
        }

        if(value == 1.0) {
            return 0;
        }

        if (value > 100000) {
            return avgLnGamma(value);
        }

        int arrayIndex = (int) Math.ceil((value * GAMMA_CACHE_DIVIDER));

        if (logGammaValues.size() > arrayIndex) {
            return logGammaValues.get(arrayIndex);
        }

        int size = (logGammaValues.isEmpty()) ? 1 : logGammaValues.size();

        double val = 0.0;
        for (int i = size; i <= arrayIndex; i++) {
            val = Gamma.logGamma((i / GAMMA_CACHE_DIVIDER));
            logGammaValues.add(val);
        }

        return val;
    }

    public static double avgLnGamma(double value) {
        int floor = (int) Math.floor(value);
        double diff = Math.abs(value - floor);
        if (diff <= 0.01) {
            return lnFactorial(floor - 1);
        }

        int ceil = (int) Math.ceil(value);
        diff = Math.abs(ceil - value);
        if (diff <= 0.01) {
            return lnFactorial(ceil - 1);
        }

        double logFactCeil = lnFactorial(ceil);
        double logFactFloor = lnFactorial(floor);
        double overFloor = value - floor;
        return (overFloor * logFactFloor) + (1 - overFloor) * logFactCeil;
    }
    
    public static double getNormalizedCompressionDistance(double langsCost, double l1Cost, double l2Cost) {
        // NCD(1,2) = ( C12 - min (C11, C22)) / max (C11, C22)
        double min = Math.min(l1Cost, l2Cost);
        double max = Math.max(l1Cost, l2Cost);

        double dist = (langsCost - min) / max;
        return dist;
    }

    public static double base2LogFactorial(int n) {
        if (n == 0) {
            return 0.0;
        }
        if(n < 0) {
            throw new RuntimeException("Sorry, you're trying to get a bad number");
        }

        return lnFactorial(n) / LOG2;
        //return logValues.get(n-1)/LOG2;
    }

    // return ln n!
    public static double lnFactorial(int n) {
        if (n > 0 && logValues.size() >= n) {
            return logValues.get(n-1);
        }

        double val = 0.0;
        int start = 1;

        if (logValues.size() > 1) {
            start = logValues.size();
            val = logValues.get(start - 1);
        }

        for (int i = start; i <= n; i++) {
            val += Math.log(i);
            logValues.add(val);
        }

        return val;
    }

    // return log binomial n choose k.
    public static double lnBinomial(int n, int k) {
        return lnFactorial(n) - lnFactorial(k) - lnFactorial(n - k);
    }

    public static double base2LogBinomial(int n, int k) {
        return base2LogFactorial(n) - base2LogFactorial(k) - base2LogFactorial(n - k);
    }

    // base 2 log, duh.
    public static double base2Log(double value) {
        return (Math.log(value) / LOG2);
    }

    //// Could be used at the initial rounds, after which switch to accurate calculations
    // approximations for faster calc
    public static double approxPow(final double a, final double b) {
        final int x = (int) (Double.doubleToLongBits(a) >> 32);
        final int y = (int) (b * (x - 1072632447) + 1072632447);
        return Double.longBitsToDouble(((long) y) << 32);
    }

    public static double approxLn(double val) {
        final double x = (Double.doubleToLongBits(val) >> 32);
        return (x - 1072632447) / 1512775;
    }

    public static double approxExp(double val) {
        final long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }

    public static double approxLogGamma(double x) {
        double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
        double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1)
                + 24.01409822 / (x + 2) - 1.231739516 / (x + 3)
                + 0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
        return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
    }

    public static double approxGamma(double x) {
        return Math.exp(approxLogGamma(x));
    }

    public static float roundFloat(float val) {
        return (float) (Math.round(val * 1000.0) / 1000.0);
    }


    public static float roundFloat(double val) {
        return (float) (Math.round(val * 1000.0) / 1000.0);
    }

    public static double getMin(Collection<Double> values) {
        double min = Double.MAX_VALUE;
        for(double value: values) {
            min = Math.min(min, value);
        }

        return min;
    }

    public static double getMax(Collection<Double> values) {
        double max = Double.MIN_VALUE;
        for(double value: values) {
            max = Math.max(max, value);
        }

        return max;
    }

    public static double getAverage(Collection<Double> values) {
        return getMean(values);
    }

    public static double getMean(Collection<Double> values) {
        if(values == null || values.isEmpty()) {
            return 0;
        }

        double val = 0.0;
        for(double value: values) {
            val += value;
        }

        return val / values.size();
    }

    public static double getExpectedAbsoluteDeviation(Collection<Double> values) {
        double avg = getAverage(values);

        double absoluteDeviations = 0;
        for(double val: values) {
            absoluteDeviations += Math.abs(avg - val);
        }

        double expectedAbsoluteDeviation = absoluteDeviations / values.size();
        return expectedAbsoluteDeviation;
    }

    public static double getVariance(Collection<Double> values) {
        return getExpectedSquaredDeviation(values);
    }

    public static double getStandardDeviation(Collection<Double> values) {
        return Math.sqrt(getVariance(values));
    }

    public static double getExpectedSquaredDeviation(Collection<Double> values) {
        double expectedSquaredDeviation = getSquaredDeviation(values) / values.size();
        return expectedSquaredDeviation;
    }

    public static double getSquaredDeviation(Collection<Double> values) {
        double avg = getAverage(values);

        double squaredDeviation = 0;
        for(double val: values) {
            squaredDeviation += Math.pow(Math.abs(avg - val), 2);
        }

        return squaredDeviation;
    }

    public static void main(String[] args) {
        double oyToOi = (98.0 / (298116-97))*(1.0/3344)*(200.0/3345);
        System.out.println(oyToOi);

        double oToO = (32.0/546) * (1.0/3344) * (200.0 / 3345);
        System.out.println(oToO);

        double yToI = 20.0 / 3344;
        System.out.println(yToI);

        double oToOToYToIPath = oToO * yToI;
        System.out.println(oToOToYToIPath);

        double pathCost = -1.0 * base2Log(oToOToYToIPath);
        System.out.println(pathCost);

        double combinedPathCost = -1.0 * base2Log(oyToOi);
        System.out.println(combinedPathCost);
    }
}
