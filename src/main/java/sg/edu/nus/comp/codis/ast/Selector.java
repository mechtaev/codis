package sg.edu.nus.comp.codis.ast;

import sg.edu.nus.comp.codis.ast.BottomUpVisitor;
import sg.edu.nus.comp.codis.ast.TopDownVisitor;
import sg.edu.nus.comp.codis.ast.Variable;

/**
 * Created by Sergey Mechtaev on 16/4/2016.
 *
 * Selector is a boolean variable primarily used for assumptions, not test-instantiated
 * Selectors use physical equality
 */
public class Selector extends Variable {
    @Override
    public void accept(BottomUpVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(TopDownVisitor visitor) {
        visitor.visit(this);
    }
}
