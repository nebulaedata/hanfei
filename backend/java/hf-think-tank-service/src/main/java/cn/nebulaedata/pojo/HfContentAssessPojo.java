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
public class HfContentAssessPojo {
    private String fileUuid;
    private String assessMethod;
    private Object assessModelList;
    private Object assessDetailedModelList;
    private Object scoreRat;
    private String priceAssessPlan;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String assessId;
    private String elementType;
    private String paramNameLike;
    private String fileVersionId;
    private String paramsUseSceneId;
    private String outlineId;

    // 提交前检查用字段
    private List<String> checkOptionsList;

}
