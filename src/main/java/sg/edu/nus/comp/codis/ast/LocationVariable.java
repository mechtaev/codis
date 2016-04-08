package sg.edu.nus.comp.codis.ast;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class LocationVariable extends Variable {

    private Variable variable;

    public Variable getVariable() {
        return variable;
    }

    public LocationVariable(Variable variable) {
        assert variable instanceof ComponentInput || variable instanceof ComponentOutput;
        this.variable = variable;
    }

    @Override
    public String toString() {
        return "l(" + variable + ")";
    }


    @Override
    public void accept(BottomUpVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(TopDownVisitor visitor) {
        visitor.visit(this);
    }

}
