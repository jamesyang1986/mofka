package com.immomo.mofka.data;

/**
 * message header defination
 * Autor: jianjunyang
 * Date:17/1/16
 * <p/>
 * |--1byte(magic num)--|--4byte(checksum)--|--4byte(body length)--|
 */
public class MofkaHeader {
    private byte magic = (byte) 0xcc;
    private int checksum;
    private int len;

    public byte getMagic() {
        return magic;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }
}
