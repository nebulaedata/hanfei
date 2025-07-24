package cn.nebulaedata.service;

import cn.nebulaedata.pojo.*;
import cn.nebulaedata.vo.TResponseVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/8/12 15:11
 * @note
 */
public interface WorkingTableService {

    /**
     * 工作台
     */
    /**
     * 工作台-获取大纲树
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getOutlineListService(String fileUuid, String fileVersionId) throws Exception;

    /**
     * 工作台-获取模板内容
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getContentService(String fileUuid, String fileVersionId, String outlineId, Boolean origin, String userId, String searchUuid) throws Exception;


    /**
     * 项目
     */
    /**
     * 新建项目
     */
    public TResponseVo addProjectService(ProjectPojo projectPojo, String userId) throws Exception;

    /**
     * 删除项目
     */
    public TResponseVo delProjectService(String projectId) throws Exception;

    /**
     * 修改项目
     */
    public TResponseVo updateProjectService(ProjectPojo projectPojo) throws Exception;

    /**
     * 获取项目信息
     */
    public TResponseVo getProjectInfoService(String projectId) throws Exception;

    /**
     * 获取项目列表
     */
    public TResponseVo getProjectListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 标段
     */
    /**
     * 新建标段
     */
    public TResponseVo addStageService(StagePojo stagePojo, String userId) throws Exception;

    /**
     * 删除标段
     */
    public TResponseVo delStageService(String stageId) throws Exception;

    /**
     * 修改标段
     */
    public TResponseVo updateStageService(StagePojo stagePojo) throws Exception;

    /**
     * 获取标段信息
     */
    public TResponseVo getStageInfoService(String stageId) throws Exception;

    /**
     * 获取标段列表
     */
    public TResponseVo getStageListService(PagePojo pagePojo, String projectId) throws Exception;

    /**
     * 批次
     */
    /**
     * 新建批次
     */
    public TResponseVo addBatchService(BatchPojo batchPojo, String userId) throws Exception;

    /**
     * 删除批次
     */
    public TResponseVo delBatchService(String batchId) throws Exception;

    /**
     * 修改批次
     */
    public TResponseVo updateBatchService(BatchPojo batchPojo) throws Exception;

    /**
     * 修改批次项目信息属性
     */
    public TResponseVo updateBatchPropertyService(LabelValuePojo labelValuePojo) throws Exception;


    /**
     * 获取批次信息
     */
    public TResponseVo getBatchInfoService(String batchId) throws Exception;

    /**
     * 获取批次列表
     */
    public TResponseVo getBatchListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 获取当前用户有权限的项目与分包信息
     */
    public TResponseVo getProjectListByUserService(String userId) throws Exception;

    /**
     * 批次管理-项目列表-添加项目
     */
    public TResponseVo getOptionsService(String type) throws Exception;

    /**
     * 批次管理-项目列表-添加项目
     */
    public TResponseVo addBatchProjectService(String batchId, String projectStageName, String buyType, String mainPerson) throws Exception;

    /**
     * 批次管理-项目列表-删除项目
     */
    public TResponseVo delBatchProjectService(String batchId, String projectStageId) throws Exception;

    /**
     * 批次管理-项目列表-获取项目信息
     */
    public TResponseVo getBatchProjectInfoService(String batchId, String projectStageId) throws Exception;

    /**
     * 批次管理-项目列表-获取项目列表
     */
    public TResponseVo getBatchProjectListService(PagePojo pagePojo, String batchId) throws Exception;

    /**
     * 批次管理-项目列表-调整顺序
     */
    public TResponseVo orderBatchProjectListService(BatchPojo batchPojo) throws Exception;

    /**
     * 批次信息-添加人员
     */
    public TResponseVo addBatchUserService(String batchId, String userId) throws Exception;

    /**
     * 批次信息-删除人员
     */
    public TResponseVo delBatchUserService(String batchId, String userId) throws Exception;

    /**
     * 批次信息-获取用户清单
     */
    public TResponseVo getBatchUserListService(PagePojo pagePojo, String batchId) throws Exception;

    /**
     * 批次文件上传
     */
    public TResponseVo uploadBatchFileService(MultipartFile file, String userId, String batchId, String fileNameNew, String fileType, String projectStageId) throws Exception;

    /**
     * 批次文件新增
     */
    public TResponseVo addBatchFileService(MultipartFile file, String userId, String batchId) throws Exception;

    /**
     * 批次文件删除
     */
    public TResponseVo delBatchFileService(String fileUuid) throws Exception;

    /**
     * 批次文件修改
     */
    public TResponseVo updateBatchFileService(String fileUuid, String projectStageId) throws Exception;

    /**
     * 获取批次文件信息
     */
    public TResponseVo getBatchFileInfoService(String fileUuid) throws Exception;

    /**
     * 获取批次文件清单
     */
    public TResponseVo getBatchFileListService(PagePojo pagePojo, String batchId, String projectStageId) throws Exception;


    /**
     * 批次新加入分包 多选文件
     */
    public TResponseVo addPackageToBatchService(String packageId, List<String> fileUuidList, String batchId, String userId) throws Exception;

    /**
     * 获取用户下所有涉及项目及项目下标段信息列表
     */
    public TResponseVo getUserStageListService(String userId) throws Exception;

    /**
     * 分包
     */
    /**
     * 获取项目策划预选方案
     */
    public TResponseVo getPackagePlanService() throws Exception;

