package com.aref.jdb.persist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

public class FileUtils {

    public static void saveData(String path, byte[] data) throws IOException {
        String tmpPath = path + ".tmp." + randomInt();
        File tmpFile = new File(tmpPath);

        if (!tmpFile.createNewFile()) {
            throw new IOException("Failed to create temporary file: " + tmpPath);
        }

        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
            fos.write(data);
            fos.flush();
        } catch (IOException e) {
            tmpFile.delete();
            throw e;
        }

        Files.move(tmpFile.toPath(), new File(path).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static int randomInt() {
        return ThreadLocalRandom.current().nextInt();
    }

}
