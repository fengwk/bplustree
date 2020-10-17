package fun.fengwk.bplustree;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

/**
 * <h1>B+树
 * <p>该库提供一个允许插入重复关键码的B+树实现，该实现方案参考了
 * <a href="https://www.ixueshu.com/document/1ea00ff27973989b76d7c4d383484657318947a18e7f9386.html">一种处理B+树重复键值的方法<a/>，
 * 文献中描述了一种除溢出页之外的处理重复关键码方法，由于该文献并未详述实现细节，因此在具体实现上细节上会有一定差异。
 *
 * <p>如果此前从未了解过B+树，建议优先学习
 * <a href="https://zh.wikipedia.org/wiki/B%2B%E6%A0%91">B+树wiki<a/>，
 * 通常B+树的实现中约定不引入重复关键码，或者使用溢出页处理重复关键码，当前实现并非采用前两种方案实现，因此在定义上会与wiki中有一定差异。
 *
 * <h1>定义
 * <p>D.以下定义针对于m阶B+树：
 * <ol>
 * <li>内部节包含分支和关键码，最多有m个分支和m-1个关键码，除根节点外最少有ceil(m/2)个分支和ceil(m/2)-1个关键码。
 * <li>叶子节包含关键码和对应值，点最多有m-1个关键码，除根节点外最少有ceil(m/2)-1个关键码。
 * <li>根节点最少有2个分支和1个关键码。
 * <li>内部节点中第k个关键码定存放的是其第k+1个子节点为根的子树中第一次出现的”新“的数据关键码。
 * </ol>
 * <p>DS.定义的实现细节：
 * <ol>
 * <li>为了简化定义4的实现，为所有内部节点设置一个哨兵关键码，但进行具体关键码数量计算时并不计入哨兵关键码。
 * </ol>
 *
 * <h1>约定
 * <ol>
 * <li>为了实现空关键码的定义，实现中不允许用户插入空的关键码。
 * <li>为了明确返回value的语义，实现中不允许用户插入空的value。
 * </ol>
 *
 * <h1>数据结构
 * <p>内部节点
 * <ol>
 * <li>parent：指向父节点。
 * <li>keys：关键码列表，按照升序列排列，首个关键码是哨兵关键码，因此数量上与分支相同，根据DS.1除根节点外最多包含m个，最少包含ceil(m/2)个。
 * <li>children：子节点列表，children[i]是keys[i]的右子树，即D4中描述的k和k+1关系。
 * </ol>
 * <p>叶子节点
 * <ol>
 * <li>parent：指向父节点。
 * <li>prev：指向前一个叶子节点。
 * <li>next：指向后一个叶子节点。
 * <li>keys：关键码列表，按照升序列排列，除根节点外最多包含m-1个，最少包含ceil(m/2)-1个。
 * <li>values：值列表，keys[i]与value[i]对应。
 * </ol>
 * <p>B+树
 * <ol>
 * <li>m：阶次。
 * <li>root：根节点。
 * </ol>
 *
 * <h1>算法
 * <p>见README.md描述。
 *
 * @author fengwk
 */
public class BPlusTree<K extends Comparable<K>, V> {

    /* 数据结构定义 */

    /**
     * 抽象节点
     */
    abstract class Node {

        /**
         * 指向父节点。
         */
        protected InternalNode parent;

        /**
         * 关键码列表，按照升序列排列，首个关键码是哨兵关键码，因此数量上与分支相同，根据DS.1除根节点外最多包含m个，最少包含ceil(m/2)个。
         */
        protected LinkedList<K> keys;

        /**
         *
         * @param parent
         * @param keys
         */
        protected Node(InternalNode parent, LinkedList<K> keys) {
            this.parent = parent;
            this.keys = keys;
        }

    }

    /**
     * 内部节点
     */
    class InternalNode extends Node {

        /**
         * 子节点列表，children[i]是keys[i]的右子树，即D4中描述的k和k+1关系。
         */
        LinkedList<Node> children;

        /**
         * 构造一个内部节点。
         *
         * @param parent
         * @param keys
         * @param children
         */
        InternalNode(InternalNode parent, LinkedList<K> keys, LinkedList<Node> children) {
            super(parent, keys);
            this.children = children;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            for (int i = 0; i < keys.size(); i++) {
                K k = keys.get(i);
                builder.append(k == null ? '#' : k).append(',');
            }
            return (builder.length() > 1 ? builder.substring(0, builder.length()-1) : builder.toString()) + '}';
        }

    }

