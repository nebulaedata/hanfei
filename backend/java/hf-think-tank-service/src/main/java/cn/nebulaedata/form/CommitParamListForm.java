package cn.nebulaedata.form;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/1/20 14:35
 * @note
 */
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class CommitParamListForm {
    private String fileUuid;
    private String fileVersionId;
    private List<CommitParamListItemForm> lists;

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

    public List<CommitParamListItemForm> getLists() {
        return lists;
    }

    public void setLists(List<CommitParamListItemForm> lists) {
        this.lists = lists;
    }
}
