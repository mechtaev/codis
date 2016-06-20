package sg.edu.nus.comp.codis.ast;

import java.util.function.Predicate;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public abstract class Node {
    public abstract void accept(BottomUpVisitor visitor);
    public abstract void accept(TopDownVisitor visitor);

    /**
     * Rename variables under condition
     */
    public Node index(int index, Predicate<Variable> p) {
        return Traverse.transform(this, n -> {
            if (n instanceof Variable && p.test((Variable)n)) {
                return new Indexed((Variable)n, index);
            }
            return n;
        });
    }

    /**
     * Rename variables so that the formulas for different tests can be conjoined
     */
    public Node instantiate(TestCase testCase) {
        return Traverse.transform(this, n -> {
            if (n instanceof Variable && ((Variable)n).isTestInstantiable()) {
                return new TestInstance((Variable)n, testCase);
            }
            return n;
        });
    }

    private static boolean seen;

    public boolean contains(Node subnode) {
        seen = false;
        Traverse.transform(this, n -> {
            if (n.equals(subnode)) {
                seen = true;
            }
            return n;
        });
        return seen;
    }
}
