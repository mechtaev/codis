package sg.edu.nus.comp.codis;

import org.junit.BeforeClass;
import org.junit.Test;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by Sergey Mechtaev on 8/4/2016.
 */
public class TestEvaluator {

    private static Evaluator evaluator;

    @BeforeClass
    public static void initSolver() {
        evaluator = new Evaluator(MathSAT.buildSolver());
    }


    private final ProgramVariable x = ProgramVariable.mkInt("x");
    private final ProgramVariable y = ProgramVariable.mkInt("y");
    private final ProgramVariable v = ProgramVariable.mkBV("v", 32);

    @Test
    public void testBitvectors() {
        Node n = new BVAdd(BVConst.ofLong(3, 32), new BVMult(v, BVConst.ofLong(2, 32)));
        Map<ProgramVariable, Node> assignment = new HashMap<>();
        assignment.put(v, BVConst.ofLong(2, 32));
        Node result = evaluator.eval(n, assignment);
        assertEquals(BVConst.ofLong(7, 32), result);
    }

    @Test
    public void testIntBool() {
        Node expr = new Or(new Greater(x, IntConst.of(2)), new Less(y, IntConst.of(5)));
        Map<ProgramVariable, Node> assignment = new HashMap<>();
        assignment.put(x, IntConst.of(2));
        assignment.put(y, IntConst.of(2));
        Node result = evaluator.eval(expr, assignment);
        assertEquals(BoolConst.TRUE, result);
    }

}
