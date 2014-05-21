#!/bin/bash

cd hibernate-tools && mvn clean install -DskipTests=true && cd .. 
cd hibernate3-maven-plugin-3.0 && mvn clean install -DskipTests=true && cd ..
