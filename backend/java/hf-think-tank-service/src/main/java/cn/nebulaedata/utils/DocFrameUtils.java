package cn.nebulaedata.utils;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author 贾亦真
 * @date 2020/12/21 09:54
 * @note
 */
public class DocFrameUtils {

    /**
     * 保留两位小数
     *
     * @param num
     * @return
     */
    public static Double decimalKeepTwo(Double num) {
        if (num == null) {
            num = 0.0;
        }
        Double round = (double) (Math.round(num * 100)) / 100;
        return round;
    }

    /**
     * 保留N位小数
     *
     * @param num
     * @param n
     * @return
     */
    public static Double decimalKeepN(Double num, Integer n) {
        if (num == null) {
            num = 0.0;
        }
        if (n == null) {
            n = 2;
        }
        Double round = (double) (Math.round(num * Math.pow(10, n))) / Math.pow(10, n);
        return round;
    }

    /**
     * 正式版本号计算
     *
     * @param version
     * @param i
     * @return
     */
    public static String versionCount(String version, Integer i) {
        if (version == null) {
            return "V1.0";
        }
        if (i == null || i == 0) {
            return version;
        }
        version = version.replaceAll("\\-.*?", "");
        Double versionNum = Double.parseDouble(version.replaceAll("[a-zA-Z]", ""));
        versionNum = versionNum + (double) i / 10;
        return "V" + Double.toString(decimalKeepN(versionNum, 1));
    }

    /**
     * 正式版本号计算
     *
     * @param version
     * @return
     */
    public static String versionCount(String version) {
        return versionCount(version, 1);
    }

    /**
     * 草稿版本号计算
     *
     * @param version
     * @param i
     * @return
     */
    public static String draftVersionCount(String version, Integer i) {
        String[] versions = version.split("\\-");
        String versionMain = null;
        String versionSeco = null;
        if (versions.length == 2) {
            versionMain = versions[0];
            versionSeco = versions[1];
            Integer versionSecoNum = Integer.parseInt(versionSeco.replaceAll("D", "")) + i;
            versionSeco = "D" + Integer.toString(versionSecoNum);
        } else if (versions.length == 1) {
            versionMain = versions[0];
            versionSeco = "D1";
        } else {
            versionMain = "V1.0";
            versionSeco = "D1";
        }
        return versionMain + "-" + versionSeco;
    }

    /**
     * 草稿版本号计算
     *
     * @param version
     * @return
     */
    public static String draftVersionCount(String version) {
        return draftVersionCount(version, 1);
    }

    /**
     * 列表转字符串
     *
     * @param list
     * @param split
     * @return
     */
    public static String listConStr(List list, String split) {
        StringBuffer resultStr = new StringBuffer();
        if (list == null) {
            return resultStr.toString();
        }
        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                resultStr.append(list.get(i));
            } else {
                resultStr.append(list.get(i));
                resultStr.append(split);
            }
        }
        return resultStr.toString();
    }

    public static List<String> strConList(String list, String split) {
        if (StringUtils.isBlank(list)) {
            return new ArrayList<>();
        }
        return Arrays.asList(list.split(split));
    }

//    /**
//     * 从页面树中摘下leafs叶子节点
//     *
//     * @param tree
//     * @param leafs
//     * @return
//     */
//    public static List<DocPagesPojo> pickingLeaves(List<DocPagesPojo> tree, List<String> leafs) {
//        ArrayList<DocPagesPojo> result = new ArrayList<>();
//        for (DocPagesPojo docPagesPojo : tree) {
//            String key = docPagesPojo.getKey();
//            if (leafs.contains(key)) {
//                List<DocPagesPojo> child = docPagesPojo.getChildren();
//                List<DocPagesPojo> resultTmp = pickingLeaves(child, leafs);
//                docPagesPojo.setChildren(resultTmp);
//                result.add(docPagesPojo);
//            }
//        }
//        return result;
//    }

