package sg.edu.nus.comp.codis.ast;

import sg.edu.nus.comp.codis.ast.theory.*;

/**
 * Created by Sergey Mechtaev on 8/4/2016.
 */
public interface TopDownVisitor {
    void visit(ProgramVariable programVariable);

    void visit(Location location);

    void visit(UIFApplication UIFApplication);

    void visit(Equal equal);

    void visit(Add add);

    void visit(Sub sub);

    void visit(Mult mult);

    void visit(Div div);

    void visit(And and);

    void visit(Or or);

    void visit(Iff iff);

    void visit(Impl impl);

    void visit(Greater greater);

    void visit(Less less);

    void visit(GreaterOrEqual greaterOrEqual);

    void visit(LessOrEqual lessOrEqual);

    void visit(Minus minus);

    void visit(Not not);

    void visit(IntConst intConst);

    void visit(BoolConst boolConst);

    void visit(ComponentInput componentInput);

    void visit(ComponentOutput componentOutput);

    void visit(TestInstance testInstance);

    void visit(Parameter parameter);

    void visit(Hole hole);

    void visit(ITE ite);

    void visit(Selector selector);

    void visit(BVConst bvConst);

    void visit(BVAdd bvAdd);

    void visit(BVAnd bvAnd);

    void visit(BVMult bvMult);

    void visit(BVNeg bvNeg);

    void visit(BVNot bvNot);

    void visit(BVOr bvOr);

    void visit(BVShiftLeft bvShiftLeft);

    void visit(BVSignedDiv bvSignedDiv);

    void visit(BVSignedGreater bvSignedGreater);

    void visit(BVSignedGreaterOrEqual bvSignedGreaterOrEqual);

    void visit(BVSignedLess bvSignedLess);

    void visit(BVSignedLessOrEqual bvSignedLessOrEqual);

    void visit(BVSignedModulo bvSignedModulo);

    void visit(BVSignedRemainder bvSignedRemainder);

    void visit(BVSignedShiftRight bvSignedShiftRight);

    void visit(BVSub bvSub);

    void visit(BVUnsignedDiv bvUnsignedDiv);

    void visit(BVUnsignedGreater bvUnsignedGreater);

    void visit(BVUnsignedGreaterOrEqual bvUnsignedGreaterOrEqual);

    void visit(BVUnsignedLess bvUnsignedLess);

    void visit(BVUnsignedLessOrEqual bvUnsignedLessOrEqual);

    void visit(BVUnsignedRemainder bvUnsignedRemainder);

    void visit(BVUnsignedShiftRight bvUnsignedShiftRight);
}
