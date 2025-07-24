package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/1/21 18:04
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfFileLibraryPojo {
    private String uuid;
    private String fileUuid;
    private String fileVersionId;
    private String fileStatus;
    private String auditingStatus;
    private String auditingReason;
    private String auditingUserId;
    private String createUserId;
    private Date   createTime;
    private Date   updateTime;

}
