package cn.nebulaedata.service;


import cn.nebulaedata.pojo.DocFileIndexPojo;
import cn.nebulaedata.pojo.DocTaggingPojo;
import cn.nebulaedata.vo.TResponseVo;

/**
 * @author 贾亦真
 * @date 2021/1/11 14:01
 * @note
 */
public interface FileIndexService {

    /**
     * 上传新文件
     *
     * @param docFileIndexPojo
     * @return
     * @throws Exception
     */
    public Boolean AddNewFileService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 上传新文件
     *
     * @param sql
     * @return
     * @throws Exception
     */
    public TResponseVo insertOutlineService(String sql) throws Exception;

    /**
     * 上传新文件
     *
     * @param sql
     * @return
     * @throws Exception
     */
    public TResponseVo insertContentService(String sql) throws Exception;



//    /**
//     * 获取我的文档列表
//     *
//     * @param userId
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo GetMyDocumentService(Integer pageNum, Integer pageSize, String userId, String searchLike) throws Exception;

    /**
     * 文档搜索接口
     *
     * @param searchContent
     * @param searchIdSelect1
     * @param searchIdSelect2
     * @param searchIdSelect3
     * @return
     * @throws Exception
     */
    public TResponseVo FileSearchService(Integer pageNum, Integer pageSize, String searchContent, String searchIdSelect1, String searchIdSelect2, String searchIdSelect3, String userId) throws Exception;

//    /**
//     * 热门搜索
//     *
//     * @param num
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo GetHotSearchService(Integer num) throws Exception;

//    /**
//     * 热门文件排序
//     *
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo GetHotFileService(String type) throws Exception;

//    /**
//     * 获取热门摘编
//     *
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo GetTaggingService() throws Exception;

    /**
     * 删除文档
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    public TResponseVo DelDocumentService(String fileUuid, String fileVersionId) throws Exception;

//    /**
//     * 添加标注
//     *
//     * @param docTaggingPojo
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo AddTaggingService(DocTaggingPojo docTaggingPojo) throws Exception;

//    /**
//     * 查询标注
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo GetTaggingService(String fileUuid, String fileVersionId, String fileParagraphId) throws Exception;

//    /**
//     * 获取查询历史标注
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @param fileParagraphId
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo GetTaggingHisService(String fileUuid, String fileVersionId, String fileParagraphId) throws Exception;


//    /**
//     * 获取首页文档推荐
//     *
//     * @return
//     * @throws Exception
//     */
//    public TResponseVo getHomePageDocumentListService() throws Exception;
}
