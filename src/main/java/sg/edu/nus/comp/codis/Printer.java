package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.*;

/**
 * Created by Sergey Mechtaev on 26/6/2016.
 */
public class Printer {

    // printing with memoization, but currently uses slow string concatenation
    public static String print(Node node) {
        NodePrinterVisitor visitor = new NodePrinterVisitor();
        node.accept(visitor);
        return visitor.getRepr();
    }

    private static class NodePrinterVisitor implements BottomUpMemoVisitor {

        private Map<Node, String> nodeToMathsatMemo;

        private Stack<String> exprs;

        NodePrinterVisitor() {
            this.exprs = new Stack<>();
            nodeToMathsatMemo = new IdentityHashMap<>();
        }

        String getRepr() {
            assert exprs.size() == 1;
            return exprs.peek();
        }

        private void processVariable(Variable variable) {
            pushAndMemoExpr(variable.toString(), variable);
        }

        private void pushAndMemoExpr(String repr, Node node) {
            this.nodeToMathsatMemo.put(node, repr);
            exprs.push(repr);
        }

        @Override
        public boolean alreadyVisited(Node node) {
            return this.nodeToMathsatMemo.containsKey(node);
        }

        @Override
        public void visitAgain(Node node) {
            exprs.push(this.nodeToMathsatMemo.get(node));
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
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(Equal equal) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " = " + right + ")", equal);
        }

        @Override
        public void visit(Add add) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " + " + right + ")", add);
        }

        @Override
        public void visit(Sub sub) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " - " + right + ")", sub);
        }

        @Override
        public void visit(Mult mult) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " * " + right + ")", mult);
        }

        @Override
        public void visit(Div div) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " / " + right + ")", div);
        }

        @Override
        public void visit(And and) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " & " + right + ")", and);
        }

        @Override
        public void visit(Or or) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " | " + right + ")", or);
        }

        @Override
        public void visit(Iff iff) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " <=> " + right + ")", iff);
        }

        @Override
        public void visit(Impl impl) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " => " + right + ")", impl);
        }

        @Override
        public void visit(Greater greater) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " > " + right + ")", greater);
        }

        @Override
        public void visit(Less less) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " < " + right + ")", less);
        }

        @Override
        public void visit(GreaterOrEqual greaterOrEqual) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " >= " + right + ")", greaterOrEqual);
        }

        @Override
        public void visit(LessOrEqual lessOrEqual) {
            String right = exprs.pop();
            String left = exprs.pop();
            pushAndMemoExpr("(" + left + " <= " + right + ")", lessOrEqual);
        }

        @Override
        public void visit(Minus minus) {
            String arg = exprs.pop();
            pushAndMemoExpr("-" + arg, minus);
        }

        @Override
        public void visit(Not not) {
            String arg = exprs.pop();
            pushAndMemoExpr("!" + arg, not);
        }

        @Override
        public void visit(IntConst intConst) {
            pushAndMemoExpr(Integer.toString(intConst.getValue()), intConst);
        }

        @Override
        public void visit(BoolConst boolConst) {
            String repr;
            if (boolConst.getValue()) {
                repr = "true";
            } else {
                repr = "false";
            }
            pushAndMemoExpr(repr, boolConst);
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
        public void visit(BranchOutput branchOutput) {
            processVariable(branchOutput);
        }

        @Override
        public void visit(ITE ite) {
            String elseBranch = exprs.pop();
            String thenBranch = exprs.pop();
            String condition = exprs.pop();
            pushAndMemoExpr("(if " + condition + " " + thenBranch + " " + elseBranch + ")", ite);
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

        @Override
        public void visit(ProgramOutput programOutput) {
            processVariable(programOutput);
        }

        @Override
        public void visit(Dummy dummy) {
            processVariable(dummy);
        }

        @Override
        public void visit(Indexed indexed) {
            processVariable(indexed);
        }

    }

}
