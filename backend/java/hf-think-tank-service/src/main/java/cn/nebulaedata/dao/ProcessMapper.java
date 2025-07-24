package cn.nebulaedata.dao;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 贾亦真
 * @date 2021/1/22 09:34
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */
public interface ProcessMapper {

//    /**
//     * 审批流配置
//     *
//     * @param docAppFlowPojo
//     * @return
//     */
//    public Integer insertApprovalFlowDao(DocAppFlowPojo docAppFlowPojo);
//
//    /**
//     * 审批流配置
//     *
//     * @param approvalId
//     * @return
//     */
//    public Integer deleteApprovalFlowDao(String approvalId);
//
//    /**
//     * 审批流配置
//     *
//     * @param docAppLevelPojo
//     * @return
//     */
//    public Integer insertApprovalLevelDao(DocAppLevelPojo docAppLevelPojo);
//
//    /**
//     * 批量新增
//     *
//     * @param docAppLevelPojos
//     * @return
//     */
//    public Integer insertApprovalLevelBeachDao(@Param("lists") List<DocAppLevelPojo> docAppLevelPojos);
//
//    /**
//     * 审批流配置
//     *
//     * @param approvalId
//     * @return
//     */
//    public Integer deleteApprovalLevelDao(String approvalId);
//
//    /**
//     * 新增
//     *
//     * @param docAppUserPojo
//     * @return
//     */
//    public Integer insertApprovalUserDao(DocAppUserPojo docAppUserPojo);
//
//    /**
//     * 批量新增
//     *
//     * @param docAppUserPojo
//     * @return
//     */
//    public Integer insertApprovalUserBeachDao(@Param("lists") List<DocAppUserPojo> docAppUserPojo);
//
//    /**
//     * 审批流配置
//     *
//     * @param approvalId
//     * @return
//     */
//    public Integer deleteApprovalUserDao(String approvalId);
//
//    /**
//     * 审批流
//     *
//     * @param docAppRecoPojo
//     * @return
//     */
//    public Integer insertApprovalRecoDao(DocAppRecoPojo docAppRecoPojo);
//
//    /**
//     * 审批流配置
//     *
//     * @param docAppRecoLevelPojo
//     * @return
//     */
//    public Integer insertApprovalRecordLevelDao(DocAppRecoLevelPojo docAppRecoLevelPojo);
//
//    /**
//     * 批量新增
//     *
//     * @param docAppRecoLevelPojo
//     * @return
//     */
//    public Integer insertApprovalRecordLevelBeachDao(@Param("lists") List<DocAppRecoLevelPojo> docAppRecoLevelPojo);
//
//    /**
//     * 查询审批流
//     *
//     * @param
//     * @return
//     */
//    public List<DocAppFlowPojo> selectApprovalFlowDao(String orgId);
//
//    /**
//     * 查询审批类型
//     *
//     * @return
//     */
//    public List<DocAppTypePojo> selectApprovalTypeDao(String orgId);
//
//    /**
//     * @param orgId
//     * @return
//     */
//    public List<DocAppFlowPojo> selectApprovalFlowBeachDao(@Param("lists") List<String> orgId, @Param("approvalType") String approvalType);
//
//    /**
//     * 获取单条主审批单信息
//     *
//     * @param approvalRecordId
//     * @return
//     */
//    public DocAppRecoPojo selectApprovalRecoSingleDao(String approvalRecordId);
//
//    /**
//     * 获取主审批单对应的各级审批
//     *
//     * @param approvalRecordId
//     * @return
//     */
//    public List<DocAppRecoLevelPojo> selectApprovalRecoLevelDao(String approvalRecordId);
//
//    /**
//     * 获取当前用户我的待办
//     *
//     * @param userId
//     * @return
//     */
//    public List<DocAppRecoPojo> selectCurrUserAppRecoDao(@Param("userId") String userId, @Param("searchLike") String searchLike);
//
//    /**
//     * 获取当前用户我的待办数量
//     *
//     * @param userId
//     * @return
//     */
//    public Integer selectCurrUserAppRecoNumDao(@Param("userId") String userId);
//
//    /**
//     * 审批流流转
//     * 更新状态
//     *
//     * @param approvalRecordId
//     * @param currentLevel
//     * @param approvalStatus
//     * @return
//     */
//    public Integer updateApprovalRecoStatusDao(@Param("approvalRecordId") String approvalRecordId, @Param("currentLevel") Integer currentLevel, @Param("approvalStatus") String approvalStatus);
//
//    /**
//     * 审批流流转-提交用户备注
//     *
//     * @param approvalRecordId
//     * @param approvalRecordLevel
//     * @param approvalUser
//     * @param approvalOpinion
//     * @param approvalRemarks
//     * @return
//     */
//    public Integer updateApprovalRecoLevelStatusDao(@Param("approvalRecordId") String approvalRecordId, @Param("approvalRecordLevel") Integer approvalRecordLevel, @Param("approvalUser") String approvalUser, @Param("approvalOpinion") String approvalOpinion, @Param("approvalRemarks") String approvalRemarks);
//
//    /**
//     * 查询状态
//     *
//     * @param approvalRecordId
//     * @return
//     */
//    public Integer checkAppRecoMaxLevelDao(String approvalRecordId);
//
//    /**
//     * 检查单子当前等级
//     *
//     * @param approvalRecordId
//     * @return
//     */
//    public Integer checkAppRecoLevelDao(String approvalRecordId);
//
//    /**
//     * 检查配置是否存在
//     *
//     * @param approvalType
//     * @return
//     */
//    public Integer checkAppFlowTypeIsExistDao(@Param("approvalType") String approvalType, @Param("orgId") String orgId);
//
//    /**
//     * 更新草稿历史版本
//     *
//     * @param fileUuid
//     * @param versionStatus
//     * @return
//     */
//    public Integer updateDraftVersionStatusDao(@Param("fileUuid") String fileUuid, @Param("versionStatus") String versionStatus);
//
//    /**
//     * 更新历史版本
//     *
//     * @param fileUuid
//     * @param versionStatus
//     * @param fileVersionId
//     * @return
//     */
//    public Integer updateVersionStatusDao(@Param("fileUuid") String fileUuid, @Param("versionStatus") String versionStatus, @Param("fileVersionId") String fileVersionId);
//
//    public Integer updateDraftVersionInfoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("fileVersionName") String fileVersionName, @Param("isDraft") String isDraft, @Param("versionStatus") String versionStatus);
//
//    /**
//     * 更新草稿
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @param isDraft
//     * @param versionStatus
//     * @return
//     */
//    public Integer updateDraftVersionInfoNoNameDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("isDraft") String isDraft, @Param("versionStatus") String versionStatus);
//
//    /**
//     * @param fileUuid
//     * @param sql
//     * @return
//     */
//    public Integer updateFileIndexDraftTurnDao(@Param("fileUuid") String fileUuid, @Param("sql") String sql);
//
//    /**
//     * @param approvalRecordId
//     * @return
//     */
//    public DocAppRecoPojo selectMainAppRecoSingleDao(@Param("approvalRecordId") String approvalRecordId);
//
    /**
     * 更新版本状态
     *
     * @param fileUuid
     * @param versionStatus
     * @param fileVersionId
     * @return
     */
    public Integer updateSingleVersionStatusDao(@Param("fileUuid") String fileUuid, @Param("versionStatus") String versionStatus, @Param("fileVersionId") String fileVersionId, @Param("isDraft") String isDraft);
//
//    /**
//     * @param fileUuid
//     * @return
//     */
//    public List<DocAppRecoPojo> selectAppAnotherRecoDao(@Param("approvalRecordId") String approvalRecordId, @Param("fileUuid") String fileUuid);
//
//    /**
//     * 批量更新单子
//     *
//     * @param currentLevel
//     * @param approvalStatus
//     * @return
//     */
//    public Integer updateApprovalRecoStatusBeachsDao(@Param("currentLevel") Integer currentLevel, @Param("approvalStatus") String approvalStatus, @Param("lists") List<String> approvalRecordIds);
//

}


