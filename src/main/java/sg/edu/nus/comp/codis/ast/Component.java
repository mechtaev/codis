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

}