    /**
     * 根据策划新建若干分包
     */
    public TResponseVo addPackageByPlanService(PackagePojo packagePojo) throws Exception;

    /**
     * 新建分包
     */
    public TResponseVo addPackageService(PackagePojo packagePojo) throws Exception;

    /**
     * 删除分包
     */
    public TResponseVo delPackageService(String packageId) throws Exception;

    /**
     * 修改分包
     */
    public TResponseVo updatePackageService(PackagePojo packagePojo) throws Exception;

    /**
     * 获取分包信息
     */
    public TResponseVo getPackageInfoService(String packageId) throws Exception;

    /**
     * 获取分包列表
     */
    public TResponseVo getPackageListService(PagePojo pagePojo, String projectId) throws Exception;


    /**
     * 公司
     */
    /**
     * 新建公司
     */
    public TResponseVo addCompanyService(CompanyPojo companyPojo, String userId) throws Exception;

    /**
     * 删除公司
     */
    public TResponseVo delCompanyService(String companyId) throws Exception;

    /**
     * 修改公司
     */
    public TResponseVo updateCompanyService(CompanyPojo companyPojo) throws Exception;

    /**
     * 获取公司信息
     */
    public TResponseVo getCompanyInfoService(String companyId) throws Exception;

    /**
     * 获取公司列表
     */
    public TResponseVo getCompanyListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 设为默认值
     */
    public TResponseVo setDefaultCompanyService(String companyId, String userId) throws Exception;

    /**
     * 退出公司
     */
    public TResponseVo quitCompanyService(String companyId, String userId) throws Exception;

    /**
     * 根据搜索码获取公司名称
     */
    public TResponseVo getCompanyByAuthService(String authorizationCode) throws Exception;

    /**
     * 申请加入公司
     */
    public TResponseVo applyJoinCompanyService(String companyId, String userId) throws Exception;

    /**
     * 加入公司
     */
    public TResponseVo joinCompanyService(String applyContent) throws Exception;

    /**
     * 项目策划
     */
    public TResponseVo projectPlanService(String projectId) throws Exception;

    /**
     * 项目推荐
     */
    public TResponseVo getIntroductionListService(String projectTypeId) throws Exception;

    /**
     * 获取无项目文件列表
     */
    public TResponseVo getDocumentListService(String userId) throws Exception;

    /**
     * 新增项目文件
     */
    public TResponseVo addProjectFileService(MultipartFile file, String projectId, String fileName, String packageId, String userId, String fileInfo, String fileType, String fileUuid) throws Exception;

    /**
     * 上传项目文件
     */
    public TResponseVo uploadFileService(MultipartFile file, String projectId, String fileName, String packageId, String userId, String fileInfo, String fileType) throws Exception;

    /**
     * 下载
     */
    public TResponseVo downloadFileService(String fileUuid, HttpServletResponse response) throws Exception;

    /**
     * 删除项目文件
     */
    public TResponseVo delProjectFileService(String fileUuid) throws Exception;

    /**
     * 确认文件状态
     */
    public TResponseVo conformFileStatusService(String fileUuid, String fileStatus) throws Exception;


    /**
     * 获取项目标段列表
     */
//    public TResponseVo getStageIdListService(String projectId, String stageName) throws Exception;

    /**
     * 修改项目文件所属分标
     */
//    public TResponseVo updateFileStageIdService(String fileUuid, String stageId) throws Exception;

    /**
     * 获取文件类型列表
     */
    public TResponseVo getProjectFileTypeListService(String typeGroupId) throws Exception;

    /**
     * 修改项目文件属性
     */
    public TResponseVo updateFilePropertyService(ProjectFilePojo projectFilePojo) throws Exception;


    /**
     * 获取文件列表
     */
//    public TResponseVo getProjectFileInfoService(PagePojo pagePojo, ProjectFilePojo projectFilePojo) throws Exception;

    /**
     * 获取文件列表
     */
    public TResponseVo getProjectFileListService(PagePojo pagePojo, ProjectFilePojo projectFilePojo) throws Exception;


    /**
     * 获取存储空间信息
     */
    public TResponseVo getMemoryInfoService(String projectId) throws Exception;

    /**
     * 获取收件箱文件清单
     */
    public TResponseVo getInboxFileListService(String projectId, String batchId, String statusId) throws Exception;

    /**
     * 修改收件箱文件状态
     */
    public TResponseVo updateInboxFileService(String fileUuid, String statusId, String userId) throws Exception;

    /**
     * 获取项目状态
     */
    public TResponseVo getProjectStatusService(String projectId) throws Exception;

    /**
     * 获取文件预览
     */
    public TResponseVo getPreviewService(String fileUuid) throws Exception;


    /**
     * 批次管理
     */
    /**
     * 获取批次状态
     */
    public TResponseVo getBatchStatusService(String batchId) throws Exception;

//    /**
//     * 获取批次信息
//     */
//    public TResponseVo getBatchInfoService(String batchId) throws Exception;


//    /**
//     * 批次新纳入标段(分包)
//     */
//    public TResponseVo addStageToBatchService(String stageId,String batchId) throws Exception;
//
//    /**
//     * 从当前批次删除
//     */
//    public TResponseVo delStageFromBatchService(String stageId) throws Exception;
//
//    /**
//     * 获取当前批次内容(分包列表)
//     */
//    public TResponseVo getStageListService(String batchId) throws Exception;

//    /**
//     * 获取项目标段信息
//     */
//    public TResponseVo getStageInfoService(String stageId) throws Exception;

