package sg.edu.nus.comp.codis;

import fj.P;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 *
 * Oracle-guided Component-based Program Synthesis, ICSE'10
 */
public class CBS implements Synthesis {

    private Logger logger = LoggerFactory.getLogger(CBS.class);

    private Solver solver;

    public CBS(Solver solver) {
        this.solver = solver;
    }

    @Override
    public Optional<Node> synthesize(ArrayList<TestCase> testSuite, Map<Node, Integer> componentMultiset) {
        ArrayList<Component> components = flattenComponentMultiset(componentMultiset);
        Type outputType = TypeInference.typeOf(testSuite.get(0).getOutput());
        Component result = new Component(new Hole("result", outputType, Node.class));
        ArrayList<Node> clauses = encode(testSuite, components, result);
        Optional<Map<Variable, Constant>> assignment = solver.getModel(clauses);
        if (assignment.isPresent()) {
            return Optional.of(decode(assignment.get(), result));
        } else {
            return Optional.empty();
        }
    }

    private ArrayList<Component> flattenComponentMultiset(Map<Node, Integer> componentMultiset) {
        ArrayList<Component> components = new ArrayList<>();
        for (Node node : componentMultiset.keySet()) {
            IntStream.range(0, componentMultiset.get(node)).forEach(i -> components.add(new Component(node)));
        }
        return components;
    }

    /**
     * Allocating components on an interval
     *
     * Possible improvements:
     * 1. Allocate variable before functions
     * 2. Forbid multiple occurrences
     * 3. Take into account hole superclass field
     */
    public static ArrayList<Node> wellFormedness(ArrayList<Component> components, Component result) {
        Map<Type, Integer> outputNum = new HashMap<>();
        for (Component component : components) {
            Type type = TypeInference.typeOf(component);
            if (outputNum.containsKey(type)) {
                outputNum.put(type, outputNum.get(type) + 1);
            } else {
                outputNum.put(type, 1);
            }
        }

        Map<Type, Pair<Integer, Integer>> outputIntervals = new HashMap<>();
        int lastTypeIndex = 0;
        for (Type type : outputNum.keySet()) {
            Pair<Integer, Integer> interval = new ImmutablePair<>(lastTypeIndex, lastTypeIndex + outputNum.get(type));
            lastTypeIndex = lastTypeIndex + outputNum.get(type);
            outputIntervals.put(type, interval);
        }

        ArrayList<Node> intervalConstraints = new ArrayList<>();
        for (Component component : components) {
            Location outputLocation = new Location(new ComponentOutput(component));
            Pair<Integer, Integer> outputInterval = outputIntervals.get(TypeInference.typeOf(component));
            intervalConstraints.add(insideInterval(outputLocation, outputInterval));
            for (Hole hole : component.getInputs()) {
                Location inputLocation = new Location(new ComponentInput(component, hole));
                Pair<Integer, Integer> inputInterval = outputIntervals.get(TypeInference.typeOf(hole));
                intervalConstraints.add(insideInterval(inputLocation, inputInterval));
            }
        }
        Hole resultHole = new ArrayList<>(result.getInputs()).get(0);
        Location resultInputLocation = new Location(new ComponentInput(result, resultHole));
        Pair<Integer, Integer> inputInterval = outputIntervals.get(TypeInference.typeOf(resultHole));
        Node resultInterval = insideInterval(resultInputLocation, inputInterval);

        ArrayList<Node> consistencyConstraints = new ArrayList<>();
        for (Component firstComponent : components) {
            for (Component secondComponent : components) {
                if (!firstComponent.equals(secondComponent)) {
                    Location firstLocation = new Location(new ComponentOutput(firstComponent));
                    Location secondLocation = new Location(new ComponentOutput(secondComponent));
                    consistencyConstraints.add(new Not(new Equal(firstLocation, secondLocation)));
                }
            }
        }

        ArrayList<Node> acyclicityConstraints = new ArrayList<>();
        for (Component component : components) {
            Location outputLocation = new Location(new ComponentOutput(component));
            for (Hole hole : component.getInputs()) {
                Location inputLocation = new Location(new ComponentInput(component, hole));
                acyclicityConstraints.add(new Less(inputLocation, outputLocation));
            }
        }

        ArrayList<Node> clauses = new ArrayList<>();
        clauses.addAll(intervalConstraints);
        clauses.add(resultInterval);
        clauses.addAll(consistencyConstraints);
        clauses.addAll(acyclicityConstraints);
        return clauses;
    }

