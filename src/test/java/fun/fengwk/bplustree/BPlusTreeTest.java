package fun.fengwk.bplustree;

import org.junit.Test;

/**
 * @author fengwk
 */
public class BPlusTreeTest {

    @Test
    public void test() {
        fun.fengwk.bplustree.BPlusTree<Integer, Integer> bpTree = new fun.fengwk.bplustree.BPlusTree<>(3);

        bpTree.insert(3, 31);
        System.out.println(bpTree);

        bpTree.insert(3, 32);
        System.out.println(bpTree);

        bpTree.insert(3, 33);
        System.out.println(bpTree);

        bpTree.insert(3, 34);
        System.out.println(bpTree);

        bpTree.insert(3, 35);
        System.out.println(bpTree);

        bpTree.insert(3, 36);
        System.out.println(bpTree);

        bpTree.insert(3, 37);
        System.out.println(bpTree);

        bpTree.insert(3, 38);
        System.out.println(bpTree);

        bpTree.insert(1, 11);
        System.out.println(bpTree);

        bpTree.insert(-1, -11);
        System.out.println(bpTree);

        bpTree.insert(5, 51);
        System.out.println(bpTree);

        bpTree.insert(5, 52);
        System.out.println(bpTree);

        bpTree.insert(4, 41);
        System.out.println(bpTree);

        System.out.println(bpTree.search(3));
        System.out.println(bpTree.search(-1));
        System.out.println(bpTree.search(1));
        System.out.println(bpTree.search(4));
        System.out.println(bpTree.search(5));

        bpTree.delete(3);
        System.out.println(bpTree);

        bpTree.delete(3);
        System.out.println(bpTree);

        bpTree.delete(3);
        System.out.println(bpTree);

        bpTree.delete(3);
        System.out.println(bpTree);

        bpTree.delete(3);
        System.out.println(bpTree);

        bpTree.delete(3);
        System.out.println(bpTree);

        bpTree.delete(3);
        System.out.println(bpTree);

        bpTree.delete(3);
        System.out.println(bpTree);

        bpTree.delete(1);
        System.out.println(bpTree);

        bpTree.delete(-1);
        System.out.println(bpTree);

        bpTree.delete(5);
        System.out.println(bpTree);

        bpTree.delete(5);
        System.out.println(bpTree);

        bpTree.delete(4);
        System.out.println(bpTree);
    }

}
