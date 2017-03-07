package com.kole.mofka.monitor;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * autor: yang.jianjun@immomo.com
 * created Date:2017-03-02
 */
public class MofkaMonitor {
    public static AtomicInteger nread = new AtomicInteger(0);
    public static AtomicInteger nwrite = new AtomicInteger(0);
    public static AtomicInteger byteIn = new AtomicInteger(0);
    public static AtomicInteger byteOut = new AtomicInteger(0);

    private final static Logger LOG = Logger.getLogger("mofka-monitor");

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        es.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                LOG.info(getStatTitle());
                LOG.info(getStatContent());
                clearMonitorData();

            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static String getStatTitle() {

        return "time" + "\t\t\t" + "nread" + "\t" + "nwrite" + "\t" + "byteIn" + "\t" + "byteOut" + "\t" + "\n";
    }

    public static String getStatContent() {
        return dateFormat.format(new Date()) + "\t" +
                nread + "\t" + nwrite + "\t" + byteIn + "\t" + byteOut + "\t" + "\n";
    }

    public static void clearMonitorData() {
        nread.set(0);
        nwrite.set(0);
        byteIn.set(0);
        byteOut.set(0);
    }

}
