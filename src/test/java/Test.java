import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.TreeMap;

/**
 * Autor: jianjunyang
 * Date:17/1/17
 */
public class Test {

    public static void main(String[] args) {


        try {
            File file = new File("/tmp/DemoRandomAccessFile.out");
            RandomAccessFile raf = new RandomAccessFile(file, "rw");


            for (int i = 0; i < 10; i++) {
                FileChannel ch = raf.getChannel();
                // Seek to the end of file
                raf.seek(file.length());
//                raf.writeBytes("This will complete the Demo");
                ch.write(ByteBuffer.wrap("This will complete the Demo".getBytes()));
//                ch.close();
            }

//            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TreeMap<Integer, Integer> testMap = new TreeMap<Integer, Integer>();
        testMap.put(1, 2);
        testMap.put(2, 6);
        testMap.put(3, 66);
        testMap.put(4, 666);
        testMap.put(5, 3322);

        System.out.println(testMap.firstEntry() + "----" + testMap.lastEntry());

    }
}
