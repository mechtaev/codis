package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
import org.apache.commons.lang3.tuple.Pair;
import sg.edu.nus.comp.codis.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 *
 * Test-Driven Synthesis, PLDI'14
 */
public class TDS extends Synthesis {

    @Override
    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<TestCase> testSuite,
                                                                        Multiset<Node> components) {
        throw new UnsupportedOperationException();
    }

}
