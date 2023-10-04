package yurtalanoglu.ozan.jsaga.saga;

public class BuildingSagaStatusFactory {

    protected BuildingSagaStatus createWithSaga(Saga saga){
        BuildingSagaStatus buildingSagaStatus = new BuildingSagaStatus();
        setStartingStepRelatedStatus(buildingSagaStatus, saga);
        setDispatcherStepRelatedStatus(buildingSagaStatus,saga);
        setFinallyBuildingSagaRelatedStatus(buildingSagaStatus,saga);
        return buildingSagaStatus;
    }


    protected void setStartingStepRelatedStatus(BuildingSagaStatus buildingSagaStatus, Saga saga){
        buildingSagaStatus.setStartingStepAdded(saga.getStartingSagaStep() == null ? false : true);
        buildingSagaStatus.setStartingStepCompleted(saga.getStartingSagaStep() == null || saga.getLocalAction().isEmpty() || saga.getLocalActionInput() == null
                || saga.getLocalCompensationAction().isEmpty() ? false : true);
    }

    protected void setDispatcherStepRelatedStatus(BuildingSagaStatus buildingSagaStatus, Saga saga){
        buildingSagaStatus.setHasAnyDispatcherStep(!saga.isDispatcherStepListEmpty());
        if(saga.getLastAddedDispatcherStep().isPresent()){
            buildingSagaStatus.setLastDispatcherStepCompleted(saga.getLastAddedDispatcherStepMessageGenerator().isEmpty()
                    || saga.getLastAddedDispatcherStepCompensationNeededChecker().isEmpty() ? false : true);
        }
        else{
            buildingSagaStatus.setLastDispatcherStepCompleted(false);
        }
    }

    protected void setFinallyBuildingSagaRelatedStatus(BuildingSagaStatus buildingSagaStatus, Saga saga){
        Boolean sagaCanBeBuilt = buildingSagaStatus.isStartingStepCompleted()
                && (! buildingSagaStatus.hasAnyDispatcherStep() || ! saga.getLastAddedDispatcherStepMessageGenerator().isEmpty());
        buildingSagaStatus.setSagaCanBeBuilt(sagaCanBeBuilt);
    }

    


}
