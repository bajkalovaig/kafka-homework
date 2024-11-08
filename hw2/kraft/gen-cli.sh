#!/bin/bash

# Parameters
read -p "Enter CN name: " cn_name
read -s -p "Enter CN password: " cn_pass
echo ""
read -s -p "Validate CN password: " cn_val_pass
echo ""
if [ "$cn_pass" != "$cn_val_pass" ]; then
    echo "Password not match"
    exit 1
fi
read -s -p "Enter CA password: " ca_pass
echo ""
echo "Generating $cn_name.keystore..."
keytool \
  -genkey \
  -keystore "$cn_name".keystore \
  -alias "$cn_name" \
  -dname CN="$cn_name" \
  -keyalg RSA \
  -validity 365 \
  -storepass "$cn_pass"

echo "Export the certificate of the user from the keystore..."
keytool \
  -certreq \
  -keystore "$cn_name".keystore \
  -alias "$cn_name" \
  -file "$cn_name".unsigned.crt \
  -storepass "$cn_pass"

echo "Sign the certificate signing request with the root CA..."
openssl x509 \
  -req \
  -CA ca.crt \
  -CAkey ca.key \
  -in "$cn_name".unsigned.crt \
  -out "$cn_name".crt \
  -days 365 \
  -CAcreateserial \
  -passin pass:"$ca_pass"

echo "Import the certificate of the CA into the user keystore..."
keytool \
  -importcert \
  -file ca.crt \
  -alias ca \
  -keystore "$cn_name".keystore \
  -storepass "$cn_pass" \
  -noprompt

echo "Import the signed certificate into the user keystore..."
keytool \
  -importcert \
  -file "$cn_name".crt \
  -alias "$cn_name" \
  -keystore "$cn_name".keystore \
  -storepass "$cn_pass"


rm "$cn_name".unsigned.crt

echo "Complite " "$cn_name".truststore