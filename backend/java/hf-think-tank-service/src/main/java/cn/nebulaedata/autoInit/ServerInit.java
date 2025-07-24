package cn.nebulaedata.autoInit;

import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.interceptor.AdminInterceptor;
import cn.nebulaedata.pojo.DocParamsPojo;
import cn.nebulaedata.service.impl.FileOperationServiceImpl;
import cn.nebulaedata.socket.RoomServer;
import cn.nebulaedata.utils.RedisUtils;
import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.nebulaedata.utils.AmqUtils.buildQueue;

/**
 * 每次服务重启后需要执行的任务
 * @author 徐衍旭
 * @date 2023/12/15 11:08
 * @note 监听Spring容器启动完成，完成后执行
 */
@Component
public class ServerInit implements ApplicationRunner {

    private static Logger LOG = LoggerFactory.getLogger(AdminInterceptor.class);
    @Autowired
    private RedisUtils redisUtils;
    @Value("${doc-frame-service.env-name}")
    private String envName;
    @Value("${server.servlet.session.timeout}")
    private Integer sessionTimeout;
    @Autowired
    private FileOperationServiceImpl fileOperationServiceImpl;
    @Autowired
    private RoomServer roomServer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // 初始化所有用户的登录状态
            redisUtils.del(redisUtils.dimSearch("login_" + envName + "*"));
            System.out.println("初始化所有用户的登录状态成功");
        } catch (Exception e) {
            System.out.println("初始化所有用户的登录状态失败");
        }

        try {
            // 初始化所有房间用户为离线状态
            String[] roomKeyList = redisUtils.dimSearch(envName + "_roomKey_*");
            if (roomKeyList.length != 0) {
                for (String roomKey : roomKeyList) {
                    try {
                        // 获取房间信息
                        HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
                        List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");

                        // 重置所有人的加入状态为false
                        for (Map<String, Object> userMap : userList) {
                            userMap.put("join", false);
//                            userMap.put("leaveTime",new Date());
                        }
                        // 维护房间信息
                        redisUtils.set(roomKey, roomMap);
//                        System.out.println("已重置房间用户状态(1/2)");

                        // 根据redis房间信息 维护queue
                        // 1.创建房间消息队列mq (已存在不会重建)
                        String fileUuid = roomKey.split("roomKey")[1].split("_")[1];
                        String fileVersionId = roomKey.split("roomKey")[1].split("_")[2];
                        String queueName = "hf-" + envName + "-" + fileUuid + fileVersionId;
                        Channel channel = buildQueue(queueName);
                        // 2.新建房间queue消费者
                        /**
                         * 1.queue:队列名称
                         * 2.aotoACK：是否自动确认
                         * 3.callback：回调对象
                         */
                        Consumer consumer = new DefaultConsumer(channel) {
                            /**
                             * 回调方法。当收到消息之后会自动执行该方法
                             * @param consumerTag 标识
                             * @param envelope 获取一些信息，交换机，路由Key
                             * @param properties 配置信息
                             * @param body 数据
                             * @throws IOException
                             */
                            @Override
                            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                                String msg = new String(body);
                                DocParamsPojo paramsPojo = JSON.parseObject(msg, DocParamsPojo.class);
                                try {
                                    paramsPojo.setWriteWay("team");
                                    fileOperationServiceImpl.writeParamService(paramsPojo);
                                    // 查询当前文件的房间是否存在
                                    String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
                                    if (!redisUtils.hasKey(roomKey)) {
                                        throw new WorkTableException("协同编辑房间未创建");
                                    }
                                    // 获取房间信息
                                    HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
                                    String roomId = (String) roomMap.get("roomId");

                                    // ws 填参完成后通知
                                    HashMap<String, Object> map = new HashMap<>();
                                    map.put("role", "feedback");
                                    map.put("data", new HashMap<String, Object>());
                                    map.put("action", "completeWriteParam");
                                    roomServer.sendInfo(map, roomId);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        channel.basicConsume(queueName, true, consumer);
//                        System.out.println("已重置房间消费者状态(2/2)");

                    } catch (Exception e) {
                        System.out.println("重置已存在房间"+roomKey+"状态失败");
                    }
                }
                System.out.println("重置已存在房间完成");
            } else {
                System.out.println("无需重置房间");
            }
        } catch (Exception e) {
            System.out.println("重置已存在房间状态失败");
        }


    }
}
