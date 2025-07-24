package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class ProjectFilePojo {
    private String fileUuid;  // 文件ID
    private String fileName;  // 文件名称
    private String fileExtension;  // 文件后缀名
    private String fileInfo;  // 文件信息
    private String fileType;  // 文件类型
    private String projectId;  // 所属项目ID
    private String packageId;  // 所属项目ID
    private String mainPerson;  // 负责人
    private String createUserId;  // 创建人
    private String filePath;  // 文件全路径
    private Object isStruct;  // 是否结构化 1是0否
    private String isShow;  // 是否展示 1是0否
    private Date createTime;  // 创建时间
    private Date updateTime;  // 修改时间

    /**
     * 非表字段
     */
//    private String stageId;  // 所属标段id
    private String mainPersonName;  // 负责人名称
    private String packageName;  // 所属标段id
    private String createUserName;  // 所属标段id
    private String userId;  // 用户id
    private String url;  // 文件路径
    private String fileTypeName;  // 文件类型名称


    private MultipartFile file;  // 上传的文件
    private String fileClass;  // 文件类型

    private List<DocParamsPojo> paramsInfoList;  // 参数信息

}