    /**
     * 添加关联文件
     */
    public TResponseVo addStageFileService(String batchId) throws Exception;

    /**
     * 删除关联文件
     */
    public TResponseVo delStageFileService(String batchId) throws Exception;

    /**
     * 获取关联文件列表
     */
    public TResponseVo getStageFileListService(String batchId) throws Exception;

    /**
     * 我的任务
     */
    /**
     * 我的代办
     */
    public TResponseVo getMyTODOListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 我的草稿
     */
    public TResponseVo getMyDraftListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 我的文档
     */
    public TResponseVo getMyDocumentListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 我的文档新建
     */
    public TResponseVo newMyDocumentService(DocFileIndexPojo docFileIndexPojo, String userId) throws Exception;

    /**
     * 我的文档删除
     */
    public TResponseVo delMyDocumentService(String fileUuid, String fileVersionId) throws Exception;


    /**
     * 招标文件
     */
    /**
     * 我的项目列表
     */
    public TResponseVo getMyProjectListService(String userId) throws Exception;

    /**
     * 创建招标文件
     */
    public Map<String, String> newBidDocumentService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 将招标文件投递至项目中
     */
//    public TResponseVo sendToProjectService(String fileUuid) throws Exception;

    /**
     * 删除招标文件
     */
    public TResponseVo delBidDocumentService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 获取招标文件信息
     */
    public TResponseVo getBidDocumentInfoService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 获取文件大纲信息
     */
    public TResponseVo getOutlineIdListService(String fileUuid, String fileVersionId) throws Exception;

    /**
     * 获取采购类别,采购方式,评标办法
     */
    public TResponseVo getLabelInfoListService(String fileTypeId) throws Exception;


/**
 * 投标文件
 */
    /**
     * 获取投标文件组
     */
    public TResponseVo getTendDocumentService(String fileUuid, String bidKey) throws Exception;

    /**
     * 获取投标文件段落
     */
    public TResponseVo getTendDocumentOutlineService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 根据招标文件识别码和秘钥 创建投标文件
     */
    public TResponseVo newTendDocumentService(List<DocFileIndexPojo> docFileIndexPojoList, String userId, String mainFileUuid) throws Exception;

    /**
     * 根据投标文件获取招标文件fileUuid
     */
    public TResponseVo getBidFromTendService(String fileUuid) throws Exception;

    /**
     * 根据文件id获取最新版本信息
     */
    public TResponseVo getLastVersionService(String fileUuid) throws Exception;

    /**
     * 获取用户列表
     */
    public TResponseVo getUserListService() throws Exception;


/**
 * 新建模板+文件流程
 */
    /**
     * 新建模板
     */
    public TResponseVo newDocumentService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception;

    /**
     * 新建模板-其他文件
     */
    public TResponseVo newQTDocumentService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception;


/**
 * 辅助工具
 */
    /**
     * 新增辅助工具表单
     */
    public TResponseVo newSupToolFormService(HfSupToolFormPojo hfSupToolFormPojo) throws Exception;

    /**
     * 删除辅助工具表单
     */
    public TResponseVo delSupToolFormService(String formId) throws Exception;

    /**
     * 修改辅助工具表单
     *
     * @param hfSupToolFormPojo
     * @return
     * @throws Exception
     */
    public TResponseVo updateSupToolFormService(HfSupToolFormPojo hfSupToolFormPojo) throws Exception;

    /**
     * 查询辅助工具表单信息
     *
     * @param formId
     * @return
     * @throws Exception
     */
    public TResponseVo getSupToolFormInfoService(String formId) throws Exception;

    /**
     * 查询辅助工具表单信息
     *
     * @param pagePojo
     * @param userId
     * @return
     * @throws Exception
     */
    public TResponseVo getSupToolFormListService(PagePojo pagePojo, String userId) throws Exception;

/**
 * 新辅助工具
 */
    /**
     * 新建辅助工具模型
     */
    public TResponseVo newSupTableService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception;

    /**
     * 新增辅助工具分类标签
     */
    public TResponseVo addSupLabelService(LabelValuePojo labelValuePojo, String userId) throws Exception;

    /**
     * 删除辅助工具分类标签
     */
    public TResponseVo delSupLabelService(String uuid, String userId) throws Exception;


    /**
     * 删除辅助工具分类标签
     */
    public TResponseVo getSupLabelListService(String userId) throws Exception;


    /**
     * 删除辅助工具模型
     */
    public TResponseVo delSupTableService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception;

    /**
     * 更新辅助工具模型
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    public TResponseVo updateSupTableService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception;

    /**
     * 查询辅助工具模型
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    public TResponseVo getSupTableInfoService(HfSupTableFormPojo hfSupTableFormPojo, String userId) throws Exception;

    /**
     * 查询辅助工具模型列表
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getSupTableListService(PagePojo pagePojo, String userId, List<String> label, Boolean enable) throws Exception;


    /**
     * 新建辅助工具数据
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    public TResponseVo newSupTableFormService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception;

    /**
     * 删除辅助工具数据
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    public TResponseVo delSupTableFormService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception;

    /**
     * 修改辅助工具数据
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    public TResponseVo updateSupTableFormService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception;

    /**
     * 查询辅助工具数据
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    public TResponseVo getSupTableFormInfoService(HfSupTableFormPojo hfSupTableFormPojo, String userId) throws Exception;

    /**
     * 查询辅助工具数据列表
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getSupTableFormListService(PagePojo pagePojo, String userId) throws Exception;


    /**
     * 辅助工具数据下载excel
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo downloadSupTableExcelService(String tableId, List<String> formIdList, HttpServletResponse response) throws Exception;


    /**
     * 辅助工具上传excel
     */
    public TResponseVo uploadSupTableExcelService(MultipartFile file, String userId) throws Exception;

