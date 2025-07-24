package cn.nebulaedata.utils;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * @author 贾亦真
 * @data 2020/10/14 23:22
 * @note
 */

@Component
public class AmqUtils {

    private static Logger LOG = LoggerFactory.getLogger(AmqUtils.class);

    @Autowired
    private AmqpTemplate amqpTemplate;
    @Value("${doc-frame-service.env-name}")
    private String envName;

    //静态化工具类变量
    private static AmqUtils amqUtils;

    @PostConstruct
    public void init() {
        System.out.println("初始化AmqUtils");
        amqUtils = new AmqUtils();
        amqUtils.envName = this.envName;
        amqUtils.amqpTemplate = this.amqpTemplate;
        System.out.println("初始化AmqUtils完毕");
    }

    /**
     * 发送消息
     *
     * @param name
     * @param queue
     * @param msg
     */
    public void sendMsgToMq(String name, String queue, String msg) {
        LOG.info("[Class]:{}-[BusinessId]:{}-[Queue]:{}-发送内容-{}", "sendMsgToMq", name, queue, msg);
        amqUtils.amqpTemplate.convertAndSend(queue, msg);
    }


    /**
     * 创建队列
     *
     * @param queueName
     * @throws Exception
     */
    public static Channel buildQueue(String queueName) throws Exception {
        //1. 获取连接对象
        Connection connection = AmqConnectionUtils.getConnection();

        //2. 构建Channel
        Channel channel = connection.createChannel();

        //3. 构建队列 如果没有queueName队列会自动创建，有就不会创建
        channel.queueDeclare(queueName, true, false, false, null);

//        //4. 发布消息
//        HashMap<String, String> map = new HashMap<>();
//        map.put("type", "test");
//        map.put("msg", "空间创建");
//        channel.basicPublish("", queueName, null, JSON.toJSONString(map).getBytes());
//        System.out.println("消息发送成功！");

        return channel;
    }



    /**
     * 销毁队列
     *
     * @param queueName
     * @throws Exception
     */
    public static void destroyQueue(String queueName) throws Exception {
        // try的括号中所有实现Closeable的类声明都可以写在里面，最常见的是流操作，socket操作等。括号中可以写多行语句，会自动关闭括号中的资源
        try (Connection connection = AmqConnectionUtils.getConnection()) {
            Channel channel = connection.createChannel();

            // 删除指定队列
            channel.queueDelete(queueName);
            System.out.println("队列已被成功删除！");
            channel.close();
        } catch (Exception e) {
            System.err.println("无法删除该队列！");
            e.printStackTrace();
        }
    }


}
