package sg.edu.nus.comp.codis;

import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 20/9/2016.
 */
public class CODISBuilder {

    private CODIS instance;

    public CODISBuilder(Solver solver, InterpolatingSolver iSolver, Tester tester, int incrementBound) {
        this.instance = new CODIS();
        this.instance.solver = solver;
        this.instance.tester = tester;
        this.instance.iSolver = iSolver;
        this.instance.incrementBound = incrementBound;
    }

    public CODISBuilder setTotalBound(int bound) {
        this.instance.totalBound = Optional.of(bound);
        return this;
    }

    public CODISBuilder setMaximumConflictsCheck(int bound) {
        this.instance.maximumConflictsCheck = bound;
        return this;
    }

    public CODISBuilder setMaximumLeafExpansions(int bound) {
        this.instance.maximumLeafExpansions = Optional.of(bound);
        return this;
    }

    public CODISBuilder setMaximumInterpolantSize(int size) {
        this.instance.maximumInterpolantSize = size;
        return this;
    }

    public CODISBuilder setIterationsBeforeRestart(int iterations) {
        this.instance.iterationsBeforeRestart = Optional.of(iterations);
        return this;
    }

    public CODISBuilder setIncrementalImprovement(int improvement) {
        this.instance.incrementalImprovement = improvement;
        return this;
    }

    public CODISBuilder checkExpansionSatisfiability() {
        this.instance.checkExpansionSatisfiability = true;
        return this;
    }

    public CODISBuilder forbidStructure() {
        this.instance.forbidStructure = true;
        return this;
    }

    public CODISBuilder disableConflictLearning() {
        this.instance.conflictLearning = false;
        return this;
    }

    public CODISBuilder disableBacktrackingOnTests() {
        this.instance.testBacktracking = false;
        return this;
    }

    public CODISBuilder disableBacktrackingOnTransformations() {
        this.instance.transformationBacktracking = false;
        return this;
    }

    public CODISBuilder disableConciseInterpolants() {
        this.instance.conciseInterpolants = false;
        return this;
    }

    public CODISBuilder enableInvertedLearning() {
        this.instance.invertedLearning = true;
        return this;
    }

    public CODISBuilder enableDebugMode() {
        this.instance.debugMode = true;
        return this;
    }

    public CODIS build() {
        return this.instance;
    }

}
