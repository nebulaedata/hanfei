package cn.nebulaedata.controller;

import cn.nebulaedata.dao.UserMapper;
import cn.nebulaedata.enums.ResponseEnum;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.form.DocUserLoginForm;
import cn.nebulaedata.pojo.AliyunSmsPojo;
import cn.nebulaedata.pojo.DocUserPojo;
import cn.nebulaedata.service.UserService;
import cn.nebulaedata.utils.RedisUtils;
import cn.nebulaedata.vo.TResponseVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.*;

import net.coobird.thumbnailator.Thumbnails;

/**
 * @author 徐衍旭
 * @date 2021/8/16 14:11
 * @note
 */

@RestController
@RequestMapping("/web/user")
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);
    @Value("${doc-frame-service.headImg-path}")
    private String headImgPath;
    @Autowired
    private UserService userService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Value("${doc-frame-service.env-name}")
    private String envName;
    @Value("${server.servlet.session.timeout}")
    private Integer sessionTimeout;

    @PostMapping("/userlogin")
    public TResponseVo userlogin(@RequestBody @Valid DocUserLoginForm docUserLoginForm, BindingResult bindingResult, HttpSession session, HttpServletResponse response) throws Exception {
        if (bindingResult.hasErrors()) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}-{}-{}", "userlogin", "用户登录", "必填字段未填写", bindingResult.getFieldError().getField(), bindingResult.getFieldError().getDefaultMessage());
            return TResponseVo.error(ResponseEnum.WEB_LOGIN_ERROR, bindingResult.getFieldError().getDefaultMessage());
        }
        TResponseVo<DocUserPojo> docUserPojoTResponseVo = userService.UserLoginCheckService(docUserLoginForm);
        Integer status = docUserPojoTResponseVo.getStatus();
        if (status == 0) {
            DocUserPojo user = docUserPojoTResponseVo.getData();
            session.setAttribute("user", user);
            String userId = user.getUserId();
//            redisUtils.set("displace|||"+user.getUserId(),session.getId(),24*3600*1000);
            Cookie userId1 = new Cookie("userId", userId);
            userId1.setPath("/");
            response.addCookie(userId1);
            try {
                redisUtils.set("login_" + envName + "_" + user.getUserId() + "_" + session.getId(), 30, sessionTimeout);  // 记录登录状态
            } catch (Exception e) {

            }

            return docUserPojoTResponseVo;
        } else {
            return docUserPojoTResponseVo;
        }
    }

    @PostMapping("/userlogout")
    public TResponseVo userlogout(HttpSession session, HttpServletResponse response) throws Exception {
        session.setAttribute("user", null);
        Cookie userId = new Cookie("userId", null);
        userId.setMaxAge(0);
        userId.setPath("/");
        response.addCookie(userId);
        Cookie JESSIONID = new Cookie("JESSIONID", null);
        JESSIONID.setMaxAge(0);
        JESSIONID.setPath("/");
        response.addCookie(JESSIONID);
        try {
            DocUserPojo user = (DocUserPojo) session.getAttribute("user");
            String loginUserId = user.getUserId();
            redisUtils.del("login_" + envName + "_" + loginUserId + "_" + session.getId());  // 删除登录状态
        } catch (Exception e) {

        }

        return TResponseVo.success("登出成功");
    }

    @GetMapping("/getUserInfo")
    public TResponseVo getUserInfo(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        if (user == null) {
            return TResponseVo.error("用户未登录");
        }
        String userId = user.getUserId();
        return userService.getUserInfoService(userId);
    }

    /**
     * 判断用户权限
     * @param menuId
     * @param session
     * @return
     * @throws Exception
     */
    @GetMapping("/checkUserPermission")
    public TResponseVo checkUserPermission(String menuId, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        if (user == null) {
            return TResponseVo.error("用户未登录");
        }
        String userId = user.getUserId();
//        return userService.checkUserPermissionService(userId, menuId);
        return TResponseVo.success(true);
    }

    @PostMapping("/uploadUserHeadImg")
    public TResponseVo uploadUserHeadImg(@RequestParam(value = "file", required = false) MultipartFile file, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        if (file != null) {
            // 检查文件后缀名是否正确
            String headImgFileName = file.getOriginalFilename();
            String extension = headImgFileName.substring(headImgFileName.lastIndexOf(".")).toLowerCase();
            List<String> strings = Arrays.asList(".jpg", ".png", ".bmp", ".jpeg");
            if (!strings.contains(extension)) {
                return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传错误,目前只支持图片格式");
            }

            // 创建用户头像目录
            File userHeadImgDocument = new File(this.headImgPath + "/" + userId);
            if (!userHeadImgDocument.exists()) {
                userHeadImgDocument.mkdir();
            }

            // 创建文件
            String headImgUuid = UUID.randomUUID().toString().replaceAll("-", "");
            String headImgPathNew = this.headImgPath + "/" + userId + "/" + headImgUuid + extension;
            File newHeadImg = new File(headImgPathNew);
            try {
                file.transferTo(newHeadImg);
            } catch (IOException e) {
                LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "updateUserInfo", "头像上传Controller", "头像上传报错");
            }
            // 图片可以压缩
//            String absolutePath = headImgFile.getOriginalFilename();
            if (strings.contains(extension)) {
                Thumbnails.of(headImgPathNew).size(100, 100).toFile(headImgPathNew);
            }

            return TResponseVo.success(new HashMap<String, Object>() {{
                put("url", headImgPathNew);
            }});
        } else {
            throw new WorkTableException("头像文件为空");
        }

    }

    @PostMapping("/updateUserInfo")
    public TResponseVo updateUserInfo(@RequestBody DocUserPojo docUserPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return userService.updateUserInfoService(docUserPojo, userId);
    }

    @PostMapping("/updatePasswd")
    public TResponseVo updatePasswd(@RequestBody DocUserPojo docUserPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String newPassword = docUserPojo.getNewPassword();
        String oldPassword = docUserPojo.getOldPassword();
        return userService.updatePasswdService(newPassword, oldPassword, userId);
    }

    @PostMapping("/sendValidateCode")
    public TResponseVo sendSms(@RequestBody AliyunSmsPojo params) throws Exception {
        String phoneNumbers = params.getPhoneNumbers();
        if (phoneNumbers == null) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "手机号不能为空");
        }
        return userService.sendVerificationCodeService(phoneNumbers);
    }
}
