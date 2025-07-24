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
public class HfFileTypeDiPojo {

    /**
     * 表字段
     */
    private String fileTypeId;
    private String fileTypeName;
    private String fileTypeWorkflow;
    private String fileTypeGroupId;
    private String fileTypeGroupName;
    private String inUse;
    private String fileTypeDesc;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */

    private Boolean isUsed;
}
