package yurtalanoglu.ozan.jsaga.saga;



import yurtalanoglu.ozan.jsaga.exception.KafkaMessageCouldNotSendException;
import yurtalanoglu.ozan.jsaga.exception.SagaCommandResponseTimeoutExceededException;
import yurtalanoglu.ozan.jsaga.kafka.SagaKafkaConsumer;
import yurtalanoglu.ozan.jsaga.kafka.SagaKafkaProducer;
import yurtalanoglu.ozan.jsaga.kafka.SagaKafkaTopic;
import yurtalanoglu.ozan.jsaga.sagastep.DispatcherSagaStep;
import yurtalanoglu.ozan.jsaga.sagastep.LocalActionDataHolder;
import yurtalanoglu.ozan.jsaga.kafka.SagaKafkaMessage;
import yurtalanoglu.ozan.jsaga.sagastep.StartingSagaStep;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;



public class Saga<T,S> {

    private StartingSagaStep<T,S> startingSagaStep;
    private ArrayList<DispatcherSagaStep<S>> dispatcherSagaStepArrayList = new ArrayList<>();
    private Integer stepIndex = -1;
    private LocalActionDataHolder<S> localActionDataHolder;
    private T localActionInput;
    private Boolean isCompensationState = false;

    private final BuildingSagaStatusFactory buildingSagaStatusFactory = new BuildingSagaStatusFactory();
    private final BuildingSagaValidator buildingSagaValidator = new BuildingSagaValidator();

    private SagaKafkaProducer sagaKafkaProducer = new SagaKafkaProducer();

    private SagaKafkaConsumer sagaKafkaConsumer = new SagaKafkaConsumer();

    private static final Integer COMMAND_MESSAGE_RESPONSE_DEFAULT_TIMEOUT_SECONDS = 10;

    private Integer commandMessageResponseTimeoutSeconds;

    private List<SagaExecutionFailure<S>> dispatcherStepFailureList = new ArrayList<>();

    private List<SagaExecutionFailure<S>> onCompensationDispatcherStepFailureList = new ArrayList<>();

    {
        try (InputStream inputStream = SagaKafkaProducer.class.getClassLoader().getResourceAsStream("application.properties")) {
            Properties propertiesInTheFile = new Properties();
            propertiesInTheFile.load(inputStream);
            String commandMessageResponseTimeoutSecondsString = propertiesInTheFile.getProperty("jsaga.command-message-response-timeout-seconds");
            if(commandMessageResponseTimeoutSecondsString == null){
                commandMessageResponseTimeoutSeconds = COMMAND_MESSAGE_RESPONSE_DEFAULT_TIMEOUT_SECONDS;
            }
            else{
                commandMessageResponseTimeoutSeconds = Integer.valueOf(commandMessageResponseTimeoutSecondsString);
            }

        } catch (Exception e) {
            commandMessageResponseTimeoutSeconds = COMMAND_MESSAGE_RESPONSE_DEFAULT_TIMEOUT_SECONDS;
        }
    }


    public Boolean executeSaga(){
        executeStartingStep();
        executeDispatcherSteps();
        if(isCompensationState){
            executeDispatcherStepsInCompensationState();
            executeLocalCompensationAction();
            return false;
        }
        return true;
    }

    private void executeLocalCompensationAction(){
        startingSagaStep.doLocalCompensationAction(localActionDataHolder);
    }

    private void executeStartingStep(){
        localActionDataHolder = startingSagaStep.doLocalAction(localActionInput);
        stepIndex++;
    }

    private void executeDispatcherSteps(){
        while(stepIndex < dispatcherSagaStepArrayList.size()){
            executeNextDispatcherStep();
            if (isCompensationState){
                break;
            }
        }
    }

    private void executeDispatcherStepsInCompensationState(){
        while (stepIndex > -1){
            executeNextCompensationDispatcherStep();
        }
    }

     private SagaKafkaMessage sendCommandAndTakeResponseWithKafka(SagaKafkaMessage commandMessage) throws SagaCommandResponseTimeoutExceededException, KafkaMessageCouldNotSendException {
        String uuid = sagaKafkaProducer.sendMessage(commandMessage);
        StringBuilder commandResponseTopicStringBuilder = new StringBuilder();
        commandResponseTopicStringBuilder.append(commandMessage.getTopicName()).append("-response");
        SagaKafkaTopic commandResponseTopic = new SagaKafkaTopic(commandResponseTopicStringBuilder.toString());
        sagaKafkaConsumer.setAndSubscribeTopic(commandResponseTopic,uuid);
        return sagaKafkaConsumer.consumeMessageWithTimeout(commandMessageResponseTimeoutSeconds);
    }


