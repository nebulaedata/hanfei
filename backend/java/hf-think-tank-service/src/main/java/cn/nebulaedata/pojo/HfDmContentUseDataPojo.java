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
public class HfDmContentUseDataPojo {
    private String dataId;
    private String dataContent;
    private String tableId;
    private String order;
    private String createUserId;
    private String fileUuid;
    private String fileVersionId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
}
