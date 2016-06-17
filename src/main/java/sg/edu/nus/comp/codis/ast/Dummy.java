package sg.edu.nus.comp.codis.ast;

/**
 * Variable that implements physical equality
 */
public class Dummy extends Variable {
    private Type type;

    public Type getType() {
        return type;
    }

    public Dummy(Type type) {
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
        return "Dummy";
    }

}
