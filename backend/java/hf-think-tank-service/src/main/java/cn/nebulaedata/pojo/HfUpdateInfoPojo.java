package cn.nebulaedata.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/1/21 18:04
 * @note
 */

@Data
public class HfUpdateInfoPojo {
    private String updateUuid;
    private String fileUuid;
    private String updateOutlineId;
    private String updateOutlineIdAct;
    private String pushFileUuid;
    private String status;
    private String userId;
    private Date createTime;
    private Date updateTime;
}
