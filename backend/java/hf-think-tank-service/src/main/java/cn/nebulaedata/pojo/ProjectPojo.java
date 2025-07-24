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
public class ProjectPojo {
    private String projectId;  // 项目ID
    private String projectNo;  // 项目编号
    private String projectName;  // 项目名称
    private String projectInfo;  // 项目简介
    private String projectStatus;  // 项目状态
    private String modelId;  // 选用的模板ID
    private Object includeUserId;  // 项目关系人
    private String createUserId;  // 创建人
    private Object biddingCompAgency;  // 代理公司(list)
    private Object biddingUserAgency;  // 代理人(list)
    private String mainPerson;  // 负责人
    private String filePath;  // 项目文件路径
    private String spaceSize;  // 项目空间大小(MB)
    private String projectType;  // 项目类型
    private String projectAddress;  // 项目建设地点
    private String projectBudget;  // 项目预算(万元)
    private String projectStartDate;  // 项目开工日期
    private String projectEndDate;  // 项目竣工日期
    private String hasChild;  // 有无子项目 0/1
    private Date createTime;  // 创建时间
    private Date updateTime;

    /**
     * 非表字段
     */
    private List<StagePojo> children;

    private String usedSize;  // 已使用容量
    private String totalSize;  // 总容量
    private String usedRate;  // 使用率
    private String createUserName;  // 项目创建人
    private String mainPersonName;  // 项目创建人
    private String timeText;  // 展示用的时间
    private String packageCount;  // 分包数量
    private String fileCount;  // 文件数量
    private Object includeUserName;  // 项目关系人名称

}
