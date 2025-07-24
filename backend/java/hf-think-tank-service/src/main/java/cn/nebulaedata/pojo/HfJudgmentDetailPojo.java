package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfJudgmentDetailPojo {
    private String fileUuid;  // 文档id
    private String fileVersionId;  // 版本id
    private String judgmentMethodId;  // 评标办法id
    private String judgmentModuleId;  // 评标模块id
    private String judgmentDetailId;  // 评标细则id
    private String judgmentDetailName;  // 评审因素
    private String judgmentDetailContent;  // 评审内容
    private String bookmarkUuid;  // 引用书签id
    private String paramsUuid;  // 评审对象(参数id)
    private String gradationStage;  // 级差
    private String createUserId;  // 创建人
    private Date createTime;  // 创建时间
    private Date updateTime;  // 修改时间

    /**
     * 评标办法
     */
    private String judgmentMethodName;  // 评标办法名称
    private List<String> outlineIdList;  // 关联章节

    /**
     * 评标模块
     */
    private String judgmentModuleName;  // 评标模块名称
    private String outlineId;  // 关联章节
    private String stepId;  // 评标环节 10初评 20详评
    private String judgmentOrder;  // 评标序列
    private String judgmentType;  // 评标类型 10代表判断 20代表评分
    private String weight;  // 权重占比

    /**
     * 非表字段
     */
    private String outlineIdListStr;  // 关联章节
    private List<HfJudgmentDetailPojo> children;
    private String label;
    private String value;
    private String type;
    private List<String> paramsUuidList;  // 接收多个参数
    private List<String> paramsNameList;  // 参数名
    private String bookmarkName;  // 书签名
    private String bookmarkOutlineId;  // 书签所在段落
    private String sql;

}
