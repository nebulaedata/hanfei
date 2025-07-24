package cn.nebulaedata.utils;

import org.ofdrw.converter.ofdconverter.PDFConverter;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.element.Paragraph;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OFDUtils {


    public static String pdfToOfd(String pdfPath, String ofdPath) throws IOException {
        Path src = Paths.get(pdfPath);
        Path dst = Paths.get(ofdPath);
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src);
        }
        return dst.toAbsolutePath().toString();
    }

    public static void main(String[] args) throws IOException {
//        Path path = Paths.get("HelloWorld.ofd");
//        try (OFDDoc ofdDoc = new OFDDoc(path)) {
//            Paragraph p = new Paragraph("你好呀，OFD Reader&Writer！");
//            ofdDoc.add(p);
//        }
//        System.out.println("生成文档位置: " + path.toAbsolutePath());

        String pdfPath = "C:\\Users\\xuyanxu\\Downloads\\招标文件 (1).pdf";
        String ofdPath = "C:\\Users\\xuyanxu\\Downloads\\招标文件 (1).ofd";
//        System.out.println(pdfToOfd(pdfPath,ofdPath));
        String extension = pdfPath.substring(pdfPath.lastIndexOf("."));
        System.out.println(extension);
    }
}
