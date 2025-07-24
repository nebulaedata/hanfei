package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class ContentPojo {
    private String contentId;
    private String contentText;
    private String outlineId;
    private String fileUuid;
    private String fileVersionId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表数据
     */
    private String searchUuid;
    private String userId;
}
