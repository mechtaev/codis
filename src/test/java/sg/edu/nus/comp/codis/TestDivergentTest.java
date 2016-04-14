package sg.edu.nus.comp.codis;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.BeforeClass;
import org.junit.Test;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.BoolConst;
import sg.edu.nus.comp.codis.ast.theory.IntConst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 15/4/2016.
 */
public class TestDivergentTest {

    private static DivergentTest generator;

    @BeforeClass
    public static void initSolver() {
        generator = new DivergentTest(Z3.getInstance());
    }

    private final ProgramVariable x = ProgramVariable.mkInt("x");
    private final ProgramVariable y = ProgramVariable.mkInt("y");

    @Test
    public void testInequalities() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        componentMultiset.put(x, 1);
        componentMultiset.put(y, 1);
        componentMultiset.put(Components.GT, 1);
        componentMultiset.put(Components.GE, 1);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(2));
        testSuite.add(new TestCase(assignment1, BoolConst.FALSE));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(2));
        assignment2.put(y, IntConst.of(1));
        testSuite.add(new TestCase(assignment2, BoolConst.TRUE));

        Optional<Triple<TestCase, Node, Node>> result = generator.generate(componentMultiset, testSuite);

        assertTrue(result.isPresent());
        assertEquals(result.get().getLeft().getAssignment().get(x), result.get().getLeft().getAssignment().get(y));
    }

}
