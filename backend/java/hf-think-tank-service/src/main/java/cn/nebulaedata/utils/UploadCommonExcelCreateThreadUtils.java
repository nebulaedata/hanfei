package cn.nebulaedata.utils;


import cn.jiayizhen.tools.java.office2.html.creation.JOfficeHtmlCreate;
import cn.jiayizhen.tools.java.office2.model.DocxInfoObj;
import cn.nebulaedata.dao.FileOperationMapper;
import cn.nebulaedata.dao.SplitTableMapper;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.pojo.HfDmColumns;
import cn.nebulaedata.pojo.HfDmData;
import cn.nebulaedata.pojo.HfDmTable;
import cn.nebulaedata.socket.WebSocketServer;
import cn.nebulaedata.web.excel.common.CommonExcelListener;
import cn.nebulaedata.web.excel.common.CommonExcelModel;
import cn.nebulaedata.web.excel.common.CommonExcelSaveFun;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
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
public class UploadCommonExcelCreateThreadUtils implements Runnable {

    @Autowired
    private WorkingTableMapper workingTableMapper;
    @Autowired
    private FileOperationMapper fileOperationMapper;
    @Autowired
    private SplitTableMapper splitTableMapper;
    @Value("${split-database.dm-content-data}")
    private String dmContentDataDatabase;

    //静态化工具类变量
    public static UploadCommonExcelCreateThreadUtils uploadCommonExcelCreateThreadUtils;

    //注解用于告诉次代码在Spring加载之前就运行
    @PostConstruct
    public void init() {
        uploadCommonExcelCreateThreadUtils = this;//工具类的实例赋值给fileUtils
        uploadCommonExcelCreateThreadUtils.workingTableMapper = this.workingTableMapper;//会激活Spring对Dao的管理并赋给此类
        uploadCommonExcelCreateThreadUtils.fileOperationMapper = this.fileOperationMapper;//会激活Spring对Dao的管理并赋给此类
        uploadCommonExcelCreateThreadUtils.splitTableMapper = this.splitTableMapper;//会激活Spring对Dao的管理并赋给此类
        uploadCommonExcelCreateThreadUtils.dmContentDataDatabase = this.dmContentDataDatabase;//会激活Spring对Dao的管理并赋给此类
        System.out.println("UploadCommonExcelCreateThreadUtils工具类已经初始化了，被纳入spring管理");

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

    public UploadCommonExcelCreateThreadUtils() {

    }

    public UploadCommonExcelCreateThreadUtils(String taskId, String dbId, String tableId, String desc, String userId, InputStream inputStream, String excelPw, String fileUuid, String fileVersionId) {
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
                webSocketMap.put("action", "create");
                webSocketServer.sendInfo(webSocketMap, threadUserId);
                // 解析过程
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
                    webSocketMap.put("action", "create");
                    webSocketServer.sendInfo(webSocketMap, threadUserId);
                }
                List<CommonExcelModel> excelData = commonExcelSaveFun.getExcelData();
                String sheetName = commonExcelSaveFun.getSheetName();
                msg = "excel解析完成";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 40);
                webSocketMap.put("action", "create");
                webSocketServer.sendInfo(webSocketMap, threadUserId);
                long end = System.currentTimeMillis();
                System.out.println("excel运行时间：" + (end - start) + "ms");

                // 1.1 list的第一行是表头
                CommonExcelModel columnsMap = excelData.get(0);  // 每个元素是一个字段
                Map<String, Object> map = new JsonKeyUtils().convertEntityToMap(columnsMap);

                ArrayList<HfDmColumns> hfDmColumnsList = new ArrayList<>();  // 表头入库
                ArrayList<String> dataIndexList = new ArrayList<>();  // 记录列序和data_index的映射
                Date createTime = new Date();
                for (int i = 0; i <= 100; i++) {  // CommonExcelModel的列数
                    if (map.get("index" + i) != null) {
                        String title = String.valueOf(map.get("index" + i));
                        HfDmColumns hfDmColumns = new HfDmColumns();
                        hfDmColumns.setTableId(threadTableId);
                        String key = "index" + i;
                        hfDmColumns.setKey(key);
                        hfDmColumns.setTitle(title);
                        hfDmColumns.setDataIndex(key);
                        dataIndexList.add(key);
                        hfDmColumns.setFieldType("40");  // 先默认都是文本类型
                        hfDmColumns.setOptions(null);
                        hfDmColumns.setUnit(null);
                        hfDmColumns.setCreateUserId(threadUserId);
                        hfDmColumns.setCreateTime(createTime);
                        hfDmColumns.setOrder(String.valueOf(i));
                        hfDmColumnsList.add(hfDmColumns);
                    } else {
                        break;
                    }
                }

