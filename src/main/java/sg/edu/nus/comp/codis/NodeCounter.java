package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;
import sg.edu.nus.comp.codis.ast.theory.*;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Created by Sergey Mechtaev on 27/6/2016.
 */
public class NodeCounter {

    public static int count(Node node) {
        NodeCounterVisitor visitor = new NodeCounterVisitor();
        node.accept(visitor);
        return visitor.getCount();
    }

    private static class NodeCounterVisitor implements BottomUpMemoVisitor {

        private Map<Node, Boolean> nodeMemo;

        private int counter;

        NodeCounterVisitor() {
            nodeMemo = new IdentityHashMap<>();
            counter = 0;
        }

        int getCount() {
            return counter;
        }

        private void countAndMemo(Node node) {
            this.nodeMemo.put(node, true);
            this.counter++;
        }

        @Override
        public boolean alreadyVisited(Node node) {
            return this.nodeMemo.containsKey(node);
        }

        @Override
        public void visitAgain(Node node) {
            // do nothing
        }

        @Override
        public void visit(ProgramVariable programVariable) {
            countAndMemo(programVariable);
        }

        @Override
        public void visit(Location location) {
            countAndMemo(location);
        }

        @Override
        public void visit(UIFApplication UIFApplication) {
            countAndMemo(UIFApplication);
        }

        @Override
        public void visit(Equal equal) {
            countAndMemo(equal);
        }

        @Override
        public void visit(Add add) {
            countAndMemo(add);
        }

        @Override
        public void visit(Sub sub) {
            countAndMemo(sub);
        }

        @Override
        public void visit(Mult mult) {
            countAndMemo(mult);
        }

        @Override
        public void visit(Div div) {
            countAndMemo(div);
        }

        @Override
        public void visit(And and) {
            countAndMemo(and);
        }

        @Override
        public void visit(Or or) {
            countAndMemo(or);
        }

        @Override
        public void visit(Iff iff) {
            countAndMemo(iff);
        }

        @Override
        public void visit(Impl impl) {
            countAndMemo(impl);
        }

        @Override
        public void visit(Greater greater) {
            countAndMemo(greater);
        }

        @Override
        public void visit(Less less) {
            countAndMemo(less);
        }

        @Override
        public void visit(GreaterOrEqual greaterOrEqual) {
            countAndMemo(greaterOrEqual);
        }

        @Override
        public void visit(LessOrEqual lessOrEqual) {
            countAndMemo(lessOrEqual);
        }

        @Override
        public void visit(Minus minus) {
            countAndMemo(minus);
        }

        @Override
        public void visit(Not not) {
            countAndMemo(not);
        }

        @Override
        public void visit(IntConst intConst) {
            countAndMemo(intConst);
        }

        @Override
        public void visit(BoolConst boolConst) {
            countAndMemo(boolConst);
        }

        @Override
        public void visit(ComponentInput componentInput) {
            countAndMemo(componentInput);
        }

        @Override
        public void visit(ComponentOutput componentOutput) {
            countAndMemo(componentOutput);
        }

        @Override
        public void visit(TestInstance testInstance) {
            countAndMemo(testInstance);
        }

        @Override
        public void visit(Parameter parameter) {
            countAndMemo(parameter);
        }

        @Override
        public void visit(Hole hole) {
            countAndMemo(hole);
        }

        @Override
        public void visit(BranchOutput branchOutput) {
            countAndMemo(branchOutput);
        }

        @Override
        public void visit(ITE ite) {
            countAndMemo(ite);
        }

        @Override
        public void visit(Selector selector) {
            countAndMemo(selector);
        }

        @Override
        public void visit(BVConst bvConst) {
            countAndMemo(bvConst);
        }

        @Override
        public void visit(BVAdd bvAdd) {
            countAndMemo(bvAdd);
        }

        @Override
        public void visit(BVAnd bvAnd) {
            countAndMemo(bvAnd);
        }

        @Override
        public void visit(BVMult bvMult) {
            countAndMemo(bvMult);
        }

        @Override
        public void visit(BVNeg bvNeg) {
            countAndMemo(bvNeg);
        }

        @Override
        public void visit(BVNot bvNot) {
            countAndMemo(bvNot);
        }

        @Override
        public void visit(BVOr bvOr) {
            countAndMemo(bvOr);
        }

        @Override
        public void visit(BVShiftLeft bvShiftLeft) {
            countAndMemo(bvShiftLeft);
        }

        @Override
        public void visit(BVSignedDiv bvSignedDiv) {
            countAndMemo(bvSignedDiv);
        }

        @Override
        public void visit(BVSignedGreater bvSignedGreater) {
            countAndMemo(bvSignedGreater);
        }

        @Override
        public void visit(BVSignedGreaterOrEqual bvSignedGreaterOrEqual) {
            countAndMemo(bvSignedGreaterOrEqual);
        }

        @Override
        public void visit(BVSignedLess bvSignedLess) {
            countAndMemo(bvSignedLess);
        }

        @Override
        public void visit(BVSignedLessOrEqual bvSignedLessOrEqual) {
            countAndMemo(bvSignedLessOrEqual);
        }

        @Override
        public void visit(BVSignedModulo bvSignedModulo) {
            countAndMemo(bvSignedModulo);
        }

        @Override
        public void visit(BVSignedRemainder bvSignedRemainder) {
            countAndMemo(bvSignedRemainder);
        }

        @Override
        public void visit(BVSignedShiftRight bvSignedShiftRight) {
            countAndMemo(bvSignedShiftRight);
        }

        @Override
        public void visit(BVSub bvSub) {
            countAndMemo(bvSub);
        }

        @Override
        public void visit(BVUnsignedDiv bvUnsignedDiv) {
            countAndMemo(bvUnsignedDiv);
        }

        @Override
        public void visit(BVUnsignedGreater bvUnsignedGreater) {
            countAndMemo(bvUnsignedGreater);
        }

        @Override
        public void visit(BVUnsignedGreaterOrEqual bvUnsignedGreaterOrEqual) {
            countAndMemo(bvUnsignedGreaterOrEqual);
        }

        @Override
        public void visit(BVUnsignedLess bvUnsignedLess) {
            countAndMemo(bvUnsignedLess);
        }

        @Override
        public void visit(BVUnsignedLessOrEqual bvUnsignedLessOrEqual) {
            countAndMemo(bvUnsignedLessOrEqual);
        }

        @Override
        public void visit(BVUnsignedRemainder bvUnsignedRemainder) {
            countAndMemo(bvUnsignedRemainder);
        }

        @Override
        public void visit(BVUnsignedShiftRight bvUnsignedShiftRight) {
            countAndMemo(bvUnsignedShiftRight);
        }

        @Override
        public void visit(BVNand bvNand) {
            countAndMemo(bvNand);
        }

        @Override
        public void visit(BVXor bvXor) {
            countAndMemo(bvXor);
        }

        @Override
        public void visit(BVNor bvNor) {
            countAndMemo(bvNor);
        }

        @Override
        public void visit(BVXnor bvXnor) {
            countAndMemo(bvXnor);
        }

        @Override
        public void visit(ProgramOutput programOutput) {
            countAndMemo(programOutput);
        }

        @Override
        public void visit(Dummy dummy) {
            countAndMemo(dummy);
        }

        @Override
        public void visit(Indexed indexed) {
            countAndMemo(indexed);
        }

    }
}
