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
public class EdgeForm {
    private String source;
    private String target;
    private String label;
}
