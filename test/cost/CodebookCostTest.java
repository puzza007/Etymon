/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
=== FINAL OUTPUT ===
CODEBOOK 229.32623 CONDITIONAL 1769.8256
Costs: ( 2P 1999.1519 JT 0 FA 1869.9622 ) AVG: ( 2P 3.990323 JT 0 FA 3.7324593 ) UNIQUE-GLYPHS: 25

 *ALIGNMENT-COUNTS*
.  a  b  d  e  g  h  i  j  k  l  m  n  o  p  r  s  t  u  v  ä  õ  ö  ü
.   0  2  0  1  1  0  0  0  0  1  0  1  0  0  0  0  0  1  0  0  0  0  0  0
a   7 29  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0
e   0  0  0  0 14  1  0  2  0  0  0  0  0  0  0  0  0  0  2  0  0  0  0  0
h   1  0  0  0  0  0  8  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0
i   9  0  0  0  0  0  0  9  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0
j   0  0  0  0  0  0  0  0 11  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0
k   1  0  0  0  0  8  0  0  0  6  0  0  0  0  0  0  0  0  0  0  0  0  0  0
l   0  0  0  0  0  0  0  0  0  0  8  0  0  0  0  0  0  0  0  0  0  0  0  0
m   0  0  0  0  0  0  0  0  0  0  0  9  0  0  0  0  0  0  0  0  0  0  0  0
n   1  0  0  0  2  0  0  0  0  0  0  0  7  0  0  0  0  0  0  0  0  0  0  0
o   1  0  0  0  1  0  0  0  0  0  0  0  0  3  0  0  0  0  4  0  0  4  0  0
p   0  0  1  0  0  0  0  0  0  0  0  0  0  0  3  0  0  0  0  0  0  0  0  0
r   0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  4  0  0  0  0  0  0  0  0
s   0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0 14  0  0  0  0  0  0  0
t   1  0  0  3  0  0  0  0  0  0  0  0  0  0  0  0  0  3  0  0  0  0  0  0
u   0  0  0  0  0  0  0  0  0  0  0  0  0  2  0  0  0  0  3  0  0  0  0  0
v   1  0  0  0  0  0  0  0  1  0  0  0  0  0  0  0  0  0  0  1  0  0  0  0
y   0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  1  4
ä   2  6  0  0  0  0  0  1  0  0  0  1  0  0  0  0  0  0  0  0  6  0  0  0
ö   0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  0  1  0

 */
package cost;

import etymology.align.Kind;
import etymology.align.matrices.TwoLangKindHolder;
import etymology.cost.TwoPartCodeCostNoKindsUniformPrior;
import etymology.cost.TwoPartCodeCostFunctionWithKindsNotSeparate;
import etymology.util.EtyMath;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author avihavai
 */
public class CodebookCostTest {

    private int[][] predefinedAlignmentMatrix;

