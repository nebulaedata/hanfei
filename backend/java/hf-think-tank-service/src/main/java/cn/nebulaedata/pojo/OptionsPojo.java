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
public class OptionsPojo {

    public OptionsPojo() {

    }

    public OptionsPojo(String label, String value) {
        this.label = label;
        this.value = value;
    }

    private String id;
    private String label;
    private String value;
    private String type;
    private String typeDesc;
    private Boolean isDel;
    private Boolean isShow;
    private Boolean allowDel;
    private Date createTime;
    private Date updateTime;


}