    /**
     * 辅助工具自动填写-预填写
     */
    public TResponseVo supAutoWriteService(String fileUuid, String fileVersionId, String userId, String formId) throws Exception;

    /**
     * 辅助工具自动填写-确认填写
     */
    public TResponseVo supAutoWriteSureService(String fileUuid, String fileVersionId, String userId, Map<String, Object> map) throws Exception;


    /**
     * 获取辅助工具填写历史列表
     */
    public TResponseVo getSupTableHistoryListService(String fileUuid, String fileVersionId) throws Exception;

    /**
     * 获取辅助工具填写历史详情
     */
    public TResponseVo getSupTableRecentService(String historyUuid) throws Exception;


    /**
     * 获取辅助工具及实例级联树
     */
    public TResponseVo getSupTableTreeService(String userId) throws Exception;


    /**
     * 获取文件类型清单
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getFileTypeDiService() throws Exception;

    /**
     * 完全删除某篇文章
     *
     * @return
     * @throws Exception
     */
    public TResponseVo delFileService(String fileUuid) throws Exception;

    /**
     * 完全删除某篇文章
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getModelListService(String userId, String type) throws Exception;

    /**
     * 模板组
     */
    /**
     * 新建模板组
     *
     * @param modelGroupPojo
     * @return
     * @throws Exception
     */
    public TResponseVo newModelGroupService(ModelGroupPojo modelGroupPojo) throws Exception;

    /**
     * 删除模板组
     *
     * @param modelGroupPojo
     * @return
     * @throws Exception
     */
    public TResponseVo delModelGroupService(ModelGroupPojo modelGroupPojo) throws Exception;

    /**
     * 修改模板组
     *
     * @param modelGroupPojo
     * @return
     * @throws Exception
     */
    public TResponseVo updateModelGroupService(ModelGroupPojo modelGroupPojo) throws Exception;

    /**
     * 查询模板组
     *
     * @param modelGroupPojo
     * @return
     * @throws Exception
     */
    public TResponseVo getModelGroupInfoService(ModelGroupPojo modelGroupPojo) throws Exception;

    /**
     * 查询模板组列表
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getModelGroupListService() throws Exception;

    /**
     * 查询模板组列表KV
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getModelGroupListKVService() throws Exception;

    /**
     * 获取平台视图
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getGlobalViewService(ModelGroupPojo modelGroupPojo) throws Exception;

    /**
     * 获取模板组标签预筛选按钮列表
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getModelGroupLabelListService(ModelGroupPojo modelGroupPojo) throws Exception;


    /**
     * 文库
     */
    /**
     * 获取文库常用列表
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getCommonUseListService(PagePojo pagePojo) throws Exception;

    /**
     * 获取文库母版列表
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getLibraryListService(PagePojo pagePojo) throws Exception;

    /**
     * 获取文库搜索结果
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo searchLibraryService(PagePojo pagePojo) throws Exception;

    /**
     * 文库-文件血缘关系
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getLibraryViewService(PagePojo pagePojo) throws Exception;

    /**
     * 文库-热门搜索
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getLibraryHotSearchService() throws Exception;

    /**
     * 首页
     */
    /**
     * 模板类型分布
     */
    public TResponseVo getModelRateService() throws Exception;

    /**
     * 模板文件分布
     */
    public TResponseVo getFileRateService() throws Exception;

    /**
     * 首页-我的文档统计
     */
    public TResponseVo getMyFileStatisticsService(String userId) throws Exception;

    /**
     * 首页-热力图
     */
    public TResponseVo getHeatMapService() throws Exception;


    /**
     * 母版管理
     */
    /**
     * 获取模板清单
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getMasterModelService(PagePojo pagePojo) throws Exception;

    /**
     * 获取模板清单LV
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getMasterModelLVService() throws Exception;

    /**
     * 获取最近使用文档目录
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getRecentFileListService(String userId, Object limit, String fileTypeGroupId) throws Exception;

    /**
     * 文库管理-文档审核
     */
    /**
     * 获取文库文档审核列表
     */
    public TResponseVo getAuditingFileListService(PagePojo pagePojo) throws Exception;


    /**
     * 文库文档审核操作
     */
    public TResponseVo changeAuditingFileStatusService(String fileUuid, String fileVersionId, String auditingStatus, String auditingReason, String auditingUserId) throws Exception;

    /**
     * 文库管理-文档管理
     */
    /**
     * 获取文库文档管理列表
     */
    public TResponseVo getLibraryFileManagementListService(PagePojo pagePojo) throws Exception;

    /**
     * 获取文库文档下拉版本记录
     */
    public TResponseVo getLibraryFileVersionListService(String fileUuid) throws Exception;

    /**
     * 文库文档上下线操作
     */
    public TResponseVo changeLibraryFileStatusService(String fileUuid, String fileVersionId, String fileStatus) throws Exception;

    /**
     * 文库文档删除
     */
    public TResponseVo deleteLibraryFileService(String fileUuid) throws Exception;

