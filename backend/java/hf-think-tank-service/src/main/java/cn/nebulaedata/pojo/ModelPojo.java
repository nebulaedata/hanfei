package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.annotation.sql.DataSourceDefinition;
import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class ModelPojo {
    private String modelId;
    private String modelName;
    private String modelFatherId;
    private String isInit;
    private String createUserId;
    private String uploadUserId;
    private String suitRole;
    private String suitType;
    private String suitUser;
    private String suitWay;
    private String suitBidMethod;
    private Date createTime;
    private Date updateTime;

    private List<ModelPojo> children;

    private String type;
}
