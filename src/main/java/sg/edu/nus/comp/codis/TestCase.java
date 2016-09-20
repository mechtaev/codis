package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.Type;
import sg.edu.nus.comp.codis.ast.Variable;

import java.util.List;

/**
 * Represent constraints over inputs and output. Defines physical equality.
 */
public interface TestCase {
    List<Node> getConstraints(Variable output);

    Type getOutputType();
}
