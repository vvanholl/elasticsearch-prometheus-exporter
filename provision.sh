#!/bin/bash

sudo yum install java-1.8.0-openjdk-devel -y

sudo wget https://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo

sudo yum install rpm-build -y
sudo yum install apache-maven -y

sudo alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java
sudo alternatives --set javac /usr/lib/jvm/java-1.8.0-openjdk.x86_64/bin/javac

ln -s /vagrant /home/vagrant/elasticsearch-prometheus-exporter
