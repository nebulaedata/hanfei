package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class HfParamWriteHistoryPojo {
    private String uuid;
    private String fileUuid;
    private String fileVersionId;
    private String paramsUuid;
    private String paramsText;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
}
