#!/bin/bash

javac -d bin vsue/rmi/VSAuctionRMIServer.java

echo "Starte RMI-Server mit Argumenten: $@"

java -cp bin vsue.rmi.VSAuctionRMIServer "$@"