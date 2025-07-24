package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/11/8 14:33
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class DocParamsTypeStylePojo {
    private String styleId;
    private String styleContent;
    private String unitId;
    private String hasTime;
    private String paramsTypeId;
    private Date createTime;
    private Date updateTime;
}
