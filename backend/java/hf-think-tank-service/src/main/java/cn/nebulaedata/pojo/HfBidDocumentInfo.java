package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class HfBidDocumentInfo {
    private String fileUuid;  // 文档id
    private String fileVersionId;  // 版本id
    private String projectId;  // 所属项目ID
    private String projectName;  // 所属项目名称
    private String zbrCompanyId;  // 招标人企业id
    private String zbrCompanyName;  // 招标人企业名称
    private String jgCompanyId;  // 招标代理机构企业id
    private String jgCompanyName;  // 招标代理机构企业名称
    private String batchId;  // 所属批次ID
    private String batchName;  // 所属批次名称
    private String packageId;  // 分包ID
    private String packageName;  // 分包名称
    private String createUserId;  // 创建人
    private Date createTime;    // 创建时间
    private Date updateTime;  // 修改时间

    /**
     * 非表字段
     */
    private String fileName;  // 文档名称
    private String createUserName;  // 创建人名称
    private String modelName; // 模板名称
    private String biddingType; // 采购类别
    private String biddingStyle;  // 采购方式
    private String judgmentMethod;  // 评标办法
}
