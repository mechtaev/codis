package sg.edu.nus.comp.codis.ast;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 *
 * Components implement physical equality because we need multisets
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

    @Override
    public String toString() {
        return semantics.toString();
    }

}
