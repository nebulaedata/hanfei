package cn.nebulaedata.scheduled;

import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.globalVariables.RoomServerVariables;
import cn.nebulaedata.pojo.DocParamsPojo;
import cn.nebulaedata.utils.AmqUtils;
import cn.nebulaedata.utils.RedisUtils;
import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static cn.nebulaedata.utils.AmqUtils.buildQueue;
import static cn.nebulaedata.utils.AmqUtils.destroyQueue;

/**
 * @author 徐衍旭
 * @date 2021/11/15 15:44
 * @note
 */
@Configuration      // 1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class roomServerTask {
    @Value("${doc-frame-service.env-name}")
    private String envName;
    @Autowired
    private AmqUtils amqUtils;
    @Autowired
    private RedisUtils redisUtils;
    // 获取全局变量
    private RoomServerVariables roomServerVariables = RoomServerVariables.getInstance();

    /**
     * 向房间的queue分发过滤后的消息 只做分发 不做处理
     * 指定时间间隔3秒
     */
    @Scheduled(fixedRate = 3000)
    private void sendMQTasks() {
//        System.err.println("执行静态定时任务时间: " + LocalDateTime.now());
        Map<String, Map<String, Object>> globalVar = roomServerVariables.getRoomServerChangeValues();
        for (String ffid : globalVar.keySet()) {
            String fileUuid = ffid.substring(0, 32);
            String fileVersionId = ffid.substring(32, 64);
            if (roomServerVariables.getRoomServerChangeValues().get(ffid) != null) {
                Set<String> paramUuidSet = roomServerVariables.getRoomServerChangeValues().get(ffid).keySet();
                for (String paramUuid : paramUuidSet) {
                    List<Object> paramTextAndUserId = (List) roomServerVariables.getRoomServerChangeValues().get(ffid).get(paramUuid);
                    // 取出数据后丢掉
                    roomServerVariables.getRoomServerChangeValues().get(ffid).remove(paramUuid);
                    // 获取填写信息
                    Object paramText = paramTextAndUserId.get(0);
                    String userId = (String) paramTextAndUserId.get(1);
                    // 创建填参数据
                    DocParamsPojo paramsPojo = new DocParamsPojo();
                    paramsPojo.setParamsUuid(paramUuid);
                    paramsPojo.setFileUuid(fileUuid);
                    paramsPojo.setFileVersionId(fileVersionId);
                    paramsPojo.setUserId(userId);
                    if (paramText instanceof Map || paramText instanceof List) {
                        paramsPojo.setParamsTextList(paramText);
                    } else {
                        paramsPojo.setParamsText(paramText);
                    }
                    String queueName = "hf-" + envName + "-" + fileUuid + fileVersionId;
                    amqUtils.sendMsgToMq("发送消息", queueName, JSON.toJSONString(paramsPojo));
                }
            }
        }
    }


    /**
     * 定期删除过期房间信息
     * 10分钟扫描一次
     * 启动后第一次等待5分钟后执行
     */
    @Scheduled(initialDelay = 60000 * 5, fixedRate = 60000 * 10)
    private void deleteRoomKeyTasks() {
        System.err.println("执行deleteRoomKeyTasks任务时间: " + LocalDateTime.now());
        Date date = new Date();
        try {
            // 初始化所有房间用户为离线状态
            String[] roomKeyList = redisUtils.dimSearch(envName + "_roomKey_*");
            if (roomKeyList.length != 0) {
                for (String roomKey : roomKeyList) {
                    try {
                        // 获取房间信息
                        HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
                        List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");

                        // 判断是否所有人join状态为false 并且 leaveTime时间超过30分钟
                        Boolean del = true;
                        for (Map<String, Object> userMap : userList) {
                            // 如果有一个人的在线状态为true 或者false但离开时间不超过30分钟 就不删除 , 否则删除
                            if ((userMap.get("join") != null && (Boolean) userMap.get("join")) // 有人在线
                                    ||
                                    (userMap.get("leaveTime") != null && date.getTime() - ((Date) userMap.get("leaveTime")).getTime() < 1000 * 60 * 30)) { // 离线时间不足30分钟
                                System.out.println(date.getTime() - ((Date) userMap.get("leaveTime")).getTime());
                                del = false;
                                break;
                            }
                        }
                        if (del) {
                            redisUtils.del(roomKey);
                            System.out.println("清除过期房间" + roomKey);

                            // 根据redis房间信息 维护queue
                            // 1.创建房间消息队列mq (已存在不会重建)
                            String fileUuid = roomKey.split("roomKey")[1].split("_")[1];
                            String fileVersionId = roomKey.split("roomKey")[1].split("_")[2];
                            String queueName = "hf-" + envName + "-" + fileUuid + fileVersionId;
                            destroyQueue(queueName);
                            System.out.println("清除过期mq" + queueName);
                        } else {
                            System.out.println("房间" + roomKey + "无需删除");
                        }

                    } catch (Exception e) {
                        System.out.println("deleteRoomKeyTasks失败[1]");
                    }
                }
                System.out.println("deleteRoomKeyTasks完成");
            } else {
                System.out.println("deleteRoomKeyTasks无需执行");
            }
        } catch (Exception e) {
            System.out.println("deleteRoomKeyTasks失败[2]");
        }
    }
}
