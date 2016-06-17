package sg.edu.nus.comp.codis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import fj.data.Either;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Equal;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class CODIS extends SynthesisWithLearning {

    private Logger logger = LoggerFactory.getLogger(CEGIS.class);

    private Map<Multiset<Node>, Node> conflicts;
    private List<Triple<Node, TestCase, Map<Integer, Node>>> path;
    private int bound;
    private Tester tester;
    private InterpolatingSolver iSolver;

    public CODIS(Solver solver, InterpolatingSolver iSolver, int bound) {
        this.bound = bound;
        this.tester = new Tester(solver);
        this.iSolver = iSolver;
    }

    public static List<Component> getLeaves(Program p) {
        List<Component> current = new ArrayList<>();
        if (p.isLeaf()) {
            current.add(p.getRoot());
        } else {
            for (Program program : p.getChildren().values()) {
                current.addAll(getLeaves(program));
            }
        }
        return current;
    }

    public static Program substitute(Program p, Map<Component, Program> mapping) {
        if (p.isLeaf()) {
            if (mapping.containsKey(p.getRoot())) {
                return mapping.get(p.getRoot());
            } else {
                return p;
            }
        } else {
            return Program.app(p.getRoot(), p.getChildren().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> substitute(e.getValue(), mapping)
                    )));
        }
    }

    private Multiset<Node> remainingComponents(Multiset<Node> total, Program p, Component leaf) {
        Multiset<Node> result = HashMultiset.create(total);
        removeUsedComponents(result, p, leaf);
        return result;
    }

    private void removeUsedComponents(Multiset<Node> components, Program p, Component leaf) {
        if (!p.getRoot().equals(leaf)) {
            components.remove(p.getRoot().getSemantics());
        }
        for (Program program : p.getChildren().values()) {
            removeUsedComponents(components, program, leaf);
        }
    }


    class SynthesisContext extends TestCase {

        private TestCase outerTest;
        private Pair<Program, Map<Parameter, Constant>> context;

        public SynthesisContext(TestCase outerTest, Pair<Program, Map<Parameter, Constant>> context, Component leaf) {
            this.outerTest = outerTest;
            this.context = context;
            this.leaf = leaf;
        }

        private Component leaf;

        @Override
        public List<Node> getConstraints(Variable output) {
            List<Node> constraints = new ArrayList<>();
            ProgramVariable globalOutput = new ProgramVariable("<globalOutput>", outerTest.getOutputType());
            constraints.addAll(outerTest.getConstraints(globalOutput));
            Map<Component, Program> mapping = new HashMap<>();
            mapping.put(leaf, Program.leaf(new Component(output)));
            Node substituted = substitute(context.getLeft(), mapping).getSemantics(context.getRight());
            Node contextClause = new Equal(globalOutput, substituted);
            constraints.add(contextClause);
            return constraints;
        }

        @Override
        public Type getOutputType() {
            return TypeInference.typeOf(leaf);
        }
    }

    /**
     * TODO:
     * 1. Handle parameters more correctly (when they are parts of components)
     * 2. Memorize explored programs
     * 3. Ensure that each component is used once at each iteration
     * 4. Current procedure is not exhaustive
     */
    @Override
    public Either<Pair<Program, Map<Parameter, Constant>>, Node> synthesizeOrLearn(List<TestCase> testSuite,
                                                                                   Multiset<Node> components) {
        conflicts = new HashMap<>();
        TreeBoundedSynthesis initialSynthesizer = new TreeBoundedSynthesis(iSolver, 1, true);
        List<TestCase> initialTestSuite = new ArrayList<>();
        initialTestSuite.add(testSuite.get(0));
        Pair<Program, Map<Parameter, Constant>> initial = initialSynthesizer.synthesize(initialTestSuite, components).get();
        List<TestCase> fixed = new ArrayList<>();
        fixed.add(testSuite.get(0));
        return expand(initial, components, testSuite, fixed);
    }

    public Either<Pair<Program, Map<Parameter, Constant>>, Node> expand(Pair<Program, Map<Parameter, Constant>> last,
                                                                        Multiset<Node> components,
                                                                        List<TestCase> testSuite,
                                                                        List<TestCase> fixed) {
        List<TestCase> failing = new ArrayList<>();
        for (TestCase testCase : testSuite) {
            if (!tester.isPassing(last.getLeft(), last.getRight(), testCase)) {
                failing.add(testCase);
            }
        }
        logger.info("Current program: " + last.getLeft().getSemantics(last.getRight()));
        logger.info("Fixed/Failing/Total: " + fixed.size() + "/" + failing.size() + "/" + testSuite.size());
        if (failing.isEmpty()) {
            return Either.left(last);
        }
        for (Component leaf : getLeaves(last.getLeft())) {
            Either<Pair<Program, Map<Parameter, Constant>>, Node> result =
                    expandLeaf(last, leaf, components, testSuite, fixed, failing);
            if (result.isLeft()) {
                return result;
            }
        }
        return Either.right(new ProgramVariable("<unknown>", testSuite.get(0).getOutputType()));
    }

    public Either<Pair<Program, Map<Parameter, Constant>>, Node> expandLeaf(Pair<Program, Map<Parameter, Constant>> last,
                                                                            Component leaf,
                                                                            Multiset<Node> components,
                                                                            List<TestCase> testSuite,
                                                                            List<TestCase> fixed,
                                                                            List<TestCase> failing) {
        Multiset<Node> remaining = remainingComponents(components, last.getLeft(), leaf);
        for (TestCase failingTest : failing) {
            Either<Pair<Program, Map<Parameter, Constant>>, Node> result =
                    expandLeafForTest(last, leaf, remaining, testSuite, fixed, failingTest);
            if (result.isLeft()) {
                return result;
            }
        }
        return Either.right(new ProgramVariable("<unknown>", testSuite.get(0).getOutputType()));
    }

    public Either<Pair<Program, Map<Parameter, Constant>>, Node> expandLeafForTest(Pair<Program, Map<Parameter, Constant>> last,
                                                                               Component leaf,
                                                                               Multiset<Node> components,
                                                                               List<TestCase> testSuite,
                                                                               List<TestCase> fixed,
                                                                               TestCase failing) {
        List<TestCase> newFixed = new ArrayList<>();
        newFixed.addAll(fixed);
        newFixed.add(failing);
        List<TestCase> contextTestSuite = new ArrayList<>();
        for (TestCase testCase : newFixed) {
            contextTestSuite.add(new SynthesisContext(testCase, last, leaf));
        }
        TreeBoundedSynthesis synthesizer = new TreeBoundedSynthesis(iSolver, bound, true);
        Either<Pair<Program, Map<Parameter, Constant>>, Node> result =
                synthesizer.synthesizeOrLearn(contextTestSuite, components);
        if (result.isRight()) {
            conflicts.put(components, result.right().value());
            return result;
        }

        //logger.info("Base program: " + last.getLeft().getSemantics(last.getRight()));
        //logger.info("Leaf: " + leaf);

        Map<Parameter, Constant> newParameterValuation = new HashMap<>();
        newParameterValuation.putAll(last.getRight());
        newParameterValuation.putAll(result.left().value().getRight());

        Map<Component, Program> mapping = new HashMap<>();
        mapping.put(leaf, result.left().value().getLeft());
        Program newProgram = substitute(last.getLeft(), mapping);

        for (TestCase testCase : newFixed) {
            if (!tester.isPassing(newProgram, newParameterValuation, testCase)) {
                logger.error("Wrong expansion");
            }
        }

        return expand(new ImmutablePair<>(newProgram, newParameterValuation), components, testSuite, newFixed);
    }

}
