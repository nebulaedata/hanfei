package cn.nebulaedata.enums;

/**
 * @author 贾亦真
 * @data 2020/7/30 21:45
 * @note 返回前端错误码枚举类
 */
public enum ResponseEnum {
//    ERROR(-1,"服务器错误,请查看错误日志解决"),
    ERROR(-1,"网络资源忙，请稍候重试"),
    WEB_INPARAM_ERROR(1,"传入参数有误"),
    WEB_REGISTER_ERROR(2,"用户注册有误"),
    WEB_LOGIN_ERROR(3,"用户登录有误"),
    WEB_USER_ERROR(4,"用户模块有误"),
    WEB_DEPART_ERROR(5,"部门操作有误"),
    WEB_LOGIN_NO_ERROR(6,"用户未登录"),
    WEB_APP_NOAPPUSER_ERROR(7,"审核单无审核人"),
    WEB_INPARAM_NOENOUGH_ERROR(8,"必填参数为空"),
    SUCCESS(0,"成功");

    Integer code;
    String desc;

    ResponseEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}

