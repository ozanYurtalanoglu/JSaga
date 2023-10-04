package yurtalanoglu.ozan.jsaga.kafka;

public class SagaKafkaTopic {

    private String topicName;

    public SagaKafkaTopic(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

}
