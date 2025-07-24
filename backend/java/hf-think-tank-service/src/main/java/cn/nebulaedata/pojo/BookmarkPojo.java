package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class BookmarkPojo {
    private String bookmarkUuid;
    private String bookmarkName;
    private String bookmarkDesc;
    private String fileUuid;
    private String fileVersionId;
    private String outlineId;
    private String createUserId;
    private String isDel;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String contentText;
    private List<Map<String,String >> actions;
    private String searchUuid;
    private String userId;
    private String type;
    private List<Map<String,String>> bookmarkUuidList;
    private int bookmarkUuidListLength;
    private String key;
    private String createUserName;
    private String useCnt;  // 书签被使用次数
}
