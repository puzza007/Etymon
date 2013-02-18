/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cost;

//import etymology.cost.TwoPartCost;
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
public class BaselineCostFunctionTest {
    //private TwoPartCost costFunction;

    public BaselineCostFunctionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        //costFunction = new TwoPartCost();
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void testSingleCellCost() {
        int[] singleCellMatrix = {0};
        double expectedSingleCellMatrixCost = 0;

        //Assert.assertEquals(costFunction.getConditionalCostForTrees(singleCellMatrix), expectedSingleCellMatrixCost, 0);
    }


    @Test
    public void testTwoDimensionCost() {
        int[][] alignmentMatrix = {{1001, 2002}, {3003, 4004}};
        double expectedSingleCellMatrixCost = 18534.328;

        //Assert.assertEquals(costFunction.getConditionalCostForTrees(alignmentMatrix), expectedSingleCellMatrixCost, 0);
    }
}