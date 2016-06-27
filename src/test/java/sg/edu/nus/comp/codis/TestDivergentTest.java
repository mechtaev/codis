package sg.edu.nus.comp.codis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.BeforeClass;
import org.junit.Test;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.BoolConst;
import sg.edu.nus.comp.codis.ast.theory.IntConst;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 15/4/2016.
 */
public class TestDivergentTest {

    private static DivergentTest generator;
    private static Solver solver;

    @BeforeClass
    public static void initSolver() {
        solver = MathSAT.buildSolver();
        generator = new DivergentTest(solver);
    }

    private final ProgramVariable x = ProgramVariable.mkInt("x");
    private final ProgramVariable y = ProgramVariable.mkInt("y");

    @Test
    public void testInequalities() {
        Multiset<Node> components = HashMultiset.create();
        components.add(x);
        components.add(y);
        components.add(Components.GT);
        components.add(Components.GE);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment1, BoolConst.FALSE));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(2));
        assignment2.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment2, BoolConst.TRUE));

        Optional<Triple<TestCase, Node, Node>> result = generator.generate(components, testSuite, Arrays.asList(x, y));

        assertTrue(result.isPresent());
        Selector dummy = new Selector();
        Map<Variable, Constant> model = solver.getModel(result.get().getLeft().getConstraints(dummy)).get();
        assertEquals(model.get(x), model.get(y));
    }

}
