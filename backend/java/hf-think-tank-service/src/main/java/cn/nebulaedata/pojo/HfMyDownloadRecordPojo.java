package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 贾亦真
 * @date 2020/12/28 14:50
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfMyDownloadRecordPojo {

    private String logId;
    private String uuid;
    private String userId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String fileName;
    private String fileType;
    private String userName;
}