                // 1.2 list的其余行是dataSource
                excelData.remove(0);  // 去除表头
                ArrayList<HfDmData> hfDmDataList = new ArrayList<>();  // 数据入库
                if (excelData.size() != 0) {
                    Integer order = 0;
                    for (CommonExcelModel CommonExcelModelMap : excelData) {  // n-1行数据
                        Map<String, Object> contentMap = new JsonKeyUtils().convertEntityToMap(CommonExcelModelMap);
                        // 遍历每行数据
                        HfDmData hfDmData = new HfDmData();
                        hfDmData.setDataId(UUID.randomUUID().toString().replaceAll("-", ""));
                        hfDmData.setTableId(threadTableId);
                        hfDmData.setOrder(String.valueOf(order++));
                        hfDmData.setCreateUserId(threadUserId);
                        hfDmData.setCreateTime(createTime);
                        HashMap<String, Object> dataContentMap = new HashMap<>();
                        for (int i = 0; i <= 100; i++) {
                            // 处理每行数据
                            // 行数据和表头不对齐
                            try {
                                dataContentMap.put(dataIndexList.get(i), contentMap.get("index" + i));
                            } catch (Exception e) {
                                break;
                            }
                        }
                        hfDmData.setDataContent(JSON.toJSONString(dataContentMap));
                        hfDmDataList.add(hfDmData);
                    }
                }
                // 删除临时文件
//                file.delete();

