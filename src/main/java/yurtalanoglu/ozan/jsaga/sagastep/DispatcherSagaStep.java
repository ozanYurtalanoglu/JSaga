package yurtalanoglu.ozan.jsaga.sagastep;

import yurtalanoglu.ozan.jsaga.kafka.SagaKafkaMessage;

import java.util.function.Function;
import java.util.function.Predicate;


public class DispatcherSagaStep<T> {


    private Function<LocalActionDataHolder<T>, SagaKafkaMessage> stepMessageGenerator;
    private Function<LocalActionDataHolder<T>, SagaKafkaMessage> onCompensationMessageGenerator;
    private Predicate<SagaKafkaMessage> compensationNeededChecker;

    private LocalActionDataHolder<T> localActionDataHolderOnCompensation;




    public SagaKafkaMessage generateStepMessage(LocalActionDataHolder<T> localActionDataHolder){
        return stepMessageGenerator.apply(localActionDataHolder);
    }

    public SagaKafkaMessage generateOnCompensationMessage(LocalActionDataHolder<T> localActionDataHolder){
        return onCompensationMessageGenerator.apply(localActionDataHolder);
    }

    public Boolean checkCompensationNeededWithReturnMessage(SagaKafkaMessage returnMessage){
        return compensationNeededChecker.test(returnMessage);
    }

    public void setStepMessageGenerator(Function<LocalActionDataHolder<T>, SagaKafkaMessage> stepMessageGenerator) {
        this.stepMessageGenerator = stepMessageGenerator;
    }

    public Function<LocalActionDataHolder<T>, SagaKafkaMessage> getStepMessageGenerator() {
        return stepMessageGenerator;
    }

    public void setOnCompensationMessageGenerator(Function<LocalActionDataHolder<T>, SagaKafkaMessage> onCompensationMessageGenerator) {
        this.onCompensationMessageGenerator = onCompensationMessageGenerator;
    }

    public void setCompensationNeededChecker(Predicate<SagaKafkaMessage> compensationNeededChecker) {
        this.compensationNeededChecker = compensationNeededChecker;
    }

    public Function<LocalActionDataHolder<T>, SagaKafkaMessage> getOnCompensationMessageGenerator() {
        return onCompensationMessageGenerator;
    }

    public Predicate<SagaKafkaMessage> getCompensationNeededChecker() {
        return compensationNeededChecker;
    }

    public LocalActionDataHolder<T> getLocalActionDataHolderOnCompensation() {
        return localActionDataHolderOnCompensation;
    }

    public void setLocalActionDataHolderOnCompensation(LocalActionDataHolder<T> localActionDataHolderOnCompensation) {
        this.localActionDataHolderOnCompensation = localActionDataHolderOnCompensation;
    }
}

