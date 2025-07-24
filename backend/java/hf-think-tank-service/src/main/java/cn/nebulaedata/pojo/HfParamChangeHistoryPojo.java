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
public class HfParamChangeHistoryPojo {
    private String uuid;
    private String paramsUuid;
    private String paramsChoose;
    private String paramsText;
    private String paramsTypeId;
    private String newParamsText;  // 要展示的值
    private String fileUuid;
    private String fileVersionId;
    private String createUserId;
    private Date createTime;
    private Date updateTime;
}
