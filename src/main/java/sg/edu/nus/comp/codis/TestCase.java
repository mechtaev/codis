package sg.edu.nus.comp.codis;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import sg.edu.nus.comp.codis.ast.Constant;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.ProgramVariable;

import java.util.Map;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class TestCase {

    private Map<ProgramVariable, Node> assignment;

    private Constant output;

    public TestCase(Map<ProgramVariable, Node> assignment, Constant output) {
        this.assignment = assignment;
        this.output = output;
    }

    public Map<ProgramVariable, Node> getAssignment() {
        return assignment;
    }

    public Constant getOutput() {
        return output;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TestCase))
            return false;
        if (obj == this)
            return true;

        TestCase rhs = (TestCase) obj;
        return new EqualsBuilder().
                append(assignment, rhs.assignment).
                append(output, rhs.output).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(assignment).
                append(output).
                toHashCode();
    }

    @Override
    public String toString() {
        String inputs = "";
        boolean first = true;
        for (ProgramVariable variable : assignment.keySet()) {
            if (first) {
                inputs += variable + "=" + assignment.get(variable);
                first = false;
            } else {
                inputs += ", " + variable + "=" + assignment.get(variable);
            }
        }
        return "{ " + inputs + " -> " + output + " }";
    }

}
