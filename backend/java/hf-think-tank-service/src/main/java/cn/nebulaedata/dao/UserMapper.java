package cn.nebulaedata.dao;

import cn.nebulaedata.form.DocUserLoginForm;
import cn.nebulaedata.pojo.DocUserPojo;
import cn.nebulaedata.pojo.HfConfig;

/**
 * @author 徐衍旭
 * @date 2021/8/16 14:21
 * @note
 */
public interface UserMapper {
    /**
     * 登录校验
     *
     * @param docUserLoginForm
     * @return
     */
    public DocUserPojo checkUserLoginDao(DocUserLoginForm docUserLoginForm);

    /**
     * 校验用户是否存在
     * @param loginId
     * @return
     */
    public Integer checkUserExistDao(String loginId);
    public Integer checkPhoneNumberExistDao(String loginId);

    /**
     * 获取用户信息
     */
    public DocUserPojo getUserInfoDao(String userId);

    /**
     *
     * @param userId
     * @param menuId
     * @return
     */
    public Integer checkUserPermissionDao(String userId,String menuId);

    /**
     * 获取用户信息根据手机号
     */
    public DocUserPojo getUserInfoByPhoneNumberDao(String PhoneNumber);


    /**
     * 更新用户信息
     */
    public Integer updateUserInfoDao(DocUserPojo docUserPojo);

    /**
     * 查询配置
     */
    public HfConfig getHfConfigDao(String id);
    /**
     * 查询配置
     */
    public Integer updatePasswdDao(String userPassword,String userId);
}
