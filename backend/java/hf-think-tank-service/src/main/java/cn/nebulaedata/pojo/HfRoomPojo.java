package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfRoomPojo {
    private String roomId;
    private String fileUuid;
    private String fileVersionId;
    private String roomStatus;
    private String adminUserId;
    private List<Map<String,Object>> userList;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String noticeUserId;
    private String applyUserId;
    private String permission;
    private List<Map<String,Object>> inviteUserList;
    private Boolean result;
    private Boolean admin;

    private Object settings;

}
