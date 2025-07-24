package cn.nebulaedata.dao;

import cn.nebulaedata.pojo.*;
import cn.nebulaedata.vo.TResponseVo;
import org.apache.ibatis.annotations.Param;

import java.util.*;

/**
 * @author 徐衍旭
 * @date 2021/8/12 15:09
 * @note
 */
public interface WorkingTableMapper {

    /**
     * 工作台-获取大纲树
     *
     * @return
     */
    public List<OutLinePojo> getOutlineListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<OutLinePojo> getOutlineListDao1(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 工作台-获取模板内容
     *
     * @return
     */
    public List<OutLinePojo> getContentDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("outlineId") String outlineId);

    /**
     * 工作台-新建项目
     *
     * @return
     */
    public Integer addProjectDao(ProjectPojo projectPojo);

    /**
     * 工作台-检查项目名称是否重复
     *
     * @return
     */
    public Integer checkProjectNameDao(String projectName, String projectId);

    /**
     * 工作台-删除项目
     *
     * @return
     */
    public Integer delProjectDao(@Param("projectId") String projectId);

    /**
     * 工作台-修改项目
     *
     * @return
     */
    public Integer updateProjectDao(ProjectPojo projectPojo);

    /**
     * 工作台-获取项目信息
     *
     * @return
     */
    public ProjectPojo getProjectInfoDao(@Param("projectId") String projectId);

    /**
     * 工作台-获取用户列表
     *
     * @return
     */
    public List<LabelValuePojo> getUserListDao();

    /**
     * 工作台-获取分包个数
     *
     * @return
     */
    public Integer getPackageCountDao(@Param("projectId") String projectId);

    /**
     * 工作台-获取分包个数
     *
     * @return
     */
    public Integer getFileCountDao(@Param("projectId") String projectId);

