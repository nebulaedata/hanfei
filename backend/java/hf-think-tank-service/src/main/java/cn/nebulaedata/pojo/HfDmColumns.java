package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class HfDmColumns {
    private String tableId;
    private String key;
    private String title;
    private String dataIndex;
    private String fieldType;
    private Object options;
    private Object unit;
    private String order;
    private String defaultValue;
    private Boolean required;
    private Boolean visible;
    private String kind;  // 字段类型(公式:formula 其他:null)
    private String formula;  // 公式
    private String createUserId;
    private String matchType;  // 匹配类型
    private String matchDbId;  // 匹配范围(数据库)
    private String matchFolderId;  // 匹配范围(文件夹)
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private List<Map<String,Object>> columns;
    private Boolean showPagination;
    private Boolean ellipsis;

    private String fileUuid;
    private String fileVersionId;

    private String label;
    private String value;
    private String columnsId;
    private String dataId;
    private String execute;
}
