package yurtalanoglu.ozan.jsaga.exception;

public class KafkaMessageCouldNotSendException extends Exception{
    public KafkaMessageCouldNotSendException() {
        super("Kafka producer could not send the message.");
    }
}
