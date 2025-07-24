package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 贾亦真
 * @date 2020/12/28 14:50
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfErrLogPojo {


    private String uuid;
    private String errorType;
    private String href;
    private String browser;
    private String error;
    private String userId;
    private Object extraInfo;
    private String createUserId;
    private Date createTime;
    private Date updateTime;


}
