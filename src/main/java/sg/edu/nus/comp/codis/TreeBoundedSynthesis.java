package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Sergey Mechtaev on 2/5/2016.
 */
public class TreeBoundedSynthesis extends Synthesis {

    private int bound;
    private Solver solver;
    private Map<BranchOutput, List<BranchOutput>> tree;
    private Map<BranchOutput, List<Selector>> choices;
    private Map<Selector, Component> selected;

    public TreeBoundedSynthesis(Solver solver, int bound) {
        this.bound = bound;
        this.solver = solver;
    }

    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesizeWithForbidden(List<TestCase> testSuite,
                                                                                     Multiset<Node> components,
                                                                                     List<Program> forbidden) {
        tree = new HashMap<>();
        selected = new HashMap<>();
        choices = new HashMap<>();

        List<Component> flattenedComponents = components.stream().map(Component::new).collect(Collectors.toList());
        BranchOutput root = new BranchOutput(testSuite.get(0).getOutputType());
        Pair<List<Node>, List<List<Selector>>> branchClauses = encodeBranch(root, bound, flattenedComponents, forbidden);
        List<Node> clauses = new ArrayList<>();
        for (TestCase test : testSuite) {
            for (Node node : branchClauses.getLeft()) {
                clauses.add(instantiate(node, test));
            }
            clauses.addAll(testToConstraint(test, root));
        }
        for (List<Selector> selectors : choices.values()) {
            if (!selectors.isEmpty()) {
                clauses.add(disjunction(selectors));
            }
        }
        for (List<Selector> selectors : branchClauses.getRight()) {
            if (!selectors.isEmpty()) {
                clauses.add(disjunction(selectors.stream().map(Not::new).collect(Collectors.toList())));
            }
        }
        Optional<Map<Variable, Constant>> assignment = solver.getModel(clauses);
        if (assignment.isPresent()) {
            return Optional.of(decode(assignment.get(), root));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<TestCase> testSuite,
                                                                        Multiset<Node> components) {
        return synthesizeWithForbidden(testSuite, components, new ArrayList<>());
    }

    public Optional<Node> synthesizeNodeWithForbidden(List<TestCase> testSuite,
                                                                                         Multiset<Node> components,
                                                                                         List<Program> forbidden) {
        Optional<Pair<Program, Map<Parameter, Constant>>> result = synthesizeWithForbidden(testSuite, components, forbidden);
        if (!result.isPresent())
            return Optional.empty();

        return Optional.of(Traverse.substitute(result.get().getLeft().getSemantics(), result.get().getRight()));
    }



    private List<Node> testToConstraint(TestCase testCase, BranchOutput output) {
        List<Node> clauses = new ArrayList<>();
        List<Node> testClauses = testCase.getConstraints(output);
        for (Node clause : testClauses) {
            clauses.add(instantiate(clause, testCase));
        }
        return clauses;
    }

    /**
     * @return encoding plus a list of disabled selectors for each forbidden program (empty if impossible)
     */
    private Pair<List<Node>, List<List<Selector>>> encodeBranch(BranchOutput output, int size, List<Component> components, List<Program> forbidden) {
        tree.put(output, new ArrayList<>());
        choices.put(output, new ArrayList<>());

        List<Node> clauses = new ArrayList<>();
        List<Component> relevantComponents = new ArrayList<>(components);
        relevantComponents.removeIf(c -> !TypeInference.typeOf(c).equals(output.getType()));
        List<Component> leafComponents = new ArrayList<>(relevantComponents);
        leafComponents.removeIf(c -> !(c.isLeaf()));
        List<Component> functionComponents = new ArrayList<>(relevantComponents);
        functionComponents.removeIf(Component::isLeaf);

        List<List<Selector>> forbiddenSelectors = new ArrayList<>();
        for (Program program : forbidden) {
            forbiddenSelectors.add(new ArrayList<>());
        }

        for (Component component : leafComponents) {
            Selector selector = new Selector();
            for (int i=0; i < forbidden.size(); i++) {
                if (forbidden.get(i).getRoot().getSemantics().equals(component.getSemantics())) {
                    forbiddenSelectors.get(i).add(selector);
                }
            }
            clauses.add(new Impl(selector, new Equal(output, component.getSemantics())));
            selected.put(selector, component);
            choices.get(output).add(selector);
        }
        if (size > 1) {
            List<BranchOutput> lazyChildren = new ArrayList<>();
            Map<Hole, List<List<Selector>>> subnodeForbiddenSelectors = new HashMap<>();
            for (Component component : functionComponents) {
                boolean infeasibleComponent = false;
                Set<BranchOutput> children = new HashSet<>(lazyChildren);
                Map<Hole, Node> args = new HashMap<>();
                for (Hole input : component.getInputs()) {
                    Optional<BranchOutput> child = children.stream().filter(o -> o.getType().equals(input.getType())).findFirst();
                    if (child.isPresent()) {
                        args.put(input, child.get());
                        children.remove(child.get());
                    } else {
                        BranchOutput branch = new BranchOutput(input.getType());
                        List<Program> subnodeForbidden = forbidden.stream().map(f -> f.getChildren().get(input)).collect(Collectors.toList());
                        Pair<List<Node>, List<List<Selector>>> childClauses = encodeBranch(branch, size - 1, components, subnodeForbidden);
                        subnodeForbiddenSelectors.put(input, childClauses.getRight());
                        if (childClauses.getLeft().isEmpty()) {
                            infeasibleComponent = true;
                            break;
                        }
                        clauses.addAll(childClauses.getLeft());
                        lazyChildren.add(branch);
                        args.put(input, branch);
                    }
                }
                if (infeasibleComponent) {
                    continue;
                }
                Selector selector = new Selector();
                for (int i=0; i < forbidden.size(); i++) {
                    if (forbidden.get(i).getRoot().getSemantics().equals(component.getSemantics())) {
                        forbiddenSelectors.get(i).add(selector);
                    }
                }
                clauses.add(new Impl(selector, new Equal(output, Traverse.substitute(component.getSemantics(), args))));
                selected.put(selector, component);
                choices.get(output).add(selector);
            }
            for (int i=0; i < forbidden.size(); i++) {
                if (!forbiddenSelectors.get(i).isEmpty()) {
                    for (List<List<Selector>> lists : subnodeForbiddenSelectors.values()) {
                        if (lists.get(i).isEmpty()) {
                            forbiddenSelectors.set(i, new ArrayList<>());
                            break;
                        } else {
                            forbiddenSelectors.get(i).addAll(lists.get(i));
                        }
                    }
                }
            }
            tree.put(output, lazyChildren);
        }
        return new ImmutablePair<>(clauses, forbiddenSelectors);
    }

    private Node disjunction(List<? extends Node> clauses) {
        Node node = BoolConst.FALSE;
        for (Node clause : clauses) {
            node = new Or(node, clause);
        }
        return node;
    }

    private Pair<Program, Map<Parameter, Constant>> decode(Map<Variable, Constant> assignment,
                                                           BranchOutput root) {
        List<Selector> nodeChoices = choices.get(root);
        Selector choice = nodeChoices.stream().filter(s -> assignment.get(s).equals(BoolConst.TRUE)).findFirst().get();
        Component component = selected.get(choice);
        Map<Parameter, Constant> parameterValuation = new HashMap<>();
        if (component.getSemantics() instanceof Parameter) {
            Parameter p = (Parameter) component.getSemantics();
            parameterValuation.put(p, assignment.get(p));
        }

        if (component.isLeaf()) {
            return new ImmutablePair<>(Program.leaf(component), parameterValuation);
        }

        Map<Hole, Program> args = new HashMap<>();
        List<BranchOutput> children = new ArrayList<>(tree.get(root));
        for (Hole input : component.getInputs()) {
            BranchOutput child = children.stream().filter(o -> o.getType().equals(input.getType())).findFirst().get();
            children.remove(child);
            Pair<Program, Map<Parameter, Constant>> subresult = decode(assignment, child);
            parameterValuation.putAll(subresult.getRight());
            args.put(input, subresult.getLeft());
        }

        return new ImmutablePair<>(Program.app(component, args), parameterValuation);
    }

    public Node instantiate(Node node, TestCase testCase) {
        return Traverse.transform(node, n -> {
            if (n instanceof ProgramVariable) {
                return new TestInstance((ProgramVariable)n, testCase);
            } else if (n instanceof BranchOutput) {
                return new TestInstance((BranchOutput)n, testCase);
            }
            return n;
        });
    }

}
