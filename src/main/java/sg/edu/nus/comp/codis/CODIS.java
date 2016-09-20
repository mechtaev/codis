package sg.edu.nus.comp.codis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.sun.security.auth.module.LdapLoginModule;
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
import java.util.stream.Collectors;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class CODIS extends SynthesisWithLearning {

    private Logger logger = LoggerFactory.getLogger(CODIS.class);

    private Tester tester;
    private InterpolatingSolver iSolver;
    private Solver solver;

    private CODISConfig config;

    private Map<Type, ProgramOutput> conflictVariables;

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


    private void logSearchTreeNode(SearchTreeNode n, int iteration) {
        logger.info("Iteration " + iteration +
                " Score: "  + n.fixed.size() + "/" + n.failing.size() +
                " Program: " + n.program.getLeft().getSemantics(n.program.getRight()));
        //logger.info("Used: " + n.program.getLeft().getComponents());
        List<Component> flattenedComponents = n.remainingComponents.stream().map(Component::new).collect(Collectors.toList());
        //logger.info("Remaining: " + flattenedComponents);
        //logger.info("Fixed: " + n.fixed);
        //logger.info("Failing: " + n.failing);
        //logger.info("Remaining: " + n.remainingTests);
        //logger.info("Leaf: " + n.leaf);
        //logger.info("Test: " + n.test);
        //logger.info("Explored: " + n.explored);
    }

    protected class SynthesisContext extends TestCase {

        private TestCase outerTest;
        private Pair<Program, Map<Parameter, Constant>> context;
        private Variable globalOutput;
        private Component leaf;

        SynthesisContext(TestCase outerTest, Pair<Program, Map<Parameter, Constant>> context, Component leaf) {
            super();
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
     * 7. Note that learning algorithm with concise interpolants relies on the fact that all program variables have concrete value in each test
     */
    @Override
    public Either<Pair<Program, Map<Parameter, Constant>>, Node> synthesizeOrLearn(List<? extends TestCase> testSuite,
                                                                                   Multiset<Node> components) {
        Either<List<Pair<Program, Map<Parameter, Constant>>>, Node> result = synthesizeAux(testSuite, components, false);
        if (result.isRight()) {
            return Either.right(result.right().value());
        } else {
            return Either.left(result.left().value().get(0));
        }
    }

    @Override
    public List<Pair<Program, Map<Parameter, Constant>>> synthesizeAll(List<? extends TestCase> testSuite, Multiset<Node> components) {
        Either<List<Pair<Program, Map<Parameter, Constant>>>, Node> result = synthesizeAux(testSuite, components, true);
        if (result.isRight()) {
            return new ArrayList<>();
        } else {
            return result.left().value();
        }

    }

    public Either<List<Pair<Program, Map<Parameter, Constant>>>, Node> synthesizeAux(List<? extends TestCase> testSuite,
                                                                                     Multiset<Node> components,
                                                                                     boolean findAll) {

        ConflictDatabase conflicts = new ConflictDatabase(components);
        Stack<SearchTreeNode> synthesisSequence = new Stack<>();
        conflictVariables = new HashMap<>();

        List<Pair<Program, Map<Parameter, Constant>>> found = new ArrayList<>();

        Set<String> history = new HashSet<>();

        //FIXME: instead of synthesizing, I can just choose an arbitrary leaf component
        TreeBoundedSynthesis initialSynthesizer = new TreeBoundedSynthesis(iSolver, new TBSConfig(1));
        List<TestCase> initialTestSuite = new ArrayList<>();
        initialTestSuite.add(testSuite.get(0));
        Pair<Program, Map<Parameter, Constant>> initial = initialSynthesizer.synthesize(initialTestSuite, components).get();

        List<TestCase> fixed = new ArrayList<>();
        fixed.add(testSuite.get(0));

        List<TestCase> failing = getFailing(initial, testSuite);

        if (failing.isEmpty()) {
            found.add(initial);
            return Either.left(found); //don't need to check findAll since not expansions from here
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
        if (config.forbidStructure) {
            tbsConfig.disableLeafMatching();
        }

        int synthesisIteration = 0;
        int lastRestart = synthesisIteration;

        while (!synthesisSequence.isEmpty()) {
            if (config.iterationsBeforeRestart.isPresent() &&
                    config.iterationsBeforeRestart.get() <= (synthesisIteration - lastRestart)) {
                logger.info("RESTART");
                lastRestart = synthesisIteration;
                while (synthesisSequence.size() > 1) {
                    synthesisSequence.pop();
                }
                continue;
            }

            SearchTreeNode current = synthesisSequence.pop();

            if (!synthesisSequence.isEmpty() && config.maximumLeafExpansions.isPresent() &&
                    config.maximumLeafExpansions.get() < current.explored.size()) {
                logger.debug("reached maximum number of leaf expansions");
                continue;
            }

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
            if (!conflictVariables.containsKey(leafType)) {
                ProgramOutput output = new ProgramOutput(leafType);
                conflictVariables.put(leafType, output);
                tbsConfig.setProgramOutput(leafType, output);
            }

            boolean isSubsumed = false;
            boolean substitutionExists = true; //NOTE: true actually means don't know

            if (config.checkExpansionSatisfiability && current.explored.isEmpty()) {
                substitutionExists = substitutionExists(current.leaf, contextTestSuite);
            }

            //FIXME: restricted is a bad solution. Should limit the depth for which we perform expansions
            if (config.conflictLearning && !restricted && substitutionExists && current.explored.isEmpty()) {
                List<Node> relevantConflicts = conflicts.query(remainingWithRemovedLeaf);
                if (!relevantConflicts.isEmpty()) {
                    isSubsumed = isSubsumedAtLeaf(current.leaf, contextTestSuite, relevantConflicts);
                }
            }

            if ((!substitutionExists || isSubsumed) && !config.debugMode) {
                if (!current.remainingTests.isEmpty()) {
                    synthesisSequence.push(chooseNextTest(current));
                } else if (!current.remainingLeaves.isEmpty()) {
                    synthesisSequence.push(chooseNextLeaf(current));
                }
                continue;
            }

            Either<Pair<Program, Map<Parameter, Constant>>, Node> result =
                    synthesizer.synthesizeOrLearn(contextTestSuite, remainingWithRemovedLeaf);

            if (result.isRight()) {
                Node conflict = result.right().value();
                if (conflict.equals(BoolConst.FALSE)) {
                    logger.debug("conflict is false");
                }
                if (conflict.equals(BoolConst.TRUE)) {
                    logger.debug("conflict is true");
                }
                if (config.debugMode && substitutionExists && isSubsumed) {
                    logger.info("Correct subsumption");
                }
                //FIXME: why are conflicts true/false sometimes?
                if (config.conflictLearning && !conflict.equals(BoolConst.FALSE) && !conflict.equals(BoolConst.TRUE)) {
                    int size = NodeCounter.count(conflict);
                    logger.debug("Interpolant size: " + size);
                    if (size > config.maximumInterpolantSize) {
                        logger.debug("skipping interpolant because of size");
                    } else {
                        conflicts.insert(remainingWithRemovedLeaf, conflict);
                    }
                }

                if (!current.remainingTests.isEmpty()) {
                    synthesisSequence.push(chooseNextTest(current));
                } else if (!current.remainingLeaves.isEmpty()) {
                    synthesisSequence.push(chooseNextLeaf(current));
                }
                continue;
            }

            if (config.debugMode && isSubsumed) {
                logger.error("INVALID SUBSUMPTION!");
            }

            synthesisIteration++;

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
                found.add(next);
                if (!findAll) {
                    return Either.left(found);
                } else {
                    logger.info("FOUND: " + next.getLeft().getSemantics(next.getRight()));
                    continue;
                }
            }

            Multiset<Node> newComponents = remainingComponents(components, newProgram);

            List<Component> newLeaves = newProgram.getLeaves();

            SearchTreeNode newNode =
                    chooseNextLeaf(
                            new SearchTreeNode(next, newComponents, newFixed, newFailing, null, newLeaves, null, newFailing, new ArrayList<Program>()));

            logSearchTreeNode(newNode, synthesisIteration);

//            String repr = newNode.program.getLeft().getSemantics(newNode.program.getRight()).toString();
//            if (history.contains(repr)) {
//                //logger.warn("REPETITION");
//            } else {
//                history.add(repr);
//            }

            synthesisSequence.push(newNode);
        }

        if (found.isEmpty()) {
            return Either.right(new Dummy(BoolType.TYPE));
        } else {
            return Either.left(found);
        }
    }

    private boolean isSubsumedAtLeaf(Component leaf, List<SynthesisContext> context, List<Node> conflicts) {
        if (conflicts.size() > config.maximumConflictsCheck) {
            logger.debug("found " + conflicts.size() + " conflicts, but only " + config.maximumConflictsCheck + " used");
            conflicts = conflicts.subList(0, config.maximumConflictsCheck);
        }

        List<Node> clauses = new ArrayList<>();

        Type leafType = TypeInference.typeOf(leaf);

        for (SynthesisContext testCase : context) {
            ProgramOutput output = conflictVariables.get(leafType);
            List<Node> testClauses = testCase.getConstraints(output);
            for (Node testClause : testClauses) {
                clauses.add(testClause.instantiate(testCase.getOuterTest()));
            }
        }

        Node conflict;
        if (config.invertedLearning) {
            conflict = Node.conjunction(conflicts);
        } else {
            conflict = new Not(Node.disjunction(conflicts));
        }
        clauses.add(conflict);

        if (solver instanceof MathSAT) {
            ((MathSAT) solver).enableMemoization();
        }
        boolean intersected = solver.isSatisfiable(clauses);
        if (solver instanceof MathSAT) {
            ((MathSAT) solver).disableMemoization();
        }
        if (!intersected) {
            logger.debug("SUBSUMED by " + conflicts.size() + " conflicts");
        }

        return !intersected;
    }

    private boolean substitutionExists(Component leaf, List<SynthesisContext> context) {
        List<Node> clauses = new ArrayList<>();

        Type leafType = TypeInference.typeOf(leaf);

        for (SynthesisContext testCase : context) {
            ProgramOutput output = conflictVariables.get(leafType);
            List<Node> testClauses = testCase.getConstraints(output);
            for (Node testClause : testClauses) {
                clauses.add(testClause.instantiate(testCase.getOuterTest()));
            }
        }

        return solver.isSatisfiable(clauses);

    }

}