    /**
     * 叶子节点
     */
    class LeafNode extends Node {

        /**
         * 指向前一个叶子节点。
         */
        LeafNode prev;

        /**
         * 指向后一个叶子节点。
         */
        LeafNode next;

        /**
         * 值列表，keys[i]与value[i]对应。
         */
        LinkedList<V> values;

        /**
         * 构造一个叶子节点。
         *
         * @param parent
         * @param prev
         * @param next
         * @param keys
         * @param values
         */
        LeafNode(InternalNode parent, LeafNode prev, LeafNode next, LinkedList<K> keys, LinkedList<V> values) {
            super(parent, keys);
            this.prev = prev;
            this.next = next;
            this.values = values;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            for (int i = 0; i < keys.size(); i++) {
                builder.append(keys.get(i)).append('=').append(values.get(i)).append(',');
            }
            return (builder.length() > 1 ? builder.substring(0, builder.length()-1) : builder.toString()) + '}';
        }

    }

    /* B+树属性 */

    /**
     * 阶次。
     */
    final int m;

    /**
     * 根节点。
     */
    Node root;

    /* 辅助算法 */

    /**
     * 构造一棵m阶B+树。
     *
     * @param m
     */
    public BPlusTree(int m) {
        this.m = m;
    }

    /**
     * A1.判断根节点。
     *
     * @return
     */
    boolean isRoot(Node node) {
        return node.parent == null;
    }

    /**
     * A2.判断上溢。
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    boolean isOverflow(Node node) {
        return node instanceof BPlusTree.InternalNode ? isOverflow((InternalNode) node) : isOverflow((LeafNode) node);
    }

    boolean isOverflow(InternalNode node) {
        return node.children.size() > m;
    }

    boolean isOverflow(LeafNode node) {
        return node.keys.size() > m-1;
    }

    /**
     * A3.判断下溢。
     *
     * @param node
     * @return
     */
    @SuppressWarnings("unchecked")
    boolean isUnderflow(Node node) {
        return node instanceof BPlusTree.InternalNode ? isUnderflow((InternalNode) node) : isUnderflow((LeafNode) node);
    }

    boolean isUnderflow(InternalNode node) {
        return isRoot(node) ? node.children.size() < 2 : node.children.size() < (m+1)/2;
    }

    boolean isUnderflow(LeafNode node) {
        return isRoot(node) ? node.keys.size() < 1 : node.keys.size() < (m+1)/2-1;
    }

    /**
     * A4.key所在子树。
     *
     * @param node
     * @param key
     * @return
     */
    Node locateChildByKey(InternalNode node, K key) {
        int r = 0;// 从索引0处开始查找的隐含意义是如果keys[0]就大于key，那么就向children[0]深入，因为这是全树的最左侧。
        int i = 0;
        for (K k : node.keys) {
            if (k != null) {
                if (k.compareTo(key) > 0) {
                    break;
                }
                // else k <= key，由于相等的key会以null表示，这里使用<=是安全的。
                r = i;
            }
            i++;
        }
        return node.children.get(r);
    }

    /**
     * A5.key所在叶子。
     *
     * @param node
     * @param key
     * @return
     */
    LeafNode locateLeafByKey(LeafNode node, K key) {
        while (key.compareTo(node.keys.getLast()) > 0 && node.next != null) {
            node = node.next;
        }
        return node;
    }

    /**
     * A6.搜索叶子节点。
     *
     * @param node
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    LeafNode searchLeaf(Node node, K key) {
        while (node instanceof BPlusTree.InternalNode) {
            node = locateChildByKey((InternalNode) node, key);
        }
        return locateLeafByKey((LeafNode) node, key);
    }

    /**
     * A7.在keys中查找key。
     * 查找keys中查找小于key秩最大的元素的下标。
     *
     * @param keys
     * @param key
     * @return
     */
    int littleLess(LinkedList<K> keys, K key) {
        int r = -1;
        Iterator<K> iter = keys.iterator();
        while (iter.hasNext() && iter.next().compareTo(key) < 0) {
            r++;
        }
        return r;
    }

