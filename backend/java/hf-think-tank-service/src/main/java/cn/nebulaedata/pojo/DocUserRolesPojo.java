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
public class DocUserRolesPojo {

    /**
     * 表字段
     */
    private String rolesId;
    private String rolesName;
    private Boolean idDel;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private List<String> menu;
    private List<String> menuName;
    private String menuStr;
    private String menuNameStr;
    private List<Object> menuTree;

}