    /**
     * 文库文档推荐
     */
    public TResponseVo changeLibraryFileRecommendService(String fileUuid, String recommend) throws Exception;

    /**
     * 获取审核结果通知
     */
    public TResponseVo getResultNoticeService(PagePojo pagePojo, String userId, Boolean isRead, String noticeType) throws Exception;

    /**
     * 已读
     */
    public TResponseVo readResultNoticeService(String auditingUuid) throws Exception;

    /**
     * 用户管理
     *
     */
    /**
     * 新增用户
     */
    public TResponseVo addUserService(DocUserPojo docUserPojo) throws Exception;

    /**
     * 删除用户
     */
    public TResponseVo delUserService(DocUserPojo docUserPojo) throws Exception;

    /**
     * 修改用户
     */
    public TResponseVo updateUserService(DocUserPojo docUserPojo) throws Exception;

    /**
     * 查询用户列表
     */
    public TResponseVo getUserListService(PagePojo pagePojo) throws Exception;

    /**
     * 查询用户信息
     */
    public TResponseVo getUserInfoService(DocUserPojo docUserPojo) throws Exception;


    /**
     * 重置密码
     */
    public TResponseVo resetPasswordService(DocUserPojo docUserPojo) throws Exception;

    /**
     * 角色管理
     */
    /**
     * 新增角色
     *
     * @param docUserRolesPojo
     * @return
     * @throws Exception
     */
    public TResponseVo addRolesService(DocUserRolesPojo docUserRolesPojo) throws Exception;

    /**
     * 删除角色
     *
     * @param docUserRolesPojo
     * @return
     * @throws Exception
     */
    public TResponseVo delRolesService(DocUserRolesPojo docUserRolesPojo) throws Exception;

    /**
     * 获取权限清单
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getMenuListService() throws Exception;

    /**
     * 修改角色
     *
     * @param docUserRolesPojo
     * @return
     * @throws Exception
     */
    public TResponseVo chgRolesService(DocUserRolesPojo docUserRolesPojo) throws Exception;

    /**
     * 获取角色信息
     *
     * @param docUserRolesPojo
     * @return
     * @throws Exception
     */
    public TResponseVo getRolesInfoService(DocUserRolesPojo docUserRolesPojo) throws Exception;

    /**
     * 获取角色清单
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    public TResponseVo getRolesListService(PagePojo pagePojo) throws Exception;


    /**
     * 获取角色列表的LV
     */
    public TResponseVo getRolesService() throws Exception;

    /**
     * 我的贡献
     */
    public TResponseVo getMyContributionService(PagePojo pagePojo, String userId) throws Exception;


    /**
     * 申请下载
     */
    public TResponseVo getDocxService(String fileName, String fileUuid, String fileVersionId, String userId, String html, String downloadType) throws Exception;


    /**
     * 获取我的下载列表
     */
    public TResponseVo getMyDownloadListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 删除我的下载
     */
    public TResponseVo delMyDownloadService(String uuid) throws Exception;

    /**
     * 下载我的下载
     */
    public TResponseVo downloadMyDownloadService(String uuid, String userId, HttpServletResponse response) throws Exception;

    /**
     * 查看下载日志
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getDownloadRecordListService(PagePojo pagePojo) throws Exception;


/**
 * 评审模型管理
 */
    /**
     * 新增评审模型
     */
    public TResponseVo newAssessModelService(HfAssessModelPojo hfAssessModelPojo) throws Exception;

    /**
     * 删除评审模型
     */
    public TResponseVo delAssessModelService(String assessId) throws Exception;

    /**
     * 修改评审模型
     */
    public TResponseVo updateAssessModelService(HfAssessModelPojo hfAssessModelPojo) throws Exception;

    /**
     * 获取评审模型信息
     */
    public TResponseVo getAssessModelInfoService(String assessId) throws Exception;

    /**
     * 获取评审模型清单
     */
    public TResponseVo getAssessModelListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 新增评审要素
     */
    public TResponseVo newAssessModelElementService(HfAssessModelElementPojo hfAssessModelElementPojo) throws Exception;

    /**
     * 删除评审要素
     */
    public TResponseVo delAssessModelElementService(String assessId, String elementType) throws Exception;

    /**
     * 修改评审要素
     */
    public TResponseVo updateAssessModelElementService(HfAssessModelElementPojo hfAssessModelElementPojo) throws Exception;

    /**
     * 查询评审要素
     */
    public TResponseVo getAssessModelElementInfoService(String assessId, String elementType) throws Exception;

    /**
     * 查询评审要素
     */
    public TResponseVo getAssessModelElementListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 调整评审条款顺序
     */
    public TResponseVo updateAssessModelOrderService(HfAssessModelElementPojo hfAssessModelElementPojo) throws Exception;

    /**
     * 参数下拉列表
     */
    public TResponseVo getAssessModelParamService(HfAssessModelElementPojo hfAssessModelElementPojo, String userId) throws Exception;

    /**
     * 招采库
     */
    public TResponseVo getZhaocaiGlobalViewService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 招采库-法律主图
     */
    public TResponseVo getZhaocaiGlobalViewLabelService() throws Exception;


    /**
     * 评标工具-获取评审标准类型
     */
    public TResponseVo getElementTypeOptionsService(String fileUuid, String assessId) throws Exception;


    /**
     * 帮助文档-目录
     */
    public TResponseVo getHelpDocCatalogueService() throws Exception;

