package cn.nebulaedata.form;

import lombok.Data;

import java.util.Date;

/**
 * @author 贾亦真
 * @data 2020/8/8 14:46
 * @note 通用接收类来接收通用请求
 */
@Data
public class CurrencyForm {

    private String userId;
    private String departId;
    private String isPaged = "1";
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String departNameLike;
    private String departName;
    private String orgId;
    private String orgName;
    private String orgNameLike;
    private String rolesId;
    private String rolesNameLike;
    private String key;
    private String title;
    private String isSpecial;
    private String orgType;
    private String userNameLike;
    private Integer fileEditionUuid;
    private String fileUuid;
    private String fileName;
    private String fileUrl;
    private String fileText;
    private String fileVersionId;
    private String fileEdition;
    private String isLast;
    private Date createTime;
    private Date updateTime;
    private String shape;
    private String path;
    private String fileTypeId;
    private String fileTypeNameLike;
    private String searchLike;
    private String fileUseSceneName;
    private String fileUseSceneId;
    private String fileUseSceneNameLike;
    private String fileLabelName;
    private String fileLabelId;
    private String fileLabelNameLike;
    private String registerType;
    private String phone;
    private String searchText;
    private String searchContent;
    private String searchIdSelect1;
    private String searchIdSelect2;
    private String searchIdSelect3;
    private String ancestorsDepartId;
    private String orgDutyParagraph;
    private Integer num = 10;
    private String type;
    private String approvalId;
    private String approvalType;
    private String approvalRecordId;
    private Integer approvalRecordLevel;
    private String approvalOpinion;
    private String approvalRemarks;
    private Integer currentLevel;
    private String  approvalStatus;
    private String labelId;
    private String labelName;
    private String fileParagraphId;
    private String paramNameLike;

    public String getFileParagraphId() {
        return fileParagraphId;
    }

    public void setFileParagraphId(String fileParagraphId) {
        this.fileParagraphId = fileParagraphId;
    }

    public String getSearchLike() {
        return searchLike;
    }

    public void setSearchLike(String searchLike) {
        this.searchLike = searchLike;
    }

    public String getLabelId() {
        return labelId;
    }

