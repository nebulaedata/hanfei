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
public class HfUploadModelFilePojo {
    private String uploadUuid;
    private String fileUuid;
    private String fileName;
    private String extension;
    private String folderId;
    private String filePath;
    private Boolean isDel;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String fileVersionId;
    private String result;
    private String fileTypeId;
    private String type;
    private String fileTypeName;
    private String createUserName;
    private String fileVersionName;
    private String url;

}
