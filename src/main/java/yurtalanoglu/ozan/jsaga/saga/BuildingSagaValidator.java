package yurtalanoglu.ozan.jsaga.saga;

public class BuildingSagaValidator {

    protected void validateWhenAddingStartingStep (BuildingSagaStatus buildingSagaStatus) throws IllegalStateException {
        if(buildingSagaStatus.isStartingStepAdded()){
            throw new IllegalStateException();
        }
    }

    protected void validateWhenBuildingStartingStep (BuildingSagaStatus buildingSagaStatus) throws IllegalStateException {
        if(! buildingSagaStatus.isStartingStepAdded()
        || buildingSagaStatus.hasAnyDispatcherStep()){
            throw new IllegalStateException();
        }
    }

    protected void validateWhenAddingDispatcherStep (BuildingSagaStatus buildingSagaStatus) throws IllegalStateException {
        //There is no way to add dispatcher step before adding a starting step
        //So we do not need to check whether any dispatcher step added as additional 'OR' condition
        if(! buildingSagaStatus.isStartingStepCompleted()
                || (buildingSagaStatus.hasAnyDispatcherStep() && !buildingSagaStatus.isLastDispatcherStepCompleted())){
            throw new IllegalStateException();
        }
    }

    protected void validateWhenBuildingDispatcherStep (BuildingSagaStatus buildingSagaStatus) throws IllegalStateException {
        if(! buildingSagaStatus.hasAnyDispatcherStep()){
            throw new IllegalStateException();
        }
    }

    protected void validateWhenFinallyBuildingSaga (BuildingSagaStatus buildingSagaStatus) throws IllegalStateException {
        if(! buildingSagaStatus.getSagaCanBeBuilt()){
            throw new IllegalStateException();
        }
    }





}
