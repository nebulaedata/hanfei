package cn.nebulaedata.exception;

/**
 * @author 徐衍旭
 * @date 2023/3/15 14:35
 * @note
 */
public class EditToolException extends Exception {
    public EditToolException(){
    }

    public EditToolException(Integer code){
        this.code = code;
    }

    public EditToolException(String content){
        this.content = content;
    }

    private Integer code;

    private String content;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
