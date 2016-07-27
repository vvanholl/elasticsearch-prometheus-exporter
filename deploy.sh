#!/bin/bash


scp target/releases/elasticsearch-prometheus-exporter-1.0-SNAPSHOT.zip root@develop01:/tmp
ssh root@develop01 "JAVA_HOME=/usr/local/java/jdk /usr/local/elasticsearch/bin/plugin remove prometheus-exporter"
ssh root@develop01 "JAVA_HOME=/usr/local/java/jdk /usr/local/elasticsearch/bin/plugin install file:///tmp/elasticsearch-prometheus-exporter-1.0-SNAPSHOT.zip"
