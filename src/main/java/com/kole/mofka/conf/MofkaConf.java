package com.kole.mofka.conf;

/**
 * Autor: jianjunyang
 * Date:17/1/16
 */
public class MofkaConf {
    private int port = 9097;
    private int segmentMaxSize = 32 * 1024 * 1024;
    private int indexMaxSize = 10 * 1024 * 1024;
    private String dataDir;
    private String indexDir;

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getIndexDir() {
        return indexDir;
    }

    public void setIndexDir(String indexDir) {
        this.indexDir = indexDir;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    private int serverId;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getSegmentMaxSize() {
        return segmentMaxSize;
    }

    public void setSegmentMaxSize(int segmentMaxSize) {
        this.segmentMaxSize = segmentMaxSize;
    }

    public int getIndexMaxSize() {
        return indexMaxSize;
    }

    public void setIndexMaxSize(int indexMaxSize) {
        this.indexMaxSize = indexMaxSize;
    }


    /**
     * load conf from confPath
     *
     * @param confPath
     * @return MofkaConf
     */
    public static MofkaConf load(String confPath) {
        return new MofkaConf();
    }

}
