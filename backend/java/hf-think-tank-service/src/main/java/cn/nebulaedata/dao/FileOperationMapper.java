package cn.nebulaedata.dao;

import cn.nebulaedata.pojo.*;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2020/12/18 10:55
 * @note
 */
public interface FileOperationMapper {

    public String getStructureDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public String getCreateUserIdDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer insertCollectionDao(@Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("createTime") Date createTime, @Param("collectionType") String collectionType);

    public Integer checkCollectionDao(@Param("userId") String userId, @Param("fileUuid") String fileUuid);

    public Integer deleteCollectionDao(@Param("userId") String userId, @Param("fileUuid") String fileUuid);

    public String getAuditingStatusDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 获取首页我的收藏列表
     *
     * @param userId
     * @return
     */
    public List<DocFileIndexPojo> selectMyCollectFileDao(@Param("collectionType") String collectionType, @Param("userId") String userId, @Param("searchLike") String searchLike);

//    /**
//     * 获取我的收藏列表
//     *
//     * @param userId
//     * @return
//     */
//    public List<DocFileIndexPojo> selectMyCollectFilePageDao(@Param("collectionType") String collectionType, @Param("userId") String userId, @Param("searchLike") String searchLike);


    public Integer insertDraftIndexDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("fileVersionName") String fileVersionName, @Param("createTime") Date createTime, @Param("fileVersionDesc") String fileVersionDesc, @Param("parentsVersionId") String parentsVersionId, @Param("isRootVersion") String isRootVersion, @Param("createUserId") String createUserId, @Param("updateUserId") String updateUserId);

    public Integer insertDraftStructureDao(@Param("fileVersionId") String fileVersionId, @Param("fileUuid") String fileUuid, @Param("fileText") String fileText, @Param("createTime") Date createTime, @Param("orderList") String orderList, @Param("fileCompletePath") String fileCompletePath);

    public Integer insertDraftParamDao(@Param("fileVersionId") String fileVersionId, @Param("fileUuid") String fileUuid, @Param("createTime") Date createTime, @Param("lists") List<String> lists);

    public Integer insertDraftParam2Dao(@Param("fileVersionId") String fileVersionId, @Param("fileUuid") String fileUuid, @Param("createTime") Date createTime, @Param("paramId") String paramId, @Param("paramRelatUuid") String paramRelatUuid);

