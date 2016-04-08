package sg.edu.nus.comp.codis;

import fj.test.Bool;
import org.junit.Test;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 8/4/2016.
 */
public class TestSimplifier {

    @Test
    public void testNoSimplification() {
        Node n = new Add(new ProgramVariable("x", IntType.TYPE), IntConst.of(2));
        Node s = Simplifier.symplify(n);
        assertEquals(n, s);
    }

    @Test
    public void testEvaluation() {
        Node n = new Add(IntConst.of(1), IntConst.of(2));
        Node s = Simplifier.symplify(n);
        assertEquals(IntConst.of(3), s);
    }

    @Test
    public void testArithmetic() {
        Node n = new Add(new Parameter("p", IntType.TYPE),
                         new Sub(new Mult(IntConst.of(1),
                                 new Parameter("a", IntType.TYPE)), new Parameter("a", IntType.TYPE)));
        Node s = Simplifier.symplify(n);
        assertEquals(new Parameter("p", IntType.TYPE), s);
    }

    @Test
    public void testLogic() {
        Node n = new Or(new Parameter("p", BoolType.TYPE),
                new Impl(BoolConst.TRUE,
                         new And(new Parameter("a", BoolType.TYPE), new Not(new Parameter("a", BoolType.TYPE)))));
        Node s = Simplifier.symplify(n);
        assertEquals(new Parameter("p", BoolType.TYPE), s);
    }

}
