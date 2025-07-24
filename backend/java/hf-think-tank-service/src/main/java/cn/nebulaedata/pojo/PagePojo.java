package cn.nebulaedata.pojo;

/**
 * @author 徐衍旭
 * @date 2021/9/7 09:38
 * @note
 */

import lombok.Data;

import java.util.List;

@Data
public class PagePojo {
    private Integer Limit;
    private Integer pageNum=1;
    private Integer pageSize=10;
    private String paramNameLike;
    private String isPaged="1";

    /**
     * 外挂
     */
    private List<String> paramsClassify;
    private String fileUuid;
    private String fileVersionId;
    private String outlineId;

    // 增加筛选参数分组，参数类型，使用场景，填参角色，数据来源
    private Object paramsGroupId;
    private Object paramsTypeId;
    private Object paramsUseSaturation;
    private Object paramsUseSceneId;
    private Object dataSource;

    // 我的空间模板分类筛选
    private Object fileTypeId;
    private List<String> fileLabelList;

    // 根据上线下线状态筛选
    private Object fileLineStatus;

    private String tableId;
    private String batchId;
    private String projectStageId;
    private String type;
    private String status;
    private List<String> label;

    private String assessId;
    private String searchContent;
    private Boolean enabled;

    private Object fileClass;  // 文件模板类型 (模板/母版)

    private Object createTime;
    private Object updateTime;

    private String fastId;
    private String sort;
    private String folderId;
//    private String folderType;
    private Boolean folderFlag;

    private String userId;

    private String noticeType;  // 通知类型

    private Integer level;

    private Boolean all;  // 是否查树
}
