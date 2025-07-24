package cn.nebulaedata.form;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2022/11/8 14:58
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class NodeForm {
    private Map<String,Object> data;
    private String id;
    private String label;
}
