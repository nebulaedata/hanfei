package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfDmDb {
    private String dbId;
    private String dbName;
    private String dbDesc;
    private String order;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */

    private String key;
    private String title;
    private String type;
    private String desc;
    private String createUserName;
    private List<HfDmTable> children;

    private String fileUuid;
    private String fileVersionId;

    private String label;
    private String value;


}
