package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
import fj.data.Either;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Sergey Mechtaev on 2/5/2016.
 */
public class TreeBoundedSynthesis extends SynthesisWithLearning {

    private TBSConfig config;

    private Logger logger = LoggerFactory.getLogger(TreeBoundedSynthesis.class);

    private class EncodingResult {
        // branch values tree
        private Map<Variable, List<Variable>> tree;

        // possible choices for each branch
        private Map<Variable, List<Selector>> nodeChoices;

        // selected components
        private Map<Selector, Component> selectedComponent;

        // branch is activated by any of these selectors
        private Map<Variable, List<Selector>> branchDependencies;

        // selectors corresponding to the same component
        private Map<Component, List<Selector>> componentUsage;

        // from forbidden program to corresponding selectors
        // list of lists because at each node there can be several matches that must be disjoined
        private Map<Program, List<List<Selector>>> forbiddenSelectors;

        private List<Node> clauses;

        public EncodingResult(Map<Variable, List<Variable>> tree,
                              Map<Variable, List<Selector>> nodeChoices,
                              Map<Selector, Component> selectedComponent,
                              Map<Variable, List<Selector>> branchDependencies,
                              Map<Component, List<Selector>> componentUsage,
                              Map<Program, List<List<Selector>>> forbiddenSelectors,
                              List<Node> clauses) {
            this.tree = tree;
            this.nodeChoices = nodeChoices;
            this.selectedComponent = selectedComponent;
            this.branchDependencies = branchDependencies;
            this.componentUsage = componentUsage;
            this.forbiddenSelectors = forbiddenSelectors;
            this.clauses = clauses;
        }
    }

    private InterpolatingSolver solver;

    // NOTE: now forbidden check prefixes if they are larger than size
    public TreeBoundedSynthesis(InterpolatingSolver solver, TBSConfig config) {
        this.solver = solver;
        this.config = config;
    }

