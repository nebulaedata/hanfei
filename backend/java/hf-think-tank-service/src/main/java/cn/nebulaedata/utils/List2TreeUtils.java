package cn.nebulaedata.utils;

import cn.nebulaedata.anno.EnsureField;
import cn.nebulaedata.pojo.LabelValuePojo;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import sun.reflect.generics.tree.Tree;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2023/11/24 18:32
 * @note
 */
public class List2TreeUtils<T> {
//    public static List<TreeMap> getTreeList(List<Map> list) {
//
//    }

    @Data
    static class TreeMap {

        private String id;
        private String fatherId;
        private List<TreeMap> children;

        public TreeMap() {

        }

        public TreeMap(String id) {
            this.id = id;
        }

        public TreeMap(String id, String fatherId) {
            this.id = id;
            this.fatherId = fatherId;
        }


    }
//    public static List<TreeMap> recursionMethod(List<TreeMap> treeList) {
//        List<TreeMap> trees = new ArrayList<>();
//        for (TreeMap tree : treeList) {
//            // 找出父节点
//            if (StringUtils.isBlank(tree.fatherId)) {
//                trees.add(findChildren(tree,treeList));
//            }
//        }
//        return trees;
//    }
//    public static TreeMap findChildren(TreeMap tree, List<TreeMap> treeList) {
//        for (TreeMap node : treeList) {
//            if (tree.id.equals(node.fatherId)) {
//                if (tree.children == null) {
//                    tree.children = new ArrayList<>();
//                }
//                tree.children.add(findChildren(node,treeList));
//            }
//        }
//        return tree;
//    }

    public static String firstCharUpper(String str) {
        if (StringUtils.isBlank(str)) return null;
        // 获取字符串的第一个字母并转换为大写
        char firstChar = Character.toUpperCase(str.charAt(0));
        // 构建新的字符串，将原来的字符串除了第一个字母外都保持不变
        StringBuilder newStrBuilder = new StringBuilder();
        for (int i = 1; i < str.length(); i++) {
            newStrBuilder.append(str.charAt(i));
        }
        return firstChar + newStrBuilder.toString();
    }

    public List<T> recursionMethod(List<T> treeList, String id, String fatherId) throws Exception {
        if (treeList.size() == 0) {
            return treeList;
        }
        Class<?> tClass = treeList.get(0).getClass();
        Method getFatherIdId = tClass.getMethod("get" + firstCharUpper(fatherId));
        List<T> trees = new ArrayList<>();
        for (T tree : treeList) {
            // 找出父节点
            if (StringUtils.isBlank((String) getFatherIdId.invoke(tree))) {
                trees.add(findChildren(tree, treeList, id, fatherId));
            }
        }
        return trees;
    }

    public T findChildren(T tree, List<T> treeList, String id, String fatherId) throws Exception {
        Class<?> tClass = tree.getClass();
        Method getId = tClass.getMethod("get" + firstCharUpper(id));
        Method getFatherIdId = tClass.getMethod("get" + firstCharUpper(fatherId));
        Method getChildren = tClass.getMethod("getChildren");
        Method setChildren = tClass.getMethod("setChildren", List.class);
        for (T node : treeList) {
            if (getId.invoke(tree).equals(getFatherIdId.invoke(node))) {
                if (getChildren.invoke(tree) == null) {
                    setChildren.invoke(tree, new ArrayList<>());
                }
                List children = (List) getChildren.invoke(tree);
                children.add(findChildren(node, treeList, id, fatherId));
            }
        }
        return tree;
    }

    public static void main(String[] args) throws Exception {


//        //正常的调用
//        LabelValuePojo apple = new LabelValuePojo();
//        apple.setLabel("5");
//        System.out.println("Apple Price:" + apple.getLabel());
//        //使用反射调用
//        Class clz = Class.forName("cn.nebulaedata.pojo.LabelValuePojo");
//        Method setPriceMethod = clz.getMethod("setLabel", String.class);
//        Constructor appleConstructor = clz.getConstructor();
//        Object appleObj = appleConstructor.newInstance();
//        setPriceMethod.invoke(appleObj, "14");
//        Method getPriceMethod = clz.getMethod("getLabel");
//        System.out.println("Apple Price:" + getPriceMethod.invoke(appleObj));

        List<TreeMap> treeMaps = new ArrayList<>();
        treeMaps.add(new TreeMap("1", "a"));
        treeMaps.add(new TreeMap("2", "a"));
        treeMaps.add(new TreeMap("3", "a"));
        treeMaps.add(new TreeMap("a"));
        treeMaps.add(new TreeMap("b"));
        treeMaps.add(new TreeMap("4", "b"));
        treeMaps.add(new TreeMap("5", "b"));
        treeMaps.add(new TreeMap("6", "b"));
        List<TreeMap> treeMaps1 = new List2TreeUtils<TreeMap>().recursionMethod(treeMaps, "id", "fatherId");
        System.out.println(JSON.toJSONString(treeMaps1));
    }
}
