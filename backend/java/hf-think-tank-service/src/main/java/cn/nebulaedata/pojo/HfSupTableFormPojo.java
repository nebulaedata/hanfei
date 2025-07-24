package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 贾亦真
 * @date 2020/12/28 14:50
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfSupTableFormPojo {

    /**
     * 表字段
     */
    private String formId;
    private String formName;
    private Object formValue;
    private String tableId;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String title;
    private Object enabled;
    private Object columns;
    private String userId;
    private String fileUuid;
    private String fileVersionId;
    private String formCnt="0";
    private List<String> formIdList;
    private List<Map<String,String>> recentlyForm;
    private Object label;
    private Object useCnt;
    private List<String> labelNameList;
    private List<DocParamsPojo> paramsPojoList;
    private Map<String,Object> retList;

    private String unit;
    private String unitName;
}
