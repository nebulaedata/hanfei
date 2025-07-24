package cn.nebulaedata.netty;

/**
 * @author 徐衍旭
 * @date 2023/12/11 14:03
 * @note
 */

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 监听Spring容器启动完成，完成后启动Netty服务器
 * @author Gjing
 **/
@Component
public class NettyStartListener implements ApplicationRunner {
    @Resource
    private SocketServer socketServer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        this.socketServer.start();
    }
}
