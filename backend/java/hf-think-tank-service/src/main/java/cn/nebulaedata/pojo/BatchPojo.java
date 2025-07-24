package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class BatchPojo {
    private String batchId;  // 批次ID
    private String batchName;  // 批次名称
    private String batchNo;  // 批次编号
    private String projectId;  //
    private Object includeUserId;  // 批次参与人
    private Object includeUserInfo;  // 批次参与人信息
    private String createUserId;  // 创建人
    private String batchState;  // 所处状态 1策划阶段 2招标阶段 3投标阶段 4开标阶段 5评标阶段 6定标阶段
    private Object batchProperty;  // 批次属性
//    private String buyWay;  // 采购方式
//    private String location;  // 开评标地点
//    private String publishDate;  // 招标文件发布日期
//    private String signUpEndDate;  // 报名截止日期
//    private String startDate;  // 开标日期
//    private String businessType;  // 业务类别
    private String mainPerson;  // 负责人
    private String batchManager;  // 批次经理
    private String projectUnit;  // 项目单位
    private String filePath;  // 批次文件存放路径
    private String spaceSize;  // 项目空间大小(MB)
    private Object projectStageList;  // 项目标段信息
    //    private String groupId;  // 组别
    private Date createTime;  // 创建时间
    private Date updateTime;  //

    /**
     * 非表字段
     */
    private String mainPersonName;  // 负责人姓名
    private String userId;  //
    private String projectStageId;  //
    private String projectStageName;  //
    private String buyType;  //
}
