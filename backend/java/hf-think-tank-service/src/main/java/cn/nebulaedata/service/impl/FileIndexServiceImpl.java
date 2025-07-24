package cn.nebulaedata.service.impl;


import cn.nebulaedata.dao.FileIndexMapper;
import cn.nebulaedata.dao.ProcessMapper;
import cn.nebulaedata.dao.UserMapper;
import cn.nebulaedata.exception.FileIndexException;
import cn.nebulaedata.pojo.DocFileIndexPojo;
import cn.nebulaedata.pojo.DocFileVerIndexPojo;
import cn.nebulaedata.pojo.DocLogSendPojo;
import cn.nebulaedata.pojo.DocTaggingPojo;
import cn.nebulaedata.service.FileIndexService;
import cn.nebulaedata.utils.AmqUtils;
import cn.nebulaedata.utils.DocFrameUtils;
import cn.nebulaedata.vo.TResponseVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author 贾亦真
 * @date 2021/1/11 14:01
 * @note
 */
@Service
public class FileIndexServiceImpl implements FileIndexService {

    private static final Logger LOG = LoggerFactory.getLogger(FileIndexServiceImpl.class);

    @Autowired
    private FileIndexMapper fileIndexMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProcessMapper processMapper;
    @Autowired
    private AmqUtils amqUtils;

//    @Value("${doc-frame-service.file_search_log_mq}")
//    private String fileSearchLogMq;
//    @Value("${doc-frame-service.static-resource-path}")
//    private String staticResourcePath;
//    @Value("${doc-frame-service.file-search-index-del-mq}")
//    private String fileSearchIndexDelMq;


