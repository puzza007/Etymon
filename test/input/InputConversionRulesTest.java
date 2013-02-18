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
public class InputConversionRulesTest {

    private ConversionRules conversionRules;
    private Input input;

    public InputConversionRulesTest() {
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    @Test
    public void testConversionRulesReadFromFileFinnish() throws Exception {
        Configuration.getInstance().setUseFeatures(false);
        conversionRules = new ConversionRules(new File("test/conversionrulesample"));
        input = new Input(new File("test/starling-input-data.utf8"), Arrays.asList("FIN", "EST"), conversionRules);

        int languageId = input.getLanguageId("FIN");

        for(String word: input.getVocabulary(languageId).getWords()) {
            if(word.contains("j")) {
                Assert.fail();
            }
        }
    }

    @Test
    public void testConversionRulesFinnish() throws Exception {
        Configuration.getInstance().setUseFeatures(false);
        conversionRules = new ConversionRules("FIN  j   k\nFIN  a  o");
        input = new Input(new File("test/starling-input-data.utf8"), Arrays.asList("FIN", "EST"), conversionRules);


        int languageId = input.getLanguageId("FIN");

        for(String word: input.getVocabulary(languageId).getWords()) {
            if(word.contains("j")) {
                Assert.fail();
            }

            if(word.contains("a")) {
                Assert.fail();
            }
        }
    }

    @Test
    public void testConversionRulesReadFromFileEstonian() throws Exception {
        Configuration.getInstance().setUseFeatures(false);
        conversionRules = new ConversionRules(new File("test/conversionrulesample"));
        input = new Input(new File("test/starling-input-data.utf8"), Arrays.asList("FIN", "EST"), conversionRules);

        int languageId = input.getLanguageId("EST");

        for(String word: input.getVocabulary(languageId).getWords()) {
            if(word.contains("ka")) {
                Assert.fail();
            }
        }
    }
}
