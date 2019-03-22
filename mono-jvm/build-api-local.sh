#!/bin/bash

# at least until https://youtrack.jetbrains.com/issue/KT-27188 is resolved, need to use jdk1.8

JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/Home/
gradle clean shadowJar && $JAVA_HOME/bin/java -jar mono-api/build/libs/mono-api-1.0-SNAPSHOT.jar server mono-api/config/local.yml
