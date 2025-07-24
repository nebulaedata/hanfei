package cn.nebulaedata.controller;


//import cn.nebulaedata.entity.Movie;

import cn.nebulaedata.enums.ResponseEnum;
import cn.nebulaedata.form.CurrencyForm;
import cn.nebulaedata.pojo.*;
import cn.nebulaedata.service.EditToolService;
import cn.nebulaedata.service.FileOperationService;
import cn.nebulaedata.service.impl.EditToolServiceImpl;
import cn.nebulaedata.service.impl.FileIndexServiceImpl;
import cn.nebulaedata.service.impl.FileOperationServiceImpl;
import cn.nebulaedata.utils.AmqUtils;
import cn.nebulaedata.utils.Doc2html;
import cn.nebulaedata.utils.DocFrameUtils;
import cn.nebulaedata.utils.HtmlUtils;
import cn.nebulaedata.vo.TResponseVo;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

//import cn.nebulaedata.pojo.DocTaggingPojo;
//import cn.nebulaedata.utils.Doc2html;


@RestController
@RequestMapping("/web/editTool/")
public class EditToolController {

    private static final Logger LOG = LoggerFactory.getLogger(EditToolController.class);

    @Autowired
    private EditToolServiceImpl editToolServiceImpl;
    @Value("${doc-frame-service.upload-path}")
    private String filePath;
    @Value("${doc-frame-service.doc2html-path}")
    private String htmlPath;
    @Value("${doc-frame-service.docUnzip-path}")
    private String docUnzipPath;

    @Autowired
    private FileIndexServiceImpl fileIndexService;
    @Autowired
    private FileOperationServiceImpl fileOperationServiceImpl;

    @PostMapping("/newFolder")
    public TResponseVo newFolder(@RequestBody EditToolFolderPojo editToolFolderPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        editToolFolderPojo.setCreateUserId(userId);
        return editToolServiceImpl.newFolderService(editToolFolderPojo);
    }

    @PostMapping("/delFolder")
    public TResponseVo delFolder(@RequestBody EditToolFolderPojo editToolFolderPojo, HttpSession session) throws Exception {
        List<String> keyList = editToolFolderPojo.getKeyList();
        return editToolServiceImpl.delFolderService(keyList);
    }

    @PostMapping("/renameFolder")
    public TResponseVo renameFolder(@RequestBody EditToolFolderPojo editToolFolderPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        editToolFolderPojo.setCreateUserId(userId);
        return editToolServiceImpl.renameFolderService(editToolFolderPojo);
    }

    @PostMapping("/renameDocument")
    public TResponseVo renameDocument(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        return editToolServiceImpl.renameDocumentService(docFileIndexPojo);
    }

    @PostMapping("/getFolderInfo")
    public TResponseVo getFolderInfo(@RequestBody EditToolFolderPojo editToolFolderPojo, HttpSession session) throws Exception {
        String folderId = editToolFolderPojo.getFolderId();
        return editToolServiceImpl.getFolderInfoService(folderId);
    }

    @PostMapping("/getDocumentInfo")
    public TResponseVo getDocumentInfo(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        return editToolServiceImpl.getDocumentInfoService(fileUuid);
    }

    @PostMapping("/getMoveFolderList")
    public TResponseVo getMoveFolderList(@RequestBody(required = false) PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return editToolServiceImpl.getMoveFolderListService(pagePojo, userId);
    }

    @PostMapping("/moveFolder")
    public TResponseVo moveFolder(@RequestBody EditToolFolderPojo editToolFolderPojo, HttpSession session) throws Exception {
        List<String> keyList = editToolFolderPojo.getKeyList();
        String folderId = editToolFolderPojo.getFolderId();
        return editToolServiceImpl.moveFolderService(keyList, folderId);
    }

