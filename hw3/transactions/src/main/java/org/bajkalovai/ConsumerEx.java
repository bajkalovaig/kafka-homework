package org.bajkalovai;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.common.serialization.StringDeserializer;

public class ConsumerEx {
    private final static String groupId = "cons-3-group";
    private final static String HOST = "localhost:9091";
    public static Logger log = LoggerFactory.getLogger("Reader: ");
    private static final Set<String> topics = Set.of("topic1","topic2");

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        log.info("Создаем Consumer для чтения commited messages");
        try (
                KafkaConsumer<String,String> consumerCommited = new KafkaConsumer<>(new HashMap<>() {{
                    put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, HOST);
                    put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                    put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
                    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                }})
                )
        {

            log.info("Подписываемся на {}", topics);
            consumerCommited.subscribe(topics);

            while (!Thread.interrupted()){
                var read = consumerCommited.poll(Duration.ofSeconds(1));
                for (var record : read) {
                    log.info("Receive commited {}:{} at {}", record.key(), record.value(), record.offset());
                }
            }

        }
    }
}
