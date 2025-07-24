package cn.nebulaedata.service;

import cn.nebulaedata.form.DocParamsForm;
import cn.nebulaedata.pojo.*;
import cn.nebulaedata.vo.TResponseVo;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/8/12 15:11
 * @note
 */
public interface FileOperationService {

    /**
     * 新增收藏
     *
     * @param userId
     * @param fileUuid
     * @return
     * @throws Exception
     */
    public TResponseVo addCollectionService(String userId, String fileUuid, String collectionType) throws Exception;

    /**
     * 取消收藏
     *
     * @param userId
     * @param fileUuid
     * @return
     * @throws Exception
     */
    public TResponseVo delCollectionService(String userId, String fileUuid) throws Exception;

    /**
     * 我的收藏
     *
     * @param isPaged
     * @param userId
     * @return
     * @throws Exception
     */
    public TResponseVo myCollectionService(String collectionType, Integer pageNum, Integer pageSize, String isPaged, String userId, String searchLike) throws Exception;

    /**
     * 读取版本记录列表
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    public TResponseVo getVersionListService(String fileUuid) throws Exception;

    /**
     * 读取版本详细信息
     *
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    public TResponseVo getVersionDetailService(String fileUuid, String fileVersionId) throws Exception;


    /**
     * 点击派生关系
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    public TResponseVo getVersionTreeService(String fileUuid) throws Exception;

    /**
     * 点击完整关系图
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    public TResponseVo getVersionCompleteTreeService(String fileUuid) throws Exception;


    /**
     * 读取版本记录快照
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    public TResponseVo getVersionPhotoService(String fileUuid, String fileVersionId) throws Exception;


    /**
     * 父文件更新
     */

    /**
     * 通知子文件变更(并非直接通知 而是将变化记录在hf_update_info表中)
     *
     * @param fileUuid
     * @return
     */
    public TResponseVo newUpdateInfoService(String fileUuid, List<Map<String, String>> updateInfoList, String userId) throws Exception;

    /**
     * 获取父文件更新信息
     *
     * @param fileUuid 当前文件的uuid
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getUpdateInfoService(String fileUuid) throws Exception;

    /**
     * 确认更新信息
     *
     * @param
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo confirmUpdateInfoService(String updateUuid, String status, String userId) throws Exception;


    /**
     * 法律法规更新
     */
    /**
     * 获取法律法规更新信息
     *
     * @param fileUuid 当前文件的uuid
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getLawUpdateService(String fileUuid) throws Exception;


    /**
     * 派生次数
     */
    /**
     * 获取子文件清单
     *
     * @param fileUuid
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getDerivationListService(String fileUuid) throws Exception;


    /**
     * 应用次数
     */
    /**
     * 获取应用清单
     *
     * @param fileUuid
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getUseListService(String fileUuid) throws Exception;


    /**
     * 新增参数
     *
     * @param docParamsPojo
     * @return
     * @throws Exception
     */
    public TResponseVo addParamService(DocParamsPojo docParamsPojo, String userId) throws Exception;

    /**
     * 新增参数(AI知识库版本)
     *
     * @param docParamsPojo
     * @return
     * @throws Exception
     */
    public TResponseVo addParamService2(DocParamsPojo docParamsPojo, String userId) throws Exception;

    /**
     * 参数类型维表
     */
    public TResponseVo getParamTypeDiService() throws Exception;

    /**
     * 参数填参角色维表
     */
    public TResponseVo getParamSceneDiService() throws Exception;

//    /**
//     * 参数数据来源维表
//     */
//    public TResponseVo getParamSourceDiService() throws Exception;

    /**
     * 参数分组来源维表
     */
    public TResponseVo getParamGroupDiService(String fileUuid, String fileVersionId) throws Exception;

    /**
     * 新增分组
     */
    public TResponseVo addParamGroupService(DocParamsGroupPojo docParamsGroupPojo) throws Exception;

