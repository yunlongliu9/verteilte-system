package vsue.serialise.ProblemTest;

import java.io.*;

public class analyzeTest {

    public static void main(String[] args) throws Exception {
        analyzePureNull();
        analyzeEmptyObject();
        analyzeIntegerImpact();
        analyzeStringImpact();
        analyzeArrayImpact();
    }



    // =========================
    // 1. 纯 null 对象
    // =========================
    public static void analyzePureNull() throws Exception {

    System.out.println("\n==== Pure null object ====");

    byte[] data = serialize(null);

    System.out.println("Size: " + data.length);

    System.out.print("Hex: ");

    printHex(data);

    System.out.println("👉 Expected: 70 (TC_NULL)");

}
    // =========================
    // 2. 空对象 
    // =========================
    public static void analyzeEmptyObject() throws Exception {
        System.out.println("==== Empty Object ====");

        VSTestMessage msg = new VSTestMessage(0, null, null);
        byte[] data = serialize(msg);

        printResult(data);
    }

    // =========================
    // 3. integer 影响
    // =========================
    public static void analyzeIntegerImpact() throws Exception {
        System.out.println("\n==== Integer Impact ====");

        testInteger(0);
        testInteger(1);
        testInteger(999999);
    }

    private static void testInteger(int value) throws Exception {
        VSTestMessage msg = new VSTestMessage(value, null, null);
        byte[] data = serialize(msg);

        System.out.println("Integer = " + value + ", Size = " + data.length);
    }

    // =========================
    // 4. string 长度影响
    // =========================
    public static void analyzeStringImpact() throws Exception {
        System.out.println("\n==== String Impact ====");

        testString("A");
        testString("AAAAAA");
        testString("AAAAAAAAAAAAAAAAAAAA");
    }

    private static void testString(String s) throws Exception {
        VSTestMessage msg = new VSTestMessage(0, s, null);
        byte[] data = serialize(msg);

        System.out.println("String length = " + s.length() + ", Size = " + data.length);
    }

    // =========================
    // 5. 数组大小影响
    // =========================
    public static void analyzeArrayImpact() throws Exception {
        System.out.println("\n==== Array Impact ====");

        testArray(0);
        testArray(5);
        testArray(50);
    }

    private static void testArray(int size) throws Exception {
        Object[] arr = new Object[size];
        VSTestMessage msg = new VSTestMessage(0, null, arr);

        byte[] data = serialize(msg);

        System.out.println("Array size = " + size + ", Serialized size = " + data.length);
    }

    // =========================
    // 通用方法
    // =========================
    public static byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(obj);
        oos.flush();

        return bos.toByteArray();
    }

    public static void printResult(byte[] data) {
        System.out.println("Size: " + data.length);
        System.out.print("Hex: ");
        printHex(data);
        System.out.println();
    }

    public static void printHex(byte[] data) {
        for (byte b : data) {
            System.out.print(String.format("%02X ", b));
        }
        System.out.println();
    }
}