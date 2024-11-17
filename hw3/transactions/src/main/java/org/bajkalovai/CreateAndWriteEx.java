package org.bajkalovai;

import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;

public class CreateAndWriteEx {
    private static final Logger log = Utils.log;
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        log.info("Hello and welcome!");
        log.info("Добавляем топики...");
        Utils.addTopics();
        try (
                var producer = new KafkaProducer<String, String>(Producer.createProducerConfig(b -> {
                    b.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "hw3");
                }))
        ) {

            log.info("Открываем транзакцию");
            producer.initTransactions();
            producer.beginTransaction();
            log.info("Отправляем по 5 сообщений в topic1 и topic2");
            for (int i = 0; i < 4; ++i) {
                producer.send(new ProducerRecord<>("topic1", "message topic 1: " + i));
                producer.send(new ProducerRecord<>("topic2", "message topic 2: " + i));
            }
            log.info("Подтверждаем транзакцию");
            producer.commitTransaction();
            Thread.sleep(1000);
            log.info("Открываем вторую транзакцию");
            producer.beginTransaction();
            log.info("Отправляем по 2 сообщения в topic1 и topic2");
            for(int i = 0; i < 2; ++i){
                producer.send(new ProducerRecord<>("topic1", "rollback message topic 1: " + i));
                producer.send(new ProducerRecord<>("topic2", "rollback message topic 2: " + i));
            }
            producer.flush(); // точно запишем сообщения
            log.info("Отменяем вторую транзакцию");
            producer.abortTransaction();
        }

        log.info("Робота программы завершена");
    }
}