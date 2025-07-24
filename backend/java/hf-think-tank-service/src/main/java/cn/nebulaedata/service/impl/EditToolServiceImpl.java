package cn.nebulaedata.service.impl;

import cn.nebulaedata.dao.EditToolMapper;
import cn.nebulaedata.dao.FileOperationMapper;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.exception.EditToolException;
import cn.nebulaedata.pojo.*;
import cn.nebulaedata.service.EditToolService;
import cn.nebulaedata.utils.JsonKeyUtils;
import cn.nebulaedata.utils.List2TreeUtils;
import cn.nebulaedata.utils.RedisUtils;
import cn.nebulaedata.vo.TResponseVo;
import com.alibaba.fastjson.JSON;
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
import java.util.stream.Collectors;

import static cn.nebulaedata.utils.JsonKeyUtils.getHttpStatusCode;

/**
 * @author 徐衍旭
 * @date 2023/3/15 14:11
 * @note
 */
@Service
public class EditToolServiceImpl implements EditToolService {

    private static final Logger LOG = LoggerFactory.getLogger(EditToolServiceImpl.class);
    @Autowired
    private FileOperationMapper fileOperationMapper;
    @Autowired
    private WorkingTableMapper workingTableMapper;
    @Autowired
    private EditToolMapper editToolMapper;
    @Value("${doc-frame-service.env-name}")
    private String envName;
    @Value("${doc-frame-service.host-name}")
    private String hostName;
    @Value("${protocol.http}")
    private String httpProtocol;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 文档空间-文件空间-新建文件夹
     *
     * @param editToolFolderPojo
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo newFolderService(EditToolFolderPojo editToolFolderPojo) throws Exception {
        // 必填参数
        String folderName = editToolFolderPojo.getFolderName();
        if (StringUtils.isBlank(folderName)) {
            return TResponseVo.error("新建文件夹名称不能为空");
        }
        // 同路径下同名文件夹校验
        String createUserId = editToolFolderPojo.getCreateUserId();
        String folderParentId = editToolFolderPojo.getFolderParentId();
        if ("root".equals(folderParentId)) {
            folderParentId = null;
        }

        String type = editToolFolderPojo.getType();
        if (StringUtils.isBlank(type) || (!"template".equals(type) && !"origin".equals(type))) {
            type = "file";
        }

        Integer i = 0;
        if ("origin".equals(type)) {
            if (StringUtils.isBlank(folderParentId)) {
                i = editToolMapper.checkFolderNameDao(folderName, null, null, type, null);
            } else {
                i = editToolMapper.checkFolderNameDao(folderName, null, folderParentId, type, null);
            }
        } else {
            if (StringUtils.isBlank(folderParentId)) {
                i = editToolMapper.checkFolderNameDao(folderName, createUserId, null, type, null);
            } else {
                i = editToolMapper.checkFolderNameDao(folderName, createUserId, folderParentId, type, null);
            }
        }
        if (i > 0) {
            EditToolException editToolException = new EditToolException();
            editToolException.setCode(2003);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newFolderService", folderName, "当前路径下已存在同名文件夹");
            throw editToolException;
        }

        String folderId = UUID.randomUUID().toString().replaceAll("-", "");
        editToolFolderPojo.setFolderId(folderId);
        editToolFolderPojo.setFolderType(type);
        try {
            i = editToolMapper.newFolderDao(editToolFolderPojo);  // 新建文件夹
            if (i != 1) {
                EditToolException editToolException = new EditToolException();
                editToolException.setCode(2001);
                // TODO 报错未处理
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newFolderService", folderName, "新增文件夹失败");
                throw editToolException;
            }
        } catch (EditToolException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            EditToolException editToolException = new EditToolException();
            editToolException.setCode(2002);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newFolderService", folderName, "SQL错误");
            throw editToolException;
        }
        return TResponseVo.success("新建文件夹成功");
    }


