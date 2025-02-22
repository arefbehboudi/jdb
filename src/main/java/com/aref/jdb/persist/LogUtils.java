package com.aref.jdb.persist;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class LogUtils {

    public static RandomAccessFile logCreate(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.createNewFile();
        }
        return new RandomAccessFile(file, "rw");
    }

    public static void logAppend(RandomAccessFile file, String line) throws IOException {
        file.seek(file.length());
        file.writeBytes(line + "\n");
        file.getFD().sync();
    }

}
