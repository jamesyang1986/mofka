package com.immomo.mofka.bootstrap;

import com.immomo.mofka.conf.MofkaConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Autor: jianjunyang
 * Date:17/1/16
 */
public class MofkaServer {
    private MofkaConf conf;

    private static Logger LOG = LoggerFactory.getLogger(MofkaServer.class);

    public MofkaServer(MofkaConf conf) {
        this.conf = conf;
    }

    public void run() {
        LOG.info("start to run mofka server the port is:" + conf.getPort());

    }

}
