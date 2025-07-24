package cn.nebulaedata.config;

import cn.nebulaedata.interceptor.AdminInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author 徐衍旭
 * @date 2021/9/22 14:53
 * @note
 */
@Configuration
public class LoginConfig implements WebMvcConfigurer {

    @Bean
    public AdminInterceptor getAdminInterceptor() {
        return new AdminInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册TestInterceptor拦截器
        registry.addInterceptor(getAdminInterceptor())
                .addPathPatterns("/**")                     //所有路径都被拦截
        ;
    }
}