    @PostMapping("/getFolderList")
    public TResponseVo getFolderList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return editToolServiceImpl.getFolderListService(pagePojo, userId);
    }

    @PostMapping("/getFolderListLV")
    public TResponseVo getFolderListLV(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return editToolServiceImpl.getFolderListLVService(userId);
    }


    @PostMapping("/search")
    public TResponseVo search(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        return editToolServiceImpl.searchService(pagePojo);
    }

    @PostMapping("/getRecentFile")
    public TResponseVo getRecentFile(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return editToolServiceImpl.getRecentFileService(userId);
    }

    @PostMapping("/getApplicationList")
    public TResponseVo getApplicationList(@RequestBody(required = false) HfFileTypeDiPojo hfFileTypeDiPojo) throws Exception {
        return editToolServiceImpl.getApplicationListService(hfFileTypeDiPojo);
    }

    @PostMapping("/changeApplicationStatus")
    public TResponseVo changeApplicationStatus(@RequestBody HfFileTypeDiPojo hfFileTypeDiPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return editToolServiceImpl.changeApplicationStatusService(hfFileTypeDiPojo);
    }


    @PostMapping("/modelFileUpload")
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo fileupload(@RequestParam(value = "file") MultipartFile file,
                                  @RequestParam(value = "fileTypeId", required = false) String fileTypeId,
                                  @RequestParam(value = "fileLabelIds", required = false) String fileLabelIds,
                                  @RequestParam(value = "includeUserList", required = false) String includeUserList,
                                  @RequestParam(value = "fileName", required = false) String fileName,
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
            if (fileName.length() < 6 || !fileName.substring(fileName.length() - 5).equals(".docx") || !fileName.substring(fileName.length() - 4).equals(".doc")) {
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
                    htmlPath + "/",
                    fileUuid + ".html");
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
        docFileIndexPojo.setFileClass("1");
        docFileIndexPojo.setAncestorsFileUuid(fileUuid);

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
        docFileIndexPojo.setAssessStatus("编制中");
        Boolean aBoolean = fileIndexService.AddNewFileService(docFileIndexPojo);


        // 给html打H标签
        String htmlStr = new HtmlUtils().htmlAddHTag(htmlPath + "/" + fileUuid + ".html", docUnzipPath + "/" + fileUuid + "/word/styles.xml");
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
            sql = sql + "(\"" + UUID.randomUUID().toString().replaceAll("-", "") + "\",\"" + titleMap.get("content").replaceAll("\\\"", "\\\\\"").replaceAll("\\\n", "") + "\",\"" + titleMap.get("id") + "\",\"" + fileUuid + "\",\"" + versionId + "\",\"" + dateString + "\"),";
        }
//        System.out.println(sql);
        if (sql.length() != 0) {
            sql = sql.substring(0, sql.length() - 1);
            fileIndexService.insertContentService(sql);
        }
        return fileOperationServiceImpl.getTrueVersionIdService(fileUuid, session);
//        HashMap<String, String> ret = new HashMap<>();
//        ret.put("info", "上传成功");
//        ret.put("fileUuid", fileUuid);
//        ret.put("fileVersionId", versionId);
//        return TResponseVo.success(ret);
    }

    @PostMapping("/addPramsWorkflow")
    public TResponseVo addPramsWorkflow(@RequestBody ParamsWorkflowPojo paramsWorkflowPojo) throws Exception {
        return editToolServiceImpl.addParamsWorkflowService(paramsWorkflowPojo);
    }

    @PostMapping("/deletePramsWorkflow")
    public TResponseVo deletePramsWorkflow(@RequestBody ParamsWorkflowPojo paramsWorkflowPojo) throws Exception {
        return editToolServiceImpl.deleteParamsWorkflowService(paramsWorkflowPojo);
    }

    @PostMapping("/searchPramsWorkflow")
    public TResponseVo searchPramsWorkflow(@RequestBody ParamsWorkflowPojo paramsWorkflowPojo) throws Exception {
        return editToolServiceImpl.searchParamsWorkflowService(paramsWorkflowPojo);
    }

    @PostMapping("/updatePramsWorkflow")
    public TResponseVo updatePramsWorkflow(@RequestBody ParamsWorkflowPojo paramsWorkflowPojo) throws Exception {
        return editToolServiceImpl.updateParamsWorkflowService(paramsWorkflowPojo);
    }
}
