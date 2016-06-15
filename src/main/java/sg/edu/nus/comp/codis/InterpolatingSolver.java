package sg.edu.nus.comp.codis;

import fj.data.Either;
import sg.edu.nus.comp.codis.ast.Constant;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.Variable;

import java.util.List;
import java.util.Map;

/**
 * Created by Sergey Mechtaev on 15/6/2016.
 */
public interface InterpolatingSolver {
    Either<Map<Variable, Constant>, Node> getModelOrInterpolant(List<Node> leftClauses, List<Node> rightClauses);
}