    /**
     * 工作台-获取项目列表
     *
     * @return
     */
    public List<ProjectPojo> getProjectListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit);


    /**
     * 工作台-新建标段
     *
     * @return
     */
    public Integer addStageDao(StagePojo stagePojo);

    /**
     * 工作台-检查标段名重复
     *
     * @return
     */
    public Integer checkStageNameDao(String stageName);

    /**
     * 工作台-删除标段
     *
     * @return
     */
    public Integer delStageDao(@Param("stageId") String stageId);

    /**
     * 工作台-修改标段
     *
     * @return
     */
    public Integer updateStageDao(StagePojo stagePojo);

    /**
     * 工作台-获取标段信息
     *
     * @return
     */
    public StagePojo getStageInfoDao(@Param("stageId") String stageId);

    /**
     * 工作台-获取标段列表
     *
     * @return
     */
    public List<StagePojo> getStageListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("projectId") String projectId);


    /**
     * 获取选项
     *
     * @return
     */
    public List<OptionsPojo> getOptionsDao(String type, String flag);

    /**
     * 工作台-新建分支
     *
     * @return
     */
    public Integer addBatchDao(BatchPojo batchPojo);

    /**
     * 工作台-检查分支名称重复性
     *
     * @return
     */
    public Integer checkBatchNoDao(@Param("batchNo") String batchNo, @Param("batchId") String batchId);

    /**
     * 工作台-删除分支
     *
     * @return
     */
    public Integer delBatchDao(@Param("batchId") String batchId);

    /**
     * 工作台-修改分支
     *
     * @return
     */
    public Integer updateBatchDao(BatchPojo batchPojo);

    /**
     * 工作台-获取分支信息
     *
     * @return
     */
    public BatchPojo getBatchInfoDao(@Param("batchId") String batchId);

    /**
     * 工作台-获取分支信息
     *
     * @return
     */
    public BatchPojo getBatchInfoByBatchNoDao(@Param("batchNo") String batchNo);

    /**
     * 工作台-获取分支列表
     *
     * @return
     */
    public List<BatchPojo> getBatchListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit);

    /**
     * 获取当前用户有权限的项目与分包信息
     *
     * @return
     */
    public List<ProjectPojo> getProjectListByUserDao();


    /**
     * 获取当前用户有权限的项目与分包信息
     *
     * @return
     */
    public List<PackagePojo> getPackageListByProjectIdListDao(@Param("projectIdListStr") String projectIdListStr);

    /**
     * 获取当前用户有权限的项目与分包信息
     *
     * @return
     */
    public Integer updateBatchProjectDao(@Param("batchId") String batchId, @Param("projectStageListStr") String projectStageListStr);


    /**
     * 批次信息-添加人员
     *
     * @return
     */
    public Integer updateBatchUserListDao(@Param("batchId") String batchId, @Param("includeUserIdStr") String includeUserIdStr, @Param("includeUserInfoStr") String includeUserInfoStr);


    /**
     * 添加批次文件
     *
     * @return
     */
    public Integer addBatchFileDao(BatchFilePojo batchFilePojo);


    /**
     * 工作台-新建分包
     *
     * @return
     */
    public Integer addPackageListDao(String sql);

    /**
     * 工作台-新建分包
     *
     * @return
     */
    public Integer addPackageDao(PackagePojo packagePojo);

    /**
     * 工作台-检查项目的分包个数
     *
     * @return
     */
    public Integer getPackageListSizeDao(@Param("projectId") String projectId);

    /**
     * 工作台-删除分包
     *
     * @return
     */
    public Integer delPackageDao(@Param("packageId") String packageId);

    /**
     * 工作台-修改分包
     *
     * @return
     */
    public Integer updatePackageDao(PackagePojo packagePojo);

    /**
     * 工作台-获取分包信息
     *
     * @return
     */
    public PackagePojo getPackageInfoDao(@Param("packageId") String packageId);

    /**
     * 工作台-获取分包列表
     *
     * @return
     */
    public List<PackagePojo> getPackageListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("projectId") String projectId);

    /**
     * 工作台-获取分包名列表
     *
     * @return
     */
    public List<String> getPackageNameListDao(@Param("projectId") String projectId);


    /**
     * 工作台-新建公司信息
     *
     * @return
     */
    public Integer addCompanyDao(CompanyPojo companyPojo);

    /**
     * 工作台-删除公司信息
     *
     * @return
     */
    public Integer delCompanyDao(@Param("companyId") String companyId);

    /**
     * 工作台-修改公司信息
     *
     * @return
     */
    public Integer updateCompanyDao(CompanyPojo companyPojo);

    /**
     * 工作台-获取公司信息信息
     *
     * @return
     */
    public CompanyPojo getCompanyInfoDao(@Param("companyId") String companyId);

    /**
     * 工作台-获取公司信息列表
     *
     * @return
     */
    public List<CompanyPojo> getCompanyListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("companyIdListStr") String companyIdListStr);

    /**
     * 工作台-修改默认公司
     *
     * @return
     */
    public Integer setDefaultCompanyDao(@Param("companyId") String companyId, @Param("userId") String userId);

    /**
     * 退出公司
     */
    public Integer updateUserCompanyIdListDao(@Param("companyIdListStr") String companyIdListStr, @Param("userId") String userId);


    /**
     * 工作台-获取招标文件列表
     *
     * @return
     */
    public List<DocFileIndexPojo> getBidDocumentListDao();

    /**
     * 工作台-新增项目文件
     *
     * @return
     */
    public Integer addProjectFileDao(ProjectFilePojo projectFilePojo);

    /**
     * 工作台-
     *
     * @return
     */
    public Integer updateFileStatusDao(String fileUuid, String fileStatus);

    /**
     * 工作台-
     *
     * @return
     */
    public Integer updateFileStatusDao2(String fileUuidListStr, String fileStatus);


    /**
     * 项目中文件置为可见或不可见
     *
     * @return
     */
    public Integer updateProjectShowDao(String fileUuidListStr, String isShow);

    /**
     * 项目中文件置为可见或不可见
     *
     * @return
     */
    public Integer updateBatchShowDao(String fileUuidListStr, String isShow);

    /**
     * 工作台-删除项目文件
     *
     * @return
     */
    public Integer delProjectFileDao(String fileUuid);

    /**
     * 工作台-删除批次文件
     *
     * @return
     */
    public Integer delBatchFileDao(String fileUuid);

    /**
     * 工作台-修改项目文件
     *
     * @return
     */
    public Integer updateProjectFileDao(ProjectFilePojo projectFilePojo);

    /**
     * 工作台-修改批次文件
     *
     * @return
     */
    public Integer updateBatchFileDao(@Param("fileUuid") String fileUuid, @Param("projectStageId") String projectStageId);

    /**
     * 工作台-修改项目文件属性
     *
     * @return
     */
    public Integer updateFilePropertyDao(ProjectFilePojo projectFilePojo);

    /**
     * 工作台-查询项目(标段)文件信息
     *
     * @return
     */
    public ProjectFilePojo getProjectFileInfoDao(String fileUuid);

    /**
     * 工作台-查询批次文件信息
     *
     * @return
     */
    public BatchFilePojo getBatchFileInfoDao(String fileUuid);

    /**
     * 工作台-查询项目(标段)文件列表
     *
     * @return
     */
    public List<ProjectFilePojo> getProjectFileListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("projectId") String projectId, @Param("packageId") String packageId);

    /**
     * 工作台-查询批次文件列表
     *
     * @return
     */
    public List<BatchFilePojo> getBatchFileListDao(@Param("paramNameLike") String paramNameLike, @Param("batchId") String batchId, @Param("projectStageId") String projectStageId);


    /**
     * 工作台-获取当前用户相关项目Id清单
     *
     * @return
     */
    public List<ProjectPojo> getUserProjectListDao(String userId);

    /**
     * 工作台-获取当前用户相关项目Id清单
     *
     * @return
     */
    public List<StagePojo> getUserStageListDao(@Param("projectListStr") String projectListStr);

    /**
     * 工作台-获取项目标段列表
     *
     * @return
     */
    public List<LabelValuePojo> getStageIdListDao(@Param("projectId") String projectId, @Param("stageName") String stageName);

    /**
     * 工作台-批次新纳入标段
     *
     * @return
     */
    public Integer updateStageBatchIdDao(@Param("stageId") String stageId, @Param("batchId") String batchId);

    /**
     * 工作台-获取当前批次内容
     *
     * @return
     */
    public List<StagePojo> getBatchStageListDao(@Param("batchId") String batchId);

    /**
     * 工作台-获取当前批次内容
     *
     * @return
     */
    public Integer updateFileStageIdDao(@Param("stageId") String stageId);

    /**
     * 工作台-获取我的文档列表
     *
     * @return
     */
    public List<DocFileIndexPojo> getMyDocumentListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("userId") String userId, @Param("companyId") String companyId, @Param("fileTypeId") String fileTypeId, @Param("createTime1") String createTime1, @Param("createTime2") String createTime2, @Param("updateTime1") String updateTime1, @Param("updateTime2") String updateTime2);

    /**
     * 工作台-我的文档新建
     *
     * @return
     */
    public Integer newMyDocumentDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 工作台-我的文档删除
     *
     * @return
     */
    public Integer delMyDocumentDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 包名查重
     *
     * @param packageName
     * @param packageId
     * @return
     */
    public Integer checkPackageNameDao(@Param("packageName") String packageName, @Param("packageId") String packageId);

    /**
     * 项目文件名查重
     *
     * @param fileName
     * @param projectId
     * @return
     */
    public Integer checkFileNameDao(@Param("fileName") String fileName, @Param("fileUuid") String fileUuid, @Param("projectId") String projectId);


    /**
     * 获取用户信息
     */
    public DocUserPojo getUserInfoDao(@Param("userId") String userId);

    /**
     * 获取公司信息
     */
    public CompanyPojo getCompanyByAuthDao(@Param("authorizationCode") String authorizationCode);

    /**
     * 加入公司
     */
    public Integer joinCompanyDao(@Param("companyIdListStrNew") String companyIdListStrNew, @Param("userId") String userId);

    /**
     * 提交申请
     */
    public Integer applyJoinCompanyDao(ApplyPojo applyPojo);

    /**
     * 获取文章开头
     */
    public String getFirstOutlineIdDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 确定招标要素-返回要素信息
     *
     * @return
     */
    public List<DocLabelPojo> getLabelInfoListDao();

    /**
     * 创建招标文件索引
     */
    public Integer newBidDocumentIndexDao(DocFileIndexPojo bidFileIndex);

    /**
     * 创建招标文件附表信息
     *
     * @param bidFileIndex
     * @return
     */
    public Integer newBidDocumentInfoDao(DocFileIndexPojo bidFileIndex);

    /**
     * 创建招标文件-版本
     */
    public Integer newBidDocumentVersionDao(DocFileIndexPojo bidFileIndex);

    /**
     * 创建招标文件-大纲
     */
    public Integer newBidDocumentOutlineDao(String sql);

    /**
     * 获取内容
     *
     * @param bidFileIndex
     * @return
     */
    public List<ContentPojo> getBidDocumentContentDao(DocFileIndexPojo bidFileIndex);

    /**
     * 创建招标文件-内容
     */
    public Integer newBidDocumentContentDao(String sql);

    /**
     * 创建招标文件-参数
     */
    public Integer newBidDocumentParamDao(DocFileIndexPojo bidFileIndex);

    /**
     * 创建招标文件-标注
     */
    public Integer newBidDocumentTagDao(DocFileIndexPojo bidFileIndex);

    /**
     * 创建招标文件-数据管理
     */
    public Integer newBidDocumentContentDmDbDao(DocFileIndexPojo bidFileIndex);

    public Integer newBidDocumentContentDmTableDao(DocFileIndexPojo bidFileIndex);

    public Integer newBidDocumentContentDmColumnsDao(DocFileIndexPojo bidFileIndex);
