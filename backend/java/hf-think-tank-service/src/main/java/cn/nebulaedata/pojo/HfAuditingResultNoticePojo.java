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
public class HfAuditingResultNoticePojo {

    private String auditingUuid;
    private String auditingContent;
    private String fileUuid;
    private String fileVersionId;
    private String noticeUserId;
    private Boolean isRead;
    private String noticeType;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String fileTypeId;
    private String fileName;
}
