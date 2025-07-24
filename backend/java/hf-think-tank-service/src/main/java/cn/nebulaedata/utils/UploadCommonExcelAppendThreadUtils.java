package cn.nebulaedata.utils;


import cn.jiayizhen.tools.java.office2.html.creation.JOfficeHtmlCreate;
import cn.jiayizhen.tools.java.office2.model.DocxInfoObj;
import cn.nebulaedata.dao.FileOperationMapper;
import cn.nebulaedata.dao.SplitTableMapper;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.pojo.HfDmColumns;
import cn.nebulaedata.pojo.HfDmData;
import cn.nebulaedata.socket.WebSocketServer;
import cn.nebulaedata.web.excel.common.CommonExcelListener;
import cn.nebulaedata.web.excel.common.CommonExcelModel;
import cn.nebulaedata.web.excel.common.CommonExcelSaveFun;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

/**
 * @author 徐衍旭
 * @date 2022/7/20 11:38
 * @note
 */
@Component
public class UploadCommonExcelAppendThreadUtils implements Runnable {

    @Autowired
    private WorkingTableMapper workingTableMapper;
    @Autowired
    private FileOperationMapper fileOperationMapper;
    @Autowired
    private SplitTableMapper splitTableMapper;
    @Value("${split-database.dm-content-data}")
    private String dmContentDataDatabase;

    //静态化工具类变量
    public static UploadCommonExcelAppendThreadUtils uploadCommonExcelAppendThreadUtils;

    //注解用于告诉次代码在Spring加载之前就运行
    @PostConstruct
    public void init() {
        uploadCommonExcelAppendThreadUtils = this;//工具类的实例赋值给fileUtils
        uploadCommonExcelAppendThreadUtils.workingTableMapper = this.workingTableMapper;//会激活Spring对Dao的管理并赋给此类
        uploadCommonExcelAppendThreadUtils.fileOperationMapper = this.fileOperationMapper;//会激活Spring对Dao的管理并赋给此类
        uploadCommonExcelAppendThreadUtils.splitTableMapper = this.splitTableMapper;//会激活Spring对Dao的管理并赋给此类
        uploadCommonExcelAppendThreadUtils.dmContentDataDatabase = this.dmContentDataDatabase;//会激活Spring对Dao的管理并赋给此类
        System.out.println("UploadCommonExcelAppendThreadUtils工具类已经初始化了，被纳入spring管理");

    }

    private Thread t;
    private String threadTaskId;
    private String threadDbId;
    private String threadTableId;
    private String threadDesc;
    private String threadUserId;
    private InputStream threadInputStream;
    private String threadExcelPw;
    private String threadFileUuid;
    private String threadFileVersionId;

    public UploadCommonExcelAppendThreadUtils() {

    }

    public UploadCommonExcelAppendThreadUtils(String taskId, String dbId, String tableId, String desc, String userId, InputStream inputStream, String excelPw, String fileUuid, String fileVersionId) {
        threadTaskId = taskId;
        threadDbId = dbId;
        threadTableId = tableId;
        threadDesc = desc;
        threadUserId = userId;
        threadInputStream = inputStream;
        threadExcelPw = excelPw;
        threadFileUuid = fileUuid;
        threadFileVersionId = fileVersionId;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        System.out.println("Running ");
        try {

            if (threadFileUuid == null || threadFileVersionId == null) {
                // 通知前端
                WebSocketServer webSocketServer = new WebSocketServer();
                long start = System.currentTimeMillis();
                HashMap<String, Object> webSocketMap = new HashMap<>();
                String msg = "开始解析excel";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 20);
                webSocketMap.put("action", "append");
                webSocketServer.sendInfo(webSocketMap, threadUserId);
                // 目标表的表头
                List<HfDmColumns> dmTableColumnsListDao = uploadCommonExcelAppendThreadUtils.workingTableMapper.getDmTableColumnsListDao(threadTableId);
                List<HfDmColumns> dmTableColumnsListDaoTmp = new ArrayList<>();
                for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                    // 过滤掉非文本类型表头
                    if ("40".equals(hfDmColumns.getFieldType())) {
                        dmTableColumnsListDaoTmp.add(hfDmColumns);
                    }
                }
                dmTableColumnsListDao = dmTableColumnsListDaoTmp;

                // 1.数据清洗+数据准备
                CommonExcelSaveFun commonExcelSaveFun = new CommonExcelSaveFun();
                commonExcelSaveFun.clearData();
                try {
                    EasyExcel.read(threadInputStream, CommonExcelModel.class, new CommonExcelListener(commonExcelSaveFun)).password(threadExcelPw).sheet(0).doRead();
                } catch (NoSuchMethodError e) {
                    throw new WorkTableException("上传解析失败，请检查是否存在时间或日期格式的列数据，请调整为文本格式后上传");
                } finally {
                    msg = "上传解析失败，请检查是否存在时间或日期格式的列数据，请调整为文本格式后上传";
                    System.out.println(msg);
                    webSocketMap.put("msg", msg);
                    webSocketMap.put("title", "数据解析");
                    webSocketMap.put("taskId", threadTaskId);
                    webSocketMap.put("tableId", threadTableId);
                    webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                    webSocketMap.put("rate", 100);
                    webSocketMap.put("action", "append");
                    webSocketServer.sendInfo(webSocketMap, threadUserId);
                }
                List<CommonExcelModel> excelData = commonExcelSaveFun.getExcelData();
                msg = "excel解析完成";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 40);
                webSocketMap.put("action", "append");
                webSocketServer.sendInfo(webSocketMap, threadUserId);

