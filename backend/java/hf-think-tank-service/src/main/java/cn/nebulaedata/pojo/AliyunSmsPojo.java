package cn.nebulaedata.pojo;

import lombok.Data;

@Data
public class AliyunSmsPojo {
    private String phoneNumbers; // 支持对多个手机号码发送短信，手机号码之间以半角逗号（,）分隔。上限为1000个手机号码
}
