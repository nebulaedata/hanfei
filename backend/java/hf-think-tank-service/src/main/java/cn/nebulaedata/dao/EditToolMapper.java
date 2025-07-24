package cn.nebulaedata.dao;

import cn.nebulaedata.pojo.*;
import cn.nebulaedata.vo.TResponseVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 徐衍旭
 * @date 2023/3/15 14:32
 * @note
 */
public interface EditToolMapper {

    /**
     * 新建文件夹
     *
     * @param editToolFolderPojo
     * @return
     */
    public Integer newFolderDao(EditToolFolderPojo editToolFolderPojo);

    /**
     * 校验文件夹名称重复性
     *
     * @param folderName
     * @param folderParentId
     * @return
     */
    public Integer checkFolderNameDao(String folderName, String createUserId, String folderParentId, String type, String folderId);

    /**
     * 校验文件夹是否为空
     *
     * @param folderId
     * @return
     */
    public Integer checkFolderEmpty1Dao(String folderId);  // 查文件夹

    public Integer checkFolderEmpty2Dao(String folderId);  // 查文件

    /**
     * 判断哪些key是文件夹
     *
     * @param folderIdListStr
     * @return
     */
    public List<String> getFolderKeyListDao(String folderIdListStr);

    /**
     * 删除文件夹
     *
     * @param
     * @return
     */
    public Integer delFolderDao(String folderKeyListDaoTotalStr);

    public Integer delDocumentDao(String documentKeyListStr, String folderKeyListDaoTotalStr);
    public Integer delUploadModelFileDao(String documentKeyListStr, String folderKeyListDaoTotalStr);

    /**
     * 获取文件夹信息
     *
     * @param folderId
     * @return
     */
    public EditToolFolderPojo getFolderInfoDao(String folderId);

    public DocFileIndexPojo getDocumentInfoDao(String fileUuid);
    public DocFileIndexPojo getUploadModelFileInfoDao(String fileUuid);

    /**
     * 获取全部文件夹信息
     *
     * @return
     */
    public List<EditToolFolderPojo> getAllFolderDao();

    /**
     * 根据folderId获取文件夹内容
     *
     * @param folderId
     * @return
     */
    public List<String> getFolderNameListByParentFolderIdDao(String folderId);

    public List<String> getFolderNameListByFolderIdDao(@Param("keyList") List<String> keyList);

    /**
     * 获取文件夹信息
     *
     * @param
     * @return
     */
    public List<EditToolFolderPojo> getFolderListDao(String searchLike, String folderId, String userId, String order, String type);

    public List<EditToolFolderPojo> getFolderListLVDao(String userId);

    public List<EditToolFolderPojo> getAllFolderListDao(String userId, String type);
    /**
     * 获取文件夹信息
     *
     * @param
     * @return
     */
//    public List<EditToolFolderPojo> getChildrenFolderListDao(@Param(value = "lists") List<String> lists);

    /**
     * 获取文件信息
     *
     * @param
     * @return
     */
    public List<DocFileIndexPojo> getDocumentListDao(String searchLike, String folderId, String userId, String order, String type);

    public List<LabelValuePojo> getDocumentListLVDao(String folderId);

    /**
     * 重命名文件夹
     *
     * @param folderId
     * @param folderName
     * @return
     */
    public Integer renameFolderDao(String folderId, String folderName);

    public Integer renameDocumentDao(String fileUuid, String fileName);
    public Integer renameUploadModelFileDao(String fileUuid, String fileName);

    /**
     * 移动文件夹
     *
     * @param folderIdListStr
     * @param folderId
     * @return
     */
    public Integer moveFolderDao(String folderIdListStr, String folderId);

    public Integer moveDocumentDao(String folderIdListStr, String folderId);

    public Integer moveUploadModelFileDao(String folderIdListStr, String folderId);

    /**
     * 搜索
     *
     * @param fileNameLike
     * @param type
     * @return
     */
    public List<DocFileIndexPojo> searchDao(String fileNameLike, String type, String folderId);

    /**
     * 审批
     *
     * @param fileUuid
     * @param assessStatus
     * @return
     */
    public Integer updateFileAssessStatusDao(String fileUuid, String assessStatus);

    /**
     * 获取功能列表
     *
     * @return
     */
    public List<HfFileTypeDiPojo> getApplicationListDao(String inUse);

    /**
     * 开通/关闭功能
     *
     * @return
     */
    public Integer changeApplicationStatusDao(HfFileTypeDiPojo hfFileTypeDiPojo);


    /**
     *
     * @param searchLike
     * @param folderId
     * @param userId
     * @param order
     * @param type
     * @return
     */
    public List<DocFileIndexPojo> getUploadModelFileListDao(String searchLike, String folderId, String userId, String order, String type);
    public List<LabelValuePojo> getUploadModelFileListLVDao(String folderId);

    public Integer addParamsWorkflowDao(ParamsWorkflowPojo paramsWorkflowPojo);

    public Integer deleteParamsWorkflowDao(ParamsWorkflowPojo paramsWorkflowPojo);

    public List<ParamsWorkflowPojo> searchParamsWorkflowDao(ParamsWorkflowPojo paramsWorkflowPojo);

    public Integer updateParamsWorkflowDao(ParamsWorkflowPojo paramsWorkflowPojo);

}
