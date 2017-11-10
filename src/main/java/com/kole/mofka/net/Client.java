package com.kole.mofka.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by yang.jianjun on 2017/11/8.
 */
public class Client {
    private static final int REQ_HEADER_SIZE = 4 + 4 + 4;
    private static final int RESP_HEADER_SIZE = 4 + 4 + 4;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket();
            socket.setKeepAlive(true);
            socket.setSoTimeout(2 * 1000);
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress("127.0.0.1", 9090));
            InputStream ins = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            ByteBuffer sendHeaderBuffer = ByteBuffer.allocate(REQ_HEADER_SIZE);
            ByteBuffer receiveHeaderBuffer = ByteBuffer.allocate(RESP_HEADER_SIZE);

            for (int i = 0; i < 500; i++) {
                System.out.println("--------------");
                String msg = "test===:" + i + ":" + System.currentTimeMillis();
                sendHeaderBuffer.clear();
                sendHeaderBuffer.putInt(11);
                sendHeaderBuffer.putInt(12);
                sendHeaderBuffer.putInt(msg.getBytes().length);

                byte[] data = new byte[REQ_HEADER_SIZE];

                sendHeaderBuffer.flip();
                sendHeaderBuffer.get(data, 0, data.length);
                out.write(data);
                out.write(msg.getBytes());
                out.flush();

                byte[] resp = new byte[RESP_HEADER_SIZE];
                ins.read(resp);
                receiveHeaderBuffer = ByteBuffer.wrap(resp);
                int crc = receiveHeaderBuffer.getInt();
                int id = receiveHeaderBuffer.getInt();
                int len = receiveHeaderBuffer.getInt();

                byte[] body = new byte[len];
                ins.read(body);
                System.out.println("the body is:" + new String(body));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
