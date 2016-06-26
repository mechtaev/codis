package sg.edu.nus.comp.codis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.junit.Test;
import sg.edu.nus.comp.codis.ast.BoolType;
import sg.edu.nus.comp.codis.ast.Dummy;
import sg.edu.nus.comp.codis.ast.Node;
import sg.edu.nus.comp.codis.ast.ProgramVariable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by Sergey Mechtaev on 24/6/2016.
 */
public class TestConflictDatabase {

    @Test
    public void testEmptyUnifier() {
        Multiset<Node> components = HashMultiset.create();
        components.add(Components.ADD);
        components.add(ProgramVariable.mkInt("x"), 2);

        ConflictDatabase db = new ConflictDatabase(components);

        Multiset<Node> set1 = HashMultiset.create();
        set1.add(ProgramVariable.mkInt("x"), 1);

        Multiset<Node> set2 = HashMultiset.create();
        set2.add(Components.ADD);
        set2.add(ProgramVariable.mkInt("x"), 1);

        Node c1 = ProgramVariable.mkBool("c1");
        Node c2 = ProgramVariable.mkBool("c2");
        Set<Node> expected = new HashSet<>();
        expected.add(c1);
        expected.add(c2);

        db.insert(set1, c1);
        db.insert(set2, c2);

        List<Node> query = db.query(set1);
        assertEquals(expected, new HashSet<>(query));
    }
}
