package sg.edu.nus.comp.codis.ast.theory;

import sg.edu.nus.comp.codis.ast.Application;
import sg.edu.nus.comp.codis.ast.Node;

/**
 * Created by Sergey Mechtaev on 8/4/2016.
 */
public abstract class UnaryOp extends Application {
    public abstract Node getArg();
}
