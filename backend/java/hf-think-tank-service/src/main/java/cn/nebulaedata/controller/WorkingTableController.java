package cn.nebulaedata.controller;

import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.enums.ResponseEnum;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.pojo.*;
import cn.nebulaedata.service.impl.FileOperationServiceImpl;
//import cn.nebulaedata.service.impl.Neo4jServiceImpl;
import cn.nebulaedata.service.impl.WorkingTableServiceImpl;
//import cn.nebulaedata.utils.Neo4jApiUtil;
import cn.nebulaedata.utils.*;
import cn.nebulaedata.vo.TResponseVo;
import cn.nebulaedata.web.excel.common.CommonExcelSaveFun;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.*;

/**
 * @author 徐衍旭
 * @date 2021/8/12 13:48
 * @note
 */
@RestController
@RequestMapping("/worktable/")
public class WorkingTableController {

    private static final Logger LOG = LoggerFactory.getLogger(cn.nebulaedata.controller.WorkingTableController.class);

    @Value("${doc-frame-service.company-path}")
    private String companyPath;
    @Value("${doc-frame-service.upload-json-path}")
    private String uploadJsonPath;
    @Value("${doc-frame-service.teach-video-path}")
    private String teachVideoPath;

    @Autowired
    private WorkingTableServiceImpl workingTableServiceImpl;

    @Autowired
    private FileOperationServiceImpl fileOperationServiceImpl;

    @Autowired
    private WorkingTableMapper workingTableMapper;

    @Autowired
    private CommonExcelSaveFun commonExcelSaveFun;

    @GetMapping("show")
    public TResponseVo show() {
        System.out.println("123");
        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("data", "hello");
        return TResponseVo.success(objectObjectHashMap);
    }

    @GetMapping("getOutlineList")
    public TResponseVo getOutlineList(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        // 通知前端
//        WebSocketServer webSocketServer = new WebSocketServer();
//        webSocketServer.sendInfo("{\"\"}", userId);
        /**
         * 工作台-获取大纲树
         */
        return workingTableServiceImpl.getOutlineListService(fileUuid, fileVersionId);
    }


    @GetMapping("getContent")
    public TResponseVo getContent(DocFileIndexPojo docFileIndexPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String outlineId = docFileIndexPojo.getOutlineId();
        Boolean origin = docFileIndexPojo.getOrigin();
        String userId = user.getUserId();
//        user.setUserId("110");
        docFileIndexPojo.setSearchUuid(pageId);
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(userId)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return workingTableServiceImpl.getContentService(fileUuid, fileVersionId, outlineId, origin, userId, pageId);
    }


    @PostMapping("addProject")
    public TResponseVo addProject(@RequestBody ProjectPojo projectPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        /**
         * 工作台-新建项目
         */
        return workingTableServiceImpl.addProjectService(projectPojo, userId);
    }

    @GetMapping("delProject")
    public TResponseVo delProject(String projectId) throws Exception {

        /**
         * 工作台-删除项目
         */
        return workingTableServiceImpl.delProjectService(projectId);
    }

    @PostMapping("updateProject")
    public TResponseVo updateProject(@RequestBody ProjectPojo projectPojo) throws Exception {

        /**
         * 工作台-修改项目
         */
        return workingTableServiceImpl.updateProjectService(projectPojo);
    }

    @GetMapping("getProjectInfo")
    public TResponseVo getProjectInfo(String projectId) throws Exception {

        /**
         * 工作台-获取项目信息
         */
        return workingTableServiceImpl.getProjectInfoService(projectId);
    }

    @GetMapping("getProjectList")
    public TResponseVo getProjectList(PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        /**
         * 工作台-获取项目列表
         */
        return workingTableServiceImpl.getProjectListService(pagePojo, userId);
    }

    @GetMapping("addStage")
    public TResponseVo addStage(StagePojo stagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        /**
         * 工作台-新建标段
         */
        return workingTableServiceImpl.addStageService(stagePojo, userId);
    }

    @GetMapping("delStage")
    public TResponseVo delStage(String stageId) throws Exception {

        /**
         * 工作台-删除标段
         */
        return workingTableServiceImpl.delStageService(stageId);
    }

    @GetMapping("updateStage")
    public TResponseVo updateStage(StagePojo stagePojo) throws Exception {

        /**
         * 工作台-修改标段
         */
        return workingTableServiceImpl.updateStageService(stagePojo);
    }

    @GetMapping("getStageInfo")
    public TResponseVo getStageInfo(String stageId) throws Exception {

        /**
         * 工作台-获取标段信息
         */
        return workingTableServiceImpl.getStageInfoService(stageId);
    }

    @GetMapping("getStageList")
    public TResponseVo getStageList(PagePojo pagePojo, String projectId) throws Exception {

        /**
         * 工作台-获取标段列表
         */
        return workingTableServiceImpl.getStageListService(pagePojo, projectId);
    }


    @PostMapping("addBatch")
    public TResponseVo addBatch(@RequestBody BatchPojo batchPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        /**
         * 工作台-新建批次
         */
        return workingTableServiceImpl.addBatchService(batchPojo, userId);
    }

    @PostMapping("delBatch")
    public TResponseVo delBatch(@RequestBody BatchPojo batchPojo) throws Exception {
        String batchId = batchPojo.getBatchId();
        /**
         * 工作台-删除批次
         */
        return workingTableServiceImpl.delBatchService(batchId);
    }

    @PostMapping("updateBatch")
    public TResponseVo updateBatch(@RequestBody BatchPojo batchPojo) throws Exception {

        /**
         * 工作台-修改批次
         */
        return workingTableServiceImpl.updateBatchService(batchPojo);
    }


    /**
     * 工作台-修改项目属性信息
     */
    @PostMapping("updateBatchProperty")
    public TResponseVo updateBatchProperty(@RequestBody LabelValuePojo labelValuePojo) throws Exception {
        return workingTableServiceImpl.updateBatchPropertyService(labelValuePojo);
    }


    @PostMapping("getBatchInfo")
    public TResponseVo getBatchInfo(@RequestBody BatchPojo batchPojo) throws Exception {
        String batchId = batchPojo.getBatchId();

        /**
         * 工作台-获取批次信息
         */
        return workingTableServiceImpl.getBatchInfoService(batchId);
    }

    @PostMapping("getBatchList")
    public TResponseVo getBatchList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        /**
         * 工作台-获取批次列表
         */
        return workingTableServiceImpl.getBatchListService(pagePojo, userId);
    }


    /**
     * 获取选项
     */
    @GetMapping("getOptions")
    public TResponseVo getOptions(String type) throws Exception {
        return workingTableServiceImpl.getOptionsService(type);
    }

    /**
     * 评标工具-获取评审标准类型
     */
    @GetMapping("getElementTypeOptions")
    public TResponseVo getElementTypeOptions(String fileUuid, String assessId) throws Exception {
        return workingTableServiceImpl.getElementTypeOptionsService(fileUuid, assessId);
    }

    /**
     * 批次管理-项目列表-添加项目
     */
    @PostMapping("addBatchProject")
    public TResponseVo addBatchProject(@RequestBody BatchPojo batchPojo) throws Exception {
        String batchId = batchPojo.getBatchId();
        String projectStageName = batchPojo.getProjectStageName();
        String buyType = batchPojo.getBuyType();
        String mainPerson = batchPojo.getMainPerson();
        return workingTableServiceImpl.addBatchProjectService(batchId, projectStageName, buyType, mainPerson);
    }

    /**
     * 批次管理-项目列表-删除项目
     */
    @PostMapping("delBatchProject")
    public TResponseVo delBatchProject(@RequestBody BatchPojo batchPojo) throws Exception {
        String batchId = batchPojo.getBatchId();
        String projectStageId = batchPojo.getProjectStageId();
        return workingTableServiceImpl.delBatchProjectService(batchId, projectStageId);
    }

    /**
     * 批次管理-项目列表-获取项目信息
     */
    @PostMapping("getBatchProjectInfo")
    public TResponseVo getBatchProjectInfo(@RequestBody BatchPojo batchPojo) throws Exception {
        String batchId = batchPojo.getBatchId();
        String projectStageId = batchPojo.getProjectStageId();
        return workingTableServiceImpl.getBatchProjectInfoService(batchId, projectStageId);
    }

    /**
     * 批次管理-项目列表-获取项目列表
     */
    @PostMapping("getBatchProjectList")
    public TResponseVo getBatchProjectList(@RequestBody PagePojo pagePojo) throws Exception {
        String batchId = pagePojo.getBatchId();
        return workingTableServiceImpl.getBatchProjectListService(pagePojo, batchId);
    }


    /**
     * 批次信息-添加人员
     */
    @PostMapping("addBatchUser")
    public TResponseVo addBatchUser(@RequestBody BatchPojo batchPojo) throws Exception {
        String userId = batchPojo.getUserId();
        String batchId = batchPojo.getBatchId();
        return workingTableServiceImpl.addBatchUserService(batchId, userId);
    }

    /**
     * 批次信息-删除人员
     */
    @PostMapping("delBatchUser")
    public TResponseVo delBatchUser(@RequestBody BatchPojo batchPojo) throws Exception {
        String userId = batchPojo.getUserId();
        String batchId = batchPojo.getBatchId();
        return workingTableServiceImpl.delBatchUserService(batchId, userId);
    }

    /**
     * 批次信息-获取用户清单
     */
    @PostMapping("getBatchUserList")
    public TResponseVo getBatchUserList(@RequestBody PagePojo pagePojo) throws Exception {
        String batchId = pagePojo.getBatchId();
        return workingTableServiceImpl.getBatchUserListService(pagePojo, batchId);
    }

    /**
     * 批次信息-获取用户清单
     */
    @PostMapping("orderBatchProjectList")
    public TResponseVo orderBatchProjectList(@RequestBody BatchPojo batchPojo) throws Exception {
        return workingTableServiceImpl.orderBatchProjectListService(batchPojo);
    }

    /**
     * 上传批次文件
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("addBatchFile")
    public TResponseVo uploadBatchFile(@RequestParam(value = "file") MultipartFile file,
                                       @RequestParam(value = "batchId") String batchId,
                                       @RequestParam(value = "fileName", required = false) String fileName,
                                       @RequestParam(value = "fileTypeId", required = false) String fileTypeId,
                                       @RequestParam(value = "projectStageId", required = false) String projectStageId,
                                       HttpSession session
    ) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.uploadBatchFileService(file, userId, batchId, fileName, fileTypeId, projectStageId);
    }

    /**
     * 删除批次文件
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("delBatchFile")
    public TResponseVo delBatchFile(@RequestBody BatchFilePojo batchFilePojo) throws Exception {
        String fileUuid = batchFilePojo.getFileUuid();
        return workingTableServiceImpl.delBatchFileService(fileUuid);
    }

    /**
     * 修改批次文件
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("updateBatchFile")
    public TResponseVo updateBatchFile(@RequestBody BatchFilePojo batchFilePojo) throws Exception {
        String fileUuid = batchFilePojo.getFileUuid();
        String projectStageId = batchFilePojo.getProjectStageId();
        return workingTableServiceImpl.updateBatchFileService(fileUuid, projectStageId);
    }

    /**
     * 查询批次文件信息
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("getBatchFileInfo")
    public TResponseVo getBatchFileInfo(@RequestBody BatchFilePojo batchFilePojo) throws Exception {
        String fileUuid = batchFilePojo.getFileUuid();
        return workingTableServiceImpl.getBatchFileInfoService(fileUuid);
    }

    /**
     * 查询批次文件清单
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("getBatchFileList")
    public TResponseVo getBatchFileList(@RequestBody PagePojo pagePojo) throws Exception {
        String batchId = pagePojo.getBatchId();
        String projectStageId = pagePojo.getProjectStageId();
        return workingTableServiceImpl.getBatchFileListService(pagePojo, batchId, projectStageId);
    }


    @GetMapping("getPackagePlan")
    public TResponseVo getPackagePlan() throws Exception {
        /**
         * 工作台-获取项目策划预选方案
         */
        return workingTableServiceImpl.getPackagePlanService();
    }

    @PostMapping("addPackageByPlan")
    public TResponseVo addPackageByPlan(@RequestBody PackagePojo packagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        packagePojo.setCreateUserId(userId);
        /**
         * 工作台-根据策划新建若干分包
         */
        return workingTableServiceImpl.addPackageByPlanService(packagePojo);
    }


    @PostMapping("addPackage")
    public TResponseVo addPackage(@RequestBody PackagePojo packagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        packagePojo.setCreateUserId(userId);
        /**
         * 工作台-新建分包
         */
        return workingTableServiceImpl.addPackageService(packagePojo);
    }

    @GetMapping("delPackage")
    public TResponseVo delPackage(String packageId) throws Exception {

        /**
         * 工作台-删除分包
         */
        return workingTableServiceImpl.delPackageService(packageId);
    }

    @PostMapping("updatePackage")
    public TResponseVo updatePackage(@RequestBody PackagePojo packagePojo) throws Exception {

        /**
         * 工作台-修改分包
         */
        return workingTableServiceImpl.updatePackageService(packagePojo);
    }

    @GetMapping("getPackageInfo")
    public TResponseVo getPackageInfo(String packageId) throws Exception {

        /**
         * 工作台-获取分包信息
         */
        return workingTableServiceImpl.getPackageInfoService(packageId);
    }

    @GetMapping("getPackageList")
    public TResponseVo getPackageList(PagePojo pagePojo, String projectId) throws Exception {

        /**
         * 工作台-获取分包列表
         */
        return workingTableServiceImpl.getPackageListService(pagePojo, projectId);
    }


    @PostMapping("addCompany")
    public TResponseVo addCompany(
            @RequestParam(value = "businessLicenseFile", required = false) MultipartFile businessLicenseFile,
            @RequestParam(value = "bankLicenseFile", required = false) MultipartFile bankLicenseFile,
            CompanyPojo companyPojo, HttpSession session
    ) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";

