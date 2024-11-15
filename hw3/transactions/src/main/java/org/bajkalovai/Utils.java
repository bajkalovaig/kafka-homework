package org.bajkalovai;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class Utils {
    public static Logger log = LoggerFactory.getLogger("application");
    public static void addTopics() throws ExecutionException, InterruptedException {
        //Создать два топика: topic1 и topic2
        Map<String, Object> properties = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9091");

        try (var client = Admin.create(properties)) {
            List<NewTopic> requestedTopic = new ArrayList<>();
            Set<String> topics = client.listTopics().names().get();
            for (int idx : List.of(1,2)) {
                String topic_name = "topic"+idx;
                if (topics.contains(topic_name)){
                    log.info("Топик {} существует, пропускаем... ", topic_name);
                    continue;
                }
                requestedTopic.add(new NewTopic("topic"+idx, Optional.empty(), Optional.empty()));
            }

            var topicResult = client.createTopics(requestedTopic);
            // не факт, что топик уже создан, пользоваться им нельзя
            try {
                // топик точно создан, можете пользоваться
                topicResult.all().get();
            }
            catch (Exception e){
                log.error("Error: ",e);
            }

            log.info("Топики: \r\n{}\r\nсозданы", requestedTopic.toString());
        }
    }
}
