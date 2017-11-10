package com.kole.mofka.net;

import com.kole.mofka.conf.InternalConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by yang.jianjun on 2017/10/19.
 */
public class ServerBootstrap {
    private int listenPort;
    private static Selector selector;
    private ServerSocketChannel ss;
    private static Logger LOG = LoggerFactory.getLogger(ServerBootstrap.class);

    static {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            LOG.error("fail to open the io selector...", e);
        }
    }

    public ServerBootstrap(int listenPort) {
        this.listenPort = listenPort;

        try {
            //set the server socket options
            setServerSocketOptions();

            ss.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select(1000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();

                List<SelectionKey> selectedKeyList = new ArrayList<SelectionKey>(selectedKeys);
                Collections.shuffle(selectedKeyList);

                Iterator<SelectionKey> iterator = selectedKeyList.iterator();
                while (iterator.hasNext()) {
                    try {
                        SelectionKey key = iterator.next();
                        if ((key.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
                            SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
                            InetAddress addr = sc.socket().getInetAddress();
                            sc.socket().setSoLinger(false, -1);
                            LOG.info("receive connection from addr:" + sc.socket().getRemoteSocketAddress());
                            sc.configureBlocking(false);
                            SelectionKey sk = sc.register(selector, SelectionKey.OP_READ);
                            ServerConneciton conn = new ServerConneciton(sk, sc);
                            sk.attach(conn);
                        } else if ((key.readyOps() & (SelectionKey.OP_READ | SelectionKey.OP_WRITE)) != 0) {
                            ServerConneciton conn = (ServerConneciton) key.attachment();
                            conn.doIO(key);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                selectedKeys.clear();
            }

        } catch (IOException e) {
            LOG.error("error to open selector,", e);
        }
    }


    private void setServerSocketOptions() throws IOException {
        this.ss = ServerSocketChannel.open();
        ss.socket().bind(new InetSocketAddress(this.listenPort));
        ss.configureBlocking(false);
        ss.socket().setSoTimeout(1000 * 2);
        ss.socket().setReceiveBufferSize(InternalConf.SOCKET_RECEIVE_BUFFER_SIZE);
        ss.socket().setReuseAddress(false);
    }

}