    /**
     * 帮助文档-获取帮助文档内容
     */
    public TResponseVo getHelpDocContentService(String helpDocId) throws Exception;

    /**
     * 帮助文档-搜索
     */
    public TResponseVo searchHelpDocService(PagePojo pagePojo, String searchContent) throws Exception;

    /**
     * 帮助文档-热搜词
     */
    public TResponseVo getHotSearchKeyService() throws Exception;

    /**
     * 我的空间-新建文档-最近创建
     */
    public TResponseVo getRecentBuildService(String userId) throws Exception;

    /**
     * 批次管理-新辅助评标
     */
    public TResponseVo newJudgeService(HfJudgePojo hfJudgePojo) throws Exception;


    /**
     * 批次管理-新辅助评标-根据分包获取评审文件及评审模型
     */
    public TResponseVo getFileAndAssessService(String projectStageId) throws Exception;


    /**
     * 批次管理-删除辅助评标
     */
    public TResponseVo delJudgeService(String judgeId) throws Exception;

    /**
     * 批次管理-修改辅助评标
     */
    public TResponseVo updateJudgeService(HfJudgePojo hfJudgePojo) throws Exception;

    /**
     * 批次管理-查询辅助评标信息
     */
    public TResponseVo getJudgeInfoService(String judgeId) throws Exception;

    /**
     * 批次管理-查询辅助评标清单
     */
    public TResponseVo getJudgeListService(PagePojo pagePojo) throws Exception;

    /**
     * 批次管理-查询辅助评标清单
     */
    public TResponseVo getJudgeDetailService(HfJudgePojo hfJudgePojo) throws Exception;

    /**
     * 创建文件-获取专业文档类型选择
     */
    public TResponseVo getFileTypeListService(String fileTypeGroupId) throws Exception;

    /**
     * 创建文件-企业常用模板
     */
    public TResponseVo getCompanyUseFileListService(Integer limit) throws Exception;

    /**
     * 评审步骤模板配置-新增评审步骤模板
     */
    public TResponseVo createFastAssessService(HfFastAssessPojo hfFastAssessPojo) throws Exception;

    /**
     * 评审步骤模板配置-删除评审步骤模板
     */
    public TResponseVo delFastAssessService(HfFastAssessPojo hfFastAssessPojo) throws Exception;

    /**
     * 评审步骤模板配置-更新评审步骤模板
     */
    public TResponseVo updateFastAssessService(HfFastAssessPojo hfFastAssessPojo) throws Exception;

    /**
     * 评审步骤模板配置-获取评审步骤模板信息
     */
    public TResponseVo getFastAssessInfoService(String fastId) throws Exception;

    /**
     * 评审步骤模板配置-获取评审步骤模板清单
     */
    public TResponseVo getFastAssessListService(PagePojo pagePojo) throws Exception;

    /**
     * 评审步骤模板配置-获取评审步骤模板清单LV
     */
    public TResponseVo getFastAssessListLVService(PagePojo pagePojo) throws Exception;

    /**
     * 评审步骤模板配置-读取预览
     */
    public TResponseVo getFastAssessPreviewService(String fastId) throws Exception;

    /**
     * 通过工具服务链接获取同步数据
     */
    public void syncFileService(DocFileIndexPojo docFileIndexPojo, HttpServletResponse response) throws Exception;

    /**
     * 接收离线同步数据-上传
     */
    public Map<String, String> upLoadSyncDataService(Map<String, Object> map, String fileName, String fileDesc, List<String> fileLabelIds, String userId) throws Exception;


    /**
     * 获取菜单类型
     */
    public TResponseVo getOptionsTypeListService() throws Exception;

    /**
     * 获取菜单
     */
    public TResponseVo getOptionsListService(PagePojo pagePojo) throws Exception;

    /**
     * 新增菜单
     */
    public TResponseVo addOptionsService(OptionsPojo optionsPojo) throws Exception;

    /**
     * 删除菜单
     */
    public TResponseVo delOptionsService(String id) throws Exception;

/**
 * 我的数据-数据管理工具
 */
    /**
     * 新增数据管理工具
     *
     * @param hfDmDb
     * @return
     * @throws Exception
     */
    public TResponseVo createDmDbService(HfDmDb hfDmDb) throws Exception;

    public TResponseVo createDmDbInFileService(HfDmDb hfDmDb) throws Exception;  // 文内

    /**
     * 删除数据管理工具
     *
     * @param dbId
     * @return
     * @throws Exception
     */
    public TResponseVo delDmDbService(String dbId) throws Exception;

