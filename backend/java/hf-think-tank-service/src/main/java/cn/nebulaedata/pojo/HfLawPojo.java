package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class HfLawPojo {
    private String textId;
    private String textContent;
    private String lawId;
    private String lawName;
    private String paragraphTitle;
    private String paragraphContent;
    private String textitemId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String noticeUuid;  // 通知id
    private String updateId;  // 变更id
    private String updateType;  // 变更类型 0未知 1新增 2变更 3删除
    private Date getTime;  // 变更获取时间

    private String fileUuid;  // 文件id
    private String fileVersionId;  // 版本id
    private Boolean read;  // 版本id
    private String groupId;  // 校验批次

    /**
     * 跳转
     */
    private String tagId;  //
    private String outLineId;  //
}