//    public Integer newBidDocumentContentDmDataDao(DocFileIndexPojo bidFileIndex);

    /**
     * 创建招标文件-数据表
     */
    public Integer newBidDocumentContentDbDao(DocFileIndexPojo bidFileIndex);

    /**
     * 创建招标文件-书签
     */
    public Integer newBidDocumentBookmarkDao(DocFileIndexPojo bidFileIndex);

    /**
     * 创建招标文件-书签
     */
    public List<DocFileIndexPojo> getAllSubsidiaryBidFileListDao(DocFileIndexPojo bidFileIndex);

    /**
     * 校验bidKey
     */
    public Integer checkBidKeyDao(@Param("fileUuid") String fileUuid, @Param("bidKey") String bidKey);


    /**
     * 获取招标文件信息
     */
    public DocFileIndexPojo getBidDocumentInfoDao(@Param("fileUuid") String fileUuid);

    /**
     * 判断某项目中某文件是否已存在
     */
    public Integer checkFileExistsDao(@Param("projectId") String projectId, @Param("fileUuid") String fileUuid);

    /**
     * 获取投标文件
     */
    public List<LabelValuePojo> getMyProjectListDao();

    /**
     * 获取投标文件
     */
    public List<LabelValuePojo> getTendDocumentInfoDao(@Param("fileUuid") String fileUuid);

    /**
     * 获取当前文库发行版信息
     */
    public DocFileIndexPojo getOnlineVersionInfoDao(@Param("fileUuid") String fileUuid);

    /**
     * 获取投标文件
     */
    public List<LabelValuePojo> getTendDocumentInfo2Dao(@Param("fileUuid") String fileUuid);

    /**
     * 创建投标文件索引
     */
    public Integer newTendFileIndexDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 创建投标版本信息
     */
    public Integer newTendFileVersionDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 获取选中的大纲
     */
    public List<OutLinePojo> getNewTendFileOutlineDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 创建投标文件大纲
     */
    public Integer newTendFileOutlineDao(String sql);

    /**
     * 获取选中的内容
     */
    public List<ContentPojo> getTendFileContentDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 创建投标文件内容
     */
    public Integer newTendFileContentDao(String sql);

    /**
     * 创建参数
     */
    public Integer newTendContentParamDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 创建标注
     */
    public Integer newTendContentTagDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 创建数据管理
     */
    public Integer newTendContentDmDbDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newTendContentDmTableDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newTendContentDmColumnsDao(DocFileIndexPojo docFileIndexPojo);
