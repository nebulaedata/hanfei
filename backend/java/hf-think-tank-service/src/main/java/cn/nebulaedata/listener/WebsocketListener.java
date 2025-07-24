//package cn.nebulaedata.listener;
//
//import org.springframework.stereotype.Component;
//
//import javax.servlet.ServletRequestEvent;
//import javax.servlet.ServletRequestListener;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpSession;
//
//用于Websocket获取httpsession
//@Component
//public class WebsocketListener implements ServletRequestListener{
//    @Override
//    public void requestInitialized(ServletRequestEvent sre)  {
//        HttpSession session = ((HttpServletRequest) sre.getServletRequest()).getSession();
//    }
//
//    public WebsocketListener(){}
//
//    @Override
//    public void requestDestroyed(ServletRequestEvent arg0)  {}
//}