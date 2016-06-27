package sg.edu.nus.comp.codis;

import java.util.Optional;

/**
 * Created by Sergey Mechtaev on 26/6/2016.
 */
public class CODISConfig {

    protected int incrementBound;

    public CODISConfig(int incrementBound) {
        this.incrementBound = incrementBound;
    }

    // default options:
    protected Optional<Integer> totalBound = Optional.empty(); //FIXME: this should be actually "skip expansion from depth"
    protected boolean conflictLearning = true;
    protected boolean conciseInterpolants = true;
    protected boolean checkExpansionSatisfiability = false;
    protected boolean invertedLearning = false;
    protected Optional<Integer> iterationsBeforeRestart = Optional.empty();
    protected Optional<Integer> maximumLeafExpansions = Optional.empty();
    protected int maximumInterpolantSize = 100;
    protected int maximumConflictsCheck = 20;

    // Unsupported options:
    protected boolean testBacktracking = true;
    protected boolean transformationBacktracking = true;
    protected int incrementalImprovement = 1;


    public CODISConfig setTotalBound(int bound) {
        this.totalBound = Optional.of(bound);
        return this;
    }

    public CODISConfig setMaximumConflictsCheck(int bound) {
        this.maximumConflictsCheck = bound;
        return this;
    }

    public CODISConfig setMaximumLeafExpansions(int bound) {
        this.maximumLeafExpansions = Optional.of(bound);
        return this;
    }

    public CODISConfig setMaximumInterpolantSize(int size) {
        this.maximumInterpolantSize = size;
        return this;
    }

    public CODISConfig setIterationsBeforeRestart(int iterations) {
        this.iterationsBeforeRestart = Optional.of(iterations);
        return this;
    }

    public CODISConfig setIncrementalImprovement(int improvement) {
        this.incrementalImprovement = improvement;
        return this;
    }

    public CODISConfig checkExpansionSatisfiability() {
        this.checkExpansionSatisfiability = true;
        return this;
    }

    public CODISConfig disableConflictLearning() {
        this.conflictLearning = false;
        return this;
    }

    public CODISConfig disableBacktrackingOnTests() {
        this.testBacktracking = false;
        return this;
    }

    public CODISConfig disableBacktrackingOnTransformations() {
        this.transformationBacktracking = false;
        return this;
    }

    public CODISConfig disableConciseInterpolants() {
        this.conciseInterpolants = false;
        return this;
    }

    public CODISConfig enableInvertedLearning() {
        this.invertedLearning = true;
        return this;
    }


}
