package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class DocWordsPojo {
    private String wordsUuid;
    private String wordsName;
    private String wordsDesc;
    private String createUserId;
    private Date   createTime;
    private Date   updateTime;
}
