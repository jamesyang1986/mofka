package com.kole.mofka.bootstrap;

import com.kole.mofka.conf.MofkaConf;
import com.kole.mofka.data.MofkaData;
import com.kole.mofka.log.DataLog;
import com.kole.mofka.net.ServerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Autor: jianjunyang
 * Date:17/1/16
 */
public class Bootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        LOG.info("start to mofka server....");
        MofkaConf conf = new MofkaConf();
        conf.setDataDir("/tmp/mofka/data");
        conf.setIndexDir("/tmp/mofka/index");

        int defaultPort = 9090;
        if (args.length > 0) {
            try {
                defaultPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                LOG.error("fail to use the port", args[0]);
                //ignore...
            }
        }

        ServerBootstrap bootstrap = new ServerBootstrap(defaultPort);

//        MofkaServer server = new MofkaServer(conf);
//        DataLog log = new DataLog(conf);
//
////        testSendData(log, conf);
//        System.out.println(new String(log.readData(8999L)));
    }

    private static void testSendData(DataLog log, MofkaConf conf) {
        String str = "sadfwerqwrwqerwerweqrweqrrsasadfczvsdf:";
        for (int i = 0; i < 1000000; i++) {
            byte[] strData = (str + i).getBytes();
            MofkaData data = new MofkaData(strData);
            log.putData(data);
        }
        System.out.println("finish to test send data...");
    }

}
