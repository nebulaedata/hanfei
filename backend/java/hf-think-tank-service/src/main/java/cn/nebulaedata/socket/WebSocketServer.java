package cn.nebulaedata.socket;


import com.alibaba.fastjson.JSON;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 服务类
 *
 * @author :ZHANGPENGFEI
 * @create 2018-07-11 13:54
 **/
@ServerEndpoint("/websocket/{sid}")
@Component
public class WebSocketServer {
    static Log log = LogFactory.getLog(WebSocketServer.class);
    // 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
    //concurrent 包的线程安全 Set ，用来存放每个客户端对应的 MyWebSocket 对象。
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();

    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    // 接收 sid
    private String sid = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        this.session = session;
        webSocketSet.add(this); // 加入 set 中
        addOnlineCount(); // 在线数加 1
        log.info(" 有新窗口开始监听 :" + sid + ", 当前在线人数为 " + getOnlineCount());
        this.sid = sid;
        try {
            System.out.println();
//            sendMessage(" 连接成功 ");
        } catch (Exception e) {
            log.error("websocket IO 异常 ");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this); // 从 set 中删除
        subOnlineCount(); // 在线数减 1
        log.info(" 有一连接关闭！当前在线人数为 " + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info(" 收到来自窗口 " + sid + " 的信息 :" + message);
// 群发消息
        for (WebSocketServer item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error(" 发生错误 ");
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message, @PathParam("sid") String sid) throws IOException {
        log.info(" 推送消息到窗口 " + sid + " ，推送内容 :" + message);
        for (WebSocketServer item : webSocketSet) {
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
        for (WebSocketServer item : webSocketSet) {
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

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

    public static void main(String[] args) throws IOException {
        WebSocketServer webSocketServer = new WebSocketServer();
        webSocketServer.sendInfo("1212","110");
    }
}