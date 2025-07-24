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
public interface EditToolService {

    /**
     * 文档空间
     */
    /**
     * 文档空间-文件空间-新建文件夹
     *
     * @return
     * @throws Exception
     */
    public TResponseVo newFolderService(EditToolFolderPojo editToolFolderPojo) throws Exception;

    /**
     * 文档空间-文件空间-删除文件夹
     *
     * @return
     * @throws Exception
     */
    public TResponseVo delFolderService(List<String> folderIdList) throws Exception;

    /**
     * 文档空间-文件空间-重命名文件夹
     *
     * @return
     * @throws Exception
     */
    public TResponseVo renameFolderService(EditToolFolderPojo editToolFolderPojo) throws Exception;

    /**
     * 文档空间-文件空间-重命名文件
     *
     * @return
     * @throws Exception
     */
    public TResponseVo renameDocumentService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 文档空间-文件空间-获取文件夹信息
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getFolderInfoService(String folderId) throws Exception;

    public TResponseVo getDocumentInfoService(String fileUuid) throws Exception;


    /**
     * 文档空间-文件空间-移动文件夹-获取文件夹清单
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getMoveFolderListService(PagePojo pagePojo, String userId) throws Exception;

    /**
     * 文档空间-文件空间-移动文件夹-移动
     *
     * @return
     * @throws Exception
     */
    public TResponseVo moveFolderService(List<String> folderIdList, String folderId) throws Exception;

    /**
     * 文档空间-文件空间-获取文件夹清单
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getFolderListService(PagePojo pagePojo, String userId) throws Exception;

    public TResponseVo getFolderListLVService(String userId) throws Exception;

    /**
     * 文档空间-文件空间-搜索
     *
     * @return
     * @throws Exception
     */
    public TResponseVo searchService(PagePojo pagePojo) throws Exception;


    /**
     * 最近打开的文档
     */
    public TResponseVo getRecentFileService(String userId) throws Exception;

    /**
     * 提交审核
     */
    public TResponseVo submitAssessService(DocFileIndexPojo docFileIndexPojo) throws Exception;

    /**
     * 获取待审核列表
     */
    public TResponseVo getNeedAssessListService(PagePojo pagePojo) throws Exception;


    /**
     * 获取功能列表
     */
    public TResponseVo getApplicationListService(HfFileTypeDiPojo hfFileTypeDiPojo) throws Exception;

    /**
     * 开通/关闭功能
     */
    public TResponseVo changeApplicationStatusService(HfFileTypeDiPojo hfFileTypeDiPojo) throws Exception;

    public TResponseVo addParamsWorkflowService(ParamsWorkflowPojo paramsWorkflowPojo);

    public TResponseVo deleteParamsWorkflowService(ParamsWorkflowPojo paramsWorkflowPojo);

    public TResponseVo searchParamsWorkflowService(ParamsWorkflowPojo paramsWorkflowPojo);

    public TResponseVo updateParamsWorkflowService(ParamsWorkflowPojo paramsWorkflowPojo);

}