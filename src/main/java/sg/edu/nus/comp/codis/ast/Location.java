package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class Location extends Variable {

    private Variable variable;

    public Variable getVariable() {
        return variable;
    }

    public Location(Variable variable) {
        assert variable instanceof ComponentInput || variable instanceof ComponentOutput;
        this.variable = variable;
    }

    @Override
    public String toString() {
        return "L(" + variable + ")";
    }


    @Override
    public void accept(BottomUpVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(TopDownVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Location))
            return false;
        if (obj == this)
            return true;

        Location rhs = (Location) obj;
        return new EqualsBuilder().
                append(variable, rhs.variable).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(variable).
                toHashCode();
    }

}
