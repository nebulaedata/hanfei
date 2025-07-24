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
public class HfDmContentUseTablePojo {
    private String uuid;
    private String name;
    private String desc;
    private Object dataSource;
    private Object oriDataSource;
    private Object fields;
    private Object rows;
    private String remark;
    private String fileUuid;
    private String fileVersionId;
    private String outlineId;
    private Boolean updated;  // 是否数据更新
    private String isDel;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private List<Map<String, Object>> actions;

    private List<String> dataSourceName;
    private List<String> fieldsName;

    private String searchUuid;
    private String userId;
    private String tableId;
    private List<String> dataIdList;
}
