package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import sg.edu.nus.comp.codis.TestCase;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class ComponentInstance extends Variable {

    private TestCase test;

    private Variable variable;

    public ComponentInstance(Variable variable, TestCase test) {
        assert variable instanceof ComponentInput || variable instanceof ComponentOutput;
        this.variable = variable;
        this.test = test;
    }

    public Variable getVariable() {
        return variable;
    }

    public TestCase getTest() {
        return test;
    }

    @Override
    public void accept(BottomUpVisitor visitor) {
        variable.accept(visitor);
        visitor.visit(this);
    }

    @Override
    public void accept(TopDownVisitor visitor) {
        visitor.visit(this);
        variable.accept(visitor);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComponentInstance))
            return false;
        if (obj == this)
            return true;

        ComponentInstance rhs = (ComponentInstance) obj;
        return new EqualsBuilder().
                append(variable, rhs.variable).
                append(test, rhs.test).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(variable).
                append(test).
                toHashCode();
    }

    @Override
    public String toString() {
        return "i(" + variable + ")[" + test + "]";
    }


}
