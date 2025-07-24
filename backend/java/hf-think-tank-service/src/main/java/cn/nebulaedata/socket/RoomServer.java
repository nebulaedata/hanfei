package cn.nebulaedata.socket;


import cn.nebulaedata.dao.UserMapper;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.globalVariables.RoomServerVariables;
import cn.nebulaedata.pojo.DocUserPojo;
import cn.nebulaedata.utils.AmqUtils;
import cn.nebulaedata.utils.RedisUtils;
import com.alibaba.fastjson.JSON;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 服务类
 *
 * @author :ZHANGPENGFEI
 * @create 2018-07-11 13:54
 **/
@ServerEndpoint(value = "/room/{sid}/{userId}")
@Component
public class RoomServer {
    @Autowired
    private RedisUtils redisUtils;
    @Value("${doc-frame-service.env-name}")
    private String envName;
    @Autowired
    private AmqUtils amqUtils;
    @Autowired
    private UserMapper userMapper;

    private static RoomServer roomServer;

    @PostConstruct
    public void init() {
        roomServer = new RoomServer();
        roomServer.redisUtils = this.redisUtils;
        roomServer.envName = this.envName;
        roomServer.amqUtils = this.amqUtils;
        roomServer.userMapper = this.userMapper;
    }


    // 获取全局变量
    private RoomServerVariables roomServerVariables = RoomServerVariables.getInstance();

