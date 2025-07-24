package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class HfHelpDocCatalogue {
    private String helpDocId;
    private String helpDocName;
    private String level;
    private String fatherId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */

    private String helpDocContent;
    private String searchContent;
    private String fatherHelpDocName;
    private List<HfHelpDocCatalogue> children;

}
