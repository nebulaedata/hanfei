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
public class DocUserPojo {

    /**
     * 表字段
     */
    private String userId;
    private String userPhone;
    private String userName;
    private String userPassword;
    private Date createTime;
    private Date updateTime;
    private String rolesId;
    private String orgId;
    private String departId;
    private String companyId;
    private String companyIdList;
    private String headImgPath;

    /**
     * 非表字段
     */
    private String orgName;
    private String avatar;
    private String departName;
    private String rolesName;
    private String registerType;
    private String orgType;
    private String orgDutyParagraph;
    private String isAdmin;
    private String isEdit;
    private List<String> departKeyPath;
    private List<String> departKeyPathName;
    private String companyName;
    private String oldPassword;
    private String newPassword;

    private List<Object> menuTree;


}
