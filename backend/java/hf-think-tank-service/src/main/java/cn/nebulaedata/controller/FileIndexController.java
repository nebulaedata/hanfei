package cn.nebulaedata.controller;


//import cn.nebulaedata.entity.Movie;
import cn.nebulaedata.enums.ResponseEnum;
import cn.nebulaedata.form.CurrencyForm;
import cn.nebulaedata.form.DocTaggingForm;
import cn.nebulaedata.pojo.DocFileIndexPojo;
import cn.nebulaedata.pojo.DocFileVerIndexPojo;
//import cn.nebulaedata.pojo.DocTaggingPojo;
import cn.nebulaedata.pojo.DocUserPojo;
import cn.nebulaedata.service.impl.FileIndexServiceImpl;
import cn.nebulaedata.utils.*;
//import cn.nebulaedata.utils.Doc2html;
import cn.nebulaedata.vo.TResponseVo;
import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.rabbitmq.tools.json.JSONUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.io.FileUtils;



@RestController
@RequestMapping("/web/fileindex/")
public class FileIndexController {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndexController.class);

    @Value("${doc-frame-service.upload-path}")
    private String filePath;
    @Value("${doc-frame-service.doc2html-path}")
    private String htmlPath;
    @Value("${doc-frame-service.docUnzip-path}")
    private String docUnzipPath;
//    @Value("${doc-frame-service.send-upload-message-mq}")
//    private String sendPythonMq;
//    @Value("${doc-frame-service.file_pdf_img_mq}")
//    private String filePdfImgMq;
    @Value("${doc-frame-service.file-pdf-save-path}")
    private String filePdfSavePath;
