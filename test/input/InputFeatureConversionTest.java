/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package input;

import etymology.config.Configuration;
import etymology.data.convert.ConversionRules;
import etymology.input.FeatureVocabulary;
import etymology.input.Input;
import java.io.File;
import java.util.Arrays;
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
public class InputFeatureConversionTest {
    private Input input;

    public InputFeatureConversionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() {
    }


    @Test
    public void testFeatureConversionFin() throws Exception {
        Configuration.getInstance().setUseFeatures(true);
        input = new Input(new File("test/starling-input-data.utf8"), Arrays.asList("FIN", "EST"));

        int languageId = input.getLanguageId("FIN");

        FeatureVocabulary featVoc = (FeatureVocabulary) input.getVocabulary(languageId);

        int wordIndex = featVoc.getWordIndex("aja");
        Assert.assertEquals("VlBn3,CWv+n1,VlBn3", featVoc.getWordAsStringFeatures(wordIndex));
    }

    @Test
    public void testFeatureConversionEst() throws Exception {
        Configuration.getInstance().setUseFeatures(true);
        input = new Input(new File("test/starling-input-data.utf8"), Arrays.asList("EST", "KHN"));
        int languageId = input.getLanguageId("EST");

        FeatureVocabulary featVoc = (FeatureVocabulary) input.getVocabulary(languageId);

        int wordIndex = featVoc.getWordIndex("āvtâtâk");
        Assert.assertEquals("VlBn5,CFl+n1,CPd-n1,VlBn5,CPd-n1,VlBn5,CPv-n1", featVoc.getWordAsStringFeatures(wordIndex));
    }
}
