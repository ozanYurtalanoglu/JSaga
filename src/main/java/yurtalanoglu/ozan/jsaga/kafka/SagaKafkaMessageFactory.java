package yurtalanoglu.ozan.jsaga.kafka;


public class SagaKafkaMessageFactory {

    public SagaKafkaMessage createCommandWithContentAndTopicName(String messageContent, String topicName){
        SagaKafkaMessage sagaKafkaMessage = createMessageWithContentAndTopicName(messageContent,topicName);
        sagaKafkaMessage.setMessageTypeAsCommandMessage();
        return sagaKafkaMessage;
    }

    public SagaKafkaMessage createCommandResponseWithContentAndTopicName(String messageContent, String topicName){
        SagaKafkaMessage sagaKafkaMessage = createMessageWithContentAndTopicName(messageContent,topicName);
        sagaKafkaMessage.setMessageTypeAsCommandResponseMessage();
        return sagaKafkaMessage;
    }

    public SagaKafkaMessage createCompensationWithContentAndTopicName(String messageContent, String topicName){
        SagaKafkaMessage sagaKafkaMessage = createMessageWithContentAndTopicName(messageContent,topicName);
        sagaKafkaMessage.setMessageTypeAsCompensationMessage();
        return sagaKafkaMessage;
    }

    private SagaKafkaMessage createMessageWithContentAndTopicName(String messageContent, String topicName){
        SagaKafkaMessage sagaKafkaMessage = new SagaKafkaMessage();
        sagaKafkaMessage.setMessageContent(messageContent);
        sagaKafkaMessage.setKafkaTopic(new SagaKafkaTopic(topicName));
        return sagaKafkaMessage;
    }


}
