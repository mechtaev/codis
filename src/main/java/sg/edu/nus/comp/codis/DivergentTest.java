package sg.edu.nus.comp.codis;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Equal;
import sg.edu.nus.comp.codis.ast.theory.Not;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Created by Sergey Mechtaev on 14/4/2016.
 */
public class DivergentTest {

    private Solver solver;

    public DivergentTest(Solver solver) {
        this.solver = solver;
    }

    public Optional<Triple<TestCase, Node, Node>> generate(Map<Node, Integer> componentMultiset,
                                                           ArrayList<TestCase> testSuite) {
        assert !testSuite.isEmpty();

        Type outputType = TypeInference.typeOf(testSuite.get(0).getOutput());

        Map<ProgramVariable, Parameter> parametricAssignment = new HashMap<>();
        for (ProgramVariable variable : testSuite.get(0).getAssignment().keySet()) {
            parametricAssignment.put(variable, new Parameter("<generatedInput>" + variable.getName(), variable.getType()));
        }

        ArrayList<Component> components1 = CBS.flattenComponentMultiset(componentMultiset);
        Component result1 = new Component(new Hole("result", outputType, Node.class));
        Parameter output1 = new Parameter("<generatedOutput1>", outputType);
        TestCase newTest1 = new TestCase(parametricAssignment, output1);
        ArrayList<TestCase> testSuite1 = new ArrayList<>(testSuite);
        testSuite1.add(newTest1);
        ArrayList<Node> clauses1 = CBS.encode(testSuite1, components1, result1);

        ArrayList<Component> components2 = CBS.flattenComponentMultiset(componentMultiset);
        Component result2 = new Component(new Hole("result", outputType, Node.class));
        Parameter output2 = new Parameter("<generatedOutput2>", outputType);
        TestCase newTest2 = new TestCase(parametricAssignment, output2);
        ArrayList<TestCase> testSuite2 = new ArrayList<>(testSuite);
        testSuite2.add(newTest2);
        ArrayList<Node> clauses2 = CBS.encode(testSuite2, components2, result2);

        ArrayList<Node> clauses = new ArrayList<>();

        clauses.addAll(clauses1);
        clauses.addAll(clauses2);

        clauses.add(new Not(new Equal(output1, output2)));

        Optional<Map<Variable, Constant>> model = solver.getModel(clauses);
        if (!model.isPresent()) {
            return Optional.empty();
        }

        Node program1 = CBS.decode(model.get(), components1, result1);
        Node program2 = CBS.decode(model.get(), components2, result2);

        Map<ProgramVariable, Node> newAssignment = new HashMap<>();
        for (ProgramVariable variable : parametricAssignment.keySet()) {
            newAssignment.put(variable, CBS.substituteParameters(model.get(), parametricAssignment.get(variable)));
        }

        TestCase newTest = new TestCase(newAssignment, CBS.substituteParameters(model.get(), output1));
        return Optional.of(new ImmutableTriple<>(newTest, program1, program2));
    }
}
