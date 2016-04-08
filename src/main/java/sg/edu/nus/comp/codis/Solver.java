package sg.edu.nus.comp.codis;

import fj.data.Either;
import sg.edu.nus.comp.codis.ast.Constant;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.Variable;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public interface Solver {
    Either<Map<Variable, Constant>, ArrayList<Node>> solve(ArrayList<Node> clauses);
}
