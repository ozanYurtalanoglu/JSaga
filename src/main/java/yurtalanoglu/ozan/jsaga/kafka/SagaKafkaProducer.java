package yurtalanoglu.ozan.jsaga.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.protocol.types.Field;
import yurtalanoglu.ozan.jsaga.exception.KafkaConfigurationException;
import yurtalanoglu.ozan.jsaga.exception.KafkaMessageCouldNotSendException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SagaKafkaProducer {

    private Producer<String, String> producer;

    Properties propertiesForProducer = new Properties();

    private static final Integer REQUEST_TIMEOUT_DEFAULT_MS = 15000;

    private Boolean isMessageSent = true;

    public SagaKafkaProducer() {
        Properties propertiesInTheFile = new Properties();
        try (InputStream inputStream = SagaKafkaProducer.class.getClassLoader().getResourceAsStream("application.properties")) {
            propertiesInTheFile.load(inputStream);
            Properties propertiesNeeded = new Properties();
            propertiesNeeded.put("bootstrap.servers", propertiesInTheFile.getProperty("jsaga.kafka-address"));
            String requestTimeoutString = propertiesInTheFile.getProperty("jsaga.kafka-ack-timeout-ms");
            if (requestTimeoutString == null){
                propertiesNeeded.put("request.timeout.ms", REQUEST_TIMEOUT_DEFAULT_MS);
            }
            else {
                propertiesNeeded.put("request.timeout.ms", Integer.valueOf(requestTimeoutString));
            }
            propertiesNeeded.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            propertiesNeeded.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            this.producer = new KafkaProducer<>(propertiesNeeded);
            propertiesForProducer=propertiesNeeded;
        } catch (IOException e) {
            throw new KafkaConfigurationException("Kafka producer could not be initialized. Check the configurations in application.properties file.");
        }
    }

    public void sendMessage(SagaKafkaMessage sagaKafkaMessage) throws KafkaMessageCouldNotSendException{
        ProducerRecord<String, String> record = new ProducerRecord<>(sagaKafkaMessage.getTopicName(), sagaKafkaMessage.getMessageContent());
        producer.send((record), (metadata, exception) -> {
            if (exception != null) {
                isMessageSent = false;
            }
        });
        producer.close();
        producer = new KafkaProducer<>(propertiesForProducer);
        if (!isMessageSent){
            isMessageSent = true;
            throw new KafkaMessageCouldNotSendException();
        }

    }




}

