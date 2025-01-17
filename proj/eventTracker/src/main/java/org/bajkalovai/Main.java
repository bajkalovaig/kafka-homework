package org.bajkalovai;

import java.time.Duration;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;

public class Main {
    private static final String OUTPUT_TOPIC = "events-output";
    private static final String INPUT_TOPIC = "pg_cdc.public.events";
    private static final double MAX_PRESSURE_THRESHOLD = 7.8; // бар
    private static final double MIN_PRESSURE_THRESHOLD = 3.8; // бар
    private static final long WINDOW_SIZE = 10;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static void main(String[] args) {
        System.out.println("Starting app!");

        // Конфигурация Serdes
        final Serde<String> stringSerde = Serdes.String();
        final Serde<PressureStats> pressureStatsSerde = createPressureStatsSerde();

        // Настройка свойств Streams
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "event-app");
//        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "broker01:9093");
        props.put(StreamsConfig.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name);
        props.put("sasl.mechanism", "PLAIN");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, stringSerde.getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, stringSerde.getClass());
        props.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"application\" password=\"application-pswd\";");

        // Создание топологии
        StreamsBuilder builder = new StreamsBuilder();

        // Чтение из входного топика
        KStream<String, String> events = builder.stream(INPUT_TOPIC);

        // Группировка по ключу и подсчет в рамках временного окна
        events
                .mapValues(value -> {
                    try {
                        ObjectNode node = (ObjectNode) objectMapper.readTree(value);
                        return node.get("sensor_value").asDouble();
                    } catch (Exception e) {
                        return -1.0; // Invalid reading
                    }
                })
                .filter((key, pressure) -> pressure > 0) // Отфильтровываем некорректные значения
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(WINDOW_SIZE)))
                .aggregate(
                        PressureStats::new,
                        (key, pressure, aggregate) -> {
                            aggregate.updateStats(pressure);
                            return aggregate;
                        },
                        Materialized.with(stringSerde, pressureStatsSerde)
                ).suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .filter((key, stats) -> {
                    double averagePressure = stats.getAveragePressure();
                    System.out.printf("Key: %s, Window: [%s, %s], averagePressure: %f%n",
                            key.key(),
                            key.window().startTime(),
                            key.window().endTime(),
                            averagePressure);
                    return averagePressure > MAX_PRESSURE_THRESHOLD || averagePressure < MIN_PRESSURE_THRESHOLD;
                })
                .map((key, stats) -> {
                    double averagePressure = stats.getAveragePressure();
                    String message = String.format("Alert: Average pressure %.2f is out of bounds [%.2f - %.2f]",
                            averagePressure, MIN_PRESSURE_THRESHOLD, MAX_PRESSURE_THRESHOLD);
                    return KeyValue.pair(key.key(),message);
                })
                .to(OUTPUT_TOPIC);

        // Создание и запуск Streams приложения
        try (KafkaStreams streams = new KafkaStreams(builder.build(), props)) {
            streams.start();

            // Добавление хука для корректного завершения
            Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
            // Держим приложение запущенным
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Serde<PressureStats> createPressureStatsSerde() {
        return new Serde<PressureStats>() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public Serializer<PressureStats> serializer() {
                return (topic, data) -> {
                    try {
                        return mapper.writeValueAsBytes(data);
                    } catch (Exception e) {
                        throw new RuntimeException("Error serializing PressureStats", e);
                    }
                };
            }

            @Override
            public Deserializer<PressureStats> deserializer() {
                return (topic, data) -> {
                    try {
                        return mapper.readValue(data, PressureStats.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Error deserializing PressureStats", e);
                    }
                };
            }
        };
    }

    @Setter
    @Getter
    public static class PressureStats {
        private double maxPressure;
        private double sumPressure;
        private long readingsCount;
        private double averagePressure;

        public PressureStats(double sumPressure, long readingsCount) {
            this.maxPressure = 0.0;
            this.sumPressure = sumPressure;
            this.readingsCount = readingsCount;
            this.averagePressure = getAveragePressure();
        }

        public PressureStats() {
            this(0.0, 0L);
        }

        public void updateStats(double pressure) {
            this.maxPressure = Math.max(this.maxPressure, pressure);
            this.sumPressure += pressure;
            this.readingsCount++;
            this.averagePressure = getAveragePressure();
        }

        public double getAveragePressure() {
            return readingsCount > 0 ? sumPressure / readingsCount : 0.0;
        }
    }
}