package cn.nebulaedata.utils;

/**
 * @author 徐衍旭
 * @date 2021/8/25 15:34
 * @note
 */

import java.awt.image.Kernel;
import java.io.*;
import java.util.*;

import com.alibaba.fastjson.JSON;
//import com.spire.doc.FileFormat;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jsoup解析html标签时类似于JQuery的一些符号
 *
 * @author chixh
 */
public class HtmlUtils {
    protected List<List<String>> data = new LinkedList<List<String>>();

    /**
     * 获取value值
     *
     * @param e
     * @return
     */
    public static String getValue(Element e) {
        return e.attr("value");
    }

    /**
     * 获取
     * <tr>
     * 和
     * </tr>
     * 之间的文本
     *
     * @param e
     * @return
     */
    public static String getText(Element e) {
        return e.text();
    }

    /**
     * 识别属性id的标签,一般一个html页面id唯一
     *
     * @param body
     * @param id
     * @return
     */
    public static Element getID(String body, String id) {
        Document doc = Jsoup.parse(body);
        // 所有#id的标签
        Elements elements = doc.select("#" + id);
        // 返回第一个
        return elements.first();
    }

    /**
     * 识别属性class的标签
     *
     * @param body
     * @param classTag
     * @return
     */
    public static Elements getClassTag(String body, String classTag) {
        Document doc = Jsoup.parse(body);
        // 所有#id的标签
        return doc.select("." + classTag);
    }

    /**
     * 获取tr标签元素组
     *
     * @param e
     * @return
     */
    public static Elements getTR(Element e) {
        return e.getElementsByTag("tr");
    }

    /**
     * 获取td标签元素组
     *
     * @param e
     * @return
     */
    public static Elements getTD(Element e) {
        return e.getElementsByTag("td");
    }

    /**
     * 获取表元组
     *
     * @param table
     * @return
     */
    public static List<List<String>> getTables(Element table) {
        List<List<String>> data = new ArrayList<>();

        for (Element etr : table.select("tr")) {
            List<String> list = new ArrayList<>();
            for (Element etd : etr.select("td")) {
                String temp = etd.text();
                //增加一行中的一列
                list.add(temp);
            }
            //增加一行
            data.add(list);
        }
        return data;
    }


