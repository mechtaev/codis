package sg.edu.nus.comp.codis;

import com.microsoft.z3.*;
import fj.data.Either;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class Z3 implements Solver {

    private static final Z3 INSTANCE = new Z3();

    private Context ctx;
    private com.microsoft.z3.Solver solver;

    private Z3() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        cfg.put("unsat_core", "true");
        this.ctx = new Context(cfg);
        this.solver = ctx.mkSolver();
    }

    public static Z3 getInstance() {
        return INSTANCE;
    }

    public void dispose() {
        this.ctx.dispose();
    }

    @Override
    public Either<Map<Variable, Constant>, ArrayList<Node>> getModelOrCore(ArrayList<Node> clauses,
                                                                           ArrayList<Node> assumptions) {
        solver.reset();
        VariableMarshaller marshaller = new VariableMarshaller();
        for (Node clause : clauses) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(marshaller);
            clause.accept(visitor);
            solver.add((BoolExpr)visitor.getExpr());
        }
        ArrayList<Expr> assumptionExprs = new ArrayList<>();
        for (Node assumption : assumptions) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(marshaller);
            assumption.accept(visitor);
            assumptionExprs.add(visitor.getExpr());
        }
        Expr[] assumptionArray = assumptionExprs.toArray(new Expr[assumptionExprs.size() + 1]);
        assumptionArray[assumptionArray.length - 1] = ctx.mkBoolConst("fix");
        Status status = solver.check();
        if (status.equals(Status.SATISFIABLE)) {
            Model model = solver.getModel();
            return Either.left(getAssignment(model, marshaller));
        } else {
            //TODO extract unsat core
            throw new UnsupportedOperationException();
        }
    }

    private Map<Variable, Constant> getAssignment(Model model, VariableMarshaller marshaller) {
        HashMap<Variable, Constant> assingment = new HashMap<>();
        for (Variable variable: marshaller.getVariables()) {
            if (TypeInference.typeOf(variable).equals(IntType.TYPE)) {
                Expr result = model.eval(ctx.mkIntConst(marshaller.toString(variable)), true);
                if (result instanceof IntNum) {
                    int value = ((IntNum)result).getInt();
                    assingment.put(variable, IntConst.of(value));
                } else {
                    throw new RuntimeException("unsupported Z3 expression type");
                }
            } else if (TypeInference.typeOf(variable).equals(BoolType.TYPE)) {
                Expr result = model.eval(ctx.mkBoolConst(marshaller.toString(variable)), true);
                try {
                    boolean value = result.isTrue();
                    assingment.put(variable, BoolConst.of(value));
                } catch (Z3Exception ex){
                    throw new RuntimeException("wrong variable type");
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return assingment;
    }

    @Override
    public Optional<Map<Variable, Constant>> getModel(ArrayList<Node> clauses) {
        ArrayList<Node> assumptions = new ArrayList<>();
        Either<Map<Variable, Constant>, ArrayList<Node>> result = getModelOrCore(clauses, assumptions);
        if (result.isLeft()) {
            return Optional.of(result.left().value());
        } else {
            return Optional.empty();
        }
    }

    private class NodeTranslatorVisitor implements BottomUpVisitor {

        private Stack<Expr> exprs;
        private VariableMarshaller marshaller;

        NodeTranslatorVisitor(VariableMarshaller marshaller) {
            this.marshaller = marshaller;
            this.exprs = new Stack<>();
        }

        Expr getExpr() {
            assert exprs.size() == 1;
            return exprs.peek();
        }

        private void processVariable(Variable variable) {
            if (TypeInference.typeOf(variable).equals(IntType.TYPE)) {
                exprs.push(ctx.mkIntConst(marshaller.toString(variable)));
            } else if (TypeInference.typeOf(variable).equals(BoolType.TYPE)) {
                exprs.push(ctx.mkBoolConst(marshaller.toString(variable)));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public void visit(ProgramVariable programVariable) {
            processVariable(programVariable);
        }

        @Override
        public void visit(Location location) {
            processVariable(location);
        }

        @Override
        public void visit(UIFApplication UIFApplication) {
            //TODO
            throw new NotImplementedException();
        }

        @Override
        public void visit(Equal equal) {
            Expr right = exprs.pop();
            Expr left = exprs.pop();
            exprs.push(ctx.mkEq(left, right));
        }

        @Override
        public void visit(Add add) {
            ArithExpr right = (ArithExpr) exprs.pop();
            ArithExpr left = (ArithExpr) exprs.pop();
            exprs.push(ctx.mkAdd(left, right));
        }

        @Override
        public void visit(Sub sub) {
            ArithExpr right = (ArithExpr) exprs.pop();
            ArithExpr left = (ArithExpr) exprs.pop();
            exprs.push(ctx.mkSub(left, right));
        }

        @Override
        public void visit(Mult mult) {
            ArithExpr right = (ArithExpr) exprs.pop();
            ArithExpr left = (ArithExpr) exprs.pop();
            exprs.push(ctx.mkMul(left, right));
        }

        @Override
        public void visit(Div div) {
            ArithExpr right = (ArithExpr) exprs.pop();
            ArithExpr left = (ArithExpr) exprs.pop();
            exprs.push(ctx.mkDiv(left, right));
        }

        @Override
        public void visit(And and) {
            BoolExpr right = (BoolExpr) exprs.pop();
            BoolExpr left = (BoolExpr) exprs.pop();
            exprs.push(ctx.mkAnd(left, right));
        }

        @Override
        public void visit(Or or) {
            BoolExpr right = (BoolExpr) exprs.pop();
            BoolExpr left = (BoolExpr) exprs.pop();
            exprs.push(ctx.mkOr(left, right));
        }

        @Override
        public void visit(Iff iff) {
            BoolExpr right = (BoolExpr) exprs.pop();
            BoolExpr left = (BoolExpr) exprs.pop();
            exprs.push(ctx.mkIff(left, right));
        }

        @Override
        public void visit(Impl impl) {
            BoolExpr right = (BoolExpr) exprs.pop();
            BoolExpr left = (BoolExpr) exprs.pop();
            exprs.push(ctx.mkImplies(left, right));
        }

        @Override
        public void visit(Greater greater) {
            ArithExpr right = (ArithExpr) exprs.pop();
            ArithExpr left = (ArithExpr) exprs.pop();
            exprs.push(ctx.mkGt(left, right));
        }

        @Override
        public void visit(Less less) {
            ArithExpr right = (ArithExpr) exprs.pop();
            ArithExpr left = (ArithExpr) exprs.pop();
            exprs.push(ctx.mkLt(left, right));
        }

        @Override
        public void visit(GreaterOrEqual greaterOrEqual) {
            ArithExpr right = (ArithExpr) exprs.pop();
            ArithExpr left = (ArithExpr) exprs.pop();
            exprs.push(ctx.mkGe(left, right));
        }

        @Override
        public void visit(LessOrEqual lessOrEqual) {
            ArithExpr right = (ArithExpr) exprs.pop();
            ArithExpr left = (ArithExpr) exprs.pop();
            exprs.push(ctx.mkLe(left, right));
        }

        @Override
        public void visit(Minus minus) {
            exprs.push(ctx.mkUnaryMinus((ArithExpr) exprs.pop()));
        }

        @Override
        public void visit(Not not) {
            exprs.push(ctx.mkNot((BoolExpr) exprs.pop()));
        }

        @Override
        public void visit(IntConst intConst) {
            exprs.push(ctx.mkInt(intConst.getValue()));
        }

        @Override
        public void visit(BoolConst boolConst) {
            exprs.push(ctx.mkBool(boolConst.getValue()));
        }

        @Override
        public void visit(ComponentInput componentInput) {
            processVariable(componentInput);
        }

        @Override
        public void visit(ComponentOutput componentOutput) {
            processVariable(componentOutput);
        }

        @Override
        public void visit(TestInstance testInstance) {
            processVariable(testInstance);
        }

        @Override
        public void visit(Parameter parameter) {
            processVariable(parameter);
        }

        @Override
        public void visit(Hole hole) {
            processVariable(hole);
        }

    }

}
