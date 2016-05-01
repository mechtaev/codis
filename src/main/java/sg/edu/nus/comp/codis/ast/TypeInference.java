package sg.edu.nus.comp.codis.ast;

import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Stack;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class TypeInference {

    public static Type typeOf(Node node) {
        try {
            return checkType(node); //FIXME: get type without type checking
        } catch (TypeInferenceException e) {
            throw new RuntimeException("failed to get type");
        }
    }

    public static Type typeOf(Component component) {
        return typeOf(component.getSemantics());
    }

    public static Type checkType(Node node) throws TypeInferenceException {
        TypeCheckVisitor visitor = new TypeCheckVisitor();
        node.accept(visitor);
        return visitor.getType();
    }

    public static Type checkType(Component component) throws TypeInferenceException {
        return checkType(component.getSemantics());
    }

    private static class TypeCheckVisitor implements BottomUpVisitor {

        private Stack<Type> types;

        private boolean typeError;

        TypeCheckVisitor() {
            this.types = new Stack<>();
            this.typeError = false;
        }

        Type getType() throws TypeInferenceException {
            if (types.size() != 1 || typeError) {
                throw new TypeInferenceException();
            }
            return types.peek();
        }

        @Override
        public void visit(ProgramVariable programVariable) {
            if (typeError) return;
            types.push(programVariable.getType());
        }

        @Override
        public void visit(Location location) {
            if (typeError) return;
            types.push(location.getType());
        }

        @Override
        public void visit(UIFApplication UIFApplication) {
            if (typeError) return;
            ArrayList<Type> argTypes = new ArrayList<>(UIFApplication.getUIF().getArgTypes());
            if (types.size() < argTypes.size()) {
                typeError = true;
                return;
            }
            Collections.reverse(argTypes);
            for (Type argType : argTypes) {
                if (!argType.equals(types.pop())) {
                    typeError = true;
                    return;
                }
            }
            types.push(UIFApplication.getUIF().getType());
        }

        @Override
        public void visit(Equal equal) {
            if (typeError) return;
            if (types.size() < 2 || !(types.pop().equals(types.pop()))) { // polymorphic equality
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(Add add) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(IntType.TYPE) || !types.pop().equals(IntType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(IntType.TYPE);
        }

        @Override
        public void visit(Sub sub) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(IntType.TYPE) || !types.pop().equals(IntType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(IntType.TYPE);

        }

        @Override
        public void visit(Mult mult) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(IntType.TYPE) || !types.pop().equals(IntType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(IntType.TYPE);

        }

        @Override
        public void visit(Div div) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(IntType.TYPE) || !types.pop().equals(IntType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(IntType.TYPE);
        }

        @Override
        public void visit(And and) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(BoolType.TYPE) || !types.pop().equals(BoolType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(Or or) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(BoolType.TYPE) || !types.pop().equals(BoolType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(Iff iff) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(BoolType.TYPE) || !types.pop().equals(BoolType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(Impl impl) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(BoolType.TYPE) || !types.pop().equals(BoolType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(Greater greater) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(IntType.TYPE) || !types.pop().equals(IntType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(Less less) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(IntType.TYPE) || !types.pop().equals(IntType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(GreaterOrEqual greaterOrEqual) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(IntType.TYPE) || !types.pop().equals(IntType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(LessOrEqual lessOrEqual) {
            if (typeError) return;
            if (types.size() < 2 || !types.pop().equals(IntType.TYPE) || !types.pop().equals(IntType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(Minus minus) {
            if (typeError) return;
            if (types.size() < 1 || !types.pop().equals(IntType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(IntType.TYPE);
        }

        @Override
        public void visit(Not not) {
            if (typeError) return;
            if (types.size() < 1 || !types.pop().equals(BoolType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(IntConst intConst) {
            if (typeError) return;
            types.push(IntType.TYPE);
        }

        @Override
        public void visit(BoolConst boolConst) {
            if (typeError) return;
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(ComponentInput componentInput) {
            if (typeError) return;
            types.push(componentInput.getHole().getType());
        }

        @Override
        public void visit(ComponentOutput componentOutput) {
            if (typeError) return;
            types.push(typeOf(componentOutput.getComponent()));
        }

        @Override
        public void visit(TestInstance testInstance) {
            if (typeError) return;
            types.push(typeOf(testInstance.getVariable()));
        }

        @Override
        public void visit(Parameter parameter) {
            if (typeError) return;
            types.push(parameter.getType());
        }

        @Override
        public void visit(Hole hole) {
            if (typeError) return;
            types.push(hole.getType());
        }

        @Override
        public void visit(ITE ite) {
            if (typeError) return;
            if (types.size() < 3) {
                typeError = true;
                return;
            }
            Type second = types.pop();
            Type first = types.pop();
            Type condition = types.pop();
            if (!first.equals(second) || !condition.equals(BoolType.TYPE)) {
                typeError = true;
                return;
            }
            types.push(first);
        }

        @Override
        public void visit(Selector selector) {
            if (typeError) return;
            types.push(BoolType.TYPE);
        }

        @Override
        public void visit(BVConst bvConst) {
            if (typeError) return;
            types.push(bvConst.getType());
        }

        private Optional<BVType> checkBVBinaryOpTypes(BinaryOp op) { // not used explicitly but verify that binaryop
            if (types.size() < 2) {
                typeError = true;
                return Optional.empty();
            }
            Type right = types.pop();
            Type left = types.pop();
            if (!(right instanceof BVType) || !right.equals(left)) {
                typeError = true;
                return Optional.empty();
            }
            return Optional.of((BVType)right);
        }

        private Optional<BVType> checkBVUnaryOpTypes(UnaryOp op) {
            if (types.size() < 1) {
                typeError = true;
                return Optional.empty();
            }
            Type arg = types.pop();
            if (!(arg instanceof BVType)) {
                typeError = true;
                return Optional.empty();
            }
            return Optional.of((BVType)arg);
        }


        @Override
        public void visit(BVAdd bvAdd) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvAdd);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVAnd bvAnd) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvAnd);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVMult bvMult) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvMult);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVNeg bvNeg) {
            if (typeError) return;
            Optional<BVType> type = checkBVUnaryOpTypes(bvNeg);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVNot bvNot) {
            if (typeError) return;
            Optional<BVType> type = checkBVUnaryOpTypes(bvNot);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVOr bvOr) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvOr);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVShiftLeft bvShiftLeft) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvShiftLeft);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVSignedDiv bvSignedDiv) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvSignedDiv);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVSignedGreater bvSignedGreater) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvSignedGreater);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(BoolType.TYPE);
        }

        @Override
        public void visit(BVSignedGreaterOrEqual bvSignedGreaterOrEqual) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvSignedGreaterOrEqual);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(BoolType.TYPE);
        }

        @Override
        public void visit(BVSignedLess bvSignedLess) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvSignedLess);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(BoolType.TYPE);
        }

        @Override
        public void visit(BVSignedLessOrEqual bvSignedLessOrEqual) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvSignedLessOrEqual);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(BoolType.TYPE);
        }

        @Override
        public void visit(BVSignedModulo bvSignedModulo) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvSignedModulo);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVSignedRemainder bvSignedRemainder) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvSignedRemainder);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVSignedShiftRight bvSignedShiftRight) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvSignedShiftRight);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVSub bvSub) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvSub);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVUnsignedDiv bvUnsignedDiv) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvUnsignedDiv);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVUnsignedGreater bvUnsignedGreater) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvUnsignedGreater);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(BoolType.TYPE);
        }

        @Override
        public void visit(BVUnsignedGreaterOrEqual bvUnsignedGreaterOrEqual) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvUnsignedGreaterOrEqual);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(BoolType.TYPE);
        }

        @Override
        public void visit(BVUnsignedLess bvUnsignedLess) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvUnsignedLess);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(BoolType.TYPE);
        }

        @Override
        public void visit(BVUnsignedLessOrEqual bvUnsignedLessOrEqual) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvUnsignedLessOrEqual);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(BoolType.TYPE);
        }

        @Override
        public void visit(BVUnsignedRemainder bvUnsignedRemainder) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvUnsignedRemainder);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BVUnsignedShiftRight bvUnsignedShiftRight) {
            if (typeError) return;
            Optional<BVType> type = checkBVBinaryOpTypes(bvUnsignedShiftRight);
            if (!type.isPresent()) {
                typeError = true;
                return;
            }
            this.types.push(type.get());
        }

        @Override
        public void visit(BranchOutput branchOutput) {
            if (typeError) return;
            types.push(branchOutput.getType());
        }

    }

}
