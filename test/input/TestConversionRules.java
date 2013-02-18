/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package input;

import etymology.data.convert.ConversionRules;
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
public class TestConversionRules {

    private ConversionRules conversionRules;

    public TestConversionRules() {
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
    public void testConversionRule() {
        conversionRules = new ConversionRules("FIN k x");
        String result = conversionRules.applyConversionRule("fin", "perkele");
        Assert.assertEquals("perxele", result);
    }


    @Test
    public void testMultiLineConversionRule() {
        conversionRules = new ConversionRules("FIN k x\nFIN p n");
        String result = conversionRules.applyConversionRule("fin", "perkele");
        Assert.assertEquals("nerxele", result);
    }
}
