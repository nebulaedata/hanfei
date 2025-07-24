package cn.nebulaedata.async;

import cn.nebulaedata.dao.EditToolMapper;
import cn.nebulaedata.dao.SplitTableMapper;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.pojo.*;
import cn.nebulaedata.socket.WebSocketServer;
import cn.nebulaedata.utils.CalculateUtils;
import cn.nebulaedata.utils.HashUtils;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author 徐衍旭
 * @date 2024/1/8 10:12
 * @note
 */

@Component
public class DmTableTask {

    @Autowired
    private WorkingTableMapper workingTableMapper;
    @Autowired
    private EditToolMapper editToolMapper;
    @Autowired
    private SplitTableMapper splitTableMapper;
    @Value("${split-database.dm-content-data}")
    private String dmContentDataDatabase;

    private final ConcurrentHashMap<String, String> concurrentHashMap = new ConcurrentHashMap<>();

    @Async("tableLeftJoinTaskExecutor")
    public void test() {
        for (int i = 0; i <= 20; i++) {
            System.out.println(i); // 输出当前数字

            try {
                Thread.sleep(1000); // 休眠1秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Async("tableLeftJoinTaskExecutor")
    public void tableLeftJoinThread(String leftTableId, String rightTableId, String leftSql, String rightSql, String onSql, String userId, String tableId) {

        try {
            // 生成column信息
            List<HfDmColumns> leftTableColumnsListDao = workingTableMapper.getDmTableColumnsListDao(leftTableId);
            List<HfDmColumns> rightTableColumnsListDao = workingTableMapper.getDmTableColumnsListDao(rightTableId);
            ArrayList<String> leftTableColumnsIdList = new ArrayList<>(); // 记录左表column的id
            HashMap<String, String> leftTableColumnsIdMap = new HashMap<>(); // 记录左表column的id和置换后的id
            leftTableColumnsListDao.forEach(k -> {
                leftTableColumnsIdList.add(k.getDataIndex());
                k.setTableId(tableId);
            });
            for (HfDmColumns hfDmColumns : rightTableColumnsListDao) {
                if (leftTableColumnsIdList.contains(hfDmColumns.getDataIndex())) {
                    String newColumnsId = UUID.randomUUID().toString().replaceAll("-", "");
                    leftTableColumnsIdMap.put(hfDmColumns.getDataIndex(), newColumnsId);
                    hfDmColumns.setDataIndex(newColumnsId);
                    hfDmColumns.setKey(newColumnsId);
                }
                hfDmColumns.setTableId(tableId);
                hfDmColumns.setOrder(String.valueOf(Integer.valueOf(hfDmColumns.getOrder()) + leftTableColumnsListDao.size()));
            }
            leftTableColumnsListDao.addAll(rightTableColumnsListDao);
            workingTableMapper.uploadHfDmColumnsDao(leftTableColumnsListDao);

            List<HfDmData> hfDmDataList = workingTableMapper.tableLeftJoinDao(leftTableId, rightTableId, leftSql, rightSql, onSql);
            Date createTime = new Date();
            Integer i = 0;
            for (HfDmData hfDmData : hfDmDataList) {
                Object dataContentRight = hfDmData.getDataContentRight();  // 右表数据
                if (dataContentRight != null) {  // 右表可能未匹配到
                    Map<String, Object> rightMap = JSON.parseObject(((String) dataContentRight), Map.class);
                    for (String key : leftTableColumnsIdMap.keySet()) {
                        try {  // 防止右表的key为空
                            // 将右表数据替换成新dataIndex
                            rightMap.put(leftTableColumnsIdMap.get(key), rightMap.get(key));
                            rightMap.remove(key);
                        } catch (Exception e) {
                        }
                    }
                    Object dataContentLeft = hfDmData.getDataContentLeft();// 左表数据
                    Map<String, Object> leftMap = JSON.parseObject(((String) dataContentLeft), Map.class);
                    leftMap.putAll(rightMap);
                    hfDmData.setDataContent(JSON.toJSONString(leftMap));
                    hfDmData.setDataContentLeft(null);
                    hfDmData.setDataContentRight(null);
                } else {
                    hfDmData.setDataContent(hfDmData.getDataContentLeft());
                    hfDmData.setDataContentLeft(null);
                    hfDmData.setDataContentRight(null);
                }
                hfDmData.setDataId(UUID.randomUUID().toString().replaceAll("-", ""));
                hfDmData.setTableId(tableId);
                hfDmData.setOrder(String.valueOf(i++));
                hfDmData.setCreateUserId(userId);
                hfDmData.setCreateTime(createTime);
            }

            String taskId = UUID.randomUUID().toString().replaceAll("-", "");
            // 通知前端
            WebSocketServer webSocketServer = new WebSocketServer();
            long start = System.currentTimeMillis();
            HashMap<String, Object> webSocketMap = new HashMap<>();

            int size = hfDmDataList.size();
            if (hfDmDataList.size() != 0) {
                while (hfDmDataList.size() > 1000) {
                    // 推送入库进度
                    String msg = "正在写入数据库（" + (size - hfDmDataList.size()) + "/" + size + "）";
                    System.out.println(msg);
                    webSocketMap.put("msg", msg);
                    webSocketMap.put("title", "数据关联");
                    webSocketMap.put("taskId", taskId);
                    webSocketMap.put("tableId", tableId);
                    webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                    webSocketMap.put("rate", 60 + (size - hfDmDataList.size()) * 20 / size);
                    webSocketMap.put("action", "create");
                    try {
                        webSocketServer.sendInfo(webSocketMap, userId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    List<HfDmData> hfDmData = hfDmDataList.subList(0, 1000);
                    workingTableMapper.uploadHfDmDataDao(hfDmData);
                    hfDmDataList.subList(0, 1000).clear();
                }
                workingTableMapper.uploadHfDmDataDao(hfDmDataList);
                String msg = "写入数据库完成";
                System.out.println(msg);
                webSocketMap.put("msg", msg);
                webSocketMap.put("title", "数据关联");
                webSocketMap.put("taskId", taskId);
                webSocketMap.put("tableId", tableId);
                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
                webSocketMap.put("rate", 100);
                webSocketMap.put("action", "create");
                try {
                    webSocketServer.sendInfo(webSocketMap, userId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            long end = System.currentTimeMillis();
            System.out.println("运行时间：" + (end - start) + "ms");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 公式计算
     *
     * @param tableId
     * @param columnsIdList
     * @param dataId
     * @param userId
     * @throws Exception
     */
    @Async("tableLeftJoinTaskExecutor")
    public void calculateThread(String tableId, List<String> columnsIdList, String dataId, String userId, Boolean notice) throws Exception {
        // 任务id
        String taskId = UUID.randomUUID().toString().replaceAll("-", "");

        concurrentHashMap.put(tableId + "_calculate", taskId);

        List<HfDmColumns> columnsListDao = workingTableMapper.getColumnsListDao(tableId, columnsIdList);
        // 获取key和dataIndex的映射
        List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListDao(tableId);
        HashMap<String, String> key4DataIndexMap = new HashMap<>();
        for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
            key4DataIndexMap.put(hfDmColumns.getKey(), hfDmColumns.getDataIndex());
        }

        // 提取表内数据
        List<HfDmData> dmDataListByTableIdDao = workingTableMapper.getDmDataListByTableIdDao(tableId, dataId);
        // 计算
        for (HfDmData hfDmData : dmDataListByTableIdDao) { // 逐行计算
            // 取全局变量
            // 循环
            if (taskId.equals(concurrentHashMap.get(tableId + "_calculate"))) {
                String dataContent = String.valueOf(hfDmData.getDataContent());
                Map<String, Object> dataContentMap = JSON.parseObject(dataContent, Map.class);
                for (HfDmColumns columnsInfoDao : columnsListDao) { // 需要计算的列
                    if (columnsInfoDao.getKind() != null && "formula".equals(columnsInfoDao.getKind())) {
                        String formula = columnsInfoDao.getFormula();
                        //TODO 判断公式是否合理

                        // 剥离出[key]
                        ArrayList<String> keyList = new ArrayList<>();
                        String regex = "\\[(.*?)\\]";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(formula);
                        while (matcher.find()) {
                            String result = matcher.group(1);
                            if (!keyList.contains(result)) {
                                keyList.add(result);
                            }
                        }

                        try {
                            String colFormula = formula;
                            for (String s : keyList) {
                                Object o = dataContentMap.get(key4DataIndexMap.get(s));
                                if (o == null) {
                                    colFormula = colFormula.replaceAll("\\[" + s + "\\]", "不计算");
                                } else if (o instanceof String) {
                                    colFormula = colFormula.replaceAll("\\[" + s + "\\]", "" + (String) o + "");
                                } else if (o instanceof Map) {
                                    Map<String, Object> o1 = (Map) o;
                                    Set<String> strings = o1.keySet();
                                    for (String s1 : strings) {
                                        colFormula = colFormula.replaceAll("\\[" + s + "\\]", "" + String.valueOf(o1.get(s1)) + "");
                                    }
                                } else if (o instanceof List) {

                                } else {

                                }
                            }
                            System.out.println("----------colFormula---------");
                            System.out.println(colFormula);
                            if (CalculateUtils.checkFormula(colFormula)) {
                                try {
                                    double answer = CalculateUtils.getAnswer(colFormula);
                                    System.out.println(answer);
                                    System.out.println("-------------------");
                                    dataContentMap.put(columnsInfoDao.getDataIndex(), answer);
                                } catch (ArithmeticException arithmeticException) {
                                    // 除以0了
                                    try {
                                        dataContentMap.remove(columnsInfoDao.getDataIndex());
                                    } catch (Exception e) {
                                    }
                                } catch (EmptyStackException emptyStackException) {
                                    // 空的数字栈
                                    try {
                                        dataContentMap.remove(columnsInfoDao.getDataIndex());
                                    } catch (Exception e) {
                                    }
                                } catch (Exception exception) {
                                    // 其他类型报错
                                    try {
                                        dataContentMap.remove(columnsInfoDao.getDataIndex());
                                    } catch (Exception e) {
                                    }
                                }
                            } else {
                                try {
                                    dataContentMap.remove(columnsInfoDao.getDataIndex());
                                } catch (Exception e) {
                                }
                            }
                        } catch (Exception e) {
                            try {
                                dataContentMap.remove(columnsInfoDao.getDataIndex());
                            } catch (Exception e1) {
                            }
                        }
                    } else {
                        throw new WorkTableException("非计算列");
                    }
                }

                String newDataContent = JSON.toJSONString(dataContentMap);
                hfDmData.setDataContent(newDataContent);

                // 更新行数据
                workingTableMapper.chgDmDataDao(hfDmData);

            } else {
                System.out.println("终止执行");
                return;
            }
        }

        // 通知前端
        if (notice) {
            long start = System.currentTimeMillis();
            HashMap<String, Object> webSocketMap = new HashMap<>();
            String msg = "更新数据库完成";
            webSocketMap.put("msg", msg);
            webSocketMap.put("title", "数据计算");
            webSocketMap.put("taskId", taskId);
            webSocketMap.put("tableId", tableId);
            webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
            webSocketMap.put("rate", 100);
            webSocketMap.put("action", "calculate");
            try {
                WebSocketServer.sendInfo(webSocketMap, userId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        // 通知前端
//        WebSocketServer webSocketServer = new WebSocketServer();
//        long start = System.currentTimeMillis();
//        HashMap<String, Object> webSocketMap = new HashMap<>();
//        // 异步更新行数据
//        int size = dmDataListByTableIdDao.size();
//        if (dmDataListByTableIdDao.size() != 0) {
//            while (dmDataListByTableIdDao.size() > 1000) {
//                // 推送入库进度
//                String msg = "正在更新数据库（" + (size - dmDataListByTableIdDao.size()) + "/" + size + "）";
//                System.out.println(msg);
//                webSocketMap.put("msg", msg);
//                webSocketMap.put("taskId", taskId);
//                webSocketMap.put("tableId", tableId);
//                webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
//                webSocketMap.put("rate", 60 + (size - dmDataListByTableIdDao.size()) * 20 / size);
//                webSocketMap.put("action", "create");
//                try {
//                    webSocketServer.sendInfo(webSocketMap, userId);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                List<HfDmData> hfDmData = dmDataListByTableIdDao.subList(0, 1000);
//                workingTableMapper.chgDmDatasDao(hfDmData);
//                dmDataListByTableIdDao.subList(0, 1000).clear();
//            }
//            workingTableMapper.chgDmDatasDao(dmDataListByTableIdDao);
//            String msg = "更新数据库完成";
//            System.out.println(msg);
//            webSocketMap.put("msg", msg);
//            webSocketMap.put("taskId", taskId);
//            webSocketMap.put("tableId", tableId);
//            webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
//            webSocketMap.put("rate", 100);
//            webSocketMap.put("action", "create");
//            try {
//                webSocketServer.sendInfo(webSocketMap, userId);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

    }


    /**
     * 公式计算
     *
     * @param tableId
     * @param columnsIdList
     * @param dataId
     * @param userId
     * @throws Exception
     */
    @Async("tableLeftJoinTaskExecutor")
    public void calculateInFileThread(String tableId, List<String> columnsIdList, String dataId, String userId, String fileUuid, String fileVersionId ,Boolean notice) throws Exception {
        // 任务id
        String taskId = UUID.randomUUID().toString().replaceAll("-", "");

        concurrentHashMap.put(tableId + fileUuid + fileVersionId + "_calculate", taskId);

        List<HfDmColumns> columnsListDao = workingTableMapper.getColumnsListInFileDao(tableId, columnsIdList, fileUuid, fileVersionId);
        // 获取key和dataIndex的映射
        List<HfDmColumns> dmTableColumnsListDao = workingTableMapper.getDmTableColumnsListInFileDao(tableId, fileUuid, fileVersionId);
        HashMap<String, String> key4DataIndexMap = new HashMap<>();
        for (HfDmColumns hfDmColumns : dmTableColumnsListDao) {
            key4DataIndexMap.put(hfDmColumns.getKey(), hfDmColumns.getDataIndex());
        }

        // 提取表内数据
        String hashMod = new HashUtils().getHashMod(fileUuid + fileVersionId);
        System.out.println("表编号为: " + hashMod);
        List<HfDmData> dmDataListByTableIdDao = splitTableMapper.getDmDataListByTableIdInFileDao(tableId, dataId, fileUuid, fileVersionId, hashMod, dmContentDataDatabase);
        // 计算
        for (HfDmData hfDmData : dmDataListByTableIdDao) { // 逐行计算
            // 取全局变量
            // 循环
            if (taskId.equals(concurrentHashMap.get(tableId + fileUuid + fileVersionId + "_calculate"))) {
                String dataContent = String.valueOf(hfDmData.getDataContent());
                Map<String, Object> dataContentMap = JSON.parseObject(dataContent, Map.class);
                for (HfDmColumns columnsInfoDao : columnsListDao) { // 需要计算的列
                    if (columnsInfoDao.getKind() != null && "formula".equals(columnsInfoDao.getKind())) {
                        String formula = columnsInfoDao.getFormula();
                        //TODO 判断公式是否合理

                        // 剥离出[key]
                        ArrayList<String> keyList = new ArrayList<>();
                        String regex = "\\[(.*?)\\]";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(formula);
                        while (matcher.find()) {
                            String result = matcher.group(1);
                            if (!keyList.contains(result)) {
                                keyList.add(result);
                            }
                        }

                        try {
                            String colFormula = formula;
                            for (String s : keyList) {
                                Object o = dataContentMap.get(key4DataIndexMap.get(s));
                                if (o == null) {
                                    colFormula = colFormula.replaceAll("\\[" + s + "\\]", "不计算");
                                } else if (o instanceof String) {
                                    colFormula = colFormula.replaceAll("\\[" + s + "\\]", "" + (String) o);
                                } else if (o instanceof Map) {
                                    Map<String, Object> o1 = (Map) o;
                                    Set<String> strings = o1.keySet();
                                    for (String s1 : strings) {
                                        colFormula = colFormula.replaceAll("\\[" + s + "\\]", "" + String.valueOf(o1.get(s1)));
                                    }
                                } else if (o instanceof List) {

                                } else {

                                }
                            }
                            System.out.println("----------colFormula---------");
                            System.out.println(colFormula);
                            if (CalculateUtils.checkFormula(colFormula)) {
                                try {
                                    double answer = CalculateUtils.getAnswer(colFormula);
                                    System.out.println(answer);
                                    System.out.println("-------------------");
                                    dataContentMap.put(columnsInfoDao.getDataIndex(), answer);
                                } catch (ArithmeticException arithmeticException) {
                                    // 除以0了
                                    try {
                                        dataContentMap.remove(columnsInfoDao.getDataIndex());
                                    } catch (Exception e) {
                                    }
                                } catch (EmptyStackException emptyStackException) {
                                    // 空的数字栈
                                    try {
                                        dataContentMap.remove(columnsInfoDao.getDataIndex());
                                    } catch (Exception e) {
                                    }
                                } catch (Exception exception) {
                                    // 其他类型报错
                                    try {
                                        dataContentMap.remove(columnsInfoDao.getDataIndex());
                                    } catch (Exception e) {
                                    }
                                }
                            } else {
                                try {
                                    dataContentMap.remove(columnsInfoDao.getDataIndex());
                                } catch (Exception e) {
                                }
                            }
                        } catch (Exception e) {
                            try {
                                dataContentMap.remove(columnsInfoDao.getDataIndex());
                            } catch (Exception e1) {
                            }
                        }
                    } else {
                        throw new WorkTableException("非计算列");
                    }
                }

                String newDataContent = JSON.toJSONString(dataContentMap);
                hfDmData.setDataContent(newDataContent);

                // 更新行数据
                hfDmData.setFileUuid(fileUuid);
                hfDmData.setFileVersionId(fileVersionId);
                System.out.println("表编号为: " + hashMod);
                splitTableMapper.chgDmDataInFileDao(hfDmData, hashMod, dmContentDataDatabase);

            } else {
                System.out.println("终止执行");
                return;
            }
        }

        // 通知前端
        if (notice) {
            long start = System.currentTimeMillis();
            HashMap<String, Object> webSocketMap = new HashMap<>();
            String msg = "更新数据库完成";
            webSocketMap.put("msg", msg);
            webSocketMap.put("title", "数据计算");
            webSocketMap.put("taskId", taskId);
            webSocketMap.put("tableId", tableId);
            webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
            webSocketMap.put("rate", 100);
            webSocketMap.put("action", "calculate");
            try {
                WebSocketServer.sendInfo(webSocketMap, userId + "_" + fileUuid + "_" + fileVersionId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 列匹配
     *
     * @param tableId
     * @param columnsIdList
     * @param dataId
     * @param userId
     * @throws Exception
     */
    @Async("tableLeftJoinTaskExecutor")
    public void columnsMatchThread(String tableId, List<String> columnsIdList, String dataId, String userId, String fileUuid,String fileVersionId) throws Exception {
        // 任务id
        String taskId = UUID.randomUUID().toString().replaceAll("-", "");

        long start = System.currentTimeMillis();
        HashMap<String, Object> webSocketMap = new HashMap<>();
        String msg = "更新数据库完成";
        webSocketMap.put("msg", msg);
        webSocketMap.put("title", "数据匹配");
        webSocketMap.put("taskId", taskId);
        webSocketMap.put("tableId", tableId);
        webSocketMap.put("uuid", UUID.randomUUID().toString().replaceAll("-", ""));
        webSocketMap.put("rate", 100);
        webSocketMap.put("action", "match");
        try {
            if (fileUuid != null && fileVersionId != null) {
                WebSocketServer.sendInfo(webSocketMap, userId + "_" + fileUuid + "_" + fileVersionId);
            } else {
                WebSocketServer.sendInfo(webSocketMap, userId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
