#!/bin/bash

# Default values for drop and delay probability
DROP_RATE=${1:-0.10}
DELAY_RATE=${2:-0.10}
SAMPLES=${3:-100}

echo "Building project..."
./build.sh

echo "Running Buggy Connection Test..."
java -cp bin vsue.tools.VSBuggyConnectionTester "$DROP_RATE" "$DELAY_RATE" "$SAMPLES"
