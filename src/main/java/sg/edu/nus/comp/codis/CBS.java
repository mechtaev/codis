package sg.edu.nus.comp.codis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.Component;
import sg.edu.nus.comp.codis.ast.Constant;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.Variable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 *
 * Oracle-guided Component-based Program Synthesis, ICSE'10
 */
public class CBS implements Synthesis {

    private Logger logger = LoggerFactory.getLogger(CBS.class);

    private Solver solver;

    public CBS(Solver solver) {
        this.solver = solver;
    }

    @Override
    public Optional<Node> synthesize(ArrayList<TestCase> testSuite, ArrayList<Component> components) {
        logger.warn("FOO");
        throw new UnsupportedOperationException();
    }

    private ArrayList<Node> encode(ArrayList<TestCase> testSuite, ArrayList<Component> components) {
        throw new UnsupportedOperationException();
    }

    private Node decode(Map<Variable, Constant> assignment) {
        throw new UnsupportedOperationException();
    }
}
