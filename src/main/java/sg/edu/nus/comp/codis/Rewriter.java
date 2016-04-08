package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 8/4/2016.
 */
public class Rewriter {

    private boolean modified;

    public Node applyRules(Node node, ArrayList<RewriteRule> rules) {
        modified = true;
        while (modified) {
            modified = false;

            node = Traverse.transform(node, n -> {
                for (RewriteRule rule : rules) {
                    Optional<Map<Hole, Node>> unifier = Unifier.unify(rule.getPattern(), n);
                    if (unifier.isPresent()) {
                        modified = true;
                        return rule.apply(n, unifier.get());
                    }
                }
                return n;
            });
        }
        return node;
    }

}
