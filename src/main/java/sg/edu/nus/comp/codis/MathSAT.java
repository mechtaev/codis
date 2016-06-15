package sg.edu.nus.comp.codis;

import fj.data.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Alberto Griggio on 28/4/2016.
 */
public class MathSAT implements Solver {

    private static final MathSAT INSTANCE = new MathSAT();

    private Logger logger = LoggerFactory.getLogger(MathSAT.class);

    private long config;
    private long solver;

    private MathSAT() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already instantiated");
        }
        HashMap<String, String> cfg = new HashMap<>();
        cfg.put("model", "true");
        this.config = mathsat.api.msat_create_config();
        mathsat.api.msat_set_option(this.config, "model_generation", "true");
        mathsat.api.msat_set_option(this.config, "interpolation", "true");
        mathsat.api.msat_set_option(this.config, "debug.api_call_trace", "1");
        mathsat.api.msat_set_option(this.config, "debug.api_call_trace_filename", "trace.smt2");
        logger.info("Working Directory = " + System.getProperty("user.dir"));
        this.solver = mathsat.api.msat_create_env(this.config);
    }

    public static MathSAT getInstance() {
        return INSTANCE;
    }

    public void dispose() {
        mathsat.api.msat_destroy_env(this.solver);
        mathsat.api.msat_destroy_config(this.config);
    }

    private RuntimeException msatError() {
        return new RuntimeException("MathSAT ERROR: " + mathsat.api.msat_last_error_message(solver));
    }

    @Override
    public Either<Map<Variable, Constant>, List<Node>> getModelOrCore(List<Node> clauses,
                                                                      List<Node> assumptions) {

        mathsat.api.msat_reset_env(solver);
        VariableMarshaller marshaller = new VariableMarshaller();
        List<Long> decls = new ArrayList<>();
        for (Node clause : clauses) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(marshaller);
            clause.accept(visitor);
            mathsat.api.msat_assert_formula(solver, visitor.getExpr());
            decls.addAll(visitor.getDecls());
        }
        ArrayList<Long> assumptionExprs = new ArrayList<>();
        for (Node assumption : assumptions) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(marshaller);
            assumption.accept(visitor);
            assumptionExprs.add(visitor.getExpr());
            decls.addAll(visitor.getDecls());
        }

        long[] assumptionArray = new long[assumptionExprs.size()];
        int idx = 0;
        for (long a : assumptionExprs) {
            assumptionArray[idx++] = a;
        }

        int status = mathsat.api.msat_solve_with_assumptions(solver, assumptionArray);
        if (status == mathsat.api.MSAT_SAT) {
            long model = mathsat.api.msat_get_model(solver);
            if (mathsat.api.MSAT_ERROR_MODEL(model)) {
                throw msatError();
            }
            try {
                return Either.left(getAssignment(model, marshaller));
            } finally {
                mathsat.api.msat_destroy_model(model);
            }
        } else if (status == mathsat.api.MSAT_UNKNOWN) {
            throw msatError();
        } else {
            ArrayList<Node> unsatCore = new ArrayList<>();
            long[] unsatCoreArray = mathsat.api.msat_get_unsat_assumptions(solver);
            if (unsatCoreArray == null) {
                throw msatError();
            }
            ArrayList<Long> unsatCoreExprs = new ArrayList<>();
            unsatCoreExprs.ensureCapacity(unsatCoreArray.length);
            for (int i = 0; i < unsatCoreArray.length; ++i) {
                unsatCoreExprs.add(unsatCoreArray[i]);
            }
            for (int i=0; i<assumptionExprs.size(); i++) {
                if (unsatCoreExprs.contains(assumptionExprs.get(i))) {
                    unsatCore.add(assumptions.get(i));
                }
            }
            return Either.right(unsatCore);
        }
    }

    private long getIntVarDecl(String name) {
        return mathsat.api.msat_declare_function(solver, name, mathsat.api.msat_get_integer_type(solver));
    }
    private long getIntVar(String name) {
        long d = getIntVarDecl(name);
        return mathsat.api.msat_make_constant(solver, d);
    }

    private long getBoolVarDecl(String name) {
        return mathsat.api.msat_declare_function(solver, name, mathsat.api.msat_get_bool_type(solver));
    }

    private long getBoolVar(String name) {
        long d = getBoolVarDecl(name);
        return mathsat.api.msat_make_constant(solver, d);
    }

    private Map<Variable, Constant> getAssignment(long model, VariableMarshaller marshaller) {
        HashMap<Variable, Constant> assingment = new HashMap<>();
        for (Variable variable: marshaller.getVariables()) {
            if (TypeInference.typeOf(variable).equals(IntType.TYPE)) {
                long result = mathsat.api.msat_model_eval(model, getIntVar(marshaller.toString(variable)));
                if (mathsat.api.MSAT_ERROR_TERM(result)) {
                    throw msatError();
                }
                if (mathsat.api.msat_term_is_number(solver, result) != 0) {
                    int value = Integer.parseInt(mathsat.api.msat_term_repr(result));
                    assingment.put(variable, IntConst.of(value));
                } else {
                    throw new RuntimeException("unsupported MathSAT expression type");
                }
            } else if (TypeInference.typeOf(variable).equals(BoolType.TYPE)) {
                long result = mathsat.api.msat_model_eval(model, getBoolVar(marshaller.toString(variable)));
                if (mathsat.api.MSAT_ERROR_TERM(result)) {
                    throw msatError();
                }
                if (mathsat.api.msat_term_is_true(solver, result) != 0) {
                    assingment.put(variable, BoolConst.of(true));
                } else if (mathsat.api.msat_term_is_false(solver, result) != 0) {
                    assingment.put(variable, BoolConst.of(false));
                } else {
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
        mathsat.api.msat_reset_env(solver);
        VariableMarshaller marshaller = new VariableMarshaller();
        List<Long> decls = new ArrayList<>();
        for (Node clause : clauses) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(marshaller);
            clause.accept(visitor);
            mathsat.api.msat_assert_formula(solver, visitor.getExpr());
            decls.addAll(visitor.getDecls());
        }

        if (logger.isTraceEnabled()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
            Date now = new Date();
            Path logsmt = Paths.get("cbs" + sdf.format(now) + ".smt2");
            try {
                List<String> entries = new ArrayList<>();
                entries.add("(set-option :produce-models true)");
                Set<Long> seenDecls = new HashSet<>();
                for (long d : decls) {
                    if (seenDecls.add(d)) {
                        String n = mathsat.api.msat_decl_get_name(d);
                        String tp = "Bool";
                        if (mathsat.api.msat_is_integer_type(solver, mathsat.api.msat_decl_get_return_type(d)) != 0) {
                            tp = "Int";
                        }
                        entries.add("(declare-fun |" + n.replace("|", "\\|") + "| () " + tp + ")");
                    }
                }
                long[] assertions = mathsat.api.msat_get_asserted_formulas(solver);
                for (int i = 0; i < assertions.length; ++i) {
                    String c = mathsat.api.msat_to_smtlib2_term(solver, assertions[i]);
                    entries.add("(assert " + c + ")");
                }
                entries.add("(check-sat)");
                entries.add("(get-model)");
                Files.write(logsmt, entries, Charset.forName("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int status = mathsat.api.msat_solve(solver);
        if (status == mathsat.api.MSAT_SAT) {
            long model = mathsat.api.msat_get_model(solver);
            if (mathsat.api.MSAT_ERROR_MODEL(model)) {
                throw msatError();
            }
            try {
                return Optional.of(getAssignment(model, marshaller));
            } finally {
                mathsat.api.msat_destroy_model(model);
            }
        } else if (status == mathsat.api.MSAT_UNKNOWN) {
            throw msatError();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Either<Map<Variable, Constant>, Node> getModelOrInterpolant(List<Node> leftClauses, List<Node> rightClauses) {
        mathsat.api.msat_reset_env(solver);

        VariableMarshaller marshaller = new VariableMarshaller();
        //List<FuncDecl> decls = new ArrayList<>();

        int groupA = mathsat.api.msat_create_itp_group(solver);
        int groupB = mathsat.api.msat_create_itp_group(solver);

        mathsat.api.msat_set_itp_group(solver, groupA);
        for (Node leftClause : leftClauses) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(marshaller);
            leftClause.accept(visitor);
            //decls.addAll(visitor.getDecls());
            mathsat.api.msat_assert_formula(solver, visitor.getExpr());
        }

        mathsat.api.msat_set_itp_group(solver, groupB);
        for (Node rightClause : rightClauses) {
            NodeTranslatorVisitor visitor = new NodeTranslatorVisitor(marshaller);
            rightClause.accept(visitor);
            //decls.addAll(visitor.getDecls());
            mathsat.api.msat_assert_formula(solver, visitor.getExpr());
        }

        int status = mathsat.api.msat_solve(solver);
        logger.info("solved");
        if (status == mathsat.api.MSAT_SAT) {
            long model = mathsat.api.msat_get_model(solver);
            if (mathsat.api.MSAT_ERROR_MODEL(model)) {
                throw msatError();
            }
            try {
                return Either.left(getAssignment(model, marshaller));
            } finally {
                mathsat.api.msat_destroy_model(model);
            }
        } else if (status == mathsat.api.MSAT_UNKNOWN) {
            throw msatError();
        } else {
            int[] groupsOfA = {groupA};
            long interpolant = mathsat.api.msat_get_interpolant(solver, groupsOfA, 1);
            assert(!mathsat.api.MSAT_ERROR_TERM(interpolant));
            String s = mathsat.api.msat_to_smtlib2_term(solver, interpolant);
            System.out.println("\nOK, the interpolant is: " + s);

            //TODO convert to Node
            throw new UnsupportedOperationException();
        }

    }

    private class NodeTranslatorVisitor implements BottomUpVisitor {

        private Stack<Long> exprs;

        private List<Long> decls;
        private VariableMarshaller marshaller;

        NodeTranslatorVisitor(VariableMarshaller marshaller) {
            this.marshaller = marshaller;
            this.exprs = new Stack<>();
            this.decls = new ArrayList<>();
        }

        long getExpr() {
            assert exprs.size() == 1;
            return exprs.peek();
        }

        List<Long> getDecls() {
            return decls;
        }

        private void processVariable(Variable variable) {
            if (TypeInference.typeOf(variable).equals(IntType.TYPE)) {
                pushExpr(getIntVar(marshaller.toString(variable)));
                decls.add(getIntVarDecl(marshaller.toString(variable)));
            } else if (TypeInference.typeOf(variable).equals(BoolType.TYPE)) {
                pushExpr(getBoolVar(marshaller.toString(variable)));
                decls.add(getBoolVarDecl(marshaller.toString(variable)));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        private void pushExpr(long e) {
            if (mathsat.api.MSAT_ERROR_TERM(e)) {
                throw msatError();
            }
            exprs.push(e);
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
            long right = exprs.pop();
            long left = exprs.pop();
            if (mathsat.api.msat_is_bool_type(solver, mathsat.api.msat_term_get_type(left)) != 0) {
                pushExpr(mathsat.api.msat_make_iff(solver, left, right));
            } else {
                pushExpr(mathsat.api.msat_make_equal(solver, left, right));
            }
        }

        @Override
        public void visit(Add add) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_plus(solver, left, right));
        }

        @Override
        public void visit(Sub sub) {
            long right = exprs.pop();
            long left = exprs.pop();
            long neg = mathsat.api.msat_make_number(solver, "-1");
            pushExpr(mathsat.api.msat_make_plus(solver, left, mathsat.api.msat_make_times(solver, neg, right)));
        }

        @Override
        public void visit(Mult mult) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_times(solver, left, right));
        }

        @Override
        public void visit(Div div) {
            //TODO
            throw new NotImplementedException();
        }

        @Override
        public void visit(And and) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_and(solver, left, right));
        }

        @Override
        public void visit(Or or) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_or(solver, left, right));
        }

        @Override
        public void visit(Iff iff) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_iff(solver, left, right));
        }

        @Override
        public void visit(Impl impl) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_or(solver, mathsat.api.msat_make_not(solver, left), right));
        }

        @Override
        public void visit(Greater greater) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_not(solver, mathsat.api.msat_make_leq(solver, left, right)));
        }

        @Override
        public void visit(Less less) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_not(solver, mathsat.api.msat_make_leq(solver, right, left)));
        }

        @Override
        public void visit(GreaterOrEqual greaterOrEqual) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_leq(solver, right, left));
        }

        @Override
        public void visit(LessOrEqual lessOrEqual) {
            long right = exprs.pop();
            long left = exprs.pop();
            pushExpr(mathsat.api.msat_make_leq(solver, left, right));
        }

        @Override
        public void visit(Minus minus) {
            long neg = mathsat.api.msat_make_number(solver, "-1");
            pushExpr(mathsat.api.msat_make_times(solver, neg, exprs.pop()));
        }

        @Override
        public void visit(Not not) {
            pushExpr(mathsat.api.msat_make_not(solver, exprs.pop()));
        }

        @Override
        public void visit(IntConst intConst) {
            pushExpr(mathsat.api.msat_make_number(solver, Integer.toString(intConst.getValue())));
        }

        @Override
        public void visit(BoolConst boolConst) {
            if (boolConst.getValue()) {
                pushExpr(mathsat.api.msat_make_true(solver));
            } else {
                pushExpr(mathsat.api.msat_make_false(solver));
            }
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

        @Override
        public void visit(ITE ite) {
            long elseBranch = exprs.pop();
            long thenBranch = exprs.pop();
            long condition = exprs.pop();
            pushExpr(mathsat.api.msat_make_term_ite(solver, condition, thenBranch, elseBranch));
        }

        @Override
        public void visit(Selector selector) {
            processVariable(selector);
        }

        @Override
        public void visit(BVConst bvConst) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVAdd bvAdd) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVAnd bvAnd) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVMult bvMult) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVNeg bvNeg) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVNot bvNot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVOr bvOr) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVShiftLeft bvShiftLeft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVSignedDiv bvSignedDiv) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVSignedGreater bvSignedGreater) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVSignedGreaterOrEqual bvSignedGreaterOrEqual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVSignedLess bvSignedLess) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVSignedLessOrEqual bvSignedLessOrEqual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVSignedModulo bvSignedModulo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVSignedRemainder bvSignedRemainder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVSignedShiftRight bvSignedShiftRight) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVSub bvSub) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVUnsignedDiv bvUnsignedDiv) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVUnsignedGreater bvUnsignedGreater) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVUnsignedGreaterOrEqual bvUnsignedGreaterOrEqual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVUnsignedLess bvUnsignedLess) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVUnsignedLessOrEqual bvUnsignedLessOrEqual) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVUnsignedRemainder bvUnsignedRemainder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVUnsignedShiftRight bvUnsignedShiftRight) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BranchOutput branchOutput) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVNand bvNand) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVXor bvXor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVNor bvNor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BVXnor bvXnor) {
            throw new UnsupportedOperationException();
        }

    }

}
