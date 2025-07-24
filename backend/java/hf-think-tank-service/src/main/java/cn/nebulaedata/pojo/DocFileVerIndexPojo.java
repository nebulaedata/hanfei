package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author 贾亦真
 * @date 2021/1/11 14:55
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class DocFileVerIndexPojo {

    private String fileUuid;
    private String fileVersionId;
    private String fileVersionName;
    private Date createTime;
    private Date updateTime;
    private String fileVersionDesc;
    private String parentsVersionId;
    private String isRootVersion;
    private String createUserId;
    private String updateUserId;
    private String isDraft;
    private String versionStatus;
    private String versionInfo;
    private String submitUserId;
    private Date submitTime;
    private String confirmUserId;
    private Date confirmTime;
    private Object compareInfo;
    private Object annotate;


    /**
     * 非表字段
     *
     * @return
     */
    private String createUserName;
    private HashMap fileText;
    // versionStatus -正式版本 -草稿 -审批中 -历史版本
    private String fileName;
    private String fileParentId;
    private String fileTypeId;
    private List<String> fileUseRangeIds;
    private List<String> fileUseSceneIds;
    private List<String> fileLabelIds;
    private List<DocFileVerIndexPojo> child;
    private String orderList;
    private String fileCompletePath;
    private String confirmUserName;
    private String mode;
    private String timeText;
    private String userName;
    private Object auditingContent;
    private String auditingUserName;
    private String auditingTime;
    private String auditingStatus; // 审核状态
    private Boolean online; // 在线状态
}
