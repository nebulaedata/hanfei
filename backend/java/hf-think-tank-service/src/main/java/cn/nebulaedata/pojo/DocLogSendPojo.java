package cn.nebulaedata.pojo;

import java.util.Date;

/**
 * @author 贾亦真
 * @date 2021/1/15 13:54
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */
public class DocLogSendPojo {

    private String userId;
    private String searchContent;
    private String logType;
    private Date updateTime;
    private Date createTime;
    private DocTaggingPojo tagging;

    public DocTaggingPojo getTagging() {
        return tagging;
    }

    public void setTagging(DocTaggingPojo tagging) {
        this.tagging = tagging;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSearchContent() {
        return searchContent;
    }

    public void setSearchContent(String searchContent) {
        this.searchContent = searchContent;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
