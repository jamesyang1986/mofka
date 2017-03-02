package com.immomo.mofka.log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingDeque;

import com.immomo.mofka.conf.MofkaConf;
import com.immomo.mofka.index.Index;
import org.apache.log4j.Logger;

/**
 * Autor: jianjunyang
 * Date:17/1/16
 */
public class IndexLog extends Thread {
    private static final String LOG_FILE_SUFFIX = "mofka.index";
    private static LinkedBlockingDeque<Index> queue = new LinkedBlockingDeque<Index>();

    private long lastIndex;
    private String indexDir;
    private File indexFile;
    private static volatile transient IndexLog logInstance;
    private TreeMap<Integer, long[]> filePosMap = new TreeMap<Integer, long[]>();

    private static Logger logger = Logger.getLogger(IndexLog.class);

    public static IndexLog getLogInstance(MofkaConf conf) {
        if (logInstance == null) {
            synchronized (IndexLog.class) {
                if (logInstance == null) {
                    logInstance = new IndexLog(conf);
                }
            }
        }
        return logInstance;
    }

    private IndexLog(MofkaConf conf) {
        this.indexDir = conf.getIndexDir();
        readIndex();
        this.start();
    }

    private void readIndex() {
        try {
            File indexFile = new File(indexDir + "/" + LOG_FILE_SUFFIX);
            if (!indexFile.exists()) {
                indexFile.createNewFile();
            }

            if (indexFile.length() != 0) {
                RandomAccessFile file = new RandomAccessFile(indexFile, "rw");
                FileChannel fc = file.getChannel();
                ByteBuffer bb = ByteBuffer.allocate(Index.INDEX_HEADER_SIZE);

                while (fc.read(bb) > 0) {
                    //ignore index magic
                    bb.get();
                    int fileNo = bb.getInt();
                    long start = bb.getLong();
                    long end = bb.getLong();

                    if (!filePosMap.containsKey(fileNo)) {
                        filePosMap.put(fileNo, new long[]{start, end});
                    } else {
                        long[] beginEnd = filePosMap.get(fileNo);
                        filePosMap.put(fileNo, new long[]{(start < beginEnd[0] ?
                                start : beginEnd[0]), (end > beginEnd[1] ? end : beginEnd[1])});
                    }
                    bb.clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("read index fail:", e);
            System.exit(0);
        }
    }

    public static boolean putIndex(Index index) {
        return queue.offer(index);
    }

    public void run() {
        for (; ; ) {
            try {
                Index index = queue.take();
                int fileNo = index.getFileNo();
                long start = index.getStart();
                long end = index.getEnd();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
