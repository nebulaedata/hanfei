package cn.nebulaedata.utils;


import cn.jiayizhen.tools.java.office2.html.creation.JOfficeHtmlCreate;
import cn.jiayizhen.tools.java.office2.model.DocxInfoObj;
import cn.nebulaedata.dao.FileOperationMapper;
import cn.nebulaedata.dao.SplitTableMapper;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.pojo.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author 徐衍旭
 * @date 2022/7/20 11:38
 * @note
 */
@Component
public class UploadAnnexParamByDmThreadUtils implements Runnable {

    @Autowired
    private WorkingTableMapper workingTableMapper;
    @Autowired
    private FileOperationMapper fileOperationMapper;
    @Autowired
    private SplitTableMapper splitTableMapper;
    @Value("${split-database.dm-content-data}")
    private String dmContentDataDatabase;

    //静态化工具类变量
    public static UploadAnnexParamByDmThreadUtils uploadAnnexParamByDmThreadUtils;

    //注解用于告诉次代码在Spring加载之前就运行
    @PostConstruct
    public void init() {
        uploadAnnexParamByDmThreadUtils = this;//工具类的实例赋值给fileUtils
        uploadAnnexParamByDmThreadUtils.workingTableMapper = this.workingTableMapper;//会激活Spring对Dao的管理并赋给此类
        uploadAnnexParamByDmThreadUtils.fileOperationMapper = this.fileOperationMapper;//会激活Spring对Dao的管理并赋给此类
        uploadAnnexParamByDmThreadUtils.splitTableMapper = this.splitTableMapper;//会激活Spring对Dao的管理并赋给此类
        uploadAnnexParamByDmThreadUtils.dmContentDataDatabase = this.dmContentDataDatabase;//会激活Spring对Dao的管理并赋给此类
        System.out.println("UploadAnnexParamByDmThreadUtils工具类已经初始化了，被纳入spring管理");

    }

    private Thread t;
    private String threadUid;
    private String threadKey;
    private String threadTableName;
    private String threadFileUuid;
    private String threadFileVersionId;
    private String threadAnnexPath;
    private List<String> threadFields;
    private List<String> threadRows;
    private String threadRemark;

    public UploadAnnexParamByDmThreadUtils() {

    }

    public UploadAnnexParamByDmThreadUtils(String uid, String tableId, String tableName, List<String> fields, List<String> rows, String path, String remark, String fileUuid, String fileVersionId) {
        threadUid = uid;
        threadKey = tableId;
        threadTableName = tableName;
        threadFileUuid = fileUuid;
        threadFileVersionId = fileVersionId;
        threadAnnexPath = path;
        threadFields = fields;
        threadRows = rows;
        threadRemark = remark;
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
            // 0.找表名
//            HfDmTable dmTableInfoDao = workingTableMapper.getDmTableInfoInFileDao(threadKey, threadFileUuid, threadFileVersionId);
//            String tableName = dmTableInfoDao.getTableName();

            // 1.找表头
            List<HfDmColumns> dmTableColumnsList = uploadAnnexParamByDmThreadUtils.workingTableMapper.getDmTableColumnsListInFileDao(threadKey, threadFileUuid, threadFileVersionId);
            List<HfDmColumns> dmTableColumnsListTmp = new ArrayList<>();
            for (HfDmColumns hfDmColumns : dmTableColumnsList) {
                if (threadFields.contains(hfDmColumns.getDataIndex())) {
                    dmTableColumnsListTmp.add(hfDmColumns);
                }
            }


            // 2.找数据
            List<HfDmData> dmDataList = new ArrayList<>();
            if (threadRows == null || threadRows.size() == 0) {  // 如果rows为空 或者长度为0 表示全选
                String hashMod = new HashUtils().getHashMod(threadFileUuid + threadFileVersionId);
                System.out.println("表编号为: " + hashMod);
                dmDataList = uploadAnnexParamByDmThreadUtils.splitTableMapper.getNoSortDataInFileDao(threadKey, null, null, threadFileUuid, threadFileVersionId, hashMod, uploadAnnexParamByDmThreadUtils.dmContentDataDatabase);
            } else { // 否则只保留选择的
                String hashMod = new HashUtils().getHashMod(threadFileUuid + threadFileVersionId);
                System.out.println("表编号为: " + hashMod);
                dmDataList = uploadAnnexParamByDmThreadUtils.splitTableMapper.getNoSortDataInFileDao(threadKey, threadRows, threadRows.size(), threadFileUuid, threadFileVersionId, hashMod, uploadAnnexParamByDmThreadUtils.dmContentDataDatabase);
            }

            // 获取单位信息
            List<DocframeParamsUnitDiPojo> unitDi = uploadAnnexParamByDmThreadUtils.fileOperationMapper.getUnitDiDao();

            // 3.写入excel并保存在服务器上
            HSSFWorkbook sheets = new Excel4DmUtils().writeExcel(threadTableName, dmTableColumnsListTmp, dmDataList, unitDi, threadRemark);
            new Excel4DmUtils().savePathExcel(threadAnnexPath + "/" + threadUid, threadTableName, sheets);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Thread interrupted.");
        } finally {
            System.out.println("附件" + threadUid + "上传完成");
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
