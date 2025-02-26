package com.aref.jdb.btree;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static com.aref.jdb.btree.BNode.*;

public abstract class BTree {

    private long root;


    public static int HEADER = 4;
    public static int BTREE_PAGE_SIZE = 4096;
    public static int BTREE_MAX_KEY_SIZE = 1000;
    public static int BTREE_MAX_VAL_SIZE = 3000;

    public abstract BNode get(long idx);

    public abstract Long newNode(BNode node);

    public abstract void del(long idx);


    public boolean delete(byte[] key) {
        if (key.length == 0 || key.length > BTREE_MAX_KEY_SIZE) {
            throw new IllegalArgumentException("Invalid key length");
        }

        if (root == 0) {
            return false;
        }

        BNode updated = treeDelete(this, get(root), key);

        if (updated.getData() == null || updated.getData().length == 0) {
            return false;
        }

        del(root);

        if (updated.bType() == BNODE_NODE && updated.nKeys() == 1) {
            root = updated.getPtr(0);
        } else {
            root = newNode(updated);
        }

        return true;
    }

    public void insert(byte[] key, byte[] val) {
        if (key.length == 0 || key.length > BTREE_MAX_KEY_SIZE) {
            throw new IllegalArgumentException("Invalid key length");
        }
        if (val.length > BTREE_MAX_VAL_SIZE) {
            throw new IllegalArgumentException("Invalid value length");
        }

        if (root == 0) {
            BNode root = new BNode(new byte[BTREE_PAGE_SIZE]);
            root.setHeader(BNODE_LEAF, 2);
            nodeAppendKV(root, 0, 0, null, null);
            nodeAppendKV(root, 1, 0, key, val);
            this.root = newNode(root);
            return;
        }

        BNode node = get(root);
        del(root);

        node = treeInsert(this, node, key, val);

        SplitResult splitResult = nodeSplit3(node);
        int nSplit = splitResult.count;
        BNode[] split = splitResult.nodes;
        if (nSplit > 1) {
            BNode root = new BNode(new byte[BTREE_PAGE_SIZE]);
            root.setHeader(BNODE_NODE, nSplit);
            for (int i = 0; i < nSplit; i++) {
                BNode kNode = split[i];
                long ptr = newNode(kNode);
                byte[] nodeKey = kNode.getKey(0);
                nodeAppendKV(root, (short) i, ptr, nodeKey, null);
            }
            this.root = newNode(root);
        } else {
            this.root = newNode(split[0]);
        }
    }

    public void nodeAppendKV(BNode newNode, int idx, long ptr, byte[] key, byte[] val) {
        newNode.setPtr(idx, ptr);

        int pos = newNode.kvPos(idx);

        if (key != null)
            ByteBuffer.wrap(newNode.data, pos, 2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) key.length);

