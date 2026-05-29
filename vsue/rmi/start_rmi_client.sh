#!/bin/bash

javac -d bin vsue/rmi/VSAuctionRMIClient.java

echo "Starte RMI-Client mit Argumenten: $@"

java -cp bin vsue.rmi.VSAuctionRMIClient "$@"
