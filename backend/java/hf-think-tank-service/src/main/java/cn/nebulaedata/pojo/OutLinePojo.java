package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class OutLinePojo {
    private String outlineId;
    private String outlineText;
    private String fileUuid;
    private String fileVersionId;
    private String outlineFatherId;
    private String outlineOrder;
    private String outlineLevel;
    private String outlineProperty;
    private String outlineReplaceGroupId;
    private String color;
    private String isNecessary;
    private Date createTime;
    private Date updateTime;
    private String createUserId;
    private String updateUserId;

    /**
     * 非表字段
     */
    private List<OutLinePojo> children;
    private String contentText;
    private String type;
    private String userId;
    private String Previous;  //上一个
    private String next;  // 下一个
    private String outlineTextDelHTMLTag;
    private String targetOutlineId;
    private Boolean isSelected;  // 是否被选中
    private List<String> outlineList;
    private List<Map>  outlineListMap;
    private String annotate;  // 批注
    private String uuid;  // 对比

    private String compare0;
    private String compare1;

}