    /**
     * 文档空间-文件空间-删除文件夹
     *
     * @param keyList
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo delFolderService(List<String> keyList) throws Exception {
        if (keyList == null) {
            TResponseVo.error("必填参数为空");
        }
        if (keyList.size() == 0) {
            return TResponseVo.success("删除完成");
        }
//        // 检查文件夹下是否为空
//        Integer integer = editToolMapper.checkFolderEmpty1Dao(folderId);
//        if (integer == 0) integer = editToolMapper.checkFolderEmpty2Dao(folderId);
//        if (integer > 0) {
//            EditToolException editToolException = new EditToolException();
//            editToolException.setCode(2011);
//            // TODO 报错未处理
//            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "delFolderService", folderId, "该文件夹不为空,删除失败");
//            throw editToolException;
//        }


        // 删除文件夹递归删除文件夹下的内容 如果以后做回收站 额外加一张表记录回收站内容
        String keyListStr = new JsonKeyUtils().listToString(keyList, "','");
        // 先判断keyList中哪些是文件夹
        List<String> folderKeyListDao = editToolMapper.getFolderKeyListDao(keyListStr);
        // 顺便获取文件的key
        List<String> documentKeyList = new ArrayList<>();
        for (String key : keyList) {
            if (!folderKeyListDao.contains(key)) {
                documentKeyList.add(key);
            }
        }

        List<String> folderKeyListDaoTmp = new ArrayList<>();
        List<String> folderKeyListDaoTotal = new ArrayList<>();
        folderKeyListDaoTmp.addAll(folderKeyListDao);  // 逐级存放要删除的文件夹
        folderKeyListDaoTotal.addAll(folderKeyListDao);  // 存放所有要删除文件夹
        // 获取所有文件夹清单 用于递归
        //TODO allFolderDao缓存
        List<EditToolFolderPojo> allFolderDao = editToolMapper.getAllFolderDao();
        while (folderKeyListDaoTmp.size() != 0) {
            folderKeyListDaoTmp.clear();
            for (String folderId : folderKeyListDao) {
                for (EditToolFolderPojo toolFolderPojo : allFolderDao) {
                    if (folderId.equals(toolFolderPojo.getFolderParentId())) {
                        folderKeyListDaoTmp.add(toolFolderPojo.getFolderId());
                        folderKeyListDaoTotal.add(toolFolderPojo.getFolderId());
                    }
                }
            }
            folderKeyListDao.clear();
            folderKeyListDao.addAll(folderKeyListDaoTmp);
        }


        String folderKeyListDaoTotalStr = new JsonKeyUtils().listToString(folderKeyListDaoTotal, "','");
        String documentKeyListStr = new JsonKeyUtils().listToString(documentKeyList, "','");
        Integer i = 1;
        try {
            editToolMapper.delFolderDao(folderKeyListDaoTotalStr);  // 删除文件夹
            editToolMapper.delDocumentDao(documentKeyListStr, folderKeyListDaoTotalStr);  // 删除文件
            editToolMapper.delUploadModelFileDao(documentKeyListStr, folderKeyListDaoTotalStr);    // 删除上传的模板
            if (i == 0) {
                EditToolException editToolException = new EditToolException();
                editToolException.setCode(2013);
                // TODO 报错未处理
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "delFolderService", keyListStr, "删除文件夹失败");
                throw editToolException;
            }
        } catch (EditToolException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            EditToolException editToolException = new EditToolException();
            editToolException.setCode(2014);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "delFolderService", keyListStr, "SQL错误");
            throw editToolException;
        }

        return TResponseVo.success("删除完成");
    }

    /**
     * 文档空间-文件空间-重命名文件夹
     *
     * @param editToolFolderPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo renameFolderService(EditToolFolderPojo editToolFolderPojo) throws Exception {
        // 必填参数
        String folderId = editToolFolderPojo.getFolderId();
        String folderName = editToolFolderPojo.getFolderName();
        if (StringUtils.isBlank(folderName) || StringUtils.isBlank(folderId)) {
            return TResponseVo.error("必填参数为空");
        }
        // 同路径下同名文件夹校验
        String createUserId = editToolFolderPojo.getCreateUserId();
        EditToolFolderPojo folderInfoDao = editToolMapper.getFolderInfoDao(folderId);  // 查询要修改的文件夹信息
        String folderParentId = folderInfoDao == null ? null : folderInfoDao.getFolderParentId();

        String type = folderInfoDao.getFolderType();
        if (StringUtils.isBlank(type) || (!"template".equals(type) && !"origin".equals(type))) {
            type = "file";
        }

        Integer i = 0;
        if ("origin".equals(type)) {
            if (StringUtils.isBlank(folderParentId)) {
                i = editToolMapper.checkFolderNameDao(folderName, null, null, type, null);
            } else {
                i = editToolMapper.checkFolderNameDao(folderName, null, folderParentId, type, null);
            }
        } else {
            if (StringUtils.isBlank(folderParentId)) {
                i = editToolMapper.checkFolderNameDao(folderName, createUserId, null, type, folderId);
            } else {
                i = editToolMapper.checkFolderNameDao(folderName, createUserId, folderParentId, type, folderId);
            }
        }
        if (i > 0) {
            EditToolException editToolException = new EditToolException();
            editToolException.setCode(2003);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newFolderService", folderName, "当前路径下已存在同名文件夹");
            throw editToolException;
        }

        try {
            i = editToolMapper.renameFolderDao(folderId, folderName);  // 重命名文件夹
            if (i != 1) {
                EditToolException editToolException = new EditToolException();
                editToolException.setCode(2021);
                // TODO 报错未处理
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newFolderService", folderName, "重命名文件夹失败");
                throw editToolException;
            }
        } catch (EditToolException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            EditToolException editToolException = new EditToolException();
            editToolException.setCode(2022);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newFolderService", folderName, "SQL错误");
            throw editToolException;
        }
        return TResponseVo.success("修改完成");
    }

    /**
     * 文档空间-文件空间-重命名文件
     *
     * @param docFileIndexPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo renameDocumentService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileName = docFileIndexPojo.getFileName();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileName)) {
            return TResponseVo.error("必填参数为空");
        }

        // 同路径下同名文件夹校验
//        EditToolFolderPojo folderInfoDao = editToolMapper.getFolderInfoDao(folderId);  // 查询要修改的文件夹信息
//        String folderParentId = folderInfoDao == null ? null : folderInfoDao.getFolderParentId();
        Integer i = 0;
//        if (StringUtils.isBlank(folderParentId)) {
//            i = editToolMapper.checkFolderNameDao(folderName, null);
//        } else {
//            i = editToolMapper.checkFolderNameDao(folderName, folderParentId);
//        }
//        if (i > 0) {
//            EditToolException editToolException = new EditToolException();
//            editToolException.setCode(2003);
//            // TODO 报错未处理
//            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newFolderService", folderName, "当前路径下已存在同名文件夹");
//            throw editToolException;
//        }

        try {
            i = editToolMapper.renameDocumentDao(fileUuid, fileName);  // 重命名文件夹
            i += editToolMapper.renameUploadModelFileDao(fileUuid, fileName);
            if (i != 1) {
                EditToolException editToolException = new EditToolException();
                editToolException.setContent("重命名文件失败");
                throw editToolException;
            }
        } catch (EditToolException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            EditToolException editToolException = new EditToolException();
            editToolException.setContent("重命名文件失败.");
            throw editToolException;
        }
        return TResponseVo.success("修改完成");
    }

    /**
     * 文档空间-文件空间-获取文件夹信息
     *
     * @param folderId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getFolderInfoService(String folderId) throws Exception {
        if (StringUtils.isBlank(folderId)) {
            return TResponseVo.success(null);
        }
        EditToolFolderPojo folderInfoDao = editToolMapper.getFolderInfoDao(folderId);  // 查询要修改的文件夹信息
        if (folderInfoDao == null) {
            return TResponseVo.error("当前文件夹不存在或已删除");
        }
        ArrayList<LabelValuePojo> strings = new ArrayList<>();
        strings.add(new LabelValuePojo(folderInfoDao.getFolderName(), folderInfoDao.getFolderId()));
        if (!StringUtils.isBlank(folderInfoDao.getFolderParentId())) {
            // 计算当前路径
            //TODO allFolderDao缓存
            List<EditToolFolderPojo> allFolderDao = editToolMapper.getAllFolderDao();  //
            EditToolFolderPojo lastEditToolFolderPojo = folderInfoDao;  // 当前最新
            Boolean flag = true;
            Integer i = 20;
            while (i > 0 && flag) {
                i = i - 1;
                for (EditToolFolderPojo editToolFolderPojo : allFolderDao) {
                    if (editToolFolderPojo.getFolderId().equals(lastEditToolFolderPojo.getFolderParentId())) {
                        strings.add(new LabelValuePojo(editToolFolderPojo.getFolderName(), editToolFolderPojo.getFolderId()));
                        if (StringUtils.isBlank(editToolFolderPojo.getFolderParentId())) {
                            flag = false;
                        } else {
                            lastEditToolFolderPojo = editToolFolderPojo;
                        }
                        break;
                    }
                }
            }
        }
        folderInfoDao.setFolderPath(strings);
        return TResponseVo.success(folderInfoDao);
    }

    @Override
    public TResponseVo getDocumentInfoService(String fileUuid) throws Exception {
        DocFileIndexPojo documentInfoDao = editToolMapper.getDocumentInfoDao(fileUuid);
        if (documentInfoDao == null) {
            documentInfoDao = editToolMapper.getUploadModelFileInfoDao(fileUuid);
        }
        String folderId = documentInfoDao.getFolderId();
        if (!StringUtils.isBlank(folderId)) {
            // 计算当前路径
            //TODO allFolderDao缓存
            List<EditToolFolderPojo> allFolderDao = editToolMapper.getAllFolderDao();  //
            ArrayList<LabelValuePojo> strings = new ArrayList<>();
            String lastEditToolFolderId = folderId;  // 当前最新
            Boolean flag = true;
            Integer i = 20;
            while (i > 0 && flag) {
                i = i - 1;
                for (EditToolFolderPojo editToolFolderPojo : allFolderDao) {
                    if (editToolFolderPojo.getFolderId().equals(lastEditToolFolderId)) {
                        strings.add(new LabelValuePojo(editToolFolderPojo.getFolderName(), editToolFolderPojo.getFolderId()));
                        if (StringUtils.isBlank(editToolFolderPojo.getFolderParentId())) {
                            flag = false;
                        } else {
                            lastEditToolFolderId = editToolFolderPojo.getFolderParentId();
                        }
                        break;
                    }
                }
            }
            documentInfoDao.setFolderPath(strings);
        }
        return TResponseVo.success(documentInfoDao);
    }

    /**
     * 文档空间-文件空间-移动文件夹-获取文件夹清单
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getMoveFolderListService(PagePojo pagePojo, String userId) throws Exception {
        if (pagePojo == null) {
            pagePojo = new PagePojo();
        }
        String type = pagePojo.getType();
        if (StringUtils.isBlank(type) || (!"template".equals(type) && !"origin".equals(type))) {
            type = "file";
        }

        Boolean all = pagePojo.getAll();  // 是否查树
        if (all == null) {
            all = false;
        }

        if (all) {
            List<EditToolFolderPojo> folderListDao = editToolMapper.getAllFolderListDao(userId,type);
            List list = new List2TreeUtils().recursionMethod(folderListDao, "folderId", "folderParentId");
            return TResponseVo.success(list);
        } else {
            String folderId = pagePojo.getFolderId();
            if (StringUtils.isBlank(folderId)) {
                List<EditToolFolderPojo> folderListDao = new ArrayList<>();
                EditToolFolderPojo editToolFolderPojo = new EditToolFolderPojo();
                editToolFolderPojo.setTitle("根目录");
                editToolFolderPojo.setKey("root");
                folderListDao.add(editToolFolderPojo);
                List<EditToolFolderPojo> folderListDao2 = editToolMapper.getFolderListDao(null, null, userId, null, type);
                editToolFolderPojo.setChildren(folderListDao2);
                return TResponseVo.success(folderListDao);
            } else if ("root".equals(folderId)) {
                List<EditToolFolderPojo> folderListDao = editToolMapper.getFolderListDao(null, null, userId, null, type);
                return TResponseVo.success(folderListDao);
            } else {
                List<EditToolFolderPojo> folderListDao = editToolMapper.getFolderListDao(null, folderId, userId, null, type);
                return TResponseVo.success(folderListDao);
            }
        }
    }

    /**
     * 文档空间-文件空间-移动文件夹-移动
     *
     * @param keyList
     * @param folderId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo moveFolderService(List<String> keyList, String folderId) throws Exception {
        if (keyList == null || StringUtils.isBlank(folderId)) {
            TResponseVo.error("必填参数为空");
        }
        if (keyList.size() == 0) {
            return TResponseVo.success("未发生移动");
        }

        if ("root".equals(folderId)) {
            folderId = null;
        }

        // 判断是否移动到了子文件夹下
        if (folderId != null) {  // folderId为空表示移动到根目录
            EditToolFolderPojo folderInfoDao = editToolMapper.getFolderInfoDao(folderId);  // 查询目标文件夹信息
            ArrayList<LabelValuePojo> strings = new ArrayList<>();
            ArrayList<String> folderIdList = new ArrayList<>();  // 父folderId
            strings.add(new LabelValuePojo(folderInfoDao.getFolderName(), folderInfoDao.getFolderId()));
            folderIdList.add(folderInfoDao.getFolderId());
            if (!StringUtils.isBlank(folderInfoDao.getFolderParentId())) {
                // 计算当前路径
                //TODO allFolderDao缓存
                List<EditToolFolderPojo> allFolderDao = editToolMapper.getAllFolderDao();  //
                EditToolFolderPojo lastEditToolFolderPojo = folderInfoDao;  // 当前最新
                Boolean flag = true;
                Integer i = 20;
                while (i > 0 && flag) {
                    i = i - 1;
                    for (EditToolFolderPojo editToolFolderPojo : allFolderDao) {
                        if (editToolFolderPojo.getFolderId().equals(lastEditToolFolderPojo.getFolderParentId())) {
                            strings.add(new LabelValuePojo(editToolFolderPojo.getFolderName(), editToolFolderPojo.getFolderId()));
                            folderIdList.add(editToolFolderPojo.getFolderId());
                            if (StringUtils.isBlank(editToolFolderPojo.getFolderParentId())) {
                                flag = false;
                            } else {
                                lastEditToolFolderPojo = editToolFolderPojo;
                            }
                            break;
                        }
                    }
                }
            }
            folderInfoDao.setFolderPath(strings);

            for (String key : keyList) {
                if (folderIdList.contains(key)) {
                    throw new EditToolException("目标文件夹是源文件夹的子文件夹");
                }
            }
        }


        // 同路径下同名文件夹校验
        // 获取目标文件夹下所有文件夹名
        List<String> folderNameListDao = editToolMapper.getFolderNameListByParentFolderIdDao(folderId);
        // 获取要移动的文件夹所有名称
        List<String> folderNameListByFolderIdDao = editToolMapper.getFolderNameListByFolderIdDao(keyList);
        for (String folderName : folderNameListByFolderIdDao) {
            if (folderNameListDao.contains(folderName)) {
                throw new EditToolException("存在同名文件夹");
            }
        }

//        EditToolFolderPojo folderInfoDao = editToolMapper.getFolderInfoDao(folderId);  // 查询要修改的文件夹信息
//        String folderParentId = folderInfoDao == null ? null : folderInfoDao.getFolderParentId();
//        Integer i = 0;
//        if (StringUtils.isBlank(folderParentId)) {
//            i = editToolMapper.checkFolderNameDao(folderName, null);
//        } else {
//            i = editToolMapper.checkFolderNameDao(folderName, folderParentId);
//        }
//        if (i > 0) {
//            EditToolException editToolException = new EditToolException();
//            editToolException.setCode(2003);
//            // TODO 报错未处理
//            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "newFolderService", folderName, "当前路径下已存在同名文件夹");
//            throw editToolException;
//        }
//
        String keyListStr = new JsonKeyUtils().listToString(keyList, "','");
        // 获取所有需要移动的文件及文件夹的名称集合
//        ArrayList<String> allSourceFolderName = editToolMapper.getAllSourceFolderNameDao(keyListStr);
//        ArrayList<String> allTargetFolderName = new ArrayList<>();
//        if (StringUtils.isBlank(folderParentId)) {
//            allTargetFolderName = editToolMapper.getAllTargetFolderNameDao( null);
//        } else {
//            allTargetFolderName = editToolMapper.getAllTargetFolderNameDao( folderParentId);
//        }


        Integer j = 1;
        try {
            editToolMapper.moveFolderDao(keyListStr, folderId);  // 移动文件夹
            editToolMapper.moveDocumentDao(keyListStr, folderId);  // 移动文件
            editToolMapper.moveUploadModelFileDao(keyListStr, folderId);  // 移动文件
            if (j == 0) {
                EditToolException editToolException = new EditToolException();
                editToolException.setCode(2032);
                // TODO 报错未处理
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "moveFolderService", keyListStr, "移动文件夹失败");
                throw editToolException;
            }
        } catch (EditToolException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            EditToolException editToolException = new EditToolException();
            editToolException.setCode(2033);
            // TODO 报错未处理
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "moveFolderService", keyListStr, "SQL错误");
            throw editToolException;
        }

        return TResponseVo.success("移动完成");
    }

    /**
     * 文档空间-文件空间-获取文件夹清单
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getFolderListService(PagePojo pagePojo, String userId) throws Exception {
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String isPaged = pagePojo.getIsPaged();
        String searchLike = pagePojo.getParamNameLike();
        Integer limit = pagePojo.getLimit();
        String sort = pagePojo.getSort();

        if (StringUtils.isNotBlank(searchLike)) {
            searchLike = searchLike.replaceAll("'", "");
        }
        if (StringUtils.isBlank(searchLike)) {  // 增加一行代码 解决空字串
            searchLike = null;
        }

        String folderId = pagePojo.getFolderId();
        if (StringUtils.isBlank(folderId)) {
            folderId = null;
        } else {
            EditToolFolderPojo folderInfoDao = editToolMapper.getFolderInfoDao(folderId);  // 查询要修改的文件夹信息
            if (folderInfoDao == null) {
                return TResponseVo.error("当前文件夹不存在或已删除");
            }
        }

        String type = pagePojo.getType();
        if (StringUtils.isBlank(type) || (!"template".equals(type) && !"origin".equals(type))) {
            type = "file";
        }

        if (StringUtils.isBlank(sort) || "desc".equals(sort)) {
            sort = null;
        } else {
            sort = "asc";
        }

        if ("0".equals(isPaged)) {
            List<EditToolFolderPojo> folderListDao = editToolMapper.getFolderListDao(searchLike, folderId, userId, sort, type);
            List<DocFileIndexPojo> documentListDao = editToolMapper.getDocumentListDao(searchLike, folderId, userId, null, type);
            if ("template".equals(type)) {  // 只有查模板时可查
                List<DocFileIndexPojo> uploadModelFileListDao = editToolMapper.getUploadModelFileListDao(searchLike, folderId, userId, null, type);
                if (getHttpStatusCode("https://www.office.com") == 200) {
                    uploadModelFileListDao.forEach(k -> k.setUrl("https://view.officeapps.live.com/op/view.aspx?src=" + httpProtocol + "://" + hostName + ".nebulaedata.cn/" + k.getFilePath()));
                } else {
                    uploadModelFileListDao.forEach(k -> k.setUrl(k.getFilePath()));
                }
                documentListDao.addAll(uploadModelFileListDao);
            }
            // 按照时间排序
            if (sort == null) {
                documentListDao = documentListDao.stream().sorted(Comparator.comparing(DocFileIndexPojo::getUpdateTime).reversed()).collect(Collectors.toList());
            } else {
                documentListDao = documentListDao.stream().sorted(Comparator.comparing(DocFileIndexPojo::getUpdateTime)).collect(Collectors.toList());
            }
            List<Object> list = new ArrayList<>();
            list.addAll(folderListDao);
            list.addAll(documentListDao);
            return TResponseVo.success(list);
        } else {
            // 分页
            PageHelper.startPage(pageNum, pageSize);
            List<EditToolFolderPojo> folderListDao = editToolMapper.getFolderListDao(searchLike, folderId, userId, null, type);
            PageInfo pageInfo = new PageInfo(folderListDao);
            return TResponseVo.success(pageInfo);
        }
    }

    /**
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getFolderListLVService(String userId) throws Exception {
        List<EditToolFolderPojo> folderListDao = editToolMapper.getFolderListLVDao(userId);
        return TResponseVo.success(folderListDao);
    }

    /**
     * 文档空间-文件空间-总搜索
     *
     * @param pagePojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo searchService(PagePojo pagePojo) throws Exception {
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String isPaged = pagePojo.getIsPaged();
        String searchLike = pagePojo.getParamNameLike();
        Integer limit = pagePojo.getLimit();
        String folderId = pagePojo.getFolderId();

        if (StringUtils.isBlank(folderId)) {
            folderId = null;
        }
        if (StringUtils.isNotBlank(searchLike)) {
            searchLike = searchLike.replaceAll("'", "");
        }
        String[] split = searchLike.split("@", 2);
        String fileNameLike = split[0];
        if (StringUtils.isBlank(fileNameLike)) {
            return TResponseVo.error("搜索内容不能为空");
        }
        String type = null;
        try {
            type = split[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        if ("0".equals(isPaged)) {
            List<DocFileIndexPojo> docFileIndexPojos = editToolMapper.searchDao(fileNameLike, type, folderId);
            return TResponseVo.success(docFileIndexPojos);
        } else {
            // 分页
            PageHelper.startPage(pageNum, pageSize);
            List<DocFileIndexPojo> docFileIndexPojos = editToolMapper.searchDao(fileNameLike, type, folderId);
            PageInfo pageInfo = new PageInfo(docFileIndexPojos);
            return TResponseVo.success(pageInfo);
        }
    }


    /**
     * 最近打开的文档
     *
     * @param userId
     */
    @Override
    public TResponseVo getRecentFileService(String userId) throws Exception {
        if (redisUtils.hasKey(envName + userId + "recentSearch")) {
            String s = (String) redisUtils.get(envName + userId + "recentSearch");
            List<Map<String, String>> list = JSON.parseObject(s, List.class);
            return TResponseVo.success(list);
        } else {
            return TResponseVo.success(new ArrayList<String>());
        }
    }