    /**
     * A8.内部节点索引更新
     *
     * @param node
     * @return true-进行了更新，false-未进行更新。
     */
    boolean updateIndexKey(InternalNode node) {
        if (isRoot(node)) {
            return false;
        }

        int indexInParent;
        K nextKeyInParent;
        if (shouldUpdate(keyInParent(node, indexInParent = indexInParent(node)), nextKeyInParent = getIndexKey(node))) {
            node.parent.keys.set(indexInParent, nextKeyInParent);
            return true;
        }
        return false;
    }

    /**
     * 不断地向上更新内部节点的索引，直到没有更新变化为止。
     *
     * @param node
     */
    void propagateUpdateIndexKey(InternalNode node) {
        while (updateIndexKey(node)) {
            node = node.parent;
        }
    }

    /**
     * A9.叶子节点索引更新。
     *
     * @param node
     * @return true-进行了更新，false-未进行更新。
     */
    boolean updateIndexKey(LeafNode node) {
        if (isRoot(node)) {
            return false;
        }

        K nextKeyInParent = getIndexKey(node);
        int indexInParent = indexInParent(node);
        K keyInParent = keyInParent(node, indexInParent);

        boolean shouldUpdate = shouldUpdate(keyInParent, nextKeyInParent);
        if (shouldUpdate) {
            node.parent.keys.set(indexInParent, nextKeyInParent);
        }
        return shouldUpdate;
    }

    @SuppressWarnings("unchecked")
    boolean updateIndexKey(Node node) {
        return node instanceof BPlusTree.InternalNode ? updateIndexKey((InternalNode) node) : updateIndexKey((LeafNode) node);
    }

    /**
     * 获取输入内部节点应对应的索引值。
     *
     * @param node
     * @return
     */
    K getIndexKey(InternalNode node) {
        return findFirstNotNull(node.keys);
    }

    /**
     * 获取输入叶子节点应对应的索引值。
     *
     * @param node
     * @return
     */
    K getIndexKey(LeafNode node) {
        // 空节点的索引为空。
        if (node.keys.isEmpty()) {
            return null;
        }

        // 如果前一个叶子节点不存在或者前一个叶子节点最后部分与当前叶子节点的第一个关键码不同则直接返回当前叶子节点的首个关键码即可。
        K firstKey = node.keys.getFirst();
        if (node.prev == null || node.prev.keys.isEmpty() || node.prev.keys.getLast().compareTo(firstKey) < 0) {
            return firstKey;
        }

        // 查找首个大于firstKey的关键码，不存在则索引为空。
        return findFirstGt(node.keys, firstKey);
    }

    @SuppressWarnings("unchecked")
    K getIndexKey(Node node) {
        return node instanceof BPlusTree.InternalNode ? getIndexKey((InternalNode) node) : getIndexKey((LeafNode) node);
    }

    int indexInParent(Node node) {
        return node.parent.children.indexOf(node);
    }

    K keyInParent(Node node, int indexInParent) {
        return node.parent.keys.get(indexInParent);
    }

    K findFirstNotNull(LinkedList<K> keys) {
        for (K k : keys) {
            if (k != null) {
                return k;
            }
        }
        return null;
    }

    /**
     * 查找首个大于lo的关键码，不存在则返回null。
     *
     * @param keys
     * @param lo
     * @return
     */
    K findFirstGt(LinkedList<K> keys, K lo) {
        for (K k : keys) {
            if (k != null && k.compareTo(lo) > 0) {
                return k;
            }
        }
        return null;
    }

    boolean shouldUpdate(K prevKey, K nextKey) {
        /*
         * 需要更新的几种情况：
         * 1. prevKey != null && nextKey == null
         * 2. prevKey == null && nextKey != null
         * 3. prevKey != null && nextKey != null && prevKey != nextKey
         */
        return prevKey != nextKey || (prevKey != null && prevKey.compareTo(nextKey) != 0);
    }

    /**
     * 分裂。
     *
     * @param node
     * @return
     */
    @SuppressWarnings("unchecked")
    Node split(Node node) {
        return node instanceof BPlusTree.InternalNode ? split((InternalNode) node) : split((LeafNode) node);
    }

