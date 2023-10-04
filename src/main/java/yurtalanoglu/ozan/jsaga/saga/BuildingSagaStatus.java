package yurtalanoglu.ozan.jsaga.saga;

public class BuildingSagaStatus {


    private Boolean isStartingStepAdded = false;
    private Boolean hasAnyDispatcherStep = false;
    private Boolean isStartingStepCompleted = false;
    private Boolean isLastDispatcherStepCompleted= false;

    private Boolean sagaCanBeBuilt= false;


    public Boolean getSagaCanBeBuilt() {
        return sagaCanBeBuilt;
    }

    public void setSagaCanBeBuilt(Boolean sagaCanBeBuilt) {
        this.sagaCanBeBuilt = sagaCanBeBuilt;
    }


    protected Boolean isStartingStepAdded() {
        return isStartingStepAdded;
    }

    protected void setStartingStepAdded(Boolean startingStepAdded) {
        isStartingStepAdded = startingStepAdded;
    }

    protected Boolean isStartingStepCompleted() {
        return isStartingStepCompleted;
    }

    protected void setStartingStepCompleted(Boolean isStartingStepCompleted) {
        this.isStartingStepCompleted = isStartingStepCompleted;
    }

    protected Boolean hasAnyDispatcherStep() {
        return hasAnyDispatcherStep;
    }

    protected void setHasAnyDispatcherStep(Boolean hasAnyDispatcherStep) {
        this.hasAnyDispatcherStep = hasAnyDispatcherStep;
    }

    protected Boolean isLastDispatcherStepCompleted(){
        return isLastDispatcherStepCompleted;
    }

    protected void setLastDispatcherStepCompleted(Boolean lastDispatcherStepCompleted){
        isLastDispatcherStepCompleted = lastDispatcherStepCompleted;
    }
}
