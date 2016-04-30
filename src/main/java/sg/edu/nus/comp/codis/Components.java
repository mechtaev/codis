package sg.edu.nus.comp.codis;

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


    private static final int BV_SIZE = 32;

    private static final Hole t = new Hole("t", new BVType(BV_SIZE), Node.class);
    private static final Hole p = new Hole("p", new BVType(BV_SIZE), Node.class);
    private static final Hole v = new Hole("v", new BVType(BV_SIZE), Node.class);

    public static final Node BVADD = new BVAdd(t, p);
    public static final Node BVSUB = new BVSub(t, p);
    public static final Node BVDIV = new BVSignedDiv(t, p);
    public static final Node BVUDIV = new BVUnsignedDiv(t, p);
    public static final Node BVSREM = new BVSignedRemainder(t, p);
    public static final Node BVUREM = new BVUnsignedRemainder(t, p);
    public static final Node BVSMOD = new BVSignedModulo(t, p);
    public static final Node BVMUL = new BVMult(t, p);
    public static final Node BVNEG = new BVNeg(t);

    public static final Node BVUGT = new BVUnsignedGreater(t, p);
    public static final Node BVUGE = new BVUnsignedGreaterOrEqual(t, p);
    public static final Node BVULT = new BVUnsignedLess(t, p);
    public static final Node BVULE = new BVUnsignedLessOrEqual(t, p);

    public static final Node BVSGT = new BVSignedGreater(t, p);
    public static final Node BVSGE = new BVSignedGreaterOrEqual(t, p);
    public static final Node BVSLT = new BVSignedLess(t, p);
    public static final Node BVSLE = new BVSignedLessOrEqual(t, p);

    public static final Node BVSHL = new BVShiftLeft(t, p);
    public static final Node BVLSHR = new BVUnsignedShiftRight(t, p);
    public static final Node BVASHR = new BVSignedShiftRight(t, p);

    public static final Node BVAND = new BVAnd(t, p);
    public static final Node BVOR = new BVOr(t, p);
    public static final Node BVNOT = new BVNot(t);

    public static final Node BVITE = new ITE(new Not(new Equal(v, BVConst.ofLong(0, BV_SIZE))), t, p);

}
