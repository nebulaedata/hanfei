package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfRoomUserPojo {
    private String roomId;
    private String userId;
    private String userPermission;
    private String userStatus;
    private String createUserId;
    private Date createTime;
    private Date updateTime;
}
