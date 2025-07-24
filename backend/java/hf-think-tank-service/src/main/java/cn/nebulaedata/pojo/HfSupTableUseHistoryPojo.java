package cn.nebulaedata.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/1/21 18:04
 * @note
 */

@Data
public class HfSupTableUseHistoryPojo {
    private String uuid;
    private String fileUuid;
    private String fileVersionId;
    private String userId;
    private Object content;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String userName;
}