        if (val != null)
            ByteBuffer.wrap(newNode.data, pos + 2, 2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) val.length);

        if (key != null)
            System.arraycopy(key, 0, newNode.data, pos + 4, key.length);
        if (val != null && key != null)
            System.arraycopy(val, 0, newNode.data, pos + 4 + key.length, val.length);

        if (key != null && val != null)
            newNode.setOffset(idx + 1, newNode.getOffset(idx) + 4 + key.length + val.length);
    }

    private void leafInsert(BNode newNode, BNode oldNode, int idx, byte[] key, byte[] val) {
        newNode.setHeader(BNODE_LEAF, oldNode.nKeys() + 1);
        nodeAppendRange(newNode, oldNode, 0, 0, idx);
        nodeAppendKV(newNode, idx, 0, key, val);
        nodeAppendRange(newNode, oldNode, idx + 1, idx, oldNode.nKeys() - idx);
    }

    private void leafUpdate(BNode newNode, BNode oldNode, int idx, byte[] key, byte[] val) {
        newNode.setHeader(BNODE_LEAF, oldNode.nKeys() + 1);
        nodeAppendRange(newNode, oldNode, 0, 0, idx);
        nodeAppendKV(newNode, idx, 0, key, val);
        nodeAppendRange(newNode, oldNode, idx + 1, idx, oldNode.nKeys() - idx);
    }

    private void nodeReplaceKidN(
            BTree tree, BNode newNode, BNode oldNode, int idx, BNode... kids) {

        int inc = kids.length;
        newNode.setHeader(BNODE_NODE, oldNode.nKeys() + inc - 1);

        nodeAppendRange(newNode, oldNode, 0, 0, idx);

        for (int i = 0; i < kids.length; i++) {
            BNode node = kids[i];
            nodeAppendKV(newNode, idx + i, tree.newNode(node), node.getKey(0), null);
        }

        nodeAppendRange(newNode, oldNode, idx + inc, idx + 1, oldNode.nKeys() - (idx + 1));
    }

    private SplitResult nodeSplit3(BNode oldNode) {
        final int PAGE_SIZE = BTREE_PAGE_SIZE;

        if (oldNode.nBytes() <= PAGE_SIZE) {
            oldNode.setData(Arrays.copyOf(oldNode.getData(), PAGE_SIZE));
            return new SplitResult(1, new BNode[]{oldNode});
        }

        BNode left = new BNode(new byte[2 * PAGE_SIZE]);
        BNode right = new BNode(new byte[PAGE_SIZE]);
        nodeSplit2(left, right, oldNode);

        if (left.nBytes() <= PAGE_SIZE) {
            left.setData(Arrays.copyOf(left.getData(), PAGE_SIZE));
            return new SplitResult(2, new BNode[]{left, right});
        }

        BNode leftLeft = new BNode(new byte[PAGE_SIZE]);
        BNode middle = new BNode(new byte[PAGE_SIZE]);
        nodeSplit2(leftLeft, middle, left);

        assert leftLeft.nBytes() <= PAGE_SIZE;
        return new SplitResult(3, new BNode[]{leftLeft, middle, right});
    }

    private void nodeSplit2(BNode left, BNode right, BNode old) {
        int splitIndex = old.nKeys() / 2;

        left.setHeader(BNode.BNODE_LEAF, splitIndex);
        right.setHeader(BNode.BNODE_LEAF, old.nKeys() - splitIndex);

        nodeAppendRange(left, old, 0, 0, splitIndex);
        nodeAppendRange(right, old, splitIndex, 0, old.nKeys() - splitIndex);
    }


    public void nodeAppendRange(BNode newNode, BNode oldNode, int dstNew, int srcOld, int n) {
        assert (srcOld + n <= oldNode.nKeys());
        assert (dstNew + n <= newNode.nKeys());

        if (n == 0) {
            return;
        }

        for (int i = 0; i < n; i++) {
            newNode.setPtr(dstNew + i, oldNode.getPtr(srcOld + i));
        }

        int dstBegin = newNode.getOffset(dstNew);
        int srcBegin = oldNode.getOffset(srcOld);
        for (int i = 1; i <= n; i++) {
            int offset = dstBegin + oldNode.getOffset(srcOld + i) - srcBegin;
            newNode.setOffset(dstNew + i, offset);
        }

        int begin = oldNode.kvPos(srcOld);
        int end = oldNode.kvPos(srcOld + n);
        System.arraycopy(oldNode.data, begin, newNode.data, newNode.kvPos(dstNew), end - begin);
    }

    private int nodeLookupLE(BNode node, byte[] key) {
        int nKeys = node.nKeys();
        int found = 0;

        for (int i = 1; i < nKeys; i++) {
            int cmp = Arrays.compare(node.getKey(i), key);
            if (cmp <= 0) {
                found = i;
            }
            if (cmp >= 0) {
                break;
            }
        }
        return found;
    }

    public BNode treeInsert(BTree tree, BNode node, byte[] key, byte[] val) {
        BNode newNode = new BNode(new byte[2 * BTREE_PAGE_SIZE]);

        int idx = nodeLookupLE(node, key);

        switch (node.bType()) {
            case BNODE_LEAF:
                if (Arrays.equals(key, node.getKey(idx))) {
                    leafUpdate(newNode, node, idx, key, val);
                } else {
                    leafInsert(newNode, node, idx + 1, key, val);
                }
                break;

            case BNODE_NODE:
                nodeInsert(tree, newNode, node, idx, key, val);
                break;

            default:
                throw new IllegalStateException("Invalid node type!");
        }

        return newNode;
    }

    private void nodeInsert(BTree tree, BNode newNode, BNode node, int idx, byte[] key, byte[] val) {
        long kPtr = node.getPtr(idx);
        BNode knode = tree.get(kPtr);
        tree.del(kPtr);

        knode = treeInsert(tree, knode, key, val);

        SplitResult splitResult = nodeSplit3(knode);
        BNode[] splitNodes = splitResult.nodes;
        int nSplit = splitNodes.length;

        nodeReplaceKidN(tree, newNode, node, idx, Arrays.asList(splitNodes).subList(0, nSplit).toArray(new BNode[]{}));
    }


    private void leafDelete(BNode newNode, BNode oldNode, int idx) {
        newNode.setHeader(BNODE_LEAF, oldNode.nKeys() - 1);
        nodeAppendRange(newNode, oldNode, 0, 0, idx);
        nodeAppendRange(newNode, oldNode, idx, idx + 1, oldNode.nKeys() - (idx + 1));
    }

    private BNode treeDelete(BTree tree, BNode node, byte[] key) {
        int idx = nodeLookupLE(node, key);

        switch (node.bType()) {
            case BNODE_LEAF:
                if (!Arrays.equals(key, node.getKey(idx))) {
                    return new BNode();
                }

                BNode newNode = new BNode();
                newNode.setData(new byte[BTree.BTREE_PAGE_SIZE]);
                leafDelete(newNode, node, idx);
                return newNode;

            case BNODE_NODE:
                return nodeDelete(tree, node, idx, key);

            default:
                throw new IllegalArgumentException("Bad node type");
        }
    }

    private BNode nodeDelete(BTree tree, BNode node, int idx, byte[] key) {
        long kptr = node.getPtr(idx);
        BNode updated = treeDelete(tree, tree.get(kptr), key);

        if (updated.getData().length == 0) {
            return new BNode();
        }

        tree.del(kptr);

        BNode newNode = new BNode();
        newNode.setData(new byte[BTree.BTREE_PAGE_SIZE]);

        ShouldMergeResult shouldMergeResult = shouldMerge(tree, node, idx, updated);
        int mergeDir = shouldMergeResult.shouldMerge;
        BNode sibling;
        switch (mergeDir) {
            case -1:
                sibling = tree.get(node.getPtr(idx - 1));
                BNode mergedLeft = new BNode();
                mergedLeft.setData(new byte[BTree.BTREE_PAGE_SIZE]);
                nodeMerge(mergedLeft, sibling, updated);
                tree.del(node.getPtr(idx - 1));

                nodeReplace2Kid(newNode, node, idx - 1, tree.newNode(mergedLeft), mergedLeft.getKey(0));
                break;

            case 1:
                sibling = tree.get(node.getPtr(idx + 1));
                BNode mergedRight = new BNode();
                mergedRight.setData(new byte[BTree.BTREE_PAGE_SIZE]);
                nodeMerge(mergedRight, updated, sibling);
                tree.del(node.getPtr(idx + 1));

                nodeReplace2Kid(newNode, node, idx, tree.newNode(mergedRight), mergedRight.getKey(0));
                break;

            case 0:
                assert updated.nKeys() > 0;
                nodeReplaceKidN(tree, newNode, node, idx, updated);
                break;
        }

        return newNode;
    }

    private void nodeReplace2Kid(BNode newNode, BNode oldNode, int idx, Long mergedPtr, byte[] mergedKey) {
        int numKeys = oldNode.nKeys();

        assert idx >= 0 && idx < numKeys - 1;

        newNode.setHeader(BNode.BNODE_NODE, numKeys - 1);

        nodeAppendRange(newNode, oldNode, 0, 0, idx);

        nodeAppendKV(newNode, idx, mergedPtr, mergedKey, null);

        nodeAppendRange(newNode, oldNode, idx + 2, idx + 1, numKeys - (idx + 2));

    }

    private void nodeMerge(BNode newNode, BNode left, BNode right) {
        newNode.setHeader(left.bType(), left.nKeys() + right.nKeys());
        nodeAppendRange(newNode, left, 0, 0, left.nKeys());
        nodeAppendRange(newNode, right, left.nKeys(), 0, right.nKeys());
    }

    private ShouldMergeResult shouldMerge(BTree tree, BNode node, int idx, BNode updated) {
        if (updated.nBytes() > BTREE_PAGE_SIZE / 4) {
            return new ShouldMergeResult(0, new BNode());
        }

        if (idx > 0) {
            BNode sibling = tree.get(node.getPtr(idx - 1));
            int merged = sibling.nBytes() + updated.nBytes() - HEADER;
            if (merged <= BTREE_PAGE_SIZE) {
                return new ShouldMergeResult(-1, sibling);
            }
        }

        if (idx + 1 < node.nKeys()) {
            BNode sibling = tree.get(node.getPtr(idx + 1));
            int merged = sibling.nBytes() + updated.nBytes() - HEADER;
            if (merged <= BTREE_PAGE_SIZE) {
                return new ShouldMergeResult(1, sibling);
            }
        }

        return new ShouldMergeResult(0, new BNode());
    }


    public static class ShouldMergeResult {
        public final int shouldMerge;
        public final BNode node;

        public ShouldMergeResult(int count, BNode node) {
            this.shouldMerge = count;
            this.node = node;
        }
    }

    public static class SplitResult {
        public final int count;
        public final BNode[] nodes;

        public SplitResult(int count, BNode[] nodes) {
            this.count = count;
            this.nodes = nodes;
        }
    }

}
