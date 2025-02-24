package com.aref.jdb.btree;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleBTreeTest {

    private SimpleBTree simpleBTree;

    @BeforeEach
    public void setUp() {
        simpleBTree = new SimpleBTree();
    }

    @Test
    public void testAdd() {
        String key = "apple";
        String value = "fruit";

        assertNull(simpleBTree.getRef().get(key));

        simpleBTree.add(key, value);

        assertEquals(value, simpleBTree.getRef().get(key));
    }

    @Test
    public void testDel() {
        String key = "banana";
        String value = "fruit";

        simpleBTree.add(key, value);

        assertEquals(value, simpleBTree.getRef().get(key));

        boolean result = simpleBTree.del(key);

        assertTrue(result);

        assertNull(simpleBTree.getRef().get(key));
    }

    @Test
    public void testDelNotFound() {
        String key = "orange";

        boolean result = simpleBTree.del(key);

        assertFalse(result);
    }

    @Test
    public void testDeleteCausingMerge() {
        SimpleBTree tree = new SimpleBTree();

        tree.add("A", "Alpha");
        tree.add("B", "Beta");
        tree.add("C", "Gamma");
        tree.add("D", "Delta");
        tree.add("E", "Epsilon");
        tree.add("F", "Zeta");

        boolean deleted = tree.del("D");

        assertTrue(deleted);
        //assertNull(tree.get("D"));
    }

    @Test
    public void testDeleteAllKeys() {
        SimpleBTree tree = new SimpleBTree();

        tree.add("1", "One");
        tree.add("2", "Two");
        tree.add("3", "Three");

        assertTrue(tree.del("1"));
        assertTrue(tree.del("2"));
        assertTrue(tree.del("3"));

/*        assertNull(tree.get("1"));
        assertNull(tree.get("2"));
        assertNull(tree.get("3"));*/
    }

    @Test
    public void testDeleteNonExistentKey() {
        SimpleBTree tree = new SimpleBTree();

        tree.add("X", "Xylophone");
        tree.add("Y", "Yam");

        boolean deleted = tree.del("Z");

        assertFalse(deleted);
    }

    @Test
    public void testDeleteInternalNode() {
        SimpleBTree tree = new SimpleBTree();

        tree.add("A", "Alpha");
        tree.add("B", "Beta");
        tree.add("C", "Gamma");
        tree.add("D", "Delta");
        tree.add("E", "Epsilon");

        boolean deleted = tree.del("C");

        assertTrue(deleted);
/*        assertNull(tree.get("C"));
        assertEquals("Alpha", tree.get("A"));
        assertEquals("Beta", tree.get("B"));
        assertEquals("Delta", tree.get("D"));
        assertEquals("Epsilon", tree.get("E"));*/
    }

    @Test
    public void testDeleteFromRoot() {
        SimpleBTree tree = new SimpleBTree();

        tree.add("P", "Peach");
        tree.add("Q", "Quince");

        boolean deleted = tree.del("P");

        assertTrue(deleted);
/*        assertNull(tree.get("P"));
        assertEquals("Quince", tree.get("Q"));*/
    }

    @Test
    public void testDeleteSingleNode() {
        SimpleBTree tree = new SimpleBTree();

        tree.add("X", "Xylophone");

        boolean deleted = tree.del("X");

        assertTrue(deleted);
        //assertNull(tree.get("X"));
    }

    @Test
    public void testLeafDelete() {
        SimpleBTree tree = new SimpleBTree();

        tree.add("A", "Apple");
        tree.add("B", "Banana");
        tree.add("C", "Cherry");

        // حذف مقدار میانی
        boolean deleted = tree.del("B");

        assertTrue(deleted);
/*        assertNull(tree.get("B"));
        assertEquals("Apple", tree.get("A"));
        assertEquals("Cherry", tree.get("C"));*/
    }



}