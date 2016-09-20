package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.Constant;
import sg.edu.nus.comp.codis.ast.Parameter;
import sg.edu.nus.comp.codis.ast.Program;
import sg.edu.nus.comp.codis.ast.TestCase;

import java.util.Map;

/**
 * This tester checks if a tests passes through evaluation
 */
public class EvaluationTester implements Tester {
    @Override
    public boolean isPassing(Program program, Map<Parameter, Constant> parameterValuation, TestCase test) {
        throw new UnsupportedOperationException();
    }
}
