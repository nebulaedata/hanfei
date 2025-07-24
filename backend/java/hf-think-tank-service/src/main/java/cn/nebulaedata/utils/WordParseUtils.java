//package cn.nebulaedata.utils;
//
//import org.jodconverter.DocumentConverter;
//import org.jodconverter.office.OfficeException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//
///**
// * @author 贾亦真
// * @date 2021/1/18 11:50
// * @note
// * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
// */
//@Component
//public class WordParseUtils {
//
//    @Autowired
//    private DocumentConverter converter;
//
//    public void docxToPdf(String sourcePath, String targetPath) {
//        File file = new File(sourcePath);
//        try {
//            converter.convert(file).to(new File(targetPath)).execute();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void docToDocx(String sourcePath, String targetPath) {
//        try {
//            File file = new File(sourcePath);
//            converter.convert(file).to(new File(targetPath)).execute();
//        } catch (OfficeException e) {
//            e.printStackTrace();
//        }
//    }
//
//}
