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

import etymology.config.Configuration;
import etymology.config.Constants;
import etymology.align.Alignator;
import etymology.align.AlignmentMatrix;
import etymology.align.matrices.MarginalAlignmentMatrix;
import etymology.align.matrices.TwoLangAlignmentMatrix;
import etymology.context.FeatureAlignmentMatrix;
import etymology.context.FeatureTree;
import etymology.input.Input;
import etymology.util.EtyMath;
import java.util.logging.Logger;

/**
 *
 * @author avihavai
 */
public class CostHandler {

    private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private AlignmentMatrix alignmentMatrix;
    private Input input;
    private CostFunction costFunction;
    private CostFunction costFunctionWithKindsSeparate = new TwoPartCodeCostFunctionWithKindsSeparate();
    private CostFunction costFunctionWithKindsSeparateNML = new TwoPartCodeCostFunctionWithKindsSeparateNML();
    private CostFunction costFunctionWithKindsNotSeparateNML = new TwoPartCodeCostFunctionWithKindsNotSeparateNML();

    public CostHandler(AlignmentMatrix am, Input input, CostFunction costFunction) {
        this.alignmentMatrix = am;
        this.input = input;
        this.costFunction = costFunction;
  

    }

    public CostFunction getCostFunction() {
 
        return costFunction;
    }

    public double getGlobalCost() {
        if (Configuration.getInstance().isUseFeatures()) {
            return getCostInContextModel();
        }
        //if not using two part code
        if (!(costFunction instanceof TwoPartCodeCostFunction)) {
            if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
                MarginalAlignmentMatrix mam = (MarginalAlignmentMatrix) alignmentMatrix;
                double costSum = 0;

                for (TwoLangAlignmentMatrix m : mam.getMatrices()) {
                    costSum += costFunction.getCost(m);
                }

                return costSum;
            }

            return costFunction.getCost(alignmentMatrix);
        }
        
        //using two part code
        
