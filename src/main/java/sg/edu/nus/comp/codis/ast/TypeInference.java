package sg.edu.nus.comp.codis.ast;

import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.ArrayList;
import java.util.Collections;
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

        public TypeCheckVisitor() {
            this.types = new Stack<>();
            this.typeError = false;
        }

        private Type getType() throws TypeInferenceException {
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
        public void visit(LocationVariable locationVariable) {
            if (typeError) return;
            types.push(IntType.TYPE);
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
            if (types.size() < 2 || !types.pop().equals(IntType.TYPE) || !types.pop().equals(IntType.TYPE)) {
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
        public void visit(ComponentInstance componentInstance) {
            if (typeError) return;
            if (types.size() < 1)
                typeError = true;
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
    }

}