                // 2.数据入库
                long start2 = System.currentTimeMillis();
                msg = "开始写入数据库";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 60);
                webSocketMap.put("action", "create");
                webSocketServer.sendInfo(webSocketMap, threadUserId);
                // 创建表
                HfDmTable hfDmTable = new HfDmTable();
                hfDmTable.setTableId(threadTableId);
                hfDmTable.setTableName(sheetName);
                hfDmTable.setDbId(threadDbId);
                hfDmTable.setTableDesc(threadDesc);
                hfDmTable.setCreateUserId(threadUserId);
                uploadCommonExcelCreateThreadUtils.workingTableMapper.createDmTableDao(hfDmTable);
                int size = hfDmDataList.size();
                if (hfDmColumnsList.size() != 0) {
                    // 创建表头
                    uploadCommonExcelCreateThreadUtils.workingTableMapper.uploadHfDmColumnsDao(hfDmColumnsList);
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
                            uploadCommonExcelCreateThreadUtils.workingTableMapper.uploadHfDmDataDao(hfDmData);
                            hfDmDataList.subList(0, 1000).clear();
                        }
                        uploadCommonExcelCreateThreadUtils.workingTableMapper.uploadHfDmDataDao(hfDmDataList);
                        msg = "写入数据库完成";
                        System.out.println(msg);
                        webSocketMap.put("msg", msg);
                        webSocketMap.put("title", "数据解析");
                        webSocketMap.put("taskId", threadTaskId);
                        webSocketMap.put("tableId", threadTableId);
                        webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                        webSocketMap.put("rate", 80);
                        webSocketMap.put("action", "create");
                        webSocketServer.sendInfo(webSocketMap, threadUserId);
                        long end2 = System.currentTimeMillis();
                        System.out.println("入库运行时间：" + (end2 - start2) + "ms");
                    }
                }

                msg = "上传成功,共解析" + size + "条数据";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 100);
                webSocketMap.put("action", "create");
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
                webSocketMap.put("action", "create");
                webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);
                // 解析过程
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
                    webSocketMap.put("action", "create");
                    webSocketServer.sendInfo(webSocketMap, threadUserId);
                }
                List<CommonExcelModel> excelData = commonExcelSaveFun.getExcelData();
                String sheetName = commonExcelSaveFun.getSheetName();
                msg = "excel解析完成";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 40);
                webSocketMap.put("action", "create");
                webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);

                // 1.1 list的第一行是表头
                CommonExcelModel columnsMap = excelData.get(0);  // 每个元素是一个字段
                Map<String, Object> map = new JsonKeyUtils().convertEntityToMap(columnsMap);

                ArrayList<HfDmColumns> hfDmColumnsList = new ArrayList<>();  // 表头入库
                ArrayList<String> dataIndexList = new ArrayList<>();  // 记录列序和data_index的映射
                Date createTime = new Date();
                for (int i = 0; i <= 100; i++) {  // CommonExcelModel的列数
                    if (map.get("index" + i) != null) {
                        String title = String.valueOf(map.get("index" + i));
                        HfDmColumns hfDmColumns = new HfDmColumns();
                        hfDmColumns.setTableId(threadTableId);
                        String key = "index" + i;
                        hfDmColumns.setKey(key);
                        hfDmColumns.setTitle(title);
                        hfDmColumns.setDataIndex(key);
                        dataIndexList.add(key);
                        hfDmColumns.setFieldType("40");  // 先默认都是文本类型
                        hfDmColumns.setOptions(null);
                        hfDmColumns.setUnit(null);
                        hfDmColumns.setCreateUserId(threadUserId);
                        hfDmColumns.setCreateTime(createTime);
                        hfDmColumns.setOrder(String.valueOf(i));
                        hfDmColumnsList.add(hfDmColumns);
                    } else {
                        break;
                    }
                }

                // 1.2 list的其余行是dataSource
                excelData.remove(0);  // 去除表头
                ArrayList<HfDmData> hfDmDataList = new ArrayList<>();  // 数据入库
                if (excelData.size() != 0) {
                    Integer order = 0;
                    for (CommonExcelModel CommonExcelModelMap : excelData) {  // n-1行数据
                        Map<String, Object> contentMap = new JsonKeyUtils().convertEntityToMap(CommonExcelModelMap);
                        // 遍历每行数据
                        HfDmData hfDmData = new HfDmData();
                        hfDmData.setDataId(UUID.randomUUID().toString().replaceAll("-", ""));
                        hfDmData.setTableId(threadTableId);
                        hfDmData.setOrder(String.valueOf(order++));
                        hfDmData.setCreateUserId(threadUserId);
                        hfDmData.setCreateTime(createTime);
                        HashMap<String, Object> dataContentMap = new HashMap<>();
                        for (int i = 0; i <= 100; i++) {
                            // 处理每行数据
                            // 行数据和表头不对齐
                            try {
                                dataContentMap.put(dataIndexList.get(i), contentMap.get("index" + i));
                            } catch (Exception e) {
                                break;
                            }
                        }
                        hfDmData.setDataContent(JSON.toJSONString(dataContentMap));
                        hfDmDataList.add(hfDmData);
                    }
                }

                // 2.数据入库
                msg = "开始写入数据库";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 60);
                webSocketMap.put("action", "create");
                webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);
                // 创建表
                HfDmTable hfDmTable = new HfDmTable();
                hfDmTable.setTableId(threadTableId);
                hfDmTable.setTableName(sheetName);
                hfDmTable.setDbId(threadDbId);
                hfDmTable.setTableDesc(threadDesc);
                hfDmTable.setCreateUserId(threadUserId);
                hfDmTable.setFileUuid(threadFileUuid);
                hfDmTable.setFileVersionId(threadFileVersionId);
                uploadCommonExcelCreateThreadUtils.workingTableMapper.createDmTableInFileDao(hfDmTable);
                int size = hfDmDataList.size();
                if (hfDmColumnsList.size() != 0) {
                    // 创建表头
                    uploadCommonExcelCreateThreadUtils.workingTableMapper.uploadHfDmColumnsInFileDao(hfDmColumnsList, threadFileUuid, threadFileVersionId);
                    if (hfDmDataList.size() != 0) {
                        // 创建数据
                        // 分批插入
                        String hashMod = new HashUtils().getHashMod(threadFileUuid + threadFileVersionId);
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
                            uploadCommonExcelCreateThreadUtils.splitTableMapper.uploadHfDmDataInFileDao(hfDmData, threadFileUuid, threadFileVersionId, hashMod, uploadCommonExcelCreateThreadUtils.dmContentDataDatabase);
                            hfDmDataList.subList(0, 1000).clear();
                        }
                        uploadCommonExcelCreateThreadUtils.splitTableMapper.uploadHfDmDataInFileDao(hfDmDataList, threadFileUuid, threadFileVersionId, hashMod, uploadCommonExcelCreateThreadUtils.dmContentDataDatabase);
                        msg = "写入数据库完成";
                        System.out.println(msg);
                        webSocketMap.put("msg", msg);
                        webSocketMap.put("title", "数据解析");
                        webSocketMap.put("taskId", threadTaskId);
                        webSocketMap.put("tableId", threadTableId);
                        webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                        webSocketMap.put("rate", 80);
                        webSocketMap.put("action", "create");
                        webSocketServer.sendInfo(webSocketMap, threadUserId + "_" + threadFileUuid + "_" + threadFileVersionId);
                    }
                }
                msg = "上传成功,共解析" + size + "条数据";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据解析");
                webSocketMap.put("taskId", threadTaskId);
                webSocketMap.put("tableId", threadTableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 100);
                webSocketMap.put("action", "create");
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
