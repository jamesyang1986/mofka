package com.kole.mofka.net;

import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by yang.jianjun on 2017/10/31.
 */
public class ClientBootStrap {

    private static Logger LOG = LoggerFactory.getLogger(ClientBootStrap.class);

    public static void main(String[] args) {
        String ip = "127.0.0.1";
        Integer port = 9090;

        LOG.info("start to connect the server {}  {} ", ip, port);

        try {
            SocketChannel channel = SocketChannel.open();
            channel.socket().setReuseAddress(false);
            channel.socket().connect(new InetSocketAddress(ip, port));

            ByteBuffer sendHeaderBuffer = ByteBuffer.allocateDirect(4 + 4 + 4);
            ByteBuffer receiveHeaderBuffer = ByteBuffer.allocateDirect(4 + 4 + 4);

            for (int i = 0; i < 5; i++) {
                sendMsg(channel, sendHeaderBuffer, i);

                receiveHeaderBuffer.clear();
                int rc = channel.read(receiveHeaderBuffer);
                //TODO  need Fix
                if (rc < 0) {
                    throw new RuntimeException("wrong to read the msg header");
                }

                if (receiveHeaderBuffer.remaining() == 0) {
                    receiveHeaderBuffer.flip();
                    int magic = receiveHeaderBuffer.getInt();
                    int crc = receiveHeaderBuffer.getInt();
                    int len = receiveHeaderBuffer.getInt();

                    ByteBuffer body = ByteBuffer.allocate(len);
                    while (body.hasRemaining()) {
                        channel.read(body);
                    }

                    System.out.println(new String(body.array()));
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendMsg(SocketChannel channel, ByteBuffer buffer, int i) throws IOException {
        String msg = "sadfsdafas7777777d" + i;
        buffer.putInt(0xCAFF);
        buffer.putInt(33);
        byte[] datas = msg.getBytes();
        buffer.putInt(datas.length);
        buffer.flip();
        channel.write(buffer);
        channel.write(ByteBuffer.wrap(datas));
        buffer.clear();
    }
}
