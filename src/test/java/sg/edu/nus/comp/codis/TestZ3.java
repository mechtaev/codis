package sg.edu.nus.comp.codis;

import org.junit.*;
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
}