//        MultipartFile businessLicenseFile = companyPojo.getBusinessLicenseFile();
//        MultipartFile bankLicenseFile = companyPojo.getBankLicenseFile();

        String businessFileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        String bankFileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        String companyId = UUID.randomUUID().toString().replaceAll("-", "");
        companyPojo.setCompanyId(companyId);

        // 检查文件是否上传
        if (businessLicenseFile != null) {
            // 检查文件后缀名是否正确
            String businessLicenseFileName = businessLicenseFile.getOriginalFilename();
            String extension = businessLicenseFileName.substring(businessLicenseFileName.lastIndexOf(".")).toLowerCase();
            List<String> strings = Arrays.asList(".jpg", ".png", ".bmp", ".jpeg");
            if (!strings.contains(extension)) {
                return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传错误,目前只支持图片格式");
            }
            // 上传营业执照文件
            File filePathNew = new File(this.companyPath + "/" + companyId);
            if (!filePathNew.exists()) {
                filePathNew.mkdir();
            }
            File businessLicenseFileNew = new File(this.companyPath + "/" + companyId + "/" + businessLicenseFileName);
            try {
                businessLicenseFile.transferTo(businessLicenseFileNew);
            } catch (IOException e) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
            }
            companyPojo.setBusinessLicense(this.companyPath + "/" + companyId + "/" + businessLicenseFileName);
        }
        // 检查文件是否上传
        if (bankLicenseFile != null) {
            // 检查文件后缀名是否正确
            String bankLicenseFileName = bankLicenseFile.getOriginalFilename();
            String extension = bankLicenseFileName.substring(bankLicenseFileName.lastIndexOf(".")).toLowerCase();
            List<String> strings = Arrays.asList(".jpg", ".png", ".bmp", ".jpeg");
            if (!strings.contains(extension)) {
                return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传错误,只支持图片格式");
            }

            // 上传开户许可文件
            File filePathNew = new File(this.companyPath + "/" + companyId);
            if (!filePathNew.exists()) {
                filePathNew.mkdir();
            }
            File bankLicenseFileNew = new File(this.companyPath + "/" + companyId + "/" + bankLicenseFileName);
            try {
                bankLicenseFile.transferTo(bankLicenseFileNew);
            } catch (IOException e) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
            }
            companyPojo.setBankLicense(this.companyPath + "/" + companyId + "/" + bankLicenseFileName);
        }
        /**
         * 工作台-新建公司
         */
        return workingTableServiceImpl.addCompanyService(companyPojo, userId);
    }

    @GetMapping("delCompany")
    public TResponseVo delCompany(String companyId) throws Exception {

        /**
         * 工作台-删除公司
         */
        return workingTableServiceImpl.delCompanyService(companyId);
    }

    @PostMapping("updateCompany")
    public TResponseVo updateCompany(@RequestParam(value = "businessLicenseFile", required = false) MultipartFile businessLicenseFile,
                                     @RequestParam(value = "bankLicenseFile", required = false) MultipartFile bankLicenseFile,
                                     CompanyPojo companyPojo) throws Exception {
        String companyId = companyPojo.getCompanyId();
        CompanyPojo companyPojoDao = workingTableMapper.getCompanyInfoDao(companyId);
        if (businessLicenseFile != null) {
            // 替换掉原文件
            String businessLicense = companyPojoDao.getBusinessLicense();

            // 检查文件后缀名是否正确
            String businessLicenseFileName = businessLicenseFile.getOriginalFilename();
            String extension = businessLicenseFileName.substring(businessLicenseFileName.lastIndexOf(".")).toLowerCase();
            List<String> strings = Arrays.asList(".jpg", ".png", ".bmp", ".jpeg");
            if (!strings.contains(extension)) {
                return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传错误,目前只支持图片格式");
            }

            File businessLicenseFileNew = null;
            if (businessLicense == null) {
                // 说明文件不存在
            } else {
                // 若存在 先删除
                File file = new File(businessLicense);
                if (file.isFile()) {
                    file.delete();
                }
            }
            // 创建目录
            File filePathNew = new File(this.companyPath + "/" + companyId);
            if (!filePathNew.exists()) {
                filePathNew.mkdir();
            }
            businessLicenseFileNew = new File(this.companyPath + "/" + companyId + "/" + businessLicenseFileName);
            businessLicense = this.companyPath + "/" + companyId + "/" + businessLicenseFileName;
            try {
                businessLicenseFile.transferTo(businessLicenseFileNew);
            } catch (IOException e) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
            }
            companyPojo.setBusinessLicense(businessLicense);
        }

        if (bankLicenseFile != null) {
            // 替换掉原文件
            String bankLicense = companyPojoDao.getBankLicense();

            // 检查文件后缀名是否正确
            String bankLicenseFileName = bankLicenseFile.getOriginalFilename();
            String extension = bankLicenseFileName.substring(bankLicenseFileName.lastIndexOf(".")).toLowerCase();
            List<String> strings = Arrays.asList(".jpg", ".png", ".bmp", ".jpeg");
            if (!strings.contains(extension)) {
                return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传错误,目前只支持图片格式");
            }

            File bankLicenseFileNew = null;
            if (bankLicense == null) {
                // 说明文件不存在
            } else {
                // 若存在 先删除
                File file = new File(bankLicense);
                if (file.isFile()) {
                    file.delete();
                }

            }
            // 创建目录
            File filePathNew = new File(this.companyPath + "/" + companyId);
            if (!filePathNew.exists()) {
                filePathNew.mkdir();
            }
            bankLicenseFileNew = new File(this.companyPath + "/" + companyId + "/" + bankLicenseFileName);
            bankLicense = this.companyPath + "/" + companyId + "/" + bankLicenseFileName;
            try {
                bankLicenseFile.transferTo(bankLicenseFileNew);
            } catch (IOException e) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
            }
            companyPojo.setBankLicense(bankLicense);
        }
        /**
         * 工作台-修改公司信息
         */
        return workingTableServiceImpl.updateCompanyService(companyPojo);
    }

    @GetMapping("getCompanyInfo")
    public TResponseVo getCompanyInfo(String companyId) throws Exception {

        /**
         * 工作台-获取公司信息
         */
        return workingTableServiceImpl.getCompanyInfoService(companyId);
    }

    @GetMapping("getCompanyList")
    public TResponseVo getCompanyList(PagePojo pagePojo, HttpSession session, String type) throws Exception {
        /**
         * 工作台-获取公司列表
         */
        if (type == null || !type.equals("all")) {
            DocUserPojo user = (DocUserPojo) session.getAttribute("user");
            String userId = user.getUserId();
//            String userId = "110";
            return workingTableServiceImpl.getCompanyListService(pagePojo, userId);
        } else if (type.equals("all")) {
            return workingTableServiceImpl.getCompanyListService(pagePojo, null);
        }
        return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
    }


    @GetMapping("setDefaultCompany")
    public TResponseVo setDefaultCompany(String companyId, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";
        /**
         * 工作台-设置默认公司
         */
        return workingTableServiceImpl.setDefaultCompanyService(companyId, userId);
    }


    @GetMapping("quitCompany")
    public TResponseVo quitCompany(String companyId, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";
        /**
         * 工作台-退出公司
         */
        return workingTableServiceImpl.quitCompanyService(companyId, userId);
    }


    @GetMapping("getCompanyByAuth")
    public TResponseVo getCompanyByAuth(String authorizationCode) throws Exception {
        /**
         * 工作台-获取公司信息
         */
        return workingTableServiceImpl.getCompanyByAuthService(authorizationCode);
    }

    @GetMapping("applyJoinCompany")
    public TResponseVo applyCompany(String companyId, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        /**
         * 工作台-申请加入公司
         */
        return workingTableServiceImpl.applyJoinCompanyService(companyId, userId);
    }

    @PostMapping("joinCompany")
    public TResponseVo joinCompany(@RequestParam(value = "applyContent") String applyContent) throws Exception {
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        String userId = user.getUserId();
        /**
         * 工作台-加入公司
         */
        return workingTableServiceImpl.joinCompanyService(applyContent);
    }


    @GetMapping("projectPlan")
    public TResponseVo projectPlan(String projectId) throws Exception {

        /**
         * 工作台-项目策划
         */
        return workingTableServiceImpl.projectPlanService(projectId);
    }

    @GetMapping("getIntroductionList")
    public TResponseVo getIntroductionList(String projectTypeId) throws Exception {

        /**
         * 工作台-获取策划推荐
         */
        return workingTableServiceImpl.getIntroductionListService(projectTypeId);
    }

    @GetMapping("getDocumentList")
    public TResponseVo getDocumentList(String projectTypeId, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        /**
         * 工作台-获取策划推荐
         */
        return workingTableServiceImpl.getDocumentListService(userId);
    }

    @PostMapping("addProjectFile")
    public TResponseVo addProjectFile(@RequestParam(value = "file", required = false) MultipartFile file,
                                      @RequestParam(value = "projectId") String projectId,
                                      @RequestParam(value = "fileName", required = false) String fileName,
                                      @RequestParam(value = "packageId", required = false) String packageId,
                                      @RequestParam(value = "fileInfo", required = false) String fileInfo,
                                      @RequestParam(value = "fileType", required = false) String fileType,
                                      @RequestParam(value = "fileUuid", required = false) String fileUuid,
                                      HttpSession session) throws Exception {

        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        /**
         * 工作台-获取策划推荐
         */
        return workingTableServiceImpl.addProjectFileService(file, projectId, fileName, packageId, userId, fileInfo, fileType, fileUuid);
    }

    @PostMapping("uploadFile")
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo uploadFile(@RequestParam(value = "file") MultipartFile file,
                                  @RequestParam(value = "projectId") String projectId,
                                  @RequestParam(value = "fileName", required = false) String fileName,
                                  @RequestParam(value = "packageId", required = false) String packageId,
                                  @RequestParam(value = "fileInfo", required = false) String fileInfo,
                                  @RequestParam(value = "fileType", required = false) String fileType,
                                  HttpSession session
    ) throws Exception {

        /**
         * 工作台-上传项目文件
         */
        if (file.isEmpty()) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传失败");
        }
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.uploadFileService(file, projectId, fileName, packageId, userId, fileInfo, fileType);
    }

    @GetMapping("downloadFile")
    public TResponseVo materialDownload(String fileUuid, HttpServletResponse response) throws Exception {
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return workingTableServiceImpl.downloadFileService(fileUuid, response);
    }

    @GetMapping("delProjectFile")
    public TResponseVo delProjectFile(String fileUuid) throws Exception {

        /**
         * 工作台-删除项目文件
         */
        return workingTableServiceImpl.delProjectFileService(fileUuid);
    }

    @GetMapping("conformFileStatus")
    public TResponseVo conformFileStatus(String fileUuid, String fileStatus) throws Exception {

        /**
         * 工作台-确认文件状态
         */
        return workingTableServiceImpl.conformFileStatusService(fileUuid, fileStatus);
    }

