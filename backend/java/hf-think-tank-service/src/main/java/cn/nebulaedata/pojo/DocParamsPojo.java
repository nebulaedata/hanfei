package cn.nebulaedata.pojo;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class DocParamsPojo {
    private String paramsUuid;
    private String createUserId;
    private String createCompanyId;
    private String paramsName;
    private String paramsDesc;
    private String paramsTypeId;
    private String paramsUseSceneId;
    private String paramsGroupId;
    private String isNecessary;
    private String isUnderLine;
    private Object dataSource;
    private String paramsRange;
    private Object paramsColumns;
    private String matrixDisplay;
    private String matrixMode;
    private String paramsClassify;
    private Date createTime;
    private Date updateTime;
    private String showText;
    private String staticParamsUuid;
    private String styleId;
    private String unit;
    private String remark;
    private String isInit;
    private String paramsUseSaturation;
    private Object defaultValue;
    private String oriFileUuid;
    private String oriFileName;
    private Object comeFromList;  // 来自于（可多选母版名称）
    private String isIgnore;  // 是否忽略 1是 0否

    /**
     * 非表字段
     */
    private List<String> comeFromListName;  // 来自于（可多选母版名称）
    private Object paramsText;
    private String paramsChoose;
    private List<String> paramsChooseList;
    private String lastParamsText;
    private String lastParamsChoose;
    private List<String> lastParamsChooseList;
    private String paramsUseSceneName;
    private String paramsTypeName;
    private String dataSourceName;
    private List<Object> paramsRangeList;
    private List<Object> paramsGroupIdList;
    private Boolean isCanEdit;
    private List<String> paramsGroupNameList;
    private String userId;
    private String fileUuid;
    private String fileVersionId;
    private String fileVersionName;
    private String outlineId;
    private String writeUserId;
    private String Type;
    private String orderMode;
    private String cnt;
    private String contentText;
    private String uuid;
    private List<Map<String, String>> uuidList;
    private int uuidListLength;
    private List<Map<String, String>> actions;
    private String searchUuid;
    private String newParamsUuid;
    private String isDel;
    private String styleContent;
    private String all;
    private String outlineOrder;
    private String isWrite;
    private String hasTime;  // 是否精确到时间
    private String isChange;  // 是否发生了变化
    private String getChange;  // 是否获取变化的
    private Object paramsTextList;
    //    private List<Map> paramsTextListMap;
    private List<Map<String, String>> unitList;
    private String unitName;
    private Object dataSourceList;
    private String paramsUseSaturationName;
    private String createUserName;
    private String fileName;
    private String writeUserName;
//    private List<Map<String, Object>> unitValueList;
    private Object unitValueList;
    private String historyContent;
    private String key;
    private String timeFormat;


    public void setDataSourceList(Object dataSourceList) {
        this.dataSource = JSON.toJSONString(dataSourceList);
    }

    public Object getDataSource() {
        if (dataSource == null) {
            return null;
        }
        if (dataSource instanceof String) {
            this.dataSourceList = JSON.parseObject(StringEscapeUtils.unescapeJava((String) dataSource), List.class);
            return dataSource;  // 返回给接口 List
        }
        return JSON.toJSONString(dataSource);  // 返回给数据库 String
    }


    private Boolean changeFlag;
//    private Boolean getAllFlag = false;
    private String desc;

    private String writeWay;  // 填写途径 team协同 null普通

    /**
     * 搜索关键字
     */
    private String paramNameLike;

    /**
     * 填写的内容是否为ai自动生成
     */
    private String isAiContent;

}
