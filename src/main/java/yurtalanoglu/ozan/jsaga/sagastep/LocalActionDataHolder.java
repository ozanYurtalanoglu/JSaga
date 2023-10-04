package yurtalanoglu.ozan.jsaga.sagastep;

import java.util.Optional;

public class LocalActionDataHolder<T> {
    private T dataBeforeLocalAction;
    private T dataAfterLocalAction;

    public Optional<T> getDataBeforeLocalAction() {
        if(dataBeforeLocalAction == null){
            return Optional.empty();
        }else{
            return Optional.of(dataBeforeLocalAction);
        }
    }

    public void setDataBeforeLocalAction(T dataBeforeLocalAction) {
        this.dataBeforeLocalAction = dataBeforeLocalAction;
    }

    public Optional<T> getDataAfterLocalAction() {
        if(dataAfterLocalAction == null){
            return Optional.empty();
        }else{
            return Optional.of(dataAfterLocalAction);
        }
    }

    public void setDataAfterLocalAction(T dataAfterLocalAction) {
        this.dataAfterLocalAction = dataAfterLocalAction;
    }
}
