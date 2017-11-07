package com.immomo.mofka.log;


import com.kole.mofka.bootstrap.MofkaServer;
import com.kole.mofka.conf.MofkaConf;
import com.kole.mofka.data.MofkaData;
import com.kole.mofka.log.DataLog;
import junit.framework.TestCase;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DataLogTest extends TestCase {
    private DataLog log;

    public void setUp() throws Exception {
        super.setUp();
        System.out.println("start to mofka server....");
        MofkaConf conf = new MofkaConf();
        conf.setDataDir("/tmp/mofka/data");
        conf.setIndexDir("/tmp/mofka/index");

        MofkaServer server = new MofkaServer(conf);
        log = new DataLog(conf);
    }

    public void tearDown() throws Exception {

    }

    public void testPutData() throws Exception {
        String testDataPrefix = "test----sadfasdf:";
        int i = 99;
        long index = log.putData(new MofkaData((testDataPrefix + i).getBytes()));
        assertEquals(testDataPrefix + i, new String(log.readData(index)));
    }

    public void testReadData() throws Exception {
        ExecutorService es = new ThreadPoolExecutor(5, 20, 200, MILLISECONDS, new LinkedBlockingDeque<Runnable>());

        int loop = 1024 * 1024 * 1024;
        final CountDownLatch latch = new CountDownLatch(loop);
        final StringBuilder sb = new StringBuilder("");

        for (int i = 0; i < loop; i++) {
            final int j = i;
            es.submit(new Runnable() {
                @Override
                public void run() {
                    String testDataPrefix = "test----sadfasdf:";
                    long index = log.putData(new MofkaData((testDataPrefix + j).getBytes()));
                    boolean ifEqual = (testDataPrefix + j).equals(new String(log.readData(index)));
                    synchronized (this) {
                        sb.append(ifEqual);
                    }
                    latch.countDown();
                }
            });
        }

        latch.await();
        System.out.println(sb.toString());
        assertEquals(false, sb.toString().contains("false"));
    }

    public void testasyncSaveMsg() throws Exception {
        long t1 = System.currentTimeMillis();
        int msgSize = 20000000;
        for (int i = 0; i < msgSize; i++) {
            String data = "prefix:" + i;
            log.asyncSaveMsg(new MofkaData(data.getBytes()));
        }

        long t2 = System.currentTimeMillis();

        System.out.println("finish to async save msg, it cost:" + (t2 - t1));

        Thread.currentThread().sleep(100000);

        for (int i = 0; i < msgSize; i++) {
            String data = "prefix:" + i;
            assertEquals(data, new String(log.readData(i)));
        }

    }
}