package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Variable used in CBS. Test instantiated
 */
public class ComponentOutput extends Variable {

    private Component component;

    public ComponentOutput(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
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
        if (!(obj instanceof ComponentOutput))
            return false;
        if (obj == this)
            return true;

        ComponentOutput rhs = (ComponentOutput) obj;
        return new EqualsBuilder().
                append(component, rhs.component).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(component).
                toHashCode();
    }

    @Override
    public String toString() {
        return "co(" + component + ")";
    }

    @Override
    public Type getType() {
        return TypeInference.typeOf(component);
    }

    @Override
    public boolean isTestInstantiable() {
        return true;
    }
}
