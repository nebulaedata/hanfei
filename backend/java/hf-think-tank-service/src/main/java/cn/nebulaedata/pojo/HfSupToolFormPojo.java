package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author 贾亦真
 * @date 2020/12/28 14:50
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfSupToolFormPojo {

    /**
     * 表字段
     */
    private String formId;
    private String formName;
    private Object formValue;
    private String toolId;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String toolName;
    private String toolText;
    private HashMap<String, String> formIdMap; // 辅助填写传参
    private String userId;
    private String fileUuid;
    private String fileVersionId;
}
