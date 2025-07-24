package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/1/21 18:04
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DocLabelPojo {
    private String labelUuid;
    private String labelContent;
    private String labelGroupId;
    private Object fileTypeId;
    private String isShow;
    private String createUserId;
    private Date   createTime;
    private Date   updateTime;

    /**
     * 非表数据
     */
    private String labelGroupName;
    private String createUserName;
    private Boolean is_edit;
    private Object fileTypeName;
}