//    public Integer newTendContentDmDataDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 创建数据表
     */
    public Integer newTendContentDbDao(DocFileIndexPojo docFileIndexPojo);


    /**
     * 根据投标文件uuid获取招标文件uuid
     */
    public String getBidFromTendDao(String fileUuid);

    /**
     * 根据文件id获取最新版本信息
     */
    public String getLastVersionDao(String fileUuid);

    /**
     * 根据fileUuidList获取VersionIdList
     */
    public List<DocFileIndexPojo> getFileInfoListDao(String fileUuidListStr);

    /**
     * 根据fileUuidList获取VersionIdList
     */
    public List<DocFileIndexPojo> getFileAllInfoListDao(String fileUuidListStr);

    /**
     * 校验重名
     *
     * @param formName
     * @param formId
     * @return
     */
    public Integer checkFormNameDao(String formName, String formId);

    /**
     * 新建组织设计辅助工具表单
     *
     * @param hfSupToolFormPojo
     * @return
     */
    public Integer newSupToolFormDao(HfSupToolFormPojo hfSupToolFormPojo);

    /**
     * 删除辅助工具表单
     *
     * @param formId
     * @return
     */
    public Integer delSupToolFormDao(String formId);

    /**
     * 修改辅助工具表单
     *
     * @param hfSupToolFormPojo
     * @return
     */
    public Integer updateSupToolFormDao(HfSupToolFormPojo hfSupToolFormPojo);

    /**
     * 查询辅助工具表单信息
     *
     * @param formId
     * @return
     */
    public HfSupToolFormPojo getSupToolFormInfoDao(String formId);

    /**
     * 获取辅助工具列表
     *
     * @param
     * @return
     */
    public List<HfSupToolFormPojo> getSupToolListDao();

    /**
     * 获取辅助工具清单表
     *
     * @param
     * @return
     */
    public List<HfSupToolFormPojo> getSupToolFormListDao(@Param("userId") String userId);

    /**
     * 新建辅助工具模型
     */
    public Integer newSupTableDao(HfSupTableFormPojo hfSupTableFormPojo);


    /**
     * 新增辅助工具分类标签
     */
    public Integer addSupLabelDao(@Param("uuid") String uuid, @Param("label") String label, @Param("value") String value, @Param("userId") String userId);

    /**
     * 检查辅助工具分类标签名重复
     */
    public Integer checkSupLabelDao(@Param("label") String label, @Param("userId") String userId);

    /**
     * 删除辅助工具分类标签
     */
    public Integer delSupLabelDao(@Param("uuid") String uuid);

    /**
     * 根据uuid查标签信息
     */
    public LabelValuePojo getSupLabelInfoDao(@Param("uuid") String uuid);

    /**
     * 根据uuid查标签信息
     */
    public Integer updateSupTableLabelDao(@Param("tableId") String tableId, @Param("label") String label);

    /**
     * 获取标签列表
     */
    public List<LabelValuePojo> getSupLabelListDao(@Param("userId") String userId);

    /**
     * 删除辅助工具模型
     *
     * @param tableId
     * @return
     */
    public Integer delSupTableDao(@Param("tableId") String tableId);


    /**
     * 更新辅助工具模型
     *
     * @param hfSupTableFormPojo
     * @return
     */
    public Integer updateSupTableDao(HfSupTableFormPojo hfSupTableFormPojo);

    /**
     * 获取辅助工具模型信息
     */
    public HfSupTableFormPojo getSupTableInfoDao(@Param("tableId") String tableId);


    /**
     * 获取辅助工具模型列表
     *
     * @param
     * @return
     */
    public List<HfSupTableFormPojo> getSupTableListDao(@Param("userId") String userId, @Param("paramNameLike") String paramNameLike, @Param("enable") String enable);


    /**
     * 新建辅助工具数据
     */
    public Integer updateUseCntDao(String tableId, String useCnt);


    /**
     * 新建辅助工具数据
     */
    public Integer newSupTableFormDao(HfSupTableFormPojo hfSupTableFormPojo);

    /**
     * 删除辅助工具数据
     *
     * @param formId
     * @return
     */
    public Integer delSupTableFormDao(@Param("formId") String formId, @Param("tableId") String tableId);

    /**
     * 更新辅助工具数据
     *
     * @param hfSupTableFormPojo
     * @return
     */
    public Integer updateSupTableFormDao(HfSupTableFormPojo hfSupTableFormPojo);

    /**
     * 查询辅助工具数据信息
     *
     * @param formId
     * @return
     */
    public HfSupTableFormPojo getSupTableFormInfoDao(@Param("formId") String formId);

    /**
     * 查询辅助工具数据列表
     *
     * @param userId
     * @return
     */
    public List<HfSupTableFormPojo> getSupTableFormListDao(@Param("tableId") String tableId, @Param("userId") String userId, @Param("paramNameLike") String paramNameLike);

    /**
     * 查询辅助工具数据列表
     *
     * @param formIdListStr
     * @return
     */
    public List<HfSupTableFormPojo> getSupTableFormListByformIdDao(@Param("tableId") String tableId, @Param("formIdListStr") String formIdListStr);


    /**
     * 查询辅助工具数据列表
     *
     * @param sql
     * @return
     */
    public Integer uploadSupTableExcelDao(@Param("sql") String sql);

    /**
     * 查询辅助工具数据列表
     *
     * @param
     * @return
     */
    public List<DocParamsPojo> getSupParamDataDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 查询辅助工具数据列表
     *
     * @param
     * @return
     */
    public Integer updateSupParamValueDao(DocParamsPojo docParamsPojo);

    /**
     * 获取辅助工具及实例级联树
     *
     * @param
     * @return
     */
    public List<LabelValuePojo> getSupTableListLVDao(String userId);

    /**
     * 获取辅助工具及实例级联树
     *
     * @param
     * @return
     */
    public List<LabelValuePojo> getSupTableFormListLVDao(String userId);


    /**
     * 获取收件箱文件清单
     *
     * @param
     * @return
     */
    public List<HfProjectInboxPojo> getInboxFileListDao(@Param("projectId") String projectId, @Param("batchId") String batchId, @Param("statusId") String statusId);

    /**
     * 修改收件箱文件状态
     *
     * @param
     * @return
     */
    public Integer updateInboxFileDao(@Param("fileUuid") String fileUuid, @Param("statusId") String statusId);

    /**
     * 获取收件箱某个文件信息
     *
     * @param
     * @return
     */
    public HfProjectInboxPojo getProjectInboxFileInfoDao(@Param("fileUuid") String fileUuid);

    /**
     * 获取收件箱某个文件信息
     *
     * @param
     * @return
     */
    public List<HfFileTypeDiPojo> getFileTypeDiDao(@Param("typeGroupId") String typeGroupId);

    /**
     * 获取收件箱某个文件信息
     *
     * @param
     * @return
     */
    public String getGroupIdByFileTypeIdDao(@Param("fileTypeId") String fileTypeId);

    /**
     * 获取模板列表
     *
     * @param
     * @return
     */
    public List<LabelValuePojo> getModelListDao(@Param("userId") String userId, @Param("type") String type);

    /**
     * 检查模板组唯一性
     *
     * @param
     * @return
     */
    public Integer checkModelGroupUniquenessDao(@Param("fileUuid") String fileUuid);

    /**
     * 新增模板组
     *
     * @param
     * @return
     */
    public Integer newModelGroupDao(ModelGroupPojo modelGroupPojo);

    /**
     * 获取模板组信息
     *
     * @param
     * @return
     */
    public ModelGroupPojo getModelGroupInfoDao(String modelGroupId);

    /**
     * 获取模板组信息
     *
     * @param
     * @return
     */
    public Integer delModelGroupDao(String modelGroupId);

    /**
     * 获取模板组信息
     *
     * @param
     * @return
     */
    public List<ModelGroupPojo> getModelGroupListDao();

    /**
     * 获取模板组信息
     *
     * @param
     * @return
     */
    public List<LabelValuePojo> getModelGroupListKVDao();

    /**
     * 获取模板组信息
     *
     * @param
     * @return
     */
    public List<String> getFileInfoByMainFileUuid2Dao(String fileUuid, String fileVersionId);


    /**
     * 文库-获取文库母版列表
     *
     * @return
     */
    public List<DocFileIndexPojo> getCommonUseListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") Integer limit, @Param("fileTypeId") String fileTypeId);

    /**
     * 文库-获取文库母版列表
     *
     * @return
     */
    public List<DocFileIndexPojo> getLibraryListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("fileTypeId") String fileTypeId);

    /**
     * 文库-获取文库母版列表-我的贡献
     *
     * @return
     */
    public List<DocFileIndexPojo> getCommonUseList2Dao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("fileTypeId") String fileTypeId);

    /**
     * 文库-获取文库母版列表-我的贡献
     *
     * @return
     */
    public List<DocFileIndexPojo> getLibraryList2Dao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("fileTypeId") String fileTypeId);


    /**
     * 文库-获取文库搜索
     *
     * @return
     */
    public List<DocFileIndexPojo> getSearchListDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("fileTypeId") String fileTypeId);


    /**
     * 首页-模板类型分布
     *
     * @return
     */
    public List<DocFileIndexPojo> getModelRateDao();

    /**
     * 首页-模板类型分布
     *
     * @return
     */
    public List<DocFileIndexPojo> getFileRateDao();

    /**
     * 首页-我的文档统计-我创作的模板
     *
     * @return
     */
    public Integer getMyCreateModelCntDao(@Param("userId") String userId);

    /**
     * 首页-我的文档统计-我创作的文件
     *
     * @return
     */
    public Integer getMyCreateFileCntDao(@Param("userId") String userId);

    /**
     * 首页-我的文档统计-我创作的文件
     *
     * @return
     */
    public Integer getMyCollectCntDao(@Param("userId") String userId);

    /**
     * 首页-我的文档统计-我创作的文件
     *
     * @return
     */
    public List<DocFileIndexPojo> getHeatMapDao();

    /**
     * 获取模板清单
     */
    public List<DocFileIndexPojo> getMasterModelDao(@Param("paramNameLike") String paramNameLike, @Param("limit") String limit, @Param("fileTypeId") String fileTypeId);

    public List<LabelValuePojo> getMasterModelLVDao();

    /**
     * 获取模板清单
     */
    public List<DocFileIndexPojo> getRecentFileListDao(@Param("userId") String userId, @Param("num") String num, @Param("fileTypeGroupId") String fileTypeGroupId);

    public List<DocFileIndexPojo> getRecentFileList2Dao(@Param("userId") String userId, @Param("num") String num, @Param("fileTypeGroupId") String fileTypeGroupId);

    /**
     * 获取文档审核列表
     */
    public List<DocFileIndexPojo> getAuditingFileListDao(@Param("paramNameLike") String paramNameLike);

    /**
     * 审核操作
     */
    public Integer changeAuditingFileStatusDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("auditingStatus") String auditingStatus, @Param("auditingReason") String auditingReason, @Param("auditingUserId") String auditingUserId, @Param("auditingTime") Date auditingTime);

    /**
     * 查询文库中记录
     */
    public HfFileLibraryPojo getAuditingFileRecordDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 审核完成通知用户
     */
    public Integer noticeCreateUserDao(HfAuditingResultNoticePojo hfAuditingResultNoticePojo);

    /**
     * 获取审核结果通知
     */
    public List<HfAuditingResultNoticePojo> getResultNoticeDao(@Param("userId") String userId, @Param("isRead") String isRead, @Param("noticeType") String noticeType);

    /**
     * 已读
     */
    public Integer readResultNoticeDao(@Param("auditingUuid") String auditingUuid);

    /**
     * 获取文库文档管理列表
     */
    public List<DocFileIndexPojo> getLibraryFileManagementListDao(@Param("paramNameLike") String paramNameLike, @Param("fileClassStr") String fileClassStr, @Param("fileTypeIdStr") String fileTypeIdStr);

    /**
     * 获取文库文档管理列表
     */
    public List<DocFileIndexPojo> getLibraryFileVersionListAuditingInfoDao();

    /**
     * 获取文库文档管理列表
     */
    public List<LabelValuePojo> getLibraryFileVersionListDao(@Param("fileUuid") String fileUuid);

    /**
     * 获取文库文档管理列表
     */
    public Integer changeLibraryFileStatusDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("fileStatus") String fileStatus);

    /**
     * 获取文档上线状态
     */
    public Integer getLibraryFileLineStatusDao(@Param("fileUuid") String fileUuid);

    /**
     * 文库文档删除
     */
    public Integer deleteLibraryFileDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 文档推荐
     */
    public Integer changeLibraryFileRecommendDao(@Param("fileUuid") String fileUuid, @Param("recommend") String recommend);

    /**
     * 新增用户
     */
    public Integer addUserDao(DocUserPojo docUserPojo);

    /**
     * 检查用户重复性
     */
    public Integer checkUserDao(String userPhone, String userId);

    public Integer checkUserNameDao(String userName, String userId);

    /**
     * 删除用户
     */
    public Integer delUserDao(String userId);

    /**
     * 修改用户
     */
    public Integer updateUserDao(DocUserPojo docUserPojo);

    /**
     * 获取用户列表
     */
    public List<DocUserPojo> getUserInfoListDao(String userId);

    /**
     * 获取用户列表
     */
    public Integer resetPasswordDao(String userId);

    /**
     * 新增角色
     *
     * @param docUserRolesPojo
     * @return
     */
    public Integer addRolesDao(DocUserRolesPojo docUserRolesPojo);

    /**
     * 创建角色权限
     *
     * @param rolesId
     * @param menu
     * @return
     */
    public Integer addRolesToMenuDao(String rolesId, List<String> menu);

    /**
     * 删除角色权限
     *
     * @param rolesId
     * @return
     */
    public Integer delRolesToMenuDao(String rolesId);

    /**
     * 获取角色权限
     *
     * @param rolesId
     * @return
     */
    public List<DocMenuPojo> getRolesToMenuDao(String rolesId);

    /**
     * 检查角色重复性
     */
    public Integer checkRolesNameDao(String rolesName, String rolesId);

    /**
     * 删除角色
     *
     * @param docUserRolesPojo
     * @return
     */
    public Integer delRolesDao(DocUserRolesPojo docUserRolesPojo);

    /**
     * 检查角色是否被使用
     *
     * @param rolesId
     * @return
     */
    public Integer checkRolesUseDao(String rolesId);

    /**
     * 修改角色
     *
     * @param docUserRolesPojo
     * @return
     */
    public Integer chgRolesDao(DocUserRolesPojo docUserRolesPojo);

    /**
     * 查询角色信息
     *
     * @param docUserRolesPojo
     * @return
     */
    public DocUserRolesPojo getRolesInfoDao(DocUserRolesPojo docUserRolesPojo);

    /**
     * 查询角色清单
     *
     * @param paramNameLike
     * @return
     */
    public List<DocUserRolesPojo> getRolesListDao(@Param("paramNameLike") String paramNameLike);

    /**
     * 获取权限清单
     *
     * @param
     * @return
     */
    public List<DocMenuPojo> getMenuListDao();


    /**
     * 获取用户列表的LV
     */
    public List<LabelValuePojo> getRolesDao();

    /**
     * 我的下载
     */
    /**
     * 获取用户列表
     */
    public Integer addHfMyDownloadDao(HfMyDownload hfMyDownload);

    public Integer delMyDownloadDao(String uuid);

    public HfMyDownload getMyDownloadInfoDao(String uuid);

    /**
     * 记录下载日志
     *
     * @param userId 当前用户id
     * @param uuid   请求下载的文件uuid
     * @param logId  随机id主键
     * @return
     */
    public Integer newDownloadRecordDao(String userId, String uuid, String logId);

    /**
     * 查看下载日志清单
     *
     * @param paramNameLike
     * @return
     */
    public List<HfMyDownloadRecordPojo> getDownloadRecordListDao(@Param("paramNameLike") String paramNameLike);

    public List<HfMyDownload> getMyDownloadListDao(String userId, String paramNameLike);

    public Integer updateHfMyDownloadDao(String uuid, String fileStatus, String path, String sizeInMB);


    /**
     * 新增评审模型
     */
    public Integer newAssessModelDao(HfAssessModelPojo hfAssessModelPojo);


    /**
     * 新增评审模型
     */
    public Integer delAssessModelDao(String assessId);

    /**
     * 新增评审模型
     */
    public Integer updateAssessModelDao(HfAssessModelPojo hfAssessModelPojo);

    /**
     * 新增评审模型
     */
    public HfAssessModelPojo getAssessModelInfoDao(String assessId);

    /**
     * 新增评审模型
     */
    public List<HfAssessModelPojo> getAssessModelListDao(@Param("paramNameLike") String paramNameLike);


    /**
     * 新增评审要素
     */
    public Integer newAssessModelElementDao(HfAssessModelElementPojo hfAssessModelElementPojo);

    /**
     * 删除评审要素
     */
    public Integer delAssessModelElementDao(@Param("assessId") String assessId, @Param("elementType") String elementType);

    /**
     * 修改评审要素
     */
    public Integer updateAssessModelElementDao(HfAssessModelElementPojo hfAssessModelElementPojo);

    /**
     * 查询评审要素
     */
    public List<HfAssessModelElementPojo> getAssessModelElementListByTypeDao(@Param("assessId") String assessId, @Param("elementType") String elementType);

    /**
     * 查询评审要素
     */
    public List<HfAssessModelElementPojo> getAssessModelElementListDao(@Param("paramNameLike") String paramNameLike, @Param("assessId") String assessId);

    public List<HfFastAssessElementPojo> getFastAssessElementListDao(@Param("paramNameLike") String paramNameLike, @Param("fastId") String fastId, @Param("tabUuid") String tabUuid, @Param("assessId") String assessId);

    /**
     * 查询评审要素数量
     */
    public Integer getAssessModelElementListCntDao(@Param("assessId") String assessId, @Param("elementType") String elementType);

    /**
     * 查询评审类型数量
     */
    public Integer getAssessModelTypeCntDao(@Param("assessId") String assessId, @Param("elementType") String elementType);

    /**
     * 查询评审类型的顺序字段
     */
    public String getElementTypeOrderDao(@Param("assessId") String assessId, @Param("elementType") String elementType);

    /**
     * 获取评审类型的顺序列
     */
    public List<HfAssessModelElementPojo> getElementTypeOrderListDao(@Param("assessId") String assessId);

    public List<HfFastAssessElementPojo> getFastElementTypeOrderListDao(@Param("fastId") String fastId, @Param("tabUuid") String tabUuid, @Param("assessId") String assessId);

    /**
     * 更新评审类型的顺序列
     */
    public Integer updateAssessModelOrderDao(@Param("elementTypeOrder") String elementTypeOrder, @Param("elementIdList") String elementIdList);

    /**
     * 更新评审类型的顺序列
     */
    public List<LabelValuePojo> getAssessModelParamDao(@Param("paramsUseSceneId") String paramsUseSceneId, @Param("userId") String userId);


    /**
     * 获取帮助文档目录
     */
    public List<HfHelpDocCatalogue> getHelpDocCatalogueDao();

    /**
     * 获取帮助文档目录内容
     */
    public HfHelpDocCatalogue getHelpDocContentDao(@Param("helpDocId") String helpDocId);

    /**
     * 获取帮助文档搜索
     */
    public List<HfHelpDocCatalogue> searchHelpDocDao(@Param("searchContent") String searchContent);

    /**
     * 获取最近创建
     */
    public List<LabelValuePojo> getRecentBuildDao(@Param("userId") String userId);

    /**
     * 创建辅助评审
     */
    public Integer newJudgeDao(HfJudgePojo hfJudgePojo);

    /**
     * 创建辅助评审
     */
    public Integer delJudgeDao(@Param("judgeId") String judgeId);

    /**
     * 创建辅助评审
     */
    public Integer updateJudgeDao(HfJudgePojo hfJudgePojo);

    /**
     * 创建辅助评审
     */
    public HfJudgePojo getJudgeInfoDao(@Param("judgeId") String judgeId);

    public List<HfJudgePojo> getJudgeListDao(@Param("paramNameLike") String paramNameLike, @Param("batchId") String batchId);

    public List<ProjectFilePojo> getFileByProjectStageIdDao(@Param("projectStageId") String projectStageId);

    public List<LabelValuePojo> getFileLVByProjectStageIdDao(@Param("projectStageId") String projectStageId);

    public ProjectFilePojo getBidFileByProjectStageIdDao(@Param("projectStageId") String projectStageId);

    public HfContentAssessPojo getContentAssessDao(@Param("fileUuid") String fileUuid);

    public List<DocParamsPojo> getJudgeParamsDao(@Param("fileUuidListStr") String fileUuidListStr, @Param("judgeParamsIdListStr") String judgeParamsIdListStr);

    public List<DocParamsPojo> getContentParamListDao(@Param("fileUuid") String fileUuid);

    public List<TagPojo> getContentTagListDao(@Param("fileUuid") String fileUuid);

    public List<DocFileIndexPojo> getCompanyUseFileListDao(@Param("limit") Integer limit);

    /**
     * 同步-索引
     *
     * @param docFileIndexPojo
     * @return
     */
    public DocFileIndexPojo syncFileIndexDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 同步-版本
     *
     * @param docFileIndexPojo
     * @return
     */
    public DocFileVerIndexPojo syncFileVersionDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 同步-大纲
     *
     * @param docFileIndexPojo
     * @return
     */
    public List<OutLinePojo> syncFileOutlineDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 同步-内容
     *
     * @param docFileIndexPojo
     * @return
     */
    public List<ContentPojo> syncFileContentDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 同步-参数
     *
     * @param docFileIndexPojo
     * @return
     */
    public List<DocParamsPojo> syncDeriveContentParamDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 同步-标注
     *
     * @param docFileIndexPojo
     * @return
     */
    public List<TagPojo> syncDeriveContentTagDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 同步-书签
     *
     * @param docFileIndexPojo
     * @return
     */
    public List<BookmarkPojo> syncDeriveContentBookmarkDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 同步-书签
     *
     * @param fileUuid
     * @return
     */
    public HfContentAssessPojo syncDeriveFileAssessDao(String fileUuid);

    /**
     * 同步-书签
     *
     * @param fileUuid
     * @return
     */
    public List<HfContentAssessElementPojo> syncDeriveFileAssessElementDao(String fileUuid);

    /*

     */

    /**
     * 上传同步-索引
     *
     * @param docFileIndexPojo
     * @return
     */
    public Integer uploadSyncFileIndexDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 上传同步-版本
     *
     * @param docFileVerIndexPojo
     * @return
     */
    public Integer uploadSyncFileVerIndexDao(DocFileVerIndexPojo docFileVerIndexPojo);

    /**
     * 上传同步-段落
     *
     * @param lists
     * @return
     */
    public Integer uploadSyncFileOutlineDao(@Param("lists") List<OutLinePojo> lists);

    /**
     * 上传同步-段落
     *
     * @param lists
     * @return
     */
    public Integer uploadSyncFileContentDao(@Param("lists") List<ContentPojo> lists);

    /**
     * 上传同步-段落
     *
     * @param lists
     * @return
     */
    public Integer uploadSyncFileParamsDao(@Param("lists") List<DocParamsPojo> lists);

    /**
     * 上传同步-段落
     *
     * @param lists
     * @return
     */
    public Integer uploadSyncFileTagsDao(@Param("lists") List<TagPojo> lists);

    /**
     * 上传同步-段落
     *
     * @param lists
     * @return
     */
    public Integer uploadSyncFileBookmarksDao(@Param("lists") List<BookmarkPojo> lists);

    /**
     * 上传同步-段落
     *
     * @param hfContentAssessPojo
     * @return
     */
    public Integer uploadSyncFileAssessDao(HfContentAssessPojo hfContentAssessPojo);

    /**
     * 同步-书签
     *
     * @param hfContentAssessElementPojos
     * @return
     */
    public Integer uploadSyncFileAssessElementsDao(@Param("lists") List<HfContentAssessElementPojo> hfContentAssessElementPojos);


    /**
     * 校验菜单重复
     *
     * @param
     * @return
     */
    public Integer checkOptionsDao(String label, String type);

    /**
     * 获取选项类型
     *
     * @param
     * @return
     */
    public List<LabelValuePojo> getOptionsTypeListDao(String flag);

    /**
     * 获取选项
     *
     * @param type
     * @return
     */
    public List<OptionsPojo> getOptionsListDao(String type, String paramNameLike);

    /**
     * 新增选项
     *
     * @param optionsPojo
     * @return
     */
    public Integer addOptionsDao(OptionsPojo optionsPojo);

    /**
     * 删除选项
     *
     * @param id
     * @return
     */
    public Integer delOptionsDao(String id);

