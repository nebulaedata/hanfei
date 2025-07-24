package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class EditToolFolderPojo {
    private String folderId;
    private String folderName;
    private String folderParentId;
    private String folderType;
    private Boolean isDel;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String key;  // folderId
    private String title;  // folderName
    private String type;  // 固定folder
    private String createUserName;
    private List<LabelValuePojo> folderPath;
    private List<String> keyList;

    private Boolean isLeaf;  // 是否为叶节点
    private List<EditToolFolderPojo> children;

    private String result;  // 标记是否为搜索结果 SearchResult NormalResult


    private String label;
    private String value;

    private String extension;  // 后缀 文件夹定义为.folder
    private String fileTypeNameShow;
}