    public CodebookCostTest() {
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
    public void testSimpleBinomialCalculation() {
        int n = 19;
        int k = 9;

        Assert.assertEquals(16.495262, EtyMath.base2LogBinomial(n, k), 0.001);
    }

    @Test
    public void testSimpleBaseTwoLogCalculation() {
        int n = 20;

        Assert.assertEquals(4.321928, EtyMath.base2Log(n), 0.001);
    }

    @Test
    public void testTargetToDotCost() {
        int targetToDotLength = predefinedAlignmentMatrix.length - 1;
        int numOfNonZeroEntries = 0;
        for (int rowId = 1; rowId <= targetToDotLength; rowId++) {
            if (predefinedAlignmentMatrix[rowId][0] <= 0) {
                continue;
            }

            numOfNonZeroEntries++;
        }

        Assert.assertEquals(targetToDotLength, 19);
        Assert.assertEquals(numOfNonZeroEntries, 9);

        double cost = EtyMath.base2Log(targetToDotLength + 1) + EtyMath.base2LogBinomial(targetToDotLength, numOfNonZeroEntries);
        Assert.assertEquals(20.81719, cost, 0.0001);
    }

    @Test
    public void testTargetToDotCostFromKind() {
        TwoLangKindHolder holder = TwoLangKindHolder.getSingleGlyphKindHolder(predefinedAlignmentMatrix);
        Kind k = holder.getKind(15, 0);
        Assert.assertEquals(20.81719, k.getRegionCost(), 0.0001);
    }

    @Test
    public void testDotToTargetCost() {
        int dotToTargetLength = predefinedAlignmentMatrix[0].length - 1;
        int numOfNonZeroEntries = 0;
        for (int colId = 1; colId <= dotToTargetLength; colId++) {
            if (predefinedAlignmentMatrix[0][colId] <= 0) {
                continue;
            }

            numOfNonZeroEntries++;
        }

        double dotToTargetCost = EtyMath.base2Log(dotToTargetLength + 1) + EtyMath.base2LogBinomial(dotToTargetLength, numOfNonZeroEntries);
        Assert.assertEquals(21.208204, dotToTargetCost, 0.0001);
    }

    @Test
    public void testSourceToTargetCost() {
        int numOfNonZeroEntries = 0;
        for (int rowId = 1; rowId < predefinedAlignmentMatrix.length; rowId++) {
            for (int colId = 1; colId < predefinedAlignmentMatrix[0].length; colId++) {
                if (predefinedAlignmentMatrix[rowId][colId] <= 0) {
                    continue;
                }

                numOfNonZeroEntries++;
            }
        }

        int areaSize = (predefinedAlignmentMatrix.length - 1) * (predefinedAlignmentMatrix[0].length - 1);

        double cost = EtyMath.base2Log(areaSize + 1) + EtyMath.base2LogBinomial(areaSize, numOfNonZeroEntries);
        Assert.assertEquals(180.8346, cost, 0.001);
    }

    @Test
    public void testDotToTargetCostFromKind() {
        TwoLangKindHolder holder = TwoLangKindHolder.getSingleGlyphKindHolder(predefinedAlignmentMatrix);
        Kind k = holder.getKind(0, 22);
        Assert.assertEquals(21.208204, k.getRegionCost(), 0.0001);
    }

    @Test
    public void testNumberOfNonZeroEvents() {
        TwoLangKindHolder holder = TwoLangKindHolder.getSingleGlyphKindHolder(predefinedAlignmentMatrix);

        int numOfNonZeroEvents = 1;
        for(Kind k: holder.getKinds()) {
            numOfNonZeroEvents += k.getNumOfNonZeroEvents();
        }

        int countedNumOfNonZeroEvents = 1;
        for(int[] row: predefinedAlignmentMatrix) {
            for(int cell: row) {
                if(cell == 0) {
                    continue;
                }

                countedNumOfNonZeroEvents++;
            }
        }

        Assert.assertEquals(countedNumOfNonZeroEvents, numOfNonZeroEvents);
    }

    @Test
    public void testNumberOfNonZeroEventsCountFromKinds() {
        TwoLangKindHolder holder = TwoLangKindHolder.getSingleGlyphKindHolder(predefinedAlignmentMatrix);

        int numOfNonZeroEvents = 1; // include #-#
        for(Kind k: holder.getKinds()) {
            numOfNonZeroEvents += k.getNumOfNonZeroEvents();
        }

        Assert.assertEquals(51, numOfNonZeroEvents);
    }


    @Test
    public void testNumberOfNonZeroEventsCountFromTwoPartFunction() {
        TwoPartCodeCostNoKindsUniformPrior costFunction = new TwoPartCodeCostNoKindsUniformPrior();
        Assert.assertEquals(51, costFunction.getNumberOfNonZeroEvents(predefinedAlignmentMatrix));
    }

    @Test
    public void testTwoPartUniformPriorCodebookCostWithNoKinds() {
        TwoPartCodeCostNoKindsUniformPrior costFunction = new TwoPartCodeCostNoKindsUniformPrior();
        double codebookCost = costFunction.getCodebookCostNoKinds(predefinedAlignmentMatrix);
        Assert.assertEquals(240, codebookCost, 1);
    }

//    @Test
//    public void testTwoPartUniformPriorCodebookCostWithKinds() {
//        TwoPartCodeCostUniformPrior costFunction = new TwoPartCodeCostUniformPrior();
//        Assert.assertEquals(223.85999, costFunction.get1x1CodebookCostWithKinds(predefinedAlignmentMatrix), 0.1);
//    }
}
