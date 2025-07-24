package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class PackagePojo {
    private String packageId;
    private String packageName;
    private String packageInfo;
    private String packageType1;
    private String packageType2;
    private String packageBudget;
    private String packageAddress;
    private String packageBuyWay;
    private String packageStartDate;
    private String packageFinishDate;
    private String batchId;
    private String projectId;
    private String modelId;
    private Object includeUserId;
    private String createUserId;
    private String mainPerson;
    private String stageId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private Object includeUserName;  // 关系人名称
    private String createUserName;  // 创建人名称
    private String mainPersonName;  // 负责人名称
    private Object packagePlan;  // 项目策划
    private Object projectName;  // 项目名称

}
