package yurtalanoglu.ozan.jsaga.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.protocol.types.Field;
import yurtalanoglu.ozan.jsaga.exception.KafkaConfigurationException;
import yurtalanoglu.ozan.jsaga.exception.KafkaMessageCouldNotSendException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

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

    public String sendMessage(SagaKafkaMessage sagaKafkaMessage) throws KafkaMessageCouldNotSendException {
        // Create a Headers object and add your custom header(s)
        Headers headers = new RecordHeaders();
        UUID uuid = UUID.randomUUID();
        Long uuidLong = uuid.getMostSignificantBits();
        String uuidString = String.valueOf(uuidLong);
        headers.add("uuid", uuidString.getBytes(StandardCharsets.UTF_8));

        // Use the constructor of ProducerRecord that allows to set headers
        ProducerRecord<String, String> record = new ProducerRecord<>(
                sagaKafkaMessage.getTopicName(),
                null, // you may specify the partition here or keep it null to use Kafka's default partitioner
                null, // timestamp, if needed, otherwise can be null to use current time
                null, // key - should be specified if you use a custom partitioner based on the key
                sagaKafkaMessage.getMessageContent(),
                headers
        );

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                isMessageSent = false;
            }
        });

        producer.close();
        producer = new KafkaProducer<>(propertiesForProducer);

        if (!isMessageSent){
            isMessageSent = true; // This line is not needed because if the message could not be sent, an exception is thrown and the variable is not accessed anymore
            throw new KafkaMessageCouldNotSendException();
        }
        return uuidString;
    }




}

