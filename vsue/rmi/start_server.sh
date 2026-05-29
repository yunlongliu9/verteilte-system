#!/bin/bash

javac -d bin vsue/rmi/VSServer.java

echo "Starte Server mit Argumenten: $@"

java -cp bin vsue.rmi.VSServer "$@"