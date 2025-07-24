package cn.nebulaedata.form;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author 贾亦真
 * @date 2020/12/30 10:51
 * @note
 */
@Data
public class DocUserLoginForm {

//    @NotBlank(message = "用户名或者手机号不能为空")
    private String loginId;
//    @NotBlank(message = "登录密码不能为空")
    private String loginPassword;
    private String newLoginPassword;
    private String verificationCode;
    private String phoneNumber;
}
