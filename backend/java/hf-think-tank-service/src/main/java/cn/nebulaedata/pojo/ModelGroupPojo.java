package cn.nebulaedata.pojo;

import cn.nebulaedata.anno.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

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
public class ModelGroupPojo {
    private String modelGroupId;
    private String fileUuid;
    private String modelGroupName;
    private Object fileUuidList;
    private String fileUseRangeId;
    private Date createTime;
    private Date updateTime;

    // 非表字段
    private List<Map<String,Object>> nodes;  // 存点信息
    private List<Map<String,String>> edges;  // 存线信息

    private List<String> labels;  // 标签入参信息

    private Object fileNameList;
}
