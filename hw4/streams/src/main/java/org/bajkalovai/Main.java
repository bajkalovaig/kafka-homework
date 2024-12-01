package org.bajkalovai;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Serde<String> stringSerde = Serdes.String();
        // Настройка свойств Streams
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "event-counter-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9091");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, stringSerde.getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, stringSerde.getClass());
        // Получение текущей рабочей директории
        String currentDir = System.getProperty("user.dir");
        // Формирование пути до папки src
        String statePath = currentDir + "/state_dir";
        props.put(StreamsConfig.STATE_DIR_CONFIG, statePath);

        // Создание топологии
        StreamsBuilder builder = new StreamsBuilder();

        // Чтение из входного топика
        KStream<String, String> events = builder.stream("events");

        // Группировка по ключу и подсчет в рамках временного окна
        events
                .groupByKey()
                .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(Duration.ofMinutes(5)))
                .count()
                .toStream()
                .foreach((key, value) ->
                        System.out.printf("Key: %s, Window: [%s, %s], Count: %d%n",
                                key.key(),
                                key.window().startTime(),
                                key.window().endTime(),
                                value
                ));
//                .map((key, value) -> KeyValue.pair(
//                        key.key(),
//                        String.valueOf(value)
//                ))
//                .to("event_count");

        // Создание и запуск Streams приложения
        try (KafkaStreams streams = new KafkaStreams(builder.build(), props);){
            streams.start();

            // Добавление хука для корректного завершения
            Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
            Thread.sleep(Duration.ofMinutes(2));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}