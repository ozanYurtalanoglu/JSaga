package yurtalanoglu.ozan.jsaga.sagastep;



import java.util.function.Consumer;
import java.util.function.Function;

public class StartingSagaStep<T,S> {
    private Function<T, LocalActionDataHolder<S>> localAction;
    private Consumer<LocalActionDataHolder<S>> localCompensationAction;


    public LocalActionDataHolder<S> doLocalAction(T localActionInput){
        return localAction.apply(localActionInput);
    }

    public void doLocalCompensationAction(LocalActionDataHolder<S> localActionDataHolder){
        localCompensationAction.accept(localActionDataHolder);
    }

    public void setLocalAction(Function<T, LocalActionDataHolder<S>> localAction) {
        this.localAction = localAction;
    }

    public void setLocalCompensationAction(Consumer<LocalActionDataHolder<S>> localCompensationAction) {
        this.localCompensationAction = localCompensationAction;
    }

    public Function<T, LocalActionDataHolder<S>> getLocalAction() {
        return localAction;
    }

    public Consumer<LocalActionDataHolder<S>> getLocalCompensationAction() {
        return localCompensationAction;
    }
}