//    @Value("${doc-frame-service.file-search-index-del-mq}")
//    private String fileSearchIndexDelMq;

    @Autowired
    private AmqUtils amqUtils;
    @Autowired
    private FileIndexServiceImpl fileIndexService;

    @GetMapping("show")
    public void show() throws Exception {
//        System.out.println("123");
//        new Doc2html().run();
    }

    @PostMapping("/fileupload")
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo fileupload(@RequestParam(value = "file") MultipartFile file,
                                  @RequestParam(value = "fileTypeId", required = false) String fileTypeId,
                                  @RequestParam(value = "fileLabelIds", required = false) String fileLabelIds,
                                  @RequestParam(value = "includeUserList", required = false) String includeUserList,
                                  @RequestParam(value = "fileName", required = false) String fileName,
                                  @RequestParam(value = "folderId", required = false) String folderId,
                                  HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();

        String fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        if (file.isEmpty()) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传失败");
        }
        String oriFileName = file.getOriginalFilename();
        String extension = oriFileName.substring(oriFileName.lastIndexOf("."));
        if (!extension.equals(".docx")) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传错误,目前只支持docx的文档");
        }

        // 是否重新命名
        if (StringUtils.isBlank(fileName)) {
            // 使用文档原名
            fileName = file.getOriginalFilename();
        } else {
            // 重新命名  (如果新名字长度小于等于5 或者后缀不是.docx 就给他加一个)
            if(fileName.length()<6 || !fileName.substring(fileName.length()-5).equals(".docx") || !fileName.substring(fileName.length()-4).equals(".doc")){
                fileName = fileName + extension;
            }
        }
        File filePathNew = new File(this.filePath + "/" + fileUuid);
        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        File docxFileNew = new File(this.filePath + "/" + fileUuid + "/" + fileUuid + extension);
        try {
            file.transferTo(docxFileNew);
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
        }
        System.out.println("开始解析");
        //TODO 提前清空目标文件夹
        try {
            new Doc2html().wordToHtml(this.filePath + "/" + fileUuid + "/" + fileUuid + extension,
                    htmlPath+ "/",
                    fileUuid + ".html");
            new UnzipUtils().unZipRecursion(this.filePath + "/" + fileUuid + "/" + fileUuid + extension,docUnzipPath+"/"+fileUuid);
            LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传完成");
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, file.getOriginalFilename() + "上传失败");
        }
        // 主表索引信息
        DocFileIndexPojo docFileIndexPojo = new DocFileIndexPojo();
        docFileIndexPojo.setFileUuid(fileUuid);
        docFileIndexPojo.setCreateUserId(userId);
        docFileIndexPojo.setUpdateUserId(userId);
        docFileIndexPojo.setCreateTime(new Date());
        docFileIndexPojo.setFileName(fileName.replaceAll("\\.docx", "").replaceAll("\\.doc", ""));
        docFileIndexPojo.setFileParentId(null);
        docFileIndexPojo.setIsRootFile("1");
        docFileIndexPojo.setFileStatus("1");
        docFileIndexPojo.setFileClass("0");
        docFileIndexPojo.setAncestorsFileUuid(fileUuid);
        docFileIndexPojo.setFolderId(folderId);
        if ("undefined".equals(folderId) || "null".equals(folderId) || StringUtils.isBlank(folderId)) {
            docFileIndexPojo.setFolderId(null);
        }

        docFileIndexPojo.setFileTypeId(fileTypeId);
        String versionId = UUID.randomUUID().toString().replaceAll("-", "");
        docFileIndexPojo.setFileVersionId(versionId);
        if (!StringUtils.isBlank(fileLabelIds)) {
            docFileIndexPojo.setFileLabelList(fileLabelIds);
        }
        docFileIndexPojo.setIncludeUserList(includeUserList);
        // 版本表版本信息
        DocFileVerIndexPojo docFileVerIndexPojo = new DocFileVerIndexPojo();
        docFileVerIndexPojo.setFileVersionId(versionId);
        docFileVerIndexPojo.setFileVersionName(DocFrameUtils.versionCount(null));
        docFileVerIndexPojo.setFileUuid(fileUuid);
        docFileVerIndexPojo.setIsRootVersion("1");
        docFileVerIndexPojo.setParentsVersionId(null);
        docFileVerIndexPojo.setCreateUserId(user.getUserId());
        docFileVerIndexPojo.setUpdateUserId(user.getUserId());
        docFileVerIndexPojo.setCreateTime(new Date());
        docFileVerIndexPojo.setIsDraft("0");

        docFileIndexPojo.setFileVersionId(versionId);
        docFileIndexPojo.setVersions(docFileVerIndexPojo);
        Boolean aBoolean = fileIndexService.AddNewFileService(docFileIndexPojo);


        // 给html打H标签
        String htmlStr = new HtmlUtils().htmlAddHTag(htmlPath+ "/" + fileUuid + ".html",docUnzipPath+"/"+fileUuid+"/word/styles.xml");
        // 拆分返回
        List<Map<String, String>> titleMapList = new HtmlUtils().splitHtmlByTag(htmlStr);
        String sql = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(new Date());
        // 拼装大纲sql
        for (Map<String, String> titleMap : titleMapList) {
            sql = sql + "(\"" + titleMap.get("id") + "\"," + JSON.toJSONString(titleMap.get("title")) + ",\"" + fileUuid + "\",\"" + versionId + "\",\"" + titleMap.get("fatherId") + "\",\"" + titleMap.get("id") + "\",\"" + titleMap.get("level") + "\",\"" + "0" + "\",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
        }
        if (sql.length() != 0) {
            sql = sql.substring(0, sql.length() - 1);
            fileIndexService.insertOutlineService(sql);
        }
        sql = "";
        // 拼装大纲内容sql
        for (Map<String, String> titleMap : titleMapList) {
            sql = sql + "(\"" + UUID.randomUUID().toString().replaceAll("-", "") + "\",\"" + titleMap.get("content").replaceAll("\\\"", "\\\\\"").replaceAll("\\\n","") + "\",\"" + titleMap.get("id") + "\",\"" + fileUuid + "\",\"" + versionId + "\",\"" + dateString + "\"),";
        }
//        System.out.println(sql);
        if (sql.length() != 0) {
            sql = sql.substring(0, sql.length() - 1);
            fileIndexService.insertContentService(sql);
        }
        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "上传成功");
        ret.put("fileUuid", fileUuid);
        ret.put("versionId", versionId);
        return TResponseVo.success(ret);
    }


//    @GetMapping("/filetypedetail")
//    public TResponseVo filetypedetail() throws Exception {
//        return fileIndexService.GetFileTypeDetailService();
//    }

//    @GetMapping("/mydocument")
//    public TResponseVo mydocument(CurrencyForm currencyForm, HttpSession session) throws Exception {
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        String userId = user.getUserId();
//        String searchLike = currencyForm.getSearchLike();
//        Integer pageSize = currencyForm.getPageSize();
//        Integer pageNum = currencyForm.getPageNum();
//        return fileIndexService.GetMyDocumentService(pageNum, pageSize, userId, searchLike);
//    }

    @GetMapping("/searchfile")
    public TResponseVo searchfile(CurrencyForm currencyForm, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = null;
        if (user != null) {
            userId = user.getUserId();
        }
        Integer pageNum = currencyForm.getPageNum();
        Integer pageSize = currencyForm.getPageSize();
        String searchContent = currencyForm.getSearchContent();
        if (StringUtils.isBlank(searchContent)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "搜索内容不能为空");
        }
        String searchIdSelect1 = currencyForm.getSearchIdSelect1();
        if ("-1".equals(searchIdSelect1)) {
            searchIdSelect1 = null;
        }
        String searchIdSelect2 = currencyForm.getSearchIdSelect2();
        if ("-1".equals(searchIdSelect2)) {
            searchIdSelect2 = null;
        }
        String searchIdSelect3 = currencyForm.getSearchIdSelect3();
        if ("-1".equals(searchIdSelect3)) {
            searchIdSelect3 = null;
        }
        return fileIndexService.FileSearchService(pageNum, pageSize, searchContent, searchIdSelect1, searchIdSelect2, searchIdSelect3, userId);
    }

