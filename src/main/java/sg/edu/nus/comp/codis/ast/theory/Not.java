package sg.edu.nus.comp.codis.ast.theory;

import sg.edu.nus.comp.codis.Unifier;
import sg.edu.nus.comp.codis.ast.BottomUpVisitor;
import sg.edu.nus.comp.codis.ast.Application;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.TopDownVisitor;

import java.util.ArrayList;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class Not extends UnaryOp {
    private Node arg;

    public Node getArg() {
        return arg;
    }

    public Not(Node arg) {
        this.arg = arg;
    }

    @Override
    public void accept(BottomUpVisitor visitor) {
        arg.accept(visitor);
        visitor.visit(this);
    }

    @Override
    public void accept(TopDownVisitor visitor) {
        visitor.visit(this);
        arg.accept(visitor);
    }

    @Override
    public ArrayList<Node> getArgs() {
        ArrayList<Node> result = new ArrayList<>();
        result.add(arg);
        return result;
    }

    @Override
    public String toString() {
        return "!" + arg.toString();
    }

}
