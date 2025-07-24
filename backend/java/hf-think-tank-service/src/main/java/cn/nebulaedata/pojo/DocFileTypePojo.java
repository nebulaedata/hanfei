//package cn.nebulaedata.pojo;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import lombok.Data;
//
//import java.util.Date;
//import java.util.List;
//
///**
// * @author 贾亦真
// * @date 2021/1/11 09:15
// * @note
// */
//@Data
//@JsonInclude(value = JsonInclude.Include.NON_NULL)
//public class DocFileTypePojo {
//
//    private String fileTypeId;
//    private String fileTypeName;
//    private Date createTime;
//    private Date updateTime;
//    private String userId;
//    private Integer typePriority;
//    private String fileTypeDesc;
//    private List<DocFileIndexPojo> children;
//
//    public String getFileTypeDesc() {
//        return fileTypeDesc;
//    }
//
//    public void setFileTypeDesc(String fileTypeDesc) {
//        this.fileTypeDesc = fileTypeDesc;
//    }
//
//    public List<DocFileIndexPojo> getChildren() {
//        return children;
//    }
//
//    public void setChildren(List<DocFileIndexPojo> children) {
//        this.children = children;
//    }
//
//    public String getFileTypeId() {
//        return fileTypeId;
//    }
//
//    public void setFileTypeId(String fileTypeId) {
//        this.fileTypeId = fileTypeId;
//    }
//
//    public String getFileTypeName() {
//        return fileTypeName;
//    }
//
//    public void setFileTypeName(String fileTypeName) {
//        this.fileTypeName = fileTypeName;
//    }
//
//    public Date getCreateTime() {
//        return createTime;
//    }
//
//    public void setCreateTime(Date createTime) {
//        this.createTime = createTime;
//    }
//
//    public Date getUpdateTime() {
//        return updateTime;
//    }
//
//    public void setUpdateTime(Date updateTime) {
//        this.updateTime = updateTime;
//    }
//
//    public String getUserId() {
//        return userId;
//    }
//
//    public void setUserId(String userId) {
//        this.userId = userId;
//    }
//
//    public Integer getTypePriority() {
//        return typePriority;
//    }
//
//    public void setTypePriority(Integer typePriority) {
//        this.typePriority = typePriority;
//    }
//}