/**
 * 数据库管理工具 - 数据库
 */
    /**
     * 创建数据库
     *
     * @param hfDmDb
     * @return
     */
    public Integer createDmDbDao(HfDmDb hfDmDb);

    public Integer createDmDbInFileDao(HfDmDb hfDmDb);

    /**
     * 校验重名
     *
     * @param dbName
     * @param dbId
     * @return
     */
    public Integer checkDmDbNameDao(String dbName, String dbId, String userId);

    public Integer checkDmDbNameInFileDao(String dbName, String dbId, String fileUuid, String fileVersionId);

    /**
     * 删除数据库
     *
     * @param dbId
     * @return
     */
    public Integer delDmDbDao(String dbId);

    public Integer delDmDbInFileDao(String dbId, String fileUuid, String fileVersionId);

    /**
     * 修改数据库
     *
     * @param hfDmDb
     * @return
     */
    public Integer chgDmDbDao(HfDmDb hfDmDb);

    public Integer chgDmDbInFileDao(HfDmDb hfDmDb);


    /**
     * 获取数据管理工具清单
     *
     * @param paramNameLike
     * @return
     */
    public List<HfDmDb> getDmDbListDao(String createUserId, String paramNameLike);

    public List<HfDmDb> getDmDbListInFileDao(String createUserId, String paramNameLike, String fileUuid, String fileVersionId);

    /**
     * 获取数据管理工具信息
     *
     * @param dbId
     * @return
     */
    public HfDmDb getDmDbInfoDao(String dbId);

    public HfDmDb getDmDbInfoInFileDao(String dbId, String fileUuid, String fileVersionId);

