package sg.edu.nus.comp.codis;

import org.junit.BeforeClass;
import org.junit.Test;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.ProgramVariable;
import sg.edu.nus.comp.codis.ast.theory.Add;
import sg.edu.nus.comp.codis.ast.theory.IntConst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 5/5/2016.
 */
public class TestBoundedSynthesis {
    private static BoundedSynthesis synthesizer2;

    @BeforeClass
    public static void initSolver() {
        synthesizer2 = new BoundedSynthesis(Z3.getInstance(), 2);
    }

    private final ProgramVariable x = ProgramVariable.mkInt("x");
    private final ProgramVariable y = ProgramVariable.mkInt("y");

    @Test
    public void testVariableChoice() {
        Map<Node, Integer> componentMultiset = new HashMap<>();
        componentMultiset.put(x, 1);
        componentMultiset.put(y, 1);
        componentMultiset.put(Components.ADD, 1);

        ArrayList<TestCase> testSuite = new ArrayList<>();
        Map<ProgramVariable, Node> assignment1 = new HashMap<>();
        assignment1.put(x, IntConst.of(1));
        assignment1.put(y, IntConst.of(1));
        testSuite.add(TestCase.ofAssignment(assignment1, IntConst.of(2)));

        Map<ProgramVariable, Node> assignment2 = new HashMap<>();
        assignment2.put(x, IntConst.of(1));
        assignment2.put(y, IntConst.of(2));
        testSuite.add(TestCase.ofAssignment(assignment2, IntConst.of(3)));

        Optional<Node> node = synthesizer2.synthesizeNode(testSuite, componentMultiset);
        assertTrue(node.isPresent());
        assertTrue(node.get().equals(new Add(x, y)) || node.get().equals(new Add(y, x)));
    }

}
