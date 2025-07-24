package cn.nebulaedata.service;

import cn.nebulaedata.form.DocUserLoginForm;
import cn.nebulaedata.pojo.DocUserPojo;
import cn.nebulaedata.vo.TResponseVo;

/**
 * @author 徐衍旭
 * @date 2021/8/16 14:18
 * @note
 */
public interface UserService {

    /**
     * 用户登录校验
     *
     * @param docUserLoginForm
     * @return
     * @throws Exception
     */
    public <T> TResponseVo<T> UserLoginCheckService(DocUserLoginForm docUserLoginForm) throws Exception;

    /**
     * 获取用户信息
     */
    public TResponseVo getUserInfoService(String userId) throws Exception;

    /**
     * 判断用户权限
     * @param userId
     * @return
     * @throws Exception
     */
    public TResponseVo checkUserPermissionService(String userId, String meunId) throws Exception;

    /**
     * 更改用户信息
     */
    public TResponseVo updateUserInfoService(DocUserPojo docUserPojo, String userId) throws Exception;

    /**
     * 修改密码
     */
    public TResponseVo updatePasswdService(String userPassword,String oldPassword, String userId) throws Exception;

    /**
     * 发送短信验证码
     */
    public TResponseVo sendVerificationCodeService(String phoneNumbers) throws Exception;
}
