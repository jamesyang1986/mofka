package com.kole.mofka.conf;

/**
 * Created by yang.jianjun on 2017/10/19.
 */
public class InternalConf {
    //set socket receive buffer size to 4M
    public static final int SOCKET_RECEIVE_BUFFER_SIZE = 4 * 1024 * 1024;

    //set socket send buffer size to 4M
    public static final int SOCKET_SEND_BUFFER_SIZE = 4 * 1024 * 1024;


    public static final int selector_max_select_miniseconds = 1000;



}
