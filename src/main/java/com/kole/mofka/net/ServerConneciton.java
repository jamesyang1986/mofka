package com.kole.mofka.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by yang.jianjun on 2017/10/20.
 */
public class ServerConneciton {
    //connection key for io
    private SelectionKey key;

    //socke channal for one conn
    private SocketChannel sock;

    private ByteBuffer headerBuffer = ByteBuffer.allocate(4 + 4 + 4);

    private ByteBuffer respHeaderBuffer = ByteBuffer.allocate(4 + 4 + 4);

    private ByteBuffer incomingBuffer = headerBuffer;

    // outgoing buffer for channel write
    private LinkedBlockingDeque<ByteBuffer> outingBuffers = new LinkedBlockingDeque<ByteBuffer>();

    private static Logger LOG = LoggerFactory.getLogger(ServerConneciton.class);

    public ServerConneciton(SelectionKey key, SocketChannel sock) throws IOException {
        this.key = key;
        this.sock = sock;
        sock.socket().setTcpNoDelay(true);
        key.interestOps(key.readyOps() | SelectionKey.OP_READ);
    }


    public void doIO(SelectionKey key) {
        try {
            if (key.isReadable()) {
                int read = sock.read(incomingBuffer);
                if (read < 0) {
                    throw new RuntimeException("read end to the socket...");
                }

                if (incomingBuffer.remaining() == 0) {
                    if (incomingBuffer == headerBuffer) {
                        headerBuffer.flip();
                        int magic = headerBuffer.getInt();
                        int crc = headerBuffer.getInt();
                        int size = headerBuffer.getInt();
                        headerBuffer.clear();
                        incomingBuffer = ByteBuffer.allocate(size);
                    }
                    readPayload();
                }

            } else if (key.isWritable()) {

                System.out.println("write io events is ready...");
            } else {
                throw new IllegalStateException("wrong state for connection..");
            }
        } catch (Exception e) {
            LOG.error("error for io :", e);
            this.close();
        }

    }

    private void readPayload() throws IOException {
        if (incomingBuffer.remaining() != 0) {
            int rc = sock.read(incomingBuffer);
            if (rc < 0) {
                throw new RuntimeException(" end of sock stream");
            }
        }

        if (incomingBuffer.remaining() == 0) {
            incomingBuffer.flip();
            byte[] body = incomingBuffer.array();
            System.out.println("receive msg :" + new String(body));

            writeResp(body);

            //read incomming buffer
            headerBuffer.clear();
            incomingBuffer = headerBuffer;
        }
    }


    private void writeResp(byte[] body) throws IOException {
        respHeaderBuffer.clear();
        respHeaderBuffer.putInt(44);
        respHeaderBuffer.putInt(55);


        String contents = "{\n" +
                "            \"type\": \" " + System.currentTimeMillis() + "\",\n" +
                "            \"goods_id\": \"20010020\",\n" +
                "            \"num\": \"1\"\n" +
                "            \"contents\": \" " + new String(body)
                + "        }";
        respHeaderBuffer.putInt(contents.getBytes().length);


        int len = contents.getBytes().length;
        respHeaderBuffer.putInt(len);
        respHeaderBuffer.flip();
        sock.write(respHeaderBuffer);
        respHeaderBuffer.clear();

        ByteBuffer bb = ByteBuffer.wrap(contents.getBytes());
        while (bb.hasRemaining()) {
            int rc = sock.write(bb);
            if (rc < len) {
                System.out.println(" the package is truncated...." + rc + " real len is:" + len);
            }
        }

    }


    public void close() {
        try {
            sock.finishConnect();
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
