package sg.edu.nus.comp.codis;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Equal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class AssignmentTestCase implements TestCase {

    private Map<ProgramVariable, ? extends Node> assignment;
    private Node outputValue;

    public AssignmentTestCase(Map<ProgramVariable, ? extends Node> assignment, Node outputValue) {
        objectCounter = classCounter;
        classCounter++;

        this.assignment = assignment;
        this.outputValue = outputValue;
    }

    @Override
    public List<Node> getConstraints(Variable output) {
        ArrayList<Node> inputClauses = new ArrayList<>();
        for (Map.Entry<ProgramVariable, ? extends Node> entry : assignment.entrySet()) {
            inputClauses.add(new Equal(entry.getKey(), entry.getValue()));
        }
        ArrayList<Node> clauses = new ArrayList<>();
        clauses.addAll(inputClauses);
        clauses.add(new Equal(output, outputValue));
        return clauses;
    }

    @Override
    public Type getOutputType() {
        return TypeInference.typeOf(outputValue);
    }

    public Map<ProgramVariable, ? extends Node> getAssignment() {
        return assignment;
    }

    public Node getOutputValue() {
        return outputValue;
    }

    private String id = null;

    public void setId(String id) {
        this.id = id;
    }

    private static int classCounter = 0;
    private final int objectCounter;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AssignmentTestCase))
            return false;
        if (obj == this)
            return true;

        AssignmentTestCase rhs = (AssignmentTestCase) obj;
        return new EqualsBuilder().
                append(objectCounter, rhs.objectCounter).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(objectCounter).
                toHashCode();
    }


    @Override
    public String toString() {
        if (id != null) {
            return id;
        }
        String repr = "{ ";
        for (Node clause : this.getConstraints(new ProgramVariable("Result", this.getOutputType()))) {
            repr += clause + " ";
        }
        repr += "}";
        return repr;
    }

}
