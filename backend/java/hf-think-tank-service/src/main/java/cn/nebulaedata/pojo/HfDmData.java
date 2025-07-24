package cn.nebulaedata.pojo;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/1/21 16:33
 * @note
 */

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HfDmData {
    private String dataId;
    private Object dataContent;
    private String tableId;
    private String order;
    private String createUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String key;

    private String fileUuid;
    private String fileVersionId;


    private Object dataContentLeft;
    private Object dataContentRight;

    private Object matchResult;
//    public Object getMatchResult() {
//        if (matchResult == null) {
//            return null;
//        }
//        if (matchResult instanceof String) {  // 从数据库读出
//            if (StringUtils.isBlank((String) matchResult)) {
//                return null;
//            }
//            return JSON.parseObject((String) matchResult,List.class);  // 返回给接口 List
//        }
//        return JSON.toJSONString(matchResult);  // 返回给数据库 String
//    }

}
