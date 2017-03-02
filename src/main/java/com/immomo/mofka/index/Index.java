package com.immomo.mofka.index;

import java.nio.ByteBuffer;

/**
 * file index data defination
 * <p/>
 * autor: yang.jianjun@immomo.com
 * created Date:2017-03-01
 */
public class Index {
    public static final int INDEX_HEADER_SIZE = 1 + 4 + 8 + 8;
    private byte magic = (byte) 0xaf;
    private int fileNo;
    private long start;
    private long end;

    public Index(int fileNo, long start, long end) {
        this.fileNo = fileNo;
        this.start = start;
        this.end = end;
    }

    public byte getMagic() {
        return magic;
    }

    public void setMagic(byte magic) {
        this.magic = magic;
    }

    public int getFileNo() {
        return fileNo;
    }

    public void setFileNo(int fileNo) {
        this.fileNo = fileNo;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public ByteBuffer convert4Bytes() {
        ByteBuffer buffer = ByteBuffer.allocate(21);
        buffer.put(magic);
        buffer.putInt(fileNo);
        buffer.putLong(start);
        buffer.putLong(end);
        buffer.flip();
        return buffer;
    }
}