//    @GetMapping("getStageIdList")
//    public TResponseVo getStageIdList(String projectId, String stageName) throws Exception {
//
//        /**
//         * 工作台-获取项目标段列表
//         */
//        return workingTableServiceImpl.getStageIdListService(projectId, stageName);
//    }

//    @GetMapping("updateFileStageId")
//    public TResponseVo updateFileStageId(String fileUuid, String stageId) throws Exception {
//
//        /**
//         * 工作台-修改文件所属分标
//         */
//        return workingTableServiceImpl.updateFileStageIdService(fileUuid, stageId);
//    }

    @GetMapping("getProjectFileTypeList")
    public TResponseVo getProjectFileTypeList(String typeGroupId) throws Exception {

        /**
         * 工作台-获取项目文件属性列表
         */
        return workingTableServiceImpl.getProjectFileTypeListService(typeGroupId);
    }

    @PostMapping("updateFileProperty")
    public TResponseVo updateFileStageId(@RequestBody ProjectFilePojo projectFilePojo) throws Exception {

        /**
         * 工作台-修改文件属性
         */
        return workingTableServiceImpl.updateFilePropertyService(projectFilePojo);
    }

    @GetMapping("getProjectFileList")
    public TResponseVo getProjectFileList(PagePojo pagePojo, ProjectFilePojo projectFilePojo) throws Exception {

        /**
         * 工作台-获取文件列表
         */
        return workingTableServiceImpl.getProjectFileListService(pagePojo, projectFilePojo);
    }


    @GetMapping("getMemoryInfo")
    public TResponseVo getMemoryInfo(String projectId) throws Exception {

        /**
         * 工作台-获取存储空间
         */
        return workingTableServiceImpl.getMemoryInfoService(projectId);
    }

    @GetMapping("getUserStageList")
    public TResponseVo getUserStageList(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";
        /**
         * 工作台-获取用户下所有涉及项目及项目下标段信息列表树
         */
        return workingTableServiceImpl.getUserStageListService(userId);
    }

    @PostMapping("getMyDocumentList")
    public TResponseVo getMyDocumentList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";
        /**
         * 工作台-获取我的文档
         */
        return workingTableServiceImpl.getMyDocumentListService(pagePojo, userId);
    }

    @GetMapping("newMyDocument")
    public TResponseVo newMyDocument(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";
        /**
         * 工作台-我的文档新建
         */
        String fileName = docFileIndexPojo.getFileName();
        String fileTypeId = docFileIndexPojo.getFileTypeId();

        if (StringUtils.isBlank(fileName) || StringUtils.isBlank(fileTypeId)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return workingTableServiceImpl.newMyDocumentService(docFileIndexPojo, userId);
    }

    @GetMapping("delMyDocument")
    public TResponseVo delMyDocument(String fileUuid, String fileVersionId) throws Exception {
        /**
         * 工作台-我的文档删除
         */
        return workingTableServiceImpl.delMyDocumentService(fileUuid, fileVersionId);
    }

    /**
     * 创建招标文件
     */
    /**
     * 根据标签获取相似度
     */
    @PostMapping("/getSimilarDoc")
    public TResponseVo getSimilarDoc(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        List<String> fileLabelIds = docFileIndexPojo.getFileLabelIds();
        String searchLike = docFileIndexPojo.getSearchLike();
        String fileTypeId = docFileIndexPojo.getFileTypeId();
        return fileOperationServiceImpl.getSimilarDocService(fileLabelIds, userId, searchLike, fileTypeId);
    }

    /**
     * 获取文件大纲信息
     */
    @GetMapping("/getOutlineIdList")
    public TResponseVo getOutlineIdList(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
        return workingTableServiceImpl.getOutlineIdListService(fileUuid, fileVersionId);
    }

    /**
     * 获取文件大纲信息
     */
    @GetMapping("/getLabelInfoList")
    public TResponseVo getLabelInfoList(String fileTypeId) throws Exception {
        return workingTableServiceImpl.getLabelInfoListService(fileTypeId);
    }

    /**
     * 我的项目列表
     */
    @GetMapping("/getMyProjectList")
    public TResponseVo getMyProjectList(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getMyProjectListService(userId);
    }

    /**
     * 创建招标文件
     */
    @PostMapping("/newBidDocument")
    public TResponseVo newBidDocument(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docFileIndexPojo.setCreateUserId(userId);
        return TResponseVo.success(workingTableServiceImpl.newBidDocumentService(docFileIndexPojo));
    }

    /**
     * 将招标文件投递至项目中
     */
//    @GetMapping("/sendToProject")
//    public TResponseVo sendToProject(String fileUuid) throws Exception {
//        return workingTableServiceImpl.sendToProjectService(fileUuid);
//    }

    /**
     * 获取投标文件组
     */
    @GetMapping("/getTendDocument")
    public TResponseVo getTendDocument(String fileUuid, String bidKey) throws Exception {
        return workingTableServiceImpl.getTendDocumentService(fileUuid, bidKey);
    }

    /**
     * 获取投标文件章节
     */
    @GetMapping("/getTendDocumentOutline")
    public TResponseVo getTendDocumentOutline(DocFileIndexPojo docFileIndexPojo) throws Exception {
        return workingTableServiceImpl.getTendDocumentOutlineService(docFileIndexPojo);
    }

    /**
     * 创建投标文件
     */
    @PostMapping("/newTendDocument")
    public TResponseVo newTendDocument(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        List<DocFileIndexPojo> docFileIndexPojoList = docFileIndexPojo.getDocFileIndexPojoList();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String mainFileUuid = docFileIndexPojo.getMainFileUuid();
        return workingTableServiceImpl.newTendDocumentService(docFileIndexPojoList, userId, mainFileUuid);
    }

    /**
     * 根据投标文件uuid获取招标文件uuid
     */
    @GetMapping("/getBidFromTend")
    public TResponseVo getBidFromTend(String fileUuid) throws Exception {
        return workingTableServiceImpl.getBidFromTendService(fileUuid);
    }

    /**
     * 根据文件id获取最新版本信息
     */
    @GetMapping("/getLastVersion")
    public TResponseVo getLastVersion(String fileUuid) throws Exception {
        return workingTableServiceImpl.getLastVersionService(fileUuid);
    }

    /**
     * 根据文件id获取最新版本信息
     */
    @GetMapping("/getUserList")
    public TResponseVo getUserList() throws Exception {
        return workingTableServiceImpl.getUserListService();
    }

    /**
     * 新增辅助工具表单
     */
    @PostMapping("/newSupToolForm")
    public TResponseVo newSupToolForm(@RequestBody HfSupToolFormPojo hfSupToolFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfSupToolFormPojo.setCreateUserId(userId);
        return workingTableServiceImpl.newSupToolFormService(hfSupToolFormPojo);
    }

    /**
     * 删除辅助工具表单
     */
    @GetMapping("/delSupToolForm")
    public TResponseVo delSupToolForm(String formId) throws Exception {
        return workingTableServiceImpl.delSupToolFormService(formId);
    }

    /**
     * 修改辅助工具表单
     */
    @PostMapping("/updateSupToolForm")
    public TResponseVo updateSupToolForm(@RequestBody HfSupToolFormPojo hfSupToolFormPojo) throws Exception {
        return workingTableServiceImpl.updateSupToolFormService(hfSupToolFormPojo);
    }

    /**
     * 查询辅助工具表单信息
     */
    @GetMapping("/getSupToolFormInfo")
    public TResponseVo getSupToolFormInfo(String formId) throws Exception {
        return workingTableServiceImpl.getSupToolFormInfoService(formId);
    }

    /**
     * 查询辅助工具表单列表
     */
    @GetMapping("/getSupToolFormList")
    public TResponseVo getSupToolFormList(PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getSupToolFormListService(pagePojo, userId);
    }

/**
 * 辅助工具
 */
    /**
     * 新建辅助工具模型
     */
    @PostMapping("/newSupTable")
    public TResponseVo newSupTable(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfSupTableFormPojo.setCreateUserId(userId);
        return workingTableServiceImpl.newSupTableService(hfSupTableFormPojo);
    }

    /**
     * 新增辅助工具分类标签
     */
    @PostMapping("/addSupLabel")
    public TResponseVo addSupLabel(@RequestBody LabelValuePojo labelValuePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.addSupLabelService(labelValuePojo, userId);
    }

    /**
     * 删除辅助工具分类标签
     */
    @PostMapping("/delSupLabel")
    public TResponseVo delSupLabel(@RequestBody LabelValuePojo labelValuePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String uuid = labelValuePojo.getUuid();
        return workingTableServiceImpl.delSupLabelService(uuid, userId);
    }

    /**
     * 获取辅助工具分类标签清单
     */
    @PostMapping("/getSupLabelList")
    public TResponseVo getSupLabelList(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getSupLabelListService(userId);
    }

    /**
     * 删除辅助工具模型
     */
    @PostMapping("/delSupTable")
    public TResponseVo delSupTable(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.delSupTableService(hfSupTableFormPojo);
    }

    /**
     * 更新辅助工具模型
     */
    @PostMapping("/updateSupTable")
    public TResponseVo updateSupTable(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.updateSupTableService(hfSupTableFormPojo);
    }

    /**
     * 查询辅助工具模型
     */
    @PostMapping("/getSupTableInfo")
    public TResponseVo getSupTableInfo(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getSupTableInfoService(hfSupTableFormPojo, userId);
    }

    /**
     * 查询辅助工具模型列表
     */
    @PostMapping("/getSupTableList")
    public TResponseVo getSupTableList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        List<String> label = pagePojo.getLabel();
        Boolean enable = pagePojo.getEnabled();
        return workingTableServiceImpl.getSupTableListService(pagePojo, userId, label, enable);
    }


    /**
     * 新建辅助工具数据
     */
    @PostMapping("/newSupTableForm")
    public TResponseVo newSupTableForm(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfSupTableFormPojo.setCreateUserId(userId);
        return workingTableServiceImpl.newSupTableFormService(hfSupTableFormPojo);
    }

    /**
     * 删除辅助工具数据
     */
    @PostMapping("/delSupTableForm")
    public TResponseVo delSupTableForm(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.delSupTableFormService(hfSupTableFormPojo);
    }

    /**
     * 修改辅助工具数据
     */
    @PostMapping("/updateSupTableForm")
    public TResponseVo updateSupTableForm(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.updateSupTableFormService(hfSupTableFormPojo);
    }

    /**
     * 查询辅助工具数据
     */
    @PostMapping("/getSupTableFormInfo")
    public TResponseVo getSupTableFormInfo(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getSupTableFormInfoService(hfSupTableFormPojo, userId);
    }

    /**
     * 查询辅助工具数据列表
     */
    @PostMapping("/getSupTableFormList")
    public TResponseVo getSupTableFormList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getSupTableFormListService(pagePojo, userId);
    }

    /**
     * 辅助工具excel下载
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/downloadSupTableExcel")
    public TResponseVo downloadSupTableExcel(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session, HttpServletResponse response) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String tableId = hfSupTableFormPojo.getTableId();
        List<String> formIdList = hfSupTableFormPojo.getFormIdList();
        return workingTableServiceImpl.downloadSupTableExcelService(tableId, formIdList, response);
    }

    /**
     * 辅助工具excel上传
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("uploadSupTableExcel")
    public TResponseVo uploadSupTableExcel(@RequestParam(value = "file") MultipartFile file, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.uploadSupTableExcelService(file, userId);

    }

    /**
     * 预填写
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("supAutoWrite")
    public TResponseVo supAutoWrite(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        String fileUuid = hfSupTableFormPojo.getFileUuid();
        String fileVersionId = hfSupTableFormPojo.getFileVersionId();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String formId = hfSupTableFormPojo.getFormId();
        return workingTableServiceImpl.supAutoWriteService(fileUuid, fileVersionId, userId, formId);
    }

    /**
     * 预填写-确认
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("supAutoWriteSure")
    public TResponseVo supAutoWriteSure(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        String fileUuid = hfSupTableFormPojo.getFileUuid();
        String fileVersionId = hfSupTableFormPojo.getFileVersionId();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        Map<String, Object> map = hfSupTableFormPojo.getRetList();
        return workingTableServiceImpl.supAutoWriteSureService(fileUuid, fileVersionId, userId, map);
    }

    /**
     * 预填写-历史记录
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("getSupTableHistoryList")
    public TResponseVo getSupTableHistoryList(@RequestBody HfSupTableFormPojo hfSupTableFormPojo, HttpSession session) throws Exception {
        String fileUuid = hfSupTableFormPojo.getFileUuid();
        String fileVersionId = hfSupTableFormPojo.getFileVersionId();
        return workingTableServiceImpl.getSupTableHistoryListService(fileUuid, fileVersionId);
    }

    /**
     * 预填写-历史记录
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("getSupTableRecent")
    public TResponseVo getSupTableRecent(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getSupTableRecentService(userId);
    }

    /**
     * 辅助工具excel上传
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("getSupTableTree")
    public TResponseVo getSupTableTree(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getSupTableTreeService(userId);

    }


    /**
     * 获取收件箱文件清单
     */
    @GetMapping("/getInboxFileList")
    public TResponseVo getInboxFileList(String projectId, String batchId, String statusId) throws Exception {
        return workingTableServiceImpl.getInboxFileListService(projectId, batchId, statusId);
    }

    /**
     * 修改收件箱文件状态
     */
    @GetMapping("/updateInboxFile")
    public TResponseVo updateInboxFile(String fileUuid, String statusId, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.updateInboxFileService(fileUuid, statusId, userId);
    }

    /**
     * 获取文件类型清单
     */
    @GetMapping("/getFileTypeDi")
    public TResponseVo getFileTypeDi() throws Exception {
        return workingTableServiceImpl.getFileTypeDiService();
    }

    /**
     * 新建模板与文件
     */
    @PostMapping("/newDocument")
    public TResponseVo newDocument(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docFileIndexPojo.setCreateUserId(userId);
        return workingTableServiceImpl.newDocumentService(docFileIndexPojo, session);
    }

    /**
     * 获取模板列表
     */
    @PostMapping("/getModelList")
    public TResponseVo getModelList(@RequestBody(required = false) ModelPojo modelPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String type = null;
        if (modelPojo == null) {
            type = null;
        } else {
            type = modelPojo.getType();
        }
        return workingTableServiceImpl.getModelListService(userId, type);
    }


