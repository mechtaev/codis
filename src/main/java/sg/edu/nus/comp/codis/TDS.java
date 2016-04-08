package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.Component;
import sg.edu.nus.comp.codis.ast.Node;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 *
 * Test-Driven Synthesis, PLDI'14
 */
public class TDS implements Synthesis {

    @Override
    public Optional<Node> synthesize(ArrayList<TestCase> testSuite, ArrayList<Component> components) {
        throw new UnsupportedOperationException();
    }

}