    private void executeNextDispatcherStep(){
        DispatcherSagaStep<S> nextDispatcherSagaStep = dispatcherSagaStepArrayList.get(stepIndex);
        try {
            SagaKafkaMessage commandMessage = nextDispatcherSagaStep.generateStepMessage(localActionDataHolder);
            SagaKafkaMessage commandResponseMessage = sendCommandAndTakeResponseWithKafka(commandMessage);
            isCompensationState = nextDispatcherSagaStep.checkCompensationNeededWithReturnMessage(commandResponseMessage);
        } catch (Throwable e) {
            SagaExecutionFailure sagaExecutionFailure = new SagaExecutionFailure(nextDispatcherSagaStep,e,false);
            dispatcherStepFailureList.add(sagaExecutionFailure);
            isCompensationState = true;
        }
        if (!isCompensationState){
            stepIndex++;
        }
        else {
            stepIndex--;
        }
    }

    private void executeNextCompensationDispatcherStep() {
        DispatcherSagaStep<S> nextCompensationDispatcherSagaStep = dispatcherSagaStepArrayList.get(stepIndex);
        SagaKafkaMessage compensationMessage = new SagaKafkaMessage();
        try {
            if(nextCompensationDispatcherSagaStep.getOnCompensationMessageGenerator() != null){
                compensationMessage = nextCompensationDispatcherSagaStep.generateOnCompensationMessage(localActionDataHolder);
                sagaKafkaProducer.sendMessage(compensationMessage);
            }
        }catch (Throwable e){
            SagaExecutionFailure sagaExecutionFailure = new SagaExecutionFailure(nextCompensationDispatcherSagaStep,e,true);
            if (! compensationMessage.getTopicName().isEmpty()){
                sagaExecutionFailure.setOnCompensationKafkaMesssage(compensationMessage);
            }
            onCompensationDispatcherStepFailureList.add(sagaExecutionFailure);
        }
        finally {
            stepIndex--;
        }
    }

    protected void addStartingStep(){
        BuildingSagaStatus buildingSagaStatus = buildingSagaStatusFactory.createWithSaga(this);
        buildingSagaValidator.validateWhenAddingStartingStep(buildingSagaStatus);
        startingSagaStep = new StartingSagaStep<>();
    }

    protected void setLocalActionInput(T localActionInput){
        BuildingSagaStatus buildingSagaStatus = buildingSagaStatusFactory.createWithSaga(this);
        buildingSagaValidator.validateWhenBuildingStartingStep(buildingSagaStatus);
        this.localActionInput = localActionInput;
    }

    protected void setLocalAction(Function<T, LocalActionDataHolder<S>> localAction){
        BuildingSagaStatus buildingSagaStatus = buildingSagaStatusFactory.createWithSaga(this);
        buildingSagaValidator.validateWhenBuildingStartingStep(buildingSagaStatus);
        startingSagaStep.setLocalAction(localAction);
    }

    protected void setLocalCompensationAction(Consumer<LocalActionDataHolder<S>> localCompensationAction){
        BuildingSagaStatus buildingSagaStatus = buildingSagaStatusFactory.createWithSaga(this);
        buildingSagaValidator.validateWhenBuildingStartingStep(buildingSagaStatus);
        startingSagaStep.setLocalCompensationAction(localCompensationAction);
    }


    protected void addDispatcherStep(){
        BuildingSagaStatus buildingSagaStatus = buildingSagaStatusFactory.createWithSaga(this);
        buildingSagaValidator.validateWhenAddingDispatcherStep(buildingSagaStatus);
        dispatcherSagaStepArrayList.add(new DispatcherSagaStep<>());
    }

    protected void setStepMessageGenarator (Function<LocalActionDataHolder<S>, SagaKafkaMessage> stepMessageGenarator){
        BuildingSagaStatus buildingSagaStatus = buildingSagaStatusFactory.createWithSaga(this);
        buildingSagaValidator.validateWhenBuildingDispatcherStep(buildingSagaStatus);
        int lastDispatcherStepIndex = dispatcherSagaStepArrayList.size() - 1;
        DispatcherSagaStep lastDispatcherStep = dispatcherSagaStepArrayList.get(lastDispatcherStepIndex);
        lastDispatcherStep.setStepMessageGenerator(stepMessageGenarator);
    }

    protected void setOnCompensationMessageGenarator (Function<LocalActionDataHolder<S>, SagaKafkaMessage> onCompensationMessageGenarator){
        BuildingSagaStatus buildingSagaStatus = buildingSagaStatusFactory.createWithSaga(this);
        buildingSagaValidator.validateWhenBuildingDispatcherStep(buildingSagaStatus);
        int lastDispatcherStepIndex = dispatcherSagaStepArrayList.size() - 1;
        DispatcherSagaStep lastDispatcherStep = dispatcherSagaStepArrayList.get(lastDispatcherStepIndex);
        lastDispatcherStep.setOnCompensationMessageGenerator(onCompensationMessageGenarator);
    }

    protected void setCompensationNeededChecker (Predicate<SagaKafkaMessage> compensationNeededChecker){
        BuildingSagaStatus buildingSagaStatus = buildingSagaStatusFactory.createWithSaga(this);
        buildingSagaValidator.validateWhenBuildingDispatcherStep(buildingSagaStatus);
        int lastDispatcherStepIndex = dispatcherSagaStepArrayList.size() - 1;
        DispatcherSagaStep lastDispatcherStep = dispatcherSagaStepArrayList.get(lastDispatcherStepIndex);
        lastDispatcherStep.setCompensationNeededChecker(compensationNeededChecker);
    }

