package com.aref.jdb.btree;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.aref.jdb.btree.BTree.HEADER;


public class BNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    private static final String INDEX_OUT_OF_BOUNDS = "Index out of bounds!";

    public static final int BNODE_NODE = 1;
    public static final int BNODE_LEAF = 2;

    public byte[] data;

    public BNode() {
    }

    public BNode(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int bType() {
        return ByteBuffer.wrap(data, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
    }

    public int nKeys() {
        return ByteBuffer.wrap(data, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
    }

    public void setHeader(int bType, int nKeys) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(0, (short) bType);
        buffer.putShort(2, (short) nKeys);
    }

    public long getPtr(int idx) {
        assert idx < nKeys() : INDEX_OUT_OF_BOUNDS;
        int pos = HEADER + 8 * idx;
        return ByteBuffer.wrap(data, pos, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public void setPtr(int idx, long val) {
        assert idx < nKeys() : INDEX_OUT_OF_BOUNDS;
        int pos = HEADER + 8 * idx;
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(pos, val);
    }

    private int offsetPos(int idx) {
        assert (1 <= idx && idx <= nKeys()) : INDEX_OUT_OF_BOUNDS;
        return HEADER + 8 * nKeys() + 2 * (idx - 1);
    }

    public int getOffset(int idx) {
        if (idx == 0) {
            return 0;
        }
        return ByteBuffer.wrap(data, offsetPos(idx), 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
    }

    public void setOffset(int idx, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(offsetPos(idx), (short) offset);
    }

    public int kvPos(int idx) {
        assert (idx <= nKeys()) : INDEX_OUT_OF_BOUNDS;
        return HEADER + 8 * nKeys() + 2 * nKeys() + getOffset(idx);
    }

    byte[] getKey(int idx) {
        assert (idx < nKeys()) : INDEX_OUT_OF_BOUNDS;
        int pos = kvPos(idx);
        int kLen = ByteBuffer.wrap(data, pos, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        byte[] key = new byte[kLen];
        System.arraycopy(data, pos + 4, key, 0, kLen);
        return key;
    }

    private byte[] getVal(int idx) {
        assert (idx < nKeys()) : INDEX_OUT_OF_BOUNDS;
        int pos = kvPos(idx);
        int kLen = ByteBuffer.wrap(data, pos, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        int vLen = ByteBuffer.wrap(data, pos + 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        byte[] value = new byte[vLen];
        System.arraycopy(data, pos + 4 + kLen, value, 0, vLen);
        return value;
    }

    public int nBytes() {
        return kvPos(nKeys());
    }



}