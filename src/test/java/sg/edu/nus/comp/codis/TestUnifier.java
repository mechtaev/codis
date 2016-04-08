package sg.edu.nus.comp.codis;

import org.junit.Test;
import sg.edu.nus.comp.codis.ast.BoolType;
import sg.edu.nus.comp.codis.ast.Hole;
import sg.edu.nus.comp.codis.ast.IntType;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.theory.Add;
import sg.edu.nus.comp.codis.ast.theory.IntConst;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Sergey Mechtaev on 8/4/2016.
 */
public class TestUnifier {

    @Test
    public void testEmptyUnifier() {
        Node n1 = new Add(IntConst.of(1), IntConst.of(2));
        Node n2 = new Add(IntConst.of(1), IntConst.of(2));
        Optional<Map<Hole, Node>> unifier = Unifier.unify(n1, n2);
        assertTrue(unifier.isPresent());
        assertTrue(unifier.get().isEmpty());
    }

    @Test
    public void testPrimitiveUnifier() {
        Node n1 = new Hole("x", IntType.TYPE, Node.class);
        Node n2 = new Add(IntConst.of(1), IntConst.of(2));
        Optional<Map<Hole, Node>> unifier = Unifier.unify(n1, n2);
        assertTrue(unifier.isPresent());
        assertEquals(n2, unifier.get().get(n1));
    }

    @Test
    public void testNumberMatcher() {
        Hole h = new Hole("i", IntType.TYPE, IntConst.class);
        Node n1 = new Add(h, IntConst.of(2));
        Node n2 = new Add(IntConst.of(1), IntConst.of(2));
        Optional<Map<Hole, Node>> unifier = Unifier.unify(n1, n2);
        assertTrue(unifier.isPresent());
        assertEquals(IntConst.of(1), unifier.get().get(h));
    }

    @Test
    public void testEquality() {
        Hole h = new Hole("i", IntType.TYPE, IntConst.class);
        Node n1 = new Add(h, h);
        Node n2 = new Add(IntConst.of(1), IntConst.of(1));
        Optional<Map<Hole, Node>> unifier = Unifier.unify(n1, n2);
        assertTrue(unifier.isPresent());
        assertEquals(IntConst.of(1), unifier.get().get(h));
    }

    @Test
    public void testInequality() {
        Hole h = new Hole("i", IntType.TYPE, IntConst.class);
        Node n1 = new Add(h, h);
        Node n2 = new Add(IntConst.of(1), IntConst.of(2));
        Optional<Map<Hole, Node>> unifier = Unifier.unify(n1, n2);
        assertTrue(!unifier.isPresent());
    }

}
