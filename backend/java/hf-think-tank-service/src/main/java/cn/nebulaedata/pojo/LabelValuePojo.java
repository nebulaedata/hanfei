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
public class LabelValuePojo {

    public LabelValuePojo() {

    }

    public LabelValuePojo(String label, String value) {
        this.label = label;
        this.value = value;
    }

    private String label;
    private String value;

    private List<LabelValuePojo> children;
    // 外挂
    private String hasunit;
    private String hasoptions;
    private String unitId;
    private String unitName;
    private String annotate;
    private String tableId;
    private String batchId;
    private String projectStageId;
    private String uuid;
    private String type;
    private String paramsText;
    private String modelType;

    private String fileUuid;
    private String fileVersionId;

}
