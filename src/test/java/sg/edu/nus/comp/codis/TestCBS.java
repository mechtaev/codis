package sg.edu.nus.comp.codis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.*;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import javax.print.attribute.standard.MediaSize;
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
    private static Synthesis boundIntSynthesizer;
    private static Synthesis bvSynthesizer;

    @BeforeClass
    public static void initSolver() {
        Solver solver = MathSAT.buildSolver();
        intSynthesizer = new ComponentBasedSynthesis(solver, false, Optional.empty());
        boundIntSynthesizer = new ComponentBasedSynthesis(solver, false, Optional.of(0));
        bvSynthesizer = new ComponentBasedSynthesis(solver, true, Optional.empty());
    }

    private final ProgramVariable x = ProgramVariable.mkInt("x");
    private final ProgramVariable y = ProgramVariable.mkInt("y");
    private final ProgramVariable bvX = ProgramVariable.mkBV("x", 32);
    private final ProgramVariable bvY = ProgramVariable.mkBV("y", 32);

    @Test
    public void testVariableChoice() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(1)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment2, IntConst.of(1)));

        Optional<Pair<Program, Map<Parameter, Constant>>> result = intSynthesizer.synthesize(testSuite, components);
        assertTrue(result.isPresent());
        Node node = result.get().getLeft().getSemantics(result.get().getRight());
        assertEquals(node, x);
    }

    @Test
    public void testBound() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);
        components.add(Components.ADD);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(3));
        assignment1.put(y, IntConst.of(0));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(3)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment2, IntConst.of(3)));

        Optional<Pair<Program, Map<Parameter, Constant>>> result = intSynthesizer.synthesize(testSuite, components);
        assertTrue(result.isPresent());
        Optional<Pair<Program, Map<Parameter, Constant>>> boundedResult = boundIntSynthesizer.synthesize(testSuite, components);
        assertFalse(boundedResult.isPresent());
    }

    @Test
    public void testArithmetic() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);
        components.add(Components.ADD);
        components.add(Components.SUB);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(2)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment2, IntConst.of(3)));

        Optional<Pair<Program, Map<Parameter, Constant>>> result = intSynthesizer.synthesize(testSuite, components);
        assertTrue(result.isPresent());
        Node node = result.get().getLeft().getSemantics(result.get().getRight());
        assertTrue(node.equals(new Add(x, y)) || node.equals(new Add(y, x)));
    }

    @Test
    public void testArithmeticAndLogic() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);
        components.add(Components.GT);
        components.add(Components.GE);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(2));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment1, BoolConst.TRUE));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment2, BoolConst.FALSE));

        Map<ProgramVariable, Node> assignment3 = new HashMap<>();
        assignment3.put(x, IntConst.of(1));
        assignment3.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment3, BoolConst.FALSE));

        Optional<Pair<Program, Map<Parameter, Constant>>> result = intSynthesizer.synthesize(testSuite, components);
        assertTrue(result.isPresent());
        Node node = result.get().getLeft().getSemantics(result.get().getRight());
        assertEquals(node, new Greater(x, y));
    }

    @Test
    public void testITE() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);
        components.add(IntConst.of(0));
        components.add(IntConst.of(1));
        components.add(Components.GT);
        components.add(Components.ITE);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(3));
        assignment1.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(1)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(2));
        assignment2.put(y, IntConst.of(3));
        testSuite.add(TestCase.ofAssignment(assignment2, IntConst.of(0)));

        Map<ProgramVariable, Node> assignment3 = new HashMap<>();
        assignment3.put(x, IntConst.of(1));
        assignment3.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment3, IntConst.of(0)));

        Optional<Pair<Program, Map<Parameter, Constant>>> result = intSynthesizer.synthesize(testSuite, components);
        assertTrue(result.isPresent());
        Node node = result.get().getLeft().getSemantics(result.get().getRight());
        assertEquals(node, new ITE(new Greater(x, y), IntConst.of(1), IntConst.of(0)));
    }

    @Test
    public void testTypeRestrictions() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(new Minus(new Hole("a", IntType.TYPE, Constant.class)));

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(-1)));

        Optional<Pair<Program, Map<Parameter, Constant>>> result = intSynthesizer.synthesize(testSuite, components);
        assertFalse(result.isPresent());
    }

    @Test
    public void testBVArithmeticAndLogic() {
        Multiset<Node> components = HashMultiset.create();
        components.add(bvX);
        components.add(bvY);
        components.add(Components.BVUGT);
        components.add(Components.BVUGE);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(bvX, BVConst.ofLong(2, 32));
        assignment1.put(bvY, BVConst.ofLong(1, 32));
        testSuite.add(TestCase.ofAssignment(assignment1, BoolConst.TRUE));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(bvX, BVConst.ofLong(1, 32));
        assignment2.put(bvY, BVConst.ofLong(2, 32));
        testSuite.add(TestCase.ofAssignment(assignment2, BoolConst.FALSE));

        Map<ProgramVariable, Node> assignment3 = new HashMap<>();
        assignment3.put(bvX, BVConst.ofLong(1, 32));
        assignment3.put(bvY, BVConst.ofLong(1, 32));
        testSuite.add(TestCase.ofAssignment(assignment3, BoolConst.FALSE));

        Optional<Pair<Program, Map<Parameter, Constant>>> result = bvSynthesizer.synthesize(testSuite, components);
        assertTrue(result.isPresent());
        Node node = result.get().getLeft().getSemantics(result.get().getRight());
        assertEquals(node, new BVUnsignedGreater(bvX, bvY));
    }

}
