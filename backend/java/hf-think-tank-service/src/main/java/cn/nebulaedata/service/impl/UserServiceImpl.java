package cn.nebulaedata.service.impl;

import cn.nebulaedata.dao.UserMapper;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.enums.ResponseEnum;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.form.DocUserLoginForm;
import cn.nebulaedata.pojo.DocMenuPojo;
import cn.nebulaedata.pojo.DocUserPojo;
import cn.nebulaedata.pojo.HfConfig;
import cn.nebulaedata.service.UserService;
import cn.nebulaedata.utils.AliyunSms;
import cn.nebulaedata.utils.List2TreeUtils;
import cn.nebulaedata.utils.RSAUtils;
import cn.nebulaedata.utils.RedisUtils;
import cn.nebulaedata.vo.TResponseVo;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;


import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/8/16 14:17
 * @note
 */
@Service
@Configuration
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private WorkingTableMapper workingTableMapper;



    @Bean
    AliyunSms aliyunSms() {
        return new AliyunSms("南京星恒数据", "SMS_209280237");
    }

    @Value("${doc-frame-service.env-name}")
    private String envName;

    @Value("${rsa-key.privateKey}")
    private String privateKey;

    /**
     * 用户登录校验
     *
     * @param docUserLoginForm
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo<DocUserPojo> UserLoginCheckService(DocUserLoginForm docUserLoginForm) throws Exception {
        String verificationCode = docUserLoginForm.getVerificationCode();
        // type 1: 验证码校验
        if (verificationCode != null) {
            // 查询手机号是否已注册
            String phoneNumber = docUserLoginForm.getPhoneNumber();
            if (phoneNumber != null) {
                Integer integer = userMapper.checkPhoneNumberExistDao(phoneNumber);
                if (integer == 0) {
                    // 未注册，登录失败
                    throw new WorkTableException("对不起，您的手机号未注册");
                }
            }
            // 已注册，校验验证码
            String code = (String) redisUtils.get(envName + "_" + phoneNumber + "_code");
            if (code == null || code.isEmpty()) {
                throw new WorkTableException("验证码已过期或者不存在！");
            }
            if (!verificationCode.equals(code)) {
                throw new WorkTableException("验证码错误！");
            }
            // 验证码正确，删除验证码，并登录成功
            redisUtils.del(envName + "_" + phoneNumber + "_code");
            // 登录成功，根据手机号查询用户信息
            DocUserPojo docUserPojo = userMapper.getUserInfoByPhoneNumberDao(phoneNumber);
            docUserPojo.setUserPassword(null);
            return TResponseVo.success("登录成功", docUserPojo);
        }

        // type 2: 用户名密码校验
        String loginId = docUserLoginForm.getLoginId();
        if (StringUtils.isBlank(loginId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 先判断是否已经禁止登录了
        Object o = redisUtils.get(envName + "_" + loginId + "_loginFailTimes");
        if (o != null) {
            Map<String, Integer> map = (Map) o;
            Integer times = map.get("times");
            if (times >= 5) {
                throw new WorkTableException("尝试登录次数过多，请1分钟后再试");
            }
        }
        // 检查用户是否存在
        Integer integer = userMapper.checkUserExistDao(loginId);
        if (integer == 0) {
            throw new WorkTableException("用户名不存在");
        }

        // rsa解密
        String password = docUserLoginForm.getLoginPassword();
        String decrypt = new RSAUtils().decrypt(password, privateKey);
        // md5加密 并对比数据库
        docUserLoginForm.setLoginPassword(DigestUtils.md5DigestAsHex(decrypt.getBytes()));
        DocUserPojo docUserPojo = userMapper.checkUserLoginDao(docUserLoginForm);

        if (docUserPojo != null && StringUtils.isNotBlank(docUserPojo.getUserId())) {
            redisUtils.del(envName + "_" + loginId + "_loginFailTimes");  // 清除登录失败次数
            docUserPojo.setUserPassword(null);
            return TResponseVo.success("登录成功", docUserPojo);
        } else {
            // 登录失败 增加失败次数
            o = redisUtils.get(envName + "_" + loginId + "_loginFailTimes");
            if (o != null) {
                // +1
                Map<String, Integer> map = (Map) o;
                Integer times = map.get("times");
                map.put("times", times + 1);
                redisUtils.set(envName + "_" + loginId + "_loginFailTimes", map, 60);
                if (times + 1 >= 5) {
                    throw new WorkTableException("尝试登录次数过多，请1分钟后再试");
                } else {
                    return TResponseVo.error(ResponseEnum.WEB_LOGIN_ERROR, "密码错误，还可尝试" + ((4 - times) > 0 ? (4 - times) : 0) + "次，失败后将锁定1分钟");
                }
            } else {
                HashMap<String, Integer> map = new HashMap<>();
                map.put("times", 1);
                redisUtils.set(envName + "_" + loginId + "_loginFailTimes", map, 60);
                return TResponseVo.error(ResponseEnum.WEB_LOGIN_ERROR, "密码错误，还可尝试4次，失败后将锁定1分钟");
            }

        }
    }

    /**
     * 获取用户信息
     *
     * @param userId
     */
    @Override
    public TResponseVo getUserInfoService(String userId) throws Exception {
        DocUserPojo userInfoDao = userMapper.getUserInfoDao(userId);
        userInfoDao.setUserPassword(null);
        // 查询权限
        String rolesId = userInfoDao.getRolesId();
        List<DocMenuPojo> rolesToMenuDao = workingTableMapper.getRolesToMenuDao(rolesId);
        List list = new List2TreeUtils().recursionMethod(rolesToMenuDao, "menuId", "fatherId");
        userInfoDao.setMenuTree(list);
        return TResponseVo.success(userInfoDao);
    }

    /**
     * 判断用户权限
     *
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo checkUserPermissionService(String userId, String meunId) throws Exception {
        Integer i = userMapper.checkUserPermissionDao(userId, meunId);
        if (i > 0) {
            return TResponseVo.success(true);
        } else {
            return TResponseVo.success(false);
        }
    }

    /**
     * 获取用户信息
     *
     * @param docUserPojo
     * @param userId
     */
    @Override
    public TResponseVo updateUserInfoService(DocUserPojo docUserPojo, String userId) throws Exception {
        // 如果修改手机号 验证手机号是否合法
        String userPhone = docUserPojo.getUserPhone();
        // 校验手机号
        String regex = "^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$";
        if (!userPhone.matches(regex)) {
            return TResponseVo.error("手机号格式有误");
        }
        Integer i = workingTableMapper.checkUserDao(userPhone, userId);
        if (i >= 1) {
            return TResponseVo.error("该用户名或手机号已被注册");
        }

        String userName = docUserPojo.getUserName();
        i = workingTableMapper.checkUserNameDao(userName, userId);
        if (i >= 1) {
            return TResponseVo.error("该用户名或手机号已被注册");
        }
        // 更新用户信息
        docUserPojo.setHeadImgPath(docUserPojo.getAvatar());
        docUserPojo.setUserId(userId);
        userMapper.updateUserInfoDao(docUserPojo);
        DocUserPojo userInfoDao = userMapper.getUserInfoDao(userId);
        return TResponseVo.success(userInfoDao);
    }

    /**
     * 修改密码
     *
     * @param newPassword
     * @param userId
     */
    @Override
    public TResponseVo updatePasswdService(String newPassword, String oldPassword, String userId) throws Exception {
        if (StringUtils.isBlank(newPassword) || StringUtils.isBlank(oldPassword)) {
            throw new WorkTableException("必填参数为空");
        }
        // 校验原始密码
        String md5DigestAsHex = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        DocUserPojo docUserPojo = userMapper.getUserInfoDao(userId);
        if (docUserPojo != null && docUserPojo.getUserPassword().equals(md5DigestAsHex)) {
            // 继续
        } else {
            // 否则提示密码错误
            throw new WorkTableException("旧密码错误");
        }
        // 设置新密码
        HfConfig hfConfigDao = userMapper.getHfConfigDao("10001");  // 获取特殊字符配置
        Object content = hfConfigDao.getContent();
        List<String> keyList = new ArrayList<>();
        if (content != null) { // 特殊字符
            keyList = JSON.parseObject(String.valueOf(content), List.class);
        }
        for (String c : keyList) {
            if (newPassword.contains(c)) {
                throw new WorkTableException("密码中请勿包含空格,引号,反斜线");
            }
        }
        if (newPassword.length() < 8) {
            throw new WorkTableException("密码长度不能小于8位");
        }
        userMapper.updatePasswdDao(DigestUtils.md5DigestAsHex(newPassword.getBytes()), userId);
        return TResponseVo.success("修改完成");
    }

    /**
     * 发送短信验证码
     *
     * @param phoneNumbers
     */
    @Override
    public TResponseVo sendVerificationCodeService(String phoneNumbers) throws Exception {
        // 校验手机号
        String regex = "^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$";
        if (!phoneNumbers.matches(regex)) {
            return TResponseVo.error("手机号格式有误");
        }
        // 校验注册情况
        if (phoneNumbers != null) {
            Integer integer = userMapper.checkPhoneNumberExistDao(phoneNumbers);
            if (integer == 0) {
                // 未注册，登录失败
                throw new WorkTableException("对不起，您的手机号未注册");
            }
        }
        if (redisUtils.hasKey(envName + "_" + phoneNumbers + "_code")) {
            long expire = redisUtils.getExpire(envName + "_" + phoneNumbers + "_code");
            throw new WorkTableException("验证码已发送，请" + expire + "秒后重试");
        }
        // 发送验证码
        String code = AliyunSms.generatorCode(6);
        Map<String, String> templateParam = new HashMap<>();
        templateParam.put("code", code);
        String respJson = aliyunSms().send(phoneNumbers, templateParam);
        Map<String, Object> response = JSON.parseObject(respJson, Map.class);
        Map<String, String> body = (Map<String, String>) response.get("body");
        System.out.println(response);
        if (!"OK".equals(body.get("code"))) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "短信发送失败");
        }
        //TODO 流量控制
//        else if ("isv.BUSINESS_LIMIT_CONTROL".equals(body.get("code"))) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "短信发送失败");
//        }
        // 发送成功
        System.out.println("触发短信验证：【手机号：" + phoneNumbers + " | 验证码：" + code + "】");
        // 发送成功：将验证码 code 使用 Redis 存储，并设置过期时间为 2 分钟
        redisUtils.set(envName + "_" + phoneNumbers + "_code", code, 120);
        return TResponseVo.success(body);
    }
}