    /**
     * 删除分组来源维表
     */
    public TResponseVo delParamGroupService(DocParamsGroupPojo docParamsGroupPojo) throws Exception;

    /**
     * 修改分组来源维表
     */
    public TResponseVo updateParamGroupService(DocParamsGroupPojo docParamsGroupPojo) throws Exception;

    /**
     * 获取某一分组信息
     */
    public TResponseVo getParamGroupInfoService(DocParamsGroupPojo docParamsGroupPojo) throws Exception;

    /**
     * 获取分组list
     */
    public TResponseVo getParamGroupListService(PagePojo pagePojo) throws Exception;


    /**
     * 参数分组排序
     */
    public TResponseVo orderParamGroupService(DocParamsGroupPojo docParamsGroupPojo) throws Exception;

//    /**
//     * 获取分组list
//     */
//    public TResponseVo getParamGroupListLVService(PagePojo pagePojo) throws Exception;

    /**
     * 删除参数
     *
     * @param paramsUuid
     * @return
     * @throws Exception
     */
    public TResponseVo delParamService(String paramsUuid) throws Exception;

    /**
     * 修改参数
     *
     * @param docParamsPojo
     * @return
     * @throws Exception
     */
    public TResponseVo updateParamService(DocParamsPojo docParamsPojo) throws Exception;

    /**
     * 修改参数
     *
     * @param paramsUuid
     * @return
     * @throws Exception
     */
    public TResponseVo getParamInfoService(String paramsUuid) throws Exception;

    /**
     * 读取参数库列表
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    public TResponseVo getParamService(PagePojo pagePojo, String userId, String platform) throws Exception;


    /**
     * 读取文内已使用参数列表
     *
     * @param docParamsPojo
     * @return
     * @throws Exception
     */
    public TResponseVo getContentUsedParamService(DocParamsPojo docParamsPojo) throws Exception;

//    /**
//     * 文章内参数关联至参数库
//     * @param fileUuid
//     * @param fileVersionId
//     * @param paramsIdInfile
//     * @param paramsUuid
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo updateParamListParamsRelatService(String fileUuid, String fileVersionId, String paramsIdInfile, String paramsUuid) throws Exception;


//    /**
//     * 读取参数清单
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo getParamListService(String fileUuid, String fileVersionId) throws Exception;
//
//
//    /**
//     * 提交参数清单
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @param lists
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo commitParamListService(String fileUuid, String fileVersionId, List<CommitParamListItemForm> lists) throws Exception;


//    /**
//     * 读取草稿内容
//     * @param fileUuid
//     * @param fileVersionId
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo getDraftService(String fileUuid, String fileVersionId) throws Exception;

//    /**
//     * 保存草稿
//     *
//     * @param docFileVerIndexPojo
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo saveDraftService(DocFileVerIndexPojo docFileVerIndexPojo, String userId) throws Exception;
//
//    /**
//     * 删除草稿
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo delDraftService(String fileUuid, String fileVersionId) throws Exception;

    /**
     * 文章内 编辑提交
     *
     * @param docFileIndexPojo
     * @param session
     * @return
     * @throws Exception
     */
    public TResponseVo addSubmitService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception;

    /**
     * 文章内新建派生
     *
     * @param docFileIndexPojo
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo addDeriveService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception;

    /**
     * 新增版本
     *
     * @param docFileIndexPojo
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo addVersionService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 获取文本编辑页右侧看板展示信息
     */
    public TResponseVo getBoardDataService(String fileUuid, String fileVersionId, HttpSession session) throws Exception;
    /**
     * 获取文本编辑页右侧看板展示信息
     */
    public TResponseVo getBoardSecondDataService(String fileUuid, String fileVersionId, HttpSession session) throws Exception;

    /**
     * 操作大纲
     */
    /**
     * 新增章节
     * type=0 同级章节
     * type=1 下级章节
     */
    public TResponseVo addOutlineService(OutLinePojo outLinePojo) throws Exception;