    /**
     * 读出城市列表文件
     */
    private static String readHtml(String fileName) {
        File file02 = new File(fileName);
        FileInputStream is = null;
        StringBuilder stringBuilder = null;
        try {
            if (file02.length() != 0) {
                /**
                 * 文件有内容才去读文件
                 */
                is = new FileInputStream(file02);
                InputStreamReader streamReader = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(streamReader);
                String line;
                stringBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    // stringBuilder.append(line);
                    stringBuilder.append(line);
                }
                reader.close();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(stringBuilder);

    }

    /**
     * 读html文件
     *
     * @param fileName
     * @return
     */
    public static List<Map<String, String>> analysisHtml(String fileName) {
        Document doc = Jsoup.parse(readHtml(fileName));
        System.out.println("--------------------");
        Elements x1_x3 = doc.select("p[class~=^X1 (X2|X3|X4|X5|X6|X7)($| .*)]");
        Elements elements = new Elements();
        elements.addAll(x1_x3);
        List<Map<String, String>> titleMapList = new ArrayList();
        int i = 0;
        for (Element element : elements) {
            HashMap<String, String> titleMap = new HashMap<>();
            i += 1;
            if ("".equals(element.text())) {
                continue;
            }
//            System.out.println("["+i+"] "+element.text()+" "+element.attributes().get("class").substring(4,5));
            titleMap.put("title", element.text());
            titleMap.put("level", element.attributes().get("class").substring(4, 5));
            titleMap.put("id", String.valueOf(i));
            titleMapList.add(titleMap);
        }
        List<Map<String, String>> ret = JsonKeyUtils.list2tree(titleMapList);
//        System.out.println(JSON.toJSONString(ret));
        return ret;
    }


//    /**
//     * doc转docx
//     *
//     * @param sourcePath
//     * @param targetPath
//     * @return
//     */
//    public Boolean docToDocx(String sourcePath, String targetPath) {
//        try {
//            com.spire.doc.Document dc = new com.spire.doc.Document();
//            dc.loadFromFile(sourcePath);
//            dc.saveToFile(targetPath, FileFormat.Docx_2013);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    /**
     * 给html增加H标签
     *
     * @param fileName
     * @return
     */
    public static String htmlAddHTag(String fileName, String styleFileName) {
        System.out.println("----------htmlAddHTag----------");
        String htmlStr = readHtml(fileName);
        Document doc = Jsoup.parse(htmlStr);  // Jsoup.parse导致多个连续空格变为一个  弃用
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
        // 剔除表格前的th标签导致的空格
        Elements ths = doc.select("th");  // Parser.xmlParser() 这里使用html解析而不是xml解析 因为可以使th暴露出来然后剔除 有点暴力
        for (Element th : ths) {
            if (StringUtils.isBlank(th.html())) {
                th.remove();
            }
        }

        // 优先使用style做判断
        Map<String, String> docStyleMap = getDocStyle(styleFileName);
        Boolean f = false;
        if (docStyleMap.size() != 0) {
            String selectStr = "";
            selectStr = "p[class~=(^X1 ("
                    + (docStyleMap.get("h1") == null ? "" : ("X" + docStyleMap.get("h1") + "|"))
                    + (docStyleMap.get("h2") == null ? "" : ("X" + docStyleMap.get("h2") + "|"))
                    + (docStyleMap.get("h3") == null ? "" : ("X" + docStyleMap.get("h3") + "|"))
                    + (docStyleMap.get("h4") == null ? "" : ("X" + docStyleMap.get("h4") + "|"))
                    + (docStyleMap.get("h5") == null ? "" : ("X" + docStyleMap.get("h5") + "|"))
                    + (docStyleMap.get("h6") == null ? "" : ("X" + docStyleMap.get("h6") + "|"));

            String substring = selectStr.substring(0, selectStr.length() - 1);
            selectStr = substring + ")($| .*))]";
            Elements x1_x3 = doc.select(selectStr);
            if (x1_x3.size() == 0) {
                f = true;
            }
            for (Element element : x1_x3) {
                if ("".equals(element.text())) {
                    continue;
                }
                if (docStyleMap.get("h1") != null && element.select("p[class~=(^X1 X" + docStyleMap.get("h1") + "($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h1");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (docStyleMap.get("h2") != null && element.select("p[class~=(^X1 X" + docStyleMap.get("h2") + "($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h2");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (docStyleMap.get("h3") != null && element.select("p[class~=(^X1 X" + docStyleMap.get("h3") + "($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h3");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (docStyleMap.get("h4") != null && element.select("p[class~=(^X1 X" + docStyleMap.get("h4") + "($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h4");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (docStyleMap.get("h5") != null && element.select("p[class~=(^X1 X" + docStyleMap.get("h5") + "($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h5");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (docStyleMap.get("h6") != null && element.select("p[class~=(^X1 X" + docStyleMap.get("h6") + "($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h6");
                    element1.html(html);
                    element.replaceWith(element1);
                }
            }
        }

        if (docStyleMap.size() == 0 || f) {
            Elements x1_x3 = doc.select("p[class~=(^X1 (X2|X3|X4|X5|X6|X7)($| .*)|^a (X1|X2|X3|X4|X5|X6)($| .*)|^Normal Heading(1|2|3|4|5|6)($| .*))]");
            Elements elements = new Elements();
            elements.addAll(x1_x3);
            List<Map<String, String>> titleMapList = new ArrayList();
            for (Element element : elements) {
                if ("".equals(element.text())) {
                    continue;
                }
                if (element.select("p[class~=(^X1 X2($| .*)|^a X1($| .*)|^Normal Heading1($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h1");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (element.select("p[class~=(^X1 X3($| .*)|^a X2($| .*)|^Normal Heading2($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h2");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (element.select("p[class~=(^X1 X4($| .*)|^a X3($| .*)|^Normal Heading3($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h3");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (element.select("p[class~=(^X1 X5($| .*)|^a X4($| .*)|^Normal Heading4($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h4");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (element.select("p[class~=(^X1 X6($| .*)|^a X5($| .*)|^Normal Heading5($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h5");
                    element1.html(html);
                    element.replaceWith(element1);
                } else if (element.select("p[class~=(^X1 X7($| .*)|^a X6($| .*)|^Normal Heading6($| .*))]").size() != 0) {
                    String html = element.html();
                    Element element1 = new Element("h6");
                    element1.html(html);
                    element.replaceWith(element1);
                }
            }
        }
        return doc.toString();
    }

    /**
     * 给html增加H标签
     *
     * @param fileName
     * @return
     */
    public static String htmlAddHTag2(String fileName) throws DocumentException {
        System.out.println("----------htmlAddHTag----------");
        String htmlStr = readHtml(fileName);

//        org.dom4j.Document document = null;
//        try {
//            document = DocumentHelper.parseText(htmlStr);  // 必须被同一标签包裹
//        } catch (Exception e) {
//            document = DocumentHelper.parseText("<html>" + htmlStr.replaceAll("&nbsp;", " ") + "</html>");  // 必须被同一标签包裹
//        }
//        String xpath = "//p[@class='^X1 (X2|X3|X4|X5|X6|X7)($| .*)|^a (X1|X2|X3|X4|X5|X6)($| .*)|^Normal Heading(1|2|3|4|5|6)($| .*)']";
//        List<Node> listNode = document.selectNodes(xpath);
//        for (Node o : listNode) {
//            System.out.println("       o.asXML() : "+o.asXML());
////            document.remove(o);
////            System.out.println("      document   : "+document.asXML());
//            o.getParent().remove(o);
//            System.out.println("      document2  : "+document.asXML());
////            String s3 = "<html><h4><parameter uuid=\"\" key=\"\" typeid=\"\" placeholder=\"\" title=\"\" isunderline=\"0\" styleid=\"\" unit=\"\" matrixmode=\"column\" value=\"\" active=\"false\"><p><span style=\"font-family: 华文细黑\">5. 材料和工程设备</span></p></parameter></h4></html>";
////            Document document3 = DocumentHelper.parseText(s3);
////            Node node3 = document3.selectSingleNode("/html");
////            document.remove(node3);
////            System.out.println("      document1  : "+document.asXML());
//
//            org.dom4j.Element element = (org.dom4j.Element) o;
//            element.attributeValue("id","234");
//            String  data = "<html>" + o.getText() + "</html>";
//            org.dom4j.Document document4 = DocumentHelper.parseText(data);
//            Node node4 = document4.selectSingleNode("/html");
//            org.dom4j.Element node5 = (org.dom4j.Element) node4;
//            element.appendContent(node5);
//        }


        String regEx_html_h1 = "(?<!<h1>)<p class=\"(a X1(\"| )|X1 X2(\"| )|Normal Heading1(\"| ))[^>]*>.+?</p>"; //定义HTML标签的正则表达式
        String regEx_html_h2 = "(?<!<h2>)<p class=\"(a X2(\"| )|X1 X3(\"| )|Normal Heading2(\"| ))[^>]*>.+?</p>"; //定义HTML标签的正则表达式
        String regEx_html_h3 = "(?<!<h3>)<p class=\"(a X3(\"| )|X1 X4(\"| )|Normal Heading3(\"| ))[^>]*>.+?</p>"; //定义HTML标签的正则表达式
        String regEx_html_h4 = "(?<!<h4>)<p class=\"(a X4(\"| )|X1 X5(\"| )|Normal Heading4(\"| ))[^>]*>.+?</p>"; //定义HTML标签的正则表达式
        String regEx_html_h5 = "(?<!<h5>)<p class=\"(a X5(\"| )|X1 X6(\"| )|Normal Heading5(\"| ))[^>]*>.+?</p>"; //定义HTML标签的正则表达式
        String regEx_html_h6 = "(?<!<h6>)<p class=\"(a X6(\"| )|X1 X7(\"| )|Normal Heading6(\"| ))[^>]*>.+?</p>"; //定义HTML标签的正则表达式
        List<String> list = new ArrayList<>();
        list.add(regEx_html_h1);
        list.add(regEx_html_h2);
        list.add(regEx_html_h3);
        list.add(regEx_html_h4);
        list.add(regEx_html_h5);
        list.add(regEx_html_h6);
        int i = 1;
        for (String regEx_html : list) {
            while (true) {
                String link = getLink(htmlStr, regEx_html);
                if ("".equals(link)) {
                    break;
                }
                htmlStr = htmlStr.replaceAll(link, "<h" + String.valueOf(i) + ">" + link + "</h" + String.valueOf(i) + ">");
            }
            i++;
        }

        return htmlStr;
    }

    /**
     * 提取
     *
     * @param text
     * @return
     */
    public static String getLink(String text, String rep) {
        Pattern pattern = Pattern.compile(rep);
        Matcher m = pattern.matcher(text);
        String str = "";
        if (m.find()) {
            str = m.group(0);
        }
        return str;
    }

    /**
     * 切分html文件
     *
     * @param htmlstr
     * @return
     */
    public static List<Map<String, String>> splitHtmlByTag(String htmlstr) {
        System.out.println("---------splitHtmlByTag-----------");

        // 获取大纲
        Document doc = Jsoup.parse(htmlstr);
//        Elements x1_x3 = doc.select("p[class~=(^X1 (X2|X3|X4|X5|X6|X7)($| .*)|^a (X1|X2|X3|X4|X5|X6)($| .*)|^Normal Heading(1|2|3|4|5|6)($| .*))]");
        Elements x1_x3 = doc.select("h1,h2,h3,h4,h5,h6");
        Elements elements = new Elements();
        elements.addAll(x1_x3);
        List<Map<String, String>> titleMapList = new ArrayList();
        int i = 0;
        for (Element element : elements) {
            HashMap<String, String> titleMap = new HashMap<>();
            if ("".equals(element.text())) {
                continue;
            }
            i += 1;
            titleMap.put("title", element.text());
//            titleMap.put("level", element.attributes().get("class").length() < 5 ? element.attributes().get("class").substring(3, 4) : element.attributes().get("class").length() == 5 ? element.attributes().get("class").substring(4, 5) : element.attributes().get("class").length() <= 14 ? element.attributes().get("class").substring(4, 5) : element.attributes().get("class").substring(14, 15));
            titleMap.put("level", element.tag().toString().substring(1, 2));
            titleMap.put("id", String.valueOf(i));
            titleMapList.add(titleMap);
        }
        System.out.println("titleMapList" + titleMapList.size());


        // 切分内容,获取大纲内容
        String[] split = htmlstr.split("(<h1>|<h2>|<h3>|<h4>|<h5>|<h6>)");
        ArrayList<String> htmlList = new ArrayList<>();
        for (String s : split) {
            StringBuffer sb = new StringBuffer();
            sb.append(s);
            if (s.contains("</h6>")) {
                sb.insert(0, "<h6>");
            } else if (s.contains("</h5>")) {
                sb.insert(0, "<h5>");
            } else if (s.contains("</h4>")) {
                sb.insert(0, "<h4>");
            } else if (s.contains("</h3>")) {
                sb.insert(0, "<h3>");
            } else if (s.contains("</h2>")) {
                sb.insert(0, "<h2>");
            } else if (s.contains("</h1>")) {
                sb.insert(0, "<h1>");
            } else {

            }
            s = sb.toString();
//            System.out.println(s);
            htmlList.add(s);
        }
        System.out.println("htmlList" + htmlList.size());
        System.out.println(htmlList.get(0));
        // 三种情况 1.全文没有标题 2.开头无标题 3.标题开头的正常文本
        // 1.全文没有标题,此时大纲为空,需要默认新建一个大纲用来装载全文  此时titleMapList为0 htmlList为1
        // 2.开头无标题,此时也需要新建一个大纲来装载开头部分  此时titleMapList为n htmlList为n+1
        // 3.标题开头的正常文本,但html前头有<html>的开始部分,但区别是不包含<p>  此时titleMapList为n htmlList为n+1

        for (Map<String, String> titleMap : titleMapList) {
            titleMap.put("content", htmlList.get(Integer.valueOf(titleMap.get("id"))));
        }
        // 情况1
        if (titleMapList.size() == 0) {
            Map<String, String> titleMap = new HashMap<>();
            titleMap.put("title", "封面信息");
            titleMap.put("level", "1");
            titleMap.put("id", "0");
            titleMap.put("content", htmlList.get(0));
            titleMapList.add(titleMap);
        }
        //情况2
        else if (titleMapList.size() != 0 && htmlList.get(0).contains("<p ")) {
            Map<String, String> titleMap = new HashMap<>();
            titleMap.put("title", "封面信息");
            titleMap.put("level", titleMapList.get(0).get("level"));
            titleMap.put("id", "0");
            titleMap.put("content", htmlList.get(0));
            titleMapList.add(0, titleMap);
        }
        //情况3
        else if (titleMapList.size() != 0 && !htmlList.get(0).contains("<p ")) {
            // 暂时无需多余操作
        }

        // 优化level
        // 举例 若level最大为5 则所有level减4
        Integer min = 6;
        for (Map<String, String> map : titleMapList) {
            String s1 = map.get("level");
            if (Integer.valueOf(s1) <= min) {
                min = Integer.valueOf(s1);
            }
        }
        // 计算差 5-(6-min) 即 min-1
        for (Map<String, String> map : titleMapList) {
            String s1 = map.get("level");
            map.put("level", String.valueOf(Integer.valueOf(s1) - (min - 1)));
        }

        List<Map<String, String>> ret = JsonKeyUtils.list2tree(titleMapList);
        System.out.println(JSON.toJSONString(ret));
        return ret;
    }

    /**
     * 去除html的标签 保留纯文本内容
     *
     * @param htmlStr
     * @return
     */
    public static String delHTMLTag(String htmlStr) {
        String regEx_script = "<script[^>]*?>[\\s\\S]*?<\\/script>"; //定义script的正则表达式
        String regEx_style = "<style[^>]*?>[\\s\\S]*?<\\/style>"; //定义style的正则表达式
        String regEx_html = "<[^>]+>"; //定义HTML标签的正则表达式

        Pattern p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
        Matcher m_script = p_script.matcher(htmlStr);
        htmlStr = m_script.replaceAll(""); //过滤script标签

        Pattern p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
        Matcher m_style = p_style.matcher(htmlStr);
        htmlStr = m_style.replaceAll(""); //过滤style标签

        Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
        Matcher m_html = p_html.matcher(htmlStr);
        htmlStr = m_html.replaceAll(""); //过滤html标签

        return htmlStr.trim().replaceAll("&nbsp;", ""); //返回文本字符串
    }

    /**
     * 从字符串A中找到所有字符串B
     * eg:
     * A=abcdefgabc
     * B=b
     * 返回[1,8]
     *
     * @param string    总字符串
     * @param getString 查询关键字
     */
    public static List<Integer> indexOfAll(String string, String getString) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < string.length(); i++) {
//            System.out.println("i : "+i);
            if (i <= string.length() - getString.length()) {
                if (string.indexOf(getString, i) > 0) {
                    i = string.indexOf(getString, i);
//                    System.out.println(i);//i就是目标字符串的起始位置，末尾位置即为i+getString.length();
                    list.add(i);
                } else {
                    break;
                }
            }
        }
        return list;
    }

    /**
     * 分析style文件标题结构
     * @param fileName
     * @return
     */
    public static Map<String, String> getDocStyle(String fileName) {
        String htmlStr = readHtml(fileName);
        Document doc = Jsoup.parse(htmlStr, Parser.xmlParser());
        Elements styles = doc.getElementsByTag("w:style");
        Map<String, String> map = new HashMap();
        for (Element style : styles) {
            if ("heading 1".equals(style.getElementsByTag("w:name").attr("w:val"))) {
                map.put("h1", style.attr("w:styleId"));
            } else if ("heading 2".equals(style.getElementsByTag("w:name").attr("w:val"))) {
                map.put("h2", style.attr("w:styleId"));
            } else if ("heading 3".equals(style.getElementsByTag("w:name").attr("w:val"))) {
                map.put("h3", style.attr("w:styleId"));
            } else if ("heading 4".equals(style.getElementsByTag("w:name").attr("w:val"))) {
                map.put("h4", style.attr("w:styleId"));
            } else if ("heading 5".equals(style.getElementsByTag("w:name").attr("w:val"))) {
                map.put("h5", style.attr("w:styleId"));
            } else if ("heading 6".equals(style.getElementsByTag("w:name").attr("w:val"))) {
                map.put("h6", style.attr("w:styleId"));
            }
        }
        return map;
    }

    /**
     * 获取word的style文件
     * @param fileName
     * @param outPutFilePath
     * @param newFileName
     * @return
     */
    public static void getDocStyleXml(String fileName, String outPutFilePath, String newFileName) {


    }

    public static String aa(){
        try {
            int i = 2;
            return "333";
        } catch (Exception e) {
            return "444";
        } finally {
            System.out.println("111");
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, JDOMException, DocumentException {


//        String s = readHtml("C:\\Users\\xuyanxu\\Desktop\\1\\声环境质量标准.html");
//        System.out.println(htmlAddHTag("C:\\Users\\xuyanxu\\Desktop\\1\\声环境质量标准.html"));
//        System.out.println("-----------");
//        List<Map<String, String>> mapList = splitHtmlByTag(htmlAddHTag("C:\\Users\\xuyanxu\\Desktop\\1\\声环境质量标准.html"));
//        System.out.println(JSON.toJSONString(mapList));

//        Map<String, String> docStyle = getDocStyle("C:\\Users\\xuyanxu\\Desktop\\1\\3-（SGCC-01-SGGK(2023)）国家电网有限公司标准招标采购文件范本-施工公开招标文件分册\\word\\styles.xml");

//        System.out.println(aa());

        ArrayList<String> objects = new ArrayList<>();
        objects.add(null);

    }

}