//    public Integer insertDraftParam3Dao(@Param("fileVersionId") String fileVersionId, @Param("fileUuid") String fileUuid, @Param("createTime") Date createTime, @Param("lists") List<HashMap> lists);

    public Integer insertDraftTaggingDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("createTime") Date createTime, @Param("newFileUuid") String newFileUuid, @Param("newFileVersionId") String newFileVersionId);

    public Integer insertDraftTaggingHisDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newFileUuid") String newFileUuid, @Param("newFileVersionId") String newFileVersionId);

    public Integer deleteDraftIndexDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteDraftStructureDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteDraftParamDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteDraftTaggingDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer insertParamDao(DocParamsPojo docParamsPojo);

    public Integer insertBatchParamDao(@Param("docParamsList") List<DocParamsPojo> docParamsList);

    public List<DocParamsPojo> getParamDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer checkParamNameDao(String paramsName, String paramsClassify, String userId, String companyId, String paramsUseSaturation, String paramsUuid, String fileUuid, String fileVersionId);

    public String getFileNameByIdDao(@Param("fileUuid") String fileUuid);

    public List<LabelValuePojo> getParamTypeDiDao();

    public List<LabelValuePojo> getParamSceneDiDao();

    public List<LabelValuePojo> getParamSourceDiDao();

    public List<LabelValuePojo> getParamGroupDiDao();

    public Integer deleteParamDao(@Param("paramsUuid") String paramsUuid);

    public Integer updateParamDao(DocParamsPojo docParamsPojo);

    public DocParamsPojo getParamInfoDao(String paramsUuid);

    public DocParamsPojo getContentParamInfo2Dao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsUuid") String paramsUuid);

    public List<DocParamsPojo> selectParamsDao(@Param("paramNameLike") String paramNameLike, @Param("limitNum") Integer limitNum, @Param("flag1") String flag1, @Param("flag2") String flag2, @Param("flag3") String flag3, @Param("userId") String userId, @Param("companyId") String companyId, @Param("orderby") String orderby, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateParamListParamsRelatDao(@Param("paramsUuid") String paramsUuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsIdInfile") String paramsIdInfile);

//    public List<DocParamListPojo> selectParamListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer commitParamListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsUuid") String paramsUuid, @Param("paramsText") String paramsText);

    public List<DocFileVerIndexPojo> getVersionListDao(@Param("fileUuid") String fileUuid);

    public List<OutLinePojo> getVersionPhotoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public String getLastVersionIdDao(@Param("fileUuid") String fileUuid);

    public List<HfUpdateInfoPojo> getUpdateInfoDao(@Param("fileUuid") String fileUuid, @Param("fileParentId") String fileParentId);

    public Integer confirmUpdateInfoDao(@Param("updateUuid") String updateUuid, @Param("status") String status, @Param("userId") String userId);

    public List<HfUpdateInfoPojo> getLawUpdateDao(@Param("fileUuid") String fileUuid);

    /**
     * 获取用到的版本信息
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public List<TagPojo> getThisFileLawListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 获取已经生成的通知
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public List<String> getThisFileLawUpdateNoticeDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 应生成的通知
     *
     * @param
     * @param
     * @return
     */
    public List<HfLawPojo> getLawUpdateNoticeDao(@Param("textIdListStr") String textIdListStr, @Param("updateIdListStr") String updateIdListStr);

    /**
     * 生成通知
     *
     * @param
     * @return
     */
    public Integer createUpdateNoticeDao(@Param("lists") List<HfLawPojo> lists);

    /**
     * 获取变更数
     *
     * @param
     * @return
     */
    public Integer getNoticeNumberDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<DocFileIndexPojo> getDerivationListDao(@Param("fileUuid") String fileUuid);

    public List<DocFileIndexPojo> getDerivationPassListDao(@Param("fileUuid") String fileUuid);

    public Integer getDerivationNumberDao(@Param("fileUuid") String fileUuid);

    public List<OutLinePojo> getUseListDao(@Param("fileUuid") String fileUuid);

    public Integer getUseNumberDao(@Param("fileUuid") String fileUuid);

    public DocFileIndexPojo getVersionTreeDao(@Param("fileUuid") String fileUuid);

    public DocFileIndexPojo getVersionCompleteTreeDao(@Param("fileUuid") String fileUuid);

    /**
     * 获取父文件id
     * @param fileUuid
     * @return
     */
    public String getFileParentId(@Param("fileUuid") String fileUuid);

    /**
     * 获取子文件信息
     * @param fileUuid
     * @return
     */
    public List<DocFileIndexPojo> getFileChildDao(@Param("fileUuid") String fileUuid);


//    public DocStructurePojo getDraftDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public String isDraftVersionDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer saveVersionInfo(@Param("newFileUuid") String newFileUuid, @Param("newFileVersionId") String newFileVersionId, @Param("jsonArraySt") String jsonArray);

    public Integer copyDeriveParamDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newFileUuid") String newFileUuid, @Param("newFileVersionId") String newFileVersionId, @Param("createTime") Date createTime);

    public Integer copyDeriveIndexDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newFileUuid") String newFileUuid, @Param("createTime") Date createTime, @Param("updateUserId") String updateUserId, @Param("fileName") String fileName, @Param("fileTypeId") String fileTypeId, @Param("newFileVersionId") String newFileVersionId, @Param("fileUseSceneIdList") String fileUseSceneIdList, @Param("fileUseRangeIdList") String fileUseRangeIdList, @Param("fileLabelIdList") String fileLabelIdList);

    public Integer copyDeriveStructureDao(@Param("fileVersionId") String fileVersionId, @Param("fileUuid") String fileUuid, @Param("newFileUuid") String newFileUuid, @Param("newFileVersionId") String newFileVersionId, @Param("createTime") Date createTime, @Param("fileText") String fileText);

    public Integer copyDeriveVerIndexDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newFileUuid") String newFileUuid, @Param("newFileVersionId") String newFileVersionId, @Param("createTime") Date createTime, @Param("createUserId") String createUserId, @Param("updateUserId") String updateUserId);

    public String getParentFileNameDao(@Param("fileParentId") String fileParentId);

    public String isIndexTableDao(@Param("fileVersionId") String fileVersionId);

    //    public DocStructurePojo getDocStructureDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);
    public Integer setDocStructureDao(@Param("fileText") String fileText, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

//    public DocFileVerIndexPojo getDocInfoDao(@Param("fileUuid") String fileUuid);

    public List<String> getPushFileUuidInfoDao(@Param("fileUuid") String fileUuid);

    public Integer insertUpdateInfoDao(@Param("sql") String sql);

    public DocFileVerIndexPojo getVersionInfoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public DocFileIndexPojo getDocframeFileIndexInfoDao(@Param("fileUuid") String fileUuid);

    public OutLinePojo getOutlineInfoDao(@Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateOutlineNameDao(OutLinePojo outLinePojo);

    public Integer deleteOutlineInfoDao(@Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteOutlineInfoDao1(@Param("outlineIdListStr") String outlineIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteContentDao(@Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteContentDao1(@Param("outlineIdListStr") String outlineIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteContentParamsDao(@Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteContentParamsDao1(@Param("outlineIdListStr") String outlineIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteTagDao1(@Param("outlineIdListStr") String outlineIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteBookmarkDao1(@Param("outlineIdListStr") String outlineIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer deleteJudgment1(@Param("outlineIdListStr") String outlineIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    //    public Integer addContentParamsDao(@Param("sql") String sql);
//    public Integer delContentParamsDao(@Param("sql") String sql);
    public Integer addOutlineDao(OutLinePojo outLinePojo);

    public Integer addContentDao(ContentPojo contentPojo);

    public Integer updateOutlineOrderDao(@Param("outlineOrder") String outlineOrder, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("addNumber") Integer addNumber);

    public String getOutlineIdbyOrderDao(@Param("outlineOrder") String outlineOrder, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateContentDao(ContentPojo contentPojo);

    public Integer copyOutlineDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("shadowVersionId") String shadowVersionId, @Param("createTime") Date createTime);

    public Integer copyContentDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("shadowVersionId") String shadowVersionId, @Param("createTime") Date createTime);

    public Integer copyVersionDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("shadowVersionId") String shadowVersionId, @Param("createTime") Date createTime);

    /**
     * 获取用户信息
     */
    public DocUserPojo getUserInfoDao(@Param("userId") String userId);

    public List<DocFileIndexPojo> getSimilarDocDao(@Param("userId") String userId, @Param("companyId") String companyId, @Param("fileTypeIdListStr") String fileTypeIdListStr);

    public Integer addLabelDao(DocLabelPojo docLabelPojo);

    public Integer checkLabelNameDao(@Param("labelContent") String labelContent, @Param("labelUuid") String labelUuid);

    public Integer delLabelDao(String labelUuid);

    public Integer updateLabelDao(DocLabelPojo docLabelPojo);

    public DocLabelPojo getLabelInfoDao(String labelUuid);

    public List<DocLabelPojo> getLabelListDao(@Param("paramNameLike") String paramNameLike, @Param("limitNum") Integer limitNum, @Param("labelGroupId") String labelGroupId);

    public List<LabelValuePojo> getLabelLVListDao(@Param("labelGroupId") String labelGroupId);

    public List<LabelValuePojo> getAllLabelLVListDao(@Param("labelGroupId") String labelGroupId);

    public Integer addContentParamDao(@Param("uuid") String uuid, @Param("paramsUuid") String paramsUuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("outlineId") String outlineId, @Param("createTime") Date createTime, @Param("showText") String showText, @Param("styleId") String styleId, @Param("remark") String remark, @Param("unit") String unit, @Param("isUnderLine") String isUnderLine);

    public Integer delContentParamDao(@Param("uuid") String uuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<DocParamsPojo> getContentParamDao(DocParamsPojo docParamsPojo);

    // 获取上个版本信息
    public DocFileVerIndexPojo getFatherVersionInfoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<String> getContentAllDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<OutLinePojo> getContentAllDao1(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public OutLinePojo getReplaceOutlineFatherIdDao(@Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<OutLinePojo> getReplaceOutlineList1Dao(@Param("outlineId") String outlineId, @Param("outlineFatherId") String outlineFatherId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<OutLinePojo> getReplaceOutlineList2Dao(@Param("outlineId") String outlineId, @Param("outlineFatherId") String outlineFatherId, @Param("outlineReplaceGroupId") String outlineReplaceGroupId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<OutLinePojo> getOutlineListByIdsDao(@Param("outlineListStr") String outlineListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer confirmReplaceOutlineDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("outlineListStr") String outlineListStr, @Param("outlineReplaceGroupId") String outlineReplaceGroupId, @Param("color") String color, @Param("isNecessary") String isNecessary);

    public Integer confirmReplaceOutlineDelDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("outlineListDelStr") String outlineListDelStr);

    public DocFileVerIndexPojo getVersionDetailDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public DocFileIndexPojo getFilePropertyDao(@Param("fileUuid") String fileUuid);

    public List<LabelValuePojo> getFilePropertyUserListDao(@Param("userId") String userId);

    public List<LabelValuePojo> getFileTypeListDao();

    public List<LabelValuePojo> getFileUseRangeListDao();

    public List<String> getUserNameListDao(@Param("includeUserList") String includeUserList);

    public Integer updateFilePropertyDao(DocFileIndexPojo docFileIndexPojo);

    public Integer updateContentParamShowDao(@Param("showText") String showText, @Param("styleId") String styleId, @Param("isUnderLine") String isUnderLine, @Param("unit") String unit, @Param("remark") String remark, @Param("uuid") String uuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsColumns") String paramsColumns, @Param("matrixDisplay") String matrixDisplay, @Param("matrixMode") String matrixMode);

    public Integer updateContentParamPromptDao(DocParamsPojo docParamsPojo);

    public ContentPojo getContentDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("outlineId") String outlineId);

    public List<DocParamsPojo> getContentInfoByParamDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsUuid") String paramsUuid);

    public Integer updateContentParamsNameDao(DocParamsPojo docParamsPojo);

    public String getContentParamNameDao(@Param("uuid") String uuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer cancalOutlineGroupDao(@Param("outlineReplaceGroupId") String outlineReplaceGroupId);

    //    public List<DocParamsPojo> getContentUsedParamDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);
    public List<DocParamsPojo> getContentUsedParamDao(@Param("fileVersionIdListStr") String fileVersionIdListStr);

    public List<OutLinePojo> getOutlineListByFatherIdDao(@Param("outlineFatherId") String outlineFatherId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<OutLinePojo> getOutlineListByFatherIdDao1(@Param("outlineIdListStr") String outlineIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<String> getOutlineListByOrderDao(@Param("newOutlineOrder") String newOutlineOrder, @Param("outlineOrder") String outlineOrder, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<String> getOutlineListByOrderDao1(@Param("newOutlineOrder") String newOutlineOrder, @Param("targetOutlineOrder") String targetOutlineOrder, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateOutlineOrderDao1(@Param("childrIdListStr") String childrIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("addNumber") Integer addNumber);

    public Integer updateOutlineFatherId(@Param("fatherId") String fatherId, @Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer checkParamGroupNameDao(@Param("groupName") String groupName, @Param("groupId") String groupId);

    public Integer getParamGroupCntDao();

    public Integer addParamGroupDao(DocParamsGroupPojo docParamsGroupPojo);

    public Integer delParamGroupDao(DocParamsGroupPojo docParamsGroupPojo);

    public Integer updateParamGroupDao(DocParamsGroupPojo docParamsGroupPojo);

    public DocParamsGroupPojo getParamGroupInfoDao(String groupId);

    public List<DocParamsPojo> getParamGroupUseInfoDao(String groupId);

    public List<DocParamsPojo> getParamGroupUseInfoDao2(String groupId);

    /**
     * 获取大纲树
     *
     * @return
     */
    public List<OutLinePojo> getOutlineListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<OutLinePojo> getOutlineAndContentListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 查询本文件的历史版本
     *
     * @param fileUuid
     * @return
     */
    public List<LabelValuePojo> getCompareVersionListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 查询本文件的父文件
     *
     * @param fileUuid
     * @return
     */
    public LabelValuePojo getCompareVersionList2Dao(@Param("fileUuid") String fileUuid);

    public Integer updateOutlineInfoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("outlineId") String outlineId, @Param("outlineOrder") String outlineOrder, @Param("outlineFatherId") String outlineFatherId);

    public List<DocParamsGroupPojo> getParamGroupListDao(@Param("paramNameLike") String paramNameLike, @Param("limitNum") Integer limitNum);

    /**
     * 参数分组排序
     *
     * @param sql
     * @return
     */
    public Integer orderParamGroupDao(@Param("sql") String sql);

    public Integer updateParamGroupIdDao(@Param("paramsUuid") String paramsUuid, @Param("paramsGroupId") String paramsGroupId);

    public Integer updateParamGroupIdDao2(@Param("uuid") String uuid, @Param("paramsGroupId") String paramsGroupId);

    public List<LabelValuePojo> getParamTypeStyleDiDao(@Param("paramsTypeId") String paramsTypeId);

    public List<DocParamsTypeStylePojo> getParamTypeStyleDi2Dao();

    public Integer deleteParamDailyDao(@Param("yesterday") String yesterday);

    public Integer deleteTagDailyDao(@Param("yesterday") String yesterday);

    public Integer deleteBookmarkDailyDao(@Param("yesterday") String yesterday);

    public Integer deleteAnnexDailyDao(@Param("yesterday") String yesterday);

    /**
     * 获取过期数据表清单
     *
     * @param
     * @return
     */
    public List<String> getDelDmTableListDao(@Param("yesterday") String yesterday);

    /**
     * 清除过期数据管理数据-数据表
     *
     * @param tableList
     * @return
     */
    public Integer clearDmTableDailyDao(@Param("tableList") List<String> tableList);

    /**
     * 清除过期数据管理数据-数据表表头
     *
     * @param tableList
     * @return
     */
    public Integer clearDmColumnsDailyDao(@Param("tableList") List<String> tableList);

    /**
     * 清除过期数据管理数据-数据表数据
     *
     * @param tableList
     * @return
     */
    public Integer clearDmDataDailyDao(@Param("tableList") List<String> tableList);

    public List<String> getContentAllParamNameDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsUuid") String paramsUuid);

    public List<String> getDeledParamDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateContentParamOutlineIdDao(@Param("uuid") String uuid, @Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer getSubsidiaryCntDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer newSubsidiaryFileIndexDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newSubsidiaryFileIndexDao2(DocFileIndexPojo docFileIndexPojo);

    public Integer newSubsidiaryFileIndexDao3(DocFileIndexPojo docFileIndexPojo);

    public Integer newSubsidiaryFileVersionDao(DocFileIndexPojo docFileIndexPojo);

    public List<OutLinePojo> getNewSubsidiaryFileOutlineDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newSubsidiaryFileOutlineDao(String sql);

    public List<ContentPojo> getSubsidiaryFileContentDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newSubsidiaryFileContentDao(String sql);

    //    public List<DocParamsPojo> getSubsidiaryContentParamDao(DocFileIndexPojo docFileIndexPojo);
    public Integer newSubsidiaryContentParamDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newSubsidiaryContentTagDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newSubsidiaryContentDmDbDao(DocFileIndexPojo docFileIndexPojo);
    public Integer newSubsidiaryContentDmTableDao(DocFileIndexPojo docFileIndexPojo);
    public Integer newSubsidiaryContentDmColumnsDao(DocFileIndexPojo docFileIndexPojo);
//    public Integer newSubsidiaryContentDmDataDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newSubsidiaryContentDbDao(DocFileIndexPojo docFileIndexPojo);

    public DocFileIndexPojo getDocAllInfoDao(String fileUuid);

    public List<DocFileIndexPojo> getSubsidiaryFileListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<DocFileIndexPojo> getSubsidiaryFileListDao2(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer delSubsidiaryFileIndexDao(String fileUuid);

    public Integer delSubsidiaryFileVersionDao(String fileUuid);

    public Integer delSubsidiaryFileOutlineDao(String fileUuid);

    public Integer delSubsidiaryFileContentDao(String fileUuid);

    public Integer delSubsidiaryContentParamDao(String fileUuid);

    public Integer delSubsidiaryContentTagDao(String fileUuid);

    public Integer delSubsidiaryContentBookmarkDao(String fileUuid);

    public Integer updateSubsidiaryFileNameDao(String fileUuid, String fileName, String templateTypeId);

    public DocParamsPojo getContentParamInfoDao(@Param("uuid") String uuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateAllContentParamShowDao(@Param("showText") String showText, @Param("styleId") String styleId, @Param("isUnderLine") String isUnderLine, @Param("unit") String unit, @Param("remark") String remark, @Param("paramsUuid") String paramsUuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsColumns") String paramsColumns, @Param("matrixDisplay") String matrixDisplay, @Param("matrixMode") String matrixMode);

    public List<String> getAllOutlineIdByParamDao(@Param("paramsUuid") String paramsUuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<ContentPojo> getContentByIdDao(@Param("allOutlineIdListStr") String allOutlineIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer addWordsDao(DocWordsPojo docWordsPojo);

    public Integer checkWordsNameDao(@Param("wordsName") String wordsName, @Param("wordsUuid") String wordsUuid);

    public Integer delWordsDao(@Param("wordsUuid") String wordsUuid);

    public Integer delTagWordsDao(@Param("wordsUuid") String wordsUuid);

    public Integer updateWordsDao(@Param("wordsUuid") String wordsUuid, @Param("wordsName") String wordsName, @Param("wordsDesc") String wordsDesc);

    public DocWordsPojo getWordsInfoDao(@Param("wordsUuid") String wordsUuid);

    public List<DocWordsPojo> getWordsListDao(@Param("paramNameLike") String paramNameLike, @Param("limitNum") Integer limitNum);

    public List<LabelValuePojo> getWordsLabelValueListDao();

    public Integer addTipsDao(@Param("createUserId") String createUserId, @Param("tipsName") String tipsName, @Param("tipsID") String tipsID, @Param("tipsDesc") String tipsDesc, @Param("createTime") Date createTime);

    public Integer delTipsDao(@Param("tipsUuid") String tipsUuid);

    public Integer updateTipsDao(@Param("tipsUuid") String tipsUuid, @Param("tipsName") String tipsName, @Param("tipsDesc") String tipsDesc);

    public List<DocTipsPojo> getTipsDao(@Param("paramNameLike") String paramNameLike, @Param("limitNum") Integer limitNum);

    public Integer addTagDao(TagPojo tagPojo);

    public Integer addContentTagDao(TagPojo tagPojo);

    public Integer delContentTagDao(@Param("tagId") String tagId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer delTagDao(@Param("tagId") String tagId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateTagDao(TagPojo tagPojo);

    public TagPojo getTagInfoDao(@Param("tagId") String tagId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateUseIsDelDao(@Param("wordsUuid") String wordsUuid, @Param("bookmarkUuid") String bookmarkUuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newUuid") String newUuid);

    public List<String> getUseIsDelOutlineIdListDao(@Param("wordsUuid") String wordsUuid, @Param("bookmarkUuid") String bookmarkUuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<String> getUseIsDelTagIdListDao(@Param("wordsUuid") String wordsUuid, @Param("bookmarkUuid") String bookmarkUuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<TagPojo> getTagListDao(@Param("paramNameLike") String paramNameLike, @Param("limitNum") Integer limitNum);

    public List<TagPojo> getContentTagDao(TagPojo tagPojo);

    public List<String> getDeledTagDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<String> getAliveTagDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateContentTagOutlineIdDao(@Param("tagId") String tagId, @Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer commitLawDao(TagPojo tagPojo);

    public Integer addBookmarkDao(BookmarkPojo bookmarkPojo);

    public Integer delBookmarkDao(BookmarkPojo bookmarkPojo);

    public List<String> getDeledBookmarkDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateContentBookmarkOutlineIdDao(@Param("bookmarkUuid") String bookmarkUuid, @Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateTagBookmarkOutlineIdDao(@Param("bookmarkUuid") String bookmarkUuid, @Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer addContentBookmarkDao(BookmarkPojo bookmarkPojo);

    public Integer delContentBookmarkDao(@Param("bookmarkUuid") String bookmarkUuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer delTagBookmarkDao(@Param("bookmarkUuid") String bookmarkUuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateBookmarkDao(BookmarkPojo bookmarkPojo);

    public BookmarkPojo getBookmarkInfoDao(BookmarkPojo bookmarkPojo);

    public List<BookmarkPojo> getBookmarkListDao(@Param("paramNameLike") String paramNameLike, @Param("limitNum") Integer limitNum, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("outlineId") String outlineId);

    public List<LabelValuePojo> getBookmarkLabelValueListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer checkBookmarkNameDao(@Param("bookmarkName") String bookmarkName, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("bookmarkUuid") String bookmarkUuid);

    public List<String> getFileLabelNameListDao(String fileLabelIdsStr);

    public Integer addDeriveFileIndexDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveFileIndexDao1(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveFileIndexDao2(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveFileIndexDao3(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveFileIndexDao4(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveFileVersionDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveFileVersionDao3(DocFileIndexPojo docFileIndexPojo);

    public List<OutLinePojo> getDeriveFileOutlineDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveFileOutlineDao(String sql);

    public Integer addDeriveFileOutline2Dao(String sql);

    public Integer newShadowVersion2Dao(String sql);

    public Integer addDeriveFileContentDao(String sql);

    public Integer addDeriveContentParamDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveContentParamDao2(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveContentTagDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveContentBookmarkDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveContentDmDbDao(DocFileIndexPojo docFileIndexPojo);
    public Integer addDeriveContentDmTableDao(DocFileIndexPojo docFileIndexPojo);
    public Integer addDeriveContentDmColumnsDao(DocFileIndexPojo docFileIndexPojo);
//    public Integer addDeriveContentDmDataDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addDeriveContentDbDao(DocFileIndexPojo docFileIndexPojo);

    public List<ContentPojo> getDeriveFileContentDao(DocFileIndexPojo docFileIndexPojo);

    public List<DocFileIndexPojo> getAllSubsidiaryFileListDao(DocFileIndexPojo docFileIndexPojo);

    public Integer checkLabelGroupNameDao(@Param("labelGroupName") String labelGroupName, @Param("labelGroupId") String labelGroupId);

    public Integer addLabelGroupDao(DocLabelPojo docLabelPojo);

    public Integer checkLabelGroupUseDao(String labelGroupId);

    public Integer delLabelGroupDao(String labelGroupId);

    public Integer delLabelGroupIdDao(String labelGroupId);

    public List<DocLabelPojo> getLabelGroupListDao(@Param("paramNameLike") String paramNameLike, @Param("limitNum") Integer limitNum);

    public List<LabelValuePojo> getLabelGroupLVListDao();


    public Integer updateVersionFileIndexDao(DocFileIndexPojo docFileIndexPojo);

    public Integer updateVersionFileIndexDao1(DocFileIndexPojo docFileIndexPojo);

    public Integer updateVersionFileIndexDao2(DocFileIndexPojo docFileIndexPojo);

    public Integer updateVersionFileIndexDao3(DocFileIndexPojo docFileIndexPojo);

    public Integer updateVersionFileIndexDao4(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("compareInfo") String compareInfo, @Param("createTime") Date createTime);

    public Integer updateFileVersionNameDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("fileVersionName") String fileVersionName);

    public Integer addVersionFileVersionDao(DocFileIndexPojo docFileIndexPojo);

    public List<OutLinePojo> getVersionFileOutlineDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addVersionFileOutlineDao(String sql);

    public Integer saveWriteParamDao(String sql);

    public Integer addVersionFileContentDao(String sql);

    public Integer addVersionContentParamDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addVersionContentParamDao2(DocFileIndexPojo docFileIndexPojo);

    public Integer addVersionContentTagDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addVersionContentBookmarkDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addVersionContentDmDbDao(DocFileIndexPojo docFileIndexPojo);
    public Integer addVersionContentDmTableDao(DocFileIndexPojo docFileIndexPojo);
    public Integer addVersionContentDmColumnsDao(DocFileIndexPojo docFileIndexPojo);
//    public Integer addVersionContentDmDataDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addVersionContentDbDao(DocFileIndexPojo docFileIndexPojo);

    public List<ContentPojo> getVersionFileContentDao(DocFileIndexPojo docFileIndexPojo);

    public DocFileIndexPojo getVersionAllInfoDao(@Param("fileUuid") String fileUuid);

    public DocFileIndexPojo getVersionAllInfoDao1(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer addShadowFileIndexDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addShadowFileVersionDao(DocFileIndexPojo docFileIndexPojo);

    public List<OutLinePojo> getShadowFileOutlineDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addShadowFileOutlineDao(String sql);

    public Integer addShadowFileContentDao(String sql);

    public Integer addShadowContentParamDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addShadowContentTagDao(DocFileIndexPojo docFileIndexPojo);

    public Integer addShadowContentBookmarkDao(DocFileIndexPojo docFileIndexPojo);

    public DocFileVerIndexPojo getDraftVersionIdDao(String fileUuid);

    public List<DocFileIndexPojo> getFileListByLabelDao(String labelUuid);

    public Integer updateFileLabelDao(DocFileIndexPojo docFileIndexPojo);

    public List<DocFileIndexPojo> getDocBySearchDao(String searchLike, String fileTypeIdListStr);


    public List<DocParamsPojo> getContentAllParamDao(DocParamsPojo docParamsPojo);

    public List<DocParamsPojo> getContentAllParamInfoDao(DocParamsPojo docParamsPojo);

    public List<DocParamsPojo> getContentAllParamInfoDao1(String fileUuid, String fileVersionId);

    // 填写参数 (文本)
    public Integer writeParamDao(@Param("paramsText") String paramsText, @Param("writeUserId") String writeUserId, @Param("sql") String sql);

    // 填写参数 (选择)
    public Integer writeParamSelectDao(@Param("paramsText") String paramsText, @Param("writeUserId") String writeUserId, @Param("paramsChoose") String paramsChoose, @Param("sql") String sql);

    // 填写参数 (时间)
    public Integer writeParamTimeDao(@Param("paramsText") String paramsText, @Param("uuid") String uuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    // 非必填参数忽略
    public Integer updateContentParamIgnoreDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId,@Param("paramsUuid") String paramsUuid,@Param("writeUserId") String writeUserId,@Param("isIgnore") String isIgnore);

    // 将参数更新至文内
    public Integer writeContentParamDao(DocParamsPojo paramsPojo);

    // 标记参数填写内容是否为ai生成
    public Integer updateIsAiContentDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId,@Param("paramsUuid") String paramsUuid,@Param("isAiContent") String isAiContent);

    public Integer resetParamDao(DocParamsPojo docParamsPojo, String sql);

    // 更新矩阵参数展示效果
    public Integer updateParamMatrixDisplayDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("uuid") String uuid, @Param("matrixDisplay") String matrixDisplay);

    // 获取附属文件
    public List<DocFileIndexPojo> getAllSubsidiaryFileListDao2(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<DocFileIndexPojo> getAllSubsidiaryFileListDao3(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public Integer updateVersionFileFinishDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    // 从招标文件信息附表获取招标文件左侧项目信息
    public HfBidDocumentInfo getBidLeftInfoDao(@Param("fileUuid") String fileUuid);

    public Integer updateBidLeftInfoDao(@Param("fileUuid") String fileUuid, @Param("biddingTypeStr") String biddingTypeStr, @Param("biddingStyleStr") String biddingStyleStr, @Param("judgmentMethodStr") String judgmentMethodStr);

    // 根据招标文件的fileUuid获取项目信息
    public HfBidDocumentInfo getbidDocumentInfoDao(@Param("fileUuid") String fileUuid);

    // 根据项目和公司id获取秘钥
    public String getBidKeyDao(@Param("projectId") String projectId, @Param("companyId") String companyId);

    public Integer addBidKeyDao(@Param("projectId") String projectId, @Param("companyId") String companyId, @Param("bidKey") String bidKey, @Param("createUserId") String createUserId);

    public List<LabelValuePojo> getTemplateTypeDiDao(@Param("templateTypeId") String templateTypeId);

    // 评标办法是否重名
    public Integer checkJudgmentMethodNameDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public Integer newJudgmentMethodDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public Integer delJudgmentMethodDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public Integer updateJudgmentMethodDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public HfJudgmentDetailPojo getJudgmentMethodInfoDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public List<LabelValuePojo> getJudgmentMethodOutlineDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public List<HfJudgmentDetailPojo> getJudgmentMethodListDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    // 评标模块是否重名
    public Integer checkJudgmentModuleNameDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public Integer newJudgmentModuleDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public Integer delJudgmentModuleDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public Integer updateJudgmentModuleDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public HfJudgmentDetailPojo getJudgmentModuleInfoDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public List<HfJudgmentDetailPojo> getJudgmentModuleListDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    // 评标细则
    public Integer newJudgmentDetailDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public Integer delJudgmentDetailDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public Integer updateJudgmentDetailDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public HfJudgmentDetailPojo getJudgmentDetailInfoDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public List<HfJudgmentDetailPojo> getJudgmentDetailListDao(HfJudgmentDetailPojo hfJudgmentDetailPojo);

    public List<LabelValuePojo> getTendParamsListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    //
    public List<BookmarkPojo> getBookmarkListDao2(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<DocframeParamsUnitDiPojo> getUnitDiDao();

    public List<DocframeParamsUnitDiPojo> getUnitDiDao(@Param("nodeIds") List<String> nodeIds);

    public List<LabelValuePojo> getUnitDiLVDao();

    public Integer setParamNullDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsUuid") String paramsUuid);

    public DocParamsPojo getParamDetailDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("uuid") String uuid);

    public List<LabelValuePojo> getParamUnitListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsUuid") String paramsUuid);

    public List<DocParamsPojo> getUuidListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsUuid") String paramsUuid);

    public Integer insertOutlineDao(String sql);

    public List<ContentPojo> getcontentListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("outlineIdListStr") String outlineIdListStr);

    public Integer insertContentDao(String sql);

    public List<DocParamsPojo> getParamListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("outlineIdListStr") String outlineIdListStr);

    public Integer insertParamsDao(String sql);

    public Integer newDeliverFileDao(HfProjectInboxPojo hfProjectInboxPojo);

    public List<HfProjectInboxPojo> getDeliverRecDao(@Param("fileUuid") String fileUuid, @Param("userId") String userId);


    /**
     * 根据文件获取所在项目信息
     */
    public ProjectPojo getProjectInfoByFileDao(@Param("fileUuid") String fileUuid);


    /**
     * 根据文件获取所在项目信息
     */
    public ProjectPojo getBatchInfoByFileDao(@Param("fileUuid") String fileUuid);

    /**
     * 获取项目下的结构化文件列表
     */
    public List<LabelValuePojo> getProjectMateFileDao(@Param("projectId") String projectId);

    /**
     * 获取项目下的结构化文件列表
     */
    public List<HfSupToolFormPojo> getToolListDao(@Param("toolIdListStr") String toolIdListStr);

    /**
     * 将表单数据写入参数
     */
    public Integer useToolDataDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("dataSourceStr") String dataSourceStr, @Param("paramValue") String paramValue);

    public Integer useToolDataDao2(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("ZH") String ZH, @Param("paramValue") String paramValue);

    public List<String> getToolDataDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("dataSourceStr") String dataSourceStr);

    public List<String> getToolDataDao2(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("ZH") String ZH);


    /**
     * 获取文件模板的维表
     */
    public List<LabelValuePojo> getFileTypeMBDiDao();

    /**
     * 新增参数填写历史记录
     */
    public Integer addParamChangeHistoryDao(HfParamChangeHistoryPojo hfParamChangeHistoryPojo);

    /**
     * 获取参数填写历史记录
     */
//    public List<HfParamChangeHistoryPojo> getParamChangeHistoryDao(HfParamChangeHistoryPojo hfParamChangeHistoryPojo);
    public List<DocParamsPojo> getParamChangeHistoryDao(@Param("lists") List<DocFileIndexPojo> lists, @Param("paramsUuid") String paramsUuid);

    /**
     * 清除参数填写历史记录
     */
    public Integer clearParamChangeHistoryDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("paramsUuid") String paramsUuid);

    /**
     * 获取相关联文件
     */
    public List<DocFileIndexPojo> getRelationFileListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);


    /**
     * 修改段落必选或非必选
     */
    public Integer updateOutlineNecessaryDao(@Param("outlineIdListStr") String outlineIdListStr, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("isNecessary") String isNecessary);

    /**
     * 文库新增一条记录
     */
    public Integer newHfFileLibraryDao(HfFileLibraryPojo hfFileLibraryPojo);

    /**
     * 文库新增一条记录
     */
    public Integer changeFileFinishStatusDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("isFinish") String isFinish);

    /**
     * 获取主文件versionId
     */
    public String getRealFileVersionIdDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 获取批注
     */
    public String getAnnotateDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 更新批注
     */
    public Integer updateAnnotateDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("annotateListStr") String annotateListStr);

    /**
     * 获取变更和批注
     */
    public DocFileVerIndexPojo getCompareInfoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 确认评审要素总体方案
     */
    public Integer confirmAssessTotalPlanDao(HfContentAssessPojo hfContentAssessPojo);

    /**
     * 清空总体方案
     */
    public Integer delAssessTotalPlanDao(@Param("fileUuid") String fileUuid);

    public Integer delAssessTotalPlanElementDao(@Param("fileUuid") String fileUuid);

    /**
     * 自动确认评审要素总体方案元素
     */
    public Integer copyAssessElementDao(@Param("assessId") String assessId, @Param("fileUuid") String fileUuid, @Param("createUserId") String createUserId, @Param("tabUuid") String tabUuid);

    /**
     * 获取备选方案下拉表
     */
    public List<LabelValuePojo> getAssessPlanListDao(@Param("assessMethod") String assessMethod);

    /**
     * 获取备选方案下拉表
     */
    public HfContentAssessPojo getContentAssessInfoDao(@Param("fileUuid") String fileUuid);

    /**
     * 查询具体方案内容
     */
    public List<HfContentAssessElementPojo> getAssessElementListDao(@Param("assessId") String assessId, @Param("fileUuid") String fileUuid, @Param("elementType") String elementType, @Param("paramNameLike") String paramNameLike);

    /**
     * 删除方案中某个元素
     */
    public HfContentAssessElementPojo getAssessElementInfoDao(@Param("elementId") String elementId, @Param("assessId") String assessId, @Param("fileUuid") String fileUuid);


    /**
     * 编辑方案中某个元素
     */
    public Integer updateAssessElementDao(HfContentAssessElementPojo hfContentAssessElementPojo);

    /**
     * 删除方案中某个元素
     */
    public Integer delAssessElementDao(@Param("elementId") String elementId, @Param("assessId") String assessId, @Param("fileUuid") String fileUuid);

    /**
     * 调整order后的元素顺序
     */
    public Integer updateAssessElementOrderDao(@Param("assessId") String assessId, @Param("fileUuid") String fileUuid, @Param("order") String order);

    /**
     * 调整order后的元素顺序
     */
    public Integer updateAssessSingleElementOrderDao(@Param("elementId") String elementId, @Param("assessId") String assessId, @Param("fileUuid") String fileUuid, @Param("order") String order);

    /**
     * 查询评审要素数量
     */
    public Integer getContentAssessElementListCntDao(@Param("fileUuid") String fileUuid, @Param("assessId") String assessId, @Param("elementType") String elementType);

    /**
     * 查询评审类型数量
     */
    public Integer getContentAssessTypeCntDao(@Param("fileUuid") String fileUuid, @Param("assessId") String assessId, @Param("elementType") String elementType);

    /**
     * 查询评审类型的顺序字段
     */
    public String getContentElementTypeOrderDao(@Param("fileUuid") String fileUuid, @Param("assessId") String assessId, @Param("elementType") String elementType);

    /**
     * 在方案中新增元素
     */
    public Integer addAssessElementDao(HfContentAssessElementPojo hfContentAssessElementPojo);

    /**
     * 获取文件中引用的所有评审元素
     */
    public List<HfContentAssessElementPojo> getContentAssessElementListDao(@Param("fileUuid") String fileUuid, @Param("assessId") String assessId);


    /**
     * 获取文件中引用的评审类型顺序
     */
    public List<HfContentAssessElementPojo> getContentElementTypeOrderListDao(@Param("fileUuid") String fileUuid);

    /**
     * 获取文件中已使用的参数列表
     */
    public List<LabelValuePojo> getContentParamLabelValueDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 获取文件(及附属)中已使用的参数列表
     */
    public List<LabelValuePojo> getContentAllParamLabelValueDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    public List<LabelValuePojo> getContentAllParamLabelValueDao2(@Param("lists") List<DocFileIndexPojo> lists);


    /**
     * 获取投标文件模型信息
     */
    public List<DocFileIndexPojo> getTendModelFileInfoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);


    public Integer newQTDocumentIndexDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newQTDocumentVersionDao(DocFileIndexPojo docFileIndexPojo);

    public Integer newQTDocumentOutLinePojoDao(OutLinePojo outLinePojo);

    public Integer newQTDocumentContentPojoDao(ContentPojo contentPojo);

    /**
     * 获取文内用于评审的标注
     *
     * @param hfContentAssessPojo
     * @return
     */
    public List<LabelValuePojo> getContentAssessTagDao(HfContentAssessPojo hfContentAssessPojo);


    /**
     * 获取帮助文档搜索
     */
    public Integer addDeriveFileAssessDao(@Param("fileUuid") String fileUuid, @Param("fileUuid2") String fileUuid2, @Param("createUserId") String createUserId);

    /**
     * 获取帮助文档搜索
     */
    public Integer addDeriveFileAssessElementDao(@Param("fileUuid") String fileUuid, @Param("fileUuid2") String fileUuid2, @Param("createUserId") String createUserId);

    /**
     * 记录辅助工具填写历史
     */
    public Integer addSupTableUseHistoryDao(HfSupTableUseHistoryPojo hfSupTableUseHistory);

    /**
     * 获取辅助工具填写历史清单
     */
    public List<HfSupTableUseHistoryPojo> getSupTableHistoryListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 评审步骤模板配置
     */
    /**
     * 复制评审元素到快速模板中
     */
    public Integer copyAssessElement2Dao(@Param("assessId") String assessId, @Param("fastId") String fastId, @Param("createUserId") String createUserId, @Param("tabUuid") String tabUuid);

    /**
     * 确认评审要素总体方案
     */
    public Integer confirmAssessTotalPlan2Dao(HfFastAssessPojo hfFastAssessPojo);

    /**
     * 查询评审要素总体方案
     */
    public HfFastAssessPojo getFastAssessDao(String fastId);

    /**
     * 删除评审要素总体方案元素
     */
    public Integer delFastAssessElementDao(String fastId);

    /**
     * 删除评审要素总体方案
     */
    public Integer delFastAssessDao(String fastId);

    /**
     * 获取评审要素总体方案信息
     */
    public HfFastAssessPojo getFastAssessInfoDao(String fastId);

    /**
     * 获取评审要素总体方案列表
     */
    public List<HfFastAssessPojo> getFastAssessListDao(String paramNameLike);

    public List<LabelValuePojo> getFastAssessListLVDao(String paramNameLike);

    /**
     * 获取评审要素总体方案引用的所有评审元素
     */
    public List<HfFastAssessElementPojo> getFastAssessElementListDao(@Param("fastId") String fastId, @Param("assessId") String assessId);

    /**
     * 接口获取摘编后入库 用来对比法律法规变更
     */
    public List<HfLawPojo> getLawDao();

    /**
     * 接口获取摘编后入库 用来对比法律法规变更
     */
    public Integer insertLawDao(@Param("lists") List<HfLawPojo> lists);

    public Integer deleteLawDao(@Param("lists") List<HfLawPojo> lists);

    public Integer updateLawDao(HfLawPojo hfLawPojo);

    /**
     * 法律法规变更信息保存 (包括 增 删 改)
     */
    public Integer insertLawUpdateDao(@Param("lists") List<HfLawPojo> lists);

    /**
     * 获取变化清单
     */
    public List<HfLawPojo> getLawChangeListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 已读
     */
    public Integer readLawChangeDao(@Param("noticeUuid") String noticeUuid);

    public Integer readLawAllChangeDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);  // 全部已读

    /**
     * 新增文内使用数据表
     *
     * @param hfDmContentUseTablePojo
     * @return
     */
    public Integer addContentUseTableDao(HfDmContentUseTablePojo hfDmContentUseTablePojo);


    /**
     * 删除文内使用数据表
     *
     * @param uuid
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public Integer delContentUseTableDao(@Param("uuid") String uuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 获取已经删除的文内使用数据表
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public List<String> getDeledContentUseTableDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 恢复已经删除的文内使用数据表
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public Integer updateContentUseTableOutlineIdDao(@Param("uuid") String uuid, @Param("outlineId") String outlineId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 获取文内使用数据表清单
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public List<HfDmContentUseTablePojo> getContentUseTableListDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);
    /**
     * 删除文内使用数据表清单
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public Integer delContentDbDao(@Param("uuid") String uuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);
    /**
     * 获取文内使用数据表信息
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public HfDmContentUseTablePojo getContentUseTableInfoDao(@Param("uuid") String uuid, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);
    /**
     * 更新文内使用数据表清单
     *
     * @param hfDmContentUseTablePojo
     * @return
     */
    public Integer updateContentUseTableDao(HfDmContentUseTablePojo hfDmContentUseTablePojo);
    /**
     * 校验数据表重名
     *
     * @param hfDmContentUseTablePojo
     * @return
     */
    public Integer checkContentUseTableNameDao(HfDmContentUseTablePojo hfDmContentUseTablePojo);

    /**
     * 获取文内数据表信息
     *
     * @param
     * @return
     */
    public List<HfDmContentColumns> getDmColumnsInFileDao(@Param("fields") List fields,@Param("tableId") String tableId,@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);


    /**
     * 获取文内数据表信息
     *
     * @param
     * @return
     */
//    public List<HfDmContentData> getDmDataInFileDao(@Param("rows") List rows,@Param("tableId") String tableId,@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);


    /**
     * 获取tableId生成的上卷表清单
     *
     * @param
     * @return
     */
    public List<LabelValuePojo> getRollTableListDao(@Param("tableId") String tableId,@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);


    /**
     * 根据fileTypeId获取fileTypeName
     *
     * @param
     * @return
     */
    public String getFileTypeNameDao(@Param("fileTypeId") String fileTypeId);

    public List<DocParamsPojo> getDocParamListDao(@Param("fileUuid") String fileUuid);


}
