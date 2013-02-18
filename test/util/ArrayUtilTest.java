/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import etymology.util.ArrayUtil;
import java.lang.reflect.Array;
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
public class ArrayUtilTest {

    public ArrayUtilTest() {
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
    public void testNotArray() {
        int dim = ArrayUtil.getNumberOfDimensions(42);
        Assert.assertEquals(0, dim);
    }

    @Test
    public void testSingleDimArray() {
        int[] arr = new int[2];
        int dim = ArrayUtil.getNumberOfDimensions(arr);
        Assert.assertEquals(1, dim);
    }

    @Test
    public void testThreeDimArray() {
        int[][][] arr = (int[][][]) Array.newInstance(int.class, 2, 4, 6);
        int dim = ArrayUtil.getNumberOfDimensions(arr);
        Assert.assertEquals(3, dim);
    }

    @Test
    public void testMultiDimCreation() {
        for(int dim = 0; dim < 8; dim++) {



        }

    }
}
