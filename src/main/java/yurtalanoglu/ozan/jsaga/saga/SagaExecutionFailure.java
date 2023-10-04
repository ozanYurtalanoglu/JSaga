package yurtalanoglu.ozan.jsaga.saga;

import yurtalanoglu.ozan.jsaga.exception.KafkaMessageCouldNotSendException;
import yurtalanoglu.ozan.jsaga.exception.SolvingCompensationFailureException;
import yurtalanoglu.ozan.jsaga.kafka.SagaKafkaMessage;
import yurtalanoglu.ozan.jsaga.kafka.SagaKafkaProducer;
import yurtalanoglu.ozan.jsaga.sagastep.DispatcherSagaStep;
import yurtalanoglu.ozan.jsaga.sagastep.LocalActionDataHolder;

public class SagaExecutionFailure<T> {

    private DispatcherSagaStep<T> failedDispatcherSagaStep;

    private Throwable failure;

    private Boolean isFailureSolved = false;

    private final Boolean isCompensationFailure;

    private final LocalActionDataHolder<T> localActionDataHolderWhenCompensationStepFailed;

    private SagaKafkaProducer sagaKafkaProducer = new SagaKafkaProducer();

    private SagaKafkaMessage onCompensationKafkaMesssage;

    public SagaExecutionFailure(DispatcherSagaStep<T> failedDispatcherSagaStep, Throwable failure, Boolean isCompensationFailure) {
        this.failedDispatcherSagaStep = failedDispatcherSagaStep;
        this.failure = failure;
        this.isCompensationFailure = isCompensationFailure;
        this.localActionDataHolderWhenCompensationStepFailed = failedDispatcherSagaStep.getLocalActionDataHolderOnCompensation();
    }

    public Throwable getFailure() {
        return failure;
    }

    public Boolean isFailureSolved() {
        return isFailureSolved;
    }

    public DispatcherSagaStep<T> getFailedDispatcherSagaStep() {
        return failedDispatcherSagaStep;
    }

    public LocalActionDataHolder<T> getLocalActionDataHolderWhenCompensationStepFailed() {
        return localActionDataHolderWhenCompensationStepFailed;
    }

    public Boolean isCompensationFailure() {
        return isCompensationFailure;
    }

    public SagaKafkaMessage getOnCompensationKafkaMesssage() {
        return onCompensationKafkaMesssage;
    }

    protected void setOnCompensationKafkaMesssage(SagaKafkaMessage onCompensationKafkaMesssage) {
        this.onCompensationKafkaMesssage = onCompensationKafkaMesssage;
    }

    public Boolean solveCompensationFailure(SagaKafkaMessage sagaKafkaMessage) throws SolvingCompensationFailureException{
        if(!isCompensationFailure){
            throw new SolvingCompensationFailureException("Failures which are not compensation failures cannot be solved.");
        }
        if(isFailureSolved){
            throw new SolvingCompensationFailureException("Failures which are already solved cannot be solved again.");
        }
        try {
            sagaKafkaProducer.sendMessage(sagaKafkaMessage);
            isFailureSolved = true;
            return true;
        } catch (KafkaMessageCouldNotSendException e) {
            return false;
        }


    }






}
