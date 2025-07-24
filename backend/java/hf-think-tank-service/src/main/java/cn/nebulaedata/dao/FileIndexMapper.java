package cn.nebulaedata.dao;


import cn.nebulaedata.pojo.DocFileIndexPojo;
import cn.nebulaedata.pojo.DocFileVerIndexPojo;
import cn.nebulaedata.pojo.DocTaggingPojo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 贾亦真
 * @date 2021/1/11 14:01
 * @note
 */
public interface FileIndexMapper {

    /**
     * 新增文件索引记录
     *
     * @param docFileIndexPojo
     * @return
     */
    public Integer insertFileIndexDao(DocFileIndexPojo docFileIndexPojo);

    /**
     * 新增大纲树
     *
     * @param sql
     * @return
     */
    public Integer insertOutlineDao(String sql);

    /**
     * 新增大纲对应内容
     *
     * @param sql
     * @return
     */
    public Integer insertContentDao(String sql);

    /**
     * 删除文件索引记录
     *
     * @param fileUuid
     * @return
     */
    public Integer deleteFileIndexDao(String fileUuid);

    /**
     * 查询文件索引记录
     *
     * @return
     */
    public List<DocFileIndexPojo> selectFileIndexDao(@Param("items") List<String> fileUuids);

    /**
     * 查询单一文件
     *
     * @param fileUuid
     * @return
     */
    public DocFileIndexPojo selectFileIndexSingleDao(String fileUuid);

    /**
     * 新增文件版本索引
     *
     * @param docFileVerIndexPojo
     * @return
     */
    public Integer insertFileVersionDao(DocFileVerIndexPojo docFileVerIndexPojo);

    /**
     * 删除文件版本
     *
     * @param fileVersionId
     * @return
     */
    public Integer deleteFileVersionDao(String fileVersionId);

    /**
     * 查询文件版本索引
     *
     * @return
     */
    public List<DocFileVerIndexPojo> selectFileVersionDao();



//    /**
//     * 获取主页类型文件列表
//     *
//     * @return
//     */
//    public List<DocFileTypePojo> selectFileTypeDetailDao();

//    /**
//     * 查询我的文档
//     *
//     * @return
//     */
//    public List<DocFileIndexPojo> selectMyDocumentDao(@Param("userId") String userId, @Param("searchLike") String searchLike);

    /**
     * 文件搜索
     *
     * @return
     */
    public List<DocFileIndexPojo> selectFileSearchDao(@Param("lists") List<String> searchContents, @Param("searchIdSelect1") String searchIdSelect1, @Param("searchIdSelect2") String searchIdSelect2, @Param("searchIdSelect3") String searchIdSelect3);

    /**
     * 更新文件索引
     *
     * @param fileUuid
     * @param fileStatus
     * @return
     */
    public Integer updateFileIndexStatusDao(@Param("fileUuid") String fileUuid, @Param("fileStatus") String fileStatus);

//    /**
//     * 选择热爱搜索
//     *
//     * @param num
//     * @return
//     */
//    public List<String> selectHotSearchDao(Integer num);

//    /**
//     * 热门文档列表
//     *
//     * @return
//     */
//    public List<DocFileIndexPojo> selectHotFileDao();
//
//    /**
//     * 热门点击文档列表
//     *
//     * @return
//     */
//    public List<DocFileIndexPojo> selectHotClickFileDao();

    /**
     * 查看带版本信息的单文件
     *
     * @param fileUuid
     * @return
     */
    public DocFileIndexPojo selectFileIndexSingleIncVerInfoDao(String fileUuid);

    /**
     * 选择文件标版本属性
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public DocFileVerIndexPojo selectFileIndexSingleIncVerInfoVersionDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

//    /**
//     * 获取精选
//     *
//     * @return
//     */
//    public List<DocTaggingPojo> selectGoodTaggingDao();

//    /**
//     * 获取标注数量
//     *
//     * @return
//     */
//    public List<DocTaggingPojo> selectTaggingNumDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

    /**
     * 查询文档详情信息
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public DocFileIndexPojo selectSingleFileDetailInfoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

//    /**
//     * 插入标注
//     *
//     * @param docTaggingPojo
//     * @return
//     */
//    public Integer insertFileTaggingDao(DocTaggingPojo docTaggingPojo);

//    /**
//     * 查询段落标签
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @param fileParagraphId
//     * @return
//     */
//    public DocTaggingPojo selectTaggingInfoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("fileParagraphId") String fileParagraphId);

//    /**
//     * 查询段落历史标签
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @param fileParagraphId
//     * @return
//     */
//    public List<DocTaggingPojo> selectTaggingHisDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("fileParagraphId") String fileParagraphId);

//    /**
//     * 删除标签记录
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @param fileParagraphId
//     * @return
//     */
//    public Integer deleteTaggingRecoDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("fileParagraphId") String fileParagraphId);

    /**
     * 获取文档结构文本
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public String selectDocStrDao(@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId);

//    /**
//     * 获取首页文档推荐
//     *
//     * @return
//     */
//    public List<DocFileIndexPojo> getHomePageDocumentListDao();
}