    /**
     * 修改章节
     */
    public TResponseVo updateOutlineService(OutLinePojo outLinePojo) throws Exception;

    /**
     * 删除章节
     */
    public TResponseVo delOutlineService(String outlineId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 修改章节题目
     */
    public TResponseVo updateOutlineNameService(String outlineId, String fileUuid, String fileVersionId, String outlineText) throws Exception;


    /**
     * 编辑替换章节-获取可编辑章节列表
     */
    public TResponseVo getReplaceOutlineListService(String outlineId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 编辑替换章节-确认
     */
    public TResponseVo confirmReplaceOutlineService(String outlineId, List<String> outlineList, String fileUuid, String fileVersionId, String color) throws Exception;

    /**
     * 编辑非必选章节-确认
     */
    public TResponseVo confirmNecessaryOutlineService(String outlineId, String fileUuid, String fileVersionId, String isNecessary) throws Exception;


    /**
     * 自动保存
     */
    public TResponseVo autoSaveService(ContentPojo contentPojo) throws Exception;

    /**
     * 创建一个影子版本
     */
    public TResponseVo newShadowVersionService(DocFileIndexPojo docFileIndexPojo) throws Exception;


    /**
     * 根据标签搜索
     */
    public TResponseVo getSimilarDocService(List<String> labelList, String userId, String searchLike, String fileTypeId) throws Exception;

    /**
     * 标签库-新增标签分组
     */
    public TResponseVo addLabelGroupService(DocLabelPojo docLabelPojo) throws Exception;

    /**
     * 标签库-删除标签分组
     */
    public TResponseVo delLabelGroupService(DocLabelPojo docLabelPojo) throws Exception;

    /**
     * 标签库-获取标签分组列表
     */
    public TResponseVo getLabelGroupListService(PagePojo pagePojo) throws Exception;

    /**
     * 标签库-获取标签分组键值对
     */
    public TResponseVo getLabelGroupLVListService() throws Exception;

    /**
     * 标签库-增
     */
    public TResponseVo addLabelService(DocLabelPojo docLabelPojo) throws Exception;

    /**
     * 标签库-删
     */
    public TResponseVo delLabelService(String labelUuid) throws Exception;

    /**
     * 标签库-改
     */
    public TResponseVo updateLabelService(DocLabelPojo docLabelPojo) throws Exception;

    /**
     * 标签库-查
     */
    public TResponseVo getLabelInfoService(String labelUuid) throws Exception;

    /**
     * 标签库-查
     */
    public TResponseVo getLabelListService(PagePojo pagePojo, String labelGroupId, String fileTypeId) throws Exception;

    /**
     * 标签库-查
     */
    public TResponseVo getLabelLVListService(String labelGroupId) throws Exception;


//    /**
//     * 编辑页参数清单-新增
//     */
//    public TResponseVo addContentParamService(DocParamsPojo docParamsPojo) throws Exception;
//

    /**
     * 编辑页参数清单-删除
     */
    public TResponseVo delContentParamService(DocParamsPojo docParamsPojo) throws Exception;

    /**
     * 编辑页参数清单-查询
     */
    public TResponseVo getContentParamService(DocParamsPojo docParamsPojo) throws Exception;

    /**
     * 编辑页参数清单-修改文内参数
     */
    public TResponseVo updateContentParamService(DocParamsPojo docParamsPojo) throws Exception;

    /**
     * 编辑页参数清单-修改展示效果
     */
    public TResponseVo updateContentParamShowService(DocParamsPojo docParamsPojo) throws Exception;

    /**
     * 编辑页参数清单-修改展示效果
     */
    public TResponseVo updateContentParamPromptService(DocParamsForm docParamsPojo) throws Exception;

    /**
     * 编辑页参数清单-新增和删除
     */
    public TResponseVo addAndDelContentParamService(DocParamsPojo docParamsPojo) throws Exception;

    /**
     * 编辑页搜索-搜索列表
     */
    public TResponseVo searchService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 右侧看板-文件属性-查看属性
     */
    public TResponseVo getFilePropertyService(String fileUuid) throws Exception;

    /**
     * 右侧看板-文件属性-获取参与人下拉列表
     */
    public TResponseVo getFilePropertyUserListService(String userId) throws Exception;

    /**
     * 右侧看板-文件属性-获取文件类型下拉列表
     */
    public TResponseVo getFileTypeListService() throws Exception;

    /**
     * 右侧看板-文件属性-获取适用范围下拉列表
     */
    public TResponseVo getFileUseRangeListService() throws Exception;

    /**
     * 右侧看板-文件属性-修改属性
     */
    public TResponseVo updateFilePropertyService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 根据参数类型返回显示格式
     */
    public TResponseVo getParamTypeStyleListService(String paramsTypeId) throws Exception;

    /**
     * 获取附属文件类型维表
     */
    public TResponseVo getTemplateTypeDiService(String templateTypeId) throws Exception;

    /**
     * 模板组-新增附属文件
     */
    public TResponseVo newSubsidiaryFileService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 模板组-上传附属文件
     */
    public TResponseVo uploadSubsidiaryFileService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 模板组-上传附属文件-生成大纲
     */
    public TResponseVo insertOutlineService(String sql) throws Exception;

    /**
     * 模板组-上传附属文件-生成大纲内容
     */
    public TResponseVo insertContentService(String sql) throws Exception;

    /**
     * 模板组-删除附属文件
     */
    public TResponseVo delSubsidiaryFileService(String fileUuid) throws Exception;

    /**
     * 模板组-获取附属文件列表
     */
    public TResponseVo getSubsidiaryFileListService(String fileUuid, String fileVersionId, HttpSession session) throws Exception;

    /**
     * 模板组-修改附属文件名
     */
    public TResponseVo updateSubsidiaryFileNameService(String fileUuid, String fileName, String templateTypeId) throws Exception;

/**
 * 标注内容
 */
    /**
     * 词条库-新增词条
     *
     * @param docWordsPojo
     * @return
     * @throws Exception
     */
    public TResponseVo addWordsService(DocWordsPojo docWordsPojo) throws Exception;

    /**
     * 词条库-删除词条
     *
     * @param wordsUuid
     * @return
     * @throws Exception
     */
    public TResponseVo delWordsService(String wordsUuid) throws Exception;

    /**
     * 词条库-修改词条
     *
     * @param docWordsPojo
     * @return
     * @throws Exception
     */
    public TResponseVo updateWordsService(DocWordsPojo docWordsPojo) throws Exception;

    /**
     * 词条库-查询词条信息
     *
     * @param wordsUuid
     * @return
     * @throws Exception
     */
    public TResponseVo getWordsInfoService(String wordsUuid) throws Exception;

    /**
     * 词条库-读取词条库列表
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    public TResponseVo getWordsListService(PagePojo pagePojo) throws Exception;

    /**
     * 词条库-获取labelvalue
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo getWordsLabelValueListService() throws Exception;
//    /**
//     * 新增摘编
//     *
//     * @param createUserId
//     * @param tipsName
//     * @param tipsDesc
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo addTipsService(String createUserId, String tipsName, String tipsDesc) throws Exception;
//
//    /**
//     * 删除摘编
//     *
//     * @param tipsUuid
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo delTipsService(String tipsUuid) throws Exception;
//
//    /**
//     * 修改摘编
//     *
//     * @param docTipsPojo
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo updateTipsService(DocTipsPojo docTipsPojo) throws Exception;
//
//    /**
//     * 读取摘编库列表
//     *
//     * @param pageNum
//     * @param pageSize
//     * @param tipsNameLike
//     * @param isPaged
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo getTipsService(Integer pageNum, Integer pageSize, String tipsNameLike, String isPaged) throws Exception;

//    /**
//     * 标注库-新增标注
//     *
//     * @param tagPojo
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo addTagService(TagPojo tagPojo) throws Exception;

    /**
     * 标注库-删除标注
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    public TResponseVo delTagService(TagPojo tagPojo) throws Exception;

    /**
     * 标注库-新增和删除
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    public TResponseVo addAndDelContentTagService(TagPojo tagPojo) throws Exception;

    /**
     * 标注库-修改标注
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    public TResponseVo updateTagService(TagPojo tagPojo) throws Exception;

    /**
     * 标注库-获取标注信息
     *
     * @param tagId
     * @return
     * @throws Exception
     */
    public TResponseVo getTagInfoService(String tagId, String fileUuid, String fileVersionId) throws Exception;

    /**
     * 标注库-获取标注列表
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    public TResponseVo getTagListService(PagePojo pagePojo) throws Exception;

    /**
     * 标注库-编辑页参数清单-查询
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    public TResponseVo getContentTagService(TagPojo tagPojo) throws Exception;
    /**
     * 编辑摘编-获取摘编包含的条款清单
     *
     * @param lawId
     * @return
     * @throws Exception
     */
    public TResponseVo getLawContentService(String lawId) throws Exception;
    /**
     * 编辑摘编-提交
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    public TResponseVo commitLawService(TagPojo tagPojo) throws Exception;

    /**
     * 书签库-新增书签
     */
    public TResponseVo addBookmarkService(BookmarkPojo bookmarkPojo) throws Exception;

    /**
     * 书签库-删除书签
     */
    public TResponseVo delBookmarkService(BookmarkPojo bookmarkPojo) throws Exception;

    /**
     * 书签库-增加和删除书签
     */
    public TResponseVo addAndDelBookmarkService(BookmarkPojo bookmarkPojo) throws Exception;

    /**
     * 书签库-修改书签
     */
    public TResponseVo updateBookmarkService(BookmarkPojo bookmarkPojo) throws Exception;

    /**
     * 书签库-查询书签信息
     */
    public TResponseVo getBookmarkInfoService(BookmarkPojo bookmarkPojo) throws Exception;

    /**
     * 书签库-查询书签列表
     */
    public TResponseVo getBookmarkListService(PagePojo pagePojo) throws Exception;

    /**
     * 书签库-查询LabelValue数据
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getBookmarkLabelValueListService(String fileUuid, String fileVersionId) throws Exception;

    /**
     * 获取分流版本
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getTrueVersionIdService(String fileUuid, HttpSession session) throws Exception;

    /**
     * 填写参数
     */
    public TResponseVo writeParamService(DocParamsPojo docParamsPojo) throws Exception;

    /**
     * 参数填写大保存
     */
    public TResponseVo saveWriteParamService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception;

    /**
     * 参数还原
     */
    public TResponseVo resetParamService(DocParamsPojo docParamsPojo, HttpSession session) throws Exception;

    /**
     * 删除参数附件
     */
    public TResponseVo deleteAnnexParamService(String fileUuid, String fileVersionId, String paramsUuid, String uid, String userId) throws Exception;

    /**
     * 招标文件-左侧项目信息
     */
    public TResponseVo getBidLeftInfoService(String fileUuid) throws Exception;

    /**
     * 招标文件-左侧项目信息更新
     */
    public TResponseVo updateBidLeftInfoService(DocFileIndexPojo docFileIndexPojo) throws Exception;


    /**
     * 生成招标文件秘钥
     */
    public TResponseVo getBidKeyService(String fileUuid, String userId, String companyId) throws Exception;

    /**
     * 接口读取的摘编入库 用来对比法律法规变化
     */
    public String getLawExtractTextListsService(Integer pageSize) throws Exception;
    /**
     * 对比法律法规变化
     */
    public void compareLawExtractTextService(String lawExtractTextListStr) throws Exception;
    /**
     * 获取法律法规变化清单
     */
    public TResponseVo getLawChangeListService(String fileUuid, String fileVersionId) throws Exception;
    /**
     * 已读
     */
    public TResponseVo readLawChangeService(HfLawPojo hfLawPojo) throws Exception;


/**
 * 评标阶段
 */
    /**
     * 创建评标办法
     */
    public TResponseVo newJudgmentMethodService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标办法-删除
     */
    public TResponseVo delJudgmentMethodService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标办法-修改
     */
    public TResponseVo updateJudgmentMethodService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标办法-查询
     */
    public TResponseVo getJudgmentMethodInfoService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 获取办法段落信息
     */
    public TResponseVo getJudgmentMethodOutlineService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;


    /**
     * 评标办法-查询列表
     */
    public TResponseVo getJudgmentMethodListService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;


    /**
     * 创建评标模块
     */
    public TResponseVo newJudgmentModuleService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标模块-删除
     */
    public TResponseVo delJudgmentModuleService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标模块-修改
     */
    public TResponseVo updateJudgmentModuleService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标模块-查询
     */
    public TResponseVo getJudgmentModuleInfoService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标模块-查询列表
     */
    public TResponseVo getJudgmentModuleListService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;


    /**
     * 创建评标细则(判断or评分)
     */
    public TResponseVo newJudgmentDetailService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标细则-删除
     */
    public TResponseVo delJudgmentDetailService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标细则-修改
     */
    public TResponseVo updateJudgmentDetailService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标细则-查询
     */
    public TResponseVo getJudgmentDetailInfoService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 评标细则-查询列表
     */
    public TResponseVo getJudgmentDetailListService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;

    /**
     * 查询办法与模块的树结构
     */
    public TResponseVo getJudgmentMethodModuleTreeService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception;


    /**
     * 获取投标人要填写的参数列表
     */
    public TResponseVo getTendParamsListService(String fileUuid, String fileVersionId) throws Exception;

    /**
     * 参数值的换算
     */
    public TResponseVo unitTransService(String value, String unitId, String tagUnitId) throws Exception;

    /**
     * 获取单位列表
     */
    public TResponseVo getUnitListService(List<String> nodeIds) throws Exception;

    /**
     * 获取文内某参数的详细信息(沉浸模式使用)
     */
    public TResponseVo getParamDetailService(String fileUuid, String fileVersionId, String uuid) throws Exception;


    /**
     * 获取同项目下文件列表
     */
    public TResponseVo getProjectMateFileService(String project) throws Exception;


    /**
     * 组合文件（项目内，文件编辑，点击替换章节，选择文件章节，替换）
     */
    public TResponseVo newCompoundFileService(DocFileIndexPojo docFileIndexPojo) throws Exception;


    /**
     * 项目投递
     */
    public TResponseVo newDeliverFileService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 查看投递记录
     */
    public TResponseVo getDeliverRecService(String fileUuid, String userId) throws Exception;

    /**
     * 获取数据源-辅助填写预备数据
     */
    public TResponseVo getParamSourceDiService(String userId) throws Exception;

    /**
     * 辅助工具填写-获取预填空数量
     *
     * @param docParamsPojo
     * @return
     * @throws Exception
     */
    public TResponseVo getAutoInputListService(DocParamsPojo docParamsPojo) throws Exception;

    /**
     * 辅助工具-自动填写
     */
    public TResponseVo autoInputService(HfSupToolFormPojo hfSupToolFormPojo) throws Exception;

    /**
     * 获取参数使用场景维表
     */
    public TResponseVo getParamSaturationListService() throws Exception;


/**
 * 参数填写历史记录
 */
    /**
     * 获取参数填写记录
     */
    public TResponseVo getParamChangeHistoryService(HfParamChangeHistoryPojo hfParamChangeHistoryPojo) throws Exception;

    /**
     * 清空参数填写记录
     */
    public TResponseVo clearParamChangeHistoryService(HfParamChangeHistoryPojo hfParamChangeHistoryPojo) throws Exception;


    /**
     * 添加批注
     */
    public TResponseVo addAnnotateService(OutLinePojo outLinePojo) throws Exception;


    /**
     * 获取变更列表
     */
    public TResponseVo getCompareInfoListService(OutLinePojo outLinePojo) throws Exception;

    /**
     * 查看变更信息
     *
     * @return
     */
    public void getCompareInfoDetailService(String fileUuid, String fileVersionId, String compare0, String compare1, String uuid, HttpServletResponse res) throws Exception;

    /**
     * 自动对比
     */
    public void autoCompareService(String fileUuid, String fileVersionId) throws Exception;

    /**
     * 手动对比
     */
    public TResponseVo handCompareService(String fileUuid, String fileVersionId, String fileUuid2, String fileVersionId2) throws Exception;

    /**
     * 获取可对比记录
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    public TResponseVo getCompareVersionListService(String fileUuid, String fileVersionId) throws Exception;


/**
 * 评标要素
 */
    /**
     * 模型定义评标要素-总体方案
     */
    public TResponseVo confirmAssessTotalPlanService(HfContentAssessPojo hfContentAssessPojo) throws Exception;

    /**
     * 模型定义评标要素-获取总体方案内容
     */
    public TResponseVo getAssessTotalPlanService(HfContentAssessPojo hfContentAssessPojo) throws Exception;

    /**
     * 模型定义评标要素-获取备选方案下拉表
     */
    public TResponseVo getAssessPlanListService(HfContentAssessPojo hfContentAssessPojo) throws Exception;

    /**
     * 模型定义评标要素-获取总体方案中的方案列表
     */
    public TResponseVo getAssessElementListInPlanService(HfContentAssessPojo hfContentAssessPojo) throws Exception;

    /**
     * 模型定义评标要素-查询具体方案内容
     */
    public TResponseVo getAssessElementListService(HfContentAssessPojo hfContentAssessPojo) throws Exception;

    /**
     * 模型定义评标要素-获取方案中某个元素的信息
     */
    public TResponseVo getAssessElementInfoService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception;

    /**
     * 模型定义评标要素-编辑方案中某个元素
     */
    public TResponseVo updateAssessElementService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception;

    /**
     * 模型定义评标要素-删除方案中某个元素
     */
    public TResponseVo delAssessElementService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception;

    /**
     * 模型定义评标要素-在方案中新增元素
     */
    public TResponseVo addAssessElementService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception;

    /**
     * 模型定义评标要素-调整元素顺序
     */
    public TResponseVo orderAssessElementService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception;

    /**
     * 模型定义评标要素-获取总体方案的二维表
     */
    public TResponseVo useAssessTotalPlanService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception;

    /**
     * 模型定义评标要素-获取文内已使用参数列表
     */
    public TResponseVo getContentAssessParamService(HfContentAssessPojo hfContentAssessPojo) throws Exception;

    /**
     * 模型定义评标要素-提交前检查合规性
     */
    public TResponseVo checkBeforeConfirmService(HfContentAssessPojo hfContentAssessPojo) throws Exception;

    /**
     * 模型定义评标要素-获取文内标注列表
     */
    public TResponseVo getContentAssessTagService(HfContentAssessPojo hfContentAssessPojo) throws Exception;


    /**
     * 参数清单查看
     */
    public TResponseVo getFileParamsWriteListService(DocParamsPojo docParamsPojo) throws Exception;



    /**
     * 增删数据表
     */
    public TResponseVo addAndDelContentDbService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception;

    /**
     * 获取数据表清单
     */
    public TResponseVo getContentDbService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception;
    /**
     * 右侧列表中删除数据表
     */
    public TResponseVo delContentDbService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception;
    /**
     * 右侧列表中编辑数据表
     */
    public TResponseVo updateContentDbService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception;
    /**
     * 获取卷出来的表
     */
    public TResponseVo getRollTableListService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception;






}
