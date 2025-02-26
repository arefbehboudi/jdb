package com.aref.jdb.btree;

import java.util.HashMap;
import java.util.Map;

public class SimpleBTree extends BTree {

    private final Map<String, String> ref = new HashMap<>();
    private final Map<Long, BNode> pages = new HashMap<>();

    @Override
    public BNode get(long idx) {
        return pages.get(idx);
    }

    @Override
    public Long newNode(BNode node) {
        if (node.nBytes() > BTREE_PAGE_SIZE) {
            throw new IllegalArgumentException("Node size exceeds page size.");
        }

        long key = System.identityHashCode(node);

        if (pages.containsKey(key)) {
            throw new IllegalStateException("Page already occupied");
        }

        pages.put(key, node);

        return key;
    }

    @Override
    public void del(long idx) {
        BNode bNode = pages.get(idx);
        assert bNode != null;
        pages.remove(idx);
    }

    public void add(String key, String val) {
        insert(key.getBytes(), val.getBytes());
        ref.put(key, val);
    }

    public boolean del(String key) {
        ref.remove(key);
        return delete(key.getBytes());
    }

    public Map<String, String> getRef() {
        return ref;
    }

    public Map<Long, BNode> getPages() {
        return pages;
    }
}
