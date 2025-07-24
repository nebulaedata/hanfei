package cn.nebulaedata.exception;

/**
 * @author 贾亦真
 * @date 2021/1/11 09:46
 * @note
 */
public class FileIndexException extends Exception {


    public FileIndexException(){
    }

    public FileIndexException(Integer code){
        this.code = code;
    }

    private Integer code;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
    
}
