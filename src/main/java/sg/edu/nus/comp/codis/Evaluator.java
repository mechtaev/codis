package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.*;

import java.util.Map;

/**
 * Created by Sergey Mechtaev on 7/4/2016.
 */
public class Evaluator {

    public static Node eval(Node node, Map<ProgramVariable, ? extends Node> assignment) {
        return Simplifier.simplify(Traverse.substitute(node, assignment));
    }

}
