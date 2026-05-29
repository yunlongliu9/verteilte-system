
mkdir -p bin

find vsue -name "*.java" > sources.txt

javac -d bin @sources.txt

rm sources.txt

echo "Build finished."