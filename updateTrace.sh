#!/bin/bash
rm -r src/main/resources/native-image/
mvn clean package
java -agentlib:native-image-agent=trace-output=trace.json -jar target/git-commit-code-check-0.1.jar --check-all --batch
native-image-configure generate --trace-input=trace.json --output-dir=src/main/resources/native-image/

