package sg.edu.nus.comp.codis;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.BoolConst;
import sg.edu.nus.comp.codis.ast.theory.Equal;
import sg.edu.nus.comp.codis.ast.theory.Impl;
import sg.edu.nus.comp.codis.ast.theory.Or;

import java.util.*;

/**
 * Created by Sergey Mechtaev on 2/5/2016.
 */
public class BoundedSynthesis extends Synthesis {

    private int bound;
    private Solver solver;
    private Map<BranchOutput, List<BranchOutput>> tree;
    private Map<BranchOutput, List<Selector>> choices;
    private Map<Selector, Component> selected;

    public BoundedSynthesis(Solver solver, int bound) {
        this.bound = bound;
        this.solver = solver;
    }

    @Override
    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<TestCase> testSuite,
                                                                        Map<Node, Integer> componentMultiset) {
        tree = new HashMap<>();
        selected = new HashMap<>();
        choices = new HashMap<>();

        ArrayList<Component> components = CBS.flattenComponentMultiset(componentMultiset);
        BranchOutput root = new BranchOutput(testSuite.get(0).getOutputType());
        List<Node> branchClauses = encodeBranch(root, bound, components);
        List<Node> clauses = new ArrayList<>();
        for (TestCase test : testSuite) {
            for (Node node : branchClauses) {
                clauses.add(instantiate(node, test));
            }
            clauses.addAll(testToConstraint(test, root));
        }
        for (List<Selector> selectors : choices.values()) {
            if (!selectors.isEmpty()) {
                clauses.add(disjunction(selectors));
            }
        }
        Optional<Map<Variable, Constant>> assignment = solver.getModel(clauses);
        if (assignment.isPresent()) {
            return Optional.of(decode(assignment.get(), components, root));
        } else {
            return Optional.empty();
        }
    }

    private ArrayList<Node> testToConstraint(TestCase testCase, BranchOutput output) {
        ArrayList<Node> clauses = new ArrayList<>();
        List<Node> testClauses = testCase.getConstraints(output);
        for (Node clause : testClauses) {
            clauses.add(instantiate(clause, testCase));
        }
        return clauses;
    }

    private List<Node> encodeBranch(BranchOutput output, int size, List<Component> components) {
        tree.put(output, new ArrayList<>());
        choices.put(output, new ArrayList<>());

        List<Node> clauses = new ArrayList<>();
        ArrayList<Component> relevantComponents = new ArrayList<>(components);
        relevantComponents.removeIf(c -> !TypeInference.typeOf(c).equals(output.getType()));
        ArrayList<Component> leafComponents = new ArrayList<>(relevantComponents);
        leafComponents.removeIf(c -> !(c.isLeaf()));
        ArrayList<Component> functionComponents = new ArrayList<>(relevantComponents);
        functionComponents.removeIf(Component::isLeaf);

        for (Component component : leafComponents) {
            Selector selector = new Selector();
            clauses.add(new Impl(selector, new Equal(output, component.getSemantics())));
            selected.put(selector, component);
            choices.get(output).add(selector);
        }
        if (size > 1) {
            List<BranchOutput> lazyChildren = new ArrayList<>();
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
                        List<Node> childClauses = encodeBranch(branch, size - 1, components);
                        if (childClauses.isEmpty()) {
                            infeasibleComponent = true;
                            break;
                        }
                        clauses.addAll(childClauses);
                        lazyChildren.add(branch);
                        args.put(input, branch);
                    }
                }
                if (infeasibleComponent) {
                    continue;
                }
                Selector selector = new Selector();
                clauses.add(new Impl(selector, new Equal(output, Traverse.substitute(component.getSemantics(), args))));
                selected.put(selector, component);
                choices.get(output).add(selector);
            }
            tree.put(output, lazyChildren);
        }
        return clauses;
    }

    private Node disjunction(List<? extends Node> clauses) {
        Node node = BoolConst.FALSE;
        for (Node clause : clauses) {
            node = new Or(node, clause);
        }
        return node;
    }

    private Pair<Program, Map<Parameter, Constant>> decode(Map<Variable, Constant> assignment,
                                                           ArrayList<Component> components,
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
            Pair<Program, Map<Parameter, Constant>> subresult = decode(assignment, components, child);
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
