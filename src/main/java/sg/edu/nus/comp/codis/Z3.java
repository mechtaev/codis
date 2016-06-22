package sg.edu.nus.comp.codis;

import com.microsoft.z3.*;
import fj.data.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class Z3 implements Solver, InterpolatingSolver {

    private Logger logger = LoggerFactory.getLogger(Z3.class);

    private Context globalContext;
    private com.microsoft.z3.Solver solver;

    private InterpolationContext globalIContext;
    private com.microsoft.z3.Solver iSolver;

    private Z3(boolean interpolating) {
        if (!interpolating) {
            HashMap<String, String> cfg = new HashMap<>();
            cfg.put("model", "true");
            this.globalContext = new Context(cfg);
            this.solver = globalContext.mkSolver();
        } else {
            HashMap<String, String> icfg = new HashMap<>();
            icfg.put("model", "true");
            icfg.put("proof", "true");
            this.globalIContext = new InterpolationContext(icfg);
            this.iSolver = globalIContext.mkSolver();
        }
    }

    public static Solver buildSolver() {
        return new Z3(false);
    }

    public static InterpolatingSolver buildInterpolatingSolver() {
        return new Z3(true);
    }

    public void dispose() {
        this.globalContext.dispose();
        this.globalIContext.dispose();
    }

    @Override
    public Either<Map<Variable, Constant>, List<Node>> getModelOrCore(List<Node> clauses,
                                                                      List<Node> assumptions) {

        solver.reset();
        VariableMarshaller marshaller = new VariableMarshaller();
        List<FuncDecl> decls = new ArrayList<>();
        for (Node clause : clauses) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(globalContext, marshaller);
            clause.accept(visitor);
            solver.add((BoolExpr)visitor.getExpr());
            decls.addAll(visitor.getDecls());
        }
        ArrayList<BoolExpr> assumptionExprs = new ArrayList<>();
        for (Node assumption : assumptions) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(globalContext, marshaller);
            assumption.accept(visitor);
            assumptionExprs.add((BoolExpr)visitor.getExpr());
            decls.addAll(visitor.getDecls());
        }

        if (logger.isTraceEnabled()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
            Date now = new Date();
            Path logsmt = Paths.get("cbs" + sdf.format(now) + ".smt2");
            try {
                List<String> entries = new ArrayList<>();
                entries.add("(set-option :produce-models true)");
                entries.addAll(new HashSet<>(decls.stream().map(Object::toString).collect(Collectors.toList())));
                entries.addAll(Arrays.asList(solver.getAssertions()).stream().map(c -> "(assert " + c + ")").collect(Collectors.toList()));
                entries.add("(check-sat)");
                entries.add("(get-model)");
                Files.write(logsmt, entries, Charset.forName("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BoolExpr[] assumptionArray = assumptionExprs.toArray(new BoolExpr[assumptionExprs.size()]);

        Status status = solver.check(assumptionArray);
        if (status.equals(Status.SATISFIABLE)) {
            Model model = solver.getModel();
            return Either.left(getAssignment(globalContext, model, marshaller));
        } else if (status.equals(Status.UNSATISFIABLE)) {
            ArrayList<Node> unsatCore = new ArrayList<>();
            Expr[] unsatCoreArray = solver.getUnsatCore();
            ArrayList<Expr> unsatCoreExprs = new ArrayList<>(Arrays.asList(unsatCoreArray));
            for (int i=0; i<assumptionExprs.size(); i++) {
                if (unsatCoreExprs.contains(assumptionExprs.get(i))) {
                    unsatCore.add(assumptions.get(i));
                }
            }
            return Either.right(unsatCore);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Map<Variable, Constant> getAssignment(Context ctx, Model model, VariableMarshaller marshaller) {
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
            } else if (TypeInference.typeOf(variable) instanceof BVType) {
                int size = ((BVType) TypeInference.typeOf(variable)).getSize();
                Expr result = model.eval(ctx.mkBVConst(marshaller.toString(variable), size), true);
                try {
                    long value = ((BitVecNum)result).getLong();
                    assingment.put(variable, BVConst.ofLong(value, size));
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
    public Optional<Map<Variable, Constant>> getModel(List<Node> clauses) {
        ArrayList<Node> assumptions = new ArrayList<>();
        Either<Map<Variable, Constant>, List<Node>> result = getModelOrCore(clauses, assumptions);
        if (result.isLeft()) {
            return Optional.of(result.left().value());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Either<Map<Variable, Constant>, Node> getModelOrInterpolant(List<Node> leftClauses, List<Node> rightClauses) {
        iSolver.reset();
        VariableMarshaller marshaller = new VariableMarshaller();
        //List<FuncDecl> decls = new ArrayList<>();
        BoolExpr left = globalIContext.mkBool(true);

        for (Node leftClause : leftClauses) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(globalIContext, marshaller);
            leftClause.accept(visitor);
            //decls.addAll(visitor.getDecls());
            left = globalIContext.mkAnd(left, (BoolExpr)visitor.getExpr());
        }
        BoolExpr right = globalIContext.mkBool(true);
        for (Node rightClause : rightClauses) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(globalIContext, marshaller);
            rightClause.accept(visitor);
            //decls.addAll(visitor.getDecls());
            right = globalIContext.mkAnd(right, (BoolExpr)visitor.getExpr());
        }

        iSolver.add(left);
        iSolver.add(right);

        Status status = iSolver.check();
        if (status.equals(Status.SATISFIABLE)) {
            Model model = iSolver.getModel();
            return Either.left(getAssignment(globalIContext, model, marshaller));
        } else if (status.equals(Status.UNSATISFIABLE)) {
            BoolExpr pat = globalIContext.mkAnd(globalIContext.MkInterpolant(left), right);
            Params params = globalIContext.mkParams();

            //Expr proof = iSolver.getProof();
            //Expr[] interps = globalIContext.GetInterpolant(proof, pat, params);
            //System.out.println("\nOK, the interpolant is: " + interps[0]);
            //return Either.right(convertZ3ToNode(interps[0], marshaller));
            return Either.right(new Dummy(BoolType.TYPE));
        } else {
            throw new UnsupportedOperationException();
        }

    }

    //TODO: boolean symbols true and false are not supported (if they are possible to get)
    private Node convertZ3ToNode(Expr expr, VariableMarshaller marshaller) {
        if (expr.isAdd()) {
            Expr[] args = expr.getArgs();
            return new Add(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isSub()) {
            Expr[] args = expr.getArgs();
            return new Sub(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isMul()) {
            Expr[] args = expr.getArgs();
            return new Mult(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isDiv()) {
            Expr[] args = expr.getArgs();
            return new Div(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isAnd()) {
            Expr[] args = expr.getArgs();
            return new And(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isOr()) {
            Expr[] args = expr.getArgs();
            return new Or(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isIff()) {
            Expr[] args = expr.getArgs();
            return new Iff(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isImplies()) {
            Expr[] args = expr.getArgs();
            return new Impl(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isGT()) {
            Expr[] args = expr.getArgs();
            return new Greater(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isLT()) {
            Expr[] args = expr.getArgs();
            return new Less(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isGE()) {
            Expr[] args = expr.getArgs();
            return new GreaterOrEqual(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isLE()) {
            Expr[] args = expr.getArgs();
            return new LessOrEqual(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isEq()) {
            Expr[] args = expr.getArgs();
            return new Equal(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isUMinus()) {
            Expr[] args = expr.getArgs();
            return new Minus(convertZ3ToNode(args[0], marshaller));
        } else if (expr.isNot()) {
            Expr[] args = expr.getArgs();
            return new Not(convertZ3ToNode(args[0], marshaller));
        } else if (expr.isIntNum()) {
            return IntConst.of(((IntNum)expr).getInt());
        } else if (expr.isConst()) {
            return marshaller.toVariable(expr.getFuncDecl().getName().toString());
        } else if (expr.isITE()) {
            Expr[] args = expr.getArgs();
            return new ITE(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller), convertZ3ToNode(args[2], marshaller));

        } else if (expr.isBVNumeral()) {
            BitVecNum num = (BitVecNum)expr;
            return BVConst.ofLong(num.getLong(), num.getSortSize());
        } else if (expr.isBVAdd()) {
            Expr[] args = expr.getArgs();
            return new BVAdd(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVSDiv()) {
            Expr[] args = expr.getArgs();
            return new BVSignedDiv(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVUDiv()) {
            Expr[] args = expr.getArgs();
            return new BVUnsignedDiv(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVMul()) {
            Expr[] args = expr.getArgs();
            return new BVMult(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVSub()) {
            Expr[] args = expr.getArgs();
            return new BVSub(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVUMinus()) {
            Expr[] args = expr.getArgs();
            return new BVNeg(convertZ3ToNode(args[0], marshaller));
        } else if (expr.isBVAND()) {
            Expr[] args = expr.getArgs();
            return new BVAnd(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVOR()) {
            Expr[] args = expr.getArgs();
            return new BVOr(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVNAND()) {
            Expr[] args = expr.getArgs();
            return new BVNand(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVXOR()) {
            Expr[] args = expr.getArgs();
            return new BVXor(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVXNOR()) {
            Expr[] args = expr.getArgs();
            return new BVXnor(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVNOR()) {
            Expr[] args = expr.getArgs();
            return new BVNor(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVNOT()) {
            Expr[] args = expr.getArgs();
            return new BVNot(convertZ3ToNode(args[0], marshaller));

        } else if (expr.isBVShiftLeft()) {
            Expr[] args = expr.getArgs();
            return new BVShiftLeft(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVShiftRightLogical()) {
            Expr[] args = expr.getArgs();
            return new BVUnsignedShiftRight(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVShiftRightArithmetic()) {
            Expr[] args = expr.getArgs();
            return new BVSignedShiftRight(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVSDiv()) {
            Expr[] args = expr.getArgs();
            return new BVSignedDiv(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVUDiv()) {
            Expr[] args = expr.getArgs();
            return new BVUnsignedDiv(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVSMod()) {
            Expr[] args = expr.getArgs();
            return new BVSignedModulo(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVSRem()) {
            Expr[] args = expr.getArgs();
            return new BVSignedRemainder(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVURem()) {
            Expr[] args = expr.getArgs();
            return new BVUnsignedRemainder(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));

        } else if (expr.isBVSGT()) {
            Expr[] args = expr.getArgs();
            return new BVSignedGreater(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVUGT()) {
            Expr[] args = expr.getArgs();
            return new BVUnsignedGreater(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVSGE()) {
            Expr[] args = expr.getArgs();
            return new BVSignedGreaterOrEqual(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVUGE()) {
            Expr[] args = expr.getArgs();
            return new BVUnsignedGreaterOrEqual(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVSLT()) {
            Expr[] args = expr.getArgs();
            return new BVSignedLess(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVULT()) {
            Expr[] args = expr.getArgs();
            return new BVUnsignedLess(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVSLE()) {
            Expr[] args = expr.getArgs();
            return new BVSignedLessOrEqual(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        } else if (expr.isBVULE()) {
            Expr[] args = expr.getArgs();
            return new BVUnsignedLessOrEqual(convertZ3ToNode(args[0], marshaller), convertZ3ToNode(args[1], marshaller));
        }

        throw new UnsupportedOperationException("failed to convert Z3 formula: " + expr);
    }

    private class NodeTranslatorVisitor implements BottomUpVisitor {

        private Stack<Expr> exprs;

        private List<FuncDecl> decls;
        private VariableMarshaller marshaller;
        private Context ctx;

        NodeTranslatorVisitor(Context ctx, VariableMarshaller marshaller) {
            this.marshaller = marshaller;
            this.exprs = new Stack<>();
            this.decls = new ArrayList<>();
            this.ctx = ctx;
        }

        Expr getExpr() {
            assert exprs.size() == 1;
            return exprs.peek();
        }

        List<FuncDecl> getDecls() {
            return decls;
        }

        private void processVariable(Context ctx, Variable variable) {
            if (TypeInference.typeOf(variable).equals(IntType.TYPE)) {
                exprs.push(ctx.mkIntConst(marshaller.toString(variable)));
                decls.add(ctx.mkConstDecl(marshaller.toString(variable), ctx.getIntSort()));
            } else if (TypeInference.typeOf(variable).equals(BoolType.TYPE)) {
                exprs.push(ctx.mkBoolConst(marshaller.toString(variable)));
                decls.add(ctx.mkConstDecl(marshaller.toString(variable), ctx.getBoolSort()));
            } else if (TypeInference.typeOf(variable) instanceof BVType) {
                int size = ((BVType) TypeInference.typeOf(variable)).getSize();
                exprs.push(ctx.mkBVConst(marshaller.toString(variable), size));
                decls.add(ctx.mkConstDecl(marshaller.toString(variable), ctx.mkBitVecSort(size)));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public void visit(ProgramVariable programVariable) {
            processVariable(ctx, programVariable);
        }

        @Override
        public void visit(Location location) {
            processVariable(ctx, location);
        }

        @Override
        public void visit(UIFApplication UIFApplication) {
            //TODO
            throw new UnsupportedOperationException();
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
            processVariable(ctx, componentInput);
        }

        @Override
        public void visit(ComponentOutput componentOutput) {
            processVariable(ctx, componentOutput);
        }

        @Override
        public void visit(TestInstance testInstance) {
            processVariable(ctx, testInstance);
        }

        @Override
        public void visit(Parameter parameter) {
            processVariable(ctx, parameter);
        }

        @Override
        public void visit(Hole hole) {
            processVariable(ctx, hole);
        }

        @Override
        public void visit(ITE ite) {
            Expr elseBranch = exprs.pop();
            Expr thenBranch = exprs.pop();
            BoolExpr condition = (BoolExpr) exprs.pop();
            exprs.push(ctx.mkITE(condition, thenBranch, elseBranch));
        }

        @Override
        public void visit(Selector selector) {
            processVariable(ctx, selector);
        }

        @Override
        public void visit(BVConst bvConst) {
            exprs.push(ctx.mkBV(bvConst.getLong(), bvConst.getType().getSize()));
        }

        @Override
        public void visit(BVAdd bvAdd) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVAdd(left, right));
        }

        @Override
        public void visit(BVAnd bvAnd) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVAND(left, right));
        }

        @Override
        public void visit(BVMult bvMult) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVMul(left, right));
        }

        @Override
        public void visit(BVNeg bvNeg) {
            BitVecExpr arg = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVNeg(arg));
        }

        @Override
        public void visit(BVNot bvNot) {
            BitVecExpr arg = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVNot(arg));
        }

        @Override
        public void visit(BVOr bvOr) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVOR(left, right));
        }

        @Override
        public void visit(BVShiftLeft bvShiftLeft) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVSHL(left, right));
        }

        @Override
        public void visit(BVSignedDiv bvSignedDiv) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVSDiv(left, right));
        }

        @Override
        public void visit(BVSignedGreater bvSignedGreater) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVSGT(left, right));
        }

        @Override
        public void visit(BVSignedGreaterOrEqual bvSignedGreaterOrEqual) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVSGE(left, right));
        }

        @Override
        public void visit(BVSignedLess bvSignedLess) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVSLT(left, right));
        }

        @Override
        public void visit(BVSignedLessOrEqual bvSignedLessOrEqual) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVSLE(left, right));
        }

        @Override
        public void visit(BVSignedModulo bvSignedModulo) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVSMod(left, right));
        }

        @Override
        public void visit(BVSignedRemainder bvSignedRemainder) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVSRem(left, right));
        }

        @Override
        public void visit(BVSignedShiftRight bvSignedShiftRight) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVASHR(left, right));
        }

        @Override
        public void visit(BVSub bvSub) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVSub(left, right));
        }

        @Override
        public void visit(BVUnsignedDiv bvUnsignedDiv) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVUDiv(left, right));
        }

        @Override
        public void visit(BVUnsignedGreater bvUnsignedGreater) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVUGT(left, right));
        }

        @Override
        public void visit(BVUnsignedGreaterOrEqual bvUnsignedGreaterOrEqual) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVUGE(left, right));
        }

        @Override
        public void visit(BVUnsignedLess bvUnsignedLess) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVULT(left, right));
        }

        @Override
        public void visit(BVUnsignedLessOrEqual bvUnsignedLessOrEqual) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVULE(left, right));
        }

        @Override
        public void visit(BVUnsignedRemainder bvUnsignedRemainder) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVURem(left, right));
        }

        @Override
        public void visit(BVUnsignedShiftRight bvUnsignedShiftRight) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVLSHR(left, right));
        }

        @Override
        public void visit(BranchOutput branchOutput) {
            processVariable(ctx, branchOutput);
        }

        @Override
        public void visit(BVNand bvNand) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVNAND(left, right));
        }

        @Override
        public void visit(BVXor bvXor) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVXOR(left, right));
        }

        @Override
        public void visit(BVNor bvNor) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVNOR(left, right));
        }

        @Override
        public void visit(BVXnor bvXnor) {
            BitVecExpr right = (BitVecExpr) exprs.pop();
            BitVecExpr left = (BitVecExpr) exprs.pop();
            exprs.push(ctx.mkBVXNOR(left, right));
        }

        @Override
        public void visit(ProgramOutput programOutput) {
            processVariable(ctx, programOutput);
        }

        @Override
        public void visit(Dummy dummy) {
            processVariable(ctx, dummy);
        }

        @Override
        public void visit(Indexed indexed) {
            processVariable(ctx, indexed);
        }

    }

}
