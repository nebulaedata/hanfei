package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 贾亦真
 * @date 2020/12/28 14:50
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DocMenuPojo {

    /**
     * 表字段
     */
    private String menuId;
    private String menuName;
    private String fatherId;
    private String order;
    private String route;
    private String path;
    private Boolean isMenu;
    private Boolean isDel;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String id;

    private List<DocMenuPojo> children;
}
