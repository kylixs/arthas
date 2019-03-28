package com.taobao.arthas.core.view;

import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.TraceStackOptions;
import com.taobao.arthas.core.util.TreeUtils;
import com.taobao.arthas.core.util.collection.MethodCollector;

import java.util.*;

/**
 * 树形控件
 * Created by vlinux on 15/5/26.
 */
public class TreeView implements View {

    public static final int INVOKE_ON_BEGIN = 1;
    public static final int INVOKE_AFTER_THROWING = 2;
    public static final int INVOKE_ON_INVOKE_BEFORE = 3;

    private static final String STEP_FIRST_CHAR = "`---";
    private static final String STEP_NORMAL_CHAR = "+---";
    private static final String STEP_HAS_BOARD = "|   ";
    private static final String STEP_EMPTY_BOARD = "    ";
    private static final String TIME_UNIT = "ms";

    // 是否输出耗时
    private final boolean isPrintCost;

    // 根节点
    private final Node root;

    // 当前节点
    private Node current;

    // 最耗时的节点
    private Node maxCost;


    public TreeView(boolean isPrintCost, String title) {
        this.root = new Node(title).markBegin().markEnd();
        this.current = root;
        this.isPrintCost = isPrintCost;
    }

    @Override
    public String draw() {

        final StringBuilder treeSB = new StringBuilder();

        final Ansi highlighted = Ansi.ansi().fg(Ansi.Color.RED);

        recursive(0, true, "", root, new Callback() {

            @Override
            public void callback(int deep, boolean isLast, String prefix, Node node) {
                treeSB.append(prefix).append(isLast ? STEP_FIRST_CHAR : STEP_NORMAL_CHAR);
                if (isPrintCost && !node.isRoot()) {
                    if (node == maxCost) {
                        // the node with max cost will be highlighted
                        treeSB.append(highlighted.a(node.toString()).reset().toString());
                    } else {
                        treeSB.append(node.toString());
                    }
                }
                treeSB.append(node.data);
                if (!StringUtils.isBlank(node.mark)) {
                    treeSB.append(" [").append(node.mark).append(node.marks > 1 ? "," + node.marks : "").append("]");
                }
                treeSB.append("\n");
            }

        });

        return treeSB.toString();
    }

    public void pretty() {
        if (TraceStackOptions.mergeNodes) {
            rebuildInvokeTree(root);
        }

        findMaxCostNode(root);

        sortAndFilterChildrenNodes(root);
    }

    public MethodCollector getMethodCollector(){
        MethodCollector methodCollector = new MethodCollector();
        getAllMethods(methodCollector, root.children.get(0));
        return methodCollector;
    }

    private static void getAllMethods(MethodCollector methodCollector, Node node) {
        //remove last '()'
        String methodName = node.data.substring(0, node.data.length()-2);
        String className = methodName.substring(0, methodName.indexOf(':'));
        methodName = methodName.substring(className.length()+1);
        methodCollector.addMethod(className, methodName);

        if(node.isLeaf()){
            return;
        }
        for (int i = 0; i < node.children.size(); i++) {
            Node childNode = node.children.get(i);
            getAllMethods(methodCollector, childNode);
        }
    }

    /**
     * check tree depth and total cost
     * @return
     */
    public boolean isLittleTree() {
        if(Node.getCostInMillis(root.children.get(0).totalCost) < TraceStackOptions.minCost){
            return true;
        }
        return false;
    }

    private void rebuildInvokeTree(Node node) {
        //find same invoke node
        if(node.parent != null && shouldMergeNodes(node, node.children)){
            Node childNode = node.children.get(0);
            //replace dynamic proxy className with parentNode
//            if(TraceStackOptions.decorateProxy && childNode.data.startsWith("com.sun.proxy.$Proxy")){
//                childNode.setData(node.data);
//            }
            if(!childNode.data.equals(node.data)){
                childNode.data += " ["+node.data+"]";
            }
            node.parent.replaceChild(node, childNode);
            rebuildInvokeTree(childNode);
        }else if(!node.isLeaf()){
            //node.children maybe change on rebuild
            for (int i = 0; i < node.children.size(); i++) {
                Node child = node.children.get(i);
                rebuildInvokeTree(child);
            }
        }
    }

    private boolean shouldMergeNodes(Node node, List<Node> childrenNodes) {
        if(childrenNodes.size() !=1 ){
            return false;
        }
        //合并重复的结点： merge onBegin to last level onInvokeBefore
        Node childNode = childrenNodes.get(0);
        if(childNode.invokeType == INVOKE_ON_BEGIN && node.invokeType == INVOKE_ON_INVOKE_BEFORE){
            //check class method is the same
            String[] methodNames = TreeUtils.parseNodeData(node.data);
            String[] childMethodNames = TreeUtils.parseNodeData(childNode.data);
            if(methodNames[1].equals(childMethodNames[1])) {
                //TODO check superclass or interface
                return true;
            }
        }
        return false;
    }

    /**
     * 递归遍历
     */
    private void recursive(int deep, boolean isLast, String prefix, Node node, Callback callback) {
        callback.callback(deep, isLast, prefix, node);
        if (!node.isLeaf()) {
            final int size = node.children.size();
            for (int index = 0; index < size; index++) {
                final boolean isLastFlag = index == size - 1;
                final String currentPrefix = isLast ? prefix + STEP_EMPTY_BOARD : prefix + STEP_HAS_BOARD;
                recursive(
                        deep + 1,
                        isLastFlag,
                        currentPrefix,
                        node.children.get(index),
                        callback
                );
            }
        }
    }

