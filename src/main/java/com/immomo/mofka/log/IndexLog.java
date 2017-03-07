package com.immomo.mofka.log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.SortedMap;
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

    private long lastFileIndex;
    private String indexDir;
    private File indexFile;
    private static volatile transient IndexLog logInstance;
    private TreeMap<Integer, Index> fileIndexMap = new TreeMap<Integer, Index>();

    private static Logger logger = Logger.getLogger(IndexLog.class);

    private RandomAccessFile file ;

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
        initIndexData();
        this.start();
    }

    public int searchFileIndex(long index) {
        int fileIndex = -1;
        SortedMap<Integer, Index> indexMap = fileIndexMap.tailMap(0);
        Iterator<Integer> it = indexMap.keySet().iterator();

        while (it.hasNext()) {
            Integer tmpIndex = it.next();
            Index fileIndexBeginEnd = indexMap.get(tmpIndex);
            if (index >= fileIndexBeginEnd.getStart() && index <= fileIndexBeginEnd.getEnd()) {
                fileIndex = tmpIndex;
                break;
            }
        }
        return fileIndex;
    }

    public Index getFileDataInterval(int fileIndex) {
        return fileIndexMap.get(fileIndex);
    }

    public void updateIndex(int fileNo, long start, long end) {
        Index index = constructIndex(fileNo, start, end);
        //update file index to disk
        if (index != null) {
            updateIndex2disk(index);
        }

    }

    private Index constructIndex(int fileNo, long start, long end) {
        Index index = null;
        if (!fileIndexMap.containsKey(fileNo)) {
            index = new Index(fileNo, start, end);
            fileIndexMap.put(fileNo, index);
        } else {
            index = fileIndexMap.get(fileNo);
            if (index.getEnd() < end) {
                index.setEnd(end);
            }
        }
        return index;
    }

    public void updateIndex2disk(Index index) {
        try {

            ByteBuffer bb = ByteBuffer.allocate(Index.INDEX_HEADER_SIZE);
            FileChannel fc = file.getChannel();

            if (file.length() == 0) {
                index.convert4Bytes(bb);
                fc.write(bb);
                bb.clear();
                return;
            }

            fc.position(file.length() - Index.INDEX_HEADER_SIZE);
            bb.clear();

            if (fc.read(bb) > 0) {
                //ignore index magic
                bb.flip();
                bb.get();

                int fileNo1 = bb.getInt();
                long start1 = bb.getLong();
                long end1 = bb.getLong();

                if (fileNo1 < index.getFileNo()) {
                    fc.position(file.length());
                } else if (fileNo1 == index.getFileNo()) {
                    fc.position(file.length() - Index.INDEX_HEADER_SIZE);
                } else {
                    throw new RuntimeException("wrong index file no:" + index.getFileNo());
                }

                bb.clear();
                index.convert4Bytes(bb);
                fc.write(bb);
                bb.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initIndexData() {
        try {
            File indexFileDir = new File(indexDir);
            if (!indexFileDir.exists()) {
                indexFileDir.mkdirs();
            }

            if (indexFile == null) {
                indexFile = new File(indexDir + "/" + LOG_FILE_SUFFIX);
                if (!indexFile.exists()) {
                    indexFile.createNewFile();
                }
            }

            file = new RandomAccessFile(indexFile, "rw");
            if (indexFile.length() != 0) {
                FileChannel fc = file.getChannel();
                ByteBuffer bb = ByteBuffer.allocate(Index.INDEX_HEADER_SIZE);

                while (fc.read(bb) > 0) {
                    //ignore index magic
                    bb.flip();
                    bb.get();
                    int fileNo = bb.getInt();
                    long start = bb.getLong();
                    long end = bb.getLong();

                    constructIndex(fileNo, start, end);
                    bb.clear();
                }
            }

            if (fileIndexMap.size() > 0) {
                lastFileIndex = fileIndexMap.lastKey();
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("read index fail:", e);
            System.exit(0);
        } finally {
            // ignore file close
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
                updateIndex(fileNo, start, end);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void changeFile() {
        lastFileIndex++;
    }

    public long getLastFileIndex() {
        return lastFileIndex;
    }

    public Index getLastIndex() {
        if (fileIndexMap.size() == 0)
            return null;
        return fileIndexMap.lastEntry().getValue();
    }

}
