package cn.nebulaedata.Advice;

import cn.nebulaedata.utils.AESUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * @author 贾亦真
 * @date 2022/12/6 15:16
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */
@ControllerAdvice
public class DfwResponseBodyAdvice implements ResponseBodyAdvice {
    @Value("${aes.response-encrypt}")
    private Boolean responseEncrypt;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return responseEncrypt;
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        HttpServletRequest servletRequest;
        int size = 0;
        try {
            servletRequest = ((ServletServerHttpRequest) serverHttpRequest).getServletRequest();
            size = ((StandardMultipartHttpServletRequest) servletRequest).getMultiFileMap().size();
        } catch (Exception e) {
        }
        if (0 == size) {
            String resJson = JSON.toJSONStringWithDateFormat(o, "yyyy-MM-dd HH:mm:ss", SerializerFeature.DisableCircularReferenceDetect);
            String encrypt = AESUtils.encrypt(resJson.getBytes(StandardCharsets.UTF_8), AESUtils.key.getBytes(StandardCharsets.UTF_8));
            return encrypt;
        }else {
            return o;
        }

    }

}