    /**
     * 查找耗时最大的节点，便于后续高亮展示
     * @param node
     */
    private void findMaxCostNode(Node node) {
        if (!node.isRoot() && !node.parent.isRoot()) {
            if (maxCost == null) {
                maxCost = node;
            } else if (maxCost.totalCost < node.totalCost) {
                maxCost = node;
            }
        }
        if (!node.isLeaf()) {
            for (Node n: node.children) {
                findMaxCostNode(n);
            }
        }
    }

    /**
     * 对儿子节点按照cost时间排序
     * @param node
     */
    private void sortAndFilterChildrenNodes(Node node) {
        if(node.isLeaf()){
           return;
        }
        if(TraceStackOptions.sortNodes) {
            //sort by totalCost desc
            Collections.sort(node.children, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return (int) (o2.totalCost - o1.totalCost);
                }
            });

            int outputLines = TraceStackOptions.topSize;
            if (outputLines > 1) {
                //remove n+1 children nodes ..
                for (int i = node.children.size() - 1; i >= outputLines; i--) {
                    node.children.remove(i);
                }
            }
        }

        double minCost = TraceStackOptions.minCost;
        if(minCost > 0){
            for (int i = node.children.size() - 1; i >= 0; i--) {
                Node childNode = node.children.get(i);
                if(!node.isRoot() && Node.getCostInMillis(childNode.totalCost) < minCost ){
                    node.children.remove(i);
                }
            }
        }

        for (Node child : node.children) {
            sortAndFilterChildrenNodes(child);
        }
    }

    /**
     * 创建一个分支节点
     *
     * @param data 节点数据
     * @return this
     */
    public TreeView begin(String data, int invokeType) {
        Node n = current.find(data);
        if (n != null) {
            current = n;
        } else {
            current = new Node(current, data, invokeType);
        }
        current.markBegin();
        return this;
    }

    /**
     * 结束一个分支节点
     *
     * @return this
     */
    public TreeView end() {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current.markEnd();
        current = current.parent;
        return this;
    }

    /**
     * 结束一个分支节点,并带上备注
     *
     * @return this
     */
    public TreeView end(String mark) {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current.markEnd().mark(mark);
        current = current.parent;
        return this;
    }

    public String getTopTraceData() {
        //treeView: thread info -> top method -> sub method ..
        return root.children.get(0).data;
    }

    /**
     * 树节点
     */
    private static class Node {

        /**
         * 父节点
         */
        Node parent;

        /**
         * 节点数据
         */
        String data;

//        String className;
//        String methodName;

        /**
         * 子节点
         */
        final List<Node> children = new ArrayList<Node>();

        final Map<String, Node> map = new HashMap<String, Node>();

        /**
         * 开始时间戳
         */
        private long beginTimestamp;

        /**
         * 结束时间戳
         */
        private long endTimestamp;

        /**
         * 备注
         */
        private String mark;

        /**
         * 调用方式
         */
        private int invokeType;

        /**
         * 构造树节点(根节点)
         */
        private Node(String data) {
            this.parent = null;
            this.setData(data);
            this.invokeType = INVOKE_ON_BEGIN;
        }

        /**
         * 构造树节点
         *
         * @param parent 父节点
         * @param data   节点数据
         */
        private Node(Node parent, String data, int invokeType) {
            this.parent = parent;
            this.setData(data);
            this.invokeType = invokeType;
            parent.children.add(this);
            parent.map.put(data, this);
        }

        void replaceChild(Node oldChildNode, Node newChildNode) {
            int i = this.children.indexOf(oldChildNode);
            if(i != -1){
                this.map.remove(oldChildNode.data);
                this.map.put(newChildNode.data, newChildNode);
                this.children.set(i, newChildNode);
            }
        }

        void setData(String newData) {
            this.data = newData;
//            String[] vals = TreeUtils.parseNodeData(this.data);
//            this.className = vals[0];
//            this.methodName = vals[1];
        }

        /**
         * 查找已经存在的节点
         */
        Node find(String data) {
            return map.get(data);
        }

        /**
         * 是否根节点
         *
         * @return true / false
         */
        boolean isRoot() {
            return null == parent;
        }

        /**
         * 是否叶子节点
         *
         * @return true / false
         */
        boolean isLeaf() {
            return children.isEmpty();
        }

        Node markBegin() {
            beginTimestamp = System.nanoTime();
            return this;
        }

        Node markEnd() {
            endTimestamp = System.nanoTime();

            long cost = getCost();
            if (cost < minCost) {
                minCost = cost;
            }
            if (cost > maxCost) {
                maxCost = cost;
            }
            times++;
            totalCost += cost;

            return this;
        }

        Node mark(String mark) {
            this.mark = mark;
            marks++;
            return this;
        }

        long getCost() {
            return endTimestamp - beginTimestamp;
        }

        /**
         * convert nano-seconds to milli-seconds
         */
        static double getCostInMillis(long nanoSeconds) {
            return nanoSeconds / 1000000.0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (times <= 1) {
                sb.append("[").append(getCostInMillis(getCost())).append(TIME_UNIT).append("] ");
            } else {
                sb.append("[total=").append(getCostInMillis(totalCost)).append(TIME_UNIT)
                        .append(",min=").append(getCostInMillis(minCost)).append(TIME_UNIT)
                        .append(",max=").append(getCostInMillis(maxCost)).append(TIME_UNIT)
                        .append(",count=").append(times).append("] ");
            }
            return sb.toString();
        }

        /**
         * 合并统计相同调用,并计算最小\最大\总耗时
         */
        private long minCost = Long.MAX_VALUE;
        private long maxCost = Long.MIN_VALUE;
        private long totalCost = 0;
        private long times = 0;
        private long marks = 0;

    }


    /**
     * 遍历回调接口
     */
    private interface Callback {

        void callback(int deep, boolean isLast, String prefix, Node node);

    }

}
