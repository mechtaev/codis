package sg.edu.nus.comp.codis.ast;

/**
 * Variable value of which we don't care about. Implements physical equality. Test instantiated
 */
public class Dummy extends Variable {
    private Type type;

    public Type getType() {
        return type;
    }

    @Override
    public boolean isTestInstantiable() {
        return true;
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
    public void accept(BottomUpMemoVisitor visitor) {
        if (visitor.alreadyVisited(this)) {
            visitor.visitAgain(this);
        } else {
            visitor.visit(this);
        }
    }

    @Override
    public String toString() {
        return "Dummy";
    }

}
