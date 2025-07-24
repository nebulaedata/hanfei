package cn.nebulaedata.utils;


import cn.jiayizhen.tools.java.office2.html.creation.JOfficeHtmlCreate;
import cn.jiayizhen.tools.java.office2.jenum.JStyleEnum;
import cn.jiayizhen.tools.java.office2.model.DefaultStyle;
import cn.jiayizhen.tools.java.office2.model.DocxInfoObj;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.pojo.HfMyDownload;
import cn.nebulaedata.socket.WebSocketServer;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashMap;

import static cn.nebulaedata.utils.JsonKeyUtils.bytes2kb;

/**
 * @author 徐衍旭
 * @date 2022/7/20 11:38
 * @note
 */
@Component
public class Html2DocUtils implements Runnable {

    @Autowired
    private WorkingTableMapper workingTableMapper;

    //静态化工具类变量
    public static Html2DocUtils html2DocUtils;

    //注解用于告诉次代码在Spring加载之前就运行
    @PostConstruct
    public void init() {
        html2DocUtils = this;//工具类的实例赋值给Html2DocUtils
        html2DocUtils.workingTableMapper = this.workingTableMapper;//会激活Spring对Dao的管理并赋给此类
        System.out.println("Html2DocUtils工具类已经初始化了，被纳入spring管理");

    }

    private Thread t;
    private String threadUuid;
    private String threadName;
    private String threadPath;
    private String threadHtml;
    private String threadUserId;
    private String threadFileUuid;
    private String threadFileVersionId;
    private String threadDownloadType;
    private String threadFfid;
    private Boolean threadRecord = true;  // 是否留痕

    public Html2DocUtils() {

    }

    public Html2DocUtils(String uuid, String fileUuid, String fileVersionId, String name, String html, String path, String userId, String downloadType) {
        threadUuid = uuid;
        threadFileUuid = fileUuid;
        threadFileVersionId = fileVersionId;
        threadName = name;
        System.out.println("Creating " + threadName);
        threadPath = path;
        threadHtml = html;
        threadUserId = userId;
        threadDownloadType = downloadType;
    }

