package cn.nebulaedata.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/1/21 18:04
 * @note
 */

@Data
public class HfJudgePojo {
    private String judgeId;
    private String judgeName;
    private String batchId;
    private String projectStageId;
    private Object fileUuidList;
    private String assessId;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String assessName;
    private String projectStageName;
    private String createUserName;
    private List<LabelValuePojo> fileUuidLVList;

}
