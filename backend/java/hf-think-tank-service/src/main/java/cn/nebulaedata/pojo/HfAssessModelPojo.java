package cn.nebulaedata.pojo;

/**
 * @author 徐衍旭
 * @date 2021/9/7 09:38
 * @note
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class HfAssessModelPojo {
    private String assessId;
    private String assessName;
    private String assessMethod;
    private String assessLink;
    private String assessType;
    private String modelType;
    private String clauseNumber;
    private Object label;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private List<String> labelName;

    private String assessMethodName;
    private String assessLinkName;
    private String assessTypeName;
    private String modelTypeName;
}
