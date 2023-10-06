package yurtalanoglu.ozan.jsaga.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import yurtalanoglu.ozan.jsaga.exception.KafkaConfigurationException;
import yurtalanoglu.ozan.jsaga.exception.SagaCommandResponseTimeoutExceededException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.Properties;


public class SagaKafkaConsumer {

    private KafkaConsumer<String, String> kafkaConsumer;
    private SagaKafkaTopic topic;

    private String uuid;

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

    public void setAndSubscribeTopic(SagaKafkaTopic topic,String uuid){
        setTopic(topic);
        subscribeTopic();
        this.uuid = uuid;
    }

    public SagaKafkaMessage consumeMessageWithTimeout(Integer timeoutSeconds) throws SagaCommandResponseTimeoutExceededException {
        Duration timeoutDuration = Duration.ofSeconds(timeoutSeconds);
        ConsumerRecord<String, String> firstRecord = null;
        String messageContent;
        Boolean expectedMessageHasCome = false;
        Long startTimeMillis = System.currentTimeMillis();
        Long currentTimeMillis;
        while(true){
            ConsumerRecords<String, String> records = kafkaConsumer.poll(timeoutDuration);
            messageContent = recordsContainExpectedMessage(records);
            if (messageContent != null){
                expectedMessageHasCome = true;
                break;
            }
            currentTimeMillis = System.currentTimeMillis();
            Long spentTimeMilis = currentTimeMillis - startTimeMillis;
            if (timeoutDuration.toMillis() - spentTimeMilis > 0){
                timeoutDuration = Duration.ofMillis(timeoutDuration.toMillis() - spentTimeMilis);
            }
            else {
                break;
            }
        }

        kafkaConsumer.close(); // Close the consumer after receiving the first message
        kafkaConsumer = new KafkaConsumer<>(propertiesForConsumer);
        if (expectedMessageHasCome == false) {
            throw new SagaCommandResponseTimeoutExceededException();
        }
        SagaKafkaMessageFactory sagaKafkaMessageFactory = new SagaKafkaMessageFactory();
        return sagaKafkaMessageFactory.createCommandResponseWithContentAndTopicName(messageContent,topic.getTopicName());
    }

    private String recordsContainExpectedMessage(ConsumerRecords<String, String> records){
        for(ConsumerRecord<String, String> record : records ){
            for(Header header : record.headers()){
                if (header.key().equals("uuid")){
                    String messageUuid = new String(header.value(), StandardCharsets.UTF_8);
                    if (messageUuid.equals(uuid)){
                        return record.value();
                    }
                }
            }
        }
        return null;
    }



}