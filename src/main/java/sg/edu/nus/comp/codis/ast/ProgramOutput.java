package sg.edu.nus.comp.codis.ast;

/**
 * Variable used for CODIS, conflict learning, evaluation. Test-instantiated. Physical equality.
 */
public class ProgramOutput extends Variable {

    private Type type;

    public Type getType() {
        return type;
    }

    public ProgramOutput(Type type) {
        this.type = type;
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
    public String toString() {
        return "Output";
    }

}
