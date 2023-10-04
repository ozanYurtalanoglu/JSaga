package yurtalanoglu.ozan.jsaga.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import yurtalanoglu.ozan.jsaga.exception.KafkaConfigurationException;
import yurtalanoglu.ozan.jsaga.exception.SagaCommandResponseTimeoutExceededException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;


public class SagaKafkaConsumer {

    private KafkaConsumer<String, String> kafkaConsumer;
    private SagaKafkaTopic topic;

    Properties propertiesForConsumer = new Properties();

    public SagaKafkaConsumer() {
        Properties propertiesInTheFile = new Properties();
        try (InputStream inputStream = SagaKafkaConsumer.class.getClassLoader().getResourceAsStream("application.properties")) {
            propertiesInTheFile.load(inputStream);
            Properties propertiesNeeded = new Properties();
            propertiesNeeded.put("bootstrap.servers", propertiesInTheFile.getProperty("jsaga.kafka-address"));
            propertiesNeeded.put("group.id", "jsaga.kafka-group-id");
            propertiesNeeded.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            propertiesNeeded.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            this.kafkaConsumer = new KafkaConsumer<>(propertiesNeeded);
            propertiesForConsumer = propertiesNeeded;
        } catch (IOException e) {
            throw new KafkaConfigurationException("Kafka consumer could not be initialized. Check the configurations in application.properties file.");
        }
    }

    public void setTopic(SagaKafkaTopic topic){
        this.topic = topic;
    }

    public SagaKafkaTopic getTopic() {
        return topic;
    }

    public void subscribeTopic(){
        this.kafkaConsumer.subscribe(Collections.singletonList(topic.getTopicName()));
    }

    public void setAndSubscribeTopic(SagaKafkaTopic topic){
        setTopic(topic);
        subscribeTopic();
    }

    public SagaKafkaMessage consumeMessageWithTimeout(Integer timeout) throws SagaCommandResponseTimeoutExceededException {
        Duration timeoutDuration = Duration.ofSeconds(timeout);
        ConsumerRecord<String, String> firstRecord = null;
        ConsumerRecords<String, String> records = kafkaConsumer.poll(timeoutDuration);
        if (!records.isEmpty()) {
            firstRecord = records.iterator().next();
        }
        kafkaConsumer.close(); // Close the consumer after receiving the first message
        kafkaConsumer = new KafkaConsumer<>(propertiesForConsumer);
        if (firstRecord == null) {
            throw new SagaCommandResponseTimeoutExceededException();
        }
        String messageContent = firstRecord.value();
        SagaKafkaMessageFactory sagaKafkaMessageFactory = new SagaKafkaMessageFactory();
        return sagaKafkaMessageFactory.createCommandResponseWithContentAndTopicName(messageContent,topic.getTopicName());
    }
}