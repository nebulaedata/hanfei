package cn.nebulaedata.vo;

import cn.nebulaedata.enums.ResponseEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang.StringUtils;

/**
 * @author 贾亦真
 * @data 2020/8/20 20:07
 * @note
 */

@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class TResponseVo<T> {
    private Integer status;

    private String msg;

    private T data;

    public TResponseVo(Integer status, String msg, T data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    public static <T> TResponseVo<T> success(String msg) {
        return new TResponseVo<T>(ResponseEnum.SUCCESS.getCode(), msg, null);
    }

    public static <T> TResponseVo<T> success(T data) {
        return new TResponseVo<T>(ResponseEnum.SUCCESS.getCode(), null, data);
    }

    public static <T> TResponseVo<T> success(String msg, T data) {
        return new TResponseVo<T>(ResponseEnum.SUCCESS.getCode(), msg, data);
    }

    public static <T> TResponseVo<T> success() {
        return new TResponseVo<T>(ResponseEnum.SUCCESS.getCode(), null, null);
    }

    public static <T> TResponseVo<T> error(ResponseEnum responseEnum, String msg) {
        if (StringUtils.isBlank(msg)) {
            return new TResponseVo<T>(responseEnum.getCode(), null, null);
        }
        return new TResponseVo<T>(responseEnum.getCode(), msg, null);
    }

    public static <T> TResponseVo<T> error(ResponseEnum responseEnum) {
        return new TResponseVo<T>(responseEnum.getCode(), responseEnum.getDesc(), null);
    }

    public static <T> TResponseVo<T> error(String msg) {
        return new TResponseVo<T>(-1, msg, null);
    }

    public static <T> TResponseVo<T> error(int code ,String msg) {
        return new TResponseVo<T>(code, msg, null);
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
