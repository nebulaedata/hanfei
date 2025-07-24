package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @author 张发
 * @date 2025/5/27 10:38
 */
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@Data
public class ParamsWorkflowPojo {

    private String fileUuid;

    private String fileVersionId;

    private String workflowJson;

    private String isDel;


}