//    /**
//     * 两树合并
//     *
//     * @param main
//     * @param child
//     * @return
//     */
//    public static List insertIntoChildNodes(List<DocOrgPojo> main, Map<String, DocDepartPojo> child) {
//        if (child == null || child.size() == 0) {
//            return main;
//        }
//        for (DocOrgPojo docOrgPojo : main) {
//            List children = docOrgPojo.getChildren();
//            List docOrgPojos = insertIntoChildNodes(children, child);
//            String orgId = docOrgPojo.getOrgId();
//            if (child.keySet().contains(orgId)) {
//                DocDepartPojo docDepartPojo = child.get(orgId);
//                List<DocDepartPojo> childrenDepart = docDepartPojo.getChildren();
//                if (childrenDepart != null && childrenDepart.size() != 0) {
//                    docOrgPojos.addAll(childrenDepart);
//                }
//            }
//            docOrgPojo.setChildren(docOrgPojos);
//        }
//        return main;
//    }
//
//    /**
//     * 获取所有子节点
//     *
//     * @param main
//     * @return
//     */
//    public static List<DocOrgPojo> getTreeChildList(List<DocOrgPojo> main) {
//        if (main == null || main.size() == 0) {
//            return main;
//        }
//        ArrayList<DocOrgPojo> result = new ArrayList<>();
//        for (DocOrgPojo docOrgPojo : main) {
//            List<DocOrgPojo> children = docOrgPojo.getChildren();
//            if (children != null && children.size() != 0) {
//                List<DocOrgPojo> treeChildList = getTreeChildList(children);
//                result.addAll(treeChildList);
//            }
//            docOrgPojo.setChildren(null);
//            result.add(docOrgPojo);
//        }
//        return result;
//    }
//
//    /**
//     * 获取所有子节点Key
//     *
//     * @param main
//     * @return
//     */
//    public static List<String> getTreeChildKeys(List<DocOrgPojo> main) {
//        if (main == null || main.size() == 0) {
//            return new ArrayList<>();
//        }
//        ArrayList<String> result = new ArrayList<>();
//        for (DocOrgPojo docOrgPojo : main) {
//            List<DocOrgPojo> children = docOrgPojo.getChildren();
//            if (children != null && children.size() != 0) {
//                List<String> treeChildList = getTreeChildKeys(children);
//                result.addAll(treeChildList);
//            }
//            result.add(docOrgPojo.getOrgId());
//        }
//        return result;
//    }
//
//    /**
//     * 获取部门所有子节点Key
//     *
//     * @param main
//     * @return
//     */
//    public static List<String> getDepartChildKeys(List<DocDepartPojo> main) {
//        if (main == null || main.size() == 0) {
//            return new ArrayList<>();
//        }
//        ArrayList<String> result = new ArrayList<>();
//        for (DocDepartPojo docOrgPojo : main) {
//            List<DocDepartPojo> children = docOrgPojo.getChildren();
//            if (children != null && children.size() != 0) {
//                List<String> treeChildList = getDepartChildKeys(children);
//                result.addAll(treeChildList);
//            }
//            result.add(docOrgPojo.getDepartId());
//        }
//        return result;
//    }
//
//    public static List<DocDepartPojo> setDepartPathMothod(List<DocDepartPojo> main, List<String> keyPath) {
//        if (main != null || main.size() != 0) {
//            Integer i = 0;
//            for (DocDepartPojo docDepartPojo : main) {
//                String departId = docDepartPojo.getDepartId();
//                if (i != 0) {
//                    keyPath.remove(keyPath.size() - 1);
//                }
//                keyPath.add(departId);
//                docDepartPojo.setDepartPath(new ArrayList<>(keyPath));
//                List<DocDepartPojo> children = docDepartPojo.getChildren();
//                setDepartPathMothod(children, new ArrayList<>(keyPath));
//                i++;
//            }
//        }
//        return main;
//    }
//
//    public static Map<String, List<String>> getDepartPathList(List<DocDepartPojo> main, List<String> keyPath) {
//        HashMap<String, List<String>> result = new HashMap<>();
//        if (main != null || main.size() != 0) {
//            Integer i = 0;
//            for (DocDepartPojo docDepartPojo : main) {
//                String departId = docDepartPojo.getDepartId();
//                String departName = docDepartPojo.getDepartName();
//                if (i != 0) {
//                    keyPath.remove(keyPath.size() - 1);
//                }
//                keyPath.add(departId + "||" + departName);
//                docDepartPojo.setDepartPath(new ArrayList<>(keyPath));
//                result.put(departId, new ArrayList<>(keyPath));
//                List<DocDepartPojo> children = docDepartPojo.getChildren();
//                Map<String, List<String>> departPathList = getDepartPathList(children, new ArrayList<>(keyPath));
//                result.putAll(departPathList);
//                i++;
//            }
//        }
//        return result;
//    }


    public static JSONObject parseTextStructure0(JSONObject jsonObject, Map<String, Integer> map) {
        JSONObject res = new JSONObject();
        String id = MapUtils.getString(jsonObject, "id", null);
        if (StringUtils.isNotBlank(id)) {
            if (map.keySet().contains(id)) {
                res.put("summary", map.get(id));
            }
        }
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof JSONObject) {
                JSONObject res0 = parseTextStructure0((JSONObject) value, map);
                res.put(key, res0);
            } else if (value instanceof JSONArray) {
                JSONArray res1 = parseTextStructure1((JSONArray) value, map);
                res.put(key, res1);
            } else {
                res.put(key, value);
            }
        }
        return res;
    }

    public static JSONArray parseTextStructure1(JSONArray jsonArray, Map<String, Integer> map) {
        JSONArray res = new JSONArray();
        for (Object o : jsonArray) {
            if (o instanceof JSONObject) {
                res.add(parseTextStructure0((JSONObject) o, map));
            } else {
                return jsonArray;
            }
        }
        return res;
    }

    public static String parseTextStructure2(Map map) {
        String result = "";
        String type = MapUtils.getString(map, "type");
        Object text = map.get("text");
        if (text != null && !"".equals(text)) {
            if (text instanceof List) {
                result = result + parseTextStructure2((List) text);
            } else {
                result = result + text;
            }
        }
        Object value = map.get("value");
        if (value != null && !"".equals(value)) {
            if (value instanceof List) {
                result = result + parseTextStructure2((List) value);
            } else {
                result = result + value;
            }
        }
        if ("p".equals(type) || "h1".equals(type) || "h2".equals(type) || "h3".equals(type) || "h4".equals(type) || "h5".equals(type)
                || "h6".equals(type) || "h7".equals(type) || "h8".equals(type) || "h9".equals(type) || "h10".equals(type)) {
            result = result + "\n";
        }
        if ("blank".equals(type)) {
            result = result + "_______";
        }
        map.remove("type");
        map.remove("text");
        map.remove("value");
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
            Object value1 = entry.getValue();
            if (value1 instanceof List) {
                result = result + parseTextStructure2((List) value1);
            }
            if (value1 instanceof Map) {
                result = result + parseTextStructure2((Map) value1);
            }
        }
        return result;
    }

    public static String parseTextStructure2(List list) {
        String result = "";
        for (Object o : list) {
            if (o instanceof Map) {
                if (o != null) {
                    result = result + parseTextStructure2((Map) o);
                }
            }
        }
        return result;
    }

    public static JSONObject parseTextStructure3(JSONObject json, String key, String value) {
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            String key1 = entry.getKey();
            Object value1 = entry.getValue();
            if (key1.equals(key)) {
                if (value.equals(value1)) {
                    return json;
                }
            }
            if (value1 instanceof JSONObject) {
                JSONObject jsonObject = parseTextStructure3((JSONObject) value1, key, value);
                if (jsonObject != null) {
                    return jsonObject;
                }
            } else if (value1 instanceof JSONArray) {
                JSONObject jsonObject = parseTextStructure4((JSONArray) value1, key, value);
                if (jsonObject != null) {
                    return jsonObject;
                }
            }
        }
        return null;
    }

    public static JSONObject parseTextStructure4(JSONArray jsonArray, String key, String value) {
        for (Object o : jsonArray) {
            if (o instanceof JSONObject) {
                JSONObject jsonObject = parseTextStructure3((JSONObject) o, key, value);
                if (jsonObject != null) {
                    return jsonObject;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        String v1 = "{\"k\":\"1\",\"in\":\"1\",\"in1\":{\"k\":\"2\",\"in\":\"2\",\"in1\":{\"k\":\"3\",\"in\":\"3\",\"in1\":[{\"k\":\"4.1\",\"value\":\"4.1\"},{\"k\":\"4.2\",\"value\":\"4.2\"}]}}}";
        JSONObject jsonObject = JSON.parseObject(v1);
        JSONObject s = parseTextStructure3(jsonObject,"k","4.2");
        System.out.println(s.toJSONString());
    }

}
