package cn.nebulaedata.pojo;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/1/21 18:04
 * @note
 */
public class DocTipsPojo {
    private String tipsUuid;
    private String createUserId;
    private Date   createTime;
    private Date   updateTime;
    private String tipsName;
    private String tipsDesc;

    public String getTipsUuid() {
        return tipsUuid;
    }

    public void setTipsUuid(String tipsUuid) {
        this.tipsUuid = tipsUuid;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
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
}
