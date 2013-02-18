/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import etymology.cost.PrequentialCodeLengthCostFunction;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 *
 * @author nouri
 */
public class PrequentialCodeNegativeTest {
    public static void main(String[] args) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File("/home/nouri/Desktop/changes"));
        ArrayList<ArrayList<ArrayList<Integer>>> matrices = new ArrayList<ArrayList<ArrayList<Integer>>>();
        
        ArrayList<ArrayList<Integer>> matrix = new ArrayList<ArrayList<Integer>>();
        
        while(scanner.hasNext()){
            String line = scanner.nextLine();
            if(line.startsWith("-")){
                matrices.add(matrix);
                //do calc
                doCalc(matrices);
                //go to next                                
                matrix = new ArrayList<ArrayList<Integer>>();
                matrices = new ArrayList<ArrayList<ArrayList<Integer>>>();
                continue;
            }else if(line.startsWith("+")){
                //initial done
                matrices.add(matrix);
                matrix = new ArrayList<ArrayList<Integer>>();
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(line);
            ArrayList<Integer> row = new ArrayList<Integer>();
            while(tokenizer.hasMoreTokens()){
                row.add(Integer.parseInt(tokenizer.nextToken()));
            }
            matrix.add(row);            
        }
    }

    private static void doCalc(ArrayList<ArrayList<ArrayList<Integer>>> matrices) {
        System.out.println("**************************************");
        etymology.cost.PrequentialCodeLengthCostFunction preq = new PrequentialCodeLengthCostFunction();
        int[][][] allmatrices = new int[matrices.size()][][];
        for(int i = 0 ; i < allmatrices.length ; i++){
            allmatrices[i] = new int[matrices.get(i).size()][];
            ArrayList<ArrayList<Integer>> mat = matrices.get(i);
            for(int j = 0 ; j < allmatrices[0].length ; j++){
                allmatrices[i][j] = new int[mat.get(j).size()];
                ArrayList<Integer> row = mat.get(j);
                for(int k = 0 ; k < allmatrices[0][0].length ; k++){
                    allmatrices[i][j][k] = row.get(k);
                }
            }
        }
        
        double initCost = preq.getCodeLength(allmatrices[0]);
        System.out.println("Cost of Initial: " + initCost);
        double finalCost = preq.getCodeLength(allmatrices[allmatrices.length - 1]);
        System.out.println("Cost of Final: " + finalCost);
        double expectedDiff = (finalCost - initCost);
        System.out.println("Expected difference: " + expectedDiff);
        
        //Now for each matrix starting from 2nd find difference of each step
        double costOfSteps = 0;
        for(int i = 1 ; i < allmatrices.length ; i++){
            int[][] currentMatrix = allmatrices[i];
            int[][] perviousMatrix = allmatrices[i - 1];            
            int[][] stepMatrix = clacStepMat(currentMatrix, perviousMatrix, allmatrices[0]);
            double stepCost = preq.getCodeLength(stepMatrix) - initCost;
            costOfSteps += stepCost;
            System.out.print((i==1 ? "" : " + ") + stepCost);
        }
        System.out.println("\nDifference seen by alignment: " + costOfSteps);
        System.out.println(costOfSteps - expectedDiff);
        
    }

    private static int[][] clacStepMat(int[][] currentMatrix, int[][] previousMatrix, int[][] baseMatrix) {
        int[][] matrix = new int[currentMatrix.length][];
        for(int i = 0 ; i < matrix.length ; i++){
            matrix[i] = new int[currentMatrix[i].length];
            for(int j = 0 ; j < matrix[i].length ; j++){
                matrix[i][j] = currentMatrix[i][j] - previousMatrix[i][j] + baseMatrix[i][j];
            }
        }
        return matrix;
    }
}