/**
 * 数据库管理工具 - 数据表
 */
    /**
     * 获取数据管理工具信息
     *
     * @param
     * @return
     */
    public List<HfDmTable> getDmTableListDao(String createUserId);

    public List<HfDmTable> getDmTableListInFileDao(String createUserId, String fileUuid, String fileVersionId);

    /**
     * 修改表顺序
     *
     * @param dbId
     * @param list 记录表和顺序[(table1:1)]
     * @return
     */
    public Integer orderDmTableDao(String dbId, List<HfDmTable> list);

    public Integer orderDmTableInFileDao(String dbId, List<HfDmTable> list, String fileUuid, String fileVersionId);

    /**
     * 修改库顺序
     *
     * @param list 记录库和顺序[(table1:1)]
     * @return
     */
    public Integer orderDmDbDao(List<HfDmDb> list);

    public Integer orderDmDbInFileDao(List<HfDmDb> list, String fileUuid, String fileVersionId);


    /**
     * 获取数据管理工具信息
     *
     * @param
     * @return
     */
    public HfDmTable getDmTableInfoDao(String tableId);

    public HfDmTable getDmTableInfoInFileDao(String tableId, String fileUuid, String fileVersionId);

    /**
     * 校验重名
     *
     * @param
     * @return
     */
    public Integer checkDmTableNameDao(String tableName, String tableId, String dbId, String userId);

    public Integer checkDmTableNameInFileDao(String tableName, String tableId, String dbId, String fileUuid, String fileVersionId);

    /**
     * 创建数据表
     *
     * @param
     * @return
     */
    public Integer createDmTableDao(HfDmTable hfDmTable);

    public Integer createDmTableInFileDao(HfDmTable hfDmTable);

    /**
     * 删除数据表
     *
     * @param
     * @return
     */
    public Integer delDmTableDao(String tableId);

    public Integer delDmTableInFileDao(String tableId, String fileUuid, String fileVersionId);

    /**
     * 修改数据表
     *
     * @param
     * @return
     */
    public Integer chgDmTableDao(HfDmTable hfDmTable);

    public Integer chgDmTableInFileDao(HfDmTable hfDmTable);

    /**
     * 修改视图条件
     *
     * @param
     * @return
     */
    public Integer updateViewConditionDao(String key, String fatherFilter, String fatherGroup);

    public Integer updateViewConditionInFileDao(String key, String fatherFilter, String fatherGroup, String fileUuid, String fileVersionId);
