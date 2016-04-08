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
    @Test
    public void testParameter() {
        Node n = new Add(new Parameter("p", IntType.TYPE),
                         new Mult(new ProgramVariable("x", IntType.TYPE), IntConst.of(3)));
        Map<ProgramVariable, Constant> assignment = new HashMap<>();
        assignment.put(new ProgramVariable("x", IntType.TYPE), IntConst.of(2));
        Node result = Evaluator.eval(n, assignment);
        assertEquals(new Add(new Parameter("p", IntType.TYPE), IntConst.of(6)), result);
    }

    @Test
    public void testIntBool() {
        ProgramVariable x = new ProgramVariable("x", IntType.TYPE);
        ProgramVariable y = new ProgramVariable("y", IntType.TYPE);
        Node expr = new Or(new Greater(x, IntConst.of(2)), new Less(y, IntConst.of(5)));
        Map<ProgramVariable, Constant> assignment = new HashMap<>();
        assignment.put(new ProgramVariable("x", IntType.TYPE), IntConst.of(2));
        assignment.put(new ProgramVariable("y", IntType.TYPE), IntConst.of(2));
        Node result = Evaluator.eval(expr, assignment);
        assertEquals(BoolConst.TRUE, result);
    }

}
