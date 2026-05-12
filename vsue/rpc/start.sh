
# clean old classes
rm -rf out

# create output directory
mkdir -p out

# compile from project root
javac \
-d out \
$(find vsue -name "*.java")

# result
if [ $? -eq 0 ]; then
    echo "✅ Compile successful"
else
    echo "❌ Compile failed"
fi