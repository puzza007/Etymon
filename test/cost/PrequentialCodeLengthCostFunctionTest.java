/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cost;

import etymology.cost.PrequentialCodeLengthCostFunction;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author sxhiltun
 */
public class PrequentialCodeLengthCostFunctionTest {

    private PrequentialCodeLengthCostFunction costFunction;

    public PrequentialCodeLengthCostFunctionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        costFunction = new PrequentialCodeLengthCostFunction();
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

//    @Test
//    public void testSingleCellCost() {
//        int[] singleCellMatrix = {0};
//        double expectedSingleCellMatrixCost = 0;
//        Assert.assertEquals(expectedSingleCellMatrixCost, costFunction.getPrequentialCodeLength(singleCellMatrix), 0);
//    }
//
//    @Test
//    public void testCostWithFourElements() {
//        int[] alignmentMatrix = {1001, 2002, 3003, 4004};
//        double expectedSingleCellMatrixCost = 18500;
//        Assert.assertEquals(expectedSingleCellMatrixCost, costFunction.getPrequentialCodeLength(alignmentMatrix), 1);
//    }
//
//    @Test
//    public void testCostWithTwoElements() {
//        int[] alignmentMatrix = {12, 4};
//        double expectedSingleCellMatrixCost = 14.92;
//        Assert.assertEquals(expectedSingleCellMatrixCost, costFunction.getPrequentialCodeLength(alignmentMatrix), 0.01);
//    }
}
