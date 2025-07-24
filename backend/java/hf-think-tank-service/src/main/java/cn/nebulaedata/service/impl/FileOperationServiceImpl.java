package cn.nebulaedata.service.impl;


import cn.nebulaedata.dao.FileIndexMapper;
import cn.nebulaedata.dao.FileOperationMapper;
import cn.nebulaedata.dao.SplitTableMapper;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.enums.ResponseEnum;
import cn.nebulaedata.exception.FileIndexException;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.form.DocParamsForm;
import cn.nebulaedata.pojo.*;
import cn.nebulaedata.service.FileOperationService;
import cn.nebulaedata.utils.*;
import cn.nebulaedata.vo.TResponseVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static cn.nebulaedata.utils.JsonKeyUtils.encodeURI;
import static cn.nebulaedata.utils.TimeFormatUtils.getAgo;

/**
 * @author 贾亦真
 * @date 2020/12/21 11:29
 * @note
 */
@Service
public class FileOperationServiceImpl implements FileOperationService {
    private static final Logger LOG = LoggerFactory.getLogger(FileOperationServiceImpl.class);

    @Autowired
    private FileOperationMapper fileOperationMapper;
    @Autowired
    private SplitTableMapper splitTableMapper;
    @Autowired
    private AmqUtils amqUtils;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private WorkingTableMapper workingTableMapper;
    @Autowired
    private FileIndexMapper fileIndexMapper;
    @Value("${doc-frame-service.env-name}")
    private String envName;
    @Value("${do-post.dfb-url}")
    private String dfbUrl;
    @Value("${doc-frame-service.compare-path}")
    private String comparePath;
    @Value("${doc-frame-service.compare-path-local}")
    private String comparePathLocal;
    @Value("${split-database.dm-content-data}")
    private String dmContentDataDatabase;


    private final AtomicBoolean compareLock = new AtomicBoolean(true);  // 对比更新锁
    CancellableThread<Object> objectCancellableThread;

    /**
     * 新增收藏
     *
     * @param userId
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo addCollectionService(String userId, String fileUuid, String collectionType) throws Exception {
        try {
            int j = fileOperationMapper.checkCollectionDao(userId, fileUuid);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", fileUuid, "当前类错误码-321-收藏重复");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(321);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", fileUuid, "当前类错误码-322-检查收藏重复SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(322);
            throw questionnaireException;
        }
        fileOperationMapper.insertCollectionDao(userId, fileUuid, new Date(), collectionType);
        return TResponseVo.success("收藏成功");
    }

    /**
     * 取消收藏
     *
     * @param userId
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delCollectionService(String userId, String fileUuid) throws Exception {
        Integer i = 0;
        i = fileOperationMapper.deleteCollectionDao(userId, fileUuid);
        return TResponseVo.success("取消收藏");
    }

    /**
     * 我的收藏
     *
     * @param isPaged
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo myCollectionService(String collectionType, Integer pageNum, Integer pageSize, String isPaged, String userId, String searchLike) throws Exception {
        List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
        if (StringUtils.isNotBlank(searchLike)) {
            searchLike = searchLike.replaceAll("'", "");
        }

        List<DocFileIndexPojo> docFileIndexPojos = fileOperationMapper.selectMyCollectFileDao(collectionType, userId, searchLike);
        ArrayList<String> strings = new ArrayList<>();
        for (DocFileIndexPojo docFileIndexPojo : docFileIndexPojos) {
            strings.add(docFileIndexPojo.getFileUuid());
        }
        List<DocFileIndexPojo> fileAllInfoListDao = workingTableMapper.getFileAllInfoListDao(new JsonKeyUtils().listToString(strings, "','"));

        for (DocFileIndexPojo docParamsPojo : fileAllInfoListDao) {  // 设置文件类型名称
            docParamsPojo.setCollection(true);
            for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                    docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                    break;
                }
            }
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(fileAllInfoListDao);
        }


        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(fileAllInfoListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", fileAllInfoListDao.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 版本记录
     */
    /**
     * 读取版本记录列表
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getVersionListService(String fileUuid) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
//            docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        }
        List<DocFileVerIndexPojo> versionListDao = fileOperationMapper.getVersionListDao(fileUuid);
        for (DocFileVerIndexPojo docFileVerIndexPojo : versionListDao) {
            Date createTime = docFileVerIndexPojo.getCreateTime();
            String ago = new TimeFormatUtils().getAgo(createTime);
            docFileVerIndexPojo.setTimeText(ago);
//            str转list
            String compareInfo = (String) docFileVerIndexPojo.getCompareInfo();
            List list = JSON.parseObject(compareInfo, List.class);
            docFileVerIndexPojo.setCompareInfo(list);
        }
        return TResponseVo.success(versionListDao);
    }

    /**
     * 读取版本详细信息
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getVersionDetailService(String fileUuid, String fileVersionId) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
//            docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        }
        if (StringUtils.isBlank(fileVersionId)) {
            fileVersionId = fileOperationMapper.getLastVersionIdDao(fileUuid);
        }
        DocFileVerIndexPojo versionDetailDao = fileOperationMapper.getVersionDetailDao(fileUuid, fileVersionId);
        Date createTime = versionDetailDao.getCreateTime();
        versionDetailDao.setSubmitTime(createTime);
        versionDetailDao.setConfirmTime(createTime);
        //            str转list
        String compareInfo = (String) versionDetailDao.getCompareInfo();
        List<Map<String, String>> list = JSON.parseObject(compareInfo, List.class);
        versionDetailDao.setCompareInfo(list);

        // 补充AuditingContent 变更信息列表
        String annotate = (String) versionDetailDao.getAnnotate();
        List<Map<String, String>> annotateList = JSON.parseObject(annotate, List.class);
        ArrayList<LabelValuePojo> auditingContent = new ArrayList<>();
        if (list != null) {
            for (Map<String, String> map : list) {
                LabelValuePojo labelValuePojo = new LabelValuePojo();
                labelValuePojo.setLabel(map.get("label"));
                labelValuePojo.setValue(map.get("uuid"));
                if (annotateList != null) {
                    for (Map<String, String> stringStringMap : annotateList) {
                        if (stringStringMap.get("label").equals(map.get("outlineId"))) {
                            labelValuePojo.setAnnotate(stringStringMap.get("value"));
                            break;
                        }
                    }
                }
                auditingContent.add(labelValuePojo);
            }
        }
        versionDetailDao.setCompareInfo(null);
        versionDetailDao.setAnnotate(null);
        versionDetailDao.setAuditingContent(auditingContent);
        return TResponseVo.success(versionDetailDao);
    }

    /**
     * 文件血缘
     */
    /**
     * 点击派生关系
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getVersionTreeService(String fileUuid) throws Exception {
        // 获取父
        String fileParentId = fileOperationMapper.getFileParentId(fileUuid);

        // 获取当前文件信息
        DocFileIndexPojo fileInfo = fileOperationMapper.getDocAllInfoDao(fileUuid);

        // 获取子
        List<DocFileIndexPojo> fileChildDao = fileOperationMapper.getFileChildDao(fileUuid);

        // 当前节点信息
        HashMap<String, Object> fileInfoMap = new HashMap<>();
        fileInfoMap.put("id", fileInfo.getFileUuid());
        fileInfoMap.put("fileUuid", fileInfo.getFileUuid());
        fileInfoMap.put("fileVersionId", fileInfo.getFileVersionId());
        fileInfoMap.put("fileType", fileInfo.getFileTypeId());
        HashMap<String, Object> valueMap = new HashMap<>();
        valueMap.put("title", fileInfo.getFileName());
        valueMap.put("main", true);
        List<Map<String, Object>> itemsList = new ArrayList<>();
        itemsList.add(new HashMap<String, Object>() {{
            put("text", fileInfo.getCreateUserName());
        }});  // 用户
        itemsList.add(new HashMap<String, Object>() {{
            put("text", getAgo(fileInfo.getCreateTime()));
        }});  // 创建时间
        valueMap.put("items", itemsList);
        fileInfoMap.put("value", valueMap);

        // 子节点信息
        ArrayList<HashMap<String, Object>> children = new ArrayList<>();
        for (DocFileIndexPojo docFileIndexPojo : fileChildDao) {
            HashMap<String, Object> childFileInfoMap = new HashMap<>();
            childFileInfoMap.put("id", docFileIndexPojo.getFileUuid());
            childFileInfoMap.put("fileUuid", docFileIndexPojo.getFileUuid());
            childFileInfoMap.put("fileVersionId", docFileIndexPojo.getFileVersionId());
            childFileInfoMap.put("fileType", docFileIndexPojo.getFileTypeId());
            HashMap<String, Object> childValueMap = new HashMap<>();
            childValueMap.put("title", docFileIndexPojo.getFileName());
            List<Map<String, Object>> childItemsList = new ArrayList<>();
            childItemsList.add(new HashMap<String, Object>() {{
                put("text", docFileIndexPojo.getCreateUserName());
            }});  // 用户
            childItemsList.add(new HashMap<String, Object>() {{
                put("text", getAgo(docFileIndexPojo.getCreateTime()));
            }});  // 创建时间
            childValueMap.put("items", childItemsList);
            childFileInfoMap.put("value", childValueMap);
            children.add(childFileInfoMap);
        }
        fileInfoMap.put("children", children);

        // 拼结果数据
        if (StringUtils.isNotBlank(fileParentId)) {
            DocFileIndexPojo fileParent = fileOperationMapper.getDocAllInfoDao(fileParentId);
            if (fileParent == null) {
                return TResponseVo.success(fileInfoMap);
            } else {
                HashMap<String, Object> parentFileInfoMap = new HashMap<>();
                parentFileInfoMap.put("id", fileParent.getFileUuid());
                parentFileInfoMap.put("fileUuid", fileParent.getFileUuid());
                parentFileInfoMap.put("fileVersionId", fileParent.getFileVersionId());
                parentFileInfoMap.put("fileType", fileParent.getFileTypeId());
                HashMap<String, Object> parentValueMap = new HashMap<>();
                parentValueMap.put("title", fileParent.getFileName());
                List<Map<String, Object>> parentItemsList = new ArrayList<>();
                parentItemsList.add(new HashMap<String, Object>() {{
                    put("text", fileParent.getCreateUserName());
                }});  // 用户
                parentItemsList.add(new HashMap<String, Object>() {{
                    put("text", getAgo(fileParent.getCreateTime()));
                }});  // 创建时间
                parentValueMap.put("items", parentItemsList);
                parentFileInfoMap.put("value", parentValueMap);
                ArrayList<HashMap<String, Object>> child = new ArrayList<>();
                child.add(fileInfoMap);
                parentFileInfoMap.put("children", child);
                return TResponseVo.success(parentFileInfoMap);
            }
        } else {
            return TResponseVo.success(fileInfoMap);
        }

    }

    /**
     * 完整派生关系
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getVersionCompleteTreeService(String fileUuid) throws Exception {
        // 获取父
        String fileParentId = fileOperationMapper.getFileParentId(fileUuid);

        // 获取当前文件信息
        DocFileIndexPojo fileInfo = fileOperationMapper.getDocAllInfoDao(fileUuid);

        // 获取子
        List<DocFileIndexPojo> fileChildDao = fileOperationMapper.getFileChildDao(fileUuid);

        // 当前节点信息
        HashMap<String, Object> fileInfoMap = new HashMap<>();
        fileInfoMap.put("id", fileInfo.getFileUuid());
        HashMap<String, Object> valueMap = new HashMap<>();
        valueMap.put("title", fileInfo.getFileName());
        valueMap.put("main", true);
        List<Map<String, Object>> itemsList = new ArrayList<>();
        itemsList.add(new HashMap<String, Object>() {{
            put("text", fileInfo.getCreateUserName());
        }});  // 用户
        itemsList.add(new HashMap<String, Object>() {{
            put("text", getAgo(fileInfo.getCreateTime()));
        }});  // 创建时间
        valueMap.put("items", itemsList);
        fileInfoMap.put("value", valueMap);

        // 子节点信息
        ArrayList<HashMap<String, Object>> children = new ArrayList<>();
        for (DocFileIndexPojo docFileIndexPojo : fileChildDao) {
            HashMap<String, Object> childFileInfoMap = new HashMap<>();
            childFileInfoMap.put("id", docFileIndexPojo.getFileUuid());
            HashMap<String, Object> childValueMap = new HashMap<>();
            childValueMap.put("title", docFileIndexPojo.getFileName());
            List<Map<String, Object>> childItemsList = new ArrayList<>();
            childItemsList.add(new HashMap<String, Object>() {{
                put("text", docFileIndexPojo.getCreateUserName());
            }});  // 用户
            childItemsList.add(new HashMap<String, Object>() {{
                put("text", getAgo(docFileIndexPojo.getCreateTime()));
            }});  // 创建时间
            childValueMap.put("items", childItemsList);
            childFileInfoMap.put("value", childValueMap);
            children.add(childFileInfoMap);
        }
        fileInfoMap.put("children", children);

        // 防止无限循环
        int i = 0;
        while (StringUtils.isNotBlank(fileParentId)) {
            if (i > 20) break;
            i++;
            DocFileIndexPojo fileParent = fileOperationMapper.getDocAllInfoDao(fileParentId);
            if (fileParent == null) {
                return TResponseVo.success(fileInfoMap);
            } else {
                HashMap<String, Object> parentFileInfoMap = new HashMap<>();
                parentFileInfoMap.put("id", fileParent.getFileUuid());
                HashMap<String, Object> parentValueMap = new HashMap<>();
                parentValueMap.put("title", fileParent.getFileName());
                List<Map<String, Object>> parentItemsList = new ArrayList<>();
                parentItemsList.add(new HashMap<String, Object>() {{
                    put("text", fileParent.getCreateUserName());
                }});  // 用户
                parentItemsList.add(new HashMap<String, Object>() {{
                    put("text", getAgo(fileParent.getCreateTime()));
                }});  // 创建时间
                parentValueMap.put("items", parentItemsList);
                parentFileInfoMap.put("value", parentValueMap);
                ArrayList<HashMap<String, Object>> child = new ArrayList<>();
                child.add(fileInfoMap);
                parentFileInfoMap.put("children", child);
                fileInfoMap = JSON.parseObject(JSON.toJSONString(parentFileInfoMap), HashMap.class);
                fileParentId = fileParent.getFileParentId();
            }
        }
        return TResponseVo.success(fileInfoMap);

//        return TResponseVo.success(fileOperationMapper.getVersionCompleteTreeDao(fileUuid));
    }


    /**
     * 读取版本记录快照(文件预览)
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getVersionPhotoService(String fileUuid, String fileVersionId) throws Exception {
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        String fileStatus = docAllInfoDao.getFileStatus();
        if ("3".equals(fileStatus)) {
            throw new WorkTableException("该文档已被删除，无法预览");
        }
        if (StringUtils.isBlank(fileVersionId)) {
            fileVersionId = fileOperationMapper.getLastVersionIdDao(fileUuid);
        }
        List<OutLinePojo> versionPhotoDao = fileOperationMapper.getVersionPhotoDao(fileUuid, fileVersionId);
        String contentText = "";
        for (OutLinePojo outLinePojo : versionPhotoDao) {
            contentText = contentText + outLinePojo.getContentText();
            if (outLinePojo.getOutlineText().equals("封面信息")) {
                contentText = contentText + "<pageline />";
            }
        }
        // 获取文件名与文件类型

        String fileName = docAllInfoDao.getFileName();
        String fileTypeId = docAllInfoDao.getFileTypeId();
        String fileTypeName = fileOperationMapper.getFileTypeNameDao(fileTypeId);
        DocFileVerIndexPojo versionInfoDao = fileOperationMapper.getVersionInfoDao(fileUuid, fileVersionId);
        Map<String, String> ret = new HashMap<>();
        ret.put("contentText", contentText);
        ret.put("fileName", fileName);
        ret.put("fileTypeId", fileTypeId);
        ret.put("fileTypeName", fileTypeName);
        ret.put("fileVersionName", versionInfoDao.getFileVersionName());
        return TResponseVo.success(ret);
    }


    /**
     * 通知子文件变更(并非直接通知 而是将变化记录在hf_update_info表中)
     *
     * @param fileUuid
     * @return
     */
    public TResponseVo newUpdateInfoService(String fileUuid, List<Map<String, String>> updateInfoList, String userId) {

        // 获取需要通知的子文件清单
        List<String> pushFileUuidList = fileOperationMapper.getPushFileUuidInfoDao(fileUuid);
        if (pushFileUuidList.size() == 0) {
            return TResponseVo.success("已通知子文件更新");
        }
        String sql = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(new Date());
        for (Map<String, String> updateInfo : updateInfoList) {
            for (String pushFileUuid : pushFileUuidList) {
                sql = sql + "(\"" + UUID.randomUUID().toString().replaceAll("-", "") + "\",\"" + pushFileUuid + "\",\"" + fileUuid + "\",\"" + updateInfo.get("updateOutlineId") + "\",\"" + updateInfo.get("updateOutlineIdAct") + "\",\"" + userId + "\",\"" + dateString + "\"),";
            }
        }
        if (sql.length() != 0) {
            sql = sql.substring(0, sql.length() - 1);
//            System.out.println("-------------------------");
//            System.out.println(sql);
            fileOperationMapper.insertUpdateInfoDao(sql);
        }
        return TResponseVo.success("已通知子文件更新");

    }

    /**
     * 获取父文件更新信息
     *
     * @param fileUuid 当前文件的uuid
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getUpdateInfoService(String fileUuid) throws Exception {
        String fileParentId = fileOperationMapper.getFileParentId(fileUuid);
        return TResponseVo.success(fileOperationMapper.getUpdateInfoDao(fileUuid, fileParentId));
    }

    /**
     * 确认更新信息
     *
     * @param updateUuid
     * @param status
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo confirmUpdateInfoService(String updateUuid, String status, String userId) throws Exception {
        fileOperationMapper.confirmUpdateInfoDao(updateUuid, status, userId);
        String flag = "";
        if (status.equals("1")) {
            flag = "已确认";
        } else if (status.equals("1")) {
            flag = "已忽略";
        }
        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", flag);
        return TResponseVo.success(ret);
    }


    /**
     * 获取法律法规更新信息
     *
     * @param fileUuid 当前文件的uuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getLawUpdateService(String fileUuid) throws Exception {
        return TResponseVo.success(fileOperationMapper.getLawUpdateDao(fileUuid));
    }

    /**
     * 获取子文件清单
     *
     * @param fileUuid
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getDerivationListService(String fileUuid) throws Exception {
        List<DocFileIndexPojo> derivationListDao = fileOperationMapper.getDerivationListDao(fileUuid);
        Integer derivationNumberDao = derivationListDao.size();
        HashMap<Object, Object> ret = new HashMap<>();
        ret.put("fileList", derivationListDao);
        ret.put("cntNumber", derivationNumberDao);
        return TResponseVo.success(ret);
    }

    /**
     * 获取被应用清单
     *
     * @param fileUuid
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getUseListService(String fileUuid) throws Exception {
        List<OutLinePojo> useListDao = fileOperationMapper.getUseListDao(fileUuid);
        Integer useNumberDao = useListDao.size();
//        Integer useNumberDao = fileOperationMapper.getUseNumberDao(fileUuid);
        HashMap<Object, Object> ret = new HashMap<>();
        ret.put("fileList", useListDao);
        ret.put("cntNumber", useNumberDao);
        return TResponseVo.success(ret);
    }


    /**
     * 新增参数
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo addParamService(DocParamsPojo docParamsPojo, String userId) throws Exception {
        // 校验参数名重复
        String paramsName = docParamsPojo.getParamsName();
        String paramsClassify = docParamsPojo.getParamsClassify();
        String paramsUseSaturation = docParamsPojo.getParamsUseSaturation();
        DocUserPojo userInfoDao = fileOperationMapper.getUserInfoDao(userId);
        String companyId = userInfoDao.getCompanyId();
        Integer integer = fileOperationMapper.checkParamNameDao(paramsName, paramsClassify, userId, companyId, paramsUseSaturation, null, null, null);
        if (integer >= 1) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "参数名已重复");
        }
        String paramsUuid = UUID.randomUUID().toString().replaceAll("-", "");
        docParamsPojo.setParamsUuid(paramsUuid);
        docParamsPojo.setCreateTime(new Date());
        docParamsPojo.setCreateUserId(userId);
        docParamsPojo.setCreateCompanyId(companyId);
        // 判断是否文内添加 (fileUuid不为空)
        if (!StringUtils.isBlank(docParamsPojo.getFileUuid())) {  // 记录来源
            docParamsPojo.setOriFileUuid(docParamsPojo.getFileUuid());
            String fileName = fileOperationMapper.getFileNameByIdDao(docParamsPojo.getFileUuid());
            docParamsPojo.setOriFileName(fileName);
        }
        // 值域
        List<Object> paramsRangeList = docParamsPojo.getParamsRangeList();
        String paramsRange = JSON.toJSONString(paramsRangeList);
        docParamsPojo.setParamsRange(paramsRange);
        docParamsPojo.setParamsRangeList(null);
        // 矩阵参数头
        List paramsColumns = (List) docParamsPojo.getParamsColumns();
        String paramsColumnsStr = JSON.toJSONString(paramsColumns);
        docParamsPojo.setParamsColumns(paramsColumnsStr);
        // 分组
        List<Object> paramsGroupIdList = docParamsPojo.getParamsGroupIdList();
        String paramsGroupId = JSON.toJSONString(paramsGroupIdList);
        docParamsPojo.setParamsGroupId(paramsGroupId);
        docParamsPojo.setParamsGroupIdList(null);
        // 来自于
        List comeFromList = (List) docParamsPojo.getComeFromList();
        String comeFromListStr = JSON.toJSONString(comeFromList);
        docParamsPojo.setComeFromList(comeFromListStr);
        // 金额类型参数根据styleId指定unit
        if (docParamsPojo.getParamsTypeId().equals("80")) {
            String styleId = docParamsPojo.getStyleId();
            if ("632007".equals(styleId) || "632008".equals(styleId) || "632010".equals(styleId) || "632012".equals(styleId) || "632013".equals(styleId) || "632014".equals(styleId)) {
                docParamsPojo.setUnit("770001");  // 元
            } else if ("632009".equals(styleId) || "632011".equals(styleId) || "6320071".equals(styleId)) {
                docParamsPojo.setUnit("770002");  // 万元
            }
        }

        // 设置double格式
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setGroupingUsed(false);  // 不使用科学计数法
        nf.setMaximumFractionDigits(13);  // 允许的最大小数位数
        // 确认参数类型
//        10  单选
//        20  多选
//        30  日期/时间
//        40  文本
//        50  富文本
//        60  图片
//        70  资源附件
//        80  金额
//        90  数值
        String paramsTypeId = docParamsPojo.getParamsTypeId();  // 参数类型
//        String paramsRange = "";  // 所有选项及答案
        String paramsChoose = "";  // 选项对应的内容
        String newParamsText = ""; // 更新到content中的值
//        List<Map<String, String>> paramsColumns = null; // 矩阵参数表头
        String matrixDisplay = ""; // 矩阵参数显示效果
        List<Map<String, String>> matrixDisplayList = new ArrayList<>();  // 暂时装载从feContentText解析出来的uuid对应的display

        // 第一步 更新参数表
        try {
            if (paramsTypeId.equals("40") || paramsTypeId.equals("30")) {  // 文本 时间
                // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段

            } else if (paramsTypeId.equals("10")) {  // 单选

            } else if (paramsTypeId.equals("20")) {  // 多选
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("80")) {  // 金额
                if (docParamsPojo.getDefaultValue() != null) {
                    Map<String, Object> defaultValue = (Map<String, Object>) (Map) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("90")) {  // 数值
                if (docParamsPojo.getDefaultValue() != null) {
                    Map<String, Object> defaultValue = (Map<String, Object>) (Map) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("70")) {  // 附件
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("60")) {  // 图片
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("50")) {  // 富文本

            } else if (paramsTypeId.equals("95")) { // 矩阵
                if (docParamsPojo.getDefaultValue() != null) {
                    List<Map<String, String>> defaultValue = (List<Map<String, String>>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            }
        } catch (Exception e) {

        }
        try {
            fileOperationMapper.insertParamDao(docParamsPojo);
        } catch (DataIntegrityViolationException e) {
            throw new WorkTableException("参数名称过长,请小于64个字符");
        }

        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "新增参数成功");
        ret.put("paramsUuid", paramsUuid);
        return TResponseVo.success(ret);
    }

    /**
     * 新增参数
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo addParamService2(DocParamsPojo docParamsPojo, String userId) throws Exception {
        // 校验参数名重复
        String paramsName = docParamsPojo.getParamsName();
        String paramsClassify = docParamsPojo.getParamsClassify();
        // 文件id
        String fileUuid = docParamsPojo.getFileUuid();
        String fileVersionId = docParamsPojo.getFileVersionId();
        String paramsUseSaturation = docParamsPojo.getParamsUseSaturation();
        DocUserPojo userInfoDao = fileOperationMapper.getUserInfoDao(userId);
        String companyId = userInfoDao.getCompanyId();
        Integer integer = fileOperationMapper.checkParamNameDao(paramsName, paramsClassify, userId, companyId, paramsUseSaturation, null, fileUuid, fileVersionId);
        if (integer >= 1) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "参数名已重复");
        }
        String paramsUuid = UUID.randomUUID().toString().replaceAll("-", "");
        docParamsPojo.setParamsUuid(paramsUuid);
        docParamsPojo.setCreateTime(new Date());
        docParamsPojo.setCreateUserId(userId);
        docParamsPojo.setCreateCompanyId(companyId);
        // 判断是否文内添加 (fileUuid不为空)
        if (!StringUtils.isBlank(docParamsPojo.getFileUuid())) {  // 记录来源
            docParamsPojo.setOriFileUuid(docParamsPojo.getFileUuid());
            String fileName = fileOperationMapper.getFileNameByIdDao(docParamsPojo.getFileUuid());
            docParamsPojo.setOriFileName(fileName);
        }
        // 值域
        List<Object> paramsRangeList = docParamsPojo.getParamsRangeList();
        String paramsRange = JSON.toJSONString(paramsRangeList);
        docParamsPojo.setParamsRange(paramsRange);
        docParamsPojo.setParamsRangeList(null);
        // 矩阵参数头
        List paramsColumns = (List) docParamsPojo.getParamsColumns();
        String paramsColumnsStr = JSON.toJSONString(paramsColumns);
        docParamsPojo.setParamsColumns(paramsColumnsStr);
        // 分组
        List<Object> paramsGroupIdList = docParamsPojo.getParamsGroupIdList();
        String paramsGroupId = JSON.toJSONString(paramsGroupIdList);
        docParamsPojo.setParamsGroupId(paramsGroupId);
        docParamsPojo.setParamsGroupIdList(null);
        // 来自于
        List comeFromList = (List) docParamsPojo.getComeFromList();
        String comeFromListStr = JSON.toJSONString(comeFromList);
        docParamsPojo.setComeFromList(comeFromListStr);
        // 金额类型参数根据styleId指定unit
        if (docParamsPojo.getParamsTypeId().equals("80")) {
            String styleId = docParamsPojo.getStyleId();
            if ("632007".equals(styleId) || "632008".equals(styleId) || "632010".equals(styleId) || "632012".equals(styleId) || "632013".equals(styleId) || "632014".equals(styleId)) {
                docParamsPojo.setUnit("770001");  // 元
            } else if ("632009".equals(styleId) || "632011".equals(styleId) || "6320071".equals(styleId)) {
                docParamsPojo.setUnit("770002");  // 万元
            }
        }

        // 设置double格式
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setGroupingUsed(false);  // 不使用科学计数法
        nf.setMaximumFractionDigits(13);  // 允许的最大小数位数
        // 确认参数类型
//        10  单选
//        20  多选
//        30  日期/时间
//        40  文本
//        50  富文本
//        60  图片
//        70  资源附件
//        80  金额
//        90  数值
        String paramsTypeId = docParamsPojo.getParamsTypeId();  // 参数类型
//        String paramsRange = "";  // 所有选项及答案
        String paramsChoose = "";  // 选项对应的内容
        String newParamsText = ""; // 更新到content中的值
//        List<Map<String, String>> paramsColumns = null; // 矩阵参数表头
        String matrixDisplay = ""; // 矩阵参数显示效果
        List<Map<String, String>> matrixDisplayList = new ArrayList<>();  // 暂时装载从feContentText解析出来的uuid对应的display

        // 第一步 更新参数表
        try {
            if (paramsTypeId.equals("40") || paramsTypeId.equals("30")) {  // 文本 时间
                // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段

            } else if (paramsTypeId.equals("10")) {  // 单选

            } else if (paramsTypeId.equals("20")) {  // 多选
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("80")) {  // 金额
                if (docParamsPojo.getDefaultValue() != null) {
                    Map<String, Object> defaultValue = (Map<String, Object>) (Map) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("90")) {  // 数值
                if (docParamsPojo.getDefaultValue() != null) {
                    Map<String, Object> defaultValue = (Map<String, Object>) (Map) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("70")) {  // 附件
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("60")) {  // 图片
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("50")) {  // 富文本

            } else if (paramsTypeId.equals("95")) { // 矩阵
                if (docParamsPojo.getDefaultValue() != null) {
                    List<Map<String, String>> defaultValue = (List<Map<String, String>>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            }
        } catch (Exception e) {

        }
        try {
            fileOperationMapper.insertParamDao(docParamsPojo);
        } catch (DataIntegrityViolationException e) {
            throw new WorkTableException("参数名称过长,请小于64个字符");
        }

        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "新增参数成功");
        ret.put("paramsUuid", paramsUuid);
        return TResponseVo.success(ret);
    }

    /**
     * 参数类型维表
     */
    @Override
    public TResponseVo getParamTypeDiService() throws Exception {
        List<LabelValuePojo> paramTypeDiDao = fileOperationMapper.getParamTypeDiDao();
        return TResponseVo.success(paramTypeDiDao);
    }

    /**
     * 参数填参角色维表
     */
    @Override
    public TResponseVo getParamSceneDiService() throws Exception {
        List<LabelValuePojo> paramSceneDiDao = fileOperationMapper.getParamSceneDiDao();
        return TResponseVo.success(paramSceneDiDao);
    }
//
//    /**
//     * 参数数据来源维表
//     */
//    @Override
//    public TResponseVo getParamSourceDiService() throws Exception {
//        List<LabelValuePojo> paramSourceDiDao = fileOperationMapper.getParamSourceDiDao();
//        return TResponseVo.success(paramSourceDiDao);
//    }

    /**
     * 参数数据来源维表
     */
    @Override
    public TResponseVo getParamGroupDiService(String fileUuid, String fileVersionId) throws Exception {
        List<LabelValuePojo> paramGroupDiDao = fileOperationMapper.getParamGroupDiDao();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.success(paramGroupDiDao);
        }
        List<DocParamsPojo> contentAllParamInfoDao1 = fileOperationMapper.getContentAllParamInfoDao1(fileUuid, fileVersionId);
        List<String> groupIdList = new ArrayList<>();
        for (DocParamsPojo docParamsPojo : contentAllParamInfoDao1) {
            String paramsGroupId = docParamsPojo.getParamsGroupId();
            List<String> docParamList = JSON.parseObject(paramsGroupId, List.class);
            if (docParamList == null) {
                continue;
            }
            for (String groupId : docParamList) {
                if (groupIdList.contains(groupId)) {
                    continue;
                } else {
                    groupIdList.add(groupId);
                }
            }
        }
        // 根据所有groupId的list 筛选分组
        List<LabelValuePojo> paramGroupDiDaoTmp = new ArrayList<>();
        for (LabelValuePojo labelValuePojo : paramGroupDiDao) {
            for (String groupId : groupIdList) {
                if (labelValuePojo.getValue().equals(groupId)) {
                    paramGroupDiDaoTmp.add(labelValuePojo);
                    break;
                }
            }
        }
        return TResponseVo.success(paramGroupDiDaoTmp);
    }

    /**
     * 新增分组
     */
    @Override
    public TResponseVo addParamGroupService(DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        String groupName = docParamsGroupPojo.getGroupName();
        Integer integer = fileOperationMapper.checkParamGroupNameDao(groupName, null);
        if (integer >= 1) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "分组名已存在");
        }
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        docParamsGroupPojo.setGroupId(uuid);
        docParamsGroupPojo.setCreateTime(new Date());
        Integer paramGroupCntDao = fileOperationMapper.getParamGroupCntDao();  // 顺序字段
        docParamsGroupPojo.setOrder(String.valueOf(paramGroupCntDao));
        fileOperationMapper.addParamGroupDao(docParamsGroupPojo);
        return TResponseVo.success("添加成功");
    }

    /**
     * 删除分组来源维表
     */
    @Override
    public TResponseVo delParamGroupService(DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        String groupId = docParamsGroupPojo.getGroupId();
        // 删参数库分组
        List<DocParamsPojo> paramGroupUseInfoDao = fileOperationMapper.getParamGroupUseInfoDao(groupId);
        for (DocParamsPojo docParamsPojo : paramGroupUseInfoDao) {
            String paramsGroupId = docParamsPojo.getParamsGroupId();
            List<String> docParamList = JSON.parseObject(paramsGroupId, List.class);
            List<String> docParamList1 = new ArrayList<>();
            docParamList1.addAll(docParamList);
            for (String s : docParamList) {
                if (s.equals(groupId)) {
                    docParamList1.remove(s);
                    String s1 = JSON.toJSONString(docParamList1);
                    fileOperationMapper.updateParamGroupIdDao(docParamsPojo.getParamsUuid(), s1);
                    break;
                }
            }
        }
        // 删文内参数分组
        List<DocParamsPojo> paramGroupUseInfoDao2 = fileOperationMapper.getParamGroupUseInfoDao2(groupId);
        for (DocParamsPojo docParamsPojo : paramGroupUseInfoDao2) {
            String paramsGroupId = docParamsPojo.getParamsGroupId();
            List<String> docParamList = JSON.parseObject(paramsGroupId, List.class);
            List<String> docParamList1 = new ArrayList<>();
            docParamList1.addAll(docParamList);
            for (String s : docParamList1) {
                if (s.equals(groupId)) {
                    docParamList1.remove(s);
                    String s1 = JSON.toJSONString(docParamList1);
                    fileOperationMapper.updateParamGroupIdDao2(docParamsPojo.getUuid(), s1);
                    break;
                }
            }
        }
        fileOperationMapper.delParamGroupDao(docParamsGroupPojo);
        return TResponseVo.success("删除成功");
    }

    /**
     * 修改分组来源维表
     */
    @Override
    public TResponseVo updateParamGroupService(DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        String groupName = docParamsGroupPojo.getGroupName();
        String groupId = docParamsGroupPojo.getGroupId();
        Integer integer = fileOperationMapper.checkParamGroupNameDao(groupName, groupId);
        if (integer >= 1) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "分组名已存在");
        }
        fileOperationMapper.updateParamGroupDao(docParamsGroupPojo);
        return TResponseVo.success("更新成功");
    }

    /**
     * 获取某一分组信息
     *
     * @param docParamsGroupPojo
     */
    @Override
    public TResponseVo getParamGroupInfoService(DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        String groupId = docParamsGroupPojo.getGroupId();
        DocParamsGroupPojo paramGroupInfoDao = fileOperationMapper.getParamGroupInfoDao(groupId);
        return TResponseVo.success(paramGroupInfoDao);
    }

    /**
     * 获取分组list
     *
     * @param
     */
    @Override
    public TResponseVo getParamGroupListService(PagePojo pagePojo) throws Exception {
        String paramNameLike = pagePojo.getParamNameLike();
        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        List<DocParamsGroupPojo> docParamsGroupPojos = null;
        docParamsGroupPojos = fileOperationMapper.getParamGroupListDao(paramNameLike, null);
        return TResponseVo.success(docParamsGroupPojos);

    }

    /**
     * 参数分组排序
     *
     * @param docParamsGroupPojo
     */
    @Override
    public TResponseVo orderParamGroupService(DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        List<String> groupIdList = docParamsGroupPojo.getGroupIdList();
        if (groupIdList != null && groupIdList.size() != 0) {
            String sql = "update docframe_params_group_di\n" +
                    "set `order`=  case ";
            for (int i = 0; i < groupIdList.size(); i++) {
                sql = sql + " when data_group_id = '" + groupIdList.get(i) + "' then '" + String.valueOf(i) + "' ";
            }
            sql = sql + " end\n" +
                    "where 1=1";
            fileOperationMapper.orderParamGroupDao(sql);
            return TResponseVo.success("排序完成");
        } else {
            throw new WorkTableException("groupIdList为空");
        }

    }

    /**
     * 删除参数
     *
     * @param paramsUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delParamService(String paramsUuid) throws Exception {
        fileOperationMapper.deleteParamDao(paramsUuid);
        return TResponseVo.success("删除参数成功");
    }

    /**
     * 修改参数
     *
     * @param docParamsPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo updateParamService(DocParamsPojo docParamsPojo) throws Exception {
        // 校验参数名重复
        String paramsName = docParamsPojo.getParamsName();
        String paramsClassify = docParamsPojo.getParamsClassify();
        String userId = docParamsPojo.getUserId();
        String paramsUuid = docParamsPojo.getParamsUuid();
        String paramsUseSaturation = docParamsPojo.getParamsUseSaturation();
        DocUserPojo userInfoDao = fileOperationMapper.getUserInfoDao(userId);
        String companyId = userInfoDao.getCompanyId();
        Integer integer = fileOperationMapper.checkParamNameDao(paramsName, paramsClassify, userId, companyId, paramsUseSaturation, paramsUuid, null, null);
        if (integer >= 1) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "参数名已重复");
        }
        // 值域
        List paramsRangeList = docParamsPojo.getParamsRangeList();
        String paramsRange = JSON.toJSONString(paramsRangeList);
        docParamsPojo.setParamsRange(paramsRange);
        docParamsPojo.setParamsRangeList(null);
        // 矩阵参数头
        List paramsColumns = (List) docParamsPojo.getParamsColumns();
        String paramsColumnsStr = JSON.toJSONString(paramsColumns);
        docParamsPojo.setParamsColumns(paramsColumnsStr);
        // 分组
        List paramsGroupIdList = docParamsPojo.getParamsGroupIdList();
        String paramsGroupId = JSON.toJSONString(paramsGroupIdList);
        docParamsPojo.setParamsGroupId(paramsGroupId);
        docParamsPojo.setParamsGroupIdList(null);
        // 来自于
        List comeFromList = (List) docParamsPojo.getComeFromList();
        String comeFromListStr = JSON.toJSONString(comeFromList);
        docParamsPojo.setComeFromList(comeFromListStr);
        // 金额类型参数根据styleId指定unit
        if (docParamsPojo.getParamsTypeId().equals("80")) {
            String styleId = docParamsPojo.getStyleId();
            if ("632007".equals(styleId) || "632008".equals(styleId) || "632010".equals(styleId) || "632012".equals(styleId) || "632013".equals(styleId) || "632014".equals(styleId)) {
                docParamsPojo.setUnit("770001");  // 元
            } else if ("632009".equals(styleId) || "632011".equals(styleId) || "6320071".equals(styleId)) {
                docParamsPojo.setUnit("770002");  // 万元
            }
        }

        // 设置double格式
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setGroupingUsed(false);  // 不使用科学计数法
        nf.setMaximumFractionDigits(13);  // 允许的最大小数位数
        // 确认参数类型
//        10  单选
//        20  多选
//        30  日期/时间
//        40  文本
//        50  富文本
//        60  图片
//        70  资源附件
//        80  金额
//        90  数值
        String paramsTypeId = docParamsPojo.getParamsTypeId();  // 参数类型
//        String paramsRange = "";  // 所有选项及答案
        String paramsChoose = "";  // 选项对应的内容
        String newParamsText = ""; // 更新到content中的值
//        List<Map<String, String>> paramsColumns = null; // 矩阵参数表头
        String matrixDisplay = ""; // 矩阵参数显示效果
        List<Map<String, String>> matrixDisplayList = new ArrayList<>();  // 暂时装载从feContentText解析出来的uuid对应的display

        // 第一步 更新参数表
        try {
            if (paramsTypeId.equals("40") || paramsTypeId.equals("30")) {  // 文本 时间
                // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段

            } else if (paramsTypeId.equals("10")) {  // 单选

            } else if (paramsTypeId.equals("20")) {  // 多选
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("80")) {  // 金额
                if (docParamsPojo.getDefaultValue() != null) {
                    Map<String, Object> defaultValue = (Map<String, Object>) (Map) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("90")) {  // 数值
                if (docParamsPojo.getDefaultValue() != null) {
                    Map<String, Object> defaultValue = (Map<String, Object>) (Map) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("70")) {  // 附件
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("60")) {  // 图片
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("50")) {  // 富文本

            } else if (paramsTypeId.equals("95")) { // 矩阵
                if (docParamsPojo.getDefaultValue() != null) {
                    List<Map<String, String>> defaultValue = (List<Map<String, String>>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            }
        } catch (Exception e) {

        }
        try {
            fileOperationMapper.updateParamDao(docParamsPojo);
        } catch (DataIntegrityViolationException e) {
            throw new WorkTableException("参数名称过长,请小于64个字符");
        }
        return TResponseVo.success("修改参数成功");
    }

    /**
     * 获取参数信息
     *
     * @param paramsUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getParamInfoService(String paramsUuid) throws Exception {
        DocParamsPojo paramInfoDao = fileOperationMapper.getParamInfoDao(paramsUuid);
        String paramsRange = paramInfoDao.getParamsRange();
        List list = JSON.parseObject(paramsRange, List.class);
        paramInfoDao.setParamsRangeList(list);
        paramInfoDao.setParamsRange(null);
        // 矩阵参数头
        String paramsColumnsStr = (String) paramInfoDao.getParamsColumns();
        List paramsColumns = JSON.parseObject(paramsColumnsStr, List.class);
        paramInfoDao.setParamsColumns(paramsColumns);

        // 来自于（可多选母版名称）
        Object comeFromListObj = paramInfoDao.getComeFromList();
        if (comeFromListObj != null) {
            List<String> comeFromList = JSON.parseObject(String.valueOf(comeFromListObj), List.class);
            paramInfoDao.setComeFromList(comeFromList);
        }

        String paramsGroupId = paramInfoDao.getParamsGroupId();
        List list1 = JSON.parseObject(paramsGroupId, List.class);
        paramInfoDao.setParamsGroupIdList(list1);
        paramInfoDao.setParamsGroupId(null);
        return TResponseVo.success(paramInfoDao);
    }

    /**
     * 读取参数库列表
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getParamService(PagePojo pagePojo, String userId, String platform) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        // 获取用户信息
        DocUserPojo userInfoDao = fileOperationMapper.getUserInfoDao(userId);
        String companyId = userInfoDao.getCompanyId();

        // 判断查哪个库(支持多选)
        List<String> paramsClassify = pagePojo.getParamsClassify();

        // 母版清单
        List<LabelValuePojo> motherModelList = workingTableMapper.getMasterModelLVDao();

        String flag1 = null;
        String flag2 = null;
        String flag3 = null;
        if (paramsClassify == null) {
        } else {
            if (paramsClassify.contains("1")) {
                flag1 = "1";
            }
            if (paramsClassify.contains("2")) {
                flag2 = "1";
            }
            if (paramsClassify.contains("3")) {
                flag3 = "1";
            }
        }

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        List<LabelValuePojo> paramGroupDiDao = fileOperationMapper.getParamGroupDiDao();
        List<LabelValuePojo> paramTypeStyleDiDao = fileOperationMapper.getParamTypeStyleDiDao(null);
        List<LabelValuePojo> fileTypeMBDiDao = fileOperationMapper.getFileTypeMBDiDao();  // 翻译适用场景中文


        List<DocParamsPojo> docParamsPojos = new ArrayList<DocParamsPojo>();
        if (paramsClassify != null && paramsClassify.contains("-1")) {
            // 全部参数 paramsClassify=-1
            String fileUuid = pagePojo.getFileUuid();
            String fileVersionId = pagePojo.getFileVersionId();
            // 本文参数 (本文所在组的所有文件用过的参数)
            // 先获取到组内所有文件
            DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
            List<DocFileIndexPojo> subsidiaryFileList = new ArrayList<>();
            if (docAllInfoDao.getMainFileUuid() != null) {
                subsidiaryFileList = fileOperationMapper.getSubsidiaryFileListDao2(docAllInfoDao.getMainFileUuid(), docAllInfoDao.getMainFileVersionId());
            } else if (docAllInfoDao.getMainFileUuid2() != null) {
                subsidiaryFileList = fileOperationMapper.getSubsidiaryFileListDao2(docAllInfoDao.getMainFileUuid2(), docAllInfoDao.getMainFileVersionId2());
            } else {
                subsidiaryFileList = fileOperationMapper.getSubsidiaryFileListDao2(fileUuid, fileVersionId);
            }
            // 然后根据所有文件查库
            List<String> fileVersionIdList = new ArrayList<>();
            for (DocFileIndexPojo docFileIndexPojo : subsidiaryFileList) {
                fileVersionIdList.add(docFileIndexPojo.getFileVersionId());
            }
            // fileVersionIdList加上当前草稿版本的versionId
            fileVersionIdList.add(fileVersionId);
            String fileVersionIdListStr = JsonKeyUtils.listToString(fileVersionIdList, "','");
            List<DocParamsPojo> docParamsPojos1 = fileOperationMapper.getContentUsedParamDao(fileVersionIdListStr);
            List<DocParamsPojo> docParamsPojos1tmp = new ArrayList<>();
            docParamsPojos1tmp.addAll(docParamsPojos1);
            List<String> oldParamUuidList = new ArrayList<>();
            for (DocParamsPojo docParamsPojo : docParamsPojos1) {
                // 相同的paramUuid只保留一个 根据更新时间保留最近更新的
                // 尤其要注意的一点是  这里getContentUsedParamDao我是按照params_uuid,update_time desc进行排序 所以有了下面的写法
                if (!oldParamUuidList.contains(docParamsPojo.getParamsUuid())) {
                    oldParamUuidList.add(docParamsPojo.getParamsUuid());
                    docParamsPojo.setParamsClassify("9");
                } else {
                    docParamsPojos1tmp.remove(docParamsPojo);
                }
            }
            // 参数库参数
            List<DocParamsPojo> docParamsPojos2 = fileOperationMapper.selectParamsDao(paramNameLike, null, flag1, flag2, flag3, userId, companyId, "desc", null, null);


            // 同时展示时 以本文使用的参数为准
            List<String> paramsUuidList = new ArrayList<>();
            for (DocParamsPojo docParamsPojo : docParamsPojos1) {
                String paramsUuid = docParamsPojo.getParamsUuid();
                paramsUuidList.add(paramsUuid);
            }
            List<DocParamsPojo> docParamsPojos2tmp = new ArrayList<>();
            docParamsPojos2tmp.addAll(docParamsPojos2);
            for (DocParamsPojo docParamsPojo : docParamsPojos2) {
                if (paramsUuidList.contains(docParamsPojo.getParamsUuid())) {
                    docParamsPojos2tmp.remove(docParamsPojo);
                }
            }
            docParamsPojos.addAll(docParamsPojos1tmp);
            docParamsPojos.addAll(docParamsPojos2tmp);
        } else if (paramsClassify != null && paramsClassify.contains("9")) {
            // 本文已使用参数 paramsClassify=9
            String fileUuid = pagePojo.getFileUuid();
            String fileVersionId = pagePojo.getFileVersionId();
            // 本文参数 (本文所在组的所有文件用过的参数)
            // 先获取到组内所有文件
            DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
            List<DocFileIndexPojo> subsidiaryFileList = new ArrayList<>();
            if (docAllInfoDao.getMainFileUuid() != null) {
                subsidiaryFileList = fileOperationMapper.getSubsidiaryFileListDao2(docAllInfoDao.getMainFileUuid(), docAllInfoDao.getMainFileVersionId());
            } else if (docAllInfoDao.getMainFileUuid2() != null) {
                subsidiaryFileList = fileOperationMapper.getSubsidiaryFileListDao2(docAllInfoDao.getMainFileUuid2(), docAllInfoDao.getMainFileVersionId2());
            } else {
                subsidiaryFileList = fileOperationMapper.getSubsidiaryFileListDao2(fileUuid, fileVersionId);
            }
            // 然后根据所有文件查库
             List<String> fileVersionIdList = new ArrayList<>();
            for (DocFileIndexPojo docFileIndexPojo : subsidiaryFileList) {
                fileVersionIdList.add(docFileIndexPojo.getFileVersionId());
            }
            // fileVersionIdList加上当前草稿版本的versionId
            fileVersionIdList.add(fileVersionId);
            String fileVersionIdListStr = JsonKeyUtils.listToString(fileVersionIdList, "','");

            List<DocParamsPojo> docParamsPojos1 = fileOperationMapper.getContentUsedParamDao(fileVersionIdListStr);
            List<DocParamsPojo> docParamsPojos1tmp = new ArrayList<>();
            docParamsPojos1tmp.addAll(docParamsPojos1);
            List<String> oldParamUuidList = new ArrayList<>();
            for (DocParamsPojo docParamsPojo : docParamsPojos1) {
                // 相同的paramUuid只保留一个 根据更新时间保留最近更新的
                // 尤其要注意的一点是  这里getContentUsedParamDao我是按照params_uuid,update_time desc进行排序 所以有了下面的写法
                if (!oldParamUuidList.contains(docParamsPojo.getParamsUuid())) {
                    oldParamUuidList.add(docParamsPojo.getParamsUuid());
                    docParamsPojo.setParamsClassify("9");
                } else {
                    docParamsPojos1tmp.remove(docParamsPojo);
                }
            }
            docParamsPojos = docParamsPojos1tmp;
            isPaged = "0";
        } else {
            String fileUuid = pagePojo.getFileUuid();
            String fileVersionId = pagePojo.getFileVersionId();
            docParamsPojos = fileOperationMapper.selectParamsDao(paramNameLike, null, flag1, flag2, flag3, userId, companyId, "null", fileUuid, fileVersionId);
        }

        // 如果unit是单位编码 则补充成单位名称
        List<LabelValuePojo> unitDiLVDao = fileOperationMapper.getUnitDiLVDao();
        for (DocParamsPojo docParamsPojo : docParamsPojos) {
            // 金额类型参数根据styleId指定unit
            if (docParamsPojo.getParamsTypeId().equals("80")) {
                String styleId = docParamsPojo.getStyleId();
                if ("632007".equals(styleId) || "632008".equals(styleId) || "632010".equals(styleId) || "632012".equals(styleId) || "632013".equals(styleId) || "632014".equals(styleId)) {
                    docParamsPojo.setUnit("770001");  // 元
                } else if ("632009".equals(styleId) || "632011".equals(styleId) || "6320071".equals(styleId)) {
                    docParamsPojo.setUnit("770002");  // 万元
                }
            }
            // 如果是类似编码的纯数字
            if (StringUtils.isNumeric(docParamsPojo.getUnit()) && docParamsPojo.getUnit().length() == 6) {
                for (LabelValuePojo labelValuePojo : unitDiLVDao) {
                    if (labelValuePojo.getValue().equals(docParamsPojo.getUnit())) {
                        docParamsPojo.setUnitName(labelValuePojo.getLabel());
                    }
                }
            }
        }

        for (DocParamsPojo docParamsPojo : docParamsPojos) {
            ArrayList<String> paramGroupNameList = new ArrayList<>();
            String paramsGroupId = docParamsPojo.getParamsGroupId();
            List<Object> list = JSON.parseObject(paramsGroupId, List.class);
            // 添加分组名称
            if (list != null) {
                for (Object s : list) {
                    for (LabelValuePojo labelValuePojo : paramGroupDiDao) {
                        if (labelValuePojo.getValue().equals(s)) {
                            paramGroupNameList.add(labelValuePojo.getLabel());
                        }
                    }
                }
            }
            if (docParamsPojo.getStyleId() != null) {
                for (LabelValuePojo labelValuePojo : paramTypeStyleDiDao) {
                    if (labelValuePojo.getValue().equals(docParamsPojo.getStyleId())) {
                        docParamsPojo.setStyleContent(labelValuePojo.getLabel());
                    }
                }
            }

            // 来自于（可多选母版名称）
            Object comeFromListObj = docParamsPojo.getComeFromList();
            if (comeFromListObj != null) {
                List<String> comeFromList = JSON.parseObject(String.valueOf(comeFromListObj), List.class);
                docParamsPojo.setComeFromList(comeFromList);
                ArrayList<String> comeFromListName = new ArrayList<>();
                if (comeFromList != null && comeFromList.size() != 0) {
                    for (String s : comeFromList) {
                        for (LabelValuePojo labelValuePojo : motherModelList) {
                            if (s.equals(labelValuePojo.getValue())) {
                                comeFromListName.add(labelValuePojo.getLabel());
                                break;
                            }
                        }
                    }
                }
                docParamsPojo.setComeFromListName(comeFromListName);
            }

            // 判断默认值是否需要转换格式
            String paramsTypeId = docParamsPojo.getParamsTypeId();
            if (paramsTypeId.equals("20") || paramsTypeId.equals("60") || paramsTypeId.equals("70") || paramsTypeId.equals("95")) {
                Object defaultValue = docParamsPojo.getDefaultValue();
                if (defaultValue != null) {
                    List list1 = JSON.parseObject(String.valueOf(defaultValue), List.class);
                    docParamsPojo.setDefaultValue(list1);
                }
            } else if (paramsTypeId.equals("90")) {  // 数值
                Object defaultValue = docParamsPojo.getDefaultValue();
                if (defaultValue != null) {
                    Map map1 = new HashMap();
                    try {  // 数值 金额  默认值不是map直接认为默认值无效 不返回给前端了
                        map1 = JSON.parseObject(String.valueOf(defaultValue), Map.class, Feature.NonStringKeyAsString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (map1 != null && map1.size() != 0) {
                        docParamsPojo.setDefaultValue(map1);
                    } else {
                        docParamsPojo.setDefaultValue(null);
                    }
                }
            } else if (paramsTypeId.equals("80")) {  // 金额
                Object defaultValue = docParamsPojo.getDefaultValue();
                if (defaultValue != null) {
                    Map<Object, Object> map1 = new HashMap();
                    try {  // 数值 金额  默认值不是map直接认为默认值无效 不返回给前端了
                        map1 = JSON.parseObject(String.valueOf(defaultValue), Map.class, Feature.NonStringKeyAsString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (map1 != null && map1.size() != 0) {
                        docParamsPojo.setDefaultValue(map1);
                    } else {
                        docParamsPojo.setDefaultValue(null);
                    }
                }
            }

            docParamsPojo.setParamsGroupNameList(paramGroupNameList);
            docParamsPojo.setParamsGroupIdList(list);
            docParamsPojo.setParamsGroupId(null);
            String paramsRange = docParamsPojo.getParamsRange();
            List list1 = JSON.parseObject(paramsRange, List.class);
            docParamsPojo.setParamsRangeList(list1);
            docParamsPojo.setParamsRange(null);
            // 矩阵参数头
            String paramsColumnsStr = (String) docParamsPojo.getParamsColumns();
            List paramsColumns = JSON.parseObject(paramsColumnsStr, List.class);
            docParamsPojo.setParamsColumns(paramsColumns);

            if (docParamsPojo.getParamsClassify().equals("1")) {
                docParamsPojo.setIsCanEdit(true);
            } else {
                docParamsPojo.setIsCanEdit(false);
            }

            // 添加参数数据源名称
            Object dataSource = docParamsPojo.getDataSource();
            if (dataSource != null && !StringUtils.isBlank((String) dataSource)) {
                List<String> dataSourceList = JSON.parseObject(StringEscapeUtils.unescapeJava((String) dataSource), List.class);
                if (dataSourceList.size() >= 1) {
                    if (dataSourceList.get(0).equals("10")) {
                        docParamsPojo.setDataSourceName("企业信息");
                    }
                    if (dataSourceList.get(0).equals("20")) {
                        docParamsPojo.setDataSourceName("历史文本");
                    }
                    if (dataSourceList.get(0).equals("30")) {
                        docParamsPojo.setDataSourceName("项目信息");
                    }
                    if (dataSourceList.get(0).equals("40")) {
                        docParamsPojo.setDataSourceName("辅助工具");
                    }
                }
            }

            // 添加参数使用场景(文件类型)名称
            for (LabelValuePojo labelValuePojo : fileTypeMBDiDao) {
                if (labelValuePojo.getValue().equals(docParamsPojo.getParamsUseSaturation())) {
                    docParamsPojo.setParamsUseSaturationName(labelValuePojo.getLabel());
                }
            }
        }

        // 筛选条件
        // 增加筛选参数分组，参数类型，使用场景，填参角色，数据来源
        Object paramsGroupId1 = pagePojo.getParamsGroupId();
        Object paramsTypeId = pagePojo.getParamsTypeId();
        Object paramsUseSaturation = pagePojo.getParamsUseSaturation();
        Object paramsUseSceneId = pagePojo.getParamsUseSceneId();
        Object dataSource1 = pagePojo.getDataSource();
        // paramsUseSaturation不为空时 筛选
        ArrayList<DocParamsPojo> docParamsPojosTmp = new ArrayList<>();
        docParamsPojosTmp.addAll(docParamsPojos);
//        System.out.println("docParamsPojosTmp : " + JSON.toJSONString(docParamsPojosTmp));
        for (DocParamsPojo docParamsPojo : docParamsPojosTmp) {
            // 按照参数分组过滤
            if (paramsGroupId1 != null) {
                if (paramsGroupId1 instanceof String) { // 当入参paramsGroupId1为字符串时
                    if (docParamsPojo.getParamsGroupIdList() == null || docParamsPojo.getParamsGroupIdList().size() == 0 || !docParamsPojo.getParamsGroupIdList().contains(paramsGroupId1)) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                } else if (paramsGroupId1 instanceof List) { // 当入参paramsGroupId1为数组时
                    if (docParamsPojo.getParamsGroupIdList() == null || docParamsPojo.getParamsGroupIdList().size() == 0 || !new JsonKeyUtils().containsAny(docParamsPojo.getParamsGroupIdList(), (List<Object>) paramsGroupId1)) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                }
            }
            // 按照参数类型过滤
            if (paramsTypeId != null) {
                if (paramsTypeId instanceof String) {
                    if (docParamsPojo.getParamsTypeId() == null || !docParamsPojo.getParamsTypeId().equals(paramsTypeId)) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                } else if (paramsTypeId instanceof List) { // 当入参paramsGroupId1为数组时
                    if (docParamsPojo.getParamsTypeId() == null || !((List<Object>) paramsTypeId).contains(docParamsPojo.getParamsTypeId())) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                }
            }
            // 按照使用场景过滤
            if (paramsUseSaturation != null) {
                if (paramsUseSaturation instanceof String) {
                    if (docParamsPojo.getParamsUseSaturation() == null || !docParamsPojo.getParamsUseSaturation().equals(paramsUseSaturation)) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }

                } else if (paramsUseSaturation instanceof List) {
                    if (docParamsPojo.getParamsUseSaturation() == null || !((List<Object>) paramsUseSaturation).contains(docParamsPojo.getParamsUseSaturation())) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                }
            }
            // 按照填参角色过滤
            if (paramsUseSceneId != null) {
                if (paramsUseSceneId instanceof String) {
                    if (docParamsPojo.getParamsUseSceneId() == null || !docParamsPojo.getParamsUseSceneId().equals(paramsUseSceneId)) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                } else if (paramsUseSceneId instanceof List) {
                    if (docParamsPojo.getParamsUseSceneId() == null || !((List<Object>) paramsUseSceneId).contains(docParamsPojo.getParamsUseSceneId())) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                }
            }
            // 按照数据来源过滤
            if (dataSource1 != null) {
                if (dataSource1 instanceof String) {
                    if (docParamsPojo.getDataSource() == null || !JSON.parseObject(String.valueOf(docParamsPojo.getDataSource()), List.class).get(0).equals(dataSource1)) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                } else if (dataSource1 instanceof List) {
                    if (docParamsPojo.getDataSource() == null || JSON.parseObject(String.valueOf(docParamsPojo.getDataSource()), List.class).size() == 0 || !((List<Object>) dataSource1).contains(JSON.parseObject(String.valueOf(docParamsPojo.getDataSource()), List.class).get(0))) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                }
            }

            // 按照版本过滤
            if (platform.equals("HanFei-v2")) { // 韩非2.0版本过滤掉企业级参数
                if (docParamsPojo.getParamsClassify().equals("2")) {
                    docParamsPojos.remove(docParamsPojo);
                    continue;
                }
            }
        }


        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(docParamsPojos);
        }


        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(docParamsPojos, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", docParamsPojos.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 根据参数类型返回显示格式
     *
     * @param paramsTypeId
     */
    @Override
    public TResponseVo getParamTypeStyleListService(String paramsTypeId) throws Exception {
        List<LabelValuePojo> paramTypeStyleDiDao = fileOperationMapper.getParamTypeStyleDiDao(paramsTypeId);
        // 格式化 使参数类型对应展示效果的list
//        List<String> paramsTypeIdList = new ArrayList<>();
//        List<Map<String,String>> paramsTypeIdList = new ArrayList<>();
//        for (DocParamsTypeStylePojo docParamsTypeStylePojo : paramTypeStyleDiDao) {
//            String paramsTypeId = docParamsTypeStylePojo.getParamsTypeId();
//            if (!paramsTypeIdList.contains(paramsTypeId)) {
//                paramsTypeIdList.add(paramsTypeId);
//                HashMap<String, String> map = new HashMap<>();
//                map.put("label",docParamsTypeStylePojo.getStyleContent());
//                map.put("value",docParamsTypeStylePojo.getStyleId());
//                List<Map<String,String>> mapList = new ArrayList<>();
//                mapList.add(map);
//                paramsTypeIdAndStyleMap.put(paramsTypeId,mapList);
//            } else {
//                List<Map<String, String>> mapList = paramsTypeIdAndStyleMap.get(paramsTypeId);
//            }
//
//        }
        return TResponseVo.success(paramTypeStyleDiDao);
    }

    /**
     * 读取文内已使用参数列表
     *
     * @param docParamsPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getContentUsedParamService(DocParamsPojo docParamsPojo) throws Exception {

//        fileOperationMapper.getContentUsedParamDao(docParamsPojo);
        return null;
    }


//    /**
//     * 文章内参数关联至参数库
//     * @param fileUuid
//     * @param fileVersionId
//     * @param paramsIdInfile
//     * @param paramsUuid
//     * @return
//     * @throws Exception
//     */
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public TResponseVo updateParamListParamsRelatService(String fileUuid, String fileVersionId, String paramsIdInfile, String paramsUuid) throws Exception {
//        fileOperationMapper.updateParamListParamsRelatDao(paramsUuid,fileUuid,fileVersionId,paramsIdInfile);
//        DocStructurePojo docStructurePojo = fileOperationMapper.getDocStructureDao(fileUuid, fileVersionId);
//        String text = docStructurePojo.getFileText();
//        String newText = DraftParamsUtils.updateParams(text, paramsIdInfile, paramsUuid);
//        fileOperationMapper.setDocStructureDao(newText,fileUuid, fileVersionId);
//        return TResponseVo.success("关联参数库成功");
//    }


//    /**
//     * 读取参数清单
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo getParamListService(String fileUuid, String fileVersionId) throws Exception {
//        return TResponseVo.success(fileOperationMapper.selectParamListDao(fileUuid, fileVersionId));
//    }
//
//    /**
//     * 提交参数成功
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @param lists
//     * @return
//     * @throws Exception
//     */
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public TResponseVo commitParamListService(String fileUuid, String fileVersionId, List<CommitParamListItemForm> lists) throws Exception {
//        for (CommitParamListItemForm item : lists) {
//            String paramsUuid = item.getParamsUuid();
//            String paramsText = item.getParamsText();
//            fileOperationMapper.commitParamListDao(fileUuid, fileVersionId, paramsUuid, paramsText);
//        }
//        return TResponseVo.success("提交参数成功");
//    }


//    /**
//     * 保存草稿(新建草稿)
//     *
//     * @param docFileVerIndexPojo
//     * @return
//     * @throws Exception
//     */
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public TResponseVo saveDraftService(DocFileVerIndexPojo docFileVerIndexPojo, String userId) throws Exception {
////        获得当前文章的id和版本id
//        String fileVersionId = docFileVerIndexPojo.getFileVersionId();
//        String fileUuid = docFileVerIndexPojo.getFileUuid();
//        String fileVersionDesc = docFileVerIndexPojo.getFileVersionDesc();
//        String createUserId = userId;
//        String updateUserId = userId;
////        获取当前文章版本的最高版本信息
//        DocFileVerIndexPojo docInfoDao = fileOperationMapper.getDocInfoDao(fileUuid);
//        String isRootVersion = docInfoDao.getIsRootVersion();
////        最高版本+1
//        String fileVersionName = DocFrameUtils.versionCount(docInfoDao.getFileVersionName(), 1);
////        判断当前是否正式版本 如果是就给一个新的版本id
//        String is_draft = fileOperationMapper.isDraftVersionDao(fileUuid, fileVersionId);
//        if (is_draft.equals("1")) {
//            fileOperationMapper.deleteDraftIndexDao(fileUuid, fileVersionId);
//        } else {
//            fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
//        }
//        fileOperationMapper.insertDraftIndexDao(fileUuid, fileVersionId, fileVersionName, new Date(), fileVersionDesc, docInfoDao.getFileVersionId(), isRootVersion, createUserId, updateUserId);
//
//        docFileVerIndexPojo.setFileVersionId(fileVersionId);
//        return TResponseVo.success(docFileVerIndexPojo);
//    }
//
//
//    /**
//     * 删除草稿
//     *
//     * @param fileUuid
//     * @param fileVersionId
//     * @return
//     * @throws Exception
//     */
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public TResponseVo delDraftService(String fileUuid, String fileVersionId) throws Exception {
//
//        return TResponseVo.success("删除草稿成功");
//    }

    /**
     * 文章内 编辑提交
     *
     * @param docFileIndexPojo
     * @param session
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo addSubmitService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {

        return TResponseVo.success("提交审核成功");
    }


    /**
     * 根据标签搜索匹配的文档
     *
     * @param labelList
     */
    @Override
    public TResponseVo getSimilarDocService(List<String> labelList, String userId, String searchLike, String fileTypeId) throws Exception {
        String fileTypeIdListStr = "";
        if (fileTypeId == null) {
        } else if (fileTypeId.equals("WJ-HT")) {
            fileTypeIdListStr = "MB-HT";
        } else if (fileTypeId.equals("WJ-JSGFS")) {
            fileTypeIdListStr = "MB-JSGFS";
        } else if (fileTypeId.equals("WJ-SGZZSJ")) {
            fileTypeIdListStr = "MB-SGZZSJ";
        } else if (fileTypeId.equals("WJ-ZBGG")) {
            fileTypeIdListStr = "MB-ZBGG";
        } else if (fileTypeId.equals("WJ-ZBWJ")) {
            fileTypeIdListStr = "MB-ZBWJ";
        } else if (fileTypeId.equals("MB-ZBWJ")) {
            fileTypeIdListStr = "MB-ZBWJ";
        } else {
            String[] split = fileTypeId.split("-");
            fileTypeIdListStr = "MB-" + split[1];
        }
        List<DocFileIndexPojo> docFileIndexPojos = new ArrayList<>();
        // 如果searchLike为空 则为标签推荐模式
        if (StringUtils.isBlank(searchLike)) {
            DocUserPojo userInfoDao = fileOperationMapper.getUserInfoDao(userId);
            String companyId = userInfoDao.getCompanyId();
            List<DocFileIndexPojo> superQuestionnaireClassPojo = fileOperationMapper.getSimilarDocDao(userId, companyId, fileTypeIdListStr);
            // 如果labelList不为空
//            System.out.println("superQuestionnaireClassPojo : ");
//            System.out.println(JSON.toJSONString(superQuestionnaireClassPojo));
            if (labelList != null && labelList.size() != 0) {
                // 计算每个文档的相似度
                for (DocFileIndexPojo docFileIndexPojo : superQuestionnaireClassPojo) {
                    String fileLabelListStr = docFileIndexPojo.getFileLabelList();
                    List<String> fileLabelList = JsonKeyUtils.stringToList(fileLabelListStr);
                    String similar = JsonKeyUtils.getSimilar(labelList, fileLabelList);
                    // 设置相似度
                    docFileIndexPojo.setSimilar(similar);
                }
            }
            // 根据相似度取前十名
            docFileIndexPojos = JsonKeyUtils.orderMapList2(superQuestionnaireClassPojo);
            if (docFileIndexPojos.size() > 10) { //判断list长度
                docFileIndexPojos = docFileIndexPojos.subList(0, 10);//取前10条数据
            }
        } else {
            // 如果searchLike不为空 搜索模式
            docFileIndexPojos = fileOperationMapper.getDocBySearchDao(searchLike, fileTypeIdListStr);
        }

        // 获取标签id对应内容
        List<LabelValuePojo> labelLVListDao = fileOperationMapper.getAllLabelLVListDao(null);
        List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
        // 补充返回字段
        for (DocFileIndexPojo docFileIndexPojo : docFileIndexPojos) {
            String fileLabelListStr = docFileIndexPojo.getFileLabelList();
            List<String> fileLabelList = JsonKeyUtils.stringToList(fileLabelListStr);
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            for (String labelId : fileLabelList) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            docFileIndexPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));

            // 翻译文件类型
            String fileTypeIdResult = docFileIndexPojo.getFileTypeId();
            String fileTypeName = "";
            if (fileTypeIdResult == null) {
                docFileIndexPojo.setFileTypeName("未知");
            } else {
                for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                    if (hfFileTypeDiPojo.getFileTypeId().equals(fileTypeIdResult)) {
                        docFileIndexPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                    }
                }
            }
        }

        // 是否推荐
        int i = 1;
        for (DocFileIndexPojo docFileIndexPojo : docFileIndexPojos) {
            if (i > 3) {
                break;
            }
            if (docFileIndexPojo.getSimilar() != null) {
                double v = Double.parseDouble(docFileIndexPojo.getSimilar()); //转换为double类型
                if (v >= 0.7) {
                    docFileIndexPojo.setRecommend(true);
                    i++;
                }
            }
        }
//        // 是否推荐
//        int i = 1;
//        for (DocFileIndexPojo docFileIndexPojo : docFileIndexPojos) {
//            if (i <= 3) {
//                docFileIndexPojo.setRecommend(true);
//                i++;
//            }
//        }
        return TResponseVo.success(docFileIndexPojos);
    }

    /**
     * 文章内新建派生
     *
     * @param docFileIndexPojo
     * @param
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo addDeriveService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        // 获取父文件的fileUuid fileVersionId
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            return TResponseVo.error(ResponseEnum.ERROR, "附属文件无派生功能");
        }
//        文件名称
        String fileName = docFileIndexPojo.getFileName();
        if (StringUtils.isBlank(fileName)) {
            return TResponseVo.error(ResponseEnum.ERROR, "文件名称不能为空");
        }
//        文件版本（派生默认V1.0）
        docFileIndexPojo.setFileVersionName("V1.0");
//        参与人
        List<String> includeUserIdList = docFileIndexPojo.getIncludeUserIdList();
        String includeUserList = new JsonKeyUtils().listToString(includeUserIdList);
        docFileIndexPojo.setIncludeUserList(includeUserList);
//        文件类型
        docFileIndexPojo.setFileTypeId(docAllInfoDao.getFileTypeId());

//        文件说明
        String fileDesc = docFileIndexPojo.getFileDesc();
//        文件标签
        List<String> fileLabelIds = docFileIndexPojo.getFileLabelIds();
        docFileIndexPojo.setFileLabelList(new JsonKeyUtils().listToString(fileLabelIds, ","));

        // 设置父文件uuid
        docFileIndexPojo.setFileParentId(fileUuid);
        docFileIndexPojo.setOldFileVersionId(fileVersionId);
//        if (!docAllInfoDao.getFileVersionId().equals(fileVersionId)) {
//            // 说明:以当前最新的"正式版"为准进行派生
//            docFileIndexPojo.setOldFileVersionId(docAllInfoDao.getFileVersionId());
//        }
        fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
        docFileIndexPojo.setFileUuid(fileUuid);
        docFileIndexPojo.setFileVersionId(fileVersionId);

        String userId = docFileIndexPojo.getCreateUserId();

        docFileIndexPojo.setCreateTime(new Date());

        try {
            int j = 0;
            int i = 0;
            // 创建索引
            i = fileOperationMapper.addDeriveFileIndexDao(docFileIndexPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(700);
                throw fileIndexException;
            }

            // 创建版本
            i = fileOperationMapper.addDeriveFileVersionDao(docFileIndexPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(701);
                throw fileIndexException;
            }

            // 创建大纲
            List<OutLinePojo> getDeriveFileOutlineDao = fileOperationMapper.getDeriveFileOutlineDao(docFileIndexPojo);
            // 设置groupId替换用的键值
            HashMap<String, String> groupIdMap = new HashMap<>();
            for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                if (outLinePojo.getOutlineReplaceGroupId() == null) {
                    continue;
                }
                String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
            }
            String sql = "";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = formatter.format(new Date());
            for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                j++;
                // 重新排序
                outLinePojo.setOutlineOrder(String.valueOf(j));
                String outlineReplaceGroupId = null;
                String color = null;
                if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                    outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                }
                if (outLinePojo.getColor() != null) {
                    color = "\"" + outLinePojo.getColor() + "\"";
                }
                sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + outLinePojo.getIsNecessary() + "\",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
//                System.out.println("sql : " + sql);
                fileOperationMapper.addDeriveFileOutline2Dao(sql);
            }
            // 创建内容
            List<ContentPojo> deriveFileContentDao = fileOperationMapper.getDeriveFileContentDao(docFileIndexPojo);
            sql = "";
            for (ContentPojo contentPojo : deriveFileContentDao) {
                String contentText = contentPojo.getContentText();
//                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//                Elements elements = doc.select("mark");
//                Elements elementsTmp = new Elements();
//                elementsTmp.addAll(elements);
//                for (Element element : elementsTmp) {
//                    element.before(element.html());
//                    element.remove();  // 去除mark标签
//                }
//                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
//                contentPojo.setContentText(contentText);
                sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
                fileOperationMapper.addDeriveFileContentDao(sql);
            }
            // 创建参数
            fileOperationMapper.addDeriveContentParamDao(docFileIndexPojo);

            // 创建标注
            fileOperationMapper.addDeriveContentTagDao(docFileIndexPojo);

            // 创建书签
            fileOperationMapper.addDeriveContentBookmarkDao(docFileIndexPojo);

            // 创建数据管理(4张)
            fileOperationMapper.addDeriveContentDmDbDao(docFileIndexPojo);
            fileOperationMapper.addDeriveContentDmTableDao(docFileIndexPojo);
            fileOperationMapper.addDeriveContentDmColumnsDao(docFileIndexPojo);
            String hashMod1 = new HashUtils().getHashMod(docFileIndexPojo.getFileUuid() + docFileIndexPojo.getFileVersionId());
            String hashMod2 = new HashUtils().getHashMod(docFileIndexPojo.getFileParentId() + docFileIndexPojo.getOldFileVersionId());
            System.out.println("表编号为: " + hashMod1);
            splitTableMapper.addDeriveContentDmDataDao(docFileIndexPojo, hashMod1, hashMod2, dmContentDataDatabase);

            // 创建数据表(如果有)
            fileOperationMapper.addDeriveContentDbDao(docFileIndexPojo);

            // 评审工具(如果有)
            HfContentAssessPojo contentAssessInfoDao = fileOperationMapper.getContentAssessInfoDao(docFileIndexPojo.getFileParentId());
            if (contentAssessInfoDao != null) {
                // 将评审工具也复制放到新文件里
                fileOperationMapper.addDeriveFileAssessDao(docFileIndexPojo.getFileParentId(), docFileIndexPojo.getFileUuid(), docFileIndexPojo.getCreateUserId());
                fileOperationMapper.addDeriveFileAssessElementDao(docFileIndexPojo.getFileParentId(), docFileIndexPojo.getFileUuid(), docFileIndexPojo.getCreateUserId());
            }

            // 创建附属文件
            // 查找附属
            List<DocFileIndexPojo> allSubsidiaryFileListDao = fileOperationMapper.getAllSubsidiaryFileListDao(docFileIndexPojo);
            for (DocFileIndexPojo fileIndexPojo : allSubsidiaryFileListDao) {
                // 获取父文件的fileUuid fileVersionId
                fileUuid = fileIndexPojo.getFileUuid();
                fileVersionId = fileIndexPojo.getFileVersionId();
                fileIndexPojo.setFileParentId(fileUuid);
                fileIndexPojo.setOldFileVersionId(fileVersionId);
                // 把主文件的fileUuid写到附属文件的mainFileUuid字段中
                fileIndexPojo.setMainFileUuid(docFileIndexPojo.getFileUuid());
                fileIndexPojo.setMainFileVersionId(docFileIndexPojo.getFileVersionId());
                userId = docFileIndexPojo.getCreateUserId();
                fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                fileIndexPojo.setFileUuid(fileUuid);
                fileIndexPojo.setFileVersionId(fileVersionId);

                fileIndexPojo.setCreateTime(new Date());

                j = 0;
                i = 0;
                // 创建索引
                i = fileOperationMapper.addDeriveFileIndexDao1(fileIndexPojo);
                if (i == 0) {
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(700);
                    throw fileIndexException;
                }

                // 创建版本
                i = fileOperationMapper.addDeriveFileVersionDao(fileIndexPojo);
                if (i == 0) {
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(701);
                    throw fileIndexException;
                }

                // 创建大纲
                getDeriveFileOutlineDao = fileOperationMapper.getDeriveFileOutlineDao(fileIndexPojo);
                // 设置groupId替换用的键值
                groupIdMap = new HashMap<>();
                for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                    if (outLinePojo.getOutlineReplaceGroupId() == null) {
                        continue;
                    }
                    String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                    groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
                }
                sql = "";
                formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateString = formatter.format(new Date());
                for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                    j++;
                    // 重新排序
                    outLinePojo.setOutlineOrder(String.valueOf(j));
                    String outlineReplaceGroupId = null;
                    String color = null;
                    if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                        outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                    }
                    if (outLinePojo.getColor() != null) {
                        color = "\"" + outLinePojo.getColor() + "\"";
                    }
                    sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + outLinePojo.getIsNecessary() + "\",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 1);
//                    System.out.println("sql : " + sql);
                    fileOperationMapper.addDeriveFileOutline2Dao(sql);
                }
                // 创建内容
                deriveFileContentDao = fileOperationMapper.getDeriveFileContentDao(fileIndexPojo);
                sql = "";
                for (ContentPojo contentPojo : deriveFileContentDao) {
                    String contentText = contentPojo.getContentText();
//                    Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//                    Elements elements = doc.select("mark");
//                    Elements elementsTmp = new Elements();
//                    elementsTmp.addAll(elements);
//                    for (Element element : elementsTmp) {
//                        element.before(element.html());
//                        element.remove();  // 去除mark标签
//                    }
//                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
//                    contentPojo.setContentText(contentText);
                    sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 1);
                    fileOperationMapper.addDeriveFileContentDao(sql);
                }
                // 创建参数
                fileOperationMapper.addDeriveContentParamDao(fileIndexPojo);

                // 创建标注
                fileOperationMapper.addDeriveContentTagDao(fileIndexPojo);

                // 创建书签
                fileOperationMapper.addDeriveContentBookmarkDao(fileIndexPojo);

                // 创建数据管理(4张)
                fileOperationMapper.addDeriveContentDmDbDao(fileIndexPojo);
                fileOperationMapper.addDeriveContentDmTableDao(fileIndexPojo);
                fileOperationMapper.addDeriveContentDmColumnsDao(fileIndexPojo);
                hashMod1 = new HashUtils().getHashMod(fileIndexPojo.getFileUuid() + fileIndexPojo.getFileVersionId());
                hashMod2 = new HashUtils().getHashMod(fileIndexPojo.getFileParentId() + fileIndexPojo.getOldFileVersionId());
                System.out.println("表编号为: " + hashMod1);
                splitTableMapper.addDeriveContentDmDataDao(fileIndexPojo, hashMod1, hashMod2, dmContentDataDatabase);

                // 创建数据表(如果有)
                fileOperationMapper.addDeriveContentDbDao(fileIndexPojo);
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(704);
            throw fileIndexException;
        }

        // 派生后自动调用一次获取trueVersion操作
        return getTrueVersionIdService(docFileIndexPojo.getFileUuid(), session);

//        Map<String, String> ret = new HashMap<>();
//        ret.put("fileUuid", docFileIndexPojo.getFileUuid());
//        ret.put("fileVersionId", docFileIndexPojo.getFileVersionId());
//        ret.put("info", "派生成功");
//        return TResponseVo.success(ret);

    }


    /**
     * 新增版本
     *
     * @param docFileIndexPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo addVersionService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileVersionId = docFileIndexPojo.getFileVersionId();

        // 使用范围
        String fileUseRangeId = docFileIndexPojo.getFileUseRangeId();
        String fileUseRangeName = "";
        if (fileUseRangeId == null) {
            fileUseRangeName = "场景未知"; // 未知
        } else if (fileUseRangeId.equals("GR")) {
            fileUseRangeName = "个人适用";
        } else if (fileUseRangeId.equals("GS")) {
            fileUseRangeName = "公司适用";
        } else if (fileUseRangeId.equals("PT")) {
            fileUseRangeName = "平台适用";
        } else {
            fileUseRangeName = "场景未知"; // 未知
        }
        docFileIndexPojo.setFileUseRangeId(fileUseRangeId == null ? null : fileUseRangeId);
        docFileIndexPojo.setFileUseRangeName(fileUseRangeName);
        // 文件说明

        // 文件标签
        List<String> fileLabelIds = docFileIndexPojo.getFileLabelIds();
        docFileIndexPojo.setFileLabelList(new JsonKeyUtils().listToString(fileLabelIds, ","));

        // 获取父文件的fileUuid fileVersionId
        String fileUuid = docFileIndexPojo.getFileUuid();
        DocFileVerIndexPojo draftVersion = fileOperationMapper.getDraftVersionIdDao(fileUuid);

        if (draftVersion == null) {

        } else {
            // 如果已经存在草稿 则把该草稿转正
//            String draftVersionIdDao = draftVersion.getFileVersionId();
            try {
                docFileIndexPojo.setIsDraft("1");
                // 升级版本号
                String fileVersionName = draftVersion.getFileVersionName();
                String newFileVersionName = new JsonKeyUtils().versionCount(fileVersionName, 1);
                docFileIndexPojo.setFileVersionName(newFileVersionName);
                docFileIndexPojo.setFileVersionId(draftVersion.getFileVersionId());
                // 添加版本 id
                int i = fileOperationMapper.updateVersionFileIndexDao2(docFileIndexPojo);
                if (i == 0) {
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(700);
                    throw fileIndexException;
                }
                docFileIndexPojo.setCreateTime(new Date());
                fileOperationMapper.updateVersionFileIndexDao3(docFileIndexPojo);
            } catch (FileIndexException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(704);
                throw fileIndexException;
            }
        }

        // 写入文库中
        HfFileLibraryPojo hfFileLibraryPojo = new HfFileLibraryPojo();
        hfFileLibraryPojo.setFileUuid(fileUuid);
        hfFileLibraryPojo.setFileVersionId(fileVersionId);
        hfFileLibraryPojo.setFileStatus("offline");
        hfFileLibraryPojo.setAuditingStatus("待审核");
        hfFileLibraryPojo.setCreateUserId(docFileIndexPojo.getCreateUserId());
        try {
            int i = fileOperationMapper.newHfFileLibraryDao(hfFileLibraryPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(722);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(723);
            throw fileIndexException;
        }

        // 自动审批通过
        // 调用审批接口
        changeAuditingFileStatusService(hfFileLibraryPojo.getFileUuid(), hfFileLibraryPojo.getFileVersionId(), "通过", "系统审批通过", "110");

        // 自动上线
        changeLibraryFileStatusService(hfFileLibraryPojo.getFileUuid(), hfFileLibraryPojo.getFileVersionId(), "online");

//        // 参数版本记录修改
//        List<DocParamsPojo> docParamListDao = fileOperationMapper.getDocParamListDao(fileUuid);
//        if (docParamListDao.size() > 0) {
//            String draftVersionId = draftVersion.getFileVersionId();
//            DocParamsPojo baseData = docParamListDao.get(0);
//            baseData.setFileVersionId(draftVersionId);
//            fileOperationMapper.insertParamDao(baseData);
//        }

        Map<String, String> ret = new HashMap<>();
        ret.put("fileUuid", fileUuid);
        ret.put("fileVersionId", fileVersionId);
        ret.put("info", "新版本创建成功");
        return TResponseVo.success(ret);

    }

    public TResponseVo changeAuditingFileStatusService(String fileUuid, String fileVersionId, String auditingStatus, String auditingReason, String auditingUserId) throws Exception {
        if (auditingStatus.equals("通过") || auditingStatus.equals("不通过")) {
            // 审批
            workingTableMapper.changeAuditingFileStatusDao(fileUuid, fileVersionId, auditingStatus, auditingReason, auditingUserId, new Date());
            // 查询
//            HfFileLibraryPojo auditingFileRecordDao = workingTableMapper.getAuditingFileRecordDao(fileUuid, fileVersionId);
//            String createUserId = auditingFileRecordDao.getCreateUserId();
//            // 新增通知
//            HfAuditingResultNoticePojo hfAuditingResultNoticePojo = new HfAuditingResultNoticePojo();
//            hfAuditingResultNoticePojo.setAuditingUuid(UUID.randomUUID().toString().replaceAll("-", ""));
//            hfAuditingResultNoticePojo.setAuditingContent("您提交的模板已经审批完成，审批结果为 : " + auditingStatus + "");
//            hfAuditingResultNoticePojo.setNoticeUserId(createUserId);
//            hfAuditingResultNoticePojo.setFileUuid(fileUuid);
//            hfAuditingResultNoticePojo.setFileVersionId(fileVersionId);
//            hfAuditingResultNoticePojo.setNoticeType("审核");
//            workingTableMapper.noticeCreateUserDao(hfAuditingResultNoticePojo); // 通知用户
            return TResponseVo.success("审核完成");
        } else {
            return TResponseVo.error("auditingStatus参数值错误");
        }
    }

    public TResponseVo changeLibraryFileStatusService(String fileUuid, String fileVersionId, String fileStatus) throws Exception {
        if (fileStatus != null && !fileStatus.equals("online") && !fileStatus.equals("offline")) {
            return TResponseVo.error("fileStatus入参有误(online或offline)");
        }
        workingTableMapper.changeLibraryFileStatusDao(fileUuid, null, "offline");  // 先都下线
        if (fileStatus.equals("online") && !org.apache.commons.lang3.StringUtils.isBlank(fileVersionId)) {  // 选择性上线一条 保证上线数据唯一
            workingTableMapper.changeLibraryFileStatusDao(fileUuid, fileVersionId, fileStatus);

            try {
                // 记录日志
                DocFileIndexPojo libraryFileInfoDao = workingTableMapper.getLibraryFileInfoDao(fileUuid, fileVersionId);
                String fileVersionName = libraryFileInfoDao.getFileVersionName();
                String createUserId = libraryFileInfoDao.getCreateUserId();
                String fileName = libraryFileInfoDao.getFileName();
            } catch (Exception ignored) {

            }
        }
        return TResponseVo.success("操作完成");
    }

    /**
     * 获取文本编辑页右侧看板展示信息
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo getBoardDataService(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);

        if (docAllInfoDao.getMainFileUuid() != null) {
//            fileVersionId = fileOperationMapper.getRealFileVersionIdDao(fileUuid,fileVersionId);
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        }

        // 如果fileUuid是文件 则修改其状态为编制中
        if (!docAllInfoDao.getFileClass().equals("1") && !docAllInfoDao.getFileClass().equals("0")) {  // 2或3
            fileOperationMapper.changeFileFinishStatusDao(fileUuid, docAllInfoDao.getFileVersionId(), "0");
        }

        ProjectPojo projectInfo = new ProjectPojo();
        if (docAllInfoDao.getFileStatus() != null && docAllInfoDao.getFileStatus().equals("82")) {
            // 说明是已经被接收成为项目文件
            projectInfo = fileOperationMapper.getProjectInfoByFileDao(fileUuid);
            if (projectInfo == null) {
                projectInfo = fileOperationMapper.getBatchInfoByFileDao(fileUuid);
            }
        }
        String lastVersionId = docAllInfoDao.getFileVersionId();
        DocFileVerIndexPojo versionInfoDao = fileOperationMapper.getVersionInfoDao(fileUuid, fileVersionId == null ? lastVersionId : fileVersionId);
        String userName = versionInfoDao.getUserName();

        // 判断当前用户是否有权限
        String includeUserList = docAllInfoDao.getIncludeUserList();
        List<String> includeUserIdList = new JsonKeyUtils().stringToList(includeUserList);
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();

        if (versionInfoDao == null) {
            Map<String, String> ret = new HashMap<>();
            ret.put("fileVersionName", "");
            ret.put("updateNumber", "0");
            ret.put("lawNumber", "0");
            ret.put("derivationNumber", "0");
            ret.put("useNumber", "0");
            ret.put("fileName", "");
            ret.put("userName", userName);
            if (includeUserIdList.contains(userId) || docAllInfoDao.getCreateUserId().equals(userId)) {
                ret.put("info", "RW");
            } else {
                ret.put("info", "RO");
            }
            return TResponseVo.success(ret);
        }
        // 版本记录
        String fileVersionName = versionInfoDao.getFileVersionName();

        // 父文件更新
        String fileParentId = fileOperationMapper.getFileParentId(fileUuid);
        List<HfUpdateInfoPojo> updateInfoDao = fileOperationMapper.getUpdateInfoDao(fileUuid, fileParentId);
        int updateNumber = updateInfoDao.size();


        // 派生次数
        List<DocFileIndexPojo> derivationListDao = fileOperationMapper.getDerivationListDao(fileUuid);
        int derivationNumberDao = derivationListDao.size();
        // 派生次数(已审核次数)
        List<DocFileIndexPojo> derivationPassListDao = fileOperationMapper.getDerivationPassListDao(fileUuid);
        int derivationPassNumberDao = derivationPassListDao.size();
        // 应用次数
        List<OutLinePojo> useListDao = fileOperationMapper.getUseListDao(fileUuid);
        int useNumberDao = useListDao.size();

        // 文件属性
        DocFileIndexPojo docframeFileIndexInfoDao = fileOperationMapper.getDocframeFileIndexInfoDao(fileUuid);
        String fileName = docframeFileIndexInfoDao.getFileName();

        // 是否收藏
        Integer collectCnt = fileOperationMapper.checkCollectionDao(userId, fileUuid);

        // 审核状态
        String auditingStatus = fileOperationMapper.getAuditingStatusDao(fileUuid, fileVersionId);


        Map<String, Object> ret = new HashMap<>();
        ret.put("fileVersionName", fileVersionName);
        ret.put("updateNumber", String.valueOf(updateNumber));
//        ret.put("lawNumber", String.valueOf(lawNumber));
        ret.put("derivationNumber", String.valueOf(derivationNumberDao));
        ret.put("derivationPassNumber", String.valueOf(derivationPassNumberDao));
        ret.put("derivationPassList", derivationPassListDao);
        ret.put("useNumber", String.valueOf(useNumberDao));
        ret.put("fileName", fileName);
        ret.put("userName", userName);
        ret.put("projectId", projectInfo.getProjectId());
        ret.put("projectName", projectInfo.getProjectName());
        ret.put("auditingStatus", auditingStatus);
        if (includeUserIdList.contains(userId) || docAllInfoDao.getCreateUserId().equals(userId)) {
            ret.put("info", "RW");
        } else {
            ret.put("info", "RO");
        }
        ret.put("collection", collectCnt == 0 ? false : true);
        ret.put("isDraft", "1".equals(versionInfoDao.getIsDraft()) ? true : false); // 是否草稿
        return TResponseVo.success(ret);
    }

    /**
     * 获取文本编辑页右侧看板展示信息
     *
     * @param fileUuid
     * @param fileVersionId
     * @param session
     */
    @Override
    public TResponseVo getBoardSecondDataService(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);

        if (docAllInfoDao.getMainFileUuid() != null) {
//            fileVersionId = fileOperationMapper.getRealFileVersionIdDao(fileUuid,fileVersionId);
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        }
        // 法律法规更新
        // 0.对比
        String lawExtractTextListStr = "";
        if (!redisUtils.hasKey("getLawExtractTextLists" + envName)) { // 先读取缓存
            lawExtractTextListStr = getLawExtractTextListsService(10000);
            // 写入缓存  5分钟更新一次
            redisUtils.set("getLawExtractTextLists" + envName, lawExtractTextListStr, 300);
        } else {
            lawExtractTextListStr = String.valueOf(redisUtils.get("getLawExtractTextLists" + envName));
        }
        if (!redisUtils.hasKey("compareLawExtract" + fileUuid + fileVersionId + envName)) {
            // 加锁防止频繁对比
            redisUtils.set("compareLawExtract" + fileUuid + fileVersionId + envName, 30, 30);
            // 对比
            compareLawExtractTextService(lawExtractTextListStr);
            // 1.获取本文使用的法律法规信息
            List<TagPojo> TagPojoList = fileOperationMapper.getThisFileLawListDao(fileUuid, fileVersionId);
            ArrayList<String> textIdList = new ArrayList<>();
            HashMap<String, Date> textIdDateMap = new HashMap<>();
            for (TagPojo tagPojo : TagPojoList) {
                textIdList.add(tagPojo.getLawId());
                textIdDateMap.put(tagPojo.getLawId(), tagPojo.getCreateTime());
            }
            String textIdListStr = new JsonKeyUtils().listToString(textIdList, "','");
            // 2.获取已经生成的通知 防止重复通知
            List<String> updateIdList = fileOperationMapper.getThisFileLawUpdateNoticeDao(fileUuid, fileVersionId);
            String updateIdListStr = new JsonKeyUtils().listToString(updateIdList, "','");
            // 3.本次的通知
            List<HfLawPojo> lawUpdateNoticeDao = fileOperationMapper.getLawUpdateNoticeDao(textIdListStr, updateIdListStr);
            List<HfLawPojo> lawUpdateNoticeDaoTmp = new ArrayList<>();
            for (HfLawPojo hfLawPojo : lawUpdateNoticeDao) {
                String textId = hfLawPojo.getTextId();
                if (textIdDateMap.get(textId).before(hfLawPojo.getGetTime())) {  // 摘编使用时间早于摘编变更时间
                    hfLawPojo.setNoticeUuid(UUID.randomUUID().toString().replaceAll("-", ""));
                    hfLawPojo.setFileUuid(fileUuid);
                    hfLawPojo.setFileVersionId(fileVersionId);
                    hfLawPojo.setRead(false);
                    lawUpdateNoticeDaoTmp.add(hfLawPojo);
                }
            }
            // 4.生成通知
            if (lawUpdateNoticeDaoTmp.size() != 0) {
                fileOperationMapper.createUpdateNoticeDao(lawUpdateNoticeDaoTmp);
            }
        }

        int lawNumber = 0;
        lawNumber = fileOperationMapper.getNoticeNumberDao(fileUuid, fileVersionId);

        Map<String, Object> ret = new HashMap<>();
        ret.put("lawNumber", String.valueOf(lawNumber));
        return TResponseVo.success(ret);
    }

    /**
     * 新增章节
     * type=0 同级章节
     * type=1 下级章节
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo addOutlineService(OutLinePojo outLinePojo) throws Exception {
        String type = outLinePojo.getType();
        String fileUuid = outLinePojo.getFileUuid();
        String fileVersionId = outLinePojo.getFileVersionId();
        // 删除目录树缓存信息
        redisUtils.del("getOutlineList" + fileUuid + fileVersionId);
        String outlineId = outLinePojo.getOutlineId();
        String outlineText = outLinePojo.getOutlineText();
        String userId = outLinePojo.getUserId();
        String newOutlineId = UUID.randomUUID().toString().replaceAll("-", "");
        if (type.equals("0")) {
            OutLinePojo outlineInfoDao = fileOperationMapper.getOutlineInfoDao(outlineId, fileUuid, fileVersionId);
            String outlineOrder = outlineInfoDao.getOutlineOrder();
            // 所有当前order之后的order通通加1
            fileOperationMapper.updateOutlineOrderDao(outlineOrder, fileUuid, fileVersionId, 1);
            outlineInfoDao.setOutlineId(newOutlineId);
            outlineInfoDao.setOutlineText(outlineText);
            outlineInfoDao.setCreateTime(new Date());
            outlineInfoDao.setCreateUserId(userId);
            outlineInfoDao.setOutlineOrder(String.valueOf(Integer.valueOf(outlineOrder) + 1));
            fileOperationMapper.addOutlineDao(outlineInfoDao);
        }
        if (type.equals("1")) {
            OutLinePojo outlineInfoDao = fileOperationMapper.getOutlineInfoDao(outlineId, fileUuid, fileVersionId);
            // 先判断一下是否还能继续添加子章节
            if (Integer.valueOf(outlineInfoDao.getOutlineLevel()) >= 6) {
                return TResponseVo.success("无法添加新章节");
            }
            String outlineOrder = outlineInfoDao.getOutlineOrder();
            // 所有当前order之后的order通通加1
            fileOperationMapper.updateOutlineOrderDao(outlineOrder, fileUuid, fileVersionId, 1);
            outlineInfoDao.setOutlineId(newOutlineId);
            outlineInfoDao.setOutlineText(outlineText);
            outlineInfoDao.setCreateTime(new Date());
            outlineInfoDao.setCreateUserId(userId);
            outlineInfoDao.setOutlineOrder(String.valueOf(Integer.valueOf(outlineOrder) + 1));
            outlineInfoDao.setOutlineFatherId(outlineId);
            fileOperationMapper.addOutlineDao(outlineInfoDao);
        }
        ContentPojo contentPojo = new ContentPojo();
        contentPojo.setContentId(newOutlineId);
        contentPojo.setContentText("");
        contentPojo.setOutlineId(newOutlineId);
        contentPojo.setFileUuid(fileUuid);
        contentPojo.setFileVersionId(fileVersionId);
        contentPojo.setCreateTime(new Date());
        fileOperationMapper.addContentDao(contentPojo);

        autoCompareService(fileUuid, fileVersionId); // 对比

        HashMap<String, String> ret = new HashMap<>();
        ret.put("outlineId", newOutlineId);
        ret.put("info", "新增完成");
        return TResponseVo.success(ret);
    }

    /**
     * 修改章节
     * <p>
     * type=0 同级章节
     * type=1 下级章节
     */
    @Override
    public TResponseVo updateOutlineService(OutLinePojo outLinePojo) throws Exception {
        String fileUuid = outLinePojo.getFileUuid();
        String fileVersionId = outLinePojo.getFileVersionId();
        String outlineIdMove = outLinePojo.getOutlineId();
        OutLinePojo outlineInfoDao = fileOperationMapper.getOutlineInfoDao(outlineIdMove, fileUuid, fileVersionId);

        // 顺序
        int order = 0;
        List<OutLinePojo> outlineList = new ArrayList<>();
        List<Map> outlineListMap = outLinePojo.getOutlineListMap();
        if (outlineListMap == null) {
        } else {
            List<Map<String, Object>> maps = new JsonKeyUtils().depthOrderTraversalWithRecursive(outlineListMap, null);
            if (outlineInfoDao != null && outlineInfoDao.getOutlineReplaceGroupId() != null) {
                for (Map<String, Object> linePojo : maps) {
                    // 判断被移动的节点是否破坏了分组
                    if (String.valueOf(linePojo.get("key")).equals(outlineInfoDao.getOutlineId())) {
                        // 判断父节点是否发生变化
                        if (!outlineInfoDao.getOutlineFatherId().equals(String.valueOf(linePojo.get("fatherId")))) {
                            // 不一致就报错
                            FileIndexException questionnaireException = new FileIndexException();
                            questionnaireException.setCode(600);
                            throw questionnaireException;
                        }
                    }
                }
            }

            List<OutLinePojo> outlineListDao = fileOperationMapper.getOutlineListDao(fileUuid, fileVersionId);
            for (Map<String, Object> linePojo : maps) {
                String outlineId = String.valueOf(linePojo.get("key"));
                String outlineOrder = String.valueOf(order++);
                String outlineFatherId = String.valueOf(linePojo.get("fatherId"));
                int f = 0;
                for (OutLinePojo pojo : outlineListDao) {
                    if (pojo.getOutlineId().equals(outlineId)) {
                        if (pojo.getOutlineOrder().equals(outlineOrder) && pojo.getOutlineFatherId() != null && outlineFatherId != null && pojo.getOutlineFatherId().equals(outlineFatherId)) {
                            f = 1;
                            break;
                        }
                        break;
                    }
                }
                if (f == 1) {
                    continue;
                }
                fileOperationMapper.updateOutlineInfoDao(fileUuid, fileVersionId, outlineId, outlineOrder, outlineFatherId);
            }
            // 删除目录树缓存信息
            redisUtils.del("getOutlineList" + fileUuid + fileVersionId);
        }
        return TResponseVo.success("更新完成");
    }

    /**
     * 删除章节
     *
     * @param outlineId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delOutlineService(String outlineId, String fileUuid, String fileVersionId) throws Exception {
        // 找到目标段落
        OutLinePojo outlineInfoDao = fileOperationMapper.getOutlineInfoDao(outlineId, fileUuid, fileVersionId);
        if (outlineInfoDao == null) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "段落id不存在,删除失败");
        }
        // 找到目标段落下所有的子段落
        List<OutLinePojo> outlineListDao = fileOperationMapper.getOutlineListDao(fileUuid, fileVersionId);
        List<String> outlineIdList = new ArrayList<>();
        for (OutLinePojo outLinePojo : outlineListDao) {  // 由于outlineListDao的数据是有顺序的 所以可以这么写
            if (outlineIdList.contains(outLinePojo.getOutlineFatherId())) {
                outlineIdList.add(outLinePojo.getOutlineId());
                continue;
            }
            if (outLinePojo.getOutlineId().equals(outlineId)) {
                outlineIdList.add(outlineId);
                continue;
            }
        }

        String outlineIdListStr = new JsonKeyUtils().listToString(outlineIdList, "','");
        int size = outlineIdList.size();
        // 删除目录树缓存信息
        redisUtils.del("getOutlineList" + fileUuid + fileVersionId);
        String outlineOrder = outlineInfoDao.getOutlineOrder();
        fileOperationMapper.deleteOutlineInfoDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删索引
        fileOperationMapper.deleteContentDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删内容
        fileOperationMapper.deleteContentParamsDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删参数
        fileOperationMapper.deleteTagDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删标注
        fileOperationMapper.deleteBookmarkDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删书签
        fileOperationMapper.deleteJudgment1(outlineIdListStr, fileUuid, fileVersionId);  // 删评标要素

        // 所有当前order之后的order通通减1
        Integer integer = fileOperationMapper.updateOutlineOrderDao(outlineOrder, fileUuid, fileVersionId, -size);
        // 删除章节时，返回下一章节id
        String newOutlineId;
        if (integer == 0) {
            // ==0说明删除的是最后一段
            outlineOrder = String.valueOf(Integer.valueOf(outlineOrder) - 1);
            newOutlineId = fileOperationMapper.getOutlineIdbyOrderDao(outlineOrder, fileUuid, fileVersionId);
        } else {
            // !=0说明不是最后一段
            newOutlineId = fileOperationMapper.getOutlineIdbyOrderDao(outlineOrder, fileUuid, fileVersionId);
        }

        autoCompareService(fileUuid, fileVersionId); // 对比

        HashMap<String, String> ret = new HashMap<>();
        ret.put("outlineId", newOutlineId);
        ret.put("info", "删除成功");
        return TResponseVo.success(ret);

    }

    /**
     * 修改章节题目
     *
     * @param outlineId
     * @param fileUuid
     * @param fileVersionId
     */
    @Override
    public TResponseVo updateOutlineNameService(String outlineId, String fileUuid, String fileVersionId, String outlineText) throws Exception {
        OutLinePojo outlineInfoDao = new OutLinePojo();
        outlineInfoDao.setFileUuid(fileUuid);
        outlineInfoDao.setFileVersionId(fileVersionId);
        outlineInfoDao.setOutlineId(outlineId);
        outlineInfoDao.setOutlineText(outlineText);
        fileOperationMapper.updateOutlineNameDao(outlineInfoDao);
        // 删除目录树缓存信息
        redisUtils.del("getOutlineList" + fileUuid + fileVersionId);
//        // 查缓存是否有大纲信息
//        if (redisUtils.hasKey("getOutlineList" + fileUuid + fileVersionId) && String.valueOf(redisUtils.get("getOutlineList" + fileUuid + fileVersionId)).length()>5) {
//            // 如果有就连同缓存数据一起更新
//            String s = String.valueOf(redisUtils.get("getOutlineList" + fileUuid + fileVersionId));
//            List<OutLinePojo> outlineListDao = JSON.parseObject(s, List.class);
//            return TResponseVo.success(outlineListDao);
//        }
        return TResponseVo.success("修改大纲题目成功");
    }

    /**
     * 编辑替换章节-获取可编辑章节列表
     *
     * @param outlineId
     * @param fileUuid
     * @param fileVersionId
     */
    @Override
    public TResponseVo getReplaceOutlineListService(String outlineId, String fileUuid, String fileVersionId) throws Exception {
        OutLinePojo outlineFatherIdDao = fileOperationMapper.getReplaceOutlineFatherIdDao(outlineId, fileUuid, fileVersionId);
        String outlineFatherId = outlineFatherIdDao.getOutlineFatherId();
        String outlineReplaceGroupId = outlineFatherIdDao.getOutlineReplaceGroupId();
        if (outlineReplaceGroupId == null) {
            List<OutLinePojo> replaceOutlineListDao = fileOperationMapper.getReplaceOutlineList1Dao(outlineId, outlineFatherId, fileUuid, fileVersionId);
            return TResponseVo.success(replaceOutlineListDao);
        } else {
            List<OutLinePojo> replaceOutlineListDao = fileOperationMapper.getReplaceOutlineList2Dao(outlineId, outlineFatherId, outlineReplaceGroupId, fileUuid, fileVersionId);
            return TResponseVo.success(replaceOutlineListDao);
        }
    }

    /**
     * 编辑替换章节-确认
     *
     * @param outlineId
     * @param outlineList
     * @param fileUuid
     * @param fileVersionId
     */
    @Override
    public TResponseVo confirmReplaceOutlineService(String outlineId, List<String> outlineList, String fileUuid, String fileVersionId, String color) throws Exception {
        OutLinePojo outlineFatherIdDao = fileOperationMapper.getReplaceOutlineFatherIdDao(outlineId, fileUuid, fileVersionId);
        String outlineFatherId = outlineFatherIdDao.getOutlineFatherId();
        String outlineReplaceGroupId = outlineFatherIdDao.getOutlineReplaceGroupId();
        // 如果outlinelist为空 说明取消了这个分组
        if (outlineList.size() == 0) {
            fileOperationMapper.cancalOutlineGroupDao(outlineReplaceGroupId);
            // 删除目录树缓存信息
            redisUtils.del("getOutlineList" + fileUuid + fileVersionId);
            return TResponseVo.success("完成");
        }
        // 先获取下拉中有哪些outline_id
        List<OutLinePojo> replaceOutlineListDao = null;
        if (outlineReplaceGroupId == null) {
            replaceOutlineListDao = fileOperationMapper.getReplaceOutlineList1Dao(outlineId, outlineFatherId, fileUuid, fileVersionId);
        } else {
            replaceOutlineListDao = fileOperationMapper.getReplaceOutlineList2Dao(outlineId, outlineFatherId, outlineReplaceGroupId, fileUuid, fileVersionId);
        }

        // 然后开始做分组逻辑
        outlineList.add(outlineId);
        // 如果outlineList存在非必选段落 则本list中所有段落保持统一设置为非必选
        String isNecessary = "1";
        List<OutLinePojo> outlineListByIdsDao = fileOperationMapper.getOutlineListByIdsDao(new JsonKeyUtils().listToString(outlineList, "','"), fileUuid, fileVersionId);
        for (OutLinePojo outLinePojo : outlineListByIdsDao) {
            if (outLinePojo.getIsNecessary().equals("0")) {
                isNecessary = "0";
                break;
            }
        }

        List<String> outlineListDel = new ArrayList<>();
        for (OutLinePojo outLinePojo : replaceOutlineListDao) {
            if (!outlineList.contains(outLinePojo.getOutlineId())) {
                outlineListDel.add(outLinePojo.getOutlineId());
            }
        }
        if (outlineReplaceGroupId == null) {
            // 说明是新增 需要自动分配uuid
            outlineReplaceGroupId = UUID.randomUUID().toString().replaceAll("-", "");
            String outlineListStr = new JsonKeyUtils().listToString(outlineList, "','");
            String outlineListDelStr = new JsonKeyUtils().listToString(outlineListDel, "','");
            fileOperationMapper.confirmReplaceOutlineDao(fileUuid, fileVersionId, outlineListStr, outlineReplaceGroupId, color, isNecessary);
            fileOperationMapper.confirmReplaceOutlineDelDao(fileUuid, fileVersionId, outlineListDelStr);
        } else {
            // 说明是已存在的
            String outlineListStr = new JsonKeyUtils().listToString(outlineList, "','");
            String outlineListDelStr = new JsonKeyUtils().listToString(outlineListDel, "','");
            fileOperationMapper.confirmReplaceOutlineDao(fileUuid, fileVersionId, outlineListStr, outlineReplaceGroupId, color, isNecessary);
            fileOperationMapper.confirmReplaceOutlineDelDao(fileUuid, fileVersionId, outlineListDelStr);
        }
        // 删除目录树缓存信息
        redisUtils.del("getOutlineList" + fileUuid + fileVersionId);
        return TResponseVo.success("完成");
    }

    /**
     * 编辑非必选章节-确认
     *
     * @param outlineId
     */
    @Override
    public TResponseVo confirmNecessaryOutlineService(String outlineId, String fileUuid, String fileVersionId, String isNecessary) throws Exception {
        if (isNecessary == null || (!isNecessary.equals("0") && !isNecessary.equals("1"))) {
            return TResponseVo.error("isNecessary入参值有误");
        }
        List<OutLinePojo> outlineListDao = fileOperationMapper.getOutlineListDao(fileUuid, fileVersionId);
        String outlineReplaceGroupId = null;
        for (OutLinePojo outLinePojo : outlineListDao) {
            if (outLinePojo.getOutlineId().equals(outlineId)) {  // 找到大纲 确认是否有分组信息
                outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                break;
            }
        }
        List<String> list = new ArrayList<>();
        if (outlineReplaceGroupId != null && !outlineReplaceGroupId.equals("null")) {
            for (OutLinePojo outLinePojo : outlineListDao) {
                if (outLinePojo.getOutlineReplaceGroupId() != null && outLinePojo.getOutlineReplaceGroupId().equals(outlineReplaceGroupId)) {
                    list.add(outLinePojo.getOutlineId());
                }
            }
        }
        list.add(outlineId);
        String outlineIdListStr = new JsonKeyUtils().listToString(list, "','");
        fileOperationMapper.updateOutlineNecessaryDao(outlineIdListStr, fileUuid, fileVersionId, isNecessary);
        // 删除目录树缓存信息
        redisUtils.del("getOutlineList" + fileUuid + fileVersionId);
        Map<String, String> retMap = new HashMap<>();
        retMap.put("info", "设置段落非必选属性完成");
        return TResponseVo.success(retMap);
    }

    /**
     * 自动保存
     *
     * @param contentPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo autoSaveService(ContentPojo contentPojo) throws Exception {
//        String searchUuid = contentPojo.getSearchUuid();
        String fileUuid = contentPojo.getFileUuid();
        String fileVersionId = contentPojo.getFileVersionId();
        String outlineId = contentPojo.getOutlineId();
        String userId = contentPojo.getUserId();
        String contentText = contentPojo.getContentText();

        // 搜索模式下
        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
        Elements elements = doc.select("mark");
        if (elements.size() != 0) {
            // 说明是在搜索
            String searchUuid = contentPojo.getSearchUuid();
            if (searchUuid != null) {
                // 更新后入库前更新一下缓存
                if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                    redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                }
                doc = Jsoup.parse(contentText, Parser.xmlParser());
                elements = doc.select("mark");
                for (Element element : elements) {
                    element.before(element.html());
                    element.remove();  // 去除mark标签
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                contentPojo.setContentText(contentText);
            }
        }

        // 相同的fileUuid fileVersionId outlineId的contentText不排队


        fileOperationMapper.updateContentDao(contentPojo);

        return TResponseVo.success("更新成功");
    }


    /**
     * 创建一个影子版本
     *
     * @param docFileIndexPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newShadowVersionService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        // 获取父文件的fileUuid fileVersionId
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
//        DocFileIndexPojo versionAllInfoDao = fileOperationMapper.getVersionAllInfoDao1(fileUuid, fileVersionId);
        // 升级版本号

        docFileIndexPojo.setFileParentId(fileUuid);
        docFileIndexPojo.setOldFileVersionId(fileVersionId);
        fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        if (docFileIndexPojo.getDraftVersionId() == null) {
            fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
        } else {
            fileVersionId = docFileIndexPojo.getDraftVersionId();
        }
        docFileIndexPojo.setFileUuid(fileUuid);
        docFileIndexPojo.setFileVersionId(fileVersionId);

        String userId = docFileIndexPojo.getCreateUserId();
        docFileIndexPojo.setCreateTime(new Date());

        try {
            int j = 0;
            int i = 0;
            // 创建索引
//            不需要变更索引

            // 创建版本
            i = fileOperationMapper.addVersionFileVersionDao(docFileIndexPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(701);
                throw fileIndexException;
            }

            // 创建大纲
            List<OutLinePojo> newVersionFileOutlineDao = fileOperationMapper.getVersionFileOutlineDao(docFileIndexPojo);
            // 设置groupId替换用的键值
            HashMap<String, String> groupIdMap = new HashMap<>();
            for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                if (outLinePojo.getOutlineReplaceGroupId() == null) {
                    continue;
                }
                String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
            }
            String sql = "";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = formatter.format(new Date());
            for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                j++;
                // 重新排序
                outLinePojo.setOutlineOrder(String.valueOf(j));
                String outlineReplaceGroupId = null;
                String color = null;
                if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                    outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                }
                if (outLinePojo.getColor() != null) {
                    color = "\"" + outLinePojo.getColor() + "\"";
                }
                sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + docFileIndexPojo.getFileParentId() + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + outLinePojo.getIsNecessary() + "\",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
                fileOperationMapper.addVersionFileOutlineDao(sql);
            }
            // 创建内容
            List<ContentPojo> versionFileContentDao = fileOperationMapper.getVersionFileContentDao(docFileIndexPojo);
            sql = "";
            for (ContentPojo contentPojo : versionFileContentDao) {
//                String contentText = contentPojo.getContentText();
//                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//                Elements elements = doc.select("mark");
//                Elements elementsTmp = new Elements();
//                elementsTmp.addAll(elements);
//                for (Element element : elementsTmp) {
//                    element.before(element.html());
//                    element.remove();  // 去除mark标签
//                }
//                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                contentText = contentText.replaceAll("</br>", "");
//                contentPojo.setContentText(contentText);
                sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentPojo.getContentText()) + ",\"" + contentPojo.getOutlineId() + "\",\"" + docFileIndexPojo.getFileParentId() + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
                fileOperationMapper.addVersionFileContentDao(sql);
            }
            // 创建参数
            fileOperationMapper.addVersionContentParamDao(docFileIndexPojo);

            // 创建标注
            fileOperationMapper.addVersionContentTagDao(docFileIndexPojo);

            // 创建书签
            fileOperationMapper.addVersionContentBookmarkDao(docFileIndexPojo);

            // 创建数据管理(4张)
            fileOperationMapper.addVersionContentDmDbDao(docFileIndexPojo);
            fileOperationMapper.addVersionContentDmTableDao(docFileIndexPojo);
            fileOperationMapper.addVersionContentDmColumnsDao(docFileIndexPojo);
            String hashMod1 = new HashUtils().getHashMod(docFileIndexPojo.getFileParentId() + docFileIndexPojo.getFileVersionId());
            String hashMod2 = new HashUtils().getHashMod(docFileIndexPojo.getFileParentId() + docFileIndexPojo.getOldFileVersionId());
            System.out.println("表编号为: " + hashMod1);
            splitTableMapper.addVersionContentDmDataDao(docFileIndexPojo, hashMod1, hashMod2, dmContentDataDatabase);

            // 创建数据表(如果有)
            fileOperationMapper.addVersionContentDbDao(docFileIndexPojo);

            // 创建附属文件
            // 查找附属
            List<DocFileIndexPojo> allSubsidiaryFileListDao = fileOperationMapper.getAllSubsidiaryFileListDao(docFileIndexPojo);
            for (DocFileIndexPojo fileIndexPojo : allSubsidiaryFileListDao) {
//                System.out.println("allSubsidiaryFileListDao : " + allSubsidiaryFileListDao.size());
//                System.out.println("docFileIndexPojo : " + JSON.toJSONString(docFileIndexPojo));
//                System.out.println("fileIndexPojo : " + JSON.toJSONString(fileIndexPojo));
                // 获取父文件的fileUuid fileVersionId
                fileUuid = fileIndexPojo.getFileUuid();
                fileVersionId = fileIndexPojo.getFileVersionId();
                fileIndexPojo.setFileParentId(fileUuid);
                fileIndexPojo.setOldFileVersionId(fileVersionId);
                // 把主文件的fileUuid写到附属文件的mainFileUuid字段中
                fileIndexPojo.setMainFileUuid(docFileIndexPojo.getFileParentId());
                fileIndexPojo.setMainFileVersionId(docFileIndexPojo.getFileVersionId());
                userId = docFileIndexPojo.getCreateUserId();
                fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                fileIndexPojo.setFileUuid(fileUuid);
                fileIndexPojo.setFileVersionId(fileVersionId);

                fileIndexPojo.setCreateTime(new Date());

//                System.out.println("fileIndexPojo : " + JSON.toJSONString(fileIndexPojo));
                j = 0;
                i = 0;
                // 创建索引
                i = fileOperationMapper.addDeriveFileIndexDao1(fileIndexPojo);
                if (i == 0) {
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(700);
                    throw fileIndexException;
                }

                // 创建版本
                i = fileOperationMapper.addDeriveFileVersionDao(fileIndexPojo);
                if (i == 0) {
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(701);
                    throw fileIndexException;
                }

                // 创建大纲
                newVersionFileOutlineDao = fileOperationMapper.getDeriveFileOutlineDao(fileIndexPojo);
                // 设置groupId替换用的键值
                groupIdMap = new HashMap<>();
                for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                    if (outLinePojo.getOutlineReplaceGroupId() == null) {
                        continue;
                    }
                    String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                    groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
                }
                sql = "";
                formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateString = formatter.format(new Date());
                for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                    j++;
                    // 重新排序
                    outLinePojo.setOutlineOrder(String.valueOf(j));
                    String outlineReplaceGroupId = null;
                    String color = null;
                    if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                        outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                    }
                    if (outLinePojo.getColor() != null) {
                        color = "\"" + outLinePojo.getColor() + "\"";
                    }
                    sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + outLinePojo.getIsNecessary() + "\",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 1);
//                    System.out.println("sql : " + sql);
                    fileOperationMapper.newShadowVersion2Dao(sql);
                }
                // 创建内容
                versionFileContentDao = fileOperationMapper.getDeriveFileContentDao(fileIndexPojo);
                sql = "";
                for (ContentPojo contentPojo : versionFileContentDao) {
//                    String contentText = contentPojo.getContentText();
//                    Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//                    Elements elements = doc.select("mark");
//                    Elements elementsTmp = new Elements();
//                    elementsTmp.addAll(elements);
//                    for (Element element : elementsTmp) {
//                        element.before(element.html());
//                        element.remove();  // 去除mark标签
//                    }
//                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                    contentText = contentText.replaceAll("</br>", "");
//                    contentPojo.setContentText(contentText);
                    sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentPojo.getContentText()) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 1);
                    fileOperationMapper.addDeriveFileContentDao(sql);
                }
                // 创建参数
                fileOperationMapper.addDeriveContentParamDao(fileIndexPojo);

                // 创建标注
                fileOperationMapper.addDeriveContentTagDao(fileIndexPojo);

                // 创建书签
                fileOperationMapper.addDeriveContentBookmarkDao(fileIndexPojo);

                // 创建数据管理(4张)
                fileOperationMapper.addDeriveContentDmDbDao(fileIndexPojo);
                fileOperationMapper.addDeriveContentDmTableDao(fileIndexPojo);
                fileOperationMapper.addDeriveContentDmColumnsDao(fileIndexPojo);
                hashMod1 = new HashUtils().getHashMod(fileIndexPojo.getFileUuid() + fileIndexPojo.getFileVersionId());
                hashMod2 = new HashUtils().getHashMod(fileIndexPojo.getFileParentId() + fileIndexPojo.getOldFileVersionId());
                System.out.println("表编号为: " + hashMod1);
                splitTableMapper.addDeriveContentDmDataDao(fileIndexPojo, hashMod1, hashMod2, dmContentDataDatabase);

                // 创建数据表(如果有)
                fileOperationMapper.addDeriveContentDbDao(fileIndexPojo);
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(704);
            throw fileIndexException;
        }

        Map<String, String> ret = new HashMap<>();
        ret.put("fileUuid", docFileIndexPojo.getFileParentId());
        ret.put("fileVersionId", docFileIndexPojo.getFileVersionId());
        ret.put("info", "影子版本创建成功");
        return TResponseVo.success(ret);
    }


    /**
     * 标签库-新增标签分组
     *
     * @param docLabelPojo
     */
    @Override
    public TResponseVo addLabelGroupService(DocLabelPojo docLabelPojo) throws Exception {
        String labelGroupName = docLabelPojo.getLabelGroupName();
        if (StringUtils.isBlank(labelGroupName)) {
            return TResponseVo.error(ResponseEnum.ERROR, "标签分组名称不能为空");
        }
        // 判断词条名重复
        try {
            Integer integer = fileOperationMapper.checkLabelGroupNameDao(labelGroupName, null);
            if (integer >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", labelGroupName, "当前类错误码-901-标签分组名重复");
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(901);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", labelGroupName, "当前类错误码-902-检查标签分组名重复的SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(902);
            throw fileIndexException;
        }
        String labelGroupId = UUID.randomUUID().toString().replaceAll("-", "");
        docLabelPojo.setLabelGroupId(labelGroupId);
        fileOperationMapper.addLabelGroupDao(docLabelPojo);
        HashMap<String, String> ret = new HashMap<>();
        ret.put("labelGroupId", labelGroupId);
        ret.put("info", "创建标签分组成功");
        return TResponseVo.success(ret);
    }

    /**
     * 标签库-删除标签分组
     *
     * @param docLabelPojo
     */
    @Override
    public TResponseVo delLabelGroupService(DocLabelPojo docLabelPojo) throws Exception {
        String labelGroupId = docLabelPojo.getLabelGroupId();
        Integer integer = fileOperationMapper.checkLabelGroupUseDao(labelGroupId);
        if (integer != 0) {
            return TResponseVo.error("分组下有标签不可删除");
        }
        // 删除分组
        fileOperationMapper.delLabelGroupDao(labelGroupId);
        // 将用到该分组的标签的分组信息置为空
        fileOperationMapper.delLabelGroupIdDao(labelGroupId);
        return TResponseVo.success("删除标签分组成功");
    }

    /**
     * 标签库-获取标签分组列表
     *
     * @param
     */
    @Override
    public TResponseVo getLabelGroupListService(PagePojo pagePojo) throws Exception {

        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        List<DocLabelPojo> labelGroupListDao = fileOperationMapper.getLabelGroupListDao(paramNameLike, null);

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(labelGroupListDao);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(labelGroupListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", labelGroupListDao.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 标签库-获取标签分组键值对
     *
     * @param
     */
    @Override
    public TResponseVo getLabelGroupLVListService() throws Exception {
        List<LabelValuePojo> labelGroupListDao = fileOperationMapper.getLabelGroupLVListDao();
        return TResponseVo.success(labelGroupListDao);
    }

    /**
     * 标签库-增
     *
     * @param docLabelPojo
     */
    @Override
    public TResponseVo addLabelService(DocLabelPojo docLabelPojo) throws Exception {
        String labelContent = docLabelPojo.getLabelContent();
        if (StringUtils.isBlank(labelContent)) {
            return TResponseVo.error(ResponseEnum.ERROR, "标签内容不能为空");
        }
        try {
            Integer integer = fileOperationMapper.checkLabelNameDao(labelContent, null);
            if (integer >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", labelContent, "当前类错误码-903-标签名重复");
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(903);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (UncategorizedSQLException e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", labelContent, "当前类错误码-905-检查标签名是否合法");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(905);
            throw fileIndexException;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", labelContent, "当前类错误码-904-检查标签名重复的SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(904);
            throw fileIndexException;
        }
        String labelUuid = UUID.randomUUID().toString().replaceAll("-", "");
        docLabelPojo.setLabelUuid(labelUuid);
        // list转str
        if (docLabelPojo.getFileTypeId() != null) {
            String fileTypeId = JSON.toJSONString((List) docLabelPojo.getFileTypeId());
            docLabelPojo.setFileTypeId(fileTypeId);
        } else {
            docLabelPojo.setFileTypeId(null);
        }
        fileOperationMapper.addLabelDao(docLabelPojo);
        HashMap<String, String> ret = new HashMap<>();
        ret.put("labelUuid", labelUuid);
        ret.put("info", "创建成功");
        return TResponseVo.success(ret);
    }

    /**
     * 标签库-删
     *
     * @param labelUuid
     */
    @Override
    public TResponseVo delLabelService(String labelUuid) throws Exception {
        fileOperationMapper.delLabelDao(labelUuid);
//        List<DocFileIndexPojo> fileListByLabelDao = fileOperationMapper.getFileListByLabelDao(labelUuid);
//        for (DocFileIndexPojo docFileIndexPojo : fileListByLabelDao) {
//            String fileLabelList = docFileIndexPojo.getFileLabelList();
//            List<String> fileLabelIdList = new JsonKeyUtils().stringToList(fileLabelList);
//            fileLabelIdList.remove(labelUuid);
//            fileLabelList = new JsonKeyUtils().listToString(fileLabelIdList);
//            docFileIndexPojo.setFileLabelList(fileLabelList);
//            fileOperationMapper.updateFileLabelDao(docFileIndexPojo);
//        }
        return TResponseVo.success("删除成功");
    }

    /**
     * 标签库-改
     *
     * @param docLabelPojo
     */
    @Override
    public TResponseVo updateLabelService(DocLabelPojo docLabelPojo) throws Exception {
        String labelContent = docLabelPojo.getLabelContent();
        String labelUuid = docLabelPojo.getLabelUuid();
        try {
            Integer integer = fileOperationMapper.checkLabelNameDao(labelContent, labelUuid);
            if (integer >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", labelContent, "当前类错误码-903-标签名重复");
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(903);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (UncategorizedSQLException e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", labelContent, "当前类错误码-905-检查标签名是否合法");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(905);
            throw fileIndexException;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", labelContent, "当前类错误码-904-检查标签名重复的SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(904);
            throw fileIndexException;
        }
        String fileTypeId = JSON.toJSONString((List) docLabelPojo.getFileTypeId());
        docLabelPojo.setFileTypeId(fileTypeId.equals("null") ? null : fileTypeId);
        fileOperationMapper.updateLabelDao(docLabelPojo);
        return TResponseVo.success("更新成功");
    }

    /**
     * 标签库-查
     *
     * @param labelUuid
     */
    @Override
    public TResponseVo getLabelInfoService(String labelUuid) throws Exception {
        DocLabelPojo labelInfoDao = fileOperationMapper.getLabelInfoDao(labelUuid);
        if (labelInfoDao.getFileTypeId() != null) {
            String fileTypeId = String.valueOf(labelInfoDao.getFileTypeId());
            List<String> list = JSON.parseObject(fileTypeId, List.class);
            labelInfoDao.setFileTypeId(list);
            List<LabelValuePojo> fileTypeListDao = fileOperationMapper.getFileTypeListDao();
            ArrayList<String> fileTypeNameList = new ArrayList<>();
            for (String s : list) {
                for (LabelValuePojo labelValuePojo : fileTypeListDao) {
                    if (labelValuePojo.getValue().equals(s)) {
                        fileTypeNameList.add(labelValuePojo.getLabel());
                        break;
                    }
                }
            }
            String fileTypeName = new JsonKeyUtils().listToString(fileTypeNameList);
            labelInfoDao.setFileTypeName(fileTypeName);
        }
        return TResponseVo.success(labelInfoDao);
    }

    /**
     * 标签库-查
     *
     * @param labelGroupId
     */
    @Override
    public TResponseVo getLabelListService(PagePojo pagePojo, String labelGroupId, String fileTypeId) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        List<DocLabelPojo> labelListDao = fileOperationMapper.getLabelListDao(paramNameLike, null, labelGroupId);
        if (fileTypeId != null) {
            List<DocLabelPojo> labelInfoListDaoTmp = new ArrayList<>();
            labelInfoListDaoTmp.addAll(labelListDao);
            // 筛选出fileTypeId下的标签
            for (DocLabelPojo docLabelPojo : labelInfoListDaoTmp) {
                List<String> list = JSON.parseObject(String.valueOf(docLabelPojo.getFileTypeId()), List.class);
                if (list == null || !list.contains(fileTypeId)) {
                    labelListDao.remove(docLabelPojo);
                }
            }
        }

        // 转fileTypeId 补充name
        List<LabelValuePojo> fileTypeListDao = fileOperationMapper.getFileTypeListDao();
        for (DocLabelPojo labelInfoDao : labelListDao) {
            if (labelInfoDao.getFileTypeId() != null) {
                String fileTypeIdStr = String.valueOf(labelInfoDao.getFileTypeId());
                List<String> list = JSON.parseObject(fileTypeIdStr, List.class);
                labelInfoDao.setFileTypeId(list);
                ArrayList<String> fileTypeNameList = new ArrayList<>();
                if (list != null) {
                    for (String s : list) {
                        for (LabelValuePojo labelValuePojo : fileTypeListDao) {
                            if (labelValuePojo.getValue().equals(s)) {
                                fileTypeNameList.add(labelValuePojo.getLabel());
                                break;
                            }
                        }
                    }
                }
                labelInfoDao.setFileTypeName(fileTypeNameList);
            }
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(labelListDao);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(labelListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", labelListDao.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 标签库-查键值对
     *
     * @param labelGroupId
     */
    @Override
    public TResponseVo getLabelLVListService(String labelGroupId) throws Exception {
        List<LabelValuePojo> labelListDao = fileOperationMapper.getLabelLVListDao(labelGroupId);
        return TResponseVo.success(labelListDao);
    }


//    /**
//     * 编辑页参数清单-新增
//     *
//     * @param docParamsPojo
//     */
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public TResponseVo addContentParamService(DocParamsPojo docParamsPojo) throws Exception {
//        String paramsUuid = docParamsPojo.getParamsUuid();
//        String fileUuid = docParamsPojo.getFileUuid();
//        String fileVersionId = docParamsPojo.getFileVersionId();
//        String outlineId = docParamsPojo.getOutlineId();
////        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
//        String uuid = docParamsPojo.getUuid();
//        ContentPojo contentPojo = new ContentPojo();
//        String contentText = docParamsPojo.getContentText();
//        contentPojo.setOutlineId(outlineId);
//        contentPojo.setFileUuid(fileUuid);
//        contentPojo.setFileVersionId(fileVersionId);
//
//        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//        Elements elements = doc.select("mark");
////        String regEx_html="<mark id=\"[0-9a-z]{32}\">(.*?)(</mark>)?</mark>"; //定义HTML标签的正则表达式
////        Pattern p_html=Pattern.compile(regEx_html,Pattern.CASE_INSENSITIVE);
////        Matcher m_html=p_html.matcher(contentText);
////        String group = "";
////        if (m_html.find()){
////            group = m_html.group(1);
////        }
////        contentText=m_html.replaceAll(group);
////        contentPojo.setContentText(contentText);
//        for (Element element : elements) {
//            contentText = contentText.replace(String.valueOf(element), element.text());
//        }
//        contentPojo.setContentText(contentText);
//
//        fileOperationMapper.addContentParamDao(uuid, paramsUuid, fileUuid, fileVersionId, outlineId, new Date());
//        fileOperationMapper.updateContentDao(contentPojo);
//        HashMap<String, String> ret = new HashMap<>();
//        ret.put("info", "添加参数成功");
//        ret.put("uuid", uuid);
//        return TResponseVo.success(ret);
//    }
//

    /**
     * 编辑页参数清单-删除
     *
     * @param
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delContentParamService(DocParamsPojo docParamsPojo) throws Exception {
        String uuid = docParamsPojo.getUuid();
        String fileUuid = docParamsPojo.getFileUuid();
        String fileVersionId = docParamsPojo.getFileVersionId();
        // 删除刷库
        fileOperationMapper.delContentParamDao(uuid, fileUuid, fileVersionId);
        // 删除文本内数据
        DocParamsPojo contentParamInfoDao = fileOperationMapper.getContentParamInfoDao(uuid, fileUuid, fileVersionId);
        String outlineId = contentParamInfoDao.getOutlineId();
        ContentPojo contentDao = fileOperationMapper.getContentDao(fileUuid, fileVersionId, outlineId);
        String contentText = contentDao.getContentText();


        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
        Elements elements = doc.select("parameter[uuid=" + uuid + "]");
        Elements elementsBlock = doc.select("parameterBlock[uuid=" + uuid + "]");
        // elements只会包含一个结果
        elements.addAll(elementsBlock);
        for (Element element : elements) {
            element.remove();
        }
        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
        contentText = contentText.replaceAll("</br>", "");
        contentDao.setContentText(contentText);


        // 搜索模式下
        String searchUuid = docParamsPojo.getSearchUuid();
        String userId = docParamsPojo.getUserId();
        if (searchUuid != null) {
            // 更新后入库前更新一下缓存
            if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
            }

            doc = Jsoup.parse(contentText, Parser.xmlParser());
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            elements = doc.select("mark");
            for (Element element : elements) {
                element.before(element.html());
                element.remove();  // 去除mark标签
            }
            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
            contentText = contentText.replaceAll("</br>", "");
            contentDao.setContentText(contentText);
        }

        fileOperationMapper.updateContentDao(contentDao);

        return TResponseVo.success("去除参数成功");
    }

    /**
     * 编辑页参数清单-查询
     *
     * @param
     */
    @Override
    public TResponseVo getContentParamService(DocParamsPojo docParamsPojo) throws Exception {
        String outlineId = docParamsPojo.getOutlineId();
        if (outlineId == null) {
            docParamsPojo.setType("all");
        } else {
            docParamsPojo.setType(null);
        }
        String paramNameLike = docParamsPojo.getParamNameLike();
        if (StringUtils.isBlank(paramNameLike)) {
            docParamsPojo.setParamNameLike(paramNameLike);
        } else {
            docParamsPojo.setParamNameLike(StringEscapeUtils.escapeSql(paramNameLike));
        }
        List<DocParamsPojo> contentParamDao = fileOperationMapper.getContentParamDao(docParamsPojo);

        // 是否全局顺序排序 (按照文章内出现参数的顺序排序 以文章为主)
        String orderMode = docParamsPojo.getOrderMode();
        if (true || (orderMode != null && orderMode.equals("all"))) {
            // 当orderMode排序模式为all时 按照全局首次出现顺序排序
            String fileUuid = docParamsPojo.getFileUuid();
            String fileVersionUuid = docParamsPojo.getFileVersionId();
            List<String> contentAllDao = fileOperationMapper.getContentAllDao(fileUuid, fileVersionUuid);
            ArrayList<String> paramOrderList = new ArrayList<>();
            for (String s : contentAllDao) {
                Document doc = Jsoup.parse(s);
                Elements parameters = doc.select("parameter,parameterblock");
                for (Element parameter : parameters) {
                    if (!paramOrderList.contains(parameter.attr("uuid"))) {
                        paramOrderList.add(parameter.attr("uuid"));
                    }
                }
            }
            List<DocParamsPojo> contentParamDaoRet = new ArrayList<>();
            for (String s : paramOrderList) {
                for (int i = 0; i < contentParamDao.size(); i++) {
                    if (contentParamDao.get(i).getUuid().equals(s)) {
                        contentParamDaoRet.add(contentParamDao.get(i));
                        contentParamDao.remove(contentParamDao.get(i));
                        break;
                    }
                }
            }
            contentParamDaoRet.addAll(contentParamDao);
            contentParamDao = contentParamDaoRet;
        }

        // 如果只看参数发生变动的
        String getChange = docParamsPojo.getGetChange();
        if (getChange != null) {
            List<DocParamsPojo> contentParamDaoTmp = new ArrayList<>();
            contentParamDaoTmp.addAll(contentParamDao);
            for (DocParamsPojo paramsPojo : contentParamDaoTmp) {
                if (paramsPojo.getIsChange().equals("0")) {
                    contentParamDao.remove(paramsPojo);
                }
            }
        }

        //去重
        List<DocParamsPojo> contentParamDao1 = new ArrayList<>();
        ArrayList<String> paramsIdStringList = new ArrayList<>();
        // 加显示类型的styleContent
        List<LabelValuePojo> paramTypeStyleDiDao = fileOperationMapper.getParamTypeStyleDiDao(null);
        for (DocParamsPojo paramsPojo : contentParamDao) {
            if (!paramsIdStringList.contains(paramsPojo.getParamsUuid())) {
                paramsIdStringList.add(paramsPojo.getParamsUuid());
                ArrayList<Map<String, String>> Strings = new ArrayList<>();
                Map<String, String> map = new HashMap<>();
                map.put("uuid", paramsPojo.getUuid());
                map.put("outLineId", paramsPojo.getOutlineId());
                map.put("showText", paramsPojo.getShowText());
                map.put("styleId", paramsPojo.getStyleId());
                if (paramsPojo.getStyleId() != null) {
                    for (LabelValuePojo labelValuePojo : paramTypeStyleDiDao) {
                        if (labelValuePojo.getValue().equals(paramsPojo.getStyleId())) {
                            map.put("styleContent", labelValuePojo.getLabel());
                        }
                    }
                }
                map.put("isUnderLine", paramsPojo.getIsUnderLine());
                map.put("unit", paramsPojo.getUnit());
                map.put("remark", paramsPojo.getRemark());
                map.put("outlineOrder", paramsPojo.getOutlineOrder());
                map.put("unitName", paramsPojo.getUnitName());
                map.put("paramsText", String.valueOf(paramsPojo.getParamsText()));
                map.put("matrixDisplay", paramsPojo.getMatrixDisplay());
                map.put("matrixMode", paramsPojo.getMatrixMode());
                Strings.add(map);
                paramsPojo.setUuidList(Strings);
                contentParamDao1.add(paramsPojo);
            } else {
                for (DocParamsPojo pojo : contentParamDao1) {
                    if (pojo.getParamsUuid().equals(paramsPojo.getParamsUuid())) {
                        Map<String, String> map = new HashMap<>();
                        map.put("uuid", paramsPojo.getUuid());
                        map.put("outLineId", paramsPojo.getOutlineId());
                        map.put("showText", paramsPojo.getShowText());
                        map.put("styleId", paramsPojo.getStyleId());
                        if (paramsPojo.getStyleId() != null) {
                            for (LabelValuePojo labelValuePojo : paramTypeStyleDiDao) {
                                if (labelValuePojo.getValue().equals(paramsPojo.getStyleId())) {
                                    map.put("styleContent", labelValuePojo.getLabel());
                                }
                            }
                        }
                        map.put("isUnderLine", paramsPojo.getIsUnderLine());
                        map.put("unit", paramsPojo.getUnit());
                        map.put("remark", paramsPojo.getRemark());
                        map.put("outlineOrder", paramsPojo.getOutlineOrder());
                        map.put("unitName", paramsPojo.getUnitName());
                        map.put("paramsText", String.valueOf(paramsPojo.getParamsText()));
                        map.put("matrixDisplay", paramsPojo.getMatrixDisplay());
                        map.put("matrixMode", paramsPojo.getMatrixMode());
                        pojo.getUuidList().add(map);
                    }
                }
            }
        }
        // 设置长度信息
        for (DocParamsPojo paramsPojo : contentParamDao1) {
            paramsPojo.setUuidListLength(paramsPojo.getUuidList().size());
            List<Object> list = JSON.parseObject(paramsPojo.getParamsGroupId(), List.class);
            paramsPojo.setParamsGroupIdList(list);
            List<Object> list1 = JSON.parseObject(paramsPojo.getParamsRange(), List.class);
            paramsPojo.setParamsRangeList(list1);
            // 矩阵参数头
            List paramsColumns = JSON.parseObject((String) paramsPojo.getParamsColumns(), List.class);
            paramsPojo.setParamsColumns(paramsColumns);


            // 判断默认值是否需要转换格式
            String paramsTypeId = paramsPojo.getParamsTypeId();
            if (paramsTypeId.equals("20") || paramsTypeId.equals("60") || paramsTypeId.equals("70") || paramsTypeId.equals("95")) {
                Object defaultValue = paramsPojo.getDefaultValue();
                if (defaultValue != null) {
                    List list2 = JSON.parseObject(String.valueOf(defaultValue), List.class);
                    if (list2 != null && list2.size() != 0) {
                        paramsPojo.setDefaultValue(list2);
                    } else {
                        paramsPojo.setDefaultValue(null);
                    }
                }
            } else if (paramsTypeId.equals("90")) {  // 数值 金额
                Object defaultValue = paramsPojo.getDefaultValue();
                if (defaultValue != null) {
                    Map map1 = new HashMap();
                    try {  // 数值 金额  默认值不是map直接认为默认值无效 不返回给前端了
                        map1 = JSON.parseObject(String.valueOf(defaultValue), Map.class, Feature.NonStringKeyAsString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (map1 != null && map1.size() != 0) {
                        paramsPojo.setDefaultValue(map1);
                    } else {
                        paramsPojo.setDefaultValue(null);
                    }
                }
            } else if (paramsTypeId.equals("80")) {  // 金额
                Object defaultValue = paramsPojo.getDefaultValue();
                if (defaultValue != null) {
                    Map<Object, Object> map1 = new HashMap();
                    try {  // 数值 金额  默认值不是map直接认为默认值无效 不返回给前端了
                        map1 = JSON.parseObject(String.valueOf(defaultValue), Map.class, Feature.NonStringKeyAsString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (map1 != null && map1.size() != 0) {
                        paramsPojo.setDefaultValue(map1);
                    } else {
                        paramsPojo.setDefaultValue(null);
                    }
                }
            }
        }
        // 设置单位集合
        for (DocParamsPojo paramsPojo : contentParamDao1) {
            // 只操作数值和金额
            if (paramsPojo.getParamsTypeId().equals("80") || paramsPojo.getParamsTypeId().equals("90")) {
                List<Map<String, String>> uuidList = paramsPojo.getUuidList();
                List<Map<String, String>> unitMapList = new ArrayList<>();
                List<String> unitStrList = new ArrayList<>();
                Map<String, Double> unitAndValueMap = new HashMap<>();
                for (Map<String, String> stringStringMap : uuidList) {
                    String unitName = stringStringMap.get("unitName");
                    String unit = stringStringMap.get("unit");
                    if (unit != null && !unit.equals("") && !unitStrList.contains(unit)) {
                        // 设置单位集合
                        Map<String, String> unitMap = new HashMap<>();
                        unitMap.put("label", unitName);
                        unitMap.put("value", unit);
                        unitStrList.add(unit);
                        unitMapList.add(unitMap);
                        // 设置设置单位集合和对应的回显数据
                        unitAndValueMap.put(unit, (stringStringMap.get("paramsText").equals("null") || StringUtils.isBlank(stringStringMap.get("paramsText"))) ? null : new Double(stringStringMap.get("paramsText")));
                    }
                }
                paramsPojo.setUnitList(unitMapList);
                paramsPojo.setParamsText(unitAndValueMap);
            }
        }

        contentParamDao = contentParamDao1;
//        System.out.println(contentParamDao);
        // 加分组名字
        List<LabelValuePojo> paramGroupDiDao = fileOperationMapper.getParamGroupDiDao();
        for (DocParamsPojo paramsPojo : contentParamDao) {
            String paramsGroupId = paramsPojo.getParamsGroupId();
            List<String> list = JSON.parseObject(paramsGroupId, List.class);
            ArrayList<String> paramGroupNameList = new ArrayList<>();
            if (list != null) {
                for (String s : list) {
                    for (LabelValuePojo labelValuePojo : paramGroupDiDao) {
                        if (labelValuePojo.getValue().equals(s)) {
                            paramGroupNameList.add(labelValuePojo.getLabel());
                        }
                    }
                }
            }
            paramsPojo.setParamsGroupNameList(paramGroupNameList);

            // 如果是资源附件类型参数 若参数值为空,则置为[]
            if (paramsPojo.getParamsTypeId().equals("70")) {
                if (paramsPojo.getParamsText() == null) {
                    paramsPojo.setParamsText(new ArrayList<>());
                }
            }

            // 特殊处理: 除了招标文件和投标文件 其余类型都不返回paramsUseSceneId
            if (paramsPojo.getParamsUseSaturation() != null && !"MB-ZBWJ".equals(paramsPojo.getParamsUseSaturation())) {
                paramsPojo.setParamsUseSceneId(null);
            }
        }


        // 根据出现顺序排序 即按outlineOrder排序
//        for (DocParamsPojo paramsPojo : contentParamDao) {
//            List<Map<String, String>> uuidList = paramsPojo.getUuidList();
//            List<Map<String, String>> outlineId1 = new JsonKeyUtils().orderMapList3(uuidList, "outlineOrder");
//            paramsPojo.setUuidList(outlineId1);
//
//
//        }


        // 多选的paramText返回list<String>
        List<Map<String, Object>> listRet = JSON.parseObject(JSON.toJSONString(contentParamDao), List.class);
        for (Map<String, Object> map : listRet) {
            if (map.get("paramsTypeId").equals("20") && map.get("paramsText") != null) {  // 多选
                String paramsText = String.valueOf(map.get("paramsText"));
                List<String> strings = new JsonKeyUtils().stringToList(paramsText);
                map.put("paramsText", strings);
            } else if (map.get("paramsTypeId").equals("70") && map.get("paramsText") != null) {  // 附件
                String paramsText = String.valueOf(map.get("paramsText"));
                List<String> strings = JSON.parseObject(paramsText, List.class);
                map.put("paramsText", strings);
            } else if (map.get("paramsTypeId").equals("95") && map.get("paramsText") != null) { // 矩阵
                String paramsText = String.valueOf(map.get("paramsText"));
                List<String> strings = JSON.parseObject(paramsText, List.class);
                map.put("paramsText", strings);
            } else if (map.get("paramsTypeId").equals("60") && map.get("paramsText") != null) { // 图片
                String paramsText = String.valueOf(map.get("paramsText"));
                List<String> strings = JSON.parseObject(paramsText, List.class);
                map.put("paramsText", strings);
            }

            if (map.get("remark") != null && StringUtils.isNotBlank(String.valueOf(map.get("remark")))) {
                try {
                    Map m = JSON.parseObject(String.valueOf(map.get("remark")), Map.class);
                    map.put("remark", m);
                } catch (Exception ignored) {
                }
            }


        }
        return TResponseVo.success(listRet);
    }

    /**
     * 编辑页参数清单-修改文内参数
     *
     * @param docParamsPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateContentParamService(DocParamsPojo docParamsPojo) throws Exception {
        String paramsUuid = docParamsPojo.getParamsUuid();
        String fileUuid = docParamsPojo.getFileUuid();
        String fileVersionId = docParamsPojo.getFileVersionId();
        String paramsName = docParamsPojo.getParamsName();
        String styleId = docParamsPojo.getStyleId();  // 没传
        String isUnderLine = docParamsPojo.getIsUnderLine();
        String unit = docParamsPojo.getUnit();

        // 金额类型参数根据styleId指定unit
        if (styleId == null) {
            System.out.println("修改了模板参数,但不是从其他类型转为金额类型");
        } else if ("632007".equals(styleId) || "632008".equals(styleId) || "632010".equals(styleId) || "632012".equals(styleId) || "632013".equals(styleId) || "632014".equals(styleId)) {
            docParamsPojo.setUnit("770001");  // 元
        } else if ("632009".equals(styleId) || "632011".equals(styleId) || "6320071".equals(styleId)) {
            docParamsPojo.setUnit("770002");  // 万元
        }
        String showText = docParamsPojo.getShowText();
        String paramsNameOri = null;  // 用来判断名字是否变化

        // 验证新参数名是否在本文内重名
        List<String> contentAllParamNameDao = fileOperationMapper.getContentAllParamNameDao(fileUuid, fileVersionId, paramsUuid);
        if (contentAllParamNameDao.contains(paramsName)) {
            return TResponseVo.error(ResponseEnum.ERROR, "新参数名在本文内已存在");
        }
        // 数据准备
        List<DocParamsPojo> contentByParamDao = fileOperationMapper.getContentInfoByParamDao(fileUuid, fileVersionId, paramsUuid);
        // 获取哪些参数有show_text,拿到这些参数的uuid
        List<String> uuidList = new ArrayList<>();
        for (DocParamsPojo paramsPojo : contentByParamDao) {
            if (StringUtils.isNotBlank(paramsPojo.getShowText())) {
                uuidList.add(paramsPojo.getUuid());
            }
        }
        // 去重相同的段落,保证每个段落需要且只需要处理一次
        List<String> outlineIdList = new ArrayList<>();
        List<DocParamsPojo> contentByParamDao1 = new ArrayList<>();
        for (DocParamsPojo paramsPojo : contentByParamDao) {
            if (paramsNameOri == null) {
                paramsNameOri = paramsPojo.getParamsName();
            }
            if (!outlineIdList.contains(paramsPojo.getOutlineId())) {
                outlineIdList.add(paramsPojo.getOutlineId());
                contentByParamDao1.add(paramsPojo);
            }
        }

        // 更新hf_content_params的字段
        // 值域
        List paramsRangeList = docParamsPojo.getParamsRangeList();
        // 判断是否有label 没有禁止提交
        if (paramsRangeList != null) {
            for (Map<String, String> m : (List<Map>) (List) paramsRangeList) {
                if (StringUtils.isBlank(m.get("label"))) {
                    return TResponseVo.error("值域不能为空");
                }
            }
        }
        String paramsRange = JSON.toJSONString(paramsRangeList);
        docParamsPojo.setParamsRange(paramsRange);
        docParamsPojo.setParamsRangeList(null);
        // 矩阵参数头
        List paramsColumns = (List) docParamsPojo.getParamsColumns();
        String paramsColumnsStr = JSON.toJSONString(paramsColumns);
        docParamsPojo.setParamsColumns(paramsColumnsStr);
        // 分组
        List paramsGroupIdList = docParamsPojo.getParamsGroupIdList();
        String paramsGroupId = JSON.toJSONString(paramsGroupIdList);
        docParamsPojo.setParamsGroupId(paramsGroupId);
        docParamsPojo.setParamsGroupIdList(null);
        // 如果类型不为金额或数值 单位设置为空
        if (!docParamsPojo.getParamsTypeId().equals("80") && !docParamsPojo.getParamsTypeId().equals("90")) {
            docParamsPojo.setUnit(null);
        }

        // 对比参数库中的参数名字是否有变化 如果有变化或者查不到(说明已经重给过paramuuid了)就重新给一个paramuuid
        DocParamsPojo paramInfoDao = fileOperationMapper.getParamInfoDao(paramsUuid);
        if (paramInfoDao == null) {// 说明已经是变化过的,已经找不到参数库的数据了,不需要重新给id
        } else if (!paramInfoDao.getParamsName().equals(paramsName) || !paramInfoDao.getParamsTypeId().equals(docParamsPojo.getParamsTypeId())) {// 说明第一次变化,重新给一个id
            docParamsPojo.setNewParamsUuid(UUID.randomUUID().toString().replaceAll("-", ""));
        }


        // 设置double格式
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setGroupingUsed(false);  // 不使用科学计数法
        nf.setMaximumFractionDigits(13);  // 允许的最大小数位数
        // 确认参数类型
//        10  单选
//        20  多选
//        30  日期/时间
//        40  文本
//        50  富文本
//        60  图片
//        70  资源附件
//        80  金额
//        90  数值
        String paramsTypeId = docParamsPojo.getParamsTypeId();  // 参数类型
//        String paramsRange = "";  // 所有选项及答案
        String paramsChoose = "";  // 选项对应的内容
        String newParamsText = ""; // 更新到content中的值
//        List<Map<String, String>> paramsColumns = null; // 矩阵参数表头
        String matrixDisplay = ""; // 矩阵参数显示效果
        List<Map<String, String>> matrixDisplayList = new ArrayList<>();  // 暂时装载从feContentText解析出来的uuid对应的display

        // 第一步 更新参数表
        try {
            if (paramsTypeId.equals("40") || paramsTypeId.equals("30")) {  // 文本 时间
                // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段

            } else if (paramsTypeId.equals("10")) {  // 单选

            } else if (paramsTypeId.equals("20")) {  // 多选
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("80")) {  // 金额
                if (docParamsPojo.getDefaultValue() != null) {
                    Map<String, Object> defaultValue = (Map<String, Object>) (Map) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("90")) {  // 数值
                if (docParamsPojo.getDefaultValue() != null) {
                    Map<String, Object> defaultValue = (Map<String, Object>) (Map) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("70")) {  // 附件
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("60")) {  // 图片
                if (docParamsPojo.getDefaultValue() != null) {
                    List<String> defaultValue = (List<String>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            } else if (paramsTypeId.equals("50")) {  // 富文本

            } else if (paramsTypeId.equals("95")) { // 矩阵
                if (docParamsPojo.getDefaultValue() != null) {
                    List<Map<String, String>> defaultValue = (List<Map<String, String>>) (List) docParamsPojo.getDefaultValue();
                    docParamsPojo.setDefaultValue(JSON.toJSONString(defaultValue));
                }
            }
        } catch (Exception e) {

        }

        try {
            fileOperationMapper.updateContentParamsNameDao(docParamsPojo);
        } catch (DataIntegrityViolationException e) {
            throw new WorkTableException("参数名称过长,请小于64个字符");
        }


        // 更新hf_model_content的contentText

        for (DocParamsPojo paramsPojo : contentByParamDao1) {
            String contentText = paramsPojo.getContentText();

            Document doc = Jsoup.parse(contentText, Parser.xmlParser());
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            Elements elements = doc.select("parameter[key=" + paramsUuid + "]");
            Elements elementsBlock = doc.select("parameterblock[key=" + paramsUuid + "]");
            elements.addAll(elementsBlock);
            for (Element element : elements) {
                // 利用前面的uuidList判断是否有show_text,有的话不改placeholder的数据
                if (!uuidList.contains(element.attributes().get("uuid"))) {
                    String placeholder = element.attributes().get("placeholder");  // 参数名
                    String key = element.attributes().get("key");  // 参数id
                    String alias1 = element.attributes().get("alias");  // 参数别名
                    String isunderline1 = element.attributes().get("isunderline");  // 参数是否有下划线
                    String styleid1 = element.attributes().get("styleid");  // 参数显示格式
                    String unit1 = element.attributes().get("unit");  // 参数单位
                    String s = String.valueOf(element);
//                    element.attr("unit", unit == null ? docParamsPojo.getParamsTypeId().equals("90") ? "" : unit1 : docParamsPojo.getParamsTypeId().equals("90") ? "" : unit);
                    element.attr("styleid", styleId == null ? styleid1 : styleId);
                    element.attr("isunderline", isUnderLine == null ? isunderline1 : isUnderLine);
                    element.attr("alias", showText == null ? alias1 : showText);
                    element.attr("placeholder", paramsName);
                    element.attr("key", docParamsPojo.getNewParamsUuid() == null ? key : docParamsPojo.getNewParamsUuid());
                    element.attr("unit", unit == null ? unit1 : unit);
                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
                } else {
//                    String placeholder = element.attributes().get("placeholder");  // 参数名
                    String key = element.attributes().get("key");  // 参数id
                    String alias1 = element.attributes().get("alias");  // 参数别名
                    String isunderline1 = element.attributes().get("isunderline");  // 参数是否有下划线
                    String styleid1 = element.attributes().get("styleid");  // 参数显示格式
                    String unit1 = element.attributes().get("unit");  // 参数单位
                    String s = String.valueOf(element);
//                    element.attr("unit", unit == null ? docParamsPojo.getParamsTypeId().equals("90") ? "" : unit1 : docParamsPojo.getParamsTypeId().equals("90") ? "" : unit);
                    element.attr("styleid", styleId == null ? styleid1 : styleId);
                    element.attr("isunderline", isUnderLine == null ? isunderline1 : isUnderLine);
                    element.attr("alias", showText == null ? alias1 : showText);
//                    element.attr("placeholder", paramsName);
                    element.attr("key", docParamsPojo.getNewParamsUuid() == null ? key : docParamsPojo.getNewParamsUuid());
                    element.attr("unit", unit == null ? unit1 : unit);
                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
                }
            }
            ContentPojo contentPojo = new ContentPojo();
            contentPojo.setContentText(contentText);
            contentPojo.setFileUuid(fileUuid);
            contentPojo.setFileVersionId(fileVersionId);
            contentPojo.setOutlineId(paramsPojo.getOutlineId());


            // 搜索模式下
            String searchUuid = docParamsPojo.getSearchUuid();
            String userId = docParamsPojo.getUserId();
            String outlineId = paramsPojo.getOutlineId();
            if (searchUuid != null) {
                // 更新后入库前更新一下缓存
                if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                    redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                }

                doc = Jsoup.parse(contentText, Parser.xmlParser());
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                elements = doc.select("mark");
                for (Element element : elements) {
                    element.before(element.html());
                    element.remove();  // 去除mark标签
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                contentPojo.setContentText(contentText);
            }

            fileOperationMapper.updateContentDao(contentPojo);
        }
        return TResponseVo.success("更新完成");
    }

    /**
     * 编辑页参数清单-修改展示效果
     *
     * @param docParamsPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateContentParamShowService(DocParamsPojo docParamsPojo) throws Exception {
        String showText = docParamsPojo.getShowText();
        String isUnderLine = docParamsPojo.getIsUnderLine();
        String styleId = docParamsPojo.getStyleId();
        String unit = docParamsPojo.getUnit();
        String remark = docParamsPojo.getRemark();
        String uuid = docParamsPojo.getUuid();
        String paramsColumns = (String) docParamsPojo.getParamsColumns();
        String matrixDisplay = docParamsPojo.getMatrixDisplay();
        String matrixMode = docParamsPojo.getMatrixMode();
        // 金额类型参数根据styleId指定unit
        if ("632007".equals(styleId) || "632008".equals(styleId) || "632010".equals(styleId) || "632012".equals(styleId) || "632013".equals(styleId) || "632014".equals(styleId)) {
            unit = "770001";  // 元
        } else if ("632009".equals(styleId) || "632011".equals(styleId) || "6320071".equals(styleId)) {
            unit = "770002";  // 万元
        }
        String outlineId = docParamsPojo.getOutlineId();
        String fileUuid = docParamsPojo.getFileUuid();
        String fileVersionId = docParamsPojo.getFileVersionId();
        String all = docParamsPojo.getAll();  // 是否应用全文

        if (all != null && all.equals("true")) {
            // 获取本参数文内的所有位置
            DocParamsPojo contentParamInfoDao = fileOperationMapper.getContentParamInfoDao(uuid, fileUuid, fileVersionId);
            String paramsUuid = contentParamInfoDao.getParamsUuid();
            List<String> allOutlineIdList = fileOperationMapper.getAllOutlineIdByParamDao(paramsUuid, fileUuid, fileVersionId);
            String allOutlineIdListStr = JsonKeyUtils.listToString(allOutlineIdList, "','");
            List<ContentPojo> contentByIdDao = fileOperationMapper.getContentByIdDao(allOutlineIdListStr, fileUuid, fileVersionId);
            if (StringUtils.isBlank(showText)) {
                // 如果显示内容是空 所有内容都默认展示参数名 修改参数表
                fileOperationMapper.updateAllContentParamShowDao(null, styleId, isUnderLine, unit, remark, paramsUuid, fileUuid, fileVersionId, paramsColumns, matrixDisplay, matrixMode);
                String contentParamName = contentParamInfoDao.getParamsName();
                // 修改content
                for (ContentPojo contentDao : contentByIdDao) {
                    String contentText = contentDao.getContentText();

                    Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                    doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                    Elements elements = doc.select("parameter[key=" + paramsUuid + "]");
                    Elements elementsBlock = doc.select("parameterblock[key=" + paramsUuid + "]");
                    elements.addAll(elementsBlock);
                    for (Element element : elements) {
                        String alias1 = element.attributes().get("alias");  // 参数别名
                        String isunderline1 = element.attributes().get("isunderline");  // 参数是否有下划线
                        String styleid1 = element.attributes().get("styleid");  // 参数显示格式
                        String unit1 = element.attributes().get("unit");  // 参数单位
                        String typeid1 = element.attributes().get("typeid");  // 参数单位
                        if ("95".equals(typeid1)) {  // 不为空说明这里是矩阵参数
                            if (!StringUtils.isBlank(matrixDisplay)) {
                                String text = element.html();
                                element.html(text);
                            }
                        }
                        // 修改contentText
//                        element.attr("unit", unit == null ? docParamsPojo.getParamsTypeId().equals("90") ? "" : unit1 : docParamsPojo.getParamsTypeId().equals("90") ? "" : unit);
                        element.attr("styleid", styleId == null ? styleid1 : styleId);
                        element.attr("isunderline", isUnderLine == null ? isunderline1 : isUnderLine);
                        element.attr("alias", showText == null ? alias1 : showText);
                        element.attr("placeholder", contentParamName);
                        element.attr("unit", unit == null ? unit1 : unit);
//                        contentText = contentText.replaceAll(String.valueOf(element), String.valueOf(element).replaceAll(element.attributes().get("placeholder"), contentParamName));
                    }
                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
                    contentDao.setContentText(contentText);
//
//
                    // 搜索模式下
                    String searchUuid = docParamsPojo.getSearchUuid();
                    String userId = docParamsPojo.getUserId();
                    String outlineId1 = contentDao.getOutlineId();
                    if (searchUuid != null) {
                        // 更新后入库前更新一下缓存
                        if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId1)) {
                            redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId1, contentText, 1800);
                        }

                        doc = Jsoup.parse(contentText, Parser.xmlParser());
                        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                        elements = doc.select("mark");
                        for (Element element : elements) {
                            element.before(element.html());
                            element.remove();  // 去除mark标签
                        }
                        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                        contentText = contentText.replaceAll("</br>", "");
                        contentDao.setContentText(contentText);
                    }

                    fileOperationMapper.updateContentDao(contentDao);
                }
            } else {
                fileOperationMapper.updateAllContentParamShowDao(showText, styleId, isUnderLine, unit, remark, paramsUuid, fileUuid, fileVersionId, paramsColumns, matrixDisplay, matrixMode);
                // 修改content
                for (ContentPojo contentDao : contentByIdDao) {
                    String contentText = contentDao.getContentText();

                    Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                    doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                    Elements elements = doc.select("parameter[key=" + paramsUuid + "]");
                    Elements elementsBlock = doc.select("parameterblock[key=" + paramsUuid + "]");
                    elements.addAll(elementsBlock);
                    for (Element element : elements) {
                        String alias1 = element.attributes().get("alias");  // 参数别名
                        String isunderline1 = element.attributes().get("isunderline");  // 参数是否有下划线
                        String styleid1 = element.attributes().get("styleid");  // 参数显示格式
                        String unit1 = element.attributes().get("unit");  // 参数单位
                        String typeid1 = element.attributes().get("typeid");  // 参数单位
                        if ("95".equals(typeid1)) {  // 不为空说明这里是矩阵参数
                            if (!StringUtils.isBlank(matrixDisplay)) {
                                String text = element.html();
                                element.html(text);
                            }
                        }
                        // 修改contentText
//                        element.attr("unit", unit == null ? docParamsPojo.getParamsTypeId().equals("90") ? "" : unit1 : docParamsPojo.getParamsTypeId().equals("90") ? "" : unit);
                        element.attr("styleid", styleId == null ? styleid1 : styleId);
                        element.attr("isunderline", isUnderLine == null ? isunderline1 : isUnderLine);
                        element.attr("alias", showText == null ? alias1 : showText);
                        element.attr("placeholder", showText);
                        element.attr("unit", unit == null ? unit1 : unit);

//                        contentText = contentText.replaceAll(String.valueOf(element), String.valueOf(element).replaceAll(element.attributes().get("placeholder"), showText));
                    }
                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
                    contentDao.setContentText(contentText);
//
//
                    // 搜索模式下
                    String searchUuid = docParamsPojo.getSearchUuid();
                    String userId = docParamsPojo.getUserId();
                    String outlineId1 = contentDao.getOutlineId();
                    if (searchUuid != null) {
                        // 更新后入库前更新一下缓存
                        if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId1)) {
                            redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId1, contentText, 1800);
                        }

                        doc = Jsoup.parse(contentText, Parser.xmlParser());
                        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                        elements = doc.select("mark");
                        for (Element element : elements) {
                            element.before(element.html());
                            element.remove();  // 去除mark标签
                        }
                        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                        contentText = contentText.replaceAll("</br>", "");
                        contentDao.setContentText(contentText);
                    }

                    fileOperationMapper.updateContentDao(contentDao);
                }
            }
            return TResponseVo.success("更新显示效果完成");
        } else {  // 不应用到全文
            // 如果置空 则恢复paramName展示
            DocParamsPojo contentParamInfoDao = fileOperationMapper.getContentParamInfoDao(uuid, fileUuid, fileVersionId);
            if (StringUtils.isBlank(showText)) {
                fileOperationMapper.updateContentParamShowDao(null, styleId, isUnderLine, unit, remark, uuid, fileUuid, fileVersionId, paramsColumns, matrixDisplay, matrixMode);
                // 多一步恢复内容,由显示展示效果改为显示参数名
                String contentParamNameDao = contentParamInfoDao.getParamsName();
                outlineId = contentParamInfoDao.getOutlineId();
                ContentPojo contentDao = fileOperationMapper.getContentDao(fileUuid, fileVersionId, outlineId);
                String contentText = contentDao.getContentText();


                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                Elements elements = doc.select("parameter[uuid=" + uuid + "]");
                Elements elements2 = doc.select("parameterblock[uuid=" + uuid + "]");
                elements.addAll(elements2);
                // elements只会包含一个结果
                for (Element element : elements) {
                    String alias1 = element.attributes().get("alias");  // 参数别名
                    String isunderline1 = element.attributes().get("isunderline");  // 参数是否有下划线
                    String styleid1 = element.attributes().get("styleid");  // 参数显示格式
                    String unit1 = element.attributes().get("unit");  // 参数单位
                    String typeid1 = element.attributes().get("typeid");  // 参数单位
                    if ("95".equals(typeid1)) {  // 矩阵参数多处理一步
                        if (!StringUtils.isBlank(matrixDisplay)) {
                            element.html(matrixDisplay);
                        }
                    }
                    // 修改contentText
                    element.attr("styleid", styleId == null ? styleid1 : styleId);
                    element.attr("isunderline", isUnderLine == null ? isunderline1 : isUnderLine);
                    element.attr("alias", showText == null ? alias1 : showText);
                    element.attr("placeholder", contentParamNameDao);
                    element.attr("unit", unit == null ? unit1 : unit);

//                    contentText = contentText.replaceAll(String.valueOf(element), String.valueOf(element).replaceAll(element.attributes().get("placeholder"), contentParamNameDao));
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                contentDao.setContentText(contentText);

                // 搜索模式下
                String searchUuid = docParamsPojo.getSearchUuid();
                String userId = docParamsPojo.getUserId();
                String outlineId1 = contentDao.getOutlineId();
                if (searchUuid != null) {
                    // 更新后入库前更新一下缓存
                    if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId1)) {
                        redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId1, contentText, 1800);
                    }

//                    doc = Jsoup.parse(contentText);
//                    elements = doc.select("mark");
//                    for (Element element : elements) {
//                        element.before(element.html());
//                        element.remove();  // 去除mark标签
//                    }
//                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
//                    contentDao.setContentText(contentText);
                }

                fileOperationMapper.updateContentDao(contentDao);
                return TResponseVo.success("更新显示效果完成");
            } else {
                fileOperationMapper.updateContentParamShowDao(showText, styleId, isUnderLine, unit, remark, uuid, fileUuid, fileVersionId, paramsColumns, matrixDisplay, matrixMode);
                outlineId = contentParamInfoDao.getOutlineId();
                ContentPojo contentDao = fileOperationMapper.getContentDao(fileUuid, fileVersionId, outlineId);
                String contentText = contentDao.getContentText();

                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                Elements elements = doc.select("parameter[uuid=" + uuid + "]");
                Elements elements2 = doc.select("parameterblock[uuid=" + uuid + "]");
                elements.addAll(elements2);
                // elements只会包含一个结果
                for (Element element : elements) {
                    String alias1 = element.attributes().get("alias");  // 参数别名
                    String isunderline1 = element.attributes().get("isunderline");  // 参数是否有下划线
                    String styleid1 = element.attributes().get("styleid");  // 参数显示格式
                    String unit1 = element.attributes().get("unit");  // 参数单位
                    String typeid1 = element.attributes().get("typeid");  // 参数单位
                    if ("95".equals(typeid1)) {  // 不为空说明这里是矩阵参数
                        if (!StringUtils.isBlank(matrixDisplay)) {
                            String text = element.html();
                            element.html(text);
                        }
                    }
                    // 修改contentText
//                    element.attr("unit", unit == null ? docParamsPojo.getParamsTypeId().equals("90") ? "" : unit1 : docParamsPojo.getParamsTypeId().equals("90") ? "" : unit);
                    element.attr("styleid", styleId == null ? styleid1 : styleId);
                    element.attr("isunderline", isUnderLine == null ? isunderline1 : isUnderLine);
                    element.attr("alias", showText == null ? alias1 : showText);
                    element.attr("placeholder", showText);
                    element.attr("unit", unit == null ? unit1 : unit);

//                    contentText = contentText.replaceAll(String.valueOf(element), String.valueOf(element).replaceAll(element.attributes().get("placeholder"), showText));
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                contentDao.setContentText(contentText);
//
//
                // 搜索模式下
                String searchUuid = docParamsPojo.getSearchUuid();
                String userId = docParamsPojo.getUserId();
                String outlineId1 = contentDao.getOutlineId();
                if (searchUuid != null) {
                    // 更新后入库前更新一下缓存
                    if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId1)) {
                        redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId1, contentText, 1800);
                    }

//                    doc = Jsoup.parse(contentText);
//                    elements = doc.select("mark");
//                    for (Element element : elements) {
//                        element.before(element.html());
//                        element.remove();  // 去除mark标签
//                    }
//                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
//                    contentDao.setContentText(contentText);
                }

                fileOperationMapper.updateContentDao(contentDao);
                return TResponseVo.success("更新显示效果完成");
            }
        }
    }


    /**
     * 编辑页参数清单-修改展示效果
     *
     * @param docParamsForm
     */
    @Override
    public TResponseVo updateContentParamPromptService(DocParamsForm docParamsForm) throws Exception {
        if (StringUtils.isBlank(docParamsForm.getFileUuid()) || StringUtils.isBlank(docParamsForm.getFileVersionId()) || StringUtils.isBlank(docParamsForm.getParamsUuid()) || docParamsForm.getRemark() == null || StringUtils.isBlank(String.valueOf(docParamsForm.getRemark()))) {
            TResponseVo.error("必填参数为空");
        }
        DocParamsPojo docParamsPojo = new DocParamsPojo();
        docParamsPojo.setFileUuid(docParamsForm.getFileUuid());
        docParamsPojo.setFileVersionId(docParamsForm.getFileVersionId());
        docParamsPojo.setParamsUuid(docParamsForm.getParamsUuid());
        docParamsPojo.setRemark(JSON.toJSONString(docParamsForm.getRemark()));
        fileOperationMapper.updateContentParamPromptDao(docParamsPojo);
        return TResponseVo.success("更新提示词完成");
    }

    /**
     * 编辑页参数清单-新增和删除
     *
     * @param docParamsPojo
     */
    @Override
    public TResponseVo addAndDelContentParamService(DocParamsPojo docParamsPojo) throws Exception {
        List<DocParamsTypeStylePojo> paramTypeStyleDi2Dao = fileOperationMapper.getParamTypeStyleDi2Dao();
        String fileUuid = docParamsPojo.getFileUuid();
        String fileVersionId = docParamsPojo.getFileVersionId();
        String outlineId = docParamsPojo.getOutlineId();
//        String contentText = docParamsPojo.getContentText();
        List<Map<String, String>> actions = docParamsPojo.getActions();
        List<Map<String, String>> actionsAdd = new ArrayList<>();
        List<Map<String, String>> actionsDel = new ArrayList<>();
        // 增删区分到两个栈里
        for (Map<String, String> action : actions) {
            if (action.get("action").equals("insert")) {
                // 新增栈
                actionsAdd.add(action);
            } else if (action.get("action").equals("delete")) {
                //删除栈
                actionsDel.add(action);
            }
        }
        // 成对出现的uuid出栈
        List<Map<String, String>> actionsAdd1 = new ArrayList<>();
        actionsAdd1.addAll(actionsAdd);
        List<Map<String, String>> actionsDel1 = new ArrayList<>();
        actionsDel1.addAll(actionsDel);
        for (Map<String, String> add : actionsAdd) {
            for (Map<String, String> del : actionsDel) {
                if (add.get("uuid").equals(del.get("uuid"))) {
                    actionsAdd1.remove(add);
                    actionsDel1.remove(del);
                }
            }
        }
        // 获取已经被删除的本文参数
        List<String> deledParamDao = fileOperationMapper.getDeledParamDao(fileUuid, fileVersionId);
        for (Map<String, String> stringStringMap : actionsAdd1) {
            if (deledParamDao.contains(stringStringMap.get("uuid"))) {
                // 恢复
                // 判断是否是刚才删除的uuid 如果是就直接拿回来用
                fileOperationMapper.updateContentParamOutlineIdDao(stringStringMap.get("uuid"), outlineId, fileUuid, fileVersionId);
                continue;
            }
            // 新建
            String uuid = stringStringMap.get("uuid");
            String paramsUuid = stringStringMap.get("paramUuid");
            String showText = stringStringMap.get("showText");
            String styleId = stringStringMap.get("styleId");
            String remark = stringStringMap.get("remark") == null ? "" : stringStringMap.get("remark");
            String unit = null;
            if (styleId != null) {
                for (DocParamsTypeStylePojo docParamsTypeStylePojo : paramTypeStyleDi2Dao) {
                    if (styleId.equals(docParamsTypeStylePojo.getStyleId())) {
                        unit = docParamsTypeStylePojo.getUnitId();
                    }
                }
            }
            String isUnderLine = stringStringMap.get("isUnderLine");
            fileOperationMapper.addContentParamDao(uuid, paramsUuid, fileUuid, fileVersionId, outlineId, new Date(), showText, styleId, remark, unit, isUnderLine);
        }

        // 删除刷库
        for (Map<String, String> stringStringMap : actionsDel1) {
            String uuid = stringStringMap.get("uuid");
            fileOperationMapper.delContentParamDao(uuid, fileUuid, fileVersionId);
        }
        // 内容刷库
//        ContentPojo contentPojo = new ContentPojo();
//        contentPojo.setOutlineId(outlineId);
//        contentPojo.setFileUuid(fileUuid);
//        contentPojo.setFileVersionId(fileVersionId);
//        contentPojo.setContentText(contentText);

        // 搜索模式下
//        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//        Elements elements = doc.select("mark");
//        if (elements.size() != 0) {
//            // 说明是在搜索
//            String searchUuid = docParamsPojo.getSearchUuid();
//            String userId = docParamsPojo.getUserId();
//            if (searchUuid != null) {
//                // 更新后入库前更新一下缓存
//                if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
//                    redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
//                }
////                doc = Jsoup.parse(contentText);
////                elements = doc.select("mark");
////                for (Element element : elements) {
////                    element.before(element.html());
////                    element.remove();  // 去除mark标签
////                }
////                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
////                contentText = contentText.replaceAll("</br>", "");
////                contentPojo.setContentText(contentText);
//            }
//        }
//        fileOperationMapper.updateContentDao(contentPojo);
        return TResponseVo.success("操作完成");
    }

    /**
     * 编辑页搜索-搜索列表
     *
     * @param docFileIndexPojo
     */
    @Override
    public TResponseVo searchService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        String searchLike = docFileIndexPojo.getSearchLike().replaceAll("<", "")
                .replaceAll(">", "")
                .replaceAll(" ", "")
                .replaceAll("\\.", "")
                .replaceAll("\\?", "")
                .replaceAll("\\(", "")
                .replaceAll("\\)", "")
                .replaceAll("\\+", "")
                .replaceAll("\\*", "")
                .replaceAll("\\\\", "")
                .replaceAll("!", "");
        String searchUserId = docFileIndexPojo.getSearchUserId();
        String searchUuid = docFileIndexPojo.getSearchUuid();  // 肯定不为空了
        if (searchLike == null || searchLike.equals("")) {
            String[] strings = redisUtils.dimSearch("search" + searchUserId + fileUuid + fileVersionId + searchUuid + "*");
            redisUtils.del(strings);
            HashMap<String, Object> ret = new HashMap<>();
            ret.put("searchUuid", null);
            ret.put("retList", null);
            return TResponseVo.success(ret);
        }
        List<OutLinePojo> contentAllDao = fileOperationMapper.getContentAllDao1(fileUuid, fileVersionId);
        List<Map<String, String>> retList = new ArrayList<>();
//        searchUuid = UUID.randomUUID().toString().replaceAll("-", "");
        for (OutLinePojo outLinePojo : contentAllDao) {
            if (outLinePojo.getContentText() == null) {
                continue;
            }
//            String s1 = new HtmlUtils().delHTMLTag(outLinePojo.getContentText());
//            outLinePojo.setOutlineTextDelHTMLTag(s1);
//            List<Integer> integers = new HtmlUtils().indexOfAll(s1, searchLike);
            String[] splits = StringEscapeUtils.unescapeHtml(outLinePojo.getContentText()).split("(?<=>[^<]{0,200})" + searchLike);
            if (splits.length == 1) {
                continue;
            }
            String newContentText = "";
            for (int i = 0; i < splits.length; i++) {
//                System.out.println("splits: "+splits[i]);
                String id = UUID.randomUUID().toString().replaceAll("-", "");
                newContentText = newContentText + splits[i].toString();
                if (i != splits.length - 1) {
                    newContentText = newContentText + "<mark id=\"" + id + "\">" + searchLike + "</mark>";
                    Map<String, String> retMap = new HashMap<>();
//                    System.out.println("splitPre: "+splits[i]);
                    String splitPre = new HtmlUtils().delHTMLTag(splits[i]);
//                    System.out.println("splitPre: "+splitPre);
                    String splitNext = new HtmlUtils().delHTMLTag(splits[i + 1]);
                    retMap.put("searchResult", splitPre.substring(splitPre.length() < 10 ? 0 : splitPre.length() - 10, splitPre.length()) + "<mark>" + searchLike + "</mark>" + splitNext.substring(0, splitNext.length() < 10 ? splitNext.length() : 10));
                    retMap.put("outlineId", outLinePojo.getOutlineId());
                    retMap.put("key", id);
                    retMap.put("searchUuid", searchUuid);
                    retList.add(retMap);
                }
            }
            redisUtils.set("search" + searchUserId + fileUuid + fileVersionId + searchUuid + outLinePojo.getOutlineId(), newContentText, 1800);
        }
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("searchUuid", searchUuid);
        ret.put("retList", retList);
        return TResponseVo.success(ret);
    }

    /**
     * 右侧看板-文件属性-查看属性
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo getFilePropertyService(String fileUuid) throws Exception {
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        String primaryFileUuid;
        if (docAllInfoDao.getMainFileUuid() != null) {
            primaryFileUuid = docAllInfoDao.getMainFileUuid();
        } else if (docAllInfoDao.getMainFileUuid2() != null) {
            primaryFileUuid = docAllInfoDao.getMainFileUuid2();
        } else {
            primaryFileUuid = docAllInfoDao.getFileUuid();
        }
        DocFileIndexPojo filePropertyDao = fileOperationMapper.getFilePropertyDao(primaryFileUuid);
        List<LabelValuePojo> fileTypeListDao = fileOperationMapper.getFileTypeListDao();
        String fileTypeId = filePropertyDao.getFileTypeId();
        for (LabelValuePojo labelValuePojo : fileTypeListDao) {
            if (labelValuePojo.getValue().equals(fileTypeId)) {
                filePropertyDao.setFileTypeName(labelValuePojo.getLabel());
                break;
            }
        }
        // 获取参与人名称
        String includeUserList = filePropertyDao.getIncludeUserList();
        if (StringUtils.isNotBlank(includeUserList)) {
            includeUserList = includeUserList.replaceAll(",", "','");
            List<String> userNameListDao = fileOperationMapper.getUserNameListDao(includeUserList);
            String userNameListStr = new JsonKeyUtils().listToString(userNameListDao);
            List<String> strings = new JsonKeyUtils().stringToList(includeUserList);
            filePropertyDao.setIncludeUserIdList(strings);
            filePropertyDao.setIncludeUserList(null);
            filePropertyDao.setIncludeUserListName(userNameListStr);
        }
        // 获取标签名称
        String fileLabelList = filePropertyDao.getFileLabelList();
        if (StringUtils.isNotBlank(fileLabelList)) {
            List<String> fileLabelIds = new JsonKeyUtils().stringToList(fileLabelList);
            filePropertyDao.setFileLabelIds(fileLabelIds);
            filePropertyDao.setFileLabelList(null);
            String fileLabelIdsStr = new JsonKeyUtils().listToString(fileLabelIds, "','");
            List<String> fileLabelNameListDao = fileOperationMapper.getFileLabelNameListDao(fileLabelIdsStr);
            String fileLabelName = new JsonKeyUtils().listToString(fileLabelNameListDao);
            filePropertyDao.setFileLabelName(fileLabelName);
        }
        return TResponseVo.success(filePropertyDao);
    }

    /**
     * 右侧看板-文件属性-获取参与人下拉列表
     *
     * @param userId
     */
    @Override
    public TResponseVo getFilePropertyUserListService(String userId) throws Exception {
        List<LabelValuePojo> filePropertyUserListDao = fileOperationMapper.getFilePropertyUserListDao(userId);
        return TResponseVo.success(filePropertyUserListDao);
    }

    /**
     * 右侧看板-文件属性-获取文件类型下拉列表
     */
    @Override
    public TResponseVo getFileTypeListService() throws Exception {
        List<LabelValuePojo> fileTypeListDao = fileOperationMapper.getFileTypeListDao();
        return TResponseVo.success(fileTypeListDao);
    }

    /**
     * 右侧看板-文件属性-获取适用范围下拉列表
     */
    @Override
    public TResponseVo getFileUseRangeListService() throws Exception {
        List<LabelValuePojo> fileUseRangeListDao = fileOperationMapper.getFileUseRangeListDao();
        return TResponseVo.success(fileUseRangeListDao);
    }

    /**
     * 右侧看板-文件属性-修改属性
     *
     * @param docFileIndexPojo
     */
    @Override
    public TResponseVo updateFilePropertyService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        // 判断当前文件是否是主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        String primaryFileUuid;
        if (docAllInfoDao.getMainFileUuid() != null) {
            primaryFileUuid = docAllInfoDao.getMainFileUuid();
        } else if (docAllInfoDao.getMainFileUuid2() != null) {
            primaryFileUuid = docAllInfoDao.getMainFileUuid2();
        } else {
            primaryFileUuid = docAllInfoDao.getFileUuid();
        }
        // 改主文件的属性
        docFileIndexPojo.setFileUuid(primaryFileUuid);
        // 标签list转str入库
        List<String> fileLabelIds = docFileIndexPojo.getFileLabelIds();
        docFileIndexPojo.setFileLabelList(new JsonKeyUtils().listToString(fileLabelIds));
        docFileIndexPojo.setFileLabelIds(null);
        // 文件类型
//        String fileTypeId = docFileIndexPojo.getFileTypeId();
//        docFileIndexPojo.setFileTypeId(fileTypeId == null ? null : fileTypeId);

        // 使用范围
        String fileUseRangeId = docFileIndexPojo.getFileUseRangeId();
        String fileUseRangeName = "";
        if (fileUseRangeId == null) {
            fileUseRangeName = "场景未知"; // 未知
        } else if (fileUseRangeId.equals("GR")) {
            fileUseRangeName = "个人适用";
        } else if (fileUseRangeId.equals("GS")) {
            fileUseRangeName = "公司适用";
        } else if (fileUseRangeId.equals("PT")) {
            fileUseRangeName = "平台适用";
        } else {
            fileUseRangeName = "场景未知"; // 未知
        }
        docFileIndexPojo.setFileUseRangeId(fileUseRangeId == null ? null : fileUseRangeId);
        docFileIndexPojo.setFileUseRangeName(fileUseRangeName);
        fileOperationMapper.updateFilePropertyDao(docFileIndexPojo);
        return TResponseVo.success("更新完成");
    }

    /**
     * 获取附属文件类型维表
     *
     * @param templateTypeId
     */
    @Override
    public TResponseVo getTemplateTypeDiService(String templateTypeId) throws Exception {
        List<LabelValuePojo> templateTypeDiDao = fileOperationMapper.getTemplateTypeDiDao(templateTypeId);
        return TResponseVo.success(templateTypeDiDao);
    }

    /**
     * 模板组-新增附属文件
     *
     * @param docFileIndexPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newSubsidiaryFileService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        String fileTypeId = docAllInfoDao.getFileTypeId();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        Integer subsidiaryCntDao = fileOperationMapper.getSubsidiaryCntDao(fileUuid, fileVersionId);
        if (subsidiaryCntDao > 5) {
            return TResponseVo.error(ResponseEnum.ERROR, "附属文件不能超过5个");
        }
        docFileIndexPojo.setMainFileUuid(fileUuid);
        docFileIndexPojo.setOldFileVersionId(fileVersionId);
        docFileIndexPojo.setMainFileVersionId(fileVersionId);
        String fileName = docFileIndexPojo.getFileName();
        String userId = docFileIndexPojo.getCreateUserId();
        if (StringUtils.isBlank(fileName)) {
            return TResponseVo.error(ResponseEnum.ERROR, "文件名称不能为空");
        }
        fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
        docFileIndexPojo.setFileUuid(fileUuid);
        docFileIndexPojo.setFileVersionId(fileVersionId);

        // 获取附属文件保留了哪些段落 后面复制参数的时候只保留这些段落内的参数
        List<String> outlineListMap = docFileIndexPojo.getOutlineListMap();
        String s = JsonKeyUtils.listToString(outlineListMap, "','");
        docFileIndexPojo.setOutlineIdListStr(s);

        docFileIndexPojo.setCreateTime(new Date());
        try {
            int j = 0;
            int i = 0;
            // 创建索引
            i = fileOperationMapper.newSubsidiaryFileIndexDao(docFileIndexPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(700);
                throw fileIndexException;
            }

            // 创建版本
            i = fileOperationMapper.newSubsidiaryFileVersionDao(docFileIndexPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(701);
                throw fileIndexException;
            }

            // 创建大纲
            List<OutLinePojo> newSubsidiaryFileOutlineDao = fileOperationMapper.getNewSubsidiaryFileOutlineDao(docFileIndexPojo);
            String sql = "";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = formatter.format(new Date());
            for (OutLinePojo outLinePojo : newSubsidiaryFileOutlineDao) {
                j++;
                // 重新排序
                outLinePojo.setOutlineOrder(String.valueOf(j));
                String outlineReplaceGroupId = null;
                String color = null;
                if (outLinePojo.getOutlineReplaceGroupId() != null) {
                    outlineReplaceGroupId = "\"" + outLinePojo.getOutlineReplaceGroupId() + "\"";
                }
                if (outLinePojo.getColor() != null) {
                    color = "\"" + outLinePojo.getColor() + "\"";
                }
                sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + outLinePojo.getIsNecessary() + "\",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
                fileOperationMapper.newSubsidiaryFileOutlineDao(sql);
            }
            // 创建内容
            List<ContentPojo> subsidiaryFileContentDao = fileOperationMapper.getSubsidiaryFileContentDao(docFileIndexPojo);
            sql = "";
            for (ContentPojo contentPojo : subsidiaryFileContentDao) {
                String contentText = contentPojo.getContentText();
                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                Elements elements = doc.select("bookmark");
                Elements elementsTmp = new Elements();
                elementsTmp.addAll(elements);
                for (Element element : elementsTmp) {
                    element.remove();  // 去除bookmark标签
                }

                elements = doc.select("mark");
                elementsTmp = new Elements();
                elementsTmp.addAll(elements);
                for (Element element : elementsTmp) {
                    element.before(element.html());
                    element.remove();  // 去除mark标签
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                contentPojo.setContentText(contentText);
                sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
                fileOperationMapper.newSubsidiaryFileContentDao(sql);
            }
            // 创建参数
            fileOperationMapper.newSubsidiaryContentParamDao(docFileIndexPojo);

            // 创建标注
            fileOperationMapper.newSubsidiaryContentTagDao(docFileIndexPojo);

            // 创建数据管理(4张)
//            fileOperationMapper.newSubsidiaryContentDmDbDao(docFileIndexPojo);
//            fileOperationMapper.newSubsidiaryContentDmTableDao(docFileIndexPojo);
//            fileOperationMapper.newSubsidiaryContentDmColumnsDao(docFileIndexPojo);
//            splitTableMapper.newSubsidiaryContentDmDataDao(docFileIndexPojo);

            // 创建数据表(如果有)
            fileOperationMapper.newSubsidiaryContentDbDao(docFileIndexPojo);


        } catch (FileIndexException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(704);
            throw fileIndexException;
        }

        Map<String, String> ret = new HashMap<>();
        ret.put("fileUuid", fileUuid);
        ret.put("fileVersionId", fileVersionId);
        ret.put("fileTypeId", fileTypeId);
        ret.put("info", "新建成功");
        return TResponseVo.success(ret);
    }


    /**
     * 模板组-上传附属文件
     *
     * @param docFileIndexPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo uploadSubsidiaryFileService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        String mainFileUuid = docFileIndexPojo.getMainFileUuid();
        DocFileVerIndexPojo draftVersionIdDao = fileOperationMapper.getDraftVersionIdDao(mainFileUuid);
        String fileVersionId = draftVersionIdDao.getFileVersionId();
        docFileIndexPojo.setMainFileVersionId(fileVersionId);
        Integer i = 0;
        DocFileVerIndexPojo versions = docFileIndexPojo.getVersions();
        try {
            i = fileIndexMapper.insertFileIndexDao(docFileIndexPojo);
            if (i != 1) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(710);
                // TODO 报错未处理
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "AddNewFileService", docFileIndexPojo.getFileName(), "主文件索引表新增失败");
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(711);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "AddNewFileService", docFileIndexPojo.getFileName(), "主文件索引表新增SQL错误");
            throw fileIndexException;
        }
        try {
            versions.setVersionStatus("正式版本");
            i = fileIndexMapper.insertFileVersionDao(versions);
            if (i != 1) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(712);
                // TODO 报错未处理
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "AddNewFileService", docFileIndexPojo.getFileName(), "主文件版本索引表新增失败");
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(713);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "AddNewFileService", docFileIndexPojo.getFileName(), "主文件版本索引表新增SQL错误");
            throw fileIndexException;
        }
        return TResponseVo.success("新增完成");
    }

    /**
     * 模板组-上传附属文件-生成大纲
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
     * 模板组-上传附属文件-生成大纲内容
     *
     * @param sql
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo insertContentService(String sql) throws Exception {
        return TResponseVo.success(fileIndexMapper.insertContentDao(sql));
    }

    /**
     * 模板组-删除附属文件
     *
     * @param fileUuid
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delSubsidiaryFileService(String fileUuid) throws Exception {
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() == null && docAllInfoDao.getMainFileUuid2() == null) {
            return TResponseVo.error(ResponseEnum.ERROR, "请勿在此删除主文件");
        } else {
            try {
                // 删除索引
                fileOperationMapper.delSubsidiaryFileIndexDao(fileUuid);
                // 删除版本
                fileOperationMapper.delSubsidiaryFileVersionDao(fileUuid);
                // 删除大纲
                fileOperationMapper.delSubsidiaryFileOutlineDao(fileUuid);
                // 删除内容
                fileOperationMapper.delSubsidiaryFileContentDao(fileUuid);
                // 删除参数
                fileOperationMapper.delSubsidiaryContentParamDao(fileUuid);
                // 删除标注
                fileOperationMapper.delSubsidiaryContentTagDao(fileUuid);
                // 删除书签
                fileOperationMapper.delSubsidiaryContentBookmarkDao(fileUuid);
            } catch (Exception e) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(720);
                throw fileIndexException;
            }
            return TResponseVo.success("删除成功");
        }
    }

    /**
     * 模板组-获取附属文件列表
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo getSubsidiaryFileListService(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        String fileStatus = docAllInfoDao.getFileStatus();
        if ("3".equals(fileStatus)) {
            throw new WorkTableException("该文档已被删除，无法预览");
        }
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        }

        // 判断当前用户是否有权限
//        String includeUserList = docAllInfoDao.getIncludeUserList();
//        List<String> includeUserIdList = new JsonKeyUtils().stringToList(includeUserList);
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        String userId = user.getUserId();
//        if (includeUserIdList.contains(userId) || docAllInfoDao.getCreateUserId().equals(userId)) {
//            DocFileVerIndexPojo draftVersionIdDao = fileOperationMapper.getDraftVersionIdDao(fileUuid);
//            if (draftVersionIdDao == null) {
//                fileVersionId = docAllInfoDao.getFileVersionId();
//            } else {
//                fileVersionId = draftVersionIdDao.getFileVersionId();
//            }
//        } else {
//            fileVersionId = docAllInfoDao.getFileVersionId();
//        }
        List<DocFileIndexPojo> subsidiaryFileList = fileOperationMapper.getSubsidiaryFileListDao(docAllInfoDao.getFileUuid(), fileVersionId);
        String primaryFileUuid = docAllInfoDao.getFileUuid();
        for (DocFileIndexPojo docFileIndexPojo : subsidiaryFileList) {
            if (docFileIndexPojo.getFileUuid().equals(primaryFileUuid)) {
                docFileIndexPojo.setPrimary(true);
                docFileIndexPojo.setFileVersionId(fileVersionId);
                break;
            }
        }
        return TResponseVo.success(subsidiaryFileList);
    }

    /**
     * 模板组-修改附属文件名
     *
     * @param fileUuid
     * @param fileName
     */
    @Override
    public TResponseVo updateSubsidiaryFileNameService(String fileUuid, String fileName, String templateTypeId) throws Exception {
        fileOperationMapper.updateSubsidiaryFileNameDao(fileUuid, fileName, templateTypeId);
        return TResponseVo.success("文件名更新完成");
    }

    /**
     * 词条库-新增词条
     *
     * @param docWordsPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo addWordsService(DocWordsPojo docWordsPojo) throws Exception {
        String wordsName = docWordsPojo.getWordsName();
        // 判断词条名重复
        try {
            Integer integer = fileOperationMapper.checkWordsNameDao(wordsName, null);
            if (integer >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", wordsName, "当前类错误码-801-词条名重复");
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(801);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", wordsName, "当前类错误码-802-检查词条名重复的SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(802);
            throw fileIndexException;
        }
        String wordsUuid = UUID.randomUUID().toString().replaceAll("-", "");
        docWordsPojo.setCreateTime(new Date());
        docWordsPojo.setWordsUuid(wordsUuid);
        try {
            fileOperationMapper.addWordsDao(docWordsPojo);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addWordsService", wordsName, "当前类错误码-804-新建词条SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(804);
            throw fileIndexException;
        }
        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "创建成功");
        ret.put("wordsUuid", wordsUuid);
        return TResponseVo.success(ret);
    }

    /**
     * 词条库-删除词条
     *
     * @param wordsUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delWordsService(String wordsUuid) throws Exception {
        try {
            fileOperationMapper.delWordsDao(wordsUuid);
            fileOperationMapper.delTagWordsDao(wordsUuid);
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "delWordsService", wordsUuid, "当前类错误码-805-删除词条SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(805);
            throw fileIndexException;
        }
        return TResponseVo.success("删除成功");
    }

    /**
     * 词条库-修改词条
     *
     * @param docWordsPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo updateWordsService(DocWordsPojo docWordsPojo) throws Exception {
        String wordsUuid = docWordsPojo.getWordsUuid();
        String wordsName = docWordsPojo.getWordsName();
        String wordsDesc = docWordsPojo.getWordsDesc();
        // 判断词条名重复
        try {
            Integer integer = fileOperationMapper.checkWordsNameDao(wordsName, wordsUuid);
            if (integer >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateWordsService", wordsName, "当前类错误码-806-词条名重复");
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(806);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateWordsService", wordsName, "当前类错误码-807-检查词条名重复的SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(807);
            throw fileIndexException;
        }
        fileOperationMapper.updateWordsDao(wordsUuid, wordsName, wordsDesc);
        return TResponseVo.success("更新成功");
    }

    /**
     * 词条库-查询词条信息
     *
     * @param wordsUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getWordsInfoService(String wordsUuid) throws Exception {
        DocWordsPojo wordsInfoDao = fileOperationMapper.getWordsInfoDao(wordsUuid);
        return TResponseVo.success(wordsInfoDao);
    }

    /**
     * 词条库-读取词条库列表
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getWordsListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        if ("0".equalsIgnoreCase(isPaged)) {
            List<DocWordsPojo> docWordsPojo = null;
            docWordsPojo = fileOperationMapper.getWordsListDao(paramNameLike, null);
            return TResponseVo.success(docWordsPojo);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<DocWordsPojo> docWordsPojos = fileOperationMapper.getWordsListDao(paramNameLike, null);
        PageInfo pageInfo = new PageInfo(docWordsPojos);
        return TResponseVo.success(pageInfo);
    }

    /**
     * 词条库-获取labelvalue
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getWordsLabelValueListService() throws Exception {
        List<LabelValuePojo> wordsLabelValueListDao = fileOperationMapper.getWordsLabelValueListDao();
        return TResponseVo.success(wordsLabelValueListDao);
    }


    //    /**
//     * 新增词条
//     *
//     * @param createUserId
//     * @param tipsName
//     * @param tipsDesc
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo addTipsService(String createUserId, String tipsName, String tipsDesc) throws Exception {
//        String tipsID = UUID.randomUUID().toString().replaceAll("-", "");
//        Date createTime = new Date();
//        return TResponseVo.success(fileOperationMapper.addTipsDao(createUserId, tipsName, tipsID, tipsDesc, createTime));
//    }
//
//    /**
//     * 删除词条
//     *
//     * @param tipsUuid
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo delTipsService(String tipsUuid) throws Exception {
//        return TResponseVo.success(fileOperationMapper.delTipsDao(tipsUuid));
//    }
//
//    /**
//     * 修改词条
//     *
//     * @param docTipsPojo
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo updateTipsService(DocTipsPojo docTipsPojo) throws Exception {
//        String tipsUuid = docTipsPojo.getTipsUuid();
//        String tipsName = docTipsPojo.getTipsName();
//        String tipsDesc = docTipsPojo.getTipsDesc();
//        return TResponseVo.success(fileOperationMapper.updateTipsDao(tipsUuid, tipsName, tipsDesc));
//    }
//
//    /**
//     * 读取词条库列表
//     *
//     * @param pageNum
//     * @param pageSize
//     * @param wordsNameLike
//     * @param isPaged
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo getTipsService(Integer pageNum, Integer pageSize, String wordsNameLike, String isPaged) throws Exception {
//        if (StringUtils.isNotBlank(wordsNameLike)) {
//            wordsNameLike = wordsNameLike.replaceAll("'", "");
//        }
//        if ("0".equalsIgnoreCase(isPaged)) {
//            List<DocTipsPojo> docTipsPojo = null;
//            docTipsPojo = fileOperationMapper.getTipsDao(wordsNameLike, 10);
//            return TResponseVo.success(docTipsPojo);
//        }
//        PageHelper.startPage(pageNum, pageSize);
//        List<DocTipsPojo> docTipsPojos = fileOperationMapper.getTipsDao(wordsNameLike, null);
//        PageInfo pageInfo = new PageInfo(docTipsPojos);
//        return TResponseVo.success(pageInfo);
//    }


//    /**
//     * 标注库-新增标注
//     *
//     * @param tagPojo
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo addTagService(TagPojo tagPojo) throws Exception {
//        String tagId = UUID.randomUUID().toString().replaceAll("-", "");
//        tagPojo.setTagId(tagId);
//        tagPojo.setCreateTime(new Date());
//        fileOperationMapper.addTagDao(tagPojo);
//        Map<String, String> ret = new HashMap<>();
//        ret.put("info", "创建成功");
//        ret.put("tagId", tagId);
//        return TResponseVo.success(ret);
//    }

    /**
     * 标注库-删除标注
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delTagService(TagPojo tagPojo) throws Exception {
        String tagId = tagPojo.getTagId();
        String fileUuid = tagPojo.getFileUuid();
        String fileVersionId = tagPojo.getFileVersionId();
        // 删除标注库数据
        fileOperationMapper.delTagDao(tagId, fileUuid, fileVersionId);
        // 删除文本内数据
        TagPojo tagInfoDao = fileOperationMapper.getTagInfoDao(tagId, fileUuid, fileVersionId);
        String outlineId = tagInfoDao.getOutlineId();
        ContentPojo contentDao = fileOperationMapper.getContentDao(fileUuid, fileVersionId, outlineId);
        String contentText = contentDao.getContentText();

//        org.dom4j.Document document = null;
//        try {
//            document = DocumentHelper.parseText(contentText);  // 必须被同一标签包裹
//        } catch (Exception e) {
//            document = DocumentHelper.parseText("<html>" + contentText.replaceAll("&nbsp;", "") + "</html>");  // 必须被同一标签包裹
//        }
//        // 获取节点元素对象与值
//        String xpath = "//annotation[@id='" + tagId + "']";
//        Node o = document.selectSingleNode(xpath);
//        System.out.println(o.asXML());
//        org.dom4j.Element element = (org.dom4j.Element) o;
//        String s = document.asXML().replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>\n", "");
//        String s1 = s.replaceAll(o.asXML().replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>\n", ""), o.getText());
//        org.dom4j.Document document1 = DocumentHelper.parseText(s1);
//        contentText = document1.asXML().replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>\n", "");
//        contentDao.setContentText(contentText);


        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
        Elements elements = doc.select("annotation[id=" + tagId + "]");
        for (Element element : elements) {
            element.before(element.html());
            element.remove();  // 去除annotation标签
        }
        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
        contentText = contentText.replaceAll("</br>", "");


        // 搜索模式下
        String searchUuid = tagPojo.getSearchUuid();
        String userId = tagPojo.getUserId();
        if (searchUuid != null) {
            // 更新后入库前更新一下缓存
            if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
            }

            doc = Jsoup.parse(contentText, Parser.xmlParser());
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            elements = doc.select("mark");
            for (Element element : elements) {
                element.before(element.html());
                element.remove();  // 去除mark标签
            }
            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
            contentText = contentText.replaceAll("</br>", "");
            contentDao.setContentText(contentText);
        }

        fileOperationMapper.updateContentDao(contentDao);
        return TResponseVo.success("删除成功");
    }

    /**
     * 标注库-增加和删除标注
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo addAndDelContentTagService(TagPojo tagPojo) throws Exception {
        String fileUuid = tagPojo.getFileUuid();
        String fileVersionId = tagPojo.getFileVersionId();
        String outlineId = tagPojo.getOutlineId();
//        String contentText = tagPojo.getContentText();
        String userId = tagPojo.getUserId();
        List<Map<String, String>> actions = tagPojo.getActions();
        List<Map<String, String>> actionsAdd = new ArrayList<>();
        List<Map<String, String>> actionsDel = new ArrayList<>();

        // 增删区分到两个栈里
        for (Map<String, String> action : actions) {
            if (action.get("action").equals("insert")) {
                // 新增栈
                actionsAdd.add(action);
            } else if (action.get("action").equals("delete")) {
                //删除栈
                actionsDel.add(action);
            }
        }
        // 成对出现的uuid出栈
        List<Map<String, String>> actionsAdd1 = new ArrayList<>();
        actionsAdd1.addAll(actionsAdd);
        List<Map<String, String>> actionsDel1 = new ArrayList<>();
        actionsDel1.addAll(actionsDel);
        for (Map<String, String> add : actionsAdd) {
            for (Map<String, String> del : actionsDel) {
                if (add.get("uuid").equals(del.get("uuid"))) {
                    actionsAdd1.remove(add);
                    actionsDel1.remove(del);
                }
            }
        }
        // 新增刷库
        // 获取已经被删除的本文标注
        List<String> deledTagDao = fileOperationMapper.getDeledTagDao(fileUuid, fileVersionId);
        List<String> aliveTagDao = fileOperationMapper.getAliveTagDao(fileUuid, fileVersionId);
        for (Map<String, String> stringStringMap : actionsAdd1) {
            if (deledTagDao.contains(stringStringMap.get("uuid"))) {
                // 恢复
                // 判断是否是刚才删除的uuid 如果是就直接拿回来用
                fileOperationMapper.updateContentTagOutlineIdDao(stringStringMap.get("uuid"), outlineId, fileUuid, fileVersionId);
                continue;
            }
            if (aliveTagDao.contains(stringStringMap.get("uuid"))) {
                // 如果是已存在的 则不用处理
                continue;
            }
            // 新建
            String uuid = stringStringMap.get("uuid");
            String tagContent = stringStringMap.get("tagContent");
            String lawId = stringStringMap.get("lawId");
            String wordsUuid = stringStringMap.get("wordsUuid");
            String bookmarkUuid = stringStringMap.get("bookmarkUuid");
            String typeId = stringStringMap.get("typeId");  // 1摘编 2词条 3释义 4引用 5标注
            String tagName = stringStringMap.get("tagName");
            TagPojo tag1 = new TagPojo();
            tag1.setTagId(uuid);
            tag1.setTagContent(tagContent);
            tag1.setFileUuid(fileUuid);
            tag1.setFileVersionId(fileVersionId);
            tag1.setOutlineId(outlineId);
            tag1.setLawId(lawId);
            tag1.setWordsUuid(wordsUuid);
            tag1.setBookmarkUuid(bookmarkUuid);
            tag1.setCreateTime(new Date());
            tag1.setCreateUserId(userId);
            tag1.setTypeId(typeId);
            tag1.setTagName(tagName);
            fileOperationMapper.addContentTagDao(tag1);
        }

        // 删除刷库
        for (Map<String, String> stringStringMap : actionsDel1) {
            String uuid = stringStringMap.get("uuid");
            fileOperationMapper.delContentTagDao(uuid, fileUuid, fileVersionId);
        }
        // 内容刷库
//        ContentPojo contentPojo = new ContentPojo();
//        contentPojo.setOutlineId(outlineId);
//        contentPojo.setFileUuid(fileUuid);
//        contentPojo.setFileVersionId(fileVersionId);
//        contentPojo.setContentText(contentText);
//
//        // 搜索模式下
//        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
//        Elements elements = doc.select("mark");
//        if (elements.size() != 0) {
//            // 说明是在搜索
//            String searchUuid = tagPojo.getSearchUuid();
//
//            if (searchUuid != null) {
//                // 更新后入库前更新一下缓存
//                System.out.println("searchAddTag : search" + userId + fileUuid + fileVersionId + searchUuid + outlineId);
//                if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
//                    redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
//                }
////                doc = Jsoup.parse(contentText);
////                elements = doc.select("mark");
////                for (Element element : elements) {
////                    element.before(element.html());
////                    element.remove();  // 去除mark标签
////                }
////                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                contentText = contentText.replaceAll("</br>", "");
////                contentPojo.setContentText(contentText);
//            }
//        }
//
//        fileOperationMapper.updateContentDao(contentPojo);

        return TResponseVo.success("操作完成");
    }


    /**
     * 标注库-修改标注
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo updateTagService(TagPojo tagPojo) throws Exception {
        String wordsUuid = tagPojo.getWordsUuid();
        String lawId = tagPojo.getLawId();
        String tagContent = tagPojo.getTagContent();
        String bookmarkUuid = tagPojo.getBookmarkUuid();
        String key = "";
        if (wordsUuid != null) {
            key = wordsUuid;
        } else if (lawId != null) {
            key = lawId;
        } else if (tagContent != null) {
            key = tagContent;
        } else if (bookmarkUuid != null) {
            key = bookmarkUuid;
        } else {
            // 可以报错
        }
        if (1 == 1) {
            // 修改标注库数据
            fileOperationMapper.updateTagDao(tagPojo);
            String tagId = tagPojo.getTagId();
            String fileUuid = tagPojo.getFileUuid();
            String fileVersionId = tagPojo.getFileVersionId();
            TagPojo tagInfoDao = fileOperationMapper.getTagInfoDao(tagId, fileUuid, fileVersionId);
            String outlineId = tagInfoDao.getOutlineId();
            // 修改内容数据
            ContentPojo contentDao = fileOperationMapper.getContentDao(fileUuid, fileVersionId, outlineId);
            String contentText = contentDao.getContentText();


            Document doc = Jsoup.parse(contentText, Parser.xmlParser());
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            Elements elements = doc.select("annotation[id=" + tagId + "]");
            // elements只会包含一个结果
            for (Element element : elements) {
                // 修改contentText
                System.out.println(element);
                element.attr("citiao", wordsUuid == null ? "" : wordsUuid);
                element.attr("yinyong", bookmarkUuid == null ? "" : bookmarkUuid);
                element.attr("zhaibian", lawId == null ? "" : lawId);
                element.attr("shiyi", tagContent == null ? "" : tagContent);
                element.attr("key", key);
                System.out.println(element);
            }
            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
            contentText = contentText.replaceAll("</br>", "");
            contentDao.setContentText(contentText);

            // 搜索模式下
            String searchUuid = tagPojo.getSearchUuid();
            String userId = tagPojo.getUserId();
            if (searchUuid != null) {
                // 更新后入库前更新一下缓存
                if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                    redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                }

                doc = Jsoup.parse(contentText, Parser.xmlParser());
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                elements = doc.select("mark");
                for (Element element : elements) {
                    element.before(element.html());
                    element.remove();  // 去除mark标签
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                contentDao.setContentText(contentText);
            }
            fileOperationMapper.updateContentDao(contentDao);
            return TResponseVo.success("更新成功");
        } else {
            String tagId = tagPojo.getTagId();
            String fileUuid = tagPojo.getFileUuid();
            String fileVersionId = tagPojo.getFileVersionId();
            TagPojo tagInfoDao = fileOperationMapper.getTagInfoDao(tagId, fileUuid, fileVersionId);
            String useIsDel = tagInfoDao.getUseIsDel();
            List<String> useIsDelOutlineIdList = new ArrayList<>();
            List<String> useIsDelTagIdList = new ArrayList<>();
            if (useIsDel.equals("1")) {
                // 说明当前在进行"复原标注"的操作 即对失效的引用或词条重新赋值
                // 此时要找出其他一起失效的数据 并一起赋值
                String typeId = tagInfoDao.getTypeId();
                if (typeId.equals("2")) { // 词条
                    String tmpUuid = tagInfoDao.getWordsUuid();
                    useIsDelOutlineIdList = fileOperationMapper.getUseIsDelOutlineIdListDao(tmpUuid, null, fileUuid, fileVersionId);
                    useIsDelTagIdList = fileOperationMapper.getUseIsDelTagIdListDao(tmpUuid, null, fileUuid, fileVersionId);
                    fileOperationMapper.updateUseIsDelDao(tmpUuid, null, fileUuid, fileVersionId, wordsUuid);
                } else if (typeId.equals("4")) { // 引用
                    String tmpUuid = tagInfoDao.getBookmarkUuid();
                    useIsDelOutlineIdList = fileOperationMapper.getUseIsDelOutlineIdListDao(null, tmpUuid, fileUuid, fileVersionId);
                    useIsDelTagIdList = fileOperationMapper.getUseIsDelTagIdListDao(null, tmpUuid, fileUuid, fileVersionId);
                    fileOperationMapper.updateUseIsDelDao(null, tmpUuid, fileUuid, fileVersionId, bookmarkUuid);
                } else {
                    // 暂无其他操作
                }
            } else {
                // 普通修改
                // 修改标注库数据
                fileOperationMapper.updateTagDao(tagPojo);
            }
            // 修改内容数据
            if (!useIsDel.equals("1")) {
                useIsDelOutlineIdList.add(tagInfoDao.getOutlineId());
                useIsDelTagIdList.add(tagId);
            }
            String useIsDelTagIdListStr = new JsonKeyUtils().listToString(useIsDelTagIdList, "|");
            for (String outlineId : useIsDelOutlineIdList) {
                ContentPojo contentDao = fileOperationMapper.getContentDao(fileUuid, fileVersionId, outlineId);
                String contentText = contentDao.getContentText();
                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                Elements elements = doc.select("annotation[id~=(" + useIsDelTagIdListStr + ")]");
                System.out.println("useIsDelTagIdListStr : " + useIsDelTagIdListStr);
                System.out.println(elements.size());
                // elements只会包含一个结果
                for (Element element : elements) {
                    // 修改contentText
                    System.out.println(element);
                    element.attr("citiao", wordsUuid == null ? "" : wordsUuid);
                    element.attr("yinyong", bookmarkUuid == null ? "" : bookmarkUuid);
                    element.attr("zhaibian", lawId == null ? "" : lawId);
                    element.attr("shiyi", tagContent == null ? "" : tagContent);
                    element.attr("key", key);
                    System.out.println(element);
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                contentDao.setContentText(contentText);

                // 搜索模式下
                String searchUuid = tagPojo.getSearchUuid();
                String userId = tagPojo.getUserId();
                if (searchUuid != null) {
                    // 更新后入库前更新一下缓存
                    if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                        redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                    }

//                    doc = Jsoup.parse(contentText);
//                    elements = doc.select("mark");
//                    for (Element element : elements) {
//                        element.before(element.html());
//                        element.remove();  // 去除mark标签
//                    }
//                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
//                    contentDao.setContentText(contentText);
                }

                fileOperationMapper.updateContentDao(contentDao);
            }
            return TResponseVo.success("更新成功");
        }
    }

    /**
     * 标注库-获取标注信息
     *
     * @param tagId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getTagInfoService(String tagId, String fileUuid, String fileVersionId) throws Exception {
        TagPojo tagInfoDao = fileOperationMapper.getTagInfoDao(tagId, fileUuid, fileVersionId);
        return TResponseVo.success(tagInfoDao);
    }

    /**
     * 标注库-获取标注列表
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getTagListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        if ("0".equalsIgnoreCase(isPaged)) {
            List<TagPojo> docWordsPojo = null;
            docWordsPojo = fileOperationMapper.getTagListDao(paramNameLike, null);
            return TResponseVo.success(docWordsPojo);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<TagPojo> docWordsPojos = fileOperationMapper.getTagListDao(paramNameLike, null);
        PageInfo pageInfo = new PageInfo(docWordsPojos);
        return TResponseVo.success(pageInfo);
    }


    /**
     * 标注库-编辑页参数清单-查询
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getContentTagService(TagPojo tagPojo) throws Exception {
        String outlineId = tagPojo.getOutlineId();
        // 是否本章
        if (outlineId == null) {
            tagPojo.setType("all");
        } else {
            tagPojo.setType(null);
        }
        String typeId = tagPojo.getTypeId();
        // 是否选择类型
        if (typeId == null) {
            tagPojo.setTypeId(null);
        } else {
            tagPojo.setTypeId(typeId);
        }
//        // 搜索关键字
//        String paramNameLike = tagPojo.getParamNameLike();
//        if (StringUtils.isBlank(paramNameLike)) {
//            tagPojo.setParamNameLike(null);
//        } else {
//            tagPojo.setParamNameLike(StringEscapeUtils.escapeSql(paramNameLike));
//        }
        List<TagPojo> contentTagDao = fileOperationMapper.getContentTagDao(tagPojo);

        // 获取摘编信息
        // 先读取缓存
        String lawExtractTextListStr = "";
        if (redisUtils.hasKey("getLawExtractTextLists" + envName)) {
            // 如果缓存里已经存在 则无需查库
            lawExtractTextListStr = String.valueOf(redisUtils.get("getLawExtractTextLists" + envName));
        } else {
            try {
                JSONObject params = new JSONObject();
                params.put("pageSize", 10000);
                lawExtractTextListStr = new HttpClient().doPostJson(dfbUrl + "/bgweb/law/getLawExtractTextLists", params);
                // 写入缓存  5分钟更新一次
                redisUtils.set("getLawExtractTextLists" + envName, lawExtractTextListStr, 60 * 5);
            } catch (Exception e) {

            }
        }
        if (!StringUtils.isBlank(lawExtractTextListStr)) { // 防止摘编接口未采集到数据报错
            Map<String, Object> map1 = JSON.parseObject(lawExtractTextListStr, Map.class);
            Object data = map1.get("data");
            Object list = ((Map<String, Object>) data).get("list");
            String s1 = JSON.toJSONString(list);
            ArrayList<Map<String, Object>> lawList = (ArrayList<Map<String, Object>>) (ArrayList) JSON.parseObject(s1, List.class);
            for (Map<String, Object> stringObjectMap : lawList) {
                for (TagPojo pojo : contentTagDao) {
                    if (pojo.getTypeId().equals("1")) {
                        if (pojo.getLawId().equals(String.valueOf(stringObjectMap.get("textId")))) {
                            pojo.setShowName(String.valueOf(stringObjectMap.get("textContent")));
                            pojo.setLawContent(stringObjectMap.get("items"));
                        }
                    }
                }
            }
        }

        //去重
        List<TagPojo> contentTagDao1 = new ArrayList<>();
        List<TagPojo> contentTagDao2 = new ArrayList<>();
        ArrayList<String> wordsUuidStringList = new ArrayList<>();
        // 将词条划分到一组
        for (TagPojo pojo : contentTagDao) {
            // 增加key字段 储存wordsUuid或引用id或释义本身的id 用于聚合
            if (pojo.getTypeId().equals("1")) { // 1摘编 2词条 3释义 4引用 5标注
                pojo.setKey(pojo.getLawId());
            } else if (pojo.getTypeId().equals("2")) {
                pojo.setKey(pojo.getWordsUuid());
            } else if (pojo.getTypeId().equals("3")) {
                pojo.setKey(pojo.getTagId());
            } else if (pojo.getTypeId().equals("4")) {
                pojo.setKey(pojo.getBookmarkUuid());
            } else if (pojo.getTypeId().equals("5")) {
                pojo.setKey(pojo.getTagId());
            } else {
                // TODO  getTypeId不可能为null
                continue;
            }
            // 先过滤出词条
            if (pojo.getTypeId().equals("2")) {
                if (!wordsUuidStringList.contains(pojo.getWordsUuid())) {
                    wordsUuidStringList.add(pojo.getWordsUuid());
                    ArrayList<Map<String, Object>> Strings = new ArrayList<>();
                    Map<String, Object> map = new HashMap<>();
                    map.put("tagId", pojo.getTagId());
                    map.put("outLineId", pojo.getOutlineId());
                    map.put("fileUuid", pojo.getFileUuid());
                    map.put("fileVersionId", pojo.getFileVersionId());
                    map.put("wordsUuid", pojo.getUseIsDel().equals("1") ? null : pojo.getWordsUuid());
                    map.put("bookmarkUuid", pojo.getBookmarkUuid());
                    map.put("lawId", pojo.getLawId());
                    map.put("tagContent", pojo.getTagContent());
                    map.put("showName", pojo.getShowName());
                    map.put("showType", pojo.getShowType());
                    map.put("showDesc", pojo.getShowDesc());
                    map.put("outlineOrder", pojo.getOutlineOrder());
                    Strings.add(map);
                    pojo.setTagIdList(Strings);
                    pojo.setTmpUuid(pojo.getWordsUuid());
                    pojo.setWordsUuid(pojo.getUseIsDel().equals("1") ? null : pojo.getWordsUuid());
                    contentTagDao1.add(pojo);
                } else {
                    for (TagPojo tagPojo1 : contentTagDao1) {
                        if (pojo.getWordsUuid().equals(tagPojo1.getTmpUuid())) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("tagId", pojo.getTagId());
                            map.put("outLineId", pojo.getOutlineId());
                            map.put("fileUuid", pojo.getFileUuid());
                            map.put("fileVersionId", pojo.getFileVersionId());
                            map.put("wordsUuid", pojo.getUseIsDel().equals("1") ? null : pojo.getWordsUuid());
                            map.put("bookmarkUuid", pojo.getBookmarkUuid());
                            map.put("lawId", pojo.getLawId());
                            map.put("tagContent", pojo.getTagContent());
                            map.put("showName", pojo.getShowName());
                            map.put("showType", pojo.getShowType());
                            map.put("showDesc", pojo.getShowDesc());
                            map.put("outlineOrder", pojo.getOutlineOrder());
                            tagPojo1.getTagIdList().add(map);
                        }
                    }
                }
            }  // 过滤出引用
            else if (pojo.getTypeId().equals("4")) {
                if (!wordsUuidStringList.contains(pojo.getBookmarkUuid())) {
                    wordsUuidStringList.add(pojo.getBookmarkUuid());
                    ArrayList<Map<String, Object>> Strings = new ArrayList<>();
                    Map<String, Object> map = new HashMap<>();
                    map.put("tagId", pojo.getTagId());
                    map.put("outLineId", pojo.getOutlineId());
                    map.put("fileUuid", pojo.getFileUuid());
                    map.put("fileVersionId", pojo.getFileVersionId());
                    map.put("wordsUuid", pojo.getWordsUuid());
                    map.put("bookmarkUuid", pojo.getUseIsDel().equals("1") ? null : pojo.getBookmarkUuid());
                    map.put("lawId", pojo.getLawId());
                    map.put("tagContent", pojo.getTagContent());
                    map.put("showName", pojo.getShowName());
                    map.put("showType", pojo.getShowType());
                    map.put("showDesc", pojo.getShowDesc());
                    map.put("bookmarkOutLineId", pojo.getBookmarkOutLineId());
                    map.put("outlineOrder", pojo.getOutlineOrder());
                    Strings.add(map);
                    pojo.setTagIdList(Strings);
                    pojo.setTmpUuid(pojo.getBookmarkUuid());
                    pojo.setBookmarkUuid(pojo.getUseIsDel().equals("1") ? null : pojo.getBookmarkUuid());
                    contentTagDao1.add(pojo);
                } else {
                    for (TagPojo tagPojo1 : contentTagDao1) {
                        if (pojo.getBookmarkUuid().equals(tagPojo1.getTmpUuid())) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("tagId", pojo.getTagId());
                            map.put("outLineId", pojo.getOutlineId());
                            map.put("fileUuid", pojo.getFileUuid());
                            map.put("fileVersionId", pojo.getFileVersionId());
                            map.put("wordsUuid", pojo.getWordsUuid());
                            map.put("bookmarkUuid", pojo.getUseIsDel().equals("1") ? null : pojo.getBookmarkUuid());
                            map.put("lawId", pojo.getLawId());
                            map.put("tagContent", pojo.getTagContent());
                            map.put("showName", pojo.getShowName());
                            map.put("showType", pojo.getShowType());
                            map.put("showDesc", pojo.getShowDesc());
                            map.put("bookmarkOutLineId", pojo.getBookmarkOutLineId());
                            map.put("outlineOrder", pojo.getOutlineOrder());
                            tagPojo1.getTagIdList().add(map);
                        }
                    }
                }
            }   // 过滤出摘编
            else if (pojo.getTypeId().equals("1")) {
                if (!wordsUuidStringList.contains(pojo.getLawId())) {
                    wordsUuidStringList.add(pojo.getLawId()); // 合并相同摘编
                    ArrayList<Map<String, Object>> Strings = new ArrayList<>();
                    Map<String, Object> map = new HashMap<>();
                    map.put("tagId", pojo.getTagId());
                    map.put("outLineId", pojo.getOutlineId());
                    map.put("fileUuid", pojo.getFileUuid());
                    map.put("fileVersionId", pojo.getFileVersionId());
                    map.put("wordsUuid", pojo.getWordsUuid());
                    map.put("bookmarkUuid", pojo.getUseIsDel().equals("1") ? null : pojo.getBookmarkUuid());
                    map.put("lawId", pojo.getLawId());
                    Object textitemIdList = pojo.getTextitemIdList();
                    if (textitemIdList != null) {
                        List list = JSON.parseObject(String.valueOf(textitemIdList), List.class);
                        if (list.size() != 0) {
                            map.put("textitemIdList", list);
                            List<Map<String, String>> lawContent = (List) pojo.getLawContent();
                            List<Map<String, String>> lawContentTmp = new ArrayList<>();
                            for (Map<String, String> stringStringMap : lawContent) {
                                if (list.contains(stringStringMap.get("textitemId"))) {
                                    // 解决重复引用时出现$ref: "$.list[2]" 所以重建对象
                                    lawContentTmp.add(JSON.parseObject(JSON.toJSONString(stringStringMap), Map.class));
                                }
                            }
                            map.put("lawContent", lawContentTmp);
                        } else {// 不勾选默认展示全部
                            List<Map<String, String>> lawContent = (List) pojo.getLawContent();
                            map.put("lawContent", JSON.parseObject(JSON.toJSONString(lawContent), List.class));
                        }
                    } else {// 不勾选默认展示全部
                        List<Map<String, String>> lawContent = (List) pojo.getLawContent();
                        map.put("lawContent", JSON.parseObject(JSON.toJSONString(lawContent), List.class));
                    }
                    map.put("tagContent", pojo.getTagContent());
                    map.put("showName", pojo.getShowName());
                    map.put("showType", pojo.getShowType());
                    map.put("showDesc", pojo.getShowDesc());
                    map.put("bookmarkOutLineId", pojo.getBookmarkOutLineId());
                    map.put("outlineOrder", pojo.getOutlineOrder());
                    Strings.add(map);
                    pojo.setTagIdList(Strings);
                    pojo.setTmpUuid(pojo.getLawId());
                    pojo.setLawId(pojo.getUseIsDel().equals("1") ? null : pojo.getLawId());
                    contentTagDao1.add(pojo);
                } else {
                    for (TagPojo tagPojo1 : contentTagDao1) {
                        if (pojo.getLawId().equals(tagPojo1.getTmpUuid())) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("tagId", pojo.getTagId());
                            map.put("outLineId", pojo.getOutlineId());
                            map.put("fileUuid", pojo.getFileUuid());
                            map.put("fileVersionId", pojo.getFileVersionId());
                            map.put("wordsUuid", pojo.getWordsUuid());
                            map.put("bookmarkUuid", pojo.getUseIsDel().equals("1") ? null : pojo.getBookmarkUuid());
                            map.put("lawId", pojo.getLawId());
                            Object textitemIdList = pojo.getTextitemIdList();
                            if (textitemIdList != null) {
                                List list = JSON.parseObject(String.valueOf(textitemIdList), List.class);
                                if (list.size() != 0) {
                                    map.put("textitemIdList", list);
                                    List<Map<String, String>> lawContent = (List) pojo.getLawContent();
                                    List<Map<String, String>> lawContentTmp = new ArrayList<>();
                                    for (Map<String, String> stringStringMap : lawContent) {
                                        if (list.contains(stringStringMap.get("textitemId"))) {
                                            // 解决重复引用时出现$ref: "$.list[2]" 所以重建对象
                                            lawContentTmp.add(JSON.parseObject(JSON.toJSONString(stringStringMap), Map.class));
                                        }
                                    }
                                    map.put("lawContent", lawContentTmp);
                                } else { // 不勾选默认展示全部
                                    List<Map<String, String>> lawContent = (List) pojo.getLawContent();
                                    map.put("lawContent", JSON.parseObject(JSON.toJSONString(lawContent), List.class));
                                }
                            } else {  // 不勾选默认展示全部
                                List<Map<String, String>> lawContent = (List) pojo.getLawContent();
                                map.put("lawContent", JSON.parseObject(JSON.toJSONString(lawContent), List.class));
                            }
                            map.put("tagContent", pojo.getTagContent());
                            map.put("showName", pojo.getShowName());
                            map.put("showType", pojo.getShowType());
                            map.put("showDesc", pojo.getShowDesc());
                            map.put("bookmarkOutLineId", pojo.getBookmarkOutLineId());
                            map.put("outlineOrder", pojo.getOutlineOrder());
                            tagPojo1.getTagIdList().add(map);
                        }
                    }
                }
            } else {
                ArrayList<Map<String, Object>> Strings = new ArrayList<>();
                Map<String, Object> map = new HashMap<>();
                map.put("tagId", pojo.getTagId());
                map.put("outLineId", pojo.getOutlineId());
                map.put("fileUuid", pojo.getFileUuid());
                map.put("fileVersionId", pojo.getFileVersionId());
                map.put("wordsUuid", pojo.getWordsUuid());
                map.put("bookmarkUuid", pojo.getBookmarkUuid());
                map.put("lawId", pojo.getLawId());
                map.put("tagContent", pojo.getTagContent());
                map.put("showName", pojo.getShowName());
                map.put("showType", pojo.getShowType());
                map.put("showDesc", pojo.getShowDesc());
                map.put("outlineOrder", pojo.getOutlineOrder());
                Strings.add(map);
                pojo.setTagIdList(Strings);
                pojo.setTagIdListLength(1);
                contentTagDao2.add(pojo);
            }
        }

        // 设置长度信息
        for (TagPojo tag : contentTagDao1) {
            tag.setTagIdListLength(tag.getTagIdList().size());
        }

        // 两个结果集合并
        List<TagPojo> contentTagDao3 = new ArrayList<>();
        contentTagDao3.addAll(contentTagDao1);
        contentTagDao3.addAll(contentTagDao2);

        // 搜索关键字
        String paramNameLike = tagPojo.getParamNameLike();
        List<TagPojo> contentTagDao3Tmp = new ArrayList<>();
        if (StringUtils.isNotBlank(paramNameLike)) {
            for (int i = 0; i < contentTagDao3.size(); i++) {
                if (contentTagDao3.get(i).getShowName() != null && contentTagDao3.get(i).getShowName().contains(paramNameLike)) {
                    contentTagDao3Tmp.add(contentTagDao3.get(i));
                }
            }
        } else {
            contentTagDao3Tmp.addAll(contentTagDao3);
        }


        // 根据出现顺序排序 即按outlineOrder排序
        List<TagPojo> contentTagDao4 = new JsonKeyUtils().orderMapList4(contentTagDao3Tmp);
        for (TagPojo paramsPojo : contentTagDao4) {
            List<Map<String, Object>> tagIdList = paramsPojo.getTagIdList();
            List<Map<String, Object>> tagIdList1 = new JsonKeyUtils().orderMapList3_1(tagIdList, "outlineOrder");
            paramsPojo.setTagIdList(tagIdList1);
        }
        return TResponseVo.success(contentTagDao4);
    }


    /**
     * 编辑摘编-获取摘编内容
     * 说明: 入参的lawId是后端定义的摘编主键    前端定义的摘编主键是textId 条款主键是textitemId
     *
     * @param lawId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getLawContentService(String lawId) throws Exception {
        // 获取摘编信息
        // 先读取缓存
        String lawExtractTextListStr = "";
        if (redisUtils.hasKey("getLawExtractTextLists" + envName)) {
            // 如果缓存里已经存在 则无需查库
            lawExtractTextListStr = String.valueOf(redisUtils.get("getLawExtractTextLists" + envName));
        } else {
            try {
                JSONObject params = new JSONObject();
                params.put("pageSize", 10000);
                lawExtractTextListStr = new HttpClient().doPostJson(dfbUrl + "/bgweb/law/getLawExtractTextLists", params);
                // 写入缓存  5分钟更新一次
                redisUtils.set("getLawExtractTextLists" + envName, lawExtractTextListStr, 60 * 5);
            } catch (Exception e) {

            }
        }
        if (!StringUtils.isBlank(lawExtractTextListStr)) { // 防止摘编接口未采集到数据报错
            Map<String, Object> map1 = JSON.parseObject(lawExtractTextListStr, Map.class);
            Object data = map1.get("data");
            Object list = ((Map<String, Object>) data).get("list"); // 摘编
            String s1 = JSON.toJSONString(list);
            ArrayList<Map<String, Object>> lawList = (ArrayList<Map<String, Object>>) (ArrayList) JSON.parseObject(s1, List.class);  // 摘编的list
            for (Map<String, Object> stringObjectMap : lawList) {
                if (lawId.equals(String.valueOf(stringObjectMap.get("textId")))) {
                    return TResponseVo.success(stringObjectMap);
                }
            }
        }
        return TResponseVo.error("未找到对应条款内容");
    }

    /**
     * 编辑摘编-提交
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo commitLawService(TagPojo tagPojo) throws Exception {
        // 获取联合主键
        String fileUuid = tagPojo.getFileUuid();
        String fileVersionId = tagPojo.getFileVersionId();
        String tagId = tagPojo.getTagId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(tagId)) {
            TResponseVo.success("必填参数为空");
        }
        // 获取更新内容
        Object textitemIdList = tagPojo.getTextitemIdList();
        if (textitemIdList != null) {
            tagPojo.setTextitemIdList(JSON.toJSONString(textitemIdList));
        } else {
            tagPojo.setTextitemIdList(null);
        }
        // 写库
        fileOperationMapper.commitLawDao(tagPojo);
        return TResponseVo.success("提交成功");
    }

    /**
     * 书签库-新增书签
     *
     * @param bookmarkPojo
     */
    @Override
    public TResponseVo addBookmarkService(BookmarkPojo bookmarkPojo) throws Exception {
        String bookmarkUuid = UUID.randomUUID().toString().replaceAll("-", "");
        bookmarkPojo.setBookmarkUuid(bookmarkUuid);
        fileOperationMapper.addBookmarkDao(bookmarkPojo);
        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "新建书签完成");
        ret.put("bookmarkUuid", bookmarkUuid);
        return TResponseVo.success(ret);
    }

    /**
     * 书签库-删除书签
     *
     * @param bookmarkPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delBookmarkService(BookmarkPojo bookmarkPojo) throws Exception {
        // 删除刷库
        String bookmarkUuid = bookmarkPojo.getBookmarkUuid();
        String fileUuid = bookmarkPojo.getFileUuid();
        String fileVersionId = bookmarkPojo.getFileVersionId();
        fileOperationMapper.delContentBookmarkDao(bookmarkUuid, fileUuid, fileVersionId);
//        fileOperationMapper.delTagBookmarkDao(bookmarkUuid, fileUuid, fileVersionId);
//
        // 删除文本内数据
        BookmarkPojo bookmarkInfoDao = fileOperationMapper.getBookmarkInfoDao(bookmarkPojo);
        if (bookmarkInfoDao != null) {  // 如果是空 说明段落已经被删了 这里判断防止报错
            String outlineId = bookmarkInfoDao.getOutlineId();
            ContentPojo contentDao = fileOperationMapper.getContentDao(fileUuid, fileVersionId, outlineId);
            String contentText = contentDao.getContentText();


//            org.dom4j.Document document = null;
//            try {
//                document = DocumentHelper.parseText(contentText);  // 必须被同一标签包裹
//            } catch (Exception e) {
//                document = DocumentHelper.parseText("<html>" + contentText.replaceAll("&nbsp;", "") + "</html>");  // 必须被同一标签包裹
//            }
//            // 获取节点元素对象与值
//            String xpath = "//bookmark[@id='" + bookmarkUuid + "']";
//            Node o = document.selectSingleNode(xpath);
//            o.getParent().remove(o);
//            contentText = document.asXML().replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>\n", "");
//            contentDao.setContentText(contentText);


            Document doc = Jsoup.parse(contentText, Parser.xmlParser());
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            Elements elements = doc.select("bookmark[id=" + bookmarkUuid + "]");
            // elements只会包含一个结果
            for (Element element : elements) {
                element.remove();
            }
            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
            contentText = contentText.replaceAll("</br>", "");
            contentDao.setContentText(contentText);

            fileOperationMapper.updateContentDao(contentDao);
        }

        // 更新hf_tag的use_is_del字段为1
        fileOperationMapper.delTagBookmarkDao(bookmarkUuid, fileUuid, fileVersionId);
        return TResponseVo.success("删除成功");
    }


    /**
     * 书签库-增加和删除书签
     *
     * @param bookmarkPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo addAndDelBookmarkService(BookmarkPojo bookmarkPojo) throws Exception {
        String fileUuid = bookmarkPojo.getFileUuid();
        String fileVersionId = bookmarkPojo.getFileVersionId();
        String outlineId = bookmarkPojo.getOutlineId();
//        String contentText = bookmarkPojo.getContentText();
        String userId = bookmarkPojo.getCreateUserId();
        List<Map<String, String>> actions = bookmarkPojo.getActions();
        List<Map<String, String>> actionsAdd = new ArrayList<>();
        List<Map<String, String>> actionsDel = new ArrayList<>();

        // 增删区分到两个栈里
        for (Map<String, String> action : actions) {
            if (action.get("action").equals("insert")) {
                // 新增栈
                actionsAdd.add(action);
            } else if (action.get("action").equals("delete")) {
                //删除栈
                actionsDel.add(action);
            }
        }
        // 成对出现的uuid出栈
        List<Map<String, String>> actionsAdd1 = new ArrayList<>();
        actionsAdd1.addAll(actionsAdd);
        List<Map<String, String>> actionsDel1 = new ArrayList<>();
        actionsDel1.addAll(actionsDel);
        for (Map<String, String> add : actionsAdd) {
            for (Map<String, String> del : actionsDel) {
                if (add.get("bookmarkUuid").equals(del.get("bookmarkUuid"))) {
                    actionsAdd1.remove(add);
                    actionsDel1.remove(del);
                }
            }
        }
        // 新增刷库
        // 获取已经被删除的本文书签
        List<String> deledBookmarkDao = fileOperationMapper.getDeledBookmarkDao(fileUuid, fileVersionId);
        for (Map<String, String> stringStringMap : actionsAdd1) {
            if (deledBookmarkDao.contains(stringStringMap.get("bookmarkUuid"))) {
                // 恢复
                // 判断是否是刚才删除的uuid 如果是就直接拿回来用
                fileOperationMapper.updateContentBookmarkOutlineIdDao(stringStringMap.get("bookmarkUuid"), outlineId, fileUuid, fileVersionId);
                fileOperationMapper.updateTagBookmarkOutlineIdDao(stringStringMap.get("bookmarkUuid"), outlineId, fileUuid, fileVersionId);
                continue;
            }
            // 新建
            String bookmarkUuid = stringStringMap.get("bookmarkUuid");
            String bookmarkName = stringStringMap.get("bookmarkName");
            String bookmarkDesc = stringStringMap.get("bookmarkDesc");
            BookmarkPojo Bookmark1 = new BookmarkPojo();
            Bookmark1.setBookmarkUuid(bookmarkUuid);
            Bookmark1.setBookmarkName(bookmarkName);
            Bookmark1.setBookmarkDesc(bookmarkDesc);
            Bookmark1.setFileUuid(fileUuid);
            Bookmark1.setFileVersionId(fileVersionId);
            Bookmark1.setOutlineId(outlineId);
            Bookmark1.setCreateTime(new Date());
            Bookmark1.setCreateUserId(userId);
            // 校验书签重名
            Integer i = fileOperationMapper.checkBookmarkNameDao(bookmarkName, fileUuid, fileVersionId, bookmarkUuid);
            if (i >= 1) {
                return TResponseVo.error(ResponseEnum.ERROR, "本文已存在同名书签");
            }
            try {
                fileOperationMapper.addContentBookmarkDao(Bookmark1);
            } catch (Exception e) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(890);
                throw fileIndexException;
            }

        }

        // 删除刷库
        for (Map<String, String> stringStringMap : actionsDel1) {
            String bookmarkUuid = stringStringMap.get("bookmarkUuid");
            fileOperationMapper.delContentBookmarkDao(bookmarkUuid, fileUuid, fileVersionId);
            fileOperationMapper.delTagBookmarkDao(bookmarkUuid, fileUuid, fileVersionId);
        }
        // 内容刷库
//        ContentPojo contentPojo = new ContentPojo();
//        contentPojo.setOutlineId(outlineId);
//        contentPojo.setFileUuid(fileUuid);
//        contentPojo.setFileVersionId(fileVersionId);
//        contentPojo.setContentText(contentText);
//
//        // 搜索模式下
//        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
//        Elements elements = doc.select("mark");
//        if (elements.size() != 0) {
//            // 说明是在搜索
//            String searchUuid = bookmarkPojo.getSearchUuid();
//
//            if (searchUuid != null) {
//                // 更新后入库前更新一下缓存
//                System.out.println("searchAddTag : search" + userId + fileUuid + fileVersionId + searchUuid + outlineId);
//                if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
//                    redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
//                }
////                doc = Jsoup.parse(contentText);
////                elements = doc.select("mark");
////                for (Element element : elements) {
////                    element.before(element.html());
////                    element.remove();  // 去除mark标签
////                }
////                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                contentText = contentText.replaceAll("</br>", "");
////                contentPojo.setContentText(contentText);
//            }
//        }
//
//        fileOperationMapper.updateContentDao(contentPojo);

        return TResponseVo.success("操作完成");
    }


    /**
     * 书签库-修改书签
     *
     * @param bookmarkPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateBookmarkService(BookmarkPojo bookmarkPojo) throws Exception {
        String bookmarkUuid = bookmarkPojo.getBookmarkUuid();
        if (StringUtils.isBlank(bookmarkUuid)) {
            return TResponseVo.error(ResponseEnum.ERROR, "书签id不能为空");
        }
        fileOperationMapper.updateBookmarkDao(bookmarkPojo);
        return TResponseVo.success("更新完成");
    }

    /**
     * 书签库-查询书签信息
     *
     * @param bookmarkPojo
     */
    @Override
    public TResponseVo getBookmarkInfoService(BookmarkPojo bookmarkPojo) throws Exception {
        BookmarkPojo bookmarkPojoDao = fileOperationMapper.getBookmarkInfoDao(bookmarkPojo);
        return TResponseVo.success(bookmarkPojoDao);
    }

    /**
     * 书签库-查询书签列表
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getBookmarkListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        String fileUuid = pagePojo.getFileUuid();
        String fileVersionId = pagePojo.getFileVersionId();
        String outlineId = pagePojo.getOutlineId();
        List<BookmarkPojo> bookmarkListDao = fileOperationMapper.getBookmarkListDao(paramNameLike, null, fileUuid, fileVersionId, outlineId);

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(bookmarkListDao);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(bookmarkListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", bookmarkListDao.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 书签库-查询LabelValue数据
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getBookmarkLabelValueListService(String fileUuid, String fileVersionId) throws Exception {
        List<LabelValuePojo> bookmarkLabelValueListDao = fileOperationMapper.getBookmarkLabelValueListDao(fileUuid, fileVersionId);
        return TResponseVo.success(bookmarkLabelValueListDao);
    }

    /**
     * 判断读取文件的versionId
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getTrueVersionIdService(String fileUuid, HttpSession session) throws Exception {
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao == null) {
            throw new WorkTableException("文件不能打开");
        }
        // 如果不是模板类文件 直接返回数据
        if (!docAllInfoDao.getFileClass().equals("1") && !docAllInfoDao.getFileClass().equals("0")) {
            HashMap<String, String> ret = new HashMap<>();
            ret.put("info", "RW");
            ret.put("fileUuid", fileUuid);
            ret.put("fileVersionId", docAllInfoDao.getFileVersionId());
            ret.put("fileTypeId", docAllInfoDao.getFileTypeId());
            ret.put("fileTypeName", docAllInfoDao.getFileTypeName());
            return TResponseVo.success(ret);
        }
        String fileTypeId = docAllInfoDao.getFileTypeId();
        String fileTypeName = docAllInfoDao.getFileTypeName();
        String fileVersionId = docAllInfoDao.getFileVersionId();
        // 判断当前用户是否有权限
        String includeUserList = docAllInfoDao.getIncludeUserList();
        List<String> includeUserIdList = new JsonKeyUtils().stringToList(includeUserList);
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        DocUserPojo userInfoDao = workingTableMapper.getUserInfoDao(userId);

        if (includeUserIdList.contains(userId) || docAllInfoDao.getCreateUserId().equals(userId) || "642dc231344149d98f5db898e3c0f4f9".equals(userInfoDao.getRolesId())) {
            // 有编辑权限
            DocFileVerIndexPojo draftVersion = fileOperationMapper.getDraftVersionIdDao(fileUuid);
            if (draftVersion == null) {
                String newFileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                docAllInfoDao.setDraftVersionId(newFileVersionId);
                docAllInfoDao.setIsDraft("1");
                // 升级版本号
                DocFileIndexPojo versionAllInfoDao = fileOperationMapper.getVersionAllInfoDao1(fileUuid, fileVersionId);
                String fileVersionName = versionAllInfoDao.getFileVersionName();
                String newFileVersionName = new JsonKeyUtils().versionCount(fileVersionName, 1);
                docAllInfoDao.setFileVersionName(newFileVersionName);
                newShadowVersionService(docAllInfoDao);
                HashMap<String, String> ret = new HashMap<>();
                ret.put("info", "RW");
                ret.put("fileUuid", fileUuid);
                ret.put("fileVersionId", newFileVersionId);
                ret.put("fileTypeId", fileTypeId);
                ret.put("fileTypeName", fileTypeName);

                // 同步参数库历史记录
                // 获取最新版本 id
                String versionId = fileOperationMapper.getLastVersionIdDao(fileUuid);
                // 获取发布版本参数
                List<DocParamsPojo> paramDao = fileOperationMapper.getParamDao(fileUuid, versionId);
                List<DocParamsPojo> paramList = paramDao.stream().map(docParamsPojo -> {
                    docParamsPojo.setFileVersionId(newFileVersionId);
                    return docParamsPojo;
                }).collect(Collectors.toList());
                if (paramList.size() > 0) {
                    fileOperationMapper.insertBatchParamDao(paramList);
                }

                return TResponseVo.success(ret);
            } else {
                String draftVersionIdDao = draftVersion.getFileVersionId();
                HashMap<String, String> ret = new HashMap<>();
                ret.put("info", "RW");
                ret.put("fileUuid", fileUuid);
                ret.put("fileVersionId", draftVersionIdDao);
                ret.put("fileTypeId", fileTypeId);
                ret.put("fileTypeName", fileTypeName);
                return TResponseVo.success(ret);
            }
        } else {
            // 没有编辑权限
            HashMap<String, String> ret = new HashMap<>();
            ret.put("info", "RO");
            ret.put("fileUuid", fileUuid);
            ret.put("fileVersionId", fileVersionId);
            ret.put("fileTypeId", fileTypeId);
            ret.put("fileTypeName", fileTypeName);
            return TResponseVo.success(ret);
        }
    }

    /**
     * 填写参数
     *
     * @param docParamsPojo
     * @param
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo writeParamService(DocParamsPojo docParamsPojo) throws Exception {
        // 设置double格式
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setGroupingUsed(false);  // 不使用科学计数法
        nf.setMaximumFractionDigits(13);  // 允许的最大小数位数

        String fileUuid = docParamsPojo.getFileUuid();
        String fileVersionId = docParamsPojo.getFileVersionId();
        String feOutlineId = docParamsPojo.getOutlineId();  // 前端正在填写的页面
        String feContentText = docParamsPojo.getContentText(); // 前端正在填写的页面的内容
        String paramsUuid = docParamsPojo.getParamsUuid();
        String isIgnore = docParamsPojo.getIsIgnore();  // 是否忽略
        String paramsText = String.valueOf(docParamsPojo.getParamsText());


        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(paramsUuid)) {
            return TResponseVo.error(ResponseEnum.ERROR, "必填参数为空");
        }


        // 判断是否正在协同编辑
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (redisUtils.hasKey(roomKey)) {
            // 如果正在协同 就判断本次填参是否来自协同空间
            String writeWay = docParamsPojo.getWriteWay();
            if (StringUtils.isNotBlank(writeWay) && "team".equals(writeWay)) {
                // 正常执行
            } else {
                throw new WorkTableException("本文正在协同编辑，请前往协同编辑中填参。");
            }
        }


        if (StringUtils.isBlank(paramsText) || docParamsPojo.getParamsText() == null) {
            paramsText = "";
        }
        // 找到整个文件组的所有这个参数
        // 根据`file_uuid`,`file_version_id`,`params_uuid`就能确定文件组内所有该参数位置
//        docParamsPojo.setGetAllFlag(false);  // 决定填写的参数是否联动到本文件以外的其他关联文件
        List<DocParamsPojo> docParamsPojos = fileOperationMapper.getContentAllParamDao(docParamsPojo);

        // 用于存放金额与数值参数的当前段落返回值 格式:[{ uuid: '', value: '', typeId: '参数类型' }]
        List<Map<String, Object>> newParamsTextList = new ArrayList<>();
        // 获取单位进制维表
        List<DocframeParamsUnitDiPojo> unitDiDao = fileOperationMapper.getUnitDiDao();
        // 确认参数类型
//        10  单选
//        20  多选
//        30  日期/时间
//        40  文本
//        50  富文本
//        60  图片
//        70  资源附件
//        80  金额
//        90  数值
        String paramsTypeId = "";  // 参数类型
        String paramsRange = "";  // 所有选项及答案
        String paramsChoose = "";  // 选项对应的内容
        String newParamsText = ""; // 更新到content中的值
        List<Map<String, String>> paramsColumns = null; // 矩阵参数表头
        String matrixDisplay = ""; // 矩阵参数显示效果
        List<Map<String, String>> matrixDisplayList = new ArrayList<>();  // 暂时装载从feContentText解析出来的uuid对应的display
        for (DocParamsPojo paramsPojo : docParamsPojos) {
            paramsTypeId = paramsPojo.getParamsTypeId();
            paramsRange = paramsPojo.getParamsRange();
            paramsColumns = JSON.parseObject(String.valueOf(paramsPojo.getParamsColumns()), List.class);
            matrixDisplay = paramsPojo.getMatrixDisplay();
            break;
        }

        // 第一步 更新参数表
        if (isIgnore == null) {
            Integer integer = 0;  // 记录更新的条目数
            if (paramsTypeId.equals("40") || paramsTypeId.equals("30")) {  // 文本 时间
                // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段
                String sql = "";
                for (DocParamsPojo paramsPojo : docParamsPojos) {
                    sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 3);
                    integer = fileOperationMapper.writeParamDao(paramsText, docParamsPojo.getUserId(), sql);
                }
            } else if (paramsTypeId.equals("10")) {  // 单选
                List<Map<String, String>> paramsRangeList = JSON.parseObject(paramsRange, List.class);
                // 找到选项的内容
                for (Map<String, String> m : paramsRangeList) {
                    if (m.get("value").equals(paramsText)) {
                        paramsChoose = m.get("label");
                        break;
                    }
                }
                // 把内容写到paramsChoose字段中
                String sql = "";
                for (DocParamsPojo paramsPojo : docParamsPojos) {
                    sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 3);
                    integer = fileOperationMapper.writeParamSelectDao(paramsText, docParamsPojo.getUserId(), paramsChoose, sql);
                }
            } else if (paramsTypeId.equals("20")) {  // 多选
                List<String> paramsTextList = (List<String>) (List) docParamsPojo.getParamsTextList();
                String paramsTextListStr = new JsonKeyUtils().listToString(paramsTextList);
                if (paramsTextList == null || paramsTextList.size() == 0) {
                    paramsChoose = "";
                } else {
                    List<Map<String, String>> paramsRangeList = JSON.parseObject(paramsRange, List.class);
                    // 找到选项的内容
                    for (Map<String, String> m : paramsRangeList) {
                        for (String paramsT : paramsTextList) {
                            if (m.get("value").equals(paramsT)) {
                                paramsChoose = paramsChoose + m.get("label") + "、";
                                break;
                            }
                        }
                    }
                    paramsChoose = paramsChoose.substring(0, paramsChoose.length() - 1);
                }
                // 把内容写到paramsChoose字段中
                String sql = "";
                for (DocParamsPojo paramsPojo : docParamsPojos) {
                    sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 3);
                    integer = fileOperationMapper.writeParamSelectDao(paramsTextListStr, docParamsPojo.getUserId(), paramsChoose, sql);
                }
            } else if (paramsTypeId.equals("80")) {  // 金额
                Map<String, Object> paramsTextList = (Map<String, Object>) (Map) docParamsPojo.getParamsTextList();
                if (paramsTextList != null && paramsTextList.keySet().size() != 0) {
                    integer = fileOperationMapper.setParamNullDao(fileUuid, fileVersionId, paramsUuid);
                    for (String uninId : paramsTextList.keySet()) {
                        String unitName = "";
                        unitName = uninId;
                        String sql = "";
                        for (DocParamsPojo paramsPojo : docParamsPojos) {
                            if (unitName.equals(paramsPojo.getUnit())) {
                                sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                            } else {
                                continue;
                            }
                        }
                        if (sql.length() != 0) { // 填写的单位不在文内参数的单位中 就不填
                            sql = sql.substring(0, sql.length() - 3);
                            integer += fileOperationMapper.writeParamDao(nf.format(paramsTextList.get(uninId)), docParamsPojo.getUserId(), sql);
                        }
                    }
                } else {
                    String sql = "";
                    for (DocParamsPojo paramsPojo : docParamsPojos) {
                        sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 3);
                        integer += fileOperationMapper.writeParamDao(paramsText, docParamsPojo.getUserId(), sql);
                    }
                }
            } else if (paramsTypeId.equals("90")) {  // 数值
                Map<String, Object> paramsTextList = (Map<String, Object>) (Map) docParamsPojo.getParamsTextList();
                if (paramsTextList != null && paramsTextList.keySet().size() != 0) {
                    integer = fileOperationMapper.setParamNullDao(fileUuid, fileVersionId, paramsUuid);
                    for (String uninId : paramsTextList.keySet()) {
                        String unitName = "";
                        unitName = uninId;
                        String sql = "";
                        for (DocParamsPojo paramsPojo : docParamsPojos) {
                            if (unitName.equals(paramsPojo.getUnit())) {
                                sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                            } else {
                                continue;
                            }
                        }
                        if (sql.length() != 0) { // 填写的单位不在文内参数的单位中 就不填
                            sql = sql.substring(0, sql.length() - 3);
                            integer += fileOperationMapper.writeParamDao(nf.format(paramsTextList.get(uninId)), docParamsPojo.getUserId(), sql);
                        }
                    }
                } else {
                    String sql = "";
                    for (DocParamsPojo paramsPojo : docParamsPojos) {
                        sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 3);
                        integer += fileOperationMapper.writeParamDao(paramsText, docParamsPojo.getUserId(), sql);
                    }
                }
            } else if (paramsTypeId.equals("70")) {  // 附件
                List<Map<String, String>> paramsTextList = (List<Map<String, String>>) (List) docParamsPojo.getParamsTextList();
                paramsText = JSON.toJSONString(paramsTextList);
                // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段
                String sql = "";
                for (DocParamsPojo paramsPojo : docParamsPojos) {
                    sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 3);
                    integer = fileOperationMapper.writeParamDao(paramsText, docParamsPojo.getUserId(), sql);
                }
            } else if (paramsTypeId.equals("60")) {  // 图片
                List<Map<String, String>> paramsTextList = (List<Map<String, String>>) (List) docParamsPojo.getParamsTextList();
                paramsText = JSON.toJSONString(paramsTextList);
                // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段
                String sql = "";
                for (DocParamsPojo paramsPojo : docParamsPojos) {
                    sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 3);
                    integer = fileOperationMapper.writeParamDao(paramsText, docParamsPojo.getUserId(), sql);
                }
            } else if (paramsTypeId.equals("50")) {  // 富文本
                // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段
                String sql = "";
                for (DocParamsPojo paramsPojo : docParamsPojos) {
                    sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 3);
                    integer = fileOperationMapper.writeParamDao(paramsText, docParamsPojo.getUserId(), sql);
                }
            } else if (paramsTypeId.equals("95")) { // 矩阵
                // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段
                List<Map<String, String>> paramsTextList = (List<Map<String, String>>) (List) docParamsPojo.getParamsTextList();
                paramsText = JSON.toJSONString(paramsTextList);
                String sql = "";
                for (DocParamsPojo paramsPojo : docParamsPojos) {
                    sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                }
                if (sql.length() != 0) {
                    sql = sql.substring(0, sql.length() - 3);
                    integer = fileOperationMapper.writeParamDao(paramsText, docParamsPojo.getUserId(), sql);
                }
                // 手动类型的矩阵参数要同时更新matrix_display
                DocParamsPojo paramInfoDao = fileOperationMapper.getContentParamInfo2Dao(fileUuid, fileVersionId, paramsUuid);
                if ("matrix".equals(paramInfoDao.getMatrixMode())) {
                    Document feContentTextDoc = Jsoup.parse(feContentText, Parser.xmlParser());
                    Elements elementsBlock = feContentTextDoc.select("parameterBlock[key~=(" + paramsUuid + ")]");
                    Elements elementsTmp = new Elements();
                    elementsTmp.addAll(elementsBlock);
                    for (Element element : elementsTmp) {
                        Element table = element.selectFirst("table");
                        String uuid = element.attr("uuid");
                        Elements trList = table.select("tr");
                        for (int i = 1; i < trList.size(); i++) {  // 按行逐一获取
                            Element elementRow = trList.get(i);
                            Elements tablecellpos = elementRow.select("tablecellpos");
                            for (Element tablecellpo : tablecellpos) {  // 单行中的单元格
                                tablecellpo.removeAttr("value");
                            }
                        }
                        fileOperationMapper.updateParamMatrixDisplayDao(fileUuid, fileVersionId, uuid, table.toString());
                        Map<String, String> map = new HashMap<>();
                        map.put("uuid", uuid);
                        map.put("matrixDisplay", table.toString());
                        matrixDisplayList.add(map);
//                    System.out.println("  11111 " + fileUuid + "  11111 " + fileVersionId + "  11111 " + uuid);
                    }
                }
            }

            // 第二步 更新文内参数
            if (integer != 0) {  // 说明触发了更新 不然则未触发更新
                // 保证每个独立的段落只更新一次
                ArrayList<DocParamsPojo> docParamsPojosTmp = new ArrayList<>();
//            docParamsPojosTmp.addAll(docParamsPojos);
                String fileUuid1 = "";
                String fileVersionId1 = "";
                String outlineId1 = "";
                String uuid1 = "";
                DocParamsPojo paramsPojo1 = new DocParamsPojo();
                String fileUuid2 = "";
                String fileVersionId2 = "";
                String outlineId2 = "";
                String uuid2 = "";
                DocParamsPojo paramsPojo2 = new DocParamsPojo();
                for (DocParamsPojo paramsPojo : docParamsPojos) {
                    docParamsPojosTmp.add(paramsPojo);
                    fileUuid1 = paramsPojo.getFileUuid();
                    fileVersionId1 = paramsPojo.getFileVersionId();
                    outlineId1 = paramsPojo.getOutlineId();
                    uuid1 = paramsPojo.getUuid();
                    paramsPojo1 = paramsPojo;
                    if (fileUuid1.equals(fileUuid2) && fileVersionId1.equals(fileVersionId2) && outlineId1.equals(outlineId2)) {
                        paramsPojo.setUuid(uuid1 + "|" + uuid2);
                        docParamsPojosTmp.remove(paramsPojo2);
                    }
                    fileUuid2 = fileUuid1;
                    fileVersionId2 = fileVersionId1;
                    outlineId2 = outlineId1;
                    uuid2 = paramsPojo.getUuid();
                    paramsPojo2 = paramsPojo1;
                }
                // docParamsPojos数据被破坏了 重新查一遍
                docParamsPojos = fileOperationMapper.getContentAllParamDao(docParamsPojo);
                // 将参数更新至content中
                for (DocParamsPojo paramsPojo : docParamsPojosTmp) {  // docParamsPojosTmp的size表示要更新的段落数量
                    String contentText = paramsPojo.getContentText();


                    Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                    doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                    Elements elements = doc.select("parameter[uuid~=(" + paramsPojo.getUuid() + ")]");  // paramsPojo.getUuid()是以|隔开的
                    Elements elementsBlock = doc.select("parameterblock[uuid~=(" + paramsPojo.getUuid() + ")]");  // paramsPojo.getUuid()是以|隔开的
                    Elements elementsTmp = new Elements();
                    elementsTmp.addAll(elements);
                    elementsTmp.addAll(elementsBlock);
                    for (Element element : elementsTmp) {  // elementsTmp的size表示段落中要更新几处
                        if (paramsTypeId.equals("40")) {  // 文本参数没有格式 直接更新至文本中
                            newParamsText = null;
                            if (paramsText == null) {
                                element.removeAttr("value");
                            } else {
                                element.attr("value", paramsText);
                                element.attr("typeId", "40");
                                newParamsText = paramsText;
                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put("uuid", element.attr("uuid"));
                            map.put("value", newParamsText);
                            map.put("typeId", "40");
                            newParamsTextList.add(map);
                        } else if (paramsTypeId.equals("30")) {  // 时间格式 需要匹配样式
                            newParamsText = null;
                            for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                if (element.attr("uuid").equals(pojo.getUuid())
//                                    && pojo.getFileUuid().equals(fileUuid)
//                                    && pojo.getFileVersionId().equals(fileVersionId)
                                ) {
                                    String styleContent = pojo.getStyleContent();
                                    newParamsText = new TimeFormatUtils().getFormatTimeStr(paramsText, styleContent);
                                    break;
                                }
                            }
                            if (newParamsText == null) {
                                element.removeAttr("value");
                            } else {
                                element.attr("value", newParamsText);
                                element.attr("typeId", "30");
                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put("uuid", element.attr("uuid"));
                            map.put("value", newParamsText);
                            map.put("typeId", "30");
                            newParamsTextList.add(map);
                        } else if (paramsTypeId.equals("10")) {  // 单选 也需要匹配样式
                            newParamsText = "";
                            for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                if (element.attr("uuid").equals(pojo.getUuid())
//                                    && pojo.getFileUuid().equals(fileUuid)
//                                    && pojo.getFileVersionId().equals(fileVersionId)
                                ) {
                                    String styleId = pojo.getStyleId();
                                    // 根据styleId来判断显示样式
                                    if (styleId == null) {  // 无格式默认 选项A/选项B
                                        newParamsText = paramsChoose;
                                        break;
                                    } else if (styleId.equals("632001")) {  // 选项A/选项B
                                        newParamsText = paramsChoose;
                                        break;
                                    } else if (styleId.equals("632002")) {  // 口 选项A；口 选项B
                                        List<Map<String, String>> paramsRangeList = JSON.parseObject(pojo.getParamsRange(), List.class);
                                        // 找到选项的内容
                                        int j = 0;  // 标记位
                                        for (Map<String, String> m : paramsRangeList) {
                                            if (m.get("value").equals(paramsText)) {
                                                newParamsText = newParamsText + "▣️" + m.get("label") + "；";
                                                j = 1;
                                            } else {
                                                newParamsText = newParamsText + "▢" + m.get("label") + "；";
                                            }
                                        }
                                        if (j == 0) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = newParamsText.substring(0, newParamsText.length() - 1);
                                        }
                                        break;
                                    }
                                }
                            }
                            if (newParamsText == null) {
                                element.removeAttr("value");
                            } else {
                                element.attr("value", newParamsText);
                                element.attr("typeId", "10");
                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put("uuid", element.attr("uuid"));
                            map.put("value", newParamsText);
                            map.put("typeId", "10");
                            newParamsTextList.add(map);
                        } else if (paramsTypeId.equals("20")) {  // 多选 匹配样式
                            newParamsText = "";
                            for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                if (element.attr("uuid").equals(pojo.getUuid())
//                                    && pojo.getFileUuid().equals(fileUuid)
//                                    && pojo.getFileVersionId().equals(fileVersionId)
                                ) {
                                    String styleId = pojo.getStyleId();
                                    // 根据styleId来判断显示样式
                                    if (styleId == null) {  // 无格式默认 选项A、选项B、选项C
                                        newParamsText = paramsChoose;
                                        break;
                                    } else if (styleId.equals("632003")) {  // 选项A、选项B、选项C
                                        newParamsText = paramsChoose;
                                        break;
                                    }
                                }
                            }
                            if (newParamsText == null) {
                                element.removeAttr("value");
                            } else {
                                element.attr("value", newParamsText);
                                element.attr("typeId", "20");
                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put("uuid", element.attr("uuid"));
                            map.put("value", newParamsText);
                            map.put("typeId", "20");
                            newParamsTextList.add(map);
                        } else if (paramsTypeId.equals("80")) {  // 金额 匹配样式
                            newParamsText = "";
                            for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                if (element.attr("uuid").equals(pojo.getUuid())
//                                    && pojo.getFileUuid().equals(fileUuid)
//                                    && pojo.getFileVersionId().equals(fileVersionId)
                                ) {
                                    element.removeAttr("value");
                                    // 根据当前单位来判断填写值
                                    Map<String, Object> paramsTextList = (Map<String, Object>) (Map) docParamsPojo.getParamsTextList();

                                    if (paramsTextList != null) {
                                        paramsText = null;
                                        for (String uninId : paramsTextList.keySet()) {
                                            if (uninId.equals(pojo.getUnit())) {
                                                paramsText = paramsTextList.get(uninId) == null ? null : nf.format(paramsTextList.get(uninId));
                                                break;
                                            }
                                        }
                                    } else {
                                        paramsText = null;
                                    }


                                    String styleId = pojo.getStyleId();
                                    // 根据styleId来判断显示样式
                                    if (styleId == null) {  // 无格式默认
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText;
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632007")) {  // ***（单位为元，元不显示在正文）
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText;
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("6320071")) {  // ***（单位为万元，万元不显示在正文）
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText;
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632008")) {  // ***元
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText + "元";
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632009")) {  // ***万元
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText + "万元";
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632010")) {  // ***（元）
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText + "(元)";
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632011")) {  // ***（万元）
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText + "(万元)";
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632012")) {  // 大写
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            try {
                                                newParamsText = new CNMoneyUtils().number2CNMontrayUnit(paramsText);
                                            } catch (Exception e) {
                                                newParamsText = paramsText;
                                            }
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632013")) {  // ¥***
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = "¥" + paramsText;
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632014")) {  // （¥***）
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = "(¥" + paramsText + ")";
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "80");
                                        newParamsTextList.add(map);
                                        break;
                                    }
                                }
                            }
                            if (newParamsText == null) {
                                element.removeAttr("value");
                            } else {
                                element.attr("value", newParamsText);
                                element.attr("typeId", "80");
                            }
                        } else if (paramsTypeId.equals("90")) {  // 数值 匹配样式
                            newParamsText = "";
                            String unitName = "";
                            for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                if (element.attr("uuid").equals(pojo.getUuid())
//                                    && pojo.getFileUuid().equals(fileUuid)
//                                    && pojo.getFileVersionId().equals(fileVersionId)
                                ) {
                                    element.removeAttr("value");
                                    // 根据当前单位来判断填写值
                                    Map<String, Object> paramsTextList = (Map<String, Object>) (Map) docParamsPojo.getParamsTextList();
                                    if (paramsTextList != null) {
                                        paramsText = null;
                                        for (String uninId : paramsTextList.keySet()) {
                                            if (uninId.equals(pojo.getUnit())) {
                                                paramsText = paramsTextList.get(uninId) == null ? null : nf.format(paramsTextList.get(uninId));
                                                break;
                                            }
                                        }
                                    } else {
                                        paramsText = null;
                                    }
                                    // 获取到当前数值的单位名称
                                    for (DocframeParamsUnitDiPojo unitDiPojo : unitDiDao) {
                                        if (unitDiPojo.getUnitId().equals(pojo.getUnit())) {
                                            unitName = unitDiPojo.getUnitName();  // 获取到当前数值的单位名称
                                            break;
                                        }
                                    }
                                    // 获取styleId样式
                                    String styleId = pojo.getStyleId();
                                    // 根据styleId来判断显示样式
                                    if (styleId == null) {  // 无格式默认
                                        newParamsText = paramsText;
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "90");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632004")) {  // ***
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText;
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "90");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632005")) {  // ***单位
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText + unitName;
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "90");
                                        newParamsTextList.add(map);
                                        break;
                                    } else if (styleId.equals("632006")) {  // ***(单位)
                                        if (paramsText == null) {
                                            newParamsText = null;
                                        } else {
                                            newParamsText = paramsText + "（" + unitName + "）";
                                        }
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("uuid", pojo.getUuid());
                                        map.put("value", newParamsText);
                                        map.put("typeId", "90");
                                        newParamsTextList.add(map);
                                        break;
                                    }
                                }
                            }
                            if (newParamsText == null) {
                                element.removeAttr("value");
                            } else {
                                element.attr("value", newParamsText);
                                element.attr("typeId", "90");
                            }
                        } else if (paramsTypeId.equals("70")) {  // 附件
                            newParamsText = "";
                            for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                if (element.attr("uuid").equals(pojo.getUuid())
//                                    && pojo.getFileUuid().equals(fileUuid)
//                                    && pojo.getFileVersionId().equals(fileVersionId)
                                ) {
                                    String styleId = pojo.getStyleId();
                                    // 根据styleId来判断显示样式
                                    if (styleId == null) {  // 无格式默认
                                        newParamsText = paramsText;
                                        break;
                                    } else if (styleId.equals("632041")) {  // ***
                                        newParamsText = paramsText;
                                        break;
                                    }
                                }
                            }
                            if (newParamsText == null) {
                                element.removeAttr("value");
                            } else {
                                element.attr("value", encodeURI(newParamsText));
                                element.attr("typeId", "70");
                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put("uuid", element.attr("uuid"));
                            map.put("value", encodeURI(newParamsText));
                            map.put("typeId", "70");
                            newParamsTextList.add(map);
                        } else if (paramsTypeId.equals("60")) {  // 图片
                            newParamsText = "";
                            for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                if (element.attr("uuid").equals(pojo.getUuid())
//                                    && pojo.getFileUuid().equals(fileUuid)
//                                    && pojo.getFileVersionId().equals(fileVersionId)
                                ) {
                                    String styleId = pojo.getStyleId();
                                    // 根据styleId来判断显示样式
                                    if (styleId == null) {  // 无格式默认
                                        newParamsText = paramsText;
                                        break;
                                    } else if (styleId.equals("632040")) {  // ***
                                        newParamsText = paramsText;
                                        break;
                                    }
                                }
                            }
                            if (newParamsText == null) {
                                element.removeAttr("value");
                            } else {
                                element.attr("value", encodeURI(newParamsText));
//                            element.attr("value", JSON.parseObject(newParamsText,List.class));
                                element.attr("typeId", "60");
                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put("uuid", element.attr("uuid"));
                            map.put("value", encodeURI(newParamsText));
//                        map.put("value", JSON.parseObject(newParamsText,List.class));
                            map.put("typeId", "60");
                            newParamsTextList.add(map);
                        } else if (paramsTypeId.equals("50")) {  // 富文本
                            newParamsText = null;
                            if (paramsText == null) {
                                element.removeAttr("value");
                                element.empty();
                            } else {
                                element.attr("value", paramsText);
                                element.attr("typeId", "50");
                                element.html(paramsText);
                                newParamsText = paramsText;
                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put("uuid", element.attr("uuid"));
                            map.put("value", newParamsText);
                            map.put("typeId", "50");
                            newParamsTextList.add(map);
                        } else if (paramsTypeId.equals("95")) {  // 矩阵
                            newParamsText = null;
                            if (paramsText == null) {
                                element.removeAttr("value");
                            } else {
                                element.attr("value", paramsText);
                                element.attr("typeId", "95");
                            }
                            // 判断矩阵填写模式
                            if ("matrix".equals(paramsPojo.getMatrixMode())) {  // 手动填充

                                for (Map<String, String> map : matrixDisplayList) {
                                    if (paramsPojo.getUuid().equals(map.get("uuid"))) {
                                        matrixDisplay = map.get("matrixDisplay");
                                    }
                                }

                                Document matrixDisplayDoc = Jsoup.parse(matrixDisplay, Parser.xmlParser());
                                matrixDisplayDoc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                                Elements trList = matrixDisplayDoc.select("tr");
//                            Element headTr = trList.get(0);  // 获取表头

                                // 收集所有列的id
                                ArrayList<String> valueList = new ArrayList<>();
                                for (Map<String, String> paramsColumn : paramsColumns) {
                                    valueList.add(paramsColumn.get("value"));
                                }
                                // 收集填入的数据
                                List<Map<String, String>> paramsTextList = (List) docParamsPojo.getParamsTextList();
                                // 组装Element
                                Element tbody = element.selectFirst("tbody");
                                if (tbody == null) {
                                    tbody = new Element("tbody");
                                }
                                tbody.empty();
//                            tbody.appendChild(headTr);
                                // 根据坐标获取值
                                // 1.先获取坐标
                                for (int i = 0; i < trList.size(); i++) {  // 按行逐一获取
                                    Element elementRow = trList.get(i);
                                    Elements tablecellpos = elementRow.select("tablecellpos");
                                    for (Element tablecellpo : tablecellpos) {  // 单行中的单元格
                                        String row = tablecellpo.attr("row");  // 第几行
                                        String col = tablecellpo.attr("col");  // 第几列
                                        // 根据第几行第几列获取paramsTextList中的值
                                        try {
                                            String value = paramsTextList.get(Integer.valueOf(row) - 1).get(valueList.get(Integer.valueOf(col) - 1));
//                                        tablecellpo.before(value);
                                            tablecellpo.attr("value", value);
                                        } catch (Exception e) {
                                            System.out.println("未获取到填写值");
                                        }
                                    }
                                    tbody.appendChild(elementRow);
                                }
                                newParamsText = element.select("table").toString();

                            } else if ("column".equals(paramsPojo.getMatrixMode())) {  // 自动填充
//                            String matrixDisplay = paramsPojo.getMatrixDisplay();
                                Document matrixDisplayDoc = Jsoup.parse(matrixDisplay, Parser.xmlParser());
                                matrixDisplayDoc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                                Elements trList = matrixDisplayDoc.select("tr");
                                Element headTr = trList.get(0);  // 获取表头
                                Element contentTr = trList.get(1);  // 获取表内容单行格式
                                // 收集所有列的id
//                            List<Map<String, String>> paramsColumns = (List) docParamsPojo.getParamsColumns();
                                ArrayList<String> valueList = new ArrayList<>();
                                for (Map<String, String> paramsColumn : paramsColumns) {
                                    valueList.add(paramsColumn.get("value"));
                                }
                                // 收集填入的数据
                                List<Map<String, String>> paramsTextList = (List) docParamsPojo.getParamsTextList();
                                // 组装Element
                                Element tbody = element.selectFirst("tbody");
                                if (tbody == null) {
                                    tbody = new Element("tbody");
                                }
                                tbody.empty();
                                tbody.appendChild(headTr);
                                // 构建content内容
                                if (paramsTextList != null) {
                                    for (Map<String, String> map : paramsTextList) {
                                        Element cloneTr = contentTr.clone();
                                        Elements ps = cloneTr.select("p");
                                        for (int i = 0; i < ps.size(); i++) {
                                            if (map.get(valueList.get(i)) != null) {
                                                ps.get(i).text(map.get(valueList.get(i)));
                                            }
                                        }
                                        tbody.appendChild(cloneTr);
                                    }
                                }

                                newParamsText = element.select("table").toString();

                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put("uuid", element.attr("uuid"));
                            map.put("value", newParamsText);
                            map.put("typeId", "95");
                            newParamsTextList.add(map);
                        }
                        //
                    }
                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
                    paramsPojo.setContentText(contentText);

                    // 搜索模式下
                    String searchUuid = docParamsPojo.getSearchUuid();
                    String userId = docParamsPojo.getUserId();
                    String outlineId = paramsPojo.getOutlineId();
                    if (searchUuid != null) {
                        // 更新后入库前更新一下缓存
                        if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                            redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                        }

//                    doc = Jsoup.parse(contentText);
//                    elements = doc.select("mark");
//                    for (Element element : elements) {
//                        element.before(element.html());
//                        element.remove();  // 去除mark标签
//                    }
//                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                        contentText = contentText.replaceAll("</br>", "");
//                    paramsPojo.setContentText(contentText);
                    }
//                System.out.println("paramsPojo :" + paramsPojo);
                    fileOperationMapper.writeContentParamDao(paramsPojo);
                }
            }

            // 记录填写历史
            HfParamChangeHistoryPojo hfParamChangeHistoryPojo = new HfParamChangeHistoryPojo();
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            hfParamChangeHistoryPojo.setUuid(uuid);
            hfParamChangeHistoryPojo.setFileUuid(fileUuid);
            hfParamChangeHistoryPojo.setFileVersionId(fileVersionId);
            hfParamChangeHistoryPojo.setParamsUuid(paramsUuid);
            hfParamChangeHistoryPojo.setParamsChoose(paramsChoose);
            hfParamChangeHistoryPojo.setParamsTypeId(paramsTypeId);
            if ("40".equals(paramsTypeId) || "30".equals(paramsTypeId) || "10".equals(paramsTypeId) || "50".equals(paramsTypeId)) {
                hfParamChangeHistoryPojo.setParamsText(paramsText);
            } else if ("80".equals(paramsTypeId) || "90".equals(paramsTypeId)) {
                Map<String, Object> paramsTextList = (Map<String, Object>) (Map) docParamsPojo.getParamsTextList();
                paramsText = JSON.toJSONString(paramsTextList);
                hfParamChangeHistoryPojo.setParamsText(paramsText);
            } else if ("20".equals(paramsTypeId) || "60".equals(paramsTypeId) || "70".equals(paramsTypeId) || "95".equals(paramsTypeId)) {
                List<Map<String, String>> paramsTextList = (List<Map<String, String>>) (List) docParamsPojo.getParamsTextList();
                paramsText = JSON.toJSONString(paramsTextList);
                hfParamChangeHistoryPojo.setParamsText(paramsText);
            }
            hfParamChangeHistoryPojo.setNewParamsText(newParamsText);
            hfParamChangeHistoryPojo.setCreateUserId(docParamsPojo.getUserId());
            fileOperationMapper.addParamChangeHistoryDao(hfParamChangeHistoryPojo);
        } else {
            // 1.修改数据库忽略状态
            fileOperationMapper.updateContentParamIgnoreDao(fileUuid, fileVersionId, paramsUuid, docParamsPojo.getUserId(), isIgnore);

            // 2.修改文内数据状态
            // 保证每个独立的段落只更新一次
            ArrayList<DocParamsPojo> docParamsPojosTmp = new ArrayList<>();
//            docParamsPojosTmp.addAll(docParamsPojos);
            String fileUuid1 = "";
            String fileVersionId1 = "";
            String outlineId1 = "";
            String uuid1 = "";
            DocParamsPojo paramsPojo1 = new DocParamsPojo();
            String fileUuid2 = "";
            String fileVersionId2 = "";
            String outlineId2 = "";
            String uuid2 = "";
            DocParamsPojo paramsPojo2 = new DocParamsPojo();
            for (DocParamsPojo paramsPojo : docParamsPojos) {
                docParamsPojosTmp.add(paramsPojo);
                fileUuid1 = paramsPojo.getFileUuid();
                fileVersionId1 = paramsPojo.getFileVersionId();
                outlineId1 = paramsPojo.getOutlineId();
                uuid1 = paramsPojo.getUuid();
                paramsPojo1 = paramsPojo;
                if (fileUuid1.equals(fileUuid2) && fileVersionId1.equals(fileVersionId2) && outlineId1.equals(outlineId2)) {
                    paramsPojo.setUuid(uuid1 + "|" + uuid2);
                    docParamsPojosTmp.remove(paramsPojo2);
                }
                fileUuid2 = fileUuid1;
                fileVersionId2 = fileVersionId1;
                outlineId2 = outlineId1;
                uuid2 = paramsPojo.getUuid();
                paramsPojo2 = paramsPojo1;
            }

            // 将参数更新至content中
            for (DocParamsPojo paramsPojo : docParamsPojosTmp) {  // docParamsPojosTmp的size表示要更新的段落数量
                String contentText = paramsPojo.getContentText();

                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                Elements elements = doc.select("parameter[uuid~=(" + paramsPojo.getUuid() + ")]");  // paramsPojo.getUuid()是以|隔开的
                Elements elementsBlock = doc.select("parameterblock[uuid~=(" + paramsPojo.getUuid() + ")]");  // paramsPojo.getUuid()是以|隔开的
                Elements elementsTmp = new Elements();
                elementsTmp.addAll(elements);
                elementsTmp.addAll(elementsBlock);
                for (Element element : elementsTmp) {  // elementsTmp的size表示段落中要更新几处
                    element.attr("isIgnore", isIgnore);
                    Map<String, Object> map = new HashMap<>();
                    map.put("uuid", element.attr("uuid"));
                    newParamsTextList.add(map);
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                paramsPojo.setContentText(contentText);

                // 搜索模式下
                String searchUuid = docParamsPojo.getSearchUuid();
                String userId = docParamsPojo.getUserId();
                String outlineId = paramsPojo.getOutlineId();
                if (searchUuid != null) {
                    // 更新后入库前更新一下缓存
                    if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                        redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                    }
                    contentText = contentText.replaceAll("</br>", "");
                }
                fileOperationMapper.writeContentParamDao(paramsPojo);
            }
        }

        try {
            if (StringUtils.isNotBlank(docParamsPojo.getIsAiContent())) {
                fileOperationMapper.updateIsAiContentDao(fileUuid, fileVersionId, paramsUuid, docParamsPojo.getIsAiContent());
            }
        } catch (Exception ignored) {
        }

        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "填写完成");
        ret.put("transformList", newParamsTextList.size() == 0 ? newParamsText : newParamsTextList);
        return TResponseVo.success(ret);
    }

    /**
     * 参数填写大保存 (创建一个新的招标文件版本)
     *
     * @param docFileIndexPojo
     * @param session
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo saveWriteParamService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        String oriFileUuid = docFileIndexPojo.getFileUuid();
        String oriFileVersionId = docFileIndexPojo.getFileVersionId();
        String fileUuid1 = docFileIndexPojo.getFileUuid();
        String fileVersionId1 = docFileIndexPojo.getFileVersionId();
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid1);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid1 = docAllInfoDao.getMainFileUuid();
            fileVersionId1 = docAllInfoDao.getMainFileVersionId();
        }
        docFileIndexPojo.setFileUuid(fileUuid1);
        docFileIndexPojo.setFileVersionId(fileVersionId1);


        // 获取父文件的fileUuid fileVersionId
        String fileUuid = docFileIndexPojo.getFileUuid();
        DocFileVerIndexPojo draftVersion = fileOperationMapper.getDraftVersionIdDao(fileUuid);

        if (draftVersion == null) {
            // 如果没有预设草稿版本 则直接新建一个版本 但不新建草稿版本
            docFileIndexPojo.setDraftVersionId(UUID.randomUUID().toString().replaceAll("-", ""));
            try {
                docFileIndexPojo.setIsDraft("0");
                // 升级版本号
                DocFileIndexPojo versionAllInfoDao = fileOperationMapper.getVersionAllInfoDao(fileUuid);
                String fileVersionName = versionAllInfoDao.getFileVersionName();
                String newFileVersionName = new JsonKeyUtils().versionCount(fileVersionName, 1);
                docFileIndexPojo.setFileVersionName(fileVersionName);


                String fileVersionId = docFileIndexPojo.getFileVersionId();
                docFileIndexPojo.setFileParentId(fileUuid);
                docFileIndexPojo.setOldFileVersionId(fileVersionId);
                fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
//                fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                fileVersionId = docFileIndexPojo.getDraftVersionId();
                docFileIndexPojo.setFileUuid(fileUuid);
                docFileIndexPojo.setFileVersionId(fileVersionId);

                String userId = docFileIndexPojo.getCreateUserId();
                docFileIndexPojo.setCreateTime(new Date());

                try {
                    int j = 0;
                    int i = 0;
                    // 创建索引
//            不需要变更索引

                    // 创建版本
                    i = fileOperationMapper.addVersionFileVersionDao(docFileIndexPojo);
                    if (i == 0) {
                        FileIndexException fileIndexException = new FileIndexException();
                        fileIndexException.setCode(701);
                        throw fileIndexException;
                    }

                    // 创建大纲
                    List<OutLinePojo> newVersionFileOutlineDao = fileOperationMapper.getVersionFileOutlineDao(docFileIndexPojo);
                    // 设置groupId替换用的键值
                    HashMap<String, String> groupIdMap = new HashMap<>();
                    for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                        if (outLinePojo.getOutlineReplaceGroupId() == null) {
                            continue;
                        }
                        String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                        groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
                    }
                    String sql = "";
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String dateString = formatter.format(new Date());
                    for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                        j++;
                        // 重新排序
                        outLinePojo.setOutlineOrder(String.valueOf(j));
                        String outlineReplaceGroupId = null;
                        String color = null;
                        if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                            outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                        }
                        if (outLinePojo.getColor() != null) {
                            color = "\"" + outLinePojo.getColor() + "\"";
                        }
                        sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + docFileIndexPojo.getFileParentId() + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
//                        System.out.println("sql : " + sql);
                        fileOperationMapper.saveWriteParamDao(sql);
                    }
                    // 创建内容
                    List<ContentPojo> versionFileContentDao = fileOperationMapper.getVersionFileContentDao(docFileIndexPojo);
                    sql = "";
                    for (ContentPojo contentPojo : versionFileContentDao) {
                        String contentText = contentPojo.getContentText();
//                        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//                        Elements elements = doc.select("mark");
//                        Elements elementsTmp = new Elements();
//                        elementsTmp.addAll(elements);
//                        for (Element element : elementsTmp) {
//                            element.before(element.html());
//                            element.remove();  // 去除mark标签
//                        }
//                        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                        contentText = contentText.replaceAll("</br>", "");
//                        contentPojo.setContentText(contentText);
                        sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + docFileIndexPojo.getFileParentId() + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
                        fileOperationMapper.addVersionFileContentDao(sql);
                    }
                    // 创建参数
                    fileOperationMapper.addVersionContentParamDao2(docFileIndexPojo);

                    // 创建标注
                    fileOperationMapper.addVersionContentTagDao(docFileIndexPojo);

                    // 创建书签
                    fileOperationMapper.addVersionContentBookmarkDao(docFileIndexPojo);

                    // 创建数据管理(4张)
//                    fileOperationMapper.addVersionContentDmDbDao(docFileIndexPojo);
//                    fileOperationMapper.addVersionContentDmTableDao(docFileIndexPojo);
//                    fileOperationMapper.addVersionContentDmColumnsDao(docFileIndexPojo);
//                    splitTableMapper.addVersionContentDmDataDao(docFileIndexPojo);

                    // 创建数据表(如果有)
//                    fileOperationMapper.addVersionContentDbDao(docFileIndexPojo);

                    // 创建附属文件
                    // 查找附属
                    List<DocFileIndexPojo> allSubsidiaryFileListDao = fileOperationMapper.getAllSubsidiaryFileListDao(docFileIndexPojo);
                    for (DocFileIndexPojo fileIndexPojo : allSubsidiaryFileListDao) {
//                        System.out.println("allSubsidiaryFileListDao : " + allSubsidiaryFileListDao.size());
//                        System.out.println("docFileIndexPojo : " + JSON.toJSONString(docFileIndexPojo));
//                        System.out.println("fileIndexPojo : " + JSON.toJSONString(fileIndexPojo));
                        // 获取父文件的fileUuid fileVersionId
                        fileUuid = fileIndexPojo.getFileUuid();
                        fileVersionId = fileIndexPojo.getFileVersionId();
                        fileIndexPojo.setFileParentId(fileUuid);
                        fileIndexPojo.setOldFileVersionId(fileVersionId);
                        // 把主文件的fileUuid写到附属文件的mainFileUuid字段中
                        fileIndexPojo.setMainFileUuid(docFileIndexPojo.getFileParentId());
                        fileIndexPojo.setMainFileVersionId(docFileIndexPojo.getFileVersionId());
                        userId = docFileIndexPojo.getCreateUserId();
                        fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                        fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                        fileIndexPojo.setFileUuid(fileUuid);
                        fileIndexPojo.setFileVersionId(fileVersionId);

                        fileIndexPojo.setCreateTime(new Date());

//                            System.out.println("fileIndexPojo : " + JSON.toJSONString(fileIndexPojo));
                        j = 0;
                        i = 0;
                        // 创建索引
                        i = fileOperationMapper.addDeriveFileIndexDao1(fileIndexPojo);
                        if (i == 0) {
                            FileIndexException fileIndexException = new FileIndexException();
                            fileIndexException.setCode(700);
                            throw fileIndexException;
                        }

                        // 创建版本
                        i = fileOperationMapper.addDeriveFileVersionDao(fileIndexPojo);
                        if (i == 0) {
                            FileIndexException fileIndexException = new FileIndexException();
                            fileIndexException.setCode(701);
                            throw fileIndexException;
                        }

                        // 创建大纲
                        newVersionFileOutlineDao = fileOperationMapper.getDeriveFileOutlineDao(fileIndexPojo);
                        // 设置groupId替换用的键值
                        groupIdMap = new HashMap<>();
                        for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                            if (outLinePojo.getOutlineReplaceGroupId() == null) {
                                continue;
                            }
                            String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                            groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
                        }
                        sql = "";
                        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        dateString = formatter.format(new Date());
                        for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                            j++;
                            // 重新排序
                            outLinePojo.setOutlineOrder(String.valueOf(j));
                            String outlineReplaceGroupId = null;
                            String color = null;
                            if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                                outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                            }
                            if (outLinePojo.getColor() != null) {
                                color = "\"" + outLinePojo.getColor() + "\"";
                            }
                            sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                        }
                        if (sql.length() != 0) {
                            sql = sql.substring(0, sql.length() - 1);
//                            System.out.println("sql : " + sql);
                            fileOperationMapper.addDeriveFileOutlineDao(sql);
                        }
                        // 创建内容
                        versionFileContentDao = fileOperationMapper.getDeriveFileContentDao(fileIndexPojo);
                        sql = "";
                        for (ContentPojo contentPojo : versionFileContentDao) {
                            String contentText = contentPojo.getContentText();
//                            Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//                            Elements elements = doc.select("mark");
//                            Elements elementsTmp = new Elements();
//                            elementsTmp.addAll(elements);
//                            for (Element element : elementsTmp) {
//                                element.before(element.html());
//                                element.remove();  // 去除mark标签
//                            }
//                            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                            contentText = contentText.replaceAll("</br>", "");
//                            contentPojo.setContentText(contentText);
                            sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                        }
                        if (sql.length() != 0) {
                            sql = sql.substring(0, sql.length() - 1);
                            fileOperationMapper.addDeriveFileContentDao(sql);
                        }
                        // 创建参数
                        fileOperationMapper.addDeriveContentParamDao2(fileIndexPojo);

                        // 创建标注
                        fileOperationMapper.addDeriveContentTagDao(fileIndexPojo);

                        // 创建书签
                        fileOperationMapper.addDeriveContentBookmarkDao(fileIndexPojo);

                        // 创建数据管理(4张)
//                        fileOperationMapper.addDeriveContentDmDbDao(fileIndexPojo);
//                        fileOperationMapper.addDeriveContentDmTableDao(fileIndexPojo);
//                        fileOperationMapper.addDeriveContentDmColumnsDao(fileIndexPojo);
//                        splitTableMapper.addDeriveContentDmDataDao(fileIndexPojo);

                        // 创建数据表(如果有)
//                        fileOperationMapper.addDeriveContentDbDao(fileIndexPojo);
                    }
                } catch (FileIndexException e) {
                    throw e;
                } catch (Exception e) {
                    e.printStackTrace();
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(704);
                    throw fileIndexException;
                }

                docFileIndexPojo.setFileTypeId("Z");
                docFileIndexPojo.setFileTypeName("招标文件");
//                    int i = 0;
//                    // 更新索引 newFileVersionName
//                    i = fileOperationMapper.updateVersionFileIndexDao(docFileIndexPojo);
//                    if (i == 0) {
//                        FileIndexException fileIndexException = new FileIndexException();
//                        fileIndexException.setCode(700);
//                        throw fileIndexException;
//                    }
                fileOperationMapper.updateFileVersionNameDao(docFileIndexPojo.getFileParentId(), docFileIndexPojo.getOldFileVersionId(), newFileVersionName);
            } catch (FileIndexException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(704);
                throw fileIndexException;
            }
        } else {
            // 如果已经存在草稿 则把该草稿转正 然后新建一个新的草稿
            String draftVersionIdDao = draftVersion.getFileVersionId();
            try {
                docFileIndexPojo.setIsDraft("1");
                // 升级版本号
                String fileVersionName = draftVersion.getFileVersionName();
                String newFileVersionName = new JsonKeyUtils().versionCount(fileVersionName, 1);
                docFileIndexPojo.setFileVersionName(newFileVersionName);
                docFileIndexPojo.setFileVersionId(draftVersion.getFileVersionId());


                String fileVersionId = docFileIndexPojo.getFileVersionId();
                docFileIndexPojo.setFileParentId(fileUuid);
                docFileIndexPojo.setOldFileVersionId(fileVersionId);
                fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                docFileIndexPojo.setFileUuid(fileUuid);
                docFileIndexPojo.setFileVersionId(fileVersionId);

                String userId = docFileIndexPojo.getCreateUserId();
                docFileIndexPojo.setCreateTime(new Date());

                try {
                    int j = 0;
                    int i = 0;
                    // 创建索引
//            不需要变更索引

                    // 创建版本
                    i = fileOperationMapper.addVersionFileVersionDao(docFileIndexPojo);
                    if (i == 0) {
                        FileIndexException fileIndexException = new FileIndexException();
                        fileIndexException.setCode(701);
                        throw fileIndexException;
                    }

                    // 创建大纲
                    List<OutLinePojo> newVersionFileOutlineDao = fileOperationMapper.getVersionFileOutlineDao(docFileIndexPojo);
                    // 设置groupId替换用的键值
                    HashMap<String, String> groupIdMap = new HashMap<>();
                    for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                        if (outLinePojo.getOutlineReplaceGroupId() == null) {
                            continue;
                        }
                        String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                        groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
                    }
                    String sql = "";
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String dateString = formatter.format(new Date());
                    for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                        j++;
                        // 重新排序
                        outLinePojo.setOutlineOrder(String.valueOf(j));
                        String outlineReplaceGroupId = null;
                        String color = null;
                        if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                            outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                        }
                        if (outLinePojo.getColor() != null) {
                            color = "\"" + outLinePojo.getColor() + "\"";
                        }
                        sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + docFileIndexPojo.getFileParentId() + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
//                        System.out.println("sql : " + sql);
                        fileOperationMapper.saveWriteParamDao(sql);
                    }
                    // 创建内容
                    List<ContentPojo> versionFileContentDao = fileOperationMapper.getVersionFileContentDao(docFileIndexPojo);
                    sql = "";
                    for (ContentPojo contentPojo : versionFileContentDao) {
                        String contentText = contentPojo.getContentText();
//                        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//                        Elements elements = doc.select("mark");
//                        Elements elementsTmp = new Elements();
//                        elementsTmp.addAll(elements);
//                        for (Element element : elementsTmp) {
//                            element.before(element.html());
//                            element.remove();  // 去除mark标签
//                        }
//                        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                        contentText = contentText.replaceAll("</br>", "");
//                        contentPojo.setContentText(contentText);
                        sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + docFileIndexPojo.getFileParentId() + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
                        fileOperationMapper.addVersionFileContentDao(sql);
                    }
                    // 创建参数
                    fileOperationMapper.addVersionContentParamDao2(docFileIndexPojo);

                    // 创建标注
                    fileOperationMapper.addVersionContentTagDao(docFileIndexPojo);

                    // 创建书签
                    fileOperationMapper.addVersionContentBookmarkDao(docFileIndexPojo);

//                    // 创建数据管理(4张)
//                    fileOperationMapper.addVersionContentDmDbDao(docFileIndexPojo);
//                    fileOperationMapper.addVersionContentDmTableDao(docFileIndexPojo);
//                    fileOperationMapper.addVersionContentDmColumnsDao(docFileIndexPojo);
//                    splitTableMapper.addVersionContentDmDataDao(docFileIndexPojo);
//
//                    // 创建数据表(如果有)
//                    fileOperationMapper.addVersionContentDbDao(docFileIndexPojo);
                    // 创建附属文件
                    // 查找附属
                    List<DocFileIndexPojo> allSubsidiaryFileListDao = fileOperationMapper.getAllSubsidiaryFileListDao(docFileIndexPojo);
                    for (DocFileIndexPojo fileIndexPojo : allSubsidiaryFileListDao) {
//                        System.out.println("allSubsidiaryFileListDao : " + allSubsidiaryFileListDao.size());
//                        System.out.println("docFileIndexPojo : " + JSON.toJSONString(docFileIndexPojo));
//                        System.out.println("fileIndexPojo : " + JSON.toJSONString(fileIndexPojo));
                        // 获取父文件的fileUuid fileVersionId
                        fileUuid = fileIndexPojo.getFileUuid();
                        fileVersionId = fileIndexPojo.getFileVersionId();
                        fileIndexPojo.setFileParentId(fileUuid);
                        fileIndexPojo.setOldFileVersionId(fileVersionId);
                        // 把主文件的fileUuid写到附属文件的mainFileUuid字段中
                        fileIndexPojo.setMainFileUuid(docFileIndexPojo.getFileParentId());
                        fileIndexPojo.setMainFileVersionId(docFileIndexPojo.getFileVersionId());
                        userId = docFileIndexPojo.getCreateUserId();
                        fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                        fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                        fileIndexPojo.setFileUuid(fileUuid);
                        fileIndexPojo.setFileVersionId(fileVersionId);

                        fileIndexPojo.setCreateTime(new Date());

                        j = 0;
                        i = 0;
                        // 创建索引
                        i = fileOperationMapper.addDeriveFileIndexDao1(fileIndexPojo);
                        if (i == 0) {
                            FileIndexException fileIndexException = new FileIndexException();
                            fileIndexException.setCode(700);
                            throw fileIndexException;
                        }

                        // 创建版本
                        i = fileOperationMapper.addDeriveFileVersionDao(fileIndexPojo);
                        if (i == 0) {
                            FileIndexException fileIndexException = new FileIndexException();
                            fileIndexException.setCode(701);
                            throw fileIndexException;
                        }

                        // 创建大纲
                        newVersionFileOutlineDao = fileOperationMapper.getDeriveFileOutlineDao(fileIndexPojo);
                        // 设置groupId替换用的键值
                        groupIdMap = new HashMap<>();
                        for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                            if (outLinePojo.getOutlineReplaceGroupId() == null) {
                                continue;
                            }
                            String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                            groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
                        }
                        sql = "";
                        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        dateString = formatter.format(new Date());
                        for (OutLinePojo outLinePojo : newVersionFileOutlineDao) {
                            j++;
                            // 重新排序
                            outLinePojo.setOutlineOrder(String.valueOf(j));
                            String outlineReplaceGroupId = null;
                            String color = null;
                            if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                                outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                            }
                            if (outLinePojo.getColor() != null) {
                                color = "\"" + outLinePojo.getColor() + "\"";
                            }
                            sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                        }
                        if (sql.length() != 0) {
                            sql = sql.substring(0, sql.length() - 1);
//                            System.out.println("sql : " + sql);
                            fileOperationMapper.addDeriveFileOutlineDao(sql);
                        }
                        // 创建内容
                        versionFileContentDao = fileOperationMapper.getDeriveFileContentDao(fileIndexPojo);
                        sql = "";
                        for (ContentPojo contentPojo : versionFileContentDao) {
                            String contentText = contentPojo.getContentText();
//                            Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//                            Elements elements = doc.select("mark");
//                            Elements elementsTmp = new Elements();
//                            elementsTmp.addAll(elements);
//                            for (Element element : elementsTmp) {
//                                element.before(element.html());
//                                element.remove();  // 去除mark标签
//                            }
//                            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                            contentText = contentText.replaceAll("</br>", "");
//                            contentPojo.setContentText(contentText);
                            sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                        }
                        if (sql.length() != 0) {
                            sql = sql.substring(0, sql.length() - 1);
                            fileOperationMapper.addDeriveFileContentDao(sql);
                        }
                        // 创建参数
                        fileOperationMapper.addDeriveContentParamDao2(fileIndexPojo);

                        // 创建标注
                        fileOperationMapper.addDeriveContentTagDao(fileIndexPojo);

                        // 创建书签
                        fileOperationMapper.addDeriveContentBookmarkDao(fileIndexPojo);

//                        // 创建数据管理(4张)
//                        fileOperationMapper.addDeriveContentDmDbDao(fileIndexPojo);
//                        fileOperationMapper.addDeriveContentDmTableDao(fileIndexPojo);
//                        fileOperationMapper.addDeriveContentDmColumnsDao(fileIndexPojo);
//                        splitTableMapper.addDeriveContentDmDataDao(fileIndexPojo);
//
//                        // 创建数据表(如果有)
//                        fileOperationMapper.addDeriveContentDbDao(fileIndexPojo);
                    }
                } catch (FileIndexException e) {
                    throw e;
                } catch (Exception e) {
                    e.printStackTrace();
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(704);
                    throw fileIndexException;
                }


                docFileIndexPojo.setDraftVersionId(draftVersionIdDao);
//                    int i = 0;
//                    // 更新索引
//                    i = fileOperationMapper.updateVersionFileIndexDao(docFileIndexPojo);
//                    if (i == 0) {
//                        FileIndexException fileIndexException = new FileIndexException();
//                        fileIndexException.setCode(700);
//                        throw fileIndexException;
//                    }
                docFileIndexPojo.setCreateTime(new Date());
                fileOperationMapper.updateVersionFileIndexDao1(docFileIndexPojo);
            } catch (FileIndexException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(704);
                throw fileIndexException;
            }
        }

        // 提交时 同步所有参数到关联文件
//        DocParamsPojo paramsPojo = new DocParamsPojo();
//        paramsPojo.setFileUuid(oriFileUuid);
//        paramsPojo.setFileVersionId(oriFileVersionId);
//        paramsPojo.setType("all");
//        List<DocParamsPojo> contentParamDao = fileOperationMapper.getContentParamDao(paramsPojo);
//        ArrayList<DocParamsPojo> contentParamDaoTmp = new ArrayList<>();
//        ArrayList<String> paramUuidList = new ArrayList<>();
//        for (DocParamsPojo docParamsPojo : contentParamDao) {  // 参数分组打包
//            if (!paramUuidList.contains(docParamsPojo.getParamsUuid())) {
//                paramUuidList.add(docParamsPojo.getParamsUuid());
//                // 分类
//                if ("40".equals(docParamsPojo.getParamsTypeId()) || "30".equals(docParamsPojo.getParamsTypeId())) { // 文本 时间
//                } else if ("10".equals(docParamsPojo.getParamsTypeId())) {  // 单选
//                } else if ("20".equals(docParamsPojo.getParamsTypeId())) {  // 多选
//                    if (docParamsPojo.getParamsText() != null && StringUtils.isNotBlank(String.valueOf(docParamsPojo.getParamsText()))) {
//                        docParamsPojo.setParamsTextList(new JsonKeyUtils().stringToList(String.valueOf(docParamsPojo.getParamsText())));
//                    } else {
//                        docParamsPojo.setParamsTextList(new ArrayList<>());
//                    }
//                } else if ("80".equals(docParamsPojo.getParamsTypeId())) {  // 金额
//                    if (docParamsPojo.getParamsText() != null) {
//                        HashMap<String, Double> map = new HashMap<>();
//                        map.put(docParamsPojo.getUnit(),new JsonKeyUtils().str2Dou(String.valueOf(docParamsPojo.getParamsText())));
//                        docParamsPojo.setParamsTextList(map);
//                    } else {
//                        docParamsPojo.setParamsTextList(new HashMap<>());
//                    }
//                } else if ("90".equals(docParamsPojo.getParamsTypeId())) {  // 数值
//                    if (docParamsPojo.getParamsText() != null) {
//                        HashMap<String, Double> map = new HashMap<>();
//                        map.put(docParamsPojo.getUnit(),Double.parseDouble(String.valueOf(docParamsPojo.getParamsText())));
//                        docParamsPojo.setParamsTextList(map);
//                    } else {
//                        docParamsPojo.setParamsTextList(new HashMap<>());
//                    }
//                } else if ("70".equals(docParamsPojo.getParamsTypeId())) {  // 附件
//                    if (docParamsPojo.getParamsText() != null) {
//                        List list = JSON.parseObject(String.valueOf(docParamsPojo.getParamsText()), List.class);
//                        docParamsPojo.setParamsTextList(list);
//                    } else {
//                        docParamsPojo.setParamsTextList(new ArrayList<>());
//                    }
//                } else if ("60".equals(docParamsPojo.getParamsTypeId())) {  // 图片
//                    if (docParamsPojo.getParamsText() != null) {
//                        List list = JSON.parseObject(String.valueOf(docParamsPojo.getParamsText()), List.class);
//                        docParamsPojo.setParamsTextList(list);
//                    } else {
//                        docParamsPojo.setParamsTextList(new ArrayList<>());
//                    }
//                } else if ("50".equals(docParamsPojo.getParamsTypeId())) {  // 富文本
//
//                } else if ("95".equals(docParamsPojo.getParamsTypeId())) {  // 矩阵
//                    if (docParamsPojo.getParamsText() != null) {
//                        List list = JSON.parseObject(String.valueOf(docParamsPojo.getParamsText()), List.class);
//                        docParamsPojo.setParamsTextList(list);
//                    } else {
//                        docParamsPojo.setParamsTextList(new ArrayList<>());
//                    }
//                }
//                contentParamDaoTmp.add(docParamsPojo);
//            } else {
//                // 分类
//                if ("80".equals(docParamsPojo.getParamsTypeId())) {  // 金额
//                    for (DocParamsPojo pojo : contentParamDaoTmp) {
//                        if (pojo.getParamsUuid().equals(docParamsPojo.getParamsUuid())) {
//                            if (docParamsPojo.getParamsText() != null) {
//                                Map<String,Double> paramsTextList = (Map) pojo.getParamsTextList();
//                                paramsTextList.put(docParamsPojo.getUnit(),Double.parseDouble(String.valueOf(docParamsPojo.getParamsText())));
//                            }
//                            break;
//                        }
//                    }
//                } else if ("90".equals(docParamsPojo.getParamsTypeId())) {  // 数值
//                    for (DocParamsPojo pojo : contentParamDaoTmp) {
//                        if (pojo.getParamsUuid().equals(docParamsPojo.getParamsUuid())) {
//                            if (docParamsPojo.getParamsText() != null) {
//                                Map<String,Double> paramsTextList = (Map) pojo.getParamsTextList();
//                                paramsTextList.put(docParamsPojo.getUnit(),Double.parseDouble(String.valueOf(docParamsPojo.getParamsText())));
//                            }
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//        for (DocParamsPojo docParamsPojo : contentParamDaoTmp) {
//            docParamsPojo.setGetAllFlag(true);
//            writeParamService(docParamsPojo);
//        }


        // 判断是否是提交操作
        String finalFlag = docFileIndexPojo.getFinalFlag();
        if (finalFlag != null) {
            fileOperationMapper.updateVersionFileFinishDao(docFileIndexPojo.getFileParentId(), docFileIndexPojo.getOldFileVersionId());
            // 查找附属
            List<DocFileIndexPojo> allSubsidiaryFileListDao = fileOperationMapper.getAllSubsidiaryFileListDao3(docFileIndexPojo.getFileParentId(), docFileIndexPojo.getOldFileVersionId());
            for (DocFileIndexPojo fileIndexPojo : allSubsidiaryFileListDao) {
                fileOperationMapper.updateVersionFileFinishDao(fileIndexPojo.getFileUuid(), fileIndexPojo.getFileVersionId());
            }
        }

        Map<String, String> ret = new HashMap<>();
        ret.put("fileUuid", docFileIndexPojo.getFileParentId());
        ret.put("fileVersionId", docFileIndexPojo.getFileVersionId());
        ret.put("info", "新版本创建成功");
        return TResponseVo.success(ret);

    }

    /**
     * 参数还原
     *
     * @param docParamsPojo
     * @param session
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo resetParamService(DocParamsPojo docParamsPojo, HttpSession session) throws Exception {
        String fileUuid = docParamsPojo.getFileUuid();
        String fileVersionId = docParamsPojo.getFileVersionId();
        String paramsUuid = docParamsPojo.getParamsUuid();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(paramsUuid)) {
            return TResponseVo.error(ResponseEnum.ERROR, "必填参数为空");
        }

        // 找到整个文件组的所有这个参数
        // 根据`file_uuid`,`file_version_id`,`params_uuid`就能确定文件组内所有该参数位置
        List<DocParamsPojo> docParamsPojos = fileOperationMapper.getContentAllParamInfoDao(docParamsPojo);

        String sql = "";
        for (DocParamsPojo paramsPojo : docParamsPojos) {
            sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
        }
        if (sql.length() != 0) {
            sql = sql.substring(0, sql.length() - 3);
            fileOperationMapper.resetParamDao(docParamsPojo, sql);
        }
        return TResponseVo.success("还原成功");
    }

    /**
     * 删除参数附件
     *
     * @param fileUuid
     * @param fileVersionId
     * @param paramsUuid
     * @param uid
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo deleteAnnexParamService(String fileUuid, String fileVersionId, String paramsUuid, String uid, String writeUserId) throws Exception {
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(paramsUuid) || StringUtils.isBlank(uid)) {
            return TResponseVo.error(ResponseEnum.ERROR, "必填参数为空");
        }

        DocParamsPojo docParamsPojo = new DocParamsPojo();
        docParamsPojo.setFileUuid(fileUuid);
        docParamsPojo.setFileVersionId(fileVersionId);
        docParamsPojo.setParamsUuid(paramsUuid);
        // 找到整个文件组的所有这个参数
        // 根据`file_uuid`,`file_version_id`,`params_uuid`就能确定文件组内所有该参数位置
        String typeId = null;
        List<DocParamsPojo> docParamsPojos = fileOperationMapper.getContentAllParamDao(docParamsPojo);
        if (docParamsPojos != null && docParamsPojos.size() != 0) {
            typeId = docParamsPojos.get(0).getParamsTypeId();
        }

        // 获取原答案
        List<Map<String, String>> listTmp = new ArrayList<>();
        for (DocParamsPojo paramsPojo : docParamsPojos) {
            String paramsText = String.valueOf(paramsPojo.getParamsText());
            List<Map<String, String>> list = JSON.parseObject(paramsText, List.class);
            listTmp.addAll(list);
            for (Map<String, String> stringStringMap : list) {
                if (stringStringMap.get("uid").equals(uid)) {
                    listTmp.remove(stringStringMap);
                    break;
                }
            }
            break;
        }
        String paramsText = JSON.toJSONString(listTmp);
        String sql = "";
        for (DocParamsPojo paramsPojo : docParamsPojos) {
            sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
        }
        Integer integer = 0;
        if (sql.length() != 0) {
            sql = sql.substring(0, sql.length() - 3);
            integer = fileOperationMapper.writeParamDao(paramsText, writeUserId, sql);
        }
        // 第二步 更新文内参数
        if (integer != 0) {  // 说明触发了更新 不然则未触发更新
            // 保证每个独立的段落只更新一次
            ArrayList<DocParamsPojo> docParamsPojosTmp = new ArrayList<>();
//            docParamsPojosTmp.addAll(docParamsPojos);
            String fileUuid1 = "";
            String fileVersionId1 = "";
            String outlineId1 = "";
            String uuid1 = "";
            DocParamsPojo paramsPojo1 = new DocParamsPojo();
            String fileUuid2 = "";
            String fileVersionId2 = "";
            String outlineId2 = "";
            String uuid2 = "";
            DocParamsPojo paramsPojo2 = new DocParamsPojo();
            for (DocParamsPojo paramsPojo : docParamsPojos) {
                docParamsPojosTmp.add(paramsPojo);
                fileUuid1 = paramsPojo.getFileUuid();
                fileVersionId1 = paramsPojo.getFileVersionId();
                outlineId1 = paramsPojo.getOutlineId();
                uuid1 = paramsPojo.getUuid();
                paramsPojo1 = paramsPojo;
                if (fileUuid1.equals(fileUuid2) && fileVersionId1.equals(fileVersionId2) && outlineId1.equals(outlineId2)) {
                    paramsPojo.setUuid(uuid1 + "|" + uuid2);
                    docParamsPojosTmp.remove(paramsPojo2);
                }
                fileUuid2 = fileUuid1;
                fileVersionId2 = fileVersionId1;
                outlineId2 = outlineId1;
                uuid2 = paramsPojo.getUuid();
                paramsPojo2 = paramsPojo1;
            }
//            System.out.println("docParamsPojosTmp : " + JSON.toJSONString(docParamsPojosTmp));

            // 将参数更新至content中
            for (DocParamsPojo paramsPojo : docParamsPojosTmp) {  // docParamsPojosTmp的size表示要更新的段落数量
                String contentText = paramsPojo.getContentText();
                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                Elements elements = doc.select("parameter[uuid~=(" + paramsPojo.getUuid() + ")]");
                Elements elementsBlock = doc.select("parameterblock[key=" + paramsUuid + "]");
                elements.addAll(elementsBlock);
                Elements elementsTmp = new Elements();
                elementsTmp.addAll(elements);
                for (Element element : elementsTmp) {  // elementsTmp的size表示段落中要更新几处
                    String newParamsText = "";
                    for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                        if (element.attr("uuid").equals(pojo.getUuid()) && pojo.getFileUuid().equals(fileUuid) && pojo.getFileVersionId().equals(fileVersionId)) {
                            String styleId = pojo.getStyleId();
                            // 根据styleId来判断显示样式
                            if (styleId == null) {  // 无格式默认
                                newParamsText = paramsText;
                                break;
                            } else if (styleId.equals("632041")) {  // 附件参数
                                newParamsText = paramsText;
                                break;
                            } else if (styleId.equals("632040")) {  // 图片参数蹭车
                                newParamsText = paramsText;
                                break;
                            }
                        }
                    }
                    if (newParamsText == null) {
                        element.removeAttr("value");
                    } else {
                        element.attr("value", encodeURI(newParamsText));
                        element.attr("typeId", typeId);
                    }
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                paramsPojo.setContentText(contentText);

                // 搜索模式下
                String searchUuid = docParamsPojo.getSearchUuid();
                String userId = docParamsPojo.getUserId();
                String outlineId = paramsPojo.getOutlineId();
                if (searchUuid != null) {
                    // 更新后入库前更新一下缓存
                    if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                        redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                    }

                    doc = Jsoup.parse(contentText, Parser.xmlParser());
                    doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                    elements = doc.select("mark");
                    for (Element element : elements) {
                        element.before(element.html());
                        element.remove();  // 去除mark标签
                    }
                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                    contentText = contentText.replaceAll("</br>", "");
                    paramsPojo.setContentText(contentText);
                }
//                System.out.println("paramsPojo :" + paramsPojo);
                fileOperationMapper.writeContentParamDao(paramsPojo);
            }
        }
        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "附件删除完成");
        return TResponseVo.success(ret);
    }

    /**
     * 招标文件-左侧项目信息
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo getBidLeftInfoService(String fileUuid) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
        }
        HfBidDocumentInfo leftProjectInfoDao = fileOperationMapper.getBidLeftInfoDao(fileUuid);
        Map<String, Object> map = JSON.parseObject(JSON.toJSONString(leftProjectInfoDao), Map.class);
        // 采购类别
        if (map.get("biddingType") == null) {
            map.put("biddingType", new ArrayList<>());
        } else {
            String biddingTypeStr = String.valueOf(map.get("biddingType"));
            List<String> biddingType = new JsonKeyUtils().stringToList(biddingTypeStr);
            map.put("biddingType", biddingType);
        }
        // 采购方式
        if (map.get("biddingStyle") == null) {
            map.put("biddingStyle", new ArrayList<>());
        } else {
            String biddingStyleStr = String.valueOf(map.get("biddingStyle"));
            List<String> biddingStyle = new JsonKeyUtils().stringToList(biddingStyleStr);
            map.put("biddingStyle", biddingStyle);
        }
        // 评标办法
        if (map.get("judgmentMethod") == null) {
            map.put("judgmentMethod", new ArrayList<>());
        } else {
            String judgmentMethodStr = String.valueOf(map.get("judgmentMethod"));
            List<String> judgmentMethod = new JsonKeyUtils().stringToList(judgmentMethodStr);
            map.put("judgmentMethod", judgmentMethod);
        }
        return TResponseVo.success(map);

    }

    /**
     * 招标文件-左侧项目信息更新
     *
     * @param docFileIndexPojo
     */
    @Override
    public TResponseVo updateBidLeftInfoService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        // 获取招标要素
        List<String> biddingType = (List) docFileIndexPojo.getBiddingType();// 采购类别
        List<String> biddingStyle = (List) docFileIndexPojo.getBiddingStyle();// 采购方式
        List<String> judgmentMethod = (List) docFileIndexPojo.getJudgmentMethod();// 评标办法
        String biddingTypeStr = new JsonKeyUtils().listToString(biddingType);
        String biddingStyleStr = new JsonKeyUtils().listToString(biddingStyle);
        String judgmentMethodStr = new JsonKeyUtils().listToString(judgmentMethod);
        fileOperationMapper.updateBidLeftInfoDao(fileUuid, biddingTypeStr, biddingStyleStr, judgmentMethodStr);
        Map<String, String> ret = new HashMap<>();
        ret.put("info", "更新完成");
        return TResponseVo.success(ret);
    }


    /**
     * 购买并生成招标文件秘钥
     *
     * @param fileUuid
     * @param companyId
     */
    @Override
    public TResponseVo getBidKeyService(String fileUuid, String userId, String companyId) throws Exception {
        // 要实现投标的公司和项目之间的绑定
        // 判断是否是公司人员
        if (companyId == null) {
            return TResponseVo.error(ResponseEnum.ERROR, "请加入公司后再尝试");
        }
        // 先判断fileUuid是否已经制作完成
        DocFileIndexPojo versionAllInfoDao = fileOperationMapper.getVersionAllInfoDao(fileUuid);
        if (versionAllInfoDao == null || !versionAllInfoDao.getIsFinish()) {
            return TResponseVo.error(ResponseEnum.ERROR, "无效的识别码，请检查输入是否正确");
        }
        // 根据招标文件获取项目信息
        HfBidDocumentInfo hfBidDocumentInfo = fileOperationMapper.getbidDocumentInfoDao(fileUuid);
        if (hfBidDocumentInfo == null) {
            return TResponseVo.error(ResponseEnum.ERROR, "未找到对应项目信息，请检查识别码");
        }
        String projectId = hfBidDocumentInfo.getProjectId();
        if (projectId == null) {
            return TResponseVo.error(ResponseEnum.ERROR, "未获取到项目信息，请检查识别码");
        }
        // 先查询数据库中是否已经有记录
        String bidKey = fileOperationMapper.getBidKeyDao(projectId, companyId);
        if (bidKey == null) {
            // 根据当前公司id和项目id计算出固定的4位秘钥
            String pc = projectId + companyId;
            bidKey = DigestUtils.md5DigestAsHex(pc.getBytes()).substring(0, 4);
            // 记录到数据库中
            fileOperationMapper.addBidKeyDao(projectId, companyId, bidKey, userId);
        }
        Map<String, String> ret = new HashMap<>();
        ret.put("info", "获取秘钥");
        ret.put("key", bidKey);
        return TResponseVo.success(ret);
    }

    /**
     * 接口读取的摘编入库 用来对比法律法规变化
     */
    @Override
    public String getLawExtractTextListsService(Integer pageSize) throws Exception {
        String lawExtractTextListStr = "";
        try {
            JSONObject params = new JSONObject();
            if (pageSize == null) {
//                    params.put("pageSize", pageSize);
                params.put("pageSize", 10000);
            } else {
                params.put("pageSize", pageSize);
            }
            lawExtractTextListStr = new HttpClient().doPostJson(dfbUrl + "/bgweb/law/getLawExtractTextLists", params);

        } catch (Exception e) {
            throw new WorkTableException("未请求到摘编库数据");
        }
        return lawExtractTextListStr;
    }

    /**
     * 对比法律法规变化
     *
     * @param lawExtractTextListStr
     */
    @Override
    public void compareLawExtractTextService(String lawExtractTextListStr) throws Exception {
        // lawExtractTextListStr是新请求到的法律法规数据
        if (StringUtils.isBlank(lawExtractTextListStr)) {

        } else {
            // 解析接口摘编内容
            Map<String, Object> map = JSON.parseObject(lawExtractTextListStr, Map.class);
            Map<String, Object> data = (Map) map.get("data");
            List<Map> list = (List) data.get("list");  // 摘编清单
            ArrayList<HfLawPojo> hfLawPojos = new ArrayList<>();  // 所有条款
            for (Map lawMap : list) {
                String textId = String.valueOf(lawMap.get("textId"));
                List<Map<String, String>> items = (List) lawMap.get("items");
                for (Map<String, String> item : items) {
                    HfLawPojo hfLawPojo = new HfLawPojo();
                    hfLawPojo.setTextId(textId);
                    hfLawPojo.setTextContent(String.valueOf(lawMap.get("textContent")));
                    hfLawPojo.setLawId(item.get("lawId"));
                    hfLawPojo.setLawName(item.get("lawName"));
                    hfLawPojo.setParagraphTitle(item.get("paragraphTitle"));
                    hfLawPojo.setParagraphContent(item.get("paragraphContent"));
                    if (item.get("paragraphContent") == null) continue;
                    hfLawPojo.setTextitemId(item.get("textitemId"));
                    hfLawPojo.setGetTime(new TimeFormatUtils().getDateByString(item.get("updateTime")));
                    hfLawPojos.add(hfLawPojo);
                }
            }

            // 从库中读取所有条款
            List<HfLawPojo> lawList = fileOperationMapper.getLawDao();
            if (lawList.size() == 0) {
                // 初始化法律法规库
                if (hfLawPojos.size() != 0) {
                    fileOperationMapper.insertLawDao(hfLawPojos);
                }

            } else {
                // 记录结果
                Date getTime = new Date();  // 校验时间
                String groupId = UUID.randomUUID().toString().replaceAll("-", "");  // 校验批次
                ArrayList<HfLawPojo> addList = new ArrayList<>();  // 新增条款
                ArrayList<HfLawPojo> delList = new ArrayList<>();  // 删除条款
                ArrayList<HfLawPojo> chgListNew = new ArrayList<>();  // 变更条款
                ArrayList<HfLawPojo> chgListOld = new ArrayList<>();  // 变更条款

                for (HfLawPojo lawPojoDB : lawList) {
                    Boolean flag = false;  // 匹配到就是true
                    for (HfLawPojo lawPojoIF : hfLawPojos) {
                        if (lawPojoDB.getTextitemId().equals(lawPojoIF.getTextitemId())) { // 匹配到
                            flag = true;
                            // 判断内容是否更新
                            if (!lawPojoDB.getParagraphContent().equals(lawPojoIF.getParagraphContent())) {
                                // 内容不相等 说明发生了变更
                                lawPojoIF.setGetTime(getTime);
                                lawPojoIF.setUpdateId(UUID.randomUUID().toString().replaceAll("-", ""));
                                lawPojoIF.setUpdateType("2");
                                lawPojoIF.setGroupId(groupId);
                                chgListNew.add(lawPojoIF);
                                lawPojoDB.setGetTime(getTime);
                                lawPojoDB.setUpdateId(UUID.randomUUID().toString().replaceAll("-", ""));
                                lawPojoDB.setUpdateType("2");
                                lawPojoDB.setGroupId(groupId);
//                                lawPojoDB.setGetTime(new Date());
                                chgListOld.add(lawPojoDB);
                            }
                        }
                    }
                    // 如果没有匹配到
                    if (!flag) {  // 说明被删除
                        lawPojoDB.setGetTime(getTime);
                        lawPojoDB.setUpdateId(UUID.randomUUID().toString().replaceAll("-", ""));
                        lawPojoDB.setUpdateType("3");
                        lawPojoDB.setGroupId(groupId);
//                        lawPojoDB.setGetTime(new Date());
                        delList.add(lawPojoDB);
                    }
                }

                for (HfLawPojo lawPojoIF : hfLawPojos) {
                    Boolean flag = false;  // 匹配到就是true
                    for (HfLawPojo lawPojoDB : lawList) {
                        if (lawPojoDB.getTextitemId().equals(lawPojoIF.getTextitemId())) { // 匹配到
                            flag = true;
                        }
                    }
                    // 如果没有匹配到
                    if (!flag) {  // 说明新增了条款
                        lawPojoIF.setGetTime(getTime);
                        lawPojoIF.setUpdateId(UUID.randomUUID().toString().replaceAll("-", ""));
                        lawPojoIF.setUpdateType("1");
                        lawPojoIF.setGroupId(groupId);
                        addList.add(lawPojoIF);
                    }
                }

                // 不同的条款入库 (新增或变更或删除)
                ArrayList<HfLawPojo> allList = new ArrayList<>();
                allList.addAll(addList);
                allList.addAll(delList);
                allList.addAll(chgListOld);

                // 将变化情况保存至变化库
                if (allList.size() != 0) {
                    fileOperationMapper.insertLawUpdateDao(allList);
                    // 将最新内容更新至法律法规库
                    if (addList.size() != 0) {
                        fileOperationMapper.insertLawDao(addList);
                    }
                    if (delList.size() != 0) {
                        fileOperationMapper.deleteLawDao(delList);
                    }
                    if (chgListNew.size() != 0) {
                        for (HfLawPojo hfLawPojo : chgListNew) {
                            fileOperationMapper.updateLawDao(hfLawPojo);
                        }
                    }
                } else {
                    System.out.println("无更新内容");
                }
            }
        }
    }

    /**
     * 获取法律法规变化清单
     *
     * @param fileUuid
     * @param fileVersionId
     */
    @Override
    public TResponseVo getLawChangeListService(String fileUuid, String fileVersionId) throws Exception {

        List<HfLawPojo> lawChangeListDao = fileOperationMapper.getLawChangeListDao(fileUuid, fileVersionId);
//        ArrayList<String> textIdList = new ArrayList<>();
//        List<HashMap<String, Object>> lawChangeListDaoTmp = new ArrayList<>();
//        for (HfLawPojo hfLawPojo : lawChangeListDao) {  // 分组
//            String textId = hfLawPojo.getTextId();
//            if (!textIdList.contains(textId)) {
//                textIdList.add(textId);
//                HashMap<String, Object> map = new HashMap<>();
//                map.put("textId", textId);
//                map.put("textContent", hfLawPojo.getTextContent());
//                ArrayList<HfLawPojo> list = new ArrayList<>();
//                list.add(hfLawPojo);
//                map.put("items", list);
//                lawChangeListDaoTmp.add(map);
//            } else {
//                for (HashMap<String, Object> map : lawChangeListDaoTmp) {
//                    String textId1 = (String) map.get("textId");
//                    if (textId1.equals(textId)) {
//                        List<HfLawPojo> items = (List) map.get("items");
//                        items.add(hfLawPojo);
//                        break;
//                    }
//                }
//            }
//
//        }
        TagPojo tagPojo = new TagPojo();
        tagPojo.setFileUuid(fileUuid);
        tagPojo.setFileVersionId(fileVersionId);
        tagPojo.setTypeId("1");
        tagPojo.setType("all");
        List<TagPojo> contentTagDao = fileOperationMapper.getContentTagDao(tagPojo);
        for (HfLawPojo hfLawPojo : lawChangeListDao) {
            for (TagPojo pojo : contentTagDao) {
                if (hfLawPojo.getTextId().equals(pojo.getLawId())) {
                    hfLawPojo.setTagId(pojo.getTagId());
                    hfLawPojo.setOutLineId(pojo.getOutlineId());
                    break;
                }
            }
        }
        return TResponseVo.success(lawChangeListDao);
    }

    /**
     * 已读
     */
    @Override
    public TResponseVo readLawChangeService(HfLawPojo hfLawPojo) throws Exception {
        String noticeUuid = hfLawPojo.getNoticeUuid();
        String fileUuid = hfLawPojo.getFileUuid();
        String fileVersionId = hfLawPojo.getFileVersionId();
        if (StringUtils.isBlank(noticeUuid)) {
            if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
                throw new WorkTableException("必填参数为空");
            } else {
                fileOperationMapper.readLawAllChangeDao(fileUuid, fileVersionId);
            }
        } else {
            fileOperationMapper.readLawChangeDao(noticeUuid);
        }
        return TResponseVo.success("已读");

    }

    /**
     * 评标阶段
     */
    /**
     * 创建评标办法
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newJudgmentMethodService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        String judgmentMethodName = hfJudgmentDetailPojo.getJudgmentMethodName();
        if (StringUtils.isBlank(judgmentMethodName)) {
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(871);
            throw fileIndexException;
//            return TResponseVo.error("评标办法名称不能为空");
        }
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        List<String> outlineIdList = hfJudgmentDetailPojo.getOutlineIdList();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || outlineIdList == null) {
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(872);
            throw fileIndexException;
//            return TResponseVo.error("必填参数为空");
        }

        // 判断重名
        Integer integer = fileOperationMapper.checkJudgmentMethodNameDao(hfJudgmentDetailPojo);
        if (integer != 0) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newJudgmentMethodService", judgmentMethodName, "当前类错误码-000-评标办法名重复");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(873);
            throw fileIndexException;
//            return TResponseVo.error("该办法名已存在,请重新命名");
        }
        // 设置methodId
        String judgmentMethodId = UUID.randomUUID().toString().replaceAll("-", "");
        hfJudgmentDetailPojo.setJudgmentMethodId(judgmentMethodId);
        // 转换list
        String outlineIdListStr = JSON.toJSONString(outlineIdList);
        hfJudgmentDetailPojo.setOutlineIdListStr(outlineIdListStr);
        try {
            Integer i = fileOperationMapper.newJudgmentMethodDao(hfJudgmentDetailPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(851);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newJudgmentMethodService", judgmentMethodName, "当前类错误码-851-创建评标办法失败");
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newJudgmentMethodService", judgmentMethodName, "当前类错误码-852-创建评标办法失败SQL错误");
            e.printStackTrace();
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(852);
            throw fileIndexException;
        }

        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "创建评标办法成功");
        ret.put("judgmentMethodId", judgmentMethodId);
        return TResponseVo.success(ret);
    }

    /**
     * 评标办法-删除
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo delJudgmentMethodService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        // 参数校验
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId)) {
            return TResponseVo.error("必填参数为空");
        }
        fileOperationMapper.delJudgmentMethodDao(hfJudgmentDetailPojo);
        //TODO 模块和细则也删除
        fileOperationMapper.delJudgmentModuleDao(hfJudgmentDetailPojo);
        fileOperationMapper.delJudgmentDetailDao(hfJudgmentDetailPojo);
        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "删除评标办法成功");
        return TResponseVo.success(ret);
    }

    /**
     * 评标办法-修改
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo updateJudgmentMethodService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        String judgmentMethodName = hfJudgmentDetailPojo.getJudgmentMethodName();
        if (StringUtils.isBlank(judgmentMethodName)) {
            return TResponseVo.error("评标办法名称不能为空");
        }
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId)) {
            return TResponseVo.error("必填参数为空");
        }

        // 判断重名
        Integer integer = fileOperationMapper.checkJudgmentMethodNameDao(hfJudgmentDetailPojo);
        if (integer != 0) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newJudgmentMethodService", judgmentMethodName, "当前类错误码-000-评标办法名重复");
            return TResponseVo.error("该办法名已存在,请重新命名");
        }
        // 转换list
        List<String> outlineIdList = hfJudgmentDetailPojo.getOutlineIdList();
        String outlineIdListStr = JSON.toJSONString(outlineIdList);
        hfJudgmentDetailPojo.setOutlineIdListStr(outlineIdListStr);
        try {
            Integer i = fileOperationMapper.updateJudgmentMethodDao(hfJudgmentDetailPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(853);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newJudgmentMethodService", judgmentMethodName, "当前类错误码-853-修改评标办法失败");
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newJudgmentMethodService", judgmentMethodName, "当前类错误码-854-修改评标办法失败SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(854);
            throw fileIndexException;
        }

        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "修改评标办法成功");
        return TResponseVo.success(ret);
    }

    /**
     * 评标办法-查询
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo getJudgmentMethodInfoService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        // 校验参数
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId)) {
            return TResponseVo.error("必填参数为空");
        }
        HfJudgmentDetailPojo judgmentMethodInfoDao = fileOperationMapper.getJudgmentMethodInfoDao(hfJudgmentDetailPojo);
        String outlineIdListStr = judgmentMethodInfoDao.getOutlineIdListStr();
        List<String> outlineIdList = JSON.parseObject(outlineIdListStr, List.class);
        judgmentMethodInfoDao.setOutlineIdList(outlineIdList);
        judgmentMethodInfoDao.setOutlineIdListStr(null);
        return TResponseVo.success(judgmentMethodInfoDao);
    }

    /**
     * 获取办法段落信息
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo getJudgmentMethodOutlineService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        // 校验参数
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
//        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.error("必填参数为空");
        }
//        HfJudgmentDetailPojo judgmentMethodInfoDao = fileOperationMapper.getJudgmentMethodInfoDao(hfJudgmentDetailPojo);
//        String outlineIdListStr = judgmentMethodInfoDao.getOutlineIdListStr();
        List<String> outlineIdList = hfJudgmentDetailPojo.getOutlineIdList();
        String s = new JsonKeyUtils().listToString(outlineIdList, "','");
        hfJudgmentDetailPojo.setSql(s);
        List<LabelValuePojo> judgmentMethodOutlineDao = fileOperationMapper.getJudgmentMethodOutlineDao(hfJudgmentDetailPojo);
        return TResponseVo.success(judgmentMethodOutlineDao);
    }

    /**
     * 评标办法-查询列表
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo getJudgmentMethodListService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        // 校验参数
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.error("必填参数为空");
        }
        List<HfJudgmentDetailPojo> judgmentMethodList = fileOperationMapper.getJudgmentMethodListDao(hfJudgmentDetailPojo);
        for (HfJudgmentDetailPojo judgmentMethodInfoDao : judgmentMethodList) {
            String outlineIdListStr = judgmentMethodInfoDao.getOutlineIdListStr();
            List<String> outlineIdList = JSON.parseObject(outlineIdListStr, List.class);
            judgmentMethodInfoDao.setOutlineIdList(outlineIdList);
            judgmentMethodInfoDao.setOutlineIdListStr(null);
        }
        return TResponseVo.success(judgmentMethodList);
    }


    /**
     * 创建评标模块
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newJudgmentModuleService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        String judgmentModuleName = hfJudgmentDetailPojo.getJudgmentModuleName();
        if (StringUtils.isBlank(judgmentModuleName)) {
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(881);
            throw fileIndexException;
//            return TResponseVo.error("评标模块名称不能为空");
        }
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        String outlineId = hfJudgmentDetailPojo.getOutlineId();
        String stepId = hfJudgmentDetailPojo.getStepId();
        String judgmentType = hfJudgmentDetailPojo.getJudgmentType();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId) || StringUtils.isBlank(outlineId) || StringUtils.isBlank(stepId) || StringUtils.isBlank(judgmentType)) {
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(882);
            throw fileIndexException;
//            return TResponseVo.error("必填参数为空");
        }
        if (judgmentType.equals("20") && hfJudgmentDetailPojo.getWeight() == null) {
            throw new FileIndexException(883);
//            return TResponseVo.error("评标类型为'评分'时,权重不能为空");
        }

        // 判断重名
        Integer integer = fileOperationMapper.checkJudgmentModuleNameDao(hfJudgmentDetailPojo);
        if (integer != 0) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newJudgmentModuleService", judgmentModuleName, "当前类错误码-000-评标模块名重复");
            throw new FileIndexException(884);
//            return TResponseVo.error("该模块名已存在,请重新命名");
        }
        //
        String judgmentModuleId = UUID.randomUUID().toString().replaceAll("-", "");
        hfJudgmentDetailPojo.setJudgmentModuleId(judgmentModuleId);

        try {
            Integer i = fileOperationMapper.newJudgmentModuleDao(hfJudgmentDetailPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(851);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newJudgmentModuleService", judgmentModuleName, "当前类错误码-851-创建评标模块失败");
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newJudgmentModuleService", judgmentModuleName, "当前类错误码-852-创建评标模块失败SQL错误");
            e.printStackTrace();
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(852);
            throw fileIndexException;
        }

        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "创建评标模块成功");
        ret.put("judgmentModuleId", judgmentModuleId);
        return TResponseVo.success(ret);
    }

    /**
     * 评标模块-删除
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo delJudgmentModuleService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        // 参数校验
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
//        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        String judgmentModuleId = hfJudgmentDetailPojo.getJudgmentModuleId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentModuleId)) {
            return TResponseVo.error("必填参数为空");
        }
        fileOperationMapper.delJudgmentModuleDao(hfJudgmentDetailPojo);
        //TODO 删除评标细则
        fileOperationMapper.delJudgmentDetailDao(hfJudgmentDetailPojo);
        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "删除评标模块成功");
        return TResponseVo.success(ret);
    }

    /**
     * 评标模块-修改
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo updateJudgmentModuleService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        String judgmentModuleName = hfJudgmentDetailPojo.getJudgmentModuleName();
        if (StringUtils.isBlank(judgmentModuleName)) {
            return TResponseVo.error("评标办法名称不能为空");
        }
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        String outlineId = hfJudgmentDetailPojo.getOutlineId();
        String stepId = hfJudgmentDetailPojo.getStepId();
        String judgmentType = hfJudgmentDetailPojo.getJudgmentType();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId) || StringUtils.isBlank(outlineId) || StringUtils.isBlank(stepId) || StringUtils.isBlank(judgmentType)) {
            return TResponseVo.error("必填参数为空");
        }
        if (judgmentType.equals("20") && hfJudgmentDetailPojo.getWeight() == null) {
            return TResponseVo.error("评标类型为'评分'时,权重不能为空");
        }

        // 判断重名
        Integer integer = fileOperationMapper.checkJudgmentModuleNameDao(hfJudgmentDetailPojo);
        if (integer != 0) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateJudgmentModuleService", judgmentModuleName, "当前类错误码-000-评标模块名重复");
            return TResponseVo.error("该办法名已存在,请重新命名");
        }

        try {
            Integer i = fileOperationMapper.updateJudgmentModuleDao(hfJudgmentDetailPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(853);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateJudgmentModuleService", judgmentModuleName, "当前类错误码-853-修改评标模块失败");
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateJudgmentModuleService", judgmentModuleName, "当前类错误码-854-修改评标模块失败SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(854);
            throw fileIndexException;
        }

        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "修改评标模块成功");
        return TResponseVo.success(ret);
    }

    /**
     * 评标模块-查询
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo getJudgmentModuleInfoService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        // 校验参数
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        String judgmentModuleId = hfJudgmentDetailPojo.getJudgmentModuleId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId) || StringUtils.isBlank(judgmentModuleId)) {
            return TResponseVo.error("必填参数为空");
        }
        HfJudgmentDetailPojo judgmentModuleInfoDao = fileOperationMapper.getJudgmentModuleInfoDao(hfJudgmentDetailPojo);
        return TResponseVo.success(judgmentModuleInfoDao);
    }

    /**
     * 评标模块-查询列表
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo getJudgmentModuleListService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        // 校验参数
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId)) {
            return TResponseVo.error("必填参数为空");
        }
        List<HfJudgmentDetailPojo> judgmentModuleList = fileOperationMapper.getJudgmentModuleListDao(hfJudgmentDetailPojo);
        return TResponseVo.success(judgmentModuleList);
    }


    /**
     * 创建评标细则(判断or评分)
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newJudgmentDetailService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        // 参数校验
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.error("必填参数为空");
        }

        // 如果judgmentMethodId为空 则代表新建评标方法
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        if (StringUtils.isBlank(judgmentMethodId)) {
            newJudgmentMethodService(hfJudgmentDetailPojo);
        } else {
            updateJudgmentMethodService(hfJudgmentDetailPojo);
        }
        // 如果judgmentModuleId为空 则代表新建评标模块
        String judgmentModuleId = hfJudgmentDetailPojo.getJudgmentModuleId();
        if (StringUtils.isBlank(judgmentModuleId)) {
            newJudgmentModuleService(hfJudgmentDetailPojo);
        } else {
            updateJudgmentModuleService(hfJudgmentDetailPojo);
        }

        // 开始创建评标细则
        String judgmentDetailName = hfJudgmentDetailPojo.getJudgmentDetailName();
        String judgmentDetailContent = hfJudgmentDetailPojo.getJudgmentDetailContent();
        if (StringUtils.isBlank(judgmentDetailName) || StringUtils.isBlank(judgmentDetailContent)) {
            return TResponseVo.error("评审因素和评审标准不能为空");
        }
        // 处理选中的参数
        List<String> paramsUuidList = hfJudgmentDetailPojo.getParamsUuidList();
        if (paramsUuidList == null) {
            hfJudgmentDetailPojo.setParamsUuid("[]");
        } else {
            String paramsUuid = JSON.toJSONString(paramsUuidList);
            hfJudgmentDetailPojo.setParamsUuid(paramsUuid);
        }

        String judgmentDetailId = hfJudgmentDetailPojo.getJudgmentDetailId();
        if (StringUtils.isBlank(hfJudgmentDetailPojo.getJudgmentDetailId())) {
            // 初始化
            judgmentDetailId = UUID.randomUUID().toString().replaceAll("-", "");
            hfJudgmentDetailPojo.setJudgmentDetailId(judgmentDetailId);
            fileOperationMapper.newJudgmentDetailDao(hfJudgmentDetailPojo);
        } else {
            updateJudgmentDetailService(hfJudgmentDetailPojo);
        }

        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "操作成功");
        ret.put("judgmentMethodId", hfJudgmentDetailPojo.getJudgmentMethodId());
        ret.put("judgmentModuleId", hfJudgmentDetailPojo.getJudgmentModuleId());
        ret.put("judgmentDetailId", judgmentDetailId);
        return TResponseVo.success(ret);
    }

    /**
     * 评标细则-删除
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo delJudgmentDetailService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        // 参数校验
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        String judgmentModuleId = hfJudgmentDetailPojo.getJudgmentModuleId();
        String judgmentDetailId = hfJudgmentDetailPojo.getJudgmentDetailId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId) || StringUtils.isBlank(judgmentModuleId) || StringUtils.isBlank(judgmentDetailId)) {
            return TResponseVo.error("必填参数为空");
        }
        fileOperationMapper.delJudgmentDetailDao(hfJudgmentDetailPojo);
        //TODO 删除评标细则
        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "删除评标模块成功");
        return TResponseVo.success(ret);
    }

    /**
     * 评标细则-修改
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateJudgmentDetailService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        String judgmentModuleId = hfJudgmentDetailPojo.getJudgmentModuleId();
        String judgmentDetailId = hfJudgmentDetailPojo.getJudgmentDetailId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId) || StringUtils.isBlank(judgmentModuleId) || StringUtils.isBlank(judgmentDetailId)) {
            return TResponseVo.error("必填参数为空");
        }
        // 处理选中的参数
        List<String> paramsUuidList = hfJudgmentDetailPojo.getParamsUuidList();
        if (paramsUuidList == null) {
            hfJudgmentDetailPojo.setParamsUuid("[]");
        } else {
            String paramsUuid = JSON.toJSONString(paramsUuidList);
            hfJudgmentDetailPojo.setParamsUuid(paramsUuid);
        }
        try {
            Integer i = fileOperationMapper.updateJudgmentDetailDao(hfJudgmentDetailPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(853);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            LOG.error("[Class]:{}-[Msg]:{}", "updateJudgmentDetailService", "当前类错误码-853-修改失败");
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[Msg]:{}", "updateJudgmentDetailService", "当前类错误码-854-修改失败SQL错误");
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(854);
            throw fileIndexException;
        }

        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "修改成功");
        return TResponseVo.success(ret);
    }

    /**
     * 评标细则-查询
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo getJudgmentDetailInfoService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        String judgmentModuleId = hfJudgmentDetailPojo.getJudgmentModuleId();
        String judgmentDetailId = hfJudgmentDetailPojo.getJudgmentDetailId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId) || StringUtils.isBlank(judgmentModuleId) || StringUtils.isBlank(judgmentDetailId)) {
            return TResponseVo.error("必填参数为空");
        }
        // 获取填参信息
        List<LabelValuePojo> tendParamsListDao = fileOperationMapper.getTendParamsListDao(fileUuid, fileVersionId);
        // 获取书签
        List<BookmarkPojo> bookmarkLabelValueListDao = fileOperationMapper.getBookmarkListDao2(fileUuid, fileVersionId);
        // 补充参数信息
        HfJudgmentDetailPojo judgmentDetailInfoDao = fileOperationMapper.getJudgmentDetailInfoDao(hfJudgmentDetailPojo);
        String paramsUuid = judgmentDetailInfoDao.getParamsUuid();
        List<String> paramsUuidList = JSON.parseObject(paramsUuid, List.class);
        judgmentDetailInfoDao.setParamsUuidList(paramsUuidList);
        judgmentDetailInfoDao.setParamsUuid(null);
        // 创建参数名list
        List<String> paramsNameList = new ArrayList<>();
        for (String pUuid : paramsUuidList) {
            for (LabelValuePojo labelValuePojo : tendParamsListDao) {
                if (labelValuePojo.getValue().equals(pUuid)) {
                    paramsNameList.add(labelValuePojo.getLabel());
                    break;
                }
            }
        }
        judgmentDetailInfoDao.setParamsNameList(paramsNameList);
        // 补充书签信息
        for (BookmarkPojo bookmarkPojo : bookmarkLabelValueListDao) {
            if (bookmarkPojo.getBookmarkUuid().equals(judgmentDetailInfoDao.getBookmarkUuid())) {
                judgmentDetailInfoDao.setBookmarkName(bookmarkPojo.getBookmarkName());
                judgmentDetailInfoDao.setBookmarkOutlineId(bookmarkPojo.getOutlineId());
                break;
            }
        }
        return TResponseVo.success(judgmentDetailInfoDao);
    }

    /**
     * 评标细则-查询列表
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo getJudgmentDetailListService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        String judgmentMethodId = hfJudgmentDetailPojo.getJudgmentMethodId();
        String judgmentModuleId = hfJudgmentDetailPojo.getJudgmentModuleId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(judgmentMethodId) || StringUtils.isBlank(judgmentModuleId)) {
            return TResponseVo.error("必填参数为空");
        }
        // 获取填参信息
        List<LabelValuePojo> tendParamsListDao = fileOperationMapper.getTendParamsListDao(fileUuid, fileVersionId);
        // 获取书签
        List<BookmarkPojo> bookmarkLabelValueListDao = fileOperationMapper.getBookmarkListDao2(fileUuid, fileVersionId);

        List<HfJudgmentDetailPojo> judgmentDetailListDao = fileOperationMapper.getJudgmentDetailListDao(hfJudgmentDetailPojo);
        for (HfJudgmentDetailPojo judgmentDetailPojo : judgmentDetailListDao) {
            // 补充参数信息
            String paramsUuid = judgmentDetailPojo.getParamsUuid();
            List<String> paramsUuidList = JSON.parseObject(paramsUuid, List.class);
            judgmentDetailPojo.setParamsUuidList(paramsUuidList);
            judgmentDetailPojo.setParamsUuid(null);
            // 创建参数名list
            List<String> paramsNameList = new ArrayList<>();
            for (String pUuid : paramsUuidList) {
                for (LabelValuePojo labelValuePojo : tendParamsListDao) {
                    if (labelValuePojo.getValue().equals(pUuid)) {
                        paramsNameList.add(labelValuePojo.getLabel());
                        break;
                    }
                }
            }
            judgmentDetailPojo.setParamsNameList(paramsNameList);
            // 补充书签信息
            for (BookmarkPojo bookmarkPojo : bookmarkLabelValueListDao) {
                if (bookmarkPojo.getBookmarkUuid().equals(judgmentDetailPojo.getBookmarkUuid())) {
                    judgmentDetailPojo.setBookmarkName(bookmarkPojo.getBookmarkName());
                    judgmentDetailPojo.setBookmarkOutlineId(bookmarkPojo.getOutlineId());
                    break;
                }
            }
        }
        return TResponseVo.success(judgmentDetailListDao);
    }


    /**
     * 查询办法与模块的树结构
     *
     * @param hfJudgmentDetailPojo
     */
    @Override
    public TResponseVo getJudgmentMethodModuleTreeService(HfJudgmentDetailPojo hfJudgmentDetailPojo) throws Exception {
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.error("必填参数为空");
        }
        List<HfJudgmentDetailPojo> judgmentMethodListDao = fileOperationMapper.getJudgmentMethodListDao(hfJudgmentDetailPojo);
        List<HfJudgmentDetailPojo> judgmentModuleListDao = fileOperationMapper.getJudgmentModuleListDao(hfJudgmentDetailPojo);
        List<HfJudgmentDetailPojo> judgmentModuleListDaoTmp = new ArrayList<>();
        judgmentModuleListDaoTmp.addAll(judgmentModuleListDao);
        for (HfJudgmentDetailPojo methodPojo : judgmentMethodListDao) {
            methodPojo.setLabel(methodPojo.getJudgmentMethodName());
            methodPojo.setValue(methodPojo.getJudgmentMethodId());
            methodPojo.setType("method");
            String outlineIdListStr = methodPojo.getOutlineIdListStr();
            methodPojo.setOutlineIdList(JSON.parseObject(outlineIdListStr, List.class));
            methodPojo.setOutlineIdListStr(null);
            List<HfJudgmentDetailPojo> children = new ArrayList<>();
            for (HfJudgmentDetailPojo modulePojo : judgmentModuleListDao) {
                modulePojo.setLabel(modulePojo.getJudgmentModuleName());
                modulePojo.setValue(modulePojo.getJudgmentModuleId());
                modulePojo.setType("module");
                if (modulePojo.getJudgmentMethodId().equals(methodPojo.getJudgmentMethodId())) {
                    children.add(modulePojo);
                    judgmentModuleListDaoTmp.remove(modulePojo);
                }
            }
            methodPojo.setChildren(children);
            judgmentModuleListDao.clear();
            judgmentModuleListDao.addAll(judgmentModuleListDaoTmp);
        }
        return TResponseVo.success(judgmentMethodListDao);
    }

    /**
     * 获取投标人要填写的参数列表
     *
     * @param
     */
    @Override
    public TResponseVo getTendParamsListService(String fileUuid, String fileVersionId) throws Exception {
        List<LabelValuePojo> tendParamsListDao = fileOperationMapper.getTendParamsListDao(fileUuid, fileVersionId);
        return TResponseVo.success(tendParamsListDao);
    }


    /**
     * 参数值的换算
     *
     * @param value
     * @param unitId
     */
    @Override
    public TResponseVo unitTransService(String value, String unitId, String tagUnitId) throws Exception {
        if (StringUtils.isBlank(value) || StringUtils.isBlank(unitId) || StringUtils.isBlank(tagUnitId)) {
            return TResponseVo.error("必填参数为空");
        }
        List<DocframeParamsUnitDiPojo> unitDiDao = fileOperationMapper.getUnitDiDao();
        DocframeParamsUnitDiPojo unitPojo = new DocframeParamsUnitDiPojo();
        DocframeParamsUnitDiPojo tagUnitPojo = new DocframeParamsUnitDiPojo();
        for (DocframeParamsUnitDiPojo docframeParamsUnitDiPojo : unitDiDao) {
            if (docframeParamsUnitDiPojo.getUnitId().equals(unitId)) {
                unitPojo = docframeParamsUnitDiPojo;
            }
            if (docframeParamsUnitDiPojo.getUnitId().equals(tagUnitId)) {
                tagUnitPojo = docframeParamsUnitDiPojo;
            }
        }
        BigDecimal value1 = new BigDecimal(value);
        BigDecimal rule1 = new BigDecimal(unitPojo.getRule());
        BigDecimal tagRule1 = new BigDecimal(tagUnitPojo.getRule());
        BigDecimal multiply = value1.multiply(tagRule1).divide(rule1, 2, BigDecimal.ROUND_HALF_UP);
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "执行成功");
        ret.put("result", multiply.stripTrailingZeros().toPlainString());
        return TResponseVo.success(ret);
    }

    /**
     * 获取单位列表
     */
    @Override
    public TResponseVo getUnitListService(List<String> nodeIds) throws Exception {
        List<DocframeParamsUnitDiPojo> unitDiDao = fileOperationMapper.getUnitDiDao(nodeIds);
        // 找到第一层级 所有类型
        ArrayList<String> unitTypeList = new ArrayList<>();
        for (DocframeParamsUnitDiPojo unitDiPojo : unitDiDao) {
            if (!unitTypeList.contains(unitDiPojo.getUnitType())) {
                unitTypeList.add(unitDiPojo.getUnitType());
            }
        }
        // 拼装第二层 把单位挂到类型下面
        ArrayList<Map<String, Object>> unitTypeMapList = new ArrayList<>();
        for (String unitType : unitTypeList) {
            // 剔除不要的类型
//            if (unitType.equals("人民币")) {
//                continue;
//            }
            Map<String, Object> map = new HashMap<>();
            map.put("value", unitType);
            map.put("label", unitType);
            unitTypeMapList.add(map);
        }
        for (Map<String, Object> map : unitTypeMapList) {
            for (DocframeParamsUnitDiPojo unitDiPojo : unitDiDao) {
                if (map.get("value").equals(unitDiPojo.getUnitType())) {
                    if (map.get("children") == null) {
                        ArrayList<Map<String, String>> children = new ArrayList<>();
                        Map<String, String> m = new HashMap<>();
                        m.put("value", unitDiPojo.getUnitId());
                        m.put("label", unitDiPojo.getUnitName());
                        children.add(m);
                        map.put("children", children);
                    } else {
                        Map<String, String> m = new HashMap<>();
                        m.put("value", unitDiPojo.getUnitId());
                        m.put("label", unitDiPojo.getUnitName());
                        ((List) map.get("children")).add(m);
                    }
                }
            }
        }
        return TResponseVo.success(unitTypeMapList);
    }

    /**
     * 获取文内某参数的详细信息(沉浸模式使用)
     *
     * @param fileUuid
     * @param fileVersionId
     * @param
     */
    @Override
    public TResponseVo getParamDetailService(String fileUuid, String fileVersionId, String uuid) throws Exception {
        DocParamsPojo paramDetailDao = fileOperationMapper.getParamDetailDao(fileUuid, fileVersionId, uuid);
        if (paramDetailDao == null) {
            return TResponseVo.error("该参数数据不存在,请检查入参数据是否匹配");
        }

        if ("70".equals(paramDetailDao.getParamsTypeId()) || "95".equals(paramDetailDao.getParamsTypeId())) {  // 如果是附件参数或矩阵参数 就把paramsText转换成数组list
            paramDetailDao.setParamsText(JSON.parseObject((String) paramDetailDao.getParamsText(), List.class) == null ? new ArrayList<>() : JSON.parseObject((String) paramDetailDao.getParamsText(), List.class));
        }

        String paramsUuid = paramDetailDao.getParamsUuid();
        List<LabelValuePojo> paramUnitListDao = fileOperationMapper.getParamUnitListDao(fileUuid, fileVersionId, paramsUuid);
        List<Map<String, String>> unitlist = JSON.parseObject(JSON.toJSONString(paramUnitListDao), List.class);
        paramDetailDao.setUnitList(unitlist);

        List<DocParamsPojo> uuidListDao = fileOperationMapper.getUuidListDao(fileUuid, fileVersionId, paramsUuid);
        List<Map<String, String>> uuidList = JSON.parseObject(JSON.toJSONString(uuidListDao), List.class);
        for (Map<String, String> map : uuidList) {
            String s = map.get("outlineId");
            map.put("outLineId", s);  // key的大小写
            map.remove("outlineId");
        }
        paramDetailDao.setUuidList(uuidList);

        // 选择题的range
        String paramsRange = paramDetailDao.getParamsRange();
        List paramsRangeList = JSON.parseObject(paramsRange, List.class);
        paramDetailDao.setParamsRangeList(paramsRangeList);
        paramDetailDao.setParamsRange(null);
        // 矩阵参数头
        String paramsColumnsStr = (String) paramDetailDao.getParamsColumns();
        List paramsColumns = JSON.parseObject(paramsColumnsStr, List.class);
        paramDetailDao.setParamsColumns(paramsColumns);
        return TResponseVo.success(paramDetailDao);
    }


    /**
     * 获取同项目下文件列表
     *
     * @param projectId
     */
    @Override
    public TResponseVo getProjectMateFileService(String projectId) throws Exception {
        List<LabelValuePojo> projectMateFileList = fileOperationMapper.getProjectMateFileDao(projectId);
        return TResponseVo.success(projectMateFileList);
    }

    /**
     * 组合文件（项目内，文件编辑，点击替换章节，选择文件章节，替换）
     *
     * @param
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newCompoundFileService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        String outlineId = docFileIndexPojo.getOutlineId();
        String fileUuid2 = docFileIndexPojo.getFileUuid2();
        DocFileIndexPojo docAllInfoDao2 = fileOperationMapper.getDocAllInfoDao(fileUuid2);
        String fileVersionId2 = docAllInfoDao2.getFileVersionId();
        List<String> outlineIdList2 = docFileIndexPojo.getOutlineIdList2();
        Boolean includeParam = docFileIndexPojo.getIncludeParam();
        //TODO 要验证outlineIdList2能否满足树结构

//        System.out.println("outlineId : " + outlineId + ",fileUuid : " + fileUuid + ",fileVersionId : " + fileVersionId);
        OutLinePojo outlineInfoDao = fileOperationMapper.getOutlineInfoDao(outlineId, fileUuid, fileVersionId);
        // 找到目标段落下所有的子段落
        List<OutLinePojo> outlineListDao = fileOperationMapper.getOutlineListDao(fileUuid, fileVersionId);
        List<String> outlineIdList = new ArrayList<>();
        for (OutLinePojo outLinePojo : outlineListDao) {  // 由于outlineListDao的数据是有顺序的 所以可以这么写
            if (outlineIdList.contains(outLinePojo.getOutlineFatherId())) {
                outlineIdList.add(outLinePojo.getOutlineId());
                continue;
            }
            if (outLinePojo.getOutlineId().equals(outlineId)) {
                outlineIdList.add(outlineId);
                continue;
            }
        }

        String outlineIdListStr = new JsonKeyUtils().listToString(outlineIdList, "','");
        int size = outlineIdList.size();

        // 获取被删除的首节点的节点信息
        String outlineFatherId1 = outlineInfoDao.getOutlineFatherId();
        String outlineOrder1 = outlineInfoDao.getOutlineOrder();

        // 删除目录树缓存信息
        redisUtils.del("getOutlineList" + fileUuid + fileVersionId);
        fileOperationMapper.deleteOutlineInfoDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删索引
        fileOperationMapper.deleteContentDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删内容
        fileOperationMapper.deleteContentParamsDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删参数
        fileOperationMapper.deleteTagDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删标注
        fileOperationMapper.deleteBookmarkDao1(outlineIdListStr, fileUuid, fileVersionId);  // 删书签
        fileOperationMapper.deleteJudgment1(outlineIdListStr, fileUuid, fileVersionId);  // 删评标要素

        // 后插入
        List<OutLinePojo> outlineListDao2 = fileOperationMapper.getOutlineListDao(fileUuid2, fileVersionId2);
        ArrayList<OutLinePojo> outlineListDao2Tmp = new ArrayList<>();  // B文章被选中的段落信息
        for (OutLinePojo outLinePojo : outlineListDao2) {
            if (outlineIdList2.contains(outLinePojo.getOutlineId())) {
                outlineListDao2Tmp.add(outLinePojo);
            }
        }

        // outlineListDao2Tmp是有序的
        // 处理方法: 找到B文第一个段落的父节点id 将其替换为A文被删除节点的父节点id,同时,后续段落中如果有与第一个段落的父节点相同的节点,继续做同样操作
        // 1 找到第一个段落的父节点id
        OutLinePojo outLinePojo2 = outlineListDao2Tmp.get(0);
        String outlineFatherId2 = outLinePojo2.getOutlineFatherId();
        // 2 将所有父节点为替outlineFatherId2换为被删除节点的父节点outlineFatherId1
        for (OutLinePojo outLinePojo : outlineListDao2Tmp) {
            if (outLinePojo.getOutlineFatherId().equals(outlineFatherId2)) {
                outLinePojo.setOutlineFatherId("XXXXXXXX");
            }
        }
        // 3 将B文里所有的outlineId重置
        HashMap<String, String> map = new HashMap<>();
        for (OutLinePojo outLinePojo : outlineListDao2Tmp) {
            String outlineId2 = outLinePojo.getOutlineId();
            String newOutlineId = UUID.randomUUID().toString().replaceAll("-", "");
            map.put(outlineId2, newOutlineId);
            outLinePojo.setOutlineId(newOutlineId);
        }
        map.put("XXXXXXXX", outlineFatherId1);
        // 4 将B文里所有fatherOutlineId重置
        for (OutLinePojo outLinePojo : outlineListDao2Tmp) {
            String outlineFatherId = outLinePojo.getOutlineFatherId();
            outLinePojo.setOutlineFatherId(map.get(outlineFatherId));
        }
        // 5 将B文的fileUuid和verid重置
        for (OutLinePojo outLinePojo : outlineListDao2Tmp) {
            outLinePojo.setFileUuid(fileUuid);
            outLinePojo.setFileVersionId(fileVersionId);
        }
        // 6 所有当前order之后的order通通减(或增)outlineListDao2Tmp的长度
        fileOperationMapper.updateOutlineOrderDao(outlineOrder1, fileUuid, fileVersionId, -size + outlineListDao2Tmp.size());

        // 7 重置新插入段落的order顺序 以适应文档目录
        for (OutLinePojo outLinePojo : outlineListDao2Tmp) {
            outLinePojo.setOutlineOrder(outlineOrder1);
            outlineOrder1 = String.valueOf(Integer.valueOf(outlineOrder1) + 1);
        }
        // 8 将B文索引数据插入数据库
        String sql = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(new Date());
        for (OutLinePojo outLinePojo : outlineListDao2Tmp) {
            sql = sql + "(\"" + outLinePojo.getOutlineId()
                    + "\"," + JSON.toJSONString(outLinePojo.getOutlineText())
                    + ",\"" + outLinePojo.getFileUuid()
                    + "\",\"" + outLinePojo.getFileVersionId()
                    + "\",\"" + outLinePojo.getOutlineFatherId()
                    + "\",\"" + outLinePojo.getOutlineOrder()
                    + "\",\"" + outLinePojo.getOutlineLevel()
                    + "\",\"" + outLinePojo.getOutlineProperty()
                    + "\",\"" + dateString
                    + "\",\"" + outLinePojo.getCreateUserId()
                    + "\",\"" + outLinePojo.getUpdateUserId()
                    + "\"),";
        }
        if (sql.length() != 0) {
            sql = sql.substring(0, sql.length() - 1);
            fileOperationMapper.insertOutlineDao(sql);
        }
        // 9 将B文内容数据插入数据库
        String outlineIdListStr2 = new JsonKeyUtils().listToString(outlineIdList2, "','");
        List<ContentPojo> contentPojoList = fileOperationMapper.getcontentListDao(fileUuid2, fileVersionId2, outlineIdListStr2);
        if (includeParam == true) {
            // 10 将B文参数数据插入数据库
            List<DocParamsPojo> ParamsPojoList = fileOperationMapper.getParamListDao(fileUuid2, fileVersionId2, outlineIdListStr2);
            HashMap<String, String> uuidMap = new HashMap<>(); // 记录参数 key:老uuid value:新uuid
            for (DocParamsPojo paramsPojo : ParamsPojoList) {
                String uuid = UUID.randomUUID().toString();
                uuidMap.put(paramsPojo.getUuid(), uuid);
                paramsPojo.setUuid(uuid);
            }
            sql = "";
            for (DocParamsPojo paramsPojo : ParamsPojoList) {
                sql = sql + "(\"" + paramsPojo.getUuid()
                        + "\",\"" + paramsPojo.getParamsUuid()
                        + "\"," + (paramsPojo.getParamsChoose() == null ? null : "\"" + paramsPojo.getParamsChoose() + "\"")
//                        + "," + (paramsPojo.getParamsText() == null?null:"\'"+paramsPojo.getParamsText()+"\'")
                        + "," + (paramsPojo.getParamsText() == null ? null : "\'" + String.valueOf(paramsPojo.getParamsText()).replaceAll("\\\\", "\\\\\\\\") + "\'")
                        + "," + (paramsPojo.getLastParamsChoose() == null ? null : "\"" + paramsPojo.getLastParamsChoose() + "\"")
                        + "," + (paramsPojo.getLastParamsText() == null ? null : "\'" + paramsPojo.getLastParamsText().replaceAll("\\\\", "\\\\\\\\") + "\'")
                        + ",\"" + fileUuid
                        + "\",\"" + fileVersionId
                        + "\",\"" + map.get(paramsPojo.getOutlineId())
                        + "\",\"" + dateString
                        + "\"," + (paramsPojo.getCreateUserId() == null ? null : "\"" + paramsPojo.getCreateUserId() + "\"")
                        + "," + (paramsPojo.getCreateCompanyId() == null ? null : "\"" + paramsPojo.getCreateCompanyId() + "\"")
                        + "," + (paramsPojo.getParamsName() == null ? null : "\"" + paramsPojo.getParamsName() + "\"")
                        + "," + (paramsPojo.getParamsDesc() == null ? null : "\"" + paramsPojo.getParamsDesc() + "\"")
                        + "," + (paramsPojo.getParamsTypeId() == null ? null : "\"" + paramsPojo.getParamsTypeId() + "\"")
                        + "," + (paramsPojo.getParamsUseSceneId() == null ? null : "\"" + paramsPojo.getParamsUseSceneId() + "\"")
                        + "," + (paramsPojo.getParamsGroupId() == null ? null : "\'" + paramsPojo.getParamsGroupId() + "\'")
                        + "," + (paramsPojo.getIsNecessary() == null ? null : "\"" + paramsPojo.getIsNecessary() + "\"")
                        + "," + (paramsPojo.getIsUnderLine() == null ? null : "\"" + paramsPojo.getIsUnderLine() + "\"")
                        + "," + (paramsPojo.getDataSource() == null ? null : "\'" + paramsPojo.getDataSource() + "\'")
                        + "," + (paramsPojo.getParamsRange() == null ? null : "\'" + paramsPojo.getParamsRange() + "\'")
                        + "," + (paramsPojo.getParamsColumns() == null ? null : "\'" + paramsPojo.getParamsColumns() + "\'")
                        + "," + (paramsPojo.getMatrixDisplay() == null ? null : "\'" + paramsPojo.getMatrixDisplay() + "\'")
                        + "," + (paramsPojo.getMatrixMode() == null ? null : "\'" + paramsPojo.getMatrixMode() + "\'")
                        + "," + (paramsPojo.getParamsClassify() == null ? null : "\"" + paramsPojo.getParamsClassify() + "\"")
                        + "," + (paramsPojo.getShowText() == null ? null : "\"" + paramsPojo.getShowText() + "\"")
                        + "," + (paramsPojo.getStaticParamsUuid() == null ? null : "\"" + paramsPojo.getStaticParamsUuid() + "\"")
                        + "," + (paramsPojo.getIsDel() == null ? null : "\"" + paramsPojo.getIsDel() + "\"")
                        + "," + (paramsPojo.getStyleId() == null ? null : "\"" + paramsPojo.getStyleId() + "\"")
                        + "," + (paramsPojo.getUnit() == null ? null : "\"" + paramsPojo.getUnit() + "\"")
                        + "," + (paramsPojo.getRemark() == null ? null : "\"" + paramsPojo.getRemark() + "\"")
                        + "," + (paramsPojo.getIsInit() == null ? null : "\"" + paramsPojo.getIsInit() + "\"")
                        + "),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
//                System.out.println("-------" + sql);
                try {
                    fileOperationMapper.insertParamsDao(sql);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error("[Class]:{}-[Msg]:{}", "updateJudgmentDetailService", "当前类错误码-1000-段落中存在同源参数,不可替换");
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(1000);
                    throw fileIndexException;
                }
            }

//            内容数据插入数据库
            sql = "";
            for (ContentPojo contentPojo : contentPojoList) {
                // 替换掉原文中的uuid
                String contentText = contentPojo.getContentText();


//                org.dom4j.Document document = null;
//                try {
//                    document = DocumentHelper.parseText(contentText);  // 必须被同一标签包裹
//                } catch (Exception e) {
//                    document = DocumentHelper.parseText("<html>" + contentText.replaceAll("&nbsp;", "") + "</html>");  // 必须被同一标签包裹
//                }
//                // 获取节点元素对象与值
//                String xpath = "//parameter";
//                List<Node> list = document.selectNodes(xpath);
//                for (Node o : list) {
//                    System.out.println(o.asXML());
//                    org.dom4j.Element element = (org.dom4j.Element) o;
//                    String uuid = element.attributeValue("uuid");
//                    element.addAttribute("uuid",uuidMap.get(uuid) == null ? UUID.randomUUID().toString() : uuidMap.get(uuid));
//                }
//                contentText = document.asXML().replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>\n", "");
//                contentPojo.setContentText(contentText);


                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                Elements elements = doc.select("parameter");
                Elements elementsBlock = doc.select("parameterblock");
                elements.addAll(elementsBlock);
                Elements elementsTmp = new Elements();
                elementsTmp.addAll(elements);
                for (Element element : elementsTmp) {
                    String uuid = element.attr("uuid");
                    element.attr("uuid", uuidMap.get(uuid) == null ? UUID.randomUUID().toString() : uuidMap.get(uuid));  // 对于从左向右无法定位的参数,uuidMap.get(uuid)会为null,此时随机一个uuid
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                contentPojo.setContentText(contentText);
                // 拼装sql
                sql = sql + "(\"" + UUID.randomUUID().toString().replaceAll("-", "")
                        + "\"," + JSON.toJSONString(contentPojo.getContentText())
                        + ",\"" + map.get(contentPojo.getOutlineId())
                        + "\",\"" + fileUuid
                        + "\",\"" + fileVersionId
                        + "\",\"" + dateString
                        + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
                fileOperationMapper.insertContentDao(sql);
            }
        } else {
            // 否则踢出文内的参数
            sql = "";
            for (ContentPojo contentPojo : contentPojoList) {
                String contentText = contentPojo.getContentText();
                Document doc = Jsoup.parse(contentText, Parser.xmlParser());  // Jsoup.parse不用改
                doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                Elements elements = doc.select("parameter");
                Elements elementsBlock = doc.select("parameterblock");
                elements.addAll(elementsBlock);
                Elements elementsTmp = new Elements();
                elementsTmp.addAll(elements);
                for (Element element : elementsTmp) {
                    element.before(element.html());
                    element.remove();  // 去除mark标签
                }
                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
                contentPojo.setContentText(contentText);
                sql = sql + "(\"" + UUID.randomUUID().toString().replaceAll("-", "")
                        + "\"," + JSON.toJSONString(contentText)
                        + ",\"" + map.get(contentPojo.getOutlineId())
                        + "\",\"" + fileUuid
                        + "\",\"" + fileVersionId
                        + "\",\"" + dateString
                        + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
                fileOperationMapper.insertContentDao(sql);
            }
        }

        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("info", "合并完成");
        return TResponseVo.success(retMap);
    }


    /**
     * 项目投递
     *
     * @param docFileIndexPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newDeliverFileService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        // 获取父文件的fileUuid fileVersionId
        String fileUuid = docFileIndexPojo.getFileUuid();
        String oldFileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        String projectId = docFileIndexPojo.getProjectId();
        String batchNo = null;
        String batchId = null;
        if (workingTableMapper.getProjectInfoDao(projectId) == null) {
            batchNo = projectId;
            BatchPojo batchInfoByBatchNoDao = workingTableMapper.getBatchInfoByBatchNoDao(batchNo);
            if (batchInfoByBatchNoDao == null) {
                return TResponseVo.error(ResponseEnum.ERROR, "该项目编号不存在,请检查");
            }
            batchId = batchInfoByBatchNoDao.getBatchId();
            projectId = null;
        }
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            return TResponseVo.error(ResponseEnum.ERROR, "附属文件无投递功能");
        }

        List<Map<String, String>> fileList = docFileIndexPojo.getFileList();
        ArrayList<String> fileUuidList = new ArrayList<>();  // 记录被勾选的文件
        if (fileList != null) {
            for (Map<String, String> map : fileList) {
                fileUuidList.add(map.get("value"));
            }
        }

//        文件名称
        if (StringUtils.isBlank(docFileIndexPojo.getFileName())) {
            docFileIndexPojo.setFileName(docAllInfoDao.getFileName());
        }
//        文件版本（默认V1.0）
        docFileIndexPojo.setFileVersionName("V1.0");
//        参与人
        List<String> includeUserIdList = docFileIndexPojo.getIncludeUserIdList();
        String includeUserList = new JsonKeyUtils().listToString(includeUserIdList);
        docFileIndexPojo.setIncludeUserList(includeUserList);
        docFileIndexPojo.setFileTypeId(docAllInfoDao.getFileTypeId());
//        适用范围

//        文件说明
        String fileDesc = docFileIndexPojo.getFileDesc();
//        文件标签
        List<String> fileLabelIds = docFileIndexPojo.getFileLabelIds();
        docFileIndexPojo.setFileLabelList(new JsonKeyUtils().listToString(fileLabelIds, ","));

        // 设置父文件uuid
        docFileIndexPojo.setFileParentId(fileUuid);
        docFileIndexPojo.setOldFileVersionId(fileVersionId);
        if (!docAllInfoDao.getFileVersionId().equals(fileVersionId)) {
            // 说明 已当前最新的"正式版"为准
            docFileIndexPojo.setOldFileVersionId(docAllInfoDao.getFileVersionId());
        }
        fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
        docFileIndexPojo.setFileUuid(fileUuid);
        docFileIndexPojo.setFileVersionId(fileVersionId);

        String userId = docFileIndexPojo.getCreateUserId();

        docFileIndexPojo.setCreateTime(new Date());

        try {
            int j = 0;
            int i = 0;
            // 创建索引
            i = fileOperationMapper.addDeriveFileIndexDao2(docFileIndexPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(700);
                throw fileIndexException;
            }

            // 创建版本
            i = fileOperationMapper.addDeriveFileVersionDao(docFileIndexPojo);
            if (i == 0) {
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(701);
                throw fileIndexException;
            }

            // 创建大纲
            List<OutLinePojo> getDeriveFileOutlineDao = fileOperationMapper.getDeriveFileOutlineDao(docFileIndexPojo);
            // 设置groupId替换用的键值
            HashMap<String, String> groupIdMap = new HashMap<>();
            for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                if (outLinePojo.getOutlineReplaceGroupId() == null) {
                    continue;
                }
                String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
            }
            String sql = "";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = formatter.format(new Date());
            for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                j++;
                // 重新排序
                outLinePojo.setOutlineOrder(String.valueOf(j));
                String outlineReplaceGroupId = null;
                String color = null;
                if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                    outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                }
                if (outLinePojo.getColor() != null) {
                    color = "\"" + outLinePojo.getColor() + "\"";
                }
                sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
//                System.out.println("sql : " + sql);
                fileOperationMapper.addDeriveFileOutlineDao(sql);
            }
            // 创建内容
            List<ContentPojo> deriveFileContentDao = fileOperationMapper.getDeriveFileContentDao(docFileIndexPojo);
            sql = "";
            for (ContentPojo contentPojo : deriveFileContentDao) {
                String contentText = contentPojo.getContentText();
//                Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//                Elements elements = doc.select("mark");
//                Elements elementsTmp = new Elements();
//                elementsTmp.addAll(elements);
//                for (Element element : elementsTmp) {
//                    element.before(element.html());
//                    element.remove();  // 去除mark标签
//                }
//                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                contentText = contentText.replaceAll("</br>", "");
//                contentPojo.setContentText(contentText);
                sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
            }
            if (sql.length() != 0) {
                sql = sql.substring(0, sql.length() - 1);
                fileOperationMapper.addDeriveFileContentDao(sql);
            }
            // 创建参数
            fileOperationMapper.addDeriveContentParamDao(docFileIndexPojo);

            // 创建标注
            fileOperationMapper.addDeriveContentTagDao(docFileIndexPojo);

            // 创建书签
            fileOperationMapper.addDeriveContentBookmarkDao(docFileIndexPojo);

            // 创建数据管理(4张)
            fileOperationMapper.addDeriveContentDmDbDao(docFileIndexPojo);
            fileOperationMapper.addDeriveContentDmTableDao(docFileIndexPojo);
            fileOperationMapper.addDeriveContentDmColumnsDao(docFileIndexPojo);
            String hashMod1 = new HashUtils().getHashMod(docFileIndexPojo.getFileUuid() + docFileIndexPojo.getFileVersionId());
            String hashMod2 = new HashUtils().getHashMod(docFileIndexPojo.getFileParentId() + docFileIndexPojo.getOldFileVersionId());
            System.out.println("表编号为: " + hashMod1);
            splitTableMapper.addDeriveContentDmDataDao(docFileIndexPojo, hashMod1, hashMod2, dmContentDataDatabase);

            // 创建数据表(如果有)
            fileOperationMapper.addDeriveContentDbDao(docFileIndexPojo);

            // 评审工具(如果有)
            HfContentAssessPojo contentAssessInfoDao = fileOperationMapper.getContentAssessInfoDao(docFileIndexPojo.getFileParentId());
            if (contentAssessInfoDao != null) {
                // 将评审工具也复制放到新文件里
                fileOperationMapper.addDeriveFileAssessDao(docFileIndexPojo.getFileParentId(), docFileIndexPojo.getFileUuid(), docFileIndexPojo.getCreateUserId());
                fileOperationMapper.addDeriveFileAssessElementDao(docFileIndexPojo.getFileParentId(), docFileIndexPojo.getFileUuid(), docFileIndexPojo.getCreateUserId());
            }

            // 创建附属文件
            // 查找附属
            List<DocFileIndexPojo> allSubsidiaryFileListDao = fileOperationMapper.getAllSubsidiaryFileListDao(docFileIndexPojo);
            for (DocFileIndexPojo fileIndexPojo : allSubsidiaryFileListDao) {
                //        文件版本（默认V1.0）
                fileIndexPojo.setFileVersionName("V1.0");
                fileIndexPojo.setIsDraft("0");
                fileIndexPojo.setFileClass("2");
                if (fileUuidList.contains(fileIndexPojo.getFileUuid())) {  // 需要剥离出去的
                    // 获取父文件的fileUuid fileVersionId
                    fileUuid = fileIndexPojo.getFileUuid();
                    String fileName = fileIndexPojo.getFileName();
                    for (Map<String, String> map : fileList) {
                        if (fileUuid.equals(map.get("value"))) {
                            fileName = map.get("label");
                            break;
                        }
                    }
                    fileVersionId = fileIndexPojo.getFileVersionId();
                    fileIndexPojo.setFileParentId(fileUuid);
                    fileIndexPojo.setOldFileUuid(fileUuid);
                    fileIndexPojo.setOldFileVersionId(fileVersionId);
                    fileIndexPojo.setFileName(fileName);
                    // 把主文件的fileUuid写到附属文件的mainFileUuid字段中
                    fileIndexPojo.setMainFileUuid(null);
                    fileIndexPojo.setMainFileVersionId(null);
                    fileIndexPojo.setMainFileUuid2(docFileIndexPojo.getFileUuid());
                    fileIndexPojo.setMainFileVersionId2(docFileIndexPojo.getFileVersionId());
                    userId = docFileIndexPojo.getCreateUserId();
                    fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                    fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                    fileIndexPojo.setFileUuid(fileUuid);
                    fileIndexPojo.setFileVersionId(fileVersionId);

                    fileIndexPojo.setCreateTime(new Date());
//                    System.out.println("fileIndexPojo : " + JSON.toJSONString(fileIndexPojo));
                    j = 0;
                    i = 0;
                    // 创建索引
                    HashMap<String, String> modelFileMap = new HashMap<>();
                    modelFileMap.put("MB-HT", "WJ-HT");
                    modelFileMap.put("MB-JSGFS", "WJ-JSGFS");
                    modelFileMap.put("MB-ZBGG", "WJ-ZBGG");
                    modelFileMap.put("MB-ZBWJ", "WJ-ZBWJ");
                    HashMap<String, String> modelFileNameMap = new HashMap<>();
                    modelFileNameMap.put("WJ-HT", "文件-合同");
                    modelFileNameMap.put("WJ-JSGFS", "文件-技术规范书");
                    modelFileNameMap.put("WJ-ZBGG", "文件-招标公告");
                    modelFileNameMap.put("WJ-ZBWJ", "文件-招标文件");
                    if (modelFileMap.get(fileIndexPojo.getFileTypeId()) != null) {
                        fileIndexPojo.setFileTypeId(modelFileMap.get(fileIndexPojo.getFileTypeId()));
                        fileIndexPojo.setFileTypeName(modelFileNameMap.get(fileIndexPojo.getFileTypeId()));
                        i = fileOperationMapper.addDeriveFileIndexDao4(fileIndexPojo);
                    } else {
                        fileIndexPojo.setFileTypeId(docAllInfoDao.getFileTypeId());
                        i = fileOperationMapper.addDeriveFileIndexDao3(fileIndexPojo);
                    }
                    if (i == 0) {
                        FileIndexException fileIndexException = new FileIndexException();
                        fileIndexException.setCode(700);
                        throw fileIndexException;
                    }

                    // 创建版本
                    i = fileOperationMapper.addDeriveFileVersionDao3(fileIndexPojo);
                    if (i == 0) {
                        FileIndexException fileIndexException = new FileIndexException();
                        fileIndexException.setCode(701);
                        throw fileIndexException;
                    }

                    // 创建大纲
                    getDeriveFileOutlineDao = fileOperationMapper.getDeriveFileOutlineDao(fileIndexPojo);
                    // 设置groupId替换用的键值
                    groupIdMap = new HashMap<>();
                    for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                        if (outLinePojo.getOutlineReplaceGroupId() == null) {
                            continue;
                        }
                        String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                        groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
                    }
                    sql = "";
                    formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    dateString = formatter.format(new Date());
                    for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                        j++;
                        // 重新排序
                        outLinePojo.setOutlineOrder(String.valueOf(j));
                        String outlineReplaceGroupId = null;
                        String color = null;
                        if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                            outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                        }
                        if (outLinePojo.getColor() != null) {
                            color = "\"" + outLinePojo.getColor() + "\"";
                        }
                        sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
//                        System.out.println("sql : " + sql);
                        fileOperationMapper.addDeriveFileOutlineDao(sql);
                    }
                    // 创建内容
                    deriveFileContentDao = fileOperationMapper.getDeriveFileContentDao(fileIndexPojo);
                    sql = "";
                    for (ContentPojo contentPojo : deriveFileContentDao) {
                        String contentText = contentPojo.getContentText();
                        sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
                        fileOperationMapper.addDeriveFileContentDao(sql);
                    }
                    // 创建参数
                    fileOperationMapper.addDeriveContentParamDao(fileIndexPojo);

                    // 创建标注
                    fileOperationMapper.addDeriveContentTagDao(fileIndexPojo);

                    // 创建书签
                    fileOperationMapper.addDeriveContentBookmarkDao(fileIndexPojo);

                    // 创建数据管理(4张)
                    fileOperationMapper.addDeriveContentDmDbDao(fileIndexPojo);
                    fileOperationMapper.addDeriveContentDmTableDao(fileIndexPojo);
                    fileOperationMapper.addDeriveContentDmColumnsDao(fileIndexPojo);
                    hashMod1 = new HashUtils().getHashMod(fileIndexPojo.getFileUuid() + fileIndexPojo.getFileVersionId());
                    hashMod2 = new HashUtils().getHashMod(fileIndexPojo.getFileParentId() + fileIndexPojo.getOldFileVersionId());
                    System.out.println("表编号为: " + hashMod1);
                    splitTableMapper.addDeriveContentDmDataDao(fileIndexPojo, hashMod1, hashMod2, dmContentDataDatabase);

                    // 创建数据表(如果有)
                    fileOperationMapper.addDeriveContentDbDao(fileIndexPojo);
//                    if (projectId != null) {
//                        // 将剥离出的附属文件写入项目空间
//                        ProjectFilePojo projectFilePojo1 = new ProjectFilePojo();
//                        projectFilePojo1.setFileUuid(fileUuid);
//                        projectFilePojo1.setFileName(fileName);
//                        projectFilePojo1.setFileExtension(".docx");
//                        projectFilePojo1.setCreateTime(new Date());
//                        projectFilePojo1.setCreateUserId(fileIndexPojo.getCreateUserId());
//                        projectFilePojo1.setMainPerson(userId);
//                        projectFilePojo1.setPackageId(null);
//                        projectFilePojo1.setFilePath(null);
//                        projectFilePojo1.setFileInfo(fileIndexPojo.getVersionInfo());
//                        projectFilePojo1.setFileType(fileIndexPojo.getFileTypeId());
//                        projectFilePojo1.setProjectId(projectId);
//                        projectFilePojo1.setIsStruct("1");
//                        projectFilePojo1.setIsShow("0");
//                        workingTableMapper.addProjectFileDao(projectFilePojo1);
//                    } else if (batchId != null) {
//                        // 将剥离出的附属文件写入批次空间
//                        BatchFilePojo batchFilePojo1 = new BatchFilePojo();
//                        batchFilePojo1.setFileUuid(fileUuid);
//                        batchFilePojo1.setFileName(fileName);
//                        batchFilePojo1.setFileExtension(".docx");
//                        batchFilePojo1.setCreateTime(new Date());
//                        batchFilePojo1.setCreateUserId(fileIndexPojo.getCreateUserId());
//                        batchFilePojo1.setMainPerson(userId);
//                        batchFilePojo1.setPackageId(null);
//                        batchFilePojo1.setFilePath(null);
//                        batchFilePojo1.setFileInfo(fileIndexPojo.getVersionInfo());
//                        batchFilePojo1.setFileType(fileIndexPojo.getFileTypeId());
//                        batchFilePojo1.setBatchId(batchId);
//                        batchFilePojo1.setIsStruct("1");
//                        batchFilePojo1.setIsShow("0");
//                        workingTableMapper.addBatchFileDao(batchFilePojo1);
//                    }

                    // 收件箱增加记录
                    HfProjectInboxPojo hfProjectInboxPojo = new HfProjectInboxPojo();
                    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                    hfProjectInboxPojo.setUuid(uuid);
                    hfProjectInboxPojo.setFileUuid(fileIndexPojo.getFileUuid());
                    hfProjectInboxPojo.setFileVersionId(fileIndexPojo.getFileVersionId());
                    hfProjectInboxPojo.setFileName(fileIndexPojo.getFileName());
                    hfProjectInboxPojo.setFileTypeId(fileIndexPojo.getFileTypeId());
                    hfProjectInboxPojo.setProjectId(projectId);
                    hfProjectInboxPojo.setBatchId(batchId);
                    hfProjectInboxPojo.setStatusId("1");
                    hfProjectInboxPojo.setOldFileUuid(fileIndexPojo.getOldFileUuid());
                    hfProjectInboxPojo.setCreateUserId(userId);
                    fileOperationMapper.newDeliverFileDao(hfProjectInboxPojo);

                } else {  // 依然作为附属挂着的
                    // 获取父文件的fileUuid fileVersionId
                    fileUuid = fileIndexPojo.getFileUuid();
                    fileVersionId = fileIndexPojo.getFileVersionId();
                    fileIndexPojo.setFileParentId(fileUuid);
                    fileIndexPojo.setOldFileVersionId(fileVersionId);
                    // 把主文件的fileUuid写到附属文件的mainFileUuid字段中
                    fileIndexPojo.setMainFileUuid(docFileIndexPojo.getFileUuid());
                    fileIndexPojo.setMainFileVersionId(docFileIndexPojo.getFileVersionId());
                    fileIndexPojo.setMainFileUuid2(null);
                    fileIndexPojo.setMainFileVersionId2(null);
                    userId = docFileIndexPojo.getCreateUserId();
                    fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                    fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                    fileIndexPojo.setFileUuid(fileUuid);
                    fileIndexPojo.setFileVersionId(fileVersionId);

                    fileIndexPojo.setCreateTime(new Date());

                    System.out.println("fileIndexPojo : " + JSON.toJSONString(fileIndexPojo));
                    j = 0;
                    i = 0;
                    // 创建索引
                    i = fileOperationMapper.addDeriveFileIndexDao3(fileIndexPojo);
                    if (i == 0) {
                        FileIndexException fileIndexException = new FileIndexException();
                        fileIndexException.setCode(700);
                        throw fileIndexException;
                    }

                    // 创建版本
                    i = fileOperationMapper.addDeriveFileVersionDao3(fileIndexPojo);
                    if (i == 0) {
                        FileIndexException fileIndexException = new FileIndexException();
                        fileIndexException.setCode(701);
                        throw fileIndexException;
                    }

                    // 创建大纲
                    getDeriveFileOutlineDao = fileOperationMapper.getDeriveFileOutlineDao(fileIndexPojo);
                    // 设置groupId替换用的键值
                    groupIdMap = new HashMap<>();
                    for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                        if (outLinePojo.getOutlineReplaceGroupId() == null) {
                            continue;
                        }
                        String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                        groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
                    }
                    sql = "";
                    formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    dateString = formatter.format(new Date());
                    for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
                        j++;
                        // 重新排序
                        outLinePojo.setOutlineOrder(String.valueOf(j));
                        String outlineReplaceGroupId = null;
                        String color = null;
                        if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                            outlineReplaceGroupId = "\"" + groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) + "\"";
                        }
                        if (outLinePojo.getColor() != null) {
                            color = "\"" + outLinePojo.getColor() + "\"";
                        }
                        sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
//                        System.out.println("sql : " + sql);
                        fileOperationMapper.addDeriveFileOutlineDao(sql);
                    }
                    // 创建内容
                    deriveFileContentDao = fileOperationMapper.getDeriveFileContentDao(fileIndexPojo);
                    sql = "";
                    for (ContentPojo contentPojo : deriveFileContentDao) {
                        String contentText = contentPojo.getContentText();
                        sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
                        fileOperationMapper.addDeriveFileContentDao(sql);
                    }
                    // 创建参数
                    fileOperationMapper.addDeriveContentParamDao(fileIndexPojo);

                    // 创建标注
                    fileOperationMapper.addDeriveContentTagDao(fileIndexPojo);

                    // 创建书签
                    fileOperationMapper.addDeriveContentBookmarkDao(fileIndexPojo);

                    // 创建数据管理(4张)
                    fileOperationMapper.addDeriveContentDmDbDao(fileIndexPojo);
                    fileOperationMapper.addDeriveContentDmTableDao(fileIndexPojo);
                    fileOperationMapper.addDeriveContentDmColumnsDao(fileIndexPojo);
                    hashMod1 = new HashUtils().getHashMod(fileIndexPojo.getFileUuid() + fileIndexPojo.getFileVersionId());
                    hashMod2 = new HashUtils().getHashMod(fileIndexPojo.getFileParentId() + fileIndexPojo.getOldFileVersionId());
                    System.out.println("表编号为: " + hashMod1);
                    splitTableMapper.addDeriveContentDmDataDao(fileIndexPojo, hashMod1, hashMod2, dmContentDataDatabase);

                    // 创建数据表(如果有)
                    fileOperationMapper.addDeriveContentDbDao(fileIndexPojo);
                }
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            FileIndexException fileIndexException = new FileIndexException();
            fileIndexException.setCode(704);
            throw fileIndexException;
        }

        // 判断是否是提交操作
        fileOperationMapper.updateVersionFileFinishDao(docFileIndexPojo.getFileParentId(), docFileIndexPojo.getOldFileVersionId());
        // 查找附属
        List<DocFileIndexPojo> allSubsidiaryFileListDao = fileOperationMapper.getAllSubsidiaryFileListDao3(docFileIndexPojo.getFileParentId(), docFileIndexPojo.getOldFileVersionId());
        for (DocFileIndexPojo fileIndexPojo : allSubsidiaryFileListDao) {
            fileOperationMapper.updateVersionFileFinishDao(fileIndexPojo.getFileUuid(), fileIndexPojo.getFileVersionId());
        }


        // 收件箱增加记录
        HfProjectInboxPojo hfProjectInboxPojo = new HfProjectInboxPojo();
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        hfProjectInboxPojo.setUuid(uuid);
        hfProjectInboxPojo.setFileUuid(docFileIndexPojo.getFileUuid());
        hfProjectInboxPojo.setFileVersionId(docFileIndexPojo.getFileVersionId());
        hfProjectInboxPojo.setFileName(docFileIndexPojo.getFileName());
        hfProjectInboxPojo.setFileTypeId(docAllInfoDao.getFileTypeId());
        hfProjectInboxPojo.setProjectId(projectId);
        hfProjectInboxPojo.setBatchId(batchId);
        hfProjectInboxPojo.setStatusId("1");
        hfProjectInboxPojo.setOldFileUuid(oldFileUuid);
        hfProjectInboxPojo.setCreateUserId(userId);
        fileOperationMapper.newDeliverFileDao(hfProjectInboxPojo);

        Map<String, String> ret = new HashMap<>();
        ret.put("fileUuid", docFileIndexPojo.getFileUuid());
        ret.put("fileVersionId", docFileIndexPojo.getFileVersionId());
        ret.put("info", "投递成功");
        return TResponseVo.success(ret);

    }

    /**
     * 查看投递记录
     *
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getDeliverRecService(String fileUuid, String userId) throws Exception {
        List<HfProjectInboxPojo> deliverRecDao = fileOperationMapper.getDeliverRecDao(fileUuid, userId);
        for (HfProjectInboxPojo hfProjectInboxPojo : deliverRecDao) {
            if (hfProjectInboxPojo.getStatusId().equals("1")) {
                hfProjectInboxPojo.setStatus("已投递");
            } else if (hfProjectInboxPojo.getStatusId().equals("2")) {
                hfProjectInboxPojo.setStatus("已接收");
            } else if (hfProjectInboxPojo.getStatusId().equals("3")) {
                hfProjectInboxPojo.setStatus("已忽略");
            } else if (hfProjectInboxPojo.getStatusId().equals("4")) {
                hfProjectInboxPojo.setStatus("已拒绝");
            }
        }
        return TResponseVo.success(deliverRecDao);
    }

    /**
     * 获取数据源-辅助填写预备数据
     *
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getParamSourceDiService(String userId) throws Exception {
        HashMap<String, Object> supToolDataMap = new HashMap<>();
        supToolDataMap.put("label", "辅助工具");
        supToolDataMap.put("value", "40");
        ArrayList<HashMap<String, Object>> supToolDataChildren = new ArrayList<>();
        List<HfSupToolFormPojo> supToolListDao = workingTableMapper.getSupToolListDao();
        for (HfSupToolFormPojo hfSupToolPojo : supToolListDao) {
            HashMap<String, Object> ToolMap = new HashMap<>();
            ToolMap.put("value", hfSupToolPojo.getToolId());
            ToolMap.put("label", hfSupToolPojo.getToolName());
            // 取出预设字段
            String toolText = hfSupToolPojo.getToolText();
            if (StringUtils.isBlank(toolText)) {
                continue;
            }
            List<Map<String, String>> list = JSON.parseObject(toolText, List.class);
            ArrayList<Map<String, String>> list1 = new ArrayList<>();
            for (Map<String, String> map : list) {
                HashMap<String, String> map1 = new HashMap<>();
                map1.put("label", map.get("label"));
                map1.put("value", map.get("name"));
                list1.add(map1);
            }
            ToolMap.put("children", list1);
            supToolDataChildren.add(ToolMap);
        }
        supToolDataMap.put("children", supToolDataChildren);

        ArrayList<Map<String, Object>> ret = new ArrayList<>();
        HashMap<String, Object> m1 = new HashMap<>();
        m1.put("label", "企业信息");
        m1.put("value", "10");
        HashMap<String, Object> m2 = new HashMap<>();
        m2.put("label", "项目信息");
        m2.put("value", "30");
        ret.add(supToolDataMap);
        ret.add(m1);
        ret.add(m2);
        return TResponseVo.success(ret);
    }

    /**
     * 辅助工具填写-获取预填空数量
     *
     * @param docParamsPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getAutoInputListService(DocParamsPojo docParamsPojo) throws Exception {
        docParamsPojo.setType("all");
        List<DocParamsPojo> contentParamDao = fileOperationMapper.getContentParamDao(docParamsPojo);
        // 计算dataSource所需要的辅助填写工具大类清单
        ArrayList<String> toolIdList = new ArrayList<>();
        for (DocParamsPojo paramsPojo : contentParamDao) {
            if (paramsPojo.getDataSource() == null) {
                continue;
            }
            // 辅助工具深度
            List<String> dataSource = JSON.parseObject((String) paramsPojo.getDataSource(), List.class);
            for (int i = 0; i < dataSource.size(); i++) {
                if (i == 0) {
                    if (!dataSource.get(i).equals("40")) {  // 第一位是40 说明是辅助工具数据源  这种情况下才继续 否则不处理
                        break;
                    }
                }
                if (i == 1) {
                    if (!toolIdList.contains(dataSource.get(i))) {
                        toolIdList.add(dataSource.get(i));
                    }
                }
            }
        }
//        System.out.println("toolIdList: "+JSON.toJSONString(toolIdList));
        String toolIdListStr = new JsonKeyUtils().listToString(toolIdList, "','");
        List<HfSupToolFormPojo> toolListDao = fileOperationMapper.getToolListDao(toolIdListStr);
        List<HfSupToolFormPojo> supToolFormList = workingTableMapper.getSupToolFormListDao(docParamsPojo.getUserId());
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (HfSupToolFormPojo tool : toolListDao) {
            Map<String, Object> map = new HashMap<>();
            map.put("label", tool.getToolName());
            map.put("name", tool.getToolId());
            ArrayList<Map<String, String>> options = new ArrayList<>();
            for (HfSupToolFormPojo hfSupToolFormPojo : supToolFormList) {
                if (hfSupToolFormPojo.getToolId().equals(tool.getToolId())) {
                    Map<String, String> option = new HashMap<>();
                    option.put("label", hfSupToolFormPojo.getFormName());
                    option.put("value", hfSupToolFormPojo.getFormId());
                    options.add(option);
                }
            }
            map.put("options", options);
            list.add(map);
        }
        return TResponseVo.success(list);
    }

    /**
     * 辅助工具-自动填写
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo autoInputService(HfSupToolFormPojo hfSupToolFormPojo) throws Exception {
        HashMap<String, String> formIdMap = hfSupToolFormPojo.getFormIdMap();
        String userId = hfSupToolFormPojo.getUserId();
        String fileUuid = hfSupToolFormPojo.getFileUuid();
        String fileVersionId = hfSupToolFormPojo.getFileVersionId();
        // 获取要用的tool信息
        ArrayList<String> toolIdList = new ArrayList<>();
        for (String toolId : formIdMap.keySet()) {
            toolIdList.add(toolId);
        }
        String toolIdListStr = new JsonKeyUtils().listToString(toolIdList, "','");
        List<HfSupToolFormPojo> toolListDao = fileOperationMapper.getToolListDao(toolIdListStr);

        //TODO 把参数都抽出来  全量更新

        List<HfSupToolFormPojo> supToolFormList = workingTableMapper.getSupToolFormListDao(userId);
        int i = 0;
        int j = 0;
        HashMap<String, String> retMap = new HashMap<>();  // 用来返回被修改掉的数据
        for (String toolId : formIdMap.keySet()) {
            // 先模糊匹配
            for (HfSupToolFormPojo supToolFormPojo : toolListDao) {
                if (supToolFormPojo.getToolId().equals(toolId)) {
                    List<Map<String, Object>> toolTextList = JSON.parseObject(supToolFormPojo.getToolText(), List.class);  // 获取toolId字段中文(用于模糊匹配)
                    // 找到toolid对应填入的表单
                    HfSupToolFormPojo toolForm = null;
                    Map<String, String> formValueMap = null;
                    for (HfSupToolFormPojo toolFormPojo : supToolFormList) {
                        if (toolFormPojo.getFormId().equals(formIdMap.get(toolId))) {
                            toolForm = toolFormPojo;
                            Map<String, String> map = JSON.parseObject((String) toolForm.getFormValue(), Map.class);
                            formValueMap = map;
                            break;
                        }
                    }
                    List<Map<String, Object>> ZH_ValueMapList = new ArrayList<>();
                    for (Map<String, Object> map : toolTextList) {
                        HashMap<String, Object> ZH_ValueMap = new HashMap<>();  // ZH_ValueMap 中文-参数值
                        for (String key : formValueMap.keySet()) {
                            if (key.equals(map.get("name"))) {
                                ZH_ValueMap.put("ZH", map.get("label"));
                                ZH_ValueMap.put("paramValue", formValueMap.get(key));
                                ZH_ValueMapList.add(ZH_ValueMap);
                                break;
                            }
                        }
                    }
                    // 将 中文-参数值 更新到表中
                    for (Map<String, Object> map : ZH_ValueMapList) {
                        List<String> paramIdList = fileOperationMapper.getToolDataDao2(fileUuid, fileVersionId, (String) map.get("ZH"));// 获取要修改的数据 提出paramId
                        Integer integer = fileOperationMapper.useToolDataDao2(fileUuid, fileVersionId, (String) map.get("ZH"), (String) map.get("paramValue"));
                        j += integer;
                        // 记录被修改掉的数据
                        for (String paramId : paramIdList) {
                            retMap.put(paramId, (String) map.get("paramValue"));
                        }
                        // 修改文内参数
                        for (String paramId : paramIdList) {
                            DocParamsPojo docParamsPojo = new DocParamsPojo();
                            docParamsPojo.setFileUuid(fileUuid);
                            docParamsPojo.setFileVersionId(fileVersionId);
                            docParamsPojo.setParamsUuid(paramId);

                            List<DocParamsPojo> docParamsPojos = fileOperationMapper.getContentAllParamDao(docParamsPojo);
                            if (j != 0) {  // 说明触发了更新 不然则未触发更新
                                // 保证每个独立的段落只更新一次
                                ArrayList<DocParamsPojo> docParamsPojosTmp = new ArrayList<>();
                                String fileUuid1 = "";
                                String fileVersionId1 = "";
                                String outlineId1 = "";
                                String uuid1 = "";
                                DocParamsPojo paramsPojo1 = new DocParamsPojo();
                                String fileUuid2 = "";
                                String fileVersionId2 = "";
                                String outlineId2 = "";
                                String uuid2 = "";
                                DocParamsPojo paramsPojo2 = new DocParamsPojo();
                                for (DocParamsPojo paramsPojo : docParamsPojos) {
                                    docParamsPojosTmp.add(paramsPojo);
                                    fileUuid1 = paramsPojo.getFileUuid();
                                    fileVersionId1 = paramsPojo.getFileVersionId();
                                    outlineId1 = paramsPojo.getOutlineId();
                                    uuid1 = paramsPojo.getUuid();
                                    paramsPojo1 = paramsPojo;
                                    if (fileUuid1.equals(fileUuid2) && fileVersionId1.equals(fileVersionId2) && outlineId1.equals(outlineId2)) {
                                        paramsPojo.setUuid(uuid1 + "|" + uuid2);
                                        docParamsPojosTmp.remove(paramsPojo2);
                                    }
                                    fileUuid2 = fileUuid1;
                                    fileVersionId2 = fileVersionId1;
                                    outlineId2 = outlineId1;
                                    uuid2 = paramsPojo.getUuid();
                                    paramsPojo2 = paramsPojo1;
                                }

                                // 将参数更新至content中
                                for (DocParamsPojo paramsPojo : docParamsPojosTmp) {  // docParamsPojosTmp的size表示要更新的段落数量
                                    String contentText = paramsPojo.getContentText();

                                    Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                                    doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                                    Elements elements = doc.select("parameter[uuid~=(" + paramsPojo.getUuid() + ")]");  // paramsPojo.getUuid()是以|隔开的
                                    Elements elementsBlock = doc.select("parameterblock[uuid~=(" + paramsPojo.getUuid() + ")]");
                                    elements.addAll(elementsBlock);
                                    Elements elementsTmp = new Elements();
                                    elementsTmp.addAll(elements);
                                    for (Element element : elementsTmp) {  // elementsTmp的size表示段落中要更新几处
                                        if ((String) map.get("paramValue") == null) {
                                            element.removeAttr("value");
                                        } else {
                                            element.attr("value", (String) map.get("paramValue"));
                                        }
                                    }
                                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                                    contentText = contentText.replaceAll("</br>", "");
                                    paramsPojo.setContentText(contentText);

                                    // 搜索模式下
                                    String searchUuid = docParamsPojo.getSearchUuid();
//                                    String userId = docParamsPojo.getUserId();
                                    String outlineId = paramsPojo.getOutlineId();
                                    if (searchUuid != null) {
                                        // 更新后入库前更新一下缓存
                                        if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                                            redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                                        }

                                        doc = Jsoup.parse(contentText, Parser.xmlParser());
                                        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                                        elements = doc.select("mark");
                                        for (Element element : elements) {
                                            element.before(element.html());
                                            element.remove();  // 去除mark标签
                                        }
                                        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                                        contentText = contentText.replaceAll("</br>", "");
                                        paramsPojo.setContentText(contentText);
                                    }

                                    fileOperationMapper.writeContentParamDao(paramsPojo);
                                }
                            }
                        }
                    }
                    break;
                }
            }

            // 再精确匹配
            String formId = formIdMap.get(toolId);
            for (HfSupToolFormPojo toolForm : supToolFormList) {
                if (toolForm.getFormId().equals(formId)) {  // 找到对应的辅助工具表单
                    Map<String, String> formValue = JSON.parseObject((String) toolForm.getFormValue(), Map.class);
                    for (String key : formValue.keySet()) {
                        String paramValue = formValue.get(key); // 拿到表单中预填数据
                        ArrayList<String> strings = new ArrayList<>();
                        strings.add("40");
                        strings.add(toolId);
                        strings.add(key);
                        String dataSourceStr = JSON.toJSONString(strings);
                        List<String> paramIdList = fileOperationMapper.getToolDataDao(fileUuid, fileVersionId, dataSourceStr);// 获取要修改的数据 提出paramId
                        Integer integer = fileOperationMapper.useToolDataDao(fileUuid, fileVersionId, dataSourceStr, paramValue);
                        i += integer;
                        // 记录被修改掉的数据
                        for (String paramId : paramIdList) {
                            retMap.put(paramId, paramValue);
                        }
                        // 修改文内参数
                        for (String paramId : paramIdList) {
                            DocParamsPojo docParamsPojo = new DocParamsPojo();
                            docParamsPojo.setFileUuid(fileUuid);
                            docParamsPojo.setFileVersionId(fileVersionId);
                            docParamsPojo.setParamsUuid(paramId);

                            List<DocParamsPojo> docParamsPojos = fileOperationMapper.getContentAllParamDao(docParamsPojo);
                            if (i != 0) {  // 说明触发了更新 不然则未触发更新
                                // 保证每个独立的段落只更新一次
                                ArrayList<DocParamsPojo> docParamsPojosTmp = new ArrayList<>();
                                String fileUuid1 = "";
                                String fileVersionId1 = "";
                                String outlineId1 = "";
                                String uuid1 = "";
                                DocParamsPojo paramsPojo1 = new DocParamsPojo();
                                String fileUuid2 = "";
                                String fileVersionId2 = "";
                                String outlineId2 = "";
                                String uuid2 = "";
                                DocParamsPojo paramsPojo2 = new DocParamsPojo();
                                for (DocParamsPojo paramsPojo : docParamsPojos) {
                                    docParamsPojosTmp.add(paramsPojo);
                                    fileUuid1 = paramsPojo.getFileUuid();
                                    fileVersionId1 = paramsPojo.getFileVersionId();
                                    outlineId1 = paramsPojo.getOutlineId();
                                    uuid1 = paramsPojo.getUuid();
                                    paramsPojo1 = paramsPojo;
                                    if (fileUuid1.equals(fileUuid2) && fileVersionId1.equals(fileVersionId2) && outlineId1.equals(outlineId2)) {
                                        paramsPojo.setUuid(uuid1 + "|" + uuid2);
                                        docParamsPojosTmp.remove(paramsPojo2);
                                    }
                                    fileUuid2 = fileUuid1;
                                    fileVersionId2 = fileVersionId1;
                                    outlineId2 = outlineId1;
                                    uuid2 = paramsPojo.getUuid();
                                    paramsPojo2 = paramsPojo1;
                                }

                                // 将参数更新至content中
                                for (DocParamsPojo paramsPojo : docParamsPojosTmp) {  // docParamsPojosTmp的size表示要更新的段落数量
                                    String contentText = paramsPojo.getContentText();

                                    Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                                    doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                                    Elements elements = doc.select("parameter[uuid~=(" + paramsPojo.getUuid() + ")]");  // paramsPojo.getUuid()是以|隔开的
                                    Elements elementsTmp = new Elements();
                                    elementsTmp.addAll(elements);
                                    for (Element element : elementsTmp) {  // elementsTmp的size表示段落中要更新几处
                                        if (paramValue == null) {
                                            element.removeAttr("value");
                                        } else {
                                            element.attr("value", paramValue);
                                        }
                                    }
                                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                                    contentText = contentText.replaceAll("</br>", "");
                                    paramsPojo.setContentText(contentText);

                                    // 搜索模式下
                                    String searchUuid = docParamsPojo.getSearchUuid();
//                                    String userId = docParamsPojo.getUserId();
                                    String outlineId = paramsPojo.getOutlineId();
                                    if (searchUuid != null) {
                                        // 更新后入库前更新一下缓存
                                        if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                                            redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                                        }

                                        doc = Jsoup.parse(contentText, Parser.xmlParser());
                                        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                                        elements = doc.select("mark");
                                        for (Element element : elements) {
                                            element.before(element.html());
                                            element.remove();  // 去除mark标签
                                        }
                                        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                                        contentText = contentText.replaceAll("</br>", "");
                                        paramsPojo.setContentText(contentText);
                                    }
                                    fileOperationMapper.writeContentParamDao(paramsPojo);
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }

        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "辅助填写完成");
        ret.put("sure", String.valueOf(i));
        ret.put("notSure", String.valueOf(j));
        ret.put("retList", retMap);
        return TResponseVo.success(ret);
    }

    /**
     * 获取参数使用场景维表
     */
    @Override
    public TResponseVo getParamSaturationListService() throws Exception {
        List<LabelValuePojo> mb = fileOperationMapper.getFileTypeMBDiDao();
        return TResponseVo.success(mb);
    }


    /**
     * 参数填写历史记录
     */
    /**
     * 获取参数填写记录
     *
     * @param hfParamChangeHistoryPojo
     */
    @Override
    public TResponseVo getParamChangeHistoryService(HfParamChangeHistoryPojo hfParamChangeHistoryPojo) throws Exception {
        //        10  单选
        //        20  多选
        //        30  日期/时间
        //        40  文本
        //        50  富文本
        //        60  图片
        //        70  资源附件
        //        80  金额
        //        90  数值
        String fileUuid = hfParamChangeHistoryPojo.getFileUuid();
        String fileVersionId = hfParamChangeHistoryPojo.getFileVersionId();
        String paramsUuid = hfParamChangeHistoryPojo.getParamsUuid();
        // 寻找相关文件 (不包括自己)
        List<DocFileIndexPojo> relationFileListDao = fileOperationMapper.getRelationFileListDao(fileUuid, fileVersionId);
        // 加入自己
        DocFileIndexPojo docFileIndexPojo = new DocFileIndexPojo();
        docFileIndexPojo.setFileUuid(fileUuid);
        docFileIndexPojo.setFileVersionId(fileVersionId);
        relationFileListDao.add(docFileIndexPojo);
        List<DocParamsPojo> paramChangeHistoryDao = fileOperationMapper.getParamChangeHistoryDao(relationFileListDao, paramsUuid);
        for (DocParamsPojo paramsPojo : paramChangeHistoryDao) {
            String paramsTypeId = paramsPojo.getParamsTypeId();
            if ("40".equals(paramsTypeId) || "30".equals(paramsTypeId) || "10".equals(paramsTypeId) || "50".equals(paramsTypeId)) {
                Map<String, Object> unitValueList = new HashMap<>();
                unitValueList.put("unit", null);
                unitValueList.put("paramsText", paramsPojo.getParamsText());
                paramsPojo.setUnitValueList(unitValueList);
            } else if ("80".equals(paramsTypeId) || "90".equals(paramsTypeId)) {
                Map<String, Object> map = JSON.parseObject((String) paramsPojo.getParamsText(), Map.class);
                ArrayList<Map<String, Object>> list = new ArrayList<>();
                if (map != null) {
                    for (String o : map.keySet()) {
                        HashMap<String, Object> map1 = new HashMap<>();
                        map1.put("unit", o);
                        map1.put("paramsText", map.get(o));
                        list.add(map1);
                    }
                }
                paramsPojo.setUnitValueList(list);
            } else if ("20".equals(paramsTypeId) || "60".equals(paramsTypeId) || "70".equals(paramsTypeId) || "95".equals(paramsTypeId)) {
                List list = JSON.parseObject((String) paramsPojo.getParamsText(), List.class);
                Map<String, Object> unitValueList = new HashMap<>();
                unitValueList.put("unit", null);
                unitValueList.put("paramsText", list);
                paramsPojo.setUnitValueList(unitValueList);
            }
        }

//        ArrayList<DocParamsPojo> docParamsPojosTmp = new ArrayList<>();
//        ArrayList<String> fileVersionIdList = new ArrayList<>();
//        for (DocParamsPojo paramsPojo : paramChangeHistoryDao) {  // groupby versionId操作
//            if (fileVersionIdList.contains(paramsPojo.getFileVersionId())) {
//                for (DocParamsPojo docParamsPojo : docParamsPojosTmp) {
//                    if (docParamsPojo.getFileVersionId().equals(paramsPojo.getFileVersionId())) {  // 多个参数结果时unitValueList返回list 单一结果返回map
//                        Object unitValueList = docParamsPojo.getUnitValueList();
//                        if (unitValueList instanceof List) {
//                            List<Map<String, Object>> list = (List<Map<String, Object>>) docParamsPojo.getUnitValueList();
//                            Map<String, Object> map = new HashMap<>();
//                            map.put("unit", paramsPojo.getUnit());
//                            if ("70".equals(paramsPojo.getParamsTypeId()) || "95".equals(paramsPojo.getParamsTypeId()) || "60".equals(paramsPojo.getParamsTypeId())) {  // 如果是附件参数或矩阵参数 就把paramsText转换成数组list
//                                map.put("paramsText", JSON.parseObject((String) paramsPojo.getParamsText(), List.class) == null ? new ArrayList<>() : JSON.parseObject((String) paramsPojo.getParamsText(), List.class));
//                            } else {
//                                map.put("paramsText", paramsPojo.getParamsText());
//                            }
//                            list.add(map); // UnitValueList补充单位对应值 此时是list
//                        } else if (unitValueList instanceof Map) {
//                            List<Map<String, Object>> list = new ArrayList<>();
//                            Map<String, Object> map = new HashMap<>();
//                            map.put("unit", paramsPojo.getUnit());
//                            if ("70".equals(paramsPojo.getParamsTypeId()) || "95".equals(paramsPojo.getParamsTypeId()) || "60".equals(paramsPojo.getParamsTypeId())) {  // 如果是附件参数或矩阵参数 就把paramsText转换成数组list
//                                map.put("paramsText", JSON.parseObject((String) paramsPojo.getParamsText(), List.class) == null ? new ArrayList<>() : JSON.parseObject((String) paramsPojo.getParamsText(), List.class));
//                            } else {
//                                map.put("paramsText", paramsPojo.getParamsText());
//                            }
//                            list.add(map);
//                            list.add((Map<String, Object>) unitValueList);
//                            docParamsPojo.setUnitValueList(list); // 设置UnitValueList 此时是从map变成list
//                        }
//                        break;
//                    }
//                }
//            } else {
//                fileVersionIdList.add(paramsPojo.getFileVersionId());
//                Map<String, Object> unitValueList = new HashMap<>();
////                Map<String, Object> map = new HashMap<>();
//                unitValueList.put("unit", paramsPojo.getUnit());
//                if ("70".equals(paramsPojo.getParamsTypeId()) || "95".equals(paramsPojo.getParamsTypeId()) || "60".equals(paramsPojo.getParamsTypeId())) {  // 如果是附件参数或矩阵参数 就把paramsText转换成数组list
//                    unitValueList.put("paramsText", JSON.parseObject((String) paramsPojo.getParamsText(), List.class) == null ? new ArrayList<>() : JSON.parseObject((String) paramsPojo.getParamsText(), List.class));
//                } else {
//                    unitValueList.put("paramsText", paramsPojo.getParamsText());
//                }
////                unitValueList.add(map);
//                paramsPojo.setUnitValueList(unitValueList); // 设置UnitValueList 此时是map
//                paramsPojo.setParamsText(null);
//                paramsPojo.setUnit(null);
//                docParamsPojosTmp.add(paramsPojo);
//            }
//
//        }

        // 如果unit是单位编码 则补充成单位名称
        List<LabelValuePojo> unitDiLVDao = fileOperationMapper.getUnitDiLVDao();
        Map<String, String> unitDict = new HashMap<>();
        for (LabelValuePojo labelValuePojo : unitDiLVDao) {
            unitDict.put(labelValuePojo.getValue(), labelValuePojo.getLabel());
        }

        // 补充展示用的字段historyContent
        for (DocParamsPojo paramsPojo : paramChangeHistoryDao) {
            paramsPojo.setKey(UUID.randomUUID().toString().replaceAll("-", ""));
            if ("80".equals(paramsPojo.getParamsTypeId()) || "90".equals(paramsPojo.getParamsTypeId())) {  // 金额 数值
                Object unitValueList = paramsPojo.getUnitValueList();
                if (unitValueList instanceof List) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) paramsPojo.getUnitValueList();
                    String historyContent = "";
                    for (Map<String, Object> map : list) {
                        String unit = map.get("unit") == null ? null : String.valueOf(map.get("unit"));
                        String unitName = unitDict.get(unit);
                        String paramsText = map.get("paramsText") == null ? null : String.valueOf(map.get("paramsText"));
                        historyContent += paramsText + unitName + ";";
                    }
                    paramsPojo.setHistoryContent(historyContent);
                } else if (unitValueList instanceof Map) {
                    Map<String, Object> map = (Map) paramsPojo.getUnitValueList();
                    String historyContent = "";
                    String unit = map.get("unit") == null ? null : String.valueOf(map.get("unit"));
                    String unitName = unitDict.get(unit);
                    String paramsText = map.get("paramsText") == null ? null : String.valueOf(map.get("paramsText"));
                    historyContent += paramsText + unitName + ";";
                    paramsPojo.setHistoryContent(historyContent);
                }
            } else if ("10".equals(paramsPojo.getParamsTypeId()) || "20".equals(paramsPojo.getParamsTypeId())) { // 单选 多选
                paramsPojo.setHistoryContent(paramsPojo.getParamsChoose());
            } else if ("30".equals(paramsPojo.getParamsTypeId())) { // 时间 日期
                Object unitValueList = paramsPojo.getUnitValueList();
                if (unitValueList instanceof Map) {
                    Map<String, Object> map = (Map) paramsPojo.getUnitValueList();
                    String paramsText = map.get("paramsText") == null ? null : String.valueOf(map.get("paramsText"));
                    paramsPojo.setHistoryContent(paramsText);
                }
            } else if ("40".equals(paramsPojo.getParamsTypeId())) { // 文本
                Object unitValueList = paramsPojo.getUnitValueList();
                if (unitValueList instanceof Map) {
                    Map<String, Object> map = (Map) paramsPojo.getUnitValueList();
                    String paramsText = map.get("paramsText") == null ? null : String.valueOf(map.get("paramsText"));
                    paramsPojo.setHistoryContent(paramsText);
                }
            }
        }

        // 美化返回内容
        for (DocParamsPojo paramsPojo : paramChangeHistoryDao) {
            // 加ago时间
            Date updateTime = paramsPojo.getUpdateTime();
            String ago = new TimeFormatUtils().getAgo(updateTime);
            paramsPojo.setTimeFormat(ago);
            // 本文历史标记
            if (paramsPojo.getFileUuid().equals(fileUuid) && paramsPojo.getFileVersionId().equals(fileVersionId)) {
                paramsPojo.setDesc("来自本文");
            }
            //
            paramsPojo.setFileName("来自《" + paramsPojo.getFileName() + "》");
        }
        return TResponseVo.success(paramChangeHistoryDao);
    }

    /**
     * 清空参数填写记录
     *
     * @param hfParamChangeHistoryPojo
     */
    @Override
    public TResponseVo clearParamChangeHistoryService(HfParamChangeHistoryPojo hfParamChangeHistoryPojo) throws Exception {
        String fileUuid = hfParamChangeHistoryPojo.getFileUuid();
        String fileVersionId = hfParamChangeHistoryPojo.getFileVersionId();
        String paramsUuid = hfParamChangeHistoryPojo.getParamsUuid();
        if (StringUtils.isBlank(paramsUuid)) {
            paramsUuid = null;
        }
        fileOperationMapper.clearParamChangeHistoryDao(fileUuid, fileVersionId, paramsUuid);
        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("info", "清除参数填写记录");
        return TResponseVo.success(retMap);
    }

    /**
     * 添加批注
     *
     * @param
     */
    @Override
    public TResponseVo addAnnotateService(OutLinePojo outLinePojo) throws Exception {
        String fileUuid = outLinePojo.getFileUuid();
        String fileVersionId = outLinePojo.getFileVersionId();
        String outlineId = outLinePojo.getOutlineId();
        String annotate = outLinePojo.getAnnotate();
        // 获取现有批注
        String annotateDao = fileOperationMapper.getAnnotateDao(fileUuid, fileVersionId);
        if (StringUtils.isBlank(annotateDao)) {
            ArrayList<LabelValuePojo> list = new ArrayList<>();
            LabelValuePojo labelValuePojo = new LabelValuePojo();
            labelValuePojo.setLabel(outlineId);
            labelValuePojo.setValue(annotate);
            list.add(labelValuePojo);
            fileOperationMapper.updateAnnotateDao(fileUuid, fileVersionId, JSON.toJSONString(list));
        } else {
            List<Map<String, Object>> list = JSON.parseObject(annotateDao, List.class);
            // 如果已有该段落的批注 则更新
            Boolean flag = true;  // true表示
            for (Map<String, Object> map : list) {
                if (map.get("label") != null && map.get("label").equals(outlineId)) {
                    map.put("value", annotate);
                    flag = false;
                    break;
                }
            }
            if (flag) {
                // 如果没有该段落的批注 则新增
                Map<String, Object> map = new HashMap<>();
                map.put("label", outlineId);
                map.put("value", annotate);
                list.add(map);
            }
            fileOperationMapper.updateAnnotateDao(fileUuid, fileVersionId, JSON.toJSONString(list));
        }
        return TResponseVo.success("批注添加成功");
    }


    /**
     * 获取变更列表
     *
     * @param outLinePojo
     */
    @Override
    public TResponseVo getCompareInfoListService(OutLinePojo outLinePojo) throws Exception {
        String fileUuid = outLinePojo.getFileUuid();
        String fileVersionId = outLinePojo.getFileVersionId();
        autoCompareService(fileUuid, fileVersionId);// 对比
        DocFileVerIndexPojo compareInfoDao = fileOperationMapper.getCompareInfoDao(fileUuid, fileVersionId);

        String key1 = fileUuid + "-" + fileVersionId;
        String key2 = fileUuid + "-" + compareInfoDao.getParentsVersionId();
        // 配置初始主键
        List<String> versionDiff = new ArrayList<>();
        versionDiff.add(key2);
        versionDiff.add(key1);

        String compareInfo = (String) compareInfoDao.getCompareInfo();
        String annotate = (String) compareInfoDao.getAnnotate();
        List<Map<String, Object>> compareInfoList = JSON.parseObject(compareInfo, List.class);
        List<Map<String, Object>> annotateList = JSON.parseObject(annotate, List.class);
        if (compareInfoList != null && annotateList != null) {
            for (Map<String, Object> compareInfoPojo : compareInfoList) {
                //
                for (Map<String, Object> annotatePojo : annotateList) {
                    if (annotatePojo.get("label") != null && annotatePojo.get("label").equals(compareInfoPojo.get("outlineId"))) {
                        compareInfoPojo.put("annotate", annotatePojo.get("value"));
                        break;
                    }
                }
            }
        }
        // 设置初始主键
        if (compareInfoList != null && compareInfoList.size() != 0) {
            for (Map<String, Object> map : compareInfoList) {
                map.put("versionDiff", versionDiff);
            }
        }
        return TResponseVo.success(compareInfoList);
    }

    /**
     * 查看变更信息
     *
     * @param
     * @return
     */
    @Override
    public void getCompareInfoDetailService(String fileUuid, String fileVersionId, String compare0, String compare1, String uuid, HttpServletResponse res) throws Exception {
        String compareInfo = null;
        if (compare0 == null || compare1 == null) {
            DocFileIndexPojo versionAllInfoDao1 = fileOperationMapper.getVersionAllInfoDao1(fileUuid, fileVersionId);
            compareInfo = versionAllInfoDao1.getCompareInfo();
        } else {
            String[] split = compare0.split("-");
            fileUuid = split[0];
            fileVersionId = split[1];
            String[] split2 = compare1.split("-");
            String fileUuid2 = split2[0];
            String fileVersionId2 = split2[1];
            Object o = redisUtils.get(envName + "handCompareService-" + fileUuid + fileVersionId + fileUuid2 + fileVersionId2);
            if (o != null) {
                // 续租
                redisUtils.set(envName + "handCompareService-" + fileUuid + fileVersionId + fileUuid2 + fileVersionId2, o, 600);
                compareInfo = (String) o;
            } else {
                throw new WorkTableException("文档对比结果超时,请重新对比");
            }
        }

        List<Map<String, Object>> list = JSON.parseObject(compareInfo, List.class);
        String contentTextNew = "";
        String contentTextOld = "";
        for (Map<String, Object> map : list) {
            if (map.get("uuid").equals(uuid)) {
                contentTextNew = map.get("contentTextNew") == null ? "" : (String) map.get("contentTextNew");
                contentTextOld = map.get("contentTextOld") == null ? "" : (String) map.get("contentTextOld");
                break;
            }
        }

//        1、如果 n 大于 0，代表分割字符串后数组的最大长度，则模式将被最多应用 n  - 1 次，数组的长度将不会大于 n ，而且数组的最后一项将包含所有超出最后匹配的定界符的输入。
//        2、如果 n 为非正，代表获取数组所有值，不会丢弃末尾空值，那么模式将被应用尽可能多的次数，而且数组可以是任何长度。
//        3、如果 n 为 0，那么模式将被应用尽可能多的次数，数组可以是任何长度，并且结尾空字符串将被丢弃。
        String[] split = contentTextOld.split("</p>|</h[0-9]+>",-1);  // delHtmlTags时留的</p>和</h12345678>等
        List<String> strings = Arrays.asList(split);
        String[] split2 = contentTextNew.split("</p>|</h[0-9]+>",-1);
        List<String> strings2 = Arrays.asList(split2);

        DiffHandleUtils.generateDiffHtml3(strings, strings2, res);
//        List<String> list1 = DiffHandleUtils.diffString(strings, strings2);
//        DiffHandleUtils.generateDiffHtml(list1, res);
    }

    /**
     * 自动对比保存记录
     *
     * @param
     */
    @Override
    public void autoCompareService(String fileUuid, String fileVersionId) throws Exception {
        // 版本数据变更记录
        DocFileVerIndexPojo draftVersion = fileOperationMapper.getDraftVersionIdDao(fileUuid);
        List<Map<String, Object>> list = new ArrayList<>(); // 记录变更数据 用户更新数据库
        if (draftVersion == null) {

        } else {
            String parentsVersionId = draftVersion.getParentsVersionId();
            String fileUuid2 = fileUuid;
            String fileVersionId2 = fileVersionId;
            DocFileVerIndexPojo versionInfoDao = fileOperationMapper.getVersionInfoDao(fileUuid, parentsVersionId);
            if (versionInfoDao == null || StringUtils.isBlank(fileUuid) || StringUtils.isBlank(parentsVersionId) || StringUtils.isBlank(fileUuid2) || StringUtils.isBlank(fileVersionId2)) {

            } else {
                // 异步对比
                if (compareLock.get()) {
                    compareLock.set(false);

                    try {
                        List<OutLinePojo> outlineListDao = fileOperationMapper.getOutlineAndContentListDao(fileUuid, parentsVersionId);  // old
                        List<OutLinePojo> outlineListDao2 = fileOperationMapper.getOutlineAndContentListDao(fileUuid2, fileVersionId2);  // new
                        for (OutLinePojo outLinePojo : outlineListDao) {  // old
                            Boolean flag = true;  // true 说明新文档删除了该段落
                            for (OutLinePojo outLinePojo2 : outlineListDao2) {  // new
                                if (outLinePojo.getOutlineId().equals(outLinePojo2.getOutlineId())) {
                                    flag = false;
                                    String contentText1 = outLinePojo.getContentText();
                                    String contentText2 = outLinePojo2.getContentText();
                                    if (contentText1 != null && contentText2 != null) {  // 不相等时才比较
                                        String contentTextDelTags = new JsonKeyUtils().delHtmlTags(contentText1);
                                        String contentTextDelTags2 = new JsonKeyUtils().delHtmlTags(contentText2);
                                        if (!contentTextDelTags.equals(contentTextDelTags2)) {
                                            // 记录变更信息
                                            HashMap<String, Object> map = new HashMap<>();
                                            ArrayList<String> versionDiff = new ArrayList<>();
                                            versionDiff.add(versionInfoDao.getFileVersionName());
                                            versionDiff.add(draftVersion.getFileVersionName());
                                            map.put("versionDiff", versionDiff);
                                            map.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                                            map.put("label", "段落变更：" + outLinePojo2.getOutlineText());
                                            map.put("outlineId", outLinePojo2.getOutlineId());
                                            map.put("contentTextOld", contentTextDelTags);
                                            map.put("contentTextNew", contentTextDelTags2);
                                            map.put("outlineOrder", outLinePojo2.getOutlineOrder());
//                                            map.put("value", comparePathLocal + "/" + outLinePojo2.getFileUuid() + "/" + outLinePojo2.getFileVersionId() + "/" + outLinePojo2.getOutlineId() + ".html");
                                            list.add(map);
                                        }
                                    }
                                }
                            }
                            if (flag) {
                                HashMap<String, Object> map = new HashMap<>();
                                ArrayList<String> versionDiff = new ArrayList<>();
                                versionDiff.add(versionInfoDao.getFileVersionName());
                                versionDiff.add(draftVersion.getFileVersionName());
                                map.put("versionDiff", versionDiff);
                                map.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                                map.put("label", "段落删除：" + outLinePojo.getOutlineText());
                                map.put("outlineId", outLinePojo.getOutlineId());
                                map.put("contentTextOld", new JsonKeyUtils().delHtmlTags(outLinePojo.getContentText()));
                                map.put("contentTextNew", " ");
                                map.put("outlineOrder", outLinePojo.getOutlineOrder());
//                                map.put("value", "");
                                list.add(map);
                            }
                        }
                        // 寻找新增段落
                        for (OutLinePojo outLinePojo2 : outlineListDao2) {  // new
                            Boolean flag = true;  // true 说明新文档删除了该段落
                            for (OutLinePojo outLinePojo : outlineListDao) {  // old
                                if (outLinePojo.getOutlineId().equals(outLinePojo2.getOutlineId())) {
                                    flag = false;
                                    break;
                                }
                            }
                            if (flag) {
                                HashMap<String, Object> map = new HashMap<>();
                                ArrayList<String> versionDiff = new ArrayList<>();
                                versionDiff.add(versionInfoDao.getFileVersionName());
                                versionDiff.add(draftVersion.getFileVersionName());
                                map.put("versionDiff", versionDiff);
                                map.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                                map.put("label", "段落新增：" + outLinePojo2.getOutlineText());
                                map.put("outlineId", outLinePojo2.getOutlineId());
                                map.put("contentTextOld", "");
                                map.put("contentTextNew", new JsonKeyUtils().delHtmlTags(outLinePojo2.getContentText()));
                                map.put("outlineOrder", outLinePojo2.getOutlineOrder());
//                                map.put("value", "");
                                list.add(map);
                            }
                        }
                        // list按照段落顺序排序
                        List<Map<String, Object>> list1 = new JsonKeyUtils().orderMapList3_1(list, "outlineOrder");

                        fileOperationMapper.updateVersionFileIndexDao4(fileUuid, fileVersionId, JSON.toJSONString(list1), new Date());
                    } catch (Exception e) {

                    } finally {
                        compareLock.set(true);
                    }

//                    CancellableThread<Object> objectCancellableThread = new CancellableThread<>(new CancellableThread.Callback<Object>() {
//                        @Override
//                        public void onComplete(Object result) {
//                            try {
//                                fileOperationMapper.updateVersionFileIndexDao4(fileUuid, fileVersionId, JSON.toJSONString(list), new Date());
//                            } finally {
//                                compareLock.set(true);
//                            }
//                        }
//
//                        @Override
//                        public Object onExecute() {
//                            List<OutLinePojo> outlineListDao = fileOperationMapper.getOutlineAndContentListDao(fileUuid, parentsVersionId);  // old
//                            List<OutLinePojo> outlineListDao2 = fileOperationMapper.getOutlineAndContentListDao(fileUuid2, fileVersionId2);  // new
//                            for (OutLinePojo outLinePojo : outlineListDao) {  // old
//                                Boolean flag = true;  // true 说明新文档删除了该段落
//                                for (OutLinePojo outLinePojo2 : outlineListDao2) {  // new
//                                    if (outLinePojo.getOutlineId().equals(outLinePojo2.getOutlineId())) {
//                                        flag = false;
//                                        String contentText1 = outLinePojo.getContentText();
//                                        String contentText2 = outLinePojo2.getContentText();
//                                        if (contentText1 != null && contentText2 != null) {  // 不相等时才比较
//                                            String contentTextDelTags = new JsonKeyUtils().delHtmlTags(contentText1);
//                                            String contentTextDelTags2 = new JsonKeyUtils().delHtmlTags(contentText2);
//                                            if (!contentTextDelTags.equals(contentTextDelTags2)) {
//
//                                                // 记录变更信息
//                                                HashMap<String, Object> map = new HashMap<>();
//                                                ArrayList<String> versionDiff = new ArrayList<>();
//                                                versionDiff.add(versionInfoDao.getFileVersionName());
//                                                versionDiff.add(draftVersion.getFileVersionName());
//                                                map.put("versionDiff", versionDiff);
//                                                map.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
//                                                map.put("label", "段落变更：" + outLinePojo2.getOutlineText());
//                                                map.put("outlineId", outLinePojo2.getOutlineId());
//                                                map.put("contentTextOld", contentTextDelTags);
//                                                map.put("contentTextNew", contentTextDelTags2);
//                                                map.put("value", comparePathLocal + "/" + outLinePojo2.getFileUuid() + "/" + outLinePojo2.getFileVersionId() + "/" + outLinePojo2.getOutlineId() + ".html");
//                                                list.add(map);
//                                            }
//                                        }
//                                    }
//                                }
//                                if (flag) {
//                                    HashMap<String, Object> map = new HashMap<>();
//                                    ArrayList<String> versionDiff = new ArrayList<>();
//                                    versionDiff.add(versionInfoDao.getFileVersionName());
//                                    versionDiff.add(draftVersion.getFileVersionName());
//                                    map.put("versionDiff", versionDiff);
//                                    map.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
//                                    map.put("label", "段落删除：" + outLinePojo.getOutlineText());
//                                    map.put("outlineId", outLinePojo.getOutlineId());
//                                    map.put("value", "");
//                                    list.add(map);
//                                }
//                            }
//                            // 寻找新增段落
//                            for (OutLinePojo outLinePojo2 : outlineListDao2) {  // new
//                                Boolean flag = true;  // true 说明新文档删除了该段落
//                                for (OutLinePojo outLinePojo : outlineListDao) {  // old
//                                    if (outLinePojo.getOutlineId().equals(outLinePojo2.getOutlineId())) {
//                                        flag = false;
//                                        break;
//                                    }
//                                }
//                                if (flag) {
//                                    HashMap<String, Object> map = new HashMap<>();
//                                    ArrayList<String> versionDiff = new ArrayList<>();
//                                    versionDiff.add(versionInfoDao.getFileVersionName());
//                                    versionDiff.add(draftVersion.getFileVersionName());
//                                    map.put("versionDiff", versionDiff);
//                                    map.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
//                                    map.put("label", "段落新增：" + outLinePojo2.getOutlineText());
//                                    map.put("outlineId", outLinePojo2.getOutlineId());
//                                    map.put("value", "");
//                                    list.add(map);
//                                }
//                            }
//
//                            return null;
//                        }
//                    });
//                    objectCancellableThread.start();
                } else {
                    System.out.println("请求忽略" + new Date());
                }
            }
        }

    }

    /**
     * 手动对比
     *
     * @param fileUuid
     * @param fileVersionId
     * @param fileUuid2
     * @param fileVersionId2
     */
    @Override
    public TResponseVo handCompareService(String fileUuid, String fileVersionId, String fileUuid2, String fileVersionId2) throws Exception {
        Object o = redisUtils.get(envName + "handCompareService-" + fileUuid + fileVersionId + fileUuid2 + fileVersionId2);
        if (o != null) {
            // 续租
            redisUtils.set(envName + "handCompareService-" + fileUuid + fileVersionId + fileUuid2 + fileVersionId2, o, 600);
            String compareInfo = (String) o;
            List list = JSON.parseObject(compareInfo, List.class);
            return TResponseVo.success(list);
        }

        List<OutLinePojo> outlineListDao = fileOperationMapper.getOutlineAndContentListDao(fileUuid, fileVersionId);
        List<OutLinePojo> outlineListDao2 = fileOperationMapper.getOutlineAndContentListDao(fileUuid2, fileVersionId2);
        List<Map<String, Object>> list = new ArrayList<>(); // 记录变更数据 用户更新数据库
        for (OutLinePojo outLinePojo : outlineListDao) {  // old
            Boolean flag = true;  // true 说明新文档删除了该段落
            for (OutLinePojo outLinePojo2 : outlineListDao2) {  // new
                if (outLinePojo.getOutlineId().equals(outLinePojo2.getOutlineId())) {
                    flag = false;
                    String contentText1 = outLinePojo.getContentText();
                    String contentText2 = outLinePojo2.getContentText();
                    if (contentText1 != null && contentText2 != null) {  // 不相等时才比较
                        String contentTextDelTags = new JsonKeyUtils().delHtmlTags(contentText1);
                        String contentTextDelTags2 = new JsonKeyUtils().delHtmlTags(contentText2);
                        if (!contentTextDelTags.equals(contentTextDelTags2)) {
                            // 记录变更信息
                            HashMap<String, Object> map = new HashMap<>();
                            ArrayList<String> versionDiff = new ArrayList<>();
                            versionDiff.add(fileUuid + "-" + fileVersionId);
                            versionDiff.add(fileUuid2 + "-" + fileVersionId2);
                            map.put("versionDiff", versionDiff);
                            map.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                            map.put("label", "段落变更：" + outLinePojo2.getOutlineText());
                            map.put("outlineId", outLinePojo2.getOutlineId());
                            map.put("contentTextOld", contentTextDelTags);
                            map.put("contentTextNew", contentTextDelTags2);
                            map.put("outlineOrder", outLinePojo2.getOutlineOrder());
//                            map.put("value", comparePathLocal + "/" + outLinePojo2.getFileUuid() + "/" + outLinePojo2.getFileVersionId() + "/" + outLinePojo2.getOutlineId() + ".html");
                            list.add(map);
                        }
                    }
                }
            }
            if (flag) {
                HashMap<String, Object> map = new HashMap<>();
                ArrayList<String> versionDiff = new ArrayList<>();
                versionDiff.add(fileUuid + "-" + fileVersionId);
                versionDiff.add(fileUuid2 + "-" + fileVersionId2);
                map.put("versionDiff", versionDiff);
                map.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                map.put("label", "段落删除：" + outLinePojo.getOutlineText());
                map.put("outlineId", outLinePojo.getOutlineId());
                map.put("contentTextOld", new JsonKeyUtils().delHtmlTags(outLinePojo.getContentText()));
                map.put("contentTextNew", " ");
                map.put("outlineOrder", outLinePojo.getOutlineOrder());
//                map.put("value", "");
                list.add(map);
            }
        }
        // 寻找新增段落
        for (OutLinePojo outLinePojo2 : outlineListDao2) {  // new
            Boolean flag = true;  // true 说明新文档删除了该段落
            for (OutLinePojo outLinePojo : outlineListDao) {  // old
                if (outLinePojo.getOutlineId().equals(outLinePojo2.getOutlineId())) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                HashMap<String, Object> map = new HashMap<>();
                ArrayList<String> versionDiff = new ArrayList<>();
                versionDiff.add(fileUuid + "-" + fileVersionId);
                versionDiff.add(fileUuid2 + "-" + fileVersionId2);
                map.put("versionDiff", versionDiff);
                map.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                map.put("label", "段落新增：" + outLinePojo2.getOutlineText());
                map.put("outlineId", outLinePojo2.getOutlineId());
                map.put("contentTextOld", "");
                map.put("contentTextNew", new JsonKeyUtils().delHtmlTags(outLinePojo2.getContentText()));
                map.put("outlineOrder", outLinePojo2.getOutlineOrder());
//                map.put("value", "");
                list.add(map);
            }
        }
        // list按照段落顺序排序
        List<Map<String, Object>> list1 = new JsonKeyUtils().orderMapList3_1(list, "outlineOrder");


        // 将结果缓存至redis
        if (list1 != null && list1.size() != 0) {
            redisUtils.set(envName + "handCompareService-" + fileUuid + fileVersionId + fileUuid2 + fileVersionId2, JSON.toJSONString(list1), 600);
        }
        return TResponseVo.success(list1);
    }

    /**
     * 获取可对比记录
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getCompareVersionListService(String fileUuid, String fileVersionId) throws Exception {
        List<LabelValuePojo> compareVersionListDao = fileOperationMapper.getCompareVersionListDao(fileUuid, fileVersionId);
        LabelValuePojo compareVersionList2Dao = fileOperationMapper.getCompareVersionList2Dao(fileUuid);
        if (compareVersionList2Dao != null) {
            compareVersionListDao.add(0, compareVersionList2Dao);
        }
        return TResponseVo.success(compareVersionListDao);
    }

    /**
     * 模型定义评标要素-总体方案
     *
     * @param hfContentAssessPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo confirmAssessTotalPlanService(HfContentAssessPojo hfContentAssessPojo) throws Exception {
        String fileUuid = hfContentAssessPojo.getFileUuid();
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
        }
        String createUserId = hfContentAssessPojo.getCreateUserId();
        // 先清空以前的方案
        fileOperationMapper.delAssessTotalPlanDao(fileUuid);
        fileOperationMapper.delAssessTotalPlanElementDao(fileUuid);
        // 初评的所有方案集合
//        List<HfAssessModelPojo> assessModelListDao = workingTableMapper.getAssessModelListDao(null);
        List<Map<String, Object>> assessModelList = (List) hfContentAssessPojo.getAssessModelList();
        for (Map<String, Object> map : assessModelList) {  // 增加模型类型
            List<String> assessIdList = (List) map.get("value");
            String assessIdListStr = new JsonKeyUtils().listToString(assessIdList, "','");
            String tabUuid = UUID.randomUUID().toString().replaceAll("-", "");
            fileOperationMapper.copyAssessElementDao(assessIdListStr, fileUuid, createUserId, tabUuid);
            map.put("value", tabUuid);
            map.put("assessIdList", assessIdList);
            map.put("modelType", "判断");
//            for (HfAssessModelPojo hfAssessModelPojo : assessModelListDao) {
//                if (assessId.equals(hfAssessModelPojo.getAssessId())) {
//                    map.put("modelType", hfAssessModelPojo.getModelType());
//                }
//            }
        }
        hfContentAssessPojo.setAssessModelList(JSON.toJSONString(assessModelList));
        // 详评的所有方案集合
        if (hfContentAssessPojo.getAssessDetailedModelList() != null && ((List) hfContentAssessPojo.getAssessDetailedModelList()).size() != 0) {
            List<Map<String, Object>> assessDetailedModelList = (List) hfContentAssessPojo.getAssessDetailedModelList();
            for (Map<String, Object> map : assessDetailedModelList) {  // 增加模型类型
                List<String> assessIdList = (List) map.get("value");
                String assessIdListStr = new JsonKeyUtils().listToString(assessIdList, "','");
                String tabUuid = UUID.randomUUID().toString().replaceAll("-", "");
                fileOperationMapper.copyAssessElementDao(assessIdListStr, fileUuid, createUserId, tabUuid);
                map.put("value", tabUuid);
                map.put("assessIdList", assessIdList);
                map.put("modelType", "评分");
//            for (HfAssessModelPojo hfAssessModelPojo : assessModelListDao) {
//                if (assessId.equals(hfAssessModelPojo.getAssessId())) {
//                    map.put("modelType", hfAssessModelPojo.getModelType());
//                }
//            }
            }
            hfContentAssessPojo.setAssessDetailedModelList(JSON.toJSONString(assessDetailedModelList));

            // 分值构成
            if (hfContentAssessPojo.getScoreRat() == null && ((List) hfContentAssessPojo.getScoreRat()).size() == 0) {
                return TResponseVo.error("未配置分值构成比例");
            }
            List<Map<String, Object>> scoreRat = (List) hfContentAssessPojo.getScoreRat();
            int tot = 0;
            for (Map<String, Object> map : scoreRat) {
                try {
                    int s = (Integer) map.get("value");
                    tot = tot + s;
                } catch (ClassCastException e) {
                    throw new WorkTableException("比例不能为小数");
                }
            }
            if (tot != 100) {
                return TResponseVo.error("分数占比总和不为100%");
            }
            // 方案重复性判断
            ArrayList<String> strings = new ArrayList<>();
            for (Map<String, Object> map : scoreRat) {
                String label = String.valueOf(map.get("label"));
                if (strings.contains(label)) {
                    return TResponseVo.error("方案名称重复,请检查");
                } else {
                    strings.add(label);
                }
            }
            hfContentAssessPojo.setScoreRat(JSON.toJSONString(scoreRat));
        } else {
            hfContentAssessPojo.setAssessDetailedModelList(null);
            hfContentAssessPojo.setScoreRat(null);
        }

        fileOperationMapper.confirmAssessTotalPlanDao(hfContentAssessPojo);

        // 获取方案元素
//        for (Map<String, String> map : assessModelList) {
//            String assessId = map.get("value");
//            fileOperationMapper.copyAssessElementDao(assessId, fileUuid, createUserId);
//        }
        return TResponseVo.success("提交完成");
    }

    /**
     * 模型定义评标要素-获取总体方案内容
     *
     * @param hfContentAssessPojo
     */
    @Override
    public TResponseVo getAssessTotalPlanService(HfContentAssessPojo hfContentAssessPojo) throws Exception {
        String fileUuid = hfContentAssessPojo.getFileUuid();
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error("必填参数为空");
        }
        HfContentAssessPojo contentAssessInfoDao = fileOperationMapper.getContentAssessInfoDao(fileUuid);
        if (contentAssessInfoDao == null) {
            return TResponseVo.error("该文件下无评审方案");
        }
        List<HfContentAssessElementPojo> contentAssessElementListDao = fileOperationMapper.getContentAssessElementListDao(fileUuid, null);
        // 评审模型集合(初评和详评)
        String assessModelList = (String) contentAssessInfoDao.getAssessModelList();
        List<Map<String, Object>> list = JSON.parseObject(assessModelList, List.class);
        for (Map<String, Object> map : list) {
            if (map.get("assessIdList") != null) {  // 兼容老数据 没有assessIdList的情况
                List<String> assessIdList = (List<String>) map.get("assessIdList");
                map.put("value", assessIdList);
            } else {
                String tabUuid = (String) map.get("value");
                List<String> assessIdList = new ArrayList<>();
                for (HfContentAssessElementPojo hfContentAssessElementPojo : contentAssessElementListDao) {
                    if (hfContentAssessElementPojo.getTabUuid().equals(tabUuid)) {
                        assessIdList.add(hfContentAssessElementPojo.getAssessId());
                    }
                }
                map.put("value", assessIdList);
            }
        }
        contentAssessInfoDao.setAssessModelList(list);

        if (contentAssessInfoDao.getAssessDetailedModelList() != null && StringUtils.isNotBlank((String) contentAssessInfoDao.getAssessDetailedModelList())) {
            String assessDetailedModelList = (String) contentAssessInfoDao.getAssessDetailedModelList();
            List<Map<String, Object>> list2 = JSON.parseObject(assessDetailedModelList, List.class);
            for (Map<String, Object> map : list2) {
                if (map.get("assessIdList") != null) {  // 兼容老数据 没有assessIdList的情况
                    List<String> assessIdList = (List<String>) map.get("assessIdList");
                    map.put("value", assessIdList);
                } else {
                    String tabUuid = (String) map.get("value");
                    List<String> assessIdList = new ArrayList<>();
                    for (HfContentAssessElementPojo hfContentAssessElementPojo : contentAssessElementListDao) {
                        if (hfContentAssessElementPojo.getTabUuid().equals(tabUuid)) {
                            assessIdList.add(hfContentAssessElementPojo.getAssessId());
                        }
                    }
                    map.put("value", assessIdList);
                }
            }
            contentAssessInfoDao.setAssessDetailedModelList(list2);
        }
        // 分值构成
        String scoreRat = (String) contentAssessInfoDao.getScoreRat();
        List list1 = JSON.parseObject(scoreRat, List.class);
        contentAssessInfoDao.setScoreRat(list1);

        return TResponseVo.success(contentAssessInfoDao);
    }

    /**
     * 模型定义评标要素-获取备选方案下拉表
     *
     * @param hfContentAssessPojo
     */
    @Override
    public TResponseVo getAssessPlanListService(HfContentAssessPojo hfContentAssessPojo) throws Exception {
        String assessMethod = hfContentAssessPojo.getAssessMethod();
        List<LabelValuePojo> assessPlanListDao = fileOperationMapper.getAssessPlanListDao(null);
        return TResponseVo.success(assessPlanListDao);
    }

    /**
     * 模型定义评标要素-获取总体方案中的方案列表
     *
     * @param hfContentAssessPojo
     */
    @Override
    public TResponseVo getAssessElementListInPlanService(HfContentAssessPojo hfContentAssessPojo) throws Exception {
        String fileUuid = hfContentAssessPojo.getFileUuid();
        if (fileUuid == null) {
            return TResponseVo.error("必填参数为空");
        }
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
        }
        HfContentAssessPojo contentAssessInfoDao = fileOperationMapper.getContentAssessInfoDao(fileUuid);
        if (contentAssessInfoDao == null) {
            return TResponseVo.success("该文件下无评审方案");
        }
        // 初评+详评
        String assessModelList = (String) contentAssessInfoDao.getAssessModelList();
        List<Map<String, String>> list = JSON.parseObject(assessModelList, List.class);
        if (contentAssessInfoDao.getAssessDetailedModelList() != null && StringUtils.isNotBlank((String) contentAssessInfoDao.getAssessDetailedModelList())) {
            String assessDetailedModelList = (String) contentAssessInfoDao.getAssessDetailedModelList();
            List<Map<String, String>> list2 = JSON.parseObject(assessDetailedModelList, List.class);
            list.addAll(list2);
        }
        return TResponseVo.success(list);
    }

    /**
     * 模型定义评标要素-查询具体方案内容(如商务初评方案/技术初评方案等)
     *
     * @param hfContentAssessPojo
     */
    @Override
    public TResponseVo getAssessElementListService(HfContentAssessPojo hfContentAssessPojo) throws Exception {
        String fileUuid = hfContentAssessPojo.getFileUuid();
        String fileVersionId = hfContentAssessPojo.getFileVersionId();
        String assessId = hfContentAssessPojo.getAssessId();
        String elementType = hfContentAssessPojo.getElementType();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(assessId)) {
            return TResponseVo.error("必填参数为空");
        }
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        String paramNameLike = hfContentAssessPojo.getParamNameLike();
        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        List<HfContentAssessElementPojo> assessElementListDao = fileOperationMapper.getAssessElementListDao(assessId, fileUuid, elementType, paramNameLike);

        // 将本文参数都查出来
//        List<LabelValuePojo> contentParamLabelValueDao = fileOperationMapper.getContentAllParamLabelValueDao(fileUuid, fileVersionId);
        List<DocFileIndexPojo> allSubsidiaryFileListDao2 = fileOperationMapper.getAllSubsidiaryFileListDao2(fileUuid, fileVersionId);
        List<LabelValuePojo> contentParamLabelValueDao = fileOperationMapper.getContentAllParamLabelValueDao2(allSubsidiaryFileListDao2);
//        List<DocFileIndexPojo> tendModelFileInfoList = fileOperationMapper.getTendModelFileInfoDao(fileUuid, fileVersionId);  // 校验是否有投标文件模板
        List<DocFileIndexPojo> tendModelFileInfoList = new ArrayList<>();
        for (DocFileIndexPojo docFileIndexPojo : allSubsidiaryFileListDao2) {
            if ("700101".equals(docFileIndexPojo.getTemplateTypeId())) {
                tendModelFileInfoList.add(docFileIndexPojo);
            }
        }
        for (DocFileIndexPojo tendModelFileInfoDao : tendModelFileInfoList) {
            if (tendModelFileInfoDao != null) {
                String fileUuid1 = tendModelFileInfoDao.getFileUuid();
                String fileVersionId1 = tendModelFileInfoDao.getFileVersionId();
                List<LabelValuePojo> contentParamLabelValueDao1 = fileOperationMapper.getContentParamLabelValueDao(fileUuid1, fileVersionId1);
                contentParamLabelValueDao.addAll(contentParamLabelValueDao1);  // 将投标文件模板中的参数收纳进来
                // 去重
                List<LabelValuePojo> contentParamLabelValueDaoTmp = new ArrayList<>();
                contentParamLabelValueDaoTmp.addAll(contentParamLabelValueDao);
                ArrayList<String> valueList = new ArrayList<>();
                for (LabelValuePojo labelValuePojo : contentParamLabelValueDaoTmp) {
                    if (valueList.contains(labelValuePojo.getValue())) {
                        contentParamLabelValueDao.remove(labelValuePojo);
                    } else {
                        valueList.add(labelValuePojo.getValue());
                    }
                }
            }
        }
        List<LabelValuePojo> assessModelParamDao = new ArrayList<>(); // 投标人参数
        List<LabelValuePojo> assessModelParamDao2 = new ArrayList<>(); // 招标人参数
        for (LabelValuePojo labelValuePojo : contentParamLabelValueDao) {
            if ("20".equals(labelValuePojo.getType())) {
                assessModelParamDao.add(labelValuePojo);
            } else if ("10".equals(labelValuePojo.getType())) {
                assessModelParamDao2.add(labelValuePojo);
            }
        }
        // 将本文的标注都查出来(typeId=5)
        List<LabelValuePojo> contentAssessTagDao = fileOperationMapper.getContentAssessTagDao(hfContentAssessPojo);

        for (HfContentAssessElementPojo hfContentAssessElementPojo : assessElementListDao) {
            String elementStandard = (String) hfContentAssessElementPojo.getElementStandard();
            if (elementStandard != null) {
                Map<String, String> map = JSON.parseObject(elementStandard, Map.class);
                hfContentAssessElementPojo.setElementStandard(map);
            } else {
                hfContentAssessElementPojo.setElementStandard(null);
            }
            String elementStandardExtra = (String) hfContentAssessElementPojo.getElementStandardExtra();
            if (elementStandardExtra != null) {
                Map<String, String> map = JSON.parseObject(elementStandardExtra, Map.class);
                hfContentAssessElementPojo.setElementStandardExtra(map);
            } else {
                hfContentAssessElementPojo.setElementStandardExtra(null);
            }
            String quantizationStandard = (String) hfContentAssessElementPojo.getQuantizationStandard();
            if (quantizationStandard != null) {
                Map<String, String> map = JSON.parseObject(quantizationStandard, Map.class);
                hfContentAssessElementPojo.setQuantizationStandard(map);
            } else {
                hfContentAssessElementPojo.setQuantizationStandard(null);
            }
            String quantizationStandardExtra = (String) hfContentAssessElementPojo.getQuantizationStandardExtra();
            if (quantizationStandardExtra != null) {
                Map<String, String> map = JSON.parseObject(quantizationStandardExtra, Map.class);
                hfContentAssessElementPojo.setQuantizationStandardExtra(map);
            } else {
                hfContentAssessElementPojo.setQuantizationStandardExtra(null);
            }
            // 级差
            String gradation = (String) hfContentAssessElementPojo.getGradation();
            if (gradation == null) {
                hfContentAssessElementPojo.setGradation(null);
            } else {
                hfContentAssessElementPojo.setGradation(gradation);
            }
            // 阈值
            String thresholdValue = (String) hfContentAssessElementPojo.getThresholdValue();
            if (thresholdValue == null) {
                hfContentAssessElementPojo.setThresholdValue(null);
            } else {
                hfContentAssessElementPojo.setThresholdValue(JSON.parseObject(thresholdValue, List.class));
            }
            // 投标人参数
            String paramsIdList = (String) hfContentAssessElementPojo.getParamsIdList();
            if (paramsIdList == null) {
                hfContentAssessElementPojo.setParamsIdList(null);
            } else {
                List<String> list = JSON.parseObject(paramsIdList, List.class);
                hfContentAssessElementPojo.setParamsIdList(list);
                // 补充参数名
                List<String> paramsNameList = new ArrayList<>();
                for (String s : list) {
                    for (LabelValuePojo labelValuePojo : assessModelParamDao) {
                        if (s.equals(labelValuePojo.getValue())) {
                            paramsNameList.add(labelValuePojo.getLabel());
                            break;
                        }
                    }
                }
                hfContentAssessElementPojo.setParamsNameList(paramsNameList);
            }
            // 招标人参数
            Map<String, Object> map = null;
            map = (Map) hfContentAssessElementPojo.getElementStandard();
            if (map == null) {
                map = (Map) hfContentAssessElementPojo.getQuantizationStandard();
            }
            if (map == null) {
                hfContentAssessElementPojo.setStandardText(null);
            } else if ("text".equals(map.get("type"))) {  // 文本
                hfContentAssessElementPojo.setStandardText(String.valueOf(map.get("value")));
            } else if ("parameter".equals(map.get("type"))) {  // 是参数
                for (LabelValuePojo labelValuePojo : assessModelParamDao2) {
                    if (map.get("value") != null && map.get("value").equals(labelValuePojo.getValue())) {
                        hfContentAssessElementPojo.setParamsName(labelValuePojo.getLabel());
                        hfContentAssessElementPojo.setStandardText("【" + "未来填参数内容" + "】");
                        break;
                    }
                }
            } else if ("mark".equals(map.get("type"))) {  // 标记
                if (map.get("value") != null) {
                    List<String> list = (List<String>) map.get("value");
                    ArrayList<String> strings = new ArrayList<>();
                    for (String s : list) {
                        for (LabelValuePojo labelValuePojo : contentAssessTagDao) {
                            if (s.equals(labelValuePojo.getValue())) {
                                strings.add(labelValuePojo.getLabel());
                                break;
                            }
                        }
                    }
                    hfContentAssessElementPojo.setStandardText(new JsonKeyUtils().listToString(strings));
                }
            }

        }
        // 顺序
        List<HfContentAssessElementPojo> retList = new JsonKeyUtils().orderMapList6(assessElementListDao);
        return TResponseVo.success(retList);
    }


    /**
     * 模型定义评标要素-获取方案中某个元素的信息
     *
     * @param hfContentAssessElementPojo
     */
    @Override
    public TResponseVo getAssessElementInfoService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        String fileUuid = hfContentAssessElementPojo.getFileUuid();
        String assessId = hfContentAssessElementPojo.getAssessId();
        String elementId = hfContentAssessElementPojo.getElementId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(assessId) || StringUtils.isBlank(elementId)) {
            return TResponseVo.error("必填参数为空");
        }
        HfContentAssessElementPojo assessElementInfoDao = fileOperationMapper.getAssessElementInfoDao(elementId, assessId, fileUuid);

        String elementStandard = (String) assessElementInfoDao.getElementStandard();
        if (elementStandard != null) {
            Map<String, String> map = JSON.parseObject(elementStandard, Map.class);
            assessElementInfoDao.setElementStandard(map);
        } else {
            assessElementInfoDao.setElementStandard(null);
        }
        String elementStandardExtra = (String) assessElementInfoDao.getElementStandardExtra();
        if (elementStandardExtra != null) {
            Map<String, String> map = JSON.parseObject(elementStandardExtra, Map.class);
            assessElementInfoDao.setElementStandardExtra(map);
        } else {
            assessElementInfoDao.setElementStandardExtra(null);
        }
        String quantizationStandard = (String) assessElementInfoDao.getQuantizationStandard();
        if (quantizationStandard != null) {
            Map<String, String> map = JSON.parseObject(quantizationStandard, Map.class);
            assessElementInfoDao.setQuantizationStandard(map);
        } else {
            assessElementInfoDao.setQuantizationStandard(null);
        }
        String quantizationStandardExtra = (String) assessElementInfoDao.getQuantizationStandardExtra();
        if (quantizationStandardExtra != null) {
            Map<String, String> map = JSON.parseObject(quantizationStandardExtra, Map.class);
            assessElementInfoDao.setQuantizationStandardExtra(map);
        } else {
            assessElementInfoDao.setQuantizationStandardExtra(null);
        }
        // 级差
        String gradation = (String) assessElementInfoDao.getGradation();
        if (gradation == null) {
            assessElementInfoDao.setGradation(null);
        } else {
            assessElementInfoDao.setGradation(gradation);
        }
        // 阈值
        String thresholdValue = (String) assessElementInfoDao.getThresholdValue();
        if (thresholdValue == null) {
            assessElementInfoDao.setThresholdValue(null);
        } else {
            assessElementInfoDao.setThresholdValue(JSON.parseObject(thresholdValue, List.class));
        }
        // 投标人参数
        String paramsIdList = (String) assessElementInfoDao.getParamsIdList();
        if (paramsIdList == null) {
            assessElementInfoDao.setParamsIdList(null);
        } else {
            assessElementInfoDao.setParamsIdList(JSON.parseObject(paramsIdList, List.class));
        }
        return TResponseVo.success(assessElementInfoDao);
    }

    /**
     * 模型定义评标要素-编辑方案中某个元素
     *
     * @param hfContentAssessElementPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateAssessElementService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        String fileUuid = hfContentAssessElementPojo.getFileUuid();
        String assessId = hfContentAssessElementPojo.getAssessId();
        String elementId = hfContentAssessElementPojo.getElementId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(assessId) || StringUtils.isBlank(elementId)) {
            return TResponseVo.error("必填参数为空");
        }
        // 标准
        Map<String, String> quantizationStandard = (Map) hfContentAssessElementPojo.getQuantizationStandard();
        if (quantizationStandard == null) {
            hfContentAssessElementPojo.setQuantizationStandard(null);
        } else {
            String s = JSON.toJSONString(quantizationStandard);
            hfContentAssessElementPojo.setQuantizationStandard(s);
        }
        Map<String, String> quantizationStandardExtra = (Map) hfContentAssessElementPojo.getQuantizationStandardExtra();
        if (quantizationStandardExtra == null) {
            hfContentAssessElementPojo.setQuantizationStandardExtra(null);
        } else {
            String s = JSON.toJSONString(quantizationStandardExtra);
            hfContentAssessElementPojo.setQuantizationStandardExtra(s);
        }
        Map<String, String> elementStandard = (Map) hfContentAssessElementPojo.getElementStandard();
        if (elementStandard == null) {
            hfContentAssessElementPojo.setElementStandard(null);
        } else {
            String s = JSON.toJSONString(elementStandard);
            hfContentAssessElementPojo.setElementStandard(s);
        }
        Map<String, String> elementStandardExtra = (Map) hfContentAssessElementPojo.getElementStandardExtra();
        if (elementStandardExtra == null) {
            hfContentAssessElementPojo.setElementStandardExtra(null);
        } else {
            String s = JSON.toJSONString(elementStandardExtra);
            hfContentAssessElementPojo.setElementStandardExtra(s);
        }
        // 级差
        Object gradation = hfContentAssessElementPojo.getGradation();
        if (gradation == null) {
            hfContentAssessElementPojo.setGradation(null);
        } else {
            String gradationStr = String.valueOf(gradation);
            if (gradationStr.substring(0, 1).equals("-")) {
                return TResponseVo.error("级差不可为负数");
            }
            hfContentAssessElementPojo.setGradation(gradationStr);
        }
        // 阈值
        List<Object> thresholdValue = (List) hfContentAssessElementPojo.getThresholdValue();
        if (thresholdValue == null) {
            hfContentAssessElementPojo.setThresholdValue(null);
        } else {
            hfContentAssessElementPojo.setThresholdValue(JSON.toJSONString(thresholdValue));
        }
        // 投标人参数
        List<String> paramsIdList = (List) hfContentAssessElementPojo.getParamsIdList();
        if (paramsIdList == null) {
            hfContentAssessElementPojo.setParamsIdList(null);
        } else {
            hfContentAssessElementPojo.setParamsIdList(JSON.toJSONString(paramsIdList));
        }
        fileOperationMapper.updateAssessElementDao(hfContentAssessElementPojo);
        return TResponseVo.success("编辑完成");
    }

    /**
     * 模型定义评标要素-删除方案中某个元素
     *
     * @param hfContentAssessElementPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delAssessElementService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        String fileUuid = hfContentAssessElementPojo.getFileUuid();
        String assessId = hfContentAssessElementPojo.getAssessId();
        String elementId = hfContentAssessElementPojo.getElementId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(assessId) || StringUtils.isBlank(elementId)) {
            return TResponseVo.error("必填参数为空");
        }
        HfContentAssessElementPojo assessElementInfoDao = fileOperationMapper.getAssessElementInfoDao(elementId, assessId, fileUuid);
        if (assessElementInfoDao == null) {
            return TResponseVo.error("该评审元素或已删除,可刷新查看");
        }
        String order = assessElementInfoDao.getOrder();
        fileOperationMapper.updateAssessElementOrderDao(assessId, fileUuid, order); // 大于order的每个数减1

        fileOperationMapper.delAssessElementDao(elementId, assessId, fileUuid);
        return TResponseVo.success("删除完成");
    }

    /**
     * 模型定义评标要素-在方案中新增元素
     *
     * @param hfContentAssessElementPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo addAssessElementService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        String fileUuid = hfContentAssessElementPojo.getFileUuid();
        String assessId = hfContentAssessElementPojo.getAssessId();
        String elementType = hfContentAssessElementPojo.getElementType();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(assessId) || StringUtils.isBlank(elementType)) {
            return TResponseVo.error("必填参数为空");
        }
        hfContentAssessElementPojo.setTabUuid(assessId);
        // 主键
        String elementId = UUID.randomUUID().toString().replaceAll("-", "");
        hfContentAssessElementPojo.setElementId(elementId);
        // 设置元素顺序字段
        // 判断当前模型中同类评审类型下有无已存在数据 如果存在就根据最大值继续增加 若不存在从1开始
        Integer order = fileOperationMapper.getContentAssessElementListCntDao(fileUuid, assessId, elementType);
        if (order == null) {
            order = 0;
        }
        // 设置条款顺序字段
        // 先判断当前elementType是否已存在  存在就取现成的顺序
        Integer elementTypeOrder = fileOperationMapper.getContentAssessTypeCntDao(fileUuid, assessId, elementType) + 1; // 当前有多少种类
        String elementTypeOrderDao = fileOperationMapper.getContentElementTypeOrderDao(fileUuid, assessId, elementType); // 试图寻找新增类型在当前模型中的应用
        if (elementTypeOrderDao != null) {
            elementTypeOrder = Integer.valueOf(elementTypeOrderDao);
        }

        // 标准
        Map<String, String> quantizationStandard = (Map) hfContentAssessElementPojo.getQuantizationStandard();
        if (quantizationStandard == null) {
            hfContentAssessElementPojo.setQuantizationStandard(null);
        } else {
            String s = JSON.toJSONString(quantizationStandard);
            hfContentAssessElementPojo.setQuantizationStandard(s);
        }
        Map<String, String> quantizationStandardExtra = (Map) hfContentAssessElementPojo.getQuantizationStandardExtra();
        if (quantizationStandardExtra == null) {
            hfContentAssessElementPojo.setQuantizationStandardExtra(null);
        } else {
            String s = JSON.toJSONString(quantizationStandardExtra);
            hfContentAssessElementPojo.setQuantizationStandardExtra(s);
        }
        Map<String, String> elementStandard = (Map) hfContentAssessElementPojo.getElementStandard();
        if (elementStandard == null) {
            hfContentAssessElementPojo.setElementStandard(null);
        } else {
            String s = JSON.toJSONString(elementStandard);
            hfContentAssessElementPojo.setElementStandard(s);
        }
        Map<String, String> elementStandardExtra = (Map) hfContentAssessElementPojo.getElementStandardExtra();
        if (elementStandardExtra == null) {
            hfContentAssessElementPojo.setElementStandardExtra(null);
        } else {
            String s = JSON.toJSONString(elementStandardExtra);
            hfContentAssessElementPojo.setElementStandardExtra(s);
        }
        // 级差
        Object gradation = hfContentAssessElementPojo.getGradation();
        if (gradation == null) {
            hfContentAssessElementPojo.setGradation(null);
        } else {
            hfContentAssessElementPojo.setGradation(String.valueOf(gradation));
        }
        // 阈值
        List<Object> thresholdValue = (List) hfContentAssessElementPojo.getThresholdValue();
        if (thresholdValue == null) {
            hfContentAssessElementPojo.setThresholdValue(null);
        } else {
            hfContentAssessElementPojo.setThresholdValue(JSON.toJSONString(thresholdValue));
        }
        // 顺序
        order += 1;
        hfContentAssessElementPojo.setOrder(String.valueOf(order));
        // 类型顺序
        hfContentAssessElementPojo.setElementTypeOrder(String.valueOf(elementTypeOrder));

        // 投标人参数
        List<String> paramsIdList = (List) hfContentAssessElementPojo.getParamsIdList();
        if (paramsIdList == null) {
            hfContentAssessElementPojo.setParamsIdList(null);
        } else {
            hfContentAssessElementPojo.setParamsIdList(JSON.toJSONString(paramsIdList));
        }
        fileOperationMapper.addAssessElementDao(hfContentAssessElementPojo);
        return TResponseVo.success("新建完成");
    }

    /**
     * 模型定义评标要素-调整元素顺序
     *
     * @param hfContentAssessElementPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo orderAssessElementService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        String fileUuid = hfContentAssessElementPojo.getFileUuid();
        String assessId = hfContentAssessElementPojo.getAssessId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(assessId)) {
            return TResponseVo.error("必填参数为空");
        }
        List<String> elementIdList = hfContentAssessElementPojo.getElementIdList();// 新的顺序
        // 获取原顺序
//        List<HfContentAssessElementPojo> assessElementListDao = fileOperationMapper.getAssessElementListDao(assessId, fileUuid, elementType, null);
//        for (int i = 0; i < elementIdList.size(); i++) {
//            String elementId1 = elementIdList.get(i);
//            String elementId2 = assessElementListDao.get(i).getElementId();
//            System.out.println("elementId1 : "+elementId1);
//            System.out.println("elementId2 : "+elementId2);
//            if (!elementId1.equals(elementId2)) {
//                fileOperationMapper.updateAssessSingleElementOrderDao(elementId1, assessId, fileUuid,String.valueOf(i+1)); // 大于order的每个数减1
//                fileOperationMapper.updateAssessSingleElementOrderDao(elementId2, assessId, fileUuid,String.valueOf(i+2)); // 大于order的每个数减1
//                break;
//            }
//        }
        for (int i = 0; i < elementIdList.size(); i++) {
            fileOperationMapper.updateAssessSingleElementOrderDao(elementIdList.get(i), assessId, fileUuid, String.valueOf(i + 1));
        }

        return TResponseVo.success("顺序调整完成");
    }


    /**
     * 模型定义评标要素-获取总体方案的二维表
     *
     * @param hfContentAssessElementPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo useAssessTotalPlanService(HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        String fileUuid = hfContentAssessElementPojo.getFileUuid();
        String fileVersionId = hfContentAssessElementPojo.getFileVersionId();
        String assessId1 = hfContentAssessElementPojo.getAssessId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(assessId1)) {
            return TResponseVo.error("必填参数为空");
        }

        // 判断是否本文已存在
        Boolean check = hfContentAssessElementPojo.getCheck();
        if (check == null) {
            check = true;
        }
        if (check) {
            List<ContentPojo> contentPojoList = fileOperationMapper.getcontentListDao(fileUuid, fileVersionId, null);
            for (ContentPojo outLinePojo : contentPojoList) {
                String contentText = outLinePojo.getContentText();
                if (contentText.contains("</bid>")) {
                    Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                    doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                    Elements elements = doc.select("bid[id=" + assessId1 + "]");
                    if (elements.size() != 0) {
//                        String outlineId = outLinePojo.getOutlineId();
//                        OutLinePojo outlineInfoDao = fileOperationMapper.getOutlineInfoDao(outlineId, fileUuid, fileVersionId);
//                        String outlineText = outlineInfoDao.getOutlineText();
                        return TResponseVo.error(-2, outLinePojo.getOutlineId());
                    }
                }
            }
        }


        // 将本文参数都查出来
        List<LabelValuePojo> contentParamLabelValueDao = fileOperationMapper.getContentParamLabelValueDao(fileUuid, fileVersionId);
        List<DocFileIndexPojo> tendModelFileInfoList = fileOperationMapper.getTendModelFileInfoDao(fileUuid, fileVersionId);  // 校验是否有投标文件模板
        for (DocFileIndexPojo tendModelFileInfoDao : tendModelFileInfoList) {
            if (tendModelFileInfoDao != null) {
                String fileUuid1 = tendModelFileInfoDao.getFileUuid();
                String fileVersionId1 = tendModelFileInfoDao.getFileVersionId();
                List<LabelValuePojo> contentParamLabelValueDao1 = fileOperationMapper.getContentParamLabelValueDao(fileUuid1, fileVersionId1);
                contentParamLabelValueDao.addAll(contentParamLabelValueDao1);  // 将投标文件模板中的参数收纳进来
                // 去重
                List<LabelValuePojo> contentParamLabelValueDaoTmp = new ArrayList<>();
                contentParamLabelValueDaoTmp.addAll(contentParamLabelValueDao);
                ArrayList<String> valueList = new ArrayList<>();
                for (LabelValuePojo labelValuePojo : contentParamLabelValueDaoTmp) {
                    if (valueList.contains(labelValuePojo.getValue())) {
                        contentParamLabelValueDao.remove(labelValuePojo);
                    } else {
                        valueList.add(labelValuePojo.getValue());
                    }
                }
            }
        }
        List<LabelValuePojo> assessModelParamDao = new ArrayList<>(); // 投标人参数
        List<LabelValuePojo> assessModelParamDao2 = new ArrayList<>(); // 招标人参数
        for (LabelValuePojo labelValuePojo : contentParamLabelValueDao) {
            if ("20".equals(labelValuePojo.getType())) {
                assessModelParamDao.add(labelValuePojo);
            } else if ("10".equals(labelValuePojo.getType())) {
                assessModelParamDao2.add(labelValuePojo);
            }
        }
        // 将本文的标注都查出来(typeId=5)
        HfContentAssessPojo hfContentAssessPojo = new HfContentAssessPojo();
        hfContentAssessPojo.setFileUuid(fileUuid);
        hfContentAssessPojo.setFileVersionId(fileVersionId);
        List<LabelValuePojo> contentAssessTagDao = fileOperationMapper.getContentAssessTagDao(hfContentAssessPojo);

        // 获取文件下的全部评审元素
        List<HfContentAssessElementPojo> assessModelListDao = fileOperationMapper.getContentAssessElementListDao(fileUuid, assessId1);
        for (HfContentAssessElementPojo assessModelElementInfoDao : assessModelListDao) {
            String elementStandard = (String) assessModelElementInfoDao.getElementStandard();
            if (elementStandard != null) {
                Map<String, String> map = JSON.parseObject(elementStandard, Map.class);
                assessModelElementInfoDao.setElementStandard(map);
            } else {
                assessModelElementInfoDao.setElementStandard(null);
            }
            String quantizationStandard = (String) assessModelElementInfoDao.getQuantizationStandard();
            if (quantizationStandard != null) {
                Map<String, String> map = JSON.parseObject(quantizationStandard, Map.class);
                assessModelElementInfoDao.setQuantizationStandard(map);
            } else {
                assessModelElementInfoDao.setQuantizationStandard(null);
            }
            // 级差
            String gradation = (String) assessModelElementInfoDao.getGradation();
            if (gradation == null) {
                assessModelElementInfoDao.setGradation(null);
            } else {
                assessModelElementInfoDao.setGradation(gradation);
            }
            // 阈值
            String thresholdValue = (String) assessModelElementInfoDao.getThresholdValue();
            if (thresholdValue == null) {
                assessModelElementInfoDao.setThresholdValue(null);
            } else {
                assessModelElementInfoDao.setThresholdValue(JSON.parseObject(thresholdValue, List.class));
            }
            // 投标人参数
            String paramsIdList = (String) assessModelElementInfoDao.getParamsIdList();
            if (paramsIdList == null) {
                assessModelElementInfoDao.setParamsIdList(null);
            } else {
                List<String> list = JSON.parseObject(paramsIdList, List.class);
                assessModelElementInfoDao.setParamsIdList(list);
                // 补充参数名
                List<String> paramsNameList = new ArrayList<>();
                for (String s : list) {
                    for (LabelValuePojo labelValuePojo : assessModelParamDao) {
                        if (s.equals(labelValuePojo.getValue())) {
                            paramsNameList.add(labelValuePojo.getLabel());
                            break;
                        }
                    }
                }
                assessModelElementInfoDao.setParamsNameList(paramsNameList);
            }
            // 招标人参数
            Map<String, Object> map = null;
            map = (Map) assessModelElementInfoDao.getElementStandard();
            if (map == null) {
                map = (Map) assessModelElementInfoDao.getQuantizationStandard();
            }
            if (map == null) {
                assessModelElementInfoDao.setStandardText(null);
            } else if ("text".equals(map.get("type"))) {
                assessModelElementInfoDao.setStandardText(String.valueOf(map.get("value")));
            } else if ("parameter".equals(map.get("type"))) {  // 是参数
                int flag = 0;
                for (LabelValuePojo labelValuePojo : assessModelParamDao2) {
                    if (map.get("value") != null && map.get("value").equals(labelValuePojo.getValue())) {
                        flag = 1;
                        assessModelElementInfoDao.setParamsName(labelValuePojo.getLabel());
                        String paramsText = labelValuePojo.getParamsText();
                        if (StringUtils.isBlank(paramsText)) {
                            assessModelElementInfoDao.setStandardText("【" + labelValuePojo.getLabel() + "】");
                        } else {
                            assessModelElementInfoDao.setStandardText(paramsText);
                        }
                        break;
                    }
                }
                if (flag == 0) {
                    assessModelElementInfoDao.setStandardText("【文内未找到对应参数,请调整】");
                }
            } else if ("mark".equals(map.get("type"))) {  // 标记
                if (map.get("value") != null) {
                    List<String> list = (List<String>) map.get("value");
                    ArrayList<String> strings = new ArrayList<>();
                    for (String s : list) {
                        for (LabelValuePojo labelValuePojo : contentAssessTagDao) {
                            if (s.equals(labelValuePojo.getValue())) {
                                strings.add(labelValuePojo.getLabel());
                                break;
                            }
                        }
                    }
                    hfContentAssessElementPojo.setStandardText(new JsonKeyUtils().listToString(strings));
                }
            }

        }
        // 获取判断/评分
        HfContentAssessPojo contentAssessInfoDao = fileOperationMapper.getContentAssessInfoDao(fileUuid);
        if (contentAssessInfoDao == null) {
            throw new WorkTableException("未查询到评审模型,请重新配置评标工具");
        }
        String assessModelListStr = (String) contentAssessInfoDao.getAssessModelList();
        List<Map<String, String>> assessModelList = JSON.parseObject(assessModelListStr, List.class);
//        String assessDetailedModelListStr = (String) contentAssessInfoDao.getAssessDetailedModelList();
//        List<Map<String, String>> assessDetailedModelList = JSON.parseObject(assessDetailedModelListStr, List.class);

        // 创建二维数组
        ArrayList<String> elementTypeList1 = new ArrayList<>();  // 评分
        ArrayList<String> elementTypeList2 = new ArrayList<>();  // 判断
        ArrayList<List<HfContentAssessElementPojo>> lists1 = new ArrayList<>();
        ArrayList<List<HfContentAssessElementPojo>> lists2 = new ArrayList<>();
        for (HfContentAssessElementPojo hfAssessModelElementPojo : assessModelListDao) {
            // 逐一判断模型类型(判断/评分)
            String tabUuid = hfAssessModelElementPojo.getTabUuid();
            String modelType = null;
            String initialNo = null;
            for (Map<String, String> map : assessModelList) {
                if (tabUuid.equals(map.get("value"))) {
                    modelType = map.get("modelType");
                    initialNo = String.valueOf(map.get("initialNo"));
                    hfAssessModelElementPojo.setInitialNo(initialNo);
                    break;
                }
            }
            if (contentAssessInfoDao.getAssessDetailedModelList() != null && StringUtils.isNotBlank((String) contentAssessInfoDao.getAssessDetailedModelList())) {
                String assessDetailedModelListStr = (String) contentAssessInfoDao.getAssessDetailedModelList();
                List<Map<String, String>> assessDetailedModelList = JSON.parseObject(assessDetailedModelListStr, List.class);
                for (Map<String, String> map : assessDetailedModelList) {
                    if (tabUuid.equals(map.get("value"))) {
                        modelType = map.get("modelType");
                        initialNo = String.valueOf(map.get("initialNo"));
                        hfAssessModelElementPojo.setInitialNo(initialNo);
                        break;
                    }
                }
            }
            if ("评分".equals(modelType)) {  // 评分
                String elementType = hfAssessModelElementPojo.getElementType();
                if (elementTypeList1.contains(elementType)) {
                    for (List<HfContentAssessElementPojo> list : lists1) {
                        if (elementType.equals(list.get(0).getElementType())) {
                            list.add(hfAssessModelElementPojo);
                            break;
                        }
                    }
                } else {
                    ArrayList<HfContentAssessElementPojo> hfAssessModelElementList = new ArrayList<>();
                    elementTypeList1.add(elementType);
                    hfAssessModelElementList.add(hfAssessModelElementPojo);
                    lists1.add(hfAssessModelElementList);
                }
            } else {  // 判断
                String elementType = hfAssessModelElementPojo.getElementType();
                if (elementTypeList2.contains(elementType)) {
                    for (List<HfContentAssessElementPojo> list : lists2) {
                        if (elementType.equals(list.get(0).getElementType())) {
                            list.add(hfAssessModelElementPojo);
                            break;
                        }
                    }
                } else {
                    ArrayList<HfContentAssessElementPojo> hfAssessModelElementList = new ArrayList<>();
                    elementTypeList2.add(elementType);
                    hfAssessModelElementList.add(hfAssessModelElementPojo);
                    lists2.add(hfAssessModelElementList);
                }
            }
        }
        // 对二维数组中的元素排序
        ArrayList<List<HfContentAssessElementPojo>> listsTmp1 = new ArrayList<>();
        listsTmp1.addAll(lists1);
        for (List<HfContentAssessElementPojo> list : listsTmp1) {
            List<HfContentAssessElementPojo> retList = new JsonKeyUtils().orderMapList6(list);
            lists1.remove(list);
            lists1.add(retList);
        }
        ArrayList<List<HfContentAssessElementPojo>> listsTmp2 = new ArrayList<>();
        listsTmp2.addAll(lists2);
        for (List<HfContentAssessElementPojo> list : listsTmp2) {
            List<HfContentAssessElementPojo> retList = new JsonKeyUtils().orderMapList6(list);
            lists2.remove(list);
            lists2.add(retList);
        }
        // 获取条款顺序
        List<HfContentAssessElementPojo> elementTypeOrderListDao = fileOperationMapper.getContentElementTypeOrderListDao(fileUuid);
        // 按条款顺序排序
        ArrayList<List<HfContentAssessElementPojo>> listsTmp3 = new ArrayList<>();
        ArrayList<List<HfContentAssessElementPojo>> listsTmp4 = new ArrayList<>();
        for (HfContentAssessElementPojo hfAssessModelElementPojo : elementTypeOrderListDao) {
            String elementType = hfAssessModelElementPojo.getElementType();
            String assessId = hfAssessModelElementPojo.getAssessId();
            for (List<HfContentAssessElementPojo> h : lists1) {
                if (elementType.equals(h.get(0).getElementType()) && assessId.equals(h.get(0).getAssessId())) {
                    listsTmp3.add(h);
                    break;
                }
            }
            for (List<HfContentAssessElementPojo> h : lists2) {
                if (elementType.equals(h.get(0).getElementType()) && assessId.equals(h.get(0).getAssessId())) {
                    listsTmp4.add(h);
                    break;
                }
            }
        }

        // 数据拼装
        HashMap<String, Object> retMap = new HashMap<>();
        if (listsTmp3.size() != 0) {
            retMap.put("table", listsTmp3);
            retMap.put("modelType", "评分");
        } else {
            retMap.put("table", listsTmp4);
            retMap.put("modelType", "判断");
        }
        return TResponseVo.success(retMap);
    }


    /**
     * 模型定义评标要素-获取文内已使用参数列表
     *
     * @param hfContentAssessPojo
     */
    @Override
    public TResponseVo getContentAssessParamService(HfContentAssessPojo hfContentAssessPojo) throws Exception {
        String fileUuid = hfContentAssessPojo.getFileUuid();
        String fileVersionId = hfContentAssessPojo.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.error("必填参数为空");
        }
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        //优化getContentAllParamLabelValueDao
        // 将本文参数都查出来
//        List<LabelValuePojo> contentParamLabelValueDao = fileOperationMapper.getContentAllParamLabelValueDao(fileUuid, fileVersionId);
        List<DocFileIndexPojo> allSubsidiaryFileListDao2 = fileOperationMapper.getAllSubsidiaryFileListDao2(fileUuid, fileVersionId);
        List<LabelValuePojo> contentParamLabelValueDao = fileOperationMapper.getContentAllParamLabelValueDao2(allSubsidiaryFileListDao2);
//        List<DocFileIndexPojo> tendModelFileInfoList = fileOperationMapper.getTendModelFileInfoDao(fileUuid, fileVersionId);  // 校验是否有投标文件模板
        List<DocFileIndexPojo> tendModelFileInfoList = new ArrayList<>();
        for (DocFileIndexPojo docFileIndexPojo : allSubsidiaryFileListDao2) {
            if ("700101".equals(docFileIndexPojo.getTemplateTypeId())) {
                tendModelFileInfoList.add(docFileIndexPojo);
            }
        }
        for (DocFileIndexPojo tendModelFileInfoDao : tendModelFileInfoList) {
            if (tendModelFileInfoDao != null) {
                String fileUuid1 = tendModelFileInfoDao.getFileUuid();
                String fileVersionId1 = tendModelFileInfoDao.getFileVersionId();
                List<LabelValuePojo> contentParamLabelValueDao1 = fileOperationMapper.getContentParamLabelValueDao(fileUuid1, fileVersionId1);
                contentParamLabelValueDao.addAll(contentParamLabelValueDao1);  // 将投标文件模板中的参数收纳进来
                // 去重
                List<LabelValuePojo> contentParamLabelValueDaoTmp = new ArrayList<>();
                contentParamLabelValueDaoTmp.addAll(contentParamLabelValueDao);
                ArrayList<String> valueList = new ArrayList<>();
                for (LabelValuePojo labelValuePojo : contentParamLabelValueDaoTmp) {
                    if (valueList.contains(labelValuePojo.getValue())) {
                        contentParamLabelValueDao.remove(labelValuePojo);
                    } else {
                        valueList.add(labelValuePojo.getValue());
                    }
                }
            }
        }

        List<LabelValuePojo> assessModelParamDao = new ArrayList<>(); // 投标人参数
        List<LabelValuePojo> assessModelParamDao2 = new ArrayList<>(); // 招标人参数
        for (LabelValuePojo labelValuePojo : contentParamLabelValueDao) {
            if ("20".equals(labelValuePojo.getType())) {
                assessModelParamDao.add(labelValuePojo);
            } else if ("10".equals(labelValuePojo.getType())) {
                assessModelParamDao2.add(labelValuePojo);
            }
            labelValuePojo.setType(null);
        }
        if ("10".equals(hfContentAssessPojo.getParamsUseSceneId())) {
            return TResponseVo.success(assessModelParamDao2);
        } else if ("20".equals(hfContentAssessPojo.getParamsUseSceneId())) {
            return TResponseVo.success(assessModelParamDao);
        } else {
            return TResponseVo.success(new ArrayList<LabelValuePojo>());
        }
    }

    /**
     * 提交前检查合规性
     *
     * @param hfContentAssessPojo
     */
    @Override
    public TResponseVo checkBeforeConfirmService(HfContentAssessPojo hfContentAssessPojo) throws Exception {
        String fileUuid = hfContentAssessPojo.getFileUuid();
        String fileVersionId = hfContentAssessPojo.getFileVersionId();
        List<String> checkOptionsList = hfContentAssessPojo.getCheckOptionsList();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || checkOptionsList == null) {
            return TResponseVo.error("必填参数为空");
        }
        // 最终返回
        ArrayList<HashMap<String, String>> retList = new ArrayList<>();

        for (String checkOption : checkOptionsList) {
            if ("assess".equals(checkOption)) {  // 评标工具
                // 获取右侧base64
                HfContentAssessElementPojo hfContentAssessElementPojo = new HfContentAssessElementPojo();
                hfContentAssessElementPojo.setFileUuid(fileUuid);
                hfContentAssessElementPojo.setFileVersionId(fileVersionId);
                hfContentAssessElementPojo.setCheck(false);
                TResponseVo tResponseVo = useAssessTotalPlanService(hfContentAssessElementPojo);
                Map<String, List<Map>> data = (Map) tResponseVo.getData();
                String dataStr = JSON.toJSONString(data);
                String dataStrBase = Base64.getEncoder().encodeToString(dataStr.getBytes());

                // 获取左侧base64
                String remarkBase = "";
                List<ContentPojo> contentPojoList = fileOperationMapper.getcontentListDao(fileUuid, fileVersionId, null);
                for (ContentPojo outLinePojo : contentPojoList) {
                    String contentText = outLinePojo.getContentText();
                    if (contentText.contains("</bid>")) {
                        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
                        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
                        Element element = doc.selectFirst("bid");  // paramsPojo.getUuid()是以|隔开的
                        remarkBase = element.attr("remark");  // 获取remark属性中的内容
                        // 解码
//                        byte[] decodeBytes = java.util.Base64.getDecoder().decode(remarkBase);
//                        String remark = new String(decodeBytes);
                    }
                }

                // 对比左右
                HashMap<String, String> retMap = new HashMap<>();
                retMap.put("label", "assess");
                retMap.put("desc", "评标工具");
                if (remarkBase.equals(dataStrBase)) {
                    retMap.put("value", "通过");
                } else {
                    retMap.put("value", "不通过");
                }
                retList.add(retMap);

            }
        }
        return TResponseVo.success(retList);
    }

    /**
     * 模型定义评标要素-获取文内标注列表
     *
     * @param hfContentAssessPojo
     */
    @Override
    public TResponseVo getContentAssessTagService(HfContentAssessPojo hfContentAssessPojo) throws Exception {
        List<LabelValuePojo> contentAssessTagDao = fileOperationMapper.getContentAssessTagDao(hfContentAssessPojo);
        return TResponseVo.success(contentAssessTagDao);
    }


    /**
     * 参数填写清单查看
     *
     * @param docParamsPojo
     */
    @Override
    public TResponseVo getFileParamsWriteListService(DocParamsPojo docParamsPojo) throws Exception {
        // 获取当前版本的参数清单
        String outlineId = docParamsPojo.getOutlineId();
        if (outlineId == null) {
            docParamsPojo.setType("all");
        } else {
            docParamsPojo.setType(null);
        }
        String paramNameLike = docParamsPojo.getParamNameLike();
        if (StringUtils.isBlank(paramNameLike)) {
            docParamsPojo.setParamNameLike(paramNameLike);
        } else {
            docParamsPojo.setParamNameLike(StringEscapeUtils.escapeSql(paramNameLike));
        }
        List<DocParamsPojo> contentParamDao = fileOperationMapper.getContentParamDao(docParamsPojo);
        DocFileVerIndexPojo fatherVersionInfoDao = fileOperationMapper.getFatherVersionInfoDao(docParamsPojo.getFileUuid(), docParamsPojo.getFileVersionId());
        ArrayList<String> changeList = new ArrayList<>();
        if (fatherVersionInfoDao == null) {
            // 未保存过 此时填写的全部算作变化
            for (DocParamsPojo paramsPojo : contentParamDao) {  // 本版本
                if (paramsPojo.getParamsText() != null) {
                    paramsPojo.setChangeFlag(true);
                    changeList.add(paramsPojo.getParamsUuid());
                } else {
                    paramsPojo.setChangeFlag(false);
                }
            }
        } else {
            // 保存过 对比两个版本间的不同
            DocParamsPojo paramsPojo1 = new DocParamsPojo();
            paramsPojo1.setFileUuid(fatherVersionInfoDao.getFileUuid());
            paramsPojo1.setFileVersionId(fatherVersionInfoDao.getFileVersionId());
            paramsPojo1.setType("all");
            List<DocParamsPojo> contentAllParamDao = fileOperationMapper.getContentParamDao(paramsPojo1);
            for (DocParamsPojo paramsPojo : contentParamDao) {  // 本版本
                for (DocParamsPojo pojo : contentAllParamDao) {   // 上个版本
                    if (paramsPojo.getUuid().equals(pojo.getUuid())) {
                        if (paramsPojo.getParamsText() == null && pojo.getParamsText() == null) {
                            paramsPojo.setChangeFlag(false);
                        } else if (paramsPojo.getParamsText() == null && pojo.getParamsText() != null) {
                            paramsPojo.setChangeFlag(true);
                            changeList.add(paramsPojo.getParamsUuid());
                        } else if (paramsPojo.getParamsText() != null && pojo.getParamsText() == null) {
                            paramsPojo.setChangeFlag(true);
                            changeList.add(paramsPojo.getParamsUuid());
                        } else if (!paramsPojo.getParamsText().equals(pojo.getParamsText())) {
                            paramsPojo.setChangeFlag(true);
                            changeList.add(paramsPojo.getParamsUuid());
                        } else if (paramsPojo.getParamsText().equals(pojo.getParamsText())) {
                            paramsPojo.setChangeFlag(false);
                        } else {
                            paramsPojo.setChangeFlag(false);
                        }
                        break;
                    }
                }
            }
        }


        // 如果只看参数发生变动的
        String getChange = docParamsPojo.getGetChange();
        if (getChange != null) {
            List<DocParamsPojo> contentParamDaoTmp = new ArrayList<>();
            contentParamDaoTmp.addAll(contentParamDao);
            for (DocParamsPojo paramsPojo : contentParamDaoTmp) {
                if (paramsPojo.getIsChange().equals("0")) {
                    contentParamDao.remove(paramsPojo);
                }
            }
        }

        //去重
        List<DocParamsPojo> contentParamDao1 = new ArrayList<>();
        ArrayList<String> paramsIdStringList = new ArrayList<>();
        // 加显示类型的styleContent
        List<LabelValuePojo> paramTypeStyleDiDao = fileOperationMapper.getParamTypeStyleDiDao(null);
        for (DocParamsPojo paramsPojo : contentParamDao) {
            if (!paramsIdStringList.contains(paramsPojo.getParamsUuid())) {
                paramsIdStringList.add(paramsPojo.getParamsUuid());
                ArrayList<Map<String, String>> Strings = new ArrayList<>();
                Map<String, String> map = new HashMap<>();
                map.put("uuid", paramsPojo.getUuid());
                map.put("outLineId", paramsPojo.getOutlineId());
                map.put("showText", paramsPojo.getShowText());
                map.put("styleId", paramsPojo.getStyleId());
                if (paramsPojo.getStyleId() != null) {
                    for (LabelValuePojo labelValuePojo : paramTypeStyleDiDao) {
                        if (labelValuePojo.getValue().equals(paramsPojo.getStyleId())) {
                            map.put("styleContent", labelValuePojo.getLabel());
                        }
                    }
                }
                map.put("isUnderLine", paramsPojo.getIsUnderLine());
                map.put("unit", paramsPojo.getUnit());
                map.put("remark", paramsPojo.getRemark());
                map.put("outlineOrder", paramsPojo.getOutlineOrder());
                map.put("unitName", paramsPojo.getUnitName());
                map.put("paramsText", String.valueOf(paramsPojo.getParamsText()));
                map.put("matrixDisplay", paramsPojo.getMatrixDisplay());
                map.put("matrixMode", paramsPojo.getMatrixMode());
                map.put("changeFlag", paramsPojo.getChangeFlag() ? "1" : "0");
                Strings.add(map);
                paramsPojo.setUuidList(Strings);
                contentParamDao1.add(paramsPojo);
            } else {
                for (DocParamsPojo pojo : contentParamDao1) {
                    if (pojo.getParamsUuid().equals(paramsPojo.getParamsUuid())) {
                        Map<String, String> map = new HashMap<>();
                        map.put("uuid", paramsPojo.getUuid());
                        map.put("outLineId", paramsPojo.getOutlineId());
                        map.put("showText", paramsPojo.getShowText());
                        map.put("styleId", paramsPojo.getStyleId());
                        if (paramsPojo.getStyleId() != null) {
                            for (LabelValuePojo labelValuePojo : paramTypeStyleDiDao) {
                                if (labelValuePojo.getValue().equals(paramsPojo.getStyleId())) {
                                    map.put("styleContent", labelValuePojo.getLabel());
                                }
                            }
                        }
                        map.put("isUnderLine", paramsPojo.getIsUnderLine());
                        map.put("unit", paramsPojo.getUnit());
                        map.put("remark", paramsPojo.getRemark());
                        map.put("outlineOrder", paramsPojo.getOutlineOrder());
                        map.put("unitName", paramsPojo.getUnitName());
                        map.put("paramsText", String.valueOf(paramsPojo.getParamsText()));
                        map.put("matrixDisplay", paramsPojo.getMatrixDisplay());
                        map.put("matrixMode", paramsPojo.getMatrixMode());
                        map.put("changeFlag", paramsPojo.getChangeFlag() ? "1" : "0");
                        pojo.getUuidList().add(map);
                    }
                }
            }
        }
        // 设置长度信息
        for (DocParamsPojo paramsPojo : contentParamDao1) {
            paramsPojo.setUuidListLength(paramsPojo.getUuidList().size());
            List<Object> list = JSON.parseObject(paramsPojo.getParamsGroupId(), List.class);
            paramsPojo.setParamsGroupIdList(list);
            List<Object> list1 = JSON.parseObject(paramsPojo.getParamsRange(), List.class);
            paramsPojo.setParamsRangeList(list1);
            // 矩阵参数头
            List paramsColumns = JSON.parseObject((String) paramsPojo.getParamsColumns(), List.class);
            paramsPojo.setParamsColumns(paramsColumns);


            // 判断默认值是否需要转换格式
            String paramsTypeId = paramsPojo.getParamsTypeId();
            if (paramsTypeId.equals("20") || paramsTypeId.equals("60") || paramsTypeId.equals("70") || paramsTypeId.equals("95")) {
                Object defaultValue = paramsPojo.getDefaultValue();
                if (defaultValue != null) {
                    List list2 = JSON.parseObject(String.valueOf(defaultValue), List.class);
                    if (list2 != null && list2.size() != 0) {
                        paramsPojo.setDefaultValue(list2);
                    } else {
                        paramsPojo.setDefaultValue(null);
                    }
                }
            } else if (paramsTypeId.equals("90")) {  // 数值 金额
                Object defaultValue = paramsPojo.getDefaultValue();
                if (defaultValue != null) {
                    Map map1 = new HashMap();
                    try {  // 数值 金额  默认值不是map直接认为默认值无效 不返回给前端了
                        map1 = JSON.parseObject(String.valueOf(defaultValue), Map.class, Feature.NonStringKeyAsString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (map1 != null && map1.size() != 0) {
                        paramsPojo.setDefaultValue(map1);
                    } else {
                        paramsPojo.setDefaultValue(null);
                    }
                }
            } else if (paramsTypeId.equals("80")) {  // 金额
                Object defaultValue = paramsPojo.getDefaultValue();
                if (defaultValue != null) {
                    Map<Object, Object> map1 = new HashMap();
                    try {  // 数值 金额  默认值不是map直接认为默认值无效 不返回给前端了
                        map1 = JSON.parseObject(String.valueOf(defaultValue), Map.class, Feature.NonStringKeyAsString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (map1 != null && map1.size() != 0) {
                        paramsPojo.setDefaultValue(map1);
                    } else {
                        paramsPojo.setDefaultValue(null);
                    }
                }
            }
        }
        // 设置单位集合
        for (DocParamsPojo paramsPojo : contentParamDao1) {
            // 只操作数值和金额
            if (paramsPojo.getParamsTypeId().equals("80") || paramsPojo.getParamsTypeId().equals("90")) {
                List<Map<String, String>> uuidList = paramsPojo.getUuidList();
                List<Map<String, String>> unitMapList = new ArrayList<>();
                List<String> unitStrList = new ArrayList<>();
                Map<String, Double> unitAndValueMap = new HashMap<>();
                for (Map<String, String> stringStringMap : uuidList) {
                    String unitName = stringStringMap.get("unitName");
                    String unit = stringStringMap.get("unit");
                    if (unit != null && !unit.equals("") && !unitStrList.contains(unit)) {
                        // 设置单位集合
                        Map<String, String> unitMap = new HashMap<>();
                        unitMap.put("label", unitName);
                        unitMap.put("value", unit);
                        unitStrList.add(unit);
                        unitMapList.add(unitMap);
                        // 设置设置单位集合和对应的回显数据
                        unitAndValueMap.put(unit, (stringStringMap.get("paramsText").equals("null") || StringUtils.isBlank(stringStringMap.get("paramsText"))) ? null : new Double(stringStringMap.get("paramsText")));
                    }
                }
                paramsPojo.setUnitList(unitMapList);
                paramsPojo.setParamsText(unitAndValueMap);
            }
        }

        contentParamDao = contentParamDao1;
//        System.out.println(contentParamDao);
        // 加分组名字
        List<LabelValuePojo> paramGroupDiDao = fileOperationMapper.getParamGroupDiDao();
        for (DocParamsPojo paramsPojo : contentParamDao) {
            String paramsGroupId = paramsPojo.getParamsGroupId();
            List<String> list = JSON.parseObject(paramsGroupId, List.class);
            ArrayList<String> paramGroupNameList = new ArrayList<>();
            if (list != null) {
                for (String s : list) {
                    for (LabelValuePojo labelValuePojo : paramGroupDiDao) {
                        if (labelValuePojo.getValue().equals(s)) {
                            paramGroupNameList.add(labelValuePojo.getLabel());
                        }
                    }
                }
            }
            paramsPojo.setParamsGroupNameList(paramGroupNameList);

            // 如果是资源附件类型参数 若参数值为空,则置为[]
            if (paramsPojo.getParamsTypeId().equals("70")) {
                if (paramsPojo.getParamsText() == null) {
                    paramsPojo.setParamsText(new ArrayList<>());
                }
            }

            // 标记是否发生变化
            if (changeList.contains(paramsPojo.getParamsUuid())) paramsPojo.setChangeFlag(true);
        }

        // 是否全局顺序排序 (按照文章内出现参数的顺序排序 以文章为主)
        String orderMode = docParamsPojo.getOrderMode();
        if (orderMode != null && orderMode.equals("all")) {
            // 当orderMode排序模式为all时 按照全局首次出现顺序排序
            String fileUuid = docParamsPojo.getFileUuid();
            String fileVersionUuid = docParamsPojo.getFileVersionId();
            List<String> contentAllDao = fileOperationMapper.getContentAllDao(fileUuid, fileVersionUuid);
            ArrayList<String> paramOrderList = new ArrayList<>();
            for (String s : contentAllDao) {
                Document doc = Jsoup.parse(s);
                Elements parameters = doc.select("parameter");
                for (Element parameter : parameters) {
                    if (!paramOrderList.contains(String.valueOf(parameter).substring(16, 48))) {
                        paramOrderList.add(String.valueOf(parameter).substring(16, 48));
                    }
                }
            }
            List<DocParamsPojo> contentParamDaoRet = new ArrayList<>();
            for (String s : paramOrderList) {
                for (DocParamsPojo paramsPojo : contentParamDao) {
                    if (paramsPojo.getParamsUuid().equals(s)) {
                        contentParamDaoRet.add(paramsPojo);
                        break;
                    }
                }
            }
            contentParamDao = contentParamDaoRet;
        }

        // 根据出现顺序排序 即按outlineOrder排序
        for (DocParamsPojo paramsPojo : contentParamDao) {
            List<Map<String, String>> uuidList = paramsPojo.getUuidList();
            List<Map<String, String>> outlineId1 = new JsonKeyUtils().orderMapList3(uuidList, "outlineOrder");
            paramsPojo.setUuidList(outlineId1);

            // 特殊处理: 除了招标文件和投标文件 其余类型都不返回paramsUseSceneId
            if (paramsPojo.getParamsUseSaturation() != null && !"MB-ZBWJ".equals(paramsPojo.getParamsUseSaturation())) {
                paramsPojo.setParamsUseSceneId(null);
            }
        }


        // 多选的paramText返回list<String>
        List<Map<String, Object>> listRet = JSON.parseObject(JSON.toJSONString(contentParamDao), List.class);
        for (Map<String, Object> map : listRet) {
            if (map.get("paramsTypeId").equals("20") && map.get("paramsText") != null) {  // 多选
                String paramsText = String.valueOf(map.get("paramsText"));
                List<String> strings = new JsonKeyUtils().stringToList(paramsText);
                map.put("paramsText", strings);
            } else if (map.get("paramsTypeId").equals("70") && map.get("paramsText") != null) {  // 附件
                String paramsText = String.valueOf(map.get("paramsText"));
                List<String> strings = JSON.parseObject(paramsText, List.class);
                map.put("paramsText", strings);
            } else if (map.get("paramsTypeId").equals("95") && map.get("paramsText") != null) { // 矩阵
                String paramsText = String.valueOf(map.get("paramsText"));
                List<String> strings = JSON.parseObject(paramsText, List.class);
                map.put("paramsText", strings);
            } else if (map.get("paramsTypeId").equals("60") && map.get("paramsText") != null) { // 图片
                String paramsText = String.valueOf(map.get("paramsText"));
                List<String> strings = JSON.parseObject(paramsText, List.class);
                map.put("paramsText", strings);
            }
        }
        return TResponseVo.success(listRet);
    }

    /**
     * 参数清单查看
     *
     * @param hfDmContentUseTablePojo
     */
    @Override
    public TResponseVo addAndDelContentDbService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception {
//        List<DocParamsTypeStylePojo> paramTypeStyleDi2Dao = fileOperationMapper.getParamTypeStyleDi2Dao();
        String fileUuid = hfDmContentUseTablePojo.getFileUuid();
        String fileVersionId = hfDmContentUseTablePojo.getFileVersionId();
        String outlineId = hfDmContentUseTablePojo.getOutlineId();
        String createUserId = hfDmContentUseTablePojo.getCreateUserId();
        List<Map<String, Object>> actions = hfDmContentUseTablePojo.getActions();
        List<Map<String, Object>> actionsAdd = new ArrayList<>();
        List<Map<String, Object>> actionsDel = new ArrayList<>();
        // 增删区分到两个栈里
        for (Map<String, Object> action : actions) {
            if (action.get("action").equals("insert")) {
                // 新增栈
                actionsAdd.add(action);
            } else if (action.get("action").equals("delete")) {
                // 删除栈
                actionsDel.add(action);
            }
        }
        // 成对出现的uuid出栈
        List<Map<String, Object>> actionsAdd1 = new ArrayList<>();
        actionsAdd1.addAll(actionsAdd);
        List<Map<String, Object>> actionsDel1 = new ArrayList<>();
        actionsDel1.addAll(actionsDel);
        for (Map<String, Object> add : actionsAdd) {
            for (Map<String, Object> del : actionsDel) {
                if (add.get("uuid").equals(del.get("uuid"))) {
                    actionsAdd1.remove(add);
                    actionsDel1.remove(del);
                }
            }
        }
        // 获取已经被删除的本文参数
        List<String> deledParamDao = fileOperationMapper.getDeledContentUseTableDao(fileUuid, fileVersionId);
        for (Map<String, Object> stringStringMap : actionsAdd1) {
            if (deledParamDao.contains(stringStringMap.get("uuid"))) {
                // 恢复
                // 判断是否是刚才删除的uuid 如果是就直接拿回来用
                fileOperationMapper.updateContentUseTableOutlineIdDao((String) stringStringMap.get("uuid"), outlineId, fileUuid, fileVersionId);
                continue;
            }
            // 新建
            List<String> dataSource = (List) stringStringMap.get("dataSource");
            if (dataSource.size() < 2) {
                throw new WorkTableException("未指定数据库下的具体数据表或视图");
            }
            String dataSourceStr = JSON.toJSONString(dataSource);
            List<Object> fields = (List) stringStringMap.get("fields");
            String fieldsStr = JSON.toJSONString(fields);
            String name = (String) stringStringMap.get("name");
            String desc = (String) stringStringMap.get("desc");
            String uuid = (String) stringStringMap.get("uuid");
            HfDmContentUseTablePojo ht = new HfDmContentUseTablePojo();
//            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            ht.setUuid(uuid);
            ht.setName(name);
            ht.setDesc(desc);
            ht.setDataSource(dataSourceStr);
            ht.setFields(fieldsStr);
            ht.setRows(JSON.toJSONString(new ArrayList<>()));
            ht.setFileUuid(fileUuid);
            ht.setFileVersionId(fileVersionId);
            ht.setOutlineId(outlineId);
            ht.setIsDel("0");
            ht.setCreateUserId(createUserId);
            // 验证新参数名是否在本文内重名
//            Integer integer = fileOperationMapper.checkContentUseTableNameDao(ht);
//            if (integer != 0) {
//                throw new WorkTableException("已存在同名表");
//            }
            fileOperationMapper.addContentUseTableDao(ht);
        }

        // 删除刷库
        for (Map<String, Object> stringStringMap : actionsDel1) {
            String uuid = (String) stringStringMap.get("uuid");
            fileOperationMapper.delContentUseTableDao(uuid, fileUuid, fileVersionId);
        }

        return TResponseVo.success("操作完成");
    }

    /**
     * 获取数据表清单
     *
     * @param hfDmContentUseTablePojo
     */
    @Override
    public TResponseVo getContentDbService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception {
        String fileUuid = hfDmContentUseTablePojo.getFileUuid();
        String fileVersionId = hfDmContentUseTablePojo.getFileVersionId();
        // 本文数据表清单
        List<HfDmContentUseTablePojo> contentUseTableListDao = fileOperationMapper.getContentUseTableListDao(fileUuid, fileVersionId);

        // 是否全局顺序排序 (按照文章内出现参数的顺序排序 以文章为主)
        if (true) {
            // 当orderMode排序模式为all时 按照全局首次出现顺序排序
            List<String> contentAllDao = fileOperationMapper.getContentAllDao(hfDmContentUseTablePojo.getFileUuid(), hfDmContentUseTablePojo.getFileVersionId());
            ArrayList<String> dbOrderList = new ArrayList<>();
            for (String s : contentAllDao) {
                Document doc = Jsoup.parse(s);
                Elements parameters = doc.select("database");
                for (Element parameter : parameters) {
                    if (!dbOrderList.contains(parameter.attr("uuid"))) {
                        dbOrderList.add(parameter.attr("uuid"));
                    }
                }
            }
            List<HfDmContentUseTablePojo> contentUseTableListDaoRet = new ArrayList<>();
            for (String s : dbOrderList) {
                for (int i = 0; i < contentUseTableListDao.size(); i++) {
                    if (contentUseTableListDao.get(i).getUuid().equals(s)) {
                        contentUseTableListDaoRet.add(contentUseTableListDao.get(i));
                        contentUseTableListDao.remove(contentUseTableListDao.get(i));
                        break;
                    }
                }
            }
            contentUseTableListDaoRet.addAll(contentUseTableListDao);
            contentUseTableListDao = contentUseTableListDaoRet;
        }

        // 数据准备
        // 查询主文件 把fileUuid换成主文件的
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        List<HfDmDb> dmDbListInFileDao = workingTableMapper.getDmDbListInFileDao(null, null, fileUuid, fileVersionId);
        List<HfDmTable> dmTableListInFileDao = workingTableMapper.getDmTableListInFileDao(null, fileUuid, fileVersionId);
        List<HfDmColumns> dmTableColumnsListInFileDao = workingTableMapper.getDmTableColumnsListInFileDao(null, fileUuid, fileVersionId);

        for (HfDmContentUseTablePojo dmContentUseTablePojo : contentUseTableListDao) {
            // dataSource
            String dataSource = (String) dmContentUseTablePojo.getDataSource();
            // list两个元素 第一个是databaseId 第二个是tableId
            String databaseId = null;
            String tableId = null;
            String viewId = null;
            String lastId = null; // dataSource的最后一个元素(即tableId或者viewId)
            if (StringUtils.isNotBlank(dataSource)) {
                List<String> list = JSON.parseObject(dataSource, List.class);
                try {
                    databaseId = list.get(0);
                    tableId = list.get(1);
                    lastId = list.get(list.size() - 1);
                    viewId = list.get(2);  // 如果有viewId 就取viewId
                } catch (Exception e) {

                }
                ArrayList<String> dataSourceName = new ArrayList<>();
                if (databaseId != null) {
                    for (HfDmDb hfDmDb : dmDbListInFileDao) {
                        if (hfDmDb.getKey().equals(databaseId)) {
                            dataSourceName.add(hfDmDb.getTitle());
                            break;
                        }
                    }
                }
                if (tableId != null) {
                    for (HfDmTable hfDmTable : dmTableListInFileDao) {
                        if (hfDmTable.getKey().equals(tableId)) {
                            dataSourceName.add(hfDmTable.getTitle());
                            break;
                        }
                    }
                }
                if (viewId != null) {
                    for (HfDmTable hfDmTable : dmTableListInFileDao) {
                        if (hfDmTable.getKey().equals(viewId)) {
                            dataSourceName.add(hfDmTable.getTitle());
                            break;
                        }
                    }
                }

                dmContentUseTablePojo.setDataSource(list);
                dmContentUseTablePojo.setDataSourceName(dataSourceName);
            } else {
                dmContentUseTablePojo.setDataSource(new ArrayList<>());
            }
            // oriDataSource
            String oriDataSource = (String) dmContentUseTablePojo.getOriDataSource();
            // list两个元素 第一个是databaseId 第二个是tableId 第三个(若有)是viewId
            if (StringUtils.isNotBlank(dataSource)) {
                List<String> list = JSON.parseObject(oriDataSource, List.class);
                dmContentUseTablePojo.setOriDataSource(list);
            } else {
                dmContentUseTablePojo.setOriDataSource(new ArrayList<>());
            }
            // fields
            if (dmContentUseTablePojo.getFields() != null) {
                String fields = (String) dmContentUseTablePojo.getFields();
                ArrayList<String> fieldsName = new ArrayList<>();
                if (databaseId != null && lastId != null && StringUtils.isNotBlank(fields)) {
                    List<String> list = JSON.parseObject(fields, List.class);
                    for (String dataIndex : list) {
                        for (HfDmColumns hfDmColumns : dmTableColumnsListInFileDao) {
                            if (lastId.equals(hfDmColumns.getTableId()) && dataIndex.equals(hfDmColumns.getDataIndex())) {
                                fieldsName.add(hfDmColumns.getTitle());
                                break;
                            }
                        }

                    }
                    dmContentUseTablePojo.setFields(list);
                    dmContentUseTablePojo.setFieldsName(fieldsName);
                } else {
                    dmContentUseTablePojo.setFields(new ArrayList<>());
                }
            }
            // rows
            if (dmContentUseTablePojo.getRows() != null) {
                String rows = (String) dmContentUseTablePojo.getRows();
                try {
                    List list = JSON.parseObject(rows, List.class);
                    dmContentUseTablePojo.setRows(list);
                } catch (Exception e) {
                    dmContentUseTablePojo.setRows(new ArrayList<>());
                }

            }
        }
        return TResponseVo.success(contentUseTableListDao);
    }

    /**
     * 编辑页数据表清单-删除
     *
     * @param
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delContentDbService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception {
        String uuid = hfDmContentUseTablePojo.getUuid();
        String fileUuid = hfDmContentUseTablePojo.getFileUuid();
        String fileVersionId = hfDmContentUseTablePojo.getFileVersionId();
        // 删除刷库
        fileOperationMapper.delContentDbDao(uuid, fileUuid, fileVersionId);
        // 删除文本内数据
        HfDmContentUseTablePojo contentUseTableInfoDao = fileOperationMapper.getContentUseTableInfoDao(uuid, fileUuid, fileVersionId);
        String outlineId = contentUseTableInfoDao.getOutlineId();
        ContentPojo contentDao = fileOperationMapper.getContentDao(fileUuid, fileVersionId, outlineId);
        String contentText = contentDao.getContentText();


        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
        Elements elements = doc.select("database[uuid=" + uuid + "]");
        // elements只会包含一个结果
        for (Element element : elements) {
            element.remove();
        }
        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
        contentText = contentText.replaceAll("</br>", "");
        contentDao.setContentText(contentText);


        // 搜索模式下
        String searchUuid = hfDmContentUseTablePojo.getSearchUuid();
        String userId = hfDmContentUseTablePojo.getUserId();
        if (searchUuid != null) {
            // 更新后入库前更新一下缓存
            if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
            }

            doc = Jsoup.parse(contentText, Parser.xmlParser());
            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            elements = doc.select("mark");
            for (Element element : elements) {
                element.before(element.html());
                element.remove();  // 去除mark标签
            }
            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
            contentText = contentText.replaceAll("</br>", "");
            contentDao.setContentText(contentText);
        }

        fileOperationMapper.updateContentDao(contentDao);

        return TResponseVo.success("去除数据表成功");
    }

    /**
     * 右侧列表中编辑数据表
     *
     * @param hfDmContentUseTablePojo
     */
    @Override
    public TResponseVo updateContentDbService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception {
        String uuid = hfDmContentUseTablePojo.getUuid(); // 使用表的id
        String fileUuid = hfDmContentUseTablePojo.getFileUuid(); //
        String fileVersionId = hfDmContentUseTablePojo.getFileVersionId(); //
        String name = hfDmContentUseTablePojo.getName();
        String desc = hfDmContentUseTablePojo.getDesc(); // 没传

        // 验证新参数名是否在本文内重名
//        if (name != null) {
//            Integer integer = fileOperationMapper.checkContentUseTableNameDao(hfDmContentUseTablePojo);
//            if (integer != 0) {
//                throw new WorkTableException("已存在同名表");
//            }
//        }

        List<String> dataSource = null;
        List<String> fields = null;
        List<String> rows = null;
        if (hfDmContentUseTablePojo.getDataSource() != null) {
            dataSource = (List) hfDmContentUseTablePojo.getDataSource();
            hfDmContentUseTablePojo.setDataSource(JSON.toJSONString(dataSource));
            // 区分模板阶段还是文件阶段调用update 模板阶段name不为空
            // 模板阶段需要同时更新dataSource和oriDataSource
            if (name != null) {
                hfDmContentUseTablePojo.setOriDataSource(JSON.toJSONString(dataSource));
            }
        }
        if (hfDmContentUseTablePojo.getFields() != null) {
            fields = (List) hfDmContentUseTablePojo.getFields();
            hfDmContentUseTablePojo.setFields(JSON.toJSONString(fields));
        }
        if (hfDmContentUseTablePojo.getRows() != null) {
            rows = (List) hfDmContentUseTablePojo.getRows();
            hfDmContentUseTablePojo.setRows(JSON.toJSONString(rows));
        }

        // 更新表数据
        fileOperationMapper.updateContentUseTableDao(hfDmContentUseTablePojo);

        // 更新后直接取表
        HfDmContentUseTablePojo contentUseTableInfoDao = fileOperationMapper.getContentUseTableInfoDao(uuid, fileUuid, fileVersionId);

        String tableId = null;
        if (dataSource != null && dataSource.size() != 0) {
            try {
//                tableId = dataSource.get(1);
                tableId = dataSource.get(dataSource.size() - 1);
            } catch (IndexOutOfBoundsException e) {
                throw new WorkTableException("未指定到数据库下的具体数据表或视图");
            }
        } else {
            List<String> datasourceList = JSON.parseObject((String) contentUseTableInfoDao.getDataSource(), List.class);
            try {
//                tableId = datasourceList.get(1);
                tableId = datasourceList.get(datasourceList.size() - 1);
            } catch (IndexOutOfBoundsException e) {
                throw new WorkTableException("未指定到数据库下的具体数据表或视图");
            }
        }

        if (contentUseTableInfoDao.getFields() != null) {
            fields = JSON.parseObject((String) contentUseTableInfoDao.getFields(), List.class);
        }
        if (contentUseTableInfoDao.getRows() != null) {
            rows = JSON.parseObject((String) contentUseTableInfoDao.getRows(), List.class);
        }

        // 查fields
        List<HfDmContentColumns> dmColumnsInFileDaoTmp = new ArrayList<>();
        ArrayList<String> fieldOnlyList = new ArrayList<>();
        if (fields != null && fields.size() != 0) {
            List<HfDmContentColumns> dmColumnsInFileDao = fileOperationMapper.getDmColumnsInFileDao(fields, tableId, fileUuid, fileVersionId);
            for (HfDmContentColumns dmColumns : dmColumnsInFileDao) {
                // 转义option
                Object options = dmColumns.getOptions();
                if (options != null && StringUtils.isNotBlank(String.valueOf(options))) {
                    List list = JSON.parseObject(String.valueOf(options), List.class);
                    dmColumns.setOptions(list);
                } else {
                    dmColumns.setOptions(new ArrayList<>());
                }
                // 转义unit
                Object unit = dmColumns.getUnit();
                if (unit != null && StringUtils.isNotBlank(String.valueOf(unit))) {
                    List list = JSON.parseObject(String.valueOf(unit), List.class);
                    dmColumns.setUnit(list);
                } else {
                    dmColumns.setUnit(new ArrayList<>());
                }
            }
            // dmColumnsInFileDao 按照 fields 勾选的顺序排序
            for (String field : fields) {
                for (HfDmContentColumns hfDmContentColumns : dmColumnsInFileDao) {
                    if (field.equals(hfDmContentColumns.getDataIndex())) {
                        dmColumnsInFileDaoTmp.add(hfDmContentColumns);
                        fieldOnlyList.add(hfDmContentColumns.getDataIndex());
                        break;
                    }
                }
            }
        }

        // 查rows
        ArrayList<Object> objects = new ArrayList<>();
        if (rows != null && rows.size() != 0) {
            String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
            System.out.println("表编号为: " + hashMod);
            List<HfDmContentData> dmDataInFileDao = splitTableMapper.getDmDataInFileDao(rows, tableId, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            for (String row : rows) {
                for (HfDmContentData hfDmContentData : dmDataInFileDao) {
                    if (row.equals(hfDmContentData.getDataId())) {
                        if (hfDmContentData.getDataContent() != null) {
                            String dataContent = (String) hfDmContentData.getDataContent();
                            Map<String, Object> map = JSON.parseObject(dataContent, Map.class);
                            map.keySet().removeIf(k -> !fieldOnlyList.contains(k));
                            map.put("key", UUID.randomUUID().toString().replaceAll("-", ""));
                            objects.add(map);
                        }
                        break;
                    }
                }
            }
        }

        // 返回
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("fields", dmColumnsInFileDaoTmp);
        retMap.put("rows", objects);
        return TResponseVo.success(retMap);

//        // 序列化
//        String dataSourceStr = new JsonKeyUtils().listToString(dataSource);
//        String fieldsStr = JSON.toJSONString(fields);
//
//        // 更新hf_model_content的contentText
//        HfDmContentUseTablePojo contentUseTableInfoDao = fileOperationMapper.getContentUseTableInfoDao(uuid, fileUuid, fileVersionId);
//        String outlineId = contentUseTableInfoDao.getOutlineId();
//        ContentPojo contentDao = fileOperationMapper.getContentDao(fileUuid, fileVersionId, outlineId);
//        String contentText = contentDao.getContentText();
//
//        Document doc = Jsoup.parse(contentText, Parser.xmlParser());
//        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
//        Element element = doc.selectFirst("database[uuid=" + uuid + "]");
//
//        if (name != null) element.attr("name", name);
//        if (desc != null) element.attr("desc", desc);
//        if (dataSource != null) element.attr("data-source", dataSourceStr);
//        // 查询主文件信息
//        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
//        String oriFileUuid = fileUuid;
//        String oriFileVersionId = fileVersionId;
//        if (docAllInfoDao.getMainFileUuid() != null) {
//            fileUuid = docAllInfoDao.getMainFileUuid();
//            fileVersionId = docAllInfoDao.getMainFileVersionId();
//        }
//        if (fields != null) {
//            element.attr("fields", fieldsStr);
//            if (fields.size() == 0) {
//                element.attr("fields","");
//            } else {
//                List<String> datasourceList = JSON.parseObject((String) contentUseTableInfoDao.getDataSource(), List.class);
//                String tableId = datasourceList.get(1);
//                element.empty();
//                // 更新<database>标签内容
//                List<HfDmContentColumns> dmColumnsInFileDao = fileOperationMapper.getDmColumnsInFileDao(fields, tableId, fileUuid, fileVersionId);
//                for (HfDmContentColumns dmColumns : dmColumnsInFileDao) {
//                    // 转义option
//                    Object options = dmColumns.getOptions();
//                    if (options != null && StringUtils.isNotBlank(String.valueOf(options))) {
//                        List list = JSON.parseObject(String.valueOf(options), List.class);
//                        dmColumns.setOptions(list);
//                    } else {
//                        dmColumns.setOptions(new ArrayList<>());
//                    }
//                    // 转义unit
//                    Object unit = dmColumns.getUnit();
//                    if (unit != null && StringUtils.isNotBlank(String.valueOf(unit))) {
//                        List list = JSON.parseObject(String.valueOf(unit), List.class);
//                        dmColumns.setUnit(list);
//                    } else {
//                        dmColumns.setUnit(new ArrayList<>());
//                    }
//                }
//                // dmColumnsInFileDao 按照 fields 顺序排序
//                List<HfDmContentColumns> dmColumnsInFileDaoTmp = new ArrayList<>();
//                for (String field : fields) {
//                    for (HfDmContentColumns hfDmContentColumns : dmColumnsInFileDao) {
//                        if (field.equals(hfDmContentColumns.getDataIndex())) {
//                            dmColumnsInFileDaoTmp.add(hfDmContentColumns);
//                            break;
//                        }
//                    }
//                }
//                element.attr("fields", JSON.toJSONString(dmColumnsInFileDaoTmp));
//            }
//        }
//        if (rows != null) {
//            if (rows.size() == 0) {
//                element.empty();
//                element.removeAttr("rows");
//            } else {
//                List<String> datasourceList = JSON.parseObject((String) contentUseTableInfoDao.getDataSource(), List.class);
//                String tableId = datasourceList.get(1);
//                element.empty();
//                // 更新<database>标签内容
//                List<HfDmContentData> dmDataInFileDao = fileOperationMapper.getDmDataInFileDao(rows, tableId, fileUuid, fileVersionId);
//                ArrayList<Object> objects = new ArrayList<>();
//                for (HfDmContentData hfDmContentData : dmDataInFileDao) {
//                    if (hfDmContentData.getDataContent() != null) {
//                        String dataContent = (String) hfDmContentData.getDataContent();
//                        Map map = JSON.parseObject(dataContent, Map.class);
//                        map.put("key",UUID.randomUUID().toString().replaceAll("-", ""));
//                        objects.add(map);
//                    }
//                }
//                element.attr("rows", JSON.toJSONString(objects));
//            }
//        }
//        String remark = hfDmContentUseTablePojo.getRemark();
//        if (remark != null) {
//            element.attr("remark",remark);
//        } else {
//            element.removeAttr("remark");
//        }
//
//        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//        contentText = contentText.replaceAll("</br>", "");
//        contentDao.setContentText(contentText);
//
//
//        // 搜索模式下
//        String searchUuid = hfDmContentUseTablePojo.getSearchUuid();
//        String userId = hfDmContentUseTablePojo.getUserId();
//        if (searchUuid != null) {
//            // 更新后入库前更新一下缓存
//            if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + oriFileUuid + oriFileVersionId + searchUuid + outlineId)) {
//                redisUtils.set("search" + userId + oriFileUuid + oriFileVersionId + searchUuid + outlineId, contentText, 1800);
//            }
//
//            doc = Jsoup.parse(contentText, Parser.xmlParser());
//            doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
//            Elements elements = doc.select("mark");
//            for (Element ele : elements) {
//                ele.before(ele.html());
//                ele.remove();  // 去除mark标签
//            }
//            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//            contentText = contentText.replaceAll("</br>", "");
//            contentDao.setContentText(contentText);
//        }
//        fileOperationMapper.updateContentDao(contentDao);
//        return TResponseVo.success("更新完成");
    }


    /**
     * 获取卷出来的表
     *
     * @param hfDmContentUseTablePojo
     */
    @Override
    public TResponseVo getRollTableListService(HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception {
        String fileUuid = hfDmContentUseTablePojo.getFileUuid(); //
        String fileVersionId = hfDmContentUseTablePojo.getFileVersionId(); //
        Object oriDataSource = hfDmContentUseTablePojo.getOriDataSource();
        if (oriDataSource == null || StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            throw new WorkTableException("必填参数为空");
        }
        List<String> oriDataSourceList = (List) oriDataSource;
        String tableId = oriDataSourceList.get(1);
        List<LabelValuePojo> rollTableListDao = fileOperationMapper.getRollTableListDao(tableId, fileUuid, fileVersionId);
        HfDmTable dmTableInfoInFileDao = workingTableMapper.getDmTableInfoInFileDao(tableId, fileUuid, fileVersionId);
        try {
            rollTableListDao.add(0, new LabelValuePojo(dmTableInfoInFileDao.getTableName(), tableId));
        } catch (Exception e) {
            // do nothing
        }
        return TResponseVo.success(rollTableListDao);
    }
}
