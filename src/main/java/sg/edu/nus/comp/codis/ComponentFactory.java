package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class ComponentFactory {

    private static Hole x = new Hole("x", IntType.TYPE, Node.class);
    private static Hole y = new Hole("y", IntType.TYPE, Node.class);

    private static Hole a = new Hole("a", BoolType.TYPE, Node.class);
    private static Hole b = new Hole("b", BoolType.TYPE, Node.class);

    public static Component buildAdd() {
        return new Component(new Add(x, y));
    }

    public static Component buildSub() {
        return new Component(new Sub(x, y));
    }

    public static Component buildMult() {
        return new Component(new Mult(x, y));
    }

    public static Component buildDiv() {
        return new Component(new Div(x, y));
    }

    public static Component buildGreater() {
        return new Component(new Greater(x, y));
    }

    public static Component buildGreaterOfEqual() {
        return new Component(new GreaterOrEqual(x, y));
    }

    public static Component buildLess() {
        return new Component(new Less(x, y));
    }

    public static Component buildLessOrEqual() {
        return new Component(new Add(x, y));
    }

    public static Component buildMinus() {
        return new Component(new Minus(x));
    }

    public static Component buildIff() {
        return new Component(new Iff(a, b));
    }

    public static Component buildImpl() {
        return new Component(new Impl(a, b));
    }

    public static Component buildAnd() {
        return new Component(new And(a, b));
    }

    public static Component buildOr() {
        return new Component(new Or(a, b));
    }

    public static Component buildNot() {
        return new Component(new Not(a));
    }

    public static Component buildVariable(ProgramVariable variable) {
        return new Component(variable);
    }

    public static Component buildParameter(Parameter parameter) {
        return new Component(parameter);
    }

    public static Component buildIntConst(IntConst intConst) {
        return new Component(intConst);
    }

    public static Component buildBoolConst(BoolConst boolConst) {
        return new Component(boolConst);
    }

}
