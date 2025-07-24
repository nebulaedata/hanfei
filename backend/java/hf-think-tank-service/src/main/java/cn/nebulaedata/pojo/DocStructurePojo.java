package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class DocStructurePojo {
    private String fileVersionId;
    private String fileUuid;
    private String fileText;
    private Map fileTextMap;
    private String fileName;
    private String userName;
    private String fileVersionName;
    private String createUserName;
    private String updateUserName;
    private Boolean isCollect;
    private String parentFileName;
    private String fileTypeId;
    private String fileTypeName;
    private String fileUseRangeId;
    private String fileUseSceneId;
    private String fileLabelId;
    private String versionInfo;
    private String idEdit="0";
    private Date createTime;
    private Date updateTime;
    private String isDraft;
    private String fileParentId;
    private List<String> fileUseRangeIds;
    private List<String> fileUseSceneIds;
    private List<String> fileLabelIds;
    private String orderList;
    private String fileCompletePath;

    public String getIdEdit() {
        return idEdit;
    }

    public void setIdEdit(String idEdit) {
        this.idEdit = idEdit;
    }

    public String getOrderList() {
        return orderList;
    }

    public void setOrderList(String orderList) {
        this.orderList = orderList;
    }

    public String getFileCompletePath() {
        return fileCompletePath;
    }

    public void setFileCompletePath(String fileCompletePath) {
        this.fileCompletePath = fileCompletePath;
    }

    public String getFileTypeName() {
        return fileTypeName;
    }

    public void setFileTypeName(String fileTypeName) {
        this.fileTypeName = fileTypeName;
    }

    public String getFileParentId() {
        return fileParentId;
    }

    public void setFileParentId(String fileParentId) {
        this.fileParentId = fileParentId;
    }

    public List<String> getFileUseRangeIds() {
        return fileUseRangeIds;
    }

    public void setFileUseRangeIds(List<String> fileUseRangeIds) {
        this.fileUseRangeIds = fileUseRangeIds;
    }

    public List<String> getFileUseSceneIds() {
        return fileUseSceneIds;
    }

    public void setFileUseSceneIds(List<String> fileUseSceneIds) {
        this.fileUseSceneIds = fileUseSceneIds;
    }

    public List<String> getFileLabelIds() {
        return fileLabelIds;
    }

    public void setFileLabelIds(List<String> fileLabelIds) {
        this.fileLabelIds = fileLabelIds;
    }

    public String getIsDraft() {
        return isDraft;
    }

    public void setIsDraft(String isDraft) {
        this.isDraft = isDraft;
    }

    public Boolean getCollect() {
        return isCollect;
    }

    public void setCollect(Boolean collect) {
        isCollect = collect;
    }

    public String getParentFileName() {
        return parentFileName;
    }

    public void setParentFileName(String parentFileName) {
        this.parentFileName = parentFileName;
    }

    public String getFileTypeId() {
        return fileTypeId;
    }

    public void setFileTypeId(String fileTypeId) {
        this.fileTypeId = fileTypeId;
    }

    public String getFileLabelId() {
        return fileLabelId;
    }

    public void setFileLabelId(String fileLabelId) {
        this.fileLabelId = fileLabelId;
    }

    public String getCreateUserName() {
        return createUserName;
    }

    public void setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
    }

    public String getUpdateUserName() {
        return updateUserName;
    }

    public void setUpdateUserName(String updateUserName) {
        this.updateUserName = updateUserName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFileVersionName() {
        return fileVersionName;
    }

    public void setFileVersionName(String fileVersionName) {
        this.fileVersionName = fileVersionName;
    }

    public Map getFileTextMap() {
        return fileTextMap;
    }

    public void setFileTextMap(Map fileTextMap) {
        this.fileTextMap = fileTextMap;
    }

    public String getFileVersionId() {
        return fileVersionId;
    }

    public void setFileVersionId(String fileVersionId) {
        this.fileVersionId = fileVersionId;
    }

    public String getFileUuid() {
        return fileUuid;
    }

    public void setFileUuid(String fileUuid) {
        this.fileUuid = fileUuid;
    }

    public String getFileText() {
        return fileText;
    }

    public void setFileText(String fileText) {
        this.fileText = fileText;
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

    public String getFileUseRangeId() {
        return fileUseRangeId;
    }

    public void setFileUseRangeId(String fileUseRangeId) {
        this.fileUseRangeId = fileUseRangeId;
    }

    public String getFileUseSceneId() {
        return fileUseSceneId;
    }

    public void setFileUseSceneId(String fileUseSceneId) {
        this.fileUseSceneId = fileUseSceneId;
    }


    public String getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }
}
