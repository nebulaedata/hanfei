package cn.nebulaedata.utils;

/**
 * @author 徐衍旭
 * @date 2021/12/5 17:10
 * @note
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
public class Client240 {

//    public static void main(String args[]) throws IOException {
//        //为了简单起见，所有的异常信息都往外抛
//        try{
//            //创建一个服务器对象，端口3333
//            ServerSocket serverSocket=new ServerSocket(20101);
//            //创建一个客户端对象，这里的作用是用作多线程，必经服务器服务的不是一个客户端
//            Socket client=null;
//            boolean flag=true;
//
//            while(flag){
//                System.out.println("服务器已启动，等待客户端请求。。。。");
//                //accept是阻塞式方法，对新手来说这里很有可能出错，下面的注意事项我会说到
//                client=serverSocket.accept();
//                //创建一个线程，每个客户端对应一个线程
//                new Thread(new EchoThread(client)).start();
//            }
//            client.close();
//            serverSocket.close();
//            System.out.println("服务器已关闭。");
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//
//    }


    public static void main(String[] args) throws Exception {
        /**
         * 实现UDP协议通信的客户端
         */
        System.out.println("==========这是客户端=============");

        String content = "初次学习Socket网络编程！";
        byte[] bytes = content.getBytes();

        //声明byte数组，为最原始的接收ip地址
        byte[] addr = {127,0,0,1};
        //创建InetAddress对象，封装需要传输到的主机
        InetAddress address = InetAddress.getByAddress(addr);

        //创建DatagramPacket对象，封装数据以及需要传输到的地址和端口
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length,address,7777);

        //创建DatagramSocket对象，用来将数据进行打包服务，指定快递员的端口为8888
        DatagramSocket socket = new DatagramSocket(8888);

        //实现数据传输
        socket.send(packet);

        socket.close();
    }



}

//class EchoThread implements Runnable{
//    private Socket client;
//    public EchoThread(Socket client){
//        this.client=client;
//
//    }
//    public void run(){
//        //run不需要自己去执行，好像是线程器去执行了来着，可以去看api
//        try {
//            BufferedReader in=null;
//            String br=null;
//            boolean flag=true;
//            while(flag==true){
//                //Java流的操作没意见吧
//                in=new BufferedReader(new InputStreamReader(client.getInputStream()));
//                br=in.readLine();
//                System.out.println("++:"+br);
////                recordMsg(br+);//写入到文件
//            }
//
//        } catch (IOException e1) {
//            // TODO 自动生成的 catch 块
//            e1.printStackTrace();
//        }catch (Exception e) {
//            // TODO: handle exception
//            System.out.println("error");
//        }
//
//
//    }
//    public void recordMsg(String br) throws IOException{
//        File file=new File("test.data");
//        if(!file.exists()){
//            file.createNewFile();
//        }
//        FileWriter writer=new FileWriter(file,true);
//        writer.write(br+"\r\n");
//        writer.close();
//
//    }
