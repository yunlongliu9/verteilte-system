#!/bin/bash

javac -d bin vsue/rmi/VSClient.java

echo "Starte Client mit Argumenten: $@"

java -cp bin vsue.rmi.VSClient "$@"
