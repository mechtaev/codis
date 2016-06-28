package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
import org.apache.commons.lang3.tuple.Pair;
import sg.edu.nus.comp.codis.ast.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 *
 * Test-Driven Synthesis, PLDI'14
 */
public class TestDrivenSynthesis implements Synthesis {

    @Override
    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<? extends TestCase> testSuite,
                                                                        Multiset<Node> components) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Pair<Program, Map<Parameter, Constant>>> synthesizeAll(List<? extends TestCase> testSuite, Multiset<Node> components) {
        throw new UnsupportedOperationException();
    }

}