    /**
     * insideInterval(v, (l, r)) returns l <= v < r
     */
    private static Node insideInterval(Variable v, Pair<Integer, Integer> interval) {
        return new And(new LessOrEqual(IntConst.of(interval.getLeft()), v), new Less(v, IntConst.of(interval.getRight())));
    }

    public static ArrayList<Node> library(ArrayList<Component> components, Component result) {
        ArrayList<Node> libraryConstraints = new ArrayList<>();
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

    public static ArrayList<Node> connection(ArrayList<Component> components, Component result) {
        Map<Type, Set<ComponentOutput>> outputs = new HashMap<>();
        Map<Type, Set<ComponentInput>> inputs = new HashMap<>();
        for (Component component : components) {
            Type outputType = TypeInference.typeOf(component);
            if (!outputs.containsKey(outputType)) {
                outputs.put(outputType, new HashSet<>());
            }
            outputs.get(outputType).add(new ComponentOutput(component));
            for (Hole hole : component.getInputs()) {
                Type inputType = hole.getType();
                if (!inputs.containsKey(inputType)) {
                    inputs.put(inputType, new HashSet<>());
                }
                inputs.get(inputType).add(new ComponentInput(component, hole));
            }
        }
        Hole resultHole = new ArrayList<>(result.getInputs()).get(0);
        if (!inputs.containsKey(resultHole.getType())) {
            inputs.put(resultHole.getType(), new HashSet<>());
        }
        // note that result output does not have location
        inputs.get(resultHole.getType()).add(new ComponentInput(result, resultHole));
        ArrayList<Node> clauses = new ArrayList<>();
        for (Type type : outputs.keySet()) {
            for (ComponentOutput co: outputs.get(type)) {
                for (ComponentInput ci : inputs.get(type)) {
                    clauses.add(new Impl(new Equal(new Location(ci), new Location(co)), new Equal(ci, co)));
                }
            }
        }
        return clauses;
    }

    public static Node instantiate(Node node, TestCase testCase) {
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

    public static ArrayList<Node> testToConstraint(TestCase testCase, Component result) {
        ArrayList<Node> clauses = new ArrayList<>();
        clauses.add(new Equal(new TestInstance(new ComponentOutput(result), testCase), testCase.getOutput()));
        for (ProgramVariable variable : testCase.getAssignment().keySet()) {
            Node value = instantiate(testCase.getAssignment().get(variable), testCase);
            clauses.add(new Equal(new TestInstance(variable, testCase), value));
        }
        return clauses;
    }

    private ArrayList<Node> encode(ArrayList<TestCase> testSuite, ArrayList<Component> components, Component result) {
        ArrayList<Node> wfp = wellFormedness(components, result);
        ArrayList<Node> lib = library(components, result);
        ArrayList<Node> connections = connection(components, result);
        ArrayList<Node> clauses = new ArrayList<>();
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

    private Node decode(Map<Variable, Constant> assignment, Component result) {
        Hole resultHole = new ArrayList<>(result.getInputs()).get(0);
        Location root = new Location(new ComponentInput(result, resultHole));
        return buildFromRoot(assignment, root);
    }

    private Node buildFromRoot(Map<Variable, Constant> assignment, Location root) {
        if (!assignment.containsKey(root)) {
            throw new RuntimeException("undefined location");
        }
        Constant location = assignment.get(root);
        Optional<Variable> reference = Optional.empty();
        for (Map.Entry<Variable, Constant> entry : assignment.entrySet()) {
            if (entry.getKey() instanceof Location) {
                Variable variable = ((Location)entry.getKey()).getVariable();
                if (variable instanceof ComponentOutput && entry.getValue().equals(location)) {
                    reference = Optional.of(variable);
                    break;
                }
            }
        }
        if (!reference.isPresent()) {
            throw new RuntimeException("dangling pointer");
        }
        Component rootComponent = ((ComponentOutput)reference.get()).getComponent();
        Map<Variable, Node> mapping = rootComponent.getInputs().stream().collect(Collectors.toMap(Function.identity(),
                hole -> buildFromRoot(assignment, new Location(new ComponentInput(rootComponent, hole)))));
        return Traverse.substitute(rootComponent.getSemantics(), mapping);
    }
}
