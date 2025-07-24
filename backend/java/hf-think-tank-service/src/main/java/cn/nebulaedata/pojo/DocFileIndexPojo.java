package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
//import org.neo4j.ogm.annotation.GeneratedValue;
//import org.neo4j.ogm.annotation.Id;
//import org.neo4j.ogm.annotation.NodeEntity;
//import org.neo4j.ogm.annotation.Property;

import java.util.*;

/**
 * @author 贾亦真
 * @date 2020/12/28 14:52
 * @note
 */
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@Data
public class DocFileIndexPojo {

    private String fileUuid;
    private String fileVersionId;
    private String includeUserList;
    private String suitRole;
    private String suitType;
    private String suitUser;
    private String suitWay;
    private String suitBidMethod;
    private String createUserId;
    private String updateUserId;
    private String fileName;
    private String fileTypeId;
    private String fileTypeName;
    private String fileParentId;
    private String isRootFile;
    private String fileStatus;
    private String ancestorsFileUuid;
    private Date createTime;
    private Date updateTime;
    private String fileClass;
    private String fileCompanyId;
    private String fileLabelList;
    private String fileUseRangeId;
    private String fileUseRangeName;
    private String fileUseRangeText;
    private String fileDesc;
    private String mainFileUuid;
    private String mainFileVersionId;
    private String mainFileUuid2;
    private String mainFileVersionId2;
    private String templateTypeId;


    /**
     * 非表字段
     */
    private String createUserName;
    private String updateUserName;
    private String fileVersionName;
    private String fileStatusName;
    private String fileUseSceneId;
    private String fileUseSceneName;
    private String fileLabelName;
    private String status;
    private List<DocFileIndexPojo> children;
    private DocFileVerIndexPojo versions;
    private Integer collectNum;
    private String cover;
    private String isDraft;
    private List<String> fileUseRangeIds;
    private List<String> fileUseSceneIds;
    private List<String> fileLabelIds;
    private HashMap fileText;
    private String outlineId;
    private String similar;   // 相似度
    private String searchLike;
    private String searchUserId;  // 区分搜索用户
    private String searchUuid;  // 区分搜索
    private Boolean origin;
    private String includeUserListName;
    private List<String> outlineListMap;
    private List<Map> outlineIdListMap;
    private String oldFileUuid;
    private String oldFileVersionId;
    private String outlineIdListStr;
    private Boolean primary;
    private String versionInfo;
    private List<String> includeUserIdList;
    private String draftVersionId;

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

    private Boolean recommend; // 是否推荐
    private String finalFlag; // 为null是招标文件大保存 为1是招标文件提交

    private Boolean isFinish; // 是否为完结版本
    private Object biddingType; // 采购类别
    private Object biddingStyle;  // 采购方式
    private Object judgmentMethod;  // 评标办法

    private List<DocFileIndexPojo> docFileIndexPojoList;
    private List<String> chapters;  // 选中的投标文件章节

    private String showStatus;  // 前端展示的文章状态

    // 文件复合使用
    private String fileUuid2;
    private String fileVersionId2;
    private Boolean includeParam = true;
    private List<String> outlineIdList2;

    // 控制流程
    private String flag;

    // 文件上传信息
    private Map<String,Object> file;

    private List<Map<String,Object>> merge;  // 存放自定义模板合并的数据
    private Boolean buildSub;  // 选择该模板是否生成附属模板
    private String autoFlag = "0";  // 默认为0
    private String staticFileVersionId;  //
    private String staticFileUuid;  //

    // 投递
    private List<Map<String,String>> fileList;

    private int collectionCnt; // 收藏次数
    private int derivationCnt; // 派生次数
    private int useCnt; // 应用次数
    private String fileTypeNameNew;

    private int cnt; // 统计数量
    private String createDate; // 统计日期

    private Boolean collection;  // 是否收藏

    private Boolean edit=true;  // 是否具有编辑权限

    // 文库审批
    private String auditingStatus;  // 文档审核状态
    private String auditingReason;  // 文档审核意见
    private String auditingTime;  // 文档审核时间
    private String fileLineStatus;  // 文档在线状态
    private String fileLineStatusName;  // 文档在线状态
    private String fileVersionNameOnline;  // 文档上线版本
    private String isRecommend;  // 是否推荐
    private List<LabelValuePojo> fileVersionList;  // 文档版本集合

    private String compareInfo;
    private String annotate;
    private Object auditingContent;


    // 招采库
    private String id;
    private Integer level;
    private List<String> labelList;

    private String timeFormat;  // 最近更新时间
    private String fileTypeWorkflow;  // 流程类型
    private String assessStatus;  // 流程类型
    private String folderId;
    private String key;
    private ArrayList<LabelValuePojo> folderPath;

    private String result;  // 标记是否为搜索结果 SearchResult NormalResult

    private String derivationNumber;  // 派生数量
    private String starNum;  // 收藏数量
    private String type;  // 收藏数量

    private Boolean isDel;
    private String filePath;
    private String extension;  // 后缀
    private String url;  // 资源地址
    private String fileTypeNameShow;  // 类型展示字段
}
