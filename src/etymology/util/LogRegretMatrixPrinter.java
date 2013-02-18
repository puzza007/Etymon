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

import etymology.util.EtyMath;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author sxhiltun
 */
public class LogRegretMatrixPrinter {
    
    public static void main(String[] args) throws IOException {
        String filename = "/home/group/langtech/Etymology-Project/logRegretMatrix-10kx1k";
        printLogRegretMatrix(filename, 10000, 1000);
        getLogRegretMatrix(filename);
    }

    private static void printLogRegretMatrix(String filename, int N, int K) throws IOException {
        
        File f = new File(filename);
        PrintStream printStream = new PrintStream(f);
        
        double matrix[][] = EtyMath.initLogRegret(K, N);
        
        //print number of rows and cols
        printStream.print(K);
        printStream.print("\t");
        printStream.print(N);
        printStream.print("\n");
        
        for (double[] row : matrix) {
            for (double val : row) {
                printStream.print((val)/Math.log(2));
                printStream.print("\t");                
            }
            printStream.print("\n");
        }
        printStream.close();
    }
    
    public static double[][] getLogRegretMatrix(String filename) throws FileNotFoundException {
        Scanner sc = new Scanner(new File(filename));
        
        int rows = sc.nextInt();
        int cols = sc.nextInt();
        double[][] matrix;
        
//        System.out.println("rows: " + rows);
//        System.out.println("cols: " + cols);
                        
        if (rows < 0 || cols < 0 ) {
            System.out.println("Failed to read the matrix dimensions. ");
            System.exit(-1);
        }
        
        matrix = new double[rows][cols];
        
        int row = 0;
        while(sc.hasNext()) {
            //System.out.println(sc.nextLine());
            //System.out.println(sc.next());
            for (int col=0; col<cols; col++) {
                matrix[row][col] = Double.parseDouble(sc.next());
            }
            row++;
        }
        
        if (row != rows) {
            System.err.println(row + "Number of rows doesn't match!");
        }
        sc.close();
        
//        for (double[] mrow : matrix) {
//            System.out.println(Arrays.toString(mrow));
//        }
//        System.out.println("matrix.length: " + matrix.length);
//        System.out.println("matrix[0].length: " + matrix[0].length);
        return matrix;
    }
    
}
