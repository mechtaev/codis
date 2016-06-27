package sg.edu.nus.comp.codis;

import sg.edu.nus.comp.codis.ast.Program;
import sg.edu.nus.comp.codis.ast.ProgramOutput;
import sg.edu.nus.comp.codis.ast.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Sergey Mechtaev on 26/6/2016.
 */
public class TBSConfig {

    protected int bound;

    public TBSConfig(int bound) {
        this.bound = bound;
    }

    // default options:
    protected boolean uniqueUsage = true;
    protected boolean conciseInterpolants = false;
    protected boolean invertedLearning = false;
    protected boolean matchLeaves = true;
    protected List<Program> forbidden = new ArrayList<>();
    protected Map<Type, ProgramOutput> outputs = new HashMap<>();

    public TBSConfig setForbidden(List<Program> forbidden) {
        this.forbidden = forbidden;
        return this;
    }

    public TBSConfig setProgramOutput(Type type, ProgramOutput output) {
        this.outputs.put(type, output);
        return this;
    }

    public TBSConfig setBound(int bound) {
        this.bound = bound;
        return this;
    }

    public TBSConfig enableInvertedLearning() {
        this.invertedLearning = true;
        return this;
    }

    public TBSConfig disableLeafMatching() {
        this.matchLeaves = false;
        return this;
    }

    public TBSConfig disableUniqueUsage() {
        this.uniqueUsage = false;
        return this;
    }

    public TBSConfig enableConciseInterpolants() {
        this.conciseInterpolants = true;
        return this;
    }

}
