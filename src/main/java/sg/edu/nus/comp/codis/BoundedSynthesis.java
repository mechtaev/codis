package sg.edu.nus.comp.codis;

import org.apache.commons.lang3.tuple.Pair;
import sg.edu.nus.comp.codis.CBS;
import sg.edu.nus.comp.codis.Synthesis;
import sg.edu.nus.comp.codis.TestCase;
import sg.edu.nus.comp.codis.ast.*;

import java.util.*;

/**
 * Created by Sergey Mechtaev on 2/5/2016.
 */
public class BoundedSynthesis extends Synthesis {

    private int bound;
    private Solver solver;
    private Map<BranchOutput, List<BranchOutput>> tree;
    private Map<Selector, Component> selected;

    public BoundedSynthesis(Solver solver, int bound) {
        this.bound = bound;
        this.solver = solver;
    }

    @Override
    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<TestCase> testSuite,
                                                                        Map<Node, Integer> componentMultiset) {
        this.tree = new HashMap<>();
        this.selected = new HashMap<>();

        ArrayList<Component> components = CBS.flattenComponentMultiset(componentMultiset);
        BranchOutput root = new BranchOutput(testSuite.get(0).getOutputType());
        List<Node> clauses = encodeBranch(root, bound, components);
        Optional<Map<Variable, Constant>> assignment = solver.getModel(clauses);
        if (assignment.isPresent()) {
            return Optional.of(decode(assignment.get(), components, root));
        } else {
            return Optional.empty();
        }
    }

    private List<Node> encodeBranch(BranchOutput output, int size, List<Component> components) {
        if (size == 1) {
        }
        throw new UnsupportedOperationException();
    }

    private Pair<Program, Map<Parameter, Constant>> decode(Map<Variable, Constant> assignment,
                                                           ArrayList<Component> components,
                                                           BranchOutput root) {
        throw new UnsupportedOperationException();
    }

}
