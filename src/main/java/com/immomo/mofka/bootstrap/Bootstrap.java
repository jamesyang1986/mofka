package com.immomo.mofka.bootstrap;

import com.immomo.mofka.conf.MofkaConf;
import com.immomo.mofka.data.MofkaData;
import com.immomo.mofka.log.DataLog;
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

        MofkaServer server = new MofkaServer(conf);
        DataLog log = new DataLog(conf);

//        testSendData(log, conf);
        System.out.println(new String(log.readData(8999L)));
    }

    private static void testSendData(DataLog log, MofkaConf conf) {
        String str = "sadfwerqwrqwrweqrwqerwerweqrweqrrsasadfczvsdf:";

        for (int i = 0; i < 1000000; i++) {
            byte[] strData = (str + i).getBytes();
            MofkaData data = new MofkaData(strData);
            log.putData(data);
        }
        System.out.println("finish to test send data...");
    }

}
