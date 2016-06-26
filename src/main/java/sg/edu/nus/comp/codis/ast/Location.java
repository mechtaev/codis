package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class Location extends Variable {

    private Variable variable;
    private Type type;

    public Variable getVariable() {
        return variable;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean isTestInstantiable() {
        return false;
    }

    public Location(Variable variable, Type type) {
        assert variable instanceof ComponentInput || variable instanceof ComponentOutput;
        assert type instanceof IntType || type instanceof BVType;
        this.variable = variable;
        this.type = type;
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
    public void accept(BottomUpMemoVisitor visitor) {
        if (visitor.alreadyVisited(this)) {
            visitor.visitAgain(this);
        } else {
            visitor.visit(this);
        }
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