        double codebook = getCodeBookCost();
        double conditional = 0;
        if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE) { 
            //System.out.println(Configuration.getInstance().getCostFunctionIdentifier());
            conditional = getConditionalCostWithKindsSeparate(); 
        }
        else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE){
            conditional = getConditionalCostWithKindsNotSeparate();
        } else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE_NML) {
            conditional = getConditionalCostWithKindsSeparateNML();
        } else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML) {
            conditional = getConditionalCostWithKindsNotSeparateNML();
        }  else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_NO_KINDS) {
            conditional = getConditionalCostCodebookNoKinds();
        } 
        
        //System.out.println("COST: " + (codebook + conditional));
        return codebook + conditional;

    }

    public double getCostInContextModel() {
        FeatureAlignmentMatrix fam = Alignator.getInstance().getFeatureAlignmentMatrix();

        double cost = 0;
        for (FeatureTree tree : fam.getTrees()) {

            cost += tree.getTotalTreeCost();
        }

        return cost;

    }

    private double getCodeBookCost() {
        if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE) {
            TwoPartCodeCostFunction tpCodeCf = (TwoPartCodeCostFunctionWithKindsNotSeparate) costFunction;

            //for 3D model
         if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
             double codebookCost = 0.0;
             for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                  codebookCost += tpCodeCf.getCodebookCost(matrix);
                }

                return codebookCost;
         }

            return tpCodeCf.getCodebookCost(alignmentMatrix);
            
        } else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE){
            TwoPartCodeCostFunction tpCodeCfwks = (TwoPartCodeCostFunctionWithKindsSeparate) costFunction;

            //for 3D model
         if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
             double codebookCost = 0.0;
             for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                  codebookCost += tpCodeCfwks.getCodebookCost(matrix);
                }

                return codebookCost;
         }

            return tpCodeCfwks.getCodebookCost(alignmentMatrix);
            
        } else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_NO_KINDS){
            TwoPartCodeCostFunction tpCodeCfnk = (TwoPartCodeCostNoKindsUniformPrior) costFunction;

            //for 3D model
         if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
             double codebookCost = 0.0;
             for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                  codebookCost += tpCodeCfnk.getCodebookCost(matrix);
                }

                return codebookCost;
         }

            return tpCodeCfnk.getCodebookCost(alignmentMatrix);
            
        } else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE_NML) {
            TwoPartCodeCostFunction tpCodeCfwksNML = (TwoPartCodeCostFunctionWithKindsSeparateNML) costFunctionWithKindsSeparateNML;

            //for 3D model
         if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
             double codebookCost = 0.0;
             for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                  codebookCost += tpCodeCfwksNML.getCodebookCost(matrix);
                }

                return codebookCost;
         }

            return tpCodeCfwksNML.getCodebookCost(alignmentMatrix);
        } else if (Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML) {
            TwoPartCodeCostFunction tpCodeCfwknsNML = (TwoPartCodeCostFunctionWithKindsNotSeparateNML) costFunctionWithKindsNotSeparateNML;

            //for 3D model
         if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
             double codebookCost = 0.0;
             for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                  codebookCost += tpCodeCfwknsNML.getCodebookCost(matrix);
                }

                return codebookCost;
         }

            return tpCodeCfwknsNML.getCodebookCost(alignmentMatrix);
        }
       
        return 0;
    } 

    /*
     * two part code with kinds
     * NO separate kinds
     */
    private double getConditionalCostCodebookNoKinds() {
        TwoPartCodeCostFunction tpCodeCf = (TwoPartCodeCostNoKindsUniformPrior) costFunction;
        
        //for 2 languages
        if (input.getNumOfLanguages() == 2) {
            return tpCodeCf.getConditionalCost(alignmentMatrix);
        }

        if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
            double condCost = 0.0;
            for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                condCost += tpCodeCf.getConditionalCost(matrix);
            }

            return condCost;
        }

        return getConditionalCostForThreeLanguages();
    }
    
    
    private double getConditionalCostWithKindsNotSeparate() {
        TwoPartCodeCostFunction tpCodeCf = (TwoPartCodeCostFunctionWithKindsNotSeparate) costFunction;
        
        //for 2 languages
        if (input.getNumOfLanguages() == 2) {
            return tpCodeCf.getConditionalCost(alignmentMatrix);
        }

        if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
            double condCost = 0.0;
            for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                condCost += tpCodeCf.getConditionalCost(matrix);
            }

            return condCost;
        }

        return getConditionalCostForThreeLanguages();
    }
    private double getConditionalCostWithKindsNotSeparateNML() {
        TwoPartCodeCostFunction tpCodeCfwknsNML = (TwoPartCodeCostFunctionWithKindsNotSeparateNML) costFunctionWithKindsNotSeparateNML;
        
        //for 2 languages
        if (input.getNumOfLanguages() == 2) {
            return tpCodeCfwknsNML.getConditionalCost(alignmentMatrix);
        }
        //for 3 langs maybe
        if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
            double condCost = 0.0;
            for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                condCost += tpCodeCfwknsNML.getConditionalCost(matrix);
            }

            return condCost;
        }

        return getConditionalCostForThreeLanguages();
    }
    /*
     * two part code with kinds
     * YES separate kinds
     */
    private double getConditionalCostWithKindsSeparate() {
        TwoPartCodeCostFunctionWithKindsSeparate tpCodeCfwks = (TwoPartCodeCostFunctionWithKindsSeparate) costFunctionWithKindsSeparate;
        
        //for 2 languages
        if (input.getNumOfLanguages() == 2) {
            
            return tpCodeCfwks.getConditionalCost(alignmentMatrix);
        }
        //for 3 langs maybe
        if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
            double condCost = 0.0;
            for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                condCost += tpCodeCfwks.getConditionalCost(matrix);
            }

            return condCost;
        }

        return getConditionalCostForThreeLanguages();
    }
    
    private double getConditionalCostWithKindsSeparateNML() {
        TwoPartCodeCostFunction tpCodeCfwkNML = (TwoPartCodeCostFunctionWithKindsSeparateNML) costFunctionWithKindsSeparateNML;
        
        //for 2 languages
        if (input.getNumOfLanguages() == 2) {
            return tpCodeCfwkNML.getConditionalCost(alignmentMatrix);
        }
        //for 3 langs maybe
        if (alignmentMatrix instanceof MarginalAlignmentMatrix) {
            double condCost = 0.0;
            for (TwoLangAlignmentMatrix matrix : ((MarginalAlignmentMatrix) alignmentMatrix).getMatrices()) {
                condCost += tpCodeCfwkNML.getConditionalCost(matrix);
            }

            return condCost;
        }

        return getConditionalCostForThreeLanguages();
    }
    
    

    private double getConditionalCostForThreeLanguages() {
        double negativeSumLogGammaCounts = -1.0 * EtyMath.base2LogFactorial(input.getNumOfWords());
        double positiveSumCounts = input.getNumOfWords();
        int numOfNonzeroEvents = 1; // starting with start - to - start

        for (int i = 0; i < alignmentMatrix.getL1SymbolCount(); i++) {
            for (int j = 0; j < alignmentMatrix.getL2SymbolCount(); j++) {
                for (int k = 0; k < alignmentMatrix.getL3SymbolCount(); k++) {
                    double count = alignmentMatrix.getAlignmentCountAtIndex(i, j, k);

                    if (count == 0) {
                        continue;
                    }

                    negativeSumLogGammaCounts -= EtyMath.base2LogGamma(count + 1); // EtyMath.base2LogFactorial(count);
                    positiveSumCounts += (count + 1);
                    numOfNonzeroEvents++;
                }
            }
        }

        // System.out.println("NNZ: " + numOfNonzeroEvents);

        double conditionalCost = negativeSumLogGammaCounts
                + EtyMath.base2LogGamma(positiveSumCounts) // EtyMath.base2LogFactorial((positiveSumCounts + numOfNonzeroEvents - 1))
                - EtyMath.base2LogGamma(numOfNonzeroEvents);  // EtyMath.base2LogFactorial(numOfNonzeroEvents - 1);

        return conditionalCost;
    }


    public String getCostString() {
        //For context model
        if (Configuration.getInstance().isUseFeatures()) {
            StringBuilder sb = new StringBuilder();
            double costInContextModel = getCostInContextModel();
            
            sb.append("COST OF TREES ");
            sb.append(Constants.COST_FORMAT.format(costInContextModel));
            sb.append("\n");

            return sb.toString();
        }

        if (Configuration.getInstance().isRemoveSuffixes()) {
            double codebook = getCodeBookCost();
            double conditional = getConditionalCostWithKindsNotSeparate();

            double[] codebookThings = ((TwoPartCodeCostNoKindsWithSuffixes) costFunction).getAllCodebookCosts();
            double[] conditionalThings = ((TwoPartCodeCostNoKindsWithSuffixes) costFunction).getAllConditionalCosts();

            String ret =  costFunction.getName() + "\n";
            ret += "Total Cost: " + Constants.COST_FORMAT.format((codebook + conditional)) + "\n";
            ret += "(CODEBOOK alignments) " + Constants.COST_FORMAT.format(codebookThings[0]);
            ret += " (CODEBOOK suffixes) " + Constants.COST_FORMAT.format(codebookThings[1] + codebookThings[2]) + "\n";
            ret += "(COND alignments) " + Constants.COST_FORMAT.format(conditionalThings[0]) + " (COND suffixes) "
                    + Constants.COST_FORMAT.format(conditionalThings[1] + conditionalThings[2]) + "\n";

            return ret;

        }
        
        if(Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE) {
            
            
                double codebook = getCodeBookCost();
                double conditional = getConditionalCostWithKindsSeparate();
                // return costFunction.getName()       + " (BOOK) " + Constants.COST_FORMAT.format(codebook) + " + (COND) " + Constants.COST_FORMAT.format(conditional) + " = " + Constants.COST_FORMAT.format((codebook + conditional));
                return costFunction.getName() + " (BOOK) " + Constants.COST_FORMAT.format(codebook) + " + (COND) " + Constants.COST_FORMAT.format(conditional) + " = " + Constants.COST_FORMAT.format((codebook + conditional));
            
        }  
        else if(Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.BASELINE) {
                double cost = getGlobalCost();
                return costFunction.getName() + " (BOOK) " + Constants.COST_FORMAT.format(cost) ;
            }  
        else if(Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_NO_KINDS) {
                double codebook = getCodeBookCost();
                double conditional = getConditionalCostCodebookNoKinds();
                return costFunction.getName() + " (BOOK) " + Constants.COST_FORMAT.format(codebook) + " + (COND) " + Constants.COST_FORMAT.format(conditional) + " = " + Constants.COST_FORMAT.format((codebook + conditional));
            }
         
        else if(Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE){
                double codebook = getCodeBookCost();
                double conditional = getConditionalCostWithKindsNotSeparate();
                return costFunction.getName() + " (BOOK) " + Constants.COST_FORMAT.format(codebook) + " + (COND) " + Constants.COST_FORMAT.format(conditional) + " = " + Constants.COST_FORMAT.format((codebook + conditional));
        } 
        else if(Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_SEPARATE_NML) {
                double codebook = getCodeBookCost();
                double conditional = getConditionalCostWithKindsSeparateNML();
                return costFunctionWithKindsSeparateNML.getName() + " (BOOK) " + Constants.COST_FORMAT.format(codebook) + " + (COND) " + Constants.COST_FORMAT.format(conditional) + " = " + Constants.COST_FORMAT.format((codebook + conditional));
        } 
        else if(Configuration.getInstance().getCostFunctionIdentifier() == CostFunctionIdentifier.CODEBOOK_WITH_KINDS_NOT_SEPARATE_NML) {
                double codebook = getCodeBookCost();
                double conditional = getConditionalCostWithKindsSeparateNML();
                return costFunctionWithKindsNotSeparateNML.getName() + " (BOOK) " + Constants.COST_FORMAT.format(codebook) + " + (COND) " + Constants.COST_FORMAT.format(conditional) + " = " + Constants.COST_FORMAT.format((codebook + conditional));
        }
        
        return costFunction.getName() + " " + Constants.COST_FORMAT.format(getGlobalCost());
    }
}
