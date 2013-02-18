/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package etymology.viterbi;

import etymology.cost.AlignmentCostFunction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author arto
 */
public class RecursiveViterbiMatrixTest {
    AlignmentCostFunction mockCost = new MockAlignmentCostFunction(true);

    public RecursiveViterbiMatrixTest() {
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
    public void testFailCreateRecursiveViterbiMatrix() {
        boolean failed = false;

        try {
            RecursiveViterbiMatrix vit = new RecursiveViterbiMatrix(null, mockCost);
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertEquals(failed, true);
    }

    @Test
    public void testSuccessCreateOneDimensionalRecursiveViterbiMatrix() {
        boolean failed = false;
        List<Integer> wIndexes = Arrays.asList(1, 3, 5, 7);
        Map<Integer, List<Integer>> langIdToWordIndexes = new HashMap();
        langIdToWordIndexes.put(0, wIndexes);

        try {
            RecursiveViterbiMatrix vit = new RecursiveViterbiMatrix(langIdToWordIndexes, mockCost);
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertEquals(false, failed);
    }


    @Test
    public void testSuccessCreateThreeDimensionalRecursiveViterbiMatrix() {
        boolean failed = false;
        List<Integer> wIndexes = Arrays.asList(1, 3, 5, 7);

        Map<Integer, List<Integer>> langIdToWordIndexes = new HashMap();
        for(int langId = 0; langId < wIndexes.size(); langId++) {
            langIdToWordIndexes.put(langId, wIndexes);
        }

        try {
            RecursiveViterbiMatrix vit = new RecursiveViterbiMatrix(langIdToWordIndexes, mockCost);
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertEquals(false, failed);
    }


    @Test
    public void testExistingViterbiCell() {
        List<Integer> wIndexes = Arrays.asList(1, 3, 5, 7);

        Map<Integer, List<Integer>> langIdToWordIndexes = new HashMap();
        langIdToWordIndexes.put(0, wIndexes);

        RecursiveViterbiMatrix vit = new RecursiveViterbiMatrix(langIdToWordIndexes, mockCost);

        ViterbiCell cell = vit.getViterbiCell(Arrays.asList(0));
        Assert.assertNotNull(cell);
    }


    @Test
    public void testExistingRootViterbiCellInThreeDimensions() {
        List<Integer> wIndexes = Arrays.asList(1, 3, 5, 7);

        Map<Integer, List<Integer>> langIdToWordIndexes = new HashMap();
        for(int langId = 0; langId < wIndexes.size() - 1; langId++) {
            langIdToWordIndexes.put(langId, wIndexes);
        }
        
        RecursiveViterbiMatrix vit = new RecursiveViterbiMatrix(langIdToWordIndexes, mockCost);

        ViterbiCell cell = vit.getViterbiCell(Arrays.asList(0, 0, 0));
        Assert.assertNotNull(cell);
    }

    @Test
    public void testSensibleCosts() {
        List<Integer> wIndexes = Arrays.asList(1, 3, 5, 7);

        Map<Integer, List<Integer>> langIdToWordIndexes = new HashMap();
        langIdToWordIndexes.put(0, wIndexes);

        RecursiveViterbiMatrix vit = new RecursiveViterbiMatrix(langIdToWordIndexes, mockCost);

        ViterbiCell cell = vit.getViterbiCell(Arrays.asList(4));
        while(cell.getParent() != null) {
            Assert.assertTrue(cell.getCost() >= cell.getParent().getCost());
            cell = cell.getParent();
        }
    }


    @Test
    public void testSensibleCostsInThreeDimensions() {
        List<Integer> wIndexes = Arrays.asList(1, 3, 5, 7);

        Map<Integer, List<Integer>> langIdToWordIndexes = new HashMap();
        for(int langId = 0; langId < wIndexes.size() - 1; langId++) {
            langIdToWordIndexes.put(langId, wIndexes);
        }

        RecursiveViterbiMatrix vit = new RecursiveViterbiMatrix(langIdToWordIndexes, mockCost);

        ViterbiCell cell = vit.getViterbiCell(Arrays.asList(4, 4, 4));
        while(cell.getParent() != null) {
            Assert.assertTrue(cell.getCost() >= cell.getParent().getCost());
            cell = cell.getParent();
        }
    }
}