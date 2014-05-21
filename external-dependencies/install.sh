#!/bin/bash

cd hibernate-tools && (mvn clean install > /dev/null 2>&1 ) && cd .. 
cd hibernate3-maven-plugin-3.0 && (mvn clean install > /dev/null 2>&1 ) && cd ..
