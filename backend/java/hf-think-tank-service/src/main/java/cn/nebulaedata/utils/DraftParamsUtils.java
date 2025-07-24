package cn.nebulaedata.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 徐衍旭
 * @date 2021/1/18 10:35
 * @note 根据草稿内容doctext获取参数列表paramIdList
 */
public class DraftParamsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DraftParamsUtils.class);

    public static ArrayList<String> getParams(String doctext) {
        JSONObject jsonStr = JSONObject.parseObject(doctext);
        String  param= JSON.toJSONString(jsonStr);
        ArrayList<String > paramIdList=(ArrayList<String>)JSONPath.read(param,"$..paramId");
        return paramIdList;
    }

    public static ArrayList<Map<String ,String >> getParamsRelat(String doctext) {
        ArrayList<Map<String ,String >> paramsRelatList = new ArrayList();
        String reg = "\"paramId\":\"[0-9]{7}\",\"paramRelatUuid\":\"[a-f0-9]{32}\"|\"paramId\":\"[a-f0-9-]{36}\",\"paramRelatUuid\":\"[a-f0-9]{32}\"";
        Pattern p = Pattern.compile(reg);
        Matcher m =p.matcher(doctext);
        while (m.find()){
            System.out.println(m.group(0));
            String paramsRelat = "{"+m.group(0)+"}";
            JSONObject jsonObject1 = JSONObject.parseObject(paramsRelat);
            paramsRelatList.add((Map) jsonObject1);
        }
        return paramsRelatList;
    }

    public static String updateParams(String text,String paramUuid,String paramRelatUuid){
        String oldStr ="\"paramId\":\""+paramUuid+"\"";
        String oldStr2="\"paramId\":\""+paramUuid+"\",\"paramRelatUuid\"";
        String newStr ="\"paramId\":\""+paramUuid+"\",\"paramRelatUuid\":\""+paramRelatUuid+"\"";
        String newText;
        if (text.contains(oldStr2)){
            oldStr2 = "\"paramId\":\""+paramUuid+"\",\"paramRelatUuid\":\"[a-f0-9]{32}\"";
            newText=text.replaceAll(oldStr2,newStr);
        } else {
            newText=text.replace(oldStr,newStr);
        }
        return newText;
    }



    /**
     * 通过KEY查询VALUE
     * @param obj
     * @param name
     * @return
     */
    public static Object getValByKey(Object obj, String name) {
        List<Map<String, Object> > arrayList = new ArrayList<>();
//        Map<String, Object> map = new HashMap<>();
        getValForObj(obj, name, arrayList);
        return arrayList;
    }

    /**
     * 查询值
     * @param obj
     * @param name
     * @param list
     */
    public static void getValForObj(Object obj, String name, List<Map<String, Object>> list) {
        if(obj instanceof JSONObject) {
            getValForJObj((JSONObject)obj, name, list);
        }
        if(obj instanceof JSONArray) {
            getValForJArr((JSONArray)obj, name, list);
        }
    }

    /**
     * 处理JsonObject 对象查值
     * @param jobj
     * @param name
     * @param list
     */
    public static void getValForJObj(JSONObject jobj, String name, List<Map<String, Object>> list) {
        for(Map.Entry<String, Object> entry : jobj.entrySet()) {
            HashMap<String, Object> map = new HashMap();
            if(StringUtils.equals(entry.getKey(), name)) {
                Object paramRelatUuid = MapUtils.getString(jobj, "paramRelatUuid", "");
//                Object paramRelatUuid = jobj.get("paramRelatUuid");
//                System.out.println(String.valueOf(entry.getValue()));
//                System.out.println(paramRelatUuid);
                map.put(name,String.valueOf(entry.getValue()));
                map.put("paramRelatUuid", paramRelatUuid);
//                String jsonString = JSON.toJSONString(map);
                list.add(map);
            }
            getValForObj(entry.getValue(), name, list);
        }
    }

    /**
     * 处理JsonAarray 对象查值
     * @param arr
     * @param name
     * @param list
     */
    public static void getValForJArr(JSONArray arr, String name, List<Map<String, Object>> list) {
        Iterator<Object> iner = arr.iterator();
        while(iner.hasNext()) {
            getValForObj(iner.next(), name, list);
        }
    }


