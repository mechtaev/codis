package sg.edu.nus.comp.codis;

import org.junit.*;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 13/4/2016.
 */
public class TestCBS {

    private static Synthesis intSynthesizer;
    private static Synthesis bvSynthesizer;

    @BeforeClass
    public static void initSolver() {
        intSynthesizer = new CBS(Z3.getInstance(), false);
        bvSynthesizer = new CBS(Z3.getInstance(), true);
    }

    private final ProgramVariable x = ProgramVariable.mkInt("x");
    private final ProgramVariable y = ProgramVariable.mkInt("y");
    private final ProgramVariable bvX = ProgramVariable.mkBV("x", 32);
    private final ProgramVariable bvY = ProgramVariable.mkBV("y", 32);

    @Test
    public void testVariableChoice() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        componentMultiset.put(x, 1);
        componentMultiset.put(y, 1);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(new TestCase(assignment1, IntConst.of(1)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(new TestCase(assignment2, IntConst.of(1)));

        Optional<Node> node = intSynthesizer.synthesize(testSuite, componentMultiset);
        assertTrue(node.isPresent());
        assertEquals(node.get(), x);
    }

    @Test
    public void testArithmetic() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        componentMultiset.put(x, 1);
        componentMultiset.put(y, 1);
        componentMultiset.put(Components.ADD, 1);
        componentMultiset.put(Components.SUB, 1);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(new TestCase(assignment1, IntConst.of(2)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(new TestCase(assignment2, IntConst.of(3)));

        Optional<Node> node = intSynthesizer.synthesize(testSuite, componentMultiset);
        assertTrue(node.isPresent());
        assertTrue(node.get().equals(new Add(x, y)) || node.get().equals(new Add(y, x)));
    }

    @Test
    public void testArithmeticAndLogic() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        componentMultiset.put(x, 1);
        componentMultiset.put(y, 1);
        componentMultiset.put(Components.GT, 1);
        componentMultiset.put(Components.GE, 1);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(2));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(new TestCase(assignment1, BoolConst.TRUE));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(new TestCase(assignment2, BoolConst.FALSE));

        Map<ProgramVariable, Node> assignment3 = new HashMap<>();
        assignment3.put(x, IntConst.of(1));
        assignment3.put(y, IntConst.of(1));
        testSuite.add(new TestCase(assignment3, BoolConst.FALSE));

        Optional<Node> node = intSynthesizer.synthesize(testSuite, componentMultiset);
        assertTrue(node.isPresent());
        assertEquals(node.get(), new Greater(x, y));
    }

    @Test
    public void testITE() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        componentMultiset.put(x, 1);
        componentMultiset.put(y, 1);
        componentMultiset.put(IntConst.of(0), 1);
        componentMultiset.put(IntConst.of(1), 1);
        componentMultiset.put(Components.GT, 1);
        componentMultiset.put(Components.ITE, 1);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(3));
        assignment1.put(y, IntConst.of(2));
        testSuite.add(new TestCase(assignment1, IntConst.of(1)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(2));
        assignment2.put(y, IntConst.of(3));
        testSuite.add(new TestCase(assignment2, IntConst.of(0)));

        Map<ProgramVariable, Node> assignment3 = new HashMap<>();
        assignment3.put(x, IntConst.of(1));
        assignment3.put(y, IntConst.of(1));
        testSuite.add(new TestCase(assignment3, IntConst.of(0)));

        Optional<Node> node = intSynthesizer.synthesize(testSuite, componentMultiset);
        assertTrue(node.isPresent());
        assertEquals(node.get(), new ITE(new Greater(x, y), IntConst.of(1), IntConst.of(0)));
    }

    @Test
    public void testTypeRestrictions() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        componentMultiset.put(x, 1);
        componentMultiset.put(new Minus(new Hole("a", IntType.TYPE, Constant.class)), 1);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        testSuite.add(new TestCase(assignment1, IntConst.of(-1)));

        Optional<Node> node = intSynthesizer.synthesize(testSuite, componentMultiset);
        assertFalse(node.isPresent());
    }

    @Test
    public void testBVArithmeticAndLogic() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        componentMultiset.put(bvX, 1);
        componentMultiset.put(bvY, 1);
        componentMultiset.put(Components.BVUGT, 1);
        componentMultiset.put(Components.BVUGE, 1);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(bvX, BVConst.ofLong(2, 32));
        assignment1.put(bvY, BVConst.ofLong(1, 32));
        testSuite.add(new TestCase(assignment1, BoolConst.TRUE));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(bvX, BVConst.ofLong(1, 32));
        assignment2.put(bvY, BVConst.ofLong(2, 32));
        testSuite.add(new TestCase(assignment2, BoolConst.FALSE));

        Map<ProgramVariable, Node> assignment3 = new HashMap<>();
        assignment3.put(bvX, BVConst.ofLong(1, 32));
        assignment3.put(bvY, BVConst.ofLong(1, 32));
        testSuite.add(new TestCase(assignment3, BoolConst.FALSE));

        Optional<Node> node = bvSynthesizer.synthesize(testSuite, componentMultiset);
        assertTrue(node.isPresent());
        assertEquals(node.get(), new BVUnsignedGreater(bvX, bvY));
    }

}