    InternalNode split(InternalNode node) {
        LinkedList<K> keys = node.keys;
        LinkedList<Node> children = node.children;

        /*
         * 因为上溢发生，所以此时children.size()为m+1。
         * 将区间分裂为左侧[0...mi)和右侧[mi...m+1)，左右区间关键码数量分别为mi和m+1-mi。
         * mi取children.size()/2，即为ceil(m/2)，可得左右区间关键码数量分别为ceil(m/2)和floor(m/2)+1，不会继而发生下溢。
         */
        int mi = children.size() / 2;

        LinkedList<K> rightKeys = new LinkedList<>(keys.subList(mi, keys.size()));
        LinkedList<Node> rightChildren = new LinkedList<>(children.subList(mi, children.size()));
        InternalNode rightNode = new InternalNode(node.parent, rightKeys, rightChildren);
        for (Node rightNodeChild : rightChildren) {
            rightNodeChild.parent = rightNode;
        }

        keys.subList(mi, keys.size()).clear();
        children.subList(mi, children.size()).clear();

        return rightNode;
    }

    LeafNode split(LeafNode node) {
        LinkedList<K> keys = node.keys;
        LinkedList<V> values = node.values;

        /*
         * 因为上溢发生，所以此时keys.size()为m。
         * 将区间分裂为左侧[0...mi)和右侧[mi...m+1)，左右区间关键码数量分别为mi和m+1-mi。
         * mi取keys.size()/2，即为floor(m/2)，可得左右区间关键码数量分别为floor(m/2)和m+1-floor(m/2)，不会继而发生下溢。
         */
        int mi = keys.size() / 2;

        LinkedList<K> rightKeys = new LinkedList<>(keys.subList(mi, keys.size()));
        LinkedList<V> rightValues = new LinkedList<>(values.subList(mi, values.size()));
        LeafNode rightNode = new LeafNode(node.parent, node, node.next, rightKeys, rightValues);
        if (node.next != null) {
            node.next.prev = rightNode;
        }
        node.next = rightNode;

        keys.subList(mi, keys.size()).clear();
        values.subList(mi, values.size()).clear();

        return rightNode;
    }

    @SuppressWarnings("unchecked")
    void leftLendRight(Node left, Node right) {
        if (left instanceof BPlusTree.InternalNode) {
            leftLendRight((InternalNode) left, (InternalNode) right);
        } else {
            leftLendRight((LeafNode) left, (LeafNode) right);
        }
    }

    void leftLendRight(InternalNode left, InternalNode right) {
        K leftLastKey = left.keys.removeLast();
        Node leftLastChild = left.children.removeLast();

        right.keys.addFirst(leftLastKey);
        right.children.addFirst(leftLastChild);
        leftLastChild.parent = right;

        updateIndexKey(left);
        updateIndexKey(right);
    }

    void leftLendRight(LeafNode left, LeafNode right) {
        K leftLastKey = left.keys.removeLast();
        V leftLastValue = left.values.removeLast();

        right.keys.addFirst(leftLastKey);
        right.values.addFirst(leftLastValue);

        updateIndexKey(left);
        updateIndexKey(right);
    }

    @SuppressWarnings("unchecked")
    void rightLendLeft(Node left, Node right) {
        if (left instanceof BPlusTree.InternalNode) {
            rightLendLeft((InternalNode) left, (InternalNode) right);
        } else {
            rightLendLeft((LeafNode) left, (LeafNode) right);
        }
    }

    void rightLendLeft(InternalNode left, InternalNode right) {
        K rightFirstKey = right.keys.removeFirst();
        Node rightFirstChild = right.children.removeFirst();

        left.keys.addLast(rightFirstKey);
        left.children.addLast(rightFirstChild);
        rightFirstChild.parent = left;

        updateIndexKey(left);
        updateIndexKey(right);
    }

    void rightLendLeft(LeafNode left, LeafNode right) {
        K rightFirstKey = right.keys.removeFirst();
        V rightFirstValue = right.values.removeFirst();

        left.keys.addLast(rightFirstKey);
        left.values.addLast(rightFirstValue);

        updateIndexKey(left);
        updateIndexKey(right);
    }

    /**
     * 合并。
     *
     * @param left
     */
    @SuppressWarnings("unchecked")
    void merge(Node left) {
        if (left instanceof BPlusTree.InternalNode) {
            merge((InternalNode) left);
        } else {
            merge((LeafNode) left);
        }
    }

