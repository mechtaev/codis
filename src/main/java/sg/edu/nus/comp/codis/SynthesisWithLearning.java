package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
import fj.data.Either;
import org.apache.commons.lang3.tuple.Pair;
import sg.edu.nus.comp.codis.ast.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 15/6/2016.
 */
public abstract class SynthesisWithLearning implements Synthesis {

    /**
     * @return either program and parameter valuation or conflict
     */
    public abstract Either<Pair<Program, Map<Parameter, Constant>>, Node> synthesizeOrLearn(List<? extends TestCase> testSuite,
                                                                                            Multiset<Node> components);


    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<? extends TestCase> testSuite,
                                                                        Multiset<Node> components) {
        Either<Pair<Program, Map<Parameter, Constant>>, Node> result = synthesizeOrLearn(testSuite, components);
        if (result.isRight()) {
            return Optional.empty();
        }

        return Optional.of(result.left().value());
    }

}
