package cn.nebulaedata.utils;

import com.aspose.words.Document;
import com.aspose.words.FontSettings;
import com.aspose.words.License;
import com.aspose.words.SaveFormat;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

@Component
public class WordToPdfUtils {

    @Value("${doc-frame-service.pdf-license-path}")
    private String pdfLicensePath;

    //静态化工具类变量
    public static WordToPdfUtils wordToPdfUtils;

    @PostConstruct
    public void init() {
        wordToPdfUtils = new WordToPdfUtils();
        wordToPdfUtils.pdfLicensePath = this.pdfLicensePath;
        System.out.println("初始化wordToPdfUtils完毕");
    }

    /**
     * 加载license
     *
     * @author LCheng
     * @date 2020/12/25 13:51
     */
    @SneakyThrows
    private static void getLicense() {
        File file = new File(wordToPdfUtils.pdfLicensePath);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            License license = new License();
            license.setLicense(fileInputStream);
        }
    }

    /**
     * word转pdf
     *
     * @param wordPath word文件保存的路径
     * @param pdfPath  转换后pdf文件保存的路径
     * @author LCheng
     * @date 2020/12/25 13:51
     */
    @SneakyThrows
    public static void wordToPdf(String wordPath, String pdfPath) {
        getLicense();
        File file = new File(pdfPath);
        try (FileOutputStream os = new FileOutputStream(file)) {
            String lowerCase = System.getProperty("os.name").toLowerCase();
            if (lowerCase.contains("nux")) {
                FontSettings.setFontsFolder("/usr/share/fonts/chinese", true);
            }
            Document doc = new Document(wordPath);
            doc.save(os, SaveFormat.PDF);
        }
    }

    public static void main(String[] args) {
        String property = System.getProperty("os.name");
        System.out.println(property);
        Properties properties = System.getProperties();
        properties.list(System.out);
//        wordToPdf("C:\\Users\\xuyanxu\\Downloads\\招标文件 (1).docx","C:\\Users\\xuyanxu\\Downloads\\招标文件 (1).pdf");
    }
}
