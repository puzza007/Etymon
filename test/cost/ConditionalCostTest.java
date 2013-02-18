/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cost;

import etymology.cost.TwoPartCodeCostFunctionWithKindsNotSeparate;
import etymology.util.EtyMath;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author avihavai
 */
public class ConditionalCostTest {

    private int[][] predefinedAlignmentMatrix;
    private int numberOfWords;

    public ConditionalCostTest() {
        predefinedAlignmentMatrix = new int[][]{
                    {0, 2, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
                    {7, 29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 14, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0},
                    {1, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {9, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {1, 0, 0, 0, 0, 8, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 4, 0, 0, 4, 0, 0},
                    {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 0, 0, 0, 0, 0, 0, 0},
                    {1, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0},
                    {1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4},
                    {2, 6, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0},
                    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0}
                };

        numberOfWords = 52;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
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
    public void testConditionalCost() {
        TwoPartCodeCostFunctionWithKindsNotSeparate costFunction = new TwoPartCodeCostFunctionWithKindsNotSeparate();
        Assert.assertEquals(1341.4093, costFunction.getConditionalCostKindsNotSeparate(predefinedAlignmentMatrix, numberOfWords), 1);
    }
    
    @Test
    public void testNegativeSumLogGammaOverEventCount() {
        double negativeSumLogGamma = new TwoPartCodeCostFunctionWithKindsNotSeparate().getNegativeSumLogGammaCounts(predefinedAlignmentMatrix, numberOfWords);
        Assert.assertEquals(-617.9639, negativeSumLogGamma, 1);
    }

    @Test
    public void testEventCount() {
        int eventCount = getEventCount();
        Assert.assertEquals(317, eventCount);
    }

    @Test
    public void testPositiveLogGammaSumOverEventCount() {
        int eventCount = getEventCount();

        double positiveLogGammaOverEventCount = EtyMath.base2LogGamma(eventCount);
        Assert.assertEquals(2173.5813, positiveLogGammaOverEventCount, 1);
    }

    private int getEventCount() {
        new TwoPartCodeCostFunctionWithKindsNotSeparate().getNumberOfNonZeroEvents(predefinedAlignmentMatrix);
        int eventCount = numberOfWords + 1; // #-#

        for(int[] row: predefinedAlignmentMatrix) {
            for(int cellCount: row) {
                if(cellCount <= 0) {
                    continue;
                }

                eventCount += (cellCount + 1);
            }
        }

        return eventCount;
    }

    @Test
    public void testNegativeSumLogGammaOverNonZeroCountEvents() {
        int nonZeroEventCount = new TwoPartCodeCostFunctionWithKindsNotSeparate().getNumberOfNonZeroEvents(predefinedAlignmentMatrix);

        double negativeSumLogGammaOverNonZeroEvents = EtyMath.base2LogGamma(nonZeroEventCount);
        Assert.assertEquals(214.20816, negativeSumLogGammaOverNonZeroEvents, 1);
    }

    @Test
    public void testConditionalCostWithFourCells() {
        int[][] fourCells = {{1001, 2002}, {3003, 4004}};
        int numOfWords = 1;

        double expectedConditionalCost = 18525.156;
        TwoPartCodeCostFunctionWithKindsNotSeparate costFunction = new TwoPartCodeCostFunctionWithKindsNotSeparate();
        Assert.assertEquals(expectedConditionalCost, costFunction.getConditionalCostKindsNotSeparate(fourCells, numOfWords), 1);
    }


    @Test
    public void testCodebookCostWithFourCellsAndNoKinds() {
        int[][] fourCells = {{1001, 2002}, {3003, 4004}};
        double expectedCodebookCost = 3.321928;
        TwoPartCodeCostFunctionWithKindsNotSeparate costFunction = new TwoPartCodeCostFunctionWithKindsNotSeparate();
        Assert.assertEquals(expectedCodebookCost, costFunction.getCodebookCostNoKinds(fourCells), 0.1);
    }
}