
if [ $# -lt 1 ]; then
  echo "Usage: ./run-client.sh <username>"
  exit 1
fi

USER=$1

echo "Starting client: $USER"

java -cp build vsue.rmi.VSAuctionRMIClient $USER localhost 1099