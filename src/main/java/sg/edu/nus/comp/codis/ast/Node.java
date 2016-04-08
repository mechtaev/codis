package sg.edu.nus.comp.codis.ast;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public interface Node {
    void accept(BottomUpVisitor visitor);
    void accept(TopDownVisitor visitor);
}
