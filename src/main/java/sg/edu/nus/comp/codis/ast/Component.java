package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 *
 */
public class Component {

    // Node with holes
    private Node semantics;

    public Component(Node semantics) {
        this.semantics = semantics;
        objectCounter = classCounter;
        classCounter++;
    }

    public Node getSemantics() {
        return semantics;
    }

    public List<Hole> getInputs() {
        return Traverse.collectByType(this.semantics, Hole.class);
    }

    public boolean isLeaf() {
        return this.getInputs().isEmpty();
    }

    public Type getType() {
        return TypeInference.typeOf(semantics);
    }

    @Override
    public String toString() {
        return semantics.toString();
    }

    private static int classCounter = 0;
    private final int objectCounter;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Component))
            return false;
        if (obj == this)
            return true;

        Component rhs = (Component) obj;
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


}
