package cn.nebulaedata.netty.client;

/**
 * @author 徐衍旭
 * @date 2023/12/11 14:09
 * @note
 */

import java.io.IOException;

/**
 * @author Gjing
 **/
public class Client1 {
    public static void main(String[] args) throws IOException {
        new ChatClient().start("李四");
    }
}
