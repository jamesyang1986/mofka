package com.kole.mofka.data;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Autor: jianjunyang
 * Date:17/1/16
 */
public class MofkaData {
    private byte[] data;
    private static MessageDigest md = null;

    public MofkaData(byte[] data) {
        this.data = data;
    }

    static {
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public int length() {
        return (1 + 4 + 4 + data.length);
    }

    public byte[] dump() {
        ByteBuffer bb = ByteBuffer.allocate(1 + 4 + 4 + data.length);

        //magic number
        byte b = (byte) 0x3f;
        bb.put(b);

        //set checksum of data
        bb.putInt(genCheckSum(data));

        //set data length
        bb.putInt(data.length);

        //put the raw data
        bb.put(data);
        return bb.array();
    }


    private static int genCheckSum(byte[] data) {
        md.update(data);
        BigInteger tmp = new BigInteger(1, md.digest());
        return tmp.intValue();
    }

}