//    @Autowired
//    private Neo4jServiceImpl neo4jServiceImpl;

//    /**
//     * 获取电影列表
//     * @return
//     * @throws Exception
//     */
//    @PostMapping("/getMovieList")
//    public TResponseVo getMovieList() throws Exception {
//        return neo4jServiceImpl.getMovieListService();
//    }
//
//
//
//    /**
//     * 获取文档列表
//     * @return
//     * @throws Exception
//     */
//    @PostMapping("/getFileIndexListNeo")
//    public TResponseVo getFileIndexListNeo() throws Exception {
//        return neo4jServiceImpl.getFileIndexListNeoService();
//    }
//
//    /**
//     * 新建文档信息
//     * @return
//     * @throws Exception
//     */
//    @PostMapping("/newFileIndexNeo")
//    public TResponseVo newFileIndexNeo(@RequestBody FileIndexEntity fileIndexEntity) throws Exception {
//        return neo4jServiceImpl.newFileIndexNeoService(fileIndexEntity);
//    }
//
//    /**
//     * 新增生成信息
//     * @return
//     * @throws Exception
//     */
//    @PostMapping("/addFileIndexToFileIndex")
//    public TResponseVo addFileIndexToFileIndex(@RequestBody FileIndexEntity fileIndexEntity) throws Exception {
//        String fileUuid1 = fileIndexEntity.getFileUuid();
//        String fileUuid2 = fileIndexEntity.getFileUuid2();
//        return neo4jServiceImpl.addFileIndexToFileIndexService(fileUuid1,fileUuid2);
//    }
//
//
//    /**
//     * 删除文档节点
//     * @return
//     * @throws Exception
//     */
//    @PostMapping("/delFileIndexNeo")
//    public TResponseVo delFileIndexNeo(@RequestBody ModelGroup modelGroup) throws Exception {
//        String fileUuid = modelGroup.getFileUuid();
//        return neo4jServiceImpl.delFileIndexNeoService(fileUuid);
//    }


    /**
     * 新建模板组
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/newModelGroup")
    public TResponseVo newModelGroup(@RequestBody ModelGroupPojo modelGroupPojo) throws Exception {
        return workingTableServiceImpl.newModelGroupService(modelGroupPojo);
    }

    /**
     * 删除模板组
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/delModelGroup")
    public TResponseVo delModelGroup(@RequestBody ModelGroupPojo modelGroupPojo) throws Exception {
        return workingTableServiceImpl.delModelGroupService(modelGroupPojo);
    }

    /**
     * 编辑模板组
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/updateModelGroup")
    public TResponseVo updateModelGroup(@RequestBody ModelGroupPojo modelGroupPojo) throws Exception {
        return workingTableServiceImpl.updateModelGroupService(modelGroupPojo);
    }

    /**
     * 查询某一模板组信息
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/getModelGroupInfo")
    public TResponseVo getModelGroupInfo(@RequestBody ModelGroupPojo modelGroupPojo) throws Exception {
        return workingTableServiceImpl.getModelGroupInfoService(modelGroupPojo);
    }

    /**
     * 查询模板组列表
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/getModelGroupList")
    public TResponseVo getModelGroupList() throws Exception {
        return workingTableServiceImpl.getModelGroupListService();
    }

    /**
     * 查询模板组列表KV
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/getModelGroupListKV")
    public TResponseVo getModelGroupListKV() throws Exception {
        return workingTableServiceImpl.getModelGroupListKVService();
    }

    /**
     * 获取平台视图
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/getGlobalView")
    public TResponseVo getGlobalView(@RequestBody(required = false) ModelGroupPojo modelGroupPojo) throws Exception {
        return workingTableServiceImpl.getGlobalViewService(modelGroupPojo);
    }


    /**
     * 获取平台视图
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/getModelGroupLabelList")
    public TResponseVo getModelGroupLabelList(@RequestBody(required = false) ModelGroupPojo modelGroupPojo) throws Exception {
        return workingTableServiceImpl.getModelGroupLabelListService(modelGroupPojo);
    }


    /**
     * 获取文库常用列表
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/getCommonUseList")
    public TResponseVo getCommonUseList(@RequestBody(required = false) PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getCommonUseListService(pagePojo);
    }


    /**
     * 获取文库母版列表
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/getLibraryList")
    public TResponseVo getLibraryList(@RequestBody(required = false) PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getLibraryListService(pagePojo);
    }


    /**
     * 获取文库搜索结果
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/searchLibrary")
    public TResponseVo searchLibrary(@RequestBody(required = false) PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.searchLibraryService(pagePojo);
    }

    /**
     * 文库-文件血缘关系
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/getLibraryView")
    public TResponseVo getLibraryView(@RequestBody(required = false) PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getLibraryViewService(pagePojo);
    }

    /**
     * 文库-热门搜索
     *
     * @return
     * @throws Exception
     */
    @PostMapping("/getLibraryHotSearch")
    public TResponseVo getLibraryHotSearch() throws Exception {
        return workingTableServiceImpl.getLibraryHotSearchService();
    }


    /**
     * 首页
     */
    /**
     * 首页-模板类型分布
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getModelRate")
    public TResponseVo getModelRate() throws Exception {
        return workingTableServiceImpl.getModelRateService();
    }

    /**
     * 首页-模板类型分布
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getFileRate")
    public TResponseVo getFileRate() throws Exception {
        return workingTableServiceImpl.getFileRateService();
    }

    /**
     * 首页-我的文档统计
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getMyFileStatistics")
    public TResponseVo getMyFileStatistics(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getMyFileStatisticsService(userId);
    }


    /**
     * 首页-热力图
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getHeatMap")
    public TResponseVo getHeatMap() throws Exception {
        return workingTableServiceImpl.getHeatMapService();
    }


    /**
     * 母版管理
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getMasterModel")
    public TResponseVo getMasterModel(@RequestBody PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getMasterModelService(pagePojo);
    }


    @PostMapping("/getMasterModelLV")
    public TResponseVo getMasterModelLV() throws Exception {
        return workingTableServiceImpl.getMasterModelLVService();
    }

    /**
     * 获取最近使用文档目录
     *
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getRecentFileList")
    public TResponseVo getRecentFileList(String limit, String fileTypeGroupId, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getRecentFileListService(userId, limit, fileTypeGroupId);
    }

/**
 * 文库管理-文档审核
 */
    /**
     * 获取文库文档审核列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getAuditingFileList")
    public TResponseVo getAuditingFileList(@RequestBody PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getAuditingFileListService(pagePojo);
    }

    /**
     * 文库文档审核操作
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/changeAuditingFileStatus")
    public TResponseVo changeAuditingFileStatus(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        String auditingStatus = docFileIndexPojo.getAuditingStatus();
        String auditingReason = docFileIndexPojo.getAuditingReason();

        return workingTableServiceImpl.changeAuditingFileStatusService(fileUuid, fileVersionId, auditingStatus, auditingReason, userId);
    }

    /**
     * 获取文库文档管理列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getLibraryFileManagementList")
    public TResponseVo getLibraryFileManagementList(@RequestBody PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getLibraryFileManagementListService(pagePojo);
    }

    /**
     * 获取文库文档下拉版本记录
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getLibraryFileVersionList")
    public TResponseVo getLibraryFileVersionList(@RequestBody DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        return workingTableServiceImpl.getLibraryFileVersionListService(fileUuid);
    }

    /**
     * 文库文档上下线操作
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/changeLibraryFileStatus")
    public TResponseVo changeLibraryFileStatus(@RequestBody DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        String fileStatus = docFileIndexPojo.getFileStatus();
        return workingTableServiceImpl.changeLibraryFileStatusService(fileUuid, fileVersionId, fileStatus);
    }

    /**
     * 文库文档删除
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/deleteLibraryFile")
    public TResponseVo deleteLibraryFile(@RequestBody DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        return workingTableServiceImpl.deleteLibraryFileService(fileUuid);
    }

    /**
     * 文库文档推荐
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/changeLibraryFileRecommend")
    public TResponseVo changeLibraryFileRecommend(@RequestBody DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        String isRecommend = docFileIndexPojo.getIsRecommend();
        return workingTableServiceImpl.changeLibraryFileRecommendService(fileUuid, isRecommend);
    }


    /**
     * 用户管理
     */
    /**
     * 新增用户
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/addUser")
    public TResponseVo addUser(@RequestBody DocUserPojo docUserPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String companyId = user.getCompanyId();
        docUserPojo.setCompanyId(companyId);
        return workingTableServiceImpl.addUserService(docUserPojo);
    }

    /**
     * 删除用户
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/delUser")
    public TResponseVo delUser(@RequestBody DocUserPojo docUserPojo) throws Exception {
        return workingTableServiceImpl.delUserService(docUserPojo);
    }

    /**
     * 修改用户
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/updateUser")
    public TResponseVo updateUser(@RequestBody DocUserPojo docUserPojo) throws Exception {
        return workingTableServiceImpl.updateUserService(docUserPojo);
    }

    /**
     * 获取用户列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getUserList")
    public TResponseVo getUserList(@RequestBody PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getUserListService(pagePojo);
    }

    /**
     * 获取用户信息
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getUserInfo")
    public TResponseVo getUserInfo(@RequestBody DocUserPojo docUserPojo) throws Exception {
        return workingTableServiceImpl.getUserInfoService(docUserPojo);
    }

    /**
     * 重置用户密码
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/resetPassword")
    public TResponseVo resetPassword(@RequestBody DocUserPojo docUserPojo) throws Exception {
        return workingTableServiceImpl.resetPasswordService(docUserPojo);
    }


    /**
     * 新增角色
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/addRoles")
    public TResponseVo addRoles(@RequestBody DocUserRolesPojo docUserRolesPojo) throws Exception {
        return workingTableServiceImpl.addRolesService(docUserRolesPojo);
    }


    /**
     * 获取角色列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/delRoles")
    public TResponseVo delRoles(@RequestBody DocUserRolesPojo docUserRolesPojo) throws Exception {
        return workingTableServiceImpl.delRolesService(docUserRolesPojo);
    }


    /**
     * 获取权限菜单
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getMenuList")
    public TResponseVo getMenuList() throws Exception {
        return workingTableServiceImpl.getMenuListService();
    }


    /**
     * 获取角色列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/chgRoles")
    public TResponseVo chgRoles(@RequestBody DocUserRolesPojo docUserRolesPojo) throws Exception {
        return workingTableServiceImpl.chgRolesService(docUserRolesPojo);
    }


    /**
     * 获取角色列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getRolesInfo")
    public TResponseVo getRolesInfo(@RequestBody DocUserRolesPojo docUserRolesPojo) throws Exception {
        return workingTableServiceImpl.getRolesInfoService(docUserRolesPojo);
    }


    /**
     * 获取角色列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getRolesList")
    public TResponseVo getRolesList(@RequestBody PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getRolesListService(pagePojo);
    }


    /**
     * 获取角色列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getRoles")
    public TResponseVo getRoles() throws Exception {
        return workingTableServiceImpl.getRolesService();
    }


    /**
     * 获取审核通知
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getResultNotice")
    public TResponseVo getResultNotice(@RequestBody(required = false) PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        Boolean isRead = false;
        String noticeType = pagePojo.getNoticeType();
        return workingTableServiceImpl.getResultNoticeService(pagePojo, userId, isRead, noticeType);
    }

    /**
     * 已读
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/readResultNotice")
    public TResponseVo readResultNotice(@RequestBody HfAuditingResultNoticePojo hfAuditingResultNoticePojo) throws Exception {
        String auditingUuid = hfAuditingResultNoticePojo.getAuditingUuid();
        return workingTableServiceImpl.readResultNoticeService(auditingUuid);
    }


    /**
     * 我的贡献
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getMyContribution")
    public TResponseVo getMyContribution(@RequestBody(required = false) PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getMyContributionService(pagePojo, userId);
    }


    /**
     * 下载
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getDocx")
    public TResponseVo getDocx(@RequestBody HfMyDownload hfMyDownload, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String fileUuid = hfMyDownload.getFileUuid();
        String fileVersionId = hfMyDownload.getFileVersionId();
        String fileName = hfMyDownload.getFileName();
        String html = hfMyDownload.getHtml();
        String downloadType = hfMyDownload.getDownloadType();
        return workingTableServiceImpl.getDocxService(fileName, fileUuid, fileVersionId, userId, html, downloadType);
    }


    /**
     * 查看我的下载列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getMyDownloadList")
    public TResponseVo getMyDownloadList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getMyDownloadListService(pagePojo, userId);
    }


    /**
     * 删除我的下载
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/delMyDownload")
    public TResponseVo delMyDownload(@RequestBody HfMyDownload hfMyDownload, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String uuid = hfMyDownload.getUuid();
        return workingTableServiceImpl.delMyDownloadService(uuid);
    }


    /**
     * 下载我的下载
     *
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/downloadMyDownload")
    public TResponseVo downloadMyDownload(HfMyDownload hfMyDownload, HttpSession session, HttpServletResponse response) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String uuid = hfMyDownload.getUuid();
        return workingTableServiceImpl.downloadMyDownloadService(uuid, userId, response);
    }

    /**
     * 获取下载日志清单
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getDownloadRecordList")
    public TResponseVo getDownloadRecordList(@RequestBody(required = false) PagePojo pagePojo) throws Exception {
        if (pagePojo == null) {
            pagePojo = new PagePojo();
        }
        return workingTableServiceImpl.getDownloadRecordListService(pagePojo);
    }


    /**
     * 下载我的下载
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/newAssessModel")
    public TResponseVo newAssessModel(@RequestBody HfAssessModelPojo hfAssessModelPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfAssessModelPojo.setCreateUserId(userId);
        return workingTableServiceImpl.newAssessModelService(hfAssessModelPojo);
    }

    /**
     * 下载我的下载
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/delAssessModel")
    public TResponseVo delAssessModel(@RequestBody HfAssessModelPojo hfAssessModelPojo) throws Exception {
        String assessId = hfAssessModelPojo.getAssessId();
        return workingTableServiceImpl.delAssessModelService(assessId);
    }

    /**
     * 下载我的下载
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/updateAssessModel")
    public TResponseVo updateAssessModel(@RequestBody HfAssessModelPojo hfAssessModelPojo, HttpSession session) throws Exception {
        return workingTableServiceImpl.updateAssessModelService(hfAssessModelPojo);
    }

    /**
     * 下载我的下载
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getAssessModelInfo")
    public TResponseVo getAssessModelInfo(@RequestBody HfAssessModelPojo hfAssessModelPojo) throws Exception {
        String assessId = hfAssessModelPojo.getAssessId();
        return workingTableServiceImpl.getAssessModelInfoService(assessId);
    }

    /**
     * 下载我的下载
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getAssessModelList")
    public TResponseVo getAssessModelList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getAssessModelListService(pagePojo, userId);
    }

    /**
     * 新增评审要素
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/newAssessModelElement")
    public TResponseVo newAssessModelElement(@RequestBody HfAssessModelElementPojo hfAssessModelElementPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfAssessModelElementPojo.setCreateUserId(userId);
        return workingTableServiceImpl.newAssessModelElementService(hfAssessModelElementPojo);
    }

    /**
     * 新增评审要素
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/delAssessModelElement")
    public TResponseVo delAssessModelElement(@RequestBody HfAssessModelElementPojo hfAssessModelElementPojo, HttpSession session) throws Exception {
        String assessId = hfAssessModelElementPojo.getAssessId();
        String elementType = hfAssessModelElementPojo.getElementType();
        return workingTableServiceImpl.delAssessModelElementService(assessId, elementType);
    }

    /**
     * 新增评审要素
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/updateAssessModelElement")
    public TResponseVo updateAssessModelElement(@RequestBody HfAssessModelElementPojo hfAssessModelElementPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfAssessModelElementPojo.setCreateUserId(userId);
        return workingTableServiceImpl.updateAssessModelElementService(hfAssessModelElementPojo);
    }

    /**
     * 新增评审要素
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getAssessModelElementInfo")
    public TResponseVo getAssessModelElementInfo(@RequestBody HfAssessModelElementPojo hfAssessModelElementPojo, HttpSession session) throws Exception {
        String assessId = hfAssessModelElementPojo.getAssessId();
        String elementType = hfAssessModelElementPojo.getElementType();
        return workingTableServiceImpl.getAssessModelElementInfoService(assessId, elementType);
    }

    /**
     * 新增评审要素
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getAssessModelElementList")
    public TResponseVo getAssessModelElementList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getAssessModelElementListService(pagePojo, userId);
    }

    /**
     * 调整评审条款顺序
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/updateAssessModelOrder")
    public TResponseVo updateAssessModelOrder(@RequestBody HfAssessModelElementPojo hfAssessModelElementPojo) throws Exception {
        return workingTableServiceImpl.updateAssessModelOrderService(hfAssessModelElementPojo);
    }

    /**
     * 参数下拉列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getAssessModelParam")
    public TResponseVo getAssessModelParam(@RequestBody HfAssessModelElementPojo hfAssessModelElementPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getAssessModelParamService(hfAssessModelElementPojo, userId);
    }

    /**
     * 招采库-主图
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getZhaocaiGlobalView")
    public TResponseVo getZhaocaiGlobalView(@RequestBody DocFileIndexPojo docFileIndexPojo) throws Exception {
        return workingTableServiceImpl.getZhaocaiGlobalViewService(docFileIndexPojo);
    }

    /**
     * 招采库-主图
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getZhaocaiGlobalViewLabel")
    public TResponseVo getZhaocaiGlobalViewLabel() throws Exception {
        return workingTableServiceImpl.getZhaocaiGlobalViewLabelService();
    }

    /**
     * 帮助文档-目录
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getHelpDocCatalogue")
    public TResponseVo getHelpDocCatalogue() throws Exception {
        return workingTableServiceImpl.getHelpDocCatalogueService();
    }

    /**
     * 帮助文档-目录
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getHelpDocContent")
    public TResponseVo getHelpDocContent(@RequestBody HfHelpDocCatalogue hfHelpDocCatalogue) throws Exception {
        String helpDocId = hfHelpDocCatalogue.getHelpDocId();
        return workingTableServiceImpl.getHelpDocContentService(helpDocId);
    }

    /**
     * 帮助文档-搜索
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/searchHelpDoc")
    public TResponseVo searchHelpDoc(@RequestBody PagePojo pagePojo) throws Exception {
        String searchContent = pagePojo.getSearchContent();
        return workingTableServiceImpl.searchHelpDocService(pagePojo, searchContent);
    }

    /**
     * 帮助文档-热词
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getHotSearchKey")
    public TResponseVo getHotSearchKey() throws Exception {
        return workingTableServiceImpl.getHotSearchKeyService();
    }

    /**
     * 我的空间-新建文档-最近创建类型
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getRecentBuild")
    public TResponseVo getRecentBuild(@RequestBody HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getRecentBuildService(userId);
    }

    /**
     * 我的空间-新建文档-最近创建
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/newJudge")
    public TResponseVo newJudge(@RequestBody HfJudgePojo hfJudgePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgePojo.setCreateUserId(userId);
        return workingTableServiceImpl.newJudgeService(hfJudgePojo);
    }

    /**
     * 我的空间-新建文档-最近创建
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getFileAndAssess")
    public TResponseVo getFileAndAssess(@RequestBody HfJudgePojo hfJudgePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String projectStageId = hfJudgePojo.getProjectStageId();
        return workingTableServiceImpl.getFileAndAssessService(projectStageId);
    }

    /**
     * 我的空间-新建文档-最近创建
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/delJudge")
    public TResponseVo delJudge(@RequestBody HfJudgePojo hfJudgePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String judgeId = hfJudgePojo.getJudgeId();
        return workingTableServiceImpl.delJudgeService(judgeId);
    }

    /**
     * 我的空间-新建文档-最近创建
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/updateJudge")
    public TResponseVo updateJudge(@RequestBody HfJudgePojo hfJudgePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.updateJudgeService(hfJudgePojo);
    }

    /**
     * 我的空间-新建文档-最近创建
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getJudgeInfo")
    public TResponseVo getJudgeInfo(@RequestBody HfJudgePojo hfJudgePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String judgeId = hfJudgePojo.getJudgeId();
        return workingTableServiceImpl.getJudgeInfoService(judgeId);
    }

    /**
     * 我的空间-新建文档-最近创建
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getJudgeList")
    public TResponseVo getJudgeList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getJudgeListService(pagePojo);
    }

    /**
     * 我的空间-新建文档-最近创建
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getJudgeDetail")
    public TResponseVo getJudgeDetail(@RequestBody HfJudgePojo hfJudgePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getJudgeDetailService(hfJudgePojo);
    }

    /**
     * 创建文件-企业常用模板
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getCompanyUseFileList")
    public TResponseVo getCompanyUseFileList(@RequestParam(value = "limit") Integer limit, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.getCompanyUseFileListService(limit);
    }

    /**
     * 评审步骤模板配置-新增评审步骤模板
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/createFastAssess")
    public TResponseVo createFastAssess(@RequestBody HfFastAssessPojo hfFastAssessPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfFastAssessPojo.setCreateUserId(userId);
        return workingTableServiceImpl.createFastAssessService(hfFastAssessPojo);
    }

    /**
     * 评审步骤模板配置-删除评审步骤模板
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/delFastAssess")
    public TResponseVo delFastAssess(@RequestBody HfFastAssessPojo hfFastAssessPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfFastAssessPojo.setCreateUserId(userId);
        return workingTableServiceImpl.delFastAssessService(hfFastAssessPojo);
    }

    /**
     * 评审步骤模板配置-更新评审步骤模板
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/updateFastAssess")
    public TResponseVo updateFastAssess(@RequestBody HfFastAssessPojo hfFastAssessPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfFastAssessPojo.setCreateUserId(userId);
        return workingTableServiceImpl.updateFastAssessService(hfFastAssessPojo);
    }

    /**
     * 评审步骤模板配置-评审要素总体方案列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getFastAssessInfo")
    public TResponseVo getFastAssessInfo(@RequestBody HfFastAssessPojo hfFastAssessPojo) throws Exception {
        String fastId = hfFastAssessPojo.getFastId();
        if (StringUtils.isBlank(fastId)) {
            return TResponseVo.error("必填参数为空");
        }
        return workingTableServiceImpl.getFastAssessInfoService(fastId);
    }

    /**
     * 评审步骤模板配置-评审要素总体方案列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getFastAssessList")
    public TResponseVo getFastAssessList(@RequestBody(required = false) PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getFastAssessListService(pagePojo);
    }

    /**
     * 评审步骤模板配置-评审要素总体方案列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getFastAssessListLV")
    public TResponseVo getFastAssessListLV(@RequestBody(required = false) PagePojo pagePojo) throws Exception {
        if (pagePojo == null) {
            pagePojo = new PagePojo();
        }
        return workingTableServiceImpl.getFastAssessListLVService(pagePojo);
    }

    /**
     * 评审步骤模板配置-读取预览
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("/getFastAssessPreview")
    public TResponseVo getFastAssessPreview(@RequestBody(required = false) HfFastAssessPojo hfFastAssessPojo) throws Exception {
        String fastId = hfFastAssessPojo.getFastId();
        return workingTableServiceImpl.getFastAssessPreviewService(fastId);
    }

    @GetMapping("/syncFile")
    public void syncFile(DocFileIndexPojo docFileIndexPojo, HttpSession session, HttpServletResponse response) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docFileIndexPojo.setCreateUserId(userId);
        workingTableServiceImpl.syncFileService(docFileIndexPojo, response);
    }

    @PostMapping("/upLoadSyncData")
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo upLoadSyncData(@RequestParam(value = "file") MultipartFile file,
//                                  @RequestParam(value = "fileTypeId", required = false) String fileTypeId,
                                      @RequestParam(value = "fileLabelIds", required = false) List<String> fileLabelIds,
                                      @RequestParam(value = "includeUserList", required = false) String includeUserList,
                                      @RequestParam(value = "fileName", required = false) String fileName,
                                      @RequestParam(value = "fileDesc", required = false) String fileDesc,
                                      HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();

        String fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        if (file.isEmpty()) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传失败");
        }
//        String oriFileName = file.getOriginalFilename();
//        String extension = oriFileName.substring(oriFileName.lastIndexOf("."));
//        if (!extension.equals(".docx")) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传错误,目前只支持docx的文档");
//        }

//        // 是否重新命名
//        if (StringUtils.isBlank(fileName)) {
//            // 使用文档原名
//            fileName = file.getOriginalFilename();
//        } else {
//            // 重新命名  (如果新名字长度小于等于5 或者后缀不是.docx 就给他加一个)
//            if(fileName.length()<6 || !fileName.substring(fileName.length()-5).equals(".docx") || !fileName.substring(fileName.length()-4).equals(".doc")){
//                fileName = fileName + extension;
//            }
//        }
        File filePathNew = new File(this.uploadJsonPath + "/" + fileUuid);
        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        File docxFileNew = new File(this.uploadJsonPath + "/" + fileUuid + "/" + fileUuid);
        try {
            file.transferTo(docxFileNew);
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
        }
        System.out.println("开始解析");
//        //TODO 提前清空目标文件夹
//        try {
//            new Doc2html().wordToHtml(this.uploadJsonPath + "/" + fileUuid + "/" + fileUuid + extension,
//                    htmlPath+ "/",
//                    fileUuid + ".html");
//            LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传完成");
//        } catch (IOException e) {
//            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, file.getOriginalFilename() + "上传失败");
//        }
        FileInputStream fis = new FileInputStream(docxFileNew);
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = fis.read(buffer)) != -1) {
            outSteam.write(buffer, 0, len);
        }
        outSteam.close();
        fis.close();
        byte[] bytes = outSteam.toByteArray();
        String json = new String(bytes);

        System.out.println(json);

        Map<String, Object> map = JSON.parseObject(json, Map.class);

        Map<String, String> ret = workingTableServiceImpl.upLoadSyncDataService(map, fileName, fileDesc, fileLabelIds, userId);
        ret.put("info", "上传成功");
        return TResponseVo.success(ret);
    }


    /**
     * 获取菜单类型清单
     *
     * @throws Exception
     */
    @PostMapping("/getOptionsTypeList")
    public TResponseVo getOptionsTypeList() throws Exception {
        return workingTableServiceImpl.getOptionsTypeListService();
    }

    /**
     * 获取菜单选项清单
     *
     * @throws Exception
     */
    @PostMapping("/getOptionsList")
    public TResponseVo getOptionsList(@RequestBody(required = false) PagePojo pagePojo) throws Exception {
        return workingTableServiceImpl.getOptionsListService(pagePojo);
    }

    /**
     * 新增菜单项
     *
     * @throws Exception
     */
    @PostMapping("/addOptions")
    public TResponseVo addOptions(@RequestBody OptionsPojo optionsPojo) throws Exception {
        return workingTableServiceImpl.addOptionsService(optionsPojo);
    }

    /**
     * 删除菜单项
     *
     * @throws Exception
     */
    @PostMapping("/delOptions")
    public TResponseVo delOptions(@RequestBody OptionsPojo optionsPojo) throws Exception {
        String id = optionsPojo.getId();
        return workingTableServiceImpl.delOptionsService(id);
    }