    public TResponseVo delDmDbInFileService(String dbId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 修改数据管理工具
     *
     * @param hfDmDb
     * @return
     * @throws Exception
     */
    public TResponseVo chgDmDbService(HfDmDb hfDmDb) throws Exception;

    public TResponseVo chgDmDbInFileService(HfDmDb hfDmDb) throws Exception;

    /**
     * 获取数据管理工具清单
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    public TResponseVo getDmDbListService(PagePojo pagePojo) throws Exception;

    public TResponseVo getDmDbListInFileService(PagePojo pagePojo) throws Exception;

    /**
     * 获取数据管理工具详情
     *
     * @param dbId
     * @return
     * @throws Exception
     */
    public TResponseVo getDmDbInfoService(String dbId) throws Exception;

    public TResponseVo getDmDbInfoInFileService(String dbId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 数据表排序
     *
     * @param dbId        数据库id
     * @param tableIdList 数据表顺序数组
     * @return
     * @throws Exception
     */
    public TResponseVo orderDmTableService(String dbId, List<String> tableIdList) throws Exception;

    public TResponseVo orderDmTableInFileService(String dbId, List<String> tableIdList, String fileUuid, String fileVersionId) throws Exception;
    /**
     * 数据表
     */
    /**
     * 新增数据管理工具
     *
     * @param hfDmTable
     * @return
     * @throws Exception
     */
    public TResponseVo createDmTableService(HfDmTable hfDmTable) throws Exception;

    public TResponseVo createDmTableInFileService(HfDmTable hfDmTable) throws Exception;

    /**
     * 删除数据管理工具
     *
     * @param dbId
     * @return
     * @throws Exception
     */
    public TResponseVo delDmTableService(String dbId) throws Exception;

    public TResponseVo delDmTableInFileService(String dbId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 修改数据管理工具
     *
     * @param hfDmTable
     * @return
     * @throws Exception
     */
    public TResponseVo chgDmTableService(HfDmTable hfDmTable) throws Exception;

    public TResponseVo chgDmTableInFileService(HfDmTable hfDmTable) throws Exception;

    /**
     * 新增表头字段
     *
     * @param hfDmColumns
     * @return
     * @throws Exception
     */
    public TResponseVo addDmTableColumnsService(HfDmColumns hfDmColumns) throws Exception;

    public TResponseVo addDmTableColumnsInFileService(HfDmColumns hfDmColumns) throws Exception;

    /**
     * 删除表头字段
     *
     * @param map
     * @return
     * @throws Exception
     */
    public TResponseVo delDmTableColumnsService(Map map) throws Exception;

    public TResponseVo delDmTableColumnsInFileService(Map map, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 修改表头字段
     *
     * @param hfDmColumns
     * @return
     * @throws Exception
     */
    public TResponseVo chgDmTableColumnsService(HfDmColumns hfDmColumns) throws Exception;

    public TResponseVo chgDmTableColumnsInFileService(HfDmColumns hfDmColumns) throws Exception;

    /**
     * 查询表头字段
     *
     * @param hfDmColumns
     * @return
     * @throws Exception
     */
    public TResponseVo getDmTableColumnsListService(HfDmColumns hfDmColumns) throws Exception;

    public TResponseVo getDmTableColumnsListInFileService(HfDmColumns hfDmColumns) throws Exception;

    /**
     * 获取数据表内容
     *
     * @param hfDmTable
     * @return
     * @throws Exception
     */
    public TResponseVo getDmTableContentService(HfDmTable hfDmTable) throws Exception;

    public TResponseVo getDmTableContentInFileService(HfDmTable hfDmTable) throws Exception;

    /**
     * 修改视图(view)的条件(condition)
     *
     * @param key    视图id
     * @param filter 过滤条件
     * @param group  聚合条件
     * @return
     * @throws Exception
     */
    public TResponseVo updateViewConditionService(String key, List<Map<String, Object>> filter, Map<String, Object> group) throws Exception;

    public TResponseVo updateViewConditionInFileService(String key, List<Map<String, Object>> filter, Map<String, Object> group, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 重跑视图(view)数据
     *
     * @param key 需要重跑的视图id
     * @return
     * @throws Exception
     */
    public TResponseVo rerunViewDataService(String key, String userId) throws Exception;

    public TResponseVo rerunViewDataInFileService(String key, String userId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 复制表
     *
     * @param key
     * @return
     * @throws Exception
     */
    public TResponseVo copyTableService(String key, String newTableName, List<String> targetDb, String userId, Boolean includeData) throws Exception;

    public TResponseVo copyTableInFileService(String key, String newTableName, List<String> targetDb, String userId, Boolean includeData, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 用数据管理的表替换文件阶段的表
     *
     * @param key            数据管理表id
     * @param replaceTableId 文内被替换的表id
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    public TResponseVo replaceTableService(String key, String replaceTableId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 保存结果数据
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo saveDmDataService(HfDmTable hfDmTable, String saveType) throws Exception;

    public TResponseVo saveDmDataInFileService(HfDmTable hfDmTable, String saveType) throws Exception;

    /**
     * 新增数据
     *
     * @param hfDmData
     * @return
     * @throws Exception
     */
    public TResponseVo addDmDataService(HfDmData hfDmData) throws Exception;

    public TResponseVo addDmDataInFileService(HfDmData hfDmData) throws Exception;

    /**
     * 删除数据
     *
     * @param hfDmData
     * @return
     * @throws Exception
     */
    public TResponseVo delDmDataService(HfDmData hfDmData) throws Exception;

    public TResponseVo delDmDataInFileService(HfDmData hfDmData) throws Exception;

    /**
     * 修改数据
     *
     * @param hfDmData
     * @return
     * @throws Exception
     */
    public TResponseVo chgDmDataService(HfDmData hfDmData) throws Exception;

    public TResponseVo chgDmDataInFileService(HfDmData hfDmData) throws Exception;

    /**
     * 获取数据内容清单
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    public TResponseVo getDmDataListService(PagePojo pagePojo) throws Exception;

    public TResponseVo getDmDataListInFileService(PagePojo pagePojo) throws Exception;

    /**
     * 获取表数据量
     *
     * @param userId
     * @param key    tableId
     * @return
     * @throws Exception
     */
    public TResponseVo getDmDataTotalService(String userId, String key) throws Exception;

    public TResponseVo getDmDataTotalInFileService(String userId, String key, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 字段排序
     *
     * @param hfDmColumns
     * @return
     * @throws Exception
     */
    public TResponseVo orderDmTableColumnsService(HfDmColumns hfDmColumns) throws Exception;

    public TResponseVo orderDmTableColumnsInFileService(HfDmColumns hfDmColumns) throws Exception;
    /**
     * 上传excel至数据管理
     * @param
     * @return
     * @throws Exception
     */
//    public TResponseVo uploadDmDataService(MultipartFile file,String dbId,String desc,String userId,String type) throws Exception;

    /**
     * 上传excel至数据管理 new
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo uploadCommonExcelService(MultipartFile file, String dbId, String desc, String userId, String type, String excelPw) throws Exception;

    public TResponseVo uploadCommonExcelInFileService(MultipartFile file, String dbId, String desc, String userId, String type, String excelPw, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 数据管理下载excel
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo downloadDmDataService(String tableId, String userId, HttpServletResponse response) throws Exception;

    public TResponseVo downloadDmDataInFileService(String tableId, String fileUuid, String fileVersionId, String userId, HttpServletResponse response) throws Exception;


    /**
     * 从数据管理导入数据
     *
     * @param databaseId    文内目标数据库
     * @param tableId       数据管理数据表
     * @param userId        当前用户
     * @param fileUuid      目标文件
     * @param fileVersionId 目标版本
     * @return
     * @throws Exception
     */
    public TResponseVo importDmDataInFileService(String databaseId, String tableId, String desc, String userId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 从数据管理单纯导入数据 包含表头 包含data
     *
     * @param toTableId     目标表tableId
     * @param fromTableId   来源表tableId
     * @param overwrite     是否覆盖数据
     * @param userId        当前用户
     * @param fileUuid      目标文件
     * @param fileVersionId 目标版本
     * @return
     * @throws Exception
     */
    public TResponseVo importOnlyDataService(String toTableId, String fromTableId, Boolean overwrite, String userId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 数据管理填写到附件参数,并形成excel文件
     *
     * @return
     * @throws Exception
     */
    public TResponseVo uploadAnnexParamByDmService(List<String> tableKey, List<String> fields, List<String> rows, String userId, String remark, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 获取数据管理中引用的相关模板清单
     *
     * @param fileUuid      目标文件
     * @param fileVersionId 目标版本
     * @return
     * @throws Exception
     */
    public TResponseVo getRelatModelByDmService(String fileUuid, String fileVersionId) throws Exception;

    /**
     * 数据管理关联的模板,填写到附件参数,并形成word文件
     *
     * @return
     * @throws Exception
     */
    public TResponseVo uploadAnnexParamByRelatModelService(String html,String userId,String type,String fileUuid,String fileVersionId,String ffid) throws Exception;


    /**
     * 填写文件时获取表格全选的keys
     */
    public TResponseVo getTotalKeysService(HfDmTable hfDmTable) throws Exception;


    /**
     * 表left join操作
     *
     * @param leftTableId   左表id
     * @param rightTableId  右表id
     * @param columnsIdList 关联字段id
     * @param tableName     新表名
     * @param dbId          表位置
     * @return
     * @throws Exception
     */
    public TResponseVo tableLeftJoinService(String leftTableId, String rightTableId, List<Map<String, String>> columnsIdList, String tableName, String dbId, String userId) throws Exception;

    /**
     * 计算列
     *
     * @param tableId   表id
     * @param columnsId 列id
     * @param dataId    数据id
     * @return
     * @throws Exception
     */
    public TResponseVo calculateService(String tableId, String columnsId, String dataId, String userId) throws Exception;

    /**
     * 列匹配
     *
     * @param tableId       表id
     * @param columnsIdList 列id
     * @param dataId        数据id
     * @return
     * @throws Exception
     */
    public TResponseVo columnsMatchService(String tableId, String columnsId, String dataId, String userId) throws Exception;


/**
 * 报错日志记录
 */
    /**
     * 记录错误日志
     */
    public TResponseVo saveErrLogService(HfErrLogPojo hfErrLogPojo, String referer, String userAgent) throws Exception;


/**
 * 教程视频
 */
    /**
     * 创建视频教程
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    public TResponseVo createTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception;

    /**
     * 删除视频教程
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    public TResponseVo delTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception;

    /**
     * 修改视频教程
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    public TResponseVo updateTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception;

    /**
     * 获取视频教程信息
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    public TResponseVo getTeachVideoInfoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception;

    /**
     * 获取视频教程列表
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getTeachVideoListService(PagePojo pagePojo) throws Exception;

    /**
     * 有用
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    public TResponseVo likeTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception;

    /**
     * 没用
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    public TResponseVo dislikeTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception;

    /**
     * 上传模板
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo uploadModelFileService(MultipartFile file, String userId, String folderId) throws Exception;


//    /**
//     * 将上传模板作为附件上传到参数内
//     * @param fileUuid
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo uploadAnnexParamByUploadModelService(String fileUuid) throws Exception;


    /**
     * 预览关联模板
     * @param fileUuid
     * @return
     * @throws Exception
     */
    public TResponseVo previewMatchFileService(String fileUuid) throws Exception;

    /**
     * 下载uploadModel类型的模板文档
     * @param fileUuid
     * @return
     * @throws Exception
     */
    public TResponseVo downloadModelService(String fileUuid, HttpServletResponse response) throws Exception;

}