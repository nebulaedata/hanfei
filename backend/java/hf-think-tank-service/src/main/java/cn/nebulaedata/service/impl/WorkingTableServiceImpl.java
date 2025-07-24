package cn.nebulaedata.service.impl;


import cn.nebulaedata.async.DmTableTask;
import cn.nebulaedata.dao.EditToolMapper;
import cn.nebulaedata.dao.FileOperationMapper;
import cn.nebulaedata.dao.SplitTableMapper;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.enums.ResponseEnum;
import cn.nebulaedata.exception.FileIndexException;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.form.EdgeForm;
import cn.nebulaedata.form.NodeForm;
import cn.nebulaedata.pojo.*;
import cn.nebulaedata.service.FileOperationService;
//import cn.nebulaedata.service.Neo4jService;
import cn.nebulaedata.service.WorkingTableService;
import cn.nebulaedata.socket.WebSocketServer;
import cn.nebulaedata.utils.*;
import cn.nebulaedata.vo.TResponseVo;
import com.alibaba.fastjson.JSON;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.nebulaedata.utils.JsonKeyUtils.*;

/**
 * @author 徐衍旭
 * @date 2021/8/12 15:12
 * @note
 */
@Service
public class WorkingTableServiceImpl implements WorkingTableService {

    private static final Logger LOG = LoggerFactory.getLogger(cn.nebulaedata.service.impl.WorkingTableServiceImpl.class);
    @Autowired
    private WorkingTableMapper workingTableMapper;
    @Autowired
    private FileOperationMapper fileOperationMapper;
    @Autowired
    private SplitTableMapper splitTableMapper;
    @Autowired
    private EditToolMapper editToolMapper;
    @Autowired
    private DmTableTask dmTableTask;
    @Value("${doc-frame-service.project-file-path}")
    private String projectFilePath;
    @Value("${doc-frame-service.batch-file-path}")
    private String batchFilePath;
    @Value("${doc-frame-service.compare-path}")
    private String comparePath;
    @Value("${doc-frame-service.download-path}")
    private String downloadPath;
    @Value("${doc-frame-service.env-name}")
    private String envName;
    @Value("${doc-frame-service.host-name}")
    private String hostName;
    @Value("${protocol.http}")
    private String httpProtocol;
    @Value("${doc-frame-service.upload-dm-data-path}")
    private String uploadDmDataPath;
    @Value("${doc-frame-service.annex-path}")
    private String annexPath;
    @Value("${split-database.dm-content-data}")
    private String dmContentDataDatabase;
    @Value("${doc-frame-service.upload-model-path}")
    private String uploadModelPath;

    @Autowired
    private FileOperationService fileOperationService;
//    @Autowired
//    Neo4jService neo4jService;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    FileOperationServiceImpl fileOperationServiceImpl;
//    @Autowired
//    private CommonExcelSaveFun commonExcelSaveFun;

    // 记录辅助工具最近使用记录
    Map<String, List<String>> supTableRecent = new HashMap();

    private final AtomicBoolean uploadLock = new AtomicBoolean(true);  // 数据管理上传锁

    /**
     * 工作台-获取大纲树
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getOutlineListService(String fileUuid, String fileVersionId) throws Exception {
        if (fileVersionId == null) {
            fileVersionId = fileOperationMapper.getLastVersionIdDao(fileUuid);
        }
        // 先查缓存是否有大纲信息
        if (redisUtils.hasKey("getOutlineList" + fileUuid + fileVersionId) && String.valueOf(redisUtils.get("getOutlineList" + fileUuid + fileVersionId)).length() > 5) {
            String s = String.valueOf(redisUtils.get("getOutlineList" + fileUuid + fileVersionId));
            // 使用过就重置失效时间
            redisUtils.expire("getOutlineList" + fileUuid + fileVersionId, 7200);
            List<OutLinePojo> outlineListDao = JSON.parseObject(s, List.class);
            return TResponseVo.success(outlineListDao);
        } else {
//            List<OutLinePojo> outlineListDao = workingTableMapper.getOutlineListDao(fileUuid, fileVersionId);
            List<OutLinePojo> outlineListDao1 = workingTableMapper.getOutlineListDao1(fileUuid, fileVersionId);  // 记录全部
            List<OutLinePojo> outLineListDao2 = new ArrayList<>();  // 记录剩余的 当剩余为0时结束
            List<OutLinePojo> outLineListDaoTmp = new ArrayList<>();  // 记录每层
            List<OutLinePojo> outLineListResult = new ArrayList<>(); // 记录结果
            outLineListDao2.addAll(outlineListDao1);
            while (outLineListDao2.size() != 0) {
                if (outLineListResult.size() == 0) {
                    // 第一次
                    for (OutLinePojo outLinePojo : outlineListDao1) {
                        if (outLinePojo.getChildren() == null) {
                            outLinePojo.setChildren(new ArrayList<>());
                        }
                        if (outLinePojo.getOutlineFatherId() == null || outLinePojo.getOutlineFatherId().equals("") || outLinePojo.getOutlineFatherId().equals("null")) {
                            outLineListResult.add(outLinePojo);
                            outLineListDaoTmp.add(outLinePojo);
                            outLineListDao2.remove(outLinePojo);
                        }
                    }
                    outlineListDao1 = new ArrayList<>();
                    outlineListDao1.addAll(outLineListDao2);
                } else {
                    List<OutLinePojo> outLineListDaoTmp1 = new ArrayList<>();
                    for (OutLinePojo outLinePojo : outlineListDao1) {
                        for (OutLinePojo linePojo : outLineListDaoTmp) {
                            if (linePojo.getOutlineId().equals(outLinePojo.getOutlineFatherId())) {
                                linePojo.getChildren().add(outLinePojo);
                                outLineListDaoTmp1.add(outLinePojo);
                                outLineListDao2.remove(outLinePojo);
                            }
                        }
                    }
                    outLineListDaoTmp = new ArrayList<>();  // 记录每层
                    outLineListDaoTmp.addAll(outLineListDaoTmp1);
                    outlineListDao1 = new ArrayList<>();
                    outlineListDao1.addAll(outLineListDao2);
                }
            }
//            System.out.println(JSON.toJSONString(outLineListResult));
//            System.out.println(JSON.toJSONString(outlineListDao));
            // 查的结果放入缓存 2小时过期时间
            redisUtils.set("getOutlineList" + fileUuid + fileVersionId, JSON.toJSONString(outLineListResult), 7200);
            return TResponseVo.success(outLineListResult);
        }
    }


    /**
     * 工作台-获取模板内容
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getContentService(String fileUuid, String fileVersionId, String outlineId, Boolean origin, String userId, String searchUuid) throws Exception {
        if (StringUtils.isBlank(fileVersionId)) {
//            fileVersionId = fileOperationMapper.getLastVersionIdDao(fileUuid);
            throw new WorkTableException("必填参数为空");
        }
        if (StringUtils.isBlank(outlineId)) {
            String firstOutlineIdDao = workingTableMapper.getFirstOutlineIdDao(fileUuid, fileVersionId);
            outlineId = firstOutlineIdDao;
        }
        List<OutLinePojo> outlineListDao = workingTableMapper.getContentDao(fileUuid, fileVersionId, outlineId);
        if (outlineListDao.size() == 1) {
            OutLinePojo outLinePojo1 = outlineListDao.get(0);
            String outlineOrder = outLinePojo1.getOutlineOrder();
            outlineOrder = String.valueOf(Integer.valueOf(outlineOrder) - 1);
            String Previous = fileOperationMapper.getOutlineIdbyOrderDao(outlineOrder, fileUuid, fileVersionId);
            outlineOrder = String.valueOf(Integer.valueOf(outlineOrder) + 1 + 1);
            String next = fileOperationMapper.getOutlineIdbyOrderDao(outlineOrder, fileUuid, fileVersionId);
//            System.out.println(Previous);
//            System.out.println(next);
            outLinePojo1.setPrevious(Previous);
            outLinePojo1.setNext(next);

            if (origin == null || !origin) {
                if (redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                    String s = String.valueOf(redisUtils.get("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId));
                    outLinePojo1.setContentText(s);
                }
            }
        }

        return TResponseVo.success(outlineListDao);
    }


    /**
     * 新建项目
     *
     * @param projectPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo addProjectService(ProjectPojo projectPojo, String userId) throws Exception {
        String projectName = projectPojo.getProjectName();
        String projectInfo = projectPojo.getProjectInfo();
        String projectStartDate = projectPojo.getProjectStartDate();
        String projectEndDate = projectPojo.getProjectEndDate();
        ArrayList includeUserId = (ArrayList) projectPojo.getIncludeUserId();
//        String projectNo = projectPojo.getProjectNo();
        if (StringUtils.isBlank(projectName) || StringUtils.isBlank(projectStartDate) || StringUtils.isBlank(projectEndDate) || StringUtils.isBlank(userId)) {
            return TResponseVo.error("必填参数为空");
        }
        try {
            int j = workingTableMapper.checkProjectNameDao(projectName, null);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addProjectService", projectName, "当前类错误码-101-项目名已存在");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(101);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addProjectService", projectName, "当前类错误码-102-检查项目名SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(102);
            throw questionnaireException;
        }
        String projectId = UUID.randomUUID().toString().replaceAll("-", "");
        projectPojo.setProjectId(projectId);
        projectPojo.setCreateUserId(userId);
        projectPojo.setMainPerson(userId);
        projectPojo.setIncludeUserId(includeUserId == null || includeUserId.size() == 0 ? null : JSON.toJSONString(includeUserId));
        projectPojo.setCreateTime(new Date());
        projectPojo.setFilePath(projectFilePath + "/" + projectId);
        workingTableMapper.addProjectDao(projectPojo);
        // 在创建项目的时候 默认创建一个分包 这个分包的信息和项目一致
        PackagePojo packagePojo = new PackagePojo();
        String packageId = UUID.randomUUID().toString().replaceAll("-", "");
        packagePojo.setPackageId(packageId);
        packagePojo.setPackageName(projectName);
        packagePojo.setCreateUserId(userId);
        packagePojo.setMainPerson(userId);
        packagePojo.setPackageInfo(projectInfo);
        packagePojo.setIncludeUserId(includeUserId == null || includeUserId.size() == 0 ? null : JSON.toJSONString(includeUserId));
        packagePojo.setProjectId(projectId);
        packagePojo.setCreateTime(new Date());
        workingTableMapper.addPackageDao(packagePojo);
        // 创建返回值
        HashMap<Object, Object> ret = new HashMap<>();
        ret.put("info", "新建项目成功");
        ret.put("projectId", projectId);
        ret.put("packageId", packageId);
        return TResponseVo.success(ret);
    }

    /**
     * 删除项目
     *
     * @param projectId
     */
    @Override
    public TResponseVo delProjectService(String projectId) throws Exception {
        ProjectPojo projectInfoDao = workingTableMapper.getProjectInfoDao(projectId);
        if (projectInfoDao == null) {
            return TResponseVo.success("删除成功");
        }
        String filePath = projectInfoDao.getFilePath();
        // 删除项目信息
        workingTableMapper.delProjectDao(projectId);
        // 同时删除项目文件夹
        new DeleteDirectoryUtils().deleteDir(filePath);
        return TResponseVo.success("删除成功");
    }

    /**
     * 修改项目
     *
     * @param projectPojo
     */
    @Override
    public TResponseVo updateProjectService(ProjectPojo projectPojo) throws Exception {
        String projectName = projectPojo.getProjectName();
        String projectId = projectPojo.getProjectId();
        List<String> includeUserId = (ArrayList) projectPojo.getIncludeUserId();
        try {
            int j = workingTableMapper.checkProjectNameDao(projectName, projectId);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateProjectService", projectName, "当前类错误码-101-项目名已存在");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(101);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateProjectService", projectName, "当前类错误码-102-检查项目名SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(102);
            throw questionnaireException;
        }
        if (projectPojo.getIncludeUserId() == null) {
            projectPojo.setIncludeUserId(null);
        } else {
            projectPojo.setIncludeUserId(JSON.toJSONString(includeUserId));
        }
        workingTableMapper.updateProjectDao(projectPojo);
        return TResponseVo.success("更新成功");
    }

    /**
     * 获取项目信息
     *
     * @param projectId
     */
    @Override
    public TResponseVo getProjectInfoService(String projectId) throws Exception {
        ProjectPojo projectPojo = workingTableMapper.getProjectInfoDao(projectId);
        if (projectPojo == null) {
            return TResponseVo.error("项目不存在或已被删除");
        }
        // 计算项目空间使用率
        String spaceSize = projectPojo.getSpaceSize();
        String filePath = projectPojo.getFilePath();
        long totalSizeOfFile = new FileSizeUtils().getTotalSizeOfFile(filePath);
        String s2 = String.valueOf(JsonKeyUtils.decimalKeepN(Double.valueOf(totalSizeOfFile) / 1024 / 1024, 2));
        projectPojo.setUsedSize(s2 + " MB");
        projectPojo.setTotalSize(spaceSize + " MB");
        double rate = Double.valueOf(s2) / Double.valueOf(spaceSize) * 100;
        projectPojo.setUsedRate(String.valueOf(JsonKeyUtils.decimalKeepN(rate, 2)));
        // 易读时间格式
        String ago = new TimeFormatUtils().getAgo(projectPojo.getCreateTime());
        projectPojo.setTimeText(ago);
        // 项目状态

        // 分包个数
        Integer packageCount = workingTableMapper.getPackageCountDao(projectId);
        projectPojo.setPackageCount(String.valueOf(packageCount));
        // 文件总个数
        Integer fileCount = workingTableMapper.getFileCountDao(projectId);
//        String fileCount = new FileSizeUtils().numberOfFiles(filePath);
        projectPojo.setFileCount(String.valueOf(fileCount));
        // 项目参与人
        String includeUserId = String.valueOf(projectPojo.getIncludeUserId());
        ArrayList<String> includeUserIdList = JSON.parseObject(includeUserId, ArrayList.class);
        ArrayList<String> includeUserNameList = new ArrayList<>();
        List<LabelValuePojo> userList = workingTableMapper.getUserListDao();
        if (includeUserIdList != null) {
            for (String userId : includeUserIdList) {
                for (LabelValuePojo labelValuePojo : userList) {
                    if (userId.equals(labelValuePojo.getValue())) {
                        includeUserNameList.add(labelValuePojo.getLabel());
                        break;
                    }
                }
            }
        }
        projectPojo.setIncludeUserId(includeUserIdList);
        projectPojo.setIncludeUserName(includeUserNameList);
        projectPojo.setFilePath(null);
        return TResponseVo.success(projectPojo);
    }


    /**
     * 获取项目列表
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getProjectListService(PagePojo pagePojo, String userId) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<ProjectPojo> superQuestionnaireClassPojo = workingTableMapper.getProjectListDao(paramNameLike, null);

        List<ProjectPojo> superQuestionnaireClassPojoTmp = new ArrayList<>();
        superQuestionnaireClassPojoTmp.addAll(superQuestionnaireClassPojo);
        for (ProjectPojo projectPojo : superQuestionnaireClassPojoTmp) {
            if (!projectPojo.getCreateUserId().equals(userId)) {
                superQuestionnaireClassPojo.remove(projectPojo);
            }
        }

        // 计算空间使用率
        for (ProjectPojo projectPojo : superQuestionnaireClassPojo) {
            String spaceSize = projectPojo.getSpaceSize();
            String filePath = projectPojo.getFilePath();
            long totalSizeOfFile = new FileSizeUtils().getTotalSizeOfFile(filePath);
            String s2 = String.valueOf(JsonKeyUtils.decimalKeepN(Double.valueOf(totalSizeOfFile) / 1024 / 1024, 2));
            projectPojo.setUsedSize(s2 + " MB");
            projectPojo.setTotalSize(spaceSize + " MB");
            double rate = Double.valueOf(s2) / Double.valueOf(spaceSize) * 100;
            projectPojo.setUsedRate(String.valueOf(JsonKeyUtils.decimalKeepN(rate, 2)));
            // 易读时间格式
            String ago = new TimeFormatUtils().getAgo(projectPojo.getCreateTime());
            projectPojo.setTimeText(ago);
            projectPojo.setFilePath(null);
        }


        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(superQuestionnaireClassPojo);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(superQuestionnaireClassPojo, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", superQuestionnaireClassPojo.size());
        return TResponseVo.success(resultMap);

    }

    /**
     * 获取项目策划预选方案
     *
     * @param
     */
    @Override
    public TResponseVo getPackagePlanService() throws Exception {
//        ProjectPojo projectInfoDao = workingTableMapper.getProjectInfoDao(projectId);
        String planTree1 = "[{\"title\":\"总包\",\"key\":\"0-0\",\"children\":[{\"title\":\"可研\",\"key\":\"0-0-0\"},{\"title\":\"设计\",\"key\":\"0-0-1\",\"children\":[{\"title\":\"总体设计\",\"key\":\"0-0-1-0\"},{\"title\":\"专业设计\",\"key\":\"0-0-1-1\"}]},{\"title\":\"施工\",\"key\":\"0-0-2\"},{\"title\":\"货物\",\"key\":\"0-0-3\"},{\"title\":\"工程\",\"key\":\"0-0-4\"}]}]";
        String planTree2 = "[{\"title\":\"江苏\",\"key\":\"0-0\",\"children\":[{\"title\":\"镇江\",\"key\":\"0-0-0\"},{\"title\":\"南京\",\"key\":\"0-0-1\",\"children\":[{\"title\":\"江宁\",\"key\":\"0-0-1-0\"},{\"title\":\"建邺\",\"key\":\"0-0-1-1\"}]},{\"title\":\"南通\",\"key\":\"0-0-2\"},{\"title\":\"徐州\",\"key\":\"0-0-3\"},{\"title\":\"连云港\",\"key\":\"0-0-4\"}]}]";
        List list1 = JSON.parseObject(planTree1, List.class);
        List list2 = JSON.parseObject(planTree2, List.class);
        List<List> planList = new ArrayList<>();
        planList.add(list1);
        planList.add(list2);
        Map<String, List> retMap = new HashMap<>();
        retMap.put("planList", planList);
        return TResponseVo.success(retMap);
    }

    /**
     * 根据策划新建若干分包
     *
     * @param packagePojo
     */
    @Override
    public TResponseVo addPackageByPlanService(PackagePojo packagePojo) throws Exception {
        List<Map<String, String>> packagePojoList = new ArrayList<>();
        List<String> packagePojoNameList = new ArrayList<>();
        Map<String, Map<String, Object>> packagePlan = (Map) packagePojo.getPackagePlan();
        String createUserId = packagePojo.getCreateUserId();
        String projectId = packagePojo.getProjectId();
        // 需要增加参数的话 继续在keyList中add即可
        List<String> keyList = new ArrayList<>();
        keyList.add("packageName");
        keyList.add("packageInfo");
        keyList.add("mainPerson");
        keyList.add("includeUserId");
        keyList.add("packageId");

        for (String packageId : packagePlan.keySet()) {
            Map<String, Object> map = packagePlan.get(packageId);
            Map<String, String> tmpMap = new HashMap<>();
            for (String key : keyList) {
                if (key.equals("includeUserId")) {
                    String value = JSON.toJSONString(map.get(key));
                    value = value.replaceAll("\"", "\\\\\"");
                    tmpMap.put(key, value);
                } else if (key.equals("packageId")) {
                    String value = packageId;
                    tmpMap.put(key, value);
                } else {
                    String value = map.get(key) == null ? null : String.valueOf(map.get(key));
                    tmpMap.put(key, value);
                }
            }
            packagePojoList.add(tmpMap);
            packagePojoNameList.add(tmpMap.get("packageName"));  // 把分包名字收集起来
        }
        // 备份packagePojoList
        List<Map<String, String>> packagePojoListTmp = new ArrayList<>();
        packagePojoListTmp.addAll(packagePojoList);

        // 根据项目id获取项目中的所有包名 然后对比要新增的包名 判断是否有重复的分包
        List<String> packageNameList = workingTableMapper.getPackageNameListDao(projectId);
        for (String packageName : packagePojoNameList) {
            if (packageNameList.contains(packageName)) {
                // 如果已经存在同名分包 则在本次新增动作中剔除
                for (Map<String, String> map : packagePojoListTmp) {
                    if (map.get("packageName").equals(packageName)) {
                        packagePojoList.remove(map);
                        break;
                    }
                }
            }
        }

        // 所有新增分包信息已经存入packagePojoList中
        String sql = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(new Date());
        for (Map<String, String> m : packagePojoList) {
            sql = sql + "(\"" + projectId + "\",\"" + m.get("packageId") + "\",\"" + m.get("packageName") + "\",\"" + m.get("packageInfo") + "\",\"" + m.get("includeUserId") + "\",\"" + createUserId + "\",\"" + m.get("mainPerson") + "\",\"" + dateString + "\"),";
        }
        if (sql.length() != 0) {  // 防止空文档无段落
            sql = sql.substring(0, sql.length() - 1);
            workingTableMapper.addPackageListDao(sql);
        }
        return TResponseVo.success("根据策划新建分包成功");
    }

    /**
     * 新建分包
     *
     * @param
     */
    @Override
    public TResponseVo addPackageService(PackagePojo packagePojo) throws Exception {
        String packageName = packagePojo.getPackageName();
        String projectId = packagePojo.getProjectId();
        String mainPerson = packagePojo.getMainPerson();
        if (StringUtils.isBlank(packageName) || StringUtils.isBlank(projectId) || StringUtils.isBlank(mainPerson)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_NOENOUGH_ERROR);
        }
        try {
            Integer i = workingTableMapper.checkPackageNameDao(packageName, null);
            if (i >= 1) {
                FileIndexException fileIndexException = new FileIndexException();
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addPackageService", packageName, "当前类错误码-151-分包名已存在");
                fileIndexException.setCode(151);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            FileIndexException fileIndexException = new FileIndexException();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addPackageService", packageName, "当前类错误码-152-检查sql报错");
            fileIndexException.setCode(152);
            throw fileIndexException;
        }
        // 项目参与人
        packagePojo.setIncludeUserId(JSON.toJSONString((List) packagePojo.getIncludeUserId()));
        packagePojo.setCreateTime(new Date());
        String packageId = UUID.randomUUID().toString().replaceAll("-", "");
        packagePojo.setPackageId(packageId);
        workingTableMapper.addPackageDao(packagePojo);
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "新建分包成功");
        ret.put("packageId", packageId);
        return TResponseVo.success(ret);
    }

    /**
     * 删除分包
     *
     * @param packageId
     */
    @Override
    public TResponseVo delPackageService(String packageId) throws Exception {
        PackagePojo packageInfoDao = workingTableMapper.getPackageInfoDao(packageId);
        String projectId = packageInfoDao.getProjectId();
        Integer packageListSize = workingTableMapper.getPackageListSizeDao(projectId);
        if (packageListSize <= 1) {
            return TResponseVo.error("项目中至少保留一个分包");
        }
        workingTableMapper.delPackageDao(packageId);
        return TResponseVo.success("删除分包成功");
    }

    /**
     * 修改分包
     *
     * @param packagePojo
     */
    @Override
    public TResponseVo updatePackageService(PackagePojo packagePojo) throws Exception {
        String packageId = packagePojo.getPackageId();
        PackagePojo packageInfoDao = workingTableMapper.getPackageInfoDao(packageId);
//        String batchId = packageInfoDao.getBatchId();
        String projectId = packageInfoDao.getProjectId();
        // 获取更新的内容
        String packageName = packagePojo.getPackageName();
        try {
            Integer i = workingTableMapper.checkPackageNameDao(packageName, packageId);
            if (i >= 1) {
                FileIndexException fileIndexException = new FileIndexException();
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addPackageService", packageName, "当前类错误码-151-分包名已存在");
                fileIndexException.setCode(151);
                throw fileIndexException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            FileIndexException fileIndexException = new FileIndexException();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addPackageService", packageName, "当前类错误码-152-检查sql报错");
            fileIndexException.setCode(152);
            throw fileIndexException;
        }
        // 更新到对象中
        packagePojo.setProjectId(projectId);
        // 项目参与人
        packagePojo.setIncludeUserId(JSON.toJSONString((List) packagePojo.getIncludeUserId()));
        workingTableMapper.updatePackageDao(packagePojo);
        return TResponseVo.success("更新成功");
    }

    /**
     * 获取分包信息
     *
     * @param packageId
     */
    @Override
    public TResponseVo getPackageInfoService(String packageId) throws Exception {
        if (packageId == null || StringUtils.isBlank(packageId)) {
            Map retMap = new HashMap<>();
            return TResponseVo.success(retMap);
        }

        PackagePojo packageInfoDao = workingTableMapper.getPackageInfoDao(packageId);
        // 项目参与人
        String includeUserId = String.valueOf(packageInfoDao.getIncludeUserId());
        ArrayList<String> includeUserIdList = JSON.parseObject(includeUserId, ArrayList.class);
        ArrayList<String> includeUserNameList = new ArrayList<>();
        List<LabelValuePojo> userList = workingTableMapper.getUserListDao();
        if (includeUserIdList != null) {
            for (String userId : includeUserIdList) {
                for (LabelValuePojo labelValuePojo : userList) {
                    if (userId.equals(labelValuePojo.getValue())) {
                        includeUserNameList.add(labelValuePojo.getLabel());
                        break;
                    }
                }
            }
        }
        packageInfoDao.setIncludeUserId(includeUserIdList);
        packageInfoDao.setIncludeUserName(includeUserNameList);
        return TResponseVo.success(packageInfoDao);
    }

    /**
     * 获取分包列表
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getPackageListService(PagePojo pagePojo, String projectId) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        List<PackagePojo> packageListDao = workingTableMapper.getPackageListDao(paramNameLike, null, projectId);
        for (PackagePojo packageInfoDao : packageListDao) {
            // 项目参与人
            String includeUserId = String.valueOf(packageInfoDao.getIncludeUserId());
            ArrayList<String> includeUserIdList = JSON.parseObject(includeUserId, ArrayList.class);
            ArrayList<String> includeUserNameList = new ArrayList<>();
            List<LabelValuePojo> userList = workingTableMapper.getUserListDao();
            if (includeUserIdList != null && includeUserIdList.size() != 0) {
                for (String userId : includeUserIdList) {
                    for (LabelValuePojo labelValuePojo : userList) {
                        if (userId.equals(labelValuePojo.getValue())) {
                            includeUserNameList.add(labelValuePojo.getLabel());
                            break;
                        }
                    }
                }
                packageInfoDao.setIncludeUserId(includeUserIdList);
                packageInfoDao.setIncludeUserName(includeUserNameList);
            } else {
                packageInfoDao.setIncludeUserId(new ArrayList<>());
                packageInfoDao.setIncludeUserName(new ArrayList<>());
            }
        }


        String s = JSON.toJSONString(packageListDao);
        List list = JSON.parseObject(s, List.class);

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(list);
        }


        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(list, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", list.size());
        return TResponseVo.success(resultMap);

    }


    /**
     * 新建标段
     *
     * @param stagePojo
     * @param userId
     */
    @Override
    public TResponseVo addStageService(StagePojo stagePojo, String userId) throws Exception {
        String stageName = stagePojo.getStageName();
        try {
            int j = workingTableMapper.checkStageNameDao(stageName);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addStageService", stageName, "当前类错误码-131-标段名已存在");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(131);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addStageService", stageName, "当前类错误码-132-检查重复SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(132);
            throw questionnaireException;
        }
        String stageId = UUID.randomUUID().toString().replaceAll("-", "");
        stagePojo.setStageId(stageId);
        stagePojo.setCreateUserId(userId);
        stagePojo.setCreateTime(new Date());
        workingTableMapper.addStageDao(stagePojo);
        HashMap<Object, Object> ret = new HashMap<>();
        ret.put("info", "新建分标成功");
        ret.put("stageId", stageId);
        return TResponseVo.success(ret);
    }

    /**
     * 删除标段
     *
     * @param stageId
     */
    @Override
    public TResponseVo delStageService(String stageId) throws Exception {
        workingTableMapper.delStageDao(stageId);
        // 项目文件为该标段的 标段id置空
        workingTableMapper.updateFileStageIdDao(stageId);
        return TResponseVo.success("删除成功");
    }

    /**
     * 修改标段
     *
     * @param stagePojo
     */
    @Override
    public TResponseVo updateStageService(StagePojo stagePojo) throws Exception {
        String stageName = stagePojo.getStageName();
        try {
            int j = workingTableMapper.checkStageNameDao(stageName);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", stageName, "当前类错误码-111-项目名已存在");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(111);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", stageName, "当前类错误码-112-检查项目名SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(112);
            throw questionnaireException;
        }
//        String stageId = stagePojo.getStageId();
//        StagePojo stageInfoDao = workingTableMapper.getStageInfoDao(stageId);
//        // 更新到对象中
//        stageInfoDao.setStageName(stageName);
        workingTableMapper.updateStageDao(stagePojo);
        return TResponseVo.success("更新成功");
    }

    /**
     * 获取标段信息
     *
     * @param stageId
     */
    @Override
    public TResponseVo getStageInfoService(String stageId) throws Exception {
        StagePojo stageInfoDao = workingTableMapper.getStageInfoDao(stageId);
        return TResponseVo.success(stageInfoDao);
    }

    /**
     * 获取标段列表
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getStageListService(PagePojo pagePojo, String projectId) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        if ("0".equalsIgnoreCase(isPaged)) {
            List<StagePojo> superQuestionnaireClassPojo = null;
            superQuestionnaireClassPojo = workingTableMapper.getStageListDao(paramNameLike, null, projectId);
            return TResponseVo.success(superQuestionnaireClassPojo);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<StagePojo> superQuestionnaireClassPojo = workingTableMapper.getStageListDao(paramNameLike, null, projectId);
        PageInfo pageInfo = new PageInfo(superQuestionnaireClassPojo);
        return TResponseVo.success(pageInfo);
    }

    /**
     * 新建批次
     *
     * @param batchPojo
     */
    @Override
    public TResponseVo addBatchService(BatchPojo batchPojo, String userId) throws Exception {
        String batchNo = batchPojo.getBatchNo();
        try {
            int j = workingTableMapper.checkBatchNoDao(batchNo, null);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", batchNo, "当前类错误码-121-批次编码已存在");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(121);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", batchNo, "当前类错误码-122-检查名SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(122);
            throw questionnaireException;
        }
        String batchId = UUID.randomUUID().toString().replaceAll("-", "");
        batchPojo.setBatchId(batchId);
        batchPojo.setCreateUserId(userId);
        batchPojo.setCreateTime(new Date());
        Object includeUserId = batchPojo.getIncludeUserId();
        if (includeUserId == null) {
            batchPojo.setIncludeUserId(null);
        } else {
            batchPojo.setIncludeUserId(JSON.toJSONString(includeUserId));
        }
        Object batchProperty = batchPojo.getBatchProperty();
        if (batchProperty == null) {
            batchPojo.setBatchProperty(null);
        } else {
            batchPojo.setBatchProperty(JSON.toJSONString(batchProperty));
        }
        batchPojo.setMainPerson(userId);
        batchPojo.setBatchManager(userId);
        batchPojo.setFilePath(batchFilePath + "/" + batchId);
        workingTableMapper.addBatchDao(batchPojo);
        HashMap<Object, Object> ret = new HashMap<>();
        ret.put("info", "新建批次成功");
        ret.put("batchId", batchId);
        return TResponseVo.success(ret);
    }

    /**
     * 删除批次
     *
     * @param batchId
     */
    @Override
    public TResponseVo delBatchService(String batchId) throws Exception {
        workingTableMapper.delBatchDao(batchId);
        return TResponseVo.success("删除批次成功");
    }

    /**
     * 修改批次
     *
     * @param batchPojo
     */
    @Override
    public TResponseVo updateBatchService(BatchPojo batchPojo) throws Exception {
        String batchNo = batchPojo.getBatchNo();
        String batchId = batchPojo.getBatchId();
        try {
            int j = workingTableMapper.checkBatchNoDao(batchNo, batchId);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", batchNo, "当前类错误码-121-批次编码已存在");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(121);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", batchNo, "当前类错误码-122-检查批次编码SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(122);
            throw questionnaireException;
        }
        Object includeUserId = batchPojo.getIncludeUserId();
        if (includeUserId == null) {
            batchPojo.setIncludeUserId(null);
        } else {
            batchPojo.setIncludeUserId(JSON.toJSONString(includeUserId));
        }
        Object batchProperty = batchPojo.getBatchProperty();
        if (batchProperty == null) {
            batchPojo.setBatchProperty(null);
        } else {
            batchPojo.setBatchProperty(JSON.toJSONString(batchProperty));
        }
        workingTableMapper.updateBatchDao(batchPojo);
        return TResponseVo.success("更新成功");
    }

    /**
     * 修改批次项目信息属性
     *
     * @param labelValuePojo
     */
    @Override
    public TResponseVo updateBatchPropertyService(LabelValuePojo labelValuePojo) throws Exception {
        String batchId = labelValuePojo.getBatchId();
        String projectStageId = labelValuePojo.getProjectStageId();
        String label = labelValuePojo.getLabel();
        String value = labelValuePojo.getValue();
        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        String projectStageList = (String) batchInfoDao.getProjectStageList();
        List<Map<String, Object>> list = JSON.parseObject(projectStageList, List.class);
        List<Map<String, String>> projectProperty = new ArrayList<>();
        for (Map<String, Object> map : list) {
            if (projectStageId.equals(map.get("projectStageId"))) {
                projectProperty = (List) map.get("projectProperty");
                for (Map<String, String> stringStringMap : projectProperty) {
                    if (label.equals(stringStringMap.get("label"))) {
                        stringStringMap.put("value", value);
                        break;
                    }
                }
                break;
            }
        }
        projectStageList = JSON.toJSONString(list);
        workingTableMapper.updateBatchProjectDao(batchId, projectStageList);
        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("info", "调整完成");
        return TResponseVo.success(retMap);
    }

    /**
     * 获取批次信息
     *
     * @param batchId
     */
    @Override
    public TResponseVo getBatchInfoService(String batchId) throws Exception {
        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);

        String includeUserId = (String) batchInfoDao.getIncludeUserId();
        List<String> list = JSON.parseObject(includeUserId, List.class);
        batchInfoDao.setIncludeUserId(list);

        String batchProperty = (String) batchInfoDao.getBatchProperty();
        Map map = JSON.parseObject(batchProperty, Map.class);
        batchInfoDao.setBatchProperty(map);

        String includeUserInfo = (String) batchInfoDao.getIncludeUserInfo();
        List<Map<String, Object>> includeUserInfoList = JSON.parseObject(includeUserInfo, List.class);
        batchInfoDao.setIncludeUserInfo(includeUserInfoList);
        return TResponseVo.success(batchInfoDao);
    }

    /**
     * 获取批次列表
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getBatchListService(PagePojo pagePojo, String userId) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<BatchPojo> superQuestionnaireClassPojo = workingTableMapper.getBatchListDao(paramNameLike, null);
        List<BatchPojo> superQuestionnaireClassPojoTmp = new ArrayList<>();
        superQuestionnaireClassPojoTmp.addAll(superQuestionnaireClassPojo);
        for (BatchPojo batchPojo : superQuestionnaireClassPojoTmp) {
            String includeUserId = (String) batchPojo.getIncludeUserId();
            String mainPerson = batchPojo.getMainPerson();
            String batchManager = batchPojo.getBatchManager();
            List<String> includeUserIdList = JSON.parseObject(includeUserId, List.class);
            if (includeUserId == null) {
                includeUserIdList = new ArrayList<>();
            }
            if (includeUserIdList.contains(userId) || userId.equals(mainPerson) || userId.equals(batchManager)) {
                batchPojo.setIncludeUserId(includeUserIdList);
                String batchProperty = (String) batchPojo.getBatchProperty();
                Map map = JSON.parseObject(batchProperty, Map.class);
                batchPojo.setBatchProperty(map);
                String includeUserInfo = (String) batchPojo.getIncludeUserInfo();
                List<Map<String, Object>> includeUserInfoList = JSON.parseObject(includeUserInfo, List.class);
                batchPojo.setIncludeUserInfo(includeUserInfoList);
            } else {
                superQuestionnaireClassPojo.remove(batchPojo); // 只保留当前用户参与的批次
            }
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(superQuestionnaireClassPojo);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(superQuestionnaireClassPojo, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", superQuestionnaireClassPojo.size());
        return TResponseVo.success(resultMap);

    }

    /**
     * 获取当前用户有权限的项目与分包信息
     *
     * @param userId
     */
    @Override
    public TResponseVo getProjectListByUserService(String userId) throws Exception {
        List<ProjectPojo> projectListByUserDao = workingTableMapper.getProjectListByUserDao();
        List<ProjectPojo> projectListByUserDaoTmp = new ArrayList<>();
        projectListByUserDaoTmp.addAll(projectListByUserDao);
        ArrayList<String> projectIdList = new ArrayList<>();
        for (ProjectPojo projectPojo : projectListByUserDaoTmp) {  // 只保留当前用户有权限的项目
            Object biddingUserAgency = projectPojo.getBiddingUserAgency();
            if (biddingUserAgency == null) {
                projectListByUserDao.remove(projectPojo);
            } else {
                List<String> userIdList = JSON.parseObject((String) biddingUserAgency, List.class);
                if (!userIdList.contains(userId)) {
                    projectListByUserDao.remove(projectPojo);
                } else {
                    projectIdList.add(projectPojo.getProjectId());
                }
            }
        }

        String projectIdListStr = new JsonKeyUtils().listToString(projectIdList, "','");
        List<PackagePojo> packagePojoList = workingTableMapper.getPackageListByProjectIdListDao(projectIdListStr);
        for (ProjectPojo projectPojo : projectListByUserDao) {
            String projectId = projectPojo.getProjectId();
            for (PackagePojo packagePojo : packagePojoList) {
                if (projectId.equals(packagePojo.getProjectId())) {

                }
            }
        }


        return null;
    }

    /**
     * 批次管理-项目列表-添加项目
     *
     * @param type
     */
    @Override
    public TResponseVo getOptionsService(String type) throws Exception {
        List<OptionsPojo> optionsDao = workingTableMapper.getOptionsDao(type, null);
        return TResponseVo.success(optionsDao);
    }

    /**
     * 批次管理-项目列表-添加项目
     */
    @Override
    public TResponseVo addBatchProjectService(String batchId, String projectStageName, String buyType, String mainPerson) throws Exception {
        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        String projectStageListStr = (String) batchInfoDao.getProjectStageList();
        List<Map<String, Object>> projectStageList = JSON.parseObject(projectStageListStr, List.class);
        if (projectStageListStr == null) {
            projectStageList = new ArrayList<>();
        }
        // 新增
        int packageNo = projectStageList.size() + 1;
        Map<String, Object> map = new HashMap<>();
        String projectStageId = UUID.randomUUID().toString().replaceAll("-", "");
        map.put("projectStageId", projectStageId);
        map.put("packageNo", "包" + String.valueOf(packageNo));
        map.put("packageNoInt", packageNo);
        map.put("projectStageName", projectStageName);
        map.put("buyType", buyType);
        map.put("mainPerson", mainPerson);

        // 配置项目信息
        ArrayList<Map<String, String>> batchPropertyList = new ArrayList<>();
        HashMap<String, String> m1 = new HashMap();
        HashMap<String, String> m2 = new HashMap();
        HashMap<String, String> m3 = new HashMap();
        HashMap<String, String> m4 = new HashMap();
        HashMap<String, String> m5 = new HashMap();
        HashMap<String, String> m6 = new HashMap();
        HashMap<String, String> m7 = new HashMap();
        HashMap<String, String> m8 = new HashMap();
        HashMap<String, String> m9 = new HashMap();
        HashMap<String, String> m10 = new HashMap();
        HashMap<String, String> m11 = new HashMap();
        HashMap<String, String> m12 = new HashMap();
        HashMap<String, String> m13 = new HashMap();
        m1.put("label", "采购内容及描述");
        m2.put("label", "采购范围");
        m3.put("label", "服务时间");
        m4.put("label", "潜在供应商名单及联系方式");
        m5.put("label", "采购方式");
        m6.put("label", "资质及业绩要求");
        m7.put("label", "报价方式");
        m8.put("label", "评标（审）办法");
        m9.put("label", "发包人要求");
        m10.put("label", "技术资料");
        m11.put("label", "合同范本");
        m12.put("label", "工程类设计图纸，工程量清单及控制价");
        m13.put("label", "需求部门，需求人及联系方式");
        m1.put("value", "");
        m2.put("value", "");
        m3.put("value", "");
        m4.put("value", "");
        m5.put("value", "");
        m6.put("value", "");
        m7.put("value", "");
        m8.put("value", "");
        m9.put("value", "");
        m10.put("value", "");
        m11.put("value", "");
        m12.put("value", "");
        m13.put("value", "");
        batchPropertyList.add(m1);
        batchPropertyList.add(m2);
        batchPropertyList.add(m3);
        batchPropertyList.add(m4);
        batchPropertyList.add(m5);
        batchPropertyList.add(m6);
        batchPropertyList.add(m7);
        batchPropertyList.add(m8);
        batchPropertyList.add(m9);
        batchPropertyList.add(m10);
        batchPropertyList.add(m11);
        batchPropertyList.add(m12);
        batchPropertyList.add(m13);
        map.put("projectProperty", batchPropertyList);

        projectStageList.add(map);
        projectStageListStr = JSON.toJSONString(projectStageList);
        workingTableMapper.updateBatchProjectDao(batchId, projectStageListStr);
        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("info", "添加完成");
        retMap.put("projectStageId", projectStageId);
        return TResponseVo.success(retMap);
    }

    /**
     * 批次管理-项目列表-删除项目
     *
     * @param batchId
     */
    @Override
    public TResponseVo delBatchProjectService(String batchId, String projectStageId) throws Exception {
        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        String projectStageListStr = (String) batchInfoDao.getProjectStageList();
        List<Map<String, Object>> projectStageList = JSON.parseObject(projectStageListStr, List.class);
        if (projectStageListStr == null) {
            projectStageList = new ArrayList<>();
        }
        // 删除项目分包
        List<Map<String, Object>> projectStageListTmp = new ArrayList<>();
        projectStageListTmp.addAll(projectStageList);
        int packageNoInt = 0;
        for (Map<String, Object> map : projectStageListTmp) {
            String projectStageId1 = (String) map.get("projectStageId");
            if (projectStageId1.equals(projectStageId)) {
                packageNoInt = (Integer) map.get("packageNoInt");
                projectStageList.remove(map);
                break;
            }
        }
        // 重新给分包顺序
        for (Map<String, Object> map : projectStageList) {
            Integer packageNoInt1 = (Integer) map.get("packageNoInt");
            if (packageNoInt1 > packageNoInt) {  // 如果剩下的分包中包号大于被删除的包号 -1
                int packageNoInt2 = packageNoInt1 - 1;
                map.put("packageNo", "包" + String.valueOf(packageNoInt2));
                map.put("packageNoInt", packageNoInt2);
            }
        }
        // 更新库
        projectStageListStr = JSON.toJSONString(projectStageList);
        workingTableMapper.updateBatchProjectDao(batchId, projectStageListStr);
        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("info", "删除完成");
        return TResponseVo.success(retMap);
    }

    /**
     * 批次管理-项目列表-获取项目信息
     *
     * @param batchId
     * @param projectStageId
     */
    @Override
    public TResponseVo getBatchProjectInfoService(String batchId, String projectStageId) throws Exception {
        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        if (batchInfoDao == null) {
            return TResponseVo.error("该批次不存在或已删除");
        }
        String projectStageListStr = (String) batchInfoDao.getProjectStageList();
        List<Map<String, Object>> projectStageList = JSON.parseObject(projectStageListStr, List.class);
        if (projectStageListStr == null) {
            projectStageList = new ArrayList<>();
        }

        Map<String, Object> projectStageInfo = new HashMap<>();
        for (Map<String, Object> map : projectStageList) {
            if (((String) map.get("projectStageId")).equals(projectStageId)) {
                projectStageInfo = map;
                break;
            }
        }

        // 补充负责人姓名
        List<LabelValuePojo> userListDao = workingTableMapper.getUserListDao();
        for (LabelValuePojo labelValuePojo : userListDao) {
            if (labelValuePojo.getValue().equals((String) projectStageInfo.get("mainPerson"))) {
                projectStageInfo.put("mainPersonName", labelValuePojo.getLabel());
            }
        }

        // 返回批次名称
        projectStageInfo.put("batchId", batchInfoDao.getBatchId());
        projectStageInfo.put("batchName", batchInfoDao.getBatchName());

        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("info", "查询成功");
        retMap.put("data", projectStageInfo);
        return TResponseVo.success(projectStageInfo);
    }

    /**
     * 批次管理-项目列表-获取项目列表
     *
     * @param batchId
     */
    @Override
    public TResponseVo getBatchProjectListService(PagePojo pagePojo, String batchId) throws Exception {

        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        if (batchInfoDao == null) {
            return TResponseVo.error("该批次不存在或已删除");
        }
        String projectStageListStr = (String) batchInfoDao.getProjectStageList();
        List<Map<String, Object>> projectStageList = JSON.parseObject(projectStageListStr, List.class);
        if (projectStageListStr == null) {
            projectStageList = new ArrayList<>();
        }


        // 搜索
        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");

            List<Map<String, Object>> projectStageListTmp = new ArrayList<>();
            projectStageListTmp.addAll(projectStageList);
            for (Map<String, Object> map : projectStageListTmp) {
                String projectStageName = (String) map.get("projectStageName");
                if (!projectStageName.contains(paramNameLike)) {
                    projectStageList.remove(map);
                }
            }
        }

        // 补充负责人姓名
        List<LabelValuePojo> userListDao = workingTableMapper.getUserListDao();
        for (Map<String, Object> map : projectStageList) {
            String mainPerson = (String) map.get("mainPerson");
            for (LabelValuePojo labelValuePojo : userListDao) {
                if (labelValuePojo.getValue().equals(mainPerson)) {
                    map.put("mainPersonName", labelValuePojo.getLabel());
                }
            }
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(projectStageList);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(projectStageList, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", projectStageList.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 批次管理-项目列表-调整顺序
     *
     * @param batchPojo
     */
    @Override
    public TResponseVo orderBatchProjectListService(BatchPojo batchPojo) throws Exception {
        List<String> projectStageIdList = (List) batchPojo.getProjectStageList();
        String batchId = batchPojo.getBatchId();

        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        if (batchInfoDao == null) {
            return TResponseVo.error("该批次不存在或已删除");
        }
        String projectStageListStr = (String) batchInfoDao.getProjectStageList();
        List<Map<String, Object>> projectStageList = JSON.parseObject(projectStageListStr, List.class);
        if (projectStageListStr == null) {
            projectStageList = new ArrayList<>();
        }

        // 按照顺序重新排列
        List<Map<String, Object>> projectStageListNew = new ArrayList<>();
        int packageNoInt = 1;
        for (String projectStageId : projectStageIdList) {
            for (Map<String, Object> map : projectStageList) {
                if (projectStageId.equals(map.get("projectStageId"))) {
                    map.put("packageNo", "包" + String.valueOf(packageNoInt));
                    map.put("packageNoInt", packageNoInt);
                    projectStageListNew.add(map);
                    packageNoInt += 1;
                    break;
                }
            }
        }

        // 更新库
        projectStageListStr = JSON.toJSONString(projectStageListNew);
        workingTableMapper.updateBatchProjectDao(batchId, projectStageListStr);
        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("info", "调整完成");
        return TResponseVo.success(retMap);
    }

    /**
     * 批次信息-添加人员
     *
     * @param batchId
     * @param userId
     */
    @Override
    public TResponseVo addBatchUserService(String batchId, String userId) throws Exception {
        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        String includeUserId = (String) batchInfoDao.getIncludeUserId();
        List<String> includeUserIdList = JSON.parseObject(includeUserId, List.class);
        if (includeUserId == null) {
            includeUserIdList = new ArrayList<>();
        }
        String includeUserInfo = (String) batchInfoDao.getIncludeUserInfo();
        List<Map<String, Object>> includeUserInfoList = JSON.parseObject(includeUserInfo, List.class);
        if (includeUserInfo == null) {
            includeUserInfoList = new ArrayList<>();
        }
        // 获取用户列表
        List<LabelValuePojo> userListDao = workingTableMapper.getUserListDao();
        if (!includeUserIdList.contains(userId)) {
            includeUserIdList.add(userId);
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            for (LabelValuePojo labelValuePojo : userListDao) {
                if (userId.equals(labelValuePojo.getValue())) {
                    map.put("userName", labelValuePojo.getLabel());
                    break;
                }
            }
            map.put("joinTime", new TimeFormatUtils().timestampToStr(new Date()));
            includeUserInfoList.add(map);
        }
        includeUserId = JSON.toJSONString(includeUserIdList);
        includeUserInfo = JSON.toJSONString(includeUserInfoList);
        workingTableMapper.updateBatchUserListDao(batchId, includeUserId, includeUserInfo);
        return TResponseVo.success("添加成功");
    }

    /**
     * 批次信息-删除人员
     *
     * @param batchId
     * @param userId
     */
    @Override
    public TResponseVo delBatchUserService(String batchId, String userId) throws Exception {
        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        String includeUserId = (String) batchInfoDao.getIncludeUserId();
        List<String> includeUserIdList = JSON.parseObject(includeUserId, List.class);
        if (includeUserId == null) {
            includeUserIdList = new ArrayList<>();
        }
        String includeUserInfo = (String) batchInfoDao.getIncludeUserInfo();
        List<Map<String, Object>> includeUserInfoList = JSON.parseObject(includeUserInfo, List.class);
        if (includeUserInfo == null) {
            includeUserInfoList = new ArrayList<>();
        }
        // 删除用户
        List<String> includeUserIdListTmp = new ArrayList<>();
        includeUserIdListTmp.addAll(includeUserIdList);
        for (String s : includeUserIdListTmp) {
            if (s.equals(userId)) {
                includeUserIdList.remove(s);
                break;
            }
        }
        List<Map<String, Object>> includeUserInfoListTmp = new ArrayList<>();
        includeUserInfoListTmp.addAll(includeUserInfoList);
        for (Map<String, Object> map : includeUserInfoListTmp) {
            String s = (String) map.get("userId");
            if (s.equals(userId)) {
                includeUserInfoList.remove(map);
                break;
            }
        }
        // 更新库
        includeUserId = JSON.toJSONString(includeUserIdList);
        includeUserInfo = JSON.toJSONString(includeUserInfoList);
        workingTableMapper.updateBatchUserListDao(batchId, includeUserId, includeUserInfo);
        return TResponseVo.success("删除成功");
    }

    /**
     * 批次信息-获取用户清单
     *
     * @param pagePojo
     * @param batchId
     */
    @Override
    public TResponseVo getBatchUserListService(PagePojo pagePojo, String batchId) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        if (batchInfoDao == null) {
            return TResponseVo.error("该批次不存在或已删除");
        }
        String includeUserInfo = (String) batchInfoDao.getIncludeUserInfo();
        List<Map<String, Object>> includeUserInfoList = JSON.parseObject(includeUserInfo, List.class);
        if (includeUserInfo == null) {
            includeUserInfoList = new ArrayList<>();
        }

        // 搜索
        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");

            List<Map<String, Object>> includeUserInfoListTmp = new ArrayList<>();
            includeUserInfoListTmp.addAll(includeUserInfoList);
            for (Map<String, Object> map : includeUserInfoListTmp) {
                String userName = (String) map.get("userName");
                if (!userName.contains(paramNameLike)) {
                    includeUserInfoList.remove(map);
                }
            }
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(includeUserInfoList);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(includeUserInfoList, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", includeUserInfoList.size());
        return TResponseVo.success(resultMap);
    }


    /**
     * 批次文件上传
     *
     * @param file
     * @param userId
     */
    @Override
    public TResponseVo uploadBatchFileService(MultipartFile file, String userId, String batchId, String fileNameNew, String fileTypeId, String projectStageId) throws Exception {
        String fileUuid = UUID.randomUUID().toString().replaceAll("-", "");

        String fileName = file.getOriginalFilename();
        int i = fileName.lastIndexOf(".");
        if (i == -1) {
            return TResponseVo.error("请补全上传文件的后缀名");
        }
        String extension = fileName.substring(i);// 获取后缀名
        String fileNameWithoutExtension = fileNameNew;
        // 如果fileNameNew为空 则用原始名称
        if (StringUtils.isBlank(fileNameNew)) {
            fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
        }
        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        String filePath = batchInfoDao.getFilePath();
        File filePathNew = new File(filePath);
        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        // 查询批次额定空间大小
        long spaceSize = Integer.valueOf(batchInfoDao.getSpaceSize()) * 1024 * 1024;
        // 当前存储情况
        long totalSizeOfFile = new FileSizeUtils().getTotalSizeOfFile(filePath);
        long size = file.getSize();
        if (totalSizeOfFile + size > spaceSize) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadFileService", fileNameWithoutExtension, "当前类错误码--项目空间不足,上传失败");
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "项目空间不足,上传失败");
        }

        // 检查相同文件是否已存在
        File docxFileNew = new File(filePath + "/" + fileNameWithoutExtension + extension);
        if (!docxFileNew.exists()) {
            try {
                file.transferTo(docxFileNew);
            } catch (IOException e) {
                return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传报错");
            }
            System.out.println("文件已创建成功");
        } else {
            System.out.println("文件已存在");
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "该项目下已存在同名文件");
        }

        BatchFilePojo batchFilePojo = new BatchFilePojo();
        batchFilePojo.setBatchId(batchId);
        batchFilePojo.setFileUuid(fileUuid);
        batchFilePojo.setFileName(fileNameWithoutExtension);
        batchFilePojo.setFileExtension(extension);
        batchFilePojo.setCreateTime(new Date());
        batchFilePojo.setCreateUserId(userId);
        batchFilePojo.setMainPerson(userId);
        batchFilePojo.setFileType(fileTypeId);
        batchFilePojo.setFilePath(filePath + "/" + fileNameWithoutExtension + extension);
        batchFilePojo.setIsStruct("0");
        batchFilePojo.setIsShow("1");
        batchFilePojo.setProjectStageId(projectStageId);
        workingTableMapper.addBatchFileDao(batchFilePojo);

        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "上传成功");
        ret.put("batchFilePojo", batchFilePojo);
        return TResponseVo.success(ret);
    }

    /**
     * 批次文件新增
     *
     * @param file
     * @param userId
     * @param batchId
     */
    @Override
    public TResponseVo addBatchFileService(MultipartFile file, String userId, String batchId) throws Exception {

        return null;
    }

    /**
     * 批次文件删除
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo delBatchFileService(String fileUuid) throws Exception {
        BatchFilePojo batchFileInfoDao = workingTableMapper.getBatchFileInfoDao(fileUuid);
        if (batchFileInfoDao == null) {
            return TResponseVo.success("删除成功");
        }
        // 判断是上传的还是选择的
        String isStruct = (String) batchFileInfoDao.getIsStruct();
        if (isStruct.equals("0")) {
            // 如果是上传的 把原文件也删了
            String filePath = batchFileInfoDao.getFilePath();
            File file = new File(filePath);
            if (file.isFile()) {
                file.delete();
            }
        } else {
            // 如果是选择的 把文件退回给作者
//            workingTableMapper.updateFileStatusDao(fileUuid, "1");
        }
        Integer integer = workingTableMapper.delBatchFileDao(fileUuid);
        if (integer > 0) {
            return TResponseVo.success("删除成功");
        } else {
            return TResponseVo.error("文件不存在或已删除");
        }
    }

    /**
     * 批次文件修改
     *
     * @param fileUuid
     * @param projectStageId
     */
    @Override
    public TResponseVo updateBatchFileService(String fileUuid, String projectStageId) throws Exception {
        if ("-1".equals(projectStageId)) {
            projectStageId = null;
        }
        workingTableMapper.updateBatchFileDao(fileUuid, projectStageId);
        return TResponseVo.success("更新成功");
    }

    /**
     * 获取批次文件信息
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo getBatchFileInfoService(String fileUuid) throws Exception {
        BatchFilePojo batchFileInfoDao = workingTableMapper.getBatchFileInfoDao(fileUuid);
        return TResponseVo.success(batchFileInfoDao);
    }

    /**
     * 获取批次文件清单
     *
     * @param pagePojo
     * @param batchId
     */
    @Override
    public TResponseVo getBatchFileListService(PagePojo pagePojo, String batchId, String projectStageId) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        if (batchInfoDao == null) {
            return TResponseVo.error("该批次不存在或已删除");
        }

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<BatchFilePojo> batchFileListDao = workingTableMapper.getBatchFileListDao(paramNameLike, batchId, projectStageId);

        if (projectStageId != null) {
            // 补充projectStageName批次项目名
            String projectStageListStr = (String) batchInfoDao.getProjectStageList();
            List<Map<String, Object>> projectStageList = JSON.parseObject(projectStageListStr, List.class);
            if (projectStageListStr == null) {
                projectStageList = new ArrayList<>();
            }
            for (BatchFilePojo batchFilePojo : batchFileListDao) {
                for (Map<String, Object> map : projectStageList) {
                    if (batchFilePojo.getProjectStageId().equals((String) map.get("projectStageId"))) {
                        batchFilePojo.setProjectStageName((String) map.get("projectStageName"));
                        break;
                    }
                }
            }
        }


        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(batchFileListDao);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(batchFileListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", batchFileListDao.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 批次新加入分包 多选文件
     *
     * @param packageId
     * @param fileUuidList
     * @param batchId
     * @param userId
     */
    @Override
    public TResponseVo addPackageToBatchService(String packageId, List<String> fileUuidList, String batchId, String userId) throws Exception {

        return null;
    }

    /**
     * 获取用户下所有涉及项目及项目下标段信息列表树
     *
     * @param userId
     */
    @Override
    public TResponseVo getUserStageListService(String userId) throws Exception {
        List<ProjectPojo> ProjectList = workingTableMapper.getUserProjectListDao(userId);
        ArrayList<String> ProjectIdList = new ArrayList<>();
        for (ProjectPojo projectPojo : ProjectList) {
            ProjectIdList.add(projectPojo.getProjectId());
            projectPojo.setChildren(new ArrayList<StagePojo>());
        }
        String ProjectListStr = JsonKeyUtils.listToString(ProjectIdList, "','");
        List<StagePojo> userStageList = workingTableMapper.getUserStageListDao(ProjectListStr);
        // 组成树状结构
        for (ProjectPojo project : ProjectList) {
            for (StagePojo stagePojo : userStageList) {
                if (stagePojo.getProjectId().equals(project.getProjectId())) {
                    List<StagePojo> children = project.getChildren();
                    children.add(stagePojo);
                }
            }
        }
        return TResponseVo.success(ProjectList);
    }


    /**
     * 新建公司
     *
     * @param companyPojo
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo addCompanyService(CompanyPojo companyPojo, String userId) throws Exception {
//        List<Map<String, String>> shareholderNameList = companyPojo.getShareholderNameList();
//        String shareholderNameListStr = JSON.toJSONString(shareholderNameList);
        String companyId = companyPojo.getCompanyId();
//        companyPojo.setShareholderNameListStr(shareholderNameListStr);
        companyPojo.setCreateUserId(userId);
        companyPojo.setCreateTime(new Date());
        String authorizationCode = RandomUtils.randomUtil();
        companyPojo.setAuthorizationCode(authorizationCode);
        workingTableMapper.addCompanyDao(companyPojo);

        // 将本人加入该公司
        DocUserPojo userInfoDao = workingTableMapper.getUserInfoDao(userId);
        String companyIdListStr = userInfoDao.getCompanyIdList();
        List<String> companyIdList = JsonKeyUtils.stringToList(companyIdListStr);
//        ArrayList<String> companyIdArrayList = new ArrayList<>();
//        companyIdArrayList.addAll(companyIdList);
//        companyIdArrayList.add(companyId);
        companyIdList.add(companyId);
        String companyIdListStrNew = JsonKeyUtils.listToString(companyIdList);
        workingTableMapper.joinCompanyDao(companyIdListStrNew, userId);

        HashMap<Object, Object> ret = new HashMap<>();
        ret.put("info", "新建公司信息成功");
        ret.put("companyId", companyId);
        return TResponseVo.success(ret);
    }

    /**
     * 删除公司
     *
     * @param companyId
     */
    @Override
    public TResponseVo delCompanyService(String companyId) throws Exception {
        workingTableMapper.delCompanyDao(companyId);
        return TResponseVo.success("删除企业信息成功");
    }

    /**
     * 修改公司
     *
     * @param companyPojo
     */
    @Override
    public TResponseVo updateCompanyService(CompanyPojo companyPojo) throws Exception {
//        List<Map<String, String>> shareholderNameList = companyPojo.getShareholderNameList();
//        String shareholderNameListStr = JSON.toJSONString(shareholderNameList);
//        companyPojo.setShareholderNameListStr(shareholderNameListStr);
        workingTableMapper.updateCompanyDao(companyPojo);
        return TResponseVo.success("更新成功");
    }

    /**
     * 获取公司信息
     *
     * @param companyId
     */
    @Override
    public TResponseVo getCompanyInfoService(String companyId) throws Exception {
        CompanyPojo companyInfoDao = workingTableMapper.getCompanyInfoDao(companyId);
        String shareholderNameListStr = companyInfoDao.getShareholderNameListStr();
        List<Map<String, String>> shareholderNameList = JSON.parseObject(shareholderNameListStr, List.class);
        companyInfoDao.setShareholderNameList(shareholderNameList);
        companyInfoDao.setShareholderNameListStr(null);
        // 获取文件名
        String businessLicense = companyInfoDao.getBusinessLicense();
        String businessLicenseName = businessLicense.substring(businessLicense.lastIndexOf("/") + 1);
        companyInfoDao.setBusinessLicenseName(businessLicenseName);
        String bankLicense = companyInfoDao.getBankLicense();
        String bankLicenseName = bankLicense.substring(bankLicense.lastIndexOf("/") + 1);
        companyInfoDao.setBankLicenseName(bankLicenseName);
        return TResponseVo.success(companyInfoDao);
    }

    /**
     * 获取公司列表
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getCompanyListService(PagePojo pagePojo, String userId) throws Exception {
        String companyIdListStr = null;
        String mainCompany = null;
        if (userId != null) {
            DocUserPojo userInfoDao = workingTableMapper.getUserInfoDao(userId);
            String companyIdList = userInfoDao.getCompanyIdList();
            List<String> strings = JsonKeyUtils.stringToList(companyIdList);
            companyIdListStr = JsonKeyUtils.listToString(strings, "','");
            mainCompany = userInfoDao.getCompanyId();
        }

        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        if ("0".equalsIgnoreCase(isPaged)) {
            List<CompanyPojo> superQuestionnaireClassPojo = null;
            superQuestionnaireClassPojo = workingTableMapper.getCompanyListDao(paramNameLike, null, companyIdListStr);
            for (CompanyPojo companyPojo : superQuestionnaireClassPojo) {
                if (companyPojo.getCompanyId().equals(mainCompany)) {
                    companyPojo.setMainCompany(true);
                }
                // 设置股东信息list
                String shareholderNameListStr = companyPojo.getShareholderNameListStr();
                List<Map<String, String>> shareholderNameList = JSON.parseObject(shareholderNameListStr, List.class);
                companyPojo.setShareholderNameList(shareholderNameList);
                companyPojo.setShareholderNameListStr(null);
                // 获取文件名
                String businessLicense = companyPojo.getBusinessLicense();
                String businessLicenseName = businessLicense != null ? businessLicense.substring(businessLicense.lastIndexOf("/") + 1) : null;
                companyPojo.setBusinessLicenseName(businessLicenseName);
                String bankLicense = companyPojo.getBankLicense();
                String bankLicenseName = bankLicense != null ? bankLicense.substring(bankLicense.lastIndexOf("/") + 1) : null;
                companyPojo.setBankLicenseName(bankLicenseName);
            }
            return TResponseVo.success(superQuestionnaireClassPojo);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<CompanyPojo> superQuestionnaireClassPojo = workingTableMapper.getCompanyListDao(paramNameLike, null, companyIdListStr);
        for (CompanyPojo companyPojo : superQuestionnaireClassPojo) {
            if (companyPojo.getCompanyId().equals(mainCompany)) {
                companyPojo.setMainCompany(true);
            }
            // 设置股东信息list
            String shareholderNameListStr = companyPojo.getShareholderNameListStr();
            List<Map<String, String>> shareholderNameList = JSON.parseObject(shareholderNameListStr, List.class);
            companyPojo.setShareholderNameList(shareholderNameList);
            companyPojo.setShareholderNameListStr(null);
            // 获取文件名
            String businessLicense = companyPojo.getBusinessLicense();
            String businessLicenseName = businessLicense.substring(businessLicense.lastIndexOf("/") + 1);
            companyPojo.setBusinessLicenseName(businessLicenseName);
            String bankLicense = companyPojo.getBankLicense();
            String bankLicenseName = bankLicense.substring(bankLicense.lastIndexOf("/") + 1);
            companyPojo.setBankLicenseName(bankLicenseName);
        }
        PageInfo pageInfo = new PageInfo(superQuestionnaireClassPojo);
        return TResponseVo.success(pageInfo);
    }

    /**
     * 修改默认公司
     *
     * @param companyId
     * @param userId
     */
    @Override
    public TResponseVo setDefaultCompanyService(String companyId, String userId) throws Exception {
        workingTableMapper.setDefaultCompanyDao(companyId, userId);
        return TResponseVo.success("修改成功");
    }

    /**
     * 退出公司
     *
     * @param companyId
     * @param userId
     */
    @Override
    public TResponseVo quitCompanyService(String companyId, String userId) throws Exception {
        DocUserPojo userInfoDao = workingTableMapper.getUserInfoDao(userId);
        String companyIdNow = userInfoDao.getCompanyId();
        if (companyIdNow.equals(companyId)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "无法退出当前默认企业");
        }
        String companyIdList = userInfoDao.getCompanyIdList();
        List<String> strings = JsonKeyUtils.stringToList(companyIdList);
        ArrayList<String> strings1 = new ArrayList<>();
        strings1.addAll(strings);
        for (String string : strings) {
            if (string.equals(companyId)) {
                strings1.remove(string);
            }
        }
        String companyIdListStr = JsonKeyUtils.listToString(strings1);
        workingTableMapper.updateUserCompanyIdListDao(companyIdListStr, userId);
        return TResponseVo.success("退出成功");
    }

    /**
     * 根据搜索码获取公司名称
     *
     * @param authorizationCode
     */
    @Override
    public TResponseVo getCompanyByAuthService(String authorizationCode) throws Exception {
        CompanyPojo companyByAuthDao = workingTableMapper.getCompanyByAuthDao(authorizationCode);
        return TResponseVo.success(companyByAuthDao);
    }

    /**
     * 申请加入公司
     *
     * @param companyId
     * @param userId
     */
    @Override
    public TResponseVo applyJoinCompanyService(String companyId, String userId) throws Exception {
        String applyId = UUID.randomUUID().toString().replaceAll("-", "");
        ApplyPojo applyPojo = new ApplyPojo();
        applyPojo.setApplyId(applyId);
        applyPojo.setCreateTime(new Date());
        Map<String, String> contentMap = new HashMap();
        contentMap.put("companyId", companyId);
        contentMap.put("userId", userId);
        String contentMapStr = JSON.toJSONString(contentMap);
        applyPojo.setApplyContent(contentMapStr);
        applyPojo.setApplyUserId(userId);
        applyPojo.setApplyDesc("申请加入公司");
        workingTableMapper.applyJoinCompanyDao(applyPojo);
        return TResponseVo.success("申请已提交");
    }

    /**
     * 同意加入公司
     *
     * @param applyContent
     */
    @Override
    public TResponseVo joinCompanyService(String applyContent) throws Exception {
        Map<String, String> applyContentMap = JSON.parseObject(applyContent, Map.class);
        String userId = applyContentMap.get("userId");
        String companyId = applyContentMap.get("companyId");
        DocUserPojo userInfoDao = workingTableMapper.getUserInfoDao(userId);
        String companyIdListStr = userInfoDao.getCompanyIdList();
        List<String> companyIdList = JsonKeyUtils.stringToList(companyIdListStr);
        if (companyIdList.contains(companyId)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "当前用户已存在于该公司");
        } else {
            companyIdList.add(companyId);
            String companyIdListStrNew = JsonKeyUtils.listToString(companyIdList);
            workingTableMapper.joinCompanyDao(companyIdListStrNew, userId);
            return TResponseVo.success("加入成功");
        }
    }

    /**
     * 项目策划
     *
     * @param ProjectId
     */
    @Override
    public TResponseVo projectPlanService(String ProjectId) throws Exception {
        return null;
    }

    /**
     * 项目推荐
     *
     * @param projectTypeId
     */
    @Override
    public TResponseVo getIntroductionListService(String projectTypeId) throws Exception {
        return null;
    }

    /**
     * 获取招标文件列表
     *
     * @param userId
     */
    @Override
    public TResponseVo getDocumentListService(String userId) throws Exception {
        List<DocFileIndexPojo> bidDocumentListDao = workingTableMapper.getBidDocumentListDao();
        List<DocFileIndexPojo> bidDocumentListDaoTmp = new ArrayList<>();
        for (DocFileIndexPojo docFileIndexPojo : bidDocumentListDao) {
            String includeUserList = docFileIndexPojo.getIncludeUserList();
            List<String> userList = new JsonKeyUtils().stringToList(includeUserList);
            if (userList.contains(userId) || docFileIndexPojo.getCreateUserId().equals(userId)) {
                bidDocumentListDaoTmp.add(docFileIndexPojo);
            }
        }
        List<LabelValuePojo> retList = new ArrayList<>();
        for (DocFileIndexPojo docFileIndexPojo : bidDocumentListDaoTmp) {
            LabelValuePojo labelValuePojo = new LabelValuePojo();
            labelValuePojo.setValue(docFileIndexPojo.getFileUuid());
            labelValuePojo.setLabel(docFileIndexPojo.getFileName());
            retList.add(labelValuePojo);
        }
        return TResponseVo.success(retList);
    }

    /**
     * 新增项目文件
     *
     * @param
     */
    @Override
    public TResponseVo addProjectFileService(MultipartFile file, String projectId, String fileName, String packageId, String userId, String fileInfo, String fileType, String fileUuid) throws Exception {
        // 检查文件重名
        try {
            int j = workingTableMapper.checkFileNameDao(fileName, null, projectId);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", fileName, "当前类错误码-141-项目文件名已存在");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(141);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", fileName, "当前类错误码-142-检查项目文件名SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(142);
            throw questionnaireException;
        }

        if (fileUuid == null) {
            // 新上传
            return uploadFileService(file, projectId, fileName, packageId, userId, fileInfo, fileType);
        } else {
            // 从工作台里选
            DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
            ProjectFilePojo projectFilePojo1 = new ProjectFilePojo();
            projectFilePojo1.setFileUuid(fileUuid);
            projectFilePojo1.setFileName(StringUtils.isBlank(fileName) ? docAllInfoDao.getFileName() : fileName);
            projectFilePojo1.setFileExtension(".docx");
            projectFilePojo1.setCreateTime(new Date());
            projectFilePojo1.setCreateUserId(userId);
            projectFilePojo1.setMainPerson(userId);
            projectFilePojo1.setPackageId(packageId == null ? "" : packageId);
            projectFilePojo1.setFilePath(null);
            projectFilePojo1.setFileInfo(fileInfo);
            projectFilePojo1.setFileType(fileType);
            projectFilePojo1.setProjectId(projectId);
            projectFilePojo1.setIsStruct("1");
            try {
                workingTableMapper.addProjectFileDao(projectFilePojo1);
            } catch (Exception e) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addProjectFileService", fileUuid + "-" + projectId, "当前类错误码-103-主键重复");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(103);
                throw questionnaireException;
            }
            return TResponseVo.success("新增项目文件完成");
        }
    }

    /**
     * 上传项目文件
     *
     * @param
     */
    @Override
    public TResponseVo uploadFileService(MultipartFile file, String projectId, String fileNameNew, String packageId, String userId, String fileInfo, String fileType) throws Exception {
        String fileUuid = UUID.randomUUID().toString().replaceAll("-", "");

        String fileName = file.getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String fileNameWithoutExtension = fileNameNew;
        // 如果fileNameNew为空 则用原始名称
        if (StringUtils.isBlank(fileNameNew)) {
            fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
        }
//        if(fileName.indexOf(".")>=0) {  // 如果文件有后缀名
//            fileNameWithoutExtension = fileName.substring(0,fileName.lastIndexOf("."));
//        }

        ProjectPojo projectInfoDao = workingTableMapper.getProjectInfoDao(projectId);
        String filePath = projectInfoDao.getFilePath();
        File filePathNew = new File(filePath);
        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        // 查询项目额定空间大小
        long spaceSize = Integer.valueOf(projectInfoDao.getSpaceSize()) * 1024 * 1024;
        // 当前存储情况
        long totalSizeOfFile = new FileSizeUtils().getTotalSizeOfFile(filePath);
        long size = file.getSize();
//        System.out.println("spaceSize: "+spaceSize+"--totalSizeOfFile: "+totalSizeOfFile+"--size: "+size);
        if (totalSizeOfFile + size > spaceSize) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadFileService", fileNameWithoutExtension, "当前类错误码--项目空间不足,上传失败");
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "项目空间不足,上传失败");
        }

        // 检查相同文件是否已存在
        File docxFileNew = new File(filePath + "/" + fileNameWithoutExtension + extension);
        if (!docxFileNew.exists()) {
            try {
                file.transferTo(docxFileNew);
            } catch (IOException e) {
                return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传报错");
            }
            System.out.println("文件已创建成功");
        } else {
            System.out.println("文件已存在");
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "该项目下已存在同名文件");
        }

        ProjectFilePojo projectFilePojo = new ProjectFilePojo();
        projectFilePojo.setFileUuid(fileUuid);
        projectFilePojo.setFileName(fileNameWithoutExtension);
        projectFilePojo.setFileExtension(extension);
        projectFilePojo.setCreateTime(new Date());
        projectFilePojo.setCreateUserId(userId);
        projectFilePojo.setMainPerson(userId);
        projectFilePojo.setPackageId(packageId == null ? "" : packageId);
        projectFilePojo.setFilePath(filePath + "/" + fileNameWithoutExtension + extension);
        projectFilePojo.setFileInfo(fileInfo);
        projectFilePojo.setFileType(fileType);
        projectFilePojo.setProjectId(projectId);
        projectFilePojo.setIsStruct("0");
        projectFilePojo.setIsShow("1");
        workingTableMapper.addProjectFileDao(projectFilePojo);

        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "上传成功");
        ret.put("projectFilePojo", JSON.toJSONString(projectFilePojo));
        return TResponseVo.success(ret);
    }

    /**
     * 项目文件下载
     *
     * @param fileUuid
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo downloadFileService(String fileUuid, HttpServletResponse response) throws Exception {
        ProjectFilePojo projectFileInfoDao = workingTableMapper.getProjectFileInfoDao(fileUuid);
        String filePath = projectFileInfoDao.getFilePath();
        String fileName = projectFileInfoDao.getFileName();
        String fileExtension = projectFileInfoDao.getFileExtension();
        File file = new File(filePath);
        if (file.exists()) {
            //response为HttpServletResponse对象
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + fileExtension);
//            response.setContentType("content-type:octet-stream");
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                OutputStream outputStream = response.getOutputStream();
                int i = bis.read(buffer);
                while (i != -1) {
                    outputStream.write(buffer, 0, i);
                    i = bis.read(buffer);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
//                file.delete();
        } else {
            return TResponseVo.error("下载失败,文件不存在或已删除");
        }
        return TResponseVo.success("下载成功");
    }

    /**
     * 删除项目文件
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo delProjectFileService(String fileUuid) throws Exception {
        ProjectFilePojo projectFileInfoDao = workingTableMapper.getProjectFileInfoDao(fileUuid);
        if (projectFileInfoDao == null) {
            return TResponseVo.success("删除成功");
        }
        // 判断是上传的还是选择的
        String isStruct = (String) projectFileInfoDao.getIsStruct();
        if (isStruct.equals("0")) {
            // 如果是上传的 把原文件也删了
            String filePath = projectFileInfoDao.getFilePath();
            File file = new File(filePath);
            if (file.isFile()) {
                file.delete();
            }
        } else {
            // 如果是选择的 把文件退回给作者
//            workingTableMapper.updateFileStatusDao(fileUuid, "1");
        }
        Integer integer = workingTableMapper.delProjectFileDao(fileUuid);
        if (integer > 0) {
            return TResponseVo.success("删除成功");
        } else {
            return TResponseVo.error("文件不存在或已删除");
        }

    }

    /**
     * 确认文件状态
     *
     * @param fileUuid
     * @param fileStatus
     */
    @Override
    public TResponseVo conformFileStatusService(String fileUuid, String fileStatus) throws Exception {
        if (fileStatus.equals("1")) {
            LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "conformFileStatusService", fileUuid, "当前文件被保留");
        } else if (fileStatus.equals("3")) {
            LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "conformFileStatusService", fileUuid, "当前文件被删除");
        } else {
            return TResponseVo.error("状态枚举值不在定义范围内(1或3),请检查");
        }
        workingTableMapper.updateFileStatusDao(fileUuid, fileStatus);
        return TResponseVo.success("执行成功");
    }

//    /**
//     * 获取项目标段列表(用来选择)
//     *
//     * @param projectId
//     */
//    @Override
//    public TResponseVo getStageIdListService(String projectId, String stageName) throws Exception {
//        List<LabelValuePojo> stageIdListDao = workingTableMapper.getStageIdListDao(projectId, stageName);
//        return TResponseVo.success(stageIdListDao);
//    }

//    /**
//     * 修改文件所属分标
//     *
//     * @param fileUuid
//     * @param stageId
//     */
//    @Override
//    public TResponseVo updateFileStageIdService(String fileUuid, String stageId) throws Exception {
//        ProjectFilePojo projectFileInfoDao = workingTableMapper.getProjectFileInfoDao(fileUuid);
//        projectFileInfoDao.setStageId(stageId);
//        Integer integer = workingTableMapper.updateProjectFileDao(projectFileInfoDao);
//        if (integer > 0) {
//            return TResponseVo.success("更新成功");
//        } else {
//            return TResponseVo.error("文件不存在或已删除");
//        }
//    }


    /**
     * 获取项目文件类型列表
     *
     * @param
     */
    @Override
    public TResponseVo getProjectFileTypeListService(String typeGroupId) throws Exception {
        if (StringUtils.isBlank(typeGroupId)) {
            typeGroupId = null;
        }
        List<HfFileTypeDiPojo> fileTypeDiDao = workingTableMapper.getFileTypeDiDao(typeGroupId);
        ArrayList<LabelValuePojo> retList = new ArrayList<>();
        for (HfFileTypeDiPojo hfFileTypeDiPojo : fileTypeDiDao) {
            LabelValuePojo labelValuePojo = new LabelValuePojo();
            // 去掉"文件-"
            if (hfFileTypeDiPojo.getFileTypeName().contains("-")) {
                String[] split = hfFileTypeDiPojo.getFileTypeName().split("-", 2);  // 分成2份
                labelValuePojo.setLabel(split[1]);
            } else {
                labelValuePojo.setLabel(hfFileTypeDiPojo.getFileTypeName());
            }
            labelValuePojo.setValue(hfFileTypeDiPojo.getFileTypeId());
            retList.add(labelValuePojo);
        }
//        retList.add(new LabelValuePojo("技术规范书", "技术规范书"));
//        retList.add(new LabelValuePojo("招标公告", "招标公告"));
//        retList.add(new LabelValuePojo("招标文件", "招标文件"));
//        retList.add(new LabelValuePojo("评标办法", "评标办法"));
//        retList.add(new LabelValuePojo("合同", "合同"));
        return TResponseVo.success(retList);
    }

    /**
     * 修改项目文件属性
     *
     * @param projectFilePojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateFilePropertyService(ProjectFilePojo projectFilePojo) throws Exception {
        String fileUuid = projectFilePojo.getFileUuid();

        String newName = projectFilePojo.getFileName();  // 新文件名
        // 获取当前文件信息
        ProjectFilePojo projectFileInfoDao = workingTableMapper.getProjectFileInfoDao(fileUuid);
        // 判断是上传的还是从系统选的
        if (projectFileInfoDao.getIsStruct().equals("0")) {
            String filePath = projectFileInfoDao.getFilePath();
            String fileExtension = projectFileInfoDao.getFileExtension();  // 后缀名
            String oldFileName = projectFileInfoDao.getFileName();
            // 获取当前项目信息
            String projectId = projectFileInfoDao.getProjectId();  // 项目id
            ProjectPojo projectInfoDao = workingTableMapper.getProjectInfoDao(projectId);  // 获取项目信息
            String ProjectFilePath = projectInfoDao.getFilePath();  // 获取项目的文件存储路径

            // 检查文件重名
//            try {
//                int j = workingTableMapper.checkFileNameDao(newName, fileUuid, projectId);
//                if (j >= 1) {
//                    LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", newName, "当前类错误码-141-项目文件名已存在");
//                    FileIndexException questionnaireException = new FileIndexException();
//                    questionnaireException.setCode(141);
//                    throw questionnaireException;
//                }
//            } catch (FileIndexException e) {
//                throw e;
//            } catch (Exception e) {
//                e.printStackTrace();
//                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", newName, "当前类错误码-142-检查项目文件名SQL错误");
//                FileIndexException questionnaireException = new FileIndexException();
//                questionnaireException.setCode(142);
//                throw questionnaireException;
//            }

            // 处理当前文件信息
            File file = new File(filePath);
            if (file.exists()) {
                // 新全路径加文件名
                String newFilePath = ProjectFilePath + "/" + newName + fileExtension;
                // 如果名字没变 就无需修改文件系统内的文件名
                if (!oldFileName.equals(newName)) {
                    // 验证新文件路径是否已经存在同名文件
                    File fileNew = new File(newFilePath);
                    if (fileNew.exists()) {
                        LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateFilePropertyService", "projectName", "当前类错误码-1002-本项目存在同名文件,文件属性修改失败");
                        throw new FileIndexException(1002);
                    }
                }
                // 改数据库中的文件名
                projectFilePojo.setFilePath(newFilePath);

                try {
                    workingTableMapper.updateFilePropertyDao(projectFilePojo);
                } catch (Exception e) {
                    LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateFilePropertyService", "projectName", "当前类错误码-1001-修改文件属性sql报错");
                    e.printStackTrace();
                    throw new FileIndexException(1001);
                }
                // 改文件系统中的文件名
                file.renameTo(new File(newFilePath)); //改名
                return TResponseVo.success("更新成功");
            } else {
                return TResponseVo.error("文件不存在或已删除");
            }
        } else {
            // 获取当前项目信息
            String projectId = projectFileInfoDao.getProjectId();  // 项目id
            // 检查文件重名
//            try {
//                int j = workingTableMapper.checkFileNameDao(newName, fileUuid, projectId);
//                if (j >= 1) {
//                    LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", newName, "当前类错误码-141-项目文件名已存在");
//                    FileIndexException questionnaireException = new FileIndexException();
//                    questionnaireException.setCode(141);
//                    throw questionnaireException;
//                }
//            } catch (FileIndexException e) {
//                throw e;
//            } catch (Exception e) {
//                e.printStackTrace();
//                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateStageService", newName, "当前类错误码-142-检查项目文件名SQL错误");
//                FileIndexException questionnaireException = new FileIndexException();
//                questionnaireException.setCode(142);
//                throw questionnaireException;
//            }

            try {
                workingTableMapper.updateFilePropertyDao(projectFilePojo);
            } catch (Exception e) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateFilePropertyService", "projectName", "当前类错误码-1001-修改文件属性sql报错");
                e.printStackTrace();
                throw new FileIndexException(1001);
            }
            return TResponseVo.success("更新成功");
        }
    }

    /**
     * 获取文件列表
     *
     * @param pagePojo
     * @param projectFilePojo
     */
    @Override
    public TResponseVo getProjectFileListService(PagePojo pagePojo, ProjectFilePojo projectFilePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        // 搜索范围
        String projectId = projectFilePojo.getProjectId();
        String packageId = projectFilePojo.getPackageId();
        if (StringUtils.isBlank(packageId)) {
            packageId = null;
        }

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<ProjectFilePojo> projectFileListDao = workingTableMapper.getProjectFileListDao(paramNameLike, null, projectId, packageId);
        for (ProjectFilePojo filePojo : projectFileListDao) {
            if (filePojo.getIsStruct().equals("1")) {
                filePojo.setIsStruct(true);
                filePojo.setUrl("");
            } else {
                filePojo.setIsStruct(false);
            }
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(projectFileListDao);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(projectFileListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", projectFileListDao.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 获取存储空间信息
     *
     * @param projectId
     */
    @Override
    public TResponseVo getMemoryInfoService(String projectId) throws Exception {
        ProjectPojo projectInfoDao = workingTableMapper.getProjectInfoDao(projectId);
        String spaceSize = projectInfoDao.getSpaceSize();
        String filePath = projectInfoDao.getFilePath();
        long totalSizeOfFile = new FileSizeUtils().getTotalSizeOfFile(filePath);
        String s1 = String.valueOf(totalSizeOfFile);
        String s2 = String.valueOf(JsonKeyUtils.decimalKeepN(Double.valueOf(totalSizeOfFile) / 1024 / 1024, 2));
        HashMap<String, String> ret = new HashMap<>();
        ret.put("Byte", s1);
        ret.put("usedSize", s2 + " MB");
        ret.put("totalSize", spaceSize + " MB");
        return TResponseVo.success(ret);
    }

    /**
     * 获取收件箱文件清单
     *
     * @param projectId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getInboxFileListService(String projectId, String batchId, String statusId) throws Exception {
        List<HfProjectInboxPojo> inboxFileListDao = workingTableMapper.getInboxFileListDao(projectId, batchId, statusId);
        for (HfProjectInboxPojo hfProjectInboxPojo : inboxFileListDao) {
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
        return TResponseVo.success(inboxFileListDao);
    }

    /**
     * 修改收件箱文件状态
     *
     * @param fileUuid
     * @param statusId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateInboxFileService(String fileUuid, String statusId, String userId) throws Exception {
        HfProjectInboxPojo projectInboxFileInfoDao = workingTableMapper.getProjectInboxFileInfoDao(fileUuid);
        String projectId = projectInboxFileInfoDao.getProjectId();  // 获取项目id
        String batchId = projectInboxFileInfoDao.getBatchId();  // 获取项目id
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        workingTableMapper.updateInboxFileDao(fileUuid, statusId);
        if (statusId.equals("1")) {
//            已投递
        } else if (statusId.equals("2")) {
//            已接收
            if (projectId != null) {
                workingTableMapper.updateFileStatusDao(fileUuid, "82");
                // 新增项目文件
                ProjectFilePojo projectFilePojo1 = new ProjectFilePojo();
                projectFilePojo1.setFileUuid(fileUuid);
                projectFilePojo1.setFileName(docAllInfoDao.getFileName());
                projectFilePojo1.setFileExtension(".docx");
                projectFilePojo1.setCreateTime(new Date());
                projectFilePojo1.setCreateUserId(docAllInfoDao.getCreateUserId());
                projectFilePojo1.setMainPerson(userId);
                projectFilePojo1.setPackageId(null);
                projectFilePojo1.setFilePath(null);
                projectFilePojo1.setFileInfo(docAllInfoDao.getVersionInfo());
                projectFilePojo1.setFileType(docAllInfoDao.getFileTypeId());
                projectFilePojo1.setProjectId(projectId);
                projectFilePojo1.setIsStruct("1");
                projectFilePojo1.setIsShow("1");
                try {
                    workingTableMapper.addProjectFileDao(projectFilePojo1);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addProjectFileService", fileUuid + "-" + projectId, "当前类错误码-103-主键重复");
                    FileIndexException questionnaireException = new FileIndexException();
                    questionnaireException.setCode(103);
                    throw questionnaireException;
                }
                // 找到相关文件 置为可见
                List<String> fileUuidList = workingTableMapper.getFileInfoByMainFileUuid2Dao(fileUuid, docAllInfoDao.getFileVersionId());
                String fileUuidListStr = new JsonKeyUtils().listToString(fileUuidList, "','");
                workingTableMapper.updateFileStatusDao2(fileUuidListStr, "82");
                workingTableMapper.updateProjectShowDao(fileUuidListStr, "1");
            } else if (batchId != null) {
                workingTableMapper.updateFileStatusDao(fileUuid, "82");
                // 新增项目文件
                BatchFilePojo batchFilePojo1 = new BatchFilePojo();
                batchFilePojo1.setFileUuid(fileUuid);
                batchFilePojo1.setFileName(docAllInfoDao.getFileName());
                batchFilePojo1.setFileExtension(".docx");
                batchFilePojo1.setCreateTime(new Date());
                batchFilePojo1.setCreateUserId(docAllInfoDao.getCreateUserId());
                batchFilePojo1.setMainPerson(userId);
                batchFilePojo1.setPackageId(null);
                batchFilePojo1.setFilePath(null);
                batchFilePojo1.setFileInfo(docAllInfoDao.getVersionInfo());
                batchFilePojo1.setFileType(docAllInfoDao.getFileTypeId());
                batchFilePojo1.setBatchId(batchId);
                batchFilePojo1.setIsStruct("1");
                batchFilePojo1.setIsShow("1");
                try {
                    workingTableMapper.addBatchFileDao(batchFilePojo1);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "addProjectFileService", fileUuid + "-" + projectId, "当前类错误码-103-主键重复");
                    FileIndexException questionnaireException = new FileIndexException();
                    questionnaireException.setCode(103);
                    throw questionnaireException;
                }
                // 找到相关文件 置为可见
                List<String> fileUuidList = workingTableMapper.getFileInfoByMainFileUuid2Dao(fileUuid, docAllInfoDao.getFileVersionId());
                String fileUuidListStr = new JsonKeyUtils().listToString(fileUuidList, "','");
                workingTableMapper.updateFileStatusDao2(fileUuidListStr, "82");
                workingTableMapper.updateBatchShowDao(fileUuidListStr, "1");
            }
        } else if (statusId.equals("3")) {
//            已忽略
            workingTableMapper.updateFileStatusDao(fileUuid, "83");
        } else if (statusId.equals("4")) {
//            已拒绝
            workingTableMapper.updateFileStatusDao(fileUuid, "84");
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("info", "操作完成");
        return TResponseVo.success(map);
    }

    /**
     * 获取项目状态
     *
     * @param projectId
     */
    @Override
    public TResponseVo getProjectStatusService(String projectId) throws Exception {
        return null;
    }

    /**
     * 获取文件预览
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo getPreviewService(String fileUuid) throws Exception {
        return null;
    }


    /**
     * 获取批次状态
     *
     * @param batchId
     */
    @Override
    public TResponseVo getBatchStatusService(String batchId) throws Exception {
        return null;
    }

//    /**
//     * 批次中新纳入标段(新建分包)
//     *
//     * @param stageId
//     * @param batchId
//     */
//    @Override
//    public TResponseVo addStageToBatchService(String stageId, String batchId) throws Exception {
//        workingTableMapper.updateStageBatchIdDao(stageId, batchId);
//        return TResponseVo.success("新纳入标段成功");
//    }
//
//    /**
//     * 从当前批次删除
//     *
//     * @param stageId
//     */
//    @Override
//    public TResponseVo delStageFromBatchService(String stageId) throws Exception {
//        workingTableMapper.updateStageBatchIdDao(stageId, null);
//        return TResponseVo.success("新纳入标段成功");
//    }
//
//    /**
//     * 获取当前批次内容(分包列表)
//     *
//     * @param batchId
//     */
//    @Override
//    public TResponseVo getStageListService(String batchId) throws Exception {
//        List<StagePojo> batchStageListDao = workingTableMapper.getBatchStageListDao(batchId);
//        return TResponseVo.success(batchStageListDao);
//    }

    /**
     * 添加关联文件
     *
     * @param batchId
     */
    @Override
    public TResponseVo addStageFileService(String batchId) throws Exception {
        return null;
    }

    /**
     * 删除关联文件
     *
     * @param batchId
     */
    @Override
    public TResponseVo delStageFileService(String batchId) throws Exception {
        return null;
    }

    /**
     * 获取关联文件列表
     *
     * @param batchId
     */
    @Override
    public TResponseVo getStageFileListService(String batchId) throws Exception {
        return null;
    }

    /**
     * 我的代办
     *
     * @param userId
     */
    @Override
    public TResponseVo getMyTODOListService(PagePojo pagePojo, String userId) throws Exception {
        return null;
    }

    /**
     * 我的草稿
     *
     * @param userId
     */
    @Override
    public TResponseVo getMyDraftListService(PagePojo pagePojo, String userId) throws Exception {
        return null;
    }

    /**
     * 我的文档
     *
     * @param userId
     */
    @Override
    public TResponseVo getMyDocumentListService(PagePojo pagePojo, String userId) throws Exception {
        List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
        String fileTypeId = String.valueOf(pagePojo.getFileTypeId());  // 模板分类筛选
        if (pagePojo.getFileTypeId() == null || StringUtils.isBlank(fileTypeId)) {
            fileTypeId = null;
        }
        List<String> fileLabelList = pagePojo.getFileLabelList();  // 标签筛选 (只显示全命中的文档)

        DocUserPojo userInfoDao = workingTableMapper.getUserInfoDao(userId);
        String companyId = userInfoDao.getCompanyId();
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        String type = pagePojo.getType();  // 文件or模板
        String status = pagePojo.getStatus();  // 文档状态
        List<String> createTime = (List) pagePojo.getCreateTime();
        List<String> updateTime = (List) pagePojo.getUpdateTime();
        String createTime1 = null;
        String createTime2 = null;
        String updateTime1 = null;
        String updateTime2 = null;
        if (createTime != null && createTime.size() == 2) {
            createTime1 = createTime.get(0);
            createTime2 = createTime.get(1);
        }
        if (updateTime != null && updateTime.size() == 2) {
            updateTime1 = updateTime.get(0);
            updateTime2 = updateTime.get(1);
        }

        List<DocFileIndexPojo> docParamsPojos = workingTableMapper.getMyDocumentListDao(paramNameLike, null, userId, companyId, fileTypeId, createTime1, createTime2, updateTime1, updateTime2);
        List<DocFileIndexPojo> docParamsPojosTmp = new ArrayList<DocFileIndexPojo>();
        docParamsPojosTmp.addAll(docParamsPojos);
        for (DocFileIndexPojo docParamsPojo : docParamsPojosTmp) {  // 根据标签过滤数据
            // 过滤文档大类型
            String fileClass = docParamsPojo.getFileClass();
            if (type != null) {
                if ("WJ".equals(type)) {
                    if (!fileClass.equals("2") && !fileClass.equals("3")) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                }
                if ("MB".equals(type)) {
                    if (!fileClass.equals("1")) {
                        docParamsPojos.remove(docParamsPojo);
                        continue;
                    }
                }
            }
            // 过滤文档状态
            String showStatus = docParamsPojo.getShowStatus();
            if (status != null) {
                if (!showStatus.equals(status)) {
                    docParamsPojos.remove(docParamsPojo);
                    continue;
                }
            }

            String fileLabelList2 = docParamsPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            if (!new JsonKeyUtils().containsAll(strings, fileLabelList)) {  // 如果文档的标签完全覆盖'标签筛选'所选内容
                docParamsPojos.remove(docParamsPojo);
            }
        }

        for (DocFileIndexPojo docParamsPojo : docParamsPojos) {  // 设置文件类型名称
            for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                    docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                    break;
                }
            }
        }

        // 标签数据预搜索
        List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);
        for (DocFileIndexPojo docParamsPojo : docParamsPojos) {  // 根据标签过滤数据
            String fileLabelList2 = docParamsPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            for (String labelId : strings) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            docParamsPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));
        }


        for (DocFileIndexPojo docParamsPojo : docParamsPojos) {  // 最近更新时间
            Date time = docParamsPojo.getUpdateTime();
            String ago = new TimeFormatUtils().getAgo(time);
            docParamsPojo.setTimeFormat(ago);
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
     * 我的文档新建
     *
     * @param docFileIndexPojo
     */
    @Override
    public TResponseVo newMyDocumentService(DocFileIndexPojo docFileIndexPojo, String userId) throws Exception {
        String fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        String fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
        docFileIndexPojo.setFileUuid(fileUuid);
        docFileIndexPojo.setFileVersionId(fileVersionId);
        docFileIndexPojo.setCreateTime(new Date());
        docFileIndexPojo.setCreateUserId(userId);
        docFileIndexPojo.setUpdateUserId(userId);
        docFileIndexPojo.setIsRootFile("1");
        String fileTypeId = docFileIndexPojo.getFileTypeId();
//        String fileTypeName = "";
//        if (fileTypeId == null) {
//            fileTypeName = "未知"; // 未知
//        } else if (fileTypeId.equals("B")) {
//            fileTypeName = "标准文件";
//        } else if (fileTypeId.equals("F")) {
//            fileTypeName = "范本";
//        } else if (fileTypeId.equals("M")) {
//            fileTypeName = "模板";
//        } else {
//            fileTypeName = "未知"; // 未知
//        }
//        docFileIndexPojo.setFileTypeId(fileTypeId == null ? "W" : fileTypeId);
//        docFileIndexPojo.setFileTypeName(fileTypeName);
        docFileIndexPojo.setFileTypeId(fileTypeId);
        docFileIndexPojo.setFileStatus("1");
        docFileIndexPojo.setFileParentId(null);
        docFileIndexPojo.setFileClass("1");
        workingTableMapper.newMyDocumentDao(docFileIndexPojo);
        HashMap<String, String> ret = new HashMap<>();
        ret.put("fileUuid", fileUuid);
        ret.put("fileVersionId", fileVersionId);
        ret.put("info", "新建成功");
        return TResponseVo.success(ret);
    }

    /**
     * 我的文档删除
     *
     * @param fileUuid
     * @param fileVersionId
     */
    @Override
    public TResponseVo delMyDocumentService(String fileUuid, String fileVersionId) throws Exception {
        workingTableMapper.delMyDocumentDao(fileUuid, fileVersionId);
        return TResponseVo.success("删除成功");
    }


    /**
     * 招标文件
     */
    /**
     * 我的项目列表
     *
     * @param userId
     */
    @Override
    public TResponseVo getMyProjectListService(String userId) throws Exception {
        List<LabelValuePojo> myProjectListDao = workingTableMapper.getMyProjectListDao();
        return TResponseVo.success(myProjectListDao);
    }

    /**
     * 创建招标文件
     *
     * @param docFileIndexPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> newBidDocumentService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        // 验证模板是否含有投标模板 有的话就继续 没有就提示报错
        String fileUuid = docFileIndexPojo.getFileUuid();

        if ("WJ-ZBWJ".equals(docFileIndexPojo.getFileTypeId())) {  // 招标文件需要判断是否包含招标文件模板
            List<LabelValuePojo> tendDocumentInfoList = workingTableMapper.getTendDocumentInfoDao(fileUuid);
            if (tendDocumentInfoList.size() == 0) {
//                return TResponseVo.error(ResponseEnum.ERROR, "此模板中未创建投标文件模板,无法创建招标文件");
                throw new WorkTableException("此模板中未创建投标文件模板,无法创建招标文件");
            }
        }
        // 验证入参
        String projectName = docFileIndexPojo.getProjectName();
        String zbrCompanyName = docFileIndexPojo.getZbrCompanyName();
        String jgCompanyName = docFileIndexPojo.getJgCompanyName();
        String fileName = docFileIndexPojo.getFileName();
        if (StringUtils.isBlank(fileName)) {
            throw new WorkTableException("必填参数不能为空");
        }

        //        文件标签
        List<String> fileLabelIds = docFileIndexPojo.getFileLabelIds();
        docFileIndexPojo.setFileLabelList(new JsonKeyUtils().listToString(fileLabelIds, ","));


        // 获取当前最新版本
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
//        String fileVersionId = docAllInfoDao.getFileVersionId();
        // 获取当前文库的发行版本
//        DocFileIndexPojo docAllInfoDao = workingTableMapper.getOnlineVersionInfoDao(fileUuid);
//        String fileVersionId = docAllInfoDao.getFileVersionId();  // 发行版
        String fileVersionId = docFileIndexPojo.getFileVersionId();

        String ancestorsFileUuid = docAllInfoDao.getAncestorsFileUuid();  // 源头文件id

        // 选择模板中的章节
        List<String> outlineListMap = docFileIndexPojo.getOutlineListMap();  // 前端选好的章节
        List<OutLinePojo> outlineListDao = workingTableMapper.getOutlineListDao1(fileUuid, fileVersionId); // 库中的全部章节
        // 先判断有多少组可替换章节
        List<String> outlineReplaceGroupIdList = new ArrayList<>();
        for (OutLinePojo outLinePojo : outlineListDao) {
            if (outLinePojo.getOutlineReplaceGroupId() != null && !outlineReplaceGroupIdList.contains(outLinePojo.getOutlineReplaceGroupId())) {
                outlineReplaceGroupIdList.add(outLinePojo.getOutlineReplaceGroupId());
            }
        }
        List<OutLinePojo> outlineListDaoTmp = new ArrayList<>();
        List<OutLinePojo> outlineListDaoTmp2 = new ArrayList<>();
        outlineListDaoTmp.addAll(outlineListDao);
        outlineListDaoTmp2.addAll(outlineListDao);

        // 记录模板中的章节id
        for (OutLinePojo outLinePojo : outlineListDao) {
            if (!outlineListMap.contains(outLinePojo.getOutlineId())) {
                outlineListDaoTmp.remove(outLinePojo);

                // 如果有子节点 一并删除
                for (OutLinePojo linePojo : outlineListDaoTmp2) {
                    if (linePojo.getOutlineFatherId().equals(outLinePojo.getOutlineId())) {
                        outlineListDaoTmp.remove(linePojo);

                        for (OutLinePojo linePojo2 : outlineListDaoTmp2) {
                            if (linePojo2.getOutlineFatherId().equals(outLinePojo.getOutlineId())) {
                                outlineListDaoTmp.remove(linePojo2);

                                for (OutLinePojo linePojo3 : outlineListDaoTmp2) {
                                    if (linePojo3.getOutlineFatherId().equals(outLinePojo.getOutlineId())) {
                                        outlineListDaoTmp.remove(linePojo3);

                                        for (OutLinePojo linePojo4 : outlineListDaoTmp2) {
                                            if (linePojo4.getOutlineFatherId().equals(outLinePojo.getOutlineId())) {
                                                outlineListDaoTmp.remove(linePojo4);

                                                for (OutLinePojo linePojo5 : outlineListDaoTmp2) {
                                                    if (linePojo5.getOutlineFatherId().equals(outLinePojo.getOutlineId())) {
                                                        outlineListDaoTmp.remove(linePojo5);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        List<String> outlineIdList = new ArrayList<>();
        for (OutLinePojo outLinePojo : outlineListDaoTmp) {
            outlineIdList.add(outLinePojo.getOutlineId());
        }
        String s = JsonKeyUtils.listToString(outlineIdList, "','");
        docFileIndexPojo.setOutlineIdListStr(s);

        // 创建招标文件索引信息
        DocFileIndexPojo bidFileIndex = new DocFileIndexPojo();
        String bidFileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        String bidFileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
        String userId = docFileIndexPojo.getCreateUserId();
        // 主索引信息
        bidFileIndex.setFileUuid(bidFileUuid);
        bidFileIndex.setFileVersionId(bidFileVersionId);
        bidFileIndex.setFileName(fileName);  // 招标文件名
        bidFileIndex.setFileClass("2");  // 文件类型:招标文件
        bidFileIndex.setFileParentId(fileUuid);  // 父文件是被选中的模板fileUuid
        bidFileIndex.setCreateTime(new Date());
        bidFileIndex.setCreateUserId(userId);  // 设置创建人
        bidFileIndex.setUpdateUserId(userId);
        bidFileIndex.setAncestorsFileUuid(ancestorsFileUuid);  // 设置源头id
        bidFileIndex.setFileTypeId(docFileIndexPojo.getFileTypeId());  // 设置文件类型
//        bidFileIndex.setFileTypeName("招标文件");
        bidFileIndex.setIsRootFile("0");  // 不是根文件
        bidFileIndex.setFileStatus("1");  // 正常状态
        // 保留的outlineId
        bidFileIndex.setOutlineIdListStr(s);
        // 记录原文档id
        bidFileIndex.setOldFileUuid(fileUuid);
        bidFileIndex.setOldFileVersionId(fileVersionId);
        // 附表信息
        bidFileIndex.setProjectName(projectName);  // 项目名称
        bidFileIndex.setZbrCompanyName(zbrCompanyName);  // 招标人
        bidFileIndex.setJgCompanyName(jgCompanyName);  // 招标机构
//        bidFileIndex.setBiddingType(biddingTypeStr);
//        bidFileIndex.setBiddingStyle(biddingStyleStr);
//        bidFileIndex.setJudgmentMethod(judgmentMethodStr);
        bidFileIndex.setFileLabelList(docFileIndexPojo.getFileLabelList());
        bidFileIndex.setFolderId(docFileIndexPojo.getFolderId());
        try {
            int j = 0;
            int i = 0;
            // 根据模板创建招标文件索引
            workingTableMapper.newBidDocumentIndexDao(bidFileIndex);  // 索引信息
            workingTableMapper.newBidDocumentInfoDao(bidFileIndex);  // 附表信息

            // 创建版本
            workingTableMapper.newBidDocumentVersionDao(bidFileIndex);  //

            // 创建大纲
            // 根据最终确认的模板大纲 保留大纲信息
            for (OutLinePojo outLinePojo : outlineListDaoTmp) {
                if (outLinePojo.getOutlineReplaceGroupId() != null) {
                    // 清洗掉分组信息
                    outLinePojo.setOutlineReplaceGroupId(null);
                }
            }
            String sql = "";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = formatter.format(new Date());
            for (OutLinePojo outLinePojo : outlineListDaoTmp) {
                j++;
                // 重新排序
                outLinePojo.setOutlineOrder(String.valueOf(j));
                String outlineReplaceGroupId = null;
                String color = null;
                sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + bidFileUuid + "\",\"" + bidFileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
            }
            if (sql.length() != 0) {  // 防止空文档无段落
                sql = sql.substring(0, sql.length() - 1);
//                System.out.println("sql : " + sql);
                workingTableMapper.newBidDocumentOutlineDao(sql);  //
            }
            // 创建内容
            List<ContentPojo> deriveFileContentDao = workingTableMapper.getBidDocumentContentDao(bidFileIndex);
            sql = "";
            for (ContentPojo contentPojo : deriveFileContentDao) {
                String contentText = contentPojo.getContentText();
//                Document doc = Jsoup.parse(contentText);
//                Elements elements = doc.select("mark");
//                Elements elementsTmp = new Elements();
//                elementsTmp.addAll(elements);
//                for (Element element : elementsTmp) {
//                    element.before(element.html());
//                    element.remove();  // 去除mark标签
//                }
//                contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                contentPojo.setContentText(contentText);
                sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + bidFileUuid + "\",\"" + bidFileVersionId + "\",\"" + dateString + "\"),";
            }
            if (sql.length() != 0) {  // 防止空文档无段落
                sql = sql.substring(0, sql.length() - 1);
                workingTableMapper.newBidDocumentContentDao(sql);  //
            }
            // 创建参数
            workingTableMapper.newBidDocumentParamDao(bidFileIndex);  //

            // 创建标注
            workingTableMapper.newBidDocumentTagDao(bidFileIndex);  //

            // 创建书签
//            workingTableMapper.newBidDocumentBookmarkDao(bidFileIndex);  //

            // 创建数据管理(4张)
            workingTableMapper.newBidDocumentContentDmDbDao(bidFileIndex);
            workingTableMapper.newBidDocumentContentDmTableDao(bidFileIndex);
            workingTableMapper.newBidDocumentContentDmColumnsDao(bidFileIndex);
            String hashMod1 = new HashUtils().getHashMod(bidFileIndex.getFileUuid() + bidFileIndex.getFileVersionId());
            String hashMod2 = new HashUtils().getHashMod(bidFileIndex.getOldFileUuid() + bidFileIndex.getOldFileVersionId());
            System.out.println("表编号为: " + hashMod1);
            splitTableMapper.newBidDocumentContentDmDataDao(bidFileIndex, hashMod1, hashMod2, dmContentDataDatabase);

            // 创建数据表(如果有)
            workingTableMapper.newBidDocumentContentDbDao(bidFileIndex);

            // 评审工具(如果有)
            HfContentAssessPojo contentAssessInfoDao = fileOperationMapper.getContentAssessInfoDao(bidFileIndex.getOldFileUuid());
            if (contentAssessInfoDao != null) {
                // 将评审工具也复制放到新文件里
                fileOperationMapper.addDeriveFileAssessDao(bidFileIndex.getOldFileUuid(), bidFileIndex.getFileUuid(), bidFileIndex.getCreateUserId());
                fileOperationMapper.addDeriveFileAssessElementDao(bidFileIndex.getOldFileUuid(), bidFileIndex.getFileUuid(), bidFileIndex.getCreateUserId());
            }

            // 创建附属文件
            // 查找附属
            List<DocFileIndexPojo> allSubsidiaryFileListDao = workingTableMapper.getAllSubsidiaryBidFileListDao(bidFileIndex);
            for (DocFileIndexPojo fileIndexPojo : allSubsidiaryFileListDao) {
                // 获取父文件的fileUuid fileVersionId
                fileUuid = fileIndexPojo.getFileUuid();
                fileVersionId = fileIndexPojo.getFileVersionId();
                fileIndexPojo.setFileParentId(fileUuid);
                fileIndexPojo.setOldFileVersionId(fileVersionId);
                // 把主文件的fileUuid写到附属文件的mainFileUuid字段中
                fileIndexPojo.setMainFileUuid(bidFileIndex.getFileUuid());
                fileIndexPojo.setMainFileVersionId(bidFileIndex.getFileVersionId());
                userId = bidFileIndex.getCreateUserId();
                fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                fileIndexPojo.setFileUuid(fileUuid);
                fileIndexPojo.setFileVersionId(fileVersionId);

                fileIndexPojo.setCreateTime(new Date());
                fileIndexPojo.setFileTypeId(docFileIndexPojo.getFileTypeId());
                fileIndexPojo.setFileClass("2");
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
                List<OutLinePojo> getDeriveFileOutlineDao = fileOperationMapper.getDeriveFileOutlineDao(fileIndexPojo);
                // 设置groupId替换用的键值
                Map groupIdMap = new HashMap<>();
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
                if (sql.length() != 0) {  // 防止空文档无段落
                    sql = sql.substring(0, sql.length() - 1);
//                    System.out.println("sql : " + sql);
                    fileOperationMapper.addDeriveFileOutlineDao(sql);
                }
                // 创建内容
                deriveFileContentDao = fileOperationMapper.getDeriveFileContentDao(fileIndexPojo);
                sql = "";
                for (ContentPojo contentPojo : deriveFileContentDao) {
                    String contentText = contentPojo.getContentText();
//                    Document doc = Jsoup.parse(contentText);
//                    Elements elements = doc.select("mark");
//                    Elements elementsTmp = new Elements();
//                    elementsTmp.addAll(elements);
//                    for (Element element : elementsTmp) {
//                        element.before(element.html());
//                        element.remove();  // 去除mark标签
//                    }
//                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                    contentPojo.setContentText(contentText);
                    sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                }
                if (sql.length() != 0) {  // 防止空文档无段落
                    sql = sql.substring(0, sql.length() - 1);
                    fileOperationMapper.addDeriveFileContentDao(sql);
                }
                // 创建参数
                fileOperationMapper.addDeriveContentParamDao(fileIndexPojo);

                // 创建标注
                fileOperationMapper.addDeriveContentTagDao(fileIndexPojo);

                // 创建书签
//                fileOperationMapper.addDeriveContentBookmarkDao(fileIndexPojo);

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
        ret.put("fileUuid", bidFileUuid);
        ret.put("fileVersionId", bidFileVersionId);
        ret.put("info", "创建文件成功");
        return ret;
    }

//    /**
//     * 将招标文件投递至项目中
//     *
//     * @param fileUuid
//     */
//    @Override
//    public TResponseVo sendToProjectService(String fileUuid) throws Exception {
//        DocFileIndexPojo docFileIndexPojo = workingTableMapper.getBidDocumentInfoDao(fileUuid);
//        if (docFileIndexPojo == null) {
//            return TResponseVo.error("文件不存在,请检查文件索引id是否正确");
//        }
//        String projectId = docFileIndexPojo.getProjectId();
//        if (StringUtils.isBlank(projectId)) {
//            return TResponseVo.error("文件创建阶段未指定项目");
//        }
//        Integer integer = workingTableMapper.checkFileExistsDao(projectId, fileUuid);
//        if (integer >= 1) {
//            return TResponseVo.error("文件已经投递到指定项目中,无需重复投递");
//        }
//
//        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
//        String fileName = docAllInfoDao.getFileName();
//        String createUserId = docAllInfoDao.getCreateUserId();
//        ProjectFilePojo projectFilePojo1 = new ProjectFilePojo();
//        projectFilePojo1.setFileUuid(fileUuid);
//        projectFilePojo1.setFileName(fileName);
//        projectFilePojo1.setFileExtension(".docx");
//        projectFilePojo1.setCreateTime(new Date());
//        projectFilePojo1.setCreateUserId(createUserId);
//        projectFilePojo1.setMainPerson(createUserId);
//        projectFilePojo1.setPackageId(null);
//        projectFilePojo1.setFilePath(null);
//        projectFilePojo1.setFileInfo(null);
//        projectFilePojo1.setFileType(null);
//        projectFilePojo1.setProjectId(projectId);
//        projectFilePojo1.setIsStruct("1");
//        workingTableMapper.addProjectFileDao(projectFilePojo1);
//        workingTableMapper.updateFileStatusDao(fileUuid, "6");
//        return TResponseVo.success("投递完成");
//    }

    /**
     * 删除招标文件
     *
     * @param docFileIndexPojo
     */
    @Override
    public TResponseVo delBidDocumentService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        return null;
    }

    /**
     * 获取招标文件信息
     *
     * @param docFileIndexPojo
     */
    @Override
    public TResponseVo getBidDocumentInfoService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        return null;
    }

    /**
     * 获取文件大纲信息
     *
     * @param
     */
    @Override
    public TResponseVo getOutlineIdListService(String fileUuid, String fileVersionId) throws Exception {
        fileVersionId = fileOperationMapper.getLastVersionIdDao(fileUuid);
//        DocFileIndexPojo onlineVersionInfoDao = workingTableMapper.getOnlineVersionInfoDao(fileUuid);
//        fileVersionId = onlineVersionInfoDao.getFileVersionId();
        if (1 == 0 && redisUtils.hasKey("getOutlineList" + fileUuid + fileVersionId) && String.valueOf(redisUtils.get("getOutlineList" + fileUuid + fileVersionId)).length() > 5) {
            String s = String.valueOf(redisUtils.get("getOutlineList" + fileUuid + fileVersionId));
            // 使用过就重置失效时间
            redisUtils.expire("getOutlineList" + fileUuid + fileVersionId, 7200);
            List<OutLinePojo> outlineListDao = JSON.parseObject(s, List.class);

            return TResponseVo.success(outlineListDao);
        } else {
//            List<OutLinePojo> outlineListDao = workingTableMapper.getOutlineListDao(fileUuid, fileVersionId);
            List<OutLinePojo> outlineListDao1 = workingTableMapper.getOutlineListDao1(fileUuid, fileVersionId);  // 记录全部
            // 去除多余的返回信息
            for (OutLinePojo outLinePojo : outlineListDao1) {
                outLinePojo.setCreateUserId(null);
                outLinePojo.setUpdateUserId(null);
                outLinePojo.setCreateTime(null);
                outLinePojo.setUpdateTime(null);
                outLinePojo.setColor(null);
                outLinePojo.setOutlineProperty(null);
                outLinePojo.setOutlineLevel(null);
                outLinePojo.setOutlineOrder(null);
            }
            List<OutLinePojo> outLineListDao2 = new ArrayList<>();  // 记录剩余的 当剩余为0时结束
            List<OutLinePojo> outLineListDaoTmp = new ArrayList<>();  // 记录每层
            List<OutLinePojo> outLineListResult = new ArrayList<>(); // 记录结果
            outLineListDao2.addAll(outlineListDao1);
            while (outLineListDao2.size() != 0) {
                if (outLineListResult.size() == 0) {
                    // 第一次
                    for (OutLinePojo outLinePojo : outlineListDao1) {
                        if (outLinePojo.getChildren() == null) {
                            outLinePojo.setChildren(new ArrayList<>());
                        }
                        if (outLinePojo.getOutlineFatherId() == null || outLinePojo.getOutlineFatherId().equals("") || outLinePojo.getOutlineFatherId().equals("null")) {
                            outLineListResult.add(outLinePojo);
                            outLineListDaoTmp.add(outLinePojo);
                            outLineListDao2.remove(outLinePojo);
                        }
                    }
                    outlineListDao1 = new ArrayList<>();
                    outlineListDao1.addAll(outLineListDao2);
                } else {
                    List<OutLinePojo> outLineListDaoTmp1 = new ArrayList<>();
                    for (OutLinePojo outLinePojo : outlineListDao1) {
                        for (OutLinePojo linePojo : outLineListDaoTmp) {
                            if (linePojo.getOutlineId().equals(outLinePojo.getOutlineFatherId())) {
                                linePojo.getChildren().add(outLinePojo);
                                outLineListDaoTmp1.add(outLinePojo);
                                outLineListDao2.remove(outLinePojo);
                            }
                        }
                    }
                    outLineListDaoTmp = new ArrayList<>();  // 记录每层
                    outLineListDaoTmp.addAll(outLineListDaoTmp1);
                    outlineListDao1 = new ArrayList<>();
                    outlineListDao1.addAll(outLineListDao2);
                }
            }
            return TResponseVo.success(outLineListResult);
        }
    }

    /**
     * 获取采购类别,采购方式,评标办法
     */
    @Override
    public TResponseVo getLabelInfoListService(String fileTypeId) throws Exception {
        List<DocLabelPojo> labelInfoListDao = workingTableMapper.getLabelInfoListDao();

        if (fileTypeId != null) {
            List<DocLabelPojo> labelInfoListDaoTmp = new ArrayList<>();
            labelInfoListDaoTmp.addAll(labelInfoListDao);
            // 筛选出fileTypeId下的标签
            for (DocLabelPojo docLabelPojo : labelInfoListDaoTmp) {
                List<String> list = JSON.parseObject(String.valueOf(docLabelPojo.getFileTypeId()), List.class);
                if (list == null || !list.contains(fileTypeId)) {
                    labelInfoListDao.remove(docLabelPojo);
                }
            }
        }

        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        ArrayList<String> groupIdList = new ArrayList<>();
        for (DocLabelPojo docLabelPojo : labelInfoListDao) {
            if (docLabelPojo.getLabelGroupId() == null) {
                docLabelPojo.setLabelGroupId("weifenzu");
                docLabelPojo.setLabelGroupName("未分组标签");
            }
            if (groupIdList.contains(docLabelPojo.getLabelGroupId())) {
                for (HashMap<String, Object> stringObjectHashMap : list) {
                    if (docLabelPojo.getLabelGroupId() != null && stringObjectHashMap.get("groupId") != null && stringObjectHashMap.get("groupId").equals(docLabelPojo.getLabelGroupId())) {
                        LabelValuePojo labelValuePojo = new LabelValuePojo();
                        labelValuePojo.setLabel(docLabelPojo.getLabelContent());
                        labelValuePojo.setValue(docLabelPojo.getLabelUuid());
                        ((List) stringObjectHashMap.get("options")).add(labelValuePojo);
                        break;
                    }
                }
            } else {
                LabelValuePojo labelValuePojo = new LabelValuePojo();
                labelValuePojo.setLabel(docLabelPojo.getLabelContent());
                labelValuePojo.setValue(docLabelPojo.getLabelUuid());
                List<LabelValuePojo> labelValuePojos = new ArrayList<>();
                labelValuePojos.add(labelValuePojo);
                HashMap<String, Object> stringListHashMap = new HashMap<>();
                stringListHashMap.put("options", labelValuePojos);
                stringListHashMap.put("groupId", docLabelPojo.getLabelGroupId());
                stringListHashMap.put("groupName", docLabelPojo.getLabelGroupName());
                list.add(stringListHashMap);
                groupIdList.add(docLabelPojo.getLabelGroupId());
            }

        }
        return TResponseVo.success(list);
    }

    /**
     * 获取投标文件组
     *
     * @param fileUuid
     * @param bidKey
     */
    @Override
    public TResponseVo getTendDocumentService(String fileUuid, String bidKey) throws Exception {
        // 验证bidKey
//        Integer i = workingTableMapper.checkBidKeyDao(fileUuid, bidKey);
//        if (i == 0){
//            return TResponseVo.error(ResponseEnum.ERROR,"无效的秘钥,请检查输入是否正确");
//        }
        // 获取主文件对应的文件组 取出投标文件
        List<LabelValuePojo> tendDocumentInfoList = workingTableMapper.getTendDocumentInfo2Dao(fileUuid);
        return TResponseVo.success(tendDocumentInfoList);
    }

    /**
     * 获取投标文件段落
     *
     * @param
     */
    @Override
    public TResponseVo getTendDocumentOutlineService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        if (docFileIndexPojo == null) {
            return TResponseVo.error(ResponseEnum.ERROR, "未能获取投标文件格式");
        }
        // 获取投标文件id和版本id
        String tendFileUuid = docFileIndexPojo.getFileUuid();
        // 获取最新版
//        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(tendFileUuid);
//        String tendFileVersionId = docAllInfoDao.getFileVersionId();
        // 获取当前文库的发行版本
//        DocFileIndexPojo docAllInfoDao = workingTableMapper.getOnlineVersionInfoDao(tendFileUuid);
//        String tendFileVersionId = docAllInfoDao.getFileVersionId();  // 发行版
        String tendFileVersionId = docFileIndexPojo.getFileVersionId();
        return getOutlineListService(tendFileUuid, tendFileVersionId);
    }

    /**
     * 根据招标文件识别码和秘钥 创建投标文件
     *
     * @param
     */
    @Override
    public TResponseVo newTendDocumentService(List<DocFileIndexPojo> docFileIndexPojoList, String userId, String mainFileUuid) throws Exception {
        String mainFileVersionId = null;
        if (mainFileUuid == null) {
            mainFileUuid = null;
            mainFileVersionId = null;
        } else {
            DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(mainFileUuid);
            if (docAllInfoDao.getMainFileUuid() != null) {
                mainFileUuid = docAllInfoDao.getMainFileUuid();
                mainFileVersionId = docAllInfoDao.getMainFileVersionId();
            } else {
                mainFileUuid = docAllInfoDao.getFileUuid();
                mainFileVersionId = docAllInfoDao.getFileVersionId();
            }
        }
        for (DocFileIndexPojo docFileIndexPojo : docFileIndexPojoList) {
            String fileUuid = docFileIndexPojo.getFileUuid();
            DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
            String fileVersionId = docAllInfoDao.getFileVersionId();
//            String fileVersionId = docFileIndexPojo.getFileVersionId();
            String fileName = docFileIndexPojo.getFileName();

            // 获取附属文件保留了哪些段落 后面复制参数的时候只保留这些段落内的参数
            List<String> outlineListMap = docFileIndexPojo.getChapters();
            String s = JsonKeyUtils.listToString(outlineListMap, "','");
            docFileIndexPojo.setOutlineIdListStr(s);
            docFileIndexPojo.setFileName(fileName);
            // 设置fileUuid versionId
            docFileIndexPojo.setOldFileUuid(fileUuid);
            docFileIndexPojo.setOldFileVersionId(fileVersionId);
            fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
            fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
            docFileIndexPojo.setFileUuid(fileUuid);
            docFileIndexPojo.setFileVersionId(fileVersionId);
            // 设置创建时间
            docFileIndexPojo.setCreateTime(new Date());
            // 设置用户信息
            docFileIndexPojo.setCreateUserId(userId);
            docFileIndexPojo.setUpdateUserId(userId);
            // 设置主文件信息
            // 第一个文件默认为主文件 其余的作为主文件的附属文件
            if (mainFileUuid == null) {
                mainFileUuid = fileUuid;
                mainFileVersionId = fileVersionId;
            } else {
                docFileIndexPojo.setMainFileUuid(mainFileUuid);
                docFileIndexPojo.setMainFileVersionId(mainFileVersionId);
            }
            try {
                int j = 0;
                int i = 0;
                // 创建索引
                i = workingTableMapper.newTendFileIndexDao(docFileIndexPojo);
                if (i == 0) {
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(700);
                    throw fileIndexException;
                }

                // 创建版本
                i = workingTableMapper.newTendFileVersionDao(docFileIndexPojo);
                if (i == 0) {
                    FileIndexException fileIndexException = new FileIndexException();
                    fileIndexException.setCode(701);
                    throw fileIndexException;
                }

                // 创建大纲
                List<OutLinePojo> newSubsidiaryFileOutlineDao = workingTableMapper.getNewTendFileOutlineDao(docFileIndexPojo);
                String sql = "";
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String dateString = formatter.format(new Date());
                for (OutLinePojo outLinePojo : newSubsidiaryFileOutlineDao) {
                    j++;
                    // 重新排序
                    outLinePojo.setOutlineOrder(String.valueOf(j));
                    sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outLinePojo.getOutlineReplaceGroupId() + "," + outLinePojo.getColor() + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                }
                if (sql.length() != 0) {  // 防止空文档无段落
                    sql = sql.substring(0, sql.length() - 1);
                    workingTableMapper.newTendFileOutlineDao(sql);
                }
                // 创建内容
                List<ContentPojo> subsidiaryFileContentDao = workingTableMapper.getTendFileContentDao(docFileIndexPojo);
                sql = "";
                for (ContentPojo contentPojo : subsidiaryFileContentDao) {
                    String contentText = contentPojo.getContentText();
//                    Document doc = Jsoup.parse(contentText);
//                    Elements elements = doc.select("bookmark");
//                    Elements elementsTmp = new Elements();
//                    elementsTmp.addAll(elements);
//                    for (Element element : elementsTmp) {
//                        element.remove();  // 去除bookmark标签
//                    }
//
//                    elements = doc.select("mark");
//                    elementsTmp = new Elements();
//                    elementsTmp.addAll(elements);
//                    for (Element element : elementsTmp) {
//                        element.before(element.html());
//                        element.remove();  // 去除mark标签
//                    }
//                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                    contentPojo.setContentText(contentText);
                    sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                }
                if (sql.length() != 0) {  // 防止空文档无段落
                    sql = sql.substring(0, sql.length() - 1);
                    workingTableMapper.newTendFileContentDao(sql);
                }
                // 创建参数
                workingTableMapper.newTendContentParamDao(docFileIndexPojo);

                // 创建标注
                workingTableMapper.newTendContentTagDao(docFileIndexPojo);

                // 创建数据表(如果有)
                workingTableMapper.newTendContentDbDao(docFileIndexPojo);

                // 投标文件由附属文件而来  附属文件的数据管理来自主文件  所以这里取主文件的数据管理
                // 获取招标文件中的投标文件模板的主文件信息
                // 查询主文件
                DocFileIndexPojo mainFile = fileOperationMapper.getDocAllInfoDao(docFileIndexPojo.getOldFileUuid());
                if (mainFile.getMainFileUuid() != null) {
                    docFileIndexPojo.setOldFileUuid(mainFile.getMainFileUuid());
                    docFileIndexPojo.setOldFileVersionId(mainFile.getMainFileVersionId());
                }
                // 创建数据管理(4张)
                workingTableMapper.newTendContentDmDbDao(docFileIndexPojo);
                workingTableMapper.newTendContentDmTableDao(docFileIndexPojo);
                workingTableMapper.newTendContentDmColumnsDao(docFileIndexPojo);
                String hashMod1 = new HashUtils().getHashMod(docFileIndexPojo.getFileUuid() + docFileIndexPojo.getFileVersionId());
                String hashMod2 = new HashUtils().getHashMod(docFileIndexPojo.getOldFileUuid() + docFileIndexPojo.getOldFileVersionId());
                System.out.println("表编号为: " + hashMod1);
                splitTableMapper.newTendContentDmDataDao(docFileIndexPojo, hashMod1, hashMod2, dmContentDataDatabase);

            } catch (FileIndexException e) {
                e.printStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                FileIndexException fileIndexException = new FileIndexException();
                fileIndexException.setCode(704);
                throw fileIndexException;
            }
        }

        HashMap<String, String> ret = new HashMap<>();
        ret.put("fileUuid", mainFileUuid);
        ret.put("fileVersionId", mainFileVersionId);
        return TResponseVo.success(ret);

    }


    /**
     * 根据投标文件获取招标文件fileUuid
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo getBidFromTendService(String fileUuid) throws Exception {
        String bidFromTendDao = workingTableMapper.getBidFromTendDao(fileUuid);
        Map<String, String> ret = new HashMap<>();
        ret.put("info", "执行成功");
        ret.put("fileUuid", bidFromTendDao);
        return TResponseVo.success(ret);
    }

    /**
     * 根据文件id获取最新版本信息
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo getLastVersionService(String fileUuid) throws Exception {
        String fileVersionId = workingTableMapper.getLastVersionDao(fileUuid);
        Map<String, String> retMap = new HashMap<>();
        retMap.put("fileVersionId", fileVersionId);
        return TResponseVo.success(retMap);
    }

    /**
     * 获取用户列表
     */
    @Override
    public TResponseVo getUserListService() throws Exception {
        List<LabelValuePojo> userListDao = workingTableMapper.getUserListDao();
        return TResponseVo.success(userListDao);
    }


    /**
     * 新建模板
     *
     * @param docFileIndexPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newDocumentService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        String fileTypeId = docFileIndexPojo.getFileTypeId();
        String fileTypeGroupId = workingTableMapper.getGroupIdByFileTypeIdDao(fileTypeId);
//        if (fileTypeId.equals("MB-QTWJ")) {
//            // 如果是其他文件类型的模板
//            //        文件名称
//            String fileName = docFileIndexPojo.getFileName();
//            if (StringUtils.isBlank(fileName)) {
//                return TResponseVo.error(ResponseEnum.ERROR, "文件名称不能为空");
//            }
//            //        文件版本（派生默认V1.0）
//            docFileIndexPojo.setFileVersionName("V1.0");
//            //        参与人
//            List<String> includeUserIdList = docFileIndexPojo.getIncludeUserIdList();
//            String includeUserList = new JsonKeyUtils().listToString(includeUserIdList);
//            docFileIndexPojo.setIncludeUserList(includeUserList);
//            //        文件类型
//            docFileIndexPojo.setFileTypeId(fileTypeId);
//            //        文件说明
//            String fileDesc = docFileIndexPojo.getFileDesc();
//            //        文件标签
//            List<String> fileLabelIds = docFileIndexPojo.getFileLabelIds();
//            docFileIndexPojo.setFileLabelList(new JsonKeyUtils().listToString(fileLabelIds, ","));
//
//            // 定义主键
//            String fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
//            String fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
//            docFileIndexPojo.setFileUuid(fileUuid);
//            docFileIndexPojo.setFileVersionId(fileVersionId);
//
//            String userId = docFileIndexPojo.getCreateUserId();
//
//            docFileIndexPojo.setCreateTime(new Date());
//            docFileIndexPojo.setFileClass("1");
//            docFileIndexPojo.setIsRootFile("1");
//            docFileIndexPojo.setFileStatus("1");
//            docFileIndexPojo.setAncestorsFileUuid(fileUuid);
//            if ("undefined".equals(docFileIndexPojo.getFolderId()) || "null".equals(docFileIndexPojo.getFolderId()) || StringUtils.isBlank(docFileIndexPojo.getFolderId())) {
//                docFileIndexPojo.setFolderId(null);
//            }
//
//            int i = 0;
//            // 创建索引
//            i = fileOperationMapper.newQTDocumentIndexDao(docFileIndexPojo);
//            if (i == 0) {
//                FileIndexException fileIndexException = new FileIndexException();
//                fileIndexException.setCode(700);
//                throw fileIndexException;
//            }
//
//            // 创建版本
//            i = fileOperationMapper.newQTDocumentVersionDao(docFileIndexPojo);
//            if (i == 0) {
//                FileIndexException fileIndexException = new FileIndexException();
//                fileIndexException.setCode(701);
//                throw fileIndexException;
//            }
//
//            // 创建大纲
//            OutLinePojo outLinePojo = new OutLinePojo();
//            outLinePojo.setOutlineId("0");
//            outLinePojo.setOutlineText("欢迎使用");
//            outLinePojo.setFileUuid(fileUuid);
//            outLinePojo.setFileVersionId(fileVersionId);
//            outLinePojo.setOutlineOrder("1");
//            outLinePojo.setOutlineLevel("1");
//            outLinePojo.setCreateUserId(userId);
//            outLinePojo.setUpdateUserId(userId);
//            outLinePojo.setCreateTime(new Date());
//            fileOperationMapper.newQTDocumentOutLinePojoDao(outLinePojo);
//
//            // 创建内容
//            ContentPojo contentPojo = new ContentPojo();
//            contentPojo.setContentId(UUID.randomUUID().toString().replaceAll("-", ""));
//            contentPojo.setFileUuid(fileUuid);
//            contentPojo.setFileVersionId(fileVersionId);
//            contentPojo.setContentText("欢迎使用韩非智库");
//            contentPojo.setOutlineId("0");
//            contentPojo.setCreateTime(new Date());
//            fileOperationMapper.newQTDocumentContentPojoDao(contentPojo);
//
//            return fileOperationServiceImpl.getTrueVersionIdService(docFileIndexPojo.getFileUuid(), session);
//        }
//        // 根据fileTypeGroupId判断创建的是模板还是文件
//        else
            if (fileTypeGroupId.equals("MB")) { // 模板
            // 如果模板来源是系统内的 则获取uuid 否则生成一个
            String fileUuid = docFileIndexPojo.getFileUuid();
            if (StringUtils.isBlank(fileUuid)) { // 上传模板路线
                // 模板文件名
                String fileName = docFileIndexPojo.getFileName();
                if (StringUtils.isBlank(fileName)) {
                    return TResponseVo.error(ResponseEnum.ERROR, "文件名称不能为空");
                }
                Map<String, Object> fileInfoMap = docFileIndexPojo.getFile();
                fileUuid = (String) fileInfoMap.get("fileUuid");
                // 获取父文件的fileUuid fileVersionId
//                String fileUuid = docFileIndexPojo.getFileUuid();
                DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
                if (docAllInfoDao.getMainFileUuid() != null) {
                    return TResponseVo.error(ResponseEnum.ERROR, "附属文件无派生功能");
                }
                String fileVersionId = docAllInfoDao.getFileVersionId();

//        文件版本（派生默认V1.0）
                docFileIndexPojo.setFileVersionName("V1.0");
//        参与人
                List<String> includeUserIdList = docFileIndexPojo.getIncludeUserIdList();
                String includeUserList = new JsonKeyUtils().listToString(includeUserIdList);
                docFileIndexPojo.setIncludeUserList(includeUserList);
//        文件类型
                docFileIndexPojo.setFileTypeId(fileTypeId);
//        适用范围
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
                        sql = sql + "(\"" + outLinePojo.getOutlineId() + "\"," + JSON.toJSONString(outLinePojo.getOutlineText()) + ",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + outLinePojo.getOutlineFatherId() + "\",\"" + outLinePojo.getOutlineOrder() + "\",\"" + outLinePojo.getOutlineLevel() + "\",\"" + outLinePojo.getOutlineProperty() + "\"," + outlineReplaceGroupId + "," + color + ",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
//                        System.out.println("sql : " + sql);
                        fileOperationMapper.addDeriveFileOutlineDao(sql);
                    }

                    // 创建内容
                    List<ContentPojo> deriveFileContentDao = fileOperationMapper.getDeriveFileContentDao(docFileIndexPojo);
                    sql = "";
                    for (ContentPojo contentPojo : deriveFileContentDao) {
                        String contentText = contentPojo.getContentText();
//                        Document doc = Jsoup.parse(contentText);
//                        Elements elements = doc.select("mark");
//                        Elements elementsTmp = new Elements();
//                        elementsTmp.addAll(elements);
//                        for (Element element : elementsTmp) {
//                            element.before(element.html());
//                            element.remove();  // 去除mark标签
//                        }
//                        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                        contentPojo.setContentText(contentText);
                        sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                    }
                    if (sql.length() != 0) {
                        sql = sql.substring(0, sql.length() - 1);
                        fileOperationMapper.addDeriveFileContentDao(sql);
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
                ret.put("fileUuid", docFileIndexPojo.getFileUuid());
                ret.put("fileVersionId", docFileIndexPojo.getFileVersionId());
                ret.put("info", "创建成功");
                return fileOperationServiceImpl.getTrueVersionIdService(docFileIndexPojo.getFileUuid(), session);
            } else {
                // 选择模板路线 (派生)
                DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
                if (docAllInfoDao.getMainFileUuid() != null) {
                    return TResponseVo.error(ResponseEnum.ERROR, "附属文件无派生功能");
                }
//                String fileVersionId = docAllInfoDao.getFileVersionId();
                String fileVersionId = docFileIndexPojo.getFileVersionId();
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
                docFileIndexPojo.setFileTypeId(fileTypeId);
//        适用范围
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
//        文件说明
                String fileDesc = docFileIndexPojo.getFileDesc();
//        文件标签
                List<String> fileLabelIds = docFileIndexPojo.getFileLabelIds();
                docFileIndexPojo.setFileLabelList(new JsonKeyUtils().listToString(fileLabelIds, ","));

                // 设置父文件uuid
                docFileIndexPojo.setFileParentId(fileUuid);
                docFileIndexPojo.setOldFileVersionId(fileVersionId);
//                if (!docAllInfoDao.getFileVersionId().equals(fileVersionId)) {
//                    // 说明 已当前最新的"正式版"为准
//                    docFileIndexPojo.setOldFileVersionId(docAllInfoDao.getFileVersionId());
//                }
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
                    if (sql.length() != 0) {  // 防止空文档无段落
                        sql = sql.substring(0, sql.length() - 1);
//                        System.out.println("sql : " + sql);
                        fileOperationMapper.addDeriveFileOutline2Dao(sql);
                    }

                    // 创建内容
                    List<ContentPojo> deriveFileContentDao = fileOperationMapper.getDeriveFileContentDao(docFileIndexPojo);
                    sql = "";
                    for (ContentPojo contentPojo : deriveFileContentDao) {
                        String contentText = contentPojo.getContentText();
//                        Document doc = Jsoup.parse(contentText);
//                        Elements elements = doc.select("mark");
//                        Elements elementsTmp = new Elements();
//                        elementsTmp.addAll(elements);
//                        for (Element element : elementsTmp) {
//                            element.before(element.html());
//                            element.remove();  // 去除mark标签
//                        }
//                        contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                        contentPojo.setContentText(contentText);
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
//                        System.out.println("allSubsidiaryFileListDao : " + allSubsidiaryFileListDao.size());
//                        System.out.println("docFileIndexPojo : " + JSON.toJSONString(docFileIndexPojo));
//                        System.out.println("fileIndexPojo : " + JSON.toJSONString(fileIndexPojo));
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
                        if (sql.length() != 0) {  // 防止空文档无段落
                            sql = sql.substring(0, sql.length() - 1);
//                            System.out.println("sql : " + sql);
                            fileOperationMapper.addDeriveFileOutline2Dao(sql);
                        }
                        // 创建内容
                        deriveFileContentDao = fileOperationMapper.getDeriveFileContentDao(fileIndexPojo);
                        sql = "";
                        for (ContentPojo contentPojo : deriveFileContentDao) {
                            String contentText = contentPojo.getContentText();
//                            Document doc = Jsoup.parse(contentText);
//                            Elements elements = doc.select("mark");
//                            Elements elementsTmp = new Elements();
//                            elementsTmp.addAll(elements);
//                            for (Element element : elementsTmp) {
//                                element.before(element.html());
//                                element.remove();  // 去除mark标签
//                            }
//                            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                            contentPojo.setContentText(contentText);
                            sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                        }
                        if (sql.length() != 0) {  // 防止空文档无段落
                            sql = sql.substring(0, sql.length() - 1);
                            fileOperationMapper.addDeriveFileContentDao(sql);
                        }
                        // 创建参数
                        fileOperationMapper.addDeriveContentParamDao(fileIndexPojo);

                        // 创建标注
                        fileOperationMapper.addDeriveContentTagDao(fileIndexPojo);

                        // 创建书签
                        fileOperationMapper.addDeriveContentBookmarkDao(fileIndexPojo);

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

                // 是否存在模板自定义
                List<Map<String, Object>> merge = docFileIndexPojo.getMerge();
                if (merge != null) {  // 如果merge不为空 说明存在模板自定义操作
                    Boolean buildSub = docFileIndexPojo.getBuildSub();
                    ArrayList<String> fileUuid2List = new ArrayList<>();  // 用来记录被选模板的fileUuid 并去重
                    String outlineId = "";  // 被替换的段落id
                    String fileUuid2 = "";  // 来源文件id
                    List<String> outlineIdList = new ArrayList<>();  // 来源段落列表
                    for (Map<String, Object> map : merge) {
                        outlineId = (String) ((Map) map.get("target")).get("outlineId");
                        outlineIdList = (List) ((Map) map.get("value")).get("outlineId");
                        fileUuid2 = (String) ((Map) map.get("value")).get("fileUuid");
                        if (!fileUuid2List.contains(fileUuid2)) {
                            fileUuid2List.add(fileUuid2);  //
                        }
                        DocFileIndexPojo pojo = new DocFileIndexPojo();
                        pojo.setFileUuid(docFileIndexPojo.getFileUuid());
                        pojo.setFileVersionId(docFileIndexPojo.getFileVersionId());
                        pojo.setOutlineId(outlineId);
                        pojo.setFileUuid2(fileUuid2);
                        pojo.setOutlineIdList2(outlineIdList);
//                        System.out.println("pojo : " + JSON.toJSONString(pojo));
                        fileOperationService.newCompoundFileService(pojo);
                    }
                    if (buildSub != null && buildSub == true) {
                        for (String fileUuid3 : fileUuid2List) {
//                            DocFileIndexPojo docAllInfo = fileOperationMapper.getDocAllInfoDao(fileUuid3);
                            DocFileIndexPojo docAllInfo = workingTableMapper.getOnlineVersionInfoDao(fileUuid3);
                            if (docAllInfo == null) {
                                throw new WorkTableException("替换模板中有模板已经下线,请重新选择");
                            }
                            String fileVersionId3 = docAllInfo.getFileVersionId();

                            docAllInfo.setMainFileUuid(fileUuid3);
                            docAllInfo.setOldFileVersionId(fileVersionId3);
                            docAllInfo.setMainFileVersionId(fileVersionId3);
                            docAllInfo.setStaticFileUuid(docFileIndexPojo.getFileUuid());
                            docAllInfo.setStaticFileVersionId(docFileIndexPojo.getFileVersionId());

                            fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                            fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                            docAllInfo.setFileUuid(fileUuid);
                            docAllInfo.setFileVersionId(fileVersionId);

                            // 获取附属文件保留了哪些段落 后面复制参数的时候只保留这些段落内的参数
                            docAllInfo.setOutlineIdListStr(null);

                            docAllInfo.setCreateTime(new Date());
                            try {
                                int j = 0;
                                int i = 0;
                                // 创建索引
                                i = fileOperationMapper.newSubsidiaryFileIndexDao2(docAllInfo);
                                if (i == 0) {
                                    FileIndexException fileIndexException = new FileIndexException();
                                    fileIndexException.setCode(700);
                                    throw fileIndexException;
                                }

                                // 创建版本
                                i = fileOperationMapper.newSubsidiaryFileVersionDao(docAllInfo);
                                if (i == 0) {
                                    FileIndexException fileIndexException = new FileIndexException();
                                    fileIndexException.setCode(701);
                                    throw fileIndexException;
                                }

                                // 创建大纲
                                List<OutLinePojo> newSubsidiaryFileOutlineDao = fileOperationMapper.getNewSubsidiaryFileOutlineDao(docAllInfo);  // OutlineReplaceGroupId color 是null
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
                                if (sql.length() != 0) {  // 防止空文档无段落
                                    sql = sql.substring(0, sql.length() - 1);
                                    fileOperationMapper.newSubsidiaryFileOutlineDao(sql);
                                }
                                // 创建内容
                                List<ContentPojo> subsidiaryFileContentDao = fileOperationMapper.getSubsidiaryFileContentDao(docAllInfo);
                                sql = "";
                                for (ContentPojo contentPojo : subsidiaryFileContentDao) {
                                    String contentText = contentPojo.getContentText();
//                                    Document doc = Jsoup.parse(contentText);
//                                    Elements elements = doc.select("bookmark");
//                                    Elements elementsTmp = new Elements();
//                                    elementsTmp.addAll(elements);
//                                    for (Element element : elementsTmp) {
//                                        element.remove();  // 去除bookmark标签
//                                    }
//
//                                    elements = doc.select("mark");
//                                    elementsTmp = new Elements();
//                                    elementsTmp.addAll(elements);
//                                    for (Element element : elementsTmp) {
//                                        element.before(element.html());
//                                        element.remove();  // 去除mark标签
//                                    }
//                                    contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
//                                    contentPojo.setContentText(contentText);
                                    sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                                }
                                if (sql.length() != 0) {  // 防止空文档无段落
                                    sql = sql.substring(0, sql.length() - 1);
                                    fileOperationMapper.newSubsidiaryFileContentDao(sql);
                                }
                                // 创建参数
                                fileOperationMapper.newSubsidiaryContentParamDao(docAllInfo);

                                // 创建标注
                                fileOperationMapper.newSubsidiaryContentTagDao(docAllInfo);


                            } catch (FileIndexException e) {
                                e.printStackTrace();
                                throw e;
                            } catch (Exception e) {
                                e.printStackTrace();
                                FileIndexException fileIndexException = new FileIndexException();
                                fileIndexException.setCode(704);
                                throw fileIndexException;
                            }
                        }
                    }
                }

                Map<String, String> ret = new HashMap<>();
                ret.put("fileUuid", docFileIndexPojo.getFileUuid());
                ret.put("fileVersionId", docFileIndexPojo.getFileVersionId());
                ret.put("info", "创建成功");
                return fileOperationServiceImpl.getTrueVersionIdService(docFileIndexPojo.getFileUuid(), session);
            }
        } else if (fileTypeGroupId.equals("WJ")) { // 文件
            if (fileTypeId.equals("WJ-TBWJ")) { // 如果是投标文件
                List<DocFileIndexPojo> docFileIndexPojoList = docFileIndexPojo.getDocFileIndexPojoList();
                String userId = docFileIndexPojo.getCreateUserId();
                String mainFileUuid = docFileIndexPojo.getMainFileUuid();
                return newTendDocumentService(docFileIndexPojoList, userId, mainFileUuid);
            } else {
                List<Map<String, Object>> merge = docFileIndexPojo.getMerge();
                if (merge != null) {  // 如果merge不为空 说明存在模板自定义操作
                    String fileUuid = docFileIndexPojo.getFileUuid();
                    DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
                    String fileVersionId = docAllInfoDao.getFileVersionId();
                    docFileIndexPojo.setFileVersionId(fileVersionId);
                    // 判断是否有自定义选择的段落
                    List<String> outlineListMap = docFileIndexPojo.getOutlineListMap();
                    if (outlineListMap != null && outlineListMap.size() != 0) {
                        docFileIndexPojo.setOutlineListMap(outlineListMap);
                    } else {
                        List<OutLinePojo> outlineListDao = fileOperationMapper.getOutlineListDao(fileUuid, fileVersionId);
                        ArrayList<String> strings = new ArrayList<>();
                        for (OutLinePojo outLinePojo : outlineListDao) {
                            String outlineId = outLinePojo.getOutlineId();
                            strings.add(outlineId);
                        }
                        docFileIndexPojo.setOutlineListMap(strings);
                    }
                    Map<String, String> retMap = newBidDocumentService(docFileIndexPojo);

                    String userId = docFileIndexPojo.getCreateUserId();
                    Boolean buildSub = docFileIndexPojo.getBuildSub();
                    ArrayList<String> fileUuid2List = new ArrayList<>();  // 用来记录被选模板的fileUuid 并去重
                    String outlineId = "";  // 被替换的段落id
                    String fileUuid2 = "";  // 来源文件id
                    List<String> outlineIdList = new ArrayList<>();  // 来源段落列表
                    for (Map<String, Object> map : merge) {
                        outlineId = (String) ((Map) map.get("target")).get("outlineId");
                        outlineIdList = (List) ((Map) map.get("value")).get("outlineId");
                        fileUuid2 = (String) ((Map) map.get("value")).get("fileUuid");
                        if (!fileUuid2List.contains(fileUuid2)) {
                            fileUuid2List.add(fileUuid2);  //
                        }
                        DocFileIndexPojo pojo = new DocFileIndexPojo();
                        pojo.setFileUuid(retMap.get("fileUuid"));
                        pojo.setFileVersionId(retMap.get("fileVersionId"));
                        pojo.setOutlineId(outlineId);
                        pojo.setFileUuid2(fileUuid2);
                        pojo.setOutlineIdList2(outlineIdList);
//                        System.out.println("pojo : " + JSON.toJSONString(pojo));
                        fileOperationService.newCompoundFileService(pojo);
                    }
                    if (buildSub != null && buildSub == true) {
                        // 文件类型由模板改为文件
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
                        for (String fileUuid3 : fileUuid2List) {

                            DocFileIndexPojo docAllInfo = fileOperationMapper.getDocAllInfoDao(fileUuid3);
                            String fileVersionId3 = docAllInfo.getFileVersionId();

                            docAllInfo.setMainFileUuid(fileUuid3);
                            docAllInfo.setOldFileVersionId(fileVersionId3);
                            docAllInfo.setMainFileVersionId(fileVersionId3);
                            docAllInfo.setStaticFileUuid(retMap.get("fileUuid"));
                            docAllInfo.setStaticFileVersionId(retMap.get("fileVersionId"));

                            fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
                            fileVersionId = UUID.randomUUID().toString().replaceAll("-", "");
                            docAllInfo.setFileUuid(fileUuid);
                            docAllInfo.setFileVersionId(fileVersionId);

                            // 获取附属文件保留了哪些段落 后面复制参数的时候只保留这些段落内的参数
                            docAllInfo.setOutlineIdListStr(null);

                            docAllInfo.setCreateTime(new Date());
                            try {
                                int j = 0;
                                int i = 0;
                                // 创建索引
                                if (modelFileMap.get(docAllInfo.getFileTypeId()) != null) {
                                    docAllInfo.setFileTypeId(modelFileMap.get(docAllInfo.getFileTypeId()));
                                    docAllInfo.setFileTypeName(modelFileNameMap.get(docAllInfo.getFileTypeId()));
                                }
                                i = fileOperationMapper.newSubsidiaryFileIndexDao3(docAllInfo);
                                if (i == 0) {
                                    FileIndexException fileIndexException = new FileIndexException();
                                    fileIndexException.setCode(700);
                                    throw fileIndexException;
                                }

                                // 创建版本
                                i = fileOperationMapper.newSubsidiaryFileVersionDao(docAllInfo);
                                if (i == 0) {
                                    FileIndexException fileIndexException = new FileIndexException();
                                    fileIndexException.setCode(701);
                                    throw fileIndexException;
                                }

                                // 创建大纲
                                List<OutLinePojo> newSubsidiaryFileOutlineDao = fileOperationMapper.getNewSubsidiaryFileOutlineDao(docAllInfo);  // OutlineReplaceGroupId color 是null
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
                                if (sql.length() != 0) {  // 防止空文档无段落
                                    sql = sql.substring(0, sql.length() - 1);
                                    fileOperationMapper.newSubsidiaryFileOutlineDao(sql);
                                }
                                // 创建内容
                                List<ContentPojo> subsidiaryFileContentDao = fileOperationMapper.getSubsidiaryFileContentDao(docAllInfo);
                                sql = "";
                                for (ContentPojo contentPojo : subsidiaryFileContentDao) {
                                    String contentText = contentPojo.getContentText();
                                    sql = sql + "(\"" + contentPojo.getContentId() + "\"," + JSON.toJSONString(contentText) + ",\"" + contentPojo.getOutlineId() + "\",\"" + fileUuid + "\",\"" + fileVersionId + "\",\"" + dateString + "\"),";
                                }
                                if (sql.length() != 0) {  // 防止空文档无段落
                                    sql = sql.substring(0, sql.length() - 1);
                                    fileOperationMapper.newSubsidiaryFileContentDao(sql);
                                }
                                // 创建参数
                                fileOperationMapper.newSubsidiaryContentParamDao(docAllInfo);

                                // 创建标注
                                fileOperationMapper.newSubsidiaryContentTagDao(docAllInfo);

                                // 创建数据表(如果有)
                                fileOperationMapper.newSubsidiaryContentDbDao(docAllInfo);


                            } catch (FileIndexException e) {
                                e.printStackTrace();
                                throw e;
                            } catch (Exception e) {
                                e.printStackTrace();
                                FileIndexException fileIndexException = new FileIndexException();
                                fileIndexException.setCode(704);
                                throw fileIndexException;
                            }
                        }
                    }
                    return TResponseVo.success(retMap);
                } else {
                    Map<String, String> retMap = newBidDocumentService(docFileIndexPojo);
                    return TResponseVo.success(retMap);
                }

            }

        } else {
            return TResponseVo.error("请检查文件类型字段入参是否正确");
        }

    }

    /**
     * 新建模板-其他文件
     *
     * @param docFileIndexPojo
     * @param session
     */
    @Override
    public TResponseVo newQTDocumentService(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        return null;
    }

    /**
     * 新增辅助工具表单
     *
     * @param hfSupToolFormPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newSupToolFormService(HfSupToolFormPojo hfSupToolFormPojo) throws Exception {
        String formId = UUID.randomUUID().toString().replaceAll("-", "");
        String formName = hfSupToolFormPojo.getFormName();
        if (StringUtils.isBlank(formName)) {
            TResponseVo.error("名称不能为空");
        }
        try {
            int j = workingTableMapper.checkFormNameDao(formName, null);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newSupToolFormService", formName, "当前类错误码-2101-名称已存在");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(2101);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newSupToolFormService", formName, "当前类错误码-2102-校重SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(2102);
            throw questionnaireException;
        }
        hfSupToolFormPojo.setFormId(formId);
        hfSupToolFormPojo.setFormValue(JSON.toJSONString(hfSupToolFormPojo.getFormValue()));
        workingTableMapper.newSupToolFormDao(hfSupToolFormPojo);
        HashMap<String, String> map = new HashMap<>();
        map.put("info", "创建完成");
        map.put("formId", formId);
        return TResponseVo.success(map);
    }

    /**
     * 删除辅助工具表单
     *
     * @param formId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delSupToolFormService(String formId) throws Exception {
        workingTableMapper.delSupToolFormDao(formId);
        HashMap<String, String> map = new HashMap<>();
        map.put("info", "删除完成");
        return TResponseVo.success(map);
    }

    /**
     * 修改辅助工具表单
     *
     * @param hfSupToolFormPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateSupToolFormService(HfSupToolFormPojo hfSupToolFormPojo) throws Exception {
        String formId = hfSupToolFormPojo.getFormId();
        String formName = hfSupToolFormPojo.getFormName();
        if (StringUtils.isBlank(formId) || StringUtils.isBlank(formName)) {
            TResponseVo.error("必填参数不能为空");
        }
        try {
            int j = workingTableMapper.checkFormNameDao(formName, formId);
            if (j >= 1) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateSupToolFormService", formName, "当前类错误码-2101-名称已存在");
                FileIndexException questionnaireException = new FileIndexException();
                questionnaireException.setCode(2101);
                throw questionnaireException;
            }
        } catch (FileIndexException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateSupToolFormService", formName, "当前类错误码-2102-校重SQL错误");
            FileIndexException questionnaireException = new FileIndexException();
            questionnaireException.setCode(2102);
            throw questionnaireException;
        }
        hfSupToolFormPojo.setFormValue(JSON.toJSONString(hfSupToolFormPojo.getFormValue()));
        workingTableMapper.updateSupToolFormDao(hfSupToolFormPojo);
        HashMap<String, String> map = new HashMap<>();
        map.put("info", "修改完成");
        return TResponseVo.success(map);
    }

    /**
     * 查询辅助工具表单信息
     *
     * @param formId
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getSupToolFormInfoService(String formId) throws Exception {
        if (StringUtils.isBlank(formId)) {
            TResponseVo.error("必填参数不能为空");
        }
        HfSupToolFormPojo supToolFormInfoDao = workingTableMapper.getSupToolFormInfoDao(formId);
        supToolFormInfoDao.setFormValue(JSON.parseObject(String.valueOf(supToolFormInfoDao.getFormValue()), Map.class));
        return TResponseVo.success(supToolFormInfoDao);
    }

    /**
     * 查询辅助工具表单信息
     *
     * @param pagePojo
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getSupToolFormListService(PagePojo pagePojo, String userId) throws Exception {
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<HfSupToolFormPojo> hfSupToolPojoList = workingTableMapper.getSupToolListDao();
        List<HfSupToolFormPojo> hfSupToolFormPojoList = workingTableMapper.getSupToolFormListDao(userId);

        // 组装返回数据格式
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        for (HfSupToolFormPojo hfSupToolPojo : hfSupToolPojoList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("key", hfSupToolPojo.getToolId());
            map.put("label", hfSupToolPojo.getToolName());
            map.put("fields", JSON.parseObject(hfSupToolPojo.getToolText(), List.class));
            list.add(map);
        }
        for (HfSupToolFormPojo hfSupToolFormPojo : hfSupToolFormPojoList) {
            for (HashMap<String, Object> stringObjectHashMap : list) {
                if (stringObjectHashMap.get("key").equals(hfSupToolFormPojo.getToolId())) {
                    if (stringObjectHashMap.get("children") == null) {
                        ArrayList<HashMap<String, Object>> children = new ArrayList<>();
                        HashMap<String, Object> child = new HashMap<>();
                        child.put("key", hfSupToolFormPojo.getFormId());
                        child.put("parentKey", stringObjectHashMap.get("key"));
                        child.put("label", hfSupToolFormPojo.getFormName());
                        child.put("fieldValues", JSON.parseObject(String.valueOf(hfSupToolFormPojo.getFormValue()), Map.class));
                        children.add(child);
                        stringObjectHashMap.put("children", children);
                    } else {
                        HashMap<String, Object> child = new HashMap<>();
                        child.put("key", hfSupToolFormPojo.getFormId());
                        child.put("parentKey", stringObjectHashMap.get("key"));
                        child.put("label", hfSupToolFormPojo.getFormName());
                        child.put("fieldValues", JSON.parseObject(String.valueOf(hfSupToolFormPojo.getFormValue()), Map.class));
                        ((List<HashMap<String, Object>>) stringObjectHashMap.get("children")).add(child);
                    }
                    break;
                }
            }
        }
        return TResponseVo.success(list);
    }


    /**
     * 新建辅助工具模型
     *
     * @param hfSupTableFormPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newSupTableService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception {
        // 分配id
        String tableId = UUID.randomUUID().toString().replaceAll("-", "");
        hfSupTableFormPojo.setTableId(tableId);
        // 对象转str
        List<Map<String, String>> tableText = (List) hfSupTableFormPojo.getColumns();
        if (tableText == null) {
            tableText = new ArrayList<>();
        }

        // 前端删除title 后端将dataIndex写入title 与后续逻辑保持一致
        for (Map<String, String> map : tableText) {
            String s = map.get("dataIndex");
            map.put("title", s);
        }

        String tableTextStr = JSON.toJSONString(tableText);
        List<Map<String, Object>> tableTextList = JSON.parseObject(tableTextStr, List.class);
        for (Map<String, Object> map : tableTextList) {
            if ("80".equals(String.valueOf(map.get("dataTypeId")))) {
                map.put("unitId", "770001"); // 遇到金额默认设置为"元"
            }
        }
        tableTextStr = JSON.toJSONString(tableTextList);

        // 布尔转str
        String enabled = String.valueOf(hfSupTableFormPojo.getEnabled());
        hfSupTableFormPojo.setEnabled(enabled);
        // 判断主键重复
        List<Map<String, Object>> tableTextlist = JSON.parseObject(tableTextStr, List.class);
        List<String> dataIndexList = new ArrayList<>();
        for (Map<String, Object> map : tableTextlist) {
            String dataIndex = String.valueOf(map.get("dataIndex"));
            if (dataIndexList.contains(dataIndex)) {
                return TResponseVo.error("模型中的字段名(主键)不允许重复");
            } else {
                dataIndexList.add(dataIndex);
            }
        }
        hfSupTableFormPojo.setColumns(tableTextStr);
        // label的list转str
        if (hfSupTableFormPojo.getLabel() == null) {
            hfSupTableFormPojo.setLabel(null);
        } else {
            String label = JSON.toJSONString(hfSupTableFormPojo.getLabel());
            hfSupTableFormPojo.setLabel(label);
        }

        // 入库
        workingTableMapper.newSupTableDao(hfSupTableFormPojo);

        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("info", "创建成功");
        retMap.put("tableId", tableId);
        return TResponseVo.success(retMap);
    }

    /**
     * 新增辅助工具分类标签
     *
     * @param labelValuePojo
     */
    @Override
    public TResponseVo addSupLabelService(LabelValuePojo labelValuePojo, String userId) throws Exception {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String label = labelValuePojo.getLabel();
        String value = UUID.randomUUID().toString().replaceAll("-", "");
        // 统一用户下不允许重名标签
        Integer integer = workingTableMapper.checkSupLabelDao(label, userId);
        if (integer > 0) {
            return TResponseVo.error("同名标签已存在");
        }
        workingTableMapper.addSupLabelDao(uuid, label, value, userId);
        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("info", "新增标签完成");
        retMap.put("uuid", uuid);
        return TResponseVo.success(retMap);
    }

    /**
     * 删除辅助工具分类标签
     *
     * @param
     */
    @Override
    public TResponseVo delSupLabelService(String uuid, String userId) throws Exception {
        LabelValuePojo supLabelInfoDao = workingTableMapper.getSupLabelInfoDao(uuid);
        if (supLabelInfoDao != null) {
            String value = supLabelInfoDao.getValue();
            // 检查该标签是否已被使用
            List<HfSupTableFormPojo> supTableList = workingTableMapper.getSupTableListDao(userId, null, null);
            for (HfSupTableFormPojo hfSupTableFormPojo : supTableList) {
                String label = (String) hfSupTableFormPojo.getLabel();
                List<String> labelList = JSON.parseObject(label, List.class);
                if (labelList != null) {
                    if (labelList.contains(value)) {
//                        labelList.remove(value);
//                        label = JSON.toJSONString(labelList);
//                        workingTableMapper.updateSupTableLabelDao(hfSupTableFormPojo.getTableId(), label);
                        return TResponseVo.error("该标签已被使用无法删除");
                    }
                }
            }
            workingTableMapper.delSupLabelDao(uuid);
            HashMap<String, String> retMap = new HashMap<>();
            retMap.put("info", "删除标签完成");
            return TResponseVo.success(retMap);
        } else {
            return TResponseVo.error("数据不存在或已删除");
        }
    }

    /**
     * 获取辅助工具分类标签列表
     *
     * @param userId
     */
    @Override
    public TResponseVo getSupLabelListService(String userId) throws Exception {
        List<LabelValuePojo> supLabelListDao = workingTableMapper.getSupLabelListDao(userId);
        return TResponseVo.success(supLabelListDao);
    }

    /**
     * 删除辅助工具模型
     *
     * @param hfSupTableFormPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delSupTableService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception {
        String tableId = hfSupTableFormPojo.getTableId();
        workingTableMapper.delSupTableFormDao(null, tableId);
        workingTableMapper.delSupTableDao(tableId);
        return TResponseVo.success("删除成功");
    }

    /**
     * 更新辅助工具模型
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateSupTableService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception {
        // 对象转str
        Object tableText = hfSupTableFormPojo.getColumns();
        String tableTextStr = JSON.toJSONString(tableText);
        List<Map<String, Object>> tableTextList = JSON.parseObject(tableTextStr, List.class);
        // 判断主键重复
        List<String> dataIndexList = new ArrayList<>();
        for (Map<String, Object> map : tableTextList) {
            String dataIndex = String.valueOf(map.get("dataIndex"));
            if (dataIndexList.contains(dataIndex)) {
                return TResponseVo.error("模型中的字段名(主键)不允许重复");
            } else {
                dataIndexList.add(dataIndex);
            }
        }
        // 默认金额单位 元
        for (Map<String, Object> map : tableTextList) {
            Object s = map.get("dataIndex");
            map.put("title", s);
            if ("80".equals(String.valueOf(map.get("dataTypeId")))) {
                map.put("unitId", "770001"); // 遇到金额默认设置为"元"
            }
        }

        tableTextStr = JSON.toJSONString(tableTextList);
        hfSupTableFormPojo.setColumns(tableTextStr);
        // 布尔转str
        String enabled = String.valueOf(hfSupTableFormPojo.getEnabled());
        hfSupTableFormPojo.setEnabled(enabled);
        // label的list转str
        if (hfSupTableFormPojo.getLabel() == null) {
            hfSupTableFormPojo.setLabel(null);
        } else {
            List<String> label = (List) hfSupTableFormPojo.getLabel();
            if (label.size() == 0) {
                hfSupTableFormPojo.setLabel(null);
            } else {
                String s = JSON.toJSONString(label);
                hfSupTableFormPojo.setLabel(s);
            }
        }

        // 更新库
        workingTableMapper.updateSupTableDao(hfSupTableFormPojo);
        return TResponseVo.success("更新成功");
    }

    /**
     * 查询辅助工具模型
     *
     * @param hfSupTableFormPojo
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getSupTableInfoService(HfSupTableFormPojo hfSupTableFormPojo, String userId) throws Exception {
        String tableId = hfSupTableFormPojo.getTableId();
        HfSupTableFormPojo supTableInfoDao = workingTableMapper.getSupTableInfoDao(tableId);
        String label = (String) supTableInfoDao.getLabel();
        List<String> labelList = JSON.parseObject(label, List.class);
        if (label == null) {
            labelList = new ArrayList<>();
        }
        // 标签中文名
        List<String> labelNameList = new ArrayList<>();
        List<LabelValuePojo> supLabelListDao = workingTableMapper.getSupLabelListDao(userId);
        for (String s : labelList) {
            for (LabelValuePojo labelValuePojo : supLabelListDao) {
                if (s.equals(labelValuePojo.getValue())) {
                    labelNameList.add(labelValuePojo.getLabel());
                }
            }
        }
        supTableInfoDao.setLabel(labelList);
        supTableInfoDao.setLabelNameList(labelNameList);

        if (userId.equals(supTableInfoDao.getCreateUserId())) {
            // str转对象
            String columns = String.valueOf(supTableInfoDao.getColumns());
            supTableInfoDao.setColumns(JSON.parseObject(columns, List.class));
            // str转布尔
            String enabled = String.valueOf(supTableInfoDao.getEnabled());
            supTableInfoDao.setEnabled("true".equals(enabled) ? true : false);
            return TResponseVo.success(supTableInfoDao);
        } else {
            return TResponseVo.error("无权限查看该数据");
        }
    }

    /**
     * 查询辅助工具模型列表
     *
     * @param pagePojo
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getSupTableListService(PagePojo pagePojo, String userId, List<String> label, Boolean enable) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        String enableStr = null;
        if (enable != null) {
            if (enable) {
                enableStr = "true";
            } else {
                enableStr = "false";
            }
        }
        List<HfSupTableFormPojo> supTableList = workingTableMapper.getSupTableListDao(userId, paramNameLike, enableStr);
        // 先根据label做一步筛选
        if (label != null) {  // 当筛选条件不为空时
            List<HfSupTableFormPojo> supTableListTmp = new ArrayList<>();
            supTableListTmp.addAll(supTableList);
            for (HfSupTableFormPojo hfSupTableFormPojo : supTableListTmp) {
                String label1 = (String) hfSupTableFormPojo.getLabel();
                List<String> labelList1 = JSON.parseObject(label1, List.class);
                for (String l : label) { // 对选中的标签逐一判断 有一个不包含就剔除
                    if (labelList1 != null && labelList1.contains(l)) {
                        continue;
                    } else {
                        supTableList.remove(hfSupTableFormPojo);
                        break;
                    }
                }
            }
        }

        // 用于匹配数量和前三条等信息
        List<HfSupTableFormPojo> supTableFormList = workingTableMapper.getSupTableFormListDao(null, userId, paramNameLike);
        // 用于查询标签中文名
        List<LabelValuePojo> supLabelListDao = workingTableMapper.getSupLabelListDao(userId);

        for (HfSupTableFormPojo hfSupTableFormPojo : supTableList) {
            // str转对象
            String columns = String.valueOf(hfSupTableFormPojo.getColumns());
            hfSupTableFormPojo.setColumns(JSON.parseObject(columns, List.class));
            // str转布尔
            String enabled = String.valueOf(hfSupTableFormPojo.getEnabled());
            hfSupTableFormPojo.setEnabled("true".equals(enabled) ? true : false);

            // 标签中文名
            List<String> labelNameList = new ArrayList<>();
            List<String> labelList = JSON.parseObject((String) hfSupTableFormPojo.getLabel(), List.class);
            if (labelList != null && labelList.size() != 0) {
                for (String s : labelList) {
                    for (LabelValuePojo labelValuePojo : supLabelListDao) {
                        if (s.equals(labelValuePojo.getValue())) {
                            labelNameList.add(labelValuePojo.getLabel());
                            break;
                        }
                    }
                }
                hfSupTableFormPojo.setLabel(labelList);
                hfSupTableFormPojo.setLabelNameList(labelNameList);
            }

            // 补充最近三条展示数据
            String tableId = hfSupTableFormPojo.getTableId();
            hfSupTableFormPojo.setRecentlyForm(new ArrayList<>());
            if (hfSupTableFormPojo.getRecentlyForm().size() < 3) {
                for (HfSupTableFormPojo supTableFormPojo : supTableFormList) {
                    if (tableId.equals(supTableFormPojo.getTableId()) && hfSupTableFormPojo.getRecentlyForm().size() < 3) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("formName", supTableFormPojo.getFormName());
                        map.put("createTime", new TimeFormatUtils().getAgo(supTableFormPojo.getCreateTime()));
                        hfSupTableFormPojo.getRecentlyForm().add(map);
                    }
                }
            }
        }

        // str转int
        for (HfSupTableFormPojo hfSupTableFormPojo : supTableList) {
            String useCnt = (String) hfSupTableFormPojo.getUseCnt();
            hfSupTableFormPojo.setUseCnt(Integer.valueOf(useCnt));
        }


        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(supTableList);
        }


        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(supTableList, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", supTableList.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 新建辅助工具数据
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newSupTableFormService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception {
        // 分配id
        String formId = UUID.randomUUID().toString().replaceAll("-", "");
        hfSupTableFormPojo.setFormId(formId);
        // 对象转str
        Object formValue = hfSupTableFormPojo.getFormValue();
        String formValueStr = JSON.toJSONString(formValue);
        hfSupTableFormPojo.setFormValue(formValueStr);
        // 入库
        workingTableMapper.newSupTableFormDao(hfSupTableFormPojo);
        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("info", "创建成功");
        retMap.put("formId", formId);
        return TResponseVo.success(retMap);
    }


    /**
     * 删除辅助工具数据
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delSupTableFormService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception {
        String formId = hfSupTableFormPojo.getFormId();
        workingTableMapper.delSupTableFormDao(formId, null);
        return TResponseVo.success("删除成功");
    }


    /**
     * 修改辅助工具数据
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateSupTableFormService(HfSupTableFormPojo hfSupTableFormPojo) throws Exception {
        // 对象转str
        Object formValue = hfSupTableFormPojo.getFormValue();
        String formValueStr = JSON.toJSONString(formValue);
        hfSupTableFormPojo.setFormValue(formValueStr);
        // 更新库
        workingTableMapper.updateSupTableFormDao(hfSupTableFormPojo);
        return TResponseVo.success("更新成功");
    }

    /**
     * 查询辅助工具数据
     *
     * @param hfSupTableFormPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getSupTableFormInfoService(HfSupTableFormPojo hfSupTableFormPojo, String userId) throws Exception {
        String formId = hfSupTableFormPojo.getFormId();
        HfSupTableFormPojo supTableFormInfoDao = workingTableMapper.getSupTableFormInfoDao(formId);
        // str转对象
        String formValueStr = String.valueOf(supTableFormInfoDao.getFormValue());
        supTableFormInfoDao.setFormValue(JSON.parseObject(formValueStr, Map.class));
        return TResponseVo.success(supTableFormInfoDao);
    }

    /**
     * 查询辅助工具数据列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getSupTableFormListService(PagePojo pagePojo, String userId) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        String tableId = pagePojo.getTableId();
        List<HfSupTableFormPojo> supTableFormList = workingTableMapper.getSupTableFormListDao(tableId, userId, paramNameLike);

        for (HfSupTableFormPojo hfSupTableFormPojo : supTableFormList) {
            // str转对象
            String formValueStr = String.valueOf(hfSupTableFormPojo.getFormValue());
            hfSupTableFormPojo.setFormValue(JSON.parseObject(formValueStr, Map.class));
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(supTableFormList);
        }


        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(supTableFormList, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", supTableFormList.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 辅助工具数据下载excel
     *
     * @param tableId@return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo downloadSupTableExcelService(String tableId, List<String> formIdList, HttpServletResponse response) throws Exception {
        HfSupTableFormPojo supTableInfoDao = workingTableMapper.getSupTableInfoDao(tableId);

        String columns = String.valueOf(supTableInfoDao.getColumns());
        List<Map<String, String>> columnsList = JSON.parseObject(columns, List.class);
        List<String> dataIndexList = new ArrayList<>(); // 存dataIndex
        List<String> dataTypeIdList = new ArrayList<>(); // 存dataIndex
        List<String> titleList = new ArrayList<>(); // 存dataIndex
        List<String> aliasList = new ArrayList<>(); // 存dataIndex
        List<String> unitIdList = new ArrayList<>(); // 存dataIndex
        for (Map<String, String> map : columnsList) {
            dataIndexList.add(map.get("dataIndex"));
            dataTypeIdList.add(map.get("dataTypeId"));
            titleList.add(map.get("title"));
            aliasList.add(map.get("alias"));
            unitIdList.add(map.get("unitId"));
        }

        String formIdListStr;
        if (formIdList == null) {
            formIdListStr = null;
        } else {
            formIdListStr = new JsonKeyUtils().listToString(formIdList, "','");
        }
        List<HfSupTableFormPojo> supTableFormListByformIdDao = workingTableMapper.getSupTableFormListByformIdDao(tableId, formIdListStr);
        ArrayList<Map<String, Object>> dataList = new ArrayList<>();
        for (HfSupTableFormPojo h : supTableFormListByformIdDao) {
            String formValue = String.valueOf(h.getFormValue());
            String formName = h.getFormName();
            Map<String, Object> formValueMap = JSON.parseObject(formValue, Map.class);  // 原数据
            // 将原数据处理成与dataIndex对应顺序的list数据
            ArrayList<Object> valueList = new ArrayList<>();
            for (String dataIndex : dataIndexList) {
                valueList.add(formValueMap.get(dataIndex));
            }

            HashMap<String, Object> map = new HashMap<>();
            map.put("formName", formName);
            map.put("valueList", valueList);
            dataList.add(map);
        }
        new ExcelUtils().writeExcel(dataIndexList, dataTypeIdList, titleList, aliasList, unitIdList, dataList, 1, response);


        return TResponseVo.success("下载完成");
    }

    /**
     * 辅助工具上传excel
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo uploadSupTableExcelService(MultipartFile file, String userId) throws Exception {
        if (file.isEmpty()) {
            throw new WorkTableException("文件上传失败");
        }
        String oriFileName = file.getOriginalFilename();
        String extension = oriFileName.substring(oriFileName.lastIndexOf("."));
        if (!extension.equals(".xls")) {
            throw new WorkTableException("文件上传错误,目前只支持xls的文档");
        }
        // hf_sup_table表
        InputStream inputStream = file.getInputStream();
        List<List<String>> dataIndexByExcel = new ArrayList<>();
        try {
            dataIndexByExcel = new ExcelUtils().getListByExcel(inputStream, oriFileName, 1);
        } catch (Exception e) {
            throw new WorkTableException("上传的文件不是系统的模型文件,请检查选择的文件是否正确");
        }
        // 校验dataIndex重复性
        if (checkStrRepeat(dataIndexByExcel.get(0)) || checkStrRepeat(dataIndexByExcel.get(2))) { // dataIndex和title 各自都不要有重复项
//            return TResponseVo.error("模型中的字段名(主键)不允许重复");
            throw new WorkTableException("模型中的字段名(主键)不允许重复");
        }

        ArrayList<Map<String, String>> columnsList = new ArrayList<>();
        HashMap<String, String> dataIndexAndType = new HashMap<>();
        for (int i = 0; i < dataIndexByExcel.get(0).size(); i++) {
            HashMap<String, String> column = new HashMap<>();
            column.put("dataIndex", StringEscapeUtils.escapeSql(dataIndexByExcel.get(0).get(i)));  // 防注入
            column.put("dataTypeId", StringEscapeUtils.escapeSql(dataIndexByExcel.get(1).get(i)));
            dataIndexAndType.put(StringEscapeUtils.escapeSql(dataIndexByExcel.get(0).get(i)), StringEscapeUtils.escapeSql(dataIndexByExcel.get(1).get(i)));  // dataIndex 对应 dataType
            column.put("title", StringEscapeUtils.escapeSql(dataIndexByExcel.get(2).get(i)));
            try {
                column.put("alias", dataIndexByExcel.get(3).get(i));
            } catch (Exception e) {
            }
            try {
                column.put("unitId", dataIndexByExcel.get(4).get(i));
            } catch (Exception e) {
            }
            columnsList.add(column);
        }
        HfSupTableFormPojo hfSupTableFormPojo = new HfSupTableFormPojo();
        String tableId = UUID.randomUUID().toString().replaceAll("-", "");
        hfSupTableFormPojo.setTableId(tableId);
        hfSupTableFormPojo.setTitle("新导入模型");
        hfSupTableFormPojo.setEnabled("false");
        hfSupTableFormPojo.setColumns(JSON.toJSONString(columnsList));
        hfSupTableFormPojo.setCreateUserId(userId);
        workingTableMapper.newSupTableDao(hfSupTableFormPojo);

        // hf_sup_table_form表
        inputStream = file.getInputStream();
        List<List<String>> dataListByExcel = new ExcelUtils().getListByExcel(inputStream, oriFileName, 0);
        System.out.println("dataListByExcel : " + JSON.toJSONString(dataListByExcel));
//        String sql = "";
        // sql
        for (int i = 1; i < dataListByExcel.size(); i++) {
            HashMap<String, Object> map = new HashMap<>();
            List<String> dataIndexList = dataListByExcel.get(0);
            List<String> data = dataListByExcel.get(i);
            for (int j = 0; j < dataIndexList.size(); j++) {
                String dataIndex = dataIndexList.get(j);
                if ("实例名称".equals(dataIndex)) {
                    continue;
                }
                try {
                    String dataType = dataIndexAndType.get(dataIndex);
                    if ("20".equals(dataType)) {  // 多选
                        map.put(dataIndex, JSON.parseObject(String.valueOf(data.get(j)), List.class));
                    } else if ("80".equals(dataType) || "90".equals(dataType)) { // 数值 金额
                        map.put(dataIndex, data.get(j));  // 防注入
                    } else if ("60".equals(dataType) || "70".equals(dataType)) { // 图片 附件
                        map.put(dataIndex, JSON.parseObject(String.valueOf(data.get(j)), List.class));
                    } else {
                        map.put(dataIndex, data.get(j));  // 防注入
                    }
                } catch (IndexOutOfBoundsException e) {
                    map.put(dataIndex, "");
                }
            }
            String formId = UUID.randomUUID().toString().replaceAll("-", "");
//            sql = sql + "('" + formId + "','" + data.get(0) + "','" + JSON.toJSONString(map) + "','" + tableId + "','" + userId + "'),";

            hfSupTableFormPojo.setFormId(formId);
            hfSupTableFormPojo.setFormName(data.get(0));
            hfSupTableFormPojo.setFormValue(JSON.toJSONString(map));
            workingTableMapper.newSupTableFormDao(hfSupTableFormPojo);
        }

//        if (sql.length() > 5) {
//            sql = sql.substring(0, sql.length() - 1);
//            workingTableMapper.uploadSupTableExcelDao(sql);
//        }
        return TResponseVo.success("上传完成");
    }

    /**
     * 辅助工具自动填写
     *
     * @param fileUuid
     * @param fileVersionId
     * @param userId
     * @param formId
     */
    @Override
    public TResponseVo supAutoWriteService(String fileUuid, String fileVersionId, String userId, String formId) throws Exception {
        // 设置double格式
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setGroupingUsed(false);  // 不使用科学计数法
        nf.setMaximumFractionDigits(13);  // 允许的最大小数位数

        // 获取单位进制维表
        List<DocframeParamsUnitDiPojo> unitDiDao = fileOperationMapper.getUnitDiDao();


        // 根据formId获取tableId 获取参数类型与别名
        HfSupTableFormPojo supTableFormInfoDao = workingTableMapper.getSupTableFormInfoDao(formId);
        if (supTableFormInfoDao == null) {
            return TResponseVo.error("该实例已被删除");
        }
        String tableId = supTableFormInfoDao.getTableId();
        String formValue = String.valueOf(supTableFormInfoDao.getFormValue());
        Map<String, Object> formValueMap = JSON.parseObject(formValue, Map.class);
        HfSupTableFormPojo supTableInfoDao = workingTableMapper.getSupTableInfoDao(tableId);
        // 使用次数+1
        Integer useCnt = Integer.valueOf((String) supTableInfoDao.getUseCnt());
        useCnt = useCnt + 1;
        workingTableMapper.updateUseCntDao(tableId, String.valueOf(useCnt));

        // 更新最近使用的辅助工具
        List<String> list = supTableRecent.get(userId);
        if (list == null) {
            list = new ArrayList<>();
        }
        if (list.size() >= 11) {
            list.remove(list.size() - 1);
        }
        if (!list.contains(tableId)) {
            list.add(0, tableId);
        }
        supTableRecent.put(userId, list);

        String columns = String.valueOf(supTableInfoDao.getColumns());
        List<Map<String, Object>> columnsList = JSON.parseObject(columns, List.class);
        // 把form中的填值拿过来 拼出完整数据
        for (Map<String, Object> columnMap : columnsList) {
            String dataIndex = String.valueOf(columnMap.get("dataIndex"));
            columnMap.put("value", formValueMap.get(dataIndex));
        }

        List<DocParamsPojo> paramsPojosList = workingTableMapper.getSupParamDataDao(fileUuid, fileVersionId);  // 获取应用到文内的所有参数列表
        // 根据参数名判断是否需要自动填写
        ArrayList<DocParamsPojo> paramsPojosListTmp = new ArrayList<>();
        paramsPojosListTmp.addAll(paramsPojosList);
        HashMap<String, Object> retMap = new HashMap<>();  // 用来返回被修改掉的数据
        List<DocParamsPojo> paramsPojoList = new ArrayList<>();  // 用来返回被修改掉的数据
        HashMap<String, Object> typeWrongMap = new HashMap<>();  // 用来返回类型不一致而放弃填写的数据
        for (DocParamsPojo params : paramsPojosListTmp) {  // 遍历文内参数 判断是否需要填写
            Boolean titleFlag = false;  // 是否匹配到title
            String paramsName = params.getParamsName();
            for (Map<String, Object> columnsMap : columnsList) {
                String title = String.valueOf(columnsMap.get("title"));
                if (paramsName.equals(title)) {  // 如果参数名等于辅助工具字段名
                    titleFlag = true;
                    // 再判断类型是否相同
                    String dataTypeId = String.valueOf(columnsMap.get("dataTypeId"));
                    if (params.getParamsTypeId().equals(dataTypeId)) {
                        // 如果相同就填入
                        String paramsText = columnsMap.get("value") == null ? "" : String.valueOf(columnsMap.get("value"));
                        String paramsUuid = params.getParamsUuid();
                        DocParamsPojo docParamsPojo = new DocParamsPojo();
                        docParamsPojo.setFileUuid(fileUuid);
                        docParamsPojo.setFileVersionId(fileVersionId);
                        docParamsPojo.setParamsUuid(paramsUuid);
                        docParamsPojo.setParamsText(paramsText);
                        docParamsPojo.setUserId(userId);

                        List<DocParamsPojo> docParamsPojos = fileOperationMapper.getContentAllParamDao(docParamsPojo);

                        // 第一步 更新参数表
                        Integer integer = 0;  // 记录更新的条目数


                        if (dataTypeId.equals("40") || dataTypeId.equals("30")) {  // 文本 时间
                            // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段
                            retMap.put(params.getParamsUuid(), paramsText);
                            params.setParamsText(paramsText);
                            params.setParamsTypeName(dataTypeId.equals("40") ? "文本" : "时间");
                            paramsPojoList.add(params);
                        } else if (dataTypeId.equals("80")) {  // 金额
                            String unitId = String.valueOf(columnsMap.get("unitId"));  // 确定辅助工具的单位进制尺
                            Long rule = 1L;
                            for (DocframeParamsUnitDiPojo unitDiPojo : unitDiDao) {
                                if (unitDiPojo.getUnitId().equals(unitId)) {
                                    rule = Long.valueOf(unitDiPojo.getRule());
                                }
                            }

                            Map<String, Object> paramsTextList = new HashMap<>();
                            List<Map<String, String>> unitList = new ArrayList<>();
                            List<String> unitIdList = new ArrayList<>();
                            for (DocParamsPojo paramsPojo : docParamsPojos) {
                                if (paramsUuid.equals(paramsPojo.getParamsUuid())) {
                                    String unit = paramsPojo.getUnit();
                                    for (DocframeParamsUnitDiPojo unitDiPojo : unitDiDao) {
                                        if (unitDiPojo.getUnitId().equals(unit)) {
                                            try {  // 防止未填写
                                                BigDecimal value1 = new BigDecimal(paramsText);
                                                BigDecimal rule1 = new BigDecimal(rule);
                                                BigDecimal tagRule1 = new BigDecimal(unitDiPojo.getRule());
                                                BigDecimal multiply = value1.multiply(tagRule1).divide(rule1, 13, BigDecimal.ROUND_HALF_UP);
                                                paramsTextList.put(unit, Double.valueOf(multiply.stripTrailingZeros().toPlainString()));
                                                if (!unitIdList.contains(unit)) {
                                                    Map<String, String> map = new HashMap<>();
                                                    map.put("label", unitDiPojo.getUnitName());
                                                    map.put("value", unit);
                                                    unitList.add(map);
                                                    unitIdList.add(unit);
                                                }
                                                docParamsPojo.setParamsTextList(paramsTextList);
                                            } catch (Exception e) {
                                                paramsTextList.put(unit, null);
                                                docParamsPojo.setParamsTextList(paramsTextList);
                                            }
                                        }
                                    }
                                }
                            }

                            HashMap<String, Object> map = new HashMap<>();
                            for (String uninId : paramsTextList.keySet()) {
                                map.put(uninId, paramsTextList.get(uninId));
                            }
                            retMap.put(params.getParamsUuid(), map);
                            params.setParamsText(map);
                            params.setUnitList(unitList);
                            params.setParamsTypeName("金额");
                            paramsPojoList.add(params);
                        } else if (dataTypeId.equals("90")) {  // 数值
                            String unitId = String.valueOf(columnsMap.get("unitId"));  // 确定单位进制尺
                            Long rule = 1L;
                            for (DocframeParamsUnitDiPojo unitDiPojo : unitDiDao) {
                                if (unitDiPojo.getUnitId().equals(unitId)) {
                                    rule = Long.valueOf(unitDiPojo.getRule());
                                }
                            }

                            Map<String, Object> paramsTextList = new HashMap<>();
                            List<Map<String, String>> unitList = new ArrayList<>();
                            List<String> unitIdList = new ArrayList<>();
                            for (DocParamsPojo paramsPojo : docParamsPojos) {
                                if (paramsUuid.equals(paramsPojo.getParamsUuid())) {
                                    String unit = paramsPojo.getUnit();
                                    for (DocframeParamsUnitDiPojo unitDiPojo : unitDiDao) {
                                        if (unitDiPojo.getUnitId().equals(unit)) {
                                            try {  // 防止未填写
                                                BigDecimal value1 = new BigDecimal(paramsText);
                                                BigDecimal rule1 = new BigDecimal(rule);
                                                BigDecimal tagRule1 = new BigDecimal(unitDiPojo.getRule());
                                                BigDecimal multiply = value1.multiply(tagRule1).divide(rule1, 13, BigDecimal.ROUND_HALF_UP);
                                                paramsTextList.put(unit, Double.valueOf(multiply.stripTrailingZeros().toPlainString()));
                                                if (!unitIdList.contains(unit)) {
                                                    Map<String, String> map = new HashMap<>();
                                                    map.put("label", unitDiPojo.getUnitName());
                                                    map.put("value", unit);
                                                    unitList.add(map);
                                                    unitIdList.add(unit);
                                                }
                                                docParamsPojo.setParamsTextList(paramsTextList);
                                            } catch (Exception e) {
                                                paramsTextList.put(unit, null);
                                                docParamsPojo.setParamsTextList(paramsTextList);
                                            }
                                        }
                                    }
                                }
                            }

                            HashMap<String, Object> map = new HashMap<>();
                            for (String uninId : paramsTextList.keySet()) {
                                map.put(uninId, paramsTextList.get(uninId));
                            }
                            retMap.put(params.getParamsUuid(), map);
                            params.setParamsText(map);
                            params.setUnitList(unitList);
                            params.setParamsTypeName("数值");
                            paramsPojoList.add(params);
                        } else if (dataTypeId.equals("50")) {  // 富文本
                            retMap.put(params.getParamsUuid(), paramsText);
                            params.setParamsText(paramsText);
                            params.setParamsTypeName("富文本");
                            paramsPojoList.add(params);
                        }
                    } else {
                        break;  // 参数名对上了 但是类型对不上 直接pass
                    }
                }
            }
            if (!titleFlag) {  // 如果没有匹配到title 再匹配一遍alias 参数匹配标识
                for (Map<String, Object> columnsMap : columnsList) {
                    String alias = String.valueOf(columnsMap.get("alias"));
                    if (paramsName.equals(alias)) {  // 如果参数名等于别名段名
                        // TODO 把逻辑复制过来
                    }
                }
            }
        }

        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "辅助填写完成");
//        ret.put("retList", retMap);
//        ret.put("paramsPojoList", paramsPojoList);
        ret.put("retList", JSON.toJSON(retMap));
        ret.put("paramsPojoList", JSON.toJSON(paramsPojoList));
        return TResponseVo.success(ret);
    }

    /**
     * 辅助工具自动填写-确认填写
     */
    @Override
    public TResponseVo supAutoWriteSureService(String fileUuid, String fileVersionId, String userId, Map<String, Object> mapParams) throws Exception {
        // 判断是否正在协同编辑
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (redisUtils.hasKey(roomKey)) {
            throw new WorkTableException("本文正在协同编辑，请前往协同编辑中填参。");
        }

        // 将mapParams转换成list
        ArrayList<DocParamsPojo> list = new ArrayList<>();
        for (String paramsUuid : mapParams.keySet()) {
            DocParamsPojo paramsPojo = new DocParamsPojo();
            paramsPojo.setParamsUuid(paramsUuid);
            paramsPojo.setParamsText(mapParams.get(paramsUuid));
            list.add(paramsPojo);
        }

        // 设置double格式
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setGroupingUsed(false);  // 不使用科学计数法
        nf.setMaximumFractionDigits(13);  // 允许的最大小数位数

        // 获取单位进制维表
        List<DocframeParamsUnitDiPojo> unitDiDao = fileOperationMapper.getUnitDiDao();


        List<DocParamsPojo> paramsPojosList = workingTableMapper.getSupParamDataDao(fileUuid, fileVersionId);  // 获取应用到文内的所有参数列表
        // 根据参数名判断是否需要自动填写
        ArrayList<DocParamsPojo> paramsPojosListTmp = new ArrayList<>();
        paramsPojosListTmp.addAll(paramsPojosList);
        HashMap<String, Object> retMap = new HashMap<>();  // 用来返回被修改掉的数据
        List<DocParamsPojo> paramsPojoList = new ArrayList<>();  // 用来返回被修改掉的数据
        HashMap<String, Object> typeWrongMap = new HashMap<>();  // 用来返回类型不一致而放弃填写的数据

        for (DocParamsPojo params : paramsPojosListTmp) {  // 遍历文内参数 判断是否需要填写
            for (DocParamsPojo newParam : list) {
                // 找到ParamsUuid相同的说明要填写
                if (params.getParamsUuid().equals(newParam.getParamsUuid())) {
//                    if (!params.getParamsTypeId().equals(newParam.getParamsTypeId())) {
//                        // 验证参数类型是否一致
//                        break;
//                    }
                    String dataTypeId = params.getParamsTypeId();

                    // 如果相同就填入
                    String paramsUuid = newParam.getParamsUuid();
                    DocParamsPojo docParamsPojo = new DocParamsPojo();
                    docParamsPojo.setFileUuid(fileUuid);
                    docParamsPojo.setFileVersionId(fileVersionId);
                    docParamsPojo.setParamsUuid(paramsUuid);
                    docParamsPojo.setUserId(userId);

                    List<DocParamsPojo> docParamsPojos = fileOperationMapper.getContentAllParamDao(docParamsPojo);

//                    Object paramsText = paramsPojo.getParamsText();
//                    if (paramsText instanceof String) {
//
//                    } else if (paramsText instanceof List) {
//                    }
//                    params.getUnit()
                    // 第一步 更新参数表
                    Integer integer = 0;  // 记录更新的条目数
                    if (dataTypeId.equals("40") || dataTypeId.equals("30")) {  // 文本 时间
                        // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段
                        String paramsText = newParam.getParamsText() == null ? "" : String.valueOf(newParam.getParamsText());
                        String sql = "";
                        for (DocParamsPojo paramsPojo : docParamsPojos) {
                            sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                        }
                        if (sql.length() != 0) {
                            sql = sql.substring(0, sql.length() - 3);
                            integer = fileOperationMapper.writeParamDao(paramsText, docParamsPojo.getUserId(), sql);
                        }
                        retMap.put(params.getParamsUuid(), paramsText);

                        params.setParamsText(paramsText);
                        params.setParamsTypeName(dataTypeId.equals("40") ? "文本" : "时间");
                        paramsPojoList.add(params);
                    } else if (dataTypeId.equals("80")) {  // 金额
                        Map<String, String> paramsTextList = (Map) newParam.getParamsText();
                        integer = fileOperationMapper.setParamNullDao(fileUuid, fileVersionId, paramsUuid);
                        HashMap<String, Object> map = new HashMap<>();
                        List<Map<String, String>> unitList = new ArrayList<>();
                        List<String> unitIdList = new ArrayList<>();
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
                            if (sql.length() != 0) {
                                sql = sql.substring(0, sql.length() - 3);
                                integer += fileOperationMapper.writeParamDao(paramsTextList.get(uninId) == null ? null : nf.format(paramsTextList.get(uninId)), docParamsPojo.getUserId(), sql);
                            }
                            map.put(uninId, paramsTextList.get(uninId));
                            if (!unitIdList.contains(uninId)) {
                                for (DocframeParamsUnitDiPojo unitDiPojo : unitDiDao) {
                                    if (unitDiPojo.getUnitId().equals(uninId)) {
                                        Map<String, String> unitMap = new HashMap<>();
                                        unitMap.put("label", unitDiPojo.getUnitName());
                                        unitMap.put("value", uninId);
                                        unitList.add(unitMap);
                                        unitIdList.add(uninId);
                                    }
                                }
                            }
                        }
                        retMap.put(params.getParamsUuid(), map);

                        params.setParamsText(map);
                        params.setUnitList(unitList);
                        params.setParamsTypeName("金额");
                        paramsPojoList.add(params);
                    } else if (dataTypeId.equals("90")) {  // 数值
                        Map<String, String> paramsTextList = (Map) newParam.getParamsText();
                        integer = fileOperationMapper.setParamNullDao(fileUuid, fileVersionId, paramsUuid);
                        HashMap<String, Object> map = new HashMap<>();
                        List<Map<String, String>> unitList = new ArrayList<>();
                        List<String> unitIdList = new ArrayList<>();
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
                            if (sql.length() != 0) {
                                sql = sql.substring(0, sql.length() - 3);
                                integer += fileOperationMapper.writeParamDao(paramsTextList.get(uninId) == null ? null : nf.format(paramsTextList.get(uninId)), docParamsPojo.getUserId(), sql);
                            }
                            map.put(uninId, paramsTextList.get(uninId));
                            if (!unitIdList.contains(uninId)) {
                                for (DocframeParamsUnitDiPojo unitDiPojo : unitDiDao) {
                                    if (unitDiPojo.getUnitId().equals(uninId)) {
                                        Map<String, String> unitMap = new HashMap<>();
                                        unitMap.put("label", unitDiPojo.getUnitName());
                                        unitMap.put("value", uninId);
                                        unitList.add(unitMap);
                                        unitIdList.add(uninId);
                                    }
                                }
                            }
                        }
                        retMap.put(params.getParamsUuid(), map);
                        params.setParamsText(map);
                        params.setUnitList(unitList);
                        params.setParamsTypeName("数值");
                        paramsPojoList.add(params);
                    } else if (dataTypeId.equals("50")) {  // 富文本
                        // 根据联合主键`uuid`,`file_uuid`,`file_version_id` 更新params_text字段
                        String paramsText = newParam.getParamsText() == null ? "" : String.valueOf(newParam.getParamsText());
                        String sql = "";
                        for (DocParamsPojo paramsPojo : docParamsPojos) {
                            sql = sql + "(uuid=\"" + paramsPojo.getUuid() + "\" and file_uuid=\"" + paramsPojo.getFileUuid() + "\" and file_version_id=\"" + paramsPojo.getFileVersionId() + "\") or ";
                        }
                        if (sql.length() != 0) {
                            sql = sql.substring(0, sql.length() - 3);
                            integer = fileOperationMapper.writeParamDao(paramsText, docParamsPojo.getUserId(), sql);
                        }
                        retMap.put(params.getParamsUuid(), paramsText);
                        params.setParamsText(paramsText);
                        params.setParamsTypeName("富文本");
                        paramsPojoList.add(params);
                    }


                    // 第二步 更新文内参数
                    if (integer != 0) {  // 说明触发了更新 不然则未触发更新
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
                            ArrayList<Map<String, Object>> newParamsTextList = new ArrayList<>();
                            for (Element element : elementsTmp) {  // elementsTmp的size表示段落中要更新几处
                                if (dataTypeId.equals("40")) {  // 文本参数没有格式 直接更新至文本中
                                    String paramsText = newParam.getParamsText() == null ? "" : String.valueOf(newParam.getParamsText());
                                    String newParamsText = null;
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
                                } else if (dataTypeId.equals("30")) {  // 时间格式 需要匹配样式
                                    String paramsText = newParam.getParamsText() == null ? "" : String.valueOf(newParam.getParamsText());
                                    String newParamsText = null;
                                    for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                        if (element.attr("uuid").equals(pojo.getUuid())) {
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
                                } else if (dataTypeId.equals("80")) {  // 金额 匹配样式
                                    String newParamsText = "";
                                    for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                        if (element.attr("uuid").equals(pojo.getUuid())) {
                                            element.removeAttr("value");
                                            // 根据当前单位来判断填写值
                                            String paramsText = null;
                                            Map<String, Object> paramsTextList = (Map) newParam.getParamsText();
                                            for (String uninId : paramsTextList.keySet()) {
                                                if (uninId.equals(pojo.getUnit())) {
                                                    paramsText = paramsTextList.get(uninId) == null ? null : nf.format(paramsTextList.get(uninId));
                                                    break;
                                                }
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
                                } else if (dataTypeId.equals("90")) {  // 数值 匹配样式
                                    String newParamsText = "";
                                    String unitName = "";
                                    for (DocParamsPojo pojo : docParamsPojos) {  // 按照样式调整显示文本
                                        if (element.attr("uuid").equals(pojo.getUuid())) {
                                            element.removeAttr("value");
                                            // 根据当前单位来判断填写值
                                            String paramsText = null;
                                            Map<String, Object> paramsTextList = (Map) newParam.getParamsText();
                                            for (String uninId : paramsTextList.keySet()) {
                                                if (uninId.equals(pojo.getUnit())) {
                                                    paramsText = paramsTextList.get(uninId) == null ? null : nf.format(paramsTextList.get(uninId));
                                                    break;
                                                }
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
                                } else if (dataTypeId.equals("50")) {  // 富文本
                                    String paramsText = newParam.getParamsText() == null ? "" : String.valueOf(newParam.getParamsText());
                                    String newParamsText = null;
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
                                }
                            }
                            contentText = doc.toString().replaceAll("\\s*\\n\\s*", "");
                            contentText = contentText.replaceAll("</br>", "");
                            paramsPojo.setContentText(contentText);

                            // 搜索模式下
                            String searchUuid = docParamsPojo.getSearchUuid();
                            String outlineId = paramsPojo.getOutlineId();
                            if (searchUuid != null) {
                                // 更新后入库前更新一下缓存
                                if (searchUuid != null && !searchUuid.equals("") && redisUtils.hasKey("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId)) {
                                    redisUtils.set("search" + userId + fileUuid + fileVersionId + searchUuid + outlineId, contentText, 1800);
                                }
                            }
                            fileOperationMapper.writeContentParamDao(paramsPojo);
                        }
                    }

                }
            }
        }
//        if (!titleFlag) {  // 如果没有匹配到title 再匹配一遍alias 参数匹配标识
//            for (Map<String, Object> columnsMap : columnsList) {
//                String alias = String.valueOf(columnsMap.get("alias"));
//                if (paramsName.equals(alias)) {  // 如果参数名等于别名段名
//                    // TODO 把逻辑复制过来
//                }
//            }
//        }
        // 记录填写历史
        HfSupTableUseHistoryPojo hfSupTableUseHistory = new HfSupTableUseHistoryPojo();
        hfSupTableUseHistory.setUuid(UUID.randomUUID().toString().replaceAll("-", ""));
        hfSupTableUseHistory.setFileUuid(fileUuid);
        hfSupTableUseHistory.setFileVersionId(fileVersionId);
        hfSupTableUseHistory.setUserId(userId);
        hfSupTableUseHistory.setContent(JSON.toJSONString(paramsPojoList));
        fileOperationMapper.addSupTableUseHistoryDao(hfSupTableUseHistory);

        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "辅助填写完成");
        ret.put("retList", retMap);
        ret.put("paramsPojoList", paramsPojoList);
        return TResponseVo.success(ret);
    }

    /**
     * 获取辅助工具填写历史列表
     *
     * @param fileUuid
     * @param fileVersionId
     */
    @Override
    public TResponseVo getSupTableHistoryListService(String fileUuid, String fileVersionId) throws Exception {
        List<HfSupTableUseHistoryPojo> supTableHistoryListDao = fileOperationMapper.getSupTableHistoryListDao(fileUuid, fileVersionId);
        for (HfSupTableUseHistoryPojo hfSupTableUseHistoryPojo : supTableHistoryListDao) {
            String content = (String) hfSupTableUseHistoryPojo.getContent();
            List list = JSON.parseObject(content, List.class);
            hfSupTableUseHistoryPojo.setContent(list);
        }
        return TResponseVo.success(supTableHistoryListDao);
    }


    /**
     * 获取辅助工具填写历史详情
     *
     * @param userId
     */
    @Override
    public TResponseVo getSupTableRecentService(String userId) throws Exception {
        List<String> list = supTableRecent.get(userId);
        if (list == null) {
            list = new ArrayList<>();
        }

        List<HfSupTableFormPojo> supTableList = workingTableMapper.getSupTableListDao(userId, null, "true");

        // 用于匹配数量和前三条等信息
        List<HfSupTableFormPojo> supTableFormList = workingTableMapper.getSupTableFormListDao(null, userId, null);
        // 用于查询标签中文名
        List<LabelValuePojo> supLabelListDao = workingTableMapper.getSupLabelListDao(userId);

        List<HfSupTableFormPojo> supTableListTmp = new ArrayList<>();
        for (String tableId : list) {
            for (HfSupTableFormPojo hfSupTableFormPojo : supTableList) {
                if (tableId.equals(hfSupTableFormPojo.getTableId())) {
                    supTableListTmp.add(hfSupTableFormPojo);
                }
            }
        }
        supTableList = supTableListTmp;


        for (HfSupTableFormPojo hfSupTableFormPojo : supTableList) {
            // str转对象
            String columns = String.valueOf(hfSupTableFormPojo.getColumns());
            hfSupTableFormPojo.setColumns(JSON.parseObject(columns, List.class));
            // str转布尔
            String enabled = String.valueOf(hfSupTableFormPojo.getEnabled());
            hfSupTableFormPojo.setEnabled("true".equals(enabled) ? true : false);

            // 标签中文名
            List<String> labelNameList = new ArrayList<>();
            List<String> labelList = JSON.parseObject((String) hfSupTableFormPojo.getLabel(), List.class);
            if (labelList != null && labelList.size() != 0) {
                for (String s : labelList) {
                    for (LabelValuePojo labelValuePojo : supLabelListDao) {
                        if (s.equals(labelValuePojo.getValue())) {
                            labelNameList.add(labelValuePojo.getLabel());
                            break;
                        }
                    }
                }
                hfSupTableFormPojo.setLabel(labelList);
                hfSupTableFormPojo.setLabelNameList(labelNameList);
            }

            // 补充最近三条展示数据
            String tableId = hfSupTableFormPojo.getTableId();
            hfSupTableFormPojo.setRecentlyForm(new ArrayList<>());
            if (hfSupTableFormPojo.getRecentlyForm().size() < 3) {
                for (HfSupTableFormPojo supTableFormPojo : supTableFormList) {
                    if (tableId.equals(supTableFormPojo.getTableId()) && hfSupTableFormPojo.getRecentlyForm().size() < 3) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("formName", supTableFormPojo.getFormName());
                        map.put("createTime", new TimeFormatUtils().getAgo(supTableFormPojo.getCreateTime()));
                        hfSupTableFormPojo.getRecentlyForm().add(map);
                    }
                }
            }
        }

        return TResponseVo.success(supTableList);
    }

    /**
     * 获取辅助工具及实例级联树
     */
    @Override
    public TResponseVo getSupTableTreeService(String userId) throws Exception {
        List<LabelValuePojo> getSupTableListLVDao = workingTableMapper.getSupTableListLVDao(userId);
        List<LabelValuePojo> getSupTableFormListLVDao = workingTableMapper.getSupTableFormListLVDao(userId);
        for (LabelValuePojo table : getSupTableListLVDao) {
            String tableId = table.getValue();
            for (LabelValuePojo form : getSupTableFormListLVDao) {
                if (form.getTableId().equals(tableId)) {
                    if (table.getChildren() == null) {
                        ArrayList<LabelValuePojo> labelValuePojos = new ArrayList<>();
                        labelValuePojos.add(form);
                        table.setChildren(labelValuePojos);
                    } else {
                        table.getChildren().add(form);
                    }
                }
            }

        }
        return TResponseVo.success(getSupTableListLVDao);
    }

    /**
     * 获取文件类型清单
     *
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo getFileTypeDiService() throws Exception {
        List<HfFileTypeDiPojo> fileTypeDiDao = workingTableMapper.getFileTypeDiDao("ALL");
        ArrayList<HashMap<String, Object>> resultList = new ArrayList<>();
        ArrayList<String> typeGroupIdList = new ArrayList<>();
        for (HfFileTypeDiPojo hfFileTypeDiPojo : fileTypeDiDao) {
            if (hfFileTypeDiPojo.getFileTypeGroupId() == null) {
                continue;
            } else {
                String fileTypeGroupId = hfFileTypeDiPojo.getFileTypeGroupId();
                if (typeGroupIdList.contains(fileTypeGroupId)) {
                    for (HashMap<String, Object> stringObjectHashMap : resultList) {
                        if (stringObjectHashMap.get("key").equals(fileTypeGroupId)) {
                            Map<String, String> chlid = new HashMap<>();
                            chlid.put("key", hfFileTypeDiPojo.getFileTypeId());
                            chlid.put("text", hfFileTypeDiPojo.getFileTypeName());
                            chlid.put("workflow", hfFileTypeDiPojo.getFileTypeWorkflow());
                            ((List<Map<String, String>>) stringObjectHashMap.get("children")).add(chlid);
                        }
                    }
                } else {
                    typeGroupIdList.add(fileTypeGroupId);
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("key", fileTypeGroupId);
                    map.put("text", hfFileTypeDiPojo.getFileTypeGroupName());
                    ArrayList<Map<String, String>> children = new ArrayList<>();
                    Map<String, String> chlid = new HashMap<>();
                    chlid.put("key", hfFileTypeDiPojo.getFileTypeId());
                    chlid.put("text", hfFileTypeDiPojo.getFileTypeName());
                    chlid.put("workflow", hfFileTypeDiPojo.getFileTypeWorkflow());
                    children.add(chlid);
                    map.put("children", children);
                    resultList.add(map);
                }
            }
        }
        return TResponseVo.success(resultList);
    }

    /**
     * 完全删除某篇文章
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delFileService(String fileUuid) throws Exception {
        return null;
    }

    /**
     * 获取模板列表
     *
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getModelListService(String userId, String type) throws Exception {
        if (StringUtils.isBlank(type) || !"WJ".equals(type)) {
            type = "MB";
        } else {
            type = "WJ";
        }
        List<LabelValuePojo> modelListDao = workingTableMapper.getModelListDao(userId, type);
        return TResponseVo.success(modelListDao);
    }


    /**
     * 新建模板组
     *
     * @param modelGroupPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newModelGroupService(ModelGroupPojo modelGroupPojo) throws Exception {
        String fileUuid = modelGroupPojo.getFileUuid();
        List<String> fileUuidList = (List) modelGroupPojo.getFileUuidList();
        List<String> fileUuidNeoList = new ArrayList<>();
        fileUuidNeoList.addAll(fileUuidList);
        // 判断必填
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.ERROR, "文件fileUuid不能为空");
        }
        if (fileUuidList == null || fileUuidList.size() == 0) {
            return TResponseVo.error(ResponseEnum.ERROR, "被选中文件fileUuidList不能为空");
        }
        if (fileUuidList.contains(fileUuid)) {
            return TResponseVo.error(ResponseEnum.ERROR, "被选中的关联模板不可以包含中心模板");
        }

        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        String fileName = docAllInfoDao.getFileName();
        String modelGroupName = fileName;
        modelGroupPojo.setModelGroupName(modelGroupName);  // 模板组名称与模板名称保持一致
        String fileUseRangeId = docAllInfoDao.getFileUseRangeId();
        // 判断同fileUuid在同fileUseRangeId下是否重复创建
        Integer integer = workingTableMapper.checkModelGroupUniquenessDao(fileUuid);
        if (integer != 0) {
            return TResponseVo.error(ResponseEnum.ERROR, "该模板组已被创建");
        }
        // 预查询标签数据
        List<DocLabelPojo> labelInfoListDao = workingTableMapper.getLabelInfoListDao();

        // 随机id
        String modelGroupId = UUID.randomUUID().toString().replaceAll("-", "");
        modelGroupPojo.setModelGroupId(modelGroupId);

        // 把list转str入库
        String s = new JsonKeyUtils().listToString(fileUuidList);
        modelGroupPojo.setFileUuidList(s);
        // 入数据库
        workingTableMapper.newModelGroupDao(modelGroupPojo);
//        // 图数据库
//        FileIndexEntity f = fileIndexEntityRepository.findByFileUuid(fileUuid);
//        if (f == null) {// 如果文件未创建实体
//            FileIndexEntity fileIndexEntity = new FileIndexEntity(fileUuid, null, fileName);
//            neo4jService.newFileIndexNeoService(fileIndexEntity);  // 创建文件实体
//            // 然后创建标签的实体
//            String fileLabelListStr = docAllInfoDao.getFileLabelList();
//            List<String> fileLabelList = new JsonKeyUtils().stringToList(fileLabelListStr);
//            for (String labelUuid : fileLabelList) { // fileLabelList每个标签逐一创建标签实体
//                for (DocLabelPojo docLabelPojo : labelInfoListDao) {  // 在预查询的labelList中查找label信息
//                    if (docLabelPojo.getLabelUuid().equals(labelUuid)) {
//                        String labelContent = docLabelPojo.getLabelContent();
//                        String labelUuidnew = UUID.randomUUID().toString().replaceAll("-", "");
//                        LabelEntity labelEntity = new LabelEntity(labelContent, labelUuidnew, labelUuid);
//                        neo4jService.newLabelNeoService(labelEntity);
//                        // 创建关系
//                        neo4jService.addLabelToFileIndexService(labelUuidnew, fileUuid);
//                        break;
//                    }
//                }
//            }
//        }
//        for (String fileUuid2 : fileUuidNeoList) {
//            // 检查节点是否创建 如果没有创建就建一个
//            FileIndexEntity f2 = fileIndexEntityRepository.findByFileUuid(fileUuid2);
//            if (f2 == null) {
//                DocFileIndexPojo docAllInfoDao2 = fileOperationMapper.getDocAllInfoDao(fileUuid2);
//                FileIndexEntity fileIndexEntity = new FileIndexEntity(fileUuid2, null, docAllInfoDao2.getFileName());
//                neo4jService.newFileIndexNeoService(fileIndexEntity);
//                // 然后创建标签的实体
//                String fileLabelListStr = docAllInfoDao2.getFileLabelList();
//                List<String> fileLabelList = new JsonKeyUtils().stringToList(fileLabelListStr);
//                for (String labelUuid : fileLabelList) { // fileLabelList每个标签逐一创建标签实体
//                    for (DocLabelPojo docLabelPojo : labelInfoListDao) {  // 在预查询的labelList中查找label信息
//                        if (docLabelPojo.getLabelUuid().equals(labelUuid)) {
//                            String labelContent = docLabelPojo.getLabelContent();
//                            String labelUuidnew = UUID.randomUUID().toString().replaceAll("-", "");
//                            LabelEntity labelEntity = new LabelEntity(labelContent, labelUuidnew, labelUuid);
//                            neo4jService.newLabelNeoService(labelEntity);
//                            // 创建关系
//                            neo4jService.addLabelToFileIndexService(labelUuidnew, fileUuid2);
//                            break;
//                        }
//                    }
//                }
//            }
//            // 创建关系
//            neo4jService.addFileIndexToFileIndexService(fileUuid, fileUuid2);
//        }

        // 创建返回值
        HashMap<Object, Object> ret = new HashMap<>();
        ret.put("info", "新建模板组成功");
        ret.put("modelGroupId", modelGroupId);
        return TResponseVo.success(ret);
    }

    /**
     * 删除模板组
     *
     * @param modelGroupPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo delModelGroupService(ModelGroupPojo modelGroupPojo) throws Exception {
        String modelGroupId = modelGroupPojo.getModelGroupId();
        // 获取fileUuid
        ModelGroupPojo modelGroupPojoInfo = workingTableMapper.getModelGroupInfoDao(modelGroupId);
        if (modelGroupPojoInfo == null) {
            return TResponseVo.error("该模板组id不存在 请检查");
        }
        String fileUuid = modelGroupPojoInfo.getFileUuid();
        // 根据fileUuid在图数据库中找出模板组关联的节点
//        FileIndexEntity f = fileIndexEntityRepository.findByFileUuid(fileUuid);
//        Set<FileIndexEntity> actors = f.getActors();
//        if (actors!= null && actors.size()!=0) {
//            for (FileIndexEntity actor : actors) {  // 逐一删除模板组中 中心模板与周围模板的关系
//                String fileUuid2 = actor.getFileUuid();
//                // 图数据库
//                neo4jService.delFileIndexFromFileIndexService(fileUuid, fileUuid2);
//            }
//        }
        // 删除图上关于fileUuid点的USE_IN关系
//        neo4jService.delFileIndexFromFileIndexAllService(fileUuid, "USE_IN");
        // 数据库
        workingTableMapper.delModelGroupDao(modelGroupId);
        return TResponseVo.success("删除模板组完成");
    }

    /**
     * 修改模板组
     *
     * @param modelGroupPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo updateModelGroupService(ModelGroupPojo modelGroupPojo) throws Exception {
        String modelGroupId = modelGroupPojo.getModelGroupId();
        if (StringUtils.isBlank(modelGroupId)) {
            return TResponseVo.error("modelGroupId传参为null");
        }
        ModelGroupPojo modelGroupPojoInfo = workingTableMapper.getModelGroupInfoDao(modelGroupId);
        if (modelGroupPojoInfo == null) {
            HashMap<String, String> ret = new HashMap<>();
            ret.put("info", "未查询到数据");
            return TResponseVo.error("未查询到数据");
        }
        String fileUuid = modelGroupPojoInfo.getFileUuid();

        List<String> fileUuidList = (List) modelGroupPojo.getFileUuidList();
        List<String> fileUuidNeoList = new ArrayList<>();
        fileUuidNeoList.addAll(fileUuidList);
        // 判断必填
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.ERROR, "文件fileUuid不能为空");
        }
        if (fileUuidList == null) {
            return TResponseVo.error(ResponseEnum.ERROR, "被选中文件fileUuidList不能为空");
        }
        if (fileUuidList.contains(fileUuid)) {
            return TResponseVo.error(ResponseEnum.ERROR, "被选中的关联模板不可以包含中心模板");
        }
        // 预查询标签数据
        List<DocLabelPojo> labelInfoListDao = workingTableMapper.getLabelInfoListDao();
        // 编辑采用先删后增的方法
        // 删除图上关于fileUuid点的USE_IN关系
//        neo4jService.delFileIndexFromFileIndexAllService(fileUuid, "USE_IN");
        // 后增
//        // 图数据库
//        for (String fileUuid2 : fileUuidNeoList) {
//            // 检查节点是否创建 如果没有创建就建一个
//            FileIndexEntity f2 = fileIndexEntityRepository.findByFileUuid(fileUuid2);
//            if (f2 == null) {
//                DocFileIndexPojo docAllInfoDao2 = fileOperationMapper.getDocAllInfoDao(fileUuid2);
//                FileIndexEntity fileIndexEntity = new FileIndexEntity(fileUuid2, null, docAllInfoDao2.getFileName());
//                neo4jService.newFileIndexNeoService(fileIndexEntity);
//                // 然后创建标签的实体
//                String fileLabelListStr = docAllInfoDao2.getFileLabelList();
//                List<String> fileLabelList = new JsonKeyUtils().stringToList(fileLabelListStr);
//                for (String labelUuid : fileLabelList) { // fileLabelList每个标签逐一创建标签实体
//                    for (DocLabelPojo docLabelPojo : labelInfoListDao) {  // 在预查询的labelList中查找label信息
//                        if (docLabelPojo.getLabelUuid().equals(labelUuid)) {
//                            String labelContent = docLabelPojo.getLabelContent();
//                            String labelUuidnew = UUID.randomUUID().toString().replaceAll("-", "");
//                            LabelEntity labelEntity = new LabelEntity(labelContent, labelUuidnew, labelUuid);
//                            neo4jService.newLabelNeoService(labelEntity);
//                            // 创建关系
//                            neo4jService.addLabelToFileIndexService(labelUuidnew, fileUuid2);
//                            break;
//                        }
//                    }
//                }
//            }
//            // 创建关系
//            neo4jService.addFileIndexToFileIndexService(fileUuid, fileUuid2);
//        }

        // 创建返回值
        HashMap<Object, Object> ret = new HashMap<>();
        ret.put("info", "编辑模板组成功");
        ret.put("modelGroupId", modelGroupId);
        return TResponseVo.success(ret);
    }

    /**
     * 查询某一模板组信息
     *
     * @param modelGroupPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getModelGroupInfoService(ModelGroupPojo modelGroupPojo) throws Exception {
        String modelGroupId = modelGroupPojo.getModelGroupId();
        if (StringUtils.isBlank(modelGroupId)) {
            return TResponseVo.error("modelGroupId传参为null");
        }
        ModelGroupPojo modelGroupPojoInfo = workingTableMapper.getModelGroupInfoDao(modelGroupId);
        if (modelGroupPojoInfo == null) {
            HashMap<String, String> ret = new HashMap<>();
            ret.put("info", "未查询到数据");
            return TResponseVo.error("未查询到数据");
        }

        List<String> labelList = modelGroupPojo.getLabels();

        List<String> list = new ArrayList<>();  // 储存次级模板fileUuid信息

        String fileUuid = modelGroupPojoInfo.getFileUuid();
        // 定义nodes 用来保存图数据库的点数据
        List<Map<String, Object>> nodes = new ArrayList<>();
        // 定义edges 用来保存图数据库的线数据
        List<Map<String, String>> edges = new ArrayList<>();
//        // 查图数据库
//        FileIndexEntity f = fileIndexEntityRepository.findByFileUuid(fileUuid);
//        Set<FileIndexEntity> actors = f.getActors();
//
//        // 查询所有次级模板的补充数据信息
//        ArrayList<String> fileUuidList = new ArrayList<>();
//        fileUuidList.add(fileUuid);
//        for (FileIndexEntity actor : actors) {
//            if (!fileUuidList.contains(actor.getFileUuid())) {
//                fileUuidList.add(actor.getFileUuid());
//            }
//        }
//        List<DocFileIndexPojo> fileAllInfoListDao = workingTableMapper.getFileAllInfoListDao(new JsonKeyUtils().listToString(fileUuidList, "','"));
//
//
//        for (FileIndexEntity actor : actors) {
//            boolean active = false;
//            list.add(actor.getFileUuid()); // 模板组次级模板列表
//            // 次级模板标签
//            // TODO 这里需要使用actor.getFileUuid()单独查询一次  而不是直接使用这里的actor对象 因为actor.getLabels()查出的Set<LabelEntity> labels为null
//            //  原因猜测:为了防止出现死循环 所以只能查到普通属性的值 无法查到set的内容 (佐证:Set<FileIndexEntity> actors查出来也是null)
//            FileIndexEntity f1 = fileIndexEntityRepository.findByFileUuid(actor.getFileUuid());
//            Set<LabelEntity> labels = f1.getLabels();
//            ArrayList<String> labelContentList = new ArrayList<>();
//            if (labels == null && (labelList == null || labelList.size() == 0)) {
//                active = true;
//            } else if (labels != null) {
//                for (LabelEntity label : labels) {
//                    boolean active2 = false;
//                    if (!labelContentList.contains(label.getLabelContent())) {
//                        labelContentList.add(label.getLabelContent());  // 返回节点上的标签信息
//                    }
//                    if (labelList == null || labelList.size() == 0) {
//                        active = true;
//                        active2 = true;
//                    } else if (labelList != null && labelList.contains(label.getMainLabelUuid())) {
//                        active = true;
//                        active2 = true;
//                    }
//                    // 储存标签点信息
//                    HashMap<String, Object> lab = new HashMap<>();
//                    lab.put("id", label.getLabelUuid());
//                    lab.put("label", label.getLabelContent());
//                    HashMap<String, Object> map6 = new HashMap<>();
//                    map6.put("type", "3");
//                    map6.put("active", active2);
//                    lab.put("data", map6);
//                    nodes.add(lab);
//                    // 储存标签线信息
//                    HashMap<String, String> l = new HashMap<>();
//                    l.put("target", actor.getFileUuid());
//                    l.put("source", label.getLabelUuid());
//                    l.put("label", "标签");
//                    edges.add(l);
//                }
//            }
//
//            // 储存线信息
//            HashMap<String, String> line = new HashMap<>();
//            line.put("target", f.getFileUuid());
//            line.put("source", actor.getFileUuid());
//            line.put("label", "用于");
//            edges.add(line);
//            // 储存次级模板点信息
//            HashMap<String, Object> map1 = new HashMap<>();
//            map1.put("id", actor.getFileUuid());
//            map1.put("label", actor.getFileName());
//            HashMap<String, Object> map5 = new HashMap<>();
//            map5.put("type", "2");
//            map5.put("tags", labelContentList);
//            map5.put("active", active);
//            for (DocFileIndexPojo docFileIndexPojo : fileAllInfoListDao) {
//                if (docFileIndexPojo.getFileUuid().equals(actor.getFileUuid())) {
//                    map5.put("updateTime", docFileIndexPojo.getUpdateTime());
//                    map5.put("fileTypeName", docFileIndexPojo.getFileTypeNameNew());
//                    map5.put("starNum", docFileIndexPojo.getCollectionCnt());
//                    map5.put("forkNum", docFileIndexPojo.getDerivationCnt());
//                    map5.put("fileUuid", actor.getFileUuid());
//                    map5.put("fileVersionName", docFileIndexPojo.getFileVersionName());
//                    break;
//                }
//            }
//            map1.put("data", map5);
//            nodes.add(map1);
//        }
//        // 中心模板标签
//        boolean active = false;
//        Set<LabelEntity> labels = f.getLabels();
//        ArrayList<String> labelContentList = new ArrayList<>();
//        for (LabelEntity label : labels) {
//            boolean active2 = false;
//            if (!labelContentList.contains(label.getLabelContent())) {
//                labelContentList.add(label.getLabelContent());
//            }
//            if (labelList == null || labelList.size() == 0) {
//                active = true;
//                active2 = true;
//            } else if (labelList != null && labelList.contains(label.getMainLabelUuid())) {
//                active = true;
//                active2 = true;
//            }
//            // 储存标签点信息
//            HashMap<String, Object> lab = new HashMap<>();
//            lab.put("id", label.getLabelUuid());
//            lab.put("label", label.getLabelContent());
//            HashMap<String, Object> map5 = new HashMap<>();
//            map5.put("type", "3");
//            map5.put("active", active2);
//            lab.put("data", map5);
//            nodes.add(lab);
//            // 储存标签线信息
//            HashMap<String, String> l = new HashMap<>();
//            l.put("target", f.getFileUuid());
//            l.put("source", label.getLabelUuid());
//            l.put("label", "标签");
//            edges.add(l);
//        }
//        // 储存中心模板点信息
//        HashMap<String, Object> map = new HashMap<>();
//        map.put("id", f.getFileUuid());
//        map.put("label", f.getFileName());
//        HashMap<String, Object> map5 = new HashMap<>();
//        map5.put("type", "1");
//        map5.put("tags", labelContentList);
//        map5.put("active", active);
//        for (DocFileIndexPojo docFileIndexPojo : fileAllInfoListDao) {
//            if (docFileIndexPojo.getFileUuid().equals(f.getFileUuid())) {
//                map5.put("updateTime", docFileIndexPojo.getUpdateTime());
//                map5.put("fileTypeName", docFileIndexPojo.getFileTypeNameNew());
//                map5.put("starNum", docFileIndexPojo.getCollectionCnt());
//                map5.put("forkNum", docFileIndexPojo.getDerivationCnt());
//                map5.put("fileUuid", f.getFileUuid());
//                fileUuidList.remove(fileUuid);
//                map5.put("fileUuidList", fileUuidList);
//                map5.put("fileVersionName", docFileIndexPojo.getFileVersionName());
//                break;
//            }
//        }
//        map.put("data", map5);
//        nodes.add(map);
//        // 构建返回信息
//        modelGroupPojoInfo.setNodes(nodes);
//        modelGroupPojoInfo.setEdges(edges);
//        modelGroupPojoInfo.setFileUuidList(list);
//        return TResponseVo.success(modelGroupPojoInfo);
        return null;
    }

    /**
     * 查询模板组列表
     *
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getModelGroupListService() throws Exception {
        List<ModelGroupPojo> modelGroupPojoList = workingTableMapper.getModelGroupListDao();
        for (ModelGroupPojo modelGroupPojoInfo : modelGroupPojoList) {
            List<String> list = new ArrayList<>();  // 储存次级模板fileUuid信息
            List<String> listName = new ArrayList<>();  // 储存次级模板fileUuid信息
            String fileUuid = modelGroupPojoInfo.getFileUuid();
//            FileIndexEntity f = fileIndexEntityRepository.findByFileUuid(fileUuid);
//            Set<FileIndexEntity> actors = f.getActors();
//            for (FileIndexEntity actor : actors) {
//                list.add(actor.getFileUuid()); // 模板组次级模板列表
//                listName.add(actor.getFileName()); // 模板组次级模板列表
//            }
            modelGroupPojoInfo.setFileUuidList(list);
            modelGroupPojoInfo.setFileNameList(listName);
        }
        return TResponseVo.success(modelGroupPojoList);
    }

    /**
     * 查询模板组列表KV
     *
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getModelGroupListKVService() throws Exception {
        List<LabelValuePojo> modelGroupList = workingTableMapper.getModelGroupListKVDao();
        return TResponseVo.success(modelGroupList);
    }

    /**
     * 获取平台视图
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getGlobalViewService(ModelGroupPojo modelGroupPojo) throws Exception {
        // 定义nodes 用来保存图数据库的点数据
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<String> ids = new ArrayList<>(); // 记录node的id 防止数据重复
        // 定义edges 用来保存图数据库的线数据
        List<Map<String, String>> edges = new ArrayList<>();

        // 筛选信息
        List<String> labelList = null;
        if (modelGroupPojo != null) {
            labelList = modelGroupPojo.getLabels();
        }


        List<ModelGroupPojo> modelGroupPojoList = workingTableMapper.getModelGroupListDao();
        for (ModelGroupPojo modelGroupPojoInfo : modelGroupPojoList) {
            String fileUuid = modelGroupPojoInfo.getFileUuid();
            ids.add(fileUuid);
            // 查图数据库
//            FileIndexEntity f = fileIndexEntityRepository.findByFileUuid(fileUuid);
//            Set<FileIndexEntity> actors = f.getActors();
//
//            // 查询所有次级模板的补充数据信息
//            ArrayList<String> fileUuidList = new ArrayList<>();
//            fileUuidList.add(fileUuid);
//            if (actors != null && actors.size() != 0) {
//                for (FileIndexEntity actor : actors) {
//                    if (!fileUuidList.contains(actor.getFileUuid())) {
//                        fileUuidList.add(actor.getFileUuid());
//                    }
//                }
//            }
//            List<DocFileIndexPojo> fileAllInfoListDao = workingTableMapper.getFileAllInfoListDao(new JsonKeyUtils().listToString(fileUuidList, "','"));
//
//            if (actors != null && actors.size() != 0) {
//                for (FileIndexEntity actor : actors) {
//                    boolean active = false;
//                    FileIndexEntity f1 = fileIndexEntityRepository.findByFileUuid(actor.getFileUuid());
//                    Set<LabelEntity> labels = f1.getLabels();
//                    ArrayList<String> labelContentList = new ArrayList<>();
//                    if (labels == null && (labelList == null || labelList.size() == 0)) {
//                        active = true;
//                    } else if (labels != null) {
//                        for (LabelEntity label : labels) {
//                            if (!labelContentList.contains(label.getLabelContent())) {
//                                labelContentList.add(label.getLabelContent());  // 返回节点上的标签信息
//                            }
//                            if (labelList == null || labelList.size() == 0) {
//                                active = true;
//                            } else if (labelList != null && labelList.contains(label.getMainLabelUuid())) {
//                                active = true;
//                            }
//                        }
//                    }
//
//                    // 储存点信息
//                    HashMap<String, Object> map1 = new HashMap<>();
//                    map1.put("id", actor.getFileUuid());
//                    map1.put("label", actor.getFileName());
//                    HashMap<String, Object> map6 = new HashMap<>();
//                    map6.put("type", "2");
//                    map6.put("tags", labelContentList);
//                    map6.put("active", active);
//                    for (DocFileIndexPojo docFileIndexPojo : fileAllInfoListDao) {
//                        if (docFileIndexPojo.getFileUuid().equals(f.getFileUuid())) {
//                            map6.put("updateTime", docFileIndexPojo.getUpdateTime());
//                            map6.put("fileTypeName", docFileIndexPojo.getFileTypeNameNew());
//                            map6.put("starNum", docFileIndexPojo.getCollectionCnt());
//                            map6.put("forkNum", docFileIndexPojo.getDerivationCnt());
//                            map6.put("fileUuid", actor.getFileUuid());
//                            map6.put("fileVersionName", docFileIndexPojo.getFileVersionName());
//                            break;
//                        }
//                    }
//                    map1.put("data", map6);
//                    if (!ids.contains(actor.getFileUuid())) {
//                        ids.add(actor.getFileUuid());
//                        nodes.add(map1);
//                    }
//                    // 储存线信息
//                    HashMap<String, String> line = new HashMap<>();
//                    line.put("target", f.getFileUuid());
//                    line.put("source", actor.getFileUuid());
//                    line.put("label", "用于");
//                    edges.add(line);
//                }
//            }
//
//            // 中心模板标签
//            boolean active = false;
//            Set<LabelEntity> labels = f.getLabels();
//            ArrayList<String> labelContentList = new ArrayList<>();
//            for (LabelEntity label : labels) {
//                if (!labelContentList.contains(label.getLabelContent())) {
//                    labelContentList.add(label.getLabelContent());
//                }
//                if (labelList == null || labelList.size() == 0) {
//                    active = true;
//                } else if (labelList != null && labelList.contains(label.getMainLabelUuid())) {
//                    active = true;
//                }
//            }
//            // 储存中心模板点信息
//            HashMap<String, Object> map = new HashMap<>();
//            map.put("id", f.getFileUuid());
//            map.put("label", f.getFileName());
//            HashMap<String, Object> map5 = new HashMap<>();
//            map5.put("type", "1");
//            map5.put("tags", labelContentList);
//            map5.put("modelGroupId", modelGroupPojoInfo.getModelGroupId());
//            map5.put("active", active);
//            for (DocFileIndexPojo docFileIndexPojo : fileAllInfoListDao) {
//                if (docFileIndexPojo.getFileUuid().equals(f.getFileUuid())) {
//                    map5.put("updateTime", docFileIndexPojo.getUpdateTime());
//                    map5.put("fileTypeName", docFileIndexPojo.getFileTypeNameNew());
//                    map5.put("starNum", docFileIndexPojo.getCollectionCnt());
//                    map5.put("forkNum", docFileIndexPojo.getDerivationCnt());
//                    fileUuidList.remove(fileUuid);
//                    map5.put("fileUuidList", fileUuidList);
//                    map5.put("fileUuid", fileUuid);
//                    map5.put("fileVersionName", docFileIndexPojo.getFileVersionName());
//                    break;
//                }
//            }
//            map.put("data", map5);
//            if (!ids.contains(f.getFileUuid())) {
//                ids.add(f.getFileUuid());
//            } else {
//                for (Map<String, Object> node : nodes) {
//                    if (node.get("id").equals(map.get("id"))) {
//                        nodes.remove(node);
//                        break;
//                    }
//                }
//            }
//            nodes.add(map);
        }
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("nodes", nodes);
        ret.put("edges", edges);
        return TResponseVo.success(ret);
    }

    /**
     * 获取模板组标签预筛选按钮列表
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getModelGroupLabelListService(ModelGroupPojo modelGroupPojo) throws Exception {
        return  null;
//        ArrayList<LabelValuePojo> labelValuePojos = new ArrayList<>();
//        ArrayList<String> labelList = new ArrayList<>();
//
//        String modelGroupId = null;
//        if (modelGroupPojo != null) {
//            modelGroupId = modelGroupPojo.getModelGroupId();
//        }
//
//        if (StringUtils.isBlank(modelGroupId)) {
//            Iterable<LabelEntity> all = labelEntityRepository.findAll();
//            for (LabelEntity labelEntity : all) {
//                if (!labelList.contains(labelEntity.getMainLabelUuid())) {
//                    labelValuePojos.add(new LabelValuePojo(labelEntity.getLabelContent(), labelEntity.getMainLabelUuid()));
//                    labelList.add(labelEntity.getMainLabelUuid());
//                }
//            }
//            return TResponseVo.success(labelValuePojos);
//        } else {
//            ModelGroupPojo modelGroupPojoInfo = workingTableMapper.getModelGroupInfoDao(modelGroupId);
//            if (modelGroupPojoInfo == null) {
//                HashMap<String, String> ret = new HashMap<>();
//                ret.put("info", "未查询到数据");
//                return TResponseVo.success(ret);
//            }
//
//            String fileUuid = modelGroupPojoInfo.getFileUuid();
//            // 查图数据库
//            FileIndexEntity f = fileIndexEntityRepository.findByFileUuid(fileUuid);
//            Set<FileIndexEntity> actors = f.getActors();
//            for (FileIndexEntity actor : actors) {
//                FileIndexEntity f1 = fileIndexEntityRepository.findByFileUuid(actor.getFileUuid());
//                Set<LabelEntity> labels = f1.getLabels();
//                if (labels != null) {
//                    for (LabelEntity labelEntity : labels) {
//                        // 储存标签点信息
//                        if (!labelList.contains(labelEntity.getMainLabelUuid())) {
//                            labelValuePojos.add(new LabelValuePojo(labelEntity.getLabelContent(), labelEntity.getMainLabelUuid()));
//                            labelList.add(labelEntity.getMainLabelUuid());
//                        }
//                    }
//                }
//            }
//            Set<LabelEntity> labels = f.getLabels();
//            for (LabelEntity labelEntity : labels) {
//                if (!labelList.contains(labelEntity.getMainLabelUuid())) {
//                    labelValuePojos.add(new LabelValuePojo(labelEntity.getLabelContent(), labelEntity.getMainLabelUuid()));
//                    labelList.add(labelEntity.getMainLabelUuid());
//                }
//            }
//            return TResponseVo.success(labelValuePojos);
//        }

    }

    /**
     * 获取文库常用列表
     *
     * @param pagePojo@return
     * @throws Exception
     */
    @Override
    public TResponseVo getCommonUseListService(PagePojo pagePojo) throws Exception {
        // 标签数据预搜索
        List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);

        List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
        String fileTypeId = String.valueOf(pagePojo.getFileTypeId());  // 模板分类筛选
        if (pagePojo.getFileTypeId() == null || StringUtils.isBlank(fileTypeId)) {
            fileTypeId = null;
        }
        List<String> fileLabelList = pagePojo.getFileLabelList();  // 标签筛选 (只显示全命中的文档)

        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<DocFileIndexPojo> docParamsPojos = workingTableMapper.getCommonUseListDao(paramNameLike, null, fileTypeId);
        List<DocFileIndexPojo> docParamsPojosTmp = new ArrayList<>();
        docParamsPojosTmp.addAll(docParamsPojos);
        for (DocFileIndexPojo docParamsPojo : docParamsPojosTmp) {  // 根据标签过滤数据
            String fileLabelList2 = docParamsPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            if (!new JsonKeyUtils().containsAll(strings, fileLabelList)) {  // 如果文档的标签完全覆盖'标签筛选'所选内容
                docParamsPojos.remove(docParamsPojo);
            }
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            for (String labelId : strings) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            docParamsPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));
        }

        for (DocFileIndexPojo docParamsPojo : docParamsPojos) {  // 设置文件类型名称
            docParamsPojo.setEdit(false);  // 设置不可编辑
            for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                    docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                    break;
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
     * 获取文库母版列表
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getLibraryListService(PagePojo pagePojo) throws Exception {
        // 标签数据预搜索
        List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);

        List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
        String fileTypeId = String.valueOf(pagePojo.getFileTypeId());  // 模板分类筛选
        if (pagePojo.getFileTypeId() == null || StringUtils.isBlank(fileTypeId)) {
            fileTypeId = null;
        }
        List<String> fileLabelList = pagePojo.getFileLabelList();  // 标签筛选 (只显示全命中的文档)

        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<DocFileIndexPojo> docParamsPojos = workingTableMapper.getLibraryListDao(paramNameLike, null, fileTypeId);
        List<DocFileIndexPojo> docParamsPojosTmp = new ArrayList<>();
        docParamsPojosTmp.addAll(docParamsPojos);
        for (DocFileIndexPojo docParamsPojo : docParamsPojosTmp) {  // 根据标签过滤数据
            String fileLabelList2 = docParamsPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            if (!new JsonKeyUtils().containsAll(strings, fileLabelList)) {  // 如果文档的标签完全覆盖'标签筛选'所选内容
                docParamsPojos.remove(docParamsPojo);
            }
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            for (String labelId : strings) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            docParamsPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));
        }

        for (DocFileIndexPojo docParamsPojo : docParamsPojos) {  // 设置文件类型名称
            docParamsPojo.setEdit(false);  // 设置不可编辑
            for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                    docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                    break;
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
     * 获取文库搜索结果
     *
     * @param pagePojo@return
     * @throws Exception
     */
    @Override
    public TResponseVo searchLibraryService(PagePojo pagePojo) throws Exception {
        // 标签数据预搜索
        List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);

        List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
        String fileTypeId = String.valueOf(pagePojo.getFileTypeId());  // 模板分类筛选
        if (pagePojo.getFileTypeId() == null || StringUtils.isBlank(fileTypeId)) {
            fileTypeId = null;
        }
        List<String> fileLabelList = pagePojo.getFileLabelList();  // 标签筛选 (只显示全命中的文档)

        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<DocFileIndexPojo> docParamsPojos = workingTableMapper.getSearchListDao(paramNameLike, null, fileTypeId);

        // 将搜索关键字放入热词
        if (redisUtils.hasKey(envName + "LibraryHotSearch")) {
            Object helpDocHotSearchKey = redisUtils.get(envName + "LibraryHotSearch");
            List<Map<String, String>> list = JSON.parseObject(String.valueOf(helpDocHotSearchKey), List.class);
            boolean flag = true;
            for (Map<String, String> m : list) {
                if (m.get("content").equals(paramNameLike)) {
                    m.put("cnt", String.valueOf(Integer.valueOf(m.get("cnt")) + 1));
                    flag = false;
                    redisUtils.set(envName + "LibraryHotSearch", JSON.toJSONString(list), 3600 * 24);
                    break;
                }
            }
            if (flag) {
                if (StringUtils.isNotBlank(paramNameLike)) {
                    Map<String, String> map = new HashMap<>();
                    map.put("content", paramNameLike);
                    map.put("cnt", "1");
                    list.add(map);
                }
                redisUtils.set(envName + "LibraryHotSearch", JSON.toJSONString(list), 3600 * 24);
            }
        } else {
            if (StringUtils.isNotBlank(paramNameLike)) {
                ArrayList<Map<String, String>> list = new ArrayList<>();
                Map<String, String> map = new HashMap<>();
                map.put("content", paramNameLike);
                map.put("cnt", "1");
                list.add(map);
                redisUtils.set(envName + "LibraryHotSearch", JSON.toJSONString(list), 3600 * 24);
            }
        }

        List<DocFileIndexPojo> docParamsPojosTmp = new ArrayList<>();
        docParamsPojosTmp.addAll(docParamsPojos);
        for (DocFileIndexPojo docParamsPojo : docParamsPojosTmp) {  // 根据标签过滤数据
            String fileLabelList2 = docParamsPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            if (!new JsonKeyUtils().containsAll(strings, fileLabelList)) {  // 如果文档的标签完全覆盖'标签筛选'所选内容
                docParamsPojos.remove(docParamsPojo);
            }
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            for (String labelId : strings) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            docParamsPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));
        }

        for (DocFileIndexPojo docParamsPojo : docParamsPojos) {  // 设置文件类型名称
            for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                    docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                    break;
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
     * 文库-文件血缘关系图
     *
     * @param pagePojo@return
     * @throws Exception
     */
    @Override
    public TResponseVo getLibraryViewService(PagePojo pagePojo) throws Exception {
        if (pagePojo == null) {
            pagePojo = new PagePojo();
        }
        String paramNameLike = pagePojo.getParamNameLike();
        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        List<DocFileIndexPojo> libraryList = workingTableMapper.getLibraryListDao(paramNameLike, null, null);
        List<DocFileIndexPojo> commonUseList = workingTableMapper.getCommonUseListDao(paramNameLike, 200, null);
        // 标签数据预搜索
        List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);

        ArrayList<NodeForm> nodes = new ArrayList<>();
        for (DocFileIndexPojo docFileIndexPojo : libraryList) {  // 母版节点
            NodeForm nodeForm = new NodeForm();
            HashMap<String, Object> data = new HashMap<>();
            nodeForm.setId(docFileIndexPojo.getFileUuid());
            nodeForm.setLabel(docFileIndexPojo.getFileName());
            data.put("fileUuid", docFileIndexPojo.getFileUuid());
            data.put("fileVersionId", docFileIndexPojo.getFileVersionId());
            data.put("fileTypeId", docFileIndexPojo.getFileTypeId());
            data.put("updateTime", docFileIndexPojo.getUpdateTime());
            data.put("userName", docFileIndexPojo.getCreateUserName());
            data.put("fileVersionName", docFileIndexPojo.getFileVersionName());
            data.put("forkNum", docFileIndexPojo.getDerivationNumber());
            data.put("starNum", docFileIndexPojo.getStarNum());
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            String fileLabelList2 = docFileIndexPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            for (String labelId : strings) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            data.put("tags", fileLabelContentList);
            data.put("tagIds", strings);
            data.put("fileClass", "母版");
            nodeForm.setData(data);
            nodes.add(nodeForm);
        }
        for (DocFileIndexPojo docFileIndexPojo : commonUseList) {  // 模版节点
            NodeForm nodeForm = new NodeForm();
            HashMap<String, Object> data = new HashMap<>();
            nodeForm.setId(docFileIndexPojo.getFileUuid());
            nodeForm.setLabel(docFileIndexPojo.getFileName());
            data.put("fileUuid", docFileIndexPojo.getFileUuid());
            data.put("fileVersionId", docFileIndexPojo.getFileVersionId());
            data.put("fileTypeId", docFileIndexPojo.getFileTypeId());
            data.put("updateTime", docFileIndexPojo.getUpdateTime());
            data.put("userName", docFileIndexPojo.getCreateUserName());
            data.put("fileVersionName", docFileIndexPojo.getFileVersionName());
            data.put("forkNum", docFileIndexPojo.getDerivationNumber());
            data.put("starNum", docFileIndexPojo.getStarNum());
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            String fileLabelList2 = docFileIndexPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            for (String labelId : strings) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            data.put("tags", fileLabelContentList);
            data.put("tagIds", strings);
            data.put("fileClass", "模板");
            nodeForm.setData(data);
            nodes.add(nodeForm);
        }

        ArrayList<EdgeForm> edges = new ArrayList<>();
        for (DocFileIndexPojo docFileIndexPojo : libraryList) {  // 母版节点
            String fileUuid = docFileIndexPojo.getFileUuid();
            for (DocFileIndexPojo docFileIndexPojo2 : commonUseList) {  // 模版节点
                if (fileUuid.equals(docFileIndexPojo2.getFileParentId())) {
                    EdgeForm edgeForm = new EdgeForm();
                    edgeForm.setSource(docFileIndexPojo.getFileUuid());
                    edgeForm.setTarget(docFileIndexPojo2.getFileUuid());
                    edgeForm.setLabel("派生");
                    edges.add(edgeForm);
                }
            }
        }
        // 模板之间的连接
        for (DocFileIndexPojo docFileIndexPojo : commonUseList) {  // 模版节点
            String fileUuid = docFileIndexPojo.getFileUuid();
            for (DocFileIndexPojo docFileIndexPojo2 : commonUseList) {  // 模版节点
                if (fileUuid.equals(docFileIndexPojo2.getFileParentId())) {
                    EdgeForm edgeForm = new EdgeForm();
                    edgeForm.setSource(docFileIndexPojo.getFileUuid());
                    edgeForm.setTarget(docFileIndexPojo2.getFileUuid());
                    edgeForm.setLabel("派生");
                    edges.add(edgeForm);
                }
            }
        }

        Map retMap = new HashMap();
        retMap.put("nodes", nodes);
        retMap.put("edges", edges);
        return TResponseVo.success(retMap);
    }


    /**
     * 文库-热门搜索
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getLibraryHotSearchService() throws Exception {
        if (redisUtils.hasKey(envName + "LibraryHotSearch")) {
            Object helpDocHotSearchKey = redisUtils.get(envName + "LibraryHotSearch");
            List<Map<String, String>> list = JSON.parseObject(String.valueOf(helpDocHotSearchKey), List.class);
            List<Map<String, String>> mapList = new JsonKeyUtils().orderMapList7(list);
            ArrayList<String> strings = new ArrayList<>();
            int max = mapList.size() < 5 ? mapList.size() : 5;
            for (int i = 0; i < max; i++) {
                strings.add(mapList.get(i).get("content"));
            }
            return TResponseVo.success(strings);
        } else {
            return TResponseVo.success(new ArrayList<String>());
        }
    }

    /**
     * 模板类型分布
     *
     * @param
     */
    @Override
    public TResponseVo getModelRateService() throws Exception {
        List<LabelValuePojo> fileTypeListDao = fileOperationMapper.getFileTypeListDao();
        LabelValuePojo lv = new LabelValuePojo();
        lv.setLabel("未知类型");
        lv.setValue("W");
        fileTypeListDao.add(lv);
        List<DocFileIndexPojo> modelRateDao = workingTableMapper.getModelRateDao();
        List<Map<String, Object>> retList = new ArrayList<>();
        int sum = 0;
        for (DocFileIndexPojo docFileIndexPojo : modelRateDao) {
            sum = sum + docFileIndexPojo.getCnt();
        }
        for (DocFileIndexPojo docFileIndexPojo : modelRateDao) {
            for (LabelValuePojo labelValuePojo : fileTypeListDao) {
                if (labelValuePojo.getValue().equals(docFileIndexPojo.getFileTypeId())) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("item", labelValuePojo.getLabel());
                    map.put("count", docFileIndexPojo.getCnt());
                    map.put("percent", new JsonKeyUtils().decimalKeepN((double) docFileIndexPojo.getCnt() / sum, 3));
                    retList.add(map);
                    break;
                }
            }
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("data", retList);
        retMap.put("sum", sum);
        return TResponseVo.success(retMap);
    }

    /**
     * 模板文件分布
     *
     * @param
     */
    @Override
    public TResponseVo getFileRateService() throws Exception {
        List<LabelValuePojo> fileTypeListDao = fileOperationMapper.getFileTypeListDao();
        LabelValuePojo lv = new LabelValuePojo();
        lv.setLabel("未知类型");
        lv.setValue("W");
        fileTypeListDao.add(lv);
        List<DocFileIndexPojo> fileRateDao = workingTableMapper.getFileRateDao();
        List<Map<String, Object>> retList = new ArrayList<>();
        int sum = 0;
        for (DocFileIndexPojo docFileIndexPojo : fileRateDao) {
            sum = sum + docFileIndexPojo.getCnt();
        }
        for (DocFileIndexPojo docFileIndexPojo : fileRateDao) {
            for (LabelValuePojo labelValuePojo : fileTypeListDao) {
                if (labelValuePojo.getValue().equals(docFileIndexPojo.getFileTypeId())) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("item", labelValuePojo.getLabel());
                    map.put("count", docFileIndexPojo.getCnt());
                    map.put("percent", new JsonKeyUtils().decimalKeepN((double) docFileIndexPojo.getCnt() / sum, 3));
                    retList.add(map);
                }
            }
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("data", retList);
        retMap.put("sum", sum);
        return TResponseVo.success(retMap);
    }

    /**
     * 首页-我的文档统计
     *
     * @param userId
     */
    @Override
    public TResponseVo getMyFileStatisticsService(String userId) throws Exception {
        // 我的-创作的模板数
        Integer myCreateModelCnt = workingTableMapper.getMyCreateModelCntDao(userId);
        // 我的-创作的文件数
        Integer myCreateFileCnt = workingTableMapper.getMyCreateFileCntDao(userId);
        // 我的-参与的项目数
        Integer myJoinProjectCnt = 0;
        List<ProjectPojo> projectListDao = workingTableMapper.getProjectListDao(null, null);
        for (ProjectPojo projectPojo : projectListDao) {
            if (userId.equals(projectPojo.getCreateUserId())) {
                myJoinProjectCnt++;
                continue;
            }
            Object includeUserId = projectPojo.getIncludeUserId();
            if (includeUserId != null && !includeUserId.equals("null")) {
                List<String> includeUserIdString = JSON.parseObject((String) includeUserId, List.class);
                if (includeUserIdString.contains(userId)) {
                    myJoinProjectCnt++;
                }
            }
        }
        // 我的-收藏的模板数
        Integer myCollectCnt = workingTableMapper.getMyCollectCntDao(userId);

        // 构建返回内容
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("myCreateModelCnt", myCreateModelCnt);
        retMap.put("myCreateFileCnt", myCreateFileCnt);
        retMap.put("myJoinProjectCnt", myJoinProjectCnt);
        retMap.put("myCollectCnt", myCollectCnt);
        return TResponseVo.success(retMap);
    }

    /**
     * 首页-热力图
     */
    @Override
    public TResponseVo getHeatMapService() throws Exception {
        JsonKeyUtils jsonKeyUtils = new JsonKeyUtils();
        String dayAgoOrAfterString = jsonKeyUtils.getDayAgoOrAfterString(-120);
        String today = jsonKeyUtils.getDayAgoOrAfterString(0);
        List<String> days = jsonKeyUtils.getDays(dayAgoOrAfterString, today);
        List<DocFileIndexPojo> heatMapDao = workingTableMapper.getHeatMapDao();
        List<Map<String, Object>> retList = new ArrayList<>();
        int week = 0;
        for (String day : days) {
            HashMap<String, Object> dayMap = new HashMap<>();
            dayMap.put("date", day);  // 日期
            dayMap.put("month", Integer.valueOf(day.substring(5, 7)) - 1);  // 月份
            dayMap.put("day", jsonKeyUtils.dateToWeek(day));  // 周几  0-星期天  1-星期一  6-星期六
            dayMap.put("week", jsonKeyUtils.dateToWeek(day) == 6 ? week++ : week);  // 第几周
//            dayMap.put("week",jsonKeyUtils.weekOfYear(day));  // 第几周
            for (DocFileIndexPojo docFileIndexPojo : heatMapDao) {
                if (day.equals(docFileIndexPojo.getCreateDate())) {
                    dayMap.put("commits", docFileIndexPojo.getCnt());
                    break;
                } else {
                    dayMap.put("commits", 0);
                }
            }
            retList.add(dayMap);
        }
        return TResponseVo.success(retList);
    }

    /**
     * 获取母版清单
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getMasterModelService(PagePojo pagePojo) throws Exception {
        // 标签数据预搜索
        List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);

        List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
        String fileTypeId = String.valueOf(pagePojo.getFileTypeId());  // 模板分类筛选
        if (pagePojo.getFileTypeId() == null || StringUtils.isBlank(fileTypeId)) {
            fileTypeId = null;
        }
        List<String> fileLabelList = pagePojo.getFileLabelList();  // 标签筛选 (只显示全命中的文档)

        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<DocFileIndexPojo> docParamsPojos = workingTableMapper.getMasterModelDao(paramNameLike, null, fileTypeId);
        List<DocFileIndexPojo> docParamsPojosTmp = new ArrayList<>();
        docParamsPojosTmp.addAll(docParamsPojos);
        for (DocFileIndexPojo docParamsPojo : docParamsPojosTmp) {  // 根据标签过滤数据
            String fileLabelList2 = docParamsPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            if (!new JsonKeyUtils().containsAll(strings, fileLabelList)) {  // 如果文档的标签完全覆盖'标签筛选'所选内容
                docParamsPojos.remove(docParamsPojo);
            }
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            for (String labelId : strings) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            docParamsPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));
        }

        for (DocFileIndexPojo docParamsPojo : docParamsPojos) {  // 设置文件类型名称
            for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                    docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                    break;
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
     * 获取模板清单LV
     *
     * @param
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getMasterModelLVService() throws Exception {
        List<DocFileIndexPojo> docParamsPojos = workingTableMapper.getMasterModelDao(null, null, null);
        List<LabelValuePojo> docParamsPojosTmp = new ArrayList<>();
        List<DocLabelPojo> labelInfoListDao = workingTableMapper.getLabelInfoListDao();
        List<String> targetLabelUuid = new ArrayList<>();
        for (DocLabelPojo docLabelPojo : labelInfoListDao) {
            if ("标准文件".equals(docLabelPojo.getLabelContent())) {
                targetLabelUuid.add(docLabelPojo.getLabelUuid());
            }
        }
        for (DocFileIndexPojo docParamsPojo : docParamsPojos) {
            String label = docParamsPojo.getFileLabelList();
            List<String> list = new JsonKeyUtils().stringToList(label);
            for (String s : list) {
                if (targetLabelUuid.contains(s)) {
                    docParamsPojosTmp.add(new LabelValuePojo(docParamsPojo.getFileName(), docParamsPojo.getFileUuid()));
                    break;
                }
            }
        }
        return TResponseVo.success(docParamsPojosTmp);
    }

    /**
     * 获取最近使用文档目录
     *
     * @param limit
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getRecentFileListService(String userId, Object limit, String fileTypeGroupId) throws Exception {
        String num = "";
        if (limit == null) {
            num = "30";
        } else if (limit instanceof Integer) {
            if ((Integer) limit >= 30) {
                num = "30";
            } else {
                num = String.valueOf(Math.abs((Integer) limit));
            }
        } else if (limit instanceof String) {
            if (Integer.valueOf((String) limit) >= 30) {
                num = "30";
            } else {
                num = (String) limit;
            }
        }
        if (!StringUtils.isBlank(fileTypeGroupId)) {
            fileTypeGroupId = null;

            List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
            List<DocFileIndexPojo> recentFileListDao = workingTableMapper.getRecentFileListDao(userId, num, fileTypeGroupId);
            for (DocFileIndexPojo docParamsPojo : recentFileListDao) {
                // 设置时间
                Date updateTime = docParamsPojo.getUpdateTime();
                String ago = new TimeFormatUtils().getAgo(updateTime);
                docParamsPojo.setTimeFormat(ago);
                // 设置文件类型名称
                for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                    if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                        docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                        break;
                    }
                }
            }

            // 标签数据预搜索
            List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);
            for (DocFileIndexPojo docParamsPojo : recentFileListDao) {  // 根据标签过滤数据
                String fileLabelList2 = docParamsPojo.getFileLabelList();
                List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
                // 翻译标签内容
                List<String> fileLabelContentList = new ArrayList<>();
                for (String labelId : strings) {
                    for (LabelValuePojo labelValuePojo : labelLVListDao) {
                        if (labelValuePojo.getValue().equals(labelId)) {
                            fileLabelContentList.add(labelValuePojo.getLabel());
                        }
                    }
                }
                docParamsPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));
            }

            return TResponseVo.success(recentFileListDao);
        } else {
            List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
            List<DocFileIndexPojo> recentFileListDao = workingTableMapper.getRecentFileList2Dao(userId, num, fileTypeGroupId);
            for (DocFileIndexPojo docParamsPojo : recentFileListDao) {
                // 设置时间
                Date updateTime = docParamsPojo.getUpdateTime();
                String ago = new TimeFormatUtils().getAgo(updateTime);
                docParamsPojo.setTimeFormat(ago);
                // 设置文件类型名称
                for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                    if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                        docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                        break;
                    }
                }
            }

            // 标签数据预搜索
            List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);
            for (DocFileIndexPojo docParamsPojo : recentFileListDao) {  // 根据标签过滤数据
                String fileLabelList2 = docParamsPojo.getFileLabelList();
                List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
                // 翻译标签内容
                List<String> fileLabelContentList = new ArrayList<>();
                for (String labelId : strings) {
                    for (LabelValuePojo labelValuePojo : labelLVListDao) {
                        if (labelValuePojo.getValue().equals(labelId)) {
                            fileLabelContentList.add(labelValuePojo.getLabel());
                        }
                    }
                }
                docParamsPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));
            }

            return TResponseVo.success(recentFileListDao);
        }
    }

    /**
     * 获取文档审核列表
     */
    @Override
    public TResponseVo getAuditingFileListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        List<DocFileIndexPojo> auditingFileListDao = workingTableMapper.getAuditingFileListDao(paramNameLike);

        for (DocFileIndexPojo docFileIndexPojo : auditingFileListDao) {
            String compareInfo = docFileIndexPojo.getCompareInfo();
            String annotate = docFileIndexPojo.getAnnotate();
            List<Map<String, String>> annotateList = JSON.parseObject(annotate, List.class);
            ArrayList<LabelValuePojo> auditingContent = new ArrayList<>();
            List<Map<String, String>> list = JSON.parseObject(compareInfo, List.class);
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
            docFileIndexPojo.setCompareInfo(null);
            docFileIndexPojo.setAnnotate(null);
            docFileIndexPojo.setAuditingContent(auditingContent);
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(auditingFileListDao);
        }
        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(auditingFileListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", auditingFileListDao.size());
        return TResponseVo.success(resultMap);

    }

    /**
     * 审核操作
     *
     * @param fileUuid
     * @param fileVersionId
     * @param auditingStatus
     */
    @Override
    public TResponseVo changeAuditingFileStatusService(String fileUuid, String fileVersionId, String auditingStatus, String auditingReason, String auditingUserId) throws Exception {
        if (auditingStatus.equals("通过") || auditingStatus.equals("不通过")) {
            // 审批
            workingTableMapper.changeAuditingFileStatusDao(fileUuid, fileVersionId, auditingStatus, auditingReason, auditingUserId, new Date());
            // 查询
            HfFileLibraryPojo auditingFileRecordDao = workingTableMapper.getAuditingFileRecordDao(fileUuid, fileVersionId);
            String createUserId = auditingFileRecordDao.getCreateUserId();
            // 新增通知
            HfAuditingResultNoticePojo hfAuditingResultNoticePojo = new HfAuditingResultNoticePojo();
            hfAuditingResultNoticePojo.setAuditingUuid(UUID.randomUUID().toString().replaceAll("-", ""));
            hfAuditingResultNoticePojo.setAuditingContent("您提交的模板已经审批完成，审批结果为 : " + auditingStatus + "");
            hfAuditingResultNoticePojo.setNoticeUserId(createUserId);
            hfAuditingResultNoticePojo.setFileUuid(fileUuid);
            hfAuditingResultNoticePojo.setFileVersionId(fileVersionId);
            hfAuditingResultNoticePojo.setNoticeType("审核");
            workingTableMapper.noticeCreateUserDao(hfAuditingResultNoticePojo); // 通知用户
            return TResponseVo.success("审核完成");
        } else {
            return TResponseVo.error("auditingStatus参数值错误");
        }
    }

    /**
     * 获取审核结果通知
     */
    @Override
    public TResponseVo getResultNoticeService(PagePojo pagePojo, String userId, Boolean isRead, String noticeType) throws Exception {
        if (pagePojo == null) {
            pagePojo = new PagePojo();
        }
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        // noticeType 不传查所有 | 如果以逗号分隔,查多个类型
        if (StringUtils.isNotBlank(noticeType)) {
            String s = noticeType.replaceAll(",", "','");
            noticeType = s;
        } else {
            noticeType = null;
        }

        List<HfAuditingResultNoticePojo> resultNoticeDao = new ArrayList<>();
        if ("0".equalsIgnoreCase(isPaged)) {
            resultNoticeDao = workingTableMapper.getResultNoticeDao(userId, "1", noticeType);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            resultNoticeDao = workingTableMapper.getResultNoticeDao(userId, "1", noticeType);
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(resultNoticeDao);
        } else {
            PageInfo pageInfo = new PageInfo(resultNoticeDao);
            return TResponseVo.success(pageInfo);
        }

    }


    /**
     * 已读
     *
     * @param auditingUuid
     */
    @Override
    public TResponseVo readResultNoticeService(String auditingUuid) throws Exception {
        workingTableMapper.readResultNoticeDao(auditingUuid);
        return TResponseVo.success("已读");
    }

    /**
     * 获取文库文档管理列表
     */
    @Override
    public TResponseVo getLibraryFileManagementListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<String> fileLineStatusList = (List) pagePojo.getFileLineStatus();
        List<String> fileTypeId = (List) pagePojo.getFileTypeId();
        String fileTypeIdStr = new JsonKeyUtils().listToString(fileTypeId, "','");
        if (fileTypeId == null || fileTypeId.size() == 0) {
            fileTypeIdStr = null;
        }
        List<String> fileClass = (List) pagePojo.getFileClass();
        String fileClassStr = new JsonKeyUtils().listToString(fileClass, "','");
        if (fileClass == null || fileClass.size() == 0) {
            fileClassStr = null;
        }
        List<DocFileIndexPojo> auditingFileListDao = workingTableMapper.getLibraryFileManagementListDao(paramNameLike, fileClassStr, fileTypeIdStr);
        List<DocFileIndexPojo> libraryFileVersionListAuditingInfoDao = workingTableMapper.getLibraryFileVersionListAuditingInfoDao();
        for (DocFileIndexPojo docFileIndexPojo : auditingFileListDao) {
            List<LabelValuePojo> list = new ArrayList<>();
            docFileIndexPojo.setFileLineStatus("offline");
            docFileIndexPojo.setFileVersionNameOnline("-");
            docFileIndexPojo.setFileVersionName("-");
            docFileIndexPojo.setIsRecommend("0");
            for (DocFileIndexPojo fileIndexPojo : libraryFileVersionListAuditingInfoDao) {
                if (docFileIndexPojo.getFileUuid().equals(fileIndexPojo.getFileUuid())) {
                    LabelValuePojo labelValuePojo = new LabelValuePojo();
                    labelValuePojo.setValue(fileIndexPojo.getFileVersionId());
                    labelValuePojo.setLabel(fileIndexPojo.getFileVersionName());
                    list.add(labelValuePojo);
                    if (fileIndexPojo.getFileLineStatus() != null && fileIndexPojo.getFileLineStatus().equals("online")) {  // 上线状态
                        docFileIndexPojo.setFileLineStatus("online");
                        docFileIndexPojo.setFileVersionNameOnline(fileIndexPojo.getFileVersionName());
                        docFileIndexPojo.setFileVersionName(fileIndexPojo.getFileVersionName());
                    }
                    if (fileIndexPojo.getIsRecommend() != null && fileIndexPojo.getIsRecommend().equals("1")) {  // 是否推荐
                        docFileIndexPojo.setIsRecommend("1");
                    }
                    if (fileIndexPojo.getCreateTime().after(docFileIndexPojo.getCreateTime())) {  // 更新时间
                        docFileIndexPojo.setCreateTime(fileIndexPojo.getCreateTime());
                    }
                }
            }
            docFileIndexPojo.setFileVersionList(list);
        }

        List<DocFileIndexPojo> auditingFileListDaoTmp = new ArrayList<>();
        auditingFileListDaoTmp.addAll(auditingFileListDao);
        for (DocFileIndexPojo docFileIndexPojo : auditingFileListDaoTmp) {
            if (docFileIndexPojo.getFileVersionList().size() == 0) { // 剔除没有'通过'状态版本的文档
                auditingFileListDao.remove(docFileIndexPojo);
            }
            if (fileLineStatusList != null) {  // 根据筛选条件
                if (!fileLineStatusList.contains(docFileIndexPojo.getFileLineStatus())) {
                    auditingFileListDao.remove(docFileIndexPojo);
                }
            }
        }

        // 添加状态中文名
        for (DocFileIndexPojo docFileIndexPojo : auditingFileListDao) {
            if ("offline".equals(docFileIndexPojo.getFileLineStatus())) {
                docFileIndexPojo.setFileLineStatusName("下线");
            } else if ("online".equals(docFileIndexPojo.getFileLineStatus())) {
                docFileIndexPojo.setFileLineStatusName("上线");
            }
        }

        // 按照createtime排序
        auditingFileListDao = new JsonKeyUtils().orderMapTimeList(auditingFileListDao, "createTime");

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(auditingFileListDao);
        }
        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(auditingFileListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", auditingFileListDao.size());
        return TResponseVo.success(resultMap);
    }


    /**
     * 获取文库文档下拉版本记录
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo getLibraryFileVersionListService(String fileUuid) throws Exception {
        List<LabelValuePojo> libraryFileVersionListDao = workingTableMapper.getLibraryFileVersionListDao(fileUuid);
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", libraryFileVersionListDao);
        resultMap.put("total", libraryFileVersionListDao.size());
        return TResponseVo.success(resultMap);
    }


    /**
     * 文库文档上下线操作
     *
     * @param fileUuid
     * @param fileVersionId
     */
    @Override
    public TResponseVo changeLibraryFileStatusService(String fileUuid, String fileVersionId, String fileStatus) throws Exception {
        if (fileStatus != null && !fileStatus.equals("online") && !fileStatus.equals("offline")) {
            return TResponseVo.error("fileStatus入参有误(online或offline)");
        }
        workingTableMapper.changeLibraryFileStatusDao(fileUuid, null, "offline");  // 先都下线
        if (fileStatus.equals("online") && !StringUtils.isBlank(fileVersionId)) {  // 选择性上线一条 保证上线数据唯一
            workingTableMapper.changeLibraryFileStatusDao(fileUuid, fileVersionId, fileStatus);
        }
        return TResponseVo.success("操作完成");
    }

    /**
     * 文库文档删除
     *
     * @param fileUuid
     */
    @Override
    public TResponseVo deleteLibraryFileService(String fileUuid) throws Exception {
        Integer i = workingTableMapper.getLibraryFileLineStatusDao(fileUuid);
        if (i >= 1) {
            return TResponseVo.error("该文档未下线,请下线后操作");
        }
        workingTableMapper.deleteLibraryFileDao(fileUuid, null);
        return TResponseVo.success("操作完成");
    }

    /**
     * 文库文档推荐
     *
     * @param fileUuid
     * @param recommend
     */
    @Override
    public TResponseVo changeLibraryFileRecommendService(String fileUuid, String recommend) throws Exception {
        workingTableMapper.changeLibraryFileRecommendDao(fileUuid, recommend);
        return TResponseVo.success("操作完成");
    }

    /**
     * 新增用户
     *
     * @param
     */
    @Override
    public TResponseVo addUserService(DocUserPojo docUserPojo) throws Exception {
        if (StringUtils.isBlank(docUserPojo.getUserName())) {
            return TResponseVo.error("用户名不能为空");
        }
        if (StringUtils.isBlank(docUserPojo.getUserPhone())) {
            return TResponseVo.error("用户手机号码不能为空");
        }
        String userPhone = docUserPojo.getUserPhone();
        Integer i = workingTableMapper.checkUserDao(userPhone, null);
        if (i >= 1) {
            return TResponseVo.error("该手机号已被注册");
        }
        String userName = docUserPojo.getUserName();
        i = workingTableMapper.checkUserNameDao(userName, null);
        if (i >= 1) {
            return TResponseVo.error("该用户名已存在");
        }
        // 校验手机号
        String regex = "^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$";
        if (!userPhone.matches(regex)) {
            return TResponseVo.error("手机号格式有误");
        }

        String userId = UUID.randomUUID().toString().replaceAll("-", "");
        docUserPojo.setUserId(userId);
        docUserPojo.setCreateTime(new Date());
        docUserPojo.setUserPassword("8ddcff3a80f4189ca1c9d4d902c3c909");
        docUserPojo.setCompanyIdList(docUserPojo.getCompanyId());
        if (StringUtils.isBlank(docUserPojo.getRolesId())) {  // 角色默认系统用户
            docUserPojo.setRolesId("8ddcff3a80f4189ca1c9d4d902c3c909");
        }
        workingTableMapper.addUserDao(docUserPojo);
        return TResponseVo.success("新建用户成功");
    }


    /**
     * 删除用户
     *
     * @param docUserPojo
     */
    @Override
    public TResponseVo delUserService(DocUserPojo docUserPojo) throws Exception {
        String userId = docUserPojo.getUserId();
        workingTableMapper.delUserDao(userId);
        return TResponseVo.success("删除用户成功");
    }

    /**
     * 修改用户
     *
     * @param docUserPojo
     */
    @Override
    public TResponseVo updateUserService(DocUserPojo docUserPojo) throws Exception {
        String userPhone = docUserPojo.getUserPhone();
        Integer i = workingTableMapper.checkUserDao(userPhone, docUserPojo.getUserId());
        if (i >= 1) {
            return TResponseVo.error("该用户名或手机号已被注册");
        }
        String userName = docUserPojo.getUserName();
        i = workingTableMapper.checkUserNameDao(userName, docUserPojo.getUserId());
        if (i >= 1) {
            return TResponseVo.error("该用户名或手机号已被注册");
        }
        // 校验手机号
        String regex = "^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$";
        if (!userPhone.matches(regex)) {
            return TResponseVo.error("手机号格式有误");
        }
        workingTableMapper.updateUserDao(docUserPojo);
        return TResponseVo.success("修改用户成功");
    }

    /**
     * 查询用户列表
     *
     * @param
     */
    @Override
    public TResponseVo getUserListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<DocUserPojo> userInfoListDao = workingTableMapper.getUserInfoListDao(paramNameLike);

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(userInfoListDao);
        }
        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(userInfoListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", userInfoListDao.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 查询用户信息
     *
     * @param docUserPojo
     */
    @Override
    public TResponseVo getUserInfoService(DocUserPojo docUserPojo) throws Exception {
        String userId = docUserPojo.getUserId();
        DocUserPojo userInfoDao = workingTableMapper.getUserInfoDao(userId);
        return TResponseVo.success(userInfoDao);
    }

    /**
     * 重置密码
     *
     * @param docUserPojo
     */
    @Override
    public TResponseVo resetPasswordService(DocUserPojo docUserPojo) throws Exception {
        String userId = docUserPojo.getUserId();
        workingTableMapper.resetPasswordDao(userId);
        return TResponseVo.success("重置完成");
    }

    /**
     * 新增角色
     *
     * @param docUserRolesPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo addRolesService(DocUserRolesPojo docUserRolesPojo) throws Exception {
        String rolesName = docUserRolesPojo.getRolesName();
        if (StringUtils.isBlank(rolesName)) {
            throw new WorkTableException("未定义角色名");
        }
        // 校验角色重名
        Integer integer = workingTableMapper.checkRolesNameDao(rolesName, null);
        if (integer > 0) {
            throw new WorkTableException("角色名已存在");
        }
        String rolesId = UUID.randomUUID().toString().replaceAll("-", ""); // 生成角色id
        docUserRolesPojo.setRolesId(rolesId);
        workingTableMapper.addRolesDao(docUserRolesPojo);
        List<String> menu = docUserRolesPojo.getMenu();
        // 过滤掉menu中的null
        menu = menu.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (menu != null && menu.size() != 0) {
            workingTableMapper.addRolesToMenuDao(rolesId, menu);
        }
        return TResponseVo.success("角色创建完成");
    }

    /**
     * 删除角色
     *
     * @param docUserRolesPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delRolesService(DocUserRolesPojo docUserRolesPojo) throws Exception {
        String rolesId = docUserRolesPojo.getRolesId();
        if (StringUtils.isBlank(rolesId)) {
            throw new WorkTableException("未指定角色Id");
        }
        Integer integer = workingTableMapper.checkRolesUseDao(rolesId);
        if (integer > 0) {
            throw new WorkTableException("该角色下存在用户,不能删除");
        }
        workingTableMapper.delRolesDao(docUserRolesPojo);
//        workingTableMapper.delRolesToMenuDao(rolesId);
        return TResponseVo.success("角色删除完成");
    }

    /**
     * 获取权限清单
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getMenuListService() throws Exception {
        List<DocMenuPojo> menuListDao = workingTableMapper.getMenuListDao();
        List list = new List2TreeUtils().recursionMethod(menuListDao, "menuId", "fatherId");
        return TResponseVo.success(list);
    }

    /**
     * 修改角色
     *
     * @param docUserRolesPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo chgRolesService(DocUserRolesPojo docUserRolesPojo) throws Exception {
        String rolesId = docUserRolesPojo.getRolesId();
        if (StringUtils.isBlank(rolesId)) {
            throw new WorkTableException("未指定角色Id");
        }
        String rolesName = docUserRolesPojo.getRolesName();
        if (StringUtils.isBlank(rolesName)) {
            throw new WorkTableException("未定义角色名");
        }
        // 校验角色重名
        Integer integer = workingTableMapper.checkRolesNameDao(rolesName, rolesId);
        if (integer > 0) {
            throw new WorkTableException("角色名已存在");
        }
        workingTableMapper.chgRolesDao(docUserRolesPojo);
        List<String> menu = docUserRolesPojo.getMenu();
        // 过滤掉menu中的null
        menu = menu.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (menu != null && menu.size() != 0) {
            workingTableMapper.delRolesToMenuDao(rolesId);
            workingTableMapper.addRolesToMenuDao(rolesId, menu);
        }
        return TResponseVo.success("角色修改完成");
    }

    /**
     * 获取角色信息
     *
     * @param docUserRolesPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getRolesInfoService(DocUserRolesPojo docUserRolesPojo) throws Exception {
        String rolesId = docUserRolesPojo.getRolesId();
        if (StringUtils.isBlank(rolesId)) {
            throw new WorkTableException("未指定角色Id");
        }
        DocUserRolesPojo rolesInfoDao = workingTableMapper.getRolesInfoDao(docUserRolesPojo);
        List<DocMenuPojo> rolesToMenuDao = workingTableMapper.getRolesToMenuDao(rolesId);
        ArrayList<String> menu = new ArrayList<>();
        rolesToMenuDao.forEach(k -> {
            menu.add(k.getMenuId());
        });
        List list = new List2TreeUtils().recursionMethod(rolesToMenuDao, "menuId", "fatherId");
        rolesInfoDao.setMenu(menu);
        rolesInfoDao.setMenuTree(list);
        return TResponseVo.success(rolesInfoDao);
    }

    /**
     * 获取角色清单
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getRolesListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<DocUserRolesPojo> myDownloadDao = new ArrayList<>();
        if ("0".equalsIgnoreCase(isPaged)) {
            myDownloadDao = workingTableMapper.getRolesListDao(paramNameLike);
            myDownloadDao.forEach(k -> {
                k.setMenu(stringToList(k.getMenuStr()));
                k.setMenuStr(null);
                k.setMenuName(stringToList(k.getMenuNameStr()));
                k.setMenuNameStr(null);
            });
        } else {
            PageHelper.startPage(pageNum, pageSize);
            myDownloadDao = workingTableMapper.getRolesListDao(paramNameLike);
            myDownloadDao.forEach(k -> {
                k.setMenu(stringToList(k.getMenuStr()));
                k.setMenuStr(null);
                k.setMenuName(stringToList(k.getMenuNameStr()));
                k.setMenuNameStr(null);
            });
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            HashMap<String, Object> retMap = new HashMap<>();
            retMap.put("total", myDownloadDao.size());
            retMap.put("list", myDownloadDao);
            return TResponseVo.success(retMap);
        } else {
            PageInfo pageInfo = new PageInfo(myDownloadDao);
            return TResponseVo.success(pageInfo);
        }
    }

    /**
     * 获取角色列表的LV
     */
    @Override
    public TResponseVo getRolesService() throws Exception {
        List<LabelValuePojo> rolesDao = workingTableMapper.getRolesDao();
        return TResponseVo.success(rolesDao);
    }

    /**
     * 我的贡献
     *
     * @param userId
     */
    @Override
    public TResponseVo getMyContributionService(PagePojo pagePojo, String userId) throws Exception {
        // 标签数据预搜索
        List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);

        List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
        String fileTypeId = String.valueOf(pagePojo.getFileTypeId());  // 模板分类筛选
        if (pagePojo.getFileTypeId() == null || StringUtils.isBlank(fileTypeId)) {
            fileTypeId = null;
        }
        List<String> fileLabelList = pagePojo.getFileLabelList();  // 标签筛选 (只显示全命中的文档)

        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<DocFileIndexPojo> docParamsPojos = workingTableMapper.getCommonUseList2Dao(paramNameLike, null, fileTypeId); // 常用模板
        List<DocFileIndexPojo> docParamsPojos2 = workingTableMapper.getLibraryList2Dao(paramNameLike, null, fileTypeId);  // 母版
        docParamsPojos.addAll(docParamsPojos2);
        List<DocFileIndexPojo> docParamsPojosTmp = new ArrayList<>();
        docParamsPojosTmp.addAll(docParamsPojos);
        for (DocFileIndexPojo docParamsPojo : docParamsPojosTmp) {  // 根据标签过滤数据
            if (!userId.equals(docParamsPojo.getCreateUserId())) {
                docParamsPojos.remove(docParamsPojo);
                continue;
            }
            String fileLabelList2 = docParamsPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            if (!new JsonKeyUtils().containsAll(strings, fileLabelList)) {  // 如果文档的标签完全覆盖'标签筛选'所选内容
                docParamsPojos.remove(docParamsPojo);
            }
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            for (String labelId : strings) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            docParamsPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));
        }

        for (DocFileIndexPojo docParamsPojo : docParamsPojos) {  // 设置文件类型名称
            docParamsPojo.setEdit(false);  // 设置不可编辑
            for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                    docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                    break;
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
     * 申请下载
     *
     * @param fileName      文件名
     * @param fileUuid      fid
     * @param fileVersionId vid
     * @param userId        用户id
     * @param html
     * @param downloadType  下载类型
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getDocxService(String fileName, String fileUuid, String fileVersionId, String userId, String html, String downloadType) throws Exception {
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        String fileStatus = docAllInfoDao.getFileStatus();
        if ("3".equals(fileStatus)) {
            throw new WorkTableException("该文档已被删除，无法下载");
        }

        Integer downloadingCnt = workingTableMapper.getDownloadingCntDao(fileUuid + fileVersionId);
        if (downloadingCnt > 0) {
            throw new WorkTableException("附件中的文件正在生成,请稍候下载");
        }

        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String uid = UUID.randomUUID().toString().replaceAll("-", "");
        String path = downloadPath + "/" + uid;
        Html2DocUtils T1 = new Html2DocUtils(uuid, fileUuid, fileVersionId, fileName, html, path, userId, downloadType);
        Thread task = new Thread(T1);
        task.start();

        // TODO 修改数据库的下载状态
        return TResponseVo.success("请求下载成功");

    }


    /**
     * 获取我的下载列表
     *
     * @param pagePojo
     * @param userId
     */
    @Override
    public TResponseVo getMyDownloadListService(PagePojo pagePojo, String userId) throws Exception {

        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<HfMyDownload> myDownloadDao = new ArrayList<>();
        if ("0".equalsIgnoreCase(isPaged)) {
            myDownloadDao = workingTableMapper.getMyDownloadListDao(userId, paramNameLike);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            myDownloadDao = workingTableMapper.getMyDownloadListDao(userId, paramNameLike);
        }


//        for (HfMyDownload hfMyDownload : myDownloadDao) {
//            hfMyDownload.setFileTypeName("docx");
//            String fileName = hfMyDownload.getFileName();
//            if (fileName.contains("-")) {
//                String[] split = fileName.split("-", 2);
//                hfMyDownload.setFileName(split[1]);
//            }
//        }


        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(myDownloadDao);
        } else {
            PageInfo pageInfo = new PageInfo(myDownloadDao);
            return TResponseVo.success(pageInfo);
        }
    }


    /**
     * 删除我的下载
     *
     * @param uuid
     */
    @Override
    public TResponseVo delMyDownloadService(String uuid) throws Exception {
        HfMyDownload myDownloadInfoDao = workingTableMapper.getMyDownloadInfoDao(uuid);
        String filePath = myDownloadInfoDao.getFilePath();
        File file = new File(filePath);
        file.delete();
        workingTableMapper.delMyDownloadDao(uuid);
        return TResponseVo.success("删除完成");
    }


    /**
     * 下载我的下载
     *
     * @param uuid
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo downloadMyDownloadService(String uuid, String userId, HttpServletResponse response) throws Exception {
        HfMyDownload myDownloadInfoDao = workingTableMapper.getMyDownloadInfoDao(uuid);

        String filePath = myDownloadInfoDao.getFilePath();
        String fileName = myDownloadInfoDao.getFileName();
        String fileType = myDownloadInfoDao.getFileType();

        File file = new File(filePath);
        if (file.exists()) {
            Path path = file.toPath();
            String mimeType = Files.probeContentType(path);
            //response为HttpServletResponse对象
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + "." + fileType);
            response.setContentType(mimeType);
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                OutputStream outputStream = response.getOutputStream();
                int i = bis.read(buffer);
                while (i != -1) {
                    outputStream.write(buffer, 0, i);
                    i = bis.read(buffer);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
//                file.delete();
        } else {
            return TResponseVo.error("下载失败,文件不存在或已删除");
        }
        // 记录
        String logId = UUID.randomUUID().toString().replaceAll("-", "");
        workingTableMapper.newDownloadRecordDao(userId, uuid, logId);
        return TResponseVo.success("下载成功");
    }

    /**
     * 查看下载日志
     *
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getDownloadRecordListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<HfMyDownloadRecordPojo> myDownloadRecordDao = new ArrayList<>();
        if ("0".equalsIgnoreCase(isPaged)) {
            myDownloadRecordDao = workingTableMapper.getDownloadRecordListDao(paramNameLike);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            myDownloadRecordDao = workingTableMapper.getDownloadRecordListDao(paramNameLike);
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(myDownloadRecordDao);
        } else {
            PageInfo pageInfo = new PageInfo(myDownloadRecordDao);
            return TResponseVo.success(pageInfo);
        }
    }

    /**
     * 新增评审模型
     *
     * @param hfAssessModelPojo
     */
    @Override
    public TResponseVo newAssessModelService(HfAssessModelPojo hfAssessModelPojo) throws Exception {
        String assessId = UUID.randomUUID().toString().replaceAll("-", "");
        List<String> labelList = (List) hfAssessModelPojo.getLabel();
        if (labelList == null || labelList.size() == 0) {
            hfAssessModelPojo.setLabel(null);
        } else {
            String label = JSON.toJSONString(labelList);
            hfAssessModelPojo.setLabel(label);
        }
        hfAssessModelPojo.setAssessId(assessId);
        try {
            Integer i = workingTableMapper.newAssessModelDao(hfAssessModelPojo);
        } catch (Exception e) {
            FileIndexException fileIndexException = new FileIndexException();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newAssessModelService", "", "执行sql命令时出错,请联系开发人员");
            fileIndexException.setCode(50);
            throw fileIndexException;
        }
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "新增评审模型完成");
        ret.put("assessId", assessId);
        return TResponseVo.success(ret);
    }

    /**
     * 删除评审模型
     *
     * @param assessId
     */
    @Override
    public TResponseVo delAssessModelService(String assessId) throws Exception {
        if (StringUtils.isBlank(assessId)) {
            return TResponseVo.error("assessId不能为空");
        }
        try {
            Integer i = workingTableMapper.delAssessModelDao(assessId);
        } catch (Exception e) {
            FileIndexException fileIndexException = new FileIndexException();
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newAssessModelService", "", "执行sql命令时出错,请联系开发人员");
            fileIndexException.setCode(50);
            throw fileIndexException;
        }
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "删除评审模型完成");
        return TResponseVo.success(ret);
    }

    /**
     * 修改评审模型
     *
     * @param hfAssessModelPojo
     */
    @Override
    public TResponseVo updateAssessModelService(HfAssessModelPojo hfAssessModelPojo) throws Exception {
        List<String> labelList = (List) hfAssessModelPojo.getLabel();
        if (labelList == null || labelList.size() == 0) {
            hfAssessModelPojo.setLabel(null);
        } else {
            String label = JSON.toJSONString(labelList);
            hfAssessModelPojo.setLabel(label);
        }
        workingTableMapper.updateAssessModelDao(hfAssessModelPojo);
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "修改评审模型完成");
        return TResponseVo.success(ret);
    }

    /**
     * 获取评审模型信息
     *
     * @param assessId
     */
    @Override
    public TResponseVo getAssessModelInfoService(String assessId) throws Exception {
        HfAssessModelPojo assessModelInfoDao = workingTableMapper.getAssessModelInfoDao(assessId);
        String label = (String) assessModelInfoDao.getLabel();
        List<String> list = JSON.parseObject(label, List.class);
        if (label == null) {
            list = new ArrayList<>();
        }
        assessModelInfoDao.setLabel(list);

        // 获取标签数据
        List<LabelValuePojo> labelListDao = fileOperationMapper.getLabelLVListDao(null);
        ArrayList<String> labelName = new ArrayList<>();
        if (list != null) {
            for (String s : list) {
                for (LabelValuePojo labelValuePojo : labelListDao) {
                    if (s.equals(labelValuePojo.getValue())) {
                        labelName.add(labelValuePojo.getLabel());
                        break;
                    }
                }
            }
        }
        assessModelInfoDao.setLabelName(labelName);

        // 选项名
        List<OptionsPojo> optionsDao = workingTableMapper.getOptionsDao(null, null);
        List<OptionsPojo> optionsDaoTmp = new ArrayList<>();
        optionsDaoTmp.addAll(optionsDao);
        for (OptionsPojo optionsPojo : optionsDaoTmp) {
            if (!"assessMethod".equals(optionsPojo.getType()) && !"assessLink".equals(optionsPojo.getType()) && !"assessType".equals(optionsPojo.getType()) && !"modelType".equals(optionsPojo.getType())) {
                optionsDao.remove(optionsPojo);
            }
        }
        for (OptionsPojo optionsPojo : optionsDao) {
            // 选项名
            if (optionsPojo.getValue().equals(assessModelInfoDao.getAssessMethod())) {
                assessModelInfoDao.setAssessMethodName(optionsPojo.getLabel());
                continue;
            }
            if (optionsPojo.getValue().equals(assessModelInfoDao.getAssessLink())) {
                assessModelInfoDao.setAssessLinkName(optionsPojo.getLabel());
                continue;
            }
            if (optionsPojo.getValue().equals(assessModelInfoDao.getAssessType())) {
                assessModelInfoDao.setAssessTypeName(optionsPojo.getLabel());
                continue;
            }
            if (optionsPojo.getValue().equals(assessModelInfoDao.getModelType())) {
                assessModelInfoDao.setModelTypeName(optionsPojo.getLabel());
            }
        }
        return TResponseVo.success(assessModelInfoDao);
    }

    /**
     * 获取评审模型清单
     *
     * @param pagePojo
     * @param userId
     */
    @Override
    public TResponseVo getAssessModelListService(PagePojo pagePojo, String userId) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        // 获取标签数据
        List<LabelValuePojo> labelListDao = fileOperationMapper.getLabelLVListDao(null);
        // 获取选项数据
        List<OptionsPojo> optionsDao = workingTableMapper.getOptionsDao(null, null);
        List<OptionsPojo> optionsDaoTmp = new ArrayList<>();
        optionsDaoTmp.addAll(optionsDao);
        for (OptionsPojo optionsPojo : optionsDaoTmp) {
            if (!"assessMethod".equals(optionsPojo.getType()) && !"assessLink".equals(optionsPojo.getType()) && !"assessType".equals(optionsPojo.getType()) && !"modelType".equals(optionsPojo.getType())) {
                optionsDao.remove(optionsPojo);
            }
        }

        List<HfAssessModelPojo> assessModelListDao = workingTableMapper.getAssessModelListDao(paramNameLike);
        for (HfAssessModelPojo hfAssessModelPojo : assessModelListDao) {
            String label = (String) hfAssessModelPojo.getLabel();
            List<String> list = JSON.parseObject(label, List.class);
            if (label == null) {
                list = new ArrayList<>();
            }
            hfAssessModelPojo.setLabel(list);
            // 标签名
            ArrayList<String> labelName = new ArrayList<>();
            if (list != null) {
                for (String s : list) {
                    for (LabelValuePojo labelValuePojo : labelListDao) {
                        if (s.equals(labelValuePojo.getValue())) {
                            labelName.add(labelValuePojo.getLabel());
                            break;
                        }
                    }
                }
            }
            hfAssessModelPojo.setLabelName(labelName);

            // 选项名
            for (OptionsPojo optionsPojo : optionsDao) {
                if (optionsPojo.getValue().equals(hfAssessModelPojo.getAssessMethod())) {
                    hfAssessModelPojo.setAssessMethodName(optionsPojo.getLabel());
                    continue;
                }
                if (optionsPojo.getValue().equals(hfAssessModelPojo.getAssessLink())) {
                    hfAssessModelPojo.setAssessLinkName(optionsPojo.getLabel());
                    continue;
                }
                if (optionsPojo.getValue().equals(hfAssessModelPojo.getAssessType())) {
                    hfAssessModelPojo.setAssessTypeName(optionsPojo.getLabel());
                    continue;
                }
                if (optionsPojo.getValue().equals(hfAssessModelPojo.getModelType())) {
                    hfAssessModelPojo.setModelTypeName(optionsPojo.getLabel());
                }
            }
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(assessModelListDao);
        }

        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(assessModelListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", assessModelListDao.size());
        return TResponseVo.success(resultMap);
    }


    /**
     * 新增评审要素
     *
     * @param hfAssessModelElementPojo
     */
    @Override
    public TResponseVo newAssessModelElementService(HfAssessModelElementPojo hfAssessModelElementPojo) throws Exception {
        String assessId = hfAssessModelElementPojo.getAssessId();
//        HfAssessModelPojo assessModelInfoDao = workingTableMapper.getAssessModelInfoDao(assessId);
        // 二级数据
        List<HfAssessModelElementPojo> elementList = hfAssessModelElementPojo.getElementList();
        if (elementList == null) {
            return TResponseVo.error("评审要素内容为空");
        }
        // 公共数据
        String elementType = hfAssessModelElementPojo.getElementType();
        String createUserId = hfAssessModelElementPojo.getCreateUserId();

        // 设置元素顺序字段
        // 判断当前模型中同类评审类型下有无已存在数据 如果存在就根据最大值继续增加 若不存在从1开始
        Integer order = workingTableMapper.getAssessModelElementListCntDao(assessId, elementType);
        if (order == null) {
            order = 0;
        }

        // 设置条款顺序字段
        // 先判断当前elementType是否已存在  存在就取现成的顺序
        Integer elementTypeOrder = workingTableMapper.getAssessModelTypeCntDao(assessId, elementType) + 1; // 当前有多少种类
        String elementTypeOrderDao = workingTableMapper.getElementTypeOrderDao(assessId, elementType); // 试图寻找新增类型在当前模型中的应用
        if (elementTypeOrderDao != null) {
            elementTypeOrder = Integer.valueOf(elementTypeOrderDao);
        }


        for (HfAssessModelElementPojo assessModelElementPojo : elementList) {
            assessModelElementPojo.setAssessId(assessId);
            String elementId = UUID.randomUUID().toString().replaceAll("-", "");
            assessModelElementPojo.setElementId(elementId);
            assessModelElementPojo.setElementType(elementType);
            assessModelElementPojo.setCreateUserId(createUserId);

            Map<String, String> quantizationStandard = (Map) assessModelElementPojo.getQuantizationStandard();
            if (quantizationStandard == null) {
                assessModelElementPojo.setQuantizationStandard(null);
            } else {
                String s = JSON.toJSONString(quantizationStandard);
                assessModelElementPojo.setQuantizationStandard(s);
            }
            Map<String, String> elementStandard = (Map) assessModelElementPojo.getElementStandard();
            if (elementStandard == null) {
                assessModelElementPojo.setElementStandard(null);
            } else {
                String s = JSON.toJSONString(elementStandard);
                assessModelElementPojo.setElementStandard(s);
            }

            // 级差
            Object gradation = assessModelElementPojo.getGradation();
            if (gradation == null) {
                assessModelElementPojo.setGradation(null);
            } else {
                assessModelElementPojo.setGradation(String.valueOf(gradation));
            }
            // 阈值
            List<Object> thresholdValue = (List) assessModelElementPojo.getThresholdValue();
            if (thresholdValue == null) {
                assessModelElementPojo.setThresholdValue(null);
            } else {
                assessModelElementPojo.setThresholdValue(JSON.toJSONString(thresholdValue));
            }
            // 顺序
            order += 1;
            assessModelElementPojo.setOrder(String.valueOf(order));
            // 类型顺序
            assessModelElementPojo.setElementTypeOrder(String.valueOf(elementTypeOrder));
            // 投标人参数
            List<String> paramsIdList = (List) assessModelElementPojo.getParamsIdList();
            if (paramsIdList == null) {
                assessModelElementPojo.setParamsIdList(null);
            } else {
                assessModelElementPojo.setParamsIdList(JSON.toJSONString(paramsIdList));
            }
            workingTableMapper.newAssessModelElementDao(assessModelElementPojo);
        }

        return TResponseVo.success("创建完成");
    }

    /**
     * 删除评审要素
     *
     * @param assessId
     * @param elementType
     */
    @Override
    public TResponseVo delAssessModelElementService(String assessId, String elementType) throws Exception {
        workingTableMapper.delAssessModelElementDao(assessId, elementType);
        return TResponseVo.success("删除完成");
    }

    /**
     * 修改评审要素
     *
     * @param hfAssessModelElementPojo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo updateAssessModelElementService(HfAssessModelElementPojo hfAssessModelElementPojo) throws Exception {
        String assessId = hfAssessModelElementPojo.getAssessId();
        HfAssessModelPojo assessModelInfoDao = workingTableMapper.getAssessModelInfoDao(assessId);

        // 公共数据
        String elementType = hfAssessModelElementPojo.getElementType();
        String createUserId = hfAssessModelElementPojo.getCreateUserId();

        // 设置顺序字段
        Integer order = 0;

        // 设置条款顺序字段
        // 先判断当前elementType是否已存在  存在就取现成的顺序
        Integer elementTypeOrder = workingTableMapper.getAssessModelTypeCntDao(assessId, elementType) + 1; // 当前有多少种类
        String elementTypeOrderDao = workingTableMapper.getElementTypeOrderDao(assessId, elementType); // 试图寻找新增类型在当前模型中的应用
        if (elementTypeOrderDao != null) {
            elementTypeOrder = Integer.valueOf(elementTypeOrderDao);
        }

        // 全删全增
        workingTableMapper.delAssessModelElementDao(assessId, elementType);

        // 二级数据
        List<HfAssessModelElementPojo> elementList = hfAssessModelElementPojo.getElementList();
        for (HfAssessModelElementPojo assessModelElementPojo : elementList) {
            assessModelElementPojo.setAssessId(assessId);
            String elementId = UUID.randomUUID().toString().replaceAll("-", "");
            assessModelElementPojo.setElementId(elementId);
            assessModelElementPojo.setElementType(elementType);
            assessModelElementPojo.setCreateUserId(createUserId);

            Map<String, String> quantizationStandard = (Map) assessModelElementPojo.getQuantizationStandard();
            if (quantizationStandard == null) {
                assessModelElementPojo.setQuantizationStandard(null);
            } else {
                String s = JSON.toJSONString(quantizationStandard);
                assessModelElementPojo.setQuantizationStandard(s);
            }
            Map<String, String> elementStandard = (Map) assessModelElementPojo.getElementStandard();
            if (elementStandard == null) {
                assessModelElementPojo.setElementStandard(null);
            } else {
                String s = JSON.toJSONString(elementStandard);
                assessModelElementPojo.setElementStandard(s);
            }

            // 级差
            Object gradation = assessModelElementPojo.getGradation();
            if (gradation == null) {
                assessModelElementPojo.setGradation(null);
            } else {
                assessModelElementPojo.setGradation(String.valueOf(gradation));
            }
            // 阈值
            List<Object> thresholdValue = (List) assessModelElementPojo.getThresholdValue();
            if (thresholdValue == null) {
                assessModelElementPojo.setThresholdValue(null);
            } else {
                assessModelElementPojo.setThresholdValue(JSON.toJSONString(thresholdValue));
            }
            // 顺序
            order += 1;
            assessModelElementPojo.setOrder(String.valueOf(order));
            // 类型顺序
            assessModelElementPojo.setElementTypeOrder(String.valueOf(elementTypeOrder));
            // 投标人参数
            List<String> paramsIdList = (List) assessModelElementPojo.getParamsIdList();
            if (paramsIdList == null) {
                assessModelElementPojo.setParamsIdList(null);
            } else {
                assessModelElementPojo.setParamsIdList(JSON.toJSONString(paramsIdList));
            }
            workingTableMapper.newAssessModelElementDao(assessModelElementPojo);
        }

        return TResponseVo.success("修改完成");
    }

    /**
     * 查询评审要素
     *
     * @param assessId
     * @param elementType
     */
    @Override
    public TResponseVo getAssessModelElementInfoService(String assessId, String elementType) throws Exception {
        List<HfAssessModelElementPojo> assessModelElementList = workingTableMapper.getAssessModelElementListByTypeDao(assessId, elementType);
        List<HfAssessModelElementPojo> elementList = new ArrayList<>();
        for (HfAssessModelElementPojo assessModelElementInfoDao : assessModelElementList) {
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
                assessModelElementInfoDao.setParamsIdList(JSON.parseObject(paramsIdList, List.class));
            }

            elementList.add(assessModelElementInfoDao);
        }
        // 顺序
        // 对二维数组中的元素排序
        List<HfAssessModelElementPojo> retList = new JsonKeyUtils().orderMapList5(elementList);

        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("assessId", assessId);
        retMap.put("elementType", elementType);
        retMap.put("elementList", retList);
        return TResponseVo.success(retMap);
    }

    /**
     * 查询评审要素
     *
     * @param pagePojo
     * @param userId
     */
    @Override
    public TResponseVo getAssessModelElementListService(PagePojo pagePojo, String userId) throws Exception {
        String type = pagePojo.getType();
        if (type == null) {
            String isPaged = pagePojo.getIsPaged();
            Integer pageNum = pagePojo.getPageNum();
            Integer pageSize = pagePojo.getPageSize();
            String paramNameLike = pagePojo.getParamNameLike();
            String assessId = pagePojo.getAssessId();

            if (StringUtils.isNotBlank(paramNameLike)) {
                paramNameLike = paramNameLike.replaceAll("'", "");
            }

            List<HfAssessModelElementPojo> assessModelListDao = workingTableMapper.getAssessModelElementListDao(paramNameLike, assessId);
//            Integer min = 0;
//            Integer max = 0;
            for (HfAssessModelElementPojo assessModelElementInfoDao : assessModelListDao) {
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
                    List<Integer> list = JSON.parseObject(thresholdValue, List.class);
//                    min = min + list.get(0);
//                    max = max + list.get(1);
                    assessModelElementInfoDao.setThresholdValue(list);
                }
                // 投标人参数
                String paramsIdList = (String) assessModelElementInfoDao.getParamsIdList();
                if (paramsIdList == null) {
                    assessModelElementInfoDao.setParamsIdList(null);
                } else {
                    assessModelElementInfoDao.setParamsIdList(JSON.parseObject(paramsIdList, List.class));
                }
            }

            if ("0".equalsIgnoreCase(isPaged)) {
                return TResponseVo.success(assessModelListDao);
            }

            /**
             * 分页
             */
            List<Map<Object, Object>> mapList = new ListPageUtils().test0(assessModelListDao, pageSize, pageNum);

            /**
             * 构建返回内容
             */
            HashMap<String, Object> resultMap = new HashMap<>();
            resultMap.put("list", mapList);
            resultMap.put("total", assessModelListDao.size());
            return TResponseVo.success(resultMap);
        } else {
            List<LabelValuePojo> assessModelParamDao = workingTableMapper.getAssessModelParamDao(null, null);  // 获取参数名

            String assessId = pagePojo.getAssessId();
            List<HfAssessModelElementPojo> assessModelListDao = workingTableMapper.getAssessModelElementListDao(null, assessId);
            for (HfAssessModelElementPojo assessModelElementInfoDao : assessModelListDao) {
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
                Map<String, String> map = null;
                map = (Map) assessModelElementInfoDao.getElementStandard();
                if (map == null) {
                    map = (Map) assessModelElementInfoDao.getQuantizationStandard();
                }
                if (map == null) {
                    assessModelElementInfoDao.setStandardText(null);
                } else if ("text".equals(map.get("type"))) {
                    assessModelElementInfoDao.setStandardText(map.get("value"));
                } else {  // 是参数
                    for (LabelValuePojo labelValuePojo : assessModelParamDao) {
                        if (map.get("value") != null && map.get("value").equals(labelValuePojo.getValue())) {
//                            assessModelElementInfoDao.setParamsName();
                            assessModelElementInfoDao.setStandardText("【" + labelValuePojo.getLabel() + "】");
                            break;
                        }
                    }
                }
            }

            // 创建二维数组
            ArrayList<String> elementTypeList = new ArrayList<>();
            ArrayList<List<HfAssessModelElementPojo>> lists = new ArrayList<>();
            for (HfAssessModelElementPojo hfAssessModelElementPojo : assessModelListDao) {
                String elementType = hfAssessModelElementPojo.getElementType();
                if (elementTypeList.contains(elementType)) {
                    for (List<HfAssessModelElementPojo> list : lists) {
                        if (elementType.equals(list.get(0).getElementType())) {
                            list.add(hfAssessModelElementPojo);
                            break;
                        }
                    }
                } else {
                    ArrayList<HfAssessModelElementPojo> hfAssessModelElementList = new ArrayList<>();
                    elementTypeList.add(elementType);
                    hfAssessModelElementList.add(hfAssessModelElementPojo);
                    lists.add(hfAssessModelElementList);
                }
            }
            // 对二维数组中的元素排序
            ArrayList<List<HfAssessModelElementPojo>> listsTmp = new ArrayList<>();
            listsTmp.addAll(lists);
            for (List<HfAssessModelElementPojo> list : listsTmp) {
                List<HfAssessModelElementPojo> retList = new JsonKeyUtils().orderMapList5(list);
                lists.remove(list);
                lists.add(retList);
            }
            // 获取条款顺序
            List<HfAssessModelElementPojo> elementTypeOrderListDao = workingTableMapper.getElementTypeOrderListDao(assessId);
            ArrayList<List<HfAssessModelElementPojo>> listsTmp2 = new ArrayList<>();
            for (HfAssessModelElementPojo hfAssessModelElementPojo : elementTypeOrderListDao) {
                String elementType = hfAssessModelElementPojo.getElementType();
                for (List<HfAssessModelElementPojo> h : lists) {
                    if (elementType.equals(h.get(0).getElementType())) {
                        listsTmp2.add(h);
                        break;
                    }
                }
            }
            return TResponseVo.success(listsTmp2);
        }
    }

    /**
     * 调整评审条款顺序
     *
     * @param hfAssessModelElementPojo
     */
    @Override
    public TResponseVo updateAssessModelOrderService(HfAssessModelElementPojo hfAssessModelElementPojo) throws Exception {
        String assessId = hfAssessModelElementPojo.getAssessId();
        List<String> elementTypeList = hfAssessModelElementPojo.getElementTypeList();
        List<HfAssessModelElementPojo> assessModelElementListDao = workingTableMapper.getAssessModelElementListDao(null, assessId);
        int i = 1;
        for (String s : elementTypeList) {
            ArrayList<String> strings = new ArrayList<>();
            for (HfAssessModelElementPojo assessModelElementPojo : assessModelElementListDao) {
                if (assessModelElementPojo.getElementType().equals(s)) {
                    strings.add(assessModelElementPojo.getElementId());
                }
            }
            String elementIdList = new JsonKeyUtils().listToString(strings, "','");
            workingTableMapper.updateAssessModelOrderDao(String.valueOf(i), elementIdList);
            i += 1;
        }
        return TResponseVo.success("顺序变更完成");
    }

    /**
     * 参数下拉列表
     *
     * @param
     */
    @Override
    public TResponseVo getAssessModelParamService(HfAssessModelElementPojo hfAssessModelElementPojo, String userId) throws Exception {
        String paramsUseSceneId = hfAssessModelElementPojo.getParamsUseSceneId();
        List<LabelValuePojo> assessModelParamDao = workingTableMapper.getAssessModelParamDao(paramsUseSceneId, userId);
        return TResponseVo.success(assessModelParamDao);
    }

    /**
     * 招采库
     *
     * @param
     */
    @Override
    public TResponseVo getZhaocaiGlobalViewService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        return null;
//        // 定义nodes 用来保存图数据库的点数据
//        List<Map<String, Object>> nodes = new ArrayList<>();
//        List<String> ids = new ArrayList<>(); // 记录node的id 防止数据重复
//        // 定义edges 用来保存图数据库的线数据
//        List<Map<String, Object>> edges = new ArrayList<>();
//
//        // 筛选信息
//        Integer level = docFileIndexPojo.getLevel();  // 图层
//        String id = docFileIndexPojo.getId();  // 节点id
//
//        if (1 == level) {
//            KnowledgeClassEntity zhaocai = knowledgeClassEntityRepository.findByName("招采库");
//            HashMap<String, Object> map0 = new HashMap<>();  // 知识体系对象
//            map0.put("id", zhaocai.getId().toString());
//            map0.put("label", zhaocai.getName());
//            HashMap<String, Object> mapData0 = new HashMap<>();
//            mapData0.put("type", "0");
//            mapData0.put("level", level);
////            mapData1.put("tags", labelContentList);
////            mapData1.put("active", active);
//            map0.put("data", mapData0);
//            nodes.add(map0);
//            Set<KnowledgeClassEntity> KnowledgeClassEntitySet = zhaocai.getKnowledgeClassEntitySet();  // 获取知识体系分类
//            for (KnowledgeClassEntity knowledgeClassEntity : KnowledgeClassEntitySet) {
//                HashMap<String, Object> map1 = new HashMap<>();  // 知识体系对象
//                map1.put("id", knowledgeClassEntity.getId().toString());
//                map1.put("label", knowledgeClassEntity.getName());
//                HashMap<String, Object> mapData1 = new HashMap<>();
//                mapData1.put("type", "1");
//                mapData1.put("level", level);
////            mapData1.put("tags", labelContentList);
////            mapData1.put("active", active);
//                map1.put("data", mapData1);
//                nodes.add(map1);
//                Set<LawsAndRegulationsEntity> LawsAndRegulationsEntityList = knowledgeClassEntityRepository.findByName(knowledgeClassEntity.getName()).getActors(); // 获取法律法规
//                if (LawsAndRegulationsEntityList != null) {
//                    for (LawsAndRegulationsEntity LawsAndRegulation : LawsAndRegulationsEntityList) {
//                        HashMap<String, Object> map2 = new HashMap<>();  // 法律法规
//                        map2.put("id", LawsAndRegulation.getId().toString());
//                        map2.put("label", LawsAndRegulation.getName());
//                        HashMap<String, Object> mapData2 = new HashMap<>();
//                        mapData2.put("type", "2");
//                        mapData2.put("level", level);
//                        map2.put("data", mapData2);
//                        nodes.add(map2);
//                        Set<LawForceEntity> LawForceEntityList = lawsAndRegulationsEntityRepository.findByName(LawsAndRegulation.getName()).getActors();  // 获取法律效力
//                        if (LawForceEntityList != null) {
//                            for (LawForceEntity lawForceEntity : LawForceEntityList) {
//                                HashMap<String, Object> map3 = new HashMap<>();  // 效力层级
//                                map3.put("id", lawForceEntity.getId().toString());
//                                map3.put("label", lawForceEntity.getName());
//                                HashMap<String, Object> mapData3 = new HashMap<>();
//                                mapData3.put("type", "3");
//                                mapData3.put("level", level);
//                                map3.put("data", mapData3);
//                                nodes.add(map3);
//                                // 储存线信息
//                                HashMap<String, Object> line = new HashMap<>();
//                                line.put("target", LawsAndRegulation.getId().toString());
//                                line.put("source", lawForceEntity.getId().toString());
//                                line.put("label", "属于");
//                                edges.add(line);
//                            }
//                        }
//                        // 储存线信息
//                        HashMap<String, Object> line = new HashMap<>();
//                        line.put("target", knowledgeClassEntity.getId().toString());
//                        line.put("source", LawsAndRegulation.getId().toString());
//                        line.put("label", "属于");
//                        edges.add(line);
//                    }
//                }
//                // 储存线信息
//                HashMap<String, Object> line = new HashMap<>();
//                line.put("target", zhaocai.getId().toString());
//                line.put("source", knowledgeClassEntity.getId().toString());
//                line.put("label", "属于");
//                edges.add(line);
//            }
//        } else if (2 == level) {
//            if (id == null) {
//                return TResponseVo.error("未传入节点id");
//            }
//            Optional<LawForceEntity> byId = lawForceEntityRepository.findById(Long.valueOf(id));
//            LawForceEntity lawForceEntity = byId.get();
//
//            HashMap<String, Object> map3 = new HashMap<>();  // 效力层级
//            map3.put("id", lawForceEntity.getId().toString());
//            map3.put("label", lawForceEntity.getName());
//            HashMap<String, Object> mapData3 = new HashMap<>();
//            mapData3.put("type", "3");
//            mapData3.put("level", level);
//            map3.put("data", mapData3);
//            nodes.add(map3);
//            Set<LawEntity> lawList = lawForceEntity.getActors();// 获取法律
//            if (lawList != null) {
//                List<String> labelList = docFileIndexPojo.getLabelList();
//                List<String> isValidList = new ArrayList<>();
//                if (labelList != null && labelList.size() != 0) {
//                    // 把有效失效从标签中分离开
//                    if (labelList.contains("有效")) {
//                        isValidList.add("有效");
//                        labelList.remove("有效");
//                    }
//                    if (labelList.contains("失效")) {
//                        isValidList.add("失效");
//                        labelList.remove("失效");
//                    }
//                }
//
//                for (LawEntity lawEntity : lawList) {
//                    boolean flag = false;
//                    if (labelList != null && labelList.size() != 0) {
//                        String lawEntityPublishUnit = lawEntity.getPublishUnit();
//                        for (String label : labelList) {
//                            // 再判断是否包含
//                            if (lawEntityPublishUnit.contains(label.replaceAll("\\d+", ""))) {
//                                flag = true;
//                                break;
//                            }
//                        }
//                    } else {
//                        flag = true;
//                    }
//
//                    // 根据选中的有效与无效标签统一计算
//                    String isValid = lawEntity.getIsValid();
//                    if (isValidList.contains("有效") && isValidList.contains("失效")) {
//
//                    } else if (isValidList.contains("有效")) {
//                        if (isValid.equals("失效")) {
//                            flag = false;
//                        }
//                    } else if (isValidList.contains("失效")) {
//                        if (isValid.equals("有效")) {
//                            flag = false;
//                        }
//                    }
//
//                    if (flag) {
//                        HashMap<String, Object> map4 = new HashMap<>();  // 法律
//                        map4.put("id", lawEntity.getId().toString());
//                        map4.put("label", lawEntity.getName());
//                        HashMap<String, Object> mapData4 = new HashMap<>();
//                        mapData4.put("type", "4");
//                        mapData4.put("level", level);
//                        mapData4.put("publishDate", lawEntity.getPublishDate());
//                        mapData4.put("publishUnit", lawEntity.getPublishUnit());
//                        mapData4.put("useDate", lawEntity.getUseDate());
//                        mapData4.put("isValid", lawEntity.getIsValid());
//                        map4.put("data", mapData4);
//                        nodes.add(map4);
//                        // 储存线信息
//                        HashMap<String, Object> line = new HashMap<>();
//                        line.put("target", id.toString());
//                        line.put("source", lawEntity.getId().toString());
//                        line.put("label", "属于");
//                        edges.add(line);
//                    }
//                }
//            }
//        } else if (3 == level) {
//            if (id == null) {
//                return TResponseVo.error("未传入节点id");
//            }
//
//            HashMap<String, String> map = new HashMap<String, String>() {
//                {
//                    put("《北京市住房和城乡建设委员会关于进一步落实招标人主体责任加强招标投标服务监管有关工作的通知（2022）》", "【每日招言】北京市发文，明确类似项目业绩认定渠道");
//                    put("《关于印发落实建设单位工程质量首要责任实施意见的通知（2021）》", "江苏：推进施工过程结算，进度款支付比例不低于60%！12月10日起施行！");
//                    put("《山东省住房和城乡建设厅山东省发展和改革委员会山东省财政厅关于印发山东省工程造价改革实施方案的通知（2021）》", "住建厅：全面推行施工过程结算，取消最高投标限价按“定额计价”规定！");
//                    put("《国家发展改革委关于进一步推进投资项目审批制度改革的若干意见（2021）》", "要闻│国家发展改革委关于进一步推进投资项目审批制度改革的若干意见");
//                    put("《省发展改革委关于印发《浙江省工程建设项目招标投标“评定分离”试点定标操作指引（试行）》的通知（2022）》", "招投标大改！中标候选人3人，不排序！又一省发文推进");
//                    put("《国务院关于投资体制改革的决定（2004）》", "5-4什么是审批制、核准制、备案制？");
//                    put("《广州市财政局关于印发广州市政府采购文件编制指导意见（2021年修订）的通知（2021）》", "知招带你看丨招采要闻（02.07-02.13）");
//                    put("《山东省政府采购履约验收管理办法（2022）》", "山东：政采项目验收标准应包括所有客观量化指标");
//                    put("《关于印发《上海市建设项目工程总承包招标评标办法》的通知(2022)》", "【建纬观点】《上海市建设项目工程总承包招标评标办法》解读");
//                    put("《北京市发展和改革委员会关于进一步推动优化招标投标领域营商环境改革的通知（2022）》", "");
//                    put("《财政部关于在政府采购活动中落实平等对待内外资企业有关政策的通知(2021)》", "重磅|财政部发文强调政府采购平等对待内外资企业");
//                    put("《关于加快推进全过程工程咨询服务发展的实施意见（征求意见稿）2021》", "又一省发布《关于加快推进全过程工程咨询服务发展的实施意见（征求意见稿）》");
//                    put("《四川省住房和城乡建设厅关于建设工程合同中价格风险约定和价格调整的指导意见（征求意见稿）2021》", "招标文件/施工合同中采用“无限风险”等规定计价风险的，予以处罚！该省发文");
//                    put("《加快转变建筑业发展方式推动建筑强省建设工作方案（2022）》", "住建厅发文：即日起，特级和总包壹级企业，可将其低等级总包资质分立至省内全资子公司！");
//                    put("《关于全省房屋建筑和市政基础设施工程建设项目招标投标实行全过程信息公开的通知（2022）》", "【每日招言】江西省发文，要求全省房建市政工程招投标实行全过程信息公开");
//                    put("《关于进一步规范本级政府采购代理机构代理行为的通知（2022）》", "采购代理机构设置评审因素时不宜使用哪些表述？西藏发文规定了");
//                    put("《关于印发《四川省远程异地评标管理暂行办法》的通知（2021）》", "7月1日起，这3类项目应当采用远程异地评标！该省印发《远程异地评标管理暂行办法》");
//                    put("《江苏省各级党政机关、事业单位及团体组织公务用车框架协议采购（新能源汽车）（征求意见稿）（2022）》", "江苏｜又一框架协议采购文件公开征求意见");
//                    put("《河南省财政厅关于防范供应商串通投标促进政府采购公平竞争的通知（2022）》", "知招带你看丨招采一周要闻（01.03-01.09）");
//                    put("《江苏省党政机关、事业单位及团体组织服务器框架协议采购公开招标采购文件（征求意见稿）（2022）》", "江苏｜又一框架协议采购文件公开征求意见");
//                    put("《关于简化政府采购供应商资格审查有关事项的通知（2022）》", "上海简化政府采购供应商资格审查流程");
//                    put("《广东省建设工程监理条例（2021）》", "广东省|监理工程师因严重失职或过错，造成重大质量和重大伤亡事故，最高可处终身不予注册​");
//                    put("《关于进一步夯实招标人主体责任的通知（2022）》", "动态关注│南京：9月1日起，建立招标人责任追究终身制");
//                    put("《注册造价工程师（水利工程）管理办法（2021）》", "水利部印发《注册造价工程师（水利工程）管理办法》");
//                    put("《住房和城乡建设部、人力资源社会保障部关于修改《建筑工人实名制管理办法（试行）》的通知（2022）》", "");
//                    put("《拖欠农民工工资失信联合惩戒对象名单管理暂行办法（2021）》", "人社部：这些情形将列入失信联合惩戒！限制招投标/市场准入/评优评先……");
//                    put("《住房和城乡建设行政处罚程序规定（2022）》", "住建部公布《住房和城乡建设行政处罚程序规定》，5月1日起施行！");
//                    put("《云南省发展和改革委员会关于修订印发云南省公共资源交易平台远程异地评标工作规程（试行）的通知(2021)》", "云南｜修订完善远程异地评标工作流程");
//                    put("《关于印发《天津市工程建设项目招标投标活动投诉处理工作指引》的通知（2022）》", "【每日招言】天津市发文，对行政监督部门处理投诉的做法作出规定");
//                    put("《北京市财政局关于印发《北京市政府采购文件示范文本（试行）（2022年版）》的通知（2022）》", "北京发布政府采购文件示范文本，值得参考！（附下载链接）");
//                    put("《河北省房屋建筑和市政基础设施项目工程总承包管理办法（2021）》", "知招带你看丨招采一周要闻（01.03-01.09）");
//                    put("《财政违法行为处罚处分条例(2011修订)》", "山东省财政厅关于印发《山东省政府购买服务管理实施办法》的通知");
//                    put("《政府购买服务管理办法（2020）》", "山东省财政厅关于印发《山东省政府购买服务管理实施办法》的通知");
//                    put("《四川省人民政府办公厅关于印发四川省评标专家和综合评标专家库管理办法的通知（2021）》", "四川｜评标专家一年内拒绝参加评标五次并超过抽中次数三分之一的，自动解聘");
//                    put("《中共中央国务院关于加快建设全国统一大市场的意见（2022）》", "国务院：不得要求企业必须在某地登记注册或者设立分公司！资质认定不得对外地企业要求更高");
//                    put("《汝州市财政局关于印发《政府采购履约验收操作指引》的通知（2022）》", "验收怎么“验”？河南汝州印发《操作指引》");
//                    put("《国务院关于开展营商环境创新试点工作的意见（2021）》", "国办发文！部分二级工程资质下放到市、区（县）审批！清除招投标和政府采购隐性门槛和壁垒");
//                    put("《承装（修、试）电力设施许可证注销管理办法（2021）》", "国家能源局印发《承装（修、试）电力设施许可证注销管理办法》");
//                    put("《省住房城乡建设厅关于印发《2022年全省建筑业工作要点》的通知（2022）》", "住建厅：明确“评定分离”适用范围、调整施工许可证办理限额、资质改革平稳过渡...");
//                    put("《关于开展政府采购代理机构和评审专家履职评价工作（试行）的通知（征求意见稿）2021》", "财政部公布采购代理机构和评审专家履职评价指标（试行），公开征求意见");
//                    put("《黑龙江省房屋建筑和市政基础设施工程直接发包管理暂行办法（征求意见稿）2021》", "征求意见｜黑龙江有严重失信等行为的建筑业企业，不得作为直接发包工程的承包人");
//                    put("《河北雄安新区工程建设项目招标投标领域严重失信主体名单管理暂行办法（2022）》", "知招带你看丨招采要闻（05.28 - 06.17）");
//                    put("《政府采购家具项目采购需求管理指南（2021）》", "【地方】全国首个政府采购家具项目需求管理团体标准下月起实施");
//                    put("《财政行政处罚听证实施办法（2022）》", "财政部：禁止供应商参加采购、采购代理机构代理业务应告知其有要求听证的权利");
//                    put("《甘肃省建设工程计价规则（2020）》", "知招带你看丨招采一周要闻（01.03-01.09）");
//                    put("《住房和城乡建设部办公厅关于全面实行一级建造师电子注册证书的通知（2021）》", "");
//                    put("《关于规范政府采购保证金管理的通知（征求意见稿）2021》", "征求意见｜福建拟综合考虑相关因素，合理确定保证金收取比例和退还条件");
//                    put("《四川省建筑管理条例（2021）》", "即日起，政府项目不得要求施工单位垫资！四川修改《四川省建筑管理条例》");
//                    put("《湖南省房屋建筑和市政基础设施工程投标担保和经评审最低投标价中标履约担保管理办法（2022）》", "湖南印发工程投标担保管理办法！");
//                    put("《省住房城乡建设厅关于印发建设工程新冠疫情防控费用计取指导标准的通知（2022）》", "权威发布｜江苏住建厅｜新冠疫情常态化防控费作为总价措施项目费由发包人承担");
//                    put("《关于印发《工程建设领域防止拖欠工程款和农民工工资若干措施》的通知（2022）》", "安徽省住建厅等7部门印发《工程建设领域防止拖欠工程款和农民工工资若干措施》");
//                    put("《关于规范招投标领域工程建设保证金收取有关工作的通知（2022）》", "住建厅：任何单位和个人一律不得新设保证金项目！严查不付预付款行为");
//                    put("《政府采购框架协议采购方式管理暂行办法（2022）》", "新的采购方式来了！财政部出台《政府采购框架协议采购方式管理暂行办法》");
//                    put("《省住房城乡建设厅关于修改施工项目经理部关键岗位人员网上备案标准及有关管理要求的通知（2022）》", "住建厅修改“关于项目经理部关键岗位人员配备”文件，即日起施行！");
//                    put("《《建筑工程施工发包与承包计价管理办法》（征求意见稿）2021》", "刚刚，住建部拟修订《建筑工程施工发包与承包计价管理办法》！增加推行施工过程结算条款！");
//                    put("《关于切实加强全省房屋建筑和市政基础设施工程招标投标活动管理的通知（2022）》", "安徽：严禁违规上浮或下调招标控制价！");
//                    put("《上海市住房和城乡建设管理委员会关于印发《关于新冠肺炎疫情影响下建设工程履约及工程造价相关事项的指导意见》的通知（2022）》", "疫情停工期间滞留工地人员的工资或生活费计入工程造价，由发包人承担！上海明确！");
//                    put("《关于加强房屋建筑和市政基础设施工程招标投标活动管理的通知（2022）》", "工程招标项目实行诚信承诺函制！招标人未提交函的予以重点监管！该省发文");
//                    put("《关于公开征求《政府采购物业管理服务项目采购需求标准（征求意见稿）》意见的函（2021）》", "浙江｜拒绝“优”“良”“中”“一般”，物业管理服务项目采购需求标准公开征求意见");
//                    put("《江苏省高级人民法院、江苏省工程造价管理协会关于印发《关于建立建设工程价款纠纷联动解纷机制的意见》的通知（2022）》", "");
//                    put("《省财政厅关于印发《湖北省政府采购评审专家管理实施办法》的通知（2022）》", "湖北：评审专家聘期内不参加培训将暂停其评审活动");
//                    put("《湖南省政府采购工程管理办法（2021）》", "动态｜湖南发布《政府采购工程管理办法》");
//                    put("《国务院办公厅转发国家发展改革委关于在重点工程项目中大力实施以工代赈促进当地群众就业增收工作方案的通知（2022）》", "发改委｜在招标投标过程中明确以工代赈用工及劳务报酬发放要求");
//                    put("《关于印发黑龙江省房屋建筑和市政基础设施项目工程总承包招标投标管理办法（试行）的通知（2021）》", "监理工程师可担任工程总承包项目经理！招标人应承担至少7项风险，不得要求承包单位承担所有风险！该省发文");
//                    put("《财政部关于《中华人民共和国政府采购法实施条例》第十九条第一款 “较大数额罚款”具体适用问题的意见（2022）》", "【重磅】财政部正式发文明确“较大数额罚款”标准");
//                }
//            };
//
//            Optional<LawEntity> byId = lawEntityRepository.findById(Long.valueOf(id));
//            LawEntity lawEntity = byId.get();
//
//            HashMap<String, Object> map4 = new HashMap<>();  // 获取法律
//            map4.put("id", lawEntity.getId().toString());
//            map4.put("label", lawEntity.getName());
//            HashMap<String, Object> mapData4 = new HashMap<>();
//            mapData4.put("type", "4");
//            mapData4.put("level", level);
//            mapData4.put("publishDate", lawEntity.getPublishDate());
//            mapData4.put("publishUnit", lawEntity.getPublishUnit());
//            mapData4.put("useDate", lawEntity.getUseDate());
//            mapData4.put("isValid", lawEntity.getIsValid());
//            map4.put("data", mapData4);
//            nodes.add(map4);
//
//            String contentName = map.get(lawEntity.getName());  // 关联内容
//            if (contentName != null) {
//                HashMap<String, Object> map5 = new HashMap<>();  // 法律
//                String uuid = UUID.randomUUID().toString().replaceAll("-", "");
//                map5.put("id", uuid);
//                map5.put("label", contentName);
//                HashMap<String, Object> mapData5 = new HashMap<>();
//                mapData5.put("type", "5");
//                mapData5.put("level", level);
//                map5.put("data", mapData5);
//                nodes.add(map5);
//                // 储存线信息
//                HashMap<String, Object> line = new HashMap<>();
//                line.put("target", id.toString());
//                line.put("source", uuid);
//                line.put("label", "属于");
//                edges.add(line);
//            }
//
//        }
//
//        HashMap<String, Object> ret = new HashMap<>();
//        ret.put("nodes", nodes);
//        ret.put("edges", edges);
//        return TResponseVo.success(ret);
    }

    /**
     * 招采库-法律主图
     *
     * @param
     */
    @Override
    public TResponseVo getZhaocaiGlobalViewLabelService() throws Exception {
        ArrayList<Map<String, String>> obj = new ArrayList<Map<String, String>>() {
            {
                add(new HashMap<String, String>() {{
                    put("label", "有效");
                    put("value", "有效");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "失效");
                    put("value", "失效");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国招标投标协会");
                    put("value", "中国招标投标协会1003");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国物流与采购联合会");
                    put("value", "中国物流与采购联合会1004");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国国家标准化管理委员会");
                    put("value", "中国国家标准化管理委员会1005");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国建筑学会");
                    put("value", "中国建筑学会1006");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国招标协会");
                    put("value", "中国招标协会1007");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国建设监理协会");
                    put("value", "中国建设监理协会1008");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国建设工程造价管理协会");
                    put("value", "中国建设工程造价管理协会1009");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司");
                    put("value", "国家电网有限公司1010");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国网江苏省电力有限公司");
                    put("value", "国网江苏省电力有限公司1011");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国网江苏省电力有限公司物资部");
                    put("value", "国网江苏省电力有限公司物资部1012");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏苏电产业管理有限公司合规审计部");
                    put("value", "江苏苏电产业管理有限公司合规审计部1013");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司物资部");
                    put("value", "国家电网有限公司物资部1014");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司产业部");
                    put("value", "国家电网有限公司产业部1015");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "物资部");
                    put("value", "物资部1016");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司基建部");
                    put("value", "国家电网有限公司基建部1017");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司法律部");
                    put("value", "国家电网有限公司法律部1018");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏苏电产业管理有限公司");
                    put("value", "江苏苏电产业管理有限公司1019");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏鑫顺能源产业集团有限公司物资部");
                    put("value", "江苏鑫顺能源产业集团有限公司物资部1020");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国网江苏省电力有限公司安全监察部");
                    put("value", "国网江苏省电力有限公司安全监察部1021");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏鑫顺能源产业集团有限公司");
                    put("value", "江苏鑫顺能源产业集团有限公司1022");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司财务部");
                    put("value", "国家电网有限公司财务部1023");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司办公厅");
                    put("value", "国家电网有限公司办公厅1024");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司审计部");
                    put("value", "国家电网有限公司审计部1025");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国网江苏省电力有限公司发展策划部");
                    put("value", "国网江苏省电力有限公司发展策划部1026");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司建设部");
                    put("value", "国家电网有限公司建设部1027");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司发展部");
                    put("value", "国家电网有限公司发展部1028");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏苏电产业管理有限公司办公室");
                    put("value", "江苏苏电产业管理有限公司办公室1029");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国网江苏省电力有限公司建设部");
                    put("value", "国网江苏省电力有限公司建设部1030");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国网江苏省电力有限公司安监部");
                    put("value", "国网江苏省电力有限公司安监部1031");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国网江苏省电力有限公司产业部");
                    put("value", "国网江苏省电力有限公司产业部1032");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏苏电产业管理有限公司安全质量部");
                    put("value", "江苏苏电产业管理有限公司安全质量部1033");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电网有限公司办公室");
                    put("value", "国家电网有限公司办公室1034");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国网江苏省电力有限公司办公室");
                    put("value", "国网江苏省电力有限公司办公室1035");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "最高人民法院");
                    put("value", "最高人民法院1036");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家发展和改革委员会（含原国家发展计划委员会、原国家计划委员会）");
                    put("value", "国家发展和改革委员会1037");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "工业和信息化部");
                    put("value", "工业和信息化部1038");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "住房和城乡建设部（含原建设部）");
                    put("value", "住房和城乡建设部1039");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "交通运输部");
                    put("value", "交通运输部1040");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "水利部");
                    put("value", "水利部1041");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "商务部");
                    put("value", "商务部1042");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家铁路局");
                    put("value", "国家铁路局1043");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国民用航空局");
                    put("value", "中国民用航空局1044");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "财政部");
                    put("value", "财政部1045");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "住房和城乡建设部");
                    put("value", "住房和城乡建设部1046");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省政府采购中心");
                    put("value", "江苏省政府采购中心1047");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省发改委");
                    put("value", "广东省发改委1048");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "住建厅等七部门");
                    put("value", "住建厅等七部门1049");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "上海市财政局");
                    put("value", "上海市财政局1050");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "南京市城乡建设委员会");
                    put("value", "南京市城乡建设委员会1051");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省人力资源和社会保障厅");
                    put("value", "江苏省人力资源和社会保障厅1052");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省发展改革委");
                    put("value", "广东省发展改革委1053");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省工业和信息化厅");
                    put("value", "广东省工业和信息化厅1054");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省住房和城乡建设厅");
                    put("value", "广东省住房和城乡建设厅1055");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省交通运输厅");
                    put("value", "广东省交通运输厅1056");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省水利厅");
                    put("value", "广东省水利厅1057");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省农业农村厅");
                    put("value", "广东省农业农村厅1058");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省政务服务数据管理局");
                    put("value", "广东省政务服务数据管理局1059");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省能源局");
                    put("value", "广东省能源局1060");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "南京市发改委");
                    put("value", "南京市发改委1061");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省高级人民法院");
                    put("value", "江苏省高级人民法院1062");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省工程造价管理协会");
                    put("value", "江苏省工程造价管理协会1063");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省人力资源和社会保障厅");
                    put("value", "安徽省人力资源和社会保障厅1064");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省发展和改革委员会");
                    put("value", "安徽省发展和改革委员会1065");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省财政厅");
                    put("value", "安徽省财政厅1066");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省住房和城乡建设厅");
                    put("value", "安徽省住房和城乡建设厅1067");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省交通运输厅");
                    put("value", "安徽省交通运输厅1068");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省水利厅");
                    put("value", "安徽省水利厅1069");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省高级人民法院");
                    put("value", "安徽省高级人民法院1070");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北雄安新区管理委员会规划建设局");
                    put("value", "河北雄安新区管理委员会规划建设局1071");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北雄安新区管理委员会改革发展局");
                    put("value", "河北雄安新区管理委员会改革发展局1072");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北雄安新区管理委员会公共服务局");
                    put("value", "河北雄安新区管理委员会公共服务局1073");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖北省财政厅");
                    put("value", "湖北省财政厅1074");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广州市发改委");
                    put("value", "广州市发改委1075");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "天津市人民政府政务服务办公室");
                    put("value", "天津市人民政府政务服务办公室1076");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青海省政务服务监督管理局");
                    put("value", "青海省政务服务监督管理局1077");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青海省发改委");
                    put("value", "青海省发改委1078");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青海省政府国有资产监督管理委员会");
                    put("value", "青海省政府国有资产监督管理委员会1079");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青海省财政厅");
                    put("value", "青海省财政厅1080");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青海省住建厅");
                    put("value", "青海省住建厅1081");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青海省交通运输厅");
                    put("value", "青海省交通运输厅1082");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青海省水利厅");
                    put("value", "青海省水利厅1083");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省交通运输厅");
                    put("value", "四川省交通运输厅1084");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "云南省发展和改革委员会");
                    put("value", "云南省发展和改革委员会1085");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "西藏自治区财政厅");
                    put("value", "西藏自治区财政厅1086");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "天津市人力资源和社会保障局");
                    put("value", "天津市人力资源和社会保障局1087");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "重庆市住房和城乡建设委员会");
                    put("value", "重庆市住房和城乡建设委员会1088");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "上海市人力资源和社会保障局");
                    put("value", "上海市人力资源和社会保障局1089");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "汝州市财政局");
                    put("value", "汝州市财政局1090");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "福建省财政厅");
                    put("value", "福建省财政厅1091");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省物价局");
                    put("value", "江苏省物价局1092");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省住房和城乡建设厅（含原江苏省建设厅）");
                    put("value", "江苏省住房和城乡建设厅1093");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省住房和城乡建设厅");
                    put("value", "江苏省住房和城乡建设厅1094");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省发展和改革委员会");
                    put("value", "江苏省发展和改革委员会1095");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省财政厅");
                    put("value", "江苏省财政厅1096");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省交通运输厅");
                    put("value", "江苏省交通运输厅1097");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省商务厅");
                    put("value", "江苏省商务厅1098");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省国资委");
                    put("value", "江苏省国资委1099");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省市场监管局");
                    put("value", "江苏省市场监管局1100");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "南京海关");
                    put("value", "南京海关1101");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖南省发展和改革委员会");
                    put("value", "湖南省发展和改革委员会1102");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖南省住房和城乡建设厅");
                    put("value", "湖南省住房和城乡建设厅1103");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖南省交通运输厅");
                    put("value", "湖南省交通运输厅1104");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖南省水利厅");
                    put("value", "湖南省水利厅1105");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宁波市公共资源交易办公室");
                    put("value", "宁波市公共资源交易办公室1106");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宁波市发展和改革委员会");
                    put("value", "宁波市发展和改革委员会1107");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宁波市住房和城乡建设局");
                    put("value", "宁波市住房和城乡建设局1108");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宁波市交通运输局");
                    put("value", "宁波市交通运输局1109");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宁波市水利局");
                    put("value", "宁波市水利局1110");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省发展和改革委员会");
                    put("value", "广东省发展和改革委员会1111");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省经济和信息化委");
                    put("value", "广东省经济和信息化委1112");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省商务厅");
                    put("value", "广东省商务厅1113");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖南省公共资源交易管理委员会办公室");
                    put("value", "湖南省公共资源交易管理委员会办公室1114");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "档案局");
                    put("value", "档案局1115");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省委办公厅");
                    put("value", "江苏省委办公厅1116");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省人民政府办公厅");
                    put("value", "江苏省人民政府办公厅1117");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "浙江省人民政府办公厅");
                    put("value", "浙江省人民政府办公厅1118");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "扬州市住房和城乡建设局");
                    put("value", "扬州市住房和城乡建设局1119");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "浙江省发展和改革委员会");
                    put("value", "浙江省发展和改革委员会1120");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江西省发展和改革委员会");
                    put("value", "江西省发展和改革委员会1121");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山西省发展和改革委员会");
                    put("value", "山西省发展和改革委员会1122");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省人民政府");
                    put("value", "江苏省人民政府1123");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省人民检察院");
                    put("value", "江苏省人民检察院1124");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省发展改革委员会");
                    put("value", "江苏省发展改革委员会1125");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省经济和信息化委员会");
                    put("value", "江苏省经济和信息化委员会1126");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省科学技术厅");
                    put("value", "江苏省科学技术厅1127");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省住房与城乡建设厅");
                    put("value", "江苏省住房与城乡建设厅1128");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省水利厅");
                    put("value", "江苏省水利厅1129");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省政务服务管理办公室");
                    put("value", "江苏省政务服务管理办公室1130");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "建设领导小组办公室");
                    put("value", "建设领导小组办公室1131");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省监察厅");
                    put("value", "江苏省监察厅1132");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省建筑工程管理局");
                    put("value", "江苏省建筑工程管理局1133");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "济南市住房和城乡建设局");
                    put("value", "济南市住房和城乡建设局1134");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "济南市发展和改革委员会");
                    put("value", "济南市发展和改革委员会1135");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "济南市行政审批服务局");
                    put("value", "济南市行政审批服务局1136");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省公共资源交易中心");
                    put("value", "山东省公共资源交易中心1137");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "黑龙江省住房和城乡建设厅");
                    put("value", "黑龙江省住房和城乡建设厅1138");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省人民政府办公厅");
                    put("value", "广东省人民政府办公厅1139");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省财政厅");
                    put("value", "四川省财政厅1140");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省政务服务管理办公室");
                    put("value", "河北省政务服务管理办公室1141");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广州市建设工程招标管理办公室");
                    put("value", "广州市建设工程招标管理办公室1142");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "杭州市建设工程招标造价服务中心");
                    put("value", "杭州市建设工程招标造价服务中心1143");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中共云南省委办公厅");
                    put("value", "中共云南省委办公厅1144");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "云南省人民政府办公厅");
                    put("value", "云南省人民政府办公厅1145");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宁夏回族自治区人民政府办公厅");
                    put("value", "宁夏回族自治区人民政府办公厅1146");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "新疆维吾尔自治区交通运输厅办公室");
                    put("value", "新疆维吾尔自治区交通运输厅办公室1147");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖北省人民政府");
                    put("value", "湖北省人民政府1148");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "福建省数字福建建设领导小组办公室");
                    put("value", "福建省数字福建建设领导小组办公室1149");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省财政厅");
                    put("value", "山东省财政厅1150");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "黑龙江省财政厅");
                    put("value", "黑龙江省财政厅1151");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省财政厅");
                    put("value", "河北省财政厅1152");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "福建省公共资源交易中心");
                    put("value", "福建省公共资源交易中心1153");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市财政局");
                    put("value", "北京市财政局1154");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市发展和改革委员会");
                    put("value", "北京市发展和改革委员会1155");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广州市财政局");
                    put("value", "广州市财政局1156");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省发展和改革委员会");
                    put("value", "四川省发展和改革委员会1157");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省政府政务服务和公共资源交易服务中心");
                    put("value", "四川省政府政务服务和公共资源交易服务中心1158");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "浙江省政府采购联合会");
                    put("value", "浙江省政府采购联合会1159");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "浙江省家具行业协会");
                    put("value", "浙江省家具行业协会1160");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河南省财政厅");
                    put("value", "河南省财政厅1161");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖南省财政厅");
                    put("value", "湖南省财政厅1162");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宁夏回族自治区财政厅");
                    put("value", "宁夏回族自治区财政厅1163");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "内蒙古自治区公共资源交易中心");
                    put("value", "内蒙古自治区公共资源交易中心1164");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "吉林省人民政府办公厅");
                    put("value", "吉林省人民政府办公厅1165");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "浙江省财政厅");
                    put("value", "浙江省财政厅1166");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "辽宁省水利厅");
                    put("value", "辽宁省水利厅1167");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市交通委员会");
                    put("value", "北京市交通委员会1168");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "福建省发展和改革委员会");
                    put("value", "福建省发展和改革委员会1169");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "厦门市财政局");
                    put("value", "厦门市财政局1170");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "海南省财政厅");
                    put("value", "海南省财政厅1171");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "天津市公共资源交易中心");
                    put("value", "天津市公共资源交易中心1172");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "浙江省人民政府");
                    put("value", "浙江省人民政府1173");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省公共资源交易中心");
                    put("value", "河北省公共资源交易中心1174");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广西壮族自治区财政厅");
                    put("value", "广西壮族自治区财政厅1175");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "阿坝州发展和改革委员会");
                    put("value", "阿坝州发展和改革委员会1176");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省财政厅");
                    put("value", "广东省财政厅1177");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "陕西省财政厅");
                    put("value", "陕西省财政厅1178");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "贵州省发展和改革委员会");
                    put("value", "贵州省发展和改革委员会1179");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省人民政府办公厅");
                    put("value", "四川省人民政府办公厅1180");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "海南省交通运输厅");
                    put("value", "海南省交通运输厅1181");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广西自治区水利厅");
                    put("value", "广西自治区水利厅1182");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "长沙市发展和改革委员会");
                    put("value", "长沙市发展和改革委员会1183");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "长沙市人民政府办公厅");
                    put("value", "长沙市人民政府办公厅1184");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河南省发展和改革委员会");
                    put("value", "河南省发展和改革委员会1185");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省水利厅");
                    put("value", "山东省水利厅1186");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江西省财政厅");
                    put("value", "江西省财政厅1187");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省安全生产委员会");
                    put("value", "江苏省安全生产委员会1188");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省政府");
                    put("value", "江苏省政府1189");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省卫生厅");
                    put("value", "江苏省卫生厅1190");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省民政厅");
                    put("value", "江苏省民政厅1191");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "无锡市人民政府");
                    put("value", "无锡市人民政府1192");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省测绘局");
                    put("value", "江苏省测绘局1193");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省国土资源厅");
                    put("value", "江苏省国土资源厅1194");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "南京市发展和改革委员会");
                    put("value", "南京市发展和改革委员会1195");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "南京市人民政府");
                    put("value", "南京市人民政府1196");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "吉林省财政厅");
                    put("value", "吉林省财政厅1197");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "辽宁省财政厅");
                    put("value", "辽宁省财政厅1198");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "辽宁省民政厅");
                    put("value", "辽宁省民政厅1199");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "辽宁省工商行政管理局");
                    put("value", "辽宁省工商行政管理局1200");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "内蒙古自治区发展和改革委员会");
                    put("value", "内蒙古自治区发展和改革委员会1201");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "内蒙古自治区财政厅");
                    put("value", "内蒙古自治区财政厅1202");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "内蒙古自治区住房和城乡建设厅");
                    put("value", "内蒙古自治区住房和城乡建设厅1203");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "内蒙古自治区交通运输厅");
                    put("value", "内蒙古自治区交通运输厅1204");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "内蒙古自治区水利厅");
                    put("value", "内蒙古自治区水利厅1205");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国人民银行呼和浩特中心支行");
                    put("value", "中国人民银行呼和浩特中心支行1206");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "内蒙古自治区人民政府办公厅");
                    put("value", "内蒙古自治区人民政府办公厅1207");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北雄安新区管理委员会");
                    put("value", "河北雄安新区管理委员会1208");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省发展和改革委员会");
                    put("value", "河北省发展和改革委员会1209");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省司法厅");
                    put("value", "河北省司法厅1210");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省市场监督管理局");
                    put("value", "河北省市场监督管理局1211");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省住房和城乡建设厅");
                    put("value", "河北省住房和城乡建设厅1212");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省交通运输厅");
                    put("value", "河北省交通运输厅1213");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省水利厅");
                    put("value", "河北省水利厅1214");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省商务厅");
                    put("value", "河北省商务厅1215");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市规划和自然资源委员会");
                    put("value", "北京市规划和自然资源委员会1216");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市住房和城乡建设委员会");
                    put("value", "北京市住房和城乡建设委员会1217");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市城市管理委员会");
                    put("value", "北京市城市管理委员会1218");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市经济和信息化局");
                    put("value", "北京市经济和信息化局1219");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市水务局");
                    put("value", "北京市水务局1220");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市园林绿化局");
                    put("value", "北京市园林绿化局1221");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宜昌市公共资源交易中心");
                    put("value", "宜昌市公共资源交易中心1222");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宜昌市财政局");
                    put("value", "宜昌市财政局1223");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宜昌市公共资源交易监督管理局");
                    put("value", "宜昌市公共资源交易监督管理局1224");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "苏州市财政局");
                    put("value", "苏州市财政局1225");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河南省财政厅政府采购监督管理处");
                    put("value", "河南省财政厅政府采购监督管理处1226");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "浙江省人力资源和社会保障厅");
                    put("value", "浙江省人力资源和社会保障厅1227");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "深圳市监理工程师协会");
                    put("value", "深圳市监理工程师协会1228");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省住房城乡建设厅");
                    put("value", "江苏省住房城乡建设厅1229");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "住房和城乡建设部办公厅");
                    put("value", "住房和城乡建设部办公厅1230");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "上海市住房和城乡建设管理委员会");
                    put("value", "上海市住房和城乡建设管理委员会1231");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省建设工程招标投标办公室");
                    put("value", "江苏省建设工程招标投标办公室1232");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省住建和城乡建设厅");
                    put("value", "河北省住建和城乡建设厅1233");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "天津市住房和城乡建设委员会");
                    put("value", "天津市住房和城乡建设委员会1234");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "南京市住房和城乡建设委员会");
                    put("value", "南京市住房和城乡建设委员会1235");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省住房和城乡建设厅");
                    put("value", "山东省住房和城乡建设厅1236");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河南省住房和城乡建设厅");
                    put("value", "河南省住房和城乡建设厅1237");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "杭州市住房和城乡建设委员会");
                    put("value", "杭州市住房和城乡建设委员会1238");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "苏州市住房和城乡建设局");
                    put("value", "苏州市住房和城乡建设局1239");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江西省住房和城乡建设厅");
                    put("value", "江西省住房和城乡建设厅1240");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江西省人力资源社会保障厅");
                    put("value", "江西省人力资源社会保障厅1241");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江西省发展改革委");
                    put("value", "江西省发展改革委1242");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省建设工程招投标办公室");
                    put("value", "江苏省建设工程招投标办公室1243");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省建设工程招投标协会");
                    put("value", "江苏省建设工程招投标协会1244");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省住房和城乡建设厅");
                    put("value", "四川省住房和城乡建设厅1245");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "苏州市建设局（已撤销）");
                    put("value", "苏州市建设局1246");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "福建省住房和城乡建设厅");
                    put("value", "福建省住房和城乡建设厅1247");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "上海市交通委员会");
                    put("value", "上海市交通委员会1248");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "上海市发展和改革委员会");
                    put("value", "上海市发展和改革委员会1249");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省建设工程招标投标协会");
                    put("value", "江苏省建设工程招标投标协会1250");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "浙江省住房和城乡建设厅");
                    put("value", "浙江省住房和城乡建设厅1251");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖北省住房和城乡建设厅");
                    put("value", "湖北省住房和城乡建设厅1252");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广西省住房和城乡建设厅");
                    put("value", "广西省住房和城乡建设厅1253");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省发展和改革委员会");
                    put("value", "山东省发展和改革委员会1254");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "甘肃省住房和城乡建设厅");
                    put("value", "甘肃省住房和城乡建设厅1255");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "陕西省住房和城乡建设厅");
                    put("value", "陕西省住房和城乡建设厅1256");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "云南省住房和城乡建设厅");
                    put("value", "云南省住房和城乡建设厅1257");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "吉林省住房和城乡建设厅");
                    put("value", "吉林省住房和城乡建设厅1258");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省教育厅");
                    put("value", "安徽省教育厅1259");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省科学技术厅");
                    put("value", "安徽省科学技术厅1260");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省经济和信息化厅");
                    put("value", "安徽省经济和信息化厅1261");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省交通运输厅等");
                    put("value", "安徽省交通运输厅等1262");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "合肥市住房和城乡建设局");
                    put("value", "合肥市住房和城乡建设局1263");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青岛市住房和城乡建设局");
                    put("value", "青岛市住房和城乡建设局1264");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省经济和信息化厅");
                    put("value", "四川省经济和信息化厅1265");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省科学技术厅");
                    put("value", "四川省科学技术厅1266");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省人力资源和社会保障厅");
                    put("value", "四川省人力资源和社会保障厅1267");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国人民银行成都分行");
                    put("value", "中国人民银行成都分行1268");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国人民银行南京分行");
                    put("value", "中国人民银行南京分行1269");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国银行保险监督管理委员会江苏监管局");
                    put("value", "中国银行保险监督管理委员会江苏监管局1270");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "海南省住房和城乡建设厅");
                    put("value", "海南省住房和城乡建设厅1271");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "宜昌市住房和城乡建设局");
                    put("value", "宜昌市住房和城乡建设局1272");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国银行保险监督管理委员会");
                    put("value", "中国银行保险监督管理委员会1273");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东监理局");
                    put("value", "山东监理局1274");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青岛监管局");
                    put("value", "青岛监管局1275");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国人民银行济南分行");
                    put("value", "中国人民银行济南分行1276");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省科学技术厅");
                    put("value", "山东省科学技术厅1277");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省人力资源和社会保障厅");
                    put("value", "山东省人力资源和社会保障厅1278");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省生态环境厅");
                    put("value", "山东省生态环境厅1279");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省交通运输厅");
                    put("value", "山东省交通运输厅1280");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省商务厅");
                    put("value", "山东省商务厅1281");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家税务总局山东省税务局");
                    put("value", "国家税务总局山东省税务局1282");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国银行保险监督管理委员会山东监管局");
                    put("value", "中国银行保险监督管理委员会山东监管局1283");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河南住房和城乡建设厅");
                    put("value", "河南住房和城乡建设厅1284");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青岛市财政局");
                    put("value", "青岛市财政局1285");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国银行保险监督管理委员会青岛监管局");
                    put("value", "中国银行保险监督管理委员会青岛监管局1286");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青岛市地方金融监督管理局");
                    put("value", "青岛市地方金融监督管理局1287");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "吉林省发展和改革委员会");
                    put("value", "吉林省发展和改革委员会1288");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国人民银行长春中心支行");
                    put("value", "中国人民银行长春中心支行1289");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广西壮族自治区住房和城乡建设厅");
                    put("value", "广西壮族自治区住房和城乡建设厅1290");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山西省住房和城乡建设厅");
                    put("value", "山西省住房和城乡建设厅1291");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖北省发展和改革委员会");
                    put("value", "湖北省发展和改革委员会1292");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖北省公共资源交易监督管理局");
                    put("value", "湖北省公共资源交易监督管理局1293");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "陕西省发展和改革委员会");
                    put("value", "陕西省发展和改革委员会1294");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省水利厅");
                    put("value", "四川省水利厅1295");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省自然资源厅");
                    put("value", "山东省自然资源厅1296");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省应急管理厅");
                    put("value", "山东省应急管理厅1297");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省人民政府国有资产监督管理委员会");
                    put("value", "山东省人民政府国有资产监督管理委员会1298");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省市场监督管理局");
                    put("value", "山东省市场监督管理局1299");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国网山东省电力公司");
                    put("value", "国网山东省电力公司1300");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广州市住房和城乡建设局");
                    put("value", "广州市住房和城乡建设局1301");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广州公共资源交易中心");
                    put("value", "广州公共资源交易中心1302");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖南省公共资源交易中心");
                    put("value", "湖南省公共资源交易中心1303");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "珠海市住房和城乡规划建设局");
                    put("value", "珠海市住房和城乡规划建设局1304");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖南省公安厅");
                    put("value", "湖南省公安厅1305");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省建设工程造价管理总站");
                    put("value", "江苏省建设工程造价管理总站1306");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "南京市建设工程造价管理处");
                    put("value", "南京市建设工程造价管理处1307");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省建设厅关于建设工程监理招标报价问题的复函");
                    put("value", "江苏省建设厅关于建设工程监理招标报价问题的复函1308");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市人力资源和社会保障局");
                    put("value", "北京市人力资源和社会保障局1309");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山西省财政厅");
                    put("value", "山西省财政厅1310");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山西省人力资源和社会保障厅");
                    put("value", "山西省人力资源和社会保障厅1311");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国人民银行太原中心支行");
                    put("value", "中国人民银行太原中心支行1312");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国银保监会山西监管局");
                    put("value", "中国银保监会山西监管局1313");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "交通运输部（原铁道部）");
                    put("value", "交通运输部1314");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家能源局");
                    put("value", "国家能源局1315");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家发改委");
                    put("value", "国家发改委1316");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "人力资源社会保障部");
                    put("value", "人力资源社会保障部1317");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国务院办公厅");
                    put("value", "国务院办公厅1318");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "建设部（已撤销）");
                    put("value", "建设部1319");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "监察部");
                    put("value", "监察部1320");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中共中央办公厅");
                    put("value", "中共中央办公厅1321");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家税务总局");
                    put("value", "国家税务总局1322");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "海关总署");
                    put("value", "海关总署1323");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "税务总局");
                    put("value", "税务总局1324");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "应急管理部");
                    put("value", "应急管理部1325");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "教育部");
                    put("value", "教育部1326");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "科学技术部");
                    put("value", "科学技术部1327");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "监察部（已撤销）");
                    put("value", "监察部1328");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "住房和城乡建设部标准定额司");
                    put("value", "住房和城乡建设部标准定额司1329");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家经济贸易委员会（已变更）");
                    put("value", "国家经济贸易委员会1330");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国土资源部（已撤销）");
                    put("value", "国土资源部1331");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国务院");
                    put("value", "国务院1332");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "住房和城乡建设部建筑市场监管司");
                    put("value", "住房和城乡建设部建筑市场监管司1333");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家发展和改革委员会");
                    put("value", "国家发展和改革委员会1334");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国人民银行");
                    put("value", "中国人民银行1335");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家市场监督管理总局");
                    put("value", "国家市场监督管理总局1336");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国证券监督管理委员会");
                    put("value", "中国证券监督管理委员会1337");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国国家铁路集团有限公司");
                    put("value", "中国国家铁路集团有限公司1338");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家发展改革委");
                    put("value", "国家发展改革委1339");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "自然资源部");
                    put("value", "自然资源部1340");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "农业农村部");
                    put("value", "农业农村部1341");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "全国人大");
                    put("value", "全国人大1342");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "科技部");
                    put("value", "科技部1343");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "人民银行");
                    put("value", "人民银行1344");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家医疗保障局");
                    put("value", "国家医疗保障局1345");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "工业和信息化部中小企业局");
                    put("value", "工业和信息化部中小企业局1346");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "人力资源和社会保障部");
                    put("value", "人力资源和社会保障部1347");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "生态环境部");
                    put("value", "生态环境部1348");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "文化和旅游部");
                    put("value", "文化和旅游部1349");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家税务总局等");
                    put("value", "国家税务总局等1350");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "司法部");
                    put("value", "司法部1351");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "住房城乡建设部");
                    put("value", "住房城乡建设部1352");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家广播电视总局");
                    put("value", "国家广播电视总局1353");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "能源局");
                    put("value", "能源局1354");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "铁路局");
                    put("value", "铁路局1355");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "民航局");
                    put("value", "民航局1356");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家质量监督检验检疫总局（已撤销）");
                    put("value", "国家质量监督检验检疫总局1357");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家安全生产监督管理总局（原国家安全生产监督管理局）（已撤销）");
                    put("value", "国家安全生产监督管理总局1358");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "人事部（已撤销）");
                    put("value", "人事部1359");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "发改委");
                    put("value", "发改委1360");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "全国人大常委会法制工作委员会");
                    put("value", "全国人大常委会法制工作委员会1361");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "银保监会");
                    put("value", "银保监会1362");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "铁道部");
                    put("value", "铁道部1363");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中央国家机关政府采购中心");
                    put("value", "中央国家机关政府采购中心1364");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家知识产权局");
                    put("value", "国家知识产权局1365");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家电力监管委员会（已撤销）");
                    put("value", "国家电力监管委员会1366");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "民政部");
                    put("value", "民政部1367");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家工商行政管理总局（已撤销）");
                    put("value", "国家工商行政管理总局1368");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "最高人民检察院");
                    put("value", "最高人民检察院1369");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "住房和城乡建设部（含原建设部）");
                    put("value", "住房和城乡建设部（含原建设部）1370");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "铁道部（已撤销）");
                    put("value", "铁道部1371");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国务院法制办公室（含国务院法制局）（已撤销）");
                    put("value", "国务院法制办公室1372");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国人民共和国中央军事委员会");
                    put("value", "中国人民共和国中央军事委员会1373");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家档案局");
                    put("value", "国家档案局1374");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "全国人民代表大会");
                    put("value", "全国人民代表大会1375");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "全国人大常委会");
                    put("value", "全国人大常委会1376");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "山东省人民政府");
                    put("value", "山东省人民政府1377");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "温州市人民政府");
                    put("value", "温州市人民政府1378");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "青海省人民政府");
                    put("value", "青海省人民政府1379");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "上海市人民政府");
                    put("value", "上海市人民政府1380");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "吉林省人民政府");
                    put("value", "吉林省人民政府1381");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省人大及其常委会");
                    put("value", "江苏省人大及其常委会1382");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "江苏省人大（含常委会）");
                    put("value", "江苏省人大1383");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "湖北省人大（含常委会）");
                    put("value", "湖北省人大1384");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "河北省人大（含常委会）");
                    put("value", "河北省人大1385");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "内蒙古自治区人大（含常委会）");
                    put("value", "内蒙古自治区人大1386");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "浙江省人大（含常委会）");
                    put("value", "浙江省人大1387");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "福建省人大（含常委会）");
                    put("value", "福建省人大1388");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "广东省人大（含常委会）");
                    put("value", "广东省人大1389");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "贵州省人大（含常委会）");
                    put("value", "贵州省人大1390");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "甘肃省人大（含常委会）");
                    put("value", "甘肃省人大1391");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "安徽省人大（含常委会）");
                    put("value", "安徽省人大1392");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "四川省人大（含常委会）");
                    put("value", "四川省人大1393");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "北京市人大（含常委会）");
                    put("value", "北京市人大1394");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "无锡市人大（含常委会）");
                    put("value", "无锡市人大1395");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "南京市人大（含常委会）");
                    put("value", "南京市人大1396");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "建设部");
                    put("value", "建设部1397");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "交通部");
                    put("value", "交通部1398");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "信息产业部");
                    put("value", "信息产业部1399");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中国民用航空总局");
                    put("value", "中国民用航空总局1400");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家广播电影电视总局（已撤销）");
                    put("value", "国家广播电影电视总局1401");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "城乡建设环境部（已变更）");
                    put("value", "城乡建设环境部1402");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "公安部");
                    put("value", "公安部1403");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "文化部（已撤销）");
                    put("value", "文化部1404");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "银监会");
                    put("value", "银监会1405");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "保监会");
                    put("value", "保监会1406");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国务院国有资产监督管理委员会");
                    put("value", "国务院国有资产监督管理委员会1407");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "环境保护部（已撤销）");
                    put("value", "环境保护部1408");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "人力资源和社会保障部门");
                    put("value", "人力资源和社会保障部门1409");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家工商行政管理总局");
                    put("value", "国家工商行政管理总局1410");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国土资源部");
                    put("value", "国土资源部1411");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家互联网信息办公室");
                    put("value", "国家互联网信息办公室1412");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家保密局");
                    put("value", "国家保密局1413");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "中华人民共和国国土资源部");
                    put("value", "中华人民共和国国土资源部1414");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家经贸委");
                    put("value", "国家经贸委1415");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国家经济贸易委员会");
                    put("value", "国家经济贸易委员会1416");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "电力工业部");
                    put("value", "电力工业部1417");
                }});
                add(new HashMap<String, String>() {{
                    put("label", "国内贸易部（已变更）");
                    put("value", "国内贸易部1418");
                }});
            }
        };
        return TResponseVo.success(obj);
    }


    /**
     * 评标工具-获取评审标准类型
     *
     * @param
     */
    @Override
    public TResponseVo getElementTypeOptionsService(String fileUuid, String assessId) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
        }
        List<OptionsPojo> optionsDao = workingTableMapper.getOptionsDao("elementType", "all");
        List<OptionsPojo> optionsDaoTmp = new ArrayList<>();
        optionsDaoTmp.addAll(optionsDao);
        if (fileUuid != null && assessId != null) {
            List<HfContentAssessElementPojo> assessElementListDao = fileOperationMapper.getAssessElementListDao(assessId, fileUuid, null, null);
            for (OptionsPojo optionsPojo : optionsDaoTmp) {
                String value = optionsPojo.getValue();
                boolean flag = true;
                for (HfContentAssessElementPojo hfContentAssessElementPojo : assessElementListDao) {
                    String elementType = hfContentAssessElementPojo.getElementType();
                    if (elementType.equals(value)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    optionsDao.remove(optionsPojo);
                }
            }

        }
        return TResponseVo.success(optionsDao);
    }

    /**
     * 帮助文档-获取目录
     */
    @Override
    public TResponseVo getHelpDocCatalogueService() throws Exception {
        List<HfHelpDocCatalogue> helpDocCatalogueDao = workingTableMapper.getHelpDocCatalogueDao();
        List<HfHelpDocCatalogue> helpDocCatalogueDao1 = new ArrayList<>(); //level=1
        List<HfHelpDocCatalogue> helpDocCatalogueDao2 = new ArrayList<>(); //level=2
        // 区分目录等级
        for (HfHelpDocCatalogue hfHelpDocCatalogue : helpDocCatalogueDao) {
            if (hfHelpDocCatalogue.getLevel().equals("1")) {
                helpDocCatalogueDao1.add(hfHelpDocCatalogue);
            } else {
                helpDocCatalogueDao2.add(hfHelpDocCatalogue);
            }
        }

        for (HfHelpDocCatalogue helpDocCatalogue1 : helpDocCatalogueDao1) {
            for (HfHelpDocCatalogue helpDocCatalogue2 : helpDocCatalogueDao2) {
                if (helpDocCatalogue1.getHelpDocId().equals(helpDocCatalogue2.getFatherId())) {
                    if (helpDocCatalogue1.getChildren() == null) {
                        helpDocCatalogue1.setChildren(new ArrayList<HfHelpDocCatalogue>());
                    }
                    helpDocCatalogue1.getChildren().add(helpDocCatalogue2);
                }
            }
        }

        return TResponseVo.success(helpDocCatalogueDao1);
    }

    /**
     * 帮助文档-获取内容
     *
     * @param helpDocId
     */
    @Override
    public TResponseVo getHelpDocContentService(String helpDocId) throws Exception {
        HfHelpDocCatalogue helpDocContentDao = workingTableMapper.getHelpDocContentDao(helpDocId);
        if (helpDocContentDao == null) {
            return TResponseVo.error("未查询到该帮助文档对应内容");
        } else {
            String content = helpDocContentDao.getHelpDocContent();
            Map<String, Object> map = JSON.parseObject(content, Map.class);
            map.put("helpDocName", helpDocContentDao.getHelpDocName());
            return TResponseVo.success(map);
        }

    }

    /**
     * 帮助文档-搜索
     *
     * @param searchContent
     */
    @Override
    public TResponseVo searchHelpDocService(PagePojo pagePojo, String searchContent) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();

        if (StringUtils.isBlank(searchContent)) {
            TResponseVo.error("搜索内容为空");
        }

        List<HfHelpDocCatalogue> hfHelpDocCatalogues = workingTableMapper.searchHelpDocDao(searchContent);
        // 将搜索关键字放入热词
        if (redisUtils.hasKey(envName + "helpDocHotSearchKey")) {
            Object helpDocHotSearchKey = redisUtils.get(envName + "helpDocHotSearchKey");
            List<Map<String, String>> list = JSON.parseObject(String.valueOf(helpDocHotSearchKey), List.class);
            boolean flag = true;
            for (Map<String, String> m : list) {
                if (m.get("content").equals(searchContent)) {
                    m.put("cnt", String.valueOf(Integer.valueOf(m.get("cnt")) + 1));
                    flag = false;
                    redisUtils.set(envName + "helpDocHotSearchKey", JSON.toJSONString(list), 3600 * 24);
                    break;
                }
            }
            if (flag) {
                if (StringUtils.isNotBlank(searchContent)) {
                    Map<String, String> map = new HashMap<>();
                    map.put("content", searchContent);
                    map.put("cnt", "1");
                    list.add(map);
                }
                redisUtils.set(envName + "helpDocHotSearchKey", JSON.toJSONString(list), 3600 * 24);
            }
        } else {
            if (StringUtils.isNotBlank(searchContent)) {
                ArrayList<Map<String, String>> list = new ArrayList<>();
                Map<String, String> map = new HashMap<>();
                map.put("content", searchContent);
                map.put("cnt", "1");
                list.add(map);
                redisUtils.set(envName + "helpDocHotSearchKey", JSON.toJSONString(list), 3600 * 24);
            }
        }

        //
        List<Map<String, Object>> retList = new ArrayList<>();
        for (HfHelpDocCatalogue hfHelpDocCatalogue : hfHelpDocCatalogues) {
            String helpDocContent = hfHelpDocCatalogue.getHelpDocContent();
            Map<String, Object> helpDocContentMap = JSON.parseObject(helpDocContent, Map.class);
            if (helpDocContentMap.get("text") == null) {
                continue;
            }
            String text = String.valueOf(helpDocContentMap.get("text"));

            String[] splits = text.split(searchContent);
            if (splits.length == 1) {
                continue;
            }
            String newContentText = "";
            for (int i = 0; i < splits.length; i++) {
                String id = UUID.randomUUID().toString().replaceAll("-", "");
                newContentText = newContentText + splits[i].toString();
                if (i != splits.length - 1) {
                    newContentText = newContentText + "<mark id=\"" + id + "\">" + searchContent + "</mark>";
                    Map<String, Object> retMap = new HashMap<>();
                    String splitPre = splits[i];
                    String splitNext = splits[i + 1];
                    retMap.put("searchResult", splitPre.substring(splitPre.length() < 10 ? 0 : splitPre.length() - 10, splitPre.length()) + "<mark>" + searchContent + "</mark>" + splitNext.substring(0, splitNext.length() < 10 ? splitNext.length() : 10));
                    HashMap<String, String> labelvalue = new HashMap<>();
                    labelvalue.put("label", hfHelpDocCatalogue.getFatherHelpDocName());
                    labelvalue.put("value", hfHelpDocCatalogue.getFatherId());
                    HashMap<String, String> labelvalue1 = new HashMap<>();
                    labelvalue1.put("label", hfHelpDocCatalogue.getHelpDocName());
                    labelvalue1.put("value", hfHelpDocCatalogue.getHelpDocId());
                    ArrayList<Map<String, String>> list = new ArrayList<>();
                    list.add(labelvalue);
                    list.add(labelvalue1);
                    retMap.put("outlineId", list);
                    retList.add(retMap);
                }
            }
        }

        if ("0".equalsIgnoreCase(isPaged)) {
            HashMap<String, Object> ret = new HashMap<>();
            ret.put("retList", retList);
            return TResponseVo.success(ret);
        }
        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(retList, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", retList.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 帮助文档-热搜词
     *
     * @param
     */
    @Override
    public TResponseVo getHotSearchKeyService() throws Exception {
        if (redisUtils.hasKey(envName + "helpDocHotSearchKey")) {
            Object helpDocHotSearchKey = redisUtils.get(envName + "helpDocHotSearchKey");
            List<Map<String, String>> list = JSON.parseObject(String.valueOf(helpDocHotSearchKey), List.class);
            List<Map<String, String>> mapList = new JsonKeyUtils().orderMapList7(list);
            ArrayList<String> strings = new ArrayList<>();
            int max = mapList.size() < 5 ? mapList.size() : 5;
            for (int i = 0; i < max; i++) {
                strings.add(mapList.get(i).get("content"));
            }
            return TResponseVo.success(strings);
        } else {
            return TResponseVo.success(new ArrayList<String>());
        }
    }

    /**
     * 我的空间-新建文档-最近创建
     *
     * @param userId
     */
    @Override
    public TResponseVo getRecentBuildService(String userId) throws Exception {
        List<LabelValuePojo> fileTypeIdList = workingTableMapper.getRecentBuildDao(userId);
        ArrayList<String> list = new ArrayList<>();
        ArrayList<LabelValuePojo> retList = new ArrayList<>();
        for (LabelValuePojo labelValuePojo : fileTypeIdList) {
            if (!list.contains(labelValuePojo.getValue())) {
                list.add(labelValuePojo.getValue());
                retList.add(labelValuePojo);
            }
        }
        return TResponseVo.success(retList);
    }

    /**
     * 批次管理-新辅助评标
     *
     * @param
     */
    @Override
    public TResponseVo newJudgeService(HfJudgePojo hfJudgePojo) throws Exception {
        String batchId = hfJudgePojo.getBatchId();
        String projectStageId = hfJudgePojo.getProjectStageId();
        Object fileUuidList = hfJudgePojo.getFileUuidList();
        String assessId = hfJudgePojo.getAssessId();
        String judgeName = hfJudgePojo.getJudgeName();
        if (StringUtils.isBlank(batchId) || StringUtils.isBlank(projectStageId) || fileUuidList == null || StringUtils.isBlank(assessId) || StringUtils.isBlank(judgeName)) {
            return TResponseVo.error("必填参数为空");
        }
        String judgeId = UUID.randomUUID().toString().replaceAll("-", "");
        hfJudgePojo.setJudgeId(judgeId);
        String fileUuidStr = new JsonKeyUtils().listToString((List) fileUuidList);
        hfJudgePojo.setFileUuidList(fileUuidStr);
        workingTableMapper.newJudgeDao(hfJudgePojo);

        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("info", "新增成功");
        retMap.put("judgeId", judgeId);
        return TResponseVo.success(retMap);
    }

    /**
     * 批次管理-新辅助评标-根据分包获取评审文件及评审模型
     *
     * @param projectStageId
     */
    @Override
    public TResponseVo getFileAndAssessService(String projectStageId) throws Exception {
        // 投标文件清单
        List<LabelValuePojo> fileByProjectStageIdDao = workingTableMapper.getFileLVByProjectStageIdDao(projectStageId);

        // 评审模型清单
        ProjectFilePojo projectFilePojos = workingTableMapper.getBidFileByProjectStageIdDao(projectStageId);  // 招标模板
        List<Map<String, String>> assessList = new ArrayList<>();
        if (projectFilePojos == null) {
            assessList = new ArrayList<>();
        } else {
            String fileUuid = projectFilePojos.getFileUuid();
            HfContentAssessPojo contentAssessDao = workingTableMapper.getContentAssessDao(fileUuid);
            if (contentAssessDao == null) {
                assessList = new ArrayList<>();
            } else {
                String assessModelList = (String) contentAssessDao.getAssessModelList();
                if (contentAssessDao.getAssessModelList() == null) {
                    // 无操作
                } else {
                    assessList.addAll(JSON.parseObject(assessModelList, List.class) == null ? new ArrayList<>() : JSON.parseObject(assessModelList, List.class));
                }
                String assessDetailedModelList = (String) contentAssessDao.getAssessDetailedModelList();
                if (contentAssessDao.getAssessDetailedModelList() == null) {
                    // 无操作
                } else {
                    assessList.addAll(JSON.parseObject(assessDetailedModelList, List.class) == null ? new ArrayList<>() : JSON.parseObject(assessDetailedModelList, List.class));
                }
            }

        }

        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("fileList", fileByProjectStageIdDao);
        retMap.put("assessList", assessList);
        return TResponseVo.success(retMap);
    }

    /**
     * 批次管理-删除辅助评标
     *
     * @param judgeId
     */
    @Override
    public TResponseVo delJudgeService(String judgeId) throws Exception {
        workingTableMapper.delJudgeDao(judgeId);
        return TResponseVo.success("删除完成");
    }

    /**
     * 批次管理-修改辅助评标
     *
     * @param hfJudgePojo
     */
    @Override
    public TResponseVo updateJudgeService(HfJudgePojo hfJudgePojo) throws Exception {
        String batchId = hfJudgePojo.getBatchId();
        String projectStageId = hfJudgePojo.getProjectStageId();
        Object fileUuidList = hfJudgePojo.getFileUuidList();
        String assessId = hfJudgePojo.getAssessId();
        if (StringUtils.isBlank(batchId) || StringUtils.isBlank(projectStageId) || fileUuidList == null || StringUtils.isBlank(assessId)) {
            return TResponseVo.error("必填参数为空");
        }
        String fileUuidStr = new JsonKeyUtils().listToString((List) fileUuidList);
        hfJudgePojo.setFileUuidList(fileUuidStr);
        workingTableMapper.updateJudgeDao(hfJudgePojo);
        return TResponseVo.success("修改完成");
    }

    /**
     * 批次管理-查询辅助评标信息
     *
     * @param judgeId
     */
    @Override
    public TResponseVo getJudgeInfoService(String judgeId) throws Exception {
        HfJudgePojo judgeInfoDao = workingTableMapper.getJudgeInfoDao(judgeId);
        String batchId = judgeInfoDao.getBatchId();

        // 设置评审模型中文名
        String projectStageId = judgeInfoDao.getProjectStageId();
        ProjectFilePojo projectFilePojos = workingTableMapper.getBidFileByProjectStageIdDao(projectStageId);  // 招标模板
        List<Map<String, String>> assessList = new ArrayList<>();
        if (projectFilePojos == null) {
            assessList = new ArrayList<>();
        } else {
            String fileUuid = projectFilePojos.getFileUuid();
            HfContentAssessPojo contentAssessDao = workingTableMapper.getContentAssessDao(fileUuid);
            String assessModelList = (String) contentAssessDao.getAssessModelList();
            assessList.addAll(JSON.parseObject(assessModelList, List.class) == null ? new ArrayList<>() : JSON.parseObject(assessModelList, List.class));
            String assessDetailedModelList = (String) contentAssessDao.getAssessDetailedModelList();
            assessList.addAll(JSON.parseObject(assessDetailedModelList, List.class) == null ? new ArrayList<>() : JSON.parseObject(assessDetailedModelList, List.class));
            if (assessList != null) {
                for (Map<String, String> map : assessList) {
                    if (map.get("value").equals(judgeInfoDao.getAssessId())) {
                        judgeInfoDao.setAssessName(map.get("label"));
                        break;
                    }
                }
            }
        }

        // 设置文件列表
        String fileUuidListStr = (String) judgeInfoDao.getFileUuidList();
        List<String> fileUuidList = new JsonKeyUtils().stringToList(fileUuidListStr);
        judgeInfoDao.setFileUuidList(fileUuidList);
        //补充投标文件信息
        List<BatchFilePojo> allFileInfo = workingTableMapper.getBatchFileListDao(null, batchId, "ALL");
        ArrayList<LabelValuePojo> labelValuePojos = new ArrayList<>();
        for (String fileUuid : fileUuidList) {
            for (BatchFilePojo batchFilePojo : allFileInfo) {
                if (fileUuid.equals(batchFilePojo.getFileUuid())) {
                    labelValuePojos.add(new LabelValuePojo(batchFilePojo.getFileName(), batchFilePojo.getFileUuid()));
                    break;
                }
            }
        }
        judgeInfoDao.setFileUuidLVList(labelValuePojos);


        // 设置分包中文名
        BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
        if (batchInfoDao != null) {
            String projectStageListStr = (String) batchInfoDao.getProjectStageList();
            if (batchInfoDao.getProjectStageList() != null) {
                List<Map<String, String>> projectStageList = JSON.parseObject(projectStageListStr, List.class);
                for (Map<String, String> map : projectStageList) {
                    if (map.get("projectStageId").equals(projectStageId)) {
                        judgeInfoDao.setProjectStageName(map.get("projectStageName"));
                        break;
                    }
                }
            }
        }

        return TResponseVo.success(judgeInfoDao);
    }

    /**
     * 批次管理-查询辅助评标清单
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getJudgeListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        String batchId = pagePojo.getBatchId();
        if (StringUtils.isBlank(batchId)) {
            return TResponseVo.error("必填参数为空");
        }
        List<HfJudgePojo> judgeListDao = workingTableMapper.getJudgeListDao(paramNameLike, batchId);

        // 补充字段
        for (HfJudgePojo judgeInfoDao : judgeListDao) {
            // 设置评审模型中文名
            String projectStageId = judgeInfoDao.getProjectStageId();
            ProjectFilePojo projectFilePojos = workingTableMapper.getBidFileByProjectStageIdDao(projectStageId);  // 招标模板
            List<Map<String, String>> assessList = new ArrayList<>();
            if (projectFilePojos == null) {
                assessList = new ArrayList<>();
            } else {
                String fileUuid = projectFilePojos.getFileUuid();
                HfContentAssessPojo contentAssessDao = workingTableMapper.getContentAssessDao(fileUuid);
                String assessModelList = (String) contentAssessDao.getAssessModelList();
                assessList.addAll(JSON.parseObject(assessModelList, List.class) == null ? new ArrayList<>() : JSON.parseObject(assessModelList, List.class));
                String assessDetailedModelList = (String) contentAssessDao.getAssessDetailedModelList();
                assessList.addAll(JSON.parseObject(assessDetailedModelList, List.class) == null ? new ArrayList<>() : JSON.parseObject(assessDetailedModelList, List.class));
                if (assessList != null) {
                    for (Map<String, String> map : assessList) {
                        if (map.get("value").equals(judgeInfoDao.getAssessId())) {
                            judgeInfoDao.setAssessName(map.get("label"));
                            break;
                        }
                    }
                }
            }

            // 设置文件列表
            String fileUuidListStr = (String) judgeInfoDao.getFileUuidList();
            List<String> fileUuidList = new JsonKeyUtils().stringToList(fileUuidListStr);
            judgeInfoDao.setFileUuidList(fileUuidList);
            //补充投标文件信息
            List<BatchFilePojo> allFileInfo = workingTableMapper.getBatchFileListDao(null, batchId, "ALL");
            ArrayList<LabelValuePojo> labelValuePojos = new ArrayList<>();
            for (String fileUuid : fileUuidList) {
                for (BatchFilePojo batchFilePojo : allFileInfo) {
                    if (fileUuid.equals(batchFilePojo.getFileUuid())) {
                        labelValuePojos.add(new LabelValuePojo(batchFilePojo.getFileName(), batchFilePojo.getFileUuid()));
                        break;
                    }
                }
            }
            judgeInfoDao.setFileUuidLVList(labelValuePojos);


            // 设置分包中文名
            BatchPojo batchInfoDao = workingTableMapper.getBatchInfoDao(batchId);
            if (batchInfoDao != null) {
                String projectStageListStr = (String) batchInfoDao.getProjectStageList();
                if (batchInfoDao.getProjectStageList() != null) {
                    List<Map<String, String>> projectStageList = JSON.parseObject(projectStageListStr, List.class);
                    for (Map<String, String> map : projectStageList) {
                        if (map.get("projectStageId").equals(projectStageId)) {
                            judgeInfoDao.setProjectStageName(map.get("projectStageName"));
                            break;
                        }
                    }
                }
            }
        }


        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(judgeListDao);
        }
        /**
         * 分页
         */
        List<Map<Object, Object>> mapList = new ListPageUtils().test0(judgeListDao, pageSize, pageNum);

        /**
         * 构建返回内容
         */
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("list", mapList);
        resultMap.put("total", judgeListDao.size());
        return TResponseVo.success(resultMap);
    }

    /**
     * 批次管理-查询辅助评标清单
     *
     * @param hfJudgePojo
     */
    @Override
    public TResponseVo getJudgeDetailService(HfJudgePojo hfJudgePojo) throws Exception {
        String judgeId = hfJudgePojo.getJudgeId();
        if (StringUtils.isBlank(judgeId)) {
            return TResponseVo.error("必填参数为空");
        }

        HfJudgePojo judgeInfoDao = workingTableMapper.getJudgeInfoDao(judgeId);
        String projectStageId = judgeInfoDao.getProjectStageId();
        List<ProjectFilePojo> fileByProjectStageIdDao = workingTableMapper.getFileByProjectStageIdDao(projectStageId);
        ProjectFilePojo projectFilePojos1 = new ProjectFilePojo();  // 招标模板
        ArrayList<ProjectFilePojo> projectFilePojos2 = new ArrayList<>();  // 投标文件
        for (ProjectFilePojo projectFilePojo : fileByProjectStageIdDao) {
            String fileClass = projectFilePojo.getFileClass();
            if ("2".equals(fileClass)) {
                projectFilePojos1 = projectFilePojo;
            } else if ("3".equals(fileClass)) {
                projectFilePojos2.add(projectFilePojo);
            }
        }
        // 按照评审辅助中的文件id过滤
        String fileUuidList1Str = (String) judgeInfoDao.getFileUuidList();
        List<String> fileUuidList1 = new JsonKeyUtils().stringToList(fileUuidList1Str);
        // 然后是手动选择的
        List<String> fileUuidList2 = (List) hfJudgePojo.getFileUuidList();

        ArrayList<ProjectFilePojo> projectFilePojos2Tmp = new ArrayList<>();
        projectFilePojos2Tmp.addAll(projectFilePojos2);
        for (ProjectFilePojo projectFilePojo : projectFilePojos2Tmp) {
            if (fileUuidList1 != null && !fileUuidList1.contains(projectFilePojo.getFileUuid())) {
                projectFilePojos2.remove(projectFilePojo);
            }
            if (fileUuidList2 != null && !fileUuidList2.contains(projectFilePojo.getFileUuid())) {
                projectFilePojos2.remove(projectFilePojo);
            }
        }

        // 根据招标模板获取评审模型
        String fileUuid = projectFilePojos1.getFileUuid();
        String assessId = judgeInfoDao.getAssessId();
        if (fileUuid == null) {
            return TResponseVo.error("未获取到招标文件信息");
        }
        // 获取element的数组
        List<HfContentAssessElementPojo> assessElementListDao = fileOperationMapper.getAssessElementListDao(assessId, fileUuid, null, null);

        // 获取要判断的paramIdList数组
        ArrayList<String> judgeParamsIdList = new ArrayList<>();
        for (HfContentAssessElementPojo hfContentAssessElementPojo : assessElementListDao) {
            String paramsIdListStr = (String) hfContentAssessElementPojo.getParamsIdList();
            List<String> paramsIdList = JSON.parseObject(paramsIdListStr, List.class);
            if (hfContentAssessElementPojo.getParamsIdList() != null) {
                for (String paramsId : paramsIdList) {
                    if (!judgeParamsIdList.contains(paramsId)) {
                        judgeParamsIdList.add(paramsId);
                    }
                }
            }
        }
        // 根据projectFilePojos2查出库中的填参信息 并绑定到对应文件上
        ArrayList<String> fileUuidList = new ArrayList<>();
        for (ProjectFilePojo projectFilePojo : projectFilePojos2) {
            String fileUuid1 = projectFilePojo.getFileUuid();
            if (!fileUuidList.contains(fileUuid1)) {
                fileUuidList.add(fileUuid1);
            }
        }
        String judgeParamsIdListStr = new JsonKeyUtils().listToString(judgeParamsIdList, "','");
        String fileUuidListStr = new JsonKeyUtils().listToString(fileUuidList, "','");
        List<DocParamsPojo> judgeParamsDao = workingTableMapper.getJudgeParamsDao(fileUuidListStr, judgeParamsIdListStr);
        for (ProjectFilePojo projectFilePojo : projectFilePojos2) {
            for (DocParamsPojo paramsPojo : judgeParamsDao) {
                if (projectFilePojo.getFileUuid().equals(paramsPojo.getFileUuid())) {
                    if (projectFilePojo.getParamsInfoList() == null) {
                        projectFilePojo.setParamsInfoList(new ArrayList<DocParamsPojo>());
                    }
                    projectFilePojo.getParamsInfoList().add(paramsPojo);
                }
            }
        }


        // 根据element_id把数据分块
        for (HfContentAssessElementPojo hfContentAssessElementPojo : assessElementListDao) {
            String paramsIdListStr = (String) hfContentAssessElementPojo.getParamsIdList();
            List<String> paramsIdList = JSON.parseObject(paramsIdListStr, List.class);
            if (hfContentAssessElementPojo.getParamsIdList() != null) {
                for (String paramsId : paramsIdList) {
                    for (ProjectFilePojo projectFilePojo : projectFilePojos2) {  // 遍历投标文件 找出参数id是paramsId的
                        List<DocParamsPojo> paramsInfoList = projectFilePojo.getParamsInfoList();
                        if (paramsInfoList != null) {
                            for (DocParamsPojo paramsPojo : paramsInfoList) {
                                if (paramsPojo.getParamsUuid().equals(paramsId)) {
                                    HashMap<String, Object> map = new HashMap<>();
                                    map.put("fileUuid", projectFilePojo.getFileUuid());
                                    map.put("fileName", projectFilePojo.getFileName());
                                    map.put("paramsText", paramsPojo.getParamsText());
                                    map.put("paramsId", paramsPojo.getParamsUuid());
                                    map.put("paramsType", paramsPojo.getParamsTypeId());
                                    map.put("paramsName", paramsPojo.getParamsName());
                                    if (hfContentAssessElementPojo.getParamsValueList() == null) {
                                        hfContentAssessElementPojo.setParamsValueList(new ArrayList<>());
                                    }
                                    hfContentAssessElementPojo.getParamsValueList().add(map);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 增加评审依据 quantizationStandard
        List<DocParamsPojo> contentParamListDao = workingTableMapper.getContentParamListDao(fileUuid);
        List<TagPojo> contentTagListDao = workingTableMapper.getContentTagListDao(fileUuid);
        for (HfContentAssessElementPojo hfContentAssessElementPojo : assessElementListDao) {
            String quantizationStandard = (String) hfContentAssessElementPojo.getQuantizationStandard();
            if (hfContentAssessElementPojo.getQuantizationStandard() != null) {
                Map<String, Object> map = JSON.parseObject(quantizationStandard, Map.class);
                String type = (String) map.get("type");
                if ("parameter".equals(type)) {
                    String value = (String) map.get("value");
                    for (DocParamsPojo paramsPojo : contentParamListDao) {
                        if (paramsPojo.getParamsUuid().equals(value)) {
                            hfContentAssessElementPojo.setQuantizationStandardContent((String) paramsPojo.getParamsText());
                            break;
                        }
                    }
                } else if ("mark".equals(type)) {
                    List<String> tagIdList = (List) map.get("value");
//                    List<String> tagIdList = JSON.parseObject(tagIdListStr, List.class);
                    ArrayList<String> strings = new ArrayList<>();
                    for (String tagId : tagIdList) {
                        for (TagPojo tagPojo : contentTagListDao) {
                            if (tagPojo.getTagId().equals(tagId)) {
                                strings.add(tagPojo.getTagContent());
                            }
                        }
                    }
                    String s = new JsonKeyUtils().listToString(strings, ";");
                    hfContentAssessElementPojo.setQuantizationStandardContent(s);
                } else if ("text".equals(type)) {
                    String value = (String) map.get("value");
                    hfContentAssessElementPojo.setQuantizationStandardContent(value);
                } else {

                }
            }
        }

        // 将paramsValueList的结果增加文件层级
        for (HfContentAssessElementPojo hfContentAssessElementPojo : assessElementListDao) {
            List<Map<String, Object>> paramsValueList = hfContentAssessElementPojo.getParamsValueList();
            ArrayList<String> strings = new ArrayList<>();
            ArrayList<Map<String, Object>> stringsMap = new ArrayList<>();
            if (paramsValueList != null) {
                for (Map<String, Object> map : paramsValueList) {
                    String fileUuid1 = (String) map.get("fileUuid");
                    if (strings.contains(fileUuid1)) {
                    } else {
                        strings.add(fileUuid1);
                        stringsMap.add(map);
                    }
                }

                List<Map<String, Object>> paramsValueListTmp = new ArrayList<>();
                paramsValueListTmp.addAll(paramsValueList);
                //深度复制
                List<Map<String, Object>> paramsValueListTmpnew = JSON.parseObject(JSON.toJSONString(paramsValueListTmp), List.class);

                for (Map<String, Object> map : stringsMap) {
                    map.remove("paramsText");
                    map.remove("paramsId");
                    map.remove("paramsType");
                    map.remove("paramsName");
                    for (Map<String, Object> stringObjectMap : paramsValueListTmpnew) {
                        stringObjectMap.remove("fileName");
                        if (((String) map.get("fileUuid")).equals((String) stringObjectMap.get("fileUuid"))) {
                            if (map.get("valueList") == null) {
                                map.put("valueList", new ArrayList<Map<String, Object>>());
                            }
                            ((List) map.get("valueList")).add(stringObjectMap);
                        }
                    }
                }
            }
            hfContentAssessElementPojo.setParamsValueList(stringsMap);
        }

        return TResponseVo.success(assessElementListDao);
    }

    /**
     * 创建文件-获取专业文档类型选择
     *
     * @param fileTypeGroupId
     */
    @Override
    public TResponseVo getFileTypeListService(String fileTypeGroupId) throws Exception {
        List<HfFileTypeDiPojo> fileTypeDiDao = workingTableMapper.getFileTypeDiDao(fileTypeGroupId);
        return TResponseVo.success(fileTypeDiDao);
    }

    /**
     * 创建文件-企业常用模板
     */
    @Override
    public TResponseVo getCompanyUseFileListService(Integer limit) throws Exception {
        List<DocFileIndexPojo> companyUseFileListDao = workingTableMapper.getCompanyUseFileListDao(limit);
        List<HfFileTypeDiPojo> FileTypeDi = workingTableMapper.getFileTypeDiDao("ALL");
        // 标签数据预搜索
        List<LabelValuePojo> labelLVListDao = fileOperationMapper.getLabelLVListDao(null);
        for (DocFileIndexPojo docParamsPojo : companyUseFileListDao) {  // 根据标签过滤数据
            String fileLabelList2 = docParamsPojo.getFileLabelList();
            List<String> strings = new JsonKeyUtils().stringToList(fileLabelList2);
            // 翻译标签内容
            List<String> fileLabelContentList = new ArrayList<>();
            for (String labelId : strings) {
                for (LabelValuePojo labelValuePojo : labelLVListDao) {
                    if (labelValuePojo.getValue().equals(labelId)) {
                        fileLabelContentList.add(labelValuePojo.getLabel());
                    }
                }
            }
            docParamsPojo.setFileLabelName(new JsonKeyUtils().listToString(fileLabelContentList));

            // 设置文件类型名称
            for (HfFileTypeDiPojo hfFileTypeDiPojo : FileTypeDi) {
                if (hfFileTypeDiPojo.getFileTypeId().equals(docParamsPojo.getFileTypeId())) {
                    docParamsPojo.setFileTypeName(hfFileTypeDiPojo.getFileTypeName());
                    break;
                }
            }
        }

        return TResponseVo.success(companyUseFileListDao);
    }


    /**
     * 评审步骤模板配置-新增评审步骤模板
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo createFastAssessService(HfFastAssessPojo hfFastAssessPojo) throws Exception {
        String createUserId = hfFastAssessPojo.getCreateUserId();
        String fastId = UUID.randomUUID().toString().replaceAll("-", "");
        // 初评的所有方案集合
        List<Map<String, Object>> assessModelList = (List) hfFastAssessPojo.getAssessModelList();
        for (Map<String, Object> map : assessModelList) {  // 增加模型类型
            List<String> assessIdList = (List) map.get("value");
            String assessIdListStr = new JsonKeyUtils().listToString(assessIdList, "','");
            String tabUuid = UUID.randomUUID().toString().replaceAll("-", "");
            fileOperationMapper.copyAssessElement2Dao(assessIdListStr, fastId, createUserId, tabUuid);
            map.put("value", tabUuid);
            map.put("assessIdList", assessIdList);
            map.put("modelType", "判断");
        }
        hfFastAssessPojo.setAssessModelList(JSON.toJSONString(assessModelList));
        // 详评的所有方案集合
        if (hfFastAssessPojo.getAssessDetailedModelList() != null && ((List) hfFastAssessPojo.getAssessDetailedModelList()).size() != 0) {
            List<Map<String, Object>> assessDetailedModelList = (List) hfFastAssessPojo.getAssessDetailedModelList();
            for (Map<String, Object> map : assessDetailedModelList) {  // 增加模型类型
                List<String> assessIdList = (List) map.get("value");
                String assessIdListStr = new JsonKeyUtils().listToString(assessIdList, "','");
                String tabUuid = UUID.randomUUID().toString().replaceAll("-", "");
                fileOperationMapper.copyAssessElement2Dao(assessIdListStr, fastId, createUserId, tabUuid);
                map.put("value", tabUuid);
                map.put("assessIdList", assessIdList);
                map.put("modelType", "评分");
            }
            hfFastAssessPojo.setAssessDetailedModelList(JSON.toJSONString(assessDetailedModelList));

            // 分值构成
            if (hfFastAssessPojo.getScoreRat() == null && ((List) hfFastAssessPojo.getScoreRat()).size() == 0) {
                throw new WorkTableException("未配置分值构成比例");
            }
            List<Map<String, Object>> scoreRat = (List) hfFastAssessPojo.getScoreRat();
            int tot = 0;
            for (Map<String, Object> map : scoreRat) {
                int s = (Integer) map.get("value");
                tot = tot + s;
            }
            if (tot != 100) {
                throw new WorkTableException("分数占比总和不为100%");
            }
            // 方案重复性判断
            ArrayList<String> strings = new ArrayList<>();
            for (Map<String, Object> map : scoreRat) {
                String label = String.valueOf(map.get("label"));
                if (strings.contains(label)) {
                    throw new WorkTableException("方案名称重复,请检查");
                } else {
                    strings.add(label);
                }
            }
            hfFastAssessPojo.setScoreRat(JSON.toJSONString(scoreRat));
        } else {
            hfFastAssessPojo.setAssessDetailedModelList(null);
            hfFastAssessPojo.setScoreRat(null);
        }

        hfFastAssessPojo.setFastId(fastId);
        fileOperationMapper.confirmAssessTotalPlan2Dao(hfFastAssessPojo);
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("info", "创建完成");
        retMap.put("fastId", fastId);
        return TResponseVo.success(retMap);
    }


    /**
     * 评审步骤模板配置-删除评审步骤模板
     *
     * @param hfFastAssessPojo
     */
    @Override
    public TResponseVo delFastAssessService(HfFastAssessPojo hfFastAssessPojo) throws Exception {
        String fastId = hfFastAssessPojo.getFastId();
        if (StringUtils.isBlank(fastId)) {
            return TResponseVo.error("必填参数为空");
        }
//        HfFastAssessPojo fastAssessDao = fileOperationMapper.getFastAssessDao(fastId);
//        String assessModelList = (String)fastAssessDao.getAssessModelList();  // 初评
//        List<Map<String, String>> list = JSON.parseObject(assessModelList, List.class);
//
//        String assessDetailedModelList = (String)fastAssessDao.getAssessDetailedModelList();  // 详评
//        List<Map<String, String>> list2 = JSON.parseObject(assessDetailedModelList, List.class);
//
//        ArrayList<String> tabUuidList = new ArrayList<>();
//        if (list != null) {
//            for (Map<String, String> map : list) {
//                String tabUuid = map.get("value");
//                tabUuidList.add(tabUuid);
//            }
//        }
//        if (list2 != null) {
//            for (Map<String, String> map : list2) {
//                String tabUuid = map.get("value");
//                tabUuidList.add(tabUuid);
//            }
//        }
        fileOperationMapper.delFastAssessElementDao(fastId);
        fileOperationMapper.delFastAssessDao(fastId);

        return TResponseVo.success("删除完成");
    }

    /**
     * 评审步骤模板配置-更新评审步骤模板
     *
     * @param hfFastAssessPojo
     */
    @Override
    public TResponseVo updateFastAssessService(HfFastAssessPojo hfFastAssessPojo) throws Exception {
        String fastId = hfFastAssessPojo.getFastId();
        if (StringUtils.isBlank(fastId)) {
            return TResponseVo.error("必填参数为空");
        }

        // 先清空以前的方案
        delFastAssessService(hfFastAssessPojo);

        String createUserId = hfFastAssessPojo.getCreateUserId();
        // 初评的所有方案集合
//        List<HfAssessModelPojo> assessModelListDao = workingTableMapper.getAssessModelListDao(null);
        List<Map<String, Object>> assessModelList = (List) hfFastAssessPojo.getAssessModelList();
        for (Map<String, Object> map : assessModelList) {  // 增加模型类型
            List<String> assessIdList = (List) map.get("value");
            String assessIdListStr = new JsonKeyUtils().listToString(assessIdList, "','");
            String tabUuid = UUID.randomUUID().toString().replaceAll("-", "");
            fileOperationMapper.copyAssessElement2Dao(assessIdListStr, fastId, createUserId, tabUuid);
            map.put("value", tabUuid);
            map.put("assessIdList", assessIdList);
            map.put("modelType", "判断");
        }
        hfFastAssessPojo.setAssessModelList(JSON.toJSONString(assessModelList));
        // 详评的所有方案集合
        if (hfFastAssessPojo.getAssessDetailedModelList() != null && ((List) hfFastAssessPojo.getAssessDetailedModelList()).size() != 0) {
            List<Map<String, Object>> assessDetailedModelList = (List) hfFastAssessPojo.getAssessDetailedModelList();
            for (Map<String, Object> map : assessDetailedModelList) {  // 增加模型类型
                List<String> assessIdList = (List) map.get("value");
                String assessIdListStr = new JsonKeyUtils().listToString(assessIdList, "','");
                String tabUuid = UUID.randomUUID().toString().replaceAll("-", "");
                fileOperationMapper.copyAssessElement2Dao(assessIdListStr, fastId, createUserId, tabUuid);
                map.put("value", tabUuid);
                map.put("assessIdList", assessIdList);
                map.put("modelType", "评分");
            }
            hfFastAssessPojo.setAssessDetailedModelList(JSON.toJSONString(assessDetailedModelList));

            // 分值构成
            if (hfFastAssessPojo.getScoreRat() == null && ((List) hfFastAssessPojo.getScoreRat()).size() == 0) {
                throw new WorkTableException("未配置分值构成比例");
            }
            List<Map<String, Object>> scoreRat = (List) hfFastAssessPojo.getScoreRat();
            int tot = 0;
            for (Map<String, Object> map : scoreRat) {
                int s = (Integer) map.get("value");
                tot = tot + s;
            }
            if (tot != 100) {
                throw new WorkTableException("分数占比总和不为100%");
            }
            // 方案重复性判断
            ArrayList<String> strings = new ArrayList<>();
            for (Map<String, Object> map : scoreRat) {
                String label = String.valueOf(map.get("label"));
                if (strings.contains(label)) {
                    throw new WorkTableException("方案名称重复,请检查");
                } else {
                    strings.add(label);
                }
            }
            hfFastAssessPojo.setScoreRat(JSON.toJSONString(scoreRat));
        } else {
            hfFastAssessPojo.setAssessDetailedModelList(null);
            hfFastAssessPojo.setScoreRat(null);
        }

        hfFastAssessPojo.setFastId(fastId);
        fileOperationMapper.confirmAssessTotalPlan2Dao(hfFastAssessPojo);

        return TResponseVo.success("更新完成");
    }

    /**
     * 评审步骤模板配置-获取评审步骤模板信息
     *
     * @param fastId
     */
    @Override
    public TResponseVo getFastAssessInfoService(String fastId) throws Exception {
        HfFastAssessPojo hfFastAssessPojo = fileOperationMapper.getFastAssessInfoDao(fastId);
        if (hfFastAssessPojo == null) {
            return TResponseVo.error("该文件下无评审方案");
        }

        List<HfFastAssessElementPojo> contentAssessElementListDao = fileOperationMapper.getFastAssessElementListDao(fastId, null);
        // 评审模型集合(初评和详评)
        String assessModelList = (String) hfFastAssessPojo.getAssessModelList();
        List<Map<String, Object>> list = JSON.parseObject(assessModelList, List.class);
        for (Map<String, Object> map : list) {
            if (map.get("assessIdList") != null) {  // 兼容老数据 没有assessIdList的情况
                List<String> assessIdList = (List<String>) map.get("assessIdList");
                map.put("value", assessIdList);
            } else {
                String tabUuid = (String) map.get("value");
                List<String> assessIdList = new ArrayList<>();
                for (HfFastAssessElementPojo hfContentAssessElementPojo : contentAssessElementListDao) {
                    if (hfContentAssessElementPojo.getTabUuid().equals(tabUuid)) {
                        assessIdList.add(hfContentAssessElementPojo.getAssessId());
                    }
                }
                map.put("value", assessIdList);
            }
        }
        hfFastAssessPojo.setAssessModelList(list);

        if (hfFastAssessPojo.getAssessDetailedModelList() != null && StringUtils.isNotBlank((String) hfFastAssessPojo.getAssessDetailedModelList())) {
            String assessDetailedModelList = (String) hfFastAssessPojo.getAssessDetailedModelList();
            List<Map<String, Object>> list2 = JSON.parseObject(assessDetailedModelList, List.class);
            for (Map<String, Object> map : list2) {
                if (map.get("assessIdList") != null) {  // 兼容老数据 没有assessIdList的情况
                    List<String> assessIdList = (List<String>) map.get("assessIdList");
                    map.put("value", assessIdList);
                } else {
                    String tabUuid = (String) map.get("value");
                    List<String> assessIdList = new ArrayList<>();
                    for (HfFastAssessElementPojo hfContentAssessElementPojo : contentAssessElementListDao) {
                        if (hfContentAssessElementPojo.getTabUuid().equals(tabUuid)) {
                            assessIdList.add(hfContentAssessElementPojo.getAssessId());
                        }
                    }
                    map.put("value", assessIdList);
                }
            }
            hfFastAssessPojo.setAssessDetailedModelList(list2);
        }
        // 分值构成
        String scoreRat = (String) hfFastAssessPojo.getScoreRat();
        List list1 = JSON.parseObject(scoreRat, List.class);
        hfFastAssessPojo.setScoreRat(list1);
        return TResponseVo.success(hfFastAssessPojo);
    }

    /**
     * 评审步骤模板配置-获取评审步骤模板清单
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getFastAssessListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();
        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<HfFastAssessPojo> hfFastAssessPojos = new ArrayList<>();
        if ("0".equalsIgnoreCase(isPaged)) {
            hfFastAssessPojos = fileOperationMapper.getFastAssessListDao(paramNameLike);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            hfFastAssessPojos = fileOperationMapper.getFastAssessListDao(paramNameLike);
        }
        for (HfFastAssessPojo hfFastAssessPojo : hfFastAssessPojos) {
            String fastId = hfFastAssessPojo.getFastId();
            List<HfFastAssessElementPojo> contentAssessElementListDao = fileOperationMapper.getFastAssessElementListDao(fastId, null);
            // 评审模型集合(初评和详评)
            String assessModelList = (String) hfFastAssessPojo.getAssessModelList();
            List<Map<String, Object>> list = JSON.parseObject(assessModelList, List.class);
            for (Map<String, Object> map : list) {
                if (map.get("assessIdList") != null) {  // 兼容老数据 没有assessIdList的情况
                    List<String> assessIdList = (List<String>) map.get("assessIdList");
                    map.put("value", assessIdList);
                } else {
                    String tabUuid = (String) map.get("value");
                    List<String> assessIdList = new ArrayList<>();
                    for (HfFastAssessElementPojo hfContentAssessElementPojo : contentAssessElementListDao) {
                        if (hfContentAssessElementPojo.getTabUuid().equals(tabUuid)) {
                            assessIdList.add(hfContentAssessElementPojo.getAssessId());
                        }
                    }
                    map.put("value", assessIdList);
                }
            }
            hfFastAssessPojo.setAssessModelList(list);

            if (hfFastAssessPojo.getAssessDetailedModelList() != null && StringUtils.isNotBlank((String) hfFastAssessPojo.getAssessDetailedModelList())) {
                String assessDetailedModelList = (String) hfFastAssessPojo.getAssessDetailedModelList();
                List<Map<String, Object>> list2 = JSON.parseObject(assessDetailedModelList, List.class);
                for (Map<String, Object> map : list2) {
                    if (map.get("assessIdList") != null) {  // 兼容老数据 没有assessIdList的情况
                        List<String> assessIdList = (List<String>) map.get("assessIdList");
                        map.put("value", assessIdList);
                    } else {
                        String tabUuid = (String) map.get("value");
                        List<String> assessIdList = new ArrayList<>();
                        for (HfFastAssessElementPojo hfContentAssessElementPojo : contentAssessElementListDao) {
                            if (hfContentAssessElementPojo.getTabUuid().equals(tabUuid)) {
                                assessIdList.add(hfContentAssessElementPojo.getAssessId());
                            }
                        }
                        map.put("value", assessIdList);
                    }
                }
                hfFastAssessPojo.setAssessDetailedModelList(list2);
            }
            // 分值构成
            String scoreRat = (String) hfFastAssessPojo.getScoreRat();
            List list1 = JSON.parseObject(scoreRat, List.class);
            hfFastAssessPojo.setScoreRat(list1);
        }


        if ("0".equalsIgnoreCase(isPaged)) {
            return TResponseVo.success(hfFastAssessPojos);
        } else {
            PageInfo pageInfo = new PageInfo(hfFastAssessPojos);
            return TResponseVo.success(pageInfo);
        }
    }

    /**
     * 评审步骤模板配置-获取评审步骤模板清单LV
     */
    @Override
    public TResponseVo getFastAssessListLVService(PagePojo pagePojo) throws Exception {
        String paramNameLike = pagePojo.getParamNameLike();
        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }
        List<LabelValuePojo> labelValuePojos = fileOperationMapper.getFastAssessListLVDao(paramNameLike);
        return TResponseVo.success(labelValuePojos);
    }

    /**
     * 评审步骤模板配置-读取预览
     *
     * @param fastId
     */
    @Override
    public TResponseVo getFastAssessPreviewService(String fastId) throws Exception {
        HfFastAssessPojo hfFastAssessPojo = fileOperationMapper.getFastAssessInfoDao(fastId);
        if (hfFastAssessPojo == null) {
            return TResponseVo.error("该文件下无评审方案");
        }
        List<LabelValuePojo> assessModelParamDao = workingTableMapper.getAssessModelParamDao(null, null);  // 获取参数名

        ArrayList<Object> resultList = new ArrayList<>();
        List assessModelList = (List) JSON.parseObject(String.valueOf(hfFastAssessPojo.getAssessModelList()), List.class);  // 初评模板
        List assessDetailedModelList = (List) JSON.parseObject(String.valueOf(hfFastAssessPojo.getAssessDetailedModelList()), List.class);  // 详评模板
        if (assessDetailedModelList != null) {
            assessModelList.addAll(assessDetailedModelList);
        }
        for (Object assessModelObj : assessModelList) {
            Map assessModel = JSON.parseObject(String.valueOf(assessModelObj), Map.class);
            List<String> assessIdList = (List<String>) assessModel.get("assessIdList");
            String tabUuid = String.valueOf(assessModel.get("value"));

            String type = (String) assessModel.get("modelType");
            String title = (String) assessModel.get("label");
            for (String assessId : assessIdList) {
                List<HfFastAssessElementPojo> assessModelListDao = workingTableMapper.getFastAssessElementListDao(null, fastId, tabUuid, assessId);
                for (HfFastAssessElementPojo assessModelElementInfoDao : assessModelListDao) {
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
                    Map<String, String> map = null;
                    map = (Map) assessModelElementInfoDao.getElementStandard();
                    if (map == null) {
                        map = (Map) assessModelElementInfoDao.getQuantizationStandard();
                    }
                    if (map == null) {
                        assessModelElementInfoDao.setStandardText(null);
                    } else if ("text".equals(map.get("type"))) {
                        assessModelElementInfoDao.setStandardText(map.get("value"));
                    } else {  // 是参数
                        for (LabelValuePojo labelValuePojo : assessModelParamDao) {
                            if (map.get("value") != null && map.get("value").equals(labelValuePojo.getValue())) {
//                            assessModelElementInfoDao.setParamsName();
                                assessModelElementInfoDao.setStandardText("【" + labelValuePojo.getLabel() + "】");
                                break;
                            }
                        }
                    }
                }

                // 创建二维数组
                ArrayList<String> elementTypeList = new ArrayList<>();
                ArrayList<List<HfFastAssessElementPojo>> lists = new ArrayList<>();
                for (HfFastAssessElementPojo hfAssessModelElementPojo : assessModelListDao) {
                    String elementType = hfAssessModelElementPojo.getElementType();
                    if (elementTypeList.contains(elementType)) {
                        for (List<HfFastAssessElementPojo> list : lists) {
                            if (elementType.equals(list.get(0).getElementType())) {
                                list.add(hfAssessModelElementPojo);
                                break;
                            }
                        }
                    } else {
                        ArrayList<HfFastAssessElementPojo> hfAssessModelElementList = new ArrayList<>();
                        elementTypeList.add(elementType);
                        hfAssessModelElementList.add(hfAssessModelElementPojo);
                        lists.add(hfAssessModelElementList);
                    }
                }
                // 对二维数组中的元素排序
                ArrayList<List<HfFastAssessElementPojo>> listsTmp = new ArrayList<>();
                listsTmp.addAll(lists);
                for (List<HfFastAssessElementPojo> list : listsTmp) {
                    List<HfFastAssessElementPojo> retList = new JsonKeyUtils().orderMapList51(list);
                    lists.remove(list);
                    lists.add(retList);
                }
                // 获取条款顺序
                List<HfFastAssessElementPojo> elementTypeOrderListDao = workingTableMapper.getFastElementTypeOrderListDao(fastId, tabUuid, assessId);
                ArrayList<List<HfFastAssessElementPojo>> listsTmp2 = new ArrayList<>();
                for (HfFastAssessElementPojo hfAssessModelElementPojo : elementTypeOrderListDao) {
                    String elementType = hfAssessModelElementPojo.getElementType();
                    for (List<HfFastAssessElementPojo> h : lists) {
                        if (elementType.equals(h.get(0).getElementType())) {
                            listsTmp2.add(h);
                            break;
                        }
                    }
                }

                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("type", type);
                resultMap.put("title", title);
                resultMap.put("dataSource", listsTmp2);
                resultList.add(resultMap);
            }

        }

        return TResponseVo.success(resultList);
    }

    /**
     * 通过工具服务链接获取同步数据
     *
     * @param docFileIndexPojo
     */
    @Override
    public void syncFileService(DocFileIndexPojo docFileIndexPojo, HttpServletResponse response) throws Exception {
        // 获取父文件的fileUuid fileVersionId
        String fileUuid = docFileIndexPojo.getFileUuid();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            TResponseVo.error(ResponseEnum.ERROR, "附属文件无派生功能");
        }
        docFileIndexPojo.setFileVersionId(docAllInfoDao.getFileVersionId());

        String newFileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        String newFileVersionId = UUID.randomUUID().toString().replaceAll("-", "");

        // 获取索引
//        fileOperationMapper.addDeriveFileIndexDao2(docFileIndexPojo);
        DocFileIndexPojo docFileIndexPojo1 = workingTableMapper.syncFileIndexDao(docFileIndexPojo);
        docFileIndexPojo1.setFileUuid(newFileUuid);
        docFileIndexPojo1.setFileVersionId(newFileVersionId);


        // 获取版本
//        fileOperationMapper.addDeriveFileVersionDao(docFileIndexPojo);
        DocFileVerIndexPojo docFileVerIndexPojo = workingTableMapper.syncFileVersionDao(docFileIndexPojo);
        docFileVerIndexPojo.setFileUuid(newFileUuid);
        docFileVerIndexPojo.setFileVersionId(newFileVersionId);

        // 创建大纲
        List<OutLinePojo> getDeriveFileOutlineDao = workingTableMapper.syncFileOutlineDao(docFileIndexPojo);
        for (OutLinePojo outLinePojo : getDeriveFileOutlineDao) {
            outLinePojo.setFileUuid(newFileUuid);
            outLinePojo.setFileVersionId(newFileVersionId);
        }

        // 创建内容
        List<ContentPojo> deriveFileContentDao = workingTableMapper.syncFileContentDao(docFileIndexPojo);
        for (ContentPojo outLinePojo : deriveFileContentDao) {
            outLinePojo.setFileUuid(newFileUuid);
            outLinePojo.setFileVersionId(newFileVersionId);
        }

        // 创建参数
        List<DocParamsPojo> paramsPojos = workingTableMapper.syncDeriveContentParamDao(docFileIndexPojo);
        for (DocParamsPojo outLinePojo : paramsPojos) {
            outLinePojo.setFileUuid(newFileUuid);
            outLinePojo.setFileVersionId(newFileVersionId);
        }


        // 创建标注
        List<TagPojo> tagPojos = workingTableMapper.syncDeriveContentTagDao(docFileIndexPojo);
        for (TagPojo outLinePojo : tagPojos) {
            outLinePojo.setFileUuid(newFileUuid);
            outLinePojo.setFileVersionId(newFileVersionId);
        }


        // 创建书签
        List<BookmarkPojo> bookmarkPojos = workingTableMapper.syncDeriveContentBookmarkDao(docFileIndexPojo);
        for (BookmarkPojo outLinePojo : bookmarkPojos) {
            outLinePojo.setFileUuid(newFileUuid);
            outLinePojo.setFileVersionId(newFileVersionId);
        }

        // 评审工具(如果有)
        HfContentAssessPojo contentAssessInfoDao = fileOperationMapper.getContentAssessInfoDao(fileUuid);
        HfContentAssessPojo hfContentAssessPojo = null;
        List<HfContentAssessElementPojo> hfContentAssessElementPojos = null;
        if (contentAssessInfoDao != null) {
            // 将评审工具也复制放到新文件里
            hfContentAssessPojo = workingTableMapper.syncDeriveFileAssessDao(fileUuid);
            hfContentAssessPojo.setFileUuid(newFileUuid);
            hfContentAssessPojo.setFileVersionId(newFileVersionId);
            hfContentAssessElementPojos = workingTableMapper.syncDeriveFileAssessElementDao(fileUuid);
            for (HfContentAssessElementPojo hfContentAssessElementPojo : hfContentAssessElementPojos) {
                hfContentAssessElementPojo.setFileUuid(newFileUuid);
                hfContentAssessElementPojo.setFileVersionId(newFileVersionId);
            }
        }

        HashMap<String, Object> mapRet = new HashMap<>();
        mapRet.put("docFileIndexPojo", docFileIndexPojo1);
        mapRet.put("docFileVerIndexPojo", docFileVerIndexPojo);
        mapRet.put("deriveFileOutlineDao", getDeriveFileOutlineDao);
        mapRet.put("deriveFileContentDao", deriveFileContentDao);
        mapRet.put("paramsPojos", paramsPojos);
        mapRet.put("tagPojos", tagPojos);
        mapRet.put("bookmarkPojos", bookmarkPojos);
        mapRet.put("HfContentAssessPojo", hfContentAssessPojo);
        mapRet.put("hfContentAssessElementPojos", hfContentAssessElementPojos);

        String s = JSON.toJSONString(mapRet);

        ServletOutputStream outputStream = null;
        response.setContentType("application/octet-stream;charset=UTF-8");
        byte[] buffer = new byte[1024];
        InputStream inputStream = null;
        BufferedInputStream bis = null;
        try {
            inputStream = new ByteArrayInputStream(s.getBytes("UTF-8"));
            bis = new BufferedInputStream(inputStream);
            outputStream = response.getOutputStream();
            int i = bis.read(buffer);
            while (i != -1) {
                outputStream.write(buffer, 0, i);
                i = bis.read(buffer);
            }
            outputStream.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 接收离线同步数据-上传
     *
     * @param map
     */
    @Override
    public Map<String, String> upLoadSyncDataService(Map<String, Object> map, String fileName, String fileDesc, List<String> fileLabelIds, String userId) throws Exception {
        Map<String, Object> docFileIndexPojoMap = (Map) map.get("docFileIndexPojo");
        Map<String, Object> docFileVerIndexPojoMap = (Map) map.get("docFileVerIndexPojo");
        List<Map<String, Object>> deriveFileOutlineDao = (List) map.get("deriveFileOutlineDao");
        List<Map<String, Object>> deriveFileContentDao = (List) map.get("deriveFileContentDao");
        List<Map<String, Object>> paramsPojosDao = (List) map.get("paramsPojos");
        List<Map<String, Object>> tagPojosDao = (List) map.get("tagPojos");
        List<Map<String, Object>> bookmarkPojosDao = (List) map.get("bookmarkPojos");
        Map<String, Object> hfContentAssessPojoMap = (Map) map.get("hfContentAssessPojo");
        List<Map<String, Object>> hfContentAssessElementPojosDao = (List) map.get("hfContentAssessElementPojos");


        String newFileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        String newFileVersionId = UUID.randomUUID().toString().replaceAll("-", "");

        // map转实体
        DocFileIndexPojo docFileIndexPojo = JSON.parseObject(JSON.toJSONString(docFileIndexPojoMap), DocFileIndexPojo.class);
        docFileIndexPojo.setFileUuid(newFileUuid);
        docFileIndexPojo.setFileVersionId(newFileVersionId);
        docFileIndexPojo.setCreateUserId(userId);
        docFileIndexPojo.setFileParentId(null);
        if (fileLabelIds != null && fileLabelIds.size() != 0) {
            String s = new JsonKeyUtils().listToString(fileLabelIds);
            docFileIndexPojo.setFileLabelList(s);
        }
        if (!StringUtils.isBlank(fileName)) {
            docFileIndexPojo.setFileName(fileName);
        }
        if (!StringUtils.isBlank(fileDesc)) {
            docFileIndexPojo.setFileDesc(fileDesc);
        }
        workingTableMapper.uploadSyncFileIndexDao(docFileIndexPojo);

        DocFileVerIndexPojo docFileVerIndexPojo = JSON.parseObject(JSON.toJSONString(docFileVerIndexPojoMap), DocFileVerIndexPojo.class);
        docFileVerIndexPojo.setFileUuid(newFileUuid);
        docFileVerIndexPojo.setFileVersionId(newFileVersionId);
        docFileVerIndexPojo.setCreateUserId(userId);
        docFileVerIndexPojo.setFileVersionName("V1.0");
        workingTableMapper.uploadSyncFileVerIndexDao(docFileVerIndexPojo);

        if (deriveFileOutlineDao != null && deriveFileOutlineDao.size() != 0) {
            // 设置groupId替换用的键值
            HashMap<String, String> groupIdMap = new HashMap<>();
            for (Map<String, Object> m : deriveFileOutlineDao) {
                OutLinePojo outLinePojo = JSON.parseObject(JSON.toJSONString(m), OutLinePojo.class);
                if (outLinePojo.getOutlineReplaceGroupId() == null) {
                    continue;
                }
                String outlineReplaceGroupId = outLinePojo.getOutlineReplaceGroupId();
                groupIdMap.put(outlineReplaceGroupId, UUID.randomUUID().toString().replaceAll("-", ""));
            }
//            int j = 0;
//            String sql = "";
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            String dateString = formatter.format(new Date());
            ArrayList<OutLinePojo> outLinePojos = new ArrayList<>();
            for (Map<String, Object> m : deriveFileOutlineDao) {
                OutLinePojo outLinePojo = JSON.parseObject(JSON.toJSONString(m), OutLinePojo.class);
//                j++;
                if (groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()) != null) {
                    outLinePojo.setOutlineReplaceGroupId(groupIdMap.get(outLinePojo.getOutlineReplaceGroupId()));
                }
                outLinePojo.setFileUuid(newFileUuid);
                outLinePojo.setFileVersionId(newFileVersionId);
                outLinePojo.setCreateUserId(userId);
                outLinePojos.add(outLinePojo);
            }
            workingTableMapper.uploadSyncFileOutlineDao(outLinePojos);
        }

        // 创建内容
        if (deriveFileContentDao != null && deriveFileContentDao.size() != 0) {
            ArrayList<ContentPojo> contentPojos = new ArrayList<>();
            for (Map<String, Object> m : deriveFileContentDao) {
                ContentPojo contentPojo = JSON.parseObject(JSON.toJSONString(m), ContentPojo.class);
                String contentText = contentPojo.getContentText();
                contentText = contentText.replaceAll("</br>", "");
                contentPojo.setContentText(contentText);
                contentPojo.setFileUuid(newFileUuid);
                contentPojo.setFileVersionId(newFileVersionId);
                contentPojos.add(contentPojo);
            }
            workingTableMapper.uploadSyncFileContentDao(contentPojos);
        }

        // 创建参数
        if (paramsPojosDao != null && paramsPojosDao.size() != 0) {
            ArrayList<DocParamsPojo> paramsPojos = new ArrayList<>();
            for (Map<String, Object> m : paramsPojosDao) {
                DocParamsPojo paramsPojo = JSON.parseObject(JSON.toJSONString(m), DocParamsPojo.class);
                paramsPojo.setFileUuid(newFileUuid);
                paramsPojo.setFileVersionId(newFileVersionId);
                paramsPojo.setCreateUserId(userId);
                paramsPojos.add(paramsPojo);
            }
            workingTableMapper.uploadSyncFileParamsDao(paramsPojos);
        }

        // 创建标注
        if (tagPojosDao != null && tagPojosDao.size() != 0) {
            ArrayList<TagPojo> tagPojos = new ArrayList<>();
            for (Map<String, Object> m : tagPojosDao) {
                TagPojo tagPojo = JSON.parseObject(JSON.toJSONString(m), TagPojo.class);
                tagPojo.setFileUuid(newFileUuid);
                tagPojo.setFileVersionId(newFileVersionId);
                tagPojo.setCreateUserId(userId);
                tagPojos.add(tagPojo);
            }
            workingTableMapper.uploadSyncFileTagsDao(tagPojos);
        }

        // 创建书签
        if (bookmarkPojosDao != null && bookmarkPojosDao.size() != 0) {
            ArrayList<BookmarkPojo> bookmarkPojos = new ArrayList<>();
            for (Map<String, Object> m : bookmarkPojosDao) {
                BookmarkPojo bookmarkPojo = JSON.parseObject(JSON.toJSONString(m), BookmarkPojo.class);
                bookmarkPojo.setFileUuid(newFileUuid);
                bookmarkPojo.setFileVersionId(newFileVersionId);
                bookmarkPojo.setCreateUserId(userId);
                bookmarkPojos.add(bookmarkPojo);
            }
            workingTableMapper.uploadSyncFileBookmarksDao(bookmarkPojos);
        }

        // 评审工具(如果有)
        HfContentAssessPojo hfContentAssessPojo = JSON.parseObject(JSON.toJSONString(hfContentAssessPojoMap), HfContentAssessPojo.class);
        if (hfContentAssessPojo != null) {
            hfContentAssessPojo.setFileUuid(newFileUuid);
            hfContentAssessPojo.setFileVersionId(newFileVersionId);
            hfContentAssessPojo.setCreateUserId(userId);
            workingTableMapper.uploadSyncFileAssessDao(hfContentAssessPojo);
        }
        ArrayList<HfContentAssessElementPojo> hfContentAssessElementPojos = new ArrayList<>();
        if (hfContentAssessPojo != null) {
            for (Map<String, Object> m : hfContentAssessElementPojosDao) {
                HfContentAssessElementPojo hfContentAssessElementPojo = JSON.parseObject(JSON.toJSONString(m), HfContentAssessElementPojo.class);
                hfContentAssessElementPojo.setFileUuid(newFileUuid);
                hfContentAssessElementPojo.setFileVersionId(newFileVersionId);
                hfContentAssessElementPojo.setCreateUserId(userId);
                hfContentAssessElementPojos.add(hfContentAssessElementPojo);
            }
            workingTableMapper.uploadSyncFileAssessElementsDao(hfContentAssessElementPojos);
        }

        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("fileUuid", newFileUuid);
        retMap.put("fileVersionId", newFileVersionId);
        retMap.put("fileTypeId", docFileIndexPojo.getFileTypeId());
        return retMap;
    }

    /**
     * 获取菜单类型
     */
    @Override
    public TResponseVo getOptionsTypeListService() throws Exception {
        List<LabelValuePojo> optionsTypeListDao = workingTableMapper.getOptionsTypeListDao(null);
        return TResponseVo.success(optionsTypeListDao);
    }

    /**
     * 获取所有下拉菜单选项
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getOptionsListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }


        List<OptionsPojo> optionsListDao = new ArrayList<>();
        String type = pagePojo.getType();

        if ("0".equalsIgnoreCase(isPaged)) {
            optionsListDao = workingTableMapper.getOptionsListDao(type, paramNameLike);
            return TResponseVo.success(optionsListDao);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            optionsListDao = workingTableMapper.getOptionsListDao(type, paramNameLike);
            PageInfo pageInfo = new PageInfo(optionsListDao);
            return TResponseVo.success(pageInfo);
        }

    }

    /**
     * 新增菜单
     *
     * @param optionsPojo
     */
    @Override
    public TResponseVo addOptionsService(OptionsPojo optionsPojo) throws Exception {
        String label = optionsPojo.getLabel();
        String type = optionsPojo.getType();
        if (StringUtils.isBlank(label) || StringUtils.isBlank(type)) {
            throw new WorkTableException("必填参数为空");
        }
        // 校验重复
        Integer i = 0;
        i = workingTableMapper.checkOptionsDao(label, type);
        if (i >= 1) {
            throw new WorkTableException("该类型下已存在相同菜单名称");
        }
        List<LabelValuePojo> optionsTypeList = workingTableMapper.getOptionsTypeListDao("all");
        for (LabelValuePojo labelValuePojo : optionsTypeList) {
            if (labelValuePojo.getValue().equals(type)) {
                optionsPojo.setTypeDesc(labelValuePojo.getLabel());
                break;
            }
        }
        String id = UUID.randomUUID().toString().replaceAll("-", "");
        optionsPojo.setId(id);
        optionsPojo.setValue(label);
        optionsPojo.setIsDel(false);
        optionsPojo.setIsShow(true);
        optionsPojo.setAllowDel(true);
        workingTableMapper.addOptionsDao(optionsPojo);
        return TResponseVo.success("添加完成");
    }

    /**
     * 删除菜单
     *
     * @param id
     */
    @Override
    public TResponseVo delOptionsService(String id) throws Exception {
        workingTableMapper.delOptionsDao(id);
        return TResponseVo.success("删除完成");
    }


    /**
     * 数据管理
     */
    /**
     * 新增数据管理工具
     *
     * @param hfDmDb
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo createDmDbService(HfDmDb hfDmDb) throws Exception {
        String title = hfDmDb.getTitle();
        if (StringUtils.isBlank(title)) {
            throw new WorkTableException("必填参数为空");
        }
        String createUserId = hfDmDb.getCreateUserId();
//        Integer integer = workingTableMapper.checkDmDbNameDao(title, null, createUserId);
//        if (integer > 0) {
//            throw new WorkTableException("该数据库名称已存在");
//        }
        hfDmDb.setDbName(title);
        hfDmDb.setDbDesc(hfDmDb.getDesc());
        hfDmDb.setDbId(UUID.randomUUID().toString().replaceAll("-", ""));
        workingTableMapper.createDmDbDao(hfDmDb);
        return TResponseVo.success("数据库创建成功");
    }

    @Override
    public TResponseVo createDmDbInFileService(HfDmDb hfDmDb) throws Exception {
        String title = hfDmDb.getTitle();
        if (StringUtils.isBlank(title)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询主文件
        String fileUuid = hfDmDb.getFileUuid();
        String fileVersionId = hfDmDb.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            hfDmDb.setFileUuid(fileUuid);
            hfDmDb.setFileVersionId(fileVersionId);
        }
//        Integer integer = workingTableMapper.checkDmDbNameInFileDao(title, null, fileUuid, fileVersionId);
//        if (integer > 0) {
//            throw new WorkTableException("该数据库名称已存在");
//        }
        hfDmDb.setDbName(title);
        hfDmDb.setDbDesc(hfDmDb.getDesc());
        hfDmDb.setDbId(UUID.randomUUID().toString().replaceAll("-", ""));
        workingTableMapper.createDmDbInFileDao(hfDmDb);
        return TResponseVo.success("数据库创建成功");
    }

    /**
     * 删除数据管理工具
     *
     * @param dbId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delDmDbService(String dbId) throws Exception {
        workingTableMapper.delDmDbDao(dbId);
        return TResponseVo.success("删除成功");
    }

    @Override
    public TResponseVo delDmDbInFileService(String dbId, String fileUuid, String fileVersionId) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        workingTableMapper.delDmDbInFileDao(dbId, fileUuid, fileVersionId);
        return TResponseVo.success("删除成功");
    }

    /**
     * 修改数据管理工具
     *
     * @param hfDmDb
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo chgDmDbService(HfDmDb hfDmDb) throws Exception {
        String title = hfDmDb.getTitle();
        String key = hfDmDb.getKey();
        if (StringUtils.isBlank(title) || StringUtils.isBlank(key)) {
            throw new WorkTableException("必填参数为空");
        }
        String createUserId = hfDmDb.getCreateUserId();
//        Integer integer = workingTableMapper.checkDmDbNameDao(title, key, createUserId);
//        if (integer > 0) {
//            throw new WorkTableException("该数据库名称已存在");
//        }
        hfDmDb.setDbName(title);
        hfDmDb.setDbDesc(hfDmDb.getDesc());
        hfDmDb.setDbId(key);
        workingTableMapper.chgDmDbDao(hfDmDb);
        return TResponseVo.success("修改完成");
    }

    @Override
    public TResponseVo chgDmDbInFileService(HfDmDb hfDmDb) throws Exception {
        String title = hfDmDb.getTitle();
        String key = hfDmDb.getKey();
        if (StringUtils.isBlank(title) || StringUtils.isBlank(key)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询主文件
        String fileUuid = hfDmDb.getFileUuid();
        String fileVersionId = hfDmDb.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            hfDmDb.setFileUuid(fileUuid);
            hfDmDb.setFileVersionId(fileVersionId);
        }
//        Integer integer = workingTableMapper.checkDmDbNameInFileDao(title, key, fileUuid, fileVersionId);
//        if (integer > 0) {
//            throw new WorkTableException("该数据库名称已存在");
//        }
        hfDmDb.setDbName(title);
        hfDmDb.setDbDesc(hfDmDb.getDesc());
        hfDmDb.setDbId(key);
        workingTableMapper.chgDmDbInFileDao(hfDmDb);
        return TResponseVo.success("修改完成");
    }

    /**
     * 获取数据管理工具清单
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getDmDbListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();
        String userId = pagePojo.getUserId();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        Integer level = 3;
        if (pagePojo.getLevel() != null) {
            level = pagePojo.getLevel();
        }

        List<HfDmDb> dmDbList = new ArrayList<>();

        if (true || "0".equalsIgnoreCase(isPaged)) {
            // 获取数据库清单
            dmDbList = workingTableMapper.getDmDbListDao(userId, paramNameLike);

            // 获取数据表和视图清单
            List<HfDmTable> dmTableListDaoTmp = workingTableMapper.getDmTableListDao(userId);
            dmTableListDaoTmp.forEach(k -> {
                List<String> list = new ArrayList<>(Arrays.asList(k.getDbId(), k.getFatherTableId(), k.getTableId()));
                list.removeIf(StringUtils::isBlank);
                k.setPath(list);
            });


            List<HfDmTable> dmViewListDao = new ArrayList<>();  // 区分出 视图or表
            List<HfDmTable> dmTableListDao = new ArrayList<>();  // 区分出 视图or表
            for (HfDmTable hfDmTable : dmTableListDaoTmp) {
                if (StringUtils.isNotBlank(hfDmTable.getFatherTableId())) {
                    hfDmTable.setType("view");  // 手动将type设置为view
                    dmViewListDao.add(hfDmTable);  // 将视图挑选出来
                } else {
                    dmTableListDao.add(hfDmTable);  // 将表挑选出来
                }
            }

            if (level >= 3) {
                for (HfDmTable dmView : dmViewListDao) { // 将视图装载到表上
                    for (HfDmTable hfDmTable : dmTableListDao) {
                        if (dmView.getFatherTableId().equals(hfDmTable.getTableId())) {
                            dmView.setFatherKey(hfDmTable.getTableId());
                            // 匹配到
                            if (hfDmTable.getChildren() == null) {
                                ArrayList<HfDmTable> children = new ArrayList<>();
                                children.add(dmView);
                                hfDmTable.setChildren(children);
                            } else {
                                List<HfDmTable> children = hfDmTable.getChildren();
                                children.add(dmView);
                            }
                            break;
                        }
                    }
                }
            }

            if (level >= 2) {
                for (HfDmTable hfDmTable : dmTableListDao) {
                    // 表放到库下
                    for (HfDmDb hfDmDb : dmDbList) {
                        if (hfDmDb.getDbId().equals(hfDmTable.getDbId())) {
                            hfDmTable.setFatherKey(hfDmDb.getDbId());
                            hfDmTable.setDbName(hfDmDb.getDbName());
                            hfDmTable.setDbDesc(hfDmDb.getDbDesc());
                            List<HfDmTable> children = hfDmDb.getChildren();
                            if (children == null) {
                                ArrayList<HfDmTable> hfDmTables = new ArrayList<>();
                                hfDmTables.add(hfDmTable);
                                hfDmDb.setChildren(hfDmTables);
                            } else {
                                children.add(hfDmTable);
                            }
                            break;
                        }
                    }
                }
            }
            return TResponseVo.success(dmDbList);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            dmDbList = workingTableMapper.getDmDbListDao(userId, paramNameLike);
            PageInfo pageInfo = new PageInfo(dmDbList);
            return TResponseVo.success(pageInfo);
        }
    }

    @Override
    public TResponseVo getDmDbListInFileService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();
        String userId = pagePojo.getUserId();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        Integer level = 3;
        if (pagePojo.getLevel() != null) {
            level = pagePojo.getLevel();
        }

        List<HfDmDb> optionsListDao = new ArrayList<>();
        String fileUuid = pagePojo.getFileUuid();
        String fileVersionId = pagePojo.getFileVersionId();
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        if (true || "0".equalsIgnoreCase(isPaged)) {
            optionsListDao = workingTableMapper.getDmDbListInFileDao(userId, paramNameLike, fileUuid, fileVersionId);
            List<HfDmTable> dmTableListDaoTmp = workingTableMapper.getDmTableListInFileDao(userId, fileUuid, fileVersionId);
            dmTableListDaoTmp.forEach(k -> {
                List<String> list = new ArrayList<>(Arrays.asList(k.getDbId(), k.getFatherTableId(), k.getTableId()));
                list.removeIf(StringUtils::isBlank);
                k.setPath(list);
            });

            List<HfDmTable> dmViewListDao = new ArrayList<>();  // 区分出 视图or表
            List<HfDmTable> dmTableListDao = new ArrayList<>();  // 区分出 视图or表
            for (HfDmTable hfDmTable : dmTableListDaoTmp) {
                if (StringUtils.isNotBlank(hfDmTable.getFatherTableId())) {
                    hfDmTable.setType("view");  // 手动将type设置为view
                    dmViewListDao.add(hfDmTable);  // 将视图挑选出来
                } else {
                    dmTableListDao.add(hfDmTable);  // 将表挑选出来
                }
            }

            if (level >= 3) {
                for (HfDmTable dmView : dmViewListDao) { // 将视图装载到表上
                    for (HfDmTable hfDmTable : dmTableListDao) {
                        if (dmView.getFatherTableId().equals(hfDmTable.getTableId())) {
                            dmView.setFatherKey(hfDmTable.getTableId());
                            // 匹配到
                            if (hfDmTable.getChildren() == null) {
                                ArrayList<HfDmTable> children = new ArrayList<>();
                                children.add(dmView);
                                hfDmTable.setChildren(children);
                            } else {
                                List<HfDmTable> children = hfDmTable.getChildren();
                                children.add(dmView);
                            }
                            break;
                        }
                    }
                }
            }

            if (level >= 2) {
                for (HfDmTable hfDmTable : dmTableListDao) {
                    // 表放到库下
                    for (HfDmDb hfDmDb : optionsListDao) {
                        if (hfDmDb.getDbId().equals(hfDmTable.getDbId())) {
                            hfDmTable.setFatherKey(hfDmDb.getDbId());
                            hfDmTable.setDbName(hfDmDb.getDbName());
                            hfDmTable.setDbDesc(hfDmDb.getDbDesc());
                            List<HfDmTable> children = hfDmDb.getChildren();
                            if (children == null) {
                                ArrayList<HfDmTable> hfDmTables = new ArrayList<>();
                                hfDmTables.add(hfDmTable);
                                hfDmDb.setChildren(hfDmTables);
                            } else {
                                children.add(hfDmTable);
                            }
                            break;
                        }
                    }
                }
            }
            return TResponseVo.success(optionsListDao);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            optionsListDao = workingTableMapper.getDmDbListInFileDao(userId, paramNameLike, fileUuid, fileVersionId);
            PageInfo pageInfo = new PageInfo(optionsListDao);
            return TResponseVo.success(pageInfo);
        }
    }


    /**
     * 数据表排序
     *
     * @param dbId        数据库id
     * @param tableIdList 数据表顺序数组
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo orderDmTableService(String dbId, List<String> tableIdList) throws Exception {
        if (dbId == null || tableIdList == null) {
            throw new WorkTableException("必填参数为空");
        }
        if (tableIdList.contains(dbId)) {
            // 排数据库
            ArrayList<HfDmDb> orderList = new ArrayList<>();
            for (int i = 0; i < tableIdList.size(); i++) {
                HfDmDb hfDmDb = new HfDmDb();
                hfDmDb.setDbId(tableIdList.get(i));
                hfDmDb.setOrder(String.valueOf(i));
                orderList.add(hfDmDb);
            }
            workingTableMapper.orderDmDbDao(orderList);
            return TResponseVo.success("顺序更新完成");
        } else {
            // 排表和视图
            ArrayList<HfDmTable> orderList = new ArrayList<>();
            for (int i = 0; i < tableIdList.size(); i++) {
                HfDmTable hfDmTable = new HfDmTable();
                hfDmTable.setTableId(tableIdList.get(i));
                hfDmTable.setOrder(String.valueOf(i));
                orderList.add(hfDmTable);
            }
            workingTableMapper.orderDmTableDao(dbId, orderList);
            return TResponseVo.success("顺序更新完成");
        }
    }

    @Override
    public TResponseVo orderDmTableInFileService(String dbId, List<String> tableIdList, String fileUuid, String fileVersionId) throws Exception {
        if (dbId == null || tableIdList == null) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        if (tableIdList.contains(dbId)) {
            // 排数据库
            ArrayList<HfDmDb> orderList = new ArrayList<>();
            for (int i = 0; i < tableIdList.size(); i++) {
                HfDmDb hfDmDb = new HfDmDb();
                hfDmDb.setDbId(tableIdList.get(i));
                hfDmDb.setOrder(String.valueOf(i));
                orderList.add(hfDmDb);
            }
            workingTableMapper.orderDmDbInFileDao(orderList, fileUuid, fileVersionId);
            return TResponseVo.success("顺序更新完成");
        } else {
            ArrayList<HfDmTable> orderList = new ArrayList<>();
            for (int i = 0; i < tableIdList.size(); i++) {
                HfDmTable hfDmTable = new HfDmTable();
                hfDmTable.setTableId(tableIdList.get(i));
                hfDmTable.setOrder(String.valueOf(i));
                orderList.add(hfDmTable);
            }
            workingTableMapper.orderDmTableInFileDao(dbId, orderList, fileUuid, fileVersionId);
            return TResponseVo.success("顺序更新完成");
        }
    }

    /**
     * 获取数据管理工具详情
     *
     * @param dbId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getDmDbInfoService(String dbId) throws Exception {
        if (StringUtils.isBlank(dbId)) {
            throw new WorkTableException("必填参数为空");
        }
        HfDmDb dmDbInfoDao = workingTableMapper.getDmDbInfoDao(dbId);
        return TResponseVo.success(dmDbInfoDao);
    }

    @Override
    public TResponseVo getDmDbInfoInFileService(String dbId, String fileUuid, String fileVersionId) throws Exception {
        if (StringUtils.isBlank(dbId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        HfDmDb dmDbInfoDao = workingTableMapper.getDmDbInfoInFileDao(dbId, fileUuid, fileVersionId);
        return TResponseVo.success(dmDbInfoDao);
    }

    /**
     * 新增数据管理工具-数据表
     *
     * @param hfDmTable
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo createDmTableService(HfDmTable hfDmTable) throws Exception {
        String title = hfDmTable.getTitle();
        String dbId = hfDmTable.getDbId();
        if (StringUtils.isBlank(title) || StringUtils.isBlank(dbId)) {
            throw new WorkTableException("必填参数为空");
        }
        String createUserId = hfDmTable.getCreateUserId();
//        Integer integer = workingTableMapper.checkDmTableNameDao(title, null, dbId, createUserId);
//        if (integer > 0) {
//            throw new WorkTableException("该数据库名称已存在");
//        }
        hfDmTable.setTableName(title);
        hfDmTable.setTableDesc(hfDmTable.getDesc());
        hfDmTable.setTableId(UUID.randomUUID().toString().replaceAll("-", ""));
        workingTableMapper.createDmTableDao(hfDmTable);
        return TResponseVo.success("数据库创建成功");
    }

    @Override
    public TResponseVo createDmTableInFileService(HfDmTable hfDmTable) throws Exception {
        String title = hfDmTable.getTitle();
        String dbId = hfDmTable.getDbId();
        if (StringUtils.isBlank(title) || StringUtils.isBlank(dbId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询主文件
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            hfDmTable.setFileUuid(fileUuid);
            hfDmTable.setFileVersionId(fileVersionId);
        }
//        Integer integer = workingTableMapper.checkDmTableNameInFileDao(title, null, dbId, fileUuid, fileVersionId);
//        if (integer > 0) {
//            throw new WorkTableException("该数据库名称已存在");
//        }
        hfDmTable.setTableName(title);
        hfDmTable.setTableDesc(hfDmTable.getDesc());
        hfDmTable.setTableId(UUID.randomUUID().toString().replaceAll("-", ""));
        workingTableMapper.createDmTableInFileDao(hfDmTable);
        return TResponseVo.success("数据库创建成功");
    }

    /**
     * 删除数据管理工具-数据表
     *
     * @param dbId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delDmTableService(String dbId) throws Exception {
        workingTableMapper.delDmTableDao(dbId);
        return TResponseVo.success("删除完成");
    }

    @Override
    public TResponseVo delDmTableInFileService(String dbId, String fileUuid, String fileVersionId) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        workingTableMapper.delDmTableInFileDao(dbId, fileUuid, fileVersionId);
        return TResponseVo.success("删除完成");
    }

    /**
     * 修改数据管理工具-数据表
     *
     * @param hfDmTable
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo chgDmTableService(HfDmTable hfDmTable) throws Exception {
        String title = hfDmTable.getTitle();
        String key = hfDmTable.getKey();
        if (StringUtils.isBlank(title) || StringUtils.isBlank(key)) {
            throw new WorkTableException("必填参数为空");
        }
//        // 获取dbId
//        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoDao(key);
//        String dbId = dmTableInfoDao.getDbId();
//
//        String createUserId = hfDmTable.getCreateUserId();
//        Integer integer = workingTableMapper.checkDmTableNameDao(title, key, dbId, createUserId);
//        if (integer > 0) {
//            throw new WorkTableException("该数据库名称已存在");
//        }
        hfDmTable.setTableName(title);
        hfDmTable.setTableDesc(hfDmTable.getDesc());
        hfDmTable.setTableId(key);
        workingTableMapper.chgDmTableDao(hfDmTable);
        return TResponseVo.success("修改完成");
    }

    @Override
    public TResponseVo chgDmTableInFileService(HfDmTable hfDmTable) throws Exception {
        String title = hfDmTable.getTitle();
        String key = hfDmTable.getKey();
        if (StringUtils.isBlank(title) || StringUtils.isBlank(key)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询主文件
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            hfDmTable.setFileUuid(fileUuid);
            hfDmTable.setFileVersionId(fileVersionId);
        }
//        // 获取dbId
//        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoInFileDao(key, fileUuid, fileVersionId);
//        String dbId = dmTableInfoDao.getDbId();
//        Integer integer = workingTableMapper.checkDmTableNameInFileDao(title, key, dbId, fileUuid, fileVersionId);
//        if (integer > 0) {
//            throw new WorkTableException("该数据库名称已存在");
//        }
        hfDmTable.setTableName(title);
        hfDmTable.setTableDesc(hfDmTable.getDesc());
        hfDmTable.setTableId(key);
        workingTableMapper.chgDmTableInFileDao(hfDmTable);
        return TResponseVo.success("修改完成");
    }

    /**
     * 新增表头字段
     *
     * @param hfDmColumns
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo addDmTableColumnsService(HfDmColumns hfDmColumns) throws Exception {
        String key = hfDmColumns.getKey();
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("未指定数据表");
        }
        hfDmColumns.setTableId(key);
        String columnsId = UUID.randomUUID().toString().replaceAll("-", "");
        hfDmColumns.setKey(columnsId);
        // 转义option
        Object options = hfDmColumns.getOptions();
        if (options != null) {
            hfDmColumns.setOptions(JSON.toJSONString(options));
        }

        // 转义unit
        Object unit = hfDmColumns.getUnit();
        if (unit != null) {
            hfDmColumns.setUnit(JSON.toJSONString(unit));
        }

        // 如果kind是formula type转为文本
        String kind = hfDmColumns.getKind();
        if ("formula".equals(kind)) {
            hfDmColumns.setFieldType("40");
        }

        // 获取当前最大order
        Integer maxOrder = workingTableMapper.getNowColumnsOrderDao(key);
        if (maxOrder == null) {
            maxOrder = 0;
        } else {
            maxOrder = maxOrder + 1;
        }
        hfDmColumns.setOrder(String.valueOf(maxOrder));
        // 是否必填
        Boolean required = hfDmColumns.getRequired();
        if (required == null) {
            hfDmColumns.setRequired(false);
        }
        // 入库
        workingTableMapper.addDmTableColumnsDao(hfDmColumns);
        // 计算列的结果
        if ("formula".equals(kind)) {
            dmTableTask.calculateThread(key, Arrays.asList(columnsId), null, hfDmColumns.getCreateUserId(), true);
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("msg", "新增完成");
        hfDmColumns.setOptions(options);
        hfDmColumns.setVisible(true);
        hfDmColumns.setUnit(unit);
        map.put("column", hfDmColumns);
        return TResponseVo.success(map);
    }

    @Override
    public TResponseVo addDmTableColumnsInFileService(HfDmColumns hfDmColumns) throws Exception {
        String key = hfDmColumns.getKey();
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("未指定数据表");
        }
        // 查询主文件
        String fileUuid = hfDmColumns.getFileUuid();
        String fileVersionId = hfDmColumns.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            hfDmColumns.setFileUuid(fileUuid);
            hfDmColumns.setFileVersionId(fileVersionId);
        }

        hfDmColumns.setTableId(key);
        String columnsId = UUID.randomUUID().toString().replaceAll("-", "");
        hfDmColumns.setKey(columnsId);
        // 转义option
        Object options = hfDmColumns.getOptions();
        if (options != null) {
            hfDmColumns.setOptions(JSON.toJSONString(options));
        }

        // 转义unit
        Object unit = hfDmColumns.getUnit();
        if (unit != null) {
            hfDmColumns.setUnit(JSON.toJSONString(unit));
        }

        // 如果kind是formula type转为文本
        String kind = hfDmColumns.getKind();
        if ("formula".equals(kind)) {
            hfDmColumns.setFieldType("40");
        }

        // 获取当前最大order
        Integer maxOrder = workingTableMapper.getNowColumnsOrderInFileDao(key, fileUuid, fileVersionId);
        if (maxOrder == null) {
            maxOrder = 0;
        } else {
            maxOrder = maxOrder + 1;
        }
        hfDmColumns.setOrder(String.valueOf(maxOrder));
        // 是否必填
        Boolean required = hfDmColumns.getRequired();
        if (required == null) {
            hfDmColumns.setRequired(false);
        }
        // 入库
        workingTableMapper.addDmTableColumnsInFileDao(hfDmColumns);
        if ("formula".equals(kind)) {
            dmTableTask.calculateInFileThread(key, Arrays.asList(columnsId), null, hfDmColumns.getCreateUserId(), fileUuid, fileVersionId, true);
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("msg", "新增完成");
        hfDmColumns.setOptions(options);
        hfDmColumns.setVisible(true);
        hfDmColumns.setUnit(unit);
        map.put("column", hfDmColumns);
        return TResponseVo.success(map);
    }

    /**
     * 删除表头字段
     *
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delDmTableColumnsService(Map map) throws Exception {
        Object key = map.get("key");
        Object tableId = map.get("tableId");
        if (key == null || tableId == null) {
            throw new WorkTableException("必填参数为空");
        }
        if (key instanceof String) {
            workingTableMapper.delDmTableColumnsDao(String.valueOf(key), String.valueOf(tableId));
        } else if (key instanceof List) {
            List<String> keyList = (List) key;
            workingTableMapper.delDmTableColumns2Dao(keyList, String.valueOf(tableId));
        }
        return TResponseVo.success("删除完成");
    }

    @Override
    public TResponseVo delDmTableColumnsInFileService(Map map, String fileUuid, String fileVersionId) throws Exception {
        Object key = map.get("key");
        Object tableId = map.get("tableId");
        if (key == null || tableId == null) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        if (key instanceof String) {
            workingTableMapper.delDmTableColumnsInFileDao(String.valueOf(key), String.valueOf(tableId), fileUuid, fileVersionId);
        } else if (key instanceof List) {
            List<String> keyList = (List) key;
            workingTableMapper.delDmTableColumns2InFileDao(keyList, String.valueOf(tableId), fileUuid, fileVersionId);
        }
        return TResponseVo.success("删除完成");
    }

    /**
     * 修改表头字段
     *
     * @param hfDmColumns
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo chgDmTableColumnsService(HfDmColumns hfDmColumns) throws Exception {
        String key = hfDmColumns.getKey();
        String tableId = hfDmColumns.getTableId();
        String userId = hfDmColumns.getCreateUserId();
        if (StringUtils.isBlank(key) || StringUtils.isBlank(tableId)) {
            throw new WorkTableException("未指定数据表字段");
        }

        // 转义option
        Object options = hfDmColumns.getOptions();
        if (options != null) {
            hfDmColumns.setOptions(JSON.toJSONString(options));
        }
        // 转义unit
        Object unit = hfDmColumns.getUnit();
        if (unit != null) {
            hfDmColumns.setUnit(JSON.toJSONString(unit));
        }
        // 匹配-重置
        String matchType = hfDmColumns.getMatchType();
        if (StringUtils.isBlank(matchType)) {
            hfDmColumns.setMatchType(null);
            hfDmColumns.setMatchDbId(null);
            hfDmColumns.setMatchFolderId(null);
        }
        workingTableMapper.chgDmTableColumnsDao(hfDmColumns);
        HfDmColumns dmTableColumnsInfoDao = workingTableMapper.getDmTableColumnsInfoDao(tableId, key);
        if ("formula".equals(dmTableColumnsInfoDao.getKind())) {
            dmTableTask.calculateThread(tableId, Arrays.asList(key), null, userId, true);
        }
        if (StringUtils.isNotBlank(dmTableColumnsInfoDao.getMatchType())) {
            dmTableTask.columnsMatchThread(tableId, Arrays.asList(key), null, userId, null, null);
        }

        return TResponseVo.success("修改完成");
    }

    @Override
    public TResponseVo chgDmTableColumnsInFileService(HfDmColumns hfDmColumns) throws Exception {
        String key = hfDmColumns.getKey();
        String tableId = hfDmColumns.getTableId();
        String userId = hfDmColumns.getCreateUserId();
        if (StringUtils.isBlank(key) || StringUtils.isBlank(tableId)) {
            throw new WorkTableException("未指定数据表字段");
        }
        // 查询主文件
        String fileUuid = hfDmColumns.getFileUuid();
        String fileVersionId = hfDmColumns.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            hfDmColumns.setFileUuid(fileUuid);
            hfDmColumns.setFileVersionId(fileVersionId);
        }

        // 转义option
        Object options = hfDmColumns.getOptions();
        if (options != null) {
            hfDmColumns.setOptions(JSON.toJSONString(options));
        }
        // 转义unit
        Object unit = hfDmColumns.getUnit();
        if (unit != null) {
            hfDmColumns.setUnit(JSON.toJSONString(unit));
        }
        // 匹配-重置
        String matchType = hfDmColumns.getMatchType();
        if (StringUtils.isBlank(matchType)) {
            hfDmColumns.setMatchType(null);
            hfDmColumns.setMatchDbId(null);
            hfDmColumns.setMatchFolderId(null);
        }
        workingTableMapper.chgDmTableColumnsInFileDao(hfDmColumns);
        HfDmColumns dmTableColumnsInfoDao = workingTableMapper.getDmTableColumnsInfoInFileDao(tableId, key, fileUuid, fileVersionId);
        if ("formula".equals(dmTableColumnsInfoDao.getKind())) {
            dmTableTask.calculateInFileThread(tableId, Arrays.asList(key), null, userId, fileUuid, fileVersionId, true);
        }
        if (StringUtils.isNotBlank(dmTableColumnsInfoDao.getMatchType())) {
            dmTableTask.columnsMatchThread(tableId, Arrays.asList(key), null, userId, fileUuid, fileVersionId);
        }
        return TResponseVo.success("修改完成");
    }

    /**
     * 查询表头字段
     *
     * @param hfDmColumns
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getDmTableColumnsListService(HfDmColumns hfDmColumns) throws Exception {
        String key = hfDmColumns.getKey();
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("未指定表");
        }
        List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListDao(key);
        for (HfDmColumns dmColumns : dmTableColumnsListDao) {
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
        return TResponseVo.success(dmTableColumnsListDao);
    }

    @Override
    public TResponseVo getDmTableColumnsListInFileService(HfDmColumns hfDmColumns) throws Exception {
        String key = hfDmColumns.getKey();
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("未指定表");
        }
        // 查询主文件
        String fileUuid = hfDmColumns.getFileUuid();
        String fileVersionId = hfDmColumns.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListInFileDao(key, fileUuid, fileVersionId);
        for (HfDmColumns dmColumns : dmTableColumnsListDao) {
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
        return TResponseVo.success(dmTableColumnsListDao);
    }

    /**
     * 获取数据表内容
     *
     * @param hfDmTable
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getDmTableContentService(HfDmTable hfDmTable) throws Exception {
        // 分页信息
        String isPaged = hfDmTable.getIsPaged();
        Integer pageNum = hfDmTable.getPageNum();
        Integer pageSize = hfDmTable.getPageSize();

        // 判断必填条件
        String key = hfDmTable.getKey();
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("未指定数据表");
        }

        // 当前用户
        String userId = hfDmTable.getCreateUserId();

        // 表信息
        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoDao(key);
        if (dmTableInfoDao == null) {
            throw new WorkTableException("该表或视图不存在或已删除,请刷新后重试");
        }
        String fatherTableId = dmTableInfoDao.getFatherTableId();
        String fatherFilter = dmTableInfoDao.getFatherFilter();
        String fatherGroup = dmTableInfoDao.getFatherGroup();
        Object fatherFilterObj = null;
        Object fatherGroupObj = null;
        if (StringUtils.isNotBlank(fatherFilter)) {
            fatherFilterObj = JSON.parseObject(fatherFilter, List.class);
        }
        if (StringUtils.isNotBlank(fatherGroup)) {
            fatherGroupObj = JSON.parseObject(fatherGroup, Map.class);
        }
        //原表表头信息
        HfDmTable fatherTableInfo = workingTableMapper.getDmTableInfoDao(fatherTableId);
        if (fatherTableInfo == null) {
            fatherTableInfo = new HfDmTable();
        }
        List<HfDmColumns> fatherTableColumnsListDao = workingTableMapper.getDmTableColumnsListDao(fatherTableId);
        for (HfDmColumns dmColumns : fatherTableColumnsListDao) {
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


        // 表头信息
        List<String> dmTableColumnsKeyList = new ArrayList<>();
        List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListDao(key);
        // 公式
        List<String> columnsIdList = dmTableColumnsListDao.stream().filter(k -> "formula".equals(k.getKind())).map(HfDmColumns::getKey).collect(Collectors.toList());
        // 匹配
        // matchMap用于记录预读取 需要用到的参与匹配的数据 {matchId:[LabelValuePojo]}
        // matchDbId 和 matchFolderId 统称 matchId
        HashMap<String, List<LabelValuePojo>> matchMap = new HashMap<>();
        ArrayList<String> matchIdList = new ArrayList<>();
        HashMap<String, String> key4DataIndexMap = new HashMap<>();
        List<HfDmColumns> columnsIdMatchList = new ArrayList<>();  // 记录需要匹配的列

        List<String> columnsIdAllList = dmTableColumnsListDao.stream().map(HfDmColumns::getKey).collect(Collectors.toList());
        for (HfDmColumns dmColumns : dmTableColumnsListDao) {
            dmTableColumnsKeyList.add(dmColumns.getDataIndex());
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

            // 计算
            if (columnsIdList.contains(dmColumns.getKey())) {  // 公式字段
                // 剥离出[key]
                ArrayList<String> keyList = new ArrayList<>();
                String regex = "\\[(.*?)\\]";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(dmColumns.getFormula());
                while (matcher.find()) {
                    String result = matcher.group(1);
                    if (!keyList.contains(result)) {
                        keyList.add(result);
                    }
                }
                // 判断key是否都还存在
                for (String formulaKey : keyList) {
                    if (!columnsIdAllList.contains(formulaKey)) {
                        dmColumns.setExecute("failed:公式中列不存在");
                        break;
                    }
                }
            }

            // 预先匹配要用到的数据库和模板文件夹 记录在matchMap中
            String matchType = dmColumns.getMatchType();
            if ("table".equals(matchType)) {  // 数据表
                columnsIdMatchList.add(dmColumns);
                String matchDbId = dmColumns.getMatchDbId();
                if (!matchIdList.contains(matchDbId)) {
                    List<LabelValuePojo> dmTableListLVByDbIdDao = workingTableMapper.getDmTableListLVByDbIdDao(matchDbId);
                    matchMap.put(matchDbId, dmTableListLVByDbIdDao);
                    matchIdList.add(matchDbId);
                }
            } else if ("file".equals(matchType)) {  // 文件
                columnsIdMatchList.add(dmColumns);
                String matchFolderId = dmColumns.getMatchFolderId();
                if (!matchIdList.contains(matchFolderId)) {
                    List<LabelValuePojo> dmTableListLVByDbIdDao = editToolMapper.getDocumentListLVDao(matchFolderId);
                    // 上传模板
                    List<LabelValuePojo> uploadModelFileListLVDao = editToolMapper.getUploadModelFileListLVDao(matchFolderId);
                    dmTableListLVByDbIdDao.addAll(uploadModelFileListLVDao);
                    matchMap.put(matchFolderId, dmTableListLVByDbIdDao);
                    matchIdList.add(matchFolderId);
                }
            }
            key4DataIndexMap.put(dmColumns.getKey(), dmColumns.getDataIndex());
        }

//        10  单选
//        20  多选
//        30  日期/时间
//        40  文本
//        50  富文本
//        60  图片
//        70  资源附件
//        80  金额
//        90  数值
        // 数据信息
        // 1.复杂筛选
        Object filterObj = hfDmTable.getFilter();
        String sql = null;  // 拼装查询语句
        if (filterObj != null && filterObj instanceof List) {
            List<Map<String, Object>> filterList = (List) hfDmTable.getFilter();
            List<HfDmTable> filter = new ArrayList<>();
            for (Map<String, Object> map : filterList) {
                filter.add(JSON.parseObject(JSON.toJSONString(map), HfDmTable.class));
            }

            // 确定查询条件是与还是或
            String qType = "and"; // 查询类型默认and
            for (HfDmTable dmTable : filter) {
                if (dmTable.getType() != null) {
                    qType = dmTable.getType();  // and or
                    break;
                }
            }
            sql = "";
            for (HfDmTable f : filter) {
                if (f.getField() != null) { // 防止[{}]
                    // 判断筛选条件的字段类型
                    for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                        if (f.getField().equals(hfDmColumns.getDataIndex())) {
                            String fieldType = hfDmColumns.getFieldType();  // 获取到类型
                            // 根据类型组装查询语句
                            if ("10".equals(fieldType)) {  // 单选
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not".equals(f.getCondition())) {  // 不是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '')", qType, f.getField(), f.getField());
                                }
                            } else if ("20".equals(fieldType)) {  // 多选
                                // condition条件类型
                                if ("in".equals(f.getCondition())) {  // 包含
                                    sql = sql + String.format("%s (json_contains(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')), '%s'))", qType, f.getField(), JSON.toJSONString(f.getValue()).replaceAll("\\\"", "\\\\\"").replaceAll("\\\'", "\\\\\'"));
                                } else if ("not in".equals(f.getCondition())) {  // 不包含
                                    sql = sql + String.format("%s (!json_contains(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')), '%s'))", qType, f.getField(), JSON.toJSONString(f.getValue()).replaceAll("\\\"", "\\\\\"").replaceAll("\\\'", "\\\\\'"));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '[]')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '[]')", qType, f.getField(), f.getField());
                                }
                            } else if ("30".equals(fieldType)) {  // 时间
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not".equals(f.getCondition())) {  // 不等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gt".equals(f.getCondition())) {  // 大于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) > \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gte".equals(f.getCondition())) {  // 大于等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) >= \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lt".equals(f.getCondition())) {  // 小于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) < \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lte".equals(f.getCondition())) {  // 小于等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) <= \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '')", qType, f.getField(), f.getField());
                                }
                            } else if ("80".equals(fieldType) || "90".equals(fieldType)) {  // 金额 数值
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 等于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) = %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not".equals(f.getCondition())) {  // 不等于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) != %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gt".equals(f.getCondition())) {  // 大于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) > %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gte".equals(f.getCondition())) {  // 大于等于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) >= %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lt".equals(f.getCondition())) {  // 小于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) < %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lte".equals(f.getCondition())) {  // 小于等于
//                                sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) <= \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"","\\\\\""));
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) <= %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '{}')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '{}')", qType, f.getField(), f.getField());
                                }
                            } else if ("40".equals(fieldType) || "50".equals(fieldType)) {  // 文本 富文本
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("like".equals(f.getCondition())) {  // 包含
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) like \"%%%s%%\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not like".equals(f.getCondition())) {  // 不包含
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) not like \"%%%s%%\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("start with".equals(f.getCondition())) {  // 开头是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) like \"%s%%\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("end with".equals(f.getCondition())) {  // 结尾是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) like \"%%%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '')", qType, f.getField(), f.getField());
                                }
                            } else {
                                // condition条件类型
                                if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '' or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '[]')", qType, f.getField(), f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '' and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '[]')", qType, f.getField(), f.getField(), f.getField());
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if (sql != null && sql.length() > 3) {
                sql = sql.substring(3);
            } else {
                sql = null;
            }
        }

        // 排序
        Object sortObj = hfDmTable.getSort();
        String sortBySql = null;
        if (sortObj != null && sortObj instanceof List) {
            List<Map<String, String>> sort = (List) hfDmTable.getSort();
            sortBySql = " order by ";
            for (Map<String, String> map : sort) {
                String type = map.get("type");
                // 判断排序条件的字段类型
                for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                    if (hfDmColumns.getDataIndex().equals(map.get("field"))) {
                        String fieldType = hfDmColumns.getFieldType();  // 获取到类型
                        // 根据类型组装查询语句
                        if ("10".equals(fieldType)) {  // 单选
                            sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                        } else if ("20".equals(fieldType)) {  // 多选
                            sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                        } else if ("30".equals(fieldType)) {  // 时间
                            sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                        } else if ("80".equals(fieldType) || "90".equals(fieldType)) {  // 金额 数值
                            sortBySql = sortBySql + String.format("CAST(SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) as DECIMAL(13,3)) %s,", map.get("field"), map.get("field"), map.get("sort"));
                        } else if ("40".equals(fieldType)) {  // 文本
                            if ("number".equals(type)) {  // number
                                sortBySql = sortBySql + String.format("CAST(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) as DECIMAL(13,3)) %s,", map.get("field"), map.get("sort"));
                            } else {  // string
                                sortBySql = sortBySql + String.format("CONVERT(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) USING gbk) %s,", map.get("field"), map.get("sort"));
                            }
                        } else {
                            // 其余类型不排序
//                            sortBySql = sortBySql + "";
                        }
                        break;
                    }
                }
            }
            if (!" order by ".equals(sortBySql)) {
                sortBySql = sortBySql.substring(0, sortBySql.length() - 1);
            } else {
                sortBySql = null;
            }
        }

        // 获取上卷聚合字段
        Map<String, Object> group = hfDmTable.getGroup();
        if (group != null && group.keySet().size() != 0) {
            // 记录上卷加聚合用到的字段的dataIndex 形成统计表的表头
            List<Map<String, Object>> groupTableDataIndexList = new ArrayList<>();
            int order = 0;
            // 上卷
            Object groupByObj = group.get("groupBy");
            String groupBySql = null;
            String groupBySqlWithName = null;
            String groupBySqlNotNull = null;
            List<String> groupBy = new ArrayList<>();
            if (groupByObj != null && groupByObj instanceof List) { // 不为空
                groupBy = (List) group.get("groupBy");
                if (groupBy.size() != 0) { // 并且长度不为0
                    groupBySql = "";
                    groupBySqlWithName = "";
                    groupBySqlNotNull = "";
                    for (String s : groupBy) {
                        // 记录上卷字段s是否在当前表中存在 不存在就抛出异常
                        Boolean sExist = false;
                        for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                            if (s.equals(hfDmColumns.getDataIndex())) {
                                sExist = true;
                                // 对照已有字段 获取字段名
                                HashMap<String, Object> map = new HashMap<>();
                                map.put("key", s);
                                map.put("dataIndex", s);
                                map.put("title", hfDmColumns.getTitle());
//                                map.put("fieldType", "40");
                                map.put("fieldType", hfDmColumns.getFieldType());
                                map.put("options", hfDmColumns.getOptions());
                                map.put("unit", hfDmColumns.getUnit());
                                map.put("visible", true);
                                map.put("required", false);
                                map.put("order", String.valueOf(order++));
                                groupTableDataIndexList.add(map);
                                groupBySql = groupBySql + String.format(" JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) ,", s);
                                groupBySqlWithName = groupBySqlWithName + String.format(" JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) `%s`,", s, s);
                                groupBySqlNotNull = groupBySqlNotNull + String.format(" JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and,", s);
                                break;
                            }
                        }
                        if (!sExist) {
                            throw new WorkTableException("上卷字段在当前表中不存在,请检查是否已被删除");
                        }
                    }
                    // 去除末位多余逗号
                    groupBySql = groupBySql.substring(0, groupBySql.length() - 1);  // 去除末位逗号
                    groupBySqlWithName = groupBySqlWithName.substring(0, groupBySqlWithName.length() - 1);  // 去除末位逗号
                    groupBySqlNotNull = groupBySqlNotNull.substring(0, groupBySqlNotNull.length() - 4); // 去除末位and
                }
            }
            // 聚合
            Object groupByFunctionsObj = group.get("groupByFunctions");
            String groupByFunctionsSql = null;
            List<Map<String, Object>> groupByFunctions = new ArrayList<>();
            if (groupByFunctionsObj != null && groupByFunctionsObj instanceof List) {
                groupByFunctions = (List) group.get("groupByFunctions");
                groupByFunctionsSql = "";
                for (Map<String, Object> map : groupByFunctions) { // { field: string, function: sum | count | max | avg | groupConcat, distinct: boolean, rename: string }
                    String field = (String) map.get("field");  // 聚合字段
                    String function = (String) map.get("function");  // 聚合方法 sum | count | max | avg | groupConcat
                    String rename = null;
                    if (map.get("rename") != null) {
                        rename = (String) map.get("rename");  // 聚合结果命名
                    } else { // 如果未提供 则用原名
                        for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                            if (field.equals(hfDmColumns.getDataIndex())) {
                                rename = hfDmColumns.getTitle();
                                break;
                            }
                        }
                    }
                    String fieldType = null;  // 字段类型  这里只判断80 90 数值 金额
                    String unitId = null;  // 单位的unitId
                    for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                        if (field.equals(hfDmColumns.getDataIndex())) {
                            fieldType = hfDmColumns.getFieldType();
                            try {
                                List<String> unit = (List) hfDmColumns.getUnit();
                                unitId = unit.get(1);
                            } catch (Exception e) {

                            }
                            break;
                        }
                    }
                    // 特别的 如果field的值为system-count那么计算行统计
                    if ("system-count".equals(field)) {
                        function = "count";
                        rename = "行数统计";
                    }

                    // 对照已有字段 获取字段名
                    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                    HashMap<String, Object> m = new HashMap<>();
                    m.put("key", uuid);
                    m.put("dataIndex", uuid);
                    m.put("title", rename);
                    m.put("fieldType", "40");
                    m.put("visible", true);
                    m.put("required", false);
                    m.put("order", String.valueOf(order++));
                    groupTableDataIndexList.add(m);

                    if ("sum".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", sum(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"'))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", sum(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("count".equals(function)) {
                        if ("system-count".equals(field)) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", count(1) `%s`", uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", count(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("max".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", max(cast(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"')) as DECIMAL(13,3))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", max(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("min".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", min(cast(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"')) as DECIMAL(13,3))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", min(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("avg".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", avg(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"'))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", avg(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("groupConcat".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", group_concat(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"'))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", group_concat(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("groupConcatDistinct".equals(function)) {  // 去重组合
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", group_concat(distinct JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"'))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", group_concat(distinct JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("countDistinct".equals(function)) {  // 去重计数
                        groupByFunctionsSql = groupByFunctionsSql + String.format(", count(distinct JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                    } else {
                        // 未配置的新聚合方法
                    }
                }
            }

            List<Map<String, Object>> optionsListGroupByDao = new ArrayList<>();
            long start1 = System.currentTimeMillis();  // 上传完成 开始解析
            if ("1".equals(isPaged)) {
                // 需要分页
                PageHelper.startPage(pageNum, pageSize);
                optionsListGroupByDao = workingTableMapper.getDmDataListGroupByDao(userId, key, null, sql, sortBySql, groupBySql, groupBySqlWithName, groupBySqlNotNull, groupByFunctionsSql, null);
            } else {
                // 不需要分页
                optionsListGroupByDao = workingTableMapper.getDmDataListGroupByDao(userId, key, null, sql, sortBySql, groupBySql, groupBySqlWithName, groupBySqlNotNull, groupByFunctionsSql, null);
            }
            // 过滤掉null
//            optionsListGroupByDao = optionsListGroupByDao.stream().filter(Objects::nonNull).collect(Collectors.toList());
            long start2 = System.currentTimeMillis();  // 上传完成 开始解析
            System.out.println("本次聚合运算耗时：" + (start2 - start1) + "ms");

            HashMap<String, String> typeMap = new HashMap<String, String>();  // 快捷记录类型
            for (Map<String, Object> map : optionsListGroupByDao) {
                // 判断optionsListGroupByDao中的数据类型 如果是数值就转为map 如果是选项就转list
                for (String s : map.keySet()) {
                    if (typeMap.keySet().contains(s)) {
                        if ("20".equals(typeMap.get(s))) {  // 多选
                            map.put(s, JSON.parseObject(String.valueOf(map.get(s)), List.class));
                        } else if ("80".equals(typeMap.get(s)) || "90".equals(typeMap.get(s))) { // 数值 金额
                            map.put(s, JSON.parseObject(String.valueOf(map.get(s)), Map.class));
                        } else if ("60".equals(typeMap.get(s)) || "70".equals(typeMap.get(s))) { // 图片 附件
                            map.put(s, JSON.parseObject(String.valueOf(map.get(s)), List.class));
                        } else {
                            // 不用动
                        }
                    } else {
                        for (Map<String, Object> stringObjectMap : groupTableDataIndexList) { // 对比表头 获取信息
                            if (s.equals((String) stringObjectMap.get("dataIndex"))) {
                                typeMap.put(s, (String) stringObjectMap.get("fieldType"));
                                if ("20".equals(typeMap.get(s))) {  // 多选
                                    map.put(s, JSON.parseObject(String.valueOf(map.get(s)), List.class));
                                } else if ("80".equals(typeMap.get(s)) || "90".equals(typeMap.get(s))) { // 数值 金额
                                    map.put(s, JSON.parseObject(String.valueOf(map.get(s)), Map.class));
                                } else if ("60".equals(typeMap.get(s)) || "70".equals(typeMap.get(s))) { // 图片 附件
                                    map.put(s, JSON.parseObject(String.valueOf(map.get(s)), List.class));
                                } else {
                                    // 不用动
                                }
                            }
                        }
                    }
                }
                //  增加key
                map.put("key", UUID.randomUUID().toString().replaceAll("-", ""));
            }

            // 返回结果
            if ("1".equals(isPaged)) {
                // 需要分页
                PageInfo pageInfo = new PageInfo(optionsListGroupByDao);
                // Map转实体类
                Map<String, Object> m = JSON.parseObject(JSON.toJSONString(pageInfo), Map.class);
                m.put("columns", groupTableDataIndexList);
                m.put("allColumns", dmTableColumnsListDao);
                HashMap<String, Object> condition = new HashMap<>();
                condition.put("filter", fatherFilterObj);
                condition.put("group", fatherGroupObj);
                condition.put("tableName", fatherTableInfo.getTableName());
                condition.put("tableId", fatherTableInfo.getTableId());
                condition.put("allColumns", fatherTableColumnsListDao);
                m.put("condition", condition);
                return TResponseVo.success(m);
            } else {
                // 不需要分页
                HashMap<String, Object> retMap = new HashMap<>();
                retMap.put("list", optionsListGroupByDao);
                retMap.put("columns", groupTableDataIndexList);
                retMap.put("allColumns", dmTableColumnsListDao);
                HashMap<String, Object> condition = new HashMap<>();
                condition.put("filter", fatherFilterObj);
                condition.put("group", fatherGroupObj);
                condition.put("tableName", fatherTableInfo.getTableName());
                condition.put("tableId", fatherTableInfo.getTableId());
                condition.put("allColumns", fatherTableColumnsListDao);
                retMap.put("condition", condition);
                return TResponseVo.success(retMap);
            }
        } else {
            List<HfDmData> optionsListDao = new ArrayList<>();
            if ("1".equals(isPaged)) {
                // 需要分页
                PageHelper.startPage(pageNum, pageSize);
                optionsListDao = workingTableMapper.getDmDataListDao(userId, key, null, sql, sortBySql, null);
            } else {
                // 不需要分页
                optionsListDao = workingTableMapper.getDmDataListDao(userId, key, null, sql, sortBySql, null);
            }

            // 2.查库
            ArrayList<Map<String, Object>> list = new ArrayList<>();  // 记录返回内容


            for (HfDmData hfDmData : optionsListDao) {
                // 反序列化
                Object dataContent = hfDmData.getDataContent();
                if (dataContent == null || StringUtils.isBlank(String.valueOf(dataContent))) {
                    System.out.println(hfDmData.getDataId() + "数据格式有误");
                } else {
                    Map<String, Object> dataContentMap = JSON.parseObject(String.valueOf(dataContent), Map.class);

                    // 即时匹配
                    hfDmData.setMatchResult(null);
                    List<Map<String, String>> matchResultList = new ArrayList<>();
                    for (HfDmColumns columnsInfoDao : columnsIdMatchList) { // 需要匹配的列
                        if ("40".equals(columnsInfoDao.getFieldType()) && columnsInfoDao.getMatchType() != null) {  // 只匹配文本类型字段
                            if ("table".equals(columnsInfoDao.getMatchType())) {
                                String dataIndex = key4DataIndexMap.get(columnsInfoDao.getKey());
                                Object o = dataContentMap.get(dataIndex);
                                if (o != null) {
                                    String leftKey = String.valueOf(o);  // 行数据中用于匹配的值
                                    List<LabelValuePojo> labelValuePojos = matchMap.get(columnsInfoDao.getMatchDbId());
                                    List<String> listResult = labelValuePojos.stream().filter(k -> leftKey.equals(k.getLabel())).map(LabelValuePojo::getValue).collect(Collectors.toList());
                                    if (!listResult.isEmpty()) {
                                        // 放入新数据
                                        HashMap<String, String> stringStringHashMap = new HashMap<>();
                                        stringStringHashMap.put("tableId", listResult.get(0));
                                        stringStringHashMap.put("dataIndex", dataIndex);
                                        matchResultList.add(stringStringHashMap);
                                    }
                                }
                            } else if ("file".equals(columnsInfoDao.getMatchType())) {
                                String dataIndex = key4DataIndexMap.get(columnsInfoDao.getKey());
                                Object o = dataContentMap.get(dataIndex);
                                if (o != null) {
                                    String leftKey = String.valueOf(o);  // 行数据中用于匹配的值
                                    List<LabelValuePojo> labelValuePojos = matchMap.get(columnsInfoDao.getMatchFolderId());
                                    List<String> listResult = labelValuePojos.stream().filter(k -> leftKey.equals(k.getLabel())).map(LabelValuePojo::getValue).collect(Collectors.toList());
                                    if (!listResult.isEmpty()) {
                                        // 放入新数据
                                        HashMap<String, String> stringStringHashMap = new HashMap<>();
                                        stringStringHashMap.put("fileUuid", listResult.get(0));
                                        stringStringHashMap.put("dataIndex", dataIndex);
                                        matchResultList.add(stringStringHashMap);
                                    }
                                }
                            }

                        } else {
                            System.out.println("非匹配列");
                        }
                    }

                    if (matchResultList == null || matchResultList.isEmpty()) {
                        hfDmData.setMatchResult(null);
                    } else {
                        hfDmData.setMatchResult(matchResultList);
                    }

                    // 如果当前表头不包含 key就不返回
                    dataContentMap.keySet().removeIf(k -> !dmTableColumnsKeyList.contains(k));
                    dataContentMap.put("key", hfDmData.getDataId());
                    dataContentMap.put("matchResult", hfDmData.getMatchResult());
                    list.add(dataContentMap);

                }
            }

            // 返回结果
            List<Map<String, Object>> groupTableDataIndexList = new ArrayList<>(); // dmTableColumnsListDao转成listmap返回
            groupTableDataIndexList = JSON.parseObject(JSON.toJSONString(dmTableColumnsListDao), List.class);
            if ("1".equals(isPaged)) {
                // 需要分页
                PageInfo pageInfo = new PageInfo(optionsListDao);
                pageInfo.setList(list);
                // Map转实体类
                Map<String, Object> m = JSON.parseObject(JSON.toJSONString(pageInfo), Map.class);
                m.put("columns", groupTableDataIndexList);
                m.put("allColumns", dmTableColumnsListDao);
                HashMap<String, Object> condition = new HashMap<>();
                condition.put("filter", fatherFilterObj);
                condition.put("group", fatherGroupObj);
                condition.put("tableName", fatherTableInfo.getTableName());
                condition.put("tableId", fatherTableInfo.getTableId());
                condition.put("allColumns", fatherTableColumnsListDao);
                m.put("condition", condition);
                return TResponseVo.success(m);
            } else {
                // 不需要分页
                HashMap<String, Object> retMap = new HashMap<>();
                retMap.put("list", list);
                retMap.put("columns", groupTableDataIndexList);
                retMap.put("allColumns", dmTableColumnsListDao);
                HashMap<String, Object> condition = new HashMap<>();
                condition.put("filter", fatherFilterObj);
                condition.put("group", fatherGroupObj);
                condition.put("tableName", fatherTableInfo.getTableName());
                condition.put("tableId", fatherTableInfo.getTableId());
                condition.put("allColumns", fatherTableColumnsListDao);
                retMap.put("condition", condition);
                return TResponseVo.success(retMap);
            }
        }
    }

    @Override
    public TResponseVo getDmTableContentInFileService(HfDmTable hfDmTable) throws Exception {
        // 分页信息
        String isPaged = hfDmTable.getIsPaged();
        Integer pageNum = hfDmTable.getPageNum();
        Integer pageSize = hfDmTable.getPageSize();

        // 判断必填条件
        String key = hfDmTable.getKey();
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("未指定数据表");
        }
        // 查询主文件
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        // 当前用户
        String userId = hfDmTable.getCreateUserId();

        // 表信息
        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoInFileDao(key, fileUuid, fileVersionId);
        if (dmTableInfoDao == null) {
            throw new WorkTableException("该表或视图不存在或已删除,请刷新后重试");
        }
        String fatherTableId = dmTableInfoDao.getFatherTableId();
        String fatherFilter = dmTableInfoDao.getFatherFilter();
        String fatherGroup = dmTableInfoDao.getFatherGroup();
        Object fatherFilterObj = null;
        Object fatherGroupObj = null;
        if (StringUtils.isNotBlank(fatherFilter)) {
            fatherFilterObj = JSON.parseObject(fatherFilter, List.class);
        }
        if (StringUtils.isNotBlank(fatherGroup)) {
            fatherGroupObj = JSON.parseObject(fatherGroup, Map.class);
        }
        //原表表头信息
        HfDmTable fatherTableInfo = workingTableMapper.getDmTableInfoInFileDao(fatherTableId, fileUuid, fileVersionId);
        if (fatherTableInfo == null) {
            fatherTableInfo = new HfDmTable();
        }
        List<HfDmColumns> fatherTableColumnsListDao = workingTableMapper.getDmTableColumnsListInFileDao(fatherTableId, fileUuid, fileVersionId);
        for (HfDmColumns dmColumns : fatherTableColumnsListDao) {
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

        // 表头信息
        List<String> dmTableColumnsKeyList = new ArrayList<>();
        List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListInFileDao(key, fileUuid, fileVersionId);
        // 公式
        List<String> columnsIdList = dmTableColumnsListDao.stream().filter(k -> "formula".equals(k.getKind())).map(HfDmColumns::getKey).collect(Collectors.toList());
        // 匹配
        // matchMap用于记录预读取 需要用到的参与匹配的数据 {matchId:[LabelValuePojo]}
        // matchDbId 和 matchFolderId 统称 matchId
        HashMap<String, List<LabelValuePojo>> matchMap = new HashMap<>();
        ArrayList<String> matchIdList = new ArrayList<>();
        HashMap<String, String> key4DataIndexMap = new HashMap<>();
        List<HfDmColumns> columnsIdMatchList = new ArrayList<>();  // 记录需要匹配的列

        List<String> columnsIdAllList = dmTableColumnsListDao.stream().map(HfDmColumns::getKey).collect(Collectors.toList());
        for (HfDmColumns dmColumns : dmTableColumnsListDao) {
            dmTableColumnsKeyList.add(dmColumns.getDataIndex());
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

            // 计算
            if (columnsIdList.contains(dmColumns.getKey())) {  // 公式字段
                // 剥离出[key]
                ArrayList<String> keyList = new ArrayList<>();
                String regex = "\\[(.*?)\\]";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(dmColumns.getFormula());
                while (matcher.find()) {
                    String result = matcher.group(1);
                    if (!keyList.contains(result)) {
                        keyList.add(result);
                    }
                }
                // 判断key是否都还存在
                for (String formulaKey : keyList) {
                    if (!columnsIdAllList.contains(formulaKey)) {
                        dmColumns.setExecute("failed:公式中列不存在");
                        break;
                    }
                }
            }

            // 匹配
            String matchType = dmColumns.getMatchType();
            if ("table".equals(matchType)) {  // 数据表
                columnsIdMatchList.add(dmColumns);
                String matchDbId = dmColumns.getMatchDbId();
                if (!matchIdList.contains(matchDbId)) {
                    List<LabelValuePojo> dmTableListLVByDbIdDao = workingTableMapper.getDmTableListLVByDbIdInFileDao(matchDbId, fileUuid, fileVersionId);
                    matchMap.put(matchDbId, dmTableListLVByDbIdDao);
                    matchIdList.add(matchDbId);
                }
            } else if ("file".equals(matchType)) {  // 文件
                columnsIdMatchList.add(dmColumns);
                String matchFolderId = dmColumns.getMatchFolderId();
                if (!matchIdList.contains(matchFolderId)) {
                    List<LabelValuePojo> dmTableListLVByDbIdDao = editToolMapper.getDocumentListLVDao(matchFolderId);
                    // 上传模板
                    List<LabelValuePojo> uploadModelFileListLVDao = editToolMapper.getUploadModelFileListLVDao(matchFolderId);
                    dmTableListLVByDbIdDao.addAll(uploadModelFileListLVDao);
                    matchMap.put(matchFolderId, dmTableListLVByDbIdDao);
                    matchIdList.add(matchFolderId);
                }
            }
            key4DataIndexMap.put(dmColumns.getKey(), dmColumns.getDataIndex());
        }

//        10  单选
//        20  多选
//        30  日期/时间
//        40  文本
//        50  富文本
//        60  图片
//        70  资源附件
//        80  金额
//        90  数值
        // 数据信息
        // 1.复杂筛选
        Object filterObj = hfDmTable.getFilter();
        String sql = null;  // 拼装查询语句
        if (filterObj != null && filterObj instanceof List) {
            List<Map<String, Object>> filterList = (List) hfDmTable.getFilter();
            List<HfDmTable> filter = new ArrayList<>();
            for (Map<String, Object> map : filterList) {
                filter.add(JSON.parseObject(JSON.toJSONString(map), HfDmTable.class));
            }

            // 确定查询条件是与还是或
            String qType = "and"; // 查询类型默认and
            for (HfDmTable dmTable : filter) {
                if (dmTable.getType() != null) {
                    qType = dmTable.getType();  // and or
                    break;
                }
            }
            sql = "";
            for (HfDmTable f : filter) {
                if (f.getField() != null) {  // 防止[{}]
                    // 判断筛选条件的字段类型
                    for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                        if (f.getField().equals(hfDmColumns.getDataIndex())) {
                            String fieldType = hfDmColumns.getFieldType();  // 获取到类型
                            // 根据类型组装查询语句
                            if ("10".equals(fieldType)) {  // 单选
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not".equals(f.getCondition())) {  // 不是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '')", qType, f.getField(), f.getField());
                                }
                            } else if ("20".equals(fieldType)) {  // 多选
                                // condition条件类型
                                if ("in".equals(f.getCondition())) {  // 包含
                                    sql = sql + String.format("%s (json_contains(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')), '%s'))", qType, f.getField(), JSON.toJSONString(f.getValue()).replaceAll("\\\"", "\\\\\"").replaceAll("\\\'", "\\\\\'"));
                                } else if ("not in".equals(f.getCondition())) {  // 不包含
                                    sql = sql + String.format("%s (!json_contains(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')), '%s'))", qType, f.getField(), JSON.toJSONString(f.getValue()).replaceAll("\\\"", "\\\\\"").replaceAll("\\\'", "\\\\\'"));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '[]')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '[]')", qType, f.getField(), f.getField());
                                }
                            } else if ("30".equals(fieldType)) {  // 时间
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not".equals(f.getCondition())) {  // 不等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gt".equals(f.getCondition())) {  // 大于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) > \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gte".equals(f.getCondition())) {  // 大于等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) >= \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lt".equals(f.getCondition())) {  // 小于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) < \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lte".equals(f.getCondition())) {  // 小于等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) <= \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '')", qType, f.getField(), f.getField());
                                }
                            } else if ("80".equals(fieldType) || "90".equals(fieldType)) {  // 金额 数值
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 等于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) = %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not".equals(f.getCondition())) {  // 不等于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) != %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gt".equals(f.getCondition())) {  // 大于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) > %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gte".equals(f.getCondition())) {  // 大于等于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) >= %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lt".equals(f.getCondition())) {  // 小于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) < %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lte".equals(f.getCondition())) {  // 小于等于
//                                sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) <= \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"","\\\\\""));
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) <= %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '{}')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '{}')", qType, f.getField(), f.getField());
                                }
                            } else if ("40".equals(fieldType) || "50".equals(fieldType)) {  // 文本 富文本
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("like".equals(f.getCondition())) {  // 包含
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) like \"%%%s%%\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not like".equals(f.getCondition())) {  // 不包含
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) not like \"%%%s%%\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("start with".equals(f.getCondition())) {  // 开头是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) like \"%s%%\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("end with".equals(f.getCondition())) {  // 结尾是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) like \"%%%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '')", qType, f.getField(), f.getField());
                                }
                            } else {
                                // condition条件类型
                                if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '' or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '[]')", qType, f.getField(), f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '' and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '[]')", qType, f.getField(), f.getField(), f.getField());
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if (sql != null && sql.length() > 3) {
                sql = sql.substring(3);
            } else {
                sql = null;
            }

        }

        // 排序
        Object sortObj = hfDmTable.getSort();
        String sortBySql = null;
        if (sortObj != null && sortObj instanceof List) {
            List<Map<String, String>> sort = (List<Map<String, String>>) hfDmTable.getSort();
            sortBySql = " order by ";
            for (Map<String, String> map : sort) {
                String type = map.get("type");
                // 判断排序条件的字段类型
                for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                    if (hfDmColumns.getDataIndex().equals(map.get("field"))) {
                        String fieldType = hfDmColumns.getFieldType();  // 获取到类型
                        // 根据类型组装查询语句
                        if ("10".equals(fieldType)) {  // 单选
                            sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                        } else if ("20".equals(fieldType)) {  // 多选
                            sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                        } else if ("30".equals(fieldType)) {  // 时间
                            sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                        } else if ("80".equals(fieldType) || "90".equals(fieldType)) {  // 金额 数值
                            sortBySql = sortBySql + String.format("CAST(SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) as DECIMAL(13,3)) %s,", map.get("field"), map.get("field"), map.get("sort"));
                        } else if ("40".equals(fieldType)) {  // 文本
                            if ("number".equals(type)) {  // number
                                sortBySql = sortBySql + String.format("CAST(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) as DECIMAL(13,3)) %s,", map.get("field"), map.get("sort"));
                            } else {  // string
                                sortBySql = sortBySql + String.format("CONVERT(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) USING gbk) %s,", map.get("field"), map.get("sort"));
                            }
                        } else {
                            // 其余类型不排序
//                            sortBySql = sortBySql + "";
                        }
                        break;
                    }
                }
            }
            if (!" order by ".equals(sortBySql)) {
                sortBySql = sortBySql.substring(0, sortBySql.length() - 1);
            } else {
                sortBySql = null;
            }
        }

        // 获取上卷聚合字段
        Map<String, Object> group = hfDmTable.getGroup();
        if (group != null && group.keySet().size() != 0) {
            // 记录上卷加聚合用到的字段的dataIndex 形成统计表的表头
            List<Map<String, Object>> groupTableDataIndexList = new ArrayList<>();
            int order = 0;
            // 上卷
            Object groupByObj = group.get("groupBy");
            String groupBySql = null;
            String groupBySqlWithName = null;
            String groupBySqlNotNull = null;
            List<String> groupBy = new ArrayList<>();
            if (groupByObj != null && groupByObj instanceof List) { // 不为空
                groupBy = (List) group.get("groupBy");
                if (groupBy.size() != 0) { // 并且长度不为0
                    groupBySql = "";
                    groupBySqlWithName = "";
                    groupBySqlNotNull = "";
                    for (String s : groupBy) {
                        // 记录上卷字段s是否在当前表中存在 不存在就抛出异常
                        Boolean sExist = false;
                        for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                            if (s.equals(hfDmColumns.getDataIndex())) {
                                sExist = true;
                                // 对照已有字段 获取字段名
                                HashMap<String, Object> map = new HashMap<>();
                                map.put("key", s);
                                map.put("dataIndex", s);
                                map.put("title", hfDmColumns.getTitle());
//                                map.put("fieldType", "40");
                                map.put("fieldType", hfDmColumns.getFieldType());
                                map.put("options", hfDmColumns.getOptions());
                                map.put("unit", hfDmColumns.getUnit());
                                map.put("visible", true);
                                map.put("required", false);
                                map.put("order", String.valueOf(order++));
                                groupTableDataIndexList.add(map);
                                groupBySql = groupBySql + String.format(" JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) ,", s);
                                groupBySqlWithName = groupBySqlWithName + String.format(" JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) `%s`,", s, s);
                                groupBySqlNotNull = groupBySqlNotNull + String.format(" JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and", s);
                                break;
                            }
                        }
                        if (!sExist) {
                            throw new WorkTableException("上卷字段在当前表中不存在,请检查是否已被删除");
                        }
                    }
                    // 去除末位多余逗号
                    groupBySql = groupBySql.substring(0, groupBySql.length() - 1);
                    groupBySqlWithName = groupBySqlWithName.substring(0, groupBySqlWithName.length() - 1);
                    groupBySqlNotNull = groupBySqlNotNull.substring(0, groupBySqlNotNull.length() - 4);  // 去除末位and
                }
            }
            // 聚合
            Object groupByFunctionsObj = group.get("groupByFunctions");
            String groupByFunctionsSql = null;
            List<Map<String, Object>> groupByFunctions = new ArrayList<>();
            if (groupByFunctionsObj != null && groupByFunctionsObj instanceof List) {
                groupByFunctions = (List) group.get("groupByFunctions");
                groupByFunctionsSql = "";
                for (Map<String, Object> map : groupByFunctions) { // { field: string, function: sum | count | max | avg | groupConcat, distinct: boolean, rename: string }
                    String field = (String) map.get("field");  // 聚合字段
                    String function = (String) map.get("function");  // 聚合方法 sum | count | max | avg | groupConcat
                    String rename = null;
                    if (map.get("rename") != null) {
                        rename = (String) map.get("rename");  // 聚合结果命名
                    } else {
                        for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                            if (field.equals(hfDmColumns.getDataIndex())) {
                                rename = hfDmColumns.getTitle();
                                break;
                            }
                        }
                    }
                    String fieldType = null;  // 字段类型  这里只判断80 90 数值 金额
                    String unitId = null;  // 单位的unitId
                    for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                        if (field.equals(hfDmColumns.getDataIndex())) {
                            fieldType = hfDmColumns.getFieldType();
                            try {
                                List<String> unit = (List) hfDmColumns.getUnit();
                                unitId = unit.get(1);
                            } catch (Exception e) {

                            }
                            break;
                        }
                    }
                    // 特别的 如果field的值为system-count那么计算行统计
                    if ("system-count".equals(field)) {
                        function = "count";
                        rename = "行数统计";
                    }

                    // 对照已有字段 获取字段名
                    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                    HashMap<String, Object> m = new HashMap<>();
                    m.put("key", uuid);
                    m.put("dataIndex", uuid);
                    m.put("title", rename);
                    m.put("fieldType", "40");
                    m.put("visible", true);
                    m.put("required", false);
                    m.put("order", String.valueOf(order++));
                    groupTableDataIndexList.add(m);

                    if ("sum".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", sum(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"'))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", sum(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("count".equals(function)) {
                        if ("system-count".equals(field)) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", count(1) `%s`", uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", count(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("max".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", max(cast(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"')) as DECIMAL(13,3))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", max(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("min".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", min(cast(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"')) as DECIMAL(13,3))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", min(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("avg".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", avg(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"'))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", avg(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("groupConcat".equals(function)) {
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", group_concat(JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"'))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", group_concat(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("groupConcatDistinct".equals(function)) {  // 去重组合
                        if (("80".equals(fieldType) || "90".equals(fieldType)) && unitId != null) {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", group_concat(distinct JSON_UNQUOTE(json_extract(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')),'$.\"%s\"'))) `%s`", field, unitId, uuid);
                        } else {
                            groupByFunctionsSql = groupByFunctionsSql + String.format(", group_concat(distinct JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                        }
                    } else if ("countDistinct".equals(function)) {  // 去重计数
                        groupByFunctionsSql = groupByFunctionsSql + String.format(", count(distinct JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"'))) `%s`", field, uuid);
                    } else {
                        // 未配置的新聚合方法
                    }
                }
            }

            List<Map<String, Object>> optionsListGroupByDao = new ArrayList<>();
            if ("1".equals(isPaged)) {
                // 需要分页
                String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
                System.out.println("表编号为: " + hashMod);
                PageHelper.startPage(pageNum, pageSize);
                optionsListGroupByDao = splitTableMapper.getDmDataListInFileGroupByDao(userId, key, null, sql, sortBySql, groupBySql, groupBySqlWithName, groupBySqlNotNull, groupByFunctionsSql, null, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            } else {
                // 不需要分页
                String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
                System.out.println("表编号为: " + hashMod);
                optionsListGroupByDao = splitTableMapper.getDmDataListInFileGroupByDao(userId, key, null, sql, sortBySql, groupBySql, groupBySqlWithName, groupBySqlNotNull, groupByFunctionsSql, null, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            }

            HashMap<String, String> typeMap = new HashMap<String, String>();  // 快捷记录类型
            for (Map<String, Object> map : optionsListGroupByDao) {
                // 判断optionsListGroupByDao中的数据类型 如果是数值就转为map 如果是选项就转list
                for (String s : map.keySet()) {
                    if (typeMap.keySet().contains(s)) {
                        if ("20".equals(typeMap.get(s))) {  // 多选
                            map.put(s, JSON.parseObject(String.valueOf(map.get(s)), List.class));
                        } else if ("80".equals(typeMap.get(s)) || "90".equals(typeMap.get(s))) { // 数值 金额
                            map.put(s, JSON.parseObject(String.valueOf(map.get(s)), Map.class));
                        } else if ("60".equals(typeMap.get(s)) || "70".equals(typeMap.get(s))) { // 图片 附件
                            map.put(s, JSON.parseObject(String.valueOf(map.get(s)), List.class));
                        } else {
                            // 不用动
                        }
                    } else {
                        for (Map<String, Object> stringObjectMap : groupTableDataIndexList) {
                            if (s.equals((String) stringObjectMap.get("dataIndex"))) {
                                typeMap.put(s, (String) stringObjectMap.get("fieldType"));
                                if ("20".equals(typeMap.get(s))) {  // 多选
                                    map.put(s, JSON.parseObject(String.valueOf(map.get(s)), List.class));
                                } else if ("80".equals(typeMap.get(s)) || "90".equals(typeMap.get(s))) { // 数值 金额
                                    map.put(s, JSON.parseObject(String.valueOf(map.get(s)), Map.class));
                                } else if ("60".equals(typeMap.get(s)) || "70".equals(typeMap.get(s))) { // 图片 附件
                                    map.put(s, JSON.parseObject(String.valueOf(map.get(s)), List.class));
                                } else {
                                    // 不用动
                                }
                            }
                        }
                    }
                }
                //  增加key
                map.put("key", UUID.randomUUID().toString().replaceAll("-", ""));
            }

            // 返回结果
            if ("1".equals(isPaged)) {
                // 需要分页
                PageInfo pageInfo = new PageInfo(optionsListGroupByDao);
                // Map转实体类
                Map<String, Object> m = JSON.parseObject(JSON.toJSONString(pageInfo), Map.class);
                m.put("columns", groupTableDataIndexList);
                m.put("allColumns", dmTableColumnsListDao);
                HashMap<String, Object> condition = new HashMap<>();
                condition.put("filter", fatherFilterObj);
                condition.put("group", fatherGroupObj);
                condition.put("tableName", fatherTableInfo.getTableName());
                condition.put("tableId", fatherTableInfo.getTableId());
                condition.put("allColumns", fatherTableColumnsListDao);
                m.put("condition", condition);
                return TResponseVo.success(m);
            } else {
                // 不需要分页
                HashMap<String, Object> retMap = new HashMap<>();
                retMap.put("list", optionsListGroupByDao);
                retMap.put("columns", groupTableDataIndexList);
                retMap.put("allColumns", dmTableColumnsListDao);
                HashMap<String, Object> condition = new HashMap<>();
                condition.put("filter", fatherFilterObj);
                condition.put("group", fatherGroupObj);
                condition.put("tableName", fatherTableInfo.getTableName());
                condition.put("tableId", fatherTableInfo.getTableId());
                condition.put("allColumns", fatherTableColumnsListDao);
                retMap.put("condition", condition);
                return TResponseVo.success(retMap);
            }
        } else {
            List<HfDmData> optionsListDao = new ArrayList<>();
            if ("1".equals(isPaged)) {
                // 需要分页
                String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
                System.out.println("表编号为: " + hashMod);
                PageHelper.startPage(pageNum, pageSize);
                optionsListDao = splitTableMapper.getDmDataListInFileDao(userId, key, null, sql, sortBySql, null, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            } else {
                // 不需要分页
                String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
                System.out.println("表编号为: " + hashMod);
                optionsListDao = splitTableMapper.getDmDataListInFileDao(userId, key, null, sql, sortBySql, null, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            }

            // 2.查库
            ArrayList<Map<String, Object>> list = new ArrayList<>();  // 记录返回内容

            // 反序列化
            for (HfDmData hfDmData : optionsListDao) {
                Object dataContent = hfDmData.getDataContent();
                if (dataContent == null || StringUtils.isBlank(String.valueOf(dataContent))) {
                    System.out.println(hfDmData.getDataId() + "数据格式有误");
                } else {
                    Map<String, Object> dataContentMap = JSON.parseObject(String.valueOf(dataContent), Map.class);

                    // 即时匹配
                    hfDmData.setMatchResult(null);
                    List<Map<String, String>> matchResultList = new ArrayList<>();
                    for (HfDmColumns columnsInfoDao : columnsIdMatchList) { // 需要匹配的列
                        if ("40".equals(columnsInfoDao.getFieldType()) && columnsInfoDao.getMatchType() != null) {  // 只匹配文本类型字段
                            if ("table".equals(columnsInfoDao.getMatchType())) {
                                String dataIndex = key4DataIndexMap.get(columnsInfoDao.getKey());
                                Object o = dataContentMap.get(dataIndex);
                                if (o != null) {
                                    String leftKey = String.valueOf(o);  // 行数据中用于匹配的值
                                    List<LabelValuePojo> labelValuePojos = matchMap.get(columnsInfoDao.getMatchDbId());
                                    List<String> listResult = labelValuePojos.stream().filter(k -> leftKey.equals(k.getLabel())).map(LabelValuePojo::getValue).collect(Collectors.toList());
                                    if (!listResult.isEmpty()) {
                                        // 放入新数据
                                        HashMap<String, String> stringStringHashMap = new HashMap<>();
                                        stringStringHashMap.put("tableId", listResult.get(0));
                                        stringStringHashMap.put("dataIndex", dataIndex);
                                        matchResultList.add(stringStringHashMap);
                                    }
                                }
                            } else if ("file".equals(columnsInfoDao.getMatchType())) {
                                String dataIndex = key4DataIndexMap.get(columnsInfoDao.getKey());
                                Object o = dataContentMap.get(dataIndex);
                                if (o != null) {
                                    String leftKey = String.valueOf(o);  // 行数据中用于匹配的值
                                    List<LabelValuePojo> labelValuePojos = matchMap.get(columnsInfoDao.getMatchFolderId());
                                    List<String> listResult = labelValuePojos.stream().filter(k -> leftKey.equals(k.getLabel())).map(LabelValuePojo::getValue).collect(Collectors.toList());
                                    if (!listResult.isEmpty()) {
                                        // 放入新数据
                                        HashMap<String, String> stringStringHashMap = new HashMap<>();
                                        stringStringHashMap.put("fileUuid", listResult.get(0));
                                        stringStringHashMap.put("dataIndex", dataIndex);
                                        matchResultList.add(stringStringHashMap);
                                    }
                                }
                            }

                        } else {
                            System.out.println("非匹配列");
                        }
                    }

                    if (matchResultList == null || matchResultList.isEmpty()) {
                        hfDmData.setMatchResult(null);
                    } else {
                        hfDmData.setMatchResult(matchResultList);
                    }

                    // 如果当前表头不包含 key就不返回
                    dataContentMap.keySet().removeIf(k -> !dmTableColumnsKeyList.contains(k));
                    dataContentMap.put("key", hfDmData.getDataId());
                    dataContentMap.put("matchResult", hfDmData.getMatchResult());
                    list.add(dataContentMap);

                }
            }

            // 返回结果
            List<Map<String, Object>> groupTableDataIndexList = new ArrayList<>(); // dmTableColumnsListDao转成listmap返回
            groupTableDataIndexList = JSON.parseObject(JSON.toJSONString(dmTableColumnsListDao), List.class);
            if ("1".equals(isPaged)) {
                // 需要分页
                PageInfo pageInfo = new PageInfo(optionsListDao);
                pageInfo.setList(list);
                // Map转实体类
                Map<String, Object> m = JSON.parseObject(JSON.toJSONString(pageInfo), Map.class);
                m.put("columns", groupTableDataIndexList);
                m.put("allColumns", dmTableColumnsListDao);
                HashMap<String, Object> condition = new HashMap<>();
                condition.put("filter", fatherFilterObj);
                condition.put("group", fatherGroupObj);
                condition.put("tableName", fatherTableInfo.getTableName());
                condition.put("tableId", fatherTableInfo.getTableId());
                condition.put("allColumns", fatherTableColumnsListDao);
                m.put("condition", condition);
                return TResponseVo.success(m);
            } else {
                // 不需要分页
                HashMap<String, Object> retMap = new HashMap<>();
                retMap.put("list", list);
                retMap.put("columns", groupTableDataIndexList);
                retMap.put("allColumns", dmTableColumnsListDao);
                HashMap<String, Object> condition = new HashMap<>();
                condition.put("filter", fatherFilterObj);
                condition.put("group", fatherGroupObj);
                condition.put("tableName", fatherTableInfo.getTableName());
                condition.put("tableId", fatherTableInfo.getTableId());
                condition.put("allColumns", fatherTableColumnsListDao);
                retMap.put("condition", condition);
                return TResponseVo.success(retMap);
            }
        }
    }

    /**
     * 修改视图(view)的条件(condition)
     *
     * @param key    视图id
     * @param filter 过滤条件
     * @param group  聚合条件
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo updateViewConditionService(String key, List<Map<String, Object>> filter, Map<String, Object> group) throws Exception {
        String fatherFilter = null;
        if (filter != null && filter.size() != 0) {
            fatherFilter = JSON.toJSONString(filter);
        }
        String fatherGroup = null;
        if (group != null && group.keySet().size() != 0) {
            fatherGroup = JSON.toJSONString(group);
        }
        workingTableMapper.updateViewConditionDao(key, fatherFilter, fatherGroup);
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("msg", "视图条件修改完成");
        return TResponseVo.success(retMap);
    }

    @Override
    public TResponseVo updateViewConditionInFileService(String key, List<Map<String, Object>> filter, Map<String, Object> group, String fileUuid, String fileVersionId) throws Exception {
        String fatherFilter = null;
        if (filter != null && filter.size() != 0) {
            fatherFilter = JSON.toJSONString(filter);
        }
        String fatherGroup = null;
        if (group != null && group.keySet().size() != 0) {
            fatherGroup = JSON.toJSONString(group);
        }
        workingTableMapper.updateViewConditionInFileDao(key, fatherFilter, fatherGroup, fileUuid, fileVersionId);
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("msg", "视图条件修改完成");
        return TResponseVo.success(retMap);
    }

    /**
     * 重跑视图(view)数据
     *
     * @param key 需要重跑的视图id
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo rerunViewDataService(String key, String userId) throws Exception {
        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoDao(key);
        String fatherTableId = dmTableInfoDao.getFatherTableId();
        String fatherFilter = dmTableInfoDao.getFatherFilter();
        String fatherGroup = dmTableInfoDao.getFatherGroup();
        Object fatherFilterObj = null;
        Map<String, Object> fatherGroupObj = null;
        if (StringUtils.isNotBlank(fatherFilter)) {
            fatherFilterObj = JSON.parseObject(fatherFilter, List.class);
        }
        if (StringUtils.isNotBlank(fatherGroup)) {
            fatherGroupObj = JSON.parseObject(fatherGroup, Map.class);
        }
        HfDmTable hfDmTable = new HfDmTable();
        hfDmTable.setKey(fatherTableId);
        hfDmTable.setFilter(fatherFilterObj);
        hfDmTable.setGroup(fatherGroupObj);

        // 设置为不分页查询
        hfDmTable.setIsPaged("0");
        // 查询结果
        TResponseVo dmTableContentService = getDmTableContentService(hfDmTable);
        Map data = (Map) dmTableContentService.getData();
        List<Map<String, Object>> list = (List) data.get("list");  // data
        List<Map<String, Object>> columns = (List) data.get("columns");  // column

        Date createTime = new Date();

        ArrayList<HfDmColumns> hfDmColumnsList = new ArrayList<>();  // 表头入库
        for (int i = 0; i < columns.size(); i++) {  // CommonExcelModel的列数
            Map<String, Object> map = columns.get(i);
            String title = (String) map.get("title");
            String dataIndex = (String) map.get("dataIndex");
            HfDmColumns hfDmColumns = new HfDmColumns();
            hfDmColumns.setTableId(key);
            hfDmColumns.setKey(dataIndex);
            hfDmColumns.setTitle(title);
            hfDmColumns.setDataIndex(dataIndex);
            hfDmColumns.setFieldType((String) map.get("fieldType"));  // 先默认都是文本类型
            hfDmColumns.setOptions(map.get("options") == null ? null : JSON.toJSONString(map.get("options")));
            hfDmColumns.setUnit(map.get("unit") == null ? null : JSON.toJSONString(map.get("unit")));
            hfDmColumns.setCreateUserId(userId);
            hfDmColumns.setCreateTime(createTime);
            hfDmColumns.setOrder(String.valueOf(i));
            hfDmColumnsList.add(hfDmColumns);
        }

        ArrayList<HfDmData> hfDmDataList = new ArrayList<>();  // 数据入库
        if (list.size() != 0) {
            Integer order = 0;
            for (Map<String, Object> CommonExcelModelMap : list) {
                // 遍历每行数据
                HfDmData hfDmData = new HfDmData();
                hfDmData.setDataId(UUID.randomUUID().toString().replaceAll("-", ""));
                hfDmData.setTableId(key);
                hfDmData.setOrder(String.valueOf(order++));
                hfDmData.setCreateUserId(userId);
                hfDmData.setCreateTime(createTime);
                CommonExcelModelMap.remove("key");
                hfDmData.setDataContent(JSON.toJSONString(CommonExcelModelMap));
                hfDmDataList.add(hfDmData);
            }
        }

        // 清空视图
        workingTableMapper.clearDmDataDao(key);
        workingTableMapper.clearDmColumnsDao(key);

        if (hfDmColumnsList.size() != 0) {
            // 创建表头
            workingTableMapper.uploadHfDmColumnsDao(hfDmColumnsList);
            if (hfDmDataList.size() != 0) {
                // 创建数据
                // 分批插入
                while (hfDmDataList.size() > 1000) {
                    List<HfDmData> hfDmData = hfDmDataList.subList(0, 1000);
                    workingTableMapper.uploadHfDmDataDao(hfDmData);
                    hfDmDataList.subList(0, 1000).clear();
                }
                workingTableMapper.uploadHfDmDataDao(hfDmDataList);
            }
        }
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("msg", "数据重跑完成");
        return TResponseVo.success(retMap);
    }

    @Override
    public TResponseVo rerunViewDataInFileService(String key, String userId, String fileUuid, String fileVersionId) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoInFileDao(key, fileUuid, fileVersionId);
        String fatherTableId = dmTableInfoDao.getFatherTableId();
        String fatherFilter = dmTableInfoDao.getFatherFilter();
        String fatherGroup = dmTableInfoDao.getFatherGroup();
        Object fatherFilterObj = null;
        Map<String, Object> fatherGroupObj = null;
        if (StringUtils.isNotBlank(fatherFilter)) {
            fatherFilterObj = JSON.parseObject(fatherFilter, List.class);
        }
        if (StringUtils.isNotBlank(fatherGroup)) {
            fatherGroupObj = JSON.parseObject(fatherGroup, Map.class);
        }
        HfDmTable hfDmTable = new HfDmTable();
        hfDmTable.setKey(fatherTableId);
        hfDmTable.setFilter(fatherFilterObj);
        hfDmTable.setGroup(fatherGroupObj);
        hfDmTable.setFileUuid(fileUuid);
        hfDmTable.setFileVersionId(fileVersionId);

        // 设置为不分页查询
        hfDmTable.setIsPaged("0");
        // 查询结果
        TResponseVo dmTableContentService = getDmTableContentInFileService(hfDmTable);
        Map data = (Map) dmTableContentService.getData();
        List<Map<String, Object>> list = (List) data.get("list");  // data
        List<Map<String, Object>> columns = (List) data.get("columns");  // column

        Date createTime = new Date();

        ArrayList<HfDmColumns> hfDmColumnsList = new ArrayList<>();  // 表头入库
        for (int i = 0; i < columns.size(); i++) {  // CommonExcelModel的列数
            Map<String, Object> map = columns.get(i);
            String title = (String) map.get("title");
            String dataIndex = (String) map.get("dataIndex");
            HfDmColumns hfDmColumns = new HfDmColumns();
            hfDmColumns.setTableId(key);
            hfDmColumns.setKey(dataIndex);
            hfDmColumns.setTitle(title);
            hfDmColumns.setDataIndex(dataIndex);
            hfDmColumns.setFieldType((String) map.get("fieldType"));  // 先默认都是文本类型
            hfDmColumns.setOptions(map.get("options") == null ? null : JSON.toJSONString(map.get("options")));
            hfDmColumns.setUnit(map.get("unit") == null ? null : JSON.toJSONString(map.get("unit")));
            hfDmColumns.setCreateUserId(userId);
            hfDmColumns.setCreateTime(createTime);
            hfDmColumns.setOrder(String.valueOf(i));
            hfDmColumnsList.add(hfDmColumns);
        }

        ArrayList<HfDmData> hfDmDataList = new ArrayList<>();  // 数据入库
        if (list.size() != 0) {
            Integer order = 0;
            for (Map<String, Object> CommonExcelModelMap : list) {
                // 遍历每行数据
                HfDmData hfDmData = new HfDmData();
                hfDmData.setDataId(UUID.randomUUID().toString().replaceAll("-", ""));
                hfDmData.setTableId(key);
                hfDmData.setOrder(String.valueOf(order++));
                hfDmData.setCreateUserId(userId);
                hfDmData.setCreateTime(createTime);
                CommonExcelModelMap.remove("key");
                hfDmData.setDataContent(JSON.toJSONString(CommonExcelModelMap));
                hfDmDataList.add(hfDmData);
            }
        }

        // 清空视图
        String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
        System.out.println("表编号为: " + hashMod);
        splitTableMapper.clearDmDataInFileDao(key, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
        workingTableMapper.clearDmColumnsInFileDao(key, fileUuid, fileVersionId);

        if (hfDmColumnsList.size() != 0) {
            // 创建表头
            workingTableMapper.uploadHfDmColumnsInFileDao(hfDmColumnsList, fileUuid, fileVersionId);
            if (hfDmDataList.size() != 0) {
                // 创建数据
                // 分批插入
                hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
                System.out.println("表编号为: " + hashMod);
                while (hfDmDataList.size() > 1000) {
                    List<HfDmData> hfDmData = hfDmDataList.subList(0, 1000);
                    splitTableMapper.uploadHfDmDataInFileDao(hfDmData, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
                    hfDmDataList.subList(0, 1000).clear();
                }
                splitTableMapper.uploadHfDmDataInFileDao(hfDmDataList, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            }
        }
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("msg", "数据重跑完成");
        return TResponseVo.success(retMap);
    }

    /**
     * 复制表
     *
     * @param key
     * @param targetDb
     * @param userId
     * @param includeData
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo copyTableService(String key, String newTableName, List<String> targetDb, String userId, Boolean includeData) throws Exception {
        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoDao(key);
        if (dmTableInfoDao == null) {
            throw new WorkTableException("复制的表不存在或已删除");
        }
        String fatherTableId = dmTableInfoDao.getFatherTableId();
        if (StringUtils.isNotBlank(fatherTableId)) {
            throw new WorkTableException("不能复制视图");
        }
        String newTableId = UUID.randomUUID().toString().replaceAll("-", "");
        String dbId = targetDb.get(0);
        workingTableMapper.copyDmTableDao(key, newTableId, newTableName, dbId, userId);
        workingTableMapper.copyDmColumnsDao(key, newTableId, userId);
        if (includeData != null && includeData) {
            workingTableMapper.copyDmDataDao(key, newTableId, userId);

            // 获取该表的视图
            List<String> dmViewList = workingTableMapper.getDmViewListDao(key);
            int i = 0;
            for (String viewId : dmViewList) {
                // 随机一个新tableId
                String newViewId = UUID.randomUUID().toString().replaceAll("-", "");
                // 0.导入表索引
                workingTableMapper.copyDmViewDao(viewId, newViewId, newTableId, dbId, userId);
                // 1.导入表头
                workingTableMapper.copyDmColumnsDao(viewId, newViewId, userId);
                if (includeData != null && includeData) {
                    // 2.导入数据
                    workingTableMapper.copyDmDataDao(viewId, newViewId, userId);
                }
            }
        }
        return TResponseVo.success("复制成功");
    }

    @Override
    public TResponseVo copyTableInFileService(String key, String newTableName, List<String> targetDb, String userId, Boolean includeData, String fileUuid, String fileVersionId) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoInFileDao(key, fileUuid, fileVersionId);
        if (dmTableInfoDao == null) {
            throw new WorkTableException("复制的表不存在或已删除");
        }
        String fatherTableId = dmTableInfoDao.getFatherTableId();
        if (StringUtils.isNotBlank(fatherTableId)) {
            throw new WorkTableException("不能复制视图");
        }
        String newTableId = UUID.randomUUID().toString().replaceAll("-", "");
        String dbId = targetDb.get(0);
        workingTableMapper.copyDmTableInFileDao(key, newTableId, newTableName, dbId, userId, fileUuid, fileVersionId);
        workingTableMapper.copyDmColumnsInFileDao(key, newTableId, userId, fileUuid, fileVersionId);
        if (includeData != null && includeData) {
            String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
            System.out.println("表编号为: " + hashMod);
            splitTableMapper.copyDmDataInFileDao(key, newTableId, userId, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);

            // 获取该表的视图
            List<String> dmViewList = workingTableMapper.getDmViewListInFileDao(key, fileUuid, fileVersionId);
            int i = 0;
            for (String viewId : dmViewList) {
                // 随机一个新tableId
                String newViewId = UUID.randomUUID().toString().replaceAll("-", "");
                // 0.导入表索引
                workingTableMapper.copyDmViewInFileDao(viewId, newViewId, newTableId, dbId, userId, fileUuid, fileVersionId);
                // 1.导入表头
                workingTableMapper.copyDmColumnsInFileDao(viewId, newViewId, userId, fileUuid, fileVersionId);
                if (includeData != null && includeData) {
                    // 2.导入数据
                    hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
                    System.out.println("表编号为: " + hashMod);
                    splitTableMapper.copyDmDataInFileDao(viewId, newViewId, userId, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
                }
            }
        }

        return TResponseVo.success("复制成功");
    }


    /**
     * 用数据管理的表替换文件阶段的表
     *
     * @param key            数据管理表id
     * @param replaceTableId 文内被替换的表id
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo replaceTableService(String key, String replaceTableId, String fileUuid, String fileVersionId) throws Exception {

        return null;
    }

    /**
     * 保存结果数据
     *
     * @param hfDmTable
     * @param saveType  table|view 保存成新表|保存成视图
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo saveDmDataService(HfDmTable hfDmTable, String saveType) throws Exception {
        // sort与group互斥
        Object sort = hfDmTable.getSort();
        if (sort != null) {  // 排序加过滤 结果保存成新表
            hfDmTable.setGroup(null);
            Boolean overwrite = hfDmTable.getOverwrite();
            if (overwrite != null && overwrite) {
                // 排序
                Object sortObj = hfDmTable.getSort();
                String sortBySql = null;
                List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListDao(hfDmTable.getKey());
                if (sortObj != null && sortObj instanceof List) {
                    List<Map<String, String>> sort1 = (List) hfDmTable.getSort();
                    sortBySql = " order by ";
                    for (Map<String, String> map : sort1) {
                        String type = map.get("type");
                        // 判断排序条件的字段类型
                        for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                            if (hfDmColumns.getDataIndex().equals(map.get("field"))) {
                                String fieldType = hfDmColumns.getFieldType();  // 获取到类型
                                // 根据类型组装查询语句
                                if ("10".equals(fieldType)) {  // 单选
                                    sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                                } else if ("20".equals(fieldType)) {  // 多选
                                    sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                                } else if ("30".equals(fieldType)) {  // 时间
                                    sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                                } else if ("80".equals(fieldType) || "90".equals(fieldType)) {  // 金额 数值
                                    sortBySql = sortBySql + String.format("CAST(SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) as DECIMAL(13,3)) %s,", map.get("field"), map.get("field"), map.get("sort"));
                                } else if ("40".equals(fieldType)) {  // 文本
                                    if ("number".equals(type)) {  // number
                                        sortBySql = sortBySql + String.format("CAST(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) as DECIMAL(13,3)) %s,", map.get("field"), map.get("sort"));
                                    } else {  // string
                                        sortBySql = sortBySql + String.format("CONVERT(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) USING gbk) %s,", map.get("field"), map.get("sort"));
                                    }
                                } else {
                                    // 其余类型不排序
//                            sortBySql = sortBySql + "";
                                }
                                break;
                            }
                        }
                    }
                    if (!" order by ".equals(sortBySql)) {
                        sortBySql = sortBySql.substring(0, sortBySql.length() - 1);
                    } else {
                        sortBySql = null;
                    }
                }
                // 不需要分页
                List<String> dmDataIdList = workingTableMapper.getDmDataIdListDao(null, hfDmTable.getKey(), null, null, sortBySql, null);
                String sql = "update hf_dm_data \n" +
                        "set `order`=  case ";
                for (int i = 0; i < dmDataIdList.size(); i++) {
                    sql = sql + " when table_id = '" + hfDmTable.getKey() + "' and data_id = '" + dmDataIdList.get(i) + "' then '" + String.valueOf(i) + "' ";
                }
                sql = sql + " end\n" +
                        "where 1=1 and table_id = '" + hfDmTable.getKey() + "'";
//                System.out.println("-------------");
//                System.out.println(sql);
//                System.out.println("-------------");
                workingTableMapper.orderDmDataDao(sql);
                HashMap<String, Object> retMap = new HashMap<>();
                retMap.put("msg", "新表保存成功");
                retMap.put("tableId", hfDmTable.getKey());
                HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoDao(hfDmTable.getKey());
                retMap.put("table", dmTableInfoDao);
                return TResponseVo.success(retMap);
            }
        } else {  // 上卷加过滤 结果保存成新表
            // 有group时默认不读取sort内容
            hfDmTable.setOverwrite(false);  // 防止group时出现
        }
        // 设置为不分页查询
        hfDmTable.setIsPaged("0");

        // 记录来源表id
        String fatherTableId = hfDmTable.getKey();
        String fatherFilter = null;
        String fatherGroup = null;
        // 获取筛选字段
        Object filterObj = hfDmTable.getFilter();
        if (filterObj != null && filterObj instanceof List) {
            fatherFilter = JSON.toJSONString(filterObj);
        }
        // 获取上卷聚合字段
        Map<String, Object> group = hfDmTable.getGroup();
        if (group != null && group.keySet().size() != 0) {
            fatherGroup = JSON.toJSONString(group);
        }

        // 查询结果
        TResponseVo dmTableContentService = getDmTableContentService(hfDmTable);
        Map data = (Map) dmTableContentService.getData();
        List<Map<String, Object>> list = (List) data.get("list");  // data
        List<Map<String, Object>> columns = (List) data.get("columns");  // column
        String createUserId = hfDmTable.getCreateUserId();
        String tableName = hfDmTable.getTableName();
        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoDao(hfDmTable.getKey());
        String dbId = dmTableInfoDao.getDbId();

        // 0.准备数据表
        String tableId = UUID.randomUUID().toString().replaceAll("-", "");

        ArrayList<HfDmColumns> hfDmColumnsList = new ArrayList<>();  // 表头入库
        Date createTime = new Date();
        for (int i = 0; i < columns.size(); i++) {  // CommonExcelModel的列数
            Map<String, Object> map = columns.get(i);
            String title = (String) map.get("title");
            String dataIndex = (String) map.get("dataIndex");
            HfDmColumns hfDmColumns = new HfDmColumns();
            hfDmColumns.setTableId(tableId);
            hfDmColumns.setKey(dataIndex);
            hfDmColumns.setTitle(title);
            hfDmColumns.setDataIndex(dataIndex);
            hfDmColumns.setFieldType((String) map.get("fieldType"));  // 先默认都是文本类型
            hfDmColumns.setOptions(map.get("options") == null ? null : JSON.toJSONString(map.get("options")));
            hfDmColumns.setUnit(map.get("unit") == null ? null : JSON.toJSONString(map.get("unit")));
            hfDmColumns.setCreateUserId(createUserId);
            hfDmColumns.setCreateTime(createTime);
            hfDmColumns.setOrder(String.valueOf(i));
            hfDmColumnsList.add(hfDmColumns);
        }
        // 1.2 list的其余行是dataSource
        ArrayList<HfDmData> hfDmDataList = new ArrayList<>();  // 数据入库
        if (list.size() != 0) {
            Integer order = 0;
            for (Map<String, Object> CommonExcelModelMap : list) {
                // 遍历每行数据
                HfDmData hfDmData = new HfDmData();
                hfDmData.setDataId(UUID.randomUUID().toString().replaceAll("-", ""));
                hfDmData.setTableId(tableId);
                hfDmData.setOrder(String.valueOf(order++));
                hfDmData.setCreateUserId(createUserId);
                hfDmData.setCreateTime(createTime);
                CommonExcelModelMap.remove("key");
                hfDmData.setDataContent(JSON.toJSONString(CommonExcelModelMap));
                hfDmDataList.add(hfDmData);
            }
        }
        // 2.数据入库
        // 创建表
        HfDmTable hfDmTable1 = new HfDmTable();
        hfDmTable1.setTableId(tableId);
        hfDmTable1.setTableName(tableName == null ? dmTableInfoDao.getTableName() : tableName);
        hfDmTable1.setDbId(dbId);
        hfDmTable1.setTableDesc(hfDmTable.getDesc());
        hfDmTable1.setCreateUserId(createUserId);
        // 补充信息
        hfDmTable1.setDesc(hfDmTable.getDesc());
        hfDmTable1.setTitle(tableName == null ? dmTableInfoDao.getTableName() : tableName);
        hfDmTable1.setType("table");
        hfDmTable1.setKey(tableId);
        if ("view".equals(saveType)) {  // 如果是保存成视图view
            hfDmTable1.setFatherTableId(fatherTableId);
            hfDmTable1.setFatherFilter(fatherFilter);
            hfDmTable1.setFatherGroup(fatherGroup);
        }
        workingTableMapper.createDmTableDao(hfDmTable1);
        if (hfDmColumnsList.size() != 0) {
            // 创建表头
            workingTableMapper.uploadHfDmColumnsDao(hfDmColumnsList);
            if (hfDmDataList.size() != 0) {
                // 创建数据
                // 分批插入
                while (hfDmDataList.size() > 1000) {
                    List<HfDmData> hfDmData = hfDmDataList.subList(0, 1000);
                    workingTableMapper.uploadHfDmDataDao(hfDmData);
                    hfDmDataList.subList(0, 1000).clear();
                }
                workingTableMapper.uploadHfDmDataDao(hfDmDataList);
            }
        }
        if ("view".equals(saveType)) {  // 如果是保存成视图view
            HashMap<String, Object> retMap = new HashMap<>();
            retMap.put("msg", "新表保存成功");
            retMap.put("viewId", tableId);
            hfDmTable1.setViewId(tableId);
            hfDmTable1.setTableId(null);
            hfDmTable1.setType("view");
            retMap.put("view", hfDmTable1);
            return TResponseVo.success(retMap);
        }
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("msg", "新表保存成功");
        retMap.put("tableId", tableId);
        retMap.put("table", hfDmTable1);
        return TResponseVo.success(retMap);
    }

    @Override
    public TResponseVo saveDmDataInFileService(HfDmTable hfDmTable, String saveType) throws Exception {
        // 查询主文件
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        // sort与group互斥
        Object sort = hfDmTable.getSort();
        if (sort != null) {  // 排序加过滤 结果保存成新表
            hfDmTable.setGroup(null);
            Boolean overwrite = hfDmTable.getOverwrite();
            if (overwrite != null && overwrite) {
                // 排序
                Object sortObj = hfDmTable.getSort();
//                String fileUuid = hfDmTable.getFileUuid();
//                String fileVersionId = hfDmTable.getFileVersionId();
                String sortBySql = null;
                List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListInFileDao(hfDmTable.getKey(), fileUuid, fileVersionId);
                if (sortObj != null && sortObj instanceof List) {
                    List<Map<String, String>> sort1 = (List) hfDmTable.getSort();
                    sortBySql = " order by ";
                    for (Map<String, String> map : sort1) {
                        String type = map.get("type");
                        // 判断排序条件的字段类型
                        for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                            if (hfDmColumns.getDataIndex().equals(map.get("field"))) {
                                String fieldType = hfDmColumns.getFieldType();  // 获取到类型
                                // 根据类型组装查询语句
                                if ("10".equals(fieldType)) {  // 单选
                                    sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                                } else if ("20".equals(fieldType)) {  // 多选
                                    sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                                } else if ("30".equals(fieldType)) {  // 时间
                                    sortBySql = sortBySql + String.format("JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) %s,", map.get("field"), map.get("sort"));
                                } else if ("80".equals(fieldType) || "90".equals(fieldType)) {  // 金额 数值
                                    sortBySql = sortBySql + String.format("CAST(SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) as DECIMAL(13,3)) %s,", map.get("field"), map.get("field"), map.get("sort"));
                                } else if ("40".equals(fieldType)) {  // 文本
                                    if ("number".equals(type)) {  // number
                                        sortBySql = sortBySql + String.format("CAST(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) as DECIMAL(13,3)) %s,", map.get("field"), map.get("sort"));
                                    } else {  // string
                                        sortBySql = sortBySql + String.format("CONVERT(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) USING gbk) %s,", map.get("field"), map.get("sort"));
                                    }
                                } else {
                                    // 其余类型不排序
//                            sortBySql = sortBySql + "";
                                }
                                break;
                            }
                        }
                    }
                    if (!" order by ".equals(sortBySql)) {
                        sortBySql = sortBySql.substring(0, sortBySql.length() - 1);
                    } else {
                        sortBySql = null;
                    }
                }
                // 不需要分页
                String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
                System.out.println("表编号为: " + hashMod);
                List<String> dmDataIdListInFile = splitTableMapper.getDmDataIdListInFileDao(hfDmTable.getKey(), sortBySql, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);

                String sql = "update " + dmContentDataDatabase + ".hf_dm_content_data" + hashMod + " \n" +
                        "set `order`=  case ";
                for (int i = 0; i < dmDataIdListInFile.size(); i++) {
                    sql = sql + " when table_id = '" + hfDmTable.getKey() + "' and file_uuid = '" + fileUuid + "'" + " and file_version_id = '" + fileVersionId + "'" + " and data_id = '" + dmDataIdListInFile.get(i) + "' then '" + String.valueOf(i) + "' ";
                }
                sql = sql + " end\n" +
                        "where 1=1 and table_id = '" + hfDmTable.getKey() + "' and file_uuid = '" + fileUuid + "'" + " and file_version_id = '" + fileVersionId + "'";
//                System.out.println("-------------");
//                System.out.println(sql);
//                System.out.println("-------------");
                splitTableMapper.orderDmDataInFileDao(sql);
                HashMap<String, Object> retMap = new HashMap<>();
                retMap.put("msg", "新表保存成功");
                retMap.put("tableId", hfDmTable.getKey());
                HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoInFileDao(hfDmTable.getKey(), fileUuid, fileVersionId);
                retMap.put("table", dmTableInfoDao);
                return TResponseVo.success(retMap);
            }
        } else {  // 上卷加过滤 结果保存成新表
            // 有group时默认不读取sort内容
            hfDmTable.setOverwrite(false);  // 防止group时出现
        }
        // 设置为不分页查询
        hfDmTable.setIsPaged("0");

        // 记录来源表id
        String fatherTableId = hfDmTable.getKey();
        String fatherFilter = null;
        String fatherGroup = null;
        // 获取筛选字段
        Object filterObj = hfDmTable.getFilter();
        if (filterObj != null && filterObj instanceof List) {
            fatherFilter = JSON.toJSONString(filterObj);
        }
        // 获取上卷聚合字段
        Map<String, Object> group = hfDmTable.getGroup();
        if (group != null && group.keySet().size() != 0) {
            fatherGroup = JSON.toJSONString(group);
        }

        // 查询结果
        TResponseVo dmTableContentService = getDmTableContentInFileService(hfDmTable);
        Map data = (Map) dmTableContentService.getData();
        List<Map<String, Object>> list = (List) data.get("list");  // data
        List<Map<String, Object>> columns = (List) data.get("columns");  // column
        String createUserId = hfDmTable.getCreateUserId();
        String tableName = hfDmTable.getTableName();
//        String fileUuid = hfDmTable.getFileUuid();
//        String fileVersionId = hfDmTable.getFileVersionId();
        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoInFileDao(hfDmTable.getKey(), fileUuid, fileVersionId);
        String dbId = dmTableInfoDao.getDbId();

        // 0.准备数据表
        String tableId = UUID.randomUUID().toString().replaceAll("-", "");

        ArrayList<HfDmColumns> hfDmColumnsList = new ArrayList<>();  // 表头入库
        Date createTime = new Date();
        for (int i = 0; i < columns.size(); i++) {  // CommonExcelModel的列数
            Map<String, Object> map = columns.get(i);
            String title = (String) map.get("title");
            String dataIndex = (String) map.get("dataIndex");
            HfDmColumns hfDmColumns = new HfDmColumns();
            hfDmColumns.setTableId(tableId);
            hfDmColumns.setKey(dataIndex);
            hfDmColumns.setTitle(title);
            hfDmColumns.setDataIndex(dataIndex);
            hfDmColumns.setFieldType((String) map.get("fieldType"));  // 先默认都是文本类型
            hfDmColumns.setOptions(map.get("options") == null ? null : JSON.toJSONString(map.get("options")));
            hfDmColumns.setUnit(map.get("unit") == null ? null : JSON.toJSONString(map.get("unit")));
            hfDmColumns.setCreateUserId(createUserId);
            hfDmColumns.setCreateTime(createTime);
            hfDmColumns.setOrder(String.valueOf(i));
            hfDmColumnsList.add(hfDmColumns);

        }

        // 1.2 list的其余行是dataSource
        ArrayList<HfDmData> hfDmDataList = new ArrayList<>();  // 数据入库
        if (list.size() != 0) {
            Integer order = 0;
            for (Map<String, Object> CommonExcelModelMap : list) {
                // 遍历每行数据
                HfDmData hfDmData = new HfDmData();
                hfDmData.setDataId(UUID.randomUUID().toString().replaceAll("-", ""));
                hfDmData.setTableId(tableId);
                hfDmData.setOrder(String.valueOf(order++));
                hfDmData.setCreateUserId(createUserId);
                hfDmData.setCreateTime(createTime);
                CommonExcelModelMap.remove("key");
                hfDmData.setDataContent(JSON.toJSONString(CommonExcelModelMap));
                hfDmDataList.add(hfDmData);
            }
        }

        // 2.数据入库
        // 创建表
        HfDmTable hfDmTable1 = new HfDmTable();
        hfDmTable1.setTableId(tableId);
        hfDmTable1.setTableName(tableName);
        hfDmTable1.setDbId(dbId);
        hfDmTable1.setTableDesc(hfDmTable.getDesc());
        hfDmTable1.setCreateUserId(createUserId);
        hfDmTable1.setFileUuid(fileUuid);
        hfDmTable1.setFileVersionId(fileVersionId);
        // 补充信息
        hfDmTable1.setDesc(hfDmTable.getDesc());
        hfDmTable1.setTitle(tableName);
        hfDmTable1.setType("table");
        hfDmTable1.setKey(tableId);
        if ("view".equals(saveType)) {  // 如果是保存成视图view
            hfDmTable1.setFatherTableId(fatherTableId);
            hfDmTable1.setFatherFilter(fatherFilter);
            hfDmTable1.setFatherGroup(fatherGroup);
        }
        workingTableMapper.createDmTableInFileDao(hfDmTable1);
        if (hfDmColumnsList.size() != 0) {
            // 创建表头
            workingTableMapper.uploadHfDmColumnsInFileDao(hfDmColumnsList, fileUuid, fileVersionId);
            if (hfDmDataList.size() != 0) {
                // 创建数据
                // 分批插入
                String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
                System.out.println("表编号为: " + hashMod);
                while (hfDmDataList.size() > 1000) {
                    List<HfDmData> hfDmData = hfDmDataList.subList(0, 1000);
                    splitTableMapper.uploadHfDmDataInFileDao(hfDmData, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
                    hfDmDataList.subList(0, 1000).clear();
                }

                splitTableMapper.uploadHfDmDataInFileDao(hfDmDataList, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            }
        }
        if ("view".equals(saveType)) {  // 如果是保存成视图view
            HashMap<String, Object> retMap = new HashMap<>();
            retMap.put("msg", "新表保存成功");
            retMap.put("viewId", tableId);
            hfDmTable1.setViewId(tableId);
            hfDmTable1.setTableId(null);
            hfDmTable1.setType("view");
            retMap.put("view", hfDmTable1);
            return TResponseVo.success(retMap);
        }
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("msg", "新表保存成功");
        retMap.put("tableId", tableId);
        retMap.put("table", hfDmTable1);
        return TResponseVo.success(retMap);

    }

    /**
     * 新增数据
     *
     * @param hfDmData
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo addDmDataService(HfDmData hfDmData) throws Exception {
        Object dataContent = hfDmData.getDataContent();
        if (dataContent == null || StringUtils.isBlank(String.valueOf(dataContent))) {
            throw new WorkTableException("数据内容不可为空");
        }
        hfDmData.setDataContent(JSON.toJSONString(dataContent));
        String tableId = hfDmData.getTableId();
        if (StringUtils.isBlank(tableId)) {
            throw new WorkTableException("数据未指定属表");
        }
        // 获取当前最大order
        Integer maxOrder = workingTableMapper.getNowDataOrderDao(tableId);
        if (maxOrder == null) {
            maxOrder = 0;
        } else {
            maxOrder = maxOrder + 1;
        }
        hfDmData.setOrder(String.valueOf(maxOrder));
        String key = UUID.randomUUID().toString().replaceAll("-", "");
        hfDmData.setDataId(key);
        workingTableMapper.addDmDataDao(hfDmData);

        HashMap<String, Object> map = new HashMap<>();
        map.put("msg", "新增完成");
        map.put("key", key);
        return TResponseVo.success(map);
    }

    @Override
    public TResponseVo addDmDataInFileService(HfDmData hfDmData) throws Exception {
        Object dataContent = hfDmData.getDataContent();
        if (dataContent == null || StringUtils.isBlank(String.valueOf(dataContent))) {
            throw new WorkTableException("数据内容不可为空");
        }
        // 查询主文件
        String fileUuid = hfDmData.getFileUuid();
        String fileVersionId = hfDmData.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            hfDmData.setFileUuid(fileUuid);
            hfDmData.setFileVersionId(fileVersionId);
        }

        hfDmData.setDataContent(JSON.toJSONString(dataContent));
        String tableId = hfDmData.getTableId();
        if (StringUtils.isBlank(tableId)) {
            throw new WorkTableException("数据未指定属表");
        }
        // 获取当前最大order
        String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
        System.out.println("表编号为: " + hashMod);
        Integer maxOrder = splitTableMapper.getNowDataOrderInFileDao(tableId, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
        if (maxOrder == null) {
            maxOrder = 0;
        } else {
            maxOrder = maxOrder + 1;
        }
        hfDmData.setOrder(String.valueOf(maxOrder));
        String key = UUID.randomUUID().toString().replaceAll("-", "");
        hfDmData.setDataId(key);
        hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
        System.out.println("表编号为: " + hashMod);
        splitTableMapper.addDmDataInFileDao(hfDmData, hashMod, dmContentDataDatabase);

        HashMap<String, Object> map = new HashMap<>();
        map.put("msg", "新增完成");
        map.put("key", key);
        return TResponseVo.success(map);
    }

    /**
     * 删除数据
     *
     * @param hfDmData
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delDmDataService(HfDmData hfDmData) throws Exception {
        String tableId = hfDmData.getTableId();
        String dataId = hfDmData.getDataId();
        if (StringUtils.isBlank(tableId) || StringUtils.isBlank(dataId)) {
            throw new WorkTableException("必填参数为空");
        }
        workingTableMapper.delDmDataDao(tableId, dataId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("msg", "删除完成");
        return TResponseVo.success(map);
    }

    @Override
    public TResponseVo delDmDataInFileService(HfDmData hfDmData) throws Exception {
        String tableId = hfDmData.getTableId();
        String dataId = hfDmData.getDataId();
        if (StringUtils.isBlank(tableId) || StringUtils.isBlank(dataId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询主文件
        String fileUuid = hfDmData.getFileUuid();
        String fileVersionId = hfDmData.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
        System.out.println("表编号为: " + hashMod);
        splitTableMapper.delDmDataInFileDao(tableId, dataId, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
        HashMap<String, Object> map = new HashMap<>();
        map.put("msg", "删除完成");
        return TResponseVo.success(map);

    }

    /**
     * 修改数据
     *
     * @param hfDmData
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo chgDmDataService(HfDmData hfDmData) throws Exception {
        String tableId = hfDmData.getTableId();
        String dataId = hfDmData.getDataId();
        if (StringUtils.isBlank(tableId) || StringUtils.isBlank(dataId)) {
            throw new WorkTableException("必填参数为空");
        }
        Object dataContent = hfDmData.getDataContent();
        if (dataContent == null || StringUtils.isBlank(String.valueOf(dataContent))) {
            throw new WorkTableException("数据内容不可为空");
        }

        hfDmData.setDataContent(JSON.toJSONString(dataContent));
        try {
            workingTableMapper.chgDmDataDao(hfDmData);
            // 重新计算行内的计算字段
            List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListDao(tableId);
            List<String> columnsIdList = dmTableColumnsListDao.stream().filter(k -> "formula".equals(k.getKind())).map(HfDmColumns::getKey).collect(Collectors.toList());
            dmTableTask.calculateThread(hfDmData.getTableId(), columnsIdList, dataId, hfDmData.getCreateUserId(), false);
        } catch (Exception e) {
            throw new WorkTableException("该条数据不存在或已删除");
        }
        return TResponseVo.success("修改完成");
    }

    @Override
    public TResponseVo chgDmDataInFileService(HfDmData hfDmData) throws Exception {
        String tableId = hfDmData.getTableId();
        String dataId = hfDmData.getDataId();
        if (StringUtils.isBlank(tableId) || StringUtils.isBlank(dataId)) {
            throw new WorkTableException("必填参数为空");
        }
        Object dataContent = hfDmData.getDataContent();
        if (dataContent == null || StringUtils.isBlank(String.valueOf(dataContent))) {
            throw new WorkTableException("数据内容不可为空");
        }
        // 查询主文件
        String fileUuid = hfDmData.getFileUuid();
        String fileVersionId = hfDmData.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
            hfDmData.setFileUuid(fileUuid);
            hfDmData.setFileVersionId(fileVersionId);
        }
        hfDmData.setDataContent(JSON.toJSONString(dataContent));
        try {
            String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
            System.out.println("表编号为: " + hashMod);
            splitTableMapper.chgDmDataInFileDao(hfDmData, hashMod, dmContentDataDatabase);
            // 重新计算行内的计算字段
            List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListInFileDao(tableId, fileUuid, fileVersionId);
            List<String> columnsIdList = dmTableColumnsListDao.stream().filter(k -> "formula".equals(k.getKind())).map(HfDmColumns::getKey).collect(Collectors.toList());
            dmTableTask.calculateInFileThread(hfDmData.getTableId(), columnsIdList, dataId, hfDmData.getCreateUserId(), fileUuid, fileVersionId, false);
        } catch (Exception e) {
            throw new WorkTableException("该条数据不存在或已删除");
        }
        return TResponseVo.success("修改完成");

    }

    /**
     * 获取数据内容清单
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getDmDataListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();
        String userId = pagePojo.getUserId();
        String tableId = pagePojo.getTableId();
        if (StringUtils.isBlank(tableId)) {
            throw new WorkTableException("必填参数为空");
        }

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<HfDmData> optionsListDao = new ArrayList<>();

        if (true || "0".equalsIgnoreCase(isPaged)) {
            optionsListDao = workingTableMapper.getDmDataListDao(userId, tableId, paramNameLike, null, null, null);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            optionsListDao = workingTableMapper.getDmDataListDao(userId, tableId, paramNameLike, null, null, null);
        }

        ArrayList<Map<String, Object>> list = new ArrayList<>();
        // 反序列化
        for (HfDmData hfDmData : optionsListDao) {
            Object dataContent = hfDmData.getDataContent();
            if (dataContent == null || StringUtils.isBlank(String.valueOf(dataContent))) {
                System.out.println(hfDmData.getDataId() + "数据格式有误");
            } else {
                Map<String, Object> dataContentMap = JSON.parseObject(String.valueOf(dataContent), Map.class);
                dataContentMap.put("key", hfDmData.getDataId());
                list.add(dataContentMap);
            }
        }
        return TResponseVo.success(list);
    }

    @Override
    public TResponseVo getDmDataListInFileService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();
        String userId = pagePojo.getUserId();
        String tableId = pagePojo.getTableId();
        if (StringUtils.isBlank(tableId)) {
            throw new WorkTableException("必填参数为空");
        }

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<HfDmData> optionsListDao = new ArrayList<>();
        String fileUuid = pagePojo.getFileUuid();
        String fileVersionId = pagePojo.getFileVersionId();
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        if (true || "0".equalsIgnoreCase(isPaged)) {
            String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
            System.out.println("表编号为: " + hashMod);
            optionsListDao = splitTableMapper.getDmDataListInFileDao(userId, tableId, paramNameLike, null, null, null, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
        } else {
            String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
            System.out.println("表编号为: " + hashMod);
            PageHelper.startPage(pageNum, pageSize);
            optionsListDao = splitTableMapper.getDmDataListInFileDao(userId, tableId, paramNameLike, null, null, null, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
        }

        ArrayList<Map<String, Object>> list = new ArrayList<>();
        // 反序列化
        for (HfDmData hfDmData : optionsListDao) {
            Object dataContent = hfDmData.getDataContent();
            if (dataContent == null || StringUtils.isBlank(String.valueOf(dataContent))) {
                System.out.println(hfDmData.getDataId() + "数据格式有误");
            } else {
                Map<String, Object> dataContentMap = JSON.parseObject(String.valueOf(dataContent), Map.class);
                dataContentMap.put("key", hfDmData.getDataId());
                list.add(dataContentMap);
            }
        }
        return TResponseVo.success(list);
    }

    /**
     * 获取表数据量
     *
     * @param key tableId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getDmDataTotalService(String userId, String key) throws Exception {
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("必填参数为空");
        }
        Integer total = workingTableMapper.getDmDataTotalDao(userId, key);
        return TResponseVo.success(total);
    }

    @Override
    public TResponseVo getDmDataTotalInFileService(String userId, String key, String fileUuid, String fileVersionId) throws Exception {
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
        System.out.println("表编号为: " + hashMod);
        Integer total = splitTableMapper.getDmDataTotalInFileDao(userId, key, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
        return TResponseVo.success(total);
    }

    /**
     * 字段排序
     *
     * @param hfDmColumns
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo orderDmTableColumnsService(HfDmColumns hfDmColumns) throws Exception {
        String tableId = hfDmColumns.getTableId();
        if (StringUtils.isBlank(tableId)) {
            throw new WorkTableException("未指定要排序的表");
        }
        // 是否分页
        Boolean showPagination = hfDmColumns.getShowPagination();
        if (showPagination != null) {
            workingTableMapper.chgPageStatusDao(tableId, showPagination);
        }
        // 是否缩略
        Boolean ellipsis = hfDmColumns.getEllipsis();
        if (ellipsis != null) {
            workingTableMapper.chgEllipsisStatusDao(tableId, ellipsis);
        }

        // 新顺序
        List<Map<String, Object>> orderList = hfDmColumns.getColumns();
        if (orderList != null) {  // 如果orderList不为空 再进行下列操作
            // 旧顺序
            List<HfDmColumns> dmColumnsDao = workingTableMapper.getDmTableColumnsListDao(tableId);
            // 记录发生变化的字段
            List<HfDmColumns> dmColumnsDaoTmp = new ArrayList<>();
            for (int i = 0; i < orderList.size(); i++) {
                for (HfDmColumns dmColumns : dmColumnsDao) {
                    if (orderList.get(i).get("key").equals(dmColumns.getKey())) {  // 顺序使用key 不使用dataIndex 从0开始
                        if (!dmColumns.getOrder().equals(String.valueOf(i)) || !dmColumns.getVisible().equals(orderList.get(i).get("visible"))) {  // 顺序字段发生变化的 才做修改 减少操作次数
                            dmColumns.setOrder(String.valueOf(i));
                            dmColumns.setVisible((Boolean) orderList.get(i).get("visible"));
                            dmColumnsDaoTmp.add(dmColumns);
                        }
                        break;
                    }
                }
            }
            // 找到顺序字段发生变化的字段 并更新
            for (HfDmColumns dmColumns : dmColumnsDaoTmp) {
                workingTableMapper.updateDmTableColumnsOrderDao(dmColumns);
            }
        }
        return TResponseVo.success("调整完成");
    }

    @Override
    public TResponseVo orderDmTableColumnsInFileService(HfDmColumns hfDmColumns) throws Exception {
        String tableId = hfDmColumns.getTableId();
        if (StringUtils.isBlank(tableId)) {
            throw new WorkTableException("未指定要排序的表");
        }
        // 查询主文件
        String fileUuid = hfDmColumns.getFileUuid();
        String fileVersionId = hfDmColumns.getFileVersionId();
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        // 是否分页
        Boolean showPagination = hfDmColumns.getShowPagination();
        if (showPagination != null) {
            workingTableMapper.chgPageStatusInFileDao(tableId, showPagination, fileUuid, fileVersionId);
        }
        // 是否缩略
        Boolean ellipsis = hfDmColumns.getEllipsis();
        if (ellipsis != null) {
            workingTableMapper.chgEllipsisStatusInFileDao(tableId, ellipsis, fileUuid, fileVersionId);
        }

        // 新顺序
        List<Map<String, Object>> orderList = hfDmColumns.getColumns();
        if (orderList != null) {  // 如果orderList不为空 再进行下列操作
            // 旧顺序
            List<HfDmColumns> dmColumnsDao = workingTableMapper.getDmTableColumnsListInFileDao(tableId, fileUuid, fileVersionId);
            // 记录发生变化的字段
            List<HfDmColumns> dmColumnsDaoTmp = new ArrayList<>();
            for (int i = 0; i < orderList.size(); i++) {
                for (HfDmColumns dmColumns : dmColumnsDao) {
                    if (orderList.get(i).get("key").equals(dmColumns.getKey())) {  // 顺序使用key 不使用dataIndex 从0开始
                        if (!dmColumns.getOrder().equals(String.valueOf(i)) || !dmColumns.getVisible().equals(orderList.get(i).get("visible"))) {  // 顺序字段发生变化的 才做修改 减少操作次数
                            dmColumns.setOrder(String.valueOf(i));
                            dmColumns.setVisible((Boolean) orderList.get(i).get("visible"));
                            dmColumnsDaoTmp.add(dmColumns);
                        }
                        break;
                    }
                }
            }
            // 找到顺序字段发生变化的字段 并更新
            for (HfDmColumns dmColumns : dmColumnsDaoTmp) {
                workingTableMapper.updateDmTableColumnsOrderInFileDao(dmColumns);
            }
        }
        return TResponseVo.success("调整完成");
    }


    /**
     * 上传excel至数据管理
     *
     * @param mulFile
     * @param k
     * @param desc
     * @param userId
     * @param type
     * @param excelPw
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo uploadCommonExcelService(MultipartFile mulFile, String k, String desc, String userId, String type, String excelPw) throws Exception {
        // 判断上传类型
        if (type == null || StringUtils.isBlank(type)) {
            throw new WorkTableException("未指定上传类型");
        }
        if ("create".equals(type)) {  // 新建
            if (uploadLock.get()) {
                uploadLock.set(false);
                try {
                    String dbId = k;
                    if (StringUtils.isBlank(dbId)) {
                        throw new WorkTableException("未指定上传库");
                    }
                    if (mulFile == null) {
                        throw new WorkTableException("上传文件为空");
                    }
                    HfDmDb dmDbInfoDao = workingTableMapper.getDmDbInfoDao(dbId);
                    if (dmDbInfoDao == null) {
                        throw new WorkTableException("指定数据库不存在或已删除");
                    }
                    // 0.准备数据表
                    String tableId = UUID.randomUUID().toString().replaceAll("-", "");
                    // 获取文件流
                    InputStream inputStream = mulFile.getInputStream();

                    // 本次上传任务的id
                    String taskId = UUID.randomUUID().toString().replaceAll("-", "");
                    UploadCommonExcelCreateThreadUtils uploadCommonExcelCreateThreadUtils = new UploadCommonExcelCreateThreadUtils(taskId, dbId, tableId, desc, userId, inputStream, excelPw, null, null);
                    Thread thread = new Thread(uploadCommonExcelCreateThreadUtils);
                    thread.start();

                    HashMap<String, String> retMap = new HashMap<>();
                    retMap.put("msg", "正在上传,请稍候");
                    retMap.put("tableId", tableId);
                    return TResponseVo.success(retMap);
                } catch (WorkTableException e) {
                    throw e;
                } catch (Exception e) {
                    throw new WorkTableException("数据导入失败");
                } finally {
                    uploadLock.set(true);
                }
            }
        } else if ("append".equals(type)) { // 追加
            if (uploadLock.get()) {
                uploadLock.set(false);
                try {
                    String tableId = k;
                    if (StringUtils.isBlank(tableId)) {
                        throw new WorkTableException("未指定上传表");
                    }
                    if (mulFile == null) {
                        throw new WorkTableException("上传文件为空");
                    }
                    // 获取文件流
                    InputStream inputStream = mulFile.getInputStream();

                    // 本次上传任务的id
                    String taskId = UUID.randomUUID().toString().replaceAll("-", "");
                    UploadCommonExcelAppendThreadUtils uploadCommonExcelAppendThreadUtils = new UploadCommonExcelAppendThreadUtils(taskId, null, tableId, desc, userId, inputStream, excelPw, null, null);
                    Thread thread = new Thread(uploadCommonExcelAppendThreadUtils);
                    thread.start();

                    HashMap<String, String> retMap = new HashMap<>();
                    retMap.put("msg", "正在上传,请稍候");
                    retMap.put("tableId", tableId);
                    return TResponseVo.success(retMap);
                } catch (WorkTableException e) {
                    throw e;
                } catch (Exception e) {
                    throw new WorkTableException("数据导入失败");
                } finally {
                    uploadLock.set(true);
                }
            }

        } else {
            throw new WorkTableException("未知的上传类型");
        }
        throw new WorkTableException("上传通道占用,请稍候重试");
    }

    @Override
    public TResponseVo uploadCommonExcelInFileService(MultipartFile mulFile, String k, String desc, String userId, String type, String excelPw, String fileUuid, String fileVersionId) throws Exception {
        // 判断上传类型
        if (type == null || StringUtils.isBlank(type)) {
            throw new WorkTableException("未指定上传类型");
        }
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        if ("create".equals(type)) {  // 新建
            if (uploadLock.get()) {
                uploadLock.set(false);
                try {
                    String dbId = k;
                    if (StringUtils.isBlank(dbId)) {
                        throw new WorkTableException("未指定上传库");
                    }
                    if (mulFile == null) {
                        throw new WorkTableException("上传文件为空");
                    }
                    HfDmDb dmDbInfoDao = workingTableMapper.getDmDbInfoInFileDao(dbId, fileUuid, fileVersionId);
                    if (dmDbInfoDao == null) {
                        throw new WorkTableException("指定数据库不存在或已删除");
                    }

                    // 0.准备数据表
                    String tableId = UUID.randomUUID().toString().replaceAll("-", "");
                    // 获取文件流
                    InputStream inputStream = mulFile.getInputStream();

                    // 本次上传任务的id
                    String taskId = UUID.randomUUID().toString().replaceAll("-", "");
                    UploadCommonExcelCreateThreadUtils uploadCommonExcelCreateThreadUtils = new UploadCommonExcelCreateThreadUtils(taskId, dbId, tableId, desc, userId, inputStream, excelPw, fileUuid, fileVersionId);
                    Thread thread = new Thread(uploadCommonExcelCreateThreadUtils);
                    thread.start();

                    HashMap<String, String> retMap = new HashMap<>();
                    retMap.put("msg", "正在上传,请稍候");
                    retMap.put("tableId", tableId);
                    return TResponseVo.success(retMap);
                } catch (WorkTableException e) {
                    throw e;
                } catch (Exception e) {
                    throw new WorkTableException("数据导入失败");
                } finally {
                    uploadLock.set(true);
                }
            }
        } else if ("append".equals(type)) { // 追加
            if (uploadLock.get()) {
                uploadLock.set(false);
                try {
                    String tableId = k;
                    if (StringUtils.isBlank(tableId)) {
                        throw new WorkTableException("未指定上传表");
                    }
                    if (mulFile == null) {
                        throw new WorkTableException("上传文件为空");
                    }
                    // 获取文件流
                    InputStream inputStream = mulFile.getInputStream();

                    // 本次上传任务的id
                    String taskId = UUID.randomUUID().toString().replaceAll("-", "");
                    UploadCommonExcelAppendThreadUtils uploadCommonExcelAppendThreadUtils = new UploadCommonExcelAppendThreadUtils(taskId, null, tableId, desc, userId, inputStream, excelPw, fileUuid, fileVersionId);
                    Thread thread = new Thread(uploadCommonExcelAppendThreadUtils);
                    thread.start();

                    HashMap<String, String> retMap = new HashMap<>();
                    retMap.put("msg", "正在上传,请稍候");
                    retMap.put("tableId", tableId);
                    return TResponseVo.success(retMap);
                } catch (WorkTableException e) {
                    throw e;
                } catch (Exception e) {
                    throw new WorkTableException("数据导入失败");
                } finally {
                    uploadLock.set(true);
                }
            }

        } else {
            throw new WorkTableException("未知的上传类型");
        }
        throw new WorkTableException("上传通道占用,请稍候重试");
    }

    /**
     * 数据管理下载excel
     *
     * @param key
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo downloadDmDataService(String key, String userId, HttpServletResponse response) throws Exception {
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("未指定下载的数据表");
        }

        String tableId = key;
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String path = downloadPath + "/" + uuid;

        MyDownloadThreadUtils myDownloadThreadUtils = new MyDownloadThreadUtils(uuid, tableId, path, userId, null, null);
        Thread task = new Thread(myDownloadThreadUtils);
        task.start();
        return TResponseVo.success("后台下载中,请稍候至下载中心查看");
    }

    @Override
    public TResponseVo downloadDmDataInFileService(String key, String fileUuid, String fileVersionId, String userId, HttpServletResponse response) throws Exception {
        if (StringUtils.isBlank(key)) {
            throw new WorkTableException("未指定下载的数据表");
        }
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        String tableId = key;
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String path = downloadPath + "/" + uuid;

        MyDownloadThreadUtils myDownloadThreadUtils = new MyDownloadThreadUtils(uuid, tableId, path, userId, fileUuid, fileVersionId);
        Thread task = new Thread(myDownloadThreadUtils);
        task.start();
        return TResponseVo.success("后台下载中,请稍候至下载中心查看");
    }

    /**
     * 从数据管理导入数据(文件内)
     *
     * @param tableId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo importDmDataInFileService(String databaseId, String tableId, String desc, String userId, String fileUuid, String fileVersionId) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }
        // 随机一个新tableId
        String newTableId = UUID.randomUUID().toString().replaceAll("-", "");

        // 本次上传任务的id
        String taskId = UUID.randomUUID().toString().replaceAll("-", "");
        // 通知前端
        WebSocketServer webSocketServer = new WebSocketServer();
        long start = System.currentTimeMillis();
        HashMap<String, Object> webSocketMap = new HashMap<>();
        String msg = "开始导入";
        System.out.println(msg);
        webSocketMap.put("msg", msg);
        webSocketMap.put("title", "数据导入");
        webSocketMap.put("taskId", taskId);
        webSocketMap.put("tableId", newTableId);
        webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
        webSocketMap.put("rate", 20);
        webSocketMap.put("action", "create");
        WebSocketServer.sendInfo(webSocketMap, userId + "_" + fileUuid + "_" + fileVersionId);

        // 找出带关联关系的数据表和模板的columnsId
        List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableMatchColumnsListDao(tableId);
        Map<String, List<String>> dbColumnsMap = new HashMap<>();  // 记录使用了哪些数据库中的哪些表名
//        Map<String, List<String>> folderColumnsMap = new HashMap<>();  // 记录使用了哪些模板库中的哪些模板名
        dmTableColumnsListDao.forEach(k -> {
            if ("table".equals(k.getMatchType())) {
                if (dbColumnsMap.keySet().contains(k.getMatchDbId())) {
                    if (!dbColumnsMap.get(k.getMatchDbId()).contains(k.getDataIndex())) {  // 重复的columnsId就不要了
                        dbColumnsMap.get(k.getMatchDbId()).add(k.getDataIndex());
                    }
                } else {
                    List<String> titleList = new ArrayList<>();
                    titleList.add(k.getDataIndex());
                    dbColumnsMap.put(k.getMatchDbId(), titleList);
                }
            }
        });

        // 数据准备
        // 获取用到的数据库中数据表的名称
        HashMap<String, List<String>> dbTableNameMap = new HashMap<>();
        for (String dbId : dbColumnsMap.keySet()) {
            List<LabelValuePojo> dmTableListLVByDbIdDao = workingTableMapper.getDmTableListLVByDbIdDao(dbId);
            dbTableNameMap.put(dbId, dmTableListLVByDbIdDao.stream().map(LabelValuePojo::getLabel).collect(Collectors.toList()));
        }

        List<String> dmTableAllDataDao = workingTableMapper.getDmTableAllDataDao(tableId);
        // 定义map dbId:[tableName]
        HashMap<String, List<String>> dbTargetTableNameMap = new HashMap<>();
//        new ArrayList<String>()
        for (String dataContent : dmTableAllDataDao) {
            Map<String, String> dataContentMap = JSON.parseObject(dataContent, Map.class);
            for (String dbId : dbColumnsMap.keySet()) {
                List<String> tableNameList = dbTableNameMap.get(dbId);
                for (String dataIndexId : dbColumnsMap.get(dbId)) {
                    String cellText = dataContentMap.get(dataIndexId);
                    if (tableNameList.contains(cellText)) {
                        if (dbTargetTableNameMap.keySet().contains(dbId)) {
                            if (!dbTargetTableNameMap.get(dbId).contains(cellText)) {
                                dbTargetTableNameMap.get(dbId).add(cellText);
                            }
                        } else {
                            List<String> targetTableNameList = new ArrayList<>();
                            targetTableNameList.add(cellText);
                            dbTargetTableNameMap.put(dbId, targetTableNameList);
                        }
                    }
                }
            }
        }

        // 根据dbColumnsMap找到要同步导入文内数据管理的数据表 同时要将文内已经存在的过滤掉
        List<String> dmDistinctSourceTableId = workingTableMapper.getDmSourceTableIdDistinctDao(databaseId);  // 获取已经导入过的数据表
        HashMap<String, List<String>> relationDmTableIdMap = new HashMap<>();
        for (String dbId : dbTargetTableNameMap.keySet()) {
            List<HfDmTable> relationDmTable = workingTableMapper.getRelationDmTableDao(dbId, dbTargetTableNameMap.get(dbId));// 根据dbId和title的list 找到tableId
            // relationDmTableIdDao中相同表名只保留第一个tableId
            List<String> relationDmTableIdList = new ArrayList<>();
            List<String> relationDmTableNameList = new ArrayList<>();
            for (HfDmTable hfDmTable : relationDmTable) {
                if (!relationDmTableNameList.contains(hfDmTable.getTableName())) {
                    relationDmTableNameList.add(hfDmTable.getTableName());
                    relationDmTableIdList.add(hfDmTable.getTableId());
                }
            }
            // 过滤掉导入过已存在的表
            relationDmTableIdList.removeIf(dmDistinctSourceTableId::contains);
            relationDmTableIdMap.put(dbId, relationDmTableIdList);
        }

        for (String dbId : relationDmTableIdMap.keySet()) {
            for (String relationTableId : relationDmTableIdMap.get(dbId)) {  // 关联表的id
                // 随机一个新tableId
                String newRelationTableId = UUID.randomUUID().toString().replaceAll("-", "");
                // 0.导入表索引
                workingTableMapper.importDmTableDao(databaseId, relationTableId, null, userId, fileUuid, fileVersionId, newRelationTableId, null);
                // 1.导入表头
                workingTableMapper.importDmColumnsDao(relationTableId, userId, fileUuid, fileVersionId, newRelationTableId);
                // 2.导入数据
                String hashMod = HashUtils.getHashMod(fileUuid + fileVersionId);
                System.out.println("表编号为: " + hashMod);
                splitTableMapper.importDmDataDao(relationTableId, userId, fileUuid, fileVersionId, newRelationTableId, hashMod, dmContentDataDatabase);
            }
        }
        // 0.导入表索引
        workingTableMapper.importDmTableDao(databaseId, tableId, desc, userId, fileUuid, fileVersionId, newTableId, null);
        // 1.导入表头
        workingTableMapper.importDmColumnsResetMatchDao(tableId, userId, fileUuid, fileVersionId, newTableId, databaseId);
        // 2.导入数据
        String hashMod = HashUtils.getHashMod(fileUuid + fileVersionId);
        System.out.println("表编号为: " + hashMod);
        splitTableMapper.importDmDataDao(tableId, userId, fileUuid, fileVersionId, newTableId, hashMod, dmContentDataDatabase);


        // 获取该表的视图
        List<String> dmViewList = workingTableMapper.getDmViewListDao(tableId);
        int i = 0;
        for (String viewId : dmViewList) {
            msg = "数据表导入完成,正在导入视图（" + i++ + "/" + dmViewList.size() + "）";
            System.out.println(msg);
            webSocketMap.put("msg", msg);
            webSocketMap.put("title", "数据导入");
            webSocketMap.put("taskId", taskId);
            webSocketMap.put("tableId", newTableId);
            webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
            webSocketMap.put("rate", 40 + i * 40 / dmViewList.size());
            webSocketMap.put("action", "create");
            WebSocketServer.sendInfo(webSocketMap, userId + "_" + fileUuid + "_" + fileVersionId);

            // 随机一个新tableId
            String newViewId = UUID.randomUUID().toString().replaceAll("-", "");
            // 0.导入表索引
            workingTableMapper.importDmTableDao(databaseId, viewId, null, userId, fileUuid, fileVersionId, newViewId, newTableId);
            // 1.导入表头
            workingTableMapper.importDmColumnsDao(viewId, userId, fileUuid, fileVersionId, newViewId);
            // 2.导入数据
            hashMod = HashUtils.getHashMod(fileUuid + fileVersionId);
            System.out.println("表编号为: " + hashMod);
            splitTableMapper.importDmDataDao(viewId, userId, fileUuid, fileVersionId, newViewId, hashMod, dmContentDataDatabase);
        }


        msg = "导入完成";
        System.out.println(msg);
        webSocketMap.put("msg", msg);
        webSocketMap.put("title", "数据导入");
        webSocketMap.put("taskId", taskId);
        webSocketMap.put("tableId", newTableId);
        webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
        webSocketMap.put("rate", 100);
        webSocketMap.put("action", "create");
        WebSocketServer.sendInfo(webSocketMap, userId + "_" + fileUuid + "_" + fileVersionId);

        return TResponseVo.success("数据导入完成");
    }


    /**
     * 从数据管理单纯导入数据 包含表头 包含data
     *
     * @param toTableId     目标表tableId
     * @param fromTableId   来源表tableId
     * @param overwrite     是否覆盖数据
     * @param userId        当前用户
     * @param fileUuid      目标文件
     * @param fileVersionId 目标版本
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo importOnlyDataService(String toTableId, String fromTableId, Boolean overwrite, String userId, String fileUuid, String fileVersionId) throws Exception {
        if (fileUuid == null || fileVersionId == null || toTableId == null || fromTableId == null) {
            throw new WorkTableException("必填参数为空");
        }

        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoInFileDao(toTableId, fileUuid, fileVersionId);
        if (dmTableInfoDao == null) {
            throw new WorkTableException("目标表不存在或已删除");
        }
        String databaseId = dmTableInfoDao.getDbId();

//        // 随机一个新tableId
//        String newTableId = UUID.randomUUID().toString().replaceAll("-", "");
//        // 0.导入表索引
//        workingTableMapper.importDmTableDao(databaseId, tableId, userId, fileUuid, fileVersionId, newTableId);
//        // 1.导入表头
//        workingTableMapper.importDmColumnsDao(tableId, userId, fileUuid, fileVersionId, newTableId);
        // 2.导入数据
        String hashMod = HashUtils.getHashMod(fileUuid + fileVersionId);
        System.out.println("表编号为: " + hashMod);
        if (true) {
            overwrite = true;
        }
        if (overwrite) {
            // 先清空目标表
            workingTableMapper.clearTableColumnsDao(toTableId, fileUuid, fileVersionId);
            splitTableMapper.clearTableDataDao(toTableId, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            // 然后清空视图
            // 获取该表的视图
            List<String> dmViewList = workingTableMapper.getDmViewListInFileDao(toTableId, fileUuid, fileVersionId);
            int i = 0;
            for (String viewId : dmViewList) {
                workingTableMapper.delDmTableInFileDao(viewId, fileUuid, fileVersionId);
                workingTableMapper.clearTableColumnsDao(viewId, fileUuid, fileVersionId);
                splitTableMapper.clearTableDataDao(viewId, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            }
        }

        // 插入新表的数据和表头
        workingTableMapper.importDmColumnsDao(fromTableId, userId, fileUuid, fileVersionId, toTableId);
        splitTableMapper.importOnlyDataDao(fromTableId, toTableId, userId, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
        // 获取该表的视图
        List<String> dmViewList = workingTableMapper.getDmViewListDao(fromTableId);
        int i = 0;
        for (String viewId : dmViewList) {
            // 随机一个新tableId
            String newViewId = UUID.randomUUID().toString().replaceAll("-", "");
            // 0.导入表索引
            workingTableMapper.importDmTableDao(databaseId, viewId, null, userId, fileUuid, fileVersionId, newViewId, toTableId);
            // 1.导入表头
            workingTableMapper.importDmColumnsDao(viewId, userId, fileUuid, fileVersionId, newViewId);
            // 2.导入数据
            hashMod = HashUtils.getHashMod(fileUuid + fileVersionId);
            System.out.println("表编号为: " + hashMod);
            splitTableMapper.importDmDataDao(viewId, userId, fileUuid, fileVersionId, newViewId, hashMod, dmContentDataDatabase);
        }


        return TResponseVo.success("数据导入成功");
    }

    /**
     * 数据管理填写到附件参数,并形成excel文件
     *
     * @param tableKey
     * @param fields
     * @param rows
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo uploadAnnexParamByDmService(List<String> tableKey, List<String> fields, List<String> rows, String remark, String userId, String fileUuid, String fileVersionId) throws Exception {
        if (tableKey == null || fields == null) {
            throw new WorkTableException("必填参数为空");
        }
        if (fields.size() == 0) {
            throw new WorkTableException("未选择数据表的列信息");
        }
        String dbId = null;
        String tableId = null;
        String lastId = null;
        try {
            dbId = tableKey.get(0);
            tableId = tableKey.get(1);
            lastId = tableKey.get(tableKey.size() - 1);
        } catch (Exception e) {
            throw new WorkTableException("未正确指定数据表或视图");
        }
        if (dbId == null || tableId == null || lastId == null) {
            throw new WorkTableException("未正确指定数据表或视图");
        }
//        List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListDao(lastId);

        String key = lastId;

        HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoInFileDao(key, fileUuid, fileVersionId);
        String tableName = dmTableInfoDao.getTableName();

        String uid = UUID.randomUUID().toString().replaceAll("-", ""); // 参数uid 与填参逻辑保持一致
        UploadAnnexParamByDmThreadUtils uploadAnnexParamByDmThreadUtils = new UploadAnnexParamByDmThreadUtils(uid, lastId, tableName, fields, rows, annexPath, remark, fileUuid, fileVersionId);
        Thread thread = new Thread(uploadAnnexParamByDmThreadUtils);
        thread.start();

        HashMap<String, Object> retMap = new HashMap<>();
        HashMap<String, String> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", tableName + ".xls");
        map.put("url", annexPath + "/" + uid + "/" + tableName + ".xls");
        retMap.put("paramsText", map);
        retMap.put("info", "保存成功");
        return TResponseVo.success(retMap);
    }


    /**
     * 获取数据管理中引用的相关模板清单
     *
     * @param fileUuid      目标文件
     * @param fileVersionId 目标版本
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getRelatModelByDmService(String fileUuid, String fileVersionId) throws Exception {
        // 查询主文件
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao.getMainFileUuid() != null) {
            fileUuid = docAllInfoDao.getMainFileUuid();
            fileVersionId = docAllInfoDao.getMainFileVersionId();
        }

        // 找到文件中所有的导入表(source_table_id不为空)
        List<String> importTableIdList = workingTableMapper.getImportTableIdListDao(fileUuid, fileVersionId);
        if (importTableIdList.size() == 0) {
            return TResponseVo.success(new ArrayList<LabelValuePojo>());
        }
        // 获取上述表中带有模板库关联关系的表头
        List<HfDmColumns> importTableMatchColumns = workingTableMapper.getImportTableMatchColumnsDao(importTableIdList, fileUuid, fileVersionId);
        // 把importTableIdList中没有模板库关联关系的表剔除 只保留有的
        importTableIdList = importTableMatchColumns.stream().map(HfDmColumns::getTableId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        // 找到使用的模板库id并去重
        List<String> matchDistFolderId = importTableMatchColumns.stream().map(HfDmColumns::getMatchFolderId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        // 找到模板库中的文件名map
        Map<String, List<String>> folderIdModelNameMap = new HashMap<>();
        Map<String, List<LabelValuePojo>> folderIdModelMap = new HashMap<>();
        for (String folderId : matchDistFolderId) {
            List<LabelValuePojo> dmModelListLVByDbIdDao = editToolMapper.getDocumentListLVDao(folderId);
            // 上传模板
            List<LabelValuePojo> uploadModelFileListLVDao = editToolMapper.getUploadModelFileListLVDao(folderId);
            dmModelListLVByDbIdDao.addAll(uploadModelFileListLVDao);
            folderIdModelNameMap.put(folderId, dmModelListLVByDbIdDao.stream().map(LabelValuePojo::getLabel).distinct().collect(Collectors.toList()));
            folderIdModelMap.put(folderId, dmModelListLVByDbIdDao);
        }


        HashMap<String, List<String>> resultMap = new HashMap<>(); // 记录实际用到了哪些模板
//        HashMap<String, List<LabelValuePojo>> resultLVMap = new HashMap<>(); // 记录实际用到了哪些模板
        List<LabelValuePojo> resultLVList = new ArrayList<>();
        try {
            // 找到目标表们的数据
            HashMap<String, List<String>> tableId2DataMap = new HashMap<>();
            String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
            System.out.println("表编号为: " + hashMod);
            List<HfDmData> dmTableAllDataInFileDao = splitTableMapper.getDmTableAllDataInFileDao(importTableIdList, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
            for (HfDmData hfDmData : dmTableAllDataInFileDao) {
                if (tableId2DataMap.keySet().contains(hfDmData.getTableId())) {
                    tableId2DataMap.get(hfDmData.getTableId()).add(String.valueOf(hfDmData.getDataContent()));
                } else {
                    ArrayList<String> strings = new ArrayList<>();
                    strings.add(String.valueOf(hfDmData.getDataContent()));
                    tableId2DataMap.put(hfDmData.getTableId(), strings);
                }
            }

            for (String tableId : tableId2DataMap.keySet()) {
                for (String dataContent : tableId2DataMap.get(tableId)) {
                    Map<String, String> dataContentMap = JSON.parseObject(dataContent, Map.class);
                    for (HfDmColumns hfDmColumns : importTableMatchColumns) {
                        if (hfDmColumns.getTableId().equals(tableId)) {
                            String cellText = dataContentMap.get(hfDmColumns.getDataIndex());
                            if (hfDmColumns.getMatchFolderId() != null
                                    && folderIdModelNameMap.get(hfDmColumns.getMatchFolderId()) != null
                                    && folderIdModelNameMap.get(hfDmColumns.getMatchFolderId()).contains(cellText)) {
                                if (resultMap.keySet().contains(hfDmColumns.getMatchFolderId())) {
                                    if (!resultMap.get(hfDmColumns.getMatchFolderId()).contains(cellText)) {
                                        resultMap.get(hfDmColumns.getMatchFolderId()).add(cellText);
                                        for (LabelValuePojo labelValuePojo : folderIdModelMap.get(hfDmColumns.getMatchFolderId())) {
                                            if (cellText.equals(labelValuePojo.getLabel())) {
//                                                resultLVMap.get(hfDmColumns.getMatchFolderId()).add(labelValuePojo);
                                                resultLVList.add(labelValuePojo);
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    ArrayList<String> titleList = new ArrayList<>();
                                    titleList.add(cellText);
                                    resultMap.put(hfDmColumns.getMatchFolderId(), titleList);
//                                    ArrayList<LabelValuePojo> titleLVList = new ArrayList<>();
                                    for (LabelValuePojo labelValuePojo : folderIdModelMap.get(hfDmColumns.getMatchFolderId())) {
                                        if (cellText.equals(labelValuePojo.getLabel())) {
//                                            titleLVList.add(labelValuePojo);
//                                            resultLVMap.put(hfDmColumns.getMatchFolderId(), titleLVList);
                                            resultLVList.add(labelValuePojo);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("getRelatModelByDmService异常");
            e.printStackTrace();
        }

        return TResponseVo.success(resultLVList);
    }

    /**
     * 数据管理关联的模板,填写到附件参数,并形成word文件
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo uploadAnnexParamByRelatModelService(String html, String userId,String type, String fileUuid, String fileVersionId, String ffid) throws Exception {
        if ("hfDoc".equals(type)) {
            if (fileUuid == null) {
                throw new WorkTableException("必填参数为空");
            }

            DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
            String fileStatus = docAllInfoDao.getFileStatus();
            if ("3".equals(fileStatus)) {
                throw new WorkTableException("该文档已被删除，无法下载");
            }

            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String fileName = docAllInfoDao.getFileName();
            String uid = UUID.randomUUID().toString().replaceAll("-", ""); // 参数uid 与填参逻辑保持一致
            String path = annexPath + "/" + uid;
            Html2DocUtils T1 = new Html2DocUtils(uuid, fileUuid, fileVersionId, fileName, html, path, userId, null, false,ffid);
            Thread task = new Thread(T1);
            task.start();

            HashMap<String, Object> retMap = new HashMap<>();
            HashMap<String, String> map = new HashMap<>();
            map.put("uid", uid);
            map.put("name", fileName + ".docx");
            map.put("url", annexPath + "/" + uid + "/" + fileName + ".docx");
            retMap.put("paramsText", map);
            retMap.put("info", "保存成功");
            return TResponseVo.success(retMap);
        } else if ("uploadModel".equals(type)){
            // 将上传模板作为附件上传到参数内
            if (fileUuid == null) {
                throw new WorkTableException("必填参数为空");
            }

            DocFileIndexPojo docAllInfoDao = editToolMapper.getUploadModelFileInfoDao(fileUuid);
            if (docAllInfoDao == null || docAllInfoDao.getIsDel()) {
                throw new WorkTableException("该文档已被删除，无法下载");
            }
            String fileName = docAllInfoDao.getFileName();
            String extension = docAllInfoDao.getExtension();
            String uid = UUID.randomUUID().toString().replaceAll("-", ""); // 参数uid 与填参逻辑保持一致
            HashMap<String, Object> retMap = new HashMap<>();
            HashMap<String, String> map = new HashMap<>();
            map.put("uid", uid);
            map.put("name", fileName + extension);
            map.put("url", docAllInfoDao.getFilePath());
            retMap.put("paramsText", map);
            retMap.put("info", "保存成功");
            return TResponseVo.success(retMap);
        } else {
            throw new WorkTableException("未获取到附件类型");
        }
    }

    /**
     * 填写文件时获取表格全选的keys
     *
     * @param hfDmTable
     */
    @Override
    public TResponseVo getTotalKeysService(HfDmTable hfDmTable) throws Exception {
        String fileUuid = hfDmTable.getFileUuid();
        String fileVersionId = hfDmTable.getFileVersionId();
        String key = hfDmTable.getKey();
        String tableId = key;
        List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListInFileDao(key, fileUuid, fileVersionId);

        // 1.复杂筛选
        Object filterObj = hfDmTable.getFilter();
        String sql = null;  // 拼装查询语句
        if (filterObj != null && filterObj instanceof List) {
            List<Map<String, Object>> filterList = (List) hfDmTable.getFilter();
            List<HfDmTable> filter = new ArrayList<>();
            for (Map<String, Object> map : filterList) {
                filter.add(JSON.parseObject(JSON.toJSONString(map), HfDmTable.class));
            }

            // 确定查询条件是与还是或
            String qType = "and"; // 查询类型默认and
            for (HfDmTable dmTable : filter) {
                if (dmTable.getType() != null) {
                    qType = dmTable.getType();  // and or
                    break;
                }
            }
            sql = "";
            for (HfDmTable f : filter) {
                if (f.getField() != null) {  // 防止[{}]
                    // 判断筛选条件的字段类型
                    for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                        if (f.getField().equals(hfDmColumns.getDataIndex())) {
                            String fieldType = hfDmColumns.getFieldType();  // 获取到类型
                            // 根据类型组装查询语句
                            if ("10".equals(fieldType)) {  // 单选
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not".equals(f.getCondition())) {  // 不是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '')", qType, f.getField(), f.getField());
                                }
                            } else if ("20".equals(fieldType)) {  // 多选
                                // condition条件类型
                                if ("in".equals(f.getCondition())) {  // 包含
                                    sql = sql + String.format("%s (json_contains(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')), '%s'))", qType, f.getField(), JSON.toJSONString(f.getValue()).replaceAll("\\\"", "\\\\\"").replaceAll("\\\'", "\\\\\'"));
                                } else if ("not in".equals(f.getCondition())) {  // 不包含
                                    sql = sql + String.format("%s (!json_contains(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')), '%s'))", qType, f.getField(), JSON.toJSONString(f.getValue()).replaceAll("\\\"", "\\\\\"").replaceAll("\\\'", "\\\\\'"));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '[]')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '[]')", qType, f.getField(), f.getField());
                                }
                            } else if ("30".equals(fieldType)) {  // 时间
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not".equals(f.getCondition())) {  // 不等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gt".equals(f.getCondition())) {  // 大于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) > \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gte".equals(f.getCondition())) {  // 大于等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) >= \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lt".equals(f.getCondition())) {  // 小于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) < \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lte".equals(f.getCondition())) {  // 小于等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) <= \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '')", qType, f.getField(), f.getField());
                                }
                            } else if ("80".equals(fieldType) || "90".equals(fieldType)) {  // 金额 数值
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 等于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) = %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not".equals(f.getCondition())) {  // 不等于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) != %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gt".equals(f.getCondition())) {  // 大于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) > %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("gte".equals(f.getCondition())) {  // 大于等于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) >= %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lt".equals(f.getCondition())) {  // 小于
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) < %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("lte".equals(f.getCondition())) {  // 小于等于
//                                sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) <= \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"","\\\\\""));
                                    sql = sql + String.format("%s (SUBSTR(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')),2,LENGTH(JSON_UNQUOTE(json_extract(data_content,'$.\"%s\".*')))-2) <= %s)", qType, f.getField(), f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '{}')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '{}')", qType, f.getField(), f.getField());
                                }
                            } else if ("40".equals(fieldType) || "50".equals(fieldType)) {  // 文本 富文本
                                // condition条件类型
                                if ("is".equals(f.getCondition())) {  // 等于
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = \"%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("like".equals(f.getCondition())) {  // 包含
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) like \"%%%s%%\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("not like".equals(f.getCondition())) {  // 不包含
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) not like \"%%%s%%\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("start with".equals(f.getCondition())) {  // 开头是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) like \"%s%%\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("end with".equals(f.getCondition())) {  // 结尾是
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) like \"%%%s\")", qType, f.getField(), String.valueOf(f.getValue()).replaceAll("\\\"", "\\\\\""));
                                } else if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '')", qType, f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '')", qType, f.getField(), f.getField());
                                }
                            } else {
                                // condition条件类型
                                if ("is null".equals(f.getCondition())) {  // 为空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is null or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '' or JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) = '[]')", qType, f.getField(), f.getField(), f.getField());
                                } else if ("not null".equals(f.getCondition())) {  // 非空
                                    sql = sql + String.format("%s (JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) is not null and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '' and JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) != '[]')", qType, f.getField(), f.getField(), f.getField());
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if (sql != null && sql.length() > 3) {
                sql = sql.substring(3);
            } else {
                sql = null;
            }

        }

        List<String> totalKeys = new ArrayList<>();
        String hashMod = HashUtils.getHashMod(fileUuid + fileVersionId);
        System.out.println("表编号为: " + hashMod);
        totalKeys = splitTableMapper.getTotalKeysDao(null, key, null, sql, null, null, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);

        return TResponseVo.success(totalKeys);
    }

    /**
     * 表left join操作
     *
     * @param leftTableId   左表id
     * @param rightTableId  右表id
     * @param columnsIdList 关联字段id
     * @param tableName     新表名
     * @param dbId          表位置
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo tableLeftJoinService(String leftTableId, String rightTableId, List<Map<String, String>> columnsIdList, String tableName, String dbId, String userId) throws Exception {


        // 生成table信息
        String tableId = UUID.randomUUID().toString().replaceAll("-", "");


        // 生成data信息
        String leftSql = "";
        String rightSql = "";
        String onSql = "";
        // 目标sql
//        select JSON_UNQUOTE(JSON_MERGE_PATCH(ta.data_content,IFNULL(tb.data_content,'{}'))) as data_content
//        from (
//                select data_content,JSON_UNQUOTE(json_extract(data_content,'$."dd28579c-a0f3-4b4a-af9d-61504e30fc68"')) `dd28579c-a0f3-4b4a-af9d-61504e30fc68`
//                from hf_dm_data
//                where table_id = '916dcb5ad2b04113b6feaee7dfc490c8'
//        ) ta
//        left join (
//                select data_content,JSON_UNQUOTE(json_extract(data_content,'$."c139f431-63ca-43c1-9cd6-0279ab1e1001"')) `c139f431-63ca-43c1-9cd6-0279ab1e1001`
//                from hf_dm_data
//                where table_id = '7be86a5626da45a5b0a453656d3517d0'
//        ) tb
//        on ta.`dd28579c-a0f3-4b4a-af9d-61504e30fc68` = tb.`c139f431-63ca-43c1-9cd6-0279ab1e1001`
        List<String> checkUniqueSqlList = new ArrayList<>(); // 记录校验唯一性sql
        for (Map<String, String> map : columnsIdList) {
            String key1 = map.get("leftKey");
            String key2 = map.get("rightKey");
            String randomKey1 = UUID.randomUUID().toString().replaceAll("-", "");
            String randomKey2 = UUID.randomUUID().toString().replaceAll("-", "");
            leftSql = leftSql + String.format(",JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) `%s`", key1, randomKey1);
            rightSql = rightSql + String.format(",JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) `%s`", key2, randomKey2);
            onSql = onSql + String.format(" ta.`%s` = tb.`%s` and ", randomKey1, randomKey2);
            checkUniqueSqlList.add(String.format(" JSON_UNQUOTE(json_extract(data_content,'$.\"%s\"')) ", key2));
        }

        // 校验右表关联键是否唯一
        Integer integer = workingTableMapper.checkUniqueSqlDao(rightTableId, checkUniqueSqlList);
        if (integer != null && integer > 1) {
            throw new WorkTableException("右表关联键不唯一,请调整关联键或右表数据");
        }

        if (onSql.length() > 0) {
            onSql = onSql.substring(0, onSql.length() - 4);
        }

        // 表头信息入库
        HfDmTable hfDmTable1 = new HfDmTable();
        hfDmTable1.setTableId(tableId);
        hfDmTable1.setTableName(tableName);
        hfDmTable1.setDbId(dbId);
        hfDmTable1.setCreateUserId(userId);
        // 补充信息
        hfDmTable1.setTitle(tableName);
        hfDmTable1.setType("table");
        hfDmTable1.setKey(tableId);
        workingTableMapper.createDmTableDao(hfDmTable1);

        // 放到单独线程中执行
//        tableLeftJoinTask.test();
        dmTableTask.tableLeftJoinThread(leftTableId, rightTableId, leftSql, rightSql, onSql, userId, tableId);

        return TResponseVo.success(new HashMap<String, String>() {{
            put("key", tableId);
        }});
    }

    /**
     * 计算列
     *
     * @param tableId   表id
     * @param columnsId 列id
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo calculateService(String tableId, String columnsId, String dataId, String userId) throws Exception {
        ArrayList<String> strings = new ArrayList<>();
        strings.add(columnsId);
        try {
            dmTableTask.calculateThread(tableId, strings, dataId, userId, true);
        } catch (Exception e) {

        }
        return TResponseVo.success();
    }


    /**
     * 列匹配
     *
     * @param tableId   表id
     * @param columnsId 列id
     * @param dataId    数据id
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo columnsMatchService(String tableId, String columnsId, String dataId, String userId) throws Exception {
        ArrayList<String> strings = new ArrayList<>();
        strings.add(columnsId);
        try {
//            dmTableTask.columnsMatchThread(tableId, strings, dataId, userId);
        } catch (Exception e) {

        }
        return TResponseVo.success();
    }

    /**
     * 记录错误日志
     *
     * @param hfErrLogPojo
     */
    @Override
    public TResponseVo saveErrLogService(HfErrLogPojo hfErrLogPojo, String referer, String userAgent) throws Exception {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        hfErrLogPojo.setUuid(uuid);
        // 额外信息
        HashMap<String, Object> map = new HashMap<>();
        map.put("referer", referer);
        map.put("userAgent", userAgent);
        hfErrLogPojo.setExtraInfo(JSON.toJSONString(map));
        workingTableMapper.createErrLogDao(hfErrLogPojo);
        return TResponseVo.success("记录完成");
    }


    /**
     * 创建视频教程
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo createTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        String title = hfTeachVideoPojo.getTitle();
        String videoUrl = hfTeachVideoPojo.getVideoUrl();
        if (StringUtils.isBlank(title) || StringUtils.isBlank(videoUrl)) {
            throw new WorkTableException("必填参数为空");
        }
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        hfTeachVideoPojo.setVideoId(uuid);
        workingTableMapper.createTeachVideoDao(hfTeachVideoPojo);
        return TResponseVo.success(new HashMap<String, Object>() {{
            put("videoId", uuid);
        }});
    }

    /**
     * 删除视频教程
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        String videoId = hfTeachVideoPojo.getVideoId();
        if (StringUtils.isBlank(videoId)) {
            throw new WorkTableException("必填参数为空");
        }
        workingTableMapper.delTeachVideoDao(videoId);
        return TResponseVo.success("删除完成");
    }

    /**
     * 修改视频教程
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo updateTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        String videoId = hfTeachVideoPojo.getVideoId();
        if (StringUtils.isBlank(videoId)) {
            throw new WorkTableException("必填参数为空");
        }
        workingTableMapper.updateTeachVideoDao(hfTeachVideoPojo);
        return TResponseVo.success("更新完成");
    }

    /**
     * 获取视频教程信息
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getTeachVideoInfoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        HfTeachVideoPojo teachVideoInfoDao = workingTableMapper.getTeachVideoInfoDao(hfTeachVideoPojo.getVideoId());
        return TResponseVo.success(teachVideoInfoDao);
    }

    /**
     * 获取视频教程列表
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getTeachVideoListService(PagePojo pagePojo) throws Exception {
        String isPaged = pagePojo.getIsPaged();
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String paramNameLike = pagePojo.getParamNameLike();

        if (StringUtils.isNotBlank(paramNameLike)) {
            paramNameLike = paramNameLike.replaceAll("'", "");
        }

        List<HfTeachVideoPojo> teachVideoListDao = new ArrayList<>();
        if ("0".equalsIgnoreCase(isPaged)) {
            teachVideoListDao = workingTableMapper.getTeachVideoListDao(paramNameLike);
            return TResponseVo.success(teachVideoListDao);
        } else {
            PageHelper.startPage(pageNum, pageSize);
            teachVideoListDao = workingTableMapper.getTeachVideoListDao(paramNameLike);
            PageInfo pageInfo = new PageInfo(teachVideoListDao);
            return TResponseVo.success(pageInfo);
        }

    }

    /**
     * 有用
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo likeTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        String videoId = hfTeachVideoPojo.getVideoId();
        if (StringUtils.isBlank(videoId)) {
            throw new WorkTableException("必填参数为空");
        }
        workingTableMapper.likeTeachVideoDao(videoId);
        return TResponseVo.success();
    }

    /**
     * 没用
     *
     * @param hfTeachVideoPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo dislikeTeachVideoService(HfTeachVideoPojo hfTeachVideoPojo) throws Exception {
        String videoId = hfTeachVideoPojo.getVideoId();
        if (StringUtils.isBlank(videoId)) {
            throw new WorkTableException("必填参数为空");
        }
        workingTableMapper.dislikeTeachVideoDao(videoId);
        return TResponseVo.success();
    }

    /**
     * 上传模板
     *
     * @param file
     * @param userId
     * @param folderId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo uploadModelFileService(MultipartFile file, String userId, String folderId) throws Exception {
        if (file.isEmpty()) {
            throw new WorkTableException("文件为空，上传失败");
        }

        String fileName = file.getOriginalFilename(); // 文件名
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();  // 后缀名小写
        String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));

        List<String> extensionList = new ArrayList<>();
        extensionList.add(".doc");
        extensionList.add(".docx");
        extensionList.add(".xls");
        extensionList.add(".xlsx");
        extensionList.add(".wps");
        if (!extensionList.contains(extension)) {
            throw new WorkTableException("文件上传错误,请上传Word或Excel文档");
        }

        String fileUuid = UUID.randomUUID().toString().replaceAll("-", "");

        File filePathNew = new File(this.uploadModelPath + "/" + fileUuid);
        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        File docxFileNew = new File(this.uploadModelPath + "/" + fileUuid + "/" + fileUuid + extension);
        try {
            file.transferTo(docxFileNew);
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
            throw new WorkTableException("文件上传报错，上传失败");
        }
        if (StringUtils.isBlank(folderId)) {
            folderId = null;
        }

        // 信息记录
        HfUploadModelFilePojo hfUploadModelFilePojo = new HfUploadModelFilePojo();
        hfUploadModelFilePojo.setUploadUuid(fileUuid);
        hfUploadModelFilePojo.setFileUuid(fileUuid);
        hfUploadModelFilePojo.setFileName(fileNameWithoutExtension);
        hfUploadModelFilePojo.setExtension(extension);
        hfUploadModelFilePojo.setCreateUserId(userId);
        hfUploadModelFilePojo.setFolderId(folderId);
        hfUploadModelFilePojo.setFilePath(this.uploadModelPath + "/" + fileUuid + "/" + fileUuid + extension);
        workingTableMapper.uploadModelFileDao(hfUploadModelFilePojo);
        return TResponseVo.success("上传完成");
    }


//    /**
//     * 将上传模板作为附件上传到参数内
//     *
//     * @param fileUuid
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public TResponseVo uploadAnnexParamByUploadModelService(String fileUuid) throws Exception {
//        if (fileUuid == null) {
//            throw new WorkTableException("必填参数为空");
//        }
//
//        DocFileIndexPojo docAllInfoDao = editToolMapper.getUploadModelFileInfoDao(fileUuid);
//        if (docAllInfoDao.getIsDel()) {
//            throw new WorkTableException("该文档已被删除，无法下载");
//        }
//        String fileName = docAllInfoDao.getFileName();
//        String extension = docAllInfoDao.getExtension();
//        String uid = UUID.randomUUID().toString().replaceAll("-", ""); // 参数uid 与填参逻辑保持一致
//        HashMap<String, Object> retMap = new HashMap<>();
//        HashMap<String, String> map = new HashMap<>();
//        map.put("uid", uid);
//        map.put("name", fileName + extension);
//        map.put("url", docAllInfoDao.getFilePath());
//        retMap.put("paramsText", map);
//        retMap.put("info", "保存成功");
//        return TResponseVo.success(retMap);
//    }

    /**
     * 预览关联模板
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo previewMatchFileService(String fileUuid) throws Exception {
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        if (docAllInfoDao == null) {
            docAllInfoDao = editToolMapper.getUploadModelFileInfoDao(fileUuid);
            if (docAllInfoDao == null) {
                throw new WorkTableException("关联文件不存在或已删除");
            } else {
                // 是上传的模板
                HashMap<String, String> retMap = new HashMap<>();
                retMap.put("type", "uploadModel");
                retMap.put("fileName", docAllInfoDao.getFileName());
                if (getHttpStatusCode("https://www.office.com") == 200) {
                    retMap.put("url", "https://view.officeapps.live.com/op/view.aspx?src=" + httpProtocol + "://" + hostName + ".nebulaedata.cn/" + docAllInfoDao.getFilePath());
                } else {
                    retMap.put("url", httpProtocol + "://" + hostName + ".nebulaedata.cn/" + docAllInfoDao.getFilePath());
                }
                return TResponseVo.success(retMap);
            }
        } else {
            // 是韩非文档
            HashMap<String, String> retMap = new HashMap<>();
            retMap.put("type", "hfDoc");
            return TResponseVo.success(retMap);
        }
    }

    /**
     * 下载uploadModel类型的模板文档
     *
     * @param fileUuid
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo downloadModelService(String fileUuid, HttpServletResponse response) throws Exception {
        DocFileIndexPojo uploadModelFileInfoDao = editToolMapper.getUploadModelFileInfoDao(fileUuid);
        if (uploadModelFileInfoDao == null) {
            throw new WorkTableException("文件非上传模板或已删除");
        }

        String filePath = uploadModelFileInfoDao.getFilePath();
        String fileName = uploadModelFileInfoDao.getFileName();
        String extension = uploadModelFileInfoDao.getExtension();

        File file = new File(filePath);
        if (file.exists()) {
            Path path = file.toPath();
            String mimeType = Files.probeContentType(path);
            //response为HttpServletResponse对象
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + extension);
            response.setContentType(mimeType);
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                OutputStream outputStream = response.getOutputStream();
                int i = bis.read(buffer);
                while (i != -1) {
                    outputStream.write(buffer, 0, i);
                    i = bis.read(buffer);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
//                file.delete();
        } else {
            throw new WorkTableException("下载失败,文件不存在或已删除");
        }
        // 记录
//        String logId = UUID.randomUUID().toString().replaceAll("-", "");
//        workingTableMapper.newDownloadRecordDao(userId, uuid, logId);
        return TResponseVo.success("下载成功");
    }
}
