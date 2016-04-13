package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.Component;
import sg.edu.nus.comp.codis.ast.Node;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public interface Synthesis {

    Optional<Node> synthesize(ArrayList<TestCase> testSuite, Map<Node, Integer> componentMultiset);

}
