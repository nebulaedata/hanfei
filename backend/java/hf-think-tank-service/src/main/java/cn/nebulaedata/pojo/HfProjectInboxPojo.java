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
public class HfProjectInboxPojo {

    /**
     * 表字段
     */
    private String uuid;
    private String fileUuid;
    private String fileVersionId;
    private String fileName;
    private String fileTypeId;
    private String projectId;
    private String batchId;
    private String statusId;
    private String oldFileUuid;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String status;
    private String userName;
    private String projectName;
    private String batchName;
}
