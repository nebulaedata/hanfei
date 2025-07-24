package cn.nebulaedata.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.List;
/**
 * 使用httpclient调用外部接口
 */
public class HttpClient {
    /**
     * get 请求
     * @return
     */
    public static String doHttpGet(String url, List<NameValuePair> params){
        String result = null;
        //1.获取httpclient
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //接口返回结果
        CloseableHttpResponse response = null;
        String paramStr = null;
        try {
            paramStr = EntityUtils.toString(new UrlEncodedFormEntity(params));
            //拼接参数
            StringBuffer sb = new StringBuffer();
            sb.append(url);
            sb.append("?");
            sb.append(paramStr);
            //2.创建get请求
            HttpGet httpGet = new HttpGet(sb.toString());
            //3.设置请求和传输超时时间
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
            httpGet.setConfig(requestConfig);
            /*此处可以添加一些请求头信息，例如：
            httpGet.addHeader("content-type","text/xml");*/
            //4.提交参数
            response = httpClient.execute(httpGet);
            //5.得到响应信息
            int statusCode = response.getStatusLine().getStatusCode();
            //6.判断响应信息是否正确
            if(HttpStatus.SC_OK != statusCode){
                //终止并抛出异常
                httpGet.abort();
                throw new RuntimeException("HttpClient,error status code :" + statusCode);
            }
            //7.转换成实体类
            HttpEntity entity = response.getEntity();
            if(null != entity){
                result = EntityUtils.toString(entity);
            }
            EntityUtils.consume(entity);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //8.关闭所有资源连接
            if(null != response){
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(null != httpClient){
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * http post 请求
     */
    public static String doPost(String url, List<NameValuePair> params){
        String result = null;
        //1. 获取httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            //2. 创建post请求
            HttpPost httpPost = new HttpPost(url);

            //3.设置请求和传输超时时间
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
            httpPost.setConfig(requestConfig);

            //4.提交参数发送请求
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params);
            /*此处可以设置传输时的编码格式，和数据格式
            urlEncodedFormEntity.setContentEncoding("UTF-8");
            urlEncodedFormEntity.setContentType("application/json");*/
            httpPost.setEntity(urlEncodedFormEntity);

            response = httpClient.execute(httpPost);

            //5.得到响应信息
            int statusCode = response.getStatusLine().getStatusCode();
            //6. 判断响应信息是否正确
            if(HttpStatus.SC_OK != statusCode){
                //结束请求并抛出异常
                httpPost.abort();
                throw new RuntimeException("HttpClient,error status code :" + statusCode);
            }
            //7. 转换成实体类
            HttpEntity entity = response.getEntity();
            if(null != entity){
                result = EntityUtils.toString(entity,"UTF-8");
            }
            EntityUtils.consume(entity);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //8. 关闭所有资源连接
            if(null != response){
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(null != httpClient){
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static String doPostJson(String url, JSONObject params) {
        String strResult = "";
        // 1. 获取默认的client实例
        CloseableHttpClient client = HttpClients.createDefault();
        // 2. 创建httppost实例
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json;charset=utf-8"); //添加请求头
        try {
            httpPost.setEntity(new StringEntity(params.toJSONString(), "utf-8"));
            CloseableHttpResponse resp = client.execute(httpPost);
            try {
                // 7. 获取响应entity
                HttpEntity respEntity = resp.getEntity();
                strResult = EntityUtils.toString(respEntity, "UTF-8");
            } finally {
                resp.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return strResult;
    }


    public static void doPostJsonR(String url, JSONObject params, String path) {
        InputStream ips = null;
        // 1. 获取默认的client实例
        CloseableHttpClient client = HttpClients.createDefault();
        // 2. 创建httppost实例
        HttpPost httpPost = new HttpPost(url);
//        httpPost.addHeader("Content-Type", "application/json;charset=utf-8"); //添加请求头
        try {
            httpPost.setEntity(new StringEntity(params.toJSONString(), "utf-8"));
            CloseableHttpResponse resp = client.execute(httpPost);
            try {
                // 7. 获取响应entity
                HttpEntity respEntity = resp.getEntity();
                ips = respEntity.getContent();

                int index;
                byte[] bytes = new byte[1024];
                FileOutputStream downloadFile = new FileOutputStream(path);
                while ((index = ips.read(bytes)) != -1) {
                    downloadFile.write(bytes, 0, index);
                    downloadFile.flush();
                }
                downloadFile.close();
                ips.close();

            } finally {
                resp.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        return ips;
    }

    /**
     * 将InputStream写入本地文件
     * @param destination 写入本地目录
     * @param input	输入流
     * @throws IOException
     */
    public static void writeToLocal(String destination, InputStream input) throws IOException {
        int index;
        byte[] bytes = new byte[1024];
        FileOutputStream downloadFile = new FileOutputStream(destination);
        while ((index = input.read(bytes)) != -1) {
            downloadFile.write(bytes, 0, index);
            downloadFile.flush();
        }
        downloadFile.close();
        input.close();
    }

    public static void main(String[] args) throws IOException {
        String fileUuid = "577ca9875ae34649965b855d48ba656c";
        String html = "<html><head/><body><h4><parameter uuid=\"\" key=\"\" typeid=\"\" placeholder=\"\" title=\"\" isunderline=\"0\" styleid=\"\" unit=\"\" matrixmode=\"datasource\" value=\"\" active=\"false\"><p><span style=\"font-family: 华文细黑\">1.1 项目概况</span></p></parameter></h4><p>1.1.1根据《中华人民共和国招标投标法》等有关法律、<span><parameter uuid=\"85aeabd0-3070-44f8-be93-32a22aee7956\" key=\"4ff1a7f2ddaa4371908da72e0212c8dd\" typeid=\"40\" placeholder=\"longlongago\" title=\"\" isunderline=\"0\" styleid=\"632038\" unit=\"\" matrixmode=\"datasource\" value=\"\" active=\"false\" alias=\"\"/></span>和规章的规定，本招标项目<annotation id=\"1fad06e5-b287-4159-b20f-e767751ea0ec\" key=\"1fad06e5-b287-4159-b20f-e767751ea0ec\" shiyi=\"1\" typeid=\"3\">已具备招</annotation>标条件，<annotation id=\"ab5b2d39-2942-4927-9f50-eb441858dcc0\" key=\"a9ad4a11779640ec9a730d80cf18cbcd\" zhaibian=\"a9ad4a11779640ec9a730d80cf18cbcd\" typeid=\"1\">现对本标</annotation>段施工进行招标。</p><p>1.1.2 本招标项目招标人：见投标人须知前附表。<parameter uuid=\"9b0244e1-4539-45ca-b3db-5a37c3254b7a\" key=\"883614f787824fb68f54ef9a894f5a3c\" typeid=\"90\" placeholder=\"两个柱子间的距离\" title=\"\" isunderline=\"1\" styleid=\"632006\" unit=\"\" matrixmode=\"datasource\" value=\"\" active=\"false\"/></p><p>1.1.3 本标段招标代理机构：见投标人须知前附表。</p><p>1.1.4 本招标项目名称：见投标人须知前附表。<parameter uuid=\"90afddab-35cc-4b72-a4ca-078877ce5f80\" key=\"883614f787824fb68f54ef9a894f5a3c\" typeid=\"90\" placeholder=\"两个柱子间的距离\" title=\"\" isunderline=\"1\" styleid=\"632006\" unit=\"\" matrixmode=\"datasource\" value=\"\" active=\"false\"/></p><p>1.1.5 本标段建设地点：见投标人须知<annotation id=\"2224ccf7-07fc-4e88-960d-e2cc5c0040c1\" key=\"c5a5cd5f-37b0-448e-8d71-df9d1016e59c\" yinyong=\"c5a5cd5f-37b0-448e-8d71-df9d1016e59c\" typeid=\"4\">前附表</annotation>。</p><p/><p/><p/><p/><p/><p/></body></html>";
        JSONObject params = new JSONObject();
        params.put("fileName", fileUuid);
        params.put("html", html);
//        String s = doPostJson("http://47.98.211.72:16652/jofficeservice/getDocx", params);
        doPostJsonR("http://47.98.211.72:16652/jofficeservice/getDocx", params, "C:\\Users\\xuyanxu\\Desktop\\1\\121.txt");
//        writeToLocal("C:\\Users\\xuyanxu\\Desktop\\1\\121.txt",inputStream);
////        System.out.println(s);
//        System.out.println(inputStream);
    }
}

