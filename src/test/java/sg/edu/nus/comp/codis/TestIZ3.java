package sg.edu.nus.comp.codis;

import fj.data.Either;
import org.junit.*;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 12/4/2016.
 */
public class TestIZ3 {

    @Test
    public void testInequality() {
        ProgramVariable a = ProgramVariable.mkInt("a");
        ProgramVariable b = ProgramVariable.mkInt("b");
        ProgramVariable c = ProgramVariable.mkInt("c");
        ProgramVariable d = ProgramVariable.mkInt("d");

        ArrayList<Node> left = new ArrayList<>();
        ArrayList<Node> right = new ArrayList<>();

        left.add(new Less(a, b));
        left.add(new Less(b, c));

        right.add(new Less(c, d));
        right.add(new Less(d, a));

        Either<Map<Variable, Constant>, Node> result = Z3.getInstance().getModelOrInterpolant(left, right);
        assertTrue(result.isRight());
        //TODO: can check more here
    }

    @Test
    public void testEquality() {
        ProgramVariable a = ProgramVariable.mkInt("a");
        ProgramVariable b = ProgramVariable.mkInt("b");

        ArrayList<Node> left = new ArrayList<>();
        ArrayList<Node> right = new ArrayList<>();

        left.add(new Equal(a, IntConst.of(1)));
        left.add(new Equal(b, IntConst.of(0)));

        right.add(new Less(a, b));

        Either<Map<Variable, Constant>, Node> result = Z3.getInstance().getModelOrInterpolant(left, right);
        assertTrue(result.isRight());
        //TODO: can check more here
    }



}
