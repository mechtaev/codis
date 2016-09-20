package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;

import java.util.Map;

import static sg.edu.nus.comp.codis.ast.Traverse.substitute;

/**
 * This tester checks if a test passes through evaluation
 * For this purpose we use simplifier that works only for integer arithmetic expressions
 */
public class EvaluationTester implements Tester {
    @Override
    public boolean isPassing(Program program, Map<Parameter, Constant> parameterValuation, TestCase test) {
        if (test instanceof AssignmentTestCase) {
            Node node = Traverse.substitute(program.getSemantics(parameterValuation), ((AssignmentTestCase) test).getAssignment());
            return Simplifier.simplify(node).equals(((AssignmentTestCase) test).getOutputValue());
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
