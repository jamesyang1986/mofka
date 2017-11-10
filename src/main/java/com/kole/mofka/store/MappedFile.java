package com.kole.mofka.store;

import com.kole.mofka.data.MofkaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by yang.jianjun on 2017/10/16.
 */
public class MappedFile {
    private String fileName;
    private long fileSize;
    private File file;
    private FileChannel fileChannel;
    private MappedByteBuffer mappedByteBuffer;

    private Logger logger = LoggerFactory.getLogger(MappedFile.class);

    public MappedFile(String fileName, long fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;

        try {
            file = new File(fileName);
            fileChannel = new RandomAccessFile(file, "rw").getChannel();
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE,
                    0, fileSize);
        } catch (FileNotFoundException e) {
            logger.error("file not found " + fileName, e);
        } catch (IOException e) {
            logger.error("io error for read file " + fileName, e);
        }

    }

    public void saveMsg(MofkaData data) {
        ByteBuffer buffer = this.mappedByteBuffer.slice();
        buffer.put(data.dump());
    }

}
