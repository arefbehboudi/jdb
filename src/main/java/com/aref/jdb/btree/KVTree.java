package com.aref.jdb.btree;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static com.aref.jdb.btree.BTree.BTREE_PAGE_SIZE;

public class KVTree {

    private String path;

    private RandomAccessFile file;

    private FileChannel fileChannel;

    private BTree tree;

    private MMap mmap;

    private Page page;

    public KVTree(String path) throws Exception {
        this.path = path;
        this.file = new RandomAccessFile(new File(path), "rw");
        this.fileChannel = file.getChannel();
        this.tree = new BTree();
        this.mmap = new MMap();
        this.page = new Page();
    }

    public void close() throws Exception {
        fileChannel.close();
        file.close();
    }

    public void extendMmap(int npages) throws IOException {
        long requiredSize = (long) npages * BTREE_PAGE_SIZE;

        if (mmap.totalSize >= requiredSize) {
            return;
        }

        long newSize = mmap.totalSize == 0 ? requiredSize : mmap.totalSize * 2;
        fileChannel.truncate(newSize);

        MappedByteBuffer chunk = fileChannel.map(FileChannel.MapMode.READ_WRITE, mmap.totalSize, newSize - mmap.totalSize);
        mmap.totalSize = newSize;
        mmap.chunks.add(chunk);
    }

    private class MMap {
        private long fileSize;
        private long totalSize;
        private List<MappedByteBuffer> chunks;

        public MMap() {
            this.fileSize = 0;
            this.totalSize = 0;
            this.chunks = new ArrayList<>();
        }
    }

    private class Page {
        private long flushedPages;
        private List<byte[]> tempPages;

        public Page() {
            this.flushedPages = 0;
            this.tempPages = new ArrayList<>();
        }
    }
}
