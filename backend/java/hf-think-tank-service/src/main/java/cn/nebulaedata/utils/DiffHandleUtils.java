package cn.nebulaedata.utils;

import cn.nebulaedata.vo.TResponseVo;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class DiffHandleUtils {

    /**
     * 对比两文件的差异，返回原始文件+diff格式
     *
     * @param original 原文件内容
     * @param revised  对比文件内容
     */
    public static List<String> diffString(List<String> original, List<String> revised) {
        return diffString(original, revised, null, null);
    }

    /**
     * 对比两文件的差异，返回原始文件+diff格式
     *
     * @param original         原文件内容
     * @param revised          对比文件内容
     * @param originalFileName 原始文件名
     * @param revisedFileName  对比文件名
     */
    public static List<String> diffString(List<String> original, List<String> revised, String originalFileName, String revisedFileName) {
        originalFileName = originalFileName == null ? "原始文件" : originalFileName;
        revisedFileName = revisedFileName == null ? "对比文件" : revisedFileName;
        //两文件的不同点
        Patch<String> patch = com.github.difflib.DiffUtils.diff(original, revised);
        //生成统一的差异格式
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(originalFileName, revisedFileName, original, patch, 0);
        if (unifiedDiff.size() == 0) {
            //如果两文件没差异则插入如下
            unifiedDiff.add("--- " + originalFileName);
            unifiedDiff.add("+++ " + revisedFileName);
            unifiedDiff.add("@@ -0,0 +0,0 @@");
        } else if (unifiedDiff.size() >= 3 && !unifiedDiff.get(2).contains("@@ -1,")) {
            //如果第一行没变化则插入@@ -0,0 +0,0 @@
            unifiedDiff.add(2, "@@ -0,0 +0,0 @@");
        }
        //原始文件中每行前加空格
        List<String> original1 = original.stream().map(v -> " " + v).collect(Collectors.toList());
        //差异格式插入到原始文件中
        return insertOrig(original1, unifiedDiff);
    }


    /**
     * 对比两文件的差异，返回原始文件+diff格式
     *
     * @param filePathOriginal 原文件路径
     * @param filePathRevised  对比文件路径
     */
    public static List<String> diffString(String filePathOriginal, String filePathRevised) {
        //原始文件
        List<String> original = null;
        //对比文件
        List<String> revised = null;
        File originalFile = new File(filePathOriginal);
        File revisedFile = new File(filePathRevised);
        try {
            original = Files.readAllLines(originalFile.toPath());
            revised = Files.readAllLines(revisedFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return diffString(original, revised, originalFile.getName(), revisedFile.getName());
    }

    /**
     * 通过两文件的差异diff生成 html文件，打开此 html文件便可看到文件对比的明细内容
     *
     * @param diffString 调用上面 diffString方法获取到的对比结果
     * @param htmlPath   生成的html路径，如:/user/var/mbos/ent/21231/diff.html
     */
    public static void generateDiffHtml(List<String> diffString, String htmlPath) {
        StringBuilder builder = new StringBuilder();
        for (String line : diffString) {
            builder.append(line);
            builder.append("\n");
        }
        //如果打开html为空白界面，可能cdn加载githubCss失败 ,githubCss 链接可替换为 https://cdnjs.cloudflare.com/ajax/libs/highlight.js/10.7.1/styles/github.min.css
        String githubCss = "src/main/resources/css/github.min.css";
//        String githubCss = "https://cdn.jsdelivr.net/gh/1506085843/fillDiff@master/src/main/resources/css/github.min.css";
        //如果打开html为空白界面，可能cdn加载diff2htmlCss失败 ,diff2htmlCss 链接可替换为 https://cdn.jsdelivr.net/npm/diff2html/bundles/css/diff2html.min.css
        String diff2htmlCss = "src/main/resources/css/diff2html.min.css";
//        String diff2htmlCss = "https://cdn.jsdelivr.net/gh/1506085843/fillDiff@master/src/main/resources/css/diff2html.min.css";
        //如果打开html为空白界面，可能cdn加载diff2htmlJs失败, diff2htmlJs 链接可替换为 https://cdn.jsdelivr.net/npm/diff2html/bundles/js/diff2html-ui.min.js
        String diff2htmlJs = "src/main/resources/js/diff2html-ui.min.js";
//        String diff2htmlJs = "https://cdn.jsdelivr.net/gh/1506085843/fillDiff@master/src/main/resources/js/diff2html-ui.min.js";
        //如果githubCss、diff2htmlCss、diff2htmlJs都加载失败可从 https://github.com/1506085843/fillDiff/tree/main/src/main/resources下载css和js手动引入到html

        String template = "<!DOCTYPE html>\n" +
                "<html lang=\"en-us\">\n" +
                "  <head>\n" +
                "    <meta charset=\"utf-8\" />\n" +
                "    <link rel=\"stylesheet\" href=\"" + githubCss + "\" />\n" +
                "     <link rel=\"stylesheet\" type=\"text/css\" href=\"" + diff2htmlCss + "\" />\n" +
                "    <script type=\"text/javascript\" src=\"" + diff2htmlJs + "\"></script>\n" +
                "  </head>\n" +
                "  <script>\n" +
                "    const diffString = `\n" +
                "temp\n" +
                "`;\n" +
                "\n" +
                "\n" +
                "     document.addEventListener('DOMContentLoaded', function () {\n" +
                "      var targetElement = document.getElementById('myDiffElement');\n" +
                "      var configuration = {\n" +
                "        drawFileList: true,\n" +
                "        fileListToggle: false,\n" +
                "        fileListStartVisible: false,\n" +
                "        fileContentToggle: false,\n" +
                "        matching: 'lines',\n" +
                "        outputFormat: 'side-by-side',\n" +
                "        synchronisedScroll: true,\n" +
                "        highlight: true,\n" +
                "        renderNothingWhenEmpty: true,\n" +
                "      };\n" +
                "      var diff2htmlUi = new Diff2HtmlUI(targetElement, diffString, configuration);\n" +
                "      diff2htmlUi.draw();\n" +
                "      diff2htmlUi.highlightCode();\n" +
                "    });\n" +
                "  </script>\n" +
                "  <body>\n" +
                "    <div id=\"myDiffElement\"></div>\n" +
                "  </body>\n" +
                "</html>";
        template = template.replace("temp", builder.toString());
        FileWriter f = null; //文件读取为字符流
        try {
            f = new FileWriter(htmlPath);
            BufferedWriter buf = new BufferedWriter(f); //文件加入缓冲区
            buf.write(template); //向缓冲区写入
            buf.close(); //关闭缓冲区并将信息写入文件
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过两文件的差异diff生成 html文件，打开此 html文件便可看到文件对比的明细内容
     *
     * @param diffString 调用上面 diffString方法获取到的对比结果
     * @param response   生成的html路径，如:/user/var/mbos/ent/21231/diff.html
     */
    public static void generateDiffHtml(List<String> diffString, HttpServletResponse response) {
        StringBuilder builder = new StringBuilder();
        for (String line : diffString) {
            builder.append(line);
            builder.append("\n");
        }
        response.setContentType( "application/json; charset=UTF-8");
        try {
            PrintWriter writer = response.getWriter();
            writer.write(builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //统一差异格式插入到原始文件
    public static List<String> insertOrig(List<String> original, List<String> unifiedDiff) {
        List<String> result = new ArrayList<>();
        //unifiedDiff中根据@@分割成不同行，然后加入到diffList中
        List<List<String>> diffList = new ArrayList<>();
        List<String> d = new ArrayList<>();
        for (int i = 0; i < unifiedDiff.size(); i++) {
            String u = unifiedDiff.get(i);
            if (u.startsWith("@@") && !"@@ -0,0 +0,0 @@".equals(u) && !u.contains("@@ -1,")) {
                List<String> twoList = new ArrayList<>();
                twoList.addAll(d);
                diffList.add(twoList);
                d.clear();
                d.add(u);
                continue;
            }
            if (i == unifiedDiff.size() - 1) {
                d.add(u);
                List<String> twoList = new ArrayList<>();
                twoList.addAll(d);
                diffList.add(twoList);
                d.clear();
                break;
            }
            d.add(u);
        }

        //将diffList和原始文件original插入到result，返回result
        for (int i = 0; i < diffList.size(); i++) {
            List<String> diff = diffList.get(i);
            List<String> nexDiff = i == diffList.size() - 1 ? null : diffList.get(i + 1);
            //含有@@的一行
            String simb = i == 0 ? diff.get(2) : diff.get(0);
            String nexSimb = nexDiff == null ? null : nexDiff.get(0);
            //插入到result
            insert(result, diff);
            //解析含有@@的行，得到原文件从第几行开始改变，改变了多少（即增加和减少的行）
            Map<String, Integer> map = getRowMap(simb);
            if (null != nexSimb) {
                Map<String, Integer> nexMap = getRowMap(nexSimb);
                int start = 0;
                if (map.get("orgRow") != 0) {
                    start = map.get("orgRow") + map.get("orgDel") - 1;
                }
                int end = nexMap.get("revRow") - 2;
                //插入不变的
                insert(result, getOrigList(original, start, end));
            }

            if (simb.contains("@@ -1,") && null == nexSimb) {
                insert(result, getOrigList(original, 0, original.size() - 1));
            } else if (null == nexSimb && map.get("orgRow") < original.size()) {
                insert(result, getOrigList(original, map.get("orgRow"), original.size() - 1));
            }
        }
        return result;
    }

    //将源文件中没变的内容插入result
    public static void insert(List<String> result, List<String> noChangeContent) {
        for (String ins : noChangeContent) {
            result.add(ins);
        }
    }

    //解析含有@@的行得到修改的行号删除或新增了几行
    public static Map<String, Integer> getRowMap(String str) {
        Map<String, Integer> map = new HashMap<>();
        if (str.startsWith("@@")) {
            String[] sp = str.split(" ");
            String org = sp[1];
            String[] orgSp = org.split(",");
            //源文件要删除行的行号
            map.put("orgRow", Integer.valueOf(orgSp[0].substring(1)));
            //源文件删除的行数
            map.put("orgDel", Integer.valueOf(orgSp[1]));

            String[] revSp = org.split(",");
            //对比文件要增加行的行号
            map.put("revRow", Integer.valueOf(revSp[0].substring(1)));
            map.put("revAdd", Integer.valueOf(revSp[1]));
        }
        return map;
    }

    //从原文件中获取指定的部分行
    public static List<String> getOrigList(List<String> original1, int start, int end) {
        List<String> list = new ArrayList<>();
        if (start <= end && end < original1.size()) {
            for (; start <= end; start++) {
                list.add(original1.get(start));
            }
        }
        return list;
    }


    /**
     * 通过两文件的差异diff生成 html文件，打开此 html文件便可看到文件对比的明细内容
     *
     * @param diffString1 调用上面 diffString方法获取到的对比结果
     * @param diffString2 调用上面 diffString方法获取到的对比结果
     * @param htmlPath    生成的html路径，如:/user/var/mbos/ent/21231/diff.html
     */
    public static void generateDiffHtml2(List<String> diffString1, List<String> diffString2, String htmlPath) {
        StringBuilder builder1 = new StringBuilder();
        for (String line : diffString1) {
            builder1.append(line);
            builder1.append("\n");
        }
        StringBuilder builder2 = new StringBuilder();
        for (String line : diffString2) {
            builder2.append(line);
            builder2.append("\n");
        }

        String template = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\" /><title>Mergely - Simple Example</title>\n" +
                "  <meta http-equiv=\"X-UA-Compatible\" content=\"chrome=1, IE=edge\">\n" +
                "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"/>\n" +
                "  <meta name=\"description\" content=\"Merge and Diff your documents with diff online and share\" />\n" +
                "  <meta name=\"keywords\" content=\"diff,merge,compare,jsdiff,comparison,difference,file,text,unix,patch,algorithm,saas,longest common subsequence\" />\n" +
                "  <meta name=\"author\" content=\"Jamie Peabody\" />\n" +
                "\n" +
                "  <!-- Requires jQuery -->\n" +
                "  <script type=\"text/javascript\" src=\"../../src/jquery.min.js\"></script>\n" +
                "\n" +
                "  <!-- Requires CodeMirror -->\n" +
                "  <script type=\"text/javascript\" src=\"../../src/codemirror.min.js\"></script>\n" +
                "  <script type=\"text/javascript\" src=\"../../src/searchcursor.min.js\"></script>\n" +
                "  <link type=\"text/css\" rel=\"stylesheet\" href=\"../../src/codemirror.min.css\" />\n" +
                "\n" +
                "  <!-- Requires Mergely -->\n" +
                "  <script type=\"text/javascript\" src=\"../../src/mergely.js\"></script>\n" +
                "  <link type=\"text/css\" rel=\"stylesheet\" href=\"../../src/mergely.css\" />\n" +
                "\n" +
                "  <script type=\"text/javascript\">\n" +
                "  var left =`\n" +
                "  temp1\n" +
                "  `;\n" +
                "  var right =`\n" +
                "  temp2\n" +
                "  `;\n" +
                "    $(document).ready(function () {\n" +
                "      $('#mergely').mergely({\n" +
                "        sidebar:true,   //是否显示侧边栏，设置成false可以提高大型文档的性能\n" +
                "        ignorews:false,  //是否忽略空格对比\n" +
                "        license: 'lgpl',\n" +
                "        cmsettings: {\n" +
                "          readOnly: true //false则展示合并箭头，运行两边能够合并\n" +
                "        },\n" +
                "        lhs: function(setValue) {\n" +
                "          setValue(left);\n" +
                "        },\n" +
                "        rhs: function(setValue) {\n" +
                "          setValue(right);\n" +
                "        }\n" +
                "      });\n" +
                "    });\n" +
                "  </script>\n" +
                "</head>\n" +
                "\n" +
                "<body>\n" +
                "  <div>\n" +
                "    <div class=\"mergely-full-screen-8\">\n" +
                "      <div class=\"mergely-resizer\">\n" +
                "        <div id=\"mergely\"></div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "\n" +
                "</html>\n";

        template = template.replace("temp1", builder1.toString());
        template = template.replace("temp2", builder2.toString());
        FileWriter f = null; //文件读取为字符流
        try {
            f = new FileWriter(htmlPath);
            BufferedWriter buf = new BufferedWriter(f); //文件加入缓冲区
            buf.write(template); //向缓冲区写入
            buf.close(); //关闭缓冲区并将信息写入文件
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void generateDiffHtml3(List<String> contentTextOld, List<String> contentTextNew, HttpServletResponse response) throws IOException {

//        String[] split = contentTextOld.split("</p>|</h[0-9]+>");
//        List<String> strings = Arrays.asList(split);
//        String[] split2 = contentTextNew.split("</p>|</h[0-9]+>");
//        List<String> strings2 = Arrays.asList(split2);

        StringBuilder builder1 = new StringBuilder();
        for (String line : contentTextOld) {
            builder1.append(line);
            builder1.append("\n");
        }
        StringBuilder builder2 = new StringBuilder();
        for (String line : contentTextNew) {
            builder2.append(line);
            builder2.append("\n");
        }

        String template = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\" /><title>Mergely - Simple Example</title>\n" +
                "  <meta http-equiv=\"X-UA-Compatible\" content=\"chrome=1, IE=edge\">\n" +
                "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"/>\n" +
                "  <meta name=\"description\" content=\"Merge and Diff your documents with diff online and share\" />\n" +
                "  <meta name=\"keywords\" content=\"diff,merge,compare,jsdiff,comparison,difference,file,text,unix,patch,algorithm,saas,longest common subsequence\" />\n" +
                "  <meta name=\"author\" content=\"Jamie Peabody\" />\n" +
                "\n" +
                "  <!-- Requires jQuery -->\n" +
                "  <script type=\"text/javascript\" src=\"/compare/jquery.min.js\"></script>\n" +
                "\n" +
                "  <!-- Requires CodeMirror -->\n" +
                "  <script type=\"text/javascript\" src=\"/compare/codemirror.min.js\"></script>\n" +
                "  <script type=\"text/javascript\" src=\"/compare/searchcursor.min.js\"></script>\n" +
                "  <link type=\"text/css\" rel=\"stylesheet\" href=\"/compare/codemirror.min.css\" />\n" +
                "\n" +
                "  <!-- Requires Mergely -->\n" +
                "  <script type=\"text/javascript\" src=\"/compare/mergely.js\"></script>\n" +
                "  <link type=\"text/css\" rel=\"stylesheet\" href=\"/compare/mergely.css\" />\n" +
                "\n" +
                "  <script type=\"text/javascript\">\n" +
                "  var left =`\n" +
                "  temp1\n" +
                "  `;\n" +
                "  var right =`\n" +
                "  temp2\n" +
                "  `;\n" +
                "    $(document).ready(function () {\n" +
                "      $('#mergely').mergely({\n" +
                "        sidebar:true,   //是否显示侧边栏，设置成false可以提高大型文档的性能\n" +
                "        ignorews:false,  //是否忽略空格对比\n" +
                "        license: 'lgpl',\n" +
                "        cmsettings: {\n" +
                "          readOnly: true //false则展示合并箭头，运行两边能够合并\n" +
                "        },\n" +
                "        lhs: function(setValue) {\n" +
                "          setValue(left);\n" +
                "        },\n" +
                "        rhs: function(setValue) {\n" +
                "          setValue(right);\n" +
                "        }\n" +
                "      });\n" +
                "    });\n" +
                "  </script>\n" +
                "</head>\n" +
                "\n" +
                "<body>\n" +
                "  <div>\n" +
                "    <div class=\"mergely-full-screen-8\">\n" +
                "      <div class=\"mergely-resizer\">\n" +
                "        <div id=\"mergely\"></div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "\n" +
                "</html>\n";

        template = template.replace("temp1", StringEscapeUtils.unescapeHtml(builder1.toString()).replaceAll("`", "\\\\`").replaceAll("\\$", "\\\\\\$"));
        template = template.replace("temp2", StringEscapeUtils.unescapeHtml(builder2.toString()).replaceAll("`", "\\\\`").replaceAll("\\$", "\\\\\\$"));
        FileWriter f = null; //文件读取为字符流
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        try {
            f = new FileWriter("D:\\workspace\\hf-think-tank-service\\src\\main\\resources\\dev\\static\\tmp" + uuid + ".html");
            BufferedWriter buf = new BufferedWriter(f); //文件加入缓冲区
            buf.write(template); //向缓冲区写入
            buf.close(); //关闭缓冲区并将信息写入文件
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // 发送给客户端的数据
        // 读取filename
//        ClassPathResource classPathResource = new ClassPathResource();
        ServletOutputStream outputStream = null;
        File file = new File("D:\\workspace\\hf-think-tank-service\\src\\main\\resources\\dev\\static\\tmp" + uuid + ".html");
        if (file.exists()) {
            //response为HttpServletResponse对象
//            response.setHeader("Content-Disposition", "attachment;filename=temp.html");
//            response.setHeader("Content-type", "text/html");
            response.setContentType("text/html");
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                outputStream = response.getOutputStream();
                int i = bis.read(buffer);
                while (i != -1) {
                    outputStream.write(buffer, 0, i);
                    i = bis.read(buffer);
                }
                outputStream.flush();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
//                file.delete();
        }

        file.delete();
//        return outputStream;
    }


    public static void main(String[] args) {
        String contentTextOld = "<h3><span style=\"font-size: 14pt; font-family: 黑体\">16. 不可抗力</span></h3><p><span style=\"font-size: 14pt; font-family: 仿宋\">16.3（修改为）双方当事人应在不可抗力事件结束或其影响消除后立即继续履行其合同义务，合同期限也应相应顺延。如果不可抗力事件的影响可能造成卖方无法完成合同履行，双方可协商解除本合同。由于合同解除所引起的后续问题由双方友好协商解决。</span></p>";
        String[] split = contentTextOld.split("</p>|</h[0-9]+>");
        List<String> strings = Arrays.asList(split);

//        String contentTextDelTags = new JsonKeyUtils().delHtmlTags("<p>附件二：</p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><h2 style=\"text-align: center\">中华人</h2><h2 style=\"text-align: center\">民共和国</h2><h2 style=\"text-align: center\"><parameter uuid=\"f3b4c2d8-79a7-46be-9b7c-a2c239fb08c3\" key=\"883614f787824fb68f54ef9a894f5a3c\" typeid=\"\" placeholder=\"两个柱子间的距离\" title=\"参数\" isunderline=\"1\" styleid=\"632006\" unit=\"\" matrixmode=\"column\" value=\"\" active=\"false\"></parameter></h2><p style=\"text-align: center\"><parameter uuid=\"33d114c7-488f-492a-a57e-f2d4970d4d6a\" key=\"e6c2272b990a4c718680216c009b4c70\" typeid=\"\" placeholder=\"招标人用的附件参数\" title=\"参数\" isunderline=\"1\" styleid=\"632041\" unit=\"\" matrixmode=\"column\" value=\"\" active=\"false\"></parameter></p><h1 style=\"text-align: center\"><strong>标准施</strong>工招<strong>标文件</strong></h1><h1 style=\"text-align: center\"><parameter uuid=\"72893f0d-dc33-41d5-b2c8-d4717b93a593\" key=\"883614f787824fb68f54ef9a894f5a3c\" typeid=\"\" placeholder=\"两个柱子间的距离\" title=\"参数\" isunderline=\"1\" styleid=\"632006\" unit=\"\" matrixmode=\"column\" value=\"\" active=\"false\"></parameter></h1><h2 style=\"text-align: center\">（2007年版）</h2><p style=\"text-align: center\"><parameter uuid=\"2f77242e-5930-46f6-86e6-165a45179685\" key=\"c8fdf3699f0e4b9c91a58b905ca7908d\" typeid=\"\" placeholder=\"这是一个附件参数\" title=\"参数\" isunderline=\"1\" styleid=\"632041\" unit=\"\" matrixmode=\"column\" value=\"\" active=\"false\"></parameter></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p></p><p>zhang</p><p>111222</p><p style=\"text-align: center\"></p>");
//        String contentTextDelTags2 = new JsonKeyUtils().delHtmlTags("<p>附件二：</p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"><bookmark id=\"58f609af-c728-47e3-9127-7b7505826f59\" name=\"1\" description=\"1\"></bookmark><bookmark id=\"d8978e4d-24e0-40bf-b2ae-22e0a8f3ef5a\" name=\"2\" description=\"2\"></bookmark><bookmark id=\"4b7954e3-fad1-4d23-ac80-03b578bfc072\" name=\"3\" description=\"3\"></bookmark><bookmark id=\"73e65091-066f-4c10-a3c4-5e403a918e0c\" name=\"4\" description=\"4\"></bookmark><bookmark id=\"f934aa84-c51d-4906-b3e6-c3d07b326246\" name=\"5\" description=\"5\"></bookmark><bookmark id=\"e3f08fac-234c-4ee2-ae9d-8b905ce57612\" name=\"6\" description=\"6\"></bookmark></p><h2 style=\"text-align: center\">中华人11111111111222222222222222222222222222</h2><p style=\"text-align: center\">fjdlajfldajfdlajfjkddddddddddddkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk</p><p style=\"text-align: center\">fdasfdafdafdafdaf111111333333334444441111111jfldjafldjafldjalj1111111111111111111111111111111111111111111111111111111111111111111111111</p><p style=\"text-align: center\">fjdlajfdkkkkkkkkkkkkkkkkkkkkkkkkk111111111111111111111111111111111111112222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222</p><h2 style=\"text-align: center\">民共和国</h2><h2 style=\"text-align: center\"><parameter uuid=\"f3b4c2d8-79a7-46be-9b7c-a2c239fb08c3\" key=\"883614f787824fb68f54ef9a894f5a3c\" typeid=\"\" placeholder=\"两个柱子间的距离\" title=\"参数\" isunderline=\"1\" styleid=\"632006\" unit=\"\" matrixmode=\"column\" value=\"\" active=\"false\"></parameter></h2><p style=\"text-align: center\"><parameter uuid=\"33d114c7-488f-492a-a57e-f2d4970d4d6a\" key=\"e6c2272b990a4c718680216c009b4c70\" typeid=\"\" placeholder=\"招标人用的附件参数\" title=\"参数\" isunderline=\"1\" styleid=\"632041\" unit=\"\" matrixmode=\"column\" value=\"\" active=\"false\"></parameter></p><h1 style=\"text-align: center\"><strong>标准施</strong>工招<strong>标文件</strong></h1><h1 style=\"text-align: center\"><parameter uuid=\"72893f0d-dc33-41d5-b2c8-d4717b93a593\" key=\"883614f787824fb68f54ef9a894f5a3c\" typeid=\"\" placeholder=\"两个柱子间的距离\" title=\"参数\" isunderline=\"1\" styleid=\"632006\" unit=\"\" matrixmode=\"column\" value=\"\" active=\"false\"></parameter></h1><h2 style=\"text-align: center\">（2007年版）</h2><p style=\"text-align: center\"><parameter uuid=\"2f77242e-5930-46f6-86e6-165a45179685\" key=\"c8fdf3699f0e4b9c91a58b905ca7908d\" typeid=\"\" placeholder=\"这是一个附件参数\" title=\"参数\" isunderline=\"1\" styleid=\"632041\" unit=\"\" matrixmode=\"column\" value=\"\" active=\"false\"></parameter></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p style=\"text-align: center\"></p><p></p><p>zhang</p><p>111222</p><p style=\"text-align: center\"></p>");
//        String[] split = contentTextDelTags.split("</p>|</h[0-9]+>");
//        List<String> strings = Arrays.asList(split);
//        String[] split2 = contentTextDelTags2.split("</p>|</h[0-9]+>");
//        List<String> strings2 = Arrays.asList(split2);
//        generateDiffHtml2(strings, strings2, "C:\\Users\\xuyanxu\\Desktop\\diff2\\diff.html");

//        对比 F:\n1.txt和 F:\n2.txt 两个文件，获得不同点
        List<String> diffString = DiffHandleUtils.diffString("C:\\Users\\xuyanxu\\Desktop\\桌面3\\diff\\1.txt", "C:\\Users\\xuyanxu\\Desktop\\桌面3\\diff\\2.txt");
//        在F盘生成一个diff.html文件，打开便可看到两个文件的对比
        DiffHandleUtils.generateDiffHtml(diffString, "C:\\Users\\xuyanxu\\Desktop\\桌面3\\diff\\diff.html");
    }

}

