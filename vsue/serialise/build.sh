
echo "Cleaning build..."
rm -rf ../../build
mkdir -p ../../build

echo "Compiling..."
cd ../../
javac -d build $(find vsue -name "*.java")

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Done."