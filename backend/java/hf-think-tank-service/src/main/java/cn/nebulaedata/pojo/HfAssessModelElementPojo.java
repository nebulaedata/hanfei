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
public class HfAssessModelElementPojo {
    private String elementId;
    private String elementType;
    private String elementYinsu;
    private Object elementStandard;
    private String elementStandardExtra;
    private String quantizationYinsu;
    private Object quantizationStandard;
    private String quantizationStandardExtra;
    private String quantizationRules;
    private Object gradation;
    private Object thresholdValue;
    private Object paramsIdList;
    private String assessId;
    private String order;
    private String elementTypeOrder;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非标字段
     */
    private List<HfAssessModelElementPojo> elementList;
    private List<String> elementTypeList;
    private String paramsUseSceneId;
    private String standardText;
    private List<String> paramsNameList;
}
