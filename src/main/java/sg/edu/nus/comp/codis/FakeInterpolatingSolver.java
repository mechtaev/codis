package sg.edu.nus.comp.codis;

import fj.data.Either;
import sg.edu.nus.comp.codis.ast.Constant;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.Variable;
import sg.edu.nus.comp.codis.ast.theory.BoolConst;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This is a temporary solution to avoid configuring SMT solver for bitvector interpolation
 */
public class FakeInterpolatingSolver implements InterpolatingSolver {

    private Solver solver;

    public FakeInterpolatingSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public Either<Map<Variable, Constant>, Node> getModelOrInterpolant(List<Node> leftClauses, List<Node> rightClauses) {
        List<Node> clauses = new ArrayList<>();
        clauses.addAll(leftClauses);
        clauses.addAll(rightClauses);
        Optional<Map<Variable, Constant>> result = solver.getModel(clauses);
        if (result.isPresent()) {
            return Either.left(result.get());
        } else {
            return Either.right(BoolConst.FALSE);
        }
    }

}
