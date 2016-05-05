package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
import org.apache.commons.lang3.tuple.Pair;
import sg.edu.nus.comp.codis.ast.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public abstract class Synthesis {

    public abstract Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<TestCase> testSuite,
                                                                                 Multiset<Node> components);

    public Optional<Node> synthesizeNode(List<TestCase> testSuite,
                                         Multiset<Node> components) {
        Optional<Pair<Program, Map<Parameter, Constant>>> result = synthesize(testSuite, components);
        if (!result.isPresent())
            return Optional.empty();

        return Optional.of(Traverse.substitute(result.get().getLeft().getSemantics(), result.get().getRight()));
    }

}
