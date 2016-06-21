package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
import com.sun.applet2.AppletParameters;
import fj.P;
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

    private int bound;
    private boolean uniqueUsage;
    private List<Program> globalForbidden;

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
    public TreeBoundedSynthesis(InterpolatingSolver solver, int bound, boolean uniqueUsage, List<Program> forbidden) {
        this.bound = bound;
        this.solver = solver;
        this.uniqueUsage = uniqueUsage;
        this.globalForbidden = forbidden;
    }

    public TreeBoundedSynthesis(InterpolatingSolver solver, int bound, boolean uniqueUsage) {
        this.bound = bound;
        this.solver = solver;
        this.uniqueUsage = uniqueUsage;
        this.globalForbidden = new ArrayList<>();
    }

    @Override
    public Either<Pair<Program, Map<Parameter, Constant>>, Node> synthesizeOrLearn(List<TestCase> testSuite,
                                                                                   Multiset<Node> components) {
        List<Component> flattenedComponents = components.stream().map(Component::new).collect(Collectors.toList());
        ProgramOutput root = new ProgramOutput(testSuite.get(0).getOutputType());
        // top level -> current level
        Map<Program, Program> initialForbidden =
                globalForbidden.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));

        Optional<EncodingResult> result = encodeBranch(root, bound, flattenedComponents, initialForbidden);

        if (!result.isPresent()) {
            throw new IllegalArgumentException("wrong synthesis input");
        }
        List<Node> contextClauses = new ArrayList<>();
        List<Node> synthesisClauses = new ArrayList<>();
        for (TestCase test : testSuite) {
            for (Node node : result.get().clauses) {
                synthesisClauses.add(node.instantiate(test));
            }
            contextClauses.addAll(testToConstraint(test, root));
        }

        for (Map.Entry<Variable, List<Selector>> entry : result.get().nodeChoices.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Node precondition;
                if (result.get().branchDependencies.containsKey(entry.getKey())) {
                    precondition = disjunction(result.get().branchDependencies.get(entry.getKey()));
                } else {
                    precondition = BoolConst.TRUE;
                }
                synthesisClauses.add(new Impl(precondition, disjunction(entry.getValue())));
            }
        }
        for (List<List<Selector>> selectors : result.get().forbiddenSelectors.values()) {
            if (!selectors.isEmpty()) {
                synthesisClauses.add(
                        disjunction(selectors.stream().map(l ->
                                conjunction(l.stream().map(Not::new).collect(Collectors.toList()))).collect(Collectors.toList())));
            }
        }
        if (uniqueUsage) {
            for (Component component : flattenedComponents) {
                if (result.get().componentUsage.containsKey(component)) {
                    synthesisClauses.addAll(Cardinality.pairwise(result.get().componentUsage.get(component)));
                }
            }
        }
        Either<Map<Variable, Constant>, Node> solverResult = solver.getModelOrInterpolant(contextClauses, synthesisClauses);
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
            clauses.add(clause.instantiate(testCase));
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
        List<EncodingResult> subresults = new ArrayList<>();

        if (size > 1) {
            for (Component component : functionComponents) {
                boolean infeasibleComponent = false;
                Map<Hole, Node> args = new HashMap<>();
                List<Variable> availableChildren = new ArrayList<>(children);
                List<Variable> usedByComponent = new ArrayList<>();
                List<EncodingResult> componentSubresults = new ArrayList<>();
                for (Hole input : component.getInputs()) {
                    Optional<Variable> child = availableChildren.stream().filter(o -> o.getType().equals(input.getType())).findFirst();
                    if (child.isPresent()) {
                        usedByComponent.add(child.get());
                        args.put(input, child.get());
                        availableChildren.remove(child.get());
                    } else {
                        BranchOutput branch = new BranchOutput(input.getType());
                        Map<Program, Program> subnodeForbidden = new HashMap<>();
                        for (Program global : forbidden.keySet()) {
                            if (forbidden.get(global).getChildren().containsKey(input)) {
                                subnodeForbidden.put(global, forbidden.get(global).getChildren().get(input));
                            }
                        }
                        Optional<EncodingResult> subresult = encodeBranch(branch, size - 1, components, subnodeForbidden);
                        if (!subresult.isPresent()) {
                            infeasibleComponent = true;
                            break;
                        }
                        componentSubresults.add(subresult.get());
                        usedByComponent.add(branch);
                        args.put(input, branch);
                    }
                }
                if (infeasibleComponent) {
                    continue;
                }
                for (Variable variable : usedByComponent) {
                    if (!children.contains(variable)) {
                        children.add(variable); //NODE: add only if feasible
                    }
                }
                subresults.addAll(componentSubresults); //NOTE: add only if it is feasible

                Selector selector = new Selector();
                for (Variable child : usedByComponent) {
                    if (branchDependencies.containsKey(child)) {
                        branchDependencies.get(child).add(selector);
                    } else {
                        List<Selector> dependentSelector = new ArrayList<>();
                        dependentSelector.add(selector);
                        branchDependencies.put(child, dependentSelector);
                    }
                }
                for (Program program : localForbidden) {
                    if (program.getRoot().getSemantics().equals(component.getSemantics())) {
                        localForbiddenSelectors.get(program).add(selector);
                    }
                }
                clauses.add(new Impl(selector, new Equal(output, Traverse.substitute(component.getSemantics(), args))));
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

        for (EncodingResult subresult: subresults) {
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

        //FIXME: this part just completely wrong!
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
                    //???
                    for (EncodingResult encodingResult : subresults) {
                        Map<Program, List<List<Selector>>> subnodeForbidden = encodingResult.forbiddenSelectors;
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

    private Node disjunction(List<? extends Node> clauses) {
        Node node = BoolConst.FALSE;
        for (Node clause : clauses) {
            node = new Or(node, clause);
        }
        return node;
    }

    private Node conjunction(List<? extends Node> clauses) {
        Node node = BoolConst.TRUE;
        for (Node clause : clauses) {
            node = new And(node, clause);
        }
        return node;
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
