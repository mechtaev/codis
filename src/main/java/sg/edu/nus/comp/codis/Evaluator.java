package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Equal;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class Evaluator {

    private Solver solver;

    public Evaluator(Solver solver) {
        this.solver = solver;
    }

    public Node eval(Node node, Map<ProgramVariable, ? extends Node> assignment) {
        ProgramVariable result = new ProgramVariable("<evaluationResult>", TypeInference.typeOf(node));
        ArrayList<Node> clauses = new ArrayList<>();
        clauses.add(new Equal(result, node));
        for (Map.Entry<ProgramVariable, ? extends Node> entry : assignment.entrySet()) {
            clauses.add(new Equal(entry.getKey(), entry.getValue()));
        }
        return solver.getModel(clauses).get().get(result);
    }

}
