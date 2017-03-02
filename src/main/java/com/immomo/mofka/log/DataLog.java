package com.immomo.mofka.log;

import com.immomo.mofka.conf.MofkaConf;
import com.immomo.mofka.data.MofkaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Autor: jianjunyang
 * Date:17/1/16
 */
public class DataLog {

    private AtomicInteger curIndex = new AtomicInteger(0);
    private MofkaConf conf;
    private RandomAccessFile curFile;
    private FileChannel curFileChannel;
    private Logger LOG = LoggerFactory.getLogger(DataLog.class);

    private String dataDir;

    private AtomicLong dataNum = new AtomicLong(0L);
    TreeMap<Integer, long[]> filePosMap = new TreeMap<Integer, long[]>();

    public DataLog(MofkaConf conf) {
        this.conf = conf;
        dataDir = conf.getDataDir();
        try {
            File dataFile = new File(dataDir);
            if (!dataFile.exists()) {
                dataFile.mkdirs();
            }
            curFile = new RandomAccessFile(conf.getDataDir() + "/" + curIndex + ".log", "rw");
            curFileChannel = curFile.getChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long putData(MofkaData data) {
        try {
            long curPos = curFile.getFilePointer();
            if (curPos > conf.getSegmentMaxSize()
                    || (curPos + data.length() > conf.getSegmentMaxSize())) {
                curIndex.getAndIncrement();
                curFile.close();
                curFileChannel.close();
                curFile = new RandomAccessFile(conf.getDataDir() + "/" + curIndex + ".log", "rw");
                curFileChannel = curFile.getChannel();
            }

            FileChannel channel = curFile.getChannel();

            channel.position(curFile.length());
            channel.write(ByteBuffer.wrap(data.dump()));

            //record the segment file index interval
            Integer index = new Integer(curIndex.intValue());
            if (!filePosMap.containsKey(index)) {
                filePosMap.put(index, new long[]{dataNum.get(), dataNum.addAndGet(1L)});
            } else {
                filePosMap.get(index)[1] = dataNum.addAndGet(1L);
            }
        } catch (Exception e) {
            LOG.error("fail to change data log ...", e);
        }

        return 0;
    }

    public byte[] readData(long index) {
        int fileIndex = -1;
        long[] fileIndexBeginEnd = null;
        SortedMap<Integer, long[]> indexMap = filePosMap.tailMap(0);
        Iterator<Integer> it = indexMap.keySet().iterator();

        while (it.hasNext()) {
            Integer tmpIndex = it.next();
            fileIndexBeginEnd = indexMap.get(tmpIndex);
            if (index >= fileIndexBeginEnd[0] && index < fileIndexBeginEnd[1]) {
                fileIndex = tmpIndex;
                break;
            }
        }

        if (fileIndex == -1) {
            return null;
        }

        System.out.println(String.format(" the file index is:%d, start: %d, end:%d, data index is:%d", fileIndex,
                fileIndexBeginEnd[0], fileIndexBeginEnd[1], index));
        try {
            curFile = new RandomAccessFile(conf.getDataDir() + "/" + fileIndex + ".log", "r");
            curFileChannel = curFile.getChannel();

            for (long j = fileIndexBeginEnd[0]; j <= index; j++) {

                ByteBuffer header = ByteBuffer.allocate(1 + 4 + 4);
                curFileChannel.read(header);
                header.flip();

                if (header.get() != (byte) 0x3f) {
                    System.out.println("error magic number...");
                }

                int checkSum = header.getInt();
                int length = header.getInt();
                header.clear();

                if (j == index) {
                    ByteBuffer body = ByteBuffer.allocate(length);
                    curFileChannel.read(body);
                    return body.array();
                }

                curFileChannel.position(curFileChannel.position() + length);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
