package cn.nebulaedata.utils;

import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.pojo.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.security.Escape;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.util.DigestUtils;
//import com.github.houbb.word.checker.util.ZhWordCheckers;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author 徐衍旭
 * @date 2021/3/5 14:09
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */
public class JsonKeyUtils {

    /**
     * 获取所有value 无论它在第几层
     */
    public static List<String> test8(String json) {
        Map<String, List<String>> map = JSON.parseObject(json, Map.class);
        ArrayList<String> Strings1 = new ArrayList<>();
        for (String entry : map.keySet()) {
            List<String> strings = map.get(entry);
            for (String s : strings) {
                Strings1.add(s);
            }
        }
        return Strings1;
    }

    /**
     * 数组转纯净版字符串
     *
     * @param list
     * @param separator
     * @return
     */
    public static String listToString(List list, char separator) {
        if (list == null) {
            return "";
        }
        return org.apache.commons.lang.StringUtils.join(list.toArray(), separator);
    }

    public static String listToString(List list, String separator) {
        if (list == null) {
            return "";
        }
        return org.apache.commons.lang.StringUtils.join(list.toArray(), separator);
    }

    public static String listToString(List list) {
        if (list == null) {
            return "";
        }
        return org.apache.commons.lang.StringUtils.join(list.toArray(), ",");
    }

