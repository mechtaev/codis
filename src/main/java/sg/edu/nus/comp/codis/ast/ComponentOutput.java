package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
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

}
