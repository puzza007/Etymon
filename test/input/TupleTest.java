/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package input;

import etymology.input.Tuple;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class TupleTest {

    public TupleTest() {
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
    public void testTupleConstructor() {
        Tuple<String, Integer> t = new Tuple("testi", 42);
    }

    @Test
    public void testConstructionOfTuplesFromGivenCollectionHasCorrectAmountOfTuples() {
        List<Integer> ids = Arrays.asList(4, 5, 1, 2, 8);

        List<Tuple> tuples = Tuple.getAsPairs(ids);
        Assert.assertTrue(tuples.size() == 10);
    }

    @Test
    public void testTupleEquals() {
        List<Integer> ids = Arrays.asList(4, 5);
        List<Tuple> tuples = Tuple.getAsPairs(ids);

        Tuple t = new Tuple(4, 5);
        Tuple generated = tuples.get(0);
        Assert.assertTrue(t.equals(generated));
    }


    @Test
    public void testRemovingTuples() {
        List<Integer> ids = Arrays.asList(4, 5, 1);
        
        Set<Tuple> expectedTuples = new HashSet();
        expectedTuples.add(new Tuple(4, 5));
        expectedTuples.add(new Tuple(4, 1));
        expectedTuples.add(new Tuple(5, 1));

        expectedTuples.removeAll(Tuple.getAsPairs(ids));

        Assert.assertTrue(expectedTuples.isEmpty());
    }
}