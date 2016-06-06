package sg.edu.nus.comp.codis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.junit.BeforeClass;
import org.junit.Test;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Add;
import sg.edu.nus.comp.codis.ast.theory.IntConst;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 5/5/2016.
 */
public class TestTreeBoundedSynthesis {
    private static TreeBoundedSynthesis synthesizer2;

    @BeforeClass
    public static void initSolver() {
        synthesizer2 = new TreeBoundedSynthesis(Z3.getInstance(), 2);
    }

    private final ProgramVariable x = ProgramVariable.mkInt("x");
    private final ProgramVariable y = ProgramVariable.mkInt("y");

    @Test
    public void testAddition() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);
        components.add(Components.ADD);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(2)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment2, IntConst.of(3)));

        Optional<Node> node = synthesizer2.synthesizeNode(testSuite, components);
        assertTrue(node.isPresent());
        assertTrue(node.get().equals(new Add(x, y)) || node.get().equals(new Add(y, x)));
    }

    @Test
    public void testForbiddenChoice() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);
        components.add(Components.ADD);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(2)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment2, IntConst.of(3)));

        List<Program> forbidden = new ArrayList<>();
        Map<Hole, Program> args = new HashMap<>();
        args.put((Hole)Components.ADD.getLeft(), Program.leaf(new Component(x)));
        args.put((Hole)Components.ADD.getRight(), Program.leaf(new Component(y)));
        forbidden.add(Program.app(new Component(Components.ADD), args));

        Optional<Node> node = synthesizer2.synthesizeNodeWithForbidden(testSuite, components, forbidden);
        assertTrue(node.isPresent());
        node.get().equals(new Add(y, x));
    }

    @Test
    public void testForbiddenAll() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);
        components.add(Components.ADD);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(2)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment2, IntConst.of(3)));

        List<Program> forbidden = new ArrayList<>();
        Map<Hole, Program> args = new HashMap<>();
        args.put((Hole)Components.ADD.getLeft(), Program.leaf(new Component(x)));
        args.put((Hole)Components.ADD.getRight(), Program.leaf(new Component(y)));
        Map<Hole, Program> args2 = new HashMap<>();
        args2.put((Hole)Components.ADD.getLeft(), Program.leaf(new Component(y)));
        args2.put((Hole)Components.ADD.getRight(), Program.leaf(new Component(x)));
        forbidden.add(Program.app(new Component(Components.ADD), args));
        forbidden.add(Program.app(new Component(Components.ADD), args2));

        Optional<Node> node = synthesizer2.synthesizeNodeWithForbidden(testSuite, components, forbidden);
        assertFalse(node.isPresent());

    }

    @Test
    public void testForbiddenNonexistent() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);
        components.add(Components.ADD);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(2)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment2, IntConst.of(3)));

        List<Program> forbidden = new ArrayList<>();
        Map<Hole, Program> args = new HashMap<>();
        args.put((Hole)Components.ADD.getLeft(), Program.leaf(new Component(x)));
        args.put((Hole)Components.ADD.getRight(), Program.leaf(new Component(y)));
        forbidden.add(Program.app(new Component(Components.SUB), args));

        Optional<Node> node = synthesizer2.synthesizeNodeWithForbidden(testSuite, components, forbidden);
        assertTrue(node.isPresent());
        assertTrue(node.get().equals(new Add(x, y)) || node.get().equals(new Add(y, x)));
    }

}
