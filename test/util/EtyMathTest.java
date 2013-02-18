/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import etymology.util.EtyMath;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class EtyMathTest {

    public EtyMathTest() {
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

    @Test
    public void testLogFactorial() {
        double result = EtyMath.base2LogFactorial(1);
        Assert.assertEquals(result, 0, 0);

        result = EtyMath.base2LogFactorial(5);
        Assert.assertEquals(result, 6.906891, 0.0001);

        result = EtyMath.base2LogFactorial(3);
        Assert.assertEquals(result, 2.584963, 0.0001);
    }

    @Test
    public void testLogFactorialAgainstLispResults() {
        Map<Integer, Double> valueToExpectedResultMap = new HashMap();
        valueToExpectedResultMap.put(3, 2.5849624);
        valueToExpectedResultMap.put(5, 6.9068904);
        valueToExpectedResultMap.put(600, 4677.616);
        valueToExpectedResultMap.put(6000, 66655.88);
        valueToExpectedResultMap.put(60000, 865821.9);
        valueToExpectedResultMap.put(1, 0.0);

        for (int factorialOn : valueToExpectedResultMap.keySet()) {
            double expected = valueToExpectedResultMap.get(factorialOn);
            double delta = getDelta(expected);
            Assert.assertEquals(EtyMath.base2LogFactorial(factorialOn), expected, delta);
        }
    }

    @Test
    public void testVariance() {
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        Assert.assertEquals(2.91666666, EtyMath.getVariance(data), 0.0001);
    }

    @Test
    public void testAbsoluteDeviation() {
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        Assert.assertEquals(1.5, EtyMath.getExpectedAbsoluteDeviation(data), 0.000001);
    }

    @Test
    public void testStandardDeviation() {
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        Assert.assertEquals(1.7078251, EtyMath.getStandardDeviation(data), 0.00001);
    }

    @Test
    public void testMean() {
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
        Assert.assertEquals(3.5, EtyMath.getMean(data), 0.0000001);
    }

    private double getDelta(double forValue) {
        int val = (int) Math.round(forValue);
        String s = "" + val;
        return Math.pow(10, s.length() - 4);
    }
}
