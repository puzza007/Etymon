/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package etymology.context;

import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sxhiltun
 */
public class ContextCellContainerTest {

    public ContextCellContainerTest() {
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

    /**
     * Test of addContextCell method, of class ContextCellContainer.
     */
    @Test
    public void testAddContextCell() throws Exception {
        System.out.println("addContextCell");
        int symbol = 0;
        int wordIndex = 0;
        int positionInWordIndex = 0;
        ContextCellContainer instance = new ContextCellContainer();
        
        ContextCell result = instance.addContextCell(symbol, wordIndex, positionInWordIndex);
        assertEquals(positionInWordIndex, result.getPositionInWord());
    }

    /**
     * Test of getContextCell method, of class ContextCellContainer.
     */
    @Test
    public void testGetContextCell() {
        System.out.println("getContextCell");
        int wordIndex = 0;
        int positionInWordIndex = 0;
        ContextCellContainer instance = new ContextCellContainer();
        ContextCell expResult = null;
        ContextCell result = instance.getContextCell(wordIndex, positionInWordIndex);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of removeContextCell method, of class ContextCellContainer.
     */
    @Test
    public void testRemoveContextCell() {
        System.out.println("removeContextCell");
        int wordIndex = 0;
        int positionInWordIndex = 0;
        ContextCellContainer instance = new ContextCellContainer();
        instance.removeContextCell(wordIndex, positionInWordIndex);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSize method, of class ContextCellContainer.
     */
    @Test
    public void testGetSize() {
        System.out.println("getSize");
        ContextCellContainer instance = new ContextCellContainer();
        int expResult = 0;
        int result = instance.getSize();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCells method, of class ContextCellContainer.
     */
    @Test
    public void testGetCells() {
        System.out.println("getCells");
        ContextCellContainer instance = new ContextCellContainer();
        List expResult = null;
        List result = instance.getCells();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}