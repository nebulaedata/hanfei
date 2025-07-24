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
public class TagPojo {
    private String tagId;
    private String tagContent;
    private String fileUuid;
    private String fileVersionId;
    private String outlineId;
    private String lawId;
    private Object textitemIdList;
    private String wordsUuid;
    private String bookmarkUuid;
    private String createUserId;
    private String typeId;
    private String isDel;
    private String useIsDel;
    private Date createTime;
    private Date updateTime;
    private String tagName;

    /**
     * 非表字段
     */
    private String contentText;
    private List<Map<String,String >> actions;
    private String searchUuid;
    private String userId;
    private String type;
    private List<Map<String,Object>> tagIdList;
    private int tagIdListLength;
    private String key;
    private String bookmarkOutLineId;
    private String outlineOrder;

    /**
     * 统一返回字段
     */
    private String showName;
    private String showType;
    private String showDesc;

    // 摘编内容专用
    private Object lawContent;
    private List<Object> uuidList;

    /**
     * 判断聚合用的临时字段 词条存wordsUuid 引用存bookmarkUuid
     */
    private String tmpUuid;

    /**
     * 搜索关键字
     */
    private String paramNameLike;
}
