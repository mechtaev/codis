package sg.edu.nus.comp.codis;

import fj.test.Bool;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 13/4/2016.
 */
public class TestCBS {

    private static Z3 z3;
    private static Synthesis synthesizer;

    private static Hole i = new Hole("i", IntType.TYPE, Node.class);
    private static Hole j = new Hole("j", IntType.TYPE, Node.class);
    private static Hole a = new Hole("a", BoolType.TYPE, Node.class);
    private static Hole b = new Hole("b", BoolType.TYPE, Node.class);

    @BeforeClass
    public static void initSolver() {
        z3 = new Z3();
        synthesizer = new CBS(z3);
    }

    @AfterClass
    public static void disposeSolver() {
        z3.dispose();
    }

    @Test
    public void testVariableChoice() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        ProgramVariable x = new ProgramVariable("x", IntType.TYPE);
        ProgramVariable y = new ProgramVariable("y", IntType.TYPE);
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

        Optional<Node> node = synthesizer.synthesize(testSuite, componentMultiset);
        assertTrue(node.isPresent());
        assertEquals(node.get(), x);
    }

    @Test
    public void testArithmetic() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        ProgramVariable x = new ProgramVariable("x", IntType.TYPE);
        ProgramVariable y = new ProgramVariable("y", IntType.TYPE);
        componentMultiset.put(x, 1);
        componentMultiset.put(y, 1);
        componentMultiset.put(new Add(i, j), 1);
        componentMultiset.put(new Sub(i, j), 1);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(new TestCase(assignment1, IntConst.of(2)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(new TestCase(assignment2, IntConst.of(3)));

        Optional<Node> node = synthesizer.synthesize(testSuite, componentMultiset);
        assertTrue(node.isPresent());
        assertTrue(node.get().equals(new Add(x, y)) || node.get().equals(new Add(y, x)));
    }

    @Test
    public void testArithmeticAndLogic() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        ProgramVariable x = new ProgramVariable("x", IntType.TYPE);
        ProgramVariable y = new ProgramVariable("y", IntType.TYPE);
        componentMultiset.put(x, 1);
        componentMultiset.put(y, 1);
        componentMultiset.put(new Greater(i, j), 1);
        componentMultiset.put(new GreaterOrEqual(i, j), 1);

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
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(1));
        testSuite.add(new TestCase(assignment3, BoolConst.FALSE));

        Optional<Node> node = synthesizer.synthesize(testSuite, componentMultiset);
        assertTrue(node.isPresent());
        assertEquals(node.get(), new Greater(x, y));
    }


}
