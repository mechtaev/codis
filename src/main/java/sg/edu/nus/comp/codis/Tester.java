package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Equal;

import java.util.List;
import java.util.Map;

/**
 * Created by Sergey Mechtaev on 2/5/2016.
 */
public class Tester {

    private Solver solver;

    public Tester(Solver solver) {
        this.solver = solver;
    }

    public boolean isPassing(Program program, Map<Parameter, Constant> parameterValuation, TestCase test) {
        ProgramVariable result = new ProgramVariable("<testResult>", test.getOutputType());
        List<Node> clauses = test.getConstraints(result);
        clauses.add(new Equal(program.getSemantics(), result));
        return solver.getModel(clauses).isPresent();
    }
}
