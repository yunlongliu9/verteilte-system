#!/bin/bash

if [ "$#" -ne 6 ]; then
    echo "Usage: ./start_benchmark.sh <username> <server_cip> <client_cip> <max_auctions> <step> <samples>"
    echo "Example: ./start_benchmark.sh <username> cipterm0 cip1e3 2000 100 1000"
    exit 1
fi

USERNAME=$1
SERVER=$2.cip.cs.fau.de
CLIENT=$3.cip.cs.fau.de
MAX=$4
STEP=$5
SAMPLES=$6

DIR="~/verteilte-systeme-gruppe-04"

echo "============================================="
echo "Building project on $SERVER..."
echo "============================================="
ssh -t "$USERNAME@$SERVER" "cd $DIR && git pull && ./build.sh && rm -f output.csv && echo 'mode,auctionCount,callDurationMicros' > output.csv"

echo "============================================="
echo "Round 1: RMI Benchmark"
echo "============================================="
# Start RMI Server in the background and redirect output so ssh doesn't hang
echo "Starting RMI server on $SERVER..."
ssh "$USERNAME@$SERVER" "pkill -f 'java -cp bin vsue'"
ssh "$USERNAME@$SERVER" "cd $DIR && nohup java -cp bin vsue.rmi.VSAuctionRMIServer </dev/null > rmi_server.log 2>&1 &" &
sleep 5
echo "Running RMI benchmark on $CLIENT..."
# Run client (blocking)
ssh -t "$USERNAME@$CLIENT" "cd $DIR && java -cp bin vsue.tools.VSAuctionLatencyBenchmark rmi $SERVER 11111 $MAX $STEP $SAMPLES >> output.csv"
# Kill Server
ssh "$USERNAME@$SERVER" "pkill -f vsue.rmi.VSAuctionRMIServer"

echo "============================================="
echo "Round 2: RPC Benchmark (connectionsReuse=false)"
echo "============================================="
echo "Starting RPC server on $SERVER with connectionsReuse=false..."
ssh "$USERNAME@$SERVER" "cd $DIR && nohup java -cp bin vsue.rpc.VSAuctionServer 1111 11111 false > /dev/null 2>&1 &" &
sleep 5

echo "Running RPC benchmark on $CLIENT"
ssh -t "$USERNAME@$CLIENT" "cd $DIR && java -cp bin vsue.tools.VSAuctionLatencyBenchmark rpc_noreuse $SERVER 11111 $MAX $STEP $SAMPLES >> output.csv"

ssh "$USERNAME@$SERVER" "pkill -f vsue.rpc.VSAuctionServer"

echo "============================================="
echo "Round 3: RPC Benchmark (connectionsReuse=true)"
echo "============================================="
echo "Starting RPC server on $SERVER with connectionsReuse=true..."
ssh "$USERNAME@$SERVER" "cd $DIR && nohup java -cp bin vsue.rpc.VSAuctionServer 1111 11111 true > /dev/null 2>&1 &" &
sleep 5

echo "Running RPC benchmark on $CLIENT"
ssh -t "$USERNAME@$CLIENT" "cd $DIR && java -cp bin vsue.tools.VSAuctionLatencyBenchmark rpc_reuse $SERVER 11111 $MAX $STEP $SAMPLES >> output.csv"

ssh "$USERNAME@$SERVER" "pkill -f vsue.rpc.VSAuctionServer"

echo "============================================="
echo "Fetching output.csv from remote..."
echo "============================================="
scp "$USERNAME@$SERVER:$DIR/output.csv" ./output.csv

echo "Done! The output.csv file is now saved locally."
echo "Running plot script to generate rpc_vs_rmi.png..."

python compare_rpc_rmi.py
