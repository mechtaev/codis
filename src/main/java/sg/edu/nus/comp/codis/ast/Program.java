package sg.edu.nus.comp.codis.ast;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Sergey Mechtaev on 2/5/2016.
 *
 * Programs are either leafs of applications. They implement physical equality, but it should not be used anyway
 */
public class Program {

    private Component root;
    private Map<Hole, Program> children;

    private Program(Component root, Map<Hole, Program> children) {
        this.root = root;
        this.children = children;
    }

    public static Program leaf(Component c) {
        return new Program(c, new HashMap<>());
    }

    public static Program app(Component function, Map<Hole, Program> arguments) {
        return new Program(function, arguments);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public Node getSemantics() {
        return getSemantics(new HashMap<>());
    }

    public Node getSemantics(Map<Parameter, Constant> parameterValuation) {
        Node semantics;
        if (isLeaf()) {
            semantics = root.getSemantics();
        } else {
            Map<Hole, Node> map = children.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(),
                                                                                        e -> e.getValue().getSemantics()));
            semantics = Traverse.substitute(root.getSemantics(), map);
        }
        return Traverse.substitute(semantics, parameterValuation);
    }

    @Override
    public String toString() {
        return this.getSemantics().toString();
    }
}
