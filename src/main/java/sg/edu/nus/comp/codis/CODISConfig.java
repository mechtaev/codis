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
    protected Optional<Integer> totalBound = Optional.empty();
    protected boolean conflictLearning = true;
    protected boolean testBacktracking = true;
    protected boolean transformationBacktracking = true;
    protected boolean conciseInterpolants = true;
    protected boolean checkSubstitutionExists = false;
    protected boolean invertedLearning = false;


    public CODISConfig setTotalBound(int bound) {
        this.totalBound = Optional.of(bound);
        return this;
    }

    public CODISConfig checkSubstitutionExists() {
        this.checkSubstitutionExists = false;
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
