package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class ApplyPojo {
    private String applyId;  // 申请ID
    private String applyDesc;  // 申请说明
    private String applyUserId;  // 申请人id
    private String applyContent;  // 申请内容
    private Date createTime;  // 创建时间
    private Date updateTime;  //

}
