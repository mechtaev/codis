package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.tuple.Pair;
import sg.edu.nus.comp.codis.CBS;
import sg.edu.nus.comp.codis.Synthesis;
import sg.edu.nus.comp.codis.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 2/5/2016.
 */
public class BoundedSynthesis extends Synthesis {

    private int bound;

    public BoundedSynthesis(int bound) {
        this.bound = bound;
    }

    @Override
    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<TestCase> testSuite,
                                                                        Map<Node, Integer> componentMultiset) {
        ArrayList<Component> components = CBS.flattenComponentMultiset(componentMultiset);
        return null;
    }

//    private Pair<List<Node>, Map<Selector, Component>> encodeBranch(BranchOutput output, int size, List<Component> components) {
//
//    }
//
//    private Program decode() {
//
//    }
}