    void merge(InternalNode left) {
        int leftIndexInParent = indexInParent(left);
        left.parent.keys.remove(leftIndexInParent+1);
        @SuppressWarnings("unchecked")
        InternalNode right = (InternalNode) left.parent.children.remove(leftIndexInParent+1);

        left.keys.addAll(right.keys);
        left.children.addAll(right.children);
        for (Node child : right.children) {
            child.parent = left;
        }

        updateIndexKey(left);
    }

    void merge(LeafNode left) {
        int leftIndexInParent = indexInParent(left);
        left.parent.keys.remove(leftIndexInParent+1);
        @SuppressWarnings("unchecked")
        LeafNode right = (LeafNode) left.parent.children.remove(leftIndexInParent+1);

        left.keys.addAll(right.keys);
        left.values.addAll(right.values);
        left.next = right.next;
        if (right.next != null) {
            right.next.prev = left;
        }

        updateIndexKey(left);
    }

    /**
     * A10.解决上溢。
     *
     * @param node
     */
    void trySolveOverflow(Node node) {
        while (node != null && isOverflow(node)) {
            Node rightNode = split(node);
            InternalNode parent = node.parent;
            if (parent == null) {
                /*
                 * 根节点被分裂了。
                 */
                InternalNode newRoot = new InternalNode(null, new LinkedList<>(), new LinkedList<>());
                newRoot.keys.add(getIndexKey(node));
                newRoot.keys.add(getIndexKey(rightNode));
                newRoot.children.add(node);
                newRoot.children.add(rightNode);
                node.parent = newRoot;
                rightNode.parent = newRoot;
                this.root = newRoot;
            } else {
                /*
                 * 对于parent来说需要更新分裂左右子树的索引。
                 * 对于parent.parent来说产生分裂的子树整体没有新增或者删除关键码，只是内部拓扑结构发生了变化，因此无需更新索引。
                 * 因此分裂的索引更新只会影响其parent，而不会再向上传播。
                 */
                int leftIndexInParent = indexInParent(node);
                updateIndexKey(node);
                parent.keys.add(leftIndexInParent+1, getIndexKey(rightNode));
                parent.children.add(leftIndexInParent+1, rightNode);
            }
            node = parent;
        }
    }

    /**
     * A11.解决下溢。
     *
     * @param node
     */
    @SuppressWarnings("unchecked")
    void trySolveUnderflow(Node node) {
        while (node != null && isUnderflow(node)) {
            InternalNode parent = node.parent;
            if (parent == null) {
                /*
                 * 根节点发生下溢。
                 */
                Node newRoot;
                if (node instanceof BPlusTree.InternalNode) {
                    // 因为根节点分支数只有1时才会发生下溢，因此直接取children[0]作为新的根节点即可。
                    newRoot = ((InternalNode) node).children.get(0);
                    newRoot.parent = null;
                } else {
                    newRoot = null;
                }
                this.root = newRoot;
            } else {
                int indexInParent = indexInParent(node);
                Node leftSibling = indexInParent-1 >= 0 ? parent.children.get(indexInParent-1) : null;
                Node rightSibling = indexInParent+1 < parent.children.size() ? parent.children.get(indexInParent+1) : null;

                /*
                 * 首先尝试从兄弟借，成功则无需再向上传递。
                 */
                if (leftSibling != null && leftSibling.keys.size() > (m+1)/2) {
                    leftLendRight(leftSibling, node);
                    break;
                }
                if (rightSibling != null && rightSibling.keys.size() > (m+1)/2) {
                    rightLendLeft(node, rightSibling);
                    break;
                }

                /*
                 * 合并。
                 */
                if (leftSibling != null) {
                    merge(leftSibling);
                } else {
                    merge(node);
                }
            }
            node = parent;
        }
    }

    void tryUpdateIndexKeyAfterInsert(LeafNode node) {
        if (updateIndexKey(node)) {
            propagateUpdateIndexKey(node.parent);
        }
    }

