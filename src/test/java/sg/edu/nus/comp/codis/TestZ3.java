package sg.edu.nus.comp.codis;

import fj.data.Either;
import org.junit.*;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 12/4/2016.
 */
public class TestZ3 {

    @Test
    public void testEquality() {
        ArrayList<Node> clauses = new ArrayList<>();
        ProgramVariable x = ProgramVariable.mkInt("x");
        ProgramVariable y = ProgramVariable.mkInt("y");
        clauses.add(new Equal(x, y));
        clauses.add(new Equal(x, IntConst.of(5)));
        Optional<Map<Variable, Constant>> result = Z3.getInstance().getModel(clauses);
        assertTrue(result.isPresent());
        assertEquals(result.get().get(y), IntConst.of(5));
    }

    @Test
    public void testUnsatCore() {
        ArrayList<Node> clauses = new ArrayList<>();
        ProgramVariable x = ProgramVariable.mkInt("x");
        ProgramVariable y = ProgramVariable.mkInt("y");
        ProgramVariable a = ProgramVariable.mkBool("a");
        ProgramVariable b = ProgramVariable.mkBool("b");
        clauses.add(new Equal(x, IntConst.of(1)));
        clauses.add(new Equal(y, IntConst.of(2)));
        clauses.add(new Or(a, new Equal(x, y)));
        clauses.add(new Or(b, new LessOrEqual(x, y)));
        ArrayList<Node> assumptions = new ArrayList<>();
        assumptions.add(new Not(a));
        assumptions.add(new Not(b));
        Either<Map<Variable, Constant>, ArrayList<Node>> unsatCore = Z3.getInstance().getModelOrCore(clauses, assumptions);
        assertTrue(unsatCore.isRight());
        assertTrue(unsatCore.right().value().contains(new Not(a)));
        assertFalse(unsatCore.right().value().contains(new Not(b)));
    }

    @Test
    public void testBitvectors() {
        ArrayList<Node> clauses = new ArrayList<>();
        ProgramVariable x = ProgramVariable.mkBV("x", 32);
        ProgramVariable y = ProgramVariable.mkBV("y", 32);
        clauses.add(new Equal(new BVAdd(x, y), BVConst.ofLong(5, 32)));
        clauses.add(new Equal(y, BVConst.ofLong(2, 32)));
        Optional<Map<Variable, Constant>> model = Z3.getInstance().getModel(clauses);
        assertTrue(model.isPresent());
        assertEquals(model.get().get(x), BVConst.ofLong(3, 32));
    }

}
