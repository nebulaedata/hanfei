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
public class StagePojo {
    private String stageId;  // 标段ID
    private String stageName;  // 标段名称
    private String batchId;  // 所属批次ID
    private String projectId;  // 所属项目ID
    private String fenbiaoId;  // 所属分标ID
    private String modelId;  // 选用的模板ID
    private String includeUserId;  // 标段负责人
    private String createUserId;  // 创建人
    private String stageInfo;  // 标段简介
    private String stageType1;  // 标段类型
    private String stageType2;  // 标段类别
    private String stageBudget;  // 预算(万元)
    private String stageAddress;  // 建设地点
    private String stageBuyWay;  // 采购方式
    private String stageStartDate;  // 开工日期
    private String stageFinishDate;  // 竣工日期
    private Date createTime;  // 创建时间
    private Date updateTime;  // 修改时间

}