/**
 * 我的数据-数据管理工具
 */
    /**
     * 新增数据管理工具
     *
     * @throws Exception
     */
    @PostMapping("/createDmDb")
    public TResponseVo createDmDb(@RequestBody HfDmDb hfDmDb, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmDb.setCreateUserId(userId);
        String fileUuid = hfDmDb.getFileUuid();
        String fileVersionId = hfDmDb.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.createDmDbInFileService(hfDmDb);
        } else {
            return workingTableServiceImpl.createDmDbService(hfDmDb);
        }
    }

    /**
     * 删除数据管理工具
     *
     * @throws Exception
     */
    @PostMapping("/delDmDb")
    public TResponseVo delDmDb(@RequestBody HfDmDb hfDmDb) throws Exception {
        String key = hfDmDb.getKey();
        String fileUuid = hfDmDb.getFileUuid();
        String fileVersionId = hfDmDb.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.delDmDbInFileService(key, fileUuid, fileVersionId);
        } else {
            return workingTableServiceImpl.delDmDbService(key);
        }
    }

    /**
     * 修改数据管理工具
     *
     * @throws Exception
     */
    @PostMapping("/chgDmDb")
    public TResponseVo chgDmDb(@RequestBody HfDmDb hfDmDb, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmDb.setCreateUserId(userId);
        String fileUuid = hfDmDb.getFileUuid();
        String fileVersionId = hfDmDb.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.chgDmDbInFileService(hfDmDb);
        } else {
            return workingTableServiceImpl.chgDmDbService(hfDmDb);
        }
    }

    /**
     * 获取数据管理工具清单
     *
     * @throws Exception
     */
    @PostMapping("/getDmDbList")
    public TResponseVo getDmDbList(@RequestBody(required = false) PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        if (pagePojo == null) {
            pagePojo = new PagePojo();
            pagePojo.setIsPaged("0");  // 默认不分页
        }
        pagePojo.setUserId(userId);
        String fileUuid = pagePojo.getFileUuid();
        String fileVersionId = pagePojo.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.getDmDbListInFileService(pagePojo);
        } else {
            return workingTableServiceImpl.getDmDbListService(pagePojo);
        }
    }

    /**
     * 调整数据管理表顺序
     * @param hfDmTable
     * @return
     * @throws Exception
     */
    @PostMapping("/orderDmTable")
    public TResponseVo orderDmTable(@RequestBody(required = false) HfDmTable hfDmTable) throws Exception {
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.orderDmTableInFileService(hfDmTable.getDbId(),hfDmTable.getTableIdList(),fileUuid,fileVersionId);
        } else {
            return workingTableServiceImpl.orderDmTableService(hfDmTable.getDbId(),hfDmTable.getTableIdList());
        }
    }



    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/getDmDbInfo")
    public TResponseVo getDmDbInfo(@RequestBody HfDmDb hfDmDb) throws Exception {
        String key = hfDmDb.getKey();
        String fileUuid = hfDmDb.getFileUuid();
        String fileVersionId = hfDmDb.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.getDmDbInfoInFileService(key, fileUuid, fileVersionId);
        } else {
            return workingTableServiceImpl.getDmDbInfoService(key);
        }

    }

    /**
     * 数据表
     */
    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/createDmTable")
    public TResponseVo createDmTable(@RequestBody HfDmTable hfDmTable, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmTable.setCreateUserId(userId);
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.createDmTableInFileService(hfDmTable);
        } else {
            return workingTableServiceImpl.createDmTableService(hfDmTable);
        }
    }

    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/delDmTable")
    public TResponseVo delDmTable(@RequestBody HfDmTable hfDmTable) throws Exception {
        String key = hfDmTable.getKey();
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.delDmTableInFileService(key, fileUuid, fileVersionId);
        } else {
            return workingTableServiceImpl.delDmTableService(key);
        }
    }

    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/chgDmTable")
    public TResponseVo chgDmTable(@RequestBody HfDmTable hfDmTable) throws Exception {
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.chgDmTableInFileService(hfDmTable);
        } else {
            return workingTableServiceImpl.chgDmTableService(hfDmTable);
        }
    }

    /**
     * 新增表头
     *
     * @throws Exception
     */
    @PostMapping("/addDmTableColumns")
    public TResponseVo addDmTableColumns(@RequestBody HfDmColumns hfDmColumns, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmColumns.setCreateUserId(userId);
        String fileUuid = hfDmColumns.getFileUuid();
        String fileVersionId = hfDmColumns.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.addDmTableColumnsInFileService(hfDmColumns);
        } else {
            return workingTableServiceImpl.addDmTableColumnsService(hfDmColumns);
        }
    }

    /**
     * 删除表头
     *
     * @throws Exception
     */
    @PostMapping("/delDmTableColumns")
    public TResponseVo delDmTableColumns(@RequestBody Map<String, Object> map) throws Exception {
        Object fileUuid = map.get("fileUuid");
        Object fileVersionId = map.get("fileVersionId");
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.delDmTableColumnsInFileService(map, String.valueOf(fileUuid), String.valueOf(fileVersionId));
        } else {
            return workingTableServiceImpl.delDmTableColumnsService(map);
        }

    }

    /**
     * 修改表头
     *
     * @throws Exception
     */
    @PostMapping("/chgDmTableColumns")
    public TResponseVo chgDmTableColumns(@RequestBody HfDmColumns hfDmColumns, HttpSession session) throws Exception {
        String fileUuid = hfDmColumns.getFileUuid();
        String fileVersionId = hfDmColumns.getFileVersionId();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmColumns.setCreateUserId(userId);
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.chgDmTableColumnsInFileService(hfDmColumns);
        } else {
            return workingTableServiceImpl.chgDmTableColumnsService(hfDmColumns);
        }

    }

    /**
     * 获取表头
     *
     * @throws Exception
     */
    @PostMapping("/getDmTableColumnsList")
    public TResponseVo getDmTableColumnsList(@RequestBody HfDmColumns hfDmColumns) throws Exception {
        String fileUuid = hfDmColumns.getFileUuid();
        String fileVersionId = hfDmColumns.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.getDmTableColumnsListInFileService(hfDmColumns);
        } else {
            return workingTableServiceImpl.getDmTableColumnsListService(hfDmColumns);
        }
    }


    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/getDmTableContent")
    public TResponseVo getDmTableContent(@RequestBody HfDmTable hfDmTable, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmTable.setCreateUserId(userId);
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.getDmTableContentInFileService(hfDmTable);
        } else {
            return workingTableServiceImpl.getDmTableContentService(hfDmTable);
        }
    }


    /**
     * 修改视图条件
     *
     * @throws Exception
     */
    @PostMapping("/updateViewCondition")
    public TResponseVo updateViewCondition(@RequestBody HashMap<String, Object> map, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String key = (String) map.get("key");
        String fileUuid = (String) map.get("fileUuid");
        String fileVersionId = (String) map.get("fileVersionId");
        Object condition = map.get("condition");
        List<Map<String, Object>> filter = null;
        Map<String, Object> group = null;
        if (condition != null) {
            filter = (List) ((Map) condition).get("filter");  // 过滤条件
            group = (Map) ((Map) condition).get("group");  // 聚合条件
        }
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.updateViewConditionInFileService(key, filter, group, fileUuid, fileVersionId);
        } else {
            return workingTableServiceImpl.updateViewConditionService(key, filter, group);
        }
    }

    /**
     * 视图数据重跑
     *
     * @throws Exception
     */
    @PostMapping("/rerunViewData")
    public TResponseVo rerunViewData(@RequestBody HfDmTable hfDmTable, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String key = hfDmTable.getKey();
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.rerunViewDataInFileService(key, userId, fileUuid, fileVersionId);
        } else {
            return workingTableServiceImpl.rerunViewDataService(key, userId);
        }
    }

    /**
     * 复制表
     *
     * @throws Exception
     */
    @PostMapping("/copyTable")
    public TResponseVo copyTable(@RequestBody HfDmTable hfDmTable, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String key = hfDmTable.getKey();
        String tableName = hfDmTable.getTableName();
        List<String> targetDb = hfDmTable.getTargetDb();
        Boolean includeData = hfDmTable.getIncludeData();
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.copyTableInFileService(key, tableName, targetDb, userId, includeData, fileUuid, fileVersionId);
        } else {
            return workingTableServiceImpl.copyTableService(key, tableName, targetDb, userId, includeData);
        }
    }


    /**
     * 保存结果数据为新表
     *
     * @throws Exception
     */
    @PostMapping("/saveDmData")
    public TResponseVo saveDmData(@RequestBody HfDmTable hfDmTable, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmTable.setCreateUserId(userId);
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.saveDmDataInFileService(hfDmTable, "table");
        } else {
            return workingTableServiceImpl.saveDmDataService(hfDmTable, "table");
        }
    }

    /**
     * 保存结果数据为视图
     *
     * @throws Exception
     */
    @PostMapping("/saveDmView")
    public TResponseVo saveDmView(@RequestBody HfDmTable hfDmTable, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmTable.setCreateUserId(userId);
        hfDmTable.setTableName(hfDmTable.getViewName());
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.saveDmDataInFileService(hfDmTable, "view");
        } else {
            return workingTableServiceImpl.saveDmDataService(hfDmTable, "view");
        }
    }

    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/addDmData")
    public TResponseVo addDmData(@RequestBody HfDmData hfDmData, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmData.setCreateUserId(userId);
        String fileUuid = hfDmData.getFileUuid();
        String fileVersionId = hfDmData.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.addDmDataInFileService(hfDmData);
        } else {
            return workingTableServiceImpl.addDmDataService(hfDmData);
        }
    }

    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/delDmData")
    public TResponseVo delDmData(@RequestBody HfDmData hfDmData, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmData.setCreateUserId(userId);
        String fileUuid = hfDmData.getFileUuid();
        String fileVersionId = hfDmData.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.delDmDataInFileService(hfDmData);
        } else {
            return workingTableServiceImpl.delDmDataService(hfDmData);
        }

    }

    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/chgDmData")
    public TResponseVo chgDmData(@RequestBody HfDmData hfDmData, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmData.setCreateUserId(userId);
        String fileUuid = hfDmData.getFileUuid();
        String fileVersionId = hfDmData.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.chgDmDataInFileService(hfDmData);
        } else {
            return workingTableServiceImpl.chgDmDataService(hfDmData);
        }

    }

    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/getDmDataList")
    public TResponseVo getDmDataList(@RequestBody PagePojo pagePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        pagePojo.setUserId(userId);
        String fileUuid = pagePojo.getFileUuid();
        String fileVersionId = pagePojo.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.getDmDataListInFileService(pagePojo);
        } else {
            return workingTableServiceImpl.getDmDataListService(pagePojo);
        }

    }

    /**
     * 获取数据管理工具详情
     *
     * @throws Exception
     */
    @PostMapping("/getDmDataTotal")
    public TResponseVo getDmDataTotal(@RequestBody HfDmData hfDmData, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String key = hfDmData.getKey();
        String fileUuid = hfDmData.getFileUuid();
        String fileVersionId = hfDmData.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.getDmDataTotalInFileService(userId, key, fileUuid, fileVersionId);
        } else {
            return workingTableServiceImpl.getDmDataTotalService(userId, key);
        }
    }

    /**
     * 字段排序
     *
     * @throws Exception
     */
    @PostMapping("/orderDmTableColumns")
    public TResponseVo orderDmTableColumns(@RequestBody HfDmColumns hfDmColumns) throws Exception {
        String fileUuid = hfDmColumns.getFileUuid();
        String fileVersionId = hfDmColumns.getFileVersionId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.orderDmTableColumnsInFileService(hfDmColumns);
        } else {
            return workingTableServiceImpl.orderDmTableColumnsService(hfDmColumns);
        }

    }

    /**
     * 上传数据至数据库(新建表)
     *
     * @param file
     * @param key
     * @param desc
     * @param type
     * @param excelPw
     * @param session
     * @return
     * @throws Exception
     */
    @PostMapping("/uploadDmData")
    public TResponseVo uploadDmData(@RequestParam(value = "file", required = false) MultipartFile file,
                                    @RequestParam(value = "key", required = false) String key,
                                    @RequestParam(value = "desc", required = false) String desc,
                                    @RequestParam(value = "type", required = false) String type,
                                    @RequestParam(value = "excelPw", required = false) String excelPw,
                                    @RequestParam(value = "fileUuid", required = false) String fileUuid,
                                    @RequestParam(value = "fileVersionId", required = false) String fileVersionId,
                                    @RequestParam(value = "database", required = false) String database,
                                    HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        if (fileUuid != null && fileVersionId != null) {
            if (database != null) {
                // 从数据管理导入至文内数据
                List<String> list = new JsonKeyUtils().stringToList(database);
                if (list.size() != 2) {
                    throw new WorkTableException("未指定到数据表");
                }
                return workingTableServiceImpl.importDmDataInFileService(key, list.get(1), desc, userId, fileUuid, fileVersionId);
            } else {
                // 上传至文内数据管理
                return workingTableServiceImpl.uploadCommonExcelInFileService(file, key, desc, userId, type, excelPw, fileUuid, fileVersionId);
            }
        } else {
            // 直接上传至数据管理
            return workingTableServiceImpl.uploadCommonExcelService(file, key, desc, userId, type, excelPw);
        }
    }

    /**
     * 从数据管理单纯导入数据 包含表头 包含data
     *
     * @throws Exception
     */
    @PostMapping("/importOnlyData")
    public TResponseVo importOnlyData(@RequestBody HfDmTable hfDmTable, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        String toTableId = hfDmTable.getToTableId();
        String fromTableId = hfDmTable.getFromTableId();
        Boolean overwrite = hfDmTable.getOverwrite();
        return workingTableServiceImpl.importOnlyDataService(toTableId, fromTableId, overwrite, userId, fileUuid, fileVersionId);
    }

    /**
     * 下载
     *
     * @throws Exception
     */
    @GetMapping("/downloadDmData")
    public TResponseVo downloadDmData(String key, String fileUuid, String fileVersionId, HttpSession session, HttpServletResponse response) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        if (fileUuid != null && fileVersionId != null) {
            return workingTableServiceImpl.downloadDmDataInFileService(key, fileUuid, fileVersionId, userId, response);
        } else {
            return workingTableServiceImpl.downloadDmDataService(key, userId, response);
        }
    }

