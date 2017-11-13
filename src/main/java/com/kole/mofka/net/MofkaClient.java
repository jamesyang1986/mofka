package com.kole.mofka.net;

import com.kole.mofka.data.Msg;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by yang.jianjun on 2017/11/8.
 */
public class MofkaClient {
    private static final int HIGH_WATER_WRITE_MARK = 50;
    private static final int LOW_WATER_READ_MARK = 50;
    private static final int BATCH_SEND_MAX_COUNT = 50;

    private static Selector selector;

    private String serverIp;
    private int serverPort;

    private SocketChannel sc;

    // save the outgoing msg to send
    private LinkedBlockingQueue<Msg> outgoingQueue = new LinkedBlockingQueue<Msg>(100000);

    //receive the comming data for parse
    private LinkedBlockingQueue<String> inCommingQueue = new LinkedBlockingQueue<String>();

    private ByteBuffer sendHeaderBuffer = ByteBuffer.allocateDirect(4 + 4 + 4);
    private ByteBuffer receiveHeaderBuffer = ByteBuffer.allocateDirect(4 + 4 + 4);

    private static Logger LOG = LoggerFactory.getLogger(MofkaClient.class);

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    private Thread sendThred = new Thread();

    private SocketChannel socketChannel;

    private static final CountDownLatch latch = new CountDownLatch(1);

    static {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            LOG.error("fail to open selector", e);
        }
    }

    public MofkaClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        try {
            socketChannel = createSock(serverIp, serverPort);
            sendThred = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        eventLoop();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            sendThred.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void eventLoop() throws IOException {
        while (true) {
            try {
                selector.select(1000);
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    SocketChannel sc = (SocketChannel) key.channel();

                    if (key.isConnectable()) {
                        LOG.info("start to connect the server.. ip:{}, port:{}" + serverIp, serverPort);
                        sc.finishConnect();
                        key.interestOps(key.interestOps() | (SelectionKey.OP_WRITE));
                    } else if (key.isReadable()) {
                        handleRead(sc, key);
                    } else if (key.isWritable()) {
                        handleWrite(sc, key);
                    }
                }
                keys.clear();
            } catch (Exception e) {
                LOG.error("fail to handle io event ....", e);
                e.printStackTrace();
            }
        }
    }

    private void handleWrite(SocketChannel sc, SelectionKey key) throws InterruptedException, IOException {
        Msg msg;
        int i = 0;
        while ((msg = outgoingQueue.poll(10, TimeUnit.MILLISECONDS)) != null
                && i < HIGH_WATER_WRITE_MARK) {
            String json = msg.toJson();
            sendMsg(sc, sendHeaderBuffer, json);
            i++;
        }

        if (outgoingQueue.size() == 0) {
            disableWrite(key);
            return;
        }

//        key.interestOps(key.interestOps() | SelectionKey.OP_READ);

        System.out.println("-------finish to send " + i + " msgs---------");

    }


    synchronized void enableWrite(SelectionKey sockKey) {
        int i = sockKey.interestOps();
        if ((i & SelectionKey.OP_WRITE) == 0) {
            sockKey.interestOps(i | SelectionKey.OP_WRITE);
        }
    }

    public synchronized void disableWrite(SelectionKey sockKey) {
        int i = sockKey.interestOps();
        if ((i & SelectionKey.OP_WRITE) != 0) {
            sockKey.interestOps(i & (~SelectionKey.OP_WRITE));
        }
    }

    synchronized private void enableRead(SelectionKey sockKey) {
        int i = sockKey.interestOps();
        if ((i & SelectionKey.OP_READ) == 0) {
            sockKey.interestOps(i | SelectionKey.OP_READ);
        }
    }

    synchronized void enableReadWriteOnly(SelectionKey sockKey) {
        sockKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }


    private void handleRead(SocketChannel sc, SelectionKey key) throws IOException {
        receiveHeaderBuffer.clear();

        int rc = sc.read(receiveHeaderBuffer);
        //TODO  need Fix
        if (rc <= 0) {
            key.interestOps(key.readyOps() | (~SelectionKey.OP_READ));
            return;
        }

        if (receiveHeaderBuffer.remaining() == 0) {
            receiveHeaderBuffer.flip();
            int magic = receiveHeaderBuffer.getInt();
            int crc = receiveHeaderBuffer.getInt();
            int len = receiveHeaderBuffer.getInt();

            ByteBuffer body = ByteBuffer.allocate(len);

            while (body.hasRemaining()) {
                sc.read(body);
            }

            System.out.println(new String(body.array()));

//            key.interestOps(key.interestOps() | (SelectionKey.OP_WRITE));
        }
    }

    private SocketChannel createSock(String serverIp, int serverPort) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.socket().setTcpNoDelay(true);
        sc.socket().setSoTimeout(2000);
        sc.socket().setSoLinger(false, -1);
        sc.register(selector, SelectionKey.OP_CONNECT);
        sc.connect(new InetSocketAddress(serverIp, serverPort));
        return sc;
    }

    private static void sendMsg(SocketChannel channel, ByteBuffer buffer, String msg) throws IOException {
        buffer.putInt(0xCAFF);
        buffer.putInt(33);
        byte[] datas = msg.getBytes();
        buffer.putInt(datas.length);
        buffer.flip();
        channel.write(buffer);
        channel.write(ByteBuffer.wrap(datas));
        buffer.clear();
    }

    public static void main(String[] args) {

        MofkaClient client = new MofkaClient("39.106.48.109", 9090);
        int start = (int) (System.currentTimeMillis() / 1000);

        for (int i = start; i < start + 5; i++) {
            Msg msg = new Msg("topic1888", "test:::" + i);
            msg.setCreateTime(System.currentTimeMillis());
            client.sendMsg(msg);
            if (i > 0 && i % 1000 == 0) {
                try {
                    Thread.currentThread().sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public boolean sendMsg(Msg msg) {
        try {
            outgoingQueue.put(msg);
            if (outgoingQueue.size() > 0) {
                selector.wakeup();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

}
