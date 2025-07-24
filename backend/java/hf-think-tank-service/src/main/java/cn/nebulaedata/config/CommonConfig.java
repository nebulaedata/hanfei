package cn.nebulaedata.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.MultipartConfigElement;

/**
 * @author 徐衍旭
 * @date 2023/7/5 09:25
 * @note
 */
@Configuration
public class CommonConfig {
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(1024 * 1024 * 1024); // 限制上传文件大小
        return factory.createMultipartConfig();
    }
}
