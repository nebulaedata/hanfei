//package cn.nebulaedata.interceptor;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.nio.charset.StandardCharsets;
//
///**
// * @author 贾亦真
// * @date 2022/12/6 10:25
// * @note
// * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
// */
//@Slf4j
//public class EncryptionInterception implements HandlerInterceptor {
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        EncryHttpServletRequest encryHttpServletRequest = new EncryHttpServletRequest(request);
//        log.info("{}-传入参数-：{}", request.getRequestURI(), encryHttpServletRequest.getBody());
//        return HandlerInterceptor.super.preHandle(encryHttpServletRequest, response, handler);
//    }
//
//}
