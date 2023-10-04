package yurtalanoglu.ozan.jsaga.kafka;

import yurtalanoglu.ozan.jsaga.kafka.SagaKafkaTopic;

public class SagaKafkaMessage {
    private static final Short COMMAND_MESSAGE = 0;
    private static final Short COMMAND_MESSAGE_RESPONSE = 1;
    private static final Short COMPENSATION_MESSAGE = 2;
    private String messageContent;
    private SagaKafkaTopic kafkaTopic;
    private Short messageType;

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }


    public SagaKafkaTopic getKafkaTopic() {
        return kafkaTopic;
    }

    public void setKafkaTopic(SagaKafkaTopic kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }

    public void setMessageTypeAsCommandMessage() {
        this.messageType = COMMAND_MESSAGE;
    }

    public void setMessageTypeAsCommandResponseMessage() {
        this.messageType = COMMAND_MESSAGE_RESPONSE;
    }


    public void setMessageTypeAsCompensationMessage() {
        this.messageType = COMPENSATION_MESSAGE;
    }

    public String getTopicName(){
        return kafkaTopic.getTopicName();
    }
}
