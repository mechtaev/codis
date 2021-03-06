package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
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
 * Created by Sergey Mechtaev on 7/4/2016.
 *
 * Oracle-guided Component-based Program Synthesis, ICSE'10
 */
public class ComponentBasedSynthesis implements Synthesis {

    private Logger logger = LoggerFactory.getLogger(ComponentBasedSynthesis.class);

    private Solver solver;

    private Type encodingType;
    private Optional<Integer> sizeBound;

    public ComponentBasedSynthesis(Solver solver, boolean useBV32, Optional<Integer> sizeBound) {
        this.solver = solver;
        if (useBV32) {
            encodingType = new BVType(32);
        } else {
            encodingType = IntType.TYPE;
        }
        this.sizeBound = sizeBound;
    }

    @Override
    public Optional<Pair<Program, Map<Parameter, Constant>>> synthesize(List<? extends TestCase> testSuite, Multiset<Node> components) {
        List<Component> flattenedComponents = components.stream().map(Component::new).collect(Collectors.toList());
        Type outputType = testSuite.get(0).getOutputType();
        Component result = new Component(new Hole("result", outputType, Node.class));
        List<Node> clauses = encode(testSuite, flattenedComponents, result);

        Optional<Map<Variable, Constant>> assignment = solver.getModel(clauses);
        if (assignment.isPresent()) {
            return Optional.of(decode(assignment.get(), flattenedComponents, result));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public List<Pair<Program, Map<Parameter, Constant>>> synthesizeAll(List<? extends TestCase> testSuite, Multiset<Node> components) {
        // Don't know how to synthesize all with this encoding
        throw new UnsupportedOperationException();
    }

    /**
     * Allocating components on an interval
     *
     * Possible improvements:
     * 1. Forbid multiple occurrences
     * 2. Take into account hole superclass field
     * 3. Why not to fix the root output location? Maybe it will be more efficient
     *
     * Currently, size bound indicates maximum number of non-leaf components. It is a good definition?
     */
    public List<Node> wellFormedness(List<Component> components, Component result) {
        List<Component> variableComponents = new ArrayList<>(components);
        variableComponents.removeIf(c -> !(c.isLeaf()));
        List<Component> functionComponents = new ArrayList<>(components);
        functionComponents.removeIf(Component::isLeaf);

        Pair<Integer, Integer> variableOutputInterval = new ImmutablePair<>(0, variableComponents.size());
        Pair<Integer, Integer> functionOutputInterval = new ImmutablePair<>(variableComponents.size(), components.size());
        Pair<Integer, Integer> inputInterval = new ImmutablePair<>(0, components.size());

        List<Node> intervalConstraints = new ArrayList<>();
        for (Component component : components) {
            Location outputLocation = new Location(new ComponentOutput(component), encodingType);
            if (component.getSemantics() instanceof Variable || component.getSemantics() instanceof Constant) {
                intervalConstraints.add(insideInterval(outputLocation, variableOutputInterval));
            } else {
                intervalConstraints.add(insideInterval(outputLocation, functionOutputInterval));
            }
            for (Hole hole : component.getInputs()) {
                Location inputLocation = new Location(new ComponentInput(component, hole), encodingType);
                intervalConstraints.add(insideInterval(inputLocation, inputInterval));
            }
        }
        Pair<Integer, Integer> resultInputInterval = inputInterval;
        if (sizeBound.isPresent()) {
            resultInputInterval = new ImmutablePair<>(0, Math.min(components.size(), variableComponents.size() + sizeBound.get()));
        }
        Hole resultHole = new ArrayList<>(result.getInputs()).get(0);
        Location resultInputLocation = new Location(new ComponentInput(result, resultHole), encodingType);
        Node resultInterval = insideInterval(resultInputLocation, resultInputInterval);

        List<Node> consistencyConstraints = new ArrayList<>();
        for (Component firstComponent : components) {
            for (Component secondComponent : components) {
                if (!firstComponent.equals(secondComponent)) {
                    Location firstLocation = new Location(new ComponentOutput(firstComponent), encodingType);
                    Location secondLocation = new Location(new ComponentOutput(secondComponent), encodingType);
                    consistencyConstraints.add(new Not(new Equal(firstLocation, secondLocation)));
                }
            }
        }

        List<Node> acyclicityConstraints = new ArrayList<>();
        for (Component component : components) {
            Location outputLocation = new Location(new ComponentOutput(component), encodingType);
            for (Hole hole : component.getInputs()) {
                Location inputLocation = new Location(new ComponentInput(component, hole), encodingType);
                if (encodingType instanceof BVType) {
                    acyclicityConstraints.add(new BVUnsignedLess(inputLocation, outputLocation));
                } else {
                    acyclicityConstraints.add(new Less(inputLocation, outputLocation));
                }
            }
        }

        List<Node> clauses = new ArrayList<>();
        clauses.addAll(intervalConstraints);
        clauses.add(resultInterval);
        clauses.addAll(consistencyConstraints);
        clauses.addAll(acyclicityConstraints);
        return clauses;
    }

    /**
     * insideInterval(v, (l, r)) returns l <= v < r
     */
    private Node insideInterval(Variable v, Pair<Integer, Integer> interval) {
        if (encodingType instanceof BVType) {
            BVConst left = BVConst.ofLong(interval.getLeft(), ((BVType) encodingType).getSize());
            BVConst right = BVConst.ofLong(interval.getRight(), ((BVType) encodingType).getSize());
            return new And(new BVUnsignedLessOrEqual(left, v), new BVUnsignedLess(v, right));
        } else {
            return new And(new LessOrEqual(IntConst.of(interval.getLeft()), v), new Less(v, IntConst.of(interval.getRight())));
        }
    }

    public List<Node> library(List<Component> components, Component result) {
        List<Node> libraryConstraints = new ArrayList<>();
        for (Component component : components) {
            ComponentOutput output = new ComponentOutput(component);
            Map<Hole, ComponentInput> inputMapping =
                    component.getInputs().stream().collect(Collectors.toMap(Function.identity(),
                            hole -> new ComponentInput(component, hole)));
            Node instantiation = Traverse.substitute(component.getSemantics(), inputMapping);
            libraryConstraints.add(new Equal(output, instantiation));
        }
        Hole resultHole = new ArrayList<>(result.getInputs()).get(0);
        Node resultLib = new Equal(new ComponentInput(result, resultHole), new ComponentOutput(result));
        libraryConstraints.add(resultLib);
        return libraryConstraints;
    }

    public List<Node> connection(List<Component> components, Component result) {
        Set<ComponentOutput> outputs = new HashSet<>();
        Set<ComponentInput> inputs = new HashSet<>();
        for (Component component : components) {
            outputs.add(new ComponentOutput(component));
            for (Hole hole : component.getInputs()) {
                inputs.add(new ComponentInput(component, hole));
            }
        }

        Hole resultHole = new ArrayList<>(result.getInputs()).get(0);
        inputs.add(new ComponentInput(result, resultHole));
        List<Node> clauses = new ArrayList<>();

        for (ComponentOutput co : outputs) {
            for (ComponentInput ci : inputs) {
                if (TypeInference.typeOf(ci).equals(TypeInference.typeOf(co)) &&
                        ci.getHole().getSuperclass().isInstance(co.getComponent().getSemantics())) {
                    clauses.add(new Impl(new Equal(new Location(ci, encodingType), new Location(co, encodingType)),
                                         new Equal(ci, co)));
                } else {
                    clauses.add(new Not(new Equal(new Location(ci, encodingType), new Location(co, encodingType))));
                }
            }
        }
        return clauses;
    }

    public Node instantiate(Node node, TestCase testCase) {
        return Traverse.transform(node, n -> {
            if (n instanceof ProgramVariable) {
                return new TestInstance((ProgramVariable)n, testCase);
            } else if (n instanceof ComponentOutput) {
                return new TestInstance((ComponentOutput)n, testCase);
            } else if (n instanceof ComponentInput) {
                return new TestInstance((ComponentInput)n, testCase);
            }
            return n;
        });
    }

    public List<Node> testToConstraint(TestCase testCase, Component result) {
        List<Node> clauses = new ArrayList<>();
        List<Node> testClauses = testCase.getConstraints(new ComponentOutput(result));
        for (Node clause : testClauses) {
            clauses.add(instantiate(clause, testCase));
        }
        return clauses;
    }

    public List<Node> encode(List<? extends TestCase> testSuite, List<Component> components, Component result) {
        List<Node> wfp = wellFormedness(components, result);
        List<Node> lib = library(components, result);
        List<Node> connections = connection(components, result);
        List<Node> clauses = new ArrayList<>();
        clauses.addAll(wfp);
        for (TestCase test : testSuite) {
            for (Node node : lib) {
                clauses.add(instantiate(node, test));
            }
            for (Node node : connections) {
                clauses.add(instantiate(node, test));
            }
            clauses.addAll(testToConstraint(test, result));
        }
        return clauses;
    }

    public Pair<Program, Map<Parameter, Constant>> decode(Map<Variable, Constant> assignment, List<Component> components, Component result) {
        Hole resultHole = new ArrayList<>(result.getInputs()).get(0);
        Location root = new Location(new ComponentInput(result, resultHole), encodingType);
        Map<Parameter, Constant> parameterValuation = new HashMap<>();
        for (Variable variable : assignment.keySet()) {
            if (variable instanceof Parameter) {
                parameterValuation.put((Parameter) variable, assignment.get(variable));
            }
        }
        Program program = buildFromRoot(assignment, components, root);
        return new ImmutablePair<>(program, parameterValuation);
    }

    private Program buildFromRoot(Map<Variable, Constant> assignment, List<Component> components, Location root) {
        if (!assignment.containsKey(root)) {
            throw new RuntimeException("undefined location");
        }
        Constant location = assignment.get(root);
        Optional<Variable> reference = Optional.empty();
        for (Map.Entry<Variable, Constant> entry : assignment.entrySet()) {
            if (entry.getKey() instanceof Location) {
                Variable variable = ((Location)entry.getKey()).getVariable();
                if (variable instanceof ComponentOutput &&
                        components.contains(((ComponentOutput)variable).getComponent()) &&
                        entry.getValue().equals(location)) {
                    reference = Optional.of(variable);
                    break;
                }
            }
        }
        if (!reference.isPresent()) {
            throw new RuntimeException("dangling pointer");
        }
        Component rootComponent = ((ComponentOutput)reference.get()).getComponent();
        Map<Hole, Program> mapping = rootComponent.getInputs().stream().collect(Collectors.toMap(Function.identity(),
                hole -> buildFromRoot(assignment,
                                      components,
                                      new Location(new ComponentInput(rootComponent, hole), encodingType))));
        return Program.app(rootComponent, mapping);
    }
}
