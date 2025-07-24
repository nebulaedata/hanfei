package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfDmTable {
    private String tableId;
    private String tableName;
    private String tableDesc;
    private Boolean showPagination;  // 分页状态
    private Boolean ellipsis;  // 缩略状态
    private String dbId;
    private String order;
    private String createUserId;
    private Boolean isDel;  // 是否删除
    private String fatherTableId;  // 来源表id
    private String fatherFilter;  // 记录来源筛选
    private String fatherGroup;  // 记录来源聚合
    private String sourceTableId;  // 如果是数据管理导入,则记录来源的tableId
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String key;
    private String title;
    private String type;
    private String desc;
    private String createUserName;

    private String dbName;
    private String dbDesc;
    private List<HfDmColumns> columns;

    // 过滤条件
//    private List<HfDmTable> filter;
    private Object filter;
//    private String type;  // and or
    private String field;  // dataIndex
    private String condition;
    private Object value;

    // 上卷聚合
    private Map<String,Object> group;
    // 上卷条件
//    private Object groupBy;  // 维度
    // 聚合条件
//    private Object groupByFunctions;  // 度量

    // 排序条件
//    private List<Map<String,String>> sort;
    private Object sort;

    // 分页条件
    private String isPaged = "1";
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Integer max;

    private String fileUuid;
    private String fileVersionId;

    private String label;
//    private String value;

    private String fromTableId;
    private String toTableId;
    private Boolean overwrite;  // 是否保存成新表

    private List<HfDmTable> children;  // 视图

    private String viewId;
    private String viewName;

    private List<String> targetDb;
    private Boolean includeData;

    private List<String> path;

    private List<String> tableIdList;

    private String fatherKey;


}
