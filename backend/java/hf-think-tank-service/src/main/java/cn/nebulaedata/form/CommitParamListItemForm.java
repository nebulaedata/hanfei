package cn.nebulaedata.form;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author 徐衍旭
 * @date 2021/1/20 15:05
 * @note
 */
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class CommitParamListItemForm {
    private String paramsUuid;
    private String paramsText;

    public String getParamsUuid() {
        return paramsUuid;
    }

    public void setParamsUuid(String paramsUuid) {
        this.paramsUuid = paramsUuid;
    }

    public String getParamsText() {
        return paramsText;
    }

    public void setParamsText(String paramsText) {
        this.paramsText = paramsText;
    }
}
