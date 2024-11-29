docker exec kafka1-otuskafka bash -c "kafka-topics --create  --bootstrap-server localhost:9091 --replication-factor 1  --partitions 1 --topic events"


docker exec -it kafka1-otuskafka bash -c "kafka-console-producer --bootstrap-server localhost:9091 --topic events --property 'parse.key=true' --property 'key.separator=:'"