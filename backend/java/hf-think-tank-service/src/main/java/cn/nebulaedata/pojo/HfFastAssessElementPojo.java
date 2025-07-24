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
import java.util.Map;

@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class HfFastAssessElementPojo {
    private String tabUuid;
    private String elementId;
    private String elementType;
    private String elementYinsu;
    private Object elementStandard;
    private Object elementStandardExtra;
    private String quantizationYinsu;
    private Object quantizationStandard;
    private Object quantizationStandardExtra;
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
    private List<HfFastAssessElementPojo> elementList;
    private List<LabelValuePojo> paramsLabelValueList;
    private List<String> elementIdList;
    private List<String> paramsNameList;
    private String standardText;
    private String paramsName;
    private String fileVersionId;
    private String initialNo;
    private Boolean check;

    private List<Map<String,Object>> paramsValueList;
    private String quantizationStandardContent;
}
