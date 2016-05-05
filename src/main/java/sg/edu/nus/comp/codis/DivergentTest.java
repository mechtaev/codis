package sg.edu.nus.comp.codis;

import com.google.common.collect.Multiset;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Equal;
import sg.edu.nus.comp.codis.ast.theory.Not;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Created by Sergey Mechtaev on 14/4/2016.
 */
public class DivergentTest {

    private Solver solver;

    public DivergentTest(Solver solver) {
        this.solver = solver;
    }

    public Optional<Triple<TestCase, Node, Node>> generate(Multiset<Node> components,
                                                           List<TestCase> testSuite,
                                                           List<ProgramVariable> inputVariables) {
        assert !testSuite.isEmpty();

        CBS cbs = new CBS(Z3.getInstance(), false, Optional.empty()); // integer only for now

        Type outputType = testSuite.get(0).getOutputType();

        Map<ProgramVariable, Parameter> parametricAssignment = new HashMap<>();
        for (ProgramVariable variable : inputVariables) {
            parametricAssignment.put(variable, new Parameter("<generatedInput>" + variable.getName(), variable.getType()));
        }

        List<Component> flattenedComponents1 = components.stream().map(Component::new).collect(Collectors.toList());;
        Component result1 = new Component(new Hole("result", outputType, Node.class));
        Parameter output1 = new Parameter("<generatedOutput1>", outputType);
        TestCase newTest1 = TestCase.ofAssignment(parametricAssignment, output1);
        List<TestCase> testSuite1 = new ArrayList<>(testSuite);
        testSuite1.add(newTest1);
        List<Node> clauses1 = cbs.encode(testSuite1, flattenedComponents1, result1);

        List<Component> flattenedComponents2 = components.stream().map(Component::new).collect(Collectors.toList());
        Component result2 = new Component(new Hole("result", outputType, Node.class));
        Parameter output2 = new Parameter("<generatedOutput2>", outputType);
        TestCase newTest2 = TestCase.ofAssignment(parametricAssignment, output2);
        List<TestCase> testSuite2 = new ArrayList<>(testSuite);
        testSuite2.add(newTest2);
        List<Node> clauses2 = cbs.encode(testSuite2, flattenedComponents2, result2);

        List<Node> clauses = new ArrayList<>();

        clauses.addAll(clauses1);
        clauses.addAll(clauses2);

        clauses.add(new Not(new Equal(output1, output2)));

        Optional<Map<Variable, Constant>> model = solver.getModel(clauses);
        if (!model.isPresent()) {
            return Optional.empty();
        }

        Pair<Program, Map<Parameter, Constant>> p1 = cbs.decode(model.get(), flattenedComponents1, result1);
        Pair<Program, Map<Parameter, Constant>> p2 = cbs.decode(model.get(), flattenedComponents2, result2);
        Map<Parameter, Constant> parameterValuation = new HashMap<>();
        parameterValuation.putAll(p1.getRight());
        parameterValuation.putAll(p2.getRight());

        Map<ProgramVariable, Node> newAssignment = new HashMap<>();
        for (ProgramVariable variable : parametricAssignment.keySet()) {
            newAssignment.put(variable, parameterValuation.get(parametricAssignment.get(variable)));
        }

        TestCase newTest = TestCase.ofAssignment(newAssignment, parameterValuation.get(output1));
        return Optional.of(new ImmutableTriple<>(newTest,
                p1.getLeft().getSemantics(p1.getRight()),
                p2.getLeft().getSemantics(p2.getRight())));
    }
}
