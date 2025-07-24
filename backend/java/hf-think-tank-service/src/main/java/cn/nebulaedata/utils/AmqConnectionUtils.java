package cn.nebulaedata.utils;

import cn.nebulaedata.interceptor.AdminInterceptor;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
/**
 * 构建amq连接
 *
 * @author 徐衍旭
 * @date 2023/12/7 15:59
 * @note
 */
@Component
public class AmqConnectionUtils {

    @Value("${spring.rabbitmq.addresses}")
    public String RABBITMQ_HOST;
    @Value("${spring.rabbitmq.port}")
    public int RABBITMQ_PORT;
    @Value("${spring.rabbitmq.username}")
    public String RABBITMQ_USERNAME;
    @Value("${spring.rabbitmq.password}")
    public String RABBITMQ_PASSWORD;

//    public static String RABBITMQ_VIRTUAL_HOST = "/";

    //静态化工具类变量
    private static AmqConnectionUtils amqConnectionUtils;

    @PostConstruct
    public void init() {
        System.out.println("初始化AmqConnectionUtils工厂");
        amqConnectionUtils = getInstance();
        amqConnectionUtils.RABBITMQ_HOST = this.RABBITMQ_HOST;
        amqConnectionUtils.RABBITMQ_PORT = this.RABBITMQ_PORT;
        amqConnectionUtils.RABBITMQ_USERNAME = this.RABBITMQ_USERNAME;
        amqConnectionUtils.RABBITMQ_PASSWORD = this.RABBITMQ_PASSWORD;
    }

    // 单例
    private AmqConnectionUtils() {
    }

    ;

    private static class AmqConnectionUtilsInstance {
        private static final AmqConnectionUtils userInstance = new AmqConnectionUtils();
    }

    public static AmqConnectionUtils getInstance() {
        return AmqConnectionUtils.AmqConnectionUtilsInstance.userInstance;
    }


    /**
     * 构建RabbitMQ的连接对象
     * @return
     */
    public static Connection getConnection() throws Exception {
        //1. 创建Connection工厂
        ConnectionFactory factory = new ConnectionFactory();

        //2. 设置RabbitMQ的连接信息
        factory.setHost(amqConnectionUtils.RABBITMQ_HOST);
        factory.setPort(amqConnectionUtils.RABBITMQ_PORT);
        factory.setUsername(amqConnectionUtils.RABBITMQ_USERNAME);
        factory.setPassword(amqConnectionUtils.RABBITMQ_PASSWORD);
//        factory.setVirtualHost(RABBITMQ_VIRTUAL_HOST);

        //3. 返回连接对象
        Connection connection = factory.newConnection();
        return connection;
    }

}
