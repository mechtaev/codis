package sg.edu.nus.comp.codis;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Equal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 *
 * Represent constraints over inputs and output. Defines physical equality.
 */
public abstract class TestCase {

    public abstract List<Node> getConstraints(Variable output);

    public abstract Type getOutputType();

    public static TestCase ofAssignment(Map<ProgramVariable, ? extends Node> assignment, Node outputValue) {
        ArrayList<Node> inputClauses = new ArrayList<>();
        for (Map.Entry<ProgramVariable, ? extends Node> entry : assignment.entrySet()) {
            inputClauses.add(new Equal(entry.getKey(), entry.getValue()));
        }
        return new TestCase() {
            @Override
            public List<Node> getConstraints(Variable output) {
                ArrayList<Node> clauses = new ArrayList<>();
                clauses.addAll(inputClauses);
                clauses.add(new Equal(output, outputValue));
                return clauses;
            }

            @Override
            public Type getOutputType() {
                return TypeInference.typeOf(outputValue);
            }
        };

    }

    private String id = null;

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        if (id != null) {
            return id;
        }
        String repr = "{ ";
        for (Node clause : this.getConstraints(new ProgramVariable("output", this.getOutputType()))) {
            repr += clause + " ";
        }
        repr += "}";
        return repr;
    }

}
