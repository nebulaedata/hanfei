package cn.nebulaedata;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

/**
 * @author 徐衍旭
 * @date 2021/8/12 13:49
 * @note
 */
@SpringBootApplication(exclude= {DataSourceAutoConfiguration.class})
@EnableAutoConfiguration(exclude = { FreeMarkerAutoConfiguration.class })
@MapperScan(basePackages = {"cn.nebulaedata.dao"})
@EnableAspectJAutoProxy
public class StartApplication {
    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class);
    }
}
