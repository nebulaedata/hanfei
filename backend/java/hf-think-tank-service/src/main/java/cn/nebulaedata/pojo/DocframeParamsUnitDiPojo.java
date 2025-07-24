package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DocframeParamsUnitDiPojo {
    private String unitId;
    private String unitName;
    private String rule;
    private String unitType;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String value;
    private String tagUnitName;
    private String tagUnitId;
    private List<String> nodeIds;
}
