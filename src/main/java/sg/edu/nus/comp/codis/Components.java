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


    private Components(int size) {
        t = new Hole("t", new BVType(size), Node.class);
        p = new Hole("p", new BVType(size), Node.class);
        BVADD = new BVAdd(t, p);
        BVSUB = new BVSub(t, p);
        BVDIV = new BVSignedDiv(t, p);
        BVUDIV = new BVUnsignedDiv(t, p);
        BVSREM = new BVSignedRemainder(t, p);
        BVUREM = new BVUnsignedRemainder(t, p);
        BVSMOD = new BVSignedModulo(t, p);
        BVMUL = new BVMult(t, p);
        BVNEG = new BVNeg(t);
        BVUGT = new BVUnsignedGreater(t, p);
        BVUGE = new BVUnsignedGreaterOrEqual(t, p);
        BVULT = new BVUnsignedLess(t, p);
        BVULE = new BVUnsignedLessOrEqual(t, p);
        BVSGT = new BVSignedGreater(t, p);
        BVSGE = new BVSignedGreaterOrEqual(t, p);
        BVSLT = new BVSignedLess(t, p);
        BVSLE = new BVSignedLessOrEqual(t, p);
        BVSHL = new BVShiftLeft(t, p);
        BVLSHR = new BVUnsignedShiftRight(t, p);
        BVASHR = new BVSignedShiftRight(t, p);
        BVAND = new BVAnd(t, p);
        BVOR = new BVOr(t, p);
        BVXOR = new BVXor(t, p);
        BVNOT = new BVNot(t);
        BVITE = new ITE(a, t, p);
    }

    public static Components ofSize(int size) {
        return new Components(size);

    }

    private final Hole t;
    private final Hole p;

    public final BinaryOp BVADD;
    public final BinaryOp BVSUB;
    public final BinaryOp BVDIV;
    public final BinaryOp BVUDIV;
    public final BinaryOp BVSREM;
    public final BinaryOp BVUREM;
    public final BinaryOp BVSMOD;
    public final BinaryOp BVMUL;
    public final UnaryOp BVNEG;

    public final BinaryOp BVUGT;
    public final BinaryOp BVUGE;
    public final BinaryOp BVULT;
    public final BinaryOp BVULE;

    public final BinaryOp BVSGT;
    public final BinaryOp BVSGE;
    public final BinaryOp BVSLT;
    public final BinaryOp BVSLE;

    public final BinaryOp BVSHL;
    public final BinaryOp BVLSHR;
    public final BinaryOp BVASHR;

    public final BinaryOp BVAND;
    public final BinaryOp BVOR;
    public final BinaryOp BVXOR;
    public final UnaryOp BVNOT;

    public final Application BVITE;
}
