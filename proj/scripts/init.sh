#!/bin/bash
echo "Waiting for Kafka to start listening on broker01 ⏳"
while ! nc -z broker01 9093;
do
    echo "Kafka broker is not available yet, retrying in 5 seconds ..."
    sleep 5
done
echo "Kafka broker is available."

echo "Init ACLS"
kafka-topics --bootstrap-server broker01:9093 --command-config /etc/kafka/admin.properties --create --topic pg_cdc.public.events
kafka-topics --bootstrap-server broker01:9093 --command-config /etc/kafka/admin.properties --create --topic events-output
kafka-acls --bootstrap-server broker01:9093 --command-config /etc/kafka/admin.properties  --add --allow-principal User:consumer --consumer --group '*' --topic events-output
# Allow Streams to read the input topics:
kafka-acls --bootstrap-server broker01:9093 --command-config /etc/kafka/admin.properties  --add --allow-principal User:application --operation READ --topic pg_cdc.public.events
# Allow Streams to write to the output topics:
kafka-acls --bootstrap-server broker01:9093 --command-config /etc/kafka/admin.properties  --add --allow-principal User:application --operation WRITE --operation CREATE --topic events-output
# Allow Streams to manage its own internal topics:
kafka-acls --bootstrap-server broker01:9093 --command-config /etc/kafka/admin.properties  --add --allow-principal User:application --operation READ --operation DELETE --operation WRITE --operation CREATE --resource-pattern-type prefixed --topic event-app
# Allow Streams to manage its own consumer groups:
kafka-acls --bootstrap-server broker01:9093 --command-config /etc/kafka/admin.properties  --add --allow-principal User:application --operation READ --operation DESCRIBE --group event-app
# Allow Streams EOS:
kafka-acls --bootstrap-server broker01:9093 --command-config /etc/kafka/admin.properties  --add --allow-principal User:application --operation WRITE --operation DESCRIBE --transactional-id event-app --resource-pattern-type prefixed
echo "Init Connectors"
echo "Waiting for Kafka Connect to start listening on connect ⏳"
while true
do
    curl_status="$(curl -s -o /dev/null -w '%{http_code}' 'http://connect:8083/connectors')"
    if [ $curl_status -eq 200 ]
    then
        break
    fi
    echo -e "$(date)" " Kafka Connect listener HTTP state: " $curl_status " (waiting for 200)"
    sleep 5 
done
curl -X POST --data-binary "@/scripts/sensors.json" -H "Content-Type: application/json" http://connect:8083/connectors
echo "Connector initialized."
echo "Exit."