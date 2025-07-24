package cn.nebulaedata.controller;

/**
 * @author 徐衍旭
 * @date 2021/6/22 15:27
 * @note
 */


import cn.nebulaedata.socket.WebSocketServer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * 消息推送
 *
 * @author :ZHANGPENGFEI
 * @create 2018-07-11 14:12
 **/
@Controller
@RequestMapping("/checkcenter")
public class CheckCenterController {

    // 页面请求
    @GetMapping("/socket/{cid}")
    public ModelAndView socket(@PathVariable String cid) {
        ModelAndView mav = new ModelAndView("/socket");
        mav.addObject("cid", cid);
        return mav;
    }

    // 推送数据接口
    @ResponseBody
    @RequestMapping("/socket/push/{cid}")
    public String pushToWeb(@PathVariable String cid, String message) {
        try {
            WebSocketServer.sendInfo(message, cid);
        } catch (IOException e) {
            e.printStackTrace();
            return cid + "#" + e.getMessage();
        }
        return cid;
    }
}