//    public static String ReadDoc(String path){
//        String resullt = "";
//        //首先判断文件中的是doc/docx
//        try {
//            if (path.endsWith(".doc")) {
//                InputStream is = new FileInputStream(new File(path));
//                WordExtractor re = new WordExtractor(is);
//                resullt = re.getText();
//                re.close();
//            } else if (path.endsWith(".docx")) {
//                OPCPackage opcPackage = POIXMLDocument.openPackage(path);
//                POIXMLTextExtractor extractor = new XWPFWordExtractor(opcPackage);
//                resullt = extractor.getText();
//                extractor.close();
//            }
//        } catch(Exception e){
//            e.printStackTrace();
//        }
//        return resullt;
//    }
//
//    public static void main(String[] args) {
//        JSONObject jsonStr = JSONObject.parseObject("{\"id\":\"100000013\",\"paramRelatUuid\":\"4444444\",\"paramId\": \"405b0b62-2912-4a2e-a2e9-6c9bdab5cc22\",\"text\":[{\"type\":\"text\",\"value\":\"标准施工招\"},{\"id\":\"50b212e5-58b0-4f3b-91ef-ab862ce30875\",\"type\":\"blank\",\"paramId\":\"405b0b62-2912-4a2e-a2e9-6c9bdab5cc23\",\"paramRelatUuid\":\"25626245\",\"value\":\"\"},{\"type\":\"text\",\"value\":\"标文件\"},{\"type\":\"text\",\"value\":\"\",\"paramId\": \"405b0b62-2912-4a2e-a2e9-6c9bdab5cc23\"}],\"type\":\"p\"}");
////        String str1 = "\"{\"id\":\"100017279\",\"type\":\"p\",\"text\":[{\"id\":\"100017280\",\"paramId\":\"1000787\",\"paramRelatUuid\":\"6e93bbd7c2974b2f89697eb49d3885a2\",\"type\":\"blank\",\"value\":\"},{\"id\":\"100017281\",\"type\":\"text\",\"value\":\"（项目名称）\"},{\"id\":\"100017282\",\"paramId\":\"1000788\",\"paramRelatUuid\":\"6e93bbd7c2974b2f89697eb49d3885a2\",\"type\":\"blank\",\"value\":\"},{\"id\":\"100017283\",\"type\":\"text\",\"value\":\"标段\"}]}\"";
//        String  param= JSON.toJSONString(jsonStr);
////        System.out.println("test: "+str1);
//        JSONObject jsonObject = JSONObject.parseObject(param);
//        System.out.println(jsonObject.get("paramId"));
//        ArrayList<String > paramIdList= (ArrayList<String>) JSONPath.read(param,"$..id");
//        System.out.println(JSONPath.read(param,"$..id").getClass().toString());
//        System.out.println(cn.nebulaedata.docframeservice.web.utils.DraftParamsUtils.getParams(param));
////        System.out.println(DraftParamsUtils.getParamsRelat(str1));
////        ArrayList<Map<String ,String >> paramsRelatList = DraftParamsUtils.getParamsRelat(str1);
////        for(Map<String ,String > paramsRelat : paramsRelatList){
////            System.out.println(paramsRelat.get("paramId"));
////            System.out.println(paramsRelat.get("paramRelatUuid"));
////            paramsRelat.get("paramId");
////            paramsRelat.get("paramRelatUuid");
////        }
//        System.out.println("--------");
//        System.out.println(cn.nebulaedata.docframeservice.web.utils.DraftParamsUtils.getValByKey(jsonStr,"paramId"));
//        ArrayList paramId = (ArrayList) cn.nebulaedata.docframeservice.web.utils.DraftParamsUtils.getValByKey(jsonStr, "paramId");
//        System.out.println(paramId);
//    }
}
