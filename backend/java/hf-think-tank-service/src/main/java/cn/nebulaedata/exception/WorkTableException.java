package cn.nebulaedata.exception;

/**
 * @author 贾亦真
 * @date 2021/1/11 09:46
 * @note
 */
public class WorkTableException extends Exception {


    public WorkTableException(){
    }

    public WorkTableException(Integer code){
        this.code = code;
    }

    public WorkTableException(Integer code,String content){
        this.code = code;
        this.content = content;
    }

    public WorkTableException(String content){
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