    //
    public Html2DocUtils(String uuid, String fileUuid, String fileVersionId, String name, String html, String path, String userId, String downloadType, Boolean record, String ffid) {
        threadUuid = uuid;
        threadFileUuid = fileUuid;
        threadFileVersionId = fileVersionId;
        threadName = name;
        System.out.println("Creating " + threadName);
        threadPath = path;
        threadHtml = html;
        threadUserId = userId;
        threadDownloadType = downloadType;
        threadRecord = record;
        threadFfid = ffid;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @SneakyThrows
    @Override
    public void run() {
        System.out.println("Running " + threadName);
        String fileStatus = "正在下载";
        String generateResultDocxFilePath = "";
        String generateResultPdfFilePath = "";
        String generateResultOfdFilePath = "";
        String finalPath = "";
        String sizeInMB = "";
        try {

            // 入库内容
            HfMyDownload hfMyDownload = new HfMyDownload();
            hfMyDownload.setUuid(threadUuid);
            hfMyDownload.setUserId(threadUserId);
            hfMyDownload.setFileName(threadName);
            hfMyDownload.setFileUuid(threadFileUuid);
            hfMyDownload.setFileVersionId(threadFileVersionId);
            hfMyDownload.setFileStatus(fileStatus);
            if (threadRecord) {
                hfMyDownload.setDType("0");
            } else {
                hfMyDownload.setDType("annex");
            }
            if (threadDownloadType == null || "docx".equalsIgnoreCase(threadDownloadType)) {
                hfMyDownload.setFileType("docx");
            }else if ("pdf".equalsIgnoreCase(threadDownloadType)) {
                hfMyDownload.setFileType("pdf");
            }else if ("ofd".equalsIgnoreCase(threadDownloadType)) {
                hfMyDownload.setFileType("ofd");
            }
            hfMyDownload.setFilePath(generateResultDocxFilePath);
            hfMyDownload.setFfid(threadFfid);
            html2DocUtils.workingTableMapper.addHfMyDownloadDao(hfMyDownload);

            // 生成word pdf ofd
            if (threadDownloadType == null || "docx".equalsIgnoreCase(threadDownloadType)) {
                generateResultDocxFilePath = downloadDocx(threadHtml,threadPath,threadName);
                finalPath = generateResultDocxFilePath;
            } else if ("pdf".equalsIgnoreCase(threadDownloadType)) {
                generateResultDocxFilePath = downloadDocx(threadHtml,threadPath,threadName);
                // 创建pdf路径
                generateResultPdfFilePath = generateResultDocxFilePath.substring(0, generateResultDocxFilePath.lastIndexOf("."));
                downloadPDF(generateResultDocxFilePath, generateResultPdfFilePath);
                finalPath = generateResultPdfFilePath;
            } else if ("ofd".equalsIgnoreCase(threadDownloadType)) {
                generateResultDocxFilePath = downloadDocx(threadHtml,threadPath,threadName);
                // 创建pdf路径
                generateResultPdfFilePath = generateResultDocxFilePath.substring(0, generateResultDocxFilePath.lastIndexOf(".")) + ".pdf";
                downloadPDF(generateResultDocxFilePath, generateResultPdfFilePath);
                // 创建ofd路径
                generateResultOfdFilePath = generateResultDocxFilePath.substring(0, generateResultDocxFilePath.lastIndexOf(".")) + ".ofd";
                downloadOFD(generateResultPdfFilePath, generateResultOfdFilePath);
                finalPath = generateResultOfdFilePath;
            }
            try {
                sizeInMB = bytes2kb(new File(finalPath).length());
            } catch (Exception e) {}
            fileStatus = "下载完成";
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Thread " + threadName + " interrupted.");
            fileStatus = "下载异常";
        } finally {
            html2DocUtils.workingTableMapper.updateHfMyDownloadDao(threadUuid, fileStatus, finalPath, sizeInMB);
            // 通知前端
            WebSocketServer webSocketServer = new WebSocketServer();
            HashMap<String, Object> webSocketMap = new HashMap<>();
            String msg = "下载完成";
            System.out.println(msg);
            webSocketMap.put("msg", msg);
            webSocketMap.put("title", "数据下载");
            webSocketMap.put("uuid", threadUuid);
            webSocketMap.put("fileName", threadName);
            webSocketMap.put("status", "下载完成".equals(fileStatus) ? "success" : "fail");
            webSocketServer.sendInfo(webSocketMap, threadUserId + "_downloadMyDownload");
        }
        System.out.println("Thread " + threadName + " exiting.");
    }

    public String downloadDocx(String threadHtml,String basePath,String fileName) throws Exception {
        String html = threadHtml;
        html = html.replaceAll("\"/application/hf/", "\"http://han.nebulaedata.cn/application/hf/");
        html = html.replaceAll("\"/application/tools/", "\"http://tools.nebulaedata.cn/application/tools/");
        html = html.replaceAll("\"/application/hf_sit/", "\"http://hansit.nebulaedata.cn/application/hf_sit/");
        html = html.replaceAll("\"/application/tools_sit/", "\"https://toolssit.nebulaedata.cn/application/tools_sit/");
        html = html.replaceAll("\"/application/tools_bj_sit/", "\"https://toolssit.nebulaedata.cn/application/tools_bj_sit/");
        html = html.replaceAll("\"/application/tools_bj/", "\"https://toolswz.nebulaedata.cn/application/tools_bj/");
        html = html.replaceAll("\"/application/tools_xl/", "\"https://toolsxl.nebulaedata.cn/application/tools_xl/");
        DefaultStyle defaultStyle = new DefaultStyle();
        defaultStyle.setFontFamily(JStyleEnum.J_FONTS_ENUM.宋体);
        defaultStyle.setRowSpacingPoundStyle("20");
        defaultStyle.setRowTableSpacingMultipleStyle("1");
        defaultStyle.setTableCellAlignCenterStyle(true);
        DocxInfoObj docxInfoObj = JOfficeHtmlCreate.parseHtml(html, defaultStyle, 668l);
        File filePathNew = new File(basePath);
        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        docxInfoObj.setBasePath(basePath);
        docxInfoObj.setFileName(fileName);
        docxInfoObj.saveDocxFile();
        return docxInfoObj.getGenerateResultDocxFilePath();
    }

    public void downloadPDF(String wordPath, String pdfPath) {
        WordToPdfUtils.wordToPdf(wordPath, pdfPath);
    }

    public void downloadOFD(String pdfPath, String ofdPath) throws IOException {
        OFDUtils.pdfToOfd(pdfPath, ofdPath);
    }

    public void start() {
        System.out.println("Starting " + threadName);
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }

}
