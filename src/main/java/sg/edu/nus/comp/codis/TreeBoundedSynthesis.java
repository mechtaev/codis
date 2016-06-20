package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
import fj.data.Either;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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


    // branch values tree
    private Map<Variable, List<Variable>> tree;

    // possible choices for each branch
    private Map<Variable, List<Selector>> choices;

    // selected components
    private Map<Selector, Component> selectedComponent;

    // branch is activated by any of these selectors
    private Map<Variable, List<Selector>> branchDependencies;

    // selectors corresponding to the same component
    private Map<Component, List<Selector>> componentUsage;

    private InterpolatingSolver solver;

    // NOTE: now forbidden effectively check prefixes
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

    /*
    TODO:
     1. Try with equivalent components
     */
    @Override
    public Either<Pair<Program, Map<Parameter, Constant>>, Node> synthesizeOrLearn(List<TestCase> testSuite,
                                                                                   Multiset<Node> components) {
        tree = new HashMap<>();
        selectedComponent = new HashMap<>();
        branchDependencies = new HashMap<>();
        choices = new HashMap<>();

        componentUsage = new HashMap<>();
        List<Component> flattenedComponents = components.stream().map(Component::new).collect(Collectors.toList());
        for (Component component : flattenedComponents) {
            componentUsage.put(component, new ArrayList<>());
        }

        ProgramOutput root = new ProgramOutput(testSuite.get(0).getOutputType());
        // top level -> current level
        Map<Program, Program> initialForbidden =
                globalForbidden.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
        Pair<List<Node>, Map<Program, List<Selector>>> branchClauses = encodeBranch(root, bound, flattenedComponents, initialForbidden);
        List<Node> contextClauses = new ArrayList<>();
        List<Node> synthesisClauses = new ArrayList<>();
        for (TestCase test : testSuite) {
            for (Node node : branchClauses.getLeft()) {
                synthesisClauses.add(node.instantiate(test));
            }
            contextClauses.addAll(testToConstraint(test, root));
        }
        for (Map.Entry<Variable, List<Selector>> entry : choices.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Node precondition;
                if (branchDependencies.containsKey(entry.getKey())) {
                    precondition = disjunction(branchDependencies.get(entry.getKey()));
                } else {
                    precondition = BoolConst.TRUE;
                }
                synthesisClauses.add(new Impl(precondition, disjunction(entry.getValue())));
            }
        }
        for (List<Selector> selectors : branchClauses.getRight().values()) {
            if (!selectors.isEmpty()) {
                synthesisClauses.add(disjunction(selectors.stream().map(Not::new).collect(Collectors.toList())));
            }
        }
        if (uniqueUsage) {
            for (Component component : flattenedComponents) {
                synthesisClauses.addAll(Cardinality.pairwise(componentUsage.get(component)));
            }
        }
        List<Node> clauses = new ArrayList<>();
        //FIXME: this could potentially reduce performance of TBS when we don't need conflicts
        Either<Map<Variable, Constant>, Node> result = solver.getModelOrInterpolant(contextClauses, synthesisClauses);
        if (result.isLeft()) {
            return Either.left(decode(result.left().value(), root));
        } else {
            return Either.right(result.right().value()); // there must be a more elegant way to express this
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

    /**
     * @return encoding plus a list of disabled selectors for each forbidden program (empty if impossible)
     */
    private Pair<List<Node>, Map<Program, List<Selector>>> encodeBranch(Variable output, int size, List<Component> components, Map<Program, Program> forbidden) {
        tree.put(output, new ArrayList<>());
        choices.put(output, new ArrayList<>());

        List<Node> clauses = new ArrayList<>();
        List<Component> relevantComponents = new ArrayList<>(components);
        relevantComponents.removeIf(c -> !TypeInference.typeOf(c).equals(output.getType()));
        List<Component> leafComponents = new ArrayList<>(relevantComponents);
        leafComponents.removeIf(c -> !(c.isLeaf()));
        List<Component> functionComponents = new ArrayList<>(relevantComponents);
        functionComponents.removeIf(Component::isLeaf);

        // for each program of the current level, the set of corresponding selectors
        Set<Program> currentLevel = new HashSet<>(forbidden.values());
        Map<Program, List<Selector>> localForbiddenSelectors = new HashMap<>();
        for (Program program : currentLevel) {
            localForbiddenSelectors.put(program, new ArrayList<>());
        }
        Map<Program, List<Selector>> globalForbiddenResult = new HashMap<>();

        for (Component component : leafComponents) {
            Selector selector = new Selector();
            for (Program program : currentLevel) {
                if (program.getRoot().getSemantics().equals(component.getSemantics())) {
                    localForbiddenSelectors.get(program).add(selector);
                }
            }
            clauses.add(new Impl(selector, new Equal(output, component.getSemantics())));
            componentUsage.get(component).add(selector);
            selectedComponent.put(selector, component);
            choices.get(output).add(selector);
        }

        Map<Variable, Map<Program, List<Selector>>> subnodeForbiddenSelectors = new HashMap<>();

        if (size > 1) {
            List<Variable> lazyChildren = new ArrayList<>();
            // child -> (forbidden global -> selectors)
            for (Component component : functionComponents) {
                boolean infeasibleComponent = false;
                List<Variable> children = new ArrayList<>(lazyChildren);
                List<Variable> usedChildren = new ArrayList<>();
                Map<Hole, Node> args = new HashMap<>();
                for (Hole input : component.getInputs()) {
                    Optional<Variable> child = children.stream().filter(o -> o.getType().equals(input.getType())).findFirst();
                    if (child.isPresent()) {
                        usedChildren.add(child.get());
                        args.put(input, child.get());
                        children.remove(child.get());
                    } else {
                        BranchOutput branch = new BranchOutput(input.getType());
                        Map<Program, Program> subnodeForbidden = new HashMap<>();
                        for (Program global : forbidden.keySet()) {
                            if (forbidden.get(global).getChildren().containsKey(input)) {
                                subnodeForbidden.put(global, forbidden.get(global).getChildren().get(input));
                            }
                        }
                        Pair<List<Node>, Map<Program, List<Selector>>> childClauses =
                                encodeBranch(branch, size - 1, components, subnodeForbidden);
                        subnodeForbiddenSelectors.put(branch, childClauses.getRight());
                        if (childClauses.getLeft().isEmpty()) {
                            infeasibleComponent = true;
                            break;
                        }
                        clauses.addAll(childClauses.getLeft());
                        usedChildren.add(branch);
                        lazyChildren.add(branch);
                        args.put(input, branch);
                    }
                }
                if (infeasibleComponent) {
                    continue;
                }
                Selector selector = new Selector();
                for (Variable child : usedChildren) {
                    if (branchDependencies.containsKey(child)) {
                        branchDependencies.get(child).add(selector);
                    }
                    List<Selector> dependentSelector = new ArrayList<>();
                    dependentSelector.add(selector);
                    branchDependencies.put(child, dependentSelector);
                }
                for (Program program : currentLevel) {
                    if (program.getRoot().getSemantics().equals(component.getSemantics())) {
                        localForbiddenSelectors.get(program).add(selector);
                    }
                }
                clauses.add(new Impl(selector, new Equal(output, Traverse.substitute(component.getSemantics(), args))));
                componentUsage.get(component).add(selector);
                selectedComponent.put(selector, component);
                choices.get(output).add(selector);
            }
            tree.put(output, lazyChildren);
        }

        for (Program global : forbidden.keySet()) {
            Program local = forbidden.get(global);
            if (localForbiddenSelectors.get(local).isEmpty()) {
                globalForbiddenResult.put(global, new ArrayList<>()); //NOTE: even if subnode selectors are not empty
            } else {
                globalForbiddenResult.put(global, localForbiddenSelectors.get(local));
                boolean failed = false;
                for (Map<Program, List<Selector>> subnodeResult : subnodeForbiddenSelectors.values()) {
                    if (subnodeResult.containsKey(global) && !subnodeResult.get(global).isEmpty()) {
                        globalForbiddenResult.get(global).addAll(subnodeResult.get(global));
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

        return new ImmutablePair<>(clauses, globalForbiddenResult);
    }

    private Node disjunction(List<? extends Node> clauses) {
        Node node = BoolConst.FALSE;
        for (Node clause : clauses) {
            node = new Or(node, clause);
        }
        return node;
    }

    private Pair<Program, Map<Parameter, Constant>> decode(Map<Variable, Constant> assignment,
                                                           Variable root) {
        List<Selector> nodeChoices = choices.get(root);
        Selector choice = nodeChoices.stream().filter(s -> assignment.get(s).equals(BoolConst.TRUE)).findFirst().get();
        Component component = selectedComponent.get(choice);
        Map<Parameter, Constant> parameterValuation = new HashMap<>();
        if (component.getSemantics() instanceof Parameter) {
            Parameter p = (Parameter) component.getSemantics();
            parameterValuation.put(p, assignment.get(p));
        }

        if (component.isLeaf()) {
            return new ImmutablePair<>(Program.leaf(component), parameterValuation);
        }

        Map<Hole, Program> args = new HashMap<>();
        List<Variable> children = new ArrayList<>(tree.get(root));
        for (Hole input : component.getInputs()) {
            Variable child = children.stream().filter(o -> o.getType().equals(input.getType())).findFirst().get();
            children.remove(child);
            Pair<Program, Map<Parameter, Constant>> subresult = decode(assignment, child);
            parameterValuation.putAll(subresult.getRight());
            args.put(input, subresult.getLeft());
        }

        return new ImmutablePair<>(Program.app(component, args), parameterValuation);
    }

}