                // 1.1 list的第一行是表头
                CommonExcelModel columnsMap = excelData.get(0);  // 每个元素是一个字段
                Map<String, Object> map = new JsonKeyUtils().convertEntityToMap(columnsMap);

                Map<Integer, String> dataIndexMap = new HashMap<>();  // 记录列序和data_index的映射
                // 找到对应表头
                for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                    for (int i = 0; i <= 100; i++) {
                        // 文本类型 且 列名相等
                        if ("40".equals(hfDmColumns.getFieldType()) && hfDmColumns.getTitle().equals(map.get("index" + i))) {
                            dataIndexMap.put(i, hfDmColumns.getDataIndex());  // 只保存表头一致的列
                            break;
                        }
                    }
                }
                Date createTime = new Date();
                // 1.2 list的其余行是dataSource
                excelData.remove(0);  // 去除表头
                ArrayList<HfDmData> hfDmDataList = new ArrayList<>();  // 数据入库
                // 获取当前最大order
                Integer maxOrder = uploadCommonExcelAppendThreadUtils.workingTableMapper.getNowDataOrderDao(threadTableId);
                if (maxOrder == null) {
                    maxOrder = 0;
                } else {
                    maxOrder = maxOrder + 1;
                }
                if (excelData.size() != 0) {
                    for (int i = 0; i < excelData.size(); i++) { // n-1行数据
                        CommonExcelModel dataMap = excelData.get(i);
                        Map<String, Object> contentMap = new JsonKeyUtils().convertEntityToMap(dataMap);
                        // 遍历每行数据
                        HfDmData hfDmData = new HfDmData();
                        hfDmData.setDataId(UUID.randomUUID().toString().replaceAll("-", ""));
                        hfDmData.setTableId(threadTableId);
                        hfDmData.setOrder(String.valueOf(maxOrder++));
                        hfDmData.setCreateUserId(threadUserId);
                        hfDmData.setCreateTime(createTime);
                        HashMap<String, Object> dataContentMap = new HashMap<>();
                        for (int j = 0; j <= 100; j++) {
                            if (dataIndexMap.keySet().contains(j)) {
                                // 处理每行数据
                                dataContentMap.put(dataIndexMap.get(j), contentMap.get("index" + j));
                            }
                        }
                        hfDmData.setDataContent(JSON.toJSONString(dataContentMap));
                        if (dataContentMap.size() != 0) {
                            hfDmDataList.add(hfDmData);
                        }

                    }
                }

                msg = "开始写入数据库";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 60);
                webSocketMap.put("action", "append");
                webSocketServer.sendInfo(webSocketMap, threadUserId);
                // 2.数据入库
                int size = hfDmDataList.size();
                if (hfDmDataList.size() != 0) {
                    // 创建数据
                    // 分批插入
                    while (hfDmDataList.size() > 1000) {
                        // 推送入库进度
                        msg = "正在写入数据库（" + (size - hfDmDataList.size()) + "/" + size + "）";
                        System.out.println(msg);
                        webSocketMap.put("msg", msg);
                        webSocketMap.put("title", "数据解析");
                        webSocketMap.put("taskId", threadTaskId);
                        webSocketMap.put("tableId", threadTableId);
                        webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                        webSocketMap.put("rate", 60 + (size - hfDmDataList.size()) * 20 / size);
                        webSocketMap.put("action", "create");
                        webSocketServer.sendInfo(webSocketMap, threadUserId);

                        List<HfDmData> hfDmData = hfDmDataList.subList(0, 1000);
                        uploadCommonExcelAppendThreadUtils.workingTableMapper.uploadHfDmDataDao(hfDmData);
                        hfDmDataList.subList(0, 1000).clear();
                    }
                    uploadCommonExcelAppendThreadUtils.workingTableMapper.uploadHfDmDataDao(hfDmDataList);
                    msg = "写入数据库完成";
                    System.out.println(msg);
                    webSocketMap.put("msg", msg);
                    webSocketMap.put("title", "数据解析");
                    webSocketMap.put("taskId", threadTaskId);
                    webSocketMap.put("tableId", threadTableId);
                    webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                    webSocketMap.put("rate", 80);
                    webSocketMap.put("action", "append");
                    webSocketServer.sendInfo(webSocketMap, threadUserId);
                }
                msg = "上传成功,共解析" + size + "条数据";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 100);
                webSocketMap.put("action", "append");
                webSocketServer.sendInfo(webSocketMap, threadUserId);
            } else {
                // 通知前端
                WebSocketServer webSocketServer = new WebSocketServer();
                long start = System.currentTimeMillis();
                HashMap<String, Object> webSocketMap = new HashMap<>();
                String msg = "开始解析excel";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 20);
                webSocketMap.put("action", "append");
                webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);
                // 目标表的表头
                List<HfDmColumns> dmTableColumnsListDao = uploadCommonExcelAppendThreadUtils.workingTableMapper.getDmTableColumnsListInFileDao(threadTableId, threadFileUuid, threadFileVersionId);
                List<HfDmColumns> dmTableColumnsListDaoTmp = new ArrayList<>();
                for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                    // 过滤掉非文本类型表头
                    if ("40".equals(hfDmColumns.getFieldType())) {
                        dmTableColumnsListDaoTmp.add(hfDmColumns);
                    }
                }
                dmTableColumnsListDao = dmTableColumnsListDaoTmp;

                // 1.数据清洗+数据准备
                CommonExcelSaveFun commonExcelSaveFun = new CommonExcelSaveFun();
                commonExcelSaveFun.clearData();
                try {
                    EasyExcel.read(threadInputStream, CommonExcelModel.class, new CommonExcelListener(commonExcelSaveFun)).password(threadExcelPw).sheet(0).doRead();
                } catch (NoSuchMethodError e) {
                    throw new WorkTableException("上传解析失败，请检查是否存在时间或日期格式的列数据，请调整为文本格式后上传");
                } finally {
                    msg = "上传解析失败，请检查是否存在时间或日期格式的列数据，请调整为文本格式后上传";
                    System.out.println(msg);
                    webSocketMap.put("msg", msg);
                    webSocketMap.put("title", "数据解析");
                    webSocketMap.put("taskId", threadTaskId);
                    webSocketMap.put("tableId", threadTableId);
                    webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                    webSocketMap.put("rate", 100);
                    webSocketMap.put("action", "append");
                    webSocketServer.sendInfo(webSocketMap, threadUserId);
                }
                List<CommonExcelModel> excelData = commonExcelSaveFun.getExcelData();
                msg = "excel解析完成";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 40);
                webSocketMap.put("action", "append");
                webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);
                // 1.1 list的第一行是表头
                CommonExcelModel columnsMap = excelData.get(0);  // 每个元素是一个字段
                Map<String, Object> map = new JsonKeyUtils().convertEntityToMap(columnsMap);

                Map<Integer, String> dataIndexMap = new HashMap<>();  // 记录列序和data_index的映射
                // 找到对应表头
                for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
                    for (int i = 0; i <= 100; i++) {
                        // 文本类型 且 列名相等
                        if ("40".equals(hfDmColumns.getFieldType()) && hfDmColumns.getTitle().equals(map.get("index" + i))) {
                            dataIndexMap.put(i, hfDmColumns.getDataIndex());  // 只保存表头一致的列
                            break;
                        }
                    }
                }
                Date createTime = new Date();
                // 1.2 list的其余行是dataSource
                excelData.remove(0);  // 去除表头
                ArrayList<HfDmData> hfDmDataList = new ArrayList<>();  // 数据入库
                // 获取当前最大order
                String hashMod = new HashUtils().getHashMod(threadFileUuid + threadFileVersionId);
                System.out.println("表编号为: " + hashMod);
                Integer maxOrder = uploadCommonExcelAppendThreadUtils.splitTableMapper.getNowDataOrderInFileDao(threadTableId, threadFileUuid, threadFileVersionId, hashMod, uploadCommonExcelAppendThreadUtils.dmContentDataDatabase);
                if (maxOrder == null) {
                    maxOrder = 0;
                } else {
                    maxOrder = maxOrder + 1;
                }
                if (excelData.size() != 0) {
                    for (int i = 0; i < excelData.size(); i++) { // n-1行数据
                        CommonExcelModel dataMap = excelData.get(i);
                        Map<String, Object> contentMap = new JsonKeyUtils().convertEntityToMap(dataMap);
                        // 遍历每行数据
                        HfDmData hfDmData = new HfDmData();
                        hfDmData.setDataId(UUID.randomUUID().toString().replaceAll("-", ""));
                        hfDmData.setTableId(threadTableId);
                        hfDmData.setOrder(String.valueOf(maxOrder++));
                        hfDmData.setCreateUserId(threadUserId);
                        hfDmData.setCreateTime(createTime);
                        HashMap<String, Object> dataContentMap = new HashMap<>();
                        for (int j = 0; j <= 100; j++) {
                            if (dataIndexMap.keySet().contains(j)) {
                                // 处理每行数据
                                dataContentMap.put(dataIndexMap.get(j), contentMap.get("index" + j));
                            }
                        }
                        hfDmData.setDataContent(JSON.toJSONString(dataContentMap));
                        if (dataContentMap.size() != 0) {
                            hfDmDataList.add(hfDmData);
                        }

                    }
                }

                msg = "开始写入数据库";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 60);
                webSocketMap.put("action", "append");
                webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);
                // 2.数据入库
                int size = hfDmDataList.size();
                if (hfDmDataList.size() != 0) {
                    // 创建数据
                    // 分批插入
                    hashMod = new HashUtils().getHashMod(threadFileUuid + threadFileVersionId);
                    System.out.println("表编号为: " + hashMod);
                    while (hfDmDataList.size() > 1000) {
                        // 推送入库进度
                        msg = "正在写入数据库（" + (size - hfDmDataList.size()) + "/" + size + "）";
                        System.out.println(msg);
                        webSocketMap.put("msg", msg);
                        webSocketMap.put("title", "数据解析");
                        webSocketMap.put("taskId", threadTaskId);
                        webSocketMap.put("tableId", threadTableId);
                        webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                        webSocketMap.put("rate", 60 + (size - hfDmDataList.size()) * 20 / size);
                        webSocketMap.put("action", "create");
                        webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);

                        List<HfDmData> hfDmData = hfDmDataList.subList(0, 1000);
                        uploadCommonExcelAppendThreadUtils.splitTableMapper.uploadHfDmDataInFileDao(hfDmData, threadFileUuid, threadFileVersionId, hashMod, uploadCommonExcelAppendThreadUtils.dmContentDataDatabase);
                        hfDmDataList.subList(0, 1000).clear();
                    }
                    uploadCommonExcelAppendThreadUtils.splitTableMapper.uploadHfDmDataInFileDao(hfDmDataList, threadFileUuid, threadFileVersionId, hashMod, uploadCommonExcelAppendThreadUtils.dmContentDataDatabase);
                    msg = "写入数据库完成";
                    System.out.println(msg);
                    webSocketMap.put("msg", msg);
                    webSocketMap.put("title", "数据解析");
                    webSocketMap.put("taskId", threadTaskId);
                    webSocketMap.put("tableId", threadTableId);
                    webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                    webSocketMap.put("rate", 80);
                    webSocketMap.put("action", "append");
                    webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);
                }
                msg = "上传成功,共解析" + size + "条数据";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 100);
                webSocketMap.put("action", "append");
                webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Thread interrupted.");
        } finally {
            System.out.println("附件上传完成");
        }
        System.out.println("Thread exiting.");
    }

    public void start() {
        System.out.println("Starting ");
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }

    public static void main(String[] args) throws Exception {

        DocxInfoObj docxInfoObj = JOfficeHtmlCreate.parseHtml("<p>　　本招标项目<span class=\"react-renderer node-parameter\"><span as=\"span\" contenteditable=\"false\" class=\"index_parameter__3jTWd index_readOnly__3NVtI\" data-node-view-wrapper=\"\" style=\"text-decoration: none; white-space: normal;\"><span id=\"4d4c2f5a-ddae-43ba-b2ed-64abf9395d9c\" class=\"index_inner__2pfx_\" role=\"button\" tabindex=\"0\" title=\"参数\" data-parameter-key=\"aa990b18ec3a4ebeb1ee9e24c10d0666\" data-parameter-unit=\"0\" data-parameter-type-id=\"\" data-parameter-style-id=\"0\" data-parameter-is-underline=\"0\">项目名称</span></span></span>（项目名称）已由<span class=\"react-renderer node-parameter\"><span as=\"span\" contenteditable=\"false\" class=\"index_parameter__3jTWd index_readOnly__3NVtI\" data-node-view-wrapper=\"\" style=\"text-decoration: none; white-space: normal;\"><span id=\"cec62e56-b9bd-4330-a9a2-9042e384cab0\" class=\"index_inner__2pfx_\" role=\"button\" tabindex=\"0\" title=\"参数\" data-parameter-key=\"4786077f1a744a17a5013c6287191557\" data-parameter-unit=\"0\" data-parameter-type-id=\"\" data-parameter-style-id=\"0\" data-parameter-is-underline=\"0\">项目审批、核准或备案机关名称</span></span></span>（项目审批、核准或备案机关名称）以<span class=\"react-renderer node-parameter\"><span as=\"span\" contenteditable=\"false\" class=\"index_parameter__3jTWd index_readOnly__3NVtI\" data-node-view-wrapper=\"\" style=\"text-decoration: none; white-space: normal;\"><span id=\"81e6bc23-f6f2-40f5-8477-2566b4678d5e\" class=\"index_inner__2pfx_\" role=\"button\" tabindex=\"0\" title=\"参数\" data-parameter-key=\"29c6b69532bd4c67b52628ecdc0ea766\" data-parameter-unit=\"0\" data-parameter-type-id=\"\" data-parameter-style-id=\"0\" data-parameter-is-underline=\"0\">项目批文名称及编号</span></span></span>（批文名称及编号）批准建设，项目业主为<span class=\"react-renderer node-parameter\"><span as=\"span\" contenteditable=\"false\" class=\"index_parameter__3jTWd index_readOnly__3NVtI\" data-node-view-wrapper=\"\" style=\"text-decoration: none; white-space: normal;\"><span id=\"33c42054-8520-4d30-aeab-125dd59bcb9c\" class=\"index_inner__2pfx_\" role=\"button\" tabindex=\"0\" title=\"参数\" data-parameter-key=\"803094d1f9c5445297bb078ca7996f98\" data-parameter-unit=\"0\" data-parameter-type-id=\"\" data-parameter-style-id=\"0\" data-parameter-is-underline=\"0\">项目业主名称</span></span></span> ，建设资金来自 （<span class=\"react-renderer node-parameter\"><span as=\"span\" contenteditable=\"false\" class=\"index_parameter__3jTWd index_readOnly__3NVtI\" data-node-view-wrapper=\"\" style=\"text-decoration: none; white-space: normal;\"><span id=\"ae35e8e5-f9b4-4346-aa64-f93204e98bc8\" class=\"index_inner__2pfx_\" role=\"button\" tabindex=\"0\" title=\"参数\" data-parameter-key=\"31ed502e78d34e6da19d492706b9f8e9\" data-parameter-unit=\"0\" data-parameter-type-id=\"\" data-parameter-style-id=\"0\" data-parameter-is-underline=\"0\">项目资金来源</span></span></span>）（资金来源），项目出资比例为<span class=\"react-renderer node-parameter\"><span as=\"span\" contenteditable=\"false\" class=\"index_parameter__3jTWd index_readOnly__3NVtI\" data-node-view-wrapper=\"\" style=\"text-decoration: none; white-space: normal;\"><span id=\"8c866a2f-5160-4edb-bdeb-e4016fea4bf1\" class=\"index_inner__2pfx_\" role=\"button\" tabindex=\"0\" title=\"参数\" data-parameter-key=\"be83f80228d94f39bdbc289574f8e351\" data-parameter-unit=\"0\" data-parameter-type-id=\"\" data-parameter-style-id=\"0\" data-parameter-is-underline=\"0\">项目出资比例</span></span></span>，招标人为<span class=\"react-renderer node-parameter\"><span as=\"span\" contenteditable=\"false\" class=\"index_parameter__3jTWd index_readOnly__3NVtI\" data-node-view-wrapper=\"\" style=\"text-decoration: none; white-space: normal;\"><span id=\"4b637e05-1f3f-46a5-8810-97a413938b1e\" class=\"index_inner__2pfx_\" role=\"button\" tabindex=\"0\" title=\"参数\" data-parameter-key=\"56b9187eadc84a9fbc5a4ed1089e2716\" data-parameter-unit=\"0\" data-parameter-type-id=\"\" data-parameter-style-id=\"0\" data-parameter-is-underline=\"0\">招标人名称</span></span></span> 。项目已具备招标条件，现对该项目的施工进行公开招标。</p>");
        docxInfoObj.getGenerateResultDocxFilePath();
        docxInfoObj.setFileName("threadName");
        docxInfoObj.saveDocxFile();
    }
}
