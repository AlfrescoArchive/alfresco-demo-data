#!/bin/bash
# Downloads the spring-loaded lib if not existing and runs the full all-in-one
# (Alfresco + Share + Solr) using the runner project
springloadedfile=~/.m2/repository/org/springframework/springloaded/1.2.3.RELEASE/springloaded-1.2.3.RELEASE.jar

if [ ! -f $springloadedfile ]; then
mvn validate -Psetup
fi
# 5.1 + spring loaded != good combo right now
MAVEN_OPTS="-noverify -Xms512m -Xmx4G" mvn clean install -Prun
#MAVEN_OPTS="-javaagent:$springloadedfile -noverify -Xms256m -Xmx2G" mvn clean install -Prun
#MAVEN_OPTS="-javaagent:$springloadedfile -noverify -Xms256m -Xmx2G -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044" mvn clean install -Prun
