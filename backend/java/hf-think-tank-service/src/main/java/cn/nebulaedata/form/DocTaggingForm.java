package cn.nebulaedata.form;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * @author 贾亦真
 * @date 2021/1/27 16:33
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */

public class DocTaggingForm {

    @NotBlank(message = "文件ID不能为空")
    private String fileUuid;
    @NotBlank(message = "版本ID不能为空")
    private String fileVersionId;
    @NotBlank(message = "段落ID不能为空")
    private String fileParagraphId;
    private String fileParagraphContent;
    private String tipsUuid;
    private String paragraphInterpretation;
    private String userId;
    private Date createTime;
    private Date updateTime;
    private String fileName;
    private String fileVersionName;
    private String tipsName;
    private String tipsDesc;
    private String userName;
    private Integer tagNum;

    public Integer getTagNum() {
        return tagNum;
    }

    public void setTagNum(Integer tagNum) {
        this.tagNum = tagNum;
    }

    public String getTipsName() {
        return tipsName;
    }

    public void setTipsName(String tipsName) {
        this.tipsName = tipsName;
    }

    public String getTipsDesc() {
        return tipsDesc;
    }

    public void setTipsDesc(String tipsDesc) {
        this.tipsDesc = tipsDesc;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileVersionName() {
        return fileVersionName;
    }

    public void setFileVersionName(String fileVersionName) {
        this.fileVersionName = fileVersionName;
    }

    public String getFileUuid() {
        return fileUuid;
    }

    public void setFileUuid(String fileUuid) {
        this.fileUuid = fileUuid;
    }

    public String getFileVersionId() {
        return fileVersionId;
    }

    public void setFileVersionId(String fileVersionId) {
        this.fileVersionId = fileVersionId;
    }

    public String getFileParagraphId() {
        return fileParagraphId;
    }

    public void setFileParagraphId(String fileParagraphId) {
        this.fileParagraphId = fileParagraphId;
    }

    public String getFileParagraphContent() {
        return fileParagraphContent;
    }

    public void setFileParagraphContent(String fileParagraphContent) {
        this.fileParagraphContent = fileParagraphContent;
    }

    public String getTipsUuid() {
        return tipsUuid;
    }

    public void setTipsUuid(String tipsUuid) {
        this.tipsUuid = tipsUuid;
    }

    public String getParagraphInterpretation() {
        return paragraphInterpretation;
    }

    public void setParagraphInterpretation(String paragraphInterpretation) {
        this.paragraphInterpretation = paragraphInterpretation;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