    @Override
    public Either<Pair<Program, Map<Parameter, Constant>>, Node> synthesizeOrLearn(List<? extends TestCase> testSuite,
                                                                                   Multiset<Node> components) {
        List<Component> flattenedComponents = components.stream().map(Component::new).collect(Collectors.toList());
        ProgramOutput root;
        if (config.outputs.containsKey(testSuite.get(0).getOutputType())) {
            root = config.outputs.get(testSuite.get(0).getOutputType());
        } else {
            root = new ProgramOutput(testSuite.get(0).getOutputType());
        }
        // top level -> current level
        Map<Program, Program> initialForbidden =
                config.forbidden.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));

        Optional<EncodingResult> result = encodeBranch(root, config.bound, flattenedComponents, initialForbidden);

        if (!result.isPresent()) {
            throw new IllegalArgumentException("wrong synthesis input");
        }
        List<Node> contextClauses = new ArrayList<>();
        List<Node> synthesisClauses = new ArrayList<>();
        for (TestCase test : testSuite) {
            for (Node node : result.get().clauses) {
                if (test instanceof CODIS.SynthesisContext) {
                    //this is a bad hack, because I want to pretend that conflicts are computed in the outer context
                    synthesisClauses.add(node.instantiate(((CODIS.SynthesisContext) test).getOuterTest()));
                } else {
                    synthesisClauses.add(node.instantiate(test));
                }
            }
            contextClauses.addAll(testToConstraint(test, root));
        }

        for (Map.Entry<Variable, List<Selector>> entry : result.get().nodeChoices.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Node precondition;
                if (result.get().branchDependencies.containsKey(entry.getKey())) {
                    precondition = Node.disjunction(result.get().branchDependencies.get(entry.getKey()));
                } else {
                    precondition = BoolConst.TRUE;
                }
                synthesisClauses.add(new Impl(precondition, Node.disjunction(entry.getValue())));
            }
        }
        for (List<List<Selector>> selectors : result.get().forbiddenSelectors.values()) {
            if (!selectors.isEmpty()) {
                synthesisClauses.add(
                        Node.disjunction(selectors.stream().map(l ->
                                Node.conjunction(l.stream().map(Not::new).collect(Collectors.toList()))).collect(Collectors.toList())));
            }
        }
        if (config.uniqueUsage) {
            for (Component component : flattenedComponents) {
                if (result.get().componentUsage.containsKey(component)) {
                    synthesisClauses.addAll(Cardinality.pairwise(result.get().componentUsage.get(component)));
                }
            }
        }

        Either<Map<Variable, Constant>, Node> solverResult;
        if (!config.conciseInterpolants) {
            if (config.invertedLearning) {
                solverResult = solver.getModelOrInterpolant(synthesisClauses, contextClauses);
            } else {
                solverResult = solver.getModelOrInterpolant(contextClauses, synthesisClauses);
            }
        } else {
            List<Node> renamedContextClauses = new ArrayList<>();
            List<Node> renamedSynthesisClauses = new ArrayList<>();
            for (Node contextClause : contextClauses) {
                renamedContextClauses.add(
                        contextClause.index(0, c -> !(c instanceof TestInstance && ((TestInstance)c).getVariable().equals(root))));
            }
            renamedSynthesisClauses.addAll(synthesisClauses);
            for (Node contextClause : contextClauses) {
                renamedSynthesisClauses.add(
                        contextClause.index(0, c -> c instanceof TestInstance && ((TestInstance)c).getVariable().equals(root)));
            }
            if (config.invertedLearning) {
                solverResult = solver.getModelOrInterpolant(renamedSynthesisClauses, renamedContextClauses);
            } else {
                solverResult = solver.getModelOrInterpolant(renamedContextClauses, renamedSynthesisClauses);
            }
        }
        if (solverResult.isLeft()) {
            Pair<Program, Map<Parameter, Constant>> decoded = decode(solverResult.left().value(), root, result.get());
            return Either.left(decoded);
        } else {
            return Either.right(solverResult.right().value());
        }
    }

    private List<Node> testToConstraint(TestCase testCase, Variable output) {
        List<Node> clauses = new ArrayList<>();
        List<Node> testClauses = testCase.getConstraints(output);
        for (Node clause : testClauses) {
            if (testCase instanceof CODIS.SynthesisContext) {
                //this is a bad hack, because I want to pretend that conflicts are computed in the outer context
                clauses.add(clause.instantiate(((CODIS.SynthesisContext) testCase).getOuterTest()));
            } else {
                clauses.add(clause.instantiate(testCase));
            }
        }
        return clauses;
    }

    private Optional<EncodingResult> encodeBranch(Variable output, int size, List<Component> components, Map<Program, Program> forbidden) {
        // Local results:
        List<Selector> currentChoices = new ArrayList<>();
        Map<Selector, Component> selectedComponent = new HashMap<>();
        Map<Variable, List<Selector>> branchDependencies = new HashMap<>();
        Map<Component, List<Selector>> componentUsage = new HashMap<>();

        List<Node> clauses = new ArrayList<>();

        List<Component> relevantComponents = new ArrayList<>(components);
        relevantComponents.removeIf(c -> !TypeInference.typeOf(c).equals(output.getType()));
        List<Component> leafComponents = new ArrayList<>(relevantComponents);
        leafComponents.removeIf(c -> !(c.isLeaf()));
        List<Component> functionComponents = new ArrayList<>(relevantComponents);
        functionComponents.removeIf(Component::isLeaf);

        Set<Program> localForbidden = new HashSet<>(forbidden.values());
        // mapping from current level to selectors
        Map<Program, List<Selector>> localForbiddenSelectors = new HashMap<>();
        for (Program program : localForbidden) {
            localForbiddenSelectors.put(program, new ArrayList<>());
        }
        Map<Program, List<Selector>> localForbiddenLeavesSelectors = new HashMap<>();
        Map<Program, List<List<Selector>>> globalForbiddenResult = new HashMap<>();

        for (Component component : leafComponents) {
            Selector selector = new Selector();
            for (Program program : localForbidden) {
                if (program.getRoot().getSemantics().equals(component.getSemantics())) {
                    if (!localForbiddenLeavesSelectors.containsKey(program)) {
                        localForbiddenLeavesSelectors.put(program, new ArrayList<>());
                    }
                    localForbiddenLeavesSelectors.get(program).add(selector);
                }
            }
            clauses.add(new Impl(selector, new Equal(output, component.getSemantics())));
            if (!componentUsage.containsKey(component)) {
                componentUsage.put(component, new ArrayList<>());
            }
            componentUsage.get(component).add(selector);
            selectedComponent.put(selector, component);
            currentChoices.add(selector);
        }

        List<Variable> children = new ArrayList<>();
        // from child branch to its encoding:
        Map<Variable, EncodingResult> subresults = new HashMap<>();

        List<Component> feasibleComponents = new ArrayList<>(functionComponents);

        if (size > 1) {
            Map<Component, Map<Hole, Variable>> branchMatching = new HashMap<>();
            // components dependent of the branch:
            Map<Variable, List<Component>> componentDependencies = new HashMap<>();
            // forbidden for each branch:
            Map<Variable, Map<Program, Program>> subnodeForbidden = new HashMap<>();
            // first we need to precompute all required branches and match them with subnodes of forbidden programs:
            for (Component component : functionComponents) {
                Map<Hole, Variable> args = new HashMap<>();
                List<Variable> availableChildren = new ArrayList<>(children);
                for (Hole input : component.getInputs()) {
                    Variable child;
                    Optional<Variable> existingChild = availableChildren.stream().filter(o -> o.getType().equals(input.getType())).findFirst();
                    if (existingChild.isPresent()) {
                        child = existingChild.get();
                        availableChildren.remove(child);
                    } else {
                        child = new BranchOutput(input.getType());
                        componentDependencies.put(child, new ArrayList<>());
                    }
                    componentDependencies.get(child).add(component);
                    args.put(input, child);

                    subnodeForbidden.put(child, new HashMap<>());
                    for (Program local : localForbidden) {
                        if (local.getRoot().getSemantics().equals(component.getSemantics())) {
                            for (Program global : forbidden.keySet()) {
                                if (forbidden.get(global).equals(local)) {
                                    // NOTE: can be repetitions, but it is OK
                                    subnodeForbidden.get(child).put(global, local.getChildren().get(input));
                                }
                            }
                        }
                    }
                }
                for (Variable variable : args.values()) {
                    if (!children.contains(variable)) {
                        children.add(variable);
                    }
                }
                branchMatching.put(component, args);
            }

            List<Variable> infeasibleChildren = new ArrayList<>();
            // encoding subnodes and removing infeasible children and components:
            for (Variable child : children) {
                Optional<EncodingResult> subresult = encodeBranch(child, size - 1, components, subnodeForbidden.get(child));
                if (!subresult.isPresent()) {
                    feasibleComponents.removeAll(componentDependencies.get(child));
                    infeasibleChildren.add(child);
                } else {
                    subresults.put(child, subresult.get());
                }
            }
            children.removeAll(infeasibleChildren);

            // for all encoded components, creating node constraints:
            for (Component component : feasibleComponents) {
                Selector selector = new Selector();
                Collection<Variable> usedBranches = branchMatching.get(component).values();
                for (Variable child : usedBranches) {
                    if (!branchDependencies.containsKey(child)) {
                        branchDependencies.put(child, new ArrayList<>());
                    }
                    branchDependencies.get(child).add(selector);
                }
                for (Program program : localForbidden) {
                    if (program.getRoot().getSemantics().equals(component.getSemantics())) {
                        localForbiddenSelectors.get(program).add(selector);
                    }
                }
                clauses.add(new Impl(selector, new Equal(output, Traverse.substitute(component.getSemantics(), branchMatching.get(component)))));
                if (!componentUsage.containsKey(component)) {
                    componentUsage.put(component, new ArrayList<>());
                }
                componentUsage.get(component).add(selector);
                selectedComponent.put(selector, component);
                currentChoices.add(selector);
            }

        }

        if (currentChoices.isEmpty()) {
            return Optional.empty();
        }

        Map<Variable, List<Selector>> nodeChoices = new HashMap<>();
        nodeChoices.put(output, currentChoices);
        Map<Variable, List<Variable>> tree = new HashMap<>();
        tree.put(output, new ArrayList<>());
        tree.put(output, children);

        // merging subnodes information:
        for (EncodingResult subresult: subresults.values()) {
            clauses.addAll(subresult.clauses);
            for (Map.Entry<Component, List<Selector>> usage : subresult.componentUsage.entrySet()) {
                if (componentUsage.containsKey(usage.getKey())) {
                    componentUsage.get(usage.getKey()).addAll(usage.getValue());
                } else {
                    componentUsage.put(usage.getKey(), usage.getValue());
                }
            }
            tree.putAll(subresult.tree);
            nodeChoices.putAll(subresult.nodeChoices);
            selectedComponent.putAll(subresult.selectedComponent);
            branchDependencies.putAll(subresult.branchDependencies);
        }

        for (Program global : forbidden.keySet()) {
            Program local = forbidden.get(global);
            if (localForbiddenLeavesSelectors.containsKey(local)) {
                globalForbiddenResult.put(global, new ArrayList<>());
                globalForbiddenResult.get(global).add(localForbiddenLeavesSelectors.get(local)); // matching leaves
            } else {
                if (localForbiddenSelectors.get(local).isEmpty()) {
                    globalForbiddenResult.put(global, new ArrayList<>()); //NOTE: even if subnode selectors are not empty
                } else {
                    globalForbiddenResult.put(global, new ArrayList<>());
                    globalForbiddenResult.get(global).add(localForbiddenSelectors.get(local));
                    boolean failed = false;
                    for (Map.Entry<Variable, EncodingResult> entry : subresults.entrySet()) {
                        Map<Program, List<List<Selector>>> subnodeForbidden = entry.getValue().forbiddenSelectors;
                        if (!subnodeForbidden.containsKey(global)) { // means that it is not matched with local program
                            continue;
                        }
                        if (!subnodeForbidden.get(global).isEmpty()) {
                            globalForbiddenResult.get(global).addAll(subnodeForbidden.get(global));
                        } else {
                            failed = true;
                            break;
                        }
                    }
                    if (failed) {
                        globalForbiddenResult.put(global, new ArrayList<>()); //erasing
                    }
                }
            }
        }

        return Optional.of(new EncodingResult(tree, nodeChoices, selectedComponent, branchDependencies, componentUsage, globalForbiddenResult, clauses));
    }

    private Pair<Program, Map<Parameter, Constant>> decode(Map<Variable, Constant> assignment,
                                                           Variable root,
                                                           EncodingResult result) {
        List<Selector> nodeChoices = result.nodeChoices.get(root);
        Selector choice = nodeChoices.stream().filter(s -> assignment.get(s).equals(BoolConst.TRUE)).findFirst().get();
        Component component = result.selectedComponent.get(choice);
        Map<Parameter, Constant> parameterValuation = new HashMap<>();
        if (component.getSemantics() instanceof Parameter) {
            Parameter p = (Parameter) component.getSemantics();
            parameterValuation.put(p, assignment.get(p));
        }

        if (component.isLeaf()) {
            return new ImmutablePair<>(Program.leaf(component), parameterValuation);
        }

        Map<Hole, Program> args = new HashMap<>();
        List<Variable> children = new ArrayList<>(result.tree.get(root));
        for (Hole input : component.getInputs()) {
            Variable child = children.stream().filter(o -> o.getType().equals(input.getType())).findFirst().get();
            children.remove(child);
            Pair<Program, Map<Parameter, Constant>> subresult = decode(assignment, child, result);
            parameterValuation.putAll(subresult.getRight());
            args.put(input, subresult.getLeft());
        }

        return new ImmutablePair<>(Program.app(component, args), parameterValuation);
    }

}
