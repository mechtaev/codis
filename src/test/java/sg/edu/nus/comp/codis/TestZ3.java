package sg.edu.nus.comp.codis;

import fj.data.Either;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 12/4/2016.
 */
public class TestZ3 {

    private static Z3 z3;

    @BeforeClass
    public static void initSolver() {
        z3 = new Z3();
    }

    @AfterClass
    public static void disposeSolver() {
        z3.dispose();
    }

    @Test
    public void testEquality() {
        ArrayList<Node> clauses = new ArrayList<>();
        clauses.add(new Equal(new ProgramVariable("x", IntType.TYPE), new ProgramVariable("y", IntType.TYPE)));
        clauses.add(new Equal(new ProgramVariable("x", IntType.TYPE), IntConst.of(5)));
        Optional<Map<Variable, Constant>> result = z3.getModel(clauses);
        assertTrue(result.isPresent());
        assertEquals(result.get().get(new ProgramVariable("y", IntType.TYPE)), IntConst.of(5));
    }
}