    /**
     * 数组转纯净版字符串
     *
     * @param str
     * @return
     */
    public static List<String> stringToList(String str) {
        if (str == null || str.equals("")) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(str.split(",")));
    }

    public static List<String> stringToList(String str, String separator) {
        if (str == null || str.equals("")) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(str.split(separator)));
    }


    public static List<String> oneClear(List<String> arr) {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < arr.size(); i++) {
            if (!list.contains(arr.get(i))) {
                list.add(arr.get(i));
            }
        }
        return list;
    }

    /**
     * 统计list中某个值出现了几次,输出map
     *
     * @param arr
     * @return
     */
    public static Map<String, Integer> oneClear2(List<String> arr) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < arr.size(); i++) {
            if (!map.keySet().contains(arr.get(i))) {
                map.put(arr.get(i), 1);
            } else {
                Integer x = map.get(arr.get(i));
                map.put(arr.get(i), x + 1);
            }
        }
        return map;
    }


    /**
     * 返回Map<descId,List[research1,research2....]>
     *
     * @param arr
     * @return
     */
    public static Map<String, List<String>> oneClear3(List<Map<String, String>> arr) {
        Map<String, List<String>> map = new HashMap<>();
        List<String> keytmp = new ArrayList<>();  // 存放出现过的key
        for (Map<String, String> ss : arr) {
            for (String k : ss.keySet()) { // 只循环一次
                if (keytmp.contains(k)) {
                    map.get(k).add(ss.get(k));
                } else {
                    List tmpList = new ArrayList();
                    tmpList.add(ss.get(k));
                    map.put(k, tmpList);
                    keytmp.add(k);
                }
            }
        }
        return map;
    }


    /**
     * 阿拉伯数字输出成中文
     *
     * @return
     */
    public static String numberToChinese(Object inputNumber) throws Exception {
        String str = "";
        try {
            if (inputNumber instanceof String) {
                str = String.valueOf(inputNumber);
            } else if (inputNumber instanceof Integer) {
                str = String.valueOf(inputNumber);
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            throw e;
        }
        String ret = "";
        char[] arr = {'十', '百', '千'};
        char[] brr = {'万', '亿'};
        char[] crr = {'一', '二', '三', '四', '五', '六', '七', '八', '九'};
//        Scanner sc=new Scanner(System.in);
//        String str=sc.nextLine();
        int len = str.length();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != 0) {
                break;
            } else {
                len--;
            }
        }
        int a = len / 4;
        int b = len % 4;
        if (b == 0) {
            b = 4;
            a = a - 1;
        }
        for (int i = str.length() - len; i < str.length(); ) {
            boolean flag = false;
            while (b >= 1) {
                if (b == 3 && str.charAt(i) == '0' && str.charAt(i - 1) != '0' && str.charAt(i + 2) != '0' && str.charAt(i + 1) != '0') {
                    ret = ret + "零";
//                    System.out.print("零");
                } else if (b == 2 && str.charAt(i) == '0' && str.charAt(i - 1) != '0' && str.charAt(i + 1) != '0') {
                    ret = ret + "零";
//                    System.out.print("零");
                } else if (str.charAt(i) == '0') {
                    ;
                } else if (b >= 2) {
                    flag = true;
                    ret = ret + crr[str.charAt(i) - '1'] + "" + arr[b - 2];
//                    System.out.print(crr[str.charAt(i)-'1']+""+arr[b-2]);

                } else {
                    flag = true;
                    char c = crr[str.charAt(i) - '1'];
                    ret = ret + String.valueOf(c);
//                    System.out.print(crr[str.charAt(i) - '1']);
                }
                i++;
                b--;
            }
            if (flag == true && a >= 1) {
                ret = ret + brr[a - 1];
//                System.out.print(brr[a-1]);
            }
            a--;
            b = 4;

        }
        ret = ret.replaceAll("^一十", "十");
        return ret;
    }


    /**
     * 第一个List比第二个List多的元素
     * [a,b,c],[b,c,d]
     * 返回[a]
     *
     * @param
     * @return
     */
    public static List<String> listSub(List<String> arr1, List<String> arr2) {
        List<String> tmp = new ArrayList<>();
        for (String ss : arr1) {
            if (arr2.contains(ss)) {
                continue;
            } else {
                tmp.add(ss);
            }
        }
        return tmp;
    }

    /**
     * 第一个List可以完全覆盖第二个list
     * [a,b,c],[b,c,d]
     * 返回[false]
     * [a,b,c],[b,c]
     * 返回[true]
     *
     * @param
     * @return
     */
    public static boolean containsAll(List<String> arr1, List<String> arr2) {
        if (arr2 == null || arr2.size() == 0) {
            return true;
        }
        for (String ss : arr2) {
            if (arr1.contains(ss)) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }


    /**
     * 第一个List包含任意第二个list中的元素
     * [a,b,c],[b,c,d]
     * 返回[true]
     * [a,b,c],[b,c]
     * 返回[true]
     * [a,b,c],[e]
     * 返回[false]
     *
     * @param
     * @return
     */
    public static boolean containsAny(List<Object> arr1, List<Object> arr2) {
        for (Object ss : arr2) {
            if (arr1.contains(ss)) {
                return true;
            } else {
                continue;
            }
        }
        return false;
    }


    /**
     * 根据List中每一个Map中的某一个key的value排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<Map<String, Object>> orderMapList(List<Map<String, Object>> list, String key) {
        List<Map<String, Object>> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (JSON.toJSONString(list.get(0).get(key)).compareTo(JSON.toJSONString(list.get(1).get(key))) < 0) {
                return list;
            } else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                Map<String, Object> minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (JSON.toJSONString(minMap.get(key)).compareTo(JSON.toJSONString(list.get(i + 1).get(key))) < 0) {
                        continue;
                    } else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }

    /**
     * 根据List中每一个Map中的某一个key的时间字段排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<DocFileIndexPojo> orderMapTimeList(List<DocFileIndexPojo> list, String key) {
        List<DocFileIndexPojo> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (((Date) list.get(0).getCreateTime()).after((Date) list.get(1).getCreateTime())) {
                return list;
            } else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                DocFileIndexPojo minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (((Date) minMap.getCreateTime()).after((Date) list.get(i + 1).getCreateTime())) {
                        continue;
                    } else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }

    /**
     * 根据List中每一个Map中的某一个key的value排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<Map<String, Object>> orderMapList1(List<Map<String, Object>> list, String key) {
        List<Map<String, Object>> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            list.get(0).put(key, ((String) list.get(0).get(key)).replaceAll("^<.+>", ""));
            return list;
        }
        if (list.size() == 2) {
            if (JSON.toJSONString(list.get(0).get(key)).compareTo(JSON.toJSONString(list.get(1).get(key))) < 0) {
                list.get(0).put(key, ((String) list.get(0).get(key)).replaceAll("^<.+>", ""));
                list.get(1).put(key, ((String) list.get(1).get(key)).replaceAll("^<.+>", ""));
                return list;
            } else {
                list.get(0).put(key, ((String) list.get(0).get(key)).replaceAll("^<.+>", ""));
                list.get(1).put(key, ((String) list.get(1).get(key)).replaceAll("^<.+>", ""));
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                Map<String, Object> minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (JSON.toJSONString(minMap.get(key)).compareTo(JSON.toJSONString(list.get(i + 1).get(key))) < 0) {
                        continue;
                    } else {
                        minMap = list.get(i + 1);
                    }
                }
                minMap.put(key, ((String) minMap.get(key)).replaceAll("^<.+>", ""));
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }

    /**
     * 根据List中每一个Map中的similar的值排序
     * 输入 [{"similar":"2"},{"similar":"1"},{"similar":"3"}]
     * 返回 [{"similar":"3"},{"similar":"2"},{"similar":"1"}]
     *
     * @param
     * @return
     */
    public static List<DocFileIndexPojo> orderMapList2(List<DocFileIndexPojo> list) {
        List<DocFileIndexPojo> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (JSON.toJSONString(list.get(0).getSimilar()).compareTo(JSON.toJSONString(list.get(1).getSimilar())) > 0) {
                return list;
            } else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                DocFileIndexPojo minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (JSON.toJSONString(minMap.getSimilar()).compareTo(JSON.toJSONString(list.get(i + 1).getSimilar())) > 0) {
                        continue;
                    } else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }

    /**
     * 根据List中每一个Map中的某一个key的value排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<Map<String, String>> orderMapList3(List<Map<String, String>> list, String key) {
        List<Map<String, String>> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (Integer.valueOf(list.get(0).get(key))<Integer.valueOf(list.get(1).get(key))) {
                return list;
            }
//            if (JSON.toJSONString(list.get(0).get(key)).compareTo(JSON.toJSONString(list.get(1).get(key))) < 0) {
//                return list;
//            }
            else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                Map<String, String> minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (Integer.valueOf(minMap.get(key))<Integer.valueOf(list.get(i + 1).get(key))) {
                        continue;
                    }
//                    if (JSON.toJSONString(minMap.get(key)).compareTo(JSON.toJSONString(list.get(i + 1).get(key))) < 0) {
//
//                    }
                    else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }

    /**
     * 根据List中每一个Map中的某一个key的value排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<Map<String, Object>> orderMapList3_1(List<Map<String, Object>> list, String key) {
        List<Map<String, Object>> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (JSON.toJSONString(list.get(0).get(key)).compareTo(JSON.toJSONString(list.get(1).get(key))) < 0) {
                return list;
            } else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                Map<String, Object> minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if ((Integer.valueOf((String)minMap.get(key)))<(Integer.valueOf((String) list.get(i + 1).get(key)))) {
                        continue;
                    }
//                    if (JSON.toJSONString(minMap.get(key)).compareTo(JSON.toJSONString(list.get(i + 1).get(key))) < 0) {
//                        continue;
//                    }
                    else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }

    /**
     * 根据List中每一个Map中的某一个key的value排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<TagPojo> orderMapList4(List<TagPojo> list) {
        List<TagPojo> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (Integer.valueOf(list.get(0).getOutlineOrder()) < Integer.valueOf(list.get(1).getOutlineOrder())) {
                return list;
            }
//            if (JSON.toJSONString(list.get(0).getOutlineOrder()).compareTo(JSON.toJSONString(list.get(1).getOutlineOrder())) < 0) {
//                return list;
//            }
            else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                TagPojo minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (Integer.valueOf(minMap.getOutlineOrder()) < Integer.valueOf(list.get(i + 1).getOutlineOrder())) {
                        continue;
                    }
//                    if (JSON.toJSONString(minMap.getOutlineOrder()).compareTo(JSON.toJSONString(list.get(i + 1).getOutlineOrder())) < 0) {
//                        continue;
//                    }
                    else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }

    /**
     * 根据List中每一个Map中的某一个key的value排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<HfAssessModelElementPojo> orderMapList5(List<HfAssessModelElementPojo> list) {
        List<HfAssessModelElementPojo> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (Integer.valueOf(list.get(0).getOrder())<Integer.valueOf(list.get(1).getOrder())) {
                return list;
            } else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                HfAssessModelElementPojo minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (Integer.valueOf(minMap.getOrder())<Integer.valueOf(list.get(i + 1).getOrder())) {
                        continue;
                    } else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }
    /**
     * 根据List中每一个Map中的某一个key的value排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<HfFastAssessElementPojo> orderMapList51(List<HfFastAssessElementPojo> list) {
        List<HfFastAssessElementPojo> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (Integer.valueOf(list.get(0).getOrder())<Integer.valueOf(list.get(1).getOrder())) {
                return list;
            } else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                HfFastAssessElementPojo minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (Integer.valueOf(minMap.getOrder())<Integer.valueOf(list.get(i + 1).getOrder())) {
                        continue;
                    } else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }

    /**
     * 根据List中每一个Map中的某一个key的value排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<HfContentAssessElementPojo> orderMapList6(List<HfContentAssessElementPojo> list) {
        List<HfContentAssessElementPojo> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (Integer.valueOf(list.get(0).getOrder())<Integer.valueOf(list.get(1).getOrder())) {
                return list;
            } else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                HfContentAssessElementPojo minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (Integer.valueOf(minMap.getOrder())<Integer.valueOf(list.get(i + 1).getOrder())) {
                        continue;
                    } else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }


    /**
     * 根据List中每一个Map中的某一个key的value排序
     * 输入 [{"key":"2"},{"key":"1"},{"key":"3"}]
     * 返回 [{"key":"1"},{"key":"2"},{"key":"3"}]
     *
     * @param
     * @return
     */
    public static List<Map<String, String>> orderMapList7(List<Map<String, String>> list) {
        List<Map<String, String>> retList = new ArrayList<>();
        // 快排
        // 老list循环一遍 找出最小的
        if (list.size() == 0) {
            return list;
        }
        if (list.size() == 1) {
            return list;
        }
        if (list.size() == 2) {
            if (Integer.valueOf(list.get(0).get("cnt"))>Integer.valueOf(list.get(1).get("cnt"))) {
                return list;
            } else {
                retList.add(list.get(1));
                retList.add(list.get(0));
                return retList;
            }
        }
        if (list.size() >= 2) {
            int k = 0;
            while (k < list.size()) {
                Map<String, String> minMap = list.get(0);
                for (int i = 0; i < list.size() - 1; i++) {
                    if (Integer.valueOf(minMap.get("cnt"))>Integer.valueOf(list.get(i + 1).get("cnt"))) {
                        continue;
                    } else {
                        minMap = list.get(i + 1);
                    }
                }
                retList.add(minMap);
                list.remove(minMap);
            }
            return retList;
        }
        return null;
    }

    /**
     * 对List<String>进行排序
     *
     * @param list
     * @return
     */
    public static List<String> listSort(List<String> list) {
        Collections.sort(list);
        return list;
    }

    /**
     * 检测密码的合法性
     * 规则说明：
     * 1.密码不能含有空格字符串
     * 2.密码只能包括字母和数字
     *
     * @param
     * @return
     */
    public static boolean isValid(String password) {
        if (password.length() > 12 || password.length() < 6) {
            return false;
        }

        if (password.length() > 0) {
            //判断是否有空格字符串
            for (int t = 0; t < password.length(); t++) {
                String b = password.substring(t, t + 1);
                if (b.equals(" ")) {
                    System.out.println("有空字符串");
                    return false;
                }
            }


            //判断是否有汉字
            int count = 0;
            String regEx = "[\\u4e00-\\u9fa5]";
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(password);
            while (m.find()) {
                for (int i = 0; i <= m.groupCount(); i++) {
                    count = count + 1;
                }
            }

            if (count > 0) {
                System.out.println("有汉字");
                return false;
            }


            //判断是否是字母和数字
            int numberCounter = 0;
            for (int i = 0; i < password.length(); i++) {
                char c = password.charAt(i);
                if (!Character.isLetterOrDigit(c)) {
                    return false;
                }
                if (Character.isDigit(c)) {
                    numberCounter++;
                }
            }

        } else {
            return false;
        }
        return true;
    }


    /**
     * 计算fatherId
     *
     * @param titleMapList
     * @return
     */
    public static List<Map<String, String>> list2tree(List<Map<String, String>> titleMapList) {
        //记录每一级标题最后出现的id
        Map<String, String> newIdMap = new HashMap<>();
        //记录上一个节点信息
        Map<String, String> lastMap = new HashMap<>();
        for (Map<String, String> titleMap : titleMapList) {
            String level = titleMap.get("level");
            String id = titleMap.get("id");
            if (lastMap.size() == 0) {
                lastMap.put("level", level);
                lastMap.put("id", id);
                newIdMap.put("L" + level, id);
                continue;
            }

            if (Integer.valueOf(level) > Integer.valueOf(lastMap.get("level"))) {
                titleMap.put("fatherId", lastMap.get("id"));
                lastMap.put("fatherId", lastMap.get("id"));
                lastMap.put("level", level);
                lastMap.put("id", id);
                newIdMap.put("L" + level, id);
                newIdMap.put(id, lastMap.get("fatherId"));
            } else if (Integer.valueOf(level) == Integer.valueOf(lastMap.get("level"))) {
                titleMap.put("fatherId", lastMap.get("fatherId"));
                lastMap.put("fatherId", lastMap.get("fatherId"));
                lastMap.put("level", level);
                lastMap.put("id", id);
                newIdMap.put("L" + level, id);
                newIdMap.put(id, lastMap.get("fatherId"));
            } else if (Integer.valueOf(level) < Integer.valueOf(lastMap.get("level"))) {
                titleMap.put("fatherId", newIdMap.get(newIdMap.get("L" + level)));
                lastMap.put("fatherId", newIdMap.get(newIdMap.get("L" + level)));
                lastMap.put("level", level);
                lastMap.put("id", id);
                newIdMap.put(id, newIdMap.get(newIdMap.get("L" + level)));
                newIdMap.put("L" + level, id);
            }
        }
        return titleMapList;
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
     * 计算标签的相似度
     *
     * @return
     */
    public static String getSimilar(List<String> listA, List<String> listB) {
        int sizeA = listA.size();
        int sizeB = listB.size();
        HashMap<String, Boolean> map = new HashMap();
        for (int i = 0; i < sizeA; i++) {
            map.put(listA.get(i), true);
        }
        List<String> listC = new ArrayList<>();
        for (int i = 0; i < sizeB; i++) {
            String x = listB.get(i);
            if (map.containsKey(x)) {
                listC.add(x);
            }
        }
        DecimalFormat df = new DecimalFormat("0.00");//格式化小数
        String num = df.format(sizeA < sizeB ? (float) listC.size() / sizeB : (float) listC.size() / sizeA);//返回的是String类型
        return num;
    }


    /**
     * 深度遍历
     * 将带有层级的listMap1铺平成没有层级的listMap2 采用深度遍历
     *
     * @return
     */
    public static List<Map<String, Object>> depthOrderTraversalWithRecursive(List<Map> listMap1, String fatherId) {
        List<Map<String, Object>> listMap2 = new ArrayList<>();
        for (Map<String, Object> map : listMap1) {
            if (map.get("children") == null) {
                map.put("fatherId", fatherId);
                listMap2.add(map);
            } else {
                Object children = map.get("children");
                if (children instanceof Boolean) {
                    map.put("fatherId", fatherId);
                    listMap2.add(map);
                } else if (children instanceof List && ((List) map.get("children")).size() == 0) {
                    map.put("fatherId", fatherId);
                    listMap2.add(map);
                } else {
                    Map<String, Object> map1 = new HashMap<>();
                    map1.putAll(map);
                    map1.put("children", new ArrayList());
                    map1.put("fatherId", fatherId);
                    listMap2.add(map1);
                    List list = depthOrderTraversalWithRecursive(((List) map.get("children")), String.valueOf(map.get("key")));
                    listMap2.addAll(list);
                }
            }
        }
        return listMap2;
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
     * 加密方法
     */
    public static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";

    /**
     * Description:
     *
     * @param str
     * @return
     * @throws UnsupportedEncodingException
     * @see
     */
    public static String encodeURI(String str)
            throws UnsupportedEncodingException {
        String isoStr = new String(str.getBytes("UTF8"), "ISO-8859-1");
        char[] chars = isoStr.toCharArray();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            if ((chars[i] <= 'z' && chars[i] >= 'a') || (chars[i] <= 'Z' && chars[i] >= 'A')
                    || chars[i] == '-' || chars[i] == '_' || chars[i] == '.' || chars[i] == '!'
                    || chars[i] == '~' || chars[i] == '*' || chars[i] == '\'' || chars[i] == '('
                    || chars[i] == ')' || chars[i] == ';' || chars[i] == '/' || chars[i] == '?'
                    || chars[i] == ':' || chars[i] == '@' || chars[i] == '&' || chars[i] == '='
                    || chars[i] == '+' || chars[i] == '$' || chars[i] == ',' || chars[i] == '#'
                    || (chars[i] <= '9' && chars[i] >= '0')) {
                sb.append(chars[i]);
            } else {
                sb.append("%");
                sb.append(Integer.toHexString(chars[i]));
            }
        }
        return sb.toString();
    }

    /**
     * Description:
     *
     * @param input
     * @return
     * @see
     */
    public static String encodeURIComponent(String input) {
        if (null == input || "".equals(input.trim())) {
            return input;
        }

        int l = input.length();
        StringBuilder o = new StringBuilder(l * 3);
        try {
            for (int i = 0; i < l; i++) {
                String e = input.substring(i, i + 1);
                if (ALLOWED_CHARS.indexOf(e) == -1) {
                    byte[] b = e.getBytes("utf-8");
                    o.append(getHex(b));
                    continue;
                }
                o.append(e);
            }
            return o.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return input;
    }

    private static String getHex(byte buf[]) {
        StringBuilder o = new StringBuilder(buf.length * 3);
        for (int i = 0; i < buf.length; i++) {
            int n = (int) buf[i] & 0xff;
            o.append("%");
            if (n < 0x10) {
                o.append("0");
            }
            o.append(Long.toString(n, 16).toUpperCase());
        }
        return o.toString();
    }

    /**
     * 2.base64转图片
     *
     * @param base64Code base64码
     */
    public static void convertBase64ToImage(String base64Code) {
        BufferedImage image = null;
        byte[] imageByte = null;
        try {
            imageByte = DatatypeConverter.parseBase64Binary(base64Code);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
            image = ImageIO.read(new ByteArrayInputStream(imageByte));
            bis.close();
            File outputfile = new File("E:/sealImg.jpg");
            ImageIO.write(image, "jpg", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算两个时间内所有日期
     *
     * @param date1
     * @param date2
     * @return
     * @throws ParseException
     */
    public static List<String> getDays(String date1, String date2) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Long startTime = sdf.parse(date1).getTime();
        Long endTime = sdf.parse(date2).getTime();

        List<String> dateList = new ArrayList<String>();
        Long oneDay = 1000 * 60 * 60 * 24l;

        Long time = startTime;
        while (time <= endTime) {
            Date d = new Date(time);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String date = df.format(d);
//            System.out.println(date);
            dateList.add(date);
            time += oneDay;
        }
        return dateList;
    }

    /**
     * @param num ﹣3 三天前 +3 三天后
     * @return
     */
    public static String getDayAgoOrAfterString(int num) {
        Calendar calendar1 = Calendar.getInstance();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        calendar1.add(Calendar.DATE, num);
        return sdf1.format(calendar1.getTime());
    }

    /**
     * 判断某天是周几
     *
     * @param date
     * @return
     */
    public static int dateToWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        //因为数组下标从0开始，而返回的是数组的内容，是数组{1,2,3,4,5,6,7}中用1~7来表示，所以要减1
        int week = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (week == 0) {
            week = 7;
        }
        return week;
    }

    /**
     * 判断某天是周几
     *
     * @param dateStr
     * @return
     */
    public static int dateToWeek(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Long dateLong = sdf.parse(dateStr).getTime();
        Date date = new Date(dateLong);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        //因为数组下标从0开始，而返回的是数组的内容，是数组{1,2,3,4,5,6,7}中用1~7来表示，所以要减1
        int week = cal.get(Calendar.DAY_OF_WEEK) - 1;
        return week;
    }

    /**
     * 判断某天是第几周
     *
     * @param dateStr
     * @return
     */
    public static int weekOfYear(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Long dateLong = sdf.parse(dateStr).getTime();
        Date date = new Date(dateLong);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        //因为数组下标从0开始，而返回的是数组的内容，是数组{1,2,3,4,5,6,7}中用1~7来表示，所以要减1
        int week = cal.get(Calendar.WEEK_OF_YEAR) - 1;
        return week;
    }

    /**
     * 去除html代码中含有的标签
     *
     * @param htmlStr
     * @return
     */
    public static String delHtmlTags(String htmlStr) {
        Document doc = Jsoup.parse(htmlStr);
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
        Elements elements = doc.select("parameter");  // paramsPojo.getUuid()是以|隔开的
        Elements elementsBlock = doc.select("parameterblock");  // paramsPojo.getUuid()是以|隔开的
        elements.addAll(elementsBlock);
        for (Element element : elements) {
            String placeholder = element.attr("placeholder");
//            if ("招标人用的附件参数".equals(placeholder))
//                System.out.println("11111");
//            htmlStr = htmlStr.replaceAll(element.toString(), placeholder);
            element.before(placeholder);
            element.remove();
        }
//        htmlStr = doc.toString().replaceAll("\\&quot;", "").replaceAll("\\s*\\n\\s*", "");

        //定义script的正则表达式，去除js可以防止注入
        String scriptRegex = "<script[^>]*?>[\\s\\S]*?<\\/script>";
        //定义style的正则表达式，去除style样式，防止css代码过多时只截取到css样式代码
        String styleRegex = "<style[^>]*?>[\\s\\S]*?<\\/style>";
        //定义HTML标签的正则表达式，去除标签，只提取文字内容
//        String htmlRegex = "<(?!/?(parameter|parameterBlock))(?!/p)(?!/h[0-9]+)[^>]+>";
        String htmlRegex = "<(?!/p)(?!/h[0-9]+)[^>]+>";
//        String htmlRegex="<[^>]+>";
        //定义空格,回车,换行符,制表符
        String spaceRegex = "\\s*|\t|\r|\n";

        // 过滤script标签
        htmlStr = htmlStr.replaceAll(scriptRegex, "");
        // 过滤style标签
        htmlStr = htmlStr.replaceAll(styleRegex, "");
        // 过滤html标签
        htmlStr = htmlStr.replaceAll(htmlRegex, "");
        // 过滤空格等
//        htmlStr = htmlStr.replaceAll(spaceRegex, "");
        // 过滤&nbsp;
        htmlStr = htmlStr.replaceAll("&nbsp;", "");
        // 过滤所有的空格
//        htmlStr = htmlStr.replaceAll(" ", "");
        // 过滤</parameter> </parameterblock>;
        htmlStr = htmlStr.replaceAll("</parameter>", "").replaceAll("</parameterblock>", "");

        return htmlStr.trim(); // 返回文本字符串
    }


    public static void writeExcel(String txtPath,String excelPath,Integer max) {
        //创建Excel文件薄
        HSSFWorkbook workbook = new HSSFWorkbook();
        //创建工作表sheeet
        HSSFSheet sheet0 = workbook.createSheet();  // 记录数据信息
        ArrayList<List<String>> lists = readTxt(txtPath);

        // 先创建经纬度
        for (int i=0;i<=max;i++) {
            if (i==0) { // 第一行
                HSSFRow row0 = sheet0.createRow(0);
                for (int j=1;j<=max;j++) {
                    row0.createCell(j).setCellValue(j);
                }
            } else { // 创建剩余行 并填满0
                HSSFRow rowi = sheet0.createRow(i);
                rowi.createCell(0).setCellValue(i);
                for (int j=1;j<=max;j++) {
                    rowi.createCell(j).setCellValue(0);
                }

            }
        }

        // 描点
        for (List<String> list : lists) {
            if (sheet0.getRow(Integer.valueOf(list.get(0))) == null) {
                HSSFRow row = sheet0.createRow(Integer.valueOf(list.get(0)));
                HSSFCell cell_title = row.createCell(Integer.valueOf(list.get(1)));
                cell_title.setCellValue(1);
            } else {
                HSSFRow row = sheet0.getRow(Integer.valueOf(list.get(0)));
                HSSFCell cell_title = row.createCell(Integer.valueOf(list.get(1)));
                cell_title.setCellValue(1);
            }

        }

        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(excelPath);
            workbook.write(outputStream);
        } catch (Exception e) {

        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (Exception e) {

            }
            try{
                if(workbook != null){
//                    workbook.close();
                }
            } catch (Exception e){

            }
        }
//        HSSFCell cell_title = null;
        // 设置数据主键
//        for (int j = 0; j < 2; j++) {
            //创建第j+1行
//            HSSFRow row = sheet0.createRow(j);
//            for (int i = 0; i < dataIndexList.size(); i++) {
//                cell_title = row.createCell(i + 1);
//                if (j == 0) {
//                    cell_title.setCellValue(dataIndexList.get(i));
//                } else if (j == 1) {
//                    cell_title.setCellValue(titleList.get(i));
//                }
//            }
//        }


    }

    public static void writeExcel1(String txtPath,String excelPath,Integer max) {
        //创建Excel文件薄
        XSSFWorkbook workbook = new XSSFWorkbook();
        //创建工作表sheeet
        XSSFSheet sheet0 = workbook.createSheet();  // 记录数据信息
        // 读取文件
        ArrayList<List<String>> lists = readTxt(txtPath);
        ArrayList<String> points = new ArrayList<>();
        for (List<String> list : lists) {
            if (!points.contains(list.get(0))) {
                points.add(list.get(0));
            }
            if (!points.contains(list.get(1))) {
                points.add(list.get(1));
            }
        }

        // 对点排序
        ArrayList<Integer> pointsTmpA = new ArrayList<>();
        ArrayList<Integer> pointsTmpB = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).substring(0,1).equals("a")) {
                pointsTmpA.add(Integer.valueOf(points.get(i).substring(1)));
            }
            if (points.get(i).substring(0,1).equals("b")) {
                pointsTmpB.add(Integer.valueOf(points.get(i).substring(1)));
            }
        }
        // 单独对A队列的数字排序
        Collections.sort(pointsTmpA);
        Collections.sort(pointsTmpB);
        System.out.println(pointsTmpA);
        System.out.println(pointsTmpB);
        ArrayList<String> pointsTmp = new ArrayList<>();
        for (Integer integer : pointsTmpA) {
            pointsTmp.add("a" + String.valueOf(integer));
        }
        for (Integer integer : pointsTmpB) {
            pointsTmp.add("b" + String.valueOf(integer));
        }
        System.out.println(pointsTmp);
        points = pointsTmp;
        max = points.size();

        // 先创建经纬度
        for (int i=0;i<=max;i++) {
            if (i==0) { // 第一行
                XSSFRow row0 = sheet0.createRow(0);
                for (int j=0;j<max;j++) {
                    row0.createCell(j+1).setCellValue(points.get(j));
                }
            } else { // 创建剩余行 并填满0
                XSSFRow rowi = sheet0.createRow(i);
                rowi.createCell(0).setCellValue(points.get(i-1));
                for (int j=1;j<=max;j++) {
                    rowi.createCell(j).setCellValue(0);
                }

            }
        }

        // 描点
        boolean flag = false;
        for (List<String> list : lists) {
            for (Integer i = 0; i <= max; i++) {
                if (list.get(0).equals(points.get(i))) {
                    for (Integer j = 0; j <= max; j++) {
                        if (list.get(1).equals(points.get(j))) {
                            System.out.println(i+","+j);
                            sheet0.getRow(j+1).createCell(i+1).setCellValue(1);
                            flag = true;
                            break;
                        }
                    }
                    if (flag) {
                        flag = false;
                        break;
                    }
                }
            }



//            for (Integer i = 1; i <= max; i++) {
//                if (sheet0.getRow(0).getCell(i).getStringCellValue().equals(list.get(0))) {
//                    for (Integer j = 1; j <= max; j++) {
//                        if (sheet0.getRow(j).getCell(0).getStringCellValue().equals(list.get(1))) {
//                            System.out.println(i+","+j);
//                            sheet0.createRow(j).createCell(i).setCellValue(1);
//                            flag = true;
//                            break;
//                        }
//                    }
//                    if (flag) {
//                        flag = false;
//                        break;
//                    }
//                }
//            }
        }

        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(excelPath);
            workbook.write(outputStream);
        } catch (Exception e) {

        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (Exception e) {

            }
            try{
                if(workbook != null){
//                    workbook.close();
                }
            } catch (Exception e){

            }
        }

    }

    public static ArrayList<List<String>> readTxt(String txtPath) {
        File file = new File(txtPath);
        if (file.isFile() && file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuffer sb = new StringBuffer();
                String text = null;
                ArrayList<List<String>> strings = new ArrayList<>();
                while ((text = bufferedReader.readLine()) != null) {
                    List<String> list = stringToList(text, " |\t");
                    strings.add(list);

                    ArrayList<String> l = new ArrayList<>();
                    l.add(list.get(1));
                    l.add(list.get(0));
                    strings.add(l);
                }
                return strings;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 去除double类型后多余的 0
     * @param str
     * @return
     */
    public static Double noZeros(String str) {
        double num = Double.parseDouble(str);
        BigDecimal value = new BigDecimal(num);
        //去除多余0
        BigDecimal noZeros = value.stripTrailingZeros();
        //BigDecimal => double
        return noZeros.doubleValue();
    }

    public static Object doubleTrans(double d){
        double eps = 1e-10;
        //判断是否为整数
        if(d-Math.floor(d) < eps){
            return String.valueOf((long)d);
        }
        DecimalFormat df = new DecimalFormat("#.#############");  // 13位小数
        String format = df.format(d);
        try {
            return Integer.parseInt(format);
        } catch (Exception e) {
            return Double.parseDouble(format);
        }
    }

    public static Object str2Number(String str) throws WorkTableException {
        try {
            return doubleTrans(noZeros(str));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new WorkTableException("默认值类型错误");
        }
    }

    public static Double str2Dou(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过反射实体类转map  缺点: 只能返回第一次层的键值对
     * @param entity
     * @param <T>
     * @return
     */
    public static <T> Map<String, Object> convertEntityToMap(T entity) {
        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = entity.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();

            try {
                Object value = field.get(entity);
                map.put(fieldName, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return map;
    }


    /**
     * 校验List<String>中元素的重复性
     * @param list
     * @return
     */
    public static boolean checkStrRepeat(List<String> list) {
//        Map<String, Long> map = list.stream()
//                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
//        System.out.println(JSON.toJSONString(map));

        return list.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values()
                .stream()
                .anyMatch(n -> n > 1);
    }

    /**
     * byte(字节)根据长度转成kb(千字节)和mb(兆字节)
     *
     * @param bytes
     * @return
     */
    public static String bytes2kb(long bytes) {
        BigDecimal filesize = new BigDecimal(bytes);
        BigDecimal megabyte = new BigDecimal(1024 * 1024);
        float returnValue = filesize.divide(megabyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        if (returnValue > 1)
            return returnValue + "mb";
        BigDecimal kilobyte = new BigDecimal(1024);
        returnValue = filesize.divide(kilobyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        return Math.ceil(returnValue) + "kb";
    }

    /**
     * 检测url是否可达
     * @param url
     * @return
     */
    public static int getHttpStatusCode(String url) {
        int statusCode = 0;
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

            // 设置请求方式为GET，注意：如果服务器不接受GET请求，可能会返回错误
            connection.setRequestMethod("GET");

            // 设置通用的请求属性
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // 发起连接
            connection.connect();

            // 获取服务器的响应代码
            statusCode = connection.getResponseCode();

            // 断开连接
            connection.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusCode;
    }

    public static void main(String[] args) throws Exception {

        String urlToTest = "https://www.office.com";
        System.out.println("URL: " + urlToTest + " - Status Code: " + getHttpStatusCode(urlToTest));
        if (getHttpStatusCode(urlToTest) == 200) {
            System.out.println(1);
        } else {
            System.out.println(0);
        }

        System.out.println(bytes2kb(897453645));
        ;

//        ArrayList<HashMap<String, String>> list = new ArrayList<>();
//        list.add(new HashMap<String,String>(){{put("key","1");}});
//        list.add(new HashMap<String,String>(){{put("key","2");}});
//        list.add(new HashMap<String,String>(){{put("key","3");}});
//        list.add(new HashMap<String,String>(){{put("key","4");}});
//        for (HashMap<String, String> map : list) {
//            System.out.println(JSON.toJSONString(map));
//            if (map.get("key").equals("2")) {
//                list.remove(map);
//            }
//        }
//        System.out.println(JSON.toJSONString(list));

//        String s1 = "</p></p></h2>项目建</p></p></h3>";
//        String s2 = "</p></p></h2>项目建</p></p></p></p></h3>";
//        String[] split = s1.split("</p>|</h[0-9]+>");  // delHtmlTags时留的</p>和</h12345678>等
//        List<String> strings = Arrays.asList(split);
//        String[] split2 = s2.split("</p>|</h[0-9]+>",-1);
//        List<String> strings2 = Arrays.asList(split2);
//        System.out.println(strings);
//        System.out.println(strings2);
//
//        String[] split1 = s1.split("</p>");
//        List<String> strings1 = Arrays.asList(split1);
//        System.out.println(strings1);
//
//        String s3 = "</p></p></p></p>";
//        String[] split4 = s3.split("</p>");
//        List<String> strings4 = Arrays.asList(split4);
//        System.out.println(strings4);
//
//
//        String s5 = "ppapp";
//        String[] split5 = s5.split("p");
//        List<String> strings5 = Arrays.asList(split5);
//        System.out.println(strings5);
//
//        String s = delHtmlTags("1.采购条件</h2>项目建设单位为XX（以下简称“项目单位”），项目资金来自企业自有资金，出资比例为100%，资金来源已落实，采购人为XX，并委托XX为采购代理机构。项目已具备采购条件，现对该项目进行邀请竞争性谈判采购。</p>本项目积极响应《中共中央国务院关于开展质量提升行动的指导意见》，倡导潜在应答人用先进标准引领产品质量提升，有效治理质量突出问题，推动形成优质优价的采购机制。</p></p></p></p></p></p>");
//        System.out.println(s);
//        for (int i = 0; i < 255; i++) {
//            System.out.println(i);
//            System.out.println(i);
//        }
//        for (int i = 0; i < 10; i++) {
//            for (int j = 0; j < 4; j++) {
//                if (i==5) {
//                    System.out.println(1111);
//                    break;
//                }
//            }
//            System.out.println(i);
//        }
//
//        System.out.println(StringUtils.isBlank(null));
//        System.out.println(StringUtils.isBlank(""));
//        System.out.println(StringUtils.isBlank("    "));

//        List<String> list = Arrays.asList("A", "B", "B", "C", "C", "D", "E");
//        boolean b = checkStrRepeat(list);
//        System.out.println(b);
//        Object defaultValue = "{770020:10}";
//        if (defaultValue != null) {
//            Map map1 = new HashMap();
//            try {  // 数值 金额  默认值不是map直接认为默认值无效 不返回给前端了
//                map1 = JSON.parseObject(String.valueOf(defaultValue), Map.class);
//                System.out.println(JSON.toJSONString(map1));
//                map1 = JSON.parseObject(String.valueOf(defaultValue), Map.class, Feature.NonStringKeyAsString);
//                System.out.println(JSON.toJSONString(map1));
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//        }
//        ArrayList list = new ArrayList<>();
//        HashMap<String, Object> map1 = new HashMap<>();
//        map1.put("12","21");
//        HashMap<String, Object> map2 = new HashMap<>();
//        map2.put("12",true);
//        list.add(map1);
//        list.add(map2);
//
//        for (Object o : list) {
//            Map<String,String> o1 = (Map) o;
//            String s = o1.get("12");
//            System.out.println(s);
//        }

//        List<String> list = new ArrayList<>(Arrays.asList("1","2","3","4","5","6","7","8","9","1","2","3","4","5","6","7","8","9"));
//        while (list.size()>3) {
//            List<String> list1 = list.subList(0, 3);
//            System.out.println(list1);
//            list.subList(0, 3).clear();
//        }
//        System.out.println(list);
//
//        ArrayList<String> objects = new ArrayList<>();
//        objects.add("'");
//        objects.add("\"");
//        objects.add("\\");
//        objects.add(" ");
//        String s = JSON.toJSONString(objects);
//        System.out.println(s);
//
//        ArrayList<HashMap<String, Object>> list1 = new ArrayList<>();
//        ArrayList<HashMap<String, Object>> list2 = new ArrayList<>();
//        for (int i = 0; i < 5; i++) {
//            HashMap<String, Object> map = new HashMap<>();
//            map.put("uuid",String.valueOf(i));
//            list1.add(map);
//        }
//        for (int i = 3; i < 8; i++) {
//            HashMap<String, Object> map = new HashMap<>();
//            map.put("uuid",String.valueOf(i));
//            list2.add(map);
//        }
//        HashMap<String, Object> map = new HashMap<>();
//        map.put("uuid",String.valueOf(3));
//        list2.add(map);
//        System.out.println(list1);
//        System.out.println(list2);
//
//        List<Map<String, Object>> actionsAdd1 = new ArrayList<>();
//        actionsAdd1.addAll(list1);
//        List<Map<String, Object>> actionsDel1 = new ArrayList<>();
//        actionsDel1.addAll(list2);
//
//
//        for (Map<String, Object> add : list1) {
//            for (Map<String, Object> del : list2) {
//                if (add.get("uuid").equals(del.get("uuid"))) {
//                    actionsAdd1.remove(add);
//                    actionsDel1.remove(del);
//                }
//            }
//        }
//
//        System.out.println(actionsAdd1);
//        System.out.println(actionsDel1);


    }
}
