package yurtalanoglu.ozan.jsaga.exception;

public class SagaCommandResponseTimeoutExceededException extends Exception{
    public SagaCommandResponseTimeoutExceededException() {
        super("Saga response has not been taken in timeout duration");
    }
}