    protected StartingSagaStep<T, S> getStartingSagaStep() {
        return startingSagaStep;
    }

    public Optional<LocalActionDataHolder<S>> getLocalActionDataHolder() {
        if(localActionDataHolder != null){
            return Optional.of(localActionDataHolder);
        }
        else{
            return Optional.empty();
        }
    }

    protected T getLocalActionInput() {
        return localActionInput;
    }

    protected Optional<Function<T, LocalActionDataHolder<S>>> getLocalAction(){
        if(startingSagaStep != null){
            return startingSagaStep.getLocalAction() == null ? Optional.empty() : Optional.of(startingSagaStep.getLocalAction());
        }
        else{
            return Optional.empty();
        }
    }

    protected Optional<Consumer<LocalActionDataHolder<S>>> getLocalCompensationAction(){
        if(startingSagaStep != null){
            return startingSagaStep.getLocalCompensationAction() == null ? Optional.empty() : Optional.of(startingSagaStep.getLocalCompensationAction());
        }
        else{
            return Optional.empty();
        }
    }

    protected Boolean isDispatcherStepListEmpty(){
        return dispatcherSagaStepArrayList.isEmpty();
    }

    protected Optional<DispatcherSagaStep> getLastAddedDispatcherStep(){
        return isDispatcherStepListEmpty() ? Optional.empty() : Optional.of(dispatcherSagaStepArrayList.get(dispatcherSagaStepArrayList.size()-1));
    }

    protected Optional<Function<LocalActionDataHolder<S>, SagaKafkaMessage>> getLastAddedDispatcherStepMessageGenerator(){
        if (getLastAddedDispatcherStep().isEmpty()){
            return Optional.empty();
        }
        else {
            return getLastAddedDispatcherStep().get().getStepMessageGenerator() != null ? Optional.of(getLastAddedDispatcherStep().get().getStepMessageGenerator()) : Optional.empty();
        }
    }

    protected Optional<Predicate<SagaKafkaMessage>> getLastAddedDispatcherStepCompensationNeededChecker(){
        if (getLastAddedDispatcherStep().isEmpty()){
            return Optional.empty();
        }
        else {
            return getLastAddedDispatcherStep().get().getCompensationNeededChecker() != null ? Optional.of(getLastAddedDispatcherStep().get().getCompensationNeededChecker()) : Optional.empty();
        }
    }

    public List<SagaExecutionFailure<S>> getDispatcherStepFailureList() {
        return dispatcherStepFailureList;
    }

    public List<SagaExecutionFailure<S>> getOnCompensationDispatcherStepFailureList() {
        return onCompensationDispatcherStepFailureList;
    }

    public static class SagaBuilder<U,V>{
        private Saga<U,V> builtSaga = new Saga<>();

        public SagaBuilder<U,V> addStartingStep(){
            builtSaga.addStartingStep();
            return this;
        }

        public SagaBuilder<U,V> setLocalAction(Function<U, LocalActionDataHolder<V>> localAction){
            builtSaga.setLocalAction(localAction);
            return this;
        }

        public SagaBuilder<U,V> setLocalActionInput(U localActionInput){
            builtSaga.setLocalActionInput(localActionInput);
            return this;
        }

        public SagaBuilder<U,V> setLocalCompensationAction(Consumer<LocalActionDataHolder<V>> localCompensationAction){
            builtSaga.setLocalCompensationAction(localCompensationAction);
            return this;
        }

        public SagaBuilder<U,V> addDispatcherStep(){
            builtSaga.addDispatcherStep();
            return this;
        }

        public SagaBuilder<U,V> setStepMessageGenerator(Function<LocalActionDataHolder<V>, SagaKafkaMessage> stepMessageGenarator){
            builtSaga.setStepMessageGenarator(stepMessageGenarator);
            return this;
        }

        public SagaBuilder<U,V> setOnCompensationMessageGenerator(Function<LocalActionDataHolder<V>, SagaKafkaMessage> onCompensationMessageGenarator){
            builtSaga.setOnCompensationMessageGenarator(onCompensationMessageGenarator);
            return this;
        }

        public SagaBuilder<U,V> setCompensationNeededChecker (Predicate<SagaKafkaMessage> compensationNeededChecker){
            builtSaga.setCompensationNeededChecker(compensationNeededChecker);
            return this;
        }

        public Saga<U,V> buildSaga(){
            //Validating adding dispatcher step and validating building saga has same rules.
            //So we can use validateWhenAddingDispatcherStep method.
            BuildingSagaStatusFactory buildingSagaStatusFactory = new BuildingSagaStatusFactory();
            BuildingSagaStatus buildingSagaStatus = buildingSagaStatusFactory.createWithSaga(builtSaga);
            BuildingSagaValidator buildingSagaValidator = new BuildingSagaValidator();
            buildingSagaValidator.validateWhenFinallyBuildingSaga(buildingSagaStatus);
            return builtSaga;
        }




    }




}
