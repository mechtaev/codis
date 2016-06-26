package sg.edu.nus.comp.codis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import fj.data.Either;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.BoolConst;
import sg.edu.nus.comp.codis.ast.theory.Equal;
import sg.edu.nus.comp.codis.ast.theory.Not;

import java.util.*;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class CODIS extends SynthesisWithLearning {

    private Logger logger = LoggerFactory.getLogger(CODIS.class);

    private Tester tester;
    private InterpolatingSolver iSolver;
    private Solver solver;

    private CODISConfig config;

    public CODIS(Solver solver, InterpolatingSolver iSolver, CODISConfig config) {
        this.solver = solver;
        this.config = config;
        this.tester = new Tester(solver);
        this.iSolver = iSolver;
    }

    private Multiset<Node> remainingComponents(Multiset<Node> total, Program p) {
        Multiset<Node> result = HashMultiset.create(total);
        removeUsedComponents(result, p);
        return result;
    }

    private void removeUsedComponents(Multiset<Node> components, Program p) {
        components.remove(p.getRoot().getSemantics());
        for (Program program : p.getChildren().values()) {
            removeUsedComponents(components, program);
        }
    }

    private class SearchTreeNode {
        private Pair<Program, Map<Parameter, Constant>> program;
        private Multiset<Node> remainingComponents;
        private List<TestCase> fixed;
        private List<TestCase> failing;
        private Component leaf;
        private List<Component> remainingLeaves;
        private TestCase test;
        private List<TestCase> remainingTests;
        private List<Program> explored;

        SearchTreeNode(Pair<Program, Map<Parameter, Constant>> program,
                       Multiset<Node> remainingComponents,
                       List<TestCase> fixed,
                       List<TestCase> failing,
                       Component leaf,
                       List<Component> remainingLeaves,
                       TestCase test,
                       List<TestCase> remainingTests,
                       List<Program> explored) {
            this.program = program;
            this.fixed = fixed;
            this.failing = failing;
            this.leaf = leaf;
            this.test = test;
            this.explored = explored;
            this.remainingLeaves = remainingLeaves;
            this.remainingTests = remainingTests;
            this.remainingComponents = remainingComponents;
        }

    }

    //NOTE: choose next leaf and restore remaining tests
    private SearchTreeNode chooseNextLeaf(SearchTreeNode s) {
        List<Component> remaining = s.remainingLeaves;
        Component next = remaining.get(0);
        List<Component> nextRemaining = remaining.subList(1, remaining.size());
        return chooseNextTest(new SearchTreeNode(s.program, s.remainingComponents, s.fixed, s.failing, next, nextRemaining, s.test, s.failing, new ArrayList<>()));
    }

    private SearchTreeNode chooseNextTest(SearchTreeNode s) {
        List<TestCase> remaining = s.remainingTests;
        TestCase next = remaining.get(0);
        List<TestCase> nextRemaining = remaining.subList(1, remaining.size());
        return new SearchTreeNode(s.program, s.remainingComponents, s.fixed, s.failing, s.leaf, s.remainingLeaves, next, nextRemaining, s.explored);
    }

    private SearchTreeNode addExplored(SearchTreeNode s, Program p) {
        List<Program> explored = new ArrayList<>(s.explored);
        explored.add(p);
        return new SearchTreeNode(s.program, s.remainingComponents, s.fixed, s.failing, s.leaf, s.remainingLeaves, s.test, s.remainingTests, explored);
    }


    private void logSearchTreeNode(SearchTreeNode n) {
        logger.info("Program: " + n.program.getLeft().getSemantics(n.program.getRight()));
        //logger.info("Used: " + n.program.getLeft().getComponents());
        //List<Component> flattenedComponents = n.remainingComponents.stream().map(Component::new).collect(Collectors.toList());
        //logger.info("Remaining: " + flattenedComponents);
        //logger.info("Fixed: " + n.fixed);
        //logger.info("Failing: " + n.failing);
        //logger.info("Remaining: " + n.remainingTests);
        //logger.info("Leaf: " + n.leaf);
        //logger.info("Test: " + n.test);
        logger.info("Fixed/Failing: " + n.fixed.size() + "/" + n.failing.size());
        //logger.info("Explored: " + n.explored);
    }

    protected class SynthesisContext extends TestCase {

        private TestCase outerTest;
        private Pair<Program, Map<Parameter, Constant>> context;
        private Variable globalOutput;
        private Component leaf;

        SynthesisContext(TestCase outerTest, Pair<Program, Map<Parameter, Constant>> context, Component leaf) {
            this.outerTest = outerTest;
            this.context = context;
            this.leaf = leaf;
            this.globalOutput = new ProgramOutput(outerTest.getOutputType());
        }

        public TestCase getOuterTest() {
            return outerTest;
        }

        @Override
        public List<Node> getConstraints(Variable output) {
            List<Node> constraints = new ArrayList<>();

            constraints.addAll(outerTest.getConstraints(globalOutput));

            Map<Component, Program> mapping = new HashMap<>();
            mapping.put(leaf, Program.leaf(new Component(output)));
            Program substituted = context.getLeft().substitute(mapping);
            Node contextClause = new Equal(globalOutput, substituted.getSemantics(context.getRight()));
            constraints.add(contextClause);

            //NOTE: I need to relax constraints for parameters that are not used or only used in the leaf
            for (Map.Entry<Parameter, Constant> parameterConstantEntry : context.getRight().entrySet()) {
                Parameter p = parameterConstantEntry.getKey();
                Constant v = parameterConstantEntry.getValue();
                if (!substituted.getSemantics().contains(p)) {
                    continue;
                }
                constraints.add(new Equal(p, v));
            }

            return constraints;
        }

        @Override
        public Type getOutputType() {
            return TypeInference.typeOf(leaf);
        }
    }

    private List<TestCase> getFailing(Pair<Program, Map<Parameter, Constant>> p, List<? extends TestCase> t) {
        List<TestCase> failing = new ArrayList<>();
        for (TestCase testCase : t) {
            if (!tester.isPassing(p.getLeft(), p.getRight(), testCase)) {
                failing.add(testCase);
            }
        }
        return failing;
    }

    private int leafDepth(Program p, Component leaf) {
        if (p.getRoot().equals(leaf)) {
            return 1;
        }
        int depth = 0;
        for (Program program : p.getChildren().values()) {
            depth += leafDepth(program, leaf);
        }
        if (depth == 0) {
            return 0;
        } else {
            return depth + 1;
        }
    }


    /**
     * TODO:
     * 1. Am I handling parameters correctly when they are parts of components?
     * Not actually. At least if a parameter is both in context and components, then conflict should depend on this parameter
     * 2. In which direction should I compute interpolants?
     * 3. Prioritize expansions (by number of fixed tests (divide by 2?), by size, etc)
     * 5. Merge choosing steps
     * 6. Should start from an empty program, because leaf program is not always possible
     */
    @Override
    public Either<Pair<Program, Map<Parameter, Constant>>, Node> synthesizeOrLearn(List<? extends TestCase> testSuite,
                                                                                   Multiset<Node> components) {
        ConflictDatabase conflicts = new ConflictDatabase(components);
        Stack<SearchTreeNode> synthesisSequence = new Stack<>();

        Set<String> history = new HashSet<>();

        TreeBoundedSynthesis initialSynthesizer = new TreeBoundedSynthesis(iSolver, new TBSConfig(1));
        List<TestCase> initialTestSuite = new ArrayList<>();
        initialTestSuite.add(testSuite.get(0));
        Pair<Program, Map<Parameter, Constant>> initial = initialSynthesizer.synthesize(initialTestSuite, components).get();

        List<TestCase> fixed = new ArrayList<>();
        fixed.add(testSuite.get(0));

        List<TestCase> failing = getFailing(initial, testSuite);

        if (failing.isEmpty()) {
            return Either.left(initial);
        }

        Multiset<Node> remaining = remainingComponents(components, initial.getLeft());

        SearchTreeNode first =
                chooseNextLeaf(
                        new SearchTreeNode(initial, remaining, fixed, failing, null, initial.getLeft().getLeaves(), null, failing, new ArrayList<>()));
        synthesisSequence.push(first);

        TBSConfig tbsConfig = new TBSConfig(config.incrementBound);
        if (config.conciseInterpolants) {
            tbsConfig.enableConciseInterpolants();
        }
        if (config.invertedLearning) {
            tbsConfig.enableInvertedLearning();
        }

        Map<Type, ProgramOutput> conflictVariables = new HashMap<>();

        int synthesized = 1;
        int subsumed = 1;

        while (!synthesisSequence.isEmpty()) {
            SearchTreeNode current = synthesisSequence.pop();

            List<TestCase> newFixed = new ArrayList<>(current.fixed);
            newFixed.add(current.test);

            List<SynthesisContext> contextTestSuite = new ArrayList<>();
            for (TestCase testCase : newFixed) {
                contextTestSuite.add(new SynthesisContext(testCase, current.program, current.leaf));
            }

            Multiset<Node> remainingWithRemovedLeaf = HashMultiset.create();
            remainingWithRemovedLeaf.addAll(current.remainingComponents);
            remainingWithRemovedLeaf.add(current.leaf.getSemantics());

            boolean restricted;
            if (config.totalBound.isPresent()) {
                restricted = true;
                int budget = config.totalBound.get() - leafDepth(current.program.getLeft(), current.leaf) + 1;
                tbsConfig.setBound(Math.min(config.incrementBound, budget));
            } else {
                restricted = false;
                tbsConfig.setBound(config.incrementBound);
            }

            tbsConfig.setForbidden(current.explored);
            TreeBoundedSynthesis synthesizer = new TreeBoundedSynthesis(iSolver, tbsConfig);

            Type leafType = TypeInference.typeOf(current.leaf);

            boolean isSubsumed = false;

            if (config.conflictLearning && !restricted) {
                List<Node> relevantConflicts = conflicts.query(remainingWithRemovedLeaf);
                if (!relevantConflicts.isEmpty()) {

                    List<Node> clauses = new ArrayList<>();

                    for (SynthesisContext testCase : contextTestSuite) {
                        ProgramOutput output = conflictVariables.get(leafType);
                        List<Node> testClauses = testCase.getConstraints(output);
                        for (Node testClause : testClauses) {
                            clauses.add(testClause.instantiate(testCase.getOuterTest()));
                        }
                    }

                    boolean result;
                    if (config.checkSubstitutionExists) {
                        result = solver.check(clauses);
                    } else {
                        result = true;
                    }
                    if (result) {

                        Node conflict;
                        if (config.invertedLearning) {
                            conflict = Node.conjunction(relevantConflicts);
                        } else {
                            conflict = new Not(Node.disjunction(relevantConflicts));
                        }
                        clauses.add(conflict);

                        if (solver instanceof MathSAT) {
                            ((MathSAT) solver).enableMemoization();
                        }
                        result = solver.check(clauses);
                        if (solver instanceof MathSAT) {
                            ((MathSAT) solver).disableMemoization();
                        }
                        if (!result) {
                            subsumed++;
                            //temporary:
                            isSubsumed = true;
                            result = true;
                            //logger.info("SUBSUMED");
                        }
                    }
                    if (!result) {
                        if (!current.remainingTests.isEmpty()) {
                            synthesisSequence.push(chooseNextTest(current));
                        } else if (!current.remainingLeaves.isEmpty()) {
                            synthesisSequence.push(chooseNextLeaf(current));
                        }
                        continue;
                    }
                }
            }

            if (!conflictVariables.containsKey(leafType)) {
                ProgramOutput output = new ProgramOutput(leafType);
                conflictVariables.put(leafType, output);
                tbsConfig.setProgramOutput(leafType, output);
            }

            Either<Pair<Program, Map<Parameter, Constant>>, Node> result =
                    synthesizer.synthesizeOrLearn(contextTestSuite, remainingWithRemovedLeaf);

            if (result.isRight()) {
                Node conflict = result.right().value();
                //FIXME: why do we get true and false?
                //if (conflict.equals(BoolConst.FALSE)) logger.warn("FALSE");
                //if (conflict.equals(BoolConst.TRUE)) logger.warn("TRUE");
                if (config.conflictLearning && !conflict.equals(BoolConst.FALSE) && !conflict.equals(BoolConst.TRUE)) {
//                    String print = Printer.print(conflict);
//                    if (print.length() > 1000) {
//                        print = "<too long>";
//                    }
//                    logger.info("learned conflict: " + print);
//                    logger.info("for components: " + remainingWithRemovedLeaf);
                    conflicts.insert(remainingWithRemovedLeaf, conflict);
                }

                if (!current.remainingTests.isEmpty()) {
                    synthesisSequence.push(chooseNextTest(current));
                } else if (!current.remainingLeaves.isEmpty()) {
                    synthesisSequence.push(chooseNextLeaf(current));
                }
                continue;
            }

            if (isSubsumed) {
                logger.error("WE HAVE SERIOUS PROBLEMS");
            }

            synthesized++;

            Pair<Program, Map<Parameter, Constant>> substitution = result.left().value();

            synthesisSequence.push(addExplored(current, substitution.getLeft()));

            Map<Parameter, Constant> newParameterValuation = new HashMap<>();
            newParameterValuation.putAll(current.program.getRight());
            newParameterValuation.putAll(substitution.getRight()); //NOTE: substitution can override parameters
            Map<Component, Program> mapping = new HashMap<>();
            mapping.put(current.leaf, substitution.getLeft());
            Program newProgram = current.program.getLeft().substitute(mapping);

            Pair<Program, Map<Parameter, Constant>> next = new ImmutablePair<>(newProgram, newParameterValuation);

            List<TestCase> newFailing = getFailing(next, testSuite);
            if (newFailing.isEmpty()) {
                logger.info("Total synthesized: " + synthesized);
                logger.info("Total subsumed: " + subsumed);
                return Either.left(next);
            }

            Multiset<Node> newComponents = remainingComponents(components, newProgram);

            List<Component> newLeaves = newProgram.getLeaves();

            SearchTreeNode newNode =
                    chooseNextLeaf(
                            new SearchTreeNode(next, newComponents, newFixed, newFailing, null, newLeaves, null, newFailing, new ArrayList<Program>()));

            logSearchTreeNode(newNode);

            String repr = newNode.program.getLeft().getSemantics(newNode.program.getRight()).toString();
            if (history.contains(repr)) {
                //logger.warn("REPETITION");
            } else {
                history.add(repr);
            }

            synthesisSequence.push(newNode);
        }

        return Either.right(new Dummy(BoolType.TYPE));
    }

}