/**
 * 表头 Columns
 */
    /**
     * 获取表头排序
     *
     * @param
     * @return
     */
    public Integer getNowColumnsOrderDao(String tableId);

    public Integer getNowColumnsOrderInFileDao(String tableId, String fileUuid, String fileVersionId);

    /**
     * 新增表头
     *
     * @param
     * @return
     */
    public Integer addDmTableColumnsDao(HfDmColumns hfDmColumns);

    public Integer addDmTableColumnsInFileDao(HfDmColumns hfDmColumns);

    /**
     * 删除表头
     *
     * @param
     * @return
     */
    public Integer delDmTableColumnsDao(String key, String tableId);

    public Integer delDmTableColumnsInFileDao(String key, String tableId, String fileUuid, String fileVersionId);

    public Integer delDmTableColumns2Dao(@Param("keyList") List keyList, String tableId);

    public Integer delDmTableColumns2InFileDao(@Param("keyList") List keyList, String tableId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 修改表头
     *
     * @param
     * @return
     */
    public Integer chgDmTableColumnsDao(HfDmColumns hfDmColumns);

    public Integer chgDmTableColumnsInFileDao(HfDmColumns hfDmColumns);

    /**
     * 查询表头信息
     *
     * @param
     * @return
     */
    public HfDmColumns getDmTableColumnsInfoDao(String tableId, String columnsId);

    public HfDmColumns getDmTableColumnsInfoInFileDao(String tableId, String columnsId, String fileUuid, String fileVersionId);

    /**
     * 查询表头清单
     *
     * @param
     * @return
     */
    public List<HfDmColumns> getDmTableColumnsListDao(String tableId);

    public List<HfDmColumns> getDmTableColumnsListInFileDao(String tableId, String fileUuid, String fileVersionId);


    /**
     * 新增数据
     *
     * @param
     * @return
     */
    public Integer addDmDataDao(HfDmData hfDmData);

//    public Integer addDmDataInFileDao(HfDmData hfDmData);

    /**
     * 获取当前最大顺序
     *
     * @param
     * @return
     */
    public Integer getNowDataOrderDao(String tableId);

//    public Integer getNowDataOrderInFileDao(String tableId, String fileUuid, String fileVersionId);

    /**
     * 删除数据
     *
     * @param
     * @return
     */
    public Integer delDmDataDao(String tableId, String dataId);

//    public Integer delDmDataInFileDao(String tableId,String dataId, String fileUuid, String fileVersionId);

    /**
     * 变更数据
     *
     * @param
     * @return
     */
    public Integer chgDmDataDao(HfDmData hfDmData);

//    public Integer chgDmDataInFileDao(HfDmData hfDmData);

    /**
     * 变更数据匹配信息
     *
     * @param
     * @return
     */
//    public Integer chgDmDataMatchInfoDao(HfDmData hfDmData);

    /**
     * 获取数据清单
     *
     * @param
     * @return
     */
    public List<HfDmData> getDmDataListDao(String createUserId, String tableId, String paramNameLike, String sql, String sortBySql, Integer limit);

    public List<Map<String, Object>> getDmDataListGroupByDao(String createUserId, String tableId, String paramNameLike, String sql, String sortBySql, String groupBySql, String groupBySqlWithName, String groupBySqlNotNull, String groupByFunctionsSql, Integer limit);

    /**
     * 获取数据dataId清单
     *
     * @param createUserId
     * @param tableId
     * @param paramNameLike
     * @param sql
     * @param sortBySql
     * @param limit
     * @return
     */
    public List<String> getDmDataIdListDao(String createUserId, String tableId, String paramNameLike, String sql, String sortBySql, Integer limit);

//    public List<HfDmData> getDmDataListInFileDao(String createUserId, String tableId, String paramNameLike, String sql, String sortBySql, Integer limit, String fileUuid, String fileVersionId);
//    public List<Map<String,Object>> getDmDataListInFileGroupByDao(String createUserId, String tableId, String paramNameLike, String sql, String sortBySql, String groupBySql,String groupBySqlWithName,String groupBySqlNotNull, String groupByFunctionsSql, Integer limit, String fileUuid, String fileVersionId);

    /**
     * 重新排序
     *
     * @param sql
     * @return
     */
    public Integer orderDmDataDao(String sql);


    /**
     * 获取数据信息
     *
     * @param
     * @return
     */
//    public List<String> getTotalKeysDao(String createUserId, String tableId, String paramNameLike, String sql, String sortBySql, Integer limit, String fileUuid, String fileVersionId);

    /**
     * 获取数据信息
     *
     * @param
     * @return
     */
//    public List<HfDmData> getNoSortDataInFileDao(String tableId,List<String> keyList,Integer limit, String fileUuid, String fileVersionId);


    /**
     * 获取数据信息
     *
     * @param
     * @return
     */
    public Integer getDmDataTotalDao(String createUserId, String tableId);

//    public Integer getDmDataTotalInFileDao(String createUserId, String tableId, String fileUuid, String fileVersionId);

    /**
     * 修改表头顺序
     *
     * @param
     * @return
     */
    public Integer updateDmTableColumnsOrderDao(HfDmColumns hfDmColumns);

    public Integer updateDmTableColumnsOrderInFileDao(HfDmColumns hfDmColumns);

    /**
     * 修改表的分页状态
     *
     * @param
     * @return
     */
    public Integer chgPageStatusDao(String tableId, Boolean showPagination);

    public Integer chgPageStatusInFileDao(String tableId, Boolean showPagination, String fileUuid, String fileVersionId);

    /**
     * 修改表的缩略显示状态
     *
     * @param
     * @return
     */
    public Integer chgEllipsisStatusDao(String tableId, Boolean ellipsis);

    public Integer chgEllipsisStatusInFileDao(String tableId, Boolean ellipsis, String fileUuid, String fileVersionId);

    /**
     * 上传表头
     *
     * @param
     * @return
     */
    public Integer uploadHfDmColumnsDao(@Param("lists") List<HfDmColumns> lists);

    public Integer uploadHfDmColumnsInFileDao(@Param("lists") List<HfDmColumns> lists, String fileUuid, String fileVersionId);

    /**
     * 上传表数据
     *
     * @param
     * @return
     */
    public Integer uploadHfDmDataDao(@Param("lists") List<HfDmData> lists);
//    public Integer uploadHfDmDataInFileDao(@Param("lists") List<HfDmData> lists,String fileUuid, String fileVersionId);

    /**
     * 清空表数据
     *
     * @param
     * @return
     */
    public Integer clearDmDataDao(@Param("tableId") String tableId);
//    public Integer clearDmDataInFileDao(@Param("tableId") String tableId);

    public Integer clearDmColumnsDao(@Param("tableId") String tableId);

    public Integer clearDmColumnsInFileDao(@Param("tableId") String tableId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 从数据管理导入数据表头
     *
     * @param
     * @return
     */
    public Integer importDmTableDao(@Param("databaseId") String databaseId, @Param("tableId") String tableId, @Param("desc") String desc, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newTableId") String newTableId, @Param("fatherTableId") String fatherTableId);

    /**
     * 从数据管理导入数据表头
     *
     * @param
     * @return
     */
    public Integer importDmColumnsDao(@Param("tableId") String tableId, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newTableId") String newTableId);

    /**
     * @param tableId
     * @param userId
     * @param fileUuid
     * @param fileVersionId
     * @param newTableId
     * @return
     */
    public Integer importDmColumnsResetMatchDao(@Param("tableId") String tableId, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newTableId") String newTableId, @Param("matchDbId") String matchDbId);

    /**
     * 获取某表视图
     *
     * @param
     * @return
     */
    public List<String> getDmViewListDao(@Param("tableId") String tableId);

    public List<String> getDmViewListInFileDao(@Param("tableId") String tableId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 清空指定表表头
     *
     * @param tableId
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public Integer clearTableColumnsDao(@Param("tableId") String tableId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);


    /**
     * 从数据管理导入数据
     *
     * @param
     * @return
     */
//    public Integer importDmDataDao(@Param("tableId") String tableId, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newTableId") String newTableId);

    /**
     * 记录报错信息 错误信息
     *
     * @param hfErrLogPojo 错误实体对象
     * @return
     */
    public Integer createErrLogDao(HfErrLogPojo hfErrLogPojo);


    /**
     * 复制表
     *
     * @param tableId      原表id
     * @param newTableId   新表id
     * @param newTableName 新表名
     * @param dbId         目标数据库
     * @param userId       操作人
     * @return
     */
    public Integer copyDmTableDao(@Param("tableId") String tableId, @Param("newTableId") String newTableId, @Param("newTableName") String newTableName, @Param("dbId") String dbId, @Param("userId") String userId);

    public Integer copyDmTableInFileDao(@Param("tableId") String tableId, @Param("newTableId") String newTableId, @Param("newTableName") String newTableName, @Param("dbId") String dbId, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer copyDmColumnsDao(@Param("tableId") String tableId, @Param("newTableId") String newTableId, @Param("userId") String userId);

    public Integer copyDmColumnsInFileDao(@Param("tableId") String tableId, @Param("newTableId") String newTableId, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer copyDmDataDao(@Param("tableId") String tableId, @Param("newTableId") String newTableId, @Param("userId") String userId);
//    public Integer copyDmDataInFileDao(@Param("tableId") String tableId, @Param("newTableId") String newTableId, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 复制视图
     *
     * @param viewId
     * @param newViewId
     * @param newTableId
     * @param dbId
     * @param userId
     * @return
     */
    public Integer copyDmViewDao(@Param("viewId") String viewId, @Param("newViewId") String newViewId, @Param("newTableId") String newTableId, @Param("dbId") String dbId, @Param("userId") String userId);

    public Integer copyDmViewInFileDao(@Param("viewId") String viewId, @Param("newViewId") String newViewId, @Param("newTableId") String newTableId, @Param("dbId") String dbId, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 视频教程增删改查
     *
     * @param hfTeachVideoPojo
     * @return
     */
    public Integer createTeachVideoDao(HfTeachVideoPojo hfTeachVideoPojo);

    public Integer delTeachVideoDao(String videoId);

    public Integer updateTeachVideoDao(HfTeachVideoPojo hfTeachVideoPojo);

    public HfTeachVideoPojo getTeachVideoInfoDao(String videoId);

    public List<HfTeachVideoPojo> getTeachVideoListDao(@Param("paramNameLike") String paramNameLike);

    public Integer likeTeachVideoDao(String videoId);

    public Integer dislikeTeachVideoDao(String videoId);


    /**
     * 数据表关联
     *
     * @param leftTableId  左表
     * @param rightTableId 右表
     * @param leftSql      左查询字段
     * @param rightSql     右查询字段
     * @param onSql        关联条件
     * @return
     */
    public List<HfDmData> tableLeftJoinDao(String leftTableId, String rightTableId, String leftSql, String rightSql, String onSql);

    /**
     * 判断右表关联键的唯一性
     *
     * @param rightTableId
     * @param checkUniqueSqlList
     * @return
     */
    public Integer checkUniqueSqlDao(String rightTableId, List<String> checkUniqueSqlList);

    /**
     * 获取列信息
     *
     * @param tableId       表id
     * @param columnsIdList 列id
     * @return
     */
    public List<HfDmColumns> getColumnsListDao(String tableId, List<String> columnsIdList);

    public List<HfDmColumns> getColumnsListInFileDao(String tableId, List<String> columnsIdList, String fileUuid, String fileVersionId);

    /**
     * 根据tableId获取data_content字段内容
     *
     * @param tableId 表id
     * @return
     */
    public List<HfDmData> getDmDataListByTableIdDao(String tableId, String dataId);
//    public List<HfDmData> getDmDataListByTableIdInFileDao(String tableId, String dataId,String fileUuid,String fileVersionId);

    /**
     * 根据dbId获取表清单
     *
     * @param dbId
     * @return
     */
    public List<HfDmTable> getDmTableListByDbIdDao(String dbId);

    public List<LabelValuePojo> getDmTableListLVByDbIdDao(String dbId);

    public List<LabelValuePojo> getDmTableListLVByDbIdInFileDao(String dbId, String fileUuid, String fileVersionId);


    /**
     * 在db中根据titleList找到tableIdList
     *
     * @param dbId
     * @param titleList
     * @return
     */
    public List<HfDmTable> getRelationDmTableDao(String dbId, List<String> titleList);

    /**
     * 去重获取sourceTableId
     *
     * @param dbId
     * @return
     */
    public List<String> getDmSourceTableIdDistinctDao(String dbId);

    /**
     * 获取带有匹配项的字段
     *
     * @param tableId
     * @return
     */
    public List<HfDmColumns> getDmTableMatchColumnsListDao(String tableId);


    /**
     * 获取表内所有数据
     *
     * @param tableId
     * @return
     */
    public List<String> getDmTableAllDataDao(String tableId);

    /**
     * 获取导入的表信息
     *
     * @param fileUuid      文件
     * @param fileVersionId 版本
     * @return
     */
    public List<String> getImportTableIdListDao(String fileUuid, String fileVersionId);

    /**
     * 获取上述表中带有关联关系的表头
     *
     * @param importTableIdList
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public List<HfDmColumns> getImportTableMatchColumnsDao(List<String> importTableIdList, String fileUuid, String fileVersionId);

    /**
     * 上传模板记录
     * @param hfUploadModelFilePojo
     * @return
     */
    public Integer uploadModelFileDao(HfUploadModelFilePojo hfUploadModelFilePojo);

    /**
     * 上传模板记录
     * @param ffid
     * @return
     */
    public Integer getDownloadingCntDao(String ffid);

    /**
     * 获取文库表文档信息
     */
    public DocFileIndexPojo getLibraryFileInfoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);
}
