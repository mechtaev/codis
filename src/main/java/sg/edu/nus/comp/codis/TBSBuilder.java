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
public class TBSBuilder {

    private TreeBoundedSynthesis instance;

    public TBSBuilder(InterpolatingSolver solver, int bound) {
        this.instance = new TreeBoundedSynthesis();
        this.instance.solver = solver;
        this.instance.bound = bound;
    }


    public TBSBuilder setForbidden(List<Program> forbidden) {
        this.instance.forbidden = forbidden;
        return this;
    }

    public TBSBuilder setProgramOutput(Type type, ProgramOutput output) {
        this.instance.outputs.put(type, output);
        return this;
    }

    public TBSBuilder setBound(int bound) {
        this.instance.bound = bound;
        return this;
    }

    public TBSBuilder enableInvertedLearning() {
        this.instance.invertedLearning = true;
        return this;
    }

    public TBSBuilder disableLeafMatching() {
        this.instance.matchLeaves = false;
        return this;
    }

    public TBSBuilder disableUniqueUsage() {
        this.instance.uniqueUsage = false;
        return this;
    }

    public TBSBuilder enableConciseInterpolants() {
        this.instance.conciseInterpolants = true;
        return this;
    }

    public TreeBoundedSynthesis build() {
        return this.instance;
    }

}
