package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Equal;

import java.util.List;
import java.util.Map;

/**
 * This tester checks if test passes using SMT solver
 */
public class SolverTester implements Tester {

    private Solver solver;

    public SolverTester(Solver solver) {
        this.solver = solver;
    }

    public boolean isPassing(Program program, Map<Parameter, Constant> parameterValuation, TestCase test) {
        Variable result = new ProgramOutput(test.getOutputType());
        List<Node> clauses = test.getConstraints(result);
        clauses.add(new Equal(program.getSemantics(parameterValuation), result));
        return solver.isSatisfiable(clauses);
    }
}
