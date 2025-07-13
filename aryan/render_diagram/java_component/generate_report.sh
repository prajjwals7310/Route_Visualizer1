#!/bin/bash

CONFIG_FILE=input/config.properties
CAMEL_VERSION=$1

cd "$(dirname "$0")" 

echo "📥 Step 1: Generate JSON"
mvn compile exec:java -Dexec.mainClass="com.example.camel.JsonGenerator" -Dexec.args="$CAMEL_VERSION" -Dconfig.file=$CONFIG_FILE

echo "📄 Step 2: Generate Text Report"
mvn compile exec:java -Dexec.mainClass="com.example.camel.TextReportGenerator" -Dconfig.file=$CONFIG_FILE

echo "✅ Done!"
