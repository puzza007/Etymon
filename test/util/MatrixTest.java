/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package util;

import etymology.util.Matrix;
import java.util.ArrayList;
import java.util.List;
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
public class MatrixTest {

    public MatrixTest() {
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
    public void testMatrixConstruction() {
        Matrix<String> m = new Matrix(String.class, 3);
    }
    
    @Test
    public void testThreeDimStringMatrixSet() {
        Matrix<String> m = new Matrix(String.class, 3, 5, 3);
        String expected = "Foo bar";

        m.setCell(expected, 0, 1, 0);

        List<Integer> indexesInList = new ArrayList();
        indexesInList.add(0);
        indexesInList.add(1);
        indexesInList.add(0);
        String result = m.getCell(indexesInList);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testFailSetCell() {
        Matrix<String> m = new Matrix(String.class, 3);
        boolean failed = false;

        try {
            m.setCell("No such dimension", 0, 0);
        } catch (IllegalArgumentException e) {
            failed = true;
        }

        Assert.assertEquals(true, failed);
    }


    @Test
    public void testFailSetCellAtNegativeIndex() {
        Matrix<String> m = new Matrix(String.class, 3);
        boolean failed = false;

        try {
            m.setCell("No such index, should fail..", -1);
        } catch (IllegalArgumentException e) {
            failed = true;
        }

        Assert.assertEquals(true, failed);
    }

    @Test
    public void testSuccessSetAndGetCellAtZeroIndex() {
        Matrix<String> m = new Matrix(String.class, 3);
        m.setCell("Pewpew", 0);

        Assert.assertEquals("Pewpew", m.getCell(0));
    }
}