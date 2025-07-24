package cn.nebulaedata.pojo;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @author 贾亦真
 * @date 2020/12/28 14:50
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfTeachVideoPojo {

    private String videoId;
    private String videoUrl;
    private String title;
    private String description;
    private Object like;
    private Object dislike;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    public Object getLike() {
        if (like == null) {
            return null;
        }
        if (like instanceof String) {  // 从数据库读出
            if (StringUtils.isBlank((String) like)) {
                return null;
            }
            return Integer.valueOf((String) like);  // 返回给接口 List
        }
        return String.valueOf(like);  // 返回给数据库 String
    }

    public Object getDislike() {
        if (dislike == null) {
            return null;
        }
        if (dislike instanceof String) {  // 从数据库读出
            if (StringUtils.isBlank((String) dislike)) {
                return null;
            }
            return Integer.valueOf((String) dislike);  // 返回给接口 List
        }
        return String.valueOf(dislike);  // 返回给数据库 String
    }

}
