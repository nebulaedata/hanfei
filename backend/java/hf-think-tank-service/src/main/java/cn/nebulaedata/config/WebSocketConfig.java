package cn.nebulaedata.config;

/**
 * @author 徐衍旭
 * @date 2021/6/22 15:24
 * @note
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * 开启 WebSocket 支持
 *
 * @author :ZHANGPENGFEI
 * @create 2018-07-11 13:52
 **/
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}