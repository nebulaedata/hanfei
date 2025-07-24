package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/11/8 14:33
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class DocParamsGroupPojo {
    private String groupId;
    private String groupName;
    private String isDel;
    private String order;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private List<String> groupIdList;
}
