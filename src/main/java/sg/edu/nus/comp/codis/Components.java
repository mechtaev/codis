package sg.edu.nus.comp.codis;

import fj.P;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

/**
 * Created by Sergey Mechtaev on 14/4/2016.
 */
public class Components {

    private static final Hole i = new Hole("i", IntType.TYPE, Node.class);
    private static final Hole j = new Hole("j", IntType.TYPE, Node.class);
    private static final Hole a = new Hole("a", BoolType.TYPE, Node.class);
    private static final Hole b = new Hole("b", BoolType.TYPE, Node.class);

    public static final Node ADD = new Add(i, j);
    public static final Node SUB = new Sub(i, j);
    public static final Node DIV = new Div(i, j);
    public static final Node MUL = new Mult(i, j);
    public static final Node MINUS = new Minus(i);

    public static final Node GT = new Greater(i, j);
    public static final Node GE = new GreaterOrEqual(i, j);
    public static final Node LT = new Less(i, j);
    public static final Node LE = new LessOrEqual(i, j);

    public static final Node AND = new And(a, b);
    public static final Node OR = new Or(a, b);
    public static final Node IMP = new Impl(a, b);
    public static final Node IFF = new Iff(a, b);
    public static final Node NOT = new Not(a);

    public static final Node ITE = new ITE(a, i, j);

}
