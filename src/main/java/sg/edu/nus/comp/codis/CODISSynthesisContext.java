package sg.edu.nus.comp.codis;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.Equal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Sergey Mechtaev on 20/9/2016.
 */
class CODISSynthesisContext implements TestCase {

    private TestCase outerTest;
    private Pair<Program, Map<Parameter, Constant>> context;
    private Variable globalOutput;
    private Component leaf;

    CODISSynthesisContext(TestCase outerTest, Pair<Program, Map<Parameter, Constant>> context, Component leaf) {
        objectCounter = classCounter;
        classCounter++;

        this.outerTest = outerTest;
        this.context = context;
        this.leaf = leaf;
        this.globalOutput = new ProgramOutput(outerTest.getOutputType());
    }

    public TestCase getOuterTest() {
        return outerTest;
    }

    @Override
    public List<Node> getConstraints(Variable output) {
        List<Node> constraints = new ArrayList<>();

        constraints.addAll(outerTest.getConstraints(globalOutput));

        Map<Component, Program> mapping = new HashMap<>();
        mapping.put(leaf, Program.leaf(new Component(output)));
        Program substituted = context.getLeft().substitute(mapping);
        Node contextClause = new Equal(globalOutput, substituted.getSemantics(context.getRight()));
        constraints.add(contextClause);

        //NOTE: I need to relax constraints for parameters that are not used or only used in the leaf
        for (Map.Entry<Parameter, Constant> parameterConstantEntry : context.getRight().entrySet()) {
            Parameter p = parameterConstantEntry.getKey();
            Constant v = parameterConstantEntry.getValue();
            if (!substituted.getSemantics().contains(p)) {
                continue;
            }
            constraints.add(new Equal(p, v));
        }

        return constraints;
    }

    @Override
    public Type getOutputType() {
        return TypeInference.typeOf(leaf);
    }

    private static int classCounter = 0;
    private final int objectCounter;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CODISSynthesisContext))
            return false;
        if (obj == this)
            return true;

        CODISSynthesisContext rhs = (CODISSynthesisContext) obj;
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
