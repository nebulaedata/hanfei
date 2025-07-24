package cn.nebulaedata.interceptor;

import cn.nebulaedata.exception.FileIndexException;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.pojo.DocParamsPojo;
import cn.nebulaedata.pojo.DocUserPojo;
import cn.nebulaedata.utils.RedisUtils;
import cn.nebulaedata.utils.TimeFormatUtils;
import cn.nebulaedata.utils.UploadAnnexParamByDmThreadUtils;
import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.nebulaedata.utils.AmqUtils.buildQueue;

/**
 * @author 徐衍旭
 * @date 2021/9/22 14:51
 * @note
 */
public class AdminInterceptor implements HandlerInterceptor {

    private static Logger LOG = LoggerFactory.getLogger(AdminInterceptor.class);
    @Autowired
    private RedisUtils redisUtils;
    @Value("${doc-frame-service.env-name}")
    private String envName;
    @Value("${server.servlet.session.timeout}")
    private Integer sessionTimeout;

//    //静态化工具类变量
//    private static AdminInterceptor adminInterceptor;
//
//    @PostConstruct
//    public void init() {
//        adminInterceptor = getInstance();
//        adminInterceptor.redisUtils = this.redisUtils;
//        adminInterceptor.envName = this.envName;
//        adminInterceptor.sessionTimeout = this.sessionTimeout;
//        System.out.println("初始化AdminInterceptor拦截器");
//
//    }
//
//    // 单例
//    private AdminInterceptor() {
//    }
//
//    ;
//
//    private static class AdminInterceptorInstance {
//        private static final AdminInterceptor userInstance = new AdminInterceptor();
//    }
//
//    public static AdminInterceptor getInstance() {
//        return AdminInterceptorInstance.userInstance;
//    }

    /**
     * 在请求处理之前进行调用（Controller方法调用之前）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        String requestURI = request.getRequestURI();
        String ip = request.getRemoteAddr();
        System.out.println("执行了TestInterceptor的preHandle方法! 接口名:" + requestURI + ", Time: " + new TimeFormatUtils().timestampToStr(new Date()));
        System.out.println(ip);
//        return true;
        try {
            DocUserPojo docUserPojo = new DocUserPojo();
            docUserPojo.setUserId("12345");
            docUserPojo.setUserName("用户");
            session.setAttribute("user", docUserPojo);
            return true;
        } catch (Exception e) {
            session.setAttribute("user", null);
            Cookie userId = new Cookie("userId", null);
            userId.setMaxAge(0);
            userId.setPath("/");
            response.addCookie(userId);
            Cookie JESSIONID = new Cookie("JESSIONID", null);
            JESSIONID.setMaxAge(0);
            JESSIONID.setPath("/");
            response.addCookie(JESSIONID);
            response.setStatus(401);
            return false;
        }
        //如果设置为true时，请求将会继续执行后面的操作
    }

    /**
     * 请求处理之后进行调用，但是在视图被渲染之前（Controller方法调用之后）
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
//        System.out.println("执行了TestInterceptor的postHandle方法");
//        System.out.println("只有当controller执行return时才调用这里,throw就不走这里了");
    }

    /**
     * 在整个请求结束之后被调用，也就是在DispatcherServlet 渲染了对应的视图之后执行（主要是用于进行资源清理工作）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
//        System.out.println("执行了TestInterceptor的afterCompletion方法");
//        System.out.println("不论return 还是 throw了 都走这里");
        // 记录操作日志

    }
}