    /**
     * 提交审核
     *
     * @param docFileIndexPojo
     */
    @Override
    public TResponseVo submitAssessService(DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        editToolMapper.updateFileAssessStatusDao(fileUuid, "待审核");
        return TResponseVo.success("提交审核完成");
    }

    /**
     * 获取待审核列表
     *
     * @param pagePojo
     */
    @Override
    public TResponseVo getNeedAssessListService(PagePojo pagePojo) throws Exception {
        Integer pageNum = pagePojo.getPageNum();
        Integer pageSize = pagePojo.getPageSize();
        String isPaged = pagePojo.getIsPaged();
        String searchLike = pagePojo.getParamNameLike();
        Integer limit = pagePojo.getLimit();

        if (StringUtils.isNotBlank(searchLike)) {
            searchLike = searchLike.replaceAll("'", "");
        }

        return null;
    }

    /**
     * 获取功能列表
     *
     * @param
     */
    @Override
    public TResponseVo getApplicationListService(HfFileTypeDiPojo hfFileTypeDiPojo) throws Exception {
        String inUse = null;
        if (hfFileTypeDiPojo != null) {
            if (hfFileTypeDiPojo.getIsUsed() == null) {
                inUse = null;
            } else if (hfFileTypeDiPojo.getIsUsed()) {
                inUse = "1";
            } else {
                inUse = null;
            }
        }
        List<HfFileTypeDiPojo> applicationListDao = editToolMapper.getApplicationListDao(inUse);
        return TResponseVo.success(applicationListDao);
    }

