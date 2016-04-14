package sg.edu.nus.comp.codis;

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

    private final ProgramVariable x = ProgramVariable.mkInt("x");
    private final ProgramVariable y = ProgramVariable.mkInt("y");

    @Test
    public void testParameter() {
        Parameter p = Parameter.mkInt("p");
        Node n = new Add(p, new Mult(x, IntConst.of(3)));
        Map<ProgramVariable, Node> assignment = new HashMap<>();
        assignment.put(x, IntConst.of(2));
        Node result = Evaluator.eval(n, assignment);
        assertEquals(new Add(p, IntConst.of(6)), result);
    }

    @Test
    public void testIntBool() {
        Node expr = new Or(new Greater(x, IntConst.of(2)), new Less(y, IntConst.of(5)));
        Map<ProgramVariable, Node> assignment = new HashMap<>();
        assignment.put(x, IntConst.of(2));
        assignment.put(y, IntConst.of(2));
        Node result = Evaluator.eval(expr, assignment);
        assertEquals(BoolConst.TRUE, result);
    }

}