    public void setLabelId(String labelId) {
        this.labelId = labelId;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Integer getApprovalRecordLevel() {
        return approvalRecordLevel;
    }

    public void setApprovalRecordLevel(Integer approvalRecordLevel) {
        this.approvalRecordLevel = approvalRecordLevel;
    }

    public String getApprovalOpinion() {
        return approvalOpinion;
    }

    public void setApprovalOpinion(String approvalOpinion) {
        this.approvalOpinion = approvalOpinion;
    }

    public String getApprovalRemarks() {
        return approvalRemarks;
    }

    public void setApprovalRemarks(String approvalRemarks) {
        this.approvalRemarks = approvalRemarks;
    }

    public String getApprovalRecordId() {
        return approvalRecordId;
    }

    public void setApprovalRecordId(String approvalRecordId) {
        this.approvalRecordId = approvalRecordId;
    }

    public String getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public String getOrgDutyParagraph() {
        return orgDutyParagraph;
    }

    public void setOrgDutyParagraph(String orgDutyParagraph) {
        this.orgDutyParagraph = orgDutyParagraph;
    }

    public String getAncestorsDepartId() {
        return ancestorsDepartId;
    }

    public void setAncestorsDepartId(String ancestorsDepartId) {
        this.ancestorsDepartId = ancestorsDepartId;
    }

    public String getSearchContent() {
        return searchContent;
    }

    public void setSearchContent(String searchContent) {
        this.searchContent = searchContent;
    }

    public String getSearchIdSelect1() {
        return searchIdSelect1;
    }

    public void setSearchIdSelect1(String searchIdSelect1) {
        this.searchIdSelect1 = searchIdSelect1;
    }

    public String getSearchIdSelect2() {
        return searchIdSelect2;
    }

    public void setSearchIdSelect2(String searchIdSelect2) {
        this.searchIdSelect2 = searchIdSelect2;
    }

    public String getSearchIdSelect3() {
        return searchIdSelect3;
    }

    public void setSearchIdSelect3(String searchIdSelect3) {
        this.searchIdSelect3 = searchIdSelect3;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(String registerType) {
        this.registerType = registerType;
    }

    public String getFileLabelName() {
        return fileLabelName;
    }

    public void setFileLabelName(String fileLabelName) {
        this.fileLabelName = fileLabelName;
    }

    public String getFileLabelId() {
        return fileLabelId;
    }

    public void setFileLabelId(String fileLabelId) {
        this.fileLabelId = fileLabelId;
    }

    public String getFileLabelNameLike() {
        return fileLabelNameLike;
    }

    public void setFileLabelNameLike(String fileLabelNameLike) {
        this.fileLabelNameLike = fileLabelNameLike;
    }

    public String getFileUseSceneNameLike() {
        return fileUseSceneNameLike;
    }

    public void setFileUseSceneNameLike(String fileUseSceneNameLike) {
        this.fileUseSceneNameLike = fileUseSceneNameLike;
    }

    private String fileVersionName;
    private String createUserId;
    private String updateUserId;

    public String getFileVersionName() {
        return fileVersionName;
    }

    public void setFileVersionName(String fileVersionName) {
        this.fileVersionName = fileVersionName;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public String getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(String updateUserId) {
        this.updateUserId = updateUserId;
    }

    public String getFileVersionId() {
        return fileVersionId;
    }

    public void setFileVersionId(String fileVersionId) {
        this.fileVersionId = fileVersionId;
    }

    public String getFileTypeNameLike() {
        return fileTypeNameLike;
    }

    public void setFileTypeNameLike(String fileTypeNameLike) {
        this.fileTypeNameLike = fileTypeNameLike;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getFileEditionUuid() {
        return fileEditionUuid;
    }

    public void setFileEditionUuid(Integer fileEditionUuid) {
        this.fileEditionUuid = fileEditionUuid;
    }

    public String getFileUuid() {
        return fileUuid;
    }

    public void setFileUuid(String fileUuid) {
        this.fileUuid = fileUuid;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileText() {
        return fileText;
    }

    public void setFileText(String fileText) {
        this.fileText = fileText;
    }

    public String getFileEdition() {
        return fileEdition;
    }

    public void setFileEdition(String fileEdition) {
        this.fileEdition = fileEdition;
    }

    public String getIsLast() {
        return isLast;
    }

    public void setIsLast(String isLast) {
        this.isLast = isLast;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDepartId() {
        return departId;
    }

    public void setDepartId(String departId) {
        this.departId = departId;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getDepartNameLike() {
        return departNameLike;
    }

    public void setDepartNameLike(String departNameLike) {
        this.departNameLike = departNameLike;
    }

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public String getIsPaged() {
        return isPaged;
    }

    public void setIsPaged(String isPaged) {
        this.isPaged = isPaged;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOrgNameLike() {
        return orgNameLike;
    }

    public void setOrgNameLike(String orgNameLike) {
        this.orgNameLike = orgNameLike;
    }

    public String getRolesId() {
        return rolesId;
    }

    public void setRolesId(String rolesId) {
        this.rolesId = rolesId;
    }

    public String getRolesNameLike() {
        return rolesNameLike;
    }

    public void setRolesNameLike(String rolesNameLike) {
        this.rolesNameLike = rolesNameLike;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIsSpecial() {
        return isSpecial;
    }

    public void setIsSpecial(String isSpecial) {
        this.isSpecial = isSpecial;
    }

    public String getOrgType() {
        return orgType;
    }

    public void setOrgType(String orgType) {
        this.orgType = orgType;
    }

    public String getUserNameLike() {
        return userNameLike;
    }

    public void setUserNameLike(String userNameLike) {
        this.userNameLike = userNameLike;
    }

    public String getFileTypeId() {
        return fileTypeId;
    }

    public void setFileTypeId(String fileTypeId) {
        this.fileTypeId = fileTypeId;
    }

    public String getFileUseSceneName() {
        return fileUseSceneName;
    }

    public void setFileUseSceneName(String fileUseSceneName) {
        this.fileUseSceneName = fileUseSceneName;
    }

    public String getFileUseSceneId() {
        return fileUseSceneId;
    }

    public void setFileUseSceneId(String fileUseSceneId) {
        this.fileUseSceneId = fileUseSceneId;
    }
}