//    @GetMapping("/hotsearch")
//    public TResponseVo hotsearch(CurrencyForm currencyForm) throws Exception {
//        Integer num = currencyForm.getNum();
//        return fileIndexService.GetHotSearchService(num);
//    }

//    @GetMapping("/gethotfile")
//    public TResponseVo gethotfile(CurrencyForm currencyForm) throws Exception {
//        String type = currencyForm.getType();
//        type = StringUtils.defaultIfBlank(type, "collection");
//        return fileIndexService.GetHotFileService(type);
//    }

    @GetMapping("/pdfpreview")
    public TResponseVo pdfpreview(CurrencyForm currencyForm, HttpServletResponse response) throws Exception {
        String fileUuid = currencyForm.getFileUuid();
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            InputStream in = new FileInputStream(new File(filePdfSavePath + fileUuid + ".pdf"));
            int i = IOUtils.copy(in, outputStream);
            in.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return TResponseVo.success();
    }

//    @GetMapping("/getstandfile")
//    public TResponseVo getstandfile() throws Exception {
//        return fileIndexService.GetHotFileService("collection");
//    }

//    @GetMapping("/gettagging")
//    public TResponseVo gettagging() throws Exception {
//        return fileIndexService.GetTaggingService();
//    }

    @GetMapping("/deltext")
    public TResponseVo deltext(CurrencyForm currencyForm) throws Exception {
        String fileUuid = currencyForm.getFileUuid();
        String fileVersionId = currencyForm.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileIndexService.DelDocumentService(fileUuid, fileVersionId);
    }

//    @PostMapping("/posttagging")
//    public TResponseVo posttagging(@RequestBody @Valid DocTaggingForm docTaggingForm, BindingResult bindingResult, HttpSession session) throws Exception {
//        if (bindingResult.hasErrors()) {
//            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}-{}-{}", "posttagging", "提交标注", "必填字段未填写", bindingResult.getFieldError().getField(), bindingResult.getFieldError().getDefaultMessage());
//            return TResponseVo.error(ResponseEnum.WEB_REGISTER_ERROR, bindingResult.getFieldError().getDefaultMessage());
//        }
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        String userId = user.getUserId();
//        String fileUuid = docTaggingForm.getFileUuid();
//        String fileVersionId = docTaggingForm.getFileVersionId();
//        String paragraphInterpretation = docTaggingForm.getParagraphInterpretation();
//        String tipsUuid = docTaggingForm.getTipsUuid();
//        if (StringUtils.isBlank(paragraphInterpretation) && StringUtils.isBlank(tipsUuid)) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        DocTaggingPojo docTaggingPojo = new DocTaggingPojo();
//        BeanUtils.copyProperties(docTaggingForm, docTaggingPojo);
//        docTaggingPojo.setUserId(userId);
//        return fileIndexService.AddTaggingService(docTaggingPojo);
//    }

//    @GetMapping("/gettaggingdetail")
//    public TResponseVo gettaggingdetail(CurrencyForm currencyForm) throws Exception {
//        String fileUuid = currencyForm.getFileUuid();
//        String fileVersionId = currencyForm.getFileVersionId();
//        String fileParagraphId = currencyForm.getFileParagraphId();
//        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(fileParagraphId)) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        return fileIndexService.GetTaggingService(fileUuid, fileVersionId, fileParagraphId);
//    }

//    @GetMapping("/gethistagging")
//    public TResponseVo gethistagging(CurrencyForm currencyForm) throws Exception {
//        String fileUuid = currencyForm.getFileUuid();
//        String fileVersionId = currencyForm.getFileVersionId();
//        String fileParagraphId = currencyForm.getFileParagraphId();
//        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(fileParagraphId)) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        return fileIndexService.GetTaggingHisService(fileUuid, fileVersionId, fileParagraphId);
//    }

//    @GetMapping("/getHomePageDocumentList")
//    public TResponseVo getHomePageDocumentList() throws Exception {
//
//        return fileIndexService.getHomePageDocumentListService();
//    }


}