    /**
     * 上传新文件
     *
     * @param docFileIndexPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean AddNewFileService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        Integer i = 0;
        DocFileVerIndexPojo versions = docFileIndexPojo.getVersions();
        try {
            i = fileIndexMapper.insertFileIndexDao(docFileIndexPojo);
            if (i != 1) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(3);
                // TODO 报错未处理
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "AddNewFileService", docFileIndexPojo.getFileName(), "主文件索引表新增失败");
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(5);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "AddNewFileService", docFileIndexPojo.getFileName(), "主文件索引表新增SQL错误");
            throw fileIndexException;
        }
        try {
            versions.setVersionStatus("正式版本");
            i = fileIndexMapper.insertFileVersionDao(versions);
            if (i != 1) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(4);
                // TODO 报错未处理
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "AddNewFileService", docFileIndexPojo.getFileName(), "主文件版本索引表新增失败");
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(6);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "AddNewFileService", docFileIndexPojo.getFileName(), "主文件版本索引表新增SQL错误");
            throw fileIndexException;
        }
        return true;
    }


    /**
     * 上传新文件
     *
     * @param sql
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo insertOutlineService(String sql) throws Exception {
        return TResponseVo.success(fileIndexMapper.insertOutlineDao(sql));
    }

    /**
     * 上传新文件
     *
     * @param sql
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo insertContentService(String sql) throws Exception {
        return TResponseVo.success(fileIndexMapper.insertContentDao(sql));
    }





//    /**
//     * 获取我的文档列表
//     *
//     * @param userId
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo GetMyDocumentService(Integer pageNum, Integer pageSize, String userId, String searchLike) throws Exception {
//        if (StringUtils.isNotBlank(searchLike)) {
//            searchLike = searchLike.replaceAll("'", "");
//        }
//        PageHelper.startPage(pageNum, pageSize);
//        List<DocFileIndexPojo> docFileTypePojos = fileIndexMapper.selectMyDocumentDao(userId, searchLike);
//        PageInfo pageInfo = new PageInfo(docFileTypePojos);
//        return TResponseVo.success(pageInfo);
//    }

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
    @Override
    public TResponseVo FileSearchService(Integer pageNum, Integer pageSize, String searchContent, String searchIdSelect1, String searchIdSelect2, String searchIdSelect3, String userId) throws Exception {
        searchContent = searchContent.replaceAll("-", "");
        List<String> searchContents = DocFrameUtils.strConList(searchContent, " ");
        PageHelper.startPage(pageNum, pageSize);
        List<DocFileIndexPojo> docFileIndexPojos = fileIndexMapper.selectFileSearchDao(searchContents, searchIdSelect1, searchIdSelect2, searchIdSelect3);
        PageInfo pageInfo = new PageInfo(docFileIndexPojos);
        DocLogSendPojo docLogSendPojo = new DocLogSendPojo();
        docLogSendPojo.setLogType("1");
        docLogSendPojo.setSearchContent(searchContent);
        docLogSendPojo.setUserId(userId);
        docLogSendPojo.setCreateTime(new Date());
//        amqUtils.sendMsgToMq("发送搜索日志", fileSearchLogMq, JSON.toJSONString(docLogSendPojo));
        return TResponseVo.success(pageInfo);
    }

//    @Override
//    public TResponseVo GetHotSearchService(Integer num) throws Exception {
//        List<String> hots = fileIndexMapper.selectHotSearchDao(num);
//        return TResponseVo.success(hots);
//    }

//    /**
//     * 热门文件排序
//     *
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo GetHotFileService(String type) throws Exception {
//        if ("click".equalsIgnoreCase(type)) {
//            return TResponseVo.success(fileIndexMapper.selectHotClickFileDao());
//        }
//        List<DocFileIndexPojo> docFileIndexPojos = fileIndexMapper.selectHotFileDao();
//        return TResponseVo.success(docFileIndexPojos);
//    }

//    /**
//     * 获取热门摘编
//     *
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo GetTaggingService() throws Exception {
//        return TResponseVo.success(fileIndexMapper.selectGoodTaggingDao());
//    }

    /**
     * 删除文档
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo DelDocumentService(String fileUuid, String fileVersionId) throws Exception {
        DocFileIndexPojo docFileIndexPojo = fileIndexMapper.selectSingleFileDetailInfoDao(fileUuid, fileVersionId);
        String fileVersionId1 = docFileIndexPojo.getFileVersionId();
        Integer i = 0;
        try {
            if (fileVersionId.equals(fileVersionId1)) {
                i = fileIndexMapper.updateFileIndexStatusDao(fileUuid, "3");
                i = processMapper.updateSingleVersionStatusDao(fileUuid, "已删除", fileVersionId,null);
            } else {
                i = processMapper.updateSingleVersionStatusDao(fileUuid, "已删除草稿", fileVersionId,null);
            }
        } catch (Exception e) {
            FileIndexException err = new FileIndexException();
            LOG.error("[Class]:{}-[BusinessId]:fileUuid-{}-Version-{}-[Msg]:{}", "DelDocumentService", fileUuid, fileVersionId, "更新删除状态SQL错误");
            err.setCode(7);
            throw err;
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("fileUuid", fileUuid);
        map.put("fileVersionId", fileVersionId);
//        amqUtils.sendMsgToMq("发送删除搜索和索引信息", fileSearchIndexDelMq, JSON.toJSONString(map));
        return TResponseVo.success("删除成功");
    }

//    /**
//     * 添加标注
//     *
//     * @param docTaggingPojo
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo AddTaggingService(DocTaggingPojo docTaggingPojo) throws Exception {
//        docTaggingPojo.setCreateTime(new Date());
//        Integer i = 0;
//        try {
//            String docStr = fileIndexMapper.selectDocStrDao(docTaggingPojo.getFileUuid(), docTaggingPojo.getFileVersionId());
//            JSONObject jsonObject = JSON.parseObject(docStr);
//            String fileParagraphId = docTaggingPojo.getFileParagraphId();
//            JSONObject jsonObject1 = DocFrameUtils.parseTextStructure3(jsonObject,"id",fileParagraphId);
//            String text = DocFrameUtils.parseTextStructure2(jsonObject1);
//            docTaggingPojo.setFileParagraphContent(text);
//            i = fileIndexMapper.deleteTaggingRecoDao(docTaggingPojo.getFileUuid(),docTaggingPojo.getFileVersionId(), fileParagraphId);
//            i = fileIndexMapper.insertFileTaggingDao(docTaggingPojo);
//            DocLogSendPojo docLogSendPojo = new DocLogSendPojo();
//            docLogSendPojo.setLogType("3");
//            docLogSendPojo.setTagging(docTaggingPojo);
////            amqUtils.sendMsgToMq("发送标注历史日志", fileSearchLogMq, JSON.toJSONString(docLogSendPojo));
//        } catch (Exception e) {
//            FileIndexException err = new FileIndexException();
//            err.setCode(8);
//            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "AddTaggingService", "新增标注", "新增标注错误");
//            throw err;
//        }
//        return TResponseVo.success("新增标注成功");
//    }

//    /**
//     * 查询标注
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @param fileParagraphId
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo GetTaggingService(String fileUuid, String fileVersionId, String fileParagraphId) throws Exception {
//        DocTaggingPojo docTaggingPojo = fileIndexMapper.selectTaggingInfoDao(fileUuid, fileVersionId, fileParagraphId);
//        Map map = JSON.parseObject(JSON.toJSONStringWithDateFormat(docTaggingPojo,"yyyy-MM-dd HH:mm:ss"), Map.class);
//        HashMap<String, String> map1 = new HashMap<>();
//        map1.put("tipsUuid",docTaggingPojo.getTipsUuid());
//        map1.put("tipsName",docTaggingPojo.getTipsName());
//        map1.put("tipsDesc",docTaggingPojo.getTipsDesc());
//        HashMap<String, String> map2 = new HashMap<>();
//        map2.put("fileParagraphId",docTaggingPojo.getFileParagraphId());
//        map2.put("paragraphInterpretation",docTaggingPojo.getParagraphInterpretation());
//        map.put("tips",map1);
//        map.put("content",map2);
//        return TResponseVo.success(map);
//    }

//    /**
//     * 获取查询历史标注
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @param fileParagraphId
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo GetTaggingHisService(String fileUuid, String fileVersionId, String fileParagraphId) throws Exception {
//        List<DocTaggingPojo> docTaggingPojos = fileIndexMapper.selectTaggingHisDao(fileUuid, fileVersionId, fileParagraphId);
//        List<Map> result = new ArrayList<>();
//        for (DocTaggingPojo docTaggingPojo : docTaggingPojos) {
//            Map map = JSON.parseObject(JSON.toJSONStringWithDateFormat(docTaggingPojo,"yyyy-MM-dd HH:mm:ss"), Map.class);
//            HashMap<String, String> map1 = new HashMap<>();
//            map1.put("tipsUuid",docTaggingPojo.getTipsUuid());
//            map1.put("tipsName",docTaggingPojo.getTipsName());
//            map1.put("tipsDesc",docTaggingPojo.getTipsDesc());
//            HashMap<String, String> map2 = new HashMap<>();
//            map2.put("fileParagraphId",docTaggingPojo.getFileParagraphId());
//            map2.put("paragraphInterpretation",docTaggingPojo.getParagraphInterpretation());
//            map.put("tips",map1);
//            map.put("content",map2);
//            result.add(map);
//        }
//        return TResponseVo.success(result);
//    }


//    /**
//     * 获取首页文档推荐
//     *
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo getHomePageDocumentListService() throws Exception {
//        List<DocFileIndexPojo> homePageDocumentListDao = fileIndexMapper.getHomePageDocumentListDao();
//        return TResponseVo.success(homePageDocumentListDao);
//    }
}