    /**
     * 开通/关闭功能
     *
     * @param hfFileTypeDiPojo
     */
    @Override
    public TResponseVo changeApplicationStatusService(HfFileTypeDiPojo hfFileTypeDiPojo) throws Exception {
        Boolean isUsed = hfFileTypeDiPojo.getIsUsed();
        if (isUsed) {
            hfFileTypeDiPojo.setInUse("1");
        } else {
            hfFileTypeDiPojo.setInUse("0");
        }
        editToolMapper.changeApplicationStatusDao(hfFileTypeDiPojo);
        String fileTypeId = hfFileTypeDiPojo.getFileTypeId();
        HfFileTypeDiPojo hfFileTypeDiPojo1 = new HfFileTypeDiPojo();
        hfFileTypeDiPojo1.setFileTypeId(fileTypeId.replaceAll("MB-", "WJ-"));
        hfFileTypeDiPojo1.setInUse(hfFileTypeDiPojo.getInUse());
        editToolMapper.changeApplicationStatusDao(hfFileTypeDiPojo1);
        return TResponseVo.success("功能调整完成");
    }

    @Override
    public TResponseVo addParamsWorkflowService(ParamsWorkflowPojo paramsWorkflowPojo) {
        String fileUuid = paramsWorkflowPojo.getFileUuid();
        String fileVersionId = paramsWorkflowPojo.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.error("参数有误");
        }
        ParamsWorkflowPojo searchPojo = new ParamsWorkflowPojo();
        searchPojo.setFileVersionId(fileVersionId);
        searchPojo.setFileUuid(fileUuid);
        List<ParamsWorkflowPojo> paramsWorkflowPojos = editToolMapper.searchParamsWorkflowDao(searchPojo);
        if (paramsWorkflowPojos.size() > 0) {
            editToolMapper.deleteParamsWorkflowDao(searchPojo);
        }
        editToolMapper.addParamsWorkflowDao(paramsWorkflowPojo);
        return TResponseVo.success("保存成功");
    }

    @Override
    public TResponseVo deleteParamsWorkflowService(ParamsWorkflowPojo paramsWorkflowPojo) {
        String fileUuid = paramsWorkflowPojo.getFileUuid();
        String fileVersionId = paramsWorkflowPojo.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.error("参数有误");
        }
        editToolMapper.deleteParamsWorkflowDao(paramsWorkflowPojo);
        return TResponseVo.success("删除成功");
    }

    @Override
    public TResponseVo searchParamsWorkflowService(ParamsWorkflowPojo paramsWorkflowPojo) {
        ParamsWorkflowPojo res = new ParamsWorkflowPojo();
        List<ParamsWorkflowPojo> paramsWorkflowPojos = editToolMapper.searchParamsWorkflowDao(paramsWorkflowPojo);
        if (paramsWorkflowPojos.size() > 0) {
            res = paramsWorkflowPojos.get(0);
        }
        return TResponseVo.success(res);
    }

    @Override
    public TResponseVo updateParamsWorkflowService(ParamsWorkflowPojo paramsWorkflowPojo) {
        String fileUuid = paramsWorkflowPojo.getFileUuid();
        String fileVersionId = paramsWorkflowPojo.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            return TResponseVo.error("参数有误");
        }
        editToolMapper.updateParamsWorkflowDao(paramsWorkflowPojo);
        return TResponseVo.success("修改成功");

    }
}
