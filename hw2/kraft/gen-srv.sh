#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

if ! command -v keytool &> /dev/null
then
    echo -e "${RED}keytool could not be found${NC}"
    exit 1
fi

if ! command -v openssl &> /dev/null
then
    echo -e "${RED}openssl could not be found${NC}"
    exit 1
fi

read -s -p "Enter CN password: " cn_pass
echo ""
read -s -p "Validate CN password: " cn_val_pass
echo ""
if [ "$cn_pass" != "$cn_val_pass" ]; then
    echo -e "${RED}Password not match${NC}"
    exit 1
fi

read -s -p "Enter CA password: " ca_pass
echo ""
read -s -p "Validate CA password: " ca_val_pass
echo ""
if [ "$ca_pass" != "$ca_val_pass" ]; then
    echo -e "${RED}Password not match${NC}"
    exit 1
fi

read -p "Hostname: " dns_val
echo ""
read -p "IP: " ip_val
echo ""

function create_ca_cert {
  echo -e "${GREEN}Generate a private key and a self-signed certificate for the CA... ${NC}"
  openssl req \
    -new \
    -x509 \
    -days 365 \
    -keyout ca.key \
    -out ca.crt \
    -subj "/C=RU/L=Abakan/CN=Certificate Authority" \
    -passout pass:"$ca_pass"
}

echo "Do you wish create CA cert?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) create_ca_cert; break;;
        No ) break;;
    esac
done

echo -e "${GREEN}Generate SSL Keys and Certificate for Kafka Brokers...${NC}"
keytool \
  -genkey \
  -keystore server.keystore \
  -alias localhost \
  -dname CN=localhost \
  -keyalg RSA \
  -validity 365 \
  -ext san=dns:"$dns_val",ip:"$ip_val" \
  -storepass "$cn_pass" \
  -noprompt

echo -e "${GREEN}Sign Broker Certificate (Using CA)...${NC}"

keytool \
  -certreq \
  -keystore server.keystore \
  -alias localhost \
  -file server.unsigned.crt \
  -storepass "$cn_pass"

echo "subjectAltName=DNS:""$dns_val"",DNS:localhost,IP:""$ip_val" > server-ext.cnf

openssl x509 \
  -req \
  -CA ca.crt \
  -CAkey ca.key \
  -in server.unsigned.crt \
  -extfile server-ext.cnf \
  -out server.crt \
  -days 365 \
  -CAcreateserial \
  -passin pass:"$ca_pass"

echo -e "${GREEN}Import Certificates to Broker Keystore...${NC}"

keytool \
  -import \
  -file ca.crt \
  -keystore server.keystore \
  -alias ca \
  -storepass "$cn_pass" \
  -noprompt

keytool \
  -import \
  -file server.crt \
  -keystore server.keystore \
  -alias localhost \
  -storepass "$cn_pass" \
  -noprompt

echo -e "${GREEN}Import CA Certificate to Client Truststore...${NC}"
keytool \
  -import \
  -file ca.crt \
  -keystore client.truststore \
  -alias ca \
  -storepass "$cn_pass" \
  -noprompt

echo -e "${GREEN}Done!${NC}"