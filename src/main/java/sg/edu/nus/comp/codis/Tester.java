package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;

import java.util.Map;

/**
 * Created by Sergey Mechtaev on 20/9/2016.
 */
interface Tester {
    boolean isPassing(Program program, Map<Parameter, Constant> parameterValuation, TestCase test);
}