    static Log log = LogFactory.getLog(RoomServer.class);
    // 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static HashMap<String, Integer> onlineCountMap = new HashMap<>();
    //concurrent 包的线程安全 Set ，用来存放每个客户端对应的 MyWebSocket 对象。
    private static CopyOnWriteArraySet<RoomServer> webSocketSet = new CopyOnWriteArraySet<RoomServer>();

    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    // 接收 sid
    private String sid = "";
    // 接收 userId
    private String userId = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid, @PathParam("userId") String userId) {
        this.session = session;
        webSocketSet.add(this); // 加入 set 中
        addOnlineCount(sid); // 在线数加 1
        log.info(" 窗口开始监听 :" + sid + ", 窗口在线人数为 " + getOnlineCount(sid));
        this.sid = sid;
        this.userId = userId;
        try {
            // 获取双id
            String fvu = new String(Base64.getDecoder().decode(sid));
            String[] split = fvu.split("/");
            String fileUuid = split[0];
            String fileVersionId = split[1];

            // 查询当前文件的房间是否存在
            String roomKey = roomServer.envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
            // 获取房间信息
            HashMap<String, Object> roomMap = (HashMap<String, Object>) roomServer.redisUtils.get(roomKey);
            List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");

            // 获取被修改人员信息
            Map<String, Object> changeUserMap = new HashMap<>();
            for (Map<String, Object> userMap : userList) {
                if (userId.equals(String.valueOf(userMap.get("userId")))) {
                    userMap.put("join", true);
                    userMap.put("joinTime", new Date());
                    changeUserMap = userMap;
                    break;
                }
            }
            // 维护房间信息
            roomServer.redisUtils.set(roomKey, roomMap);

            // 发送系统通知
            HashMap<String, Object> map = new HashMap<>();
            map.put("role", "system");
            Map<String, Object> finalChangeUserMap = changeUserMap;
            map.put("data", new HashMap<String, Object>() {{
                put("allValues", JSON.parseObject(JSON.toJSONString(roomMap), Map.class));
                put("changedValues", JSON.parseObject(JSON.toJSONString(finalChangeUserMap), Map.class));
            }});
            map.put("action", "userJoin");
            System.out.println("用户加入");
            sendInfoExceptMe(map, sid, userId);
        } catch (Exception e) {
            log.error("websocket IO 异常 ");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid, @PathParam("userId") String userId) {
        boolean remove = webSocketSet.remove(this);// 从 set 中删除
        if (remove) {
            closeConnect(sid, userId);
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) throws WorkTableException {
        log.info(" 收到来自窗口 " + sid + " 的信息 :" + message);
        Map<String, Object> map = new HashMap<>();
        try {
            map = JSON.parseObject(message, Map.class);
        } catch (Exception e) {
            throw new WorkTableException("非JSON字符串");
        }
        // 处理消息
        // 判断消息类型
        String role = (String) map.get("role");
        String action = (String) map.get("action");
        Object data = (Object) map.get("data");
        if ("1".equals(role)) {
            System.out.println(1111);
            for (RoomServer item : webSocketSet) {
                try {
                    if (item.sid.equals(sid) && "666".equals(item.userId)) {  // 分发给自己以外的用户
                        item.sendMessage(message);
                    }
                } catch (IOException e) {
                    continue;
                }
            }
        } else if ("user".equals(role)) {
            System.out.println("收到用户操作");
            if ("writeParam".equals(action)) {
                // 带副作用 向mq中写数据
                // 根据sid 查询fileUuid fileVersionId
                try {
                    // 获取双id
                    String fvu = new String(Base64.getDecoder().decode(sid));
                    String[] split = fvu.split("/");
                    String fileUuid = split[0];
                    String fileVersionId = split[1];

                    if (data instanceof Map) {
                        Map<String, Object> changedValues = (Map) ((Map) data).get("changedValues");
                        for (String paramUuid : changedValues.keySet()) {  // 长度为1
                            Object paramText = changedValues.get(paramUuid);
                            if (roomServerVariables.getRoomServerChangeValues().get(fileUuid + fileVersionId) == null) {
                                roomServerVariables.getRoomServerChangeValues().put(fileUuid + fileVersionId, new HashMap<>());
                            }
                            // 两个长度的list 分别对应填写内容和userId
                            roomServerVariables.getRoomServerChangeValues().get(fileUuid + fileVersionId).put(paramUuid, new ArrayList<Object>() {{
                                add(paramText);
                                add(userId);
                            }});

//                            DocParamsPojo paramsPojo = new DocParamsPojo();
//                            paramsPojo.setParamsUuid(paramUuid);
//                            paramsPojo.setFileUuid(fileUuid);
//                            paramsPojo.setFileVersionId(fileVersionId);
//                            paramsPojo.setUserId(userId);
//                            if (paramText instanceof Map || paramText instanceof List) {
//                                paramsPojo.setParamsTextList(paramText);
//                            } else {
//                                paramsPojo.setParamsText(paramText);
//                            }
//                            String queueName = "hf-" + roomServer.envName + "-" + fileUuid+fileVersionId;
//                            roomServer.amqUtils.sendMsgToMq("发送消息",queueName,JSON.toJSONString(paramsPojo));
                        }
                    } else {
                        throw new WorkTableException("数据类型未知");
                    }
                } catch (WorkTableException e) {
                    throw new WorkTableException("数据类型未知");
                } catch (Exception e) {
                    throw new WorkTableException("发生错误");
                }
            }
            for (RoomServer item : webSocketSet) {
                try {
                    if (item.sid.equals(sid) && !item.userId.equals(userId)) {  // 分发给自己以外的用户
                        item.sendMessage(message);
                    }
                } catch (IOException e) {
                    continue;
                }
            }

        } else if ("system".equals(role)) {
            System.out.println("收到系统信息");
            for (RoomServer item : webSocketSet) {
                try {
                    if (item.sid.equals(sid) && !item.userId.equals(userId)) {  // 分发给自己以外的用户
                        item.sendMessage(message);
                    }
                } catch (IOException e) {
                    continue;
                }
            }
        } else {
            System.out.println("未知类型");
        }

    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, @PathParam("sid") String sid, @PathParam("userId") String userId, Throwable error) {
        log.error(" 发生错误 ");
        error.printStackTrace();
        boolean remove = webSocketSet.remove(this);// 从 set 中删除
        if (remove) {
            closeConnect(sid, userId);
        }
    }

    /**
     * 断开连接
     *
     * @param sid
     * @param userId
     */
    public void closeConnect(@PathParam("sid") String sid, @PathParam("userId") String userId) {
        subOnlineCount(sid); // 在线数减 1
        log.info(" 有一连接关闭！ " + sid + "窗口当前在线人数为 " + getOnlineCount(sid));
        try {
            // 获取双id
            String fvu = new String(Base64.getDecoder().decode(sid));
            String[] split = fvu.split("/");
            String fileUuid = split[0];
            String fileVersionId = split[1];

            // 查询当前文件的房间是否存在
            String roomKey = roomServer.envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
            // 获取房间信息
            HashMap<String, Object> roomMap = (HashMap<String, Object>) roomServer.redisUtils.get(roomKey);
            List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");

            // 获取被修改人员信息
            Map<String, Object> changeUserMap = new HashMap<>();
            for (Map<String, Object> userMap : userList) {
                if (userId.equals(String.valueOf(userMap.get("userId")))) {
                    userMap.put("join", false);
                    userMap.put("leaveTime",new Date());
                    changeUserMap = userMap;
                    break;
                }
            }
            // 维护房间信息
            roomServer.redisUtils.set(roomKey, roomMap);
            if (changeUserMap.get("userName") == null) {  // 防止用户remove后 无法在userList获取到信息
                try {
                    DocUserPojo userInfoDao = roomServer.userMapper.getUserInfoDao(userId);
                    changeUserMap.put("userName", userInfoDao.getUserName());
                    changeUserMap.put("userId", userInfoDao.getUserId());
                } catch (Exception e) {
                }
            }

            // 发送系统通知
            HashMap<String, Object> map = new HashMap<>();
            map.put("role", "system");
            Map<String, Object> finalChangeUserMap = changeUserMap;
            map.put("data", new HashMap<String, Object>() {{
                put("allValues", JSON.parseObject(JSON.toJSONString(roomMap), Map.class));
                put("changedValues", JSON.parseObject(JSON.toJSONString(finalChangeUserMap), Map.class));
            }});
            map.put("action", "userLeave");
            System.out.println("用户离开");
            sendInfoExceptMe(map, sid, userId);
        } catch (Exception e) {
            log.error("websocket IO 异常 ");
        }
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 群发(除自己外)自定义消息
     */
    public static void sendInfoExceptMe(String message, @PathParam("sid") String sid, String selfUserId) throws IOException {
        log.info(" 推送消息到窗口 " + sid + " ，推送内容 :" + message);
        for (RoomServer item : webSocketSet) {
            try {
                // 这里可以设定只推送给这个 sid 的，为 null 则全部推送
                if (item.sid.equals(sid) && !item.userId.equals(selfUserId)) {  // 分发给自己以外的用户
                    item.sendMessage(message);
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    /**
     * 群发(除自己外)自定义消息
     */
    public static void sendInfoExceptMe(Object message, @PathParam("sid") String sid, String selfUserId) throws IOException {
        log.info(" 推送消息到窗口 " + sid + " ，推送内容 :" + message);
        for (RoomServer item : webSocketSet) {
            try {
                // 这里可以设定只推送给这个 sid 的，为 null 则全部推送
                if (item.sid.equals(sid) && !item.userId.equals(selfUserId)) {  // 分发给自己以外的用户
                    item.sendMessage(JSON.toJSONString(message));
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message, @PathParam("sid") String sid) throws IOException {
        log.info(" 推送消息到窗口 " + sid + " ，推送内容 :" + message);
        for (RoomServer item : webSocketSet) {
            try {
                // 这里可以设定只推送给这个 sid 的，为 null 则全部推送
                if (sid == null) {
                    item.sendMessage(message);
                } else if (item.sid.equals(sid)) {
                    item.sendMessage(message);
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    /**
     * 群发自定义消息
     */
    public static void sendInfo(Object obj, @PathParam("sid") String sid) throws IOException {
        log.info(" 推送消息到窗口 " + sid + " ，推送内容 :" + JSON.toJSONString(obj));
        for (RoomServer item : webSocketSet) {
            try {
                // 这里可以设定只推送给这个 sid 的，为 null 则全部推送
                if (sid == null) {
                    item.sendMessage(JSON.toJSONString(obj));
                } else if (item.sid.equals(sid)) {
                    item.sendMessage(JSON.toJSONString(obj));
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    public static synchronized int getOnlineCount(String sid) {
        if (RoomServer.onlineCountMap.get(sid) == null) {
            return 0;
        } else {
            return RoomServer.onlineCountMap.get(sid);
        }
    }

    public static synchronized void addOnlineCount(String sid) {
        if (RoomServer.onlineCountMap.get(sid) == null) {
            RoomServer.onlineCountMap.put(sid, 1);
        } else {
            RoomServer.onlineCountMap.put(sid, RoomServer.onlineCountMap.get(sid) + 1);
        }
    }

    public static synchronized void subOnlineCount(String sid) {
        if (RoomServer.onlineCountMap.get(sid) == null) {
            RoomServer.onlineCountMap.put(sid, 0);
        } else {
            RoomServer.onlineCountMap.put(sid, RoomServer.onlineCountMap.get(sid) - 1);
        }
    }

    public static void main(String[] args) throws IOException {
        RoomServer webSocketServer = new RoomServer();
        webSocketServer.sendInfo("1212", "222");
    }
}