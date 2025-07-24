package cn.nebulaedata.utils;


import cn.nebulaedata.exception.WorkTableException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.PicturesManager;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.PictureType;
import org.apache.poi.xwpf.converter.core.FileImageExtractor;
import org.apache.poi.xwpf.converter.core.IURIResolver;
import org.apache.poi.xwpf.converter.core.XWPFConverterException;
import org.apache.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.impl.values.XmlValueOutOfRangeException;
import org.w3c.dom.Document;
//import site.suremotoo.doc.DocToHtml;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/8/24 09:30
 * @note
 */
public class Doc2html {

    /**
     * word转html方法
     *
     * @param wordPath    word文件路径
     * @param htmlPath    生成html文件路径
     * @param newFilename 生成html的文件名
     * @throws Exception
     */
    public static void wordToHtml(String wordPath, String htmlPath, String newFilename) throws Exception {
        convert2Html(wordPath, htmlPath, newFilename);
    }

    /**
     * 将内容写入指定路径
     *
     * @param content 写入内容
     * @param path    路径
     * @throws Exception
     */
    public static void writeFile(String content, String path) throws Exception {
        FileOutputStream fos = null;
        BufferedWriter bw = null;
        try {
            File file = new File(path);
            fos = new FileOutputStream(file);
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(content);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ie) {
            }
        }
    }

    /**
     * 将word转换成html
     * 支持 .doc and .docx
     *
     * @param fileName       word文件名
     * @param outPutFilePath html存储路径
     * @param newFileName    html名
     * @throws Exception
     */
    public static void convert2Html(String fileName, String outPutFilePath, String newFileName)
            throws Exception {
        String substring = fileName.substring(fileName.lastIndexOf(".") + 1);
        org.apache.commons.io.output.ByteArrayOutputStream out = new ByteArrayOutputStream();

        /**
         * word2007和word2003的构建方式不同，
         * 前者的构建方式是xml，后者的构建方式是dom树。
         * 文件的后缀也不同，前者后缀为.docx，后者后缀为.doc
         * 相应的，apache.poi提供了不同的实现类。
         */
        if ("docx".equals(substring)) {
            //step 1 : load DOCX into XWPFDocument
            InputStream inputStream = new FileInputStream(new File(fileName));
            XWPFDocument document = new XWPFDocument(inputStream);
            //step 2 : prepare XHTML options
            final String imageUrl = "";
            XHTMLOptions options = XHTMLOptions.create();
            options.setExtractor(new FileImageExtractor(new File(outPutFilePath + imageUrl)));
            options.setIgnoreStylesIfUnused(false);
            options.setFragment(true);
            options.URIResolver(new IURIResolver() {
                //step 3 : convert XWPFDocument to XHTML
                public String resolve(String uri) {
                    return imageUrl + uri;
                }
            });
            try {
                XHTMLConverter.getInstance().convert(document, out, options);
            } catch (XmlValueOutOfRangeException e) {
                throw new WorkTableException("上传文件中可能存在列数过大(100列以上)的表格");
            } catch (XWPFConverterException e) {
                throw new WorkTableException("上传文件中可能存在列数过大(100列以上)的表格");
            }
         } else {
            //WordToHtmlUtils.loadDoc(new FileInputStream(inputFile));
            HWPFDocument wordDocument = new HWPFDocument(new FileInputStream(fileName));
            WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(
                    DocumentBuilderFactory.newInstance().newDocumentBuilder()
                            .newDocument());

            wordToHtmlConverter.setPicturesManager(new PicturesManager() {
                public String savePicture(byte[] content,
                                          PictureType pictureType, String suggestedName,
                                          float widthInches, float heightInches) {
                    return suggestedName;
                }
            });
            wordToHtmlConverter.processDocument(wordDocument);
            //save pictures
            List pics = wordDocument.getPicturesTable().getAllPictures();

            if (pics != null && !pics.isEmpty()) {
                for (int i = 0; i < pics.size(); i++) {
                    Picture pic = (Picture) pics.get(i);
                    System.out.println();
                    try {
                        pic.writeImageContent(new FileOutputStream(outPutFilePath
                                + pic.suggestFullFileName()));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            Document htmlDocument = wordToHtmlConverter.getDocument();
            DOMSource domSource = new DOMSource(htmlDocument);
            StreamResult streamResult = new StreamResult(out);

            //这个应该是转换成xml的
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(OutputKeys.METHOD, "html");
            serializer.transform(domSource, streamResult);
        }

        out.close();
//        writeFile(new String(out.toByteArray()), outPutFilePath + newFileName);
        writeFile(convertToChinese(new String(out.toByteArray())), outPutFilePath + newFileName);


    }


    private static String convertToChinese(String dataStr) {
        System.out.println("--------data str---->" + dataStr);
        if(dataStr == null || dataStr.length() == 0) {
            return dataStr;
        }
        int start = 0;
        int end = 0;
        final StringBuffer buffer = new StringBuffer();
        while (start > -1) {
            int system = 10;// 进制
            if (start == 0) {
                int t = dataStr.indexOf("&#");
                if (start != t)
                    start = t;
                if(start > 0) {
                    buffer.append(dataStr.substring(0, start));
                }
                if(start == -1) {
                    return dataStr;
                }
            }
            end = dataStr.indexOf(";", start + 2);
            String charStr = "";
            if (end != -1) {
                charStr = dataStr.substring(start + 2, end);
                // 判断进制
                char s = charStr.charAt(0);
                if (s == 'x' || s == 'X') {
                    system = 16;
                    charStr = charStr.substring(1);
                }
                // 转换
                try {
                    char letter = (char) Integer.parseInt(charStr, system);
                    buffer.append(new Character(letter).toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            // 处理当前unicode字符到下一个unicode字符之间的非unicode字符
            start = dataStr.indexOf("&#", end);
            if (start - end > 1) {
                buffer.append(dataStr.substring(end + 1, start));
            }
            // 处理最后面的非 unicode字符
            if (start == -1) {
                int length = dataStr.length();
                if (end + 1 != length) {
                    buffer.append(dataStr.substring(end + 1, length));
                }
            }
        }
        return buffer.toString();
    }

    public void run() throws Exception {
        wordToHtml("C:\\Users\\xuyanxu\\Downloads\\技术规范书模板-服务通用.docx",
                "C:\\Users\\xuyanxu\\Desktop\\1\\",
                "声环境质量标准3.html");
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        System.out.println("开始解析...请等待");
        new Doc2html().run();
        System.out.println("解析完成!!!");
        long end = System.currentTimeMillis();
        System.out.println("程序运行时间："+(end-start)+"ms");
    }
}
