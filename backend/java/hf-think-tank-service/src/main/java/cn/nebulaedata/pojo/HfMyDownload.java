package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2022/7/20 15:50
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class HfMyDownload {
    public HfMyDownload() {
    }
    private String uuid;
    private String userId;
    private String fileUuid;
    private String fileVersionId;
    private String fileName;
    private String filePath;
    private String fileStatus;
    private String fileType;  // 文档类型
    private String fileSize;  // 文档大小
    private String dType;  // 下载模式 0普通 annex附件
    private String ffid;  // 所属文件双id
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String html;
    private String fileTypeName;
    private String downloadType;
    private String type;

}