    void tryUpdateIndexKeyAfterDelete(LeafNode node, boolean shouldTryUpdateNextLeafNodeIndexKey) {
        InternalNode propagateNode = null;
        InternalNode nextPropagateNode = null;
        if (updateIndexKey(node)) {
            propagateNode = node.parent;
        }
        if (shouldTryUpdateNextLeafNodeIndexKey && updateIndexKey(node.next)) {
            nextPropagateNode = node.next.parent;
        }
        if (propagateNode != null) {
            propagateUpdateIndexKey(propagateNode);
        }
        if (nextPropagateNode != null && nextPropagateNode != propagateNode) {
            propagateUpdateIndexKey(nextPropagateNode);
        }
    }

    /* 主算法 */

    /**
     * M1.搜索。
     * <p>搜索关键码key所对应的值。
     * 由于当前B+树实现约束中不支持存放空的值，因此一旦返回null则表明关键码key在当前B树中不存在。
     *
     * @param key
     * @return
     */
    public V search(K key) {
        System.out.println("search " + key);
        Objects.requireNonNull(key, "Key cannot be null.");

        Node root = this.root;
        if (root == null) {
            return null;
        }

        LeafNode node = searchLeaf(root, key);
        int r = littleLess(node.keys, key);
        return r+1 < node.keys.size() && node.keys.get(r+1).compareTo(key) == 0 ? node.values.get(r+1) : null;
    }

    /**
     * M2.插入。
     * <p>向B+树中插入关键码以及对应值。
     *
     * @param key
     * @param value
     */
    public void insert(K key, V value) {
        System.out.println("insert " + key + " " + value);
        Objects.requireNonNull(key, "Key cannot be null.");
        Objects.requireNonNull(value, "Value cannot be null.");

        // 首先处理树为空的情况
        Node root = this.root;
        if (root == null) {
            LeafNode newRoot = new LeafNode(null, null, null, new LinkedList<>(), new LinkedList<>());
            newRoot.keys.add(key);
            newRoot.values.add(value);
            this.root = newRoot;
            return;
        }

        // 定位插入点
        LeafNode node = searchLeaf(root, key);
        int r = littleLess(node.keys, key);
        // 插入
        node.keys.add(r+1, key);
        node.values.add(r+1, value);
        // 尝试更新索引
        tryUpdateIndexKeyAfterInsert(node);
        // 尝试解决上溢
        trySolveOverflow(node);
    }

    /**
     * M3.删除。
     * <p>删除关键码为key的节点，并返回被删除的值。
     * 由于当前B+树实现约束中不支持存放空的值，因此一旦返回null则表明关键码key在当前B树中不存在。
     *
     * @param key
     * @return
     */
    public V delete(K key) {
        System.out.println("delete " + key);
        Objects.requireNonNull(key, "Key cannot be null.");

        // 首先处理树为空的情况
        Node root = this.root;
        if (root == null) {
            return null;
        }

        // 定位删除点
        LeafNode node = searchLeaf(root, key);
        int r = littleLess(node.keys, key);
        // 处理关键码不存在的情况
        if (r+1 > node.keys.size() || node.keys.get(r+1).compareTo(key) > 0) {
            return null;
        }

        boolean shouldTryUpdateNextLeafNodeIndexKey = r+2 == node.keys.size() && node.next != null;
        // 删除
        node.keys.remove(r+1);
        V deletedValue = node.values.remove(r+1);
        // 尝试更新索引
        tryUpdateIndexKeyAfterDelete(node, shouldTryUpdateNextLeafNodeIndexKey);
        // 尝试解决下溢
        trySolveUnderflow(node);

        return deletedValue;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("B+Tree\n");
        if (root == null) {
            builder.append("Empty");
            return builder.toString();
        }
        LinkedList<Node> curQueue = new LinkedList<>();
        LinkedList<Node> nextQueue = new LinkedList<>();
        curQueue.offer(root);
        while (!curQueue.isEmpty()) {
            Node cur = curQueue.poll();
            builder.append(cur).append(' ');
            if (cur instanceof BPlusTree.InternalNode) {
                for (Node child : ((InternalNode) cur).children) {
                    if (child != null) {
                        nextQueue.offer(child);
                    }
                }
            }
            if (curQueue.isEmpty()) {
                builder.append('\n');
                LinkedList<Node> temp = curQueue;
                curQueue = nextQueue;
                nextQueue = temp;
            }
        }
        return builder.toString();
    }

}
