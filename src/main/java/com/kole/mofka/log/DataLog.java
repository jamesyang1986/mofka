package com.kole.mofka.log;

import com.kole.mofka.conf.MofkaConf;
import com.kole.mofka.data.MofkaData;
import com.kole.mofka.exception.MsgQueueFullException;
import com.kole.mofka.index.Index;
import com.kole.mofka.monitor.MofkaMonitor;
import com.kole.mofka.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Autor: jianjunyang
 * Date:17/1/16
 */
public class DataLog extends Thread {

    //    private AtomicInteger curIndex = new AtomicInteger(0);
    private MofkaConf conf;
    private RandomAccessFile curFile;
    private FileChannel curFileChannel;
    private Logger LOG = LoggerFactory.getLogger(DataLog.class);

    private String dataDir;
    private AtomicLong dataNum = new AtomicLong(0L);
    private IndexLog indexInstance;
    private LinkedBlockingQueue<MofkaData> dataQueue = new LinkedBlockingQueue<MofkaData>(Constants.DATA_QUEUE_MAX_SIEZ);

    public DataLog(MofkaConf conf) {
        this.conf = conf;
        dataDir = conf.getDataDir();
        indexInstance = IndexLog.getLogInstance(conf);
        dataNum = new AtomicLong(indexInstance.getLastIndex() == null ?
                0L : indexInstance.getLastIndex().getEnd());

        initDataFile(conf);
        this.start();
    }

    private void initDataFile(MofkaConf conf) {
        try {
            File dataFile = new File(dataDir);
            if (!dataFile.exists()) {
                dataFile.mkdirs();
            }
            curFile = getLastWriteFile();
            curFileChannel = getLastWriteFile().getChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RandomAccessFile getLastWriteFile() throws FileNotFoundException {
        return new RandomAccessFile(conf.getDataDir() + "/" + indexInstance.getLastFileIndex() + ".log", "rw");
    }

    public void asyncSaveMsg(MofkaData data) throws InterruptedException {
        if (!dataQueue.offer(data, 1000, TimeUnit.MILLISECONDS)) {
            LOG.error("fail to save msg, the queue is full:" + dataQueue.size());
            throw new MsgQueueFullException(" msg queue is full...");
        }
    }


    public synchronized long putData(MofkaData data) {
        try {
            long curPos = curFile.getFilePointer();
            if (curPos > conf.getSegmentMaxSize()
                    || (curPos + data.length() > conf.getSegmentMaxSize())) {
                indexInstance.changeFile();
                curFile.close();
                curFileChannel.close();
                curFile = new RandomAccessFile(conf.getDataDir() + "/" + indexInstance.getLastFileIndex() + ".log", "rw");
                curFileChannel = curFile.getChannel();
            }

            FileChannel channel = curFile.getChannel();

            channel.position(curFile.length());
            byte[] msg = data.dump();
            channel.write(ByteBuffer.wrap(msg));

            MofkaMonitor.nwrite.addAndGet(1);
            MofkaMonitor.byteIn.addAndGet(msg.length);

            //record the segment file index interval
            long dataIndex = dataNum.get();
            indexInstance.updateIndex((int) indexInstance.getLastFileIndex(), dataNum.get(), dataNum.addAndGet(1L));
            return dataIndex;
        } catch (Exception e) {
            LOG.error("fail to change data log ...", e);
        }

        return 0L;
    }

    public byte[] readData(long index) {
        int fileIndex = indexInstance.searchFileIndex(index);
        if (fileIndex == -1) {
            return null;
        }

        Index fileIndexBeginEnd = indexInstance.getFileDataInterval(fileIndex);

        RandomAccessFile curFile = null;
        try {
            curFile = new RandomAccessFile(conf.getDataDir() + "/" + fileIndex + ".log", "r");
            FileChannel curFileChannel = curFile.getChannel();

            for (long j = fileIndexBeginEnd.getStart(); j <= index && j < curFile.length(); j++) {
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
                    MofkaMonitor.nread.addAndGet(1);
                    return body.array();
                }

                curFileChannel.position(curFileChannel.position() + length);
            }

        } catch (Exception e) {
            if (curFile != null) {
                try {
                    curFile.close();
                } catch (IOException e1) {
                }
            }
        }

        return null;
    }


    /**
     * save msg to disk async
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                MofkaData msg = dataQueue.take();
                putData(msg);
            } catch (InterruptedException e) {
                LOG.error("thread is interrupt...die...");
                Thread.currentThread().interrupt();
            } catch (Exception e1) {
                LOG.error("save msg to disk error, ", e1);
            }
        }

    }
}