//    /**
//     * 从数据管理导入至文内数据
//     *
//     * @throws Exception
//     */
//    @PostMapping("/importDmDataInFile")
//    public TResponseVo importDmDataInFile(@RequestBody HfDmColumns hfDmColumns, HttpSession session) throws Exception {
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        String userId = user.getUserId();
//        String fileUuid = hfDmColumns.getFileUuid();
//        String fileVersionId = hfDmColumns.getFileVersionId();
//        String tableId = hfDmColumns.getTableId();
//        if (fileUuid == null || fileVersionId == null || tableId == null) {
//            throw new WorkTableException("必填参数为空");
//        }
//        return workingTableServiceImpl.importDmDataInFileService(tableId, userId, fileUuid,fileVersionId);
//    }

    /**
     * 数据管理填写到附件参数,并形成excel文件
     *
     * @throws Exception
     */
    @PostMapping("/uploadAnnexParamByDm")
    public TResponseVo uploadAnnexParamByDm(@RequestBody Map<String, Object> map, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        List<String> tableKey = (List) map.get("tableKey");
        List<String> fields = (List) map.get("fields");
        List<String> rows = (List) map.get("rows");
        String remark = null;
        if (map.get("remark") != null) {
            remark = (String) map.get("remark");
        }
        String fileUuid = (String) map.get("fileUuid");
        String fileVersionId = (String) map.get("fileVersionId");
        return workingTableServiceImpl.uploadAnnexParamByDmService(tableKey, fields, rows, remark, userId, fileUuid, fileVersionId);
    }


    /**
     * 填写文件时获取表格全选的keys
     *
     * @throws Exception
     */
    @PostMapping("/getRelatModelByDm")
    public TResponseVo getRelatModelByDm(@RequestBody HfDmTable hfDmTable, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmTable.setCreateUserId(userId);
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        return workingTableServiceImpl.getRelatModelByDmService(fileUuid,fileVersionId);
    }
    /**
     * 上传并返回填参用的数据
     *
     * @throws Exception
     */
    @PostMapping("/uploadAnnexParamByRelatModel")
    public TResponseVo uploadAnnexParamByRelatModel(@RequestBody HfMyDownload hfMyDownload, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String fileUuid = hfMyDownload.getFileUuid();
        String fileVersionId = hfMyDownload.getFileVersionId();
        String html = hfMyDownload.getHtml();
        String type = hfMyDownload.getType();
        String ffid = hfMyDownload.getFfid();
        return workingTableServiceImpl.uploadAnnexParamByRelatModelService(html,userId,type,fileUuid,fileVersionId,ffid);
    }


    /**
     * 填写文件时获取表格全选的keys
     *
     * @throws Exception
     */
    @PostMapping("/getTotalKeys")
    public TResponseVo getTotalKeys(@RequestBody HfDmTable hfDmTable, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmTable.setCreateUserId(userId);
        return workingTableServiceImpl.getTotalKeysService(hfDmTable);
    }

    /**
     * 填写文件时获取表格全选的keys
     *
     * @throws Exception
     */
    @PostMapping("/calculate")
    public TResponseVo calculate(@RequestBody HfDmColumns hfDmColumns, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String tableId = hfDmColumns.getTableId();
        String columnsId = hfDmColumns.getColumnsId();
        String dataId = hfDmColumns.getDataId();
        return workingTableServiceImpl.calculateService(tableId, columnsId, dataId, userId);
    }


    /**
     * 记录错误日志
     *
     * @throws Exception
     */
    @PostMapping("/saveErrLog")
    public TResponseVo saveErrLog(@RequestBody HfErrLogPojo hfErrLogPojo, HttpSession session
            , @RequestHeader(value = "referer", required = false) String referer
            , @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfErrLogPojo.setCreateUserId(userId);
        return workingTableServiceImpl.saveErrLogService(hfErrLogPojo, referer, userAgent);
    }


    /**
     * 上传教程视频
     *
     * @param file
     * @return
     * @throws Exception
     */
    @PostMapping("/uploadTeachVideo")
    public TResponseVo uploadTeachVideo(@RequestParam(value = "file") MultipartFile file) throws Exception {
        long start = System.currentTimeMillis();  // 接口开始执行时间

        // 自定义uuid
        String uid = UUID.randomUUID().toString().replaceAll("-", "");
        // 获取文件格式
        String oriFileName = file.getOriginalFilename();
        String extension = oriFileName.substring(oriFileName.lastIndexOf(".")).toLowerCase();
        // 创建文件夹  如果不存在 先创建
        if (!new File(this.teachVideoPath).exists()) new File(this.teachVideoPath).mkdir();
        File filePathNew = new File(this.teachVideoPath + "/" + uid);
        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        String docxFilePathNew = this.teachVideoPath + "/" + uid + "/" + uid + extension;
        File docxFileNew = new File(docxFilePathNew);
        try {
            file.transferTo(docxFileNew);
            LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadSubsidiaryFile", "文件上传Controller", "文件上传完成");
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadSubsidiaryFile", "文件上传Controller", "文件上传报错");
            throw new WorkTableException("视频上传报错");
        }
        long start2 = System.currentTimeMillis();  // 上传完成 开始解析
        System.out.println("程序上传运行时间：" + (start2 - start) + "ms");

        // 返回上传路径
        return TResponseVo.success(new HashMap<String, Object>() {{
            put("videoUrl", docxFilePathNew);
        }});
    }


    /**
     * 创建视频教程
     *
     * @throws Exception
     */
    @PostMapping("/createTeachVideo")
    public TResponseVo createTeachVideo(@RequestBody HfTeachVideoPojo hfTeachVideoPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfTeachVideoPojo.setCreateUserId(userId);
        return workingTableServiceImpl.createTeachVideoService(hfTeachVideoPojo);
    }

    /**
     * 删除视频教程
     *
     * @throws Exception
     */
    @PostMapping("/delTeachVideo")
    public TResponseVo delTeachVideo(@RequestBody HfTeachVideoPojo hfTeachVideoPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfTeachVideoPojo.setCreateUserId(userId);
        return workingTableServiceImpl.delTeachVideoService(hfTeachVideoPojo);
    }

    /**
     * 修改视频教程
     *
     * @throws Exception
     */
    @PostMapping("/updateTeachVideo")
    public TResponseVo updateTeachVideo(@RequestBody HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        return workingTableServiceImpl.updateTeachVideoService(hfTeachVideoPojo);
    }

    /**
     * 查询视频教程信息
     *
     * @throws Exception
     */
    @PostMapping("/getTeachVideoInfo")
    public TResponseVo getTeachVideoInfo(@RequestBody HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        return workingTableServiceImpl.getTeachVideoInfoService(hfTeachVideoPojo);
    }

    /**
     * 查询视频教程清单
     *
     * @throws Exception
     */
    @PostMapping("/getTeachVideoList")
    public TResponseVo getTeachVideoList(@RequestBody(required = false) PagePojo pagePojo) throws Exception {
        if (pagePojo == null) {
            pagePojo = new PagePojo();
            pagePojo.setIsPaged("1");  // 默认分页
        }
        return workingTableServiceImpl.getTeachVideoListService(pagePojo);
    }

    /**
     * 有用+1
     *
     * @throws Exception
     */
    @PostMapping("/likeTeachVideo")
    public TResponseVo likeTeachVideo(@RequestBody HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        return workingTableServiceImpl.likeTeachVideoService(hfTeachVideoPojo);
    }

    /**
     * 没用+1
     *
     * @throws Exception
     */
    @PostMapping("/dislikeTeachVideo")
    public TResponseVo dislikeTeachVideo(@RequestBody HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        return workingTableServiceImpl.dislikeTeachVideoService(hfTeachVideoPojo);
    }

    /**
     * 数据表左关联
     *
     * @throws Exception
     */
    @PostMapping("/tableLeftJoin")
    public TResponseVo tableLeftJoin(@RequestBody Map<String, Object> map, HttpSession session) throws Exception {
        Object leftTableIdObj = map.get("leftTableId");
        Object rightTableIdObj = map.get("rightTableId");
        Object columnsIdListObj = map.get("columnsIdList");
        Object tableNameObj = map.get("tableName");
        Object dbIdObj = map.get("dbId");
        if (leftTableIdObj == null || rightTableIdObj == null || columnsIdListObj == null || tableNameObj == null || dbIdObj == null) {
            throw new WorkTableException("必填参数为空");
        }
        String leftTableId = String.valueOf(leftTableIdObj);
        String rightTableId = "";
        if (((List<String>) rightTableIdObj).size() < 2) {
            throw new WorkTableException("未指定数据表或数据视图");
        }
        try {
            rightTableId = ((List<String>) rightTableIdObj).get(((List<String>) rightTableIdObj).size() - 1);
        } catch (Exception e) {
            throw new WorkTableException("未获取到关联表主键");
        }
        List<Map<String, String>> columnsIdList = (List<Map<String, String>>) columnsIdListObj;
        String tableName = String.valueOf(tableNameObj);
        String dbId = String.valueOf(dbIdObj);
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.tableLeftJoinService(leftTableId, rightTableId, columnsIdList, tableName, dbId, userId);
    }

    /**
     * 上传模板
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("uploadModelFile")
    public TResponseVo uploadModelFile(@RequestParam(value = "file") MultipartFile file,
                                       @RequestParam(value = "folderId", required = false) String folderId,
                                       HttpSession session
    ) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return workingTableServiceImpl.uploadModelFileService(file, userId,folderId);
    }

//    /**
//     * 用上传模板上传到参数内
//     *
//     * @param
//     * @return
//     * @throws Exception
//     */
//    @PostMapping("uploadAnnexParamByUploadModel")
//    public TResponseVo uploadAnnexParamByUploadModel(@RequestBody HfUploadModelFilePojo hfUploadModelFilePojo) throws Exception {
//        String fileUuid = hfUploadModelFilePojo.getFileUuid();
//        return workingTableServiceImpl.uploadAnnexParamByUploadModelService(fileUuid);
//    }

    /**
     * 判断文档类型
     *
     * @param
     * @return
     * @throws Exception
     */
    @PostMapping("previewMatchFile")
    public TResponseVo previewMatchFile(@RequestBody HfUploadModelFilePojo hfUploadModelFilePojo) throws Exception {
        String fileUuid = hfUploadModelFilePojo.getFileUuid();
        return workingTableServiceImpl.previewMatchFileService(fileUuid);
    }

    /**
     * 下载uploadModel类型的模板文档
     *
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("downloadModel")
    public TResponseVo downloadModel(String fileUuid,HttpServletResponse response) throws Exception {
        return workingTableServiceImpl.downloadModelService(fileUuid,response);
    }

}
