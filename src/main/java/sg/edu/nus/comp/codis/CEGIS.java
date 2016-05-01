package sg.edu.nus.comp.codis;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.Constant;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.Parameter;
import sg.edu.nus.comp.codis.ast.Program;

import java.util.*;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class CEGIS extends Synthesis {

    private Synthesis synthesizer;

    private Tester tester;

    private Logger logger = LoggerFactory.getLogger(CEGIS.class);

    public CEGIS(Synthesis synthesizer, Solver solver) {
        this.synthesizer = synthesizer;
        this.tester = new Tester(solver);
    }

    @Override
    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<TestCase> testSuite,
                                                                        Map<Node, Integer> componentMultiset) {
        assert testSuite.size() > 0;

        Set<TestCase> remaining = new HashSet<>(testSuite);
        Set<TestCase> current = new HashSet<>();

        Optional<TestCase> counterExample = Optional.of(testSuite.get(0));

        Optional<Pair<Program, Map<Parameter, Constant>>> result = Optional.empty();

        while(counterExample.isPresent()) {
            current.add(counterExample.get());
            logger.info("Adding test " + counterExample.get());

            remaining.remove(counterExample.get());
            counterExample = Optional.empty();

            result = synthesizer.synthesize(new ArrayList<>(current), componentMultiset);

            if (!result.isPresent()) {
                logger.info("Failed");
                break;
            }

            logger.info("Synthesized program: " + result.get().getLeft().getSemantics(result.get().getRight()));

            boolean counterExampleFound = false;
            int score = current.size();
            for (TestCase testCase : remaining) {
                if (!tester.isPassing(result.get().getLeft(), result.get().getRight(), testCase)) {
                    if (!counterExampleFound) {
                        counterExample = Optional.of(testCase);
                        counterExampleFound = true;
                    }
                } else {
                    score++;
                }
            }
            logger.info("Score: " + score + "/" + testSuite.size());
        }

        logger.info("Succeeded");

        return result;
    }
}
