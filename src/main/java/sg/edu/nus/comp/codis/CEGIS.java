package sg.edu.nus.comp.codis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.Node;

import java.util.*;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class CEGIS implements Synthesis {

    private Synthesis synthesizer;

    private Logger logger = LoggerFactory.getLogger(CBS.class);

    public CEGIS(Synthesis synthesizer) {
        this.synthesizer = synthesizer;
    }

    @Override
    public Optional<Node> synthesize(ArrayList<TestCase> testSuite, Map<Node, Integer> componentMultiset) {
        assert testSuite.size() > 0;

        Set<TestCase> remaining = new HashSet<>(testSuite);
        Set<TestCase> current = new HashSet<>();

        Optional<TestCase> counterExample = Optional.of(testSuite.get(0));

        Optional<Node> node = Optional.empty();

        while(counterExample.isPresent()) {
            current.add(counterExample.get());
            logger.info("Adding test " + counterExample.get());

            remaining.remove(counterExample.get());
            counterExample = Optional.empty();

            node = synthesizer.synthesize(new ArrayList<>(current), componentMultiset);

            if (!node.isPresent()) {
                logger.info("Failed");
                break;
            }

            logger.info("Synthesized program: " + node.get());
            logger.info("Simplified: " + Simplifier.simplify(node.get()));

            boolean counterExampleFound = false;
            int score = current.size();
            for (TestCase testCase : remaining) {
                if (!Evaluator.eval(node.get(), testCase.getAssignment()).equals(testCase.getOutput())) {
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

        return node;
    }
}
