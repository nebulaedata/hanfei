package cn.nebulaedata.Advice;

import cn.nebulaedata.utils.AESUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * 功能描述 encryptKey是配置在yml文件中的加密解密key（该key要为16字节或者是16字节的n次方,要不然报错）
 */
@ControllerAdvice
@Slf4j
public class DecryptRequestAdvice extends RequestBodyAdviceAdapter {
    @Value("${aes.request-decrypt}")
    private Boolean requestDecrypt;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return requestDecrypt;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        String encoding = "UTF-8";
        //①：获取http请求中原始的body
        String body = IOUtils.toString(inputMessage.getBody(), encoding);
        //②：解密body，EncryptionUtils源码在后面
        String decryptBody = new String(AESUtils.decrypt(body.getBytes()));
        //将解密之后的body数据重新封装为HttpInputMessage作为当前方法的返回值
        InputStream inputStream = IOUtils.toInputStream(decryptBody, encoding);
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() throws IOException {
                return inputStream;
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }
}