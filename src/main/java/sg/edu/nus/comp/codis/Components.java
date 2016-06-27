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

    public static final BinaryOp ADD = new Add(i, j);
    public static final BinaryOp SUB = new Sub(i, j);
    public static final BinaryOp DIV = new Div(i, j);
    public static final BinaryOp MUL = new Mult(i, j);
    public static final UnaryOp MINUS = new Minus(i);

    public static final BinaryOp GT = new Greater(i, j);
    public static final BinaryOp GE = new GreaterOrEqual(i, j);
    public static final BinaryOp LT = new Less(i, j);
    public static final BinaryOp LE = new LessOrEqual(i, j);

    public static final BinaryOp AND = new And(a, b);
    public static final BinaryOp OR = new Or(a, b);
    public static final BinaryOp IMP = new Impl(a, b);
    public static final BinaryOp IFF = new Iff(a, b);
    public static final UnaryOp NOT = new Not(a);

    public static final Application ITE = new ITE(a, i, j);


    private static final int BV_SIZE = 32;

    private static final Hole t = new Hole("t", new BVType(BV_SIZE), Node.class);
    private static final Hole p = new Hole("p", new BVType(BV_SIZE), Node.class);
    private static final Hole v = new Hole("v", new BVType(BV_SIZE), Node.class);

    public static final BinaryOp BVADD = new BVAdd(t, p);
    public static final BinaryOp BVSUB = new BVSub(t, p);
    public static final BinaryOp BVDIV = new BVSignedDiv(t, p);
    public static final BinaryOp BVUDIV = new BVUnsignedDiv(t, p);
    public static final BinaryOp BVSREM = new BVSignedRemainder(t, p);
    public static final BinaryOp BVUREM = new BVUnsignedRemainder(t, p);
    public static final BinaryOp BVSMOD = new BVSignedModulo(t, p);
    public static final BinaryOp BVMUL = new BVMult(t, p);
    public static final UnaryOp BVNEG = new BVNeg(t);

    public static final BinaryOp BVUGT = new BVUnsignedGreater(t, p);
    public static final BinaryOp BVUGE = new BVUnsignedGreaterOrEqual(t, p);
    public static final BinaryOp BVULT = new BVUnsignedLess(t, p);
    public static final BinaryOp BVULE = new BVUnsignedLessOrEqual(t, p);

    public static final BinaryOp BVSGT = new BVSignedGreater(t, p);
    public static final BinaryOp BVSGE = new BVSignedGreaterOrEqual(t, p);
    public static final BinaryOp BVSLT = new BVSignedLess(t, p);
    public static final BinaryOp BVSLE = new BVSignedLessOrEqual(t, p);

    public static final BinaryOp BVSHL = new BVShiftLeft(t, p);
    public static final BinaryOp BVLSHR = new BVUnsignedShiftRight(t, p);
    public static final BinaryOp BVASHR = new BVSignedShiftRight(t, p);

    public static final BinaryOp BVAND = new BVAnd(t, p);
    public static final BinaryOp BVOR = new BVOr(t, p);
    public static final UnaryOp BVNOT = new BVNot(t);

    public static final Application BVITE = new ITE(a, t, p);
}
